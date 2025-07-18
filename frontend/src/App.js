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
import ResetPassword from './pages/ResetPassword';
import VerifyEmail from './pages/VerifyEmail';
import Dashboard from './pages/Dashboard';
import Reports from './pages/Reports';
import SystemDashboard from './pages/SystemDashboard';
import Home from './pages/Home';
import AdminPanel from './pages/AdminPanel';
import { isAuthenticated } from './utils/auth';


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
    '/login',
    '/forgot-password',
    '/reset-password',
    '/verify-email',
    '/resend-verification'
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
              <Route path="/reset-password" element={<ResetPassword />} />
              <Route path="/verify-email" element={<VerifyEmail />} />
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
              <Route path="/system" element={
                <ProtectedRoute>
                  <SystemDashboard />
                </ProtectedRoute>
              } />
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
