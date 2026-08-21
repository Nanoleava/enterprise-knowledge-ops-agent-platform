package com.ljl.agent.auth;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.dto.request.UserLoginRequest;
import com.ljl.agent.dto.response.LoginResponse;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.redis.TokenBlacklistService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * 编排标准用户名密码认证与 JWT 签发，不重复实现密码校验。
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final TokenBlacklistService blacklistService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            TokenBlacklistService blacklistService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.blacklistService = blacklistService;
    }

    public LoginResponse login(UserLoginRequest request) {
        String username = request.getUsername().trim();
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            username,
                            request.getPassword()
                    )
            );
        } catch (InternalAuthenticationServiceException exception) {
            // 数据库/用户加载组件故障属于系统异常，不能伪装成账号密码错误。
            throw exception;
        } catch (AuthenticationException exception) {
            throw new BusinessException(
                    ErrorCode.INVALID_USERNAME_OR_PASSWORD
            );
        }

        if (!(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            throw new IllegalStateException("认证结果缺少项目用户身份");
        }

        JwtTokenService.IssuedToken token = jwtTokenService.issue(loginUser);
        return new LoginResponse(
                token.value(),
                token.expiresIn(),
                loginUser.toUserVO()
        );
    }

    public void logout(Jwt jwt) {
        blacklistService.revoke(jwt);
    }
}
