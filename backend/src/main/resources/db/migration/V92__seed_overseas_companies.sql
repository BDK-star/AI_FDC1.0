-- 海外主体：公司信息 + 公司项目（country_code 使用 ISO 3166-1 alpha-2，与 CN 种子一致）

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
    ('CI-US-001', '美国特拉华控股有限责任公司', '北美', 'Wilmington, DE', '美国', '北美投资与合规主体', '海外,北美', 'Y', 'N', 1, 1),
    ('CI-SG-001', '新加坡亚太财务中心有限公司', '东南亚', '新加坡滨海湾', '新加坡', '区域财资与共享服务', '海外,东南亚', 'Y', 'N', 1, 1),
    ('CI-DE-001', '德国慕尼黑技术研发有限公司', '西欧', '慕尼黑', '德国', '欧洲研发与采购协调', '海外,欧洲', 'Y', 'N', 1, 1),
    ('CI-JP-001', '日本东京贸易株式会社', '东亚', '东京港区', '日本', '东亚贸易与结算', '海外,东亚', 'Y', 'N', 1, 1),
    ('CI-GB-001', '英国伦敦贸易有限责任公司', '西欧', '伦敦', '英国', '欧洲贸易与法务协调', '海外,英国', 'Y', 'N', 1, 1)
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
    ('CP-US-2026-101', '美国特拉华控股（档案主体）', 'US', '北美', '海外'),
    ('CP-SG-2026-102', '新加坡亚太财务中心（档案主体）', 'SG', '东南亚', '海外'),
    ('CP-DE-2026-103', '德国慕尼黑研发（档案主体）', 'DE', '西欧', '海外'),
    ('CP-JP-2026-104', '日本东京贸易（档案主体）', 'JP', '东亚', '海外'),
    ('CP-GB-2026-105', '英国伦敦贸易（档案主体）', 'GB', '西欧', '海外')
) AS v(code, name, country, area, tag)
WHERE NOT EXISTS (
    SELECT 1 FROM fdc_company_project_t p
    WHERE p.company_project_code = v.code AND p.delete_flag = 'N'
);
