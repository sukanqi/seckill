package com.example.seckill.limiter;

import com.google.common.util.concurrent.RateLimiter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Aspect
@Component
public class RateLimiterAspect {

    /**
     * Cache for RateLimiter instances per method
     */
    private final ConcurrentMap<String, RateLimiter> rateLimiterMap = new ConcurrentHashMap<>();

    /**
     * Custom annotation for rate limiting
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SeckillRateLimit {
        /**
         * Permits per second (QPS)
         */
        double permitsPerSecond() default 100.0;

        /**
         * Timeout in milliseconds to wait for a permit
         */
        long timeoutMs() default 0;
    }

    @Around("@annotation(com.example.seckill.limiter.RateLimiterAspect.SeckillRateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SeckillRateLimit rateLimit = method.getAnnotation(SeckillRateLimit.class);

        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        // Generate a unique key for the method
        String methodKey = method.getDeclaringClass().getName() + "." + method.getName();
        double permitsPerSecond = rateLimit.permitsPerSecond();
        long timeoutMs = rateLimit.timeoutMs();

        // Get or create RateLimiter (token bucket)
        RateLimiter rateLimiter = rateLimiterMap.computeIfAbsent(methodKey,
                key -> RateLimiter.create(permitsPerSecond));

        // Try to acquire a permit
        boolean acquired;
        if (timeoutMs > 0) {
            acquired = rateLimiter.tryAcquire(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            // Non-blocking try
            acquired = rateLimiter.tryAcquire();
        }

        if (!acquired) {
            log.warn("Rate limit exceeded for method: {}, QPS limit: {}", methodKey, permitsPerSecond);
            // Return error response if possible
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletResponse response = attributes.getResponse();
                if (response != null && !response.isCommitted()) {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(429);
                    try {
                        response.getWriter().write("{\"status\":0,\"orderId\":null,\"msg\":\"请求过于频繁，请稍后重试\"}");
                    } catch (IOException e) {
                        log.error("Error writing rate limit response", e);
                    }
                }
            }
            return null;
        }

        log.debug("Rate limit permit acquired for method: {}", methodKey);
        return joinPoint.proceed();
    }
}
