import { apiRequest } from '../utils/api';

export const documentAPI = {
  // Document upload and management
  uploadDocuments: (files, projectCode, materialCode, workflowId) => {
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });
    formData.append('projectCode', projectCode);
    formData.append('materialCode', materialCode);
    if (workflowId) {
      formData.append('workflowId', workflowId);
    }
    
    return apiRequest('/api/documents/upload', {
      method: 'POST',
      body: formData,
      headers: {
        // Don't set Content-Type for FormData
      }
    });
  },
  
  // Document reuse functionality
  getReusableDocuments: (projectCode, materialCode) => 
    apiRequest(`/api/documents/reusable?projectCode=${encodeURIComponent(projectCode)}&materialCode=${encodeURIComponent(materialCode)}`),
  
  reuseDocuments: (documentIds, workflowId) => 
    apiRequest('/api/documents/reuse', {
      method: 'POST',
      body: JSON.stringify({
        documentIds,
        workflowId
      })
    }),
  
  // Document access
  downloadDocument: (documentId) => 
    apiRequest(`/api/documents/${documentId}/download`, {
      method: 'GET',
      headers: {
        'Accept': 'application/octet-stream'
      }
    }).then(response => response.blob()),
  
  getDocumentInfo: (documentId) => 
    apiRequest(`/api/documents/${documentId}`),
  
  getWorkflowDocuments: (workflowId) => 
    apiRequest(`/api/workflows/${workflowId}/documents`),
  
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
  }
};