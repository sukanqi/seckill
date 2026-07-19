package com.example.seckill.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SPA 路由转发控制器
 * 将前端路由（非 /api 路径）转发到 index.html
 */
@Controller
public class SpaController {

    @RequestMapping(value = {"/{path:[^\\.]*}", "/**/{path:[^\\.]*}"})
    public String forward() {
        return "forward:/index.html";
    }
}
