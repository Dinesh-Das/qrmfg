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
  Modal,
  message,
  Spin,
  Alert,
  Divider,
  Space,
  Tooltip,
  Badge
} from 'antd';
import {
  SaveOutlined,
  QuestionCircleOutlined,
  InfoCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  ArrowLeftOutlined,
  ArrowRightOutlined
} from '@ant-design/icons';
import { workflowAPI } from '../../services/workflowAPI';
import { queryAPI } from '../../services/queryAPI';
import QueryRaisingModal from './QueryRaisingModal';
import MaterialContextPanel from './MaterialContextPanel';

const { Step } = Steps;
const { TextArea } = Input;
const { Option } = Select;

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

  // Network status monitoring
  useEffect(() => {
    const handleOnline = () => {
      setIsOffline(false);
      if (pendingChanges) {
        handleSaveDraft(true); // Auto-sync when back online
        setPendingChanges(false);
      }
    };
    
    const handleOffline = () => {
      setIsOffline(true);
      message.warning('You are offline. Changes will be saved locally.');
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, [pendingChanges]);

  // Define questionnaire steps
  const questionnaireSteps = [
    {
      title: 'Basic Information',
      description: 'Material identification and basic properties',
      fields: [
        {
          name: 'materialName',
          label: 'Material Name',
          type: 'input',
          required: true,
          placeholder: 'Enter the full material name'
        },
        {
          name: 'materialType',
          label: 'Material Type',
          type: 'select',
          required: true,
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
          placeholder: 'e.g., 64-17-5'
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
          placeholder: 'Enter boiling point in Celsius'
        },
        {
          name: 'meltingPoint',
          label: 'Melting Point (°C)',
          type: 'input',
          placeholder: 'Enter melting point in Celsius'
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
          placeholder: 'List all applicable H-statements (e.g., H225, H319)'
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
          placeholder: 'List all applicable P-statements (e.g., P210, P280)'
        },
        {
          name: 'personalProtection',
          label: 'Personal Protection Equipment',
          type: 'checkbox',
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
  }, [formData, autoSaveEnabled]);

  // Auto-recovery on component mount
  useEffect(() => {
    const recoverDraftData = () => {
      try {
        const draftKey = `plant_questionnaire_draft_${workflowId}`;
        const savedDraft = localStorage.getItem(draftKey);
        
        if (savedDraft) {
          const draftData = JSON.parse(savedDraft);
          const draftTimestamp = draftData.timestamp;
          const currentTime = Date.now();
          
          // Only recover if draft is less than 24 hours old
          if (currentTime - draftTimestamp < 24 * 60 * 60 * 1000) {
            setFormData(prev => ({ ...prev, ...draftData.formData }));
            form.setFieldsValue(draftData.formData);
            setCurrentStep(draftData.currentStep || 0);
            
            message.info('Draft data recovered from previous session');
          } else {
            // Remove old draft
            localStorage.removeItem(draftKey);
          }
        }
      } catch (error) {
        console.error('Failed to recover draft data:', error);
      }
    };

    if (workflowId && !workflowData) {
      recoverDraftData();
    }
  }, [workflowId, workflowData, form]);

  const loadWorkflowData = async () => {
    try {
      setLoading(true);
      const workflow = await workflowAPI.getWorkflow(workflowId);
      setWorkflowData(workflow);
      
      // Load existing responses if any
      if (workflow.responses && workflow.responses.length > 0) {
        const existingData = {};
        const completed = new Set();
        
        workflow.responses.forEach(response => {
          existingData[response.fieldName] = response.fieldValue;
          completed.add(response.stepNumber);
        });
        
        setFormData(existingData);
        setCompletedSteps(completed);
        form.setFieldsValue(existingData);
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

  const handleSaveDraft = useCallback(async (silent = false) => {
    try {
      setSaving(true);
      const currentValues = form.getFieldsValue();
      const updatedFormData = { ...formData, ...currentValues };
      
      // Save to local storage as backup
      const draftKey = `plant_questionnaire_draft_${workflowId}`;
      const draftData = {
        formData: updatedFormData,
        currentStep,
        timestamp: Date.now(),
        completedSteps: Array.from(completedSteps)
      };
      
      try {
        localStorage.setItem(draftKey, JSON.stringify(draftData));
      } catch (localStorageError) {
        console.warn('Failed to save draft to local storage:', localStorageError);
      }
      
      // Save to server
      await workflowAPI.saveDraftResponses(workflowId, {
        responses: Object.entries(updatedFormData).map(([fieldName, fieldValue]) => ({
          fieldName,
          fieldValue: typeof fieldValue === 'object' ? JSON.stringify(fieldValue) : String(fieldValue),
          stepNumber: getStepForField(fieldName)
        })),
        currentStep,
        completedSteps: Array.from(completedSteps)
      });
      
      setFormData(updatedFormData);
      
      if (!silent) {
        message.success('Draft saved successfully');
      }
      
      if (onSaveDraft) {
        onSaveDraft(updatedFormData);
      }
    } catch (error) {
      console.error('Failed to save draft:', error);
      if (!silent) {
        message.error('Failed to save draft. Your progress is saved locally.');
      }
    } finally {
      setSaving(false);
    }
  }, [form, formData, workflowId, onSaveDraft, currentStep, completedSteps]);

  const getStepForField = (fieldName) => {
    for (let i = 0; i < questionnaireSteps.length; i++) {
      if (questionnaireSteps[i].fields.some(field => field.name === fieldName)) {
        return i;
      }
    }
    return 0;
  };

  const handleStepChange = (step) => {
    // Validate current step before moving
    const currentStepFields = questionnaireSteps[currentStep].fields.map(field => field.name);
    
    form.validateFields(currentStepFields)
      .then(() => {
        setCurrentStep(step);
        setCompletedSteps(prev => new Set([...prev, currentStep]));
      })
      .catch(() => {
        message.warning('Please complete all required fields before proceeding');
      });
  };

  const handleNext = () => {
    if (currentStep < questionnaireSteps.length - 1) {
      handleStepChange(currentStep + 1);
    }
  };

  const handlePrevious = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleRaiseQuery = (fieldName) => {
    const field = questionnaireSteps[currentStep].fields.find(f => f.name === fieldName);
    setSelectedField({
      ...field,
      stepNumber: currentStep,
      stepTitle: questionnaireSteps[currentStep].title
    });
    setQueryModalVisible(true);
  };

  const handleQueryCreated = (queryData) => {
    setQueryModalVisible(false);
    setSelectedField(null);
    loadQueries(); // Reload queries
    message.success('Query raised successfully');
  };

  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      
      // Validate all fields
      const allFields = questionnaireSteps.flatMap(step => step.fields.map(field => field.name));
      await form.validateFields(allFields);
      
      const finalData = form.getFieldsValue();
      
      await workflowAPI.submitQuestionnaire(workflowId, {
        responses: Object.entries(finalData).map(([fieldName, fieldValue]) => ({
          fieldName,
          fieldValue: typeof fieldValue === 'object' ? JSON.stringify(fieldValue) : String(fieldValue),
          stepNumber: getStepForField(fieldName)
        }))
      });
      
      message.success('Questionnaire submitted successfully');
      
      if (onComplete) {
        onComplete(finalData);
      }
    } catch (error) {
      console.error('Failed to submit questionnaire:', error);
      message.error('Failed to submit questionnaire');
    } finally {
      setSubmitting(false);
    }
  };

  const renderField = (field) => {
    const fieldQueries = queries.filter(q => 
      q.fieldName === field.name && q.stepNumber === currentStep
    );
    
    const hasOpenQuery = fieldQueries.some(q => q.status === 'OPEN');
    const hasResolvedQuery = fieldQueries.some(q => q.status === 'RESOLVED');

    const fieldLabel = (
      <Space>
        {field.label}
        {field.required && <span style={{ color: 'red' }}>*</span>}
        <Tooltip title="Raise a query about this field">
          <Button
            type="text"
            size="small"
            icon={<QuestionCircleOutlined />}
            onClick={() => handleRaiseQuery(field.name)}
          />
        </Tooltip>
        {hasOpenQuery && (
          <Badge status="error" text="Query Open" />
        )}
        {hasResolvedQuery && !hasOpenQuery && (
          <Badge status="success" text="Query Resolved" />
        )}
      </Space>
    );

    const commonProps = {
      name: field.name,
      label: fieldLabel,
      rules: field.required ? [{ required: true, message: `${field.label} is required` }] : [],
    };

    switch (field.type) {
      case 'input':
        return (
          <Form.Item {...commonProps}>
            <Input placeholder={field.placeholder} />
          </Form.Item>
        );
      
      case 'textarea':
        return (
          <Form.Item {...commonProps}>
            <TextArea rows={4} placeholder={field.placeholder} />
          </Form.Item>
        );
      
      case 'select':
        return (
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
        return (
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
        return (
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
                <Tooltip title={autoSaveEnabled ? 'Auto-save enabled' : 'Auto-save disabled'}>
                  <Button
                    type={autoSaveEnabled ? 'primary' : 'default'}
                    size="small"
                    onClick={() => setAutoSaveEnabled(!autoSaveEnabled)}
                  >
                    Auto
                  </Button>
                </Tooltip>
              </Space>
            }
          >
            {/* Offline Indicator */}
            {isOffline && (
              <Alert
                message="Offline Mode"
                description="You are currently offline. Changes will be saved locally and synced when connection is restored."
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
                closable
              />
            )}

            {/* Progress Indicator */}
            <div style={{ marginBottom: 24 }}>
              <Progress
                percent={progress}
                status="active"
                format={() => `Step ${currentStep + 1}/${questionnaireSteps.length}`}
              />
            </div>

            {/* Steps Navigation */}
            {!isMobile && (
              <Steps
                current={currentStep}
                onChange={handleStepChange}
                style={{ marginBottom: 24 }}
                size="small"
              >
                {questionnaireSteps.map((step, index) => (
                  <Step
                    key={index}
                    title={step.title}
                    description={step.description}
                    status={
                      completedSteps.has(index) ? 'finish' :
                      index === currentStep ? 'process' : 'wait'
                    }
                    icon={
                      completedSteps.has(index) ? <CheckCircleOutlined /> :
                      queries.some(q => q.stepNumber === index && q.status === 'OPEN') ? 
                        <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} /> : undefined
                    }
                  />
                ))}
              </Steps>
            )}

            {/* Mobile Step Indicator */}
            {isMobile && (
              <Card size="small" style={{ marginBottom: 16 }}>
                <div style={{ textAlign: 'center' }}>
                  <h4>{currentStepData.title}</h4>
                  <p style={{ margin: 0, color: '#666' }}>{currentStepData.description}</p>
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