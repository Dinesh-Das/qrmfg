import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Table, 
  Typography, 
  Button, 
  Space, 
  Tag, 
  Modal, 
  Row, 
  Col,
  Statistic,
  Alert,
  Spin,
  message
} from 'antd';
import {
  FormOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  EyeOutlined
} from '@ant-design/icons';
import { workflowAPI } from '../services/workflowAPI';
import { getCurrentUser } from '../services/auth';
import PlantQuestionnaire from '../components/PlantQuestionnaire';

const { Title } = Typography;

const PlantView = () => {
  const [loading, setLoading] = useState(true);
  const [workflows, setWorkflows] = useState([]);
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [questionnaireVisible, setQuestionnaireVisible] = useState(false);
  const [dashboardStats, setDashboardStats] = useState({});

  useEffect(() => {
    loadPlantWorkflows();
    loadDashboardStats();
  }, []);

  const loadPlantWorkflows = async () => {
    try {
      setLoading(true);
      const currentUser = getCurrentUser();
      const userWorkflows = await workflowAPI.getWorkflowsByUser(currentUser);
      
      // Filter for plant-pending workflows
      const plantWorkflows = userWorkflows.filter(w => 
        w.state === 'PLANT_PENDING' || w.assignedPlant === getCurrentPlant()
      );
      
      setWorkflows(plantWorkflows);
    } catch (error) {
      console.error('Failed to load plant workflows:', error);
      message.error('Failed to load workflows');
    } finally {
      setLoading(false);
    }
  };

  const loadDashboardStats = async () => {
    try {
      const stats = await workflowAPI.getDashboardSummary();
      setDashboardStats(stats);
    } catch (error) {
      console.error('Failed to load dashboard stats:', error);
    }
  };

  const getCurrentPlant = () => {
    // Get current user's plant from localStorage or user context
    return localStorage.getItem('userPlant') || 'Default Plant';
  };

  const handleStartQuestionnaire = (workflow) => {
    setSelectedWorkflow(workflow);
    setQuestionnaireVisible(true);
  };

  const handleQuestionnaireComplete = (formData) => {
    setQuestionnaireVisible(false);
    setSelectedWorkflow(null);
    loadPlantWorkflows(); // Refresh the list
    message.success('Questionnaire completed successfully!');
  };

  const handleSaveDraft = (formData) => {
    message.success('Draft saved successfully');
  };

  const getStateColor = (state) => {
    const colors = {
      'PLANT_PENDING': 'orange',
      'CQS_PENDING': 'purple',
      'TECH_PENDING': 'cyan',
      'COMPLETED': 'green'
    };
    return colors[state] || 'default';
  };

  const getDaysInState = (lastModified) => {
    if (!lastModified) return 0;
    const now = new Date();
    const modified = new Date(lastModified);
    const diffTime = Math.abs(now - modified);
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  const columns = [
    {
      title: 'Material Code',
      dataIndex: 'materialCode',
      key: 'materialCode',
      width: 120,
    },
    {
      title: 'Material Name',
      dataIndex: 'materialName',
      key: 'materialName',
      ellipsis: true,
    },
    {
      title: 'State',
      dataIndex: 'state',
      key: 'state',
      width: 120,
      render: (state) => (
        <Tag color={getStateColor(state)}>
          {state.replace('_', ' ')}
        </Tag>
      ),
    },
    {
      title: 'Days Pending',
      key: 'daysPending',
      width: 100,
      render: (_, record) => {
        const days = getDaysInState(record.lastModified);
        return (
          <span style={{ color: days > 3 ? '#ff4d4f' : 'inherit' }}>
            {days}
          </span>
        );
      },
    },
    {
      title: 'Open Queries',
      dataIndex: 'openQueries',
      key: 'openQueries',
      width: 100,
      render: (count) => (
        count > 0 ? (
          <Tag color="red">{count}</Tag>
        ) : (
          <Tag color="green">0</Tag>
        )
      ),
    },
    {
      title: 'Progress',
      key: 'progress',
      width: 100,
      render: (_, record) => {
        const completed = record.completedSteps || 0;
        const total = 6; // Total questionnaire steps
        const percentage = Math.round((completed / total) * 100);
        return `${percentage}%`;
      },
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button
            type="primary"
            size="small"
            icon={<FormOutlined />}
            onClick={() => handleStartQuestionnaire(record)}
            disabled={record.state !== 'PLANT_PENDING'}
          >
            {record.completedSteps > 0 ? 'Continue' : 'Start'}
          </Button>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => {
              // View workflow details
              console.log('View workflow:', record.id);
            }}
          >
            View
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Plant Dashboard</Title>
      
      {/* Dashboard Statistics */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={6} md={6} lg={6}>
          <Card>
            <Statistic
              title="Pending Tasks"
              value={workflows.filter(w => w.state === 'PLANT_PENDING').length}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6} md={6} lg={6}>
          <Card>
            <Statistic
              title="Overdue"
              value={workflows.filter(w => getDaysInState(w.lastModified) > 3).length}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6} md={6} lg={6}>
          <Card>
            <Statistic
              title="Open Queries"
              value={workflows.reduce((sum, w) => sum + (w.openQueries || 0), 0)}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col xs={12} sm={6} md={6} lg={6}>
          <Card>
            <Statistic
              title="Completed Today"
              value={dashboardStats.completedToday || 0}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Instructions */}
      <Alert
        message="Plant Questionnaire Instructions"
        description="Complete the multi-step questionnaire for each assigned material. You can save drafts and raise queries to other teams when needed. Focus on materials that are overdue (more than 3 days pending)."
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
      />

      {/* Workflows Table */}
      <Card title="Assigned Materials" extra={
        <Button onClick={loadPlantWorkflows} loading={loading}>
          Refresh
        </Button>
      }>
        <Table
          dataSource={workflows}
          columns={columns}
          loading={loading}
          rowKey="id"
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) => 
              `${range[0]}-${range[1]} of ${total} materials`
          }}
          rowClassName={(record) => {
            const days = getDaysInState(record.lastModified);
            return days > 3 ? 'overdue-row' : '';
          }}
        />
      </Card>

      {/* Questionnaire Modal */}
      <Modal
        title={`Material Questionnaire - ${selectedWorkflow?.materialCode}`}
        open={questionnaireVisible}
        onCancel={() => {
          setQuestionnaireVisible(false);
          setSelectedWorkflow(null);
        }}
        footer={null}
        width="95%"
        style={{ top: 20 }}
        destroyOnClose
      >
        {selectedWorkflow && (
          <PlantQuestionnaire
            workflowId={selectedWorkflow.id}
            onComplete={handleQuestionnaireComplete}
            onSaveDraft={handleSaveDraft}
          />
        )}
      </Modal>

      <style jsx>{`
        .overdue-row {
          background-color: #fff2f0;
        }
        .overdue-row:hover {
          background-color: #ffebe6 !important;
        }
      `}</style>
    </div>
  );
};

export default PlantView; 