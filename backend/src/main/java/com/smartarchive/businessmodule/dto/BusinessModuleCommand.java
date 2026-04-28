package com.smartarchive.businessmodule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessModuleCommand {
    @NotBlank
    private String moduleCode;
    @NotBlank
    private String moduleName;
    private String parentCode;
    private String enabledFlag;
    private Integer sortOrder;
    private String securityLevelCode;
    private String integrationType;
    private String description;
    private String remark;
}
