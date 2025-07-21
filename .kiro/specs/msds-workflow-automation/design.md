# Design Document

## Overview

The MSDS Workflow Automation system extends the existing QRMFG portal to provide a comprehensive workflow management solution for Material Safety Data Sheet processing. The system leverages the existing Spring Boot + React architecture with Ant Design components, Oracle database, and JWT authentication to create a state-driven workflow engine with role-based access control.

The design follows a layered architecture pattern with clear separation between presentation, business logic, and data access layers. The workflow engine manages state transitions through a finite state machine pattern, while the query system provides asynchronous communication between teams.

## Architecture

### High-Level Architecture

```mermaid
graph TB
    subgraph "Frontend (React + Ant Design)"
        A[Dashboard Components]
        B[Workflow Forms]
        C[Query Management UI]
        D[Audit Timeline]
    end
    
    subgraph "Backend (Spring Boot)"
        E[REST Controllers]
        F[Workflow Service]
        G[Query Service]
        H[Notification Service]
        I[Security Layer]
    end
    
    subgraph "Data Layer"
        J[Oracle Database]
        K[JPA Repositories]
        L[Audit Tables]
    end
    
    A --> E
    B --> E
    C --> E
    D --> E
    E --> F
    E --> G
    E --> H
    F --> K
    G --> K
    H --> K
    K --> J
    L --> J
    I --> E
```

### Workflow State Machine

```mermaid
stateDiagram-v2
    [*] --> JVC_PENDING
    JVC_PENDING --> PLANT_PENDING : JVC Extension
    PLANT_PENDING --> CQS_PENDING : Query to CQS
    PLANT_PENDING --> TECH_PENDING : Query to Tech
    CQS_PENDING --> PLANT_PENDING : Query Resolved
    TECH_PENDING --> PLANT_PENDING : Query Resolved
    PLANT_PENDING --> COMPLETED : All Steps Complete
    COMPLETED --> [*]
```

## Components and Interfaces

### Backend Components

#### 1. Core Entities

**MaterialWorkflow Entity**
```java
@Entity
@Audited
public class MaterialWorkflow {
    @Id
    @GeneratedValue
    private Long id;
    
    @Column(unique = true)
    private String materialId;
    
    @Enumerated(EnumType.STRING)
    private WorkflowState state;
    
    private String assignedPlant;
    private String initiatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL)
    private List<Query> queries;
    
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL)
    private List<QuestionnaireResponse> responses;
}
```

**Query Entity**
```java
@Entity
@Audited
public class Query {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    private MaterialWorkflow workflow;
    
    private String question;
    private Integer stepNumber;
    private String fieldName;
    
    @Enumerated(EnumType.STRING)
    private QueryTeam assignedTeam; // CQS, TECH
    
    @Enumerated(EnumType.STRING)
    private QueryStatus status; // OPEN, RESOLVED
    
    private String response;
    private String raisedBy;
    private String resolvedBy;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
```

**QuestionnaireResponse Entity**
```java
@Entity
@Audited
public class QuestionnaireResponse {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    private MaterialWorkflow workflow;
    
    private Integer stepNumber;
    private String fieldName;
    private String fieldValue;
    private LocalDateTime lastModified;
    private String modifiedBy;
}
```

#### 2. Service Layer

**WorkflowService**
- Manages workflow state transitions
- Validates state change permissions
- Triggers notifications on state changes
- Provides workflow status queries

**QueryService**
- Handles query creation and resolution
- Manages query assignments to teams
- Tracks query SLA metrics
- Provides query search and filtering

**NotificationService**
- Sends email/Slack notifications
- Manages notification templates
- Tracks notification delivery status
- Provides notification preferences

**QuestionnaireService**
- Manages dynamic form configurations
- Handles response validation
- Provides progress tracking
- Manages draft saves and auto-recovery

#### 3. Controller Layer

