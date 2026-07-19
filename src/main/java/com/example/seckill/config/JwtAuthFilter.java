package com.example.seckill.config;

import com.example.seckill.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 鉴权过滤器
 * 如果请求携带有效 JWT token，解析 userId 放入 request 属性
 * 不强制要求 token（兼容旧版请求），但管理后台写操作等敏感接口仍可在控制器内校验
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 如果有 Authorization header，尝试解析 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    request.setAttribute("userId", userId);
                }
            } catch (Exception ignored) {}
        }

        // 所有请求都放行（控制器根据 userId 是否为空决定是否要鉴权）
        filterChain.doFilter(request, response);
    }
}
