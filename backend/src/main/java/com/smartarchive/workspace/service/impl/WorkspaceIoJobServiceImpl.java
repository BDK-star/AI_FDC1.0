package com.smartarchive.workspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.workspace.domain.WorkspaceIoJob;
import com.smartarchive.workspace.dto.WorkspaceExportArtifactResult;
import com.smartarchive.archivemanage.service.support.SecurityLevelResolver;
import com.smartarchive.workspace.dto.WorkspaceImportQueryResultRecordResponse;
import com.smartarchive.workspace.dto.WorkspaceIoJobCreateCommand;
import com.smartarchive.workspace.dto.WorkspaceIoJobPageResponse;
import com.smartarchive.workspace.dto.WorkspaceIoJobQueryCommand;
import com.smartarchive.workspace.dto.WorkspaceIoJobSummaryResponse;
import com.smartarchive.workspace.mapper.WorkspaceIoJobMapper;
import com.smartarchive.workspace.service.WorkspaceIoJobService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceIoJobServiceImpl implements WorkspaceIoJobService {
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final int IMPORT_HISTORY_RETENTION_DAYS = 90;
    private static final int FAILED_IMPORT_FILE_RETENTION_DAYS = 7;
    private static final int EXPORT_ARTIFACT_RETENTION_DAYS = 7;
    private static final int IMPORT_RESULT_ARTIFACT_RETENTION_DAYS = 7;
    private static final int EXPORT_TASKS_PER_USER_PER_DAY = 20;
    private static final int MAX_EXPORT_ARTIFACT_CHARS = 2_000_000;
    /** 「我的导入」仅展示此类 jobType（不含 EXPORT_QUERY） */
    private static final List<String> IMPORT_TASK_JOB_TYPES = List.of(
        "IMPORT_QUERY",
        "IMPORT_PENDING_ARCHIVE",
        "IMPORT_PENDING_ARCHIVE_ADJUST"
    );

    private final WorkspaceIoJobMapper workspaceIoJobMapper;
    private final JdbcTemplate jdbcTemplate;
    private final SecurityLevelResolver securityLevelResolver;

    @Override
    public WorkspaceIoJobSummaryResponse create(WorkspaceIoJobCreateCommand command, long operatorUserId) {
        if (command == null) {
            throw new BusinessException("command is required");
        }
        if (operatorUserId <= 0) {
            throw new BusinessException("operatorUserId is invalid");
        }
        if (!StringUtils.hasText(command.getJobType())) {
            throw new BusinessException("jobType is required");
        }
        if (!StringUtils.hasText(command.getDataType())) {
            throw new BusinessException("dataType is required");
        }
        if (!StringUtils.hasText(command.getJobName())) {
            throw new BusinessException("jobName is required");
        }
        if (!StringUtils.hasText(command.getJobStatus())) {
            throw new BusinessException("jobStatus is required");
        }

        String jobType = command.getJobType().trim();
        String artifact = command.getResultArtifactText();
        if ("EXPORT_QUERY".equals(jobType) && StringUtils.hasText(artifact)) {
            if (artifact.length() > MAX_EXPORT_ARTIFACT_CHARS) {
                throw new BusinessException("导出文件过大，请缩小筛选范围后重试");
            }
            assertExportDailyLimit(operatorUserId);
        }

        String normalizedStatus = normalizeExportStatus(command.getJobStatus().trim(), artifact);

        WorkspaceIoJob entity = new WorkspaceIoJob();
        entity.setJobType(jobType);
        entity.setDataType(command.getDataType().trim());
        entity.setJobName(command.getJobName().trim());
        entity.setDocumentTypeCode(trimToNull(command.getDocumentTypeCode()));
        entity.setQueryConfigJson(command.getQueryConfigJson());
        entity.setInputFileName(trimToNull(command.getInputFileName()));
        entity.setInputTotal(command.getInputTotal());
        entity.setResultTotal(command.getResultTotal());
        entity.setDurationMs(command.getDurationMs());
        entity.setJobStatus(normalizedStatus);
        entity.setErrorMessage(trimToNull(command.getErrorMessage()));
        entity.setFailedFileCsv(command.getFailedFileCsv());
        entity.setExportFileFormat(trimToNull(command.getExportFileFormat()));
        entity.setResultArtifactText(StringUtils.hasText(artifact) ? artifact : null);
        if ("EXPORT_QUERY".equals(jobType) && StringUtils.hasText(artifact)) {
            entity.setArtifactExpiresAt(LocalDateTime.now().plusDays(EXPORT_ARTIFACT_RETENTION_DAYS));
        }
        entity.setDeleteFlag("N");
        entity.setCreatedBy(operatorUserId);
        entity.setLastUpdatedBy(operatorUserId);
        entity.setTenantid(DEFAULT_TENANT_ID);
        entity.setCreationDate(LocalDateTime.now());
        entity.setLastUpdateDate(LocalDateTime.now());
        workspaceIoJobMapper.insert(entity);

        if ("EXPORT_QUERY".equals(jobType)) {
            String ext = exportArtifactExtension(entity.getExportFileFormat());
            Long jobId = entity.getJobId();
            if (jobId != null && jobId > 0) {
                entity.setJobName("workspace-export-" + jobId + "." + ext);
                workspaceIoJobMapper.updateById(entity);
            } else {
                log.warn("WorkspaceIoJob insert did not populate jobId; skip export file rename (jobType={})", jobType);
            }
        }

        return toSummary(entity);
    }

    private static String exportArtifactExtension(String exportFileFormat) {
        if (!StringUtils.hasText(exportFileFormat)) {
            return "csv";
        }
        String f = exportFileFormat.trim().toUpperCase(Locale.ROOT);
        if ("EXCEL".equals(f) || "XLSX".equals(f)) {
            return "xlsx";
        }
        if ("PDF".equals(f)) {
            return "pdf";
        }
        return "csv";
    }

    private void assertExportDailyLimit(long operatorUserId) {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        Long cnt = workspaceIoJobMapper.selectCount(new LambdaQueryWrapper<WorkspaceIoJob>()
            .eq(WorkspaceIoJob::getDeleteFlag, "N")
            .eq(WorkspaceIoJob::getJobType, "EXPORT_QUERY")
            .eq(WorkspaceIoJob::getCreatedBy, operatorUserId)
            .ge(WorkspaceIoJob::getCreationDate, dayStart)
            .isNotNull(WorkspaceIoJob::getResultArtifactText));
        if (cnt != null && cnt >= EXPORT_TASKS_PER_USER_PER_DAY) {
            throw new BusinessException("今日可登记导出任务次数已达上限(20)");
        }
    }

    private static String normalizeExportStatus(String requested, String artifactText) {
        if (!StringUtils.hasText(artifactText)) {
            return requested;
        }
        if ("SUCCESS".equalsIgnoreCase(requested) || "COMPLETED".equalsIgnoreCase(requested)) {
            return "COMPLETED";
        }
        return requested;
    }

    @Override
    public WorkspaceIoJobPageResponse query(WorkspaceIoJobQueryCommand command, long operatorUserId) {
        if (operatorUserId <= 0) {
            throw new BusinessException("operatorUserId is invalid");
        }
        int page = command.getPage() != null && command.getPage() > 0 ? command.getPage() : 1;
        int pageSize = command.getPageSize() != null && command.getPageSize() > 0 ? command.getPageSize() : 20;

        LocalDateTime createdStart = parseDateTimeOrNull(command.getCreatedStart());
        LocalDateTime createdEnd = parseDateTimeOrNull(command.getCreatedEnd());

        String statusFilter = trimToNull(command.getJobStatus());
        boolean displayStatusFilter =
            "EXPIRED".equalsIgnoreCase(statusFilter) || "COMPLETED".equalsIgnoreCase(statusFilter);

        // 注意：MyBatis-Plus 要求 WHERE 条件在 orderBy 之前追加；此前在 orderBy 之后再 in(jobType)，可能导致 job_type 过滤未生效，
        // 「我的导入」会误查出 EXPORT_QUERY 导出任务。
        LambdaQueryWrapper<WorkspaceIoJob> wrapper = new LambdaQueryWrapper<WorkspaceIoJob>()
            .eq(WorkspaceIoJob::getDeleteFlag, "N")
            .eq(WorkspaceIoJob::getTenantid, DEFAULT_TENANT_ID)
            .eq(WorkspaceIoJob::getCreatedBy, operatorUserId)
            .eq(StringUtils.hasText(command.getJobType()), WorkspaceIoJob::getJobType, trimToNull(command.getJobType()))
            .eq(StringUtils.hasText(command.getDataType()), WorkspaceIoJob::getDataType, trimToNull(command.getDataType()))
            .eq(StringUtils.hasText(command.getExportFileFormat()), WorkspaceIoJob::getExportFileFormat, trimToNull(command.getExportFileFormat()))
            .eq(StringUtils.hasText(command.getJobStatus()) && !displayStatusFilter, WorkspaceIoJob::getJobStatus, statusFilter)
            .eq(command.getInputTotal() != null, WorkspaceIoJob::getInputTotal, command.getInputTotal())
            .eq(command.getResultTotal() != null, WorkspaceIoJob::getResultTotal, command.getResultTotal())
            .ge(createdStart != null, WorkspaceIoJob::getCreationDate, createdStart)
            .le(createdEnd != null, WorkspaceIoJob::getCreationDate, createdEnd)
            .like(StringUtils.hasText(command.getInputFileName()), WorkspaceIoJob::getInputFileName, trimToNull(command.getInputFileName()))
            .and(StringUtils.hasText(command.getKeyword()), w -> w
                .like(WorkspaceIoJob::getJobName, trimToNull(command.getKeyword()))
                .or()
                .like(WorkspaceIoJob::getInputFileName, trimToNull(command.getKeyword()))
                .or()
                .like(WorkspaceIoJob::getDocumentTypeCode, trimToNull(command.getKeyword()))
            );

        String jtFilter = trimToNull(command.getJobType());
        boolean importTasksOnly = Boolean.TRUE.equals(command.getImportTasksOnly());
        if (importTasksOnly) {
            wrapper.in(WorkspaceIoJob::getJobType, IMPORT_TASK_JOB_TYPES);
            wrapper.ge(WorkspaceIoJob::getCreationDate, LocalDateTime.now().minusDays(IMPORT_HISTORY_RETENTION_DAYS));
        } else if (jtFilter == null) {
            wrapper.in(WorkspaceIoJob::getJobType, IMPORT_TASK_JOB_TYPES);
            wrapper.ge(WorkspaceIoJob::getCreationDate, LocalDateTime.now().minusDays(IMPORT_HISTORY_RETENTION_DAYS));
        } else if ("IMPORT_QUERY".equals(jtFilter) || "IMPORT_PENDING_ARCHIVE".equals(jtFilter) || "IMPORT_PENDING_ARCHIVE_ADJUST".equals(jtFilter)) {
            wrapper.ge(WorkspaceIoJob::getCreationDate, LocalDateTime.now().minusDays(IMPORT_HISTORY_RETENTION_DAYS));
        }

        wrapper.orderByDesc(WorkspaceIoJob::getCreationDate).orderByDesc(WorkspaceIoJob::getJobId);

        List<WorkspaceIoJob> all = workspaceIoJobMapper.selectList(wrapper);
        if (importTasksOnly) {
            all = all.stream()
                .filter(j -> j.getJobType() != null && IMPORT_TASK_JOB_TYPES.contains(j.getJobType()))
                .toList();
        }
        if (displayStatusFilter && statusFilter != null) {
            LocalDateTime now = LocalDateTime.now();
            all = all.stream()
                .filter(j -> statusFilter.equalsIgnoreCase(computeDisplayStatus(j, now)))
                .toList();
        }
        long total = all.size();
        int start = Math.max(0, (page - 1) * pageSize);
        int end = Math.min(start + pageSize, all.size());
        List<WorkspaceIoJobSummaryResponse> records = start < end ? all.subList(start, end).stream().map(this::toSummary).toList() : List.of();
        int pages = (int) Math.ceil(total * 1.0 / pageSize);

        return WorkspaceIoJobPageResponse.builder()
            .records(records)
            .total(total)
            .pages(pages)
            .page(page)
            .pageSize(pageSize)
            .build();
    }

    @Override
    public WorkspaceIoJobSummaryResponse get(Long jobId, long operatorUserId) {
        WorkspaceIoJob job = requireOwnedJob(jobId, operatorUserId);
        return toSummary(job);
    }

    private String computeDisplayStatus(WorkspaceIoJob entity, LocalDateTime now) {
        if ("EXPORT_QUERY".equals(entity.getJobType()) && StringUtils.hasText(entity.getResultArtifactText())) {
            LocalDateTime exp = entity.getArtifactExpiresAt();
            if (exp != null && exp.isBefore(now)) {
                return "EXPIRED";
            }
            if ("COMPLETED".equalsIgnoreCase(entity.getJobStatus()) || "SUCCESS".equalsIgnoreCase(entity.getJobStatus())) {
                return "COMPLETED";
            }
        }
        if (("IMPORT_PENDING_ARCHIVE".equals(entity.getJobType()) || "IMPORT_PENDING_ARCHIVE_ADJUST".equals(entity.getJobType()))
            && StringUtils.hasText(entity.getResultArtifactBase64())) {
            LocalDateTime exp = entity.getArtifactExpiresAt();
            if (exp != null && exp.isBefore(now)) {
                return "EXPIRED";
            }
        }
        if ("IMPORT_QUERY".equals(entity.getJobType()) && StringUtils.hasText(entity.getResultArtifactText())) {
            LocalDateTime exp = entity.getArtifactExpiresAt();
            if (exp != null && exp.isBefore(now)) {
                return "EXPIRED";
            }
        }
        return entity.getJobStatus();
    }

    private WorkspaceIoJobSummaryResponse toSummary(WorkspaceIoJob entity) {
        LocalDateTime now = LocalDateTime.now();
        boolean failedFileExpired = isFailedImportFileExpired(entity, now);
        String failedCsv = failedFileExpired ? null : entity.getFailedFileCsv();

        String displayStatus = computeDisplayStatus(entity, now);
        boolean exportDownloadable = false;
        if ("EXPORT_QUERY".equals(entity.getJobType()) && StringUtils.hasText(entity.getResultArtifactText())) {
            LocalDateTime exp = entity.getArtifactExpiresAt();
            if ("COMPLETED".equalsIgnoreCase(displayStatus) && exp != null && !exp.isBefore(now)) {
                exportDownloadable = true;
            }
        }

        boolean resultArtifactDownloadable = false;
        if (("IMPORT_PENDING_ARCHIVE".equals(entity.getJobType()) || "IMPORT_PENDING_ARCHIVE_ADJUST".equals(entity.getJobType()))
            && StringUtils.hasText(entity.getResultArtifactBase64())) {
            LocalDateTime exp = entity.getArtifactExpiresAt();
            if (exp != null && !exp.isBefore(now) && !"EXPIRED".equalsIgnoreCase(displayStatus)) {
                resultArtifactDownloadable = true;
            }
        }
        if ("IMPORT_QUERY".equals(entity.getJobType()) && StringUtils.hasText(entity.getResultArtifactText())) {
            LocalDateTime exp = entity.getArtifactExpiresAt();
            if (exp != null && !exp.isBefore(now) && !"EXPIRED".equalsIgnoreCase(displayStatus)) {
                resultArtifactDownloadable = true;
            }
        }

        return WorkspaceIoJobSummaryResponse.builder()
            .jobId(entity.getJobId())
            .jobType(entity.getJobType())
            .dataType(entity.getDataType())
            .jobName(entity.getJobName())
            .documentTypeCode(entity.getDocumentTypeCode())
            .queryConfigJson(entity.getQueryConfigJson())
            .inputFileName(entity.getInputFileName())
            .inputTotal(entity.getInputTotal())
            .resultTotal(entity.getResultTotal())
            .durationMs(entity.getDurationMs())
            .jobStatus(entity.getJobStatus())
            .errorMessage(entity.getErrorMessage())
            .failedFileCsv(failedCsv)
            .exportFileFormat(entity.getExportFileFormat())
            .artifactExpiresAt(entity.getArtifactExpiresAt())
            .displayStatus(displayStatus)
            .exportDownloadable(exportDownloadable)
            .resultArtifactDownloadable(resultArtifactDownloadable)
            .creationDate(entity.getCreationDate())
            .build();
    }

    private boolean isFailedImportFileExpired(WorkspaceIoJob entity, LocalDateTime now) {
        if (!"IMPORT_QUERY".equals(entity.getJobType())) {
            return false;
        }
        if (!StringUtils.hasText(entity.getFailedFileCsv())) {
            return false;
        }
        if (entity.getCreationDate() == null) {
            return true;
        }
        return entity.getCreationDate().plusDays(FAILED_IMPORT_FILE_RETENTION_DAYS).isBefore(now);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private LocalDateTime parseDateTimeOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String v = value.trim();
        try {
            return LocalDateTime.parse(v);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @Override
    public void delete(Long jobId, long operatorUserId) {
        if (jobId == null) {
            throw new BusinessException("jobId is required");
        }
        requireOwnedJob(jobId, operatorUserId);
        workspaceIoJobMapper.update(null, new LambdaUpdateWrapper<WorkspaceIoJob>()
            .eq(WorkspaceIoJob::getJobId, jobId)
            .eq(WorkspaceIoJob::getTenantid, DEFAULT_TENANT_ID)
            .set(WorkspaceIoJob::getDeleteFlag, "Y")
            .set(WorkspaceIoJob::getLastUpdatedBy, operatorUserId)
            .set(WorkspaceIoJob::getLastUpdateDate, LocalDateTime.now()));
    }

    @Override
    public String getFailedFileCsv(Long jobId, long operatorUserId) {
        WorkspaceIoJob job = requireOwnedJob(jobId, operatorUserId);
        if (!StringUtils.hasText(job.getFailedFileCsv())) {
            throw new BusinessException("No failed file");
        }
        if (isFailedImportFileExpired(job, LocalDateTime.now())) {
            throw new BusinessException("失败文件已超过保留期(7天)，已清理");
        }
        return job.getFailedFileCsv();
    }

    @Override
    public WorkspaceExportArtifactResult downloadExportArtifact(Long jobId, long operatorUserId) {
        WorkspaceIoJob job = requireOwnedJob(jobId, operatorUserId);
        if (!"EXPORT_QUERY".equals(job.getJobType())) {
            throw new BusinessException("Not an export task");
        }
        if (!StringUtils.hasText(job.getResultArtifactText())) {
            throw new BusinessException("该任务无可下载文件（仅查询未登记导出）");
        }
        LocalDateTime now = LocalDateTime.now();
        if (job.getArtifactExpiresAt() == null || job.getArtifactExpiresAt().isBefore(now)) {
            throw new BusinessException("导出文件已过期(7天)，请重新申请");
        }

        String fmt = StringUtils.hasText(job.getExportFileFormat()) ? job.getExportFileFormat().trim().toUpperCase(Locale.ROOT) : "CSV";
        byte[] body;
        String fileName;
        String contentType;
        String text = job.getResultArtifactText();
        if ("EXCEL".equals(fmt)) {
            String withBom = "\uFEFF" + text;
            body = withBom.getBytes(StandardCharsets.UTF_8);
            fileName = "export-" + jobId + ".csv";
            contentType = "text/csv; charset=utf-8";
        } else if ("PDF".equals(fmt)) {
            body = text.getBytes(StandardCharsets.UTF_8);
            fileName = "export-" + jobId + ".txt";
            contentType = "text/plain; charset=utf-8";
        } else {
            // CSV：加 BOM，与同步导出一致，避免 Excel 打开 UTF-8 时中文或列错位被误认为「无内容」
            String withBom = "\uFEFF" + text;
            body = withBom.getBytes(StandardCharsets.UTF_8);
            fileName = "export-" + jobId + ".csv";
            contentType = "text/csv; charset=utf-8";
        }

        recordExportDownloadAudit(jobId, operatorUserId, fileName);
        return new WorkspaceExportArtifactResult(body, fileName, contentType);
    }

    @Override
    public WorkspaceExportArtifactResult downloadImportResultArtifact(Long jobId, long operatorUserId) {
        WorkspaceIoJob job = requireOwnedJob(jobId, operatorUserId);
        if ("IMPORT_QUERY".equals(job.getJobType())) {
            if (!StringUtils.hasText(job.getResultArtifactText())) {
                throw new BusinessException("该任务暂无可下载的结果文件");
            }
            LocalDateTime now = LocalDateTime.now();
            if (job.getArtifactExpiresAt() == null || job.getArtifactExpiresAt().isBefore(now)) {
                throw new BusinessException("结果文件已过期，请重新导入查询");
            }
            byte[] body = job.getResultArtifactText().getBytes(StandardCharsets.UTF_8);
            String fileName = "import-query-result-" + jobId + ".csv";
            recordImportResultDownloadAudit(jobId, operatorUserId, fileName);
            return new WorkspaceExportArtifactResult(body, fileName, "text/csv; charset=utf-8");
        }
        if (!"IMPORT_PENDING_ARCHIVE".equals(job.getJobType()) && !"IMPORT_PENDING_ARCHIVE_ADJUST".equals(job.getJobType())) {
            throw new BusinessException("不是应归档批量导入/批量更新任务");
        }
        if (!StringUtils.hasText(job.getResultArtifactBase64())) {
            throw new BusinessException("该任务暂无可下载的结果文件");
        }
        LocalDateTime now = LocalDateTime.now();
        if (job.getArtifactExpiresAt() == null || job.getArtifactExpiresAt().isBefore(now)) {
            throw new BusinessException("结果文件已过期，请重新导入");
        }
        byte[] body;
        try {
            body = java.util.Base64.getMimeDecoder().decode(job.getResultArtifactBase64().trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("结果文件损坏，无法下载");
        }
        String fileName = "pending-archive-import-result-" + jobId + ".xlsx";
        recordImportResultDownloadAudit(jobId, operatorUserId, fileName);
        return new WorkspaceExportArtifactResult(
            body,
            fileName,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );
    }

    @Override
    public List<WorkspaceImportQueryResultRecordResponse> listImportQueryResults(Long jobId, long operatorUserId) {
        WorkspaceIoJob job = requireOwnedJob(jobId, operatorUserId);
        if (!"IMPORT_QUERY".equals(job.getJobType())) {
            throw new BusinessException("不是文档批量导入查询任务");
        }
        return jdbcTemplate.query(
            """
            select distinct on (d.doc_id)
                   d.doc_id as archive_id,
                   d.doc_biz_no as business_code,
                   d.company_name as company_project_name,
                   d.biz_module_code as archive_type_code,
                   to_char(d.start_period, 'YYYY-MM') as begin_period,
                   to_char(d.end_period, 'YYYY-MM') as end_period,
                   d.arch_place_alpha2_code as archive_destination,
                   d.origin_place_alpha2_code as origin_place,
                   d.doc_organization_code as document_organization_code,
                   d.lifecycle_status,
                   d.doc_name as document_name,
                   to_char(d.doc_gen_date, 'YYYY-MM-DD HH24:MI:SS') as document_date,
                   coalesce(
                    nullif(trim(concat_ws(' ', nullif(owner.user_name, ''), nullif(owner.employee_no, ''))), ''),
                     cast(d.doc_resp_person_id as varchar)
                   ) as duty_person,
                   cast(d.doc_resp_dept_id as varchar) as duty_department,
                   d.carrier_type as carrier_type_code,
                   case when coalesce(d.visible_flag, '1') = '0' then '否' else '是' end as document_visibility,
                   d.source_system,
                   d.security_level,
                   d.description as remark,
                   to_char(d.creation_date, 'YYYY-MM-DD HH24:MI:SS') as creation_date,
                   coalesce(
                    nullif(trim(concat_ws(' ', nullif(cu.user_name, ''), nullif(cu.employee_no, ''))), ''),
                     cast(d.created_by as varchar)
                   ) as created_by
              from fdc_workspace_import_query_result_t r
              join fdc_document_t d
                on d.doc_id = r.archive_id
               and coalesce(d.delete_flag, 0) = 0
              left join tpl_user_t owner on owner.user_id = d.doc_resp_person_id
              left join tpl_user_t cu on cu.user_id = d.created_by
             where r.job_id = ?
             order by d.doc_id, r.query_row_no
            """,
            (rs, rowNum) -> {
                String lifecycle = rs.getString("lifecycle_status");
                SecurityLevelResolver.Resolved sec = securityLevelResolver.resolve(rs.getString("security_level"));
                String archiveStatus = "ARCHIVED".equalsIgnoreCase(lifecycle)
                    ? "已归档"
                    : ("DRAFT".equalsIgnoreCase(lifecycle) ? "草稿" : "未归档");
                return WorkspaceImportQueryResultRecordResponse.builder()
                .archiveId(rs.getLong("archive_id"))
                .businessCode(rs.getString("business_code"))
                .companyProjectName(rs.getString("company_project_name"))
                .archiveTypeCode(rs.getString("archive_type_code"))
                .beginPeriod(rs.getString("begin_period"))
                .endPeriod(rs.getString("end_period"))
                .archiveDestination(rs.getString("archive_destination"))
                .originPlace(rs.getString("origin_place"))
                .documentOrganizationCode(rs.getString("document_organization_code"))
                .archiveStatus(archiveStatus)
                .documentName(rs.getString("document_name"))
                .documentDate(rs.getString("document_date"))
                .dutyPerson(rs.getString("duty_person"))
                .dutyDepartment(rs.getString("duty_department"))
                .carrierTypeCode(rs.getString("carrier_type_code"))
                .documentVisibility(rs.getString("document_visibility"))
                .sourceSystem(rs.getString("source_system"))
                .securityLevelName(sec.displayName())
                .remark(rs.getString("remark"))
                .creationDate(rs.getString("creation_date"))
                .createdBy(rs.getString("created_by"))
                .build();
            },
            jobId
        );
    }

    private void recordImportResultDownloadAudit(Long jobId, long operatorUserId, String fileName) {
        jdbcTemplate.update(
            """
            insert into fdc_audit_log_t (
              tenantid, object_id, object_type, operated_by, operation_type, op_content,
              operation_time, created_by, creation_date, last_updated_by, last_update_date, last_update_version
            ) values (?, ?, ?, ?, ?, ?, current_timestamp, ?, current_timestamp, ?, current_timestamp, 0)
            """,
            DEFAULT_TENANT_ID,
            jobId,
            "WORKSPACE_IMPORT",
            operatorUserId,
            "DOWNLOAD",
            "下载应归档批量导入结果: " + fileName,
            operatorUserId,
            operatorUserId
        );
    }

    private void recordExportDownloadAudit(Long jobId, long operatorUserId, String fileName) {
        jdbcTemplate.update(
            """
            insert into fdc_audit_log_t (
              tenantid, object_id, object_type, operated_by, operation_type, op_content,
              operation_time, created_by, creation_date, last_updated_by, last_update_date, last_update_version
            ) values (?, ?, ?, ?, ?, ?, current_timestamp, ?, current_timestamp, ?, current_timestamp, 0)
            """,
            DEFAULT_TENANT_ID,
            jobId,
            "WORKSPACE_EXPORT",
            operatorUserId,
            "DOWNLOAD",
            "下载导出文件: " + fileName,
            operatorUserId,
            operatorUserId
        );
    }

    private WorkspaceIoJob requireOwnedJob(Long jobId, long operatorUserId) {
        if (jobId == null) {
            throw new BusinessException("jobId is required");
        }
        WorkspaceIoJob job = workspaceIoJobMapper.selectOne(new LambdaQueryWrapper<WorkspaceIoJob>()
            .eq(WorkspaceIoJob::getJobId, jobId)
            .eq(WorkspaceIoJob::getTenantid, DEFAULT_TENANT_ID)
            .eq(WorkspaceIoJob::getDeleteFlag, "N"));
        if (job == null) {
            throw new BusinessException("Job not found");
        }
        if (job.getCreatedBy() == null || !job.getCreatedBy().equals(operatorUserId)) {
            throw new BusinessException("Forbidden");
        }
        return job;
    }
}
