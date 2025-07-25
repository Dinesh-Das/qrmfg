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
  downloadDocument: (documentId, workflowId = null) =>
    apiRequest(`/documents/${documentId}/download${workflowId ? `?workflowId=${workflowId}` : ''}`, {
      method: 'GET',
      headers: {
        'Accept': 'application/octet-stream'
      }
    }).then(response => response.blob()),

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