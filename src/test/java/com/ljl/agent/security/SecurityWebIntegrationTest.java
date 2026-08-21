package com.ljl.agent.security;

import com.ljl.agent.auth.AuthService;
import com.ljl.agent.auth.JwtTokenService;
import com.ljl.agent.config.OpenApiConfig;
import com.ljl.agent.config.SecurityConfig;
import com.ljl.agent.controller.AuthController;
import com.ljl.agent.controller.DocumentUploadController;
import com.ljl.agent.controller.HealthController;
import com.ljl.agent.controller.KnowledgeBaseController;
import com.ljl.agent.controller.UserController;
import com.ljl.agent.dto.response.UserVO;
import com.ljl.agent.entity.User;
import com.ljl.agent.exception.GlobalExceptionHandler;
import com.ljl.agent.ingestion.DocumentIngestionService;
import com.ljl.agent.mapper.UserMapper;
import com.ljl.agent.redis.TokenBlacklistService;
import com.ljl.agent.redis.BlacklistUnavailableException;
import com.ljl.agent.service.KnowledgeBaseService;
import com.ljl.agent.service.UserService;
import com.ljl.agent.util.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(classes = SecurityWebIntegrationTest.TestApplication.class)
@ActiveProfiles("dev")
class SecurityWebIntegrationTest {

