package com.cqs.qrmfg.repository;

import com.cqs.qrmfg.model.MaterialWorkflow;
import com.cqs.qrmfg.model.WorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<MaterialWorkflow, Long> {
    
    // Basic finders
    Optional<MaterialWorkflow> findByProjectCodeAndMaterialCodeAndPlantCodeAndBlockId(
        String projectCode, String materialCode, String plantCode, String blockId);
    
    // Eager loading version for duplicate check
    @Query("SELECT DISTINCT w FROM MaterialWorkflow w LEFT JOIN FETCH w.queries WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode AND w.plantCode = :plantCode AND w.blockId = :blockId")
    Optional<MaterialWorkflow> findByProjectCodeAndMaterialCodeAndPlantCodeAndBlockIdWithQueries(
        @Param("projectCode") String projectCode, 
        @Param("materialCode") String materialCode, 
        @Param("plantCode") String plantCode, 
        @Param("blockId") String blockId);
    List<MaterialWorkflow> findByState(WorkflowState state);
    List<MaterialWorkflow> findByPlantCode(String plantCode);
    List<MaterialWorkflow> findByInitiatedBy(String initiatedBy);
    List<MaterialWorkflow> findByProjectCode(String projectCode);
    List<MaterialWorkflow> findByMaterialCode(String materialCode);
    
    // State-based queries
    List<MaterialWorkflow> findByStateIn(List<WorkflowState> states);
    List<MaterialWorkflow> findByStateNot(WorkflowState state);
    
    // Time-based queries
    List<MaterialWorkflow> findByCreatedAtAfter(LocalDateTime dateTime);
    List<MaterialWorkflow> findByCompletedAtAfter(LocalDateTime dateTime);
    List<MaterialWorkflow> findByCompletedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Dashboard data queries
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.state != 'COMPLETED' ORDER BY w.createdAt DESC")
    List<MaterialWorkflow> findPendingWorkflows();
    
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.state != 'COMPLETED' AND w.plantCode = :plantCode ORDER BY w.createdAt DESC")
    List<MaterialWorkflow> findPendingWorkflowsByPlant(@Param("plantCode") String plantCode);
    
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.state != 'COMPLETED' AND w.initiatedBy = :username ORDER BY w.createdAt DESC")
    List<MaterialWorkflow> findPendingWorkflowsByUser(@Param("username") String username);
    
    @Query(value = "SELECT * FROM qrmfg_material_workflows WHERE workflow_state != 'COMPLETED' AND ((workflow_state = 'JVC_PENDING' AND created_at < CURRENT_TIMESTAMP - 3) OR (workflow_state = 'PLANT_PENDING' AND COALESCE(extended_at, created_at) < CURRENT_TIMESTAMP - 3) OR (workflow_state IN ('CQS_PENDING', 'TECH_PENDING') AND last_modified < CURRENT_TIMESTAMP - 3))", nativeQuery = true)
    List<MaterialWorkflow> findOverdueWorkflows();
    
    @Query("SELECT DISTINCT w FROM MaterialWorkflow w JOIN w.queries q WHERE q.status = 'OPEN'")
    List<MaterialWorkflow> findWorkflowsWithOpenQueries();
    
    // Project/Material filtering for dashboard
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.projectCode = :projectCode AND w.materialCode = :materialCode ORDER BY w.createdAt DESC")
    List<MaterialWorkflow> findByProjectAndMaterial(@Param("projectCode") String projectCode, @Param("materialCode") String materialCode);
    
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.projectCode = :projectCode ORDER BY w.createdAt DESC")
    List<MaterialWorkflow> findByProjectCodeOrderByCreatedAt(@Param("projectCode") String projectCode);
    
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.materialCode = :materialCode ORDER BY w.createdAt DESC")
    List<MaterialWorkflow> findByMaterialCodeOrderByCreatedAt(@Param("materialCode") String materialCode);
    
    // Count queries
    long countByState(WorkflowState state);
    
    @Query(value = "SELECT COUNT(*) FROM qrmfg_material_workflow WHERE state != 'COMPLETED' AND ((state = 'JVC_PENDING' AND (SYSDATE - created_at) > 3) OR (state = 'PLANT_PENDING' AND (SYSDATE - NVL(extended_at, created_at)) > 3) OR (state IN ('CQS_PENDING', 'TECH_PENDING') AND (SYSDATE - last_modified) > 3))", nativeQuery = true)
    long countOverdueWorkflows();
    
    @Query(value = "SELECT COUNT(DISTINCT w.id) FROM qrmfg_material_workflow w JOIN qrmfg_query q ON w.id = q.workflow_id WHERE q.status = 'OPEN'", nativeQuery = true)
    long countWorkflowsWithOpenQueries();
    
    // Plant-specific queries
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.plantCode = :plantCode AND w.state = :state")
    List<MaterialWorkflow> findByPlantAndState(@Param("plantCode") String plantCode, @Param("state") WorkflowState state);
    
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.plantCode = :plantCode AND w.state != 'COMPLETED'")
    List<MaterialWorkflow> findPendingByPlant(@Param("plantCode") String plantCode);
    
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.plantCode = :plantCode AND w.blockId = :blockId")
    List<MaterialWorkflow> findByPlantAndBlock(@Param("plantCode") String plantCode, @Param("blockId") String blockId);
    
    // User-specific queries
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.initiatedBy = :username AND w.state != 'COMPLETED'")
    List<MaterialWorkflow> findPendingByInitiatedBy(@Param("username") String username);
    
    // Fetch workflows with queries eagerly loaded to avoid LazyInitializationException
    // Note: Cannot fetch multiple collections simultaneously due to Hibernate MultipleBagFetchException
    @Query("SELECT DISTINCT w FROM MaterialWorkflow w LEFT JOIN FETCH w.queries WHERE w.initiatedBy = :username")
    List<MaterialWorkflow> findByInitiatedByWithQueries(@Param("username") String username);
    
    @Query("SELECT DISTINCT w FROM MaterialWorkflow w LEFT JOIN FETCH w.queries")
    List<MaterialWorkflow> findAllWithQueries();
    
    @Query("SELECT DISTINCT w FROM MaterialWorkflow w LEFT JOIN FETCH w.queries WHERE w.state = :state")
    List<MaterialWorkflow> findByStateWithQueries(@Param("state") WorkflowState state);
    
    @Query("SELECT DISTINCT w FROM MaterialWorkflow w LEFT JOIN FETCH w.queries WHERE w.plantCode = :plantCode")
    List<MaterialWorkflow> findByPlantCodeWithQueries(@Param("plantCode") String plantCode);
    

    
    // Material ID existence check
    boolean existsByMaterialCode(String materialCode);
    
    // Enhanced workflow existence check
    boolean existsByProjectCodeAndMaterialCodeAndPlantCodeAndBlockId(String projectCode, String materialCode, String plantCode, String blockId);
    
    // Workflow filtering for enhanced dashboard
    @Query("SELECT w FROM MaterialWorkflow w WHERE " +
           "(:projectCode IS NULL OR w.projectCode = :projectCode) AND " +
           "(:materialCode IS NULL OR w.materialCode = :materialCode) AND " +
           "(:plantCode IS NULL OR w.plantCode = :plantCode) AND " +
           "(:state IS NULL OR w.state = :state) " +
           "ORDER BY w.createdAt DESC")
    List<MaterialWorkflow> findWorkflowsWithFilters(
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode, 
        @Param("plantCode") String plantCode,
        @Param("state") WorkflowState state);
    
    // Admin monitoring specific queries
    long countByStateNot(WorkflowState state);
    long countByStateNotAndCreatedAtBefore(WorkflowState state, LocalDateTime dateTime);
    
    List<MaterialWorkflow> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<MaterialWorkflow> findByCreatedAtBefore(LocalDateTime dateTime);
    List<MaterialWorkflow> findByStateAndCreatedAtBetween(WorkflowState state, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT w.plantCode, COUNT(w) FROM MaterialWorkflow w GROUP BY w.plantCode")
    List<Object[]> countByPlantCodeGrouped();
    
    @Query(value = "SELECT TRUNC(created_at), COUNT(*) FROM qrmfg_material_workflow WHERE created_at >= :startDate GROUP BY TRUNC(created_at) ORDER BY TRUNC(created_at)", nativeQuery = true)
    List<Object[]> countByCreatedAtAfterGroupByDay(@Param("startDate") LocalDateTime startDate);
    
    @Query(value = "SELECT AVG((last_modified - created_at) * 24) FROM qrmfg_material_workflow WHERE state = 'COMPLETED'", nativeQuery = true)
    Double calculateAverageCompletionTimeHours();
    
    @Query(value = "SELECT state, AVG((last_modified - created_at) * 24) FROM qrmfg_material_workflow GROUP BY state", nativeQuery = true)
    List<Object[]> calculateAverageTimeInEachStateGrouped();
    
    @Query(value = "SELECT state, COUNT(*) FROM qrmfg_material_workflow WHERE state != 'COMPLETED' AND ((state = 'JVC_PENDING' AND (SYSDATE - created_at) > 7) OR (state = 'PLANT_PENDING' AND (SYSDATE - NVL(extended_at, created_at)) > 7) OR (state IN ('CQS_PENDING', 'TECH_PENDING') AND (SYSDATE - last_modified) > 7)) GROUP BY state", nativeQuery = true)
    List<Object[]> countOverdueWorkflowsByStateGrouped();
    
    @Query(value = "SELECT plant_code, COUNT(*) FROM qrmfg_material_workflows WHERE workflow_state != 'COMPLETED' AND ((workflow_state = 'JVC_PENDING' AND (SYSDATE - created_at) > 7) OR (workflow_state = 'PLANT_PENDING' AND (SYSDATE - NVL(last_modified, created_at)) > 7) OR (workflow_state IN ('CQS_PENDING', 'TECH_PENDING', 'JVC_PENDING') AND (SYSDATE - last_modified) > 7)) GROUP BY plant_code", nativeQuery = true)
    List<Object[]> countDelayedWorkflowsByPlantGrouped();

    // Enhanced dashboard data queries for project/material filtering
    @Query("SELECT w FROM MaterialWorkflow w WHERE " +
           "(:projectCode IS NULL OR w.projectCode = :projectCode) AND " +
           "(:materialCode IS NULL OR w.materialCode = :materialCode) AND " +
           "(:plantCode IS NULL OR w.plantCode = :plantCode) AND " +
           "(:blockId IS NULL OR w.blockId = :blockId) AND " +
           "(:state IS NULL OR w.state = :state) AND " +
           "(:initiatedBy IS NULL OR w.initiatedBy = :initiatedBy) " +
           "ORDER BY w.createdAt DESC")
    List<MaterialWorkflow> findWorkflowsWithAdvancedFilters(
        @Param("projectCode") String projectCode,
        @Param("materialCode") String materialCode, 
        @Param("plantCode") String plantCode,
        @Param("blockId") String blockId,
        @Param("state") WorkflowState state,
        @Param("initiatedBy") String initiatedBy);

    // Dashboard summary queries
    @Query("SELECT w.state, COUNT(w) FROM MaterialWorkflow w WHERE w.state != 'COMPLETED' GROUP BY w.state")
    List<Object[]> getPendingWorkflowCountByState();

    @Query("SELECT w.projectCode, COUNT(w) FROM MaterialWorkflow w WHERE w.state != 'COMPLETED' GROUP BY w.projectCode ORDER BY COUNT(w) DESC")
    List<Object[]> getPendingWorkflowCountByProject();

    @Query("SELECT w.plantCode, w.state, COUNT(w) FROM MaterialWorkflow w WHERE w.state != 'COMPLETED' GROUP BY w.plantCode, w.state ORDER BY w.plantCode, w.state")
    List<Object[]> getPendingWorkflowCountByPlantAndState();

    // Performance and analytics queries
    @Query(value = "SELECT AVG((CASE WHEN completed_at IS NOT NULL THEN completed_at ELSE SYSDATE END - created_at) * 24) FROM qrmfg_material_workflows WHERE workflow_state = 'COMPLETED' AND created_at >= :startDate", nativeQuery = true)
    Double calculateAverageCompletionTimeHoursSince(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT workflow_state, COUNT(*), AVG((last_modified - created_at) * 24) FROM qrmfg_material_workflows WHERE created_at >= :startDate GROUP BY workflow_state", nativeQuery = true)
    List<Object[]> getWorkflowMetricsByState(@Param("startDate") LocalDateTime startDate);

    // Material reuse analysis
    @Query("SELECT w.projectCode, w.materialCode, COUNT(w) as workflowCount FROM MaterialWorkflow w GROUP BY w.projectCode, w.materialCode HAVING COUNT(w) > 1 ORDER BY COUNT(w) DESC")
    List<Object[]> findFrequentlyUsedProjectMaterialCombinations();

    // SLA and urgency queries
    @Query(value = "SELECT * FROM qrmfg_material_workflows WHERE workflow_state != 'COMPLETED' AND " +
           "((workflow_state = 'JVC_PENDING' AND (SYSDATE - created_at) > :jvcSlaHours/24) OR " +
           "(workflow_state = 'PLANT_PENDING' AND (SYSDATE - NVL(extended_at, created_at)) > :plantSlaHours/24) OR " +
           "(workflow_state IN ('CQS_PENDING', 'TECH_PENDING') AND (SYSDATE - last_modified) > :querySlaHours/24))", nativeQuery = true)
    List<MaterialWorkflow> findWorkflowsOverSLA(@Param("jvcSlaHours") int jvcSlaHours, @Param("plantSlaHours") int plantSlaHours, @Param("querySlaHours") int querySlaHours);

    // Bulk operations support
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.id IN :workflowIds")
    List<MaterialWorkflow> findByIds(@Param("workflowIds") List<Long> workflowIds);

    @Query("SELECT w FROM MaterialWorkflow w WHERE w.projectCode IN :projectCodes AND w.state != 'COMPLETED'")
    List<MaterialWorkflow> findPendingByProjectCodes(@Param("projectCodes") List<String> projectCodes);

    @Query("SELECT w FROM MaterialWorkflow w WHERE w.materialCode IN :materialCodes AND w.state != 'COMPLETED'")
    List<MaterialWorkflow> findPendingByMaterialCodes(@Param("materialCodes") List<String> materialCodes);

    // Enhanced dashboard data queries with comprehensive filtering
    @Query("SELECT w FROM MaterialWorkflow w WHERE " +
           "(:projectCodes IS NULL OR w.projectCode IN :projectCodes) AND " +
           "(:materialCodes IS NULL OR w.materialCode IN :materialCodes) AND " +
           "(:plantCodes IS NULL OR w.plantCode IN :plantCodes) AND " +
           "(:states IS NULL OR w.state IN :states) AND " +
           "(:initiatedBy IS NULL OR w.initiatedBy = :initiatedBy) AND " +
           "(:priorityLevel IS NULL OR w.priorityLevel = :priorityLevel) AND " +
           "(:createdAfter IS NULL OR w.createdAt >= :createdAfter) AND " +
           "(:createdBefore IS NULL OR w.createdAt <= :createdBefore) " +
           "ORDER BY w.createdAt DESC")
    List<MaterialWorkflow> findWorkflowsWithBulkFilters(
        @Param("projectCodes") List<String> projectCodes,
        @Param("materialCodes") List<String> materialCodes,
        @Param("plantCodes") List<String> plantCodes,
        @Param("states") List<WorkflowState> states,
        @Param("initiatedBy") String initiatedBy,
        @Param("priorityLevel") String priorityLevel,
        @Param("createdAfter") LocalDateTime createdAfter,
        @Param("createdBefore") LocalDateTime createdBefore);

    // Dashboard summary statistics
    @Query("SELECT w.priorityLevel, w.state, COUNT(w) FROM MaterialWorkflow w WHERE w.state != 'COMPLETED' GROUP BY w.priorityLevel, w.state ORDER BY w.priorityLevel, w.state")
    List<Object[]> getPendingWorkflowCountByPriorityAndState();

    @Query(value = "SELECT TRUNC(created_at), COUNT(*) FROM qrmfg_material_workflows WHERE created_at >= :startDate GROUP BY TRUNC(created_at) ORDER BY TRUNC(created_at)", nativeQuery = true)
    List<Object[]> getWorkflowCreationTrend(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT TRUNC(completed_at), COUNT(*) FROM qrmfg_material_workflows WHERE completed_at >= :startDate AND workflow_state = 'COMPLETED' GROUP BY TRUNC(completed_at) ORDER BY TRUNC(completed_at)", nativeQuery = true)
    List<Object[]> getWorkflowCompletionTrend(@Param("startDate") LocalDateTime startDate);

    // Performance metrics for dashboard
    @Query(value = "SELECT w.plant_code, w.workflow_state, COUNT(*) as count, AVG((CASE WHEN w.completed_at IS NOT NULL THEN w.completed_at ELSE SYSDATE END - w.created_at) * 24) as avg_hours FROM qrmfg_material_workflows w WHERE w.created_at >= :startDate GROUP BY w.plant_code, w.workflow_state ORDER BY w.plant_code, w.workflow_state", nativeQuery = true)
    List<Object[]> getWorkflowPerformanceByPlantAndState(@Param("startDate") LocalDateTime startDate);

    // Material reuse analytics for dashboard
    @Query("SELECT w.projectCode, w.materialCode, COUNT(w) as usageCount, COUNT(DISTINCT w.plantCode) as plantCount FROM MaterialWorkflow w GROUP BY w.projectCode, w.materialCode HAVING COUNT(w) > 1 ORDER BY COUNT(w) DESC")
    List<Object[]> getMaterialReuseAnalytics();

    // User activity analytics
    @Query("SELECT w.initiatedBy, COUNT(w) as totalWorkflows, COUNT(CASE WHEN w.state = 'COMPLETED' THEN 1 END) as completedWorkflows FROM MaterialWorkflow w WHERE w.createdAt >= :startDate GROUP BY w.initiatedBy ORDER BY COUNT(w) DESC")
    List<Object[]> getUserActivityStats(@Param("startDate") LocalDateTime startDate);

    // Workflow aging analysis
    @Query(value = "SELECT workflow_state, COUNT(*) as count, AVG((SYSDATE - created_at) * 24) as avg_age_hours, MAX((SYSDATE - created_at) * 24) as max_age_hours FROM qrmfg_material_workflows WHERE workflow_state != 'COMPLETED' GROUP BY workflow_state", nativeQuery = true)
    List<Object[]> getWorkflowAgingAnalysis();

    // Project/Material combination insights
    @Query("SELECT w.projectCode, COUNT(DISTINCT w.materialCode) as materialCount, COUNT(DISTINCT w.plantCode) as plantCount, COUNT(w) as totalWorkflows FROM MaterialWorkflow w GROUP BY w.projectCode ORDER BY COUNT(w) DESC")
    List<Object[]> getProjectInsights();

    // Plant workload distribution
    @Query("SELECT w.plantCode, w.blockId, COUNT(w) as workflowCount, COUNT(CASE WHEN w.state != 'COMPLETED' THEN 1 END) as pendingCount FROM MaterialWorkflow w GROUP BY w.plantCode, w.blockId ORDER BY w.plantCode, w.blockId")
    List<Object[]> getPlantWorkloadDistribution();

    // Priority-based filtering for dashboard
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.state != 'COMPLETED' AND (" +
           "w.priorityLevel IN ('HIGH', 'URGENT') OR " +
           "(w.state = 'JVC_PENDING' AND w.createdAt < CURRENT_TIMESTAMP - 2) OR " +
           "(w.state = 'PLANT_PENDING' AND COALESCE(w.extendedAt, w.createdAt) < CURRENT_TIMESTAMP - 2) OR " +
           "(w.state IN ('CQS_PENDING', 'TECH_PENDING') AND w.lastModified < CURRENT_TIMESTAMP - 1)" +
           ") ORDER BY w.priorityLevel DESC, w.createdAt ASC")
    List<MaterialWorkflow> findHighPriorityAndOverdueWorkflows();

    // Document-related workflow queries
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.id IN (SELECT DISTINCT wd.workflow.id FROM WorkflowDocument wd)")
    List<MaterialWorkflow> findWorkflowsWithDocuments();

    @Query("SELECT w FROM MaterialWorkflow w WHERE w.id NOT IN (SELECT DISTINCT wd.workflow.id FROM WorkflowDocument wd WHERE wd.workflow.id IS NOT NULL)")
    List<MaterialWorkflow> findWorkflowsWithoutDocuments();

    @Query("SELECT w.projectCode, w.materialCode, COUNT(DISTINCT w.id) as workflowCount, COUNT(DISTINCT wd.id) as documentCount FROM MaterialWorkflow w LEFT JOIN w.documents wd GROUP BY w.projectCode, w.materialCode ORDER BY COUNT(DISTINCT wd.id) DESC")
    List<Object[]> getDocumentUsageByProjectMaterial();
}