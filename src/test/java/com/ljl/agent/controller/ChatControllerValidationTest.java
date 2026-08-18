package com.ljl.agent.controller;

import com.ljl.agent.dto.request.ChatMessageCreateRequest;
import com.ljl.agent.dto.response.ChatMessageVO;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.exception.GlobalExceptionHandler;
import com.ljl.agent.service.ChatService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerValidationTest {

    private ChatService chatService;
    private LocalValidatorFactoryBean validator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatController(chatService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void shouldReturnSessionFieldErrors() throws Exception {
        mockMvc.perform(post("/api/chat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 0,
                                  "title": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("参数校验失败"))
                .andExpect(jsonPath("$.data.userId")
                        .value("用户 ID 必须大于 0"))
                .andExpect(jsonPath("$.data.title")
                        .value("会话标题不能为空"));

        verifyNoInteractions(chatService);
    }

    @Test
    void shouldReturnMessageFieldErrors() throws Exception {
        mockMvc.perform(post("/api/chat/sessions/20/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "SYSTEM",
                                  "content": "   ",
                                  "requestId": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data.role")
                        .value("消息角色只能是 USER 或 ASSISTANT"))
                .andExpect(jsonPath("$.data.content")
                        .value("消息内容不能为空"))
                .andExpect(jsonPath("$.data.requestId")
                        .value("requestId 不能为空"));

        verifyNoInteractions(chatService);
    }

    @Test
    void shouldExplainMissingUserIdQueryParameter() throws Exception {
        mockMvc.perform(get("/api/chat/sessions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message")
                        .value("缺少必要参数：userId"));

        verifyNoInteractions(chatService);
    }

    @Test
    void shouldMapMissingSessionToHttp404() throws Exception {
        when(chatService.listMessages(20L)).thenThrow(
                new BusinessException(
                        40404,
                        "聊天会话不存在，sessionId=20"
                )
        );

        mockMvc.perform(get("/api/chat/sessions/20/messages"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40404))
                .andExpect(jsonPath("$.message")
                        .value("聊天会话不存在，sessionId=20"));
    }

    @Test
    void shouldMapDuplicateRequestIdToHttp409() throws Exception {
        when(chatService.createMessage(
                eq(20L),
                any(ChatMessageCreateRequest.class)
        )).thenThrow(new BusinessException(
                40905,
                "requestId 已存在：request-1"
        ));

        mockMvc.perform(post("/api/chat/sessions/20/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "USER",
                                  "content": "你好",
                                  "requestId": "request-1"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40905));
    }

    @Test
    void shouldReturnCreatedMessageUsingUnifiedResult() throws Exception {
        ChatMessageVO message = new ChatMessageVO();
        message.setId(30L);
        message.setSessionId(20L);
        message.setUserId(7L);
        message.setRole("USER");
        message.setContent("你好");
        message.setRequestId("request-1");
        when(chatService.createMessage(
                eq(20L),
                any(ChatMessageCreateRequest.class)
        )).thenReturn(message);

        mockMvc.perform(post("/api/chat/sessions/20/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "USER",
                                  "content": "你好",
                                  "requestId": "request-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(30))
                .andExpect(jsonPath("$.data.sessionId").value(20))
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.requestId")
                        .value("request-1"));
    }
}
