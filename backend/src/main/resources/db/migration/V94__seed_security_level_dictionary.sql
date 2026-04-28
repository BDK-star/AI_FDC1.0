-- 字典管理：密级分类与字典项

INSERT INTO fdc_dict_category_t (
    category_code,
    category_name,
    description,
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
    'SECURITY_LEVEL',
    '密级',
    '文档密级字典',
    'Y',
    'N',
    1,
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP,
    1,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM fdc_dict_category_t
    WHERE category_code = 'SECURITY_LEVEL'
      AND delete_flag = 'N'
);

UPDATE fdc_dict_category_t
SET category_name = '密级',
    description = '文档密级字典',
    enable_flag = 'Y',
    delete_flag = 'N',
    last_updated_by = 1,
    last_update_date = CURRENT_TIMESTAMP
WHERE category_code = 'SECURITY_LEVEL';

INSERT INTO fdc_dict_item_t (
    category_code,
    item_code,
    item_name,
    item_value,
    sort_order,
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
    'SECURITY_LEVEL',
    v.item_code,
    v.item_name,
    v.item_name,
    v.sort_order,
    'Y',
    'N',
    1,
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP,
    1,
    0
FROM (VALUES
    ('SECRET', '秘密', 1),
    ('CONFIDENTIAL', '机密', 2),
    ('TOP_SECRET', '绝密', 3),
    ('INTERNAL_PUBLIC', '内部公开', 4)
) AS v(item_code, item_name, sort_order)
WHERE NOT EXISTS (
    SELECT 1 FROM fdc_dict_item_t i
    WHERE i.category_code = 'SECURITY_LEVEL'
      AND i.item_code = v.item_code
      AND i.delete_flag = 'N'
);

UPDATE fdc_dict_item_t
SET item_name = v.item_name,
    item_value = v.item_name,
    sort_order = v.sort_order,
    enable_flag = 'Y',
    delete_flag = 'N',
    last_updated_by = 1,
    last_update_date = CURRENT_TIMESTAMP
FROM (VALUES
    ('SECRET', '秘密', 1),
    ('CONFIDENTIAL', '机密', 2),
    ('TOP_SECRET', '绝密', 3),
    ('INTERNAL_PUBLIC', '内部公开', 4)
) AS v(item_code, item_name, sort_order)
WHERE fdc_dict_item_t.category_code = 'SECURITY_LEVEL'
  AND fdc_dict_item_t.item_code = v.item_code;
