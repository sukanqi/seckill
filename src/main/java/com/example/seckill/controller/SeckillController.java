package com.example.seckill.controller;

import com.example.seckill.dto.SeckillRequest;
import com.example.seckill.dto.SeckillResult;
import com.example.seckill.limiter.RateLimiterAspect.SeckillRateLimit;
import com.example.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * Place a flash sale order
     * POST /api/seckill/order
     * 优先从 JWT token 获取 userId，兼容旧版 body 传参
     */
    @PostMapping("/order")
    @SeckillRateLimit(permitsPerSecond = 100.0, timeoutMs = 0)
    public SeckillResult placeOrder(@RequestBody SeckillRequest request,
                                    @RequestAttribute(value = "userId", required = false) Long jwtUserId) {
        Long userId = jwtUserId != null ? jwtUserId : request.getUserId();
        log.info("Received seckill order request: userId={}, productId={}", userId, request.getProductId());

        if (userId == null || request.getProductId() == null) {
            return SeckillResult.fail("参数不完整");
        }

        return seckillService.placeOrder(userId, request.getProductId());
    }

    /**
     * Poll for flash sale result
     * GET /api/seckill/result?userId=xxx&productId=xxx
     * 优先从 JWT token 获取 userId
     */
    @GetMapping("/result")
    public SeckillResult queryResult(@RequestParam(value = "userId", required = false) Long userId,
                                     @RequestParam("productId") Long productId,
                                     @RequestAttribute(value = "userId", required = false) Long jwtUserId) {
        Long uid = jwtUserId != null ? jwtUserId : userId;
        log.info("Query seckill result: userId={}, productId={}", uid, productId);

        if (uid == null || productId == null) {
            return SeckillResult.fail("参数不完整");
        }

        return seckillService.queryResult(uid, productId);
    }
}
