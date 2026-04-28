<template>
  <div class="f02-data-maintenance" :class="{ 'f02-data-maintenance--full-table': tableFullPage }">
    <div class="f02-inner">
      <section class="f02-card f02-filters">
        <div class="f02-filter-grid">
          <div class="f02-field f02-field--required" :class="{ 'f02-field--required-missing': !docTypeReady }">
            <label>
              <span class="f02-required">*</span>文档类型
              <el-tooltip content="请选择文档类型后再进行查询与批量操作" placement="top" :disabled="docTypeReady">
                <el-icon class="f02-required-label-tip" :class="{ 'is-visible': !docTypeReady }"><WarningFilled /></el-icon>
              </el-tooltip>
            </label>
            <el-select v-model="filters.documentTypeCode" clearable filterable placeholder="请选择" class="f02-control" @change="handleDocumentTypeChange">
              <el-option v-for="t in options.documentTypes" :key="t.code" :label="t.name" :value="t.code" />
            </el-select>
          </div>
          <div class="f02-field">
            <label>公司</label>
            <el-select v-model="filters.companyCode" clearable filterable placeholder="请选择公司" class="f02-control">
              <el-option v-for="c in companySelectOptions" :key="c.code" :label="`${c.code} · ${c.name}`" :value="c.code" />
            </el-select>
          </div>
          <div class="f02-field">
            <label>业务模块</label>
            <el-tree-select
              v-model="filters.archiveTypeCode"
              :data="businessModuleTreeOptions"
              filterable
              clearable
              check-strictly
              default-expand-all
              :render-after-expand="false"
              placeholder="请选择业务模块"
              class="f02-control"
              node-key="moduleCode"
              :props="{ value: 'moduleCode', label: 'queryLabel', children: 'children' }"
              @update:model-value="handleArchiveTypeChange"
            />
          </div>
          <div class="f02-field">
            <label>开始档期</label>
            <el-date-picker
              v-model="periodRange"
              type="monthrange"
              value-format="YYYY-MM"
              start-placeholder="开始"
              end-placeholder="结束"
              range-separator="~"
              class="f02-control f02-date-range"
            />
          </div>
          <div class="f02-field">
            <label>载体类型</label>
            <el-select v-model="filters.carrierType" clearable placeholder="请选择" class="f02-control">
              <el-option v-for="item in options.carrierTypes" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </div>
          <div class="f02-field">
            <label>文档生成时间</label>
            <el-date-picker
              v-model="filters.docGenerationRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始"
              end-placeholder="结束"
              range-separator="~"
              class="f02-control f02-date-range"
            />
          </div>
          <div class="f02-field">
            <label>文档业务编码</label>
            <F03MultiLineFilterInput
              v-model="filters.businessCode"
              drawer-title="文档业务编码（多行）"
              placeholder="单行模糊；多行请点右侧图标，每行精确（忽略大小写）"
              drawer-hint="主框仅一行时：模糊匹配。在抽屉中每行一条时：精确匹配（忽略大小写），多行之间为「或」，与其它筛选条件为「且」。最多100 行。"
              class="f02-control"
            />
          </div>
          <div class="f02-field">
            <label>文档组织</label>
            <el-select v-model="filters.docOrganization" clearable filterable placeholder="请选择" class="f02-control">
              <el-option v-for="item in options.documentOrganizations" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </div>
        </div>

        <div v-show="moreFilters" class="f02-filter-grid f02-filter-grid--more">
          <div v-for="field in visibleMoreFilterFields" :key="field.key" class="f02-field">
            <label>{{ field.label }}</label>
            <el-select
              v-if="field.type === 'select'"
              v-model="(filters as any)[field.key]"
              clearable
              placeholder="请选择"
              class="f02-control"
              :multiple="field.multiple ?? false"
            >
              <el-option v-for="opt in (field.options || moreFieldOptionsMap[field.key] || [])" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-date-picker
              v-else-if="field.type === 'daterange'"
              v-model="(filters as any)[field.key]"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始"
              end-placeholder="结束"
              range-separator="~"
              class="f02-control f02-date-range"
            />
            <F03MultiLineFilterInput
              v-else-if="field.type === 'input' && field.multilineDrawer"
              v-model="(filters as any)[field.key]"
              :placeholder="field.placeholder || '请输入'"
              :drawer-title="`编辑：${field.label}`"
              drawer-hint="每行一条，空行忽略；与其它筛选组合为「且」，本字段多行为「或」。最多 100 行。"
              class="f02-control"
            />
            <el-input
              v-else
              v-model="(filters as any)[field.key]"
              clearable
              :placeholder="field.placeholder || '请输入'"
              class="f02-control"
            />
          </div>
          <div v-for="field in moduleExtFilterFields" :key="`ext-${field.fieldCode}`" class="f02-field">
            <label>{{ field.fieldName }}</label>
            <el-input
              v-model="extFilterValues[field.fieldCode]"
              clearable
              placeholder="请输入"
              class="f02-control"
            />
          </div>
        </div>

        <div class="f02-filter-actions">
          <el-button link type="primary" :disabled="!docTypeReady" @click="moreFilters = !moreFilters">
            {{ moreFilters ? '收起筛选条件' : '更多筛选条件' }}
            <el-icon class="el-icon--right"><ArrowDown v-if="!moreFilters" /><ArrowUp v-else /></el-icon>
          </el-button>
        </div>
        <div class="f02-query-buttons fdc-query-action-buttons">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="runQuery" :disabled="!docTypeReady">查询</el-button>
        </div>
      </section>

      <section class="f02-toolbar">
        <div class="f02-toolbar__left">
          <el-button type="primary" @click="goCreate" :disabled="!docTypeReady">
            <el-icon class="el-icon--left"><Plus /></el-icon>
            应归档数据创建
          </el-button>
          <el-button @click="openBatchDialog('CREATE')" :disabled="!docTypeReady">
            <el-icon class="el-icon--left"><Upload /></el-icon>
            批量创建
          </el-button>
          <el-button @click="openBatchDialog('UPDATE')" :disabled="!docTypeReady">
            <el-icon class="el-icon--left"><RefreshRight /></el-icon>
            批量更新
          </el-button>
          <el-button @click="exportCsv" :disabled="!docTypeReady">批量导出</el-button>
          <el-button type="primary" @click="openBatchDialog('IMPORT_QUERY')" :disabled="!docTypeReady">
            <el-icon class="el-icon--left"><Search /></el-icon>
            批量导入查询
          </el-button>
        </div>
        <div class="f02-toolbar__right">
          <el-tooltip content="列设置" placement="top">
            <el-button circle :icon="Setting" @click="handleColumnSettingClick" />
          </el-tooltip>
          <el-tooltip :content="tableFullPage ? '退出全页面展示' : '列表栏信息全页面展示'" placement="top">
            <el-button circle :icon="FullScreen" @click="tableFullPage = !tableFullPage" />
          </el-tooltip>
          <el-tooltip content="刷新数据" placement="top">
            <el-button circle :icon="RefreshRight" @click="runQuery" :disabled="!docTypeReady" />
          </el-tooltip>
        </div>
      </section>

      <section class="f02-card f02-table-wrap">
        <div class="table-section">
        <el-table :data="rows" border stripe class="f02-table" empty-text="暂无数据，请先查询" @selection-change="onSelectionChange">
          <el-table-column type="selection" width="48" />
          <el-table-column label="操作" width="120" fixed="left">
            <template #default="{ row }">
              <div class="f02-row-actions">
                <el-button link type="primary" :icon="Edit" title="编辑" @click="goEdit(row.docId)" />
                <el-button link type="danger" :icon="Delete" title="删除" @click="confirmDelete(row)" />
              </div>
            </template>
          </el-table-column>
          <template v-for="col in displayedDataColumns" :key="col.prop">
            <el-table-column v-if="col.prop === 'businessCode'" :label="col.label" :min-width="col.minWidth">
              <template #default="{ row }">
                <el-link type="primary" @click="goDetail(row)">{{ row.businessCode }}</el-link>
              </template>
            </el-table-column>
            <el-table-column
              v-else
              :prop="col.prop"
              :label="col.label"
              :min-width="col.minWidth"
              :width="col.width"
              :show-overflow-tooltip="col.showOverflow ?? false"
            >
              <template v-if="isDateTimeColumn(col.prop)" #default="{ row }">
                {{ formatDateTime((row as any)[col.prop]) }}
              </template>
            </el-table-column>
          </template>
        </el-table>
        </div>
        <div class="f02-pagination">
          <span class="f02-pagination__info">共 {{ rows.length }} 条</span>
          <el-pagination layout="prev, pager, next, sizes" :total="rows.length" :page-size="20" disabled />
        </div>
      </section>
    </div>

    <F03BatchImportModal
      v-model="batchDialog.open"
      :title="batchDialogTitle"
      :header-icon="batchDialog.mode === 'UPDATE' ? 'sync' : 'upload'"
      :hint="batchDialogHint"
      :template-csv="batchDialogSample"
      :download-file-name="batchDownloadFileName"
      :loading="batchDialog.loading"
      :enable-operation-inputs="batchDialog.mode === 'CREATE' || batchDialog.mode === 'UPDATE'"
      :show-go-my-import="batchDialog.mode === 'CREATE'"
      :upload-pending-audit-attachment="uploadPendingAuditAttachment"
      @confirm="handleBatchModalConfirm"
    />

    <el-dialog
      v-model="columnSettingVisible"
      width="760px"
      class="f02-column-setting-dialog"
      :show-close="false"
      align-center
      append-to-body
    >
      <template #header>
        <div class="f02-column-setting-dialog__header">
          <h3>字段选择</h3>
          <el-button link @click="columnSettingVisible = false">✕</el-button>
        </div>
      </template>
      <div class="f02-column-setting-dialog__body">
        <el-input
          v-model="columnSearchKeyword"
          :prefix-icon="Search"
          placeholder="搜索字段名称"
          clearable
          class="f02-column-setting-dialog__search"
        />
        <el-checkbox-group v-model="columnDraftKeys" class="f02-column-setting-dialog__grid">
          <el-checkbox
            v-for="col in filteredColumnOptions"
            :key="col.prop"
            :label="col.prop"
            :disabled="col.prop === 'businessCode'"
          >
            {{ col.label }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <div class="f02-column-setting-dialog__footer">
          <el-button @click="resetColumnDraftSelection">重置</el-button>
          <el-button type="primary" @click="confirmColumnDraftSelection">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="exportSuccessVisible" title="提示" width="460px">
      <div class="export-success-tip">
        导出提交成功，系统正在处理，稍后请到
        <el-link type="primary" @click="goMyExports">我的导出</el-link>
        中查看导出结果。
      </div>
      <template #footer>
        <el-button @click="exportSuccessVisible = false">关闭</el-button>
        <el-button type="primary" @click="goMyExports">前往我的导出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, ArrowUp, Delete, Edit, FullScreen, Plus, RefreshRight, Search, Setting, Upload, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  createPendingDocumentsExportJob,
  fetchArchiveCreateOptions,
  getArchiveDetail,
  queryPendingDocuments,
  submitPendingArchiveBatchImport,
  submitPendingImportQueryJob,
  updatePendingDocument,
  uploadPendingAuditAttachment,
  type PendingAuditAttachmentRef,
  type PendingDocumentQueryCommand,
  type PendingDocumentRowResponse
} from '../../api/modules/archiveManagement'
import {
  buildModuleQueryTree,
  fetchBusinessModuleExtFields,
  fetchBusinessModuleTree,
  filterBusinessModuleTreeByDocumentType,
  isModuleCodeWithinDocumentType,
  resolveRootDocumentTypeCodeByModule,
  type ModuleQueryTreeNode
} from '../../api/modules/businessModule'
import { fetchCompanyInfos } from '../../api/modules/companyInfo'
import { fetchUsers } from '../../api/modules/security'
import { parseMultiValueLines, validateMultiValueInput } from '../../utils/multiValueQuery'
import type { ArchiveCreateOptions, ArchiveRecordSummary, BusinessModuleExtField, BusinessModuleNode } from '../../types'
import type { User } from '../../api/modules/security'
import { useLayoutStore } from '../../stores/useLayoutStore'
import F03BatchImportModal from '../../components/f03/F03BatchImportModal.vue'
import F03MultiLineFilterInput from '../../components/f03/F03MultiLineFilterInput.vue'
import { getVisibleMoreFilterFields, pendingArchiveQueryPageConfig } from './queryPageConfig'
import { EXT_DETAIL_FIELD_ORDER, hardCodedExtLabelMap, isHardCodedFieldVisible } from './extFieldDisplayConfig'
import {
  buildCoreOnlyPendingBatchTemplateCsv,
  buildPendingArchiveBatchImportTemplateCsv
} from './pendingArchiveBatchImportTemplate'

