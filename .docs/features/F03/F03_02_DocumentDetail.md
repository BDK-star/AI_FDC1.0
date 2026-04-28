# F03_02 文档详情

> 执行口径：仅以当前前后端代码实现为准。

## 0. 背景与范围

- 背景：从文档查询列表进入详情，集中查看档案主数据、附件、操作日志。
- 范围：详情展示、附件预览/下载、操作日志查看。
- 非范围：内部独立附件预览页（由附件接口直接返回资源流，详见 `F03_03`）。

## 1. 业务场景

- **S-01 查看文档详情**
  - **触发**：从文档查询列表点击“文档业务编码”
  - **系统响应**：加载并展示基本信息、扩展信息、归档信息、附件列表、操作日志

- **S-02 附件预览/下载**
  - **触发**：附件列表操作列点击“预览/下载”，或“批量下载”
  - **系统响应**：按权限执行；预览通过接口返回资源流，由浏览器直接打开

## 2. 页面结构（当前页面）

- **页面头区**：业务编码标题 + 状态标签 + 条件显示“编辑”按钮
- **面包屑**：首页 / 文档查询 / 文档详情
- **标题区**：业务编码 + 状态标签（载体类型/归档状态/密级）+ 编辑按钮（待归档场景显示）
- **信息区**（折叠/展开）：
  - 文档基本信息
  - 扩展信息（可折叠）
  - 归档信息
- **附件列表区**
- **操作日志区**

## 3. 字段清单（代码口径）

> 字段命名/类型/长度以 `/.docs/01_DataModel.md` 为准。本节仅沉淀“展示字段集合与来源关系”。

### 3.1 文档基本信息

字段来源（节选）：

- 文档类型：`fdc_doc_t.business_module_id -> fdc_business_module_t.document_type_id -> fdc_document_type_t.document_type_name`
- 文档业务编码：`fdc_doc_t.doc_busi_no`
- 公司/主体：`fdc_doc_t.archived_entity_unit_id -> fdc_archived_entity_unit_t -> fdc_archived_entity_t.archived_entity_name`
- 业务模块：`fdc_doc_t.business_module_id -> fdc_business_module_t.business_module_name`
- 开始/结束档期：`fdc_doc_t.start_period` / `fdc_doc_t.end_period`
- 归档地/产生地：`fdc_doc_t.*_alpha2_code` → 行政区划服务（按层级入参）
- 归档责任人：`fdc_doc_t.owner_id` → 用户服务
- 责任部门：`fdc_doc_t.dept_code` → 部门查询服务
- 载体类型：`fdc_doc_t.carrier_type` → LOOKUP `FDC_CARRIER_TYPE`
- 系统来源：`fdc_doc_t.source_system` → LOOKUP `FDC_SOURCE_SYS`
- 密级：`securityLevelName` 或 `securityLevelCode`
- 描述/备注：`fdc_doc_t.description`
- 创建人/创建时间：`fdc_doc_t.created_by` / `fdc_doc_t.creation_date`（字段名以数据模型为准）

### 3.2 归档信息（示例字段集合）

- 条码模块、档案条码、册内序号、册号、册条码
- 文档组织、库房、库位
- 份数、剩余份数、档案类型
- 是否可见：`documentVisibility` 或扩展字段回退

### 3.3 扩展信息（待定字段集合）

扩展字段由业务模块扩展配置与 `extValues` 决定，页面按配置动态展示。

## 3. 附件列表

### 4.1 列表字段（当前实现）

- 文件名：`fdc_doc_att_t.file_name`
- 附件类型：`fdc_doc_att_t.att_type`
- 大小：`fdc_doc_att_t.file_size`
- 上传时间：`fdc_doc_att_t.upload_time`
- 补充信息：`fdc_doc_att_t.additional_info`
- 操作：预览/下载（图标按钮）

### 4.2 操作与权限

