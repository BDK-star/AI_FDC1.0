-- Demo companies (fdc_company_info_t + fdc_company_project_t) and a multi-level business module tree
-- under FIN_ACC / FIN_TAX. Module codes use DEMO_* prefix so they do not collide with document-type sync.

-- ========== 公司信息（配置中心 / 公司档案）==========
INSERT INTO fdc_company_info_t (
    company_code,
    company_name,
    region,
    representative_office,
    country,
    description,
    tags,
    enabled_flag,
    delete_flag,
    created_by,
    last_updated_by
)
SELECT v.*
FROM (VALUES
    ('CI-HQ-001', '华东财务控股有限公司', '华东', '上海代表处', '中国', '集团总部与共享中心', '集团,总部', 'Y', 'N', 1, 1),
    ('CI-SH-002', '上海亚太实业有限公司', '华东', '上海总部', '中国', '贸易与结算主体', '贸易,亚太', 'Y', 'N', 1, 1),
    ('CI-BJ-003', '北京华北运营中心', '华北', '北京办公室', '中国', '北方区域运营', '运营,华北', 'Y', 'N', 1, 1),
    ('CI-SZ-004', '深圳华南供应链有限公司', '华南', '深圳前海', '中国', '供应链与仓储', '供应链', 'Y', 'N', 1, 1)
) AS v(
    company_code,
    company_name,
    region,
    representative_office,
    country,
    description,
    tags,
    enabled_flag,
    delete_flag,
    created_by,
    last_updated_by
)
WHERE NOT EXISTS (
    SELECT 1 FROM fdc_company_info_t c
    WHERE c.company_code = v.company_code AND c.delete_flag = 'N'
);

-- ========== 公司项目（档案/移交等下拉常用）==========
INSERT INTO fdc_company_project_t (
    company_project_code,
    company_project_name,
    country_code,
    management_area,
    company_tag,
    enable_flag,
    delete_flag,
    created_by,
    creation_date,
    last_updated_by,
    last_update_date,
    tenantid,
    last_update_version
)
SELECT
    v.code,
    v.name,
    v.country,
    v.area,
    v.tag,
    'Y',
    'N',
    1,
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP,
    1,
    0
FROM (VALUES
    ('CP-SH-2026-001', '上海亚太实业（档案主体）', 'CN', '华东', '亚太'),
    ('CP-SZ-2026-002', '深圳华南供应链（档案主体）', 'CN', '华南', '供应链'),
    ('CP-CD-2026-003', '成都西南物流中心', 'CN', '西南', '物流'),
    ('CP-HZ-2026-004', '杭州电商结算中心', 'CN', '华东', '电商')
) AS v(code, name, country, area, tag)
WHERE NOT EXISTS (
    SELECT 1 FROM fdc_company_project_t p
    WHERE p.company_project_code = v.code AND p.delete_flag = 'N'
);

-- ========== 多层级业务模块（挂在已有根节点 FIN_ACC、FIN_TAX 下）==========
-- Level 2 under FIN_ACC
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO_ACC_GRP_VCH', '凭证与单据', 'FIN_ACC', 2, 'FIN_ACC', 'Y', '公开', '不集成',
       '演示：会计类二级模块', NULL, 10, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_ACC' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO_ACC_GRP_VCH' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO_ACC_GRP_BOOK', '账簿与报表', 'FIN_ACC', 2, 'FIN_ACC', 'Y', '公开', '不集成',
       '演示：会计类二级模块', NULL, 11, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_ACC' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO_ACC_GRP_BOOK' AND x.delete_flag = 'N');

-- Level 3 under DEMO_ACC_GRP_VCH
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO_ACC_VCH_RCPT', '收款凭证', 'DEMO_ACC_GRP_VCH', 3, 'FIN_ACC/DEMO_ACC_GRP_VCH', 'Y', '公开', '不集成',
       '演示：三级模块', NULL, 1, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t p WHERE p.module_code = 'DEMO_ACC_GRP_VCH' AND p.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO_ACC_VCH_RCPT' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO_ACC_VCH_PAY', '付款凭证', 'DEMO_ACC_GRP_VCH', 3, 'FIN_ACC/DEMO_ACC_GRP_VCH', 'Y', '公开', '不集成',
       '演示：三级模块', NULL, 2, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t p WHERE p.module_code = 'DEMO_ACC_GRP_VCH' AND p.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO_ACC_VCH_PAY' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO_ACC_VCH_TRANS', '转账凭证', 'DEMO_ACC_GRP_VCH', 3, 'FIN_ACC/DEMO_ACC_GRP_VCH', 'Y', '公开', '不集成',
       '演示：三级模块', NULL, 3, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t p WHERE p.module_code = 'DEMO_ACC_GRP_VCH' AND p.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO_ACC_VCH_TRANS' AND x.delete_flag = 'N');

-- Level 3 under DEMO_ACC_GRP_BOOK
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO_ACC_RPT_GL', '总账报表', 'DEMO_ACC_GRP_BOOK', 3, 'FIN_ACC/DEMO_ACC_GRP_BOOK', 'Y', '公开', '不集成',
       '演示：三级模块', NULL, 1, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t p WHERE p.module_code = 'DEMO_ACC_GRP_BOOK' AND p.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO_ACC_RPT_GL' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO_ACC_RPT_SUB', '明细账报表', 'DEMO_ACC_GRP_BOOK', 3, 'FIN_ACC/DEMO_ACC_GRP_BOOK', 'Y', '公开', '不集成',
       '演示：三级模块', NULL, 2, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t p WHERE p.module_code = 'DEMO_ACC_GRP_BOOK' AND p.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO_ACC_RPT_SUB' AND x.delete_flag = 'N');

-- Level 2 / 3 under FIN_TAX
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO_TAX_GRP_FILING', '纳税申报', 'FIN_TAX', 2, 'FIN_TAX', 'Y', '公开', '不集成',
       '演示：税务二级模块', NULL, 10, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_TAX' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO_TAX_GRP_FILING' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO_TAX_VAT', '增值税申报', 'DEMO_TAX_GRP_FILING', 3, 'FIN_TAX/DEMO_TAX_GRP_FILING', 'Y', '公开', '不集成',
       '演示：税务三级模块', NULL, 1, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t p WHERE p.module_code = 'DEMO_TAX_GRP_FILING' AND p.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO_TAX_VAT' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO_TAX_CIT', '企业所得税申报', 'DEMO_TAX_GRP_FILING', 3, 'FIN_TAX/DEMO_TAX_GRP_FILING', 'Y', '公开', '不集成',
       '演示：税务三级模块', NULL, 2, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t p WHERE p.module_code = 'DEMO_TAX_GRP_FILING' AND p.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO_TAX_CIT' AND x.delete_flag = 'N');
