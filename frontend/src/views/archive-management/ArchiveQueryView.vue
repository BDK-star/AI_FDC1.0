<template>
  <div class="f02-data-maintenance" :class="{ 'f02-data-maintenance--full-table': tableFullPage }">
    <div class="f02-inner">
    <section class="f02-card f02-filters">
      <div class="filter-section">
        <div class="f02-filter-grid">
          <div class="f02-field f02-field--required" :class="{ 'f02-field--required-missing': !docTypeReady }">
            <label>
              <span class="f02-required">*</span>文档类型
              <el-tooltip content="请选择文档类型后再进行查询与批量操作" placement="top" :disabled="docTypeReady">
                <el-icon class="f02-required-label-tip" :class="{ 'is-visible': !docTypeReady }"><WarningFilled /></el-icon>
              </el-tooltip>
            </label>
            <el-select v-model="query.documentTypeCode" clearable filterable placeholder="请选择" @change="handleQueryTypeChange" class="f02-control">
              <el-option v-for="item in options.documentTypes" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </div>
          <div class="f02-field">
            <label>公司</label>
            <el-select v-model="query.companyProjectCode" clearable filterable placeholder="请选择公司" class="f02-control">
              <el-option v-for="item in companySelectOptions" :key="item.code" :label="`${item.code} · ${item.name}`" :value="item.code" />
            </el-select>
          </div>
          <div class="f02-field">
            <label>业务模块</label>
            <el-tree-select
              v-model="query.archiveTypeCodes"
              :data="businessModuleTreeOptions"
              multiple
              filterable
              clearable
              collapse-tags
              collapse-tags-tooltip
              check-strictly
              default-expand-all
              :render-after-expand="false"
              placeholder="可多选"
              class="f02-control"
              node-key="moduleCode"
              :props="{ value: 'moduleCode', label: 'queryLabel', children: 'children' }"
              @update:model-value="handleArchiveTypeChange"
            />
          </div>
          <div class="f02-field">
            <label>开始档期</label>
            <el-date-picker v-model="periodRange" type="monthrange" value-format="YYYY-MM" start-placeholder="开始" end-placeholder="结束" range-separator="~" class="f02-control f02-date-range" />
          </div>
          <div class="f02-field">
            <label>载体类型</label>
            <el-select
              v-model="query.carrierTypeCodes"
              multiple
              clearable
              collapse-tags
              collapse-tags-tooltip
              placeholder="可多选"
              class="f02-control"
            >
              <el-option v-for="item in options.carrierTypes" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </div>
          <div class="f02-field">
            <label>文档生成时间</label>
            <el-date-picker v-model="docGenerationRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" range-separator="~" class="f02-control f02-date-range" />
          </div>
          <div class="f02-field">
            <label>文档业务编码</label>
            <F03MultiLineFilterInput
              v-model="query.businessCode"
              drawer-title="文档业务编码（多行）"
              placeholder="单行模糊；多行请点右侧图标，每行精确（忽略大小写）"
              drawer-hint="主框仅一行时：模糊匹配。在抽屉中每行一条时：精确匹配（忽略大小写），多行之间为「或」，与其它筛选条件为「且」。最多100 行。"
              class="f02-control"
            />
          </div>
          <div class="f02-field">
            <label>文档组织</label>
            <el-select v-model="query.documentOrganizationCode" clearable filterable placeholder="请选择" class="f02-control">
              <el-option v-for="item in options.documentOrganizations" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
          </div>
        </div>

        <div class="f02-filter-actions">
          <el-button link type="primary" :disabled="!docTypeReady" @click="showMoreFilters = !showMoreFilters">
            {{ showMoreFilters ? '收起筛选条件' : '更多筛选条件' }}
            <el-icon class="el-icon--right"><ArrowDown v-if="!showMoreFilters" /><ArrowUp v-else /></el-icon>
          </el-button>
        </div>

        <div v-show="showMoreFilters" class="f02-filter-grid f02-filter-grid--more">
          <div class="f02-field">
            <label>文档名称</label>
            <el-input v-model="query.documentName" clearable placeholder="请输入" class="f02-control" />
          </div>
          <div v-for="field in visibleMoreFilterFields" :key="field.key" class="f02-field">
            <label>{{ field.label }}</label>
            <el-cascader
              v-if="field.type === 'cascader' && field.optionSource === 'archiveDestinationCascader'"
              v-model="archiveDestinationMorePath"
              :options="archiveDestinationCascaderOptions"
              :props="{ value: 'value', label: 'label', children: 'children', emitPath: true, checkStrictly: true }"
              clearable
              filterable
              :placeholder="field.placeholder || '请选择国家/省份/城市'"
              class="f02-control"
              style="width: 100%"
            />
            <el-select
              v-else-if="field.optionSource === 'geoCountries'"
              v-model="(advancedFilters as any)[field.key]"
              clearable
              filterable
              :placeholder="field.placeholder || '请选择国家'"
              class="f02-control"
            >
              <el-option v-for="item in options.geoCountries" :key="item.code" :label="item.name" :value="item.code" />
            </el-select>
            <el-select
              v-else-if="field.optionSource === 'barcodeModules'"
              v-model="(advancedFilters as any).barcodeModuleCodes"
              multiple
              clearable
              filterable
              collapse-tags
              collapse-tags-tooltip
              :placeholder="field.placeholder || '请选择'"
              class="f02-control"
            >
              <el-option
                v-for="b in barcodeModuleOptions"
                :key="b.barcodeCode"
                :label="`${b.barcodeCode} ｜ ${b.barcodeName}`"
                :value="b.barcodeCode"
              />
            </el-select>
            <el-select
              v-else-if="field.type === 'select'"
              v-model="(advancedFilters as any)[field.key]"
              clearable
              filterable
              :multiple="field.multiple ?? false"
              :placeholder="field.placeholder || '请选择'"
              class="f02-control"
            >
              <el-option v-for="opt in (field.options || moreFieldOptionsMap[field.key] || [])" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
            <el-date-picker
              v-else-if="field.type === 'daterange'"
              v-model="(advancedFilters as any)[field.key]"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始"
              end-placeholder="结束"
              range-separator="~"
              class="f02-control f02-date-range"
            />
            <F03MultiLineFilterInput
              v-else-if="field.type === 'input' && field.multilineDrawer"
              v-model="(advancedFilters as any)[field.key]"
              :placeholder="field.placeholder || '请输入'"
              :drawer-title="`编辑：${field.label}`"
              drawer-hint="每行一条，空行忽略；与其它筛选组合为「且」，本字段多行为「或」。最多 100 行。"
              class="f02-control"
            />
            <el-input
              v-else
              v-model="(advancedFilters as any)[field.key]"
              clearable
              :placeholder="field.placeholder || '请输入'"
              class="f02-control"
            />
          </div>
          <div v-for="field in moduleExtFilterFields" :key="`ext-${field.fieldCode}`" class="f02-field">
            <label class="module-ext-filter-label">
              <span>{{ field.fieldName }}</span>
              <el-tag v-if="field.fieldScope === 'ATTACHMENT'" size="small" effect="light" type="primary" class="module-ext-scope-tag">附件</el-tag>
            </label>
            <el-input
              v-model="queryExtFilters[field.fieldCode]"
              clearable
              placeholder="请输入"
              class="f02-control"
            />
          </div>
        </div>

        <div class="f02-query-buttons fdc-query-action-buttons">
          <el-button @click="resetFilters">重置</el-button>
          <el-button type="primary" @click="runQuery" :disabled="!docTypeReady">查询</el-button>
        </div>
      </div>

    </section>

    <section class="f02-toolbar">
      <div class="f02-toolbar__left">
        <el-button type="primary" @click="importQueryDialogVisible = true" :disabled="!docTypeReady">
          <el-icon class="el-icon--left"><Search /></el-icon>
          批量导入查询
        </el-button>
        <el-button @click="exportCsv" :disabled="!docTypeReady" :loading="exporting">批量导出</el-button>
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
        <el-table :data="queryResult.records" border @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" />
          <el-table-column v-for="column in visibleColumns" :key="column.prop" :prop="column.prop" :label="column.label" :width="column.width" :min-width="column.minWidth" show-overflow-tooltip>
            <template v-if="column.prop === 'businessCode'" #default="{ row }">
              <el-link type="primary" @click="viewArchiveDetail(row)">{{ row[column.prop] }}</el-link>
            </template>
            <template v-else-if="isDateTimeColumn(column.prop)" #default="{ row }">
              {{ formatDateTime(row[column.prop]) }}
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-container" v-if="queryResult.total > 0">
          <div class="pagination-info">
            共 {{ queryResult.total }} 条记录，每页显示
            <el-select v-model="query.pageSize" size="small" @change="runQuery" style="width: 100px; margin: 0 8px;" :disabled="!docTypeReady">
              <el-option v-for="size in pageSizeOptions" :key="size" :label="size" :value="size" />
            </el-select>
            条
          </div>
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.pageSize"
            :page-sizes="pageSizeOptions"
            layout="total, sizes, prev, pager, next, jumper"
            :total="queryResult.total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </section>

    <F03BatchImportModal
      v-model="importQueryDialogVisible"
      title="批量导入查询"
      header-icon="upload"
      :hint="importQueryModalHint"
      :template-csv="importQueryTemplateCsv"
      :download-file-name="importQueryDownloadFileName"
      accept=".csv,text/csv"
      accept-hint="仅支持 CSV（UTF-8）；模板固定6列，首行为表头"
      confirm-label="确认导入"
      :loading="importing"
      @confirm="handleImportQueryModalConfirm"
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

    <el-dialog v-model="exportSuccessVisible" title="导出成功" width="440px" destroy-on-close>
      <p class="export-success-tip">导出成功，请到我的导出查看并下载 CSV 文件。</p>
      <template #footer>
        <el-button @click="exportSuccessVisible = false">关闭</el-button>
        <el-button type="primary" @click="goMyExports">前往我的导出</el-button>
      </template>
    </el-dialog>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, ArrowUp, FullScreen, RefreshRight, Search, Setting, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onActivated, onMounted, reactive, ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createPendingDocumentsExportJob, fetchArchiveCreateOptions, queryArchives, submitArchiveImportQueryJob, type ArchiveQueryCommand } from '../../api/modules/archiveManagement'
