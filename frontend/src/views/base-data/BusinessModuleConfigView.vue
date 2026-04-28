<template>
  <div class="business-module-page">
    <section class="module-hero">
      <div>
        <span>Business Module Config</span>
        <h2>业务模块配置</h2>
        <p>按层级维护业务模块，并为每个层级配置附件、文档的扩展字段。</p>
      </div>
    </section>

    <el-card shadow="never" class="query-card">
      <el-form class="module-query" :model="moduleQuery" label-position="top">
        <el-form-item label="业务模块">
          <el-tree-select
            v-model="moduleQuery.moduleCodes"
            :data="moduleQueryTreeOptions"
            multiple
            show-checkbox
            check-strictly
            default-expand-all
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            node-key="moduleCode"
            :props="{ value: 'moduleCode', label: 'queryLabel', children: 'children' }"
            placeholder="请选择业务模块"
            @change="handleModuleQueryCodesChange"
          />
        </el-form-item>
        <el-form-item label="集成方式">
          <el-select v-model="moduleQuery.integrationType" clearable placeholder="请选择集成方式">
            <el-option label="全部集成" value="全部集成" />
            <el-option label="部分集成" value="部分集成" />
            <el-option label="不集成" value="不集成" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用标志">
          <el-select v-model="moduleQuery.enabledFlag" clearable placeholder="请选择启用标志">
            <el-option label="启用" value="Y" />
            <el-option label="停用" value="N" />
          </el-select>
        </el-form-item>
        <div class="query-actions">
          <el-button type="primary" @click="applyModuleQuery">查询</el-button>
          <el-button @click="resetModuleQuery">重置</el-button>
        </div>
      </el-form>
    </el-card>

    <div class="module-layout">
      <el-card shadow="never" class="tree-card">
        <template #header>
          <div class="card-head">
            <div class="tree-title">
              <el-button class="tree-toggle-button" circle :title="moduleTreeToggleTitle" :aria-label="moduleTreeToggleTitle" @click="toggleAllModuleNodes">
                <span class="tree-toggle-icon">↕</span>
              </el-button>
              <strong>业务模块层级</strong>
            </div>
          </div>
        </template>
        <el-tree
          v-if="pagedTreeData.length"
          ref="moduleTreeRef"
          :data="pagedTreeData"
          node-key="moduleCode"
          default-expand-all
          highlight-current
          :expand-on-click-node="false"
          :props="{ label: 'moduleName', children: 'children' }"
          @node-click="selectNode"
        >
          <template #default="{ data }">
            <div class="tree-node">
              <div class="tree-node-main">
                <div class="tree-node-title">{{ data.moduleCode }}（{{ data.moduleName }}）</div>
                <div class="tree-node-meta">
                  <span>第 {{ data.levelNum }} 层</span>
                  <span>{{ data.parentCode || 'ROOT' }}</span>
                </div>
              </div>
              <div class="tree-node-side">
                <el-tag size="small" effect="light" :type="data.enabledFlag === 'Y' ? 'success' : 'info'">{{ data.enabledFlag === 'Y' ? '启用' : '停用' }}</el-tag>
              </div>
            </div>
          </template>
        </el-tree>
        <el-empty v-else description="暂无业务模块" />
        <el-pagination
          v-if="treeData.length"
          v-model:current-page="treePagination.currentPage"
          v-model:page-size="treePagination.pageSize"
          class="tree-pagination"
          small
          background
          layout="total, prev, pager, next, sizes"
          :page-sizes="[5, 10, 20, 50]"
          :total="treeData.length"
          @size-change="handleTreePageSizeChange"
          @current-change="handleTreePageChange"
        />
        <div v-if="!treeData.length" class="empty-actions">
          <el-button type="primary" round @click="openAddModuleDialog">新增</el-button>
        </div>
      </el-card>

      <div class="detail-stack">
        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <div class="card-title-with-path">
                <strong>业务模块信息</strong>
                <el-tag v-if="selectedNode" effect="plain" type="primary" class="module-path-tag">{{ selectedModulePath }}</el-tag>
              </div>
              <div>
                <el-button size="small" type="primary" @click="openAddModuleDialog">新增</el-button>
                <el-button size="small" :disabled="!selectedNode" @click="openEditDialog">编辑</el-button>
                <el-tooltip content="CSV 导入时，密级列支持填写字典编码或名称" placement="top">
                  <el-button size="small" @click="triggerModuleImport">导入</el-button>
                </el-tooltip>
                <el-button size="small" @click="exportModules">导出</el-button>
                <input ref="moduleImportInputRef" class="hidden-file-input" type="file" accept=".csv,text/csv" @change="importModules" />
              </div>
            </div>
          </template>
          <el-descriptions v-if="selectedNode" :column="2" border>
            <el-descriptions-item label="层级">第 {{ selectedNode.levelNum }} 层</el-descriptions-item>
            <el-descriptions-item label="业务模块编码">{{ selectedNode.moduleCode }}</el-descriptions-item>
            <el-descriptions-item label="业务模块名称">{{ selectedNode.moduleName }}</el-descriptions-item>
            <el-descriptions-item label="排序">{{ selectedNode.sortOrder }}</el-descriptions-item>
            <el-descriptions-item label="密级">{{ resolveSecurityLevelName(selectedNode.securityLevelCode || selectedNode.securityLevel || 'INTERNAL_PUBLIC') }}</el-descriptions-item>
            <el-descriptions-item label="集成类型">{{ selectedNode.integrationType || '不集成' }}</el-descriptions-item>
            <el-descriptions-item label="启用标志">{{ selectedNode.enabledFlag === 'Y' ? '启用' : '停用' }}</el-descriptions-item>
            <el-descriptions-item label="修改人">{{ resolveUserDisplayById(selectedNode.lastUpdatedBy) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ selectedNode.lastUpdateDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ selectedNode.description || '无' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ selectedNode.remark || '无' }}</el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="请选择左侧业务模块" />
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="card-head">
              <strong>扩展字段维护</strong>
              <el-button size="small" type="primary" :disabled="!selectedNode" @click="openFieldDialog()">新增</el-button>
            </div>
          </template>
          <el-tabs v-model="activeScope" @tab-change="loadFields">
            <el-tab-pane label="文档扩展字段" name="BASIC" />
            <el-tab-pane label="附件扩展字段" name="ATTACHMENT" />
          </el-tabs>
          <el-form class="field-query" :model="fieldQueryForm" label-position="top">
            <el-form-item label="应用功能">
              <el-select v-model="fieldQueryForm.applicationFunctions" multiple clearable collapse-tags collapse-tags-tooltip placeholder="请选择应用功能">
                <el-option v-for="item in applicationFunctionOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="扩展字段">
              <el-select v-model="fieldQueryForm.extAttributes" multiple clearable collapse-tags collapse-tags-tooltip placeholder="请选择扩展字段">
                <el-option v-for="item in queryExtAttributeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="查询">
              <el-select v-model="fieldQueryForm.queryFlags" multiple clearable collapse-tags placeholder="请选择">
                <el-option label="是" value="Y" />
                <el-option label="否" value="N" />
              </el-select>
            </el-form-item>
            <el-form-item label="必填">
              <el-select v-model="fieldQueryForm.requiredFlags" multiple clearable collapse-tags placeholder="请选择">
                <el-option label="是" value="Y" />
                <el-option label="否" value="N" />
              </el-select>
            </el-form-item>
            <el-form-item label="启用">
              <el-select v-model="fieldQueryForm.enabledFlags" multiple clearable collapse-tags placeholder="请选择">
                <el-option label="是" value="Y" />
                <el-option label="否" value="N" />
              </el-select>
            </el-form-item>
            <div class="field-query-actions">
              <el-button type="primary" @click="applyFieldQuery">查询</el-button>
              <el-button @click="resetFieldQuery">重置</el-button>
            </div>
          </el-form>
          <el-table :data="pagedFields" border empty-text="暂无扩展字段">
            <el-table-column prop="moduleCode" label="业务模块" min-width="170">
              <template #default="{ row }">{{ formatModuleLabel(row.moduleCode) }}</template>
            </el-table-column>
            <el-table-column prop="applicationFunctions" label="应用功能" min-width="150">
              <template #default="{ row }">
                <el-tag v-for="item in row.applicationFunctions || []" :key="item" class="function-tag" type="primary">{{ item }}</el-tag>
                <span v-if="!row.applicationFunctions?.length">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="extAttribute" label="扩展字段" width="120">
              <template #default="{ row }">{{ row.extAttribute || '-' }}</template>
            </el-table-column>
            <el-table-column prop="fieldName" label="字段名" min-width="150" />
            <el-table-column prop="englishFieldName" label="字段编码" min-width="150">
              <template #default="{ row }">{{ row.englishFieldName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="dataType" label="数据类型" width="120" />
            <el-table-column prop="queryFlag" label="查询" width="90">
              <template #default="{ row }"><el-tag :type="row.queryFlag === 'Y' ? 'success' : 'info'">{{ flagText(row.queryFlag) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="requiredFlag" label="必填" width="90">
              <template #default="{ row }"><el-tag :type="row.requiredFlag === 'Y' ? 'danger' : 'info'">{{ flagText(row.requiredFlag) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="enabledFlag" label="启用" width="90">
              <template #default="{ row }"><el-tag :type="row.enabledFlag === 'Y' ? 'success' : 'info'">{{ flagText(row.enabledFlag) }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="sortOrder" label="排序" width="90" />
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openFieldDialog(row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-if="filteredFields.length"
            v-model:current-page="fieldPagination.currentPage"
            v-model:page-size="fieldPagination.pageSize"
            class="field-pagination"
            small
            background
            layout="total, prev, pager, next, sizes"
            :page-sizes="[5, 10, 20, 50]"
            :total="filteredFields.length"
            @size-change="handleFieldPageSizeChange"
            @current-change="handleFieldPageChange"
          />
        </el-card>
      </div>
    </div>

    <el-dialog v-model="moduleDialogVisible" :title="moduleMode === 'create' ? '新增业务模块' : '编辑业务模块'" width="560px">
      <el-form label-position="top" :model="moduleForm">
        <el-form-item label="上级业务模块">
          <el-select v-model="moduleForm.parentCode" clearable filterable placeholder="ROOT 根节点">
            <el-option v-for="item in parentOptions" :key="item.code" :label="`${item.code} | ${item.description || '-'}`" :value="item.code" />
          </el-select>
          <div class="level-hint">清空后保存为一级模块；选择其他上级后，将自动调整当前模块及其下级模块层级。</div>
        </el-form-item>
        <el-form-item label="业务模块编码" required>
          <el-select v-if="isRootModuleCreate" v-model="moduleForm.moduleCode" filterable clearable placeholder="请选择文档类型编码" @change="handleRootModuleCodeChange">
            <el-option v-for="item in documentTypeOptions" :key="item.typeCode" :label="`${item.typeCode} ｜ ${item.typeName}`" :value="item.typeCode" />
          </el-select>
          <el-input v-else v-model="moduleForm.moduleCode" :disabled="moduleMode === 'edit'" />
        </el-form-item>
        <el-form-item label="业务模块名称" required><el-input v-model="moduleForm.moduleName" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="moduleForm.sortOrder" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="密级">
          <el-select v-model="moduleForm.securityLevelCode" placeholder="请选择密级">
            <el-option v-for="item in securityLevelOptions" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="集成类型">
          <el-select v-model="moduleForm.integrationType">
            <el-option label="全部集成" value="全部集成" />
            <el-option label="部分集成" value="部分集成" />
            <el-option label="不集成" value="不集成" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用标志"><el-radio-group v-model="moduleForm.enabledFlag"><el-radio value="Y">启用</el-radio><el-radio value="N">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="描述"><el-input v-model="moduleForm.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="moduleForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="moduleDialogVisible = false">取消</el-button><el-button type="primary" @click="saveModule">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="fieldDialogVisible" :title="fieldMode === 'create' ? '新增扩展字段' : '编辑扩展字段'" width="560px">
      <el-form label-position="top" :model="fieldForm">
        <el-form-item label="字段归属"><el-radio-group v-model="fieldForm.fieldScope"><el-radio value="BASIC">文档</el-radio><el-radio value="ATTACHMENT">附件</el-radio></el-radio-group></el-form-item>
        <el-form-item label="应用功能" required>
          <el-select v-model="fieldForm.applicationFunctions" multiple collapse-tags collapse-tags-tooltip placeholder="请选择应用功能">
            <el-option label="应归档数据" value="应归档数据" />
            <el-option label="移交" value="移交" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据类型" required>
          <el-select v-model="fieldForm.dataType">
            <el-option label="文本" value="TEXT" />
            <el-option label="数字" value="NUMBER" />
            <el-option label="日期" value="DATE" />
            <el-option label="日期时间" value="DATETIME" />
            <el-option label="数据字典" value="DICT" />
            <el-option label="是/否" value="BOOLEAN" />
          </el-select>
        </el-form-item>
        <el-form-item label="扩展字段" required>
          <el-select v-model="fieldForm.extAttribute" clearable placeholder="请先选择字段类型，再选择扩展字段">
            <el-option v-for="item in availableExtAttributeOptions" :key="item" :label="item" :value="item" :disabled="isExtAttributeUsed(item)">
              <div class="ext-attribute-option">
                <span>{{ item }}</span>
                <el-tag v-if="isExtAttributeUsed(item)" size="small" type="warning" effect="light">已使用</el-tag>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="字段名" required><el-input v-model="fieldForm.fieldName" /></el-form-item>
        <el-form-item label="字段编码" required><el-input v-model="fieldForm.englishFieldName" /></el-form-item>
        <div class="field-options">
          <el-form-item label="查询"><el-switch v-model="fieldForm.queryFlag" active-value="Y" inactive-value="N" /></el-form-item>
          <el-form-item label="必填"><el-switch v-model="fieldForm.requiredFlag" active-value="Y" inactive-value="N" /></el-form-item>
          <el-form-item label="启用"><el-switch v-model="fieldForm.enabledFlag" active-value="Y" inactive-value="N" /></el-form-item>
          <el-form-item label="排序"><el-input-number v-model="fieldForm.sortOrder" :min="1" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="fieldDialogVisible = false">取消</el-button><el-button type="primary" @click="saveField">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  buildModuleQueryTree,
  createBusinessModule,
  createBusinessModuleExtField,
  deleteBusinessModule,
  deleteBusinessModuleExtField,
  fetchBusinessModuleExtFields,
  fetchBusinessModuleParentOptions,
  fetchBusinessModuleTree,
  updateBusinessModule,
  updateBusinessModuleExtField,
  type BusinessModuleCommand,
  type BusinessModuleExtFieldCommand,
  type BusinessModuleParentOption
} from '../../api/modules/businessModule'
import { fetchDictionaryItems } from '../../api/modules/dictionary'
import { fetchUsers } from '../../api/modules/security'
import { fetchDocumentTypeTree } from '../../api/modules/documentType'
import type { BusinessModuleExtField, BusinessModuleNode, DocumentTypeTreeNode } from '../../types'

