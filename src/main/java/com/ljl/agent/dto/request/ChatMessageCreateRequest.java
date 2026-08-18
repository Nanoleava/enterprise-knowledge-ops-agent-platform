package com.ljl.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "聊天消息创建请求")
public class ChatMessageCreateRequest {

    @Schema(description = "消息角色", allowableValues = {"USER", "ASSISTANT"}, example = "USER")
    @NotBlank(message = "消息角色不能为空")
    @Pattern(
            regexp = "(?i)^(USER|ASSISTANT)$",
            message = "消息角色只能是 USER 或 ASSISTANT"
    )
    private String role;

    @Schema(description = "消息正文", example = "请解释 MyBatis 动态 SQL")
    @NotBlank(message = "消息内容不能为空")
    private String content;

    @Schema(description = "客户端幂等请求 ID", example = "req-20260814-001")
    @NotBlank(message = "requestId 不能为空")
    @Size(max = 64, message = "requestId 长度不能超过 64")
    private String requestId;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
