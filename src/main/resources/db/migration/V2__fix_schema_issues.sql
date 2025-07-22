-- Fix schema issues for existing tables
-- This migration handles the NOT NULL column additions safely

-- Fix FSObjectReference table
-- First check if OBJECT_ID column exists and is nullable
BEGIN
    -- Add OBJECT_ID column as nullable first if it doesn't exist
    EXECUTE IMMEDIATE 'ALTER TABLE FSOBJECTREFERENCE ADD OBJECT_ID VARCHAR2(255)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN -- Column already exists
            RAISE;
        END IF;
END;
/

-- Update any NULL values with generated IDs
UPDATE FSOBJECTREFERENCE 
SET OBJECT_ID = 'OBJ_' || ROWNUM 
WHERE OBJECT_ID IS NULL;

-- Now make it NOT NULL
ALTER TABLE FSOBJECTREFERENCE MODIFY OBJECT_ID VARCHAR2(255) NOT NULL;

-- Add primary key constraint if it doesn't exist
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE FSOBJECTREFERENCE ADD CONSTRAINT PK_FSOBJECTREFERENCE PRIMARY KEY (OBJECT_ID)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2260 THEN -- Constraint already exists
            RAISE;
        END IF;
END;
/

-- Fix QRMFGQuestionnaireMaster table
-- First check if ID column exists
BEGIN
    -- Add ID column as nullable first if it doesn't exist
    EXECUTE IMMEDIATE 'ALTER TABLE QRMFG_QUESTIONNAIRE_MASTER ADD ID NUMBER(19,0)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN -- Column already exists
            RAISE;
        END IF;
END;
/

-- Create sequence if it doesn't exist
BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE QUESTIONNAIRE_MASTER_SEQ START WITH 1 INCREMENT BY 1';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN -- Sequence already exists
            RAISE;
        END IF;
END;
/

-- Update any NULL values with sequence values
UPDATE QRMFG_QUESTIONNAIRE_MASTER 
SET ID = QUESTIONNAIRE_MASTER_SEQ.NEXTVAL 
WHERE ID IS NULL;

-- Now make it NOT NULL
ALTER TABLE QRMFG_QUESTIONNAIRE_MASTER MODIFY ID NUMBER(19,0) NOT NULL;

-- Add primary key constraint if it doesn't exist
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE QRMFG_QUESTIONNAIRE_MASTER ADD CONSTRAINT PK_QRMFG_QUESTIONNAIRE_MASTER PRIMARY KEY (ID)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2260 THEN -- Constraint already exists
            RAISE;
        END IF;
END;
/

-- Ensure material workflow sequence exists
BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE MATERIAL_WORKFLOW_SEQ START WITH 1 INCREMENT BY 1';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN -- Sequence already exists
            RAISE;
        END IF;
END;
/

COMMIT;