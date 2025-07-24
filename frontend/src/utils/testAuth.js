// Test authentication utilities for development
import { setToken } from '../services/auth';

// For development, we'll use the backend's login endpoint to get a real token
export const loginWithTestUser = async (username = 'admin', password = 'admin') => {
  try {
    const response = await fetch('http://localhost:8081/qrmfg/api/v1/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password }),
    });

    if (response.ok) {
      const data = await response.json();
      if (data.token) {
        setToken(data.token);
        console.log('Test authentication successful:', { username });
        return data.token;
      }
    }
    
    console.warn('Test login failed, using fallback token');
    return createFallbackToken(username);
  } catch (error) {
    console.warn('Test login error, using fallback token:', error.message);
    return createFallbackToken(username);
  }
};

// Fallback: Create a simple test token (may not pass backend validation)
export const createFallbackToken = (username = 'admin') => {
  const header = {
    alg: 'HS512',
    typ: 'JWT'
  };

  const payload = {
    sub: username,
    username: username,
    iat: Math.floor(Date.now() / 1000),
    exp: Math.floor(Date.now() / 1000) + (24 * 60 * 60) // 24 hours
  };

  // Simple base64 encoding (not secure, just for testing)
  const encodedHeader = btoa(JSON.stringify(header));
  const encodedPayload = btoa(JSON.stringify(payload));
  const signature = 'test-signature';

  return `${encodedHeader}.${encodedPayload}.${signature}`;
};

// Set up test authentication
export const setupTestAuth = async (username = 'admin', password = 'admin') => {
  const token = await loginWithTestUser(username, password);
  console.log('Test authentication set up:', { username });
  return token;
};

// Clear test authentication
export const clearTestAuth = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('authToken');
  console.log('Test authentication cleared');
};

// Auto-setup test auth in development
if (process.env.NODE_ENV === 'development' && !localStorage.getItem('token')) {
  setupTestAuth('admin', 'admin').catch(console.error);
}