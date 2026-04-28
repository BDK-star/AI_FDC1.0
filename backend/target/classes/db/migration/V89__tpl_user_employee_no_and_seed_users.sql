-- 员工工号（8 位数字）；user_name 存人名全拼（小写、无分隔）。
ALTER TABLE tpl_user_t ADD COLUMN IF NOT EXISTS employee_no VARCHAR(8);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'ck_tpl_user_t_employee_no_format'
    ) THEN
        ALTER TABLE tpl_user_t ADD CONSTRAINT ck_tpl_user_t_employee_no_format
            CHECK (employee_no IS NULL OR employee_no ~ '^[0-9]{8}$');
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tpl_user_t_employee_no_active
    ON tpl_user_t (employee_no)
    WHERE delete_flag = 'N' AND employee_no IS NOT NULL;

-- 10 名演示用户：user_name = 拼音，employee_no = 工号（与中文姓名对应见注释）
INSERT INTO tpl_user_t (
    user_name,
    employee_no,
    email,
    phone,
    status,
    created_by,
    creation_date,
    last_updated_by,
    last_update_date,
    delete_flag
)
SELECT v.user_name,
       v.employee_no,
       v.user_name || '@company.local',
       NULL,
       'ACTIVE',
       1,
       CURRENT_TIMESTAMP,
       1,
       CURRENT_TIMESTAMP,
       'N'
FROM (VALUES
    ('zhangwei', '10000101'),   -- 张伟
    ('wangfang', '10000102'),   -- 王芳
    ('lina', '10000103'),       -- 李娜
    ('liuyang', '10000104'),    -- 刘洋
    ('chenjing', '10000105'),   -- 陈静
    ('yanglei', '10000106'),    -- 杨磊
    ('zhaomin', '10000107'),    -- 赵敏
    ('huangjun', '10000108'),   -- 黄军
    ('zhoujie', '10000109'),    -- 周杰
    ('wuting', '10000110')      -- 吴婷
) AS v(user_name, employee_no)
WHERE NOT EXISTS (
    SELECT 1 FROM tpl_user_t u
    WHERE u.delete_flag = 'N'
      AND (u.user_name = v.user_name OR u.employee_no = v.employee_no)
);
