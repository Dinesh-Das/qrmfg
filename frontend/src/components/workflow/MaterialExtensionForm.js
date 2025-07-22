import React, { useState, useEffect, useCallback } from 'react';
import {
  Form,
  Select,
  Upload,
  Button,
  Card,
  Row,
  Col,
  message,
  Space,
  Typography,
  Alert,
  Divider,
  List,
  Tag,
  Checkbox,
  Progress,
  Tooltip,
  Spin,
  Empty,
  Badge,
  Descriptions,
  Statistic,
  Modal
} from 'antd';
import {
  UploadOutlined,
  FileTextOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  EyeOutlined,
  DeleteOutlined,
  InfoCircleOutlined,
  CloudUploadOutlined,
  FolderOpenOutlined,
  ExclamationCircleOutlined,
  SafetyCertificateOutlined,
  WarningOutlined,
  FileAddOutlined
} from '@ant-design/icons';
import { projectAPI } from '../../services/projectAPI';
import { documentAPI } from '../../services/documentAPI';

const { Option } = Select;
const { Title, Text } = Typography;

const MaterialExtensionForm = ({ onSubmit, loading }) => {
  const [form] = Form.useForm();
  const [projects, setProjects] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [plants, setPlants] = useState([]);
  const [blocks, setBlocks] = useState([]);
  const [fileList, setFileList] = useState([]);
  const [reusableDocuments, setReusableDocuments] = useState([]);
  const [selectedReusableDocuments, setSelectedReusableDocuments] = useState([]);
  const [showReusableDocuments, setShowReusableDocuments] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [formValidation, setFormValidation] = useState({
    isValid: false,
    errors: []
  });
  const [loadingStates, setLoadingStates] = useState({
    projects: false,
    materials: false,
    plants: false,
    blocks: false,
    reusableDocuments: false
  });

  useEffect(() => {
    loadProjects();
    loadPlants();
  }, []);

  useEffect(() => {
    validateForm();
  }, [projects, materials, plants, blocks, fileList, selectedReusableDocuments]);

  const validateForm = useCallback(() => {
    const values = form.getFieldsValue();
    const errors = [];

    if (!values.projectCode) errors.push('Project Code is required');
    if (!values.materialCode) errors.push('Material Code is required');
    if (!values.plantCode) errors.push('Plant Code is required');
    if (!values.blockId) errors.push('Block ID is required');

    if (fileList.length === 0 && selectedReusableDocuments.length === 0) {
      errors.push('At least one document must be uploaded or reused');
    }

    const hasInvalidFiles = fileList.some(file => file.status === 'error');
    if (hasInvalidFiles) {
      errors.push('Some uploaded files have validation errors');
    }

    setFormValidation({
      isValid: errors.length === 0,
      errors
    });
  }, [form, fileList, selectedReusableDocuments]);

  const setLoadingState = (key, value) => {
    setLoadingStates(prev => ({ ...prev, [key]: value }));
  };

  const loadProjects = async () => {
    try {
      setLoadingState('projects', true);
      const projectData = await projectAPI.getProjects();
      setProjects(projectData || []);
    } catch (error) {
      console.error('Error loading projects:', error);
      message.error('Failed to load projects');
    } finally {
      setLoadingState('projects', false);
    }
  };

  const loadPlants = async () => {
    try {
      setLoadingState('plants', true);
      const plantData = await projectAPI.getPlants();
      setPlants(plantData || []);
    } catch (error) {
      console.error('Error loading plants:', error);
      message.error('Failed to load plants');
    } finally {
      setLoadingState('plants', false);
    }
  };

  const handleProjectChange = async (projectCode) => {
    try {
      setLoadingState('materials', true);
      setMaterials([]);
      form.setFieldsValue({ materialCode: undefined });

      const materialData = await projectAPI.getMaterialsByProject(projectCode);
      setMaterials(materialData || []);

      // Clear reusable documents when project changes
      setReusableDocuments([]);
      setSelectedReusableDocuments([]);
      setShowReusableDocuments(false);
    } catch (error) {
      console.error('Error loading materials:', error);
      message.error('Failed to load materials for selected project');
    } finally {
      setLoadingState('materials', false);
    }
  };

  const handlePlantChange = async (plantCode) => {
    try {
      setLoadingState('blocks', true);
      setBlocks([]);
      form.setFieldsValue({ blockId: undefined });

      const blockData = await projectAPI.getBlocksByPlant(plantCode);
      setBlocks(blockData || []);
    } catch (error) {
      console.error('Error loading blocks:', error);
      message.error('Failed to load blocks for selected plant');
    } finally {
      setLoadingState('blocks', false);
    }
  };

  const handleMaterialChange = async (materialCode) => {
    const projectCode = form.getFieldValue('projectCode');
    if (projectCode && materialCode) {
      await checkForReusableDocuments(projectCode, materialCode);
    }
  };

  const checkForReusableDocuments = async (projectCode, materialCode) => {
    try {
      setLoadingState('reusableDocuments', true);
      const reusableDocs = await documentAPI.getReusableDocuments(projectCode, materialCode);

      if (reusableDocs && reusableDocs.length > 0) {
        setReusableDocuments(reusableDocs);
        setShowReusableDocuments(true);
        // Pre-select all reusable documents by default
        setSelectedReusableDocuments(reusableDocs.map(doc => doc.id));
      } else {
        setReusableDocuments([]);
        setShowReusableDocuments(false);
        setSelectedReusableDocuments([]);
      }
    } catch (error) {
      console.error('Error checking for reusable documents:', error);
      // Don't show error message as this is not critical
    } finally {
      setLoadingState('reusableDocuments', false);
    }
  };

  const handleFileChange = ({ fileList: newFileList }) => {
    // Validate each file and add enhanced metadata
    const validatedFileList = newFileList.map(file => {
      if (file.originFileObj) {
        const validation = documentAPI.validateFile(file.originFileObj);
        if (!validation.isValidType) {
          file.status = 'error';
          file.response = 'Invalid file type. Only PDF, Word, and Excel files are allowed.';
        } else if (!validation.isValidSize) {
          file.status = 'error';
          file.response = 'File size exceeds 25MB limit.';
        } else {
          file.status = 'done';
          file.percent = 100;
        }

        // Add file metadata for better display
        file.size = file.originFileObj.size;
        file.type = file.originFileObj.type;
        file.lastModified = file.originFileObj.lastModified;
      }
      return file;
    });

    setFileList(validatedFileList);

    // Update upload progress
    const totalFiles = validatedFileList.length;
    const validFiles = validatedFileList.filter(f => f.status === 'done').length;
    setUploadProgress(totalFiles > 0 ? (validFiles / totalFiles) * 100 : 0);
  };

  const handleSubmit = async (values) => {
    try {
      if (!formValidation.isValid) {
        message.error('Please fix all validation errors before submitting');
        return;
      }

      // Prepare submission data with enhanced metadata
      const submissionData = {
        ...values,
        uploadedFiles: fileList.filter(file => file.status === 'done'),
        reusedDocuments: selectedReusableDocuments,
        metadata: {
          totalDocuments: fileList.length + selectedReusableDocuments.length,
          newDocuments: fileList.length,
          reusedDocuments: selectedReusableDocuments.length,
          submittedAt: new Date().toISOString()
        }
      };

      await onSubmit(submissionData);

      // Reset form after successful submission
      resetForm();
      message.success('Material extension created successfully!');
    } catch (error) {
      console.error('Error submitting form:', error);
      message.error('Failed to create material extension. Please try again.');
    }
  };

  const resetForm = () => {
    form.resetFields();
    setFileList([]);
    setReusableDocuments([]);
    setSelectedReusableDocuments([]);
    setShowReusableDocuments(false);
    setMaterials([]);
    setBlocks([]);
    setUploadProgress(0);
    setFormValidation({ isValid: false, errors: [] });
  };

  const uploadProps = {
    name: 'file',
    multiple: true,
    fileList,
    onChange: handleFileChange,
    beforeUpload: (file) => {
      const validation = documentAPI.validateFile(file);

      if (!validation.isValidType) {
        message.error({
          content: `${file.name}: Invalid file type. Only PDF, Word, and Excel files are allowed.`,
          duration: 5
        });
        return false;
      }

      if (!validation.isValidSize) {
        message.error({
          content: `${file.name}: File size exceeds 25MB limit (${(file.size / 1024 / 1024).toFixed(2)}MB).`,
          duration: 5
        });
        return false;
      }

      return false; // Prevent automatic upload, we'll handle it manually
    },
    onRemove: (file) => {
      const index = fileList.indexOf(file);
      const newFileList = fileList.slice();
      newFileList.splice(index, 1);
      setFileList(newFileList);
    },
    showUploadList: {
      showPreviewIcon: true,
      showRemoveIcon: true,
      showDownloadIcon: false,
      removeIcon: <DeleteOutlined />,
      previewIcon: <EyeOutlined />
    },
    onPreview: (file) => {
      if (file.originFileObj) {
        const url = URL.createObjectURL(file.originFileObj);
        window.open(url, '_blank');
      }
    }
  };

  const handleReusableDocumentChange = (documentIds) => {
    setSelectedReusableDocuments(documentIds);
  };

  const previewDocument = async (document) => {
    try {
      const blob = await documentAPI.downloadDocument(document.id);
      const url = window.URL.createObjectURL(blob);
      window.open(url, '_blank');
    } catch (error) {
      console.error('Error previewing document:', error);
      message.error('Failed to preview document');
    }
  };

  return (
    <Card
      title={
        <Space>
          <SafetyCertificateOutlined />
          <span>Material Extension Form</span>
          {!formValidation.isValid && (
            <Badge count={formValidation.errors.length} style={{ backgroundColor: '#ff4d4f' }} />
          )}
        </Space>
      }
      extra={
        <Space>
          {formValidation.isValid && (
            <Tag color="green" icon={<CheckCircleOutlined />}>
              Ready to Submit
            </Tag>
          )}
          <Tooltip title="Reset all form fields">
            <Button
              icon={<ReloadOutlined />}
              onClick={resetForm}
              size="small"
            >
              Reset
            </Button>
          </Tooltip>
        </Space>
      }
    >
      {/* Form Progress Summary */}
      <Card size="small" style={{ marginBottom: 16, backgroundColor: '#fafafa' }}>
        <Row gutter={16} align="middle">
          <Col span={6}>
            <Space>
              <Text strong>Form Progress:</Text>
              <Progress
                type="circle"
                size={40}
                percent={formValidation.isValid ? 100 : Math.max(25, (4 - formValidation.errors.length) * 25)}
                status={formValidation.isValid ? 'success' : 'active'}
                format={() => formValidation.isValid ? '✓' : `${4 - formValidation.errors.length}/4`}
              />
            </Space>
          </Col>
          <Col span={18}>
            {formValidation.errors.length > 0 ? (
              <Alert
                message={`${formValidation.errors.length} field(s) need attention`}
                description={formValidation.errors.join(', ')}
                type="warning"
                showIcon
                size="small"
              />
            ) : (
              <Alert
                message="Form is ready for submission"
                description="All required fields are completed and documents are ready"
                type="success"
                showIcon
                size="small"
              />
            )}
          </Col>
        </Row>
      </Card>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        requiredMark={false}
        size="large"
        onFieldsChange={validateForm}
      >
        {/* Project and Material Selection */}
        <Card
          title={
            <Space>
              <FolderOpenOutlined />
              <span>Project & Material Selection</span>
            </Space>
          }
          size="small"
          style={{ marginBottom: 16 }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label={
                  <Space>
                    <span>Project Code</span>
                    <Tooltip title="Select the project this material extension belongs to">
                      <InfoCircleOutlined style={{ color: '#1890ff' }} />
                    </Tooltip>
                  </Space>
                }
                name="projectCode"
                rules={[{ required: true, message: 'Please select a project' }]}
              >
                <Select
                  placeholder="Select project code"
                  showSearch
                  optionFilterProp="children"
                  loading={loadingStates.projects}
                  onChange={handleProjectChange}
                  suffixIcon={loadingStates.projects ? <ReloadOutlined spin /> : undefined}
                  notFoundContent={loadingStates.projects ? <Spin size="small" /> : <Empty description="No projects found" />}
                >
                  {projects.map(project => (
                    <Option key={project.value} value={project.value}>
                      <Space>
                        <Text strong>{project.value}</Text>
                        {project.label !== project.value && (
                          <Text type="secondary">- {project.label}</Text>
                        )}
                      </Space>
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item
                label={
                  <Space>
                    <span>Material Code</span>
                    <Tooltip title="Material codes are filtered based on selected project">
                      <InfoCircleOutlined style={{ color: '#1890ff' }} />
                    </Tooltip>
                  </Space>
                }
                name="materialCode"
                rules={[{ required: true, message: 'Please select a material' }]}
              >
                <Select
                  placeholder={!form.getFieldValue('projectCode') ? "Select project first" : "Select material code"}
                  showSearch
                  optionFilterProp="children"
                  loading={loadingStates.materials}
                  disabled={!form.getFieldValue('projectCode')}
                  onChange={handleMaterialChange}
                  suffixIcon={loadingStates.materials ? <ReloadOutlined spin /> : undefined}
                  notFoundContent={
                    loadingStates.materials ?
                      <Spin size="small" /> :
                      !form.getFieldValue('projectCode') ?
                        "Select a project first" :
                        <Empty description="No materials found for this project" />
                  }
                  dropdownRender={(menu) => (
                    <div>
                      {menu}
                      {materials.length > 0 && (
                        <div style={{ padding: '8px', borderTop: '1px solid #f0f0f0', color: '#666', fontSize: '12px' }}>
                          {materials.length} material(s) available for {form.getFieldValue('projectCode')}
                        </div>
                      )}
                    </div>
                  )}
                >
                  {materials.map(material => (
                    <Option key={material.value} value={material.value}>
                      <Space direction="vertical" size="small">
                        <Text strong>{material.value}</Text>
                        <Text type="secondary" style={{ fontSize: '12px' }}>
                          {material.label}
                        </Text>
                      </Space>
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Card>

        {/* Plant and Block Selection */}
        <Card
          title={
            <Space>
              <FolderOpenOutlined />
              <span>Location Selection</span>
            </Space>
          }
          size="small"
          style={{ marginBottom: 16 }}
        >
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                label={
                  <Space>
                    <span>Plant Code</span>
                    <Tooltip title="Select the plant where this material will be used">
                      <InfoCircleOutlined style={{ color: '#1890ff' }} />
                    </Tooltip>
                  </Space>
                }
                name="plantCode"
                rules={[{ required: true, message: 'Please select a plant' }]}
              >
                <Select
                  placeholder="Select plant code"
                  showSearch
                  optionFilterProp="children"
                  loading={loadingStates.plants}
                  onChange={handlePlantChange}
                  suffixIcon={loadingStates.plants ? <ReloadOutlined spin /> : undefined}
                  notFoundContent={loadingStates.plants ? <Spin size="small" /> : <Empty description="No plants found" />}
                >
                  {plants.map(plant => (
                    <Option key={plant.value} value={plant.value}>
                      <Space>
                        <Text strong>{plant.value}</Text>
                        {plant.label !== plant.value && (
                          <Text type="secondary">- {plant.label}</Text>
                        )}
                      </Space>
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>

            <Col span={12}>
              <Form.Item
                label={
                  <Space>
                    <span>Block ID</span>
                    <Tooltip title="Block IDs are filtered based on selected plant">
                      <InfoCircleOutlined style={{ color: '#1890ff' }} />
                    </Tooltip>
                  </Space>
                }
                name="blockId"
                rules={[{ required: true, message: 'Please select a block' }]}
              >
                <Select
                  placeholder={!form.getFieldValue('plantCode') ? "Select plant first" : "Select block ID"}
                  showSearch
                  optionFilterProp="children"
                  loading={loadingStates.blocks}
                  disabled={!form.getFieldValue('plantCode')}
                  suffixIcon={loadingStates.blocks ? <ReloadOutlined spin /> : undefined}
                  notFoundContent={
                    loadingStates.blocks ?
                      <Spin size="small" /> :
                      !form.getFieldValue('plantCode') ?
                        "Select a plant first" :
                        <Empty description="No blocks found for this plant" />
                  }
                >
                  {blocks.map(block => (
                    <Option key={block.value} value={block.value}>
                      <Space>
                        <Text strong>{block.value}</Text>
                        {block.label !== block.value && (
                          <Text type="secondary">- {block.label}</Text>
                        )}
                      </Space>
                    </Option>
                  ))}
                </Select>
              </Form.Item>
            </Col>
          </Row>
        </Card>

        {/* Document Reuse Section */}
        {showReusableDocuments && reusableDocuments.length > 0 && (
          <Card
            title={
              <Space>
                <CheckCircleOutlined style={{ color: '#52c41a' }} />
                <span>Reusable Documents Found</span>
                <Badge count={reusableDocuments.length} style={{ backgroundColor: '#52c41a' }} />
              </Space>
            }
            size="small"
            style={{ marginBottom: 16 }}
            extra={
              <Space>
                <Text type="secondary">
                  {selectedReusableDocuments.length} of {reusableDocuments.length} selected
                </Text>
                <Button
                  size="small"
                  type="link"
                  onClick={() => setSelectedReusableDocuments(
                    selectedReusableDocuments.length === reusableDocuments.length ?
                      [] :
                      reusableDocuments.map(doc => doc.id)
                  )}
                >
                  {selectedReusableDocuments.length === reusableDocuments.length ? 'Deselect All' : 'Select All'}
                </Button>
              </Space>
            }
          >
            <Alert
              message="Document Reuse Available"
              description={`Found ${reusableDocuments.length} documents from previous extensions with the same project and material combination. Reusing documents saves time and ensures consistency.`}
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
            />

            <Checkbox.Group
              value={selectedReusableDocuments}
              onChange={handleReusableDocumentChange}
              style={{ width: '100%' }}
            >
              <List
                dataSource={reusableDocuments}
                renderItem={document => (
                  <List.Item
                    actions={[
                      <Tooltip title="Preview document">
                        <Button
                          type="text"
                          icon={<EyeOutlined />}
                          onClick={() => previewDocument(document)}
                          size="small"
                        />
                      </Tooltip>
                    ]}
                  >
                    <List.Item.Meta
                      avatar={
                        <Checkbox
                          value={document.id}
                          style={{ transform: 'scale(1.2)' }}
                        />
                      }
                      title={
                        <Space>
                          <FileTextOutlined style={{ color: '#1890ff' }} />
                          <Text strong>{document.originalFileName}</Text>
                          <Tag color="blue">{document.fileType?.toUpperCase()}</Tag>
                          <Tag color="green">Reusable</Tag>
                        </Space>
                      }
                      description={
                        <Descriptions size="small" column={1}>
                          <Descriptions.Item label="Size">
                            {(document.fileSize / 1024 / 1024).toFixed(2)} MB
                          </Descriptions.Item>
                          <Descriptions.Item label="Uploaded">
                            {new Date(document.uploadedAt).toLocaleDateString()} by {document.uploadedBy}
                          </Descriptions.Item>
                          <Descriptions.Item label="Previous Usage">
                            Used in {document.usageCount || 1} workflow(s)
                          </Descriptions.Item>
                        </Descriptions>
                      }
                    />
                  </List.Item>
                )}
              />
            </Checkbox.Group>
          </Card>
        )}

        {/* Document Upload Section */}
        <Card
          title={
            <Space>
              <CloudUploadOutlined />
              <span>Document Upload</span>
              {fileList.length > 0 && (
                <Badge count={fileList.length} style={{ backgroundColor: '#1890ff' }} />
              )}
            </Space>
          }
          size="small"
          style={{ marginBottom: 16 }}
          extra={
            fileList.length > 0 && (
              <Space>
                <Text type="secondary">
                  {fileList.filter(f => f.status === 'done').length} of {fileList.length} valid
                </Text>
                {uploadProgress > 0 && uploadProgress < 100 && (
                  <Progress
                    type="circle"
                    size="small"
                    percent={Math.round(uploadProgress)}
                  />
                )}
              </Space>
            )
          }
        >
          <Form.Item
            label={
              <Space>
                <span>Upload New Documents</span>
                <Tooltip title="Supported formats: PDF, Word (.doc/.docx), Excel (.xls/.xlsx). Maximum 25MB per file.">
                  <InfoCircleOutlined style={{ color: '#1890ff' }} />
                </Tooltip>
              </Space>
            }
            extra="Upload new documents (PDF, Word, Excel). Maximum 25MB per file."
          >
            <Upload.Dragger {...uploadProps}>
              <p className="ant-upload-drag-icon">
                <CloudUploadOutlined style={{ fontSize: '48px', color: '#1890ff' }} />
              </p>
              <p className="ant-upload-text">Click or drag files to this area to upload</p>
              <p className="ant-upload-hint">
                <Space direction="vertical" size="small">
                  <Text>Support for PDF, Word, and Excel files</Text>
                  <Text type="secondary">Maximum 25MB per file • Multiple files supported</Text>
                </Space>
              </p>
            </Upload.Dragger>
          </Form.Item>

          {/* File Upload Progress */}
          {fileList.length > 0 && (
            <div style={{ marginTop: 16 }}>
              <Text strong>Upload Summary:</Text>
              <div style={{ marginTop: 8 }}>
                <Progress
                  percent={Math.round(uploadProgress)}
                  status={uploadProgress === 100 ? 'success' : 'active'}
                  format={(percent) => `${fileList.filter(f => f.status === 'done').length}/${fileList.length} files`}
                />
              </div>
            </div>
          )}
        </Card>

        {/* Document Summary */}
        {(fileList.length > 0 || selectedReusableDocuments.length > 0) && (
          <Card
            title={
              <Space>
                <FileTextOutlined />
                <span>Document Summary</span>
              </Space>
            }
            size="small"
            style={{ marginBottom: 16 }}
          >
            <Row gutter={16}>
              {selectedReusableDocuments.length > 0 && (
                <Col span={12}>
                  <Statistic
                    title="Reused Documents"
                    value={selectedReusableDocuments.length}
                    prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
                    valueStyle={{ color: '#52c41a' }}
                  />
                </Col>
              )}
              {fileList.length > 0 && (
                <Col span={12}>
                  <Statistic
                    title="New Documents"
                    value={fileList.filter(f => f.status === 'done').length}
                    suffix={`/ ${fileList.length}`}
                    prefix={<CloudUploadOutlined style={{ color: '#1890ff' }} />}
                    valueStyle={{ color: '#1890ff' }}
                  />
                </Col>
              )}
            </Row>

            <Divider />

            <Space direction="vertical" style={{ width: '100%' }}>
              <Text strong>Total Documents: {selectedReusableDocuments.length + fileList.filter(f => f.status === 'done').length}</Text>
              {fileList.some(f => f.status === 'error') && (
                <Alert
                  message="Some files have validation errors"
                  type="warning"
                  showIcon
                  size="small"
                />
              )}
            </Space>
          </Card>
        )}

        {/* Submit Section */}
        <Card size="small">
          <Form.Item style={{ marginBottom: 0 }}>
            <Row justify="space-between" align="middle">
              <Col>
                <Space>
                  <Text type="secondary">
                    Ready to submit: {formValidation.isValid ? 'Yes' : 'No'}
                  </Text>
                  {!formValidation.isValid && (
                    <Text type="danger">
                      ({formValidation.errors.length} issue{formValidation.errors.length !== 1 ? 's' : ''})
                    </Text>
                  )}
                </Space>
              </Col>
              <Col>
                <Space>
                  <Button onClick={resetForm}>
                    Cancel
                  </Button>
                  <Button
                    type="primary"
                    htmlType="submit"
                    loading={loading}
                    disabled={!formValidation.isValid}
                    size="large"
                    icon={<SafetyCertificateOutlined />}
                  >
                    Create Material Extension
                  </Button>
                </Space>
              </Col>
            </Row>
          </Form.Item>
        </Card>
      </Form>
    </Card>
  );
};

export default MaterialExtensionForm;