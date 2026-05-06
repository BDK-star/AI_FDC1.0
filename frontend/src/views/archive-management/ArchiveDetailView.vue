<template>
  <div class="doc-detail">
    <el-alert
      v-if="loadError"
      type="error"
      :closable="false"
      show-icon
      :title="loadError"
      class="doc-detail__load-alert"
    />
    <section class="doc-detail__title">
      <div class="doc-detail__head-row">
        <h1 class="doc-detail__biz-code">{{ headlineBusinessCode }}</h1>
        <el-button v-if="showPendingEdit" type="primary" class="doc-detail__edit-btn" @click="goPendingEdit">编辑</el-button>
      </div>
      <div v-if="detail" class="doc-detail__title-tags">
        <el-tag effect="plain" type="primary">{{ carrierTag }}</el-tag>
        <el-tag effect="plain" type="success">{{ detail.archiveStatus || '草稿' }}</el-tag>
        <el-tag effect="plain" type="danger">{{ securityLevelHeadline }}</el-tag>
      </div>
    </section>

    <el-card shadow="never" class="doc-section">
      <template #header>
        <div class="doc-section__header">
          <div class="doc-section__title">
            <el-icon><Document /></el-icon>
            <span>文档基本信息</span>
          </div>
        </div>
      </template>
      <div v-if="loading"><el-skeleton :rows="8" animated /></div>
      <div v-else class="doc-info-grid">
        <div v-for="item in basicInfoItems" :key="item.label" class="doc-info-item" :class="{ 'doc-info-item--full': item.full }">
          <div class="doc-info-item__label">{{ item.label }}</div>
          <div class="doc-info-item__value">{{ item.value || '-' }}</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="doc-section">
      <template #header>
        <div class="doc-section__header">
          <div class="doc-section__title">
            <el-icon><Collection /></el-icon>
            <span>扩展信息</span>
          </div>
          <el-button text @click="toggleSection('ext')">
            <el-icon><ArrowUp v-if="sectionOpen.ext" /><ArrowDown v-else /></el-icon>
          </el-button>
        </div>
      </template>
      <div v-show="sectionOpen.ext" class="doc-info-grid">
        <div class="doc-info-item doc-info-item--full">
          <div class="doc-info-item__value doc-muted-hint">
            扩展字段来自「业务模块配置」中维护的、应用功能含「应收」的档案扩展字段（BASIC），与国家/地区等信息一并展示。
          </div>
        </div>
        <div v-for="(item, idx) in extInfoItems" :key="`${item.label}-${idx}`" class="doc-info-item">
          <div class="doc-info-item__label">{{ item.label }}</div>
          <div class="doc-info-item__value">{{ item.value || '-' }}</div>
        </div>
        <el-empty v-if="!extInfoItems.length" description="暂无扩展信息" />
      </div>
    </el-card>

    <el-card shadow="never" class="doc-section">
      <template #header>
        <div class="doc-section__header">
          <div class="doc-section__title">
            <el-icon><Box /></el-icon>
            <span>归档信息</span>
          </div>
          <el-button text @click="toggleSection('archive')">
            <el-icon><ArrowUp v-if="sectionOpen.archive" /><ArrowDown v-else /></el-icon>
          </el-button>
        </div>
      </template>
      <div v-show="sectionOpen.archive" class="doc-info-grid">
        <div v-for="item in archiveInfoItems" :key="item.label" class="doc-info-item">
          <div class="doc-info-item__label">{{ item.label }}</div>
          <div class="doc-info-item__value">{{ item.value || '-' }}</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="doc-section">
      <template #header>
        <div class="doc-section__header">
          <div class="doc-section__title">
            <el-icon><Paperclip /></el-icon>
            <span>文档附件列表</span>
          </div>
          <div class="doc-section__actions">
            <el-button size="small" @click="downloadAll">
              <el-icon><Download /></el-icon>
              批量下载
            </el-button>
            <el-button text @click="toggleSection('attachment')">
              <el-icon><ArrowUp v-if="sectionOpen.attachment" /><ArrowDown v-else /></el-icon>
            </el-button>
          </div>
        </div>
      </template>
      <div v-show="sectionOpen.attachment">
        <el-table :data="attachmentRows" border>
          <el-table-column prop="fileName" label="文件名" min-width="260" show-overflow-tooltip />
          <el-table-column prop="attachmentTypeCode" label="附件类型" width="140" />
          <el-table-column prop="sizeText" label="大小" width="120" />
          <el-table-column prop="creationDate" label="上传时间" width="170" />
          <el-table-column prop="additionalInfo" label="补充信息" min-width="180" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <div class="attachment-op-cell">
                <el-tooltip content="预览" placement="top">
                  <el-button text @click="previewAttachment(row)">
                    <el-icon><View /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="下载" placement="top">
                  <el-button text @click="downloadAttachment(row)">
                    <el-icon><Download /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <el-card shadow="never" class="doc-section">
      <template #header>
        <div class="doc-section__header">
          <div class="doc-section__title">
            <el-icon><Clock /></el-icon>
            <span>操作日志</span>
          </div>
          <el-button text @click="toggleSection('log')">
            <el-icon><ArrowUp v-if="sectionOpen.log" /><ArrowDown v-else /></el-icon>
          </el-button>
        </div>
      </template>
      <div v-show="sectionOpen.log">
        <el-table :data="operationLogs" border empty-text="暂无操作记录">
          <el-table-column prop="operator" label="操作人" width="120" />
          <el-table-column prop="operationType" label="操作类型" width="140" />
          <el-table-column label="操作内容" min-width="260">
            <template #default="{ row }">
              <span v-html="row.contentHtml || row.content"></span>
            </template>
          </el-table-column>
          <el-table-column prop="operationTime" label="时间" width="170" />
          <el-table-column prop="remarks" label="备注" min-width="200" show-overflow-tooltip />
          <el-table-column label="补充附件" min-width="220">
            <template #default="{ row }">
              <template v-if="row.auditAttachments?.length">
                <el-button
                  v-for="(att, idx) in row.auditAttachments"
                  :key="idx"
                  link
                  type="primary"
                  class="log-att-btn"
                  @click="downloadLogAttachment(att)"
                >
                  {{ att.fileName || att.storageKey || '附件' }}
                </el-button>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, ArrowUp, Box, Clock, Collection, Document, Download, Paperclip, View } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchCompanyProjectCountries } from '../../api/modules/companyProject'
