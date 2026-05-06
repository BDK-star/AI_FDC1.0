package com.smartarchive.businessmodule.controller;

import com.smartarchive.barcodemodule.dto.BarcodeModuleCommand;
import com.smartarchive.barcodemodule.dto.BarcodeModuleResponse;
import com.smartarchive.barcodemodule.dto.BarcodeModuleUpdateCommand;
import com.smartarchive.barcodemodule.dto.LinkedBusinessModuleItem;
import com.smartarchive.barcodemodule.service.BarcodeModuleService;
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

/**
 * 条码模块主数据接口。放在 {@code businessmodule.controller} 包下，与业务模块等基座接口一并扫描，避免部分运行环境下子包控制器未注册导致的 404。
 */
@RestController
@RequestMapping("/api/base-data/barcode-modules")
@RequiredArgsConstructor
public class BarcodeModuleController {

    private final BarcodeModuleService barcodeModuleService;

    @GetMapping
    public ApiResponse<List<BarcodeModuleResponse>> list(@RequestParam(required = false) Boolean enabledOnly) {
        return ApiResponse.success(barcodeModuleService.list(enabledOnly));
    }

    @GetMapping("/{barcodeModuleCode}/linked-business-modules")
    public ApiResponse<List<LinkedBusinessModuleItem>> listLinkedBusinessModules(
        @PathVariable String barcodeModuleCode
    ) {
        return ApiResponse.success(barcodeModuleService.listLinkedBusinessModules(barcodeModuleCode));
    }

    @PostMapping
    public ApiResponse<BarcodeModuleResponse> create(@Valid @RequestBody BarcodeModuleCommand command) {
        return ApiResponse.success(barcodeModuleService.create(command));
    }

    @PutMapping("/{barcodeId}")
    public ApiResponse<BarcodeModuleResponse> update(
        @PathVariable long barcodeId,
        @Valid @RequestBody BarcodeModuleUpdateCommand command
    ) {
        return ApiResponse.success(barcodeModuleService.update(barcodeId, command));
    }

    @DeleteMapping("/{barcodeId}")
    public ApiResponse<Void> delete(@PathVariable long barcodeId) {
        barcodeModuleService.delete(barcodeId);
        return ApiResponse.success(null);
    }
}
