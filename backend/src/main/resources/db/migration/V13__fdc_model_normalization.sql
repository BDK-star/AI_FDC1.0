-- FDC physical model normalization — see .docs/FDC_MODEL_MAPPING.md

BEGIN;

-- ========== 1) Rename tables ==========
DO $$
DECLARE
    pair RECORD;
BEGIN
    FOR pair IN
        SELECT * FROM (VALUES
            ('doc_document_type', 'fdc_document_type_t'),
            ('com_operation_audit', 'fdc_operation_audit_log_t'),
            ('md_document_organization', 'fdc_document_organization_t'),
            ('md_document_organization_city', 'fdc_document_organization_city_t'),
            ('md_country', 'fdc_country_t'),
            ('md_company_project', 'fdc_company_project_t'),
            ('md_company_project_line', 'fdc_company_project_line_t'),
            ('md_company_project_org_category', 'fdc_company_project_org_category_t'),
            ('md_security_level', 'fdc_security_level_t'),
            ('md_archive_flow_rule', 'fdc_archive_rule_t'),
            ('md_dict_category', 'fdc_dict_category_t'),
            ('md_dict_item', 'fdc_dict_item_t'),
            ('arc_ext_field_config', 'fdc_doc_field_config_t'),
            ('arc_archive_create_session', 'fdc_archive_create_session_t'),
            ('arc_archive', 'fdc_arch_t'),
            ('arc_archive_ext_value', 'fdc_doc_ext_t'),
            ('arc_archive_attachment', 'fdc_arch_attachment_t'),
            ('arc_archive_paper', 'fdc_arch_paper_t'),
            ('arc_archive_content', 'fdc_arch_content_t'),
            ('arc_archive_content_chunk', 'fdc_arch_content_chunk_t'),
            ('arc_archive_chunk_vector', 'fdc_arch_chunk_vector_t'),
            ('arc_archive_ai_task', 'fdc_arch_ai_task_t'),
            ('ai_model_config', 'fdc_ai_model_config_t'),
            ('wh_warehouse', 'fdc_warehouse_t'),
            ('wh_area', 'fdc_warehouse_area_t'),
            ('wh_rack', 'fdc_warehouse_rack_t'),
            ('wh_location', 'fdc_warehouse_location_t'),
            ('arc_archive_object', 'fdc_archive_object_t'),
            ('arc_archive_receipt', 'fdc_archive_receipt_t'),
            ('arc_catalog_task', 'fdc_catalog_task_t'),
            ('wf_workflow_instance', 'fdc_workflow_instance_t'),
            ('wf_workflow_task', 'fdc_workflow_task_t'),
            ('wf_workflow_history', 'fdc_workflow_history_t'),
            ('arc_borrow_record', 'fdc_borrow_record_t'),
            ('arc_inventory_task', 'fdc_inventory_task_t'),
            ('arc_disposal_record', 'fdc_disposal_record_t'),
            ('arc_bind_batch', 'fdc_bind_batch_t'),
            ('arc_bind_volume', 'fdc_bind_volume_t'),
            ('arc_bind_volume_item', 'fdc_bind_volume_item_t'),
            ('arc_storage_batch', 'fdc_storage_batch_t'),
            ('arc_storage_batch_item', 'fdc_storage_batch_item_t'),
            ('arc_storage_ledger', 'fdc_storage_ledger_t'),
            ('kg_matching_rule_version', 'fdc_kg_matching_rule_version_t'),
            ('kg_rebuild_task', 'fdc_kg_rebuild_task_t'),
            ('kg_graph_node', 'fdc_kg_graph_node_t'),
            ('kg_graph_edge', 'fdc_kg_graph_edge_t'),
            ('kg_procurement_conversation', 'fdc_procurement_conversation_t'),
            ('kg_procurement_conversation_message', 'fdc_procurement_conversation_message_t'),
            ('kg_procurement_context_snapshot', 'fdc_procurement_context_snapshot_t')
        ) AS v(src, dst)
    LOOP
        IF to_regclass('public.' || pair.src) IS NOT NULL
           AND to_regclass('public.' || pair.dst) IS NULL THEN
            EXECUTE format('ALTER TABLE %I RENAME TO %I', pair.src, pair.dst);
        END IF;
    END LOOP;
