# Native SQL Query Updates Required

## Files to Update:

### WorkflowRepository.java
Replace all occurrences of:
- `QRMFG_MATERIAL_WORKFLOWS` → `QRMFG_WORKFLOWS`

### WorkflowDocumentRepository.java  
Replace all occurrences of:
- `QRMFG_WORKFLOW_DOCUMENTS` → `QRMFG_DOCUMENTS`

### DashboardRepository.java
Replace all occurrences of:
- `qrmfg_material_workflows` → `QRMFG_WORKFLOWS`
- `qrmfg_queries` → `QRMFG_QUERIES`
- `qrmfg_workflow_documents` → `QRMFG_DOCUMENTS`

## Strategy:
Since you have `spring.jpa.hibernate.ddl-auto=update`, Hibernate will automatically create the new tables with the readable names when the application starts. The old tables (if they exist) will remain, but new data will go to the new tables.

This is actually the cleanest approach since:
1. No complex migration scripts needed
2. Hibernate handles table creation
3. Fresh start with clean, readable names
4. No risk of data corruption during migration