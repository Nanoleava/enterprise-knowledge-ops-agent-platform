package com.ljl.agent.service;

import com.ljl.agent.dto.request.KnowledgeBaseCreateRequest;
import com.ljl.agent.dto.response.KnowledgeBaseVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public interface KnowledgeBaseService {

    KnowledgeBaseVO create(
            @NotNull(message = "当前用户ID不能为空")
            @Positive(message = "当前用户ID必须是正整数")
            Long currentUserId,
            @NotNull(message = "知识库创建参数不能为空")
            @Valid KnowledgeBaseCreateRequest request
    );

    List<KnowledgeBaseVO> listByCurrentUser(
            @NotNull(message = "当前用户ID不能为空")
            @Positive(message = "当前用户ID必须是正整数")
            Long currentUserId
    );

    KnowledgeBaseVO getById(
            @NotNull(message = "当前用户ID不能为空")
            @Positive(message = "当前用户ID必须是正整数")
            Long currentUserId,
            @NotNull(message = "知识库ID不能为空")
            @Positive(message = "知识库ID必须是正整数")
            Long id
    );
}
