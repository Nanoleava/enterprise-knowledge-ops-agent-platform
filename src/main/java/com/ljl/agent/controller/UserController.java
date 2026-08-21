package com.ljl.agent.controller;

import com.ljl.agent.common.Result;
import com.ljl.agent.dto.request.UserRegisterRequest;
import com.ljl.agent.dto.response.UserVO;
import com.ljl.agent.security.CurrentUser;
import com.ljl.agent.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户 HTTP 接口。
 *
 * <p>Controller 只接收参数、调用 Service、返回 Result。</p>
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户", description = "用户注册、查询和密码校验")
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    /**
     * 注册普通用户。
     */
    @PostMapping("/register")
    @Operation(summary = "注册普通用户")
    @SecurityRequirements
    public Result<UserVO> register(
            @Valid @RequestBody UserRegisterRequest request) {

        UserVO user = userService.register(request);
        return Result.success(user);
    }

    /**
     * 根据 ID 查询用户。
     */
    @GetMapping("/{id}")
    @Operation(summary = "按 ID 查询用户")
    public Result<UserVO> findById(
            @Parameter(description = "用户 ID", example = "1")
            @PathVariable Long id) {

        UserVO user = userService.findById(id);
        return Result.success(user);
    }

    /**
     * 查询全部用户。
     */
    @GetMapping
    @Operation(summary = "查询全部用户")
    public Result<List<UserVO>> findAll() {
        List<UserVO> users = userService.findAll();
        return Result.success(users);
    }

    /**
     * 查询当前 JWT 对应的本人信息。
     */
    @GetMapping("/me")
    @Operation(summary = "查询当前登录用户")
    public Result<UserVO> me(Authentication authentication) {
        return Result.success(
                userService.findById(
                        currentUser.requireUserId(authentication)
                )
        );
    }

}
