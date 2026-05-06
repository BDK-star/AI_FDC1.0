package com.smartarchive.businessmodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartarchive.businessmodule.domain.BusinessModule;
import com.smartarchive.businessmodule.domain.BusinessModuleExtField;
import com.smartarchive.businessmodule.dto.BusinessModuleCommand;
import com.smartarchive.businessmodule.dto.BusinessModuleExtFieldCommand;
import com.smartarchive.businessmodule.dto.BusinessModuleExtFieldResponse;
import com.smartarchive.businessmodule.dto.BusinessModuleNodeResponse;
import com.smartarchive.businessmodule.dto.BusinessModuleParentOptionResponse;
import com.smartarchive.businessmodule.dto.BusinessModuleUpdateCommand;
import com.smartarchive.barcodemodule.domain.BarcodeModule;
import com.smartarchive.barcodemodule.mapper.BarcodeModuleMapper;
import com.smartarchive.businessmodule.mapper.BusinessModuleExtFieldMapper;
import com.smartarchive.businessmodule.mapper.BusinessModuleMapper;
import com.smartarchive.businessmodule.service.BusinessModuleService;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.dictionary.domain.DictionaryItem;
import com.smartarchive.dictionary.mapper.DictionaryItemMapper;
import com.smartarchive.documenttypeconfig.domain.DocumentTypeConfig;
import com.smartarchive.documenttypeconfig.mapper.DocumentTypeConfigMapper;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.stream.IntStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BusinessModuleServiceImpl implements BusinessModuleService {
    private static final Long SYSTEM_OPERATOR_ID = 1L;
    private static final int MAX_LEVEL = 6;
    private static final String SECURITY_LEVEL_CATEGORY_CODE = "SECURITY_LEVEL";
    private static final String DEFAULT_SECURITY_LEVEL_CODE = "INTERNAL_PUBLIC";
    private static final List<String> SUPPORTED_APPLICATION_FUNCTIONS = List.of("应归档数据", "移交");
    private static final Set<String> BASIC_TEXT_ATTRIBUTES = buildAttributePool("ATTR", 1, 50);
    private static final Set<String> BASIC_NUMBER_ATTRIBUTES = buildAttributePool("ATTR", 51, 80);
    private static final Set<String> BASIC_DATE_ATTRIBUTES = buildAttributePool("ATTR", 81, 90);
    private static final Set<String> BASIC_DATETIME_ATTRIBUTES = buildAttributePool("ATTR", 91, 100);
    private static final Set<String> ATTACHMENT_TEXT_ATTRIBUTES = buildAttributePool("ATTRIBUTE", 1, 20);
    private static final Set<String> ATTACHMENT_NUMBER_ATTRIBUTES = buildAttributePool("ATTRIBUTE", 21, 30);
    private static final Set<String> ATTACHMENT_DATE_ATTRIBUTES = buildAttributePool("ATTRIBUTE", 31, 40);
    private static final Set<String> ATTACHMENT_DATETIME_ATTRIBUTES = buildAttributePool("ATTRIBUTE", 41, 50);

    private final BusinessModuleMapper businessModuleMapper;
    private final BusinessModuleExtFieldMapper extFieldMapper;
    private final DocumentTypeConfigMapper documentTypeConfigMapper;
    private final DictionaryItemMapper dictionaryItemMapper;
    private final BarcodeModuleMapper barcodeModuleMapper;

    @Override
    public List<BusinessModuleNodeResponse> listTree() {
        syncDocumentTypesToBusinessModules();
        SecurityLevelDictionarySnapshot securityLevelSnapshot = loadSecurityLevelSnapshot();
        List<BusinessModule> modules = businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getDeleteFlag, "N")
                .orderByAsc(BusinessModule::getLevelNum)
                .orderByAsc(BusinessModule::getSortOrder)
                .orderByAsc(BusinessModule::getModuleCode));
        Map<String, BarcodeModule> barcodeByCode = loadBarcodeMapByCodes(
            modules.stream().map(BusinessModule::getBarcodeModuleCode).filter(StringUtils::hasText).toList());
        Map<String, BusinessModuleNodeResponse> map = modules.stream()
                .map(module -> toNode(module, securityLevelSnapshot, barcodeByCode))
                .collect(Collectors.toMap(BusinessModuleNodeResponse::getModuleCode, Function.identity(), (a, b) -> a));
        List<BusinessModuleNodeResponse> roots = new ArrayList<>();
        for (BusinessModule module : modules) {
            BusinessModuleNodeResponse node = map.get(module.getModuleCode());
            if (!StringUtils.hasText(module.getParentCode()) || !map.containsKey(module.getParentCode())) {
                roots.add(node);
            } else {
                map.get(module.getParentCode()).getChildren().add(node);
            }
        }
        sortTree(roots);
        return roots;
    }

    @Override
    public List<BusinessModuleParentOptionResponse> listParentOptions() {
        syncDocumentTypesToBusinessModules();
        Map<String, BusinessModuleParentOptionResponse> optionMap = new LinkedHashMap<>();
        List<BusinessModule> businessModules = businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getDeleteFlag, "N")
                .orderByAsc(BusinessModule::getLevelNum)
                .orderByAsc(BusinessModule::getSortOrder)
                .orderByAsc(BusinessModule::getModuleCode));
        businessModules.forEach(module -> optionMap.putIfAbsent(module.getModuleCode(), toParentOption(module)));

        List<DocumentTypeConfig> documentTypes = documentTypeConfigMapper.selectList(new LambdaQueryWrapper<DocumentTypeConfig>()
                .eq(DocumentTypeConfig::getDeleteFlag, "N")
                .isNull(DocumentTypeConfig::getParentCode)
                .eq(DocumentTypeConfig::getLevelNum, 1)
                .orderByAsc(DocumentTypeConfig::getDocTypeCode));
        documentTypes.forEach(type -> optionMap.putIfAbsent(type.getDocTypeCode(), toParentOption(type)));

        return new ArrayList<>(optionMap.values());
    }

    @Override
    @Transactional
    public BusinessModuleNodeResponse create(BusinessModuleCommand command) {
        ensureCodeAvailable(command.getModuleCode().trim());
        TreeMeta meta = resolveMeta(command.getParentCode(), null);
        BusinessModule entity = new BusinessModule();
        entity.setModuleCode(command.getModuleCode().trim());
        entity.setModuleName(command.getModuleName().trim());
        entity.setParentCode(trimToNull(command.getParentCode()));
        entity.setLevelNum(meta.levelNum());
        entity.setAncestorPath(meta.ancestorPath());
        entity.setEnabledFlag(normalizeFlag(command.getEnabledFlag(), "Y"));
        entity.setSortOrder(command.getSortOrder() == null ? nextSortOrder(command.getParentCode()) : command.getSortOrder());
        entity.setSecurityLevel(normalizeSecurityLevelCode(command.getSecurityLevelCode()));
        entity.setIntegrationType(normalizeIntegrationType(command.getIntegrationType()));
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setRemark(trimToNull(command.getRemark()));
        String barcodeRef = normalizeBarcodeModuleRef(command.getBarcodeModuleCode());
        assertBarcodeModuleAssignable(barcodeRef);
        assertBarcodeOnlyForLeafModule(command.getModuleCode().trim(), barcodeRef);
        entity.setBarcodeModuleCode(barcodeRef);
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(LocalDateTime.now());
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(LocalDateTime.now());
        businessModuleMapper.insert(entity);
        if (StringUtils.hasText(entity.getParentCode())) {
            clearBarcodeWhenModuleHasChildren(entity.getParentCode().trim());
        }
        return toNode(entity, loadSecurityLevelSnapshot(), loadBarcodeMapByCodes(toBarcodeCodeList(entity.getBarcodeModuleCode())));
    }

    @Override
    @Transactional
    public BusinessModuleNodeResponse update(String moduleCode, BusinessModuleUpdateCommand command) {
        BusinessModule entity = requireModule(moduleCode);
        String oldParentCode = entity.getParentCode();
        TreeMeta meta = resolveMeta(command.getParentCode(), moduleCode);
        entity.setModuleName(command.getModuleName().trim());
        entity.setParentCode(trimToNull(command.getParentCode()));
        entity.setLevelNum(meta.levelNum());
        entity.setAncestorPath(meta.ancestorPath());
        entity.setEnabledFlag(normalizeFlag(command.getEnabledFlag(), "Y"));
        entity.setSortOrder(command.getSortOrder() == null ? entity.getSortOrder() : command.getSortOrder());
        entity.setSecurityLevel(normalizeSecurityLevelCode(command.getSecurityLevelCode()));
        entity.setIntegrationType(normalizeIntegrationType(command.getIntegrationType()));
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setRemark(trimToNull(command.getRemark()));
        Boolean patchBarcodeFlag = command.getPatchBarcodeModuleCode();
        boolean applyBarcode = patchBarcodeFlag == null || Boolean.TRUE.equals(patchBarcodeFlag);
        if (applyBarcode) {
            String previousBarcode = normalizeBarcodeModuleRef(entity.getBarcodeModuleCode());
            String barcodeRef = normalizeBarcodeModuleRef(command.getBarcodeModuleCode());
            if (!Objects.equals(previousBarcode, barcodeRef)) {
                assertBarcodeModuleAssignable(barcodeRef);
            }
            assertBarcodeOnlyForLeafModule(moduleCode, barcodeRef);
            entity.setBarcodeModuleCode(barcodeRef);
        }
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(LocalDateTime.now());
        businessModuleMapper.updateById(entity);
        refreshDescendants(entity);
        String newParentCode = entity.getParentCode();
        if (!Objects.equals(trimToNull(oldParentCode), trimToNull(newParentCode)) && StringUtils.hasText(newParentCode)) {
            clearBarcodeWhenModuleHasChildren(newParentCode.trim());
        }
        BusinessModule refreshed = requireModule(moduleCode);
        return toNode(refreshed, loadSecurityLevelSnapshot(), loadBarcodeMapByCodes(toBarcodeCodeList(refreshed.getBarcodeModuleCode())));
    }

    @Override
    @Transactional
    public void delete(String moduleCode) {
        BusinessModule entity = requireModule(moduleCode);
        if (hasChildren(moduleCode)) {
            throw new BusinessException("当前业务模块存在下级节点，不能直接删除");
        }
        businessModuleMapper.update(null, new LambdaUpdateWrapper<BusinessModule>()
                .eq(BusinessModule::getId, entity.getId())
                .set(BusinessModule::getDeleteFlag, "Y")
                .set(BusinessModule::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
                .set(BusinessModule::getLastUpdateDate, LocalDateTime.now()));
    }

    @Override
    public List<BusinessModuleExtFieldResponse> listFields(String moduleCode, String fieldScope) {
        BusinessModule module = requireModule(moduleCode);
        String normalizedScope = StringUtils.hasText(fieldScope) ? fieldScope.trim().toUpperCase() : null;
        List<String> lineageCodes = resolveLineageModuleCodes(module);
        List<BusinessModuleExtField> all = extFieldMapper.selectList(new LambdaQueryWrapper<BusinessModuleExtField>()
                .in(BusinessModuleExtField::getModuleCode, lineageCodes)
                .eq(BusinessModuleExtField::getDeleteFlag, "N")
                .eq(StringUtils.hasText(normalizedScope), BusinessModuleExtField::getFieldScope, normalizedScope)
                .orderByAsc(BusinessModuleExtField::getSortOrder)
                .orderByAsc(BusinessModuleExtField::getFieldCode));
        return resolveEffectiveInheritedFields(lineageCodes, all).stream().map(this::toFieldResponse).toList();
    }

    @Override
    public List<BusinessModuleExtFieldResponse> listFieldsByApplicationFunction(String moduleCode,
                                                                                 String applicationFunction,
                                                                                 String fieldScope) {
        BusinessModule module = requireModule(moduleCode);
        if (!StringUtils.hasText(applicationFunction)) {
            throw new BusinessException("应用功能不能为空");
        }
        String fn = applicationFunction.trim();
        String normalizedScope = StringUtils.hasText(fieldScope) ? fieldScope.trim().toUpperCase() : null;
        List<String> lineageCodes = resolveLineageModuleCodes(module);
        List<BusinessModuleExtField> all = extFieldMapper.selectList(new LambdaQueryWrapper<BusinessModuleExtField>()
                .in(BusinessModuleExtField::getModuleCode, lineageCodes)
                .eq(BusinessModuleExtField::getDeleteFlag, "N")
                .eq(StringUtils.hasText(normalizedScope), BusinessModuleExtField::getFieldScope, normalizedScope)
                .orderByAsc(BusinessModuleExtField::getSortOrder)
                .orderByAsc(BusinessModuleExtField::getFieldCode));
        return resolveEffectiveInheritedFields(lineageCodes, all).stream()
                .filter(entity -> parseApplicationFunctions(entity.getApplicationFunctions()).contains(fn))
                .map(this::toFieldResponse)
                .toList();
    }

    @Override
    public List<BusinessModuleExtField> listEffectivePendingArchiveBasicExtFields(String leafModuleCode) {
        if (!StringUtils.hasText(leafModuleCode)) {
            return List.of();
        }
        BusinessModule module = requireModule(leafModuleCode.trim());
        List<String> lineageCodes = resolveLineageModuleCodes(module);
        List<BusinessModuleExtField> all = extFieldMapper.selectList(new LambdaQueryWrapper<BusinessModuleExtField>()
            .in(BusinessModuleExtField::getModuleCode, lineageCodes)
            .eq(BusinessModuleExtField::getDeleteFlag, "N")
            .eq(BusinessModuleExtField::getEnabledFlag, "Y")
            .eq(BusinessModuleExtField::getFieldScope, "BASIC")
            .orderByAsc(BusinessModuleExtField::getSortOrder)
            .orderByAsc(BusinessModuleExtField::getFieldCode));
        if (all == null) {
            return List.of();
        }
        return resolveEffectiveInheritedFields(lineageCodes, all).stream()
            .filter(entity -> parseApplicationFunctions(entity.getApplicationFunctions()).contains("应归档数据"))
            .toList();
    }

    @Override
    public List<BusinessModuleExtFieldResponse> listPendingArchiveBasicExtFieldsUnionUnderDocumentType(String documentTypeRootCode) {
        if (!StringUtils.hasText(documentTypeRootCode)) {
            throw new BusinessException("documentTypeRootCode cannot be blank");
        }
        String root = documentTypeRootCode.trim();
        requireModule(root);
        return resolvePendingArchiveBasicExtFieldsUnionEntities(root).stream().map(this::toFieldResponse).toList();
    }

    @Override
    public List<BusinessModuleExtField> listPendingArchiveBasicExtFieldEntitiesUnionUnderDocumentType(String documentTypeRootCode) {
        if (!StringUtils.hasText(documentTypeRootCode)) {
            throw new BusinessException("documentTypeRootCode cannot be blank");
        }
        String root = documentTypeRootCode.trim();
        requireModule(root);
        return resolvePendingArchiveBasicExtFieldsUnionEntities(root);
    }

    /**
     * 文档类型根下子树内「应归档数据」BASIC 字段按 canonical 键去重（层级浅者优先），与批量导入模板并集一致。
     */
    private List<BusinessModuleExtField> resolvePendingArchiveBasicExtFieldsUnionEntities(String rootCode) {
        List<String> subtree = collectSelfAndDescendantModuleCodes(rootCode);
        if (subtree.isEmpty()) {
            return List.of();
        }
        Map<String, BusinessModule> moduleByCode = businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getDeleteFlag, "N"))
            .stream()
            .collect(Collectors.toMap(BusinessModule::getModuleCode, Function.identity(), (a, b) -> a));

        List<BusinessModuleExtField> rows = extFieldMapper.selectList(new LambdaQueryWrapper<BusinessModuleExtField>()
            .in(BusinessModuleExtField::getModuleCode, subtree)
            .eq(BusinessModuleExtField::getFieldScope, "BASIC")
            .eq(BusinessModuleExtField::getDeleteFlag, "N")
            .eq(BusinessModuleExtField::getEnabledFlag, "Y")
            .orderByAsc(BusinessModuleExtField::getSortOrder)
            .orderByAsc(BusinessModuleExtField::getFieldCode));

        List<BusinessModuleExtField> candidates = rows.stream()
            .filter(f -> parseApplicationFunctions(f.getApplicationFunctions()).contains("应归档数据"))
            .sorted(Comparator
                .comparing((BusinessModuleExtField f) -> levelNumOf(moduleByCode, f.getModuleCode()))
                .thenComparing(f -> f.getSortOrder() == null ? 0 : f.getSortOrder())
                .thenComparing(BusinessModuleExtField::getFieldCode))
            .toList();

        Map<String, BusinessModuleExtField> byCanonical = new LinkedHashMap<>();
        for (BusinessModuleExtField f : candidates) {
            String canonical = canonicalBusinessExtKey(f);
            if (!StringUtils.hasText(canonical)) {
                continue;
            }
            byCanonical.putIfAbsent(canonical, f);
        }
        return byCanonical.values().stream()
            .sorted(Comparator
                .comparing((BusinessModuleExtField f) -> levelNumOf(moduleByCode, f.getModuleCode()))
                .thenComparing(f -> f.getSortOrder() == null ? 0 : f.getSortOrder())
                .thenComparing(BusinessModuleExtField::getFieldCode))
            .toList();
    }

    private static int levelNumOf(Map<String, BusinessModule> moduleByCode, String moduleCode) {
        BusinessModule m = moduleByCode.get(moduleCode);
        if (m == null || m.getLevelNum() == null) {
            return Integer.MAX_VALUE;
        }
        return m.getLevelNum();
    }

    private static String canonicalBusinessExtKey(BusinessModuleExtField f) {
        if (f == null) {
            return "";
        }
        String en = f.getEnglishFieldName() != null ? f.getEnglishFieldName().trim() : "";
        if (StringUtils.hasText(en)) {
            return en;
        }
        return f.getFieldCode() != null ? f.getFieldCode().trim() : "";
    }

    /**
     * 自根节点起的子树前序（按 sortOrder、moduleCode 稳定遍历子节点）。
     */
    private List<String> collectSelfAndDescendantModuleCodes(String rootCode) {
        List<BusinessModule> all = businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getDeleteFlag, "N"));
        Map<String, List<BusinessModule>> childrenByParent = new HashMap<>();
        Set<String> codes = new HashSet<>();
        for (BusinessModule m : all) {
            codes.add(m.getModuleCode());
            String pk = StringUtils.hasText(m.getParentCode()) ? m.getParentCode().trim() : "";
            childrenByParent.computeIfAbsent(pk, k -> new ArrayList<>()).add(m);
        }
        if (!codes.contains(rootCode)) {
            return List.of();
        }
        for (List<BusinessModule> kids : childrenByParent.values()) {
            kids.sort(Comparator.comparing(BusinessModule::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BusinessModule::getModuleCode));
        }
        List<String> order = new ArrayList<>();
        ArrayDeque<String> q = new ArrayDeque<>();
        q.add(rootCode);
        Set<String> seen = new HashSet<>();
        while (!q.isEmpty()) {
            String cur = q.poll();
            if (!seen.add(cur)) {
                continue;
            }
            order.add(cur);
            List<BusinessModule> kids = childrenByParent.get(cur);
            if (kids == null) {
                continue;
            }
            for (BusinessModule ch : kids) {
                q.add(ch.getModuleCode());
            }
        }
        return order;
    }

    @Override
    @Transactional
    public BusinessModuleExtFieldResponse createField(String moduleCode, BusinessModuleExtFieldCommand command) {
        requireModule(moduleCode);
        validateField(command);
        String normalizedScope = command.getFieldScope().trim().toUpperCase();
        String normalizedAttribute = normalizeExtAttribute(command.getExtAttribute(), command.getFieldScope(), command.getDataType());
        FieldSemanticReference semanticReference = resolveSemanticReference(
                normalizedScope,
                normalizedAttribute,
                null,
                command.getFieldName(),
                command.getEnglishFieldName());
        List<String> functions = normalizeApplicationFunctionList(command.getApplicationFunctions());
        String primaryFunction = functions.isEmpty() ? "EXT" : functions.get(0);
        String requestedFieldCode = command.getFieldCode().trim();
        String fieldCode = resolveFieldCodeForCreate(requestedFieldCode, primaryFunction, semanticReference.reused());

        BusinessModuleExtField entity = new BusinessModuleExtField();
        entity.setFieldCode(fieldCode);
        entity.setModuleCode(moduleCode);
        applyField(entity, command, functions, semanticReference);
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(LocalDateTime.now());
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(LocalDateTime.now());
        extFieldMapper.insert(entity);
        return toFieldResponse(entity);
    }

    @Override
    @Transactional
    public BusinessModuleExtFieldResponse updateField(String moduleCode, String fieldCode, BusinessModuleExtFieldCommand command) {
        requireModule(moduleCode);
        validateField(command);
        BusinessModuleExtField entity = requireField(moduleCode, fieldCode);
        if (!fieldCode.equals(command.getFieldCode().trim())) {
            throw new BusinessException("字段编码不允许修改");
        }
        String normalizedScope = command.getFieldScope().trim().toUpperCase();
        String normalizedAttribute = normalizeExtAttribute(command.getExtAttribute(), command.getFieldScope(), command.getDataType());
        FieldSemanticReference semanticReference = resolveSemanticReference(
                normalizedScope,
                normalizedAttribute,
                entity.getFieldId(),
                command.getFieldName(),
                command.getEnglishFieldName());
        applyField(entity, command, command.getApplicationFunctions(), semanticReference);
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(LocalDateTime.now());
        extFieldMapper.updateById(entity);
        return toFieldResponse(entity);
    }

    @Override
    @Transactional
    public void deleteField(String moduleCode, String fieldCode) {
        BusinessModuleExtField entity = requireField(moduleCode, fieldCode);
        extFieldMapper.update(null, new LambdaUpdateWrapper<BusinessModuleExtField>()
                .eq(BusinessModuleExtField::getFieldId, entity.getFieldId())
                .set(BusinessModuleExtField::getDeleteFlag, "Y")
                .set(BusinessModuleExtField::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
                .set(BusinessModuleExtField::getLastUpdateDate, LocalDateTime.now()));
    }

    private void applyField(BusinessModuleExtField entity,
                            BusinessModuleExtFieldCommand command,
                            List<String> applicationFunctions,
                            FieldSemanticReference semanticReference) {
        entity.setFieldScope(command.getFieldScope().trim().toUpperCase());
        entity.setApplicationFunctions(normalizeApplicationFunctions(applicationFunctions));
        entity.setExtAttribute(normalizeExtAttribute(command.getExtAttribute(), command.getFieldScope(), command.getDataType()));
        if (semanticReference.reused()) {
            entity.setFieldName(semanticReference.fieldName());
            entity.setEnglishFieldName(semanticReference.englishFieldName());
        } else {
            entity.setFieldName(command.getFieldName().trim());
            entity.setEnglishFieldName(trimToNull(command.getEnglishFieldName()));
        }
        entity.setDataType(command.getDataType().trim().toUpperCase());
        entity.setQueryFlag(normalizeFlag(command.getQueryFlag(), "N"));
        entity.setRequiredFlag(normalizeFlag(command.getRequiredFlag(), "N"));
        entity.setEnabledFlag(normalizeFlag(command.getEnabledFlag(), "Y"));
        entity.setSortOrder(command.getSortOrder() == null ? 1 : command.getSortOrder());
    }

    private void validateField(BusinessModuleExtFieldCommand command) {
        if (!StringUtils.hasText(command.getFieldCode())) {
            throw new BusinessException("字段编码不能为空");
        }
        if (!List.of("BASIC", "ATTACHMENT").contains(command.getFieldScope().trim().toUpperCase())) {
            throw new BusinessException("字段归属仅支持 BASIC 或 ATTACHMENT");
        }
        if (!List.of("TEXT", "NUMBER", "DATE", "DATETIME", "DICT", "BOOLEAN").contains(command.getDataType().trim().toUpperCase())) {
            throw new BusinessException("数据类型仅支持 TEXT、NUMBER、DATE、DATETIME、DICT、BOOLEAN");
        }
    }

    private TreeMeta resolveMeta(String parentCode, String currentCode) {
        if (!StringUtils.hasText(parentCode)) {
            return new TreeMeta(1, "");
        }
        if (parentCode.equals(currentCode)) {
            throw new BusinessException("上级业务模块不能选择自身");
        }
        BusinessModule parent = findModule(parentCode);
        if (parent == null) {
            if (isBusinessModuleCode(parentCode) || isDocumentTypeCode(parentCode)) {
                return new TreeMeta(1, parentCode);
            }
            throw new BusinessException("上级业务模块不存在");
        }
        if (parent.getLevelNum() >= MAX_LEVEL) {
            throw new BusinessException("业务模块最多支持 " + MAX_LEVEL + " 层");
        }
        if (StringUtils.hasText(currentCode) && StringUtils.hasText(parent.getAncestorPath())
                && List.of(parent.getAncestorPath().split("/")).contains(currentCode)) {
            throw new BusinessException("上级业务模块不能选择自身下级");
        }
        String ancestorPath = StringUtils.hasText(parent.getAncestorPath()) ? parent.getAncestorPath() + "/" + parent.getModuleCode() : parent.getModuleCode();
        return new TreeMeta(parent.getLevelNum() + 1, ancestorPath);
    }

    private void refreshDescendants(BusinessModule parent) {
        List<BusinessModule> children = businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getParentCode, parent.getModuleCode())
                .eq(BusinessModule::getDeleteFlag, "N"));
        for (BusinessModule child : children) {
            String ancestorPath = StringUtils.hasText(parent.getAncestorPath()) ? parent.getAncestorPath() + "/" + parent.getModuleCode() : parent.getModuleCode();
            child.setLevelNum(parent.getLevelNum() + 1);
            child.setAncestorPath(ancestorPath);
            child.setLastUpdateDate(LocalDateTime.now());
            businessModuleMapper.updateById(child);
            refreshDescendants(child);
        }
    }

    private BusinessModule requireModule(String moduleCode) {
        BusinessModule module = findModule(moduleCode);
        if (module == null) {
            throw new BusinessException("业务模块不存在");
        }
        return module;
    }

    private BusinessModule findModule(String moduleCode) {
        return businessModuleMapper.selectOne(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getModuleCode, moduleCode)
                .eq(BusinessModule::getDeleteFlag, "N")
                .last("limit 1"));
    }

    private boolean isBusinessModuleCode(String parentCode) {
        return businessModuleMapper.selectCount(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getModuleCode, parentCode)
                .eq(BusinessModule::getDeleteFlag, "N")) > 0;
    }

    private boolean isDocumentTypeCode(String parentCode) {
        return documentTypeConfigMapper.selectCount(new LambdaQueryWrapper<DocumentTypeConfig>()
                .eq(DocumentTypeConfig::getDocTypeCode, parentCode)
                .eq(DocumentTypeConfig::getDeleteFlag, "N")) > 0;
    }

    @Transactional
    protected void syncDocumentTypesToBusinessModules() {
        List<DocumentTypeConfig> documentTypes = documentTypeConfigMapper.selectList(new LambdaQueryWrapper<DocumentTypeConfig>()
                .eq(DocumentTypeConfig::getDeleteFlag, "N")
                .orderByAsc(DocumentTypeConfig::getDocTypeCode));
        if (documentTypes.isEmpty()) {
            return;
        }
        Map<String, DocumentTypeConfig> documentTypeMap = documentTypes.stream()
                .filter(type -> StringUtils.hasText(type.getDocTypeCode()))
                .collect(Collectors.toMap(type -> type.getDocTypeCode().trim(), Function.identity(), (a, b) -> a));
        Set<String> docTypeCodes = new HashSet<>(documentTypeMap.keySet());
        List<BusinessModule> existingModules = businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getDeleteFlag, "N"));
        Map<String, BusinessModule> existingModuleMap = existingModules.stream()
                .filter(module -> StringUtils.hasText(module.getModuleCode()))
                .collect(Collectors.toMap(BusinessModule::getModuleCode, Function.identity(), (a, b) -> a));
        Set<String> existingCodes = new HashSet<>(existingModuleMap.keySet());

        int nextSortOrder = nextSortOrder(null);
        LocalDateTime now = LocalDateTime.now();
        for (DocumentTypeConfig documentType : documentTypes) {
            String code = trimToNull(documentType.getDocTypeCode());
            if (!StringUtils.hasText(code)) {
                continue;
            }
            String desiredParentCode = normalizeDocumentTypeParentCode(code, documentType.getParentCode(), docTypeCodes);
            TreeMeta desiredMeta = buildMetaFromDocumentType(code, documentTypeMap, docTypeCodes);
            BusinessModule existing = existingModuleMap.get(code);
            if (existing != null) {
                boolean parentChanged = !java.util.Objects.equals(trimToNull(existing.getParentCode()), desiredParentCode);
                boolean levelChanged = !java.util.Objects.equals(existing.getLevelNum(), desiredMeta.levelNum());
                boolean ancestorChanged = !java.util.Objects.equals(trimToNull(existing.getAncestorPath()), desiredMeta.ancestorPath());
                if (parentChanged || levelChanged || ancestorChanged) {
                    businessModuleMapper.update(null, new LambdaUpdateWrapper<BusinessModule>()
                            .eq(BusinessModule::getId, existing.getId())
                            .set(BusinessModule::getParentCode, desiredParentCode)
                            .set(BusinessModule::getLevelNum, desiredMeta.levelNum())
                            .set(BusinessModule::getAncestorPath, desiredMeta.ancestorPath())
                            .set(BusinessModule::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
                            .set(BusinessModule::getLastUpdateDate, now));
                }
                continue;
            }
            BusinessModule entity = new BusinessModule();
            entity.setModuleCode(code);
            entity.setModuleName(StringUtils.hasText(documentType.getDocTypeDescription()) ? documentType.getDocTypeDescription().trim() : code);
            entity.setParentCode(desiredParentCode);
            entity.setLevelNum(desiredMeta.levelNum());
            entity.setAncestorPath(desiredMeta.ancestorPath());
            entity.setEnabledFlag(normalizeFlag(documentType.getEnableFlag(), "Y"));
            entity.setSecurityLevel(DEFAULT_SECURITY_LEVEL_CODE);
            entity.setIntegrationType("不集成");
            entity.setDescription(trimToNull(documentType.getDocTypeDescription()));
            entity.setRemark(null);
            entity.setSortOrder(documentType.getSortOrder() == null ? nextSortOrder++ : documentType.getSortOrder());
            entity.setDeleteFlag("N");
            entity.setCreatedBy(SYSTEM_OPERATOR_ID);
            entity.setCreationDate(now);
            entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            entity.setLastUpdateDate(now);
            businessModuleMapper.insert(entity);
            existingCodes.add(code);
        }
    }

    private TreeMeta buildMetaFromDocumentType(String code, Map<String, DocumentTypeConfig> documentTypeMap, Set<String> docTypeCodes) {
        String parentCode = normalizeDocumentTypeParentCode(code, documentTypeMap.get(code) == null ? null : documentTypeMap.get(code).getParentCode(), docTypeCodes);
        if (!StringUtils.hasText(parentCode)) {
            return new TreeMeta(1, "");
        }
        Set<String> visited = new HashSet<>();
        List<String> ancestors = new ArrayList<>();
        String current = parentCode;
        while (StringUtils.hasText(current) && visited.add(current)) {
            DocumentTypeConfig parent = documentTypeMap.get(current);
            if (parent == null) {
                break;
            }
            String next = normalizeDocumentTypeParentCode(current, parent.getParentCode(), docTypeCodes);
            if (!StringUtils.hasText(next)) {
                break;
            }
            current = next;
            ancestors.add(0, current);
        }
        ancestors.add(parentCode);
        return new TreeMeta(ancestors.size() + 1, String.join("/", ancestors));
    }

    private String normalizeDocumentTypeParentCode(String code, String parentCode, Set<String> docTypeCodes) {
        String normalizedParent = trimToNull(parentCode);
        if (!StringUtils.hasText(normalizedParent) || code.equals(normalizedParent)) {
            return null;
        }
        if (docTypeCodes.contains(normalizedParent)) {
            return normalizedParent;
        }
        String inferred = inferParentFromCode(code, docTypeCodes);
        return StringUtils.hasText(inferred) ? inferred : null;
    }

    private String inferParentFromCode(String code, Set<String> docTypeCodes) {
        if (!StringUtils.hasText(code) || !code.contains("_")) {
            return null;
        }
        String candidate = code;
        while (candidate.contains("_")) {
            candidate = candidate.substring(0, candidate.lastIndexOf('_'));
            if (docTypeCodes.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private BusinessModuleExtField requireField(String moduleCode, String fieldCode) {
        BusinessModuleExtField field = extFieldMapper.selectOne(new LambdaQueryWrapper<BusinessModuleExtField>()
                .eq(BusinessModuleExtField::getModuleCode, moduleCode)
                .eq(BusinessModuleExtField::getFieldCode, fieldCode)
                .eq(BusinessModuleExtField::getDeleteFlag, "N")
                .last("limit 1"));
        if (field == null) {
            throw new BusinessException("扩展字段不存在");
        }
        return field;
    }

    private void ensureCodeAvailable(String moduleCode) {
        Long count = businessModuleMapper.selectCount(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getModuleCode, moduleCode)
                .eq(BusinessModule::getDeleteFlag, "N"));
        if (count > 0) {
            throw new BusinessException("业务模块编码已存在");
        }
    }

    private void ensureFieldCodeAvailable(String fieldCode) {
        Long count = extFieldMapper.selectCount(new LambdaQueryWrapper<BusinessModuleExtField>()
                .eq(BusinessModuleExtField::getFieldCode, fieldCode)
                .eq(BusinessModuleExtField::getDeleteFlag, "N"));
        if (count > 0) {
            throw new BusinessException("字段编码已存在");
        }
    }

    private String resolveFieldCodeForCreate(String requestedFieldCode, String applicationFunction, boolean allowAutoDerive) {
        Long count = extFieldMapper.selectCount(new LambdaQueryWrapper<BusinessModuleExtField>()
                .eq(BusinessModuleExtField::getFieldCode, requestedFieldCode)
                .eq(BusinessModuleExtField::getDeleteFlag, "N"));
        if (count == 0) {
            return requestedFieldCode;
        }
        if (!allowAutoDerive) {
            throw new BusinessException("字段编码已存在");
        }
        return buildDerivedFieldCode(requestedFieldCode, applicationFunction);
    }

    private FieldSemanticReference resolveSemanticReference(String fieldScope,
                                                            String extAttribute,
                                                            Long excludeFieldId,
                                                            String requestedFieldName,
                                                            String requestedEnglishFieldName) {
        BusinessModuleExtField existing = extFieldMapper.selectOne(new LambdaQueryWrapper<BusinessModuleExtField>()
                .eq(BusinessModuleExtField::getFieldScope, fieldScope)
                .eq(BusinessModuleExtField::getExtAttribute, extAttribute)
                .eq(BusinessModuleExtField::getDeleteFlag, "N")
                .ne(excludeFieldId != null, BusinessModuleExtField::getFieldId, excludeFieldId)
                .orderByAsc(BusinessModuleExtField::getCreationDate)
                .orderByAsc(BusinessModuleExtField::getFieldId)
                .last("limit 1"));
        if (existing == null) {
            return new FieldSemanticReference(false, null, null);
        }
        String fixedFieldName = existing.getFieldName() == null ? "" : existing.getFieldName().trim();
        String fixedEnglishFieldName = trimToNull(existing.getEnglishFieldName());
        String requestedName = trimToNull(requestedFieldName);
        String requestedEnglishName = trimToNull(requestedEnglishFieldName);
        if (!fixedFieldName.equals(requestedName == null ? "" : requestedName)
                || !java.util.Objects.equals(fixedEnglishFieldName, requestedEnglishName)) {
            throw new BusinessException("扩展字段语义冲突：同一扩展字段只能定义一套字段名/字段编码");
        }
        return new FieldSemanticReference(true, fixedFieldName, fixedEnglishFieldName);
    }

    private boolean hasChildren(String moduleCode) {
        return businessModuleMapper.selectCount(new LambdaQueryWrapper<BusinessModule>()
                .eq(BusinessModule::getParentCode, moduleCode)
                .eq(BusinessModule::getDeleteFlag, "N")) > 0;
    }

    private Integer nextSortOrder(String parentCode) {
        Long count = businessModuleMapper.selectCount(new LambdaQueryWrapper<BusinessModule>()
                .eq(StringUtils.hasText(parentCode), BusinessModule::getParentCode, parentCode)
                .isNull(!StringUtils.hasText(parentCode), BusinessModule::getParentCode)
                .eq(BusinessModule::getDeleteFlag, "N"));
        return count.intValue() + 1;
    }

    private String normalizeFlag(String flag, String defaultValue) {
        String normalized = StringUtils.hasText(flag) ? flag.trim().toUpperCase() : defaultValue;
        if (!List.of("Y", "N").contains(normalized)) {
            throw new BusinessException("启用/查询/必填标志仅支持 Y 或 N");
        }
        return normalized;
    }

    private String normalizeSecurityLevelCode(String securityLevel) {
        SecurityLevelDictionarySnapshot snapshot = loadSecurityLevelSnapshot();
        String normalized = StringUtils.hasText(securityLevel) ? securityLevel.trim() : DEFAULT_SECURITY_LEVEL_CODE;
        if (snapshot.codeToName.containsKey(normalized)) {
            return normalized;
        }
        String resolvedCode = snapshot.nameToCode.get(normalized);
        if (StringUtils.hasText(resolvedCode)) {
            return resolvedCode;
        }
        throw new BusinessException("密级无效，请从字典管理中选择有效密级");
    }

    private String normalizeIntegrationType(String integrationType) {
        String normalized = StringUtils.hasText(integrationType) ? integrationType.trim() : "不集成";
        if (!List.of("全部集成", "部分集成", "不集成").contains(normalized)) {
            throw new BusinessException("集成类型仅支持：全部集成、部分集成、不集成");
        }
        return normalized;
    }

    private String normalizeApplicationFunctions(List<String> applicationFunctions) {
        List<String> normalized = normalizeApplicationFunctionList(applicationFunctions);
        if (normalized.isEmpty() || !SUPPORTED_APPLICATION_FUNCTIONS.containsAll(normalized)) {
            throw new BusinessException("应用功能仅支持：应归档数据、移交");
        }
        return String.join(",", normalized);
    }

    private List<String> normalizeApplicationFunctionList(List<String> applicationFunctions) {
        if (applicationFunctions == null || applicationFunctions.isEmpty()) {
            throw new BusinessException("应用功能不能为空");
        }
        return applicationFunctions.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> "应收".equals(value) ? "应归档数据" : value)
                .distinct()
                .toList();
    }

    private String buildDerivedFieldCode(String baseFieldCode, String applicationFunction) {
        String suffix = switch (applicationFunction) {
            case "应归档数据", "应收" -> "AR";
            case "移交" -> "TR";
            default -> "EXT";
        };
        int maxBaseLength = Math.max(1, 64 - suffix.length() - 1);
        String normalizedBase = baseFieldCode.length() > maxBaseLength ? baseFieldCode.substring(0, maxBaseLength) : baseFieldCode;
        String candidate = normalizedBase + "_" + suffix;
        int serial = 1;
        while (extFieldMapper.selectCount(new LambdaQueryWrapper<BusinessModuleExtField>()
                .eq(BusinessModuleExtField::getFieldCode, candidate)
                .eq(BusinessModuleExtField::getDeleteFlag, "N")) > 0) {
            String serialSuffix = "_" + suffix + serial++;
            int dynamicBaseLength = Math.max(1, 64 - serialSuffix.length());
            String dynamicBase = baseFieldCode.length() > dynamicBaseLength ? baseFieldCode.substring(0, dynamicBaseLength) : baseFieldCode;
            candidate = dynamicBase + serialSuffix;
        }
        return candidate;
    }

    private List<String> parseApplicationFunctions(String applicationFunctions) {
        if (!StringUtils.hasText(applicationFunctions)) {
            return List.of();
        }
        return List.of(applicationFunctions.split(",")).stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(value -> "应收".equals(value) ? "应归档数据" : value)
                .distinct()
                .toList();
    }

    private String normalizeExtAttribute(String extAttribute, String fieldScope, String dataType) {
        if (!StringUtils.hasText(extAttribute)) {
            throw new BusinessException("扩展字段不能为空");
        }
        String normalized = extAttribute.trim().toUpperCase();
        Set<String> allowedAttributes = resolveAllowedAttributes(fieldScope, dataType);
        if (!allowedAttributes.contains(normalized)) {
            throw new BusinessException("扩展字段与字段类型不匹配，请按字段池规则选择");
        }
        return normalized;
    }

    private record FieldSemanticReference(boolean reused, String fieldName, String englishFieldName) {}

    private Set<String> resolveAllowedAttributes(String fieldScope, String dataType) {
        String scope = StringUtils.hasText(fieldScope) ? fieldScope.trim().toUpperCase() : "";
        String type = StringUtils.hasText(dataType) ? dataType.trim().toUpperCase() : "";
        if ("BASIC".equals(scope)) {
            return switch (type) {
                case "TEXT", "DICT", "BOOLEAN" -> BASIC_TEXT_ATTRIBUTES;
                case "NUMBER" -> BASIC_NUMBER_ATTRIBUTES;
                case "DATE" -> BASIC_DATE_ATTRIBUTES;
                case "DATETIME" -> BASIC_DATETIME_ATTRIBUTES;
                default -> Set.of();
            };
        }
        if ("ATTACHMENT".equals(scope)) {
            return switch (type) {
                case "TEXT", "DICT", "BOOLEAN" -> ATTACHMENT_TEXT_ATTRIBUTES;
                case "NUMBER" -> ATTACHMENT_NUMBER_ATTRIBUTES;
                case "DATE" -> ATTACHMENT_DATE_ATTRIBUTES;
                case "DATETIME" -> ATTACHMENT_DATETIME_ATTRIBUTES;
                default -> Set.of();
            };
        }
        return Set.of();
    }

    private static Set<String> buildAttributePool(String prefix, int startInclusive, int endInclusive) {
        Set<String> result = new HashSet<>();
        for (int i = startInclusive; i <= endInclusive; i++) {
            result.add(prefix + i);
        }
        return Set.copyOf(result);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<String> resolveLineageModuleCodes(BusinessModule module) {
        List<String> codes = new ArrayList<>();
        codes.add(module.getModuleCode());
        if (StringUtils.hasText(module.getAncestorPath())) {
            List<String> ancestors = List.of(module.getAncestorPath().split("/")).stream()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .toList();
            for (int i = ancestors.size() - 1; i >= 0; i--) {
                if (isBusinessModuleCode(ancestors.get(i))) {
                    codes.add(ancestors.get(i));
                }
            }
        }
        return codes;
    }

    private List<BusinessModuleExtField> resolveEffectiveInheritedFields(List<String> lineageCodes, List<BusinessModuleExtField> allFields) {
        Map<String, Integer> priorityByModuleCode = new LinkedHashMap<>();
        for (int i = 0; i < lineageCodes.size(); i++) {
            priorityByModuleCode.put(lineageCodes.get(i), i);
        }
        Map<String, BusinessModuleExtField> effective = new LinkedHashMap<>();
        allFields.stream()
                .sorted(Comparator
                        .comparingInt((BusinessModuleExtField f) -> priorityByModuleCode.getOrDefault(f.getModuleCode(), Integer.MAX_VALUE))
                        .thenComparing(BusinessModuleExtField::getSortOrder)
                        .thenComparing(BusinessModuleExtField::getFieldCode))
                .forEach(field -> {
                    String key = field.getFieldScope() + "::" + field.getExtAttribute();
                    effective.putIfAbsent(key, field);
                });
        return effective.values().stream()
                .sorted(Comparator
                        .comparingInt((BusinessModuleExtField f) -> priorityByModuleCode.getOrDefault(f.getModuleCode(), Integer.MAX_VALUE))
                        .thenComparing(BusinessModuleExtField::getSortOrder)
                        .thenComparing(BusinessModuleExtField::getFieldCode))
                .toList();
    }

    private void sortTree(List<BusinessModuleNodeResponse> nodes) {
        nodes.sort(Comparator.comparing(BusinessModuleNodeResponse::getSortOrder).thenComparing(BusinessModuleNodeResponse::getModuleCode));
        nodes.forEach(node -> sortTree(node.getChildren()));
    }

    private Map<String, BarcodeModule> loadBarcodeMapByCodes(Collection<String> rawCodes) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            return Map.of();
        }
        List<String> codes = rawCodes.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(StringUtils::hasText)
            .map(s -> s.toUpperCase(Locale.ROOT))
            .distinct()
            .toList();
        if (codes.isEmpty()) {
            return Map.of();
        }
        // PostgreSQL 区分大小写：须按 upper(trim(code)) 匹配，否则树节点无法带出条码名称、前台长期显示「未映射」
        LambdaQueryWrapper<BarcodeModule> bmWhere = new LambdaQueryWrapper<BarcodeModule>()
            .eq(BarcodeModule::getDeleteFlag, "N");
        if (codes.size() == 1) {
            bmWhere.apply("upper(trim(barcode_module_code)) = {0}", codes.get(0));
        } else {
            String placeholders = IntStream.range(0, codes.size())
                .mapToObj(i -> "{" + i + "}")
                .collect(Collectors.joining(","));
            bmWhere.apply("upper(trim(barcode_module_code)) IN (" + placeholders + ")", codes.toArray());
        }
        List<BarcodeModule> list = barcodeModuleMapper.selectList(bmWhere);
        return list.stream().collect(Collectors.toMap(
            b -> b.getBarcodeCode().trim().toUpperCase(Locale.ROOT),
            Function.identity(),
            (a, b) -> a));
    }

    private static List<String> toBarcodeCodeList(String code) {
        if (!StringUtils.hasText(code)) {
            return List.of();
        }
        return List.of(code.trim().toUpperCase(Locale.ROOT));
    }

    private static String normalizeBarcodeModuleRef(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private void assertBarcodeModuleAssignable(String barcodeModuleCode) {
        if (!StringUtils.hasText(barcodeModuleCode)) {
            return;
        }
        String code = barcodeModuleCode.trim().toUpperCase(Locale.ROOT);
        BarcodeModule b = barcodeModuleMapper.selectOne(new LambdaQueryWrapper<BarcodeModule>()
            .eq(BarcodeModule::getDeleteFlag, "N")
            .apply("upper(trim(barcode_module_code)) = {0}", code)
            .last("limit 1"));
        if (b == null) {
            throw new BusinessException("条码模块不存在或已删除");
        }
        if (!"Y".equalsIgnoreCase(StringUtils.hasText(b.getEnableFlag()) ? b.getEnableFlag().trim() : "")) {
            throw new BusinessException("条码模块已停用，无法配置到业务模块");
        }
    }

    /**
     * 仅最下层叶子业务模块（无下级业务模块）可映射条码模块。
     */
    private void assertBarcodeOnlyForLeafModule(String moduleCode, String barcodeModuleCode) {
        if (!StringUtils.hasText(barcodeModuleCode)) {
            return;
        }
        if (hasChildren(moduleCode)) {
            throw new BusinessException("仅最下层叶子业务模块可配置条码模块，请先删除或调整下级模块后再试");
        }
    }

    /** 当某模块已存在下级业务模块时，清除其条码映射（例如在其下新增子模块或调整上级后）。 */
    private void clearBarcodeWhenModuleHasChildren(String moduleCode) {
        if (!StringUtils.hasText(moduleCode)) {
            return;
        }
        if (!hasChildren(moduleCode.trim())) {
            return;
        }
        BusinessModule module = findModule(moduleCode.trim());
        if (module == null || !StringUtils.hasText(module.getBarcodeModuleCode())) {
            return;
        }
        businessModuleMapper.update(null, new LambdaUpdateWrapper<BusinessModule>()
            .eq(BusinessModule::getId, module.getId())
            .set(BusinessModule::getBarcodeModuleCode, null)
            .set(BusinessModule::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(BusinessModule::getLastUpdateDate, LocalDateTime.now()));
    }

    private BusinessModuleNodeResponse toNode(BusinessModule entity, SecurityLevelDictionarySnapshot snapshot, Map<String, BarcodeModule> barcodeByCode) {
        BusinessModuleNodeResponse node = new BusinessModuleNodeResponse();
        node.setId(entity.getId());
        node.setModuleCode(entity.getModuleCode());
        node.setModuleName(entity.getModuleName());
        node.setParentCode(entity.getParentCode());
        node.setLevelNum(entity.getLevelNum());
        node.setAncestorPath(entity.getAncestorPath());
        node.setEnabledFlag(entity.getEnabledFlag());
        String code = entity.getSecurityLevel();
        String name = snapshot.codeToName.getOrDefault(code, code);
        node.setSecurityLevelCode(code);
        node.setSecurityLevelName(name);
        node.setSecurityLevel(name);
        node.setIntegrationType(entity.getIntegrationType());
        node.setDescription(entity.getDescription());
        node.setRemark(entity.getRemark());
        node.setSortOrder(entity.getSortOrder());
        String ref = StringUtils.hasText(entity.getBarcodeModuleCode())
            ? entity.getBarcodeModuleCode().trim().toUpperCase(Locale.ROOT)
            : null;
        node.setBarcodeModuleCode(ref);
        node.setBarcodeId(null);
        node.setBarcodeCode(null);
        node.setBarcodeName(null);
        if (ref != null && barcodeByCode != null) {
            BarcodeModule bm = barcodeByCode.get(ref);
            if (bm != null) {
                node.setBarcodeId(bm.getBarcodeId());
                node.setBarcodeCode(bm.getBarcodeCode());
                node.setBarcodeName(bm.getBarcodeName());
            } else {
                // 业务表已有编码但主数据未命中映射时仍展示编码，避免前台误认为「未映射」
                node.setBarcodeCode(ref);
                node.setBarcodeName("");
            }
        }
        node.setLastUpdatedBy(entity.getLastUpdatedBy());
        node.setLastUpdateDate(entity.getLastUpdateDate());
        return node;
    }

    private SecurityLevelDictionarySnapshot loadSecurityLevelSnapshot() {
        List<DictionaryItem> items = dictionaryItemMapper.selectList(new LambdaQueryWrapper<DictionaryItem>()
                .eq(DictionaryItem::getCategoryCode, SECURITY_LEVEL_CATEGORY_CODE)
                .eq(DictionaryItem::getDeleteFlag, "N")
                .eq(DictionaryItem::getEnabledFlag, "Y")
                .orderByAsc(DictionaryItem::getSortOrder)
                .orderByAsc(DictionaryItem::getItemCode));
        Map<String, String> codeToName = new LinkedHashMap<>();
        Map<String, String> nameToCode = new LinkedHashMap<>();
        for (DictionaryItem item : items) {
            if (!StringUtils.hasText(item.getItemCode())) {
                continue;
            }
            String code = item.getItemCode().trim();
            String name = StringUtils.hasText(item.getItemName()) ? item.getItemName().trim() : code;
            codeToName.put(code, name);
            nameToCode.putIfAbsent(name, code);
        }
        if (codeToName.isEmpty()) {
            throw new BusinessException("未配置密级字典，请先在字典管理中维护 SECURITY_LEVEL");
        }
        return new SecurityLevelDictionarySnapshot(codeToName, nameToCode);
    }

    private BusinessModuleExtFieldResponse toFieldResponse(BusinessModuleExtField entity) {
        BusinessModuleExtFieldResponse response = new BusinessModuleExtFieldResponse();
        response.setFieldId(entity.getFieldId());
        response.setFieldCode(entity.getFieldCode());
        response.setModuleCode(entity.getModuleCode());
        response.setFieldScope(entity.getFieldScope());
        response.setApplicationFunctions(parseApplicationFunctions(entity.getApplicationFunctions()));
        response.setExtAttribute(entity.getExtAttribute());
        response.setFieldName(entity.getFieldName());
        response.setEnglishFieldName(entity.getEnglishFieldName());
        response.setDataType(entity.getDataType());
        response.setQueryFlag(entity.getQueryFlag());
        response.setRequiredFlag(entity.getRequiredFlag());
        response.setEnabledFlag(entity.getEnabledFlag());
        response.setSortOrder(entity.getSortOrder());
        response.setLastUpdateDate(entity.getLastUpdateDate());
        return response;
    }

    private BusinessModuleParentOptionResponse toParentOption(BusinessModule module) {
        BusinessModuleParentOptionResponse response = new BusinessModuleParentOptionResponse();
        response.setCode(module.getModuleCode());
        response.setDescription(StringUtils.hasText(module.getDescription()) ? module.getDescription() : module.getModuleName());
        response.setSourceType("BUSINESS_MODULE");
        return response;
    }

    private BusinessModuleParentOptionResponse toParentOption(DocumentTypeConfig documentType) {
        BusinessModuleParentOptionResponse response = new BusinessModuleParentOptionResponse();
        response.setCode(documentType.getDocTypeCode());
        response.setDescription(documentType.getDocTypeDescription());
        response.setSourceType("DOCUMENT_TYPE");
        return response;
    }

    private record TreeMeta(Integer levelNum, String ancestorPath) {}

    private record SecurityLevelDictionarySnapshot(Map<String, String> codeToName, Map<String, String> nameToCode) {}
}
