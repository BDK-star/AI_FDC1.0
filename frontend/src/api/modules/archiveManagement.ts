import http, { apiRequest, getApiBaseUrl, type ApiResponse } from '../http'
import { CURRENT_OPERATOR_USER_ID } from '../../constants/currentUser'
import type {
  ArchiveAskResult,
  ArchiveAiModelSummary,
  ArchiveCreateOptions,
  ArchiveCreateSession,
  ArchiveDefaultResolve,
  ArchiveQueryResult,
  ArchiveRecordSummary,
  AuditRecord,
  BindBatch,
  BindOptions,
  BindPreviewResult,
  DocumentTypeExtField,
  FourAttrInspectionConfig,
  FourAttrInspectionDetail,
  LabelValueOption,
  StorageBatch,
  StorageLedger,
  StorageOptions,
  StorageQueryResult,
  WorkspaceIoJobSummary
} from '../../types'

export interface DocumentTypeExtFieldCreateCommand {
  usageModule: string
  relatedModuleCode: string
  relatedField: string
  fieldName: string
  fieldType: 'TEXT' | 'DICT'
  dictCategoryCode?: string
  requiredFlag: 'Y' | 'N'
  enabledFlag: 'Y' | 'N'
  formSortOrder: number
  queryEnabledFlag: 'Y' | 'N'
  querySortOrder: number
}

export interface ArchiveCreateCommand {
  sessionCode?: string
  createMode?: 'AUTO' | 'MANUAL'
  busiModuleCode: string
  companyProjectCode: string
  beginPeriod: string
  endPeriod: string
  businessCode?: string
  documentName: string
  dutyPerson: string
  dutyPersonId?: number
  dutyDepartment: string
  documentDate: string
  securityLevelCode: string
  sourceSystem?: string
  archiveDestination?: string
  originPlace?: string
  carrierTypeCode: 'ELECTRONIC' | 'PAPER' | 'HYBRID'
  remark?: string
  aiArchiveSummary?: string
  documentOrganizationCode: string
  retentionPeriodYears: number
  archiveTypeCode: string
  countryCode?: string
  customRule?: string
  extValues?: Record<string, string>
  paperInfo?: {
    plannedCopyCount?: number
    actualCopyCount?: number
    remark?: string
  }
}

export interface ArchiveQueryCommand {
  keyword?: string
  /** 兼容后端别名：按文档类型过滤（后端映射到 busiModuleCode） */
  documentTypeCode?: string
  busiModuleCode?: string
  companyProjectCode?: string
  archiveTypeCode?: string
  /** 多个业务模块编码（或）；与 archiveTypeCode 并存时以后端合并为准 */
  archiveTypeCodes?: string[]
  carrierTypeCode?: string
  carrierTypeCodes?: string[]
  securityLevelCode?: string
  beginPeriod?: string
  endPeriod?: string
  documentName?: string
  businessCode?: string
  dutyPerson?: string
  archiveDestination?: string
  sourceSystem?: string
  documentOrganizationCode?: string
  extFilters?: Record<string, string>
  excludeSubmittedTransferApplied?: boolean
  /** 条码模块编码，多选为「或」 */
  barcodeModuleCodes?: string[]
}

export interface ArchiveAskCommand {
  question: string
  busiModuleCode?: string
  companyProjectCode?: string
}

export interface PendingDocumentQueryCommand {
  documentTypeCode?: string
  companyCode?: string
  archiveTypeCode?: string
  archiveTypeCodes?: string[]
  carrierType?: string
  carrierTypes?: string[]
  businessCode?: string
  /** 多条业务编码（优先于 businessCode 文本）；避免 JSON 内换行在传输中丢失 */
  businessCodes?: string[]
  /** 多个值空格分隔，与 refNo、businessCode 同时存在时取交集 */
  invoiceNo?: string
  refNo?: string
  /** 多条其他相关编号（优先于 refNo 文本） */
  refNos?: string[]
  docOrganization?: string
  beginPeriod?: string
  endPeriod?: string
  docGenerationStart?: string
  docGenerationEnd?: string
  custodyStatus?: string
  country?: string
  repOffice?: string
  region?: string
  dutyPerson?: string
  /** 与后端登录用户 id 对齐；未传则不按创建人过滤 */
  createdByUserId?: number
  /** 条码模块编码，多选为「或」；仅筛选业务模块已映射到所选条码模块的文档 */
  barcodeModuleCodes?: string[]
  /** 档案类型编码（arch_type_code / ext.archiveType），多选为「或」 */
  documentArchiveTypeCodes?: string[]
  /** 与归档规则管理一致：国家/省/市级联叶子编码（fdc_document_t.arch_place_alpha2_code） */
  archiveDestination?: string
  /** 国家维表编码（fdc_document_t.origin_place_alpha2_code） */
  originPlace?: string
}

