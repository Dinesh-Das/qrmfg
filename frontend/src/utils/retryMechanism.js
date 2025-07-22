import { message } from 'antd';

// Default retry configuration
const DEFAULT_RETRY_CONFIG = {
  maxRetries: 3,
  baseDelay: 1000, // 1 second
  maxDelay: 10000, // 10 seconds
  backoffFactor: 2,
  retryCondition: (error) => {
    // Retry on network errors, timeouts, and 5xx server errors
    if (!error.response) return true; // Network error
    if (error.code === 'ECONNABORTED') return true; // Timeout
    if (error.response.status >= 500) return true; // Server error
    return false;
  }
};

// Calculate delay with exponential backoff
const calculateDelay = (attempt, baseDelay, backoffFactor, maxDelay) => {
  const delay = baseDelay * Math.pow(backoffFactor, attempt - 1);
  return Math.min(delay, maxDelay);
};

// Sleep utility
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// Main retry function
export const retryRequest = async (requestFn, config = {}) => {
  const {
    maxRetries,
    baseDelay,
    maxDelay,
    backoffFactor,
    retryCondition,
    onRetry,
    onMaxRetriesReached
  } = { ...DEFAULT_RETRY_CONFIG, ...config };
  
  let lastError;
  
  for (let attempt = 1; attempt <= maxRetries + 1; attempt++) {
    try {
      const result = await requestFn();
      
      // Success - reset any retry indicators
      if (attempt > 1) {
        message.success('Connection restored');
      }
      
      return result;
    } catch (error) {
      lastError = error;
      
      // Check if we should retry
      if (attempt <= maxRetries && retryCondition(error)) {
        const delay = calculateDelay(attempt, baseDelay, backoffFactor, maxDelay);
        
        console.warn(`Request failed (attempt ${attempt}/${maxRetries + 1}). Retrying in ${delay}ms...`, error);
        
        // Call retry callback if provided
        if (onRetry) {
          onRetry(error, attempt, delay);
        }
        
        // Show retry message for user feedback
        if (attempt === 1) {
          message.loading(`Connection issue detected. Retrying... (${attempt}/${maxRetries})`, 2);
        } else {
          message.loading(`Retrying... (${attempt}/${maxRetries})`, 2);
        }
        
        await sleep(delay);
      } else {
        // No more retries or error not retryable
        break;
      }
    }
  }
  
  // All retries exhausted
  if (onMaxRetriesReached) {
    onMaxRetriesReached(lastError);
  }
  
  throw lastError;
};

// Specific retry configurations for different types of requests
export const retryConfigs = {
  // Critical operations - more aggressive retry
  critical: {
    maxRetries: 5,
    baseDelay: 500,
    maxDelay: 5000,
    backoffFactor: 1.5
  },
  
  // Standard operations
  standard: {
    maxRetries: 3,
    baseDelay: 1000,
    maxDelay: 10000,
    backoffFactor: 2
  },
  
  // Background operations - less aggressive
  background: {
    maxRetries: 2,
    baseDelay: 2000,
    maxDelay: 15000,
    backoffFactor: 2.5
  },
  
  // Quick operations - fast retry
  quick: {
    maxRetries: 2,
    baseDelay: 300,
    maxDelay: 2000,
    backoffFactor: 2
  }
};

// Wrapper for API calls with retry
export const withRetry = (apiCall, retryType = 'standard') => {
  return (...args) => {
    return retryRequest(
      () => apiCall(...args),
      retryConfigs[retryType]
    );
  };
};

// Batch retry for multiple requests
export const retryBatch = async (requests, config = {}) => {
  const {
    concurrency = 3,
    failFast = false,
    ...retryConfig
  } = config;
  
  const results = [];
  const errors = [];
  
  // Process requests in batches
  for (let i = 0; i < requests.length; i += concurrency) {
    const batch = requests.slice(i, i + concurrency);
    
    const batchPromises = batch.map(async (request, index) => {
      try {
        const result = await retryRequest(request, retryConfig);
        return { success: true, result, index: i + index };
      } catch (error) {
        const errorInfo = { success: false, error, index: i + index };
        
        if (failFast) {
          throw errorInfo;
        }
        
        return errorInfo;
      }
    });
    
    const batchResults = await Promise.all(batchPromises);
    
    batchResults.forEach(result => {
      if (result.success) {
        results[result.index] = result.result;
      } else {
        errors[result.index] = result.error;
      }
    });
  }
  
  return { results, errors };
};

// Circuit breaker pattern for preventing cascade failures
class CircuitBreaker {
  constructor(config = {}) {
    this.failureThreshold = config.failureThreshold || 5;
    this.resetTimeout = config.resetTimeout || 30000; // 30 seconds
    this.monitoringPeriod = config.monitoringPeriod || 60000; // 1 minute
    
    this.state = 'CLOSED'; // CLOSED, OPEN, HALF_OPEN
    this.failureCount = 0;
    this.lastFailureTime = null;
    this.successCount = 0;
  }
  
  async execute(requestFn) {
    if (this.state === 'OPEN') {
      if (Date.now() - this.lastFailureTime >= this.resetTimeout) {
        this.state = 'HALF_OPEN';
        this.successCount = 0;
      } else {
        throw new Error('Circuit breaker is OPEN - service temporarily unavailable');
      }
    }
    
    try {
      const result = await requestFn();
      
      if (this.state === 'HALF_OPEN') {
        this.successCount++;
        if (this.successCount >= 3) {
          this.reset();
        }
      } else {
        this.reset();
      }
      
      return result;
    } catch (error) {
      this.recordFailure();
      throw error;
    }
  }
  
  recordFailure() {
    this.failureCount++;
    this.lastFailureTime = Date.now();
    
    if (this.failureCount >= this.failureThreshold) {
      this.state = 'OPEN';
      console.warn('Circuit breaker opened due to repeated failures');
      message.warning('Service temporarily unavailable. Please try again later.');
    }
  }
  
  reset() {
    this.state = 'CLOSED';
    this.failureCount = 0;
    this.lastFailureTime = null;
    this.successCount = 0;
  }
  
  getState() {
    return {
      state: this.state,
      failureCount: this.failureCount,
      lastFailureTime: this.lastFailureTime
    };
  }
}

// Global circuit breaker instances for different services
export const circuitBreakers = {
  workflow: new CircuitBreaker({ failureThreshold: 5, resetTimeout: 30000 }),
  query: new CircuitBreaker({ failureThreshold: 3, resetTimeout: 20000 }),
  notification: new CircuitBreaker({ failureThreshold: 10, resetTimeout: 60000 })
};

// Wrapper to use circuit breaker with retry
export const withCircuitBreaker = (apiCall, breakerName = 'workflow') => {
  const breaker = circuitBreakers[breakerName];
  
  return (...args) => {
    return breaker.execute(() => apiCall(...args));
  };
};

export default {
  retryRequest,
  withRetry,
  retryBatch,
  retryConfigs,
  CircuitBreaker,
  circuitBreakers,
  withCircuitBreaker
};