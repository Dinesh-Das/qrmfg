import React, { useState, useEffect } from 'react';
import {
  Typography,
  Table,
  Tag,
  Button,
  message,
  Space,
  Badge,
  Dropdown,
  Modal,
  Descriptions,
  Card,
  Row,
  Col
} from 'antd';
import {
  ReloadOutlined,
  EyeOutlined,
  SendOutlined,
  MoreOutlined,
  FileTextOutlined,
  InfoCircleOutlined
} from '@ant-design/icons';
import { apiRequest } from '../api/api';

const { Title } = Typography;

const PendingTasks = () => {
  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [detailsModalVisible, setDetailsModalVisible] = useState(false);

  const fetchWorkflows = async () => {
    setLoading(true);
    try {
      const response = await apiRequest('/workflows/pending');
      setWorkflows(response);
    } catch (error) {
      message.error('Failed to fetch pending workflows');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWorkflows();
  }, []);

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
      render: (id) => <Badge count={id} style={{ backgroundColor: '#1890ff' }} />
    },
    {
      title: 'Material Info',
      key: 'materialInfo',
      width: 200,
      render: (_, record) => (
        <div>
          <div style={{ fontWeight: 'bold', marginBottom: 4 }}>
            {record.materialCode}
          </div>
          <div style={{ fontSize: '12px', color: '#666', marginBottom: 2 }}>
            {record.materialName || 'No material name'}
          </div>
          <div style={{ fontSize: '11px', color: '#999' }}>
            Project: {record.projectCode}
          </div>
        </div>
      ),
    },
    {
      title: 'Block ID',
      dataIndex: 'blockId',
      key: 'blockId',
      width: 100,
      render: (blockId) => (
        <Tag color="blue" style={{ fontFamily: 'monospace' }}>
          {blockId}
        </Tag>
      ),
    },
    {
      title: 'Plant',
      dataIndex: 'assignedPlant',
      key: 'assignedPlant',
      width: 100,
      render: (plant) => (
        <Tag color="green">{plant}</Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'state',
      key: 'state',
      width: 120,
      render: (state) => {
        const stateConfig = {
          'JVC_PENDING': { color: 'orange', text: 'JVC Pending' },
          'PLANT_PENDING': { color: 'blue', text: 'Plant Pending' },
          'CQS_PENDING': { color: 'purple', text: 'CQS Pending' },
          'TECH_PENDING': { color: 'cyan', text: 'Tech Pending' },
          'COMPLETED': { color: 'green', text: 'Completed' }
        };
        const config = stateConfig[state] || { color: 'default', text: state };
        return <Tag color={config.color}>{config.text}</Tag>;
      },
    },
    {
      title: 'Days Pending',
      key: 'daysPending',
      width: 100,
      render: (_, record) => {
        const days = record.daysPending || 0;
        return (
          <Tag color={days > 7 ? 'red' : days > 3 ? 'orange' : 'green'}>
            {days} days
          </Tag>
        );
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_, record) => {
        const menuItems = [
          {
            key: 'view',
            icon: <EyeOutlined />,
            label: 'View Details',
            onClick: () => showWorkflowDetails(record)
          },
          {
            key: 'questionnaire',
            icon: <FileTextOutlined />,
            label: 'Open Questionnaire',
            onClick: () => openQuestionnaire(record.id)
          }
        ];

        if (record.state === 'JVC_PENDING') {
          menuItems.push({
            key: 'extend',
            icon: <SendOutlined />,
            label: 'Extend to Plant',
            onClick: () => handleWorkflowAction(record.id)
          });
        }

        return (
          <Space>
            <Button
              size="small"
              icon={<FileTextOutlined />}
              onClick={() => openQuestionnaire(record.id)}
            >
              Questionnaire
            </Button>
            <Dropdown
              menu={{ items: menuItems }}
              trigger={['click']}
            >
              <Button size="small" icon={<MoreOutlined />} />
            </Dropdown>
          </Space>
        );
      },
    },
  ];

  const handleWorkflowAction = async (workflowId) => {
    try {
      await apiRequest(`/workflows/${workflowId}/extend`, { method: 'PUT' });
      message.success('Workflow extended successfully');
      fetchWorkflows();
    } catch (error) {
      message.error('Failed to extend workflow');
    }
  };

  const showWorkflowDetails = (workflow) => {
    setSelectedWorkflow(workflow);
    setDetailsModalVisible(true);
  };

  const openQuestionnaire = (workflowId) => {
    // Navigate to questionnaire page
    window.open(`/questionnaire/${workflowId}`, '_blank');
  };

  const closeDetailsModal = () => {
    setDetailsModalVisible(false);
    setSelectedWorkflow(null);
  };

  return (
    <div style={{ padding: '24px' }}>
      {/* Header Section */}
      <div style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Title level={2} style={{ margin: 0 }}>
            Pending Workflows
            <Badge
              count={workflows.length}
              style={{ backgroundColor: '#1890ff', marginLeft: 12 }}
            />
          </Title>
          <Space>
            <Button
              icon={<ReloadOutlined />}
              onClick={fetchWorkflows}
              loading={loading}
            >
              Refresh
            </Button>
          </Space>
        </div>

        {/* Summary Cards */}
        <Row gutter={16} style={{ marginBottom: 16 }}>
          <Col span={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#1890ff' }}>
                  {workflows.length}
                </div>
                <div style={{ color: '#666' }}>Total Workflows</div>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#52c41a' }}>
                  {workflows.filter(w => w.state === 'COMPLETED').length}
                </div>
                <div style={{ color: '#666' }}>Completed</div>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#faad14' }}>
                  {workflows.filter(w => w.state === 'JVC_PENDING').length}
                </div>
                <div style={{ color: '#666' }}>JVC Pending</div>
              </div>
            </Card>
          </Col>
          <Col span={6}>
            <Card size="small">
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: '#ff4d4f' }}>
                  {workflows.filter(w => (w.daysPending || 0) > 7).length}
                </div>
                <div style={{ color: '#666' }}>Overdue (&gt; 7 days)</div>
              </div>
            </Card>
          </Col>
        </Row>
      </div>

      {/* Main Table */}
      <Card>
        <Table
          columns={columns}
          dataSource={workflows}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 15,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => `${range[0]}-${range[1]} of ${total} workflows`
          }}
          scroll={{ x: 1200 }}
          size="small"
        />
      </Card>

      {/* Workflow Details Modal */}
      <Modal
        title={
          <Space>
            <InfoCircleOutlined />
            Workflow Details
            {selectedWorkflow && <Badge count={selectedWorkflow.id} style={{ backgroundColor: '#1890ff' }} />}
          </Space>
        }
        open={detailsModalVisible}
        onCancel={closeDetailsModal}
        footer={[
          <Button key="close" onClick={closeDetailsModal}>
            Close
          </Button>,
          selectedWorkflow && (
            <Button
              key="questionnaire"
              type="primary"
              icon={<FileTextOutlined />}
              onClick={() => {
                openQuestionnaire(selectedWorkflow.id);
                closeDetailsModal();
              }}
            >
              Open Questionnaire
            </Button>
          )
        ]}
        width={800}
      >
        {selectedWorkflow && (
          <Descriptions column={2} bordered size="small">
            <Descriptions.Item label="Workflow ID">{selectedWorkflow.id}</Descriptions.Item>
            <Descriptions.Item label="Status">
              <Tag color="blue">{selectedWorkflow.state}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Material Code">{selectedWorkflow.materialCode}</Descriptions.Item>
            <Descriptions.Item label="Material Name">{selectedWorkflow.materialName || 'Not available'}</Descriptions.Item>
            <Descriptions.Item label="Project Code">{selectedWorkflow.projectCode}</Descriptions.Item>
            <Descriptions.Item label="Block ID">
              <Tag color="blue">{selectedWorkflow.blockId}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Assigned Plant">
              <Tag color="green">{selectedWorkflow.assignedPlant}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Days Pending">
              <Tag color={(selectedWorkflow.daysPending || 0) > 7 ? 'red' : 'green'}>
                {selectedWorkflow.daysPending || 0} days
              </Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Initiated By">{selectedWorkflow.initiatedBy}</Descriptions.Item>
            <Descriptions.Item label="Created At">
              {selectedWorkflow.createdAt ? new Date(selectedWorkflow.createdAt).toLocaleString() : 'N/A'}
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  );
};

export default PendingTasks;