package com.nesting.assistant.domain.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nesting.assistant.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Select("SELECT * FROM users WHERE user_id = #{userId}")
    User findByUserId(@Param("userId") String userId);
}
