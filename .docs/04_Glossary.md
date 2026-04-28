# 财经文档归档系统 - 专业术语表 (Glossary)

## 1. 核心业务实体 (Core Business Entities)


| 中文术语        | 英文术语 (Class/Code) | 字段缩写           | 业务定义与约束                                             |
| ----------- | ----------------- | -------------- | --------------------------------------------------- |
| **应归档数据**   | Archivable Data   | `arch_data`    | 从 30+ 源系统集成的原始结构化数据，尚未转化为文档。                        |
| **文档**      | Document          | `doc`          | 携带业务属性的逻辑实体，如生成的 PDF、电子凭证或表单。                       |
| **档案**      | Archive           | `arch`         | 已完成签收/核销、获得唯一档案条码并正式生效的法律凭证。                        |
| **附件**      | Attachment        | `att`          | 挂载在文档下的原始扫描件、发票图片或补充协议。                             |
| **档案册 (卷)** | Volume            | `vol`          | 档案按条码顺序封装后的最小管理单位（对应“一册一库位”）。                       |
| **档案条码**    | Archive Barcode   | `arch_barcode` | 唯一身份标识，分为 **Domestic** (国内) 与 **Overseas** (国外) 流水。 |
| **文档业务编码**  | Doc Business No.  | `doc_biz_no`   | 源系统业务 ID，如：保函电子流编码 (LG Workflow Code)、报销单号。         |


## 2. 核心基础概念 (Core Foundation Concepts)


| 术语         | 英文术语                | 定义与业务规则                                                          |
| ---------- | ------------------- | ---------------------------------------------------------------- |
| **文档类型**   | Document Type       | 财经分类标识（会计、税务、资金等），存储于 `fdc_document_type_t`。                     |
| **归档主体**   | Archiving Entity    | 文档所属的公司或法律主体，关联 `fdc_archived_entity_t`。                         |
| **业务模块**   | Business Module     | 文档所属业务领域（ERP 应收、应付、资产），关联 `fdc_business_module_t`。               |
| **载体类型**   | Carrier Type        | 存在形式：**Physical** (实物件)、**Electronic** (电子件)、**Hybrid** (实物+扫描)。 |
| **密级**     | Security Level      | 安全等级，统一由字典 `SECURITY_LEVEL` 管理；模型存编码（如 `INTERNAL_PUBLIC`、`SECRET`、`CONFIDENTIAL`、`TOP_SECRET`），前端展示名称（内部公开、秘密、机密、绝密）。 |
| **库位**     | Storage Location    | 档案册在库房中的物理坐标（财经规则：1 册 : 1 库位）。                                   |
| **流水连续性**  | Sequence Continuity | **强制规则**：档案条码流水严禁断号，成册必须严格按条码顺序排列。                               |
| **财经扩展属性** | Extended Attributes | 财经专业字段：如发票号 (Invoice No)、金额 (Amount)、开立行 (Issuing Bank)。         |

## 2.1 人员字段约定 (Personnel Field Convention)

| 约定项 | 规则 |
| --- | --- |
| **底层存储** | 所有人员相关字段底层统一存 `tpl_user_t.user_id`。 |
| **前端展示** | 统一展示为 `user_name employee_no`（中间为空格，不使用 `+`）。 |
| **查询输入** | 推荐输入 `user_name employee_no`；兼容历史输入 `user_name + employee_no`。 |
| **接口命名** | 查询参数优先使用语义字段名（如 `dutyPerson`），避免页面状态使用别名（如 `owner`）导致失配。 |
| **扩展人员字段** | `verifiedBy / assembledBy / storedBy / accountant / scannedBy` 按同一规则处理（存ID、显名称+工号）。 |


## 3. 状态与操作 (States & Operations)


| 中文术语        | 英文术语                     | 对应枚举值         | 逻辑说明                      |
| ----------- | ------------------------ | ------------- | ------------------------- |
| **核销 (签收)** | Verification / Clearance | `VERIFIED`    | 确认文档属性无误，系统生成档案条码，转为正式档案。 |
| **成册**      | Bundling / Volumizing    | `BUNDLED`     | 将多份档案按流水号顺序逻辑打包，分配唯一册号。   |
| **入库**      | Stocking / Check-in      | `STOCKED`     | 档案册分配物理库位并确认上架，状态转为“在库”。  |
| **移交**      | Handover                 | `HANDED_OVER` | 管                         |


