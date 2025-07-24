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
  DashboardOutlined,
  HomeOutlined,
  CheckSquareOutlined,
  BankOutlined,
  ApiOutlined,
  AuditOutlined,
  UsergroupAddOutlined,
  KeyOutlined,
  ClockCircleOutlined,
  MonitorOutlined
} from '@ant-design/icons';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { isAuthenticated, isAdmin } from '../services/auth';
import axios from 'axios';
import { apiRequest } from '../api/api';

const { Sider } = Layout;

const Navigation = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [allowedScreens, setAllowedScreens] = useState([]);
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    if (isAuthenticated() && !isAdmin()) {
      const token = localStorage.getItem('token');
      apiRequest('/admin/screen-role-mapping/my-screens')
        .then(data => {
          setAllowedScreens(data);
          console.log('Allowed screens:', data);
        })
        .catch(error => {
          console.error('Error fetching allowed screens:', error);
          setAllowedScreens([]);
        });
    }
  }, []);

  if (!isAuthenticated()) {
    return null;
  }

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/qrmfg/login');
  };

  const menuItems = [
    { key: '/qrmfg', icon: <HomeOutlined />, label: <Link to="/qrmfg">Home</Link> },
    { key: '/qrmfg/dashboard', icon: <DashboardOutlined />, label: <Link to="/qrmfg/dashboard">Dashboard</Link> },
    { key: '/qrmfg/systemdashboard', icon: <DesktopOutlined />, label: <Link to="/qrmfg/systemdashboard">System Dashboard</Link> },
    { key: '/qrmfg/workflows', icon: <AppstoreOutlined />, label: <Link to="/qrmfg/workflows">Workflows</Link> },
    { key: '/qrmfg/admin', icon: <UserOutlined />, label: <Link to="/qrmfg/admin">Admin Panel</Link> },
    { key: '/qrmfg/reports', icon: <FileSearchOutlined />, label: <Link to="/qrmfg/reports">Reports</Link> },
    { key: '/qrmfg/pendingtasks', icon: <CheckSquareOutlined />, label: <Link to="/qrmfg/pendingtasks">Pending Tasks</Link> },
    { key: '/qrmfg/jvc', icon: <AppstoreOutlined />, label: <Link to="/qrmfg/jvc">JVC</Link> },
    { key: '/qrmfg/cqs', icon: <SafetyOutlined />, label: <Link to="/qrmfg/cqs">CQS</Link> },
    { key: '/qrmfg/tech', icon: <TeamOutlined />, label: <Link to="/qrmfg/tech">TECH</Link> },
    { key: '/qrmfg/plant', icon: <BankOutlined />, label: <Link to="/qrmfg/plant">PLANT</Link> },
    { key: '/qrmfg/auditlogs', icon: <AuditOutlined />, label: <Link to="/qrmfg/auditlogs">Audit Logs</Link> },
    { key: '/qrmfg/users', icon: <UserOutlined />, label: <Link to="/qrmfg/users">Users</Link> },
    { key: '/qrmfg/roles', icon: <KeyOutlined />, label: <Link to="/qrmfg/roles">Roles</Link> },
    { key: '/qrmfg/sessions', icon: <ClockCircleOutlined />, label: <Link to="/qrmfg/sessions">Sessions</Link> },
    { key: '/qrmfg/user-role-management', icon: <UsergroupAddOutlined />, label: <Link to="/qrmfg/user-role-management">User Role Management</Link> },
    { key: '/qrmfg/workflow-monitoring', icon: <MonitorOutlined />, label: <Link to="/qrmfg/workflow-monitoring">Workflow Monitoring</Link> },
    { key: '/qrmfg/api-test', icon: <ApiOutlined />, label: <Link to="/qrmfg/api-test">API Test</Link> },
    { key: '/qrmfg/settings', icon: <SettingOutlined />, label: <Link to="/qrmfg/settings">Settings</Link> },
  ];

  // Show all menu items for admin, otherwise filter by allowed screens
  const filteredMenuItems = isAdmin() ? menuItems : menuItems.filter(item => allowedScreens.includes(item.key));

  return (
    <Sider
      collapsible
      collapsed={collapsed}
      onCollapse={(value) => setCollapsed(value)}
      style={{
        overflow: 'auto',
        height: '100vh',
        position: 'fixed',
        left: 0,
        top: 0,
        bottom: 0,
      }}
      width={250}
      collapsedWidth={80}
      breakpoint="lg"
    >
      <div style={{
        height: 64,
        margin: '16px',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: '#fff',
        fontSize: collapsed ? 14 : 20,
        whiteSpace: 'nowrap',
        overflow: 'hidden'
      }}>
        {collapsed ? 'QR' : 'QR Manufacturing'}
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[location.pathname]}
        items={filteredMenuItems}
        style={{
          borderRight: 0,
          flex: 1
        }}
      />
      <div style={{
        padding: collapsed ? '8px 8px' : '16px',
        marginTop: 'auto',
        marginBottom: '16px'
      }}>
        <Button
          type="primary"
          icon={<LogoutOutlined />}
          danger
          onClick={handleLogout}
          block
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: collapsed ? 'center' : 'flex-start'
          }}
        >
          {!collapsed && 'Logout'}
        </Button>
      </div>
    </Sider>
  );
};

export default Navigation; 