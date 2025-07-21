import React from 'react';
import { Card, Row, Col, Typography, Button } from 'antd';
import { Link } from 'react-router-dom';

const { Title, Paragraph } = Typography;

const Home = () => (
  <div style={{ padding: 24 }}>
    <Title level={2}>Welcome to the RBAC System</Title>
    <Paragraph>
      This is your enterprise Role-Based Access Control (RBAC) management platform. Use the quick links below to access key features.
    </Paragraph>
    <Row gutter={16} style={{ marginBottom: 24 }}>
      <Col span={6}>
        <Card title="Dashboard" bordered={false}>
          <Button type="primary" block as={Link} to="/qrmfg/dashboard">
            Go to Dashboard
          </Button>
        </Card>
      </Col>
      <Col span={6}>
        <Card title="Reports" bordered={false}>
          <Button type="primary" block as={Link} to="/qrmfg/reports">
            Go to Reports
          </Button>
        </Card>
      </Col>
      <Col span={6}>
        <Card title="Profile" bordered={false}>
          <Button type="primary" block as={Link} to="/qrmfg/profile">
            Go to Profile
          </Button>
        </Card>
      </Col>
      <Col span={6}>
        <Card title="System" bordered={false}>
          <Button type="primary" block as={Link} to="/qrmfg/system">
            System Dashboard
          </Button>
        </Card>
      </Col>
    </Row>
    <Card>
      <Title level={4}>Get Started</Title>
      <ul>
        <li>Manage users, roles, groups, and permissions from the side menu.</li>
        <li>View audit logs and monitor security events.</li>
        <li>Customize your profile and settings.</li>
      </ul>
    </Card>
  </div>
);

export default Home; 