package com.ljl.agent.ingestion.storage;

import com.ljl.agent.config.DocumentIngestionProperties;
import com.ljl.agent.ingestion.ValidatedUpload;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 使用临时文件与服务端 UUID 文件名实现的本地安全存储。
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path storageRoot;
    private final long maxFileSizeBytes;

    public LocalFileStorageService(DocumentIngestionProperties properties) {
        this.storageRoot = initializeRoot(properties.getStorageRoot());
        this.maxFileSizeBytes = properties.getMaxFileSizeBytes();
    }

    @Override
    public StoredFile save(
            Long userId,
            Long knowledgeBaseId,
            MultipartFile file,
            ValidatedUpload validatedUpload
    ) {
        requirePositive(userId, "用户ID");
        requirePositive(knowledgeBaseId, "知识库ID");
        if (file == null || validatedUpload == null) {
            throw new DocumentStorageException("缺少待存储文件");
        }

        String storedFileName = UUID.randomUUID()
                .toString()
                .replace("-", "")
                + "."
                + validatedUpload.fileType().canonicalExtension();
        Path directory = resolveInsideRoot(
                Path.of(userId.toString(), knowledgeBaseId.toString())
        );
        Path finalPath = resolveInsideRoot(
                storageRoot.relativize(directory).resolve(storedFileName)
        );
        Path temporaryPath = resolveInsideRoot(
                storageRoot.relativize(directory).resolve(
                        ".upload-" + UUID.randomUUID() + ".tmp"
                )
        );

        try {
            Files.createDirectories(directory);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream source = new DigestInputStream(
                    file.getInputStream(),
                    digest
            ); OutputStream target = Files.newOutputStream(temporaryPath)) {
                byte[] buffer = new byte[8192];
                long copied = 0;
                int read;
                while ((read = source.read(buffer)) != -1) {
                    copied += read;
                    if (copied > maxFileSizeBytes) {
                        throw new DocumentStorageException(
                                "文件实际大小超过允许上限"
                        );
                    }
                    target.write(buffer, 0, read);
                }
            }

            long actualSize = Files.size(temporaryPath);
            if (actualSize <= 0 || actualSize > maxFileSizeBytes) {
                throw new DocumentStorageException(
                        "文件实际大小不在允许范围内"
                );
            }
            if (actualSize != validatedUpload.fileSize()) {
                throw new DocumentStorageException("文件大小在上传过程中发生变化");
            }

            moveIntoPlace(temporaryPath, finalPath);
            return new StoredFile(
                    storedFileName,
                    validatedUpload.fileType(),
                    actualSize,
                    toPortableRelativePath(finalPath),
                    HexFormat.of().formatHex(digest.digest())
            );
        } catch (DocumentStorageException exception) {
            cleanupFailedWrite(temporaryPath, finalPath);
            throw exception;
        } catch (IOException | NoSuchAlgorithmException exception) {
            cleanupFailedWrite(temporaryPath, finalPath);
            throw new DocumentStorageException("文档文件存储失败", exception);
        }
    }

    @Override
    public Path resolveForRead(String relativePath) {
        Path resolved = resolveRelativeString(relativePath);
        if (!Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
            throw new DocumentStorageException("文档源文件不可用");
        }
        return resolved;
    }

    @Override
    public void deleteIfExists(String relativePath) {
        Path resolved = resolveRelativeString(relativePath);
        try {
            Files.deleteIfExists(resolved);
            pruneEmptyParents(resolved.getParent());
        } catch (IOException exception) {
            throw new DocumentStorageException("文档文件清理失败", exception);
        }
    }

    private Path initializeRoot(Path configuredRoot) {
        Path normalized = configuredRoot.toAbsolutePath().normalize();
        try {
            Files.createDirectories(normalized);
            if (!Files.isDirectory(normalized) || !Files.isWritable(normalized)) {
                throw new IllegalStateException("文档存储根目录不可写");
            }
            return normalized;
        } catch (IOException exception) {
            throw new IllegalStateException("无法初始化文档存储根目录", exception);
        }
    }

    private Path resolveRelativeString(String relativePath) {
        if (relativePath == null
                || relativePath.isBlank()
                || relativePath.indexOf('\0') >= 0) {
            throw new DocumentStorageException("文档相对路径不合法");
        }
        try {
            Path path = Path.of(relativePath);
            if (path.isAbsolute()) {
                throw new DocumentStorageException("文档相对路径不合法");
            }
            return resolveInsideRoot(path);
        } catch (InvalidPathException exception) {
            throw new DocumentStorageException("文档相对路径不合法", exception);
        }
    }

    private Path resolveInsideRoot(Path relativePath) {
        Path resolved = storageRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new DocumentStorageException("文档路径越出受控存储目录");
        }
        return resolved;
    }

    private void moveIntoPlace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private String toPortableRelativePath(Path path) {
        return storageRoot.relativize(path)
                .toString()
                .replace('\\', '/');
    }

    private void cleanupFailedWrite(Path temporaryPath, Path finalPath) {
        try {
            Files.deleteIfExists(temporaryPath);
            Files.deleteIfExists(finalPath);
            pruneEmptyParents(finalPath.getParent());
        } catch (IOException ignored) {
            // 保留原异常；上层只记录安全摘要，不暴露物理路径。
        }
    }

    private void pruneEmptyParents(Path directory) throws IOException {
        Path current = directory;
        while (current != null && !current.equals(storageRoot)) {
            try (var entries = Files.list(current)) {
                if (entries.findAny().isPresent()) {
                    return;
                }
            }
            Files.deleteIfExists(current);
            current = current.getParent();
        }
    }

    private void requirePositive(Long value, String name) {
        if (value == null || value <= 0) {
            throw new DocumentStorageException(name + "必须是正整数");
        }
    }
}
