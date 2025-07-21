import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import WorkflowDashboard from './WorkflowDashboard';
import QueryWidget from './QueryWidget';
import AuditTimeline from './AuditTimeline';

// Mock the API services
jest.mock('../../services/workflowAPI', () => ({
  workflowAPI: {
    getDashboardSummary: jest.fn(() => Promise.resolve({
      totalWorkflows: 10,
      activeWorkflows: 5,
      overdueWorkflows: 2,
      completedWorkflows: 3,
      openQueries: 4,
      avgResolutionTimeHours: 24,
      recentWorkflows: 2
    })),
    getOverdueWorkflows: jest.fn(() => Promise.resolve([])),
    getRecentActivity: jest.fn(() => Promise.resolve([])),
    getWorkflowCountsByState: jest.fn(() => Promise.resolve({}))
  }
}));

jest.mock('../../services/queryAPI', () => ({
  queryAPI: {
    getQueriesByWorkflow: jest.fn(() => Promise.resolve([]))
  }
}));

jest.mock('../../services/auditAPI', () => ({
  auditAPI: {
    getWorkflowAuditHistory: jest.fn(() => Promise.resolve([]))
  }
}));

describe('Workflow Components', () => {
  describe('WorkflowDashboard', () => {
    test('renders dashboard with statistics', async () => {
      render(<WorkflowDashboard />);
      
      await waitFor(() => {
        expect(screen.getByText('Total Workflows')).toBeInTheDocument();
        expect(screen.getByText('Active Workflows')).toBeInTheDocument();
        expect(screen.getByText('Overdue')).toBeInTheDocument();
        expect(screen.getByText('Completed')).toBeInTheDocument();
      });
    });

    test('handles workflow selection', async () => {
      const mockOnWorkflowSelect = jest.fn();
      render(<WorkflowDashboard onWorkflowSelect={mockOnWorkflowSelect} />);
      
      await waitFor(() => {
        expect(screen.getByText('Total Workflows')).toBeInTheDocument();
      });
    });
  });

  describe('QueryWidget', () => {
    test('renders query tabs', async () => {
      render(<QueryWidget workflowId="123" userRole="PLANT" />);
      
      await waitFor(() => {
        expect(screen.getByText('All Queries')).toBeInTheDocument();
        expect(screen.getByText('Open')).toBeInTheDocument();
        expect(screen.getByText('Resolved')).toBeInTheDocument();
        expect(screen.getByText('My Queries')).toBeInTheDocument();
      });
    });

    test('shows raise query button for plant users', async () => {
      render(<QueryWidget workflowId="123" userRole="PLANT" />);
      
      await waitFor(() => {
        expect(screen.getByText('Raise Query')).toBeInTheDocument();
      });
    });

    test('does not show raise query button for CQS users', async () => {
      render(<QueryWidget workflowId="123" userRole="CQS" />);
      
      await waitFor(() => {
        expect(screen.queryByText('Raise Query')).not.toBeInTheDocument();
      });
    });
  });

  describe('AuditTimeline', () => {
    test('renders audit timeline', async () => {
      render(<AuditTimeline workflowId="123" />);
      
      await waitFor(() => {
        expect(screen.getByText('Audit Timeline')).toBeInTheDocument();
        expect(screen.getByText('Refresh')).toBeInTheDocument();
      });
    });

    test('shows no audit history message when empty', async () => {
      render(<AuditTimeline workflowId="123" />);
      
      await waitFor(() => {
        expect(screen.getByText('No audit history available')).toBeInTheDocument();
      });
    });
  });
});

describe('Responsive Design', () => {
  beforeEach(() => {
    // Mock window.innerWidth for responsive tests
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 768,
    });
  });

  test('components adapt to mobile screen size', async () => {
    // Set mobile screen size
    window.innerWidth = 400;
    window.dispatchEvent(new Event('resize'));

    render(<WorkflowDashboard />);
    
    await waitFor(() => {
      expect(screen.getByText('Total Workflows')).toBeInTheDocument();
    });
  });

  test('components adapt to tablet screen size', async () => {
    // Set tablet screen size
    window.innerWidth = 900;
    window.dispatchEvent(new Event('resize'));

    render(<QueryWidget workflowId="123" userRole="PLANT" />);
    
    await waitFor(() => {
      expect(screen.getByText('All Queries')).toBeInTheDocument();
    });
  });
});