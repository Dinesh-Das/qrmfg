package com.cqs.qrmfg.service.impl;

import com.cqs.qrmfg.exception.QueryAlreadyResolvedException;
import com.cqs.qrmfg.exception.QueryException;
import com.cqs.qrmfg.exception.QueryNotFoundException;
import com.cqs.qrmfg.exception.WorkflowNotFoundException;
import com.cqs.qrmfg.model.Workflow;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import com.cqs.qrmfg.model.WorkflowState;
import com.cqs.qrmfg.repository.WorkflowRepository;
import com.cqs.qrmfg.repository.QueryRepository;
import com.cqs.qrmfg.service.NotificationService;
import com.cqs.qrmfg.service.QueryService;
import com.cqs.qrmfg.service.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class QueryServiceImpl implements QueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryServiceImpl.class);
    private static final int DEFAULT_SLA_HOURS = 72; // 3 days
    
    @Autowired
    private QueryRepository queryRepository;
    
    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private WorkflowService workflowService;
    
    @Autowired
    private NotificationService notificationService;
    
    // Basic CRUD operations
    @Override
    public Query save(Query query) {
        logger.debug("Saving query: {}", query.getQuestion());
        return queryRepository.save(query);
    }
    
    @Override
    public Query update(Query query) {
        if (query.getId() == null) {
            throw new QueryException("Cannot update query without ID");
        }
        
        Optional<Query> existingOpt = queryRepository.findById(query.getId());
        if (!existingOpt.isPresent()) {
            throw new QueryNotFoundException(query.getId());
        }
        
        logger.debug("Updating query ID: {}", query.getId());
        return queryRepository.save(query);
    }
    
    @Override
    public void delete(Long id) {
        if (!queryRepository.existsById(id)) {
            throw new QueryNotFoundException(id);
        }
        logger.debug("Deleting query with ID: {}", id);
        queryRepository.deleteById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Query> findById(Long id) {
        return queryRepository.findById(id);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findAll() {
        return queryRepository.findAll();
    }
    
    // Query creation
    @Override
    public Query createQuery(Long workflowId, String question, QueryTeam assignedTeam, String raisedBy) {
        return createQuery(workflowId, question, null, null, assignedTeam, raisedBy);
    }
    
    @Override
    public Query createQuery(Long workflowId, String question, Integer stepNumber, String fieldName,
                           QueryTeam assignedTeam, String raisedBy) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        validateQueryCreation(workflowId, assignedTeam);
        
        Query query = new Query(workflow, question, stepNumber, fieldName, assignedTeam, raisedBy);
        
        logger.info("Creating query for workflow {} assigned to {} by user: {}", 
                   workflow.getMaterialCode(), assignedTeam, raisedBy);
        
        Query savedQuery = queryRepository.save(query);
        
        // TODO: Temporarily disabled to debug 500 error
        // Transition workflow to appropriate query state
        // WorkflowState queryState = assignedTeam.getCorrespondingWorkflowState();
        // workflowService.transitionToState(workflowId, queryState, raisedBy);
        
        // Send comprehensive notifications for query creation
        try {
            // Notify the assigned team about the new query
            // notificationService.notifyQueryRaised(savedQuery);
            
            // Also notify as a query assignment to the team
            // notificationService.notifyQueryAssigned(savedQuery, raisedBy);
            logger.info("Query created successfully, notifications temporarily disabled for debugging");
        } catch (Exception e) {
            logger.warn("Failed to send query raised notification for query {}: {}", 
                       savedQuery.getId(), e.getMessage());
        }
        
        return savedQuery;
    }
    
    @Override
    public Query createQuery(String materialCode, String question, QueryTeam assignedTeam, String raisedBy) {
        Workflow workflow = workflowRepository.findByMaterialCode(materialCode)
            .stream().findFirst().orElseThrow(() -> WorkflowNotFoundException.forMaterialCode(materialCode));
        
        return createQuery(workflow.getId(), question, assignedTeam, raisedBy);
    }
    
    @Override
    public Query createQuery(String materialCode, String question, Integer stepNumber, String fieldName,
                           QueryTeam assignedTeam, String raisedBy) {
        Workflow workflow = workflowRepository.findByMaterialCode(materialCode)
            .stream().findFirst().orElseThrow(() -> WorkflowNotFoundException.forMaterialCode(materialCode));
        
        return createQuery(workflow.getId(), question, stepNumber, fieldName, assignedTeam, raisedBy);
    }
    
    // Query resolution
    @Override
    public Query resolveQuery(Long queryId, String response, String resolvedBy) {
        return resolveQuery(queryId, response, resolvedBy, null);
    }
    
    @Override
    public Query resolveQuery(Long queryId, String response, String resolvedBy, String priorityLevel) {
        Query query = queryRepository.findById(queryId)
            .orElseThrow(() -> new QueryNotFoundException(queryId));
        
        validateQueryResolution(query, resolvedBy);
        
        if (query.getStatus() == QueryStatus.RESOLVED) {
            throw new QueryAlreadyResolvedException(queryId);
        }
        
        logger.info("Resolving query {} for workflow {} by user: {}", 
                   queryId, query.getWorkflow().getMaterialCode(), resolvedBy);
        
        query.resolve(response, resolvedBy);
        
        if (priorityLevel != null) {
            query.setPriorityLevel(priorityLevel);
        }
        
        Query resolvedQuery = queryRepository.save(query);
        
        // Send notification for query resolution
        try {
            notificationService.notifyQueryResolved(resolvedQuery);
        } catch (Exception e) {
            logger.warn("Failed to send query resolved notification for query {}: {}", 
                       resolvedQuery.getId(), e.getMessage());
        }
        
        // Check if workflow can return to PLANT_PENDING state
        Workflow workflow = query.getWorkflow();
        if (!workflow.hasOpenQueries()) {
            workflowService.returnFromQueryState(workflow.getId(), resolvedBy);
        }
        
        return resolvedQuery;
    }
    
    @Override
    public void bulkResolveQueries(List<Long> queryIds, String response, String resolvedBy) {
        for (Long queryId : queryIds) {
            try {
                resolveQuery(queryId, response, resolvedBy);
            } catch (Exception e) {
                logger.error("Failed to resolve query {}: {}", queryId, e.getMessage());
                // Continue with other queries
            }
        }
    }
    
    // Query assignment and management
    @Override
    public Query assignToTeam(Long queryId, QueryTeam newTeam, String updatedBy) {
        Query query = queryRepository.findById(queryId)
            .orElseThrow(() -> new QueryNotFoundException(queryId));
        
        if (query.getStatus() == QueryStatus.RESOLVED) {
            throw new QueryException("Cannot reassign resolved query");
        }
        
        QueryTeam oldTeam = query.getAssignedTeam();
        query.setAssignedTeam(newTeam);
        query.setUpdatedBy(updatedBy);
        
        logger.info("Reassigning query {} from {} to {} by user: {}", 
                   queryId, oldTeam, newTeam, updatedBy);
        
        Query updatedQuery = queryRepository.save(query);
        
        // Send notification for query assignment
        try {
            notificationService.notifyQueryAssigned(updatedQuery, updatedBy);
        } catch (Exception e) {
            logger.warn("Failed to send query assignment notification for query {}: {}", 
                       updatedQuery.getId(), e.getMessage());
        }
        
        // Update workflow state if necessary
        WorkflowState newState = newTeam.getCorrespondingWorkflowState();
        workflowService.transitionToState(query.getWorkflow().getId(), newState, updatedBy);
        
        return updatedQuery;
    }
    
    @Override
    public Query updatePriority(Long queryId, String priorityLevel, String updatedBy) {
        Query query = queryRepository.findById(queryId)
            .orElseThrow(() -> new QueryNotFoundException(queryId));
        
        query.setPriorityLevel(priorityLevel);
        query.setUpdatedBy(updatedBy);
        
        logger.debug("Updated priority for query {} to {} by user: {}", queryId, priorityLevel, updatedBy);
        return queryRepository.save(query);
    }
    
    @Override
    public Query updateCategory(Long queryId, String category, String updatedBy) {
        Query query = queryRepository.findById(queryId)
            .orElseThrow(() -> new QueryNotFoundException(queryId));
        
        query.setQueryCategory(category);
        query.setUpdatedBy(updatedBy);
        
        logger.debug("Updated category for query {} to {} by user: {}", queryId, category, updatedBy);
        return queryRepository.save(query);
    }
    
    // Query search and filtering
    @Override
    @Transactional(readOnly = true)
    public List<Query> findByWorkflowId(Long workflowId) {
        return queryRepository.findByWorkflow_Id(workflowId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findByMaterialCode(String materialCode) {
        return queryRepository.findByWorkflow_MaterialCode(materialCode);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findByStatus(QueryStatus status) {
        return queryRepository.findByStatus(status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findByAssignedTeam(QueryTeam team) {
        return queryRepository.findByAssignedTeam(team);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findByRaisedBy(String username) {
        return queryRepository.findByRaisedBy(username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findByResolvedBy(String username) {
        return queryRepository.findByResolvedBy(username);
    }
    
    // Team-specific query inbox
    @Override
    @Transactional(readOnly = true)
    public List<Query> findOpenQueriesForTeam(QueryTeam team) {
        return queryRepository.findTeamInboxQueries(team);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findResolvedQueriesForTeam(QueryTeam team) {
        return queryRepository.findTeamResolvedQueries(team);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findQueriesForTeam(QueryTeam team, QueryStatus status) {
        return queryRepository.findByAssignedTeamAndStatus(team, status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findMyRaisedQueries(String username) {
        return queryRepository.findQueriesRaisedByUser(username);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findMyResolvedQueries(String username) {
        return queryRepository.findQueriesResolvedByUser(username);
    }
    
    // Query filtering with criteria
    @Override
    @Transactional(readOnly = true)
    public List<Query> findQueriesByWorkflowAndStatus(Long workflowId, QueryStatus status) {
        return queryRepository.findByWorkflow_IdAndStatus(workflowId, status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findQueriesByTeamAndStatus(QueryTeam team, QueryStatus status) {
        return queryRepository.findByAssignedTeamAndStatus(team, status);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findQueriesByPriority(String priorityLevel) {
        return queryRepository.findByPriorityLevel(priorityLevel);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findOverdueQueries() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(72); // Consider queries older than 72 hours (3 days) as overdue
        return queryRepository.findOverdueQueries(cutoffTime);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findRecentQueries(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        return queryRepository.findRecentQueries(cutoffDate);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findQueriesCreatedBetween(LocalDateTime start, LocalDateTime end) {
        return queryRepository.findByCreatedAtBetween(start, end);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findQueriesResolvedBetween(LocalDateTime start, LocalDateTime end) {
        return queryRepository.findByResolvedAtBetween(start, end);
    }
    
    // SLA and metrics
    @Override
    @Transactional(readOnly = true)
    public List<Query> findQueriesOverSLA(int slaHours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(slaHours);
        return queryRepository.findQueriesOverSLA(cutoffTime);
    }
    
    @Override
    @Transactional(readOnly = true)
    public double getAverageResolutionTimeHours(QueryTeam team) {
        Double avgTime = queryRepository.getAverageResolutionTimeHours(team);
        return avgTime != null ? avgTime : 0.0;
    }
    
    @Override
    @Transactional(readOnly = true)
    public double getAverageResolutionTimeHours(QueryTeam team, LocalDateTime start, LocalDateTime end) {
        Double avgTime = queryRepository.getAverageResolutionTimeHours(team, start, end);
        return avgTime != null ? avgTime : 0.0;
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countOpenQueriesByTeam(QueryTeam team) {
        return queryRepository.countByAssignedTeamAndStatus(team, QueryStatus.OPEN);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countResolvedQueriesByTeam(QueryTeam team) {
        return queryRepository.countByAssignedTeamAndStatus(team, QueryStatus.RESOLVED);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countOverdueQueries() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(72); // Consider queries older than 72 hours (3 days) as overdue
        return queryRepository.countOverdueQueries(cutoffTime);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countOverdueQueriesByTeam(QueryTeam team) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(72); // Consider queries older than 72 hours (3 days) as overdue
        return queryRepository.countOverdueQueriesByTeam(team, cutoffTime);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countQueriesCreatedToday() {
        return queryRepository.countQueriesCreatedToday();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countQueriesResolvedToday() {
        return queryRepository.countQueriesResolvedToday();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countQueriesResolvedTodayByTeam(QueryTeam team) {
        return queryRepository.countQueriesResolvedTodayByTeam(team);
    }
    
    // Dashboard and reporting
    @Override
    @Transactional(readOnly = true)
    public List<Query> findPendingQueriesForDashboard() {
        return queryRepository.findPendingQueriesForDashboard();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findHighPriorityQueries() {
        return queryRepository.findHighPriorityQueries();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findQueriesNeedingAttention() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(48); // Queries older than 48 hours need attention
        return queryRepository.findQueriesNeedingAttention(cutoffTime);
    }
    
    // Validation and business rules
    @Override
    public void validateQueryCreation(Long workflowId, QueryTeam assignedTeam) {
        Workflow workflow = workflowRepository.findById(workflowId)
            .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
        
        // Can only create queries from PLANT_PENDING state
        if (workflow.getState() != WorkflowState.PLANT_PENDING) {
            throw new QueryException(
                String.format("Cannot create query for workflow %s in state %s. Must be in PLANT_PENDING state.",
                             workflow.getMaterialCode(), workflow.getState()));
        }
        
        // Validate team assignment
        if (assignedTeam == null) {
            throw new QueryException("Query must be assigned to a team");
        }
    }
    
    @Override
    public void validateQueryResolution(Query query, String resolvedBy) {
        if (query.getStatus() == QueryStatus.RESOLVED) {
            throw new QueryAlreadyResolvedException(query.getId());
        }
        
        if (resolvedBy == null || resolvedBy.trim().isEmpty()) {
            throw new QueryException("Query must be resolved by a valid user");
        }
        
        // Additional business rules can be added here
        // For example: check if user has permission to resolve queries for this team
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canUserResolveQuery(Query query, String username) {
        // Basic implementation - can be enhanced with role-based checks
        return query.getStatus() == QueryStatus.OPEN && username != null && !username.trim().isEmpty();
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isQueryOverdue(Query query) {
        if (query.getStatus() == QueryStatus.RESOLVED) {
            return false;
        }
        return query.getDaysOpen() > 3; // Business rule: overdue after 3 days
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isQueryHighPriority(Query query) {
        return query.isHighPriority();
    }
    
    // Workflow integration
    @Override
    public void handleWorkflowStateChange(Long workflowId, String previousState, String newState) {
        // Handle any query-related logic when workflow state changes
        logger.debug("Handling workflow state change for workflow {}: {} -> {}", 
                    workflowId, previousState, newState);
        
        // Additional logic can be added here based on business requirements
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Query> findQueriesBlockingWorkflow(Long workflowId) {
        return queryRepository.findOpenQueriesByWorkflow(workflowId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasWorkflowOpenQueries(Long workflowId) {
        return queryRepository.hasWorkflowOpenQueries(workflowId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Query> searchQueriesWithContext(String materialCode, String projectCode, String plantCode, String blockId, String team, String status, String priority, int minDaysOpen) {
        // TODO: Implement actual search logic
        return java.util.Collections.emptyList();
    }
}