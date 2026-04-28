package com.smartarchive.archivemanage.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransferApplicationResponse {
    private Long applicationId;
    private String applicationNumber;
    private Long applicant;
    private String applicantName;
    private LocalDateTime applicationDate;
    private String department;
    private String busiModuleCode;
    private String applyMethod;
    private String expressType;
    private String expressNumber;
    private Long documentRecipient;
    private String documentRecipientName;
    private String handoverForm;
    private String carrierType;
    private String applicationStatus;
    private String status;
    private String diffReasonCode;
    private String applicationDescription;
    private String enableFlag;
    private String deleteFlag;
    private Long tenantid;
    private LocalDateTime creationDate;
    private LocalDateTime lastUpdateDate;
    /** 审批通过后是否已写入档案库 Y/N */
    private String archivesMaterialized;
    private List<TransferApplicationDetailResponse> details;
}
