<template>
  <div class="user-role-config-page">
    <h1 class="page-title">系统用户角色配置</h1>
    
    <div class="config-container">
      <!-- 用户列表卡片 -->
      <el-card class="user-list-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span class="btn-icon">group</span>
            <span>用户列表</span>
          </div>
        </template>
        <el-table
          :data="users"
          row-key="userId"
          highlight-current-row
          @current-change="handleUserSelect"
          v-loading="loadingUsers"
        >
          <el-table-column label="用户名(拼音)" min-width="120">
            <template #default="{ row }">
              {{ row.userName ?? row.username ?? '—' }}
            </template>
          </el-table-column>
          <el-table-column label="工号" min-width="100">
            <template #default="{ row }">
              {{ row.employeeNo ?? '—' }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 角色配置卡片 -->
      <el-card class="role-config-card" shadow="never" v-loading="loadingConfig || loadingRoleData">
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <span class="btn-icon">manage_accounts</span>
              <span>角色与维度配置</span>
              <el-tag v-if="selectedUser" type="success" effect="plain" class="ml-2">
                当前用户: {{ selectedUser.userName ?? selectedUser.username ?? '—' }}<template v-if="selectedUser.employeeNo"> · 工号 {{ selectedUser.employeeNo }}</template>
              </el-tag>
            </div>
            <div class="header-actions">
              <el-button type="primary" :disabled="!selectedUser" @click="handleSave">
                <span class="btn-icon">save</span>保存配置
              </el-button>
            </div>
          </div>
        </template>

        <div v-if="!selectedUser" class="empty-state">
          <span class="btn-icon large">touch_app</span>
          <p>请从左侧选择一个用户开始配置</p>
        </div>

        <div v-else class="config-form">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="角色分配" name="roles">
              <div class="role-selection">
                <p class="section-tip">请选择该用户拥有的角色：</p>
                <el-checkbox-group v-model="selectedRoleCodes">
                  <el-row :gutter="20">
                    <el-col v-for="role in allRoles" :key="role.roleCode" :span="8">
                      <el-checkbox :label="role.roleCode" class="role-checkbox">
                        <div class="role-info">
                          <span class="role-name">{{ role.roleName }}</span>
                          <span class="role-desc">{{ role.description }}</span>
                        </div>
                      </el-checkbox>
                    </el-col>
                  </el-row>
                </el-checkbox-group>
              </div>
            </el-tab-pane>

            <el-tab-pane label="维度范围 (Data Scope)" name="scopes">
              <div class="scope-configuration">
                <div v-if="selectedRoleCodes.length === 0" class="empty-state mini">
                  <p>请先在“角色分配”页签选择角色</p>
                </div>
                <div v-else v-for="roleCode in selectedRoleCodes" :key="roleCode" class="role-scope-item">
                  <h3 class="role-scope-title">
                    <span class="btn-icon">verified_user</span>
                    {{ getRoleName(roleCode) }} 的数据维度
                  </h3>
                  
                  <el-form label-position="top">
                    <el-row :gutter="20">
                      <!-- 公司维度（数据范围，码表 ARCHIVED_ENTITY） -->
                      <el-col :span="12">
                        <el-form-item label="公司 (ARCHIVED_ENTITY)">
                          <el-select
                            v-model="roleScopes[roleCode].ARCHIVED_ENTITY"
                            multiple
                            collapse-tags
                            placeholder="全部公司"
                            style="width: 100%"
                          >
                            <el-option
                              v-for="entity in allEntities"
                              :key="entity.documentOrganizationCode"
                              :label="entity.documentOrganizationName"
                              :value="entity.documentOrganizationCode"
                            />
                          </el-select>
                        </el-form-item>
                      </el-col>

                      <!-- 业务模块维度 -->
                      <el-col :span="12">
                        <el-form-item label="业务模块 (BUSINESS_MODULE)">
                          <el-tree-select
                            v-model="roleScopes[roleCode].BUSINESS_MODULE"
                            :data="moduleTree"
                            multiple
                            collapse-tags
                            show-checkbox
                            check-strictly
                            node-key="typeCode"
                            :props="{ label: 'typeName' }"
                            placeholder="全部模块"
                            style="width: 100%"
                          />
                        </el-form-item>
                      </el-col>
                    </el-row>
                  </el-form>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  fetchUsers, 
  fetchRoles, 
  fetchUserRoleConfig, 
  saveUserRoleConfig,
  type User,
  type Role,
  type UserRoleSaveCommand
} from '../../api/modules/security'
import { fetchDocumentOrganizations } from '../../api/modules/documentOrganization'
import { fetchDocumentTypeTree } from '../../api/modules/documentType'

const users = ref<User[]>([])
const allRoles = ref<Role[]>([])
const allEntities = ref<any[]>([])
const moduleTree = ref<any[]>([])

const selectedUser = ref<User | null>(null)
const selectedRoleCodes = ref<string[]>([])
const roleScopes = ref<Record<string, Record<string, string[]>>>({})

const loadingUsers = ref(false)
const loadingConfig = ref(false)
const loadingRoleData = ref(false)
const activeTab = ref('roles')

