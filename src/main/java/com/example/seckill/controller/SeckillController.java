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
     */
    @PostMapping("/order")
    @SeckillRateLimit(permitsPerSecond = 100.0, timeoutMs = 0)
    public SeckillResult placeOrder(@RequestBody SeckillRequest request) {
        log.info("Received seckill order request: userId={}, productId={}",
                request.getUserId(), request.getProductId());

        if (request.getUserId() == null || request.getProductId() == null) {
            return SeckillResult.fail("参数不完整");
        }

        return seckillService.placeOrder(request.getUserId(), request.getProductId());
    }

    /**
     * Poll for flash sale result
     * GET /api/seckill/result?userId=xxx&productId=xxx
     */
    @GetMapping("/result")
    public SeckillResult queryResult(@RequestParam("userId") Long userId,
                                     @RequestParam("productId") Long productId) {
        log.info("Query seckill result: userId={}, productId={}", userId, productId);

        if (userId == null || productId == null) {
            return SeckillResult.fail("参数不完整");
        }

        return seckillService.queryResult(userId, productId);
    }
}
