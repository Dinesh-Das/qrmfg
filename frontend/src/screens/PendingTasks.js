import React, { useState, useEffect } from 'react';
import { Typography, Table, Tag, Button, message } from 'antd';
import axios from 'axios';
import { apiRequest } from '../api/api';

const { Title } = Typography;

const PendingTasks = () => {
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchTasks = async () => {
    setLoading(true);
    try {
      const response = await apiRequest('/tasks/pending');
      setTasks(response);
    } catch (error) {
      message.error('Failed to fetch pending tasks');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTasks();
  }, []);

  const columns = [
    {
      title: 'Task ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: 'Priority',
      dataIndex: 'priority',
      key: 'priority',
      render: (priority) => (
        <Tag color={priority === 'HIGH' ? 'red' : priority === 'MEDIUM' ? 'orange' : 'green'}>
          {priority}
        </Tag>
      ),
    },
    {
      title: 'Due Date',
      dataIndex: 'dueDate',
      key: 'dueDate',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === 'PENDING' ? 'orange' : 'green'}>
          {status}
        </Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Button type="primary" onClick={() => handleTaskAction(record.id)}>
          Process
        </Button>
      ),
    },
  ];

  const handleTaskAction = async (taskId) => {
    try {
      await apiRequest(`/tasks/${taskId}/process`, { method: 'POST' });
      message.success('Task processed successfully');
      fetchTasks();
    } catch (error) {
      message.error('Failed to process task');
    }
  };

  return (
    <div>
      <Title level={2}>Pending Tasks</Title>
      <Table
        columns={columns}
        dataSource={tasks}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
      />
    </div>
  );
};

export default PendingTasks; 