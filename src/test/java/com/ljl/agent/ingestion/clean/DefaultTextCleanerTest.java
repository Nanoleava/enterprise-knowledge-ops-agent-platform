package com.ljl.agent.ingestion.clean;

import com.ljl.agent.ingestion.parser.DocumentParsingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultTextCleanerTest {

    private final DefaultTextCleaner cleaner = new DefaultTextCleaner();

    @Test
    void shouldNormalizeWithoutDestroyingMarkdownStructure() {
        assertEquals(
                "# 标题\n\n\n- 列表\n    code",
                cleaner.clean(
                        "\uFEFF\r\n# 标题  \r\n\r\n\r\n\r\n- 列表\t\r\n    code\r\n"
                )
        );
    }

    @Test
    void shouldRejectTextThatIsEmptyAfterCleaning() {
        assertThrows(
                DocumentParsingException.class,
                () -> cleaner.clean("\uFEFF\0\r\n  \r\n")
        );
    }
}
