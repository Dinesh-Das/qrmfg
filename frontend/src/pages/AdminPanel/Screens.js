import React, { useEffect, useState } from 'react';
import { Table, Button, Select, message, Typography } from 'antd';
import axios from 'axios';

const { Title } = Typography;

// Dynamically get all .js files in pages and subdirectories (excluding test files)
const pagesContext = require.context('../../pages', true, /^(?!.*\.test\.js$).*\.js$/);
const pageFiles = pagesContext.keys().map(f => f.replace('./', ''));

const getScreenRoute = (file) => {
  let name = file.replace('.js', '').split('/').pop();
  name = name.replace(/View$/i, ''); // Remove 'View' suffix if present
  if (name.toLowerCase() === 'home') return '/';
  return '/' + name.toLowerCase();
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
    const name = file.replace('.js', '').split('/').pop();
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