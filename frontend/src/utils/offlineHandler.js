import { message, notification } from 'antd';
import { WifiOutlined, DisconnectOutlined } from '@ant-design/icons';

// Offline state management
class OfflineManager {
  constructor() {
    this.isOnline = navigator.onLine;
    this.requestQueue = [];
    this.listeners = [];
    this.maxQueueSize = 100;
    this.syncInProgress = false;
    
    this.init();
  }
  
  init() {
    // Listen for online/offline events
    window.addEventListener('online', this.handleOnline.bind(this));
    window.addEventListener('offline', this.handleOffline.bind(this));
    
    // Periodic connectivity check
    this.startConnectivityCheck();
  }
  
  startConnectivityCheck() {
    setInterval(() => {
      this.checkConnectivity();
    }, 30000); // Check every 30 seconds
  }
  
  async checkConnectivity() {
    try {
      // Try to fetch a small resource to verify connectivity
      const response = await fetch('/qrmfg/api/v1/health', {
        method: 'HEAD',
        cache: 'no-cache',
        timeout: 5000
      });
      
      const wasOnline = this.isOnline;
      this.isOnline = response.ok;
      
      if (!wasOnline && this.isOnline) {
        this.handleOnline();
      } else if (wasOnline && !this.isOnline) {
        this.handleOffline();
      }
    } catch (error) {
      if (this.isOnline) {
        this.isOnline = false;
        this.handleOffline();
      }
    }
  }
  
  handleOnline() {
    console.log('Connection restored');
    this.isOnline = true;
    
    notification.success({
      message: 'Connection Restored',
      description: 'You are back online. Syncing queued requests...',
      icon: <WifiOutlined style={{ color: '#52c41a' }} />,
      duration: 3
    });
    
    this.notifyListeners('online');
    this.syncQueuedRequests();
  }
  
  handleOffline() {
    console.log('Connection lost');
    this.isOnline = false;
    
    notification.warning({
      message: 'Connection Lost',
      description: 'You are offline. Requests will be queued and synced when connection is restored.',
      icon: <DisconnectOutlined style={{ color: '#faad14' }} />,
      duration: 0, // Don't auto-close
      key: 'offline-notification'
    });
    
    this.notifyListeners('offline');
  }
  
  addListener(callback) {
    this.listeners.push(callback);
    return () => {
      this.listeners = this.listeners.filter(listener => listener !== callback);
    };
  }
  
  notifyListeners(event) {
    this.listeners.forEach(callback => {
      try {
        callback(event, this.isOnline);
      } catch (error) {
        console.error('Error in offline listener:', error);
      }
    });
  }
  
  queueRequest(requestConfig) {
    if (this.requestQueue.length >= this.maxQueueSize) {
      // Remove oldest request to make room
      this.requestQueue.shift();
      console.warn('Request queue full, removing oldest request');
    }
    
    const queuedRequest = {
      id: Date.now() + Math.random(),
      timestamp: new Date(),
      config: requestConfig,
      retries: 0,
      maxRetries: 3
    };
    
    this.requestQueue.push(queuedRequest);
    
    message.info(`Request queued (${this.requestQueue.length} pending)`);
    
    return queuedRequest.id;
  }
  
  async syncQueuedRequests() {
    if (this.syncInProgress || this.requestQueue.length === 0) {
      return;
    }
    
    this.syncInProgress = true;
    
    try {
      const totalRequests = this.requestQueue.length;
      let successCount = 0;
      let failureCount = 0;
      
      // Process requests in order
      while (this.requestQueue.length > 0) {
        const request = this.requestQueue.shift();
        
        try {
          await this.executeQueuedRequest(request);
          successCount++;
        } catch (error) {
          console.error('Failed to sync queued request:', error);
          
          // Retry logic
          if (request.retries < request.maxRetries) {
            request.retries++;
            this.requestQueue.push(request); // Re-queue for retry
          } else {
            failureCount++;
          }
        }
        
        // Small delay between requests to avoid overwhelming the server
        await new Promise(resolve => setTimeout(resolve, 100));
      }
      
      // Show sync results
      if (successCount > 0) {
        notification.success({
          message: 'Sync Complete',
          description: `${successCount} requests synced successfully${failureCount > 0 ? `, ${failureCount} failed` : ''}`,
          duration: 4
        });
      }
      
      if (failureCount > 0) {
        notification.error({
          message: 'Sync Issues',
          description: `${failureCount} requests could not be synced. Please try again manually.`,
          duration: 0
        });
      }
      
    } finally {
      this.syncInProgress = false;
      notification.close('offline-notification');
    }
  }
  
