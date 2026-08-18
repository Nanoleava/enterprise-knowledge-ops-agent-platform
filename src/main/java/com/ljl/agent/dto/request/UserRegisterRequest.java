package com.ljl.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求 DTO。
 */
@Schema(description = "用户注册请求")
public class UserRegisterRequest {

    @Schema(description = "唯一用户名", example = "java_user")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在 3 到 50 个字符之间")
    @Pattern(
            regexp = "^[\\p{L}\\p{N}_]+$",
            message = "用户名只能包含中文、字母、数字和下划线"
    )
    private String username;

    @Schema(
            description = "原始密码，仅用于请求输入",
            format = "password",
            accessMode = Schema.AccessMode.WRITE_ONLY
    )
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 64, message = "密码长度必须在 8 到 64 个字符之间")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S+$",
            message = "密码必须包含字母和数字，且不能包含空白字符"
    )
    private String password;

    // 邮箱为可选字段；填写时才校验格式和长度。
    @Schema(description = "可选邮箱", example = "java_user@example.com")
    @Email(message = "邮箱格式不正确")
    @Size(max = 255, message = "邮箱长度不能超过 255 个字符")
    private String email;

    public UserRegisterRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }
}
