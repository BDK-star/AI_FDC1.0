-- V39 post-migration validation script (PostgreSQL)
-- Purpose: verify fdc_document_t data quality after V39.
-- Safe to run multiple times; read-only.

-- 1) Row count comparison: legacy vs new
SELECT 'row_count_legacy' AS check_name, COUNT(*)::BIGINT AS check_value
FROM fdc_document_t_legacy_v33
UNION ALL
SELECT 'row_count_new', COUNT(*)::BIGINT
FROM fdc_document_t;

-- 2) Missing ID mapping (legacy -> new)
SELECT COUNT(*)::BIGINT AS missing_doc_id_count
FROM fdc_document_t_legacy_v33 l
LEFT JOIN fdc_document_t n ON n.doc_id = l.document_id
WHERE n.doc_id IS NULL;

-- 3) Mandatory fields null/blank checks (new table)
SELECT 'null_doc_id' AS check_name, COUNT(*)::BIGINT AS check_value FROM fdc_document_t WHERE doc_id IS NULL
UNION ALL
SELECT 'blank_company_code', COUNT(*)::BIGINT FROM fdc_document_t WHERE company_code IS NULL OR BTRIM(company_code) = ''
UNION ALL
SELECT 'blank_company_name', COUNT(*)::BIGINT FROM fdc_document_t WHERE company_name IS NULL OR BTRIM(company_name) = ''
UNION ALL
SELECT 'null_start_period', COUNT(*)::BIGINT FROM fdc_document_t WHERE start_period IS NULL
UNION ALL
SELECT 'blank_biz_module_code', COUNT(*)::BIGINT FROM fdc_document_t WHERE biz_module_code IS NULL OR BTRIM(biz_module_code) = ''
UNION ALL
SELECT 'blank_doc_biz_no', COUNT(*)::BIGINT FROM fdc_document_t WHERE doc_biz_no IS NULL OR BTRIM(doc_biz_no) = ''
UNION ALL
SELECT 'null_doc_gen_date', COUNT(*)::BIGINT FROM fdc_document_t WHERE doc_gen_date IS NULL
UNION ALL
SELECT 'blank_arch_place_alpha2_code', COUNT(*)::BIGINT FROM fdc_document_t WHERE arch_place_alpha2_code IS NULL OR BTRIM(arch_place_alpha2_code) = ''
UNION ALL
SELECT 'blank_origin_place_alpha2_code', COUNT(*)::BIGINT FROM fdc_document_t WHERE origin_place_alpha2_code IS NULL OR BTRIM(origin_place_alpha2_code) = ''
UNION ALL
SELECT 'blank_carrier_type', COUNT(*)::BIGINT FROM fdc_document_t WHERE carrier_type IS NULL OR BTRIM(carrier_type) = ''
UNION ALL
SELECT 'blank_doc_name', COUNT(*)::BIGINT FROM fdc_document_t WHERE doc_name IS NULL OR BTRIM(doc_name) = ''
UNION ALL
SELECT 'blank_doc_organization_code', COUNT(*)::BIGINT FROM fdc_document_t WHERE doc_organization_code IS NULL OR BTRIM(doc_organization_code) = ''
UNION ALL
SELECT 'null_doc_resp_dept_id', COUNT(*)::BIGINT FROM fdc_document_t WHERE doc_resp_dept_id IS NULL
UNION ALL
SELECT 'null_doc_resp_person_id', COUNT(*)::BIGINT FROM fdc_document_t WHERE doc_resp_person_id IS NULL
UNION ALL
SELECT 'null_rentention_term', COUNT(*)::BIGINT FROM fdc_document_t WHERE rentention_term IS NULL
UNION ALL
SELECT 'blank_security_level', COUNT(*)::BIGINT FROM fdc_document_t WHERE security_level IS NULL OR BTRIM(security_level) = ''
UNION ALL
SELECT 'blank_doc_version', COUNT(*)::BIGINT FROM fdc_document_t WHERE doc_version IS NULL OR BTRIM(doc_version) = ''
UNION ALL
SELECT 'blank_source_system', COUNT(*)::BIGINT FROM fdc_document_t WHERE source_system IS NULL OR BTRIM(source_system) = ''
UNION ALL
SELECT 'blank_lifecycle_status', COUNT(*)::BIGINT FROM fdc_document_t WHERE lifecycle_status IS NULL OR BTRIM(lifecycle_status) = ''
UNION ALL
SELECT 'blank_custody_status', COUNT(*)::BIGINT FROM fdc_document_t WHERE custody_status IS NULL OR BTRIM(custody_status) = ''
UNION ALL
SELECT 'null_delete_flag', COUNT(*)::BIGINT FROM fdc_document_t WHERE delete_flag IS NULL
UNION ALL
SELECT 'null_created_by', COUNT(*)::BIGINT FROM fdc_document_t WHERE created_by IS NULL
UNION ALL
SELECT 'null_creation_date', COUNT(*)::BIGINT FROM fdc_document_t WHERE creation_date IS NULL
UNION ALL
SELECT 'null_last_updated_by', COUNT(*)::BIGINT FROM fdc_document_t WHERE last_updated_by IS NULL
UNION ALL
SELECT 'null_last_update_date', COUNT(*)::BIGINT FROM fdc_document_t WHERE last_update_date IS NULL;

-- 4) Referential checks for dependent tables
SELECT 'orphan_document_attach' AS check_name, COUNT(*)::BIGINT AS check_value
FROM fdc_document_attach_t a
LEFT JOIN fdc_document_t d ON d.doc_id = a.document_id
WHERE d.doc_id IS NULL
UNION ALL
SELECT 'orphan_arch_storage', COUNT(*)::BIGINT
FROM fdc_arch_storage_t s
LEFT JOIN fdc_document_t d ON d.doc_id = s.document_id
WHERE d.doc_id IS NULL;

-- 5) Duplicate business key check (tenantid + doc_biz_no)
SELECT tenantid, doc_biz_no, COUNT(*) AS dup_count
FROM fdc_document_t
GROUP BY tenantid, doc_biz_no
HAVING COUNT(*) > 1
ORDER BY dup_count DESC, tenantid, doc_biz_no;

-- 6) Distribution check (top values)
SELECT lifecycle_status, COUNT(*) AS cnt
FROM fdc_document_t
GROUP BY lifecycle_status
ORDER BY cnt DESC;

SELECT custody_status, COUNT(*) AS cnt
FROM fdc_document_t
GROUP BY custody_status
ORDER BY cnt DESC;

-- 7) Sampling check (latest 20 rows)
SELECT
    doc_id,
    tenantid,
    doc_biz_no,
    company_code,
    company_name,
    doc_name,
    lifecycle_status,
    custody_status,
    creation_date,
    last_update_date
FROM fdc_document_t
ORDER BY doc_id DESC
LIMIT 20;
