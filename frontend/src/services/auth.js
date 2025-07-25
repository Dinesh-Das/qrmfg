import axios from "axios";

// Configure axios base URL
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '/qrmfg/api/v1';
axios.defaults.baseURL = API_BASE_URL;

export const getToken = () => localStorage.getItem('token');
export const setToken = (token) => localStorage.setItem('token', token);
export const removeToken = () => localStorage.removeItem('token');
export const isAuthenticated = () => !!getToken();

export const getRefreshToken = () => localStorage.getItem('refreshToken');
export const setRefreshToken = (refreshToken) => localStorage.setItem('refreshToken', refreshToken);
export const removeRefreshToken = () => localStorage.removeItem('refreshToken');

export const refreshAccessToken = async () => {
  const token = getToken();
  if (!token) return null;

  try {
    const response = await axios.post('/qrmfg/api/v1/auth/refresh', {}, {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    if (response.data && response.data.token) {
      setToken(response.data.token);
      return response.data.token;
    }
  } catch (err) {
    console.warn('Token refresh failed:', err.message);
    removeToken();
    removeRefreshToken();
    // Redirect to login page
    window.location.href = '/qrmfg/login';
  }
  return null;
};

export const getUserRoles = () => {
  const token = getToken();
  if (!token) return [];
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    // Try common claim names for roles
    return payload.roles || payload.authorities || payload.role || [];
  } catch {
    return [];
  }
};

export const getUserRole = () => {
  const roles = getUserRoles();
  return roles && roles.length > 0 ? roles[0] : null;
};

export const isAdmin = () => {
  const roles = getUserRoles();
  return roles.some(role =>
    role.toLowerCase() === 'admin' ||
    role.toLowerCase() === 'role_admin' ||
    role.toLowerCase().startsWith('admin')
  );
};

export const getCurrentUser = () => {
  const token = getToken();
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub || payload.username || payload.user || 'Unknown User';
  } catch {
    return null;
  }
};

export const getCurrentUserPayload = () => {
  const token = getToken();
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload;
  } catch {
    return null;
  }
};

axios.interceptors.request.use(
  (config) => {
    // Don't add token to login and refresh endpoints
    if (!config.url.includes("/auth/login") && !config.url.includes("/auth/refresh")) {
      const token = getToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Add a response interceptor to handle 401 errors and try to refresh the token
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      // Check if this is a token expiration error
      const errorData = error.response.data;
      if (errorData && (errorData.error === 'JWT token has expired' || errorData.message === 'Please login again')) {
        console.warn('JWT token expired, attempting refresh...');

        // Try to refresh the token
        const newToken = await refreshAccessToken();
        if (newToken) {
          originalRequest.headers["Authorization"] = `Bearer ${newToken}`;
          return axios(originalRequest);
        }
      }
    }

    return Promise.reject(error);
  }
); 