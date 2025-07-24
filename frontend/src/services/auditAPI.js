import { apiRequest } from '../api/api';

export const auditAPI = {
  // Workflow audit endpoints
  getWorkflowAuditHistory: (workflowId) => 
    apiRequest(`/audit/workflow/${workflowId}`),
  
  getQueryAuditHistory: (queryId) => 
    apiRequest(`/audit/query/${queryId}`),
  
  getQuestionnaireResponseAuditHistory: (responseId) => 
    apiRequest(`/audit/response/${responseId}`),
  
  getCompleteWorkflowAuditTrail: (workflowId) => 
    apiRequest(`/audit/workflow/${workflowId}/complete`),
  
  // Recent audit activity
  getRecentAuditActivity: (days = 7) => 
    apiRequest(`/audit/recent?days=${days}`),
  
  getAuditActivityByUser: (username) => 
    apiRequest(`/audit/by-user/${encodeURIComponent(username)}`),
  
  getAuditActivityByEntityType: (entityType, days = 7) => 
    apiRequest(`/audit/by-entity/${entityType}?days=${days}`),
  
  // Audit search and filtering
  searchAuditLogs: (searchParams) => 
    apiRequest('/audit/search', {
      method: 'POST',
      body: JSON.stringify(searchParams)
    }),
  
  // Export audit data
  exportAuditLogs: (workflowId, format = 'csv') => 
    apiRequest(`/audit/export/${workflowId}?format=${format}`, {
      method: 'GET',
      headers: {
        'Accept': format === 'csv' ? 'text/csv' : 'application/json'
      }
    }),
  
  // Read-only workflow view
  getReadOnlyWorkflowView: (workflowId) => 
    apiRequest(`/audit/workflow/${workflowId}/readonly`),
  
  // Version history for questionnaire responses
  getQuestionnaireResponseVersions: (workflowId) => 
    apiRequest(`/audit/workflow/${workflowId}/response-versions`)
};