package com.ljl.agent.controller;

import com.ljl.agent.common.Result;
import com.ljl.agent.dto.request.ChatMessageCreateRequest;
import com.ljl.agent.dto.request.ChatSessionCreateRequest;
import com.ljl.agent.dto.response.ChatMessageVO;
import com.ljl.agent.dto.response.ChatSessionVO;
import com.ljl.agent.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Validated
@Tag(name = "聊天", description = "聊天会话和消息持久化，不调用大模型")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建聊天会话")
    public Result<ChatSessionVO> createSession(
            @Valid @RequestBody ChatSessionCreateRequest request) {

        return Result.success(
                chatService.createSession(request)
        );
    }

    @GetMapping("/sessions")
    @Operation(summary = "查询用户的聊天会话")
    public Result<List<ChatSessionVO>> listSessions(
            @Parameter(description = "用户 ID", example = "1")
            @RequestParam
            @Positive(message = "用户 ID 必须大于 0")
            Long userId) {

        return Result.success(
                chatService.listSessions(userId)
        );
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "保存聊天消息")
    public Result<ChatMessageVO> createMessage(
            @Parameter(description = "会话 ID", example = "1")
            @PathVariable
            @Positive(message = "会话 ID 必须大于 0")
            Long sessionId,
            @Valid
            @RequestBody
            ChatMessageCreateRequest request) {

        return Result.success(
                chatService.createMessage(sessionId, request)
        );
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "查询会话消息")
    public Result<List<ChatMessageVO>> listMessages(
            @Parameter(description = "会话 ID", example = "1")
            @PathVariable
            @Positive(message = "会话 ID 必须大于 0")
            Long sessionId) {

        return Result.success(
                chatService.listMessages(sessionId)
        );
    }
}
