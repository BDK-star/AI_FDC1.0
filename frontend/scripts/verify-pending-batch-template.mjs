/**
 * 验证应归档批量模板：1) 后端单测 2) 生产包中 PendingArchiveQueryView 核心列顺序（归档责任人→载体类型）
 */
import { execSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(__dirname, '..', '..')

console.log('(1/2) mvn test PendingBatchImportTemplateServiceTest …')
execSync('mvn -q test -Dtest=PendingBatchImportTemplateServiceTest', {
  cwd: path.join(repoRoot, 'backend'),
  stdio: 'inherit',
  env: process.env
})

const distDir = path.join(repoRoot, 'frontend', 'dist', 'assets')
const files = fs.existsSync(distDir) ? fs.readdirSync(distDir).filter((f) => f.startsWith('PendingArchiveQueryView-') && f.endsWith('.js')) : []
if (files.length === 0) {
  console.warn('(2/2) skip: no frontend/dist PendingArchiveQueryView chunk — run npm run build in frontend/')
  process.exit(0)
}
const p = path.join(distDir, files[0])
const s = fs.readFileSync(p, 'utf8')
// 构建产物变量名会变化，用「归档责任人 → 载体类型」与禁止的旧序「→ 文档责任部门」判断
if (s.includes('key:`dutyPerson`},{label:`文档责任部门`')) {
  console.error('FAIL: old template column order (dutyPerson → 文档责任部门) in', files[0])
  process.exit(1)
}
if (!s.includes('key:`dutyPerson`},{label:`载体类型`')) {
  console.error('FAIL: expected dutyPerson → 载体类型 in', files[0])
  process.exit(1)
}
console.log('(2/2) OK dist chunk', files[0], 'core column order')
console.log('All checks passed.')
