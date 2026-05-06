-- 修正 V98 的唯一约束粒度：
-- 业务要求允许不同业务模块复用同一扩展字段（共享字段名/字段编码语义），
-- 因此唯一性应限制在“同一业务模块内”，而非全局。

DROP INDEX IF EXISTS public.uq_fdc_bm_ext_field_scope_attr_active;

CREATE UNIQUE INDEX IF NOT EXISTS uq_fdc_bm_ext_field_module_scope_attr_active
    ON public.fdc_business_module_ext_field_t (module_code, field_scope, ext_attribute)
    WHERE delete_flag = 'N';