type DemoRow = PendingDocumentRowResponse

const router = useRouter()
const layout = useLayoutStore()

const dataColumnOptions = pendingArchiveQueryPageConfig.columns as Array<{ prop: keyof DemoRow; label: string; minWidth?: number; width?: number | string; showOverflow?: boolean }>
const visibleDataColumnProps = ref<string[]>([...pendingArchiveQueryPageConfig.defaultVisibleColumns])
const tableFullPage = ref(false)
const columnSettingVisible = ref(false)
const columnSearchKeyword = ref('')
const columnDraftKeys = ref<string[]>([...pendingArchiveQueryPageConfig.defaultVisibleColumns])

watch(
  visibleDataColumnProps,
  (next) => {
    if (!next.includes('businessCode')) {
      visibleDataColumnProps.value = [...next, 'businessCode']
    }
  },
  { deep: true }
)

const displayedDataColumns = computed(() => dataColumnOptions.filter((c) => visibleDataColumnProps.value.includes(c.prop)))
const filteredColumnOptions = computed(() => {
  const keyword = columnSearchKeyword.value.trim().toLowerCase()
  if (!keyword) return dataColumnOptions
  return dataColumnOptions.filter((col) =>
    String(col.label || '').toLowerCase().includes(keyword) ||
    String(col.prop || '').toLowerCase().includes(keyword)
  )
})

