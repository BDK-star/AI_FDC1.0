-- 条码模块主数据；业务模块通过 barcode_id 映射（多对一）
CREATE TABLE IF NOT EXISTS fdc_barcode_t (
    barcode_id BIGSERIAL PRIMARY KEY,
    barcode_code VARCHAR(64) NOT NULL,
    barcode_name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
    delete_flag CHAR(1) NOT NULL DEFAULT 'N',
    created_by BIGINT NOT NULL DEFAULT 1,
    creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_updated_by BIGINT NOT NULL DEFAULT 1,
    last_update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_fdc_barcode_t_code
    ON fdc_barcode_t (barcode_code)
    WHERE delete_flag = 'N';

ALTER TABLE fdc_business_module_t
    ADD COLUMN IF NOT EXISTS barcode_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_fdc_business_module_t_barcode_id'
    ) THEN
        ALTER TABLE fdc_business_module_t
            ADD CONSTRAINT fk_fdc_business_module_t_barcode_id
            FOREIGN KEY (barcode_id) REFERENCES fdc_barcode_t (barcode_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_fdc_business_module_t_barcode_id
    ON fdc_business_module_t (barcode_id)
    WHERE barcode_id IS NOT NULL AND delete_flag = 'N';
