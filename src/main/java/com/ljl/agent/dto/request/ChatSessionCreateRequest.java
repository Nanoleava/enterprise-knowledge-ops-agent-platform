package com.ljl.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "聊天会话创建请求")
public class ChatSessionCreateRequest {

    @Schema(description = "所属用户 ID", example = "1")
    @NotNull(message = "用户 ID 不能为空")
    @Positive(message = "用户 ID 必须大于 0")
    private Long userId;

    @Schema(description = "会话标题", example = "Java 面试复习")
    @NotBlank(message = "会话标题不能为空")
    @Size(max = 200, message = "会话标题长度不能超过 200")
    private String title;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
