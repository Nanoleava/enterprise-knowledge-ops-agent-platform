package com.ljl.agent.controller;

import com.ljl.agent.common.PageResult;
import com.ljl.agent.common.Result;
import com.ljl.agent.dto.request.DocumentChunkCreateRequest;
import com.ljl.agent.dto.request.DocumentCreateRequest;
import com.ljl.agent.dto.request.DocumentPageQuery;
import com.ljl.agent.dto.response.DocumentChunkVO;
import com.ljl.agent.dto.response.DocumentVO;
import com.ljl.agent.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Validated
@Tag(name = "文档与切片", description = "文档 CRUD、分页和手工文本切片")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(
            DocumentService documentService) {

        this.documentService = documentService;
    }

    @PostMapping("/documents")
    @Operation(summary = "创建文档")
    public Result<DocumentVO> create(
            @Valid
            @RequestBody
            DocumentCreateRequest request) {

        DocumentVO document =
                documentService.create(request);

        return Result.success(document);
    }

    @GetMapping("/documents")
    @Operation(
            summary = "分页查询文档",
            description = "支持 page、size、keyword、knowledgeBaseId 动态条件"
    )
    public Result<PageResult<DocumentVO>> page(
            @Valid @ModelAttribute DocumentPageQuery query) {

        return Result.success(documentService.page(query));
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    @Operation(summary = "查询知识库下的文档")
    public Result<List<DocumentVO>> listByKnowledgeBaseId(
            @Parameter(description = "知识库 ID", example = "1")
            @PathVariable Long knowledgeBaseId) {

        List<DocumentVO> documents =
                documentService.listByKnowledgeBaseId(
                        knowledgeBaseId
                );

        return Result.success(documents);
    }

    @PostMapping("/documents/{documentId}/chunks")
    @Operation(summary = "为文档创建手工文本切片")
    public Result<DocumentChunkVO> createChunk(
            @Parameter(description = "文档 ID", example = "1")
            @PathVariable Long documentId,
            @Valid @RequestBody DocumentChunkCreateRequest request
    ) {
        DocumentChunkVO chunk = documentService.createChunk(
                documentId,
                request
        );

        return Result.success(chunk);
    }

    @GetMapping("/documents/{documentId}/chunks")
    @Operation(summary = "查询文档切片")
    public Result<List<DocumentChunkVO>> listChunksByDocumentId(
            @Parameter(description = "文档 ID", example = "1")
            @PathVariable Long documentId
    ) {
        List<DocumentChunkVO> chunks =
                documentService.listChunksByDocumentId(documentId);

        return Result.success(chunks);
    }

    @GetMapping("/documents/{id}")
    @Operation(summary = "按 ID 查询文档")
    public Result<DocumentVO> getById(
            @Parameter(description = "文档 ID", example = "1")
            @PathVariable
            @Positive(message = "文档ID必须是正整数")
            Long id) {

        return Result.success(
                documentService.getById(id)
        );
    }

    @DeleteMapping("/documents/{id}")
    @Operation(
            summary = "删除文档",
            description = "在事务中删除文档，由数据库外键级联清理 document_chunk"
    )
    public Result<Void> deleteById(
            @Parameter(description = "文档 ID", example = "1")
            @PathVariable
            @Positive(message = "文档ID必须是正整数")
            Long id) {

        documentService.deleteById(id);

        return Result.success(null);
    }
}
