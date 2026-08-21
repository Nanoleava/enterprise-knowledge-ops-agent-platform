package com.ljl.agent.service.impl;

import com.ljl.agent.common.ErrorCode;
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
import com.ljl.agent.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;

@Service
@Validated
public class ChatServiceImpl implements ChatService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ChatServiceImpl.class);

    private final UserMapper userMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    public ChatServiceImpl(
            UserMapper userMapper,
            ChatSessionMapper chatSessionMapper,
            ChatMessageMapper chatMessageMapper) {
        this.userMapper = userMapper;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    @Transactional
    public ChatSessionVO createSession(
            Long currentUserId,
            ChatSessionCreateRequest request
    ) {
        if (request == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "创建会话请求不能为空"
            );
        }

        Long userId = requirePositiveId(
                currentUserId,
                "当前用户 ID 必须是正整数"
        );
        requireEnabledUser(userId);

        String title = requireText(
                request.getTitle(),
                "会话标题不能为空"
        );
        if (title.length() > 200) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "会话标题长度不能超过 200"
            );
        }

        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);

        int affectedRows = chatSessionMapper.insert(session);
        if (affectedRows != 1) {
            throw new BusinessException(
                    ErrorCode.CHAT_SESSION_CREATE_FAILED
            );
        }

        ChatSession saved =
                chatSessionMapper.selectById(session.getId());
        if (saved == null) {
            throw new BusinessException(
                    ErrorCode.CHAT_SESSION_CREATE_FAILED,
                    "聊天会话创建后查询失败，sessionId=" + session.getId()
            );
        }

        LOGGER.info(
                "chat session created: sessionId={}, userId={}",
                saved.getId(),
                saved.getUserId()
        );

        return ChatSessionVO.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSessionVO> listSessions(Long currentUserId) {
        Long validUserId = requirePositiveId(
                currentUserId,
                "当前用户 ID 必须是正整数"
        );
        requireUser(validUserId);

        return chatSessionMapper.selectByUserId(validUserId)
                .stream()
                .map(ChatSessionVO::from)
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageVO createMessage(
            Long currentUserId,
            Long sessionId,
            ChatMessageCreateRequest request) {

        Long userId = requirePositiveId(
                currentUserId,
                "当前用户 ID 必须是正整数"
        );
        Long validSessionId = requirePositiveId(
                sessionId,
                "会话 ID 必须是正整数"
        );

        if (request == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "创建消息请求不能为空"
            );
        }

        ChatSession session = requireOwnedSession(userId, validSessionId);
        requireEnabledUser(userId);

        String role = normalizeRole(request.getRole());

        String content = requireContent(request.getContent());

        String requestId = requireText(
                request.getRequestId(),
                "requestId 不能为空"
        );

        if (requestId.length() > 64) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "requestId 长度不能超过 64"
            );
        }

        /*
         * 第一层：业务预检查。
         * 作用是尽早给出可读的冲突信息。
         */
        if (chatMessageMapper.selectByRequestId(requestId) != null) {
            throw new BusinessException(
                    ErrorCode.CHAT_MESSAGE_DUPLICATE,
                    "requestId 已存在：" + requestId
            );
        }

        ChatMessage message = new ChatMessage();

        message.setSessionId(session.getId());

        /*
         * 不从客户端接收 userId。
         * 始终根据 session 推导，防止 userId 与 sessionId 归属不一致。
         */
        message.setUserId(userId);

        message.setRole(role);
        message.setContent(content);
        message.setRequestId(requestId);

        try {
            /*
             * 第二层：
             * 数据库 UNIQUE(request_id) 解决真正的并发竞争。
             */
            int affectedRows = chatMessageMapper.insert(message);
            if (affectedRows != 1) {
                throw new BusinessException(
                        ErrorCode.CHAT_MESSAGE_CREATE_FAILED
                );
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(
                    ErrorCode.CHAT_MESSAGE_DUPLICATE,
                    "requestId 已存在：" + requestId,
                    e
            );
        }

        chatSessionMapper.touchUpdatedAt(session.getId());

        ChatMessage saved =
                chatMessageMapper.selectById(message.getId());
        if (saved == null) {
            throw new BusinessException(
                    ErrorCode.CHAT_MESSAGE_CREATE_FAILED,
                    "聊天消息创建后查询失败，messageId=" + message.getId()
            );
        }

        LOGGER.info(
                "chat message saved: messageId={}, sessionId={}, userId={}, role={}, requestId={}",
                saved.getId(),
                saved.getSessionId(),
                saved.getUserId(),
                saved.getRole(),
                saved.getRequestId()
        );

        return ChatMessageVO.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageVO> listMessages(
            Long currentUserId,
            Long sessionId
    ) {
        Long userId = requirePositiveId(
                currentUserId,
                "当前用户 ID 必须是正整数"
        );
        Long validSessionId = requirePositiveId(
                sessionId,
                "会话 ID 必须是正整数"
        );
        requireOwnedSession(userId, validSessionId);

        return chatMessageMapper.selectBySessionId(validSessionId)
                .stream()
                .map(ChatMessageVO::from)
                .toList();
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(
                    ErrorCode.USER_NOT_FOUND,
                    "用户不存在，userId=" + userId
            );
        }

        return user;
    }

    private User requireEnabledUser(Long userId) {
        User user = requireUser(userId);
        if (!Integer.valueOf(User.STATUS_ENABLED).equals(user.getStatus())) {
            throw new BusinessException(
                    ErrorCode.USER_DISABLED,
                    "用户已被禁用，不能写入聊天数据，userId=" + userId
            );
        }
        return user;
    }

    private ChatSession requireSession(Long sessionId) {
        ChatSession session =
                chatSessionMapper.selectById(sessionId);

        if (session == null) {
            throw new BusinessException(
                    ErrorCode.CHAT_SESSION_NOT_FOUND,
                    "聊天会话不存在，sessionId=" + sessionId
            );
        }

        return session;
    }

    private ChatSession requireOwnedSession(
            Long currentUserId,
            Long sessionId
    ) {
        ChatSession session = requireSession(sessionId);
        if (!currentUserId.equals(session.getUserId())) {
            throw new BusinessException(
                    ErrorCode.CHAT_SESSION_FORBIDDEN,
                    "聊天会话不属于当前用户，userId="
                            + currentUserId
                            + ", sessionId="
                            + sessionId
            );
        }
        return session;
    }

    private String normalizeRole(String role) {
        String normalized =
                requireText(role, "消息角色不能为空")
                        .toUpperCase(Locale.ROOT);

        if (!ChatMessage.ROLE_USER.equals(normalized)
                && !ChatMessage.ROLE_ASSISTANT.equals(normalized)) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "消息角色只能是 USER 或 ASSISTANT"
            );
        }

        return normalized;
    }

    private String requireText(
            String value,
            String errorMessage) {

        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    errorMessage
            );
        }

        return value.trim();
    }

    private String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "消息内容不能为空"
            );
        }
        return content;
    }

    private Long requirePositiveId(
            Long id,
            String errorMessage) {

        if (id == null || id <= 0) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    errorMessage
            );
        }
        return id;
    }
}