const formatDateTime = (value: unknown) => {
  if (value === null || value === undefined || value === '') return '-'
  const text = String(value).trim()
  if (!text) return '-'
  const normalized = text.includes('T') ? text : text.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return text
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const isDateTimeColumn = (prop: string) => ['docGenerationDate', 'creationTime', 'updatedAt'].includes(prop)

const options = reactive<ArchiveCreateOptions>({
  companyProjects: [],
  documentTypes: [],
  archiveDestinations: [],
  documentOrganizations: [],
  securityLevels: [],
  carrierTypes: [],
  attachmentTypes: [],
  archiveTypes: [],
  aiModels: [],
  geoCountries: [],
  geoRepOffices: [],
  geoRegions: [],
  custodyStatuses: []
})

const resolvePendingSecurityFields = (raw: string) => {
  const t = String(raw ?? '').trim()
  if (!t) return { securityLevelCode: '', securityLevelName: '', securityLevel: '' }
  const o = options.securityLevels.find((x) => x.code === t || x.name === t)
  if (o) return { securityLevelCode: o.code, securityLevelName: o.name, securityLevel: o.name }
  const upper = t.toUpperCase()
  const byCode = options.securityLevels.find((x) => x.code === upper)
  if (byCode) return { securityLevelCode: byCode.code, securityLevelName: byCode.name, securityLevel: byCode.name }
  return { securityLevelCode: t, securityLevelName: t, securityLevel: t }
}

const companySelectOptions = ref<Array<{ code: string; name: string }>>([])
const userSelectOptions = ref<Array<{ label: string; value: string }>>([])
const userDisplayById = computed<Record<string, string>>(() =>
  Object.fromEntries(userSelectOptions.value.map((item) => [item.value, item.label]))
)
const businessModuleSourceTree = ref<BusinessModuleNode[]>([])
const businessModuleTreeOptions = ref<ModuleQueryTreeNode[]>([])
const moduleExtFilterFields = ref<BusinessModuleExtField[]>([])
const extFilterValues = reactive<Record<string, string>>({})

const filters = reactive({
  documentTypeCode: '',
  /** 对应数据模型公司编码（API 字段仍为 companyProjectCode） */
  companyCode: '',
  archiveTypeCode: '',
  carrierType: '',
  docGenerationRange: null as [string, string] | null,
  businessCode: '',
  docOrganization: '',
  documentName: '',
  status: '',
  country: '',
  repOffice: '',
  region: '',
  custodyStatus: '',
  securityLevel: '',
  description: '',
  archPlace: '',
  originatingPlace: '',
  dutyPerson: '',
  respArchDept: '',
  createdBy: '',
  creationDateRange: null as [string, string] | null,
  sourceSystemFilter: [] as string[],
  archivedEntityName: '',
  barcodeModule: '',
  archiveBarcodeRange: '',
  verificationDateRange: null as [string, string] | null,
  verifiedBy: [] as string[],
  volumeSeqNo: '',
  volumeBarcodeRange: '',
  volumizationDateRange: null as [string, string] | null,
  assembledBy: [] as string[],
  volumeNoRange: '',
  repository: '',
  storageLocationRange: '',
  storageDateRange: null as [string, string] | null,
  storedBy: [] as string[],
  copies: '',
  remainingCopies: '',
  archiveType: '',
  visibilityFilter: [] as string[],
  invoiceNo: '',
  refNo: '',
  accountant: [] as string[],
  scannedBy: [] as string[],
  issueDateRange: null as [string, string] | null,
  maturityDateRange: null as [string, string] | null,
  lgExpiryDateRange: null as [string, string] | null,
  lgLedgerStatus: [] as string[],
  bankName: '',
  currency: [] as string[],
  amount: '',
  issuingAuthority: '',
  disposalTimeRange: null as [string, string] | null,
  businessVolumeNo: '',
  lgWorkflowNo: '',
  lgNo: ''
})
const periodRange = ref<[string, string] | null>(null)
const moreFilters = ref(false)
const docTypeReady = computed(() => Boolean(filters.documentTypeCode && filters.documentTypeCode.trim()))
const selectedDocTypeName = computed(() => {
  const item = options.documentTypes.find((d) => d.code === filters.documentTypeCode)
  return item?.name || ''
})

const LEGACY_FIXED_EXT_FILTER_KEYS = new Set([
  'invoiceNo',
  'refNo',
  'issueDateRange',
  'maturityDateRange',
  'lgExpiryDateRange',
  'lgLedgerStatus',
  'bankName',
  'currency',
  'amount',
  'issuingAuthority',
  'disposalTimeRange',
  'businessVolumeNo',
  'lgWorkflowNo',
  'lgNo'
])
const visibleMoreFilterFields = computed(() =>
  getVisibleMoreFilterFields(pendingArchiveQueryPageConfig.moreFilterFields, selectedDocTypeName.value)
    .filter((field) => !LEGACY_FIXED_EXT_FILTER_KEYS.has(field.key))
)
const moreFieldOptionsMap = computed<Record<string, Array<{ label: string; value: string }>>>(() => ({
  country: options.geoCountries.map((item) => ({ label: item.name, value: item.code })),
  repOffice: options.geoRepOffices.map((item) => ({ label: item.name, value: item.name })),
  region: options.geoRegions.map((item) => ({ label: item.name, value: item.name })),
  custodyStatus: options.custodyStatuses
    .filter((item) => item.code === 'UNARCHIVED')
    .map((item) => ({ label: item.name, value: item.code })),
  verifiedBy: userSelectOptions.value,
  assembledBy: userSelectOptions.value,
  storedBy: userSelectOptions.value,
  accountant: userSelectOptions.value,
  scannedBy: userSelectOptions.value
}))

watch(
  () => filters.documentTypeCode,
  (next) => {
    layout.setDocumentTypeCode(next || '')
    syncBusinessModuleOptionsByDocumentType(next)
  }
)

const syncBusinessModuleOptionsByDocumentType = (documentTypeCode?: string) => {
  const filtered = filterBusinessModuleTreeByDocumentType(
    businessModuleSourceTree.value,
    documentTypeCode || ''
  )
  businessModuleTreeOptions.value = buildModuleQueryTree(filtered)
  if (
    filters.archiveTypeCode &&
    !isModuleCodeWithinDocumentType(
      businessModuleSourceTree.value,
      documentTypeCode || '',
      filters.archiveTypeCode
    )
  ) {
    filters.archiveTypeCode = ''
  }
}

const handleDocumentTypeChange = (next?: string) => {
  syncBusinessModuleOptionsByDocumentType(next)
  moduleExtFilterFields.value = []
  Object.keys(extFilterValues).forEach((key) => delete extFilterValues[key])
}

const handleArchiveTypeChange = (next?: string) => {
  const moduleCode = String(next || '').trim()
  if (!moduleCode) {
    moduleExtFilterFields.value = []
    Object.keys(extFilterValues).forEach((key) => delete extFilterValues[key])
    return
  }
  if (filters.documentTypeCode?.trim()) {
    if (
      !isModuleCodeWithinDocumentType(
        businessModuleSourceTree.value,
        filters.documentTypeCode,
        moduleCode
      )
    ) {
      const rootDocType = resolveRootDocumentTypeCodeByModule(businessModuleSourceTree.value, moduleCode)
      if (rootDocType) {
        filters.documentTypeCode = rootDocType
        syncBusinessModuleOptionsByDocumentType(rootDocType)
      } else {
        filters.archiveTypeCode = ''
      }
    }
    loadModuleExtFilterFields(moduleCode)
    return
  }
  const rootDocType = resolveRootDocumentTypeCodeByModule(businessModuleSourceTree.value, moduleCode)
  if (!rootDocType) return
  filters.documentTypeCode = rootDocType
  syncBusinessModuleOptionsByDocumentType(rootDocType)
  loadModuleExtFilterFields(moduleCode)
}

const loadModuleExtFilterFields = async (moduleCode: string) => {
  const code = String(moduleCode || '').trim()
  if (!code) {
    moduleExtFilterFields.value = []
    return
  }
  try {
    const [basic, attachment] = await Promise.all([
      fetchBusinessModuleExtFields(code, 'BASIC'),
      fetchBusinessModuleExtFields(code, 'ATTACHMENT')
    ])
    const merged = [...basic, ...attachment]
      .filter((item) => item.enabledFlag === 'Y' && item.queryFlag === 'Y')
      .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
    const unique = new Map<string, BusinessModuleExtField>()
    merged.forEach((item) => unique.set(item.fieldCode, item))
    moduleExtFilterFields.value = Array.from(unique.values())
  } catch (e: any) {
    moduleExtFilterFields.value = []
    ElMessage.error(e?.message || '加载业务模块扩展筛选字段失败')
  }
}

const rows = ref<DemoRow[]>([])
const selectedRows = ref<DemoRow[]>([])
const exportSuccessVisible = ref(false)

const loadOptions = async () => {
  const [data, companies, moduleTree, users] = await Promise.all([
    fetchArchiveCreateOptions(),
    fetchCompanyInfos({ enabledFlag: 'Y' }),
    fetchBusinessModuleTree().catch((): BusinessModuleNode[] => []),
    fetchUsers().catch((): User[] => [])
  ])
  Object.assign(options, data)
  companySelectOptions.value = companies.map((c) => ({ code: c.companyCode, name: c.companyName }))
  userSelectOptions.value = users
    .map((u) => {
      const userId = Number(u.userId)
      if (!Number.isFinite(userId) || userId <= 0) return null
      const userName = String(u.userName || u.username || `用户-${userId}`).trim()
      const display = u.employeeNo ? `${userName} ${u.employeeNo}` : userName
      return { label: display, value: String(userId) }
    })
    .filter((item): item is { label: string; value: string } => Boolean(item))
  businessModuleSourceTree.value = moduleTree
  syncBusinessModuleOptionsByDocumentType(filters.documentTypeCode)
}

const applyLocalExtFilters = (data: DemoRow[]) => {
  const activeEntries = Object.entries(extFilterValues)
    .map(([key, value]) => [key, String(value || '').trim()] as const)
    .filter(([, value]) => value.length > 0)
  if (!activeEntries.length) return data
  return data.filter((row) => {
    const anyRow = row as any
    const rowExtValues = (anyRow.extValues || {}) as Record<string, unknown>
    return activeEntries.every(([fieldCode, keyword]) => {
      const candidate = String(
        anyRow[fieldCode] ??
        rowExtValues[fieldCode] ??
        ''
      ).toLowerCase()
      return candidate.includes(keyword.toLowerCase())
    })
  })
}

const runQuery = async () => {
  if (!docTypeReady.value) return
  const multiErr = validateMultiValueInput({
    文档业务编码: filters.businessCode,
    发票号: filters.invoiceNo,
    其他相关编号: filters.refNo
  })
  if (multiErr) {
    ElMessage.warning(multiErr)
    return
  }
  try {
    const bizParsed = parseMultiValueLines(filters.businessCode || '')
    const refParsed = parseMultiValueLines(filters.refNo || '')
    const command: PendingDocumentQueryCommand = {
      documentTypeCode: filters.documentTypeCode || undefined,
      companyCode: filters.companyCode || undefined,
      archiveTypeCode: filters.archiveTypeCode || undefined,
      carrierType: filters.carrierType || undefined,
      businessCode: bizParsed.length === 1 ? bizParsed[0] : undefined,
      businessCodes: bizParsed.length > 1 ? bizParsed : undefined,
      invoiceNo: filters.invoiceNo || undefined,
      // 任意条数都用 refNos 数组，避免「仅 length>1 才传数组」时若解析成 1 段又退回 refNo 字符串、传输丢换行导致不加 ref 条件
      refNos: refParsed.length > 0 ? refParsed : undefined,
      docOrganization: filters.docOrganization || undefined,
      beginPeriod: periodRange.value?.[0] || undefined,
      endPeriod: periodRange.value?.[1] || undefined,
      docGenerationStart: filters.docGenerationRange?.[0] || undefined,
      docGenerationEnd: filters.docGenerationRange?.[1] || undefined,
      custodyStatus: filters.custodyStatus || undefined,
      country: filters.country || undefined,
      repOffice: filters.repOffice || undefined,
      region: filters.region || undefined,
      dutyPerson: filters.dutyPerson || undefined
    }
    rows.value = applyLocalExtFilters(await queryPendingDocuments(command))
    console.log('[PendingArchiveQuery] query command:', command, 'rows:', rows.value.length)
    if (rows.value.length === 0) {
      ElMessage.info('未查询到匹配数据')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '查询失败，请检查后端服务与代理端口')
    rows.value = []
  }
}

const resetFilters = () => {
  filters.documentTypeCode = ''
  filters.companyCode = ''
  filters.archiveTypeCode = ''
  filters.carrierType = ''
  filters.docGenerationRange = null
  filters.businessCode = ''
  filters.docOrganization = ''
  filters.documentName = ''
  filters.status = ''
  filters.country = ''
  filters.repOffice = ''
  filters.region = ''
  filters.custodyStatus = ''
  filters.securityLevel = ''
  filters.description = ''
  filters.archPlace = ''
  filters.originatingPlace = ''
  filters.dutyPerson = ''
  filters.respArchDept = ''
  filters.createdBy = ''
  filters.creationDateRange = null
  filters.sourceSystemFilter = []
  filters.archivedEntityName = ''
  filters.barcodeModule = ''
  filters.archiveBarcodeRange = ''
  filters.verificationDateRange = null
  filters.verifiedBy = []
  filters.volumeSeqNo = ''
  filters.volumeBarcodeRange = ''
  filters.volumizationDateRange = null
  filters.assembledBy = []
  filters.volumeNoRange = ''
  filters.repository = ''
  filters.storageLocationRange = ''
  filters.storageDateRange = null
  filters.storedBy = []
  filters.copies = ''
  filters.remainingCopies = ''
  filters.archiveType = ''
  filters.visibilityFilter = []
  filters.invoiceNo = ''
  filters.refNo = ''
  filters.accountant = []
  filters.scannedBy = []
  filters.issueDateRange = null
  filters.maturityDateRange = null
  filters.lgExpiryDateRange = null
  filters.lgLedgerStatus = []
  filters.bankName = ''
  filters.currency = []
  filters.amount = ''
  filters.issuingAuthority = ''
  filters.disposalTimeRange = null
  filters.businessVolumeNo = ''
  filters.lgWorkflowNo = ''
  filters.lgNo = ''
  syncBusinessModuleOptionsByDocumentType('')
  moduleExtFilterFields.value = []
  Object.keys(extFilterValues).forEach((key) => delete extFilterValues[key])
  periodRange.value = null
}

const goCreate = () => {
  if (!docTypeReady.value) return
  router.push({ path: '/archive-management/pending-archive/create', query: { documentTypeCode: filters.documentTypeCode } })
}

const goEdit = (docId: string) => {
  router.push({
    path: `/archive-management/pending-archive/edit/${encodeURIComponent(docId)}`,
    query: { from: 'query' }
  })
}

const goDetail = (row: DemoRow) => {
  const resolved = router.resolve({
    path: `/archive-management/detail/${encodeURIComponent(row.docId)}`,
    query: {
      from: 'pending',
      docId: row.docId,
      businessCode: row.businessCode,
      documentName: row.documentName
    }
  })
  window.open(resolved.href, '_blank', 'noopener,noreferrer')
}

const confirmDelete = (row: DemoRow) => {
  ElMessageBox.confirm(`确定删除文档「${row.documentName}」（${row.docId}）吗？`, '删除确认', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      rows.value = rows.value.filter((r) => r.docId !== row.docId)
      ElMessage.success('已删除（演示）')
    })
    .catch(() => {})
}

