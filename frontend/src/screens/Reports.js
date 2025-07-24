import React, { useEffect, useState } from 'react';
import { Table, Tabs, Button, Typography, message } from 'antd';
import axios from 'axios';
import { apiRequest } from '../api/api';

const { Title } = Typography;
const { TabPane } = Tabs;

const exportToCSV = (data, filename) => {
  if (!data || !data.length) return;
  const keys = Object.keys(data[0]);
  const csv = [keys.join(',')].concat(
    data.map(row => keys.map(k => JSON.stringify(row[k] ?? '')).join(','))
  ).join('\n');
  const blob = new Blob([csv], { type: 'text/csv' });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  window.URL.revokeObjectURL(url);
};

const Reports = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [activity, setActivity] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      apiRequest('/reports/users'),
      apiRequest('/reports/roles'),
      apiRequest('/reports/activity'),
    ]).then(([users, roles, activity]) => {
      setUsers(users);
      setRoles(roles);
      setActivity(activity);
    }).catch(() => {
      message.error('Failed to fetch reports');
    }).finally(() => setLoading(false));
  }, []);

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Reports</Title>
      <Tabs defaultActiveKey="users">
        <TabPane tab="Users" key="users">
          <Button onClick={() => exportToCSV(users, 'users_report.csv')} style={{ marginBottom: 8 }}>Export CSV</Button>
          <Table dataSource={users} rowKey="id" loading={loading} bordered scroll={{ x: true }} />
        </TabPane>
        <TabPane tab="Roles" key="roles">
          <Button onClick={() => exportToCSV(roles, 'roles_report.csv')} style={{ marginBottom: 8 }}>Export CSV</Button>
          <Table dataSource={roles} rowKey="id" loading={loading} bordered scroll={{ x: true }} />
        </TabPane>
        <TabPane tab="Activity" key="activity">
          <Button onClick={() => exportToCSV(activity, 'activity_report.csv')} style={{ marginBottom: 8 }}>Export CSV</Button>
          <Table dataSource={activity} rowKey="id" loading={loading} bordered scroll={{ x: true }} />
        </TabPane>
      </Tabs>
    </div>
  );
};

export default Reports; 