import { resolveCustodyStatusLabelOptions } from './archiveCustodyStatusOptions'
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
import { fetchBarcodeModules } from '../../api/modules/barcodeModule'
import { fetchCountryRegions } from '../../api/modules/countryRegion'
import type {
  ArchiveCreateOptions,
  ArchiveQueryResult,
  BarcodeModule,
  BusinessModuleExtField,
  BusinessModuleNode,
  CountryRegionItem
} from '../../types'
import type { User } from '../../api/modules/security'
import { validateMultiValueInput } from '../../utils/multiValueQuery'
import { useLayoutStore } from '../../stores/useLayoutStore'
import F03BatchImportModal from '../../components/f03/F03BatchImportModal.vue'
import F03MultiLineFilterInput from '../../components/f03/F03MultiLineFilterInput.vue'
import { archiveQueryPageConfig, getVisibleMoreFilterFields } from './queryPageConfig'
import {
  buildArchiveDestinationCascaderOptions,
  buildArchiveDestinationPath
} from '../../utils/archiveFlowAlignedFieldUtils'
import { EXT_DETAIL_FIELD_ORDER, hardCodedExtLabelMap, isHardCodedFieldVisible } from './extFieldDisplayConfig'

const route = useRoute()
const router = useRouter()
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

type ArchiveQueryCommandWithPage = ArchiveQueryCommand & {
  page: number
  pageSize: number
  archiveTypeCodes: string[]
  carrierTypeCodes: string[]
}

