import React, { useState, useEffect } from 'react';
import { Typography, Table, Tag, Button, message } from 'antd';
import { apiRequest } from '../api/api';

const { Title } = Typography;

const PendingTasks = () => {
  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchWorkflows = async () => {
    setLoading(true);
    try {
      const response = await apiRequest('/workflows/pending');
      setWorkflows(response);
    } catch (error) {
      message.error('Failed to fetch pending workflows');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWorkflows();
  }, []);

  const columns = [
    {
      title: 'Workflow ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: 'Material Code',
      dataIndex: 'materialCode',
      key: 'materialCode',
    },
    {
      title: 'Material Name',
      dataIndex: 'materialName',
      key: 'materialName',
    },
    {
      title: 'Plant',
      dataIndex: 'assignedPlant',
      key: 'assignedPlant',
    },
    {
      title: 'Days Pending',
      dataIndex: 'daysPending',
      key: 'daysPending',
      render: (days) => (
        <Tag color={days > 7 ? 'red' : days > 3 ? 'orange' : 'green'}>
          {days} days
        </Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'currentState',
      key: 'currentState',
      render: (state) => (
        <Tag color={state === 'JVC_PENDING' ? 'orange' : state === 'PLANT_PENDING' ? 'blue' : 'green'}>
          {state}
        </Tag>
      ),
    },
    {
      title: 'Overdue',
      dataIndex: 'overdue',
      key: 'overdue',
      render: (overdue) => (
        overdue ? <Tag color="red">OVERDUE</Tag> : <Tag color="green">ON TIME</Tag>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Button type="primary" onClick={() => handleWorkflowAction(record.id)}>
          Extend to Plant
        </Button>
      ),
    },
  ];

  const handleWorkflowAction = async (workflowId) => {
    try {
      await apiRequest(`/workflows/${workflowId}/extend`, { method: 'PUT' });
      message.success('Workflow extended successfully');
      fetchWorkflows();
    } catch (error) {
      message.error('Failed to extend workflow');
    }
  };

  return (
    <div>
      <Title level={2}>Pending Workflows</Title>
      <Table
        columns={columns}
        dataSource={workflows}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
      />
    </div>
  );
};

export default PendingTasks; 