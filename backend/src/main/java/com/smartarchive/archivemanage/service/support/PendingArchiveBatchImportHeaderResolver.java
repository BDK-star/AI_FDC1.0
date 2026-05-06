package com.smartarchive.archivemanage.service.support;

import com.smartarchive.archivemanage.dto.DocumentTypeExtFieldResponse;
import com.smartarchive.businessmodule.dto.BusinessModuleExtFieldResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.util.StringUtils;

/**
 * 将导入 CSV 表头（中文展示名或英文键）解析为应归档写入命令使用的规范字段键。
 * 中文名与应归档创建页 / 详情页展示保持一致。
 */
public final class PendingArchiveBatchImportHeaderResolver {

    private static final Set<String> CANONICAL_KEYS = Set.of(
        "companyProjectCode", "archiveTypeCode", "businessCode", "beginPeriod", "endPeriod",
        "archiveDestination", "originPlace", "documentName", "documentDate", "dutyPerson",
        "dutyDepartment", "carrierTypeCode", "sourceSystem", "securityLevelCode", "remark",
        "documentOrganizationCode", "retentionPeriodYears", "custodyStatus", "visibility", "barcodeModule",
        "country", "repOffice", "region", "companyTag", "invoiceNo", "refNo", "accountant", "scannedBy",
        "issueDateRange", "maturityDateRange", "lgExpiryDateRange", "lgLedgerStatus", "bankName",
        "currency", "amount", "issuingAuthority", "disposalTimeRange", "businessVolumeNo",
        "lgWorkflowNo", "lgNo"
    );

    private static final Map<String, String> HEADER_TO_CANONICAL = new LinkedHashMap<>();

    static {
        putCn("文档业务编码", "businessCode");
        putCn("公司", "companyProjectCode");
        putCn("子公司", "companyProjectCode");
        putCn("业务模块", "archiveTypeCode");
        putCn("开始档期", "beginPeriod");
        putCn("结束档期", "endPeriod");
        putCn("归档地", "archiveDestination");
        putCn("产生地", "originPlace");
        putCn("文档名称", "documentName");
        putCn("文档生成日期", "documentDate");
        putCn("归档责任人", "dutyPerson");
        putCn("文档责任部门", "dutyDepartment");
        putCn("载体类型", "carrierTypeCode");
        putCn("系统来源", "sourceSystem");
        putCn("密级", "securityLevelCode");
        putCn("密级编码", "securityLevelCode");
        putCn("描述", "remark");
        putCn("文档组织", "documentOrganizationCode");
        putCn("是否可见", "visibility");
        putCn("条码模块", "barcodeModule");
        putCn("保管状态", "custodyStatus");
        putCn("保管年限", "retentionPeriodYears");

        putCn("国家", "country");
        putCn("代表处", "repOffice");
        putCn("地区部", "region");
        putCn("公司标签", "companyTag");
        putCn("发票号", "invoiceNo");
        putCn("其他相关编号", "refNo");
        putCn("其他文档编号", "refNo");
        putCn("会计", "accountant");
        putCn("扫描员", "scannedBy");
        putCn("开立日期", "issueDateRange");
        putCn("到期日", "maturityDateRange");
        putCn("保函失效日期", "lgExpiryDateRange");
        putCn("保函台账状态", "lgLedgerStatus");
        putCn("银行名称", "bankName");
        putCn("币种", "currency");
        putCn("金额", "amount");
        putCn("签发机构", "issuingAuthority");
        putCn("报废时间", "disposalTimeRange");
        putCn("业务册号", "businessVolumeNo");
        putCn("保函电子流编号", "lgWorkflowNo");
        putCn("保函编号", "lgNo");

        for (String k : CANONICAL_KEYS) {
            HEADER_TO_CANONICAL.putIfAbsent(k, k);
        }
    }

    private static void putCn(String label, String canonical) {
        HEADER_TO_CANONICAL.put(label.trim(), canonical);
    }

    private PendingArchiveBatchImportHeaderResolver() {
    }

    /**
     * @param fieldDisplayNameToCode 扩展字段「显示名称」→ fieldCode（来自文档类型有效扩展配置）
     */
    public static String resolve(String rawHeader, Map<String, String> fieldDisplayNameToCode) {
        if (!StringUtils.hasText(rawHeader)) {
            return "";
        }
        String t = rawHeader.trim();
        String mapped = HEADER_TO_CANONICAL.get(t);
        if (mapped != null) {
            return mapped;
        }
        String norm = t.toLowerCase(Locale.ROOT);
        String lowerHit = HEADER_TO_CANONICAL.entrySet().stream()
            .filter(e -> e.getKey().toLowerCase(Locale.ROOT).equals(norm))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
        if (lowerHit != null) {
            return lowerHit;
        }
        if (fieldDisplayNameToCode != null && fieldDisplayNameToCode.containsKey(t)) {
            return fieldDisplayNameToCode.get(t);
        }
        return t;
    }

    public static Map<String, String> buildFieldDisplayNameToCodeMap(List<DocumentTypeExtFieldResponse> fields) {
        Map<String, String> m = new LinkedHashMap<>();
        if (fields == null) {
            return m;
        }
        for (DocumentTypeExtFieldResponse f : fields) {
            if (f == null || !"Y".equalsIgnoreCase(f.getEnabledFlag())) {
                continue;
            }
            String code = f.getFieldCode() != null ? f.getFieldCode().trim() : "";
            if (!StringUtils.hasText(code)) {
                continue;
            }
            m.putIfAbsent(code, code);
            String name = f.getFieldName() != null ? f.getFieldName().trim() : "";
            if (StringUtils.hasText(name)) {
                m.putIfAbsent(name, code);
            }
        }
        return m;
    }

    /**
     * 业务模块扩展：中文名 / fieldCode → 写入应归档使用的键（优先 englishFieldName，否则 fieldCode）。
     */
    public static Map<String, String> buildBusinessModuleExtDisplayToKeyMap(List<BusinessModuleExtFieldResponse> fields) {
        Map<String, String> m = new LinkedHashMap<>();
        if (fields == null) {
            return m;
        }
        for (BusinessModuleExtFieldResponse f : fields) {
            if (f == null || !"Y".equalsIgnoreCase(f.getEnabledFlag())) {
                continue;
            }
            String en = f.getEnglishFieldName() != null ? f.getEnglishFieldName().trim() : "";
            String fc = f.getFieldCode() != null ? f.getFieldCode().trim() : "";
            String canonical = StringUtils.hasText(en) ? en : fc;
            if (!StringUtils.hasText(canonical)) {
                continue;
            }
            m.putIfAbsent(canonical, canonical);
            if (StringUtils.hasText(fc)) {
                m.putIfAbsent(fc, canonical);
            }
            String name = f.getFieldName() != null ? f.getFieldName().trim() : "";
            if (StringUtils.hasText(name)) {
                m.putIfAbsent(name, canonical);
            }
        }
        return m;
    }
}