import http from '../../api/http'
import {
  downloadArchiveAttachment,
  downloadArchiveAttachmentsZip,
  fetchOperationAuditsByBusinessKey,
  getArchiveDetail,
  previewArchiveAttachmentUrl
} from '../../api/modules/archiveManagement'
import type { ArchiveAttachmentItem, ArchiveRecordSummary, AuditRecord, BusinessModuleExtField } from '../../types'
import { getCountryLabel } from '../base-data/companyProjectShared'
import { hardCodedExtLabelMap } from './extFieldDisplayConfig'
import {
  COMPANY_SYNC_EXT_KEYS,
  extKeyForBusinessField,
  fetchReceivableBasicExtFields
} from './pendingArchiveExtShared'

/** 临时：文档详情「扩展信息」不展示公司标签；恢复时设为 false */
const HIDE_COMPANY_TAG_IN_ARCHIVE_DETAIL_EXT = true

interface OperationLogRow {
  operator: string
  operationType: string
  content: string
  contentHtml?: string
  operationTime: string
  remarks: string
  auditAttachments?: Array<{ fileId?: number; fileName?: string; storageKey?: string; fileSize?: number }>
}

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const loadError = ref('')
const detail = ref<ArchiveRecordSummary | null>(null)
/** 与应归档创建页一致：fdc_business_module_ext_field_t BASIC + 应收 */
const receivableBusinessExtFields = ref<BusinessModuleExtField[]>([])
/** 国家编码 → 名称（来自后台公司项目国家选项；与筛选/建档下拉一致） */
const countryNameByCode = ref<Record<string, string>>({})

