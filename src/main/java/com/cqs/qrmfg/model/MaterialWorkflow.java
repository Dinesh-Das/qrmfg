package com.cqs.qrmfg.model;

// import org.hibernate.envers.Audited;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qrmfg_material_workflows")
// @Audited  // Temporarily disabled to fix constraint issues
public class MaterialWorkflow {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "material_workflow_seq")
    @SequenceGenerator(name = "material_workflow_seq", sequenceName = "MATERIAL_WORKFLOW_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "project_code", nullable = false, length = 50)
    private String projectCode;

    @Column(name = "material_code", nullable = false, length = 50)
    private String materialCode;

    @Column(name = "plant_code", nullable = false, length = 50)
    private String plantCode;

    @Column(name = "block_id", nullable = false, length = 50)
    private String blockId;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_state", nullable = false, length = 20)
    private WorkflowState state = WorkflowState.JVC_PENDING;

    @Column(name = "initiated_by", nullable = false, length = 100)
    private String initiatedBy;

    @Column(name = "material_name", length = 200)
    private String materialName;

    @Column(name = "material_description", length = 1000)
    private String materialDescription;

    @Column(name = "safety_documents_path", length = 500)
    private String safetyDocumentsPath;

    @Column(name = "priority_level", length = 20)
    private String priorityLevel = "NORMAL";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    @Column(name = "extended_at")
    private LocalDateTime extendedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Query> queries = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuestionnaireResponse> responses = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<WorkflowDocument> documents = new ArrayList<>();

    public MaterialWorkflow() {}

    public MaterialWorkflow(String projectCode, String materialCode, String plantCode, String blockId, String initiatedBy) {
        this.projectCode = projectCode;
        this.materialCode = materialCode;
        this.plantCode = plantCode;
        this.blockId = blockId;
        this.initiatedBy = initiatedBy;
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.createdBy = initiatedBy;
        this.updatedBy = initiatedBy;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        lastModified = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastModified = LocalDateTime.now();
    }

    // Business logic methods
    public boolean canTransitionTo(WorkflowState newState) {
        return this.state.canTransitionTo(newState);
    }

    public void transitionTo(WorkflowState newState, String updatedBy) {
        if (!canTransitionTo(newState)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", this.state, newState)
            );
        }
        this.state = newState;
        this.updatedBy = updatedBy;
        this.lastModified = LocalDateTime.now();

        if (newState == WorkflowState.PLANT_PENDING && this.extendedAt == null) {
            this.extendedAt = LocalDateTime.now();
        }
        if (newState == WorkflowState.COMPLETED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public boolean hasOpenQueries() {
        return queries.stream().anyMatch(query -> query.getStatus() == QueryStatus.OPEN);
    }

    public long getOpenQueriesCount() {
        return queries.stream().filter(query -> query.getStatus() == QueryStatus.OPEN).count();
    }

    public long getTotalQueriesCount() {
        return queries.size();
    }

    public int getDaysPending() {
        LocalDateTime referenceTime = LocalDateTime.now();
        LocalDateTime startTime;
        
        switch (state) {
            case JVC_PENDING:
                startTime = createdAt;
                break;
            case PLANT_PENDING:
                startTime = extendedAt != null ? extendedAt : createdAt;
                break;
            case CQS_PENDING:
            case TECH_PENDING:
                // Find the most recent query creation time for current state
                startTime = queries.stream()
                    .filter(q -> q.getAssignedTeam().getCorrespondingWorkflowState() == state)
                    .filter(q -> q.getStatus() == QueryStatus.OPEN)
                    .map(Query::getCreatedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(lastModified);
                break;
            case COMPLETED:
                startTime = null;
                break;
            default:
                startTime = createdAt;
                break;
        }

        if (startTime == null) {
            return 0;
        }

        return (int) java.time.Duration.between(startTime, referenceTime).toDays();
    }

    public boolean isOverdue() {
        return getDaysPending() > 3; // Business rule: overdue after 3 days
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getPlantCode() { return plantCode; }
    public void setPlantCode(String plantCode) { this.plantCode = plantCode; }

    public String getBlockId() { return blockId; }
    public void setBlockId(String blockId) { this.blockId = blockId; }

    public WorkflowState getState() { return state; }
    public void setState(WorkflowState state) { this.state = state; }

    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getMaterialDescription() { return materialDescription; }
    public void setMaterialDescription(String materialDescription) { this.materialDescription = materialDescription; }

    public String getSafetyDocumentsPath() { return safetyDocumentsPath; }
    public void setSafetyDocumentsPath(String safetyDocumentsPath) { this.safetyDocumentsPath = safetyDocumentsPath; }

    public String getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(String priorityLevel) { this.priorityLevel = priorityLevel; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastModified() { return lastModified; }
    public void setLastModified(LocalDateTime lastModified) { this.lastModified = lastModified; }

    public LocalDateTime getExtendedAt() { return extendedAt; }
    public void setExtendedAt(LocalDateTime extendedAt) { this.extendedAt = extendedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public List<Query> getQueries() { return queries; }
    public void setQueries(List<Query> queries) { this.queries = queries; }

    public List<QuestionnaireResponse> getResponses() { return responses; }
    public void setResponses(List<QuestionnaireResponse> responses) { this.responses = responses; }

    public List<WorkflowDocument> getDocuments() { return documents; }
    public void setDocuments(List<WorkflowDocument> documents) { this.documents = documents; }

    public String getAssignedPlant() {
        return plantCode; // Map to new field
    }

    @Override
    public String toString() {
        return String.format("MaterialWorkflow{id=%d, projectCode='%s', materialCode='%s', plantCode='%s', blockId='%s', state=%s}", 
                           id, projectCode, materialCode, plantCode, blockId, state);
    }
}