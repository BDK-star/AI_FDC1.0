package com.smartarchive.security.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("tpl_user_t")
public class User {
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    @TableField("user_name")
    private String userName;
    /** 员工工号，8 位数字 */
    @TableField("employee_no")
    private String employeeNo;
    /** 归档责任部门（创建页按责任人自动带出） */
    private String dutyDepartment;
    /** 工作国家编码，对应产生地 */
    private String workCountryCode;
    private String email;
    private String phone;
    private String status;
    
    private Long createdBy;
    private LocalDateTime creationDate;
    private Long lastUpdatedBy;
    private LocalDateTime lastUpdateDate;
    
    @TableLogic(value = "N", delval = "Y")
    private String deleteFlag;
}
