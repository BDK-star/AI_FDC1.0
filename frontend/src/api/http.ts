import axios, { AxiosHeaders, type AxiosError } from 'axios'
import { CURRENT_OPERATOR_USER_ID } from '../constants/currentUser'

export interface ApiResponse<T> {
  code?: number
  msg?: string
  success?: boolean
  message?: string
  data: T
}

/**
 * 仅在前端与后端使用「两条 ngrok 隧道」时需要配置（无尾斜杠）。
 * 若只把 Vite 开发服映射到公网、/api 仍走本机代理，可留空。
 */
export function getApiBaseUrl(): string {
  const raw = import.meta.env.VITE_API_BASE_URL as string | undefined
  if (raw == null || typeof raw !== 'string') {
    return ''
  }
  return raw.trim().replace(/\/$/, '')
}

function shouldAddNgrokBrowserBypassHeader(url: string, baseURL: string | undefined): boolean {
  const combined = url.startsWith('http') ? url : `${baseURL ?? ''}${url}`
  return /ngrok/i.test(combined)
}

const http = axios.create({
  // 默认空：与页面同源，由 Vite 把 /api 代理到 localhost:8080。
  // 设置 VITE_API_BASE_URL 时指向后端公网地址（如第二条 ngrok）。
  baseURL: getApiBaseUrl(),
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const headers = AxiosHeaders.from(config.headers ?? {})
  if (headers.get('X-User-Id') == null && headers.get('x-user-id') == null) {
    headers.set('X-User-Id', String(CURRENT_OPERATOR_USER_ID))
  }
  if (shouldAddNgrokBrowserBypassHeader(config.url ?? '', config.baseURL)) {
    headers.set('ngrok-skip-browser-warning', 'true')
  }
  config.headers = headers
  return config
})

function extractMessageFromUnknownPayload(data: unknown): string | null {
  if (data == null) return null
  if (typeof data === 'string') {
    const s = data.trim()
    if (!s || s.startsWith('<')) return null
    try {
      const parsed = JSON.parse(s) as Record<string, unknown>
      return extractMessageFromUnknownPayload(parsed)
    } catch {
      return s.length <= 280 ? s : `${s.slice(0, 280)}…`
    }
  }
  if (typeof data !== 'object') return null
  const o = data as Record<string, unknown>
  const direct =
    (typeof o.msg === 'string' && o.msg.trim()) ||
    (typeof o.message === 'string' && o.message.trim()) ||
    (typeof o.error === 'string' && o.error.trim())
  if (direct) return direct
  if (typeof o.detail === 'string' && o.detail.trim()) return o.detail.trim()
  const errs = (o as { errors?: Array<{ defaultMessage?: string }> }).errors
  if (Array.isArray(errs) && errs.length > 0 && typeof errs[0]?.defaultMessage === 'string') {
    return errs[0].defaultMessage.trim()
  }
  const status = typeof o.status === 'number' ? o.status : undefined
  const path = typeof o.path === 'string' ? o.path : ''
  const title = typeof o.title === 'string' ? o.title : ''
  if (status != null || title || path) {
    return [title || '请求失败', status != null ? `(HTTP ${status})` : '', path].filter(Boolean).join(' ')
  }
  return null
}

function backendMessageFromAxiosError(error: AxiosError<unknown>): string {
  const status = error.response?.status
  const raw = error.response?.data

  const fromPayload = extractMessageFromUnknownPayload(raw)
  if (fromPayload) return fromPayload

  if (error.code === 'ECONNABORTED' || (error.message && error.message.includes('timeout'))) {
    return '请求超时，请稍后重试'
  }
  if (status === 502 || status === 503 || status === 504) {
    return '网关或服务不可用（请确认后端已在 8080 端口启动）'
  }
  if (!error.response && error.message === 'Network Error') {
    return '网络异常或无法连接服务器'
  }
  return error.message || 'Request failed'
}

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<unknown>) => Promise.reject(new Error(backendMessageFromAxiosError(error)))
)

function isApiSuccess(payload: ApiResponse<unknown>): boolean {
  if (payload.success === true) return true
  if (payload.success === false) return false
  if (typeof payload.code === 'number') return payload.code === 0
  if (typeof payload.code === 'string' && payload.code !== '') return Number(payload.code) === 0
  // 部分后端/Jackson 配置会省略数值 0，导致无 code 字段；统一封装成功时常为 msg=OK 且含 data
  const msg = typeof payload.msg === 'string' ? payload.msg.trim().toUpperCase() : ''
  const hasDataKey = Object.prototype.hasOwnProperty.call(payload, 'data')
  if (payload.code === undefined && hasDataKey && msg === 'OK') return true
  return false
}

export async function apiRequest<T>(promise: Promise<{ data: ApiResponse<T> | unknown }>): Promise<T> {
  const response = await promise
  const rawPayload = response.data as unknown

  if (rawPayload == null || typeof rawPayload !== 'object') {
    throw new Error('服务器返回格式异常')
  }

  const payload = rawPayload as ApiResponse<T>

  if (!isApiSuccess(payload)) {
    const hint =
      extractMessageFromUnknownPayload(rawPayload) ||
      (typeof payload.msg === 'string' && payload.msg.trim()) ||
      (typeof payload.message === 'string' && payload.message.trim())
    throw new Error(hint || 'Request failed')
  }

  return payload.data
}

export default http
