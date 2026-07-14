package com.example.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillOrder {
    private Long id;
    private Long userId;
    private Long productId;
    private String orderId;
    private BigDecimal seckillPrice;
    private Integer status;
    private LocalDateTime createTime;
}
