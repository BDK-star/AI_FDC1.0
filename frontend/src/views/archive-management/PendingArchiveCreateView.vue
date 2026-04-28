<template>
  <div class="doc-detail">
    <section class="doc-detail__title">
      <div class="doc-detail__head-row">
        <h1 class="doc-detail__biz-code">{{ headlineTitle }}</h1>
        <div class="doc-detail__head-actions">
          <el-button :disabled="submitting" @click="saveDraft">保存草稿</el-button>
          <el-button type="primary" :loading="submitting" @click="submit">提交</el-button>
          <el-button :disabled="submitting" @click="cancel">取消</el-button>
        </div>
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
      <div v-if="loading"><el-skeleton :rows="12" animated /></div>
      <div v-else class="doc-info-grid">
        <div
          v-for="spec in createBasicSpecs"
          :key="spec.key"
          class="doc-info-item"
          :class="{ 'doc-info-item--full': spec.full }"
        >
          <div class="doc-info-item__label">{{ spec.label }}</div>
          <div class="doc-info-item__value">
            <el-select
              v-if="spec.key === 'documentTypeCode'"
              v-model="form.documentTypeCode"
              filterable
              clearable
              placeholder="请选择文档类型"
              class="doc-edit-control"
            >
              <el-option v-for="t in options.documentTypes" :key="t.code" :label="t.name" :value="t.code" />
            </el-select>
            <el-input
              v-else-if="spec.key === 'businessCode'"
              v-model="form.businessCode"
              clearable
              placeholder="请输入文档业务编码"
              class="doc-edit-control"
            />
            <el-select
              v-else-if="spec.key === 'companyProjectCode'"
              v-model="form.companyProjectCode"
              filterable
              clearable
              placeholder="请选择公司"
              class="doc-edit-control"
            >
              <el-option
                v-for="c in companySelectOptions"
                :key="c.code"
                :label="`${c.code} · ${c.name}`"
                :value="c.code"
              />
            </el-select>
            <el-tree-select
              v-else-if="spec.key === 'archiveTypeCode'"
              v-model="form.archiveTypeCode"
              :data="businessModuleTreeOptions"
              filterable
              clearable
              check-strictly
              default-expand-all
              :render-after-expand="false"
              placeholder="请选择业务模块"
              class="doc-edit-control"
              node-key="moduleCode"
              :props="{ value: 'moduleCode', label: 'queryLabel', children: 'children' }"
            />
            <el-date-picker
              v-else-if="spec.key === 'beginPeriod'"
              v-model="form.beginPeriod"
              type="month"
              value-format="YYYY-MM"
              placeholder="开始档期"
              class="doc-edit-control"
            />
            <el-date-picker
              v-else-if="spec.key === 'endPeriod'"
              v-model="form.endPeriod"
              type="month"
              value-format="YYYY-MM"
              placeholder="可选，不填则与开始档期相同"
              class="doc-edit-control"
            />
            <el-select
              v-else-if="spec.key === 'archiveDestination'"
              v-model="form.archiveDestination"
              filterable
              clearable
              placeholder="请选择归档地"
              class="doc-edit-control"
            >
              <el-option v-for="o in archiveDestinationSelectOptions" :key="o.code" :label="o.name" :value="o.code" />
            </el-select>
            <el-input
              v-else-if="spec.key === 'originPlace'"
              v-model="form.originPlace"
              clearable
              placeholder="产生地（可由责任人工作国家带出）"
              class="doc-edit-control"
            />
            <el-input
              v-else-if="spec.key === 'documentName'"
              v-model="form.documentName"
              clearable
              placeholder="请输入文档名称"
              class="doc-edit-control"
            />
            <el-date-picker
              v-else-if="spec.key === 'documentDate'"
              v-model="form.documentDate"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="文档生成日期"
              class="doc-edit-control"
            />
            <el-input
              v-else-if="spec.key === 'dutyPerson'"
              v-model="form.dutyPerson"
              clearable
              placeholder="登录名，失焦后带出部门与产生地"
              class="doc-edit-control"
              @blur="onDutyPersonBlur"
            />
            <el-input
              v-else-if="spec.key === 'dutyDepartment'"
              v-model="form.dutyDepartment"
              clearable
              placeholder="文档责任部门"
              class="doc-edit-control"
            />
            <el-select
              v-else-if="spec.key === 'carrierTypeCode'"
              v-model="form.carrierTypeCode"
              filterable
              clearable
              placeholder="载体类型"
              class="doc-edit-control"
            >
              <el-option v-for="o in options.carrierTypes" :key="o.code" :label="o.name" :value="o.code" />
            </el-select>
            <el-input
              v-else-if="spec.key === 'sourceSystem'"
              v-model="form.sourceSystem"
              clearable
              placeholder="系统来源"
              class="doc-edit-control"
            />
            <el-select
              v-else-if="spec.key === 'securityLevelCode'"
              v-model="form.securityLevelCode"
              filterable
              clearable
              placeholder="密级"
              class="doc-edit-control"
            >
              <el-option v-for="o in options.securityLevels" :key="o.code" :label="o.name" :value="o.code" />
            </el-select>
            <el-input
              v-else-if="spec.key === 'remark'"
              v-model="form.remark"
              type="textarea"
              :rows="4"
              placeholder="请输入描述"
              class="doc-edit-control doc-edit-control--block"
            />
          </div>
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
            国家、地区部、代表处、公司标签由公司信息自动带出，不可编辑。选择业务模块后，将展示该模块在「业务模块配置」中维护的、应用功能含「应收」的档案扩展字段（BASIC）。
          </div>
        </div>
        <div v-for="row in extCreateRows" :key="row.key" class="doc-info-item">
          <div class="doc-info-item__label">
            <span v-if="row.requiredFlag === 'Y'" class="f02-required">*</span>{{ row.label }}
          </div>
          <div class="doc-info-item__value">
            <template v-if="row.readonly">{{ row.display }}</template>
            <el-date-picker
              v-else-if="row.dataType === 'DATE'"
              v-model="extForm[row.key]"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择日期"
              class="doc-edit-control"
              clearable
            />
            <el-date-picker
              v-else-if="row.dataType === 'DATETIME'"
              v-model="extForm[row.key]"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="请选择日期时间"
              class="doc-edit-control"
              clearable
            />
            <el-input
              v-else-if="row.dataType === 'NUMBER'"
              v-model="extForm[row.key]"
              clearable
              placeholder="请输入数字"
              class="doc-edit-control"
            />
            <el-select
              v-else-if="row.dataType === 'BOOLEAN'"
              v-model="extForm[row.key]"
              clearable
              placeholder="请选择"
              class="doc-edit-control"
            >
              <el-option label="是" value="是" />
              <el-option label="否" value="否" />
            </el-select>
            <el-input
              v-else
              v-model="extForm[row.key]"
              clearable
              :placeholder="row.dataType === 'DICT' ? '字典项（文本）' : '请输入'"
              class="doc-edit-control"
            />
          </div>
        </div>
        <el-empty
          v-if="form.archiveTypeCode.trim() && !receivableBusinessExtFields.length && !loadingReceivableExt"
          description="当前业务模块未配置「应收」档案扩展字段"
          class="ext-empty-hint"
        />
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
        <div class="doc-info-item">
          <div class="doc-info-item__label">文档组织</div>
          <div class="doc-info-item__value">
            <el-input
              :model-value="documentOrganizationDisplay"
              readonly
              disabled
              class="doc-edit-control"
              :placeholder="documentOrganizationPlaceholder"
            />
            <div class="doc-flow-hint">由归档流向规则决定（公司、文档类型、业务模块、归档地匹配后自动带出）</div>
          </div>
        </div>
        <div class="doc-info-item">
          <div class="doc-info-item__label">是否可见</div>
          <div class="doc-info-item__value">
            <el-input v-model="extForm.visibility" clearable placeholder="默认：是" class="doc-edit-control" />
          </div>
        </div>
        <div class="doc-info-item">
          <div class="doc-info-item__label">条码模块</div>
          <div class="doc-info-item__value">
            <el-input v-model="extForm.barcodeModule" clearable placeholder="请输入" class="doc-edit-control" />
          </div>
        </div>
        <div class="doc-info-item">
          <div class="doc-info-item__label">保管状态</div>
          <div class="doc-info-item__value">
            <el-select
              v-model="form.custodyStatus"
              filterable
              clearable
              placeholder="请选择保管状态"
              class="doc-edit-control"
            >
              <el-option v-for="o in options.custodyStatuses" :key="o.code" :label="o.name" :value="o.code" />
            </el-select>
          </div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="doc-section">
      <template #header>
        <div class="doc-section__header">
          <div class="doc-section__title">
            <el-icon><Document /></el-icon>
            <span>本次操作</span>
          </div>
        </div>
      </template>
      <div class="doc-info-grid">
        <div class="doc-info-item doc-info-item--full">
          <div class="doc-info-item__label">备注</div>
          <div class="doc-info-item__value">
            <el-input
              v-model="operationRemark"
              type="textarea"
              :rows="2"
              placeholder="可选，将写入文档操作日志"
              class="doc-edit-control doc-edit-control--block"
            />
          </div>
        </div>
        <div class="doc-info-item doc-info-item--full">
          <div class="doc-info-item__label">补充说明附件</div>
          <div class="doc-info-item__value">
            <el-upload :http-request="handleAuditUpload" :show-file-list="false" multiple>
              <el-button type="primary" plain>上传文件</el-button>
            </el-upload>
            <div v-if="auditAttachments.length" class="audit-att-tags">
              <el-tag
                v-for="(a, i) in auditAttachments"
                :key="a.storageKey + i"
                closable
                class="audit-att-tag"
                @close="auditAttachments.splice(i, 1)"
              >
                {{ a.fileName || a.storageKey }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown, ArrowUp, Box, Collection, Document } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadRequestOptions } from 'element-plus'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  createPendingDocument,
  fetchArchiveCreateOptions,
  getArchiveDetail,
  updatePendingDocument,
  uploadPendingAuditAttachment
} from '../../api/modules/archiveManagement'
import { fetchArchiveRuleMatch } from '../../api/modules/archiveFlow'
import {
  buildModuleQueryTree,
  fetchBusinessModuleTree,
  filterBusinessModuleTreeByDocumentType,
  isModuleCodeWithinDocumentType,
  resolveRootDocumentTypeCodeByModule,
  type ModuleQueryTreeNode
} from '../../api/modules/businessModule'
import { fetchCompanyInfos } from '../../api/modules/companyInfo'
import { fetchCompanyProjectCountries, fetchCompanyProjectDetail } from '../../api/modules/companyProject'
import { fetchUserDutyProfile } from '../../api/modules/security'
import type {
  ArchiveCreateOptions,
  BusinessModuleExtField,
  BusinessModuleNode,
  CompanyInfo,
  CompanyProjectDetail,
  LabelValueOption
} from '../../types'
import { hardCodedExtLabelMap } from './extFieldDisplayConfig'
import {
  COMPANY_SYNC_EXT_KEYS,
  extKeyForBusinessField,
  fetchReceivableBasicExtFields
} from './pendingArchiveExtShared'
import { CURRENT_OPERATOR_USER_ID } from '../../constants/currentUser'