type FieldQueryState = {
  applicationFunctions: string[]
  extAttributes: string[]
  queryFlags: string[]
  requiredFlags: string[]
  enabledFlags: string[]
}

const rawTreeData = ref<BusinessModuleNode[]>([])
const documentTypeTree = ref<DocumentTypeTreeNode[]>([])
const parentOptionSource = ref<BusinessModuleParentOption[]>([])
const selectedNode = ref<BusinessModuleNode>()
const hasManualModuleSelection = ref(false)
const moduleTreeRef = ref()
const moduleImportInputRef = ref<HTMLInputElement>()
const securityLevelOptions = ref<Array<{ code: string; name: string }>>([])
const userDisplayById = ref<Record<number, string>>({})
const fields = ref<BusinessModuleExtField[]>([])
const usedExtAttributes = ref<Set<string>>(new Set())
const areModuleNodesExpanded = ref(true)
const activeScope = ref<'BASIC' | 'ATTACHMENT'>('BASIC')
const moduleDialogVisible = ref(false)
const fieldDialogVisible = ref(false)
const moduleMode = ref<'create' | 'edit'>('create')
const fieldMode = ref<'create' | 'edit'>('create')
const editingFieldCode = ref('')

const moduleForm = reactive<BusinessModuleCommand>({ moduleCode: '', moduleName: '', parentCode: '', enabledFlag: 'Y', sortOrder: 1, securityLevelCode: 'INTERNAL_PUBLIC', integrationType: '不集成', description: '', remark: '' })
const fieldForm = reactive<BusinessModuleExtFieldCommand>({ fieldCode: '', fieldScope: 'BASIC', applicationFunctions: [], extAttribute: undefined, fieldName: '', englishFieldName: '', dataType: 'TEXT', queryFlag: 'N', requiredFlag: 'N', enabledFlag: 'Y', sortOrder: 1 })
const applicationFunctionOptions = ['应归档数据', '移交'] as const
const buildAttrRange = (prefix: string, start: number, end: number) =>
  Array.from({ length: end - start + 1 }, (_, index) => `${prefix}${start + index}`)
