import { apiRequest } from '../api/api';

export const queryAPI = {
  // Query CRUD operations
  createQuery: (workflowId, queryData) =>
    apiRequest(`/queries/workflow/${workflowId}`, {
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
    apiRequest(`/queries/${id}/assign`, {
      method: 'PUT',
      body: JSON.stringify({ team: newTeam, reason })
    }),

  // Query search and filtering
  getQueriesByWorkflow: (workflowId) =>
    apiRequest(`/queries/workflow/${workflowId}`),

  getQueriesByTeam: (team) =>
    apiRequest(`/queries/team/${team}`),

  getQueriesByStatus: (status) =>
    apiRequest(`/queries/status/${status}`),

  getQueriesByUser: (username) =>
    apiRequest(`/queries/my-raised`),

  searchQueries: (searchParams) => {
    const queryString = new URLSearchParams(searchParams).toString();
    return apiRequest(`/queries/search?${queryString}`);
  },

  // Query statistics
  getQueryStats: (timeRange) =>
    apiRequest(`/queries/recent?days=${timeRange}`),

  getQueryCountsByTeam: (team) =>
    apiRequest(`/queries/stats/count-open/${team}`),

  getAvgResolutionTimeByTeam: (team) =>
    apiRequest(`/queries/stats/avg-resolution-time/${team}`),

  getOverdueQueries: (dayThreshold = 3) =>
    apiRequest('/queries/overdue'),

  // Team-specific statistics
  getOverdueQueriesCountByTeam: (team) =>
    apiRequest(`/queries/stats/overdue-count/${team}`),

  getQueriesResolvedTodayByTeam: (team) =>
    apiRequest(`/queries/stats/resolved-today/${team}`),

  // Query SLA tracking
  getQuerySLAStatus: (id) =>
    apiRequest(`/queries/${id}/is-overdue`),

  getQueriesNearingSLA: (hoursThreshold = 24) =>
    apiRequest('/queries/needing-attention'),

  // Query comments/updates (not implemented in backend yet)
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
    apiRequest(`/queries/${id}/priority`, {
      method: 'PUT',
      body: JSON.stringify({ priorityLevel: 'HIGH' })
    })
};