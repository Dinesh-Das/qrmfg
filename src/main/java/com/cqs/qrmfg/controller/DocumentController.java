package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.DocumentAccessLogDto;
import com.cqs.qrmfg.dto.DocumentReuseRequest;
import com.cqs.qrmfg.dto.DocumentSummary;
import com.cqs.qrmfg.exception.DocumentException;
import com.cqs.qrmfg.exception.DocumentNotFoundException;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.model.WorkflowDocument;
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
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * Upload documents for a workflow - JVC users only
     */
    @PostMapping("/upload")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummary>> uploadDocuments(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam String projectCode,
            @RequestParam String materialCode,
            @RequestParam Long workflowId,
            Authentication authentication) {
        
        String uploadedBy = getCurrentUsername(authentication);
        
        List<DocumentSummary> uploadedDocuments = documentService.uploadDocuments(
            files, projectCode, materialCode, workflowId, uploadedBy);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedDocuments);
    }

    /**
     * Download a document with access control and logging - Plant and JVC users can access
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id,
            @RequestParam(required = false) Long workflowId,
            Authentication authentication,
            HttpServletRequest request) {
        
        WorkflowDocument document = documentService.getDocumentById(id);
        String userId = getCurrentUsername(authentication);
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        
        Resource resource = documentService.downloadDocumentSecure(id, userId, ipAddress, userAgent, workflowId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                .body(resource);
    }

    /**
     * Get reusable documents for same project/material combination with enhanced metadata - JVC users only
     */
    @GetMapping("/reusable")
    @PreAuthorize("hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<DocumentSummary>> getReusableDocuments(
            @RequestParam String projectCode, 
            @RequestParam String materialCode,
            @RequestParam(defaultValue = "false") boolean enhanced) {
        
        List<DocumentSummary> documents;
        if (enhanced) {
            documents = documentService.getReusableDocumentsEnhanced(projectCode, materialCode);
        } else {
            documents = documentService.getReusableDocuments(projectCode, materialCode);
        }
        return ResponseEntity.ok(documents);
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
            WorkflowDocument document = documentService.getDocumentById(id);
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