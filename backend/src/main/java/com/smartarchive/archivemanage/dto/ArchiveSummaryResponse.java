package com.smartarchive.archivemanage.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArchiveSummaryResponse {
    private Long archiveId;
    private String archiveCode;
    private String documentTypeCode;
    private String documentTypeName;
    private String companyProjectCode;
    private String companyProjectName;
    private String beginPeriod;
    private String endPeriod;
    private String documentName;
    private String businessCode;
    private String dutyPerson;
    private String createdBy;
    private String dutyDepartment;
    private LocalDateTime documentDate;
    private String securityLevelCode;
    /** 密级中文名，与 securityLevelCode 成对；未知字典值时与编码相同 */
    private String securityLevelName;
    private String sourceSystem;
    private String archiveDestination;
    private String originPlace;
    private String carrierTypeCode;
    private String remark;
    private String aiArchiveSummary;
    private String documentOrganizationCode;
    private Integer retentionPeriodYears;
    private String archiveTypeCode;
    /** 三级业务模块原始 type_code（fdc_document_t.biz_module_code），与 archiveTypeCode（展示名）区分 */
    private String businessModuleTypeCode;
    private String archiveStatus;
    /** 生命周期状态原始编码（如 UNARCHIVED/ARCHIVED/DRAFT） */
    private String lifecycleStatus;
    private String custodyStatus;
    /** 是否可见（fdc_document_t.visible_flag 映射为「是」「否」），在归档信息区展示 */
    private String documentVisibility;
    private String parseStatus;
    private String vectorStatus;
    private LocalDateTime lastUpdateDate;
    private Integer attachmentCount;
    private Map<String, String> extValues;
    private List<ArchiveAttachmentResponse> attachments;

    // Backward-compatible alias for merged branches still using busiModuleName.
    public String getBusiModuleName() {
        return documentTypeName;
    }
}
