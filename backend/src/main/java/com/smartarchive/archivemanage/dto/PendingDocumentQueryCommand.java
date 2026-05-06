package com.smartarchive.archivemanage.dto;

import java.util.List;
import lombok.Data;

@Data
public class PendingDocumentQueryCommand {
    private String documentTypeCode;
    private String companyCode;
    private String archiveTypeCode;
    /** 多个业务模块编码（或） */
    private List<String> archiveTypeCodes;
    private String carrierType;
    /** 多个载体类型（或） */
    private List<String> carrierTypes;
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
    /** 实物保管状态字典编码（fdc_document_t.custody_status）；列表默认仍为 lifecycle=未归档 */
    private String custodyStatus;
    private String country;
    private String repOffice;
    private String region;
    private String dutyPerson;
    /** 仅返回指定用户创建的文档（fdc_document_t.created_by） */
    private Long createdByUserId;

    /** 文档业务模块所映射的条码模块编码，多选为「或」 */
    private List<String> barcodeModuleCodes;

    /** 档案类型编码（fdc_document_t.arch_type_code），多选为「或」 */
    private List<String> documentArchiveTypeCodes;

    /** 归档地叶子编码，与归档流向规则中级联选址一致 */
    private String archiveDestination;

    /** 产生地：国家编码（国家维表） */
    private String originPlace;
}