type CreateBasicKey =
  | 'documentTypeCode'
  | 'businessCode'
  | 'companyProjectCode'
  | 'archiveTypeCode'
  | 'beginPeriod'
  | 'endPeriod'
  | 'archiveDestination'
  | 'originPlace'
  | 'documentName'
  | 'documentDate'
  | 'dutyPerson'
  | 'dutyDepartment'
  | 'carrierTypeCode'
  | 'sourceSystem'
  | 'securityLevelCode'
  | 'remark'

const createBasicSpecs: { label: string; key: CreateBasicKey; full?: boolean }[] = [
  { label: '文档类型', key: 'documentTypeCode' },
  { label: '文档业务编码', key: 'businessCode' },
  { label: '公司', key: 'companyProjectCode' },
  { label: '业务模块', key: 'archiveTypeCode' },
  { label: '开始档期', key: 'beginPeriod' },
  { label: '结束档期', key: 'endPeriod' },
  { label: '归档地', key: 'archiveDestination' },
  { label: '产生地', key: 'originPlace' },
  { label: '文档名称', key: 'documentName', full: true },
  { label: '文档生成日期', key: 'documentDate' },
  { label: '归档责任人', key: 'dutyPerson' },
  { label: '文档责任部门', key: 'dutyDepartment' },
  { label: '载体类型', key: 'carrierTypeCode' },
  { label: '系统来源', key: 'sourceSystem' },
  { label: '密级', key: 'securityLevelCode' },
  { label: '描述', key: 'remark', full: true }
]

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const submitting = ref(false)
/** 从「我的草稿」继续编辑时避免 watch 清空已加载的业务模块 */
const bootstrappingDraft = ref(false)
const resumeArchiveId = ref<number | null>(null)
const loadedRetentionYears = ref(10)
const operationRemark = ref('')
const auditAttachments = ref<Array<{ fileId: number; fileName?: string; storageKey?: string; fileSize?: number }>>([])
const companySelectOptions = ref<Array<{ code: string; name: string }>>([])
const businessModuleSourceTree = ref<BusinessModuleNode[]>([])
const businessModuleTreeOptions = ref<ModuleQueryTreeNode[]>([])
const countryNameByCode = ref<Record<string, string>>({})
/** 当前业务模块下、档案 BASIC 且应用功能含「应收」的扩展字段（fdc_business_module_ext_field_t） */
const receivableBusinessExtFields = ref<BusinessModuleExtField[]>([])
const loadingReceivableExt = ref(false)
/** 写入 extForm 的业务模块扩展字段 key，便于切换模块时清理 */
const lastReceivableExtKeys = ref(new Set<string>())

