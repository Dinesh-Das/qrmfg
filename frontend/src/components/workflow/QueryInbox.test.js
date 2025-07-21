import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { message } from 'antd';
import QueryInbox from './QueryInbox';

// Mock the API calls
global.fetch = jest.fn();

// Mock antd message
jest.mock('antd', () => ({
  ...jest.requireActual('antd'),
  message: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

// Mock child components
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
  return function MockMaterialContextDisplay({ materialId, workflowId }) {
    return (
      <div data-testid="material-context-display">
        Material: {materialId}, Workflow: {workflowId}
      </div>
    );
  };
});

const mockQueries = [
  {
    id: 1,
    materialId: 'MAT001',
    materialName: 'Test Material',
    question: 'What is the flash point?',
    fieldContext: 'Safety Data',
    stepNumber: 3,
    priorityLevel: 'HIGH',
    status: 'OPEN',
    daysOpen: 2,
    raisedBy: 'plant_user',
    assignedTeam: 'CQS',
    createdAt: '2024-01-15T10:00:00Z',
    workflowId: 123
  },
  {
    id: 2,
    materialId: 'MAT002',
    materialName: 'Another Material',
    question: 'Storage requirements?',
    fieldContext: 'Storage',
    stepNumber: 5,
    priorityLevel: 'MEDIUM',
    status: 'RESOLVED',
    daysOpen: 1,
    raisedBy: 'plant_user2',
    resolvedBy: 'cqs_user',
    response: 'Store in cool, dry place',
    assignedTeam: 'CQS',
    createdAt: '2024-01-16T10:00:00Z',
    resolvedAt: '2024-01-17T10:00:00Z',
    workflowId: 124
  }
];

const mockStats = {
  openCount: 5,
  resolvedCount: 10,
  avgTime: 24.5
};

describe('QueryInbox', () => {
  beforeEach(() => {
    fetch.mockClear();
    message.success.mockClear();
    message.error.mockClear();
  });

  const setupMockFetch = () => {
    fetch
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockQueries)
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockStats.openCount)
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockStats.resolvedCount)
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([])
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(mockStats.avgTime)
      });
  };

  test('renders query inbox with statistics', async () => {
    setupMockFetch();

    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    expect(screen.getByText('CQS Team Query Inbox')).toBeInTheDocument();
    
    // Check statistics cards
    expect(screen.getByText('Total Queries')).toBeInTheDocument();
    expect(screen.getByText('Open Queries')).toBeInTheDocument();
    expect(screen.getByText('Overdue')).toBeInTheDocument();
    expect(screen.getByText('Avg Resolution Time')).toBeInTheDocument();

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith('/qrmfg/api/v1/queries/inbox/CQS');
    });
  });

  test('displays queries in table', async () => {
    setupMockFetch();

    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      expect(screen.getByText('MAT001')).toBeInTheDocument();
      expect(screen.getByText('What is the flash point?')).toBeInTheDocument();
      expect(screen.getByText('Safety Data')).toBeInTheDocument();
      expect(screen.getByText('HIGH')).toBeInTheDocument();
    });
  });

  test('filters queries by status', async () => {
    setupMockFetch();

    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      expect(screen.getByText('MAT001')).toBeInTheDocument();
      expect(screen.getByText('MAT002')).toBeInTheDocument();
    });

    // Filter by OPEN status
    const statusFilter = screen.getByDisplayValue('All Status');
    fireEvent.change(statusFilter, { target: { value: 'OPEN' } });

    await waitFor(() => {
      expect(screen.getByText('MAT001')).toBeInTheDocument();
      expect(screen.queryByText('MAT002')).not.toBeInTheDocument();
    });
  });

  test('opens query detail modal', async () => {
    setupMockFetch();

    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      const viewButtons = screen.getAllByText('View');
      fireEvent.click(viewButtons[0]);
    });

    expect(screen.getByText('Query #1 Details')).toBeInTheDocument();
    expect(screen.getByText('What is the flash point?')).toBeInTheDocument();
  });

  test('opens resolve query modal', async () => {
    setupMockFetch();

    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      const resolveButtons = screen.getAllByText('Resolve');
      fireEvent.click(resolveButtons[0]);
    });

    expect(screen.getByText('Resolve Query #1')).toBeInTheDocument();
    expect(screen.getByTestId('query-response-editor')).toBeInTheDocument();
    expect(screen.getByTestId('material-context-display')).toBeInTheDocument();
  });

  test('resolves query successfully', async () => {
    setupMockFetch();
    
    // Mock the resolve API call
    fetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve({ success: true })
    });

    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      const resolveButtons = screen.getAllByText('Resolve');
      fireEvent.click(resolveButtons[0]);
    });

    // Fill in the response
    const responseEditor = screen.getByTestId('query-response-editor');
    fireEvent.change(responseEditor, { target: { value: 'Flash point is 25°C' } });

    // Submit the form
    const okButton = screen.getByText('OK');
    fireEvent.click(okButton);

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith('/qrmfg/api/v1/queries/1/resolve', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          response: 'Flash point is 25°C',
          priorityLevel: 'HIGH'
        })
      });
      expect(message.success).toHaveBeenCalledWith('Query resolved successfully');
    });
  });

  test('handles API error gracefully', async () => {
    fetch.mockRejectedValueOnce(new Error('API Error'));

    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      expect(message.error).toHaveBeenCalledWith('Failed to load queries');
    });
  });

  test('shows overdue alert when queries are overdue', async () => {
    const overdueQueries = [
      {
        ...mockQueries[0],
        daysOpen: 4,
        assignedTeam: 'CQS'
      }
    ];

    fetch
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve([overdueQueries[0]])
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(1)
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(0)
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(overdueQueries)
      })
      .mockResolvedValueOnce({
        ok: true,
        json: () => Promise.resolve(48)
      });

    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      expect(screen.getByText(/queries are overdue/)).toBeInTheDocument();
    });
  });

  test('refreshes data when refresh button is clicked', async () => {
    setupMockFetch();

    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      expect(screen.getByText('MAT001')).toBeInTheDocument();
    });

    // Setup fresh mock for refresh
    setupMockFetch();

    const refreshButton = screen.getByText('Refresh');
    fireEvent.click(refreshButton);

    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(10); // 5 initial + 5 refresh calls
    });
  });

  test('clears filters when clear filters button is clicked', async () => {
    setupMockFetch();

    render(<QueryInbox team="CQS" userRole="CQS_USER" />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('All Status')).toBeInTheDocument();
    });

    // Set some filters
    const statusFilter = screen.getByDisplayValue('All Status');
    fireEvent.change(statusFilter, { target: { value: 'OPEN' } });

    const materialFilter = screen.getByPlaceholderText('Material ID/Name');
    fireEvent.change(materialFilter, { target: { value: 'MAT001' } });

    // Clear filters
    const clearButton = screen.getByText('Clear Filters');
    fireEvent.click(clearButton);

    expect(screen.getByDisplayValue('All Status')).toBeInTheDocument();
    expect(materialFilter.value).toBe('');
  });
});