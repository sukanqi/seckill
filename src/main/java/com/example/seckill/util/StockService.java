package com.example.seckill.util;

import com.example.seckill.entity.SeckillProduct;
import com.example.seckill.mapper.SeckillProductMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SeckillProductMapper seckillProductMapper;

    private static final String STOCK_PREFIX = "seckill:stock:";
    private static final String LUA_SCRIPT_PATH = "seckill.lua";

    private DefaultRedisScript<Long> decrementScript;
    private DefaultRedisScript<Long> incrementScript;

    @PostConstruct
    public void init() {
        // Load Lua scripts
        decrementScript = new DefaultRedisScript<>();
        decrementScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_SCRIPT_PATH)));
        decrementScript.setResultType(Long.class);

        // Increment script (simple INCBY)
        incrementScript = new DefaultRedisScript<>();
        incrementScript.setScriptText(
                "local key = KEYS[1]\n" +
                "local delta = tonumber(ARGV[1])\n" +
                "return redis.call('INCRBY', key, delta)"
        );
        incrementScript.setResultType(Long.class);

        // Warm up cache: load all seckill product stock into Redis
        warmUpCache();
    }

    /**
     * Cache warmup: load seckill product stock from DB into Redis
     */
    public void warmUpCache() {
        List<SeckillProduct> products = seckillProductMapper.selectAll();
        if (products == null || products.isEmpty()) {
            log.warn("No seckill products found for cache warmup");
            return;
        }

        for (SeckillProduct product : products) {
            String stockKey = STOCK_PREFIX + product.getProductId();
            // Only set if not already present (allow manual override)
            Boolean exists = stringRedisTemplate.hasKey(stockKey);
            if (Boolean.FALSE.equals(exists)) {
                stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(product.getStockCount()));
                log.info("Cache warmup: productId={}, stock={}", product.getProductId(), product.getStockCount());
            }
        }
        log.info("Cache warmup completed for {} products", products.size());
    }

    /**
     * Warm up a specific product's stock in cache
     */
    public void warmUpProduct(Long productId) {
        SeckillProduct product = seckillProductMapper.selectByProductId(productId);
        if (product == null) {
            log.warn("Product not found for warmup: productId={}", productId);
            return;
        }
        String stockKey = STOCK_PREFIX + productId;
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(product.getStockCount()));
        log.info("Product cache warmup: productId={}, stock={}", productId, product.getStockCount());
    }

    /**
     * Atomic stock decrement using Lua script
     * Returns the remaining stock after decrement, or -1 if insufficient stock
     */
    public Long decrementStock(Long productId) {
        String stockKey = STOCK_PREFIX + productId;
        List<String> keys = Collections.singletonList(stockKey);
        // ARGV[1] = decrement amount (1)
        Long result = stringRedisTemplate.execute(decrementScript, keys, String.valueOf(1));
        log.debug("Stock decrement: productId={}, result={}", productId, result);
        return result;
    }

    /**
     * Atomic stock increment (rollback operation)
     */
    public Long incrementStock(Long productId) {
        String stockKey = STOCK_PREFIX + productId;
        List<String> keys = Collections.singletonList(stockKey);
        Long result = stringRedisTemplate.execute(incrementScript, keys, String.valueOf(1));
        log.debug("Stock increment (rollback): productId={}, result={}", productId, result);
        return result;
    }

    /**
     * Get current stock from Redis
     */
    public Integer getStock(Long productId) {
        String stockKey = STOCK_PREFIX + productId;
        String stockStr = stringRedisTemplate.opsForValue().get(stockKey);
        return stockStr != null ? Integer.parseInt(stockStr) : null;
    }
}
