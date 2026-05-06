-- 签收时间、签收人（文档实物签收，与核销 verified_* 区分）

ALTER TABLE fdc_document_t ADD COLUMN IF NOT EXISTS sign_time TIMESTAMP;
ALTER TABLE fdc_document_t ADD COLUMN IF NOT EXISTS signed_by BIGINT;

COMMENT ON COLUMN fdc_document_t.sign_time IS '签收时间';
COMMENT ON COLUMN fdc_document_t.signed_by IS '签收人 tpl_user_t.user_id';
