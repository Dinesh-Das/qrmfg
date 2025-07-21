import React, { useState, useEffect } from 'react';
import {
  Card,
  Descriptions,
  Tag,
  Space,
  Button,
  Collapse,
  Alert,
  Divider,
  Typography,
  Tooltip,
  Badge,
  Spin
} from 'antd';
import {
  InfoCircleOutlined,
  FileTextOutlined,
  TeamOutlined,
  CalendarOutlined,
  DownloadOutlined,
  EyeOutlined,
  WarningOutlined
} from '@ant-design/icons';
import { workflowAPI } from '../../services/workflowAPI';

const { Panel } = Collapse;
const { Text, Title } = Typography;

const MaterialContextPanel = ({ workflowData }) => {
  const [loading, setLoading] = useState(false);
  const [jvcDocuments, setJvcDocuments] = useState([]);
  const [expanded, setExpanded] = useState(['basic', 'workflow']);

  useEffect(() => {
    if (workflowData?.id) {
      loadJvcDocuments();
    }
  }, [workflowData]);

  const loadJvcDocuments = async () => {
    try {
      setLoading(true);
      const documents = await workflowAPI.getWorkflowDocuments(workflowData.id);
      setJvcDocuments(documents || []);
    } catch (error) {
      console.error('Failed to load JVC documents:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStateColor = (state) => {
    const colors = {
      'JVC_PENDING': 'blue',
      'PLANT_PENDING': 'orange',
      'CQS_PENDING': 'purple',
      'TECH_PENDING': 'cyan',
      'COMPLETED': 'green'
    };
    return colors[state] || 'default';
  };

  const getStateName = (state) => {
    const names = {
      'JVC_PENDING': 'JVC Pending',
      'PLANT_PENDING': 'Plant Pending',
      'CQS_PENDING': 'CQS Pending',
      'TECH_PENDING': 'Tech Pending',
      'COMPLETED': 'Completed'
    };
    return names[state] || state;
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const calculateDaysInState = (lastModified) => {
    if (!lastModified) return 0;
    const now = new Date();
    const modified = new Date(lastModified);
    const diffTime = Math.abs(now - modified);
    return Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  };

  const handleDocumentDownload = async (documentId, filename) => {
    try {
      const blob = await workflowAPI.downloadDocument(documentId);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Failed to download document:', error);
    }
  };

  const handleDocumentPreview = (documentId) => {
    // Open document in new tab for preview
    const previewUrl = `/api/workflows/documents/${documentId}/preview`;
    window.open(previewUrl, '_blank');
  };

  if (!workflowData) {
    return (
      <Card title="Material Context" style={{ height: 'fit-content' }}>
        <Alert
          message="No Material Data"
          description="Material context will appear here once a workflow is selected."
          type="info"
          showIcon
        />
      </Card>
    );
  }

  const daysInCurrentState = calculateDaysInState(workflowData.lastModified);
  const isOverdue = daysInCurrentState > 3;

  return (
    <Card
      title={
        <Space>
          <InfoCircleOutlined />
          Material Context
          {isOverdue && (
            <Badge status="error" text={`${daysInCurrentState} days`} />
          )}
        </Space>
      }
      style={{ height: 'fit-content', position: 'sticky', top: 24 }}
      size="small"
    >
      <Collapse
        activeKey={expanded}
        onChange={setExpanded}
        ghost
        size="small"
      >
        {/* Basic Material Information */}
        <Panel
          header={
            <Space>
              <FileTextOutlined />
              <strong>Basic Information</strong>
            </Space>
          }
          key="basic"
        >
          <Descriptions column={1} size="small">
            <Descriptions.Item label="Material ID">
              <Text strong>{workflowData.materialId}</Text>
            </Descriptions.Item>
            <Descriptions.Item label="Material Name">
              {workflowData.materialName || 'Not specified'}
            </Descriptions.Item>
            <Descriptions.Item label="Assigned Plant">
              <Tag color="blue">{workflowData.assignedPlant}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Initiated By">
              <Space>
                <TeamOutlined />
                {workflowData.initiatedBy}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="Created">
              <Space>
                <CalendarOutlined />
                {formatDate(workflowData.createdAt)}
              </Space>
            </Descriptions.Item>
          </Descriptions>
        </Panel>

        {/* Workflow Status */}
        <Panel
          header={
            <Space>
              <TeamOutlined />
              <strong>Workflow Status</strong>
              {isOverdue && <WarningOutlined style={{ color: '#ff4d4f' }} />}
            </Space>
          }
          key="workflow"
        >
          <Space direction="vertical" style={{ width: '100%' }}>
            <div>
              <Text strong>Current State:</Text>
              <div style={{ marginTop: 4 }}>
                <Tag color={getStateColor(workflowData.state)} size="default">
                  {getStateName(workflowData.state)}
                </Tag>
              </div>
            </div>
            
            <div>
              <Text strong>Time in Current State:</Text>
              <div style={{ marginTop: 4 }}>
                <Text style={{ color: isOverdue ? '#ff4d4f' : 'inherit' }}>
                  {daysInCurrentState} day{daysInCurrentState !== 1 ? 's' : ''}
                  {isOverdue && ' (Overdue)'}
                </Text>
              </div>
            </div>

            <div>
              <Text strong>Last Modified:</Text>
              <div style={{ marginTop: 4 }}>
                <Text type="secondary">
                  {formatDate(workflowData.lastModified)}
                </Text>
              </div>
            </div>

            {workflowData.totalQueries > 0 && (
              <div>
                <Text strong>Queries:</Text>
                <div style={{ marginTop: 4 }}>
                  <Space>
                    <Tag color="red">
                      {workflowData.openQueries || 0} Open
                    </Tag>
                    <Tag color="green">
                      {(workflowData.totalQueries || 0) - (workflowData.openQueries || 0)} Resolved
                    </Tag>
                  </Space>
                </div>
              </div>
            )}
          </Space>
        </Panel>

        {/* JVC Provided Documents */}
        <Panel
          header={
            <Space>
              <FileTextOutlined />
              <strong>JVC Documents</strong>
              <Badge count={jvcDocuments.length} size="small" />
            </Space>
          }
          key="documents"
        >
          {loading ? (
            <div style={{ textAlign: 'center', padding: '20px' }}>
              <Spin size="small" />
            </div>
          ) : jvcDocuments.length > 0 ? (
            <Space direction="vertical" style={{ width: '100%' }}>
              {jvcDocuments.map((doc, index) => (
                <Card
                  key={doc.id || index}
                  size="small"
                  style={{ marginBottom: 8 }}
                  bodyStyle={{ padding: '8px 12px' }}
                >
                  <div style={{ marginBottom: 4 }}>
                    <Text strong style={{ fontSize: '12px' }}>
                      {doc.filename || `Document ${index + 1}`}
                    </Text>
                  </div>
                  <div style={{ marginBottom: 8 }}>
                    <Text type="secondary" style={{ fontSize: '11px' }}>
                      {doc.description || 'No description available'}
                    </Text>
                  </div>
                  <Space size="small">
                    <Tooltip title="Preview document">
                      <Button
                        type="text"
                        size="small"
                        icon={<EyeOutlined />}
                        onClick={() => handleDocumentPreview(doc.id)}
                      />
                    </Tooltip>
                    <Tooltip title="Download document">
                      <Button
                        type="text"
                        size="small"
                        icon={<DownloadOutlined />}
                        onClick={() => handleDocumentDownload(doc.id, doc.filename)}
                      />
                    </Tooltip>
                  </Space>
                </Card>
              ))}
            </Space>
          ) : (
            <Alert
              message="No Documents"
              description="No documents have been provided by the JVC team for this material."
              type="info"
              showIcon
              size="small"
            />
          )}
        </Panel>

        {/* JVC Material Data */}
        <Panel
          header={
            <Space>
              <InfoCircleOutlined />
              <strong>JVC Material Data</strong>
            </Space>
          }
          key="jvc-data"
        >
          <Descriptions column={1} size="small">
            <Descriptions.Item label="Material Category">
              {workflowData.materialCategory || 'Not specified'}
            </Descriptions.Item>
            <Descriptions.Item label="Supplier Code">
              {workflowData.supplierCode || 'Not specified'}
            </Descriptions.Item>
            <Descriptions.Item label="Purchase Order">
              {workflowData.purchaseOrder || 'Not specified'}
            </Descriptions.Item>
            <Descriptions.Item label="Expected Usage">
              {workflowData.expectedUsage || 'Not specified'}
            </Descriptions.Item>
            <Descriptions.Item label="Regulatory Requirements">
              {workflowData.regulatoryRequirements || 'Standard compliance required'}
            </Descriptions.Item>
            <Descriptions.Item label="Special Instructions">
              {workflowData.specialInstructions || 'None'}
            </Descriptions.Item>
          </Descriptions>
          
          {workflowData.jvcNotes && (
            <div style={{ marginTop: 12 }}>
              <Text strong style={{ fontSize: '12px' }}>JVC Notes:</Text>
              <div style={{ 
                marginTop: 4, 
                padding: '8px', 
                backgroundColor: '#f6ffed', 
                border: '1px solid #b7eb8f',
                borderRadius: '4px',
                fontSize: '12px'
              }}>
                {workflowData.jvcNotes}
              </div>
            </div>
          )}
        </Panel>

        {/* Material Specifications */}
        {workflowData.specifications && (
          <Panel
            header={
              <Space>
                <InfoCircleOutlined />
                <strong>Technical Specifications</strong>
              </Space>
            }
            key="specifications"
          >
            <Descriptions column={1} size="small">
              {Object.entries(workflowData.specifications).map(([key, value]) => (
                <Descriptions.Item key={key} label={key.replace(/([A-Z])/g, ' $1').replace(/^./, str => str.toUpperCase())}>
                  {typeof value === 'object' ? JSON.stringify(value) : String(value)}
                </Descriptions.Item>
              ))}
            </Descriptions>
          </Panel>
        )}

        {/* Safety Notes */}
        {workflowData.safetyNotes && (
          <Panel
            header={
              <Space>
                <WarningOutlined />
                <strong>Safety Notes</strong>
              </Space>
            }
            key="safety"
          >
            <Alert
              message="Important Safety Information"
              description={workflowData.safetyNotes}
              type="warning"
              showIcon
              size="small"
            />
          </Panel>
        )}
      </Collapse>

      <Divider style={{ margin: '12px 0' }} />

      {/* Quick Actions */}
      <Space direction="vertical" style={{ width: '100%' }}>
        <Text strong style={{ fontSize: '12px' }}>Quick Actions:</Text>
        <Space wrap>
          <Button size="small" type="link" style={{ padding: '0 4px', height: 'auto' }}>
            View Full History
          </Button>
          <Button size="small" type="link" style={{ padding: '0 4px', height: 'auto' }}>
            Contact JVC
          </Button>
          <Button size="small" type="link" style={{ padding: '0 4px', height: 'auto' }}>
            Export Data
          </Button>
        </Space>
      </Space>
    </Card>
  );
};

export default MaterialContextPanel;