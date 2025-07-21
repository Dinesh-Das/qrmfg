import axios from "axios";

export const getToken = () => localStorage.getItem('token');
export const setToken = (token) => localStorage.setItem('token', token);
export const removeToken = () => localStorage.removeItem('token');
export const isAuthenticated = () => !!getToken();

export const getRefreshToken = () => localStorage.getItem('refreshToken');
export const setRefreshToken = (refreshToken) => localStorage.setItem('refreshToken', refreshToken);
export const removeRefreshToken = () => localStorage.removeItem('refreshToken');

export const refreshAccessToken = async () => {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;
  try {
    const response = await axios.post('/qrmfg/api/v1/auth/refresh', { refreshToken });
    if (response.data && response.data.token) {
      setToken(response.data.token);
      return response.data.token;
    }
  } catch (err) {
    removeToken();
    removeRefreshToken();
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
    return payload;
  } catch {
    return null;
  }
};

axios.interceptors.request.use(
  (config) => {
    if (!config.url.endsWith("/qrmfg/api/v1/auth/login") && !config.url.endsWith("/qrmfg/api/v1/auth/refresh")) {
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
    if (error.response && error.response.status === 401 && !originalRequest._retry && getRefreshToken()) {
      originalRequest._retry = true;
      const newToken = await refreshAccessToken();
      if (newToken) {
        originalRequest.headers["Authorization"] = `Bearer ${newToken}`;
        return axios(originalRequest);
      }
    }
    return Promise.reject(error);
  }
); 