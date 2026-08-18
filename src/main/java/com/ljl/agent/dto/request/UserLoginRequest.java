package com.ljl.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "账号密码校验请求")
public class UserLoginRequest {

    @Schema(description = "用户名", example = "java_user")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    @Schema(
            description = "原始密码",
            format = "password",
            accessMode = Schema.AccessMode.WRITE_ONLY
    )
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码长度不能超过64")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
