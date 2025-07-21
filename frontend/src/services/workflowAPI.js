import { apiRequest } from '../utils/api';

export const workflowAPI = {
  // Dashboard endpoints
  getDashboardSummary: () => 
    apiRequest('/api/dashboard/summary'),
  
  getOverdueWorkflows: (dayThreshold = 3) => 
    apiRequest(`/api/dashboard/overdue?days=${dayThreshold}`),
  
  getRecentActivity: (days = 7) => 
    apiRequest(`/api/dashboard/recent?days=${days}`),
  
  getWorkflowCountsByState: () => 
    apiRequest('/api/dashboard/counts-by-state'),
  
  getWorkflowsByPlant: (plantName) => 
    apiRequest(`/api/dashboard/by-plant?plant=${encodeURIComponent(plantName)}`),
  
  // Workflow CRUD operations
  createWorkflow: (workflowData) => 
    apiRequest('/api/workflows', {
      method: 'POST',
      body: JSON.stringify(workflowData)
    }),
  
  getWorkflow: (id) => 
    apiRequest(`/api/workflows/${id}`),
  
  updateWorkflow: (id, workflowData) => 
    apiRequest(`/api/workflows/${id}`, {
      method: 'PUT',
      body: JSON.stringify(workflowData)
    }),
  
  deleteWorkflow: (id) => 
    apiRequest(`/api/workflows/${id}`, {
      method: 'DELETE'
    }),
  
  // Workflow state management
  transitionWorkflowState: (id, newState, comment) => 
    apiRequest(`/api/workflows/${id}/transition`, {
      method: 'POST',
      body: JSON.stringify({ newState, comment })
    }),
  
  extendWorkflow: (id, extensionData) => 
    apiRequest(`/api/workflows/${id}/extend`, {
      method: 'POST',
      body: JSON.stringify(extensionData)
    }),
  
  completeWorkflow: (id, completionData) => 
    apiRequest(`/api/workflows/${id}/complete`, {
      method: 'POST',
      body: JSON.stringify(completionData)
    }),
  
  // Workflow search and filtering
  searchWorkflows: (searchParams) => 
    apiRequest('/api/workflows/search', {
      method: 'POST',
      body: JSON.stringify(searchParams)
    }),
  
  getWorkflowsByState: (state) => 
    apiRequest(`/api/workflows/by-state/${state}`),
  
  getWorkflowsByUser: (username) => 
    apiRequest(`/api/workflows/by-user/${encodeURIComponent(username)}`),
  
  // Workflow validation
  validateWorkflow: (id) => 
    apiRequest(`/api/workflows/${id}/validate`),
  
  getWorkflowValidationErrors: (id) => 
    apiRequest(`/api/workflows/${id}/validation-errors`),
  
  // Workflow statistics
  getWorkflowStats: (timeRange) => 
    apiRequest(`/api/workflows/stats?range=${timeRange}`),
  
  getCompletionRateByPlant: () => 
    apiRequest('/api/workflows/completion-rate-by-plant'),
  
  getWorkflowCompletionTrend: (months = 6) => 
    apiRequest(`/api/workflows/completion-trend?months=${months}`),
  
  // Questionnaire and draft management
  saveDraftResponses: (workflowId, draftData) =>
    apiRequest(`/api/workflows/${workflowId}/draft-responses`, {
      method: 'POST',
      body: JSON.stringify(draftData)
    }),
  
  getDraftResponses: (workflowId) =>
    apiRequest(`/api/workflows/${workflowId}/draft-responses`),
  
  submitQuestionnaire: (workflowId, questionnaireData) =>
    apiRequest(`/api/workflows/${workflowId}/submit-questionnaire`, {
      method: 'POST',
      body: JSON.stringify(questionnaireData)
    }),
  
  // Document management
  getWorkflowDocuments: (workflowId) =>
    apiRequest(`/api/workflows/${workflowId}/documents`),
  
  downloadDocument: (documentId) =>
    apiRequest(`/api/workflows/documents/${documentId}/download`, {
      method: 'GET',
      headers: {
        'Accept': 'application/octet-stream'
      }
    }).then(response => response.blob()),
  
  uploadDocument: (workflowId, file, metadata) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('metadata', JSON.stringify(metadata));
    
    return apiRequest(`/api/workflows/${workflowId}/documents`, {
      method: 'POST',
      body: formData,
      headers: {
        // Don't set Content-Type for FormData
      }
    });
  }
};