import React from 'react';
import { Table, Typography } from 'antd';

const { Title } = Typography;

const data = [
  { id: 1, name: 'Tech Task 1', status: 'pending' },
  { id: 2, name: 'Tech Task 2', status: 'completed' },
];

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: 'Name', dataIndex: 'name', key: 'name' },
  { title: 'Status', dataIndex: 'status', key: 'status' },
];

const TechView = () => (
  <div style={{ padding: 24 }}>
    <Title level={2}>Tech Dashboard</Title>
    <Table dataSource={data} columns={columns} rowKey="id" />
  </div>
);

export default TechView; 