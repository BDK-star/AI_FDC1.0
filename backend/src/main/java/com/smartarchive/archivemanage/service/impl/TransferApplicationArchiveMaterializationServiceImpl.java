package com.smartarchive.archivemanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartarchive.archiveflow.domain.ArchiveFlowRule;
import com.smartarchive.archiveflow.domain.SecurityLevelDictionary;
import com.smartarchive.archiveflow.mapper.ArchiveFlowLookupMapper;
import com.smartarchive.archiveflow.mapper.ArchiveFlowRuleMapper;
import com.smartarchive.archiveflow.mapper.SecurityLevelDictionaryMapper;
import com.smartarchive.archivemanage.domain.ArchiveExtValue;
import com.smartarchive.archivemanage.domain.ArchivePaper;
import com.smartarchive.archivemanage.domain.ArchiveRecord;
import com.smartarchive.archivemanage.domain.TransferApplication;
import com.smartarchive.archivemanage.domain.TransferApplicationDetail;
import com.smartarchive.archivemanage.dto.ArchiveDefaultResolveResponse;
import com.smartarchive.archivemanage.dto.DocumentTypeExtFieldResponse;
import com.smartarchive.archivemanage.mapper.ArchiveExtValueMapper;
import com.smartarchive.archivemanage.mapper.ArchivePaperMapper;
import com.smartarchive.archivemanage.mapper.ArchiveRecordMapper;
import com.smartarchive.archivemanage.mapper.TransferApplicationDetailMapper;
import com.smartarchive.archivemanage.mapper.TransferApplicationExtMapper;
import com.smartarchive.archivemanage.mapper.TransferApplicationMapper;
import com.smartarchive.archivemanage.service.TransferApplicationArchiveMaterializationService;
import com.smartarchive.archivemanage.service.TransferBusinessModuleExtFieldService;
import com.smartarchive.businessmodule.domain.BusinessModuleExtField;
import com.smartarchive.businessmodule.mapper.BusinessModuleExtFieldMapper;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.companyproject.domain.CompanyProject;
import com.smartarchive.companyproject.mapper.CompanyProjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class TransferApplicationArchiveMaterializationServiceImpl implements TransferApplicationArchiveMaterializationService {

    private static final Long SYSTEM_OPERATOR_ID = 1L;
    private static final Pattern ATTR_COLUMN_PATTERN = Pattern.compile("^attr([1-9][0-9]{0,2})$", Pattern.CASE_INSENSITIVE);

    private final TransferApplicationMapper transferApplicationMapper;
    private final TransferApplicationDetailMapper transferApplicationDetailMapper;
    private final TransferApplicationExtMapper transferApplicationExtMapper;
    private final ArchiveRecordMapper archiveRecordMapper;
    private final ArchiveExtValueMapper archiveExtValueMapper;
    private final ArchivePaperMapper archivePaperMapper;
    private final ArchiveFlowRuleMapper archiveFlowRuleMapper;
    private final TransferBusinessModuleExtFieldService transferBusinessModuleExtFieldService;
    private final BusinessModuleExtFieldMapper businessModuleExtFieldMapper;
    private final CompanyProjectMapper companyProjectMapper;
    private final SecurityLevelDictionaryMapper securityLevelDictionaryMapper;
    private final ArchiveFlowLookupMapper archiveFlowLookupMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void materializeAfterApproval(Long applicationId) {
        TransferApplication application = requireApplication(applicationId);
        if ("Y".equalsIgnoreCase(trimToEmpty(application.getArchivesMaterialized()))) {
            return;
        }
        List<TransferApplicationDetail> details = transferApplicationDetailMapper.selectList(
            new LambdaQueryWrapper<TransferApplicationDetail>()
                .eq(TransferApplicationDetail::getApplicationId, applicationId)
                .eq(TransferApplicationDetail::getDeleteFlag, "N")
                .orderByAsc(TransferApplicationDetail::getApplicationDetailId));
        if (details.isEmpty()) {
            throw new BusinessException("移交申请无有效明细，无法写入档案");
        }
        List<Map<String, Object>> extRows =
            transferApplicationExtMapper.selectByMasterId(applicationId, application.getTenantid());
        Map<Long, Map<String, String>> extByDetailId = decodeExtRows(extRows, details);

        for (TransferApplicationDetail detail : details) {
            String lineModule = trimToNull(detail.getBusiModuleCode());
            List<DocumentTypeExtFieldResponse> effectiveFields = lineModule == null
                ? List.of()
                : transferBusinessModuleExtFieldService.listEffectiveForTransfer(lineModule);
            syncDocumentForDetail(application, detail, extByDetailId.getOrDefault(detail.getApplicationDetailId(), Map.of()), effectiveFields);
        }

        application.setArchivesMaterialized("Y");
        application.setApplicationStatus("APPROVED");
        application.setStatus("APPROVED");
        application.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        application.setLastUpdateDate(LocalDateTime.now());
        transferApplicationMapper.updateById(application);
    }

    @Override
    @Transactional
    public void markRejected(Long applicationId) {
        TransferApplication application = transferApplicationMapper.selectOne(new LambdaQueryWrapper<TransferApplication>()
            .eq(TransferApplication::getApplicationId, applicationId)
            .eq(TransferApplication::getDeleteFlag, "N")
            .last("limit 1"));
        if (application == null) {
            return;
        }
        application.setApplicationStatus("REJECTED");
        application.setStatus("REJECTED");
        application.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        application.setLastUpdateDate(LocalDateTime.now());
        transferApplicationMapper.updateById(application);
    }

    private TransferApplication requireApplication(Long applicationId) {
        TransferApplication application = transferApplicationMapper.selectOne(new LambdaQueryWrapper<TransferApplication>()
            .eq(TransferApplication::getApplicationId, applicationId)
            .eq(TransferApplication::getDeleteFlag, "N")
            .last("limit 1"));
        if (application == null) {
            throw new BusinessException("移交申请不存在");
        }
        return application;
    }

    private void syncDocumentForDetail(TransferApplication application,
                                       TransferApplicationDetail detail,
                                       Map<String, String> extValues,
                                       List<DocumentTypeExtFieldResponse> effectiveFields) {
        String businessCode = trimToNull(detail.getDocBusiNo());
        if (!StringUtils.hasText(businessCode)) {
            businessCode = "TRN-" + application.getApplicationId() + "-" + detail.getApplicationDetailId();
        }

        CompanyProject project = requireCompanyProject(detail.getCompanyProjectCode());
        ArchiveDefaultResolveResponse defaults = resolveDefaultsForDetail(
            detail.getCompanyProjectCode(),
            application.getBusiModuleCode(),
            trimToNull(detail.getBusiModuleCode()),
            trimToNull(detail.getArchPlaceAlpha2Code()));

        String securityLevelCode = firstNonBlank(defaults.getSecurityLevelCode(), firstSecurityLevelCode());
        String documentOrganizationCode = firstNonBlank(
            trimToNull(detail.getDocumentOrganizationCode()),
            firstNonBlank(defaults.getDocumentOrganizationCode(), firstDocumentOrganizationCode()));
        Integer retentionYears = defaults.getRetentionPeriodYears() != null ? defaults.getRetentionPeriodYears() : 10;
        String countryCode = firstNonBlank(defaults.getCountryCode(), project.getCountryCode());

        LocalDate docDate = detail.getDocGenerationDate() != null ? detail.getDocGenerationDate()
            : (application.getApplicationDate() != null ? application.getApplicationDate().toLocalDate() : LocalDate.now());
        String beginPeriod = toYearMonthPeriod(detail.getStartArchPeriod(), docDate);
        String endPeriod = toYearMonthPeriod(detail.getEndArchPeriod(), docDate);
        String carrierType = resolveCarrierType(firstNonBlank(detail.getCarrierType(), application.getCarrierType()));
        String archiveTypeCode = firstNonBlank(detail.getArchTypeCode(), application.getBusiModuleCode());
        String dutyPerson = application.getApplicant() != null ? "用户-" + application.getApplicant() : "—";
        String dutyDepartment = firstNonBlank(application.getDepartment(), "—");
        String remark = buildRemark(application, detail);
        LocalDateTime now = LocalDateTime.now();
        // fdc_document_t.source_system is VARCHAR(30); keep a short code and put trace in source_id (VARCHAR(500)).
        String sourceSystem = "TRANSFER_APP";
        String sourceIdTrace = fdcDocumentSourceId(application);
        String archiveDestination = firstNonBlank(defaults.getArchiveDestination(), trimToNull(detail.getArchPlaceAlpha2Code()));
        String originPlace = trimToNull(detail.getArchPlaceAlpha2Code());
        String documentName = firstNonBlank(detail.getDocName(), "未命名文档");
        String docOrg = documentOrganizationCode;
        String extVisibleFlag = "1";
        Map<String, Object> attrColumns = buildDocumentAttrColumns(trimToNull(detail.getBusiModuleCode()), extValues);
        Object attr2 = attrColumns.get("attr2");
        Object attr3 = attrColumns.get("attr3");
        Object attr4 = attrColumns.get("attr4");
        Object attr5 = attrColumns.get("attr5");
        Object attr6 = attrColumns.get("attr6");
        Object attr7 = attrColumns.get("attr7");
        Object attr8 = attrColumns.get("attr8");
        Object attr9 = attrColumns.get("attr9");
        Object attr10 = attrColumns.get("attr10");
        Object attr11 = attrColumns.get("attr11");
        Object attr12 = attrColumns.get("attr12");
        Object attr13 = attrColumns.get("attr13");
        Object attr14 = attrColumns.get("attr14");
        Object attr15 = attrColumns.get("attr15");
        Object attr16 = attrColumns.get("attr16");
        Object attr17 = attrColumns.get("attr17");
        Object attr18 = attrColumns.get("attr18");
        Object attr19 = attrColumns.get("attr19");
        Object attr20 = attrColumns.get("attr20");
        Object attr21 = attrColumns.get("attr21");
        Object attr22 = attrColumns.get("attr22");
        Object attr23 = attrColumns.get("attr23");
        Object attr24 = attrColumns.get("attr24");
        Object attr25 = attrColumns.get("attr25");
        Object attr26 = attrColumns.get("attr26");
        Object attr27 = attrColumns.get("attr27");
        Object attr28 = attrColumns.get("attr28");
        Object attr29 = attrColumns.get("attr29");
        Object attr30 = attrColumns.get("attr30");
        Object attr31 = attrColumns.get("attr31");
        Object attr32 = attrColumns.get("attr32");
        Object attr33 = attrColumns.get("attr33");
        Object attr34 = attrColumns.get("attr34");
        Object attr35 = attrColumns.get("attr35");
        Object attr36 = attrColumns.get("attr36");
        Object attr37 = attrColumns.get("attr37");
        Object attr38 = attrColumns.get("attr38");
        Object attr39 = attrColumns.get("attr39");
        Object attr40 = attrColumns.get("attr40");
        Object attr41 = attrColumns.get("attr41");
        Object attr42 = attrColumns.get("attr42");
        Object attr43 = attrColumns.get("attr43");
        Object attr44 = attrColumns.get("attr44");
        Object attr45 = attrColumns.get("attr45");
        Object attr46 = attrColumns.get("attr46");
        Object attr47 = attrColumns.get("attr47");
        Object attr48 = attrColumns.get("attr48");
        Object attr49 = attrColumns.get("attr49");
        Object attr50 = attrColumns.get("attr50");
        Object attr51 = attrColumns.get("attr51");
        Object attr52 = attrColumns.get("attr52");
        Object attr53 = attrColumns.get("attr53");
        Object attr54 = attrColumns.get("attr54");
        Object attr55 = attrColumns.get("attr55");
        Object attr56 = attrColumns.get("attr56");
        Object attr57 = attrColumns.get("attr57");
        Object attr58 = attrColumns.get("attr58");
        Object attr59 = attrColumns.get("attr59");
        Object attr60 = attrColumns.get("attr60");
        Object attr61 = attrColumns.get("attr61");
        Object attr62 = attrColumns.get("attr62");
        Object attr63 = attrColumns.get("attr63");
        Object attr64 = attrColumns.get("attr64");
        Object attr65 = attrColumns.get("attr65");
        Object attr66 = attrColumns.get("attr66");
        Object attr67 = attrColumns.get("attr67");
        Object attr68 = attrColumns.get("attr68");
        Object attr69 = attrColumns.get("attr69");
        Object attr70 = attrColumns.get("attr70");
        Object attr71 = attrColumns.get("attr71");
        Object attr72 = attrColumns.get("attr72");
        Object attr73 = attrColumns.get("attr73");
        Object attr74 = attrColumns.get("attr74");
        Object attr75 = attrColumns.get("attr75");
        Object attr76 = attrColumns.get("attr76");
        Object attr77 = attrColumns.get("attr77");
        Object attr78 = attrColumns.get("attr78");
        Object attr79 = attrColumns.get("attr79");
        Object attr80 = attrColumns.get("attr80");
        Object attr81 = attrColumns.get("attr81");
        Object attr82 = attrColumns.get("attr82");
        Object attr83 = attrColumns.get("attr83");
        Object attr84 = attrColumns.get("attr84");
        Object attr85 = attrColumns.get("attr85");
        Object attr86 = attrColumns.get("attr86");
        Object attr87 = attrColumns.get("attr87");
        Object attr88 = attrColumns.get("attr88");
        Object attr89 = attrColumns.get("attr89");
        Object attr90 = attrColumns.get("attr90");
        Object attr91 = attrColumns.get("attr91");
        Object attr92 = attrColumns.get("attr92");
        Object attr93 = attrColumns.get("attr93");
        Object attr94 = attrColumns.get("attr94");
        Object attr95 = attrColumns.get("attr95");
        Object attr96 = attrColumns.get("attr96");
        Object attr97 = attrColumns.get("attr97");
        Object attr98 = attrColumns.get("attr98");
        Object attr99 = attrColumns.get("attr99");
        Object attr100 = attrColumns.get("attr100");

        Map<String, Object> existing = jdbcTemplate.query(
            """
            select doc_id, arch_place_alpha2_code
              from fdc_document_t
             where coalesce(delete_flag, 0) = 0
               and lower(trim(doc_biz_no)) = lower(trim(?))
             order by doc_id desc
             limit 1
            """,
            rs -> rs.next() ? Map.of(
                "docId", rs.getLong("doc_id"),
                "archiveDestination", rs.getString("arch_place_alpha2_code")
            ) : null,
            businessCode
        );
        if (existing != null) {
            String existingDestination = trimToNull(String.valueOf(existing.get("archiveDestination")));
            if (Objects.equals(existingDestination, trimToNull(archiveDestination))) {
                return;
            }
            Long docId = ((Number) existing.get("docId")).longValue();
            jdbcTemplate.update(
                """
                update fdc_document_t set
                  company_code = ?, company_name = ?, start_period = ?, end_period = ?, biz_module_code = ?, doc_biz_no = ?,
                  doc_gen_date = ?, arch_place_alpha2_code = ?, origin_place_alpha2_code = ?, carrier_type = ?, doc_name = ?,
                  doc_organization_code = ?, doc_resp_dept_id = ?, doc_resp_person_id = ?, rentention_term = ?, security_level = ?,
                  doc_version = ?, source_id = ?, source_system = ?,
                  description = ?, visible_flag = ?, attr2 = ?, attr3 = ?, attr4 = ?, attr5 = ?, attr6 = ?, attr7 = ?, attr8 = ?, attr9 = ?,
                  attr10 = ?, attr11 = ?, attr12 = ?, attr13 = ?, attr14 = ?, attr15 = ?, attr16 = ?, attr17 = ?, attr18 = ?, attr19 = ?,
                  attr20 = ?, attr21 = ?, attr22 = ?, attr23 = ?, attr24 = ?, attr25 = ?, attr26 = ?, attr27 = ?, attr28 = ?, attr29 = ?,
                  attr30 = ?, attr31 = ?, attr32 = ?, attr33 = ?, attr34 = ?, attr35 = ?, attr36 = ?, attr37 = ?, attr38 = ?, attr39 = ?,
                  attr40 = ?, attr41 = ?, attr42 = ?, attr43 = ?, attr44 = ?, attr45 = ?, attr46 = ?, attr47 = ?, attr48 = ?, attr49 = ?,
                  attr50 = ?, attr51 = ?, attr52 = ?, attr53 = ?, attr54 = ?, attr55 = ?, attr56 = ?, attr57 = ?, attr58 = ?, attr59 = ?,
                  attr60 = ?, attr61 = ?, attr62 = ?, attr63 = ?, attr64 = ?, attr65 = ?, attr66 = ?, attr67 = ?, attr68 = ?, attr69 = ?,
                  attr70 = ?, attr71 = ?, attr72 = ?, attr73 = ?, attr74 = ?, attr75 = ?, attr76 = ?, attr77 = ?, attr78 = ?, attr79 = ?,
                  attr80 = ?, attr81 = ?, attr82 = ?, attr83 = ?, attr84 = ?, attr85 = ?, attr86 = ?, attr87 = ?, attr88 = ?, attr89 = ?,
                  attr90 = ?, attr91 = ?, attr92 = ?, attr93 = ?, attr94 = ?, attr95 = ?, attr96 = ?, attr97 = ?, attr98 = ?, attr99 = ?,
                  attr100 = ?, last_updated_by = ?, last_update_date = ?
                where doc_id = ?
                """,
                detail.getCompanyProjectCode().trim(), project.getCompanyProjectName(), parseYearMonthStart(detail.getStartArchPeriod(), docDate),
                parseYearMonthEnd(detail.getEndArchPeriod(), docDate), varchar(trimToNull(detail.getBusiModuleCode()), 30), businessCode,
                docDate.atStartOfDay(), archiveDestination, originPlace, carrierType, documentName,
                docOrg, 1L, application.getApplicant(), retentionYears, varchar(securityLevelCode, 30), "1.0", sourceIdTrace, sourceSystem,
                remark, extVisibleFlag, attr2, attr3, attr4, attr5, attr6, attr7, attr8, attr9,
                attr10, attr11, attr12, attr13, attr14, attr15, attr16, attr17, attr18, attr19,
                attr20, attr21, attr22, attr23, attr24, attr25, attr26, attr27, attr28, attr29,
                attr30, attr31, attr32, attr33, attr34, attr35, attr36, attr37, attr38, attr39,
                attr40, attr41, attr42, attr43, attr44, attr45, attr46, attr47, attr48, attr49,
                attr50, attr51, attr52, attr53, attr54, attr55, attr56, attr57, attr58, attr59,
                attr60, attr61, attr62, attr63, attr64, attr65, attr66, attr67, attr68, attr69,
                attr70, attr71, attr72, attr73, attr74, attr75, attr76, attr77, attr78, attr79,
                attr80, attr81, attr82, attr83, attr84, attr85, attr86, attr87, attr88, attr89,
                attr90, attr91, attr92, attr93, attr94, attr95, attr96, attr97, attr98, attr99,
                attr100, SYSTEM_OPERATOR_ID, now, docId
            );
            return;
        }

        Long newDocId = nextFdcDocumentId();
        jdbcTemplate.update(
            """
            insert into fdc_document_t (
              doc_id, company_code, company_name, start_period, end_period, biz_module_code, doc_biz_no, doc_gen_date,
              arch_place_alpha2_code, origin_place_alpha2_code, carrier_type, doc_name, doc_organization_code,
              doc_resp_dept_id, doc_resp_person_id, rentention_term, security_level, doc_version, source_id, source_system, lifecycle_status, custody_status,
              description, visible_flag, attr2, attr3, attr4, attr5, attr6, attr7, attr8, attr9, attr10, attr11, attr12, attr13, attr14,
              attr15, attr16, attr17, attr18, attr19, attr20, attr21, attr22, attr23, attr24, attr25, attr26, attr27, attr28, attr29,
              attr30, attr31, attr32, attr33, attr34, attr35, attr36, attr37, attr38, attr39, attr40, attr41, attr42, attr43, attr44,
              attr45, attr46, attr47, attr48, attr49, attr50, attr51, attr52, attr53, attr54, attr55, attr56, attr57, attr58, attr59,
              attr60, attr61, attr62, attr63, attr64, attr65, attr66, attr67, attr68, attr69, attr70, attr71, attr72, attr73, attr74,
              attr75, attr76, attr77, attr78, attr79, attr80, attr81, attr82, attr83, attr84, attr85, attr86, attr87, attr88, attr89,
              attr90, attr91, attr92, attr93, attr94, attr95, attr96, attr97, attr98, attr99, attr100, created_by, creation_date,
              last_updated_by, last_update_date, delete_flag
            ) values (
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
              ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            """,
            newDocId, detail.getCompanyProjectCode().trim(), project.getCompanyProjectName(),
            parseYearMonthStart(detail.getStartArchPeriod(), docDate), parseYearMonthEnd(detail.getEndArchPeriod(), docDate),
            varchar(trimToNull(detail.getBusiModuleCode()), 30), businessCode, docDate.atStartOfDay(),
            archiveDestination, originPlace, carrierType, documentName, docOrg,
            1L, application.getApplicant(), retentionYears, varchar(securityLevelCode, 30), "1.0", sourceIdTrace, sourceSystem, "UNARCHIVED", "IN_STORAGE",
            remark, extVisibleFlag,
            attr2, attr3, attr4, attr5, attr6, attr7, attr8, attr9, attr10, attr11, attr12, attr13, attr14, attr15, attr16, attr17, attr18, attr19, attr20,
            attr21, attr22, attr23, attr24, attr25, attr26, attr27, attr28, attr29, attr30, attr31, attr32, attr33, attr34, attr35, attr36, attr37, attr38, attr39, attr40,
            attr41, attr42, attr43, attr44, attr45, attr46, attr47, attr48, attr49, attr50, attr51, attr52, attr53, attr54, attr55, attr56, attr57, attr58, attr59, attr60,
            attr61, attr62, attr63, attr64, attr65, attr66, attr67, attr68, attr69, attr70, attr71, attr72, attr73, attr74, attr75, attr76, attr77, attr78, attr79, attr80,
            attr81, attr82, attr83, attr84, attr85, attr86, attr87, attr88, attr89, attr90, attr91, attr92, attr93, attr94, attr95, attr96, attr97, attr98, attr99, attr100,
            SYSTEM_OPERATOR_ID, now, SYSTEM_OPERATOR_ID, now, 0
        );
    }

    private Long nextFdcDocumentId() {
        Long max = jdbcTemplate.queryForObject("select coalesce(max(doc_id), 0) from fdc_document_t", Long.class);
        return (max == null ? 0L : max) + 1L;
    }

    private static LocalDate parseYearMonthStart(String value, LocalDate fallback) {
        if (StringUtils.hasText(value) && value.trim().length() >= 7) {
            return YearMonth.parse(value.trim().substring(0, 7)).atDay(1);
        }
        return YearMonth.from(fallback).atDay(1);
    }

    private static LocalDate parseYearMonthEnd(String value, LocalDate fallback) {
        if (StringUtils.hasText(value) && value.trim().length() >= 7) {
            return YearMonth.parse(value.trim().substring(0, 7)).atEndOfMonth();
        }
        return YearMonth.from(fallback).atEndOfMonth();
    }

    private Map<String, Object> buildDocumentAttrColumns(String moduleCode, Map<String, String> extValues) {
        Map<String, Object> out = new HashMap<>();
        for (int i = 2; i <= 100; i++) {
            out.put("attr" + i, null);
        }
        if (!StringUtils.hasText(moduleCode) || extValues == null || extValues.isEmpty()) {
            return out;
        }
        List<BusinessModuleExtField> fields = businessModuleExtFieldMapper.selectList(new LambdaQueryWrapper<BusinessModuleExtField>()
            .eq(BusinessModuleExtField::getModuleCode, moduleCode.trim())
            .eq(BusinessModuleExtField::getDeleteFlag, "N")
            .eq(BusinessModuleExtField::getEnabledFlag, "Y")
            .eq(BusinessModuleExtField::getFieldScope, "BASIC"));
        for (BusinessModuleExtField field : fields) {
            if (field == null || !StringUtils.hasText(field.getExtAttribute())) {
                continue;
            }
            String attr = field.getExtAttribute().trim().toLowerCase(Locale.ROOT);
            if (!ATTR_COLUMN_PATTERN.matcher(attr).matches()) {
                continue;
            }
            String raw = extValues.get(field.getFieldCode());
            if (!StringUtils.hasText(raw) && StringUtils.hasText(field.getEnglishFieldName())) {
                raw = extValues.get(field.getEnglishFieldName().trim());
            }
            if (!StringUtils.hasText(raw)) {
                continue;
            }
            out.put(attr, raw.trim());
        }
        return out;
    }

    private Map<Long, Map<String, String>> decodeExtRows(List<Map<String, Object>> extRows,
                                                        List<TransferApplicationDetail> details) {
        if (extRows == null || extRows.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> detailIdToModule = details.stream()
            .filter(d -> d.getApplicationDetailId() != null && StringUtils.hasText(d.getBusiModuleCode()))
            .collect(Collectors.toMap(
                TransferApplicationDetail::getApplicationDetailId,
                d -> d.getBusiModuleCode().trim(),
                (left, right) -> left));
        Map<Long, Map<String, String>> result = new HashMap<>();
        for (Map<String, Object> row : extRows) {
            Object objectId = row.get("object_id");
            if (!(objectId instanceof Number number)) {
                continue;
            }
            Long detailId = number.longValue();
            String moduleCode = detailIdToModule.get(detailId);
            if (!StringUtils.hasText(moduleCode)) {
                continue;
            }
            Map<String, String> columnToFieldCode = transferBusinessModuleExtFieldService.columnToFieldCodeMap(moduleCode);
            if (columnToFieldCode.isEmpty()) {
                continue;
            }
            Map<String, String> values = new HashMap<>();
            for (Map.Entry<String, String> e : columnToFieldCode.entrySet()) {
                Object raw = row.get(e.getKey());
                if (raw == null) {
                    continue;
                }
                String str = raw instanceof BigDecimal ? ((BigDecimal) raw).stripTrailingZeros().toPlainString() : String.valueOf(raw);
                if (StringUtils.hasText(str)) {
                    values.put(e.getValue(), str.trim());
                }
            }
            if (!values.isEmpty()) {
                result.computeIfAbsent(detailId, k -> new HashMap<>()).putAll(values);
            }
        }
        return result;
    }

    private void persistArchiveExtValues(Long archiveId, List<DocumentTypeExtFieldResponse> fields, Map<String, String> extValues) {
        if (extValues == null || extValues.isEmpty()) {
            return;
        }
        Map<String, DocumentTypeExtFieldResponse> fieldMap = new HashMap<>();
        for (DocumentTypeExtFieldResponse f : fields) {
            fieldMap.put(f.getFieldCode(), f);
        }
        extValues.forEach((fieldCode, value) -> {
            if (!StringUtils.hasText(value) || !fieldMap.containsKey(fieldCode)) {
                return;
            }
            DocumentTypeExtFieldResponse field = fieldMap.get(fieldCode);
            ArchiveExtValue entity = new ArchiveExtValue();
            entity.setArchiveId(archiveId);
            entity.setFieldCode(fieldCode);
            entity.setFieldNameSnapshot(field.getFieldName());
            entity.setFieldType(field.getFieldType());
            entity.setDictCategoryCode(field.getDictCategoryCode());
            if ("DICT".equals(field.getFieldType())) {
                entity.setDictItemCode(value.trim());
                entity.setDictItemNameSnapshot(value.trim());
            } else {
                entity.setTextValue(value.trim());
            }
            entity.setValueSource("MANUAL");
            entity.setCreatedBy(SYSTEM_OPERATOR_ID);
            entity.setCreationDate(LocalDateTime.now());
            entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            entity.setLastUpdateDate(LocalDateTime.now());
            archiveExtValueMapper.insert(entity);
        });
    }

    private ArchiveDefaultResolveResponse resolveDefaultsForDetail(String companyProjectCode,
                                                                   String busiModuleCode,
                                                                   String customRule,
                                                                   String archiveDestination) {
        CompanyProject companyProject = requireCompanyProject(companyProjectCode);
        List<ArchiveFlowRule> rules = archiveFlowRuleMapper.selectList(new LambdaQueryWrapper<ArchiveFlowRule>()
            .eq(ArchiveFlowRule::getCompanyProjectCode, companyProjectCode)
            .eq(ArchiveFlowRule::getBusiModuleCode, busiModuleCode)
            .eq(ArchiveFlowRule::getDeleteFlag, "N")
            .eq(ArchiveFlowRule::getEnabledFlag, "Y"));
        ArchiveFlowRule bestMatch = rules.stream()
            .max(Comparator.comparingInt(rule -> scoreRule(rule, customRule, archiveDestination)))
            .orElse(null);
        ArchiveDefaultResolveResponse response = new ArchiveDefaultResolveResponse();
        response.setCountryCode(companyProject.getCountryCode());
        if (bestMatch != null) {
            response.setArchiveDestination(StringUtils.hasText(archiveDestination) ? archiveDestination : bestMatch.getArchiveDestination());
            response.setDocumentOrganizationCode(bestMatch.getDocumentOrganizationCode());
            response.setRetentionPeriodYears(bestMatch.getRetentionPeriodYears());
        } else {
            response.setArchiveDestination(archiveDestination);
        }
        return response;
    }

    private int scoreRule(ArchiveFlowRule rule, String customRule, String archiveDestination) {
        int score = 0;
        if (Objects.equals(trimToNull(rule.getCustomRule()), trimToNull(customRule))) {
            score += 2;
        }
        if (Objects.equals(trimToNull(rule.getArchiveDestination()), trimToNull(archiveDestination))) {
            score += 2;
        }
        if (!StringUtils.hasText(rule.getCustomRule())) {
            score += 1;
        }
        if (!StringUtils.hasText(rule.getArchiveDestination())) {
            score += 1;
        }
        return score;
    }

    private CompanyProject requireCompanyProject(String companyProjectCode) {
        if (!StringUtils.hasText(companyProjectCode)) {
            throw new BusinessException("公司编码不能为空");
        }
        CompanyProject project = companyProjectMapper.selectOne(new LambdaQueryWrapper<CompanyProject>()
            .eq(CompanyProject::getCompanyProjectCode, companyProjectCode.trim())
            .eq(CompanyProject::getDeleteFlag, "N")
            .eq(CompanyProject::getEnabledFlag, "Y")
            .last("limit 1"));
        if (project == null) {
            throw new BusinessException("公司不存在或已停用: " + companyProjectCode);
        }
        return project;
    }

    private String firstSecurityLevelCode() {
        SecurityLevelDictionary row = securityLevelDictionaryMapper.selectOne(new LambdaQueryWrapper<SecurityLevelDictionary>()
            .eq(SecurityLevelDictionary::getEnabledFlag, "Y")
            .eq(SecurityLevelDictionary::getDeleteFlag, "N")
            .orderByAsc(SecurityLevelDictionary::getSortOrder)
            .last("limit 1"));
        if (row == null || !StringUtils.hasText(row.getSecurityLevelCode())) {
            throw new BusinessException("未配置密级字典，无法从移交生成档案");
        }
        return row.getSecurityLevelCode().trim();
    }

    private String firstDocumentOrganizationCode() {
        List<String> codes = archiveFlowLookupMapper.selectEnabledDocumentOrganizationCodes();
        if (codes == null || codes.isEmpty() || !StringUtils.hasText(codes.get(0))) {
            throw new BusinessException("未配置文档所属机构，无法从移交生成档案");
        }
        return codes.get(0).trim();
    }

    private static String buildRemark(TransferApplication application, TransferApplicationDetail detail) {
        StringBuilder sb = new StringBuilder();
        sb.append("移交申请 ").append(application.getApplicationNumber());
        if (StringUtils.hasText(detail.getCatalogVolumeNo())) {
            sb.append(" 册号:").append(detail.getCatalogVolumeNo().trim());
        }
        if (StringUtils.hasText(detail.getRemark())) {
            sb.append(" ").append(detail.getRemark().trim());
        }
        return sb.toString();
    }

    private static String toYearMonthPeriod(String period, LocalDate fallback) {
        if (StringUtils.hasText(period)) {
            String normalized = period.trim();
            if (normalized.length() >= 7) {
                return normalized.substring(0, 7);
            }
        }
        return YearMonth.from(fallback).toString();
    }

    private static String resolveCarrierType(String carrierType) {
        if (!StringUtils.hasText(carrierType)) {
            return "ELECTRONIC";
        }
        String u = carrierType.trim().toUpperCase(Locale.ROOT);
        if ("ELECTRONIC".equals(u) || "PAPER".equals(u) || "HYBRID".equals(u)) {
            return u;
        }
        return "ELECTRONIC";
    }

    private static String generateCode(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        return fallback;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String varchar(String value, int maxLen) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String t = value.trim();
        return t.length() <= maxLen ? t : t.substring(0, maxLen);
    }

    private static String fdcDocumentSourceId(TransferApplication application) {
        if (application == null) {
            return null;
        }
        String num = trimToNull(application.getApplicationNumber());
        String body = num != null ? num : ("APP_ID=" + application.getApplicationId());
        return body.length() > 500 ? body.substring(0, 500) : body;
    }
}