**WorkflowController**
```java
@RestController
@RequestMapping("/api/workflows")
@PreAuthorize("hasRole('USER')")
public class WorkflowController {
    
    @PostMapping
    @PreAuthorize("hasRole('JVC_USER')")
    public ResponseEntity<MaterialWorkflow> initiateWorkflow(@RequestBody WorkflowRequest request);
    
    @PutMapping("/{id}/extend")
    @PreAuthorize("hasRole('JVC_USER')")
    public ResponseEntity<MaterialWorkflow> extendToPlant(@PathVariable Long id);
    
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('PLANT_USER')")
    public ResponseEntity<MaterialWorkflow> completeWorkflow(@PathVariable Long id);
    
    @GetMapping("/pending")
    public ResponseEntity<List<WorkflowSummary>> getPendingWorkflows();
}
```

**QueryController**
```java
@RestController
@RequestMapping("/api/queries")
@PreAuthorize("hasRole('USER')")
public class QueryController {
    
    @PostMapping
    @PreAuthorize("hasRole('PLANT_USER')")
    public ResponseEntity<Query> raiseQuery(@RequestBody QueryRequest request);
    
    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('CQS_USER') or hasRole('TECH_USER')")
    public ResponseEntity<Query> resolveQuery(@PathVariable Long id, @RequestBody QueryResponse response);
    
    @GetMapping("/inbox")
    public ResponseEntity<List<Query>> getQueryInbox(@RequestParam String team);
}
```

### Frontend Components

#### 1. Shared Components

**WorkflowDashboard**
- Displays pending tasks panel
- Shows material progress pipeline
- Provides quick action buttons
- Integrates with notification system

**QueryWidget**
- Tabbed interface for different query views
- Real-time query status updates
- Query creation and resolution forms
- Query history and audit trail

#### 2. Role-Specific Components

**JVCWorkflowInitiation**
- Material creation form with validation
- Plant selection dropdown
- Document upload functionality
- Workflow extension actions

**PlantQuestionnaire**
- Dynamic multi-step form renderer
- Progress tracking component
- Query raising modal
- Draft save functionality
- Context panel for JVC data

**QueryInbox**
- Filterable query table
- Query detail modal
- Response editor with rich text
- SLA tracking indicators

**AuditTimeline**
- Chronological event display
- State transition visualization
- User action tracking
- Export functionality

#### 3. UI Component Architecture

```mermaid
graph TD
    A[App.js] --> B[Layout.js]
    B --> C[Navigation.js]
    B --> D[Dashboard Components]
    D --> E[WorkflowDashboard]
    D --> F[QueryWidget]
    D --> G[Role-Specific Views]
    G --> H[JVCView]
    G --> I[PlantView]
    G --> J[CQSView]
    G --> K[TechView]
    G --> L[AdminView]
```

## Data Models

### Database Schema

```sql
-- Workflow Management Tables
CREATE TABLE material_workflows (
    id NUMBER PRIMARY KEY,
    material_id VARCHAR2(50) UNIQUE NOT NULL,
    state VARCHAR2(20) NOT NULL,
    assigned_plant VARCHAR2(100),
    initiated_by VARCHAR2(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE queries (
    id NUMBER PRIMARY KEY,
    workflow_id NUMBER REFERENCES material_workflows(id),
    question CLOB NOT NULL,
    step_number NUMBER,
    field_name VARCHAR2(100),
    assigned_team VARCHAR2(20) NOT NULL,
    status VARCHAR2(20) DEFAULT 'OPEN',
    response CLOB,
    raised_by VARCHAR2(100),
    resolved_by VARCHAR2(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE TABLE questionnaire_responses (
    id NUMBER PRIMARY KEY,
    workflow_id NUMBER REFERENCES material_workflows(id),
    step_number NUMBER NOT NULL,
    field_name VARCHAR2(100) NOT NULL,
    field_value CLOB,
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_by VARCHAR2(100)
);

-- Audit Tables (Hibernate Envers)
CREATE TABLE material_workflows_aud (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    material_id VARCHAR2(50),
    state VARCHAR2(20),
    assigned_plant VARCHAR2(100),
    initiated_by VARCHAR2(100),
    created_at TIMESTAMP,
    last_modified TIMESTAMP
);
```

