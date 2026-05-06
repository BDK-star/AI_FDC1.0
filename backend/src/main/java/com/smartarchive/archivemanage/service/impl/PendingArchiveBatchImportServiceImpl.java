package com.smartarchive.archivemanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartarchive.archivemanage.dto.ArchiveSummaryResponse;
import com.smartarchive.archivemanage.dto.PendingAuditAttachmentRef;
import com.smartarchive.archivemanage.dto.PendingDocumentWriteCommand;
import com.smartarchive.archivemanage.service.ArchiveManagementService;
import com.smartarchive.businessmodule.dto.BusinessModuleExtFieldResponse;
import com.smartarchive.businessmodule.service.BusinessModuleService;
import com.smartarchive.archivemanage.service.PendingArchiveBatchImportService;
import com.smartarchive.archivemanage.service.support.PendingArchiveBatchImportHeaderResolver;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.workspace.domain.WorkspaceIoJob;
import com.smartarchive.workspace.dto.WorkspaceIoJobCreateCommand;
import com.smartarchive.workspace.dto.WorkspaceIoJobSummaryResponse;
import com.smartarchive.workspace.mapper.WorkspaceIoJobMapper;
import com.smartarchive.workspace.service.WorkspaceIoJobService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingArchiveBatchImportServiceImpl implements PendingArchiveBatchImportService {

    private static final int MAX_DATA_ROWS = 5000;
    private static final int MAX_BASE64_CHARS = 25_000_000;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<String> TOP_LEVEL_KEYS = Set.of(
        "companyProjectCode", "archiveTypeCode", "businessCode", "beginPeriod", "endPeriod",
        "archiveDestination", "originPlace", "documentName", "documentDate", "dutyPerson",
        "dutyDepartment", "carrierTypeCode", "sourceSystem", "securityLevelCode", "remark",
        "documentOrganizationCode", "retentionPeriodYears", "custodyStatus", "visibility", "barcodeModule"
    );

    private final WorkspaceIoJobService workspaceIoJobService;
    private final WorkspaceIoJobMapper workspaceIoJobMapper;
    private final ArchiveManagementService archiveManagementService;
    private final BusinessModuleService businessModuleService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    @Qualifier("pendingArchiveBatchExecutor")
    private final Executor taskExecutor;

    @Override
    public WorkspaceIoJobSummaryResponse submit(
        MultipartFile file,
        String documentTypeCode,
        String operationRemark,
        List<PendingAuditAttachmentRef> auditAttachments,
        long operatorUserId
    ) {
        if (!StringUtils.hasText(documentTypeCode)) {
            throw new BusinessException("documentTypeCode is required");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 CSV 文件");
        }
        String fname = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "import.csv";
        WorkspaceIoJobCreateCommand create = new WorkspaceIoJobCreateCommand();
        create.setJobType("IMPORT_PENDING_ARCHIVE");
        create.setDataType("PENDING_ARCHIVE");
        create.setJobName(fname);
        create.setDocumentTypeCode(documentTypeCode.trim());
        create.setInputFileName(fname);
        create.setJobStatus("RUNNING");
        create.setExportFileFormat("XLSX");
        WorkspaceIoJobSummaryResponse started = workspaceIoJobService.create(create, operatorUserId);
        Long jobId = started.getJobId();
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败: " + e.getMessage());
        }
        final String dt = documentTypeCode.trim();
        final String opRemark = StringUtils.hasText(operationRemark) ? operationRemark.trim() : null;
        final List<PendingAuditAttachmentRef> auditRefs = auditAttachments == null || auditAttachments.isEmpty()
            ? List.of()
            : List.copyOf(auditAttachments);
        taskExecutor.execute(() -> runImport(jobId, bytes, dt, operatorUserId, opRemark, auditRefs));
        return workspaceIoJobService.get(jobId, operatorUserId);
    }

    @Override
    public WorkspaceIoJobSummaryResponse submitAdjust(
        MultipartFile file,
        String documentTypeCode,
        String operationRemark,
        List<PendingAuditAttachmentRef> auditAttachments,
        long operatorUserId
    ) {
        if (!StringUtils.hasText(documentTypeCode)) {
            throw new BusinessException("documentTypeCode is required");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 CSV 文件");
        }
        String fname = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "adjust.csv";
        WorkspaceIoJobCreateCommand create = new WorkspaceIoJobCreateCommand();
        create.setJobType("IMPORT_PENDING_ARCHIVE_ADJUST");
        create.setDataType("PENDING_ARCHIVE_ADJUST");
        create.setJobName(fname);
        create.setDocumentTypeCode(documentTypeCode.trim());
        create.setInputFileName(fname);
        create.setJobStatus("RUNNING");
        create.setExportFileFormat("XLSX");
        WorkspaceIoJobSummaryResponse started = workspaceIoJobService.create(create, operatorUserId);
        Long jobId = started.getJobId();
        final byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("读取上传文件失败: " + e.getMessage());
        }
        final String dt = documentTypeCode.trim();
        final String opRemark = StringUtils.hasText(operationRemark) ? operationRemark.trim() : null;
        final List<PendingAuditAttachmentRef> auditRefs = auditAttachments == null || auditAttachments.isEmpty()
            ? List.of()
            : List.copyOf(auditAttachments);
        taskExecutor.execute(() -> runAdjustImport(jobId, bytes, dt, operatorUserId, opRemark, auditRefs));
        return workspaceIoJobService.get(jobId, operatorUserId);
    }

    private void runAdjustImport(
        long jobId,
        byte[] fileBytes,
        String documentTypeCode,
        long operatorUserId,
        String operationRemark,
        List<PendingAuditAttachmentRef> auditAttachments
    ) {
        long t0 = System.currentTimeMillis();
        try {
            List<String> headers;
            List<CSVRecord> dataRecords;
            try {
                CSVParser parser = CSVParser.parse(
                    new String(fileBytes, StandardCharsets.UTF_8),
                    CSVFormat.DEFAULT
                );
                List<CSVRecord> all = parser.getRecords();
                parser.close();
                if (all.isEmpty()) {
                    persistJobOutcome(jobId, operatorUserId, 0, 0, System.currentTimeMillis() - t0, "FAILED",
                        "CSV 为空", fatalWorkbookBase64("CSV 为空"));
                    return;
                }
                List<String> rawHeader = new ArrayList<>(all.get(0).toList());
                if (!rawHeader.isEmpty()) {
                    String h0 = rawHeader.get(0);
                    if (h0 != null && !h0.isEmpty() && h0.charAt(0) == '\uFEFF') {
                        rawHeader.set(0, h0.substring(1));
                    }
                }
                headers = rawHeader.stream().map(h -> h == null ? "" : h.trim()).toList();
                dataRecords = new ArrayList<>();
                for (int i = 1; i < all.size(); i++) {
                    CSVRecord rec = all.get(i);
                    if (recordAllBlank(rec)) {
                        continue;
                    }
                    dataRecords.add(rec);
                }
            } catch (Exception ex) {
                log.warn("pending batch adjust parse error jobId={}", jobId, ex);
                persistJobOutcome(jobId, operatorUserId, 0, 0, System.currentTimeMillis() - t0, "FAILED",
                    "解析 CSV 失败: " + ex.getMessage(), fatalWorkbookBase64("解析 CSV 失败: " + ex.getMessage()));
                return;
            }

            if (headers.stream().allMatch(h -> !StringUtils.hasText(h))) {
                persistJobOutcome(jobId, operatorUserId, 0, 0, System.currentTimeMillis() - t0, "FAILED",
                    "表头无效", fatalWorkbookBase64("表头无效"));
                return;
            }

            if (dataRecords.size() > MAX_DATA_ROWS) {
                persistJobOutcome(jobId, operatorUserId, dataRecords.size(), 0, System.currentTimeMillis() - t0, "FAILED",
                    "单次最多导入 " + MAX_DATA_ROWS + " 行", fatalWorkbookBase64("超过行数上限"));
                return;
            }

            Map<String, String> fieldDisplayMap;
            try {
                fieldDisplayMap = PendingArchiveBatchImportHeaderResolver.buildBusinessModuleExtDisplayToKeyMap(
                    businessModuleService.listPendingArchiveBasicExtFieldsUnionUnderDocumentType(documentTypeCode));
            } catch (Exception ex) {
                log.warn("pending batch adjust ext field label map jobId={}", jobId, ex);
                fieldDisplayMap = new LinkedHashMap<>();
            }
            List<String> canonicalHeaders = new ArrayList<>(headers.size());
            for (String h : headers) {
                canonicalHeaders.add(PendingArchiveBatchImportHeaderResolver.resolve(h, fieldDisplayMap));
            }
            ImportLookupMaps lookupMaps = loadLookupMaps();

            List<List<String>> sheet1Rows = new ArrayList<>();
            List<ArchiveSummaryResponse> successes = new ArrayList<>();
            int ok = 0;
            List<String> sheet1Headers = new ArrayList<>(headers);
            sheet1Headers.add("导入结果");
            sheet1Headers.add("失败原因");

            for (CSVRecord rec : dataRecords) {
                String importStatus;
                String failureReason = "";
                try {
                    Map<String, String> rowVals = mergeRowValues(headers, canonicalHeaders, rec);
                    String company = getv(rowVals, "companyProjectCode");
                    String archiveType = getv(rowVals, "archiveTypeCode");
                    String business = getv(rowVals, "businessCode");
                    String beginRaw = getv(rowVals, "beginPeriod");
                    if (!StringUtils.hasText(company) || !StringUtils.hasText(archiveType)
                        || !StringUtils.hasText(business) || !StringUtils.hasText(beginRaw)) {
                        throw new BusinessException("定位键不能为空：文档业务编码、公司、业务模块、开始档期");
                    }
                    archiveManagementService.requireArchiveTypeUnderDocumentType(archiveType.trim(), documentTypeCode);
                    LocalDate startPeriod;
                    try {
                        startPeriod = YearMonth.parse(beginRaw.trim()).atDay(1);
                    } catch (DateTimeParseException ex) {
                        throw new BusinessException("开始档期须为 yyyy-MM");
                    }
                    long docId = archiveManagementService
                        .findUnarchivedFormalDocumentIdByNaturalKey(company.trim(), archiveType.trim(), startPeriod, business.trim())
                        .orElseThrow(() -> new BusinessException(
                            "未找到匹配的未归档文档（请核对文档业务编码、公司编码、业务模块编码、开始档期）"));
                    ArchiveSummaryResponse cur = archiveManagementService.getArchiveDetail(docId);
                    PendingDocumentWriteCommand patch = buildCommandFromValues(
                        rowVals,
                        documentTypeCode,
                        operatorUserId,
                        operationRemark,
                        auditAttachments,
                        lookupMaps
                    );
                    PendingDocumentWriteCommand merged = toWriteCommandFromDetail(cur, operatorUserId);
                    mergeAdjustPatch(merged, patch);
                    merged.setSubmitMode("SUBMIT");
                    merged.setOperationTypeCode("BATCH_UPDATE");
                    merged.setOperationRemark(operationRemark);
                    if (auditAttachments != null && !auditAttachments.isEmpty()) {
                        merged.setAuditAttachments(new ArrayList<>(auditAttachments));
                    }
                    ArchiveSummaryResponse updated = archiveManagementService.updatePendingDocument(docId, merged);
                    successes.add(updated);
                    ok++;
                    importStatus = "成功";
                } catch (Exception ex) {
                    importStatus = "失败";
                    failureReason = ex.getMessage() != null ? ex.getMessage() : "未知错误";
                    log.debug("pending batch adjust row failed jobId={} msg={}", jobId, failureReason);
                }
                List<String> line = new ArrayList<>();
                for (int hi = 0; hi < headers.size(); hi++) {
                    line.add(cell(rec, hi));
                }
                line.add(importStatus);
                line.add(failureReason);
                sheet1Rows.add(line);
            }

            byte[] xlsx = buildResultWorkbook(sheet1Headers, sheet1Rows, successes, documentTypeCode);
            String b64 = java.util.Base64.getEncoder().encodeToString(xlsx);
            if (b64.length() > MAX_BASE64_CHARS) {
                persistJobOutcome(jobId, operatorUserId, dataRecords.size(), ok, System.currentTimeMillis() - t0, "FAILED",
                    "结果文件过大", null);
                return;
            }

            int n = dataRecords.size();
            String status = ok == n ? "SUCCESS" : (ok == 0 ? "FAILED" : "PARTIAL_FAILED");
            String err = ok == n ? null : (ok == 0 ? "全部失败" : "部分失败");
            persistJobOutcome(jobId, operatorUserId, n, ok, System.currentTimeMillis() - t0, status, err, b64);
        } catch (Exception e) {
            log.error("pending batch adjust jobId={}", jobId, e);
            try {
                persistJobOutcome(jobId, operatorUserId, 0, 0, System.currentTimeMillis() - t0, "FAILED",
                    e.getMessage(), fatalWorkbookBase64(e.getMessage() != null ? e.getMessage() : "系统错误"));
            } catch (Exception ignored) {
                workspaceIoJobMapper.update(null, new LambdaUpdateWrapper<WorkspaceIoJob>()
                    .eq(WorkspaceIoJob::getJobId, jobId)
                    .set(WorkspaceIoJob::getJobStatus, "FAILED")
                    .set(WorkspaceIoJob::getErrorMessage, e.getMessage())
                    .set(WorkspaceIoJob::getLastUpdateDate, java.time.LocalDateTime.now()));
            }
        }
    }

    /**
     * 与库中 doc_gen_date 精度一致（含小数秒），供批量更新「未改该列」时打底稿，避免误写、审计误报。
     */
    private static String formatDocumentDateForAdjustBase(LocalDateTime dt) {
        if (dt == null) {
            return "";
        }
        return dt.toString().replace('T', ' ');
    }

    private static boolean isPlaceholderDocumentOrganization(String code) {
        return !StringUtils.hasText(code) || "default".equalsIgnoreCase(code.trim());
    }

    private static void mergeAdjustPatch(PendingDocumentWriteCommand base, PendingDocumentWriteCommand patch) {
        if (StringUtils.hasText(patch.getEndPeriod())) {
            base.setEndPeriod(patch.getEndPeriod().trim());
        }
        if (StringUtils.hasText(patch.getArchiveDestination())) {
            base.setArchiveDestination(patch.getArchiveDestination().trim());
        }
        if (StringUtils.hasText(patch.getOriginPlace())) {
            base.setOriginPlace(patch.getOriginPlace().trim());
        }
        if (StringUtils.hasText(patch.getDocumentName())) {
            base.setDocumentName(patch.getDocumentName().trim());
        }
        if (StringUtils.hasText(patch.getDocumentDate())) {
            base.setDocumentDate(patch.getDocumentDate().trim());
        }
        if (StringUtils.hasText(patch.getDutyPerson())) {
            base.setDutyPerson(patch.getDutyPerson().trim());
        }
        if (StringUtils.hasText(patch.getDutyDepartment())) {
            base.setDutyDepartment(patch.getDutyDepartment().trim());
        }
        if (StringUtils.hasText(patch.getCarrierTypeCode())) {
            base.setCarrierTypeCode(patch.getCarrierTypeCode().trim());
        }
        if (StringUtils.hasText(patch.getSourceSystem())) {
            base.setSourceSystem(patch.getSourceSystem().trim());
        }
        if (StringUtils.hasText(patch.getSecurityLevelCode())) {
            base.setSecurityLevelCode(patch.getSecurityLevelCode().trim());
        }
        if (StringUtils.hasText(patch.getRemark())) {
            base.setRemark(patch.getRemark().trim());
        }
        if (StringUtils.hasText(patch.getCustodyStatus())) {
            base.setCustodyStatus(patch.getCustodyStatus().trim());
        }
        if (!isPlaceholderDocumentOrganization(patch.getDocumentOrganizationCode())) {
            base.setDocumentOrganizationCode(patch.getDocumentOrganizationCode().trim());
        }
        if (patch.getRetentionPeriodYears() != null && patch.getRetentionPeriodYears() > 0) {
            base.setRetentionPeriodYears(patch.getRetentionPeriodYears());
        }
        if (patch.getExtValues() != null) {
            for (Map.Entry<String, String> e : patch.getExtValues().entrySet()) {
                if (e.getKey() != null && StringUtils.hasText(e.getKey()) && e.getValue() != null && StringUtils.hasText(e.getValue())) {
                    base.getExtValues().put(e.getKey().trim(), e.getValue().trim());
                }
            }
        }
    }

    private PendingDocumentWriteCommand toWriteCommandFromDetail(ArchiveSummaryResponse cur, long opId) {
        PendingDocumentWriteCommand cmd = new PendingDocumentWriteCommand();
        cmd.setOperatorUserId(opId);
        cmd.setDocumentTypeCode(requireNonBlank(cur.getDocumentTypeCode(), "documentTypeCode"));
        cmd.setCompanyProjectCode(requireNonBlank(cur.getCompanyProjectCode(), "companyProjectCode"));
        String bm = StringUtils.hasText(cur.getBusinessModuleTypeCode()) ? cur.getBusinessModuleTypeCode() : "";
        if (!StringUtils.hasText(bm)) {
            throw new BusinessException("业务模块编码缺失");
        }
        cmd.setArchiveTypeCode(bm);
        cmd.setBusinessCode(requireNonBlank(cur.getBusinessCode(), "businessCode"));
        cmd.setBeginPeriod(requireNonBlank(cur.getBeginPeriod(), "beginPeriod"));
        cmd.setEndPeriod(trimToNullLocal(cur.getEndPeriod()));
        cmd.setArchiveDestination(trimToNullLocal(cur.getArchiveDestination()));
        cmd.setOriginPlace(trimToNullLocal(cur.getOriginPlace()));
        cmd.setDocumentName(requireNonBlank(cur.getDocumentName(), "documentName"));
        cmd.setDocumentDate(formatDocumentDateForAdjustBase(cur.getDocumentDate()));
        cmd.setDutyPerson(StringUtils.hasText(cur.getDutyPerson()) ? cur.getDutyPerson().trim() : "admin");
        cmd.setDutyDepartment(trimToNullLocal(cur.getDutyDepartment()));
        cmd.setCarrierTypeCode(StringUtils.hasText(cur.getCarrierTypeCode()) ? cur.getCarrierTypeCode().trim() : "ELECTRONIC");
        cmd.setSourceSystem(trimToNullLocal(cur.getSourceSystem()));
        cmd.setSecurityLevelCode(StringUtils.hasText(cur.getSecurityLevelCode()) ? cur.getSecurityLevelCode().trim() : "INTERNAL");
        cmd.setRemark(trimToNullLocal(cur.getRemark()));
        cmd.setDocumentOrganizationCode(StringUtils.hasText(cur.getDocumentOrganizationCode()) ? cur.getDocumentOrganizationCode().trim() : "");
        cmd.setRetentionPeriodYears(cur.getRetentionPeriodYears());
        cmd.setCustodyStatus(trimToNullLocal(cur.getCustodyStatus()));
        cmd.setExtValues(cur.getExtValues() != null ? new LinkedHashMap<>(cur.getExtValues()) : new LinkedHashMap<>());
        return cmd;
    }

    private static String trimToNullLocal(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.trim();
    }

    private static String requireNonBlank(String s, String field) {
        if (!StringUtils.hasText(s)) {
            throw new BusinessException(field + " 在现有文档中缺失");
        }
        return s.trim();
    }

    private void runImport(
        long jobId,
        byte[] fileBytes,
        String documentTypeCode,
        long operatorUserId,
        String operationRemark,
        List<PendingAuditAttachmentRef> auditAttachments
    ) {
        long t0 = System.currentTimeMillis();
        try {
            List<String> headers;
            List<CSVRecord> dataRecords;
            try {
                CSVParser parser = CSVParser.parse(
                    new String(fileBytes, StandardCharsets.UTF_8),
                    CSVFormat.DEFAULT
                );
                List<CSVRecord> all = parser.getRecords();
                parser.close();
                if (all.isEmpty()) {
                    persistJobOutcome(jobId, operatorUserId, 0, 0, System.currentTimeMillis() - t0, "FAILED",
                        "CSV 为空", fatalWorkbookBase64("CSV 为空"));
                    return;
                }
                List<String> rawHeader = new ArrayList<>(all.get(0).toList());
                if (!rawHeader.isEmpty()) {
                    String h0 = rawHeader.get(0);
                    if (h0 != null && !h0.isEmpty() && h0.charAt(0) == '\uFEFF') {
                        rawHeader.set(0, h0.substring(1));
                    }
                }
                headers = rawHeader.stream().map(h -> h == null ? "" : h.trim()).toList();
                dataRecords = new ArrayList<>();
                for (int i = 1; i < all.size(); i++) {
                    CSVRecord rec = all.get(i);
                    if (recordAllBlank(rec)) {
                        continue;
                    }
                    dataRecords.add(rec);
                }
            } catch (Exception ex) {
                log.warn("pending batch import parse error jobId={}", jobId, ex);
                persistJobOutcome(jobId, operatorUserId, 0, 0, System.currentTimeMillis() - t0, "FAILED",
                    "解析 CSV 失败: " + ex.getMessage(), fatalWorkbookBase64("解析 CSV 失败: " + ex.getMessage()));
                return;
            }

            if (headers.stream().allMatch(h -> !StringUtils.hasText(h))) {
                persistJobOutcome(jobId, operatorUserId, 0, 0, System.currentTimeMillis() - t0, "FAILED",
                    "表头无效", fatalWorkbookBase64("表头无效"));
                return;
            }

            if (dataRecords.size() > MAX_DATA_ROWS) {
                persistJobOutcome(jobId, operatorUserId, dataRecords.size(), 0, System.currentTimeMillis() - t0, "FAILED",
                    "单次最多导入 " + MAX_DATA_ROWS + " 行", fatalWorkbookBase64("超过行数上限"));
                return;
            }

            Map<String, String> fieldDisplayMap;
            try {
                fieldDisplayMap = PendingArchiveBatchImportHeaderResolver.buildBusinessModuleExtDisplayToKeyMap(
                    businessModuleService.listPendingArchiveBasicExtFieldsUnionUnderDocumentType(documentTypeCode));
            } catch (Exception ex) {
                log.warn("pending batch ext field label map jobId={}", jobId, ex);
                fieldDisplayMap = new LinkedHashMap<>();
            }
            List<String> canonicalHeaders = new ArrayList<>(headers.size());
            for (String h : headers) {
                canonicalHeaders.add(PendingArchiveBatchImportHeaderResolver.resolve(h, fieldDisplayMap));
            }
            ImportLookupMaps lookupMaps = loadLookupMaps();

            List<List<String>> sheet1Rows = new ArrayList<>();
            List<ArchiveSummaryResponse> successes = new ArrayList<>();
            int ok = 0;
            List<String> sheet1Headers = new ArrayList<>(headers);
            sheet1Headers.add("导入结果");
            sheet1Headers.add("失败原因");

            for (CSVRecord rec : dataRecords) {
                String importStatus;
                String failureReason = "";
                try {
                    Map<String, String> rowVals = mergeRowValues(headers, canonicalHeaders, rec);
                    PendingDocumentWriteCommand cmd = buildCommandFromValues(
                        rowVals,
                        documentTypeCode,
                        operatorUserId,
                        operationRemark,
                        auditAttachments,
                        lookupMaps
                    );
                    ArchiveSummaryResponse created = archiveManagementService.createPendingDocument(cmd);
                    successes.add(created);
                    ok++;
                    importStatus = "成功";
                } catch (Exception ex) {
                    importStatus = "失败";
                    failureReason = ex.getMessage() != null ? ex.getMessage() : "未知错误";
                    log.debug("pending batch row failed jobId={} msg={}", jobId, failureReason);
                }
                List<String> line = new ArrayList<>();
                for (int hi = 0; hi < headers.size(); hi++) {
                    line.add(cell(rec, hi));
                }
                line.add(importStatus);
                line.add(failureReason);
                sheet1Rows.add(line);
            }

            byte[] xlsx = buildResultWorkbook(sheet1Headers, sheet1Rows, successes, documentTypeCode);
            String b64 = java.util.Base64.getEncoder().encodeToString(xlsx);
            if (b64.length() > MAX_BASE64_CHARS) {
                persistJobOutcome(jobId, operatorUserId, dataRecords.size(), ok, System.currentTimeMillis() - t0, "FAILED",
                    "结果文件过大", null);
                return;
            }

            int n = dataRecords.size();
            String status = ok == n ? "SUCCESS" : (ok == 0 ? "FAILED" : "PARTIAL_FAILED");
            String err = ok == n ? null : (ok == 0 ? "全部失败" : "部分失败");
            persistJobOutcome(jobId, operatorUserId, n, ok, System.currentTimeMillis() - t0, status, err, b64);
        } catch (Exception e) {
            log.error("pending batch import jobId={}", jobId, e);
            try {
                persistJobOutcome(jobId, operatorUserId, 0, 0, System.currentTimeMillis() - t0, "FAILED",
                    e.getMessage(), fatalWorkbookBase64(e.getMessage() != null ? e.getMessage() : "系统错误"));
            } catch (Exception ignored) {
                workspaceIoJobMapper.update(null, new LambdaUpdateWrapper<WorkspaceIoJob>()
                    .eq(WorkspaceIoJob::getJobId, jobId)
                    .set(WorkspaceIoJob::getJobStatus, "FAILED")
                    .set(WorkspaceIoJob::getErrorMessage, e.getMessage())
                    .set(WorkspaceIoJob::getLastUpdateDate, java.time.LocalDateTime.now()));
            }
        }
    }

    private static boolean recordAllBlank(CSVRecord rec) {
        for (String s : rec) {
            if (StringUtils.hasText(s)) {
                return false;
            }
        }
        return true;
    }

    private String fatalWorkbookBase64(String message) {
        try {
            return java.util.Base64.getEncoder().encodeToString(buildFatalWorkbook(message));
        } catch (IOException e) {
            return null;
        }
    }

    private void persistJobOutcome(long jobId, long opId, int inputTotal, int resultOk, long durationMs, String status,
        String errSummary, String base64) {
        workspaceIoJobMapper.update(null, new LambdaUpdateWrapper<WorkspaceIoJob>()
            .eq(WorkspaceIoJob::getJobId, jobId)
            .set(WorkspaceIoJob::getInputTotal, inputTotal)
            .set(WorkspaceIoJob::getResultTotal, resultOk)
            .set(WorkspaceIoJob::getDurationMs, durationMs)
            .set(WorkspaceIoJob::getJobStatus, status)
            .set(WorkspaceIoJob::getErrorMessage, errSummary)
            .set(WorkspaceIoJob::getResultArtifactBase64, base64)
            .set(WorkspaceIoJob::getArtifactExpiresAt, java.time.LocalDateTime.now().plusDays(7))
            .set(WorkspaceIoJob::getLastUpdatedBy, opId)
            .set(WorkspaceIoJob::getLastUpdateDate, java.time.LocalDateTime.now()));
    }

    private byte[] buildFatalWorkbook(String message) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("导入说明");
            Row r0 = sh.createRow(0);
            r0.createCell(0).setCellValue("说明");
            Row r1 = sh.createRow(1);
            r1.createCell(0).setCellValue(message);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    private byte[] buildResultWorkbook(List<String> sheet1Headers, List<List<String>> sheet1Rows,
        List<ArchiveSummaryResponse> successes, String documentTypeRootCode) throws IOException {
        List<String> extKeys = mergedExtColumnKeys(successes, documentTypeRootCode);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s1 = wb.createSheet("导入明细");
            Row h1 = s1.createRow(0);
            for (int c = 0; c < sheet1Headers.size(); c++) {
                h1.createCell(c).setCellValue(sheet1Headers.get(c));
            }
            for (int r = 0; r < sheet1Rows.size(); r++) {
                Row row = s1.createRow(r + 1);
                List<String> cells = sheet1Rows.get(r);
                for (int c = 0; c < cells.size(); c++) {
                    row.createCell(c).setCellValue(cells.get(c) != null ? cells.get(c) : "");
                }
            }

            Sheet s2 = wb.createSheet("成功明细");
            List<String> h2 = new ArrayList<>(List.of(
                "文档ID", "文档类型编码", "文档类型名称", "公司编码", "公司名称", "业务模块编码", "文档业务编码",
                "开始档期", "结束档期", "文档名称", "文档生成日期", "归档责任人", "责任部门", "归档地", "产生地",
                "密级", "文档组织", "保管状态", "生命周期", "描述", "保管年限（年）", "载体类型", "系统来源"
            ));
            for (String ek : extKeys) {
                h2.add("扩展:" + ek);
            }
            Row hr = s2.createRow(0);
            for (int c = 0; c < h2.size(); c++) {
                hr.createCell(c).setCellValue(h2.get(c));
            }
            if (successes.isEmpty()) {
                Row r = s2.createRow(1);
                r.createCell(0).setCellValue("（无成功记录）");
            } else {
                int rowIdx = 1;
                for (ArchiveSummaryResponse d : successes) {
                    Row row = s2.createRow(rowIdx++);
                    int c = 0;
                    setStr(row, c++, d.getArchiveId() != null ? String.valueOf(d.getArchiveId()) : "");
                    setStr(row, c++, d.getDocumentTypeCode());
                    setStr(row, c++, d.getDocumentTypeName());
                    setStr(row, c++, d.getCompanyProjectCode());
                    setStr(row, c++, d.getCompanyProjectName());
                    setStr(row, c++, d.getBusinessModuleTypeCode());
                    setStr(row, c++, d.getBusinessCode());
                    setStr(row, c++, d.getBeginPeriod());
                    setStr(row, c++, d.getEndPeriod());
                    setStr(row, c++, d.getDocumentName());
                    row.createCell(c++).setCellValue(d.getDocumentDate() != null ? DT_FMT.format(d.getDocumentDate()) : "");
                    setStr(row, c++, d.getDutyPerson());
                    setStr(row, c++, d.getDutyDepartment());
                    setStr(row, c++, d.getArchiveDestination());
                    setStr(row, c++, d.getOriginPlace());
                    setStr(row, c++, d.getSecurityLevelName());
                    setStr(row, c++, d.getDocumentOrganizationCode());
                    setStr(row, c++, d.getCustodyStatus());
                    setStr(row, c++, d.getLifecycleStatus());
                    setStr(row, c++, d.getRemark());
                    setStr(row, c++, d.getRetentionPeriodYears() != null ? String.valueOf(d.getRetentionPeriodYears()) : "");
                    setStr(row, c++, d.getCarrierTypeCode());
                    setStr(row, c++, d.getSourceSystem());
                    Map<String, String> ev = d.getExtValues();
                    for (String ek : extKeys) {
                        setStr(row, c++, resolveExtCell(ev, ek));
                    }
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        }
    }

    /**
     * 成功明细扩展列：先按文档类型根下「应归档」扩展并集（与导入模板一致），再并入各成功行 extValues 中多出的键（如 country、visibility）。
     */
    private List<String> mergedExtColumnKeys(List<ArchiveSummaryResponse> successes, String documentTypeRootCode) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (StringUtils.hasText(documentTypeRootCode)) {
            try {
                List<BusinessModuleExtFieldResponse> union =
                    businessModuleService.listPendingArchiveBasicExtFieldsUnionUnderDocumentType(documentTypeRootCode.trim());
                for (BusinessModuleExtFieldResponse f : union) {
                    if (f == null) {
                        continue;
                    }
                    String en = f.getEnglishFieldName() != null ? f.getEnglishFieldName().trim() : "";
                    String canonical = StringUtils.hasText(en) ? en : (f.getFieldCode() != null ? f.getFieldCode().trim() : "");
                    if (StringUtils.hasText(canonical)) {
                        keys.add(canonical);
                    }
                }
            } catch (Exception ex) {
                log.warn("merged ext columns: union under {} failed", documentTypeRootCode, ex);
            }
        }
        keys.addAll(sortedExtValueKeys(successes));
        return new ArrayList<>(keys);
    }

    /** 成功行 extValues 键合并，字典序。 */
    private static List<String> sortedExtValueKeys(List<ArchiveSummaryResponse> successes) {
        TreeSet<String> keys = new TreeSet<>();
        for (ArchiveSummaryResponse d : successes) {
            if (d.getExtValues() == null) {
                continue;
            }
            for (String k : d.getExtValues().keySet()) {
                if (StringUtils.hasText(k)) {
                    keys.add(k.trim());
                }
            }
        }
        return new ArrayList<>(keys);
    }

    private static String resolveExtCell(Map<String, String> ev, String key) {
        if (ev == null || !StringUtils.hasText(key)) {
            return "";
        }
        String v = ev.get(key);
        return v != null ? v : "";
    }

    private static void setStr(Row row, int idx, String v) {
        Cell cell = row.createCell(idx);
        cell.setCellValue(v != null ? v : "");
    }

    private record ImportLookupMaps(
        Map<String, String> carrierTypeCodeByInput,
        Map<String, String> securityLevelCodeByInput,
        Map<String, String> custodyStatusCodeByInput
    ) {}

    private ImportLookupMaps loadLookupMaps() {
        Map<String, String> carrier = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
            select item_code, item_name
              from fdc_dict_item_t
             where category_code = 'ARCHIVE_CARRIER_TYPE'
               and enable_flag = 'Y'
               and delete_flag = 'N'
            """,
            rs -> {
                while (rs.next()) {
                    String code = rs.getString("item_code");
                    String name = rs.getString("item_name");
                    putCodeLookup(carrier, code, name);
                }
            }
        );
        Map<String, String> security = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
            select security_level_code, security_level_name
              from fdc_security_level_t
             where enable_flag = 'Y'
               and delete_flag = 'N'
            """,
            rs -> {
                while (rs.next()) {
                    String code = rs.getString("security_level_code");
                    String name = rs.getString("security_level_name");
                    putCodeLookup(security, code, name);
                }
            }
        );
        Map<String, String> custody = new LinkedHashMap<>();
        jdbcTemplate.query(
            """
            select item_code, item_name
              from fdc_dict_item_t
             where category_code = 'ARCHIVE_CUSTODY_STATUS'
               and enable_flag = 'Y'
               and delete_flag = 'N'
            """,
            rs -> {
                while (rs.next()) {
                    String code = rs.getString("item_code");
                    String name = rs.getString("item_name");
                    putCodeLookup(custody, code, name);
                }
            }
        );
        return new ImportLookupMaps(carrier, security, custody);
    }

    private static void putCodeLookup(Map<String, String> lookup, String code, String name) {
        if (StringUtils.hasText(code)) {
            String c = code.trim();
            lookup.putIfAbsent(c.toLowerCase(), c);
            lookup.putIfAbsent(c, c);
        }
        if (StringUtils.hasText(name) && StringUtils.hasText(code)) {
            lookup.putIfAbsent(name.trim().toLowerCase(), code.trim());
            lookup.putIfAbsent(name.trim(), code.trim());
        }
    }

    private static String resolveCodeByDisplayOrCode(String raw, Map<String, String> lookup) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String v = raw.trim();
        if (lookup == null || lookup.isEmpty()) {
            return v;
        }
        String hit = lookup.get(v);
        if (StringUtils.hasText(hit)) {
            return hit;
        }
        hit = lookup.get(v.toLowerCase());
        return StringUtils.hasText(hit) ? hit : v;
    }

    private static Map<String, String> mergeRowValues(List<String> rawHeaders, List<String> canonicalHeaders, CSVRecord rec) {
        Map<String, String> row = new LinkedHashMap<>();
        int limit = Math.min(rawHeaders.size(), canonicalHeaders.size());
        for (int i = 0; i < limit; i++) {
            String canon = canonicalHeaders.get(i);
            if (!StringUtils.hasText(canon)) {
                continue;
            }
            String cellVal = cell(rec, i);
            row.merge(canon, cellVal, (a, b) -> StringUtils.hasText(b) ? b : (a != null ? a : ""));
        }
        return row;
    }

    private PendingDocumentWriteCommand buildCommandFromValues(
        Map<String, String> values,
        String documentTypeCode,
        long operatorUserId,
        String operationRemark,
        List<PendingAuditAttachmentRef> auditAttachments,
        ImportLookupMaps lookupMaps
    ) {
        PendingDocumentWriteCommand cmd = new PendingDocumentWriteCommand();
        cmd.setDocumentTypeCode(documentTypeCode);
        cmd.setCompanyProjectCode(getv(values, "companyProjectCode"));
        cmd.setArchiveTypeCode(getv(values, "archiveTypeCode"));
        cmd.setBusinessCode(getv(values, "businessCode"));
        cmd.setBeginPeriod(getv(values, "beginPeriod"));
        String end = getv(values, "endPeriod");
        cmd.setEndPeriod(StringUtils.hasText(end) ? end : null);
        cmd.setArchiveDestination(getv(values, "archiveDestination"));
        cmd.setOriginPlace(getv(values, "originPlace"));
        cmd.setDocumentName(getv(values, "documentName"));
        cmd.setDocumentDate(getv(values, "documentDate"));
        cmd.setDutyPerson(getv(values, "dutyPerson"));
        cmd.setDutyDepartment(getv(values, "dutyDepartment"));
        cmd.setCarrierTypeCode(resolveCodeByDisplayOrCode(getv(values, "carrierTypeCode"), lookupMaps.carrierTypeCodeByInput()));
        cmd.setSourceSystem(getv(values, "sourceSystem"));
        cmd.setSecurityLevelCode(resolveCodeByDisplayOrCode(getv(values, "securityLevelCode"), lookupMaps.securityLevelCodeByInput()));
        cmd.setRemark(getv(values, "remark"));
        cmd.setDocumentOrganizationCode(getv(values, "documentOrganizationCode"));
        cmd.setCustodyStatus(resolveCodeByDisplayOrCode(getv(values, "custodyStatus"), lookupMaps.custodyStatusCodeByInput()));

        String ry = getv(values, "retentionPeriodYears");
        if (StringUtils.hasText(ry)) {
            try {
                cmd.setRetentionPeriodYears(Integer.parseInt(ry.trim()));
            } catch (NumberFormatException ignored) {
                // ignore invalid
            }
        }

        Map<String, String> ext = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : values.entrySet()) {
            String key = e.getKey();
            if (!StringUtils.hasText(key)) {
                continue;
            }
            String v = e.getValue() != null ? e.getValue().trim() : "";
            if (TOP_LEVEL_KEYS.contains(key)) {
                if ("visibility".equals(key) || "barcodeModule".equals(key)) {
                    ext.put(key, v);
                }
                continue;
            }
            ext.put(key, v);
        }
        cmd.setExtValues(ext);
        cmd.setSubmitMode("SUBMIT");
        cmd.setOperationTypeCode("BATCH_CREATE");
        cmd.setOperatorUserId(operatorUserId);
        cmd.setOperationRemark(operationRemark);
        if (auditAttachments != null && !auditAttachments.isEmpty()) {
            cmd.setAuditAttachments(new ArrayList<>(auditAttachments));
        }
        return cmd;
    }

    private static String getv(Map<String, String> m, String k) {
        String v = m.get(k);
        return v != null ? v.trim() : "";
    }

    private static String cell(CSVRecord rec, int colIndex) {
        if (colIndex < 0 || colIndex >= rec.size()) {
            return "";
        }
        String v = rec.get(colIndex);
        return v != null ? v.trim() : "";
    }
}
