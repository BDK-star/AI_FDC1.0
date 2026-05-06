package com.smartarchive.archiveflow.service;

import com.smartarchive.archiveflow.dto.ArchiveFlowRuleCreateCommand;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleDetailResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleOptionResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRulePermissionPreviewResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleSummaryResponse;
import com.smartarchive.archiveflow.dto.ArchiveFlowRuleUpdateCommand;
import com.smartarchive.archiveflow.dto.ArchiveRuleMatchResponse;
import java.util.List;

public interface ArchiveFlowRuleService {
    List<ArchiveFlowRuleSummaryResponse> list(String keyword,
                                              String companyProjectCode,
                                              String busiModuleCode,
                                              String documentOrganizationCode,
                                              String enabledFlag);

    ArchiveFlowRuleDetailResponse getDetail(Long id);

    ArchiveFlowRuleDetailResponse create(ArchiveFlowRuleCreateCommand command);

    ArchiveFlowRuleDetailResponse update(Long id, ArchiveFlowRuleUpdateCommand command);

    void delete(Long id);

    List<ArchiveFlowRuleOptionResponse> listCompanyProjectOptions();

    List<ArchiveFlowRuleOptionResponse> listBusinessModuleOptions();

    List<ArchiveFlowRuleOptionResponse> listDocumentOrganizationOptions();

    List<ArchiveFlowRuleOptionResponse> listCityOptions();

    /**
     * 按公司 + 业务模块返回可选归档地（仅默认且启用的规则）。
     */
    List<ArchiveFlowRuleOptionResponse> listMatchedArchiveDestinationOptions(String companyProjectCode,
                                                                             String busiModuleCode);

    ArchiveFlowRulePermissionPreviewResponse getPermissionPreview();

    /**
     * 按公司、业务模块（及可选自定义条件、归档地）匹配「默认规则=是、启用=是」的归档流向规则。
     */
    ArchiveRuleMatchResponse matchArchiveRule(String companyProjectCode,
                                             String busiModuleCode,
                                             String customRule,
                                             String archiveDestination);
}
