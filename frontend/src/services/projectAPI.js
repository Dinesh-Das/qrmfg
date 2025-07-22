import { apiRequest } from '../utils/api';

export const projectAPI = {
  // Project dropdown data
  getProjects: () => 
    apiRequest('/api/projects'),
  
  getMaterialsByProject: (projectCode) => 
    apiRequest(`/api/projects/${encodeURIComponent(projectCode)}/materials`),
  
  getPlants: () => 
    apiRequest('/api/projects/plants'),
  
  getBlocksByPlant: (plantCode) => 
    apiRequest(`/api/projects/plants/${encodeURIComponent(plantCode)}/blocks`),
  
  // Questionnaire templates
  getQuestionnaireTemplates: () => 
    apiRequest('/api/projects/questionnaire-templates'),
  
  getQuestionnaireTemplate: (templateId) => 
    apiRequest(`/api/projects/questionnaire-templates/${templateId}`)
};