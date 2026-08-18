package com.ljl.agent;

import com.ljl.agent.entity.User;
import com.ljl.agent.mapper.UserMapper;
import com.ljl.agent.util.PasswordUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@Rollback
@EnabledIfEnvironmentVariable(
        named = "DB_PASSWORD",
        matches = ".+",
        disabledReason = "未提供 DB_PASSWORD，跳过真实 MySQL 集成测试"
)
class UserMapperTest extends AbstractIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldInsertAndSelectUser() {
        String username =
                "mapper_test_" + System.nanoTime();

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(
                PasswordUtils.hash("Test@123456")
        );
        user.setEmail(null);
        user.setRole(User.ROLE_USER);
        user.setStatus(User.STATUS_ENABLED);

        int affectedRows = userMapper.insert(user);

        assertEquals(1, affectedRows);
        assertNotNull(user.getId());

        User savedUser =
                userMapper.selectById(user.getId());

        assertNotNull(savedUser);
        assertEquals(username, savedUser.getUsername());
        assertEquals(User.ROLE_USER, savedUser.getRole());
        assertEquals(User.STATUS_ENABLED, savedUser.getStatus());
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());
    }
}
