package com.smartarchive.archiveflow.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("fdc_archive_rule_t")
public class ArchiveFlowRule {
    @TableId(value = "archive_rule_id", type = IdType.AUTO)
    private Long id;
    @TableField("company_code")
    private String companyProjectCode;
    @TableField("module_code")
    private String busiModuleCode;
    @TableField("cust_mapping_code")
    private String customRule;
    @TableField("arch_place_alpha2_code")
    private String archiveDestination;
    @TableField("document_organization_code")
    private String documentOrganizationCode;
    @TableField("retention_term")
    private Integer retentionPeriodYears;
    @TableField("visible_flag")
    private String externalDisplayFlag;
    private String defaultFlag;
    @TableField("enable_flag")
    private String enabledFlag;
    @TableLogic(value = "N", delval = "Y")
    private String deleteFlag;
    private Long createdBy;
    private LocalDateTime creationDate;
    private Long lastUpdatedBy;
    private LocalDateTime lastUpdateDate;

    // Backward-compatible alias for merged branches still using documentTypeCode.
    public String getDocumentTypeCode() {
        return busiModuleCode;
    }

    // Backward-compatible alias for merged branches still using documentTypeCode.
    public void setDocumentTypeCode(String documentTypeCode) {
        this.busiModuleCode = documentTypeCode;
    }
}
