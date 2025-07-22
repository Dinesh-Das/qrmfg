import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import QueryInbox from './QueryInbox';

// Mock the API calls
global.fetch = jest.fn();

// Mock the child components
jest.mock('./QueryResponseEditor', () => {
  return function MockQueryResponseEditor({ placeholder, onChange }) {
    return (
      <textarea
        data-testid="query-response-editor"
        placeholder={placeholder}
        onChange={(e) => onChange && onChange(e.target.value)}
      />
    );
  };
});

jest.mock('./MaterialContextDisplay', () => {
  return function MockMaterialContextDisplay({ materialCode, workflowId, compact }) {
    return (
      <div data-testid="material-context-display">
        Material: {materialCode}, Workflow: {workflowId}, Compact: {compact?.toString()}
      </div>
    );
  };
});

jest.mock('./QueryHistoryTracker', () => {
  return function MockQueryHistoryTracker({ materialCode, workflowId, compact }) {
    return (
      <div data-testid="query-history-tracker">
        Material: {materialCode}, Workflow: {workflowId}, Compact: {compact?.toString()}
      </div>
    );
  };
});

describe('QueryInbox', () => {
  const mockQueries = [
    {
      id: 1,
      materialCode: 'MAT-001',
      materialName: 'Test Material',
      projectCode: 'PROJ-001',
      plantCode: 'PLANT-A',
      blockId: 'BLOCK-1',
      question: 'Test question 1',
      fieldName: 'testField',
      stepNumber: 1,
      assignedTeam: 'CQS',
      status: 'OPEN',
      priorityLevel: 'HIGH',
      raisedBy: 'testuser',
      createdAt: '2024-01-01T10:00:00',
      daysOpen: 2,
      workflowId: 100
    },
    {
      id: 2,
      materialCode: 'MAT-002',
      materialName: 'Test Material 2',
      question: 'Test question 2',
      assignedTeam: 'CQS',
      status: 'RESOLVED',
      priorityLevel: 'MEDIUM',
      raisedBy: 'testuser2',
      resolvedBy: 'resolver',
      response: 'Test response',
      createdAt: '2024-01-01T09:00:00',
      resolvedAt: '2024-01-01T11:00:00',
      daysOpen: 1
    }
  ];

  const mockStats = {
    total: 2,
    open: 1,
    resolved: 1,
    overdue: 0,
    avgResolutionTime: 2.5
  };

  beforeEach(() => {
    fetch.mockClear();
    
    // Mock the queries endpoint
    fetch.mockImplementation((url) => {
      if (url.includes('/queries/inbox/CQS')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockQueries)
        });
      }
      if (url.includes('/queries/stats/count-open/CQS')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(1)
        });
      }
      if (url.includes('/queries/stats/count-resolved/CQS')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(1)
        });
      }
      if (url.includes('/queries/overdue')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([])
        });
      }
      if (url.includes('/queries/stats/avg-resolution-time/CQS')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(2.5)
        });
      }
      return Promise.reject(new Error('Unknown URL'));
    });
  });

  test('renders query inbox with enhanced material context', async () => {
    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    // Wait for data to load
    await waitFor(() => {
      expect(screen.getByText('CQS Team Query Inbox')).toBeInTheDocument();
    });

    // Check if enhanced material context is displayed
    expect(screen.getByText('MAT-001')).toBeInTheDocument();
    expect(screen.getByText('Test Material')).toBeInTheDocument();
    expect(screen.getByText('Project: PROJ-001')).toBeInTheDocument();
    expect(screen.getByText('Plant: PLANT-A | Block: BLOCK-1')).toBeInTheDocument();
  });

  test('displays enhanced statistics cards', async () => {
    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      expect(screen.getByText('Total Queries')).toBeInTheDocument();
      expect(screen.getByText('Open Queries')).toBeInTheDocument();
      expect(screen.getByText('Overdue')).toBeInTheDocument();
      expect(screen.getByText('Avg Resolution Time')).toBeInTheDocument();
    });
  });

  test('shows enhanced filters including project and plant', async () => {
    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText('Material ID/Name')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Project Code')).toBeInTheDocument();
      expect(screen.getByPlaceholderText('Plant/Block')).toBeInTheDocument();
    });
  });

  test('opens enhanced query detail modal with context and history', async () => {
    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      const viewButtons = screen.getAllByText('View');
      fireEvent.click(viewButtons[0]);
    });

    // Check if enhanced modal content is displayed
    await waitFor(() => {
      expect(screen.getByText('Query #1 Details')).toBeInTheDocument();
      expect(screen.getByText('Project: PROJ-001')).toBeInTheDocument();
      expect(screen.getByText('Plant: PLANT-A')).toBeInTheDocument();
      expect(screen.getByText('Block: BLOCK-1')).toBeInTheDocument();
    });

    // Check if context components are rendered
    expect(screen.getByTestId('material-context-display')).toBeInTheDocument();
    expect(screen.getByTestId('query-history-tracker')).toBeInTheDocument();
  });

  test('opens enhanced resolve query modal with better context', async () => {
    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      const resolveButtons = screen.getAllByText('Resolve');
      fireEvent.click(resolveButtons[0]);
    });

    // Check if enhanced resolve modal is displayed
    await waitFor(() => {
      expect(screen.getByText('Resolve Query #1')).toBeInTheDocument();
      expect(screen.getByText('Material: MAT-001')).toBeInTheDocument();
      expect(screen.getByText('Team:')).toBeInTheDocument();
      expect(screen.getByText('Priority:')).toBeInTheDocument();
    });

    // Check if response editor is present
    expect(screen.getByTestId('query-response-editor')).toBeInTheDocument();
  });

  test('filters queries by project code', async () => {
    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      const projectFilter = screen.getByPlaceholderText('Project Code');
      fireEvent.change(projectFilter, { target: { value: 'PROJ-001' } });
    });

    // The filtering logic should work (though we can't easily test the actual filtering without more complex mocking)
    expect(screen.getByDisplayValue('PROJ-001')).toBeInTheDocument();
  });

  test('filters queries by plant/block', async () => {
    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      const plantFilter = screen.getByPlaceholderText('Plant/Block');
      fireEvent.change(plantFilter, { target: { value: 'PLANT-A' } });
    });

    expect(screen.getByDisplayValue('PLANT-A')).toBeInTheDocument();
  });

  test('clears all filters including new ones', async () => {
    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      // Set some filter values
      const projectFilter = screen.getByPlaceholderText('Project Code');
      const plantFilter = screen.getByPlaceholderText('Plant/Block');
      
      fireEvent.change(projectFilter, { target: { value: 'PROJ-001' } });
      fireEvent.change(plantFilter, { target: { value: 'PLANT-A' } });
      
      // Clear filters
      const clearButton = screen.getByText('Clear Filters');
      fireEvent.click(clearButton);
    });

    // Check if filters are cleared
    expect(screen.getByPlaceholderText('Project Code')).toHaveValue('');
    expect(screen.getByPlaceholderText('Plant/Block')).toHaveValue('');
  });

  test('displays SLA progress indicators', async () => {
    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      expect(screen.getByText('2 days')).toBeInTheDocument();
    });

    // Check if progress bars are rendered (they should be in the DOM)
    const progressBars = document.querySelectorAll('.ant-progress');
    expect(progressBars.length).toBeGreaterThan(0);
  });

  test('shows team routing information', async () => {
    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      // Check if team tags are displayed
      const teamTags = screen.getAllByText('CQS');
      expect(teamTags.length).toBeGreaterThan(0);
    });
  });
});