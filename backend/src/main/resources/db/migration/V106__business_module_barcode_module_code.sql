-- 业务模块关联条码模块改为存储编码（barcode_module_code），不再使用外键 barcode_module_id

ALTER TABLE fdc_business_module_t ADD COLUMN IF NOT EXISTS barcode_module_code VARCHAR(16);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'fdc_business_module_t'
          AND column_name = 'barcode_module_id'
    ) THEN
        UPDATE fdc_business_module_t bm
        SET barcode_module_code = UPPER(TRIM(b.barcode_module_code))
        FROM fdc_barcode_module_t b
        WHERE bm.barcode_module_id IS NOT NULL
          AND b.barcode_module_id = bm.barcode_module_id
          AND COALESCE(bm.delete_flag, 'N') = 'N';
    END IF;
END $$;

ALTER TABLE fdc_business_module_t DROP CONSTRAINT IF EXISTS fk_fdc_business_module_t_barcode_module_id;

DROP INDEX IF EXISTS idx_fdc_business_module_t_barcode_module_id;

ALTER TABLE fdc_business_module_t DROP COLUMN IF EXISTS barcode_module_id;

CREATE INDEX IF NOT EXISTS idx_fdc_business_module_t_barcode_module_code
    ON fdc_business_module_t (barcode_module_code)
    WHERE barcode_module_code IS NOT NULL AND COALESCE(delete_flag, 'N') = 'N';

COMMENT ON COLUMN fdc_business_module_t.barcode_module_code IS '关联条码模块编码（对应 fdc_barcode_module_t.barcode_module_code），逻辑关联非数据库外键';
