import axios from 'axios';

// Create axios instance with base configuration
const apiClient = axios.create({
  baseURL: process.env.REACT_APP_API_BASE_URL || 'http://localhost:8081/qrmfg/api/v1',
  timeout: 30000, // 30 seconds
  headers: {
    'Content-Type': 'application/json'
  }
});

// Request interceptor for authentication and logging
apiClient.interceptors.request.use(
  (config) => {
    // Add authentication token
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Log request in development
    if (process.env.NODE_ENV === 'development') {
      console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`);
    }
    
    return config;
  },
  (error) => {
    console.error('[API] Request interceptor error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for error handling and logging
apiClient.interceptors.response.use(
  (response) => {
    if (process.env.NODE_ENV === 'development') {
      console.log(`[API] Response ${response.status}`);
    }
    return response;
  },
  (error) => {
    console.error('[API] Response error:', {
      url: error.config?.url,
      method: error.config?.method,
      status: error.response?.status,
      message: error.message
    });
    
    // Handle authentication errors
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/qrmfg/login';
    }
    
    return Promise.reject(error);
  }
);

// Simplified API methods
const api = {
  // Workflow API methods
  workflows: {
    getAll: () => apiClient.get('/workflows'),
    getById: (id) => apiClient.get(`/workflows/${id}`),
    create: (data) => apiClient.post('/workflows', data),
    update: (id, data) => apiClient.put(`/workflows/${id}`, data),
    delete: (id) => apiClient.delete(`/workflows/${id}`),
    extend: (id) => apiClient.put(`/workflows/${id}/extend`),
    complete: (id) => apiClient.put(`/workflows/${id}/complete`),
    getPending: () => apiClient.get('/workflows/pending'),
    getByMaterial: (materialCode) => apiClient.get(`/workflows/material/${materialCode}`)
  },
  
  // Query API methods
  queries: {
    getAll: () => apiClient.get('/queries'),
    getById: (id) => apiClient.get(`/queries/${id}`),
    create: (workflowId, data) => apiClient.post(`/workflows/${workflowId}/queries`, data),
    resolve: (id, data) => apiClient.put(`/queries/${id}/resolve`, data),
    getInbox: (team) => apiClient.get(`/queries/inbox?team=${team}`),
    getByWorkflow: (workflowId) => apiClient.get(`/workflows/${workflowId}/queries`)
  },
  
  // Dashboard API methods
  dashboard: {
    getPendingTasks: () => apiClient.get('/dashboard/pending-tasks'),
    getQuerySummary: () => apiClient.get('/dashboard/query-summary'),
    getWorkflowStats: () => apiClient.get('/dashboard/workflow-stats')
  },
  
  // Admin API methods
  admin: {
    getUsers: () => apiClient.get('/admin/users'),
    getRoles: () => apiClient.get('/admin/roles'),
    getScreens: () => apiClient.get('/admin/screens'),
    getAuditLogs: () => apiClient.get('/admin/audit-logs'),
    getSessions: () => apiClient.get('/admin/sessions'),
    getUserRoles: () => apiClient.get('/admin/users/roles'),
    updateUserRole: (userId, roleData) => apiClient.put(`/admin/users/${userId}/role`, roleData)
  }
};

// Simple API wrapper
const createApiWrapper = (apiMethod) => {
  return async (...args) => {
    try {
      const response = await apiMethod(...args);
      return response.data;
    } catch (error) {
      console.error('API Error:', error);
      throw error;
    }
  };
};

// Wrap all API methods
const wrappedApi = {};
Object.keys(api).forEach(category => {
  wrappedApi[category] = {};
  Object.keys(api[category]).forEach(method => {
    wrappedApi[category][method] = createApiWrapper(api[category][method]);
  });
});

export default wrappedApi;
export { apiClient };