export interface PendingDocumentRowResponse {
  docId: string
  businessCode: string
  companyEntity: string
  businessModule: string
  startPeriod: string
  endPeriod: string
  archivePlace: string
  originPlace: string
  docOrganization: string
  docStatus: string
  documentName: string
  docGenerationDate: string
  owner: string
  responsibleDept: string
  carrierType: string
  visibility: string
  sourceSystem: string
  securityLevelCode: string
  securityLevelName?: string
  /** 列表展示用，等同 securityLevelName */
  securityLevel: string
  description: string
  creationTime: string
  createdBy: string
  updatedBy: string
  updatedAt: string
}

export interface ArchiveTransferCommand {
  archiveIds: number[]
  assigneeId: string
  assigneeName?: string
  transferMethod: 'DIRECT' | 'MAIL'
  logisticsCompany?: string
  trackingNumber?: string
  remark?: string
  initiatorId?: string
  initiatorName?: string
}

export interface ArchiveTransferResponse {
  businessKey: string
  processInstanceId: string
  workflowInstanceId: number
  archiveCount: number
}

export interface BindPreviewCommand {
  bindMode: 'BUSINESS_CODE' | 'PERIOD' | 'MANUAL'
  keyword?: string
  busiModuleCode?: string
  companyProjectCode?: string
  archiveIds?: number[]
}

export interface BindCreateCommand {
  bindMode: 'BUSINESS_CODE' | 'PERIOD' | 'MANUAL'
  bindRemark?: string
  volumes: Array<{
    volumeTitle?: string
    bindRuleKey?: string
    carrierTypeCode?: string
    remark?: string
    items: Array<{
      archiveId: number
      sortNo?: number
      primaryFlag?: 'Y' | 'N'
      bindReason?: string
    }>
  }>
}

export interface BindQueryCommand {
  bindMode?: string
  bindStatus?: string
  keyword?: string
}

export interface StorageQueryCommand {
  sourceBindBatchCode?: string
  keyword?: string
}

export interface StorageCreateCommand {
  sourceType: 'BIND_GUIDED' | 'DIRECT'
  sourceBindBatchCode?: string
  warehouseCode: string
  remark?: string
  items: Array<{
    itemType: 'VOLUME' | 'ARCHIVE'
    volumeId?: number
    archiveId?: number
    locationCode: string
  }>
}

export interface StorageLedgerQueryCommand {
  storageBatchCode?: string
  bindVolumeCode?: string
  archiveCode?: string
  warehouseCode?: string
  locationCode?: string
  resultStatus?: string
}

/** 文档类型扩展字段：与后端 DocumentTypeExtFieldController 的 /api/archive-manage/business-modules/... 一致 */
function documentTypeExtFieldPath(busiModuleCode: string) {
  return `/api/archive-manage/business-modules/${busiModuleCode}/ext-fields`
}

export function fetchDocumentTypeExtFields(busiModuleCode: string) {
  return apiRequest<DocumentTypeExtField[]>(http.get(documentTypeExtFieldPath(busiModuleCode)))
}

export function fetchEffectiveDocumentTypeExtFields(busiModuleCode: string) {
  return apiRequest<DocumentTypeExtField[]>(http.get(`${documentTypeExtFieldPath(busiModuleCode)}/effective`))
}

export function createDocumentTypeExtField(busiModuleCode: string, data: DocumentTypeExtFieldCreateCommand) {
  return apiRequest<DocumentTypeExtField>(http.post(documentTypeExtFieldPath(busiModuleCode), data))
}

export function updateDocumentTypeExtField(busiModuleCode: string, fieldCode: string, data: DocumentTypeExtFieldCreateCommand) {
  return apiRequest<DocumentTypeExtField>(http.put(`${documentTypeExtFieldPath(busiModuleCode)}/${fieldCode}`, data))
}

