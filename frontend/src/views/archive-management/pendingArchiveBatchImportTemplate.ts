import { fetchPendingArchiveExtFieldsUnion } from '../../api/modules/businessModule'
import type { BusinessModuleExtField, DocumentTypeExtField } from '../../types'
import { extKeyForBusinessField } from './pendingArchiveExtShared'

/**
 * 与应归档创建页 / 详情页及后端 PendingArchiveBatchImportHeaderResolver 使用的中文列名一致。
 * 硬编码扩展方案已废弃：动态列完全由业务模块「应归档数据」BASIC 并集决定，不再维护固定黑名单；仅与下方核心列英文键去重。
 * label：CSV 表头；key：落库用字段键（与业务模块 englishFieldName 或 fieldCode 一致）。
 */
export const PENDING_BATCH_IMPORT_CORE_DISPLAY_FIELDS: { label: string; key: string }[] = [
  { label: '文档业务编码', key: 'businessCode' },
  { label: '公司', key: 'companyProjectCode' },
  { label: '业务模块', key: 'archiveTypeCode' },
  { label: '开始档期', key: 'beginPeriod' },
  { label: '结束档期', key: 'endPeriod' },
  { label: '归档地', key: 'archiveDestination' },
  { label: '产生地', key: 'originPlace' },
  { label: '文档名称', key: 'documentName' },
  { label: '文档生成日期', key: 'documentDate' },
  { label: '归档责任人', key: 'dutyPerson' },
  { label: '载体类型', key: 'carrierTypeCode' },
  { label: '系统来源', key: 'sourceSystem' },
  { label: '密级', key: 'securityLevelCode' },
  { label: '描述', key: 'remark' },
  { label: '保管状态', key: 'custodyStatus' }
]

const CORE_KEYS = new Set(PENDING_BATCH_IMPORT_CORE_DISPLAY_FIELDS.map((r) => r.key))

function escapeCsvCell(v: string): string {
  const s = v ?? ''
  if (/[",\n\r]/.test(s)) {
    return `"${s.replace(/"/g, '""')}"`
  }
  return s
}

function rowToCsvLine(cells: string[]): string {
  return cells.map(escapeCsvCell).join(',')
}

function collectConfiguredExtColumns(
  fields: DocumentTypeExtField[],
  seen: Set<string>
): { labels: string[]; keys: string[] } {
  const labels: string[] = []
  const keys: string[] = []
  const sorted = [...fields]
    .filter((f) => f.enabledFlag === 'Y')
    .sort((a, b) => (a.formSortOrder ?? 0) - (b.formSortOrder ?? 0) || (a.fieldCode || '').localeCompare(b.fieldCode || ''))
  for (const f of sorted) {
    const code = (f.fieldCode || '').trim()
    if (!code || seen.has(code) || CORE_KEYS.has(code)) continue
    seen.add(code)
    const label = (f.fieldName || '').trim() || code
    labels.push(label)
    keys.push(code)
  }
  return { labels, keys }
}

export interface BatchImportTemplateContext {
  documentTypeCode: string
  documentTypeName: string
  companyProjectCode?: string
  archiveTypeCode?: string
}

function buildLabelKeyColumns(ctx: BatchImportTemplateContext, cfgFields: DocumentTypeExtField[]): { labels: string[]; keys: string[] } {
  const labels: string[] = []
  const keys: string[] = []
  const seen = new Set<string>()
  for (const row of PENDING_BATCH_IMPORT_CORE_DISPLAY_FIELDS) {
    labels.push(row.label)
    keys.push(row.key)
    seen.add(row.key)
  }
  const extra = collectConfiguredExtColumns(cfgFields, seen)
  labels.push(...extra.labels)
  keys.push(...extra.keys)
  return { labels, keys }
}

function sampleRowForKeys(keys: string[], ctx: BatchImportTemplateContext): string[] {
  const company = (ctx.companyProjectCode || '').trim() || '0002GL00003'
  const archiveType = (ctx.archiveTypeCode || '').trim() || 'GL'
  const sampleByKey: Record<string, string> = {
    companyProjectCode: company,
    archiveTypeCode: archiveType,
    businessCode: `${company}-TAX-001`,
    beginPeriod: '2024-01',
    endPeriod: '2024-01',
    archiveDestination: 'SHANGHAI',
    originPlace: 'SHANGHAI',
    documentName: ctx.documentTypeName.includes('税务') ? '示例税务文档' : '示例文档',
    documentDate: '2024-01-15 10:00:00',
    dutyPerson: '系统',
    carrierTypeCode: 'ELECTRONIC',
    sourceSystem: 'PORTAL',
    securityLevelCode: 'INTERNAL',
    remark: '',
    custodyStatus: 'IN_STORAGE'
  }
  return keys.map((k) => sampleByKey[k] ?? '')
}

/** 不请求接口时的降级模板（仅核心列，无动态扩展列）。 */
export function buildCoreOnlyPendingBatchTemplateCsv(ctx: BatchImportTemplateContext): string {
  const { labels, keys } = buildLabelKeyColumns(ctx, [])
  return [rowToCsvLine(labels), rowToCsvLine(sampleRowForKeys(keys, ctx))].join('\n')
}

function unionToTemplateFields(union: BusinessModuleExtField[]): DocumentTypeExtField[] {
  return (union || []).map((f) => ({
    fieldId: f.fieldId,
    fieldCode: extKeyForBusinessField(f),
    documentTypeCode: f.moduleCode ?? '',
    usageModule: '',
    relatedModuleCode: '',
    relatedField: '',
    fieldName: f.fieldName,
    fieldType: 'TEXT',
    requiredFlag: 'N',
    enabledFlag: f.enabledFlag,
    formSortOrder: f.sortOrder ?? 0,
    queryEnabledFlag: 'N',
    querySortOrder: 0
  }))
}

/** 后半段为文档类型根与子树业务模块上已启用「应归档数据」BASIC 扩展的并集 */
export async function buildPendingArchiveBatchImportTemplateCsv(ctx: BatchImportTemplateContext): Promise<string> {
  const union = await fetchPendingArchiveExtFieldsUnion(ctx.documentTypeCode.trim())
  const fields = unionToTemplateFields(union)
  const { labels, keys } = buildLabelKeyColumns(ctx, fields)
  return [rowToCsvLine(labels), rowToCsvLine(sampleRowForKeys(keys, ctx))].join('\n')
}
