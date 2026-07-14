package com.example.seckill.service;

import com.example.seckill.dto.SeckillResult;

public interface SeckillService {

    /**
     * Place a flash sale order
     */
    SeckillResult placeOrder(Long userId, Long productId);

    /**
     * Query the flash sale result by userId and productId
     */
    SeckillResult queryResult(Long userId, Long productId);
}
