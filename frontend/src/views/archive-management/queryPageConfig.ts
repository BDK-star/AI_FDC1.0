export type MoreFieldShowFor =
  | 'ALL'
  | 'ACCOUNTING_OR_TAX'
  | 'ACCOUNTING'
  | 'FUND'
  | 'FUND_OR_NON_FIN'
  | 'TAX_OR_FUND'
  | 'NON_FIN'

export interface MoreFilterFieldConfig {
  key: string
  label: string
  type: 'input' | 'select' | 'daterange' | 'cascader'
  placeholder?: string
  /** 单行 + 右侧抽屉多行录入（文档业务编码、发票号等） */
  multilineDrawer?: boolean
  multiple?: boolean
  showFor?: MoreFieldShowFor
  options?: Array<{ label: string; value: string }>
  /**
   * barcodeModules — 条码模块配置
   * geoCountries — 国家维表（与新建文档/产生地一致）
   * archiveDestinationCascader — 与「归档规则管理」相同的国家/省/市三级级联，值为叶子城市 regionCode
   */
  optionSource?: 'barcodeModules' | 'geoCountries' | 'archiveDestinationCascader'
}

export interface ListColumnConfig {
  label: string
  prop: string
  minWidth?: number
  width?: number | string
  showOverflow?: boolean
}

const allMoreFields: MoreFilterFieldConfig[] = [
  {
    key: 'barcodeModuleCodes',
    label: '条码模块',
    type: 'select',
    multiple: true,
    showFor: 'ALL',
    placeholder: '按业务模块映射的条码模块筛选（配置中心已启用）',
    optionSource: 'barcodeModules'
  },
  {
    key: 'documentArchiveTypes',
    label: '档案类型',
    type: 'select',
    multiple: true,
    showFor: 'ALL',
    placeholder: '可多选（对应 fdc_document_t.arch_type_code）'
  },
  { key: 'country', label: '国家', type: 'select', multiple: false, showFor: 'ALL' },
  { key: 'repOffice', label: '代表处', type: 'select', multiple: false, showFor: 'ALL' },
  { key: 'region', label: '地区部', type: 'select', multiple: false, showFor: 'ALL' },
  { key: 'custodyStatus', label: '保管状态', type: 'select', multiple: true, showFor: 'ALL' },
  { key: 'securityLevelCode', label: '密级', type: 'select', multiple: true, showFor: 'ALL' },
  { key: 'description', label: '描述', type: 'input', showFor: 'ALL' },
  {
    key: 'archiveDestination',
    label: '归档地',
    type: 'cascader',
    showFor: 'ALL',
    placeholder: '请选择国家、省份或城市（任意一级）',
    optionSource: 'archiveDestinationCascader'
  },
  {
    key: 'originPlace',
    label: '产生地',
    type: 'select',
    multiple: false,
    showFor: 'ALL',
    placeholder: '请选择国家',
    optionSource: 'geoCountries'
  },
  { key: 'dutyPerson', label: '归档责任人', type: 'input', showFor: 'ALL' },
  { key: 'respDept', label: '文档责任部门', type: 'input', placeholder: '请选择', showFor: 'ALL' },
  { key: 'createdBy', label: '创建人', type: 'input', showFor: 'ALL' },
  { key: 'creationDateRange', label: '创建日期', type: 'daterange', showFor: 'ALL' },
  { key: 'sourceSystem', label: '系统来源', type: 'select', multiple: true, showFor: 'ALL' },
  { key: 'archivedEntityName', label: '归档主体名称（含历史）', type: 'select', showFor: 'ALL' },
  { key: 'archiveBarcodeRange', label: '档案条码', type: 'input', showFor: 'ALL' },
  { key: 'signDateRange', label: '签收日期', type: 'daterange', showFor: 'ALL' },
  { key: 'signedBy', label: '签收人', type: 'select', multiple: false, showFor: 'ALL', placeholder: '请选择签收人' },
  { key: 'verificationDateRange', label: '核销日期', type: 'daterange', showFor: 'ALL' },
  { key: 'verifiedBy', label: '核销人', type: 'select', showFor: 'ALL' },
  { key: 'volumeSeqNo', label: '文档编号', type: 'input', showFor: 'ALL' },
  { key: 'volumeBarcodeRange', label: '册条码', type: 'input', showFor: 'ALL' },
  { key: 'volumizationDateRange', label: '成册日期', type: 'daterange', showFor: 'ALL' },
  { key: 'assembledBy', label: '成册人', type: 'select', showFor: 'ALL' },
  { key: 'volumeNoRange', label: '册号', type: 'input', showFor: 'ALL' },
  { key: 'repository', label: '库房', type: 'input', showFor: 'ALL' },
  { key: 'storageLocationRange', label: '库位', type: 'input', showFor: 'ALL' },
  { key: 'storageDateRange', label: '入库日期', type: 'daterange', showFor: 'ALL' },
  { key: 'storedBy', label: '入库人', type: 'select', showFor: 'ALL' },
  { key: 'copies', label: '份数', type: 'input', showFor: 'ALL' },
  { key: 'remainingCopies', label: '剩余份数', type: 'input', showFor: 'ALL' },
  { key: 'visibility', label: '是否可见', type: 'select', multiple: false, showFor: 'ALL' },
  // 历史硬编码扩展筛选（会计/保函等）已迁移为“按业务模块动态展示”，此处不再静态配置。
]

/** 文档查询「更多筛选」中暂不展示的字段（移除对应 key 即可重新启用） */
const archiveQueryExcludedKeys = new Set<string>(['archivedEntityName'])

