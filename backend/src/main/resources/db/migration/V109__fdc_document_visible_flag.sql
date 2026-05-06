-- 是否可见：由 attr1 迁至专用列 visible_flag（单字符：0=不可见，1=可见）。
-- PostgreSQL 使用 VARCHAR(1)；Oracle 侧等价 NVARCHAR2(1)。

ALTER TABLE fdc_document_t ADD COLUMN IF NOT EXISTS visible_flag VARCHAR(1);

UPDATE fdc_document_t
SET visible_flag = CASE
        WHEN trim(coalesce(attr1, '')) IN ('否', 'N', 'n', '0', '不可见') THEN '0'
        ELSE '1'
    END
WHERE visible_flag IS NULL;

ALTER TABLE fdc_document_t ALTER COLUMN visible_flag SET DEFAULT '1';

UPDATE fdc_document_t SET visible_flag = '1' WHERE visible_flag IS NULL OR trim(coalesce(visible_flag, '')) = '';

ALTER TABLE fdc_document_t ALTER COLUMN visible_flag SET NOT NULL;

-- 历史可见性数据已迁移，释放 attr1（原为「是否可见」专用）
UPDATE fdc_document_t SET attr1 = NULL WHERE attr1 IS NOT NULL;
