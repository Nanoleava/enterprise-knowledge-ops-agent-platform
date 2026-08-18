package com.ljl.agent;

import com.ljl.agent.entity.ChatMessage;
import com.ljl.agent.entity.ChatSession;
import com.ljl.agent.entity.User;
import com.ljl.agent.mapper.ChatMessageMapper;
import com.ljl.agent.mapper.ChatSessionMapper;
import com.ljl.agent.mapper.UserMapper;
import com.ljl.agent.util.PasswordUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@Rollback
@EnabledIfEnvironmentVariable(
        named = "DB_PASSWORD",
        matches = ".+",
        disabledReason = "未提供 DB_PASSWORD，跳过真实 MySQL 集成测试"
)
class ChatMapperTest extends AbstractIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ChatSessionMapper chatSessionMapper;

    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Test
    void shouldPersistAndQuerySessionsAndMessages() {
        User user = new User();
        user.setUsername("chat_mapper_" + System.nanoTime());
        user.setPasswordHash(PasswordUtils.hash("Test@123456"));
        user.setRole(User.ROLE_USER);
        user.setStatus(User.STATUS_ENABLED);
        assertEquals(1, userMapper.insert(user));
        assertNotNull(user.getId());

        ChatSession session = new ChatSession();
        session.setUserId(user.getId());
        session.setTitle("Mapper 集成测试");
        assertEquals(1, chatSessionMapper.insert(session));
        assertNotNull(session.getId());

        ChatMessage first = message(
                session.getId(),
                user.getId(),
                ChatMessage.ROLE_USER,
                "你好",
                "mapper-request-" + System.nanoTime()
        );
        ChatMessage second = message(
                session.getId(),
                user.getId(),
                ChatMessage.ROLE_ASSISTANT,
                "你好，我能帮你什么？",
                "mapper-request-" + System.nanoTime()
        );
        assertEquals(1, chatMessageMapper.insert(first));
        assertEquals(1, chatMessageMapper.insert(second));
        assertEquals(1, chatSessionMapper.touchUpdatedAt(session.getId()));

        ChatSession savedSession = chatSessionMapper.selectById(
                session.getId()
        );
        List<ChatSession> sessions = chatSessionMapper.selectByUserId(
                user.getId()
        );
        List<ChatMessage> messages = chatMessageMapper.selectBySessionId(
                session.getId()
        );

        assertNotNull(savedSession);
        assertNotNull(savedSession.getCreatedAt());
        assertNotNull(savedSession.getUpdatedAt());
        assertEquals(session.getId(), sessions.getFirst().getId());
        assertEquals(2, messages.size());
        assertEquals(first.getId(), messages.getFirst().getId());
        assertEquals(second.getId(), messages.getLast().getId());
        assertEquals(
                first.getId(),
                chatMessageMapper
                        .selectByRequestId(first.getRequestId())
                        .getId()
        );
    }

    private ChatMessage message(
            Long sessionId,
            Long userId,
            String role,
            String content,
            String requestId
    ) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setRequestId(requestId);
        return message;
    }
}
