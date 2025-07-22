import axios from 'axios';
import { handleApiError } from './errorHandler';
import { withRetry, retryConfigs, withCircuitBreaker } from './retryMechanism';
import { setupOfflineInterceptors, offlineManager } from './offlineHandler';
import { getAuthToken, isTokenExpired, refreshToken } from './auth';

// Create axios instance with base configuration
const apiClient = axios.create({
  baseURL: '/qrmfg/api/v1',
  timeout: 30000, // 30 seconds
  headers: {
    'Content-Type': 'application/json'
  }
});

// Setup offline handling
setupOfflineInterceptors(apiClient);

// Request interceptor for authentication and logging
apiClient.interceptors.request.use(
  async (config) => {
    // Add authentication token
    const token = getAuthToken();
    if (token) {
      // Check if token is expired and refresh if needed
      if (isTokenExpired(token)) {
        try {
          const newToken = await refreshToken();
          config.headers.Authorization = `Bearer ${newToken}`;
        } catch (error) {
          console.error('Token refresh failed:', error);
          // Redirect to login or handle authentication error
          window.location.href = '/login';
          return Promise.reject(error);
        }
      } else {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    
    // Add request ID for tracking
    config.headers['X-Request-ID'] = `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
    
    // Log request in development
    if (process.env.NODE_ENV === 'development') {
      console.log(`[API] ${config.method?.toUpperCase()} ${config.url}`, {
        headers: config.headers,
        data: config.data
      });
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
    // Log response in development
    if (process.env.NODE_ENV === 'development') {
      console.log(`[API] Response ${response.status}:`, response.data);
    }
    
    return response;
  },
  (error) => {
    // Log error
    console.error('[API] Response error:', {
      url: error.config?.url,
      method: error.config?.method,
      status: error.response?.status,
      data: error.response?.data,
      message: error.message
    });
    
    return Promise.reject(error);
  }
);

// API methods with error handling and retry logic
const api = {
  // Workflow API methods
  workflows: {
    getAll: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/workflows'),
        'standard'
      ),
      'workflow'
    ),
    
    getById: withCircuitBreaker(
      withRetry(
        (id) => apiClient.get(`/workflows/${id}`),
        'standard'
      ),
      'workflow'
    ),
    
    create: withCircuitBreaker(
      withRetry(
        (data) => apiClient.post('/workflows', data),
        'critical'
      ),
      'workflow'
    ),
    
    update: withCircuitBreaker(
      withRetry(
        (id, data) => apiClient.put(`/workflows/${id}`, data),
        'critical'
      ),
      'workflow'
    ),
    
    delete: withCircuitBreaker(
      withRetry(
        (id) => apiClient.delete(`/workflows/${id}`),
        'critical'
      ),
      'workflow'
    ),
    
    extend: withCircuitBreaker(
      withRetry(
        (id) => apiClient.put(`/workflows/${id}/extend`),
        'critical'
      ),
      'workflow'
    ),
    
    complete: withCircuitBreaker(
      withRetry(
        (id) => apiClient.put(`/workflows/${id}/complete`),
        'critical'
      ),
      'workflow'
    ),
    
    getPending: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/workflows/pending'),
        'standard'
      ),
      'workflow'
    ),
    
    getByMaterial: withCircuitBreaker(
      withRetry(
        (materialCode) => apiClient.get(`/workflows/material/${materialCode}`),
        'standard'
      ),
      'workflow'
    )
  },
  
  // Query API methods
  queries: {
    getAll: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/queries'),
        'standard'
      ),
      'query'
    ),
    
    getById: withCircuitBreaker(
      withRetry(
        (id) => apiClient.get(`/queries/${id}`),
        'standard'
      ),
      'query'
    ),
    
    create: withCircuitBreaker(
      withRetry(
        (workflowId, data) => apiClient.post(`/workflows/${workflowId}/queries`, data),
        'critical'
      ),
      'query'
    ),
    
    createForMaterial: withCircuitBreaker(
      withRetry(
        (materialCode, data) => apiClient.post(`/materials/${materialCode}/queries`, data),
        'critical'
      ),
      'query'
    ),
    
    resolve: withCircuitBreaker(
      withRetry(
        (id, data) => apiClient.put(`/queries/${id}/resolve`, data),
        'critical'
      ),
      'query'
    ),
    
    getInbox: withCircuitBreaker(
      withRetry(
        (team) => apiClient.get(`/queries/inbox?team=${team}`),
        'standard'
      ),
      'query'
    ),
    
    getByWorkflow: withCircuitBreaker(
      withRetry(
        (workflowId) => apiClient.get(`/workflows/${workflowId}/queries`),
        'standard'
      ),
      'query'
    ),
    
    getOverdue: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/queries/overdue'),
        'standard'
      ),
      'query'
    )
  },
  
  // Dashboard API methods
  dashboard: {
    getPendingTasks: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/dashboard/pending-tasks'),
        'standard'
      ),
      'workflow'
    ),
    
    getQuerySummary: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/dashboard/query-summary'),
        'standard'
      ),
      'query'
    ),
    
    getWorkflowStats: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/dashboard/workflow-stats'),
        'background'
      ),
      'workflow'
    )
  },
  
  // Admin API methods
  admin: {
    getWorkflowMonitoring: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/admin/monitoring/workflows'),
        'standard'
      ),
      'workflow'
    ),
    
    getQuerySlaReport: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/admin/monitoring/query-sla'),
        'standard'
      ),
      'query'
    ),
    
    exportAuditLogs: withCircuitBreaker(
      withRetry(
        (params) => apiClient.get('/admin/audit-logs/export', { params }),
        'background'
      ),
      'workflow'
    ),
    
    getUserRoles: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/admin/users/roles'),
        'standard'
      ),
      'workflow'
    ),
    
    updateUserRole: withCircuitBreaker(
      withRetry(
        (userId, roleData) => apiClient.put(`/admin/users/${userId}/role`, roleData),
        'critical'
      ),
      'workflow'
    )
  },
  
  // Notification API methods
  notifications: {
    getPreferences: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/notifications/preferences'),
        'standard'
      ),
      'notification'
    ),
    
    updatePreferences: withCircuitBreaker(
      withRetry(
        (data) => apiClient.put('/notifications/preferences', data),
        'standard'
      ),
      'notification'
    ),
    
    markAsRead: withCircuitBreaker(
      withRetry(
        (notificationId) => apiClient.put(`/notifications/${notificationId}/read`),
        'quick'
      ),
      'notification'
    ),
    
    getUnread: withCircuitBreaker(
      withRetry(
        () => apiClient.get('/notifications/unread'),
        'quick'
      ),
      'notification'
    )
  }
};

// Enhanced API wrapper with comprehensive error handling
const createApiWrapper = (apiMethod, context = 'api') => {
  return async (...args) => {
    try {
      const response = await apiMethod(...args);
      return response.data;
    } catch (error) {
      // Handle the error using our error handler
      const errorInfo = handleApiError(error, {
        context,
        showNotification: true
      });
      
      // Re-throw the error with additional context
      const enhancedError = new Error(errorInfo.message);
      enhancedError.originalError = error;
      enhancedError.errorInfo = errorInfo;
      
      throw enhancedError;
    }
  };
};

// Wrap all API methods with error handling
const wrappedApi = {};
Object.keys(api).forEach(category => {
  wrappedApi[category] = {};
  Object.keys(api[category]).forEach(method => {
    wrappedApi[category][method] = createApiWrapper(api[category][method], category);
  });
});

// Health check endpoint
wrappedApi.health = createApiWrapper(
  withRetry(() => apiClient.get('/health'), 'quick'),
  'health'
);

// Batch operations
wrappedApi.batch = {
  workflows: async (operations) => {
    const results = [];
    const errors = [];
    
    for (const operation of operations) {
      try {
        const result = await wrappedApi.workflows[operation.method](...operation.args);
        results.push({ success: true, result, operation });
      } catch (error) {
        errors.push({ success: false, error, operation });
      }
    }
    
    return { results, errors };
  }
};

// Export the enhanced API client
export default wrappedApi;

// Export the raw axios instance for advanced usage
export { apiClient };

// Export utility functions
export const getApiStatus = () => ({
  isOnline: offlineManager.isOnline,
  queueLength: offlineManager.requestQueue.length,
  circuitBreakerStates: {
    workflow: require('./retryMechanism').circuitBreakers.workflow.getState(),
    query: require('./retryMechanism').circuitBreakers.query.getState(),
    notification: require('./retryMechanism').circuitBreakers.notification.getState()
  }
});

export const clearApiCache = () => {
  // Clear any cached responses if implemented
  console.log('API cache cleared');
};

export const resetCircuitBreakers = () => {
  const { circuitBreakers } = require('./retryMechanism');
  Object.values(circuitBreakers).forEach(breaker => breaker.reset());
  console.log('Circuit breakers reset');
};