import { notification, message } from 'antd';

// Error types mapping
const ERROR_TYPES = {
  INVALID_WORKFLOW_STATE: 'Invalid Workflow State',
  UNAUTHORIZED_WORKFLOW_ACTION: 'Unauthorized Action',
  WORKFLOW_NOT_FOUND: 'Workflow Not Found',
  QUERY_NOT_FOUND: 'Query Not Found',
  INVALID_QUERY_STATE: 'Invalid Query State',
  VALIDATION_ERROR: 'Validation Error',
  BINDING_ERROR: 'Form Error',
  CONSTRAINT_VIOLATION: 'Data Constraint Error',
  ACCESS_DENIED: 'Access Denied',
  ENTITY_NOT_FOUND: 'Resource Not Found',
  WORKFLOW_ERROR: 'Workflow Error',
  QUERY_ERROR: 'Query Error',
  QUERY_ALREADY_RESOLVED: 'Query Already Resolved',
  INTERNAL_SERVER_ERROR: 'Server Error',
  NETWORK_ERROR: 'Network Error',
  TIMEOUT_ERROR: 'Request Timeout'
};

// Error severity levels
const ERROR_SEVERITY = {
  LOW: 'info',
  MEDIUM: 'warning',
  HIGH: 'error'
};

// Get error severity based on error type
const getErrorSeverity = (errorType) => {
  const highSeverityErrors = [
    'INTERNAL_SERVER_ERROR',
    'WORKFLOW_ERROR',
    'NETWORK_ERROR',
    'TIMEOUT_ERROR'
  ];
  
  const mediumSeverityErrors = [
    'UNAUTHORIZED_WORKFLOW_ACTION',
    'ACCESS_DENIED',
    'INVALID_WORKFLOW_STATE',
    'INVALID_QUERY_STATE'
  ];
  
  if (highSeverityErrors.includes(errorType)) {
    return ERROR_SEVERITY.HIGH;
  } else if (mediumSeverityErrors.includes(errorType)) {
    return ERROR_SEVERITY.MEDIUM;
  }
  
  return ERROR_SEVERITY.LOW;
};

// Format field errors for display
const formatFieldErrors = (fieldErrors) => {
  if (!fieldErrors || Object.keys(fieldErrors).length === 0) {
    return null;
  }
  
  return Object.entries(fieldErrors).map(([field, error]) => ({
    field: field.charAt(0).toUpperCase() + field.slice(1).replace(/([A-Z])/g, ' $1'),
    message: error
  }));
};

