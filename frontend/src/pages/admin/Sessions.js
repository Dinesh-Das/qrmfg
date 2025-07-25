import React, { useState, useEffect } from 'react';
import { Table, Button, Typography } from 'antd';
import axios from 'axios';

const { Title } = Typography;

const Sessions = () => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);

  const fetchSessions = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/qrmfg/api/v1/admin/sessions');
      setSessions(response.data);
    } catch (error) {
      console.error('Failed to fetch sessions:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSessions();
  }, []);

  const handleTerminate = async (sessionId) => {
    try {
      await axios.delete(`/qrmfg/api/v1/admin/sessions/${sessionId}`);
      fetchSessions();
    } catch (error) {
      console.error('Failed to terminate session:', error);
    }
  };

  const columns = [
    {
      title: 'Session ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: 'User',
      dataIndex: 'user',
      key: 'user',
    },
    {
      title: 'Start Time',
      dataIndex: 'startTime',
      key: 'startTime',
    },
    {
      title: 'Last Activity',
      dataIndex: 'lastActivity',
      key: 'lastActivity',
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Button type="primary" danger onClick={() => handleTerminate(record.id)}>
          Terminate
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Title level={2}>Active Sessions</Title>
      <Table
        columns={columns}
        dataSource={sessions}
        rowKey="id"
        loading={loading}
      />
    </div>
  );
};

export default Sessions; 