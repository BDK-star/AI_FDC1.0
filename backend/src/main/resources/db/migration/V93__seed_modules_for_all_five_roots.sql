-- 为五个根节点业务模块（FIN_ACC/FIN_TAX/FIN_FUND/FIN_OTHER/NON_FIN）补充示例多层级子模块。

-- ---------- Level 2 under FIN_ACC ----------
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_ACC_COMPLIANCE', '会计合规管理', 'FIN_ACC', 2, 'FIN_ACC', 'Y', '公开', '不集成',
       '会计文档二级模块', NULL, 20, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_ACC' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_ACC_COMPLIANCE' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_ACC_CLOSE', '月结与年结', 'FIN_ACC', 2, 'FIN_ACC', 'Y', '公开', '不集成',
       '会计文档二级模块', NULL, 21, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_ACC' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_ACC_CLOSE' AND x.delete_flag = 'N');

-- ---------- Level 3 under DEMO2_ACC_COMPLIANCE ----------
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_ACC_COMPLIANCE_POLICY', '会计制度文件', 'DEMO2_ACC_COMPLIANCE', 3, 'FIN_ACC/DEMO2_ACC_COMPLIANCE',
       'Y', '公开', '不集成', '会计文档三级模块', NULL, 1, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t p WHERE p.module_code = 'DEMO2_ACC_COMPLIANCE' AND p.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_ACC_COMPLIANCE_POLICY' AND x.delete_flag = 'N');

-- ---------- Level 2 under FIN_TAX ----------
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_TAX_INSPECTION', '税务稽核应对', 'FIN_TAX', 2, 'FIN_TAX', 'Y', '公开', '不集成',
       '税务文档二级模块', NULL, 20, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_TAX' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_TAX_INSPECTION' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_TAX_PLAN', '税务筹划', 'FIN_TAX', 2, 'FIN_TAX', 'Y', '公开', '不集成',
       '税务文档二级模块', NULL, 21, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_TAX' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_TAX_PLAN' AND x.delete_flag = 'N');

-- ---------- Level 2 under FIN_FUND ----------
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_FUND_CASH', '现金流管理', 'FIN_FUND', 2, 'FIN_FUND', 'Y', '公开', '不集成',
       '资金文档二级模块', NULL, 20, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_FUND' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_FUND_CASH' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_FUND_BANK', '银行授信与账户', 'FIN_FUND', 2, 'FIN_FUND', 'Y', '公开', '不集成',
       '资金文档二级模块', NULL, 21, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_FUND' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_FUND_BANK' AND x.delete_flag = 'N');

-- ---------- Level 3 under DEMO2_FUND_CASH ----------
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_FUND_CASH_FORECAST', '资金预测', 'DEMO2_FUND_CASH', 3, 'FIN_FUND/DEMO2_FUND_CASH',
       'Y', '公开', '不集成', '资金文档三级模块', NULL, 1, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t p WHERE p.module_code = 'DEMO2_FUND_CASH' AND p.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_FUND_CASH_FORECAST' AND x.delete_flag = 'N');

-- ---------- Level 2 under FIN_OTHER ----------
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_OTHER_BUDGET', '预算与经营分析', 'FIN_OTHER', 2, 'FIN_OTHER', 'Y', '公开', '不集成',
       '其他财经文档二级模块', NULL, 20, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_OTHER' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_OTHER_BUDGET' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_OTHER_INTERNAL_CTRL', '内控与风险评估', 'FIN_OTHER', 2, 'FIN_OTHER', 'Y', '公开', '不集成',
       '其他财经文档二级模块', NULL, 21, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'FIN_OTHER' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_OTHER_INTERNAL_CTRL' AND x.delete_flag = 'N');

-- ---------- Level 2 under NON_FIN ----------
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_NONFIN_HR', '人力与组织文档', 'NON_FIN', 2, 'NON_FIN', 'Y', '公开', '不集成',
       '非财经文档二级模块', NULL, 20, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'NON_FIN' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_NONFIN_HR' AND x.delete_flag = 'N');

INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_NONFIN_LEGAL', '法务与合规文档', 'NON_FIN', 2, 'NON_FIN', 'Y', '公开', '不集成',
       '非财经文档二级模块', NULL, 21, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t r WHERE r.module_code = 'NON_FIN' AND r.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_NONFIN_LEGAL' AND x.delete_flag = 'N');

-- ---------- Level 3 under DEMO2_NONFIN_HR ----------
INSERT INTO fdc_business_module_t (
    module_code, module_name, parent_code, level_num, ancestor_path,
    enabled_flag, security_level, integration_type, description, remark, sort_order,
    delete_flag, created_by, creation_date, last_updated_by, last_update_date
)
SELECT 'DEMO2_NONFIN_HR_CONTRACT', '劳动合同与入离职', 'DEMO2_NONFIN_HR', 3, 'NON_FIN/DEMO2_NONFIN_HR',
       'Y', '公开', '不集成', '非财经文档三级模块', NULL, 1, 'N', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP
WHERE EXISTS (SELECT 1 FROM fdc_business_module_t p WHERE p.module_code = 'DEMO2_NONFIN_HR' AND p.delete_flag = 'N')
  AND NOT EXISTS (SELECT 1 FROM fdc_business_module_t x WHERE x.module_code = 'DEMO2_NONFIN_HR_CONTRACT' AND x.delete_flag = 'N');
