import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import QueryHistoryTracker from './QueryHistoryTracker';

// Mock the API calls
global.fetch = jest.fn();

describe('QueryHistoryTracker', () => {
  const mockHistoryData = [
    {
      id: 1,
      materialCode: 'MAT-001',
      question: 'Test question 1',
      fieldName: 'testField',
      stepNumber: 1,
      assignedTeam: 'CQS',
      status: 'RESOLVED',
      priorityLevel: 'HIGH',
      raisedBy: 'testuser',
      resolvedBy: 'resolver',
      response: 'Test response',
      createdAt: '2024-01-01T10:00:00',
      resolvedAt: '2024-01-01T12:00:00',
      daysOpen: 1
    },
    {
      id: 2,
      materialCode: 'MAT-001',
      question: 'Test question 2',
      assignedTeam: 'TECH',
      status: 'OPEN',
      priorityLevel: 'MEDIUM',
      raisedBy: 'testuser2',
      createdAt: '2024-01-02T10:00:00',
      daysOpen: 2
    }
  ];

  beforeEach(() => {
    fetch.mockClear();
    
    fetch.mockImplementation((url) => {
      if (url.includes('/queries/material/MAT-001')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockHistoryData)
        });
      }
      if (url.includes('/queries/workflow/100')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockHistoryData)
        });
      }
      if (url.includes('/queries/1/history')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([mockHistoryData[0]])
        });
      }
      return Promise.reject(new Error('Unknown URL'));
    });
  });

  test('renders query history tracker with material code', async () => {
    render(<QueryHistoryTracker materialCode="MAT-001" />);

    await waitFor(() => {
      expect(screen.getByText('Query History & Resolution Tracking')).toBeInTheDocument();
    });

    // Check if statistics are displayed
    expect(screen.getByText('Total Queries')).toBeInTheDocument();
    expect(screen.getByText('Resolved')).toBeInTheDocument();
    expect(screen.getByText('Avg Resolution Time')).toBeInTheDocument();
    expect(screen.getByText('Overdue Queries')).toBeInTheDocument();
  });

  test('renders compact version', async () => {
    render(<QueryHistoryTracker materialCode="MAT-001" compact={true} />);

    await waitFor(() => {
      expect(screen.getByText('Query History')).toBeInTheDocument();
    });

    // In compact mode, should show basic stats
    expect(screen.getByText('Total')).toBeInTheDocument();
    expect(screen.getByText('Resolved')).toBeInTheDocument();
    expect(screen.getByText('Avg Days')).toBeInTheDocument();
    expect(screen.getByText('Overdue')).toBeInTheDocument();
  });

  test('displays timeline with query details', async () => {
    render(<QueryHistoryTracker materialCode="MAT-001" />);

    await waitFor(() => {
      expect(screen.getByText('Query #1')).toBeInTheDocument();
      expect(screen.getByText('Query #2')).toBeInTheDocument();
    });

    // Check if query details are displayed
    expect(screen.getByText('Test question 1')).toBeInTheDocument();
    expect(screen.getByText('Test question 2')).toBeInTheDocument();
    expect(screen.getByText('Field: testField (Step 1)')).toBeInTheDocument();
  });

  test('shows resolved query with response', async () => {
    render(<QueryHistoryTracker materialCode="MAT-001" />);

    await waitFor(() => {
      expect(screen.getByText('Resolution:')).toBeInTheDocument();
      expect(screen.getByText('Test response')).toBeInTheDocument();
      expect(screen.getByText(/Resolved by: resolver/)).toBeInTheDocument();
    });
  });

  test('displays team and priority tags', async () => {
    render(<QueryHistoryTracker materialCode="MAT-001" />);

    await waitFor(() => {
      expect(screen.getByText('CQS')).toBeInTheDocument();
      expect(screen.getByText('TECH')).toBeInTheDocument();
      expect(screen.getByText('HIGH')).toBeInTheDocument();
      expect(screen.getByText('MEDIUM')).toBeInTheDocument();
    });
  });

  test('shows status indicators', async () => {
    render(<QueryHistoryTracker materialCode="MAT-001" />);

    await waitFor(() => {
      expect(screen.getByText('RESOLVED')).toBeInTheDocument();
      expect(screen.getByText('OPEN')).toBeInTheDocument();
    });
  });

  test('calculates and displays statistics correctly', async () => {
    render(<QueryHistoryTracker materialCode="MAT-001" />);

    await waitFor(() => {
      // Should show 2 total queries
      const totalStats = screen.getAllByText('2');
      expect(totalStats.length).toBeGreaterThan(0);
      
      // Should show 1 resolved query
      const resolvedStats = screen.getAllByText('1');
      expect(resolvedStats.length).toBeGreaterThan(0);
    });
  });

  test('handles refresh functionality', async () => {
    render(<QueryHistoryTracker materialCode="MAT-001" />);

    await waitFor(() => {
      const refreshButton = screen.getByText('Refresh');
      fireEvent.click(refreshButton);
    });

    // Should make another API call
    expect(fetch).toHaveBeenCalledTimes(2); // Initial load + refresh
  });

  test('works with workflow ID', async () => {
    render(<QueryHistoryTracker workflowId={100} />);

    await waitFor(() => {
      expect(screen.getByText('Query History & Resolution Tracking')).toBeInTheDocument();
    });

    // Should call the workflow endpoint
    expect(fetch).toHaveBeenCalledWith('/qrmfg/api/v1/queries/workflow/100');
  });

  test('works with specific query ID', async () => {
    render(<QueryHistoryTracker queryId={1} />);

    await waitFor(() => {
      expect(screen.getByText('Query History & Resolution Tracking')).toBeInTheDocument();
    });

    // Should call the query history endpoint
    expect(fetch).toHaveBeenCalledWith('/qrmfg/api/v1/queries/1/history');
  });

  test('shows empty state when no history available', async () => {
    fetch.mockImplementationOnce(() => 
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([])
      })
    );

    render(<QueryHistoryTracker materialCode="MAT-001" />);

    await waitFor(() => {
      expect(screen.getByText('No query history available')).toBeInTheDocument();
    });
  });

  test('handles loading state', () => {
    // Mock a delayed response
    fetch.mockImplementationOnce(() => 
      new Promise(resolve => 
        setTimeout(() => resolve({
          ok: true,
          json: () => Promise.resolve(mockHistoryData)
        }), 100)
      )
    );

    render(<QueryHistoryTracker materialCode="MAT-001" />);

    expect(screen.getByText('Loading query history...')).toBeInTheDocument();
  });

  test('handles API errors gracefully', async () => {
    const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    
    fetch.mockImplementationOnce(() => 
      Promise.reject(new Error('API Error'))
    );

    render(<QueryHistoryTracker materialCode="MAT-001" />);

    await waitFor(() => {
      expect(consoleSpy).toHaveBeenCalledWith('Failed to load query history:', expect.any(Error));
    });

    consoleSpy.mockRestore();
  });
});