const READONLY_EXT_KEYS = new Set<string>([...COMPANY_SYNC_EXT_KEYS])
/** 切换文档类型时保留：归档区与只读地理信息 */
const PROTECTED_EXT_FORM_KEYS = new Set<string>([
  ...READONLY_EXT_KEYS,
  'visibility',
  'barcodeModule'
])

const options = reactive<ArchiveCreateOptions>({
  companyProjects: [],
  documentTypes: [],
  archiveDestinations: [],
  documentOrganizations: [],
  securityLevels: [],
  carrierTypes: [],
  attachmentTypes: [],
  archiveTypes: [],
  aiModels: [],
  geoCountries: [],
  geoRepOffices: [],
  geoRegions: [],
  custodyStatuses: []
})

const form = reactive({
  documentTypeCode: (typeof route.query.documentTypeCode === 'string' ? route.query.documentTypeCode : '') || '',
  businessCode: '',
  companyProjectCode: '',
  archiveTypeCode: '',
  beginPeriod: '',
  endPeriod: '',
  archiveDestination: '',
  originPlace: '',
  documentName: '',
  documentDate: '',
  dutyPerson: '',
  dutyDepartment: '',
  carrierTypeCode: '',
  sourceSystem: '',
  securityLevelCode: '',
  remark: '',
  documentOrganizationCode: '',
  custodyStatus: ''
})

