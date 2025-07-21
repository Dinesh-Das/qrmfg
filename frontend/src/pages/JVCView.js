import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Form, 
  Input, 
  Select, 
  Upload, 
  Button, 
  Table, 
  Typography, 
  Row, 
  Col, 
  message, 
  Modal, 
  Tag, 
  Space,
  Divider,
  Tabs
} from 'antd';
import { 
  UploadOutlined, 
  PlusOutlined, 
  SendOutlined, 
  FileTextOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined
} from '@ant-design/icons';
import { workflowAPI } from '../services/workflowAPI';

const { Title, Text } = Typography;
const { Option } = Select;
const { TabPane } = Tabs;

const JVCView = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [pendingWorkflows, setPendingWorkflows] = useState([]);
  const [completedWorkflows, setCompletedWorkflows] = useState([]);
  const [fileList, setFileList] = useState([]);
  const [extendModalVisible, setExtendModalVisible] = useState(false);
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [activeTab, setActiveTab] = useState('initiate');

  // Plant options - in a real app, this would come from an API
  const plantOptions = [
    { value: 'plant-a', label: 'Plant A - Manufacturing' },
    { value: 'plant-b', label: 'Plant B - Assembly' },
    { value: 'plant-c', label: 'Plant C - Packaging' },
    { value: 'plant-d', label: 'Plant D - Quality Control' },
  ];

  useEffect(() => {
    loadWorkflows();
  }, []);

  const loadWorkflows = async () => {
    try {
      setLoading(true);
      // Load pending workflows (JVC_PENDING state)
      const pending = await workflowAPI.getWorkflowsByState('JVC_PENDING');
      setPendingWorkflows(pending || []);
      
      // Load completed workflows
      const completed = await workflowAPI.getWorkflowsByState('COMPLETED');
      setCompletedWorkflows(completed || []);
    } catch (error) {
      console.error('Error loading workflows:', error);
      message.error('Failed to load workflows');
    } finally {
      setLoading(false);
    }
  };

  const handleInitiateWorkflow = async (values) => {
    try {
      setLoading(true);
      
      // Prepare workflow data
      const workflowData = {
        materialId: values.materialId,
        assignedPlant: values.plantSelection,
        initiatedBy: 'current-user', // In real app, get from auth context
        documents: fileList.map(file => ({
          name: file.name,
          url: file.response?.url || file.url,
          type: file.type
        }))
      };

      await workflowAPI.createWorkflow(workflowData);
      
      message.success('Material workflow initiated successfully');
      form.resetFields();
      setFileList([]);
      loadWorkflows();
      setActiveTab('pending');
    } catch (error) {
      console.error('Error initiating workflow:', error);
      message.error('Failed to initiate workflow');
    } finally {
      setLoading(false);
    }
  };

  const handleExtendToPlant = async (workflow) => {
    setSelectedWorkflow(workflow);
    setExtendModalVisible(true);
  };

  const confirmExtendToPlant = async () => {
    try {
      setLoading(true);
      
      await workflowAPI.extendWorkflow(selectedWorkflow.id, {
        assignedPlant: selectedWorkflow.assignedPlant,
        comment: 'Extended to plant for questionnaire completion'
      });
      
      message.success(`Workflow extended to ${selectedWorkflow.assignedPlant}`);
      setExtendModalVisible(false);
      setSelectedWorkflow(null);
      loadWorkflows();
    } catch (error) {
      console.error('Error extending workflow:', error);
      message.error('Failed to extend workflow to plant');
    } finally {
      setLoading(false);
    }
  };

  const handleFileChange = ({ fileList: newFileList }) => {
    setFileList(newFileList);
  };

  const uploadProps = {
    name: 'file',
    action: '/api/upload/documents',
    headers: {
      authorization: `Bearer ${localStorage.getItem('authToken')}`,
    },
    fileList,
    onChange: handleFileChange,
    beforeUpload: (file) => {
      const isValidType = file.type === 'application/pdf' || 
                         file.type.startsWith('image/') ||
                         file.type === 'application/msword' ||
                         file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
      
      if (!isValidType) {
        message.error('You can only upload PDF, Word, or image files!');
        return false;
      }
      
      const isLt10M = file.size / 1024 / 1024 < 10;
      if (!isLt10M) {
        message.error('File must be smaller than 10MB!');
        return false;
      }
      
      return true;
    },
  };

  const pendingColumns = [
    {
      title: 'Material ID',
      dataIndex: 'materialId',
      key: 'materialId',
      render: (text) => <Text strong>{text}</Text>
    },
    {
      title: 'Assigned Plant',
      dataIndex: 'assignedPlant',
      key: 'assignedPlant',
      render: (plant) => {
        const plantOption = plantOptions.find(p => p.value === plant);
        return plantOption ? plantOption.label : plant;
      }
    },
    {
      title: 'Created',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date) => new Date(date).toLocaleDateString()
    },
    {
      title: 'Status',
      dataIndex: 'state',
      key: 'state',
      render: (state) => (
        <Tag color="orange" icon={<ClockCircleOutlined />}>
          {state === 'JVC_PENDING' ? 'Pending Action' : state}
        </Tag>
      )
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Button 
          type="primary" 
          icon={<SendOutlined />}
          onClick={() => handleExtendToPlant(record)}
          size="small"
        >
          Extend to Plant
        </Button>
      )
    }
  ];

  const completedColumns = [
    {
      title: 'Material ID',
      dataIndex: 'materialId',
      key: 'materialId',
      render: (text) => <Text strong>{text}</Text>
    },
    {
      title: 'Plant',
      dataIndex: 'assignedPlant',
      key: 'assignedPlant',
      render: (plant) => {
        const plantOption = plantOptions.find(p => p.value === plant);
        return plantOption ? plantOption.label : plant;
      }
    },
    {
      title: 'Completed',
      dataIndex: 'lastModified',
      key: 'lastModified',
      render: (date) => new Date(date).toLocaleDateString()
    },
    {
      title: 'Status',
      dataIndex: 'state',
      key: 'state',
      render: () => (
        <Tag color="green" icon={<CheckCircleOutlined />}>
          Completed
        </Tag>
      )
    }
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>JVC Dashboard</Title>
      <Text type="secondary">
        Initiate MSDS workflows and manage material safety documentation process
      </Text>
      
      <Divider />
      
      <Tabs activeKey={activeTab} onChange={setActiveTab}>
        <TabPane tab="Initiate Workflow" key="initiate" icon={<PlusOutlined />}>
          <Row gutter={24}>
            <Col span={16}>
              <Card title="Material Workflow Initiation" bordered={false}>
                <Form
                  form={form}
                  layout="vertical"
                  onFinish={handleInitiateWorkflow}
                  requiredMark={false}
                >
                  <Form.Item
                    label="Material ID"
                    name="materialId"
                    rules={[
                      { required: true, message: 'Please enter Material ID' },
                      { pattern: /^[A-Z0-9-]+$/, message: 'Material ID should contain only uppercase letters, numbers, and hyphens' }
                    ]}
                  >
                    <Input 
                      placeholder="Enter Material ID (e.g., MAT-001-2024)"
                      size="large"
                    />
                  </Form.Item>

                  <Form.Item
                    label="Plant Selection"
                    name="plantSelection"
                    rules={[{ required: true, message: 'Please select a plant' }]}
                  >
                    <Select 
                      placeholder="Select the plant for this material"
                      size="large"
                      showSearch
                      optionFilterProp="children"
                    >
                      {plantOptions.map(plant => (
                        <Option key={plant.value} value={plant.value}>
                          {plant.label}
                        </Option>
                      ))}
                    </Select>
                  </Form.Item>

                  <Form.Item
                    label="Safety Documents"
                    name="documents"
                    extra="Upload relevant safety documents (PDF, Word, or images). Maximum 10MB per file."
                  >
                    <Upload {...uploadProps} multiple>
                      <Button icon={<UploadOutlined />} size="large">
                        Upload Documents
                      </Button>
                    </Upload>
                  </Form.Item>

                  <Form.Item>
                    <Space>
                      <Button 
                        type="primary" 
                        htmlType="submit" 
                        loading={loading}
                        size="large"
                        icon={<FileTextOutlined />}
                      >
                        Initiate Workflow
                      </Button>
                      <Button 
                        onClick={() => {
                          form.resetFields();
                          setFileList([]);
                        }}
                        size="large"
                      >
                        Reset
                      </Button>
                    </Space>
                  </Form.Item>
                </Form>
              </Card>
            </Col>
            
            <Col span={8}>
              <Card title="Quick Stats" bordered={false}>
                <div style={{ textAlign: 'center' }}>
                  <div style={{ marginBottom: 16 }}>
                    <Text type="secondary">Pending Extensions</Text>
                    <div style={{ fontSize: 24, fontWeight: 'bold', color: '#fa8c16' }}>
                      {pendingWorkflows.length}
                    </div>
                  </div>
                  <div>
                    <Text type="secondary">Completed This Month</Text>
                    <div style={{ fontSize: 24, fontWeight: 'bold', color: '#52c41a' }}>
                      {completedWorkflows.length}
                    </div>
                  </div>
                </div>
              </Card>
            </Col>
          </Row>
        </TabPane>

        <TabPane tab={`Pending Extensions (${pendingWorkflows.length})`} key="pending" icon={<ClockCircleOutlined />}>
          <Card title="Pending Extensions" bordered={false}>
            <Table
              dataSource={pendingWorkflows}
              columns={pendingColumns}
              rowKey="id"
              loading={loading}
              pagination={{ pageSize: 10 }}
              locale={{
                emptyText: 'No pending extensions'
              }}
            />
          </Card>
        </TabPane>

        <TabPane tab={`Completed (${completedWorkflows.length})`} key="completed" icon={<CheckCircleOutlined />}>
          <Card title="Completed Workflows" bordered={false}>
            <Table
              dataSource={completedWorkflows}
              columns={completedColumns}
              rowKey="id"
              loading={loading}
              pagination={{ pageSize: 10 }}
              locale={{
                emptyText: 'No completed workflows'
              }}
            />
          </Card>
        </TabPane>
      </Tabs>

      {/* Extend to Plant Confirmation Modal */}
      <Modal
        title="Extend Workflow to Plant"
        open={extendModalVisible}
        onOk={confirmExtendToPlant}
        onCancel={() => {
          setExtendModalVisible(false);
          setSelectedWorkflow(null);
        }}
        confirmLoading={loading}
        okText="Extend to Plant"
        cancelText="Cancel"
      >
        {selectedWorkflow && (
          <div>
            <p>
              <strong>Material ID:</strong> {selectedWorkflow.materialId}
            </p>
            <p>
              <strong>Assigned Plant:</strong> {
                plantOptions.find(p => p.value === selectedWorkflow.assignedPlant)?.label || 
                selectedWorkflow.assignedPlant
              }
            </p>
            <p>
              Are you sure you want to extend this workflow to the plant team? 
              They will be notified and can begin the questionnaire process.
            </p>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default JVCView; 