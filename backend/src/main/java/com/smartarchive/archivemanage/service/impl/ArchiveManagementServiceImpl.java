package com.smartarchive.archivemanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartarchive.archivemanage.domain.AiModelConfig;
import com.smartarchive.archivemanage.domain.ArchiveAttachment;
import com.smartarchive.archivemanage.domain.ArchiveContent;
import com.smartarchive.archivemanage.domain.ArchiveContentChunk;
import com.smartarchive.archivemanage.domain.ArchiveCreateSession;
import com.smartarchive.archivemanage.domain.ArchiveExtValue;
import com.smartarchive.archivemanage.domain.ArchivePaper;
import com.smartarchive.archivemanage.domain.ArchiveRecord;
import com.smartarchive.archivemanage.domain.BindBatch;
import com.smartarchive.archivemanage.domain.BindVolume;
import com.smartarchive.archivemanage.domain.BindVolumeItem;
import com.smartarchive.archivemanage.domain.StorageBatch;
import com.smartarchive.archivemanage.domain.StorageBatchItem;
import com.smartarchive.archivemanage.domain.StorageLedger;
import com.smartarchive.archivemanage.dto.AiModelConfigResponse;
import com.smartarchive.archivemanage.dto.ArchiveAiParseResult;
import com.smartarchive.archivemanage.dto.ArchiveAskCommand;
import com.smartarchive.archivemanage.dto.ArchiveAskResponse;
import com.smartarchive.archivemanage.dto.ArchiveAttachmentResponse;
import com.smartarchive.archivemanage.dto.ArchiveAttachmentUpdateCommand;
import com.smartarchive.archivemanage.dto.ArchiveCreateCommand;
import com.smartarchive.archivemanage.dto.ArchiveCreateOptionsResponse;
import com.smartarchive.archivemanage.dto.ArchiveCreateSessionCommand;
import com.smartarchive.archivemanage.dto.ArchiveCreateSessionResponse;
import com.smartarchive.archivemanage.dto.ArchiveDefaultResolveResponse;
import com.smartarchive.archivemanage.dto.ArchiveQueryCommand;
import com.smartarchive.archivemanage.dto.ArchiveQueryResponse;
import com.smartarchive.archivemanage.dto.ArchiveSummaryResponse;
import com.smartarchive.archivemanage.dto.ArchiveTransferCommand;
import com.smartarchive.archivemanage.dto.ArchiveTransferResponse;
import com.smartarchive.archivemanage.dto.BindArchiveCandidateResponse;
import com.smartarchive.archivemanage.dto.BindBatchResponse;
import com.smartarchive.archivemanage.dto.BindCreateCommand;
import com.smartarchive.archivemanage.dto.BindOptionsResponse;
import com.smartarchive.archivemanage.dto.BindPreviewCommand;
import com.smartarchive.archivemanage.dto.BindPreviewResponse;
import com.smartarchive.archivemanage.dto.BindQueryCommand;
import com.smartarchive.archivemanage.dto.BindVolumeItemResponse;
import com.smartarchive.archivemanage.dto.BindVolumeResponse;
import com.smartarchive.archivemanage.dto.DocumentTypeExtFieldResponse;
import com.smartarchive.archivemanage.dto.LabelValueOption;
import com.smartarchive.archivemanage.dto.PendingAuditAttachmentRef;
import com.smartarchive.archivemanage.dto.PendingAuditDownload;
import com.smartarchive.archivemanage.dto.PendingDocumentBatchDeleteCommand;
import com.smartarchive.archivemanage.dto.PendingDocumentWriteCommand;
import com.smartarchive.archivemanage.dto.StorageBatchItemResponse;
import com.smartarchive.archivemanage.dto.StorageBatchResponse;
import com.smartarchive.archivemanage.dto.StorageCreateCommand;
import com.smartarchive.archivemanage.dto.StorageLedgerQueryCommand;
import com.smartarchive.archivemanage.dto.StorageLedgerResponse;
import com.smartarchive.archivemanage.dto.StorageOptionsResponse;
import com.smartarchive.archivemanage.dto.StorageQueryCommand;
import com.smartarchive.archivemanage.dto.StorageQueryResponse;
import com.smartarchive.archivemanage.mapper.AiModelConfigMapper;
import com.smartarchive.archivemanage.mapper.ArchiveAttachmentMapper;
import com.smartarchive.archivemanage.mapper.ArchiveContentChunkMapper;
import com.smartarchive.archivemanage.mapper.ArchiveContentMapper;
import com.smartarchive.archivemanage.mapper.ArchiveCreateSessionMapper;
import com.smartarchive.archivemanage.mapper.ArchiveExtValueMapper;
import com.smartarchive.archivemanage.mapper.ArchivePaperMapper;
import com.smartarchive.archivemanage.mapper.ArchiveRecordMapper;
import com.smartarchive.archivemanage.mapper.BindBatchMapper;
import com.smartarchive.archivemanage.mapper.BindVolumeItemMapper;
import com.smartarchive.archivemanage.mapper.BindVolumeMapper;
import com.smartarchive.archivemanage.mapper.StorageBatchItemMapper;
import com.smartarchive.archivemanage.mapper.StorageBatchMapper;
import com.smartarchive.archivemanage.mapper.StorageLedgerMapper;
import com.smartarchive.archivemanage.service.ArchiveManagementService;
import com.smartarchive.archivemanage.service.DocumentTypeExtFieldService;
import com.smartarchive.archivemanage.service.support.ArchiveAiChatService;
import com.smartarchive.archivemanage.service.support.ArchiveFileTextExtractor;
import com.smartarchive.archivemanage.service.support.ArchiveTextChunkService;
import com.smartarchive.archivemanage.service.support.ArchiveTextVectorService;
import com.smartarchive.archivemanage.service.support.MultiValueTextParse;
import com.smartarchive.archivemanage.service.support.SecurityLevelResolver;
import com.smartarchive.archive.domain.ArchiveReceipt;
import com.smartarchive.archive.mapper.ArchiveReceiptMapper;
import com.smartarchive.archiveflow.domain.ArchiveFlowRule;
import com.smartarchive.archiveflow.domain.SecurityLevelDictionary;
import com.smartarchive.archiveflow.mapper.ArchiveFlowRuleMapper;
import com.smartarchive.archiveflow.mapper.SecurityLevelDictionaryMapper;
import com.smartarchive.businessmodule.domain.BusinessModule;
import com.smartarchive.businessmodule.domain.BusinessModuleExtField;
import com.smartarchive.businessmodule.mapper.BusinessModuleExtFieldMapper;
import com.smartarchive.businessmodule.mapper.BusinessModuleMapper;
import com.smartarchive.common.audit.dto.OperationAuditAttachment;
import com.smartarchive.common.audit.service.OperationAuditService;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.companyproject.domain.CompanyProject;
import com.smartarchive.companyproject.mapper.CompanyProjectMapper;
import com.smartarchive.documentorganization.domain.DocumentOrganization;
import com.smartarchive.documentorganization.domain.DocumentOrganizationCity;
import com.smartarchive.documentorganization.mapper.DocumentOrganizationCityMapper;
import com.smartarchive.documentorganization.mapper.DocumentOrganizationMapper;
import com.smartarchive.file.domain.FdcFile;
import com.smartarchive.file.mapper.FdcFileMapper;
import com.smartarchive.workspace.dto.WorkspaceIoJobCreateCommand;
import com.smartarchive.workspace.dto.WorkspaceIoJobSummaryResponse;
import com.smartarchive.workspace.service.WorkspaceIoJobService;
import com.smartarchive.warehouse.domain.Warehouse;
import com.smartarchive.warehouse.domain.WarehouseLocation;
import com.smartarchive.warehouse.mapper.WarehouseLocationMapper;
import com.smartarchive.warehouse.mapper.WarehouseMapper;
import com.smartarchive.workflow.domain.WorkflowInstance;
import com.smartarchive.workflow.dto.StartProcessCommand;
import com.smartarchive.workflow.service.WorkflowService;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArchiveManagementServiceImpl implements ArchiveManagementService {
    private static final Long SYSTEM_OPERATOR_ID = 1L;
    private static final String SYSTEM_OPERATOR_NAME = "System";

    private static long resolveOperatorUserId(PendingDocumentWriteCommand command) {
        if (command.getOperatorUserId() != null && command.getOperatorUserId() > 0) {
            return command.getOperatorUserId();
        }
        return SYSTEM_OPERATOR_ID;
    }
    private static final Pattern BUSINESS_CODE_PATTERN = Pattern.compile("[A-Z]{2,}[A-Z0-9_-]{2,}");
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "md", "json", "csv", "xml", "sql", "log", "yml", "yaml", "properties", "java", "ts", "js", "vue", "html", "htm");
    private static final Pattern PERIOD_PATTERN = Pattern.compile("(20\\d{2})[年\\-./](\\d{1,2})");
    private static final Pattern PERSON_NAME_PATTERN = Pattern.compile("姓名[：:：]?\\s*([\\p{IsHan}A-Za-z·]{2,20})");

    private static final Pattern FILE_NAME_DATE_PATTERN = Pattern.compile("(20\\d{2})(\\d{2})(\\d{2})");
    private static final Pattern COMPANY_IN_BRACKETS_PATTERN = Pattern.compile("[（(]([A-Za-z0-9\\p{IsHan}\\-\\s]{2,40})[）)]");
    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(20\\d{2})[年\\-./](\\d{1,2})(?:[月\\-./](\\d{1,2}))?日?\\s*(?:至|到|~|—|-|–)\\s*(20\\d{2})[年\\-./](\\d{1,2})(?:[月\\-./](\\d{1,2}))?日?");
    private static final Pattern SINGLE_DATE_PATTERN = Pattern.compile("(20\\d{2})[年\\-./](\\d{1,2})(?:[月\\-./](\\d{1,2}))?日?");
    private static final Map<String, String> HARD_CODED_EXT_FIELD_ATTR_MAP = Map.ofEntries(
        Map.entry("invoiceNo", "attr41"),
        Map.entry("accountant", "attr47"),
        Map.entry("scannedBy", "attr48"),
        Map.entry("issueDateRange", "attr49"),
        Map.entry("maturityDateRange", "attr50"),
        Map.entry("lgExpiryDateRange", "attr51"),
        Map.entry("lgLedgerStatus", "attr52"),
        Map.entry("bankName", "attr53"),
        Map.entry("currency", "attr54"),
        Map.entry("amount", "attr55"),
        Map.entry("issuingAuthority", "attr56"),
        Map.entry("disposalTimeRange", "attr57"),
        Map.entry("businessVolumeNo", "attr58"),
        Map.entry("lgWorkflowNo", "attr59"),
        Map.entry("lgNo", "attr60")
    );
    private static final List<String> REF_NO_ATTR_COLUMNS = List.of("attr42", "attr43", "attr44", "attr45", "attr46");

    /**
     * {@code fdc_document_t.attr2–attr100}（attr1 单独表示是否可见）；
     * {@link #mergeExtValuesIntoAttrColumnsFromBusinessModule} 与 {@link #mergeBusinessModuleDocumentAttrsIntoExtValues} 使用同一白名单。
     */
    private static final List<String> FDC_DOC_ATTR_EXTENDED_COLUMNS;

    /** 可由 {@code fdc_business_module_ext_field_t.ext_attribute} 映射写入/读出的文档列（attr2–attr100） */
    private static final Set<String> FDC_DOC_EXT_ATTRIBUTE_WHITELIST;

    /** select attr2, attr3, … attr100（无前缀） */
    private static final String SQL_SELECT_FDC_DOC_ATTR2_TO_100;

    /** select d.attr2, d.attr3, … d.attr100 */
    private static final String SQL_SELECT_D_FDC_DOC_ATTR2_TO_100;

    /** attr2 = ?, attr3 = ?, … attr100 = ? */
    private static final String SQL_SET_FDC_DOC_ATTR2_TO_100;

    private static final String SQL_INSERT_FORMAL_FDC_DOCUMENT;

    private static final String SQL_UPDATE_FDC_DOCUMENT_PENDING_WITH_BIZ_MODULE;

    private static final String SQL_UPDATE_FDC_DOCUMENT_PENDING_WITHOUT_BIZ_MODULE;

    static {
        List<String> ext = new ArrayList<>();
        for (int i = 2; i <= 100; i++) {
            ext.add("attr" + i);
        }
        FDC_DOC_ATTR_EXTENDED_COLUMNS = List.copyOf(ext);
        FDC_DOC_EXT_ATTRIBUTE_WHITELIST = Set.copyOf(ext);
        SQL_SELECT_FDC_DOC_ATTR2_TO_100 = String.join(", ", ext);
        StringBuilder dSel = new StringBuilder();
        StringBuilder setSb = new StringBuilder();
        for (int i = 0; i < ext.size(); i++) {
            if (i > 0) {
                dSel.append(", ");
                setSb.append(", ");
            }
            dSel.append("d.").append(ext.get(i));
            setSb.append(ext.get(i)).append(" = ?");
        }
        SQL_SELECT_D_FDC_DOC_ATTR2_TO_100 = dSel.toString();
        SQL_SET_FDC_DOC_ATTR2_TO_100 = setSb.toString();
        String attrsJoined = String.join(", ", ext);
        String ph99 = String.join(", ", Collections.nCopies(99, "?"));
        String ph25 = String.join(", ", Collections.nCopies(25, "?"));
        String ph4 = String.join(", ", Collections.nCopies(4, "?"));
        SQL_INSERT_FORMAL_FDC_DOCUMENT = """
            insert into fdc_document_t (
              doc_id, company_code, company_name, start_period, end_period, biz_module_code, doc_biz_no, doc_gen_date,
              arch_place_alpha2_code, origin_place_alpha2_code, carrier_type, doc_name, doc_organization_code,
              doc_resp_dept_id, doc_resp_person_id, rentention_term, security_level, doc_version, source_id, source_system,
              lifecycle_status, custody_status, description, attr1, arch_barcode,
              """
            + attrsJoined
            + """
              ,
              delete_flag, created_by, creation_date, last_updated_by, last_update_date
            ) values (
              """
            + ph25 + ", " + ph99 + ", 0, " + ph4
            + """
            )
            """;
        SQL_UPDATE_FDC_DOCUMENT_PENDING_WITH_BIZ_MODULE = """
            update fdc_document_t set
              biz_module_code = ?,
              start_period = ?,
              end_period = ?,
              arch_place_alpha2_code = ?,
              origin_place_alpha2_code = ?,
              doc_name = ?,
              doc_gen_date = ?,
              doc_resp_dept_id = ?,
              doc_resp_person_id = ?,
              carrier_type = ?,
              source_system = ?,
              security_level = ?,
              description = ?,
              doc_organization_code = ?,
              attr1 = ?,
              arch_barcode = ?,
              """
            + SQL_SET_FDC_DOC_ATTR2_TO_100
            + """
              ,
              doc_biz_no = ?,
              company_code = ?,
              company_name = ?,
              lifecycle_status = ?,
              last_updated_by = ?, last_update_date = ?
            where doc_id = ? and coalesce(delete_flag, 0) = 0 and lifecycle_status in ('UNARCHIVED', 'DRAFT')
            """;
        SQL_UPDATE_FDC_DOCUMENT_PENDING_WITHOUT_BIZ_MODULE = """
            update fdc_document_t set
              start_period = ?,
              end_period = ?,
              arch_place_alpha2_code = ?,
              origin_place_alpha2_code = ?,
              doc_name = ?,
              doc_gen_date = ?,
              doc_resp_dept_id = ?,
              doc_resp_person_id = ?,
              carrier_type = ?,
              source_system = ?,
              security_level = ?,
              description = ?,
              doc_organization_code = ?,
              attr1 = ?,
              arch_barcode = ?,
              """
            + SQL_SET_FDC_DOC_ATTR2_TO_100
            + """
              ,
              doc_biz_no = ?,
              company_code = ?,
              company_name = ?,
              lifecycle_status = ?,
              last_updated_by = ?, last_update_date = ?
            where doc_id = ? and coalesce(delete_flag, 0) = 0 and lifecycle_status in ('UNARCHIVED', 'DRAFT')
            """;
    }

    private final ArchiveRecordMapper archiveRecordMapper;
    private final ArchiveExtValueMapper archiveExtValueMapper;
    private final ArchiveCreateSessionMapper archiveCreateSessionMapper;
    private final ArchiveAttachmentMapper archiveAttachmentMapper;
    private final ArchivePaperMapper archivePaperMapper;
    private final ArchiveContentMapper archiveContentMapper;
    private final ArchiveContentChunkMapper archiveContentChunkMapper;
    private final BindBatchMapper bindBatchMapper;
    private final BindVolumeMapper bindVolumeMapper;
    private final BindVolumeItemMapper bindVolumeItemMapper;
    private final StorageBatchMapper storageBatchMapper;
    private final StorageBatchItemMapper storageBatchItemMapper;
    private final StorageLedgerMapper storageLedgerMapper;
    private final AiModelConfigMapper aiModelConfigMapper;
    private final BusinessModuleMapper businessModuleMapper;
    private final BusinessModuleExtFieldMapper businessModuleExtFieldMapper;
    private final CompanyProjectMapper companyProjectMapper;
    private final DocumentOrganizationMapper documentOrganizationMapper;
    private final DocumentOrganizationCityMapper documentOrganizationCityMapper;
    private final ArchiveFlowRuleMapper archiveFlowRuleMapper;
    private final SecurityLevelDictionaryMapper securityLevelDictionaryMapper;
    private final SecurityLevelResolver securityLevelResolver;
    private final DocumentTypeExtFieldService documentTypeExtFieldService;
    private final WarehouseMapper warehouseMapper;
    private final WarehouseLocationMapper warehouseLocationMapper;
    private final OperationAuditService operationAuditService;
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;
    private final ArchiveAiChatService archiveAiChatService;
    private final ArchiveFileTextExtractor archiveFileTextExtractor;
    private final ArchiveTextChunkService archiveTextChunkService;
    private final ArchiveTextVectorService archiveTextVectorService;
    private final ArchiveReceiptMapper archiveReceiptMapper;
    private final WorkflowService workflowService;
    private final FdcFileMapper fdcFileMapper;
    private final WorkspaceIoJobService workspaceIoJobService;
    private final ObjectMapper objectMapper;
    @Qualifier("pendingArchiveBatchExecutor")
    private final Executor taskExecutor;
    private volatile Map<String, String> fdcDocumentColumnTypeCache;

    /** 应归档写入单独提交，避免与后续 getArchiveDetail 共用同一 JDBC 事务导致25P02 */
    private TransactionTemplate pendingDocumentWriteTemplate;

    @PostConstruct
    void initPendingDocumentWriteTemplate() {
        this.pendingDocumentWriteTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public ArchiveCreateOptionsResponse loadCreateOptions() {
        ArchiveCreateOptionsResponse response = new ArchiveCreateOptionsResponse();
        response.setCompanyProjects(listEnabledCompanyProjects());
        response.setDocumentTypes(listEnabledBusinessModules());
        response.setArchiveDestinations(listEnabledCities());
        response.setDocumentOrganizations(listEnabledDocumentOrganizations());
        response.setSecurityLevels(listSecurityLevels());
        response.setCarrierTypes(loadDictionaryOptions("ARCHIVE_CARRIER_TYPE"));
        response.setAttachmentTypes(loadDictionaryOptions("ARCHIVE_ATTACHMENT_TYPE"));
        response.setArchiveTypes(loadDictionaryOptions("ARCHIVE_TYPE"));
        response.setAiModels(listAiModelOptions());
        response.setGeoCountries(listGeoCountries());
        response.setGeoRepOffices(listGeoRepOffices());
        response.setGeoRegions(listGeoRegions());
        response.setCustodyStatuses(listCustodyStatuses());
        return response;
    }

    @Override
    public ArchiveDefaultResolveResponse resolveDefaults(String companyProjectCode, String documentTypeCode, String customRule, String archiveDestination) {
        CompanyProject companyProject = requireCompanyProject(companyProjectCode);
        requireBusinessModule(documentTypeCode);
        List<ArchiveFlowRule> rules = archiveFlowRuleMapper.selectList(new LambdaQueryWrapper<ArchiveFlowRule>()
            .eq(ArchiveFlowRule::getCompanyProjectCode, companyProjectCode)
            .eq(ArchiveFlowRule::getBusiModuleCode, documentTypeCode)
            .eq(ArchiveFlowRule::getDeleteFlag, "N")
            .eq(ArchiveFlowRule::getEnabledFlag, "Y"));
        ArchiveFlowRule bestMatch = rules.stream().max(Comparator.comparingInt(rule -> scoreRule(rule, customRule, archiveDestination))).orElse(null);
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

    @Override
    @Transactional
    public ArchiveCreateSessionResponse createSession(ArchiveCreateSessionCommand command) {
        ArchiveCreateSession session = new ArchiveCreateSession();
        session.setSessionCode(generateCode("SES"));
        session.setCreateMode(normalizeCreateMode(command.getCreateMode()));
        session.setSessionStatus("READY");
        session.setParseStatus("PENDING");
        session.setOwnerUserId(SYSTEM_OPERATOR_ID);
        session.setExpireTime(LocalDateTime.now().plusDays(2));
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        archiveCreateSessionMapper.insert(session);
        return buildSessionResponse(session, List.of(), null);
    }

    @Override
    public ArchiveCreateSessionResponse getSession(String sessionCode) {
        ArchiveCreateSession session = requireSession(sessionCode);
        List<ArchiveAttachment> attachments = listSessionAttachments(session.getSessionId());
        ArchiveAiParseResult parseResult = attachments.isEmpty() ? null : buildParseResult(session, attachments.get(attachments.size() - 1));
        return buildSessionResponse(session, attachments, parseResult);
    }

    @Override
    @Transactional
    public ArchiveAttachmentResponse uploadAttachment(String sessionCode, String attachmentRole, String attachmentTypeCode, String remark, MultipartFile file) {
        ArchiveCreateSession session = requireSession(sessionCode);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please upload a file");
        }
        Path storageRoot = Paths.get(System.getProperty("user.dir"), "storage", "archive-files", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        try { Files.createDirectories(storageRoot); } catch (IOException exception) { throw new BusinessException("Failed to create upload directory"); }
        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "upload.bin");
        String extension = extractExtension(originalFilename);
        String storageKey = UUID.randomUUID().toString().replace("-", "") + (StringUtils.hasText(extension) ? "." + extension : "");
        Path storagePath = storageRoot.resolve(storageKey);
        try { Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING); } catch (IOException exception) { throw new BusinessException("Failed to store uploaded file"); }
        ParsedAttachment parsed = parseStoredFile(storagePath, originalFilename, file.getContentType());
        ArchiveAttachment attachment = new ArchiveAttachment();
        attachment.setSessionId(session.getSessionId());
        attachment.setAttachmentRole(normalizeAttachmentRole(attachmentRole));
        String inferredAttachmentType = inferAttachmentTypeCode(originalFilename, parsed.summary(), parsed.preview());
        String inferredRemark = inferAttachmentRemark(originalFilename, parsed.summary(), parsed.preview());
        attachment.setAttachmentTypeCode(trimToNull(StringUtils.hasText(attachmentTypeCode) ? attachmentTypeCode : inferredAttachmentType));
        attachment.setAttachmentSeq(nextAttachmentSeq(session.getSessionId()));
        attachment.setVersionNo(1);
        attachment.setFileName(originalFilename);
        attachment.setFileExt(extension);
        attachment.setMimeType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setStoragePath(storagePath.toString());
        attachment.setStorageKey(storageKey);
        attachment.setFileHash(md5Hex(storagePath));
        attachment.setRemark(trimToNull(StringUtils.hasText(remark) ? remark : inferredRemark));
        attachment.setAiSummary(parsed.summary());
        attachment.setOcrStatus(parsed.hasText() ? "SUCCESS" : "FAILED");
        attachment.setParseStatus("SUCCESS");
        attachment.setVectorStatus(parsed.hasText() ? "PENDING" : "FAILED");
        attachment.setActiveFlag("Y");
        attachment.setDeleteFlag("N");
        attachment.setCreatedBy(SYSTEM_OPERATOR_ID);
        attachment.setCreationDate(LocalDateTime.now());
        attachment.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        attachment.setLastUpdateDate(LocalDateTime.now());
        archiveAttachmentMapper.insert(attachment);
        session.setBusiModuleCodeGuess(trimToNull(parsed.suggestedDocumentTypeCode()));
        session.setCarrierTypeCodeGuess("ELECTRONIC");
        session.setParseStatus("SUCCESS");
        session.setAiSummarySnapshot(parsed.summary());
        session.setUpdatedAt(LocalDateTime.now());
        archiveCreateSessionMapper.updateById(session);
        return toAttachmentResponse(attachment);
    }

    @Override
    @Transactional
    public ArchiveAttachmentResponse updateAttachment(String sessionCode, Long attachmentId, ArchiveAttachmentUpdateCommand command) {
        ArchiveCreateSession session = requireSession(sessionCode);
        ArchiveAttachment attachment = archiveAttachmentMapper.selectOne(new LambdaQueryWrapper<ArchiveAttachment>()
            .eq(ArchiveAttachment::getAttachmentId, attachmentId)
            .eq(ArchiveAttachment::getSessionId, session.getSessionId())
            .eq(ArchiveAttachment::getDeleteFlag, "N")
            .last("limit 1"));
        if (attachment == null) {
            throw new BusinessException("Attachment not found");
        }
        attachment.setAttachmentTypeCode(trimToNull(command.getAttachmentTypeCode()));
        attachment.setRemark(trimToNull(command.getRemark()));
        attachment.setAiSummary(trimToNull(command.getAiSummary()));
        attachment.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        attachment.setLastUpdateDate(LocalDateTime.now());
        archiveAttachmentMapper.updateById(attachment);
        return toAttachmentResponse(attachment);
    }

    @Override
    @Transactional
    public ArchiveSummaryResponse createArchive(ArchiveCreateCommand command) {
        validateRequired(command);
        String documentTypeCode = requireText(command.getDocumentTypeCode(), "documentTypeCode");
        CompanyProject companyProject = requireCompanyProject(command.getCompanyProjectCode());
        requireBusinessModule(documentTypeCode);
        ArchiveCreateSession session = StringUtils.hasText(command.getSessionCode()) ? requireSession(command.getSessionCode()) : null;
        List<ArchiveAttachment> sessionAttachments = session == null ? List.of() : listSessionAttachments(session.getSessionId());
        List<ArchiveAttachment> electronicAttachments = sessionAttachments.stream().filter(item -> "ELECTRONIC".equals(item.getAttachmentRole())).toList();
        List<ArchiveAttachment> paperScanAttachments = sessionAttachments.stream().filter(item -> "PAPER_SCAN".equals(item.getAttachmentRole())).toList();
        if (requiresElectronic(command.getCarrierTypeCode()) && electronicAttachments.isEmpty()) {
            throw new BusinessException("At least one electronic attachment is required");
        }
        if (requiresPaper(command.getCarrierTypeCode()) && (command.getPaperInfo() == null || command.getPaperInfo().getPlannedCopyCount() == null || command.getPaperInfo().getActualCopyCount() == null)) {
            throw new BusinessException("Paper archive information is required");
        }
        List<DocumentTypeExtFieldResponse> effectiveFields = documentTypeExtFieldService.listEffective(documentTypeCode);
        ParsedAttachment combinedParse = buildCombinedParseResult(electronicAttachments, documentTypeCode);
        Map<String, String> resolvedExtValues = resolveExtValues(command.getExtValues(), effectiveFields, combinedParse);
        validateExtValues(effectiveFields, resolvedExtValues);
        ArchiveRecord archive = new ArchiveRecord();
        archive.setArchiveCode(generateCode("ARC"));
        archive.setCreateMode(StringUtils.hasText(command.getCreateMode()) ? normalizeCreateMode(command.getCreateMode()) : (session == null ? "MANUAL" : session.getCreateMode()));
        archive.setArchiveStatus("CREATED");
        archive.setDocumentTypeCode(documentTypeCode);
        archive.setCompanyProjectCode(command.getCompanyProjectCode().trim());
        archive.setBeginPeriod(command.getBeginPeriod().trim());
        archive.setEndPeriod(command.getEndPeriod().trim());
        archive.setBusinessCode(trimToNull(command.getBusinessCode()));
        archive.setDocumentName(command.getDocumentName().trim());
        archive.setDutyPerson(command.getDutyPerson().trim());
        archive.setDutyDepartment(command.getDutyDepartment().trim());
        archive.setDocumentDate(command.getDocumentDate() == null ? null : command.getDocumentDate().atStartOfDay());
        archive.setSecurityLevelCode(securityLevelResolver.requireCanonicalForWrite(command.getSecurityLevelCode()));
        archive.setSourceSystem(trimToNull(command.getSourceSystem()));
        archive.setArchiveDestination(trimToNull(command.getArchiveDestination()));
        archive.setOriginPlace(trimToNull(command.getOriginPlace()));
        archive.setCarrierTypeCode(normalizeCarrierType(command.getCarrierTypeCode()));
        archive.setRemark(trimToNull(command.getRemark()));
        archive.setAiArchiveSummary(StringUtils.hasText(command.getAiArchiveSummary()) ? command.getAiArchiveSummary().trim() : generateArchiveSummary(command, electronicAttachments, paperScanAttachments));
        archive.setAiParseConfidence(combinedParse.confidence() == null ? null : BigDecimal.valueOf(combinedParse.confidence()));
        archive.setDocumentOrganizationCode(command.getDocumentOrganizationCode().trim());
        archive.setRetentionPeriodYears(command.getRetentionPeriodYears());
        archive.setArchiveTypeCode(command.getArchiveTypeCode().trim());
        archive.setCountryCode(StringUtils.hasText(command.getCountryCode()) ? command.getCountryCode().trim() : companyProject.getCountryCode());
        archive.setParseStatus(electronicAttachments.isEmpty() ? "FAILED" : "SUCCESS");
        archive.setVectorStatus(electronicAttachments.isEmpty() ? "FAILED" : "SUCCESS");
        archive.setQaIndexStatus(electronicAttachments.isEmpty() ? "FAILED" : "SUCCESS");
        archive.setSessionId(session == null ? null : session.getSessionId());
        archive.setDeleteFlag("N");
        archive.setCreatedBy(SYSTEM_OPERATOR_ID);
        archive.setCreationDate(LocalDateTime.now());
        archive.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        archive.setLastUpdateDate(LocalDateTime.now());
        archiveRecordMapper.insert(archive);
        persistExtValues(archive.getArchiveId(), effectiveFields, resolvedExtValues);
        if (requiresPaper(command.getCarrierTypeCode())) {
            ArchivePaper paper = new ArchivePaper();
            paper.setArchiveId(archive.getArchiveId());
            paper.setPlannedCopyCount(command.getPaperInfo().getPlannedCopyCount());
            paper.setActualCopyCount(command.getPaperInfo().getActualCopyCount());
            paper.setRemark(trimToNull(command.getPaperInfo().getRemark()));
            paper.setCreatedBy(SYSTEM_OPERATOR_ID);
            paper.setCreationDate(LocalDateTime.now());
            paper.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            paper.setLastUpdateDate(LocalDateTime.now());
            archivePaperMapper.insert(paper);
        }
        for (ArchiveAttachment attachment : mergeAttachments(electronicAttachments, paperScanAttachments)) {
            attachment.setArchiveId(archive.getArchiveId());
            attachment.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            attachment.setLastUpdateDate(LocalDateTime.now());
            archiveAttachmentMapper.updateById(attachment);
        }
        for (ArchiveAttachment attachment : electronicAttachments) {
            persistContentAndVectors(archive.getArchiveId(), attachment, parseStoredFile(Paths.get(attachment.getStoragePath()), attachment.getFileName(), attachment.getMimeType()));
        }
        if (session != null) {
            session.setSessionStatus("SAVED");
            session.setUpdatedAt(LocalDateTime.now());
            archiveCreateSessionMapper.updateById(session);
        }
        return buildArchiveSummary(archive, loadExtValueMap(List.of(archive.getArchiveId())).getOrDefault(archive.getArchiveId(), Map.of()), mergeAttachments(electronicAttachments, paperScanAttachments));
    }

    @Override
    public ArchiveQueryResponse queryArchives(ArchiveQueryCommand command) {
        return queryArchivesFromDocumentTable(command);
    }

    private ArchiveQueryResponse queryArchivesFromDocumentTable(ArchiveQueryCommand command) {
        Map<String, BusinessModule> businessModuleMap = listBusinessModuleMap();
        Map<String, String> carrierTypeNameMap = listCarrierTypeNameMap();
        StringBuilder sql = new StringBuilder("""
            select doc_id, doc_biz_no, company_code, company_name, biz_module_code, start_period, end_period,
                   arch_place_alpha2_code, origin_place_alpha2_code, doc_organization_code, lifecycle_status,
                   doc_name, doc_gen_date, doc_resp_person_id, coalesce(u.user_name, cast(fdc_document_t.doc_resp_person_id as varchar)) as duty_person_name, doc_resp_dept_id, carrier_type,
                   attr1,
                   source_system, security_level, description, fdc_document_t.creation_date as creation_date,
                   coalesce(created_u.user_name, cast(fdc_document_t.created_by as varchar)) as created_by_name,
              """).append(SQL_SELECT_FDC_DOC_ATTR2_TO_100).append("""
                   ,
                   cp.company_tag, cp.country_code, geo.rep_office_name, geo.region_name
              from fdc_document_t
              left join tpl_user_t u on u.user_id = fdc_document_t.doc_resp_person_id
              left join tpl_user_t created_u on created_u.user_id = fdc_document_t.created_by
              left join fdc_company_project_t cp on cp.company_project_code = fdc_document_t.company_code and cp.delete_flag = 'N'
              left join (
                    select country_code,
                           min(rep_office_name) as rep_office_name,
                           min(region_name) as region_name
                      from fdc_geo_region_t
                     where delete_flag = 'N'
                     group by country_code
              ) geo on geo.country_code = cp.country_code
            """);
        sql.append("""
             where coalesce(fdc_document_t.delete_flag, 0) = 0
               and lower(trim(coalesce(fdc_document_t.lifecycle_status, ''))) <> 'draft'
            """);
        List<Object> params = new ArrayList<>();

        if (StringUtils.hasText(command.getDocumentTypeCode())) {
            String docTypeCode = command.getDocumentTypeCode().trim();
            sql.append("""
                 and biz_module_code in (
                    with recursive module_tree as (
                        select module_code, parent_code
                          from fdc_business_module_t
                         where delete_flag = 'N' and module_code = ?
                        union all
                        select m.module_code, m.parent_code
                          from fdc_business_module_t m
                          join module_tree t on m.parent_code = t.module_code
                         where m.delete_flag = 'N'
                    )
                    select module_code from module_tree
                 )
                """);
            params.add(docTypeCode);
        }
        if (StringUtils.hasText(command.getCompanyProjectCode())) {
            sql.append(" and company_code = ?");
            params.add(command.getCompanyProjectCode().trim());
        }
        if (StringUtils.hasText(command.getArchiveTypeCode())) {
            sql.append(" and biz_module_code = ?");
            params.add(command.getArchiveTypeCode().trim());
        }
        if (StringUtils.hasText(command.getCarrierTypeCode())) {
            sql.append(" and carrier_type = ?");
            params.add(command.getCarrierTypeCode().trim());
        }
        if (StringUtils.hasText(command.getSecurityLevelCode())) {
            List<String> secVariants = securityLevelResolver.storedValueSqlVariants(command.getSecurityLevelCode());
            if (secVariants.size() == 1) {
                sql.append(" and security_level = ?");
                params.add(secVariants.get(0));
            } else {
                sql.append(" and security_level in (");
                sql.append(secVariants.stream().map(v -> "?").collect(Collectors.joining(",")));
                sql.append(")");
                params.addAll(secVariants);
            }
        }
        if (StringUtils.hasText(command.getBeginPeriod())) {
            sql.append(" and to_char(start_period, 'YYYY-MM') >= ?");
            params.add(command.getBeginPeriod().trim());
        }
        if (StringUtils.hasText(command.getEndPeriod())) {
            sql.append(" and to_char(end_period, 'YYYY-MM') <= ?");
            params.add(command.getEndPeriod().trim());
        }
        if (StringUtils.hasText(command.getDocumentName())) {
            sql.append(" and doc_name ilike ?");
            params.add("%" + command.getDocumentName().trim() + "%");
        }
        List<String> businessTokens = MultiValueTextParse.parseSpaceSeparatedValues(command.getBusinessCode());
        if (businessTokens.size() == 1) {
            sql.append(" and doc_biz_no ilike ?");
            params.add("%" + businessTokens.get(0) + "%");
        } else if (!businessTokens.isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < businessTokens.size(); i++) {
                if (i > 0) {
                    sql.append(" or ");
                }
                sql.append("lower(trim(doc_biz_no)) = lower(?)");
                params.add(businessTokens.get(i).trim());
            }
            sql.append(")");
        }
        if (StringUtils.hasText(command.getDutyPerson())) {
            sql.append(" and cast(doc_resp_person_id as varchar) ilike ?");
            params.add("%" + command.getDutyPerson().trim() + "%");
        }
        if (StringUtils.hasText(command.getArchiveDestination())) {
            sql.append(" and arch_place_alpha2_code = ?");
            params.add(command.getArchiveDestination().trim());
        }
        if (StringUtils.hasText(command.getSourceSystem())) {
            sql.append(" and source_system ilike ?");
            params.add("%" + command.getSourceSystem().trim() + "%");
        }
        if (StringUtils.hasText(command.getDocumentOrganizationCode())) {
            sql.append(" and doc_organization_code = ?");
            params.add(command.getDocumentOrganizationCode().trim());
        }
        appendGeoAndStatusFilterSql(sql, params, command.getExtFilters());
        appendHardCodedExtFilterSql(sql, params, command.getExtFilters());
        sql.append(" order by doc_id desc");

        Map<String, List<BusinessModuleExtField>> basicExtFieldsByModule = new HashMap<>();
        List<ArchiveSummaryResponse> rows = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            LocalDate startPeriod = rs.getObject("start_period", LocalDate.class);
            LocalDate endPeriod = rs.getObject("end_period", LocalDate.class);
            LocalDateTime docGenDate = rs.getObject("doc_gen_date", LocalDateTime.class);
            LocalDateTime creationDate = rs.getObject("creation_date", LocalDateTime.class);
            String lifecycleStatus = rs.getString("lifecycle_status");
            String businessModuleCode = rs.getString("biz_module_code");
            SecurityLevelResolver.Resolved secLv = securityLevelResolver.resolve(rs.getString("security_level"));
            return ArchiveSummaryResponse.builder()
                .archiveId(rs.getLong("doc_id"))
                .documentTypeCode(resolveRootBusinessModuleCode(businessModuleCode, businessModuleMap))
                .documentTypeName(resolveRootBusinessModuleName(businessModuleCode, businessModuleMap))
                .companyProjectCode(rs.getString("company_code"))
                .companyProjectName(rs.getString("company_name"))
                .beginPeriod(formatYearMonth(startPeriod))
                .endPeriod(formatYearMonth(endPeriod))
                .documentName(rs.getString("doc_name"))
                .businessCode(rs.getString("doc_biz_no"))
                .dutyPerson(rs.getString("duty_person_name"))
                .createdBy(rs.getString("created_by_name"))
                .dutyDepartment(String.valueOf(rs.getObject("doc_resp_dept_id")))
                .documentDate(docGenDate)
                .securityLevelCode(secLv.canonicalCode())
                .securityLevelName(secLv.displayName())
                .sourceSystem(rs.getString("source_system"))
                .archiveDestination(rs.getString("arch_place_alpha2_code"))
                .originPlace(rs.getString("origin_place_alpha2_code"))
                .carrierTypeCode(carrierTypeNameMap.getOrDefault(rs.getString("carrier_type"), rs.getString("carrier_type")))
                .remark(rs.getString("description"))
                .documentOrganizationCode(rs.getString("doc_organization_code"))
                .archiveTypeCode(resolveBusinessModuleDisplayName(businessModuleCode, businessModuleMap))
                .businessModuleTypeCode(businessModuleCode)
                .documentVisibility(StringUtils.hasText(rs.getString("attr1")) ? rs.getString("attr1").trim() : "是")
                .lifecycleStatus(lifecycleStatus)
                .archiveStatus("ARCHIVED".equalsIgnoreCase(lifecycleStatus) ? "已归档" : ("DRAFT".equalsIgnoreCase(lifecycleStatus) ? "草稿" : "未归档"))
                .custodyStatus("ARCHIVED".equalsIgnoreCase(lifecycleStatus) ? "已归档" : ("DRAFT".equalsIgnoreCase(lifecycleStatus) ? "草稿" : "未归档"))
                .lastUpdateDate(creationDate)
                .attachmentCount(0)
                .extValues(extractHardCodedExtValues(rs, businessModuleCode, basicExtFieldsByModule))
                .attachments(List.of())
                .build();
        }, params.toArray());

        rows = rows.stream()
            .filter(item -> item.getLifecycleStatus() == null
                || !"draft".equalsIgnoreCase(item.getLifecycleStatus().trim()))
            .toList();

        if (Boolean.TRUE.equals(command.getExcludeSubmittedTransferApplied()) && !rows.isEmpty()) {
            List<String> businessCodes = rows.stream()
                .map(ArchiveSummaryResponse::getBusinessCode)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
            if (!businessCodes.isEmpty()) {
                String placeholders = String.join(",", java.util.Collections.nCopies(businessCodes.size(), "?"));
                List<String> usedBusinessCodes = jdbcTemplate.queryForList(
                    """
                    select distinct d.doc_busi_no
                      from fdc_application_detail_t d
                      join fdc_application_t a on a.application_id = d.application_id
                     where d.delete_flag = 'N'
                       and a.delete_flag = 'N'
                       and d.doc_busi_no in (""" + placeholders + ")"
                       + " and upper(coalesce(a.application_status, '')) in ('DRAFT','SUBMITTED','APPROVED','REJECTED')",
                    String.class,
                    businessCodes.toArray()
                );
                if (!usedBusinessCodes.isEmpty()) {
                    Set<String> usedSet = usedBusinessCodes.stream()
                        .filter(StringUtils::hasText)
                        .map(String::trim)
                        .collect(Collectors.toSet());
                    rows = rows.stream()
                        .filter(item -> !usedSet.contains(trimToNull(item.getBusinessCode())))
                        .toList();
                }
            }
        }

        if (StringUtils.hasText(command.getKeyword())) {
            List<String> searchTerms = buildNormalizedKeywordSearchTerms(command.getKeyword());
            if (!searchTerms.isEmpty()) {
                Set<Long> keywordMatchedArchiveIds = loadUnifiedKeywordMatchedArchiveIds(searchTerms);
                List<Long> allIds = rows.stream().map(ArchiveSummaryResponse::getArchiveId).filter(Objects::nonNull).toList();
                Map<Long, List<ArchiveAttachment>> attachmentMapForKeyword = loadAttachmentMap(allIds);
                Set<Long> inMemoryMatched = collectInMemoryMatchedIdsFromSummaries(rows, attachmentMapForKeyword, searchTerms);
                Set<Long> matchedArchiveIds = new LinkedHashSet<>(keywordMatchedArchiveIds);
                matchedArchiveIds.addAll(inMemoryMatched);
                rows = rows.stream()
                    .filter(item -> item.getArchiveId() != null && matchedArchiveIds.contains(item.getArchiveId()))
                    .toList();
            }
        }

        Map<String, String> extFilters = command.getExtFilters();
        if (extFilters != null && !extFilters.isEmpty()) {
            rows = rows.stream()
                .filter(item -> matchesExtFilters(item.getExtValues() == null ? Map.of() : item.getExtValues(), extFilters))
                .toList();
        }

        Integer page = command.getPage() != null && command.getPage() > 0 ? command.getPage() : 1;
        Integer pageSize = command.getPageSize() != null && command.getPageSize() > 0 ? command.getPageSize() : 20;
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, rows.size());
        List<ArchiveSummaryResponse> pagedRows = start < end ? rows.subList(start, end) : List.of();
        List<DocumentTypeExtFieldResponse> queryFields = StringUtils.hasText(command.getDocumentTypeCode())
            ? documentTypeExtFieldService.listEffective(command.getDocumentTypeCode()).stream().filter(item -> "Y".equals(item.getQueryEnabledFlag())).toList()
            : List.of();

        return ArchiveQueryResponse.builder()
            .records(pagedRows)
            .queryFields(queryFields)
            .total((long) rows.size())
            .page(page)
            .pageSize(pageSize)
            .build();
    }

    @Override
    public ArchiveAskResponse ask(ArchiveAskCommand command) {
        AiModelConfig chatModel = findEnabledChatModel();
        List<ArchiveSummaryResponse> references = searchAskReferences(command, chatModel);
        if (references.isEmpty()) {
            return ArchiveAskResponse.builder()
                .answer("当前未检索到与你的问题直接相关的档案内容，建议换一种问法、缩小问题范围，或直接查看文档搜索结果。")
                .references(List.of())
                .build();
        }

        List<String> evidenceSnippets = references.stream()
            .map(this::buildAskEvidenceSnippet)
            .filter(StringUtils::hasText)
            .limit(6)
            .toList();

        String answer = archiveAiChatService.answer(chatModel, command.getQuestion(), references, evidenceSnippets);
        return ArchiveAskResponse.builder().answer(answer).references(references).build();
    }

    @Override
    public List<AiModelConfigResponse> listAiModels() {
        return aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfig>().eq(AiModelConfig::getDeleteFlag, "N").eq(AiModelConfig::getEnabledFlag, "Y").orderByAsc(AiModelConfig::getModelType).orderByAsc(AiModelConfig::getModelCode)).stream().map(item -> AiModelConfigResponse.builder().modelCode(item.getModelCode()).modelName(item.getModelName()).modelType(item.getModelType()).modelIdentifier(item.getModelIdentifier()).embeddingDimension(item.getEmbeddingDimension()).timeoutSeconds(item.getTimeoutSeconds()).topK(item.getTopK()).scoreThreshold(item.getScoreThreshold()).enabledFlag(item.getEnabledFlag()).remark(item.getRemark()).build()).toList();
    }

    @Override
    public ArchiveSummaryResponse getArchiveDetail(Long archiveId) {
        if (archiveId != null && archiveId > 0 && isFormalDocumentRow(archiveId)) {
            return loadArchiveDetailFromDocumentTable(archiveId);
        }
        if (archiveId != null && archiveId > 0 && isPendingDraftRow(archiveId)) {
            return loadArchiveDetailFromDraftTable(archiveId);
        }
        return loadArchiveDetailFromDocumentTable(archiveId);
    }

    @Override
    public ArchiveSummaryResponse createPendingDocument(PendingDocumentWriteCommand command) {
        boolean draft = isDraftMode(command.getSubmitMode());
        long opId = resolveOperatorUserId(command);
        if (draft) {
            if (countPendingDraftsForUser(opId) >= 100) {
                throw new BusinessException("草稿数量已达上限(100)，请先删除或提交部分草稿");
            }
            long draftId = nextPendingDraftId();
            String payload = serializeDraftPayload(command);
            LocalDateTime now = LocalDateTime.now();
            try {
                jdbcTemplate.update(
                    """
                    insert into fdc_pending_document_draft_t (
                      draft_id, payload_json, delete_flag, created_by, creation_date, last_updated_by, last_update_date
                    ) values (?, ?::jsonb, 0, ?, ?, ?, ?)
                    """,
                    draftId,
                    payload,
                    opId,
                    now,
                    opId,
                    now
                );
            } catch (DataAccessException e) {
                Throwable root = e.getMostSpecificCause() != null ? e.getMostSpecificCause() : e;
                log.error("fdc_pending_document_draft_t insert failed draftId={}", draftId, e);
                throw new BusinessException("保存草稿失败: " + root.getMessage());
            }
            Map<String, Object> afterCreate = compactDraftAuditSnapshot(draftId, payload);
            operationAuditService.record(
                "PENDING_ARCHIVE",
                "应归档",
                "FDC_PENDING_DOCUMENT_DRAFT",
                String.valueOf(draftId),
                resolvePendingOperationType(command.getOperationTypeCode(), "DRAFT_SAVE"),
                "保存应归档草稿",
                null,
                afterCreate,
                opId,
                SYSTEM_OPERATOR_NAME,
                trimToNull(command.getOperationRemark()),
                toOperationAuditAttachments(command.getAuditAttachments())
            );
            return loadArchiveDetailFromDraftTable(draftId);
        }
        Long docId = insertFormalFdcDocumentTransaction(command, opId);
        Map<String, Object> afterCreate = snapshotFdcDocumentForAudit(docId);
        String createOperationType = resolvePendingOperationType(command.getOperationTypeCode(), "CREATE");
        operationAuditService.record(
            "PENDING_ARCHIVE",
            "应归档",
            "FDC_DOCUMENT",
            String.valueOf(docId),
            createOperationType,
            "创建应归档数据",
            null,
            afterCreate,
            opId,
            SYSTEM_OPERATOR_NAME,
            trimToNull(command.getOperationRemark()),
            toOperationAuditAttachments(command.getAuditAttachments())
        );
        int attachmentCount = countIntegratedElectronicAttachments(docId);
        if (attachmentCount > 0) {
            operationAuditService.record(
                "PENDING_ARCHIVE",
                "应归档",
                "FDC_DOCUMENT",
                String.valueOf(docId),
                "ATTACH_INTEGRATE",
                "共集成" + attachmentCount + "个电子附件",
                null,
                null,
                opId,
                SYSTEM_OPERATOR_NAME,
                null,
                null
            );
        }
        return getArchiveDetail(docId);
    }

    @Override
    public ArchiveSummaryResponse updatePendingDocument(Long docId, PendingDocumentWriteCommand command) {
        if (docId == null || docId <= 0) {
            throw new BusinessException("docId is invalid");
        }
        if (isPendingDraftRow(docId)) {
            return updatePendingDraftDocument(docId, command);
        }
        boolean draft = isDraftMode(command.getSubmitMode());
        long opId = resolveOperatorUserId(command);
        final Long docIdFinal = docId;
        Map<String, Object> beforeUpdate = snapshotFdcDocumentForAudit(docIdFinal);
        pendingDocumentWriteTemplate.executeWithoutResult(status -> {
            Integer alive = jdbcTemplate.queryForObject(
                """
                select count(*) from fdc_document_t
                 where doc_id = ? and coalesce(delete_flag, 0) = 0 and lifecycle_status in ('UNARCHIVED', 'DRAFT')
                """,
                Integer.class,
                docIdFinal
            );
            if (alive == null || alive == 0) {
                throw new BusinessException("Document not found or not editable");
            }
            String currentBizModule = jdbcTemplate.queryForObject(
                "select biz_module_code from fdc_document_t where doc_id = ?",
                String.class,
                docIdFinal
            );
            if (StringUtils.hasText(command.getDocumentTypeCode()) && StringUtils.hasText(command.getArchiveTypeCode())) {
                String incoming = command.getArchiveTypeCode().trim();
                if (!Objects.equals(incoming, trimToNull(currentBizModule))) {
                    validateBusinessModuleUnderRoot(incoming, command.getDocumentTypeCode().trim());
                }
            }
            String currentCompanyCode = jdbcTemplate.queryForObject(
                "select company_code from fdc_document_t where doc_id = ?",
                String.class,
                docIdFinal
            );
            String companyForFlow = StringUtils.hasText(command.getCompanyProjectCode())
                ? command.getCompanyProjectCode().trim()
                : trimToNull(currentCompanyCode);
            if (!draft) {
                companyForFlow = requireText(companyForFlow, "companyProjectCode");
            } else if (!StringUtils.hasText(companyForFlow)) {
                companyForFlow = resolveDefaultCompanyProjectCodeForDraft();
            }
            String docTypeForFlow = StringUtils.hasText(command.getDocumentTypeCode())
                ? command.getDocumentTypeCode().trim()
                : resolveRootBusinessModuleCode(trimToNull(currentBizModule), listBusinessModuleMap());
            if (!draft) {
                docTypeForFlow = requireText(docTypeForFlow, "documentTypeCode");
            } else if (!StringUtils.hasText(docTypeForFlow)) {
                docTypeForFlow = resolveDefaultDocumentTypeCodeForDraft();
            }
            if (!draft) {
                requireText(command.getBusinessCode(), "businessCode");
                requireText(command.getBeginPeriod(), "beginPeriod");
                requireText(command.getDocumentDate(), "documentDate");
            }
            String moduleForFlow = StringUtils.hasText(command.getArchiveTypeCode()) ? command.getArchiveTypeCode().trim() : trimToNull(currentBizModule);
            if (!draft) {
                requireText(moduleForFlow, "archiveTypeCode");
            } else if (!StringUtils.hasText(moduleForFlow)) {
                moduleForFlow = docTypeForFlow;
            }
            String archDestParam = StringUtils.hasText(command.getArchiveDestination()) ? command.getArchiveDestination().trim() : null;
            // resolveDefaults 第2参为归档规则表 module_code（三级业务模块），第3参为自定义匹配条件
            ArchiveDefaultResolveResponse flow = resolveDefaults(companyForFlow, moduleForFlow, null, archDestParam);
            String archPlace = StringUtils.hasText(archDestParam)
                ? archDestParam
                : (StringUtils.hasText(flow.getArchiveDestination()) ? flow.getArchiveDestination().trim() : "CN");
            String docOrg = StringUtils.hasText(command.getDocumentOrganizationCode())
                ? command.getDocumentOrganizationCode().trim()
                : flow.getDocumentOrganizationCode();
            if (!StringUtils.hasText(docOrg)) {
                if (draft) {
                    docOrg = "DEFAULT";
                } else {
                    throw new BusinessException("documentOrganizationCode cannot be blank");
                }
            }
            LocalDate startPeriodRow = jdbcTemplate.queryForObject(
                "select start_period from fdc_document_t where doc_id = ?",
                LocalDate.class,
                docIdFinal
            );
            if (startPeriodRow == null) {
                throw new BusinessException("document start_period is missing");
            }
            LocalDate startPeriodForUpdate;
            if (draft) {
                if (StringUtils.hasText(command.getBeginPeriod())) {
                    startPeriodForUpdate = parseYearMonthToFirstDay(command.getBeginPeriod().trim());
                } else {
                    startPeriodForUpdate = startPeriodRow;
                }
            } else {
                startPeriodForUpdate = parseYearMonthToFirstDay(requireText(command.getBeginPeriod(), "beginPeriod").trim());
            }
            String docName = requireText(command.getDocumentName(), "documentName");
            String carrier = normalizeCarrierType(StringUtils.hasText(command.getCarrierTypeCode()) ? command.getCarrierTypeCode() : "ELECTRONIC");
            String securityFlowOrInput = StringUtils.hasText(command.getSecurityLevelCode())
                ? command.getSecurityLevelCode().trim()
                : null;
            if (!StringUtils.hasText(securityFlowOrInput)) {
                securityFlowOrInput = "INTERNAL";
            }
            String security = truncateVarchar(securityLevelResolver.requireCanonicalForWrite(securityFlowOrInput), 30);
            String documentDateRaw = trimToNull(command.getDocumentDate());
            LocalDateTime docGenDate;
            if (documentDateRaw != null) {
                docGenDate = parseDocumentDateTime(documentDateRaw);
            } else if (draft) {
                docGenDate = LocalDateTime.now();
            } else {
                docGenDate = parseDocumentDateTime(requireText(command.getDocumentDate(), "documentDate"));
            }
            Long userId = resolveUserIdByLoginName(command.getDutyPerson());
            long deptId = parseDeptId(command.getDutyDepartment());
            Map<String, String> ext = command.getExtValues() == null ? Map.of() : command.getExtValues();
            String visibility = StringUtils.hasText(ext.get("visibility")) ? ext.get("visibility").trim() : "是";
            String barcode = trimToNull(ext.get("barcodeModule"));
            String bizModule = StringUtils.hasText(command.getArchiveTypeCode()) ? command.getArchiveTypeCode().trim() : null;
            String moduleForAttrCols = StringUtils.hasText(bizModule) ? bizModule : trimToNull(currentBizModule);
            Map<String, String> attrCols = buildAttrColumnsFromExt(ext, moduleForAttrCols);
            LocalDate endPeriod = StringUtils.hasText(command.getEndPeriod())
                ? parseYearMonthToLastDay(command.getEndPeriod().trim())
                : startPeriodForUpdate;
            String origin = StringUtils.hasText(command.getOriginPlace()) ? command.getOriginPlace().trim() : archPlace;
            String sourceSystem = StringUtils.hasText(command.getSourceSystem()) ? command.getSourceSystem().trim() : "PORTAL";
            String remark = trimToNull(command.getRemark());
            LocalDateTime now = LocalDateTime.now();
            String lifecycleTarget = draft ? "DRAFT" : "UNARCHIVED";
            String nextDocBizNo;
            if (draft) {
                if (!StringUtils.hasText(trimToNull(command.getBusinessCode()))) {
                    nextDocBizNo = pendingAutoDocBizNo(docIdFinal);
                } else {
                    nextDocBizNo = resolvePendingDocBizNo(command.getBusinessCode().trim(), docIdFinal);
                }
            } else {
                nextDocBizNo = resolvePendingDocBizNo(requireText(command.getBusinessCode(), "businessCode").trim(), docIdFinal);
            }
            CompanyProject cpWrite = requireCompanyProject(companyForFlow);
            String companyCodeSql = cpWrite.getCompanyProjectCode();
            String companyNameSql = cpWrite.getCompanyProjectName();
            if ("UNARCHIVED".equalsIgnoreCase(lifecycleTarget)) {
                String moduleUk = StringUtils.hasText(bizModule) ? bizModule : trimToNull(currentBizModule);
                if (!StringUtils.hasText(moduleUk)) {
                    throw new BusinessException("业务模块缺失，无法校验文档唯一性");
                }
                assertUniqueFormalDocumentNaturalKey(
                    null,
                    companyCodeSql,
                    moduleUk,
                    startPeriodForUpdate,
                    truncateVarchar(nextDocBizNo, 100),
                    docIdFinal);
            }
            if (StringUtils.hasText(bizModule)) {
                List<Object> updParams = new ArrayList<>();
                updParams.add(bizModule);
                updParams.add(startPeriodForUpdate);
                updParams.add(endPeriod);
                updParams.add(truncateVarchar(archPlace, 60));
                updParams.add(truncateVarchar(origin, 60));
                updParams.add(truncateVarchar(docName, 100));
                updParams.add(docGenDate);
                updParams.add(deptId);
                updParams.add(userId);
                updParams.add(carrier);
                updParams.add(truncateVarchar(sourceSystem, 30));
                updParams.add(truncateVarchar(security, 30));
                updParams.add(truncateVarchar(remark, 500));
                updParams.add(truncateVarchar(docOrg, 60));
                updParams.add(truncateVarchar(visibility, 100));
                updParams.add(truncateVarchar(barcode, 100));
                updParams.addAll(Arrays.asList(bindAttrExtendedColumnPlaceholders(attrCols)));
                updParams.add(truncateVarchar(nextDocBizNo, 100));
                updParams.add(truncateVarchar(companyCodeSql, 60));
                updParams.add(truncateVarchar(companyNameSql, 200));
                updParams.add(lifecycleTarget);
                updParams.add(opId);
                updParams.add(now);
                updParams.add(docIdFinal);
                jdbcTemplate.update(SQL_UPDATE_FDC_DOCUMENT_PENDING_WITH_BIZ_MODULE, updParams.toArray());
            } else {
                List<Object> updParams = new ArrayList<>();
                updParams.add(startPeriodForUpdate);
                updParams.add(endPeriod);
                updParams.add(truncateVarchar(archPlace, 60));
                updParams.add(truncateVarchar(origin, 60));
                updParams.add(truncateVarchar(docName, 100));
                updParams.add(docGenDate);
                updParams.add(deptId);
                updParams.add(userId);
                updParams.add(carrier);
                updParams.add(truncateVarchar(sourceSystem, 30));
                updParams.add(truncateVarchar(security, 30));
                updParams.add(truncateVarchar(remark, 500));
                updParams.add(truncateVarchar(docOrg, 60));
                updParams.add(truncateVarchar(visibility, 100));
                updParams.add(truncateVarchar(barcode, 100));
                updParams.addAll(Arrays.asList(bindAttrExtendedColumnPlaceholders(attrCols)));
                updParams.add(truncateVarchar(nextDocBizNo, 100));
                updParams.add(truncateVarchar(companyCodeSql, 60));
                updParams.add(truncateVarchar(companyNameSql, 200));
                updParams.add(lifecycleTarget);
                updParams.add(opId);
                updParams.add(now);
                updParams.add(docIdFinal);
                jdbcTemplate.update(SQL_UPDATE_FDC_DOCUMENT_PENDING_WITHOUT_BIZ_MODULE, updParams.toArray());
            }
        });
        Map<String, Object> afterUpdate = snapshotFdcDocumentForAudit(docIdFinal);
        String updateSummary = buildPendingDocumentUpdateSummary(beforeUpdate, afterUpdate);
        String updateOperationType = resolvePendingOperationType(command.getOperationTypeCode(), draft ? "DRAFT_SAVE" : "UPDATE");
        operationAuditService.record(
            "PENDING_ARCHIVE",
            "应归档",
            "FDC_DOCUMENT",
            String.valueOf(docId),
            updateOperationType,
            draft ? "保存应归档草稿" : updateSummary,
            beforeUpdate,
            afterUpdate,
            opId,
            SYSTEM_OPERATOR_NAME,
            trimToNull(command.getOperationRemark()),
            toOperationAuditAttachments(command.getAuditAttachments())
        );
        if (!draft) {
            int attachmentCount = countIntegratedElectronicAttachments(docIdFinal);
            if (attachmentCount > 0) {
                operationAuditService.record(
                    "PENDING_ARCHIVE",
                    "应归档",
                    "FDC_DOCUMENT",
                    String.valueOf(docId),
                    "ATTACH_INTEGRATE",
                    "共集成" + attachmentCount + "个电子附件",
                    null,
                    null,
                    opId,
                    SYSTEM_OPERATOR_NAME,
                    null,
                    null
                );
            }
        }
        return getArchiveDetail(docId);
    }

    @Override
    public void deletePendingDocuments(PendingDocumentBatchDeleteCommand command, long operatorUserId) {
        if (command == null || command.getDocIds() == null || command.getDocIds().isEmpty()) {
            return;
        }
        long uid = operatorUserId > 0 ? operatorUserId : SYSTEM_OPERATOR_ID;
        for (Long id : command.getDocIds()) {
            if (id == null || id <= 0) {
                continue;
            }
            if (isPendingDraftRow(id)) {
                jdbcTemplate.update(
                    """
                    update fdc_pending_document_draft_t
                       set delete_flag = 1, last_updated_by = ?, last_update_date = current_timestamp
                     where draft_id = ? and created_by = ? and coalesce(delete_flag,0) = 0
                    """,
                    uid,
                    id,
                    uid);
            } else {
                jdbcTemplate.update(
                    """
                    update fdc_document_t set delete_flag = 1, last_updated_by = ?, last_update_date = current_timestamp
                    where doc_id = ? and created_by = ? and coalesce(delete_flag,0) = 0
                      and lifecycle_status in ('DRAFT', 'UNARCHIVED')
                    """,
                    uid,
                    id,
                    uid);
            }
        }
    }

    @Override
    public ArchiveSummaryResponse duplicatePendingDocument(Long sourceDocId, long operatorUserId) {
        if (sourceDocId == null || sourceDocId <= 0) {
            throw new BusinessException("sourceDocId is invalid");
        }
        boolean fromDraft = isPendingDraftRow(sourceDocId);
        Integer alive;
        if (fromDraft) {
            alive = jdbcTemplate.queryForObject(
                """
                select count(*) from fdc_pending_document_draft_t
                 where draft_id = ? and coalesce(delete_flag,0) = 0
                """,
                Integer.class,
                sourceDocId);
        } else {
            alive = jdbcTemplate.queryForObject(
                """
                select count(*) from fdc_document_t
                where doc_id = ? and coalesce(delete_flag,0) = 0 and lifecycle_status in ('DRAFT', 'UNARCHIVED')
                """,
                Integer.class,
                sourceDocId);
        }
        if (alive == null || alive == 0) {
            throw new BusinessException("仅可复制草稿或未归档文档");
        }
        long uid = operatorUserId > 0 ? operatorUserId : SYSTEM_OPERATOR_ID;
        Long owner;
        if (fromDraft) {
            owner = jdbcTemplate.queryForObject(
                "select created_by from fdc_pending_document_draft_t where draft_id = ? and coalesce(delete_flag,0) = 0",
                Long.class,
                sourceDocId);
        } else {
            owner = jdbcTemplate.queryForObject(
                "select created_by from fdc_document_t where doc_id = ? and coalesce(delete_flag,0) = 0",
                Long.class,
                sourceDocId);
        }
        if (owner == null || !owner.equals(uid)) {
            throw new BusinessException("Forbidden");
        }
        ArchiveSummaryResponse src = getArchiveDetail(sourceDocId);
        PendingDocumentWriteCommand cmd = new PendingDocumentWriteCommand();
        cmd.setOperatorUserId(uid);
        cmd.setDocumentTypeCode(requireText(src.getDocumentTypeCode(), "documentTypeCode"));
        cmd.setCompanyProjectCode(requireText(src.getCompanyProjectCode(), "companyProjectCode"));
        String bm = StringUtils.hasText(src.getBusinessModuleTypeCode()) ? src.getBusinessModuleTypeCode() : src.getArchiveTypeCode();
        cmd.setArchiveTypeCode(requireText(bm, "archiveTypeCode"));
        cmd.setBusinessCode("COPY-" + System.currentTimeMillis());
        cmd.setBeginPeriod(trimToNull(src.getBeginPeriod()));
        cmd.setEndPeriod(trimToNull(src.getEndPeriod()));
        cmd.setArchiveDestination(trimToNull(src.getArchiveDestination()));
        cmd.setOriginPlace(trimToNull(src.getOriginPlace()));
        cmd.setDocumentName(requireText(src.getDocumentName(), "documentName"));
        cmd.setDocumentDate(src.getDocumentDate() != null
            ? src.getDocumentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : "");
        cmd.setDutyPerson(StringUtils.hasText(src.getDutyPerson()) ? src.getDutyPerson() : "admin");
        cmd.setDutyDepartment(trimToNull(src.getDutyDepartment()));
        cmd.setCarrierTypeCode(StringUtils.hasText(src.getCarrierTypeCode()) ? src.getCarrierTypeCode() : "ELECTRONIC");
        cmd.setSourceSystem(trimToNull(src.getSourceSystem()));
        cmd.setSecurityLevelCode(StringUtils.hasText(src.getSecurityLevelCode()) ? src.getSecurityLevelCode() : "INTERNAL");
        cmd.setRemark(trimToNull(src.getRemark()));
        cmd.setDocumentOrganizationCode(StringUtils.hasText(src.getDocumentOrganizationCode()) ? src.getDocumentOrganizationCode() : "DEFAULT");
        cmd.setRetentionPeriodYears(src.getRetentionPeriodYears());
        cmd.setCustodyStatus(trimToNull(src.getCustodyStatus()));
        cmd.setSubmitMode("DRAFT");
        cmd.setExtValues(src.getExtValues() != null ? new LinkedHashMap<>(src.getExtValues()) : new LinkedHashMap<>());
        return createPendingDocument(cmd);
    }

    @Override
    public PendingAuditAttachmentRef uploadPendingAuditAttachment(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("file is required");
        }
        String original = file.getOriginalFilename();
        String safeName = StringUtils.hasText(original) ? Paths.get(original).getFileName().toString() : "upload.bin";
        if (safeName.contains("..") || safeName.indexOf('/') >= 0 || safeName.indexOf('\\') >= 0) {
            safeName = "upload.bin";
        }
        String day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = UUID.randomUUID().toString().replace("-", "");
        Path dir = Paths.get(System.getProperty("user.dir"), "storage", "pending-audit", day);
        try {
            Files.createDirectories(dir);
            String storedFileName = key + "_" + safeName;
            Path target = dir.resolve(storedFileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            String relPath = "pending-audit/" + day + "/" + storedFileName;
            String checksum = md5Hex(target);
            FdcFile row = new FdcFile();
            row.setFileName(truncateVarchar(safeName, 500));
            row.setFilePath(truncateVarchar(relPath, 1000));
            row.setFileSize(file.getSize());
            row.setFileType(StringUtils.hasText(file.getContentType()) ? truncateVarchar(file.getContentType(), 50) : null);
            row.setSourceSystem("PORTAL");
            row.setStoragePlatform("LOCAL");
            row.setFileMd5(truncateVarchar(checksum, 64));
            row.setEnableFlag("Y");
            row.setDeleteFlag("N");
            row.setCreatedBy(SYSTEM_OPERATOR_ID);
            row.setCreationDate(LocalDateTime.now());
            fdcFileMapper.insert(row);
            PendingAuditAttachmentRef ref = new PendingAuditAttachmentRef();
            ref.setFileId(row.getFileId());
            ref.setFileName(safeName);
            ref.setStorageKey(relPath);
            ref.setFileSize(file.getSize());
            return ref;
        } catch (IOException ex) {
            throw new BusinessException("Failed to store file: " + ex.getMessage());
        }
    }

    @Override
    public PendingAuditDownload downloadPendingAuditAttachment(Long fileId, String storageKey) {
        String sk = null;
        String displayName = "attachment";
        if (fileId != null && fileId > 0) {
            FdcFile f = fdcFileMapper.selectById(fileId);
            if (f == null || !"N".equals(f.getDeleteFlag())) {
                throw new BusinessException("File not found");
            }
            sk = f.getFilePath();
            displayName = StringUtils.hasText(f.getFileName()) ? f.getFileName() : displayName;
        } else if (StringUtils.hasText(storageKey)) {
            sk = storageKey.trim();
        } else {
            throw new BusinessException("fileId or storageKey is required");
        }
        if (sk.contains("..") || sk.contains("\\") || !sk.startsWith("pending-audit/")) {
            throw new BusinessException("Invalid file path");
        }
        Path base = Paths.get(System.getProperty("user.dir"), "storage").normalize();
        Path resolved = base.resolve(sk).normalize();
        if (!resolved.startsWith(base)) {
            throw new BusinessException("Invalid file path");
        }
        if (!Files.isRegularFile(resolved)) {
            throw new BusinessException("File not found");
        }
        if (fileId == null || fileId <= 0) {
            String fn = resolved.getFileName().toString();
            int us = fn.indexOf('_');
            displayName = us > 0 && us < fn.length() - 1 ? fn.substring(us + 1) : fn;
        }
        String contentType = detectContentType(resolved, null);
        return new PendingAuditDownload(new FileSystemResource(resolved), displayName, contentType);
    }

    @Override
    public PendingAuditDownload downloadArchiveAttachment(Long attachmentId) {
        DocumentAttachmentFile attachment = requireDocumentAttachmentFile(attachmentId);
        Path filePath = requireAttachmentFilePath(attachment.filePath());
        String fileName = StringUtils.hasText(attachment.fileName()) ? attachment.fileName() : filePath.getFileName().toString();
        return new PendingAuditDownload(new FileSystemResource(filePath), fileName, "application/octet-stream");
    }

    @Override
    public PendingAuditDownload previewArchiveAttachment(Long attachmentId) {
        DocumentAttachmentFile attachment = requireDocumentAttachmentFile(attachmentId);
        Path filePath = requireAttachmentFilePath(attachment.filePath());
        String fileName = StringUtils.hasText(attachment.fileName()) ? attachment.fileName() : filePath.getFileName().toString();
        return new PendingAuditDownload(new FileSystemResource(filePath), fileName, detectContentType(filePath, attachment.mimeType()));
    }

    @Override
    public PendingAuditDownload downloadArchiveAttachmentsZip(Long archiveId) {
        if (archiveId == null || archiveId <= 0) {
            throw new BusinessException("archiveId is invalid");
        }
        List<DocumentAttachmentFile> attachments = listDocumentAttachmentFiles(archiveId);
        if (attachments.isEmpty()) {
            throw new BusinessException("当前文档无可下载附件");
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            Set<String> usedNames = new HashSet<>();
            for (DocumentAttachmentFile attachment : attachments) {
                Path filePath = requireAttachmentFilePath(attachment.filePath());
                String preferredName = StringUtils.hasText(attachment.fileName()) ? attachment.fileName() : filePath.getFileName().toString();
                String entryName = uniqueZipEntryName(preferredName, usedNames);
                zos.putNextEntry(new ZipEntry(entryName));
                try (InputStream in = Files.newInputStream(filePath)) {
                    in.transferTo(zos);
                }
                zos.closeEntry();
            }
            zos.finish();
            byte[] zipped = bos.toByteArray();
            return new PendingAuditDownload(new org.springframework.core.io.ByteArrayResource(zipped), "archive-" + archiveId + "-attachments.zip", "application/zip");
        } catch (IOException ex) {
            throw new BusinessException("打包附件失败: " + ex.getMessage());
        }
    }

    @Override
    public WorkspaceIoJobSummaryResponse createPendingDocumentsExportJob(List<Long> docIds, String exportFileFormat, String exportScope, Long operatorUserId) {
        List<Long> ids = (docIds == null ? List.<Long>of() : docIds).stream()
            .filter(Objects::nonNull)
            .filter(v -> v > 0)
            .distinct()
            .toList();
        if (ids.isEmpty()) {
            throw new BusinessException("docIds is required");
        }
        String csv = buildPendingExportCsv(ids, exportScope);
        WorkspaceIoJobCreateCommand create = new WorkspaceIoJobCreateCommand();
        create.setJobType("EXPORT_QUERY");
        create.setDataType("DOCUMENT");
        create.setJobName("文档查询/应归档数据批量导出");
        create.setInputTotal(ids.size());
        create.setResultTotal(ids.size());
        create.setJobStatus("COMPLETED");
        create.setExportFileFormat(StringUtils.hasText(exportFileFormat) ? exportFileFormat.trim().toUpperCase(Locale.ROOT) : "CSV");
        create.setResultArtifactText(csv);
        return workspaceIoJobService.create(create, operatorUserId != null && operatorUserId > 0 ? operatorUserId : 1L);
    }

    @Override
    public WorkspaceIoJobSummaryResponse submitArchiveImportQueryJob(MultipartFile file, String documentTypeCode, Long operatorUserId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请上传 CSV 文件");
        }
        if (!StringUtils.hasText(documentTypeCode)) {
            throw new BusinessException("documentTypeCode is required");
        }
        long uid = operatorUserId != null && operatorUserId > 0 ? operatorUserId : 1L;
        WorkspaceIoJobCreateCommand create = new WorkspaceIoJobCreateCommand();
        create.setJobType("IMPORT_QUERY");
        create.setDataType("DOCUMENT");
        create.setJobName(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "import-query.csv");
        create.setDocumentTypeCode(documentTypeCode.trim());
        create.setInputFileName(create.getJobName());
        create.setJobStatus("RUNNING");
        WorkspaceIoJobSummaryResponse started = workspaceIoJobService.create(create, uid);
        final long jobId = started.getJobId();
        final byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException("读取文件失败: " + ex.getMessage());
        }
        final String docType = documentTypeCode.trim();
        taskExecutor.execute(() -> runArchiveImportQueryJob(jobId, content, docType, uid));
        return started;
    }

    private void runArchiveImportQueryJob(long jobId, byte[] content, String documentTypeCode, long operatorUserId) {
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
            Map<Long, ImportQueryResultRow> dedup = new LinkedHashMap<>();
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
                    ArchiveQueryCommand cmd = new ArchiveQueryCommand();
                    cmd.setDocumentTypeCode(documentTypeCode);
                    cmd.setCompanyProjectCode(trimToNull(companyCode));
                    cmd.setArchiveTypeCode(trimToNull(archiveTypeCode));
                    cmd.setBusinessCode(trimToNull(businessCode));
                    cmd.setBeginPeriod(trimToNull(beginPeriod));
                    cmd.setEndPeriod(trimToNull(beginPeriod));
                    cmd.setPage(1);
                    cmd.setPageSize(1000);
                    Map<String, String> ext = new LinkedHashMap<>();
                    if (StringUtils.hasText(invoiceNo)) {
                        ext.put("invoiceNo", invoiceNo.trim());
                    }
                    if (StringUtils.hasText(refNo)) {
                        ext.put("refNo", refNo.trim());
                    }
                    cmd.setExtFilters(ext);
                    ArchiveQueryResponse first = queryArchives(cmd);
                    List<ArchiveSummaryResponse> matched = new ArrayList<>(first.getRecords() == null ? List.of() : first.getRecords());
                    int pageSize = first.getPageSize() != null && first.getPageSize() > 0 ? first.getPageSize() : 1000;
                    long total = first.getTotal() != null ? first.getTotal() : matched.size();
                    int pages = Math.max(1, (int) Math.ceil(total * 1.0 / pageSize));
                    for (int p = 2; p <= pages; p++) {
                        cmd.setPage(p);
                        ArchiveQueryResponse next = queryArchives(cmd);
                        if (next.getRecords() != null) {
                            matched.addAll(next.getRecords());
                        }
                    }
                    if (StringUtils.hasText(businessCode)) {
                        matched = matched.stream()
                            .filter(r -> StringUtils.hasText(r.getBusinessCode())
                                && businessCode.trim().equalsIgnoreCase(r.getBusinessCode().trim()))
                            .toList();
                    }
                    matchedCounts.add(matched.size());
                    rowErrors.add("");
                    for (ArchiveSummaryResponse r : matched) {
                        if (r.getArchiveId() == null) {
                            continue;
                        }
                        dedup.putIfAbsent(r.getArchiveId(), new ImportQueryResultRow(
                            rowNo,
                            r.getArchiveId(),
                            String.valueOf(r.getArchiveId()),
                            r.getBusinessCode(),
                            r.getDocumentName(),
                            r.getArchiveStatus(),
                            r.getLifecycleStatus()
                        ));
                    }
                    successRows++;
                } catch (Exception ex) {
                    String err = ex.getMessage() == null ? "查询失败" : ex.getMessage();
                    failed.add("第" + (i + 1) + "行：" + err);
                    matchedCounts.add(0);
                    rowErrors.add(err);
                }
            }
            List<ImportQueryResultRow> results = new ArrayList<>(dedup.values());
            persistImportQueryResults(jobId, operatorUserId, results);
            String failedCsv = buildImportQueryAnnotatedCsv(headers, parsedRows, matchedCounts, rowErrors);
            String resultCsv = results.isEmpty()
                ? ""
                : buildPendingExportCsv(results.stream().map(ImportQueryResultRow::archiveId).distinct().toList(), "DOCUMENT_QUERY");
            String status = failed.isEmpty() ? "SUCCESS" : (successRows > 0 ? "PARTIAL_FAILED" : "FAILED");
            String err = failed.isEmpty() ? null : ("存在 " + failed.size() + " 行失败");
            updateImportQueryJob(jobId, operatorUserId, inputTotal, results.size(), System.currentTimeMillis() - t0, status, err, failedCsv, resultCsv);
        } catch (Exception ex) {
            updateImportQueryJob(jobId, operatorUserId, inputTotal, 0, System.currentTimeMillis() - t0, "FAILED", ex.getMessage(), null, null);
        }
    }

    private void persistImportQueryResults(long jobId, long operatorUserId, List<ImportQueryResultRow> rows) {
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
                    ps.setLong(1, jobId);
                    ps.setInt(2, r.queryRowNo());
                    ps.setLong(3, r.archiveId());
                    ps.setString(4, r.docId());
                    ps.setString(5, r.businessCode());
                    ps.setString(6, r.documentName());
                    ps.setString(7, r.docStatus());
                    ps.setString(8, r.lifecycleStatus());
                    ps.setLong(9, operatorUserId);
                }
            );
        }
        jdbcTemplate.update(
            "delete from fdc_workspace_import_query_result_t where creation_date < current_timestamp - interval '90 days'"
        );
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
            inputTotal,
            resultTotal,
            durationMs,
            status,
            errorMessage,
            failedFileCsv,
            resultArtifactText,
            resultArtifactText,
            resultArtifactText,
            operatorUserId,
            jobId
        );
    }

    private String buildImportQueryAnnotatedCsv(String[] headers, List<String[]> rows, List<Integer> matchedCounts, List<String> rowErrors) {
        List<String> out = new ArrayList<>();
        List<String> hdr = new ArrayList<>(Arrays.asList(headers));
        hdr.add("本行命中条数");
        hdr.add("失败原因");
        out.add(hdr.stream().map(this::csvEscape).collect(Collectors.joining(",")));
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            List<String> cells = new ArrayList<>(Arrays.asList(row));
            cells.add(String.valueOf(i < matchedCounts.size() ? matchedCounts.get(i) : 0));
            cells.add(i < rowErrors.size() ? Objects.toString(rowErrors.get(i), "") : "");
            out.add(cells.stream().map(this::csvEscape).collect(Collectors.joining(",")));
        }
        return String.join("\n", out);
    }

    private static String buildImportQueryFailedCsv(List<String> failedRows) {
        List<String> lines = new ArrayList<>();
        lines.add("行号,失败原因");
        for (String line : failedRows) {
            String rowNo = "";
            String reason = line;
            int idx = line.indexOf('：');
            if (idx > 0 && line.startsWith("第") && line.contains("行")) {
                rowNo = line.substring(1, line.indexOf('行'));
                reason = line.substring(idx + 1);
            }
            lines.add("\"" + rowNo.replace("\"", "\"\"") + "\",\"" + reason.replace("\"", "\"\"") + "\"");
        }
        return String.join("\n", lines);
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
        for (String k : List.of("businessCode", "invoiceNo", "refNo", "companyCode", "archiveTypeCode", "beginPeriod")) {
            if (!idx.containsKey(k)) {
                throw new BusinessException("模板字段不正确，请下载最新模板后重试");
            }
        }
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
        if (idx == null || idx < 0 || idx >= cols.length) {
            return "";
        }
        return cols[idx] == null ? "" : cols[idx].trim();
    }

    private record ImportQueryResultRow(
        int queryRowNo,
        long archiveId,
        String docId,
        String businessCode,
        String documentName,
        String docStatus,
        String lifecycleStatus
    ) {}

    private String buildPendingExportCsv(List<Long> docIds, String exportScope) {
        boolean pendingArchiveScope = "PENDING_ARCHIVE".equalsIgnoreCase(trimToNull(exportScope));
        String inSql = docIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Map<String, BusinessModule> businessModuleMap = listBusinessModuleMap();
        Map<String, String> carrierTypeNameMap = listCarrierTypeNameMap();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            select d.doc_id, d.doc_biz_no, d.doc_name, d.biz_module_code, d.company_name, d.company_code,
                   to_char(start_period,'YYYY-MM') as start_period,
                   to_char(end_period,'YYYY-MM') as end_period,
                   d.arch_place_alpha2_code, d.origin_place_alpha2_code, d.doc_organization_code,
                   d.doc_resp_dept_id, d.carrier_type, d.source_system, d.security_level, d.lifecycle_status,
                   d.custody_status, d.description, d.attr1, d.arch_barcode,
                   d.attr41, d.attr42, d.attr43, d.attr44, d.attr45, d.attr46, d.attr47, d.attr48, d.attr49, d.attr50,
                   d.attr51, d.attr52, d.attr53, d.attr54, d.attr55, d.attr56, d.attr57, d.attr58, d.attr59, d.attr60,
                   d.copies_qty, d.remaining_copies_qty,
                   coalesce(u.user_name, cast(d.doc_resp_person_id as varchar)) as duty_person_name,
                   coalesce(cu.user_name, cast(d.created_by as varchar)) as created_by_name,
                   cp.country_code, geo.rep_office_name, geo.region_name, cp.company_tag,
                   to_char(doc_gen_date, 'YYYY-MM-DD HH24:MI:SS') as doc_gen_date,
                   to_char(d.creation_date, 'YYYY-MM-DD HH24:MI:SS') as creation_date,
                   to_char(d.last_update_date, 'YYYY-MM-DD HH24:MI:SS') as last_update_date
              from fdc_document_t d
              left join tpl_user_t u on u.user_id = d.doc_resp_person_id
              left join tpl_user_t cu on cu.user_id = d.created_by
              left join fdc_company_project_t cp on cp.company_project_code = d.company_code and cp.delete_flag = 'N'
              left join (
                    select country_code,
                           min(rep_office_name) as rep_office_name,
                           min(region_name) as region_name
                      from fdc_geo_region_t
                     where delete_flag = 'N'
                     group by country_code
              ) geo on geo.country_code = cp.country_code
             where coalesce(d.delete_flag,0) = 0 and d.doc_id in (""" + inSql + ") order by d.doc_id desc"
        );
        List<String> lines = new ArrayList<>();
        if (pendingArchiveScope) {
            lines.add(
                "文档ID,文档类型,文档业务编码,公司,业务模块,开始档期,结束档期,归档地,产生地,文档名称,文档生成日期,归档责任人,文档责任部门,载体类型,系统来源,密级,文档生命周期状态,创建时间,创建人,描述,"
                    + "国家,代表处,地区部,公司标签,发票号,其他相关编号1,其他相关编号2,其他相关编号3,其他相关编号4,其他相关编号5,会计,扫描员,开立日期,到期日,保函失效日期,保函台账状态,银行名称,币种,金额,签发机构,报废时间,业务册号,保函电子流编号,保函编号,"
                    + "文档组织,是否可见,条码模块,保管状态,最后修改时间"
            );
        } else {
            lines.add(
                "文档类型,文档业务编码,公司,业务模块,开始档期,结束档期,归档地,产生地,文档名称,文档生成日期,归档责任人,文档责任部门,载体类型,系统来源,密级,文档生命周期状态,创建时间,创建人,描述,"
                    + "国家,代表处,地区部,公司标签,发票号,其他相关编号1,其他相关编号2,其他相关编号3,其他相关编号4,其他相关编号5,会计,扫描员,开立日期,到期日,保函失效日期,保函台账状态,银行名称,币种,金额,签发机构,报废时间,业务册号,保函电子流编号,保函编号,"
                    + "文档组织,是否可见,条码模块,档案条码,文档编号,册号,册条码,保管状态,库房,库位,份数,剩余份数,最后修改时间"
            );
        }
        for (Map<String, Object> row : rows) {
            String bizModuleCode = Objects.toString(row.get("biz_module_code"), "");
            String documentTypeCode = resolveRootBusinessModuleCode(bizModuleCode, businessModuleMap);
            String documentTypeName = resolveRootBusinessModuleName(bizModuleCode, businessModuleMap);
            SecurityLevelResolver.Resolved secLv = securityLevelResolver.resolve(Objects.toString(row.get("security_level"), ""));
            String lifecycleStatus = Objects.toString(row.get("lifecycle_status"), "");
            List<Object> exportValues = new ArrayList<>(Stream.of(
                StringUtils.hasText(documentTypeName) ? documentTypeName : documentTypeCode,
                row.get("doc_biz_no"),
                row.get("company_name"),
                resolveBusinessModuleDisplayName(bizModuleCode, businessModuleMap),
                row.get("start_period"),
                row.get("end_period"),
                row.get("arch_place_alpha2_code"),
                row.get("origin_place_alpha2_code"),
                row.get("doc_name"),
                row.get("doc_gen_date"),
                row.get("duty_person_name"),
                row.get("doc_resp_dept_id"),
                carrierTypeNameMap.getOrDefault(Objects.toString(row.get("carrier_type"), ""), Objects.toString(row.get("carrier_type"), "")),
                row.get("source_system"),
                secLv.displayName(),
                "ARCHIVED".equalsIgnoreCase(lifecycleStatus) ? "已归档" : ("DRAFT".equalsIgnoreCase(lifecycleStatus) ? "草稿" : "未归档"),
                row.get("creation_date"),
                row.get("created_by_name"),
                row.get("description"),
                row.get("country_code"),
                row.get("rep_office_name"),
                row.get("region_name"),
                row.get("company_tag"),
                row.get("attr41"),
                row.get("attr42"),
                row.get("attr43"),
                row.get("attr44"),
                row.get("attr45"),
                row.get("attr46"),
                row.get("attr47"),
                row.get("attr48"),
                row.get("attr49"),
                row.get("attr50"),
                row.get("attr51"),
                row.get("attr52"),
                row.get("attr53"),
                row.get("attr54"),
                row.get("attr55"),
                row.get("attr56"),
                row.get("attr57"),
                row.get("attr58"),
                row.get("attr59"),
                row.get("attr60"),
                row.get("doc_organization_code"),
                StringUtils.hasText(Objects.toString(row.get("attr1"), "")) ? row.get("attr1") : "是",
                row.get("arch_barcode")
            ).toList());
            if (pendingArchiveScope) {
                exportValues.add(0, row.get("doc_id"));
            }
            if (!pendingArchiveScope) {
                exportValues.add(""); // 档案条码
                exportValues.add(""); // 文档编号
                exportValues.add(""); // 册号
                exportValues.add(""); // 册条码
            }
            exportValues.add(row.get("custody_status")); // 保管状态
            if (!pendingArchiveScope) {
                exportValues.add(""); // 库房
                exportValues.add(""); // 库位
                exportValues.add(row.get("copies_qty")); // 份数
                exportValues.add(row.get("remaining_copies_qty")); // 剩余份数
            }
            exportValues.add(row.get("last_update_date"));
            lines.add(exportValues.stream().map(v -> csvEscape(Objects.toString(v, ""))).collect(Collectors.joining(",")));
        }
        Set<Long> exportedDocIds = rows.stream()
            .map(r -> ((Number) r.get("doc_id")).longValue())
            .collect(Collectors.toCollection(HashSet::new));
        List<Long> remaining = docIds.stream()
            .filter(id -> id != null && id > 0 && !exportedDocIds.contains(id))
            .sorted(Comparator.reverseOrder())
            .toList();
        for (Long draftId : remaining) {
            String draftLine = buildPendingExportCsvLineForDraft(
                draftId,
                pendingArchiveScope,
                businessModuleMap,
                carrierTypeNameMap
            );
            if (draftLine != null) {
                lines.add(draftLine);
            }
        }
        return String.join("\n", lines);
    }

    /**
     * 应归档草稿仅存于 {@code fdc_pending_document_draft_t}，批量导出时需从 payload_json 补行。
     */
    private String buildPendingExportCsvLineForDraft(
        long draftId,
        boolean pendingArchiveScope,
        Map<String, BusinessModule> documentTypeMap,
        Map<String, String> carrierTypeNameMap
    ) {
        DraftExportMeta meta = jdbcTemplate.query(
            """
            select payload_json::text, created_by,
                   to_char(creation_date, 'YYYY-MM-DD HH24:MI:SS') as creation_date,
                   to_char(last_update_date, 'YYYY-MM-DD HH24:MI:SS') as last_update_date
              from fdc_pending_document_draft_t
             where draft_id = ? and coalesce(delete_flag, 0) = 0
            """,
            rs -> {
                if (!rs.next()) {
                    return null;
                }
                return new DraftExportMeta(
                    rs.getString(1),
                    rs.getLong(2),
                    Objects.toString(rs.getString(3), ""),
                    Objects.toString(rs.getString(4), "")
                );
            },
            draftId
        );
        if (meta == null || !StringUtils.hasText(meta.payloadJson())) {
            return null;
        }
        PendingDocumentWriteCommand cmd;
        try {
            cmd = objectMapper.readValue(meta.payloadJson(), PendingDocumentWriteCommand.class);
        } catch (Exception ex) {
            log.warn("pending export: skip draft_id={}, {}", draftId, ex.getMessage());
            return null;
        }
        String bizModule = trimToNull(cmd.getArchiveTypeCode()) != null ? cmd.getArchiveTypeCode().trim() : "";
        String documentTypeCode = resolveRootBusinessModuleCode(bizModule, documentTypeMap);
        String documentTypeName = resolveRootBusinessModuleName(bizModule, documentTypeMap);
        SecurityLevelResolver.Resolved secLv = securityLevelResolver.resolve(Objects.toString(cmd.getSecurityLevelCode(), ""));
        Map<String, String> attrCols = buildAttrColumnsFromExt(cmd.getExtValues(), StringUtils.hasText(bizModule) ? bizModule : null);
        Map<String, Object> geoRow = loadCompanyGeoRowForExport(trimToNull(cmd.getCompanyProjectCode()));
        String companyName = resolveCompanyProjectNameForExport(trimToNull(cmd.getCompanyProjectCode()));
        String visibility = "";
        String barcodeModule = "";
        if (cmd.getExtValues() != null) {
            visibility = Objects.toString(cmd.getExtValues().get("visibility"), "");
            barcodeModule = Objects.toString(cmd.getExtValues().get("barcodeModule"), "");
        }
        List<Object> exportValues = new ArrayList<>(Stream.of(
            StringUtils.hasText(documentTypeName) ? documentTypeName : documentTypeCode,
            cmd.getBusinessCode(),
            companyName,
            resolveBusinessModuleDisplayName(bizModule, documentTypeMap),
            Objects.toString(cmd.getBeginPeriod(), ""),
            Objects.toString(cmd.getEndPeriod(), ""),
            Objects.toString(cmd.getArchiveDestination(), ""),
            Objects.toString(cmd.getOriginPlace(), ""),
            Objects.toString(cmd.getDocumentName(), ""),
            Objects.toString(cmd.getDocumentDate(), ""),
            Objects.toString(cmd.getDutyPerson(), ""),
            Objects.toString(cmd.getDutyDepartment(), ""),
            carrierTypeNameMap.getOrDefault(Objects.toString(cmd.getCarrierTypeCode(), ""), Objects.toString(cmd.getCarrierTypeCode(), "")),
            Objects.toString(cmd.getSourceSystem(), ""),
            secLv.displayName(),
            "草稿",
            meta.creationDate(),
            resolveTplUserDisplayName(meta.createdBy()),
            Objects.toString(cmd.getRemark(), ""),
            geoRow.get("country_code"),
            geoRow.get("rep_office_name"),
            geoRow.get("region_name"),
            geoRow.get("company_tag"),
            attrCols.get("attr41"),
            attrCols.get("attr42"),
            attrCols.get("attr43"),
            attrCols.get("attr44"),
            attrCols.get("attr45"),
            attrCols.get("attr46"),
            attrCols.get("attr47"),
            attrCols.get("attr48"),
            attrCols.get("attr49"),
            attrCols.get("attr50"),
            attrCols.get("attr51"),
            attrCols.get("attr52"),
            attrCols.get("attr53"),
            attrCols.get("attr54"),
            attrCols.get("attr55"),
            attrCols.get("attr56"),
            attrCols.get("attr57"),
            attrCols.get("attr58"),
            attrCols.get("attr59"),
            attrCols.get("attr60"),
            Objects.toString(cmd.getDocumentOrganizationCode(), ""),
            StringUtils.hasText(visibility) ? visibility : "是",
            barcodeModule
        ).toList());
        if (pendingArchiveScope) {
            exportValues.add(0, draftId);
        }
        if (!pendingArchiveScope) {
            exportValues.add(""); // 档案条码
            exportValues.add(""); // 文档编号
            exportValues.add(""); // 册号
            exportValues.add(""); // 册条码
        }
        exportValues.add(Objects.toString(cmd.getCustodyStatus(), ""));
        if (!pendingArchiveScope) {
            exportValues.add(""); // 库房
            exportValues.add(""); // 库位
            exportValues.add(""); // 份数
            exportValues.add(""); // 剩余份数
        }
        exportValues.add(meta.lastUpdateDate());
        return exportValues.stream().map(v -> csvEscape(Objects.toString(v, ""))).collect(Collectors.joining(","));
    }

    private record DraftExportMeta(String payloadJson, long createdBy, String creationDate, String lastUpdateDate) {}

    private String resolveCompanyProjectNameForExport(String companyProjectCode) {
        if (!StringUtils.hasText(companyProjectCode)) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList(
            """
            select company_project_name from fdc_company_project_t
             where company_project_code = ? and delete_flag = 'N' limit 1
            """,
            String.class,
            companyProjectCode.trim()
        );
        return names.isEmpty() ? companyProjectCode.trim() : names.get(0);
    }

    private Map<String, Object> loadCompanyGeoRowForExport(String companyProjectCode) {
        if (!StringUtils.hasText(companyProjectCode)) {
            return Map.of(
                "country_code", "",
                "rep_office_name", "",
                "region_name", "",
                "company_tag", ""
            );
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
            select cp.country_code, geo.rep_office_name, geo.region_name, cp.company_tag
              from fdc_company_project_t cp
              left join (
                    select country_code,
                           min(rep_office_name) as rep_office_name,
                           min(region_name) as region_name
                      from fdc_geo_region_t
                     where delete_flag = 'N'
                     group by country_code
              ) geo on geo.country_code = cp.country_code
             where cp.company_project_code = ? and cp.delete_flag = 'N'
             limit 1
            """,
            companyProjectCode.trim()
        );
        if (rows.isEmpty()) {
            return Map.of(
                "country_code", "",
                "rep_office_name", "",
                "region_name", "",
                "company_tag", ""
            );
        }
        return rows.get(0);
    }

    private String resolveTplUserDisplayName(long userId) {
        if (userId <= 0) {
            return "";
        }
        List<String> names = jdbcTemplate.queryForList(
            "select user_name from tpl_user_t where user_id = ? and delete_flag = 'N' limit 1",
            String.class,
            userId
        );
        return names.isEmpty() ? String.valueOf(userId) : names.get(0);
    }

    private String csvEscape(String value) {
        String escaped = Objects.toString(value, "").replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private DocumentAttachmentFile requireDocumentAttachmentFile(Long attachmentId) {
        if (attachmentId == null || attachmentId <= 0) {
            throw new BusinessException("attachmentId is invalid");
        }
        List<DocumentAttachmentFile> rows = jdbcTemplate.query(
            """
            select da.document_attach_id as attachment_id,
                   da.document_id as archive_id,
                   coalesce(f.file_name, concat('attachment-', da.document_attach_id)) as file_name,
                   f.file_type as mime_type,
                   f.file_path
              from fdc_document_attach_t da
              left join fdc_file_t f on f.file_id = da.file_id
             where da.document_attach_id = ?
               and coalesce(da.delete_flag,'N') = 'N'
               and coalesce(da.enable_flag,'Y') = 'Y'
               and (f.file_id is null or (coalesce(f.delete_flag,'N')='N' and coalesce(f.enable_flag,'Y')='Y'))
            """,
            (rs, rowNum) -> new DocumentAttachmentFile(
                rs.getLong("attachment_id"),
                rs.getLong("archive_id"),
                rs.getString("file_name"),
                rs.getString("mime_type"),
                rs.getString("file_path")
            ),
            attachmentId
        );
        if (rows.isEmpty()) {
            throw new BusinessException("附件不存在");
        }
        return rows.get(0);
    }

    private List<DocumentAttachmentFile> listDocumentAttachmentFiles(Long archiveId) {
        return jdbcTemplate.query(
            """
            select da.document_attach_id as attachment_id,
                   da.document_id as archive_id,
                   coalesce(f.file_name, concat('attachment-', da.document_attach_id)) as file_name,
                   f.file_type as mime_type,
                   f.file_path
              from fdc_document_attach_t da
              left join fdc_file_t f on f.file_id = da.file_id
             where da.document_id = ?
               and coalesce(da.delete_flag,'N') = 'N'
               and coalesce(da.enable_flag,'Y') = 'Y'
               and (f.file_id is null or (coalesce(f.delete_flag,'N')='N' and coalesce(f.enable_flag,'Y')='Y'))
             order by da.document_attach_id
            """,
            (rs, rowNum) -> new DocumentAttachmentFile(
                rs.getLong("attachment_id"),
                rs.getLong("archive_id"),
                rs.getString("file_name"),
                rs.getString("mime_type"),
                rs.getString("file_path")
            ),
            archiveId
        );
    }

    private Path requireAttachmentFilePath(String candidatePath) {
        String rawPath = trimToNull(candidatePath);
        if (!StringUtils.hasText(rawPath)) {
            throw new BusinessException("附件存储路径缺失");
        }
        Path input = Paths.get(rawPath).normalize();
        if (input.isAbsolute()) {
            if (!Files.isRegularFile(input)) {
                throw new BusinessException("附件文件不存在");
            }
            return input;
        }

        Path cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<Path> candidates = new ArrayList<>();
        candidates.add(cwd.resolve(rawPath).normalize());
        candidates.add(cwd.resolve("storage").resolve(rawPath).normalize());
        Path parent = cwd.getParent();
        if (parent != null) {
            candidates.add(parent.resolve(rawPath).normalize());
            candidates.add(parent.resolve("storage").resolve(rawPath).normalize());
        }
        if (rawPath.startsWith("backend/") && parent != null) {
            candidates.add(parent.resolve(rawPath.substring("backend/".length())).normalize());
        }
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("附件文件不存在");
    }

    private String detectContentType(Path path, String fallbackMime) {
        try {
            String probe = Files.probeContentType(path);
            if (StringUtils.hasText(probe)) return probe;
        } catch (IOException ignored) {}
        String mime = trimToNull(fallbackMime);
        if (StringUtils.hasText(mime)) return mime;
        return "application/octet-stream";
    }

    private String uniqueZipEntryName(String originalName, Set<String> usedNames) {
        String base = StringUtils.hasText(originalName) ? Paths.get(originalName).getFileName().toString() : "attachment.bin";
        if (usedNames.add(base)) return base;
        String name = base;
        String ext = "";
        int dot = base.lastIndexOf('.');
        if (dot > 0 && dot < base.length() - 1) {
            name = base.substring(0, dot);
            ext = base.substring(dot);
        }
        int i = 2;
        while (true) {
            String candidate = name + "(" + i + ")" + ext;
            if (usedNames.add(candidate)) return candidate;
            i++;
        }
    }

    private record DocumentAttachmentFile(Long attachmentId, Long archiveId, String fileName, String mimeType, String filePath) {}

    /** 操作审计快照（fdc_document_t 应归档行） */
    private Map<String, Object> snapshotFdcDocumentForAudit(Long docId) {
        try {
            return new LinkedHashMap<>(jdbcTemplate.queryForMap(
                """
                select doc_id, company_code, biz_module_code, doc_biz_no, doc_name,
                       doc_resp_person_id, doc_resp_dept_id,
                       doc_gen_date,
                       start_period, end_period, doc_organization_code, security_level,
                       lifecycle_status, custody_status, carrier_type, source_system,
                       arch_place_alpha2_code, origin_place_alpha2_code,
                       attr1, arch_barcode,
                       """
                + SQL_SELECT_FDC_DOC_ATTR2_TO_100
                + """
                       ,
                       description, last_update_date
                from fdc_document_t
                where doc_id = ? and coalesce(delete_flag, 0) = 0
                """,
                docId));
        } catch (EmptyResultDataAccessException ex) {
            return Map.of("docId", docId, "missing", Boolean.TRUE);
        }
    }

    private long nextFdcDocumentId() {
        Long max = jdbcTemplate.queryForObject("select coalesce(max(doc_id), 0) from fdc_document_t", Long.class);
        return max == null ? 1L : max + 1;
    }

    private static String pendingAutoDocBizNo(long docId) {
        return "PENDING-" + docId;
    }

    private static String normalizePendingBusinessCodeForApi(long docId, String docBizNo, String lifecycleStatus) {
        if (lifecycleStatus == null || !"DRAFT".equalsIgnoreCase(lifecycleStatus.trim())) {
            return docBizNo == null ? "" : docBizNo;
        }
        if (docBizNo == null) {
            return "";
        }
        String v = docBizNo.trim();
        if (v.isEmpty()) {
            return "";
        }
        if (pendingAutoDocBizNo(docId).equalsIgnoreCase(v)) {
            return "";
        }
        if ("DRAFT".equalsIgnoreCase(v)) {
            return "";
        }
        return docBizNo;
    }

    private static final long FDC_DOC_TENANT_FALLBACK = 1L;

    /**
     * 正式文档（非草稿）在租户内由「公司编码 + 业务模块 + 开始档期 + 文档业务编码」唯一定位；草稿占位 PENDING- 不参与校验。
     */
    private void assertUniqueFormalDocumentNaturalKey(
        Long tenantId,
        String companyCode,
        String bizModuleCode,
        LocalDate startPeriod,
        String docBizNo,
        Long excludeDocId) {
        if (!StringUtils.hasText(companyCode) || !StringUtils.hasText(bizModuleCode) || startPeriod == null
            || !StringUtils.hasText(docBizNo)) {
            return;
        }
        String biz = docBizNo.trim();
        if (biz.toUpperCase(Locale.ROOT).startsWith("PENDING-")) {
            return;
        }
        long tid = tenantId != null && tenantId > 0 ? tenantId : FDC_DOC_TENANT_FALLBACK;
        Integer cnt = jdbcTemplate.queryForObject(
            """
            select count(*) from fdc_document_t
             where coalesce(delete_flag, 0) = 0
               and lower(trim(coalesce(lifecycle_status, ''))) <> 'draft'
               and coalesce(tenantid, 1) = ?
               and company_code = ?
               and biz_module_code = ?
               and start_period = ?
               and doc_biz_no = ?
               and (?::bigint is null or doc_id <> ?::bigint)
            """,
            Integer.class,
            tid,
            companyCode.trim(),
            bizModuleCode.trim(),
            startPeriod,
            truncateVarchar(biz, 100),
            excludeDocId,
            excludeDocId
        );
        if (cnt != null && cnt > 0) {
            throw new BusinessException(
                "创建失败：文档业务编码+公司编码+业务模块编码+开始档期已存在，请检查后重试。"
            );
        }
    }

    private String resolvePendingDocBizNo(String requestedBizNo, long docId) {
        String candidate = StringUtils.hasText(requestedBizNo) ? requestedBizNo.trim() : pendingAutoDocBizNo(docId);
        return truncateVarchar(candidate, 100);
    }

    /**
     * 业务模块须为配置树中二级或三级节点（排除一级根节点）；须启用且归属所选一级文档类型。
     * 业务模块校验以三级节点优先，
     * 但兼容历史数据中 biz_module_code 落在二级节点的情况。
     */
    private void validateBusinessModuleUnderRoot(String moduleTypeCode, String rootCode) {
        Map<String, BusinessModule> map = listBusinessModuleMap();
        BusinessModule module = businessModuleMapper.selectOne(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getModuleCode, moduleTypeCode.trim())
            .last("limit 1"));
        if (module == null) {
            throw new BusinessException("archiveTypeCode is not a valid business module code in business module tree");
        }
        if (!"Y".equals(module.getEnabledFlag())) {
            throw new BusinessException("archiveTypeCode refers to a disabled business module");
        }
        Integer level = module.getLevelNum();
        if (level != null && level < 2) {
            throw new BusinessException("archiveTypeCode cannot be a level-1 (root) business module; choose a level-2 or level-3 module");
        }
        if (level != null && level > 3) {
            throw new BusinessException("archiveTypeCode must be at most level 3 in the business module tree");
        }
        String resolvedRoot = resolveRootBusinessModuleCode(moduleTypeCode, map);
        if (!rootCode.trim().equals(resolvedRoot)) {
            throw new BusinessException("archiveTypeCode must belong to the selected root business module");
        }
    }

    private LocalDateTime parseDocumentDateTime(String raw) {
        if (!StringUtils.hasText(raw)) {
            return LocalDateTime.now();
        }
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(s.replace(" ", "T"));
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDate.parse(s.length() >= 10 ? s.substring(0, 10) : s, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
                } catch (DateTimeParseException e3) {
                    return LocalDateTime.now();
                }
            }
        }
    }

    private LocalDate parseYearMonthToFirstDay(String ym) {
        return YearMonth.parse(ym.trim()).atDay(1);
    }

    private LocalDate parseYearMonthToLastDay(String ym) {
        return YearMonth.parse(ym.trim()).atEndOfMonth();
    }

    private Long resolveUserIdByLoginName(String dutyPersonName) {
        if (!StringUtils.hasText(dutyPersonName)) {
            return SYSTEM_OPERATOR_ID;
        }
        String name = dutyPersonName.trim();
        if (name.matches("^\\d+$")) {
            try {
                Long id = Long.parseLong(name);
                Integer exists = jdbcTemplate.queryForObject(
                    "select count(*) from tpl_user_t where user_id = ? and delete_flag = 'N'",
                    Integer.class,
                    id
                );
                if (exists != null && exists > 0) {
                    return id;
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        List<Long> ids = jdbcTemplate.queryForList(
            "select user_id from tpl_user_t where delete_flag = 'N' and user_name = ? limit 1",
            Long.class,
            name
        );
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        // 输入了新责任人姓名时自动补齐用户，避免回退到系统用户导致“修改不生效”。
        Number nextId = jdbcTemplate.queryForObject("select coalesce(max(user_id),0) + 1 from tpl_user_t", Number.class);
        long newId = nextId == null ? 1L : nextId.longValue();
        jdbcTemplate.update(
            """
            insert into tpl_user_t (
              user_id, user_name, status, created_by, creation_date, last_updated_by, last_update_date, delete_flag
            ) values (?, ?, 'ACTIVE', ?, current_timestamp, ?, current_timestamp, 'N')
            """,
            newId,
            name,
            SYSTEM_OPERATOR_ID,
            SYSTEM_OPERATOR_ID
        );
        return newId;
    }

    private long parseDeptId(String dutyDepartment) {
        if (!StringUtils.hasText(dutyDepartment)) {
            return 0L;
        }
        try {
            return Long.parseLong(dutyDepartment.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private boolean isDraftMode(String submitMode) {
        return "DRAFT".equalsIgnoreCase(trimToNull(submitMode));
    }

    private boolean isPendingDraftRow(long id) {
        Integer n = jdbcTemplate.queryForObject(
            """
            select count(*) from fdc_pending_document_draft_t
             where draft_id = ? and coalesce(delete_flag, 0) = 0
            """,
            Integer.class,
            id
        );
        return n != null && n > 0;
    }

    private boolean isFormalDocumentRow(long id) {
        Integer n = jdbcTemplate.queryForObject(
            """
            select count(*) from fdc_document_t
             where doc_id = ? and coalesce(delete_flag, 0) = 0
            """,
            Integer.class,
            id
        );
        return n != null && n > 0;
    }

    private long nextPendingDraftId() {
        Long v = jdbcTemplate.queryForObject(
            "select nextval('fdc_pending_document_draft_t_draft_id_seq')",
            Long.class
        );
        return v != null ? v : 1L;
    }

    private int countPendingDraftsForUser(long opId) {
        Integer c = jdbcTemplate.queryForObject(
            """
            select count(*) from fdc_pending_document_draft_t
             where coalesce(delete_flag, 0) = 0 and created_by = ?
            """,
            Integer.class,
            opId
        );
        return c == null ? 0 : c;
    }

    private String serializeDraftPayload(PendingDocumentWriteCommand cmd) {
        try {
            return objectMapper.writeValueAsString(cmd);
        } catch (Exception e) {
            throw new BusinessException("草稿数据序列化失败");
        }
    }

    /**
     * 审计快照不得嵌入完整草稿 JSON：{@code op_content} 在部分库上仍为 VARCHAR，且全文会膨胀 JSON。
     */
    private static Map<String, Object> compactDraftAuditSnapshot(long draftId, String payloadJson) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("draft_id", draftId);
        if (payloadJson != null) {
            m.put("payload_char_count", payloadJson.length());
        }
        return m;
    }

    private PendingDocumentWriteCommand deserializeDraftPayload(String json) {
        try {
            return objectMapper.readValue(json, PendingDocumentWriteCommand.class);
        } catch (Exception e) {
            throw new BusinessException("草稿数据损坏: " + e.getMessage());
        }
    }

    private ArchiveSummaryResponse loadArchiveDetailFromDraftTable(long draftId) {
        try {
            return jdbcTemplate.query(
                """
                select payload_json::text, creation_date, last_update_date, created_by
                  from fdc_pending_document_draft_t
                 where draft_id = ? and coalesce(delete_flag, 0) = 0
                """,
                rs -> {
                    if (!rs.next()) {
                        throw new EmptyResultDataAccessException(1);
                    }
                    PendingDocumentWriteCommand c = deserializeDraftPayload(rs.getString(1));
                    java.sql.Timestamp tsCreated = rs.getTimestamp(2);
                    java.sql.Timestamp tsUpdated = rs.getTimestamp(3);
                    LocalDateTime creationDate = tsCreated != null ? tsCreated.toLocalDateTime() : LocalDateTime.now();
                    LocalDateTime lastUpdate = tsUpdated != null ? tsUpdated.toLocalDateTime() : creationDate;
                    return buildArchiveSummaryFromDraftCommand(
                        draftId,
                        c,
                        creationDate,
                        lastUpdate,
                        rs.getLong(4)
                    );
                },
                draftId
            );
        } catch (EmptyResultDataAccessException e) {
            throw new BusinessException("Archive not found");
        }
    }

    private ArchiveSummaryResponse buildArchiveSummaryFromDraftCommand(
        long draftId,
        PendingDocumentWriteCommand c,
        LocalDateTime creationDate,
        LocalDateTime lastUpdate,
        long createdById
    ) {
        Map<String, BusinessModule> businessModuleMap = listBusinessModuleMap();
        String bizModuleCode = trimToNull(c.getArchiveTypeCode());
        String docTypeL1 = trimToNull(c.getDocumentTypeCode());
        if (!StringUtils.hasText(docTypeL1) && StringUtils.hasText(bizModuleCode)) {
            docTypeL1 = resolveRootBusinessModuleCode(bizModuleCode, businessModuleMap);
        }
        CompanyProject cp = null;
        if (StringUtils.hasText(c.getCompanyProjectCode())) {
            try {
                cp = requireCompanyProject(c.getCompanyProjectCode().trim());
            } catch (Exception ignored) {
                /*草稿允许子公司编码未就绪 */
            }
        }
        LocalDateTime docGen = null;
        if (StringUtils.hasText(c.getDocumentDate())) {
            try {
                docGen = parseDocumentDateTime(c.getDocumentDate().trim());
            } catch (Exception ignored) {
                docGen = null;
            }
        }
        Map<String, String> ext = c.getExtValues() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(c.getExtValues());
        String visibility = ext.getOrDefault("visibility", "是");
        SecurityLevelResolver.Resolved sec = securityLevelResolver.resolve(trimToNull(c.getSecurityLevelCode()));
        String dutyName = StringUtils.hasText(c.getDutyPerson()) ? c.getDutyPerson().trim() : "";
        String createdByLabel = resolveUserNameByIdText(String.valueOf(createdById));
        String beginYm = trimToNull(c.getBeginPeriod());
        String endYm = trimToNull(c.getEndPeriod());
        return ArchiveSummaryResponse.builder()
            .archiveId(draftId)
            .archiveCode(String.valueOf(draftId))
            .documentTypeCode(docTypeL1 != null ? docTypeL1 : "")
            .documentTypeName(StringUtils.hasText(docTypeL1) ? resolveRootBusinessModuleName(docTypeL1, businessModuleMap) : "")
            .companyProjectCode(c.getCompanyProjectCode() != null ? c.getCompanyProjectCode() : "")
            .companyProjectName(cp != null ? cp.getCompanyProjectName() : "")
            .beginPeriod(beginYm != null && beginYm.length() >= 7 ? beginYm.substring(0, 7) : (beginYm != null ? beginYm : ""))
            .endPeriod(endYm != null && endYm.length() >= 7 ? endYm.substring(0, 7) : (endYm != null ? endYm : ""))
            .documentName(c.getDocumentName() != null ? c.getDocumentName() : "")
            .businessCode(normalizePendingBusinessCodeForApi(draftId, trimToNull(c.getBusinessCode()), "DRAFT"))
            .dutyPerson(dutyName)
            .createdBy(createdByLabel)
            .dutyDepartment(c.getDutyDepartment() != null ? c.getDutyDepartment() : "")
            .documentDate(docGen)
            .securityLevelCode(sec.canonicalCode())
            .securityLevelName(sec.displayName())
            .sourceSystem(c.getSourceSystem() != null ? c.getSourceSystem() : "")
            .archiveDestination(c.getArchiveDestination() != null ? c.getArchiveDestination() : "")
            .originPlace(c.getOriginPlace() != null ? c.getOriginPlace() : "")
            .carrierTypeCode(c.getCarrierTypeCode() != null ? c.getCarrierTypeCode() : "")
            .remark(c.getRemark())
            .documentOrganizationCode(c.getDocumentOrganizationCode() != null ? c.getDocumentOrganizationCode() : "")
            .retentionPeriodYears(c.getRetentionPeriodYears() != null ? c.getRetentionPeriodYears() : 10)
            .archiveTypeCode(StringUtils.hasText(bizModuleCode) ? resolveBusinessModuleDisplayName(bizModuleCode, businessModuleMap) : "")
            .businessModuleTypeCode(bizModuleCode != null ? bizModuleCode : "")
            .archiveStatus("草稿")
            .lifecycleStatus("DRAFT")
            .custodyStatus(trimToNull(c.getCustodyStatus()) != null ? c.getCustodyStatus() : "UNARCHIVED")
            .documentVisibility(visibility)
            .parseStatus("")
            .vectorStatus("")
            .lastUpdateDate(lastUpdate != null ? lastUpdate : creationDate)
            .attachmentCount(0)
            .extValues(ext)
            .attachments(List.of())
            .build();
    }

    private static String firstNonBlankField(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a.trim();
        }
        return b == null ? null : b.trim();
    }

    private static Map<String, String> mergeExtPreferIncoming(Map<String, String> base, Map<String, String> inc) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        if (base != null) {
            out.putAll(base);
        }
        if (inc != null) {
            for (Map.Entry<String, String> e : inc.entrySet()) {
                if (e.getValue() != null) {
                    out.put(e.getKey(), e.getValue());
                }
            }
        }
        return out;
    }

    private PendingDocumentWriteCommand mergePendingDraftForSubmit(PendingDocumentWriteCommand base, PendingDocumentWriteCommand in) {
        PendingDocumentWriteCommand m = new PendingDocumentWriteCommand();
        m.setOperatorUserId(resolveOperatorUserId(in));
        m.setDocumentTypeCode(firstNonBlankField(in.getDocumentTypeCode(), base.getDocumentTypeCode()));
        m.setCompanyProjectCode(firstNonBlankField(in.getCompanyProjectCode(), base.getCompanyProjectCode()));
        m.setArchiveTypeCode(firstNonBlankField(in.getArchiveTypeCode(), base.getArchiveTypeCode()));
        m.setBusinessCode(firstNonBlankField(in.getBusinessCode(), base.getBusinessCode()));
        m.setBeginPeriod(firstNonBlankField(in.getBeginPeriod(), base.getBeginPeriod()));
        m.setEndPeriod(firstNonBlankField(in.getEndPeriod(), base.getEndPeriod()));
        m.setArchiveDestination(firstNonBlankField(in.getArchiveDestination(), base.getArchiveDestination()));
        m.setOriginPlace(firstNonBlankField(in.getOriginPlace(), base.getOriginPlace()));
        m.setDocumentName(firstNonBlankField(in.getDocumentName(), base.getDocumentName()));
        m.setDocumentDate(firstNonBlankField(in.getDocumentDate(), base.getDocumentDate()));
        m.setDutyPerson(firstNonBlankField(in.getDutyPerson(), base.getDutyPerson()));
        m.setDutyDepartment(firstNonBlankField(in.getDutyDepartment(), base.getDutyDepartment()));
        m.setCarrierTypeCode(firstNonBlankField(in.getCarrierTypeCode(), base.getCarrierTypeCode()));
        m.setSourceSystem(firstNonBlankField(in.getSourceSystem(), base.getSourceSystem()));
        m.setSecurityLevelCode(firstNonBlankField(in.getSecurityLevelCode(), base.getSecurityLevelCode()));
        m.setRemark(firstNonBlankField(in.getRemark(), base.getRemark()));
        m.setDocumentOrganizationCode(firstNonBlankField(in.getDocumentOrganizationCode(), base.getDocumentOrganizationCode()));
        m.setRetentionPeriodYears(in.getRetentionPeriodYears() != null ? in.getRetentionPeriodYears() : base.getRetentionPeriodYears());
        m.setCustodyStatus(firstNonBlankField(in.getCustodyStatus(), base.getCustodyStatus()));
        m.setSubmitMode("SUBMIT");
        m.setOperationRemark(in.getOperationRemark());
        m.setOperationTypeCode(in.getOperationTypeCode());
        m.setAuditAttachments(in.getAuditAttachments() != null ? in.getAuditAttachments() : base.getAuditAttachments());
        m.setExtValues(mergeExtPreferIncoming(base.getExtValues(), in.getExtValues()));
        return m;
    }

    private ArchiveSummaryResponse updatePendingDraftDocument(Long draftId, PendingDocumentWriteCommand command) {
        long opId = resolveOperatorUserId(command);
        Long owner = jdbcTemplate.query(
            """
            select created_by from fdc_pending_document_draft_t
             where draft_id = ? and coalesce(delete_flag, 0) = 0
            """,
            rs -> rs.next() ? rs.getLong(1) : null,
            draftId
        );
        if (owner == null) {
            throw new BusinessException("Document not found or not editable");
        }
        if (!owner.equals(opId)) {
            throw new BusinessException("Forbidden");
        }
        if (isDraftMode(command.getSubmitMode())) {
            String beforeJson = jdbcTemplate.query(
                "select payload_json::text from fdc_pending_document_draft_t where draft_id = ? and coalesce(delete_flag,0)=0",
                rs -> {
                    if (!rs.next()) {
                        throw new BusinessException("草稿不存在");
                    }
                    return rs.getString(1);
                },
                draftId
            );
            String payload = serializeDraftPayload(command);
            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(
                """
                update fdc_pending_document_draft_t
                   set payload_json = ?::jsonb, last_updated_by = ?, last_update_date = ?
                 where draft_id = ? and coalesce(delete_flag, 0) = 0
                """,
                payload,
                opId,
                now,
                draftId
            );
            Map<String, Object> before = compactDraftAuditSnapshot(draftId, beforeJson);
            Map<String, Object> after = compactDraftAuditSnapshot(draftId, payload);
            after.put("payload_changed", !Objects.equals(beforeJson, payload));
            operationAuditService.record(
                "PENDING_ARCHIVE",
                "应归档",
                "FDC_PENDING_DOCUMENT_DRAFT",
                String.valueOf(draftId),
                resolvePendingOperationType(command.getOperationTypeCode(), "DRAFT_SAVE"),
                "保存应归档草稿",
                before,
                after,
                opId,
                SYSTEM_OPERATOR_NAME,
                trimToNull(command.getOperationRemark()),
                toOperationAuditAttachments(command.getAuditAttachments())
            );
            return loadArchiveDetailFromDraftTable(draftId);
        }
        PendingDocumentWriteCommand merged = mergePendingDraftForSubmit(
            deserializeDraftPayload(
                jdbcTemplate.query(
                    "select payload_json::text from fdc_pending_document_draft_t where draft_id = ? and coalesce(delete_flag,0)=0",
                    rs -> {
                        if (!rs.next()) {
                            throw new BusinessException("草稿不存在");
                        }
                        return rs.getString(1);
                    },
                    draftId
                )
            ),
            command
        );
        merged.setSubmitMode("SUBMIT");
        Long newDocId = insertFormalFdcDocumentTransaction(merged, opId);
        jdbcTemplate.update(
            """
            update fdc_pending_document_draft_t
               set delete_flag = 1, last_updated_by = ?, last_update_date = current_timestamp
             where draft_id = ? and coalesce(delete_flag, 0) = 0
            """,
            opId,
            draftId
        );
        Map<String, Object> afterDraft = new LinkedHashMap<>();
        afterDraft.put("draft_id", draftId);
        afterDraft.put("materialized_doc_id", newDocId);
        operationAuditService.record(
            "PENDING_ARCHIVE",
            "应归档",
            "FDC_PENDING_DOCUMENT_DRAFT",
            String.valueOf(draftId),
            "SUBMIT",
            "草稿提交为应归档正式文档",
            null,
            afterDraft,
            opId,
            SYSTEM_OPERATOR_NAME,
            trimToNull(command.getOperationRemark()),
            toOperationAuditAttachments(command.getAuditAttachments())
        );
        Map<String, Object> afterDoc = snapshotFdcDocumentForAudit(newDocId);
        operationAuditService.record(
            "PENDING_ARCHIVE",
            "应归档",
            "FDC_DOCUMENT",
            String.valueOf(newDocId),
            resolvePendingOperationType(command.getOperationTypeCode(), "CREATE"),
            "草稿提交：创建应归档数据",
            null,
            afterDoc,
            opId,
            SYSTEM_OPERATOR_NAME,
            trimToNull(command.getOperationRemark()),
            toOperationAuditAttachments(command.getAuditAttachments())
        );
        return getArchiveDetail(newDocId);
    }

    /**
     * 写入 fdc_document_t正式应归档行（非草稿表）。
     */
    private Long insertFormalFdcDocumentTransaction(PendingDocumentWriteCommand command, long opId) {
        return pendingDocumentWriteTemplate.execute(status -> {
            String docTypeL1 = requireText(command.getDocumentTypeCode(), "documentTypeCode");
            String companyCode = requireText(command.getCompanyProjectCode(), "companyProjectCode");
            String archiveTypeL3 = requireText(command.getArchiveTypeCode(), "archiveTypeCode");
            String docName = requireText(command.getDocumentName(), "documentName");
            requireText(command.getBusinessCode(), "businessCode");
            requireText(command.getBeginPeriod(), "beginPeriod");
            requireText(command.getDocumentDate(), "documentDate");
            requireBusinessModule(docTypeL1);
            validateBusinessModuleUnderRoot(archiveTypeL3, docTypeL1);
            CompanyProject cp = requireCompanyProject(companyCode);
            String archDestParam = StringUtils.hasText(command.getArchiveDestination()) ? command.getArchiveDestination().trim() : null;
            // resolveDefaults 第2参为归档规则表 module_code（三级业务模块），第3参为自定义匹配条件
            ArchiveDefaultResolveResponse flow = resolveDefaults(companyCode, archiveTypeL3, null, archDestParam);
            String archPlace = StringUtils.hasText(archDestParam)
                ? archDestParam
                : (StringUtils.hasText(flow.getArchiveDestination()) ? flow.getArchiveDestination().trim() : "CN");
            String docOrg = StringUtils.hasText(command.getDocumentOrganizationCode())
                ? command.getDocumentOrganizationCode().trim()
                : flow.getDocumentOrganizationCode();
            if (!StringUtils.hasText(docOrg)) {
                // 与草稿落库一致；无归档流向规则时仍允许创建，避免仅因未配置规则而提交失败
                docOrg = "DEFAULT";
            }
            String securityFlowOrInput = StringUtils.hasText(command.getSecurityLevelCode())
                ? command.getSecurityLevelCode().trim()
                : null;
            if (!StringUtils.hasText(securityFlowOrInput)) {
                securityFlowOrInput = "INTERNAL";
            }
            String security = truncateVarchar(securityLevelResolver.requireCanonicalForWrite(securityFlowOrInput), 30);
            String carrier = normalizeCarrierType(StringUtils.hasText(command.getCarrierTypeCode()) ? command.getCarrierTypeCode() : "ELECTRONIC");
            LocalDateTime docGenDate = parseDocumentDateTime(requireText(command.getDocumentDate(), "documentDate"));
            LocalDate startPeriod = parseYearMonthToFirstDay(requireText(command.getBeginPeriod(), "beginPeriod").trim());
            LocalDate endPeriod = StringUtils.hasText(command.getEndPeriod())
                ? parseYearMonthToLastDay(command.getEndPeriod().trim())
                : startPeriod;
            long newDocId = nextFdcDocumentId();
            String bizNo = resolvePendingDocBizNo(requireText(command.getBusinessCode(), "businessCode").trim(), newDocId);
            Long userId = resolveUserIdByLoginName(command.getDutyPerson());
            long deptId = parseDeptId(command.getDutyDepartment());
            int retention = command.getRetentionPeriodYears() != null && command.getRetentionPeriodYears() > 0
                ? command.getRetentionPeriodYears()
                : (flow.getRetentionPeriodYears() != null && flow.getRetentionPeriodYears() > 0 ? flow.getRetentionPeriodYears() : 10);
            Map<String, String> ext = command.getExtValues() == null ? Map.of() : command.getExtValues();
            String visibility = StringUtils.hasText(ext.get("visibility")) ? ext.get("visibility").trim() : "是";
            String barcode = trimToNull(ext.get("barcodeModule"));
            String origin = StringUtils.hasText(command.getOriginPlace()) ? command.getOriginPlace().trim() : archPlace;
            String custody = StringUtils.hasText(command.getCustodyStatus()) ? command.getCustodyStatus().trim() : "UNARCHIVED";
            String sourceSystem = StringUtils.hasText(command.getSourceSystem()) ? command.getSourceSystem().trim() : "PORTAL";
            String remark = trimToNull(command.getRemark());
            Map<String, String> attrCols = buildAttrColumnsFromExt(ext, archiveTypeL3);
            LocalDateTime now = LocalDateTime.now();
            assertUniqueFormalDocumentNaturalKey(
                null,
                companyCode,
                archiveTypeL3,
                startPeriod,
                truncateVarchar(bizNo, 100),
                null);
            List<Object> insParams = new ArrayList<>();
            insParams.add(newDocId);
            insParams.add(companyCode);
            insParams.add(cp.getCompanyProjectName());
            insParams.add(startPeriod);
            insParams.add(endPeriod);
            insParams.add(archiveTypeL3);
            insParams.add(truncateVarchar(bizNo, 100));
            insParams.add(docGenDate);
            insParams.add(truncateVarchar(archPlace, 60));
            insParams.add(truncateVarchar(origin, 60));
            insParams.add(carrier);
            insParams.add(truncateVarchar(docName, 100));
            insParams.add(truncateVarchar(docOrg, 60));
            insParams.add(deptId);
            insParams.add(userId);
            insParams.add(retention);
            insParams.add(truncateVarchar(security, 30));
            insParams.add("1.0");
            insParams.add(null);
            insParams.add(truncateVarchar(sourceSystem, 30));
            insParams.add("UNARCHIVED");
            insParams.add(truncateVarchar(custody, 30));
            insParams.add(truncateVarchar(remark, 500));
            insParams.add(truncateVarchar(visibility, 100));
            insParams.add(truncateVarchar(barcode, 100));
            insParams.addAll(Arrays.asList(bindAttrExtendedColumnPlaceholders(attrCols)));
            insParams.add(opId);
            insParams.add(now);
            insParams.add(opId);
            insParams.add(now);
            jdbcTemplate.update(SQL_INSERT_FORMAL_FDC_DOCUMENT, insParams.toArray());
            return newDocId;
        });
    }

    private String resolveDefaultCompanyProjectCodeForDraft() {
        List<String> rows = jdbcTemplate.query(
            """
            select company_project_code
              from fdc_company_project_t
             where coalesce(delete_flag,'N') = 'N'
               and coalesce(enable_flag,'Y') = 'Y'
             order by company_project_code
             limit 1
            """,
            (rs, rowNum) -> rs.getString(1)
        );
        if (rows.isEmpty() || !StringUtils.hasText(rows.get(0))) {
            throw new BusinessException("companyProjectCode cannot be blank");
        }
        return rows.get(0).trim();
    }

    private String resolveDefaultDocumentTypeCodeForDraft() {
        List<String> rows = jdbcTemplate.query(
            """
            select type_code
              from fdc_document_type_t
             where coalesce(delete_flag,'N') = 'N'
               and coalesce(enable_flag,'Y') = 'Y'
               and level_num = 1
             order by sort_order, type_code
             limit 1
            """,
            (rs, rowNum) -> rs.getString(1)
        );
        if (rows.isEmpty() || !StringUtils.hasText(rows.get(0))) {
            throw new BusinessException("documentTypeCode cannot be blank");
        }
        return rows.get(0).trim();
    }

    private List<OperationAuditAttachment> toOperationAuditAttachments(List<PendingAuditAttachmentRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return null;
        }
        List<OperationAuditAttachment> list = new ArrayList<>();
        for (PendingAuditAttachmentRef ref : refs) {
            if (ref == null || ref.getFileId() == null || ref.getFileId() <= 0) {
                continue;
            }
            list.add(new OperationAuditAttachment(
                ref.getFileId(),
                ref.getFileName(),
                ref.getStorageKey() != null ? ref.getStorageKey().trim() : null,
                ref.getFileSize()
            ));
        }
        return list.isEmpty() ? null : list;
    }

    private int countIntegratedElectronicAttachments(Long docId) {
        if (docId == null || docId <= 0) {
            return 0;
        }
        Integer count = jdbcTemplate.queryForObject(
            """
            select count(*)
              from fdc_document_attach_t da
              left join fdc_file_t f on f.file_id = da.file_id
             where da.document_id = ?
               and coalesce(da.delete_flag, 'N') = 'N'
               and coalesce(da.enable_flag, 'Y') = 'Y'
               and (
                    f.file_id is null
                    or (
                      coalesce(f.delete_flag, 'N') = 'N'
                      and coalesce(f.enable_flag, 'Y') = 'Y'
                    )
               )
            """,
            Integer.class,
            docId
        );
        return count == null ? 0 : Math.max(count, 0);
    }

    private String buildPendingDocumentUpdateSummary(Map<String, Object> before, Map<String, Object> after) {
        List<String> parts = new ArrayList<>();
        appendFieldChange(parts, "company_code", "公司", before, after);
        appendFieldChange(parts, "biz_module_code", "业务模块", before, after);
        appendFieldChange(parts, "doc_biz_no", "文档业务编码", before, after);
        appendFieldChange(parts, "doc_name", "文档名称", before, after);
        appendFieldChange(parts, "doc_resp_person_id", "归档责任人", before, after);
        appendFieldChange(parts, "doc_resp_dept_id", "文档责任部门", before, after);
        appendFieldChange(parts, "doc_gen_date", "文档生成日期", before, after);
        appendFieldChange(parts, "start_period", "开始档期", before, after);
        appendFieldChange(parts, "end_period", "结束档期", before, after);
        appendFieldChange(parts, "doc_organization_code", "文档组织", before, after);
        appendFieldChange(parts, "security_level", "密级", before, after);
        appendFieldChange(parts, "lifecycle_status", "生命周期状态", before, after);
        appendFieldChange(parts, "custody_status", "保管状态", before, after);
        appendFieldChange(parts, "carrier_type", "载体类型", before, after);
        appendFieldChange(parts, "source_system", "系统来源", before, after);
        appendFieldChange(parts, "arch_place_alpha2_code", "归档地", before, after);
        appendFieldChange(parts, "origin_place_alpha2_code", "产生地", before, after);
        appendFieldChange(parts, "attr1", "是否可见", before, after);
        appendFieldChange(parts, "arch_barcode", "条码模块", before, after);
        appendFieldChange(parts, "attr41", "发票号", before, after);
        appendFieldChange(parts, "attr42", "其他相关编号1", before, after);
        appendFieldChange(parts, "attr43", "其他相关编号2", before, after);
        appendFieldChange(parts, "attr44", "其他相关编号3", before, after);
        appendFieldChange(parts, "attr45", "其他相关编号4", before, after);
        appendFieldChange(parts, "attr46", "其他相关编号5", before, after);
        appendFieldChange(parts, "attr47", "会计", before, after);
        appendFieldChange(parts, "attr48", "扫描员", before, after);
        appendFieldChange(parts, "attr49", "开立日期", before, after);
        appendFieldChange(parts, "attr50", "到期日", before, after);
        appendFieldChange(parts, "attr51", "保函失效日期", before, after);
        appendFieldChange(parts, "attr52", "保函台账状态", before, after);
        appendFieldChange(parts, "attr53", "银行名称", before, after);
        appendFieldChange(parts, "attr54", "币种", before, after);
        appendFieldChange(parts, "attr55", "金额", before, after);
        appendFieldChange(parts, "attr56", "签发机构", before, after);
        appendFieldChange(parts, "attr57", "报废时间", before, after);
        appendFieldChange(parts, "attr58", "业务册号", before, after);
        appendFieldChange(parts, "attr59", "保函电子流编号", before, after);
        appendFieldChange(parts, "attr60", "保函编号", before, after);
        for (int ai = 2; ai <= 100; ai++) {
            if (ai >= 41 && ai <= 60) {
                continue;
            }
            appendFieldChange(parts, "attr" + ai, "扩展字段attr" + ai, before, after);
        }
        appendFieldChange(parts, "description", "描述", before, after);
        if (parts.isEmpty()) {
            return "编辑应归档数据";
        }
        return String.join("；", parts);
    }

    private void appendFieldChange(List<String> out, String fieldKey, String fieldLabel, Map<String, Object> before, Map<String, Object> after) {
        String oldVal = normalizeAuditValue(fieldKey, before == null ? null : before.get(fieldKey));
        String newVal = normalizeAuditValue(fieldKey, after == null ? null : after.get(fieldKey));
        if (Objects.equals(oldVal, newVal)) {
            return;
        }
        out.add("将" + fieldLabel + "字段值由" + oldVal + "修改为" + newVal);
    }

    private String normalizeAuditValue(String fieldKey, Object raw) {
        if (raw == null) {
            return "空";
        }
        String s;
        if (raw instanceof LocalDate d) {
            s = d.toString();
        } else if (raw instanceof java.sql.Date d) {
            s = d.toLocalDate().toString();
        } else {
            s = String.valueOf(raw).trim();
        }
        if ("doc_resp_person_id".equals(fieldKey)) {
            String personName = resolveUserNameByIdText(s);
            if (StringUtils.hasText(personName)) {
                return personName;
            }
        }
        if ("start_period".equals(fieldKey) || "end_period".equals(fieldKey)) {
            String ym = normalizeYearMonthValue(s);
            if (ym != null) {
                return ym;
            }
        }
        return s.isEmpty() ? "空" : s;
    }

    private String normalizeYearMonthValue(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String v = text.trim();
        try {
            if (v.matches("^\\d{4}-\\d{2}$")) {
                return YearMonth.parse(v).toString();
            }
            if (v.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                return YearMonth.from(LocalDate.parse(v)).toString();
            }
        } catch (Exception ignored) {
            return v;
        }
        return v;
    }

    private String resolvePendingOperationType(String requested, String fallback) {
        if (!StringUtils.hasText(requested)) {
            return fallback;
        }
        String code = requested.trim().toUpperCase(Locale.ROOT);
        return switch (code) {
            case "BATCH_CREATE", "BATCH_UPDATE", "CREATE", "UPDATE", "DRAFT_SAVE", "ATTACH_INTEGRATE" -> code;
            default -> fallback;
        };
    }

    private String resolveUserNameByIdText(String idText) {
        if (!StringUtils.hasText(idText) || !idText.trim().matches("^\\d+$")) {
            return idText;
        }
        try {
            Long uid = Long.parseLong(idText.trim());
            List<String> names = jdbcTemplate.queryForList(
                "select user_name from tpl_user_t where user_id = ? and delete_flag = 'N' limit 1",
                String.class,
                uid
            );
            return names.isEmpty() ? idText : names.get(0);
        } catch (Exception ex) {
            return idText;
        }
    }

    private Map<String, String> buildAttrColumnsFromExt(Map<String, String> ext, String busiModuleCode) {
        Map<String, String> row = new LinkedHashMap<>();
        for (String col : FDC_DOC_ATTR_EXTENDED_COLUMNS) {
            row.put(col, null);
        }
        for (Map.Entry<String, String> entry : HARD_CODED_EXT_FIELD_ATTR_MAP.entrySet()) {
            String v = ext == null ? null : ext.get(entry.getKey());
            row.put(entry.getValue(), StringUtils.hasText(v) ? truncateVarchar(v.trim(), 100) : null);
        }
        String refNo = ext == null ? null : ext.get("refNo");
        if (StringUtils.hasText(refNo)) {
            String[] parts = refNo.split(";");
            for (int i = 0; i < REF_NO_ATTR_COLUMNS.size(); i++) {
                String col = REF_NO_ATTR_COLUMNS.get(i);
                row.put(col, i < parts.length && StringUtils.hasText(parts[i].trim()) ? truncateVarchar(parts[i].trim(), 100) : null);
            }
        }
        mergeExtValuesIntoAttrColumnsFromBusinessModule(row, ext, busiModuleCode);
        return row;
    }

    private Object[] bindAttrExtendedColumnPlaceholders(Map<String, String> attrCols) {
        Object[] args = new Object[FDC_DOC_ATTR_EXTENDED_COLUMNS.size()];
        int i = 0;
        for (String col : FDC_DOC_ATTR_EXTENDED_COLUMNS) {
            String v = attrCols == null ? null : attrCols.get(col);
            args[i++] = coerceFdcAttrValueByColumnType(col, v);
        }
        return args;
    }

    private Object coerceFdcAttrValueByColumnType(String columnName, String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalizedType = loadFdcDocumentColumnTypeMap().getOrDefault(columnName, "");
        String trimmed = raw.trim();
        if (!"numeric".equalsIgnoreCase(normalizedType)) {
            return truncateVarchar(trimmed, 500);
        }
        // 兼容历史库 attr41-attr60 为 numeric 的情况，无法转数值时置空避免写入失败。
        String normalized = trimmed.replace(",", "");
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Map<String, String> loadFdcDocumentColumnTypeMap() {
        Map<String, String> snapshot = fdcDocumentColumnTypeCache;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (this) {
            if (fdcDocumentColumnTypeCache != null) {
                return fdcDocumentColumnTypeCache;
            }
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                """
                select lower(column_name) as column_name, lower(data_type) as data_type
                  from information_schema.columns
                 where table_schema = 'public'
                   and table_name = 'fdc_document_t'
                """
            );
            LinkedHashMap<String, String> map = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                String name = row.get("column_name") == null ? null : String.valueOf(row.get("column_name")).trim();
                String type = row.get("data_type") == null ? null : String.valueOf(row.get("data_type")).trim();
                if (StringUtils.hasText(name) && StringUtils.hasText(type)) {
                    map.put(name, type);
                }
            }
            fdcDocumentColumnTypeCache = Collections.unmodifiableMap(map);
            return fdcDocumentColumnTypeCache;
        }
    }

    /**
     * 按 {@code fdc_business_module_ext_field_t.ext_attribute} 把扩展表单值写入 {@code fdc_document_t.attr2–attr100}（attr1 为是否可见，不由本方法写入）。
     */
    private void mergeExtValuesIntoAttrColumnsFromBusinessModule(Map<String, String> row, Map<String, String> ext, String busiModuleCode) {
        if (!StringUtils.hasText(busiModuleCode) || ext == null || ext.isEmpty()) {
            return;
        }
        List<BusinessModuleExtField> fields = businessModuleExtFieldMapper.selectList(new LambdaQueryWrapper<BusinessModuleExtField>()
            .eq(BusinessModuleExtField::getModuleCode, busiModuleCode.trim())
            .eq(BusinessModuleExtField::getDeleteFlag, "N")
            .eq(BusinessModuleExtField::getEnabledFlag, "Y")
            .eq(BusinessModuleExtField::getFieldScope, "BASIC"));
        for (BusinessModuleExtField f : fields) {
            if (f == null || !StringUtils.hasText(f.getExtAttribute())) {
                continue;
            }
            String col = f.getExtAttribute().trim().toLowerCase(Locale.ROOT);
            if (!FDC_DOC_EXT_ATTRIBUTE_WHITELIST.contains(col)) {
                continue;
            }
            String raw = resolveExtRawForBusinessModuleField(ext, f);
            row.put(col, StringUtils.hasText(raw) ? truncateVarchar(raw.trim(), 500) : null);
        }
    }

    private List<BusinessModuleExtField> loadBasicExtFieldsForModule(String moduleCode) {
        if (!StringUtils.hasText(moduleCode)) {
            return List.of();
        }
        return businessModuleExtFieldMapper.selectList(new LambdaQueryWrapper<BusinessModuleExtField>()
            .eq(BusinessModuleExtField::getModuleCode, moduleCode.trim())
            .eq(BusinessModuleExtField::getDeleteFlag, "N")
            .eq(BusinessModuleExtField::getEnabledFlag, "Y")
            .eq(BusinessModuleExtField::getFieldScope, "BASIC"));
    }

    /**
     * 从 {@code fdc_document_t} 读出 {@code attr2–attr100}，按扩展元数据填入 {@code englishFieldName} / {@code fieldCode}。
     */
    private void mergeBusinessModuleDocumentAttrsIntoExtValues(
        Map<String, String> extValues,
        java.sql.ResultSet rs,
        String bizModuleCode,
        Map<String, List<BusinessModuleExtField>> moduleFieldCache
    ) throws java.sql.SQLException {
        if (!StringUtils.hasText(bizModuleCode)) {
            return;
        }
        List<BusinessModuleExtField> fields = moduleFieldCache != null
            ? moduleFieldCache.computeIfAbsent(bizModuleCode.trim(), this::loadBasicExtFieldsForModule)
            : loadBasicExtFieldsForModule(bizModuleCode);
        for (BusinessModuleExtField f : fields) {
            if (f == null || !StringUtils.hasText(f.getExtAttribute())) {
                continue;
            }
            String col = f.getExtAttribute().trim().toLowerCase(Locale.ROOT);
            if (!FDC_DOC_EXT_ATTRIBUTE_WHITELIST.contains(col)) {
                continue;
            }
            String cell = trimToNull(rs.getString(col));
            if (!StringUtils.hasText(cell)) {
                continue;
            }
            if (StringUtils.hasText(f.getEnglishFieldName())) {
                extValues.put(f.getEnglishFieldName().trim(), cell);
            }
            if (StringUtils.hasText(f.getFieldCode())) {
                extValues.put(f.getFieldCode().trim(), cell);
            }
        }
    }

    private static String resolveExtRawForBusinessModuleField(Map<String, String> ext, BusinessModuleExtField f) {
        String raw = null;
        if (StringUtils.hasText(f.getEnglishFieldName())) {
            raw = ext.get(f.getEnglishFieldName().trim());
        }
        if (!StringUtils.hasText(raw) && StringUtils.hasText(f.getFieldCode())) {
            raw = ext.get(f.getFieldCode().trim());
        }
        return raw;
    }

    private String truncateVarchar(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    private ArchiveSummaryResponse loadArchiveDetailFromDocumentTable(Long archiveId) {
        Map<String, BusinessModule> businessModuleMap = listBusinessModuleMap();
        Map<String, String> carrierTypeNameMap = listCarrierTypeNameMap();
        String detailSql = new StringBuilder("""
            select doc_id, doc_biz_no, fdc_document_t.company_code, company_name, biz_module_code, start_period, end_period,
                   arch_place_alpha2_code, origin_place_alpha2_code, doc_organization_code, lifecycle_status,
                   doc_name, doc_gen_date, doc_resp_person_id, coalesce(u.user_name, cast(fdc_document_t.doc_resp_person_id as varchar)) as duty_person_name,
                   fdc_document_t.created_by, coalesce(created_u.user_name, cast(fdc_document_t.created_by as varchar)) as created_by_name,
                   doc_resp_dept_id, carrier_type,
                   source_system, security_level, description, fdc_document_t.creation_date as creation_date,
                   attr1, arch_barcode,
              """).append(SQL_SELECT_FDC_DOC_ATTR2_TO_100).append("""
                   ,
                   cp.company_tag, cp.country_code, geo.rep_office_name, geo.region_name
              from fdc_document_t
              left join tpl_user_t u on u.user_id = fdc_document_t.doc_resp_person_id
              left join tpl_user_t created_u on created_u.user_id = fdc_document_t.created_by
              left join fdc_company_project_t cp on cp.company_project_code = fdc_document_t.company_code and cp.delete_flag = 'N'
              left join (
                    select country_code,
                           min(rep_office_name) as rep_office_name,
                           min(region_name) as region_name
                      from fdc_geo_region_t
                     where delete_flag = 'N'
                     group by country_code
              ) geo on geo.country_code = cp.country_code
            where coalesce(fdc_document_t.delete_flag, 0) = 0
              and doc_id = ?
            limit 1
            """).toString();
        List<ArchiveSummaryResponse> rows = jdbcTemplate.query(
            detailSql,
            (rs, rowNum) -> {
                LocalDate startPeriod = rs.getObject("start_period", LocalDate.class);
                LocalDate endPeriod = rs.getObject("end_period", LocalDate.class);
                LocalDateTime docGenDate = rs.getObject("doc_gen_date", LocalDateTime.class);
                LocalDateTime creationDate = rs.getObject("creation_date", LocalDateTime.class);
                String lifecycleStatus = rs.getString("lifecycle_status");
                String businessModuleCode = rs.getString("biz_module_code");
                Map<String, String> extVals = extractHardCodedExtValues(rs, businessModuleCode, null);
                String visCol = trimToNull(rs.getString("attr1"));
                String barcodeCol = trimToNull(rs.getString("arch_barcode"));
                extVals.put("visibility", StringUtils.hasText(visCol) ? visCol : "是");
                if (barcodeCol != null) {
                    extVals.put("barcodeModule", barcodeCol);
                }
                SecurityLevelResolver.Resolved secLv = securityLevelResolver.resolve(rs.getString("security_level"));
                return ArchiveSummaryResponse.builder()
                    .archiveId(rs.getLong("doc_id"))
                    .archiveCode(String.valueOf(rs.getLong("doc_id")))
                    .documentTypeCode(resolveRootBusinessModuleCode(businessModuleCode, businessModuleMap))
                    .documentTypeName(resolveRootBusinessModuleName(businessModuleCode, businessModuleMap))
                    .companyProjectCode(rs.getString("company_code"))
                    .companyProjectName(rs.getString("company_name"))
                    .beginPeriod(formatYearMonth(startPeriod))
                    .endPeriod(formatYearMonth(endPeriod))
                    .documentName(rs.getString("doc_name"))
                    .businessCode(normalizePendingBusinessCodeForApi(rs.getLong("doc_id"), rs.getString("doc_biz_no"), lifecycleStatus))
                    .dutyPerson(rs.getString("duty_person_name"))
                    .createdBy(rs.getString("created_by_name"))
                    .dutyDepartment(String.valueOf(rs.getObject("doc_resp_dept_id")))
                    .documentDate(docGenDate)
                    .securityLevelCode(secLv.canonicalCode())
                    .securityLevelName(secLv.displayName())
                    .sourceSystem(rs.getString("source_system"))
                    .archiveDestination(rs.getString("arch_place_alpha2_code"))
                    .originPlace(rs.getString("origin_place_alpha2_code"))
                    .carrierTypeCode(carrierTypeNameMap.getOrDefault(rs.getString("carrier_type"), rs.getString("carrier_type")))
                    .remark(rs.getString("description"))
                    .documentOrganizationCode(rs.getString("doc_organization_code"))
                    .archiveTypeCode(resolveBusinessModuleDisplayName(businessModuleCode, businessModuleMap))
                    .businessModuleTypeCode(businessModuleCode)
                    .documentVisibility(StringUtils.hasText(visCol) ? visCol : "是")
                    .lifecycleStatus(lifecycleStatus)
                    .archiveStatus("ARCHIVED".equalsIgnoreCase(lifecycleStatus) ? "已归档" : ("DRAFT".equalsIgnoreCase(lifecycleStatus) ? "草稿" : "未归档"))
                    .custodyStatus("ARCHIVED".equalsIgnoreCase(lifecycleStatus) ? "已归档" : ("DRAFT".equalsIgnoreCase(lifecycleStatus) ? "草稿" : "未归档"))
                    .lastUpdateDate(creationDate)
                    .attachmentCount(0)
                    .extValues(extVals)
                    .attachments(List.of())
                    .build();
            },
            archiveId
        );
        if (rows.isEmpty()) {
            throw new BusinessException("Archive not found");
        }
        ArchiveSummaryResponse detail = rows.get(0);
        List<ArchiveAttachmentResponse> attachments = listDocumentAttachmentResponses(archiveId);
        detail.setAttachments(attachments);
        detail.setAttachmentCount(attachments.size());
        return detail;
    }

    private List<ArchiveAttachmentResponse> listDocumentAttachmentResponses(Long docId) {
        if (docId == null || docId <= 0) {
            return List.of();
        }
        return jdbcTemplate.query(
            """
            select da.document_attach_id,
                   da.att_type,
                   da.attach_category,
                   da.creation_date,
                   f.file_name,
                   f.file_type,
                   f.file_size
              from fdc_document_attach_t da
              left join fdc_file_t f on f.file_id = da.file_id
             where da.document_id = ?
               and coalesce(da.delete_flag, 'N') = 'N'
               and coalesce(da.enable_flag, 'Y') = 'Y'
               and (
                    f.file_id is null
                    or (
                      coalesce(f.delete_flag, 'N') = 'N'
                      and coalesce(f.enable_flag, 'Y') = 'Y'
                    )
               )
             order by da.document_attach_id
            """,
            (rs, rowNum) -> ArchiveAttachmentResponse.builder()
                .attachmentId(rs.getLong("document_attach_id"))
                .attachmentRole(resolveAttachmentRoleForDetail(rs.getString("attach_category")))
                .attachmentTypeCode(trimToNull(rs.getString("att_type")))
                .attachmentSeq(rowNum + 1)
                .versionNo(1)
                .fileName(rs.getString("file_name"))
                .mimeType(trimToNull(rs.getString("file_type")))
                .fileSize(rs.getObject("file_size") == null ? null : rs.getLong("file_size"))
                .remark(null)
                .aiSummary(null)
                .parseStatus("SUCCESS")
                .vectorStatus("READY")
                .creationDate(rs.getObject("creation_date", LocalDateTime.class))
                .build(),
            docId
        );
    }

    private String resolveAttachmentRoleForDetail(String category) {
        String c = trimToNull(category);
        if (!StringUtils.hasText(c)) {
            return "ELECTRONIC";
        }
        String up = c.trim().toUpperCase(Locale.ROOT);
        if ("PAPER_SCAN".equals(up)) {
            return "PAPER_SCAN";
        }
        return "ELECTRONIC";
    }

    @Override
    @Transactional
    public ArchiveTransferResponse transferArchives(ArchiveTransferCommand command) {
        if (command.getArchiveIds() == null || command.getArchiveIds().isEmpty()) {
            throw new BusinessException("Please select archives to transfer");
        }

        List<ArchiveRecord> archives = archiveRecordMapper.selectList(new LambdaQueryWrapper<ArchiveRecord>()
            .in(ArchiveRecord::getArchiveId, command.getArchiveIds())
            .eq(ArchiveRecord::getDeleteFlag, "N")
            .orderByAsc(ArchiveRecord::getArchiveId));
        if (archives.size() != command.getArchiveIds().size()) {
            throw new BusinessException("Some selected archives do not exist");
        }

        Map<Long, Map<String, String>> extValueMap = loadExtValueMap(command.getArchiveIds());
        Map<String, String> typeNameMap = listEnabledBusinessModules().stream()
            .collect(Collectors.toMap(LabelValueOption::getCode, LabelValueOption::getName, (left, right) -> left, LinkedHashMap::new));

        String initiatorId = StringUtils.hasText(command.getInitiatorId()) ? command.getInitiatorId().trim() : String.valueOf(SYSTEM_OPERATOR_ID);
        String initiatorName = StringUtils.hasText(command.getInitiatorName()) ? command.getInitiatorName().trim() : "张三";
        String assigneeId = command.getAssigneeId().trim();
        String assigneeName = StringUtils.hasText(command.getAssigneeName()) ? command.getAssigneeName().trim() : assigneeId;
        LocalDateTime now = LocalDateTime.now();
        String businessKey = generateCode("TRF");

        List<Map<String, Object>> transferDocuments = archives.stream()
            .map(archive -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("archiveId", archive.getArchiveId());
                item.put("archiveCode", archive.getArchiveCode());
                item.put("documentName", archive.getDocumentName());
                item.put("documentTypeCode", archive.getDocumentTypeCode());
                item.put("documentTypeName", typeNameMap.getOrDefault(archive.getDocumentTypeCode(), archive.getDocumentTypeCode()));
                item.put("businessCode", archive.getBusinessCode());
                item.put("documentOrganizationCode", archive.getDocumentOrganizationCode());
                item.put("extFields", extValueMap.getOrDefault(archive.getArchiveId(), Map.of()));
                return item;
            })
            .toList();

        Map<String, Object> transferForm = new LinkedHashMap<>();
        transferForm.put("assigneeId", assigneeId);
        transferForm.put("assigneeName", assigneeName);
        transferForm.put("transferMethod", trimToNull(command.getTransferMethod()));
        transferForm.put("logisticsCompany", trimToNull(command.getLogisticsCompany()));
        transferForm.put("trackingNumber", trimToNull(command.getTrackingNumber()));
        transferForm.put("remark", trimToNull(command.getRemark()));

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("assigneeId", assigneeId);
        variables.put("assigneeName", assigneeName);
        variables.put("initiatorId", initiatorId);
        variables.put("initiatorName", initiatorName);
        variables.put("transferMode", "ARCHIVE_QUERY_BATCH");
        variables.put("transferForm", transferForm);
        variables.put("transferDocuments", transferDocuments);

        StartProcessCommand startProcessCommand = new StartProcessCommand();
        startProcessCommand.setProcessDefinitionKey("documentTransfer");
        startProcessCommand.setBusinessKey(businessKey);
        startProcessCommand.setBusinessType("TRANSFER");
        startProcessCommand.setInitiatorId(initiatorId);
        startProcessCommand.setInitiatorName(initiatorName);
        startProcessCommand.setVariables(variables);
        WorkflowInstance workflowInstance = workflowService.startProcess(startProcessCommand);

        for (ArchiveRecord archive : archives) {
            archive.setArchiveStatus("TRANSFERRED");
            archive.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            archive.setLastUpdateDate(now);
            archiveRecordMapper.updateById(archive);

            ArchiveReceipt receipt = new ArchiveReceipt();
            receipt.setReceiptCode("REC-" + archive.getArchiveCode());
            receipt.setSourceDept(trimToNull(archive.getDutyDepartment()));
            receipt.setArchiveTitle(archive.getDocumentName());
            receipt.setArchiveType(typeNameMap.getOrDefault(archive.getDocumentTypeCode(), archive.getDocumentTypeCode()));
            receipt.setSecurityLevel(archive.getSecurityLevelCode());
            receipt.setReceiveStatus("PENDING_REVIEW");
            receipt.setWorkflowInstanceCode(workflowInstance.getProcessInstanceId());
            receipt.setSubmittedBy(initiatorName);
            receipt.setSubmittedAt(now);
            archiveReceiptMapper.insert(receipt);
        }

        operationAuditService.record(
            "ARCHIVE_TRANSFER",
            "档案移交",
            "WORKFLOW",
            businessKey,
            "CREATE",
            "批量移交档案并发起电子流",
            null,
            Map.of("archiveCount", archives.size(), "processInstanceId", workflowInstance.getProcessInstanceId()),
            SYSTEM_OPERATOR_ID,
            SYSTEM_OPERATOR_NAME
        );

        return ArchiveTransferResponse.builder()
            .businessKey(businessKey)
            .processInstanceId(workflowInstance.getProcessInstanceId())
            .workflowInstanceId(workflowInstance.getId())
            .archiveCount(archives.size())
            .build();
    }

    @Override
    public BindOptionsResponse loadBindOptions() {
        return BindOptionsResponse.builder()
            .bindModes(List.of(
                option("BUSINESS_CODE", "按文档类型+业务编号"),
                option("PERIOD", "按文档类型+期间"),
                option("MANUAL", "人工勾选")
            ))
            .candidates(listBindableArchiveCandidates(null, null, null))
            .build();
    }

    @Override
    public BindPreviewResponse previewBind(BindPreviewCommand command) {
        String bindMode = normalizeBindMode(command.getBindMode());
        List<ArchiveRecord> archives = loadBindCandidateArchives(command.getArchiveIds(), command.getKeyword(), command.getDocumentTypeCode(), command.getCompanyProjectCode());
        List<BindVolumeResponse> groups = buildPreviewGroups(bindMode, archives);
        return BindPreviewResponse.builder()
            .bindMode(bindMode)
            .groupCount(groups.size())
            .archiveCount(archives.size())
            .groups(groups)
            .build();
    }

    @Override
    @Transactional
    public BindBatchResponse createBindBatch(BindCreateCommand command) {
        String bindMode = normalizeBindMode(command.getBindMode());
        BindBatch bindBatch = new BindBatch();
        bindBatch.setBindBatchCode(generateCode("BND"));
        bindBatch.setBindMode(bindMode);
        bindBatch.setSourceType("ARCHIVE");
        bindBatch.setBindStatus("BOUND");
        bindBatch.setBindRemark(trimToNull(command.getBindRemark()));
        bindBatch.setGuidedStorageFlag("N");
        bindBatch.setVolumeCount(command.getVolumes().size());
        bindBatch.setArchiveCount(command.getVolumes().stream().mapToInt(item -> item.getItems().size()).sum());
        bindBatch.setDeleteFlag("N");
        bindBatch.setCreatedBy(SYSTEM_OPERATOR_ID);
        bindBatch.setCreationDate(LocalDateTime.now());
        bindBatch.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        bindBatch.setLastUpdateDate(LocalDateTime.now());
        bindBatchMapper.insert(bindBatch);

        List<BindVolumeResponse> volumeResponses = new ArrayList<>();
        for (int index = 0; index < command.getVolumes().size(); index++) {
            BindCreateCommand.BindCreateVolumeCommand volumeCommand = command.getVolumes().get(index);
            List<Long> archiveIds = volumeCommand.getItems().stream().map(BindCreateCommand.BindCreateVolumeItemCommand::getArchiveId).toList();
            List<ArchiveRecord> archives = archiveIds.isEmpty() ? List.of() : archiveRecordMapper.selectList(new LambdaQueryWrapper<ArchiveRecord>()
                .in(ArchiveRecord::getArchiveId, archiveIds)
                .eq(ArchiveRecord::getDeleteFlag, "N"));
            Map<Long, ArchiveRecord> archiveMap = archives.stream().collect(Collectors.toMap(ArchiveRecord::getArchiveId, Function.identity()));
            ensureArchivesCanBind(archiveIds, archiveMap);

            BindVolume volume = new BindVolume();
            volume.setBindBatchId(bindBatch.getBindBatchId());
            volume.setBindVolumeCode(generateCode("VOL"));
            volume.setVolumeTitle(resolveVolumeTitle(bindMode, volumeCommand, archives, index + 1));
            volume.setBindRuleKey(trimToNull(volumeCommand.getBindRuleKey()));
            volume.setCarrierTypeCode(resolveVolumeCarrierType(volumeCommand, archives));
            volume.setArchiveCount(archiveIds.size());
            volume.setTotalPageCount(resolveVolumePageCount(archiveIds));
            volume.setTotalCopyCount(resolveVolumeCopyCount(archiveIds));
            volume.setBindStatus("BOUND");
            volume.setRemark(trimToNull(volumeCommand.getRemark()));
            volume.setDeleteFlag("N");
            volume.setCreatedBy(SYSTEM_OPERATOR_ID);
            volume.setCreationDate(LocalDateTime.now());
            volume.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            volume.setLastUpdateDate(LocalDateTime.now());
            bindVolumeMapper.insert(volume);

            List<BindVolumeItemResponse> itemResponses = new ArrayList<>();
            for (int itemIndex = 0; itemIndex < volumeCommand.getItems().size(); itemIndex++) {
                BindCreateCommand.BindCreateVolumeItemCommand itemCommand = volumeCommand.getItems().get(itemIndex);
                ArchiveRecord archive = archiveMap.get(itemCommand.getArchiveId());
                if (archive == null) {
                    throw new BusinessException("Archive not found: " + itemCommand.getArchiveId());
                }
                BindVolumeItem item = new BindVolumeItem();
                item.setVolumeId(volume.getVolumeId());
                item.setArchiveId(archive.getArchiveId());
                item.setSortNo(itemCommand.getSortNo() == null ? itemIndex + 1 : itemCommand.getSortNo());
                item.setPrimaryFlag("Y".equalsIgnoreCase(itemCommand.getPrimaryFlag()) ? "Y" : "N");
                item.setBindReason(trimToNull(itemCommand.getBindReason()));
                item.setCreatedBy(SYSTEM_OPERATOR_ID);
                item.setCreationDate(LocalDateTime.now());
                item.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
                item.setLastUpdateDate(LocalDateTime.now());
                bindVolumeItemMapper.insert(item);

                archive.setArchiveStatus("BOUND");
                archive.setBindVolumeCode(volume.getBindVolumeCode());
                archive.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
                archive.setLastUpdateDate(LocalDateTime.now());
                archiveRecordMapper.updateById(archive);

                itemResponses.add(toBindVolumeItemResponse(item, archive));
            }
            volumeResponses.add(toBindVolumeResponse(volume, itemResponses));
        }
        operationAuditService.record("ARCHIVE_BIND", "档案成册", "BIND_BATCH", bindBatch.getBindBatchCode(), "CREATE", "创建成册批次", null, Map.of("archiveCount", bindBatch.getArchiveCount()), SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return buildBindBatchResponse(bindBatch, volumeResponses);
    }

    @Override
    public BindBatchResponse getBindBatch(String bindBatchCode) {
        BindBatch batch = requireBindBatch(bindBatchCode);
        return buildBindBatchResponse(batch, loadVolumesByBatch(batch.getBindBatchId()));
    }

    @Override
    public List<BindBatchResponse> queryBindBatches(BindQueryCommand command) {
        List<BindBatch> batches = bindBatchMapper.selectList(new LambdaQueryWrapper<BindBatch>()
            .eq(BindBatch::getDeleteFlag, "N")
            .eq(StringUtils.hasText(command.getBindMode()), BindBatch::getBindMode, trimToNull(command.getBindMode()))
            .eq(StringUtils.hasText(command.getBindStatus()), BindBatch::getBindStatus, trimToNull(command.getBindStatus()))
            .like(StringUtils.hasText(command.getKeyword()), BindBatch::getBindBatchCode, trimToNull(command.getKeyword()))
            .orderByDesc(BindBatch::getCreationDate)
            .last("limit 50"));
        return batches.stream().map(item -> buildBindBatchResponse(item, loadVolumesByBatch(item.getBindBatchId()))).toList();
    }

    @Override
    public StorageOptionsResponse loadStorageOptions() {
        List<LabelValueOption> warehouseOptions = warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getStatus, "ACTIVE")
                .orderByAsc(Warehouse::getWarehouseCode))
            .stream()
            .map(item -> option(item.getWarehouseCode(), item.getWarehouseName()))
            .toList();
        List<LabelValueOption> locationOptions = warehouseLocationMapper.selectList(new LambdaQueryWrapper<WarehouseLocation>()
                .eq(WarehouseLocation::getDeleteFlag, "N")
                .orderByAsc(WarehouseLocation::getWarehouseCode)
                .orderByAsc(WarehouseLocation::getLocationCode))
            .stream()
            .map(item -> option(item.getLocationCode(), item.getLocationName()))
            .toList();
        return StorageOptionsResponse.builder()
            .sourceTypes(List.of(option("BIND_GUIDED", "成册引导"), option("DIRECT", "独立入库")))
            .warehouses(warehouseOptions)
            .locations(locationOptions)
            .build();
    }

    @Override
    public StorageQueryResponse queryStorage(StorageQueryCommand command) {
        List<BindVolumeResponse> volumes = loadStorageCandidateVolumes(command.getSourceBindBatchCode(), command.getKeyword());
        List<BindArchiveCandidateResponse> archives = loadStandaloneStorageArchives(command.getKeyword());
        return StorageQueryResponse.builder().volumes(volumes).archives(archives).build();
    }

    @Override
    @Transactional
    public StorageBatchResponse createStorageBatch(StorageCreateCommand command) {
        String sourceType = normalizeStorageSourceType(command.getSourceType());
        Warehouse warehouse = requireWarehouse(command.getWarehouseCode());
        StorageBatch batch = new StorageBatch();
        batch.setStorageBatchCode(generateCode("STO"));
        batch.setSourceType(sourceType);
        batch.setSourceBindBatchCode(trimToNull(command.getSourceBindBatchCode()));
        batch.setWarehouseCode(warehouse.getWarehouseCode());
        batch.setOperatorId(SYSTEM_OPERATOR_ID);
        batch.setOperatorName(SYSTEM_OPERATOR_NAME);
        batch.setStorageStatus("IN_PROGRESS");
        batch.setRemark(trimToNull(command.getRemark()));
        batch.setCreatedAt(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());
        storageBatchMapper.insert(batch);

        List<StorageBatchItemResponse> itemResponses = new ArrayList<>();
        boolean hasFailure = false;
        for (StorageCreateCommand.StorageCreateItemCommand itemCommand : command.getItems()) {
            StorageExecutionResult result = executeStorageItem(batch, warehouse, itemCommand);
            itemResponses.add(result.response());
            hasFailure = hasFailure || "FAILED".equals(result.response().getResultStatus());
        }
        batch.setStorageStatus(hasFailure ? "FAILED" : "COMPLETED");
        batch.setUpdatedAt(LocalDateTime.now());
        storageBatchMapper.updateById(batch);

        if (StringUtils.hasText(batch.getSourceBindBatchCode())) {
            BindBatch bindBatch = requireBindBatch(batch.getSourceBindBatchCode());
            bindBatch.setGuidedStorageFlag("Y");
            if (!hasUnstoredVolumes(bindBatch.getBindBatchId())) {
                bindBatch.setBindStatus("STORED");
            }
            bindBatch.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            bindBatch.setLastUpdateDate(LocalDateTime.now());
            bindBatchMapper.updateById(bindBatch);
        }
        operationAuditService.record("ARCHIVE_STORAGE", "档案入库", "STORAGE_BATCH", batch.getStorageBatchCode(), "CREATE", "创建入库批次", null, Map.of("warehouseCode", batch.getWarehouseCode(), "status", batch.getStorageStatus()), SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return StorageBatchResponse.builder()
            .storageBatchId(batch.getStorageBatchId())
            .storageBatchCode(batch.getStorageBatchCode())
            .sourceType(batch.getSourceType())
            .sourceBindBatchCode(batch.getSourceBindBatchCode())
            .warehouseCode(batch.getWarehouseCode())
            .operatorName(batch.getOperatorName())
            .storageStatus(batch.getStorageStatus())
            .remark(batch.getRemark())
            .createdAt(batch.getCreatedAt())
            .items(itemResponses)
            .build();
    }

    @Override
    public StorageBatchResponse getStorageBatch(String storageBatchCode) {
        StorageBatch batch = requireStorageBatch(storageBatchCode);
        List<StorageBatchItemResponse> items = storageBatchItemMapper.selectList(new LambdaQueryWrapper<StorageBatchItem>()
                .eq(StorageBatchItem::getStorageBatchId, batch.getStorageBatchId())
                .orderByAsc(StorageBatchItem::getStorageItemId))
            .stream()
            .map(this::toStorageBatchItemResponse)
            .toList();
        return StorageBatchResponse.builder()
            .storageBatchId(batch.getStorageBatchId())
            .storageBatchCode(batch.getStorageBatchCode())
            .sourceType(batch.getSourceType())
            .sourceBindBatchCode(batch.getSourceBindBatchCode())
            .warehouseCode(batch.getWarehouseCode())
            .operatorName(batch.getOperatorName())
            .storageStatus(batch.getStorageStatus())
            .remark(batch.getRemark())
            .createdAt(batch.getCreatedAt())
            .items(items)
            .build();
    }

    @Override
    public List<StorageLedgerResponse> queryStorageLedger(StorageLedgerQueryCommand command) {
        return storageLedgerMapper.selectList(new LambdaQueryWrapper<StorageLedger>()
                .eq(StringUtils.hasText(command.getStorageBatchCode()), StorageLedger::getStorageBatchCode, trimToNull(command.getStorageBatchCode()))
                .eq(StringUtils.hasText(command.getBindVolumeCode()), StorageLedger::getBindVolumeCode, trimToNull(command.getBindVolumeCode()))
                .eq(StringUtils.hasText(command.getArchiveCode()), StorageLedger::getArchiveCode, trimToNull(command.getArchiveCode()))
                .eq(StringUtils.hasText(command.getWarehouseCode()), StorageLedger::getWarehouseCode, trimToNull(command.getWarehouseCode()))
                .eq(StringUtils.hasText(command.getLocationCode()), StorageLedger::getLocationCode, trimToNull(command.getLocationCode()))
                .eq(StringUtils.hasText(command.getResultStatus()), StorageLedger::getResultStatus, trimToNull(command.getResultStatus()))
                .orderByDesc(StorageLedger::getOperationTime)
                .last("limit 200"))
            .stream()
            .map(this::toStorageLedgerResponse)
            .toList();
    }

    @Override
    public StorageLedgerResponse getStorageLedger(Long ledgerId) {
        StorageLedger ledger = storageLedgerMapper.selectById(ledgerId);
        if (ledger == null) {
            throw new BusinessException("Storage ledger not found");
        }
        return toStorageLedgerResponse(ledger);
    }

    private List<LabelValueOption> listEnabledCompanyProjects() {
        return companyProjectMapper.selectList(new LambdaQueryWrapper<CompanyProject>().eq(CompanyProject::getDeleteFlag, "N").eq(CompanyProject::getEnabledFlag, "Y").orderByAsc(CompanyProject::getCompanyProjectCode)).stream().map(item -> option(item.getCompanyProjectCode(), item.getCompanyProjectName())).toList();
    }

    private List<LabelValueOption> listEnabledBusinessModules() {
        return businessModuleMapper
            .selectList(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getDeleteFlag, "N")
                .eq(BusinessModule::getEnabledFlag, "Y")
                .eq(BusinessModule::getLevelNum, 1)
                .orderByAsc(BusinessModule::getSortOrder)
                .orderByAsc(BusinessModule::getModuleCode))
            .stream()
            .map(item -> option(item.getModuleCode(), item.getModuleName()))
            .toList();
    }

    private List<LabelValueOption> listEnabledDocumentOrganizations() {
        return jdbcTemplate.query("select document_organization_code, document_organization_name from fdc_document_organization_t where enable_flag = 'Y' and delete_flag = 'N' order by document_organization_code", (rs, rowNum) -> option(rs.getString(1), rs.getString(2)));
    }

    private List<LabelValueOption> listEnabledCities() {
        return documentOrganizationCityMapper.selectList(new LambdaQueryWrapper<DocumentOrganizationCity>().eq(DocumentOrganizationCity::getEnabledFlag, "Y").eq(DocumentOrganizationCity::getDeleteFlag, "N").orderByAsc(DocumentOrganizationCity::getCountryCode).orderByAsc(DocumentOrganizationCity::getSortOrder)).stream().map(item -> option(item.getCityCode(), item.getCityName())).toList();
    }

    private List<LabelValueOption> listSecurityLevels() {
        return securityLevelDictionaryMapper.selectList(new LambdaQueryWrapper<SecurityLevelDictionary>().eq(SecurityLevelDictionary::getDeleteFlag, "N").eq(SecurityLevelDictionary::getEnabledFlag, "Y").orderByAsc(SecurityLevelDictionary::getSortOrder)).stream().map(item -> option(item.getSecurityLevelCode(), item.getSecurityLevelName())).toList();
    }

    private List<LabelValueOption> listAiModelOptions() {
        return listAiModels().stream().map(item -> option(item.getModelCode(), item.getModelName())).toList();
    }

    private List<LabelValueOption> listGeoCountries() {
        return jdbcTemplate.query(
            """
            select country_code, min(country_code) as country_name
              from fdc_geo_region_t
             where enable_flag = 'Y'
               and delete_flag = 'N'
             group by country_code
             order by country_code
            """,
            (rs, rowNum) -> option(rs.getString("country_code"), rs.getString("country_name"))
        );
    }

    private List<LabelValueOption> listGeoRepOffices() {
        return jdbcTemplate.query(
            """
            select country_code, min(rep_office_name) as rep_office_name
              from fdc_geo_region_t
             where enable_flag = 'Y'
               and delete_flag = 'N'
             group by country_code
             order by country_code
            """,
            (rs, rowNum) -> option(rs.getString("country_code"), rs.getString("rep_office_name"))
        );
    }

    private List<LabelValueOption> listGeoRegions() {
        return jdbcTemplate.query(
            """
            select country_code, min(region_name) as region_name
              from fdc_geo_region_t
             where enable_flag = 'Y'
               and delete_flag = 'N'
             group by country_code
             order by country_code
            """,
            (rs, rowNum) -> option(rs.getString("country_code"), rs.getString("region_name"))
        );
    }

    private List<LabelValueOption> listCustodyStatuses() {
        return List.of(
            option("UNARCHIVED", "未归档"),
            option("ARCHIVED", "已归档")
        );
    }

    private List<LabelValueOption> loadDictionaryOptions(String categoryCode) {
        return jdbcTemplate.query("select item_code, item_name from fdc_dict_item_t where category_code = ? and enable_flag = 'Y' and delete_flag = 'N' order by sort_order, item_code", (rs, rowNum) -> option(rs.getString(1), rs.getString(2)), categoryCode);
    }

    private List<BindArchiveCandidateResponse> listBindableArchiveCandidates(String keyword, String documentTypeCode, String companyProjectCode) {
        return loadBindCandidateArchives(null, keyword, documentTypeCode, companyProjectCode).stream().map(this::toBindArchiveCandidateResponse).toList();
    }

    private List<ArchiveRecord> loadBindCandidateArchives(List<Long> archiveIds, String keyword, String documentTypeCode, String companyProjectCode) {
        LambdaQueryWrapper<ArchiveRecord> wrapper = new LambdaQueryWrapper<ArchiveRecord>()
            .eq(ArchiveRecord::getDeleteFlag, "N")
            .ne(ArchiveRecord::getArchiveStatus, "STORED")
            .isNull(ArchiveRecord::getBindVolumeCode)
            .eq(StringUtils.hasText(documentTypeCode), ArchiveRecord::getDocumentTypeCode, trimToNull(documentTypeCode))
            .eq(StringUtils.hasText(companyProjectCode), ArchiveRecord::getCompanyProjectCode, trimToNull(companyProjectCode))
            .and(StringUtils.hasText(keyword), query -> query
                .like(ArchiveRecord::getDocumentName, trimToNull(keyword))
                .or()
                .like(ArchiveRecord::getArchiveCode, trimToNull(keyword))
                .or()
                .like(ArchiveRecord::getBusinessCode, trimToNull(keyword)))
            .orderByDesc(ArchiveRecord::getLastUpdateDate);
        if (archiveIds != null && !archiveIds.isEmpty()) {
            wrapper.in(ArchiveRecord::getArchiveId, archiveIds);
        } else {
            wrapper.last("limit 200");
        }
        return archiveRecordMapper.selectList(wrapper);
    }

    private List<BindVolumeResponse> buildPreviewGroups(String bindMode, List<ArchiveRecord> archives) {
        if (archives.isEmpty()) {
            return List.of();
        }
        List<ArchiveRecord> sortedArchives = archives.stream()
            .sorted(Comparator.comparing(ArchiveRecord::getDocumentTypeCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(ArchiveRecord::getBusinessCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(ArchiveRecord::getBeginPeriod, Comparator.nullsLast(String::compareTo))
                .thenComparing(ArchiveRecord::getArchiveCode))
            .toList();
        Map<String, List<ArchiveRecord>> grouped;
        if ("MANUAL".equals(bindMode)) {
            grouped = Map.of("MANUAL", sortedArchives);
        } else if ("BUSINESS_CODE".equals(bindMode)) {
            grouped = sortedArchives.stream().collect(Collectors.groupingBy(
                archive -> archive.getDocumentTypeCode() + "|" + Objects.toString(trimToNull(archive.getBusinessCode()), "NO_BUSINESS_CODE"),
                LinkedHashMap::new,
                Collectors.toList()
            ));
        } else {
            grouped = sortedArchives.stream().collect(Collectors.groupingBy(
                archive -> archive.getDocumentTypeCode() + "|" + archive.getBeginPeriod() + "~" + archive.getEndPeriod(),
                LinkedHashMap::new,
                Collectors.toList()
            ));
        }
        List<BindVolumeResponse> responses = new ArrayList<>();
        int groupIndex = 1;
        for (Map.Entry<String, List<ArchiveRecord>> entry : grouped.entrySet()) {
            List<BindVolumeItemResponse> items = new ArrayList<>();
            for (int index = 0; index < entry.getValue().size(); index++) {
                ArchiveRecord archive = entry.getValue().get(index);
                items.add(BindVolumeItemResponse.builder()
                    .archiveId(archive.getArchiveId())
                    .archiveCode(archive.getArchiveCode())
                    .documentName(archive.getDocumentName())
                    .sortNo(index + 1)
                    .primaryFlag(index == 0 ? "Y" : "N")
                    .bindReason(entry.getKey())
                    .build());
            }
            responses.add(BindVolumeResponse.builder()
                .volumeTitle(buildPreviewVolumeTitle(bindMode, entry.getValue(), groupIndex++))
                .bindRuleKey(entry.getKey())
                .carrierTypeCode(resolveVolumeCarrierType(null, entry.getValue()))
                .archiveCount(entry.getValue().size())
                .totalPageCount(resolveVolumePageCount(entry.getValue().stream().map(ArchiveRecord::getArchiveId).toList()))
                .totalCopyCount(resolveVolumeCopyCount(entry.getValue().stream().map(ArchiveRecord::getArchiveId).toList()))
                .bindStatus("DRAFT")
                .items(items)
                .build());
        }
        return responses;
    }

    private void ensureArchivesCanBind(List<Long> archiveIds, Map<Long, ArchiveRecord> archiveMap) {
        for (Long archiveId : archiveIds) {
            ArchiveRecord archive = archiveMap.get(archiveId);
            if (archive == null) {
                throw new BusinessException("Archive not found: " + archiveId);
            }
            if ("STORED".equals(archive.getArchiveStatus())) {
                throw new BusinessException("Stored archive cannot be bound again");
            }
            if (StringUtils.hasText(archive.getBindVolumeCode())) {
                throw new BusinessException("Archive is already bound: " + archive.getArchiveCode());
            }
        }
    }

    private String resolveVolumeTitle(String bindMode, BindCreateCommand.BindCreateVolumeCommand command, List<ArchiveRecord> archives, int index) {
        if (StringUtils.hasText(command.getVolumeTitle())) {
            return command.getVolumeTitle().trim();
        }
        return buildPreviewVolumeTitle(bindMode, archives, index);
    }

    private String buildPreviewVolumeTitle(String bindMode, List<ArchiveRecord> archives, int index) {
        ArchiveRecord sample = archives.get(0);
        if ("BUSINESS_CODE".equals(bindMode)) {
            return sample.getDocumentName() + "-" + Objects.toString(trimToNull(sample.getBusinessCode()), "未编码") + "-册" + index;
        }
        if ("PERIOD".equals(bindMode)) {
            return sample.getDocumentName() + "-" + sample.getBeginPeriod() + "~" + sample.getEndPeriod() + "-册" + index;
        }
        return sample.getDocumentName() + "-手工成册-" + index;
    }

    private String resolveVolumeCarrierType(BindCreateCommand.BindCreateVolumeCommand command, List<ArchiveRecord> archives) {
        if (command != null && StringUtils.hasText(command.getCarrierTypeCode())) {
            return normalizeCarrierType(command.getCarrierTypeCode());
        }
        Set<String> carrierTypes = archives.stream().map(ArchiveRecord::getCarrierTypeCode).filter(StringUtils::hasText).collect(Collectors.toSet());
        if (carrierTypes.size() == 1) {
            return carrierTypes.iterator().next();
        }
        return "HYBRID";
    }

    private Integer resolveVolumePageCount(List<Long> archiveIds) {
        if (archiveIds.isEmpty()) {
            return 0;
        }
        return archivePaperMapper.selectList(new LambdaQueryWrapper<ArchivePaper>().in(ArchivePaper::getArchiveId, archiveIds))
            .stream()
            .map(ArchivePaper::getActualCopyCount)
            .filter(Objects::nonNull)
            .reduce(0, Integer::sum);
    }

    private Integer resolveVolumeCopyCount(List<Long> archiveIds) {
        if (archiveIds.isEmpty()) {
            return 0;
        }
        return archivePaperMapper.selectList(new LambdaQueryWrapper<ArchivePaper>().in(ArchivePaper::getArchiveId, archiveIds))
            .stream()
            .map(ArchivePaper::getPlannedCopyCount)
            .filter(Objects::nonNull)
            .reduce(0, Integer::sum);
    }

    private BindArchiveCandidateResponse toBindArchiveCandidateResponse(ArchiveRecord archive) {
        return BindArchiveCandidateResponse.builder()
            .archiveId(archive.getArchiveId())
            .archiveCode(archive.getArchiveCode())
            .documentName(archive.getDocumentName())
            .busiModuleCode(archive.getDocumentTypeCode())
            .companyProjectCode(archive.getCompanyProjectCode())
            .businessCode(archive.getBusinessCode())
            .beginPeriod(archive.getBeginPeriod())
            .endPeriod(archive.getEndPeriod())
            .archiveStatus(archive.getArchiveStatus())
            .carrierTypeCode(archive.getCarrierTypeCode())
            .bindVolumeCode(archive.getBindVolumeCode())
            .build();
    }

    private BindVolumeItemResponse toBindVolumeItemResponse(BindVolumeItem item, ArchiveRecord archive) {
        return BindVolumeItemResponse.builder()
            .itemId(item.getItemId())
            .archiveId(archive.getArchiveId())
            .archiveCode(archive.getArchiveCode())
            .documentName(archive.getDocumentName())
            .sortNo(item.getSortNo())
            .primaryFlag(item.getPrimaryFlag())
            .bindReason(item.getBindReason())
            .build();
    }

    private BindVolumeResponse toBindVolumeResponse(BindVolume volume, List<BindVolumeItemResponse> items) {
        return BindVolumeResponse.builder()
            .volumeId(volume.getVolumeId())
            .bindVolumeCode(volume.getBindVolumeCode())
            .volumeTitle(volume.getVolumeTitle())
            .bindRuleKey(volume.getBindRuleKey())
            .carrierTypeCode(volume.getCarrierTypeCode())
            .archiveCount(volume.getArchiveCount())
            .totalPageCount(volume.getTotalPageCount())
            .totalCopyCount(volume.getTotalCopyCount())
            .bindStatus(volume.getBindStatus())
            .remark(volume.getRemark())
            .items(items)
            .build();
    }

    private BindBatchResponse buildBindBatchResponse(BindBatch batch, List<BindVolumeResponse> volumes) {
        return BindBatchResponse.builder()
            .bindBatchId(batch.getBindBatchId())
            .bindBatchCode(batch.getBindBatchCode())
            .bindMode(batch.getBindMode())
            .bindStatus(batch.getBindStatus())
            .bindRemark(batch.getBindRemark())
            .guidedStorageFlag(batch.getGuidedStorageFlag())
            .volumeCount(batch.getVolumeCount())
            .archiveCount(batch.getArchiveCount())
            .nextAction("GO_STORAGE")
            .creationDate(batch.getCreationDate())
            .volumes(volumes)
            .build();
    }

    private List<BindVolumeResponse> loadVolumesByBatch(Long bindBatchId) {
        List<BindVolume> volumes = bindVolumeMapper.selectList(new LambdaQueryWrapper<BindVolume>()
            .eq(BindVolume::getBindBatchId, bindBatchId)
            .eq(BindVolume::getDeleteFlag, "N")
            .orderByAsc(BindVolume::getCreationDate));
        if (volumes.isEmpty()) {
            return List.of();
        }
        List<Long> volumeIds = volumes.stream().map(BindVolume::getVolumeId).toList();
        List<BindVolumeItem> items = bindVolumeItemMapper.selectList(new LambdaQueryWrapper<BindVolumeItem>()
            .in(BindVolumeItem::getVolumeId, volumeIds)
            .orderByAsc(BindVolumeItem::getSortNo));
        List<Long> archiveIds = items.stream().map(BindVolumeItem::getArchiveId).distinct().toList();
        Map<Long, ArchiveRecord> archiveMap = archiveIds.isEmpty() ? Map.of() : archiveRecordMapper.selectList(new LambdaQueryWrapper<ArchiveRecord>()
                .in(ArchiveRecord::getArchiveId, archiveIds)
                .eq(ArchiveRecord::getDeleteFlag, "N"))
            .stream()
            .collect(Collectors.toMap(ArchiveRecord::getArchiveId, Function.identity()));
        Map<Long, List<BindVolumeItemResponse>> itemMap = new LinkedHashMap<>();
        for (BindVolumeItem item : items) {
            itemMap.computeIfAbsent(item.getVolumeId(), key -> new ArrayList<>()).add(toBindVolumeItemResponse(item, archiveMap.get(item.getArchiveId())));
        }
        return volumes.stream().map(item -> toBindVolumeResponse(item, itemMap.getOrDefault(item.getVolumeId(), List.of()))).toList();
    }

    private List<BindVolumeResponse> loadStorageCandidateVolumes(String sourceBindBatchCode, String keyword) {
        List<BindVolume> volumes = bindVolumeMapper.selectList(new LambdaQueryWrapper<BindVolume>()
            .eq(BindVolume::getDeleteFlag, "N")
            .ne(BindVolume::getBindStatus, "STORED")
            .orderByDesc(BindVolume::getCreationDate)
            .last("limit 100"));
        if (StringUtils.hasText(sourceBindBatchCode)) {
            BindBatch batch = requireBindBatch(sourceBindBatchCode);
            volumes = volumes.stream().filter(item -> Objects.equals(item.getBindBatchId(), batch.getBindBatchId())).toList();
        }
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = trimToNull(keyword);
            volumes = volumes.stream()
                .filter(item -> containsIgnoreCase(item.getBindVolumeCode(), normalizedKeyword.toLowerCase(Locale.ROOT)) || containsIgnoreCase(item.getVolumeTitle(), normalizedKeyword.toLowerCase(Locale.ROOT)))
                .toList();
        }
        if (volumes.isEmpty()) {
            return List.of();
        }
        List<BindVolume> selectedVolumes = volumes;
        Map<Long, List<BindVolumeResponse>> groupedResponses = volumes.stream()
            .collect(Collectors.groupingBy(BindVolume::getBindBatchId, LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), group -> loadVolumesByBatch(group.get(0).getBindBatchId()))));
        return groupedResponses.values().stream().flatMap(List::stream).filter(item -> selectedVolumes.stream().anyMatch(volume -> Objects.equals(volume.getVolumeId(), item.getVolumeId()))).toList();
    }

    private List<BindArchiveCandidateResponse> loadStandaloneStorageArchives(String keyword) {
        return archiveRecordMapper.selectList(new LambdaQueryWrapper<ArchiveRecord>()
                .eq(ArchiveRecord::getDeleteFlag, "N")
                .isNull(ArchiveRecord::getBindVolumeCode)
                .ne(ArchiveRecord::getArchiveStatus, "STORED")
                .and(StringUtils.hasText(keyword), query -> query.like(ArchiveRecord::getArchiveCode, trimToNull(keyword)).or().like(ArchiveRecord::getDocumentName, trimToNull(keyword)))
                .orderByDesc(ArchiveRecord::getLastUpdateDate)
                .last("limit 100"))
            .stream()
            .map(this::toBindArchiveCandidateResponse)
            .toList();
    }

    private StorageExecutionResult executeStorageItem(StorageBatch batch, Warehouse warehouse, StorageCreateCommand.StorageCreateItemCommand itemCommand) {
        String itemType = normalizeStorageItemType(itemCommand.getItemType());
        WarehouseLocation location = requireAvailableLocation(warehouse.getWarehouseCode(), itemCommand.getLocationCode());
        if ("VOLUME".equals(itemType)) {
            BindVolume volume = requireBindVolume(itemCommand.getVolumeId());
            List<BindVolumeItem> volumeItems = bindVolumeItemMapper.selectList(new LambdaQueryWrapper<BindVolumeItem>()
                .eq(BindVolumeItem::getVolumeId, volume.getVolumeId())
                .orderByAsc(BindVolumeItem::getSortNo));
            List<Long> archiveIds = volumeItems.stream().map(BindVolumeItem::getArchiveId).toList();
            List<ArchiveRecord> archives = archiveIds.isEmpty() ? List.of() : archiveRecordMapper.selectList(new LambdaQueryWrapper<ArchiveRecord>()
                .in(ArchiveRecord::getArchiveId, archiveIds)
                .eq(ArchiveRecord::getDeleteFlag, "N"));
            ensureLocationCapacity(location, archives.size());

            StorageBatchItem batchItem = new StorageBatchItem();
            batchItem.setStorageBatchId(batch.getStorageBatchId());
            batchItem.setItemType(itemType);
            batchItem.setVolumeId(volume.getVolumeId());
            batchItem.setBindVolumeCode(volume.getBindVolumeCode());
            batchItem.setLocationCode(location.getLocationCode());
            batchItem.setResultStatus("SUCCESS");
            batchItem.setStoredAt(LocalDateTime.now());
            storageBatchItemMapper.insert(batchItem);

            for (ArchiveRecord archive : archives) {
                updateArchiveStorageState(archive, warehouse, location, volume.getBindVolumeCode());
                createStorageLedger(batch, itemType, batch.getSourceBindBatchCode(), volume.getBindVolumeCode(), archive.getArchiveCode(), warehouse.getWarehouseCode(), location.getLocationCode(), "SUCCESS", "册内档案入库成功");
            }
            volume.setBindStatus("STORED");
            volume.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            volume.setLastUpdateDate(LocalDateTime.now());
            bindVolumeMapper.updateById(volume);
            occupyLocation(location, archives.size());
            operationAuditService.record("ARCHIVE_STORAGE", "档案入库", "VOLUME", volume.getBindVolumeCode(), "STORE", "册入库", null, Map.of("locationCode", location.getLocationCode()), SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
            return new StorageExecutionResult(toStorageBatchItemResponse(batchItem));
        }

        ArchiveRecord archive = requireArchive(itemCommand.getArchiveId());
        ensureLocationCapacity(location, 1);
        StorageBatchItem batchItem = new StorageBatchItem();
        batchItem.setStorageBatchId(batch.getStorageBatchId());
        batchItem.setItemType(itemType);
        batchItem.setArchiveId(archive.getArchiveId());
        batchItem.setArchiveCode(archive.getArchiveCode());
        batchItem.setLocationCode(location.getLocationCode());
        batchItem.setResultStatus("SUCCESS");
        batchItem.setStoredAt(LocalDateTime.now());
        storageBatchItemMapper.insert(batchItem);
        updateArchiveStorageState(archive, warehouse, location, archive.getBindVolumeCode());
        occupyLocation(location, 1);
        createStorageLedger(batch, itemType, batch.getSourceBindBatchCode(), archive.getBindVolumeCode(), archive.getArchiveCode(), warehouse.getWarehouseCode(), location.getLocationCode(), "SUCCESS", "档案入库成功");
        operationAuditService.record("ARCHIVE_STORAGE", "档案入库", "ARCHIVE", archive.getArchiveCode(), "STORE", "档案入库", null, Map.of("locationCode", location.getLocationCode()), SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return new StorageExecutionResult(toStorageBatchItemResponse(batchItem));
    }

    private void updateArchiveStorageState(ArchiveRecord archive, Warehouse warehouse, WarehouseLocation location, String bindVolumeCode) {
        archive.setArchiveStatus("STORED");
        archive.setBindVolumeCode(trimToNull(bindVolumeCode));
        archive.setCurrentWarehouseCode(warehouse.getWarehouseCode());
        archive.setCurrentLocationCode(location.getLocationCode());
        archive.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        archive.setLastUpdateDate(LocalDateTime.now());
        archiveRecordMapper.updateById(archive);
    }

    private void createStorageLedger(StorageBatch batch, String itemType, String bindBatchCode, String bindVolumeCode, String archiveCode, String warehouseCode, String locationCode, String resultStatus, String summary) {
        StorageLedger ledger = new StorageLedger();
        ledger.setLedgerCode(generateCode("LED"));
        ledger.setStorageBatchCode(batch.getStorageBatchCode());
        ledger.setItemType(itemType);
        ledger.setBindBatchCode(trimToNull(bindBatchCode));
        ledger.setBindVolumeCode(trimToNull(bindVolumeCode));
        ledger.setArchiveCode(trimToNull(archiveCode));
        ledger.setWarehouseCode(warehouseCode);
        ledger.setLocationCode(locationCode);
        ledger.setActionType("STORE");
        ledger.setResultStatus(resultStatus);
        ledger.setOperatorId(SYSTEM_OPERATOR_ID);
        ledger.setOperatorName(SYSTEM_OPERATOR_NAME);
        ledger.setOperationTime(LocalDateTime.now());
        ledger.setOperationSummary(summary);
        storageLedgerMapper.insert(ledger);
    }

    private void ensureLocationCapacity(WarehouseLocation location, int increment) {
        if (!List.of("FREE", "OCCUPIED", "WARNING").contains(location.getStatus())) {
            throw new BusinessException("Location is not available for storage");
        }
        int occupiedCount = Objects.requireNonNullElse(location.getOccupiedCount(), 0);
        int capacity = Objects.requireNonNullElse(location.getCapacity(), 0);
        if (capacity <= 0 || occupiedCount + increment > capacity) {
            throw new BusinessException("Location capacity exceeded");
        }
    }

    private void occupyLocation(WarehouseLocation location, int increment) {
        int occupiedCount = Objects.requireNonNullElse(location.getOccupiedCount(), 0) + increment;
        location.setOccupiedCount(occupiedCount);
        location.setStatus(occupiedCount >= Objects.requireNonNullElse(location.getCapacity(), 0) ? "WARNING" : "OCCUPIED");
        location.setUtilizationRate(calculateRate(occupiedCount, location.getCapacity()));
        location.setLastUpdateDate(LocalDateTime.now());
        warehouseLocationMapper.updateById(location);
    }

    private BigDecimal calculateRate(Integer occupiedCount, Integer capacity) {
        if (capacity == null || capacity <= 0 || occupiedCount == null || occupiedCount <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(occupiedCount * 100.0 / capacity).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private StorageBatchItemResponse toStorageBatchItemResponse(StorageBatchItem item) {
        return StorageBatchItemResponse.builder()
            .storageItemId(item.getStorageItemId())
            .itemType(item.getItemType())
            .volumeId(item.getVolumeId())
            .archiveId(item.getArchiveId())
            .bindVolumeCode(item.getBindVolumeCode())
            .archiveCode(item.getArchiveCode())
            .locationCode(item.getLocationCode())
            .resultStatus(item.getResultStatus())
            .errorMessage(item.getErrorMessage())
            .storedAt(item.getStoredAt())
            .build();
    }

    private StorageLedgerResponse toStorageLedgerResponse(StorageLedger ledger) {
        return StorageLedgerResponse.builder()
            .ledgerId(ledger.getLedgerId())
            .ledgerCode(ledger.getLedgerCode())
            .storageBatchCode(ledger.getStorageBatchCode())
            .itemType(ledger.getItemType())
            .bindBatchCode(ledger.getBindBatchCode())
            .bindVolumeCode(ledger.getBindVolumeCode())
            .archiveCode(ledger.getArchiveCode())
            .warehouseCode(ledger.getWarehouseCode())
            .locationCode(ledger.getLocationCode())
            .actionType(ledger.getActionType())
            .resultStatus(ledger.getResultStatus())
            .operatorName(ledger.getOperatorName())
            .operationTime(ledger.getOperationTime())
            .operationSummary(ledger.getOperationSummary())
            .build();
    }

    private BindBatch requireBindBatch(String bindBatchCode) {
        BindBatch batch = bindBatchMapper.selectOne(new LambdaQueryWrapper<BindBatch>()
            .eq(BindBatch::getBindBatchCode, bindBatchCode)
            .eq(BindBatch::getDeleteFlag, "N")
            .last("limit 1"));
        if (batch == null) {
            throw new BusinessException("Bind batch not found");
        }
        return batch;
    }

    private BindVolume requireBindVolume(Long volumeId) {
        BindVolume volume = bindVolumeMapper.selectOne(new LambdaQueryWrapper<BindVolume>()
            .eq(BindVolume::getVolumeId, volumeId)
            .eq(BindVolume::getDeleteFlag, "N")
            .last("limit 1"));
        if (volume == null) {
            throw new BusinessException("Bind volume not found");
        }
        if ("STORED".equals(volume.getBindStatus())) {
            throw new BusinessException("Bind volume is already stored");
        }
        return volume;
    }

    private ArchiveRecord requireArchive(Long archiveId) {
        ArchiveRecord archive = archiveRecordMapper.selectOne(new LambdaQueryWrapper<ArchiveRecord>()
            .eq(ArchiveRecord::getArchiveId, archiveId)
            .eq(ArchiveRecord::getDeleteFlag, "N")
            .last("limit 1"));
        if (archive == null) {
            throw new BusinessException("Archive not found");
        }
        if ("STORED".equals(archive.getArchiveStatus())) {
            throw new BusinessException("Archive is already stored");
        }
        return archive;
    }

    private Warehouse requireWarehouse(String warehouseCode) {
        Warehouse warehouse = warehouseMapper.selectOne(new LambdaQueryWrapper<Warehouse>()
            .eq(Warehouse::getWarehouseCode, warehouseCode)
            .last("limit 1"));
        if (warehouse == null) {
            throw new BusinessException("Warehouse not found");
        }
        return warehouse;
    }

    private WarehouseLocation requireAvailableLocation(String warehouseCode, String locationCode) {
        WarehouseLocation location = warehouseLocationMapper.selectOne(new LambdaQueryWrapper<WarehouseLocation>()
            .eq(WarehouseLocation::getWarehouseCode, warehouseCode)
            .eq(WarehouseLocation::getLocationCode, locationCode)
            .eq(WarehouseLocation::getDeleteFlag, "N")
            .last("limit 1"));
        if (location == null) {
            throw new BusinessException("Location not found");
        }
        return location;
    }

    private StorageBatch requireStorageBatch(String storageBatchCode) {
        StorageBatch batch = storageBatchMapper.selectOne(new LambdaQueryWrapper<StorageBatch>()
            .eq(StorageBatch::getStorageBatchCode, storageBatchCode)
            .last("limit 1"));
        if (batch == null) {
            throw new BusinessException("Storage batch not found");
        }
        return batch;
    }

    private boolean hasUnstoredVolumes(Long bindBatchId) {
        Long count = bindVolumeMapper.selectCount(new LambdaQueryWrapper<BindVolume>()
            .eq(BindVolume::getBindBatchId, bindBatchId)
            .eq(BindVolume::getDeleteFlag, "N")
            .ne(BindVolume::getBindStatus, "STORED"));
        return count != null && count > 0;
    }
    private ArchiveCreateSession requireSession(String sessionCode) {
        ArchiveCreateSession session = archiveCreateSessionMapper.selectOne(new LambdaQueryWrapper<ArchiveCreateSession>().eq(ArchiveCreateSession::getSessionCode, sessionCode).last("limit 1"));
        if (session == null) {
            throw new BusinessException("Create session does not exist");
        }
        return session;
    }

    private BusinessModule requireBusinessModule(String documentTypeCode) {
        BusinessModule module = businessModuleMapper.selectOne(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getModuleCode, documentTypeCode)
            .eq(BusinessModule::getDeleteFlag, "N")
            .eq(BusinessModule::getEnabledFlag, "Y")
            .last("limit 1"));
        if (module == null) {
            throw new BusinessException("Business module does not exist or is disabled");
        }
        return module;
    }

    private CompanyProject requireCompanyProject(String companyProjectCode) {
        CompanyProject project = companyProjectMapper.selectOne(new LambdaQueryWrapper<CompanyProject>().eq(CompanyProject::getCompanyProjectCode, companyProjectCode).eq(CompanyProject::getDeleteFlag, "N").eq(CompanyProject::getEnabledFlag, "Y").last("limit 1"));
        if (project == null) {
            throw new BusinessException("Company/project does not exist or is disabled");
        }
        return project;
    }

    private int scoreRule(ArchiveFlowRule rule, String customRule, String archiveDestination) {
        int score = 0;
        if (Objects.equals(trimToNull(rule.getCustomRule()), trimToNull(customRule))) score += 2;
        if (Objects.equals(trimToNull(rule.getArchiveDestination()), trimToNull(archiveDestination))) score += 2;
        if (!StringUtils.hasText(rule.getCustomRule())) score += 1;
        if (!StringUtils.hasText(rule.getArchiveDestination())) score += 1;
        return score;
    }

    private void validateRequired(ArchiveCreateCommand command) {
        requireText(command.getDocumentTypeCode(), "documentTypeCode");
        requireText(command.getCompanyProjectCode(), "companyProjectCode");
        requireText(command.getBeginPeriod(), "beginPeriod");
        requireText(command.getEndPeriod(), "endPeriod");
        requireText(command.getDocumentName(), "documentName");
        requireText(command.getDutyPerson(), "dutyPerson");
        requireText(command.getDutyDepartment(), "dutyDepartment");
        if (command.getDocumentDate() == null) throw new BusinessException("documentDate is required");
        requireText(command.getSecurityLevelCode(), "securityLevelCode");
        requireText(command.getCarrierTypeCode(), "carrierTypeCode");
        requireText(command.getDocumentOrganizationCode(), "documentOrganizationCode");
        if (command.getRetentionPeriodYears() == null) throw new BusinessException("retentionPeriodYears is required");
        requireText(command.getArchiveTypeCode(), "archiveTypeCode");
    }

    private void validateExtValues(List<DocumentTypeExtFieldResponse> fields, Map<String, String> extValues) {
        Map<String, String> safeValues = extValues == null ? Map.of() : extValues;
        for (DocumentTypeExtFieldResponse field : fields) {
            if ("Y".equals(field.getRequiredFlag()) && !StringUtils.hasText(safeValues.get(field.getFieldCode()))) {
                throw new BusinessException(field.getFieldName() + " is required");
            }
        }
    }

    private boolean requiresElectronic(String carrierTypeCode) { return List.of("ELECTRONIC", "HYBRID").contains(normalizeCarrierType(carrierTypeCode)); }
    private boolean requiresPaper(String carrierTypeCode) { return List.of("PAPER", "HYBRID").contains(normalizeCarrierType(carrierTypeCode)); }

    private void persistExtValues(Long archiveId, List<DocumentTypeExtFieldResponse> fields, Map<String, String> extValues) {
        if (extValues == null || extValues.isEmpty()) return;
        Map<String, DocumentTypeExtFieldResponse> fieldMap = fields.stream().collect(Collectors.toMap(DocumentTypeExtFieldResponse::getFieldCode, Function.identity(), (left, right) -> left));
        extValues.forEach((fieldCode, value) -> {
            if (!StringUtils.hasText(value) || !fieldMap.containsKey(fieldCode)) return;
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

    private Map<String, String> resolveExtValues(Map<String, String> submittedValues,
                                                 List<DocumentTypeExtFieldResponse> fields,
                                                 ParsedAttachment combinedParse) {
        Map<String, String> resolved = new LinkedHashMap<>();
        if (submittedValues != null) {
            submittedValues.forEach((key, value) -> {
                if (StringUtils.hasText(value)) {
                    resolved.put(key, value.trim());
                }
            });
        }
        if (fields == null || fields.isEmpty()) {
            return resolved;
        }
        String descriptionFallback = combinedParse.extendedValues().getOrDefault("description", combinedParse.preview());
        for (DocumentTypeExtFieldResponse field : fields) {
            if (resolved.containsKey(field.getFieldCode())) {
                continue;
            }
            String directValue = combinedParse.extendedValues().get(field.getFieldCode());
            if (StringUtils.hasText(directValue)) {
                resolved.put(field.getFieldCode(), directValue.trim());
                continue;
            }
            if ("TEXT".equals(field.getFieldType()) && StringUtils.hasText(descriptionFallback)) {
                resolved.put(field.getFieldCode(), descriptionFallback.trim());
            }
        }
        return resolved;
    }

    private void persistContentAndVectors(Long archiveId, ArchiveAttachment attachment, ParsedAttachment parsed) {
        if (!parsed.hasText()) return;
        ArchiveContent content = new ArchiveContent();
        content.setArchiveId(archiveId);
        content.setAttachmentId(attachment.getAttachmentId());
        content.setContentVersion(attachment.getVersionNo());
        content.setFullText(parsed.fullText());
        content.setTextLength(parsed.fullText().length());
        content.setParseTime(LocalDateTime.now());
        content.setOcrFlag(parsed.ocrEnhanced() ? "Y" : "N");
        content.setDeleteFlag("N");
        content.setCreatedBy(SYSTEM_OPERATOR_ID);
        content.setCreationDate(LocalDateTime.now());
        content.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        content.setLastUpdateDate(LocalDateTime.now());
        archiveContentMapper.insert(content);

        List<String> chunks = archiveTextChunkService.chunk(parsed.fullText());
        String embeddingModelCode = findEmbeddingModelCode();
        for (int index = 0; index < chunks.size(); index++) {
            String chunkText = chunks.get(index);
            ArchiveContentChunk chunk = new ArchiveContentChunk();
            chunk.setContentId(content.getContentId());
            chunk.setArchiveId(archiveId);
            chunk.setAttachmentId(attachment.getAttachmentId());
            chunk.setChunkNo(index + 1);
            chunk.setChunkText(chunkText);
            chunk.setTokenCount(chunkText.length());
            chunk.setPositionStart(index == 0 ? 0 : Math.max(0, index * 420));
            chunk.setPositionEnd(chunk.getPositionStart() + chunkText.length());
            chunk.setDeleteFlag("N");
            chunk.setCreatedBy(SYSTEM_OPERATOR_ID);
            chunk.setCreationDate(LocalDateTime.now());
            chunk.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            chunk.setLastUpdateDate(LocalDateTime.now());
            archiveContentChunkMapper.insert(chunk);

            var vector = archiveTextVectorService.embed(chunkText);
            jdbcTemplate.update(
                "insert into fdc_arch_chunk_vector_t (chunk_id, archive_id, attachment_id, embedding_model_code, vector_value, vector_dimension, content_version, delete_flag, created_by, creation_date, last_updated_by, last_update_date) values (?, ?, ?, ?, CAST(? AS vector), ?, ?, 'N', ?, current_timestamp, ?, current_timestamp)",
                chunk.getChunkId(),
                archiveId,
                attachment.getAttachmentId(),
                embeddingModelCode,
                archiveTextVectorService.toPgVectorLiteral(vector),
                archiveTextVectorService.dimension(),
                attachment.getVersionNo(),
                SYSTEM_OPERATOR_ID,
                SYSTEM_OPERATOR_ID
            );
        }
    }

    private ArchiveCreateSessionResponse buildSessionResponse(ArchiveCreateSession session, List<ArchiveAttachment> attachments, ArchiveAiParseResult parseResult) {
        return ArchiveCreateSessionResponse.builder().sessionId(session.getSessionId()).sessionCode(session.getSessionCode()).createMode(session.getCreateMode()).sessionStatus(session.getSessionStatus()).busiModuleCodeGuess(session.getBusiModuleCodeGuess()).carrierTypeCodeGuess(session.getCarrierTypeCodeGuess()).parseStatus(session.getParseStatus()).aiSummarySnapshot(session.getAiSummarySnapshot()).expireTime(session.getExpireTime()).attachments(attachments.stream().map(this::toAttachmentResponse).toList()).aiParseResult(parseResult).build();
    }

    private ArchiveSummaryResponse buildArchiveSummary(ArchiveRecord archive, Map<String, String> extValues, List<ArchiveAttachment> attachments) {
        Map<String, BusinessModule> businessModuleMap = listBusinessModuleMap();
        Map<String, String> companyNameMap = companyProjectMapper.selectList(new LambdaQueryWrapper<CompanyProject>().eq(CompanyProject::getDeleteFlag, "N")).stream().collect(Collectors.toMap(CompanyProject::getCompanyProjectCode, CompanyProject::getCompanyProjectName, (left, right) -> left));
        Map<String, CompanyGeoMeta> companyGeoMetaMap = listCompanyGeoMetaMap();
        Map<Long, String> userNameMap = listUserNameMap();
        Map<String, String> carrierTypeNameMap = listCarrierTypeNameMap();
        Map<String, String> normalizedExtValues = new LinkedHashMap<>(extValues == null ? Map.of() : extValues);
        CompanyGeoMeta companyGeoMeta = companyGeoMetaMap.get(archive.getCompanyProjectCode());
        if (companyGeoMeta != null) {
            normalizedExtValues.put("country", companyGeoMeta.countryCode());
            normalizedExtValues.put("repOffice", companyGeoMeta.repOfficeName());
            normalizedExtValues.put("region", companyGeoMeta.regionName());
            normalizedExtValues.put("companyTag", companyGeoMeta.companyTag());
        }
        SecurityLevelResolver.Resolved secLv = securityLevelResolver.resolve(archive.getSecurityLevelCode());
        return ArchiveSummaryResponse.builder().archiveId(archive.getArchiveId()).archiveCode(archive.getArchiveCode()).documentTypeCode(resolveRootBusinessModuleCode(archive.getDocumentTypeCode(), businessModuleMap)).documentTypeName(resolveRootBusinessModuleName(archive.getDocumentTypeCode(), businessModuleMap)).companyProjectCode(archive.getCompanyProjectCode()).companyProjectName(companyNameMap.getOrDefault(archive.getCompanyProjectCode(), archive.getCompanyProjectCode())).beginPeriod(normalizeYearMonth(archive.getBeginPeriod())).endPeriod(normalizeYearMonth(archive.getEndPeriod())).documentName(archive.getDocumentName()).businessCode(archive.getBusinessCode()).dutyPerson(resolveUserNameByIdString(archive.getDutyPerson(), userNameMap)).dutyDepartment(archive.getDutyDepartment()).documentDate(archive.getDocumentDate()).securityLevelCode(secLv.canonicalCode()).securityLevelName(secLv.displayName()).sourceSystem(archive.getSourceSystem()).archiveDestination(archive.getArchiveDestination()).originPlace(archive.getOriginPlace()).carrierTypeCode(carrierTypeNameMap.getOrDefault(archive.getCarrierTypeCode(), archive.getCarrierTypeCode())).remark(archive.getRemark()).aiArchiveSummary(archive.getAiArchiveSummary()).documentOrganizationCode(archive.getDocumentOrganizationCode()).retentionPeriodYears(archive.getRetentionPeriodYears()).archiveTypeCode(resolveBusinessModuleDisplayName(archive.getArchiveTypeCode(), businessModuleMap)).archiveStatus(archive.getArchiveStatus()).lifecycleStatus(archive.getArchiveStatus()).custodyStatus(archive.getArchiveStatus()).parseStatus(archive.getParseStatus()).vectorStatus(archive.getVectorStatus()).lastUpdateDate(archive.getLastUpdateDate()).attachmentCount(attachments.size()).extValues(normalizedExtValues).attachments(attachments.stream().map(this::toAttachmentResponse).toList()).build();
    }

    private Map<String, CompanyGeoMeta> listCompanyGeoMetaMap() {
        return jdbcTemplate.query(
            """
            select cp.company_project_code,
                   cp.company_tag,
                   cp.country_code,
                   geo.rep_office_name,
                   geo.region_name
              from fdc_company_project_t cp
              left join (
                    select country_code,
                           min(rep_office_name) as rep_office_name,
                           min(region_name) as region_name
                      from fdc_geo_region_t
                     where delete_flag = 'N'
                     group by country_code
              ) geo on geo.country_code = cp.country_code
             where cp.delete_flag = 'N'
            """,
            rs -> {
                Map<String, CompanyGeoMeta> result = new LinkedHashMap<>();
                while (rs.next()) {
                    result.put(
                        rs.getString("company_project_code"),
                        new CompanyGeoMeta(
                            trimToNull(rs.getString("company_tag")),
                            trimToNull(rs.getString("country_code")),
                            trimToNull(rs.getString("rep_office_name")),
                            trimToNull(rs.getString("region_name"))
                        )
                    );
                }
                return result;
            }
        );
    }

    private Map<String, BusinessModule> listBusinessModuleMap() {
        return businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>())
            .stream()
            .filter(b -> b != null && StringUtils.hasText(b.getModuleCode()))
            .collect(Collectors.toMap(BusinessModule::getModuleCode, item -> item, (left, right) -> left));
    }

    private String resolveBusinessModuleDisplayName(String moduleCode, Map<String, BusinessModule> documentTypeMap) {
        if (!StringUtils.hasText(moduleCode)) {
            return moduleCode;
        }
        BusinessModule current = documentTypeMap.get(moduleCode);
        if (current != null && StringUtils.hasText(current.getModuleName())) {
            return current.getModuleName();
        }
        String matchedCode = null;
        for (String code : documentTypeMap.keySet()) {
            if (moduleCode.startsWith(code) && (matchedCode == null || code.length() > matchedCode.length())) {
                matchedCode = code;
            }
        }
        if (matchedCode == null) {
            return moduleCode;
        }
        BusinessModule matched = documentTypeMap.get(matchedCode);
        return matched == null || !StringUtils.hasText(matched.getModuleName()) ? moduleCode : matched.getModuleName();
    }

    private String resolveRootBusinessModuleCode(String moduleCode, Map<String, BusinessModule> documentTypeMap) {
        if (!StringUtils.hasText(moduleCode)) {
            return moduleCode;
        }
        BusinessModule current = documentTypeMap.get(moduleCode);
        if (current != null && current.getLevelNum() != null && current.getLevelNum() == 1) {
            return current.getModuleCode();
        }
        if (current != null && StringUtils.hasText(current.getAncestorPath())) {
            return current.getAncestorPath().split("/")[0];
        }
        String matchedRootCode = null;
        for (String code : documentTypeMap.keySet()) {
            if (moduleCode.startsWith(code) && (matchedRootCode == null || code.length() > matchedRootCode.length())) {
                matchedRootCode = code;
            }
        }
        if (matchedRootCode == null) {
            return moduleCode;
        }
        BusinessModule matched = documentTypeMap.get(matchedRootCode);
        if (matched != null && matched.getLevelNum() != null && matched.getLevelNum() > 1 && StringUtils.hasText(matched.getAncestorPath())) {
            return matched.getAncestorPath().split("/")[0];
        }
        return matchedRootCode;
    }

    private String resolveRootBusinessModuleName(String moduleCode, Map<String, BusinessModule> documentTypeMap) {
        String rootCode = resolveRootBusinessModuleCode(moduleCode, documentTypeMap);
        BusinessModule root = documentTypeMap.get(rootCode);
        return root == null || !StringUtils.hasText(root.getModuleName()) ? rootCode : root.getModuleName();
    }

    private String formatYearMonth(LocalDate date) {
        return date == null ? null : date.toString().substring(0, 7);
    }

    private String normalizeYearMonth(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        return trimmed.length() >= 7 ? trimmed.substring(0, 7) : trimmed;
    }

    private Map<Long, String> listUserNameMap() {
        String userTableName = resolveUserTableName();
        if (userTableName == null) {
            return Map.of();
        }
        String userNameColumn = resolveUserNameColumn(userTableName);
        if (userNameColumn == null) {
            return Map.of();
        }
        return jdbcTemplate.query(
            "select user_id, " + userNameColumn + " as user_name from " + userTableName,
            rs -> {
                Map<Long, String> result = new LinkedHashMap<>();
                while (rs.next()) {
                    result.put(rs.getLong("user_id"), rs.getString("user_name"));
                }
                return result;
            }
        );
    }

    private Map<String, String> listCarrierTypeNameMap() {
        return jdbcTemplate.query(
            "select item_code, item_name from fdc_dict_item_t where category_code = 'ARCHIVE_CARRIER_TYPE' and enable_flag = 'Y' and delete_flag = 'N'",
            rs -> {
                Map<String, String> result = new LinkedHashMap<>();
                while (rs.next()) {
                    result.put(rs.getString("item_code"), rs.getString("item_name"));
                }
                return result;
            }
        );
    }

    private String resolveUserNameByIdString(String userIdOrName, Map<Long, String> userNameMap) {
        if (!StringUtils.hasText(userIdOrName)) {
            return userIdOrName;
        }
        try {
            Long userId = Long.valueOf(userIdOrName.trim());
            return userNameMap.getOrDefault(userId, userIdOrName);
        } catch (NumberFormatException ex) {
            return userIdOrName;
        }
    }

    private String resolveUserTableName() {
        Integer tplExists = jdbcTemplate.queryForObject(
            "select count(1) from information_schema.tables where table_schema = 'public' and table_name = 'tpl_user_t'",
            Integer.class
        );
        if (tplExists != null && tplExists > 0) {
            return "tpl_user_t";
        }
        Integer legacyExists = jdbcTemplate.queryForObject(
            "select count(1) from information_schema.tables where table_schema = 'public' and table_name = 'fdc_user_t'",
            Integer.class
        );
        if (legacyExists != null && legacyExists > 0) {
            return "fdc_user_t";
        }
        return null;
    }

    private String resolveUserNameColumn(String userTableName) {
        if (!StringUtils.hasText(userTableName)) {
            return null;
        }
        Integer modernExists = jdbcTemplate.queryForObject(
            "select count(1) from information_schema.columns where table_schema = 'public' and table_name = ? and column_name = 'user_name'",
            Integer.class,
            userTableName
        );
        if (modernExists != null && modernExists > 0) {
            return "user_name";
        }
        Integer legacyExists = jdbcTemplate.queryForObject(
            "select count(1) from information_schema.columns where table_schema = 'public' and table_name = ? and column_name = 'username'",
            Integer.class,
            userTableName
        );
        if (legacyExists != null && legacyExists > 0) {
            return "username";
        }
        Integer realNameExists = jdbcTemplate.queryForObject(
            "select count(1) from information_schema.columns where table_schema = 'public' and table_name = ? and column_name = 'real_name'",
            Integer.class,
            userTableName
        );
        if (realNameExists != null && realNameExists > 0) {
            return "real_name";
        }
        return null;
    }

    private List<ArchiveAttachment> listSessionAttachments(Long sessionId) {
        return archiveAttachmentMapper.selectList(new LambdaQueryWrapper<ArchiveAttachment>().eq(ArchiveAttachment::getSessionId, sessionId).eq(ArchiveAttachment::getDeleteFlag, "N").orderByAsc(ArchiveAttachment::getAttachmentSeq));
    }

    private Map<Long, List<ArchiveAttachment>> loadAttachmentMap(List<Long> archiveIds) {
        if (archiveIds == null || archiveIds.isEmpty()) return Map.of();
        return archiveAttachmentMapper.selectList(new LambdaQueryWrapper<ArchiveAttachment>().in(ArchiveAttachment::getArchiveId, archiveIds).eq(ArchiveAttachment::getDeleteFlag, "N").orderByAsc(ArchiveAttachment::getAttachmentSeq)).stream().collect(Collectors.groupingBy(ArchiveAttachment::getArchiveId, LinkedHashMap::new, Collectors.toList()));
    }

    private Set<Long> collectInMemoryMatchedIdsFromSummaries(
        List<ArchiveSummaryResponse> rows,
        Map<Long, List<ArchiveAttachment>> attachmentMap,
        List<String> searchTerms
    ) {
        if (searchTerms.isEmpty()) {
            return Set.of();
        }
        Set<Long> matchedIds = new HashSet<>();
        for (ArchiveSummaryResponse row : rows) {
            Long archiveId = row.getArchiveId();
            if (archiveId == null) {
                continue;
            }
            String content = Stream.of(row.getDocumentName(), row.getBusinessCode(), row.getRemark(), row.getAiArchiveSummary())
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
            if (searchTerms.stream().anyMatch(term -> containsIgnoreCase(content, term.toLowerCase(Locale.ROOT)))) {
                matchedIds.add(archiveId);
                continue;
            }
            Map<String, String> extValues = row.getExtValues();
            if (extValues != null && matchesSearchTerms(extValues, searchTerms)) {
                matchedIds.add(archiveId);
                continue;
            }
            List<ArchiveAttachment> attachments = attachmentMap.get(archiveId);
            if (attachments != null && matchesSearchTerms(attachments, searchTerms)) {
                matchedIds.add(archiveId);
            }
        }
        return matchedIds;
    }

    private Set<Long> collectInMemoryMatchedArchiveIds(
        List<ArchiveRecord> allRecords,
        Map<Long, Map<String, String>> extValueMap,
        Map<Long, List<ArchiveAttachment>> archiveAttachmentMap,
        List<String> searchTerms
    ) {
        Set<Long> matchedIds = new HashSet<>();
        for (ArchiveRecord record : allRecords) {
            Long archiveId = record.getArchiveId();
            // 检查档案记录本身的字段
            if (matchesSearchTerms(record, searchTerms)) {
                matchedIds.add(archiveId);
                continue;
            }
            // 检查扩展字段
            Map<String, String> extValues = extValueMap.get(archiveId);
            if (extValues != null && matchesSearchTerms(extValues, searchTerms)) {
                matchedIds.add(archiveId);
                continue;
            }
            // 检查附件
            List<ArchiveAttachment> attachments = archiveAttachmentMap.get(archiveId);
            if (attachments != null && matchesSearchTerms(attachments, searchTerms)) {
                matchedIds.add(archiveId);
            }
        }
        return matchedIds;
    }

    private boolean matchesSearchTerms(ArchiveRecord record, List<String> searchTerms) {
        String content = record.getDocumentName() + " " + record.getBusinessCode() + " " + record.getRemark();
        return searchTerms.stream().anyMatch(term -> content.toLowerCase().contains(term.toLowerCase()));
    }

    private boolean matchesSearchTerms(Map<String, String> extValues, List<String> searchTerms) {
        String content = extValues.values().stream().collect(Collectors.joining(" "));
        return searchTerms.stream().anyMatch(term -> content.toLowerCase().contains(term.toLowerCase()));
    }

    private boolean matchesSearchTerms(List<ArchiveAttachment> attachments, List<String> searchTerms) {
        String content = attachments.stream()
            .map(attachment -> attachment.getFileName() + " " + attachment.getRemark() + " " + attachment.getAiSummary())
            .collect(Collectors.joining(" "));
        return searchTerms.stream().anyMatch(term -> content.toLowerCase().contains(term.toLowerCase()));
    }

    private Map<Long, Map<String, String>> loadExtValueMap(List<Long> archiveIds) {
        if (archiveIds == null || archiveIds.isEmpty()) return Map.of();
        return archiveExtValueMapper.selectList(new LambdaQueryWrapper<ArchiveExtValue>().in(ArchiveExtValue::getArchiveId, archiveIds)).stream().collect(Collectors.groupingBy(ArchiveExtValue::getArchiveId, LinkedHashMap::new, Collectors.toMap(ArchiveExtValue::getFieldCode, this::resolveExtValue, (left, right) -> right, LinkedHashMap::new)));
    }

    private boolean matchesExtFilters(Map<String, String> extValues, Map<String, String> filters) {
        return filters.entrySet().stream()
            .filter(entry -> StringUtils.hasText(entry.getValue()))
            .filter(entry -> !"invoiceNo".equals(entry.getKey()) && !"refNo".equals(entry.getKey()))
            .allMatch(entry -> containsIgnoreCase(extValues.get(entry.getKey()), entry.getValue().trim().toLowerCase(Locale.ROOT)));
    }

    private void appendHardCodedExtFilterSql(StringBuilder sql, List<Object> params, Map<String, String> extFilters) {
        if (extFilters == null || extFilters.isEmpty()) {
            return;
        }
        List<String> refTokens = MultiValueTextParse.parseSpaceSeparatedValues(extFilters.get("refNo"));
        if (!refTokens.isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < refTokens.size(); i++) {
                if (i > 0) {
                    sql.append(" or ");
                }
                sql.append(
                    "(lower(trim(cast(coalesce(attr42, '') as varchar))) = lower(?)"
                        + " or lower(trim(cast(coalesce(attr43, '') as varchar))) = lower(?)"
                        + " or lower(trim(cast(coalesce(attr44, '') as varchar))) = lower(?)"
                        + " or lower(trim(cast(coalesce(attr45, '') as varchar))) = lower(?)"
                        + " or lower(trim(cast(coalesce(attr46, '') as varchar))) = lower(?))");
                String v = refTokens.get(i);
                for (int k = 0; k < 5; k++) {
                    params.add(v);
                }
            }
            sql.append(")");
        }
        List<String> invoiceTokens = MultiValueTextParse.parseSpaceSeparatedValues(extFilters.get("invoiceNo"));
        if (!invoiceTokens.isEmpty()) {
            sql.append(" and (");
            for (int i = 0; i < invoiceTokens.size(); i++) {
                if (i > 0) {
                    sql.append(" or ");
                }
                sql.append("lower(trim(cast(coalesce(attr41, '') as varchar))) = lower(?)");
                params.add(invoiceTokens.get(i));
            }
            sql.append(")");
        }
        for (Map.Entry<String, String> entry : HARD_CODED_EXT_FIELD_ATTR_MAP.entrySet()) {
            if ("invoiceNo".equals(entry.getKey())) {
                continue;
            }
            String value = trimToNull(extFilters.get(entry.getKey()));
            if (value == null) {
                continue;
            }
            sql.append(" and ").append(entry.getValue()).append(" = ?");
            params.add(value);
        }
    }

    private void appendGeoAndStatusFilterSql(StringBuilder sql, List<Object> params, Map<String, String> extFilters) {
        if (extFilters == null || extFilters.isEmpty()) {
            return;
        }
        String country = trimToNull(extFilters.get("country"));
        if (country != null) {
            sql.append(" and cp.country_code = ?");
            params.add(country);
        }
        String repOffice = trimToNull(extFilters.get("repOffice"));
        if (repOffice != null) {
            sql.append(" and geo.rep_office_name = ?");
            params.add(repOffice);
        }
        String region = trimToNull(extFilters.get("region"));
        if (region != null) {
            sql.append(" and geo.region_name = ?");
            params.add(region);
        }
        String custodyStatus = trimToNull(extFilters.get("custodyStatus"));
        if (custodyStatus != null) {
            sql.append(" and lifecycle_status = ?");
            params.add(custodyStatus);
        }
    }

    private Map<String, String> extractHardCodedExtValues(
        java.sql.ResultSet rs,
        String bizModuleCode,
        Map<String, List<BusinessModuleExtField>> moduleFieldCache
    ) throws java.sql.SQLException {
        Map<String, String> extValues = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : HARD_CODED_EXT_FIELD_ATTR_MAP.entrySet()) {
            String value = trimToNull(rs.getString(entry.getValue()));
            if (value != null) {
                extValues.put(entry.getKey(), value);
            }
        }
        List<String> refValues = new ArrayList<>();
        for (String column : REF_NO_ATTR_COLUMNS) {
            String value = trimToNull(rs.getString(column));
            if (value != null) {
                refValues.add(value);
            }
        }
        if (!refValues.isEmpty()) {
            extValues.put("refNo", String.join(";", refValues));
        }
        mergeBusinessModuleDocumentAttrsIntoExtValues(extValues, rs, bizModuleCode, moduleFieldCache);
        String country = trimToNull(rs.getString("country_code"));
        String repOffice = trimToNull(rs.getString("rep_office_name"));
        String region = trimToNull(rs.getString("region_name"));
        String companyTag = trimToNull(rs.getString("company_tag"));
        extValues.put("country", country == null ? "" : country);
        extValues.put("repOffice", repOffice == null ? "" : repOffice);
        extValues.put("region", region == null ? "" : region);
        extValues.put("companyTag", companyTag == null ? "" : companyTag);
        return extValues;
    }

    private List<String> buildKeywordSearchTerms(String keyword) {
        String trimmed = trimToNull(keyword);
        if (trimmed == null) return List.of();
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(trimmed);

        String normalized = trimmed
            .replaceAll("[，。、“”‘’：:；;,.!?！？（）()【】\\[\\]{}]", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (StringUtils.hasText(normalized)) {
            terms.add(normalized);
        }

        String simplified = normalized;
        for (String filler : List.of("相关文档", "相关资料", "相关文件", "文档", "资料", "文件", "内容", "查询", "搜索", "检索", "查找", "查一下", "搜一下", "看一下", "给我", "帮我", "关于", "有哪些", "有吗", "有没有", "是什么", "怎么", "如何")) {
            simplified = simplified.replace(filler, " ");
        }
        simplified = simplified.replaceAll("\\s+", " ").trim();
        if (StringUtils.hasText(simplified) && simplified.length() >= 2) {
            terms.add(simplified);
            Arrays.stream(simplified.split(" "))
                .map(String::trim)
                .filter(item -> item.length() >= 2)
                .forEach(terms::add);
        }
        return terms.stream().limit(8).toList();
    }

    private List<String> buildNormalizedKeywordSearchTerms(String keyword) {
        String trimmed = trimToNull(keyword);
        if (trimmed == null) return List.of();
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        terms.add(trimmed);

        String normalized = trimmed
            .replaceAll("[,.;:!?()\\[\\]{}<>\"'`~|/\\\\_-]+", " ")
            .replaceAll("\\s+", " ")
            .trim();
        if (StringUtils.hasText(normalized)) {
            terms.add(normalized);
        }

        String simplified = normalized;
        for (String filler : List.of(
            "相关文档", "相关资料", "相关文件", "文档", "资料", "文件", "内容",
            "查询", "搜索", "检索", "查找", "查一个", "搜一个", "看一个",
            "给我", "帮我", "关于", "有哪些", "有吗", "有没有", "是什么", "怎么", "如何",
            "related", "document", "documents", "file", "files", "search", "query"
        )) {
            simplified = simplified.replace(filler, " ");
        }
        simplified = simplified.replaceAll("\\s+", " ").trim();
        if (StringUtils.hasText(simplified) && simplified.length() >= 2) {
            terms.add(simplified);
            Arrays.stream(simplified.split(" "))
                .map(String::trim)
                .filter(item -> item.length() >= 2)
                .filter(item -> !isNoiseSearchToken(item))
                .forEach(terms::add);
        }
        return terms.stream().limit(8).toList();
    }

    private Set<Long> loadKeywordMatchedArchiveIds(List<String> searchTerms) {
        if (searchTerms.isEmpty()) return Set.of();
        Set<Long> matchedArchiveIds = new LinkedHashSet<>();
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct archive_id from fdc_arch_content_t where delete_flag = 'N' and (%s)",
            "full_text",
            searchTerms
        ));
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct archive_id from fdc_arch_attachment_t where delete_flag = 'N' and active_flag = 'Y' and (%s)",
            "file_name",
            searchTerms
        ));
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct archive_id from fdc_arch_attachment_t where delete_flag = 'N' and active_flag = 'Y' and (%s)",
            "ai_summary",
            searchTerms
        ));
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct doc_id from fdc_document_t where coalesce(delete_flag, 0) = 0 and lower(trim(coalesce(lifecycle_status, ''))) <> 'draft' and (%s)",
            "doc_name",
            searchTerms
        ));
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct doc_id from fdc_document_t where coalesce(delete_flag, 0) = 0 and lower(trim(coalesce(lifecycle_status, ''))) <> 'draft' and (%s)",
            "description",
            searchTerms
        ));
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct doc_id from fdc_document_t where coalesce(delete_flag, 0) = 0 and lower(trim(coalesce(lifecycle_status, ''))) <> 'draft' and (%s)",
            "arch_description",
            searchTerms
        ));
        return matchedArchiveIds;
    }

    private Set<Long> loadUnifiedKeywordMatchedArchiveIds(List<String> searchTerms) {
        if (searchTerms.isEmpty()) return Set.of();
        Set<Long> matchedArchiveIds = new LinkedHashSet<>();
        matchedArchiveIds.addAll(loadKeywordMatchedArchiveIds(searchTerms));
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct doc_id from fdc_document_t where coalesce(delete_flag, 0) = 0 and lower(trim(coalesce(lifecycle_status, ''))) <> 'draft' and (%s)",
            "doc_biz_no",
            searchTerms
        ));
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct doc_id from fdc_document_t where coalesce(delete_flag, 0) = 0 and lower(trim(coalesce(lifecycle_status, ''))) <> 'draft' and (%s)",
            "cast(doc_resp_dept_id as varchar)",
            searchTerms
        ));
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct doc_id from fdc_document_t where coalesce(delete_flag, 0) = 0 and lower(trim(coalesce(lifecycle_status, ''))) <> 'draft' and (%s)",
            "source_system",
            searchTerms
        ));
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct archive_id from fdc_doc_ext_t where (%s)",
            "text_value",
            searchTerms
        ));
        matchedArchiveIds.addAll(queryArchiveIdsByTerms(
            "select distinct archive_id from fdc_doc_ext_t where (%s)",
            "dict_item_name_snapshot",
            searchTerms
        ));
        return matchedArchiveIds;
    }

    private Set<Long> queryArchiveIdsByTerms(String sqlTemplate, String columnName, List<String> searchTerms) {
        List<Object> params = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        for (int index = 0; index < searchTerms.size(); index++) {
            if (index > 0) where.append(" or ");
            where.append(columnName).append(" ilike ?");
            params.add("%" + searchTerms.get(index) + "%");
        }
        String sql = sqlTemplate.formatted(where);
        return new LinkedHashSet<>(jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), params.toArray()));
    }

    private Set<Long> loadContentMatchedArchiveIds(List<String> searchTerms) {
        if (searchTerms.isEmpty()) return Set.of();
        StringBuilder sql = new StringBuilder("select distinct archive_id from fdc_arch_content_t where delete_flag = 'N' and (");
        List<Object> params = new ArrayList<>();
        for (int index = 0; index < searchTerms.size(); index++) {
            if (index > 0) sql.append(" or ");
            sql.append("full_text ilike ?");
            params.add("%" + searchTerms.get(index) + "%");
        }
        sql.append(")");
        return new LinkedHashSet<>(jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong(1), params.toArray()));
    }

    private boolean matchesKeyword(ArchiveRecord record, List<String> searchTerms) {
        return searchTerms.stream().anyMatch(term -> {
            String lowerTerm = term.toLowerCase(Locale.ROOT);
            return containsIgnoreCase(record.getDocumentName(), lowerTerm)
                || containsIgnoreCase(record.getBusinessCode(), lowerTerm)
                || containsIgnoreCase(record.getAiArchiveSummary(), lowerTerm)
                || containsIgnoreCase(record.getDutyDepartment(), lowerTerm)
                || containsIgnoreCase(record.getSourceSystem(), lowerTerm);
        });
    }

    private boolean matchesKeywordInValues(Map<String, String> extValues, List<String> searchTerms) {
        return extValues.values().stream()
            .filter(StringUtils::hasText)
            .anyMatch(value -> searchTerms.stream().anyMatch(term -> containsIgnoreCase(value, term.toLowerCase(Locale.ROOT))));
    }

    private boolean matchesKeywordInAttachments(List<ArchiveAttachment> attachments, List<String> searchTerms) {
        return attachments.stream().anyMatch(attachment -> searchTerms.stream().anyMatch(term -> {
            String lowerTerm = term.toLowerCase(Locale.ROOT);
            return containsIgnoreCase(attachment.getFileName(), lowerTerm)
                || containsIgnoreCase(attachment.getAiSummary(), lowerTerm)
                || containsIgnoreCase(attachment.getRemark(), lowerTerm);
        }));
    }

    private boolean isNoiseSearchToken(String token) {
        return Set.of("doc", "docx", "pdf", "txt", "xls", "xlsx", "ppt", "pptx")
            .contains(token.toLowerCase(Locale.ROOT));
    }

    private String resolveExtValue(ArchiveExtValue item) { return "DICT".equals(item.getFieldType()) ? item.getDictItemNameSnapshot() : item.getTextValue(); }
    private ArchiveAttachmentResponse toAttachmentResponse(ArchiveAttachment attachment) {
        return ArchiveAttachmentResponse.builder()
            .attachmentId(attachment.getAttachmentId())
            .attachmentRole(attachment.getAttachmentRole())
            .attachmentTypeCode(attachment.getAttachmentTypeCode())
            .attachmentSeq(attachment.getAttachmentSeq())
            .versionNo(attachment.getVersionNo())
            .fileName(attachment.getFileName())
            .mimeType(attachment.getMimeType())
            .fileSize(attachment.getFileSize())
            .remark(attachment.getRemark())
            .aiSummary(attachment.getAiSummary())
            .parseStatus(attachment.getParseStatus())
            .vectorStatus(attachment.getVectorStatus())
            .creationDate(attachment.getCreationDate())
            .build();
    }

    private ParsedAttachment parseStoredFile(Path storagePath, String originalFilename, String mimeType) {
        try {
            var extracted = archiveFileTextExtractor.extract(storagePath, originalFilename, mimeType);
            String text = extracted.text();
            MetadataFallback fallback = inferMetadataFallback(storagePath, originalFilename, text);
            CompanyProjectMatch companyProjectMatch = matchCompanyProject(fallback.companyName(), fallback.projectName());
            String effectiveText = StringUtils.hasText(text) ? text : buildFallbackText(originalFilename, fallback);
            return new ParsedAttachment(
                effectiveText,
                summarizeText(originalFilename, effectiveText),
                guessDocumentTypeCode(originalFilename, effectiveText, fallback.docType()),
                guessBusinessCode(effectiveText),
                companyProjectMatch.companyProjectCode(),
                companyProjectMatch.companyProjectName(),
                fallback.beginPeriod(),
                fallback.endPeriod(),
                fallback.documentDate(),
                buildParseExplain(fallback, companyProjectMatch),
                StringUtils.hasText(text) ? 0.90d : 0.35d,
                buildExtendedValues(fallback),
                extracted.ocrEnhanced()
            );
        } catch (IOException exception) {
            return new ParsedAttachment(
                "",
                stripExtension(originalFilename) + " 上传成功，但正文暂未解析出来，请人工补充信息或稍后重试。",
                null,
                null,
                null,
                null,
                null,
                null,
                readFileCreationDate(storagePath),
                "正文解析失败，文档生成日期已使用文件属性创建时间兜底",
                0.20d,
                Map.of(),
                false
            );
        }
    }

    private ParsedAttachment buildCombinedParseResult(List<ArchiveAttachment> attachments, String documentTypeCode) {
        String allText = attachments.stream()
            .map(item -> parseStoredFile(Paths.get(item.getStoragePath()), item.getFileName(), item.getMimeType()).fullText())
            .filter(StringUtils::hasText)
            .collect(Collectors.joining("\n"));
        return new ParsedAttachment(
            allText,
            summarizeText(documentTypeCode, allText),
            documentTypeCode,
            guessBusinessCode(allText),
            null,
            null,
            extractBeginPeriod(allText),
            extractEndPeriod(allText),
            extractDocumentDate(allText),
            null,
            StringUtils.hasText(allText) ? 0.92d : 0.30d,
            Map.of(),
            attachments.stream().anyMatch(item -> "SUCCESS".equalsIgnoreCase(item.getParseStatus()))
        );
    }

    private ArchiveAiParseResult buildParseResult(ArchiveCreateSession session, ArchiveAttachment attachment) {
        ParsedAttachment parsed = parseStoredFile(Paths.get(attachment.getStoragePath()), attachment.getFileName(), attachment.getMimeType());
        return ArchiveAiParseResult.builder()
            .suggestedBusiModuleCode(Optional.ofNullable(session.getBusiModuleCodeGuess()).orElse(parsed.suggestedDocumentTypeCode()))
            .suggestedCarrierTypeCode(Optional.ofNullable(session.getCarrierTypeCodeGuess()).orElse("ELECTRONIC"))
            .documentName(stripExtension(attachment.getFileName()))
            .businessCode(parsed.businessCode())
            .companyProjectCode(parsed.companyProjectCode())
            .companyProjectName(parsed.companyProjectName())
            .beginPeriod(parsed.beginPeriod())
            .endPeriod(parsed.endPeriod())
            .documentDate(parsed.documentDate())
            .sourceSystem("AI_UPLOAD")
            .aiSummary(parsed.summary())
            .extractedTextPreview(parsed.preview())
            .confidence(parsed.confidence())
            .parseExplain(parsed.parseExplain())
            .extendedValues(parsed.extendedValues())
            .build();
    }

    private String summarizeText(String title, String text) {
        if (!StringUtils.hasText(text)) {
            return "未提取到可用正文，系统已保留文件并等待人工补录或后续重试。";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        String excerpt = normalized.length() > 180 ? normalized.substring(0, 180) + "..." : normalized;
        return "《" + stripExtension(title) + "》内容摘要：" + excerpt;
    }

    private String guessDocumentTypeCode(String filename, String text, String fallbackDocType) {
        String combined = (filename + " " + Objects.toString(text, "") + " " + Objects.toString(fallbackDocType, "")).toLowerCase(Locale.ROOT);
        return businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getDeleteFlag, "N")
                .eq(BusinessModule::getEnabledFlag, "Y"))
            .stream()
            .filter(item -> combined.contains(item.getModuleName().toLowerCase(Locale.ROOT)) || combined.contains(item.getModuleCode().toLowerCase(Locale.ROOT)))
            .sorted(Comparator.comparing(BusinessModule::getLevelNum).reversed())
            .map(BusinessModule::getModuleCode)
            .findFirst()
            .orElse(null);
    }

    private String guessBusinessCode(String text) {
        if (!StringUtils.hasText(text)) return null;
        Matcher matcher = BUSINESS_CODE_PATTERN.matcher(text.toUpperCase(Locale.ROOT));
        return matcher.find() ? matcher.group() : null;
    }

    private MetadataFallback inferMetadataFallback(Path storagePath, String fileName, String fullText) {
        String safeFileName = fileName == null ? "" : fileName.trim();
        String content = fullText == null ? "" : fullText.trim();
        String baseFileName = stripExtension(safeFileName).trim();
        String firstLine = firstMeaningfulLine(content);
        String personName = extractPersonName(content);
        boolean resume = safeFileName.contains("简历")
            || content.contains("个人简历")
            || safeFileName.toUpperCase(Locale.ROOT).startsWith("JL-");
        if (resume) {
            String description = StringUtils.hasText(personName) ? personName + " 的简历档案" : "候选人简历档案";
            String projectName = StringUtils.hasText(personName) ? personName + " 简历" : null;
            String resumePeriod = extractPeriod(content);
            String documentDate = extractDocumentDate(content);
            if (!StringUtils.hasText(documentDate)) {
                documentDate = readFileCreationDate(storagePath);
            }
            return new MetadataFallback("招聘输入", "简历", null, null, projectName, resumePeriod, resumePeriod, documentDate, description);
        }

        String combined = String.join(" ", safeFileName, firstLine, content.substring(0, Math.min(content.length(), 200)));
        String docType = inferDocType(combined, firstLine);
        String beginPeriod = extractBeginPeriod(content);
        String endPeriod = extractEndPeriod(content);
        String documentDate = extractDocumentDate(content);
        if (!StringUtils.hasText(documentDate)) {
            documentDate = extractDocumentDateFromFileName(safeFileName);
        }
        if (!StringUtils.hasText(documentDate)) {
            documentDate = readFileCreationDate(storagePath);
        }
        String companyName = extractCompanyCandidate(safeFileName, content);
        String description = buildDescription(firstLine, content);
        return new MetadataFallback(
            StringUtils.hasText(baseFileName) ? baseFileName : null,
            docType,
            companyName,
            null,
            companyName,
            beginPeriod,
            endPeriod,
            documentDate,
            description
        );
    }

    private MetadataFallback inferMetadataFallback(String fileName, String fullText) {
        if (fileName != null || fullText != null) {
            return inferMetadataFallback((Path) null, fileName, fullText);
        }
        String safeFileName = fileName == null ? "" : fileName.trim();
        String content = fullText == null ? "" : fullText.trim();
        String baseFileName = stripExtension(safeFileName).trim();
        String firstLine = firstMeaningfulLine(content);
        String personName = extractPersonName(content);
        boolean resume = safeFileName.contains("简历")
            || content.contains("个人简历")
            || safeFileName.toUpperCase(Locale.ROOT).startsWith("JL-");
        if (resume) {
            String description = StringUtils.hasText(personName) ? personName + " 的简历档案" : "候选人简历档案";
            String projectName = StringUtils.hasText(personName) ? personName + " 简历" : null;
            return new MetadataFallback("招聘输入", "简历", null, null, projectName, extractPeriod(content), description);
        }
        String combined = String.join(" ", safeFileName, firstLine, content.substring(0, Math.min(content.length(), 200)));
        String docType = inferDocType(combined, firstLine);
        String periodValue = extractPeriod(content);
        String description = buildDescription(firstLine, content);
        return new MetadataFallback(StringUtils.hasText(baseFileName) ? baseFileName : null, docType, null, null, null, periodValue, description);
    }

    private String inferDocType(String combined, String firstLine) {
        String normalized = combined == null ? "" : combined.toLowerCase(Locale.ROOT);
        if (normalized.contains("简历") || normalized.contains("resume") || normalized.startsWith("jl-")) return "简历";
        if (normalized.contains("报告") || normalized.contains("report") || normalized.contains("体检")) return "报告";
        if (normalized.contains("通知") || normalized.contains("公告") || normalized.contains("意见")) return "通知";
        if (normalized.contains("合同") || normalized.contains("协议")) return "合同";
        if (normalized.contains("发票") || normalized.contains("invoice")) return "发票";
        if (normalized.contains("清单") || normalized.contains("目录") || normalized.contains("确认表")) return "清单";
        if (!isBlank(firstLine) && firstLine.length() <= 30) return firstLine;
        return null;
    }

    private String extractPeriod(String text) {
        Matcher matcher = PERIOD_PATTERN.matcher(text == null ? "" : text);
        if (!matcher.find()) {
            return null;
        }
        return formatYearMonth(matcher.group(1), matcher.group(2));
    }

    private String extractBeginPeriod(String text) {
        Matcher matcher = DATE_RANGE_PATTERN.matcher(text == null ? "" : text);
        if (matcher.find()) {
            return formatYearMonth(matcher.group(1), matcher.group(2));
        }
        return extractPeriod(text);
    }

    private String extractEndPeriod(String text) {
        Matcher matcher = DATE_RANGE_PATTERN.matcher(text == null ? "" : text);
        if (matcher.find()) {
            return formatYearMonth(matcher.group(4), matcher.group(5));
        }
        return extractPeriod(text);
    }

    private String extractDocumentDate(String text) {
        String normalized = text == null ? "" : text;
        Matcher keywordMatcher = Pattern.compile("(?:签订日期|签署日期|文档日期|成文日期|落款日期|日期)[：: ]{0,4}(20\\d{2})[年\\-./](\\d{1,2})(?:[月\\-./](\\d{1,2}))?日?").matcher(normalized);
        if (keywordMatcher.find()) {
            return formatDate(keywordMatcher.group(1), keywordMatcher.group(2), keywordMatcher.group(3));
        }
        return null;
    }

    private String extractDocumentDateFromFileName(String fileName) {
        Matcher matcher = FILE_NAME_DATE_PATTERN.matcher(fileName == null ? "" : fileName);
        if (!matcher.find()) {
            return null;
        }
        return formatDate(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    private String readFileCreationDate(Path storagePath) {
        try {
            BasicFileAttributes attributes = Files.readAttributes(storagePath, BasicFileAttributes.class);
            return attributes.creationTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
        } catch (IOException exception) {
            return null;
        }
    }

    private String extractCompanyCandidate(String fileName, String text) {
        Matcher matcher = COMPANY_IN_BRACKETS_PATTERN.matcher(fileName == null ? "" : fileName);
        if (matcher.find()) {
            String candidate = normalizeCompanyToken(matcher.group(1));
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        String normalizedText = text == null ? "" : text;
        Matcher buyerMatcher = Pattern.compile("(?:甲方|采购方|买方)[：: ]{0,3}([A-Za-z0-9\\p{IsHan}\\-（）() ]{2,40})").matcher(normalizedText);
        if (buyerMatcher.find()) {
            return normalizeCompanyToken(buyerMatcher.group(1));
        }
        Matcher partyMatcher = Pattern.compile("(?:乙方|供应商|卖方)[：: ]{0,3}([A-Za-z0-9\\p{IsHan}\\-（）() ]{2,40})").matcher(normalizedText);
        if (partyMatcher.find()) {
            return normalizeCompanyToken(partyMatcher.group(1));
        }
        return null;
    }

    private CompanyProjectMatch matchCompanyProject(String... candidates) {
        List<CompanyProject> projects = companyProjectMapper.selectList(new LambdaQueryWrapper<CompanyProject>()
            .eq(CompanyProject::getDeleteFlag, "N")
            .eq(CompanyProject::getEnabledFlag, "Y"));
        List<CompanyProject> matched = new ArrayList<>();
        for (String candidate : candidates) {
            String normalizedCandidate = normalizeCompanyToken(candidate);
            if (!StringUtils.hasText(normalizedCandidate)) {
                continue;
            }
            List<CompanyProject> currentMatches = projects.stream()
                .filter(project -> normalizedCandidate.equals(normalizeCompanyToken(project.getCompanyProjectName()))
                    || normalizedCandidate.equals(normalizeCompanyToken(project.getCompanyProjectCode())))
                .toList();
            if (currentMatches.size() == 1) {
                CompanyProject project = currentMatches.get(0);
                return new CompanyProjectMatch(project.getCompanyProjectCode(), project.getCompanyProjectName(), normalizedCandidate);
            }
            matched.addAll(currentMatches);
        }
        if (matched.size() == 1) {
            CompanyProject project = matched.get(0);
            return new CompanyProjectMatch(project.getCompanyProjectCode(), project.getCompanyProjectName(), normalizeCompanyToken(project.getCompanyProjectName()));
        }
        String candidate = Arrays.stream(candidates)
            .map(this::normalizeCompanyToken)
            .filter(StringUtils::hasText)
            .findFirst()
            .orElse(null);
        return new CompanyProjectMatch(null, null, candidate);
    }

    private String buildParseExplain(MetadataFallback fallback, CompanyProjectMatch companyProjectMatch) {
        List<String> explain = new ArrayList<>();
        if (StringUtils.hasText(companyProjectMatch.companyProjectCode())) {
            explain.add("公司：匹配自“" + companyProjectMatch.candidate() + "” -> " + companyProjectMatch.companyProjectName());
        } else if (StringUtils.hasText(companyProjectMatch.candidate())) {
            explain.add("公司：识别到“" + companyProjectMatch.candidate() + "”，但未匹配到唯一主数据");
        }
        if (StringUtils.hasText(fallback.beginPeriod()) || StringUtils.hasText(fallback.endPeriod())) {
            explain.add("开始档期/结束档期：根据全文日期归纳");
        }
        if (StringUtils.hasText(fallback.documentDate())) {
            String source = StringUtils.hasText(extractDocumentDate(fallback.description())) ? "正文日期" : "文件属性或文件名";
            explain.add("文档生成日期：来自" + source);
        }
        return explain.isEmpty() ? null : String.join("\n", explain);
    }

    private String normalizeCompanyToken(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value
            .replace('（', '(')
            .replace('）', ')')
            .replaceAll("[()]", "")
            .replaceAll("\\s+", "")
            .toUpperCase(Locale.ROOT)
            .trim();
    }

    private String formatYearMonth(String year, String month) {
        try {
            return YearMonth.of(Integer.parseInt(year), Integer.parseInt(month)).toString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String formatDate(String year, String month, String day) {
        try {
            int safeDay = StringUtils.hasText(day) ? Integer.parseInt(day) : 1;
            return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), safeDay).toString();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String buildDescription(String firstLine, String fullText) {
        if (!isBlank(firstLine)) {
            return firstLine.length() <= 80 ? firstLine : firstLine.substring(0, 80);
        }
        String normalized = (fullText == null ? "" : fullText).replaceAll("\s+", " ").trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.substring(0, Math.min(normalized.length(), 80));
    }

    private String buildFallbackText(String originalFilename, MetadataFallback fallback) {
        List<String> segments = new ArrayList<>();
        segments.add("文件名：" + stripExtension(originalFilename));
        if (StringUtils.hasText(fallback.docType())) {
            segments.add("推断文档类型：" + fallback.docType());
        }
        if (StringUtils.hasText(fallback.description())) {
            segments.add("摘要：" + fallback.description());
        }
        if (StringUtils.hasText(fallback.periodValue())) {
            segments.add("档期：" + fallback.periodValue());
        }
        return String.join("\n", segments);
    }

    private String extractPersonName(String text) {
        String normalized = text == null ? "" : text;
        Matcher matcher = Pattern.compile("(?:姓名|Name)[:：]?\\s*([\\p{IsHan}A-Za-z·\\s]{2,30})").matcher(normalized);
        boolean matched = matcher.find();
        if (!matched) {
            matcher = PERSON_NAME_PATTERN.matcher(normalized);
            matched = matcher.find();
        }
        if (!matched) {
            return null;
        }
        return matcher.group(1).replaceAll("\\s+", " ").trim();
    }

    private String firstMeaningfulLine(String text) {
        return (text == null ? "" : text).lines().map(String::trim).filter(line -> !line.isEmpty()).findFirst().orElse("");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Map<String, String> buildExtendedValues(MetadataFallback fallback) {
        Map<String, String> result = new LinkedHashMap<>();
        if (StringUtils.hasText(fallback.description())) result.put("description", fallback.description());
        if (StringUtils.hasText(fallback.periodValue())) result.put("periodValue", fallback.periodValue());
        if (StringUtils.hasText(fallback.projectName())) result.put("projectName", fallback.projectName());
        return result;
    }

    private String generateArchiveSummary(ArchiveCreateCommand command, List<ArchiveAttachment> electronicAttachments, List<ArchiveAttachment> paperScanAttachments) {
        List<String> fragments = new ArrayList<>();
        fragments.add("文档名称：" + command.getDocumentName());
        fragments.add("档案类型：" + command.getArchiveTypeCode());
        fragments.add("载体类型：" + normalizeCarrierType(command.getCarrierTypeCode()));
        fragments.add("归档责任部门：" + command.getDutyDepartment());
        if (!electronicAttachments.isEmpty()) {
            fragments.add("电子附件数量：" + electronicAttachments.size());
            fragments.addAll(electronicAttachments.stream().map(ArchiveAttachment::getAiSummary).filter(StringUtils::hasText).limit(2).toList());
        }
        if (!paperScanAttachments.isEmpty()) {
            fragments.add("纸质扫描件数量：" + paperScanAttachments.size());
        }
        return String.join("；", fragments);
    }

    private List<ArchiveSummaryResponse> searchAskReferences(ArchiveAskCommand command, AiModelConfig chatModel) {
        List<ArchiveSummaryResponse> semanticMatches;
        try {
            semanticMatches = findSemanticMatches(command.getQuestion(), command.getDocumentTypeCode(), command.getCompanyProjectCode(), chatModel);
        } catch (Exception exception) {
            semanticMatches = List.of();
        }
        if (!semanticMatches.isEmpty()) return semanticMatches;
        ArchiveQueryCommand queryCommand = new ArchiveQueryCommand();
        queryCommand.setKeyword(command.getQuestion());
        queryCommand.setDocumentTypeCode(command.getDocumentTypeCode());
        queryCommand.setCompanyProjectCode(command.getCompanyProjectCode());
        return queryArchives(queryCommand).getRecords().stream().limit(resolveReferenceLimit(chatModel)).toList();
    }

    private List<ArchiveSummaryResponse> findSemanticMatches(String question, String documentTypeCode, String companyProjectCode, AiModelConfig chatModel) {
        String embeddingModelCode = findEmbeddingModelCode();
        int limit = Math.max(resolveReferenceLimit(chatModel), 3);
        List<Double> vector = archiveTextVectorService.embed(question);
        String normalizedDocumentTypeCode = trimToNull(documentTypeCode);
        String normalizedCompanyProjectCode = trimToNull(companyProjectCode);
        StringBuilder sql = new StringBuilder("""
                select distinct on (d.doc_id)
                    d.doc_id as archive_id,
                    c.chunk_text,
                    (v.vector_value <=> cast(? as vector)) as distance
                from fdc_arch_chunk_vector_t v
                join fdc_arch_content_chunk_t c on c.chunk_id = v.chunk_id and c.delete_flag = 'N'
                join fdc_document_t d on d.doc_id = v.archive_id and coalesce(d.delete_flag, 0) = 0
                where v.delete_flag = 'N'
                  and v.embedding_model_code = ?
                """);
        List<Object> params = new ArrayList<>();
        params.add(archiveTextVectorService.toPgVectorLiteral(vector));
        params.add(embeddingModelCode);
        if (normalizedDocumentTypeCode != null) {
            sql.append(" and d.biz_module_code like ?");
            params.add(normalizedDocumentTypeCode + "%");
        }
        if (normalizedCompanyProjectCode != null) {
            sql.append(" and d.company_code = ?");
            params.add(normalizedCompanyProjectCode);
        }
        sql.append(" order by d.doc_id, distance asc limit ?");
        params.add(limit);
        List<SemanticMatchRow> rows = jdbcTemplate.query(
            sql.toString(),
            (rs, rowNum) -> new SemanticMatchRow(rs.getLong("archive_id"), rs.getString("chunk_text"), rs.getDouble("distance")),
            params.toArray()
        );
        if (rows.isEmpty()) return List.of();
        Map<Long, String> chunkMap = rows.stream().collect(Collectors.toMap(SemanticMatchRow::archiveId, SemanticMatchRow::chunkText, (left, right) -> left));
        List<Long> archiveIds = rows.stream().map(SemanticMatchRow::archiveId).distinct().toList();
        List<ArchiveSummaryResponse> summaries = new ArrayList<>();
        for (Long archiveId : archiveIds) {
            try {
                ArchiveSummaryResponse summary = loadArchiveDetailFromDocumentTable(archiveId);
                if (!StringUtils.hasText(summary.getAiArchiveSummary()) && StringUtils.hasText(chunkMap.get(archiveId))) {
                    summary.setAiArchiveSummary(chunkMap.get(archiveId));
                }
                summaries.add(summary);
            } catch (BusinessException ex) {
                log.debug("Semantic match doc skipped archiveId={} message={}", archiveId, ex.getMessage());
            }
        }
        return summaries.stream().limit(limit).toList();
    }

    private String buildAskEvidenceSnippet(ArchiveSummaryResponse summary) {
        String summaryText = StringUtils.hasText(summary.getAiArchiveSummary()) ? summary.getAiArchiveSummary() : "文档《" + summary.getDocumentName() + "》可作为当前问题的原文依据。";
        String compact = summaryText.replaceAll("\\s+", " ").trim();
        String excerpt = compact.length() > 180 ? compact.substring(0, 180) + "..." : compact;
        return "《" + summary.getDocumentName() + "》：" + excerpt;
    }

    private AiModelConfig findEnabledChatModel() { return aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfig>().eq(AiModelConfig::getDeleteFlag, "N").eq(AiModelConfig::getEnabledFlag, "Y").eq(AiModelConfig::getModelType, "CHAT").orderByAsc(AiModelConfig::getModelCode)).stream().findFirst().orElse(null); }
    private int resolveReferenceLimit(AiModelConfig chatModel) { if (chatModel == null) return 4; return Math.max(3, Objects.requireNonNullElse(chatModel.getOfficialResultCount(), Objects.requireNonNullElse(chatModel.getTopK(), 4))); }
    private String findEmbeddingModelCode() { return aiModelConfigMapper.selectList(new LambdaQueryWrapper<AiModelConfig>().eq(AiModelConfig::getDeleteFlag, "N").eq(AiModelConfig::getEnabledFlag, "Y").eq(AiModelConfig::getModelType, "EMBEDDING").orderByAsc(AiModelConfig::getModelCode)).stream().map(AiModelConfig::getModelCode).findFirst().orElse("LOCAL_EMBEDDING"); }
    private LabelValueOption option(String code, String name) {
        LabelValueOption item = new LabelValueOption();
        item.setCode(code);
        item.setName(name);
        return item;
    }
    private String normalizeBindMode(String bindMode) { String mode = requireText(bindMode, "bindMode").toUpperCase(Locale.ROOT); if (!List.of("BUSINESS_CODE", "PERIOD", "MANUAL").contains(mode)) throw new BusinessException("bindMode only supports BUSINESS_CODE, PERIOD, MANUAL"); return mode; }
    private String normalizeStorageSourceType(String sourceType) { String mode = requireText(sourceType, "sourceType").toUpperCase(Locale.ROOT); if (!List.of("BIND_GUIDED", "DIRECT").contains(mode)) throw new BusinessException("sourceType only supports BIND_GUIDED or DIRECT"); return mode; }
    private String normalizeStorageItemType(String itemType) { String type = requireText(itemType, "itemType").toUpperCase(Locale.ROOT); if (!List.of("VOLUME", "ARCHIVE").contains(type)) throw new BusinessException("itemType only supports VOLUME or ARCHIVE"); return type; }
    private String normalizeCreateMode(String createMode) { String mode = requireText(createMode, "createMode").toUpperCase(Locale.ROOT); if (!List.of("AUTO", "MANUAL").contains(mode)) throw new BusinessException("createMode only supports AUTO or MANUAL"); return mode; }
    private String normalizeCarrierType(String carrierTypeCode) { String carrierType = requireText(carrierTypeCode, "carrierTypeCode").toUpperCase(Locale.ROOT); if (!List.of("ELECTRONIC", "PAPER", "HYBRID").contains(carrierType)) throw new BusinessException("carrierTypeCode only supports ELECTRONIC, PAPER, HYBRID"); return carrierType; }
    private String normalizeAttachmentRole(String attachmentRole) { String role = requireText(attachmentRole, "attachmentRole").toUpperCase(Locale.ROOT); if (!List.of("ELECTRONIC", "PAPER_SCAN").contains(role)) throw new BusinessException("attachmentRole only supports ELECTRONIC or PAPER_SCAN"); return role; }
    private String requireText(String value, String fieldName) { if (!StringUtils.hasText(value)) throw new BusinessException(fieldName + " cannot be blank"); return value.trim(); }
    private String inferAttachmentTypeCode(String fileName, String summary, String preview) {
        String combined = (Objects.toString(fileName, "") + " " + Objects.toString(summary, "") + " " + Objects.toString(preview, "")).toLowerCase(Locale.ROOT);
        if (combined.contains("凭证") || combined.contains("voucher") || combined.contains("记账")) {
            return "ACCOUNTING_VOUCHER";
        }
        return StringUtils.hasText(combined) ? "SUPPORTING_ATTACHMENT" : null;
    }
    private String inferAttachmentRemark(String fileName, String summary, String preview) {
        String baseName = stripExtension(fileName);
        if (StringUtils.hasText(summary)) {
            String normalized = summary.replaceAll("\\s+", " ").trim();
            return normalized.length() > 80 ? normalized.substring(0, 80) : normalized;
        }
        if (StringUtils.hasText(preview)) {
            String normalized = preview.replaceAll("\\s+", " ").trim();
            return normalized.length() > 60 ? normalized.substring(0, 60) : normalized;
        }
        return StringUtils.hasText(baseName) ? baseName : null;
    }
    private String trimToNull(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private Integer nextAttachmentSeq(Long sessionId) { Long count = archiveAttachmentMapper.selectCount(new LambdaQueryWrapper<ArchiveAttachment>().eq(ArchiveAttachment::getSessionId, sessionId).eq(ArchiveAttachment::getDeleteFlag, "N")); return count == null ? 1 : count.intValue() + 1; }
    private String generateCode(String prefix) { return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT); }
    private String extractExtension(String filename) { if (!StringUtils.hasText(filename) || !filename.contains(".")) return ""; return filename.substring(filename.lastIndexOf('.') + 1); }
    private String stripExtension(String filename) { if (!StringUtils.hasText(filename) || !filename.contains(".")) return filename; return filename.substring(0, filename.lastIndexOf('.')); }
    private String md5Hex(Path path) { try { MessageDigest digest = MessageDigest.getInstance("MD5"); byte[] hashed = digest.digest(Files.readAllBytes(path)); StringBuilder builder = new StringBuilder(); for (byte item : hashed) builder.append(String.format("%02x", item)); return builder.toString(); } catch (IOException | NoSuchAlgorithmException exception) { return UUID.randomUUID().toString().replace("-", ""); } }
    private boolean containsIgnoreCase(String source, String lowerKeyword) { return source != null && source.toLowerCase(Locale.ROOT).contains(lowerKeyword); }
    private List<ArchiveAttachment> mergeAttachments(List<ArchiveAttachment> electronicAttachments, List<ArchiveAttachment> paperScanAttachments) { List<ArchiveAttachment> attachments = new ArrayList<>(); attachments.addAll(electronicAttachments); attachments.addAll(paperScanAttachments); return attachments; }

    private record ParsedAttachment(
        String fullText,
        String summary,
        String suggestedDocumentTypeCode,
        String businessCode,
        String companyProjectCode,
        String companyProjectName,
        String beginPeriod,
        String endPeriod,
        String documentDate,
        String parseExplain,
        Double confidence,
        Map<String, String> extendedValues,
        boolean ocrEnhanced
    ) {
        boolean hasText() { return StringUtils.hasText(fullText); }
        String preview() { if (!StringUtils.hasText(fullText)) return ""; String normalized = fullText.replaceAll("\\s+", " ").trim(); return normalized.length() > 200 ? normalized.substring(0, 200) + "..." : normalized; }
    }

    private record SemanticMatchRow(Long archiveId, String chunkText, Double distance) {
    }

    private record StorageExecutionResult(StorageBatchItemResponse response) {
    }

    private static final class MetadataFallback {
        private final String docSource;
        private final String docType;
        private final String companyName;
        private final String departmentName;
        private final String projectName;
        private final String periodValue;
        private final String beginPeriod;
        private final String endPeriod;
        private final String documentDate;
        private final String description;

        private MetadataFallback(
            String docSource,
            String docType,
            String companyName,
            String departmentName,
            String projectName,
            String periodValue,
            String description
        ) {
            this(docSource, docType, companyName, departmentName, projectName, periodValue, periodValue, periodValue, null, description);
        }

        private MetadataFallback(
            String docSource,
            String docType,
            String companyName,
            String departmentName,
            String projectName,
            String beginPeriod,
            String endPeriod,
            String documentDate,
            String description
        ) {
            this(docSource, docType, companyName, departmentName, projectName, beginPeriod, beginPeriod, endPeriod, documentDate, description);
        }

        private MetadataFallback(
            String docSource,
            String docType,
            String companyName,
            String departmentName,
            String projectName,
            String periodValue,
            String beginPeriod,
            String endPeriod,
            String documentDate,
            String description
        ) {
            this.docSource = docSource;
            this.docType = docType;
            this.companyName = companyName;
            this.departmentName = departmentName;
            this.projectName = projectName;
            this.periodValue = periodValue;
            this.beginPeriod = beginPeriod;
            this.endPeriod = endPeriod;
            this.documentDate = documentDate;
            this.description = description;
        }

        private String docSource() { return docSource; }
        private String docType() { return docType; }
        private String companyName() { return companyName; }
        private String departmentName() { return departmentName; }
        private String projectName() { return projectName; }
        private String periodValue() { return periodValue; }
        private String beginPeriod() { return beginPeriod; }
        private String endPeriod() { return endPeriod; }
        private String documentDate() { return documentDate; }
        private String description() { return description; }
    }

    private record CompanyProjectMatch(
        String companyProjectCode,
        String companyProjectName,
        String candidate
    ) {
    }

    private record CompanyGeoMeta(
        String companyTag,
        String countryCode,
        String repOfficeName,
        String regionName
    ) {
    }
}
