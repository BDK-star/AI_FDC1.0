-- fdc_document_t 扩展字段类型新规格：
-- - attr1  ~ attr50  : 文本（VARCHAR(500)）
-- - attr51 ~ attr80  : 数值（NUMERIC(38,10)）
-- - attr81 ~ attr90  : 日期（DATE）
-- - attr91 ~ attr100 : 日期时间（TIMESTAMP）
--
-- 兼容历史数据：无法安全转换的值置为 NULL，避免迁移失败。

DO $$
DECLARE
    i INTEGER;
    col_name TEXT;
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'fdc_document_t'
    ) THEN
        RETURN;
    END IF;

    -- 1) attr1~attr50 -> VARCHAR(500)
    FOR i IN 1..50 LOOP
        col_name := format('attr%s', i);
        EXECUTE format(
            'ALTER TABLE public.fdc_document_t ALTER COLUMN %I TYPE VARCHAR(500) USING CASE WHEN %I IS NULL THEN NULL ELSE LEFT(%I::text, 500) END',
            col_name, col_name, col_name
        );
    END LOOP;

    -- 2) attr51~attr80 -> NUMERIC(38,10)
    FOR i IN 51..80 LOOP
        col_name := format('attr%s', i);
        EXECUTE format(
            $sql$
            ALTER TABLE public.fdc_document_t
            ALTER COLUMN %I TYPE NUMERIC(38,10)
            USING CASE
                WHEN %I IS NULL THEN NULL
                WHEN btrim(%I::text) = '' THEN NULL
                WHEN regexp_replace(%I::text, ',', '', 'g') ~ '^[+-]?(\d+(\.\d+)?|\.\d+)$'
                    THEN regexp_replace(%I::text, ',', '', 'g')::NUMERIC(38,10)
                ELSE NULL
            END
            $sql$,
            col_name, col_name, col_name, col_name, col_name
        );
    END LOOP;

    -- 3) attr81~attr90 -> DATE
    FOR i IN 81..90 LOOP
        col_name := format('attr%s', i);
        EXECUTE format(
            $sql$
            ALTER TABLE public.fdc_document_t
            ALTER COLUMN %I TYPE DATE
            USING CASE
                WHEN %I IS NULL THEN NULL
                WHEN btrim(%I::text) = '' THEN NULL
                WHEN btrim(%I::text) ~ '^\d{4}-\d{2}-\d{2}$' THEN btrim(%I::text)::DATE
                WHEN btrim(%I::text) ~ '^\d{4}/\d{2}/\d{2}$' THEN to_date(btrim(%I::text), 'YYYY/MM/DD')
                WHEN btrim(%I::text) ~ '^\d{4}-\d{2}$' THEN to_date(btrim(%I::text) || '-01', 'YYYY-MM-DD')
                WHEN btrim(%I::text) ~ '^\d{4}/\d{2}$' THEN to_date(btrim(%I::text) || '/01', 'YYYY/MM/DD')
                WHEN btrim(%I::text) ~ '^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(:\d{2})?$' THEN left(replace(btrim(%I::text), ' ', 'T'), 10)::DATE
                ELSE NULL
            END
            $sql$,
            col_name,
            col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name
        );
    END LOOP;

    -- 4) attr91~attr100 -> TIMESTAMP
    FOR i IN 91..100 LOOP
        col_name := format('attr%s', i);
        EXECUTE format(
            $sql$
            ALTER TABLE public.fdc_document_t
            ALTER COLUMN %I TYPE TIMESTAMP
            USING CASE
                WHEN %I IS NULL THEN NULL
                WHEN btrim(%I::text) = '' THEN NULL
                WHEN btrim(%I::text) ~ '^\d{4}-\d{2}-\d{2}$' THEN (btrim(%I::text) || ' 00:00:00')::TIMESTAMP
                WHEN btrim(%I::text) ~ '^\d{4}/\d{2}/\d{2}$' THEN to_timestamp(btrim(%I::text), 'YYYY/MM/DD')
                WHEN btrim(%I::text) ~ '^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}$' THEN to_timestamp(replace(btrim(%I::text), 'T', ' ') || ':00', 'YYYY-MM-DD HH24:MI:SS')
                WHEN btrim(%I::text) ~ '^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}$' THEN to_timestamp(replace(btrim(%I::text), 'T', ' '), 'YYYY-MM-DD HH24:MI:SS')
                WHEN btrim(%I::text) ~ '^\d{4}/\d{2}/\d{2}[ T]\d{2}:\d{2}$' THEN to_timestamp(replace(btrim(%I::text), 'T', ' ') || ':00', 'YYYY/MM/DD HH24:MI:SS')
                WHEN btrim(%I::text) ~ '^\d{4}/\d{2}/\d{2}[ T]\d{2}:\d{2}:\d{2}$' THEN to_timestamp(replace(btrim(%I::text), 'T', ' '), 'YYYY/MM/DD HH24:MI:SS')
                ELSE NULL
            END
            $sql$,
            col_name,
            col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name, col_name
        );
    END LOOP;
END $$;

