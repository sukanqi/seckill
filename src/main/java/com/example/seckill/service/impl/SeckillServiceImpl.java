package com.example.seckill.service.impl;

import com.example.seckill.dto.SeckillResult;
import com.example.seckill.entity.SeckillOrder;
import com.example.seckill.entity.SeckillProduct;
import com.example.seckill.mapper.SeckillOrderMapper;
import com.example.seckill.mapper.SeckillProductMapper;
import com.example.seckill.service.SeckillService;
import com.example.seckill.util.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;
    private final StockService stockService;
    private final SeckillProductMapper seckillProductMapper;
    private final SeckillOrderMapper seckillOrderMapper;

    @Value("${seckill.rabbitmq.exchange}")
    private String exchange;

    @Value("${seckill.rabbitmq.routing-key}")
    private String routingKey;

    private static final String STOCK_PREFIX = "seckill:stock:";
    private static final String ORDER_SET_PREFIX = "seckill:order:";
    private static final String LOCK_PREFIX = "seckill:lock:";
    private static final String RESULT_PREFIX = "seckill:result:";

    @Override
    public SeckillResult placeOrder(Long userId, Long productId) {
        // 1. Check if the flash sale session is valid
        SeckillProduct product = seckillProductMapper.selectByProductId(productId);
        if (product == null) {
            return SeckillResult.fail("商品不存在");
        }

        // 2. User idempotency check: prevent duplicate orders using Redis SetNX
        String orderSetKey = ORDER_SET_PREFIX + productId;
        Long added = stringRedisTemplate.opsForSet().add(orderSetKey, String.valueOf(userId));
        if (added == null || added == 0) {
            // User already placed an order for this product
            log.warn("Duplicate order attempt: userId={}, productId={}", userId, productId);
            String resultKey = RESULT_PREFIX + userId + ":" + productId;
            String orderId = stringRedisTemplate.opsForValue().get(resultKey);
            if (orderId != null) {
                return SeckillResult.success(orderId);
            }
            return SeckillResult.fail("请勿重复下单");
        }

        // 3. Execute Lua script for atomic stock deduction via Redis incr
        Long stockRemaining = stockService.decrementStock(productId);
        if (stockRemaining == null || stockRemaining < 0) {
            // Rollback the idempotency set entry
            stringRedisTemplate.opsForSet().remove(orderSetKey, String.valueOf(userId));
            log.warn("Insufficient stock: productId={}", productId);
            return SeckillResult.fail("库存不足");
        }

        // 4. Acquire distributed lock for order creation to prevent overselling in cluster
        String lockKey = LOCK_PREFIX + productId + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // Try to acquire lock with watchdog auto-renewal (default 30s lease, auto-renew every 10s)
            boolean locked = lock.tryLock(3, TimeUnit.SECONDS);
            if (!locked) {
                // Rollback operations on lock failure
                stockService.incrementStock(productId);
                stringRedisTemplate.opsForSet().remove(orderSetKey, String.valueOf(userId));
                return SeckillResult.fail("系统繁忙，请重试");
            }

            // 5. Generate order ID and send async MQ message
            String orderId = generateOrderId(userId, productId);

            // 6. Store preliminary result in Redis for polling
            String resultKey = RESULT_PREFIX + userId + ":" + productId;
            stringRedisTemplate.opsForValue().set(resultKey, orderId, 30, TimeUnit.MINUTES);

            // 7. Send RabbitMQ message for asynchronous DB write
            SeckillOrder orderMsg = new SeckillOrder();
            orderMsg.setUserId(userId);
            orderMsg.setProductId(productId);
            orderMsg.setOrderId(orderId);
            orderMsg.setSeckillPrice(product.getSeckillPrice());
            orderMsg.setStatus(0);

            rabbitTemplate.convertAndSend(exchange, routingKey, orderMsg);
            log.info("Order message sent to MQ: orderId={}, userId={}, productId={}", orderId, userId, productId);

            return SeckillResult.waiting();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stockService.incrementStock(productId);
            stringRedisTemplate.opsForSet().remove(orderSetKey, String.valueOf(userId));
            return SeckillResult.fail("系统异常");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public SeckillResult queryResult(Long userId, Long productId) {
        // First check Redis for quick result
        String resultKey = RESULT_PREFIX + userId + ":" + productId;
        String orderId = stringRedisTemplate.opsForValue().get(resultKey);
        if (orderId != null) {
            return SeckillResult.success(orderId);
        }

        // Check DB if not found in Redis
        SeckillOrder order = seckillOrderMapper.selectByUserIdAndProductId(userId, productId);
        if (order != null) {
            // Cache back to Redis
            stringRedisTemplate.opsForValue().set(resultKey, order.getOrderId(), 30, TimeUnit.MINUTES);
            return SeckillResult.success(order.getOrderId());
        }

        return SeckillResult.fail("订单不存在，秒杀失败");
    }

    private String generateOrderId(Long userId, Long productId) {
        return "SK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
