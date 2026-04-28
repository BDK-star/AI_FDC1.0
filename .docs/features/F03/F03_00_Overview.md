# F03_00 文档查询总览

> 本文档用于沉淀 F03 共性约束；各页面细节见 `F03_01`~`F03_07`。

## 1. 背景与范围

### 1.0 工作空间通用能力（系统级）

工作空间菜单中的 **导入查询**、**导出查询**（与「我的导出」同一规格，见 `F03_05`）、**我的草稿**（见 `F03_07`）属于**系统通用功能**（平台级能力）：

- **全系统可用**：导入查询、导出查询**不限于文档查询（F03）**；任何具备权限的业务模块均可提交导入/导出任务，并在工作空间统一查看进度与结果。文档查询列表上的「批量导入查询」「批量导出」仅是**调用方入口之一**，不表示该能力归属或专用于文档查询。
- **不依赖**任一业务列表页上的文档类型、公司编码、业务模块等**页面级**筛选条件；
- 列表与任务按**当前登录用户**可见范围展示（管理员等扩展见 `/.docs/03_Security.md`）。

文档查询页自身的「文档类型必选」等规则（见下文 §4）**仅约束该列表页**的查询与部分工具栏动作，不反向要求工作空间上述入口，也不限制其他模块使用同一套导入/导出任务能力。

**导入查询**的写法与验收结构可与 `F03_05_MyExports.md` 对照阅读，详见 `F03_06_MyImportQueries.md`。

### 1.1 功能范围（In scope）

- 文档查询列表：多条件筛选、结果列表、跳转文档详情
- 文档详情：基本信息、归档信息、扩展信息（可折叠）、附件列表、操作日志
- 附件预览：通过附件预览接口返回资源流进行预览，下载原文（按权限）
- 批量导出：从查询结果或勾选结果发起导出任务、提示跳转“我的导出”
- 我的导出（工作空间亦称「导出查询」）：导出任务列表、下载/删除
- 工作空间「导入查询」：导入类任务列表与结果操作（详见 `F03_06`）
- 工作空间「我的草稿」：用户草稿列表与编辑入口（详见 `F03_07`）

### 1.2 非范围（Out of scope）

- 应归档数据域的「应归档数据管理」等能力不作为 F03 文档查询主流程验收范围，详见 `F03_90_ArchiveReceivableData_Reference.md`。
- 工作空间「导入查询」页面规格以 `F03_06` 为准；`F03_90` 仅保留跨域边界说明，避免与文档查询列表混淆。

## 2. 页面清单（前端代码对应）


| 页面   | 前端路由 / 视图 | 说明 |
| ---- | ---------------------------------------------- | --------------------------------- |
| 文档查询 | `/archive-management/query` + `ArchiveQueryView.vue` | 筛选区 + 列表区；含「批量导出」「批量导入查询」入口（全站任务能力调用方之一） |
| 文档详情 | `/archive-management/detail/:id` + `ArchiveDetailView.vue` | 多模块折叠信息 + 附件 + 操作日志 |
| 我的导出（导出查询） | `/workspace/export-query` + `ExportQueryWorkspaceView.vue` | 工作空间导出任务列表，与文档查询筛选条件无关；见 `F03_05` |
| 导入查询 | `/workspace/import-query` + `ImportQueryWorkspaceView.vue` | 工作空间导入任务列表，与文档查询筛选条件无关；见 `F03_06` |
| 我的草稿 | `/workspace/my-drafts` + `MyDraftsWorkspaceView.vue` | 工作空间草稿列表；见 `F03_07` |


## 3. 角色与权限（代码与安全文档口径）

> 详细权限点需与 `/.docs/03_Security.md` 的 RBAC 对齐。本节按当前功能页面能力沉淀。

角色以系统角色配置为准。  
能力项（节选）：文档查询列表、文档导出、附件预览/下载/批量下载、导入查询结果列表/重新查询/结果导出等。

## 4. 共性规则（跨页面一致）

- **必选条件**：文档查询页“文档类型”为必选；未选择时触发提示“请选择文档类型”。
- **时间范围**：涉及日期/档期范围输入时，限定范围不超过 1 年；超出提示“选择范围不能超过一年”。
- **批量输入上限**：如“文档业务编码/其他归档号”等支持多条输入时，上限 100 条；超出提示“输入条目不能超过100条”。
- **查询结果导出（当前实现）**：
  - 仅支持导出勾选集合（`docIds` 必填）
  - 未勾选时需前端拦截提示
  - 成功后提示并引导跳转“导入导出 > 我的导出”

## 5. 关键数据来源（代码口径）

> 字段命名与类型以 `/.docs/01_DataModel.md` 为准；这里仅记录“数据来源关联关系”。

- 文档主表：`fdc_doc_t`（示例字段：`doc_busi_no`、`doc_name`、`doc_status`、`start_period`、`end_period`、`carrier_type`、`source_system`…）
- 文档类型：`fdc_document_type_t`
- 业务模块：`fdc_business_module_t`
- 归档主体：`fdc_archived_entity_t`、`fdc_archived_entity_unit_t`
- 文档组织：`fdc_document_organization_t`
- 附件：`fdc_doc_att_t`
- 操作日志：`fdc_doc_op_log_t`、`fdc_doc_log_att_t`
- LOOKUP：`FDC_CARRIER_TYPE`、`FDC_DOC_STATUS`、`FDC_SECURITY_LEVEL`、`FDC_SOURCE_SYS` 等

## 6. F03 相关 API 资源（代码对齐）

> 全局约定见 `/.docs/05_API_Conventions.md`；各端点请求/响应体见 `F03_01`～`F03_07`。

| 资源 | 说明 |
|---|---|
| `archive-management/pending-documents/query` | 文档查询主列表（POST） |
| `archive-management/create/query` | 文档查询另一实现线（POST） |
| `archive-management/archives/{archiveId}` | 文档详情（GET） |
| `archive-management/attachments/{attachmentId}/preview` | 附件预览（GET，流式资源） |
| `archive-management/attachments/{attachmentId}/download` | 附件下载（GET） |
| `archive-management/archives/{archiveId}/attachments/download-all` | 附件批量下载（GET） |
| `archive-management/pending-documents/export-jobs` | 批量导出任务提交（POST，`docIds` 必填） |
| `workspace/io-jobs/*` | 我的导出/导入查询统一任务中心 |

## 7. 权限与安全（总览）

- 核心权限：文档查询、详情查看、附件预览、附件下载、导出提交、导出下载。
- 数据范围：至少按用户可见组织/业务范围过滤。
- 审计范围：查询、详情、预览、下载、导出提交与下载。

## 8. 测试与验收锚点（总览）

- 各子文档至少覆盖：列表/详情正常路径、权限拦截路径、失败路径。
- F03 统一验收关注点：
  - 查询条件与返回字段一致性
  - 附件预览下载权限正确性
  - 导出任务提交与工作空间任务可追踪性
  - 草稿能力与正式数据隔离

