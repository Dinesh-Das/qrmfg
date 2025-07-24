// API utility functions for making HTTP requests

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8081/qrmfg/api/v1';

/**
 * Get authentication token from localStorage
 */
const getAuthToken = () => {
  return localStorage.getItem('authToken') || localStorage.getItem('token');
};

/**
 * Get default headers for API requests
 */
const getDefaultHeaders = () => {
  const headers = {
    'Content-Type': 'application/json',
  };

  const token = getAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
};

/**
 * Handle API response
 */
const handleResponse = async (response) => {
  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    const error = new Error(errorData.message || `HTTP error! status: ${response.status}`);
    error.status = response.status;
    error.data = errorData;
    throw error;
  }

  const contentType = response.headers.get('content-type');
  if (contentType && contentType.includes('application/json')) {
    return await response.json();
  }

  return await response.text();
};

/**
 * Make an API request
 * @param {string} endpoint - API endpoint (relative to base URL)
 * @param {object} options - Fetch options
 * @returns {Promise} - Promise that resolves to response data
 */
export const apiRequest = async (endpoint, options = {}) => {
  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;

  const config = {
    ...options,
    headers: {
      ...getDefaultHeaders(),
      ...options.headers,
    },
  };

  try {
    const response = await fetch(url, config);
    return await handleResponse(response);
  } catch (error) {
    console.error('API request failed:', error);

    // Handle network errors
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error('Network error: Unable to connect to server');
    }

    // Handle authentication errors
    if (error.status === 401) {
      // Clear invalid token
      localStorage.removeItem('authToken');
      localStorage.removeItem('token');

      // Redirect to login if not already there
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/qrmfg/login';
      }
    }

    throw error;
  }
};

/**
 * GET request helper
 */
export const apiGet = (endpoint, params = {}) => {
  const queryString = new URLSearchParams(params).toString();
  const url = queryString ? `${endpoint}?${queryString}` : endpoint;
  return apiRequest(url, { method: 'GET' });
};

/**
 * POST request helper
 */
export const apiPost = (endpoint, data) => {
  return apiRequest(endpoint, {
    method: 'POST',
    body: JSON.stringify(data),
  });
};

/**
 * PUT request helper
 */
export const apiPut = (endpoint, data) => {
  return apiRequest(endpoint, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
};

/**
 * DELETE request helper
 */
export const apiDelete = (endpoint) => {
  return apiRequest(endpoint, { method: 'DELETE' });
};

/**
 * Upload file helper
 */
export const apiUpload = (endpoint, file, additionalData = {}) => {
  const formData = new FormData();
  formData.append('file', file);

  // Add additional form data
  Object.keys(additionalData).forEach(key => {
    formData.append(key, additionalData[key]);
  });

  return apiRequest(endpoint, {
    method: 'POST',
    body: formData,
    headers: {
      // Don't set Content-Type for FormData, let browser set it with boundary
      'Authorization': getAuthToken() ? `Bearer ${getAuthToken()}` : undefined,
    },
  });
};

const apiUtils = {
  apiRequest,
  apiGet,
  apiPost,
  apiPut,
  apiDelete,
  apiUpload,
};

export default apiUtils;