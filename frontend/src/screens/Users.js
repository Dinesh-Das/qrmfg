import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Select, message } from 'antd';
import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { apiGet, apiPost, apiPut, apiDelete } from '../api/api';

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
      const data = await apiGet('/admin/users');
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      message.error('Failed to fetch users');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchRoles = async () => {
    try {
      const data = await apiGet('/admin/roles');
      setRoles(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Failed to fetch roles:', err);
      message.error('Failed to fetch roles');
      setRoles([]);
    }
  };

  useEffect(() => {
    fetchUsers();
    fetchRoles();
  }, []);

  const handleDelete = async (userId) => {
    try {
      await apiDelete(`/admin/users/${userId}`);
      message.success('User deleted successfully');
      fetchUsers();
    } catch (err) {
      message.error('Failed to delete user');
    }
  };

  const handleEdit = (user) => {
    setEditingUser(user);
    form.setFieldsValue({
      username: user.username,
      email: user.email,
      roles: (user.roles || []).map(r => r?.id).filter(Boolean)
    });
    setModalVisible(true);
  };

  const handleAdd = () => {
    setEditingUser(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleModalOk = () => {
    form.submit();
  };

  const handleModalCancel = () => {
    setModalVisible(false);
    form.resetFields();
  };

  const onFinish = async (values) => {
    try {
      if (editingUser) {
        await apiPut(`/admin/users/${editingUser.id}`, values);
        message.success('User updated successfully');
      } else {
        await apiPost('/admin/users', values);
        message.success('User created successfully');
      }
      setModalVisible(false);
      fetchUsers();
    } catch (err) {
      message.error('Failed to save user');
    }
  };

  const columns = [
    {
      title: 'Username',
      dataIndex: 'username',
      key: 'username',
      sorter: (a, b) => a.username.localeCompare(b.username)
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
      sorter: (a, b) => a.email.localeCompare(b.email)
    },
    {
      title: 'Roles',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles) => (roles || []).map(role => role?.name || '').filter(Boolean).join(', ')
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            Edit
          </Button>
          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            Delete
          </Button>
        </>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={handleAdd}>
          Add User
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"
        loading={loading}
      />
      <Modal
        title={editingUser ? 'Edit User' : 'Add User'}
        open={modalVisible}
        onOk={handleModalOk}
        onCancel={handleModalCancel}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
        >
          <Form.Item
            name="username"
            label="Username"
            rules={[{ required: true, message: 'Please input username!' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            name="email"
            label="Email"
            rules={[
              { required: true, message: 'Please input email!' },
              { type: 'email', message: 'Please input valid email!' }
            ]}
          >
            <Input />
          </Form.Item>
          {!editingUser && (
            <Form.Item
              name="password"
              label="Password"
              rules={[{ required: true, message: 'Please input password!' }]}
            >
              <Input.Password />
            </Form.Item>
          )}
          <Form.Item
            name="roles"
            label="Roles"
            rules={[{ required: true, message: 'Please select at least one role!' }]}
          >
            <Select
              mode="multiple"
              placeholder="Select roles"
              options={roles.map(role => ({
                label: role.name,
                value: role.id
              }))}
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Users; 