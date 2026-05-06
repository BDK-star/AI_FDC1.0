-- 将 V104 的 fdc_barcode_t / barcode_* 规范为 fdc_barcode_module_t / barcode_module_*；业务表外键列 barcode_id -> barcode_module_id

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'fdc_barcode_module_t'
    ) THEN
        -- 已是新表结构（例如已执行过本脚本）；列改名与数据迁移由 ELSIF 分支完成
        NULL;
    ELSIF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'fdc_barcode_t'
    ) THEN
        ALTER TABLE fdc_business_module_t DROP CONSTRAINT IF EXISTS fk_fdc_business_module_t_barcode_id;

        IF EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'fdc_business_module_t' AND column_name = 'barcode_id'
        ) THEN
            ALTER TABLE fdc_business_module_t RENAME COLUMN barcode_id TO barcode_module_id;
        END IF;

        ALTER TABLE fdc_barcode_t RENAME TO fdc_barcode_module_t;
        ALTER TABLE fdc_barcode_module_t RENAME COLUMN barcode_id TO barcode_module_id;
        ALTER TABLE fdc_barcode_module_t RENAME COLUMN barcode_code TO barcode_module_code;
        ALTER TABLE fdc_barcode_module_t RENAME COLUMN barcode_name TO barcode_module_name;

        DROP INDEX IF EXISTS uk_fdc_barcode_t_code;
        DROP INDEX IF EXISTS idx_fdc_business_module_t_barcode_id;
    ELSE
        -- 无旧表（例如跳过 V104）：直接建新表
        CREATE TABLE IF NOT EXISTS fdc_barcode_module_t (
            barcode_module_id BIGSERIAL PRIMARY KEY,
            barcode_module_code VARCHAR(64) NOT NULL,
            barcode_module_name VARCHAR(255) NOT NULL,
            description VARCHAR(500),
            enable_flag CHAR(1) NOT NULL DEFAULT 'Y',
            delete_flag CHAR(1) NOT NULL DEFAULT 'N',
            created_by BIGINT NOT NULL DEFAULT 1,
            creation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            last_updated_by BIGINT NOT NULL DEFAULT 1,
            last_update_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );

        ALTER TABLE fdc_business_module_t ADD COLUMN IF NOT EXISTS barcode_module_id BIGINT;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_fdc_barcode_module_t_code
    ON fdc_barcode_module_t (barcode_module_code)
    WHERE delete_flag = 'N';

CREATE INDEX IF NOT EXISTS idx_fdc_business_module_t_barcode_module_id
    ON fdc_business_module_t (barcode_module_id)
    WHERE barcode_module_id IS NOT NULL AND delete_flag = 'N';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_fdc_business_module_t_barcode_module_id'
    ) THEN
        ALTER TABLE fdc_business_module_t
            ADD CONSTRAINT fk_fdc_business_module_t_barcode_module_id
            FOREIGN KEY (barcode_module_id) REFERENCES fdc_barcode_module_t (barcode_module_id);
    END IF;
END $$;
