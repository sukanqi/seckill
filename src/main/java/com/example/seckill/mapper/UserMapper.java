package com.example.seckill.mapper;

import com.example.seckill.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User selectByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);

    int insert(User user);
}
