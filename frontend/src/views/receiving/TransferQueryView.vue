<template>
  <div class="transfer-query-page">
    <el-card shadow="never">
      <el-form :model="searchForm" label-width="108px" class="filter-form" @submit.prevent>
        <div class="filter-grid">
          <el-form-item label="文档业务编码">
            <el-input v-model="searchForm.docBusiNo" class="input-w180" placeholder="模糊查询" clearable />
          </el-form-item>
          <el-form-item label="公司">
            <el-select v-model="searchForm.companyProjectCode" class="input-w180" clearable filterable placeholder="请选择公司">
              <el-option
                v-for="o in companyOptions"
                :key="o.code"
                :label="`${o.code} · ${o.name}`"
                :value="o.code"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="业务模块">
            <el-tree-select
              v-model="searchForm.busiModuleCode"
              :data="businessModuleTreeOptions"
              filterable
              clearable
              check-strictly
              default-expand-all
              :render-after-expand="false"
              placeholder="请选择业务模块"
              class="input-w180"
              node-key="moduleCode"
              :props="{ value: 'moduleCode', label: 'queryLabel', children: 'children' }"
            />
          </el-form-item>
          <el-form-item label="开始档期" class="span-range">
            <el-date-picker
              v-model="periodRange"
              type="monthrange"
              value-format="YYYY-MM"
              range-separator="~"
              start-placeholder="开始"
              end-placeholder="结束"
              unlink-panels
              class="input-range"
            />
          </el-form-item>
          <el-form-item label="申请人">
            <el-select v-model="searchForm.applicant" class="input-w180" clearable filterable placeholder="用户名 + 工号">
              <el-option v-for="u in userOptions" :key="u.id" :label="u.name" :value="u.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="申请单号">
            <el-input v-model="searchForm.applicationNumber" class="input-w180" placeholder="模糊查询" clearable />
          </el-form-item>
          <el-form-item label="申请日期" class="span-range">
            <el-date-picker
              v-model="searchForm.applicationDateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              unlink-panels
              class="input-range"
            />
          </el-form-item>
          <el-form-item label="申请状态">
            <el-select v-model="searchForm.applicationStatus" class="input-w180" clearable placeholder="请选择">
              <el-option v-for="o in statusOptions" :key="o.code" :label="o.name" :value="o.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="载体类型">
            <el-select v-model="searchForm.carrierType" class="input-w180" clearable placeholder="请选择">
              <el-option v-for="o in carrierTypeOptions" :key="o.code" :label="o.name" :value="o.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="差异原因">
            <el-select v-model="searchForm.diffReasonCode" class="input-w180" clearable placeholder="请选择">
              <el-option v-for="o in diffReasonOptions" :key="o.code" :label="o.name" :value="o.code" />
            </el-select>
          </el-form-item>
        </div>

        <div v-show="filterExpanded" class="filter-grid filter-grid--extra">
          <el-form-item label="移交方式">
            <el-select v-model="searchForm.applyMethod" class="input-w180" clearable placeholder="请选择">
              <el-option v-for="o in applyMethodOptions" :key="o.code" :label="o.name" :value="o.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="邮寄方式">
            <el-select v-model="searchForm.expressType" class="input-w180" clearable placeholder="请选择">
              <el-option v-for="o in expressTypeOptions" :key="o.code" :label="o.name" :value="o.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="邮寄单号">
            <el-input v-model="searchForm.expressNumber" class="input-w180" placeholder="模糊查询" clearable />
          </el-form-item>
          <el-form-item label="接收人">
            <el-select v-model="searchForm.documentRecipient" class="input-w180" clearable filterable placeholder="用户名 + 工号">
              <el-option v-for="u in userOptions" :key="u.id" :label="u.name" :value="u.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="册号">
            <el-input v-model="searchForm.catalogVolumeNo" class="input-w180" placeholder="模糊查询" clearable />
          </el-form-item>
        </div>

        <div class="filter-toolbar">
          <el-button link type="primary" @click="filterExpanded = !filterExpanded">
            {{ filterExpanded ? '收起更多条件' : '展开更多条件' }}
          </el-button>
          <div class="filter-toolbar__actions">
            <el-button @click="reset">重置</el-button>
            <el-button type="primary" :loading="loading" @click="runQuery">查询</el-button>
          </div>
        </div>
      </el-form>

      <div class="table-section">
        <div class="table-toolbar">
          <div class="table-toolbar__left">
            <el-button type="primary" @click="goToCreate">发起移交</el-button>
            <el-button @click="hintTodo('导出')">导出</el-button>
          </div>
          <div class="table-toolbar__right">
            <el-tooltip content="列设置" placement="top"><el-button circle :icon="Setting" @click="notifyColumnSetting" /></el-tooltip>
            <el-tooltip :content="tableFullPage ? '退出全页面展示' : '列表栏信息全页面展示'" placement="top"><el-button circle :icon="FullScreen" @click="tableFullPage = !tableFullPage" /></el-tooltip>
            <el-tooltip content="刷新数据" placement="top"><el-button circle :icon="RefreshRight" @click="runQuery" :disabled="loading" /></el-tooltip>
          </div>
        </div>

        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="8" animated />
        </div>
        <div v-else class="transfer-list">
          <el-table :data="tableRecords" border class="transfer-table">
            <el-table-column label="序号" width="64" align="center">
              <template #default="{ $index }">
                {{ (pagination.page - 1) * pagination.pageSize + $index + 1 }}
              </template>
            </el-table-column>
            <el-table-column prop="applicationNumber" label="申请单号" min-width="160" show-overflow-tooltip />
            <el-table-column label="业务模块" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">{{ row.busiModuleName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="120" show-overflow-tooltip />
            <el-table-column label="申请日期" width="170">
              <template #default="{ row }">{{ formatDateTime(row.applicationDate) }}</template>
            </el-table-column>
            <el-table-column label="申请状态" width="110">
              <template #default="{ row }">
                <el-tag v-if="row.applicationStatus" size="small" :type="statusTagType(row.applicationStatus)" effect="light">
                  {{ labelOf(statusOptions, row.applicationStatus) }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="documentRecipientName" label="接收人" width="120" show-overflow-tooltip />
            <el-table-column label="邮寄方式" width="120">
              <template #default="{ row }">{{ labelOf(expressTypeOptions, row.expressType) }}</template>
            </el-table-column>
            <el-table-column prop="expressNumber" label="邮寄单号" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openView(row.applicationId)">查看</el-button>
                <el-button
                  v-if="row.applicationStatus === 'DRAFT' || row.applicationStatus === 'REJECTED'"
                  link
                  type="primary"
                  @click="goEdit(row.applicationId)"
                >
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[20, 50, 100, 200]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="pagination.total"
              @size-change="handleSizeChange"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </div>

    </el-card>
  </div>
</template>

<script setup lang="ts">
import axios from 'axios'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { FullScreen, RefreshRight, Setting } from '@element-plus/icons-vue'
import { fetchArchiveCreateOptions } from '../../api/modules/archiveManagement'
import { buildModuleQueryTree, fetchBusinessModuleTree, type ModuleQueryTreeNode } from '../../api/modules/businessModule'
import { fetchCompanyInfos } from '../../api/modules/companyInfo'
import { fetchDictionaryItems } from '../../api/modules/dictionary'
import { fetchUsers } from '../../api/modules/security'
import {
  searchTransferApplicationRecords,
  type TransferApplicationRecordQuery,
  type TransferApplicationRecordRow
} from '../../api/modules/transferApplications'
import type { BusinessModuleNode, DictionaryItem } from '../../types'

interface LabelOption {
  code: string
  name: string
}

const DEFAULT_TENANT_ID = 1

const router = useRouter()
const loading = ref(false)
const filterExpanded = ref(false)

const tableRecords = ref<TransferApplicationRecordRow[]>([])
const tableFullPage = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

const periodRange = ref<[string, string] | null>(null)

const searchForm = reactive<TransferApplicationRecordQuery>({
  docBusiNo: '',
  companyProjectCode: '',
  busiModuleCode: '',
  applicant: undefined,
  applicationNumber: '',
  applicationDateRange: undefined,
  applicationStatus: '',
  carrierType: '',
  diffReasonCode: '',
  applyMethod: '',
  expressType: '',
  expressNumber: '',
  documentRecipient: undefined,
  catalogVolumeNo: ''
})

const userOptions = ref<Array<{ id: number; name: string }>>([])

const companyOptions = ref<LabelOption[]>([])
const documentOrganizationOptions = ref<LabelOption[]>([])
const carrierTypeOptions = ref<LabelOption[]>([])
const businessModuleTreeOptions = ref<ModuleQueryTreeNode[]>([])
const busiModuleFlatOptions = ref<LabelOption[]>([])
const statusOptions = ref<LabelOption[]>([])
const diffReasonOptions = ref<LabelOption[]>([])
const applyMethodOptions = ref<LabelOption[]>([])
const expressTypeOptions = ref<LabelOption[]>([])

function toLabelOptions(items: DictionaryItem[]): LabelOption[] {
  return items
    .filter((i) => i.enabledFlag === 'Y')
    .map((i) => ({ code: i.itemCode, name: i.itemName }))
}

async function tryDict(categoryCode: string): Promise<LabelOption[]> {
  try {
    const items = await fetchDictionaryItems(categoryCode)
    return toLabelOptions(items)
  } catch {
    return []
  }
}

function mergeOptions(primary: LabelOption[], fallback: LabelOption[]): LabelOption[] {
  if (primary.length) return primary
  return fallback
}

/** 与业务模块树选项一致，用于明细表展示模块名称 */
function flattenBusinessModulesToLabels(nodes: BusinessModuleNode[]): LabelOption[] {
  const out: LabelOption[] = []
  const walk = (ns: BusinessModuleNode[]) => {
    for (const n of ns) {
      const code = n.moduleCode?.trim()
      const name = (n.moduleName ?? '').trim()
      if (code) out.push({ code, name: name || code })
      if (n.children?.length) walk(n.children)
    }
  }
  walk(nodes)
  return out
}

const FALLBACK_STATUS: LabelOption[] = [
  { code: 'DRAFT', name: '草稿' },
  { code: 'SUBMITTED', name: '已提交' },
  { code: 'RUNNING', name: '处理中' },
  { code: 'APPROVED', name: '已通过' },
  { code: 'REJECTED', name: '已驳回' }
]

const FALLBACK_APPLY: LabelOption[] = [
  { code: 'DIRECT', name: '直接移交' },
  { code: 'MAIL', name: '邮寄' }
]

const FALLBACK_EXPRESS: LabelOption[] = [
  { code: 'SF', name: '顺丰' },
  { code: 'EMS', name: 'EMS' },
  { code: 'OTHER', name: '其他' }
]

const FALLBACK_DIFF: LabelOption[] = [
  { code: 'NONE', name: '无差异' },
  { code: 'QTY', name: '数量差异' },
  { code: 'CONTENT', name: '内容差异' }
]

function labelOf(options: LabelOption[], code?: string | null) {
  if (!code) return '-'
  return options.find((o) => o.code === code)?.name ?? code
}

function formatUser(id?: number | null) {
  if (id == null) return '-'
  return userOptions.value.find((u) => u.id === id)?.name ?? `用户-${id}`
}

function formatDateTime(value?: string | null) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function statusTagType(status: string) {
  switch (status) {
    case 'RUNNING':
      return 'warning'
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'DRAFT':
      return 'info'
    default:
      return 'info'
  }
}

function buildFilterPayload(): TransferApplicationRecordQuery {
  const f: TransferApplicationRecordQuery = {}
  if (searchForm.docBusiNo) f.docBusiNo = searchForm.docBusiNo.trim()
  if (searchForm.companyProjectCode) f.companyProjectCode = searchForm.companyProjectCode
  if (searchForm.busiModuleCode) f.busiModuleCode = searchForm.busiModuleCode
  if (periodRange.value?.length === 2) f.archPeriodRange = [...periodRange.value]
  if (searchForm.applicant != null) f.applicant = searchForm.applicant
  if (searchForm.applicationNumber) f.applicationNumber = searchForm.applicationNumber.trim()
  if (searchForm.applicationDateRange?.length === 2) f.applicationDateRange = [...searchForm.applicationDateRange]
  if (searchForm.applicationStatus) f.applicationStatus = searchForm.applicationStatus
  if (searchForm.carrierType) f.carrierType = searchForm.carrierType
  if (searchForm.diffReasonCode) f.diffReasonCode = searchForm.diffReasonCode
  if (searchForm.applyMethod) f.applyMethod = searchForm.applyMethod
  if (searchForm.expressType) f.expressType = searchForm.expressType
  if (searchForm.expressNumber) f.expressNumber = searchForm.expressNumber.trim()
  if (searchForm.documentRecipient != null) f.documentRecipient = searchForm.documentRecipient
  if (searchForm.catalogVolumeNo) f.catalogVolumeNo = searchForm.catalogVolumeNo.trim()
  return f
}

async function runQuery() {
  loading.value = true
  try {
    const res = await searchTransferApplicationRecords({
      filter: buildFilterPayload(),
      page: pagination.page,
      pageSize: pagination.pageSize,
      tenantid: DEFAULT_TENANT_ID
    })
    tableRecords.value = res.records
    pagination.total = res.total
  } catch (e) {
    console.error(e)
    let msg = '查询失败，请稍后重试'
    if (axios.isAxiosError(e) && e.response?.data && typeof e.response.data === 'object' && 'message' in e.response.data) {
      msg = String((e.response.data as { message?: string }).message || msg)
    } else if (e instanceof Error && e.message) {
      msg = e.message
    }
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

function reset() {
  searchForm.docBusiNo = ''
  searchForm.companyProjectCode = ''
  searchForm.busiModuleCode = ''
  periodRange.value = null
  searchForm.applicant = undefined
  searchForm.applicationNumber = ''
  searchForm.applicationDateRange = undefined
  searchForm.applicationStatus = ''
  searchForm.carrierType = ''
  searchForm.diffReasonCode = ''
  searchForm.applyMethod = ''
  searchForm.expressType = ''
  searchForm.expressNumber = ''
  searchForm.documentRecipient = undefined
  searchForm.catalogVolumeNo = ''
  pagination.page = 1
  runQuery()
}

function handleSizeChange() {
  pagination.page = 1
  runQuery()
}

function handlePageChange() {
  runQuery()
}

function goToCreate() {
  router.push('/archive-management/transfer')
}

function goEdit(applicationId: number) {
  router.push({ path: '/archive-management/transfer', query: { applicationId: String(applicationId) } })
}

async function openView(applicationId: number) {
  router.push(`/archive-management/transfer-detail/${applicationId}`)
}

function hintTodo(action: string) {
  ElMessage.info(`${action}功能开发中`)
}

function notifyColumnSetting() {
  ElMessage.info('列设置入口已保留，当前版本不展示具体列配置面板。')
}

onMounted(async () => {
  try {
    const [options, companies, moduleTree, users] = await Promise.all([
      fetchArchiveCreateOptions(),
      fetchCompanyInfos({ enabledFlag: 'Y' }),
      fetchBusinessModuleTree().catch((): BusinessModuleNode[] => []),
      fetchUsers().catch(() => [])
    ])
    carrierTypeOptions.value = (options.carrierTypes ?? []).map((c) => ({ code: c.code, name: c.name }))
    companyOptions.value = companies.map((c) => ({ code: c.companyCode, name: c.companyName }))
    documentOrganizationOptions.value = (options.documentOrganizations ?? []).map((o) => ({ code: o.code, name: o.name }))
    businessModuleTreeOptions.value = buildModuleQueryTree(moduleTree)
    busiModuleFlatOptions.value = flattenBusinessModulesToLabels(moduleTree)
    userOptions.value = (users || []).map((u: any) => ({
      id: Number(u.userId),
      name: `${u.userName || u.username || `用户-${u.userId}`}${u.employeeNo ? ` ${u.employeeNo}` : ''}`
    }))
  } catch {
    ElMessage.warning('加载基础选项失败，部分下拉为空')
  }

  const [st, diff, am, ex] = await Promise.all([
    tryDict('TRANSFER_APPLICATION_STATUS'),
    tryDict('TRANSFER_DIFF_REASON'),
    tryDict('TRANSFER_APPLY_METHOD'),
    tryDict('TRANSFER_EXPRESS_TYPE')
  ])
  statusOptions.value = mergeOptions(st, FALLBACK_STATUS)
  diffReasonOptions.value = mergeOptions(diff, FALLBACK_DIFF)
  applyMethodOptions.value = mergeOptions(am, FALLBACK_APPLY)
  expressTypeOptions.value = mergeOptions(ex, FALLBACK_EXPRESS)

  await runQuery()
})
</script>

<style scoped>
.transfer-query-page {
  display: grid;
  gap: 16px;
}

.filter-form {
  margin-bottom: 8px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
}

.filter-grid--extra {
  margin-top: 4px;
}

.filter-form :deep(.el-form-item) {
  margin-bottom: 10px;
}

.input-w180 {
  width: 180px;
}

.input-range {
  width: 240px;
}

.span-range :deep(.el-form-item__content) {
  flex-wrap: nowrap;
}

.filter-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  margin-bottom: 12px;
}

.filter-toolbar__actions {
  display: flex;
  gap: 10px;
}

.table-section {
  display: grid;
  gap: 12px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.table-toolbar__left {
  display: flex;
  gap: 10px;
}
.table-toolbar__right {
  display: flex;
  gap: 8px;
}

.loading-container {
  padding: 16px 0;
}

.transfer-list {
  display: grid;
  gap: 12px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1400px) {
  .filter-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
