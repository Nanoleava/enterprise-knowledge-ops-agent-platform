package com.ljl.agent.service.impl;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.dto.request.KnowledgeBaseCreateRequest;
import com.ljl.agent.dto.response.KnowledgeBaseVO;
import com.ljl.agent.entity.KnowledgeBase;
import com.ljl.agent.entity.User;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.mapper.KnowledgeBaseMapper;
import com.ljl.agent.mapper.UserMapper;
import com.ljl.agent.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final UserMapper userMapper;

    public KnowledgeBaseServiceImpl(
            KnowledgeBaseMapper knowledgeBaseMapper,
            UserMapper userMapper
    ) {
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public KnowledgeBaseVO create(
            Long currentUserId,
            KnowledgeBaseCreateRequest request
    ) {
        if (request == null) {
            throw new BusinessException(
                    ErrorCode.PARAM_INVALID,
                    "知识库创建参数不能为空"
            );
        }

        Long userId = requirePositiveId(
                currentUserId,
                "当前用户ID必须是正整数"
        );
        String name = requireText(
                request.getName(),
                "知识库名称不能为空"
        );
        String description = trimToNull(request.getDescription());

        User owner = userMapper.selectById(userId);
        if (owner == null) {
            throw new BusinessException(
                    ErrorCode.USER_NOT_FOUND,
                    "用户不存在，userId=" + userId
            );
        }

        if (!Integer.valueOf(User.STATUS_ENABLED).equals(owner.getStatus())) {
            throw new BusinessException(
                    ErrorCode.USER_DISABLED,
                    "用户已被禁用，不能创建知识库，userId=" + userId
            );
        }

        KnowledgeBase existing =
                knowledgeBaseMapper.selectByUserIdAndName(userId, name);
        if (existing != null) {
            throw duplicateKnowledgeBase(userId, name);
        }

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(userId);
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(description);

        try {
            int affectedRows = knowledgeBaseMapper.insert(knowledgeBase);
            if (affectedRows != 1) {
                throw new BusinessException(
                        ErrorCode.KNOWLEDGE_BASE_CREATE_FAILED
                );
            }
        } catch (DuplicateKeyException exception) {
            // 数据库唯一索引用于处理并发创建同名知识库的情况。
            throw duplicateKnowledgeBase(userId, name);
        }

        KnowledgeBase saved =
                knowledgeBaseMapper.selectById(knowledgeBase.getId());
        if (saved == null) {
            throw new BusinessException(
                    ErrorCode.KNOWLEDGE_BASE_CREATE_FAILED,
                    "知识库创建后查询失败，knowledgeBaseId="
                            + knowledgeBase.getId()
            );
        }

        LOGGER.info(
                "knowledge base created: knowledgeBaseId={}, userId={}",
                saved.getId(),
                saved.getUserId()
        );

        return KnowledgeBaseVO.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseVO> listByCurrentUser(Long currentUserId) {
        Long userId = requirePositiveId(
                currentUserId,
                "当前用户ID必须是正整数"
        );
        return knowledgeBaseMapper.selectByUserId(userId)
                .stream()
                .map(KnowledgeBaseVO::from)
                .toList();
    }

    private BusinessException duplicateKnowledgeBase(
            Long userId,
            String name
    ) {
        return new BusinessException(
                ErrorCode.KNOWLEDGE_BASE_DUPLICATE,
                "该用户下已存在同名知识库，userId="
                        + userId
                        + ", name="
                        + name
        );
    }

    private Long requirePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, message);
        }
        return id;
    }

    private String requireText(String text, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, message);
        }
        return text.trim();
    }

    private String trimToNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return text.trim();
    }

    private KnowledgeBase requireKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase =
                knowledgeBaseMapper.selectById(id);

        if (knowledgeBase == null) {
            throw new BusinessException(
                    ErrorCode.KNOWLEDGE_BASE_NOT_FOUND,
                    "知识库不存在，knowledgeBaseId=" + id
            );
        }

        return knowledgeBase;
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBaseVO getById(Long currentUserId, Long id) {
        Long userId = requirePositiveId(
                currentUserId,
                "当前用户ID必须是正整数"
        );
        Long validId = requirePositiveId(
                id,
                "知识库ID必须是正整数"
        );
        KnowledgeBase knowledgeBase = requireKnowledgeBase(validId);

        if (!userId.equals(knowledgeBase.getUserId())) {
            throw new BusinessException(
                    ErrorCode.KNOWLEDGE_BASE_FORBIDDEN,
                    "知识库不属于当前用户，userId="
                            + userId
                            + ", knowledgeBaseId="
                            + validId
            );
        }

        return KnowledgeBaseVO.from(knowledgeBase);
    }
}
