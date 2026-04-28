# F03_03 附件预览

> **代码对齐口径**：当前实现为本系统直接流式返回附件资源，非“返回外部预览 URL”模式。

## 1. 范围与场景

- 场景 1：从文档详情页附件列表点击“预览”。
- 场景 2：从文档详情页附件列表点击“下载”。
- 范围：预览/下载接口行为、权限门控、审计要求。

## 2. 页面与交互

- 本功能无独立预览业务页，入口在 `F03_02` 文档详情附件列表中。
- 点击“预览”后由浏览器打开流式内容（由 contentType 决定内嵌展示或下载）。

## 3. 业务规则

- **R-AP-001 权限门控**：无预览权限不可触发预览，无下载权限不可触发下载。
- **R-AP-002 附件存在性**：附件不存在或文件缺失时返回错误并提示。
- **R-AP-003 一致性**：预览与下载都以同一附件主数据为准，避免跨文档越权读取。

## 4. API（代码对齐）

| 资源路径 | 方法 | 用途 | 代码位置 |
|---|---|---|---|
| `FDC_URL/api/archive-management/attachments/{attachmentId}/preview` | GET | 附件预览（流式资源） | `ArchiveManagementController.previewArchiveAttachment` |
| `FDC_URL/api/archive-management/attachments/{attachmentId}/download` | GET | 单附件下载 | `ArchiveManagementController.downloadArchiveAttachment` |
| `FDC_URL/api/archive-management/archives/{archiveId}/attachments/download-all` | GET | 附件打包下载 | `ArchiveManagementController.downloadArchiveAttachmentsZip` |

## 5. 权限与安全

- 权限点：附件预览、附件下载（以 `/.docs/03_Security.md` 最终权限项为准）。
- 审计字段：用户、档案ID、附件ID、动作（preview/download/download-all）、结果、时间、失败原因。
- 安全要求：仅允许访问当前用户有数据权限的档案附件。

## 6. 测试计划

| 用例ID | 场景 | 期望结果 |
|---|---|---|
| TC-AP-01 | 有权限用户预览附件 | 返回可读资源流，浏览器可打开 |
| TC-AP-02 | 无权限用户预览附件 | 返回权限错误 |
| TC-AP-03 | 下载全部附件 | 返回 zip 且包含目标档案附件 |

## 7. 验收标准（AC）

- **AC-01**：详情页点击预览可打开附件内容，行为与文件类型一致。  
- **AC-02**：无权限时预览/下载均被拦截并提示。  
- **AC-03**：批量下载可获得完整附件压缩包并产生审计记录。  

## 8. 冲突点与建议

- 历史文档中的“外部 EDM 预览入口”与当前实现不一致；本版以现有流式实现为准。  