export function deleteDocumentTypeExtField(busiModuleCode: string, fieldCode: string) {
  return apiRequest<void>(http.delete(`${documentTypeExtFieldPath(busiModuleCode)}/${fieldCode}`))
}

export function fetchArchiveCreateOptions() {
  return apiRequest<ArchiveCreateOptions>(http.get('/api/archive-management/create/options'))
}

export function resolveArchiveDefaults(params: {
  companyProjectCode: string
  busiModuleCode: string
  customRule?: string
  archiveDestination?: string
}) {
  return apiRequest<ArchiveDefaultResolve>(http.get('/api/archive-management/create/defaults', { params }))
}

export function createArchiveSession(createMode: 'AUTO' | 'MANUAL') {
  return apiRequest<ArchiveCreateSession>(http.post('/api/archive-management/create/sessions', { createMode }))
}

export function fetchArchiveSession(sessionCode: string) {
  return apiRequest<ArchiveCreateSession>(http.get(`/api/archive-management/create/sessions/${sessionCode}`))
}

export async function uploadArchiveAttachment(params: {
  sessionCode: string
  attachmentRole: 'ELECTRONIC' | 'PAPER_SCAN'
  attachmentTypeCode?: string
  remark?: string
  file: File
}) {
  const formData = new FormData()
  formData.append('attachmentRole', params.attachmentRole)
  if (params.attachmentTypeCode) formData.append('attachmentTypeCode', params.attachmentTypeCode)
  if (params.remark) formData.append('remark', params.remark)
  formData.append('file', params.file)
  return apiRequest(http.post(`/api/archive-management/create/sessions/${params.sessionCode}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })) as Promise<any>
}

export function updateArchiveAttachment(params: {
  sessionCode: string
  attachmentId: number
  attachmentTypeCode?: string
  remark?: string
  aiSummary?: string
}) {
  return apiRequest(http.put(`/api/archive-management/create/sessions/${params.sessionCode}/attachments/${params.attachmentId}`, {
    attachmentTypeCode: params.attachmentTypeCode,
    remark: params.remark,
    aiSummary: params.aiSummary
  })) as Promise<any>
}

export function createArchive(data: ArchiveCreateCommand) {
  return apiRequest<ArchiveRecordSummary>(http.post('/api/archive-management/create/archives', data))
}

export function queryArchives(data: ArchiveQueryCommand) {
  return apiRequest<ArchiveQueryResult>(http.post('/api/archive-management/create/query', data))
}

export function askArchiveQuestion(data: ArchiveAskCommand) {
  return apiRequest<ArchiveAskResult>(http.post('/api/archive-management/create/ask', data))
}

export function queryPendingDocuments(data: PendingDocumentQueryCommand) {
  return apiRequest<PendingDocumentRowResponse[]>(http.post('/api/archive-management/pending-documents/query', data))
}

export interface PendingAuditAttachmentRef {
  fileId: number
  fileName?: string
  storageKey?: string
  fileSize?: number
}

export interface PendingDocumentWriteCommand {
  operatorUserId?: number
  documentTypeCode: string
  companyProjectCode: string
  archiveTypeCode: string
  businessCode?: string
  beginPeriod: string
  endPeriod?: string
  archiveDestination?: string
  originPlace?: string
  documentName: string
  documentDate: string
  dutyPerson: string
  dutyPersonId?: number
  dutyDepartment?: string
  carrierTypeCode: string
  sourceSystem?: string
  securityLevelCode: string
  remark?: string
  documentOrganizationCode: string
  retentionPeriodYears?: number
  custodyStatus?: string
  /** SUBMIT（默认）| DRAFT */
  submitMode?: 'SUBMIT' | 'DRAFT'
  operationRemark?: string
  operationTypeCode?: 'CREATE' | 'UPDATE' | 'DRAFT_SAVE' | 'ATTACH_INTEGRATE' | 'BATCH_CREATE' | 'BATCH_UPDATE'
  auditAttachments?: PendingAuditAttachmentRef[]
  extValues?: Record<string, string>
}

export interface PendingDocumentExportCommand {
  /** 字符串数组，避免大整数 docId 经 JSON number 精度丢失导致导出无数据行 */
  docIds: string[]
  exportFileFormat?: 'CSV' | 'EXCEL' | 'PDF'
  exportScope?: 'DOCUMENT_QUERY' | 'PENDING_ARCHIVE'
}

export function createPendingDocument(data: PendingDocumentWriteCommand) {
  return apiRequest<ArchiveRecordSummary>(http.post('/api/archive-management/pending-documents', data))
}

export function updatePendingDocument(docId: number, data: PendingDocumentWriteCommand) {
  return apiRequest<ArchiveRecordSummary>(http.put(`/api/archive-management/pending-documents/${docId}`, data))
}

export function batchDeletePendingDocuments(docIds: number[]) {
  return apiRequest<void>(http.post('/api/archive-management/pending-documents/batch-delete', { docIds }))
}

export function duplicatePendingDocument(docId: number) {
  return apiRequest<ArchiveRecordSummary>(http.post(`/api/archive-management/pending-documents/${docId}/duplicate`, {}))
}

export function createPendingDocumentsExportJob(data: PendingDocumentExportCommand) {
  return apiRequest<WorkspaceIoJobSummary>(http.post('/api/archive-management/pending-documents/export-jobs', data))
}

/** 批量导出（同步下载 CSV，带 BOM，不经过「我的导出」任务） */
export async function downloadPendingDocumentsCsv(data: PendingDocumentExportCommand): Promise<void> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (CURRENT_OPERATOR_USER_ID != null && CURRENT_OPERATOR_USER_ID > 0) {
    headers['X-User-Id'] = String(CURRENT_OPERATOR_USER_ID)
  }
  const base = getApiBaseUrl()
  const exportUrl = `${base}/api/archive-management/pending-documents/export-csv`
  if (base && /ngrok/i.test(base)) {
    headers['ngrok-skip-browser-warning'] = 'true'
  }
  const res = await fetch(exportUrl, {
    method: 'POST',
    headers,
    body: JSON.stringify(data)
  })
  if (!res.ok) {
    const text = await res.text()
    let msg = text
    try {
      const j = JSON.parse(text) as { msg?: string; message?: string }
      msg = (j.msg || j.message || text).trim() || `HTTP ${res.status}`
    } catch {
      msg = text.trim() || `HTTP ${res.status}`
    }
    throw new Error(msg)
  }
  const buf = await res.arrayBuffer()
  const blob = new Blob([buf], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  const scopeLabel = data.exportScope === 'PENDING_ARCHIVE' ? '应归档数据' : '文档查询'
  const ts = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')
  a.download = `${scopeLabel}导出-${ts}.csv`
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

/** 服务端生成的应归档批量创建模板 CSV（表头+示例行），与前端规则一致，可避免浏览器缓存旧脚本 */
export function fetchPendingBatchImportTemplateCsv(params: {
  documentTypeCode: string
  companyProjectCode?: string
  archiveTypeCode?: string
  documentTypeName?: string
}) {
  const search = new URLSearchParams()
  search.set('documentTypeCode', params.documentTypeCode)
  if (params.companyProjectCode) search.set('companyProjectCode', params.companyProjectCode)
  if (params.archiveTypeCode) search.set('archiveTypeCode', params.archiveTypeCode)
  if (params.documentTypeName) search.set('documentTypeName', params.documentTypeName)
  return http
    .get<string>(`/api/archive-management/pending-documents/batch-import-template?${search.toString()}`, {
      responseType: 'text',
      transformResponse: [(data) => data as string]
    })
    .then((r) => r.data)
}

export function submitPendingArchiveBatchImport(params: {
  file: File
  documentTypeCode: string
  operationRemark?: string
  auditAttachments?: PendingAuditAttachmentRef[]
}) {
  const form = new FormData()
  form.append('file', params.file)
  form.append('documentTypeCode', params.documentTypeCode)
  if (params.operationRemark) {
    form.append('operationRemark', params.operationRemark)
  }
  if (params.auditAttachments?.length) {
    form.append('auditAttachmentsJson', JSON.stringify(params.auditAttachments))
  }
  return apiRequest<WorkspaceIoJobSummary>(
    http.post('/api/archive-management/pending-documents/batch-import', form)
  )
}

/** 应归档批量更新：模板与批量创建一致，按文档业务编码+公司+业务模块+开始档期定位正式未归档文档后更新 */
export function submitPendingArchiveBatchAdjust(params: {
  file: File
  documentTypeCode: string
  operationRemark?: string
  auditAttachments?: PendingAuditAttachmentRef[]
}) {
  const form = new FormData()
  form.append('file', params.file)
  form.append('documentTypeCode', params.documentTypeCode)
  if (params.operationRemark) {
    form.append('operationRemark', params.operationRemark)
  }
  if (params.auditAttachments?.length) {
    form.append('auditAttachmentsJson', JSON.stringify(params.auditAttachments))
  }
  return apiRequest<WorkspaceIoJobSummary>(
    http.post('/api/archive-management/pending-documents/batch-import-adjust', form)
  )
}

export function submitArchiveImportQueryJob(params: {
  file: File
  documentTypeCode: string
}) {
  const form = new FormData()
  form.append('file', params.file)
  form.append('documentTypeCode', params.documentTypeCode)
  return apiRequest<WorkspaceIoJobSummary>(
    http.post('/api/archive-management/archives/import-query-jobs', form)
  )
}

export function submitPendingImportQueryJob(params: {
  file: File
  documentTypeCode: string
}) {
  const form = new FormData()
  form.append('file', params.file)
  form.append('documentTypeCode', params.documentTypeCode)
  return apiRequest<WorkspaceIoJobSummary>(
    http.post('/api/archive-management/pending-documents/import-query-jobs', form)
  )
}

export async function downloadArchiveAttachment(attachmentId: number): Promise<Blob> {
  const res = await http.get(`/api/archive-management/attachments/${attachmentId}/download`, { responseType: 'blob' })
  return res.data as Blob
}

export function previewArchiveAttachmentUrl(attachmentId: number): string {
  const base = getApiBaseUrl()
  const path = `/api/archive-management/attachments/${attachmentId}/preview`
  return base ? `${base}${path}` : path
}

export async function downloadArchiveAttachmentsZip(archiveId: number): Promise<Blob> {
  const res = await http.get(`/api/archive-management/archives/${archiveId}/attachments/download-all`, { responseType: 'blob' })
  return res.data as Blob
}

export async function uploadPendingAuditAttachment(file: File): Promise<PendingAuditAttachmentRef> {
  const fd = new FormData()
  fd.append('file', file)
  // 使用 fetch 避免 axios 在部分环境下把 FormData 按 JSON 处理，导致后端 consumes 不匹配并返回「POST 不支持」
  const base = getApiBaseUrl()
  const uploadUrl = `${base}/api/archive-management/pending-documents/audit-attachments`
  const uploadHeaders: Record<string, string> = { 'X-User-Id': String(CURRENT_OPERATOR_USER_ID) }
  if (base && /ngrok/i.test(base)) {
    uploadHeaders['ngrok-skip-browser-warning'] = 'true'
  }
  const res = await fetch(uploadUrl, {
    method: 'POST',
    headers: uploadHeaders,
    body: fd
  })
  const payload = (await res.json()) as ApiResponse<PendingAuditAttachmentRef>
  const isSuccess = typeof payload.code === 'number' ? payload.code === 0 : payload.success === true
  if (!isSuccess) {
    throw new Error(payload.msg || payload.message || 'Request failed')
  }
  return payload.data
}

export async function downloadPendingAuditAttachment(params: { fileId?: number; storageKey?: string }): Promise<Blob> {
  const payloadParams: Record<string, any> = {}
  if (params.fileId != null && Number(params.fileId) > 0) payloadParams.fileId = params.fileId
  if (params.storageKey != null && String(params.storageKey).trim()) payloadParams.storageKey = String(params.storageKey).trim()
  const res = await http.get('/api/archive-management/pending-documents/audit-attachments/download', {
    params: payloadParams,
    responseType: 'blob'
  })
  return res.data as Blob
}

export function transferArchives(data: ArchiveTransferCommand) {
  return apiRequest<ArchiveTransferResponse>(http.post('/api/archive-management/archives/transfer', data))
}

export function fetchArchiveAiModels() {
  return apiRequest<ArchiveAiModelSummary[]>(http.get('/api/archive-management/ai-models'))
}

export function getArchiveDetail(archiveId: number) {
  return apiRequest<ArchiveRecordSummary>(http.get(`/api/archive-management/archives/${archiveId}`))
}

/** 按模块 + 业务主键查操作审计（如应归档：PENDING_ARCHIVE + docId） */
export function fetchOperationAuditsByBusinessKey(moduleCode: string, businessKey: string) {
  return apiRequest<AuditRecord[]>(
    http.get(
      `/api/common/audits/modules/${encodeURIComponent(moduleCode)}/business-keys/${encodeURIComponent(businessKey)}`
    )
  )
}

export function fetchBindOptions() {
  return apiRequest<BindOptions>(http.get('/api/archive-management/bind/options'))
}

export function previewBind(data: BindPreviewCommand) {
  return apiRequest<BindPreviewResult>(http.post('/api/archive-management/bind/preview', data))
}

export function createBindBatch(data: BindCreateCommand) {
  return apiRequest<BindBatch>(http.post('/api/archive-management/bind/batches', data))
}

export function getBindBatch(bindBatchCode: string) {
  return apiRequest<BindBatch>(http.get(`/api/archive-management/bind/batches/${bindBatchCode}`))
}

export function queryBindBatches(data: BindQueryCommand) {
  return apiRequest<BindBatch[]>(http.post('/api/archive-management/bind/query', data))
}

export function fetchStorageOptions() {
  return apiRequest<StorageOptions>(http.get('/api/archive-management/storage/options'))
}

export function queryStorage(data: StorageQueryCommand) {
  return apiRequest<StorageQueryResult>(http.post('/api/archive-management/storage/query', data))
}

export function createStorageBatch(data: StorageCreateCommand) {
  return apiRequest<StorageBatch>(http.post('/api/archive-management/storage/batches', data))
}

export function getStorageBatch(storageBatchCode: string) {
  return apiRequest<StorageBatch>(http.get(`/api/archive-management/storage/batches/${storageBatchCode}`))
}

export function queryStorageLedger(data: StorageLedgerQueryCommand) {
  return apiRequest<StorageLedger[]>(http.post('/api/archive-management/storage/ledger', data))
}

export function getStorageLedger(ledgerId: number) {
  return apiRequest<StorageLedger>(http.get(`/api/archive-management/storage/ledger/${ledgerId}`))
}

export interface FourAttrInspectionQueryParams {
  inspectionName?: string
  inspectionStage?: string
  enableFlag?: 'Y' | 'N'
  tenantid?: number
}

export interface FourAttrInspectionSaveCommand {
  inspectionName: string
  inspectionStage: string
  dataPackageSpec: string
  metadataSpec: string
  enableFlag: 'Y' | 'N'
  tenantid?: number
}

export interface FourAttrInspectionDetailBatchSaveCommand {
  inspectionId: number
  tenantid?: number
  details: FourAttrInspectionDetail[]
}

export function queryFourAttrInspections(params: FourAttrInspectionQueryParams) {
  return apiRequest<FourAttrInspectionConfig[]>(http.get('/api/security/four-properties/configs', { params }))
}

export function getFourAttrInspectionDetail(inspectionId: number, tenantid?: number) {
  return apiRequest<FourAttrInspectionConfig>(http.get(`/api/security/four-properties/configs/${inspectionId}`, {
    params: tenantid ? { tenantid } : undefined
  }))
}

export function createFourAttrInspection(data: FourAttrInspectionSaveCommand) {
  return apiRequest<FourAttrInspectionConfig>(http.post('/api/security/four-properties/configs', data))
}

export function updateFourAttrInspection(inspectionId: number, data: FourAttrInspectionSaveCommand) {
  return apiRequest<FourAttrInspectionConfig>(http.put(`/api/security/four-properties/configs/${inspectionId}`, data))
}

export function saveFourAttrInspectionDetails(inspectionId: number, data: FourAttrInspectionDetailBatchSaveCommand) {
  return apiRequest<FourAttrInspectionConfig>(http.put(`/api/security/four-properties/configs/${inspectionId}/details`, data))
}

export async function exportFourAttrInspections(params: FourAttrInspectionQueryParams) {
  const response = await http.get('/api/security/four-properties/configs/export', {
    params,
    responseType: 'blob'
  })
  return response.data as Blob
}

export function importFourAttrInspections(file: File, tenantid?: number) {
  const formData = new FormData()
  formData.append('file', file)
  if (typeof tenantid === 'number') {
    formData.append('tenantid', String(tenantid))
  }
  return apiRequest<number>(http.post('/api/security/four-properties/configs/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }))
}
