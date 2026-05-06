package com.smartarchive.archivemanage.controller;

import com.smartarchive.archivemanage.support.DocumentVisibilitySupport;
import com.smartarchive.archivemanage.dto.PendingDocumentQueryCommand;
import com.smartarchive.archivemanage.dto.PendingDocumentRowResponse;
import com.smartarchive.archivemanage.service.PendingArchiveBatchImportService;
import com.smartarchive.archivemanage.service.support.PendingBatchImportTemplateService;
import com.smartarchive.archivemanage.service.support.MultiValueTextParse;
import com.smartarchive.archivemanage.service.support.SecurityLevelResolver;
import com.smartarchive.workspace.dto.WorkspaceIoJobCreateCommand;
import com.smartarchive.workspace.dto.WorkspaceIoJobSummaryResponse;
import com.smartarchive.workspace.service.WorkspaceIoJobService;
import com.smartarchive.businessmodule.domain.BusinessModule;
import com.smartarchive.businessmodule.mapper.BusinessModuleMapper;
import com.smartarchive.common.api.ApiResponse;
import com.smartarchive.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartarchive.archivemanage.dto.PendingAuditAttachmentRef;
import com.smartarchive.archivemanage.dto.PendingDocumentWriteCommand;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
import java.util.LinkedHashMap;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import java.util.Locale;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Qualifier;