const layout = useLayoutStore()

const query = reactive<ArchiveQueryCommandWithPage>({
  keyword: '',
  documentTypeCode: '',
  companyProjectCode: '',
  archiveTypeCodes: [],
  carrierTypeCodes: [],
  securityLevelCode: '',
  documentName: '',
  businessCode: '',
  dutyPerson: '',
  archiveDestination: '',
  sourceSystem: '',
  documentOrganizationCode: '',
  page: 1,
  pageSize: 20
})

const companySelectOptions = ref<Array<{ code: string; name: string }>>([])
const userSelectOptions = ref<Array<{ label: string; value: string }>>([])
const userDisplayById = computed<Record<string, string>>(() =>
  Object.fromEntries(userSelectOptions.value.map((item) => [item.value, item.label]))
)
const businessModuleSourceTree = ref<BusinessModuleNode[]>([])
const businessModuleTreeOptions = ref<ModuleQueryTreeNode[]>([])
const barcodeModuleOptions = ref<BarcodeModule[]>([])
const archiveDestinationMorePath = ref<string[]>([])
const regionCountryOptions = ref<CountryRegionItem[]>([])
const regionProvinceOptions = ref<CountryRegionItem[]>([])
const regionCityOptions = ref<CountryRegionItem[]>([])
const archiveDestinationCascaderOptions = computed(() =>
  buildArchiveDestinationCascaderOptions(
    regionCountryOptions.value,
    regionProvinceOptions.value,
    regionCityOptions.value
  )
)

const syncArchiveDestinationPathFromAdvanced = () => {
  const code = String(advancedFilters.archiveDestination || '').trim()
  archiveDestinationMorePath.value = code
    ? buildArchiveDestinationPath(code, regionCountryOptions.value, regionProvinceOptions.value, regionCityOptions.value)
    : []
}

watch(archiveDestinationMorePath, (path) => {
  if (!path?.length) {
    advancedFilters.archiveDestination = ''
    return
  }
  advancedFilters.archiveDestination = path[path.length - 1] || ''
})

