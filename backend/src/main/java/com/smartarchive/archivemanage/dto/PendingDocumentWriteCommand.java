package com.smartarchive.archivemanage.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 应归档数据（fdc_document_t，未归档）创建与更新载荷。
 */
@Data
public class PendingDocumentWriteCommand {
    /** 一级文档类型编码 */
    private String documentTypeCode;
    private String companyProjectCode;
    /** 三级业务模块 type_code */
    private String archiveTypeCode;
    private String businessCode;
    private String beginPeriod;
    private String endPeriod;
    private String archiveDestination;
    private String originPlace;
    private String documentName;
    /** yyyy-MM-dd HH:mm:ss */
    private String documentDate;
    /** 归档责任人（兼容文本输入：user_name / employee_no / "user_name employee_no"） */
    private String dutyPerson;
    /** 归档责任人ID（tpl_user_t.user_id）；优先于 dutyPerson 文本解析 */
    private Long dutyPersonId;
    /** 可填数字或文本；非数字时落0 */
    private String dutyDepartment;
    private String carrierTypeCode;
    private String sourceSystem;
    private String securityLevelCode;
    private String remark;
    private String documentOrganizationCode;
    private Integer retentionPeriodYears;
    /** 如 UNARCHIVED，默认 UNARCHIVED */
    private String custodyStatus;
    /**
     * SUBMIT：正式保存（默认）；DRAFT：保存草稿（lifecycle_status=DRAFT，校验从宽）。
     */
    @JsonAlias("submit_mode")
    private String submitMode;
    /** 本次操作备注，写入操作审计 */
    @JsonAlias("operation_remark")
    private String operationRemark;
    /** 操作类型编码（如 BATCH_CREATE/BATCH_UPDATE），不传则由后端按场景默认 */
    private String operationTypeCode;
    /** 补充说明附件（先上传接口拿到 storageKey 再提交） */
    private List<PendingAuditAttachmentRef> auditAttachments = new ArrayList<>();
    private Map<String, String> extValues = new LinkedHashMap<>();
    /**
     * 操作人（落库 created_by / last_updated_by）。未传时沿用系统默认操作员。
     * 接入统一登录后由网关或前端传入当前用户 {@code tpl_user_t.user_id}。
     */
    private Long operatorUserId;
}
