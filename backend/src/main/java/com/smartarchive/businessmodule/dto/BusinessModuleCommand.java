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
    /** 条码模块主键，可空表示不映射 */
    /** 条码模块编码（与条码模块主数据编码一致），不映射可省略 */
    private String barcodeModuleCode;
}