const extForm = reactive<Record<string, string>>({})

const sectionOpen = reactive({
  ext: true,
  archive: true
})

const headlineTitle = computed(() => {
  if (form.businessCode.trim()) return form.businessCode.trim()
  if (resumeArchiveId.value != null) return '继续完善应归档数据（草稿）'
  return '新建应归档数据'
})

/** 规则匹配返回的归档地可能在标准下拉里不存在，此处存服务侧描述供选项展示 */
const archiveDestinationLabelFromService = ref('')

/** 归档地下拉：与 options 合并，当前值仅含编码时补上「描述（编码）」 */
const archiveDestinationSelectOptions = computed(() => {
  const base = options.archiveDestinations
  const code = form.archiveDestination.trim()
  if (!code) return base
  if (base.some((o) => o.code === code)) return base
  const text = archiveDestinationLabelFromService.value.trim()
  const label = text ? `${text}（${code}）` : code
  return [...base, { code, name: label }]
})

watch(
  [() => form.archiveDestination, () => options.archiveDestinations],
  () => {
    const code = form.archiveDestination.trim()
    if (!code) {
      archiveDestinationLabelFromService.value = ''
      return
    }
    if (options.archiveDestinations.some((o) => o.code === code)) {
      archiveDestinationLabelFromService.value = ''
    }
  },
  { deep: true }
)

/** 文档组织编码由归档流向解析，展示名称+编码 */
const documentOrganizationDisplay = computed(() => {
  const code = form.documentOrganizationCode.trim()
  if (!code) return ''
  const o = options.documentOrganizations.find((x) => x.code === code)
  return o ? `${o.name}（${code}）` : code
})

const documentOrganizationPlaceholder = computed(() => {
  const company = form.companyProjectCode.trim()
  const module = form.archiveTypeCode.trim()
  if (!company) {
    return '请先选择公司'
  }
  if (!module) {
    return '请先选择业务模块'
  }
  return '未匹配到归档流向，请调整归档地或联系管理员配置归档流向规则'
})

const formatCountryExtValue = (raw: unknown) => {
  if (raw === null || raw === undefined) return '-'
  const s = String(raw).trim()
  if (!s) return '-'
  return countryNameByCode.value[s] || s
}

const formatExtCell = (raw: unknown) => {
  if (raw === null || raw === undefined) return '-'
  const s = String(raw).trim()
  return s || '-'
}

type ExtCreateRow = {
  key: string
  label: string
  readonly: boolean
  display: string
  dataType?: BusinessModuleExtField['dataType']
  requiredFlag?: string
}

const extCreateRows = computed((): ExtCreateRow[] => {
  const rows: ExtCreateRow[] = []
  for (const key of COMPANY_SYNC_EXT_KEYS) {
    const raw = extForm[key]
    rows.push({
      key,
      label: hardCodedExtLabelMap[key] || key,
      readonly: true,
      display: key === 'country' ? formatCountryExtValue(raw) : formatExtCell(raw)
    })
  }
  for (const f of receivableBusinessExtFields.value) {
    const key = extKeyForBusinessField(f)
    if (!key) continue
    rows.push({
      key,
      label: f.fieldName || key,
      readonly: false,
      display: '',
      dataType: f.dataType,
      requiredFlag: f.requiredFlag
    })
  }
  return rows
})

const toggleSection = (key: 'ext' | 'archive') => {
  sectionOpen[key] = !sectionOpen[key]
}

const matchOptionCode = (opts: { code: string; name: string }[], raw: string | undefined) => {
  if (!raw) return ''
  const u = String(raw).trim()
  if (!u) return ''
  if (opts.some((o) => o.code === u)) return u
  const byName = opts.find((o) => o.name === u)
  return byName?.code ?? u
}

function flattenModuleQueryNodes(nodes: ModuleQueryTreeNode[]): LabelValueOption[] {
  return nodes.flatMap((n) => [{ code: n.moduleCode, name: n.moduleName }, ...flattenModuleQueryNodes(n.children || [])])
}

const syncBusinessModuleOptionsByDocumentType = (documentTypeCode?: string) => {
  const filtered = filterBusinessModuleTreeByDocumentType(
    businessModuleSourceTree.value,
    documentTypeCode || ''
  )
  businessModuleTreeOptions.value = buildModuleQueryTree(filtered)
  if (
    form.archiveTypeCode &&
    !isModuleCodeWithinDocumentType(
      businessModuleSourceTree.value,
      documentTypeCode || '',
      form.archiveTypeCode
    )
  ) {
    form.archiveTypeCode = ''
  }
}