const onSelectionChange = (next: DemoRow[]) => {
  selectedRows.value = next
}

type BatchMode = 'CREATE' | 'UPDATE' | 'IMPORT_QUERY'

const batchDialog = reactive<{
  open: boolean
  mode: BatchMode
  file: File | null
  loading: boolean
}>({
  open: false,
  mode: 'CREATE',
  file: null,
  loading: false
})

const batchDialogTitle = computed(() => {
  if (batchDialog.mode === 'CREATE') return '批量创建'
  if (batchDialog.mode === 'UPDATE') return '批量更新'
  return '批量导入查询'
})

const batchDialogHint = computed(() => {
  if (batchDialog.mode === 'CREATE') {
    return '请下载模板文件，按列填写后上传 CSV。提交后系统异步导入，请在「我的工作空间 → 我的导入」查看进度并下载结果 Excel。'
  }
  if (batchDialog.mode === 'UPDATE') {
    return '请下载模板文件，按列填写后上传 CSV。将按 docId 更新匹配行（演示：仅作用于当前列表）。'
  }
  return '请下载模板文件，按6列填写后上传。每行至少填写「文档业务编码/发票号/其他相关编号」之一；行内按且（AND），逐行结果合并。'
})

const batchDownloadFileName = computed(() => {
  const d = new Date().toISOString().slice(0, 10)
  if (batchDialog.mode === 'CREATE') {
    const slug = (filters.documentTypeCode || 'doc').replace(/[^\w-]+/g, '_')
    return `pending-archive-batch-create-${slug}-${d}.csv`
  }
  if (batchDialog.mode === 'UPDATE') return `pending-archive-batch-update-${d}.csv`
  return `pending-archive-import-query-${d}.csv`
})