async function loadArchiveDestinationRegionTree() {
  try {
    const countries = await fetchCountryRegions({ regionLevel: 'COUNTRY' })
    const countryCodes = countries.map((c) => c.regionCode).filter(Boolean)
    const provincesNested = await Promise.all(
      countryCodes.map((cc) => fetchCountryRegions({ regionLevel: 'PROVINCE', parentRegionCode: cc }))
    )
    const provinces = provincesNested.flat()
    const provinceCodes = provinces.map((p) => p.regionCode).filter(Boolean)
    const citiesNested = await Promise.all(
      provinceCodes.map((pc) => fetchCountryRegions({ regionLevel: 'CITY', parentRegionCode: pc }))
    )
    const cities = citiesNested.flat()
    regionCountryOptions.value = countries
    regionProvinceOptions.value = provinces
    regionCityOptions.value = cities
    syncArchiveDestinationPathFromAdvanced()
  } catch {
    regionCountryOptions.value = []
    regionProvinceOptions.value = []
    regionCityOptions.value = []
    archiveDestinationMorePath.value = []
  }
}
const docTypeReady = computed(() => Boolean(query.documentTypeCode && query.documentTypeCode.trim()))
const selectedDocTypeName = computed(() => {
  const item = options.documentTypes.find((d) => d.code === query.documentTypeCode)
  return item?.name || ''
})
const showMoreFilters = ref(false)
const periodRange = ref<[string, string] | null>(null)
const docGenerationRange = ref<[string, string] | null>(null)
const advancedFilters = reactive<Record<string, any>>({
  barcodeModuleCodes: [] as string[],
  documentArchiveTypes: [] as string[],
  country: '',
  repOffice: '',
  region: '',
  custodyStatus: [],
  securityLevelCode: [],
  description: '',
  archiveDestination: '',
  originPlace: '',
  dutyPerson: '',
  respDept: '',
  createdBy: '',
  creationDateRange: null,
  sourceSystem: [],
  visibility: '' as string,
  archivedEntityName: '',
  archiveBarcodeRange: '',
  signDateRange: null as [string, string] | null,
  signedBy: '' as string,
  verificationDateRange: null,
  verifiedBy: [] as string[],
  volumeSeqNo: '',
  volumeBarcodeRange: '',
  volumizationDateRange: null,
  assembledBy: [] as string[],
  volumeNoRange: '',
  repository: '',
  storageLocationRange: '',
  storageDateRange: null,
  storedBy: [] as string[],
  copies: '',
  remainingCopies: '',
  invoiceNo: '',
  refNo: '',
  accountant: [] as string[],
  scannedBy: [] as string[],
  issueDateRange: null,
  maturityDateRange: null,
  lgExpiryDateRange: null,
  lgLedgerStatus: [],
  bankName: '',
  currency: [],
  amount: '',
  issuingAuthority: '',
  disposalTimeRange: null,
  businessVolumeNo: '',
  lgWorkflowNo: '',
  lgNo: ''
})

const visibleMoreFilterFields = computed(() =>
  getVisibleMoreFilterFields(archiveQueryPageConfig.moreFilterFields, selectedDocTypeName.value)
)
const moreFieldOptionsMap = computed<Record<string, Array<{ label: string; value: string }>>>(() => {
  const cc = String(advancedFilters.country || '').trim()
  const repSrc = cc
    ? options.geoRepOffices.filter((item) => String(item.code || '') === cc)
    : options.geoRepOffices
  const regSrc = cc
    ? options.geoRegions.filter((item) => String(item.code || '') === cc)
    : options.geoRegions
  return {
    country: options.geoCountries.map((item) => ({ label: item.name, value: item.code })),
    repOffice: repSrc.map((item) => ({ label: item.name, value: item.name })),
    region: regSrc.map((item) => ({ label: item.name, value: item.name })),
    documentArchiveTypes: options.archiveTypes.map((item) => ({ label: item.name, value: item.code })),
    custodyStatus: options.custodyStatuses.map((item) => ({ label: item.name, value: item.code })),
    signedBy: userSelectOptions.value,
    securityLevelCode: options.securityLevels.map((item) => ({ label: item.name, value: item.code })),
    sourceSystem: [],
    verifiedBy: userSelectOptions.value,
    assembledBy: userSelectOptions.value,
    storedBy: userSelectOptions.value,
    accountant: userSelectOptions.value,
    scannedBy: userSelectOptions.value,
    visibility: [
      { label: '是', value: '是' },
      { label: '否', value: '否' }
    ]
  }
})

watch(
  () => advancedFilters.country,
  (next) => {
    const cc = String(next || '').trim()
    if (!cc) {
      advancedFilters.repOffice = ''
      advancedFilters.region = ''
      return
    }
    const repNames = new Set(
      options.geoRepOffices.filter((o) => String(o.code || '') === cc).map((o) => o.name)
    )
    const regNames = new Set(
      options.geoRegions.filter((o) => String(o.code || '') === cc).map((o) => o.name)
    )
    const ro = String(advancedFilters.repOffice || '').trim()
    const rg = String(advancedFilters.region || '').trim()
    if (ro && !repNames.has(ro)) advancedFilters.repOffice = ''
    if (rg && !regNames.has(rg)) advancedFilters.region = ''
  }
)

const moduleExtFilterFields = ref<BusinessModuleExtField[]>([])
const queryExtFilters = reactive<Record<string, string>>({})
const queryResult = reactive<ArchiveQueryResult>({ records: [], queryFields: [], total: 0, page: 1, pageSize: 20 })
const pageSizeOptions = [20, 50, 100, 500, 1000]

