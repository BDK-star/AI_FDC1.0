package com.smartarchive.archivemanage.support;

import java.util.Locale;
import org.springframework.util.StringUtils;

/**
 * {@code fdc_document_t.visible_flag}：{@code "1"} 可见，{@code "0"} 不可见。
 * API / 前端仍以「是」「否」展示与提交。
 */
public final class DocumentVisibilitySupport {

    private DocumentVisibilitySupport() {}

    public static String toStorageFlag(String visibilityYesNoOrLegacy) {
        if (!StringUtils.hasText(visibilityYesNoOrLegacy)) {
            return "1";
        }
        String t = visibilityYesNoOrLegacy.trim();
        if ("0".equals(t)) {
            return "0";
        }
        if ("1".equals(t)) {
            return "1";
        }
        if ("否".equals(t)) {
            return "0";
        }
        if ("是".equals(t)) {
            return "1";
        }
        String u = t.toUpperCase(Locale.ROOT);
        if ("N".equals(u) || "NO".equals(u)) {
            return "0";
        }
        if ("Y".equals(u) || "YES".equals(u)) {
            return "1";
        }
        if ("不可见".equals(t)) {
            return "0";
        }
        return "1";
    }

    /** 库内 visible_flag（null 视为可见）→ 「是」「否」 */
    public static String toDisplayYesNo(String visibleFlagFromDb) {
        if (!StringUtils.hasText(visibleFlagFromDb)) {
            return "是";
        }
        return "0".equals(visibleFlagFromDb.trim()) ? "否" : "是";
    }

    /** 审计展示：0/1 → 否/是 */
    public static String formatAuditFlag(Object raw) {
        if (raw == null) {
            return "空";
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return "空";
        }
        return "0".equals(s) ? "否" : "1".equals(s) ? "是" : s;
    }
}
