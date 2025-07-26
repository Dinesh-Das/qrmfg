import { apiRequest } from '../api/api';

export const workflowAPI = {
  // Dashboard endpoints
  getDashboardSummary: () => 
    apiRequest('/dashboard/summary'),
  
  getOverdueWorkflows: (dayThreshold = 3) => 
    apiRequest(`/workflows/overdue?days=${dayThreshold}`),
  
  getRecentActivity: (days = 7) => 
    apiRequest(`/workflows/recent/created?days=${days}`),
  
  getWorkflowCountsByState: () => 
    apiRequest('/dashboard/counts-by-state'),
  
  getWorkflowsByPlant: (plantName) => 
    apiRequest(`/workflows/plant/${encodeURIComponent(plantName)}`),
  
  // Workflow CRUD operations
  createWorkflow: (workflowData) => 
    apiRequest('/workflows', {
      method: 'POST',
      body: JSON.stringify(workflowData)
    }),
  
  getWorkflow: (id) => 
    apiRequest(`/workflows/${id}`),
  
  updateWorkflow: (id, workflowData) => 
    apiRequest(`/workflows/${id}`, {
      method: 'PUT',
      body: JSON.stringify(workflowData)
    }),
  
  deleteWorkflow: (id) => 
    apiRequest(`/workflows/${id}`, {
      method: 'DELETE'
    }),
  
  // Workflow state management
  transitionWorkflowState: (id, newState, comment) => 
    apiRequest(`/workflows/${id}/transition`, {
      method: 'PUT',
      body: JSON.stringify({ newState, comment })
    }),
  
  extendWorkflow: (id, extensionData) => 
    apiRequest(`/workflows/${id}/extend`, {
      method: 'PUT',
      body: JSON.stringify(extensionData)
    }),
  
  completeWorkflow: (id, completionData) => 
    apiRequest(`/workflows/${id}/complete`, {
      method: 'PUT',
      body: JSON.stringify(completionData)
    }),
  
  // Workflow search and filtering
  searchWorkflows: (searchParams) => 
    apiRequest('/workflows/search', {
      method: 'POST',
      body: JSON.stringify(searchParams)
    }),
  
  getWorkflowsByState: (state) => 
    apiRequest(`/workflows/state/${state}`),
  
  getWorkflowsByUser: (username) => 
    apiRequest(`/workflows/initiated-by/${encodeURIComponent(username)}`),
  
  // Workflow validation
  canTransitionTo: (id, newState) =>
    apiRequest(`/workflows/${id}/can-transition/${newState}`),
  
  isReadyForCompletion: (id) =>
    apiRequest(`/workflows/${id}/ready-for-completion`),
  
  checkWorkflowExists: (projectCode, materialCode, plantCode, blockId) =>
    apiRequest(`/workflows/check-exists?projectCode=${encodeURIComponent(projectCode)}&materialCode=${encodeURIComponent(materialCode)}&plantCode=${encodeURIComponent(plantCode)}&blockId=${encodeURIComponent(blockId)}`),
  
  // Workflow statistics
  getWorkflowStats: (timeRange) => 
    apiRequest(`/workflows/stats?range=${timeRange}`),
  
  getCompletionRateByPlant: () => 
    apiRequest('/workflows/completion-rate-by-plant'),
  
  getWorkflowCompletionTrend: (months = 6) => 
    apiRequest(`/workflows/completion-trend?months=${months}`),
  
  // State-based queries
  getPendingWorkflows: () =>
    apiRequest('/workflows/pending'),
  
  getWorkflowsWithOpenQueries: () =>
    apiRequest('/workflows/with-open-queries'),
  
  // Count endpoints
  getCountByState: (state) =>
    apiRequest(`/workflows/stats/count-by-state/${state}`),
  
  getOverdueCount: () =>
    apiRequest('/workflows/stats/overdue-count'),
  
  getWorkflowsWithOpenQueriesCount: () =>
    apiRequest('/workflows/stats/with-open-queries-count'),
  
  // Recent workflows
  getRecentlyCreated: (days = 7) =>
    apiRequest(`/workflows/recent/created?days=${days}`),
  
  getRecentlyCompleted: (days = 7) =>
    apiRequest(`/workflows/recent/completed?days=${days}`),
  
  // Questionnaire and draft management
  saveDraftResponses: (workflowId, draftData) =>
    apiRequest(`/workflows/${workflowId}/draft-responses`, {
      method: 'POST',
      body: JSON.stringify(draftData)
    }),
  
  getDraftResponses: (workflowId) =>
    apiRequest(`/workflows/${workflowId}/draft-responses`),
  
  submitQuestionnaire: (workflowId, questionnaireData) =>
    apiRequest(`/workflows/${workflowId}/submit-questionnaire`, {
      method: 'POST',
      body: JSON.stringify(questionnaireData)
    }),
  
  // Document management
  getWorkflowDocuments: (workflowId) =>
    apiRequest(`/workflows/${workflowId}/documents`),
  
  getReusableDocuments: (projectCode, materialCode) =>
    apiRequest(`/workflows/documents/reusable?projectCode=${encodeURIComponent(projectCode)}&materialCode=${encodeURIComponent(materialCode)}`),
  
  downloadDocument: (documentId) =>
    apiRequest(`/workflows/documents/${documentId}/download`, {
      method: 'GET',
      headers: {
        'Accept': 'application/octet-stream'
      }
    }).then(response => response.blob()),
  
  uploadDocument: (workflowId, file, metadata) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('metadata', JSON.stringify(metadata));
    
    return apiRequest(`/workflows/${workflowId}/documents`, {
      method: 'POST',
      body: formData,
      headers: {
        // Don't set Content-Type for FormData
      }
    });
  },

  // Material name management from ProjectItemMaster
  updateAllMaterialNamesFromProjectItemMaster: () =>
    apiRequest('/workflows/update-material-names', {
      method: 'POST'
    }),

  updateMaterialNameFromProjectItemMaster: (workflowId) =>
    apiRequest(`/workflows/${workflowId}/update-material-name`, {
      method: 'POST'
    }),

  // Plant Questionnaire endpoints
  getQuestionnaireTemplate: ({ materialCode, plantCode, templateType = 'PLANT_QUESTIONNAIRE' }) =>
    apiRequest(`/plant-questionnaire/template?materialCode=${encodeURIComponent(materialCode)}&plantCode=${encodeURIComponent(plantCode)}&templateType=${encodeURIComponent(templateType)}`),

  getCqsData: ({ materialCode, plantCode }) =>
    apiRequest(`/plant-questionnaire/cqs-data?materialCode=${encodeURIComponent(materialCode)}&plantCode=${encodeURIComponent(plantCode)}`),

  getPlantSpecificData: ({ plantCode, materialCode, blockCode }) =>
    apiRequest(`/plant-questionnaire/plant-data?plantCode=${encodeURIComponent(plantCode)}&materialCode=${encodeURIComponent(materialCode)}&blockCode=${encodeURIComponent(blockCode)}`),

  getOrCreatePlantSpecificData: ({ plantCode, materialCode, blockCode, workflowId }) =>
    apiRequest(`/plant-questionnaire/plant-data/init?plantCode=${encodeURIComponent(plantCode)}&materialCode=${encodeURIComponent(materialCode)}&blockCode=${encodeURIComponent(blockCode)}&workflowId=${workflowId}`, {
      method: 'POST'
    }),

  savePlantSpecificData: (plantSpecificData, modifiedBy = 'SYSTEM') =>
    apiRequest(`/plant-questionnaire/plant-data/save?modifiedBy=${encodeURIComponent(modifiedBy)}`, {
      method: 'POST',
      body: JSON.stringify(plantSpecificData)
    }),

  saveDraftPlantResponses: (workflowId, draftData) =>
    apiRequest(`/plant-questionnaire/draft?workflowId=${workflowId}`, {
      method: 'POST',
      body: JSON.stringify(draftData)
    }),

  submitPlantQuestionnaire: (workflowId, submissionData) =>
    apiRequest(`/plant-questionnaire/submit?workflowId=${workflowId}`, {
      method: 'POST',
      body: JSON.stringify(submissionData)
    }),

  getPlantQuestionnaireStats: ({ plantCode, materialCode }) => {
    const params = new URLSearchParams({ plantCode });
    if (materialCode) {
      params.append('materialCode', materialCode);
    }
    return apiRequest(`/plant-questionnaire/stats?${params.toString()}`);
  }
};