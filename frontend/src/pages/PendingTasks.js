import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Table, Typography, Spin, Alert } from 'antd';

const { Title } = Typography;

const PendingTasks = () => {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    axios.get('/api/v1/tasks/pending')
      .then(res => {
        setTasks(res.data);
        setLoading(false);
      })
      .catch(() => {
        setError('Failed to fetch pending tasks.');
        setLoading(false);
      });
  }, []);

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: 'Title', dataIndex: 'title', key: 'title' },
    { title: 'Description', dataIndex: 'description', key: 'description' },
    { title: 'Status', dataIndex: 'status', key: 'status' },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Pending Tasks</Title>
      {loading ? <Spin /> : error ? <Alert type="error" message={error} /> :
        <Table dataSource={tasks} columns={columns} rowKey="id" />
      }
    </div>
  );
};

export default PendingTasks; 