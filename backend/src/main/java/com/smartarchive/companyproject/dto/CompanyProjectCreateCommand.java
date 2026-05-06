package com.smartarchive.companyproject.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class CompanyProjectCreateCommand {
    @NotBlank(message = "公司编码不能为空")
    @Size(max = 64, message = "公司编码长度不能超过64")
    private String companyProjectCode;

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 128, message = "公司名称长度不能超过128")
    private String companyProjectName;

    @NotBlank(message = "国家不能为空")
    @Size(max = 32, message = "国家编码长度不能超过32")
    private String countryCode;

    @NotBlank(message = "地区部不能为空")
    @Size(max = 128, message = "地区部长度不能超过128")
    private String managementArea;

    @Size(max = 128, message = "公司标签长度不能超过128")
    private String companyTag;

    @NotBlank(message = "启用标识不能为空")
    private String enabledFlag;

    @Valid
    @NotEmpty(message = "至少需要维护一条组织信息")
    private List<CompanyProjectLineCommand> lines;
}
