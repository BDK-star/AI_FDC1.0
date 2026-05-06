package com.smartarchive.archivemanage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;

@Data
public class PendingDocumentExportCommand {
    /**
     * JSON 数组：元素为字符串或数字（推荐字符串，避免大整数精度问题）。
     * 使用单一 {@link JsonNode} 绑定数组，避免 {@code List<JsonNode>} 在部分 Jackson/Spring 版本下反序列化异常或迭代中出现空指针。
     */
    @JsonProperty("docIds")
    private JsonNode docIds;
    private String exportFileFormat;
    /** DOCUMENT_QUERY | PENDING_ARCHIVE */
    private String exportScope;

    public List<Long> resolveDocIds() {
        if (docIds == null || docIds.isNull() || !docIds.isArray()) {
            return List.of();
        }
        Set<Long> seen = new LinkedHashSet<>();
        for (JsonNode n : docIds) {
            if (n == null || n.isNull() || n.isMissingNode()) {
                continue;
            }
            String s = jsonNodeToIdToken(n);
            if (s == null || s.isEmpty()) {
                continue;
            }
            try {
                long v = Long.parseLong(s);
                if (v > 0) {
                    seen.add(v);
                }
            } catch (NumberFormatException ignored) {
                // skip invalid token
            }
        }
        return new ArrayList<>(seen);
    }

    private static String jsonNodeToIdToken(JsonNode n) {
        if (n == null) {
            return null;
        }
        if (n.isTextual() || n.isNumber()) {
            return n.asText().trim();
        }
        return null;
    }
}
