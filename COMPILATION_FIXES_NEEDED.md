# Compilation Fixes Needed

## Files That Need Import/Reference Updates:

### Service Files:
1. `WorkflowService.java` - Update MaterialWorkflow → Workflow
2. `NotificationService.java` - Update MaterialWorkflow → Workflow  
3. `NotificationServiceImpl.java` - Update MaterialWorkflow → Workflow
4. `DocumentService.java` - Update WorkflowDocument → Document
5. `DocumentServiceImpl.java` - Update WorkflowDocument → Document
6. `WorkflowServiceImpl.java` - Update MaterialWorkflow → Workflow
7. `ProjectServiceImpl.java` - Update QRMFGQuestionnaireMaster → Question
8. `AdminMonitoringServiceImpl.java` - Update MaterialWorkflow → Workflow
9. `WorkflowNotificationIntegrationService.java` - Update MaterialWorkflow → Workflow
10. `NotificationSchedulerService.java` - Update MaterialWorkflow → Workflow
11. `QuestionnaireDataLoader.java` - Update all old class names
12. `MaterialQuestionnaireService.java` - Update all old class names
13. `QuestionnaireTestService.java` - Update all old class names

### Controller Files:
1. `QuestionnaireController.java` - Update all old class names

### Utility Files:
1. `WorkflowMapper.java` - Update MaterialWorkflow → Workflow

### Model Files:
1. `DocumentAccessLog.java` - Already fixed ✅

## Strategy:
1. Update imports first
2. Update method signatures
3. Update variable declarations
4. Update repository references

## Priority Order:
1. Service interfaces first
2. Service implementations
3. Controllers
4. Utilities