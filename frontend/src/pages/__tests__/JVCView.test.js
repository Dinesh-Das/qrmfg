import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { message } from 'antd';
import JVCView from '../JVCView';
import { workflowAPI } from '../../services/workflowAPI';

// Mock the workflowAPI
jest.mock('../../services/workflowAPI');

// Mock antd message
jest.mock('antd', () => ({
  ...jest.requireActual('antd'),
  message: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

describe('JVCView', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    
    // Mock API responses
    workflowAPI.getWorkflowsByState.mockImplementation((state) => {
      if (state === 'JVC_PENDING') {
        return Promise.resolve([
          {
            id: 1,
            materialCode: 'MAT-001-2024',
            assignedPlant: 'plant-a',
            createdAt: '2024-01-15T10:00:00Z',
            state: 'JVC_PENDING'
          }
        ]);
      }
      if (state === 'COMPLETED') {
        return Promise.resolve([
          {
            id: 2,
            materialCode: 'MAT-002-2024',
            assignedPlant: 'plant-b',
            lastModified: '2024-01-10T15:30:00Z',
            state: 'COMPLETED'
          }
        ]);
      }
      return Promise.resolve([]);
    });
    
    workflowAPI.createWorkflow.mockResolvedValue({ id: 3, materialCode: 'MAT-003-2024' });
    workflowAPI.extendWorkflow.mockResolvedValue({ success: true });
  });

  test('renders JVC dashboard with all tabs', async () => {
    render(<JVCView />);
    
    expect(screen.getByText('JVC Dashboard')).toBeInTheDocument();
    expect(screen.getByText('Initiate MSDS workflows and manage material safety documentation process')).toBeInTheDocument();
    
    // Check tabs are present
    expect(screen.getByText('Initiate Workflow')).toBeInTheDocument();
    expect(screen.getByText(/Pending Extensions/)).toBeInTheDocument();
    expect(screen.getByText(/Completed/)).toBeInTheDocument();
  });

  test('displays material initiation form with required fields', () => {
    render(<JVCView />);
    
    // Check form fields
    expect(screen.getByLabelText('Material Code')).toBeInTheDocument();
    expect(screen.getByLabelText('Plant Selection')).toBeInTheDocument();
    expect(screen.getByLabelText('Safety Documents')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Initiate Workflow/ })).toBeInTheDocument();
  });

  test('validates material ID format', async () => {
    render(<JVCView />);
    
    const materialCodeInput = screen.getByLabelText('Material Code');
    const submitButton = screen.getByRole('button', { name: /Initiate Workflow/ });
    
    // Test invalid format
    fireEvent.change(materialCodeInput, { target: { value: 'invalid-format' } });
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(screen.getByText('Material Code should contain only uppercase letters, numbers, and hyphens')).toBeInTheDocument();
    });
  });

  test('successfully initiates workflow with valid data', async () => {
    render(<JVCView />);
    
    const materialCodeInput = screen.getByLabelText('Material Code');
    const plantSelect = screen.getByLabelText('Plant Selection');
    const submitButton = screen.getByRole('button', { name: /Initiate Workflow/ });
    
    // Fill form with valid data
    fireEvent.change(materialCodeInput, { target: { value: 'MAT-003-2024' } });
    fireEvent.mouseDown(plantSelect);
    fireEvent.click(screen.getByText('Plant A - Manufacturing'));
    
    fireEvent.click(submitButton);
    
    await waitFor(() => {
      expect(workflowAPI.createWorkflow).toHaveBeenCalledWith({
        materialCode: 'MAT-003-2024',
        assignedPlant: 'plant-a',
        initiatedBy: 'current-user',
        documents: []
      });
      expect(message.success).toHaveBeenCalledWith('Material workflow initiated successfully');
    });
  });

  test('displays pending workflows in table', async () => {
    render(<JVCView />);
    
    // Switch to pending tab
    fireEvent.click(screen.getByText(/Pending Extensions/));
    
    await waitFor(() => {
      expect(screen.getByText('MAT-001-2024')).toBeInTheDocument();
      expect(screen.getByText('Plant A - Manufacturing')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /Extend to Plant/ })).toBeInTheDocument();
    });
  });

  test('extends workflow to plant with confirmation', async () => {
    render(<JVCView />);
    
    // Switch to pending tab
    fireEvent.click(screen.getByText(/Pending Extensions/));
    
    await waitFor(() => {
      const extendButton = screen.getByRole('button', { name: /Extend to Plant/ });
      fireEvent.click(extendButton);
    });
    
    // Check modal appears
    await waitFor(() => {
      expect(screen.getByText('Extend Workflow to Plant')).toBeInTheDocument();
      expect(screen.getByText('MAT-001-2024')).toBeInTheDocument();
    });
    
    // Confirm extension
    const confirmButton = screen.getByRole('button', { name: 'Extend to Plant' });
    fireEvent.click(confirmButton);
    
    await waitFor(() => {
      expect(workflowAPI.extendWorkflow).toHaveBeenCalledWith(1, {
        assignedPlant: 'plant-a',
        comment: 'Extended to plant for questionnaire completion'
      });
      expect(message.success).toHaveBeenCalledWith('Workflow extended to plant-a');
    });
  });

  test('displays completed workflows', async () => {
    render(<JVCView />);
    
    // Switch to completed tab
    fireEvent.click(screen.getByText(/Completed/));
    
    await waitFor(() => {
      expect(screen.getByText('MAT-002-2024')).toBeInTheDocument();
      expect(screen.getByText('Plant B - Assembly')).toBeInTheDocument();
    });
  });

  test('shows quick stats', async () => {
    render(<JVCView />);
    
    await waitFor(() => {
      expect(screen.getByText('Pending Extensions')).toBeInTheDocument();
      expect(screen.getByText('Completed This Month')).toBeInTheDocument();
      expect(screen.getByText('1')).toBeInTheDocument(); // Pending count
    });
  });

  test('handles API errors gracefully', async () => {
    workflowAPI.getWorkflowsByState.mockRejectedValue(new Error('API Error'));
    
    render(<JVCView />);
    
    await waitFor(() => {
      expect(message.error).toHaveBeenCalledWith('Failed to load workflows');
    });
  });

  test('validates file upload restrictions', () => {
    render(<JVCView />);
    
    // This would require more complex setup to test file upload
    // For now, we verify the upload component is present
    expect(screen.getByText('Upload Documents')).toBeInTheDocument();
  });

  test('resets form when reset button is clicked', async () => {
    render(<JVCView />);
    
    const materialCodeInput = screen.getByLabelText('Material Code');
    const resetButton = screen.getByRole('button', { name: /Reset/ });
    
    // Fill form
    fireEvent.change(materialCodeInput, { target: { value: 'MAT-TEST-2024' } });
    expect(materialCodeInput.value).toBe('MAT-TEST-2024');
    
    // Reset form
    fireEvent.click(resetButton);
    
    await waitFor(() => {
      expect(materialCodeInput.value).toBe('');
    });
  });
});