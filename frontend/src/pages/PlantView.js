import React from 'react';
import { Table, Typography } from 'antd';

const { Title } = Typography;

const data = [
  { id: 1, name: 'Plant Task 1', status: 'pending' },
  { id: 2, name: 'Plant Task 2', status: 'completed' },
];

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: 'Name', dataIndex: 'name', key: 'name' },
  { title: 'Status', dataIndex: 'status', key: 'status' },
];

const PlantView = () => (
  <div style={{ padding: 24 }}>
    <Title level={2}>Plant Dashboard</Title>
    <Table dataSource={data} columns={columns} rowKey="id" />
  </div>
);

export default PlantView; 