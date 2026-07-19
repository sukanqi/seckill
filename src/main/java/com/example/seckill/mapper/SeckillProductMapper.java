package com.example.seckill.mapper;

import com.example.seckill.entity.SeckillProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SeckillProductMapper {

    List<SeckillProduct> selectAll();

    SeckillProduct selectById(@Param("id") Long id);

    SeckillProduct selectByProductId(@Param("productId") Long productId);

    int updateStockCount(@Param("productId") Long productId, @Param("stockCount") Integer stockCount);

    int decrementStock(@Param("productId") Long productId);

    int incrementStock(@Param("productId") Long productId);

    int insert(SeckillProduct seckillProduct);

    int updateByPrimaryKey(SeckillProduct seckillProduct);

    int deleteByPrimaryKey(@Param("id") Long id);

    int countLowStock(@Param("threshold") Integer threshold);
}
