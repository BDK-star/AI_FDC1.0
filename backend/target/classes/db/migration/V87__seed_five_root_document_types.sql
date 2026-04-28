-- Five root document types (business modules) for FDC.
-- V71/V72 target legacy columns (type_code) and are skipped when only doc_type_code exists (V36+).

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'fdc_document_type_t'
          AND column_name = 'doc_type_code'
    ) THEN
        INSERT INTO fdc_document_type_t (
            doc_type_code,
            doc_type_description,
            enable_flag,
            delete_flag,
            created_by,
            last_updated_by,
            last_update_date,
            tenantid,
            last_update_version
        )
        VALUES
            ('FIN_ACC', '会计文档', 'Y', 'N', 1, 1, CURRENT_TIMESTAMP, 1, 0),
            ('FIN_TAX', '税务文档', 'Y', 'N', 1, 1, CURRENT_TIMESTAMP, 1, 0),
            ('FIN_FUND', '资金文档', 'Y', 'N', 1, 1, CURRENT_TIMESTAMP, 1, 0),
            ('FIN_OTHER', '其他财经文档', 'Y', 'N', 1, 1, CURRENT_TIMESTAMP, 1, 0),
            ('NON_FIN', '非财经文档', 'Y', 'N', 1, 1, CURRENT_TIMESTAMP, 1, 0)
        ON CONFLICT (tenantid, doc_type_code) DO UPDATE SET
            doc_type_description = EXCLUDED.doc_type_description,
            enable_flag = 'Y',
            delete_flag = 'N',
            last_updated_by = 1,
            last_update_date = CURRENT_TIMESTAMP;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'fdc_business_module_t'
    ) THEN
        UPDATE fdc_business_module_t AS m
        SET module_name = v.module_name,
            description = v.module_name,
            last_updated_by = 1,
            last_update_date = CURRENT_TIMESTAMP
        FROM (VALUES
            ('FIN_ACC', '会计文档'),
            ('FIN_TAX', '税务文档'),
            ('FIN_FUND', '资金文档'),
            ('FIN_OTHER', '其他财经文档'),
            ('NON_FIN', '非财经文档')
        ) AS v(code, module_name)
        WHERE m.delete_flag = 'N'
          AND m.module_code = v.code;

        INSERT INTO fdc_business_module_t (
            module_code,
            module_name,
            parent_code,
            level_num,
            ancestor_path,
            enabled_flag,
            security_level,
            integration_type,
            description,
            remark,
            sort_order,
            delete_flag,
            created_by,
            creation_date,
            last_updated_by,
            last_update_date
        )
        SELECT v.code,
               v.module_name,
               NULL,
               1,
               '',
               'Y',
               '公开',
               '不集成',
               v.module_name,
               NULL,
               v.ord,
               'N',
               1,
               CURRENT_TIMESTAMP,
               1,
               CURRENT_TIMESTAMP
        FROM (VALUES
            ('FIN_ACC', '会计文档', 1),
            ('FIN_TAX', '税务文档', 2),
            ('FIN_FUND', '资金文档', 3),
            ('FIN_OTHER', '其他财经文档', 4),
            ('NON_FIN', '非财经文档', 5)
        ) AS v(code, module_name, ord)
        WHERE NOT EXISTS (
            SELECT 1
            FROM fdc_business_module_t b
            WHERE b.module_code = v.code
              AND b.delete_flag = 'N'
        );
    END IF;
END $$;