END $$;

-- Repair: some databases at v12 never had arc_archive_object / wh_location (manual cleanup,
-- partial restore, or non-standard init). Section 1 skips rename when the legacy table is
-- missing; later steps assume the FDC table names exist.
DO $$
BEGIN
    IF to_regclass('public.fdc_archive_object_t') IS NULL THEN
        CREATE TABLE fdc_archive_object_t (
            id BIGSERIAL PRIMARY KEY,
            archive_code VARCHAR(64) NOT NULL UNIQUE,
            title VARCHAR(255) NOT NULL,
            archive_type VARCHAR(64) NOT NULL,
            classification_code VARCHAR(64),
            security_level VARCHAR(32) NOT NULL,
            retention_period VARCHAR(64) NOT NULL,
            organization_name VARCHAR(128) NOT NULL,
            fonds_name VARCHAR(128),
            carrier_type VARCHAR(32) NOT NULL,
            physical_status VARCHAR(32) NOT NULL,
            digital_status VARCHAR(32) NOT NULL,
            current_workflow_stage VARCHAR(64) NOT NULL,
            current_warehouse_code VARCHAR(64),
            current_location_code VARCHAR(64),
            responsible_person VARCHAR(64),
            formed_date DATE,
            file_count INTEGER DEFAULT 0,
            page_count INTEGER DEFAULT 0,
            ai_classified BOOLEAN DEFAULT FALSE,
            ai_metadata_extracted BOOLEAN DEFAULT FALSE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            deleted SMALLINT DEFAULT 0
        );
    END IF;

    IF to_regclass('public.fdc_warehouse_location_t') IS NULL THEN
        CREATE TABLE fdc_warehouse_location_t (
            id BIGSERIAL PRIMARY KEY,
            warehouse_code VARCHAR(64) NOT NULL,
            warehouse_name VARCHAR(128) NOT NULL,
            area_code VARCHAR(64) NOT NULL,
            shelf_code VARCHAR(64) NOT NULL,
            layer_code VARCHAR(64) NOT NULL,
            location_code VARCHAR(64) NOT NULL UNIQUE,
            location_name VARCHAR(128) NOT NULL,
            status VARCHAR(32) NOT NULL,
            capacity INTEGER NOT NULL DEFAULT 0,
            occupied_count INTEGER NOT NULL DEFAULT 0,
            x INTEGER NOT NULL DEFAULT 0,
            y INTEGER NOT NULL DEFAULT 0,
            width INTEGER NOT NULL DEFAULT 120,
            height INTEGER NOT NULL DEFAULT 72,
            utilization_rate NUMERIC(5, 2) DEFAULT 0,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            deleted SMALLINT DEFAULT 0
        );
    END IF;
END $$;

-- ========== 2) enabled_flag -> enable_flag ==========
DO $$
DECLARE
    pair RECORD;
BEGIN
    FOR pair IN
        SELECT * FROM (VALUES
            ('fdc_document_type_t', 'enabled_flag', 'enable_flag'),
            ('fdc_document_organization_t', 'enabled_flag', 'enable_flag'),
            ('fdc_document_organization_city_t', 'enabled_flag', 'enable_flag'),
            ('fdc_country_t', 'enabled_flag', 'enable_flag'),
            ('fdc_company_project_t', 'enabled_flag', 'enable_flag'),
            ('fdc_company_project_org_category_t', 'enabled_flag', 'enable_flag'),
            ('fdc_security_level_t', 'enabled_flag', 'enable_flag')
        ) AS v(tbl, src, dst)
    LOOP
        IF to_regclass('public.' || pair.tbl) IS NOT NULL
           AND EXISTS (
                SELECT 1
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = pair.tbl
                   AND column_name = pair.src
           )
           AND NOT EXISTS (
                SELECT 1
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = pair.tbl
                   AND column_name = pair.dst
           )
        THEN
            EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I', pair.tbl, pair.src, pair.dst);
        END IF;
    END LOOP;
