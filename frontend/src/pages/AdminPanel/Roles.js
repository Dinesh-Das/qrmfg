import React, { useEffect, useState } from 'react';
import { Table, Button, Modal, Form, Input, message, Popconfirm, Space, Typography, Select } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import axios from 'axios';

const { Title } = Typography;

const Roles = () => {
  const [roles, setRoles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRole, setEditingRole] = useState(null);
  const [form] = Form.useForm();
  const [allRoles, setAllRoles] = useState([]);

  const fetchRoles = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/api/v1/admin/roles');
      setRoles(res.data);
    } catch (err) {
      message.error('Failed to fetch roles');
    } finally {
      setLoading(false);
    }
  };

  const fetchAllRoles = async () => {
    try {
      const res = await axios.get('/api/v1/admin/roles');
      setAllRoles(res.data);
    } catch {}
  };

  useEffect(() => {
    fetchRoles();
    fetchAllRoles();
  }, []);

  const handleAdd = () => {
    setEditingRole(null);
    form.resetFields();
    setModalVisible(true);
  };

  const handleEdit = (role) => {
    setEditingRole(role);
    form.setFieldsValue({
      ...role
    });
    setModalVisible(true);
  };

  const handleDelete = async (roleId) => {
    setLoading(true);
    try {
      await axios.delete(`/api/v1/admin/roles/${roleId}`);
      message.success('Role deleted');
      fetchRoles();
    } catch (err) {
      message.error('Failed to delete role');
    } finally {
      setLoading(false);
    }
  };

  const handleModalOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const payload = { ...values };
      if (editingRole) {
        await axios.put(`/api/v1/admin/roles/${editingRole.id}`, payload);
        message.success('Role updated');
      } else {
        await axios.post('/api/v1/admin/roles', payload);
        message.success('Role created');
      }
      setModalVisible(false);
      fetchRoles();
      fetchAllRoles();
    } catch (err) {
      if (err.response && err.response.data && err.response.data.message) {
        message.error(err.response.data.message);
      } else if (err.errorFields) {
        // Validation error, do nothing
      } else {
        message.error('Failed to save role');
      }
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    {
      title: 'Role Name',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: 'Description',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Button icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Popconfirm title="Delete this role?" onConfirm={() => handleDelete(record.id)} okText="Yes" cancelText="No">
            <Button icon={<DeleteOutlined />} danger />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Roles</Title>
      <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd} style={{ marginBottom: 16 }}>
        Add Role
      </Button>
      <Table
        columns={columns}
        dataSource={roles}
        rowKey="id"
        loading={loading}
        bordered
      />
      <Modal
        title={editingRole ? 'Edit Role' : 'Add Role'}
        visible={modalVisible}
        onOk={handleModalOk}
        onCancel={() => setModalVisible(false)}
        okText="Save"
        confirmLoading={loading}
        destroyOnClose
      >
        <Form form={form} layout="vertical" initialValues={{ name: '', description: '' }}>
          <Form.Item
            name="name"
            label="Role Name"
            rules={[{ required: true, message: 'Please enter the role name' }]}
          >
            <Input placeholder="Enter role name" />
          </Form.Item>
          <Form.Item
            name="description"
            label="Description"
            rules={[{ required: true, message: 'Please enter a description' }]}
          >
            <Input placeholder="Enter description" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Roles; 