import React from 'react';
import { Menu } from 'antd';
import { Link, Routes, Route, useLocation, useNavigate } from 'react-router-dom';
import Users from './Users';
import Roles from './Roles';
import Screens from './Screens';
import Sessions from './Sessions';
import WorkflowMonitoring from './WorkflowMonitoring';
import UserRoleManagement from './UserRoleManagement';
import Auditlogs from './Auditlogs';

const Admin = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const currentPath = location.pathname;

  const menuItems = [
    { key: '/qrmfg/admin/users', label: 'Users' },
    { key: '/qrmfg/admin/roles', label: 'Roles' },
    { key: '/qrmfg/admin/screens', label: 'Screens' },
    { key: '/qrmfg/admin/auditlogs', label: 'Audit Logs' },
    { key: '/qrmfg/admin/sessions', label: 'Sessions' },
    { key: '/qrmfg/admin/workflow-monitoring', label: 'Workflow Monitoring' },
    { key: '/qrmfg/admin/user-role-management', label: 'User Role Management' }
  ];

  // If we're at /qrmfg/admin, redirect to /qrmfg/admin/users
  React.useEffect(() => {
    if (currentPath === '/qrmfg/admin') {
      navigate('/qrmfg/admin/users');
    }
  }, [currentPath, navigate]);

  return (
    <div style={{ padding: 24 }}>
      <h2>Admin Panel</h2>
      <Menu
        mode="horizontal"
        selectedKeys={[currentPath]}
        items={menuItems.map(item => ({
          ...item,
          label: <Link to={item.key}>{item.label}</Link>
        }))}
        style={{ marginBottom: 24 }}
      />
      <div style={{ marginTop: 24 }}>
        <Routes>
          <Route path="users" element={<Users />} />
          <Route path="roles" element={<Roles />} />
          <Route path="screens" element={<Screens />} />
          <Route path="auditlogs" element={<Auditlogs />} />
          <Route path="sessions" element={<Sessions />} />
          <Route path="workflow-monitoring" element={<WorkflowMonitoring />} />
          <Route path="user-role-management" element={<UserRoleManagement />} />
        </Routes>
      </div>
    </div>
  );
};

export default Admin; 