END $$;

DROP INDEX IF EXISTS idx_md_archive_flow_rule_enabled;
ALTER TABLE fdc_archive_rule_t RENAME COLUMN enabled_flag TO enable_flag;
CREATE INDEX IF NOT EXISTS idx_fdc_archive_rule_tn5 ON fdc_archive_rule_t (enable_flag);

ALTER TABLE fdc_dict_category_t DROP CONSTRAINT IF EXISTS ck_md_dict_category_enabled;
ALTER TABLE fdc_dict_category_t RENAME COLUMN enabled_flag TO enable_flag;
ALTER TABLE fdc_dict_category_t ADD CONSTRAINT ck_fdc_dict_category_t_enable CHECK (enable_flag IN ('Y','N'));

ALTER TABLE fdc_dict_item_t DROP CONSTRAINT IF EXISTS ck_md_dict_item_enabled;
DROP INDEX IF EXISTS idx_md_dict_item_category;
ALTER TABLE fdc_dict_item_t RENAME COLUMN enabled_flag TO enable_flag;
CREATE INDEX IF NOT EXISTS idx_fdc_dict_item_tn1 ON fdc_dict_item_t (category_code, enable_flag, delete_flag, sort_order);
ALTER TABLE fdc_dict_item_t ADD CONSTRAINT ck_fdc_dict_item_t_enable CHECK (enable_flag IN ('Y','N'));

ALTER TABLE fdc_doc_field_config_t DROP CONSTRAINT IF EXISTS ck_arc_ext_field_config_enabled;
DROP INDEX IF EXISTS idx_arc_ext_field_config_type;
DROP INDEX IF EXISTS idx_arc_ext_field_config_query;
DROP INDEX IF EXISTS idx_arc_ext_field_config_semantic;
ALTER TABLE fdc_doc_field_config_t RENAME COLUMN enabled_flag TO enable_flag;
ALTER TABLE fdc_doc_field_config_t ADD CONSTRAINT ck_fdc_doc_field_config_t_enable CHECK (enable_flag IN ('Y', 'N'));
CREATE INDEX IF NOT EXISTS idx_fdc_doc_field_config_tn1 ON fdc_doc_field_config_t (document_type_code, enable_flag, delete_flag, form_sort_order);
CREATE INDEX IF NOT EXISTS idx_fdc_doc_field_config_tn2 ON fdc_doc_field_config_t (document_type_code, query_enabled_flag, delete_flag, query_sort_order);
CREATE INDEX IF NOT EXISTS idx_fdc_doc_field_config_tn3 ON fdc_doc_field_config_t (semantic_code, document_type_code, delete_flag);

ALTER TABLE fdc_ai_model_config_t DROP CONSTRAINT IF EXISTS ck_ai_model_config_enabled;
ALTER TABLE fdc_ai_model_config_t RENAME COLUMN enabled_flag TO enable_flag;
ALTER TABLE fdc_ai_model_config_t ADD CONSTRAINT ck_fdc_ai_model_config_t_enable CHECK (enable_flag IN ('Y', 'N'));

ALTER TABLE fdc_kg_matching_rule_version_t DROP CONSTRAINT IF EXISTS ck_kg_matching_rule_enabled;
DO $$
BEGIN
    IF to_regclass('public.fdc_kg_matching_rule_version_t') IS NOT NULL
       AND EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'fdc_kg_matching_rule_version_t'
               AND column_name = 'enabled_flag'
       )
       AND NOT EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'fdc_kg_matching_rule_version_t'
               AND column_name = 'enable_flag'
       )
    THEN
        EXECUTE 'ALTER TABLE fdc_kg_matching_rule_version_t RENAME COLUMN enabled_flag TO enable_flag';
    END IF;
END $$;
ALTER TABLE fdc_kg_matching_rule_version_t ADD CONSTRAINT ck_fdc_kg_matching_rule_version_t_enable CHECK (enable_flag IN ('Y', 'N'));

