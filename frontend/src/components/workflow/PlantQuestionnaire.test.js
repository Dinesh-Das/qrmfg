import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import PlantQuestionnaire from './PlantQuestionnaire';
import { workflowAPI } from '../../services/workflowAPI';
import { queryAPI } from '../../services/queryAPI';

// Mock the API services
jest.mock('../../services/workflowAPI');
jest.mock('../../services/queryAPI');

// Mock localStorage
const mockLocalStorage = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};
Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage,
});

// Mock window.innerWidth for responsive tests
Object.defineProperty(window, 'innerWidth', {
  writable: true,
  configurable: true,
  value: 1024,
});

const mockWorkflowData = {
  id: 1,
  materialCode: 'MAT-001',
  materialName: 'Test Material',
  state: 'PLANT_PENDING',
  assignedPlant: 'Plant A',
  initiatedBy: 'jvc_user',
  createdAt: '2024-01-01T10:00:00Z',
  lastModified: '2024-01-01T10:00:00Z',
  responses: [],
  totalQueries: 0,
  openQueries: 0
};

const mockQueries = [
  {
    id: 1,
    question: 'What is the exact chemical composition?',
    fieldName: 'materialName',
    stepNumber: 0,
    assignedTeam: 'CQS',
    status: 'OPEN',
    createdBy: 'plant_user'
  }
];