// Main error handler
export const handleError = (error, options = {}) => {
  const {
    showNotification = true,
    showMessage = false,
    customMessage = null,
    onError = null,
    context = 'general'
  } = options;
  
  let errorInfo = {
    type: 'UNKNOWN_ERROR',
    message: 'An unexpected error occurred',
    fieldErrors: null,
    severity: ERROR_SEVERITY.HIGH,
    status: 500
  };
  
  // Handle different error types
  if (error?.response) {
    // HTTP error response
    const { data, status } = error.response;
    
    errorInfo = {
      type: data?.error || 'HTTP_ERROR',
      message: data?.message || `HTTP ${status} Error`,
      fieldErrors: data?.fieldErrors || null,
      severity: getErrorSeverity(data?.error),
      status: status,
      path: data?.path
    };
  } else if (error?.request) {
    // Network error
    errorInfo = {
      type: 'NETWORK_ERROR',
      message: 'Unable to connect to server. Please check your internet connection.',
      severity: ERROR_SEVERITY.HIGH,
      status: 0
    };
  } else if (error?.code === 'ECONNABORTED') {
    // Timeout error
    errorInfo = {
      type: 'TIMEOUT_ERROR',
      message: 'Request timed out. Please try again.',
      severity: ERROR_SEVERITY.HIGH,
      status: 408
    };
  } else if (error?.message) {
    // Generic error with message
    errorInfo = {
      type: 'CLIENT_ERROR',
      message: error.message,
      severity: ERROR_SEVERITY.MEDIUM,
      status: 400
    };
  }
  
  // Use custom message if provided
  if (customMessage) {
    errorInfo.message = customMessage;
  }
  
  // Log error for debugging
  console.error(`[${context.toUpperCase()}] Error:`, {
    type: errorInfo.type,
    message: errorInfo.message,
    status: errorInfo.status,
    fieldErrors: errorInfo.fieldErrors,
    originalError: error
  });
  
  // Show notification
  if (showNotification) {
    const notificationConfig = {
      message: ERROR_TYPES[errorInfo.type] || 'Error',
      description: errorInfo.message,
      duration: errorInfo.severity === ERROR_SEVERITY.HIGH ? 0 : 4.5,
      placement: 'topRight'
    };
    
    // Add field errors to description if present
    if (errorInfo.fieldErrors) {
      const formattedErrors = formatFieldErrors(errorInfo.fieldErrors);
      notificationConfig.description = (
        <div>
          <div>{errorInfo.message}</div>
          <div style={{ marginTop: 8 }}>
            <strong>Field Errors:</strong>
            <ul style={{ margin: '4px 0 0 16px' }}>
              {formattedErrors.map((fieldError, index) => (
                <li key={index}>
                  <strong>{fieldError.field}:</strong> {fieldError.message}
                </li>
              ))}
            </ul>
          </div>
        </div>
      );
    }
    
    notification[errorInfo.severity](notificationConfig);
  }
  
  // Show simple message
  if (showMessage) {
    message[errorInfo.severity](errorInfo.message);
  }
  
  // Call custom error handler
  if (onError && typeof onError === 'function') {
    onError(errorInfo);
  }
  
  return errorInfo;
};

// Specific error handlers for different contexts
export const handleWorkflowError = (error, options = {}) => {
  return handleError(error, { ...options, context: 'workflow' });
};

export const handleQueryError = (error, options = {}) => {
  return handleError(error, { ...options, context: 'query' });
};

export const handleFormError = (error, form = null, options = {}) => {
  const errorInfo = handleError(error, { ...options, context: 'form' });
  
  // Set form field errors if form instance is provided
  if (form && errorInfo.fieldErrors) {
    const formFields = {};
    Object.entries(errorInfo.fieldErrors).forEach(([field, message]) => {
      formFields[field] = {
        errors: [new Error(message)]
      };
    });
    form.setFields(Object.entries(formFields).map(([name, error]) => ({
      name,
      ...error
    })));
  }
  
  return errorInfo;
};

export const handleApiError = (error, options = {}) => {
  return handleError(error, { ...options, context: 'api' });
};

// Validation helpers
export const validateRequired = (value, fieldName) => {
  if (!value || (typeof value === 'string' && value.trim() === '')) {
    throw new Error(`${fieldName} is required`);
  }
};

export const validateMaxLength = (value, maxLength, fieldName) => {
  if (value && value.length > maxLength) {
    throw new Error(`${fieldName} must not exceed ${maxLength} characters`);
  }
};

export const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (email && !emailRegex.test(email)) {
    throw new Error('Please enter a valid email address');
  }
};

// Form validation rules
export const getValidationRules = (fieldType, options = {}) => {
  const { required = false, maxLength, minLength, pattern } = options;
  
  const rules = [];
  
  if (required) {
    rules.push({
      required: true,
      message: `${fieldType} is required`
    });
  }
  
  if (maxLength) {
    rules.push({
      max: maxLength,
      message: `${fieldType} must not exceed ${maxLength} characters`
    });
  }
  
  if (minLength) {
    rules.push({
      min: minLength,
      message: `${fieldType} must be at least ${minLength} characters`
    });
  }
  
  if (pattern) {
    rules.push({
      pattern: pattern,
      message: `${fieldType} format is invalid`
    });
  }
  
  return rules;
};

export default {
  handleError,
  handleWorkflowError,
  handleQueryError,
  handleFormError,
  handleApiError,
  validateRequired,
  validateMaxLength,
  validateEmail,
  getValidationRules,
  ERROR_TYPES,
  ERROR_SEVERITY
};