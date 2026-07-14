package com.example.seckill.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeckillProduct {
    private Long id;
    private Long productId;
    private String title;
    private String image;
    private BigDecimal originalPrice;
    private BigDecimal seckillPrice;
    private Integer stockCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