const applyCompanyInfoToExt = (info: CompanyInfo) => {
  const cc = info.country?.trim() || ''
  if (cc) {
    extForm.country = countryNameByCode.value[cc] || cc
  } else {
    extForm.country = ''
  }
  extForm.repOffice = info.representativeOffice?.trim() || ''
  extForm.region = info.region?.trim() || ''
  extForm.companyTag = info.tags?.length ? info.tags.join('、') : ''
}

const applyCompanyDetailToExt = (detail: CompanyProjectDetail) => {
  const cc = detail.countryCode?.trim() || ''
  if (cc) {
    extForm.country = countryNameByCode.value[cc] || cc
  } else {
    extForm.country = ''
  }
  const rep = options.geoRepOffices.find((o) => o.code === cc)
  const reg = options.geoRegions.find((o) => o.code === cc)
  extForm.repOffice = rep?.name || ''
  extForm.region = reg?.name || detail.managementArea?.trim() || ''
  extForm.companyTag = detail.companyTag?.trim() || ''
}

const applySelectedCompanyExtFields = async () => {
  const code = form.companyProjectCode.trim()
  if (!code) return
  try {
    const list = await fetchCompanyInfos({ companyCodes: [code], enabledFlag: 'Y' })
    const info = list[0]
    if (info) {
      applyCompanyInfoToExt(info)
      return
    }
  } catch {
    /* ignore */
  }
  try {
    const detail = await fetchCompanyProjectDetail(code)
    applyCompanyDetailToExt(detail)
  } catch {
    /* 演示环境可能无该公司详情 */
  }
}

/** 归档地、文档组织、是否可见：按「默认且启用」的归档流向规则匹配 */
const applyArchiveFlowDefaults = async () => {
  const company = form.companyProjectCode.trim()
  const module = form.archiveTypeCode.trim()
  if (!company || !module) return
  try {
    const m = await fetchArchiveRuleMatch({
      companyProjectCode: company,
      busiModuleCode: module,
      archiveDestination: form.archiveDestination.trim() || undefined
    })
    if (!m?.matched) {
      form.archiveDestination = ''
      form.documentOrganizationCode = ''
      archiveDestinationLabelFromService.value = ''
      return
    }
    const dest = (m.archiveDestination || '').trim()
    if (dest) {
      const resolved = matchOptionCode(options.archiveDestinations, dest) || dest
      form.archiveDestination = resolved
      const inStdList = options.archiveDestinations.some((o) => o.code === resolved)
      archiveDestinationLabelFromService.value = inStdList ? '' : (m.archiveDestinationName || '').trim()
    } else {
      archiveDestinationLabelFromService.value = ''
    }
    const org = (m.documentOrganizationCode || '').trim()
    if (org) {
      form.documentOrganizationCode = matchOptionCode(options.documentOrganizations, org) || org
    } else {
      form.documentOrganizationCode = ''
    }
    const vis = (m.visibilityLabel || '').trim()
    if (vis === '是' || vis === '否') {
      extForm.visibility = vis
    }
  } catch {
    /* ignore */
  }
}

const onDutyPersonBlur = async () => {
  const name = form.dutyPerson.trim()
  if (!name) return
  try {
    const p = await fetchUserDutyProfile(name)
    if (!p) return
    if (p.dutyDepartment?.trim()) form.dutyDepartment = p.dutyDepartment.trim()
    const wcc = p.workCountryCode?.trim()
    if (wcc) {
      form.originPlace = countryNameByCode.value[wcc] || wcc
    }
  } catch {
    /* 用户不存在或未配置档案字段 */
  }
}

function clearReceivableExtState() {
  for (const k of lastReceivableExtKeys.value) {
    delete extForm[k]
  }
  lastReceivableExtKeys.value.clear()
  receivableBusinessExtFields.value = []
}

const syncReceivableModuleExtFields = async () => {
  clearReceivableExtState()
  const moduleCode = form.archiveTypeCode.trim()
  if (!moduleCode) return
  loadingReceivableExt.value = true
  try {
    const filtered = await fetchReceivableBasicExtFields(moduleCode)
    receivableBusinessExtFields.value = filtered
    for (const f of filtered) {
      const key = extKeyForBusinessField(f)
      if (!key) continue
      lastReceivableExtKeys.value.add(key)
      if (extForm[key] === undefined || extForm[key] === null) extForm[key] = ''
    }
  } catch (e: unknown) {
    receivableBusinessExtFields.value = []
    ElMessage.error(e instanceof Error ? e.message : '加载业务模块扩展字段失败')
  } finally {
    loadingReceivableExt.value = false
  }
}

const syncExtFieldsForDocType = async () => {
  const preserved: Record<string, string> = {}
  for (const k of PROTECTED_EXT_FORM_KEYS) {
    if (Object.prototype.hasOwnProperty.call(extForm, k)) preserved[k] = extForm[k]
  }
  lastReceivableExtKeys.value.clear()
  receivableBusinessExtFields.value = []
  Object.keys(extForm).forEach((k) => delete extForm[k])
  Object.assign(extForm, preserved)
  if (!form.documentTypeCode.trim()) return
  await syncReceivableModuleExtFields()
}

