package com.smartarchive.barcodemodule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartarchive.barcodemodule.domain.BarcodeModule;
import com.smartarchive.barcodemodule.dto.BarcodeModuleCommand;
import com.smartarchive.barcodemodule.dto.BarcodeModuleResponse;
import com.smartarchive.barcodemodule.dto.BarcodeModuleUpdateCommand;
import com.smartarchive.barcodemodule.dto.LinkedBusinessModuleItem;
import com.smartarchive.barcodemodule.mapper.BarcodeModuleMapper;
import com.smartarchive.barcodemodule.service.BarcodeModuleService;
import com.smartarchive.businessmodule.domain.BusinessModule;
import com.smartarchive.businessmodule.mapper.BusinessModuleMapper;
import com.smartarchive.common.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BarcodeModuleServiceImpl implements BarcodeModuleService {

    private static final Long SYSTEM_OPERATOR_ID = 1L;

    private final BarcodeModuleMapper barcodeModuleMapper;
    private final BusinessModuleMapper businessModuleMapper;

    @Override
    public List<BarcodeModuleResponse> list(Boolean enabledOnly) {
        List<BarcodeModule> rows = barcodeModuleMapper.selectList(new LambdaQueryWrapper<BarcodeModule>()
            .eq(BarcodeModule::getDeleteFlag, "N")
            .eq(Boolean.TRUE.equals(enabledOnly), BarcodeModule::getEnableFlag, "Y")
            .orderByAsc(BarcodeModule::getBarcodeCode));
        List<BarcodeModuleResponse> responses = rows.stream().map(this::toResponse).toList();

        List<BusinessModule> links = businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getDeleteFlag, "N")
            .isNotNull(BusinessModule::getBarcodeModuleCode));
        Map<String, Long> countByCode = links.stream()
            .filter(m -> StringUtils.hasText(m.getBarcodeModuleCode()))
            .collect(Collectors.groupingBy(
                m -> m.getBarcodeModuleCode().trim().toUpperCase(Locale.ROOT),
                Collectors.counting()));
        for (BarcodeModuleResponse r : responses) {
            String key = StringUtils.hasText(r.getBarcodeCode())
                ? r.getBarcodeCode().trim().toUpperCase(Locale.ROOT)
                : "";
            r.setLinkedBusinessModuleCount(countByCode.getOrDefault(key, 0L).intValue());
        }
        return responses;
    }

    @Override
    @Transactional
    public BarcodeModuleResponse create(BarcodeModuleCommand command) {
        String code = normalizeBarcodeModuleCode(command.getBarcodeCode());
        ensureCodeUnique(code, null);
        LocalDateTime now = LocalDateTime.now();
        BarcodeModule entity = new BarcodeModule();
        entity.setBarcodeCode(code);
        entity.setBarcodeName(command.getBarcodeName().trim());
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setEnableFlag(normalizeFlag(command.getEnableFlag(), "Y"));
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(now);
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(now);
        barcodeModuleMapper.insert(entity);
        return toResponse(require(entity.getBarcodeId()));
    }

    @Override
    @Transactional
    public BarcodeModuleResponse update(long barcodeId, BarcodeModuleUpdateCommand command) {
        BarcodeModule entity = require(barcodeId);
        String code = normalizeBarcodeModuleCode(command.getBarcodeCode());
        ensureCodeUnique(code, barcodeId);
        entity.setBarcodeCode(code);
        entity.setBarcodeName(command.getBarcodeName().trim());
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setEnableFlag(normalizeFlag(command.getEnableFlag(), "Y"));
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(LocalDateTime.now());
        barcodeModuleMapper.updateById(entity);
        return toResponse(require(barcodeId));
    }

    @Override
    @Transactional
    public void delete(long barcodeId) {
        BarcodeModule bm = require(barcodeId);
        long ref = businessModuleMapper.selectCount(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getDeleteFlag, "N")
            .eq(BusinessModule::getBarcodeModuleCode, bm.getBarcodeCode()));
        if (ref > 0) {
            throw new BusinessException("存在业务模块已映射该条码模块，请先解除映射后再删除");
        }
        barcodeModuleMapper.update(null, new LambdaUpdateWrapper<BarcodeModule>()
            .eq(BarcodeModule::getBarcodeId, barcodeId)
            .set(BarcodeModule::getDeleteFlag, "Y")
            .set(BarcodeModule::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(BarcodeModule::getLastUpdateDate, LocalDateTime.now()));
    }

    @Override
    public List<LinkedBusinessModuleItem> listLinkedBusinessModules(String barcodeModuleCode) {
        if (!StringUtils.hasText(barcodeModuleCode)) {
            return List.of();
        }
        String code = barcodeModuleCode.trim().toUpperCase(Locale.ROOT);
        List<BusinessModule> rows = businessModuleMapper.selectList(new LambdaQueryWrapper<BusinessModule>()
            .eq(BusinessModule::getDeleteFlag, "N")
            .eq(BusinessModule::getBarcodeModuleCode, code)
            .orderByAsc(BusinessModule::getModuleCode));
        return rows.stream().map(m -> {
            LinkedBusinessModuleItem item = new LinkedBusinessModuleItem();
            item.setModuleCode(m.getModuleCode());
            item.setModuleName(m.getModuleName());
            return item;
        }).toList();
    }

    private BarcodeModule require(Long barcodeId) {
        if (barcodeId == null || barcodeId <= 0) {
            throw new BusinessException("barcodeId 无效");
        }
        BarcodeModule entity = barcodeModuleMapper.selectOne(new LambdaQueryWrapper<BarcodeModule>()
            .eq(BarcodeModule::getBarcodeId, barcodeId)
            .eq(BarcodeModule::getDeleteFlag, "N"));
        if (entity == null) {
            throw new BusinessException("条码模块不存在或已删除");
        }
        return entity;
    }

    private static String normalizeBarcodeModuleCode(String raw) {
        if (!StringUtils.hasText(raw)) {
            throw new BusinessException("条码模块编码不能为空");
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        if (!s.matches("^[0-9A-Z\\-]{1,5}$")) {
            throw new BusinessException("条码模块编码须为 1～5 位，仅含数字、大写字母与连接符 -");
        }
        return s;
    }

    private void ensureCodeUnique(String code, Long excludeId) {
        BarcodeModule dup = barcodeModuleMapper.selectOne(new LambdaQueryWrapper<BarcodeModule>()
            .eq(BarcodeModule::getDeleteFlag, "N")
            .eq(BarcodeModule::getBarcodeCode, code)
            .last("limit 1"));
        if (dup != null && (excludeId == null || !excludeId.equals(dup.getBarcodeId()))) {
            throw new BusinessException("条码模块编码已存在: " + code);
        }
    }

    private static String normalizeFlag(String raw, String defaultVal) {
        if (!StringUtils.hasText(raw)) {
            return defaultVal;
        }
        String v = raw.trim().toUpperCase();
        if ("Y".equals(v) || "N".equals(v)) {
            return v;
        }
        return defaultVal;
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }

    private BarcodeModuleResponse toResponse(BarcodeModule e) {
        BarcodeModuleResponse r = new BarcodeModuleResponse();
        r.setBarcodeId(e.getBarcodeId());
        r.setBarcodeCode(e.getBarcodeCode());
        r.setBarcodeName(e.getBarcodeName());
        r.setDescription(e.getDescription());
        r.setEnableFlag(e.getEnableFlag());
        r.setLastUpdatedBy(e.getLastUpdatedBy());
        r.setLastUpdateDate(e.getLastUpdateDate());
        return r;
    }
}
