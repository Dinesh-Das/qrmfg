import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { Layout, notification } from 'antd';
import 'antd/dist/reset.css';
import './App.css';
import Login from './pages/Login';
import ProtectedRoute from './components/ProtectedRoute';
import Navigation from './components/Navigation';
import Settings from './pages/Settings';
import Dashboard from './pages/Dashboard';
import Reports from './pages/Reports';
import Home from './pages/Home';
import AdminPanel from './pages/admin';
import { isAuthenticated } from './utils/auth';
import JVCView from './pages/JVCView';
import CQSView from './pages/CQSView';
import TechView from './pages/TechView';
import PlantView from './pages/PlantView';
import PendingTasks from './pages/PendingTasks';
import SystemDashboard from './pages/SystemDashboard';
import WorkflowPage from './pages/WorkflowPage';

const { Header, Content, Footer } = Layout;

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