- **批量下载**：需要“附件下载”权限；无权限则按钮不可用。
- **预览**：需要“附件预览”权限；无权限则置灰
- **下载**：需要“附件下载”权限；无权限则置灰

## 4. 操作日志

### 5.1 字段（当前实现）

- 操作人：`fdc_doc_op_log_t.operated_by`
- 操作类型：`fdc_doc_op_log_t.operation_type`
- 操作内容：`fdc_doc_op_log_t.op_content`
- 时间：`fdc_doc_op_log_t.operation_time`
- 备注：`fdc_doc_op_log_t.remarks`
- 补充附件：`fdc_doc_log_att_t.log_att_name` + `edm_id`（下载）

## 5. 功能详细规格（Functional Specs）

### 5.1 业务场景与用户旅程

- **S-01 查看文档详情**
  - **前置条件**：
    - 用户具备文档查询权限；
    - 列表页存在可访问的文档记录。
  - **操作步骤**：
    - 在“文档查询列表页”点击“文档业务编码”进入详情页。
  - **系统响应**：
    - 详情页加载并展示：文档基本信息、扩展信息、归档信息、附件列表、操作日志。
  - **异常与提示**：
    - 无权限或资源不可访问时返回对应错误/提示；
    - 文档不存在时给出资源不存在提示。
  - **产出**：只读展示详情信息。

- **S-02 附件预览/下载**
  - **前置条件**：
    - 用户具备附件预览/下载权限（取决于具体按钮操作）。
  - **操作步骤**：
    - 在详情页附件列表操作列点击“预览/下载”，或点击“批量下载”。
  - **系统响应**：
    - 预览：根据附件id获取该附件 `EDM_ID`（来源：`fdc_doc_att_t.additional_info` 的唯一标识码口径），调用“附件预览”接口获取外部预览入口并打开；
    - 下载触发附件下载动作（按权限）。
  - **异常与提示**：
    - 无权限时按钮不可见/置灰；
    - 下载/预览失败给出错误提示。
  - **产出**：预览内容或下载结果。

### 5.2 页面/交互说明（UI/UX）

#### 5.2.1 页面一览（本功能涉及的全部页面）
| 页面编号 | 页面名称 | 路由/入口（或菜单路径） | 页面类型 | 简述 | 详述 |
|---|---|---|---|---|---|
| P-02 | 文档详情 | 从“文档查询列表页”点击进入 | 列表页/详情页（详情页） | 展示文档基础/归档/扩展信息、附件列表、操作日志 | 见下节 P-02 |

#### 5.2.2 分页面规格（每页复制一整块）

##### P-02 《文档详情》

###### （1）页面类型与整体布局
- **页面类型**：详情页
- **布局骨架**：
  - **L1** Header：返回文档查询 + 搜索框 + 用户信息
  - **L2** 面包屑导航：显示当前页面路径
  - **L3** 标题区：显示文档名称、状态标签和操作按钮
  - **L4** 信息展示区：包含多个可折叠的信息模块（基本信息/扩展信息/归档信息）
  - **L5** 附件列表区：显示文档附件信息
  - **L6** 操作日志区：显示文档操作历史记录
- **页面实现**：`frontend/src/views/archive-management/ArchiveDetailView.vue`

###### （2）分区 → 模块拆解（按当前页面实现逐行对齐字段与按钮）

**〔L2 面包屑导航区〕— 模块 M0：页面路径（精简版）**
|元素名称|字段名-英文|类型|链接逻辑|
|--|--|--|--|
|首页|Home|链接|跳转到系统首页|
|文档查询|Document Search|链接|跳转到文档查询页面|
|文档详情|Document Detail|文本|当前页面|

