package com.ljl.agent.controller;

import com.ljl.agent.common.PageResult;
import com.ljl.agent.dto.request.DocumentChunkCreateRequest;
import com.ljl.agent.dto.request.DocumentCreateRequest;
import com.ljl.agent.dto.request.DocumentPageQuery;
import com.ljl.agent.dto.request.KnowledgeBaseCreateRequest;
import com.ljl.agent.dto.response.DocumentChunkVO;
import com.ljl.agent.dto.response.DocumentVO;
import com.ljl.agent.dto.response.KnowledgeBaseVO;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.exception.GlobalExceptionHandler;
import com.ljl.agent.security.CurrentUser;
import com.ljl.agent.service.DocumentService;
import com.ljl.agent.service.KnowledgeBaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgeDocumentControllerValidationTest {

    private KnowledgeBaseService knowledgeBaseService;
    private DocumentService documentService;
    private CurrentUser currentUser;
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        knowledgeBaseService = mock(KnowledgeBaseService.class);
        documentService = mock(DocumentService.class);
        currentUser = mock(CurrentUser.class);
        when(currentUser.requireUserId(any())).thenReturn(7L);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new KnowledgeBaseController(
                                knowledgeBaseService,
                                currentUser
                        ),
                        new DocumentController(documentService, currentUser)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void shouldReturnKnowledgeBaseFieldErrors() throws Exception {
        mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message")
                        .value("参数校验失败"))
                .andExpect(jsonPath("$.data.name")
                        .value("知识库名称不能为空"));

        verifyNoInteractions(knowledgeBaseService);
    }

    @Test
    void shouldReturnKnowledgeBaseDescriptionLengthError()
            throws Exception {
        String description = "x".repeat(501);
        String body = """
                {
                  "userId": 1,
                  "name": "Java资料",
                  "description": "%s"
                }
                """.formatted(description);

        mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.description").value(
                        "知识库描述长度不能超过 500 个字符"
                ));

        verifyNoInteractions(knowledgeBaseService);
    }

    @Test
    void shouldReturnDocumentFieldErrors() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "knowledgeBaseId": -1,
                                  "title": "   ",
                                  "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data.knowledgeBaseId")
                        .value("知识库ID必须是正整数"))
                .andExpect(jsonPath("$.data.title")
                        .value("文档标题不能为空"))
                .andExpect(jsonPath("$.data.content")
                        .value("文档内容不能为空"));

        verifyNoInteractions(documentService);
    }

    @Test
    void shouldMapDuplicateKnowledgeBaseToHttp409() throws Exception {
        when(knowledgeBaseService.create(
                eq(7L),
                any(KnowledgeBaseCreateRequest.class)
        )).thenThrow(new BusinessException(
                40902,
                "该用户下已存在同名知识库"
        ));

        mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "name": "Java资料"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));
    }

    @Test
    void shouldIgnoreForgedOwnerAndPassPrincipalUserIdToService()
            throws Exception {
        KnowledgeBaseVO created = new KnowledgeBaseVO();
        created.setId(5L);
        created.setUserId(7L);
        created.setName("Java资料");
        when(knowledgeBaseService.create(
                eq(7L),
                any(KnowledgeBaseCreateRequest.class)
        )).thenReturn(created);

        mockMvc.perform(post("/api/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 999,
                                  "name": "Java资料"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7));

        verify(knowledgeBaseService).create(
                eq(7L),
                any(KnowledgeBaseCreateRequest.class)
        );
    }

    @Test
    void shouldMapOwnershipFailureToHttp403() throws Exception {
        when(documentService.create(
                eq(7L),
                any(DocumentCreateRequest.class)
        )).thenThrow(new BusinessException(
                40302,
                "知识库不属于当前用户"
        ));

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 2,
                                  "knowledgeBaseId": 10,
                                  "title": "入门",
                                  "content": "正文"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40302));
    }

    @Test
    void shouldReturnDocumentChunkFieldErrors() throws Exception {
        mockMvc.perform(post("/api/documents/10/chunks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chunkIndex": -1,
                                  "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message")
                        .value("参数校验失败"))
                .andExpect(jsonPath("$.data.chunkIndex")
                        .value("切片序号不能小于0"))
                .andExpect(jsonPath("$.data.content")
                        .value("切片内容不能为空"));

        verifyNoInteractions(documentService);
    }

    @Test
    void shouldMapMissingDocumentForChunkToHttp404() throws Exception {
        when(documentService.createChunk(
                eq(7L),
                eq(10L),
                any(DocumentChunkCreateRequest.class)
        )).thenThrow(new BusinessException(
                40403,
                "文档不存在，documentId=10"
        ));

        mockMvc.perform(post("/api/documents/10/chunks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chunkIndex": 0,
                                  "content": "正文"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40403));
    }

    @Test
    void shouldMapDuplicateChunkToHttp409() throws Exception {
        when(documentService.createChunk(
                eq(7L),
                eq(10L),
                any(DocumentChunkCreateRequest.class)
        )).thenThrow(new BusinessException(
                40904,
                "该文档下已存在相同序号的切片"
        ));

        mockMvc.perform(post("/api/documents/10/chunks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chunkIndex": 0,
                                  "content": "正文"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40904));
    }

    @Test
    void shouldReturnCreatedDocumentChunk() throws Exception {
        DocumentChunkVO chunk = chunk(21L, 10L, 5L, 0, "第一段");
        when(documentService.createChunk(
                eq(7L),
                eq(10L),
                any(DocumentChunkCreateRequest.class)
        )).thenReturn(chunk);

        mockMvc.perform(post("/api/documents/10/chunks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chunkIndex": 0,
                                  "content": "第一段",
                                  "metadata": "{\\"page\\":1}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.documentId").value(10))
                .andExpect(jsonPath("$.data.knowledgeBaseId").value(5))
                .andExpect(jsonPath("$.data.chunkIndex").value(0))
                .andExpect(jsonPath("$.data.content").value("第一段"));
    }

    @Test
    void shouldReturnDocumentChunksInServiceOrder() throws Exception {
        when(documentService.listChunksByDocumentId(7L, 10L))
                .thenReturn(List.of(
                        chunk(21L, 10L, 5L, 0, "第一段"),
                        chunk(22L, 10L, 5L, 1, "第二段")
                ));

        mockMvc.perform(get("/api/documents/10/chunks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].chunkIndex").value(0))
                .andExpect(jsonPath("$.data[1].chunkIndex").value(1));
    }

    @Test
    void shouldReturnKnowledgeBaseById() throws Exception {
        KnowledgeBaseVO knowledgeBase = new KnowledgeBaseVO();
        knowledgeBase.setId(5L);
        knowledgeBase.setUserId(2L);
        knowledgeBase.setName("Java资料");
        when(knowledgeBaseService.getById(7L, 5L))
                .thenReturn(knowledgeBase);

        mockMvc.perform(get("/api/knowledge-bases/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Java资料"));
    }

    @Test
    void shouldReturnDocumentById() throws Exception {
        when(documentService.getById(7L, 10L))
                .thenReturn(document(10L, 5L, "分页设计"));

        mockMvc.perform(get("/api/documents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.knowledgeBaseId").value(5))
                .andExpect(jsonPath("$.data.title").value("分页设计"));
    }

    @Test
    void shouldDeleteDocumentById() throws Exception {
        mockMvc.perform(delete("/api/documents/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(documentService).deleteById(7L, 10L);
    }

    @Test
    void shouldReturnPaginatedDocumentsFromDocumentsEndpoint()
            throws Exception {
        PageResult<DocumentVO> page = new PageResult<>(
                List.of(document(10L, 5L, "Java 分页")),
                12,
                2,
                5
        );
        when(documentService.page(
                eq(7L),
                any(DocumentPageQuery.class)
        ))
                .thenReturn(page);

        mockMvc.perform(get("/api/documents")
                        .queryParam("page", "2")
                        .queryParam("size", "5")
                        .queryParam("keyword", "Java")
                        .queryParam("knowledgeBaseId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.total").value(12))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].title")
                        .value("Java 分页"));
    }

    @Test
    void shouldValidateDocumentPageParameters() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .queryParam("page", "0")
                        .queryParam("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data.page")
                        .value("page不能小于1"))
                .andExpect(jsonPath("$.data.size")
                        .value("size不能超过100"));

        verifyNoInteractions(documentService);
    }

    private DocumentChunkVO chunk(
            Long id,
            Long documentId,
            Long knowledgeBaseId,
            Integer chunkIndex,
            String content
    ) {
        DocumentChunkVO chunk = new DocumentChunkVO();
        chunk.setId(id);
        chunk.setDocumentId(documentId);
        chunk.setKnowledgeBaseId(knowledgeBaseId);
        chunk.setChunkIndex(chunkIndex);
        chunk.setContent(content);
        return chunk;
    }

    private DocumentVO document(
            Long id,
            Long knowledgeBaseId,
            String title
    ) {
        DocumentVO document = new DocumentVO();
        document.setId(id);
        document.setUserId(2L);
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(title);
        document.setContent("正文");
        document.setStatus("ACTIVE");
        return document;
    }
}
