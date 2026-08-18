package com.ljl.agent.dto.response;

/**
 * 登录成功响应，不包含密码、密码摘要或 JWT 密钥。
 */
public class LoginResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserVO user;

    public LoginResponse(
            String accessToken,
            long expiresIn,
            UserVO user
    ) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public UserVO getUser() {
        return user;
    }
}
