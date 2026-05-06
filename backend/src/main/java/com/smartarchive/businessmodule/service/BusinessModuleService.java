package com.smartarchive.businessmodule.service;

import com.smartarchive.businessmodule.domain.BusinessModuleExtField;
import com.smartarchive.businessmodule.dto.BusinessModuleCommand;
import com.smartarchive.businessmodule.dto.BusinessModuleExtFieldCommand;
import com.smartarchive.businessmodule.dto.BusinessModuleExtFieldResponse;
import com.smartarchive.businessmodule.dto.BusinessModuleNodeResponse;
import com.smartarchive.businessmodule.dto.BusinessModuleParentOptionResponse;
import com.smartarchive.businessmodule.dto.BusinessModuleUpdateCommand;
import java.util.List;

public interface BusinessModuleService {
    List<BusinessModuleNodeResponse> listTree();
    List<BusinessModuleParentOptionResponse> listParentOptions();
    BusinessModuleNodeResponse create(BusinessModuleCommand command);
    BusinessModuleNodeResponse update(String moduleCode, BusinessModuleUpdateCommand command);
    void delete(String moduleCode);
    List<BusinessModuleExtFieldResponse> listFields(String moduleCode, String fieldScope);

    /**
     * 按应用功能筛选扩展字段（如「移交」），{@code application_functions} 为逗号分隔。
     * @param fieldScope 可空；非空时与 {@link #listFields} 一致按 BASIC/ATTACHMENT 过滤
     */
    List<BusinessModuleExtFieldResponse> listFieldsByApplicationFunction(String moduleCode, String applicationFunction, String fieldScope);

    /**
     * 文档类型根 {@code documentTypeRootCode} 及其子树内全部业务模块上，BASIC、已启用且应用功能含「应归档数据」的扩展字段并集。
     * 同一英文键（englishFieldName，否则 fieldCode）在多处定义时保留层级最浅（levelNum 最小）的一条。
     */
    List<BusinessModuleExtFieldResponse> listPendingArchiveBasicExtFieldsUnionUnderDocumentType(String documentTypeRootCode);

    /**
     * 与 {@link #listPendingArchiveBasicExtFieldsUnionUnderDocumentType} 相同规则，返回实体列表（供应归档写入/读出 attr 与导入模板列一致）。
     */
    List<BusinessModuleExtField> listPendingArchiveBasicExtFieldEntitiesUnionUnderDocumentType(String documentTypeRootCode);

    /**
     * 叶子模块在应归档场景下生效的 BASIC 扩展字段（含祖先继承、同 ext 列去重），与
     * {@link #listFieldsByApplicationFunction}(leaf, 「应归档数据」, BASIC) 一致，供写入/读出 fdc_document_t.attr2–attr100。
     */
    List<BusinessModuleExtField> listEffectivePendingArchiveBasicExtFields(String leafModuleCode);

    BusinessModuleExtFieldResponse createField(String moduleCode, BusinessModuleExtFieldCommand command);
    BusinessModuleExtFieldResponse updateField(String moduleCode, String fieldCode, BusinessModuleExtFieldCommand command);
    void deleteField(String moduleCode, String fieldCode);
}
