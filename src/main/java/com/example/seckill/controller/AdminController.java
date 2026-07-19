package com.example.seckill.controller;

import com.example.seckill.entity.SeckillOrder;
import com.example.seckill.entity.SeckillProduct;
import com.example.seckill.mapper.SeckillOrderMapper;
import com.example.seckill.mapper.SeckillProductMapper;
import com.example.seckill.util.StockService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 管理后台 API
 * 管理员登录、商品管理、订单管理
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SeckillProductMapper seckillProductMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final StockService stockService;

    // 简单管理员账号（生产环境应使用数据库 + JWT）
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AdminLoginRequest request) {
        if (ADMIN_USER.equals(request.getUsername()) && ADMIN_PASS.equals(request.getPassword())) {
            String token = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            log.info("Admin login success: username={}", request.getUsername());
            return Map.of("success", true, "token", token, "username", ADMIN_USER);
        }
        return Map.of("success", false, "msg", "用户名或密码错误");
    }

    /**
     * 获取所有秒杀商品
     */
    @GetMapping("/products")
    public Map<String, Object> listProducts() {
        try {
            List<SeckillProduct> products = seckillProductMapper.selectAll();
            return Map.of("success", true, "data", products);
        } catch (Exception e) {
            log.error("Failed to list products", e);
            return Map.of("success", false, "msg", e.getMessage());
        }
    }

    /**
     * 获取单个商品
     */
    @GetMapping("/products/{id}")
    public Map<String, Object> getProduct(@PathVariable Long id) {
        try {
            SeckillProduct product = seckillProductMapper.selectById(id);
            if (product == null) {
                return Map.of("success", false, "msg", "商品不存在");
            }
            return Map.of("success", true, "data", product);
        } catch (Exception e) {
            return Map.of("success", false, "msg", e.getMessage());
        }
    }

    /**
     * 添加秒杀商品
     */
    @PostMapping("/products")
    public Map<String, Object> addProduct(@RequestBody SeckillProduct product) {
        try {
            if (product.getProductId() == null || product.getTitle() == null) {
                return Map.of("success", false, "msg", "商品ID和标题不能为空");
            }
            product.setCreateTime(LocalDateTime.now());
            seckillProductMapper.insert(product);

            // 预热 Redis 缓存
            stockService.warmUpProduct(product.getProductId());

            log.info("Admin added product: productId={}, title={}", product.getProductId(), product.getTitle());
            return Map.of("success", true, "msg", "添加成功");
        } catch (Exception e) {
            log.error("Failed to add product", e);
            return Map.of("success", false, "msg", e.getMessage());
        }
    }

    /**
     * 更新秒杀商品
     */
    @PutMapping("/products/{id}")
    public Map<String, Object> updateProduct(@PathVariable Long id, @RequestBody SeckillProduct product) {
        try {
            product.setId(id);
            seckillProductMapper.updateByPrimaryKey(product);

            // 重新预热缓存
            stockService.warmUpProduct(product.getProductId());

            log.info("Admin updated product: id={}, productId={}", id, product.getProductId());
            return Map.of("success", true, "msg", "更新成功");
        } catch (Exception e) {
            log.error("Failed to update product", e);
            return Map.of("success", false, "msg", e.getMessage());
        }
    }

    /**
     * 删除秒杀商品
     */
    @DeleteMapping("/products/{id}")
    public Map<String, Object> deleteProduct(@PathVariable Long id) {
        try {
            seckillProductMapper.deleteByPrimaryKey(id);
            log.info("Admin deleted product: id={}", id);
            return Map.of("success", true, "msg", "删除成功");
        } catch (Exception e) {
            log.error("Failed to delete product", e);
            return Map.of("success", false, "msg", e.getMessage());
        }
    }

    /**
     * 获取所有订单
     */
    @GetMapping("/orders")
    public Map<String, Object> listOrders() {
        try {
            List<SeckillOrder> orders = seckillOrderMapper.selectAll();
            return Map.of("success", true, "data", orders);
        } catch (Exception e) {
            log.error("Failed to list orders", e);
            return Map.of("success", false, "msg", e.getMessage());
        }
    }

    @Data
    public static class AdminLoginRequest {
        private String username;
        private String password;
    }
}