const BASIC_TEXT_ATTRS = buildAttrRange('ATTR', 1, 40)
const BASIC_NUMBER_ATTRS = buildAttrRange('ATTR', 41, 60)
const BASIC_DATE_ATTRS = buildAttrRange('ATTR', 61, 80)
const BASIC_DATETIME_ATTRS = buildAttrRange('ATTR', 81, 100)
const ATTACHMENT_TEXT_ATTRS = buildAttrRange('ATTRIBUTE', 1, 20)
const ATTACHMENT_NUMBER_ATTRS = buildAttrRange('ATTRIBUTE', 21, 30)
const ATTACHMENT_DATE_ATTRS = buildAttrRange('ATTRIBUTE', 31, 40)
const ATTACHMENT_DATETIME_ATTRS = buildAttrRange('ATTRIBUTE', 41, 50)
const moduleQuery = reactive<{ moduleCodes: string[]; integrationType: string; enabledFlag: '' | 'Y' | 'N' }>({
  moduleCodes: [],
  integrationType: '',
  enabledFlag: ''
})
const treePagination = reactive({
  currentPage: 1,
  pageSize: 10
})
const fieldPagination = reactive({
  currentPage: 1,
  pageSize: 10
})
const fieldQueryForm = reactive<FieldQueryState>(createEmptyFieldQuery())
const fieldQuery = reactive<FieldQueryState>(createEmptyFieldQuery())
const moduleImportExportHeaders = ['业务模块编码', '业务模块名称', '上级业务模块编码', '层级', '排序', '密级', '集成类型', '启用标志', '描述', '备注', '更新时间'] as const

