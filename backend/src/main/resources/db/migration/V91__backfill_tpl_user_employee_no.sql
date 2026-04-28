-- 历史用户（如 admin）未维护 employee_no 时，前端工号列会为空；按现有最大 8 位工号顺延补全。

DO $$
DECLARE
    next_no BIGINT;
    r RECORD;
BEGIN
    SELECT COALESCE(MAX(employee_no::bigint), 10000000)
    INTO next_no
    FROM tpl_user_t
    WHERE delete_flag = 'N'
      AND employee_no IS NOT NULL
      AND btrim(employee_no) <> ''
      AND employee_no ~ '^[0-9]{8}$';

    FOR r IN
        SELECT user_id
        FROM tpl_user_t
        WHERE delete_flag = 'N'
          AND (employee_no IS NULL OR btrim(employee_no) = '')
        ORDER BY user_id
    LOOP
        next_no := next_no + 1;
        IF next_no > 99999999 THEN
            RAISE EXCEPTION 'employee_no would exceed 8 digits';
        END IF;
        UPDATE tpl_user_t
        SET employee_no = lpad(next_no::text, 8, '0'),
            last_updated_by = 1,
            last_update_date = CURRENT_TIMESTAMP
        WHERE user_id = r.user_id
          AND delete_flag = 'N';
    END LOOP;
END $$;
