-- Seed demo documents for personnel-field testing.
-- Focus fields:
-- 1) core: doc_resp_person_id / created_by / last_updated_by
-- 2) audit-like: received_by / verified_by
-- 3) ext-person: attr33 / attr35 / attr38 / attr47 / attr48

WITH users AS (
    SELECT user_id, row_number() OVER (ORDER BY user_id) AS rn
    FROM tpl_user_t
    WHERE delete_flag = 'N'
),
u AS (
    SELECT
        COALESCE((SELECT user_id FROM users WHERE rn = 1), 1) AS u1,
        COALESCE((SELECT user_id FROM users WHERE rn = 2), 1) AS u2,
        COALESCE((SELECT user_id FROM users WHERE rn = 3), 1) AS u3,
        COALESCE((SELECT user_id FROM users WHERE rn = 4), 1) AS u4,
        COALESCE((SELECT user_id FROM users WHERE rn = 5), 1) AS u5,
        COALESCE((SELECT user_id FROM users WHERE rn = 6), 1) AS u6
),
company_pick AS (
    SELECT company_project_code, company_project_name
    FROM fdc_company_project_t
    WHERE delete_flag = 'N' AND enable_flag = 'Y'
    ORDER BY company_project_code
    LIMIT 1
),
module_pick AS (
    SELECT module_code
    FROM fdc_business_module_t
    WHERE delete_flag = 'N' AND enabled_flag = 'Y' AND COALESCE(level_num, 1) >= 2
    ORDER BY module_code
    LIMIT 1
),
base_doc_id AS (
    SELECT COALESCE(MAX(doc_id), 0) AS max_doc_id
    FROM fdc_document_t
),
seed_rows AS (
    SELECT *
    FROM (
        VALUES
            (1, 'PERS-TEST-001', '人员字段测试文档-01'),
            (2, 'PERS-TEST-002', '人员字段测试文档-02'),
            (3, 'PERS-TEST-003', '人员字段测试文档-03'),
            (4, 'PERS-TEST-004', '人员字段测试文档-04'),
            (5, 'PERS-TEST-005', '人员字段测试文档-05'),
            (6, 'PERS-TEST-006', '人员字段测试文档-06')
    ) AS t(idx, doc_biz_no, doc_name)
)
INSERT INTO fdc_document_t (
    doc_id,
    company_code,
    company_name,
    start_period,
    end_period,
    biz_module_code,
    doc_biz_no,
    doc_gen_date,
    arch_place_alpha2_code,
    origin_place_alpha2_code,
    carrier_type,
    doc_name,
    doc_organization_code,
    doc_resp_dept_id,
    doc_resp_person_id,
    rentention_term,
    security_level,
    doc_version,
    source_id,
    source_system,
    lifecycle_status,
    custody_status,
    description,
    received_by,
    received_time,
    verified_by,
    verification_time,
    attr1,
    attr33,
    attr35,
    attr38,
    attr47,
    attr48,
    delete_flag,
    created_by,
    creation_date,
    last_updated_by,
    last_update_date,
    tenantid
)
SELECT
    b.max_doc_id + s.idx,
    c.company_project_code,
    c.company_project_name,
    DATE '2026-01-01' + ((s.idx - 1) * INTERVAL '1 month'),
    DATE '2026-01-28' + ((s.idx - 1) * INTERVAL '1 month'),
    m.module_code,
    s.doc_biz_no,
    CURRENT_TIMESTAMP - (s.idx * INTERVAL '1 day'),
    'CN',
    'CN',
    CASE WHEN mod(s.idx, 2) = 0 THEN 'ELECTRONIC' ELSE 'PAPER' END,
    s.doc_name,
    'ORG_FIN',
    100 + s.idx,
    CASE mod(s.idx, 6)
        WHEN 1 THEN u.u1
        WHEN 2 THEN u.u2
        WHEN 3 THEN u.u3
        WHEN 4 THEN u.u4
        WHEN 5 THEN u.u5
        ELSE u.u6
    END,
    10,
    'INTERNAL_PUBLIC',
    '1.0',
    NULL,
    'FDC_TEST',
    'UNARCHIVED',
    'UNARCHIVED',
    '用于测试人员字段展示与查询（责任人/创建人/更新人/扩展人员字段）',
    CASE WHEN mod(s.idx, 2) = 0 THEN u.u2 ELSE u.u3 END,
    CURRENT_TIMESTAMP - (s.idx * INTERVAL '1 hour'),
    CASE WHEN mod(s.idx, 2) = 0 THEN u.u4 ELSE u.u5 END,
    CURRENT_TIMESTAMP - (s.idx * INTERVAL '30 minutes'),
    '是',
    CAST(CASE WHEN mod(s.idx, 2) = 0 THEN u.u3 ELSE u.u4 END AS varchar),
    CASE WHEN mod(s.idx, 2) = 0 THEN u.u4 ELSE u.u5 END,
    CASE WHEN mod(s.idx, 2) = 0 THEN u.u5 ELSE u.u6 END,
    CAST(CASE WHEN mod(s.idx, 2) = 0 THEN u.u1 ELSE u.u2 END AS varchar),
    CAST(CASE WHEN mod(s.idx, 2) = 0 THEN u.u2 ELSE u.u1 END AS varchar),
    0,
    CASE WHEN mod(s.idx, 2) = 0 THEN u.u5 ELSE u.u6 END,
    CURRENT_TIMESTAMP - (s.idx * INTERVAL '2 day'),
    CASE WHEN mod(s.idx, 2) = 0 THEN u.u6 ELSE u.u5 END,
    CURRENT_TIMESTAMP - (s.idx * INTERVAL '1 day'),
    1
FROM seed_rows s
CROSS JOIN base_doc_id b
CROSS JOIN u
CROSS JOIN company_pick c
CROSS JOIN module_pick m
WHERE NOT EXISTS (
    SELECT 1
    FROM fdc_document_t d
    WHERE d.doc_biz_no = s.doc_biz_no
      AND COALESCE(d.delete_flag, 0) = 0
);
