package com.ljl.agent.dto.response;

import com.ljl.agent.entity.Document;

public class DocumentUploadResponse {

    private Long documentId;
    private String title;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String parseStatus;
    private String chunkStatus;

    public static DocumentUploadResponse from(Document document) {
        DocumentUploadResponse response = new DocumentUploadResponse();
        response.setDocumentId(document.getId());
        response.setTitle(document.getTitle());
        response.setOriginalFileName(document.getOriginalFileName());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setParseStatus(document.getParseStatus());
        response.setChunkStatus(document.getChunkStatus());
        return response;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
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
}