const flatten = (nodes: BusinessModuleNode[]): BusinessModuleNode[] => nodes.flatMap(node => [node, ...flatten(node.children || [])])
const treeData = computed(() => filterModuleTree(rawTreeData.value))
const pagedTreeData = computed(() => {
  const start = (treePagination.currentPage - 1) * treePagination.pageSize
  return treeData.value.slice(start, start + treePagination.pageSize)
})
const moduleQueryTreeOptions = computed(() => buildModuleQueryTree(rawTreeData.value))
const moduleTreeToggleTitle = computed(() => areModuleNodesExpanded.value ? '全部收缩' : '全部展开')
const selectedModulePath = computed(() => {
  if (!selectedNode.value) return ''
  const moduleMap = new Map(flatten(rawTreeData.value).map(item => [item.moduleCode, item]))
  const pathCodes = [
    ...(selectedNode.value.ancestorPath || '').split('/').map(item => item.trim()).filter(Boolean),
    selectedNode.value.moduleCode
  ]
  return pathCodes
    .map(code => {
      const module = moduleMap.get(code)
      return module ? `${module.moduleCode}（${module.moduleName}）` : code
    })
    .join('/')
})
const parentOptions = computed(() => parentOptionSource.value.filter(item => {
  const editingCode = moduleMode.value === 'edit' ? moduleForm.moduleCode : ''
  if (item.code === editingCode) return false
  if (item.sourceType !== 'BUSINESS_MODULE') return true
  const candidate = flatten(rawTreeData.value).find(node => node.moduleCode === item.code)
  if (!candidate) return true
  return candidate.levelNum < 6 && !isDescendantModule(candidate, editingCode)
}))
const flattenDocumentTypes = (nodes: DocumentTypeTreeNode[]): DocumentTypeTreeNode[] => nodes.flatMap(node => [node, ...flattenDocumentTypes(node.children || [])])
const documentTypeOptions = computed(() => flattenDocumentTypes(documentTypeTree.value).filter(item => item.enabledFlag === 'Y'))
const isRootModuleCreate = computed(() => moduleMode.value === 'create' && !moduleForm.parentCode)
const flagText = (flag: string) => flag === 'Y' ? '是' : '否'
const filteredFields = computed(() => fields.value.filter(field => {
  const appMatched = !fieldQuery.applicationFunctions.length || fieldQuery.applicationFunctions.some(item => field.applicationFunctions?.includes(item as '应归档数据' | '移交'))
  const attrMatched = !fieldQuery.extAttributes.length || Boolean(field.extAttribute && fieldQuery.extAttributes.includes(field.extAttribute))
  const queryMatched = !fieldQuery.queryFlags.length || fieldQuery.queryFlags.includes(field.queryFlag)
  const requiredMatched = !fieldQuery.requiredFlags.length || fieldQuery.requiredFlags.includes(field.requiredFlag)
  const enabledMatched = !fieldQuery.enabledFlags.length || fieldQuery.enabledFlags.includes(field.enabledFlag)
  return appMatched && attrMatched && queryMatched && requiredMatched && enabledMatched
}))
const queryExtAttributeOptions = computed(() =>
  activeScope.value === 'BASIC'
    ? [...BASIC_TEXT_ATTRS, ...BASIC_NUMBER_ATTRS, ...BASIC_DATE_ATTRS, ...BASIC_DATETIME_ATTRS]
    : [...ATTACHMENT_TEXT_ATTRS, ...ATTACHMENT_NUMBER_ATTRS, ...ATTACHMENT_DATE_ATTRS, ...ATTACHMENT_DATETIME_ATTRS]
)
const availableExtAttributeOptions = computed(() => {
  if (fieldForm.fieldScope === 'BASIC') {
    if (fieldForm.dataType === 'NUMBER') return BASIC_NUMBER_ATTRS
    if (fieldForm.dataType === 'DATE') return BASIC_DATE_ATTRS
    if (fieldForm.dataType === 'DATETIME') return BASIC_DATETIME_ATTRS
    return BASIC_TEXT_ATTRS
  }
  if (fieldForm.dataType === 'NUMBER') return ATTACHMENT_NUMBER_ATTRS
  if (fieldForm.dataType === 'DATE') return ATTACHMENT_DATE_ATTRS
  if (fieldForm.dataType === 'DATETIME') return ATTACHMENT_DATETIME_ATTRS
  return ATTACHMENT_TEXT_ATTRS
})
const pagedFields = computed(() => {
  const start = (fieldPagination.currentPage - 1) * fieldPagination.pageSize
  return filteredFields.value.slice(start, start + fieldPagination.pageSize)
})
const securityLevelNameByCode = computed(() =>
  Object.fromEntries(securityLevelOptions.value.map((item) => [item.code, item.name]))
)

function createEmptyFieldQuery(): FieldQueryState {
  return {
    applicationFunctions: [],
    extAttributes: [],
    queryFlags: [],
    requiredFlags: [],
    enabledFlags: []
  }
}

function formatModuleLabel(moduleCode?: string) {
  if (!moduleCode) return '-'
  const module = flatten(rawTreeData.value).find(item => item.moduleCode === moduleCode)
  return module ? `${module.moduleCode}（${module.moduleName}）` : moduleCode
}

