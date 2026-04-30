DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'fdc_archive_create_session_t'
          AND column_name = 'document_type_code_guess'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'fdc_archive_create_session_t'
          AND column_name = 'busi_module_code_guess'
    ) THEN
        ALTER TABLE fdc_archive_create_session_t
            RENAME COLUMN document_type_code_guess TO busi_module_code_guess;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'fdc_archive_create_session_t'
          AND column_name = 'busi_module_code_guess'
    ) THEN
        ALTER TABLE fdc_archive_create_session_t
            ADD COLUMN busi_module_code_guess VARCHAR(64);
    END IF;
END $$;
