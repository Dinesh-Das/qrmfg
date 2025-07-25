package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.WorkflowDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowDocumentRepository extends JpaRepository<WorkflowDocument, Long> {

    /**
     * Find documents by workflow ID
     */
    List<WorkflowDocument> findByWorkflowId(Long workflowId);

    /**
     * Find reusable documents for same project and material combination
     */
    @Query("SELECT wd FROM WorkflowDocument wd JOIN FETCH wd.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode")
    List<WorkflowDocument> findReusableDocuments(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Find documents by workflow project and material codes
     */
    @Query("SELECT wd FROM WorkflowDocument wd JOIN FETCH wd.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode AND wd.workflow.id != :excludeWorkflowId")
    List<WorkflowDocument> findReusableDocumentsExcludingWorkflow(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode, @Param("excludeWorkflowId") Long excludeWorkflowId);

    /**
     * Count documents by workflow ID
     */
    long countByWorkflowId(Long workflowId);

    /**
     * Find documents by file name pattern
     */
    List<WorkflowDocument> findByFileNameContainingIgnoreCase(String fileName);

    /**
     * Find documents by uploaded by user
     */
    List<WorkflowDocument> findByUploadedBy(String uploadedBy);

    /**
     * Find documents by file type
     */
    List<WorkflowDocument> findByFileType(String fileType);

    /**
     * Find documents by workflow and file type
     */
    List<WorkflowDocument> findByWorkflowIdAndFileType(Long workflowId, String fileType);

    /**
     * Find reused documents
     */
    List<WorkflowDocument> findByIsReused(Boolean isReused);

    /**
     * Find original documents that can be reused
     */
    @Query("SELECT wd FROM WorkflowDocument wd WHERE wd.isReused = false")
    List<WorkflowDocument> findOriginalDocuments();

    /**
     * Find documents by original document ID (for tracking reuse chain)
     */
    @Query("SELECT wd FROM WorkflowDocument wd WHERE wd.originalDocument.id = :originalDocumentId")
    List<WorkflowDocument> findByOriginalDocumentId(@Param("originalDocumentId") Long originalDocumentId);

    /**
     * Find documents by file size range
     */
    @Query("SELECT wd FROM WorkflowDocument wd WHERE wd.fileSize BETWEEN :minSize AND :maxSize")
    List<WorkflowDocument> findByFileSizeRange(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    /**
     * Find documents uploaded within date range
     */
    @Query("SELECT wd FROM WorkflowDocument wd WHERE wd.uploadedAt BETWEEN :startDate AND :endDate")
    List<WorkflowDocument> findByUploadedAtBetween(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Find documents by project code (through workflow)
     */
    @Query("SELECT wd FROM WorkflowDocument wd JOIN wd.workflow w WHERE w.projectCode = :projectCode")
    List<WorkflowDocument> findByProjectCode(@Param("projectCode") String projectCode);

    /**
     * Find documents by material code (through workflow)
     */
    @Query("SELECT wd FROM WorkflowDocument wd JOIN wd.workflow w WHERE w.materialCode = :materialCode")
    List<WorkflowDocument> findByMaterialCode(@Param("materialCode") String materialCode);

    /**
     * Find documents by plant code (through workflow)
     */
    @Query("SELECT wd FROM WorkflowDocument wd JOIN wd.workflow w WHERE w.plantCode = :plantCode")
    List<WorkflowDocument> findByPlantCode(@Param("plantCode") String plantCode);

    /**
     * Count reusable documents for project/material combination
     */
    @Query("SELECT COUNT(wd) FROM WorkflowDocument wd JOIN wd.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode")
    Long countReusableDocuments(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Find most recent documents for project/material combination
     */
    @Query("SELECT wd FROM WorkflowDocument wd JOIN wd.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode ORDER BY wd.uploadedAt DESC")
    List<WorkflowDocument> findRecentDocumentsByProjectAndMaterial(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Find documents with filtering capabilities
     */
    @Query("SELECT wd FROM WorkflowDocument wd JOIN wd.workflow w WHERE " +
           "(:projectCode IS NULL OR w.projectCode = :projectCode) AND " +
           "(:materialCode IS NULL OR w.materialCode = :materialCode) AND " +
           "(:plantCode IS NULL OR w.plantCode = :plantCode) AND " +
           "(:fileType IS NULL OR wd.fileType = :fileType) AND " +
           "(:uploadedBy IS NULL OR wd.uploadedBy = :uploadedBy) " +
           "ORDER BY wd.uploadedAt DESC")
    List<WorkflowDocument> findDocumentsWithFilters(
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode,
        @Param("plantCode") String plantCode,
        @Param("fileType") String fileType,
        @Param("uploadedBy") String uploadedBy);

    /**
     * Get document statistics by file type
     */
    @Query("SELECT wd.fileType, COUNT(wd), SUM(wd.fileSize) FROM WorkflowDocument wd GROUP BY wd.fileType")
    List<Object[]> getDocumentStatsByFileType();

    /**
     * Get document statistics by project
     */
    @Query("SELECT w.projectCode, COUNT(wd), SUM(wd.fileSize) FROM WorkflowDocument wd JOIN wd.workflow w GROUP BY w.projectCode")
    List<Object[]> getDocumentStatsByProject();

    /**
     * Enhanced document management and reuse queries for dashboard
     */
    @Query("SELECT wd FROM WorkflowDocument wd JOIN wd.workflow w WHERE " +
           "(:projectCodes IS NULL OR w.projectCode IN :projectCodes) AND " +
           "(:materialCodes IS NULL OR w.materialCode IN :materialCodes) AND " +
           "(:plantCodes IS NULL OR w.plantCode IN :plantCodes) AND " +
           "(:fileTypes IS NULL OR wd.fileType IN :fileTypes) AND " +
           "(:uploadedBy IS NULL OR wd.uploadedBy = :uploadedBy) AND " +
           "(:isReused IS NULL OR wd.isReused = :isReused) AND " +
           "(:uploadedAfter IS NULL OR wd.uploadedAt >= :uploadedAfter) " +
           "ORDER BY wd.uploadedAt DESC")
    List<WorkflowDocument> findDocumentsWithBulkFilters(
        @Param("projectCodes") List<String> projectCodes,
        @Param("materialCodes") List<String> materialCodes,
        @Param("plantCodes") List<String> plantCodes,
        @Param("fileTypes") List<String> fileTypes,
        @Param("uploadedBy") String uploadedBy,
        @Param("isReused") Boolean isReused,
        @Param("uploadedAfter") java.time.LocalDateTime uploadedAfter);

    /**
     * Document reuse analytics for dashboard
     */
    @Query("SELECT w.projectCode, w.materialCode, COUNT(wd) as totalDocuments, COUNT(CASE WHEN wd.isReused = true THEN 1 END) as reusedDocuments FROM WorkflowDocument wd JOIN wd.workflow w GROUP BY w.projectCode, w.materialCode ORDER BY COUNT(wd) DESC")
    List<Object[]> getDocumentReuseAnalytics();

    /**
     * Document usage trends
     */
    @Query(value = "SELECT DATE(uploaded_at), file_type, COUNT(*) FROM qrmfg_workflow_documents WHERE uploaded_at >= :startDate GROUP BY DATE(uploaded_at), file_type ORDER BY DATE(uploaded_at), file_type", nativeQuery = true)
    List<Object[]> getDocumentUploadTrend(@Param("startDate") java.time.LocalDateTime startDate);

    /**
     * Storage usage analysis
     */
    @Query("SELECT wd.fileType, COUNT(wd) as fileCount, SUM(wd.fileSize) as totalSize, AVG(wd.fileSize) as avgSize FROM WorkflowDocument wd GROUP BY wd.fileType ORDER BY SUM(wd.fileSize) DESC")
    List<Object[]> getStorageUsageByFileType();

    @Query("SELECT w.projectCode, COUNT(wd) as fileCount, SUM(wd.fileSize) as totalSize FROM WorkflowDocument wd JOIN wd.workflow w GROUP BY w.projectCode ORDER BY SUM(wd.fileSize) DESC")
    List<Object[]> getStorageUsageByProject();

    /**
     * User document activity
     */
    @Query("SELECT wd.uploadedBy, COUNT(wd) as uploadCount, SUM(wd.fileSize) as totalSize FROM WorkflowDocument wd WHERE wd.uploadedAt >= :startDate GROUP BY wd.uploadedBy ORDER BY COUNT(wd) DESC")
    List<Object[]> getUserDocumentActivity(@Param("startDate") java.time.LocalDateTime startDate);

    /**
     * Document reuse efficiency
     */
    @Query("SELECT COUNT(CASE WHEN wd.isReused = false THEN 1 END) as originalDocuments, COUNT(CASE WHEN wd.isReused = true THEN 1 END) as reusedDocuments, (COUNT(CASE WHEN wd.isReused = true THEN 1 END) * 100.0 / COUNT(*)) as reusePercentage FROM WorkflowDocument wd")
    List<Object[]> getDocumentReuseEfficiency();

    /**
     * Find documents that can be reused for new workflows
     */
    @Query("SELECT wd FROM WorkflowDocument wd JOIN wd.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode AND wd.isReused = false AND w.state = 'COMPLETED' ORDER BY wd.uploadedAt DESC")
    List<WorkflowDocument> findAvailableDocumentsForReuse(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Document access patterns
     */
    @Query("SELECT w.plantCode, wd.fileType, COUNT(wd) as accessCount FROM WorkflowDocument wd JOIN wd.workflow w GROUP BY w.plantCode, wd.fileType ORDER BY w.plantCode, COUNT(wd) DESC")
    List<Object[]> getDocumentAccessPatterns();

    /**
     * Large file analysis
     */
    @Query("SELECT wd FROM WorkflowDocument wd WHERE wd.fileSize > :sizeThreshold ORDER BY wd.fileSize DESC")
    List<WorkflowDocument> findLargeDocuments(@Param("sizeThreshold") Long sizeThreshold);

    /**
     * Document validation queries
     */
    @Query("SELECT wd FROM WorkflowDocument wd WHERE wd.fileType NOT IN ('pdf', 'docx', 'xlsx')")
    List<WorkflowDocument> findInvalidFileTypes();

    @Query("SELECT wd FROM WorkflowDocument wd WHERE wd.fileSize > 26214400") // 25MB in bytes
    List<WorkflowDocument> findOversizedDocuments();

    /**
     * Orphaned document cleanup
     */
    @Query("SELECT wd FROM WorkflowDocument wd WHERE wd.workflow IS NULL")
    List<WorkflowDocument> findOrphanedDocuments();

    /**
     * Document chain analysis for reuse tracking
     */
    @Query("SELECT wd FROM WorkflowDocument wd WHERE wd.originalDocument.id = :originalDocumentId ORDER BY wd.uploadedAt")
    List<WorkflowDocument> findDocumentReuseChain(@Param("originalDocumentId") Long originalDocumentId);

    /**
     * Recent document activity for dashboard
     */
    @Query("SELECT wd FROM WorkflowDocument wd WHERE wd.uploadedAt >= :cutoffTime ORDER BY wd.uploadedAt DESC")
    List<WorkflowDocument> findRecentDocuments(@Param("cutoffTime") java.time.LocalDateTime cutoffTime);

    /**
     * Document count by workflow status
     */
    @Query("SELECT w.state, COUNT(wd) as documentCount FROM WorkflowDocument wd JOIN wd.workflow w GROUP BY w.state ORDER BY w.state")
    List<Object[]> getDocumentCountByWorkflowState();}
