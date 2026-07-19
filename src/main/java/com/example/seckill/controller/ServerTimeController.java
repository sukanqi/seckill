package com.example.seckill.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 服务器时间 API
 * 用于前端倒计时与服务器时间同步，防止用户修改本地时间作弊
 */
@RestController
@RequestMapping("/api/server")
public class ServerTimeController {

    @GetMapping("/time")
    public Map<String, Long> getServerTime() {
        return Map.of("serverTime", System.currentTimeMillis());
    }
}
