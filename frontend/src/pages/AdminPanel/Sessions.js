import React, { useEffect, useState } from 'react';
import { Table, Button, Popconfirm, Space, Typography, message, Form } from 'antd';
import { DeleteOutlined } from '@ant-design/icons';
import axios from 'axios';
import { notifySuccess, notifyError } from '../../utils/notify';

const { Title } = Typography;

const Sessions = ({ embedded }) => {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingSession, setEditingSession] = useState(null);
  const [form] = Form.useForm();

  const fetchSessions = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/api/v1/admin/sessions');
      setSessions(res.data);
    } catch (err) {
      notifyError('Fetch Failed', 'Failed to fetch sessions');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchSessions();
  }, []);

  const handleAdd = () => {
    setEditingSession(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (session) => {
    setEditingSession(session);
    form.setFieldsValue(session);
    setModalVisible(true);
  };

  const handleDelete = async (id) => {
    setLoading(true);
    try {
      await axios.delete(`/api/v1/admin/sessions/${id}`);
      notifySuccess('Session Terminated', 'The session was terminated successfully.');
      fetchSessions();
    } catch (err) {
      notifyError('Delete Failed', 'Failed to terminate session');
    } finally {
      setLoading(false);
    }
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      if (editingSession) {
        await axios.put(`/api/v1/admin/sessions/${editingSession.id}`, values);
        notifySuccess('Session Updated', 'The session was updated successfully.');
      } else {
        await axios.post('/api/v1/admin/sessions', values);
        notifySuccess('Session Created', 'The session was created successfully.');
      }
      setModalVisible(false);
      fetchSessions();
    } catch (err) {
      if (err.response && err.response.data && err.response.data.message) {
        notifyError('Save Failed', err.response.data.message);
      } else if (err.errorFields) {
        // Validation error, do nothing
      } else {
        notifyError('Save Failed', 'Failed to save session');
      }
    } finally {
      setLoading(false);
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
      render: (user) => user && user.username ? user.username : user,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Popconfirm title="Terminate this session?" onConfirm={() => handleDelete(record.id)} okText="Yes" cancelText="No">
            <Button icon={<DeleteOutlined />} danger />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Sessions</Title>
      <Table
        columns={columns}
        dataSource={sessions}
        rowKey="id"
        loading={loading}
        bordered
      />
    </div>
  );
};

export default Sessions; 