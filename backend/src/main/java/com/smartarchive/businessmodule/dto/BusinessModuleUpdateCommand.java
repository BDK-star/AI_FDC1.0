package com.smartarchive.businessmodule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessModuleUpdateCommand {
    @NotBlank
    private String moduleName;
    private String parentCode;
    private String enabledFlag;
    private Integer sortOrder;
    private String securityLevelCode;
    private String integrationType;
    private String description;
    private String remark;
    /**
     * 为 true 时按 barcodeModuleCode 更新条码映射（null 表示解除）；
     * 为 false 时忽略 barcodeModuleCode，保留库中原值（避免表单未带出条码时误传 null 清空）；
     * 未传该字段时视为 true，兼容旧客户端。
     */
    private Boolean patchBarcodeModuleCode;
    /** 条码模块编码；仅在 patchBarcodeModuleCode 为 true（或未传 patch）时生效；null 或空表示解除映射 */
    private String barcodeModuleCode;
}
