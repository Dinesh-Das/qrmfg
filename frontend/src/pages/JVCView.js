import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Button, 
  Typography, 
  Row, 
  Col, 
  message, 
  Tag, 
  Space,
  Divider,
  Tabs,
  Statistic,
  Alert,
  Table
} from 'antd';
import { 
  PlusOutlined, 
  ClockCircleOutlined,
  CheckCircleOutlined,
  MessageOutlined,
  ExclamationCircleOutlined,
  ReloadOutlined,
  HistoryOutlined
} from '@ant-design/icons';
import { workflowAPI } from '../services/workflowAPI';
import { documentAPI } from '../services/documentAPI';
import QueryInbox from '../components/workflow/QueryInbox';
import QueryHistoryTracker from '../components/workflow/QueryHistoryTracker';
import MaterialExtensionForm from '../components/workflow/MaterialExtensionForm';
import PendingExtensionsList from '../components/workflow/PendingExtensionsList';

const { Title, Text } = Typography;
const { TabPane } = Tabs;

const JVCView = () => {
  const [loading, setLoading] = useState(false);
  const [completedWorkflows, setCompletedWorkflows] = useState([]);

  const [activeTab, setActiveTab] = useState('initiate');
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [queryStats, setQueryStats] = useState({
    totalQueries: 0,
    openQueries: 0,
    resolvedToday: 0,
    overdueQueries: 0,
    avgResolutionTime: 0,
    highPriorityQueries: 0
  });

  useEffect(() => {
    loadCompletedWorkflows();
    loadQueryStats();
  }, []);

  const loadCompletedWorkflows = async () => {
    try {
      setLoading(true);
      // Load completed workflows
      const completed = await workflowAPI.getWorkflowsByState('COMPLETED');
      setCompletedWorkflows(completed || []);
    } catch (error) {
      console.error('Error loading completed workflows:', error);
      message.error('Failed to load completed workflows');
    } finally {
      setLoading(false);
    }
  };

  const loadQueryStats = async () => {
    try {
      const [openCount, resolvedToday, overdueQueries, avgTime, highPriorityQueries] = await Promise.all([
        fetch('/qrmfg/api/v1/queries/stats/count-open/JVC').then(r => r.json()),
        fetch('/qrmfg/api/v1/queries/stats/resolved-today').then(r => r.json()),
        fetch('/qrmfg/api/v1/queries/overdue').then(r => r.json()).then(data => 
          data.filter(q => q.assignedTeam === 'JVC').length
        ),
        fetch('/qrmfg/api/v1/queries/stats/avg-resolution-time/JVC').then(r => r.json()),
        fetch('/qrmfg/api/v1/queries/high-priority').then(r => r.json()).then(data =>
          data.filter(q => q.assignedTeam === 'JVC').length
        )
      ]);

      setQueryStats({
        totalQueries: openCount + resolvedToday,
        openQueries: openCount,
        resolvedToday: resolvedToday,
        overdueQueries: overdueQueries,
        avgResolutionTime: avgTime,
        highPriorityQueries: highPriorityQueries
      });
    } catch (error) {
      console.error('Failed to load query stats:', error);
    }
  };

  const handleInitiateWorkflow = async (formData) => {
    try {
      setLoading(true);
      
      // Create the workflow first
      const workflowData = {
        projectCode: formData.projectCode,
        materialCode: formData.materialCode,
        plantCode: formData.plantCode,
        blockId: formData.blockId,
        initiatedBy: 'current-user' // In real app, get from auth context
      };

      const createdWorkflow = await workflowAPI.createWorkflow(workflowData);
      
      // Upload new documents if any
      if (formData.uploadedFiles && formData.uploadedFiles.length > 0) {
        const files = formData.uploadedFiles.map(file => file.originFileObj || file);
        await documentAPI.uploadDocuments(files, formData.projectCode, formData.materialCode, createdWorkflow.id);
      }
      
      // Reuse existing documents if any selected
      if (formData.reusedDocuments && formData.reusedDocuments.length > 0) {
        await documentAPI.reuseDocuments(formData.reusedDocuments, createdWorkflow.id);
      }
      
      message.success('Material workflow initiated successfully');
      setRefreshTrigger(prev => prev + 1);
      setActiveTab('pending');
    } catch (error) {
      console.error('Error initiating workflow:', error);
      message.error('Failed to initiate workflow');
    } finally {
      setLoading(false);
    }
  };

  const handleExtendToPlant = async (workflow) => {
    try {
      setLoading(true);
      
      await workflowAPI.extendWorkflow(workflow.id, {
        plantCode: workflow.plantCode,
        comment: 'Extended to plant for questionnaire completion'
      });
      
      message.success(`Workflow extended to plant ${workflow.plantCode}`);
      setRefreshTrigger(prev => prev + 1);
    } catch (error) {
      console.error('Error extending workflow:', error);
      message.error('Failed to extend workflow to plant');
      throw error; // Re-throw to let PendingExtensionsList handle it
    } finally {
      setLoading(false);
    }
  };

  const refreshData = () => {
    loadCompletedWorkflows();
    loadQueryStats();
    setRefreshTrigger(prev => prev + 1);
  };



  const completedColumns = [
    {
      title: 'Project Code',
      dataIndex: 'projectCode',
      key: 'projectCode',
      render: (text) => <Text strong>{text}</Text>
    },
    {
      title: 'Material Code',
      dataIndex: 'materialCode',
      key: 'materialCode',
      render: (text) => <Text code>{text}</Text>
    },
    {
      title: 'Plant Code',
      dataIndex: 'plantCode',
      key: 'plantCode'
    },
    {
      title: 'Block ID',
      dataIndex: 'blockId',
      key: 'blockId'
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
      
      <Tabs 
        activeKey={activeTab} 
        onChange={setActiveTab}
        tabBarExtraContent={
          <Space>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={refreshData}
              size="small"
            >
              Refresh
            </Button>
          </Space>
        }
      >
        <TabPane tab="Initiate Workflow" key="initiate" icon={<PlusOutlined />}>
          <Row gutter={24}>
            <Col span={18}>
              <MaterialExtensionForm 
                onSubmit={handleInitiateWorkflow}
                loading={loading}
              />
            </Col>
            
            <Col span={6}>
              <Card title="Quick Stats">
                <div style={{ textAlign: 'center' }}>
                  <div style={{ marginBottom: 16 }}>
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

        <TabPane tab="Pending Extensions" key="pending" icon={<ClockCircleOutlined />}>
          <PendingExtensionsList 
            onExtendToPlant={handleExtendToPlant}
            refreshTrigger={refreshTrigger}
          />
        </TabPane>

        <TabPane tab={`Completed (${completedWorkflows.length})`} key="completed" icon={<CheckCircleOutlined />}>
          <Card title="Completed Workflows">
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

        {/* Enhanced Query Management Tabs */}
        <TabPane 
          tab={
            <Space>
              <MessageOutlined />
              <span>Query Inbox</span>
              {queryStats.openQueries > 0 && (
                <span style={{ 
                  background: '#ff4d4f', 
                  color: 'white', 
                  borderRadius: '10px', 
                  padding: '2px 6px', 
                  fontSize: '12px' 
                }}>
                  {queryStats.openQueries}
                </span>
              )}
            </Space>
          } 
          key="queries"
        >
          {/* Query Stats */}
          <Row gutter={16} style={{ marginBottom: 16 }}>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Open Queries"
                  value={queryStats.openQueries}
                  prefix={<MessageOutlined />}
                  valueStyle={{ color: '#cf1322' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Resolved Today"
                  value={queryStats.resolvedToday}
                  prefix={<CheckCircleOutlined />}
                  valueStyle={{ color: '#3f8600' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Overdue"
                  value={queryStats.overdueQueries}
                  prefix={<ExclamationCircleOutlined />}
                  valueStyle={{ color: queryStats.overdueQueries > 0 ? '#cf1322' : '#3f8600' }}
                />
              </Card>
            </Col>
            <Col span={6}>
              <Card>
                <Statistic
                  title="Avg Resolution"
                  value={queryStats.avgResolutionTime}
                  precision={1}
                  suffix="hrs"
                  prefix={<ClockCircleOutlined />}
                />
              </Card>
            </Col>
          </Row>

          {/* Alerts for urgent items */}
          {queryStats.overdueQueries > 0 && (
            <Alert
              message={`${queryStats.overdueQueries} queries are overdue (>3 days)`}
              description="These queries require immediate attention to maintain SLA compliance."
              type="error"
              showIcon
              style={{ marginBottom: 16 }}
              action={
                <Button size="small" danger>
                  View Overdue
                </Button>
              }
            />
          )}

          {queryStats.highPriorityQueries > 0 && (
            <Alert
              message={`${queryStats.highPriorityQueries} high priority queries pending`}
              description="These queries have been marked as high priority and need urgent resolution."
              type="warning"
              showIcon
              style={{ marginBottom: 16 }}
              action={
                <Button size="small" type="primary">
                  View High Priority
                </Button>
              }
            />
          )}

          <QueryInbox team="JVC" userRole="JVC_USER" />
        </TabPane>

        <TabPane 
          tab={
            <Space>
              <HistoryOutlined />
              <span>Query History</span>
            </Space>
          } 
          key="history"
        >
          <QueryHistoryTracker />
        </TabPane>
      </Tabs>


    </div>
  );
};

export default JVCView; 