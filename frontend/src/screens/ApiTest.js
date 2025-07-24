import React, { useState } from 'react';
import { Button, Card, Typography, Space, Alert } from 'antd';
import { apiGet } from '../api/api';

const { Title } = Typography;

const ApiTest = () => {
  const [results, setResults] = useState({});
  const [loading, setLoading] = useState({});

  const testEndpoint = async (name, endpoint) => {
    setLoading(prev => ({ ...prev, [name]: true }));
    try {
      const data = await apiGet(endpoint);
      setResults(prev => ({ 
        ...prev, 
        [name]: { success: true, data: JSON.stringify(data, null, 2) }
      }));
    } catch (error) {
      setResults(prev => ({ 
        ...prev, 
        [name]: { success: false, error: error.message }
      }));
    } finally {
      setLoading(prev => ({ ...prev, [name]: false }));
    }
  };

  const endpoints = [
    { name: 'Health Check', endpoint: '/health' },
    { name: 'Admin Users', endpoint: '/admin/users' },
    { name: 'Admin Roles', endpoint: '/admin/roles' },
    { name: 'Projects', endpoint: '/projects' },
    { name: 'Workflows', endpoint: '/workflows' }
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={2}>API Connection Test</Title>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        {endpoints.map(({ name, endpoint }) => (
          <Card key={name} title={name}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Button 
                type="primary" 
                loading={loading[name]}
                onClick={() => testEndpoint(name, endpoint)}
              >
                Test {endpoint}
              </Button>
              {results[name] && (
                <Alert
                  type={results[name].success ? 'success' : 'error'}
                  message={results[name].success ? 'Success' : 'Error'}
                  description={
                    <pre style={{ 
                      maxHeight: 200, 
                      overflow: 'auto', 
                      fontSize: 12,
                      whiteSpace: 'pre-wrap'
                    }}>
                      {results[name].success ? results[name].data : results[name].error}
                    </pre>
                  }
                />
              )}
            </Space>
          </Card>
        ))}
      </Space>
    </div>
  );
};

export default ApiTest;