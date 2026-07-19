package com.example.seckill.scheduler;

import com.example.seckill.entity.SeckillOrder;
import com.example.seckill.mapper.SeckillOrderMapper;
import com.example.seckill.mapper.SeckillProductMapper;
import com.example.seckill.util.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消定时任务
 * 每分钟扫描一次超时未支付订单，自动取消并释放库存
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private static final int TIMEOUT_MINUTES = 30; // 订单超时时间（分钟）

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillProductMapper seckillProductMapper;
    private final StockService stockService;

    /**
     * 每分钟执行一次，取消超时订单
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional(rollbackFor = Exception.class)
    public void cancelExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<SeckillOrder> expiredOrders = seckillOrderMapper.selectByStatusAndCreateTimeBefore(0, deadline);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Found {} expired orders to cancel (deadline: {})", expiredOrders.size(), deadline);

        for (SeckillOrder order : expiredOrders) {
            try {
                // 1. 更新订单状态为已取消 (2)
                int updated = seckillOrderMapper.updateStatus(order.getOrderId(), 2);
                if (updated <= 0) {
                    log.warn("Failed to cancel order: orderId={}", order.getOrderId());
                    continue;
                }

                // 2. 恢复数据库库存
                seckillProductMapper.incrementStock(order.getProductId());

                // 3. 恢复 Redis 缓存库存
                stockService.incrementStock(order.getProductId());

                log.info("Order cancelled and stock released: orderId={}, productId={}",
                        order.getOrderId(), order.getProductId());
            } catch (Exception e) {
                log.error("Error cancelling order: orderId={}", order.getOrderId(), e);
            }
        }
    }
}
