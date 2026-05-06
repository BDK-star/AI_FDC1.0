package com.smartarchive.archivemanage.dto;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ArchiveQueryCommand {
    private String keyword;
    private String busiModuleCode;
    private String companyProjectCode;
    /** 单个业务模块编码（兼容旧前端）；与 archiveTypeCodes 二选一或并存时合并 */
    private String archiveTypeCode;
    /** 多个业务模块编码（或）；优先级高于单值 */
    private List<String> archiveTypeCodes;
    private String carrierTypeCode;
    /** 多个载体类型（或） */
    private List<String> carrierTypeCodes;
    private String securityLevelCode;
    private String beginPeriod;
    private String endPeriod;
    private String documentName;
    private String businessCode;
    private String dutyPerson;
    private String archiveDestination;
    private String sourceSystem;
    private String documentOrganizationCode;
    private Map<String, String> extFilters;
    private Boolean excludeSubmittedTransferApplied;
    private Integer page = 1;
    private Integer pageSize = 20;

    /** 业务模块所映射的条码模块编码（配置中心已启用的条码模块），多选为「或」 */
    private List<String> barcodeModuleCodes;

    // Backward-compatible alias for merged branches still using documentTypeCode.
    public String getDocumentTypeCode() {
        return busiModuleCode;
    }

    // Backward-compatible alias for merged branches still using documentTypeCode.
    public void setDocumentTypeCode(String documentTypeCode) {
        this.busiModuleCode = documentTypeCode;
    }
}