const sectionOpen = reactive({
  ext: true,
  archive: true,
  attachment: true,
  log: true
})

const carrierTag = computed(() => {
  if (!detail.value) return '载体类型'
  return detail.value.carrierTypeCode
})

const firstQueryValue = (q: unknown) => {
  if (q == null) return ''
  return String(Array.isArray(q) ? q[0] : q)
}

const showPendingEdit = computed(() => firstQueryValue(route.query.from) === 'pending')

const goPendingEdit = () => {
  const fromQuery = firstQueryValue(route.query.docId)
  const rawPath = route.params.id
  const fromPath = typeof rawPath === 'string' ? rawPath : Array.isArray(rawPath) ? rawPath[0] : ''
  const docId = (fromQuery || fromPath || '').trim()
  if (!docId) {
    ElMessage.warning('缺少文档标识，请从应归档列表重新打开详情')
    return
  }
  router.push({
    path: `/archive-management/pending-archive/edit/${encodeURIComponent(docId)}`,
    query: { from: 'query' }
  })
}

const parsePositiveArchiveId = (v: unknown): number | null => {
  if (v == null) return null
  const raw = String(Array.isArray(v) ? v[0] : v).trim()
  if (!raw) return null
  let s = raw
  try {
    s = decodeURIComponent(raw)
  } catch {
    s = raw
  }
  if (!/^\d+$/.test(s)) return null
  const n = Number(s)
  return Number.isSafeInteger(n) && n > 0 ? n : null
}

const resolveArchiveId = (): number | null => {
  const fromPath = parsePositiveArchiveId(route.params.id)
  if (fromPath != null) return fromPath
  return parsePositiveArchiveId(route.query.docId)
}

const headlineBusinessCode = computed(() => {
  if (detail.value?.businessCode) return detail.value.businessCode
  const q = firstQueryValue(route.query.businessCode)
  return q || '-'
})