const selectedRecords = ref<any[]>([])
const tableFullPage = ref(false)
const allColumns = ref(archiveQueryPageConfig.columns)
const selectedColumnKeys = ref([...archiveQueryPageConfig.defaultVisibleColumns])
const columnSettingVisible = ref(false)
const columnSearchKeyword = ref('')
const columnDraftKeys = ref<string[]>([...archiveQueryPageConfig.defaultVisibleColumns])
const importQueryDialogVisible = ref(false)
const importing = ref(false)
const exporting = ref(false)
const exportSuccessVisible = ref(false)

function collectExportDocIdStrings(rows: Iterable<{ docId?: unknown; archiveId?: unknown; id?: unknown }>): string[] {
  const seen = new Set<string>()
  const out: string[] = []
  for (const r of rows) {
    const raw = r.docId ?? r.archiveId ?? r.id
    if (raw == null) continue
    const s = String(raw).trim()
    if (!/^\d+$/.test(s)) continue
    if (seen.has(s)) continue
    seen.add(s)
    out.push(s)
  }
  return out
}

const goMyExports = () => {
  exportSuccessVisible.value = false
  router.push('/workspace/export-query')
}

const importQueryTemplateCsv =
  ['文档业务编码,公司,业务模块,开始档期', 'DEMO-BIZ-001,CP-DEMO-001,GL,2024-01', 'DEMO-BIZ-002,CP-DEMO-001,GL,2024-01'].join('\n') + '\n'
const importQueryModalHint =
  '模板须含列：文档业务编码、公司、业务模块、开始档期。每行至少填写「文档业务编码」；若需按发票号/其他编号查，可自行追加「发票号」「其他相关编号」列。行内为且（AND），逐行合并结果。'
const importQueryDownloadFileName = computed(() => {
  const d = new Date().toISOString().slice(0, 10)
  return `archive-query-import-${d}.csv`
})

