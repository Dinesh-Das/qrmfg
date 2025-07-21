-- MSDS Workflow Database Schema
-- Oracle Database Migration Script

-- Create sequences for primary keys
CREATE SEQUENCE MATERIAL_WORKFLOW_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE QUERY_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE QUESTIONNAIRE_RESPONSE_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE REVINFO_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE NOTIFICATION_PREF_SEQ START WITH 1 INCREMENT BY 1;

-- Material Workflows table
CREATE TABLE qrmfg_material_workflows (
    id NUMBER PRIMARY KEY,
    material_id VARCHAR2(100) UNIQUE NOT NULL,
    workflow_state VARCHAR2(20) NOT NULL,
    assigned_plant VARCHAR2(100),
    initiated_by VARCHAR2(100) NOT NULL,
    material_name VARCHAR2(200),
    material_description VARCHAR2(1000),
    safety_documents_path VARCHAR2(500),
    priority_level VARCHAR2(20) DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL,
    last_modified TIMESTAMP NOT NULL,
    extended_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50)
);

-- Queries table
CREATE TABLE qrmfg_queries (
    id NUMBER PRIMARY KEY,
    workflow_id NUMBER NOT NULL,
    question VARCHAR2(2000) NOT NULL,
    step_number NUMBER,
    field_name VARCHAR2(100),
    assigned_team VARCHAR2(20) NOT NULL,
    query_status VARCHAR2(20) DEFAULT 'OPEN' NOT NULL,
    response VARCHAR2(2000),
    raised_by VARCHAR2(100) NOT NULL,
    resolved_by VARCHAR2(100),
    priority_level VARCHAR2(20) DEFAULT 'NORMAL',
    query_category VARCHAR2(50),
    created_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    last_modified TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    CONSTRAINT fk_query_workflow FOREIGN KEY (workflow_id) REFERENCES qrmfg_material_workflows(id)
);

-- Questionnaire Responses table
CREATE TABLE qrmfg_questionnaire_responses (
    id NUMBER PRIMARY KEY,
    workflow_id NUMBER NOT NULL,
    step_number NUMBER NOT NULL,
    field_name VARCHAR2(100) NOT NULL,
    field_value VARCHAR2(2000),
    field_type VARCHAR2(50) DEFAULT 'TEXT',
    is_required NUMBER(1) DEFAULT 0,
    validation_status VARCHAR2(20) DEFAULT 'VALID',
    validation_message VARCHAR2(500),
    is_draft NUMBER(1) DEFAULT 0,
    section_name VARCHAR2(100),
    display_order NUMBER,
    last_modified TIMESTAMP NOT NULL,
    modified_by VARCHAR2(100) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    created_by VARCHAR2(50),
    CONSTRAINT fk_response_workflow FOREIGN KEY (workflow_id) REFERENCES qrmfg_material_workflows(id)
);

-- Revision info table for Hibernate Envers
CREATE TABLE qrmfg_revinfo (
    id NUMBER PRIMARY KEY,
    timestamp NUMBER NOT NULL,
    username VARCHAR2(100),
    revision_date TIMESTAMP
);

-- Notification Preferences table
CREATE TABLE qrmfg_notification_preferences (
    id NUMBER PRIMARY KEY,
    username VARCHAR2(100) NOT NULL,
    notification_type VARCHAR2(50) NOT NULL,
    channel VARCHAR2(20) NOT NULL,
    enabled NUMBER(1) DEFAULT 1 NOT NULL,
    email VARCHAR2(255),
    slack_id VARCHAR2(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100)
);

-- Audit tables for Hibernate Envers
CREATE TABLE qrmfg_material_workflows_aud (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    material_id VARCHAR2(100),
    workflow_state VARCHAR2(20),
    assigned_plant VARCHAR2(100),
    initiated_by VARCHAR2(100),
    material_name VARCHAR2(200),
    material_description VARCHAR2(1000),
    safety_documents_path VARCHAR2(500),
    priority_level VARCHAR2(20),
    created_at TIMESTAMP,
    last_modified TIMESTAMP,
    extended_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_workflow_aud_rev FOREIGN KEY (rev) REFERENCES qrmfg_revinfo(id)
);

