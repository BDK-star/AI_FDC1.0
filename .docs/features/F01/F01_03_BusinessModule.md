# F01_03 业务模块管理

> 按 `/.docs/features/Template.md` 补充完整规格。

## 1. 功能目标

- 维护业务模块主数据，作为分类、流程路由和权限配置基础。

## 2. 关键数据对象

- 建议主表：`fdc_business_module_t`
- 关键字段：`business_module_id`、`business_module_code`、`business_module_name`、`enable_flag`、`security_level`
- 密级口径：`security_level` 存储**字典编码**（category=`SECURITY_LEVEL`，如 `INTERNAL_PUBLIC` / `SECRET` / `CONFIDENTIAL` / `TOP_SECRET`）
- 展示口径：前端展示字典项 `item_name`（如“内部公开/秘密/机密/绝密”）

## 3. 关键规则

- 模块编码唯一。
- 禁用模块不得被新规则引用。
- 业务模块密级统一走字典管理：写入与存储使用编码，页面展示使用名称。
- CSV 导入规则：密级列支持填写**字典编码或名称**，系统在导入时归一化为编码；无法匹配时按行报错。
- 扩展字段维护新增“字段类型”必选项，业务口径为：文本 / 数据字典 / 是否 / 数字 / 日期 / 日期时间。
- 扩展字段选择规则：必须先选“字段类型”，再从对应类型的可用扩展字段池中选择具体字段，不允许跨类型选取。
- 字段池分配规则（文档基本信息，`fdc_document_t`）：
  - 文本 / 数据字典 / 是否 -> `attr1~attr40`（`NVARCHAR2(500)`）
  - 数字 -> `attr41~attr60`（`DECIMAL`）
  - 日期 -> `attr61~attr80`（`DATE`）
  - 日期时间 -> `attr81~attr100`（`TIMESTAMP`）
- 字段池分配规则（附件，`fdc_document_attach_t`）：
  - 文本 / 数据字典 / 是否 -> `attribute1~attribute20`（`NVARCHAR2(500)`）
  - 数字 -> `attribute21~attribute30`（`DECIMAL`）
  - 日期 -> `attribute31~attribute40`（`DATE`）
  - 日期时间 -> `attribute41~attribute50`（`TIMESTAMP`）
- 可用字段判定：已被启用字段占用的扩展字段不可重复分配；禁用或删除后方可回收复用（以审计策略为准）。

## 4. 待补充章节

- 页面与交互、接口定义、权限矩阵、验收标准、测试用例。