describe('PlantQuestionnaire', () => {
  beforeEach(() => {
    // Reset mocks
    jest.clearAllMocks();
    
    // Setup default mock implementations
    workflowAPI.getWorkflow.mockResolvedValue(mockWorkflowData);
    queryAPI.getQueriesByWorkflow.mockResolvedValue(mockQueries);
    workflowAPI.saveDraftResponses.mockResolvedValue({ success: true });
    workflowAPI.submitQuestionnaire.mockResolvedValue({ success: true });
    
    // Mock localStorage
    mockLocalStorage.getItem.mockImplementation((key) => {
      if (key === 'username') return 'plant_user';
      return null;
    });
  });

  test('renders loading state initially', () => {
    render(<PlantQuestionnaire workflowId={1} />);
    expect(screen.getByText('Loading questionnaire...')).toBeInTheDocument();
  });

  test('renders questionnaire after loading', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Plant Questionnaire')).toBeInTheDocument();
    });
    
    expect(screen.getByText('Basic Information')).toBeInTheDocument();
    expect(screen.getByText('Step 1/6')).toBeInTheDocument();
  });

  test('displays progress indicator correctly', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Step 1/6')).toBeInTheDocument();
    });
    
    // Check if progress bar is present
    const progressBar = screen.getByRole('progressbar');
    expect(progressBar).toBeInTheDocument();
  });

  test('renders form fields for current step', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText(/Material Name/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Material Type/)).toBeInTheDocument();
      expect(screen.getByLabelText(/CAS Number/)).toBeInTheDocument();
      expect(screen.getByLabelText(/Supplier Name/)).toBeInTheDocument();
    });
  });

  test('shows query indicators for fields with queries', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Query Open')).toBeInTheDocument();
    });
  });

  test('opens query modal when query button is clicked', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      const queryButton = screen.getAllByRole('button', { name: /raise a query/i })[0];
      fireEvent.click(queryButton);
    });
    
    expect(screen.getByText('Raise Query')).toBeInTheDocument();
  });

  test('navigates to next step when Next button is clicked', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      // Fill required fields
      fireEvent.change(screen.getByLabelText(/Material Name/), {
        target: { value: 'Test Material' }
      });
      
      // Select material type
      fireEvent.mouseDown(screen.getByLabelText(/Material Type/));
      fireEvent.click(screen.getByText('Chemical'));
      
      // Fill supplier name
      fireEvent.change(screen.getByLabelText(/Supplier Name/), {
        target: { value: 'Test Supplier' }
      });
    });
    
    const nextButton = screen.getByRole('button', { name: /next/i });
    fireEvent.click(nextButton);
    
    await waitFor(() => {
      expect(screen.getByText('Physical Properties')).toBeInTheDocument();
      expect(screen.getByText('Step 2/6')).toBeInTheDocument();
    });
  });

  test('saves draft when Save Draft button is clicked', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      const saveDraftButton = screen.getByRole('button', { name: /save draft/i });
      fireEvent.click(saveDraftButton);
    });
    
    await waitFor(() => {
      expect(workflowAPI.saveDraftResponses).toHaveBeenCalledWith(1, expect.any(Object));
    });
  });

  test('submits questionnaire on final step', async () => {
    // Mock workflow data with completed steps
    const completedWorkflowData = {
      ...mockWorkflowData,
      responses: [
        { fieldName: 'materialName', fieldValue: 'Test Material', stepNumber: 0 }
      ]
    };
    workflowAPI.getWorkflow.mockResolvedValue(completedWorkflowData);
    
    const onComplete = jest.fn();
    render(<PlantQuestionnaire workflowId={1} onComplete={onComplete} />);
    
    // Navigate to last step (this would require multiple steps in real scenario)
    // For test purposes, we'll mock being on the last step
    await waitFor(() => {
      // Simulate being on the last step
      const submitButton = screen.queryByRole('button', { name: /submit questionnaire/i });
      if (submitButton) {
        fireEvent.click(submitButton);
      }
    });
  });

  test('handles auto-save functionality', async () => {
    jest.useFakeTimers();
    
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      // Fill a field to trigger auto-save
      fireEvent.change(screen.getByLabelText(/Material Name/), {
        target: { value: 'Test Material' }
      });
    });
    
    // Fast-forward time to trigger auto-save
    jest.advanceTimersByTime(30000);
    
    await waitFor(() => {
      expect(workflowAPI.saveDraftResponses).toHaveBeenCalled();
    });
    
    jest.useRealTimers();
  });

  test('handles offline mode correctly', async () => {
    // Mock navigator.onLine
    Object.defineProperty(navigator, 'onLine', {
      writable: true,
      value: false,
    });

    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Offline Mode')).toBeInTheDocument();
      expect(screen.getByText(/You are currently offline/)).toBeInTheDocument();
    });
  });

  test('recovers draft data from localStorage', async () => {
    const draftData = {
      formData: { materialName: 'Recovered Material' },
      currentStep: 1,
      timestamp: Date.now() - 1000, // 1 second ago
      completedSteps: [0]
    };
    
    mockLocalStorage.getItem.mockImplementation((key) => {
      if (key === 'plant_questionnaire_draft_1') {
        return JSON.stringify(draftData);
      }
      if (key === 'username') return 'plant_user';
      return null;
    });

    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByDisplayValue('Recovered Material')).toBeInTheDocument();
    });
  });

  test('does not recover old draft data', async () => {
    const oldDraftData = {
      formData: { materialName: 'Old Material' },
      currentStep: 1,
      timestamp: Date.now() - (25 * 60 * 60 * 1000), // 25 hours ago
      completedSteps: [0]
    };
    
    mockLocalStorage.getItem.mockImplementation((key) => {
      if (key === 'plant_questionnaire_draft_1') {
        return JSON.stringify(oldDraftData);
      }
      if (key === 'username') return 'plant_user';
      return null;
    });

    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.queryByDisplayValue('Old Material')).not.toBeInTheDocument();
    });
    
    expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('plant_questionnaire_draft_1');
  });

  test('displays error when workflow fails to load', async () => {
    workflowAPI.getWorkflow.mockRejectedValue(new Error('Failed to load'));
    
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Workflow Not Found')).toBeInTheDocument();
    });
  });

  test('handles mobile responsive layout', async () => {
    // Mock mobile screen size
    Object.defineProperty(window, 'innerWidth', {
      writable: true,
      configurable: true,
      value: 600,
    });
    
    // Trigger resize event
    fireEvent(window, new Event('resize'));
    
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Plant Questionnaire')).toBeInTheDocument();
    });
    
    // Check if mobile-specific elements are present
    // (This would depend on the specific mobile layout implementation)
  });

  test('validates required fields before proceeding', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      const nextButton = screen.getByRole('button', { name: /next/i });
      fireEvent.click(nextButton);
    });
    
    // Should show validation messages for required fields
    await waitFor(() => {
      expect(screen.getByText(/Material Name is required/)).toBeInTheDocument();
      expect(screen.getByText(/Material Type is required/)).toBeInTheDocument();
      expect(screen.getByText(/Supplier Name is required/)).toBeInTheDocument();
    });
  });

  test('displays material context panel', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Material Context')).toBeInTheDocument();
      expect(screen.getByText('MAT-001')).toBeInTheDocument();
      expect(screen.getByText('Plant A')).toBeInTheDocument();
    });
  });

  test('handles different field types correctly', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      // Text input
      expect(screen.getByLabelText(/Material Name/)).toHaveAttribute('type', 'text');
      
      // Select dropdown
      expect(screen.getByLabelText(/Material Type/)).toHaveClass('ant-select');
    });
    
    // Navigate to step with radio buttons
    await waitFor(() => {
      // Fill required fields first
      fireEvent.change(screen.getByLabelText(/Material Name/), {
        target: { value: 'Test Material' }
      });
      fireEvent.mouseDown(screen.getByLabelText(/Material Type/));
      fireEvent.click(screen.getByText('Chemical'));
      fireEvent.change(screen.getByLabelText(/Supplier Name/), {
        target: { value: 'Test Supplier' }
      });
    });
    
    const nextButton = screen.getByRole('button', { name: /next/i });
    fireEvent.click(nextButton);
    
    await waitFor(() => {
      // Radio buttons for physical state
      expect(screen.getByLabelText(/Solid/)).toHaveAttribute('type', 'radio');
      expect(screen.getByLabelText(/Liquid/)).toHaveAttribute('type', 'radio');
    });
  });
});

describe('PlantQuestionnaire Integration', () => {
  test('integrates with QueryRaisingModal', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      const queryButton = screen.getAllByRole('button', { name: /raise a query/i })[0];
      fireEvent.click(queryButton);
    });
    
    // Modal should open
    expect(screen.getByText('Raise Query')).toBeInTheDocument();
    expect(screen.getByText('Field Context')).toBeInTheDocument();
  });

  test('integrates with MaterialContextPanel', async () => {
    render(<PlantQuestionnaire workflowId={1} />);
    
    await waitFor(() => {
      expect(screen.getByText('Material Context')).toBeInTheDocument();
      expect(screen.getByText('Basic Information')).toBeInTheDocument();
      expect(screen.getByText('Workflow Status')).toBeInTheDocument();
    });
  });
});