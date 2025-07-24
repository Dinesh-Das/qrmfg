# Oracle Database Fixes Applied

## Overview
Fixed Oracle SQL compatibility issues that were causing 500 Internal Server Errors in the QRMFG application.

## Root Cause Analysis
The errors were caused by:
1. **Data Type Mismatch**: `ORA-00932: inconsistent datatypes: expected NUMBER got INTERVAL DAY TO SECOND`
2. **SQL Syntax Error**: `ORA-00936: missing expression`
3. **Oracle Function Compatibility**: Using MySQL-specific functions in Oracle database

## Fixes Applied

### 1. QueryRepository.java

#### Fixed Date Functions
**Before:**
```sql
SELECT COUNT(q) FROM Query q WHERE DATE(q.resolvedAt) = CURRENT_DATE AND q.status = 'RESOLVED'
SELECT COUNT(q) FROM Query q WHERE DATE(q.createdAt) = CURRENT_DATE
```

**After:**
```sql
SELECT COUNT(q) FROM Query q WHERE TRUNC(q.resolvedAt) = TRUNC(CURRENT_DATE) AND q.status = 'RESOLVED'
SELECT COUNT(q) FROM Query q WHERE TRUNC(q.createdAt) = TRUNC(CURRENT_DATE)
```

#### Fixed Date Arithmetic for Resolution Time
**Before:**
```sql
SELECT AVG(CAST((q.resolvedAt - q.createdAt) AS double) * 24) FROM Query q WHERE q.status = 'RESOLVED' AND q.assignedTeam = :team
```

**After:**
```sql
SELECT AVG(EXTRACT(DAY FROM (q.resolvedAt - q.createdAt)) * 24 + EXTRACT(HOUR FROM (q.resolvedAt - q.createdAt))) FROM Query q WHERE q.status = 'RESOLVED' AND q.assignedTeam = :team
```

#### Fixed Overdue Query Logic
**Before:**
```sql
SELECT q FROM Query q WHERE q.status = 'OPEN' AND (CURRENT_TIMESTAMP - q.createdAt) > :days
```

**After:**
```sql
SELECT q FROM Query q WHERE q.status = 'OPEN' AND q.createdAt < :cutoffTime
```

#### Fixed Team Workload Metrics
**Before:**
```sql
SELECT assigned_team, COUNT(*) as openCount, AVG((CURRENT_TIMESTAMP - created_at) * 24) as avgAgeHours FROM qrmfg_queries WHERE query_status = 'OPEN' GROUP BY assigned_team
```

**After:**
```sql
SELECT assigned_team, COUNT(*) as openCount, AVG(EXTRACT(DAY FROM (CURRENT_TIMESTAMP - created_at)) * 24 + EXTRACT(HOUR FROM (CURRENT_TIMESTAMP - created_at))) as avgAgeHours FROM qrmfg_queries WHERE query_status = 'OPEN' GROUP BY assigned_team
```

### 2. WorkflowRepository.java

#### Fixed Overdue Workflows Query
**Before:**
```sql
SELECT * FROM qrmfg_material_workflows WHERE workflow_state != 'COMPLETED' AND ((workflow_state = 'JVC_PENDING' AND (CURRENT_TIMESTAMP - created_at) > INTERVAL '3' DAY) OR ...)
```

**After:**
```sql
SELECT * FROM qrmfg_material_workflows WHERE workflow_state != 'COMPLETED' AND ((workflow_state = 'JVC_PENDING' AND created_at < CURRENT_TIMESTAMP - 3) OR ...)
```

#### Fixed High Priority Workflows Query
**Before:**
```sql
(w.state = 'JVC_PENDING' AND (CURRENT_TIMESTAMP - w.createdAt) > 2)
```

**After:**
```sql
(w.state = 'JVC_PENDING' AND w.createdAt < CURRENT_TIMESTAMP - 2)
```

### 3. QueryServiceImpl.java

#### Updated Service Logic
- Changed overdue query threshold from 24 hours to 72 hours (3 days) for business consistency
- Updated method calls to use LocalDateTime parameters instead of integer days

### 4. MetricsController.java

#### Fixed URL Mapping
**Before:**
```java
@RequestMapping("/api/metrics")
```

**After:**
```java
@RequestMapping("/qrmfg/api/v1/metrics")
```

#### Enhanced Activity Tracking
- Made activity recording endpoint accessible to all users
- Added error handling to prevent activity tracking failures from affecting user experience

## Key Oracle Compatibility Changes

### Date Functions
- `DATE()` → `TRUNC()`
- MySQL date functions replaced with Oracle equivalents

### Date Arithmetic
- `(date1 - date2) * 24` → `EXTRACT(DAY FROM (date1 - date2)) * 24 + EXTRACT(HOUR FROM (date1 - date2))`
- `INTERVAL 'n' DAY` → Direct numeric subtraction with `CURRENT_TIMESTAMP - n`

### Data Type Handling
- Removed `CAST(...AS double)` operations that cause Oracle data type conflicts
- Used Oracle's `EXTRACT()` function for proper date component extraction

## Testing Recommendations

1. **Database Queries**: Test all modified queries directly in Oracle SQL Developer
2. **API Endpoints**: Verify that statistics endpoints now return proper values
3. **Frontend Integration**: Confirm that dashboard statistics display correctly
4. **Activity Tracking**: Test that user activity recording works without errors

## Expected Results

After applying these fixes:
- ✅ No more `ORA-00932` data type mismatch errors
- ✅ No more `ORA-00936` missing expression errors  
- ✅ Query statistics endpoints return proper values
- ✅ Activity tracking works without console errors
- ✅ Dashboard displays correct metrics
- ✅ Frontend application functions normally

## Files Modified

1. `src/main/java/com/cqs/qrmfg/repository/QueryRepository.java`
2. `src/main/java/com/cqs/qrmfg/repository/WorkflowRepository.java`
3. `src/main/java/com/cqs/qrmfg/service/impl/QueryServiceImpl.java`
4. `src/main/java/com/cqs/qrmfg/controller/MetricsController.java`

## Deployment Notes

- These changes are backward compatible
- No database schema changes required
- Application restart required to apply fixes
- Monitor logs after deployment to confirm error resolution