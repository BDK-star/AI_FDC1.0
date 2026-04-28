# V39 / V40 执行模板

以下模板用于 PostgreSQL + Flyway 场景。  
请先确认已完成数据库备份。

## 0) 发布前准备

- 备份库（至少含 `fdc_document_t`、`fdc_document_attach_t`、`fdc_arch_storage_t`）
- 确认本次版本已包含：
  - `V39__align_fdc_document_t_schema.sql`
  - `V40__drop_legacy_fdc_document_t_backup.sql`
  - `/.docs/sql/V39_post_migration_validation.sql`
  - `/.docs/sql/V39_post_migration_validation_criteria.md`

## 1) 执行迁移到 V39

按你们项目现有方式执行 Flyway migrate（示例）：

```bash
# 示例：Maven + Flyway（按项目实际参数替换）
cd backend
mvn flyway:migrate
```

如果你们使用应用启动自动迁移，则发布应用后确认迁移日志已执行到 `V39`。

### 本项目可直接用的命令（推荐）

根据当前仓库配置，可在 `backend` 目录直接执行：

```bash
cd backend
mvn flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/smart_archive_clean \
  -Dflyway.user=postgres \
  -Dflyway.password='<YOUR_DB_PASSWORD>' \
  -Dflyway.locations=filesystem:src/main/resources/db/migration
```

说明：`<YOUR_DB_PASSWORD>` 请替换为你本地数据库密码。

## 2) 执行校验 SQL

```bash
# 示例：使用 psql 执行校验脚本（按实际连接信息替换）
psql "host=<HOST> port=<PORT> dbname=<DB> user=<USER> password=<PASSWORD>" \
  -f ".docs/sql/V39_post_migration_validation.sql"
```

本项目对应可用模板：

```bash
psql "host=localhost port=5432 dbname=smart_archive_clean user=postgres password=<YOUR_DB_PASSWORD>" \
  -f ".docs/sql/V39_post_migration_validation.sql"
```

## 3) 按标准判定

依据文档：

- `/.docs/sql/V39_post_migration_validation_criteria.md`

重点阻断项：

- 行数不一致
- `missing_doc_id_count > 0`
- 必填字段检查任一非 0
- 外键悬挂非 0
- `(tenantid, doc_biz_no)` 重复

## 4) 通过后执行 V40（清理备份表）

确认通过后再执行：

```bash
cd backend
mvn flyway:migrate
```

说明：`V40` 会删除 `fdc_document_t_legacy_v33`，执行后将失去库内回退比对表。

## 5) 回滚建议（简）

- 若 `V39` 后不通过：暂停执行 `V40`，优先基于备份回滚。
- 若误执行 `V40`：仅可使用发布前备份回滚，不再有 `fdc_document_t_legacy_v33`。

