package com.smartarchive.businessmodule.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.smartarchive.barcodemodule.dto.BarcodeModuleCommand;
import com.smartarchive.barcodemodule.dto.BarcodeModuleResponse;
import com.smartarchive.barcodemodule.dto.BarcodeModuleUpdateCommand;
import com.smartarchive.barcodemodule.dto.LinkedBusinessModuleItem;
import com.smartarchive.barcodemodule.service.BarcodeModuleService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Standalone MockMvc：不启动容器、不用 Mockito inline，避免沙箱/CI 下 agent 附加失败。 */
class BarcodeModuleControllerWebMvcTest {

    private final BarcodeModuleService stubService = new BarcodeModuleService() {
        @Override
        public List<BarcodeModuleResponse> list(Boolean enabledOnly) {
            return List.of();
        }

        @Override
        public BarcodeModuleResponse create(BarcodeModuleCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public BarcodeModuleResponse update(long barcodeId, BarcodeModuleUpdateCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(long barcodeId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<LinkedBusinessModuleItem> listLinkedBusinessModules(String barcodeModuleCode) {
            return List.of();
        }
    };

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new BarcodeModuleController(stubService)).build();

    @Test
    void listBarcodeModulesIsRegistered() throws Exception {
        mockMvc.perform(get("/api/base-data/barcode-modules").param("enabledOnly", "true"))
            .andExpect(status().isOk());
    }
}
