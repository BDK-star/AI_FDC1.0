package com.smartarchive.barcodemodule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BarcodeModuleCommand {
    @NotBlank
    private String barcodeCode;
    @NotBlank
    private String barcodeName;
    private String description;
    private String enableFlag;
}
