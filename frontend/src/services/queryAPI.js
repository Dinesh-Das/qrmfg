import { apiRequest } from '../api/api';

export const queryAPI = {
  // Query CRUD operations
  createQuery: (queryData) =>
    apiRequest('/queries', {
      method: 'POST',
      body: JSON.stringify(queryData)
    }),

  getQuery: (id) =>
    apiRequest(`/queries/${id}`),

  updateQuery: (id, queryData) =>
    apiRequest(`/queries/${id}`, {
      method: 'PUT',
      body: JSON.stringify(queryData)
    }),

  deleteQuery: (id) =>
    apiRequest(`/queries/${id}`, {
      method: 'DELETE'
    }),

  // Query resolution
  resolveQuery: (id, resolutionData) =>
    apiRequest(`/queries/${id}/resolve`, {
      method: 'POST',
      body: JSON.stringify(resolutionData)
    }),

  reopenQuery: (id, reason) =>
    apiRequest(`/queries/${id}/reopen`, {
      method: 'POST',
      body: JSON.stringify({ reason })
    }),

  // Query assignment
  assignQuery: (id, assignmentData) =>
    apiRequest(`/queries/${id}/assign`, {
      method: 'POST',
      body: JSON.stringify(assignmentData)
    }),

  reassignQuery: (id, newTeam, reason) =>
    apiRequest(`/queries/${id}/reassign`, {
      method: 'POST',
      body: JSON.stringify({ newTeam, reason })
    }),

  // Query search and filtering
  getQueriesByWorkflow: (workflowId) =>
    apiRequest(`/queries/by-workflow/${workflowId}`),

  getQueriesByTeam: (team) =>
    apiRequest(`/queries/by-team/${team}`),

  getQueriesByStatus: (status) =>
    apiRequest(`/queries/by-status/${status}`),

  getQueriesByUser: (username) =>
    apiRequest(`/queries/by-user/${encodeURIComponent(username)}`),

  searchQueries: (searchParams) =>
    apiRequest('/queries/search', {
      method: 'POST',
      body: JSON.stringify(searchParams)
    }),

  // Query statistics
  getQueryStats: (timeRange) =>
    apiRequest(`/queries/stats?range=${timeRange}`),

  getQueryCountsByTeam: () =>
    apiRequest('/queries/counts-by-team'),

  getAvgResolutionTimeByTeam: () =>
    apiRequest('/queries/avg-resolution-time-by-team'),

  getOverdueQueries: (dayThreshold = 3) =>
    apiRequest(`/queries/overdue?days=${dayThreshold}`),

  // Query SLA tracking
  getQuerySLAStatus: (id) =>
    apiRequest(`/queries/${id}/sla-status`),

  getQueriesNearingSLA: (hoursThreshold = 24) =>
    apiRequest(`/queries/nearing-sla?hours=${hoursThreshold}`),

  // Query comments/updates
  addQueryComment: (id, comment) =>
    apiRequest(`/queries/${id}/comments`, {
      method: 'POST',
      body: JSON.stringify({ comment })
    }),

  getQueryComments: (id) =>
    apiRequest(`/queries/${id}/comments`),

  // Query priority management
  updateQueryPriority: (id, priority) =>
    apiRequest(`/queries/${id}/priority`, {
      method: 'PUT',
      body: JSON.stringify({ priority })
    }),

  escalateQuery: (id, escalationReason) =>
    apiRequest(`/queries/${id}/escalate`, {
      method: 'POST',
      body: JSON.stringify({ reason: escalationReason })
    })
};