onMounted(async () => {
  loadingUsers.value = true
  try {
    users.value = await fetchUsers()
  } catch (err) {
    console.error(err)
    ElMessage.error('加载用户列表失败，请确认后端已启动且接口 /api/security/user-roles/users 可用')
  } finally {
    loadingUsers.value = false
  }

  loadingRoleData.value = true
  try {
    const [roleRes, entityRes, moduleRes] = await Promise.all([
      fetchRoles(),
      fetchDocumentOrganizations({}),
      fetchDocumentTypeTree()
    ])
    allRoles.value = roleRes
    allEntities.value = entityRes
    moduleTree.value = moduleRes
  } catch (err) {
    console.error(err)
    ElMessage.error('加载角色或维度数据失败（用户列表已单独加载）')
  } finally {
    loadingRoleData.value = false
  }
})

const handleUserSelect = async (user: User | null) => {
  if (!user) return
  selectedUser.value = user
  loadingConfig.value = true
  try {
    const config = await fetchUserRoleConfig(user.userId)
    
    // 初始化选择状态
    selectedRoleCodes.value = config.roles.map((r: any) => r.roleCode)
    
    // 初始化维度状态
    const newScopes: Record<string, Record<string, string[]>> = {}
    
    // 预填充所有角色的空容器
    allRoles.value.forEach(r => {
      newScopes[r.roleCode] = {
        ARCHIVED_ENTITY: [],
        BUSINESS_MODULE: []
      }
    })

    // 填充已有配置
    config.roles.forEach((role: any) => {
      role.scopes.forEach((scope: any) => {
        if (newScopes[role.roleCode][scope.dimensionCode]) {
          newScopes[role.roleCode][scope.dimensionCode] = scope.values
        }
      })
    })
    
    roleScopes.value = newScopes
  } catch (err) {
    ElMessage.error('获取用户配置失败')
  } finally {
    loadingConfig.value = false
  }
}

// 监听角色选择，确保每个被选中的角色都有对应的 scope 容器
watch(selectedRoleCodes, (newVal) => {
  newVal.forEach(code => {
    if (!roleScopes.value[code]) {
      roleScopes.value[code] = {
        ARCHIVED_ENTITY: [],
        BUSINESS_MODULE: []
      }
    }
  })
}, { deep: true })

const getRoleName = (code: string) => {
  return allRoles.value.find(r => r.roleCode === code)?.roleName || code
}

const handleSave = async () => {
  if (!selectedUser.value) return
  
  const command: UserRoleSaveCommand = {
    userId: selectedUser.value.userId,
    roles: selectedRoleCodes.value.map(roleCode => {
      const scopes = []
      const currentRoleScopes = roleScopes.value[roleCode]
      
      if (currentRoleScopes.ARCHIVED_ENTITY.length > 0) {
        scopes.push({
          dimensionCode: 'ARCHIVED_ENTITY',
          values: currentRoleScopes.ARCHIVED_ENTITY
        })
      }
      
      if (currentRoleScopes.BUSINESS_MODULE.length > 0) {
        scopes.push({
          dimensionCode: 'BUSINESS_MODULE',
          values: currentRoleScopes.BUSINESS_MODULE
        })
      }
      
      return {
        roleCode,
        scopes
      }
    })
  }

  try {
    await saveUserRoleConfig(command)
    ElMessage.success('配置保存成功')
  } catch (err) {
    ElMessage.error('保存失败')
  }
}
</script>

<style scoped>
.user-role-config-page {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: linear-gradient(135deg, #f6f7f8 0%, #e8f0f7 100%);
  animation: fadeIn 0.6s ease-out;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #1173d4 0%, #3d8ce8 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.config-container {
  display: grid;
  grid-template-columns: 350px 1fr;
  gap: 24px;
  align-items: start;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-icon {
  font-family: 'Material Symbols Outlined';
  font-size: 20px;
  vertical-align: middle;
  margin-right: 4px;
}

.btn-icon.large {
  font-size: 64px;
  color: #dbe0e6;
  margin-bottom: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 0;
  color: #617589;
}

.empty-state.mini {
  padding: 40px 0;
}

.section-tip {
  color: #617589;
  margin-bottom: 20px;
  font-size: 14px;
}

.role-checkbox {
  width: 100%;
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #dbe0e6;
  border-radius: 8px;
  transition: all 0.3s;
  height: auto;
}

.role-checkbox:hover {
  border-color: #1173d4;
  background-color: rgba(17, 115, 212, 0.02);
}

.role-checkbox.is-checked {
  border-color: #1173d4;
  background-color: rgba(17, 115, 212, 0.05);
}

:deep(.role-checkbox .el-checkbox__label) {
  width: 100%;
}

.role-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  white-space: normal;
}

.role-name {
  font-weight: 600;
  color: #111418;
}

.role-desc {
  font-size: 12px;
  color: #617589;
}

.role-scope-item {
  margin-bottom: 32px;
  padding: 24px;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #dbe0e6;
}

.role-scope-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 20px;
  color: #1173d4;
  display: flex;
  align-items: center;
  gap: 8px;
}

:deep(.el-card) {
  border-radius: 12px;
  border-color: #dbe0e6;
}

:deep(.el-tabs__item) {
  font-weight: 600;
  font-size: 15px;
}

@keyframes fadeIn {
  0% { opacity: 0; }
  100% { opacity: 1; }
}

.ml-2 { margin-left: 8px; }
</style>
