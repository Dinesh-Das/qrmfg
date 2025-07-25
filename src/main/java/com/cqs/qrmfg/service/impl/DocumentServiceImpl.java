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

    @Value("${app.document.storage.path:./documents}")
    private String documentStoragePath;

    @Value("${app.document.max.size:26214400}") // 25MB in bytes
    private long maxFileSize;

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList("pdf", "docx", "xlsx", "doc", "xls");

    @Override
    public List<DocumentSummary> uploadDocuments(MultipartFile[] files, String projectCode, String materialCode, Long workflowId, String uploadedBy) {
        MaterialWorkflow workflow = materialWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowException("Workflow not found with ID: " + workflowId));

        List<DocumentSummary> uploadedDocuments = new ArrayList<>();

        // Ensure the directory structure exists
        ensureDirectoryExists(projectCode, materialCode);

        for (MultipartFile file : files) {
            if (!isValidFile(file)) {
                throw new DocumentException("Invalid file: " + file.getOriginalFilename());
            }

            try {
                String fileName = storeFile(file, projectCode, materialCode);
                String filePath = getFilePath(projectCode, materialCode, fileName);
                
                System.out.println("Storing document: " + file.getOriginalFilename() + 
                    " at path: " + filePath + 
                    " for project: " + projectCode + 
                    ", material: " + materialCode);
                
                WorkflowDocument document = new WorkflowDocument();
                document.setWorkflow(workflow);
                document.setFileName(fileName);
                document.setOriginalFileName(file.getOriginalFilename());
                document.setFilePath(filePath);
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
        try {
            System.out.println("=== File Storage Debug ===");
            System.out.println("Document storage path: " + documentStoragePath);
            System.out.println("Project code: " + projectCode);
            System.out.println("Material code: " + materialCode);
            System.out.println("Original filename: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());
            
            // Create directory structure: documents/{projectCode}/{materialCode}/
            Path uploadPath = Paths.get(documentStoragePath, projectCode, materialCode);
            System.out.println("Upload path: " + uploadPath.toAbsolutePath());
            
            // Check if parent directories exist and are writable
            Path parentPath = uploadPath.getParent();
            if (parentPath != null) {
                System.out.println("Parent path: " + parentPath.toAbsolutePath());
                System.out.println("Parent exists: " + Files.exists(parentPath));
                System.out.println("Parent is directory: " + Files.isDirectory(parentPath));
                System.out.println("Parent is writable: " + Files.isWritable(parentPath));
            }
            
            Files.createDirectories(uploadPath);
            System.out.println("Directory created successfully: " + uploadPath.toAbsolutePath());
            System.out.println("Upload path exists: " + Files.exists(uploadPath));
            System.out.println("Upload path is directory: " + Files.isDirectory(uploadPath));
            System.out.println("Upload path is writable: " + Files.isWritable(uploadPath));

            // Generate unique filename with timestamp for better organization
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String uniqueFilename = timestamp + "_" + UUID.randomUUID().toString() + "." + fileExtension;
            System.out.println("Generated unique filename: " + uniqueFilename);

            // Store the file
            Path filePath = uploadPath.resolve(uniqueFilename);
            System.out.println("Full file path: " + filePath.toAbsolutePath());
            
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File stored successfully at: " + filePath.toAbsolutePath());
            
            // Verify file was created
            System.out.println("File exists after creation: " + Files.exists(filePath));
            System.out.println("File size after creation: " + Files.size(filePath));

            return uniqueFilename;
            
        } catch (IOException e) {
            System.err.println("Error storing file: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private String getFilePath(String projectCode, String materialCode, String fileName) {
        return Paths.get(documentStoragePath, projectCode, materialCode, fileName).toString();
    }

    private String getAbsoluteFilePath(String projectCode, String materialCode, String fileName) {
        return Paths.get(documentStoragePath, projectCode, materialCode, fileName).toAbsolutePath().toString();
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private void ensureDirectoryExists(String projectCode, String materialCode) {
        try {
            System.out.println("=== Directory Creation Debug ===");
            System.out.println("Document storage path: " + documentStoragePath);
            System.out.println("Current working directory: " + System.getProperty("user.dir"));
            
            Path uploadPath = Paths.get(documentStoragePath, projectCode, materialCode);
            System.out.println("Target upload path: " + uploadPath.toAbsolutePath());
            
            // Check if base storage path exists
            Path basePath = Paths.get(documentStoragePath);
            System.out.println("Base storage path: " + basePath.toAbsolutePath());
            System.out.println("Base path exists: " + Files.exists(basePath));
            
            if (!Files.exists(basePath)) {
                System.out.println("Creating base storage directory...");
                Files.createDirectories(basePath);
                System.out.println("Base storage directory created: " + Files.exists(basePath));
            }
            
            Files.createDirectories(uploadPath);
            System.out.println("Created/verified directory structure: " + uploadPath.toAbsolutePath());
            System.out.println("Directory exists: " + Files.exists(uploadPath));
            System.out.println("Directory is writable: " + Files.isWritable(uploadPath));
            
        } catch (IOException e) {
            System.err.println("Failed to create directory structure: " + e.getMessage());
            e.printStackTrace();
            throw new DocumentException("Failed to create directory structure for project: " + 
                projectCode + ", material: " + materialCode, e);
        }
    }

    @Override
    public java.util.Map<String, Object> getStorageInfo() {
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        
        try {
            // Get current working directory
            String currentDir = System.getProperty("user.dir");
            info.put("currentWorkingDirectory", currentDir);
            info.put("configuredStoragePath", documentStoragePath);
            
            // Get absolute path of storage directory
            Path storagePath = Paths.get(documentStoragePath);
            info.put("absoluteStoragePath", storagePath.toAbsolutePath().toString());
            info.put("storagePathExists", Files.exists(storagePath));
            
            // Test directory creation
            String testProjectCode = "SER-A-123456";
            String testMaterialCode = "R12345A";
            Path testPath = Paths.get(documentStoragePath, testProjectCode, testMaterialCode);
            
            info.put("testProjectPath", testPath.toAbsolutePath().toString());
            info.put("testProjectPathExists", Files.exists(testPath));
            
            // Create test directory
            Files.createDirectories(testPath);
            info.put("testDirectoryCreated", Files.exists(testPath));
            
            // List existing directories
            if (Files.exists(storagePath)) {
                java.util.List<String> existingDirs = new java.util.ArrayList<>();
                try (java.util.stream.Stream<Path> paths = Files.list(storagePath)) {
                    paths.filter(Files::isDirectory)
                         .forEach(path -> existingDirs.add(path.getFileName().toString()));
                }
                info.put("existingProjectDirectories", existingDirs);
            }
            
        } catch (Exception e) {
            info.put("error", e.getMessage());
            info.put("errorType", e.getClass().getSimpleName());
        }
        
        return info;
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