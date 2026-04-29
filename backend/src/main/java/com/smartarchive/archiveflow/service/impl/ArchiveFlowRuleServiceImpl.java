package com.smartarchive.archiveflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartarchive.archiveflow.domain.ArchiveFlowRule;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleCreateCommand;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleDetailResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleOptionResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRulePermissionPreviewResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleSummaryResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleUpdateCommand;
import com.smartarchive.archiveflow.dto.ArchiveRuleMatchResponse;
import com.smartarchive.archiveflow.mapper.ArchiveFlowLookupMapper;
import com.smartarchive.archiveflow.mapper.ArchiveFlowRuleMapper;
import com.smartarchive.archiveflow.service.ArchiveFlowRuleService;
import com.smartarchive.businessmodule.domain.BusinessModule;
import com.smartarchive.businessmodule.mapper.BusinessModuleMapper;
import com.smartarchive.common.audit.service.OperationAuditService;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.companyinfo.domain.CompanyInfo;
import com.smartarchive.companyinfo.mapper.CompanyInfoMapper;
import com.smartarchive.countryregion.domain.CountryRegion;
import com.smartarchive.countryregion.mapper.CountryRegionMapper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ArchiveFlowRuleServiceImpl implements ArchiveFlowRuleService {
    private static final Long SYSTEM_OPERATOR_ID = 1L;
    private static final String SYSTEM_OPERATOR_NAME = "system";
    private static final String MODULE_CODE = "ARCHIVE_FLOW_RULE";
    private static final String MODULE_NAME = "Archive Flow Rule Management";

    private final ArchiveFlowRuleMapper archiveFlowRuleMapper;
    private final ArchiveFlowLookupMapper archiveFlowLookupMapper;
    private final CompanyInfoMapper companyInfoMapper;
    private final BusinessModuleMapper businessModuleMapper;
    private final CountryRegionMapper countryRegionMapper;
    private final OperationAuditService operationAuditService;

    @Override
    public List<ArchiveFlowRuleSummaryResponse> list(String keyword,
                                                     String companyProjectCode,
                                                     String busiModuleCode,
                                                     String documentOrganizationCode,
                                                     String enabledFlag) {
        Map<String, String> companyProjectNameMap = listActiveCompanies().stream()
            .collect(Collectors.toMap(CompanyInfo::getCompanyCode, CompanyInfo::getCompanyName, (left, right) -> left));
        Map<String, String> busiModuleNameMap = listActiveBusinessModules().stream()
            .collect(Collectors.toMap(BusinessModule::getModuleCode, BusinessModule::getModuleName, (left, right) -> left));
        Map<String, String> documentOrganizationNameMap = listActiveDocumentOrganizationCodes().stream()
            .collect(Collectors.toMap(Function.identity(), Function.identity(), (left, right) -> left));
        Map<String, String> cityNameMap = buildRegionDisplayMap();

        return archiveFlowRuleMapper.selectList(new LambdaQueryWrapper<ArchiveFlowRule>()
                .eq(ArchiveFlowRule::getDeleteFlag, "N")
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                    .like(ArchiveFlowRule::getCompanyProjectCode, keyword.trim())
                    .or()
                    .like(ArchiveFlowRule::getBusiModuleCode, keyword.trim())
                    .or()
                    .like(ArchiveFlowRule::getDocumentOrganizationCode, keyword.trim()))
                .eq(StringUtils.hasText(companyProjectCode), ArchiveFlowRule::getCompanyProjectCode, trimToNull(companyProjectCode))
                .eq(StringUtils.hasText(busiModuleCode), ArchiveFlowRule::getBusiModuleCode, trimToNull(busiModuleCode))
                .eq(StringUtils.hasText(documentOrganizationCode), ArchiveFlowRule::getDocumentOrganizationCode, trimToNull(documentOrganizationCode))
                .eq(StringUtils.hasText(enabledFlag), ArchiveFlowRule::getEnabledFlag, trimToNull(enabledFlag))
                .orderByDesc(ArchiveFlowRule::getLastUpdateDate)
                .orderByAsc(ArchiveFlowRule::getCompanyProjectCode))
            .stream()
            .map(item -> toSummary(item, companyProjectNameMap, busiModuleNameMap, documentOrganizationNameMap, cityNameMap))
            .toList();
    }

    @Override
    public ArchiveFlowRuleDetailResponse getDetail(Long id) {
        return toDetail(findActiveById(id));
    }

    @Override
    @Transactional
    public ArchiveFlowRuleDetailResponse create(ArchiveFlowRuleCreateCommand command) {
        String companyProjectCode = requireText(command.getCompanyProjectCode(), "companyProjectCode");
        ensureCompanyProjectAvailable(companyProjectCode);
        ensureBusinessKeyAvailable(companyProjectCode, null);

        String busiModuleCode = requireText(command.getBusiModuleCode(), "busiModuleCode");
        String documentOrganizationCode = requireText(command.getDocumentOrganizationCode(), "documentOrganizationCode");
        String archiveDestination = trimToNull(command.getArchiveDestination());
        validateType(busiModuleCode);
        validateDocumentOrganization(documentOrganizationCode);
        validateRegion(archiveDestination);
        validateFlag(requireText(command.getExternalDisplayFlag(), "externalDisplayFlag"), "externalDisplayFlag");
        validateFlag(requireText(command.getDefaultFlag(), "defaultFlag"), "defaultFlag");
        validateFlag(requireText(command.getEnabledFlag(), "enabledFlag"), "enabledFlag");
        validateRetentionPeriodYears(command.getRetentionPeriodYears());

        LocalDateTime now = LocalDateTime.now();
        ArchiveFlowRule entity = new ArchiveFlowRule();
        entity.setCompanyProjectCode(companyProjectCode);
        entity.setBusiModuleCode(busiModuleCode);
        entity.setCustomRule(trimToNull(command.getCustomRule()));
        entity.setArchiveDestination(archiveDestination);
        entity.setDocumentOrganizationCode(documentOrganizationCode);
        entity.setRetentionPeriodYears(command.getRetentionPeriodYears());
        entity.setExternalDisplayFlag(command.getExternalDisplayFlag().trim());
        entity.setDefaultFlag(command.getDefaultFlag().trim());
        entity.setEnabledFlag(command.getEnabledFlag().trim());
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(now);
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(now);
        archiveFlowRuleMapper.insert(entity);

        ArchiveFlowRuleDetailResponse after = toDetail(entity);
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "ARCHIVE_FLOW_RULE", companyProjectCode, "CREATE", "Create archive flow rule", null, after, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return after;
    }

    @Override
    @Transactional
    public ArchiveFlowRuleDetailResponse update(Long id, ArchiveFlowRuleUpdateCommand command) {
        ArchiveFlowRule existing = findActiveById(id);
        ArchiveFlowRuleDetailResponse before = toDetail(existing);

        String busiModuleCode = requireText(command.getBusiModuleCode(), "busiModuleCode");
        String documentOrganizationCode = requireText(command.getDocumentOrganizationCode(), "documentOrganizationCode");
        String archiveDestination = trimToNull(command.getArchiveDestination());
        validateType(busiModuleCode);
        validateDocumentOrganization(documentOrganizationCode);
        validateRegion(archiveDestination);
        validateFlag(requireText(command.getExternalDisplayFlag(), "externalDisplayFlag"), "externalDisplayFlag");
        validateFlag(requireText(command.getDefaultFlag(), "defaultFlag"), "defaultFlag");
        validateFlag(requireText(command.getEnabledFlag(), "enabledFlag"), "enabledFlag");
        validateRetentionPeriodYears(command.getRetentionPeriodYears());

        existing.setBusiModuleCode(busiModuleCode);
        existing.setCustomRule(trimToNull(command.getCustomRule()));
        existing.setArchiveDestination(archiveDestination);
        existing.setDocumentOrganizationCode(documentOrganizationCode);
        existing.setRetentionPeriodYears(command.getRetentionPeriodYears());
        existing.setExternalDisplayFlag(command.getExternalDisplayFlag().trim());
        existing.setDefaultFlag(command.getDefaultFlag().trim());
        existing.setEnabledFlag(command.getEnabledFlag().trim());
        existing.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        existing.setLastUpdateDate(LocalDateTime.now());
        archiveFlowRuleMapper.updateById(existing);

        ArchiveFlowRuleDetailResponse after = toDetail(existing);
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "ARCHIVE_FLOW_RULE", existing.getCompanyProjectCode(), "UPDATE", "Update archive flow rule", before, after, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return after;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ArchiveFlowRule existing = findActiveById(id);
        ArchiveFlowRuleDetailResponse before = toDetail(existing);
        archiveFlowRuleMapper.update(null, new LambdaUpdateWrapper<ArchiveFlowRule>()
            .eq(ArchiveFlowRule::getId, existing.getId())
            .eq(ArchiveFlowRule::getDeleteFlag, "N")
            .set(ArchiveFlowRule::getDeleteFlag, "Y")
            .set(ArchiveFlowRule::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(ArchiveFlowRule::getLastUpdateDate, LocalDateTime.now()));
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "ARCHIVE_FLOW_RULE", existing.getCompanyProjectCode(), "DELETE", "Soft delete archive flow rule", before, null, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
    }

    @Override
    public List<ArchiveFlowRuleOptionResponse> listCompanyProjectOptions() {
        return listActiveCompanies().stream()
            .map(item -> ArchiveFlowRuleOptionResponse.builder().code(item.getCompanyCode()).name(item.getCompanyName()).build())
            .toList();
    }

    @Override
    public List<ArchiveFlowRuleOptionResponse> listBusinessModuleOptions() {
        return listActiveBusinessModules().stream()
            .map(item -> ArchiveFlowRuleOptionResponse.builder().code(item.getModuleCode()).name(item.getModuleName()).build())
            .toList();
    }

    @Override
    public List<ArchiveFlowRuleOptionResponse> listDocumentOrganizationOptions() {
        return listActiveDocumentOrganizationCodes().stream()
            .map(code -> ArchiveFlowRuleOptionResponse.builder().code(code).name(code).build())
            .toList();
    }

    @Override
    public List<ArchiveFlowRuleOptionResponse> listCityOptions() {
        Map<String, String> cityDisplayMap = buildRegionDisplayMap();
        return listActiveCities().stream()
            .map(item -> ArchiveFlowRuleOptionResponse.builder()
                .code(item.getRegionCode())
                .name(cityDisplayMap.getOrDefault(item.getRegionCode(), item.getRegionName()))
                .build())
            .toList();
    }

    @Override
    public ArchiveFlowRulePermissionPreviewResponse getPermissionPreview() {
        return ArchiveFlowRulePermissionPreviewResponse.builder()
            .moduleCode(MODULE_CODE)
            .moduleName(MODULE_NAME)
            .permissionPoints(List.of(
                ArchiveFlowRulePermissionPreviewResponse.PermissionPoint.builder().code("archive-flow-rule:create").name("Archive Flow Rule Create").action("CREATE").description("Allow create archive flow default rules").build(),
                ArchiveFlowRulePermissionPreviewResponse.PermissionPoint.builder().code("archive-flow-rule:edit").name("Archive Flow Rule Edit").action("UPDATE").description("Allow edit archive flow default rules").build(),
                ArchiveFlowRulePermissionPreviewResponse.PermissionPoint.builder().code("archive-flow-rule:view").name("Archive Flow Rule View").action("READ").description("Allow view archive flow default rules").build()
            ))
            .dataDimensions(List.of(
                ArchiveFlowRulePermissionPreviewResponse.DataDimension.builder().code("COMPANY_PROJECT").name("Company/Project").description("Authorize by company/project dimension").build(),
                ArchiveFlowRulePermissionPreviewResponse.DataDimension.builder().code("BUSINESS_MODULE").name("Business Module").description("Authorize by business module dimension").build(),
                ArchiveFlowRulePermissionPreviewResponse.DataDimension.builder().code("DOCUMENT_ORGANIZATION").name("Document Organization").description("Authorize by document organization dimension").build()
            ))
            .build();
    }

    @Override
    public ArchiveRuleMatchResponse matchArchiveRule(String companyProjectCode,
                                                     String busiModuleCode,
                                                     String customRule,
                                                     String archiveDestination) {
        String company = requireText(companyProjectCode, "companyProjectCode");
        String module = requireText(busiModuleCode, "busiModuleCode");
        ensureCompanyProjectAvailable(company);
        validateType(module);

        CompanyInfo companyInfo = companyInfoMapper.selectOne(new LambdaQueryWrapper<CompanyInfo>()
            .eq(CompanyInfo::getCompanyCode, company)
            .eq(CompanyInfo::getDeleteFlag, "N")
            .eq(CompanyInfo::getEnabledFlag, "Y")
            .last("limit 1"));
        String companyName = companyInfo != null ? companyInfo.getCompanyName() : company;

        BusinessModule businessModule = businessModuleMapper.selectOne(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getModuleCode, module)
            .eq(BusinessModule::getDeleteFlag, "N")
            .eq(BusinessModule::getEnabledFlag, "Y")
            .last("limit 1"));
        String busiModuleName = businessModule != null ? businessModule.getModuleName() : module;

        List<ArchiveFlowRule> rules = archiveFlowRuleMapper.selectList(new LambdaQueryWrapper<ArchiveFlowRule>()
            .eq(ArchiveFlowRule::getCompanyProjectCode, company)
            .eq(ArchiveFlowRule::getBusiModuleCode, module)
            .eq(ArchiveFlowRule::getDeleteFlag, "N")
            .eq(ArchiveFlowRule::getEnabledFlag, "Y")
            .eq(ArchiveFlowRule::getDefaultFlag, "Y"));

        Map<String, String> documentOrganizationNameMap = listActiveDocumentOrganizationCodes().stream()
            .collect(Collectors.toMap(Function.identity(), Function.identity(), (left, right) -> left));
        Map<String, String> cityNameMap = buildRegionDisplayMap();

        if (rules.isEmpty()) {
            return ArchiveRuleMatchResponse.builder()
                .matched(false)
                .companyProjectCode(company)
                .companyName(companyName)
                .busiModuleCode(module)
                .busiModuleName(busiModuleName)
                .build();
        }

        ArchiveFlowRule best = rules.stream()
            .max(Comparator.comparingInt(rule -> scoreRuleForMatch(rule, customRule, archiveDestination)))
            .orElse(null);

        if (best == null) {
            return ArchiveRuleMatchResponse.builder()
                .matched(false)
                .companyProjectCode(company)
                .companyName(companyName)
                .busiModuleCode(module)
                .busiModuleName(busiModuleName)
                .build();
        }

        String dest = StringUtils.hasText(archiveDestination) ? archiveDestination.trim() : best.getArchiveDestination();
        String destDisplayName = StringUtils.hasText(dest) ? cityNameMap.getOrDefault(dest, dest) : null;
        String vis = best.getExternalDisplayFlag();
        String visibilityLabel = "Y".equals(vis) ? "是" : "N".equals(vis) ? "否" : vis;

        return ArchiveRuleMatchResponse.builder()
            .matched(true)
            .companyProjectCode(company)
            .companyName(companyName)
            .busiModuleCode(module)
            .busiModuleName(busiModuleName)
            .customRule(best.getCustomRule())
            .archiveDestination(dest)
            .archiveDestinationName(destDisplayName)
            .documentOrganizationCode(best.getDocumentOrganizationCode())
            .documentOrganizationName(documentOrganizationNameMap.getOrDefault(best.getDocumentOrganizationCode(), best.getDocumentOrganizationCode()))
            .retentionPeriodYears(best.getRetentionPeriodYears())
            .visibleFlag(vis)
            .visibilityLabel(visibilityLabel)
            .build();
    }

    private int scoreRuleForMatch(ArchiveFlowRule rule, String customRule, String archiveDestination) {
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

    private List<CompanyInfo> listActiveCompanies() {
        return companyInfoMapper.selectList(new LambdaQueryWrapper<CompanyInfo>()
            .eq(CompanyInfo::getDeleteFlag, "N")
            .eq(CompanyInfo::getEnabledFlag, "Y")
            .orderByAsc(CompanyInfo::getCompanyCode));
    }

    private List<BusinessModule> listActiveBusinessModules() {
        return businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getDeleteFlag, "N")
            .eq(BusinessModule::getEnabledFlag, "Y")
            .eq(BusinessModule::getLevelNum, 1)
            .orderByAsc(BusinessModule::getSortOrder)
            .orderByAsc(BusinessModule::getModuleCode));
    }

    private List<String> listActiveDocumentOrganizationCodes() {
        return archiveFlowLookupMapper.selectEnabledDocumentOrganizationCodes();
    }

    private List<CountryRegion> listActiveCities() {
        return countryRegionMapper.selectList(new LambdaQueryWrapper<CountryRegion>()
            .eq(CountryRegion::getDeleteFlag, "N")
            .eq(CountryRegion::getEnabledFlag, "Y")
            .eq(CountryRegion::getRegionLevel, "CITY")
            .orderByAsc(CountryRegion::getSortOrder)
            .orderByAsc(CountryRegion::getRegionCode));
    }

    private Map<String, String> buildRegionDisplayMap() {
        List<CountryRegion> allRegions = countryRegionMapper.selectList(new LambdaQueryWrapper<CountryRegion>()
            .eq(CountryRegion::getDeleteFlag, "N")
            .eq(CountryRegion::getEnabledFlag, "Y")
            .in(CountryRegion::getRegionLevel, List.of("COUNTRY", "PROVINCE", "CITY"))
            .orderByAsc(CountryRegion::getSortOrder)
            .orderByAsc(CountryRegion::getRegionCode));
        Map<String, CountryRegion> regionMap = allRegions.stream()
            .collect(Collectors.toMap(CountryRegion::getRegionCode, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return allRegions.stream()
            .filter(item -> List.of("COUNTRY", "PROVINCE", "CITY").contains(item.getRegionLevel()))
            .collect(Collectors.toMap(
                CountryRegion::getRegionCode,
                item -> {
                    if ("COUNTRY".equals(item.getRegionLevel())) {
                        return item.getRegionName();
                    }
                    if ("PROVINCE".equals(item.getRegionLevel())) {
                        CountryRegion country = regionMap.get(item.getParentRegionCode());
                        String countryName = country == null ? "中国" : country.getRegionName();
                        return countryName + "/" + item.getRegionName();
                    }
                    CountryRegion province = regionMap.get(item.getParentRegionCode());
                    CountryRegion country = province == null ? null : regionMap.get(province.getParentRegionCode());
                    String countryName = country == null ? "中国" : country.getRegionName();
                    String provinceName = province == null ? "" : province.getRegionName();
                    return countryName + "/" + provinceName + "/" + item.getRegionName();
                },
                (left, right) -> left,
                LinkedHashMap::new
            ));
    }

    private ArchiveFlowRule findActiveById(Long id) {
        if (id == null) {
            throw new BusinessException("id cannot be null");
        }
        ArchiveFlowRule item = archiveFlowRuleMapper.selectOne(new LambdaQueryWrapper<ArchiveFlowRule>()
            .eq(ArchiveFlowRule::getId, id)
            .eq(ArchiveFlowRule::getDeleteFlag, "N")
            .last("limit 1"));
        if (item == null) {
            throw new BusinessException("Archive flow rule does not exist");
        }
        return item;
    }

    private void ensureBusinessKeyAvailable(String companyProjectCode, Long ignoreId) {
        ArchiveFlowRule existing = archiveFlowRuleMapper.selectOne(new LambdaQueryWrapper<ArchiveFlowRule>()
            .eq(ArchiveFlowRule::getCompanyProjectCode, companyProjectCode)
            .eq(ArchiveFlowRule::getDeleteFlag, "N")
            .last("limit 1"));
        if (existing != null && !existing.getId().equals(ignoreId)) {
            throw new BusinessException("companyProjectCode already exists");
        }
    }

    private void ensureCompanyProjectAvailable(String companyProjectCode) {
        Long count = companyInfoMapper.selectCount(new LambdaQueryWrapper<CompanyInfo>()
            .eq(CompanyInfo::getCompanyCode, companyProjectCode)
            .eq(CompanyInfo::getDeleteFlag, "N")
            .eq(CompanyInfo::getEnabledFlag, "Y"));
        if (count == null || count == 0) {
            throw new BusinessException("companyProjectCode must come from enabled company data");
        }
    }

    private void validateType(String busiModuleCode) {
        Long count = businessModuleMapper.selectCount(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getModuleCode, busiModuleCode)
            .eq(BusinessModule::getDeleteFlag, "N")
            .eq(BusinessModule::getEnabledFlag, "Y"));
        if (count == null || count == 0) {
            throw new BusinessException("busiModuleCode must come from enabled business modules");
        }
    }

    private void validateDocumentOrganization(String documentOrganizationCode) {
        Integer count = archiveFlowLookupMapper.countEnabledDocumentOrganizationCode(documentOrganizationCode);
        if (count == null || count == 0) {
            throw new BusinessException("documentOrganizationCode must come from enabled document organizations");
        }
    }

    private void validateRegion(String archiveDestination) {
        if (!StringUtils.hasText(archiveDestination)) {
            return;
        }
        Long count = countryRegionMapper.selectCount(new LambdaQueryWrapper<CountryRegion>()
            .eq(CountryRegion::getRegionCode, archiveDestination.trim())
            .in(CountryRegion::getRegionLevel, List.of("COUNTRY", "PROVINCE", "CITY"))
            .eq(CountryRegion::getDeleteFlag, "N")
            .eq(CountryRegion::getEnabledFlag, "Y"));
        if (count == null || count == 0) {
            throw new BusinessException("archiveDestination must come from enabled country/province/city dictionary entries");
        }
    }

    private void validateRetentionPeriodYears(Integer years) {
        if (years == null || years < 0) {
            throw new BusinessException("retentionPeriodYears must be a non-negative number");
        }
    }

    private void validateFlag(String flag, String fieldName) {
        if (!"Y".equals(flag) && !"N".equals(flag)) {
            throw new BusinessException(fieldName + " only supports Y or N");
        }
    }

    private ArchiveFlowRuleSummaryResponse toSummary(ArchiveFlowRule item,
                                                     Map<String, String> companyProjectNameMap,
                                                     Map<String, String> busiModuleNameMap,
                                                     Map<String, String> documentOrganizationNameMap,
                                                     Map<String, String> cityNameMap) {
        ArchiveFlowRuleSummaryResponse response = new ArchiveFlowRuleSummaryResponse();
        response.setId(item.getId());
        response.setCompanyProjectCode(item.getCompanyProjectCode());
        response.setCompanyProjectName(companyProjectNameMap.getOrDefault(item.getCompanyProjectCode(), item.getCompanyProjectCode()));
        response.setBusiModuleCode(item.getBusiModuleCode());
        response.setBusiModuleName(busiModuleNameMap.getOrDefault(item.getBusiModuleCode(), item.getBusiModuleCode()));
        response.setCustomRule(item.getCustomRule());
        response.setArchiveDestination(item.getArchiveDestination());
        response.setArchiveDestinationName(cityNameMap.getOrDefault(item.getArchiveDestination(), item.getArchiveDestination()));
        response.setDocumentOrganizationCode(item.getDocumentOrganizationCode());
        response.setDocumentOrganizationName(documentOrganizationNameMap.getOrDefault(item.getDocumentOrganizationCode(), item.getDocumentOrganizationCode()));
        response.setRetentionPeriodYears(item.getRetentionPeriodYears());
        response.setExternalDisplayFlag(item.getExternalDisplayFlag());
        response.setDefaultFlag(item.getDefaultFlag());
        response.setEnabledFlag(item.getEnabledFlag());
        response.setLastUpdatedBy(item.getLastUpdatedBy());
        response.setLastUpdateDate(item.getLastUpdateDate());
        return response;
    }

    private ArchiveFlowRuleDetailResponse toDetail(ArchiveFlowRule item) {
        ArchiveFlowRuleDetailResponse response = new ArchiveFlowRuleDetailResponse();
        response.setId(item.getId());
        response.setCompanyProjectCode(item.getCompanyProjectCode());
        response.setBusiModuleCode(item.getBusiModuleCode());
        response.setCustomRule(item.getCustomRule());
        response.setArchiveDestination(item.getArchiveDestination());
        response.setDocumentOrganizationCode(item.getDocumentOrganizationCode());
        response.setRetentionPeriodYears(item.getRetentionPeriodYears());
        response.setExternalDisplayFlag(item.getExternalDisplayFlag());
        response.setDefaultFlag(item.getDefaultFlag());
        response.setEnabledFlag(item.getEnabledFlag());
        response.setDeleteFlag(item.getDeleteFlag());
        response.setCreatedBy(item.getCreatedBy());
        response.setCreationDate(item.getCreationDate());
        response.setLastUpdatedBy(item.getLastUpdatedBy());
        response.setLastUpdateDate(item.getLastUpdateDate());
        return response;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
