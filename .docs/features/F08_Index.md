# F08 销毁文档（索引）

本目录为 F08「销毁文档」电子流的功能规格真相来源（SSoT），**按流程节点**拆分子文档，便于评审、实现与验收对齐。

## 1. 文档清单（按流程顺序）

| 顺序 | 节点（中文） | 文件 |
|---:|---|---|
| 0 | 总览与节点关系 | `F08/F08_00_Overview.md` |
| 1 | 提交申请 | `F08/F08_01_SubmitApplication.md` |
| 2 | 审核申请 | `F08/F08_02_ReviewApplication.md` |
| 3 | 业务鉴定 | `F08/F08_03_BusinessAppraisal.md` |
| 4 | 综合鉴定 | `F08/F08_04_ComprehensiveAppraisal.md` |
| 5 | 汇总清单 | `F08/F08_05_AggregateList.md` |
| 6 | 销毁审批 | `F08/F08_06_DestructionApproval.md` |
| 7 | 出库审批 | `F08/F08_07_OutboundApproval.md` |
| 8 | 实施销毁 | `F08/F08_08_ExecuteDestruction.md` |

## 2. 强制遵循

- 需求规格展开结构：`/.docs/features/Template.md`
- 数据模型：`/.docs/01_DataModel.md`（本功能涉及实体见总览 §4 占位；定稿后各子文档 §6 须补字段映射）
- 工作流统一口径：`/.docs/02_Workflow.md`（当前若为空，以 `F08_00` 状态机为草案，再回写 `02`）
- API 全局约定：`/.docs/05_API_Conventions.md`
- 安全与权限：`/.docs/03_Security.md`
- 术语：`/.docs/04_Glossary.md`
- 快码：`/.docs/06_Lookup.md`
