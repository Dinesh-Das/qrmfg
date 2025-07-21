import React, { useState, useEffect } from 'react';
import { 
  Row, 
  Col, 
  Card, 
  Statistic, 
  Table, 
  DatePicker, 
  Button, 
  Spin, 
  Tabs, 
  Select, 
  Typography, 
  message,
  Divider,
  Progress
} from 'antd';
import { 
  BarChartOutlined, 
  LineChartOutlined, 
  PieChartOutlined, 
  DownloadOutlined,
  ClockCircleOutlined,
  CheckCircleOutlined,
  ExclamationCircleOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons';
import axios from 'axios';
import moment from 'moment';
import { Bar, Line, Pie } from 'react-chartjs-2';
import { Chart, registerables } from 'chart.js';

// Register Chart.js components
Chart.register(...registerables);

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { TabPane } = Tabs;
const { Option } = Select;

const WorkflowMonitoring = () => {
  const [loading, setLoading] = useState(true);
  const [dashboardData, setDashboardData] = useState(null);
  const [dateRange, setDateRange] = useState([null, null]);
  const [slaReport, setSlaReport] = useState(null);
  const [slaLoading, setSlaLoading] = useState(false);
  const [exportLoading, setExportLoading] = useState(false);
  const [bottlenecks, setBottlenecks] = useState(null);
  const [bottlenecksLoading, setBottlenecksLoading] = useState(false);
  const [performanceMetrics, setPerformanceMetrics] = useState(null);
  const [performanceLoading, setPerformanceLoading] = useState(false);

  useEffect(() => {
    fetchDashboardData();
    fetchBottlenecks();
    fetchPerformanceMetrics();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const response = await axios.get('/qrmfg/api/v1/admin/monitoring/dashboard');
      setDashboardData(response.data);
    } catch (error) {
      message.error('Failed to load dashboard data');
      console.error('Error fetching dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchSlaReport = async () => {
    setSlaLoading(true);
    try {
      let url = '/qrmfg/api/v1/admin/monitoring/query-sla';
      if (dateRange[0] && dateRange[1]) {
        url += `?startDate=${dateRange[0].toISOString()}&endDate=${dateRange[1].toISOString()}`;
      }
      const response = await axios.get(url);
      setSlaReport(response.data);
    } catch (error) {
      message.error('Failed to load SLA report');
      console.error('Error fetching SLA report:', error);
    } finally {
      setSlaLoading(false);
    }
  };

  const fetchBottlenecks = async () => {
    setBottlenecksLoading(true);
    try {
      const response = await axios.get('/qrmfg/api/v1/admin/monitoring/bottlenecks');
      setBottlenecks(response.data);
    } catch (error) {
      message.error('Failed to load bottlenecks analysis');
      console.error('Error fetching bottlenecks:', error);
    } finally {
      setBottlenecksLoading(false);
    }
  };

  const fetchPerformanceMetrics = async () => {
    setPerformanceLoading(true);
    try {
      let url = '/qrmfg/api/v1/admin/monitoring/performance';
      if (dateRange[0] && dateRange[1]) {
        url += `?startDate=${dateRange[0].toISOString()}&endDate=${dateRange[1].toISOString()}`;
      }
      const response = await axios.get(url);
      setPerformanceMetrics(response.data);
    } catch (error) {
      message.error('Failed to load performance metrics');
      console.error('Error fetching performance metrics:', error);
    } finally {
      setPerformanceLoading(false);
    }
  };

  const handleDateRangeChange = (dates) => {
    setDateRange(dates);
  };

  const handleApplyDateFilter = () => {
    fetchSlaReport();
    fetchPerformanceMetrics();
  };

  const handleExportAuditLogs = async () => {
    setExportLoading(true);
    try {
      let url = '/qrmfg/api/v1/admin/monitoring/audit-logs/export';
      if (dateRange[0] && dateRange[1]) {
        url += `?startDate=${dateRange[0].toISOString()}&endDate=${dateRange[1].toISOString()}`;
      }
      
      const response = await axios.get(url, { responseType: 'blob' });
      
      // Create a download link and trigger download
      const downloadUrl = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', `audit-logs-${moment().format('YYYY-MM-DD')}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      
      message.success('Audit logs exported successfully');
    } catch (error) {
      message.error('Failed to export audit logs');
      console.error('Error exporting audit logs:', error);
    } finally {
      setExportLoading(false);
    }
  };

  const handleExportWorkflowReport = async (state = null) => {
    setExportLoading(true);
    try {
      let url = '/qrmfg/api/v1/admin/monitoring/workflows/export';
      const params = [];
      
      if (dateRange[0] && dateRange[1]) {
        params.push(`startDate=${dateRange[0].toISOString()}`);
        params.push(`endDate=${dateRange[1].toISOString()}`);
      }
      
      if (state) {
        params.push(`state=${state}`);
      }
      
      if (params.length > 0) {
        url += '?' + params.join('&');
      }
      
      const response = await axios.get(url, { responseType: 'blob' });
      
      // Create a download link and trigger download
      const downloadUrl = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', `workflow-report-${moment().format('YYYY-MM-DD')}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      
      message.success('Workflow report exported successfully');
    } catch (error) {
      message.error('Failed to export workflow report');
      console.error('Error exporting workflow report:', error);
    } finally {
      setExportLoading(false);
    }
  };

  // Prepare chart data for workflow status distribution
  const getWorkflowStatusChartData = () => {
    if (!dashboardData || !dashboardData.workflowsByState) return null;
    
    const labels = Object.keys(dashboardData.workflowsByState);
    const data = Object.values(dashboardData.workflowsByState);
    
    return {
      labels,
      datasets: [
        {
          label: 'Workflows by State',
          data,
          backgroundColor: [
            'rgba(54, 162, 235, 0.6)',
            'rgba(255, 206, 86, 0.6)',
            'rgba(75, 192, 192, 0.6)',
            'rgba(153, 102, 255, 0.6)',
            'rgba(255, 159, 64, 0.6)'
          ],
          borderColor: [
            'rgba(54, 162, 235, 1)',
            'rgba(255, 206, 86, 1)',
            'rgba(75, 192, 192, 1)',
            'rgba(153, 102, 255, 1)',
            'rgba(255, 159, 64, 1)'
          ],
          borderWidth: 1
        }
      ]
    };
  };

  // Prepare chart data for SLA compliance
  const getSlaComplianceChartData = () => {
    if (!slaReport || !slaReport.slaComplianceByTeam) return null;
    
    const labels = Object.keys(slaReport.slaComplianceByTeam);
    const data = Object.values(slaReport.slaComplianceByTeam);
    
    return {
      labels,
      datasets: [
        {
          label: 'SLA Compliance (%)',
          data,
          backgroundColor: 'rgba(75, 192, 192, 0.6)',
          borderColor: 'rgba(75, 192, 192, 1)',
          borderWidth: 1
        }
      ]
    };
  };

  // Prepare chart data for resolution times
  const getResolutionTimesChartData = () => {
    if (!slaReport || !slaReport.averageResolutionTimesByTeam) return null;
    
    const labels = Object.keys(slaReport.averageResolutionTimesByTeam);
    const data = Object.values(slaReport.averageResolutionTimesByTeam);
    
    return {
      labels,
      datasets: [
        {
          label: 'Average Resolution Time (hours)',
          data,
          backgroundColor: 'rgba(255, 159, 64, 0.6)',
          borderColor: 'rgba(255, 159, 64, 1)',
          borderWidth: 1
        }
      ]
    };
  };

  // Prepare chart data for recent activity
  const getRecentActivityChartData = () => {
    if (!dashboardData || !dashboardData.recentActivity) return null;
    
    const sortedDates = Object.keys(dashboardData.recentActivity).sort();
    const data = sortedDates.map(date => dashboardData.recentActivity[date]);
    
    return {
      labels: sortedDates,
      datasets: [
        {
          label: 'Workflow Activity',
          data,
          fill: false,
          backgroundColor: 'rgba(54, 162, 235, 0.6)',
          borderColor: 'rgba(54, 162, 235, 1)',
          tension: 0.1
        }
      ]
    };
  };

  // Render loading state
  if (loading && !dashboardData) {
    return (
      <div style={{ textAlign: 'center', padding: '50px' }}>
        <Spin size="large" />
        <p>Loading dashboard data...</p>
      </div>
    );
  }

  return (
    <div>
      <Title level={2}>Workflow Monitoring Dashboard</Title>
      
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col span={24}>
          <Card>
            <Row gutter={16}>
              <Col span={8}>
                <RangePicker 
                  onChange={handleDateRangeChange} 
                  style={{ width: '100%' }} 
                />
              </Col>
              <Col span={4}>
                <Button 
                  type="primary" 
                  onClick={handleApplyDateFilter}
                >
                  Apply Filter
                </Button>
              </Col>
              <Col span={12} style={{ textAlign: 'right' }}>
                <Button 
                  icon={<DownloadOutlined />} 
                  onClick={handleExportAuditLogs}
                  loading={exportLoading}
                  style={{ marginRight: 8 }}
                >
                  Export Audit Logs
                </Button>
                <Button 
                  icon={<DownloadOutlined />} 
                  onClick={() => handleExportWorkflowReport()}
                  loading={exportLoading}
                >
                  Export Workflow Report
                </Button>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>
      
      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card>
            <Statistic 
              title="Total Workflows" 
              value={dashboardData?.totalWorkflows || 0} 
              prefix={<BarChartOutlined />} 
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="Active Workflows" 
              value={dashboardData?.activeWorkflows || 0} 
              prefix={<ClockCircleOutlined />} 
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="Completed Workflows" 
              value={dashboardData?.completedWorkflows || 0} 
              prefix={<CheckCircleOutlined />} 
              valueStyle={{ color: '#3f8600' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="Overdue Workflows" 
              value={dashboardData?.overdueWorkflows || 0} 
              prefix={<ExclamationCircleOutlined />} 
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
      </Row>
      
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={6}>
          <Card>
            <Statistic 
              title="Total Queries" 
              value={dashboardData?.totalQueries || 0} 
              prefix={<QuestionCircleOutlined />} 
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="Open Queries" 
              value={dashboardData?.openQueries || 0} 
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="Overdue Queries" 
              value={dashboardData?.overdueQueries || 0} 
              valueStyle={{ color: '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic 
              title="Avg. Completion Time (hrs)" 
              value={dashboardData?.averageCompletionTimeHours?.toFixed(1) || 0} 
              precision={1}
            />
          </Card>
        </Col>
      </Row>
      
      <Tabs defaultActiveKey="1" style={{ marginTop: 16 }}>
        <TabPane tab="Workflow Status" key="1">
          <Row gutter={[16, 16]}>
            <Col span={12}>
              <Card title="Workflow Status Distribution">
                {dashboardData && dashboardData.workflowsByState && (
                  <Pie 
                    data={getWorkflowStatusChartData()} 
                    options={{ responsive: true, maintainAspectRatio: false }}
                    height={300}
                  />
                )}
              </Card>
            </Col>
            <Col span={12}>
              <Card title="Recent Activity">
                {dashboardData && dashboardData.recentActivity && (
                  <Line 
                    data={getRecentActivityChartData()} 
                    options={{ responsive: true, maintainAspectRatio: false }}
                    height={300}
                  />
                )}
              </Card>
            </Col>
          </Row>
          
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col span={24}>
              <Card title="Workflows by Plant">
                <Table 
                  dataSource={
                    dashboardData && dashboardData.workflowsByPlant ? 
                    Object.entries(dashboardData.workflowsByPlant).map(([plant, count]) => ({
                      key: plant,
                      plant,
                      count
                    })) : []
                  }
                  columns={[
                    { title: 'Plant', dataIndex: 'plant', key: 'plant' },
                    { title: 'Workflow Count', dataIndex: 'count', key: 'count' }
                  ]}
                  pagination={false}
                />
              </Card>
            </Col>
          </Row>
        </TabPane>
        
        <TabPane tab="Query SLA Reports" key="2">
          <Button 
            type="primary" 
            onClick={fetchSlaReport} 
            loading={slaLoading}
            style={{ marginBottom: 16 }}
          >
            Generate SLA Report
          </Button>
          
          {slaLoading ? (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Spin />
              <p>Generating SLA report...</p>
            </div>
          ) : slaReport ? (
            <>
              <Row gutter={[16, 16]}>
                <Col span={8}>
                  <Card>
                    <Statistic 
                      title="Overall SLA Compliance" 
                      value={slaReport.overallSlaCompliance.toFixed(1)} 
                      suffix="%" 
                      precision={1}
                    />
                    <Progress 
                      percent={slaReport.overallSlaCompliance} 
                      status={slaReport.overallSlaCompliance >= 90 ? "success" : "active"} 
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic 
                      title="Average Resolution Time" 
                      value={slaReport.overallAverageResolutionTime.toFixed(1)} 
                      suffix="hours" 
                      precision={1}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic 
                      title="Resolution Rate" 
                      value={(slaReport.totalResolvedQueries / slaReport.totalQueries * 100).toFixed(1)} 
                      suffix="%" 
                      precision={1}
                    />
                  </Card>
                </Col>
              </Row>
              
              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={12}>
                  <Card title="SLA Compliance by Team">
                    <Bar 
                      data={getSlaComplianceChartData()} 
                      options={{ 
                        responsive: true, 
                        maintainAspectRatio: false,
                        scales: {
                          y: {
                            beginAtZero: true,
                            max: 100
                          }
                        }
                      }}
                      height={300}
                    />
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="Average Resolution Times by Team">
                    <Bar 
                      data={getResolutionTimesChartData()} 
                      options={{ 
                        responsive: true, 
                        maintainAspectRatio: false
                      }}
                      height={300}
                    />
                  </Card>
                </Col>
              </Row>
              
              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={24}>
                  <Card title="Query Metrics by Team">
                    <Table 
                      dataSource={
                        slaReport.totalQueriesByTeam ? 
                        Object.keys(slaReport.totalQueriesByTeam).map(team => ({
                          key: team,
                          team,
                          total: slaReport.totalQueriesByTeam[team],
                          resolved: slaReport.resolvedQueriesByTeam[team],
                          overdue: slaReport.overdueQueriesByTeam[team],
                          avgTime: slaReport.averageResolutionTimesByTeam[team]?.toFixed(1),
                          compliance: slaReport.slaComplianceByTeam[team]?.toFixed(1)
                        })) : []
                      }
                      columns={[
                        { title: 'Team', dataIndex: 'team', key: 'team' },
                        { title: 'Total Queries', dataIndex: 'total', key: 'total' },
                        { title: 'Resolved', dataIndex: 'resolved', key: 'resolved' },
                        { title: 'Overdue', dataIndex: 'overdue', key: 'overdue' },
                        { title: 'Avg. Resolution Time (hrs)', dataIndex: 'avgTime', key: 'avgTime' },
                        { 
                          title: 'SLA Compliance', 
                          dataIndex: 'compliance', 
                          key: 'compliance',
                          render: (text) => `${text}%`
                        }
                      ]}
                      pagination={false}
                    />
                  </Card>
                </Col>
              </Row>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Text type="secondary">Click "Generate SLA Report" to view query SLA metrics</Text>
            </div>
          )}
        </TabPane>
        
        <TabPane tab="Bottlenecks Analysis" key="3">
          <Button 
            type="primary" 
            onClick={fetchBottlenecks} 
            loading={bottlenecksLoading}
            style={{ marginBottom: 16 }}
          >
            Refresh Bottlenecks Analysis
          </Button>
          
          {bottlenecksLoading ? (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Spin />
              <p>Analyzing bottlenecks...</p>
            </div>
          ) : bottlenecks ? (
            <>
              <Row gutter={[16, 16]}>
                <Col span={12}>
                  <Card title="Average Time in Each State (hours)">
                    <Table 
                      dataSource={
                        bottlenecks.averageTimeInState ? 
                        Object.entries(bottlenecks.averageTimeInState).map(([state, hours]) => ({
                          key: state,
                          state,
                          hours: hours.toFixed(1)
                        })) : []
                      }
                      columns={[
                        { title: 'Workflow State', dataIndex: 'state', key: 'state' },
                        { title: 'Average Hours', dataIndex: 'hours', key: 'hours' }
                      ]}
                      pagination={false}
                    />
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="Overdue Workflows by State">
                    <Table 
                      dataSource={
                        bottlenecks.overdueByState ? 
                        Object.entries(bottlenecks.overdueByState).map(([state, count]) => ({
                          key: state,
                          state,
                          count
                        })) : []
                      }
                      columns={[
                        { title: 'Workflow State', dataIndex: 'state', key: 'state' },
                        { title: 'Overdue Count', dataIndex: 'count', key: 'count' }
                      ]}
                      pagination={false}
                    />
                  </Card>
                </Col>
              </Row>
              
              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={12}>
                  <Card title="Open Queries by Team">
                    <Table 
                      dataSource={
                        bottlenecks.openQueriesByTeam ? 
                        Object.entries(bottlenecks.openQueriesByTeam).map(([team, count]) => ({
                          key: team,
                          team,
                          count
                        })) : []
                      }
                      columns={[
                        { title: 'Team', dataIndex: 'team', key: 'team' },
                        { title: 'Open Queries', dataIndex: 'count', key: 'count' }
                      ]}
                      pagination={false}
                    />
                  </Card>
                </Col>
                <Col span={12}>
                  <Card title="Delayed Workflows by Plant">
                    <Table 
                      dataSource={
                        bottlenecks.delayedByPlant ? 
                        Object.entries(bottlenecks.delayedByPlant).map(([plant, count]) => ({
                          key: plant,
                          plant,
                          count
                        })) : []
                      }
                      columns={[
                        { title: 'Plant', dataIndex: 'plant', key: 'plant' },
                        { title: 'Delayed Count', dataIndex: 'count', key: 'count' }
                      ]}
                      pagination={false}
                    />
                  </Card>
                </Col>
              </Row>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Text type="secondary">Click "Refresh Bottlenecks Analysis" to view workflow bottlenecks</Text>
            </div>
          )}
        </TabPane>
        
        <TabPane tab="Performance Metrics" key="4">
          <Button 
            type="primary" 
            onClick={fetchPerformanceMetrics} 
            loading={performanceLoading}
            style={{ marginBottom: 16 }}
          >
            Refresh Performance Metrics
          </Button>
          
          {performanceLoading ? (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Spin />
              <p>Loading performance metrics...</p>
            </div>
          ) : performanceMetrics ? (
            <>
              <Row gutter={[16, 16]}>
                <Col span={8}>
                  <Card>
                    <Statistic 
                      title="Completion Rate" 
                      value={performanceMetrics.completionRate?.toFixed(1)} 
                      suffix="%" 
                      precision={1}
                    />
                    <Progress 
                      percent={performanceMetrics.completionRate} 
                      status={performanceMetrics.completionRate >= 80 ? "success" : "active"} 
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic 
                      title="Average Completion Time" 
                      value={performanceMetrics.averageCompletionTimeHours?.toFixed(1)} 
                      suffix="hours" 
                      precision={1}
                    />
                  </Card>
                </Col>
                <Col span={8}>
                  <Card>
                    <Statistic 
                      title="Queries Per Workflow" 
                      value={performanceMetrics.queriesPerWorkflow?.toFixed(1)} 
                      precision={1}
                    />
                  </Card>
                </Col>
              </Row>
              
              <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                <Col span={24}>
                  <Card title="Monthly Workflow Throughput">
                    {performanceMetrics.throughputByMonth && (
                      <Bar 
                        data={{
                          labels: Object.keys(performanceMetrics.throughputByMonth),
                          datasets: [
                            {
                              label: 'Completed Workflows',
                              data: Object.values(performanceMetrics.throughputByMonth),
                              backgroundColor: 'rgba(75, 192, 192, 0.6)',
                              borderColor: 'rgba(75, 192, 192, 1)',
                              borderWidth: 1
                            }
                          ]
                        }}
                        options={{ 
                          responsive: true, 
                          maintainAspectRatio: false
                        }}
                        height={300}
                      />
                    )}
                  </Card>
                </Col>
              </Row>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '50px' }}>
              <Text type="secondary">Click "Refresh Performance Metrics" to view workflow performance data</Text>
            </div>
          )}
        </TabPane>
      </Tabs>
    </div>
  );
};

export default WorkflowMonitoring;