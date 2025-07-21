import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Typography, Table } from 'antd';
import axios from 'axios';

const { Title } = Typography;

const Dashboard = () => {
  const [userStats, setUserStats] = useState({ total: 0, active: 0 });
  const [roleDist, setRoleDist] = useState([]);
  const [activityTimeline, setActivityTimeline] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      axios.get('/qrmfg/api/v1/reports/analytics/user-stats'),
      axios.get('/qrmfg/api/v1/reports/analytics/role-distribution'),
      axios.get('/qrmfg/api/v1/reports/analytics/activity-timeline'),
    ]).then(([us, rd, at]) => {
      setUserStats(us.data);
      setRoleDist(Object.entries(rd.data).map(([role, count]) => ({ role, count })));
      setActivityTimeline(Object.entries(at.data).map(([date, count]) => ({ date, count })));
    }).finally(() => setLoading(false));
  }, []);

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Dashboard</Title>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}><Card><Statistic title="Total Users" value={userStats.total} loading={loading} /></Card></Col>
        <Col span={6}><Card><Statistic title="Active Users" value={userStats.active} loading={loading} /></Card></Col>
      </Row>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={12}>
          <Card title="Role Distribution">
            <Table dataSource={roleDist} columns={[{ title: 'Role', dataIndex: 'role' }, { title: 'Count', dataIndex: 'count' }]} rowKey="role" pagination={false} loading={loading} />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="Activity Timeline">
            <Table dataSource={activityTimeline} columns={[{ title: 'Date', dataIndex: 'date' }, { title: 'Count', dataIndex: 'count' }]} rowKey="date" pagination={false} loading={loading} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard; 