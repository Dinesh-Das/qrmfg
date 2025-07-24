import React, { useState, useEffect } from 'react';
import { 
  Card, 
  Tabs, 
  Table, 
  Tag, 
  Button, 
  Space, 
  Modal, 
  Form, 
  Input, 
  Select, 
  DatePicker,
  Spin,
  Alert,
  Badge
} from 'antd';
import { 
  PlusOutlined, 
  SearchOutlined, 
  FilterOutlined,
  MessageOutlined,
  ClockCircleOutlined
} from '@ant-design/icons';
import { queryAPI } from '../services/queryAPI';

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

const { TabPane } = Tabs;
const { TextArea } = Input;
const { Option } = Select;

const QueryWidget = ({ workflowId, userRole }) => {
  const [loading, setLoading] = useState(false);
  const [queries, setQueries] = useState({
    all: [],
    open: [],
    resolved: [],
    myQueries: []
  });
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [resolveModalVisible, setResolveModalVisible] = useState(false);
  const [selectedQuery, setSelectedQuery] = useState(null);
  const [form] = Form.useForm();
  const [resolveForm] = Form.useForm();
  const { isMobile, isTablet } = useResponsive();

  useEffect(() => {
    if (workflowId) {
      loadQueries();
    }
  }, [workflowId]);

  const loadQueries = async () => {
    try {
      setLoading(true);
      const allQueries = await queryAPI.getQueriesByWorkflow(workflowId);
      
      setQueries({
        all: allQueries,
        open: allQueries.filter(q => q.status === 'OPEN'),
        resolved: allQueries.filter(q => q.status === 'RESOLVED'),
        myQueries: allQueries.filter(q => q.createdBy === getCurrentUser())
      });
    } catch (error) {
      console.error('Failed to load queries:', error);
    } finally {
      setLoading(false);
    }
  };

  const getCurrentUser = () => {
    // Get current user from auth context or localStorage
    return localStorage.getItem('username') || 'current_user';
  };

  const handleCreateQuery = async (values) => {
    try {
      await queryAPI.createQuery({
        workflowId,
        ...values,
        createdBy: getCurrentUser()
      });
      setCreateModalVisible(false);
      form.resetFields();
      loadQueries();
    } catch (error) {
      console.error('Failed to create query:', error);
    }
  };

  const handleResolveQuery = async (values) => {
    try {
      await queryAPI.resolveQuery(selectedQuery.id, {
        resolution: values.resolution,
        resolvedBy: getCurrentUser()
      });
      setResolveModalVisible(false);
      resolveForm.resetFields();
      setSelectedQuery(null);
      loadQueries();
    } catch (error) {
      console.error('Failed to resolve query:', error);
    }
  };

  const getStatusColor = (status) => {
    return status === 'OPEN' ? 'red' : 'green';
  };

  const getTeamColor = (team) => {
    const colors = {
      'CQS': 'blue',
      'TECH': 'purple',
      'PLANT': 'orange',
      'JVC': 'cyan'
    };
    return colors[team] || 'default';
  };

  // Responsive column configuration for queries
  const getQueryColumns = () => {
    const baseColumns = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: isMobile ? 50 : 60,
        fixed: isMobile ? 'left' : false,
      },
      {
        title: 'Question',
        dataIndex: 'question',
        key: 'question',
        ellipsis: true,
        render: (text) => (
          <span title={text}>
            {isMobile && text.length > 30 ? `${text.substring(0, 30)}...` : text}
          </span>
        ),
      },
      {
        title: 'Team',
        dataIndex: 'assignedTeam',
        key: 'assignedTeam',
        width: isMobile ? 60 : 80,
        render: (team) => (
          <Tag color={getTeamColor(team)} size={isMobile ? 'small' : 'default'}>
            {team}
          </Tag>
        ),
      },
      {
        title: 'Status',
        dataIndex: 'status',
        key: 'status',
        width: isMobile ? 60 : 80,
        render: (status) => (
          <Tag color={getStatusColor(status)} size={isMobile ? 'small' : 'default'}>
            {isMobile ? status.substring(0, 4) : status}
          </Tag>
        ),
      },
      {
        title: 'Context',
        dataIndex: 'fieldContext',
        key: 'fieldContext',
        width: 120,
        ellipsis: true,
        responsive: ['md'],
      },
      {
        title: 'Created',
        dataIndex: 'createdAt',
        key: 'createdAt',
        width: isMobile ? 80 : 100,
        responsive: ['sm'],
        render: (date) => {
          if (!date) return '-';
          const d = new Date(date);
          return isMobile ? d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) : d.toLocaleDateString();
        },
      },
      {
        title: 'Actions',
        key: 'actions',
        width: isMobile ? 70 : 120,
        fixed: isMobile ? 'right' : false,
        render: (_, record) => (
          <Space>
            <Button 
              size="small" 
              type="link"
              onClick={() => {
                setSelectedQuery(record);
                setResolveModalVisible(true);
              }}
              disabled={record.status === 'RESOLVED' || !canResolveQuery(record)}
            >
              {record.status === 'OPEN' ? (isMobile ? 'Fix' : 'Resolve') : 'View'}
            </Button>
          </Space>
        ),
      },
    ];

    return baseColumns;
  };

  const canCreateQuery = () => {
    return ['PLANT', 'JVC'].includes(userRole);
  };

  const canResolveQuery = (query) => {
    return query.assignedTeam === userRole || userRole === 'ADMIN';
  };

  const getTabCount = (queryList) => {
    return queryList.length > 0 ? queryList.length : null;
  };

  return (
    <Card 
      title="Queries" 
      extra={
        canCreateQuery() && (
          <Button 
            type="primary" 
            icon={<PlusOutlined />}
            onClick={() => setCreateModalVisible(true)}
          >
            Raise Query
          </Button>
        )
      }
    >
      <Tabs 
        defaultActiveKey="all" 
        className={isMobile ? 'query-widget-mobile' : ''}
        size={isMobile ? 'small' : 'default'}
      >
        <TabPane 
          tab={
            <Badge count={getTabCount(queries.all)} size="small">
              <span>All Queries</span>
            </Badge>
          } 
          key="all"
        >
          <Table
            dataSource={queries.all}
            columns={getQueryColumns()}
            loading={loading}
            pagination={{ pageSize: isMobile ? 5 : 10 }}
            size="small"
            rowKey="id"
            scroll={isMobile ? { x: 500 } : undefined}
            className={isMobile ? 'touch-friendly-table' : ''}
          />
        </TabPane>
        
        <TabPane 
          tab={
            <Badge count={getTabCount(queries.open)} size="small">
              <span style={{ color: '#ff4d4f' }}>Open</span>
            </Badge>
          } 
          key="open"
        >
          <Table
            dataSource={queries.open}
            columns={getQueryColumns()}
            loading={loading}
            pagination={{ pageSize: isMobile ? 5 : 10 }}
            size="small"
            rowKey="id"
            scroll={isMobile ? { x: 500 } : undefined}
            className={isMobile ? 'touch-friendly-table' : ''}
          />
        </TabPane>
        
        <TabPane 
          tab={
            <Badge count={getTabCount(queries.resolved)} size="small">
              <span style={{ color: '#52c41a' }}>Resolved</span>
            </Badge>
          } 
          key="resolved"
        >
          <Table
            dataSource={queries.resolved}
            columns={getQueryColumns()}
            loading={loading}
            pagination={{ pageSize: isMobile ? 5 : 10 }}
            size="small"
            rowKey="id"
            scroll={isMobile ? { x: 500 } : undefined}
            className={isMobile ? 'touch-friendly-table' : ''}
          />
        </TabPane>
        
        <TabPane 
          tab={
            <Badge count={getTabCount(queries.myQueries)} size="small">
              <span>My Queries</span>
            </Badge>
          } 
          key="my"
        >
          <Table
            dataSource={queries.myQueries}
            columns={getQueryColumns()}
            loading={loading}
            pagination={{ pageSize: isMobile ? 5 : 10 }}
            size="small"
            rowKey="id"
            scroll={isMobile ? { x: 500 } : undefined}
            className={isMobile ? 'touch-friendly-table' : ''}
          />
        </TabPane>
      </Tabs>

      {/* Create Query Modal */}
      <Modal
        title="Raise New Query"
        open={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreateQuery}
        >
          <Form.Item
            name="assignedTeam"
            label="Assign to Team"
            rules={[{ required: true, message: 'Please select a team' }]}
          >
            <Select placeholder="Select team">
              <Option value="CQS">CQS Team</Option>
              <Option value="TECH">Tech Team</Option>
              <Option value="JVC">JVC Team</Option>
            </Select>
          </Form.Item>
          
          <Form.Item
            name="fieldContext"
            label="Field Context"
            help="Which field or section is this query about?"
          >
            <Input placeholder="e.g., Material Name, Safety Data, etc." />
          </Form.Item>
          
          <Form.Item
            name="question"
            label="Question"
            rules={[{ required: true, message: 'Please enter your question' }]}
          >
            <TextArea 
              rows={4} 
              placeholder="Describe your question or issue in detail..."
            />
          </Form.Item>
          
          <Form.Item
            name="priority"
            label="Priority"
            initialValue="MEDIUM"
          >
            <Select>
              <Option value="LOW">Low</Option>
              <Option value="MEDIUM">Medium</Option>
              <Option value="HIGH">High</Option>
              <Option value="URGENT">Urgent</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      {/* Resolve Query Modal */}
      <Modal
        title={`Resolve Query #${selectedQuery?.id}`}
        open={resolveModalVisible}
        onCancel={() => {
          setResolveModalVisible(false);
          resolveForm.resetFields();
          setSelectedQuery(null);
        }}
        onOk={() => resolveForm.submit()}
        width={600}
      >
        {selectedQuery && (
          <>
            <div style={{ marginBottom: 16, padding: 12, background: '#f5f5f5', borderRadius: 4 }}>
              <strong>Question:</strong> {selectedQuery.question}
              {selectedQuery.fieldContext && (
                <div><strong>Field Context:</strong> {selectedQuery.fieldContext}</div>
              )}
            </div>
            
            <Form
              form={resolveForm}
              layout="vertical"
              onFinish={handleResolveQuery}
            >
              <Form.Item
                name="resolution"
                label="Resolution"
                rules={[{ required: true, message: 'Please provide a resolution' }]}
              >
                <TextArea 
                  rows={6} 
                  placeholder="Provide detailed resolution or answer to the query..."
                />
              </Form.Item>
            </Form>
          </>
        )}
      </Modal>
    </Card>
  );
};

export default QueryWidget;