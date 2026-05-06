import http, { apiRequest } from '../http'
import type { BarcodeModule } from '../../types'

/** 与后端 BarcodeModuleCommand / BarcodeModuleUpdateCommand 字段一致（enableFlag，非 enabledFlag） */
export interface BarcodeModuleCommand {
  barcodeCode: string
  barcodeName: string
  description?: string
  enableFlag?: 'Y' | 'N'
}

export interface LinkedBusinessModuleItem {
  moduleCode: string
  moduleName: string
}

export function fetchBarcodeModules(params?: { enabledOnly?: boolean }) {
  return apiRequest<BarcodeModule[]>(
    http.get('/api/base-data/barcode-modules', {
      params: params?.enabledOnly != null ? { enabledOnly: params.enabledOnly } : undefined
    })
  )
}

export function fetchLinkedBusinessModules(barcodeModuleCode: string) {
  const code = encodeURIComponent(barcodeModuleCode.trim())
  return apiRequest<LinkedBusinessModuleItem[]>(
    http.get(`/api/base-data/barcode-modules/${code}/linked-business-modules`)
  )
}

export function createBarcodeModule(data: BarcodeModuleCommand) {
  return apiRequest<BarcodeModule>(http.post('/api/base-data/barcode-modules', data))
}

export function updateBarcodeModule(barcodeId: number, data: BarcodeModuleCommand) {
  return apiRequest<BarcodeModule>(http.put(`/api/base-data/barcode-modules/${barcodeId}`, data))
}

export function deleteBarcodeModule(barcodeId: number) {
  return apiRequest<void>(http.delete(`/api/base-data/barcode-modules/${barcodeId}`))
}
