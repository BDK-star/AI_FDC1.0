<template>
  <div class="detail-page">
    <el-card shadow="never">
      <div class="header-bar">
        <div>
          <div class="header-title">{{ pageHeading }}</div>
          <div class="header-subtitle">{{ pageSubtitle }}</div>
        </div>
        <div class="header-actions">
          <el-button @click="router.push('/base-data/company-projects')">返回列表</el-button>
          <el-button v-if="mode === 'view' && code" type="primary" plain @click="router.push(`/base-data/company-projects/${code}/edit`)">进入编辑</el-button>
          <el-button v-if="mode !== 'view'" type="primary" @click="submit">保存</el-button>
        </div>
      </div>

      <el-alert
        v-if="!readonly"
        type="info"
        :closable="false"
        class="form-alert"
        title="带 * 的字段为必填项；国家与组织类别均来自后台字典，组织行至少保留一条且组织编码不能重复。"
      />

      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px" class="detail-form" require-asterisk-position="left">
        <el-row :gutter="16">
          <el-col :md="12" :xs="24">
            <el-form-item label="公司编码" prop="companyProjectCode" required>
              <el-input v-model.trim="form.companyProjectCode" :disabled="mode !== 'create' || readonly" placeholder="请输入公司编码" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="公司名称" prop="companyProjectName" required>
              <el-input v-model.trim="form.companyProjectName" :disabled="readonly" placeholder="请输入公司名称" maxlength="128" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="国家" prop="countryCode" required>
              <el-select v-model="form.countryCode" :disabled="readonly" placeholder="请选择国家" style="width: 100%">
                <el-option v-for="item in countries" :key="item.countryCode" :label="item.countryName" :value="item.countryCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="地区部" prop="managementArea" required>
              <el-input v-model.trim="form.managementArea" :disabled="readonly" placeholder="请输入地区部" maxlength="128" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item label="启用标识" prop="enabledFlag" required>
              <el-segmented v-model="form.enabledFlag" :options="flagOptions" :disabled="readonly" />
            </el-form-item>
          </el-col>
          <el-col v-if="mode !== 'create'" :md="12" :xs="24">
            <el-form-item label="最后更新时间">
              <el-input :model-value="detail?.lastUpdateDate ?? '-'" disabled />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-header">
          <div>
            <span>组织行信息</span>
            <small v-if="!readonly" class="section-tip">组织类别、组织编码、组织名称、有效标识均为必填，且组织类别来自后台字典。</small>
          </div>
          <el-button v-if="!readonly" type="primary" plain @click="addLine">新增行</el-button>
        </div>
      </template>

      <el-table :data="form.lines" border empty-text="暂无组织行信息，请新增至少一条组织行">
        <el-table-column label="序号" width="80">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="组织类别" min-width="180">
          <template #default="{ row, $index }">
            <div class="cell-field">
              <el-select v-if="!readonly" v-model="row.orgCategory" placeholder="请选择组织类别">
                <el-option v-for="item in orgCategories" :key="item.categoryCode" :label="item.categoryName" :value="item.categoryCode" />
              </el-select>
              <span v-else>{{ formatOrgCategory(row.orgCategory) }}</span>
              <span v-if="!readonly && rowErrors[$index]?.orgCategory" class="cell-error">{{ rowErrors[$index]?.orgCategory }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="组织编码" min-width="180">
          <template #default="{ row, $index }">
            <div class="cell-field">
              <el-input v-if="!readonly" v-model.trim="row.organizationCode" placeholder="请输入组织编码" maxlength="64" />
              <span v-else>{{ row.organizationCode }}</span>
              <span v-if="!readonly && rowErrors[$index]?.organizationCode" class="cell-error">{{ rowErrors[$index]?.organizationCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="组织名称" min-width="220">
          <template #default="{ row, $index }">
            <div class="cell-field">
              <el-input v-if="!readonly" v-model.trim="row.organizationName" placeholder="请输入组织名称" maxlength="128" />
              <span v-else>{{ row.organizationName }}</span>
              <span v-if="!readonly && rowErrors[$index]?.organizationName" class="cell-error">{{ rowErrors[$index]?.organizationName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="有效标识" width="140">
          <template #default="{ row, $index }">
            <div class="cell-field">
              <el-select v-if="!readonly" v-model="row.validFlag">
                <el-option label="有效" value="Y" />
                <el-option label="无效" value="N" />
              </el-select>
              <el-tag v-else :type="row.validFlag === 'Y' ? 'success' : 'info'">{{ row.validFlag === 'Y' ? '有效' : '无效' }}</el-tag>
              <span v-if="!readonly && rowErrors[$index]?.validFlag" class="cell-error">{{ rowErrors[$index]?.validFlag }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="!readonly" label="操作" width="100" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" :disabled="form.lines.length === 1" @click="removeLine($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="side-card">
      <template #header>审计记录</template>
      <el-empty v-if="filteredAudits.length === 0" description="当前对象暂无审计记录" />
      <el-timeline v-else>
        <el-timeline-item v-for="item in filteredAudits" :key="item.id" :timestamp="item.operationTime" placement="top">
          <div class="audit-item">
            <strong>{{ item.operationSummary }}</strong>
            <span>{{ item.operationType }} / {{ item.operatorName }}</span>
            <p>{{ item.businessKey }}</p>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createCompanyProject,
  fetchCompanyProjectCountries,
  fetchCompanyProjectDetail,
  fetchCompanyProjectOrgCategories,
  fetchModuleAudits,
  updateCompanyProject,
  type CompanyProjectCreateCommand,
  type CompanyProjectLineCommand,
  type CompanyProjectUpdateCommand
} from '../../api/modules/companyProject'
import type { AuditRecord, CompanyProjectDetail, CompanyProjectOrgCategory, CountryOption } from '../../types'

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const code = computed(() => route.params.companyProjectCode as string | undefined)
const mode = computed<'create' | 'view' | 'edit'>(() => {
  if (route.path.endsWith('/edit')) return 'edit'
  if (route.path.endsWith('/view')) return 'view'
  return 'create'
})
const readonly = computed(() => mode.value === 'view')
const pageHeading = computed(() => mode.value === 'create' ? '新建公司' : mode.value === 'edit' ? '编辑公司' : '查看公司')
const pageSubtitle = computed(() => mode.value === 'create' ? '维护公司头信息与组织行信息。' : `当前对象：${code.value ?? '-'}`)
const flagOptions = [
  { label: '启用', value: 'Y' },
  { label: '停用', value: 'N' }
]
const formRules: FormRules<CompanyProjectCreateCommand> = {
  companyProjectCode: [
    { required: true, message: '请输入公司编码', trigger: 'blur' },
    { max: 64, message: '公司编码长度不能超过64', trigger: 'blur' }
  ],
  companyProjectName: [
    { required: true, message: '请输入公司名称', trigger: 'blur' },
    { max: 128, message: '公司名称长度不能超过128', trigger: 'blur' }
  ],
  countryCode: [{ required: true, message: '请选择国家', trigger: 'change' }],
  managementArea: [
    { required: true, message: '请输入地区部', trigger: 'blur' },
    { max: 128, message: '地区部长度不能超过128', trigger: 'blur' }
  ],
  enabledFlag: [{ required: true, message: '请选择启用标识', trigger: 'change' }]
}

const detail = ref<CompanyProjectDetail>()
const countries = ref<CountryOption[]>([])
const orgCategories = ref<CompanyProjectOrgCategory[]>([])
const audits = ref<AuditRecord[]>([])

const form = reactive<CompanyProjectCreateCommand>({
  companyProjectCode: '',
  companyProjectName: '',
  countryCode: '',
  managementArea: '',
  enabledFlag: 'Y',
  lines: []
})

const rowErrors = ref<Record<number, Partial<Record<'orgCategory' | 'organizationCode' | 'organizationName' | 'validFlag', string>>>>({})
const filteredAudits = computed(() => code.value ? audits.value.filter((item) => item.businessKey === code.value) : audits.value)

const emptyLine = (): CompanyProjectLineCommand => ({
  orgCategory: orgCategories.value[0]?.categoryCode ?? '',
  organizationCode: '',
  organizationName: '',
  validFlag: 'Y'
})

const fillForm = (payload: CompanyProjectDetail) => {
  form.companyProjectCode = payload.companyProjectCode
  form.companyProjectName = payload.companyProjectName
  form.countryCode = payload.countryCode
  form.managementArea = payload.managementArea
  form.enabledFlag = payload.enabledFlag
  form.lines = payload.lines.map((line) => ({
    orgCategory: line.orgCategory,
    organizationCode: line.organizationCode,
    organizationName: line.organizationName,
    validFlag: line.validFlag
  }))
}

const validateLines = () => {
  const errors: Record<number, Partial<Record<'orgCategory' | 'organizationCode' | 'organizationName' | 'validFlag', string>>> = {}
  const seenCodes = new Set<string>()
  form.lines.forEach((line, index) => {
    const current: Partial<Record<'orgCategory' | 'organizationCode' | 'organizationName' | 'validFlag', string>> = {}
    if (!line.orgCategory?.trim()) current.orgCategory = '请选择组织类别'
    if (!line.organizationCode?.trim()) {
      current.organizationCode = '请输入组织编码'
    } else if (seenCodes.has(line.organizationCode.trim())) {
      current.organizationCode = '组织编码不能重复'
    } else {
      seenCodes.add(line.organizationCode.trim())
    }
    if (!line.organizationName?.trim()) current.organizationName = '请输入组织名称'
    if (!line.validFlag?.trim()) current.validFlag = '请选择有效标识'
    if (Object.keys(current).length > 0) errors[index] = current
  })
  rowErrors.value = errors
  if (form.lines.length === 0) throw new Error('至少需要维护一条组织信息')
  if (Object.keys(errors).length > 0) throw new Error('请完善组织行中的必填字段')
}

const addLine = () => {
  form.lines.push(emptyLine())
}

const removeLine = (index: number) => {
  if (form.lines.length === 1) {
    ElMessage.warning('至少需要保留一条组织信息')
    return
  }
  form.lines.splice(index, 1)
  validateLinesSilently()
}

const validateLinesSilently = () => {
  try {
    validateLines()
  } catch {
  }
}

const loadMeta = async () => {
  countries.value = await fetchCompanyProjectCountries()
  orgCategories.value = await fetchCompanyProjectOrgCategories()
  if (!form.countryCode && countries.value.length > 0) {
    form.countryCode = countries.value[0].countryCode
  }
  if (form.lines.length === 0) addLine()
}

const loadDetail = async () => {
  if (!code.value || mode.value === 'create') return
  detail.value = await fetchCompanyProjectDetail(code.value)
  fillForm(detail.value)
}

const loadAudits = async () => {
  audits.value = await fetchModuleAudits('COMPANY_PROJECT')
}

const submit = async () => {
  try {
    const valid = await formRef.value?.validate().catch(() => false)
    if (valid === false) throw new Error('请完善头信息中的必填字段')
    validateLines()
    if (mode.value === 'create') {
      const created = await createCompanyProject(form)
      ElMessage.success('公司创建成功')
      router.push(`/base-data/company-projects/${created.companyProjectCode}/view`)
      return
    }
    const payload: CompanyProjectUpdateCommand = {
      companyProjectName: form.companyProjectName,
      countryCode: form.countryCode,
      managementArea: form.managementArea,
      enabledFlag: form.enabledFlag,
      lines: form.lines
    }
    const updated = await updateCompanyProject(code.value!, payload)
    ElMessage.success('公司更新成功')
    router.push(`/base-data/company-projects/${updated.companyProjectCode}/view`)
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  }
}

const formatOrgCategory = (categoryCode: string) => orgCategories.value.find((item) => item.categoryCode === categoryCode)?.categoryName ?? categoryCode

onMounted(async () => {
  await loadMeta()
  await loadDetail()
  await loadAudits()
  validateLinesSilently()
})
</script>

<style scoped>
.detail-page { display: grid; gap: 16px; }
.header-bar { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 16px; flex-wrap: wrap; }
.header-title { font-size: 20px; font-weight: 700; color: #1f2d3d; }
.header-subtitle { margin-top: 4px; color: #7a8899; }
.header-actions { display: flex; gap: 12px; }
.form-alert { margin-bottom: 16px; }
.detail-form { margin-top: 8px; }
.section-header { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.section-tip { display: block; margin-top: 4px; color: #94a3b8; font-size: 12px; }
.side-card { height: 100%; }
.audit-item { display: grid; gap: 4px; }
.audit-item span, .audit-item p { color: #64748b; margin: 0; }
.cell-field { display: grid; gap: 6px; }
.cell-error { color: #f56c6c; font-size: 12px; line-height: 1.2; }
</style>
