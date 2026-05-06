package com.smartarchive.archivemanage.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.smartarchive.businessmodule.dto.BusinessModuleExtFieldResponse;
import com.smartarchive.businessmodule.service.BusinessModuleService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingBatchImportTemplateServiceTest {

    @Mock
    private BusinessModuleService businessModuleService;

    @InjectMocks
    private PendingBatchImportTemplateService service;

    @BeforeEach
    void setUp() {
        when(businessModuleService.listPendingArchiveBasicExtFieldsUnionUnderDocumentType(anyString())).thenReturn(List.of());
    }

    @Test
    void coreHeaderContainsFixedCoreColumns() {
        String csv = service.buildTemplateCsv("FIN_ACC", null, null, "会计文档");
        String header = csv.lines().findFirst().orElse("");
        assertThat(header).contains("文档业务编码", "归档责任人", "载体类型", "保管状态");
    }

    @Test
    void appendsUnionExtColumnsAfterCore() {
        when(businessModuleService.listPendingArchiveBasicExtFieldsUnionUnderDocumentType("DOCX")).thenReturn(List.of(
            bmField("auxNote", "F_AUX", "辅助说明"),
            bmField("voucherNo", "F_VOU", "凭证号")
        ));
        String csv = service.buildTemplateCsv("DOCX", null, null, "");
        String header = csv.lines().findFirst().orElse("");
        assertThat(header.split(",")).hasSizeGreaterThan(15);
        assertThat(header).contains("辅助说明", "凭证号");
    }

    @Test
    void unionExtColumnsNoLongerBlockedByHardcodedBlacklist() {
        when(businessModuleService.listPendingArchiveBasicExtFieldsUnionUnderDocumentType("T")).thenReturn(List.of(
            bmField("customOk", "CUSTOM_OK", "自定义"),
            bmField("invoiceNo", "INV", "发票号"),
            bmField("dutyDept", "ANY_CODE", "文档责任部门")
        ));
        String csv = service.buildTemplateCsv("T", null, null, "");
        String header = csv.lines().findFirst().orElse("");
        assertThat(header).contains("自定义", "发票号", "文档责任部门");
    }

    private static BusinessModuleExtFieldResponse bmField(String englishName, String fieldCode, String fieldName) {
        BusinessModuleExtFieldResponse r = new BusinessModuleExtFieldResponse();
        r.setEnglishFieldName(englishName);
        r.setFieldCode(fieldCode);
        r.setFieldName(fieldName);
        r.setEnabledFlag("Y");
        r.setSortOrder(1);
        return r;
    }
}
