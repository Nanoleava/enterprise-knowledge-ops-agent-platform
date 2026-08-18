package com.ljl.agent.common;

import com.ljl.agent.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommonResultTest {

    @Test
    void shouldBuildStablePageMetadataAndDefensiveRecords() {
        List<String> source = new ArrayList<>(List.of("a", "b"));
        PageResult<String> page = new PageResult<>(source, 21, 2, 10);
        source.add("c");

        assertEquals(List.of("a", "b"), page.getRecords());
        assertEquals(21L, page.getTotal());
        assertEquals(2, page.getPage());
        assertEquals(10, page.getSize());
        assertEquals(3L, page.getTotalPages());
        assertThrows(
                UnsupportedOperationException.class,
                () -> page.getRecords().add("d")
        );
    }

    @Test
    void shouldBuildResultAndBusinessExceptionFromErrorCode() {
        Result<Void> result = Result.failure(ErrorCode.DOCUMENT_NOT_FOUND);
        IllegalStateException cause = new IllegalStateException("database");
        BusinessException exception = new BusinessException(
                ErrorCode.DOCUMENT_NOT_FOUND,
                cause
        );

        assertEquals(40403, result.getCode());
        assertEquals("文档不存在", result.getMessage());
        assertEquals(40403, exception.getCode());
        assertEquals("文档不存在", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