const visibleColumns = computed(() => {
  return allColumns.value.filter(column => selectedColumnKeys.value.includes(column.prop))
})
const filteredColumnOptions = computed(() => {
  const keyword = columnSearchKeyword.value.trim().toLowerCase()
  if (!keyword) return allColumns.value
  return allColumns.value.filter((col) =>
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

const isDateTimeColumn = (prop: string) => ['documentDate', 'creationDate', 'lastUpdateDate'].includes(prop)

const loadOptions = async () => {
  const [result, companies, moduleTree, users, barcodeMods] = await Promise.all([
    fetchArchiveCreateOptions(),
    fetchCompanyInfos({ enabledFlag: 'Y' }),
    fetchBusinessModuleTree().catch((): BusinessModuleNode[] => []),
    fetchUsers().catch((): User[] => []),
    fetchBarcodeModules({ enabledOnly: true }).catch((): BarcodeModule[] => [])
  ])
  barcodeModuleOptions.value = barcodeMods
  Object.assign(options, result)
  options.custodyStatuses = await resolveCustodyStatusLabelOptions(options.custodyStatuses ?? [])
  await loadArchiveDestinationRegionTree()
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
  syncBusinessModuleOptionsByDocumentType(query.documentTypeCode)
  console.log('Document organizations:', options.documentOrganizations)
}

const syncBusinessModuleOptionsByDocumentType = (documentTypeCode?: string) => {
  const filtered = filterBusinessModuleTreeByDocumentType(
    businessModuleSourceTree.value,
    documentTypeCode || ''
  )
  businessModuleTreeOptions.value = buildModuleQueryTree(filtered)
  const dt = documentTypeCode || ''
  if (query.archiveTypeCodes?.length) {
    query.archiveTypeCodes = query.archiveTypeCodes.filter((c) =>
      isModuleCodeWithinDocumentType(businessModuleSourceTree.value, dt, c)
    )
  }
}

const handleQueryTypeChange = async (typeCode?: string) => {
  layout.setDocumentTypeCode(typeCode || '')
  syncBusinessModuleOptionsByDocumentType(typeCode)
  moduleExtFilterFields.value = []
  Object.keys(queryExtFilters).forEach(key => delete queryExtFilters[key])
}

const handleArchiveTypeChange = async (next?: string | string[]) => {
  const codes = (Array.isArray(next) ? next : next ? [next] : [])
    .map((c) => String(c).trim())
    .filter(Boolean)
  if (!codes.length) {
    moduleExtFilterFields.value = []
    Object.keys(queryExtFilters).forEach((key) => delete queryExtFilters[key])
    return
  }
  let primary = codes[0]
  if (query.documentTypeCode?.trim()) {
    if (
      !isModuleCodeWithinDocumentType(
        businessModuleSourceTree.value,
        query.documentTypeCode,
        primary
      )
    ) {
      const rootDocType = resolveRootDocumentTypeCodeByModule(businessModuleSourceTree.value, primary)
      if (rootDocType) {
        query.documentTypeCode = rootDocType
        await handleQueryTypeChange(rootDocType)
      } else {
        query.archiveTypeCodes = []
        moduleExtFilterFields.value = []
        Object.keys(queryExtFilters).forEach((key) => delete queryExtFilters[key])
        return
      }
    }
    const dt = query.documentTypeCode || ''
    query.archiveTypeCodes = codes.filter((c) =>
      isModuleCodeWithinDocumentType(businessModuleSourceTree.value, dt, c)
    )
    if (!query.archiveTypeCodes.length) {
      moduleExtFilterFields.value = []
      Object.keys(queryExtFilters).forEach((key) => delete queryExtFilters[key])
      return
    }
    primary = query.archiveTypeCodes[0]
    await loadModuleExtFilterFields(primary)
    return
  }
  const rootDocType = resolveRootDocumentTypeCodeByModule(businessModuleSourceTree.value, primary)
  if (!rootDocType) return
  query.documentTypeCode = rootDocType
  await handleQueryTypeChange(rootDocType)
  query.archiveTypeCodes = codes.filter((c) =>
    isModuleCodeWithinDocumentType(businessModuleSourceTree.value, rootDocType, c)
  )
  await loadModuleExtFilterFields(query.archiveTypeCodes[0] || primary)
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

const buildStringExtFilters = () => {
  const sr = (advancedFilters as { signDateRange?: [string, string] | null }).signDateRange
  const normalized: Record<string, string> = {
    ...queryExtFilters,
    docGenerationStart: docGenerationRange.value?.[0] || '',
    docGenerationEnd: docGenerationRange.value?.[1] || '',
    signDateStart: Array.isArray(sr) && sr[0] ? String(sr[0]).trim() : '',
    signDateEnd: Array.isArray(sr) && sr[1] ? String(sr[1]).trim() : ''
  }

  Object.entries(advancedFilters).forEach(([key, value]) => {
    if (key === 'barcodeModuleCodes' || key === 'documentArchiveTypes' || key === 'signDateRange') return
    if (value === null || value === undefined) return
    if (Array.isArray(value)) {
      const first = value.find((item) => item !== null && item !== undefined && String(item).trim() !== '')
      if (first !== undefined) normalized[key] = String(first)
      return
    }
    if (typeof value === 'string') {
      const trimmed = value.trim()
      if (trimmed) normalized[key] = trimmed
      return
    }
    if (typeof value === 'number' || typeof value === 'boolean') {
      normalized[key] = String(value)
    }
  })

  return normalized
}

const runQuery = async () => {
  if (!docTypeReady.value) return
  const multiErr = validateMultiValueInput({
    文档业务编码: query.businessCode,
    发票号: String((advancedFilters as any).invoiceNo || ''),
    其他相关编号: String((advancedFilters as any).refNo || '')
  })
  if (multiErr) {
    ElMessage.warning(multiErr)
    return
  }
  const extFilters = buildStringExtFilters()
  const docArchiveTypes = Array.isArray((advancedFilters as any).documentArchiveTypes)
    ? (advancedFilters as any).documentArchiveTypes.filter((c: unknown) => String(c ?? '').trim())
    : []
  if (docArchiveTypes.length) {
    extFilters.archiveType = docArchiveTypes.join(',')
  }
  const queryFields = { ...query } as Record<string, unknown>
  const command: ArchiveQueryCommand = {
    ...(queryFields as unknown as typeof query),
    archiveTypeCodes: query.archiveTypeCodes?.length ? [...query.archiveTypeCodes] : undefined,
    carrierTypeCodes: query.carrierTypeCodes?.length ? [...query.carrierTypeCodes] : undefined,
    barcodeModuleCodes:
      Array.isArray((advancedFilters as any).barcodeModuleCodes) && (advancedFilters as any).barcodeModuleCodes.length
        ? [...(advancedFilters as any).barcodeModuleCodes]
        : undefined,
    beginPeriod: periodRange.value?.[0] || undefined,
    endPeriod: periodRange.value?.[1] || undefined,
    securityLevelCode:
      Array.isArray((advancedFilters as any).securityLevelCode) && (advancedFilters as any).securityLevelCode.length
        ? (advancedFilters as any).securityLevelCode[0]
        : query.securityLevelCode,
    sourceSystem:
      Array.isArray((advancedFilters as any).sourceSystem) && (advancedFilters as any).sourceSystem.length
        ? (advancedFilters as any).sourceSystem[0]
        : query.sourceSystem,
    archiveDestination: ((advancedFilters as any).archiveDestination as string) || query.archiveDestination,
    dutyPerson: ((advancedFilters as any).dutyPerson as string) || query.dutyPerson,
    extFilters
  }
  try {
    const result = await queryArchives(command)
    queryResult.records = result.records
    queryResult.queryFields = result.queryFields
    queryResult.total = result.total
    queryResult.page = result.page
    queryResult.pageSize = result.pageSize
    if (!result.records?.length) {
      ElMessage.info('未查询到匹配数据，请调整筛选条件后重试')
    }
  } catch (e: any) {
    ElMessage.error(e?.message || '文档查询失败，请稍后重试')
  }
}

watch(
  () => layout.documentTypeCode,
  (next) => {
    if (next && next !== query.documentTypeCode) {
      query.documentTypeCode = next
      handleQueryTypeChange(next)
    }
  },
  { immediate: true }
)

const resetFilters = async () => {
  Object.assign(query, {
    keyword: '',
    documentTypeCode: '',
    companyProjectCode: '',
    archiveTypeCodes: [],
    carrierTypeCodes: [],
    securityLevelCode: '',
    documentName: '',
    businessCode: '',
    dutyPerson: '',
    archiveDestination: '',
    sourceSystem: '',
    documentOrganizationCode: '',
    page: 1,
    pageSize: 20
  })
  periodRange.value = null
  docGenerationRange.value = null
  Object.assign(advancedFilters, {
    barcodeModuleCodes: [],
    documentArchiveTypes: [],
    country: '',
    repOffice: '',
    region: '',
    custodyStatus: [],
    description: '',
    archiveDestination: '',
    originPlace: '',
    respDept: '',
    createdBy: '',
    creationDateRange: null,
    visibility: '' as string,
    archivedEntityName: '',
    barcodeModule: '',
    archiveBarcodeRange: '',
    signDateRange: null,
    signedBy: '',
    verificationDateRange: null,
    verifiedBy: [],
    volumeSeqNo: '',
    volumeBarcodeRange: '',
    volumizationDateRange: null,
    assembledBy: [],
    volumeNoRange: '',
    repository: '',
    storageLocationRange: '',
    storageDateRange: null,
    storedBy: [],
    copies: '',
    remainingCopies: '',
    invoiceNo: '',
    refNo: '',
    accountant: [],
    scannedBy: [],
    issueDateRange: null,
    maturityDateRange: null,
    lgExpiryDateRange: null,
    lgLedgerStatus: [],
    bankName: '',
    currency: [],
    amount: '',
    issuingAuthority: '',
    disposalTimeRange: null,
    businessVolumeNo: '',
    lgWorkflowNo: '',
    lgNo: '',
    securityLevelCode: [],
    sourceSystem: [],
    dutyPerson: ''
  })
  archiveDestinationMorePath.value = []
  Object.keys(queryExtFilters).forEach(key => delete queryExtFilters[key])
  moduleExtFilterFields.value = []
  syncBusinessModuleOptionsByDocumentType('')
  queryResult.records = []
  queryResult.queryFields = []
  queryResult.total = 0
  queryResult.page = 1
  queryResult.pageSize = 20
  selectedRecords.value = []
}

const handleSizeChange = (size: number) => {
  query.pageSize = size
  query.page = 1
  runQuery()
}

const handleCurrentChange = (current: number) => {
  query.page = current
  runQuery()
}

const viewArchiveDetail = (row: any) => {
  const archiveId = Number(row?.archiveId)
  const resolved = router.resolve({
    path: `/archive-management/detail/${Number.isFinite(archiveId) ? archiveId : encodeURIComponent(String(row?.businessCode || '0'))}`,
    query: {
      from: 'query',
      businessCode: String(row?.businessCode || ''),
      documentName: String(row?.documentName || '')
    }
  })
  window.open(resolved.href, '_blank', 'noopener,noreferrer')
}

// 快捷筛选（仅预设条件，不自动发起查询）
const filterForMyArchive = () => {
  query.dutyPerson = ''
}

const handleSelectionChange = (selection: any[]) => {
  selectedRecords.value = selection
}

const handleColumnSettingClick = () => {
  columnSearchKeyword.value = ''
  columnDraftKeys.value = [...selectedColumnKeys.value]
  columnSettingVisible.value = true
}
const resetColumnDraftSelection = () => {
  columnDraftKeys.value = [...archiveQueryPageConfig.defaultVisibleColumns]
}
const confirmColumnDraftSelection = () => {
  const next = [...columnDraftKeys.value]
  if (!next.includes('businessCode')) next.push('businessCode')
  selectedColumnKeys.value = next
  columnSettingVisible.value = false
}

const toCsv = (data: any[]) => {
  const cols = visibleColumns.value
  const headers = cols.map((c) => c.label)
  const props = cols.map((c) => c.prop)
  const escapeCell = (v: any) => {
    const s = String(v ?? '')
    const needsQuote = /[",\n\r]/.test(s)
    const escaped = s.replaceAll('"', '""')
    return needsQuote ? `"${escaped}"` : escaped
  }
  const lines = [headers.join(',')]
  for (const row of data) lines.push(props.map((p) => escapeCell(row[p])).join(','))
  return lines.join('\n')
}

const formatDateTimeForExport = (value: unknown) => {
  if (value === null || value === undefined || value === '') return ''
  const text = String(value).trim()
  if (!text) return ''
  const normalized = text.includes('T') ? text : text.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return text
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
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
const buildDetailExportSchema = (records: any[]): DetailExportColumn[] => {
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

const resolveDetailExportValue = (row: any, prop: string) => {
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
  if (prop === 'documentDate' || prop === 'lastUpdateDate') return formatDateTimeForExport(row?.[prop])
  if (prop === 'securityLevelName') return row?.securityLevelName || row?.securityLevelCode || ''
  if (prop === 'companyProjectName') return row?.companyProjectName || row?.companyProjectCode || ''
  if (prop === 'documentTypeName') return row?.documentTypeName || row?.documentTypeCode || ''
  if (prop === 'documentVisibility') return row?.documentVisibility ?? ext.visibility ?? '是'
  if (prop === 'custodyStatus') return row?.custodyStatus || ''
  return row?.[prop] ?? ''
}

const toDetailCsv = (records: any[]) => {
  const cols = buildDetailExportSchema(records)
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
      return escapeCell(resolveDetailExportValue(row, p))
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
  const data = selectedRecords.value.length > 0 ? selectedRecords.value : queryResult.records
  if (!data.length) {
    ElMessage.warning('暂无可导出的数据')
    return
  }
  const docIds = collectExportDocIdStrings(data)
  if (!docIds.length) {
    ElMessage.warning('未找到可导出文档标识')
    return
  }
  exporting.value = true
  try {
    await createPendingDocumentsExportJob({
      docIds,
      exportFileFormat: 'CSV',
      exportScope: 'DOCUMENT_QUERY'
    })
    exportSuccessVisible.value = true
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '导出失败'
    ElMessage.error(msg)
  } finally {
    exporting.value = false
  }
}

const applyImportQuery = async (file: File) => {
  importing.value = true
  try {
    if (!query.documentTypeCode?.trim()) {
      ElMessage.warning('请先选择文档类型')
      return
    }
    await submitArchiveImportQueryJob({
      file,
      documentTypeCode: query.documentTypeCode
    })
    importQueryDialogVisible.value = false
    ElMessage.success('已提交批量导入查询，请前往「我的工作空间 → 我的导入」查看结果查询')
  } finally {
    importing.value = false
  }
}

const handleImportQueryModalConfirm = async (payload: { file: File | null }) => {
  if (!payload.file) return
  await applyImportQuery(payload.file)
}

onMounted(async () => {
  tableFullPage.value = false
  const routeKeyword = typeof route.query.q === 'string' ? route.query.q.trim() : ''
  if (routeKeyword) {
    query.keyword = routeKeyword
  }
  await loadOptions()
  if (route.query.mine === '1') {
    filterForMyArchive()
  }
})

onActivated(() => {
  tableFullPage.value = false
})

watch(
  selectedColumnKeys,
  (next) => {
    if (!next.includes('businessCode')) {
      selectedColumnKeys.value = [...next, 'businessCode']
    }
  },
  { deep: true }
)
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
.f02-card {
  background: #fff;
  border: 1px solid var(--f02-border);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  padding: 24px;
  margin-bottom: 24px;
  overflow: hidden;
}
.section-actions {
  display: flex;
  gap: 8px;
}
.f02-filters { overflow: hidden; }
.filter-section { overflow: hidden; }
.f02-filter-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  display: grid;
  gap: 16px 24px;
  align-items: end;
}
.f02-field {
  min-width: 0;
}
.f02-field label {
  display: block;
  font-size: 14px;
  color: var(--f02-text-sec);
  margin-bottom: 6px;
}
.f02-field label.module-ext-filter-label {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.module-ext-scope-tag {
  flex-shrink: 0;
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
.query-extra {
  margin: 20px 0;
  padding: 16px;
  background-color: #f9fafb;
  border-radius: 6px;
  border: 1px solid var(--f02-border);
}
.f02-filter-grid--more {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed var(--f02-border);
}
.f02-filter-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.query-extra__title {
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 600;
  color: var(--f02-text-sec);
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
.el-table {
  width: 100%;
}
:deep(.el-table th.el-table__cell) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 700;
}
:deep(.el-table td.el-table__cell) {
  color: #334155;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
}
.pagination-info {
  font-size: 13px;
  color: #64748b;
}
.export-success-tip {
  line-height: 1.8;
  color: #475569;
}
:deep(.el-button--primary) {
  --el-button-bg-color: var(--f02-primary);
  --el-button-border-color: var(--f02-primary);
}
@media (max-width: 1280px) {
  .f02-filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
@media (max-width: 1024px) {
  .f02-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 768px) {
  .f02-filter-grid {
    grid-template-columns: 1fr;
  }
  .section-head {
    flex-direction: column;
  }
  .pagination-container {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>
