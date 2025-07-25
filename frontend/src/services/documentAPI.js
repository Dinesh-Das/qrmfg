import { apiRequest } from '../api/api';

export const documentAPI = {
  // Document upload and management
  uploadDocuments: (files, projectCode, materialCode, workflowId) => {
    if (!workflowId) {
      throw new Error('workflowId is required for document upload');
    }
    
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });
    formData.append('projectCode', projectCode);
    formData.append('materialCode', materialCode);
    formData.append('workflowId', workflowId.toString());

    return apiRequest('/documents/upload', {
      method: 'POST',
      body: formData,
      headers: {
        // Don't set Content-Type for FormData
      }
    });
  },

  // Document reuse functionality
  getReusableDocuments: (projectCode, materialCode, enhanced = true) =>
    apiRequest(`/documents/reusable?projectCode=${encodeURIComponent(projectCode)}&materialCode=${encodeURIComponent(materialCode)}&enhanced=${enhanced}`),

  reuseDocuments: (documentIds, workflowId) =>
    apiRequest('/documents/reuse', {
      method: 'POST',
      body: JSON.stringify({
        workflowId,
        documentIds
      })
    }),

  // Document access
  downloadDocument: async (documentId, workflowId = null) => {
    const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '/qrmfg/api/v1';
    const url = `${API_BASE_URL}/documents/${documentId}/download${workflowId ? `?workflowId=${workflowId}` : ''}`;
    
    console.log('Downloading document from URL:', url);
    
    const token = localStorage.getItem('authToken') || localStorage.getItem('token');
    const headers = {
      'Accept': 'application/octet-stream'
    };
    
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
      console.log('Using auth token for download');
    } else {
      console.warn('No auth token found for download');
    }

    try {
      console.log('Making download request...');
      const response = await fetch(url, {
        method: 'GET',
        headers
      });

      console.log('Download response status:', response.status);
      console.log('Download response headers:', Object.fromEntries(response.headers.entries()));

      if (!response.ok) {
        let errorMessage = `HTTP error! status: ${response.status}`;
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorMessage;
        } catch (e) {
          // If response is not JSON, try to get text
          try {
            const errorText = await response.text();
            if (errorText) {
              errorMessage = errorText;
            }
          } catch (e2) {
            // Use default error message
          }
        }
        throw new Error(errorMessage);
      }

      const blob = await response.blob();
      console.log('Downloaded blob size:', blob.size, 'type:', blob.type);
      
      if (blob.size === 0) {
        throw new Error('Downloaded file is empty');
      }
      
      return blob;
    } catch (error) {
      console.error('Document download failed:', error);
      throw error;
    }
  },

  getDocumentInfo: (documentId, enhanced = false) =>
    apiRequest(`/documents/${documentId}?enhanced=${enhanced}`),

  getWorkflowDocuments: (workflowId) =>
    apiRequest(`/documents/workflow/${workflowId}`),

  getDocumentAccessLogs: (documentId) =>
    apiRequest(`/documents/${documentId}/access-logs`),

  getDocumentCount: (workflowId) =>
    apiRequest(`/documents/workflow/${workflowId}/count`),

  // Document validation
  validateFile: (file) => {
    const validTypes = [
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/vnd.ms-excel',
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    ];

    const maxSize = 25 * 1024 * 1024; // 25MB

    return {
      isValidType: validTypes.includes(file.type),
      isValidSize: file.size <= maxSize,
      type: file.type,
      size: file.size
    };
  },

  // Server-side validation
  validateFileOnServer: (file) => {
    const formData = new FormData();
    formData.append('file', file);

    return apiRequest('/documents/validate', {
      method: 'POST',
      body: formData,
      headers: {
        // Don't set Content-Type for FormData
      }
    });
  },

  // Delete document
  deleteDocument: (documentId) =>
    apiRequest(`/documents/${documentId}`, {
      method: 'DELETE'
    })
};