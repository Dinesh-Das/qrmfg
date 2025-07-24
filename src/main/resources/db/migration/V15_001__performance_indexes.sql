-- Performance optimization indexes for MSDS Workflow system
-- This migration adds indexes to improve dashboard query performance

-- Workflow table indexes for dashboard queries
CREATE INDEX IF NOT EXISTS idx_workflow_state ON qrmfg_material_workflows(workflow_state);
CREATE INDEX IF NOT EXISTS idx_workflow_created_at ON qrmfg_material_workflows(created_at);
CREATE INDEX IF NOT EXISTS idx_workflow_plant_state ON qrmfg_material_workflows(plant_code, workflow_state);
CREATE INDEX IF NOT EXISTS idx_workflow_project_material ON qrmfg_material_workflows(project_code, material_code);
CREATE INDEX IF NOT EXISTS idx_workflow_initiated_by ON qrmfg_material_workflows(initiated_by);
CREATE INDEX IF NOT EXISTS idx_workflow_last_modified ON qrmfg_material_workflows(last_modified);

-- Composite indexes for common dashboard queries
CREATE INDEX IF NOT EXISTS idx_workflow_state_created ON qrmfg_material_workflows(workflow_state, created_at);
CREATE INDEX IF NOT EXISTS idx_workflow_plant_created ON qrmfg_material_workflows(plant_code, created_at);
CREATE INDEX IF NOT EXISTS idx_workflow_project_state ON qrmfg_material_workflows(project_code, workflow_state);

-- Query table indexes
CREATE INDEX IF NOT EXISTS idx_query_workflow_id ON qrmfg_queries(workflow_id);
CREATE INDEX IF NOT EXISTS idx_query_status ON qrmfg_queries(status);
CREATE INDEX IF NOT EXISTS idx_query_assigned_team ON qrmfg_queries(assigned_team);
CREATE INDEX IF NOT EXISTS idx_query_created_at ON qrmfg_queries(created_at);
CREATE INDEX IF NOT EXISTS idx_query_resolved_at ON qrmfg_queries(resolved_at);

-- Composite indexes for query dashboard
CREATE INDEX IF NOT EXISTS idx_query_team_status ON qrmfg_queries(assigned_team, status);
CREATE INDEX IF NOT EXISTS idx_query_workflow_status ON qrmfg_queries(workflow_id, status);

-- Document table indexes
CREATE INDEX IF NOT EXISTS idx_document_workflow_id ON qrmfg_workflow_documents(workflow_id);
CREATE INDEX IF NOT EXISTS idx_document_uploaded_at ON qrmfg_workflow_documents(uploaded_at);
CREATE INDEX IF NOT EXISTS idx_document_file_type ON qrmfg_workflow_documents(file_type);
CREATE INDEX IF NOT EXISTS idx_document_uploaded_by ON qrmfg_workflow_documents(uploaded_by);

-- Reference table indexes for dropdown performance
CREATE INDEX IF NOT EXISTS idx_fsobjectref_object_type ON fsobjectreference(object_type);
CREATE INDEX IF NOT EXISTS idx_fsobjectref_object_key ON fsobjectreference(object_key);
CREATE INDEX IF NOT EXISTS idx_fsobjectref_ref_code ON fsobjectreference(ref_code);
CREATE INDEX IF NOT EXISTS idx_fslocation_location_code ON fslocation(location_code);

-- Audit table indexes
CREATE INDEX IF NOT EXISTS idx_workflow_aud_rev ON qrmfg_material_workflows_aud(rev);
CREATE INDEX IF NOT EXISTS idx_workflow_aud_id_rev ON qrmfg_material_workflows_aud(id, rev);

-- Performance monitoring indexes
CREATE INDEX IF NOT EXISTS idx_workflow_completion_time ON qrmfg_material_workflows(completed_at) WHERE completed_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_workflow_sla_monitoring ON qrmfg_material_workflows(workflow_state, created_at, last_modified) WHERE workflow_state != 'COMPLETED';

-- Dashboard summary view for optimized queries
CREATE OR REPLACE VIEW v_workflow_dashboard_summary AS
SELECT 
    w.workflow_state,
    w.plant_code,
    w.project_code,
    COUNT(*) as workflow_count,
    AVG((SYSDATE - w.created_at) * 24) as avg_age_hours,
    COUNT(CASE WHEN (SYSDATE - w.created_at) > 3 THEN 1 END) as overdue_count
FROM qrmfg_material_workflows w
WHERE w.workflow_state != 'COMPLETED'
GROUP BY w.workflow_state, w.plant_code, w.project_code;

-- Query performance view
CREATE OR REPLACE VIEW v_query_performance_summary AS
SELECT 
    q.assigned_team,
    q.status,
    COUNT(*) as query_count,
    AVG((CASE WHEN q.resolved_at IS NOT NULL THEN q.resolved_at ELSE SYSDATE END - q.created_at) * 24) as avg_resolution_hours,
    COUNT(CASE WHEN q.status = 'OPEN' AND (SYSDATE - q.created_at) > 3 THEN 1 END) as overdue_count
FROM qrmfg_queries q
GROUP BY q.assigned_team, q.status;

-- Material reuse analytics view
CREATE OR REPLACE VIEW v_material_reuse_analytics AS
SELECT 
    w.project_code,
    w.material_code,
    COUNT(DISTINCT w.id) as workflow_count,
    COUNT(DISTINCT w.plant_code) as plant_count,
    COUNT(DISTINCT wd.id) as document_count,
    MAX(w.created_at) as last_used
FROM qrmfg_material_workflows w
LEFT JOIN qrmfg_workflow_documents wd ON w.id = wd.workflow_id
GROUP BY w.project_code, w.material_code
HAVING COUNT(DISTINCT w.id) > 1;

-- Performance statistics table for monitoring
CREATE TABLE IF NOT EXISTS qrmfg_performance_stats (
    id NUMBER PRIMARY KEY,
    metric_name VARCHAR2(100) NOT NULL,
    metric_value NUMBER,
    metric_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    additional_data CLOB
);

CREATE INDEX IF NOT EXISTS idx_perf_stats_name_time ON qrmfg_performance_stats(metric_name, metric_timestamp);

-- Insert initial performance baseline
INSERT INTO qrmfg_performance_stats (id, metric_name, metric_value, additional_data)
SELECT qrmfg_performance_stats_seq.NEXTVAL, 'baseline_workflow_count', COUNT(*), 'Initial workflow count at performance optimization deployment'
FROM qrmfg_material_workflows;

INSERT INTO qrmfg_performance_stats (id, metric_name, metric_value, additional_data)
SELECT qrmfg_performance_stats_seq.NEXTVAL, 'baseline_query_count', COUNT(*), 'Initial query count at performance optimization deployment'
FROM qrmfg_queries;

-- Create sequence for performance stats
CREATE SEQUENCE IF NOT EXISTS qrmfg_performance_stats_seq START WITH 1 INCREMENT BY 1;

COMMIT;