  async executeQueuedRequest(queuedRequest) {
    const { config } = queuedRequest;
    
    // Reconstruct the request based on the stored configuration
    const response = await fetch(config.url, {
      method: config.method || 'GET',
      headers: config.headers || {},
      body: config.body ? JSON.stringify(config.body) : undefined
    });
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    
    return response.json();
  }
  
  getQueueStatus() {
    return {
      isOnline: this.isOnline,
      queueLength: this.requestQueue.length,
      syncInProgress: this.syncInProgress
    };
  }
  
  clearQueue() {
    this.requestQueue = [];
    message.success('Request queue cleared');
  }
}

// Global offline manager instance
export const offlineManager = new OfflineManager();

// Hook for React components to use offline state
export const useOfflineState = () => {
  const [isOnline, setIsOnline] = React.useState(offlineManager.isOnline);
  const [queueLength, setQueueLength] = React.useState(offlineManager.requestQueue.length);
  
  React.useEffect(() => {
    const unsubscribe = offlineManager.addListener((event, online) => {
      setIsOnline(online);
      setQueueLength(offlineManager.requestQueue.length);
    });
    
    return unsubscribe;
  }, []);
  
  return {
    isOnline,
    queueLength,
    queueStatus: offlineManager.getQueueStatus(),
    clearQueue: () => offlineManager.clearQueue(),
    syncQueue: () => offlineManager.syncQueuedRequests()
  };
};

// Enhanced fetch wrapper that handles offline scenarios
export const offlineAwareFetch = async (url, options = {}) => {
  const {
    queueWhenOffline = true,
    priority = 'normal',
    ...fetchOptions
  } = options;
  
  // If online, make the request normally
  if (offlineManager.isOnline) {
    try {
      const response = await fetch(url, fetchOptions);
      return response;
    } catch (error) {
      // If request fails and we're supposed to be online, check connectivity
      await offlineManager.checkConnectivity();
      throw error;
    }
  }
  
  // If offline and queuing is enabled
  if (queueWhenOffline) {
    const requestConfig = {
      url,
      method: fetchOptions.method || 'GET',
      headers: fetchOptions.headers,
      body: fetchOptions.body,
      priority
    };
    
    const queueId = offlineManager.queueRequest(requestConfig);
    
    // Return a promise that resolves when the request is eventually processed
    return new Promise((resolve, reject) => {
      const checkQueue = () => {
        const request = offlineManager.requestQueue.find(req => req.id === queueId);
        if (!request) {
          // Request has been processed
          resolve({ queued: true, queueId });
        } else {
          // Still in queue, check again later
          setTimeout(checkQueue, 1000);
        }
      };
      
      checkQueue();
    });
  }
  
  // If offline and not queuing, throw error
  throw new Error('No internet connection and request queuing is disabled');
};

// Axios interceptor for offline handling
export const setupOfflineInterceptors = (axiosInstance) => {
  // Request interceptor
  axiosInstance.interceptors.request.use(
    (config) => {
      // Add offline indicator to requests
      config.metadata = { 
        ...config.metadata, 
        isOnline: offlineManager.isOnline,
        timestamp: Date.now()
      };
      
      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );
  
  // Response interceptor
  axiosInstance.interceptors.response.use(
    (response) => {
      return response;
    },
    async (error) => {
      const { config } = error;
      
      // Check if this is a network error
      if (!error.response && config && !config._retry) {
        // Mark as retry to prevent infinite loops
        config._retry = true;
        
        // Check connectivity
        await offlineManager.checkConnectivity();
        
        // If we're offline and the request should be queued
        if (!offlineManager.isOnline && config.queueWhenOffline !== false) {
          const requestConfig = {
            url: config.url,
            method: config.method,
            headers: config.headers,
            body: config.data,
            priority: config.priority || 'normal'
          };
          
          offlineManager.queueRequest(requestConfig);
          
          // Return a special response indicating the request was queued
          return Promise.resolve({
            data: { queued: true, message: 'Request queued for when connection is restored' },
            status: 202,
            statusText: 'Queued',
            config
          });
        }
      }
      
      return Promise.reject(error);
    }
  );
};

// Component to show offline status
export const OfflineIndicator = ({ style = {} }) => {
  const { isOnline, queueLength } = useOfflineState();
  
  if (isOnline) {
    return null;
  }
  
  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      backgroundColor: '#faad14',
      color: 'white',
      padding: '8px 16px',
      textAlign: 'center',
      zIndex: 9999,
      ...style
    }}>
      <DisconnectOutlined style={{ marginRight: 8 }} />
      You are offline
      {queueLength > 0 && ` • ${queueLength} requests queued`}
    </div>
  );
};

export default {
  offlineManager,
  useOfflineState,
  offlineAwareFetch,
  setupOfflineInterceptors,
  OfflineIndicator
};