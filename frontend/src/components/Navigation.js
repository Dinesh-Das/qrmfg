import React, { useEffect, useState } from 'react';
import { Layout, Menu, Button } from 'antd';
import {
  UserOutlined,
  TeamOutlined,
  SafetyOutlined,
  AppstoreOutlined,
  DesktopOutlined,
  FileSearchOutlined,
  SettingOutlined,
  LogoutOutlined,
  AppstoreAddOutlined
} from '@ant-design/icons';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { isAuthenticated, getUserRoles } from '../utils/auth';
import axios from 'axios';

const { Sider } = Layout;

const Navigation = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [allowedScreens, setAllowedScreens] = useState([]);

  useEffect(() => {
    if (isAuthenticated()) {
      axios.get('/api/v1/admin/screen-role-mapping/my-screens')
        .then(res => {
          setAllowedScreens(res.data);
          console.log('Allowed screens:', res.data);
        })
        .catch(() => setAllowedScreens([]));
    }
  }, []);

  if (!isAuthenticated()) {
    return null;
  }

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const userRoles = getUserRoles();
  const isAdmin = userRoles.includes('ADMIN');

  const menuItems = [
    { key: '/admin', icon: <UserOutlined />, label: <Link to="/admin">Admin Panel</Link> },
    { key: '/reports', icon: <FileSearchOutlined />, label: <Link to="/reports">Reports</Link> },
    { key: '/home', icon: <AppstoreAddOutlined />, label: <Link to="/home">Home</Link> },
    { key: '/dashboard', icon: <AppstoreAddOutlined />, label: <Link to="/dashboard">Dashboard</Link> },
    { key: '/settings', icon: <SettingOutlined />, label: <Link to="/settings">Settings</Link> },
    { key: '/pending-tasks', icon: <FileSearchOutlined />, label: <Link to="/pending-tasks">Pending Tasks</Link> },
    { key: '/jvc', icon: <AppstoreOutlined />, label: <Link to="/jvc">JVC</Link> },
    { key: '/cqs', icon: <AppstoreOutlined />, label: <Link to="/cqs">CQS</Link> },
    { key: '/tech', icon: <AppstoreOutlined />, label: <Link to="/tech">TECH</Link> },
    { key: '/plant', icon: <AppstoreOutlined />, label: <Link to="/plant">PLANT</Link> },
  ];

  // Only show menu items that are allowed for the user
  const filteredMenuItems = menuItems.filter(item => allowedScreens.includes(item.key));

  return (
    <Sider width={220} style={{ minHeight: '100vh', position: 'fixed', left: 0, top: 0, bottom: 0, display: 'flex', flexDirection: 'column' }}>
      <div style={{ height: 64, margin: 16, fontWeight: 'bold', fontSize: 20, color: '#fff', textAlign: 'center' }}>
        Admin
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[location.pathname]}
        items={filteredMenuItems}
        style={{ flex: 1 }}
      />
      <div style={{ padding: 16, marginTop: 'auto' }}>
        <Button type="primary" icon={<LogoutOutlined />} block danger onClick={handleLogout}>
          Logout
        </Button>
      </div>
    </Sider>
  );
};

export default Navigation; 