### API Data Transfer Objects

**WorkflowSummary DTO**
```java
public class WorkflowSummary {
    private Long id;
    private String materialId;
    private WorkflowState currentState;
    private String actionRequiredBy;
    private Integer daysPending;
    private Integer totalQueries;
    private Integer openQueries;
}
```

**QuerySummary DTO**
```java
public class QuerySummary {
    private Long id;
    private String materialId;
    private String plant;
    private Integer stepNumber;
    private String question;
    private Integer daysOpen;
    private QueryTeam assignedTeam;
    private QueryStatus status;
}
```

## Error Handling

### Exception Hierarchy

```java
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidWorkflowStateException extends RuntimeException {
    public InvalidWorkflowStateException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedWorkflowActionException extends RuntimeException {
    public UnauthorizedWorkflowActionException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.NOT_FOUND)
public class WorkflowNotFoundException extends RuntimeException {
    public WorkflowNotFoundException(String materialId) {
        super("Workflow not found for material: " + materialId);
    }
}
```

### Global Exception Handler

```java
@ControllerAdvice
public class WorkflowExceptionHandler {
    
    @ExceptionHandler(InvalidWorkflowStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidWorkflowStateException ex) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("INVALID_STATE", ex.getMessage()));
    }
    
    @ExceptionHandler(UnauthorizedWorkflowActionException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedWorkflowActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("UNAUTHORIZED", ex.getMessage()));
    }
}
```

### Frontend Error Handling

- Global error boundary for React components
- Ant Design notification system for user feedback
- Retry mechanisms for failed API calls
- Offline state detection and queuing

## Testing Strategy

### Backend Testing

**Unit Tests**
- Service layer business logic validation
- State transition logic verification
- Query assignment and resolution logic
- Notification trigger conditions

**Integration Tests**
- REST API endpoint testing
- Database transaction verification
- Security role-based access testing
- Workflow state persistence testing

**Test Configuration**
```java
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class WorkflowServiceIntegrationTest {
    
    @Autowired
    private WorkflowService workflowService;
    
    @Test
    public void testWorkflowStateTransition() {
        // Test implementation
    }
}
```

### Frontend Testing

**Component Tests**
- React component rendering verification
- User interaction simulation
- Form validation testing
- State management verification

**Integration Tests**
- API integration testing
- Authentication flow testing
- Role-based UI rendering
- Notification system testing

**E2E Tests**
- Complete workflow execution
- Multi-user collaboration scenarios
- Query resolution workflows
- Mobile responsiveness testing

### Performance Testing

**Load Testing**
- Concurrent user simulation
- Database query optimization
- API response time measurement
- Memory usage profiling

**Scalability Testing**
- Large dataset handling
- Concurrent workflow processing
- Query system performance
- Notification system throughput

## Security Considerations

### Authentication & Authorization

- JWT token-based authentication (existing)
- Role-based access control with method-level security
- API endpoint protection based on user roles
- Session management and token refresh

### Data Protection

- Sensitive data encryption at rest
- HTTPS enforcement for all communications
- Input validation and sanitization
- SQL injection prevention through JPA

### Audit & Compliance

- Complete audit trail using Hibernate Envers
- User action logging with timestamps
- Data retention policies
- Export capabilities for compliance reporting

## Deployment Architecture

### Environment Configuration

**Development**
- Local Oracle database
- Hot reload for frontend development
- Debug logging enabled
- Mock notification services

**Production**
- Oracle RAC for high availability
- Load balancer for frontend
- Centralized logging with ELK stack
- Email/Slack integration for notifications

### Monitoring & Observability

- Application metrics with Micrometer
- Database performance monitoring
- API response time tracking
- User activity analytics
- Error rate monitoring and alerting