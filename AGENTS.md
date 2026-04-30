# Repository Guidelines

## Agent Workflow

Keep guidance practical and update this file only when a repeated mistake or workflow rule is worth reusing. Before changing code, inspect the relevant backend, frontend, and migration paths; do not guess from UI text alone. Preserve user changes in the working tree and avoid broad rewrites unrelated to the task.

## Project Structure

- `backend/`: Java 17 Spring Boot service. Main code is in `src/main/java/com/smartarchive`; MyBatis XML is in `src/main/resources/mapper`; Flyway migrations are in `src/main/resources/db/migration`.
- `frontend/`: Vue 3 + TypeScript + Vite app. Use `src/views`, `src/components`, `src/api/modules`, `src/router`, `src/stores`, and `src/types`.
- `docs/`: project documentation. Startup notes live in `docs/guides/项目启动参考.md`.
- `samples/`, `scripts/`, and root-level generator scripts are local data utilities.

## Local Startup

Start dependencies first: PostgreSQL, Redis, then backend, then frontend.

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local
cd frontend && npm run dev -- --host 127.0.0.1
```

The local profile is `backend/src/main/resources/application-local.yml`; it currently points to `smart_archive_clean`, Redis `localhost:6379`, and local Tesseract OCR paths.

## Build & Verification

Use these checks before claiming work is complete:

```bash
cd backend && mvn -o -q -DskipTests compile
cd frontend && npm run build
```

For database changes, verify both schema and Flyway state:

```bash
psql smart_archive_clean -c "select version,description,success from flyway_schema_history order by installed_rank desc limit 5;"
```

For API contract changes, confirm actual JSON with `curl` and keep `frontend/src/types` aligned with backend DTO names.

## Backend Conventions

Use package-by-feature layout with `controller`, `service`, `domain`, `dto`, and `mapper`. Java classes use `PascalCase`; fields and methods use `camelCase`. Prefer DTOs for API responses instead of exposing persistence entities. Flyway files must be append-only and named like `V87__archive_create_session_busi_module_guess.sql`; make migrations idempotent when local databases may already be partially repaired.

## Frontend Conventions

Vue view/component files use `PascalCase`. API calls belong in `frontend/src/api/modules`; shared shapes belong in `frontend/src/types`. If a backend response field changes, update the TypeScript interface and every consumer in the same change.

## Do Not Rules

Do not commit `node_modules`, generated `dist` assets, `target/`, local installers, database dumps, or secrets. Do not manually edit already-applied migrations; add a new migration. Do not use browser automation for user-sensitive file selection unless the user explicitly names the file/path.

## Commits & PRs

Follow existing prefixes: `feat:`, `fix:`, `chore:`. PRs should summarize backend/frontend impact, migrations, verification commands, and screenshots for UI-visible changes.