**〔L3 标题〕— 模块 M1：标题**
|**序号**|**字段名称**|**字段名-英文**|**类型**|**字段逻辑说明**|**备注**|
|--|--|--|--|--|--|
|1|文档名称|Doc Name|文本|根据文档id在fdc_doc_t表中获取doc_name字段|字体加粗，比文档业务编码字号|
|2|文档业务编码|Doc Business No.|文本|根据文档id在fdc_doc_t表中获取doc_busi_no字段||
|3|标签|Tag|文本|待定||

**〔L4 基本信息〕— 模块 M2：文档基本信息**
|**序号**|**字段名称**|**字段名-英文**|**类型**|**字段逻辑说明**|**备注**|
|--|--|--|--|--|--|
|1|文档类型|Document Type|文本|根据文档id在fdc_doc_t表中获取business_module_id，作为外键关联fdc_business_module_t获取document_type_id, 作为外键关联fdc_document_type_t获取document_type_name||
|2|文档业务编码|Doc Business No.|文本|根据文档id在fdc_doc_t表中获取doc_busi_no字段||
|3|公司/主体|Company/Entity|文本|根据文档id在fdc_doc_t表中获取archived_entity_unit_id，作为外键关联fdc_archived_entity_unit_t获取archived_entity_id，作为外键关联fdc_archived_entity_t获取archived_entity_name||
|4|业务模块|Business Module|文本|根据文档id在fdc_doc_t表中获取business_module_id，作为外键关联fdc_business_module_t获取business_module_name||
|5|开始档期|Start Period|文本|根据文档id在fdc_doc_t表中获取start_period字段||
|6|结束档期|End Period|文本|根据文档id在fdc_doc_t表中获取end_period字段||
|7|归档地|Arch Place|文本|根据文档id在fdc_doc_t表中获取arch_place_alpha2_code字段，作为入参（通过-分割符数量判断行政区域层级决定具体入参）查询****政区域获取对应AliasChinese和AliasEnglish出参||
|8|产生地|Originating Place|文本|根据文档id在fdc_doc_t表中获取origin_place_alpha2_code字段，作为入参（通过-分割符数量判断行政区域层级决定具体入参）查询****政区域获取对应AliasChinese和AliasEnglish出参||
|9|文档名称|Doc Name|文本|根据文档id在fdc_doc_t表中获取doc_name字段|单独展示一行|
|10|文档生成日期|Doc Generation Date|文本|根据文档id在fdc_doc_t表中获取doc_generation_date字段||
|11|归档责任人|Owner|文本|根据文档id在fdc_doc_t表中获取owner_id字段，作为外键外联【用户表】的user_id，带出user_name字段展示||
|12|文档责任部门|Responsible Dept.|文本|根据文档id在fdc_doc_t表中获取dept_code字段，作为入参查询****获取对应organizationName字段||
|13|载体类型|Carrier Type|文本|根据文档id在fdc_doc_t表中获取carrier_type字段，关联Lookup中的Carrier Type值||
|14|系统来源|Source System|文本|根据文档id在fdc_doc_t表中获取source_system字段||
|15|密级|Security Level|文本|根据文档id在fdc_doc_t表中获取business_module_id，作为外键关联fdc_business_module_t获取||
|16|描述|Description|文本|根据文档id在fdc_doc_t表中获取description字段|单独展示一行|
|17|文档归档编码||文本|根据文档id在fdc_doc_t表中获取||
|18|创建时间|Creation Time|文本|根据文档id在fdc_doc_t表中获取creation_date字段||
|19|创建人|Created By|文本|根据文档id在fdc_doc_t表中获取created_by字段，作为外键外联【用户表】的user_id，带出user_name字段展示||
|20|地区部|Region|文本|根据文档id在fdc_doc_t表中获取archived_entity_unit_id，作为外键关联fdc_archived_entity_unit_t获取archived_entity_id，作为外键关联fdc_archived_entity_t获取region_code，地区部名称根据region_code调用idata服务获取对应的名称||
|21|代表处|Rep Office|文本|根据文档id在fdc_doc_t表中获取archived_entity_unit_id，作为外键关联fdc_archived_entity_unit_t获取archived_entity_id，作为外键关联fdc_archived_entity_t获取rep_office_code，地区部名称根据rep_office_code 调用idata服务获取对应的名称||
|22|国家/地区|Country|文本|根据文档id在fdc_doc_t表中获取archived_entity_unit_id，作为外键关联fdc_archived_entity_unit_t获取archived_entity_id，作为外键关联fdc_archived_entity_t获取country_code，国家名称根据country_code 调用idata服务获取对应的名称||

