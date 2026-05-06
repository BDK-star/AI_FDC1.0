package com.smartarchive.barcodemodule.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("fdc_barcode_module_t")
public class BarcodeModule {
    @TableId(value = "barcode_module_id", type = IdType.AUTO)
    private Long barcodeId;

    @TableField("barcode_module_code")
    private String barcodeCode;

    @TableField("barcode_module_name")
    private String barcodeName;
    private String description;
    private String enableFlag;
    @TableLogic(value = "N", delval = "Y")
    private String deleteFlag;
    private Long createdBy;
    private LocalDateTime creationDate;
    private Long lastUpdatedBy;
    private LocalDateTime lastUpdateDate;
}
