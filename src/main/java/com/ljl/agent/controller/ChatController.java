package com.ljl.agent.controller;

import com.ljl.agent.common.Result;
import com.ljl.agent.dto.request.ChatMessageCreateRequest;
import com.ljl.agent.dto.request.ChatSessionCreateRequest;
import com.ljl.agent.dto.response.ChatMessageVO;
import com.ljl.agent.dto.response.ChatSessionVO;
import com.ljl.agent.security.CurrentUser;
import com.ljl.agent.redis.FixedWindowRateLimiter;
import com.ljl.agent.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@Validated
@Tag(name = "聊天", description = "聊天会话和消息持久化，不调用大模型")
public class ChatController {

    private final ChatService chatService;
    private final CurrentUser currentUser;
    private final FixedWindowRateLimiter rateLimiter;

    public ChatController(
            ChatService chatService,
            CurrentUser currentUser,
            FixedWindowRateLimiter rateLimiter
    ) {
        this.chatService = chatService;
        this.currentUser = currentUser;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建聊天会话")
    public Result<ChatSessionVO> createSession(
            @Valid @RequestBody ChatSessionCreateRequest request,
            Authentication authentication) {

        return Result.success(
                chatService.createSession(
                        currentUser.requireUserId(authentication),
                        request
                )
        );
    }

    @GetMapping("/sessions")
    @Operation(summary = "查询用户的聊天会话")
    public Result<List<ChatSessionVO>> listSessions(
            Authentication authentication) {

        return Result.success(
                chatService.listSessions(
                        currentUser.requireUserId(authentication)
                )
        );
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(
            summary = "保存聊天消息",
            description = "只允许会话 owner 写入；按当前 JWT 用户执行 Redis Lua 固定窗口限流"
    )
    public Result<ChatMessageVO> createMessage(
            @Parameter(description = "会话 ID", example = "1")
            @PathVariable
            @Positive(message = "会话 ID 必须大于 0")
            Long sessionId,
            @Valid
            @RequestBody
            ChatMessageCreateRequest request,
            Authentication authentication) {

        Long currentUserId = currentUser.requireUserId(authentication);
        rateLimiter.check(currentUserId);
        return Result.success(
                chatService.createMessage(
                        currentUserId,
                        sessionId,
                        request
                )
        );
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "查询会话消息")
    public Result<List<ChatMessageVO>> listMessages(
            @Parameter(description = "会话 ID", example = "1")
            @PathVariable
            @Positive(message = "会话 ID 必须大于 0")
            Long sessionId,
            Authentication authentication) {

        return Result.success(
                chatService.listMessages(
                        currentUser.requireUserId(authentication),
                        sessionId
                )
        );
    }
}
