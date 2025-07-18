import React from "react";
import { Layout, Menu, Button } from "antd";
import {
  UserOutlined,
  TeamOutlined,
  SafetyOutlined,
  AppstoreOutlined,
  FileProtectOutlined,
  DesktopOutlined,
  FileSearchOutlined,
  LogoutOutlined,
  SettingOutlined,
} from "@ant-design/icons";
import { Link, useLocation, useNavigate } from "react-router-dom";

const { Header, Sider, Content } = Layout;

const menuItems = [
  { key: "/users", icon: <UserOutlined />, label: <Link to="/users">Users</Link> },
  { key: "/roles", icon: <TeamOutlined />, label: <Link to="/roles">Roles</Link> },
  { key: "/permissions", icon: <SafetyOutlined />, label: <Link to="/permissions">Permissions</Link> },
  { key: "/field-permissions", icon: <FileProtectOutlined />, label: <Link to="/field-permissions">Field Permissions</Link> },
  { key: "/screens", icon: <DesktopOutlined />, label: <Link to="/screens">Screens</Link> },
  { key: "/audit-logs", icon: <FileSearchOutlined />, label: <Link to="/audit-logs">Audit Logs</Link> },
  { key: "/settings", icon: <SettingOutlined />, label: <Link to="/settings">Settings</Link> },
];

const AdminLayout = ({ children }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/login");
  };
  return (
    <Layout style={{ minHeight: "100vh" }}>
      <Sider breakpoint="lg" collapsedWidth="0" style={{ background: "#fff", display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
        <div>
          <div style={{ height: 64, margin: 16, textAlign: "center", fontWeight: "bold", fontSize: 22, color: "#1890ff" }}>
            RBAC Admin
          </div>
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            style={{ borderRight: 0 }}
            items={menuItems}
          />
        </div>
        <div style={{ padding: 16 }}>
          <Button
            type="primary"
            icon={<LogoutOutlined />}
            danger
            block
            onClick={handleLogout}
          >
            Logout
          </Button>
        </div>
      </Sider>
      <Layout>
        <Header style={{ background: "#fff", padding: 0, boxShadow: "0 2px 8px #f0f1f2" }}>
          {/* Removed logout from header */}
        </Header>
        <Content style={{ margin: "24px 16px 0", overflow: "initial" }}>
          <div style={{ padding: 24, background: "#fff", minHeight: 360, borderRadius: 8 }}>
            {children}
          </div>
        </Content>
      </Layout>
    </Layout>
  );
};

export default AdminLayout; 