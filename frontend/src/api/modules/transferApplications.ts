import http, { apiRequest } from '../http'
import type {
  TransferApplicationCreateCommand,
  TransferApplicationDetailAttachment,
  TransferApplicationExtValue
} from '../../types'

/** 与后端 TransferApplicationRecordQuery 字段名一致 */
export interface TransferApplicationRecordQuery {
  docBusiNo?: string
  companyProjectCode?: string
  busiModuleCode?: string
  archPeriodRange?: string[]
  applicant?: number | null
  applicationNumber?: string
  applicationDateRange?: string[]
  applicationStatus?: string
  carrierType?: string
  diffReasonCode?: string
  applyMethod?: string
  expressType?: string
  expressNumber?: string
  documentRecipient?: number | null
  catalogVolumeNo?: string
}

export interface TransferApplicationRecordPageCommand {
  filter: TransferApplicationRecordQuery
  page: number
  pageSize: number
  tenantid?: number
}

export interface TransferApplicationRecordRow {
  applicationId: number
  applicationNumber: string
  busiModuleCode: string | null
  busiModuleName: string | null
  applicant: number | null
  applicantName: string
  applicationDate: string | null
  applicationStatus: string | null
  documentRecipient: number | null
  documentRecipientName: string
  expressType: string | null
  expressNumber: string | null
}

export interface TransferApplicationRecordPageResult {
  records: TransferApplicationRecordRow[]
  total: number
  pages: number
  page: number
  pageSize: number
}

export interface TransferApplicationDetailItem {
  applicationDetailId: number
  applicationId: number
  docBusiNo?: string
  docName?: string
  busiModuleCode?: string
  companyProjectCode?: string
  archPlaceAlpha2Code?: string
  documentOrganizationCode?: string
  carrierType?: string
  endArchPeriod?: string
  startArchPeriod?: string
  busiVolumeNo?: string
  archTypeCode?: string
  docGenerationDate?: string
  archCopies?: number
  remark?: string
  description?: string
  catalogVolumeNo?: string
  extValues?: TransferApplicationExtValue[]
  attachments?: TransferApplicationDetailAttachment[]
}

export interface TransferApplicationDetailPayload {
  applicationId: number
  applicationNumber: string
  applicant?: number | null
  applicantName?: string
  applicationDate?: string | null
  department?: string
  busiModuleCode?: string
  applyMethod?: string
  expressType?: string
  expressNumber?: string
  documentRecipient?: number | null
  documentRecipientName?: string
  handoverForm?: string
  applicationStatus?: string
  status?: string
  /** 审批通过后是否已写入档案库 Y/N */
  archivesMaterialized?: string
  diffReasonCode?: string
  applicationDescription?: string
  details: TransferApplicationDetailItem[]
}

export function searchTransferApplicationRecords(cmd: TransferApplicationRecordPageCommand) {
  return apiRequest<TransferApplicationRecordPageResult>(
    http.post('/api/archive-management/transfer-applications/search-page', cmd)
  )
}

export function getTransferApplication(id: number | string) {
  return apiRequest<TransferApplicationDetailPayload>(http.get(`/api/archive-management/transfer-applications/${id}`))
}

export function createTransferApplication(command: TransferApplicationCreateCommand) {
  return apiRequest<TransferApplicationDetailPayload>(http.post('/api/archive-management/transfer-applications', command))
}

export function updateTransferApplication(id: number | string, command: TransferApplicationCreateCommand) {
  return apiRequest<TransferApplicationDetailPayload>(http.put(`/api/archive-management/transfer-applications/${id}`, command))
}

export function uploadTransferApplicationDetailAttachment(
  applicationId: number,
  detailId: number,
  file: File,
  remark?: string
) {
  const formData = new FormData()
  formData.append('file', file)
  if (remark) formData.append('remark', remark)
  return apiRequest<TransferApplicationDetailAttachment>(
    http.post(`/api/archive-management/transfer-applications/${applicationId}/details/${detailId}/attachments`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  )
}

export function listTransferApplicationDetailAttachments(applicationId: number, detailId: number) {
  return apiRequest<TransferApplicationDetailAttachment[]>(
    http.get(`/api/archive-management/transfer-applications/${applicationId}/details/${detailId}/attachments`)
  )
}

export async function downloadTransferApplicationDetailAttachment(applicationId: number, detailId: number, attachmentId: number) {
  const response = await http.get(
    `/api/archive-management/transfer-applications/${applicationId}/details/${detailId}/attachments/${attachmentId}/download`,
    { responseType: 'blob' }
  )
  return response.data as Blob
}
