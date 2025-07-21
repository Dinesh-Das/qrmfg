import React, { useEffect, useState } from 'react';
import { Table, Select, message, Typography } from 'antd';
import axios from 'axios';

const { Title } = Typography;

// Dynamically get all .js files in pages and subdirectories (excluding test files)
const pagesContext = require.context('../../pages', true, /^(?!.*\.test\.js$).*\.js$/);
const pageFiles = pagesContext.keys().map(f => f.replace('./', ''));

const getScreenRoute = (file) => {
  // Remove .js extension and split into parts
  const parts = file.replace('.js', '').split('/');
  const name = parts[parts.length - 1];
  
  // Special cases
  if (name.toLowerCase() === 'home') return '/qrmfg';
  
  // Handle admin panel routes
  if (parts.length > 1 && parts[0] === 'AdminPanel') {
    const subRoute = name.toLowerCase();
    return `/qrmfg/admin/${subRoute}`;
  }
  
  // Handle normal routes
  const routeName = name.replace(/View$/i, '').toLowerCase(); // Remove 'View' suffix if present
  return `/qrmfg/${routeName}`;
};

const getScreenName = (file) => {
  const parts = file.replace('.js', '').split('/');
  const name = parts[parts.length - 1].replace(/View$/i, '');
  
  // Convert camelCase to space-separated words
  return name.replace(/([A-Z])/g, ' $1').trim();
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
      const res = await axios.get('/qrmfg/api/v1/admin/roles');
      setAllRoles(res.data);
    } catch {}
  };

  const fetchMappings = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/qrmfg/api/v1/admin/screen-role-mapping');
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
      await axios.put('/qrmfg/api/v1/admin/screen-role-mapping', { route, roles });
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
      sorter: (a, b) => a.name.localeCompare(b.name)
    },
    {
      title: 'Route',
      dataIndex: 'route',
      key: 'route',
      sorter: (a, b) => a.route.localeCompare(b.route)
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

  const data = pageFiles
    .filter(file => !file.includes('.test.js')) // Exclude test files
    .map(file => {
      const name = getScreenName(file);
      const route = getScreenRoute(file);
      return { key: route, name, route };
    })
    .sort((a, b) => a.name.localeCompare(b.name)); // Sort by name

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