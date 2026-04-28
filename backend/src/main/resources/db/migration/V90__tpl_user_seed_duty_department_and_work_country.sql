-- 为 V89 种子用户补全责任部门、工作国家编码（产生地/工作地）
-- work_country_code 与 fdc_company_project_t.country_code 等场景一致，使用 ISO 3166-1 alpha-2（如 CN）

ALTER TABLE IF EXISTS public.tpl_user_t ADD COLUMN IF NOT EXISTS duty_department VARCHAR(200);
ALTER TABLE IF EXISTS public.tpl_user_t ADD COLUMN IF NOT EXISTS work_country_code VARCHAR(32);

UPDATE tpl_user_t AS u
SET duty_department = v.dept,
    work_country_code = v.country,
    last_updated_by = 1,
    last_update_date = CURRENT_TIMESTAMP
FROM (VALUES
    ('10000101', '财务部', 'CN'),
    ('10000102', '人力资源部', 'CN'),
    ('10000103', '法务部', 'CN'),
    ('10000104', '信息技术部', 'CN'),
    ('10000105', '审计部', 'CN'),
    ('10000106', '行政管理部', 'CN'),
    ('10000107', '采购部', 'CN'),
    ('10000108', '档案管理部', 'CN'),
    ('10000109', '运营管理部', 'CN'),
    ('10000110', '总经理办公室', 'CN')
) AS v(employee_no, dept, country)
WHERE u.delete_flag = 'N'
  AND u.employee_no = v.employee_no;
