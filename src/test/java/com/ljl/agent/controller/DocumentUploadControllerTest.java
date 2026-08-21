package com.ljl.agent.controller;

import com.ljl.agent.dto.response.DocumentProcessStatusVO;
import com.ljl.agent.dto.response.DocumentUploadResponse;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.exception.GlobalExceptionHandler;
import com.ljl.agent.ingestion.DocumentIngestionService;
import com.ljl.agent.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentUploadControllerTest {

    private DocumentIngestionService ingestionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ingestionService = mock(DocumentIngestionService.class);
        CurrentUser currentUser = mock(CurrentUser.class);
        when(currentUser.requireUserId(any())).thenReturn(7L);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new DocumentUploadController(
                                ingestionService,
                                currentUser
                        )
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldUploadMultipartWithPrincipalOwner() throws Exception {
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setDocumentId(30L);
        response.setOriginalFileName("guide.txt");
        response.setFileType("TXT");
        response.setFileSize(6L);
        response.setParseStatus("PENDING");
        response.setChunkStatus("PENDING");
        when(ingestionService.upload(eq(7L), eq(9L), any(), eq("指南")))
                .thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "guide.txt",
                "text/plain",
                "正文".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart(
                                "/api/knowledge-bases/9/documents/upload"
                        )
                        .file(file)
                        .param("title", "指南"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.documentId").value(30))
                .andExpect(jsonPath("$.data.parseStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.filePath").doesNotExist());

        verify(ingestionService).upload(
                eq(7L),
                eq(9L),
                any(),
                eq("指南")
        );
    }

    @Test
    void shouldParseAndQuerySafeStatus() throws Exception {
        DocumentProcessStatusVO statusVO = statusVO("SUCCESS", null);
        when(ingestionService.parse(7L, 30L, true)).thenReturn(statusVO);
        when(ingestionService.getStatus(7L, 30L)).thenReturn(statusVO);

        mockMvc.perform(post("/api/documents/30/parse")
                        .contentType("application/json")
                        .content("{\"force\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.parseStatus").value("SUCCESS"));

        mockMvc.perform(get("/api/documents/30/processing-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(30))
                .andExpect(jsonPath("$.data.chunkCount").value(0))
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andExpect(jsonPath("$.data.content").doesNotExist());
    }

    @Test
    void shouldMapParseFailureAndProcessingConflict() throws Exception {
        when(ingestionService.parse(7L, 30L, false))
                .thenThrow(new BusinessException(42201, "文档为空"));
        when(ingestionService.getStatus(7L, 31L))
                .thenThrow(new BusinessException(40304, "文档不属于当前用户"));

        mockMvc.perform(post("/api/documents/30/parse")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(42201));

        mockMvc.perform(get("/api/documents/31/processing-status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40304));
    }

    @Test
    void shouldRejectMultipartRequestWithoutFilePart() throws Exception {
        mockMvc.perform(multipart(
                        "/api/knowledge-bases/9/documents/upload"
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));
    }

    private DocumentProcessStatusVO statusVO(
            String parseStatus,
            String error
    ) {
        DocumentProcessStatusVO statusVO = new DocumentProcessStatusVO();
        statusVO.setDocumentId(30L);
        statusVO.setParseStatus(parseStatus);
        statusVO.setChunkStatus("PENDING");
        statusVO.setChunkCount(0);
        statusVO.setErrorMessage(error);
        return statusVO;
    }
}
