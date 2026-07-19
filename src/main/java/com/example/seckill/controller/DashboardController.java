package com.example.seckill.controller;

import com.example.seckill.mapper.SeckillOrderMapper;
import com.example.seckill.mapper.SeckillProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillProductMapper seckillProductMapper;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> stats = new HashMap<>();
        try {
            long totalOrders = seckillOrderMapper.countAll();
            long todayOrders = seckillOrderMapper.countToday();
            long pendingOrders = seckillOrderMapper.countByStatus(0);
            long completedOrders = seckillOrderMapper.countByStatus(1);
            long totalProducts = seckillProductMapper.selectAll().size();
            long lowStockProducts = seckillProductMapper.countLowStock(20);

            stats.put("success", true);
            stats.put("totalOrders", totalOrders);
            stats.put("todayOrders", todayOrders);
            stats.put("pendingOrders", pendingOrders);
            stats.put("completedOrders", completedOrders);
            stats.put("totalProducts", totalProducts);
            stats.put("lowStockProducts", lowStockProducts);
        } catch (Exception e) {
            log.error("Dashboard error", e);
            stats.put("success", false);
            stats.put("msg", e.getMessage());
        }
        return stats;
    }
}
