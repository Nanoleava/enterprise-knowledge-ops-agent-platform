package com.ljl.agent.security;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security Filter 层统一 401 JSON 响应。
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        boolean dependencyUnavailable = hasCause(
                authException,
                JwtBlacklistDependencyException.class
        );
        response.setStatus(dependencyUnavailable
                ? HttpServletResponse.SC_SERVICE_UNAVAILABLE
                : HttpServletResponse.SC_UNAUTHORIZED);
        if (!dependencyUnavailable) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getOutputStream(),
                Result.failure(dependencyUnavailable
                        ? ErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE
                        : ErrorCode.AUTHENTICATION_REQUIRED)
        );
    }

    private boolean hasCause(
            Throwable throwable,
            Class<? extends Throwable> expectedType
    ) {
        Throwable current = throwable;
        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
