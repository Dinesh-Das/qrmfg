// Screen access testing utility
import axios from 'axios';
import { isAdmin } from '../services/auth';
import { apiRequest } from '../api/api';

export const testScreenAccess = async () => {
  try {
    const token = localStorage.getItem('token');
    
    if (isAdmin()) {
      console.log('User is admin - has access to all screens');
      return true;
    }

    const allowedScreens = await apiRequest('/admin/screen-role-mapping/my-screens');
    console.log('Allowed screens for current user:', allowedScreens);
    
    const hasWorkflowAccess = allowedScreens.includes('/qrmfg/workflows');
    console.log('Has access to /qrmfg/workflows:', hasWorkflowAccess);
    
    return {
      allowedScreens,
      hasWorkflowAccess,
      isAdmin: false
    };
  } catch (error) {
    console.error('Error checking screen access:', error);
    return {
      error: error.message,
      allowedScreens: [],
      hasWorkflowAccess: false,
      isAdmin: false
    };
  }
};

// Auto-run test in development
if (process.env.NODE_ENV === 'development') {
  setTimeout(() => {
    testScreenAccess().then(result => {
      console.log('Screen access test result:', result);
    });
  }, 1000);
}