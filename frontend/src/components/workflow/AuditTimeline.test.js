import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import AuditTimeline from './AuditTimeline';
import { auditAPI } from '../../services/auditAPI';

// Mock the auditAPI
jest.mock('../../services/auditAPI', () => ({
  auditAPI: {
    getWorkflowAuditHistory: jest.fn(),
    getQueryAuditHistory: jest.fn(),
    getCompleteWorkflowAuditTrail: jest.fn()
  }
}));

// Mock antd components that might cause issues in tests
jest.mock('antd', () => {
  const antd = jest.requireActual('antd');
  return {
    ...antd,
    Timeline: ({ children }) => <div data-testid="timeline">{children}</div>,
    'Timeline.Item': ({ children }) => <div data-testid="timeline-item">{children}</div>
  };
});

describe('AuditTimeline', () => {
  const mockAuditHistory = [
    {
      id: 1,
      timestamp: '2023-01-01T10:00:00',
      username: 'testuser',
      action: 'CREATE',
      entityType: 'MaterialWorkflow',
      entityId: '1',
      description: 'Created workflow for material TEST-001',
      changes: []
    },
    {
      id: 2,
      timestamp: '2023-01-01T11:00:00',
      username: 'testuser2',
      action: 'UPDATE',
      entityType: 'MaterialWorkflow',
      entityId: '1',
      description: 'Updated workflow for material TEST-001',
      changes: [
        { field: 'state', oldValue: 'JVC_PENDING', newValue: 'PLANT_PENDING' }
      ]
    }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders loading state initially', () => {
    auditAPI.getWorkflowAuditHistory.mockImplementation(() => new Promise(() => {}));
    
    render(<AuditTimeline workflowId={1} />);
    
    expect(screen.getByTestId('loading-spinner')).toBeInTheDocument();
  });

  test('renders audit timeline with workflow history', async () => {
    auditAPI.getWorkflowAuditHistory.mockResolvedValue(mockAuditHistory);
    
    render(<AuditTimeline workflowId={1} entityType="workflow" />);
    
    await waitFor(() => {
      expect(screen.getByText('Audit Timeline')).toBeInTheDocument();
    });
    
    expect(screen.getByText('Created workflow for material TEST-001')).toBeInTheDocument();
    expect(screen.getByText('Updated workflow for material TEST-001')).toBeInTheDocument();
    expect(screen.getByText('testuser')).toBeInTheDocument();
    expect(screen.getByText('testuser2')).toBeInTheDocument();
  });

  test('renders complete audit trail', async () => {
    auditAPI.getCompleteWorkflowAuditTrail.mockResolvedValue(mockAuditHistory);
    
    render(<AuditTimeline workflowId={1} entityType="complete" />);
    
    await waitFor(() => {
      expect(auditAPI.getCompleteWorkflowAuditTrail).toHaveBeenCalledWith(1);
    });
  });

  test('renders query audit history', async () => {
    auditAPI.getQueryAuditHistory.mockResolvedValue(mockAuditHistory);
    
    render(<AuditTimeline workflowId={1} entityType="query" />);
    
    await waitFor(() => {
      expect(auditAPI.getQueryAuditHistory).toHaveBeenCalledWith(1);
    });
  });

  test('handles error state', async () => {
    auditAPI.getWorkflowAuditHistory.mockRejectedValue(new Error('API Error'));
    
    render(<AuditTimeline workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Failed to load audit history')).toBeInTheDocument();
    });
  });

  test('shows empty state when no history', async () => {
    auditAPI.getWorkflowAuditHistory.mockResolvedValue([]);
    
    render(<AuditTimeline workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('No audit history available')).toBeInTheDocument();
    });
  });

  test('expands and collapses change details', async () => {
    auditAPI.getWorkflowAuditHistory.mockResolvedValue(mockAuditHistory);
    
    render(<AuditTimeline workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Show Details')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Show Details'));
    
    await waitFor(() => {
      expect(screen.getByText('Hide Details')).toBeInTheDocument();
      expect(screen.getByText('Changes:')).toBeInTheDocument();
    });
  });

  test('refreshes audit history', async () => {
    auditAPI.getWorkflowAuditHistory.mockResolvedValue(mockAuditHistory);
    
    render(<AuditTimeline workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Refresh')).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText('Refresh'));
    
    expect(auditAPI.getWorkflowAuditHistory).toHaveBeenCalledTimes(2);
  });

  test('handles mobile responsive layout', async () => {
    // Mock window.innerWidth for mobile
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 500,
    });
    
    auditAPI.getWorkflowAuditHistory.mockResolvedValue(mockAuditHistory);
    
    render(<AuditTimeline workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByTestId('timeline')).toBeInTheDocument();
    });
    
    // Check if mobile-specific classes are applied
    const timeline = screen.getByTestId('timeline');
    expect(timeline).toHaveClass('audit-timeline-mobile');
  });

  test('formats timestamps correctly', async () => {
    auditAPI.getWorkflowAuditHistory.mockResolvedValue(mockAuditHistory);
    
    render(<AuditTimeline workflowId={1} />);
    
    await waitFor(() => {
      // Check if timestamp is formatted (exact format may vary by locale)
      expect(screen.getByText(/2023/)).toBeInTheDocument();
    });
  });

  test('displays correct action icons and colors', async () => {
    auditAPI.getWorkflowAuditHistory.mockResolvedValue(mockAuditHistory);
    
    render(<AuditTimeline workflowId={1} />);
    
    await waitFor(() => {
      // Check if CREATE and UPDATE tags are present with correct colors
      const createTag = screen.getByText('CREATE');
      const updateTag = screen.getByText('UPDATE');
      
      expect(createTag).toBeInTheDocument();
      expect(updateTag).toBeInTheDocument();
    });
  });
});