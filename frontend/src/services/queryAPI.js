import { apiRequest } from '../utils/api';

export const queryAPI = {
  // Query CRUD operations
  createQuery: (queryData) => 
    apiRequest('/api/queries', {
      method: 'POST',
      body: JSON.stringify(queryData)
    }),
  
  getQuery: (id) => 
    apiRequest(`/api/queries/${id}`),
  
  updateQuery: (id, queryData) => 
    apiRequest(`/api/queries/${id}`, {
      method: 'PUT',
      body: JSON.stringify(queryData)
    }),
  
  deleteQuery: (id) => 
    apiRequest(`/api/queries/${id}`, {
      method: 'DELETE'
    }),
  
  // Query resolution
  resolveQuery: (id, resolutionData) => 
    apiRequest(`/api/queries/${id}/resolve`, {
      method: 'POST',
      body: JSON.stringify(resolutionData)
    }),
  
  reopenQuery: (id, reason) => 
    apiRequest(`/api/queries/${id}/reopen`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    }),
  
  // Query assignment
  assignQuery: (id, assignmentData) => 
    apiRequest(`/api/queries/${id}/assign`, {
      method: 'POST',
      body: JSON.stringify(assignmentData)
    }),
  
  reassignQuery: (id, newTeam, reason) => 
    apiRequest(`/api/queries/${id}/reassign`, {
      method: 'POST',
      body: JSON.stringify({ newTeam, reason })
    }),
  
  // Query search and filtering
  getQueriesByWorkflow: (workflowId) => 
    apiRequest(`/api/queries/by-workflow/${workflowId}`),
  
  getQueriesByTeam: (team) => 
    apiRequest(`/api/queries/by-team/${team}`),
  
  getQueriesByStatus: (status) => 
    apiRequest(`/api/queries/by-status/${status}`),
  
  getQueriesByUser: (username) => 
    apiRequest(`/api/queries/by-user/${encodeURIComponent(username)}`),
  
  searchQueries: (searchParams) => 
    apiRequest('/api/queries/search', {
      method: 'POST',
      body: JSON.stringify(searchParams)
    }),
  
  // Query statistics
  getQueryStats: (timeRange) => 
    apiRequest(`/api/queries/stats?range=${timeRange}`),
  
  getQueryCountsByTeam: () => 
    apiRequest('/api/queries/counts-by-team'),
  
  getAvgResolutionTimeByTeam: () => 
    apiRequest('/api/queries/avg-resolution-time-by-team'),
  
  getOverdueQueries: (dayThreshold = 3) => 
    apiRequest(`/api/queries/overdue?days=${dayThreshold}`),
  
  // Query SLA tracking
  getQuerySLAStatus: (id) => 
    apiRequest(`/api/queries/${id}/sla-status`),
  
  getQueriesNearingSLA: (hoursThreshold = 24) => 
    apiRequest(`/api/queries/nearing-sla?hours=${hoursThreshold}`),
  
  // Query comments/updates
  addQueryComment: (id, comment) => 
    apiRequest(`/api/queries/${id}/comments`, {
      method: 'POST',
      body: JSON.stringify({ comment })
    }),
  
  getQueryComments: (id) => 
    apiRequest(`/api/queries/${id}/comments`),
  
  // Query priority management
  updateQueryPriority: (id, priority) => 
    apiRequest(`/api/queries/${id}/priority`, {
      method: 'PUT',
      body: JSON.stringify({ priority })
    }),
  
  escalateQuery: (id, escalationReason) => 
    apiRequest(`/api/queries/${id}/escalate`, {
      method: 'POST',
      body: JSON.stringify({ reason: escalationReason })
    })
};