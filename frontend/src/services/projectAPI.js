import { apiRequest } from '../api/api';

export const projectAPI = {
  // Project dropdown data
  getProjects: () => 
    apiRequest('/projects'),
  
  getMaterialsByProject: (projectCode) => 
    apiRequest(`/projects/${encodeURIComponent(projectCode)}/materials`),
  
  getPlants: () => 
    apiRequest('/projects/plants'),
  
  getBlocksByPlant: (plantCode) => 
    apiRequest(`/projects/plants/${encodeURIComponent(plantCode)}/blocks`),
  
  // Validation endpoints
  validateProjectCode: (projectCode) =>
    apiRequest(`/projects/${encodeURIComponent(projectCode)}/validate`),
  
  validateMaterialCode: (projectCode, materialCode) =>
    apiRequest(`/projects/${encodeURIComponent(projectCode)}/materials/${encodeURIComponent(materialCode)}/validate`),
  
  validatePlantCode: (plantCode) =>
    apiRequest(`/projects/plants/${encodeURIComponent(plantCode)}/validate`),
  
  validateBlockCode: (plantCode, blockCode) =>
    apiRequest(`/projects/plants/${encodeURIComponent(plantCode)}/blocks/${encodeURIComponent(blockCode)}/validate`),
  
  // Search endpoints
  searchProjects: (searchTerm) =>
    apiRequest(`/projects/search?searchTerm=${encodeURIComponent(searchTerm)}`),
  
  searchMaterials: (projectCode, searchTerm) =>
    apiRequest(`/projects/${encodeURIComponent(projectCode)}/materials/search?searchTerm=${encodeURIComponent(searchTerm)}`),
  
  searchPlants: (searchTerm) =>
    apiRequest(`/projects/plants/search?searchTerm=${encodeURIComponent(searchTerm)}`),
  
  searchBlocks: (plantCode, searchTerm) =>
    apiRequest(`/projects/plants/${encodeURIComponent(plantCode)}/blocks/search?searchTerm=${encodeURIComponent(searchTerm)}`),
  
  // Enhanced data endpoints
  getProjectsWithMaterialCount: () =>
    apiRequest('/projects/with-material-count'),
  
  getPlantsWithBlockCount: () =>
    apiRequest('/projects/plants/with-block-count'),
  
  // Questionnaire templates
  getQuestionnaireTemplates: () => 
    apiRequest('/projects/questionnaire/templates'),
  
  getQuestionnaireSteps: () =>
    apiRequest('/projects/questionnaire/steps'),
  
  getQuestionnaireTemplatesByStep: (stepNumber) =>
    apiRequest(`/projects/questionnaire/steps/${stepNumber}`),
  
  getQuestionnaireTemplate: (templateId) => 
    apiRequest(`/projects/questionnaire/questions/${templateId}`)
};