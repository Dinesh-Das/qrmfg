package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.dto.DocumentAccessLogDto;
import com.cqs.qrmfg.dto.DocumentSummary;
import com.cqs.qrmfg.exception.DocumentException;
import com.cqs.qrmfg.exception.DocumentNotFoundException;
import com.cqs.qrmfg.exception.WorkflowException;
import com.cqs.qrmfg.model.DocumentAccessLog;
import com.cqs.qrmfg.model.DocumentAccessType;
import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.WorkflowDocument;
import com.cqs.qrmfg.repository.DocumentAccessLogRepository;
import com.cqs.qrmfg.repository.WorkflowRepository;
import com.cqs.qrmfg.repository.WorkflowDocumentRepository;
import com.cqs.qrmfg.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private WorkflowDocumentRepository workflowDocumentRepository;

    @Autowired
    private WorkflowRepository materialWorkflowRepository;

    @Autowired
    private DocumentAccessLogRepository documentAccessLogRepository;

    @Value("${app.document.storage.path:app}")
    private String documentStoragePath;

    @Value("${app.document.max.size:26214400}") // 25MB in bytes
    private long maxFileSize;

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList("pdf", "docx", "xlsx", "doc", "xls");

    @Override
    public List<DocumentSummary> uploadDocuments(MultipartFile[] files, String projectCode, String materialCode, Long workflowId, String uploadedBy) {
        MaterialWorkflow workflow = materialWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowException("Workflow not found with ID: " + workflowId));

        List<DocumentSummary> uploadedDocuments = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!isValidFile(file)) {
                throw new DocumentException("Invalid file: " + file.getOriginalFilename());
            }

            try {
                String fileName = storeFile(file, projectCode, materialCode);
                
                WorkflowDocument document = new WorkflowDocument();
                document.setWorkflow(workflow);
                document.setFileName(fileName);
                document.setOriginalFileName(file.getOriginalFilename());
                document.setFilePath(getFilePath(projectCode, materialCode, fileName));
                document.setFileType(getFileExtension(file.getOriginalFilename()));
                document.setFileSize(file.getSize());
                document.setUploadedBy(uploadedBy);
                document.setUploadedAt(LocalDateTime.now());
                document.setIsReused(false);

                WorkflowDocument savedDocument = workflowDocumentRepository.save(document);
                uploadedDocuments.add(convertToDocumentSummary(savedDocument));

            } catch (IOException e) {
                throw new DocumentException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }

        return uploadedDocuments;
    }

    @Override
    public List<DocumentSummary> getWorkflowDocuments(Long workflowId) {
        List<WorkflowDocument> documents = workflowDocumentRepository.findByWorkflowId(workflowId);
        return documents.stream()
                .map(this::convertToDocumentSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentSummary> getReusableDocuments(String projectCode, String materialCode) {
        List<WorkflowDocument> documents = workflowDocumentRepository.findReusableDocuments(projectCode, materialCode);
        return documents.stream()
                .map(this::convertToDocumentSummary)
                .collect(Collectors.toList());
    }

    @Override
    public List<DocumentSummary> reuseDocuments(Long workflowId, List<Long> documentIds, String reuseBy) {
        MaterialWorkflow workflow = materialWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowException("Workflow not found with ID: " + workflowId));

        List<DocumentSummary> reusedDocuments = new ArrayList<>();

        for (Long documentId : documentIds) {
            WorkflowDocument originalDocument = workflowDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new DocumentNotFoundException(documentId));

            WorkflowDocument reusedDocument = new WorkflowDocument();
            reusedDocument.setWorkflow(workflow);
            reusedDocument.setFileName(originalDocument.getFileName());
            reusedDocument.setOriginalFileName(originalDocument.getOriginalFileName());
            reusedDocument.setFilePath(originalDocument.getFilePath());
            reusedDocument.setFileType(originalDocument.getFileType());
            reusedDocument.setFileSize(originalDocument.getFileSize());
            reusedDocument.setUploadedBy(reuseBy);
            reusedDocument.setUploadedAt(LocalDateTime.now());
            reusedDocument.setIsReused(true);
            reusedDocument.setOriginalDocumentId(documentId);

            WorkflowDocument savedDocument = workflowDocumentRepository.save(reusedDocument);
            reusedDocuments.add(convertToDocumentSummary(savedDocument));
        }

        return reusedDocuments;
    }

    @Override
    public Resource downloadDocument(Long documentId) {
        WorkflowDocument document = workflowDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        try {
            Path filePath = Paths.get(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new DocumentException("File not found or not readable: " + document.getFileName());
            }
        } catch (Exception e) {
            throw new DocumentException("Error downloading file: " + document.getFileName(), e);
        }
    }

    @Override
    public WorkflowDocument getDocumentById(Long documentId) {
        return workflowDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));
    }

    @Override
    public void deleteDocument(Long documentId, String deletedBy) {
        WorkflowDocument document = getDocumentById(documentId);
        
        // Only delete the file if it's not reused by other workflows
        if (!document.getIsReused()) {
            try {
                Path filePath = Paths.get(document.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log the error but don't fail the deletion
                System.err.println("Failed to delete physical file: " + document.getFilePath());
            }
        }

        workflowDocumentRepository.delete(document);
    }

    @Override
    public boolean isValidFile(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }

        if (file.getSize() > maxFileSize) {
            return false;
        }

        String fileExtension = getFileExtension(file.getOriginalFilename());
        return ALLOWED_FILE_TYPES.contains(fileExtension.toLowerCase());
    }

    @Override
    public long getDocumentCount(Long workflowId) {
        return workflowDocumentRepository.countByWorkflowId(workflowId);
    }

    private String storeFile(MultipartFile file, String projectCode, String materialCode) throws IOException {
        // Create directory structure: app/{projectCode}/{materialCode}/
        Path uploadPath = Paths.get(documentStoragePath, projectCode, materialCode);
        Files.createDirectories(uploadPath);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;

        // Store the file
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFilename;
    }

    private String getFilePath(String projectCode, String materialCode, String fileName) {
        return Paths.get(documentStoragePath, projectCode, materialCode, fileName).toString();
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    @Override
    public Resource downloadDocumentSecure(Long documentId, String userId, String ipAddress, String userAgent, Long workflowId) {
        WorkflowDocument document = workflowDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        // Check access permissions
        boolean hasAccess = hasDocumentAccess(documentId, userId, workflowId);
        
        if (!hasAccess) {
            logDocumentAccess(documentId, userId, DocumentAccessType.UNAUTHORIZED_ATTEMPT, 
                            ipAddress, userAgent, workflowId, false, "Access denied - insufficient permissions");
            throw new DocumentException("Access denied to document: " + documentId);
        }

        // Log successful access
        logDocumentAccess(documentId, userId, DocumentAccessType.DOWNLOAD, 
                        ipAddress, userAgent, workflowId, true, null);

        try {
            Path filePath = Paths.get(document.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new DocumentException("File not found or not readable: " + document.getFileName());
            }
        } catch (Exception e) {
            throw new DocumentException("Error downloading file: " + document.getFileName(), e);
        }
    }

    @Override
    public boolean hasDocumentAccess(Long documentId, String userId, Long workflowId) {
        WorkflowDocument document = workflowDocumentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(documentId));

        // Check if user has access to the workflow that contains this document
        if (workflowId != null && !document.getWorkflow().getId().equals(workflowId)) {
            // Check if user has access to the specific workflow
            MaterialWorkflow workflow = materialWorkflowRepository.findById(workflowId).orElse(null);
            if (workflow == null) {
                return false;
            }
            
            // Additional access control logic can be added here based on user roles
            // For now, we allow access if the workflow exists and user is authenticated
        }

        return true; // Basic implementation - can be enhanced with role-based access control
    }

    @Override
    public void logDocumentAccess(Long documentId, String userId, DocumentAccessType accessType, 
                                String ipAddress, String userAgent, Long workflowId, 
                                boolean accessGranted, String denialReason) {
        WorkflowDocument document = workflowDocumentRepository.findById(documentId).orElse(null);
        if (document != null) {
            DocumentAccessLog accessLog = new DocumentAccessLog(
                document, userId, accessType, LocalDateTime.now(), 
                ipAddress, userAgent, workflowId, accessGranted, denialReason
            );
            documentAccessLogRepository.save(accessLog);
        }
    }

    @Override
    public List<DocumentAccessLogDto> getDocumentAccessLogs(Long documentId) {
        List<DocumentAccessLog> logs = documentAccessLogRepository.findByDocumentIdOrderByAccessTimeDesc(documentId);
        return logs.stream()
                .map(this::convertToDocumentAccessLogDto)
                .collect(Collectors.toList());
    }

    @Override
    public DocumentSummary getEnhancedDocumentSummary(Long documentId) {
        WorkflowDocument document = getDocumentById(documentId);
        
        // Get download count
        long downloadCount = documentAccessLogRepository.countDocumentAccess(documentId, DocumentAccessType.DOWNLOAD);
        
        // Get last access time
        List<DocumentAccessLog> recentLogs = documentAccessLogRepository.findRecentAccessLogs(
            documentId, LocalDateTime.now().minusDays(30));
        LocalDateTime lastAccessedAt = recentLogs.isEmpty() ? null : recentLogs.get(0).getAccessTime();

        return convertToEnhancedDocumentSummary(document, downloadCount, lastAccessedAt);
    }

    @Override
    public List<DocumentSummary> getReusableDocumentsEnhanced(String projectCode, String materialCode) {
        List<WorkflowDocument> documents = workflowDocumentRepository.findReusableDocuments(projectCode, materialCode);
        return documents.stream()
                .map(doc -> {
                    long downloadCount = documentAccessLogRepository.countDocumentAccess(doc.getId(), DocumentAccessType.DOWNLOAD);
                    List<DocumentAccessLog> recentLogs = documentAccessLogRepository.findRecentAccessLogs(
                        doc.getId(), LocalDateTime.now().minusDays(30));
                    LocalDateTime lastAccessedAt = recentLogs.isEmpty() ? null : recentLogs.get(0).getAccessTime();
                    return convertToEnhancedDocumentSummary(doc, downloadCount, lastAccessedAt);
                })
                .collect(Collectors.toList());
    }

    private DocumentAccessLogDto convertToDocumentAccessLogDto(DocumentAccessLog log) {
        return new DocumentAccessLogDto(
            log.getId(),
            log.getAccessedBy(),
            log.getAccessType(),
            log.getAccessTime(),
            log.getIpAddress(),
            log.getAccessGranted(),
            log.getDenialReason(),
            log.getDocument().getOriginalFileName(),
            log.getWorkflowId()
        );
    }

    private DocumentSummary convertToEnhancedDocumentSummary(WorkflowDocument document, long downloadCount, LocalDateTime lastAccessedAt) {
        MaterialWorkflow workflow = document.getWorkflow();
        return new DocumentSummary(
            document.getId(),
            document.getFileName(),
            document.getOriginalFileName(),
            document.getFileType(),
            document.getFileSize(),
            document.getUploadedBy(),
            document.getUploadedAt(),
            document.getIsReused(),
            "/qrmfg/api/v1/documents/" + document.getId() + "/download",
            document.getOriginalDocumentId(),
            workflow.getProjectCode(),
            workflow.getMaterialCode(),
            workflow.getPlantCode(),
            workflow.getBlockId(),
            downloadCount,
            lastAccessedAt
        );
    }

    private DocumentSummary convertToDocumentSummary(WorkflowDocument document) {
        return new DocumentSummary(
            document.getId(),
            document.getFileName(),
            document.getOriginalFileName(),
            document.getFileType(),
            document.getFileSize(),
            document.getUploadedBy(),
            document.getUploadedAt(),
            document.getIsReused(),
            "/qrmfg/api/v1/documents/" + document.getId() + "/download"
        );
    }
}