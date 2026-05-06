-- 同一扩展字段语义（field_scope + ext_attribute）只允许有一条有效定义（delete_flag='N'）。
-- 迁移策略：
-- 1) 若历史上存在重复有效记录，保留最早一条，其余标记删除；
-- 2) 为有效记录建立唯一部分索引，防止后续再次写入冲突语义。

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'fdc_business_module_ext_field_t'
    ) THEN
        RETURN;
    END IF;

    WITH duplicated AS (
        SELECT
            field_id,
            ROW_NUMBER() OVER (
                PARTITION BY field_scope, ext_attribute
                ORDER BY creation_date NULLS LAST, field_id
            ) AS rn
        FROM public.fdc_business_module_ext_field_t
        WHERE delete_flag = 'N'
    )
    UPDATE public.fdc_business_module_ext_field_t t
    SET
        delete_flag = 'Y',
        last_update_date = NOW()
    FROM duplicated d
    WHERE t.field_id = d.field_id
      AND d.rn > 1;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_fdc_bm_ext_field_scope_attr_active
    ON public.fdc_business_module_ext_field_t (field_scope, ext_attribute)
    WHERE delete_flag = 'N';
