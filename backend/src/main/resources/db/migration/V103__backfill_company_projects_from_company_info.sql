-- Backfill missing company-project records from enabled company info.
-- This keeps legacy company codes selectable in pending-archive create/edit flows.
INSERT INTO fdc_company_project_t (
    company_project_code,
    company_project_name,
    country_code,
    management_area,
    company_tag,
    enable_flag,
    delete_flag,
    created_by,
    creation_date,
    last_updated_by,
    last_update_date,
    tenantid,
    last_update_version
)
SELECT
    ci.company_code,
    ci.company_name,
    COALESCE(
        (SELECT c.country_code
         FROM fdc_country_t c
         WHERE c.delete_flag = 'N'
           AND (c.country_name = ci.country OR c.country_code = ci.country)
         ORDER BY c.sort_order NULLS LAST, c.country_code
         LIMIT 1),
        'CN'
    ) AS country_code,
    NULLIF(ci.region, ''),
    NULLIF(ci.tags, ''),
    'Y',
    'N',
    1,
    CURRENT_TIMESTAMP,
    1,
    CURRENT_TIMESTAMP,
    1,
    0
FROM fdc_company_info_t ci
WHERE ci.delete_flag = 'N'
  AND ci.enabled_flag = 'Y'
  AND NOT EXISTS (
      SELECT 1
      FROM fdc_company_project_t cp
      WHERE cp.company_project_code = ci.company_code
        AND cp.delete_flag = 'N'
  );