CREATE TABLE qrmfg_queries_aud (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    workflow_id NUMBER,
    question VARCHAR2(2000),
    step_number NUMBER,
    field_name VARCHAR2(100),
    assigned_team VARCHAR2(20),
    query_status VARCHAR2(20),
    response VARCHAR2(2000),
    raised_by VARCHAR2(100),
    resolved_by VARCHAR2(100),
    priority_level VARCHAR2(20),
    query_category VARCHAR2(50),
    created_at TIMESTAMP,
    resolved_at TIMESTAMP,
    last_modified TIMESTAMP,
    created_by VARCHAR2(50),
    updated_by VARCHAR2(50),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_query_aud_rev FOREIGN KEY (rev) REFERENCES qrmfg_revinfo(id)
);

CREATE TABLE qrmfg_questionnaire_responses_aud (
    id NUMBER,
    rev NUMBER,
    revtype NUMBER,
    workflow_id NUMBER,
    step_number NUMBER,
    field_name VARCHAR2(100),
    field_value VARCHAR2(2000),
    field_type VARCHAR2(50),
    is_required NUMBER(1),
    validation_status VARCHAR2(20),
    validation_message VARCHAR2(500),
    is_draft NUMBER(1),
    section_name VARCHAR2(100),
    display_order NUMBER,
    last_modified TIMESTAMP,
    modified_by VARCHAR2(100),
    created_at TIMESTAMP,
    created_by VARCHAR2(50),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_response_aud_rev FOREIGN KEY (rev) REFERENCES qrmfg_revinfo(id)
);

-- Create indexes for better performance
CREATE INDEX idx_material_workflow_state ON qrmfg_material_workflows(workflow_state);
CREATE INDEX idx_material_workflow_plant ON qrmfg_material_workflows(assigned_plant);
CREATE INDEX idx_material_workflow_created ON qrmfg_material_workflows(created_at);

CREATE INDEX idx_query_workflow ON qrmfg_queries(workflow_id);
CREATE INDEX idx_query_status ON qrmfg_queries(query_status);
CREATE INDEX idx_query_team ON qrmfg_queries(assigned_team);
CREATE INDEX idx_query_created ON qrmfg_queries(created_at);

CREATE INDEX idx_response_workflow ON qrmfg_questionnaire_responses(workflow_id);
CREATE INDEX idx_response_step ON qrmfg_questionnaire_responses(step_number);
CREATE INDEX idx_response_field ON qrmfg_questionnaire_responses(field_name);

CREATE INDEX idx_notification_pref_username ON qrmfg_notification_preferences(username);
CREATE INDEX idx_notification_pref_type ON qrmfg_notification_preferences(notification_type);
CREATE INDEX idx_notification_pref_enabled ON qrmfg_notification_preferences(enabled);

-- Create sequences for audit and user management
CREATE SEQUENCE AUDIT_LOG_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE USER_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE ROLE_SEQ START WITH 1 INCREMENT BY 1;

-- Audit Logs table for admin monitoring
CREATE TABLE qrmfg_audit_logs (
    id NUMBER PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    username VARCHAR2(100) NOT NULL,
    action VARCHAR2(100) NOT NULL,
    entity_type VARCHAR2(50) NOT NULL,
    entity_id VARCHAR2(100),
    details VARCHAR2(2000),
    ip_address VARCHAR2(45),
    user_agent VARCHAR2(500),
    session_id VARCHAR2(100)
);

-- Users table for user management
CREATE TABLE qrmfg_users (
    id NUMBER PRIMARY KEY,
    username VARCHAR2(100) UNIQUE NOT NULL,
    email VARCHAR2(255) UNIQUE NOT NULL,
    first_name VARCHAR2(100),
    last_name VARCHAR2(100),
    department VARCHAR2(100),
    plant_assignment VARCHAR2(100),
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    last_login TIMESTAMP
);

-- Roles table for role-based access control
CREATE TABLE qrmfg_roles (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(50) UNIQUE NOT NULL,
    description VARCHAR2(200),
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100)
);

