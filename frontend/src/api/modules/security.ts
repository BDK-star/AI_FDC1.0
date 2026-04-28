import http, { apiRequest } from '../http'
import type { ApiResponse } from '../http'

/** 与后端 {@code com.smartarchive.security.domain.User} / tpl_user_t 序列化字段一致 */
export interface User {
  userId: number
  userName: string
  /** 旧前端/缓存可能仍使用小写字段名 */
  username?: string
  employeeNo?: string | null
  dutyDepartment?: string | null
  workCountryCode?: string | null
  email?: string | null
  phone?: string | null
  status?: string | null
}

export interface Role {
  roleId: number
  roleCode: string
  roleName: string
  description: string
}

export interface ScopeConfig {
  dimensionCode: string
  values: string[]
}

export interface RoleConfig {
  roleCode: string
  roleName: string
  scopes: ScopeConfig[]
}

export interface UserRoleConfigResponse {
  userId: number
  username: string
  realName: string
  roles: RoleConfig[]
}

export interface UserRoleSaveCommand {
  userId: number
  roles: {
    roleCode: string
    scopes: {
      dimensionCode: string
      values: string[]
    }[]
  }[]
}

export interface UserDutyProfile {
  userName: string
  dutyDepartment?: string
  workCountryCode?: string
}

export const fetchUsers = () => apiRequest<User[]>(http.get('/api/security/user-roles/users'))

export const fetchUserDutyProfile = (userName: string) =>
  apiRequest<UserDutyProfile | null>(http.get('/api/security/user-roles/users/duty-profile', { params: { userName } }))
export const fetchRoles = () => apiRequest<Role[]>(http.get('/api/security/user-roles/roles'))
export const fetchUserRoleConfig = (userId: number) => apiRequest<UserRoleConfigResponse>(http.get(`/api/security/user-roles/config/${userId}`))
export const saveUserRoleConfig = (command: UserRoleSaveCommand) => apiRequest<void>(http.post('/api/security/user-roles/config', command))
