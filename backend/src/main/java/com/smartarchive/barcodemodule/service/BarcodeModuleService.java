package com.smartarchive.barcodemodule.service;

import com.smartarchive.barcodemodule.dto.BarcodeModuleCommand;
import com.smartarchive.barcodemodule.dto.BarcodeModuleResponse;
import com.smartarchive.barcodemodule.dto.BarcodeModuleUpdateCommand;
import com.smartarchive.barcodemodule.dto.LinkedBusinessModuleItem;
import java.util.List;

public interface BarcodeModuleService {

    List<BarcodeModuleResponse> list(Boolean enabledOnly);

    BarcodeModuleResponse create(BarcodeModuleCommand command);

    BarcodeModuleResponse update(long barcodeId, BarcodeModuleUpdateCommand command);

    void delete(long barcodeId);

    List<LinkedBusinessModuleItem> listLinkedBusinessModules(String barcodeModuleCode);
}