const onDocumentTypeChanged = async () => {
  syncBusinessModuleOptionsByDocumentType(form.documentTypeCode)
  await syncExtFieldsForDocType()
  await applySelectedCompanyExtFields()
}

watch(
  () => form.documentTypeCode,
  () => {
    if (bootstrappingDraft.value) return
    void onDocumentTypeChanged()
  }
)

watch(
  () => form.archiveTypeCode,
  async (next) => {
    const moduleCode = String(next || '').trim()
    if (moduleCode && form.documentTypeCode.trim()) {
      if (
        !isModuleCodeWithinDocumentType(
          businessModuleSourceTree.value,
          form.documentTypeCode,
          moduleCode
        )
      ) {
        const rootDocType = resolveRootDocumentTypeCodeByModule(businessModuleSourceTree.value, moduleCode)
        if (rootDocType) {
          form.documentTypeCode = rootDocType
          await onDocumentTypeChanged()
          return
        }
        form.archiveTypeCode = ''
        return
      }
    }
    if (moduleCode && !form.documentTypeCode.trim()) {
      const rootDocType = resolveRootDocumentTypeCodeByModule(businessModuleSourceTree.value, moduleCode)
      if (rootDocType) {
        form.documentTypeCode = rootDocType
        await onDocumentTypeChanged()
        return
      }
    }
    if (bootstrappingDraft.value) return
    void syncReceivableModuleExtFields()
  }
)

watch(
  () => form.companyProjectCode,
  async (next) => {
    if (!next?.trim()) {
      for (const k of COMPANY_SYNC_EXT_KEYS) {
        if (k in extForm) extForm[k] = ''
      }
      return
    }
    await applySelectedCompanyExtFields()
  }
)

watch(
  () =>
    [form.companyProjectCode, form.archiveTypeCode, form.archiveDestination] as const,
  () => {
    void applyArchiveFlowDefaults()
  }
)

const toFormDateTime = (value: unknown): string => {
  if (value === null || value === undefined || value === '') return ''
  const text = String(value).trim()
  if (!text) return ''
  const normalized = text.includes('T') ? text : text.replace(' ', 'T')
  const date = new Date(normalized)
  if (Number.isNaN(date.getTime())) return text.slice(0, 19).replace('T', ' ')
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

const loadDraftIntoForm = async (docId: number) => {
  bootstrappingDraft.value = true
  try {
    const record = await getArchiveDetail(docId)
    if (String(record.lifecycleStatus || '').toUpperCase() !== 'DRAFT') {
      ElMessage.warning('仅草稿可从「我的草稿」以创建页形式继续编辑')
      resumeArchiveId.value = null
      await router.replace({ path: '/archive-management/pending-archive/create', query: {} })
      await onDocumentTypeChanged()
      if (!form.documentTypeCode) {
        extForm.visibility = '是'
        extForm.barcodeModule = ''
      }
      await applyArchiveFlowDefaults()
      return
    }
    resumeArchiveId.value = docId
    loadedRetentionYears.value =
      record.retentionPeriodYears != null && record.retentionPeriodYears > 0 ? record.retentionPeriodYears : 10
    form.documentTypeCode = matchOptionCode(options.documentTypes, record.documentTypeCode)
    form.businessCode = (record.businessCode || '').trim()
    form.companyProjectCode = matchOptionCode(companySelectOptions.value, record.companyProjectCode)
    const flatMods = flattenModuleQueryNodes(buildModuleQueryTree(businessModuleSourceTree.value))
    form.archiveTypeCode =
      matchOptionCode(flatMods, record.businessModuleTypeCode) || (record.businessModuleTypeCode || '').trim()
    form.beginPeriod = record.beginPeriod || ''
    form.endPeriod = record.endPeriod || ''
    form.archiveDestination = matchOptionCode(options.archiveDestinations, record.archiveDestination)
    form.originPlace = (record.originPlace || '').trim()
    form.documentName = (record.documentName || '').trim()
    form.documentDate = toFormDateTime(record.documentDate)
    form.dutyPerson = (record.dutyPerson || '').trim()
    form.dutyDepartment = String(record.dutyDepartment ?? '').trim()
    form.carrierTypeCode = matchOptionCode(options.carrierTypes, record.carrierTypeCode)
    form.sourceSystem = (record.sourceSystem || '').trim()
    form.securityLevelCode = matchOptionCode(options.securityLevels, record.securityLevelCode || record.securityLevelName)
    form.remark = (record.remark || '').trim()
    form.documentOrganizationCode = (record.documentOrganizationCode || '').trim()
    form.custodyStatus = matchOptionCode(options.custodyStatuses, record.custodyStatus) || 'UNARCHIVED'
    Object.keys(extForm).forEach((k) => delete extForm[k])
    const ext = record.extValues || {}
    for (const [k, v] of Object.entries(ext)) {
      if (k === 'visibility') continue
      extForm[k] = v != null ? String(v) : ''
    }
    extForm.visibility = String(record.documentVisibility ?? ext.visibility ?? '是').trim() || '是'
    await syncReceivableModuleExtFields()
    await applySelectedCompanyExtFields()
    await applyArchiveFlowDefaults()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载草稿失败')
    resumeArchiveId.value = null
    await router.replace({ path: '/archive-management/pending-archive/create', query: {} })
    await onDocumentTypeChanged()
  } finally {
    bootstrappingDraft.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const [data, countries, companyInfos, moduleTree] = await Promise.all([
      fetchArchiveCreateOptions(),
      fetchCompanyProjectCountries().catch(() => [] as { countryCode: string; countryName: string }[]),
      fetchCompanyInfos({ enabledFlag: 'Y' }).catch(() => [] as CompanyInfo[]),
      fetchBusinessModuleTree().catch((): BusinessModuleNode[] => [])
    ])
    Object.assign(options, data)
    countryNameByCode.value = Object.fromEntries(
      (countries || []).map((c) => [c.countryCode, c.countryName])
    )
    companySelectOptions.value = companyInfos.map((c) => ({ code: c.companyCode, name: c.companyName }))
    businessModuleSourceTree.value = moduleTree
    syncBusinessModuleOptionsByDocumentType(form.documentTypeCode)
    const rawResume =
      typeof route.query.resumeDraftId === 'string'
        ? route.query.resumeDraftId.trim()
        : Array.isArray(route.query.resumeDraftId)
          ? String(route.query.resumeDraftId[0] || '').trim()
          : ''
    if (rawResume && /^\d+$/.test(rawResume)) {
      await loadDraftIntoForm(Number(rawResume))
    } else {
      await onDocumentTypeChanged()
      if (!form.documentTypeCode) {
        extForm.visibility = '是'
        extForm.barcodeModule = ''
      }
      await applyArchiveFlowDefaults()
    }
  } finally {
    loading.value = false
  }
})

