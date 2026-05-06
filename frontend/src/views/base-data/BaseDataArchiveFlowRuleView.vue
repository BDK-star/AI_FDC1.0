<template>
  <div class="archive-flow-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-form :inline="true" :model="query" class="query-form">
          <el-form-item label="关键字">
            <el-input v-model.trim="query.keyword" placeholder="公司、业务模块或文档组织" clearable />
          </el-form-item>
          <el-form-item label="公司">
            <el-select v-model="query.companyProjectCode" placeholder="全部" clearable style="width: 220px">
              <el-option v-for="item in companyProjectOptions" :key="item.code" :label="`${item.code} - ${item.name}`" :value="item.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="文档组织">
            <el-select v-model="query.documentOrganizationCode" placeholder="全部" clearable style="width: 220px" filterable>
              <el-option v-for="item in documentOrganizationOptions" :key="item.code" :label="`${item.code} - ${item.name}`" :value="item.code" />
            </el-select>
          </el-form-item>
          <el-form-item label="启用标识">
            <el-select v-model="query.enabledFlag" placeholder="全部" clearable style="width: 120px">
              <el-option label="启用" value="Y" />
              <el-option label="停用" value="N" />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="toolbar-actions">
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button type="success" @click="startCreate">新建归档流向规则</el-button>
        </div>
      </div>

      <el-alert
        v-if="editor.visible"
        :title="editor.mode === 'create' ? '在当前页面新建归档流向规则' : editor.mode === 'edit' ? `编辑：${form.companyProjectCode}` : `查看：${form.companyProjectCode}`"
        type="info"
        :closable="false"
        class="editor-tip"
      />

      <div v-if="editor.visible" class="editor-panel">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="160px" class="editor-form">
          <el-row :gutter="16">
            <el-col :md="8" :xs="24">
              <el-form-item label="公司" prop="companyProjectCode" required>
                <el-select v-model="form.companyProjectCode" :disabled="editor.mode !== 'create' || isReadonly" placeholder="请选择公司" filterable style="width: 100%">
                  <el-option v-for="item in companyProjectOptions" :key="item.code" :label="`${item.code} - ${item.name}`" :value="item.code" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="业务模块" prop="busiModuleCode" required>
                <el-tree-select
                  v-model="form.busiModuleCode"
                  :data="businessModuleTreeOptions"
                  :props="{ label: 'label', children: 'children', value: 'value' }"
                  :disabled="isReadonly"
                  placeholder="请选择业务模块"
                  check-strictly
                  filterable
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="归档地" prop="archiveDestination">
                <el-cascader
                  v-model="archiveDestinationPath"
                  :options="archiveDestinationOptions"
                  :props="{ value: 'value', label: 'label', children: 'children', emitPath: true, checkStrictly: true }"
                  :disabled="isReadonly"
                  clearable
                  filterable
                  placeholder="请选择国家、省份或城市（任意一级）"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="文档组织" prop="documentOrganizationCode" required>
                <el-select v-model="form.documentOrganizationCode" :disabled="isReadonly" placeholder="请选择文档组织" filterable style="width: 100%">
                  <el-option v-for="item in documentOrganizationOptions" :key="item.code" :label="`${item.code} - ${item.name}`" :value="item.code" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="保存期限（年）" prop="retentionPeriodYears" required>
                <el-input v-model="retentionPeriodInput" :disabled="isReadonly" placeholder="仅支持阿拉伯数字" maxlength="4" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="是否对外展示" prop="externalDisplayFlag" required>
                <el-segmented v-model="form.externalDisplayFlag" :options="yesNoOptions" :disabled="isReadonly" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="默认规则标识" prop="defaultFlag" required>
                <el-segmented v-model="form.defaultFlag" :options="yesNoOptions" :disabled="isReadonly" />
              </el-form-item>
            </el-col>
            <el-col :md="8" :xs="24">
              <el-form-item label="启用标识" prop="enabledFlag" required>
                <el-segmented v-model="form.enabledFlag" :options="enabledOptions" :disabled="isReadonly" />
              </el-form-item>
            </el-col>
            <el-col :md="24" :xs="24">
              <el-form-item label="自定义规则" prop="customRule">
                <el-input v-model.trim="form.customRule" :disabled="isReadonly" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="可选，填写默认规则说明" />
              </el-form-item>
            </el-col>
            <el-col v-if="editor.mode !== 'create'" :md="8" :xs="24">
              <el-form-item label="最后更新时间">
                <el-input :model-value="editor.lastUpdateDate || '-'" disabled />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="editor-actions">
          <el-button @click="cancelEditor">取消</el-button>
          <el-button v-if="editor.mode === 'view'" type="primary" plain @click="editor.ruleId && startEdit(editor.ruleId)">进入编辑</el-button>
          <el-button v-else type="primary" @click="submit">保存</el-button>
        </div>
      </div>

      <el-table :data="displayedItems" border empty-text="暂无归档流向规则">
        <el-table-column prop="companyProjectCode" label="公司编码" min-width="180" />
        <el-table-column prop="companyProjectName" label="公司名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="busiModuleName" label="业务模块" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customRule" label="自定义匹配条件" min-width="200" show-overflow-tooltip />
        <el-table-column prop="archiveDestinationName" label="归档地" min-width="140" show-overflow-tooltip />
        <el-table-column prop="documentOrganizationName" label="文档组织" min-width="180" show-overflow-tooltip />
        <el-table-column prop="retentionPeriodYears" label="保存期限（年）" width="130" align="center" />
        <el-table-column label="是否可见" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.externalDisplayFlag === 'Y' ? 'success' : 'info'">{{ row.externalDisplayFlag === 'Y' ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认规则" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.defaultFlag === 'Y' ? 'success' : 'info'">{{ row.defaultFlag === 'Y' ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabledFlag === 'Y' ? 'success' : 'info'">{{ row.enabledFlag === 'Y' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastUpdateDate" label="最后更新时间" min-width="180" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="startView(row.id)">查看</el-button>
            <el-button link type="primary" @click="startEdit(row.id)">编辑</el-button>
            <el-popconfirm title="确认软删除该规则吗？" @confirm="removeItem(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-card shadow="never" class="audit-panel">
        <template #header>
          <div class="panel-header simple">
            <div>
              <strong>审计记录</strong>
              <span>通过公共审计模块记录新增、修改、删除</span>
            </div>
          </div>
        </template>
        <el-table :data="auditRecords" size="small">
          <el-table-column prop="operationType" label="操作类型" width="120" />
          <el-table-column prop="businessKey" label="业务主键" width="200" />
          <el-table-column prop="operationSummary" label="摘要" min-width="180" />
          <el-table-column prop="operatorName" label="操作人" width="120" />
          <el-table-column label="操作时间" width="180">
            <template #default="{ row }">{{ formatTime(row.operationTime) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  createArchiveFlowRule,
  deleteArchiveFlowRule,
  fetchArchiveFlowDocumentOrganizationOptions,
  fetchArchiveFlowRuleDetail,
  fetchArchiveFlowRules,
  fetchModuleAudits,
  updateArchiveFlowRule,
  type ArchiveFlowRuleCreateCommand
} from '../../api/modules/archiveFlow'
import { fetchBusinessModuleTree } from '../../api/modules/businessModule'
import { fetchCompanyInfos } from '../../api/modules/companyInfo'
import { fetchCountryRegions } from '../../api/modules/countryRegion'
import {
  buildArchiveDestinationCascaderOptions,
  buildArchiveDestinationPath,
  mapBusinessModuleToTreeOption
} from '../../utils/archiveFlowAlignedFieldUtils'
import type {
  ArchiveFlowRuleDetail,
  ArchiveFlowRuleOption,
  ArchiveFlowRuleSummary,
  AuditRecord,
  BusinessModuleNode,
  CountryRegionItem
} from '../../types'

const formRef = ref<FormInstance>()
const items = ref<ArchiveFlowRuleSummary[]>([])
const auditRecords = ref<AuditRecord[]>([])
const companyProjectOptions = ref<ArchiveFlowRuleOption[]>([])
const businessModuleTree = ref<BusinessModuleNode[]>([])
const documentOrganizationOptions = ref<ArchiveFlowRuleOption[]>([])
const countryOptions = ref<CountryRegionItem[]>([])
const provinceOptions = ref<CountryRegionItem[]>([])
const cityOptions = ref<CountryRegionItem[]>([])
const archiveDestinationPath = ref<string[]>([])

const businessModuleTreeOptions = computed(() =>
  (businessModuleTree.value || []).map(mapBusinessModuleToTreeOption)
)

const archiveDestinationOptions = computed(() =>
  buildArchiveDestinationCascaderOptions(
    countryOptions.value,
    provinceOptions.value,
    cityOptions.value
  )
)

const enabledOptions = [
  { label: '启用', value: 'Y' },
  { label: '停用', value: 'N' }
]
const yesNoOptions = [
  { label: '是', value: 'Y' },
  { label: '否', value: 'N' }
]

const query = reactive({
  keyword: '',
  companyProjectCode: '',
  busiModuleCode: '',
  documentOrganizationCode: '',
  enabledFlag: ''
})

const editor = reactive({
  visible: false,
  mode: 'create' as 'create' | 'edit' | 'view',
  ruleId: undefined as number | undefined,
  lastUpdateDate: ''
})

const form = reactive<ArchiveFlowRuleCreateCommand>({
  companyProjectCode: '',
  busiModuleCode: '',
  customRule: '',
  archiveDestination: '',
  documentOrganizationCode: '',
  retentionPeriodYears: 0,
  externalDisplayFlag: 'N',
  defaultFlag: 'N',
  enabledFlag: 'Y'
})

const retentionPeriodInput = ref('0')

watch(retentionPeriodInput, (value) => {
  const clean = (value || '').replace(/\D/g, '')
  if (clean !== value) {
    retentionPeriodInput.value = clean
    return
  }
  form.retentionPeriodYears = clean ? Number(clean) : 0
})

watch(archiveDestinationPath, (path) => {
  if (!path || path.length === 0) {
    form.archiveDestination = ''
    return
  }
  form.archiveDestination = path[path.length - 1] || ''
})

const rules: FormRules<ArchiveFlowRuleCreateCommand> = {
  companyProjectCode: [{ required: true, message: '请选择公司', trigger: 'change' }],
  busiModuleCode: [{ required: true, message: '请选择业务模块', trigger: 'change' }],
  customRule: [{ max: 500, message: '最大长度500', trigger: 'blur' }],
  archiveDestination: [{ max: 64, message: '最大长度64', trigger: 'change' }],
  documentOrganizationCode: [{ required: true, message: '请选择文档组织', trigger: 'change' }],
  retentionPeriodYears: [{
    validator: (_rule, value, callback) => {
      if (!Number.isInteger(value) || value < 0) {
        callback(new Error('请输入大于等于0的阿拉伯数字'))
        return
      }
      callback()
    },
    trigger: 'blur'
  }],
  externalDisplayFlag: [{ required: true, message: '请选择是否对外展示', trigger: 'change' }],
  defaultFlag: [{ required: true, message: '请选择默认规则标识', trigger: 'change' }],
  enabledFlag: [{ required: true, message: '请选择启用标识', trigger: 'change' }]
}

const isReadonly = computed(() => editor.mode === 'view')

const displayedItems = computed(() => {
  if (editor.visible && editor.mode === 'edit' && form.companyProjectCode) {
    return items.value.filter(item => item.companyProjectCode !== form.companyProjectCode)
  }
  return items.value
})

const resetFormState = () => {
  form.companyProjectCode = ''
  form.busiModuleCode = ''
  form.customRule = ''
  form.archiveDestination = ''
  form.documentOrganizationCode = ''
  form.retentionPeriodYears = 0
  form.externalDisplayFlag = 'N'
  form.defaultFlag = 'N'
  form.enabledFlag = 'Y'
  archiveDestinationPath.value = []
  retentionPeriodInput.value = '0'
  editor.ruleId = undefined
  editor.lastUpdateDate = ''
}

const fillForm = (detail: ArchiveFlowRuleDetail) => {
  form.companyProjectCode = detail.companyProjectCode
  form.busiModuleCode = detail.busiModuleCode
  form.customRule = detail.customRule || ''
  form.archiveDestination = detail.archiveDestination || ''
  form.documentOrganizationCode = detail.documentOrganizationCode
  form.retentionPeriodYears = detail.retentionPeriodYears
  form.externalDisplayFlag = detail.externalDisplayFlag
  form.defaultFlag = detail.defaultFlag
  form.enabledFlag = detail.enabledFlag
  archiveDestinationPath.value = buildArchiveDestinationPathForForm(detail.archiveDestination)
  retentionPeriodInput.value = String(detail.retentionPeriodYears)
  editor.ruleId = detail.id
  editor.lastUpdateDate = detail.lastUpdateDate
}

const loadList = async () => {
  items.value = await fetchArchiveFlowRules({
    keyword: query.keyword?.trim() || undefined,
    companyProjectCode: query.companyProjectCode || undefined,
    busiModuleCode: query.busiModuleCode || undefined,
    documentOrganizationCode: query.documentOrganizationCode || undefined,
    enabledFlag: query.enabledFlag || undefined
  })
}

const loadMeta = async () => {
  const [companies, businessModules, organizations, countries] = await Promise.all([
    fetchCompanyInfos({ enabledFlag: 'Y' }),
    fetchBusinessModuleTree(),
    fetchArchiveFlowDocumentOrganizationOptions(),
    fetchCountryRegions({ regionLevel: 'COUNTRY' })
  ])
  const countryCodes = countries.map((item) => item.regionCode).filter(Boolean)
  const provincesNested = await Promise.all(countryCodes.map((countryCode) =>
    fetchCountryRegions({ regionLevel: 'PROVINCE', parentRegionCode: countryCode })
  ))
  const provinces = provincesNested.flat()
  const provinceCodes = provinces.map((item) => item.regionCode).filter(Boolean)
  const citiesNested = await Promise.all(provinceCodes.map((provinceCode) =>
    fetchCountryRegions({ regionLevel: 'CITY', parentRegionCode: provinceCode })
  ))
  const cities = citiesNested.flat()

  companyProjectOptions.value = companies.map((item) => ({ code: item.companyCode, name: item.companyName }))
  businessModuleTree.value = businessModules
  documentOrganizationOptions.value = organizations
  countryOptions.value = countries
  provinceOptions.value = provinces
  cityOptions.value = cities
  resetFormState()
}

const loadAudits = async () => {
  auditRecords.value = await fetchModuleAudits('ARCHIVE_FLOW_RULE')
}

const resetQuery = async () => {
  query.keyword = ''
  query.companyProjectCode = ''
  query.busiModuleCode = ''
  query.documentOrganizationCode = ''
  query.enabledFlag = ''
  await loadList()
}

const startCreate = () => {
  editor.visible = true
  editor.mode = 'create'
  resetFormState()
}

const openDetailInEditor = async (id: number, mode: 'edit' | 'view') => {
  const detail = await fetchArchiveFlowRuleDetail(id)
  editor.visible = true
  editor.mode = mode
  fillForm(detail)
}

const startEdit = async (id: number) => {
  await openDetailInEditor(id, 'edit')
}

const startView = async (id: number) => {
  await openDetailInEditor(id, 'view')
}

const cancelEditor = () => {
  editor.visible = false
  resetFormState()
  formRef.value?.clearValidate()
}

const submit = async () => {
  try {
    const valid = await formRef.value?.validate().catch(() => false)
    if (valid === false) return

    if (editor.mode === 'create') {
      await createArchiveFlowRule({
        companyProjectCode: form.companyProjectCode,
        busiModuleCode: form.busiModuleCode,
        customRule: form.customRule?.trim() || undefined,
        archiveDestination: form.archiveDestination || undefined,
        documentOrganizationCode: form.documentOrganizationCode,
        retentionPeriodYears: form.retentionPeriodYears,
        externalDisplayFlag: form.externalDisplayFlag,
        defaultFlag: form.defaultFlag,
        enabledFlag: form.enabledFlag
      })
      ElMessage.success('归档流向规则创建成功')
    } else {
      if (!editor.ruleId) throw new Error('规则ID缺失，无法更新')
      await updateArchiveFlowRule(editor.ruleId, {
        busiModuleCode: form.busiModuleCode,
        customRule: form.customRule?.trim() || undefined,
        archiveDestination: form.archiveDestination || undefined,
        documentOrganizationCode: form.documentOrganizationCode,
        retentionPeriodYears: form.retentionPeriodYears,
        externalDisplayFlag: form.externalDisplayFlag,
        defaultFlag: form.defaultFlag,
        enabledFlag: form.enabledFlag
      })
      ElMessage.success('归档流向规则更新成功')
    }

    cancelEditor()
    await Promise.all([loadList(), loadAudits()])
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  }
}

const removeItem = async (id: number) => {
  try {
    await deleteArchiveFlowRule(id)
    ElMessage.success('归档流向规则已软删除')
    if (editor.ruleId === id) {
      cancelEditor()
    }
    await Promise.all([loadList(), loadAudits()])
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

const buildArchiveDestinationPathForForm = (storedCode?: string) =>
  buildArchiveDestinationPath(storedCode, countryOptions.value, provinceOptions.value, cityOptions.value)

const formatTime = (value?: string) => value ? value.replace('T', ' ').slice(0, 19) : '-'

onMounted(async () => {
  try {
    await loadMeta()
  } catch (error: any) {
    ElMessage.error(error?.message || '基础字典加载失败')
  }

  try {
    await loadList()
  } catch (error: any) {
    ElMessage.error(error?.message || '列表加载失败')
  }

  try {
    await loadAudits()
  } catch (error: any) {
    ElMessage.error(error?.message || '审计记录加载失败')
  }
})
</script>

<style scoped>
.archive-flow-page { display: grid; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.query-form { flex: 1; }
.toolbar-actions { display: flex; gap: 12px; align-items: flex-start; }
.editor-tip { margin-bottom: 16px; }
.editor-panel {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-light);
}
.editor-form { margin-bottom: 12px; }
.editor-actions { display: flex; justify-content: flex-end; gap: 12px; }
.audit-panel { margin-top: 16px; }
.panel-header.simple { display: flex; align-items: flex-start; }
.panel-header strong { display: block; color: #24324a; font-size: 16px; }
.panel-header span { color: #7a879a; font-size: 12px; }
</style>
