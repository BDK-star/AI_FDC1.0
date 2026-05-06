package com.smartarchive.businessmodule.controller;

import com.smartarchive.businessmodule.dto.BusinessModuleCommand;
import com.smartarchive.businessmodule.dto.BusinessModuleExtFieldCommand;
import com.smartarchive.businessmodule.dto.BusinessModuleExtFieldResponse;
import com.smartarchive.businessmodule.dto.BusinessModuleNodeResponse;
import com.smartarchive.businessmodule.dto.BusinessModuleParentOptionResponse;
import com.smartarchive.businessmodule.dto.BusinessModuleUpdateCommand;
import com.smartarchive.businessmodule.service.BusinessModuleService;
import com.smartarchive.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/base-data/business-modules")
@RequiredArgsConstructor
public class BusinessModuleController {
    private final BusinessModuleService businessModuleService;

    @GetMapping("/tree")
    public ApiResponse<List<BusinessModuleNodeResponse>> tree() {
        return ApiResponse.success(businessModuleService.listTree());
    }

    @GetMapping("/parent-options")
    public ApiResponse<List<BusinessModuleParentOptionResponse>> parentOptions() {
        return ApiResponse.success(businessModuleService.listParentOptions());
    }

    @PostMapping
    public ApiResponse<BusinessModuleNodeResponse> create(@Valid @RequestBody BusinessModuleCommand command) {
        return ApiResponse.success(businessModuleService.create(command));
    }

    @PutMapping("/{moduleCode}")
    public ApiResponse<BusinessModuleNodeResponse> update(@PathVariable String moduleCode, @Valid @RequestBody BusinessModuleUpdateCommand command) {
        return ApiResponse.success(businessModuleService.update(moduleCode, command));
    }

    @DeleteMapping("/{moduleCode}")
    public ApiResponse<Void> delete(@PathVariable String moduleCode) {
        businessModuleService.delete(moduleCode);
        return ApiResponse.success(null);
    }

    /**
     * 应归档批量导入模板：当前文档类型（根）与子树业务模块上 BASIC+应归档数据 扩展字段并集。
     * 路径不可放在 {@code /ext-fields/...} 下，否则易与 {@code /ext-fields/{fieldCode}} 的 PUT 路由混淆导致 GET 报 method not supported。
     */
    @GetMapping("/{moduleCode}/pending-archive-ext-fields-union")
    public ApiResponse<List<BusinessModuleExtFieldResponse>> pendingArchiveExtUnion(@PathVariable String moduleCode) {
        return ApiResponse.success(businessModuleService.listPendingArchiveBasicExtFieldsUnionUnderDocumentType(moduleCode));
    }

    @GetMapping("/{moduleCode}/ext-fields")
    public ApiResponse<List<BusinessModuleExtFieldResponse>> fields(@PathVariable String moduleCode,
                                                                    @RequestParam(required = false) String fieldScope,
                                                                    @RequestParam(required = false) String applicationFunction) {
        if (applicationFunction != null && !applicationFunction.isBlank()) {
            return ApiResponse.success(
                businessModuleService.listFieldsByApplicationFunction(moduleCode, applicationFunction, fieldScope));
        }
        return ApiResponse.success(businessModuleService.listFields(moduleCode, fieldScope));
    }

    @PostMapping("/{moduleCode}/ext-fields")
    public ApiResponse<BusinessModuleExtFieldResponse> createField(@PathVariable String moduleCode, @Valid @RequestBody BusinessModuleExtFieldCommand command) {
        return ApiResponse.success(businessModuleService.createField(moduleCode, command));
    }

    @PutMapping("/{moduleCode}/ext-fields/{fieldCode}")
    public ApiResponse<BusinessModuleExtFieldResponse> updateField(@PathVariable String moduleCode, @PathVariable String fieldCode, @Valid @RequestBody BusinessModuleExtFieldCommand command) {
        return ApiResponse.success(businessModuleService.updateField(moduleCode, fieldCode, command));
    }

    @DeleteMapping("/{moduleCode}/ext-fields/{fieldCode}")
    public ApiResponse<Void> deleteField(@PathVariable String moduleCode, @PathVariable String fieldCode) {
        businessModuleService.deleteField(moduleCode, fieldCode);
        return ApiResponse.success(null);
    }
}
