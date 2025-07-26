# Clean Naming Strategy for Entities, Controllers & Repositories

## 🎯 **Proposed Clean Names:**

### Entity Classes:
| Current Name | Clean Name | Reason |
|--------------|------------|---------|
| `QRMFGQuestionnaireMaster` | `Question` | Simple, clear |
| `QRMFGQuestionnaireTemplate` | `QuestionTemplate` | Readable |
| `QuestionnaireResponse` | `Answer` | Intuitive |
| `MaterialWorkflow` | `Workflow` | Concise |
| `WorkflowDocument` | `Document` | Simple |
| `NotificationPreference` | `Notification` | Shorter |
| `DocumentAccessLog` | `AccessLog` | Clear |
| `ScreenRoleMapping` | `Permission` | Intuitive |
| `QrmfgBlockMaster` | `Block` | Simple |
| `QrmfgLocationMaster` | `Location` | Clear |
| `QrmfgProjectItemMaster` | `ProjectItem` | Readable |

### Repository Classes:
| Current Name | Clean Name |
|--------------|------------|
| `QRMFGQuestionnaireMasterRepository` | `QuestionRepository` |
| `QRMFGQuestionnaireTemplateRepository` | `QuestionTemplateRepository` |
| `QuestionnaireResponseRepository` | `AnswerRepository` |
| `MaterialWorkflowRepository` | `WorkflowRepository` |
| `WorkflowDocumentRepository` | `DocumentRepository` |
| `NotificationPreferenceRepository` | `NotificationRepository` |
| `DocumentAccessLogRepository` | `AccessLogRepository` |
| `ScreenRoleMappingRepository` | `PermissionRepository` |
| `QrmfgBlockMasterRepository` | `BlockRepository` |
| `QrmfgLocationMasterRepository` | `LocationRepository` |
| `QrmfgProjectItemMasterRepository` | `ProjectItemRepository` |

### Service Classes:
| Current Name | Clean Name |
|--------------|------------|
| `PlantQuestionnaireService` | `PlantQuestionService` |
| `MaterialQuestionnaireService` | `MaterialQuestionService` |
| `QuestionnaireDataLoader` | `QuestionDataLoader` |
| `QuestionnaireTestService` | `QuestionTestService` |
| `QuestionnaireInitializationService` | `QuestionInitService` |

### Controller Classes:
| Current Name | Clean Name |
|--------------|------------|
| `QuestionnaireController` | `QuestionController` |

## 🚀 **Implementation Plan:**

1. **Rename Entity Classes** - Keep table names as they are (already clean)
2. **Rename Repository Interfaces** - Update all references
3. **Rename Service Classes** - Update all references  
4. **Rename Controller Classes** - Update all references
5. **Update all imports and references** throughout the codebase

## ✅ **Benefits:**
- **Much shorter class names** (average 40% reduction)
- **Easier to type and remember**
- **More intuitive for developers**
- **Cleaner code structure**
- **Better IDE autocomplete experience**

Would you like me to implement this clean naming strategy?