**〔L4 归档信息〕— 模块 M3：归档信息**
|**序号**|**字段名称**|**字段名-英文**|**类型**|**字段逻辑说明**|**备注**|
|--|--|--|--|--|--|
|1|条码模块|Barcode Module|文本|||
|2|档案条码|Archive Barcode|文本|||
|3|文档编号|Volume Seq. No.|文本||册内序号|
|4|册号|Volume No.|文本|||
|5|册条码|Volume Barcode|文本|||
|6|文档组织|Doc Organization|文本|||
|7|库房|Repository|文本|||
|8|库位|Storage Location|文本|||
|9|份数|Copies|文本|||
|10|剩余份数|Remaining Copies|文本|||
|11|档案类型|Archive Type|文本|||
|12|是否可见|Visibility|文本|根据文档id在fdc_doc_t表中匹配归档流向，获取visible_flag||

**按钮：**
|**编号**|**按钮名称**|**按钮名称-英文**|**显示位置**|**按钮逻辑说明（含调用UI API）**|**绑定权限项名称**|**新增权限项说明**|
|--|--|--|--|--|--|--|
|1|下拉|/|归档信息栏标题-右上1|展示|||

**〔L4 扩展信息〕— 模块 M4：扩展信息（待定）**
|**序号**|**字段名称**|**字段名-英文**|**类型**|**字段逻辑说明**|**备注**|
|--|--|--|--|--|--|
|1|文号|File No.|文本|||
|2|发票号|Invoice No.|文本|||
|3|会计|Accountant|文本|||
|4|扫描员|Scanned By|文本|||
|5|其它归档号|Other Arch No.|文本|||
|6|项目名称|Project Name|文本|||
|7|客户|Customer|文本|||
|8|交易对手|Countparty|文本|||
|9|放款日期|Loan Date|文本|||
|10|移交人|Handed over|文本|||
|11|到期日|Expiry Date|文本|||
|12|银行名称|Bank Name|文本|||
|13|币种|Currency|文本|||
|14|金额|Amount|文本|||
|15|出票日|Issue Date|文本|||
|16|出票人账号|Drawer Account No.|文本|||
|17|收款行|Payee Bank|文本|||
|18|付款行|Paying Bank|文本|||
|19|出票人|Drawer|文本|||
|20|收款行账号|Payee Account No.|文本|||
|21|前手背书人|Prior Endorser|文本|||
|22|业务处理人|Handler|文本|||
|23|起息日||文本|||
|24|原存单号||文本|||
|25|存期||文本|||
|26|收益率||文本|||
|27|银行账户||文本|||
|28|细分类型||文本|||
|29|开立日期||文本|||
|30|开立银行名称||文本|||
|31|合同号|Contract No.|文本|||
|32|文件号||文本||是否保留 = 文号？|
|33|签发机构||文本|||
|34|每份页数|Pages per Copy|文本|||
|35|业务申请人||文本|||
|36|供应商||文本|||
|37|开立方式||文本|||
|38|保函编号||文本|||
|39|保函台账状态||文本|||
|40|保函失效日期||文本|||

**按钮：**
|**编号**|**按钮名称**|**按钮名称-英文**|**显示位置**|**按钮逻辑说明（含调用UI API）**|**绑定权限项名称**|**新增权限项说明**|
|--|--|--|--|--|--|--|
|1|下拉|/|扩展信息栏标题-右上1|展示|||

