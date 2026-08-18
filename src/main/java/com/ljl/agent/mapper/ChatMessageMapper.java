package com.ljl.agent.mapper;

import com.ljl.agent.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    ChatMessage selectById(Long id);

    ChatMessage selectByRequestId(String requestId);

    List<ChatMessage> selectBySessionId(Long sessionId);
}