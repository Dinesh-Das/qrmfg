package com.cqs.qrmfg.dto;

import java.time.LocalDateTime;

/**
 * DTO for document summary information
 */
public class DocumentSummary {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private Boolean isReused;
    private String downloadUrl;
    private Long originalDocumentId;
    private String projectCode;
    private String materialCode;
    private String plantCode;
    private String blockId;
    private Long downloadCount;
    private LocalDateTime lastAccessedAt;

    public DocumentSummary() {}

    public DocumentSummary(Long id, String fileName, String originalFileName, String fileType, 
                          Long fileSize, String uploadedBy, LocalDateTime uploadedAt, 
                          Boolean isReused, String downloadUrl) {
        this.id = id;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.isReused = isReused;
        this.downloadUrl = downloadUrl;
    }

    public DocumentSummary(Long id, String fileName, String originalFileName, String fileType, 
                          Long fileSize, String uploadedBy, LocalDateTime uploadedAt, 
                          Boolean isReused, String downloadUrl, Long originalDocumentId,
                          String projectCode, String materialCode, String plantCode, String blockId,
                          Long downloadCount, LocalDateTime lastAccessedAt) {
        this.id = id;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.isReused = isReused;
        this.downloadUrl = downloadUrl;
        this.originalDocumentId = originalDocumentId;
        this.projectCode = projectCode;
        this.materialCode = materialCode;
        this.plantCode = plantCode;
        this.blockId = blockId;
        this.downloadCount = downloadCount;
        this.lastAccessedAt = lastAccessedAt;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Boolean getIsReused() {
        return isReused;
    }

    public void setIsReused(Boolean isReused) {
        this.isReused = isReused;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public Long getOriginalDocumentId() {
        return originalDocumentId;
    }

    public void setOriginalDocumentId(Long originalDocumentId) {
        this.originalDocumentId = originalDocumentId;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getPlantCode() {
        return plantCode;
    }

    public void setPlantCode(String plantCode) {
        this.plantCode = plantCode;
    }

    public String getBlockId() {
        return blockId;
    }

    public void setBlockId(String blockId) {
        this.blockId = blockId;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
}