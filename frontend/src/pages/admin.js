import React from 'react';
import { Menu } from 'antd';
import { Link, Routes, Route, useLocation, useNavigate } from 'react-router-dom';
import Users from './admin/Users';
import Roles from './admin/Roles';
import Screens from './admin/Screens';
import AuditLogs from './admin/Auditlogs';
import Sessions from './admin/Sessions';

const AdminPanel = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const currentPath = location.pathname;

  const menuItems = [
    { key: '/qrmfg/admin/users', label: 'Users' },
    { key: '/qrmfg/admin/roles', label: 'Roles' },
    { key: '/qrmfg/admin/screens', label: 'Screens' },
    { key: '/qrmfg/admin/auditlogs', label: 'Audit Logs' },
    { key: '/qrmfg/admin/sessions', label: 'Sessions' }
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
          <Route path="auditlogs" element={<AuditLogs />} />
          <Route path="sessions" element={<Sessions />} />
        </Routes>
      </div>
    </div>
  );
};

export default AdminPanel; 