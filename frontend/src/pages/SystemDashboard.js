import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Typography, Table } from 'antd';
import axios from 'axios';

const { Title } = Typography;

const pageFiles = [
  'Home.js', 'Users.js', 'Roles.js', 'Permissions.js', 'Groups.js', 'Screens.js', 'AuditLogs.js', 'Sessions.js', 'Profile.js', 'Dashboard.js', 'Reports.js', 'SystemDashboard.js', 'Login.js', 'ForgotPassword.js', 'ResetPassword.js', 'VerifyEmail.js', 'ResendVerification.js', 'Settings.js'
];

const SystemDashboard = () => {
  const [health, setHealth] = useState({});
  const [stats, setStats] = useState({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      axios.get('/api/v1/system/health'),
      axios.get('/api/v1/system/stats'),
    ]).then(([h, s]) => {
      setHealth(h.data);
      setStats(s.data);
    }).finally(() => setLoading(false));
  }, []);

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>System Dashboard</Title>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}><Card><Statistic title="System Status" value={health.status || '...'} loading={loading} /></Card></Col>
        <Col span={6}><Card><Statistic title="Uptime (ms)" value={health.uptime} loading={loading} /></Card></Col>
        <Col span={6}><Card><Statistic title="Memory Used (bytes)" value={health.memory} loading={loading} /></Card></Col>
      </Row>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}><Card><Statistic title="Active Users" value={stats.activeUsers} loading={loading} /></Card></Col>
        <Col span={8}><Card><Statistic title="Active Sessions" value={stats.activeSessions} loading={loading} /></Card></Col>
        <Col span={8}><Card><Statistic title="Total Screens" value={pageFiles.length} loading={loading} /></Card></Col>
      </Row>
      <Row gutter={16}>
        <Col span={24}>
          <Card title="All Stats">
            <Table dataSource={Object.entries(stats).map(([k, v]) => ({ key: k, stat: k, value: v }))}
                   columns={[{ title: 'Stat', dataIndex: 'stat' }, { title: 'Value', dataIndex: 'value' }]}
                   pagination={false} loading={loading} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default SystemDashboard; 