-- ========== 3) Semantic PK + sequences (tables without cross-FK on PK renames) ==========
DO $$
DECLARE
    col_pair RECORD;
    seq_pair RECORD;
BEGIN
    FOR col_pair IN
        SELECT * FROM (VALUES
            ('fdc_document_type_t', 'id', 'document_type_id'),
            ('fdc_operation_audit_log_t', 'id', 'operation_audit_log_id'),
            ('fdc_document_organization_t', 'id', 'document_organization_id'),
            ('fdc_document_organization_city_t', 'id', 'document_organization_city_id'),
            ('fdc_country_t', 'id', 'country_id'),
            ('fdc_company_project_t', 'id', 'company_project_id'),
            ('fdc_company_project_line_t', 'id', 'company_project_line_id'),
            ('fdc_company_project_org_category_t', 'id', 'company_project_org_category_id'),
            ('fdc_security_level_t', 'id', 'security_level_id'),
            ('fdc_archive_rule_t', 'id', 'archive_rule_id'),
            ('fdc_dict_category_t', 'id', 'dict_category_id'),
            ('fdc_dict_item_t', 'id', 'dict_item_id'),
            ('fdc_doc_field_config_t', 'field_id', 'doc_field_config_id'),
            ('fdc_doc_ext_t', 'value_id', 'doc_ext_id'),
            ('fdc_ai_model_config_t', 'model_config_id', 'ai_model_config_id'),
            ('fdc_warehouse_t', 'id', 'warehouse_id'),
            ('fdc_warehouse_area_t', 'id', 'warehouse_area_id'),
            ('fdc_warehouse_rack_t', 'id', 'warehouse_rack_id'),
            ('fdc_warehouse_location_t', 'id', 'warehouse_location_id'),
            ('fdc_archive_object_t', 'id', 'archive_object_id'),
            ('fdc_archive_receipt_t', 'id', 'archive_receipt_id'),
            ('fdc_catalog_task_t', 'id', 'catalog_task_id'),
            ('fdc_workflow_instance_t', 'id', 'workflow_instance_id'),
            ('fdc_workflow_task_t', 'id', 'workflow_task_id'),
            ('fdc_workflow_history_t', 'id', 'workflow_history_id'),
            ('fdc_borrow_record_t', 'id', 'borrow_record_id'),
            ('fdc_inventory_task_t', 'id', 'inventory_task_id'),
            ('fdc_disposal_record_t', 'id', 'disposal_record_id')
        ) AS v(tbl, src, dst)
    LOOP
        IF to_regclass('public.' || col_pair.tbl) IS NOT NULL
           AND EXISTS (
                SELECT 1
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = col_pair.tbl
                   AND column_name = col_pair.src
           )
           AND NOT EXISTS (
                SELECT 1
                  FROM information_schema.columns
                 WHERE table_schema = 'public'
                   AND table_name = col_pair.tbl
                   AND column_name = col_pair.dst
           )
        THEN
            EXECUTE format('ALTER TABLE %I RENAME COLUMN %I TO %I', col_pair.tbl, col_pair.src, col_pair.dst);
        END IF;
    END LOOP;

    FOR seq_pair IN
        SELECT * FROM (VALUES
            ('doc_document_type_id_seq', 'fdc_document_type_t_document_type_id_seq'),
            ('com_operation_audit_id_seq', 'fdc_operation_audit_log_t_operation_audit_log_id_seq'),
            ('md_document_organization_id_seq', 'fdc_document_organization_t_document_organization_id_seq'),
            ('md_document_organization_city_id_seq', 'fdc_document_organization_city_t_document_organization_city_id_seq'),
            ('md_country_id_seq', 'fdc_country_t_country_id_seq'),
            ('md_company_project_id_seq', 'fdc_company_project_t_company_project_id_seq'),
            ('md_company_project_line_id_seq', 'fdc_company_project_line_t_company_project_line_id_seq'),
            ('md_company_project_org_category_id_seq', 'fdc_company_project_org_category_t_company_project_org_category_id_seq'),
            ('md_security_level_id_seq', 'fdc_security_level_t_security_level_id_seq'),
            ('md_archive_flow_rule_id_seq', 'fdc_archive_rule_t_archive_rule_id_seq'),
            ('md_dict_category_id_seq', 'fdc_dict_category_t_dict_category_id_seq'),
            ('md_dict_item_id_seq', 'fdc_dict_item_t_dict_item_id_seq'),
            ('arc_ext_field_config_field_id_seq', 'fdc_doc_field_config_t_doc_field_config_id_seq'),
            ('arc_archive_ext_value_value_id_seq', 'fdc_doc_ext_t_doc_ext_id_seq'),
            ('ai_model_config_model_config_id_seq', 'fdc_ai_model_config_t_ai_model_config_id_seq'),
            ('wh_warehouse_id_seq', 'fdc_warehouse_t_warehouse_id_seq'),
            ('wh_area_id_seq', 'fdc_warehouse_area_t_warehouse_area_id_seq'),
            ('wh_rack_id_seq', 'fdc_warehouse_rack_t_warehouse_rack_id_seq'),
            ('wh_location_id_seq', 'fdc_warehouse_location_t_warehouse_location_id_seq'),
            ('arc_archive_object_id_seq', 'fdc_archive_object_t_archive_object_id_seq'),
            ('arc_archive_receipt_id_seq', 'fdc_archive_receipt_t_archive_receipt_id_seq'),
            ('arc_catalog_task_id_seq', 'fdc_catalog_task_t_catalog_task_id_seq'),
            ('wf_workflow_instance_id_seq', 'fdc_workflow_instance_t_workflow_instance_id_seq'),
            ('wf_workflow_task_id_seq', 'fdc_workflow_task_t_workflow_task_id_seq'),
            ('wf_workflow_history_id_seq', 'fdc_workflow_history_t_workflow_history_id_seq'),
            ('arc_borrow_record_id_seq', 'fdc_borrow_record_t_borrow_record_id_seq'),
            ('arc_inventory_task_id_seq', 'fdc_inventory_task_t_inventory_task_id_seq'),
            ('arc_disposal_record_id_seq', 'fdc_disposal_record_t_disposal_record_id_seq')
        ) AS v(src, dst)
    LOOP
        IF to_regclass('public.' || seq_pair.src) IS NOT NULL
           AND to_regclass('public.' || seq_pair.dst) IS NULL THEN
            EXECUTE format('ALTER SEQUENCE %I RENAME TO %I', seq_pair.src, seq_pair.dst);
        END IF;
    END LOOP;
