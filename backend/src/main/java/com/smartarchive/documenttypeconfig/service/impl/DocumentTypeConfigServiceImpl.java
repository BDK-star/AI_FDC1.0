package com.smartarchive.documenttypeconfig.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartarchive.businessmodule.domain.BusinessModule;
import com.smartarchive.businessmodule.mapper.BusinessModuleMapper;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.documenttypeconfig.domain.DocumentTypeConfig;
import com.smartarchive.documenttypeconfig.dto.DocumentTypeConfigQueryCommand;
import com.smartarchive.documenttypeconfig.dto.DocumentTypeConfigResponse;
import com.smartarchive.documenttypeconfig.dto.DocumentTypeConfigSaveCommand;
import com.smartarchive.documenttypeconfig.mapper.DocumentTypeConfigMapper;
import com.smartarchive.documenttypeconfig.service.DocumentTypeConfigService;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DocumentTypeConfigServiceImpl implements DocumentTypeConfigService {
    private static final Long SYSTEM_OPERATOR_ID = 1L;
    private static final Long DEFAULT_TENANT_ID = 1L;
    private static final String DEFAULT_SECURITY_LEVEL_CODE = "INTERNAL_PUBLIC";

    private final DocumentTypeConfigMapper mapper;
    private final BusinessModuleMapper businessModuleMapper;

    @Override
    public List<DocumentTypeConfigResponse> list(DocumentTypeConfigQueryCommand command) {
        LambdaQueryWrapper<DocumentTypeConfig> wrapper = new LambdaQueryWrapper<DocumentTypeConfig>()
            .eq(DocumentTypeConfig::getTenantid, normalizeTenant(command.getTenantid()))
            .eq(DocumentTypeConfig::getDeleteFlag, "N")
            .isNull(DocumentTypeConfig::getParentCode)
            .eq(DocumentTypeConfig::getLevelNum, 1)
            .orderByDesc(DocumentTypeConfig::getLastUpdateDate)
            .orderByDesc(DocumentTypeConfig::getDocumentTypeId);
        if (StringUtils.hasText(command.getDocTypeCode())) {
            wrapper.like(DocumentTypeConfig::getDocTypeCode, command.getDocTypeCode().trim());
        }
        if (StringUtils.hasText(command.getDocTypeDescription())) {
            wrapper.like(DocumentTypeConfig::getDocTypeDescription, command.getDocTypeDescription().trim());
        }
        if (StringUtils.hasText(command.getEnableFlag())) {
            wrapper.eq(DocumentTypeConfig::getEnableFlag, command.getEnableFlag().trim().toUpperCase());
        }
        return mapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public DocumentTypeConfigResponse create(DocumentTypeConfigSaveCommand command) {
        Long tenantId = normalizeTenant(command.getTenantid());
        String code = requireText(command.getDocTypeCode(), "docTypeCode");
        ensureCodeUnique(code, tenantId, null);
        DocumentTypeConfig entity = new DocumentTypeConfig();
        fillEntity(entity, command, tenantId, true);
        mapper.insert(entity);
        syncRootBusinessModule(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional
    public DocumentTypeConfigResponse update(Long documentTypeId, DocumentTypeConfigSaveCommand command) {
        Long tenantId = normalizeTenant(command.getTenantid());
        DocumentTypeConfig entity = requireById(documentTypeId, tenantId);
        String code = requireText(command.getDocTypeCode(), "docTypeCode");
        ensureCodeUnique(code, tenantId, documentTypeId);
        fillEntity(entity, command, tenantId, false);
        mapper.updateById(entity);
        syncRootBusinessModule(entity);
        return toResponse(entity);
    }

    @Override
    public byte[] exportCsv(DocumentTypeConfigQueryCommand command) {
        StringBuilder builder = new StringBuilder();
        builder.append("docTypeCode,docTypeDescription,enableFlag\n");
        for (DocumentTypeConfigResponse item : list(command)) {
            builder.append(csvCell(item.getDocTypeCode())).append(',')
                .append(csvCell(item.getDocTypeDescription())).append(',')
                .append(csvCell(item.getEnableFlag())).append('\n');
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportExcel(DocumentTypeConfigQueryCommand command) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("document-type-config");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("docTypeCode");
            header.createCell(1).setCellValue("docTypeDescription");
            header.createCell(2).setCellValue("enableFlag");
            int index = 1;
            for (DocumentTypeConfigResponse item : list(command)) {
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(nullSafe(item.getDocTypeCode()));
                row.createCell(1).setCellValue(nullSafe(item.getDocTypeDescription()));
                row.createCell(2).setCellValue(nullSafe(item.getEnableFlag()));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException("导出Excel失败");
        }
    }

    @Override
    @Transactional
    public Integer importCsv(InputStream inputStream, Long tenantid) {
        Long tenantId = normalizeTenant(tenantid);
        int importedRows = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line == null) {
                return 0;
            }
            while ((line = reader.readLine()) != null) {
                if (!StringUtils.hasText(line)) {
                    continue;
                }
                String[] cols = parseCsvLine(line);
                if (cols.length < 2) {
                    continue;
                }
                saveOrUpdateByImport(safeCol(cols, 0), safeCol(cols, 1), safeCol(cols, 2), tenantId);
                importedRows++;
            }
        } catch (IOException ex) {
            throw new BusinessException("导入CSV读取失败");
        }
        return importedRows;
    }

    @Override
    @Transactional
    public Integer importExcel(InputStream inputStream, Long tenantid) {
        Long tenantId = normalizeTenant(tenantid);
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            int importedRows = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String code = readCell(row.getCell(0));
                String description = readCell(row.getCell(1));
                String enableFlag = readCell(row.getCell(2));
                if (!StringUtils.hasText(code) || !StringUtils.hasText(description)) {
                    continue;
                }
                saveOrUpdateByImport(code, description, enableFlag, tenantId);
                importedRows++;
            }
            return importedRows;
        } catch (IOException ex) {
            throw new BusinessException("导入Excel读取失败");
        }
    }

    private void saveOrUpdateByImport(String code, String description, String enableFlag, Long tenantId) {
        String normalizedCode = requireText(code, "docTypeCode");
        String normalizedDescription = requireText(description, "docTypeDescription");
        String normalizedEnableFlag = normalizeFlag(enableFlag, "Y");
        DocumentTypeConfig existing = mapper.selectOne(new LambdaQueryWrapper<DocumentTypeConfig>()
            .eq(DocumentTypeConfig::getTenantid, tenantId)
            .eq(DocumentTypeConfig::getDocTypeCode, normalizedCode)
            .eq(DocumentTypeConfig::getDeleteFlag, "N")
            .last("limit 1"));
        if (existing == null) {
            DocumentTypeConfigSaveCommand command = new DocumentTypeConfigSaveCommand();
            command.setDocTypeCode(normalizedCode);
            command.setDocTypeDescription(normalizedDescription);
            command.setEnableFlag(normalizedEnableFlag);
            command.setTenantid(tenantId);
            create(command);
            return;
        }
        DocumentTypeConfigSaveCommand command = new DocumentTypeConfigSaveCommand();
        command.setDocTypeCode(normalizedCode);
        command.setDocTypeDescription(normalizedDescription);
        command.setEnableFlag(normalizedEnableFlag);
        command.setTenantid(tenantId);
        update(existing.getDocumentTypeId(), command);
    }

    private void fillEntity(DocumentTypeConfig entity, DocumentTypeConfigSaveCommand command, Long tenantId, boolean creating) {
        entity.setDocTypeCode(requireText(command.getDocTypeCode(), "docTypeCode"));
        entity.setDocTypeDescription(requireText(command.getDocTypeDescription(), "docTypeDescription"));
        entity.setParentCode(null);
        entity.setLevelNum(1);
        entity.setAncestorPath("");
        entity.setSortOrder(entity.getSortOrder() == null || entity.getSortOrder() <= 0 ? nextRootSortOrder() : entity.getSortOrder());
        entity.setEnableFlag(normalizeFlag(command.getEnableFlag(), "Y"));
        entity.setDeleteFlag("N");
        entity.setTenantid(tenantId);
        if (creating) {
            entity.setCreatedBy(SYSTEM_OPERATOR_ID);
            entity.setCreationDate(LocalDateTime.now());
            entity.setLastUpdateVersion(0);
        }
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(LocalDateTime.now());
    }

    private Integer nextRootSortOrder() {
        Long count = mapper.selectCount(new LambdaQueryWrapper<DocumentTypeConfig>()
            .eq(DocumentTypeConfig::getDeleteFlag, "N")
            .isNull(DocumentTypeConfig::getParentCode)
            .eq(DocumentTypeConfig::getLevelNum, 1));
        return count.intValue() + 1;
    }

    private void syncRootBusinessModule(DocumentTypeConfig docType) {
        String code = requireText(docType.getDocTypeCode(), "docTypeCode");
        String name = requireText(docType.getDocTypeDescription(), "docTypeDescription");
        BusinessModule existing = businessModuleMapper.selectOne(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getModuleCode, code)
            .eq(BusinessModule::getDeleteFlag, "N")
            .last("limit 1"));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            BusinessModule entity = new BusinessModule();
            entity.setModuleCode(code);
            entity.setModuleName(name);
            entity.setParentCode(null);
            entity.setLevelNum(1);
            entity.setAncestorPath("");
            entity.setEnabledFlag(normalizeFlag(docType.getEnableFlag(), "Y"));
            entity.setSecurityLevel(DEFAULT_SECURITY_LEVEL_CODE);
            entity.setIntegrationType("不集成");
            entity.setDescription(name);
            entity.setRemark(null);
            entity.setSortOrder(docType.getSortOrder() == null ? 1 : docType.getSortOrder());
            entity.setDeleteFlag("N");
            entity.setCreatedBy(SYSTEM_OPERATOR_ID);
            entity.setCreationDate(now);
            entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            entity.setLastUpdateDate(now);
            businessModuleMapper.insert(entity);
            return;
        }
        businessModuleMapper.updateById(buildRootModuleUpdate(existing, docType, name, now));
    }

    private BusinessModule buildRootModuleUpdate(BusinessModule existing, DocumentTypeConfig docType, String name, LocalDateTime now) {
        existing.setModuleName(name);
        existing.setParentCode(null);
        existing.setLevelNum(1);
        existing.setAncestorPath("");
        existing.setEnabledFlag(normalizeFlag(docType.getEnableFlag(), "Y"));
        existing.setSortOrder(docType.getSortOrder() == null ? existing.getSortOrder() : docType.getSortOrder());
        if (!StringUtils.hasText(existing.getSecurityLevel())) {
            existing.setSecurityLevel(DEFAULT_SECURITY_LEVEL_CODE);
        }
        if (!StringUtils.hasText(existing.getIntegrationType())) {
            existing.setIntegrationType("不集成");
        }
        if (!StringUtils.hasText(existing.getDescription())) {
            existing.setDescription(name);
        }
        existing.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        existing.setLastUpdateDate(now);
        return existing;
    }

    private void ensureCodeUnique(String code, Long tenantId, Long ignoreId) {
        DocumentTypeConfig existing = mapper.selectOne(new LambdaQueryWrapper<DocumentTypeConfig>()
            .eq(DocumentTypeConfig::getTenantid, tenantId)
            .eq(DocumentTypeConfig::getDocTypeCode, code)
            .eq(DocumentTypeConfig::getDeleteFlag, "N")
            .last("limit 1"));
        if (existing != null && !existing.getDocumentTypeId().equals(ignoreId)) {
            throw new BusinessException("文档类型编码已存在");
        }
    }

    private DocumentTypeConfig requireById(Long documentTypeId, Long tenantId) {
        DocumentTypeConfig entity = mapper.selectOne(new LambdaQueryWrapper<DocumentTypeConfig>()
            .eq(DocumentTypeConfig::getDocumentTypeId, documentTypeId)
            .eq(DocumentTypeConfig::getTenantid, tenantId)
            .eq(DocumentTypeConfig::getDeleteFlag, "N")
            .last("limit 1"));
        if (entity == null) {
            throw new BusinessException("文档类型配置不存在");
        }
        return entity;
    }

    private String requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(name + "不能为空");
        }
        return value.trim();
    }

    private Long normalizeTenant(Long tenantid) {
        return tenantid == null ? DEFAULT_TENANT_ID : tenantid;
    }

    private String normalizeFlag(String flag, String defaultValue) {
        if (!StringUtils.hasText(flag)) {
            return defaultValue;
        }
        String normalized = flag.trim().toUpperCase();
        if (!List.of("Y", "N").contains(normalized)) {
            throw new BusinessException("标识字段仅支持Y/N");
        }
        return normalized;
    }

    private DocumentTypeConfigResponse toResponse(DocumentTypeConfig item) {
        return DocumentTypeConfigResponse.builder()
            .documentTypeId(item.getDocumentTypeId())
            .docTypeCode(item.getDocTypeCode())
            .docTypeDescription(item.getDocTypeDescription())
            .enableFlag(item.getEnableFlag())
            .createdBy(item.getCreatedBy())
            .creationDate(item.getCreationDate())
            .lastUpdatedBy(item.getLastUpdatedBy())
            .lastUpdateDate(item.getLastUpdateDate())
            .build();
    }

    private String csvCell(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    private String safeCol(String[] cols, int idx) {
        if (idx >= cols.length) {
            return null;
        }
        return cols[idx];
    }

    private String readCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return cell.getStringCellValue();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
