package com.ljl.agent.controller;

import com.ljl.agent.common.Result;
import com.ljl.agent.dto.request.DocumentParseRequest;
import com.ljl.agent.dto.response.DocumentProcessStatusVO;
import com.ljl.agent.dto.response.DocumentUploadResponse;
import com.ljl.agent.ingestion.DocumentIngestionService;
import com.ljl.agent.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "文档摄取", description = "文件上传、TXT/Markdown 解析与处理状态")
public class DocumentUploadController {

    private final DocumentIngestionService ingestionService;
    private final CurrentUser currentUser;

    public DocumentUploadController(
            DocumentIngestionService ingestionService,
            CurrentUser currentUser
    ) {
        this.ingestionService = ingestionService;
        this.currentUser = currentUser;
    }

    @PostMapping(
            value = "/knowledge-bases/{knowledgeBaseId}/documents/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "上传 TXT 或 Markdown 文档")
    public Result<DocumentUploadResponse> upload(
            @Parameter(description = "知识库 ID", example = "1")
            @PathVariable
            @Positive(message = "知识库ID必须是正整数")
            Long knowledgeBaseId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false)
            @Size(max = 200, message = "文档标题长度不能超过200个字符")
            String title,
            Authentication authentication
    ) {
        return Result.success(ingestionService.upload(
                currentUser.requireUserId(authentication),
                knowledgeBaseId,
                file,
                title
        ));
    }

    @PostMapping("/documents/{documentId}/parse")
    @Operation(summary = "解析或重试解析上传文档")
    public Result<DocumentProcessStatusVO> parse(
            @Parameter(description = "文档 ID", example = "1")
            @PathVariable
            @Positive(message = "文档ID必须是正整数")
            Long documentId,
            @RequestBody(required = false) DocumentParseRequest request,
            Authentication authentication
    ) {
        boolean force = request != null && request.forceEnabled();
        return Result.success(ingestionService.parse(
                currentUser.requireUserId(authentication),
                documentId,
                force
        ));
    }

    @GetMapping("/documents/{documentId}/processing-status")
    @Operation(summary = "查询文档处理状态")
    public Result<DocumentProcessStatusVO> processingStatus(
            @Parameter(description = "文档 ID", example = "1")
            @PathVariable
            @Positive(message = "文档ID必须是正整数")
            Long documentId,
            Authentication authentication
    ) {
        return Result.success(ingestionService.getStatus(
                currentUser.requireUserId(authentication),
                documentId
        ));
    }
}
