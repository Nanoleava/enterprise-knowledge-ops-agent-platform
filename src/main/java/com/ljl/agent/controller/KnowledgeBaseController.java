package com.ljl.agent.controller;

import com.ljl.agent.common.Result;
import com.ljl.agent.dto.request.KnowledgeBaseCreateRequest;
import com.ljl.agent.dto.response.KnowledgeBaseVO;
import com.ljl.agent.security.CurrentUser;
import com.ljl.agent.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
@Validated
@Tag(name = "知识库", description = "知识库创建和查询")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final CurrentUser currentUser;

    public KnowledgeBaseController(
            KnowledgeBaseService knowledgeBaseService,
            CurrentUser currentUser) {

        this.knowledgeBaseService = knowledgeBaseService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Operation(summary = "创建知识库")
    public Result<KnowledgeBaseVO> create(
            @Valid
            @RequestBody
            KnowledgeBaseCreateRequest request,
            Authentication authentication) {

        KnowledgeBaseVO knowledgeBase =
                knowledgeBaseService.create(
                        currentUser.requireUserId(authentication),
                        request
                );

        return Result.success(knowledgeBase);
    }

    @GetMapping
    @Operation(summary = "查询当前用户的知识库")
    public Result<List<KnowledgeBaseVO>> list(
            Authentication authentication) {

        List<KnowledgeBaseVO> knowledgeBases =
                knowledgeBaseService.listByCurrentUser(
                        currentUser.requireUserId(authentication)
                );

        return Result.success(knowledgeBases);
    }

    @GetMapping("/{id}")
    @Operation(summary = "按 ID 查询知识库")
    public Result<KnowledgeBaseVO> getById(
            @Parameter(description = "知识库 ID", example = "1")
            @PathVariable
            @Positive(message = "知识库ID必须是正整数")
            Long id,
            Authentication authentication) {

        return Result.success(
                knowledgeBaseService.getById(
                        currentUser.requireUserId(authentication),
                        id
                )
        );
    }
}
