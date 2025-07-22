package com.cqs.qrmfg.model;

import org.hibernate.envers.Audited;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "qrmfg_workflow_documents")
@Audited
public class WorkflowDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workflow_document_seq")
    @SequenceGenerator(name = "workflow_document_seq", sequenceName = "WORKFLOW_DOCUMENT_SEQ", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private MaterialWorkflow workflow;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_type", nullable = false, length = 10)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "is_reused", nullable = false)
    private Boolean isReused = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_document_id")
    private WorkflowDocument originalDocument;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    public WorkflowDocument() {}

    public WorkflowDocument(MaterialWorkflow workflow, String fileName, String originalFileName, 
                           String filePath, String fileType, Long fileSize, String uploadedBy) {
        this.workflow = workflow;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.createdBy = uploadedBy;
        this.updatedBy = uploadedBy;
    }

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        lastModified = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }

    // Business logic methods
    public String getFileExtension() {
        if (originalFileName != null && originalFileName.contains(".")) {
            return originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toLowerCase();
        }
        return "";
    }

    public String getFormattedFileSize() {
        if (fileSize == null) return "0 B";
        
        long bytes = fileSize;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public boolean isValidFileType() {
        String extension = getFileExtension();
        return "pdf".equals(extension) || "docx".equals(extension) || "xlsx".equals(extension);
    }

    public String getStorageDirectory() {
        if (workflow != null) {
            return String.format("app/%s/%s/", workflow.getProjectCode(), workflow.getMaterialCode());
        }
        return "app/unknown/";
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MaterialWorkflow getWorkflow() { return workflow; }
    public void setWorkflow(MaterialWorkflow workflow) { this.workflow = workflow; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public Boolean getIsReused() { return isReused; }
    public void setIsReused(Boolean isReused) { this.isReused = isReused; }

    public WorkflowDocument getOriginalDocument() { return originalDocument; }
    public void setOriginalDocument(WorkflowDocument originalDocument) { this.originalDocument = originalDocument; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    // Convenience methods for backward compatibility
    public Long getOriginalDocumentId() {
        return originalDocument != null ? originalDocument.getId() : null;
    }

    public void setOriginalDocumentId(Long originalDocumentId) {
        if (originalDocumentId != null) {
            WorkflowDocument original = new WorkflowDocument();
            original.setId(originalDocumentId);
            this.originalDocument = original;
        } else {
            this.originalDocument = null;
        }
    }

    @Override
    public String toString() {
        return String.format("WorkflowDocument{id=%d, fileName='%s', fileType='%s', fileSize=%d, isReused=%s}", 
                           id, fileName, fileType, fileSize, isReused);
    }
}