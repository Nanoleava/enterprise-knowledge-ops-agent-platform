package com.ljl.agent.service.impl;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.dto.request.UserRegisterRequest;
import com.ljl.agent.dto.response.UserVO;
import com.ljl.agent.entity.User;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.mapper.UserMapper;
import com.ljl.agent.service.UserService;
import com.ljl.agent.util.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Locale;

/**
 * 用户业务实现。
 *
 * <p>负责用户注册规则、重复校验、密码处理和 Entity/VO 转换。</p>
 */
@Service
@Validated
public class UserServiceImpl implements UserService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserVO register(UserRegisterRequest request) {
        if (request == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "注册参数不能为空"
            );
        }

        String username = normalizeRequiredText(
                request.getUsername(),
                "用户名不能为空"
        );
        String rawPassword = normalizeRequiredPassword(
                request.getPassword()
        );
        String email = normalizeOptionalEmail(request.getEmail());

        // 业务层预检查：用于提供清晰错误信息。
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException(
                    ErrorCode.USER_DUPLICATE,
                    "用户名已存在：" + username
            );
        }

        if (email != null && userMapper.selectByEmail(email) != null) {
            throw new BusinessException(
                    ErrorCode.USER_DUPLICATE,
                    "邮箱已存在：" + email
            );
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(
                PasswordUtils.hash(rawPassword)
        );
        user.setEmail(email);

        // 公开注册接口只能注册普通用户。
        user.setRole(User.ROLE_USER);
        user.setStatus(User.STATUS_ENABLED);

        try {
            int affectedRows = userMapper.insert(user);

            if (affectedRows != 1) {
                throw new BusinessException(
                        ErrorCode.USER_CREATE_FAILED
                );
            }
        } catch (DuplicateKeyException exception) {
            // 数据库唯一索引的最终兜底。
            throw new BusinessException(
                    ErrorCode.USER_DUPLICATE,
                    exception
            );
        }

        /*
         * insert 后 MyBatis 会把数据库生成的 id 写回 user。
         * createdAt 和 updatedAt 由数据库生成，所以重新查询一次，
         * 得到完整数据库记录后再返回。
         */
        User savedUser = userMapper.selectById(user.getId());

        if (savedUser == null) {
            throw new BusinessException(
                    ErrorCode.USER_CREATE_FAILED,
                    "用户创建后查询失败，userId=" + user.getId()
            );
        }

        LOGGER.info(
                "user registered: userId={}, username={}",
                savedUser.getId(),
                savedUser.getUsername()
        );

        return UserVO.from(savedUser);
    }

    @Override
    public UserVO findById(Long id) {
        validateId(id);

        User user = userMapper.selectById(id);

        if (user == null) {
            throw new BusinessException(
                    ErrorCode.USER_NOT_FOUND,
                    "用户不存在，userId=" + id
            );
        }

        return UserVO.from(user);
    }

    @Override
    public List<UserVO> findAll() {
        return userMapper.selectAll()
                .stream()
                .map(UserVO::from)
                .toList();
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "用户 ID 必须是正整数"
            );
        }
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String text, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, message);
        }

        return text.trim();
    }

    private String normalizeRequiredPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "密码不能为空"
            );
        }

        return password;
    }
}