**〔L5 附件列表〕— 模块 M5：附件列表**
|**序号**|**字段名称**|**字段名-英文**|**类型**|**字段逻辑说明**|**备注**|
|--|--|--|--|--|--|
|1|文件名|File Name|文本|根据附件id在fdc_doc_att_t表中获取file_name字段|显示附件文件名和图标|
|2|附件类型|Attachment Type|文本|根据附件id在fdc_doc_att_t表中获取att_type字段|附件的业务类型|
|3|大小|File Size|文本|根据附件id在fdc_doc_att_t表中获取file_size字段|附件文件大小|
|4|上传时间|Upload Time|日期|根据附件id在fdc_doc_att_t表中获取upload_time字段|附件上传时间|
|5|补充信息|Additional Info|文本|根据附件id在fdc_doc_att_t表中获取additional_info字段|附件的唯一标识码|
|6|操作|Operations|按钮组||预览和下载功能|

**按钮：**
|**编号**|**按钮名称**|**按钮名称-英文**|**显示位置**|**按钮逻辑说明（含调用UI API）**|**绑定权限项名称**|**新增权限项说明**|
|--|--|--|--|--|--|--|
|1|批量下载|Batch Download|附件列表栏标题-右上1|点击批量下载，调用附件批量下载api，点击后弹出导出跳转提示弹窗|附件下载|如无权限，批量下载预览按钮不可见|
|2|下拉|/|附件列表栏标题-右上2|展示|||
|3|预览|图标|附件列表栏-操作列右1|点击预览：根据附件id获取 `EDM_ID`（来源：`fdc_doc_att_t.additional_info` 唯一标识码），调用“附件预览”接口获取外部预览入口（URL/重定向），并打开外部系统对应预览页面|附件预览|如无权限，预览按钮置灰|
|4|下载|图标|附件列表栏-操作列右2|点击下载，调用附件下载api，|附件下载|如无权限，下载按钮置灰|

**〔L6 操作日志〕— 模块 M6：操作日志列表**
|**序号**|**字段名称**|**字段名-英文**|**类型**|**字段逻辑说明**|**备注**|
|--|--|--|--|--|--|
|1|操作人|Operated By|文本|根据日志id在fdc_doc_op_log_t表中获取operated_by字段|操作执行人|
|2|操作类型|Operation Type|文本|根据日志id在fdc_doc_op_log_t表中获取operation_type字段|操作类型标识|
|3|操作内容|Operation Content|文本|根据日志id在fdc_doc_op_log_t表中获取op_content字段|具体操作描述|
|4|时间|Operation Time|日期时间|根据日志id在fdc_doc_op_log_t表中获取operation_time字段|操作执行时间|
|5|备注|Remarks|文本|根据日志id在fdc_doc_op_log_t表中获取remarks字段|操作补充说明|
|6|补充附件|Supporting Attachment|链接|根据日志id在fdc_doc_log_att_t表中获取log_att_name和edm_id字段|相关附件链接|

**按钮：**
|**编号**|**按钮名称**|**按钮名称-英文**|**显示位置**|**按钮逻辑说明（含调用UI API）**|**绑定权限项名称**|**新增权限项说明**|
|--|--|--|--|--|--|--|
|1|下拉|/|操作日志列表标题栏-右上1|展示|||
|2|补充附件下载|链接|操作日志列表补充附件列|点击补充附件名，调用补充附件下载api，点击后|||

### 页面API设计（前端交互类必选，来自当前实现）
| 序号 | 页面名称 | API 名称 | 使用位置 |
|---:|---|---|---|
| 1 | 文档详情 | 文档详情（整体） | 整体 |
| 2 | 文档详情 | 附件批量下载 | 附件列表栏-批量下载按钮 |
| 3 | 文档详情 | 单个附件预览 | 附件列表栏-单个预览按钮 |
| 4 | 文档详情 | 单个附件下载 | 附件列表栏-单个下载按钮 |
| 5 | 文档详情 | 补充附件下载 | 附件列表栏-补充附件链接 |

