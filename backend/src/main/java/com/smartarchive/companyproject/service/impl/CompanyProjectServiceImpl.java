package com.smartarchive.companyproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartarchive.common.audit.service.OperationAuditService;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.companyproject.domain.CompanyProject;
import com.smartarchive.companyproject.domain.CompanyProjectLine;
import com.smartarchive.companyproject.domain.CompanyProjectOrgCategory;
import com.smartarchive.companyproject.domain.Country;
import com.smartarchive.companyproject.dto.CompanyProjectCreateCommand;
import com.smartarchive.companyproject.dto.CompanyProjectDetailResponse;
import com.smartarchive.companyproject.dto.CompanyProjectLineCommand;
import com.smartarchive.companyproject.dto.CompanyProjectLineResponse;
import com.smartarchive.companyproject.dto.CompanyProjectOrgCategoryResponse;
import com.smartarchive.companyproject.dto.CompanyProjectPermissionPreviewResponse;
import com.smartarchive.companyproject.dto.CompanyProjectSummaryResponse;
import com.smartarchive.companyproject.dto.CompanyProjectUpdateCommand;
import com.smartarchive.companyproject.dto.CountryDictionaryCommand;
import com.smartarchive.companyproject.dto.CountryDictionaryItemResponse;
import com.smartarchive.companyproject.dto.CountryResponse;
import com.smartarchive.companyproject.dto.OrgCategoryDictionaryCommand;
import com.smartarchive.companyproject.dto.OrgCategoryDictionaryItemResponse;
import com.smartarchive.companyproject.mapper.CompanyProjectLineMapper;
import com.smartarchive.companyproject.mapper.CompanyProjectMapper;
import com.smartarchive.companyproject.mapper.CompanyProjectOrgCategoryMapper;
import com.smartarchive.companyproject.mapper.CountryMapper;
import com.smartarchive.companyproject.service.CompanyProjectService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CompanyProjectServiceImpl implements CompanyProjectService {
    private static final Long SYSTEM_OPERATOR_ID = 1L;
    private static final String SYSTEM_OPERATOR_NAME = "system";
    private static final String MODULE_CODE = "COMPANY_PROJECT";
    private static final String MODULE_NAME = "Company/Project Management";

    private final CompanyProjectMapper companyProjectMapper;
    private final CompanyProjectLineMapper companyProjectLineMapper;
    private final CompanyProjectOrgCategoryMapper companyProjectOrgCategoryMapper;
    private final CountryMapper countryMapper;
    private final OperationAuditService operationAuditService;

    @Override
    public List<CompanyProjectSummaryResponse> list(String keyword, String countryCode, String managementArea, String enabledFlag) {
        return companyProjectMapper.selectList(new LambdaQueryWrapper<CompanyProject>()
                .eq(CompanyProject::getDeleteFlag, "N")
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                    .like(CompanyProject::getCompanyProjectCode, keyword.trim())
                    .or()
                    .like(CompanyProject::getCompanyProjectName, keyword.trim()))
                .eq(StringUtils.hasText(countryCode), CompanyProject::getCountryCode, trimToNull(countryCode))
                .like(StringUtils.hasText(managementArea), CompanyProject::getManagementArea, trimToNull(managementArea))
                .eq(StringUtils.hasText(enabledFlag), CompanyProject::getEnabledFlag, trimToNull(enabledFlag))
                .orderByDesc(CompanyProject::getLastUpdateDate)
                .orderByAsc(CompanyProject::getCompanyProjectCode))
            .stream()
            .map(this::toSummary)
            .toList();
    }

    @Override
    public CompanyProjectDetailResponse getDetail(String companyProjectCode) {
        return toDetail(findActiveByCode(requireText(companyProjectCode, "companyProjectCode")));
    }

    @Override
    public List<CountryResponse> listCountries() {
        return countryMapper.selectList(activeCountryWrapper(true)).stream()
            .map(item -> CountryResponse.builder().countryCode(item.getCountryCode()).countryName(item.getCountryName()).build())
            .toList();
    }

    @Override
    public List<CountryDictionaryItemResponse> listCountryDictionaries() {
        return countryMapper.selectList(activeCountryWrapper(false)).stream().map(this::toCountryDictionaryItem).toList();
    }

    @Override
    @Transactional
    public CountryDictionaryItemResponse createCountryDictionary(CountryDictionaryCommand command) {
        validateEnabledFlag(requireText(command.getEnabledFlag(), "enabledFlag"), "enabledFlag");
        validateSortOrder(command.getSortOrder(), "sortOrder");
        String countryCode = requireText(command.getCountryCode(), "countryCode");
        ensureCountryCodeAvailable(countryCode, null);

        LocalDateTime now = LocalDateTime.now();
        Country entity = new Country();
        entity.setCountryCode(countryCode);
        entity.setCountryName(requireText(command.getCountryName(), "countryName"));
        entity.setSortOrder(command.getSortOrder());
        entity.setEnabledFlag(command.getEnabledFlag().trim());
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(now);
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(now);
        countryMapper.insert(entity);

        CountryDictionaryItemResponse after = toCountryDictionaryItem(entity);
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "COUNTRY_DICTIONARY", countryCode, "CREATE", "Create country dictionary", null, after, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return after;
    }

    @Override
    @Transactional
    public CountryDictionaryItemResponse updateCountryDictionary(Long id, CountryDictionaryCommand command) {
        Country existing = findActiveCountry(id);
        CountryDictionaryItemResponse before = toCountryDictionaryItem(existing);
        validateEnabledFlag(requireText(command.getEnabledFlag(), "enabledFlag"), "enabledFlag");
        validateSortOrder(command.getSortOrder(), "sortOrder");
        String countryCode = requireText(command.getCountryCode(), "countryCode");
        ensureCountryCodeAvailable(countryCode, existing.getId());

        existing.setCountryCode(countryCode);
        existing.setCountryName(requireText(command.getCountryName(), "countryName"));
        existing.setSortOrder(command.getSortOrder());
        existing.setEnabledFlag(command.getEnabledFlag().trim());
        existing.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        existing.setLastUpdateDate(LocalDateTime.now());
        countryMapper.updateById(existing);

        CountryDictionaryItemResponse after = toCountryDictionaryItem(existing);
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "COUNTRY_DICTIONARY", countryCode, "UPDATE", "Update country dictionary", before, after, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return after;
    }

    @Override
    @Transactional
    public void deleteCountryDictionary(Long id) {
        Country existing = findActiveCountry(id);
        if (isCountryReferenced(existing.getCountryCode())) {
            throw new BusinessException("Country dictionary is referenced by company/project data");
        }
        CountryDictionaryItemResponse before = toCountryDictionaryItem(existing);
        countryMapper.update(null, new LambdaUpdateWrapper<Country>()
            .eq(Country::getId, existing.getId())
            .eq(Country::getDeleteFlag, "N")
            .set(Country::getDeleteFlag, "Y")
            .set(Country::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(Country::getLastUpdateDate, LocalDateTime.now()));
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "COUNTRY_DICTIONARY", existing.getCountryCode(), "DELETE", "Soft delete country dictionary", before, null, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
    }

    @Override
    public List<CompanyProjectOrgCategoryResponse> listOrgCategories() {
        return companyProjectOrgCategoryMapper.selectList(activeOrgCategoryWrapper(true)).stream()
            .map(item -> CompanyProjectOrgCategoryResponse.builder().categoryCode(item.getCategoryCode()).categoryName(item.getCategoryName()).build())
            .toList();
    }

    @Override
    public List<OrgCategoryDictionaryItemResponse> listOrgCategoryDictionaries() {
        return companyProjectOrgCategoryMapper.selectList(activeOrgCategoryWrapper(false)).stream().map(this::toOrgCategoryDictionaryItem).toList();
    }

    @Override
    @Transactional
    public OrgCategoryDictionaryItemResponse createOrgCategoryDictionary(OrgCategoryDictionaryCommand command) {
        validateEnabledFlag(requireText(command.getEnabledFlag(), "enabledFlag"), "enabledFlag");
        validateSortOrder(command.getSortOrder(), "sortOrder");
        String categoryCode = requireText(command.getCategoryCode(), "categoryCode");
        ensureOrgCategoryCodeAvailable(categoryCode, null);

        LocalDateTime now = LocalDateTime.now();
        CompanyProjectOrgCategory entity = new CompanyProjectOrgCategory();
        entity.setCategoryCode(categoryCode);
        entity.setCategoryName(requireText(command.getCategoryName(), "categoryName"));
        entity.setSortOrder(command.getSortOrder());
        entity.setEnabledFlag(command.getEnabledFlag().trim());
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(now);
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(now);
        companyProjectOrgCategoryMapper.insert(entity);

        OrgCategoryDictionaryItemResponse after = toOrgCategoryDictionaryItem(entity);
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "ORG_CATEGORY_DICTIONARY", categoryCode, "CREATE", "Create org category dictionary", null, after, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return after;
    }

    @Override
    @Transactional
    public OrgCategoryDictionaryItemResponse updateOrgCategoryDictionary(Long id, OrgCategoryDictionaryCommand command) {
        CompanyProjectOrgCategory existing = findActiveOrgCategory(id);
        OrgCategoryDictionaryItemResponse before = toOrgCategoryDictionaryItem(existing);
        validateEnabledFlag(requireText(command.getEnabledFlag(), "enabledFlag"), "enabledFlag");
        validateSortOrder(command.getSortOrder(), "sortOrder");
        String categoryCode = requireText(command.getCategoryCode(), "categoryCode");
        ensureOrgCategoryCodeAvailable(categoryCode, existing.getId());

        existing.setCategoryCode(categoryCode);
        existing.setCategoryName(requireText(command.getCategoryName(), "categoryName"));
        existing.setSortOrder(command.getSortOrder());
        existing.setEnabledFlag(command.getEnabledFlag().trim());
        existing.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        existing.setLastUpdateDate(LocalDateTime.now());
        companyProjectOrgCategoryMapper.updateById(existing);

        OrgCategoryDictionaryItemResponse after = toOrgCategoryDictionaryItem(existing);
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "ORG_CATEGORY_DICTIONARY", categoryCode, "UPDATE", "Update org category dictionary", before, after, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return after;
    }

    @Override
    @Transactional
    public void deleteOrgCategoryDictionary(Long id) {
        CompanyProjectOrgCategory existing = findActiveOrgCategory(id);
        if (isOrgCategoryReferenced(existing.getCategoryCode())) {
            throw new BusinessException("Org category dictionary is referenced by company/project data");
        }
        OrgCategoryDictionaryItemResponse before = toOrgCategoryDictionaryItem(existing);
        companyProjectOrgCategoryMapper.update(null, new LambdaUpdateWrapper<CompanyProjectOrgCategory>()
            .eq(CompanyProjectOrgCategory::getId, existing.getId())
            .eq(CompanyProjectOrgCategory::getDeleteFlag, "N")
            .set(CompanyProjectOrgCategory::getDeleteFlag, "Y")
            .set(CompanyProjectOrgCategory::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(CompanyProjectOrgCategory::getLastUpdateDate, LocalDateTime.now()));
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "ORG_CATEGORY_DICTIONARY", existing.getCategoryCode(), "DELETE", "Soft delete org category dictionary", before, null, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
    }

    @Override
    @Transactional
    public CompanyProjectDetailResponse create(CompanyProjectCreateCommand command) {
        validateEnabledFlag(requireText(command.getEnabledFlag(), "enabledFlag"), "enabledFlag");
        validateCountry(requireText(command.getCountryCode(), "countryCode"));
        validateLines(command.getLines());
        String code = requireText(command.getCompanyProjectCode(), "companyProjectCode");
        ensureCodeAvailable(code, null);

        LocalDateTime now = LocalDateTime.now();
        CompanyProject entity = new CompanyProject();
        entity.setCompanyProjectCode(code);
        entity.setCompanyProjectName(requireText(command.getCompanyProjectName(), "companyProjectName"));
        entity.setCountryCode(requireText(command.getCountryCode(), "countryCode"));
        entity.setManagementArea(requireText(command.getManagementArea(), "managementArea"));
        entity.setCompanyTag(trimToNull(command.getCompanyTag()));
        entity.setEnabledFlag(requireText(command.getEnabledFlag(), "enabledFlag"));
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(now);
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(now);
        companyProjectMapper.insert(entity);
        replaceLines(code, command.getLines(), now);

        CompanyProjectDetailResponse after = getDetail(code);
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "COMPANY_PROJECT", code, "CREATE", "Create company/project", null, after, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return after;
    }

    @Override
    @Transactional
    public CompanyProjectDetailResponse update(String companyProjectCode, CompanyProjectUpdateCommand command) {
        validateEnabledFlag(requireText(command.getEnabledFlag(), "enabledFlag"), "enabledFlag");
        validateCountry(requireText(command.getCountryCode(), "countryCode"));
        validateLines(command.getLines());
        CompanyProject existing = findActiveByCode(requireText(companyProjectCode, "companyProjectCode"));
        CompanyProjectDetailResponse before = toDetail(existing);

        LocalDateTime now = LocalDateTime.now();
        existing.setCompanyProjectName(requireText(command.getCompanyProjectName(), "companyProjectName"));
        existing.setCountryCode(requireText(command.getCountryCode(), "countryCode"));
        existing.setManagementArea(requireText(command.getManagementArea(), "managementArea"));
        existing.setCompanyTag(trimToNull(command.getCompanyTag()));
        existing.setEnabledFlag(requireText(command.getEnabledFlag(), "enabledFlag"));
        existing.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        existing.setLastUpdateDate(now);
        companyProjectMapper.updateById(existing);

        companyProjectLineMapper.update(null, new LambdaUpdateWrapper<CompanyProjectLine>()
            .eq(CompanyProjectLine::getCompanyProjectCode, existing.getCompanyProjectCode())
            .eq(CompanyProjectLine::getDeleteFlag, "N")
            .set(CompanyProjectLine::getDeleteFlag, "Y")
            .set(CompanyProjectLine::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(CompanyProjectLine::getLastUpdateDate, now));
        replaceLines(existing.getCompanyProjectCode(), command.getLines(), now);

        CompanyProjectDetailResponse after = getDetail(existing.getCompanyProjectCode());
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "COMPANY_PROJECT", existing.getCompanyProjectCode(), "UPDATE", "Update company/project", before, after, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return after;
    }

    @Override
    @Transactional
    public void delete(String companyProjectCode) {
        CompanyProject existing = findActiveByCode(requireText(companyProjectCode, "companyProjectCode"));
        CompanyProjectDetailResponse before = toDetail(existing);
        LocalDateTime now = LocalDateTime.now();

        companyProjectMapper.update(null, new LambdaUpdateWrapper<CompanyProject>()
            .eq(CompanyProject::getId, existing.getId())
            .eq(CompanyProject::getDeleteFlag, "N")
            .set(CompanyProject::getDeleteFlag, "Y")
            .set(CompanyProject::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(CompanyProject::getLastUpdateDate, now));

        companyProjectLineMapper.update(null, new LambdaUpdateWrapper<CompanyProjectLine>()
            .eq(CompanyProjectLine::getCompanyProjectCode, existing.getCompanyProjectCode())
            .eq(CompanyProjectLine::getDeleteFlag, "N")
            .set(CompanyProjectLine::getDeleteFlag, "Y")
            .set(CompanyProjectLine::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(CompanyProjectLine::getLastUpdateDate, now));

        operationAuditService.record(MODULE_CODE, MODULE_NAME, "COMPANY_PROJECT", existing.getCompanyProjectCode(), "DELETE", "Soft delete company/project", before, null, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
    }

    @Override
    public CompanyProjectPermissionPreviewResponse getPermissionPreview() {
        return CompanyProjectPermissionPreviewResponse.builder()
            .moduleCode(MODULE_CODE)
            .moduleName(MODULE_NAME)
            .permissionPoints(List.of(
                CompanyProjectPermissionPreviewResponse.PermissionPoint.builder().code("company-project:create").name("Company/Project Create").action("CREATE").description("Allow create company/project master data").build(),
                CompanyProjectPermissionPreviewResponse.PermissionPoint.builder().code("company-project:edit").name("Company/Project Edit").action("UPDATE").description("Allow edit company/project head and lines").build(),
                CompanyProjectPermissionPreviewResponse.PermissionPoint.builder().code("company-project:view").name("Company/Project View").action("READ").description("Allow view company/project list and detail").build()
            ))
            .dataDimensions(List.of(
                CompanyProjectPermissionPreviewResponse.DataDimension.builder().code("COMPANY_PROJECT_CODE").name("Company/Project Code").description("Authorize by business code").build(),
                CompanyProjectPermissionPreviewResponse.DataDimension.builder().code("COUNTRY").name("Country").description("Authorize by country dimension").build(),
                CompanyProjectPermissionPreviewResponse.DataDimension.builder().code("MANAGEMENT_AREA").name("Management Area").description("Authorize by management area dimension").build()
            ))
            .build();
    }

    private LambdaQueryWrapper<Country> activeCountryWrapper(boolean enabledOnly) {
        return new LambdaQueryWrapper<Country>()
            .eq(Country::getDeleteFlag, "N")
            .eq(enabledOnly, Country::getEnabledFlag, "Y")
            .orderByAsc(Country::getSortOrder)
            .orderByAsc(Country::getCountryCode);
    }

    private LambdaQueryWrapper<CompanyProjectOrgCategory> activeOrgCategoryWrapper(boolean enabledOnly) {
        return new LambdaQueryWrapper<CompanyProjectOrgCategory>()
            .eq(CompanyProjectOrgCategory::getDeleteFlag, "N")
            .eq(enabledOnly, CompanyProjectOrgCategory::getEnabledFlag, "Y")
            .orderByAsc(CompanyProjectOrgCategory::getSortOrder)
            .orderByAsc(CompanyProjectOrgCategory::getCategoryCode);
    }

    private void validateCountry(String countryCode) {
        Long count = countryMapper.selectCount(new LambdaQueryWrapper<Country>()
            .eq(Country::getCountryCode, countryCode)
            .eq(Country::getDeleteFlag, "N")
            .eq(Country::getEnabledFlag, "Y"));
        if (count == null || count == 0) {
            throw new BusinessException("countryCode must come from enabled dictionary entries");
        }
    }

    private Country findActiveCountry(Long id) {
        Country item = countryMapper.selectOne(new LambdaQueryWrapper<Country>().eq(Country::getId, id).eq(Country::getDeleteFlag, "N").last("limit 1"));
        if (item == null) throw new BusinessException("Country dictionary does not exist");
        return item;
    }

    private void ensureCountryCodeAvailable(String countryCode, Long ignoreId) {
        Country existing = countryMapper.selectOne(new LambdaQueryWrapper<Country>().eq(Country::getCountryCode, countryCode).eq(Country::getDeleteFlag, "N").last("limit 1"));
        if (existing != null && !existing.getId().equals(ignoreId)) throw new BusinessException("countryCode already exists");
    }

    private boolean isCountryReferenced(String countryCode) {
        return companyProjectMapper.selectCount(new LambdaQueryWrapper<CompanyProject>().eq(CompanyProject::getCountryCode, countryCode).eq(CompanyProject::getDeleteFlag, "N")) > 0;
    }

    private CountryDictionaryItemResponse toCountryDictionaryItem(Country item) {
        CountryDictionaryItemResponse response = new CountryDictionaryItemResponse();
        response.setId(item.getId());
        response.setCountryCode(item.getCountryCode());
        response.setCountryName(item.getCountryName());
        response.setSortOrder(item.getSortOrder());
        response.setEnabledFlag(item.getEnabledFlag());
        return response;
    }

    private CompanyProjectOrgCategory findActiveOrgCategory(Long id) {
        CompanyProjectOrgCategory item = companyProjectOrgCategoryMapper.selectOne(new LambdaQueryWrapper<CompanyProjectOrgCategory>().eq(CompanyProjectOrgCategory::getId, id).eq(CompanyProjectOrgCategory::getDeleteFlag, "N").last("limit 1"));
        if (item == null) throw new BusinessException("Org category dictionary does not exist");
        return item;
    }

    private void ensureOrgCategoryCodeAvailable(String categoryCode, Long ignoreId) {
        CompanyProjectOrgCategory existing = companyProjectOrgCategoryMapper.selectOne(new LambdaQueryWrapper<CompanyProjectOrgCategory>().eq(CompanyProjectOrgCategory::getCategoryCode, categoryCode).eq(CompanyProjectOrgCategory::getDeleteFlag, "N").last("limit 1"));
        if (existing != null && !existing.getId().equals(ignoreId)) throw new BusinessException("categoryCode already exists");
    }

    private boolean isOrgCategoryReferenced(String categoryCode) {
        return companyProjectLineMapper.selectCount(new LambdaQueryWrapper<CompanyProjectLine>().eq(CompanyProjectLine::getOrgCategory, categoryCode).eq(CompanyProjectLine::getDeleteFlag, "N")) > 0;
    }

    private OrgCategoryDictionaryItemResponse toOrgCategoryDictionaryItem(CompanyProjectOrgCategory item) {
        OrgCategoryDictionaryItemResponse response = new OrgCategoryDictionaryItemResponse();
        response.setId(item.getId());
        response.setCategoryCode(item.getCategoryCode());
        response.setCategoryName(item.getCategoryName());
        response.setSortOrder(item.getSortOrder());
        response.setEnabledFlag(item.getEnabledFlag());
        return response;
    }

    private CompanyProject findActiveByCode(String companyProjectCode) {
        CompanyProject item = companyProjectMapper.selectOne(new LambdaQueryWrapper<CompanyProject>().eq(CompanyProject::getCompanyProjectCode, companyProjectCode).eq(CompanyProject::getDeleteFlag, "N").last("limit 1"));
        if (item == null) throw new BusinessException("Company/project does not exist");
        return item;
    }

    private void ensureCodeAvailable(String companyProjectCode, Long ignoreId) {
        CompanyProject existing = companyProjectMapper.selectOne(new LambdaQueryWrapper<CompanyProject>().eq(CompanyProject::getCompanyProjectCode, companyProjectCode).eq(CompanyProject::getDeleteFlag, "N").last("limit 1"));
        if (existing != null && !existing.getId().equals(ignoreId)) throw new BusinessException("companyProjectCode already exists");
    }

    private void validateLines(List<CompanyProjectLineCommand> lines) {
        Set<String> organizationCodes = new HashSet<>();
        Set<String> categories = listOrgCategories().stream().map(CompanyProjectOrgCategoryResponse::getCategoryCode).collect(Collectors.toSet());
        for (CompanyProjectLineCommand line : lines) {
            validateEnabledFlag(requireText(line.getValidFlag(), "validFlag"), "validFlag");
            String orgCategory = requireText(line.getOrgCategory(), "orgCategory");
            if (!categories.contains(orgCategory)) throw new BusinessException("orgCategory must come from enabled dictionary entries");
            String organizationCode = requireText(line.getOrganizationCode(), "organizationCode");
            if (!organizationCodes.add(organizationCode)) throw new BusinessException("organizationCode cannot repeat");
            requireText(line.getOrganizationName(), "organizationName");
        }
    }

    private void replaceLines(String companyProjectCode, List<CompanyProjectLineCommand> lines, LocalDateTime now) {
        for (int index = 0; index < lines.size(); index++) {
            CompanyProjectLineCommand line = lines.get(index);
            CompanyProjectLine entity = new CompanyProjectLine();
            entity.setCompanyProjectCode(companyProjectCode);
            entity.setLineNo(index + 1);
            entity.setOrgCategory(requireText(line.getOrgCategory(), "orgCategory"));
            entity.setOrganizationCode(requireText(line.getOrganizationCode(), "organizationCode"));
            entity.setOrganizationName(requireText(line.getOrganizationName(), "organizationName"));
            entity.setValidFlag(requireText(line.getValidFlag(), "validFlag"));
            entity.setDeleteFlag("N");
            entity.setCreatedBy(SYSTEM_OPERATOR_ID);
            entity.setCreationDate(now);
            entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
            entity.setLastUpdateDate(now);
            companyProjectLineMapper.insert(entity);
        }
    }

    private List<CompanyProjectLine> listActiveLines(String companyProjectCode) {
        return companyProjectLineMapper.selectList(new LambdaQueryWrapper<CompanyProjectLine>().eq(CompanyProjectLine::getCompanyProjectCode, companyProjectCode).eq(CompanyProjectLine::getDeleteFlag, "N").orderByAsc(CompanyProjectLine::getLineNo).orderByAsc(CompanyProjectLine::getOrganizationCode));
    }

    private CompanyProjectSummaryResponse toSummary(CompanyProject item) {
        CompanyProjectSummaryResponse response = new CompanyProjectSummaryResponse();
        response.setId(item.getId());
        response.setCompanyProjectCode(item.getCompanyProjectCode());
        response.setCompanyProjectName(item.getCompanyProjectName());
        response.setCountryCode(item.getCountryCode());
        response.setManagementArea(item.getManagementArea());
        response.setCompanyTag(item.getCompanyTag());
        response.setEnabledFlag(item.getEnabledFlag());
        response.setOrganizationCount(listActiveLines(item.getCompanyProjectCode()).size());
        response.setLastUpdatedBy(item.getLastUpdatedBy());
        response.setLastUpdateDate(item.getLastUpdateDate());
        return response;
    }

    private CompanyProjectDetailResponse toDetail(CompanyProject item) {
        CompanyProjectDetailResponse response = new CompanyProjectDetailResponse();
        response.setId(item.getId());
        response.setCompanyProjectCode(item.getCompanyProjectCode());
        response.setCompanyProjectName(item.getCompanyProjectName());
        response.setCountryCode(item.getCountryCode());
        response.setManagementArea(item.getManagementArea());
        response.setCompanyTag(item.getCompanyTag());
        response.setEnabledFlag(item.getEnabledFlag());
        response.setDeleteFlag(item.getDeleteFlag());
        response.setCreatedBy(item.getCreatedBy());
        response.setCreationDate(item.getCreationDate());
        response.setLastUpdatedBy(item.getLastUpdatedBy());
        response.setLastUpdateDate(item.getLastUpdateDate());
        response.setLines(listActiveLines(item.getCompanyProjectCode()).stream().map(this::toLineResponse).toList());
        return response;
    }

    private CompanyProjectLineResponse toLineResponse(CompanyProjectLine item) {
        CompanyProjectLineResponse response = new CompanyProjectLineResponse();
        response.setId(item.getId());
        response.setLineNo(item.getLineNo());
        response.setOrgCategory(item.getOrgCategory());
        response.setOrganizationCode(item.getOrganizationCode());
        response.setOrganizationName(item.getOrganizationName());
        response.setValidFlag(item.getValidFlag());
        response.setDeleteFlag(item.getDeleteFlag());
        response.setCreatedBy(item.getCreatedBy());
        response.setCreationDate(item.getCreationDate());
        response.setLastUpdatedBy(item.getLastUpdatedBy());
        response.setLastUpdateDate(item.getLastUpdateDate());
        return response;
    }

    private void validateEnabledFlag(String flag, String fieldName) {
        if (!"Y".equals(flag) && !"N".equals(flag)) throw new BusinessException(fieldName + " only supports Y or N");
    }

    private void validateSortOrder(Integer sortOrder, String fieldName) {
        if (sortOrder == null || sortOrder < 1) throw new BusinessException(fieldName + " must be greater than 0");
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) throw new BusinessException(fieldName + " cannot be blank");
        return value.trim();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) return null;
        return value.trim();
    }
}

