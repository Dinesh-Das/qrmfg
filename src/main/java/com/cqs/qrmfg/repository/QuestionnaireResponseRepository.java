package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.QuestionnaireResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionnaireResponseRepository extends JpaRepository<QuestionnaireResponse, Long> {
    
    // Basic finders
    List<QuestionnaireResponse> findByWorkflowId(Long workflowId);
    List<QuestionnaireResponse> findByStepNumber(Integer stepNumber);
    List<QuestionnaireResponse> findByFieldName(String fieldName);
    List<QuestionnaireResponse> findByModifiedBy(String modifiedBy);
    
    // Combined filters
    List<QuestionnaireResponse> findByWorkflowIdAndStepNumber(Long workflowId, Integer stepNumber);
    List<QuestionnaireResponse> findByWorkflowIdAndFieldName(Long workflowId, String fieldName);
    List<QuestionnaireResponse> findByStepNumberAndFieldName(Integer stepNumber, String fieldName);
    Optional<QuestionnaireResponse> findByWorkflowIdAndStepNumberAndFieldName(Long workflowId, Integer stepNumber, String fieldName);
    
    // Material-based queries
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.materialCode = :materialCode")
    List<QuestionnaireResponse> findByMaterialCode(@Param("materialCode") String materialCode);
    
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.materialCode = :materialCode AND qr.stepNumber = :stepNumber")
    List<QuestionnaireResponse> findByMaterialCodeAndStepNumber(@Param("materialCode") String materialCode, @Param("stepNumber") Integer stepNumber);
    
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.materialCode = :materialCode AND qr.fieldName = :fieldName")
    List<QuestionnaireResponse> findByMaterialCodeAndFieldName(@Param("materialCode") String materialCode, @Param("fieldName") String fieldName);
    
    // Draft and validation queries
    List<QuestionnaireResponse> findByIsDraft(Boolean isDraft);
    List<QuestionnaireResponse> findByWorkflowIdAndIsDraft(Long workflowId, Boolean isDraft);
    List<QuestionnaireResponse> findByValidationStatus(String validationStatus);
    List<QuestionnaireResponse> findByWorkflowIdAndValidationStatus(Long workflowId, String validationStatus);
    
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.isDraft = true")
    List<QuestionnaireResponse> findDraftResponsesByWorkflow(@Param("workflowId") Long workflowId);
    
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.validationStatus = 'INVALID'")
    List<QuestionnaireResponse> findInvalidResponsesByWorkflow(@Param("workflowId") Long workflowId);
    
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.isRequired = true AND (qr.fieldValue IS NULL OR qr.fieldValue = '')")
    List<QuestionnaireResponse> findMissingRequiredResponsesByWorkflow(@Param("workflowId") Long workflowId);
    
    // Section-based queries
    List<QuestionnaireResponse> findBySectionName(String sectionName);
    List<QuestionnaireResponse> findByWorkflowIdAndSectionName(Long workflowId, String sectionName);
    List<QuestionnaireResponse> findByWorkflowIdAndSectionNameOrderByDisplayOrder(Long workflowId, String sectionName);
    
    // Time-based queries
    List<QuestionnaireResponse> findByLastModifiedAfter(LocalDateTime dateTime);
    List<QuestionnaireResponse> findByLastModifiedBetween(LocalDateTime start, LocalDateTime end);
    List<QuestionnaireResponse> findByCreatedAtAfter(LocalDateTime dateTime);
    List<QuestionnaireResponse> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // User activity queries
    List<QuestionnaireResponse> findByModifiedByAndLastModifiedAfter(String modifiedBy, LocalDateTime dateTime);
    List<QuestionnaireResponse> findByCreatedByAndCreatedAtAfter(String createdBy, LocalDateTime dateTime);
    
    // Workflow completion queries
    @Query("SELECT COUNT(qr) FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId")
    long countResponsesByWorkflow(@Param("workflowId") Long workflowId);
    
    @Query("SELECT COUNT(qr) FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.isRequired = true")
    long countRequiredResponsesByWorkflow(@Param("workflowId") Long workflowId);
    
    @Query("SELECT COUNT(qr) FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.isRequired = true AND (qr.fieldValue IS NOT NULL AND qr.fieldValue != '')")
    long countCompletedRequiredResponsesByWorkflow(@Param("workflowId") Long workflowId);
    
    @Query("SELECT COUNT(qr) FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.isDraft = true")
    long countDraftResponsesByWorkflow(@Param("workflowId") Long workflowId);
    
    @Query("SELECT COUNT(qr) FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.validationStatus = 'INVALID'")
    long countInvalidResponsesByWorkflow(@Param("workflowId") Long workflowId);
    
    // Progress tracking queries
    @Query("SELECT (COUNT(qr) * 100.0 / (SELECT COUNT(qr2) FROM QuestionnaireResponse qr2 WHERE qr2.workflow.id = :workflowId)) " +
           "FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.fieldValue IS NOT NULL AND qr.fieldValue != ''")
    Double getCompletionPercentageByWorkflow(@Param("workflowId") Long workflowId);
    
    @Query("SELECT qr.stepNumber, COUNT(qr) as responseCount " +
           "FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId " +
           "GROUP BY qr.stepNumber ORDER BY qr.stepNumber")
    List<Object[]> getResponseCountByStep(@Param("workflowId") Long workflowId);
    
    @Query("SELECT qr.sectionName, COUNT(qr) as responseCount " +
           "FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId " +
           "GROUP BY qr.sectionName ORDER BY qr.sectionName")
    List<Object[]> getResponseCountBySection(@Param("workflowId") Long workflowId);
    
    // Field type queries
    List<QuestionnaireResponse> findByFieldType(String fieldType);
    List<QuestionnaireResponse> findByWorkflowIdAndFieldType(Long workflowId, String fieldType);
    
    // Recent activity queries
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.lastModified >= :cutoffDate ORDER BY qr.lastModified DESC")
    List<QuestionnaireResponse> findRecentlyModified(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.createdAt >= :cutoffDate ORDER BY qr.createdAt DESC")
    List<QuestionnaireResponse> findRecentlyCreated(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId ORDER BY qr.lastModified DESC")
    List<QuestionnaireResponse> findByWorkflowOrderByLastModified(@Param("workflowId") Long workflowId);
    
    // Bulk operations support
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.id IN :workflowIds")
    List<QuestionnaireResponse> findByWorkflowIds(@Param("workflowIds") List<Long> workflowIds);
    
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.materialCode IN :materialCodes")
    List<QuestionnaireResponse> findByMaterialCodes(@Param("materialCodes") List<String> materialCodes);
    
    // Validation and quality checks
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND " +
           "(qr.fieldValue IS NULL OR qr.fieldValue = '' OR qr.validationStatus = 'INVALID')")
    List<QuestionnaireResponse> findIncompleteOrInvalidResponsesByWorkflow(@Param("workflowId") Long workflowId);
    
    @Query("SELECT CASE WHEN COUNT(qr) > 0 THEN true ELSE false END " +
           "FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND " +
           "qr.isRequired = true AND (qr.fieldValue IS NULL OR qr.fieldValue = '')")
    boolean hasWorkflowMissingRequiredResponses(@Param("workflowId") Long workflowId);
    
    @Query("SELECT CASE WHEN COUNT(qr) > 0 THEN true ELSE false END " +
           "FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.validationStatus = 'INVALID'")
    boolean hasWorkflowInvalidResponses(@Param("workflowId") Long workflowId);
    
    // Cleanup and maintenance queries
    @Query("DELETE FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.isDraft = true")
    void deleteDraftResponsesByWorkflow(@Param("workflowId") Long workflowId);
    
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.isDraft = true AND qr.lastModified < :cutoffDate")
    List<QuestionnaireResponse> findOldDraftResponses(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Enhanced form data persistence capabilities
    @Query("SELECT qr FROM QuestionnaireResponse qr JOIN qr.workflow w WHERE " +
           "(:projectCode IS NULL OR w.projectCode = :projectCode) AND " +
           "(:materialCode IS NULL OR w.materialCode = :materialCode) AND " +
           "(:plantCode IS NULL OR w.plantCode = :plantCode) AND " +
           "(:stepNumber IS NULL OR qr.stepNumber = :stepNumber) AND " +
           "(:fieldType IS NULL OR qr.fieldType = :fieldType) AND " +
           "(:validationStatus IS NULL OR qr.validationStatus = :validationStatus) " +
           "ORDER BY qr.lastModified DESC")
    List<QuestionnaireResponse> findResponsesWithFilters(
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode,
        @Param("plantCode") String plantCode,
        @Param("stepNumber") Integer stepNumber,
        @Param("fieldType") String fieldType,
        @Param("validationStatus") String validationStatus);

    // Auto-save and recovery support
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.isDraft = true AND qr.lastModified >= :cutoffTime ORDER BY qr.lastModified DESC")
    List<QuestionnaireResponse> findRecentDraftsByWorkflow(@Param("workflowId") Long workflowId, @Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.stepNumber = :stepNumber AND qr.isDraft = true ORDER BY qr.lastModified DESC")
    List<QuestionnaireResponse> findDraftResponsesByWorkflowAndStep(@Param("workflowId") Long workflowId, @Param("stepNumber") Integer stepNumber);

    // Form completion analytics
    @Query("SELECT qr.stepNumber, COUNT(qr) as totalResponses, " +
           "SUM(CASE WHEN qr.fieldValue IS NOT NULL AND qr.fieldValue != '' THEN 1 ELSE 0 END) as completedResponses " +
           "FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId " +
           "GROUP BY qr.stepNumber ORDER BY qr.stepNumber")
    List<Object[]> getStepCompletionStats(@Param("workflowId") Long workflowId);

    @Query("SELECT qr.fieldType, COUNT(qr) as totalFields, " +
           "SUM(CASE WHEN qr.fieldValue IS NOT NULL AND qr.fieldValue != '' THEN 1 ELSE 0 END) as completedFields " +
           "FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId " +
           "GROUP BY qr.fieldType ORDER BY qr.fieldType")
    List<Object[]> getFieldTypeCompletionStats(@Param("workflowId") Long workflowId);

    // Validation and quality metrics
    @Query("SELECT qr.validationStatus, COUNT(qr) FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId GROUP BY qr.validationStatus")
    List<Object[]> getValidationStatusStats(@Param("workflowId") Long workflowId);

    @Query("SELECT qr.stepNumber, qr.validationStatus, COUNT(qr) FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId GROUP BY qr.stepNumber, qr.validationStatus ORDER BY qr.stepNumber")
    List<Object[]> getValidationStatusByStep(@Param("workflowId") Long workflowId);

    // User activity tracking
    @Query("SELECT qr.modifiedBy, COUNT(qr) as responseCount, MAX(qr.lastModified) as lastActivity " +
           "FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId " +
           "GROUP BY qr.modifiedBy ORDER BY lastActivity DESC")
    List<Object[]> getUserActivityStats(@Param("workflowId") Long workflowId);

    // Bulk operations support
    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.id IN :responseIds")
    List<QuestionnaireResponse> findByIds(@Param("responseIds") List<Long> responseIds);

    @Query("SELECT qr FROM QuestionnaireResponse qr WHERE qr.workflow.id IN :workflowIds AND qr.isDraft = false")
    List<QuestionnaireResponse> findFinalizedResponsesByWorkflowIds(@Param("workflowIds") List<Long> workflowIds);

    // Template and reuse support
    @Query("SELECT qr FROM QuestionnaireResponse qr JOIN qr.workflow w WHERE " +
           "w.projectCode = :projectCode AND w.materialCode = :materialCode AND " +
           "qr.isDraft = false AND qr.validationStatus = 'VALID' " +
           "ORDER BY qr.lastModified DESC")
    List<QuestionnaireResponse> findTemplateResponsesForProjectMaterial(
        @Param("projectCode") String projectCode, 
        @Param("materialCode") String materialCode);

    // Performance optimization queries
    @Query("SELECT qr.stepNumber, qr.fieldName, qr.fieldValue FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.isDraft = false ORDER BY qr.stepNumber, qr.displayOrder")
    List<Object[]> findFinalizedResponseDataByWorkflow(@Param("workflowId") Long workflowId);

    @Query("SELECT COUNT(qr) FROM QuestionnaireResponse qr WHERE qr.workflow.id = :workflowId AND qr.isDraft = false AND qr.isRequired = true AND (qr.fieldValue IS NULL OR qr.fieldValue = '')")
    Long countMissingRequiredFinalizedResponses(@Param("workflowId") Long workflowId);
}