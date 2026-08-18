package com.ljl.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "知识库创建请求")
public class KnowledgeBaseCreateRequest {

    @Schema(description = "所属用户 ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须是正整数")
    private Long userId;

    @Schema(description = "知识库名称", example = "Java 求职资料")
    @NotBlank(message = "知识库名称不能为空")
    @Size(max = 100, message = "知识库名称长度不能超过 100 个字符")
    private String name;

    @Schema(description = "知识库描述", example = "Java 后端学习笔记")
    @Size(max = 500, message = "知识库描述长度不能超过 500 个字符")
    private String description;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null
                ? null
                : description.trim();
    }
}
