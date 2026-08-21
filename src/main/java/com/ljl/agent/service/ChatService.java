package com.ljl.agent.service;

import com.ljl.agent.dto.request.ChatMessageCreateRequest;
import com.ljl.agent.dto.request.ChatSessionCreateRequest;
import com.ljl.agent.dto.response.ChatMessageVO;
import com.ljl.agent.dto.response.ChatSessionVO;

import java.util.List;

public interface ChatService {

    ChatSessionVO createSession(
            Long currentUserId,
            ChatSessionCreateRequest request
    );

    List<ChatSessionVO> listSessions(Long currentUserId);

    ChatMessageVO createMessage(
            Long currentUserId,
            Long sessionId,
            ChatMessageCreateRequest request
    );

    List<ChatMessageVO> listMessages(Long currentUserId, Long sessionId);
}
