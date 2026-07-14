package com.example.seckill.mapper;

import com.example.seckill.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeckillOrderMapper {

    int insert(SeckillOrder seckillOrder);

    SeckillOrder selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    SeckillOrder selectByOrderId(@Param("orderId") String orderId);

    List<SeckillOrder> selectByUserId(@Param("userId") Long userId);
}
