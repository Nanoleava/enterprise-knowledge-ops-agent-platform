package com.ljl.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "手工文档切片创建请求")
public class DocumentChunkCreateRequest {

    @Schema(description = "切片顺序，从 0 开始", example = "0")
    @NotNull(message = "切片序号不能为空")
    @Min(value = 0, message = "切片序号不能小于0")
    private Integer chunkIndex;

    @Schema(description = "切片文本内容", example = "第一段文本")
    @NotBlank(message = "切片内容不能为空")
    @Size(max = 1_000_000, message = "切片内容长度不能超过 1000000 个字符")
    private String content;

    @Schema(description = "可选 JSON 元数据", example = "{\"page\":1}")
    @Size(max = 65_535, message = "切片元数据长度不能超过 65535 个字符")
    private String metadata;

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
