package com.ljl.agent.controller;

import com.ljl.agent.auth.AuthService;
import com.ljl.agent.common.Result;
import com.ljl.agent.dto.request.UserLoginRequest;
import com.ljl.agent.dto.response.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 唯一标准登录入口。
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证", description = "用户名密码认证和 JWT 签发")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "登录并签发 Bearer JWT")
    @SecurityRequirements
    public Result<LoginResponse> login(
            @Valid @RequestBody UserLoginRequest request
    ) {
        return Result.success(authService.login(request));
    }
}
