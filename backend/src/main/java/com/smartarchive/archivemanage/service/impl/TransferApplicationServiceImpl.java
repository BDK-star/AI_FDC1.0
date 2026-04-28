package com.smartarchive.archivemanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartarchive.archivemanage.domain.ArchiveAttachment;
import com.smartarchive.archivemanage.domain.TransferApplicationExt;
import com.smartarchive.archivemanage.domain.TransferApplication;
import com.smartarchive.archivemanage.domain.TransferApplicationDetail;
import com.smartarchive.archivemanage.dto.DocumentTypeExtFieldResponse;
import com.smartarchive.archivemanage.dto.TransferApplicationCreateCommand;
import com.smartarchive.archivemanage.dto.TransferApplicationDetailCommand;
import com.smartarchive.archivemanage.dto.TransferApplicationDetailAttachmentResponse;
import com.smartarchive.archivemanage.dto.TransferApplicationDetailResponse;
import com.smartarchive.archivemanage.dto.TransferApplicationExtValueCommand;
import com.smartarchive.archivemanage.dto.TransferApplicationExtValueResponse;
import com.smartarchive.archivemanage.dto.TransferApplicationRecordPageCommand;
import com.smartarchive.archivemanage.dto.TransferApplicationRecordPageResponse;
import com.smartarchive.archivemanage.dto.TransferApplicationRecordQuery;
import com.smartarchive.archivemanage.dto.TransferApplicationRecordRowResponse;
import com.smartarchive.archivemanage.dto.TransferApplicationResponse;
import com.smartarchive.archivemanage.mapper.ArchiveAttachmentMapper;
import com.smartarchive.archivemanage.mapper.TransferApplicationDetailMapper;
import com.smartarchive.archivemanage.mapper.TransferApplicationExtMapper;
import com.smartarchive.archivemanage.mapper.TransferApplicationMapper;
import com.smartarchive.archivemanage.service.TransferApplicationService;
import com.smartarchive.archivemanage.service.TransferBusinessModuleExtFieldService;
import com.smartarchive.businessmodule.domain.BusinessModule;
import com.smartarchive.businessmodule.mapper.BusinessModuleMapper;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.workflow.domain.WorkflowInstance;
import com.smartarchive.workflow.dto.StartProcessCommand;
import com.smartarchive.workflow.mapper.WorkflowInstanceMapper;
import com.smartarchive.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TransferApplicationServiceImpl implements TransferApplicationService {
    private static final Long SYSTEM_OPERATOR_ID = 1L;
    private static final Pattern ATTR_COLUMN_PATTERN = Pattern.compile("^attr([1-9][0-9]{0,2})$");
    private static final String ATTACHMENT_BIZ_DOMAIN_TRANSFER = "TRANSFER_APPLICATION";
    private static final String ATTACHMENT_ROLE_ELECTRONIC = "ELECTRONIC";
    private static final String ATTACHMENT_TYPE_TRANSFER_DETAIL = "TRANSFER_DETAIL";

    private final TransferApplicationMapper transferApplicationMapper;
    private final TransferApplicationDetailMapper transferApplicationDetailMapper;
    private final ArchiveAttachmentMapper archiveAttachmentMapper;
    private final TransferApplicationExtMapper transferApplicationExtMapper;
    private final TransferBusinessModuleExtFieldService transferBusinessModuleExtFieldService;
    private final BusinessModuleMapper businessModuleMapper;
    private final WorkflowService workflowService;
    private final WorkflowInstanceMapper workflowInstanceMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<TransferApplicationResponse> list(Long tenantid) {
        return transferApplicationMapper.selectList(new LambdaQueryWrapper<TransferApplication>()
                .eq(tenantid != null, TransferApplication::getTenantid, tenantid)
                .eq(TransferApplication::getDeleteFlag, "N")
                .orderByDesc(TransferApplication::getLastUpdateDate))
            .stream()
            .map(item -> toResponse(item, loadDetails(item.getApplicationId(), item.getTenantid())))
            .toList();
    }

    @Override
    public TransferApplicationResponse detail(Long applicationId) {
        TransferApplication application = requireApplication(applicationId);
        return toResponse(application, loadDetails(applicationId, application.getTenantid()));
    }

    @Override
    @Transactional
    public TransferApplicationResponse create(TransferApplicationCreateCommand command) {
        String applicationNumber = requireText(command.getApplicationNumber(), "applicationNumber");
        ensureApplicationNumberAvailable(applicationNumber, command.getTenantid());
        validateHeader(command);

        LocalDateTime now = LocalDateTime.now();
        TransferApplication application = new TransferApplication();
        fillApplicationFromCommand(application, command, applicationNumber, now);
        application.setEnableFlag("Y");
        application.setDeleteFlag("N");
        application.setCreatedBy(SYSTEM_OPERATOR_ID);
        application.setCreationDate(now);
        application.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        application.setLastUpdateDate(now);
        application.setSysDescription(trimToNull(command.getSysDescription()));
        application.setLastUpdateTraceId(null);
        application.setLastUpdateVersion(0);
        application.setTenantid(command.getTenantid());
        transferApplicationMapper.insert(application);

        replaceDetails(application, command.getDetails(), now);

        afterPersistStartWorkflowIfSubmitted(application, command);

        return toResponse(application, loadDetails(application.getApplicationId(), application.getTenantid()));
    }

    @Override
    @Transactional
    public TransferApplicationResponse update(Long applicationId, TransferApplicationCreateCommand command) {
        TransferApplication application = requireApplication(applicationId);
        validateHeader(command);

        String nextApplicationNumber = requireText(command.getApplicationNumber(), "applicationNumber");
        if (!nextApplicationNumber.equals(application.getApplicationNumber())) {
            ensureApplicationNumberAvailable(nextApplicationNumber, command.getTenantid());
        }
        if (!application.getTenantid().equals(command.getTenantid())) {
            throw new BusinessException("tenantid does not match current application");
        }
        LocalDateTime now = LocalDateTime.now();
        fillApplicationFromCommand(application, command, nextApplicationNumber, now);
        application.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        application.setLastUpdateDate(now);
        transferApplicationMapper.updateById(application);

        transferApplicationDetailMapper.update(null, new LambdaUpdateWrapper<TransferApplicationDetail>()
            .eq(TransferApplicationDetail::getApplicationId, applicationId)
            .eq(TransferApplicationDetail::getTenantid, command.getTenantid())
            .eq(TransferApplicationDetail::getDeleteFlag, "N")
            .set(TransferApplicationDetail::getDeleteFlag, "Y")
            .set(TransferApplicationDetail::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(TransferApplicationDetail::getLastUpdateDate, now));
        archiveAttachmentMapper.update(null, new LambdaUpdateWrapper<ArchiveAttachment>()
            .eq(ArchiveAttachment::getBizDomain, ATTACHMENT_BIZ_DOMAIN_TRANSFER)
            .eq(ArchiveAttachment::getApplicationId, applicationId)
            .eq(ArchiveAttachment::getDeleteFlag, "N")
            .set(ArchiveAttachment::getDeleteFlag, "Y")
            .set(ArchiveAttachment::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(ArchiveAttachment::getLastUpdateDate, now));
        transferApplicationExtMapper.update(null, new LambdaUpdateWrapper<TransferApplicationExt>()
            .eq(TransferApplicationExt::getMasterId, applicationId)
            .eq(TransferApplicationExt::getTenantid, command.getTenantid())
            .eq(TransferApplicationExt::getDeleteFlag, "N")
            .set(TransferApplicationExt::getDeleteFlag, "Y")
            .set(TransferApplicationExt::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(TransferApplicationExt::getLastUpdateDate, now));

        replaceDetails(application, command.getDetails(), now);

        afterPersistStartWorkflowIfSubmitted(application, command);

        return toResponse(application, loadDetails(application.getApplicationId(), application.getTenantid()));
    }

    @Override
    public TransferApplicationRecordPageResponse searchPage(TransferApplicationRecordPageCommand command) {
        TransferApplicationRecordQuery filter =
            command.getFilter() != null ? command.getFilter() : new TransferApplicationRecordQuery();
        normalizeRecordQuery(filter);
        long tenantid = command.getTenantid() != null ? command.getTenantid() : 1L;
        long total = transferApplicationMapper.countTransferApplicationRecords(tenantid, filter);
        int page = command.getPage();
        int pageSize = command.getPageSize();
        long offset = (long) (page - 1) * pageSize;
        List<TransferApplication> rows =
            transferApplicationMapper.selectTransferApplicationRecordPage(tenantid, filter, offset, pageSize);
        int pages = pageSize <= 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        Map<String, String> busiModuleNameMap = rows.stream()
            .map(TransferApplication::getBusiModuleCode)
            .filter(StringUtils::hasText)
            .distinct()
            .collect(Collectors.collectingAndThen(Collectors.toList(), codes -> {
                if (codes.isEmpty()) {
                    return Map.<String, String>of();
                }
                return businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
                        .in(BusinessModule::getModuleCode, codes)
                        .eq(BusinessModule::getDeleteFlag, "N"))
                    .stream()
                    .filter(item -> StringUtils.hasText(item.getModuleCode()))
                    .collect(Collectors.toMap(
                        BusinessModule::getModuleCode,
                        item -> StringUtils.hasText(item.getModuleName()) ? item.getModuleName() : item.getModuleCode(),
                        (left, right) -> left
                    ));
            }));
        Map<Long, String> userDisplayMap = loadUserDisplayMapForRecordRows(rows);
        List<TransferApplicationRecordRowResponse> records = rows.stream()
            .map(item -> toRecordRow(item, busiModuleNameMap, userDisplayMap))
            .toList();
        return TransferApplicationRecordPageResponse.builder()
            .records(records)
            .total(total)
            .pages(pages)
            .page(page)
            .pageSize(pageSize)
            .build();
    }

    private void normalizeRecordQuery(TransferApplicationRecordQuery f) {
        if (f.getApplicationDateRange() != null && !f.getApplicationDateRange().isEmpty()) {
            List<String> range = f.getApplicationDateRange();
            if (range.size() >= 1 && StringUtils.hasText(range.get(0))) {
                f.setApplicationDateStart(LocalDate.parse(range.get(0).trim()).atStartOfDay());
            }
            if (range.size() >= 2 && StringUtils.hasText(range.get(1))) {
                f.setApplicationDateEnd(LocalDate.parse(range.get(1).trim()).atTime(23, 59, 59));
            }
        }
        if (f.getArchPeriodRange() != null && !f.getArchPeriodRange().isEmpty()) {
            List<String> range = f.getArchPeriodRange();
            if (range.size() >= 1 && StringUtils.hasText(range.get(0))) {
                f.setArchPeriodStart(normalizeYearMonth(range.get(0)));
            }
            if (range.size() >= 2 && StringUtils.hasText(range.get(1))) {
                f.setArchPeriodEnd(normalizeYearMonth(range.get(1)));
            }
        }
    }

    private String normalizeYearMonth(String input) {
        String value = input == null ? "" : input.trim();
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.length() >= 7) {
            return value.substring(0, 7);
        }
        throw new BusinessException("archPeriod must be yyyy-MM");
    }

    private TransferApplicationRecordRowResponse toRecordRow(TransferApplication item,
                                                             Map<String, String> busiModuleNameMap,
                                                             Map<Long, String> userDisplayMap) {
        String busiModuleCode = item.getBusiModuleCode();
        String busiModuleName = StringUtils.hasText(busiModuleCode)
            ? busiModuleNameMap.getOrDefault(busiModuleCode, busiModuleCode)
            : null;
        return TransferApplicationRecordRowResponse.builder()
            .applicationId(item.getApplicationId())
            .applicationNumber(item.getApplicationNumber())
            .busiModuleCode(busiModuleCode)
            .busiModuleName(busiModuleName)
            .applicant(item.getApplicant())
            .applicantName(formatUserDisplay(item.getApplicant(), userDisplayMap))
            .applicationDate(item.getApplicationDate())
            .applicationStatus(item.getApplicationStatus())
            .documentRecipient(item.getDocumentRecipient())
            .documentRecipientName(formatUserDisplay(item.getDocumentRecipient(), userDisplayMap))
            .expressType(item.getExpressType())
            .expressNumber(item.getExpressNumber())
            .build();
    }

    private String formatUserDisplay(Long userId, Map<Long, String> userDisplayMap) {
        if (userId == null) {
            return "-";
        }
        return userDisplayMap.getOrDefault(userId, "用户-" + userId);
    }

    private Map<Long, String> loadUserDisplayMapForRecordRows(List<TransferApplication> rows) {
        Set<Long> ids = new HashSet<>();
        for (TransferApplication row : rows) {
            if (row.getApplicant() != null && row.getApplicant() > 0) {
                ids.add(row.getApplicant());
            }
            if (row.getDocumentRecipient() != null && row.getDocumentRecipient() > 0) {
                ids.add(row.getDocumentRecipient());
            }
        }
        return loadUserDisplayMap(ids);
    }

    private Map<Long, String> loadUserDisplayMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        String placeholders = String.join(",", userIds.stream().map(id -> "?").toList());
        List<Object> params = new ArrayList<>(userIds);
        return jdbcTemplate.query(
            """
            select user_id,
                   coalesce(
                    nullif(trim(concat_ws(' ', nullif(user_name, ''), nullif(employee_no, ''))), ''),
                     cast(user_id as varchar)
                   ) as user_display
              from tpl_user_t
             where delete_flag = 'N'
               and user_id in (""" + placeholders + ")",
            rs -> {
                Map<Long, String> out = new LinkedHashMap<>();
                while (rs.next()) {
                    out.put(rs.getLong("user_id"), rs.getString("user_display"));
                }
                return out;
            },
            params.toArray()
        );
    }

    @Override
    @Transactional
    public void delete(Long applicationId) {
        TransferApplication application = requireApplication(applicationId);
        application.setDeleteFlag("Y");
        application.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        application.setLastUpdateDate(LocalDateTime.now());
        transferApplicationMapper.updateById(application);
    }

    @Override
    @Transactional
    public TransferApplicationDetailAttachmentResponse uploadDetailAttachment(Long applicationId,
                                                                              Long detailId,
                                                                              String remark,
                                                                              MultipartFile file) {
        requireApplication(applicationId);
        TransferApplicationDetail detail = requireDetail(applicationId, detailId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("附件不能为空");
        }
        String originalFilename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "attachment.bin";
        Path storageRoot = Paths.get(System.getProperty("user.dir"), "storage", "transfer-attachments",
            LocalDateTime.now().toLocalDate().toString().replace("-", ""));
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException ex) {
            throw new BusinessException("创建附件目录失败");
        }
        String storageName = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
        Path storagePath = storageRoot.resolve(storageName);
        try {
            Files.copy(file.getInputStream(), storagePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("保存附件失败");
        }
        LocalDateTime now = LocalDateTime.now();
        ArchiveAttachment entity = new ArchiveAttachment();
        entity.setBizDomain(ATTACHMENT_BIZ_DOMAIN_TRANSFER);
        entity.setApplicationId(applicationId);
        entity.setApplicationDetailId(detail.getApplicationDetailId());
        entity.setAttachmentRole(ATTACHMENT_ROLE_ELECTRONIC);
        entity.setAttachmentTypeCode(ATTACHMENT_TYPE_TRANSFER_DETAIL);
        entity.setAttachmentSeq(1);
        entity.setVersionNo(1);
        entity.setFileName(originalFilename);
        entity.setFileExt(extractFileExt(originalFilename));
        entity.setStoragePath(storagePath.toString());
        entity.setStorageKey(storageName);
        entity.setMimeType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setRemark(trimToNull(remark));
        entity.setOcrStatus("PENDING");
        entity.setParseStatus("PENDING");
        entity.setVectorStatus("PENDING");
        entity.setActiveFlag("Y");
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(now);
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(now);
        archiveAttachmentMapper.insert(entity);
        return toAttachmentResponse(entity);
    }

    @Override
    public List<TransferApplicationDetailAttachmentResponse> listDetailAttachments(Long applicationId, Long detailId) {
        requireApplication(applicationId);
        requireDetail(applicationId, detailId);
        return archiveAttachmentMapper.selectList(new LambdaQueryWrapper<ArchiveAttachment>()
                .eq(ArchiveAttachment::getBizDomain, ATTACHMENT_BIZ_DOMAIN_TRANSFER)
                .eq(ArchiveAttachment::getApplicationId, applicationId)
                .eq(ArchiveAttachment::getApplicationDetailId, detailId)
                .eq(ArchiveAttachment::getDeleteFlag, "N")
                .orderByDesc(ArchiveAttachment::getCreationDate))
            .stream()
            .map(this::toAttachmentResponse)
            .toList();
    }

    @Override
    public Resource downloadDetailAttachment(Long applicationId, Long detailId, Long attachmentId) {
        requireApplication(applicationId);
        requireDetail(applicationId, detailId);
        ArchiveAttachment entity = archiveAttachmentMapper.selectOne(
            new LambdaQueryWrapper<ArchiveAttachment>()
                .eq(ArchiveAttachment::getAttachmentId, attachmentId)
                .eq(ArchiveAttachment::getBizDomain, ATTACHMENT_BIZ_DOMAIN_TRANSFER)
                .eq(ArchiveAttachment::getApplicationId, applicationId)
                .eq(ArchiveAttachment::getApplicationDetailId, detailId)
                .eq(ArchiveAttachment::getDeleteFlag, "N")
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException("附件不存在");
        }
        Path path = Paths.get(entity.getStoragePath());
        if (!Files.exists(path)) {
            throw new BusinessException("附件文件不存在");
        }
        return new FileSystemResource(path.toFile()) {
            @Override
            public String getFilename() {
                return entity.getFileName();
            }
        };
    }

    private TransferApplication requireApplication(Long applicationId) {
        TransferApplication application = transferApplicationMapper.selectOne(new LambdaQueryWrapper<TransferApplication>()
            .eq(TransferApplication::getApplicationId, applicationId)
            .eq(TransferApplication::getDeleteFlag, "N")
            .last("limit 1"));
        if (application == null) {
            throw new BusinessException("Transfer application does not exist");
        }
        return application;
    }

    private TransferApplicationDetail requireDetail(Long applicationId, Long detailId) {
        TransferApplicationDetail detail = transferApplicationDetailMapper.selectOne(new LambdaQueryWrapper<TransferApplicationDetail>()
            .eq(TransferApplicationDetail::getApplicationDetailId, detailId)
            .eq(TransferApplicationDetail::getApplicationId, applicationId)
            .eq(TransferApplicationDetail::getDeleteFlag, "N")
            .last("limit 1"));
        if (detail == null) {
            throw new BusinessException("申请行不存在");
        }
        return detail;
    }

    private void ensureApplicationNumberAvailable(String applicationNumber, Long tenantid) {
        Long count = transferApplicationMapper.selectCount(new LambdaQueryWrapper<TransferApplication>()
            .eq(TransferApplication::getApplicationNumber, applicationNumber)
            .eq(TransferApplication::getTenantid, tenantid)
            .eq(TransferApplication::getDeleteFlag, "N"));
        if (count != null && count > 0) {
            throw new BusinessException("applicationNumber already exists under current tenant");
        }
    }

    private List<TransferApplicationDetailResponse> loadDetails(Long applicationId, Long tenantid) {
        List<TransferApplicationDetail> details = transferApplicationDetailMapper.selectList(new LambdaQueryWrapper<TransferApplicationDetail>()
                .eq(TransferApplicationDetail::getApplicationId, applicationId)
                .eq(TransferApplicationDetail::getDeleteFlag, "N")
                .orderByAsc(TransferApplicationDetail::getApplicationDetailId));
        Map<Long, List<TransferApplicationExtValueResponse>> detailExtValues =
            loadExtValuesByDetail(applicationId, tenantid, details);
        Map<Long, List<TransferApplicationDetailAttachmentResponse>> detailAttachments = loadAttachmentsByDetail(applicationId);
        return details.stream()
            .map(item -> TransferApplicationDetailResponse.builder()
                .applicationDetailId(item.getApplicationDetailId())
                .applicationId(item.getApplicationId())
                .docBusiNo(item.getDocBusiNo())
                .docName(item.getDocName())
                .busiModuleCode(item.getBusiModuleCode())
                .companyProjectCode(item.getCompanyProjectCode())
                .archPlaceAlpha2Code(item.getArchPlaceAlpha2Code())
                .documentOrganizationCode(item.getDocumentOrganizationCode())
                .endArchPeriod(item.getEndArchPeriod())
                .startArchPeriod(item.getStartArchPeriod())
                .archTypeCode(item.getArchTypeCode())
                .carrierType(item.getCarrierType())
                .docGenerationDate(item.getDocGenerationDate())
                .archCopies(item.getArchCopies())
                .remark(item.getRemark())
                .description(item.getDescription())
                .catalogVolumeNo(item.getCatalogVolumeNo())
                .busiVolumeNo(item.getBusiVolumeNo())
                .extValues(detailExtValues.getOrDefault(item.getApplicationDetailId(), List.of()))
                .attachments(detailAttachments.getOrDefault(item.getApplicationDetailId(), List.of()))
                .build())
            .toList();
    }

    private Map<Long, List<TransferApplicationDetailAttachmentResponse>> loadAttachmentsByDetail(Long applicationId) {
        List<ArchiveAttachment> rows = archiveAttachmentMapper.selectList(
            new LambdaQueryWrapper<ArchiveAttachment>()
                .eq(ArchiveAttachment::getBizDomain, ATTACHMENT_BIZ_DOMAIN_TRANSFER)
                .eq(ArchiveAttachment::getApplicationId, applicationId)
                .eq(ArchiveAttachment::getDeleteFlag, "N")
                .orderByDesc(ArchiveAttachment::getCreationDate)
        );
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<TransferApplicationDetailAttachmentResponse>> result = new HashMap<>();
        for (ArchiveAttachment row : rows) {
            result.computeIfAbsent(row.getApplicationDetailId(), k -> new ArrayList<>()).add(toAttachmentResponse(row));
        }
        return result;
    }

    private TransferApplicationDetailAttachmentResponse toAttachmentResponse(ArchiveAttachment row) {
        return TransferApplicationDetailAttachmentResponse.builder()
            .attachmentId(row.getAttachmentId())
            .applicationId(row.getApplicationId())
            .applicationDetailId(row.getApplicationDetailId())
            .fileName(row.getFileName())
            .mimeType(row.getMimeType())
            .fileSize(row.getFileSize())
            .remark(row.getRemark())
            .creationDate(row.getCreationDate())
            .build();
    }

    private String extractFileExt(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex + 1).trim().toLowerCase(Locale.ROOT);
    }

    private void validateHeader(TransferApplicationCreateCommand command) {
        String applyMethod = requireText(command.getApplyMethod(), "applyMethod");
        if ("MAIL".equalsIgnoreCase(applyMethod)) {
            requireText(command.getExpressType(), "expressType");
            requireText(command.getExpressNumber(), "expressNumber");
        }
        if (command.getDocumentRecipient() == null) {
            throw new BusinessException("documentRecipient cannot be null");
        }
    }

    private void validateDetail(TransferApplicationDetailCommand detail) {
        if (detail.getArchCopies() == null || detail.getArchCopies().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("archCopies must be greater than 0");
        }
    }

    private boolean requiresBusiVolumeNo(String handoverForm) {
        String normalized = trimToNull(handoverForm);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        // 兼容字典编码与名称两种传值
        return upper.contains("VOLUME") || normalized.contains("按册");
    }

    private void saveExtValues(TransferApplication application,
                               TransferApplicationDetail detail,
                               List<TransferApplicationExtValueCommand> extValues,
                               Map<String, DocumentTypeExtFieldResponse> configByFieldCode) {
        Map<String, String> inputMap = new HashMap<>();
        if (extValues != null) {
            for (TransferApplicationExtValueCommand extValue : extValues) {
                String fieldCode = requireText(extValue.getFieldCode(), "ext fieldCode");
                inputMap.put(fieldCode, trimToNull(extValue.getValue()));
            }
        }
        for (DocumentTypeExtFieldResponse config : configByFieldCode.values()) {
            if ("Y".equalsIgnoreCase(config.getRequiredFlag()) && !StringUtils.hasText(inputMap.get(config.getFieldCode()))) {
                throw new BusinessException("required ext field missing: " + config.getFieldName());
            }
        }
        Map<String, Object> attrValues = new HashMap<>();
        for (Map.Entry<String, String> entry : inputMap.entrySet()) {
            DocumentTypeExtFieldResponse config = configByFieldCode.get(entry.getKey());
            if (config == null) {
                throw new BusinessException("unknown ext fieldCode: " + entry.getKey());
            }
            String columnName = normalizeExtColumn(config.getDictCategoryCode(), entry.getKey());
            Object storedValue = convertExtValue(columnName, entry.getValue(), config.getFieldName());
            attrValues.put(columnName, storedValue);
        }
        if (!attrValues.isEmpty()) {
            transferApplicationExtMapper.insertExtRow(
                application.getApplicationId(),
                detail.getApplicationDetailId(),
                application.getTenantid(),
                SYSTEM_OPERATOR_ID,
                attrValues
            );
        }
    }

    private Map<Long, List<TransferApplicationExtValueResponse>> loadExtValuesByDetail(Long applicationId,
                                                                                        Long tenantid,
                                                                                        List<TransferApplicationDetail> details) {
        List<Map<String, Object>> extRows = transferApplicationExtMapper.selectByMasterId(applicationId, tenantid);
        if (extRows.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> detailIdToModule = details.stream()
            .filter(d -> d.getApplicationDetailId() != null && StringUtils.hasText(d.getBusiModuleCode()))
            .collect(Collectors.toMap(
                TransferApplicationDetail::getApplicationDetailId,
                d -> d.getBusiModuleCode().trim(),
                (left, right) -> left));
        Map<Long, List<TransferApplicationExtValueResponse>> result = new HashMap<>();
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
            List<TransferApplicationExtValueResponse> values = result.computeIfAbsent(detailId, k -> new ArrayList<>());
            for (Map.Entry<String, String> mapEntry : columnToFieldCode.entrySet()) {
                Object raw = row.get(mapEntry.getKey());
                if (raw == null) {
                    continue;
                }
                values.add(TransferApplicationExtValueResponse.builder()
                    .fieldCode(mapEntry.getValue())
                    .value(String.valueOf(raw))
                    .build());
            }
        }
        return result;
    }

    private String normalizeExtColumn(String dictCategoryCode, String fieldCode) {
        String columnName = requireText(dictCategoryCode, "dictCategoryCode for " + fieldCode).toLowerCase();
        Matcher matcher = ATTR_COLUMN_PATTERN.matcher(columnName);
        if (!matcher.matches()) {
            throw new BusinessException("dictCategoryCode must be attr1-attr300 for " + fieldCode);
        }
        int index = Integer.parseInt(matcher.group(1));
        if (index < 1 || index > 300) {
            throw new BusinessException("dictCategoryCode out of range for " + fieldCode);
        }
        return columnName;
    }

    private Object convertExtValue(String columnName, String rawValue, String fieldName) {
        if (!StringUtils.hasText(rawValue)) {
            return null;
        }
        int index = Integer.parseInt(columnName.substring(4));
        String value = rawValue.trim();
        if (index <= 100) {
            return value;
        }
        if (index <= 200) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException ex) {
                throw new BusinessException("ext field is numeric: " + fieldName);
            }
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("ext field is date(yyyy-MM-dd): " + fieldName);
        }
    }

    private TransferApplicationResponse toResponse(TransferApplication item, List<TransferApplicationDetailResponse> details) {
        Set<Long> ids = new HashSet<>();
        if (item.getApplicant() != null && item.getApplicant() > 0) ids.add(item.getApplicant());
        if (item.getDocumentRecipient() != null && item.getDocumentRecipient() > 0) ids.add(item.getDocumentRecipient());
        Map<Long, String> userDisplayMap = loadUserDisplayMap(ids);
        return TransferApplicationResponse.builder()
            .applicationId(item.getApplicationId())
            .applicationNumber(item.getApplicationNumber())
            .applicant(item.getApplicant())
            .applicantName(formatUserDisplay(item.getApplicant(), userDisplayMap))
            .applicationDate(item.getApplicationDate())
            .department(item.getDepartment())
            .busiModuleCode(item.getBusiModuleCode())
            .applyMethod(item.getApplyMethod())
            .expressType(item.getExpressType())
            .expressNumber(item.getExpressNumber())
            .documentRecipient(item.getDocumentRecipient())
            .documentRecipientName(formatUserDisplay(item.getDocumentRecipient(), userDisplayMap))
            .handoverForm(item.getHandoverForm())
            .carrierType(item.getCarrierType())
            .applicationStatus(item.getApplicationStatus())
            .status(item.getStatus())
            .diffReasonCode(item.getDiffReasonCode())
            .applicationDescription(item.getApplicationDescription())
            .enableFlag(item.getEnableFlag())
            .deleteFlag(item.getDeleteFlag())
            .tenantid(item.getTenantid())
            .creationDate(item.getCreationDate())
            .lastUpdateDate(item.getLastUpdateDate())
            .archivesMaterialized(item.getArchivesMaterialized())
            .details(details)
            .build();
    }

    private void fillApplicationFromCommand(TransferApplication application,
                                            TransferApplicationCreateCommand command,
                                            String applicationNumber,
                                            LocalDateTime now) {
        application.setApplicationNumber(applicationNumber);
        application.setApplicant(command.getApplicant());
        application.setApplicationDate(now);
        application.setDepartment(trimToNull(command.getDepartment()));
        application.setBusiModuleCode(requireText(command.getBusiModuleCode(), "busiModuleCode"));
        application.setApplyMethod(requireText(command.getApplyMethod(), "applyMethod"));
        application.setExpressType(trimToNull(command.getExpressType()));
        application.setExpressNumber(trimToNull(command.getExpressNumber()));
        application.setDocumentRecipient(command.getDocumentRecipient());
        application.setHandoverForm(trimToNull(command.getHandoverForm()));
        application.setCarrierType(null);
        String status = trimToNull(command.getApplicationStatus());
        application.setApplicationStatus(status);
        application.setStatus(status);
        application.setDiffReasonCode(trimToNull(command.getDiffReasonCode()));
        application.setApplicationDescription(trimToNull(command.getApplicationDescription()));
        application.setSysDescription(trimToNull(command.getSysDescription()));
        application.setTenantid(command.getTenantid());
    }

    private void replaceDetails(TransferApplication application, List<TransferApplicationDetailCommand> details, LocalDateTime now) {
        if (details == null) {
            return;
        }
        boolean requireBusiVolumeNo = requiresBusiVolumeNo(application.getHandoverForm());
        for (TransferApplicationDetailCommand item : details) {
            validateDetail(item);
            if (requireBusiVolumeNo && !StringUtils.hasText(item.getBusiVolumeNo())) {
                throw new BusinessException("按册移交时业务册号必填");
            }
            String rowBusi = trimToNull(item.getBusiModuleCode());
            Map<String, DocumentTypeExtFieldResponse> extFieldConfigMap = rowBusi == null
                ? Map.of()
                : transferBusinessModuleExtFieldService.asConfigMap(rowBusi);
            TransferApplicationDetail detail = new TransferApplicationDetail();
            detail.setApplicationId(application.getApplicationId());
            detail.setDocBusiNo(trimToNull(item.getDocBusiNo()));
            detail.setDocName(trimToNull(item.getDocName()));
            detail.setBusiModuleCode(trimToNull(item.getBusiModuleCode()));
            detail.setCompanyProjectCode(trimToNull(item.getCompanyProjectCode()));
            detail.setArchPlaceAlpha2Code(trimToNull(item.getArchPlaceAlpha2Code()));
            detail.setDocumentOrganizationCode(trimToNull(item.getDocumentOrganizationCode()));
            detail.setEndArchPeriod(item.getEndArchPeriod());
            detail.setStartArchPeriod(item.getStartArchPeriod());
            detail.setArchTypeCode(trimToNull(item.getArchTypeCode()));
            detail.setCarrierType(trimToNull(item.getCarrierType()));
            detail.setDocGenerationDate(item.getDocGenerationDate());
            detail.setArchCopies(item.getArchCopies());
            detail.setRemark(trimToNull(item.getRemark()));
            detail.setDescription(trimToNull(item.getDescription()));
            detail.setCatalogVolumeNo(trimToNull(item.getCatalogVolumeNo()));
            detail.setBusiVolumeNo(trimToNull(item.getBusiVolumeNo()));
            detail.setEnableFlag("Y");
            detail.setDeleteFlag("N");
            detail.setCreatedBy(SYSTEM_OPERATOR_ID);
            detail.setCreationDate(now);
            detail.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            detail.setLastUpdateDate(now);
            detail.setSysDescription(null);
            detail.setLastUpdateTraceId(null);
            detail.setLastUpdateVersion(0);
            detail.setTenantid(application.getTenantid());
            transferApplicationDetailMapper.insert(detail);
            saveExtValues(application, detail, item.getExtValues(), extFieldConfigMap);
        }
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean isSubmittedForWorkflow(TransferApplicationCreateCommand command) {
        String s = command.getApplicationStatus();
        return s != null && "SUBMITTED".equalsIgnoreCase(s.trim());
    }

    private void afterPersistStartWorkflowIfSubmitted(TransferApplication application, TransferApplicationCreateCommand command) {
        if (!isSubmittedForWorkflow(command) || application.getDocumentRecipient() == null) {
            return;
        }
        String businessKey = "TRN-APP-" + application.getApplicationId();
        Long running = workflowInstanceMapper.selectCount(new LambdaQueryWrapper<WorkflowInstance>()
            .eq(WorkflowInstance::getBusinessType, "TRANSFER_APPLICATION")
            .eq(WorkflowInstance::getBusinessKey, businessKey)
            .eq(WorkflowInstance::getStatus, "RUNNING"));
        if (running != null && running > 0) {
            return;
        }
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("assigneeId", String.valueOf(application.getDocumentRecipient()));
        variables.put("applicationId", application.getApplicationId());
        Set<Long> ids = new HashSet<>();
        if (application.getDocumentRecipient() != null && application.getDocumentRecipient() > 0) ids.add(application.getDocumentRecipient());
        Long applicant = application.getApplicant() != null ? application.getApplicant() : SYSTEM_OPERATOR_ID;
        if (applicant > 0) ids.add(applicant);
        Map<Long, String> userDisplayMap = loadUserDisplayMap(ids);
        variables.put("assigneeName", formatUserDisplay(application.getDocumentRecipient(), userDisplayMap));
        variables.put("initiatorId", String.valueOf(applicant));
        variables.put("initiatorName", formatUserDisplay(applicant, userDisplayMap));
        variables.put("applicationNumber", application.getApplicationNumber());
        variables.put("transferMode", "TRANSFER_APPLICATION");

        StartProcessCommand start = new StartProcessCommand();
        start.setProcessDefinitionKey("documentTransfer");
        start.setBusinessKey(businessKey);
        start.setBusinessType("TRANSFER_APPLICATION");
        start.setBusinessId(application.getApplicationId());
        start.setInitiatorId(String.valueOf(applicant));
        start.setInitiatorName(formatUserDisplay(applicant, userDisplayMap));
        start.setVariables(variables);
        workflowService.startProcess(start);
    }
}
