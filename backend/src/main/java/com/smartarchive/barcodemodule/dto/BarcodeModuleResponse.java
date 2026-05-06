package com.smartarchive.barcodemodule.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BarcodeModuleResponse {
    private Long barcodeId;
    private String barcodeCode;
    private String barcodeName;
    private String description;
    private String enableFlag;
    private Long lastUpdatedBy;
    private LocalDateTime lastUpdateDate;
    /** 已关联该条码模块的叶子业务模块数量（列表接口填充） */
    private Integer linkedBusinessModuleCount;
}
