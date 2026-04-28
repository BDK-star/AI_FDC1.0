package com.smartarchive.archivemanage.dto;

import java.util.List;
import lombok.Data;

@Data
public class PendingDocumentQueryCommand {
    private String documentTypeCode;
    private String companyCode;
    private String archiveTypeCode;
    private String carrierType;
    private String businessCode;
    /**
     * 多条业务编码（推荐）：前端解析后显式传数组，避免依赖 JSON 字符串内换行经网关/代理后丢失，
     * 导致后端只收到单行而走模糊/拼接逻辑、查无结果。
     */
    private List<String> businessCodes;
    /** 发票号，多个值空格分隔，与 {@link #refNo}、{@link #businessCode} 同时存在时取交集 */
    private String invoiceNo;
    /** 其他相关编号，多个值换行/逗号分隔（与 {@link #refNos} 二选一优先数组） */
    private String refNo;
    /** 多条其他相关编号（推荐）：与 businessCodes 同理，避免换行在传输中丢失导致不加过滤条件 */
    private List<String> refNos;
    private String docOrganization;
    private String beginPeriod;
    private String endPeriod;
    private String docGenerationStart;
    private String docGenerationEnd;
    private String custodyStatus;
    private String country;
    private String repOffice;
    private String region;
    private String dutyPerson;
    /** 仅返回指定用户创建的文档（fdc_document_t.created_by） */
    private Long createdByUserId;
}
