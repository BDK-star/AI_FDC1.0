package com.smartarchive.archivemanage.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ArchiveCreateCommand {
    private String sessionCode;
    private String createMode;
    private String busiModuleCode;
    private String companyProjectCode;
    private String beginPeriod;
    private String endPeriod;
    private String businessCode;
    private String documentName;
    private String dutyPerson;
    private Long dutyPersonId;
    private String dutyDepartment;
    private LocalDate documentDate;
    private String securityLevelCode;
    private String sourceSystem;
    private String archiveDestination;
    private String originPlace;
    private String carrierTypeCode;
    private String remark;
    private String aiArchiveSummary;
    private String documentOrganizationCode;
    private Integer retentionPeriodYears;
    private String archiveTypeCode;
    private String countryCode;
    private String customRule;
    private List<ArchiveAttachmentSaveCommand> attachments = new ArrayList<>();
    private List<ArchiveAttachmentSaveCommand> paperScanAttachments = new ArrayList<>();
    private ArchivePaperSaveCommand paperInfo;
    private Map<String, String> extValues;

    // Backward-compatible alias for merged branches still using documentTypeCode.
    public String getDocumentTypeCode() {
        return busiModuleCode;
    }

    // Backward-compatible alias for merged branches still using documentTypeCode.
    public void setDocumentTypeCode(String documentTypeCode) {
        this.busiModuleCode = documentTypeCode;
    }

    @Data
    public static class ArchiveAttachmentSaveCommand {
        private Long attachmentId;
        private String attachmentRole;
        private String attachmentTypeCode;
        private Integer versionNo;
        private String remark;
    }

    @Data
    public static class ArchivePaperSaveCommand {
        private Integer plannedCopyCount;
        private Integer actualCopyCount;
        private String remark;
    }
}
