import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Steps,
  Form,
  Input,
  Select,
  Radio,
  Checkbox,
  Button,
  Row,
  Col,
  Progress,
  message,
  Spin,
  Alert,
  Divider,
  Space,
  Tooltip,
  Badge,
  notification,
  Typography,
  Tag,
  Modal
} from 'antd';
import {
  SaveOutlined,
  QuestionCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined,
  CloudSyncOutlined,
  WifiOutlined,
  DisconnectOutlined
} from '@ant-design/icons';
import { workflowAPI } from '../services/workflowAPI';
import { queryAPI } from '../services/queryAPI';
import QueryRaisingModal from './QueryRaisingModal';
import MaterialContextPanel from './MaterialContextPanel';

const { Step } = Steps;
const { TextArea } = Input;
const { Option } = Select;
const { Text } = Typography;

// Hook to detect screen size
const useResponsive = () => {
  const [screenSize, setScreenSize] = useState({
    isMobile: window.innerWidth <= 768,
    isTablet: window.innerWidth > 768 && window.innerWidth <= 1024,
    isDesktop: window.innerWidth > 1024
  });

  useEffect(() => {
    const handleResize = () => {
      setScreenSize({
        isMobile: window.innerWidth <= 768,
        isTablet: window.innerWidth > 768 && window.innerWidth <= 1024,
        isDesktop: window.innerWidth > 1024
      });
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return screenSize;
};

const PlantQuestionnaire = ({ workflowId, onComplete, onSaveDraft }) => {
  const [form] = Form.useForm();
  const [currentStep, setCurrentStep] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [workflowData, setWorkflowData] = useState(null);
  const [formData, setFormData] = useState({});
  const [completedSteps, setCompletedSteps] = useState(new Set());
  const [queryModalVisible, setQueryModalVisible] = useState(false);
  const [selectedField, setSelectedField] = useState(null);
  const [queries, setQueries] = useState([]);
  const [autoSaveEnabled, setAutoSaveEnabled] = useState(true);
  const [isOffline, setIsOffline] = useState(!navigator.onLine);
  const [pendingChanges, setPendingChanges] = useState(false);
  const { isMobile, isTablet } = useResponsive();

  // Define questionnaire steps (loaded from backend template)
  const [questionnaireSteps, setQuestionnaireSteps] = useState([]);
  const [templateLoading, setTemplateLoading] = useState(true);
  const [cqsData, setCqsData] = useState({});
  const [plantSpecificData, setPlantSpecificData] = useState({});

  // Load questionnaire template from backend
  const loadQuestionnaireTemplate = async () => {
    try {
      setTemplateLoading(true);
      const template = await workflowAPI.getQuestionnaireTemplate({
        materialCode: workflowData?.materialCode,
        plantCode: workflowData?.assignedPlant,
        templateType: 'PLANT_QUESTIONNAIRE'
      });
      
      // Process template to include CQS auto-populated fields
      const processedSteps = template.steps.map(step => ({
        ...step,
        fields: step.fields.map(field => ({
          ...field,
          isCqsAutoPopulated: field.cqsAutoPopulated || false,
          cqsValue: field.cqsAutoPopulated ? 'Pending IMP' : null,
          disabled: field.cqsAutoPopulated || field.disabled || false,
          placeholder: field.cqsAutoPopulated ? 
            'Auto-populated by CQS (Pending Implementation)' : 
            field.placeholder
        }))
      }));
      
      setQuestionnaireSteps(processedSteps);
      
      // Load CQS data if available
      await loadCqsData();
      
      // Load plant-specific data
      await loadPlantSpecificData();
      
    } catch (error) {
      console.error('Failed to load questionnaire template:', error);
      message.error('Failed to load questionnaire template');
      // Fallback to default template if backend fails
      setQuestionnaireSteps(getDefaultTemplate());
    } finally {
      setTemplateLoading(false);
    }
  };

  // Load CQS auto-populated data
  const loadCqsData = async () => {
    try {
      const cqsResponse = await workflowAPI.getCqsData({
        materialCode: workflowData?.materialCode,
        plantCode: workflowData?.assignedPlant
      });
      
      setCqsData(cqsResponse.data || {});
      
      // Update form with CQS data
      const cqsFormData = {};
      Object.entries(cqsResponse.data || {}).forEach(([key, value]) => {
        if (value && value !== 'Pending IMP') {
          cqsFormData[key] = value;
        }
      });
      
      if (Object.keys(cqsFormData).length > 0) {
        setFormData(prev => ({ ...prev, ...cqsFormData }));
        form.setFieldsValue(cqsFormData);
      }
      
    } catch (error) {
      console.error('Failed to load CQS data:', error);
      // Don't show error message as CQS data might not be available yet
    }
  };

  // Load plant-specific data table
  const loadPlantSpecificData = async () => {
    try {
      const plantData = await workflowAPI.getOrCreatePlantSpecificData({
        plantCode: workflowData?.assignedPlant,
        materialCode: workflowData?.materialCode,
        blockCode: workflowData?.blockCode,
        workflowId: workflowId
      });
      
      setPlantSpecificData(plantData || {});
      
      // If plant data exists, populate form with existing plant inputs
      if (plantData?.plantInputs) {
        setFormData(prev => ({ ...prev, ...plantData.plantInputs }));
        form.setFieldsValue(plantData.plantInputs);
      }
      
    } catch (error) {
      console.error('Failed to load plant-specific data:', error);
    }
  };

  // Save plant-specific data with composite key
  const savePlantSpecificData = async (data) => {
    try {
      const plantSpecificPayload = {
        plantCode: workflowData?.assignedPlant,
        materialCode: workflowData?.materialCode,
        blockCode: workflowData?.blockCode,
        workflowId: workflowId,
        cqsInputs: cqsData,
        plantInputs: data,
        totalFields: Object.keys(data).length,
        completedFields: Object.values(data).filter(value => 
          value !== null && value !== undefined && value !== ''
        ).length
      };
      
      await workflowAPI.savePlantSpecificData(plantSpecificPayload, 'current_user');
      
    } catch (error) {
      console.error('Failed to save plant-specific data:', error);
      throw error;
    }
  };

  // Default template fallback
  const getDefaultTemplate = () => [
    {
      title: 'Basic Information',
      description: 'Material identification and basic properties',
      fields: [
        {
          name: 'materialName',
          label: 'Material Name',
          type: 'input',
          required: true,
          disabled: true,
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          placeholder: 'Auto-populated by CQS (Pending Implementation)'
        },
        {
          name: 'materialType',
          label: 'Material Type',
          type: 'select',
          required: true,
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          options: [
            { value: 'chemical', label: 'Chemical' },
            { value: 'mixture', label: 'Mixture' },
            { value: 'substance', label: 'Substance' },
            { value: 'preparation', label: 'Preparation' }
          ]
        },
        {
          name: 'casNumber',
          label: 'CAS Number',
          type: 'input',
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          placeholder: 'Auto-populated by CQS (Pending Implementation)'
        },
        {
          name: 'supplierName',
          label: 'Supplier Name',
          type: 'input',
          required: true,
          placeholder: 'Enter supplier company name'
        }
      ]
    },
    {
      title: 'Physical Properties',
      description: 'Physical characteristics and appearance',
      fields: [
        {
          name: 'physicalState',
          label: 'Physical State',
          type: 'radio',
          required: true,
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          options: [
            { value: 'solid', label: 'Solid' },
            { value: 'liquid', label: 'Liquid' },
            { value: 'gas', label: 'Gas' },
            { value: 'vapor', label: 'Vapor' }
          ]
        },
        {
          name: 'color',
          label: 'Color',
          type: 'input',
          placeholder: 'Describe the color/appearance'
        },
        {
          name: 'odor',
          label: 'Odor',
          type: 'input',
          placeholder: 'Describe the odor characteristics'
        },
        {
          name: 'boilingPoint',
          label: 'Boiling Point (°C)',
          type: 'input',
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          placeholder: 'Auto-populated by CQS (Pending Implementation)'
        },
        {
          name: 'meltingPoint',
          label: 'Melting Point (°C)',
          type: 'input',
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          placeholder: 'Auto-populated by CQS (Pending Implementation)'
        }
      ]
    },
    {
      title: 'Hazard Classification',
      description: 'Safety hazards and classifications',
      fields: [
        {
          name: 'hazardCategories',
          label: 'Hazard Categories',
          type: 'checkbox',
          required: true,
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          options: [
            { value: 'flammable', label: 'Flammable' },
            { value: 'toxic', label: 'Toxic' },
            { value: 'corrosive', label: 'Corrosive' },
            { value: 'irritant', label: 'Irritant' },
            { value: 'oxidizing', label: 'Oxidizing' },
            { value: 'explosive', label: 'Explosive' }
          ]
        },
        {
          name: 'signalWord',
          label: 'Signal Word',
          type: 'radio',
          required: true,
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          options: [
            { value: 'danger', label: 'DANGER' },
            { value: 'warning', label: 'WARNING' },
            { value: 'none', label: 'None' }
          ]
        },
        {
          name: 'hazardStatements',
          label: 'Hazard Statements',
          type: 'textarea',
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          placeholder: 'Auto-populated by CQS (Pending Implementation)'
        }
      ]
    },
    {
      title: 'Safety Measures',
      description: 'Precautionary and safety information',
      fields: [
        {
          name: 'precautionaryStatements',
          label: 'Precautionary Statements',
          type: 'textarea',
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          placeholder: 'Auto-populated by CQS (Pending Implementation)'
        },
        {
          name: 'personalProtection',
          label: 'Personal Protection Equipment',
          type: 'checkbox',
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          options: [
            { value: 'gloves', label: 'Protective Gloves' },
            { value: 'eyewear', label: 'Eye Protection' },
            { value: 'respiratory', label: 'Respiratory Protection' },
            { value: 'clothing', label: 'Protective Clothing' }
          ]
        },
        {
          name: 'firstAidMeasures',
          label: 'First Aid Measures',
          type: 'textarea',
          required: true,
          placeholder: 'Describe first aid procedures for different exposure routes'
        }
      ]
    },
    {
      title: 'Storage & Handling',
      description: 'Storage conditions and handling procedures',
      fields: [
        {
          name: 'storageConditions',
          label: 'Storage Conditions',
          type: 'textarea',
          required: true,
          placeholder: 'Describe proper storage conditions (temperature, humidity, etc.)'
        },
        {
          name: 'incompatibleMaterials',
          label: 'Incompatible Materials',
          type: 'textarea',
          placeholder: 'List materials to avoid contact with'
        },
        {
          name: 'handlingPrecautions',
          label: 'Handling Precautions',
          type: 'textarea',
          required: true,
          placeholder: 'Describe safe handling procedures'
        }
      ]
    },
    {
      title: 'Environmental Impact',
      description: 'Environmental and disposal information',
      fields: [
        {
          name: 'environmentalHazards',
          label: 'Environmental Hazards',
          type: 'checkbox',
          isCqsAutoPopulated: true,
          cqsValue: 'Pending IMP',
          disabled: true,
          options: [
            { value: 'aquatic_acute', label: 'Acute Aquatic Toxicity' },
            { value: 'aquatic_chronic', label: 'Chronic Aquatic Toxicity' },
            { value: 'ozone_depleting', label: 'Ozone Depleting' },
            { value: 'bioaccumulative', label: 'Bioaccumulative' }
          ]
        },
        {
          name: 'disposalMethods',
          label: 'Disposal Methods',
          type: 'textarea',
          required: true,
          placeholder: 'Describe proper disposal procedures and restrictions'
        },
        {
          name: 'spillCleanup',
          label: 'Spill Cleanup Procedures',
          type: 'textarea',
          required: true,
          placeholder: 'Describe procedures for cleaning up spills'
        }
      ]
    }
  ];

  // Function definitions (moved here to avoid hoisting issues)
  const handleNext = useCallback(() => {
    if (currentStep < questionnaireSteps.length - 1) {
      setCurrentStep(currentStep + 1);
    }
  }, [currentStep, questionnaireSteps.length]);

  const handlePrevious = useCallback(() => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  }, [currentStep]);

  const handleStepChange = useCallback((step) => {
    if (step >= 0 && step < questionnaireSteps.length) {
      setCurrentStep(step);
    }
  }, [questionnaireSteps.length]);

  // Helper function definitions
  const getStepForField = (fieldName) => {
    for (let i = 0; i < questionnaireSteps.length; i++) {
      if (questionnaireSteps[i].fields.some(field => field.name === fieldName)) {
        return i;
      }
    }
    return 0;
  };

  const getOverallCompletionPercentage = () => {
    let totalRequired = 0;
    let completedRequired = 0;

    questionnaireSteps.forEach((step, index) => {
      const stepFields = step.fields;
      const requiredFields = stepFields.filter(field => field.required);
      const completedRequiredFields = requiredFields.filter(field => {
        const value = formData[field.name];
        if (Array.isArray(value)) {
          return value.length > 0;
        }
        return value && value !== '' && value !== null && value !== undefined;
      });

      totalRequired += requiredFields.length;
      completedRequired += completedRequiredFields.length;
    });

    return totalRequired > 0 ? Math.round((completedRequired / totalRequired) * 100) : 0;
  };

  const handleSaveDraft = useCallback(async (silent = false) => {
    try {
      setSaving(true);
      const currentValues = form.getFieldsValue();
      const updatedFormData = { ...formData, ...currentValues };

      // Enhanced validation before saving
      const validatedFormData = {};
      Object.entries(updatedFormData).forEach(([key, value]) => {
        if (value !== null && value !== undefined && value !== '') {
          validatedFormData[key] = value;
        }
      });

      // Save to local storage as backup with enhanced metadata
      const draftKey = `plant_questionnaire_draft_${workflowId}`;
      const draftData = {
        formData: validatedFormData,
        currentStep,
        timestamp: Date.now(),
        completedSteps: Array.from(completedSteps),
        version: '2.0',
        materialCode: workflowData?.materialCode,
        materialName: workflowData?.materialName,
        assignedPlant: workflowData?.assignedPlant,
        lastSyncAttempt: Date.now(),
        syncStatus: isOffline ? 'pending' : 'synced',
        totalFields: Object.keys(validatedFormData).length,
        completionPercentage: getOverallCompletionPercentage(),
        sessionId: Date.now()
      };

      try {
        localStorage.setItem(draftKey, JSON.stringify(draftData));
      } catch (localStorageError) {
        console.warn('Failed to save draft to local storage:', localStorageError);
      }

      // Save to server if online
      if (!isOffline) {
        try {
          const draftData = {
            plantCode: workflowData?.assignedPlant,
            materialCode: workflowData?.materialCode,
            blockCode: workflowData?.blockCode,
            responses: updatedFormData,
            currentStep,
            completedSteps: Array.from(completedSteps),
            modifiedBy: 'current_user'
          };

          await workflowAPI.saveDraftPlantResponses(workflowId, draftData);

          if (!silent) {
            message.success('Draft saved successfully');
          }
        } catch (serverError) {
          console.error('Failed to save draft to server:', serverError);
          setPendingChanges(true);

          if (!silent) {
            message.warning('Draft saved locally. Will sync when connection is restored.');
          }
        }
      } else {
        setPendingChanges(true);
        if (!silent) {
          message.info('Draft saved locally. Will sync when online.');
        }
      }

      setFormData(updatedFormData);

      if (onSaveDraft) {
        onSaveDraft(updatedFormData);
      }
    } catch (error) {
      console.error('Failed to save draft:', error);
      if (!silent) {
        message.error('Failed to save draft. Please try again.');
      }
    } finally {
      setSaving(false);
    }
  }, [form, formData, workflowId, onSaveDraft, currentStep, completedSteps, isOffline, workflowData]);

  // Network status monitoring with enhanced offline handling
  useEffect(() => {
    const handleOnline = () => {
      setIsOffline(false);
      notification.success({
        message: 'Connection Restored',
        description: 'You are back online. Syncing your changes...',
        icon: <WifiOutlined style={{ color: '#52c41a' }} />,
        duration: 3
      });

      if (pendingChanges) {
        handleSaveDraft(true); // Auto-sync when back online
        setPendingChanges(false);
      }
    };

    const handleOffline = () => {
      setIsOffline(true);
      notification.warning({
        message: 'Connection Lost',
        description: 'You are offline. Changes will be saved locally and synced when connection is restored.',
        icon: <DisconnectOutlined style={{ color: '#fa8c16' }} />,
        duration: 5
      });
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [pendingChanges, handleSaveDraft]);

  // Enhanced keyboard navigation
  useEffect(() => {
    const handleKeyDown = (event) => {
      // Ctrl/Cmd + S to save draft
      if ((event.ctrlKey || event.metaKey) && event.key === 's') {
        event.preventDefault();
        handleSaveDraft();
      }

      // Ctrl/Cmd + Right Arrow to go to next step
      if ((event.ctrlKey || event.metaKey) && event.key === 'ArrowRight') {
        event.preventDefault();
        if (currentStep < questionnaireSteps.length - 1) {
          handleNext();
        }
      }

      // Ctrl/Cmd + Left Arrow to go to previous step
      if ((event.ctrlKey || event.metaKey) && event.key === 'ArrowLeft') {
        event.preventDefault();
        if (currentStep > 0) {
          handlePrevious();
        }
      }

      // F1 to show help/shortcuts
      if (event.key === 'F1') {
        event.preventDefault();
        Modal.info({
          title: 'Keyboard Shortcuts',
          content: (
            <div>
              <p><strong>Ctrl/Cmd + S:</strong> Save draft</p>
              <p><strong>Ctrl/Cmd + →:</strong> Next step</p>
              <p><strong>Ctrl/Cmd + ←:</strong> Previous step</p>
              <p><strong>Tab:</strong> Navigate between fields</p>
              <p><strong>Enter:</strong> Submit form or proceed</p>
              <p><strong>Esc:</strong> Close modals</p>
            </div>
          )
        });
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [currentStep, questionnaireSteps.length, handleNext, handlePrevious]);



  // Load workflow data and existing responses
  useEffect(() => {
    if (workflowId) {
      loadWorkflowData();
      loadQueries();
    }
  }, [workflowId]);

  // Auto-save functionality with recovery
  useEffect(() => {
    if (autoSaveEnabled && Object.keys(formData).length > 0) {
      const autoSaveTimer = setTimeout(() => {
        handleSaveDraft(true); // Silent save
      }, 30000); // Auto-save every 30 seconds

      return () => clearTimeout(autoSaveTimer);
    }
  }, [formData, autoSaveEnabled, handleSaveDraft]);

  // Enhanced form validation with field-specific rules
  const getFieldValidationRules = (field) => {
    const rules = [];

    if (field.required) {
      rules.push({
        required: true,
        message: `${field.label} is required for MSDS completion`
      });
    }

    // Add specific validation based on field type and name
    switch (field.name) {
      case 'casNumber':
        rules.push({
          pattern: /^\d{1,7}-\d{2}-\d$/,
          message: 'Please enter a valid CAS number format (e.g., 64-17-5). If unknown, raise a query to the Technical team.'
        });
        break;
      case 'boilingPoint':
      case 'meltingPoint':
        rules.push({
          pattern: /^-?\d+(\.\d+)?$/,
          message: 'Please enter a valid temperature in Celsius (e.g., 100.5 or -10)'
        });
        break;
      case 'materialName':
        rules.push({
          min: 2,
          message: 'Material name must be at least 2 characters'
        });
        rules.push({
          max: 200,
          message: 'Material name cannot exceed 200 characters'
        });
        break;
      case 'supplierName':
        rules.push({
          min: 2,
          message: 'Supplier name must be at least 2 characters'
        });
        rules.push({
          max: 100,
          message: 'Supplier name cannot exceed 100 characters'
        });
        break;
      case 'firstAidMeasures':
      case 'storageConditions':
      case 'handlingPrecautions':
      case 'disposalMethods':
      case 'spillCleanup':
        rules.push({
          min: 10,
          message: `${field.label} must be at least 10 characters for regulatory compliance`
        });
        rules.push({
          max: 2000,
          message: `${field.label} cannot exceed 2000 characters`
        });
        break;
      case 'hazardStatements':
      case 'precautionaryStatements':
        rules.push({
          pattern: /^[HP]\d{3}/,
          message: 'Please use standard H-codes (e.g., H225) or P-codes (e.g., P210). Separate multiple codes with commas.'
        });
        break;
      default:
        break;
    }

    return rules;
  };

  // Get contextual help text for fields
  const getFieldHelpText = (field) => {
    const helpTexts = {
      'casNumber': 'Chemical Abstracts Service number - unique identifier for chemical substances',
      'materialType': 'Select the most appropriate category based on the material composition',
      'physicalState': 'Physical state at room temperature (20°C)',
      'hazardCategories': 'Select all applicable hazard classifications according to GHS',
      'signalWord': 'GHS signal word based on the most severe hazard category',
      'personalProtection': 'Required PPE based on hazard assessment',
      'environmentalHazards': 'Environmental impact classifications according to GHS',
      'boilingPoint': 'Temperature at which the material changes from liquid to gas at standard pressure',
      'meltingPoint': 'Temperature at which the material changes from solid to liquid'
    };

    return helpTexts[field.name] || field.help;
  };

  // Enhanced auto-recovery on component mount with improved error handling
  useEffect(() => {
    const recoverDraftData = () => {
      try {
        const draftKey = `plant_questionnaire_draft_${workflowId}`;
        const savedDraft = localStorage.getItem(draftKey);

        if (savedDraft) {
          const draftData = JSON.parse(savedDraft);
          const draftTimestamp = draftData.timestamp;
          const currentTime = Date.now();

          // Only recover if draft is less than 7 days old (extended from 24 hours)
          if (currentTime - draftTimestamp < 7 * 24 * 60 * 60 * 1000) {
            // Enhanced validation of draft data integrity
            if (draftData.formData && typeof draftData.formData === 'object') {
              // Validate each field value before setting
              const validatedFormData = {};
              Object.entries(draftData.formData).forEach(([key, value]) => {
                if (value !== null && value !== undefined && value !== '') {
                  validatedFormData[key] = value;
                }
              });

              setFormData(prev => ({ ...prev, ...validatedFormData }));
              form.setFieldsValue(validatedFormData);

              if (typeof draftData.currentStep === 'number' &&
                draftData.currentStep >= 0 &&
                draftData.currentStep < questionnaireSteps.length) {
                setCurrentStep(draftData.currentStep);
              }

              if (Array.isArray(draftData.completedSteps)) {
                setCompletedSteps(new Set(draftData.completedSteps));
              }

              // Check if there are pending changes to sync
              if (draftData.syncStatus === 'pending') {
                setPendingChanges(true);
              }

              const recoveredFields = Object.keys(validatedFormData).length;
              const draftAge = Math.round((currentTime - draftTimestamp) / (1000 * 60 * 60));

              notification.success({
                message: 'Draft Recovered',
                description: `${recoveredFields} fields restored from ${draftAge} hours ago. Your progress has been preserved.`,
                duration: 6,
                placement: 'topRight'
              });
            } else {
              // Remove corrupted draft
              localStorage.removeItem(draftKey);
              notification.warning({
                message: 'Draft Recovery Failed',
                description: 'Previous draft data was corrupted and has been cleared.',
                duration: 4
              });
            }
          } else {
            // Remove old draft
            localStorage.removeItem(draftKey);
            const draftAge = Math.round((currentTime - draftTimestamp) / (1000 * 60 * 60 * 24));
            notification.info({
              message: 'Old Draft Cleared',
              description: `Draft from ${draftAge} days ago was automatically removed.`,
              duration: 3
            });
          }
        }
      } catch (error) {
        console.error('Failed to recover draft data:', error);
        // Remove corrupted draft
        try {
          localStorage.removeItem(`plant_questionnaire_draft_${workflowId}`);
          notification.error({
            message: 'Draft Recovery Error',
            description: 'Failed to recover previous draft. Starting fresh.',
            duration: 4
          });
        } catch (removeError) {
          console.error('Failed to remove corrupted draft:', removeError);
        }
      }
    };

    if (workflowId && !workflowData) {
      recoverDraftData();
    }
  }, [workflowId, workflowData, form, questionnaireSteps.length]);

  const loadWorkflowData = async () => {
    try {
      setLoading(true);
      const workflow = await workflowAPI.getWorkflow(workflowId);
      setWorkflowData(workflow);

      // Pre-populate material name from workflow data (from ProjectItemMaster)
      const initialData = {};
      if (workflow.materialName) {
        initialData.materialName = workflow.materialName;
      }

      // Load existing responses if any
      if (workflow.responses && workflow.responses.length > 0) {
        const existingData = { ...initialData };
        const completed = new Set();

        workflow.responses.forEach(response => {
          existingData[response.fieldName] = response.fieldValue;
          completed.add(response.stepNumber);
        });

        setFormData(existingData);
        setCompletedSteps(completed);
        form.setFieldsValue(existingData);
      } else {
        // Set initial data even if no responses exist
        setFormData(initialData);
        form.setFieldsValue(initialData);
      }
    } catch (error) {
      console.error('Failed to load workflow data:', error);
      message.error('Failed to load workflow data');
    } finally {
      setLoading(false);
    }
  };

  const loadQueries = async () => {
    try {
      const workflowQueries = await queryAPI.getQueriesByWorkflow(workflowId);
      setQueries(workflowQueries);
    } catch (error) {
      console.error('Failed to load queries:', error);
    }
  };



  // Enhanced step completion tracking with validation
  const getStepCompletionStatus = (stepIndex) => {
    const stepFields = questionnaireSteps[stepIndex].fields;
    const requiredFields = stepFields.filter(field => field.required);
    const optionalFields = stepFields.filter(field => !field.required);

    const completedRequiredFields = requiredFields.filter(field => {
      const value = formData[field.name];
      if (Array.isArray(value)) {
        return value.length > 0;
      }
      return value && value !== '' && value !== null && value !== undefined;
    });

    const completedOptionalFields = optionalFields.filter(field => {
      const value = formData[field.name];
      if (Array.isArray(value)) {
        return value.length > 0;
      }
      return value && value !== '' && value !== null && value !== undefined;
    });

    const stepQueries = queries.filter(q => q.stepNumber === stepIndex);
    const openQueries = stepQueries.filter(q => q.status === 'OPEN');
    const resolvedQueries = stepQueries.filter(q => q.status === 'RESOLVED');

    return {
      total: stepFields.length,
      required: requiredFields.length,
      optional: optionalFields.length,
      completed: completedRequiredFields.length + completedOptionalFields.length,
      requiredCompleted: completedRequiredFields.length,
      optionalCompleted: completedOptionalFields.length,
      isComplete: completedRequiredFields.length === requiredFields.length,
      hasOpenQueries: openQueries.length > 0,
      hasResolvedQueries: resolvedQueries.length > 0,
      openQueriesCount: openQueries.length,
      resolvedQueriesCount: resolvedQueries.length,
      completionPercentage: stepFields.length > 0 ?
        Math.round(((completedRequiredFields.length + completedOptionalFields.length) / stepFields.length) * 100) : 100,
      requiredCompletionPercentage: requiredFields.length > 0 ?
        Math.round((completedRequiredFields.length / requiredFields.length) * 100) : 100
    };
  };



  const handleRaiseQuery = (fieldName) => {
    const field = questionnaireSteps[currentStep].fields.find(f => f.name === fieldName);
    const currentValue = formData[fieldName] || form.getFieldValue(fieldName);

    setSelectedField({
      ...field,
      stepNumber: currentStep,
      stepTitle: questionnaireSteps[currentStep].title,
      currentValue: currentValue,
      materialContext: {
        materialCode: workflowData?.materialCode,
        materialName: workflowData?.materialName,
        materialType: formData.materialType || workflowData?.materialType,
        supplierName: formData.supplierName || workflowData?.supplierName
      }
    });
    setQueryModalVisible(true);
  };

  const handleQueryCreated = (queryData) => {
    setQueryModalVisible(false);
    setSelectedField(null);
    loadQueries(); // Reload queries
    message.success('Query raised successfully');
  };

  // Auto-scroll to field with resolved query
  const scrollToResolvedQuery = useCallback((fieldName) => {
    setTimeout(() => {
      const fieldElement = document.querySelector(`[data-field-name="${fieldName}"]`);
      if (fieldElement) {
        fieldElement.scrollIntoView({
          behavior: 'smooth',
          block: 'center',
          inline: 'nearest'
        });

        // Highlight the field briefly
        fieldElement.style.transition = 'background-color 0.3s ease';
        fieldElement.style.backgroundColor = '#f6ffed';
        setTimeout(() => {
          fieldElement.style.backgroundColor = '';
        }, 2000);
      }
    }, 100);
  }, []);

  // Check for newly resolved queries and auto-scroll
  useEffect(() => {
    if (queries.length > 0) {
      const resolvedQueriesInCurrentStep = queries.filter(q =>
        q.stepNumber === currentStep &&
        q.status === 'RESOLVED' &&
        !q.hasBeenViewed // Add this flag to track if user has seen the resolution
      );

      if (resolvedQueriesInCurrentStep.length > 0) {
        const latestResolvedQuery = resolvedQueriesInCurrentStep
          .sort((a, b) => new Date(b.resolvedAt) - new Date(a.resolvedAt))[0];

        scrollToResolvedQuery(latestResolvedQuery.fieldName);

        // Show notification about resolved query
        notification.success({
          message: 'Query Resolved',
          description: `Your query about "${latestResolvedQuery.fieldName}" has been resolved. Check the field for the response.`,
          duration: 5,
          placement: 'topRight'
        });
      }
    }
  }, [queries, currentStep, scrollToResolvedQuery]);

  const handleSubmit = async () => {
    try {
      setSubmitting(true);

      // Check for open queries
      const openQueries = queries.filter(q => q.status === 'OPEN');
      if (openQueries.length > 0) {
        Modal.confirm({
          title: 'Open Queries Detected',
          content: `You have ${openQueries.length} open queries. Are you sure you want to submit the questionnaire? It's recommended to resolve all queries before submission.`,
          okText: 'Submit Anyway',
          cancelText: 'Cancel',
          onOk: () => proceedWithSubmission()
        });
        return;
      }

      await proceedWithSubmission();
    } catch (error) {
      console.error('Failed to submit questionnaire:', error);
      if (error.status === 400) {
        message.error('Please complete all required fields before submitting');
      } else if (error.status === 401) {
        message.error('Session expired. Please log in again.');
      } else {
        message.error('Failed to submit questionnaire. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const proceedWithSubmission = async () => {
    // Validate all required fields
    const allRequiredFields = questionnaireSteps.flatMap(step =>
      step.fields.filter(field => field.required).map(field => field.name)
    );

    await form.validateFields(allRequiredFields);

    const finalData = form.getFieldsValue();

    // Check completion percentage
    const completionPercentage = getOverallCompletionPercentage();
    if (completionPercentage < 80) {
      const proceed = await new Promise((resolve) => {
        Modal.confirm({
          title: 'Incomplete Questionnaire',
          content: `Your questionnaire is only ${completionPercentage}% complete. Are you sure you want to submit?`,
          okText: 'Submit',
          cancelText: 'Continue Editing',
          onOk: () => resolve(true),
          onCancel: () => resolve(false)
        });
      });

      if (!proceed) {
        return;
      }
    }

    const submissionData = {
      plantCode: workflowData?.assignedPlant,
      materialCode: workflowData?.materialCode,
      blockCode: workflowData?.blockCode,
      responses: finalData,
      completionPercentage,
      submittedBy: 'current_user',
      totalQueries: queries.length,
      openQueries: queries.filter(q => q.status === 'OPEN').length
    };

    await workflowAPI.submitPlantQuestionnaire(workflowId, submissionData);

    // Clear draft data after successful submission
    try {
      localStorage.removeItem(`plant_questionnaire_draft_${workflowId}`);
    } catch (error) {
      console.warn('Failed to clear draft data:', error);
    }

    message.success('Questionnaire submitted successfully');

    if (onComplete) {
      onComplete(finalData);
    }
  };

  const renderField = (field) => {
    const fieldQueries = queries.filter(q =>
      q.fieldName === field.name && q.stepNumber === currentStep
    );

    const hasOpenQuery = fieldQueries.some(q => q.status === 'OPEN');
    const hasResolvedQuery = fieldQueries.some(q => q.status === 'RESOLVED');
    const resolvedQuery = fieldQueries.find(q => q.status === 'RESOLVED');

    const isFieldCompleted = formData[field.name] && formData[field.name] !== '';

    const fieldLabel = (
      <Space>
        {field.label}
        {field.required && <span style={{ color: 'red' }}>*</span>}
        {isFieldCompleted && (
          <Tooltip title="Field completed">
            <CheckCircleOutlined style={{ color: '#52c41a', fontSize: '12px' }} />
          </Tooltip>
        )}
        <Tooltip title="Raise a query about this field">
          <Button
            type="primary"
            size="small"
            icon={<QuestionCircleOutlined />}
            onClick={() => handleRaiseQuery(field.name)}
            style={{ 
              backgroundColor: '#1890ff',
              borderColor: '#1890ff',
              marginLeft: '8px'
            }}
          >
            Query
          </Button>
        </Tooltip>
        {hasOpenQuery && (
          <Badge status="error" text="Query Open" />
        )}
        {hasResolvedQuery && !hasOpenQuery && (
          <Badge status="success" text="Query Resolved" />
        )}
      </Space>
    );

    // Enhanced validation rules
    const validationRules = getFieldValidationRules(field);

    const helpContent = resolvedQuery ? (
      <div style={{
        marginTop: 4,
        padding: '8px 12px',
        backgroundColor: '#f6ffed',
        border: '1px solid #b7eb8f',
        borderRadius: '6px',
        fontSize: '12px'
      }}>
        <div style={{ marginBottom: 4 }}>
          <Text strong style={{ color: '#52c41a' }}>Query Response:</Text>
        </div>
        <div style={{ marginBottom: 4 }}>
          {resolvedQuery.response}
        </div>
        <div style={{ fontSize: '10px', color: '#666' }}>
          Resolved by {resolvedQuery.resolvedBy} on {new Date(resolvedQuery.resolvedAt).toLocaleDateString()}
        </div>
      </div>
    ) : getFieldHelpText(field);

    const commonProps = {
      name: field.name,
      label: fieldLabel,
      rules: validationRules,
      help: helpContent,
      'data-field-name': field.name // Add data attribute for auto-scrolling
    };

    const renderFormItem = (content) => (
      <div data-field-name={field.name} style={{ position: 'relative' }}>
        {content}
        {/* Visual indicator for resolved queries */}
        {hasResolvedQuery && !hasOpenQuery && (
          <div style={{
            position: 'absolute',
            top: '-2px',
            right: '-2px',
            width: '8px',
            height: '8px',
            backgroundColor: '#52c41a',
            borderRadius: '50%',
            border: '2px solid white',
            boxShadow: '0 0 4px rgba(82, 196, 26, 0.5)'
          }} />
        )}
      </div>
    );

    switch (field.type) {
      case 'input':
        return renderFormItem(
          <Form.Item {...commonProps}>
            <Input placeholder={field.placeholder} />
          </Form.Item>
        );

      case 'textarea':
        return renderFormItem(
          <Form.Item {...commonProps}>
            <TextArea rows={4} placeholder={field.placeholder} />
          </Form.Item>
        );

      case 'select':
        return renderFormItem(
          <Form.Item {...commonProps}>
            <Select placeholder={`Select ${field.label.toLowerCase()}`}>
              {field.options.map(option => (
                <Option key={option.value} value={option.value}>
                  {option.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
        );

      case 'radio':
        return renderFormItem(
          <Form.Item {...commonProps}>
            <Radio.Group>
              {field.options.map(option => (
                <Radio key={option.value} value={option.value}>
                  {option.label}
                </Radio>
              ))}
            </Radio.Group>
          </Form.Item>
        );

      case 'checkbox':
        return renderFormItem(
          <Form.Item {...commonProps} valuePropName="checked">
            <Checkbox.Group>
              {field.options.map(option => (
                <Checkbox key={option.value} value={option.value}>
                  {option.label}
                </Checkbox>
              ))}
            </Checkbox.Group>
          </Form.Item>
        );

      default:
        return null;
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
        <div style={{ marginTop: 16 }}>Loading questionnaire...</div>
      </div>
    );
  }

  if (!workflowData) {
    return (
      <Alert
        message="Workflow Not Found"
        description="The requested workflow could not be loaded."
        type="error"
        showIcon
      />
    );
  }

  const progress = Math.round(((currentStep + 1) / questionnaireSteps.length) * 100);
  const currentStepData = questionnaireSteps[currentStep];

  return (
    <div style={{ padding: isMobile ? '8px' : '24px' }}>
      <Row gutter={[16, 16]}>
        {/* Material Context Panel */}
        {!isMobile && (
          <Col xs={24} lg={8}>
            <MaterialContextPanel workflowData={workflowData} />
          </Col>
        )}

        {/* Main Questionnaire */}
        <Col xs={24} lg={!isMobile ? 16 : 24}>
          <Card
            title={
              <Space>
                <span>Plant Questionnaire</span>
                <Badge count={queries.filter(q => q.status === 'OPEN').length} />
              </Space>
            }
            extra={
              <Space>
                <Button
                  icon={<SaveOutlined />}
                  onClick={() => handleSaveDraft()}
                  loading={saving}
                  size={isMobile ? 'small' : 'default'}
                >
                  {isMobile ? 'Save' : 'Save Draft'}
                </Button>
                <Tooltip title={autoSaveEnabled ? 'Auto-save enabled (every 30s)' : 'Auto-save disabled'}>
                  <Button
                    type={autoSaveEnabled ? 'primary' : 'default'}
                    size="small"
                    onClick={() => setAutoSaveEnabled(!autoSaveEnabled)}
                  >
                    Auto
                  </Button>
                </Tooltip>
                <Tooltip title="View completion summary">
                  <Button
                    size="small"
                    onClick={() => {
                      const summaryData = questionnaireSteps.map((step, index) => {
                        const status = getStepCompletionStatus(index);
                        const stepQueries = queries.filter(q => q.stepNumber === index);
                        return {
                          step: index + 1,
                          title: step.title,
                          completed: status.requiredCompleted,
                          required: status.required,
                          percentage: status.required > 0 ? Math.round((status.requiredCompleted / status.required) * 100) : 100,
                          openQueries: stepQueries.filter(q => q.status === 'OPEN').length,
                          resolvedQueries: stepQueries.filter(q => q.status === 'RESOLVED').length
                        };
                      });

                      Modal.info({
                        title: 'Questionnaire Summary',
                        width: 600,
                        content: (
                          <div>
                            <div style={{ marginBottom: 16 }}>
                              <Text strong>Overall Progress: {getOverallCompletionPercentage()}%</Text>
                            </div>
                            {summaryData.map(step => (
                              <div key={step.step} style={{
                                marginBottom: 12,
                                padding: '8px 12px',
                                backgroundColor: step.percentage === 100 ? '#f6ffed' : '#fff7e6',
                                border: `1px solid ${step.percentage === 100 ? '#b7eb8f' : '#ffd591'}`,
                                borderRadius: '4px'
                              }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                  <Text strong>Step {step.step}: {step.title}</Text>
                                  <Tag color={step.percentage === 100 ? 'green' : 'orange'}>
                                    {step.percentage}%
                                  </Tag>
                                </div>
                                <div style={{ fontSize: '12px', color: '#666', marginTop: 4 }}>
                                  {step.completed}/{step.required} required fields completed
                                  {step.openQueries > 0 && (
                                    <span style={{ color: '#ff4d4f', marginLeft: 8 }}>
                                      • {step.openQueries} open queries
                                    </span>
                                  )}
                                  {step.resolvedQueries > 0 && (
                                    <span style={{ color: '#52c41a', marginLeft: 8 }}>
                                      • {step.resolvedQueries} resolved queries
                                    </span>
                                  )}
                                </div>
                              </div>
                            ))}
                          </div>
                        )
                      });
                    }}
                  >
                    Summary
                  </Button>
                </Tooltip>
              </Space>
            }
          >
            {/* Offline Indicator */}
            {isOffline && (
              <Alert
                message="Offline Mode"
                description={
                  <Space direction="vertical" size="small">
                    <span>You are currently offline. Changes will be saved locally and synced when connection is restored.</span>
                    {pendingChanges && (
                      <span style={{ color: '#fa8c16' }}>
                        <CloudSyncOutlined /> Pending changes will be synced automatically
                      </span>
                    )}
                  </Space>
                }
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
                closable
              />
            )}

            {/* Enhanced Progress Indicator */}
            <div style={{ marginBottom: 24 }}>
              <Row gutter={[16, 8]} align="middle">
                <Col xs={24} sm={16}>
                  <Progress
                    percent={getOverallCompletionPercentage()}
                    status="active"
                    format={() => `${getOverallCompletionPercentage()}% Complete`}
                    strokeColor={{
                      '0%': '#108ee9',
                      '100%': '#87d068',
                    }}
                  />
                  <div style={{ marginTop: 4, fontSize: '12px', color: '#666' }}>
                    Step {currentStep + 1} of {questionnaireSteps.length}: {currentStepData.title}
                  </div>
                  {/* Step-specific progress */}
                  <div style={{ marginTop: 2 }}>
                    <Progress
                      percent={(() => {
                        const status = getStepCompletionStatus(currentStep);
                        return status.required > 0 ? Math.round((status.requiredCompleted / status.required) * 100) : 100;
                      })()}
                      size="small"
                      showInfo={false}
                      strokeColor="#52c41a"
                      trailColor="#f0f0f0"
                    />
                    <Text style={{ fontSize: '11px', color: '#999' }}>
                      Current step progress
                    </Text>
                  </div>
                </Col>
                <Col xs={24} sm={8}>
                  <Space direction="vertical" size="small" style={{ width: '100%' }}>
                    <Text style={{ fontSize: '12px', color: '#666' }}>
                      Current Step: {(() => {
                        const status = getStepCompletionStatus(currentStep);
                        return `${status.requiredCompleted}/${status.required} required fields`;
                      })()}
                    </Text>
                    <Text style={{ fontSize: '12px', color: '#666' }}>
                      Overall: {completedSteps.size}/{questionnaireSteps.length} steps completed
                    </Text>
                    <Text style={{ fontSize: '12px', color: '#666' }}>
                      Total Fields: {Object.keys(formData).length}/{questionnaireSteps.reduce((sum, step) => sum + step.fields.length, 0)} filled
                    </Text>
                    {Object.keys(formData).length > 0 && (
                      <Text style={{ fontSize: '12px', color: '#52c41a' }}>
                        <CheckCircleOutlined /> Draft saved
                      </Text>
                    )}
                    {pendingChanges && (
                      <Text style={{ fontSize: '12px', color: '#fa8c16' }}>
                        <CloudSyncOutlined /> Pending sync
                      </Text>
                    )}
                    {queries.filter(q => q.status === 'RESOLVED' && q.stepNumber === currentStep).length > 0 && (
                      <Text style={{ fontSize: '12px', color: '#52c41a' }}>
                        <CheckCircleOutlined /> {queries.filter(q => q.status === 'RESOLVED' && q.stepNumber === currentStep).length} queries resolved
                      </Text>
                    )}
                  </Space>
                </Col>
              </Row>
            </div>

            {/* Steps Navigation */}
            {!isMobile && (
              <Steps
                current={currentStep}
                onChange={handleStepChange}
                style={{ marginBottom: 24 }}
                size="small"
              >
                {questionnaireSteps.map((step, index) => {
                  const stepStatus = getStepCompletionStatus(index);
                  const hasOpenQueries = queries.some(q => q.stepNumber === index && q.status === 'OPEN');
                  const hasResolvedQueries = queries.some(q => q.stepNumber === index && q.status === 'RESOLVED');

                  let stepIcon = undefined;
                  if (stepStatus.isComplete) {
                    stepIcon = <CheckCircleOutlined style={{ color: '#52c41a' }} />;
                  } else if (hasOpenQueries) {
                    stepIcon = <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />;
                  } else if (hasResolvedQueries) {
                    stepIcon = <QuestionCircleOutlined style={{ color: '#1890ff' }} />;
                  }

                  return (
                    <Step
                      key={index}
                      title={step.title}
                      description={
                        <div>
                          <div style={{ fontSize: '11px', color: '#666' }}>
                            {step.description}
                          </div>
                          <div style={{ fontSize: '10px', color: '#999', marginTop: 2 }}>
                            {stepStatus.requiredCompleted}/{stepStatus.required} required
                            {hasOpenQueries && (
                              <span style={{ color: '#ff4d4f', marginLeft: 4 }}>
                                • {queries.filter(q => q.stepNumber === index && q.status === 'OPEN').length} open queries
                              </span>
                            )}
                          </div>
                        </div>
                      }
                      status={
                        stepStatus.isComplete ? 'finish' :
                          index === currentStep ? 'process' : 'wait'
                      }
                      icon={stepIcon}
                    />
                  );
                })}
              </Steps>
            )}

            {/* Mobile Step Indicator */}
            {isMobile && (
              <Card size="small" style={{ marginBottom: 16 }}>
                <div style={{ textAlign: 'center' }}>
                  <h4>{currentStepData.title}</h4>
                  <p style={{ margin: 0, color: '#666' }}>{currentStepData.description}</p>
                  <div style={{ marginTop: 8, fontSize: '12px' }}>
                    <Space direction="vertical" size="small">
                      <Space>
                        <Text type="secondary">
                          {(() => {
                            const status = getStepCompletionStatus(currentStep);
                            return `${status.requiredCompleted}/${status.required} required fields`;
                          })()}
                        </Text>
                        {queries.filter(q => q.stepNumber === currentStep && q.status === 'OPEN').length > 0 && (
                          <Tag color="red" size="small">
                            {queries.filter(q => q.stepNumber === currentStep && q.status === 'OPEN').length} open queries
                          </Tag>
                        )}
                        {queries.filter(q => q.stepNumber === currentStep && q.status === 'RESOLVED').length > 0 && (
                          <Tag color="green" size="small">
                            {queries.filter(q => q.stepNumber === currentStep && q.status === 'RESOLVED').length} resolved
                          </Tag>
                        )}
                      </Space>
                      {/* Mobile step navigation */}
                      <div style={{ marginTop: 8 }}>
                        <Space>
                          <Button
                            size="small"
                            disabled={currentStep === 0}
                            onClick={handlePrevious}
                          >
                            ← Prev
                          </Button>
                          <Text style={{ fontSize: '11px' }}>
                            {currentStep + 1} / {questionnaireSteps.length}
                          </Text>
                          <Button
                            size="small"
                            disabled={currentStep === questionnaireSteps.length - 1}
                            onClick={handleNext}
                          >
                            Next →
                          </Button>
                        </Space>
                      </div>
                    </Space>
                  </div>
                </div>
              </Card>
            )}

            {/* Form Fields */}
            <Form
              form={form}
              layout="vertical"
              onValuesChange={(changedValues, allValues) => {
                setFormData(prev => ({ ...prev, ...allValues }));
              }}
            >
              <Row gutter={[16, 16]}>
                {currentStepData.fields.map((field, index) => (
                  <Col
                    key={field.name}
                    xs={24}
                    sm={field.type === 'textarea' ? 24 : 12}
                    md={field.type === 'textarea' ? 24 : 12}
                  >
                    {renderField(field)}
                  </Col>
                ))}
              </Row>
            </Form>

            <Divider />

            {/* Navigation Buttons */}
            <Row justify="space-between" align="middle">
              <Col>
                <Button
                  icon={<ArrowLeftOutlined />}
                  onClick={handlePrevious}
                  disabled={currentStep === 0}
                  size={isMobile ? 'small' : 'default'}
                >
                  Previous
                </Button>
              </Col>

              <Col>
                <Space>
                  {currentStep === questionnaireSteps.length - 1 ? (
                    <Button
                      type="primary"
                      onClick={handleSubmit}
                      loading={submitting}
                      size={isMobile ? 'small' : 'default'}
                    >
                      Submit Questionnaire
                    </Button>
                  ) : (
                    <Button
                      type="primary"
                      icon={<ArrowRightOutlined />}
                      onClick={handleNext}
                      size={isMobile ? 'small' : 'default'}
                    >
                      Next
                    </Button>
                  )}
                </Space>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* Query Raising Modal */}
      <QueryRaisingModal
        visible={queryModalVisible}
        onCancel={() => {
          setQueryModalVisible(false);
          setSelectedField(null);
        }}
        onSubmit={handleQueryCreated}
        workflowId={workflowId}
        fieldContext={selectedField}
      />
    </div>
  );
};

export default PlantQuestionnaire;