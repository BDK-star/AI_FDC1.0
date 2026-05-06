import { fetchDictionaryItems } from '../../api/modules/dictionary'
import type { LabelValueOption } from '../../types'

/** 与后端 Flyway V107 / loadDictionaryOptions("ARCHIVE_CUSTODY_STATUS") 一致 */
export const ARCHIVE_CUSTODY_STATUS_CATEGORY = 'ARCHIVE_CUSTODY_STATUS'

/**
 * 保管状态下拉：优先走字典 API，与「配置中心-字典管理」实时一致；
 * 字典不可用（未迁移、网络错误）时回退到创建选项接口中的列表。
 */
export async function resolveCustodyStatusLabelOptions(fallback: LabelValueOption[]): Promise<LabelValueOption[]> {
  try {
    const items = await fetchDictionaryItems(ARCHIVE_CUSTODY_STATUS_CATEGORY)
    const mapped = items
      .filter((i) => i.enabledFlag === 'Y')
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
      .map((i) => ({ code: i.itemCode, name: i.itemName }))
    if (mapped.length > 0) {
      return mapped
    }
  } catch {
    /* category 不存在或非字典环境 */
  }
  return fallback
}