const batchCreateTemplateCsv = ref('')

const batchDialogSample = computed(() => {
  if (batchDialog.mode === 'CREATE') {
    return batchCreateTemplateCsv.value
  }
  if (batchDialog.mode === 'UPDATE') {
    return ['docId,documentName,docStatus,owner,creationTime,createdBy', 'DOC-20231024-001,会计凭证 0002AP00001,未归档,系统,2023-10-25 09:00,系统'].join('\n')
  }
  return [
    '文档业务编码,发票号,其他相关编号,公司,业务模块,开始档期',
    'FUND-DEMO-2026-001,,,CP-DEMO-001,FIN_FUND_PAYMENT_PAY,2026-04',
    ',INV-DEMO-0002,,CP-DEMO-001,FIN_FUND_PAYMENT_PAY,2026-04',
    ',,REF-2026-003,CP-DEMO-001,FIN_FUND_PAYMENT_PAY,2026-04'
  ].join('\n')
})

watch(
  () =>
    [
      batchDialog.open,
      batchDialog.mode,
      filters.documentTypeCode,
      filters.companyCode,
      filters.archiveTypeCode,
      selectedDocTypeName.value
    ] as const,
  async ([open, mode, docType, companyCode, archiveTypeCode, typeName]) => {
    if (!open || mode !== 'CREATE' || !docType?.trim()) {
      return
    }
    try {
      batchCreateTemplateCsv.value = await buildPendingArchiveBatchImportTemplateCsv({
        documentTypeCode: docType,
        documentTypeName: typeName || '',
        companyProjectCode: companyCode || undefined,
        archiveTypeCode: archiveTypeCode || undefined
      })
    } catch {
      batchCreateTemplateCsv.value = buildCoreOnlyPendingBatchTemplateCsv({
        documentTypeCode: docType,
        documentTypeName: typeName || '',
        companyProjectCode: companyCode || undefined,
        archiveTypeCode: archiveTypeCode || undefined
      })
    }
  }
)

const openBatchDialog = (mode: BatchMode) => {
  if (!docTypeReady.value) return
  batchDialog.mode = mode
  batchDialog.open = true
  batchDialog.file = null
  batchDialog.loading = false
  if (mode === 'CREATE' && filters.documentTypeCode?.trim()) {
    batchCreateTemplateCsv.value = buildCoreOnlyPendingBatchTemplateCsv({
      documentTypeCode: filters.documentTypeCode,
      documentTypeName: selectedDocTypeName.value || '',
      companyProjectCode: filters.companyCode || undefined,
      archiveTypeCode: filters.archiveTypeCode || undefined
    })
  }
}

