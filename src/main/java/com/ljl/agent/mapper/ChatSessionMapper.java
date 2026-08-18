package com.ljl.agent.mapper;

import com.ljl.agent.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    int insert(ChatSession session);

    ChatSession selectById(Long id);

    List<ChatSession> selectByUserId(Long userId);

    int touchUpdatedAt(Long id);
}
