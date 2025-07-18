import React, { useEffect, useState } from 'react';
import { Table, message, Typography, Form, Input, Select, DatePicker, Button, Row, Col } from 'antd';
import axios from 'axios';
import moment from 'moment';

const { Title } = Typography;

const AuditLogs = () => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [filterForm] = Form.useForm();
  const [filtering, setFiltering] = useState(false);

  const fetchLogs = async () => {
    setLoading(true);
    try {
      const res = await axios.get('/api/v1/audit/logs');
      setLogs(res.data);
    } catch (err) {
      message.error('Failed to fetch audit logs');
    } finally {
      setLoading(false);
    }
  };

  const fetchFilteredLogs = async (filters) => {
    setLoading(true);
    let url = '/api/v1/audit/logs';
    if (filters) {
      if (filters.userId) url = `/api/v1/audit/logs/user/${filters.userId}`;
      else if (filters.action) url = `/api/v1/audit/logs/action/${filters.action}`;
      else if (filters.entityType) url = `/api/v1/audit/logs/entity/${filters.entityType}`;
      else if (filters.severity) url = `/api/v1/audit/logs/severity/${filters.severity}`;
      else if (filters.dateRange && filters.dateRange.length === 2) {
        const [start, end] = filters.dateRange;
        url = `/api/v1/audit/logs/date?start=${start.toISOString()}&end=${end.toISOString()}`;
      }
    }
    try {
      const res = await axios.get(url);
      setLogs(res.data);
    } catch (err) {
      message.error('Failed to fetch audit logs');
    } finally {
      setLoading(false);
    }
  };

  const onFilter = (values) => {
    setFiltering(true);
    fetchFilteredLogs(values).then(() => setFiltering(false));
  };

  const onReset = () => {
    filterForm.resetFields();
    fetchLogs();
  };

  useEffect(() => {
    fetchLogs();
  }, []);

  const columns = [
    {
      title: 'Timestamp',
      dataIndex: 'timestamp',
      key: 'timestamp',
    },
    {
      title: 'User',
      dataIndex: 'user',
      key: 'user',
    },
    {
      title: 'Action',
      dataIndex: 'action',
      key: 'action',
    },
    {
      title: 'Details',
      dataIndex: 'details',
      key: 'details',
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>Audit Logs</Title>
      <Form form={filterForm} layout="vertical" onFinish={onFilter} style={{ marginBottom: 24 }}>
        <Row gutter={16}>
          <Col span={4}>
            <Form.Item name="userId" label="User ID">
              <Input placeholder="User ID" />
            </Form.Item>
          </Col>
          <Col span={4}>
            <Form.Item name="action" label="Action">
              <Input placeholder="Action" />
            </Form.Item>
          </Col>
          <Col span={4}>
            <Form.Item name="entityType" label="Entity">
              <Input placeholder="Entity Type" />
            </Form.Item>
          </Col>
          <Col span={4}>
            <Form.Item name="severity" label="Severity">
              <Select allowClear placeholder="Select severity">
                <Select.Option value="INFO">INFO</Select.Option>
                <Select.Option value="WARN">WARN</Select.Option>
                <Select.Option value="ERROR">ERROR</Select.Option>
                <Select.Option value="SECURITY">SECURITY</Select.Option>
              </Select>
            </Form.Item>
          </Col>
          <Col span={6}>
            <Form.Item name="dateRange" label="Date Range">
              <DatePicker.RangePicker showTime style={{ width: '100%' }} />
            </Form.Item>
          </Col>
          <Col span={2} style={{ display: 'flex', alignItems: 'end' }}>
            <Button type="primary" htmlType="submit" loading={filtering}>Filter</Button>
          </Col>
          <Col span={2} style={{ display: 'flex', alignItems: 'end' }}>
            <Button onClick={onReset}>Reset</Button>
          </Col>
        </Row>
      </Form>
      <Table
        columns={columns}
        dataSource={logs}
        rowKey="id"
        loading={loading}
        bordered
      />
    </div>
  );
};

export default AuditLogs; 