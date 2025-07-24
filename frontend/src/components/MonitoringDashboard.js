import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Table, Progress, Alert, Spin, Tabs } from 'antd';
import { 
    DashboardOutlined, 
    ClockCircleOutlined, 
    UserOutlined, 
    BellOutlined,
    BarChartOutlined,
    LineChartOutlined
} from '@ant-design/icons';
import { Line, Bar, Pie } from '@ant-design/charts';
import { monitoringAPI } from '../services/monitoringAPI';

const { TabPane } = Tabs;

/**
 * Monitoring Dashboard component for system performance and analytics
 */
const MonitoringDashboard = () => {
    const [loading, setLoading] = useState(true);
    const [metrics, setMetrics] = useState(null);
    const [error, setError] = useState(null);
    const [refreshInterval, setRefreshInterval] = useState(null);

    useEffect(() => {
        loadMetrics();
        
        // Set up auto-refresh every 30 seconds
        const interval = setInterval(loadMetrics, 30000);
        setRefreshInterval(interval);
        
        return () => {
            if (refreshInterval) {
                clearInterval(refreshInterval);
            }
        };
    }, []);

    const loadMetrics = async () => {
        try {
            setLoading(true);
            const data = await monitoringAPI.getMetricsDashboard();
            setMetrics(data);
            setError(null);
        } catch (err) {
            setError('Failed to load monitoring metrics');
            console.error('Error loading metrics:', err);
        } finally {
            setLoading(false);
        }
    };

    const getHealthStatus = () => {
        if (!metrics) return { status: 'unknown', color: 'default' };
        
        const { workflow, query, notification } = metrics;
        const queryResolutionRate = query.totalQueries > 0 ? 
            (query.resolvedQueries / query.totalQueries) * 100 : 100;
        const notificationSuccessRate = notification.totalNotifications > 0 ? 
            (notification.successfulNotifications / notification.totalNotifications) * 100 : 100;
        
        if (queryResolutionRate >= 80 && notificationSuccessRate >= 90) {
            return { status: 'Healthy', color: 'success' };
        } else if (queryResolutionRate >= 60 && notificationSuccessRate >= 70) {
            return { status: 'Warning', color: 'warning' };
        } else {
            return { status: 'Critical', color: 'error' };
        }
    };

    const renderOverviewCards = () => {
        if (!metrics) return null;
        
        const { workflow, query, notification, userActivity } = metrics;
        const healthStatus = getHealthStatus();
        
        return (
            <Row gutter={[16, 16]}>
                <Col xs={24} sm={12} md={6}>
                    <Card>
                        <Statistic
                            title="System Health"
                            value={healthStatus.status}
                            prefix={<DashboardOutlined />}
                            valueStyle={{ color: healthStatus.color === 'success' ? '#3f8600' : 
                                                healthStatus.color === 'warning' ? '#cf1322' : '#cf1322' }}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} md={6}>
                    <Card>
                        <Statistic
                            title="Active Workflows"
                            value={workflow.activeWorkflows}
                            prefix={<ClockCircleOutlined />}
                            suffix={`/ ${workflow.totalWorkflows} total`}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} md={6}>
                    <Card>
                        <Statistic
                            title="Pending Queries"
                            value={query.pendingQueries}
                            prefix={<BellOutlined />}
                            suffix={`/ ${query.totalQueries} total`}
                        />
                    </Card>
                </Col>
                <Col xs={24} sm={12} md={6}>
                    <Card>
                        <Statistic
                            title="Active Users"
                            value={userActivity.uniqueUsers}
                            prefix={<UserOutlined />}
                            suffix="users"
                        />
                    </Card>
                </Col>
            </Row>
        );
    };

    const renderPerformanceMetrics = () => {
        if (!metrics) return null;
        
        const { workflow, query, notification } = metrics;
        
        const performanceData = [
            {
                metric: 'Workflow Processing',
                averageTime: workflow.averageProcessingTime,
                unit: 'ms',
                threshold: 2000,
                status: workflow.averageProcessingTime < 2000 ? 'good' : 'warning'
            },
            {
                metric: 'Query Resolution',
                averageTime: query.averageResolutionTime,
                unit: 'ms',
                threshold: 1500,
                status: query.averageResolutionTime < 1500 ? 'good' : 'warning'
            },
            {
                metric: 'Notification Processing',
                averageTime: notification.averageProcessingTime,
                unit: 'ms',
                threshold: 5000,
                status: notification.averageProcessingTime < 5000 ? 'good' : 'warning'
            }
        ];

        const columns = [
            {
                title: 'Metric',
                dataIndex: 'metric',
                key: 'metric',
            },
            {
                title: 'Average Time',
                dataIndex: 'averageTime',
                key: 'averageTime',
                render: (time, record) => `${Math.round(time)} ${record.unit}`,
            },
            {
                title: 'Performance',
                dataIndex: 'status',
                key: 'status',
                render: (status, record) => (
                    <Progress
                        percent={Math.min(100, (record.threshold / record.averageTime) * 100)}
                        status={status === 'good' ? 'success' : 'exception'}
                        size="small"
                    />
                ),
            },
        ];

        return (
            <Card title="Performance Metrics" extra={<BarChartOutlined />}>
                <Table
                    dataSource={performanceData}
                    columns={columns}
                    pagination={false}
                    size="small"
                />
            </Card>
        );
    };

    const renderWorkflowAnalytics = () => {
        if (!metrics) return null;
        
        const { workflow } = metrics;
        
        // Mock data for workflow state distribution
        const stateData = [
            { state: 'JVC_PENDING', count: Math.floor(workflow.activeWorkflows * 0.3) },
            { state: 'PLANT_PENDING', count: Math.floor(workflow.activeWorkflows * 0.4) },
            { state: 'CQS_PENDING', count: Math.floor(workflow.activeWorkflows * 0.2) },
            { state: 'TECH_PENDING', count: Math.floor(workflow.activeWorkflows * 0.1) },
        ];

        const config = {
            data: stateData,
            xField: 'count',
            yField: 'state',
            seriesField: 'state',
            color: ['#1890ff', '#52c41a', '#faad14', '#f5222d'],
        };

        return (
            <Card title="Workflow State Distribution" extra={<LineChartOutlined />}>
                <Bar {...config} />
            </Card>
        );
    };

    const renderUserActivityAnalytics = () => {
        if (!metrics) return null;
        
        const { userActivity } = metrics;
        
        // Mock hourly activity data
        const hourlyData = Array.from({ length: 24 }, (_, hour) => ({
            hour: `${hour}:00`,
            activities: Math.floor(Math.random() * 50) + 10,
        }));

        const config = {
            data: hourlyData,
            xField: 'hour',
            yField: 'activities',
            point: {
                size: 3,
                shape: 'circle',
            },
            smooth: true,
        };

        return (
            <Card title="User Activity Patterns" extra={<UserOutlined />}>
                <div style={{ marginBottom: 16 }}>
                    <Row gutter={16}>
                        <Col span={8}>
                            <Statistic
                                title="Total Activities"
                                value={userActivity.totalActivities}
                                precision={0}
                            />
                        </Col>
                        <Col span={8}>
                            <Statistic
                                title="Most Active User"
                                value={userActivity.mostActiveUser}
                            />
                        </Col>
                        <Col span={8}>
                            <Statistic
                                title="Most Used Feature"
                                value={userActivity.mostUsedFeature}
                            />
                        </Col>
                    </Row>
                </div>
                <Line {...config} />
            </Card>
        );
    };

    const renderNotificationMetrics = () => {
        if (!metrics) return null;
        
        const { notification } = metrics;
        
        const successRate = notification.totalNotifications > 0 ? 
            (notification.successfulNotifications / notification.totalNotifications) * 100 : 100;

        const notificationData = [
            { type: 'Successful', count: notification.successfulNotifications },
            { type: 'Failed', count: notification.failedNotifications },
        ];

        const config = {
            data: notificationData,
            angleField: 'count',
            colorField: 'type',
            radius: 0.8,
            color: ['#52c41a', '#f5222d'],
            label: {
                type: 'outer',
                content: '{name} {percentage}',
            },
        };

        return (
            <Card title="Notification System Status" extra={<BellOutlined />}>
                <Row gutter={16}>
                    <Col span={12}>
                        <Statistic
                            title="Success Rate"
                            value={successRate}
                            precision={1}
                            suffix="%"
                            valueStyle={{ color: successRate >= 90 ? '#3f8600' : '#cf1322' }}
                        />
                    </Col>
                    <Col span={12}>
                        <Statistic
                            title="Avg Processing Time"
                            value={notification.averageProcessingTime}
                            precision={0}
                            suffix="ms"
                        />
                    </Col>
                </Row>
                <div style={{ marginTop: 16 }}>
                    <Pie {...config} />
                </div>
            </Card>
        );
    };

    if (loading && !metrics) {
        return (
            <div style={{ textAlign: 'center', padding: '50px' }}>
                <Spin size="large" />
                <p>Loading monitoring dashboard...</p>
            </div>
        );
    }

    if (error) {
        return (
            <Alert
                message="Error Loading Monitoring Data"
                description={error}
                type="error"
                showIcon
                style={{ margin: '20px' }}
            />
        );
    }

    return (
        <div style={{ padding: '20px' }}>
            <h2>System Monitoring Dashboard</h2>
            
            {renderOverviewCards()}
            
            <div style={{ marginTop: '20px' }}>
                <Tabs defaultActiveKey="performance">
                    <TabPane tab="Performance" key="performance">
                        <Row gutter={[16, 16]}>
                            <Col xs={24} lg={12}>
                                {renderPerformanceMetrics()}
                            </Col>
                            <Col xs={24} lg={12}>
                                {renderWorkflowAnalytics()}
                            </Col>
                        </Row>
                    </TabPane>
                    
                    <TabPane tab="User Activity" key="activity">
                        {renderUserActivityAnalytics()}
                    </TabPane>
                    
                    <TabPane tab="Notifications" key="notifications">
                        {renderNotificationMetrics()}
                    </TabPane>
                </Tabs>
            </div>
        </div>
    );
};

export default MonitoringDashboard;