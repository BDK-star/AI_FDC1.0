package com.smartarchive.documentorganization.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartarchive.common.audit.service.OperationAuditService;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.companyproject.domain.Country;
import com.smartarchive.companyproject.dto.CountryResponse;
import com.smartarchive.companyproject.mapper.CountryMapper;
import com.smartarchive.documentorganization.domain.DocumentOrganization;
import com.smartarchive.documentorganization.domain.DocumentOrganizationCity;
import com.smartarchive.documentorganization.dto.DocumentOrganizationCityResponse;
import com.smartarchive.documentorganization.dto.DocumentOrganizationCreateCommand;
import com.smartarchive.documentorganization.dto.DocumentOrganizationDetailResponse;
import com.smartarchive.documentorganization.dto.DocumentOrganizationPermissionPreviewResponse;
import com.smartarchive.documentorganization.dto.DocumentOrganizationSummaryResponse;
import com.smartarchive.documentorganization.dto.DocumentOrganizationUpdateCommand;
import com.smartarchive.documentorganization.mapper.DocumentOrganizationCityMapper;
import com.smartarchive.documentorganization.mapper.DocumentOrganizationMapper;
import com.smartarchive.documentorganization.service.DocumentOrganizationService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DocumentOrganizationServiceImpl implements DocumentOrganizationService {
    private static final Long SYSTEM_OPERATOR_ID = 1L;
    private static final String SYSTEM_OPERATOR_NAME = "system";
    private static final String MODULE_CODE = "DOCUMENT_ORGANIZATION";
    private static final String MODULE_NAME = "Document Organization Management";

    private final DocumentOrganizationMapper documentOrganizationMapper;
    private final DocumentOrganizationCityMapper documentOrganizationCityMapper;
    private final CountryMapper countryMapper;
    private final OperationAuditService operationAuditService;

    @Override
    public List<DocumentOrganizationSummaryResponse> list(String keyword, String documentOrganizationName, String countryCode, String cityCode, String enabledFlag) {
        return documentOrganizationMapper.selectList(new LambdaQueryWrapper<DocumentOrganization>()
                .eq(DocumentOrganization::getDeleteFlag, "N")
                .like(StringUtils.hasText(keyword), DocumentOrganization::getDocumentOrganizationCode, keyword == null ? null : keyword.trim())
                .like(StringUtils.hasText(documentOrganizationName), DocumentOrganization::getDocumentOrganizationName, documentOrganizationName == null ? null : documentOrganizationName.trim())
                .eq(StringUtils.hasText(countryCode), DocumentOrganization::getCountryCode, trimToNull(countryCode))
                .eq(StringUtils.hasText(cityCode), DocumentOrganization::getCityCode, trimToNull(cityCode))
                .eq(StringUtils.hasText(enabledFlag), DocumentOrganization::getEnabledFlag, trimToNull(enabledFlag))
                .orderByDesc(DocumentOrganization::getLastUpdateDate)
                .orderByAsc(DocumentOrganization::getDocumentOrganizationCode))
            .stream()
            .map(this::toSummary)
            .toList();
    }

    @Override
    public DocumentOrganizationDetailResponse getDetail(String documentOrganizationCode) {
        return toDetail(findActiveByCode(requireText(documentOrganizationCode, "documentOrganizationCode")));
    }

    @Override
    public List<CountryResponse> listCountries() {
        return countryMapper.selectList(new LambdaQueryWrapper<Country>()
                .eq(Country::getDeleteFlag, "N")
                .eq(Country::getEnabledFlag, "Y")
                .orderByAsc(Country::getSortOrder)
                .orderByAsc(Country::getCountryCode))
            .stream()
            .map(item -> CountryResponse.builder().countryCode(item.getCountryCode()).countryName(item.getCountryName()).build())
            .toList();
    }

    @Override
    public List<DocumentOrganizationCityResponse> listCities(String countryCode) {
        return documentOrganizationCityMapper.selectList(new LambdaQueryWrapper<DocumentOrganizationCity>()
                .eq(DocumentOrganizationCity::getDeleteFlag, "N")
                .eq(DocumentOrganizationCity::getEnabledFlag, "Y")
                .eq(StringUtils.hasText(countryCode), DocumentOrganizationCity::getCountryCode, trimToNull(countryCode))
                .orderByAsc(DocumentOrganizationCity::getSortOrder)
                .orderByAsc(DocumentOrganizationCity::getCityCode))
            .stream()
            .map(item -> DocumentOrganizationCityResponse.builder()
                .countryCode(item.getCountryCode())
                .cityCode(item.getCityCode())
                .cityName(item.getCityName())
                .build())
            .toList();
    }

    @Override
    @Transactional
    public DocumentOrganizationDetailResponse create(DocumentOrganizationCreateCommand command) {
        validateEnabledFlag(requireText(command.getEnabledFlag(), "enabledFlag"), "enabledFlag");
        String code = requireText(command.getDocumentOrganizationCode(), "documentOrganizationCode");
        String name = requireText(command.getDocumentOrganizationName(), "documentOrganizationName");
        String description = requireText(command.getDescription(), "description");
        String countryCode = requireText(command.getCountryCode(), "countryCode");
        String cityCode = trimToNull(command.getCityCode());
        validateCountry(countryCode);
        validateCity(countryCode, cityCode);
        ensureCodeAvailable(code, null);

        LocalDateTime now = LocalDateTime.now();
        DocumentOrganization entity = new DocumentOrganization();
        entity.setDocumentOrganizationCode(code);
        entity.setDocumentOrganizationName(name);
        entity.setDescription(description);
        entity.setCountryCode(countryCode);
        entity.setCityCode(cityCode);
        entity.setEnabledFlag(command.getEnabledFlag().trim());
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(now);
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(now);
        documentOrganizationMapper.insert(entity);

        DocumentOrganizationDetailResponse after = toDetail(entity);
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "DOCUMENT_ORGANIZATION", code, "CREATE", "Create document organization", null, after, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return after;
    }

    @Override
    @Transactional
    public DocumentOrganizationDetailResponse update(String documentOrganizationCode, DocumentOrganizationUpdateCommand command) {
        DocumentOrganization existing = findActiveByCode(requireText(documentOrganizationCode, "documentOrganizationCode"));
        DocumentOrganizationDetailResponse before = toDetail(existing);
        validateEnabledFlag(requireText(command.getEnabledFlag(), "enabledFlag"), "enabledFlag");
        String name = requireText(command.getDocumentOrganizationName(), "documentOrganizationName");
        String description = requireText(command.getDescription(), "description");
        String countryCode = requireText(command.getCountryCode(), "countryCode");
        String cityCode = trimToNull(command.getCityCode());
        validateCountry(countryCode);
        validateCity(countryCode, cityCode);

        existing.setDocumentOrganizationName(name);
        existing.setDescription(description);
        existing.setCountryCode(countryCode);
        existing.setCityCode(cityCode);
        existing.setEnabledFlag(command.getEnabledFlag().trim());
        existing.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        existing.setLastUpdateDate(LocalDateTime.now());
        documentOrganizationMapper.updateById(existing);

        DocumentOrganizationDetailResponse after = toDetail(existing);
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "DOCUMENT_ORGANIZATION", existing.getDocumentOrganizationCode(), "UPDATE", "Update document organization", before, after, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
        return after;
    }

    @Override
    @Transactional
    public void delete(String documentOrganizationCode) {
        DocumentOrganization existing = findActiveByCode(requireText(documentOrganizationCode, "documentOrganizationCode"));
        DocumentOrganizationDetailResponse before = toDetail(existing);
        documentOrganizationMapper.update(null, new LambdaUpdateWrapper<DocumentOrganization>()
            .eq(DocumentOrganization::getId, existing.getId())
            .eq(DocumentOrganization::getDeleteFlag, "N")
            .set(DocumentOrganization::getDeleteFlag, "Y")
            .set(DocumentOrganization::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(DocumentOrganization::getLastUpdateDate, LocalDateTime.now()));
        operationAuditService.record(MODULE_CODE, MODULE_NAME, "DOCUMENT_ORGANIZATION", existing.getDocumentOrganizationCode(), "DELETE", "Soft delete document organization", before, null, SYSTEM_OPERATOR_ID, SYSTEM_OPERATOR_NAME);
    }

    @Override
    public DocumentOrganizationPermissionPreviewResponse getPermissionPreview() {
        return DocumentOrganizationPermissionPreviewResponse.builder()
            .moduleCode(MODULE_CODE)
            .moduleName(MODULE_NAME)
            .permissionPoints(List.of(
                DocumentOrganizationPermissionPreviewResponse.PermissionPoint.builder().code("document-organization:create").name("Document Organization Create").action("CREATE").description("Allow create document organization master data").build(),
                DocumentOrganizationPermissionPreviewResponse.PermissionPoint.builder().code("document-organization:edit").name("Document Organization Edit").action("UPDATE").description("Allow edit document organization master data").build(),
                DocumentOrganizationPermissionPreviewResponse.PermissionPoint.builder().code("document-organization:view").name("Document Organization View").action("READ").description("Allow view document organization list and detail").build()
            ))
            .dataDimensions(List.of(
                DocumentOrganizationPermissionPreviewResponse.DataDimension.builder().code("DOCUMENT_ORGANIZATION_CODE").name("Document Organization Code").description("Authorize by document organization business key").build(),
                DocumentOrganizationPermissionPreviewResponse.DataDimension.builder().code("COUNTRY").name("Country").description("Authorize by country dimension").build(),
                DocumentOrganizationPermissionPreviewResponse.DataDimension.builder().code("CITY").name("City").description("Authorize by city dimension").build()
            ))
            .build();
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

    private void validateCity(String countryCode, String cityCode) {
        if (!StringUtils.hasText(cityCode)) {
            return;
        }
        Long count = documentOrganizationCityMapper.selectCount(new LambdaQueryWrapper<DocumentOrganizationCity>()
            .eq(DocumentOrganizationCity::getCountryCode, countryCode)
            .eq(DocumentOrganizationCity::getCityCode, cityCode)
            .eq(DocumentOrganizationCity::getDeleteFlag, "N")
            .eq(DocumentOrganizationCity::getEnabledFlag, "Y"));
        if (count == null || count == 0) {
            throw new BusinessException("cityCode must come from enabled dictionary entries under the selected country");
        }
    }

    private DocumentOrganization findActiveByCode(String documentOrganizationCode) {
        DocumentOrganization item = documentOrganizationMapper.selectOne(new LambdaQueryWrapper<DocumentOrganization>()
            .eq(DocumentOrganization::getDocumentOrganizationCode, documentOrganizationCode)
            .eq(DocumentOrganization::getDeleteFlag, "N")
            .last("limit 1"));
        if (item == null) {
            throw new BusinessException("Document organization does not exist");
        }
        return item;
    }

    private void ensureCodeAvailable(String documentOrganizationCode, Long ignoreId) {
        DocumentOrganization existing = documentOrganizationMapper.selectOne(new LambdaQueryWrapper<DocumentOrganization>()
            .eq(DocumentOrganization::getDocumentOrganizationCode, documentOrganizationCode)
            .eq(DocumentOrganization::getDeleteFlag, "N")
            .last("limit 1"));
        if (existing != null && !existing.getId().equals(ignoreId)) {
            throw new BusinessException("documentOrganizationCode already exists");
        }
    }

    private DocumentOrganizationSummaryResponse toSummary(DocumentOrganization item) {
        DocumentOrganizationSummaryResponse response = new DocumentOrganizationSummaryResponse();
        response.setId(item.getId());
        response.setDocumentOrganizationCode(item.getDocumentOrganizationCode());
        response.setDocumentOrganizationName(item.getDocumentOrganizationName());
        response.setDescription(item.getDescription());
        response.setCountryCode(item.getCountryCode());
        response.setCityCode(item.getCityCode());
        response.setEnabledFlag(item.getEnabledFlag());
        response.setLastUpdatedBy(item.getLastUpdatedBy());
        response.setLastUpdateDate(item.getLastUpdateDate());
        return response;
    }

    private DocumentOrganizationDetailResponse toDetail(DocumentOrganization item) {
        DocumentOrganizationDetailResponse response = new DocumentOrganizationDetailResponse();
        response.setId(item.getId());
        response.setDocumentOrganizationCode(item.getDocumentOrganizationCode());
        response.setDocumentOrganizationName(item.getDocumentOrganizationName());
        response.setDescription(item.getDescription());
        response.setCountryCode(item.getCountryCode());
        response.setCityCode(item.getCityCode());
        response.setEnabledFlag(item.getEnabledFlag());
        response.setDeleteFlag(item.getDeleteFlag());
        response.setCreatedBy(item.getCreatedBy());
        response.setCreationDate(item.getCreationDate());
        response.setLastUpdatedBy(item.getLastUpdatedBy());
        response.setLastUpdateDate(item.getLastUpdateDate());
        return response;
    }

    private void validateEnabledFlag(String flag, String fieldName) {
        if (!"Y".equals(flag) && !"N".equals(flag)) {
            throw new BusinessException(fieldName + " only supports Y or N");
        }
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