const collectExtValues = (): Record<string, string> => {
  const out: Record<string, string> = {}
  for (const [k, v] of Object.entries(extForm)) {
    out[k] = v != null ? String(v) : ''
  }
  return out
}

const handleAuditUpload = async (opt: UploadRequestOptions) => {
  try {
    const ref = await uploadPendingAuditAttachment(opt.file as File)
    if (ref.fileId != null && ref.fileId > 0) {
      auditAttachments.value.push({
        fileId: ref.fileId,
        fileName: ref.fileName,
        storageKey: ref.storageKey,
        fileSize: ref.fileSize
      })
    }
    ElMessage.success('已上传')
    opt.onSuccess?.({} as any)
  } catch (e: unknown) {
    opt.onError?.(e as any)
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  }
}

const resolveRetentionYears = async (): Promise<number> => {
  let retention = 10
  const company = form.companyProjectCode.trim()
  const module = form.archiveTypeCode.trim()
  const dest = form.archiveDestination.trim()
  if (!company || !module) return retention
  try {
    const m = await fetchArchiveRuleMatch({
      companyProjectCode: company,
      busiModuleCode: module,
      archiveDestination: dest || undefined
    })
    if (m?.matched && m.retentionPeriodYears != null && m.retentionPeriodYears > 0) {
      retention = m.retentionPeriodYears
    }
  } catch {
    /* ignore */
  }
  return retention
}

