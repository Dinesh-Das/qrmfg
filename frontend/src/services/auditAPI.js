import { apiRequest } from '../utils/api';

export const auditAPI = {
  // Workflow audit endpoints
  getWorkflowAuditHistory: (workflowId) => 
    apiRequest(`/api/audit/workflow/${workflowId}`),
  
  getQueryAuditHistory: (queryId) => 
    apiRequest(`/api/audit/query/${queryId}`),
  
  getQuestionnaireResponseAuditHistory: (responseId) => 
    apiRequest(`/api/audit/response/${responseId}`),
  
  getCompleteWorkflowAuditTrail: (workflowId) => 
    apiRequest(`/api/audit/workflow/${workflowId}/complete`),
  
  // Recent audit activity
  getRecentAuditActivity: (days = 7) => 
    apiRequest(`/api/audit/recent?days=${days}`),
  
  getAuditActivityByUser: (username) => 
    apiRequest(`/api/audit/by-user/${encodeURIComponent(username)}`),
  
  getAuditActivityByEntityType: (entityType, days = 7) => 
    apiRequest(`/api/audit/by-entity/${entityType}?days=${days}`),
  
  // Audit search and filtering
  searchAuditLogs: (searchParams) => 
    apiRequest('/api/audit/search', {
      method: 'POST',
      body: JSON.stringify(searchParams)
    }),
  
  // Export audit data
  exportAuditLogs: (workflowId, format = 'csv') => 
    apiRequest(`/api/audit/export/${workflowId}?format=${format}`, {
      method: 'GET',
      headers: {
        'Accept': format === 'csv' ? 'text/csv' : 'application/json'
      }
    })
};