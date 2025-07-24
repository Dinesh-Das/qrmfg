import React, { useState, useEffect } from 'react';
import { Typography, Card, Row, Col, Statistic, Table } from 'antd';
import axios from 'axios';
import { apiRequest } from '../api/api';

const { Title } = Typography;

const SystemDashboard = () => {
  const [health, setHealth] = useState({});
  const [stats, setStats] = useState({});
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      apiRequest('/system/health'),
      apiRequest('/system/stats'),
    ]).then(([health, stats]) => {
      setHealth(health);
      setStats(stats);
    }).catch(error => {
      console.error('Failed to fetch system data:', error);
    }).finally(() => {
      setLoading(false);
    });
  }, []);

  const formatBytes = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatUptime = (ms) => {
    const seconds = Math.floor(ms / 1000);
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    return `${days}d ${hours}h ${minutes}m`;
  };

  return (
    <div>
      <Title level={2}>System Dashboard</Title>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="System Status"
              value={health.status || 'Unknown'}
              valueStyle={{ color: health.status === 'UP' ? '#3f8600' : '#cf1322' }}
              loading={loading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Uptime"
              value={health.uptime ? formatUptime(health.uptime) : 'N/A'}
              loading={loading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Memory Used"
              value={health.memory ? formatBytes(health.memory) : 'N/A'}
              loading={loading}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="Active Users"
              value={stats.activeUsers || 0}
              loading={loading}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={24}>
          <Card title="System Statistics">
            <Table
              dataSource={Object.entries(stats).map(([key, value]) => ({
                key,
                metric: key.replace(/([A-Z])/g, ' $1').toLowerCase(),
                value: typeof value === 'number' ? value.toLocaleString() : value
              }))}
              columns={[
                {
                  title: 'Metric',
                  dataIndex: 'metric',
                  key: 'metric',
                  render: text => text.charAt(0).toUpperCase() + text.slice(1)
                },
                {
                  title: 'Value',
                  dataIndex: 'value',
                  key: 'value'
                }
              ]}
              pagination={false}
              loading={loading}
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default SystemDashboard; 