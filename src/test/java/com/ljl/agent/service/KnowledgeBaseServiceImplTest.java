package com.ljl.agent.service;

import com.ljl.agent.dto.request.KnowledgeBaseCreateRequest;
import com.ljl.agent.dto.response.KnowledgeBaseVO;
import com.ljl.agent.entity.KnowledgeBase;
import com.ljl.agent.entity.User;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.mapper.KnowledgeBaseMapper;
import com.ljl.agent.mapper.UserMapper;
import com.ljl.agent.service.impl.KnowledgeBaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class KnowledgeBaseServiceImplTest {

    private KnowledgeBaseMapper knowledgeBaseMapper;
    private UserMapper userMapper;
    private KnowledgeBaseServiceImpl service;

    @BeforeEach
    void setUp() {
        knowledgeBaseMapper = mock(KnowledgeBaseMapper.class);
        userMapper = mock(UserMapper.class);
        service = new KnowledgeBaseServiceImpl(
                knowledgeBaseMapper,
                userMapper
        );
    }

    @Test
    void shouldRejectMissingOwner() {
        KnowledgeBaseCreateRequest request = request(
                7L,
                "Java资料",
                null
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.create(request)
        );

        assertEquals(40401, exception.getCode());
        assertEquals("用户不存在，userId=7", exception.getMessage());
    }

    @Test
    void shouldRejectDisabledOwner() {
        KnowledgeBaseCreateRequest request = request(
                7L,
                "Java资料",
                null
        );
        when(userMapper.selectById(7L))
                .thenReturn(user(7L, User.STATUS_DISABLED));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.create(request)
        );

        assertEquals(40301, exception.getCode());
    }

    @Test
    void shouldHandleConcurrentDuplicateWithConflictCode() {
        KnowledgeBaseCreateRequest request = request(
                7L,
                "Java资料",
                null
        );
        when(userMapper.selectById(7L))
                .thenReturn(user(7L, User.STATUS_ENABLED));
        when(knowledgeBaseMapper.insert(any(KnowledgeBase.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.create(request)
        );

        assertEquals(40902, exception.getCode());
    }

    @Test
    void shouldNormalizeAndCreateKnowledgeBase(CapturedOutput output) {
        KnowledgeBaseCreateRequest request = request(
                7L,
                "  Java资料  ",
                "  学习资料  "
        );
        AtomicReference<KnowledgeBase> inserted = new AtomicReference<>();
        when(userMapper.selectById(7L))
                .thenReturn(user(7L, User.STATUS_ENABLED));
        when(knowledgeBaseMapper.insert(any(KnowledgeBase.class)))
                .thenAnswer(invocation -> {
                    KnowledgeBase value = invocation.getArgument(0);
                    value.setId(20L);
                    inserted.set(value);
                    return 1;
                });
        when(knowledgeBaseMapper.selectById(20L))
                .thenAnswer(invocation -> {
                    KnowledgeBase value = inserted.get();
                    value.setCreatedAt(LocalDateTime.now());
                    value.setUpdatedAt(LocalDateTime.now());
                    return value;
                });

        KnowledgeBaseVO result = service.create(request);

        assertEquals("Java资料", inserted.get().getName());
        assertEquals("学习资料", inserted.get().getDescription());
        assertEquals(20L, result.getId());
        assertTrue(output.getAll().contains(
                "knowledge base created: knowledgeBaseId=20, userId=7"
        ));
    }

    @Test
    void createShouldHaveTransactionBoundary() throws Exception {
        Transactional transactional = KnowledgeBaseServiceImpl.class
                .getMethod("create", KnowledgeBaseCreateRequest.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
    }

    @Test
    void shouldGetKnowledgeBaseById() {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setId(20L);
        knowledgeBase.setUserId(7L);
        knowledgeBase.setName("Java资料");
        when(knowledgeBaseMapper.selectById(20L))
                .thenReturn(knowledgeBase);

        KnowledgeBaseVO result = service.getById(20L);

        assertEquals(20L, result.getId());
        assertEquals(7L, result.getUserId());
        assertEquals("Java资料", result.getName());
    }

    @Test
    void shouldRejectInvalidKnowledgeBaseIdBeforeQuery() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getById(0L)
        );

        assertEquals(40001, exception.getCode());
        assertEquals("知识库ID必须是正整数", exception.getMessage());
        verifyNoInteractions(knowledgeBaseMapper, userMapper);
    }

    @Test
    void shouldReturnNotFoundWhenKnowledgeBaseDoesNotExist() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.getById(20L)
        );

        assertEquals(40402, exception.getCode());
        assertEquals(
                "知识库不存在，knowledgeBaseId=20",
                exception.getMessage()
        );
    }

    @Test
    void getByIdShouldUseReadOnlyTransaction() throws Exception {
        Transactional transactional = KnowledgeBaseServiceImpl.class
                .getMethod("getById", Long.class)
                .getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertEquals(true, transactional.readOnly());
    }

    private KnowledgeBaseCreateRequest request(
            Long userId,
            String name,
            String description
    ) {
        KnowledgeBaseCreateRequest request =
                new KnowledgeBaseCreateRequest();
        request.setUserId(userId);
        request.setName(name);
        request.setDescription(description);
        return request;
    }

    private User user(Long id, int status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return user;
    }
}