    private static final String RAW_PASSWORD = "Test@123456";
    private static final String TEST_ISSUER =
            "https://ljl-agent-backend.local";
    private static final String TEST_SECRET = randomSecret();

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private DocumentIngestionService documentIngestionService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    private MockMvc mockMvc;

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.secret-base64", () -> TEST_SECRET);
        registry.add("app.security.jwt.issuer", () -> TEST_ISSUER);
        registry.add("app.security.jwt.access-token-ttl", () -> "30m");
    }

    @BeforeEach
    void setUp() {
        reset(
                userMapper,
                userService,
                knowledgeBaseService,
                documentIngestionService,
                tokenBlacklistService
        );
        mockMvc = webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldLoginThroughAuthenticationManagerAndReturnSafeJwt()
            throws Exception {
        when(userMapper.selectByUsername("login_user"))
                .thenReturn(user("login_user", User.ROLE_USER, true));

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "  login_user  ",
                                  "password": "Test@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(1800))
                .andExpect(jsonPath("$.data.user.id").value(21))
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andExpect(jsonPath("$.data.user.passwordHash")
                        .doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JsonNode claims = decodePayload(body);
        assertEquals("21", claims.path("sub").stringValue());
        assertEquals("USER", claims.path("role").stringValue());
        assertEquals(TEST_ISSUER, claims.path("iss").stringValue());
        assertFalse(claims.has("password"));
        assertFalse(claims.has("passwordHash"));
        assertFalse(body.contains("pbkdf2_sha256"));
    }

    @Test
    void shouldUseSame401ForUnknownWrongPasswordAndDisabledUser()
            throws Exception {
        assertCredentialFailure("unknown", RAW_PASSWORD);

        when(userMapper.selectByUsername("login_user"))
                .thenReturn(user("login_user", User.ROLE_USER, true));
        assertCredentialFailure("login_user", "wrong-password");

        when(userMapper.selectByUsername("disabled_user"))
                .thenReturn(user("disabled_user", User.ROLE_USER, false));
        assertCredentialFailure("disabled_user", RAW_PASSWORD);
    }

    @Test
    void shouldEnforcePublicAuthenticatedAdminAndStatelessBoundaries()
            throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("OK"));

        when(userService.register(any())).thenReturn(userView(User.ROLE_USER));
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "new_user",
                                  "password": "Test@123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("USER"));

        mockMvc.perform(get("/api/knowledge-bases"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code").value(40102))
                .andExpect(jsonPath("$.message")
                        .value("未认证或登录状态无效"))
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Bearer"
                ));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));

        when(knowledgeBaseService.listByCurrentUser(21L))
                .thenReturn(List.of());
        String userToken = signedToken(
                "21",
                User.ROLE_USER,
                TEST_ISSUER,
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300)
        );
        mockMvc.perform(get("/api/knowledge-bases")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + userToken
                        ))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        mockMvc.perform(get("/api/knowledge-bases"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));

        mockMvc.perform(get("/api/documents/30/processing-status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));

        mockMvc.perform(get("/api/users")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + userToken
                        ))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code").value(40303))
                .andExpect(jsonPath("$.message").value("权限不足"));

        when(userService.findById(21L))
                .thenReturn(userView(User.ROLE_USER));
        mockMvc.perform(get("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + userToken
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.role").value("USER"));

        when(userService.findAll()).thenReturn(List.of());
        String adminToken = signedToken(
                "22",
                User.ROLE_ADMIN,
                TEST_ISSUER,
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300)
        );
        mockMvc.perform(get("/api/users")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + adminToken
                        ))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectTamperedExpiredWrongIssuerAndIllegalRoleTokens()
            throws Exception {
        Instant now = Instant.now();
        String valid = signedToken(
                "21",
                User.ROLE_USER,
                TEST_ISSUER,
                now.minusSeconds(5),
                now.plusSeconds(300)
        );
        assertInvalidToken(tamperSignature(valid));
        assertInvalidToken(signedToken(
                "21",
                User.ROLE_USER,
                TEST_ISSUER,
                now.minusSeconds(600),
                now.minusSeconds(300)
        ));
        assertInvalidToken(signedToken(
                "21",
                User.ROLE_USER,
                "https://wrong-issuer.local",
                now.minusSeconds(5),
                now.plusSeconds(300)
        ));

        String illegalRole = signedToken(
                "21",
                "ROLE_ADMIN",
                TEST_ISSUER,
                now.minusSeconds(5),
                now.plusSeconds(300)
        );
        mockMvc.perform(get("/api/users")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + illegalRole
                        ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40303));
    }

    @Test
    void shouldCloseLogoutBlacklistLoopAndKeepLogoutIdempotent()
            throws Exception {
        String token = signedToken(
                "21",
                User.ROLE_USER,
                TEST_ISSUER,
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300)
        );

        when(tokenBlacklistService.isBlacklisted(anyString()))
                .thenReturn(false);
        mockMvc.perform(post("/api/auth/logout")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(tokenBlacklistService).revoke(any(Jwt.class));

        reset(tokenBlacklistService);
        when(tokenBlacklistService.isBlacklisted(anyString()))
                .thenReturn(true);

        mockMvc.perform(get("/api/knowledge-bases")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));

        mockMvc.perform(post("/api/auth/logout")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void shouldFailClosedWith503WhenBlacklistRedisIsUnavailable()
            throws Exception {
        when(tokenBlacklistService.isBlacklisted(anyString()))
                .thenThrow(new BlacklistUnavailableException(
                        new RedisConnectionFailureException("down")
                ));
        String token = signedToken(
                "21",
                User.ROLE_USER,
                TEST_ISSUER,
                Instant.now().minusSeconds(5),
                Instant.now().plusSeconds(300)
        );

        mockMvc.perform(get("/api/knowledge-bases")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(50301))
                .andExpect(jsonPath("$.message")
                        .value("认证服务暂时不可用"));
    }

    @Test
    void shouldExposeDevOpenApiAndBearerSecurityScheme() throws Exception {
        String apiDocs = mockMvc.perform(get(
                        "/v3/api-docs/stage-4-day-1"
                ))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertTrue(apiDocs.contains("\"/api/auth/login\""));
        assertTrue(apiDocs.contains("\"/api/auth/logout\""));
        assertTrue(apiDocs.contains("\"/api/users/me\""));
        assertFalse(apiDocs.contains("\"/api/users/login\""));
        assertTrue(apiDocs.contains("\"bearerAuth\""));
        assertTrue(apiDocs.contains("\"scheme\":\"bearer\""));
        assertTrue(apiDocs.contains(
                "\"/api/knowledge-bases/{knowledgeBaseId}/documents/upload\""
        ));
        assertTrue(apiDocs.contains(
                "\"/api/documents/{documentId}/processing-status\""
        ));
        assertTrue(apiDocs.contains("\"413\""));
        assertTrue(apiDocs.contains("\"422\""));
        assertTrue(apiDocs.contains("\"429\""));
        assertTrue(apiDocs.contains("\"503\""));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    private void assertCredentialFailure(String username, String password)
            throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code").value(40101))
                .andExpect(jsonPath("$.message")
                        .value("用户名或密码错误"));
    }

    private void assertInvalidToken(String token) throws Exception {
        mockMvc.perform(get("/api/knowledge-bases")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code").value(40102))
                .andExpect(jsonPath("$.message")
                        .value("未认证或登录状态无效"));
    }

    private String signedToken(
            String subject,
            String role,
            String issuer,
            Instant issuedAt,
            Instant expiresAt
    ) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", role)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    private JsonNode decodePayload(String loginResponse) throws Exception {
        String token = objectMapper.readTree(loginResponse)
                .path("data")
                .path("accessToken")
                .stringValue();
        String[] parts = token.split("\\.");
        byte[] json = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readTree(json);
    }

    private String tamperSignature(String token) {
        int index = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(index) == 'A' ? 'B' : 'A';
        return token.substring(0, index)
                + replacement
                + token.substring(index + 1);
    }

    private User user(String username, String role, boolean enabled) {
        User user = new User();
        user.setId(21L);
        user.setUsername(username);
        user.setPasswordHash(PasswordUtils.hash(RAW_PASSWORD));
        user.setRole(role);
        user.setStatus(enabled ? User.STATUS_ENABLED : User.STATUS_DISABLED);
        return user;
    }

    private UserVO userView(String role) {
        UserVO user = new UserVO();
        user.setId(21L);
        user.setUsername("new_user");
        user.setRole(role);
        user.setStatus(User.STATUS_ENABLED);
        return user;
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
    })
    @Import({
            SecurityConfig.class,
            OpenApiConfig.class,
            JsonAuthenticationEntryPoint.class,
            JsonAccessDeniedHandler.class,
            CurrentUser.class,
            ProjectPasswordEncoder.class,
            ProjectUserDetailsService.class,
            JwtTokenService.class,
            AuthService.class,
            AuthController.class,
            HealthController.class,
            UserController.class,
            KnowledgeBaseController.class,
            DocumentUploadController.class,
            GlobalExceptionHandler.class,
            CollaboratorConfiguration.class
    })
    static class TestApplication {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CollaboratorConfiguration {

        @Bean
        UserMapper userMapper() {
            return mock(UserMapper.class);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }

        @Bean
        KnowledgeBaseService knowledgeBaseService() {
            return mock(KnowledgeBaseService.class);
        }

        @Bean
        DocumentIngestionService documentIngestionService() {
            return mock(DocumentIngestionService.class);
        }

        @Bean
        TokenBlacklistService tokenBlacklistService() {
            return mock(TokenBlacklistService.class);
        }
    }
}
