package com.smartarchive.archivemanage.dto;

import lombok.Data;

@Data
public class ArchiveDefaultResolveResponse {
    private String securityLevelCode;
    private String archiveDestination;
    private String documentOrganizationCode;
    private Integer retentionPeriodYears;
    private String countryCode;
    /** 命中规则上的 cust_mapping_code（自定义匹配条件），供「归档规则匹配」接口展示 */
    private String resolvedCustMappingCode;
    /** 与规则表 visible_flag 一致 */
    private String externalDisplayFlag;
}
