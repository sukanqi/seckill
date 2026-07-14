# Flash Sale System

电商秒杀系统，基于 Spring Boot + Redis + RabbitMQ + MySQL 构建。

## 功能

- 高并发秒杀下单
- Redis incr 原子扣库存 + Lua 脚本保证一致性
- Redisson 分布式锁防止超卖
- 接口幂等（Redis SetNX）防止重复下单
- Guava RateLimiter 令牌桶限流
- RabbitMQ 异步削峰

## 技术亮点

- **原子扣库存**: Lua 脚本保证扣库存与校验的原子性，替代数据库行锁
- **集群超卖解决**: synchronized 本地锁 → 排查 → Redisson 分布式锁 + 看门狗自动续期
- **缓存预热**: 启动时加载库存到 Redis

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/seckill/order | 秒杀下单（请求体: {userId, productId}）|
| GET  | /api/seckill/result?userId=&productId= | 轮询秒杀结果 |

## 启动

1. 创建 MySQL 数据库并执行建表 SQL
2. 安装并启动 RabbitMQ
3. 修改 application.yml 中的相关配置
4. 运行 SeckillApplication.java
