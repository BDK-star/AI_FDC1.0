package com.smartarchive.archivemanage.service.support;

import com.smartarchive.businessmodule.dto.BusinessModuleExtFieldResponse;
import com.smartarchive.businessmodule.service.BusinessModuleService;
import com.smartarchive.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 应归档批量导入 CSV 模板（与前端 {@code pendingArchiveBatchImportTemplate.ts} 规则一致，由服务端生成以避免浏览器缓存旧脚本）。
 * 后半部分列为文档类型根及其子树业务模块上 BASIC、应用功能含应归档数据且已启用的扩展字段并集。
 * 不再使用「硬编码扩展黑名单」：凡业务模块并集中出现的字段均可进入模板，仅与固定核心列 {@link #CORE_LABEL_AND_KEY} 按英文键去重。
 */
@Service
@RequiredArgsConstructor
public class PendingBatchImportTemplateService {

    private static final List<Map.Entry<String, String>> CORE_LABEL_AND_KEY = List.of(
        Map.entry("文档业务编码", "businessCode"),
        Map.entry("公司", "companyProjectCode"),
        Map.entry("业务模块", "archiveTypeCode"),
        Map.entry("开始档期", "beginPeriod"),
        Map.entry("结束档期", "endPeriod"),
        Map.entry("归档地", "archiveDestination"),
        Map.entry("产生地", "originPlace"),
        Map.entry("文档名称", "documentName"),
        Map.entry("文档生成日期", "documentDate"),
        Map.entry("归档责任人", "dutyPerson"),
        Map.entry("载体类型", "carrierTypeCode"),
        Map.entry("系统来源", "sourceSystem"),
        Map.entry("密级", "securityLevelCode"),
        Map.entry("描述", "remark"),
        Map.entry("保管状态", "custodyStatus")
    );

    private final BusinessModuleService businessModuleService;

    public String buildTemplateCsv(
        String documentTypeCode,
        String companyProjectCode,
        String archiveTypeCode,
        String documentTypeName
    ) {
        if (!StringUtils.hasText(documentTypeCode)) {
            throw new BusinessException("documentTypeCode is required");
        }
        String dt = documentTypeCode.trim();
        String company = StringUtils.hasText(companyProjectCode) ? companyProjectCode.trim() : "0002GL00003";
        String module = StringUtils.hasText(archiveTypeCode) ? archiveTypeCode.trim() : "GL";
        String typeName = documentTypeName != null ? documentTypeName : "";

        List<String> labels = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        Set<String> seenKeys = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : CORE_LABEL_AND_KEY) {
            labels.add(e.getKey());
            keys.add(e.getValue());
            seenKeys.add(e.getValue());
        }

        List<BusinessModuleExtFieldResponse> union =
            businessModuleService.listPendingArchiveBasicExtFieldsUnionUnderDocumentType(dt);
        for (BusinessModuleExtFieldResponse f : union == null ? List.<BusinessModuleExtFieldResponse>of() : union) {
            if (f == null || !"Y".equalsIgnoreCase(f.getEnabledFlag())) {
                continue;
            }
            String en = f.getEnglishFieldName() != null ? f.getEnglishFieldName().trim() : "";
            String fc = f.getFieldCode() != null ? f.getFieldCode().trim() : "";
            String code = StringUtils.hasText(en) ? en : fc;
            if (!StringUtils.hasText(code) || seenKeys.contains(code)) {
                continue;
            }
            seenKeys.add(code);
            String disp = f.getFieldName() != null ? f.getFieldName().trim() : "";
            labels.add(StringUtils.hasText(disp) ? disp : code);
            keys.add(code);
        }

        String header = joinCsvRow(labels);
        String sample = joinCsvRow(buildSampleCells(keys, company, module, typeName));
        return header + "\n" + sample;
    }

    private static List<String> buildSampleCells(List<String> keys, String company, String module, String documentTypeName) {
        Map<String, String> sample = new LinkedHashMap<>();
        sample.put("companyProjectCode", company);
        sample.put("archiveTypeCode", module);
        sample.put("businessCode", company + "-TAX-001");
        sample.put("beginPeriod", "2024-01");
        sample.put("endPeriod", "2024-01");
        sample.put("archiveDestination", "SHANGHAI");
        sample.put("originPlace", "SHANGHAI");
        sample.put("documentName", documentTypeName.contains("税务") ? "示例税务文档" : "示例文档");
        sample.put("documentDate", "2024-01-15 10:00:00");
        sample.put("dutyPerson", "系统");
        sample.put("carrierTypeCode", "ELECTRONIC");
        sample.put("sourceSystem", "PORTAL");
        sample.put("securityLevelCode", "INTERNAL");
        sample.put("remark", "");
        sample.put("custodyStatus", "IN_STORAGE");
        List<String> cells = new ArrayList<>();
        for (String k : keys) {
            cells.add(sample.getOrDefault(k, ""));
        }
        return cells;
    }

    private static String joinCsvRow(List<String> cells) {
        List<String> out = new ArrayList<>(cells.size());
        for (String c : cells) {
            out.add(escapeCsvCell(c == null ? "" : c));
        }
        return String.join(",", out);
    }

    private static String escapeCsvCell(String s) {
        if (s.contains("\"") || s.contains(",") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
