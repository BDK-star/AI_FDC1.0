-- 为 V88 演示公司项目（CP-SH/SZ/CD/HZ）与 DEMO_* 业务模块补充归档流向；文档组织为新编码 DO-FLOW-2026-*（不使用 DO-DEMO-*）。
-- 归档地沿用 fdc_document_organization_city_t 种子：SHANGHAI / BEIJING / SHENZHEN（与既有演示数据一致）。
-- 表结构以 V78 为准：company_code, module_code, cust_mapping_code, arch_place_alpha2_code, retention_term, visible_flag, default_flag。

-- ========== 文档组织 ==========
INSERT INTO fdc_document_organization_t (
    document_organization_code,
    document_organization_name,
    description,
    country_code,
    city_code,
    enable_flag,
    delete_flag,
    created_by,
    creation_date,
    last_updated_by,
    last_update_date
)
SELECT
    'DO-FLOW-2026-SH',
    '归档流向文档组织-上海',
    'V88 演示：上海归档中心（归档流向专用）',
    'CN',
    'SHANGHAI',
    'Y',
    'N',
    1,
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM fdc_document_organization_t o
    WHERE o.document_organization_code = 'DO-FLOW-2026-SH' AND o.delete_flag = 'N'
);

INSERT INTO fdc_document_organization_t (
    document_organization_code,
    document_organization_name,
    description,
    country_code,
    city_code,
    enable_flag,
    delete_flag,
    created_by,
    creation_date,
    last_updated_by,
    last_update_date
)
SELECT
    'DO-FLOW-2026-SZ',
    '归档流向文档组织-深圳',
    'V88 演示：深圳归档中心（归档流向专用）',
    'CN',
    'SHENZHEN',
    'Y',
    'N',
    1,
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM fdc_document_organization_t o
    WHERE o.document_organization_code = 'DO-FLOW-2026-SZ' AND o.delete_flag = 'N'
);

INSERT INTO fdc_document_organization_t (
    document_organization_code,
    document_organization_name,
    description,
    country_code,
    city_code,
    enable_flag,
    delete_flag,
    created_by,
    creation_date,
    last_updated_by,
    last_update_date
)
SELECT
    'DO-FLOW-2026-BJ',
    '归档流向文档组织-北京',
    'V88 演示：北京归档中心（归档流向专用）',
    'CN',
    'BEIJING',
    'Y',
    'N',
    1,
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM fdc_document_organization_t o
    WHERE o.document_organization_code = 'DO-FLOW-2026-BJ' AND o.delete_flag = 'N'
);

