import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, message, Popconfirm, Space, Typography, Select, Switch } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import axios from 'axios';
import { hasPermission } from '../../utils/auth';
import { notifySuccess, notifyError } from '../../utils/notify';

const { Title } = Typography;
const { Option } = Select;

const statusOptions = [
  { label: 'Active', value: 'ACTIVE' },
  { label: 'Inactive', value: 'INACTIVE' },
  { label: 'Locked', value: 'LOCKED' },
];

const Users = () => {
  const [users, setUsers] = useState([]);
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [form] = Form.useForm();

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/api/v1/admin/users');
      setUsers(res.data);
    } catch (err) {
      notifyError('Fetch Failed', 'Failed to fetch users');
    } finally {
      setLoading(false);
    }
  };

  const fetchRoles = async () => {
    try {
      const res = await axios.get('/api/v1/admin/roles');
      setRoles(res.data);
    } catch (err) {
      // ignore
    }
  };

  useEffect(() => {
    fetchUsers();
    fetchRoles();
  }, []);

  const handleAdd = () => {
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (user) => {
    setEditingUser(user);
    form.setFieldsValue({
      ...user,
      roles: user.roles ? user.roles.map(r => r.id) : [],
      password: '', // don't show password
    });
    setModalVisible(true);
  };

  const handleDelete = async (userId) => {
    setLoading(true);
    try {
      await axios.delete(`/api/v1/admin/users/${userId}`);
      notifySuccess('User Deleted', 'The user was deleted successfully.');
      fetchUsers();
    } catch (err) {
      notifyError('Delete Failed', 'Failed to delete user');
    } finally {
      setLoading(false);
    }
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const payload = { ...values };
      // Map role IDs to objects
      payload.roles = (values.roles || []).map(id => ({ id }));
      if (!editingUser) {
        // password required for new user
        if (!payload.password) {
          notifyError('Save Failed', 'Password is required');
          setLoading(false);
          return;
        }
      } else {
        // don't send password if not changed
        if (!payload.password) {
          delete payload.password;
        }
      }
      if (editingUser) {
        await axios.put(`/api/v1/admin/users/${editingUser.id}`, payload);
        notifySuccess('User Updated', 'The user was updated successfully.');
      } else {
        await axios.post('/api/v1/admin/users', payload);
        notifySuccess('User Created', 'The user was created successfully.');
      }
      setModalVisible(false);
      fetchUsers();
    } catch (err) {
      if (err.response && err.response.data && err.response.data.message) {
        notifyError('Save Failed', err.response.data.message);
      } else if (err.errorFields) {
        // Validation error, do nothing
      } else {
        notifyError('Save Failed', 'Failed to save user');
      }
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: 'Username',
      dataIndex: 'username',
      key: 'username',
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'Roles',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles) => roles && roles.length ? roles.map(r => r.name).join(', ') : '-',
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
    },
    {
      title: 'Enabled',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled) => enabled ? 'Yes' : 'No',
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Popconfirm title="Delete this user?" onConfirm={() => handleDelete(record.id)} okText="Yes" cancelText="No">
            <Button icon={<DeleteOutlined />} danger />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Users</Title>
      <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>
        Add User
      </Button>
      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"
        loading={loading}
        bordered
      />
      <Modal
        title={editingUser ? 'Edit User' : 'Add User'}
        visible={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        okText="Save"
        confirmLoading={loading}
        destroyOnClose
      >
        <Form form={form} layout="vertical" initialValues={{ username: '', email: '', password: '', roles: [], status: 'ACTIVE', enabled: true }}>
          <Form.Item
            name="username"
            label="Username"
            rules={[{ required: true, message: 'Please enter the username' }]}
          >
            <Input placeholder="Enter username" />
          </Form.Item>
          <Form.Item
            name="email"
            label="Email"
            rules={[{ required: true, type: 'email', message: 'Please enter a valid email' }]}
          >
            <Input placeholder="Enter email" />
          </Form.Item>
          <Form.Item
            name="password"
            label="Password"
            rules={editingUser ? [] : [
              { required: true, message: 'Please enter a password' },
              { min: 6, message: 'Password must be at least 6 characters' }
            ]}
          >
            <Input.Password placeholder={editingUser ? 'Leave blank to keep unchanged' : 'Enter password'} />
          </Form.Item>
          <Form.Item
            name="roles"
            label="Roles"
            rules={[{ required: true, message: 'Please select at least one role' }]}
          >
            <Select mode="multiple" placeholder="Select roles">
              {roles.map(role => (
                <Option key={role.id} value={role.id}>{role.name}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="status"
            label="Status"
            rules={[{ required: true, message: 'Please select a status' }]}
          >
            <Select>
              {statusOptions.map(opt => (
                <Option key={opt.value} value={opt.value}>{opt.label}</Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item
            name="enabled"
            label="Enabled"
            valuePropName="checked"
          >
            <Switch checkedChildren="Enabled" unCheckedChildren="Disabled" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Users; 