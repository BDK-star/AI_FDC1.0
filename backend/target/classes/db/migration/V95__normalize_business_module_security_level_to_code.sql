-- 业务模块密级统一存编码：将历史中文值/旧编码迁移为字典编码（SECURITY_LEVEL）

-- 1) 与当前字典值进行名称匹配（如：秘密、机密、绝密、内部公开）
UPDATE fdc_business_module_t AS b
SET security_level = d.item_code,
    last_updated_by = 1,
    last_update_date = CURRENT_TIMESTAMP
FROM fdc_dict_item_t d
WHERE b.delete_flag = 'N'
  AND d.delete_flag = 'N'
  AND d.enable_flag = 'Y'
  AND d.category_code = 'SECURITY_LEVEL'
  AND btrim(b.security_level) = btrim(d.item_name);

-- 2) 对常见历史值做兜底映射
UPDATE fdc_business_module_t
SET security_level = 'INTERNAL_PUBLIC',
    last_updated_by = 1,
    last_update_date = CURRENT_TIMESTAMP
WHERE delete_flag = 'N'
  AND (
    security_level IS NULL
    OR btrim(security_level) = ''
    OR upper(btrim(security_level)) IN ('PUBLIC', 'INTERNAL')
    OR btrim(security_level) IN ('公开', '内部')
  );

-- 3) 已是小写英文时统一转大写（例如 secret -> SECRET）
UPDATE fdc_business_module_t
SET security_level = upper(btrim(security_level)),
    last_updated_by = 1,
    last_update_date = CURRENT_TIMESTAMP
WHERE delete_flag = 'N'
  AND security_level ~ '^[a-z_]+$';
