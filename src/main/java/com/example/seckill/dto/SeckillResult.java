package com.example.seckill.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResult {
    private Integer status;
    private String orderId;
    private String msg;

    public static SeckillResult success(String orderId) {
        return new SeckillResult(1, orderId, "秒杀成功");
    }

    public static SeckillResult fail(String msg) {
        return new SeckillResult(0, null, msg);
    }

    public static SeckillResult waiting() {
        return new SeckillResult(-1, null, "排队中，请稍后查询结果");
    }
}
