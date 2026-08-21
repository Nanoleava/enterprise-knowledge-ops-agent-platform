package com.ljl.agent.ingestion;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.config.DocumentIngestionProperties;
import com.ljl.agent.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * 同时校验文件名、扩展名、声明类型和 UTF-8 文本特征。
 */
@Component
public class DocumentUploadValidator {

    private static final int MAX_ORIGINAL_FILE_NAME_LENGTH = 255;

    private final DocumentIngestionProperties properties;

    public DocumentUploadValidator(DocumentIngestionProperties properties) {
        this.properties = properties;
    }

    public ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_FILE_INVALID,
                    "上传文件不能为空"
            );
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_FILE_TOO_LARGE,
                    "上传文件超过允许大小"
            );
        }

        String originalFileName = normalizeOriginalFileName(
                file.getOriginalFilename()
        );
        String extension = extensionOf(originalFileName);
        DocumentFileType fileType =
                DocumentFileType.fromExtension(extension);
        if (fileType == null || !properties.isAllowed(fileType)) {
            throw unsupportedType();
        }
        if (!fileType.acceptsMediaType(file.getContentType())) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_FILE_TYPE_UNSUPPORTED,
                    "文件声明类型与扩展名不匹配"
            );
        }

        inspectTextContent(file);
        return new ValidatedUpload(
                originalFileName,
                fileType,
                file.getSize()
        );
    }

    private String normalizeOriginalFileName(String rawName) {
        if (rawName == null || rawName.isBlank() || rawName.indexOf('\0') >= 0) {
            throw invalidFileName();
        }
        try {
            Path fileNamePath = Path.of(rawName.trim()).getFileName();
            if (fileNamePath == null) {
                throw invalidFileName();
            }
            String fileName = fileNamePath.toString();
            if (fileName.isBlank()
                    || ".".equals(fileName)
                    || "..".equals(fileName)
                    || fileName.length() > MAX_ORIGINAL_FILE_NAME_LENGTH) {
                throw invalidFileName();
            }
            return fileName;
        } catch (InvalidPathException exception) {
            throw invalidFileName();
        }
    }

    private String extensionOf(String fileName) {
        int separator = fileName.lastIndexOf('.');
        if (separator <= 0 || separator == fileName.length() - 1) {
            throw unsupportedType();
        }
        String stem = fileName.substring(0, separator);
        if (stem.contains(".")) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_FILE_INVALID,
                    "不允许使用双扩展名文件"
            );
        }
        return fileName.substring(separator + 1);
    }

    private void inspectTextContent(MultipartFile file) {
        byte[] signature = new byte[8];
        int signatureLength;
        try (InputStream input = file.getInputStream()) {
            signatureLength = input.read(signature);
        } catch (IOException exception) {
            throw ingestionFailure(exception);
        }

        if (hasKnownBinarySignature(signature, signatureLength)) {
            throw unsupportedType();
        }

        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (Reader reader = new InputStreamReader(
                file.getInputStream(),
                decoder
        )) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                for (int index = 0; index < read; index++) {
                    char value = buffer[index];
                    if (value == '\0'
                            || (Character.isISOControl(value)
                            && value != '\n'
                            && value != '\r'
                            && value != '\t'
                            && value != '\f')) {
                        throw unsupportedType();
                    }
                }
            }
        } catch (CharacterCodingException exception) {
            throw new BusinessException(
                    ErrorCode.DOCUMENT_FILE_TYPE_UNSUPPORTED,
                    "文本文件必须使用 UTF-8 编码"
            );
        } catch (IOException exception) {
            if (exception.getCause() instanceof CharacterCodingException) {
                throw new BusinessException(
                        ErrorCode.DOCUMENT_FILE_TYPE_UNSUPPORTED,
                        "文本文件必须使用 UTF-8 编码"
                );
            }
            throw ingestionFailure(exception);
        }
    }

    private boolean hasKnownBinarySignature(byte[] bytes, int length) {
        if (length >= 2 && bytes[0] == 'M' && bytes[1] == 'Z') {
            return true;
        }
        if (length >= 4
                && bytes[0] == 0x7f
                && bytes[1] == 'E'
                && bytes[2] == 'L'
                && bytes[3] == 'F') {
            return true;
        }
        if (length >= 2 && bytes[0] == 'P' && bytes[1] == 'K') {
            return true;
        }
        return length >= 5
                && ByteBuffer.wrap(bytes, 0, 5)
                .equals(ByteBuffer.wrap("%PDF-".getBytes(StandardCharsets.US_ASCII)));
    }

    private BusinessException invalidFileName() {
        return new BusinessException(
                ErrorCode.DOCUMENT_FILE_INVALID,
                "上传文件名不合法"
        );
    }

    private BusinessException unsupportedType() {
        return new BusinessException(
                ErrorCode.DOCUMENT_FILE_TYPE_UNSUPPORTED,
                "DAY 1 仅支持 UTF-8 TXT 和 Markdown 文件"
        );
    }

    private BusinessException ingestionFailure(IOException cause) {
        return new BusinessException(
                ErrorCode.DOCUMENT_INGESTION_FAILED,
                "读取上传文件失败",
                cause
        );
    }
}
