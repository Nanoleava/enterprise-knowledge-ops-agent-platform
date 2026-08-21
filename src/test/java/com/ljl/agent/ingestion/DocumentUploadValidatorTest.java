package com.ljl.agent.ingestion;

import com.ljl.agent.config.DocumentIngestionProperties;
import com.ljl.agent.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentUploadValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldAcceptUtf8TxtAndKeepOnlyBaseName() {
        ValidatedUpload upload = validator(DataSize.ofKilobytes(1)).validate(
                file("../guide.txt", "text/plain", "中文正文")
        );

        assertEquals("guide.txt", upload.originalFileName());
        assertEquals(DocumentFileType.TXT, upload.fileType());
        assertEquals(
                "中文正文".getBytes(StandardCharsets.UTF_8).length,
                upload.fileSize()
        );
    }

    @Test
    void shouldRejectEmptyOversizedAndUnsupportedFiles() {
        BusinessException empty = assertThrows(
                BusinessException.class,
                () -> validator(DataSize.ofKilobytes(1)).validate(
                        file("empty.txt", "text/plain", "")
                )
        );
        assertEquals(40002, empty.getCode());

        BusinessException oversized = assertThrows(
                BusinessException.class,
                () -> validator(DataSize.ofBytes(3)).validate(
                        file("large.txt", "text/plain", "1234")
                )
        );
        assertEquals(41301, oversized.getCode());

        BusinessException unsupported = assertThrows(
                BusinessException.class,
                () -> validator(DataSize.ofKilobytes(1)).validate(
                        file("run.exe", "application/x-msdownload", "MZ")
                )
        );
        assertEquals(40003, unsupported.getCode());
    }

    @Test
    void shouldRejectBinaryDisguisedAsTxtAndDoubleExtension() {
        BusinessException binary = assertThrows(
                BusinessException.class,
                () -> validator(DataSize.ofKilobytes(1)).validate(
                        new MockMultipartFile(
                                "file",
                                "run.txt",
                                "application/octet-stream",
                                new byte[]{'M', 'Z', 1, 2}
                        )
                )
        );
        assertEquals(40003, binary.getCode());

        BusinessException doubleExtension = assertThrows(
                BusinessException.class,
                () -> validator(DataSize.ofKilobytes(1)).validate(
                        file("run.exe.txt", "text/plain", "plain")
                )
        );
        assertEquals(40002, doubleExtension.getCode());
    }

    @Test
    void shouldRejectMismatchedMimeTypeAndInvalidUtf8() {
        BusinessException mime = assertThrows(
                BusinessException.class,
                () -> validator(DataSize.ofKilobytes(1)).validate(
                        file("guide.md", "application/pdf", "# 标题")
                )
        );
        assertEquals(40003, mime.getCode());

        BusinessException invalidUtf8 = assertThrows(
                BusinessException.class,
                () -> validator(DataSize.ofKilobytes(1)).validate(
                        new MockMultipartFile(
                                "file",
                                "bad.txt",
                                "text/plain",
                                new byte[]{(byte) 0xC3, (byte) 0x28}
                        )
                )
        );
        assertEquals(40003, invalidUtf8.getCode());
    }

    private DocumentUploadValidator validator(DataSize maxSize) {
        return new DocumentUploadValidator(new DocumentIngestionProperties(
                tempDir.toString(),
                maxSize,
                List.of("TXT", "MARKDOWN")
        ));
    }

    private MockMultipartFile file(
            String name,
            String contentType,
            String content
    ) {
        return new MockMultipartFile(
                "file",
                name,
                contentType,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
