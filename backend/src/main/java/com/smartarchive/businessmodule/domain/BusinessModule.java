package com.smartarchive.businessmodule.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("fdc_business_module_t")
public class BusinessModule {
    @TableId(value = "business_module_id", type = IdType.AUTO)
    private Long id;
    private String moduleCode;
    private String moduleName;
    private String parentCode;
    private Integer levelNum;
    private String ancestorPath;
    private String enabledFlag;
    private String securityLevel;
    private String integrationType;
    private String description;
    private String remark;
    private Integer sortOrder;
    /** 映射条码模块编码（fdc_barcode_module_t.barcode_module_code），多业务模块可共用同一条码模块 */
    @TableField(value = "barcode_module_code", updateStrategy = FieldStrategy.ALWAYS)
    private String barcodeModuleCode;
    @TableLogic(value = "N", delval = "Y")
    private String deleteFlag;
    private Long createdBy;
    private LocalDateTime creationDate;
    private Long lastUpdatedBy;
    private LocalDateTime lastUpdateDate;
}
