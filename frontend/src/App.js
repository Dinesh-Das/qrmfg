import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Layout, notification } from 'antd';
import 'antd/dist/reset.css';
import './App.css';
import Login from './pages/Login';
import ProtectedRoute from './components/ProtectedRoute';
import Navigation from './components/Navigation';
import Users from './pages/AdminPanel/Users';
import Roles from './pages/AdminPanel/Roles';
import AuditLogs from './pages/AdminPanel/AuditLogs';
import Screens from './pages/AdminPanel/Screens';
import Settings from './pages/Settings';

import Dashboard from './pages/Dashboard';
import Reports from './pages/Reports';
import Home from './pages/Home';
import AdminPanel from './pages/AdminPanel';
import PendingTasks from './pages/PendingTasks';
import { isAuthenticated } from './utils/auth';
import JVCView from './pages/JVCView';
import CQSView from './pages/CQSView';
import TechView from './pages/TechView';
import PlantView from './pages/PlantView';


const { Header, Content, Footer, Sider } = Layout;

function App() {
  // Example notification usage
  const openNotification = () => {
    notification.open({
      message: 'Welcome',
      description: 'This is a modern RBAC system UI.',
    });
  };

  React.useEffect(() => {
    openNotification();
  }, []);

  // Placeholder for user roles, to be fetched from backend/user context
  const userRoles = [];

  const location = window.location.pathname;
  const unauthRoutes = [
    '/login'
  ];
  const showSidebar = !unauthRoutes.includes(location) && isAuthenticated();

  return (
    <Router>
      <Layout style={{ minHeight: '100vh' }}>
        {showSidebar && (
          <Sider collapsible>
            <div className="logo" />
            <Navigation />
          </Sider>
        )}
        <Layout style={showSidebar ? {} : { marginLeft: 0 }}>
          <Header style={{ background: '#fff', padding: 0 }} />
          <Content style={{ margin: '16px' }}>
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/admin" element={<ProtectedRoute><AdminPanel /></ProtectedRoute>} />
              <Route path="/settings" element={
                <ProtectedRoute>
                  <Settings />
                </ProtectedRoute>
              } />
              <Route path="/dashboard" element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              } />
              <Route path="/reports" element={
                <ProtectedRoute>
                  <Reports />
                </ProtectedRoute>
              } />
              <Route path="/pending-tasks" element={<ProtectedRoute><PendingTasks /></ProtectedRoute>} />
              <Route path="/jvc" element={<ProtectedRoute><JVCView /></ProtectedRoute>} />
              <Route path="/cqs" element={<ProtectedRoute><CQSView /></ProtectedRoute>} />
              <Route path="/tech" element={<ProtectedRoute><TechView /></ProtectedRoute>} />
              <Route path="/plant" element={<ProtectedRoute><PlantView /></ProtectedRoute>} />
              <Route path="/" element={
                <ProtectedRoute>
                  <Home />
                </ProtectedRoute>
              } />
              
            </Routes>
          </Content>
          <Footer style={{ textAlign: 'center' }}>
            RBAC System ©{new Date().getFullYear()} Created by Your Company
          </Footer>
        </Layout>
      </Layout>
    </Router>
  );
}

export default App;
