# Workflow UI Components

This directory contains the React components for the MSDS Workflow Management system, implementing task 7 from the specification.

## Components Implemented

### 1. WorkflowDashboard
- **Location**: `WorkflowDashboard.js`
- **Purpose**: Displays workflow statistics and pending tasks panel
- **Features**:
  - Responsive statistics grid with workflow counts
  - Overdue workflows table with priority indicators
  - Recent activity tracking
  - Mobile-optimized layout with responsive columns
  - Touch-friendly interface for tablet/mobile devices

### 2. QueryWidget
- **Location**: `QueryWidget.js`
- **Purpose**: Tabbed interface for query management
- **Features**:
  - Four tabs: All Queries, Open, Resolved, My Queries
  - Query creation modal for plant users
  - Query resolution interface for CQS/Tech teams
  - Role-based access control for actions
  - Responsive table design with mobile-optimized columns
  - Badge counts for each tab

### 3. AuditTimeline
- **Location**: `AuditTimeline.js`
- **Purpose**: Displays workflow history and audit trail
- **Features**:
  - Timeline visualization of workflow events
  - Expandable details for each audit entry
  - State change tracking with visual indicators
  - Mobile-responsive timeline layout
  - Export functionality for audit logs

## Responsive Design Features

### Mobile Support (≤768px)
- Responsive grid layouts using Ant Design's grid system
- Horizontal scrolling for tables with fixed columns
- Compressed text and smaller components
- Touch-friendly button sizes (min 44px height)
- Simplified navigation and reduced information density

### Tablet Support (769px-1024px)
- Optimized layouts for medium screens
- Balanced information density
- Touch-optimized interactions
- Responsive column arrangements

### CSS Enhancements
- Added responsive CSS classes in `App.css`
- Touch-friendly styles for mobile devices
- Responsive grid utilities
- Mobile-first design approach

## API Integration

### Services Used
- `workflowAPI.js` - Dashboard data and workflow operations
- `queryAPI.js` - Query management and resolution
- `auditAPI.js` - Audit trail and history tracking

### Error Handling
- Graceful error states with retry functionality
- Loading states during API calls
- User-friendly error messages
- Fallback data for offline scenarios

## Usage

The components are integrated into the main application through `WorkflowPage.js`:

```javascript
import WorkflowDashboard from '../components/workflow/WorkflowDashboard';
import QueryWidget from '../components/workflow/QueryWidget';
import AuditTimeline from '../components/workflow/AuditTimeline';
```

## Requirements Fulfilled

✅ **4.1** - Pending tasks panel with material progress tracking
✅ **4.2** - Query widget with tabbed interface for different views
✅ **4.3** - Visual pipeline showing workflow stages
✅ **7.1** - Complete audit timeline with event tracking
✅ **8.1** - Responsive design for mobile and tablet access

## Technical Implementation

### Responsive Hook
Custom `useResponsive` hook detects screen size changes:
- `isMobile`: ≤768px
- `isTablet`: 769px-1024px
- `isDesktop`: >1024px

### Responsive Columns
Dynamic column configuration based on screen size:
- Mobile: Essential columns only, horizontal scroll
- Tablet: Balanced column visibility
- Desktop: Full column set

### Touch Optimization
- Minimum touch target sizes
- Optimized spacing for finger navigation
- Simplified interactions on mobile devices

## Testing

Test file: `WorkflowComponents.test.js`
- Component rendering tests
- Responsive behavior validation
- Role-based access control testing
- API integration testing

Note: Tests require proper mocking of `window.matchMedia` and API responses for full functionality.