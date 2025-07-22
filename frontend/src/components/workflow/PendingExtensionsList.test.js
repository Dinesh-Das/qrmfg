import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { message } from 'antd';
import PendingExtensionsList from './PendingExtensionsList';
import { workflowAPI } from '../../services/workflowAPI';
import { documentAPI } from '../../services/documentAPI';

// Mock the API services
jest.mock('../../services/workflowAPI');
jest.mock('../../services/documentAPI');

// Mock antd message
jest.mock('antd', () => ({
  ...jest.requireActual('antd'),
  message: {
    error: jest.fn(),
    success: jest.fn(),
  },
}));

describe('PendingExtensionsList', () => {
  const mockOnExtendToPlant = jest.fn();
  
  const mockPendingWorkflows = [
    {
      id: 1,
      projectCode: 'SER-A-000210',
      materialCode: 'R31516J',
      plantCode: '1001',
      blockId: '1001-A',
      createdAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(), // 2 days ago
      state: 'JVC_PENDING'
    },
    {
      id: 2,
      projectCode: 'SER-B-000211',
      materialCode: 'R31517K',
      plantCode: '1002',
      blockId: '1002-B',
      createdAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(), // 5 days ago (overdue)
      state: 'JVC_PENDING'
    }
  ];

  const mockDocuments = [
    {
      id: 1,
      originalFileName: 'test-document.pdf',
      fileType: 'pdf',
      fileSize: 1024000,
      uploadedAt: new Date().toISOString(),
      uploadedBy: 'test-user',
      isReused: false
    }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Setup API mocks
    workflowAPI.getWorkflowsByState.mockResolvedValue(mockPendingWorkflows);
    documentAPI.getWorkflowDocuments.mockResolvedValue(mockDocuments);
    documentAPI.downloadDocument.mockResolvedValue(new Blob());
  });

  test('renders pending extensions list', async () => {
    render(<PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />);
    
    await waitFor(() => {
      expect(screen.getByText(/pending extensions/i)).toBeInTheDocument();
      expect(screen.getByText('SER-A-000210')).toBeInTheDocument();
      expect(screen.getByText('SER-B-000211')).toBeInTheDocument();
    });
  });

  test('loads pending workflows on mount', async () => {
    render(<PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />);
    
    await waitFor(() => {
      expect(workflowAPI.getWorkflowsByState).toHaveBeenCalledWith('JVC_PENDING');
    });
  });

  test('displays SLA status correctly', async () => {
    render(<PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />);
    
    await waitFor(() => {
      // Should show warning for 2-day old workflow
      expect(screen.getByText('Warning')).toBeInTheDocument();
      // Should show overdue for 5-day old workflow
      expect(screen.getByText('Overdue')).toBeInTheDocument();
    });
  });

  test('filters workflows by search term', async () => {
    render(<PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />);
    
    await waitFor(() => {
      expect(screen.getByText('SER-A-000210')).toBeInTheDocument();
      expect(screen.getByText('SER-B-000211')).toBeInTheDocument();
    });

    // Search for specific project
    const searchInput = screen.getByPlaceholderText(/search by project/i);
    fireEvent.change(searchInput, { target: { value: 'SER-A' } });

    await waitFor(() => {
      expect(screen.getByText('SER-A-000210')).toBeInTheDocument();
      expect(screen.queryByText('SER-B-000211')).not.toBeInTheDocument();
    });
  });

  test('filters workflows by project code', async () => {
    render(<PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />);
    
    await waitFor(() => {
      expect(screen.getByText('SER-A-000210')).toBeInTheDocument();
      expect(screen.getByText('SER-B-000211')).toBeInTheDocument();
    });

    // Filter by project code
    const projectFilter = screen.getByDisplayValue('');
    fireEvent.mouseDown(projectFilter);
    
    await waitFor(() => {
      const option = screen.getByText('SER-A-000210');
      fireEvent.click(option);
    });

    await waitFor(() => {
      expect(screen.getByText('SER-A-000210')).toBeInTheDocument();
      expect(screen.queryByText('SER-B-000211')).not.toBeInTheDocument();
    });
  });

  test('opens workflow details modal', async () => {
    render(<PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />);
    
    await waitFor(() => {
      expect(screen.getByText('SER-A-000210')).toBeInTheDocument();
    });

    // Click view details button
    const viewButtons = screen.getAllByRole('button', { name: /view details/i });
    fireEvent.click(viewButtons[0]);

    await waitFor(() => {
      expect(screen.getByText(/workflow details/i)).toBeInTheDocument();
      expect(documentAPI.getWorkflowDocuments).toHaveBeenCalledWith(1);
    });
  });

  test('extends workflow to plant', async () => {
    mockOnExtendToPlant.mockResolvedValue();
    
    render(<PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />);
    
    await waitFor(() => {
      expect(screen.getByText('SER-A-000210')).toBeInTheDocument();
    });

    // Click extend to plant button
    const extendButtons = screen.getAllByRole('button', { name: /extend to plant/i });
    fireEvent.click(extendButtons[0]);

    await waitFor(() => {
      expect(mockOnExtendToPlant).toHaveBeenCalledWith(mockPendingWorkflows[0]);
    });
  });

  test('resets filters', async () => {
    render(<PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />);
    
    await waitFor(() => {
      expect(screen.getByText('SER-A-000210')).toBeInTheDocument();
    });

    // Apply search filter
    const searchInput = screen.getByPlaceholderText(/search by project/i);
    fireEvent.change(searchInput, { target: { value: 'SER-A' } });

    // Reset filters
    const resetButton = screen.getByRole('button', { name: /reset/i });
    fireEvent.click(resetButton);

    await waitFor(() => {
      expect(searchInput.value).toBe('');
      expect(screen.getByText('SER-A-000210')).toBeInTheDocument();
      expect(screen.getByText('SER-B-000211')).toBeInTheDocument();
    });
  });

  test('refreshes data when refreshTrigger changes', async () => {
    const { rerender } = render(
      <PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />
    );
    
    await waitFor(() => {
      expect(workflowAPI.getWorkflowsByState).toHaveBeenCalledTimes(1);
    });

    // Change refreshTrigger
    rerender(
      <PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={1} />
    );

    await waitFor(() => {
      expect(workflowAPI.getWorkflowsByState).toHaveBeenCalledTimes(2);
    });
  });

  test('handles API errors gracefully', async () => {
    workflowAPI.getWorkflowsByState.mockRejectedValue(new Error('API Error'));
    
    render(<PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />);
    
    await waitFor(() => {
      expect(message.error).toHaveBeenCalledWith('Failed to load pending workflows');
    });
  });

  test('calculates days pending correctly', async () => {
    render(<PendingExtensionsList onExtendToPlant={mockOnExtendToPlant} refreshTrigger={0} />);
    
    await waitFor(() => {
      // Should show 2 days for first workflow
      expect(screen.getByText('2 days')).toBeInTheDocument();
      // Should show 5 days for second workflow
      expect(screen.getByText('5 days')).toBeInTheDocument();
    });
  });
});