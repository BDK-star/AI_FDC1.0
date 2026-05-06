package com.smartarchive.archiveflow.controller;

import com.smartarchive.archiveflow.dto.ArchiveFlowRuleCreateCommand;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleDetailResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleOptionResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRulePermissionPreviewResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleSummaryResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleUpdateCommand;
import com.smartarchive.archiveflow.dto.ArchiveRuleMatchResponse;
import com.smartarchive.archiveflow.service.ArchiveFlowRuleService;
import com.smartarchive.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/base-data/archive-flow-rules")
@RequiredArgsConstructor
public class ArchiveFlowRuleController {
    private final ArchiveFlowRuleService archiveFlowRuleService;

    @GetMapping
    public ApiResponse<List<ArchiveFlowRuleSummaryResponse>> list(@RequestParam(required = false) String keyword,
                                                                  @RequestParam(required = false) String companyProjectCode,
                                                                  @RequestParam(required = false) String busiModuleCode,
                                                                  @RequestParam(required = false) String documentOrganizationCode,
                                                                  @RequestParam(required = false) String enabledFlag) {
        return ApiResponse.success(archiveFlowRuleService.list(keyword, companyProjectCode, busiModuleCode, documentOrganizationCode, enabledFlag));
    }

    @GetMapping("/options/company-projects")
    public ApiResponse<List<ArchiveFlowRuleOptionResponse>> listCompanyProjects() {
        return ApiResponse.success(archiveFlowRuleService.listCompanyProjectOptions());
    }

    @GetMapping("/options/business-modules")
    public ApiResponse<List<ArchiveFlowRuleOptionResponse>> listBusinessModules() {
        return ApiResponse.success(archiveFlowRuleService.listBusinessModuleOptions());
    }

    @GetMapping("/options/document-organizations")
    public ApiResponse<List<ArchiveFlowRuleOptionResponse>> listDocumentOrganizations() {
        return ApiResponse.success(archiveFlowRuleService.listDocumentOrganizationOptions());
    }

    @GetMapping("/options/cities")
    public ApiResponse<List<ArchiveFlowRuleOptionResponse>> listCities() {
        return ApiResponse.success(archiveFlowRuleService.listCityOptions());
    }

    @GetMapping("/options/archive-destinations")
    public ApiResponse<List<ArchiveFlowRuleOptionResponse>> listMatchedArchiveDestinations(@RequestParam String companyProjectCode,
                                                                                            @RequestParam String busiModuleCode) {
        return ApiResponse.success(archiveFlowRuleService.listMatchedArchiveDestinationOptions(companyProjectCode, busiModuleCode));
    }

    @GetMapping("/permissions/preview")
    public ApiResponse<ArchiveFlowRulePermissionPreviewResponse> permissionPreview() {
        return ApiResponse.success(archiveFlowRuleService.getPermissionPreview());
    }

    /**
     * 归档规则匹配：公司 + 业务模块必填；可选自定义匹配条件、归档地（参与评分）。
     */
    @GetMapping("/match")
    public ApiResponse<ArchiveRuleMatchResponse> matchArchiveRule(@RequestParam String companyProjectCode,
                                                                  @RequestParam String busiModuleCode,
                                                                  @RequestParam(required = false) String customRule,
                                                                  @RequestParam(required = false) String archiveDestination) {
        return ApiResponse.success(archiveFlowRuleService.matchArchiveRule(companyProjectCode, busiModuleCode, customRule, archiveDestination));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArchiveFlowRuleDetailResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(archiveFlowRuleService.getDetail(id));
    }

    @PostMapping
    public ApiResponse<ArchiveFlowRuleDetailResponse> create(@Valid @RequestBody ArchiveFlowRuleCreateCommand command) {
        return ApiResponse.success(archiveFlowRuleService.create(command));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArchiveFlowRuleDetailResponse> update(@PathVariable Long id,
                                                             @Valid @RequestBody ArchiveFlowRuleUpdateCommand command) {
        return ApiResponse.success(archiveFlowRuleService.update(id, command));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        archiveFlowRuleService.delete(id);
        return ApiResponse.success(null);
    }
}
