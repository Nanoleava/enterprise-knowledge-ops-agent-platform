package com.ljl.agent.service;

import com.ljl.agent.dto.request.ChatMessageCreateRequest;
import com.ljl.agent.dto.request.ChatSessionCreateRequest;
import com.ljl.agent.dto.response.ChatMessageVO;
import com.ljl.agent.dto.response.ChatSessionVO;
import com.ljl.agent.entity.ChatMessage;
import com.ljl.agent.entity.ChatSession;
import com.ljl.agent.entity.User;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.mapper.ChatMessageMapper;
import com.ljl.agent.mapper.ChatSessionMapper;
import com.ljl.agent.mapper.UserMapper;
import com.ljl.agent.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class ChatServiceImplTest {

    private UserMapper userMapper;
    private ChatSessionMapper chatSessionMapper;
    private ChatMessageMapper chatMessageMapper;
    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        chatSessionMapper = mock(ChatSessionMapper.class);
        chatMessageMapper = mock(ChatMessageMapper.class);
        service = new ChatServiceImpl(
                userMapper,
                chatSessionMapper,
                chatMessageMapper
        );
    }

    @Test
    void shouldNormalizeAndCreateSessionForEnabledUser(
            CapturedOutput output
    ) {
        ChatSessionCreateRequest request = sessionRequest(
                7L,
                "  Java Agent  "
        );
        AtomicReference<ChatSession> inserted = new AtomicReference<>();
        when(userMapper.selectById(7L))
                .thenReturn(user(7L, User.STATUS_ENABLED));
        when(chatSessionMapper.insert(any(ChatSession.class)))
                .thenAnswer(invocation -> {
                    ChatSession value = invocation.getArgument(0);
                    value.setId(20L);
                    inserted.set(value);
                    return 1;
                });
        when(chatSessionMapper.selectById(20L)).thenAnswer(invocation -> {
            ChatSession value = inserted.get();
            value.setCreatedAt(LocalDateTime.now());
            value.setUpdatedAt(LocalDateTime.now());
            return value;
        });

        ChatSessionVO result = service.createSession(request);

        assertEquals(20L, result.getId());
        assertEquals(7L, result.getUserId());
        assertEquals("Java Agent", result.getTitle());
        assertEquals("Java Agent", inserted.get().getTitle());
        assertTrue(output.getAll().contains(
                "chat session created: sessionId=20, userId=7"
        ));
    }

    @Test
    void shouldRejectSessionCreationForDisabledUser() {
        when(userMapper.selectById(7L))
                .thenReturn(user(7L, User.STATUS_DISABLED));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createSession(sessionRequest(7L, "会话"))
        );

        assertEquals(40301, exception.getCode());
        verifyNoInteractions(chatSessionMapper, chatMessageMapper);
    }

    @Test
    void shouldReportSessionInsertFailure() {
        when(userMapper.selectById(7L))
                .thenReturn(user(7L, User.STATUS_ENABLED));
        when(chatSessionMapper.insert(any(ChatSession.class)))
                .thenReturn(0);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createSession(sessionRequest(7L, "会话"))
        );

        assertEquals(50005, exception.getCode());
        assertEquals("聊天会话创建失败", exception.getMessage());
    }

    @Test
    void shouldCreateMessageWithSessionOwnerAndPreserveContent(
            CapturedOutput output
    ) {
        String content = "  第一行\n    code();\n";
        AtomicReference<ChatMessage> inserted = new AtomicReference<>();
        when(chatSessionMapper.selectById(20L))
                .thenReturn(session(20L, 7L, "会话"));
        when(userMapper.selectById(7L))
                .thenReturn(user(7L, User.STATUS_ENABLED));
        when(chatMessageMapper.insert(any(ChatMessage.class)))
                .thenAnswer(invocation -> {
                    ChatMessage value = invocation.getArgument(0);
                    value.setId(30L);
                    inserted.set(value);
                    return 1;
                });
        when(chatSessionMapper.touchUpdatedAt(20L)).thenReturn(1);
        when(chatMessageMapper.selectById(30L)).thenAnswer(invocation -> {
            ChatMessage value = inserted.get();
            value.setCreatedAt(LocalDateTime.now());
            value.setUpdatedAt(LocalDateTime.now());
            return value;
        });

        ChatMessageVO result = service.createMessage(
                20L,
                messageRequest(" assistant ", content, " request-1 ")
        );

        ChatMessage stored = inserted.get();
        assertEquals(20L, stored.getSessionId());
        assertEquals(7L, stored.getUserId());
        assertEquals(ChatMessage.ROLE_ASSISTANT, stored.getRole());
        assertEquals(content, stored.getContent());
        assertEquals("request-1", stored.getRequestId());
        assertEquals(content, result.getContent());
        verify(chatSessionMapper).touchUpdatedAt(20L);
        assertTrue(output.getAll().contains(
                "chat message saved: messageId=30, sessionId=20, userId=7, role=ASSISTANT"
        ));
        assertFalse(output.getAll().contains(content));
    }

    @Test
    void shouldRejectMessageForMissingSession() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createMessage(
                        20L,
                        messageRequest("USER", "内容", "request-1")
                )
        );

        assertEquals(40404, exception.getCode());
        assertEquals(
                "聊天会话不存在，sessionId=20",
                exception.getMessage()
        );
        verifyNoInteractions(userMapper, chatMessageMapper);
    }

    @Test
    void shouldRejectDuplicateRequestIdBeforeInsert() {
        when(chatSessionMapper.selectById(20L))
                .thenReturn(session(20L, 7L, "会话"));
        when(userMapper.selectById(7L))
                .thenReturn(user(7L, User.STATUS_ENABLED));
        when(chatMessageMapper.selectByRequestId("request-1"))
                .thenReturn(message(30L, 20L, 7L, "request-1"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createMessage(
                        20L,
                        messageRequest("USER", "内容", "request-1")
                )
        );

        assertEquals(40905, exception.getCode());
        assertEquals("requestId 已存在：request-1", exception.getMessage());
    }

    @Test
    void shouldPreserveConcurrentDuplicateAsBusinessCause() {
        when(chatSessionMapper.selectById(20L))
                .thenReturn(session(20L, 7L, "会话"));
        when(userMapper.selectById(7L))
                .thenReturn(user(7L, User.STATUS_ENABLED));
        when(chatMessageMapper.insert(any(ChatMessage.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.createMessage(
                        20L,
                        messageRequest("USER", "内容", "request-1")
                )
        );

        assertEquals(40905, exception.getCode());
        assertInstanceOf(DuplicateKeyException.class, exception.getCause());
    }

    @Test
    void shouldListMessagesOnlyForExistingSession() {
        when(chatSessionMapper.selectById(20L))
                .thenReturn(session(20L, 7L, "会话"));
        when(chatMessageMapper.selectBySessionId(20L))
                .thenReturn(List.of(
                        message(30L, 20L, 7L, "request-1"),
                        message(31L, 20L, 7L, "request-2")
                ));

        List<ChatMessageVO> result = service.listMessages(20L);

        assertEquals(2, result.size());
        assertEquals(30L, result.getFirst().getId());
        assertEquals(31L, result.getLast().getId());
    }

    @Test
    void writeMethodsShouldHaveTransactionBoundaries() throws Exception {
        Transactional createSession = ChatServiceImpl.class
                .getMethod("createSession", ChatSessionCreateRequest.class)
                .getAnnotation(Transactional.class);
        Transactional createMessage = ChatServiceImpl.class
                .getMethod(
                        "createMessage",
                        Long.class,
                        ChatMessageCreateRequest.class
                )
                .getAnnotation(Transactional.class);

        assertNotNull(createSession);
        assertNotNull(createMessage);
    }

    private ChatSessionCreateRequest sessionRequest(Long userId, String title) {
        ChatSessionCreateRequest request = new ChatSessionCreateRequest();
        request.setUserId(userId);
        request.setTitle(title);
        return request;
    }

    private ChatMessageCreateRequest messageRequest(
            String role,
            String content,
            String requestId
    ) {
        ChatMessageCreateRequest request = new ChatMessageCreateRequest();
        request.setRole(role);
        request.setContent(content);
        request.setRequestId(requestId);
        return request;
    }

    private User user(Long id, int status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    private ChatSession session(Long id, Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setId(id);
        session.setUserId(userId);
        session.setTitle(title);
        return session;
    }

    private ChatMessage message(
            Long id,
            Long sessionId,
            Long userId,
            String requestId
    ) {
        ChatMessage message = new ChatMessage();
        message.setId(id);
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(ChatMessage.ROLE_USER);
        message.setContent("内容");
        message.setRequestId(requestId);
        return message;
    }
}