END $$;

-- ========== 4) Legacy soft-delete ==========
DO $$
BEGIN
    IF to_regclass('public.fdc_archive_object_t') IS NOT NULL THEN
        ALTER TABLE fdc_archive_object_t ADD COLUMN IF NOT EXISTS delete_flag CHAR(1) NOT NULL DEFAULT 'N';
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'fdc_archive_object_t' AND column_name = 'deleted'
        ) THEN
            UPDATE fdc_archive_object_t SET delete_flag = CASE WHEN deleted IS NOT NULL AND deleted <> 0 THEN 'Y' ELSE 'N' END;
            ALTER TABLE fdc_archive_object_t DROP COLUMN deleted;
        END IF;
    END IF;

    IF to_regclass('public.fdc_warehouse_location_t') IS NOT NULL THEN
        ALTER TABLE fdc_warehouse_location_t ADD COLUMN IF NOT EXISTS delete_flag CHAR(1) NOT NULL DEFAULT 'N';
        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'fdc_warehouse_location_t' AND column_name = 'deleted'
        ) THEN
            UPDATE fdc_warehouse_location_t SET delete_flag = CASE WHEN deleted IS NOT NULL AND deleted <> 0 THEN 'Y' ELSE 'N' END;
            ALTER TABLE fdc_warehouse_location_t DROP COLUMN deleted;
        END IF;
    END IF;
END $$;

-- ========== 5) warehouse_location: time column names ==========
DO $$
BEGIN
    IF to_regclass('public.fdc_warehouse_location_t') IS NOT NULL
       AND EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'fdc_warehouse_location_t' AND column_name = 'updated_at'
       )
    THEN
        EXECUTE 'ALTER TABLE fdc_warehouse_location_t RENAME COLUMN updated_at TO last_update_date';
    END IF;
