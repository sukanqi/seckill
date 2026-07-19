package com.example.seckill.controller;

import com.example.seckill.entity.User;
import com.example.seckill.mapper.UserMapper;
import com.example.seckill.util.JwtUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            return Map.of("success", false, "msg", "用户不存在");
        }
        if (!user.getPassword().equals(request.getPassword())) {
            return Map.of("success", false, "msg", "密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Map.of(
                "success", true,
                "token", token,
                "user", Map.of("id", user.getId(), "username", user.getUsername())
        );
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody AuthRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return Map.of("success", false, "msg", "请输入用户名");
        }
        if (request.getPassword() == null || request.getPassword().length() < 3) {
            return Map.of("success", false, "msg", "密码至少3位");
        }
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            return Map.of("success", false, "msg", "用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPassword(request.getPassword());
        userMapper.insert(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Map.of(
                "success", true,
                "token", token,
                "user", Map.of("id", user.getId(), "username", user.getUsername())
        );
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) {
            return Map.of("success", false, "msg", "未登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Map.of("success", false, "msg", "用户不存在");
        }
        return Map.of("success", true, "user", Map.of("id", user.getId(), "username", user.getUsername()));
    }

    @Data
    public static class AuthRequest {
        private String username;
        private String password;
    }
}
