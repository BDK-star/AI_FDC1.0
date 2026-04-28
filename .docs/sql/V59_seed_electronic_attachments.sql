-- 为“载体类型=电子件”的文档补充可下载/预览的示例附件（幂等）
-- 说明：
-- 1) 使用仓库内已存在文件作为 file_path，避免外部存储依赖；
-- 2) 每个电子件文档补充 2 个附件（PDF 说明 + 预览样例）；
-- 3) 若文档已有关联附件，则跳过。

with electronic_docs as (
    select d.doc_id
      from fdc_document_t d
     where coalesce(d.delete_flag, 0) = 0
       and d.carrier_type = 'ELECTRONIC'
       and not exists (
            select 1
              from fdc_document_attach_t da
             where da.document_id = d.doc_id
               and coalesce(da.delete_flag, 'N') = 'N'
       )
),
seed_templates as (
    select '电子附件-凭证说明.pdf'::varchar as file_name,
           '.docs/assets/electronic_voucher_sample.pdf'::varchar as file_path,
           'application/pdf'::varchar as file_type,
           20480::bigint as file_size,
           'SCAN'::varchar as att_type
    union all
    select '电子附件-预览样例.html',
           '.docs/features/F03/reference_html/pages/attachment_preview.html',
           'text/html',
           40960,
           'PREVIEW'
),
seed_rows as (
    select d.doc_id,
           t.file_name,
           t.file_path,
           t.file_type,
           t.file_size,
           t.att_type,
           row_number() over(order by d.doc_id, t.file_name) as rn
      from electronic_docs d
      cross join seed_templates t
),
max_ids as (
    select coalesce((select max(file_id) from fdc_file_t), 0) as max_file_id,
           coalesce((select max(document_attach_id) from fdc_document_attach_t), 0) as max_attach_id
),
ins_files as (
    insert into fdc_file_t (
        file_id, file_name, file_path, file_size, file_type, source_system, storage_platform, file_md5,
        enable_flag, delete_flag, created_by, creation_date
    )
    select m.max_file_id + s.rn,
           s.file_name,
           s.file_path,
           s.file_size,
           s.file_type,
           'PORTAL',
           'LOCAL',
           null,
           'Y',
           'N',
           1,
           current_timestamp
      from seed_rows s
      cross join max_ids m
    returning file_id, file_name
)
insert into fdc_document_attach_t (
    document_attach_id, tenantid, document_id, file_id, attach_category, att_type,
    enable_flag, delete_flag, created_by, creation_date, last_updated_by, last_update_date, last_update_version
)
select m.max_attach_id + s.rn,
       1,
       s.doc_id,
       m.max_file_id + s.rn,
       'ELECTRONIC',
       s.att_type,
       'Y',
       'N',
       1,
       current_timestamp,
       1,
       current_timestamp,
       0
  from seed_rows s
  cross join max_ids m;

-- 修正序列，避免后续通过默认序列插入时撞主键
select setval(
  pg_get_serial_sequence('fdc_file_t', 'file_id'),
  greatest(coalesce((select max(file_id) from fdc_file_t), 0), 1),
  true
);

select setval(
  pg_get_serial_sequence('fdc_document_attach_t', 'document_attach_id'),
  greatest(coalesce((select max(document_attach_id) from fdc_document_attach_t), 0), 1),
  true
);
