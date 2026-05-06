import http, { apiRequest } from '../http'
import type { BusinessModuleExtField, BusinessModuleNode } from '../../types'

/** 与「业务模块配置」查询区树形下拉相同的节点结构（编码 ｜ 名称） */
export type ModuleQueryTreeNode = BusinessModuleNode & {
  queryLabel: string
  children?: ModuleQueryTreeNode[]
}

export function buildModuleQueryTree(nodes: BusinessModuleNode[]): ModuleQueryTreeNode[] {
  return nodes.map((node) => ({
    ...node,
    queryLabel: `${node.moduleCode} ｜ ${node.moduleName}`,
    children: buildModuleQueryTree(node.children || [])
  }))
}

export function isModuleCodeWithinDocumentType(
  nodes: BusinessModuleNode[],
  documentTypeCode: string,
  moduleCode: string
): boolean {
  const root = String(documentTypeCode || '').trim()
  const target = String(moduleCode || '').trim()
  if (!root || !target) return false
  if (root === target) return true
  const node = findModuleNodeByCode(nodes, target)
  if (!node) return false
  const ancestors = String(node.ancestorPath || '')
    .split('/')
    .map((s) => s.trim())
    .filter(Boolean)
  return ancestors.includes(root)
}

export function filterBusinessModuleTreeByDocumentType(
  nodes: BusinessModuleNode[],
  documentTypeCode: string
): BusinessModuleNode[] {
  const root = String(documentTypeCode || '').trim()
  if (!root) return nodes
  const cloned = cloneModuleTree(nodes)
  const rootNode = cloned.find((node) => String(node.moduleCode || '').trim() === root)
  return rootNode ? [rootNode] : []
}

export function resolveRootDocumentTypeCodeByModule(
  nodes: BusinessModuleNode[],
  moduleCode: string
): string {
  const target = String(moduleCode || '').trim()
  if (!target) return ''
  const node = findModuleNodeByCode(nodes, target)
  if (!node) return ''
  const ancestors = String(node.ancestorPath || '')
    .split('/')
    .map((s) => s.trim())
    .filter(Boolean)
  return ancestors[0] || String(node.moduleCode || '').trim()
}

function findModuleNodeByCode(nodes: BusinessModuleNode[], moduleCode: string): BusinessModuleNode | null {
  for (const node of nodes) {
    if (String(node.moduleCode || '').trim() === moduleCode) return node
    const found = findModuleNodeByCode(node.children || [], moduleCode)
    if (found) return found
  }
  return null
}

function cloneModuleTree(nodes: BusinessModuleNode[]): BusinessModuleNode[] {
  return (nodes || []).map((node) => ({
    ...node,
    children: cloneModuleTree(node.children || [])
  }))
}

export interface BusinessModuleCommand {
  moduleCode: string
  moduleName: string
  parentCode?: string
  enabledFlag: 'Y' | 'N'
  sortOrder?: number
  securityLevelCode?: string
  integrationType?: '全部集成' | '部分集成' | '不集成'
  description?: string
  remark?: string
  /** 条码模块编码，不映射可省略 */
  barcodeModuleCode?: string | null
}

export interface BusinessModuleUpdateCommand {
  moduleName: string
  parentCode?: string
  enabledFlag: 'Y' | 'N'
  sortOrder?: number
  securityLevelCode?: string
  integrationType?: '全部集成' | '部分集成' | '不集成'
  description?: string
  remark?: string
  /** true：按 barcodeModuleCode 更新（null 表示解除）；false：不改编码映射 */
  patchBarcodeModuleCode?: boolean
  /** 条码模块编码；配合 patchBarcodeModuleCode 使用 */
  barcodeModuleCode?: string | null
}

export interface BusinessModuleExtFieldCommand {
  fieldCode: string
  fieldScope: 'BASIC' | 'ATTACHMENT'
  applicationFunctions?: ('应归档数据' | '移交')[]
  extAttribute?: string
  fieldName: string
  englishFieldName?: string
  dataType: 'TEXT' | 'NUMBER' | 'DATE' | 'DATETIME' | 'DICT' | 'BOOLEAN'
  queryFlag: 'Y' | 'N'
  requiredFlag: 'Y' | 'N'
  enabledFlag: 'Y' | 'N'
  sortOrder: number
}

export interface BusinessModuleParentOption {
  code: string
  description?: string
  sourceType: 'DOCUMENT_TYPE' | 'BUSINESS_MODULE'
}

export function fetchBusinessModuleTree() {
  return apiRequest<BusinessModuleNode[]>(http.get('/api/base-data/business-modules/tree'))
}

export function fetchBusinessModuleParentOptions() {
  return apiRequest<BusinessModuleParentOption[]>(http.get('/api/base-data/business-modules/parent-options'))
}

export function createBusinessModule(data: BusinessModuleCommand) {
  return apiRequest<BusinessModuleNode>(http.post('/api/base-data/business-modules', data))
}

export function updateBusinessModule(moduleCode: string, data: BusinessModuleUpdateCommand) {
  const code = encodeURIComponent(moduleCode.trim())
  return apiRequest<BusinessModuleNode>(http.put(`/api/base-data/business-modules/${code}`, data))
}

export function deleteBusinessModule(moduleCode: string) {
  const code = encodeURIComponent(moduleCode.trim())
  return apiRequest<void>(http.delete(`/api/base-data/business-modules/${code}`))
}

export function fetchBusinessModuleExtFields(moduleCode: string, fieldScope?: 'BASIC' | 'ATTACHMENT') {
  const code = encodeURIComponent(moduleCode.trim())
  return apiRequest<BusinessModuleExtField[]>(
    http.get(`/api/base-data/business-modules/${code}/ext-fields`, { params: { fieldScope } })
  )
}

/** 文档类型根及其子树：BASIC + 应归档数据 扩展字段并集（与批量导入模板后半段一致） */
export function fetchPendingArchiveExtFieldsUnion(documentTypeRootCode: string) {
  const code = encodeURIComponent(documentTypeRootCode.trim())
  return apiRequest<BusinessModuleExtField[]>(
    http.get(`/api/base-data/business-modules/${code}/pending-archive-ext-fields-union`)
  )
}

export function createBusinessModuleExtField(moduleCode: string, data: BusinessModuleExtFieldCommand) {
  const code = encodeURIComponent(moduleCode.trim())
  return apiRequest<BusinessModuleExtField>(http.post(`/api/base-data/business-modules/${code}/ext-fields`, data))
}

export function updateBusinessModuleExtField(moduleCode: string, fieldCode: string, data: BusinessModuleExtFieldCommand) {
  const code = encodeURIComponent(moduleCode.trim())
  const fc = encodeURIComponent(fieldCode.trim())
  return apiRequest<BusinessModuleExtField>(
    http.put(`/api/base-data/business-modules/${code}/ext-fields/${fc}`, data)
  )
}

export function deleteBusinessModuleExtField(moduleCode: string, fieldCode: string) {
  const code = encodeURIComponent(moduleCode.trim())
  const fc = encodeURIComponent(fieldCode.trim())
  return apiRequest<void>(http.delete(`/api/base-data/business-modules/${code}/ext-fields/${fc}`))
}
