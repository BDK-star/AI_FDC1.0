/**
 * 应归档创建/编辑/详情：扩展信息区与「业务模块配置」BASIC + 应归档数据 一致（fdc_business_module_ext_field_t）。
 */
import { fetchBusinessModuleExtFields } from '../../api/modules/businessModule'
import type { BusinessModuleExtField } from '../../types'

export const COMPANY_SYNC_EXT_KEYS = ['country', 'repOffice', 'region', 'companyTag'] as const

export function extKeyForBusinessField(f: BusinessModuleExtField): string {
  return String(f.englishFieldName || f.fieldCode || '').trim()
}

/** 与 PendingArchiveCreateView.syncReceivableModuleExtFields 相同筛选 */
export async function fetchReceivableBasicExtFields(moduleCode: string): Promise<BusinessModuleExtField[]> {
  const code = moduleCode.trim()
  if (!code) return []
  const all = await fetchBusinessModuleExtFields(code, 'BASIC')
  return (all || [])
    .filter((f) => f.enabledFlag === 'Y')
    .filter((f) => (f.applicationFunctions || []).includes('应归档数据'))
    .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
}