const securityLevelHeadline = computed(() => {
  const d = detail.value
  if (!d) return '内部'
  return d.securityLevelName || d.securityLevelCode || '内部'
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

const formatCountryExtValue = (raw: unknown) => {
  if (raw === null || raw === undefined) return '-'
  let s = String(raw).trim()
  if (!s) return '-'
  if (s.startsWith('[')) {
    try {
      const parsed = JSON.parse(s) as unknown
      if (Array.isArray(parsed)) {
        s = parsed.map((x) => String(x).trim()).filter(Boolean).join(',')
      } else if (parsed !== null && typeof parsed === 'object' && 'countryCode' in (parsed as object)) {
        s = String((parsed as { countryCode?: string }).countryCode ?? '').trim()
      }
    } catch {
      /* 按普通字符串继续 */
    }
  }
  const map = countryNameByCode.value
  const resolveOne = (code: string) => {
    const c = code.trim()
    if (!c) return ''
    return map[c] || getCountryLabel(c)
  }
  const parts = s.split(/[,，]/).map((p) => p.trim()).filter(Boolean)
  if (!parts.length) return '-'
  const labels = parts.map(resolveOne).filter(Boolean)
  return labels.length ? labels.join('、') : '-'
}

/** 详情接口常把 carrier_type 换成字典展示名（如 ELECTRONIC →「电子件」），不能仅判断英文编码 */
const isPureElectronicCarrier = (carrier: string | undefined) => {
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

const basicInfoItems = computed(() => {
  if (!detail.value) return []
  const lifecycle = String(detail.value.lifecycleStatus || detail.value.archiveStatus || '').trim()
  return [
    { label: '文档类型', value: detail.value.documentTypeName || detail.value.documentTypeCode },
    { label: '文档业务编码', value: detail.value.businessCode },
    { label: '公司', value: detail.value.companyProjectName || detail.value.companyProjectCode },
    { label: '业务模块', value: detail.value.archiveTypeCode },
    { label: '开始档期', value: detail.value.beginPeriod },
    { label: '结束档期', value: detail.value.endPeriod },
    { label: '归档地', value: detail.value.archiveDestination },
    { label: '产生地', value: detail.value.originPlace },
    { label: '文档名称', value: detail.value.documentName, full: true },
    { label: '文档生成日期', value: formatDateTime(detail.value.documentDate) },
    { label: '归档责任人', value: detail.value.dutyPerson },
    { label: '文档责任部门', value: detail.value.dutyDepartment },
    { label: '载体类型', value: detail.value.carrierTypeCode },
    { label: '系统来源', value: detail.value.sourceSystem },
    { label: '密级', value: detail.value.securityLevelName || detail.value.securityLevelCode },
    { label: '文档生命周期状态', value: lifecycle || '-' },
    { label: '创建时间', value: formatDateTime(detail.value.lastUpdateDate) },
    { label: '创建人', value: detail.value.createdBy || '-' },
    { label: '描述', value: detail.value.remark, full: true }
  ]
})

const extInfoItems = computed(() => {
  if (!detail.value) return []
  const ext = { ...(detail.value.extValues || {}) }
  const formatExtCell = (v: unknown) => {
    if (v === null || v === undefined) return '-'
    const s = String(v).trim()
    return s === '' ? '-' : s
  }
  const items: { label: string; value: string }[] = []
  const hideCompanyTag = HIDE_COMPANY_TAG_IN_ARCHIVE_DETAIL_EXT
  for (const key of COMPANY_SYNC_EXT_KEYS) {
    if (hideCompanyTag && key === 'companyTag') continue
    const value = key === 'country' ? formatCountryExtValue(ext[key]) : formatExtCell(ext[key])
    items.push({
      label: hardCodedExtLabelMap[key] || key,
      value
    })
  }
  for (const f of receivableBusinessExtFields.value) {
    const key = extKeyForBusinessField(f)
    if (!key) continue
    if (hideCompanyTag && key === 'companyTag') continue
    const raw = ext[key]
    let value: string
    if (f.dataType === 'DATE' || f.dataType === 'DATETIME') {
      value = raw != null && String(raw).trim() !== '' ? formatDateTime(raw) : '-'
    } else {
      value = key === 'country' ? formatCountryExtValue(raw) : formatExtCell(raw)
    }
    items.push({
      label: f.fieldName || key,
      value
    })
  }
  return items
})

const archiveInfoItems = computed(() => {
  if (!detail.value) return []
  const d = detail.value
  const ext = d.extValues || {}
  const cell = (v: unknown) => {
    if (v === null || v === undefined) return '-'
    const s = String(v).trim()
    return s === '' ? '-' : s
  }
  const extCell = (...keys: string[]) => {
    for (const key of keys) {
      const v = (ext as Record<string, unknown>)[key]
      const s = String(v ?? '').trim()
      if (s) return s
    }
    return '-'
  }
  const extDateCell = (...keys: string[]) => {
    for (const key of keys) {
      const v = (ext as Record<string, unknown>)[key]
      const s = String(v ?? '').trim()
      if (s) return formatDateTime(s)
    }
    return '-'
  }
  const docOrg = { label: '文档组织', value: cell(d.documentOrganizationCode) }
  const archiveType = { label: '档案类型', value: cell(d.archiveTypeCode) }
  const visibility = { label: '是否可见', value: cell(d.documentVisibility ?? ext.visibility ?? '是') }
  const retentionYears = {
    label: '保存期限（年）',
    value:
      d.retentionPeriodYears != null && Number.isFinite(d.retentionPeriodYears)
        ? String(d.retentionPeriodYears)
        : '-'
  }
  const custodyStatus = { label: '保管状态', value: cell(d.custodyStatus) }
  const copies = { label: '份数', value: isPureElectronicCarrier(d.carrierTypeCode) ? '-' : extCell('copies') }
  const barcodeModule = { label: '条码模块', value: extCell('barcodeModule') }
  const isElectronic = isPureElectronicCarrier(d.carrierTypeCode)
  const isUnarchivedValue = (v: unknown) => {
    const raw = String(v ?? '').trim()
    if (!raw) return false
    const upper = raw.toUpperCase()
    return upper === 'UNARCHIVED' || raw === '未归档'
  }
  const isUnarchivedLifecycle = isUnarchivedValue(d.lifecycleStatus) || isUnarchivedValue(d.archiveStatus)

  if (isUnarchivedLifecycle) {
    return [docOrg, archiveType, visibility, retentionYears, custodyStatus, copies, barcodeModule]
  }

  return [
    docOrg,
    archiveType,
    visibility,
    retentionYears,
    custodyStatus,
    copies,
    barcodeModule,
    { label: '档案条码', value: isElectronic ? '-' : extCell('archiveBarcodeRange', 'archiveBarcode') },
    { label: '签收日期', value: isElectronic ? '-' : extDateCell('signDate') },
    { label: '签收人', value: isElectronic ? '-' : extCell('signedBy') },
    { label: '核销日期', value: isElectronic ? '-' : extDateCell('verificationDateRange', 'verificationDate') },
    { label: '核销人', value: isElectronic ? '-' : extCell('verifiedBy', 'verificationBy') },
    { label: '文档编号', value: isElectronic ? '-' : extCell('volumeSeqNo') },
    { label: '册条码', value: isElectronic ? '-' : extCell('volumeBarcodeRange', 'volumeBarcode') },
    { label: '成册日期', value: isElectronic ? '-' : extDateCell('volumizationDateRange', 'volumizationDate') },
    { label: '成册人', value: isElectronic ? '-' : extCell('assembledBy') },
    { label: '册号', value: isElectronic ? '-' : extCell('volumeNoRange', 'volumeNo') },
    { label: '库房', value: isElectronic ? '-' : cell(d.currentWarehouseCode) },
    { label: '库位', value: isElectronic ? '-' : cell(d.currentLocationCode) },
    { label: '入库日期', value: isElectronic ? '-' : extDateCell('storageDateRange', 'storageDate') },
    { label: '入库人', value: isElectronic ? '-' : extCell('storedBy') },
    { label: '剩余份数', value: isElectronic ? '-' : extCell('remainingCopies') }
  ]
})

const attachmentRows = computed(() => {
  return (detail.value?.attachments || []).map((item) => ({
    ...item,
    sizeText: formatFileSize(item.fileSize),
    additionalInfo: item.edmId || '-'
  }))
})

const MODULE_PENDING_ARCHIVE = 'PENDING_ARCHIVE'

const operationLogs = ref<OperationLogRow[]>([])

const auditRemarks = (a: AuditRecord): string => {
  const raw = a.operationRemark ?? (a as { operation_remark?: string }).operation_remark
  if (raw != null && String(raw).trim()) {
    return String(raw).trim()
  }
  return ''
}

const escapeHtml = (text: string): string =>
  text
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')

const formatOperationContentHtml = (content: string): string => {
  if (!content?.trim()) return '-'
  const segments = content.split('；').map((s) => s.trim()).filter(Boolean)
  const rendered = segments.map((segment) => {
    const matched = segment.match(/^将(.+?)字段值由(.+?)修改为(.+)$/)
    if (!matched) {
      return escapeHtml(segment)
    }
    const [, field, fromVal, toVal] = matched
    return `将<u>${escapeHtml(field)}</u>字段值由<u>${escapeHtml(fromVal)}</u>修改为<u>${escapeHtml(toVal)}</u>`
  })
  return rendered.join('；')
}

const loadPendingArchiveOperationLogs = async (record: ArchiveRecordSummary) => {
  operationLogs.value = []
  const aid = record.archiveId
  if (aid == null || !Number.isFinite(Number(aid)) || Number(aid) <= 0) return
  try {
    const rows = await fetchOperationAuditsByBusinessKey(MODULE_PENDING_ARCHIVE, String(aid))
    operationLogs.value = rows
      .filter((a) => (a.operationType || '').trim().toUpperCase() !== 'DRAFT_SAVE')
      .map((a) => ({
      operator: a.operatorName || '-',
      operationType: a.operationTypeName || a.operationType || '-',
      content: a.operationSummary || '-',
      contentHtml: formatOperationContentHtml(a.operationSummary || '-'),
      operationTime: formatDateTime(a.operationTime),
      remarks: auditRemarks(a),
      auditAttachments: a.auditAttachments
      }))
  } catch {
    operationLogs.value = []
  }
}

const toggleSection = (key: keyof typeof sectionOpen) => {
  sectionOpen[key] = !sectionOpen[key]
}

const formatFileSize = (size?: number) => {
  if (!size || size <= 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

const buildMockDetail = (): ArchiveRecordSummary => {
  const businessCode = String(route.query.businessCode || route.params.id || '')
  const documentName = String(route.query.documentName || `文档 ${businessCode}`)
  const docId = String(route.query.docId || businessCode)
  return {
    archiveId: 0,
    archiveCode: docId,
    documentTypeCode: 'ACCOUNTING',
    documentTypeName: '会计档案',
    companyProjectCode: 'SH',
    companyProjectName: '上海总部',
    beginPeriod: '2023-10',
    endPeriod: '2023-10',
    documentName,
    businessCode,
    dutyPerson: '李明',
    dutyDepartment: '财务管理部',
    documentDate: '2023-10-24',
    securityLevelCode: 'INTERNAL',
    securityLevelName: '内部',
    sourceSystem: 'ERP',
    archiveDestination: '中国-上海',
    originPlace: '中国-上海',
    carrierTypeCode: 'ELECTRONIC',
    remark: '演示详情数据',
    bindVolumeCode: '',
    currentWarehouseCode: 'WH-01',
    currentLocationCode: 'A-01-02',
    aiArchiveSummary: '',
    documentOrganizationCode: '财务共享中心',
    retentionPeriodYears: 10,
    archiveTypeCode: 'AP',
    archiveStatus: '未归档',
    parseStatus: 'SUCCESS',
    vectorStatus: 'READY',
    lastUpdateDate: '2023-10-24 10:00:00',
    attachmentCount: 1,
    extValues: {
      barcodeModule: 'TX-AP-2023',
      copies: '1',
      remainingCopies: '1',
      visibility: '是'
    },
    attachments: [
      {
        attachmentId: 1,
        attachmentRole: 'ELECTRONIC',
        attachmentTypeCode: 'VOUCHER',
        edmId: 'EDM_DEMO_001',
        fileName: '会计凭证.pdf',
        mimeType: 'application/pdf',
        fileSize: 2457600,
        remark: '',
        aiSummary: '',
        parseStatus: 'SUCCESS',
        vectorStatus: 'READY',
        creationDate: '2023-10-24 10:00:00'
      } as ArchiveAttachmentItem
    ]
  }
}

const loadDetail = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const id = resolveArchiveId()
    if (id == null) {
      throw new Error('无效的文档 ID，请从列表重新打开详情')
    }
    const [record, countries] = await Promise.all([
      getArchiveDetail(id),
      fetchCompanyProjectCountries().catch(() => [])
    ])
    detail.value = record
    countryNameByCode.value = Object.fromEntries((countries || []).map((c) => [c.countryCode, c.countryName]))
    receivableBusinessExtFields.value = []
    const mc = record.businessModuleTypeCode?.trim()
    if (mc) {
      try {
        receivableBusinessExtFields.value = await fetchReceivableBasicExtFields(mc)
      } catch {
        receivableBusinessExtFields.value = []
      }
    }
    if (detail.value) {
      await loadPendingArchiveOperationLogs(detail.value)
    }
  } catch (e: any) {
    const msg = e?.message || '加载文档详情失败'
    loadError.value = msg
    ElMessage.error(msg)
    detail.value = null
    operationLogs.value = []
  } finally {
    loading.value = false
  }
}

const APP_TAB_TITLE = '档案智能工作台'

watch(
  () => [route.fullPath, route.name] as const,
  () => {
    if (route.name !== 'archive-management-detail') return
    const isPending = firstQueryValue(route.query.from) === 'pending'
    document.title = isPending ? `应归档数据详情 - ${APP_TAB_TITLE}` : `文档详情 - ${APP_TAB_TITLE}`
  },
  { immediate: true }
)

watch(
  () => [route.params.id, route.query.docId] as const,
  () => {
    if (route.name !== 'archive-management-detail') return
    void loadDetail()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  document.title = APP_TAB_TITLE
})

const saveBlob = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName
  a.click()
  window.URL.revokeObjectURL(url)
}

const previewAttachment = (row: any) => {
  const attachmentId = Number(row?.attachmentId)
  if (!Number.isFinite(attachmentId) || attachmentId <= 0) {
    ElMessage.warning('无效附件')
    return
  }
  const url = previewArchiveAttachmentUrl(attachmentId)
  window.open(url, '_blank', 'noopener,noreferrer')
}

const downloadAttachment = async (row: any) => {
  const attachmentId = Number(row?.attachmentId)
  if (!Number.isFinite(attachmentId) || attachmentId <= 0) {
    ElMessage.warning('无效附件')
    return
  }
  try {
    const blob = await downloadArchiveAttachment(attachmentId)
    const name = String(row?.fileName || 'attachment')
    saveBlob(blob, name)
  } catch {
    ElMessage.error('下载失败')
  }
}

const downloadAll = async () => {
  const archiveId = Number(detail.value?.archiveId)
  if (!Number.isFinite(archiveId) || archiveId <= 0) {
    ElMessage.warning('无可下载附件')
    return
  }
  try {
    const blob = await downloadArchiveAttachmentsZip(archiveId)
    saveBlob(blob, `archive-${archiveId}-attachments.zip`)
  } catch (e: any) {
    ElMessage.error(e?.message || '批量下载失败')
  }
}

const downloadLogAttachment = async (att: { fileId?: number; storageKey?: string; fileName?: string }) => {
  const fid = att.fileId
  const sk = att.storageKey?.trim()
  if ((fid == null || !Number.isFinite(Number(fid)) || Number(fid) <= 0) && !sk) {
    ElMessage.warning('无效附件')
    return
  }
  try {
    const res = await http.get('/api/archive-management/pending-documents/audit-attachments/download', {
      params: fid != null && Number(fid) > 0 ? { fileId: fid } : { storageKey: sk },
      responseType: 'blob'
    })
    const blob = res.data as Blob
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = att.fileName?.trim() || 'attachment'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  }
}

</script>

<style scoped>
.doc-detail {
  display: grid;
  gap: 16px;
}
.doc-detail__load-alert {
  margin-bottom: 0;
}
.doc-detail__title {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}
.doc-detail__head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}
.doc-detail__biz-code {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
  word-break: break-all;
  flex: 1 1 auto;
  min-width: 0;
}
.doc-detail__edit-btn {
  flex-shrink: 0;
  align-self: center;
}
.doc-detail__title-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.doc-section {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}
.doc-section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.doc-section__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 700;
  color: #1e293b;
}
.doc-section__actions {
  display: flex;
  gap: 8px;
  align-items: center;
}
.doc-info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 20px;
}
.doc-info-item {
  display: flex;
  border-bottom: 1px solid #f1f5f9;
  padding: 8px 0;
}
.doc-info-item--full {
  grid-column: span 3;
}
.doc-info-item__label {
  width: 120px;
  color: #64748b;
  font-size: 14px;
  flex-shrink: 0;
}
.doc-info-item__value {
  color: #1f2937;
  font-size: 14px;
  word-break: break-all;
}
.log-att-btn {
  margin-right: 8px;
}
.attachment-op-cell {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}
@media (max-width: 1200px) {
  .doc-info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .doc-info-item--full {
    grid-column: span 2;
  }
}
@media (max-width: 768px) {
  .doc-detail__head-row {
    flex-wrap: wrap;
  }
  .doc-detail__edit-btn {
    margin-left: auto;
  }
  .doc-info-grid {
    grid-template-columns: 1fr;
  }
  .doc-info-item--full {
    grid-column: span 1;
  }
}
</style>
