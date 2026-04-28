# F03_08 应归档数据：批量创建（CSV）字段口径

> **范围**：应归档数据管理列表「批量创建」导入模板。  
> **区分**：与文档查询列表页的「批量导入查询」不同，后者规格见 `F03_01_DocumentSearch.md`（§2.4、§5.2.2 模块 M5）及 `F03_06_MyImportQueries.md`。  
> **目的**：统一产品/测试对「当前应填 code 还是展示值」的认知，并约定改造后用户录入逻辑。

## 1. 列对照：当前实现 vs 建议改造后

| 中文列名 | 内部字段键（概念） | 当前：用户宜填什么 | 建议改造后：用户宜填什么 | 说明要点 |
|---|---|---|---|---|
| 文档业务编码 | businessCode | 业务编号（字符串） | 同上 | 已是业务值，非字典 code。 |
| 公司 | companyProjectCode | **公司编码**（如 `fdc_company_project_t.company_project_code`） | **公司编码或公司名称**（解析为唯一 code） | 表头为「公司」，易误填公司全称。 |
| 业务模块 | archiveTypeCode | **三级业务模块 `type_code`** | **模块名称或编码**（解析为 L3 `type_code`） | 与创建页下拉「名称」不一致时易错。 |
| 开始/结束档期 | beginPeriod / endPeriod | **yyyy-MM** | 同上 | 业务值格式。 |
| 归档地 | archiveDestination | **归档地代码**（与主数据/规则一致，如 alpha 风格码） | **城市或标准地名**，映射为库内代码 | 样例常见 `SHANGHAI` 类 code。 |
| 产生地 | originPlace | 同归档地 | 同归档地 | 同上。 |
| 文档名称 | documentName | 任意文本 | 同上 | 业务值。 |
| 文档生成日期 | documentDate | **yyyy-MM-dd HH:mm:ss**（字符串） | 同上（可兼容仅日期，需产品确认） | 注意格式一致。 |
| 归档责任人 | dutyPerson | **登录名 `user_name`** 或 **纯数字 user_id** | **工号 / 登录名 / 姓名**（明确匹配优先级；避免误建新用户） | 当前按 `user_name` 匹配；匹配不到可能**自动插入用户**。 |
| 文档责任部门 | dutyDepartment | **仅数字部门 ID**（非数字按 0 处理） | **部门名称或编码**（映射为 `doc_resp_dept_id`） | 当前**不支持**部门中文名。 |
| 载体类型 | carrierTypeCode | **仅** `ELECTRONIC` / `PAPER` / `HYBRID` | **中文或英文**（如 电子件/纸质件/混合 → 映射为上述 code） | 当前**不支持**中文载体标签。 |
| 系统来源 | sourceSystem | 短文本或配置值 | 同上 | 偏配置。 |
| 密级 | securityLevelCode | **字典编码或中文名称**（字典可匹配即可） | 同上 | 已相对友好。 |
| 描述 | remark | 任意文本 | 同上 | 业务值。 |
| 文档组织 | documentOrganizationCode | **文档组织编码** | **组织名称或编码**（解析为 code） | 表头易引导填名称。 |
| 是否可见 | visibility（扩展） | **是 / 否** 等展示口径 | 同上 | 已是展示值。 |
| 条码模块 | barcodeModule | 文本 | 同上 | 业务值。 |
| 保管状态 | custodyStatus | **状态码**（如 `UNARCHIVED`，与生命周期字典一致） | **中文或编码**（映射为存储 code） | 用户更熟悉「未归档」等中文。 |
| 保管年限 | retentionPeriodYears | **整数年** | 同上 | 业务值。 |
| 国家 / 代表处 / 地区部 / 公司标签 | country / repOffice / region / companyTag | 可填；若导入逻辑按**公司编码** `enrichExtFromCompany`，可能**用公司主数据覆盖** CSV 同名字段 | 建议：**以公司为准自动带出**，或允许用户覆盖时在规格与代码中明确优先级 | 避免「用户填了却被静默覆盖」无感知。 |
| 发票号、其他相关编号等扩展 | 各扩展键 | 多为展示文本写入扩展属性 | 同上 | 一般已是值；若某扩展为字典类型，需按 `DocumentTypeExtField` 区分 code/名称。 |
| 动态扩展列（随文档类型配置） | fieldCode | 文本即值；**字典类字段可能需 code** | **优先允许名称**，后台解析为存储值 | 依赖文档类型扩展字段配置。 |

## 2. 改造后的统一录入原则（产品层）

1. **主数据类**（公司、业务模块、文档组织、归档地/产生地、部门）：用户填**界面常见名称或常用编码**；系统解析为库内**唯一 code**，解析失败**明确报错**（避免落到错误 ID）。  
2. **字典/枚举类**（载体、保管状态、密级）：用户填**中文标签或标准编码**；系统归一化为存储 code（密级已部分支持；载体、保管状态建议补齐）。  
3. **人员类**（归档责任人）：约定 **工号 → 登录名 → 姓名** 等优先级；禁止在批量场景下随意新建用户，或仅在开关开启时允许。  
4. **纯文本/数字/日期类**：用户直接填最终业务值（业务编码、文档名称、日期、年限、发票号等）。  
5. **与公司联动的地理/标签字段**：要么模板不展示、完全由公司带出，要么「用户填写优先」需在实现与本规格中同步修订（与当前「公司存在则覆盖」行为对齐说明）。

## 3. 示例（用户直觉 vs 当前必填 vs 改造后）

以下示例便于培训与验收用例编写（字段值为示意）。

| 场景说明 | 用户直觉填写 | 当前更稳妥的填写 | 改造后期望 |
|---|---|---|---|
| 公司 | `华为（杭州）` | `0002GL00003`（公司 project code） | 填 `华为（杭州）` 或 code，均解析为同一 code |
| 业务模块 | `增值税申报` | 三级 `type_code`，如 `TAX-VAT-DECL-01` | 填中文名或 code，解析为 `archiveTypeCode` |
| 载体类型 | `电子件` | `ELECTRONIC` | `电子件` → `ELECTRONIC` |
| 密级 | `内部` 或字典 code | 二者在字典可匹配时均可 | 维持 |
| 文档组织 | `财务部文档组` | `FIN-DOC-01`（组织 code） | 名称解析为 code |
| 归档责任人 | `张三` | 登录名如 `zs0001` 或数字 user_id | 按工号/登录名/姓名规则匹配；失败报错或走主数据，不随意建用户 |
| 文档责任部门 | `财务部` | 数字 ID，如 `10086` | `财务部` 或部门编码 → 部门 ID |
| 保管状态 | `未归档` | `UNARCHIVED`（与实现一致） | `未归档` → `UNARCHIVED` |

## 4. 实现备注（供研发对齐）

- 表头映射：`PendingArchiveBatchImportHeaderResolver`。  
- 行数据 → 写库命令：`PendingArchiveBatchImportServiceImpl#buildCommandFromValues` 及 `enrichExtFromCompany`。  
- 正式校验与落库：`ArchiveManagementServiceImpl` 中 `createPendingDocument`（如 `requireCompanyProject`、`validateBusinessModuleUnderRoot`、`normalizeCarrierType`、`securityLevelResolver.requireCanonicalForWrite`、`resolveUserIdByLoginName`、`parseDeptId`、`documentOrganizationCode` 等）。  
- 若产品确认「模板一律录展示值」，建议在导入服务与创建服务之间增加**归一化层**（例如 `BatchImportValueNormalizer`），并为公司/模块/组织/部门/载体等增加单测与模糊匹配策略说明。
