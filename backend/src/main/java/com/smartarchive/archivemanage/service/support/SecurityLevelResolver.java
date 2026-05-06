package com.smartarchive.archivemanage.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartarchive.archiveflow.domain.SecurityLevelDictionary;
import com.smartarchive.archiveflow.mapper.SecurityLevelDictionaryMapper;
import com.smartarchive.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class SecurityLevelResolver {
    private static final long CACHE_TTL_MS = 60_000L;

    private final SecurityLevelDictionaryMapper securityLevelDictionaryMapper;
    private volatile List<SecurityLevelDictionary> cache;
    private volatile long cacheAt;

    public record Resolved(String canonicalCode, String displayName, boolean dictionaryMatched) {}

    public Resolved resolve(String rawFromDb) {
        List<SecurityLevelDictionary> levels = activeLevels();
        if (levels == null) {
            levels = List.of();
        }
        if (!StringUtils.hasText(rawFromDb)) {
            return defaultInternal(levels);
        }
        String trimmed = rawFromDb.trim();
        for (SecurityLevelDictionary d : levels) {
            if (d == null || !StringUtils.hasText(d.getSecurityLevelCode())) {
                continue;
            }
            if (trimmed.equalsIgnoreCase(d.getSecurityLevelCode())) {
                return new Resolved(
                    d.getSecurityLevelCode(),
                    Objects.toString(d.getSecurityLevelName(), d.getSecurityLevelCode()),
                    true
                );
            }
        }
        for (SecurityLevelDictionary d : levels) {
            if (d == null) {
                continue;
            }
            if (Objects.equals(d.getSecurityLevelName(), trimmed)) {
                return new Resolved(
                    d.getSecurityLevelCode(),
                    Objects.toString(d.getSecurityLevelName(), d.getSecurityLevelCode()),
                    true
                );
            }
        }
        return new Resolved(trimmed, trimmed, false);
    }

    /**
     * 写入库前强制规范为字典编码（接受编码或中文名称）；空则默认 INTERNAL。
     */
    public String requireCanonicalForWrite(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "INTERNAL";
        }
        Resolved r = resolve(raw.trim());
        if (!r.dictionaryMatched()) {
            throw new BusinessException("密级无效，请从字典选择（公开/内部/秘密/机密或对应英文编码）");
        }
        return r.canonicalCode();
    }

    /**
     * 查询条件兼容：库中可能暂存编码或中文名（迁移后主要为编码）。
     */
    public List<String> storedValueSqlVariants(String filterInput) {
        if (!StringUtils.hasText(filterInput)) {
            return List.of();
        }
        Resolved r = resolve(filterInput.trim());
        if (!r.dictionaryMatched()) {
            return List.of(filterInput.trim());
        }
        Set<String> v = new LinkedHashSet<>();
        v.add(r.canonicalCode());
        v.add(r.displayName());
        return new ArrayList<>(v);
    }

    private Resolved defaultInternal(List<SecurityLevelDictionary> levels) {
        if (levels == null) {
            return new Resolved("INTERNAL", "内部", true);
        }
        for (SecurityLevelDictionary d : levels) {
            if (d == null || !StringUtils.hasText(d.getSecurityLevelCode())) {
                continue;
            }
            if ("INTERNAL".equalsIgnoreCase(d.getSecurityLevelCode())) {
                return new Resolved(
                    d.getSecurityLevelCode(),
                    Objects.toString(d.getSecurityLevelName(), "内部"),
                    true
                );
            }
        }
        return new Resolved("INTERNAL", "内部", true);
    }

    private List<SecurityLevelDictionary> activeLevels() {
        long now = System.currentTimeMillis();
        List<SecurityLevelDictionary> snap = cache;
        if (snap != null && now - cacheAt < CACHE_TTL_MS) {
            return snap;
        }
        snap = securityLevelDictionaryMapper.selectList(new LambdaQueryWrapper<SecurityLevelDictionary>()
            .eq(SecurityLevelDictionary::getDeleteFlag, "N")
            .eq(SecurityLevelDictionary::getEnabledFlag, "Y")
            .orderByAsc(SecurityLevelDictionary::getSortOrder));
        if (snap == null) {
            snap = List.of();
        }
        cache = snap;
        cacheAt = now;
        return snap;
    }

}