-- ========== 归档规则（uk_fdc_archive_rule_business：公司 + 模块 + trim 后 cust + trim 后归档地）==========
INSERT INTO fdc_archive_rule_t (
    company_code,
    module_code,
    cust_mapping_code,
    arch_place_alpha2_code,
    document_organization_code,
    retention_term,
    visible_flag,
    default_flag,
    enable_flag,
    delete_flag,
    created_by,
    creation_date,
    last_updated_by,
    last_update_date
)
SELECT
    v.company_code,
    v.module_code,
    v.cust_mapping_code,
    v.arch_place_alpha2_code,
    v.document_organization_code,
    v.retention_term,
    v.visible_flag,
    v.default_flag,
    'Y',
    'N',
    1,
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP
FROM (VALUES
    -- 上海亚太实业：会计凭证/报表多归档地
    ('CP-SH-2026-001', 'DEMO_ACC_VCH_RCPT', NULL::varchar, 'SHANGHAI', 'DO-FLOW-2026-SH', 30, 'N', 'Y'),
    ('CP-SH-2026-001', 'DEMO_ACC_VCH_PAY', NULL::varchar, 'SHANGHAI', 'DO-FLOW-2026-SH', 30, 'N', 'Y'),
    ('CP-SH-2026-001', 'DEMO_ACC_VCH_TRANS', NULL::varchar, 'BEIJING', 'DO-FLOW-2026-BJ', 30, 'N', 'Y'),
    ('CP-SH-2026-001', 'DEMO_ACC_RPT_GL', NULL::varchar, 'SHENZHEN', 'DO-FLOW-2026-SZ', 15, 'N', 'Y'),
    ('CP-SH-2026-001', 'DEMO_TAX_VAT', NULL::varchar, 'SHANGHAI', 'DO-FLOW-2026-SH', 10, 'Y', 'Y'),
    -- 深圳华南供应链
    ('CP-SZ-2026-002', 'DEMO_ACC_VCH_RCPT', NULL::varchar, 'SHENZHEN', 'DO-FLOW-2026-SZ', 30, 'N', 'Y'),
    ('CP-SZ-2026-002', 'DEMO_ACC_VCH_PAY', NULL::varchar, 'SHANGHAI', 'DO-FLOW-2026-SH', 30, 'N', 'Y'),
    ('CP-SZ-2026-002', 'DEMO_TAX_VAT', NULL::varchar, 'SHENZHEN', 'DO-FLOW-2026-SZ', 10, 'Y', 'Y'),
    ('CP-SZ-2026-002', 'DEMO_TAX_CIT', NULL::varchar, 'BEIJING', 'DO-FLOW-2026-BJ', 10, 'Y', 'Y'),
    -- 成都西南物流（归档地仍用字典已有城市）
    ('CP-CD-2026-003', 'DEMO_ACC_RPT_SUB', NULL::varchar, 'SHANGHAI', 'DO-FLOW-2026-SH', 15, 'N', 'Y'),
    ('CP-CD-2026-003', 'DEMO_ACC_VCH_PAY', NULL::varchar, 'BEIJING', 'DO-FLOW-2026-BJ', 30, 'N', 'Y'),
    ('CP-CD-2026-003', 'DEMO_ACC_VCH_TRANS', NULL::varchar, 'SHENZHEN', 'DO-FLOW-2026-SZ', 30, 'N', 'Y'),
    -- 杭州电商结算
    ('CP-HZ-2026-004', 'DEMO_ACC_VCH_TRANS', NULL::varchar, 'SHENZHEN', 'DO-FLOW-2026-SZ', 30, 'N', 'Y'),
    ('CP-HZ-2026-004', 'DEMO_ACC_RPT_GL', NULL::varchar, 'BEIJING', 'DO-FLOW-2026-BJ', 15, 'N', 'Y'),
    ('CP-HZ-2026-004', 'DEMO_TAX_VAT', NULL::varchar, 'SHANGHAI', 'DO-FLOW-2026-SH', 10, 'Y', 'Y'),
    -- 各主体兜底：不区分归档地/自定义条件（resolveDefaults wildcard 加分）
    ('CP-SH-2026-001', 'DEMO_ACC_VCH_RCPT', NULL::varchar, NULL::varchar, 'DO-FLOW-2026-SH', 30, 'N', 'Y'),
    ('CP-SZ-2026-002', 'DEMO_ACC_VCH_RCPT', NULL::varchar, NULL::varchar, 'DO-FLOW-2026-SZ', 30, 'N', 'Y'),
    ('CP-CD-2026-003', 'DEMO_ACC_VCH_RCPT', NULL::varchar, NULL::varchar, 'DO-FLOW-2026-SH', 30, 'N', 'Y'),
    ('CP-HZ-2026-004', 'DEMO_ACC_VCH_RCPT', NULL::varchar, NULL::varchar, 'DO-FLOW-2026-SZ', 30, 'N', 'Y')
) AS v(
    company_code,
    module_code,
    cust_mapping_code,
    arch_place_alpha2_code,
    document_organization_code,
    retention_term,
    visible_flag,
    default_flag
)
WHERE EXISTS (SELECT 1 FROM fdc_company_project_t p WHERE p.company_project_code = v.company_code AND p.delete_flag = 'N')
  AND EXISTS (SELECT 1 FROM fdc_business_module_t m WHERE m.module_code = v.module_code AND m.delete_flag = 'N')
  AND EXISTS (SELECT 1 FROM fdc_document_organization_t o WHERE o.document_organization_code = v.document_organization_code AND o.delete_flag = 'N')
  AND NOT EXISTS (
    SELECT 1 FROM fdc_archive_rule_t r
    WHERE r.delete_flag = 'N'
      AND r.company_code = v.company_code
      AND r.module_code = v.module_code
      AND COALESCE(NULLIF(BTRIM(r.cust_mapping_code), ''), '') = COALESCE(NULLIF(BTRIM(v.cust_mapping_code), ''), '')
      AND COALESCE(NULLIF(BTRIM(r.arch_place_alpha2_code), ''), '') = COALESCE(NULLIF(BTRIM(v.arch_place_alpha2_code), ''), '')
  );
