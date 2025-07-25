import React, { useState } from 'react';
import { Tabs, Card, Row, Col } from 'antd';
import { 
  DashboardOutlined, 
  QuestionCircleOutlined, 
  HistoryOutlined 
} from '@ant-design/icons';
import WorkflowDashboard from '../components/workflow/WorkflowDashboard';
import QueryWidget from '../components/workflow/QueryWidget';
import AuditTimeline from '../components/workflow/AuditTimeline';
import { getCurrentUser, getUserRole } from '../utils/auth';

const { TabPane } = Tabs;

const WorkflowPage = () => {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [selectedWorkflowId, setSelectedWorkflowId] = useState(null);
  
  const currentUser = getCurrentUser();
  const userRole = getUserRole();

  const handleTabChange = (key) => {
    setActiveTab(key);
  };

  const handleWorkflowSelect = (workflowId) => {
    setSelectedWorkflowId(workflowId);
    setActiveTab('queries'); // Switch to queries tab when workflow is selected
  };

  return (
    <div style={{ padding: '0' }}>
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