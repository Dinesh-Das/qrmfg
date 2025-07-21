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
public interface MaterialWorkflowRepository extends JpaRepository<MaterialWorkflow, Long> {
    
    // Basic finders
    Optional<MaterialWorkflow> findByMaterialId(String materialId);
    List<MaterialWorkflow> findByState(WorkflowState state);
    List<MaterialWorkflow> findByAssignedPlant(String assignedPlant);
    List<MaterialWorkflow> findByInitiatedBy(String initiatedBy);
    
    // State-based queries
    List<MaterialWorkflow> findByStateIn(List<WorkflowState> states);
    List<MaterialWorkflow> findByStateNot(WorkflowState state);
    
    // Time-based queries
    List<MaterialWorkflow> findByCreatedAtAfter(LocalDateTime dateTime);
    List<MaterialWorkflow> findByCompletedAtAfter(LocalDateTime dateTime);
    List<MaterialWorkflow> findByCompletedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Custom queries for dashboard and reporting
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.state != 'COMPLETED'")
    List<MaterialWorkflow> findPendingWorkflows();
    
    @Query(value = "SELECT * FROM qrmfg_material_workflow WHERE state != 'COMPLETED' AND ((state = 'JVC_PENDING' AND (SYSDATE - created_at) > 3) OR (state = 'PLANT_PENDING' AND (SYSDATE - NVL(extended_at, created_at)) > 3) OR (state IN ('CQS_PENDING', 'TECH_PENDING') AND (SYSDATE - last_modified) > 3))", nativeQuery = true)
    List<MaterialWorkflow> findOverdueWorkflows();
    
    @Query("SELECT DISTINCT w FROM MaterialWorkflow w JOIN w.queries q WHERE q.status = 'OPEN'")
    List<MaterialWorkflow> findWorkflowsWithOpenQueries();
    
    // Count queries
    long countByState(WorkflowState state);
    
    @Query(value = "SELECT COUNT(*) FROM qrmfg_material_workflow WHERE state != 'COMPLETED' AND ((state = 'JVC_PENDING' AND (SYSDATE - created_at) > 3) OR (state = 'PLANT_PENDING' AND (SYSDATE - NVL(extended_at, created_at)) > 3) OR (state IN ('CQS_PENDING', 'TECH_PENDING') AND (SYSDATE - last_modified) > 3))", nativeQuery = true)
    long countOverdueWorkflows();
    
    @Query(value = "SELECT COUNT(DISTINCT w.id) FROM qrmfg_material_workflow w JOIN qrmfg_query q ON w.id = q.workflow_id WHERE q.status = 'OPEN'", nativeQuery = true)
    long countWorkflowsWithOpenQueries();
    
    // Plant-specific queries
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.assignedPlant = :plant AND w.state = :state")
    List<MaterialWorkflow> findByPlantAndState(@Param("plant") String plant, @Param("state") WorkflowState state);
    
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.assignedPlant = :plant AND w.state != 'COMPLETED'")
    List<MaterialWorkflow> findPendingByPlant(@Param("plant") String plant);
    
    // User-specific queries
    @Query("SELECT w FROM MaterialWorkflow w WHERE w.initiatedBy = :username AND w.state != 'COMPLETED'")
    List<MaterialWorkflow> findPendingByInitiatedBy(@Param("username") String username);
    
    // Material ID existence check
    boolean existsByMaterialId(String materialId);
    
    // Admin monitoring specific queries
    long countByStateNot(WorkflowState state);
    long countByStateNotAndCreatedAtBefore(WorkflowState state, LocalDateTime dateTime);
    
    List<MaterialWorkflow> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<MaterialWorkflow> findByCreatedAtBefore(LocalDateTime dateTime);
    List<MaterialWorkflow> findByStateAndCreatedAtBetween(WorkflowState state, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT w.assignedPlant, COUNT(w) FROM MaterialWorkflow w GROUP BY w.assignedPlant")
    List<Object[]> countByAssignedPlantGrouped();
    
    @Query(value = "SELECT TRUNC(created_at), COUNT(*) FROM qrmfg_material_workflow WHERE created_at >= :startDate GROUP BY TRUNC(created_at) ORDER BY TRUNC(created_at)", nativeQuery = true)
    List<Object[]> countByCreatedAtAfterGroupByDay(@Param("startDate") LocalDateTime startDate);
    
    @Query(value = "SELECT AVG((last_modified - created_at) * 24) FROM qrmfg_material_workflow WHERE state = 'COMPLETED'", nativeQuery = true)
    Double calculateAverageCompletionTimeHours();
    
    @Query(value = "SELECT state, AVG((last_modified - created_at) * 24) FROM qrmfg_material_workflow GROUP BY state", nativeQuery = true)
    List<Object[]> calculateAverageTimeInEachStateGrouped();
    
    @Query(value = "SELECT state, COUNT(*) FROM qrmfg_material_workflow WHERE state != 'COMPLETED' AND ((state = 'JVC_PENDING' AND (SYSDATE - created_at) > 7) OR (state = 'PLANT_PENDING' AND (SYSDATE - NVL(extended_at, created_at)) > 7) OR (state IN ('CQS_PENDING', 'TECH_PENDING') AND (SYSDATE - last_modified) > 7)) GROUP BY state", nativeQuery = true)
    List<Object[]> countOverdueWorkflowsByStateGrouped();
    
    @Query(value = "SELECT assigned_plant, COUNT(*) FROM qrmfg_material_workflow WHERE state != 'COMPLETED' AND ((state = 'JVC_PENDING' AND (SYSDATE - created_at) > 7) OR (state = 'PLANT_PENDING' AND (SYSDATE - NVL(extended_at, created_at)) > 7) OR (state IN ('CQS_PENDING', 'TECH_PENDING') AND (SYSDATE - last_modified) > 7)) GROUP BY assigned_plant", nativeQuery = true)
    List<Object[]> countDelayedWorkflowsByPlantGrouped();
}