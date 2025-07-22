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
    @Query("SELECT wd FROM WorkflowDocument wd JOIN wd.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode")
    List<WorkflowDocument> findReusableDocuments(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Find documents by workflow project and material codes
     */
    @Query("SELECT wd FROM WorkflowDocument wd JOIN wd.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode AND wd.workflow.id != :excludeWorkflowId")
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
}