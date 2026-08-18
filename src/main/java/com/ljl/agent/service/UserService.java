package com.ljl.agent.service;

import com.ljl.agent.dto.request.UserRegisterRequest;
import com.ljl.agent.dto.response.UserVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 用户业务接口。
 */
public interface UserService {

    /**
     * 注册普通用户。
     */
    UserVO register(
            @NotNull(message = "注册参数不能为空")
            @Valid UserRegisterRequest request
    );

    /**
     * 根据用户 ID 查询用户。
     */
    UserVO findById(Long id);

    /**
     * 查询全部用户。
     */
    List<UserVO> findAll();

}
