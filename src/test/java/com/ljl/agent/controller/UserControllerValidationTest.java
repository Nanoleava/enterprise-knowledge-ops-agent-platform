package com.ljl.agent.controller;

import com.ljl.agent.dto.response.UserVO;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.exception.GlobalExceptionHandler;
import com.ljl.agent.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerValidationTest {

    private UserService userService;
    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @Test
    void shouldReturnFieldMessagesForInvalidRegistration()
            throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ab",
                                  "password": "123",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message")
                        .value("参数校验失败"))
                .andExpect(jsonPath("$.data.username").value(
                        "用户名长度必须在 3 到 50 个字符之间"
                ))
                .andExpect(jsonPath("$.data.password").value(
                        "密码长度必须在 8 到 64 个字符之间"
                ))
                .andExpect(jsonPath("$.data.email")
                        .value("邮箱格式不正确"));

        verifyNoInteractions(userService);
    }

    @Test
    void shouldExplainMalformedJson() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value(
                        "请求体格式不正确或缺少必要内容"
                ));
    }

    @Test
    void shouldExplainPasswordFormatError() throws Exception {
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "valid_user",
                                  "password": "abcdefgh"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data.password").value(
                        "密码必须包含字母和数字，且不能包含空白字符"
                ));

        verifyNoInteractions(userService);
    }

    @Test
    void shouldExplainInvalidPathVariableType() throws Exception {
        mockMvc.perform(get("/api/users/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message")
                        .value("参数 id 格式不正确"));
    }

    @Test
    void shouldMapNotFoundBusinessCodeToHttp404() throws Exception {
        when(userService.findById(99L)).thenThrow(
                new BusinessException(40401, "用户不存在，userId=99")
        );

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message")
                        .value("用户不存在，userId=99"));
    }

}
