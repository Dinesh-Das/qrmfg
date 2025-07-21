import React, { useState, useEffect } from 'react';
import { Typography, Row, Col, Card, Statistic, Tabs } from 'antd';
import { MessageOutlined, ClockCircleOutlined, CheckCircleOutlined } from '@ant-design/icons';
import QueryInbox from '../components/workflow/QueryInbox';
import WorkflowDashboard from '../components/workflow/WorkflowDashboard';

const { Title } = Typography;
const { TabPane } = Tabs;

const CQSView = () => {
  const [queryStats, setQueryStats] = useState({
    totalQueries: 0,
    openQueries: 0,
    resolvedToday: 0
  });

  useEffect(() => {
    loadDashboardStats();
  }, []);

  const loadDashboardStats = async () => {
    try {
      const [openCount, resolvedToday] = await Promise.all([
        fetch('/qrmfg/api/v1/queries/stats/count-open/CQS').then(r => r.json()),
        fetch('/qrmfg/api/v1/queries/stats/resolved-today').then(r => r.json())
      ]);

      setQueryStats({
        totalQueries: openCount + resolvedToday,
        openQueries: openCount,
        resolvedToday: resolvedToday
      });
    } catch (error) {
      console.error('Failed to load dashboard stats:', error);
    }
  };

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>CQS Dashboard</Title>
      
      {/* Quick Stats */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card>
            <Statistic
              title="Open Queries"
              value={queryStats.openQueries}
              prefix={<MessageOutlined />}
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="Resolved Today"
              value={queryStats.resolvedToday}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="Total Queries"
              value={queryStats.totalQueries}
              prefix={<ClockCircleOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* Main Content Tabs */}
      <Tabs defaultActiveKey="queries" size="large">
        <TabPane tab="Query Inbox" key="queries">
          <QueryInbox team="CQS" userRole="CQS_USER" />
        </TabPane>
        <TabPane tab="Workflow Dashboard" key="dashboard">
          <WorkflowDashboard userRole="CQS_USER" />
        </TabPane>
      </Tabs>
    </div>
  );
};

export default CQSView; 