function expandModuleCodesWithDescendants(codes: string[]) {
  const moduleMap = new Map(flatten(rawTreeData.value).map(node => [node.moduleCode, node]))
  const expandedCodes = new Set(codes)
  codes.forEach(code => {
    const node = moduleMap.get(code)
    if (!node) return
    flatten(node.children || []).forEach(child => expandedCodes.add(child.moduleCode))
  })
  return Array.from(expandedCodes)
}

function handleModuleQueryCodesChange(value: string[]) {
  moduleQuery.moduleCodes = expandModuleCodesWithDescendants(value)
}

function isExtAttributeUsed(attribute: string) {
  return usedExtAttributes.value.has(attribute)
}

function isDescendantModule(candidate: BusinessModuleNode, moduleCode?: string) {
  if (!moduleCode || !candidate.ancestorPath) return false
  return candidate.ancestorPath.split('/').includes(moduleCode)
}

function setAllModuleNodesExpanded(expanded: boolean) {
  const nodesMap = moduleTreeRef.value?.store?.nodesMap
  if (!nodesMap) return
  Object.values(nodesMap).forEach((node: any) => {
    node.expanded = expanded
  })
  areModuleNodesExpanded.value = expanded
}

function toggleAllModuleNodes() {
  setAllModuleNodesExpanded(!areModuleNodesExpanded.value)
}

function normalizeTreeCurrentPage() {
  const maxPage = Math.max(1, Math.ceil(treeData.value.length / treePagination.pageSize))
  if (treePagination.currentPage > maxPage) treePagination.currentPage = maxPage
}

function handleTreePageSizeChange() {
  treePagination.currentPage = 1
}

function handleTreePageChange() {
  areModuleNodesExpanded.value = true
}

function normalizeFieldCurrentPage() {
  const maxPage = Math.max(1, Math.ceil(filteredFields.value.length / fieldPagination.pageSize))
  if (fieldPagination.currentPage > maxPage) fieldPagination.currentPage = maxPage
}

function resolveSecurityLevelName(codeOrName?: string) {
  const text = String(codeOrName || '').trim()
  if (!text) return ''
  return securityLevelNameByCode.value[text] || text
}

function resolveSecurityLevelCode(codeOrName?: string) {
  const text = String(codeOrName || '').trim()
  if (!text) return 'INTERNAL_PUBLIC'
  const byCode = securityLevelOptions.value.find((item) => item.code === text)
  if (byCode) return byCode.code
  const byName = securityLevelOptions.value.find((item) => item.name === text)
  if (byName) return byName.code
  return text
}

function resolveUserDisplayById(userId?: number) {
  if (!userId || userId <= 0) return '-'
  return userDisplayById.value[userId] || String(userId)
}

function isKnownSecurityLevel(value?: string) {
  const text = String(value || '').trim()
  if (!text) return true
  return securityLevelOptions.value.some((item) => item.code === text || item.name === text)
}

function handleFieldPageSizeChange() {
  fieldPagination.currentPage = 1
}

function handleFieldPageChange() {
  normalizeFieldCurrentPage()
}

async function loadTree() {
  const [tree, options] = await Promise.all([fetchBusinessModuleTree(), fetchBusinessModuleParentOptions()])
  rawTreeData.value = tree
  parentOptionSource.value = options
  normalizeTreeCurrentPage()
  await syncSelectedNodeWithFilteredTree()
}

async function loadSecurityLevels() {
  const items = await fetchDictionaryItems('SECURITY_LEVEL')
  securityLevelOptions.value = items.map((item) => ({
    code: item.itemCode,
    name: item.itemName
  }))
}

async function loadUserDisplays() {
  const users = await fetchUsers()
  userDisplayById.value = Object.fromEntries(
    users.map((u) => [
      Number(u.userId),
      `${u.userName || u.username || `用户-${u.userId}`}${u.employeeNo ? ` ${u.employeeNo}` : ''}`
    ])
  )
}

async function syncSelectedNodeWithFilteredTree() {
  const nodes = flatten(treeData.value)
  const syncedSelection = selectedNode.value ? nodes.find(item => item.moduleCode === selectedNode.value?.moduleCode) : undefined
  if (hasManualModuleSelection.value && selectedNode.value && !syncedSelection) {
    hasManualModuleSelection.value = false
  }
  selectedNode.value = syncedSelection || nodes[0]
  if (!selectedNode.value) selectedNode.value = nodes[0]
  await loadFields()
}

async function loadDocumentTypeOptions() {
  documentTypeTree.value = await fetchDocumentTypeTree()
}

function handleRootModuleCodeChange(value: string) {
  const matched = documentTypeOptions.value.find(item => item.typeCode === value)
  if (matched && !moduleForm.moduleName.trim()) {
    moduleForm.moduleName = matched.typeName
  }
}

function filterModuleTree(nodes: BusinessModuleNode[]): BusinessModuleNode[] {
  return nodes
    .map(node => {
      const children = filterModuleTree(node.children || [])
      const moduleMatched = !moduleQuery.moduleCodes.length || moduleQuery.moduleCodes.includes(node.moduleCode)
      const integrationMatched = !moduleQuery.integrationType || node.integrationType === moduleQuery.integrationType
      const enabledMatched = !moduleQuery.enabledFlag || node.enabledFlag === moduleQuery.enabledFlag
      const selfMatched = moduleMatched && integrationMatched && enabledMatched
      return selfMatched || children.length ? { ...node, children } : undefined
    })
    .filter((node): node is BusinessModuleNode => Boolean(node))
}

async function applyModuleQuery() {
  selectedNode.value = undefined
  hasManualModuleSelection.value = false
  treePagination.currentPage = 1
  await syncSelectedNodeWithFilteredTree()
}

async function resetModuleQuery() {
  moduleQuery.moduleCodes = []
  moduleQuery.integrationType = ''
  moduleQuery.enabledFlag = ''
  selectedNode.value = undefined
  hasManualModuleSelection.value = false
  treePagination.currentPage = 1
  await syncSelectedNodeWithFilteredTree()
}