const pendingExcludedKeys = new Set([
  'archivedEntityName',
  'archiveBarcodeRange',
  'signDateRange',
  'signedBy',
  'verificationDateRange',
  'verifiedBy',
  'volumeSeqNo',
  'volumeBarcodeRange',
  'volumizationDateRange',
  'assembledBy',
  'volumeNoRange',
  'repository',
  'storageLocationRange',
  'storageDateRange',
  'storedBy',
  'archiveType'
])

export const archiveQueryMoreFields = allMoreFields.filter((f) => !archiveQueryExcludedKeys.has(f.key))
export const pendingQueryMoreFields = allMoreFields.filter((f) => !pendingExcludedKeys.has(f.key))

export const archiveQueryColumns: ListColumnConfig[] = [
  { label: '文档业务编码', prop: 'businessCode', width: '180' },
  { label: '公司', prop: 'companyProjectName', width: '160' },
  { label: '业务模块', prop: 'archiveTypeCode', width: '120' },
  { label: '开始档期', prop: 'beginPeriod', width: '120' },
  { label: '结束档期', prop: 'endPeriod', width: '120' },
  { label: '归档地', prop: 'archiveDestination', width: '140' },
  { label: '产生地', prop: 'originPlace', width: '140' },
  { label: '文档组织', prop: 'documentOrganizationCode', width: '140' },
  { label: '文档状态', prop: 'archiveStatus', width: '120' },
  { label: '文档名称', prop: 'documentName', minWidth: 220 },
  { label: '文档生成日期', prop: 'documentDate', width: '150' },
  { label: '归档责任人', prop: 'dutyPerson', width: '120' },
  { label: '文档责任部门', prop: 'dutyDepartment', width: '140' },
  { label: '载体类型', prop: 'carrierTypeCode', width: '120' },
  { label: '是否可见', prop: 'documentVisibility', width: '100' },
  { label: '系统来源', prop: 'sourceSystem', width: '140' },
  { label: '密级', prop: 'securityLevelName', width: '120' },
  { label: '描述', prop: 'remark', minWidth: 180 },
  { label: '创建时间', prop: 'creationDate', width: '160' },
  { label: '创建人', prop: 'createdByName', width: '120' }
]

export const archiveQueryDefaultVisibleColumns = [
  'businessCode',
  'companyProjectName',
  'archiveTypeCode',
  'beginPeriod',
  'archiveDestination',
  'documentOrganizationCode',
  'archiveStatus',
  'documentName',
  'documentDate',
  'dutyPerson'
]

export const pendingQueryColumns: ListColumnConfig[] = [
  { label: '文档业务编码', prop: 'businessCode', minWidth: 160 },
  { label: '公司', prop: 'companyEntity', minWidth: 140, showOverflow: true },
  { label: '业务模块', prop: 'businessModule', width: 120 },
  { label: '开始档期', prop: 'startPeriod', width: 120 },
  { label: '结束档期', prop: 'endPeriod', width: 120 },
  { label: '归档地', prop: 'archivePlace', minWidth: 120 },
  { label: '产生地', prop: 'originPlace', minWidth: 120 },
  { label: '文档组织', prop: 'docOrganization', minWidth: 120 },
  { label: '文档状态', prop: 'docStatus', width: 120 },
  { label: '文档名称', prop: 'documentName', minWidth: 180, showOverflow: true },
  { label: '文档生成日期', prop: 'docGenerationDate', width: 150 },
  { label: '归档责任人', prop: 'owner', width: 120 },
  { label: '文档责任部门', prop: 'responsibleDept', minWidth: 140 },
  { label: '载体类型', prop: 'carrierType', width: 120 },
  { label: '是否可见', prop: 'visibility', width: 100 },
  { label: '系统来源', prop: 'sourceSystem', width: 120 },
  { label: '密级', prop: 'securityLevelName', width: 120 },
  { label: '描述', prop: 'description', minWidth: 160, showOverflow: true },
  { label: '创建时间', prop: 'creationTime', width: 170 },
  { label: '创建人', prop: 'createdBy', width: 100 }
]

export const pendingQueryDefaultVisibleColumns = pendingQueryColumns.map((c) => c.prop)

export interface QueryPageConfig {
  key: 'archiveQuery' | 'pendingArchiveQuery'
  moreFilterFields: MoreFilterFieldConfig[]
  columns: ListColumnConfig[]
  defaultVisibleColumns: string[]
}

export function getVisibleMoreFilterFields(fields: MoreFilterFieldConfig[], documentTypeName: string) {
  const isAccounting = documentTypeName.includes('会计')
  const isTax = documentTypeName.includes('税务')
  const isFund = documentTypeName.includes('资金')
  const isNonFin = documentTypeName.includes('非财经')

  return fields.filter((f) => {
    switch (f.showFor || 'ALL') {
      case 'ACCOUNTING_OR_TAX':
        return isAccounting || isTax
      case 'ACCOUNTING':
        return isAccounting
      case 'FUND':
        return isFund
      case 'FUND_OR_NON_FIN':
        return isFund || isNonFin
      case 'TAX_OR_FUND':
        return isTax || isFund
      case 'NON_FIN':
        return isNonFin
      default:
        return true
    }
  })
}

export const archiveQueryPageConfig: QueryPageConfig = {
  key: 'archiveQuery',
  moreFilterFields: archiveQueryMoreFields,
  columns: archiveQueryColumns,
  defaultVisibleColumns: archiveQueryDefaultVisibleColumns
}

export const pendingArchiveQueryPageConfig: QueryPageConfig = {
  key: 'pendingArchiveQuery',
  moreFilterFields: pendingQueryMoreFields,
  columns: pendingQueryColumns,
  defaultVisibleColumns: pendingQueryDefaultVisibleColumns
}
