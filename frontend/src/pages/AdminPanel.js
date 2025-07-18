import React from 'react';
import { Tabs } from 'antd';
import Users from './AdminPanel/Users';
import Roles from './AdminPanel/Roles';
import Screens from './AdminPanel/Screens';
import AuditLogs from './AdminPanel/AuditLogs';
import Sessions from './AdminPanel/Sessions';


const { TabPane } = Tabs;

const AdminPanel = () => {
  return (
    <div style={{ padding: 24 }}>
      <h2>Admin Panel</h2>
      <Tabs defaultActiveKey="users" type="card">
        <TabPane tab="Users" key="users">
          <Users embedded />
        </TabPane>
        <TabPane tab="Roles" key="roles">
          <Roles embedded />
        </TabPane>
        <TabPane tab="Screens" key="screens">
          <Screens embedded />
        </TabPane>
        <TabPane tab="Audit Logs" key="audit-logs">
          <AuditLogs embedded />
        </TabPane>
        <TabPane tab="Sessions" key="sessions">
          <Sessions embedded />
        </TabPane>
      </Tabs>
    </div>
  );
};

export default AdminPanel; 