const saveDraft = async () => {
  submitting.value = true
  try {
    const retention =
      resumeArchiveId.value != null ? loadedRetentionYears.value : await resolveRetentionYears()
    const extVals = collectExtValues()
    const payload = {
      operatorUserId: CURRENT_OPERATOR_USER_ID,
      documentTypeCode: form.documentTypeCode.trim(),
      companyProjectCode: form.companyProjectCode.trim(),
      archiveTypeCode: form.archiveTypeCode.trim(),
      businessCode: form.businessCode.trim() || undefined,
      beginPeriod: form.beginPeriod || '',
      endPeriod: form.endPeriod.trim() || undefined,
      archiveDestination: form.archiveDestination.trim() || undefined,
      originPlace: form.originPlace.trim() || undefined,
      documentName: form.documentName,
      documentDate: form.documentDate || '',
      dutyPerson: form.dutyPerson,
      dutyDepartment: form.dutyDepartment.trim() || undefined,
      carrierTypeCode: form.carrierTypeCode.trim() || 'ELECTRONIC',
      sourceSystem: form.sourceSystem.trim() || undefined,
      securityLevelCode: form.securityLevelCode.trim() || 'INTERNAL',
      remark: form.remark.trim() || undefined,
      documentOrganizationCode: form.documentOrganizationCode.trim() || 'DEFAULT',
      retentionPeriodYears: retention,
      custodyStatus: (form.custodyStatus || '').trim() || 'UNARCHIVED',
      submitMode: 'DRAFT' as const,
      operationRemark: operationRemark.value.trim() || undefined,
      auditAttachments: auditAttachments.value.length ? auditAttachments.value : undefined,
      extValues: extVals
    }
    if (resumeArchiveId.value != null) {
      const updated = await updatePendingDocument(resumeArchiveId.value, payload)
      if (updated.retentionPeriodYears != null && updated.retentionPeriodYears > 0) {
        loadedRetentionYears.value = updated.retentionPeriodYears
      }
      ElMessage.success('草稿已保存')
      if (route.query.from === 'workspace-drafts') {
        await router.push('/workspace/my-drafts')
      } else {
        await router.push('/archive-management/pending-archive/query')
      }
      return
    }
    await createPendingDocument(payload)
    ElMessage.success('草稿已保存')
    await router.push('/archive-management/pending-archive/query')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '保存失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

const submit = async () => {
  if (!form.documentTypeCode.trim()) {
    ElMessage.warning('请选择文档类型')
    return
  }
  if (!form.companyProjectCode.trim()) {
    ElMessage.warning('请选择公司')
    return
  }
  if (!form.businessCode.trim()) {
    ElMessage.warning('请填写文档业务编码')
    return
  }
  if (!form.archiveTypeCode.trim()) {
    ElMessage.warning('请选择业务模块')
    return
  }
  if (!form.beginPeriod) {
    ElMessage.warning('请选择开始档期')
    return
  }
  if (!form.documentName.trim()) {
    ElMessage.warning('请填写文档名称')
    return
  }
  if (!form.documentDate) {
    ElMessage.warning('请填写文档生成日期')
    return
  }
  submitting.value = true
  try {
    const retention =
      resumeArchiveId.value != null ? loadedRetentionYears.value : await resolveRetentionYears()
    const extVals = collectExtValues()
    const payload = {
      operatorUserId: CURRENT_OPERATOR_USER_ID,
      documentTypeCode: form.documentTypeCode.trim(),
      companyProjectCode: form.companyProjectCode.trim(),
      archiveTypeCode: form.archiveTypeCode.trim(),
      businessCode: form.businessCode.trim() || undefined,
      beginPeriod: form.beginPeriod,
      endPeriod: form.endPeriod.trim() || undefined,
      archiveDestination: form.archiveDestination.trim() || undefined,
      originPlace: form.originPlace.trim() || undefined,
      documentName: form.documentName.trim(),
      documentDate: form.documentDate,
      dutyPerson: form.dutyPerson,
      dutyDepartment: form.dutyDepartment.trim() || undefined,
      carrierTypeCode: form.carrierTypeCode.trim() || 'ELECTRONIC',
      sourceSystem: form.sourceSystem.trim() || undefined,
      securityLevelCode: form.securityLevelCode.trim(),
      remark: form.remark.trim() || undefined,
      documentOrganizationCode: form.documentOrganizationCode.trim(),
      retentionPeriodYears: retention,
      custodyStatus: (form.custodyStatus || '').trim() || 'UNARCHIVED',
      submitMode: 'SUBMIT' as const,
      operationRemark: operationRemark.value.trim() || undefined,
      auditAttachments: auditAttachments.value.length ? auditAttachments.value : undefined,
      extValues: extVals
    }
    let createdId: number | null = null
    if (resumeArchiveId.value != null) {
      const updated = await updatePendingDocument(resumeArchiveId.value, payload)
      createdId = updated.archiveId ?? resumeArchiveId.value
      ElMessage.success('提交成功')
    } else {
      const created = await createPendingDocument(payload)
      createdId = created.archiveId
      ElMessage.success('创建成功')
    }
    if (createdId != null && Number.isFinite(Number(createdId)) && Number(createdId) > 0) {
      await router.push({
        path: `/archive-management/detail/${encodeURIComponent(String(createdId))}`,
        query: { from: 'pending', docId: String(createdId) }
      })
    } else {
      await router.push('/archive-management/pending-archive/query')
    }
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '提交失败'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

const cancel = () => {
  if (route.query.from === 'workspace-drafts') {
    void router.push('/workspace/my-drafts')
  } else {
    void router.push('/archive-management/pending-archive/query')
  }
}
</script>

<style scoped>
.doc-detail {
  display: grid;
  gap: 16px;
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
.doc-detail__head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
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
.audit-att-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
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
  min-width: 0;
  flex: 1;
}
.doc-edit-control {
  width: 100%;
}
.doc-edit-control--block {
  width: 100%;
}
.doc-muted-hint {
  color: #64748b;
  font-size: 12px;
  line-height: 1.4;
}
.f02-required {
  color: var(--el-color-danger);
  margin-right: 2px;
}
.ext-empty-hint {
  margin-top: 12px;
}
.doc-flow-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}
:deep(.doc-edit-control.el-input),
:deep(.doc-edit-control.el-select),
:deep(.doc-edit-control.el-date-editor),
:deep(.doc-edit-control.el-tree-select) {
  width: 100%;
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
  .doc-detail__head-actions {
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
