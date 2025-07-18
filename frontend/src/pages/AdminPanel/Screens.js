import React, { useEffect, useState } from 'react';
import { Table, Button, Select, message, Typography } from 'antd';
import axios from 'axios';

const { Title } = Typography;

// List of all page files (auto-generated or hardcoded for now)
const pageFiles = [
  'Home.js', 'Users.js', 'Roles.js', 'Permissions.js', 'Groups.js', 'Screens.js', 'AuditLogs.js', 'Sessions.js', 'Profile.js', 'Dashboard.js', 'Reports.js', 'SystemDashboard.js', 'Login.js', 'ForgotPassword.js', 'ResetPassword.js', 'VerifyEmail.js', 'ResendVerification.js', 'Settings.js'
];

const getScreenRoute = (file) => {
  const name = file.replace('.js', '');
  if (name.toLowerCase() === 'home') return '/';
  return '/' + name.replace(/([A-Z])/g, (m, p1, o) => (o > 0 ? '-' : '') + p1.toLowerCase());
};

const Screens = () => {
  const [roleMappings, setRoleMappings] = useState({}); // { route: [roleId, ...] }
  const [allRoles, setAllRoles] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchRoles();
    fetchMappings();
  }, []);

  const fetchRoles = async () => {
    try {
      const res = await axios.get('/api/v1/admin/roles');
      setAllRoles(res.data);
    } catch {}
  };

  const fetchMappings = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/api/v1/admin/screen-role-mapping');
      setRoleMappings(res.data); // { route: [roleId, ...] }
    } catch {
      setRoleMappings({});
    } finally {
      setLoading(false);
    }
  };

  const handleRoleChange = async (route, roles) => {
    setRoleMappings((prev) => ({ ...prev, [route]: roles }));
    try {
      await axios.put('/api/v1/admin/screen-role-mapping', { route, roles });
      message.success('Roles updated for screen');
    } catch {
      message.error('Failed to update roles');
    }
  };

  const columns = [
    {
      title: 'Screen',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'Route',
      dataIndex: 'route',
      key: 'route',
    },
    {
      title: 'Roles',
      key: 'roles',
      render: (_, record) => (
        <Select
          mode="multiple"
          allowClear
          style={{ minWidth: 200 }}
          placeholder="Assign roles"
          value={roleMappings[record.route] || []}
          onChange={(roles) => handleRoleChange(record.route, roles)}
          options={allRoles.map(r => ({ value: r.id, label: r.name }))}
        />
      ),
    },
  ];

  const data = pageFiles.map(file => {
    const name = file.replace('.js', '');
    const route = getScreenRoute(file);
    return { key: route, name, route };
  });

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Screens (Auto-generated from pages)</Title>
      <Table
        columns={columns}
        dataSource={data}
        rowKey="route"
        loading={loading}
        bordered
        pagination={false}
      />
    </div>
  );
};

export default Screens; 