### 5.3 规则与策略（Rules）
- **R-01 附件预览/下载权限门控**  
  - **输入**：用户在文档详情页附件列表/操作日志触发预览、下载、批量下载动作。  
  - **处理**：若不具备对应权限，则按钮不可见或置灰。  
 - **输出**：具备权限时执行对应动作（预览打开外部预览入口/下载/导出跳转提示弹窗）。

- **R-02 外部系统预览入口与审计**  
  - **输入**：用户点击“预览”。  
  - **处理**：后端基于附件id解析 `EDM_ID`，拼接/下发外部系统预览入口（URL 或重定向）；前端据此打开外部预览页；全链路记录预览请求审计（操作者、附件ID、EDM_ID、时间、结果）。  
  - **输出**：在外部系统展示预览内容；本系统不再承担内部受控水印渲染。  

### 5.4 异常/边界/幂等（Edge cases）
- **E-01 文档无权限/不存在**：返回无权限/资源不存在提示，不加载详情模块数据。
- **E-02 附件不可用**：预览/下载失败给出错误提示；预览失败时不打开外部预览页，不产生文件。

### 5.5 与其他模块的交互（UI - API）
- 文档详情页：调用“文档详情（整体）”API加载基础/扩展/归档/附件/操作日志所需数据。
- 附件列表：调用“单个附件预览/单个附件下载/附件批量下载”API。
- 操作日志补充附件下载：点击补充附件名调用“补充附件下载”API。

## 6. 接口规范（API Specs，代码对齐）

> 遵循 `/.docs/05_API_Conventions.md`。附件与操作日志采用**扁平资源** `document-attachments`、`document-operation-logs`，避免 `/documents/{id}/attachments/...` 深层嵌套。`{id}` 为数值型主键。

| 资源路径 | 方法 | 用途 | 备注 |
|---|---|---|---|
| `FDC_URL/api/archive-management/archives/{archiveId}` | GET | 文档详情 | `ArchiveManagementController.getArchiveDetail` |
| `FDC_URL/api/archive-management/attachments/{attachmentId}/preview` | GET | 单个附件预览 | 直接返回资源流 |
| `FDC_URL/api/archive-management/attachments/{attachmentId}/download` | GET | 单个附件下载 | 二进制下载 |
| `FDC_URL/api/archive-management/archives/{archiveId}/attachments/download-all` | GET | 附件批量下载 | ZIP 下载 |
| `FDC_URL/api/common/audits/page` | POST | 操作日志查询 | 详情页日志建议复用审计接口 |

## 7. 验收标准（AC）

- **AC-01**：从列表进入详情后，标题区展示文档名称与状态标签；面包屑可返回文档查询。  
- **AC-02**：附件列表显示文件名/类型/大小/上传时间；无权限时预览/下载行为按权限受控。  
- **AC-03**：点击“预览”后可打开附件内容资源；无权限时预览不可用。  
- **AC-04**：操作日志展示操作人/类型/内容/时间；存在补充附件时可按权限下载。  

## 8. 权限与安全（补充）

- 详情读取、附件预览、附件下载都需校验数据范围权限。
- 审计日志查询需校验可见范围，不可跨组织查看。

## 9. 测试计划（补充）

| 用例ID | 场景 | 期望结果 |
|---|---|---|
| TC-DD-01 | 从列表进入详情 | 详情加载成功，关键模块展示完整 |
| TC-DD-02 | 预览/下载权限校验 | 有权限可执行，无权限拦截 |
| TC-DD-03 | 附件批量下载 | 返回 zip 且文件完整 |

## 10. 冲突点说明

- 历史文档中“外部 EDM 预览入口”描述与当前后端实现不一致，本文已统一为“流式资源预览”。

