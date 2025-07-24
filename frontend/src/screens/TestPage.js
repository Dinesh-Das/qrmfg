import React, { useState, useEffect } from 'react';
import { Card, Button, Alert, Spin } from 'antd';
import { workflowAPI } from '../services/workflowAPI';
import { queryAPI } from '../services/queryAPI';
import { auditAPI } from '../services/auditAPI';
import { getCurrentUser, getUserRole } from '../services/auth';

const TestPage = () => {
  const [loading, setLoading] = useState(false);
  const [results, setResults] = useState({});
  const [error, setError] = useState(null);

  const currentUser = getCurrentUser();
  const userRole = getUserRole();

  const testAPI = async (apiName, apiCall) => {
    try {
      setLoading(true);
      setError(null);
      const result = await apiCall();
      setResults(prev => ({ ...prev, [apiName]: result }));
      console.log(`${apiName} success:`, result);
    } catch (err) {
      console.error(`${apiName} error:`, err);
      setError(`${apiName}: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const runTests = async () => {
    await testAPI('Dashboard Summary', workflowAPI.getDashboardSummary);
    await testAPI('Overdue Workflows', () => workflowAPI.getOverdueWorkflows(3));
    await testAPI('Recent Activity', () => workflowAPI.getRecentActivity(7));
    await testAPI('Queries by Workflow', () => queryAPI.getQueriesByWorkflow(1));
    await testAPI('Audit History', () => auditAPI.getWorkflowAuditHistory(1));
  };

  return (
    <div style={{ padding: '20px' }}>
      <Card title="API Test Page">
        <div style={{ marginBottom: 16 }}>
          <p><strong>Current User:</strong> {currentUser || 'Not available'}</p>
          <p><strong>User Role:</strong> {userRole || 'Not available'}</p>
          <p><strong>Mock Data:</strong> {process.env.REACT_APP_USE_MOCK_DATA === 'true' ? 'Enabled' : 'Disabled'}</p>
        </div>

        <Button 
          type="primary" 
          onClick={runTests} 
          loading={loading}
          style={{ marginBottom: 16 }}
        >
          Test All APIs
        </Button>

        {error && (
          <Alert 
            message="Error" 
            description={error} 
            type="error" 
            style={{ marginBottom: 16 }}
          />
        )}

        {loading && <Spin size="large" />}

        {Object.keys(results).length > 0 && (
          <Card title="Results" size="small">
            {Object.entries(results).map(([key, value]) => (
              <div key={key} style={{ marginBottom: 8 }}>
                <strong>{key}:</strong>
                <pre style={{ 
                  background: '#f5f5f5', 
                  padding: '8px', 
                  borderRadius: '4px',
                  fontSize: '12px',
                  overflow: 'auto',
                  maxHeight: '200px'
                }}>
                  {JSON.stringify(value, null, 2)}
                </pre>
              </div>
            ))}
          </Card>
        )}
      </Card>
    </div>
  );
};

export default TestPage;