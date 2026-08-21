package com.ljl.agent.dto.response;

import com.ljl.agent.entity.Document;

import java.time.LocalDateTime;

public class DocumentProcessStatusVO {

    private Long documentId;
    private String parseStatus;
    private String chunkStatus;
    private long chunkCount;
    private String errorMessage;
    private LocalDateTime processedAt;

    public static DocumentProcessStatusVO from(
            Document document,
            long chunkCount
    ) {
        DocumentProcessStatusVO status = new DocumentProcessStatusVO();
        status.setDocumentId(document.getId());
        status.setParseStatus(document.getParseStatus());
        status.setChunkStatus(document.getChunkStatus());
        status.setChunkCount(chunkCount);
        status.setErrorMessage(document.getProcessError());
        status.setProcessedAt(document.getProcessedAt());
        return status;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getChunkStatus() {
        return chunkStatus;
    }

    public void setChunkStatus(String chunkStatus) {
        this.chunkStatus = chunkStatus;
    }

    public long getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(long chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