-- User-Role mapping table
CREATE TABLE qrmfg_user_roles (
    user_id NUMBER NOT NULL,
    role_id NUMBER NOT NULL,
    assigned_at TIMESTAMP NOT NULL,
    assigned_by VARCHAR2(100),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES qrmfg_users(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES qrmfg_roles(id)
);

-- Create additional indexes for audit and user management
CREATE INDEX idx_audit_log_timestamp ON qrmfg_audit_logs(timestamp);
CREATE INDEX idx_audit_log_username ON qrmfg_audit_logs(username);
CREATE INDEX idx_audit_log_action ON qrmfg_audit_logs(action);
CREATE INDEX idx_audit_log_entity ON qrmfg_audit_logs(entity_type, entity_id);

CREATE INDEX idx_user_username ON qrmfg_users(username);
CREATE INDEX idx_user_email ON qrmfg_users(email);
CREATE INDEX idx_user_active ON qrmfg_users(is_active);
CREATE INDEX idx_user_plant ON qrmfg_users(plant_assignment);

CREATE INDEX idx_role_name ON qrmfg_roles(name);
CREATE INDEX idx_role_active ON qrmfg_roles(is_active);

-- Insert default roles for workflow management
INSERT INTO qrmfg_roles (id, name, description, is_active, created_at, updated_at, created_by) VALUES 
(ROLE_SEQ.NEXTVAL, 'ADMIN', 'System Administrator with full access', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO qrmfg_roles (id, name, description, is_active, created_at, updated_at, created_by) VALUES 
(ROLE_SEQ.NEXTVAL, 'JVC_USER', 'JVC team member who can initiate workflows', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO qrmfg_roles (id, name, description, is_active, created_at, updated_at, created_by) VALUES 
(ROLE_SEQ.NEXTVAL, 'PLANT_USER', 'Plant team member who completes questionnaires', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO qrmfg_roles (id, name, description, is_active, created_at, updated_at, created_by) VALUES 
(ROLE_SEQ.NEXTVAL, 'CQS_USER', 'CQS team member who resolves queries', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

INSERT INTO qrmfg_roles (id, name, description, is_active, created_at, updated_at, created_by) VALUES 
(ROLE_SEQ.NEXTVAL, 'TECH_USER', 'Technology team member who resolves technical queries', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SYSTEM');

-- Comments for documentation
COMMENT ON TABLE qrmfg_material_workflows IS 'Main workflow tracking table for MSDS materials';
COMMENT ON TABLE qrmfg_queries IS 'Queries raised during workflow processing';
COMMENT ON TABLE qrmfg_questionnaire_responses IS 'Plant team responses to questionnaire fields';
COMMENT ON TABLE qrmfg_audit_logs IS 'Audit trail for all system actions';
COMMENT ON TABLE qrmfg_users IS 'System users with workflow access';
COMMENT ON TABLE qrmfg_roles IS 'Role definitions for access control';
COMMENT ON TABLE qrmfg_user_roles IS 'User-role assignments';

COMMENT ON COLUMN qrmfg_material_workflows.workflow_state IS 'Current state: JVC_PENDING, PLANT_PENDING, CQS_PENDING, TECH_PENDING, COMPLETED';
COMMENT ON COLUMN qrmfg_queries.assigned_team IS 'Team assigned to resolve query: CQS, TECH';
COMMENT ON COLUMN qrmfg_queries.query_status IS 'Query status: OPEN, RESOLVED';
COMMENT ON COLUMN qrmfg_questionnaire_responses.field_type IS 'Field type: TEXT, NUMBER, DATE, BOOLEAN, SELECT, TEXTAREA';
COMMENT ON COLUMN qrmfg_audit_logs.action IS 'Action performed: CREATE_WORKFLOW, RESOLVE_QUERY, RAISE_QUERY, COMPLETE_WORKFLOW, etc.';
COMMENT ON COLUMN qrmfg_roles.name IS 'Role name: ADMIN, JVC_USER, PLANT_USER, CQS_USER, TECH_USER';