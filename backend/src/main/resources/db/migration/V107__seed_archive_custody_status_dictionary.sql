-- 字典：实物保管状态（与 lifecycle 解耦，不含「已归档/未归档」）

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
    'ARCHIVE_CUSTODY_STATUS',
    '保管状态',
    '档案实物保管状态（在库/借出/封存/销毁），可在字典管理中维护',
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
    WHERE category_code = 'ARCHIVE_CUSTODY_STATUS'
      AND delete_flag = 'N'
);

UPDATE fdc_dict_category_t
SET category_name = '保管状态',
    description = '档案实物保管状态（在库/借出/封存/销毁），可在字典管理中维护',
    enable_flag = 'Y',
    delete_flag = 'N',
    last_updated_by = 1,
    last_update_date = CURRENT_TIMESTAMP
WHERE category_code = 'ARCHIVE_CUSTODY_STATUS';

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
    'ARCHIVE_CUSTODY_STATUS',
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
    ('IN_STORAGE', '在库', 1),
    ('LENT_OUT', '借出', 2),
    ('SEALED', '封存', 3),
    ('DESTROYED', '销毁', 4)
) AS v(item_code, item_name, sort_order)
WHERE NOT EXISTS (
    SELECT 1 FROM fdc_dict_item_t i
    WHERE i.category_code = 'ARCHIVE_CUSTODY_STATUS'
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
    ('IN_STORAGE', '在库', 1),
    ('LENT_OUT', '借出', 2),
    ('SEALED', '封存', 3),
    ('DESTROYED', '销毁', 4)
) AS v(item_code, item_name, sort_order)
WHERE fdc_dict_item_t.category_code = 'ARCHIVE_CUSTODY_STATUS'
  AND fdc_dict_item_t.item_code = v.item_code;

-- 历史数据：曾误用生命周期码写入 custody_status 时，统一为默认「在库」
UPDATE fdc_document_t
SET custody_status = 'IN_STORAGE',
    last_updated_by = coalesce(last_updated_by, 1),
    last_update_date = CURRENT_TIMESTAMP
WHERE coalesce(delete_flag, 0) = 0
  AND lower(trim(custody_status)) IN ('unarchived', 'archived');