const handleBatchModalConfirm = async (payload: { file: File | null; operationRemark?: string; auditAttachments?: PendingAuditAttachmentRef[] }) => {
  if (!payload.file) return
  batchDialog.file = payload.file
  await confirmBatchDialog(payload.operationRemark, payload.auditAttachments)
}

const readTextFile = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result ?? ''))
    reader.onerror = () => reject(reader.error)
    reader.readAsText(file)
  })

const parseCsv = (csv: string) => {
  const lines = csv
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => l.length > 0)
  if (lines.length === 0) return { headers: [] as string[], rows: [] as string[][] }
  const splitLine = (line: string) => line.split(',').map((c) => c.trim())
  const headers = splitLine(lines[0]).map((h) => h.replace(/^"|"$/g, ''))
  const body = lines.slice(1).map((l) => splitLine(l).map((c) => c.replace(/^"|"$/g, '')))
  return { headers, rows: body }
}

const currentDateTime = () => {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const currentYearMonth = () => {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
}

const docGenerationToDateTime = (v: string | undefined) => {
  const t = String(v || '').trim()
  if (!t) return currentDateTime()
  if (/^\d{4}-\d{2}-\d{2}$/.test(t)) return `${t} 00:00:00`
  if (/^\d{4}-\d{2}$/.test(t)) return `${t}-01 00:00:00`
  if (/^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}$/.test(t)) return t
  return currentDateTime()
}

const idxBy = (headers: string[], name: string) => headers.findIndex((h) => h === name)
const getBy = (headers: string[], row: string[], name: string) => {
  const i = idxBy(headers, name)
  return i >= 0 ? row[i] : ''
}

const applyBatchUpdate = async (headers: string[], dataRows: string[][], operationRemark?: string, auditAttachments?: PendingAuditAttachmentRef[]) => {
  let updated = 0
  let failed = 0
  for (const row of dataRows) {
    const docId = Number(getBy(headers, row, 'docId').trim())
    if (!Number.isFinite(docId) || docId <= 0) {
      failed += 1
      continue
    }
    try {
      const d = await getArchiveDetail(docId)
      const sec = resolvePendingSecurityFields(getBy(headers, row, 'securityLevel').trim() || d.securityLevelCode || '')
      const startPeriod = getBy(headers, row, 'startPeriod').trim() || d.beginPeriod || currentYearMonth()
      const endPeriod = getBy(headers, row, 'endPeriod').trim() || d.endPeriod || startPeriod
      const archiveDestination = getBy(headers, row, 'archivePlace').trim() || d.archiveDestination || 'SHANGHAI'
      const originPlace = getBy(headers, row, 'originPlace').trim() || d.originPlace || archiveDestination
      const documentName = getBy(headers, row, 'documentName').trim() || d.documentName
      const dutyPerson = getBy(headers, row, 'owner').trim() || d.dutyPerson
      const dutyDepartment = getBy(headers, row, 'responsibleDept').trim() || d.dutyDepartment || undefined
      const docOrganization = getBy(headers, row, 'docOrganization').trim() || d.documentOrganizationCode
      const carrierTypeCode = getBy(headers, row, 'carrierType').trim() || d.carrierTypeCode || 'ELECTRONIC'
      const sourceSystem = getBy(headers, row, 'sourceSystem').trim() || d.sourceSystem || 'PORTAL'
      const remark = getBy(headers, row, 'description').trim() || d.remark || undefined
      const extValues = { ...(d.extValues || {}) }
      const visibility = getBy(headers, row, 'visibility').trim()
      if (visibility) extValues.visibility = visibility
      await updatePendingDocument(docId, {
        operatorUserId: 1,
        documentTypeCode: d.documentTypeCode || filters.documentTypeCode,
        companyProjectCode: d.companyProjectCode || filters.companyCode || '',
        archiveTypeCode: d.businessModuleTypeCode || filters.archiveTypeCode || '',
        businessCode: getBy(headers, row, 'businessCode').trim() || d.businessCode || undefined,
        beginPeriod: startPeriod,
        endPeriod,
        archiveDestination,
        originPlace,
        documentName,
        documentDate: docGenerationToDateTime(getBy(headers, row, 'docGenerationDate') || String(d.documentDate || '')),
        dutyPerson,
        dutyDepartment,
        carrierTypeCode,
        sourceSystem,
        securityLevelCode: sec.securityLevelCode || d.securityLevelCode || 'INTERNAL',
        remark,
        documentOrganizationCode: docOrganization,
        retentionPeriodYears: d.retentionPeriodYears,
        submitMode: 'SUBMIT',
        operationRemark,
        operationTypeCode: 'BATCH_UPDATE',
        auditAttachments,
        extValues
      })
      updated += 1
    } catch {
      failed += 1
    }
  }
  await runQuery()
  if (updated > 0 && failed > 0) ElMessage.warning(`已更新 ${updated} 条，失败 ${failed} 条`)
  else if (updated > 0) ElMessage.success(`已更新 ${updated} 条`)
  else ElMessage.warning('未更新成功，请检查模板数据')
}

const applyImportQuery = async (file: File) => {
  if (!filters.documentTypeCode?.trim()) {
    ElMessage.warning('请先选择文档类型')
    return
  }
  await submitPendingImportQueryJob({
    file,
    documentTypeCode: filters.documentTypeCode
  })
  ElMessage.success('已提交批量导入查询，请前往「我的工作空间 → 我的导入」查看结果查询')
}

const confirmBatchDialog = async (operationRemark?: string, auditAttachments?: PendingAuditAttachmentRef[]) => {
  if (!batchDialog.file) {
    ElMessage.warning('请先选择 CSV 文件')
    return
  }
  batchDialog.loading = true
  try {
    if (batchDialog.mode === 'CREATE') {
      await submitPendingArchiveBatchImport({
        file: batchDialog.file,
        documentTypeCode: filters.documentTypeCode!,
        operationRemark,
        auditAttachments
      })
      ElMessage.success('已提交应归档批量导入，请前往「我的工作空间 → 我的导入」查看进度并下载结果')
      batchDialog.open = false
      return
    }
    const text = await readTextFile(batchDialog.file)
    const { headers, rows: dataRows } = parseCsv(text)
    if (headers.length === 0) {
      ElMessage.warning('CSV 文件为空或格式不正确')
      return
    }
    if (batchDialog.mode === 'UPDATE') await applyBatchUpdate(headers, dataRows, operationRemark, auditAttachments)
    else await applyImportQuery(batchDialog.file)
    batchDialog.open = false
  } catch (e: any) {
    ElMessage.error(e?.message || '处理失败')
  } finally {
    batchDialog.loading = false
  }
}