async function loadFields() {
  if (!selectedNode.value) {
    fields.value = []
    usedExtAttributes.value = new Set()
    return
  }
  fields.value = await fetchBusinessModuleExtFields(selectedNode.value.moduleCode, activeScope.value)
  fieldPagination.currentPage = 1
  await loadUsedExtAttributes()
  normalizeFieldCurrentPage()
}

async function loadUsedExtAttributes() {
  if (!selectedNode.value) {
    usedExtAttributes.value = new Set()
    return
  }
  const relatedCodes = getRelatedModuleCodes(selectedNode.value)
  const fieldGroups = await Promise.all(relatedCodes.map(moduleCode => fetchBusinessModuleExtFields(moduleCode, activeScope.value)))
  usedExtAttributes.value = new Set(
    fieldGroups
      .flat()
      .map(field => field.extAttribute)
      .filter((attribute): attribute is string => Boolean(attribute))
  )
}

function getRelatedModuleCodes(node: BusinessModuleNode) {
  const ancestorCodes = (node.ancestorPath || '').split('/').map(item => item.trim()).filter(Boolean)
  const descendantCodes = flatten(node.children || []).map(item => item.moduleCode)
  return Array.from(new Set([...ancestorCodes, node.moduleCode, ...descendantCodes]))
}

function copyFieldQuery(target: FieldQueryState, source: FieldQueryState) {
  target.applicationFunctions = [...source.applicationFunctions]
  target.extAttributes = [...source.extAttributes]
  target.queryFlags = [...source.queryFlags]
  target.requiredFlags = [...source.requiredFlags]
  target.enabledFlags = [...source.enabledFlags]
}

function applyFieldQuery() {
  copyFieldQuery(fieldQuery, fieldQueryForm)
  fieldPagination.currentPage = 1
  normalizeFieldCurrentPage()
}

function resetFieldQuery() {
  const empty = createEmptyFieldQuery()
  copyFieldQuery(fieldQueryForm, empty)
  copyFieldQuery(fieldQuery, empty)
  fieldPagination.currentPage = 1
  normalizeFieldCurrentPage()
}

async function selectNode(node: BusinessModuleNode) {
  selectedNode.value = node
  hasManualModuleSelection.value = true
  await loadFields()
}

function resetModuleForm(parentCode = '') {
  moduleForm.moduleCode = ''
  moduleForm.moduleName = ''
  moduleForm.parentCode = parentCode
  moduleForm.enabledFlag = 'Y'
  moduleForm.sortOrder = 1
  moduleForm.securityLevelCode = 'INTERNAL_PUBLIC'
  moduleForm.integrationType = '不集成'
  moduleForm.description = ''
  moduleForm.remark = ''
}

function formatModuleCsvRow(module: BusinessModuleNode) {
  return [
    module.moduleCode,
    module.moduleName,
    module.parentCode || '',
    String(module.levelNum || ''),
    String(module.sortOrder || ''),
    resolveSecurityLevelName(module.securityLevelCode || module.securityLevel || 'INTERNAL_PUBLIC'),
    module.integrationType || '不集成',
    module.enabledFlag === 'Y' ? '启用' : '停用',
    module.description || '',
    module.remark || '',
    module.lastUpdateDate || ''
  ]
}

function escapeCsvCell(value: string) {
  const text = value ?? ''
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
}

