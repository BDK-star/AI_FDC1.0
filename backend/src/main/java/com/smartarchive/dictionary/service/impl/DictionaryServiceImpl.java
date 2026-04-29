package com.smartarchive.dictionary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smartarchive.common.exception.BusinessException;
import com.smartarchive.dictionary.domain.DictionaryCategory;
import com.smartarchive.dictionary.domain.DictionaryItem;
import com.smartarchive.dictionary.dto.DictionaryCategoryCommand;
import com.smartarchive.dictionary.dto.DictionaryCategoryResponse;
import com.smartarchive.dictionary.dto.DictionaryCategoryUpdateCommand;
import com.smartarchive.dictionary.dto.DictionaryItemCommand;
import com.smartarchive.dictionary.dto.DictionaryItemResponse;
import com.smartarchive.dictionary.dto.DictionaryItemUpdateCommand;
import com.smartarchive.dictionary.mapper.DictionaryCategoryMapper;
import com.smartarchive.dictionary.mapper.DictionaryItemMapper;
import com.smartarchive.dictionary.service.DictionaryService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DictionaryServiceImpl implements DictionaryService {
    private static final Long SYSTEM_OPERATOR_ID = 1L;

    private final DictionaryCategoryMapper dictionaryCategoryMapper;
    private final DictionaryItemMapper dictionaryItemMapper;

    @Override
    public List<DictionaryCategoryResponse> listCategories() {
        Map<String, Long> itemCountMap = dictionaryItemMapper.selectList(new LambdaQueryWrapper<DictionaryItem>()
                .eq(DictionaryItem::getDeleteFlag, "N"))
            .stream()
            .collect(Collectors.groupingBy(DictionaryItem::getCategoryCode, Collectors.counting()));
        return dictionaryCategoryMapper.selectList(new LambdaQueryWrapper<DictionaryCategory>()
                .eq(DictionaryCategory::getDeleteFlag, "N")
                .orderByAsc(DictionaryCategory::getCategoryCode))
            .stream()
            .map(item -> DictionaryCategoryResponse.builder()
                .id(item.getId())
                .categoryCode(item.getCategoryCode())
                .categoryName(item.getCategoryName())
                .description(item.getDescription())
                .enabledFlag(item.getEnabledFlag())
                .itemCount(itemCountMap.getOrDefault(item.getCategoryCode(), 0L).intValue())
                .build())
            .toList();
    }

    @Override
    public List<DictionaryItemResponse> listItems(String categoryCode) {
        requireCategory(categoryCode);
        return dictionaryItemMapper.selectList(new LambdaQueryWrapper<DictionaryItem>()
                .eq(DictionaryItem::getCategoryCode, categoryCode)
                .eq(DictionaryItem::getDeleteFlag, "N")
                .orderByAsc(DictionaryItem::getSortOrder)
                .orderByAsc(DictionaryItem::getItemCode))
            .stream()
            .map(this::toItemResponse)
            .toList();
    }

    @Override
    @Transactional
    public DictionaryCategoryResponse createCategory(DictionaryCategoryCommand command) {
        String categoryCode = requireText(command.getCategoryCode(), "categoryCode");
        ensureCategoryAvailable(categoryCode, null);
        DictionaryCategory entity = new DictionaryCategory();
        entity.setCategoryCode(categoryCode);
        entity.setCategoryName(requireText(command.getCategoryName(), "categoryName"));
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setEnabledFlag(normalizeFlag(command.getEnabledFlag()));
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(LocalDateTime.now());
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(LocalDateTime.now());
        dictionaryCategoryMapper.insert(entity);
        return DictionaryCategoryResponse.builder().id(entity.getId()).categoryCode(entity.getCategoryCode()).categoryName(entity.getCategoryName()).description(entity.getDescription()).enabledFlag(entity.getEnabledFlag()).itemCount(0).build();
    }

    @Override
    @Transactional
    public DictionaryCategoryResponse updateCategory(String categoryCode, DictionaryCategoryUpdateCommand command) {
        DictionaryCategory entity = requireCategory(categoryCode);
        entity.setCategoryName(requireText(command.getCategoryName(), "categoryName"));
        entity.setDescription(trimToNull(command.getDescription()));
        entity.setEnabledFlag(normalizeFlag(command.getEnabledFlag()));
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(LocalDateTime.now());
        dictionaryCategoryMapper.updateById(entity);
        return DictionaryCategoryResponse.builder().id(entity.getId()).categoryCode(entity.getCategoryCode()).categoryName(entity.getCategoryName()).description(entity.getDescription()).enabledFlag(entity.getEnabledFlag()).itemCount(listItems(categoryCode).size()).build();
    }

    @Override
    @Transactional
    public void deleteCategory(String categoryCode) {
        DictionaryCategory entity = requireCategory(categoryCode);
        long itemCount = dictionaryItemMapper.selectCount(new LambdaQueryWrapper<DictionaryItem>().eq(DictionaryItem::getCategoryCode, categoryCode).eq(DictionaryItem::getDeleteFlag, "N"));
        if (itemCount > 0) {
            throw new BusinessException("Please delete dictionary items under this category first");
        }
        dictionaryCategoryMapper.update(null, new LambdaUpdateWrapper<DictionaryCategory>()
            .eq(DictionaryCategory::getId, entity.getId())
            .eq(DictionaryCategory::getDeleteFlag, "N")
            .set(DictionaryCategory::getDeleteFlag, "Y")
            .set(DictionaryCategory::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(DictionaryCategory::getLastUpdateDate, LocalDateTime.now()));
    }

    @Override
    @Transactional
    public DictionaryItemResponse createItem(String categoryCode, DictionaryItemCommand command) {
        requireCategory(categoryCode);
        String itemCode = requireText(command.getItemCode(), "itemCode");
        ensureItemAvailable(categoryCode, itemCode, null);
        DictionaryItem entity = new DictionaryItem();
        entity.setCategoryCode(categoryCode);
        entity.setItemCode(itemCode);
        entity.setItemName(requireText(command.getItemName(), "itemName"));
        entity.setItemValue(StringUtils.hasText(command.getItemValue()) ? command.getItemValue().trim() : itemCode);
        entity.setSortOrder(command.getSortOrder() == null ? 1 : command.getSortOrder());
        entity.setEnabledFlag(normalizeFlag(command.getEnabledFlag()));
        entity.setDeleteFlag("N");
        entity.setCreatedBy(SYSTEM_OPERATOR_ID);
        entity.setCreationDate(LocalDateTime.now());
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(LocalDateTime.now());
        dictionaryItemMapper.insert(entity);
        return toItemResponse(entity);
    }

    @Override
    @Transactional
    public DictionaryItemResponse updateItem(String categoryCode, String itemCode, DictionaryItemUpdateCommand command) {
        DictionaryItem entity = requireItem(categoryCode, itemCode);
        entity.setItemName(requireText(command.getItemName(), "itemName"));
        entity.setItemValue(StringUtils.hasText(command.getItemValue()) ? command.getItemValue().trim() : entity.getItemCode());
        entity.setSortOrder(command.getSortOrder() == null ? 1 : command.getSortOrder());
        entity.setEnabledFlag(normalizeFlag(command.getEnabledFlag()));
        entity.setLastUpdatedBy(SYSTEM_OPERATOR_ID);
        entity.setLastUpdateDate(LocalDateTime.now());
        dictionaryItemMapper.updateById(entity);
        return toItemResponse(entity);
    }

    @Override
    @Transactional
    public void deleteItem(String categoryCode, String itemCode) {
        DictionaryItem entity = requireItem(categoryCode, itemCode);
        dictionaryItemMapper.update(null, new LambdaUpdateWrapper<DictionaryItem>()
            .eq(DictionaryItem::getId, entity.getId())
            .eq(DictionaryItem::getDeleteFlag, "N")
            .set(DictionaryItem::getDeleteFlag, "Y")
            .set(DictionaryItem::getLastUpdatedBy, SYSTEM_OPERATOR_ID)
            .set(DictionaryItem::getLastUpdateDate, LocalDateTime.now()));
    }

    private DictionaryCategory requireCategory(String categoryCode) {
        DictionaryCategory entity = dictionaryCategoryMapper.selectOne(new LambdaQueryWrapper<DictionaryCategory>()
            .eq(DictionaryCategory::getCategoryCode, categoryCode)
            .eq(DictionaryCategory::getDeleteFlag, "N")
            .last("limit 1"));
        if (entity == null) {
            throw new BusinessException("Dictionary category does not exist");
        }
        return entity;
    }

    private DictionaryItem requireItem(String categoryCode, String itemCode) {
        DictionaryItem entity = dictionaryItemMapper.selectOne(new LambdaQueryWrapper<DictionaryItem>()
            .eq(DictionaryItem::getCategoryCode, categoryCode)
            .eq(DictionaryItem::getItemCode, itemCode)
            .eq(DictionaryItem::getDeleteFlag, "N")
            .last("limit 1"));
        if (entity == null) {
            throw new BusinessException("Dictionary item does not exist");
        }
        return entity;
    }

    private void ensureCategoryAvailable(String categoryCode, Long ignoreId) {
        DictionaryCategory existing = dictionaryCategoryMapper.selectOne(new LambdaQueryWrapper<DictionaryCategory>()
            .eq(DictionaryCategory::getCategoryCode, categoryCode)
            .eq(DictionaryCategory::getDeleteFlag, "N")
            .last("limit 1"));
        if (existing != null && !existing.getId().equals(ignoreId)) {
            throw new BusinessException("Dictionary category code already exists");
        }
    }

    private void ensureItemAvailable(String categoryCode, String itemCode, Long ignoreId) {
        DictionaryItem existing = dictionaryItemMapper.selectOne(new LambdaQueryWrapper<DictionaryItem>()
            .eq(DictionaryItem::getCategoryCode, categoryCode)
            .eq(DictionaryItem::getItemCode, itemCode)
            .eq(DictionaryItem::getDeleteFlag, "N")
            .last("limit 1"));
        if (existing != null && !existing.getId().equals(ignoreId)) {
            throw new BusinessException("Dictionary item code already exists");
        }
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(fieldName + " cannot be blank");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeFlag(String flag) {
        String normalized = StringUtils.hasText(flag) ? flag.trim().toUpperCase() : "Y";
        if (!List.of("Y", "N").contains(normalized)) {
            throw new BusinessException("enabledFlag only supports Y or N");
        }
        return normalized;
    }

    private DictionaryItemResponse toItemResponse(DictionaryItem item) {
        return DictionaryItemResponse.builder()
            .id(item.getId())
            .categoryCode(item.getCategoryCode())
            .itemCode(item.getItemCode())
            .itemName(item.getItemName())
            .itemValue(item.getItemValue())
            .sortOrder(item.getSortOrder())
            .enabledFlag(item.getEnabledFlag())
            .build();
    }
}
