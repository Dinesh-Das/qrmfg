package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.DocumentAccessLogDto;
import com.cqs.qrmfg.dto.DocumentReuseRequest;
import com.cqs.qrmfg.dto.DocumentSummary;
import com.cqs.qrmfg.exception.DocumentException;
import com.cqs.qrmfg.exception.DocumentNotFoundException;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.Document;
import com.cqs.qrmfg.repository.DocumentRepository;
import com.cqs.qrmfg.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for document management operations
 */
@RestController
@RequestMapping("/qrmfg/api/v1/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    /**
     * Upload documents for a workflow - JVC users only
     */
    @PostMapping("/upload")
    // @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')") // Temporarily disabled for debugging
    public ResponseEntity<List<DocumentSummary>> uploadDocuments(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) Long workflowId,
            Authentication authentication) {
        
        try {
            System.out.println("=== DOCUMENT UPLOAD ENDPOINT REACHED ===");
            System.out.println("=== Document Upload Debug ===");
            System.out.println("Files count: " + (files != null ? files.length : 0));
            System.out.println("Project Code: " + projectCode);
            System.out.println("Material Code: " + materialCode);
            System.out.println("Workflow ID: " + workflowId);
            
            // Validate required parameters
            if (projectCode == null || materialCode == null || workflowId == null) {
                System.err.println("Missing required parameters:");
                System.err.println("  projectCode: " + projectCode);
                System.err.println("  materialCode: " + materialCode);
                System.err.println("  workflowId: " + workflowId);
                return ResponseEntity.badRequest().body(null);
            }
            
            if (files == null || files.length == 0) {
                System.err.println("No files provided for upload");
                return ResponseEntity.badRequest().body(null);
            }
            
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    MultipartFile file = files[i];
                    System.out.println("File " + i + ": " + file.getOriginalFilename() + 
                        " (size: " + file.getSize() + ", type: " + file.getContentType() + ")");
                }
            }
            
            String uploadedBy = getCurrentUsername(authentication);
            System.out.println("Uploaded by: " + uploadedBy);
            
            System.out.println("Calling documentService.uploadDocuments...");
            List<DocumentSummary> uploadedDocuments = documentService.uploadDocuments(
                files, projectCode, materialCode, workflowId, uploadedBy);
            
            System.out.println("Upload successful, returned " + uploadedDocuments.size() + " documents");
            return ResponseEntity.status(HttpStatus.CREATED).body(uploadedDocuments);
            
        } catch (Exception e) {
            System.err.println("=== ERROR IN DOCUMENT UPLOAD ===");
            System.err.println("Error type: " + e.getClass().getSimpleName());
            System.err.println("Error message: " + e.getMessage());
            System.err.println("Stack trace:");
            e.printStackTrace();
            
            // Return a proper error response instead of throwing
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Document upload failed");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Download a document with access control and logging - Plant and JVC users can access
     */
    @GetMapping("/{id}/download")
    // @PreAuthorize("hasRole('PLANT_USER') or hasRole('JVC_USER') or hasRole('ADMIN')") // Temporarily disabled for debugging
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @RequestParam(required = false) Long workflowId,
            Authentication authentication,
            HttpServletRequest request) {
        
        try {
            System.out.println("=== DOCUMENT DOWNLOAD ENDPOINT REACHED ===");
            System.out.println("Document ID: " + id);
            System.out.println("Workflow ID: " + workflowId);
            
            Document document = documentService.getDocumentById(id);
            System.out.println("Found document: " + document.getOriginalFileName());
            System.out.println("File path: " + document.getFilePath());
            
            String userId = getCurrentUsername(authentication);
            String ipAddress = getClientIpAddress(request);
            String userAgent = request.getHeader("User-Agent");
            
            System.out.println("User ID: " + userId);
            System.out.println("IP Address: " + ipAddress);
            
            Resource resource = documentService.downloadDocumentSecure(id, userId, ipAddress, userAgent, workflowId);
            System.out.println("Resource obtained: " + resource.exists() + ", readable: " + resource.isReadable());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            System.err.println("=== ERROR IN DOCUMENT DOWNLOAD ===");
            System.err.println("Error type: " + e.getClass().getSimpleName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Get reusable documents for same project/material combination with enhanced metadata - JVC users only
     */
    @GetMapping("/reusable")
    // @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')") // Temporarily disabled for debugging
    public ResponseEntity<List<DocumentSummary>> getReusableDocuments(
            @RequestParam String projectCode, 
            @RequestParam String materialCode,
            @RequestParam(defaultValue = "false") boolean enhanced) {
        
        try {
            System.out.println("=== REUSABLE DOCUMENTS ENDPOINT REACHED ===");
            System.out.println("Project Code: " + projectCode);
            System.out.println("Material Code: " + materialCode);
            System.out.println("Enhanced: " + enhanced);
            
            List<DocumentSummary> documents;
            if (enhanced) {
                System.out.println("Calling getReusableDocumentsEnhanced...");
                documents = documentService.getReusableDocumentsEnhanced(projectCode, materialCode);
            } else {
                System.out.println("Calling getReusableDocuments...");
                documents = documentService.getReusableDocuments(projectCode, materialCode);
            }
            
            System.out.println("Found " + documents.size() + " reusable documents");
            return ResponseEntity.ok(documents);
            
        } catch (Exception e) {
            System.err.println("=== ERROR IN REUSABLE DOCUMENTS ===");
            System.err.println("Error type: " + e.getClass().getSimpleName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get reusable documents");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Reuse existing documents for a new workflow - JVC users only
     */
    @PostMapping("/reuse")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummary>> reuseDocuments(
            @Valid @RequestBody DocumentReuseRequest request,
            Authentication authentication) {
        
        String reuseBy = getCurrentUsername(authentication);
        
        List<DocumentSummary> reusedDocuments = documentService.reuseDocuments(
            request.getWorkflowId(), request.getDocumentIds(), reuseBy);
        
        return ResponseEntity.ok(reusedDocuments);
    }

    /**
     * Get documents for a workflow
     */
    @GetMapping("/workflow/{workflowId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<DocumentSummary>> getWorkflowDocuments(@PathVariable Long workflowId) {
        List<DocumentSummary> documents = documentService.getWorkflowDocuments(workflowId);
        return ResponseEntity.ok(documents);
    }

    /**
     * Get document details by ID with optional enhanced metadata
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DocumentSummary> getDocumentById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean enhanced) {
        
        DocumentSummary summary;
        if (enhanced) {
            summary = documentService.getEnhancedDocumentSummary(id);
        } else {
            Document document = documentService.getDocumentById(id);
            summary = new DocumentSummary(
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
        return ResponseEntity.ok(summary);
    }

    /**
     * Get document access logs - Admin and JVC users only
     */
    @GetMapping("/{id}/access-logs")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentAccessLogDto>> getDocumentAccessLogs(@PathVariable Long id) {
        List<DocumentAccessLogDto> accessLogs = documentService.getDocumentAccessLogs(id);
        return ResponseEntity.ok(accessLogs);
    }

    /**
     * Delete a document - JVC users and Admin only
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id, Authentication authentication) {
        String deletedBy = getCurrentUsername(authentication);
        documentService.deleteDocument(id, deletedBy);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get document count for a workflow
     */
    @GetMapping("/workflow/{workflowId}/count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Long>> getDocumentCount(@PathVariable Long workflowId) {
        long count = documentService.getDocumentCount(workflowId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate file before upload
     */
    @PostMapping("/validate")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> validateFile(@RequestParam("file") MultipartFile file) {
        boolean isValid = documentService.isValidFile(file);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("valid", isValid);
        response.put("fileName", file.getOriginalFilename());
        response.put("fileSize", file.getSize());
        response.put("fileType", getFileExtension(file.getOriginalFilename()));
        
        if (!isValid) {
            response.put("error", "File validation failed. Check file type (PDF/DOCX/XLSX) and size (max 25MB)");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to verify document storage configuration
     */
    @GetMapping("/test/storage-info")
    public ResponseEntity<Map<String, Object>> getStorageInfo() {
        Map<String, Object> info = documentService.getStorageInfo();
        return ResponseEntity.ok(info);
    }

    /**
     * Simple test endpoint for upload debugging
     */
    @PostMapping("/test/upload-debug")
    public ResponseEntity<Map<String, Object>> testUpload(
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "projectCode", required = false) String projectCode,
            @RequestParam(value = "materialCode", required = false) String materialCode,
            @RequestParam(value = "workflowId", required = false) String workflowId) {
        
        System.out.println("=== UPLOAD DEBUG TEST - Reached controller ===");
        System.out.println("Files: " + (files != null ? files.length : 0));
        System.out.println("Project: " + projectCode);
        System.out.println("Material: " + materialCode);
        System.out.println("Workflow: " + workflowId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Upload test endpoint reached");
        response.put("filesCount", files != null ? files.length : 0);
        response.put("projectCode", projectCode);
        response.put("materialCode", materialCode);
        response.put("workflowId", workflowId);
        
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                response.put("file" + i + "_name", files[i].getOriginalFilename());
                response.put("file" + i + "_size", files[i].getSize());
            }
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Simple GET endpoint to test controller accessibility
     */
    @GetMapping("/test/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        System.out.println("=== PING TEST ENDPOINT REACHED ===");
        Map<String, Object> response = new HashMap<>();
        response.put("message", "DocumentController is accessible");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * Simple POST endpoint to test POST requests
     */
    @PostMapping("/test/simple-post")
    public ResponseEntity<Map<String, Object>> simplePost(@RequestBody(required = false) Map<String, Object> body) {
        System.out.println("=== SIMPLE POST TEST ENDPOINT REACHED ===");
        System.out.println("Request body: " + body);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "POST request successful");
        response.put("receivedBody", body);
        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to list all documents in storage
     */
    @GetMapping("/test/list-all")
    public ResponseEntity<List<Map<String, Object>>> listAllDocuments() {
        List<Document> allDocuments = documentRepository.findAll();
        List<Map<String, Object>> documentInfo = new java.util.ArrayList<>();
        
        for (Document doc : allDocuments) {
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("id", doc.getId());
            info.put("originalFileName", doc.getOriginalFileName());
            info.put("fileName", doc.getFileName());
            info.put("filePath", doc.getFilePath());
            info.put("fileExists", java.nio.file.Files.exists(java.nio.file.Paths.get(doc.getFilePath())));
            info.put("workflowId", doc.getWorkflow().getId());
            info.put("projectCode", doc.getWorkflow().getProjectCode());
            info.put("materialCode", doc.getWorkflow().getMaterialCode());
            info.put("uploadedBy", doc.getUploadedBy());
            info.put("uploadedAt", doc.getUploadedAt());
            documentInfo.add(info);
        }
        
        return ResponseEntity.ok(documentInfo);
    }

    // Utility methods
    private String getCurrentUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUsername();
        }
        return "SYSTEM";
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    // Exception handlers
    @ExceptionHandler(DocumentException.class)
    public ResponseEntity<Map<String, String>> handleDocumentException(DocumentException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Document error");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDocumentNotFound(DocumentNotFoundException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Document not found");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Invalid argument");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Internal server error");
        errorResponse.put("message", "An error occurred while processing the document request");
        return ResponseEntity.internalServerError().body(errorResponse);
    }
}