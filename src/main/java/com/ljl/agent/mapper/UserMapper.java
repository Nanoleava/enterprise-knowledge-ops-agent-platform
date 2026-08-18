package com.ljl.agent.mapper;

import com.ljl.agent.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户数据库访问接口。
 *
 * <p>只定义数据库操作，不写业务规则。</p>
 */
@Mapper
public interface UserMapper {

    int insert(User user);

    User selectById(@Param("id") Long id);

    User selectByUsername(@Param("username") String username);

    User selectByEmail(@Param("email") String email);

    List<User> selectAll();
}