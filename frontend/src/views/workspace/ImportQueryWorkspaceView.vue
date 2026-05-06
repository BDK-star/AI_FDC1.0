<template>
  <div class="workspace-page">
    <section class="filter-card">
      <div class="filter-grid">
        <div class="field">
          <label>文件名称</label>
          <el-input v-model="filters.inputFileName" placeholder="请输入文件名称" clearable />
        </div>
        <div class="field">
          <label>导入任务类型</label>
          <el-select v-model="filters.dataType" placeholder="全部" clearable>
            <el-option label="文档批量导入查询" value="DOCUMENT" />
            <el-option label="应归档数据批量导入查询" value="PENDING_ARCHIVE_QUERY" />
            <el-option label="应归档数据批量导入" value="PENDING_ARCHIVE_IMPORT" />
            <el-option label="应归档数据批量更新" value="PENDING_ARCHIVE_ADJUST" />
            <el-option label="业务模块配置导入" value="DOCUMENT_TYPE" />
            <el-option label="文档组织配置导入" value="DOCUMENT_ORGANIZATION" />
            <el-option label="归档规则配置导入" value="ARCHIVE_FLOW_RULE" />
          </el-select>
        </div>
        <div class="field">
          <label>导入状态</label>
          <el-select v-model="filters.jobStatus" placeholder="全部" clearable>
            <el-option label="成功" value="SUCCESS" />
            <el-option label="部分成功" value="PARTIAL_FAILED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </div>
        <div class="field">
          <label>导入时间</label>
          <el-date-picker
            v-model="filters.createdRange"
            type="datetimerange"
            value-format="YYYY-MM-DDTHH:mm:ss"
            range-separator="-"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </div>
      </div>
      <div class="filter-actions-bottom fdc-query-action-buttons">
        <el-button @click="resetFilters">重置</el-button>
        <el-button type="primary" :loading="loading" @click="loadHistory">查询</el-button>
      </div>
    </section>

    <section class="table-card">
      <div class="table-wrap">
      <el-table :data="history.records" border stripe height="560" empty-text="暂无导入记录">
        <el-table-column label="导入结果" width="100" align="center">
          <template #default="{ row }">
            <el-tooltip :content="row.resultArtifactDownloadable ? '结果下载' : '任务完成后可下载结果 Excel'">
              <el-button
                circle
                text
                class="action-btn"
                :disabled="!row.resultArtifactDownloadable"
                @click="downloadImportResult(row)"
              >
                <el-icon><Download /></el-icon>
              </el-button>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <div class="result-actions">
              <el-tooltip content="结果查询">
                <el-button circle text class="action-btn" @click="openResult(row)">
                  <el-icon><Search /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="重新查询">
                <el-button circle text class="action-btn" :disabled="!canRetry(row)" @click="retryImport(row)">
                  <el-icon><RefreshRight /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="失败明细（CSV）">
                <el-button circle text class="action-btn" :disabled="!canDownloadFailed(row)" @click="downloadFailed(row)">
                  <el-icon><Document /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="inputFileName" label="文件名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="导入任务类型" width="200">
          <template #default="{ row }">{{ importTaskTypeLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="导入时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.creationDate) }}</template>
        </el-table-column>
        <el-table-column label="导入状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.displayStatus || row.jobStatus)" effect="light">
              {{ statusLabel(row.displayStatus || row.jobStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inputTotal" label="导入总条数" width="110" />
        <el-table-column prop="resultTotal" label="导入成功条数" width="120" />
        <el-table-column label="运行时间" width="220">
          <template #default="{ row }">{{ runWindow(row.creationDate, row.durationMs) }}</template>
        </el-table-column>
        <el-table-column prop="durationMs" label="总耗时" width="110">
          <template #default="{ row }">{{ formatDuration(row.durationMs) }}</template>
        </el-table-column>
      </el-table>
      </div>

      <div class="pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="history.total"
          layout="total, sizes, prev, pager, next"
          :disabled="loading"
          @size-change="loadHistory"
          @current-change="loadHistory"
        />
      </div>
    </section>

    <el-drawer v-model="detailOpen" title="导入任务详情" size="520px">
      <el-descriptions v-if="detailRow" :column="1" border>
        <el-descriptions-item label="任务">{{ detailRow.jobName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(detailRow.displayStatus || detailRow.jobStatus) }}</el-descriptions-item>
        <el-descriptions-item label="文件名">{{ detailRow.inputFileName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="导入任务类型">{{ importTaskTypeLabel(detailRow) }}</el-descriptions-item>
        <el-descriptions-item label="导入总条数">{{ detailRow.inputTotal ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="导入成功条数">{{ detailRow.resultTotal ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="总耗时">{{ formatDuration(detailRow.durationMs) }}</el-descriptions-item>
        <el-descriptions-item label="失败原因">{{ detailRow.errorMessage || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="resultDialogOpen" title="查询结果" width="1280px">
      <el-table :data="importQueryResults" border stripe max-height="460">
        <el-table-column prop="businessCode" label="文档业务编码" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="goArchiveDetail(row)">{{ row.businessCode || '-' }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="companyProjectName" label="公司" min-width="160" show-overflow-tooltip />
        <el-table-column prop="archiveTypeCode" label="业务模块" width="160" show-overflow-tooltip />
        <el-table-column prop="beginPeriod" label="开始档期" width="120" />
        <el-table-column prop="endPeriod" label="结束档期" width="120" />
        <el-table-column prop="archiveDestination" label="归档地" width="120" />
        <el-table-column prop="originPlace" label="产生地" width="120" />
        <el-table-column prop="documentOrganizationCode" label="文档组织" min-width="130" />
        <el-table-column prop="archiveStatus" label="文档状态" width="120" />
        <el-table-column prop="documentName" label="文档名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="documentDate" label="文档生成日期" width="170" />
        <el-table-column prop="dutyPerson" label="归档责任人" width="120" />
        <el-table-column prop="dutyDepartment" label="文档责任部门" min-width="130" />
        <el-table-column prop="carrierTypeCode" label="载体类型" width="120" />
        <el-table-column prop="documentVisibility" label="是否可见" width="100" />
        <el-table-column prop="sourceSystem" label="系统来源" width="120" />
        <el-table-column prop="securityLevelName" label="密级" width="120" />
        <el-table-column prop="remark" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="creationDate" label="创建时间" width="170" />
        <el-table-column prop="createdBy" label="创建人" width="120" />
      </el-table>
      <template #footer>
        <el-button @click="resultDialogOpen = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Document, Download, RefreshRight, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchWorkspaceImportQueryResults,
  downloadWorkspaceImportResultFile,
  downloadWorkspaceIoFailedFile,
  getWorkspaceIoJob,
  queryWorkspaceIoJobs
} from '../../api/modules/workspaceIo'
import type { WorkspaceImportQueryResultRow, WorkspaceIoJobPage, WorkspaceIoJobSummary } from '../../types'

const loading = ref(false)
const router = useRouter()
const page = ref(1)
const pageSize = ref(10)
const history = ref<WorkspaceIoJobPage>({ records: [], total: 0, pages: 0, page: 1, pageSize: 10 })
const detailOpen = ref(false)
const detailRow = ref<WorkspaceIoJobSummary | null>(null)
const resultDialogOpen = ref(false)
const importQueryResults = ref<WorkspaceImportQueryResultRow[]>([])

const filters = reactive({
  inputFileName: '',
  dataType: '',
  jobStatus: '',
  createdRange: null as [string, string] | null
})

/** 与 jobType + dataType 一致，避免与「批量导出」等混淆（导出 jobType 为 EXPORT_QUERY，本页已排除） */
const importTaskTypeLabel = (row: WorkspaceIoJobSummary) => {
  const jt = row.jobType || ''
  const dt = row.dataType || ''
  if (jt === 'IMPORT_PENDING_ARCHIVE') {
    return '应归档数据批量导入'
  }
  if (jt === 'IMPORT_PENDING_ARCHIVE_ADJUST') {
    return '应归档数据批量更新'
  }
  if (jt === 'IMPORT_QUERY') {
    if (dt === 'DOCUMENT') return '文档批量导入查询'
    if (dt === 'PENDING_ARCHIVE_QUERY' || dt === 'PENDING_ARCHIVE') return '应归档数据批量导入查询'
    if (dt === 'DOCUMENT_TYPE') return '业务模块配置导入'
    if (dt === 'DOCUMENT_ORGANIZATION') return '文档组织配置导入'
    if (dt === 'ARCHIVE_FLOW_RULE') return '归档规则配置导入'
    return dt ? `导入任务（${dt}）` : '导入任务'
  }
  return dt || jt || '-'
}

const statusLabel = (v: string) =>
  v === 'SUCCESS'
    ? '成功'
    : v === 'PARTIAL_FAILED'
      ? '部分成功'
      : v === 'FAILED'
        ? '失败'
        : v === 'RUNNING'
          ? '处理中'
          : v === 'EXPIRED'
            ? '已过期'
            : v || '-'
const statusTagType = (v: string) =>
  v === 'SUCCESS'
    ? 'success'
    : v === 'PARTIAL_FAILED'
      ? 'warning'
      : v === 'FAILED'
        ? 'danger'
        : v === 'RUNNING'
          ? 'info'
          : v === 'EXPIRED'
            ? 'info'
            : 'info'
const formatDuration = (ms?: number) => (!ms && ms !== 0 ? '-' : ms < 1000 ? `${ms}ms` : `${Math.round(ms / 100) / 10}s`)
const formatDateTime = (value?: string | null) => {
  if (!value) return '-'
  const date = new Date(value.includes('T') ? value : value.replace(' ', 'T'))
  if (Number.isNaN(date.getTime())) return value
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const mi = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${mi}:${s}`
}

const runWindow = (created?: string, durationMs?: number) => {
  if (!created) return '-'
  const start = new Date(created.includes('T') ? created : created.replace(' ', 'T'))
  if (Number.isNaN(start.getTime())) return '-'
  const end = new Date(start.getTime() + (durationMs || 0))
  const f = (d: Date) => `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
  return `${f(start)} - ${f(end)}`
}

const canDownloadFailed = (row: WorkspaceIoJobSummary) =>
  row.jobType === 'IMPORT_QUERY'
    ? Boolean(row.failedFileCsv)
    : (row.jobStatus === 'FAILED' || row.jobStatus === 'PARTIAL_FAILED') && Boolean(row.failedFileCsv)
const canRetry = (row: WorkspaceIoJobSummary) => row.jobStatus === 'FAILED' || row.jobStatus === 'PARTIAL_FAILED'

const loadHistory = async () => {
  loading.value = true
  try {
    let dataTypeFilter = filters.dataType || undefined
    let jobTypeFilter: string | undefined
    if (filters.dataType === 'PENDING_ARCHIVE_ADJUST') {
      jobTypeFilter = 'IMPORT_PENDING_ARCHIVE_ADJUST'
      dataTypeFilter = undefined
    } else if (filters.dataType === 'PENDING_ARCHIVE_IMPORT') {
      jobTypeFilter = 'IMPORT_PENDING_ARCHIVE'
      dataTypeFilter = undefined
    } else if (filters.dataType === 'PENDING_ARCHIVE_QUERY') {
      jobTypeFilter = 'IMPORT_QUERY'
      dataTypeFilter = 'PENDING_ARCHIVE_QUERY'
    }
    history.value = await queryWorkspaceIoJobs({
      importTasksOnly: true,
      jobType: jobTypeFilter,
      inputFileName: filters.inputFileName || undefined,
      dataType: dataTypeFilter,
      jobStatus: filters.jobStatus || undefined,
      createdStart: filters.createdRange?.[0] || undefined,
      createdEnd: filters.createdRange?.[1] || undefined,
      page: page.value,
      pageSize: pageSize.value
    })
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.inputFileName = ''
  filters.dataType = ''
  filters.jobStatus = ''
  filters.createdRange = null
  page.value = 1
  void loadHistory()
}

const openDetail = async (row: WorkspaceIoJobSummary) => {
  try {
    detailRow.value = await getWorkspaceIoJob(row.jobId)
    detailOpen.value = true
  } catch (e: any) {
    ElMessage.error(e?.message || '加载详情失败')
  }
}

const openResult = async (row: WorkspaceIoJobSummary) => {
  if (row.jobType === 'IMPORT_QUERY') {
    try {
      importQueryResults.value = await fetchWorkspaceImportQueryResults(row.jobId)
      resultDialogOpen.value = true
    } catch (e: any) {
      ElMessage.error(e?.message || '加载查询结果失败')
    }
    return
  }
  await openDetail(row)
}

const goArchiveDetail = (row: WorkspaceImportQueryResultRow) => {
  const aid = Number(row.archiveId || 0)
  if (!Number.isFinite(aid) || aid <= 0) return
  const resolved = router.resolve({
    path: `/archive-management/detail/${aid}`,
    query: {
      from: 'query',
      businessCode: String(row.businessCode || ''),
      documentName: String(row.documentName || '')
    }
  })
  window.open(resolved.href, '_blank', 'noopener,noreferrer')
}

const retryImport = (row: WorkspaceIoJobSummary) => {
  ElMessage.info(`请使用原始文件重新发起导入（任务：${row.jobName}）`)
}

const downloadFailed = async (row: WorkspaceIoJobSummary) => {
  try {
    const blob = await downloadWorkspaceIoFailedFile(row.jobId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `import-failed-${row.jobId}.csv`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e?.message || '下载失败')
  }
}

const downloadImportResult = async (row: WorkspaceIoJobSummary) => {
  if (!row.resultArtifactDownloadable) return
  try {
    const blob = await downloadWorkspaceImportResultFile(row.jobId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.jobType === 'IMPORT_QUERY'
      ? `import-query-result-${row.jobId}.csv`
      : `pending-archive-import-result-${row.jobId}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } catch (e: any) {
    ElMessage.error(e?.message || '下载失败')
  }
}

onMounted(() => {
  void loadHistory()
})
</script>

<style scoped>
.workspace-page {
  min-height: 100%;
  display: grid;
  gap: 16px;
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
  box-sizing: border-box;
}
.filter-card,
.table-card {
  background: #fff;
  border: 1px solid #dbe0e6;
  border-radius: 12px;
  padding: 18px;
  box-shadow: 0 6px 22px rgba(15, 23, 42, 0.04);
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}
.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}
.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field label {
  font-size: 13px;
  color: #64748b;
}
.filter-actions-bottom {
  margin-top: 0;
}
.result-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}
.table-wrap {
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}
.action-btn {
  color: #1173d4;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
}
:deep(.el-table th.el-table__cell) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 700;
}
:deep(.el-table td.el-table__cell) {
  color: #334155;
}
:deep(.field .el-date-editor),
:deep(.field .el-select),
:deep(.field .el-input) {
  width: 100%;
}
:deep(.field .el-date-editor.el-range-editor) {
  width: 100% !important;
  min-width: 0 !important;
}
:deep(.fdc-query-action-buttons .el-button--primary) {
  --el-button-bg-color: #1173d4;
  --el-button-border-color: #1173d4;
  --el-button-hover-bg-color: #2c84de;
  --el-button-hover-border-color: #2c84de;
  --el-button-active-bg-color: #0f66bd;
  --el-button-active-border-color: #0f66bd;
}
:deep(.fdc-query-action-buttons) {
  border-top: none;
  padding-top: 0;
  margin-top: 14px;
}
:deep(.fdc-query-action-buttons .el-button:not(.el-button--primary)) {
  --el-button-bg-color: #ffffff;
  --el-button-border-color: #c4c8cf;
  --el-button-text-color: #6b7280;
  --el-button-hover-bg-color: #ffffff;
  --el-button-hover-border-color: #b8bdc6;
  --el-button-hover-text-color: #4b5563;
  --el-button-active-bg-color: #f9fafb;
  --el-button-active-border-color: #aeb4bf;
  --el-button-active-text-color: #374151;
}
@media (max-width: 1200px) {
  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 768px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }
}
</style>
