package com.ljl.agent.security;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * 将已经通过 Resource Server 校验的 JWT subject 转换为项目用户 ID。
 *
 * <p>身份只在 HTTP 边界读取；Service 和 Mapper 始终显式接收 userId。</p>
 */
@Component
public class CurrentUser {

    public Long requireUserId(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw invalidIdentity("当前请求缺少有效 JWT 身份");
        }

        String subject = jwtAuthentication.getToken().getSubject();
        if (subject == null || subject.isBlank()) {
            throw invalidIdentity("JWT subject 缺失");
        }

        try {
            long userId = Long.parseLong(subject);
            if (userId <= 0) {
                throw invalidIdentity("JWT subject 不是有效用户 ID");
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw invalidIdentity("JWT subject 不是有效用户 ID");
        }
    }

    private BusinessException invalidIdentity(String message) {
        return new BusinessException(
                ErrorCode.AUTHENTICATION_REQUIRED,
                message
        );
    }
}