const toCsv = (data: DemoRow[]) => {
  const cols = displayedDataColumns.value
  const headers = cols.map((c) => c.label)
  const props = cols.map((c) => c.prop)
  const escapeCell = (v: any) => {
    const s = String(v ?? '')
    const needsQuote = /[",\n\r]/.test(s)
    const escaped = s.replaceAll('"', '""')
    return needsQuote ? `"${escaped}"` : escaped
  }
  const lines = [headers.join(',')]
  for (const row of data) {
    lines.push(props.map((p) => escapeCell((row as any)[p])).join(','))
  }
  return lines.join('\n')
}

const isPureElectronicCarrierForExport = (carrier: string | undefined) => {
  if (carrier == null) return false
  const s = String(carrier).trim()
  if (!s) return false
  const u = s.toUpperCase()
  if (u === 'HYBRID' || u === 'PAPER') return false
  if (s.includes('纸质') || s.includes('+')) return false
  if (s === '纸质件') return false
  if (u === 'ELECTRONIC') return true
  if (s === '电子件' || s === '纯电子件') return true
  return false
}

type DetailExportColumn = { label: string; prop: string }
const buildPendingDetailExportSchema = (records: ArchiveRecordSummary[]): DetailExportColumn[] => {
  const base: DetailExportColumn[] = [
    { label: '文档类型', prop: 'documentTypeName' },
    { label: '文档业务编码', prop: 'businessCode' },
    { label: '公司', prop: 'companyProjectName' },
    { label: '业务模块', prop: 'archiveTypeCode' },
    { label: '开始档期', prop: 'beginPeriod' },
    { label: '结束档期', prop: 'endPeriod' },
    { label: '归档地', prop: 'archiveDestination' },
    { label: '产生地', prop: 'originPlace' },
    { label: '文档名称', prop: 'documentName' },
    { label: '文档生成日期', prop: 'documentDate' },
    { label: '归档责任人', prop: 'dutyPerson' },
    { label: '文档责任部门', prop: 'dutyDepartment' },
    { label: '载体类型', prop: 'carrierTypeCode' },
    { label: '系统来源', prop: 'sourceSystem' },
    { label: '密级', prop: 'securityLevelName' },
    { label: '创建时间', prop: 'lastUpdateDate' },
    { label: '创建人', prop: 'createdBy' },
    { label: '描述', prop: 'remark' }
  ]
  const docTypeName = String(records?.[0]?.documentTypeName || records?.[0]?.documentTypeCode || '')
  const ext: DetailExportColumn[] = EXT_DETAIL_FIELD_ORDER
    .filter((k) => isHardCodedFieldVisible(k, docTypeName))
    .map((k) => ({ label: hardCodedExtLabelMap[k] || k, prop: `ext.${k}` }))
  const archive: DetailExportColumn[] = [
    { label: '文档组织', prop: 'documentOrganizationCode' },
    { label: '档案类型', prop: 'archiveTypeCode' },
    { label: '是否可见', prop: 'documentVisibility' },
    { label: '条码模块', prop: 'ext.barcodeModule' },
    { label: '档案条码', prop: 'ext.archiveBarcodeRange' },
    { label: '文档编号', prop: 'ext.volumeSeqNo' },
    { label: '册号', prop: 'ext.volumeNoRange' },
    { label: '册条码', prop: 'ext.volumeBarcodeRange' },
    { label: '保管状态', prop: 'custodyStatus' },
    { label: '库房', prop: 'currentWarehouseCode' },
    { label: '库位', prop: 'currentLocationCode' },
    { label: '份数', prop: 'ext.copies' },
    { label: '剩余份数', prop: 'ext.remainingCopies' }
  ]
  return [...base, ...ext, ...archive]
}

const resolvePendingDetailExportValue = (row: ArchiveRecordSummary, prop: string) => {
  const ext = (row?.extValues || {}) as Record<string, string>
  if (prop.startsWith('ext.')) {
    const extKey = prop.slice(4)
    const raw = ext[extKey] ?? ''
    if (['accountant', 'scannedBy'].includes(extKey)) {
      const normalized = String(raw || '').trim()
      if (!normalized) return ''
      return (
        userDisplayById.value[normalized] ||
        (normalized.endsWith('.0') ? userDisplayById.value[normalized.slice(0, -2)] : undefined) ||
        raw
      )
    }
    return raw
  }
  if (prop === 'documentDate' || prop === 'lastUpdateDate') return formatDateTime((row as any)[prop])
  if (prop === 'securityLevelName') return row?.securityLevelName || row?.securityLevelCode || ''
  if (prop === 'companyProjectName') return row?.companyProjectName || row?.companyProjectCode || ''
  if (prop === 'documentTypeName') return row?.documentTypeName || row?.documentTypeCode || ''
  if (prop === 'documentVisibility') return row?.documentVisibility ?? ext.visibility ?? '是'
  if (prop === 'custodyStatus') return row?.custodyStatus || row?.archiveStatus || ''
  return (row as any)?.[prop] ?? ''
}

const toPendingDetailCsv = (records: ArchiveRecordSummary[]) => {
  const cols = buildPendingDetailExportSchema(records)
  const headers = cols.map((c) => c.label)
  const props = cols.map((c) => c.prop)
  const escapeCell = (v: any) => {
    const s = String(v ?? '')
    const needsQuote = /[",\n\r]/.test(s)
    const escaped = s.replaceAll('"', '""')
    return needsQuote ? `"${escaped}"` : escaped
  }
  const lines = [headers.join(',')]
  for (const row of records) {
    const isElectronic = isPureElectronicCarrierForExport(row?.carrierTypeCode)
    lines.push(props.map((p) => {
      if (
        isElectronic &&
        ['ext.archiveBarcodeRange', 'ext.volumeSeqNo', 'ext.volumeNoRange', 'ext.volumeBarcodeRange', 'currentWarehouseCode', 'currentLocationCode', 'ext.copies', 'ext.remainingCopies'].includes(p)
      ) return ''
      return escapeCell(resolvePendingDetailExportValue(row, p))
    }).join(','))
  }
  return lines.join('\n')
}

const downloadText = (filename: string, content: string, mime: string) => {
  const blob = new Blob([content], { type: mime })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

const exportCsv = async () => {
  if (!docTypeReady.value) return
  const data = selectedRows.value.length > 0 ? selectedRows.value : rows.value
  if (data.length === 0) {
    ElMessage.warning('暂无可导出的数据')
    return
  }
  const ids = data
    .map((r) => Number(r.docId))
    .filter((n) => Number.isFinite(n) && n > 0)
  if (!ids.length) {
    ElMessage.warning('未找到可导出的文档标识')
    return
  }
  await createPendingDocumentsExportJob({
    docIds: ids,
    exportFileFormat: 'CSV',
    exportScope: 'PENDING_ARCHIVE'
  })
  exportSuccessVisible.value = true
}

const handleColumnSettingClick = () => {
  columnSearchKeyword.value = ''
  columnDraftKeys.value = [...visibleDataColumnProps.value]
  columnSettingVisible.value = true
}
const resetColumnDraftSelection = () => {
  columnDraftKeys.value = [...pendingArchiveQueryPageConfig.defaultVisibleColumns]
}
const confirmColumnDraftSelection = () => {
  const next = [...columnDraftKeys.value]
  if (!next.includes('businessCode')) next.push('businessCode')
  visibleDataColumnProps.value = next
  columnSettingVisible.value = false
}

watch(
  () => layout.documentTypeCode,
  (next) => {
    if (next && next !== filters.documentTypeCode) {
      filters.documentTypeCode = next
    }
  },
  { immediate: true }
)

onMounted(() => {
  tableFullPage.value = false
  loadOptions()
})

onActivated(() => {
  tableFullPage.value = false
})

const goMyExports = () => {
  exportSuccessVisible.value = false
  router.push('/workspace/export-query')
}
</script>

<style scoped>
.f02-data-maintenance {
  --f02-primary: #1173d4;
  --f02-bg: #f6f7f8;
  --f02-border: #dbe0e6;
  --f02-text: #111418;
  --f02-text-sec: #617589;
  min-height: 100%;
  background: linear-gradient(135deg, #f6f7f8 0%, #e8f0f7 100%);
  font-family: 'Microsoft YaHei', 'Inter', 'Noto Sans SC', sans-serif;
  color: var(--f02-text);
  margin: -20px;
  padding: 24px;
}
.f02-inner {
  max-width: 1440px;
  margin: 0 auto;
}
.f02-data-maintenance--full-table .f02-inner {
  max-width: none;
}
.f02-data-maintenance--full-table .f02-filters {
  padding: 12px 24px;
}
.f02-data-maintenance--full-table .f02-filters .f02-filter-grid,
.f02-data-maintenance--full-table .f02-filters .f02-filter-actions,
.f02-data-maintenance--full-table .f02-filters .f02-filter-grid--more,
.f02-data-maintenance--full-table .f02-filters .query-extra,
.f02-data-maintenance--full-table .f02-filters .el-alert {
  display: none;
}
.f02-data-maintenance--full-table .f02-filters .f02-query-buttons {
  margin-top: 0;
  padding-top: 0;
  border-top: 0;
}
.f02-page-title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}
.f02-page-hint {
  margin: 0 0 20px;
  font-size: 12px;
  color: var(--f02-text-sec);
}
.f02-card {
  background: #fff;
  border: 1px solid var(--f02-border);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 24px;
  overflow: hidden;
}
.f02-filters {
  overflow: hidden;
}
.f02-filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px 24px;
  align-items: end;
}
.f02-field {
  min-width: 0;
}
.f02-field--span-2 {
  grid-column: span 2;
  min-width: 0;
}
.f02-filter-grid--more {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed var(--f02-border);
}
.f02-field label {
  display: block;
  font-size: 14px;
  color: var(--f02-text-sec);
  margin-bottom: 6px;
}
.f02-required {
  color: #ef4444;
  font-weight: 700;
  margin-right: 4px;
}
.f02-field--required-missing label {
  color: #c2410c;
}
.f02-field--required-missing :deep(.el-select .el-input__wrapper) {
  box-shadow: 0 0 0 1px #f59e0b inset;
  background-color: #fffaf0;
}
.f02-required-label-tip {
  margin-left: 6px;
  vertical-align: text-bottom;
  font-size: 14px;
  color: #f59e0b;
  opacity: 0;
  transition: opacity 0.2s ease;
}
.f02-required-label-tip.is-visible {
  opacity: 1;
}
.f02-control {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}
.f02-field :deep(.el-select),
.f02-field :deep(.el-date-editor),
.f02-field :deep(.el-input) {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}
.f02-field :deep(.el-select .el-input__wrapper),
.f02-field :deep(.el-date-editor .el-input__wrapper),
.f02-field :deep(.el-input .el-input__wrapper) {
  max-width: 100%;
}
.f02-field :deep(.el-date-editor.el-input__wrapper) {
  width: 100%;
  max-width: 100%;
}
.f02-date-range :deep(.el-range-input) {
  min-width: 0;
}
.f02-control--wide {
  width: 100%;
}
.f02-row-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.f02-filter-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.f02-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}
.f02-toolbar__left {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.f02-toolbar__right {
  display: flex;
  gap: 8px;
}
.f02-column-setting-dialog :deep(.el-dialog__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #e6ebf2;
}
.f02-column-setting-dialog :deep(.el-dialog) {
  max-width: calc(100vw - 32px);
}
.f02-column-setting-dialog :deep(.el-dialog__body) {
  padding: 0;
}
.f02-column-setting-dialog :deep(.el-dialog__footer) {
  padding: 10px 20px 14px;
  border-top: 1px solid #e6ebf2;
}
.f02-column-setting-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.f02-column-setting-dialog__header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}
.f02-column-setting-dialog__body {
  padding: 12px 20px 6px;
}
.f02-column-setting-dialog__search {
  margin-bottom: 10px;
}
.f02-column-setting-dialog__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 20px;
  max-height: 360px;
  overflow: auto;
}
.f02-column-setting-dialog__grid :deep(.el-checkbox) {
  margin-right: 0;
  font-size: 18px;
}
.f02-column-setting-dialog__grid :deep(.el-checkbox__label) {
  font-size: 18px;
}
.f02-column-setting-dialog__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.f02-table-wrap {
  border: 1px solid #dbe0e6;
  border-radius: 12px;
  box-shadow: 0 6px 22px rgba(15, 23, 42, 0.04);
  padding: 18px;
  overflow: hidden;
}
.table-section {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}
.f02-table {
  width: 100%;
}
.f02-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 14px;
}
.f02-pagination__info {
  font-size: 13px;
  color: #64748b;
}
.export-success-tip {
  line-height: 1.8;
  color: #475569;
}
:deep(.el-table th.el-table__cell) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 700;
}
:deep(.el-table td.el-table__cell) {
  color: #334155;
}
.f02-toast {
  position: fixed;
  top: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  color: #c2410c;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  font-size: 14px;
}
.f02-toast__icon {
  font-size: 18px;
  color: #f97316;
}
@media (max-width: 1200px) {
  .f02-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .f02-field--span-2 {
    grid-column: span 2;
  }
}
@media (max-width: 640px) {
  .f02-filter-grid {
    grid-template-columns: 1fr;
  }
  .f02-field--span-2 {
    grid-column: span 1;
  }
}
:deep(.el-button--primary) {
  --el-button-bg-color: var(--f02-primary);
  --el-button-border-color: var(--f02-primary);
}
</style>