@RestController
@RequestMapping("/api/archive-management/pending-documents")
public class PendingDocumentQueryController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final BusinessModuleMapper businessModuleMapper;
    private final SecurityLevelResolver securityLevelResolver;
    private final ObjectMapper objectMapper;
    private final PendingArchiveBatchImportService pendingArchiveBatchImportService;
    private final PendingBatchImportTemplateService pendingBatchImportTemplateService;
    private final WorkspaceIoJobService workspaceIoJobService;
    private final JdbcTemplate jdbcTemplate;
    @Qualifier("pendingArchiveBatchExecutor")
    private final Executor taskExecutor;

    public PendingDocumentQueryController(NamedParameterJdbcTemplate namedParameterJdbcTemplate,
                                          BusinessModuleMapper businessModuleMapper,
                                          SecurityLevelResolver securityLevelResolver,
                                          ObjectMapper objectMapper,
                                          PendingArchiveBatchImportService pendingArchiveBatchImportService,
                                          PendingBatchImportTemplateService pendingBatchImportTemplateService,
                                          WorkspaceIoJobService workspaceIoJobService,
                                          JdbcTemplate jdbcTemplate,
                                          @Qualifier("pendingArchiveBatchExecutor") Executor taskExecutor) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.businessModuleMapper = businessModuleMapper;
        this.securityLevelResolver = securityLevelResolver;
        this.objectMapper = objectMapper;
        this.pendingArchiveBatchImportService = pendingArchiveBatchImportService;
        this.pendingBatchImportTemplateService = pendingBatchImportTemplateService;
        this.workspaceIoJobService = workspaceIoJobService;
        this.jdbcTemplate = jdbcTemplate;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 应归档批量导入 CSV 模板（服务端生成，与前端列规则一致）。
     */
    @GetMapping(value = "/batch-import-template", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> getBatchImportTemplate(
        @RequestParam String documentTypeCode,
        @RequestParam(required = false) String companyProjectCode,
        @RequestParam(required = false) String archiveTypeCode,
        @RequestParam(required = false) String documentTypeName
    ) {
        String body = pendingBatchImportTemplateService.buildTemplateCsv(
            documentTypeCode,
            companyProjectCode,
            archiveTypeCode,
            documentTypeName != null ? documentTypeName : ""
        );
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"pending-batch-import-template.csv\"")
            .body(body);
    }

    /**
     * 与 {@code ArchiveManagementController} 下其他 pending-documents 接口同前缀，避免 multipart consumes 与路由顺序问题。
     */
    @PostMapping("/batch-import")
    public ApiResponse<WorkspaceIoJobSummaryResponse> batchImportPendingDocuments(
        @RequestParam("file") MultipartFile file,
        @RequestParam String documentTypeCode,
        @RequestParam(required = false) String operationRemark,
        @RequestParam(required = false) String auditAttachmentsJson,
        @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        long uid = userId != null && userId > 0 ? userId : 1L;
        List<PendingAuditAttachmentRef> auditRefs = parseAuditAttachmentRefs(auditAttachmentsJson);
        return ApiResponse.success(
            pendingArchiveBatchImportService.submit(file, documentTypeCode, operationRemark, auditRefs, uid));
    }

    private List<PendingAuditAttachmentRef> parseAuditAttachmentRefs(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<PendingAuditAttachmentRef> list = objectMapper.readValue(json.trim(), new TypeReference<>() { });
            return list != null ? list : List.of();
        } catch (Exception e) {
            throw new BusinessException("补充说明附件参数格式无效: " + e.getMessage());
        }
    }

    @PostMapping("/import-query-jobs")
    public ApiResponse<WorkspaceIoJobSummaryResponse> submitPendingImportQueryJob(
        @RequestParam("file") MultipartFile file,
        @RequestParam String documentTypeCode,
        @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        long uid = userId != null && userId > 0 ? userId : 1L;
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 CSV 文件");
        }
        if (!StringUtils.hasText(documentTypeCode)) {
            throw new BusinessException("documentTypeCode is required");
        }
        WorkspaceIoJobCreateCommand create = new WorkspaceIoJobCreateCommand();
        create.setJobType("IMPORT_QUERY");
        create.setDataType("PENDING_ARCHIVE_QUERY");
        create.setJobName(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "pending-import-query.csv");
        create.setDocumentTypeCode(documentTypeCode.trim());
        create.setInputFileName(create.getJobName());
        create.setJobStatus("RUNNING");
        WorkspaceIoJobSummaryResponse started = workspaceIoJobService.create(create, uid);
        final long jobId = started.getJobId();
        final byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception ex) {
            throw new BusinessException("读取文件失败: " + ex.getMessage());
        }
        final String docType = documentTypeCode.trim();
        taskExecutor.execute(() -> runPendingImportQueryJob(jobId, content, docType, uid));
        return ApiResponse.success(started);
    }

    @PostMapping("/query")
    public ApiResponse<List<PendingDocumentRowResponse>> query(@RequestBody PendingDocumentQueryCommand command) {
        validateDocumentTypeArchiveTypeConsistency(command);
        Map<String, String> documentTypeNameMap = businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getDeleteFlag, "N"))
            .stream()
            .filter(b -> b != null && StringUtils.hasText(b.getModuleCode()))
            .collect(Collectors.toMap(BusinessModule::getModuleCode, BusinessModule::getModuleName, (left, right) -> left));
        Map<String, String> carrierTypeNameMap = loadCarrierTypeNameMap();
        boolean draftOwnerQuery = command.getCreatedByUserId() != null && command.getCreatedByUserId() > 0;
        if (draftOwnerQuery) {
            return ApiResponse.success(queryDraftTableRows(command, documentTypeNameMap, carrierTypeNameMap));
        }
        StringBuilder sql = new StringBuilder(
            """
            select distinct on (fdc_document_t.doc_id)
                   fdc_document_t.doc_id, doc_biz_no, company_name, biz_module_code, start_period, end_period,
                   arch_place_alpha2_code, origin_place_alpha2_code, doc_organization_code, lifecycle_status,
                   doc_name, doc_gen_date, doc_resp_person_id, doc_resp_dept_id, carrier_type,
                   visible_flag,
                   source_system, security_level, description, fdc_document_t.creation_date as creation_date,
                   fdc_document_t.created_by as created_by, fdc_document_t.last_updated_by as last_updated_by, fdc_document_t.last_update_date as last_update_date,
                   cp.country_code, geo.rep_office_name, geo.region_name,
                   coalesce(
                    nullif(trim(concat_ws(' ', nullif(owner.user_name, ''), nullif(owner.employee_no, ''))), ''),
                     cast(fdc_document_t.doc_resp_person_id as varchar)
                   ) as owner_name,
                   coalesce(
                    nullif(trim(concat_ws(' ', nullif(created_u.user_name, ''), nullif(created_u.employee_no, ''))), ''),
                     cast(fdc_document_t.created_by as varchar)
                   ) as created_by_name,
                   coalesce(
                    nullif(trim(concat_ws(' ', nullif(updated_u.user_name, ''), nullif(updated_u.employee_no, ''))), ''),
                     cast(fdc_document_t.last_updated_by as varchar)
                   ) as updated_by_name
            from fdc_document_t
            left join tpl_user_t owner on owner.user_id = fdc_document_t.doc_resp_person_id
            left join tpl_user_t created_u on created_u.user_id = fdc_document_t.created_by
            left join tpl_user_t updated_u on updated_u.user_id = fdc_document_t.last_updated_by
            left join fdc_company_project_t cp on cp.company_project_code = fdc_document_t.company_code and cp.delete_flag = 'N'
            left join (
                select country_code, min(rep_office_name) as rep_office_name, min(region_name) as region_name
                  from fdc_geo_region_t
                 where enable_flag = 'Y' and delete_flag = 'N'
                 group by country_code
            ) geo on geo.country_code = cp.country_code
            """
        );
        sql.append("""
            where coalesce(fdc_document_t.delete_flag, 0) = 0
        """);
        MapSqlParameterSource params = new MapSqlParameterSource();
        sql.append(" and lower(trim(coalesce(fdc_document_t.lifecycle_status, ''))) = 'unarchived'");
        if (hasText(command.getCustodyStatus())) {
            sql.append(" and lower(trim(coalesce(fdc_document_t.custody_status, ''))) = lower(trim(:custodyStatus))");
            params.addValue("custodyStatus", command.getCustodyStatus().trim());
        }

        if (hasText(command.getDocumentTypeCode())) {
            // 一级文档类型对应 L1 module_code；正式表存的是叶子/二级模块编码（如 GUARANTEE），不能仅用 LIKE 'L1%'。
            sql.append("""
                and exists (
                  select 1 from fdc_business_module_t bm
                   where bm.module_code = fdc_document_t.biz_module_code
                     and coalesce(bm.delete_flag, 'N') = 'N'
                     and coalesce(bm.enabled_flag, 'Y') = 'Y'
                     and (
                          bm.module_code = :documentTypeRoot
                       or split_part(trim(both '/' from coalesce(bm.ancestor_path, '')), '/', 1) = :documentTypeRoot
                     )
                )
                """);
            params.addValue("documentTypeRoot", command.getDocumentTypeCode().trim());
        }
        if (hasText(command.getCompanyCode())) {
            sql.append(" and company_code = :companyCode");
            params.addValue("companyCode", command.getCompanyCode().trim());
        }
        List<String> archiveTypeCodes = resolveArchiveTypeCodes(command);
        if (!archiveTypeCodes.isEmpty()) {
            sql.append(" and biz_module_code in (:archiveTypeCodes)");
            params.addValue("archiveTypeCodes", archiveTypeCodes);
        }
        List<String> carrierTypeCodes = resolveCarrierTypeCodes(command);
        if (!carrierTypeCodes.isEmpty()) {
            sql.append(" and carrier_type in (:carrierTypeCodes)");
            params.addValue("carrierTypeCodes", carrierTypeCodes);
        }
        List<String> documentArchiveTypeCodes = resolveDocumentArchiveTypeCodes(command);
        if (!documentArchiveTypeCodes.isEmpty()) {
            sql.append(" and arch_type_code in (:documentArchiveTypeCodes)");
            params.addValue("documentArchiveTypeCodes", documentArchiveTypeCodes);
        }
        List<String> bizTokens = resolveBusinessCodeTokens(command);
        if (bizTokens.size() == 1) {
            sql.append(" and doc_biz_no ilike :bc0");
            params.addValue("bc0", "%" + bizTokens.get(0) + "%");
        } else if (!bizTokens.isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < bizTokens.size(); i++) {
                if (i > 0) {
                    sql.append(" or ");
                }
                sql.append("lower(trim(doc_biz_no)) = lower(:bc").append(i).append(')');
                params.addValue("bc" + i, bizTokens.get(i).trim());
            }
            sql.append(")");
        }
        List<String> invoiceTokens = MultiValueTextParse.parseSpaceSeparatedValues(command.getInvoiceNo());
        if (!invoiceTokens.isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < invoiceTokens.size(); i++) {
                if (i > 0) {
                    sql.append(" or ");
                }
                sql.append("lower(trim(cast(coalesce(attr41, '') as varchar))) = lower(:inv").append(i).append(')');
                params.addValue("inv" + i, invoiceTokens.get(i));
            }
            sql.append(")");
        }
        List<String> refTokens = resolveRefNoTokens(command);
        if (!refTokens.isEmpty()) {
            sql.append(" and (");
            for (int t = 0; t < refTokens.size(); t++) {
                if (t > 0) {
                    sql.append(" or ");
                }
                String pname = "ref" + t;
                sql.append("(lower(trim(cast(coalesce(attr42, '') as varchar))) = lower(:")
                    .append(pname).append(") or lower(trim(cast(coalesce(attr43, '') as varchar))) = lower(:")
                    .append(pname).append(") or lower(trim(cast(coalesce(attr44, '') as varchar))) = lower(:")
                    .append(pname).append(") or lower(trim(cast(coalesce(attr45, '') as varchar))) = lower(:")
                    .append(pname).append(") or lower(trim(cast(coalesce(attr46, '') as varchar))) = lower(:")
                    .append(pname).append("))");
                params.addValue(pname, refTokens.get(t));
            }
            sql.append(")");
        }
        if (hasText(command.getDocOrganization())) {
            sql.append(" and doc_organization_code = :docOrganization");
            params.addValue("docOrganization", command.getDocOrganization().trim());
        }
        if (hasText(command.getBeginPeriod())) {
            sql.append(" and to_char(start_period, 'YYYY-MM') >= :beginPeriod");
            params.addValue("beginPeriod", command.getBeginPeriod().trim());
        }
        if (hasText(command.getEndPeriod())) {
            sql.append(" and to_char(end_period, 'YYYY-MM') <= :endPeriod");
            params.addValue("endPeriod", command.getEndPeriod().trim());
        }
        if (hasText(command.getDocGenerationStart())) {
            sql.append(" and doc_gen_date >= :docGenerationStart::timestamp");
            params.addValue("docGenerationStart", command.getDocGenerationStart().trim() + " 00:00:00");
        }
        if (hasText(command.getDocGenerationEnd())) {
            sql.append(" and doc_gen_date <= :docGenerationEnd::timestamp");
            params.addValue("docGenerationEnd", command.getDocGenerationEnd().trim() + " 23:59:59");
        }
        if (hasText(command.getCountry())) {
            sql.append(" and cp.country_code = :country");
            params.addValue("country", command.getCountry().trim());
        }
        if (hasText(command.getRepOffice())) {
            sql.append(" and geo.rep_office_name = :repOffice");
            params.addValue("repOffice", command.getRepOffice().trim());
        }
        if (hasText(command.getRegion())) {
            sql.append(" and geo.region_name = :region");
            params.addValue("region", command.getRegion().trim());
        }
        if (hasText(command.getArchiveDestination())) {
            sql.append(" and arch_place_alpha2_code = :archiveDestination");
            params.addValue("archiveDestination", command.getArchiveDestination().trim());
        }
        if (hasText(command.getOriginPlace())) {
            sql.append(" and origin_place_alpha2_code = :originPlace");
            params.addValue("originPlace", command.getOriginPlace().trim());
        }
        if (hasText(command.getDutyPerson())) {
            sql.append("""
                and (
                      cast(fdc_document_t.doc_resp_person_id as varchar) ilike :dutyPerson
                   or coalesce(owner.user_name, '') ilike :dutyPerson
                   or coalesce(owner.employee_no, '') ilike :dutyPerson
                   or concat_ws(' ', coalesce(owner.user_name, ''), coalesce(owner.employee_no, '')) ilike :dutyPerson
                )
                """);
            params.addValue("dutyPerson", "%" + command.getDutyPerson().trim() + "%");
        }
        if (command.getBarcodeModuleCodes() != null && !command.getBarcodeModuleCodes().isEmpty()) {
            sql.append("""
                 and exists (
                   select 1 from fdc_business_module_t bm_bar
                    where bm_bar.module_code = fdc_document_t.biz_module_code
                      and coalesce(bm_bar.delete_flag, 'N') = 'N'
                      and bm_bar.barcode_module_code in (:barcodeModuleCodes)
                 )
                """);
            params.addValue("barcodeModuleCodes", command.getBarcodeModuleCodes().stream()
                .filter(StringUtils::hasText)
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .toList());
        }
        sql.append(" order by fdc_document_t.doc_id desc");

        List<PendingDocumentRowResponse> rows = namedParameterJdbcTemplate.query(sql.toString(), params, (ResultSet rs, int rowNum) -> {
            LocalDate startPeriod = rs.getObject("start_period", LocalDate.class);
            LocalDate endPeriod = rs.getObject("end_period", LocalDate.class);
            LocalDateTime docGen = rs.getObject("doc_gen_date", LocalDateTime.class);
            LocalDateTime creationDate = rs.getObject("creation_date", LocalDateTime.class);
            LocalDateTime updateDate = rs.getObject("last_update_date", LocalDateTime.class);
            String bizModuleCode = rs.getString("biz_module_code");
            SecurityLevelResolver.Resolved secLv = securityLevelResolver.resolve(rs.getString("security_level"));
            long rowDocId = rs.getLong("doc_id");
            String lifecycle = rs.getString("lifecycle_status");
            String rawBiz = rs.getString("doc_biz_no");
            return PendingDocumentRowResponse.builder()
                .docId(String.valueOf(rowDocId))
                .businessCode(normalizeDraftBizNoForList(rowDocId, rawBiz, lifecycle))
                .companyEntity(rs.getString("company_name"))
                .businessModule(resolveBusinessModuleName(bizModuleCode, documentTypeNameMap))
                .startPeriod(startPeriod == null ? null : startPeriod.toString().substring(0, 7))
                .endPeriod(endPeriod == null ? null : endPeriod.toString().substring(0, 7))
                .archivePlace(rs.getString("arch_place_alpha2_code"))
                .originPlace(rs.getString("origin_place_alpha2_code"))
                .docOrganization(rs.getString("doc_organization_code"))
                .docStatus("DRAFT".equalsIgnoreCase(rs.getString("lifecycle_status")) ? "草稿" : "未归档")
                .documentName(rs.getString("doc_name"))
                .docGenerationDate(formatDateTime(docGen))
                .owner(rs.getString("owner_name"))
                .responsibleDept(String.valueOf(rs.getObject("doc_resp_dept_id")))
                .carrierType(carrierTypeNameMap.getOrDefault(rs.getString("carrier_type"), rs.getString("carrier_type")))
                .visibility(DocumentVisibilitySupport.toDisplayYesNo(rs.getString("visible_flag")))
                .sourceSystem(rs.getString("source_system"))
                .securityLevelCode(secLv.canonicalCode())
                .securityLevelName(secLv.displayName())
                .securityLevel(secLv.displayName())
                .description(rs.getString("description"))
                .creationTime(formatDateTime(creationDate))
                .createdBy(rs.getString("created_by_name"))
                .updatedBy(rs.getString("updated_by_name"))
                .updatedAt(formatDateTime(updateDate))
                .build();
        });

        if (!draftOwnerQuery && rows != null && !rows.isEmpty()) {
            rows = rows.stream()
                .filter(r -> r.getDocStatus() == null || !"草稿".equals(r.getDocStatus()))
                .toList();
        }

        return ApiResponse.success(rows == null ? new ArrayList<>() : rows);
    }

    private List<String> resolveArchiveTypeCodes(PendingDocumentQueryCommand command) {
        List<String> out = new ArrayList<>();
        if (command.getArchiveTypeCodes() != null) {
            command.getArchiveTypeCodes().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .forEach(out::add);
        }
        if (out.isEmpty() && hasText(command.getArchiveTypeCode())) {
            out.add(command.getArchiveTypeCode().trim());
        }
        return out;
    }

    private List<String> resolveCarrierTypeCodes(PendingDocumentQueryCommand command) {
        List<String> out = new ArrayList<>();
        if (command.getCarrierTypes() != null) {
            command.getCarrierTypes().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .forEach(out::add);
        }
        if (out.isEmpty() && hasText(command.getCarrierType())) {
            out.add(command.getCarrierType().trim());
        }
        return out;
    }

    private List<String> resolveDocumentArchiveTypeCodes(PendingDocumentQueryCommand command) {
        if (command.getDocumentArchiveTypeCodes() == null) {
            return List.of();
        }
        return command.getDocumentArchiveTypeCodes().stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
    }

    private void validateDocumentTypeArchiveTypeConsistency(PendingDocumentQueryCommand command) {
        String docType = trimToNull(command.getDocumentTypeCode());
        if (!StringUtils.hasText(docType)) {
            return;
        }
        List<String> codes = resolveArchiveTypeCodes(command);
        if (codes.isEmpty()) {
            return;
        }
        for (String archiveType : codes) {
            validateSingleArchiveUnderDocumentType(docType, archiveType);
        }
    }

    private void validateSingleArchiveUnderDocumentType(String docType, String archiveType) {
        BusinessModule module = businessModuleMapper.selectOne(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getModuleCode, archiveType)
            .eq(BusinessModule::getDeleteFlag, "N")
            .last("limit 1"));
        if (module == null) {
            return;
        }
        if (docType.equals(archiveType)) {
            return;
        }
        String ancestorPath = trimToNull(module.getAncestorPath());
        if (!StringUtils.hasText(ancestorPath)) {
            throw new BusinessException("archiveTypeCode must belong to the selected documentTypeCode");
        }
        List<String> ancestors = Arrays.stream(ancestorPath.split("/"))
            .map(String::trim)
            .filter(StringUtils::hasText)
            .toList();
        if (!ancestors.contains(docType)) {
            throw new BusinessException("archiveTypeCode must belong to the selected documentTypeCode");
        }
    }

    /**
     * 「我的草稿」：fdc_pending_document_draft_t，载荷为 JSON，不混用 fdc_document_t。
     */
    private List<PendingDocumentRowResponse> queryDraftTableRows(
        PendingDocumentQueryCommand command,
        Map<String, String> documentTypeNameMap,
        Map<String, String> carrierTypeNameMap
    ) {
        StringBuilder sql = new StringBuilder(
            """
            select d.draft_id,
                   d.payload_json::text as payload_json,
                   d.creation_date,
                   d.last_update_date,
                   d.created_by,
                   coalesce(
                    nullif(trim(concat_ws(' ', nullif(creator.user_name, ''), nullif(creator.employee_no, ''))), ''),
                     cast(d.created_by as varchar)
                   ) as created_by_name,
                   cp.company_project_name,
                   cp.country_code,
                   geo.rep_office_name,
                   geo.region_name
              from fdc_pending_document_draft_t d
              left join tpl_user_t creator on creator.user_id = d.created_by
              left join fdc_company_project_t cp                on cp.company_project_code = (d.payload_json->>'companyProjectCode') and cp.delete_flag = 'N'
              left join (
                    select country_code, min(rep_office_name) as rep_office_name, min(region_name) as region_name
                      from fdc_geo_region_t
                     where enable_flag = 'Y' and delete_flag = 'N'
                     group by country_code ) geo on geo.country_code = cp.country_code
             where coalesce(d.delete_flag, 0) = 0
               and d.created_by = :createdByUserId
            """
        );
        MapSqlParameterSource params = new MapSqlParameterSource("createdByUserId", command.getCreatedByUserId());
        List<String> draftBiz = resolveBusinessCodeTokens(command);
        if (draftBiz.size() == 1) {
            sql.append(" and coalesce(d.payload_json->>'businessCode','') ilike :dbc0");
            params.addValue("dbc0", "%" + draftBiz.get(0) + "%");
        } else if (!draftBiz.isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < draftBiz.size(); i++) {
                if (i > 0) {
                    sql.append(" or ");
                }
                sql.append("lower(trim(coalesce(d.payload_json->>'businessCode',''))) = lower(:dbc").append(i).append(')');
                params.addValue("dbc" + i, draftBiz.get(i).trim());
            }
            sql.append(")");
        }
        List<String> draftInv = MultiValueTextParse.parseSpaceSeparatedValues(command.getInvoiceNo());
        if (!draftInv.isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < draftInv.size(); i++) {
                if (i > 0) {
                    sql.append(" or ");
                }
                sql.append("lower(trim(coalesce(d.payload_json->'extValues'->>'invoiceNo',''))) = lower(:dinv").append(i).append(')');
                params.addValue("dinv" + i, draftInv.get(i));
            }
            sql.append(")");
        }
        List<String> draftRef = resolveRefNoTokens(command);
        if (!draftRef.isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < draftRef.size(); i++) {
                if (i > 0) {
                    sql.append(" or ");
                }
                sql.append("trim(coalesce(d.payload_json->'extValues'->>'refNo','')) ilike :drf").append(i);
                params.addValue("drf" + i, "%" + draftRef.get(i) + "%");
            }
            sql.append(")");
        }
        if (hasText(command.getCompanyCode())) {
            sql.append(" and (d.payload_json->>'companyProjectCode') = :companyCode");
            params.addValue("companyCode", command.getCompanyCode().trim());
        }
        List<String> draftArchiveCodes = resolveArchiveTypeCodes(command);
        if (!draftArchiveCodes.isEmpty()) {
            sql.append(" and (d.payload_json->>'archiveTypeCode') in (:draftArchiveTypeCodes)");
            params.addValue("draftArchiveTypeCodes", draftArchiveCodes);
        }
        if (hasText(command.getDocumentTypeCode())) {
            sql.append(" and (d.payload_json->>'documentTypeCode') = :documentTypeCode");
            params.addValue("documentTypeCode", command.getDocumentTypeCode().trim());
        }
        List<String> draftCarriers = resolveCarrierTypeCodes(command);
        if (!draftCarriers.isEmpty()) {
            sql.append(" and (d.payload_json->>'carrierTypeCode') in (:draftCarrierTypeCodes)");
            params.addValue("draftCarrierTypeCodes", draftCarriers);
        }
        List<String> draftDocArchiveTypes = resolveDocumentArchiveTypeCodes(command);
        if (!draftDocArchiveTypes.isEmpty()) {
            sql.append(" and coalesce(d.payload_json->'extValues'->>'archiveType','') in (:draftDocumentArchiveTypeCodes)");
            params.addValue("draftDocumentArchiveTypeCodes", draftDocArchiveTypes);
        }
        if (hasText(command.getDocOrganization())) {
            sql.append(" and (d.payload_json->>'documentOrganizationCode') = :docOrganization");
            params.addValue("docOrganization", command.getDocOrganization().trim());
        }
        if (hasText(command.getBeginPeriod())) {
            sql.append(" and left(trim(coalesce(d.payload_json->>'beginPeriod','')), 7) >= :beginPeriod");
            params.addValue("beginPeriod", command.getBeginPeriod().trim());
        }
        if (hasText(command.getEndPeriod())) {
            sql.append(" and left(trim(coalesce(d.payload_json->>'endPeriod','')), 7) <= :endPeriod");
            params.addValue("endPeriod", command.getEndPeriod().trim());
        }
        if (hasText(command.getCountry())) {
            sql.append(" and cp.country_code = :country");
            params.addValue("country", command.getCountry().trim());
        }
        if (hasText(command.getRepOffice())) {
            sql.append(" and geo.rep_office_name = :repOffice");
            params.addValue("repOffice", command.getRepOffice().trim());
        }
        if (hasText(command.getRegion())) {
            sql.append(" and geo.region_name = :region");
            params.addValue("region", command.getRegion().trim());
        }
        if (hasText(command.getArchiveDestination())) {
            sql.append(" and coalesce(d.payload_json->>'archiveDestination','') = :draftArchiveDestination");
            params.addValue("draftArchiveDestination", command.getArchiveDestination().trim());
        }
        if (hasText(command.getOriginPlace())) {
            sql.append(" and coalesce(d.payload_json->>'originPlace','') = :draftOriginPlace");
            params.addValue("draftOriginPlace", command.getOriginPlace().trim());
        }
        sql.append(" order by d.draft_id desc");

        return namedParameterJdbcTemplate.query(sql.toString(), params, (ResultSet rs, int rowNum) -> {
            long draftId = rs.getLong("draft_id");
            PendingDocumentWriteCommand p;
            try {
                p = objectMapper.readValue(rs.getString("payload_json"), PendingDocumentWriteCommand.class);
            } catch (Exception e) {
                p = new PendingDocumentWriteCommand();
            }
            String bizModuleCode = hasText(p.getArchiveTypeCode()) ? p.getArchiveTypeCode().trim() : "";
            LocalDateTime docGen = null;
            if (hasText(p.getDocumentDate())) {
                try {
                    String raw = p.getDocumentDate().trim();
                    docGen = LocalDateTime.parse(raw.replace(" ", "T"));
                } catch (Exception e1) {
                    try {
                        docGen = LocalDateTime.parse(p.getDocumentDate().trim(), DATE_TIME_FORMATTER);
                    } catch (Exception ignored) {
                        docGen = null;
                    }
                }
            }
            String beginYm = hasText(p.getBeginPeriod()) && p.getBeginPeriod().trim().length() >= 7
                ? p.getBeginPeriod().trim().substring(0, 7)
                : (hasText(p.getBeginPeriod()) ? p.getBeginPeriod().trim() : null);
            String endYm = hasText(p.getEndPeriod()) && p.getEndPeriod().trim().length() >= 7
                ? p.getEndPeriod().trim().substring(0, 7)
                : (hasText(p.getEndPeriod()) ? p.getEndPeriod().trim() : null);
            SecurityLevelResolver.Resolved secLv = securityLevelResolver.resolve(
                hasText(p.getSecurityLevelCode()) ? p.getSecurityLevelCode().trim() : ""
            );
            Map<String, String> ext = p.getExtValues() == null ? Map.of() : p.getExtValues();
            String visibility = hasText(ext.get("visibility")) ? ext.get("visibility").trim() : "是";
            String rawBiz = hasText(p.getBusinessCode()) ? p.getBusinessCode().trim() : "";
            return PendingDocumentRowResponse.builder()
                .docId(String.valueOf(draftId))
                .businessCode(normalizeDraftBizNoForList(draftId, rawBiz, "DRAFT"))
                .companyEntity(rs.getString("company_project_name"))
                .businessModule(resolveBusinessModuleName(bizModuleCode, documentTypeNameMap))
                .startPeriod(beginYm)
                .endPeriod(endYm)
                .archivePlace(hasText(p.getArchiveDestination()) ? p.getArchiveDestination().trim() : null)
                .originPlace(hasText(p.getOriginPlace()) ? p.getOriginPlace().trim() : null)
                .docOrganization(hasText(p.getDocumentOrganizationCode()) ? p.getDocumentOrganizationCode().trim() : null)
                .docStatus("草稿")
                .documentName(hasText(p.getDocumentName()) ? p.getDocumentName().trim() : null)
                .docGenerationDate(formatDateTime(docGen))
                .owner(hasText(p.getDutyPerson()) ? p.getDutyPerson().trim() : null)
                .responsibleDept(hasText(p.getDutyDepartment()) ? p.getDutyDepartment().trim() : null)
                .carrierType(carrierTypeNameMap.getOrDefault(
                    hasText(p.getCarrierTypeCode()) ? p.getCarrierTypeCode().trim() : "",
                    hasText(p.getCarrierTypeCode()) ? p.getCarrierTypeCode().trim() : ""
                ))
                .visibility(visibility)
                .sourceSystem(hasText(p.getSourceSystem()) ? p.getSourceSystem().trim() : null)
                .securityLevelCode(secLv.canonicalCode())
                .securityLevelName(secLv.displayName())
                .securityLevel(secLv.displayName())
                .description(hasText(p.getRemark()) ? p.getRemark().trim() : null)
                .creationTime(formatDateTime(rs.getObject("creation_date", LocalDateTime.class)))
                .createdBy(rs.getString("created_by_name"))
                .updatedBy(rs.getString("created_by_name"))
                .updatedAt(formatDateTime(rs.getObject("last_update_date", LocalDateTime.class)))
                .build();
        });
    }

    /**
     * 草稿未填业务编码时库内为 PENDING-{docId}（或历史 DRAFT 占位）；列表与详情对外展示为空串。
     */
    private static String normalizeDraftBizNoForList(long docId, String docBizNo, String lifecycleStatus) {
        if (lifecycleStatus == null || !"DRAFT".equalsIgnoreCase(lifecycleStatus.trim())) {
            return docBizNo;
        }
        if (docBizNo == null) {
            return "";
        }
        String v = docBizNo.trim();
        if (v.isEmpty()) {
            return "";
        }
        if (("PENDING-" + docId).equalsIgnoreCase(v)) {
            return "";
        }
        if ("DRAFT".equalsIgnoreCase(v)) {
            return "";
        }
        return docBizNo;
    }

    /**
     * 优先使用请求体中的 {@code businessCodes} 数组；否则解析 {@code businessCode} 文本（换行/逗号等）。
     */
    private List<String> resolveBusinessCodeTokens(PendingDocumentQueryCommand command) {
        if (command.getBusinessCodes() != null && !command.getBusinessCodes().isEmpty()) {
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (String s : command.getBusinessCodes()) {
                if (s == null) {
                    continue;
                }
                String t = MultiValueTextParse.normalizeToken(s);
                if (StringUtils.hasText(t)) {
                    seen.add(t);
                }
            }
            if (seen.size() > MultiValueTextParse.MAX_VALUES_PER_FIELD) {
                throw new BusinessException(
                    "单个筛选条件最多支持 " + MultiValueTextParse.MAX_VALUES_PER_FIELD + " 个值（请换行输入，每行一条）");
            }
            return new ArrayList<>(seen);
        }
        return MultiValueTextParse.parseSpaceSeparatedValues(command.getBusinessCode());
    }

    private List<String> resolveRefNoTokens(PendingDocumentQueryCommand command) {
        if (command.getRefNos() != null && !command.getRefNos().isEmpty()) {
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (String s : command.getRefNos()) {
                if (s == null) {
                    continue;
                }
                String t = MultiValueTextParse.normalizeToken(s);
                if (StringUtils.hasText(t)) {
                    seen.add(t);
                }
            }
            if (seen.size() > MultiValueTextParse.MAX_VALUES_PER_FIELD) {
                throw new BusinessException(
                    "单个筛选条件最多支持 " + MultiValueTextParse.MAX_VALUES_PER_FIELD + " 个值（请换行输入，每行一条）");
            }
            return new ArrayList<>(seen);
        }
        return MultiValueTextParse.parseSpaceSeparatedValues(command.getRefNo());
    }

    private void runPendingImportQueryJob(long jobId, byte[] content, String documentTypeCode, long operatorUserId) {
        long t0 = System.currentTimeMillis();
        int inputTotal = 0;
        int successRows = 0;
        List<String> failed = new ArrayList<>();
        List<String[]> parsedRows = new ArrayList<>();
        List<Integer> matchedCounts = new ArrayList<>();
        List<String> rowErrors = new ArrayList<>();
        try {
            String text = new String(content, StandardCharsets.UTF_8);
            List<String> lines = Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
            if (lines.size() <= 1) {
                updateImportQueryJob(jobId, operatorUserId, 0, 0, System.currentTimeMillis() - t0, "FAILED", "CSV 内容为空", null, null);
                return;
            }
            String[] headers = parseCsvLine(lines.get(0));
            Map<String, Integer> idx = buildImportQueryHeaderIndex(headers);
            requireImportQueryHeaders(idx);
            Map<Long, PendingDocumentRowResponse> dedup = new LinkedHashMap<>();
            int rowNo = 0;
            for (int i = 1; i < lines.size(); i++) {
                rowNo++;
                String[] cols = parseCsvLine(lines.get(i));
                parsedRows.add(cols);
                inputTotal++;
                String businessCode = csvVal(cols, idx.get("businessCode"));
                String invoiceNo = csvVal(cols, idx.get("invoiceNo"));
                String refNo = csvVal(cols, idx.get("refNo"));
                String companyCode = csvVal(cols, idx.get("companyCode"));
                String archiveTypeCode = csvVal(cols, idx.get("archiveTypeCode"));
                String beginPeriod = csvVal(cols, idx.get("beginPeriod"));
                if (!StringUtils.hasText(businessCode) && !StringUtils.hasText(invoiceNo) && !StringUtils.hasText(refNo)) {
                    String err = "文档业务编码/发票号/其他相关编号至少填写一项";
                    failed.add("第" + (i + 1) + "行：" + err);
                    matchedCounts.add(0);
                    rowErrors.add(err);
                    continue;
                }
                try {
                    PendingDocumentQueryCommand cmd = new PendingDocumentQueryCommand();
                    cmd.setDocumentTypeCode(documentTypeCode);
                    cmd.setCompanyCode(trimToNull(companyCode));
                    cmd.setArchiveTypeCode(trimToNull(archiveTypeCode));
                    cmd.setBusinessCode(trimToNull(businessCode));
                    cmd.setInvoiceNo(trimToNull(invoiceNo));
                    cmd.setRefNos(StringUtils.hasText(refNo) ? List.of(refNo.trim()) : null);
                    cmd.setBeginPeriod(trimToNull(beginPeriod));
                    cmd.setEndPeriod(trimToNull(beginPeriod));
                    List<PendingDocumentRowResponse> matched = query(cmd).getData();
                    if (StringUtils.hasText(businessCode)) {
                        matched = matched.stream()
                            .filter(r -> StringUtils.hasText(r.getBusinessCode())
                                && businessCode.trim().equalsIgnoreCase(r.getBusinessCode().trim()))
                            .toList();
                    }
                    matchedCounts.add(matched.size());
                    rowErrors.add("");
                    for (PendingDocumentRowResponse r : matched) {
                        if (!StringUtils.hasText(r.getDocId())) continue;
                        try {
                            dedup.putIfAbsent(Long.valueOf(r.getDocId()), r);
                        } catch (Exception ignored) {
                            // ignore invalid id
                        }
                    }
                    successRows++;
                } catch (Exception ex) {
                    String err = ex.getMessage() == null ? "查询失败" : ex.getMessage();
                    failed.add("第" + (i + 1) + "行：" + err);
                    matchedCounts.add(0);
                    rowErrors.add(err);
                }
            }
            List<PendingDocumentRowResponse> results = new ArrayList<>(dedup.values());
            persistImportQueryResults(jobId, operatorUserId, results, rowNo);
            String failedCsv = buildImportQueryAnnotatedCsv(headers, parsedRows, matchedCounts, rowErrors);
            String resultCsv = buildPendingImportQueryResultCsv(results);
            String status = failed.isEmpty() ? "SUCCESS" : (successRows > 0 ? "PARTIAL_FAILED" : "FAILED");
            String err = failed.isEmpty() ? null : ("存在 " + failed.size() + " 行失败");
            updateImportQueryJob(jobId, operatorUserId, inputTotal, results.size(), System.currentTimeMillis() - t0, status, err, failedCsv, resultCsv);
        } catch (Exception ex) {
            updateImportQueryJob(jobId, operatorUserId, inputTotal, 0, System.currentTimeMillis() - t0, "FAILED", ex.getMessage(), null, null);
        }
    }

    private void persistImportQueryResults(long jobId, long operatorUserId, List<PendingDocumentRowResponse> rows, int defaultRowNo) {
        jdbcTemplate.update("delete from fdc_workspace_import_query_result_t where job_id = ?", jobId);
        if (rows != null && !rows.isEmpty()) {
            jdbcTemplate.batchUpdate(
                """
                insert into fdc_workspace_import_query_result_t
                  (job_id, query_row_no, archive_id, doc_id, business_code, document_name, doc_status, lifecycle_status, created_by, creation_date, tenantid)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, 1)
                """,
                rows,
                200,
                (ps, r) -> {
                    long id = Long.parseLong(r.getDocId());
                    ps.setLong(1, jobId);
                    ps.setInt(2, defaultRowNo);
                    ps.setLong(3, id);
                    ps.setString(4, r.getDocId());
                    ps.setString(5, r.getBusinessCode());
                    ps.setString(6, r.getDocumentName());
                    ps.setString(7, r.getDocStatus());
                    ps.setString(8, r.getDocStatus());
                    ps.setLong(9, operatorUserId);
                }
            );
        }
        jdbcTemplate.update("delete from fdc_workspace_import_query_result_t where creation_date < current_timestamp - interval '90 days'");
    }

    private void updateImportQueryJob(
        long jobId,
        long operatorUserId,
        int inputTotal,
        int resultTotal,
        long durationMs,
        String status,
        String errorMessage,
        String failedFileCsv,
        String resultArtifactText
    ) {
        jdbcTemplate.update(
            """
            update fdc_workspace_io_job_t
               set input_total = ?,
                   result_total = ?,
                   duration_ms = ?,
                   job_status = ?,
                   error_message = ?,
                   failed_file_csv = ?,
                   result_artifact_text = ?,
                   artifact_expires_at = case when ? is null or ? = '' then artifact_expires_at else current_timestamp + interval '7 days' end,
                   last_updated_by = ?,
                   last_update_date = current_timestamp
             where job_id = ?
            """,
            inputTotal, resultTotal, durationMs, status, errorMessage, failedFileCsv, resultArtifactText, resultArtifactText, resultArtifactText, operatorUserId, jobId
        );
    }

    private static String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (ch == ',' && !inQuote) {
                out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    private static String csvVal(String[] cols, Integer idx) {
        if (idx == null || idx < 0 || idx >= cols.length) return "";
        return cols[idx] == null ? "" : cols[idx].trim();
    }

    private static Map<String, Integer> buildImportQueryHeaderIndex(String[] headers) {
        Map<String, Integer> idx = new LinkedHashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i] == null ? "" : headers[i].trim();
            if ("文档业务编码".equals(h) || "businessCode".equalsIgnoreCase(h)) idx.put("businessCode", i);
            else if ("发票号".equals(h) || "invoiceNo".equalsIgnoreCase(h)) idx.put("invoiceNo", i);
            else if ("其他相关编号".equals(h) || "refNo".equalsIgnoreCase(h)) idx.put("refNo", i);
            else if ("公司".equals(h) || "companyCode".equalsIgnoreCase(h) || "companyProjectCode".equalsIgnoreCase(h)) idx.put("companyCode", i);
            else if ("业务模块".equals(h) || "archiveTypeCode".equalsIgnoreCase(h)) idx.put("archiveTypeCode", i);
            else if ("开始档期".equals(h) || "beginPeriod".equalsIgnoreCase(h)) idx.put("beginPeriod", i);
        }
        return idx;
    }

    private static void requireImportQueryHeaders(Map<String, Integer> idx) {
        for (String k : List.of("companyCode", "archiveTypeCode", "beginPeriod")) {
            if (!idx.containsKey(k)) {
                throw new BusinessException("模板缺少必填列：公司、业务模块、开始档期");
            }
        }
        if (!idx.containsKey("businessCode") && !idx.containsKey("invoiceNo") && !idx.containsKey("refNo")) {
            throw new BusinessException("模板须至少包含以下之一：文档业务编码、发票号、其他相关编号");
        }
    }

    private static String buildImportQueryAnnotatedCsv(String[] headers, List<String[]> rows, List<Integer> matchedCounts, List<String> rowErrors) {
        List<String> out = new ArrayList<>();
        List<String> hdr = new ArrayList<>(Arrays.asList(headers));
        hdr.add("本行命中条数");
        hdr.add("失败原因");
        out.add(String.join(",", hdr));
        for (int i = 0; i < rows.size(); i++) {
            List<String> cols = new ArrayList<>(Arrays.asList(rows.get(i)));
            cols.add(String.valueOf(i < matchedCounts.size() ? matchedCounts.get(i) : 0));
            cols.add(i < rowErrors.size() ? rowErrors.get(i) : "");
            out.add(cols.stream().map(v -> "\"" + String.valueOf(v).replace("\"", "\"\"") + "\"").collect(Collectors.joining(",")));
        }
        return String.join("\n", out);
    }

    private static String buildPendingImportQueryResultCsv(List<PendingDocumentRowResponse> rows) {
        List<String> lines = new ArrayList<>();
        lines.add("文档ID,文档业务编码,公司,业务模块,开始档期,结束档期,归档地,产生地,文档组织,文档状态,文档名称,文档生成日期,归档责任人,文档责任部门,载体类型,是否可见,系统来源,密级,描述,创建时间,创建人");
        for (PendingDocumentRowResponse r : rows) {
            lines.add(Stream.of(
                    r.getDocId(), r.getBusinessCode(), r.getCompanyEntity(), r.getBusinessModule(), r.getStartPeriod(), r.getEndPeriod(),
                    r.getArchivePlace(), r.getOriginPlace(), r.getDocOrganization(), r.getDocStatus(), r.getDocumentName(), r.getDocGenerationDate(),
                    r.getOwner(), r.getResponsibleDept(), r.getCarrierType(), r.getVisibility(), r.getSourceSystem(), r.getSecurityLevelName(),
                    r.getDescription(), r.getCreationTime(), r.getCreatedBy()
                ).map(v -> "\"" + String.valueOf(v == null ? "" : v).replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(",")));
        }
        return String.join("\n", lines);
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }

    private String resolveBusinessModuleName(String moduleCode, Map<String, String> nameMap) {
        if (!hasText(moduleCode)) {
            return moduleCode;
        }
        String exact = nameMap.get(moduleCode);
        if (hasText(exact)) {
            return exact;
        }
        String bestPrefixCode = null;
        for (String code : nameMap.keySet()) {
            if (moduleCode.startsWith(code) && (bestPrefixCode == null || code.length() > bestPrefixCode.length())) {
                bestPrefixCode = code;
            }
        }
        return bestPrefixCode == null ? moduleCode : nameMap.getOrDefault(bestPrefixCode, moduleCode);
    }

    private Map<String, String> loadCarrierTypeNameMap() {
        return namedParameterJdbcTemplate.query(
            "select item_code, item_name from fdc_dict_item_t where category_code = 'ARCHIVE_CARRIER_TYPE' and enable_flag = 'Y' and delete_flag = 'N'",
            rs -> {
                Map<String, String> map = new java.util.LinkedHashMap<>();
                while (rs.next()) {
                    map.put(rs.getString("item_code"), rs.getString("item_name"));
                }
                return map;
            }
        );
    }

}
