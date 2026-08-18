package com.ljl.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "文档创建请求")
public class DocumentCreateRequest {

    @Schema(description = "所属用户 ID", example = "1")
    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须是正整数")
    private Long userId;

    @Schema(description = "所属知识库 ID", example = "1")
    @NotNull(message = "知识库ID不能为空")
    @Positive(message = "知识库ID必须是正整数")
    private Long knowledgeBaseId;

    @Schema(description = "文档标题", example = "MyBatis 动态 SQL")
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题长度不能超过 200 个字符")
    private String title;

    @Schema(description = "文档正文", example = "动态 SQL 使用 where 和 if 组合条件。")
    @NotBlank(message = "文档内容不能为空")
    @Size(max = 1_000_000, message = "文档内容长度不能超过 1000000 个字符")
    private String content;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
