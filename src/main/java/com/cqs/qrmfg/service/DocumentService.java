package com.cqs.qrmfg.service;

import com.cqs.qrmfg.dto.DocumentAccessLogDto;
import com.cqs.qrmfg.dto.DocumentSummary;
import com.cqs.qrmfg.model.DocumentAccessType;
import com.cqs.qrmfg.model.WorkflowDocument;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service interface for document management operations
 */
public interface DocumentService {

    /**
     * Upload documents for a workflow
     */
    List<DocumentSummary> uploadDocuments(MultipartFile[] files, String projectCode, String materialCode, Long workflowId, String uploadedBy);

    /**
     * Get documents for a workflow
     */
    List<DocumentSummary> getWorkflowDocuments(Long workflowId);

    /**
     * Get reusable documents for project and material combination
     */
    List<DocumentSummary> getReusableDocuments(String projectCode, String materialCode);

    /**
     * Reuse existing documents for a new workflow
     */
    List<DocumentSummary> reuseDocuments(Long workflowId, List<Long> documentIds, String reuseBy);

    /**
     * Download a document
     */
    Resource downloadDocument(Long documentId);

    /**
     * Get document by ID
     */
    WorkflowDocument getDocumentById(Long documentId);

    /**
     * Delete a document
     */
    void deleteDocument(Long documentId, String deletedBy);

    /**
     * Validate file type and size
     */
    boolean isValidFile(MultipartFile file);

    /**
     * Get document count for workflow
     */
    long getDocumentCount(Long workflowId);

    /**
     * Download document with access control and logging
     */
    Resource downloadDocumentSecure(Long documentId, String userId, String ipAddress, String userAgent, Long workflowId);

    /**
     * Check if user has access to document
     */
    boolean hasDocumentAccess(Long documentId, String userId, Long workflowId);

    /**
     * Log document access attempt
     */
    void logDocumentAccess(Long documentId, String userId, DocumentAccessType accessType, 
                          String ipAddress, String userAgent, Long workflowId, 
                          boolean accessGranted, String denialReason);

    /**
     * Get document access logs
     */
    List<DocumentAccessLogDto> getDocumentAccessLogs(Long documentId);

    /**
     * Get enhanced document metadata with access statistics
     */
    DocumentSummary getEnhancedDocumentSummary(Long documentId);

    /**
     * Get reusable documents with enhanced metadata
     */
    List<DocumentSummary> getReusableDocumentsEnhanced(String projectCode, String materialCode);
}