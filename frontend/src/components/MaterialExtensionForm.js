import React, { useState, useEffect } from 'react';
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
  List,
  Checkbox,
  Spin,
  Badge,
  Descriptions,
  Modal,
  Result,
  Tag
} from 'antd';
import {
  UploadOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  SafetyCertificateOutlined,
  ProjectOutlined,
  ExperimentOutlined,
  HomeOutlined,
  BlockOutlined,
  FileProtectOutlined,
  RocketOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  DownloadOutlined
} from '@ant-design/icons';
import { projectAPI } from '../services/projectAPI';
import { documentAPI } from '../services/documentAPI';

const { Option } = Select;
const { Title, Text } = Typography;

const MaterialExtensionFormSimple = ({ onSubmit, loading }) => {
  const [form] = Form.useForm();
  const [projects, setProjects] = useState([]);
  const [materials, setMaterials] = useState([]);
  const [plants, setPlants] = useState([]);
  const [blocks, setBlocks] = useState([]);
  const [fileList, setFileList] = useState([]);
  const [reusableDocuments, setReusableDocuments] = useState([]);
  const [selectedReusableDocuments, setSelectedReusableDocuments] = useState([]);
  const [showReusableDocuments, setShowReusableDocuments] = useState(false);
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

      if (projectCode) {
        const materialData = await projectAPI.getMaterialsByProject(projectCode);
        setMaterials(materialData || []);

        if (materialData && materialData.length > 0) {
          message.success(`Loaded ${materialData.length} materials for project ${projectCode}`);
        }
      }

      // Clear reusable documents when project changes
      setReusableDocuments([]);
      setSelectedReusableDocuments([]);
      setShowReusableDocuments(false);
    } catch (error) {
      console.error('Error loading materials:', error);
      message.error('Failed to load materials. Please try again.');
    } finally {
      setLoadingState('materials', false);
    }
  };

  const handlePlantChange = async (plantCode) => {
    try {
      setLoadingState('blocks', true);
      setBlocks([]);

      if (plantCode) {
        const blockData = await projectAPI.getBlocksByPlant(plantCode);
        setBlocks(blockData || []);

        if (blockData && blockData.length > 0) {
          message.success(`Loaded ${blockData.length} blocks for plant ${plantCode}`);
        }
      }
    } catch (error) {
      console.error('Error loading blocks:', error);
      message.error('Failed to load blocks. Please try again.');
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
      const reusableDocs = await documentAPI.getReusableDocuments(projectCode, materialCode, true);

      if (reusableDocs && reusableDocs.length > 0) {
        setReusableDocuments(reusableDocs);
        setShowReusableDocuments(true);
        setSelectedReusableDocuments(reusableDocs.map(doc => doc.id));

        message.success(`Found ${reusableDocs.length} reusable document(s) for ${projectCode}/${materialCode}`);
      } else {
        setReusableDocuments([]);
        setShowReusableDocuments(false);
        setSelectedReusableDocuments([]);
      }
    } catch (error) {
      console.error('Error checking for reusable documents:', error);
      message.warning('Unable to check for reusable documents. You can still upload new documents.');
    } finally {
      setLoadingState('reusableDocuments', false);
    }
  };

  const handleFileChange = ({ fileList: newFileList }) => {
    const validatedFileList = newFileList.map(file => {
      if (file.originFileObj) {
        const validation = documentAPI.validateFile(file.originFileObj);
        if (!validation.isValidType) {
          file.status = 'error';
          file.response = 'Invalid file type. Only PDF, Word, and Excel files are allowed.';
        } else if (!validation.isValidSize) {
          file.status = 'error';
          file.response = `File size exceeds 25MB limit (${(file.originFileObj.size / 1024 / 1024).toFixed(2)}MB).`;
        } else {
          file.status = 'done';
          file.percent = 100;
        }

        file.size = file.originFileObj.size;
        file.type = file.originFileObj.type;
        file.lastModified = file.originFileObj.lastModified;
      }
      return file;
    });

    setFileList(validatedFileList);

    const validFiles = validatedFileList.filter(f => f.status === 'done').length;
    const errorFiles = validatedFileList.filter(f => f.status === 'error').length;

    if (errorFiles > 0) {
      message.warning(`${errorFiles} file(s) have validation errors. Please check file types and sizes.`);
    } else if (validFiles > 0) {
      message.success(`${validFiles} file(s) ready for upload.`);
    }
  };

  const handleSubmit = async (values) => {
    try {
      // Basic validation
      const totalDocs = fileList.filter(f => f.status === 'done').length + selectedReusableDocuments.length;

      if (!values.projectCode || !values.materialCode || !values.plantCode || !values.blockId) {
        message.error('Please fill in all required fields');
        return;
      }

      if (totalDocs === 0) {
        message.error('Please upload at least one document or select reusable documents');
        return;
      }

      const selectedProject = projects.find(p => p.value === values.projectCode);
      const selectedMaterial = materials.find(m => m.value === values.materialCode);
      const selectedPlant = plants.find(p => p.value === values.plantCode);
      const selectedBlock = blocks.find(b => b.value === values.blockId);

      Modal.confirm({
        title: (
          <Space>
            <RocketOutlined style={{ color: '#1890ff' }} />
            <span>Confirm Material Extension Submission</span>
          </Space>
        ),
        width: 800,
        style: { top: 80 },
        bodyStyle: {
          minHeight: '450px',
          padding: '24px'
        },
        content: (
          <div>
            <Alert
              message="Ready to Submit Material Extension"
              description="Please review the details below before creating the workflow."
              type="info"
              showIcon
              style={{ marginBottom: 16 }}
            />

            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="Project Code" span={1}>
                <Space>
                  <ProjectOutlined style={{ color: '#1890ff' }} />
                  <Text strong>{values.projectCode}</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Material Code" span={1}>
                <Space>
                  <ExperimentOutlined style={{ color: '#52c41a' }} />
                  <Text code>{values.materialCode}</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Plant Code" span={1}>
                <Space>
                  <HomeOutlined style={{ color: '#fa8c16' }} />
                  <Text strong>{values.plantCode}</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Block ID" span={1}>
                <Space>
                  <BlockOutlined style={{ color: '#722ed1' }} />
                  <Text strong>{values.blockId}</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Documents Summary" span={2}>
                <Space>
                  <FileProtectOutlined style={{ color: '#13c2c2' }} />
                  <Badge count={totalDocs} style={{ backgroundColor: totalDocs > 0 ? '#52c41a' : '#d9d9d9' }} />
                  <Text style={{ marginLeft: 8 }}>
                    {totalDocs} file{totalDocs !== 1 ? 's' : ''} ({fileList.filter(f => f.status === 'done').length} new, {selectedReusableDocuments.length} reused)
                  </Text>
                </Space>
              </Descriptions.Item>
            </Descriptions>

            {/* Document Details Section */}
            {totalDocs > 0 && (
              <div style={{ marginTop: 20 }}>
                {fileList.filter(f => f.status === 'done').length > 0 && (
                  <div style={{ marginBottom: 16 }}>
                    <Text strong style={{ color: '#1890ff', marginBottom: 8, display: 'block' }}>
                      📎 New Documents ({fileList.filter(f => f.status === 'done').length})
                    </Text>
                    <List
                      size="small"
                      bordered
                      dataSource={fileList.filter(f => f.status === 'done')}
                      renderItem={(file) => (
                        <List.Item
                          actions={[
                            <Button
                              type="link"
                              size="small"
                              icon={<DownloadOutlined />}
                              onClick={() => {
                                // Create a temporary URL for downloading the file
                                const url = URL.createObjectURL(file.originFileObj || file);
                                const link = document.createElement('a');
                                link.href = url;
                                link.download = file.name;
                                document.body.appendChild(link);
                                link.click();
                                document.body.removeChild(link);
                                URL.revokeObjectURL(url);
                              }}
                            >
                              Download
                            </Button>
                          ]}
                        >
                          <List.Item.Meta
                            avatar={<FileTextOutlined style={{ color: '#52c41a' }} />}
                            title={file.name}
                            description={`Size: ${(file.size / 1024 / 1024).toFixed(2)} MB`}
                          />
                        </List.Item>
                      )}
                    />
                  </div>
                )}

                {selectedReusableDocuments.length > 0 && (
                  <div>
                    <Text strong style={{ color: '#fa8c16', marginBottom: 8, display: 'block' }}>
                      🔄 Reused Documents ({selectedReusableDocuments.length})
                    </Text>
                    <List
                      size="small"
                      bordered
                      dataSource={reusableDocuments.filter(doc => selectedReusableDocuments.includes(doc.id))}
                      renderItem={(doc) => (
                        <List.Item
                          actions={[
                            <Button
                              type="link"
                              size="small"
                              icon={<DownloadOutlined />}
                              onClick={async () => {
                                try {
                                  const blob = await documentAPI.downloadDocument(doc.id);
                                  const url = window.URL.createObjectURL(blob);
                                  const link = document.createElement('a');
                                  link.href = url;
                                  link.download = doc.originalFileName || `document_${doc.id}`;
                                  document.body.appendChild(link);
                                  link.click();
                                  document.body.removeChild(link);
                                  window.URL.revokeObjectURL(url);
                                  message.success(`Downloaded ${doc.originalFileName}`);
                                } catch (error) {
                                  console.error('Error downloading document:', error);
                                  message.error('Failed to download document');
                                }
                              }}
                            >
                              Download
                            </Button>
                          ]}
                        >
                          <List.Item.Meta
                            avatar={<FileTextOutlined style={{ color: '#fa8c16' }} />}
                            title={doc.originalFileName}
                            description={
                              <Space split={<span style={{ color: '#d9d9d9' }}>|</span>}>
                                <Text type="secondary">Size: {(doc.fileSize / 1024 / 1024).toFixed(2)} MB</Text>
                                <Text type="secondary">From: {doc.projectCode}/{doc.materialCode}</Text>
                                <Tag color="orange" size="small">Reused</Tag>
                              </Space>
                            }
                          />
                        </List.Item>
                      )}
                    />
                  </div>
                )}
              </div>
            )}
          </div>
        ),
        onOk: () => {
          return new Promise(async (resolve, reject) => {
            const submissionData = {
              ...values,
              uploadedFiles: fileList.filter(file => file.status === 'done'),
              reusedDocuments: selectedReusableDocuments,
              metadata: {
                totalDocuments: totalDocs,
                newDocuments: fileList.filter(f => f.status === 'done').length,
                reusedDocuments: selectedReusableDocuments.length,
                submittedAt: new Date().toISOString(),
                formVersion: '3.0-simplified'
              }
            };

            try {
              const result = await onSubmit(submissionData);

              // Check if it's a duplicate workflow
              if (result && result.isDuplicate) {
                const existingWorkflow = result.existingWorkflow;

                // Show enhanced duplicate workflow modal popup
                Modal.warning({
                  title: (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <ExclamationCircleOutlined style={{ color: '#faad14', fontSize: '20px' }} />
                      <span>Duplicate Workflow Detected</span>
                    </div>
                  ),
                  width: 800,
                  style: { top: 100 },
                  bodyStyle: {
                    minHeight: '400px',
                    padding: '24px'
                  },
                  content: (
                    <div style={{ padding: '16px 0' }}>
                      <Alert
                        message="Workflow Already Exists"
                        description="A workflow with the same parameters is already in the system."
                        type="warning"
                        showIcon
                        style={{ marginBottom: 20 }}
                      />

                      <Descriptions
                        title="Submitted Parameters"
                        bordered
                        size="small"
                        column={2}
                        style={{ marginBottom: 20 }}
                      >
                        <Descriptions.Item label="Project Code" span={1}>
                          <Tag color="blue">{values.projectCode}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="Material Code" span={1}>
                          <Tag color="green">{values.materialCode}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="Plant Code" span={1}>
                          <Tag color="orange">{values.plantCode}</Tag>
                        </Descriptions.Item>
                        <Descriptions.Item label="Block ID" span={1}>
                          <Tag color="purple">{values.blockId}</Tag>
                        </Descriptions.Item>
                      </Descriptions>

                      {existingWorkflow && (
                        <Descriptions
                          title="Existing Workflow Details"
                          bordered
                          size="small"
                          column={2}
                          style={{ marginBottom: 20 }}
                        >
                          <Descriptions.Item label="Workflow ID" span={1}>
                            <Tag color="red">#{existingWorkflow.id}</Tag>
                          </Descriptions.Item>
                          <Descriptions.Item label="Current State" span={1}>
                            <Tag color="processing">
                              {existingWorkflow.state?.replace('_', ' ') || 'PENDING'}
                            </Tag>
                          </Descriptions.Item>
                          <Descriptions.Item label="Created Date" span={1}>
                            {existingWorkflow.createdAt ?
                              new Date(existingWorkflow.createdAt).toLocaleDateString('en-US', {
                                year: 'numeric',
                                month: 'short',
                                day: 'numeric',
                                hour: '2-digit',
                                minute: '2-digit'
                              }) : 'N/A'
                            }
                          </Descriptions.Item>
                          <Descriptions.Item label="Initiated By" span={1}>
                            {existingWorkflow.initiatedBy || 'Unknown'}
                          </Descriptions.Item>
                          {existingWorkflow.documentCount > 0 && (
                            <Descriptions.Item label="Documents" span={2}>
                              <Badge count={existingWorkflow.documentCount} style={{ backgroundColor: '#52c41a' }} />
                              <span style={{ marginLeft: 8 }}>files attached</span>
                            </Descriptions.Item>
                          )}
                        </Descriptions>
                      )}

                      <Alert
                        message="Next Steps"
                        description={
                          <div>
                            <p style={{ margin: '8px 0' }}>• Check the <strong>"Pending Extensions"</strong> tab to view the existing workflow</p>
                            <p style={{ margin: '8px 0' }}>• Use different parameters if you need to create a new workflow</p>
                            <p style={{ margin: '8px 0' }}>• Contact the workflow initiator if you need to modify the existing workflow</p>
                          </div>
                        }
                        type="info"
                        showIcon
                      />
                    </div>
                  ),
                  okText: 'Got It',
                  okButtonProps: {
                    size: 'large',
                    type: 'primary'
                  },
                  onOk: () => {
                    // Reset form after user acknowledges
                    resetForm();
                  }
                });
                // Close the confirmation modal
                resolve();
                return;
              }

              // Only show success modal if onSubmit completed without throwing an error
              Modal.success({
                title: 'Material Extension Created Successfully!',
                content: (
                  <Result
                    status="success"
                    title="Workflow Initiated"
                    subTitle={`Material extension for ${values.projectCode}/${values.materialCode} has been successfully assigned to plant ${values.plantCode} (Block: ${values.blockId})`}
                  />
                )
              });

              resetForm();
              resolve();
            } catch (error) {
              // Don't show success modal if onSubmit failed
              console.error('Form submission failed:', error);
              // For other errors, show error message and keep modal open
              message.error('Failed to create material extension. Please try again.');
              reject(error);
            }
          });
        }
      });
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
  };

  return (
    <Card>
      <div style={{ marginBottom: 24 }}>
        <Space>
          <SafetyCertificateOutlined />
          <Title level={4} style={{ margin: 0 }}>Material Extension Form</Title>
        </Space>
      </div>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        size="large"
      >
        <Row gutter={[16, 16]}>
          {/* Project Selection */}
          <Col xs={24} sm={12}>
            <Form.Item
              label="Project Code"
              name="projectCode"
              rules={[{ required: true, message: 'Please select a project code' }]}
            >
              <Select
                placeholder="Select project code"
                showSearch
                loading={loadingStates.projects}
                onChange={handleProjectChange}
                filterOption={(input, option) =>
                  option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
              >
                {projects.map(project => (
                  <Option key={project.value} value={project.value}>
                    {project.label || project.value}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>

          {/* Material Selection */}
          <Col xs={24} sm={12}>
            <Form.Item
              label="Material Code"
              name="materialCode"
              rules={[{ required: true, message: 'Please select a material code' }]}
            >
              <Select
                placeholder="Select material code"
                showSearch
                loading={loadingStates.materials}
                onChange={handleMaterialChange}
                disabled={materials.length === 0}
                filterOption={(input, option) =>
                  option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
              >
                {materials.map(material => (
                  <Option key={material.value} value={material.value}>
                    {material.label || material.value}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>

          {/* Plant Selection */}
          <Col xs={24} sm={12}>
            <Form.Item
              label="Plant Code"
              name="plantCode"
              rules={[{ required: true, message: 'Please select a plant code' }]}
            >
              <Select
                placeholder="Select plant code"
                showSearch
                loading={loadingStates.plants}
                onChange={handlePlantChange}
                filterOption={(input, option) =>
                  option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
              >
                {plants.map(plant => (
                  <Option key={plant.value} value={plant.value}>
                    {plant.label || plant.value}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>

          {/* Block Selection */}
          <Col xs={24} sm={12}>
            <Form.Item
              label="Block ID"
              name="blockId"
              rules={[{ required: true, message: 'Please select a block ID' }]}
            >
              <Select
                placeholder="Select block ID"
                showSearch
                loading={loadingStates.blocks}
                disabled={blocks.length === 0}
                filterOption={(input, option) =>
                  option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0
                }
              >
                {blocks.map(block => (
                  <Option key={block.value} value={block.value}>
                    {block.label || block.value}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
        </Row>

        {/* Reusable Documents Section */}
        {showReusableDocuments && (
          <Card
            title={
              <Space>
                <FileTextOutlined style={{ color: '#1890ff' }} />
                <span>Reusable Documents Found</span>
                <Badge count={reusableDocuments.length} style={{ backgroundColor: '#52c41a' }} />
              </Space>
            }
            style={{ marginBottom: 16 }}
            size="small"
          >
            <Alert
              message={`Found ${reusableDocuments.length} reusable document(s)`}
              description="These documents are automatically selected. Uncheck any you don't want to reuse."
              type="success"
              showIcon
              style={{ marginBottom: 16 }}
            />
            <Checkbox.Group
              value={selectedReusableDocuments}
              onChange={setSelectedReusableDocuments}
              style={{ width: '100%' }}
            >
              <List
                size="small"
                dataSource={reusableDocuments}
                renderItem={doc => (
                  <List.Item>
                    <Checkbox value={doc.id}>
                      <Space>
                        <FileTextOutlined style={{ color: '#1890ff' }} />
                        <span>{doc.originalFileName}</span>
                        <Text type="secondary">
                          ({(doc.fileSize / 1024 / 1024).toFixed(2)}MB)
                        </Text>
                      </Space>
                    </Checkbox>
                  </List.Item>
                )}
              />
            </Checkbox.Group>
          </Card>
        )}

        {/* File Upload Section */}
        <Form.Item
          label="Upload Documents"
          help="Upload PDF, Word, or Excel files (max 25MB each). At least one document is required."
        >
          <Upload.Dragger
            multiple
            fileList={fileList}
            onChange={handleFileChange}
            beforeUpload={() => false} // Prevent auto upload
            accept=".pdf,.doc,.docx,.xls,.xlsx"
          >
            <p className="ant-upload-drag-icon">
              <UploadOutlined />
            </p>
            <p className="ant-upload-text">Click or drag files to this area to upload</p>
            <p className="ant-upload-hint">
              Support for PDF, Word, and Excel files. Maximum file size: 25MB
            </p>
          </Upload.Dragger>
        </Form.Item>

        {/* Submit Button */}
        <Form.Item style={{ marginTop: 24 }}>
          <Space>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              size="large"
              icon={<RocketOutlined />}
            >
              Create Material Extension
            </Button>
            <Button onClick={resetForm} size="large">
              Reset Form
            </Button>
          </Space>
        </Form.Item>
      </Form>
    </Card>
  );
};

export default MaterialExtensionFormSimple;