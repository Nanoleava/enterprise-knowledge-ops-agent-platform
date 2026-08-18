package com.ljl.agent.service;

import com.ljl.agent.dto.request.UserRegisterRequest;
import com.ljl.agent.dto.response.UserVO;
import com.ljl.agent.entity.User;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.mapper.UserMapper;
import com.ljl.agent.service.impl.UserServiceImpl;
import com.ljl.agent.util.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class UserServiceImplTest {

    private UserMapper userMapper;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userService = new UserServiceImpl(userMapper);
    }

    @Test
    void shouldReturnClearErrorInsteadOfNullPointerForMissingUsername() {
        UserRegisterRequest request = request(
                null,
                "Test@123456",
                "user@example.com"
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.register(request)
        );

        assertEquals(40001, exception.getCode());
        assertEquals("用户名不能为空", exception.getMessage());
    }

    @Test
    void shouldRejectDuplicateUsernameWithConflictCode() {
        UserRegisterRequest request = request(
                "existing_user",
                "Test@123456",
                null
        );
        when(userMapper.selectByUsername("existing_user"))
                .thenReturn(new User());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> userService.register(request)
        );

        assertEquals(40901, exception.getCode());
        assertEquals(
                "用户名已存在：existing_user",
                exception.getMessage()
        );
    }

    @Test
    void shouldNormalizeEmailHashPasswordAndReturnSafeView(
            CapturedOutput output
    ) {
        UserRegisterRequest request = request(
                "new_user",
                "Test@123456",
                "User@Example.COM"
        );
        AtomicReference<User> inserted = new AtomicReference<>();

        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            inserted.set(user);
            return 1;
        });
        when(userMapper.selectById(10L)).thenAnswer(invocation -> {
            User user = inserted.get();
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            return user;
        });

        UserVO result = userService.register(request);

        User stored = inserted.get();
        assertNotNull(stored);
        assertEquals("user@example.com", stored.getEmail());
        assertEquals(User.ROLE_USER, stored.getRole());
        assertEquals(User.STATUS_ENABLED, stored.getStatus());
        assertNotEquals("Test@123456", stored.getPasswordHash());
        assertTrue(PasswordUtils.matches(
                "Test@123456",
                stored.getPasswordHash()
        ));
        assertEquals(10L, result.getId());
        assertEquals("user@example.com", result.getEmail());
        verify(userMapper).selectByEmail("user@example.com");
        assertTrue(output.getAll().contains(
                "user registered: userId=10, username=new_user"
        ));
        assertFalse(output.getAll().contains("Test@123456"));
        assertFalse(output.getAll().contains(stored.getPasswordHash()));
    }

    @Test
    void registerShouldHaveTransactionBoundary() throws Exception {
        Transactional transactional = UserServiceImpl.class
                .getMethod("register", UserRegisterRequest.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
    }

    private UserRegisterRequest request(
            String username,
            String password,
            String email
    ) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(email);
        return request;
    }

}
