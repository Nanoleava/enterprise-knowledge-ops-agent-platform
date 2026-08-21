package com.ljl.agent.ingestion.parser;

import com.ljl.agent.ingestion.DocumentFileType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentParserTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParseTxtBomAndPreserveMarkdownStructure() throws Exception {
        Path txt = tempDir.resolve("guide.txt");
        Files.writeString(txt, "\uFEFF中文正文", StandardCharsets.UTF_8);
        Path markdown = tempDir.resolve("guide.md");
        Files.writeString(
                markdown,
                "# 标题\n\n- 列表\n\n```java\nint n = 1;\n```",
                StandardCharsets.UTF_8
        );

        ParserRegistry registry = new ParserRegistry(List.of(
                new TxtParser(),
                new MarkdownParser()
        ));
        assertEquals(
                "\uFEFF中文正文",
                registry.requireParser(DocumentFileType.TXT).parse(txt)
        );
        assertEquals(
                "# 标题\n\n- 列表\n\n```java\nint n = 1;\n```",
                registry.requireParser(DocumentFileType.MARKDOWN)
                        .parse(markdown)
        );
    }

    @Test
    void shouldFailForInvalidUtf8() throws Exception {
        Path invalid = tempDir.resolve("bad.txt");
        Files.write(invalid, new byte[]{(byte) 0xC3, (byte) 0x28});

        assertThrows(
                DocumentParsingException.class,
                () -> new TxtParser().parse(invalid)
        );
    }
}
