import type { BusinessModuleNode, CountryRegionItem } from '../types'

/** 与「归档流向管理」新建规则中业务模块 el-tree-select 节点结构一致 */
export type BusinessModuleTreeSelectNode = { value: string; label: string; children: BusinessModuleTreeSelectNode[] }

export function mapBusinessModuleToTreeOption(node: BusinessModuleNode): BusinessModuleTreeSelectNode {
  return {
    value: node.moduleCode,
    label: `${node.moduleCode} - ${node.moduleName}`,
    children: (node.children || []).map(mapBusinessModuleToTreeOption)
  }
}

/** 与「归档流向管理」归档地 el-cascader 的 options 结构一致（国家 / 省 / 市） */
export type ArchiveDestinationCascaderOption = { value: string; label: string; children?: ArchiveDestinationCascaderOption[] }

export function buildArchiveDestinationCascaderOptions(
  countryOptions: CountryRegionItem[],
  provinceOptions: CountryRegionItem[],
  cityOptions: CountryRegionItem[]
): ArchiveDestinationCascaderOption[] {
  const provinceByCountry = new Map<string, CountryRegionItem[]>()
  const cityByProvince = new Map<string, CountryRegionItem[]>()
  for (const province of provinceOptions) {
    const parent = province.parentRegionCode || ''
    if (!provinceByCountry.has(parent)) provinceByCountry.set(parent, [])
    provinceByCountry.get(parent)!.push(province)
  }
  for (const city of cityOptions) {
    const parent = city.parentRegionCode || ''
    if (!cityByProvince.has(parent)) cityByProvince.set(parent, [])
    cityByProvince.get(parent)!.push(city)
  }
  return countryOptions.map((country) => ({
    value: country.regionCode,
    label: country.regionName,
    children: (provinceByCountry.get(country.regionCode) || []).map((province) => ({
      value: province.regionCode,
      label: province.regionName,
      children: (cityByProvince.get(province.regionCode) || []).map((city) => ({
        value: city.regionCode,
        label: city.regionName
      }))
    }))
  }))
}

/**
 * 将已保存的 `arch_place_alpha2_code` 还原为级联路径。
 * 支持任选一级：仅国家、仅省、市（叶子），与 `checkStrictly: true` 的级联器一致。
 */
export function buildArchiveDestinationPath(
  storedCode: string | undefined,
  countryOptions: CountryRegionItem[],
  provinceOptions: CountryRegionItem[],
  cityOptions: CountryRegionItem[]
): string[] {
  const code = String(storedCode || '').trim()
  if (!code) return []

  const city = cityOptions.find((item) => item.regionCode === code)
  if (city) {
    const provinceCode = city.parentRegionCode || ''
    const province = provinceOptions.find((item) => item.regionCode === provinceCode)
    const countryCode = province?.parentRegionCode || ''
    return [countryCode, provinceCode, code].filter(Boolean)
  }

  const province = provinceOptions.find((item) => item.regionCode === code)
  if (province) {
    const countryCode = province.parentRegionCode || ''
    return [countryCode, code].filter(Boolean)
  }

  if (countryOptions.some((c) => c.regionCode === code)) {
    return [code]
  }

  return [code]
}

export function findBusinessModuleNameByCode(nodes: BusinessModuleNode[], code: string): string {
  for (const n of nodes) {
    if (n.moduleCode === code) return n.moduleName
    const nested = findBusinessModuleNameByCode(n.children || [], code)
    if (nested) return nested
  }
  return ''
}
