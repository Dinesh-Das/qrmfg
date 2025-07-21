import React, { useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Radio,
  Button,
  Space,
  Alert,
  Divider,
  Tag,
  message
} from 'antd';
import {
  QuestionCircleOutlined,
  InfoCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons';
import { queryAPI } from '../../services/queryAPI';

const { TextArea } = Input;
const { Option } = Select;

const QueryRaisingModal = ({ 
  visible, 
  onCancel, 
  onSubmit, 
  workflowId, 
  fieldContext 
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (values) => {
    try {
      setLoading(true);
      
      const queryData = {
        workflowId,
        question: values.question,
        assignedTeam: values.assignedTeam,
        priority: values.priority || 'MEDIUM',
        fieldName: fieldContext?.name,
        fieldContext: values.fieldContext || fieldContext?.label,
        stepNumber: fieldContext?.stepNumber,
        stepTitle: fieldContext?.stepTitle,
        createdBy: getCurrentUser(),
        attachments: values.attachments || [],
        urgencyReason: values.urgencyReason || null
      };

      const createdQuery = await queryAPI.createQuery(queryData);
      
      message.success('Query raised successfully');
      form.resetFields();
      
      if (onSubmit) {
        onSubmit(createdQuery);
      }
    } catch (error) {
      console.error('Failed to create query:', error);
      message.error('Failed to raise query. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const getCurrentUser = () => {
    return localStorage.getItem('username') || 'current_user';
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  const getTeamDescription = (team) => {
    const descriptions = {
      'CQS': 'Chemical Quality & Safety team - for safety data, hazard classifications, and regulatory compliance questions',
      'TECH': 'Technical team - for technical specifications, physical properties, and process-related questions',
      'JVC': 'JVC team - for material identification, supplier information, and general material questions'
    };
    return descriptions[team] || '';
  };

  const getPriorityColor = (priority) => {
    const colors = {
      'LOW': 'green',
      'MEDIUM': 'blue',
      'HIGH': 'orange',
      'URGENT': 'red'
    };
    return colors[priority] || 'default';
  };

  return (
    <Modal
      title={
        <Space>
          <QuestionCircleOutlined />
          Raise Query
        </Space>
      }
      open={visible}
      onCancel={handleCancel}
      footer={[
        <Button key="cancel" onClick={handleCancel}>
          Cancel
        </Button>,
        <Button
          key="submit"
          type="primary"
          loading={loading}
          onClick={() => form.submit()}
        >
          Raise Query
        </Button>
      ]}
      width={600}
      destroyOnClose
    >
      {/* Field Context Information */}
      {fieldContext && (
        <Alert
          message="Field Context"
          description={
            <div>
              <p><strong>Step:</strong> {fieldContext.stepTitle}</p>
              <p><strong>Field:</strong> {fieldContext.label}</p>
              {fieldContext.placeholder && (
                <p><strong>Expected:</strong> {fieldContext.placeholder}</p>
              )}
            </div>
          }
          type="info"
          icon={<InfoCircleOutlined />}
          style={{ marginBottom: 16 }}
        />
      )}

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          priority: 'MEDIUM',
          fieldContext: fieldContext?.label
        }}
      >
        {/* Team Selection */}
        <Form.Item
          name="assignedTeam"
          label="Assign to Team"
          rules={[{ required: true, message: 'Please select a team to assign this query to' }]}
          help="Choose the most appropriate team based on your question type"
        >
          <Select
            placeholder="Select team to handle this query"
            size="large"
          >
            <Option value="CQS">
              <div>
                <div style={{ fontWeight: 'bold' }}>CQS Team</div>
                <div style={{ fontSize: '12px', color: '#666' }}>
                  Safety data, hazard classifications, regulatory compliance
                </div>
              </div>
            </Option>
            <Option value="TECH">
              <div>
                <div style={{ fontWeight: 'bold' }}>Technical Team</div>
                <div style={{ fontSize: '12px', color: '#666' }}>
                  Technical specs, physical properties, processes
                </div>
              </div>
            </Option>
            <Option value="JVC">
              <div>
                <div style={{ fontWeight: 'bold' }}>JVC Team</div>
                <div style={{ fontSize: '12px', color: '#666' }}>
                  Material identification, supplier info, general questions
                </div>
              </div>
            </Option>
          </Select>
        </Form.Item>

        {/* Field Context (editable) */}
        <Form.Item
          name="fieldContext"
          label="Field Context"
          help="Specify which field or section this query relates to"
        >
          <Input
            placeholder="e.g., Material Name, Safety Classification, Storage Conditions"
            prefix={<InfoCircleOutlined />}
          />
        </Form.Item>

        {/* Priority Selection */}
        <Form.Item
          name="priority"
          label="Priority Level"
          help="Select the urgency level for this query"
        >
          <Radio.Group>
            <Space direction="vertical">
              <Radio value="LOW">
                <Tag color={getPriorityColor('LOW')}>LOW</Tag>
                <span style={{ marginLeft: 8 }}>General information, non-blocking</span>
              </Radio>
              <Radio value="MEDIUM">
                <Tag color={getPriorityColor('MEDIUM')}>MEDIUM</Tag>
                <span style={{ marginLeft: 8 }}>Standard clarification needed</span>
              </Radio>
              <Radio value="HIGH">
                <Tag color={getPriorityColor('HIGH')}>HIGH</Tag>
                <span style={{ marginLeft: 8 }}>Important for completion</span>
              </Radio>
              <Radio value="URGENT">
                <Tag color={getPriorityColor('URGENT')}>URGENT</Tag>
                <span style={{ marginLeft: 8 }}>Blocking progress, needs immediate attention</span>
              </Radio>
            </Space>
          </Radio.Group>
        </Form.Item>

        <Divider />

        {/* Question Text */}
        <Form.Item
          name="question"
          label="Your Question"
          rules={[
            { required: true, message: 'Please enter your question' },
            { min: 10, message: 'Question must be at least 10 characters long' },
            { max: 1000, message: 'Question cannot exceed 1000 characters' }
          ]}
          help="Be specific and provide context to help the assigned team understand your question"
        >
          <TextArea
            rows={6}
            placeholder="Describe your question in detail. Include:
• What specific information you need
• Why you need this information
• Any relevant context or background
• What you've already tried or researched"
            showCount
            maxLength={1000}
          />
        </Form.Item>

        {/* Guidelines */}
        <Alert
          message="Query Guidelines"
          description={
            <ul style={{ margin: 0, paddingLeft: 20 }}>
              <li>Be specific and clear in your question</li>
              <li>Include relevant context and background information</li>
              <li>Mention any documentation or resources you've already checked</li>
              <li>For urgent queries, explain why immediate attention is needed</li>
              <li>Use appropriate technical terminology when relevant</li>
            </ul>
          }
          type="info"
          showIcon
          icon={<ExclamationCircleOutlined />}
        />
      </Form>
    </Modal>
  );
};

export default QueryRaisingModal;