package com.example.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;

        config.useSingleServer()
                .setAddress(address)
                .setConnectionPoolSize(64)
                .setConnectionMinimumIdleSize(16)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(5000)
                .setTimeout(3000);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }

        // Watchdog timeout: default 30 seconds, auto-renewed every 10 seconds
        config.setLockWatchdogTimeout(30000);

        return Redisson.create(config);
    }
}