function downloadCsv(filename: string, rows: string[][]) {
  const csv = rows.map(row => row.map(escapeCsvCell).join(',')).join('\r\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function exportModules() {
  const rows = [moduleImportExportHeaders as unknown as string[], ...flatten(treeData.value).map(formatModuleCsvRow)]
  downloadCsv(`业务模块_${new Date().toISOString().slice(0, 10)}.csv`, rows)
}

function triggerModuleImport() {
  moduleImportInputRef.value?.click()
}

function parseCsv(text: string) {
  const rows: string[][] = []
  let row: string[] = []
  let cell = ''
  let quoted = false
  for (let index = 0; index < text.length; index += 1) {
    const char = text[index]
    const next = text[index + 1]
    if (quoted) {
      if (char === '"' && next === '"') {
        cell += '"'
        index += 1
      } else if (char === '"') {
        quoted = false
      } else {
        cell += char
      }
      continue
    }
    if (char === '"') {
      quoted = true
    } else if (char === ',') {
      row.push(cell)
      cell = ''
    } else if (char === '\n') {
      row.push(cell)
      rows.push(row)
      row = []
      cell = ''
    } else if (char !== '\r') {
      cell += char
    }
  }
  row.push(cell)
  rows.push(row)
  return rows.filter(item => item.some(cellValue => cellValue.trim()))
}

function normalizeImportedFlag(value: string): 'Y' | 'N' {
  const normalized = value.trim().toUpperCase()
  return normalized === 'N' || value.trim() === '停用' || value.trim() === '否' ? 'N' : 'Y'
}

function getImportValue(record: Record<string, string>, header: string) {
  return record[header]?.trim() || ''
}

async function importModules(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const rows = parseCsv(await file.text())
    const [headers, ...dataRows] = rows
    if (!headers?.length || !dataRows.length) {
      ElMessage.warning('导入文件没有可处理的数据')
      return
    }
    const records = dataRows.map(row => Object.fromEntries(headers.map((header, index) => [header.trim().replace(/^\uFEFF/, ''), row[index] || ''])))
      .sort((a, b) => Number(getImportValue(a, '层级') || 0) - Number(getImportValue(b, '层级') || 0))
    const existingModules = new Set(flatten(rawTreeData.value).map(module => module.moduleCode))
    let createdCount = 0
    let updatedCount = 0
    for (const record of records) {
      const rowNo = records.indexOf(record) + 2
      const moduleCode = getImportValue(record, '业务模块编码')
      const moduleName = getImportValue(record, '业务模块名称')
      if (!moduleCode || !moduleName) {
        throw new Error('业务模块编码、业务模块名称不能为空')
      }
      const securityRaw = getImportValue(record, '密级')
      if (!isKnownSecurityLevel(securityRaw)) {
        throw new Error(`第 ${rowNo} 行密级无效：请填写字典编码或名称`)
      }
      const parentCode = getImportValue(record, '上级业务模块编码')
      const payload = {
        moduleCode,
        moduleName,
        parentCode: parentCode || undefined,
        enabledFlag: normalizeImportedFlag(getImportValue(record, '启用标志')),
        sortOrder: Number(getImportValue(record, '排序') || 1),
        securityLevelCode: resolveSecurityLevelCode(securityRaw || 'INTERNAL_PUBLIC'),
        integrationType: (getImportValue(record, '集成类型') || '不集成') as BusinessModuleCommand['integrationType'],
        description: getImportValue(record, '描述'),
        remark: getImportValue(record, '备注')
      }
      if (existingModules.has(moduleCode)) {
        await updateBusinessModule(moduleCode, payload)
        updatedCount += 1
      } else {
        await createBusinessModule(payload)
        existingModules.add(moduleCode)
        createdCount += 1
      }
    }
    await loadTree()
    ElMessage.success(`导入完成：新增 ${createdCount} 条，更新 ${updatedCount} 条`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败')
  } finally {
    input.value = ''
  }
}

function openModuleDialog(parentCode = '') {
  moduleMode.value = 'create'
  resetModuleForm(parentCode)
  moduleDialogVisible.value = true
}

function openAddModuleDialog() {
  openModuleDialog(hasManualModuleSelection.value ? selectedNode.value?.moduleCode || '' : '')
}

function openEditDialog() {
  if (!selectedNode.value) return
  moduleMode.value = 'edit'
  moduleForm.moduleCode = selectedNode.value.moduleCode
  moduleForm.moduleName = selectedNode.value.moduleName
  moduleForm.parentCode = selectedNode.value.parentCode || ''
  moduleForm.enabledFlag = selectedNode.value.enabledFlag
  moduleForm.sortOrder = selectedNode.value.sortOrder || 1
  moduleForm.securityLevelCode = resolveSecurityLevelCode(selectedNode.value.securityLevelCode || selectedNode.value.securityLevel || 'INTERNAL_PUBLIC')
  moduleForm.integrationType = selectedNode.value.integrationType || '不集成'
  moduleForm.description = selectedNode.value.description || ''
  moduleForm.remark = selectedNode.value.remark || ''
  moduleDialogVisible.value = true
}

async function saveModule() {
  if (!moduleForm.moduleName.trim()) return ElMessage.warning('请输入业务模块名称')
  if (moduleMode.value === 'create' && !moduleForm.moduleCode.trim()) return ElMessage.warning('请输入业务模块编码')
  if (moduleMode.value === 'create') {
    selectedNode.value = await createBusinessModule({ ...moduleForm, moduleCode: moduleForm.moduleCode.trim(), moduleName: moduleForm.moduleName.trim(), parentCode: moduleForm.parentCode || undefined })
  } else {
    selectedNode.value = await updateBusinessModule(moduleForm.moduleCode, { moduleName: moduleForm.moduleName.trim(), parentCode: moduleForm.parentCode || undefined, enabledFlag: moduleForm.enabledFlag, sortOrder: moduleForm.sortOrder, securityLevelCode: moduleForm.securityLevelCode, integrationType: moduleForm.integrationType, description: moduleForm.description, remark: moduleForm.remark })
  }
  moduleDialogVisible.value = false
  await loadTree()
  ElMessage.success('保存成功')
}

async function removeModule() {
  if (!selectedNode.value) return
  await ElMessageBox.confirm(`确认删除业务模块 ${selectedNode.value.moduleName} 吗？`, '提示', { type: 'warning' })
  await deleteBusinessModule(selectedNode.value.moduleCode)
  selectedNode.value = undefined
  hasManualModuleSelection.value = false
  await loadTree()
  ElMessage.success('删除成功')
}

function resetFieldForm() {
  fieldForm.fieldCode = ''
  fieldForm.fieldScope = activeScope.value
  fieldForm.applicationFunctions = []
  fieldForm.extAttribute = undefined
  fieldForm.fieldName = ''
  fieldForm.englishFieldName = ''
  fieldForm.dataType = 'TEXT'
  fieldForm.queryFlag = 'N'
  fieldForm.requiredFlag = 'N'
  fieldForm.enabledFlag = 'Y'
  fieldForm.sortOrder = 1
  editingFieldCode.value = ''
}

watch(
  () => [fieldForm.fieldScope, fieldForm.dataType],
  () => {
    if (!fieldForm.extAttribute) return
    if (!availableExtAttributeOptions.value.includes(fieldForm.extAttribute)) {
      fieldForm.extAttribute = undefined
    }
  }
)

function openFieldDialog(field?: BusinessModuleExtField) {
  if (!selectedNode.value) return ElMessage.warning('请先选择业务模块')
  fieldMode.value = field ? 'edit' : 'create'
  resetFieldForm()
  if (field) {
    editingFieldCode.value = field.fieldCode
    fieldForm.fieldCode = field.fieldCode
    fieldForm.fieldScope = field.fieldScope
    fieldForm.applicationFunctions = field.applicationFunctions || []
    fieldForm.extAttribute = field.extAttribute
    fieldForm.fieldName = field.fieldName
    fieldForm.englishFieldName = field.englishFieldName || ''
    fieldForm.dataType = field.dataType
    fieldForm.queryFlag = field.queryFlag
    fieldForm.requiredFlag = field.requiredFlag
    fieldForm.enabledFlag = field.enabledFlag
    fieldForm.sortOrder = field.sortOrder
  }
  fieldDialogVisible.value = true
}

async function saveField() {
  if (!selectedNode.value) return
  if (!fieldForm.applicationFunctions?.length) return ElMessage.warning('请选择应用功能')
  if (!fieldForm.extAttribute) return ElMessage.warning('请选择扩展字段')
  if (!fieldForm.fieldName.trim()) return ElMessage.warning('请输入字段名')
  if (!fieldForm.englishFieldName?.trim()) return ElMessage.warning('请输入字段编码')
  const submittedFieldCode = fieldMode.value === 'create' ? fieldForm.englishFieldName.trim() : editingFieldCode.value
  const payload = {
    ...fieldForm,
    fieldCode: submittedFieldCode,
    fieldName: fieldForm.fieldName.trim(),
    englishFieldName: fieldForm.englishFieldName?.trim()
  }
  if (fieldMode.value === 'create') {
    await createBusinessModuleExtField(selectedNode.value.moduleCode, payload)
  } else {
    await updateBusinessModuleExtField(selectedNode.value.moduleCode, editingFieldCode.value, payload)
  }
  fieldDialogVisible.value = false
  activeScope.value = fieldForm.fieldScope
  await loadFields()
  ElMessage.success('字段保存成功')
}

async function removeField(field: BusinessModuleExtField) {
  if (!selectedNode.value) return
  await ElMessageBox.confirm(`确认删除扩展字段 ${field.fieldName} 吗？`, '提示', { type: 'warning' })
  await deleteBusinessModuleExtField(selectedNode.value.moduleCode, field.fieldCode)
  await loadFields()
  ElMessage.success('字段已删除')
}

onMounted(async () => {
  await Promise.all([loadSecurityLevels(), loadUserDisplays(), loadTree(), loadDocumentTypeOptions()])
})
</script>

<style scoped>
.business-module-page { display: grid; gap: 20px; }
.module-hero { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 24px; border-radius: 24px; color: #fff; background: linear-gradient(135deg, #0f2f4c 0%, #1b6c8c 58%, #e9f8f2 58%, #f7fffb 100%); box-shadow: 0 18px 40px rgba(15, 47, 76, .14); }
.module-hero span { font-size: 12px; letter-spacing: .08em; text-transform: uppercase; opacity: .85; }
.module-hero h2 { margin: 8px 0; font-size: 30px; }
.module-hero p { margin: 0; max-width: 620px; color: rgba(255,255,255,.86); }
.query-card { border-radius: 20px; border: 1px solid #dce8ef; }
.module-query { display: grid; grid-template-columns: repeat(4, minmax(150px, 1fr)) auto; gap: 12px; align-items: end; }
.query-actions { display: flex; gap: 8px; align-items: center; padding-bottom: 18px; }
.module-layout { display: grid; grid-template-columns: 430px minmax(0, 1fr); gap: 18px; }
.tree-card, .detail-stack > .el-card { border-radius: 20px; border: 1px solid #dce8ef; }
.tree-card :deep(.el-card__body) { overflow-x: auto; }
.tree-card :deep(.el-tree) { --el-tree-node-hover-bg-color: transparent; background: transparent; }
.tree-card :deep(.el-tree-node__content) { height: auto; min-height: 70px; padding: 5px 0; align-items: stretch; border-radius: 16px; }
.tree-card :deep(.el-tree-node__content:hover .tree-node) { border-color: #9cc8de; box-shadow: 0 10px 24px rgba(28, 92, 120, .12); transform: translateY(-1px); }
.tree-card :deep(.is-current > .el-tree-node__content .tree-node) { border-color: #1b6c8c; background: linear-gradient(135deg, #f5fbff 0%, #ecf8f3 100%); box-shadow: 0 12px 28px rgba(27, 108, 140, .16); }
.tree-card :deep(.el-tree-node__children) { overflow: visible; }
.detail-stack { display: grid; gap: 18px; }
.card-head { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.tree-title { display: flex; align-items: center; gap: 8px; }
.tree-toggle-button { flex: 0 0 auto; width: 28px; height: 28px; min-height: 28px; padding: 0; }
.tree-toggle-icon { color: #1b6c8c; font-size: 15px; font-weight: 800; line-height: 1; }
.card-title-with-path { display: flex; align-items: center; min-width: 0; gap: 10px; }
.module-path-tag { max-width: min(560px, 52vw); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.tree-node { display: grid; grid-template-columns: minmax(230px, 1fr) auto; align-items: center; gap: 12px; width: 100%; min-width: 320px; margin-right: 8px; padding: 12px 14px; border: 1px solid #e0edf4; border-radius: 16px; background: linear-gradient(135deg, #ffffff 0%, #f8fbfd 100%); transition: border-color .18s ease, box-shadow .18s ease, transform .18s ease, background .18s ease; }
.tree-node-main { min-width: 0; }
.tree-node-title { color: #15344d; font-weight: 800; white-space: normal; line-height: 1.35; word-break: break-all; }
.tree-node-meta { display: flex; flex-wrap: wrap; gap: 6px 8px; color: #7a8b99; font-size: 12px; margin-top: 7px; }
.tree-node-meta span { padding: 2px 7px; border-radius: 999px; background: #f0f5f8; }
.tree-node-side { display: flex; align-items: flex-start; align-self: stretch; padding-top: 2px; }
.function-tag { margin-right: 6px; }
.ext-attribute-option { display: flex; align-items: center; justify-content: space-between; gap: 12px; width: 100%; }
.hidden-file-input { display: none; }
.empty-actions { display: flex; justify-content: center; margin-top: 12px; }
.tree-pagination { display: flex; justify-content: center; margin-top: 14px; }
.field-pagination { display: flex; justify-content: flex-end; margin-top: 14px; }
.field-options { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.field-query { display: grid; grid-template-columns: repeat(6, minmax(120px, 1fr)) auto; gap: 12px; align-items: end; margin: 4px 0 16px; padding: 14px; border: 1px solid #e2edf3; border-radius: 16px; background: #f8fbfd; }
.field-query-actions { display: flex; gap: 8px; align-items: center; padding-bottom: 18px; }
.level-hint { margin-top: 6px; color: #7a8b99; font-size: 12px; line-height: 1.5; }
@media (max-width: 1000px) { .module-layout, .module-query, .field-query { grid-template-columns: 1fr; } .module-hero { align-items: flex-start; flex-direction: column; } .field-options { grid-template-columns: repeat(2, minmax(0, 1fr)); } .query-actions, .field-query-actions { padding-bottom: 0; } }
</style>
