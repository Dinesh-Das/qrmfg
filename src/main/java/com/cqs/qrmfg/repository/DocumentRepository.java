package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.Document;
import com.cqs.qrmfg.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findByWorkflowOrderByUploadedAtDesc(Workflow workflow);
    
    List<Document> findByWorkflowAndFileType(Workflow workflow, String fileType);
    
    /**
     * Find reusable documents for same project and material combination
     */
    @Query("SELECT d FROM Document d JOIN FETCH d.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode")
    List<Document> findReusableDocuments(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Find documents by workflow project and material codes
     */
    @Query("SELECT d FROM Document d JOIN FETCH d.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode AND d.workflow.id != :excludeWorkflowId")
    List<Document> findReusableDocumentsExcludingWorkflow(
        @Param("projectCode") String projectCode, 
        @Param("materialCode") String materialCode,
        @Param("excludeWorkflowId") Long excludeWorkflowId);

    /**
     * Find documents uploaded by specific user
     */
    List<Document> findByUploadedByOrderByUploadedAtDesc(String uploadedBy);

    /**
     * Find documents uploaded after specific date
     */
    List<Document> findByUploadedAtAfterOrderByUploadedAtDesc(LocalDateTime uploadedAfter);

    /**
     * Find documents by file type
     */
    List<Document> findByFileTypeOrderByUploadedAtDesc(String fileType);

    /**
     * Find reused documents
     */
    List<Document> findByIsReusedTrueOrderByUploadedAtDesc();

    /**
     * Find original documents (not reused)
     */
    List<Document> findByIsReusedFalseOrderByUploadedAtDesc();

    /**
     * Find documents by original document reference
     */
    List<Document> findByOriginalDocumentOrderByUploadedAtDesc(Document originalDocument);

    /**
     * Count documents by workflow
     */
    long countByWorkflow(Workflow workflow);

    /**
     * Count documents by file type
     */
    long countByFileType(String fileType);

    /**
     * Count reused documents
     */
    long countByIsReusedTrue();

    /**
     * Find documents by file name pattern
     */
    @Query("SELECT d FROM Document d WHERE d.fileName LIKE %:pattern% OR d.originalFileName LIKE %:pattern% ORDER BY d.uploadedAt DESC")
    List<Document> findByFileNameContaining(@Param("pattern") String pattern);

    /**
     * Find documents by size range
     */
    @Query("SELECT d FROM Document d WHERE d.fileSize BETWEEN :minSize AND :maxSize ORDER BY d.uploadedAt DESC")
    List<Document> findByFileSizeBetween(@Param("minSize") Long minSize, @Param("maxSize") Long maxSize);

    /**
     * Find large documents (over specified size)
     */
    @Query("SELECT d FROM Document d WHERE d.fileSize > :sizeThreshold ORDER BY d.fileSize DESC")
    List<Document> findLargeDocuments(@Param("sizeThreshold") Long sizeThreshold);

    /**
     * Get total storage used by workflow
     */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.workflow = :workflow")
    Long getTotalStorageByWorkflow(@Param("workflow") Workflow workflow);

    /**
     * Get total storage used by project and material
     */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d JOIN d.workflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode")
    Long getTotalStorageByProjectAndMaterial(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);

    /**
     * Find documents for bulk operations
     */
    @Query("SELECT d FROM Document d WHERE d.workflow.id IN :workflowIds ORDER BY d.workflow.id, d.uploadedAt DESC")
    List<Document> findByWorkflowIds(@Param("workflowIds") List<Long> workflowIds);

    /**
     * Document usage trends
     */
    @Query(value = "SELECT DATE(uploaded_at), file_type, COUNT(*) FROM QRMFG_DOCUMENTS WHERE uploaded_at >= :startDate GROUP BY DATE(uploaded_at), file_type ORDER BY DATE(uploaded_at), file_type", nativeQuery = true)
    List<Object[]> getDocumentUploadTrend(@Param("startDate") LocalDateTime startDate);

    /**
     * Check if document exists by file path
     */
    boolean existsByFilePath(String filePath);

    /**
     * Find document by file path
     */
    Optional<Document> findByFilePath(String filePath);

    /**
     * Delete documents by workflow
     */
    void deleteByWorkflow(Workflow workflow);

    /**
     * Find documents that can be reused for a specific workflow
     */
    @Query("SELECT d FROM Document d JOIN d.workflow w WHERE " +
           "w.projectCode = :projectCode AND w.materialCode = :materialCode AND " +
           "d.workflow.id != :excludeWorkflowId AND d.isReused = false " +
           "ORDER BY d.uploadedAt DESC")
    List<Document> findReusableCandidates(
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode, 
        @Param("excludeWorkflowId") Long excludeWorkflowId);
    
    // Additional methods needed by DocumentServiceImpl
    List<Document> findByWorkflowId(Long workflowId);
    long countByWorkflowId(Long workflowId);
}