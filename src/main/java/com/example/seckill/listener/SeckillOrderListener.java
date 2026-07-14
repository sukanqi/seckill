package com.example.seckill.listener;

import com.example.seckill.entity.SeckillOrder;
import com.example.seckill.mapper.SeckillOrderMapper;
import com.example.seckill.mapper.SeckillProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderListener {

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillProductMapper seckillProductMapper;

    @RabbitListener(queues = "${seckill.rabbitmq.queue}")
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderMessage(SeckillOrder order) {
        log.info("Received order message from MQ: orderId={}, userId={}, productId={}",
                order.getOrderId(), order.getUserId(), order.getProductId());

        try {
            // 1. Check if order already exists in DB (idempotent consumer)
            SeckillOrder existingOrder = seckillOrderMapper.selectByOrderId(order.getOrderId());
            if (existingOrder != null) {
                log.warn("Order already exists in DB, skipping: orderId={}", order.getOrderId());
                return;
            }

            // 2. Set create time and default status
            order.setCreateTime(LocalDateTime.now());
            if (order.getStatus() == null) {
                order.setStatus(0);
            }

            // 3. Insert order into database
            int insertResult = seckillOrderMapper.insert(order);
            if (insertResult <= 0) {
                log.error("Failed to insert order into DB: orderId={}", order.getOrderId());
                throw new RuntimeException("Order insert failed");
            }

            // 4. Decrement stock in database (as a final consistency check)
            int updateResult = seckillProductMapper.decrementStock(order.getProductId());
            if (updateResult <= 0) {
                log.error("Failed to decrement stock in DB for productId={}, orderId={}",
                        order.getProductId(), order.getOrderId());
                throw new RuntimeException("Stock decrement failed");
            }

            log.info("Order successfully persisted: orderId={}", order.getOrderId());
        } catch (Exception e) {
            log.error("Error processing order message: orderId={}, error={}",
                    order.getOrderId(), e.getMessage(), e);
            // Re-throw to trigger message redelivery or DLQ
            throw e;
        }
    }
}
