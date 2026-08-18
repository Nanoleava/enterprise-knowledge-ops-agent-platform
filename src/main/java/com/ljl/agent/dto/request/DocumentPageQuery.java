package com.ljl.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "文档分页和动态条件查询")
public class DocumentPageQuery {

    @Schema(description = "页码，从 1 开始", example = "1", defaultValue = "1")
    @NotNull(message = "page不能为空")
    @Min(value = 1, message = "page不能小于1")
    private Integer page = 1;

    @Schema(description = "每页条数，最大 100", example = "10", defaultValue = "10")
    @NotNull(message = "size不能为空")
    @Min(value = 1, message = "size不能小于1")
    @Max(value = 100, message = "size不能超过100")
    private Integer size = 10;

    @Schema(description = "标题或正文关键字", example = "Java")
    @Size(max = 100, message = "keyword长度不能超过100")
    private String keyword;

    @Schema(description = "可选知识库 ID", example = "1")
    @Positive(message = "knowledgeBaseId必须为正数")
    private Long knowledgeBaseId;

    public long offset() {
        return (long) (page - 1) * size;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }
}
