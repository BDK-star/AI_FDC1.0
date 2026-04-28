1. 项目概览 (Project Overview)
项目名称: Finance Document System (财经文档系统)

当前版本: v0.1.0 (迭代期)

业务背景: 本系统旨在为中大型企业提供符合 2026 年合规标准的财经文档数字化管理方案，涵盖了电子档案/文档全生命周期管理。

核心价值：该系统不仅是一个存储文档的“仓库”，更是一个集规范化、自动化、智能化、资产化于一体的数字资产治理平台。其核心价值可以概括为以下四个维度：全生命周期的合规管控（合规价值）、从“被动归档”到“主动集成”（效率价值）、从“数据孤岛”到“知识服务”（利用价值）、数字化决策与运营透明（治理价值）

2. 核心功能路线图 (Features & Roadmap)
本项目分为十个核心模块，详见 features/ 目录：

F01 基础数据管理

    管理配置：公司（公司编码 company_code、公司名称 company_name）、文档类型、业务模块、文档组织

    管理规则：条码生成规则、文档编码生成规则、归档流向匹配规则

    管理库房：库房维护、库位维护

F02 应归档数据：自动集成应归档数据、手工创建应归档数据、批量调整应归档数据、应归档数据查询、编辑应归档数据（前端页面名称：应归档数据管理，原型 @.docs/features/F03/reference_html/pages/data_maintenance.html；创建/编辑页面对应 document_create.html、document_edit.html；规范见 @.docs/07_FrontendDesignSpec.md）

F03 文档查询：查询文档、文档详情、文档导出、附件预览、附件下载、智能搜索、文档推荐

F04 文档移交：移交申请、移交接收、移交电子流查询

F05 文档归档：核销、成册、入库、档案更新

F06 文档转库：转库申请、转库电子流查询

F07 借阅文档：借阅申请、借阅办理、续借申请、借阅记录管理、智能借阅

F08 销毁文档：提交申请、审核申请、业务鉴定、综合鉴定、汇总清单、销毁审批、出库审批、实施销毁（规格按节点拆分见 @.docs/features/F08_Index.md；查询/待办、自动规则、报告与清理等为横切能力，见各节点与 `F08_00_Overview.md`）

F09 运营可视：管理运营、归档管理可视、借阅管理可视、盘点管理可视、数据统计分析

F10 管理安全：四性检测、容灾备份、智能脱敏、审计日志

3. 技术架构 (Tech Stack 2026)
在 Cursor 生成代码时，必须严格执行以下技术路线：

前端框架: Vue3

样式库: Vuetify

数据库: PostgreSQL + Redis

向量库: pgvector (用于语义搜索功能)

后端：Spring Boot + MyBatis

4. 关键规范参考 (Truth Sources)
任何代码生成、接口设计、规格输出必须参考以下文件：

规范优先级（执行版，必须遵守）：

1) 数据字段、类型、口径、约束冲突时，以 @.docs/01_DataModel.md 为准。  
2) 流程状态、审批节点冲突时，以 @.docs/02_Workflow.md 为准。  
3) 权限、脱敏、审计、安全控制冲突时，以 @.docs/03_Security.md 为准。  
4) 术语冲突时，以 @.docs/04_Glossary.md 为准。  
5) 编写/修改 @.docs/features/*.md 前，必须先读取 @.docs/01_DataModel.md，并在文档内标注新增/变更字段映射。  
6) 若与数据模型有冲突，禁止自行重定义；需在功能文档标注“冲突点+建议修订”，并保持与数据模型一致。  
7) REST API 的路径、方法、命名、分页与错误体等冲突时，以 @.docs/05_API_Conventions.md 为准；具体端点与契约写在对应 features 文档中。  
8) 快码/字典类字段的**存储值与含义**冲突时，以 @.docs/06_Lookup.md 为准；数据列类型与命名仍以 @.docs/01_DataModel.md 为准。  

全局术语: @.docs/04_Glossary.md (变量命名严禁背离术语表)

数据模型: @.docs/01_DataModel.md (命名规范与 Decimal 精度要求)

API 规范: @.docs/05_API_Conventions.md (RESTful 全局约定；端点与契约见 features)

Lookup 快码: @.docs/06_Lookup.md (全局字典取值；库表仅存码值时含义以本文为准)

安全基准: @.docs/03_Security.md (权限逻辑、脱敏规则)

规则约束: @.cursorrules (全局开发风格限制)

前端设计规范（视觉 + 交互）： @.docs/07_FrontendDesignSpec.md（含页面布局、组件视觉、按钮与表单规范等）

features输出模板: @.docs/features/Template.md

5. 开发哲学 (AI Collaboration Policy)
文档驱动开发 (DDD): 所有功能必须先在 features/ 下更新 Markdown 规格，再要求 Cursor 实施代码编写。

先验证再执行: 复杂逻辑要求 Cursor 给出伪代码逻辑，BA (本人) 确认后方可落地。

测试先行: 重要计算逻辑，必须在 tests/ 下同步生成自动化测试用例 。

