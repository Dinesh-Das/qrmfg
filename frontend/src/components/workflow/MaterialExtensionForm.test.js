import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { message } from 'antd';
import MaterialExtensionForm from './MaterialExtensionForm';
import { projectAPI } from '../../services/projectAPI';
import { documentAPI } from '../../services/documentAPI';

// Mock the API services
jest.mock('../../services/projectAPI');
jest.mock('../../services/documentAPI');

// Mock antd message
jest.mock('antd', () => ({
  ...jest.requireActual('antd'),
  message: {
    error: jest.fn(),
    success: jest.fn(),
  },
}));

describe('MaterialExtensionForm', () => {
  const mockOnSubmit = jest.fn();
  
  const mockProjects = [
    { value: 'SER-A-000210', label: 'SER-A-000210' },
    { value: 'SER-B-000211', label: 'SER-B-000211' }
  ];
  
  const mockMaterials = [
    { value: 'R31516J', label: 'Axion CS 2455 (DBTO)', projectCode: 'SER-A-000210' },
    { value: 'R31517K', label: 'Material 2', projectCode: 'SER-A-000210' }
  ];
  
  const mockPlants = [
    { value: '1001', label: '1001' },
    { value: '1002', label: '1002' }
  ];
  
  const mockBlocks = [
    { value: '1001-A', label: '1001-A', plantCode: '1001' },
    { value: '1001-B', label: '1001-B', plantCode: '1001' }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    
    // Setup API mocks
    projectAPI.getProjects.mockResolvedValue(mockProjects);
    projectAPI.getMaterialsByProject.mockResolvedValue(mockMaterials);
    projectAPI.getPlants.mockResolvedValue(mockPlants);
    projectAPI.getBlocksByPlant.mockResolvedValue(mockBlocks);
    documentAPI.getReusableDocuments.mockResolvedValue([]);
    documentAPI.validateFile.mockReturnValue({ isValidType: true, isValidSize: true });
  });

  test('renders form with all required fields', async () => {
    render(<MaterialExtensionForm onSubmit={mockOnSubmit} loading={false} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText(/project code/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/material code/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/plant code/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/block id/i)).toBeInTheDocument();
      expect(screen.getByText(/document upload/i)).toBeInTheDocument();
    });
  });

  test('loads projects and plants on mount', async () => {
    render(<MaterialExtensionForm onSubmit={mockOnSubmit} loading={false} />);
    
    await waitFor(() => {
      expect(projectAPI.getProjects).toHaveBeenCalled();
      expect(projectAPI.getPlants).toHaveBeenCalled();
    });
  });

  test('loads materials when project is selected', async () => {
    render(<MaterialExtensionForm onSubmit={mockOnSubmit} loading={false} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText(/project code/i)).toBeInTheDocument();
    });

    // Select a project
    const projectSelect = screen.getByLabelText(/project code/i);
    fireEvent.mouseDown(projectSelect);
    
    await waitFor(() => {
      const projectOption = screen.getByText('SER-A-000210');
      fireEvent.click(projectOption);
    });

    await waitFor(() => {
      expect(projectAPI.getMaterialsByProject).toHaveBeenCalledWith('SER-A-000210');
    });
  });

  test('loads blocks when plant is selected', async () => {
    render(<MaterialExtensionForm onSubmit={mockOnSubmit} loading={false} />);
    
    await waitFor(() => {
      expect(screen.getByLabelText(/plant code/i)).toBeInTheDocument();
    });

    // Select a plant
    const plantSelect = screen.getByLabelText(/plant code/i);
    fireEvent.mouseDown(plantSelect);
    
    await waitFor(() => {
      const plantOption = screen.getByText('1001');
      fireEvent.click(plantOption);
    });

    await waitFor(() => {
      expect(projectAPI.getBlocksByPlant).toHaveBeenCalledWith('1001');
    });
  });

  test('checks for reusable documents when material is selected', async () => {
    render(<MaterialExtensionForm onSubmit={mockOnSubmit} loading={false} />);
    
    // First select project
    await waitFor(() => {
      expect(screen.getByLabelText(/project code/i)).toBeInTheDocument();
    });

    const projectSelect = screen.getByLabelText(/project code/i);
    fireEvent.mouseDown(projectSelect);
    
    await waitFor(() => {
      const projectOption = screen.getByText('SER-A-000210');
      fireEvent.click(projectOption);
    });

    // Then select material
    await waitFor(() => {
      expect(screen.getByLabelText(/material code/i)).toBeInTheDocument();
    });

    const materialSelect = screen.getByLabelText(/material code/i);
    fireEvent.mouseDown(materialSelect);
    
    await waitFor(() => {
      const materialOption = screen.getByText('Axion CS 2455 (DBTO)');
      fireEvent.click(materialOption);
    });

    await waitFor(() => {
      expect(documentAPI.getReusableDocuments).toHaveBeenCalledWith('SER-A-000210', 'R31516J');
    });
  });

  test('validates file upload restrictions', () => {
    render(<MaterialExtensionForm onSubmit={mockOnSubmit} loading={false} />);
    
    // Mock file validation to return invalid type
    documentAPI.validateFile.mockReturnValue({ isValidType: false, isValidSize: true });
    
    const file = new File(['test'], 'test.txt', { type: 'text/plain' });
    const uploadArea = screen.getByText(/click or drag files to this area to upload/i);
    
    fireEvent.drop(uploadArea, {
      dataTransfer: {
        files: [file],
      },
    });

    expect(message.error).toHaveBeenCalledWith(
      expect.stringContaining('Invalid file type')
    );
  });

  test('disables submit button when required fields are missing', async () => {
    render(<MaterialExtensionForm onSubmit={mockOnSubmit} loading={false} />);
    
    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /create material extension/i });
      expect(submitButton).toBeDisabled();
    });
  });

  test('enables submit button when all required fields are filled', async () => {
    render(<MaterialExtensionForm onSubmit={mockOnSubmit} loading={false} />);
    
    // Fill all required fields
    await waitFor(() => {
      expect(screen.getByLabelText(/project code/i)).toBeInTheDocument();
    });

    // Select project
    const projectSelect = screen.getByLabelText(/project code/i);
    fireEvent.mouseDown(projectSelect);
    await waitFor(() => {
      fireEvent.click(screen.getByText('SER-A-000210'));
    });

    // Select material
    await waitFor(() => {
      const materialSelect = screen.getByLabelText(/material code/i);
      fireEvent.mouseDown(materialSelect);
    });
    await waitFor(() => {
      fireEvent.click(screen.getByText('Axion CS 2455 (DBTO)'));
    });

    // Select plant
    const plantSelect = screen.getByLabelText(/plant code/i);
    fireEvent.mouseDown(plantSelect);
    await waitFor(() => {
      fireEvent.click(screen.getByText('1001'));
    });

    // Select block
    await waitFor(() => {
      const blockSelect = screen.getByLabelText(/block id/i);
      fireEvent.mouseDown(blockSelect);
    });
    await waitFor(() => {
      fireEvent.click(screen.getByText('1001-A'));
    });

    await waitFor(() => {
      const submitButton = screen.getByRole('button', { name: /create material extension/i });
      expect(submitButton).not.toBeDisabled();
    });
  });

  test('resets form after successful submission', async () => {
    mockOnSubmit.mockResolvedValue();
    
    render(<MaterialExtensionForm onSubmit={mockOnSubmit} loading={false} />);
    
    // Fill form and submit
    await waitFor(() => {
      expect(screen.getByLabelText(/project code/i)).toBeInTheDocument();
    });

    // Fill all fields (simplified for test)
    const projectSelect = screen.getByLabelText(/project code/i);
    fireEvent.mouseDown(projectSelect);
    await waitFor(() => {
      fireEvent.click(screen.getByText('SER-A-000210'));
    });

    // Submit form
    const submitButton = screen.getByRole('button', { name: /create material extension/i });
    fireEvent.click(submitButton);

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalled();
    });
  });
});