END $$;

-- ========== 6) utilization_rate precision ==========
ALTER TABLE fdc_warehouse_location_t
    ALTER COLUMN utilization_rate TYPE NUMERIC(7, 6)
    USING CASE WHEN utilization_rate IS NULL THEN NULL ELSE utilization_rate::NUMERIC(7, 6) END;

-- ========== 7) Governance columns ==========

ALTER TABLE fdc_document_type_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_document_type_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_document_type_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_document_type_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_operation_audit_log_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_operation_audit_log_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_operation_audit_log_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_operation_audit_log_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_document_organization_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_document_organization_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_document_organization_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_document_organization_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_document_organization_city_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_document_organization_city_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_document_organization_city_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_document_organization_city_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_country_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_country_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_country_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_country_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_company_project_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_company_project_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_company_project_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_company_project_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_company_project_line_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_company_project_line_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_company_project_line_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_company_project_line_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_company_project_org_category_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_company_project_org_category_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_company_project_org_category_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_company_project_org_category_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_security_level_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_security_level_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_security_level_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_security_level_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_archive_rule_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_archive_rule_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_archive_rule_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_archive_rule_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_dict_category_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_dict_category_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_dict_category_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_dict_category_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_dict_item_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_dict_item_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_dict_item_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_dict_item_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_doc_field_config_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_doc_field_config_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_doc_field_config_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_doc_field_config_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_arch_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_arch_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_arch_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_arch_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_doc_ext_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_doc_ext_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_doc_ext_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_doc_ext_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_arch_attachment_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_arch_attachment_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_arch_attachment_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_arch_attachment_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_ai_model_config_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_ai_model_config_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_ai_model_config_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_ai_model_config_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE fdc_warehouse_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_warehouse_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_warehouse_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_warehouse_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE fdc_warehouse_t ADD COLUMN IF NOT EXISTS enable_flag CHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE fdc_warehouse_t ADD COLUMN IF NOT EXISTS delete_flag CHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE fdc_warehouse_area_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_warehouse_area_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_warehouse_area_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_warehouse_area_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE fdc_warehouse_area_t ADD COLUMN IF NOT EXISTS enable_flag CHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE fdc_warehouse_area_t ADD COLUMN IF NOT EXISTS delete_flag CHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE fdc_warehouse_rack_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_warehouse_rack_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_warehouse_rack_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_warehouse_rack_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE fdc_warehouse_rack_t ADD COLUMN IF NOT EXISTS enable_flag CHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE fdc_warehouse_rack_t ADD COLUMN IF NOT EXISTS delete_flag CHAR(1) NOT NULL DEFAULT 'N';

ALTER TABLE fdc_warehouse_location_t ADD COLUMN IF NOT EXISTS tenantid BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_warehouse_location_t ADD COLUMN IF NOT EXISTS sys_description VARCHAR(500);
ALTER TABLE fdc_warehouse_location_t ADD COLUMN IF NOT EXISTS last_update_trace_id VARCHAR(100);
ALTER TABLE fdc_warehouse_location_t ADD COLUMN IF NOT EXISTS last_update_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE fdc_warehouse_location_t ADD COLUMN IF NOT EXISTS enable_flag CHAR(1) NOT NULL DEFAULT 'Y';
ALTER TABLE fdc_warehouse_location_t ADD COLUMN IF NOT EXISTS created_by BIGINT NOT NULL DEFAULT 1;
ALTER TABLE fdc_warehouse_location_t ADD COLUMN IF NOT EXISTS creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE fdc_warehouse_location_t ADD COLUMN IF NOT EXISTS last_updated_by BIGINT NOT NULL DEFAULT 1;

UPDATE fdc_warehouse_location_t SET creation_date = COALESCE(creation_date, last_update_date, CURRENT_TIMESTAMP),
    last_update_date = COALESCE(last_update_date, CURRENT_TIMESTAMP);

COMMIT;
