package com.example.taskmanager.mapper;

import com.example.taskmanager.entity.UserAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper {

    @Select("SELECT id, username, password_hash, created_at FROM users WHERE username = #{username}")
    UserAccount findByUsername(@Param("username") String username);

    @Select("SELECT id, username, password_hash, created_at FROM users WHERE id = #{id}")
    UserAccount findById(@Param("id") Long id);

    @Insert("INSERT INTO users (username, password_hash) VALUES (#{username}, #{passwordHash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAccount user);
}
