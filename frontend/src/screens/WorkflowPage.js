import React, { useState } from 'react';
import { Tabs, Card, Row, Col } from 'antd';
import {
  DashboardOutlined,
  QuestionCircleOutlined,
  HistoryOutlined
} from '@ant-design/icons';
import WorkflowDashboard from '../components/WorkflowDashboard';
import QueryWidget from '../components/QueryWidget';
import AuditTimeline from '../components/AuditTimeline';
import DebugInfo from '../components/DebugInfo';
import { getCurrentUser, getUserRole } from '../services/auth';

// Import test auth for development
if (process.env.NODE_ENV === 'development') {
  import('../utils/testAuth');
  import('../utils/screenAccessTest');
}

const { TabPane } = Tabs;

const WorkflowPage = () => {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [selectedWorkflowId, setSelectedWorkflowId] = useState(null);

  const currentUser = getCurrentUser();
  const userRole = getUserRole();

  // Debug logging
  console.log('WorkflowPage - currentUser:', currentUser);
  console.log('WorkflowPage - userRole:', userRole);

  const handleTabChange = (key) => {
    setActiveTab(key);
  };

  const handleWorkflowSelect = (workflowId) => {
    setSelectedWorkflowId(workflowId);
    setActiveTab('queries'); // Switch to queries tab when workflow is selected
  };

  return (
    <div style={{ padding: '0' }}>
      <DebugInfo />
      <Card
        title="MSDS Workflow Management"
        style={{ marginBottom: 24 }}
        extra={
          <span style={{ fontSize: '14px', color: '#666' }}>
            Welcome, {currentUser} ({userRole})
          </span>
        }
      >
        <Tabs
          activeKey={activeTab}
          onChange={handleTabChange}
          type="card"
          size="large"
        >
          <TabPane
            tab={
              <span>
                <DashboardOutlined />
                Dashboard
              </span>
            }
            key="dashboard"
          >
            <WorkflowDashboard onWorkflowSelect={handleWorkflowSelect} />
          </TabPane>

          <TabPane
            tab={
              <span>
                <QuestionCircleOutlined />
                Queries
              </span>
            }
            key="queries"
          >
            <Row gutter={[16, 16]}>
              <Col span={24}>
                <QueryWidget
                  workflowId={selectedWorkflowId}
                  userRole={userRole}
                />
              </Col>
            </Row>
          </TabPane>

          <TabPane
            tab={
              <span>
                <HistoryOutlined />
                Audit Trail
              </span>
            }
            key="audit"
          >
            <Row gutter={[16, 16]}>
              <Col span={24}>
                {selectedWorkflowId ? (
                  <AuditTimeline
                    workflowId={selectedWorkflowId}
                    entityType="complete"
                  />
                ) : (
                  <Card>
                    <div style={{
                      textAlign: 'center',
                      padding: '40px 20px',
                      color: '#999'
                    }}>
                      <HistoryOutlined style={{ fontSize: '48px', marginBottom: '16px' }} />
                      <h3>Select a Workflow</h3>
                      <p>Choose a workflow from the dashboard to view its audit trail</p>
                    </div>
                  </Card>
                )}
              </Col>
            </Row>
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};

export default WorkflowPage;