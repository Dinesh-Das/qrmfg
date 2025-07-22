import React, { useState, useEffect } from 'react';
import {
  Card,
  Table,
  Tag,
  Button,
  Space,
  Modal,
  Form,
  Input,
  Select,
  Tooltip,
  Badge,
  Alert,
  Spin,
  Row,
  Col,
  Statistic,
  Progress,
  Typography,
  Divider,
  message
} from 'antd';
import {
  MessageOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  FilterOutlined,
  ReloadOutlined,
  EyeOutlined
} from '@ant-design/icons';
import { queryAPI } from '../../services/queryAPI';
import QueryResponseEditor from './QueryResponseEditor';
import MaterialContextDisplay from './MaterialContextDisplay';
import QueryHistoryTracker from './QueryHistoryTracker';

const { TextArea } = Input;
const { Option } = Select;
const { Text, Title } = Typography;

const QueryInbox = ({ team, userRole }) => {
  const [loading, setLoading] = useState(false);
  const [queries, setQueries] = useState([]);
  const [filteredQueries, setFilteredQueries] = useState([]);
  const [selectedQuery, setSelectedQuery] = useState(null);
  const [resolveModalVisible, setResolveModalVisible] = useState(false);
  const [detailModalVisible, setDetailModalVisible] = useState(false);
  const [resolveForm] = Form.useForm();
  const [filters, setFilters] = useState({
    status: 'all',
    priority: 'all',
    material: '',
    daysOpen: 'all',
    project: '',
    plant: '',
    assignedTeam: 'all'
  });
  const [stats, setStats] = useState({
    total: 0,
    open: 0,
    resolved: 0,
    overdue: 0,
    avgResolutionTime: 0
  });

  useEffect(() => {
    loadQueries();
    loadStats();
  }, [team]);

  useEffect(() => {
    applyFilters();
  }, [queries, filters]);

  const loadQueries = async () => {
    try {
      setLoading(true);
      const response = await fetch(`/qrmfg/api/v1/queries/inbox/${team}`);
      if (response.ok) {
        const data = await response.json();
        setQueries(data);
      } else {
        message.error('Failed to load queries');
      }
    } catch (error) {
      console.error('Failed to load queries:', error);
      message.error('Failed to load queries');
    } finally {
      setLoading(false);
    }
  };

  const loadStats = async () => {
    try {
      const [openCount, resolvedCount, overdueQueries, avgTime] = await Promise.all([
        fetch(`/qrmfg/api/v1/queries/stats/count-open/${team}`).then(r => r.json()),
        fetch(`/qrmfg/api/v1/queries/stats/count-resolved/${team}`).then(r => r.json()),
        fetch('/qrmfg/api/v1/queries/overdue').then(r => r.json()),
        fetch(`/qrmfg/api/v1/queries/stats/avg-resolution-time/${team}`).then(r => r.json())
      ]);

      setStats({
        total: openCount + resolvedCount,
        open: openCount,
        resolved: resolvedCount,
        overdue: overdueQueries.filter(q => q.assignedTeam === team).length,
        avgResolutionTime: avgTime
      });
    } catch (error) {
      console.error('Failed to load stats:', error);
    }
  };

  const applyFilters = () => {
    let filtered = [...queries];

    if (filters.status !== 'all') {
      filtered = filtered.filter(q => q.status === filters.status);
    }

    if (filters.priority !== 'all') {
      filtered = filtered.filter(q => q.priorityLevel === filters.priority);
    }

    if (filters.material) {
      filtered = filtered.filter(q =>
        q.materialCode?.toLowerCase().includes(filters.material.toLowerCase()) ||
        q.materialName?.toLowerCase().includes(filters.material.toLowerCase())
      );
    }

    if (filters.project) {
      filtered = filtered.filter(q =>
        q.projectCode?.toLowerCase().includes(filters.project.toLowerCase())
      );
    }

    if (filters.plant) {
      filtered = filtered.filter(q =>
        q.plantCode?.toLowerCase().includes(filters.plant.toLowerCase()) ||
        q.blockId?.toLowerCase().includes(filters.plant.toLowerCase())
      );
    }

    if (filters.assignedTeam !== 'all') {
      filtered = filtered.filter(q => q.assignedTeam === filters.assignedTeam);
    }

    if (filters.daysOpen !== 'all') {
      const threshold = parseInt(filters.daysOpen);
      filtered = filtered.filter(q => q.daysOpen >= threshold);
    }

    setFilteredQueries(filtered);
  };

  const handleResolveQuery = async (values) => {
    try {
      const response = await fetch(`/qrmfg/api/v1/queries/${selectedQuery.id}/resolve`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          response: values.response,
          priorityLevel: values.priorityLevel
        })
      });

      if (response.ok) {
        message.success('Query resolved successfully');
        setResolveModalVisible(false);
        resolveForm.resetFields();
        setSelectedQuery(null);
        loadQueries();
        loadStats();
      } else {
        message.error('Failed to resolve query');
      }
    } catch (error) {
      console.error('Failed to resolve query:', error);
      message.error('Failed to resolve query');
    }
  };

  const getStatusColor = (status) => {
    return status === 'OPEN' ? 'red' : 'green';
  };

  const getPriorityColor = (priority) => {
    const colors = {
      'LOW': 'blue',
      'MEDIUM': 'orange',
      'HIGH': 'red',
      'URGENT': 'purple'
    };
    return colors[priority] || 'default';
  };

  const getDaysOpenColor = (days) => {
    if (days >= 3) return '#ff4d4f';
    if (days >= 2) return '#faad14';
    return '#52c41a';
  };

  const getSLAProgress = (daysOpen) => {
    const slaThreshold = 3; // 3 days SLA
    const progress = Math.min((daysOpen / slaThreshold) * 100, 100);
    let status = 'normal';
    if (progress >= 100) status = 'exception';
    else if (progress >= 80) status = 'active';

    return { progress, status };
  };

  const columns = [
    {
      title: 'Material Context',
      dataIndex: 'materialCode',
      key: 'materialContext',
      width: 180,
      render: (text, record) => (
        <div>
          <Text strong>{text}</Text>
          {record.materialName && (
            <div style={{ fontSize: '12px', color: '#666' }}>
              {record.materialName}
            </div>
          )}
          {record.projectCode && (
            <div style={{ fontSize: '11px', color: '#999' }}>
              Project: {record.projectCode}
            </div>
          )}
          {record.plantCode && record.blockId && (
            <div style={{ fontSize: '11px', color: '#999' }}>
              Plant: {record.plantCode} | Block: {record.blockId}
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Question',
      dataIndex: 'question',
      key: 'question',
      ellipsis: true,
      render: (text) => (
        <Tooltip title={text}>
          <Text>{text}</Text>
        </Tooltip>
      ),
    },
    {
      title: 'Field Context',
      dataIndex: 'fieldContext',
      key: 'fieldContext',
      width: 120,
      render: (text, record) => (
        <div>
          {text && <Tag color="blue">{text}</Tag>}
          {record.stepNumber && (
            <div style={{ fontSize: '12px', color: '#666' }}>
              Step {record.stepNumber}
            </div>
          )}
        </div>
      ),
    },
    {
      title: 'Priority',
      dataIndex: 'priorityLevel',
      key: 'priorityLevel',
      width: 80,
      render: (priority) => (
        <Tag color={getPriorityColor(priority)}>
          {priority || 'MEDIUM'}
        </Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (status) => (
        <Tag color={getStatusColor(status)}>
          {status}
        </Tag>
      ),
    },
    {
      title: 'Days Open',
      dataIndex: 'daysOpen',
      key: 'daysOpen',
      width: 100,
      render: (days, record) => {
        const { progress, status } = getSLAProgress(days);
        return (
          <div>
            <Text style={{ color: getDaysOpenColor(days) }}>
              {days} days
            </Text>
            <Progress
              percent={progress}
              status={status}
              size="small"
              showInfo={false}
              style={{ marginTop: 4 }}
            />
          </div>
        );
      },
    },
    {
      title: 'Raised By',
      dataIndex: 'raisedBy',
      key: 'raisedBy',
      width: 100,
    },
    {
      title: 'Actions',
      key: 'actions',
      width: 120,
      render: (_, record) => (
        <Space>
          <Button
            size="small"
            icon={<EyeOutlined />}
            onClick={() => {
              setSelectedQuery(record);
              setDetailModalVisible(true);
            }}
          >
            View
          </Button>
          {record.status === 'OPEN' && (
            <Button
              size="small"
              type="primary"
              icon={<CheckCircleOutlined />}
              onClick={() => {
                setSelectedQuery(record);
                setResolveModalVisible(true);
              }}
            >
              Resolve
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      {/* Statistics Cards */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="Total Queries"
              value={stats.total}
              prefix={<MessageOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Open Queries"
              value={stats.open}
              valueStyle={{ color: '#cf1322' }}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Overdue"
              value={stats.overdue}
              valueStyle={{ color: stats.overdue > 0 ? '#cf1322' : '#3f8600' }}
              prefix={<ExclamationCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Avg Resolution Time"
              value={stats.avgResolutionTime}
              suffix="hrs"
              precision={1}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Enhanced Filters */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 8]} align="middle">
          <Col span={4}>
            <Select
              placeholder="Status"
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
              style={{ width: '100%' }}
            >
              <Option value="all">All Status</Option>
              <Option value="OPEN">Open</Option>
              <Option value="RESOLVED">Resolved</Option>
            </Select>
          </Col>
          <Col span={4}>
            <Select
              placeholder="Priority"
              value={filters.priority}
              onChange={(value) => setFilters({ ...filters, priority: value })}
              style={{ width: '100%' }}
            >
              <Option value="all">All Priority</Option>
              <Option value="LOW">Low</Option>
              <Option value="MEDIUM">Medium</Option>
              <Option value="HIGH">High</Option>
              <Option value="URGENT">Urgent</Option>
            </Select>
          </Col>
          <Col span={4}>
            <Input
              placeholder="Material ID/Name"
              value={filters.material}
              onChange={(e) => setFilters({ ...filters, material: e.target.value })}
            />
          </Col>
          <Col span={4}>
            <Input
              placeholder="Project Code"
              value={filters.project}
              onChange={(e) => setFilters({ ...filters, project: e.target.value })}
            />
          </Col>
          <Col span={4}>
            <Input
              placeholder="Plant/Block"
              value={filters.plant}
              onChange={(e) => setFilters({ ...filters, plant: e.target.value })}
            />
          </Col>
          <Col span={4}>
            <Select
              placeholder="Team"
              value={filters.assignedTeam}
              onChange={(value) => setFilters({ ...filters, assignedTeam: value })}
              style={{ width: '100%' }}
            >
              <Option value="all">All Teams</Option>
              <Option value="CQS">CQS</Option>
              <Option value="TECH">Tech</Option>
              <Option value="JVC">JVC</Option>
            </Select>
          </Col>
          <Col span={4}>
            <Select
              placeholder="Days Open"
              value={filters.daysOpen}
              onChange={(value) => setFilters({ ...filters, daysOpen: value })}
              style={{ width: '100%' }}
            >
              <Option value="all">All</Option>
              <Option value="1">1+ days</Option>
              <Option value="2">2+ days</Option>
              <Option value="3">3+ days (Overdue)</Option>
            </Select>
          </Col>
          <Col span={8}>
            <Space>
              <Button
                icon={<ReloadOutlined />}
                onClick={() => {
                  loadQueries();
                  loadStats();
                }}
              >
                Refresh
              </Button>
              <Button
                icon={<FilterOutlined />}
                onClick={() => setFilters({
                  status: 'all',
                  priority: 'all',
                  material: '',
                  daysOpen: 'all',
                  project: '',
                  plant: '',
                  assignedTeam: 'all'
                })}
              >
                Clear Filters
              </Button>
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Query Table */}
      <Card
        title={
          <div>
            <Title level={4} style={{ margin: 0 }}>
              {team} Team Query Inbox
            </Title>
            <Text type="secondary">
              {filteredQueries.length} of {queries.length} queries
            </Text>
          </div>
        }
      >
        {stats.overdue > 0 && (
          <Alert
            message={`${stats.overdue} queries are overdue (>3 days)`}
            type="warning"
            showIcon
            style={{ marginBottom: 16 }}
          />
        )}

        <Table
          dataSource={filteredQueries}
          columns={columns}
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `${range[0]}-${range[1]} of ${total} queries`
          }}
          rowKey="id"
          size="small"
          rowClassName={(record) =>
            record.daysOpen >= 3 ? 'overdue-row' : ''
          }
        />
      </Card>

      {/* Enhanced Query Detail Modal */}
      <Modal
        title={`Query #${selectedQuery?.id} Details`}
        open={detailModalVisible}
        onCancel={() => {
          setDetailModalVisible(false);
          setSelectedQuery(null);
        }}
        footer={[
          <Button key="close" onClick={() => setDetailModalVisible(false)}>
            Close
          </Button>,
          selectedQuery?.status === 'OPEN' && (
            <Button
              key="resolve"
              type="primary"
              onClick={() => {
                setDetailModalVisible(false);
                setResolveModalVisible(true);
              }}
            >
              Resolve Query
            </Button>
          )
        ]}
        width={1200}
      >
        {selectedQuery && (
          <Row gutter={16}>
            <Col span={14}>
              {/* Query Details */}
              <div>
                <Row gutter={16}>
                  <Col span={12}>
                    <Text strong>Material Code:</Text> {selectedQuery.materialCode}
                    {selectedQuery.materialName && (
                      <div style={{ fontSize: '12px', color: '#666' }}>
                        {selectedQuery.materialName}
                      </div>
                    )}
                  </Col>
                  <Col span={12}>
                    <Text strong>Status:</Text>{' '}
                    <Tag color={getStatusColor(selectedQuery.status)}>
                      {selectedQuery.status}
                    </Tag>
                  </Col>
                </Row>
                <Divider />

                <Row gutter={16}>
                  <Col span={8}>
                    <Text strong>Priority:</Text>{' '}
                    <Tag color={getPriorityColor(selectedQuery.priorityLevel)}>
                      {selectedQuery.priorityLevel || 'MEDIUM'}
                    </Tag>
                  </Col>
                  <Col span={8}>
                    <Text strong>Team:</Text>{' '}
                    <Tag color={selectedQuery.assignedTeam === 'CQS' ? 'blue' : selectedQuery.assignedTeam === 'TECH' ? 'purple' : 'orange'}>
                      {selectedQuery.assignedTeam}
                    </Tag>
                  </Col>
                  <Col span={8}>
                    <Text strong>Days Open:</Text>{' '}
                    <Text style={{ color: getDaysOpenColor(selectedQuery.daysOpen) }}>
                      {selectedQuery.daysOpen} days
                    </Text>
                  </Col>
                </Row>
                <Divider />

                {/* Enhanced Context Information */}
                {(selectedQuery.projectCode || selectedQuery.plantCode || selectedQuery.blockId) && (
                  <>
                    <Row gutter={16}>
                      {selectedQuery.projectCode && (
                        <Col span={8}>
                          <Text strong>Project:</Text> {selectedQuery.projectCode}
                        </Col>
                      )}
                      {selectedQuery.plantCode && (
                        <Col span={8}>
                          <Text strong>Plant:</Text> {selectedQuery.plantCode}
                        </Col>
                      )}
                      {selectedQuery.blockId && (
                        <Col span={8}>
                          <Text strong>Block:</Text> {selectedQuery.blockId}
                        </Col>
                      )}
                    </Row>
                    <Divider />
                  </>
                )}

                {selectedQuery.fieldName && (
                  <>
                    <Text strong>Field Context:</Text> {selectedQuery.fieldName}
                    {selectedQuery.stepNumber && ` (Step ${selectedQuery.stepNumber})`}
                    <Divider />
                  </>
                )}

                <Text strong>Question:</Text>
                <div style={{
                  marginTop: 8,
                  padding: 12,
                  background: '#f5f5f5',
                  borderRadius: 4,
                  whiteSpace: 'pre-wrap'
                }}>
                  {selectedQuery.question}
                </div>

                {selectedQuery.response && (
                  <>
                    <Divider />
                    <Text strong>Response:</Text>
                    <div style={{
                      marginTop: 8,
                      padding: 12,
                      background: '#f0f9ff',
                      borderRadius: 4,
                      whiteSpace: 'pre-wrap'
                    }}>
                      {selectedQuery.response}
                    </div>
                    <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                      Resolved by: {selectedQuery.resolvedBy} on{' '}
                      {selectedQuery.resolvedAt && new Date(selectedQuery.resolvedAt).toLocaleString()}
                    </div>
                  </>
                )}

                <Divider />
                <div style={{ fontSize: '12px', color: '#666' }}>
                  <div>Raised by: {selectedQuery.raisedBy}</div>
                  <div>Created: {selectedQuery.createdAt && new Date(selectedQuery.createdAt).toLocaleString()}</div>
                </div>
              </div>
            </Col>
            <Col span={10}>
              {/* Material Context and Query History */}
              <MaterialContextDisplay
                materialCode={selectedQuery.materialCode}
                workflowId={selectedQuery.workflowId}
                compact={true}
              />
              <QueryHistoryTracker
                materialCode={selectedQuery.materialCode}
                workflowId={selectedQuery.workflowId}
                compact={true}
              />
            </Col>
          </Row>
        )}
      </Modal>

      {/* Enhanced Resolve Query Modal */}
      <Modal
        title={`Resolve Query #${selectedQuery?.id}`}
        open={resolveModalVisible}
        onCancel={() => {
          setResolveModalVisible(false);
          resolveForm.resetFields();
          setSelectedQuery(null);
        }}
        onOk={() => resolveForm.submit()}
        width={1400}
        okText="Resolve Query"
        okButtonProps={{
          type: 'primary',
          size: 'large'
        }}
        cancelButtonProps={{
          size: 'large'
        }}
      >
        {selectedQuery && (
          <Row gutter={16}>
            <Col span={16}>
              {/* Query Context */}
              <div style={{
                marginBottom: 16,
                padding: 16,
                background: '#f5f5f5',
                borderRadius: 6,
                border: '1px solid #d9d9d9'
              }}>
                <Row gutter={16} style={{ marginBottom: 12 }}>
                  <Col span={12}>
                    <Text strong>Material:</Text> {selectedQuery.materialCode}
                    {selectedQuery.materialName && (
                      <div style={{ fontSize: '12px', color: '#666' }}>
                        {selectedQuery.materialName}
                      </div>
                    )}
                  </Col>
                  <Col span={12}>
                    <Space>
                      <Text strong>Team:</Text>
                      <Tag color={selectedQuery.assignedTeam === 'CQS' ? 'blue' : selectedQuery.assignedTeam === 'TECH' ? 'purple' : 'orange'}>
                        {selectedQuery.assignedTeam}
                      </Tag>
                      <Text strong>Priority:</Text>
                      <Tag color={getPriorityColor(selectedQuery.priorityLevel)}>
                        {selectedQuery.priorityLevel || 'MEDIUM'}
                      </Tag>
                    </Space>
                  </Col>
                </Row>

                {(selectedQuery.projectCode || selectedQuery.plantCode || selectedQuery.blockId) && (
                  <Row gutter={16} style={{ marginBottom: 12 }}>
                    {selectedQuery.projectCode && (
                      <Col span={8}>
                        <Text strong>Project:</Text> {selectedQuery.projectCode}
                      </Col>
                    )}
                    {selectedQuery.plantCode && (
                      <Col span={8}>
                        <Text strong>Plant:</Text> {selectedQuery.plantCode}
                      </Col>
                    )}
                    {selectedQuery.blockId && (
                      <Col span={8}>
                        <Text strong>Block:</Text> {selectedQuery.blockId}
                      </Col>
                    )}
                  </Row>
                )}

                <Text strong>Question:</Text>
                <div style={{ marginTop: 8, whiteSpace: 'pre-wrap', fontSize: '14px' }}>
                  {selectedQuery.question}
                </div>

                {selectedQuery.fieldName && (
                  <div style={{ marginTop: 8 }}>
                    <Text strong>Field Context:</Text> {selectedQuery.fieldName}
                    {selectedQuery.stepNumber && ` (Step ${selectedQuery.stepNumber})`}
                  </div>
                )}

                <div style={{ marginTop: 8, fontSize: '12px', color: '#666' }}>
                  <Space>
                    <span>Raised by: {selectedQuery.raisedBy}</span>
                    <span>•</span>
                    <span>{selectedQuery.createdAt && new Date(selectedQuery.createdAt).toLocaleString()}</span>
                    <span>•</span>
                    <span style={{ color: getDaysOpenColor(selectedQuery.daysOpen) }}>
                      {selectedQuery.daysOpen} days open
                    </span>
                  </Space>
                </div>
              </div>

              {/* Resolution Form */}
              <Form
                form={resolveForm}
                layout="vertical"
                onFinish={handleResolveQuery}
              >
                <Form.Item
                  name="response"
                  label={
                    <Space>
                      <span>Resolution Response</span>
                      <Text type="secondary">(Required)</Text>
                    </Space>
                  }
                  rules={[{ required: true, message: 'Please provide a resolution response' }]}
                >
                  <QueryResponseEditor
                    placeholder="Provide detailed resolution or answer to the query. Include any relevant technical details, safety considerations, or references to documentation..."
                  />
                </Form.Item>

                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item
                      name="priorityLevel"
                      label="Update Priority (Optional)"
                      initialValue={selectedQuery.priorityLevel || 'MEDIUM'}
                    >
                      <Select size="large">
                        <Option value="LOW">Low Priority</Option>
                        <Option value="MEDIUM">Medium Priority</Option>
                        <Option value="HIGH">High Priority</Option>
                        <Option value="URGENT">Urgent Priority</Option>
                      </Select>
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <div style={{ marginTop: 30 }}>
                      <Text type="secondary">
                        This resolution will be sent to the plant team and the workflow will continue.
                      </Text>
                    </div>
                  </Col>
                </Row>
              </Form>
            </Col>
            <Col span={8}>
              {/* Context and History */}
              <MaterialContextDisplay
                materialCode={selectedQuery.materialCode}
                workflowId={selectedQuery.workflowId}
                compact={true}
              />
              <QueryHistoryTracker
                materialCode={selectedQuery.materialCode}
                workflowId={selectedQuery.workflowId}
                compact={true}
              />
            </Col>
          </Row>
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

export default QueryInbox;