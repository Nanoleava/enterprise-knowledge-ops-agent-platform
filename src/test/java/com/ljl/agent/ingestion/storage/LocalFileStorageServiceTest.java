package com.ljl.agent.ingestion.storage;

import com.ljl.agent.config.DocumentIngestionProperties;
import com.ljl.agent.ingestion.DocumentFileType;
import com.ljl.agent.ingestion.ValidatedUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreWithUuidRelativePathChecksumAndDelete() throws Exception {
        byte[] content = "安全正文".getBytes(StandardCharsets.UTF_8);
        LocalFileStorageService storage = storage();
        StoredFile stored = storage.save(
                7L,
                9L,
                new MockMultipartFile(
                        "file",
                        "../../application.txt",
                        "text/plain",
                        content
                ),
                new ValidatedUpload(
                        "application.txt",
                        DocumentFileType.TXT,
                        content.length
                )
        );

        assertTrue(stored.relativePath().matches("7/9/[0-9a-f]{32}\\.txt"));
        assertFalse(stored.storedFileName().contains("application"));
        assertEquals(
                HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(content)
                ),
                stored.sha256()
        );
        Path storedPath = storage.resolveForRead(stored.relativePath());
        assertTrue(storedPath.startsWith(tempDir.toAbsolutePath().normalize()));
        assertEquals("安全正文", Files.readString(storedPath));

        storage.deleteIfExists(stored.relativePath());
        assertFalse(Files.exists(storedPath));
        assertFalse(Files.exists(tempDir.resolve("7")));
    }

    @Test
    void shouldRejectTraversalAndAbsolutePaths() {
        LocalFileStorageService storage = storage();

        assertThrows(
                DocumentStorageException.class,
                () -> storage.resolveForRead("../../application.yml")
        );
        assertThrows(
                DocumentStorageException.class,
                () -> storage.resolveForRead(
                        tempDir.resolve("outside.txt").toAbsolutePath().toString()
                )
        );
    }

    private LocalFileStorageService storage() {
        return new LocalFileStorageService(
                new DocumentIngestionProperties(
                        tempDir.toString(),
                        DataSize.ofMegabytes(1),
                        List.of("TXT", "MARKDOWN")
                )
        );
    }
}
