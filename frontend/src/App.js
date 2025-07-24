import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Layout, notification } from 'antd';
import 'antd/dist/reset.css';
import './App.css';
import { useActivityTracking } from './components/useActivityTracking';
import Login from './screens/Login';
import ProtectedRoute from './components/ProtectedRoute';
import Navigation from './components/Navigation';
import Settings from './screens/Settings';
import Dashboard from './screens/Dashboard';
import Reports from './screens/Reports';
import Home from './screens/Home';
import AdminPanel from './screens/admin';
import { isAuthenticated } from './services/auth';
import JVCView from './screens/JVCView';
import CQSView from './screens/CQSView';
import TechView from './screens/TechView';
import PlantView from './screens/PlantView';
import PendingTasks from './screens/PendingTasks';
import SystemDashboard from './screens/SystemDashboard';
import WorkflowPage from './screens/WorkflowPage';
import ApiTest from './screens/ApiTest';
import Auditlogs from './screens/Auditlogs';
import Users from './screens/Users';
import Roles from './screens/Roles';
import Sessions from './screens/Sessions';
import UserRoleManagement from './screens/UserRoleManagement';
import WorkflowMonitoring from './screens/WorkflowMonitoring';

const { Header, Content, Footer } = Layout;

function App() {
  // Activity tracking
  const { trackAction } = useActivityTracking('app', true);

  // Example notification usage
  const openNotification = () => {
    notification.open({
      message: 'Welcome',
      description: 'This is a modern RBAC system UI.',
    });
  };

  React.useEffect(() => {
    openNotification();
    trackAction('app_start');
  }, [trackAction]);

  const location = window.location.pathname;
  const unauthRoutes = [
    '/qrmfg/login'
  ];
  const showSidebar = !unauthRoutes.includes(location) && isAuthenticated();

  return (
    <Router>
      <Layout>
        {showSidebar && <Navigation />}
        <Layout style={{ 
          minHeight: '100vh',
          marginLeft: showSidebar ? 250 : 0,
          transition: 'margin-left 0.2s'
        }}>
          <Header style={{ 
            padding: '0 24px',
            background: '#fff',
            position: 'sticky',
            top: 0,
            zIndex: 1,
            width: '100%',
            display: 'flex',
            alignItems: 'center'
          }} />
          <Content style={{ 
            margin: '24px 16px',
            padding: 24,
            minHeight: 280,
            background: '#fff',
            borderRadius: '4px'
          }}>
            <Routes>
              <Route path="/qrmfg/login" element={<Login />} />
              <Route path="/qrmfg/admin/*" element={<ProtectedRoute><AdminPanel /></ProtectedRoute>} />
              <Route path="/qrmfg/settings" element={<ProtectedRoute><Settings /></ProtectedRoute>} />
              <Route path="/qrmfg/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
              <Route path="/qrmfg/reports" element={<ProtectedRoute><Reports /></ProtectedRoute>} />
              <Route path="/qrmfg/pendingtasks" element={<ProtectedRoute><PendingTasks /></ProtectedRoute>} />
              <Route path="/qrmfg/jvc" element={<ProtectedRoute><JVCView /></ProtectedRoute>} />
              <Route path="/qrmfg/cqs" element={<ProtectedRoute><CQSView /></ProtectedRoute>} />
              <Route path="/qrmfg/tech" element={<ProtectedRoute><TechView /></ProtectedRoute>} />
              <Route path="/qrmfg/plant" element={<ProtectedRoute><PlantView /></ProtectedRoute>} />
              <Route path="/qrmfg/systemdashboard" element={<ProtectedRoute><SystemDashboard /></ProtectedRoute>} />
              <Route path="/qrmfg/workflows" element={<ProtectedRoute><WorkflowPage /></ProtectedRoute>} />
              <Route path="/qrmfg/auditlogs" element={<ProtectedRoute><Auditlogs /></ProtectedRoute>} />
              <Route path="/qrmfg/users" element={<ProtectedRoute><Users /></ProtectedRoute>} />
              <Route path="/qrmfg/roles" element={<ProtectedRoute><Roles /></ProtectedRoute>} />
              <Route path="/qrmfg/sessions" element={<ProtectedRoute><Sessions /></ProtectedRoute>} />
              <Route path="/qrmfg/user-role-management" element={<ProtectedRoute><UserRoleManagement /></ProtectedRoute>} />
              <Route path="/qrmfg/workflow-monitoring" element={<ProtectedRoute><WorkflowMonitoring /></ProtectedRoute>} />
              <Route path="/qrmfg/api-test" element={<ProtectedRoute><ApiTest /></ProtectedRoute>} />
              <Route path="/qrmfg" element={<ProtectedRoute><Home /></ProtectedRoute>} />
              <Route path="/" element={<ProtectedRoute><Home /></ProtectedRoute>} />
            </Routes>
          </Content>
          <Footer style={{ 
            textAlign: 'center',
            padding: '16px'
          }}>
            RBAC System ©{new Date().getFullYear()} Created by Your Company
          </Footer>
        </Layout>
      </Layout>
    </Router>
  );
}

export default App;
