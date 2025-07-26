package com.cqs.qrmfg.controller;

import com.cqs.qrmfg.dto.QueryCreateRequest;
import com.cqs.qrmfg.dto.QueryResolveRequest;
import com.cqs.qrmfg.dto.QuerySummaryDto;
import com.cqs.qrmfg.exception.QueryException;
import com.cqs.qrmfg.exception.QueryNotFoundException;
import com.cqs.qrmfg.model.Query;
import com.cqs.qrmfg.model.QueryStatus;
import com.cqs.qrmfg.model.QueryTeam;
import com.cqs.qrmfg.model.User;
import com.cqs.qrmfg.service.QueryService;
import com.cqs.qrmfg.util.QueryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/qrmfg/api/v1/queries")
public class QueryController {

    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    @Autowired
    private QueryService queryService;

    @Autowired
    private QueryMapper queryMapper;

    // Basic CRUD operations
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getAllQueries() {
        List<Query> queries = queryService.findAll();
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Query> getQueryById(@PathVariable Long id) {
        Optional<Query> query = queryService.findById(id);
        return query.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Query creation - Plant users only
    @PostMapping("/workflow/{workflowId}")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Query> createQueryForWorkflow(
            @PathVariable Long workflowId,
            @Valid @RequestBody QueryCreateRequest request,
            Authentication authentication) {
        
        try {
            logger.info("Creating query for workflow {} with request: {}", workflowId, request);
            
            String raisedBy = getCurrentUsername(authentication);
            logger.debug("Query raised by user: {}", raisedBy);
            
            Query query = queryService.createQuery(
                workflowId,
                request.getQuestion(),
                request.getStepNumber(),
                request.getFieldName(),
                request.getAssignedTeam(),
                raisedBy
            );
            
            if (request.getPriorityLevel() != null) {
                query.setPriorityLevel(request.getPriorityLevel());
            }
            if (request.getQueryCategory() != null) {
                query.setQueryCategory(request.getQueryCategory());
            }
            
            Query savedQuery = queryService.save(query);
            logger.info("Successfully created query with ID: {}", savedQuery.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedQuery);
        } catch (Exception e) {
            logger.error("Failed to create query for workflow {}: {}", workflowId, e.getMessage(), e);
            throw e; // Re-throw to let the exception handler deal with it
        }
    }

    @PostMapping("/material/{materialCode}")
    @PreAuthorize("hasRole('PLANT_USER') or hasRole('ADMIN')")
    public ResponseEntity<Query> createQueryForMaterial(
            @PathVariable String materialCode,
            @Valid @RequestBody QueryCreateRequest request,
            Authentication authentication) {
        
        String raisedBy = getCurrentUsername(authentication);
        
        Query query = queryService.createQuery(
            materialCode,
            request.getQuestion(),
            request.getStepNumber(),
            request.getFieldName(),
            request.getAssignedTeam(),
            raisedBy
        );
        
        if (request.getPriorityLevel() != null) {
            query.setPriorityLevel(request.getPriorityLevel());
        }
        if (request.getQueryCategory() != null) {
            query.setQueryCategory(request.getQueryCategory());
        }
        
        Query savedQuery = queryService.save(query);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedQuery);
    }

    // Query resolution - CQS/Tech/JVC users only
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<Query> resolveQuery(
            @PathVariable Long id,
            @Valid @RequestBody QueryResolveRequest request,
            Authentication authentication) {
        
        String resolvedBy = getCurrentUsername(authentication);
        Query query = queryService.resolveQuery(id, request.getResponse(), resolvedBy, request.getPriorityLevel());
        return ResponseEntity.ok(query);
    }

    @PutMapping("/bulk-resolve")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> bulkResolveQueries(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        @SuppressWarnings("unchecked")
        List<Long> queryIds = (List<Long>) request.get("queryIds");
        String response = (String) request.get("response");
        String resolvedBy = getCurrentUsername(authentication);
        
        queryService.bulkResolveQueries(queryIds, response, resolvedBy);
        return ResponseEntity.ok().build();
    }

    // Query management
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Query> assignToTeam(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String teamStr = request.get("team");
        if (teamStr == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            QueryTeam team = QueryTeam.valueOf(teamStr);
            String updatedBy = getCurrentUsername(authentication);
            Query query = queryService.assignToTeam(id, team, updatedBy);
            return ResponseEntity.ok(query);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Query> updatePriority(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String priorityLevel = request.get("priorityLevel");
        if (priorityLevel == null) {
            return ResponseEntity.badRequest().build();
        }
        
        String updatedBy = getCurrentUsername(authentication);
        Query query = queryService.updatePriority(id, priorityLevel, updatedBy);
        return ResponseEntity.ok(query);
    }

    @PutMapping("/{id}/category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Query> updateCategory(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        
        String category = request.get("category");
        if (category == null) {
            return ResponseEntity.badRequest().build();
        }
        
        String updatedBy = getCurrentUsername(authentication);
        Query query = queryService.updateCategory(id, category, updatedBy);
        return ResponseEntity.ok(query);
    }

    // Query search and filtering
    @GetMapping("/workflow/{workflowId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesByWorkflow(@PathVariable Long workflowId) {
        List<Query> queries = queryService.findByWorkflowId(workflowId);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/material/{materialCode}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesByMaterial(@PathVariable String materialCode) {
        List<Query> queries = queryService.findByMaterialCode(materialCode);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesByStatus(@PathVariable String status) {
        try {
            QueryStatus queryStatus = QueryStatus.valueOf(status);
            List<Query> queries = queryService.findByStatus(queryStatus);
            List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
            return ResponseEntity.ok(queryDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/team/{team}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesByTeam(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            List<Query> queries = queryService.findByAssignedTeam(queryTeam);
            List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
            return ResponseEntity.ok(queryDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Team-specific query inboxes
    @GetMapping("/inbox/{team}")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getTeamInbox(@PathVariable String team) {
        try {
            logger.info("Loading inbox for team: {}", team);
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            
            List<Query> queries = queryService.findOpenQueriesForTeam(queryTeam);
            logger.debug("Found {} queries for team {}", queries.size(), team);
            List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
            return ResponseEntity.ok(queryDtos);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid team name: {}", team, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Failed to load inbox for team {}: {}", team, e.getMessage(), e);
            throw e; // Re-throw to let the exception handler deal with it
        }
    }

    @GetMapping("/resolved/{team}")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getTeamResolvedQueries(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            List<Query> queries = queryService.findResolvedQueriesForTeam(queryTeam);
            List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
            return ResponseEntity.ok(queryDtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my-raised")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getMyRaisedQueries(Authentication authentication) {
        String username = getCurrentUsername(authentication);
        List<Query> queries = queryService.findMyRaisedQueries(username);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/my-resolved")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER') or hasRole('JVC_USER') or hasRole('ADMIN')")
    public ResponseEntity<List<QuerySummaryDto>> getMyResolvedQueries(Authentication authentication) {
        String username = getCurrentUsername(authentication);
        List<Query> queries = queryService.findMyResolvedQueries(username);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    // Dashboard and reporting endpoints
    @GetMapping("/pending")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getPendingQueries() {
        List<Query> queries = queryService.findPendingQueriesForDashboard();
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/high-priority")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getHighPriorityQueries() {
        List<Query> queries = queryService.findHighPriorityQueries();
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getOverdueQueries() {
        List<Query> queries = queryService.findOverdueQueries();
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/needing-attention")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesNeedingAttention() {
        List<Query> queries = queryService.findQueriesNeedingAttention();
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getRecentQueries(@RequestParam(defaultValue = "7") int days) {
        List<Query> queries = queryService.findRecentQueries(days);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    @GetMapping("/created-between")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> getQueriesCreatedBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        List<Query> queries = queryService.findQueriesCreatedBetween(start, end);
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    // Enhanced search with material context
    @GetMapping("/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<QuerySummaryDto>> searchQueries(
            @RequestParam(required = false) String materialCode,
            @RequestParam(required = false) String projectCode,
            @RequestParam(required = false) String plantCode,
            @RequestParam(required = false) String blockId,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false, defaultValue = "0") int minDaysOpen) {
        
        List<Query> queries = queryService.searchQueriesWithContext(
            materialCode, projectCode, plantCode, blockId, team, status, priority, minDaysOpen
        );
        List<QuerySummaryDto> queryDtos = queryMapper.toSummaryDtoList(queries);
        return ResponseEntity.ok(queryDtos);
    }

    // Statistics endpoints
    @GetMapping("/stats/count-open/{team}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getOpenQueriesCount(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            long count = queryService.countOpenQueriesByTeam(queryTeam);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/count-resolved/{team}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getResolvedQueriesCount(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            long count = queryService.countResolvedQueriesByTeam(queryTeam);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/overdue-count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getOverdueQueriesCount() {
        long count = queryService.countOverdueQueries();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/overdue-count/{team}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getOverdueQueriesCountByTeam(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            long count = queryService.countOverdueQueriesByTeam(queryTeam);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/created-today")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getQueriesCreatedToday() {
        long count = queryService.countQueriesCreatedToday();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/resolved-today")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getQueriesResolvedToday() {
        long count = queryService.countQueriesResolvedToday();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/stats/resolved-today/{team}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getQueriesResolvedTodayByTeam(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            long count = queryService.countQueriesResolvedTodayByTeam(queryTeam);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/stats/avg-resolution-time/{team}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Double> getAverageResolutionTime(@PathVariable String team) {
        try {
            QueryTeam queryTeam = QueryTeam.valueOf(team);
            double avgTime = queryService.getAverageResolutionTimeHours(queryTeam);
            return ResponseEntity.ok(avgTime);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Validation endpoints
    @GetMapping("/{id}/can-resolve")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Boolean> canUserResolveQuery(@PathVariable Long id, Authentication authentication) {
        Optional<Query> queryOpt = queryService.findById(id);
        if (!queryOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        String username = getCurrentUsername(authentication);
        boolean canResolve = queryService.canUserResolveQuery(queryOpt.get(), username);
        return ResponseEntity.ok(canResolve);
    }

    @GetMapping("/{id}/is-overdue")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Boolean> isQueryOverdue(@PathVariable Long id) {
        Optional<Query> queryOpt = queryService.findById(id);
        if (!queryOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        boolean isOverdue = queryService.isQueryOverdue(queryOpt.get());
        return ResponseEntity.ok(isOverdue);
    }

    // Utility method to get current username
    private String getCurrentUsername(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUsername();
        }
        return "SYSTEM";
    }

    // Exception handlers
    @ExceptionHandler(QueryNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleQueryNotFound(QueryNotFoundException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Query not found");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(QueryException.class)
    public ResponseEntity<Map<String, String>> handleQueryException(QueryException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Query error");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new java.util.HashMap<>();
        errorResponse.put("error", "Invalid argument");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}