package com.ljl.agent.service;

import com.ljl.agent.common.PageResult;
import com.ljl.agent.dto.request.DocumentChunkCreateRequest;
import com.ljl.agent.dto.request.DocumentCreateRequest;
import com.ljl.agent.dto.request.DocumentPageQuery;
import com.ljl.agent.dto.response.DocumentChunkVO;
import com.ljl.agent.dto.response.DocumentVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public interface DocumentService {

    DocumentVO create(
            @NotNull(message = "当前用户ID不能为空")
            @Positive(message = "当前用户ID必须是正整数")
            Long currentUserId,
            @NotNull(message = "文档创建参数不能为空")
            @Valid DocumentCreateRequest request
    );

    List<DocumentVO> listByKnowledgeBaseId(
            @NotNull(message = "当前用户ID不能为空")
            @Positive(message = "当前用户ID必须是正整数")
            Long currentUserId,
            @NotNull(message = "知识库ID不能为空")
            @Positive(message = "知识库ID必须是正整数")
            Long knowledgeBaseId
    );

    DocumentChunkVO createChunk(
            @NotNull(message = "当前用户ID不能为空")
            @Positive(message = "当前用户ID必须是正整数")
            Long currentUserId,
            @NotNull(message = "文档ID不能为空")
            @Positive(message = "文档ID必须是正整数")
            Long documentId,
            @NotNull(message = "文档切片创建参数不能为空")
            @Valid DocumentChunkCreateRequest request
    );

    List<DocumentChunkVO> listChunksByDocumentId(
            @NotNull(message = "当前用户ID不能为空")
            @Positive(message = "当前用户ID必须是正整数")
            Long currentUserId,
            @NotNull(message = "文档ID不能为空")
            @Positive(message = "文档ID必须是正整数")
            Long documentId
    );

    DocumentVO getById(
            @NotNull(message = "当前用户ID不能为空")
            @Positive(message = "当前用户ID必须是正整数")
            Long currentUserId,
            @NotNull(message = "文档ID不能为空")
            @Positive(message = "文档ID必须是正整数")
            Long id
    );

    void deleteById(
            @NotNull(message = "当前用户ID不能为空")
            @Positive(message = "当前用户ID必须是正整数")
            Long currentUserId,
            @NotNull(message = "文档ID不能为空")
            @Positive(message = "文档ID必须是正整数")
            Long id
    );

    PageResult<DocumentVO> page(
            @NotNull(message = "当前用户ID不能为空")
            @Positive(message = "当前用户ID必须是正整数")
            Long currentUserId,
            @NotNull(message = "分页查询参数不能为空")
            @Valid DocumentPageQuery query
    );
}
