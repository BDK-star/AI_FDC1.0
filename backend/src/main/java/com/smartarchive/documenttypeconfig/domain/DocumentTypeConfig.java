package com.smartarchive.documenttypeconfig.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("fdc_document_type_t")
public class DocumentTypeConfig {
    @TableId(value = "document_type_id", type = IdType.AUTO)
    private Long documentTypeId;
    private String docTypeCode;
    private String docTypeDescription;
    private String parentCode;
    private Integer levelNum;
    private String ancestorPath;
    private Integer sortOrder;
    private String enableFlag;
    private String deleteFlag;
    private Long createdBy;
    private LocalDateTime creationDate;
    private Long lastUpdatedBy;
    private LocalDateTime lastUpdateDate;
    private String sysDescription;
    private String lastUpdateTraceId;
    private Integer lastUpdateVersion;
    private Long tenantid;
}
