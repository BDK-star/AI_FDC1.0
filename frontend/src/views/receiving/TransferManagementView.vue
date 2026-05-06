<template>
  <div class="transfer-page">
    <el-card shadow="never">
      <el-collapse v-model="activePanels" class="transfer-collapse">
        <el-collapse-item name="header">
          <template #title>
            <div class="sub-card__title">申请头</div>
          </template>
          <el-form ref="headerFormRef" :model="headerForm" :rules="headerRules" label-width="120px">
            <div class="header-grid">
            <el-form-item label="申请人">
              <el-input v-model="currentUserName" class="input-w180" disabled />
            </el-form-item>
            <el-form-item label="申请人部门">
              <el-input v-model="currentUserDept" class="input-w180" disabled />
            </el-form-item>
            <el-form-item label="申请日期" prop="applicationDate">
              <el-input :model-value="headerForm.applicationDate" class="input-w180" disabled />
            </el-form-item>
            <el-form-item label="文档类型" prop="documentTypeCode">
              <CommonTreeSelect
                v-model="headerForm.documentTypeCode"
                :data="documentTypeLevel1Tree"
                :props="{ label: 'typeName', value: 'typeCode' }"
                class="input-w180"
                @change="handleHeaderDocTypeChange"
              />
            </el-form-item>
            <el-form-item label="移交方式" prop="applyMethod">
              <el-select v-model="headerForm.applyMethod" class="input-w180" @change="onApplyMethodChanged">
                <el-option v-for="o in applyMethodOptions" :key="o.code" :label="o.name" :value="o.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="邮寄方式" prop="expressType">
              <el-select v-model="headerForm.expressType" class="input-w180" clearable :disabled="headerForm.applyMethod !== 'MAIL'">
                <el-option v-for="o in expressTypeOptions" :key="o.code" :label="o.name" :value="o.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="邮寄单号" prop="expressNumber">
              <el-input v-model="headerForm.expressNumber" class="input-w180" :disabled="headerForm.applyMethod !== 'MAIL'" />
            </el-form-item>
            <el-form-item label="文档接收人" prop="documentRecipient">
              <el-select v-model="headerForm.documentRecipient" class="input-w180" filterable>
                <el-option v-for="u in userOptions" :key="u.id" :label="u.name" :value="u.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="showHandoverFormField" label="移交形式">
              <el-select v-model="headerForm.handoverForm" class="input-w180" clearable>
                <el-option v-for="o in handoverFormOptions" :key="o.code" :label="o.name" :value="o.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="申请描述" class="span-2">
              <el-input v-model="headerForm.applicationDescription" type="textarea" :rows="2" />
            </el-form-item>
            </div>
          </el-form>
        </el-collapse-item>

        <el-collapse-item name="details">
          <template #title>
            <div class="sub-card__title row-between collapse-title">
              <span>申请行</span>
              <div>
                <el-button type="primary" @click.stop="addRow">新增</el-button>
                <el-button type="primary" plain @click.stop="pickerVisible = true">添加待归档数据</el-button>
              </div>
            </div>
          </template>
          <el-table :data="detailRows" border>
          <el-table-column type="index" label="序号" width="64" />
          <el-table-column label="操作" width="160" fixed="left">
            <template #default="{ row, $index }">
              <el-button link type="primary" @click="openAttachmentDialog(row, $index)">上传附件</el-button>
              <el-button link type="danger" @click="removeRow($index)">删除</el-button>
            </template>
          </el-table-column>
          <el-table-column v-if="isFieldVisible('companyProjectCode')" label="公司" min-width="220">
            <template #default="{ row }">
              <template v-if="isRowLocked(row)">{{ rowCompanyLabel(row) }}</template>
              <el-select
                v-else
                v-model="row.companyProjectCode"
                filterable
                clearable
                class="cell-control-full"
                @update:model-value="() => onDetailCompanyChange(row)"
              >
                <el-option
                  v-for="item in companyProjectOptions"
                  :key="item.code"
                  :label="`${item.code} - ${item.name}`"
                  :value="item.code"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column v-if="isFieldVisible('docBusiNo')" label="文档业务编码" min-width="160"><template #default="{ row }"><el-input v-model="row.docBusiNo" :disabled="isRowLocked(row)" @blur="() => onDocBusiNoBlur(row)" /></template></el-table-column>
          <el-table-column v-if="isFieldVisible('docName')" label="文档名称" min-width="180"><template #default="{ row }"><el-input v-model="row.docName" :disabled="isRowLocked(row)" /></template></el-table-column>
          <el-table-column v-if="isFieldVisible('busiModuleCode')" label="业务模块" min-width="260">
            <template #default="{ row }">
              <el-tree-select
                v-model="row.busiModuleCode"
                :data="businessModuleTreeOptions"
                :props="{ label: 'label', children: 'children', value: 'value' }"
                filterable
                clearable
                check-strictly
                default-expand-all
                :render-after-expand="false"
                placeholder="请选择业务模块"
                class="cell-control-full"
                :disabled="isRowLocked(row)"
                @update:model-value="() => onDetailBusiModuleChange(row)"
              />
            </template>
          </el-table-column>
          <el-table-column v-if="isFieldVisible('archPlaceAlpha2Code')" label="归档地" min-width="260">
            <template #default="{ row }">
              <el-cascader
                :model-value="row.archiveDestinationPath ?? []"
                :options="archiveDestinationOptions"
                :props="{ value: 'value', label: 'label', children: 'children', emitPath: true, checkStrictly: true }"
                clearable
                filterable
                placeholder="请选择国家/省份/城市"
                class="cell-control-full"
                @update:model-value="(path: string[]) => onRowArchiveDestinationPathUpdate(row, path)"
              />
            </template>
          </el-table-column>
          <el-table-column v-if="isFieldVisible('documentOrganizationCode')" label="文档组织" min-width="240">
            <template #default="{ row }">
              <el-select v-model="row.documentOrganizationCode" filterable clearable class="cell-control-full" placeholder="请选择文档组织" :disabled="isRowLocked(row)">
                <el-option
                  v-for="item in documentOrganizationOptions"
                  :key="item.code"
                  :label="`${item.code} - ${item.name}`"
                  :value="item.code"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column v-if="isFieldVisible('startArchPeriod')" label="开始档期" min-width="150"><template #default="{ row }"><el-date-picker v-model="row.startArchPeriod" type="month" value-format="YYYY-MM" :disabled="isRowLocked(row)" /></template></el-table-column>
          <el-table-column v-if="isFieldVisible('endArchPeriod')" label="结束档期" min-width="150"><template #default="{ row }"><el-date-picker v-model="row.endArchPeriod" type="month" value-format="YYYY-MM" :disabled="isRowLocked(row)" /></template></el-table-column>
          <el-table-column v-if="showBusiVolumeNoColumn" label="业务册号" min-width="180">
            <template #default="{ row }"><el-input v-model="row.busiVolumeNo" maxlength="120" :disabled="isRowLocked(row)" /></template>
          </el-table-column>
          <el-table-column v-if="isFieldVisible('docGenerationDate')" label="文档生成日期" min-width="150"><template #default="{ row }"><el-date-picker v-model="row.docGenerationDate" type="date" value-format="YYYY-MM-DD" :disabled="isRowLocked(row)" /></template></el-table-column>
          <el-table-column v-if="isFieldVisible('carrierType')" label="载体类型" min-width="140">
            <template #default="{ row }">
              <el-select v-model="row.carrierType" clearable :disabled="isRowLocked(row)">
                <el-option v-for="o in carrierTypeOptions" :key="o.code" :label="o.name" :value="o.code" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column v-if="isFieldVisible('archCopies')" label="份数" min-width="120"><template #default="{ row }"><el-input-number v-model="row.archCopies" :min="1" :precision="0" :disabled="isRowLocked(row)" /></template></el-table-column>
          <el-table-column v-if="isFieldVisible('remark')" label="备注" min-width="150"><template #default="{ row }"><el-input v-model="row.remark" /></template></el-table-column>
          <el-table-column v-if="isFieldVisible('description')" label="描述" min-width="180"><template #default="{ row }"><el-input v-model="row.description" type="textarea" :rows="1" /></template></el-table-column>
          </el-table>
        </el-collapse-item>

        <el-collapse-item name="extInfo">
          <template #title>
            <div class="sub-card__title">扩展信息</div>
          </template>
          <p class="ext-info-hint">以下按申请行展示：请先在该行选择「业务模块」，扩展字段来自业务模块配置（应用功能含「移交」）。</p>
          <template v-for="(row, idx) in detailRows" :key="'ext-row-' + idx">
            <div
              v-if="row.busiModuleCode && (row.extFieldDefs?.length ?? 0) > 0"
              class="ext-block"
            >
              <div class="ext-block__caption">
                第 {{ idx + 1 }} 行 · {{ rowLineSummary(row) }} · 业务模块：{{ busiModuleLabel(row.busiModuleCode) }}
              </div>
              <el-form label-width="140px" class="ext-form-grid">
                <el-form-item
                  v-for="f in row.extFieldDefs"
                  :key="row.busiModuleCode + '-' + f.fieldCode"
                  :label="f.fieldName"
                  :required="f.requiredFlag === 'Y'"
                >
                  <el-input v-model="row.extValues[f.fieldCode]" class="input-ext" clearable :disabled="isRowLocked(row)" />
                </el-form-item>
              </el-form>
            </div>
          </template>
          <el-empty
            v-if="!detailRows.some((r) => r.busiModuleCode && (r.extFieldDefs?.length ?? 0) > 0)"
            description="暂无扩展信息：请在申请行选择业务模块后，将在此处按行展示对应扩展字段"
          />
        </el-collapse-item>
      </el-collapse>

      <div class="footer-actions">
        <el-button @click="cancel">取消</el-button>
        <el-button :loading="saving" @click="saveDraft">保存</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交</el-button>
      </div>
    </el-card>

    <el-dialog v-model="pickerVisible" title="添加待归档数据" width="80%">
      <el-form :inline="true" :model="pickerFilter" class="picker-form">
        <el-form-item label="文档类型"><el-input :model-value="selectedDocumentTypeName || headerForm.documentTypeCode" class="picker-input" disabled /></el-form-item>
        <el-form-item label="文档名称"><el-input v-model="pickerFilter.documentName" /></el-form-item>
        <el-form-item label="业务编码"><el-input v-model="pickerFilter.businessCode" /></el-form-item>
        <el-form-item label="公司">
          <el-select v-model="pickerFilter.companyProjectCode" class="picker-input" clearable filterable>
            <el-option
              v-for="item in companyProjectOptions"
              :key="item.code"
              :label="`${item.code} - ${item.name}`"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="picker-query-item"><el-button type="primary" @click="queryPicker">查询</el-button></el-form-item>
      </el-form>
      <el-table :data="pickerRows" @selection-change="onPickerSelect" border>
        <el-table-column type="selection" width="50" :selectable="isPickerSelectable" />
        <el-table-column prop="businessCode" label="文档业务编码" min-width="160" />
        <el-table-column prop="documentName" label="文档名称" min-width="180" />
        <el-table-column prop="companyProjectCode" label="公司" min-width="120" />
        <el-table-column label="文档类型" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ pickerDocumentTypeLabel(row) }}</template>
        </el-table-column>
        <el-table-column label="业务模块" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ pickerBusinessModuleLabel(row) }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="pickerVisible = false">取消</el-button>
        <el-button type="primary" @click="addFromPicker">添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="attachmentDialogVisible" title="申请行附件" width="760px">
      <div class="row-between" style="margin-bottom: 12px;">
        <el-upload :show-file-list="false" :before-upload="beforeUploadDetailAttachment">
          <el-button type="primary">选择文件并上传</el-button>
        </el-upload>
      </div>
      <el-table :data="detailAttachmentList" border v-loading="attachmentLoading">
        <el-table-column prop="fileName" label="文件名" min-width="260" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="大小(B)" width="120" />
        <el-table-column prop="creationDate" label="上传时间" min-width="180" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="downloadAttachment(row)">下载</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import axios from 'axios'
import CommonTreeSelect from '../../components/CommonTreeSelect.vue'
import {
  fetchBusinessModuleExtFieldsByApplicationFunction,
  fetchDocumentTypeTree
} from '../../api/modules/documentType'
import { fetchArchiveRuleMatch } from '../../api/modules/archiveFlow'
import { fetchArchiveCreateOptions, queryArchives } from '../../api/modules/archiveManagement'
import { fetchBusinessModuleTree, filterBusinessModuleTreeByDocumentType, isModuleCodeWithinDocumentType } from '../../api/modules/businessModule'
import { fetchCompanyInfos } from '../../api/modules/companyInfo'
import { fetchCountryRegions } from '../../api/modules/countryRegion'
import { fetchDictionaryItems } from '../../api/modules/dictionary'
import { fetchUsers } from '../../api/modules/security'
import {
  createTransferApplication,
  downloadTransferApplicationDetailAttachment,
  getTransferApplication,
  listTransferApplicationDetailAttachments,
  updateTransferApplication,
  uploadTransferApplicationDetailAttachment
} from '../../api/modules/transferApplications'
import { getTransferApplyFieldVisibility } from '../../api/modules/transferApplyFieldConfig'
import type {
  ArchiveCreateOptions,
  BusinessModuleNode,
  CountryRegionItem,
  DictionaryItem,
  DocumentTypeTreeNode,
  TransferApplicationCreateCommand,
  TransferApplicationDetailAttachment
} from '../../types'
import type { BusinessModuleExtFieldForApplication } from '../../api/modules/documentType'
import {
  buildArchiveDestinationCascaderOptions,
  buildArchiveDestinationPath,
  findBusinessModuleNameByCode,
  mapBusinessModuleToTreeOption
} from '../../utils/archiveFlowAlignedFieldUtils'

interface LabelOption { code: string; name: string }
interface TransferDetailRow {
  applicationDetailId?: number
  rowSource?: 'MANUAL' | 'PICKER' | 'AUTO_MATCHED'
  lockedByDocument?: boolean
  companyProjectCode: string
  companyProjectName?: string
  docBusiNo: string
  docName: string
  busiModuleCode: string
  archPlaceAlpha2Code: string
  /** 与归档流向规则一致：国家/省/市级联选中路径，末级写入 archPlaceAlpha2Code */
  archiveDestinationPath?: string[]
  documentOrganizationCode: string
  carrierType: string
  endArchPeriod: string
  startArchPeriod: string
  busiVolumeNo?: string
  docGenerationDate?: string
  archCopies: number
  remark?: string
  description?: string
  extValues: Record<string, string>
  /** 当前行业务模块下、应用功能含「移交」的扩展字段定义 */
  extFieldDefs?: BusinessModuleExtFieldForApplication[]
  attachments?: TransferApplicationDetailAttachment[]
}

const DEFAULT_TENANT_ID = 1
const route = useRoute()
const router = useRouter()
const headerFormRef = ref<FormInstance>()
const activePanels = ref(['header', 'details', 'extInfo'])
const submitting = ref(false)
const saving = ref(false)
const savedApplicationId = ref<number | null>(null)
const applicationNumber = ref(`TR-${Date.now()}`)
const pickerVisible = ref(false)
const pickerRows = ref<any[]>([])
const pickerSelection = ref<any[]>([])
const TRANSFER_APPLICATION_FUNCTION = '移交'
const documentTypeTree = ref<DocumentTypeTreeNode[]>([])
const documentTypeLevel1Tree = computed(() => (Array.isArray(documentTypeTree.value) ? documentTypeTree.value : []).map((n) => ({ ...n, children: [] })))
const companyProjectOptions = ref<LabelOption[]>([])
const carrierTypeOptions = ref<LabelOption[]>([])
const businessModuleTree = ref<BusinessModuleNode[]>([])
const countryOptions = ref<CountryRegionItem[]>([])
const provinceOptions = ref<CountryRegionItem[]>([])
const cityOptions = ref<CountryRegionItem[]>([])

const filteredBusinessModuleTree = computed(() =>
  filterBusinessModuleTreeByDocumentType(
    businessModuleTree.value || [],
    String(headerForm.documentTypeCode || '')
  )
)

const businessModuleTreeOptions = computed(() =>
  (filteredBusinessModuleTree.value || []).map(mapBusinessModuleToTreeOption)
)

const archiveDestinationOptions = computed(() =>
  buildArchiveDestinationCascaderOptions(
    countryOptions.value,
    provinceOptions.value,
    cityOptions.value
  )
)

/** 与应归档新建页一致：用于将匹配结果对齐到下拉选项编码 */
const archiveDestinationsFlat = ref<LabelOption[]>([])
const documentOrganizationOptions = ref<LabelOption[]>([])
const userOptions = ref<Array<{ id: number; name: string; dutyDepartment?: string }>>([])
const applyMethodOptions = ref<LabelOption[]>([{ code: 'DIRECT', name: '直接移交' }, { code: 'MAIL', name: '邮寄' }])
const expressTypeOptions = ref<LabelOption[]>([{ code: 'SF', name: '顺丰' }, { code: 'EMS', name: 'EMS' }, { code: 'OTHER', name: '其他' }])
const handoverFormOptions = ref<LabelOption[]>([])
const ACCOUNTING_DOCUMENT_TYPE_CODES = new Set(['ACCOUNT_DOC', 'ACCTOUNT_DOC'])
const attachmentDialogVisible = ref(false)
const attachmentLoading = ref(false)
const currentAttachmentDetailId = ref<number | null>(null)
const currentAttachmentRowIndex = ref<number | null>(null)
const detailAttachmentList = ref<TransferApplicationDetailAttachment[]>([])
const defaultTransferFieldVisibility: Record<string, boolean> = {
  companyProjectCode: true,
  docBusiNo: true,
  docName: true,
  busiModuleCode: true,
  archPlaceAlpha2Code: true,
  documentOrganizationCode: true,
  startArchPeriod: true,
  endArchPeriod: true,
  docGenerationDate: true,
  carrierType: true,
  archCopies: true,
  remark: true,
  description: true
}
const transferFieldVisibility = ref<Record<string, boolean>>({ ...defaultTransferFieldVisibility })

const currentUserId = ref(1)
const currentUserName = ref('张三')
const currentUserDept = ref('财务部')
const headerForm = reactive({
  applicationDate: '',
  documentTypeCode: '',
  applyMethod: 'DIRECT',
  expressType: '',
  expressNumber: '',
  documentRecipient: undefined as number | undefined,
  handoverForm: '',
  applicationDescription: ''
})
const detailRows = ref<TransferDetailRow[]>([])
const pickerFilter = reactive({
  documentName: '',
  businessCode: '',
  companyProjectCode: ''
})

const headerRules: FormRules = {
  documentTypeCode: [{ required: true, message: '请选择文档类型', trigger: 'change' }],
  applyMethod: [{ required: true, message: '请选择移交方式', trigger: 'change' }],
  documentRecipient: [{ required: true, message: '请选择文档接收人', trigger: 'change' }],
  expressType: [{ validator: (_r, v, cb) => (headerForm.applyMethod === 'MAIL' && !v ? cb(new Error('邮寄方式必填')) : cb()), trigger: 'change' }],
  expressNumber: [{ validator: (_r, v, cb) => (headerForm.applyMethod === 'MAIL' && !v ? cb(new Error('邮寄单号必填')) : cb()), trigger: 'blur' }]
}

const showHandoverFormField = computed(() => {
  const normalized = String(headerForm.documentTypeCode || '').trim().toUpperCase()
  return ACCOUNTING_DOCUMENT_TYPE_CODES.has(normalized)
})
const selectedDocumentTypeName = computed(() => findDocumentTypeNameByCode(documentTypeTree.value, headerForm.documentTypeCode))
const selectedHandoverFormLabel = computed(() =>
  handoverFormOptions.value.find((o) => o.code === headerForm.handoverForm)?.name || ''
)
const showBusiVolumeNoColumn = computed(() =>
  String(selectedHandoverFormLabel.value || '').includes('按册移交')
)

function matchOptionCode(opts: LabelOption[], raw: string | undefined) {
  if (!raw) return ''
  const u = String(raw).trim()
  if (!u) return ''
  if (opts.some((o) => o.code === u)) return u
  const byName = opts.find((o) => o.name === u)
  return byName?.code ?? u
}

async function applyArchiveRuleMatchForRow(row: TransferDetailRow) {
  const company = String(row.companyProjectCode || '').trim()
  const module = String(row.busiModuleCode || '').trim()
  if (!company || !module) return
  try {
    const m = await fetchArchiveRuleMatch({
      companyProjectCode: company,
      busiModuleCode: module,
      archiveDestination: String(row.archPlaceAlpha2Code || '').trim() || undefined
    })
    if (!m?.matched) return
    const dest = (m.archiveDestination || '').trim()
    if (dest) {
      const resolved = matchOptionCode(archiveDestinationsFlat.value, dest) || dest
      row.archPlaceAlpha2Code = resolved
      syncRowArchivePathFromCode(row)
    }
    const org = (m.documentOrganizationCode || '').trim()
    if (org) {
      row.documentOrganizationCode = matchOptionCode(documentOrganizationOptions.value, org) || org
    }
  } catch (e) {
    console.error(e)
  }
}

async function onDetailCompanyChange(row: TransferDetailRow) {
  if (row.busiModuleCode) {
    await applyArchiveRuleMatchForRow(row)
  }
}

function isRowLocked(row: TransferDetailRow) {
  return row.lockedByDocument === true
}

function rowCompanyLabel(row: TransferDetailRow) {
  const code = String(row.companyProjectCode || '').trim()
  const byOption = companyProjectOptions.value.find((o) => o.code === code)
  const name = String(row.companyProjectName || byOption?.name || '').trim()
  if (code && name) return `${code} - ${name}`
  if (code) return code
  return name || '-'
}

function ensureCompanyOption(code: string, name?: string) {
  const normalizedCode = String(code || '').trim()
  if (!normalizedCode) return
  const existing = companyProjectOptions.value.find((o) => o.code === normalizedCode)
  if (existing) {
    if (!existing.name && name) existing.name = name
    return
  }
  companyProjectOptions.value.push({ code: normalizedCode, name: String(name || normalizedCode) })
}

function normalizeExtValuesByDefs(
  extValues: Record<string, any> | undefined,
  defs: BusinessModuleExtFieldForApplication[] | undefined
) {
  const normalized: Record<string, string> = {}
  if (!extValues || !defs?.length) return normalized
  defs.forEach((def) => {
    const raw = extValues[def.fieldCode]
    normalized[def.fieldCode] = raw == null ? '' : String(raw)
  })
  return normalized
}

function findBusinessModuleCodeByName(nodes: BusinessModuleNode[], moduleName: string): string {
  const target = String(moduleName || '').trim()
  if (!target) return ''
  for (const node of nodes) {
    if (String(node.moduleName || '').trim() === target) {
      return String(node.moduleCode || '').trim()
    }
    if (node.children?.length) {
      const found = findBusinessModuleCodeByName(node.children, target)
      if (found) return found
    }
  }
  return ''
}

function resolveRowBusinessModuleCodeFromRecord(item: any): string {
  const code = String(item.businessModuleTypeCode || item.busiModuleCode || '').trim()
  if (code) return code
  return findBusinessModuleCodeByName(businessModuleTree.value, String(item.archiveTypeCode || '').trim())
}

async function fillRowFromArchiveRecord(row: TransferDetailRow, item: any, source: 'PICKER' | 'AUTO_MATCHED') {
  const companyCode = String(item.companyProjectCode || '').trim()
  const companyName = String(item.companyProjectName || '').trim()
  row.rowSource = source
  row.lockedByDocument = true
  row.companyProjectCode = companyCode
  row.companyProjectName = companyName
  ensureCompanyOption(companyCode, companyName)
  row.docBusiNo = String(item.businessCode || row.docBusiNo || '').trim()
  row.docName = String(item.documentName || row.docName || '').trim()
  row.busiModuleCode = resolveRowBusinessModuleCodeFromRecord(item) || row.busiModuleCode
  row.archPlaceAlpha2Code = String(item.archiveDestination || row.archPlaceAlpha2Code || '').trim()
  row.documentOrganizationCode = String(item.documentOrganizationCode || row.documentOrganizationCode || '').trim()
  row.startArchPeriod = String(item.beginPeriod || row.startArchPeriod || '').trim()
  row.endArchPeriod = String(item.endPeriod || row.endArchPeriod || '').trim()
  row.docGenerationDate = item.documentDate || row.docGenerationDate || ''
  row.carrierType = String(item.carrierTypeCode || row.carrierType || '').trim()
  row.archCopies = Number(row.archCopies || 1)
  syncRowArchivePathFromCode(row)
  if (row.busiModuleCode) {
    await loadRowExtFields(row, false)
    row.extValues = normalizeExtValuesByDefs(item.extValues || {}, row.extFieldDefs)
  }
}

async function queryArchiveByBusinessCode(docBusiNo: string) {
  const value = String(docBusiNo || '').trim()
  if (!value || !headerForm.documentTypeCode) return null
  const res = await queryArchives({
    documentTypeCode: headerForm.documentTypeCode,
    businessCode: value,
    excludeSubmittedTransferApplied: true
  })
  const records = res.records || []
  if (!records.length) return null
  return records.find((r) => String(r.businessCode || '').trim().toLowerCase() === value.toLowerCase()) || records[0]
}

async function onDocBusiNoBlur(row: TransferDetailRow) {
  if (isRowLocked(row)) return
  const value = String(row.docBusiNo || '').trim()
  if (!value || !headerForm.documentTypeCode) return
  const duplicated = detailRows.value.some((r) => r !== row && String(r.docBusiNo || '').trim() === value)
  if (duplicated) {
    ElMessage.warning('文档业务编码已存在，请勿重复添加')
    return
  }
  try {
    const matched = await queryArchiveByBusinessCode(value)
    if (!matched) return
    await fillRowFromArchiveRecord(row, matched, 'AUTO_MATCHED')
    ElMessage.success(`已自动带出业务编码 ${value} 对应文档信息`)
  } catch (e) {
    console.error(e)
    ElMessage.error(resolveErrorMessage(e, '自动带出文档信息失败'))
  }
}

function addRow() {
  detailRows.value.push({
    rowSource: 'MANUAL',
    lockedByDocument: false,
    companyProjectCode: '',
    companyProjectName: '',
    docBusiNo: '',
    docName: '',
    busiModuleCode: '',
    archPlaceAlpha2Code: '',
    archiveDestinationPath: [],
    documentOrganizationCode: '',
    carrierType: carrierTypeOptions.value[0]?.code || '',
    endArchPeriod: '',
    startArchPeriod: '',
    busiVolumeNo: '',
    docGenerationDate: '',
    archCopies: 1,
    remark: '',
    description: '',
    extValues: {},
    extFieldDefs: []
  })
}

function removeRow(index: number) {
  detailRows.value.splice(index, 1)
}

async function openAttachmentDialog(row: TransferDetailRow, index: number) {
  if (!savedApplicationId.value) {
    ElMessage.warning('请先保存申请后再上传附件')
    return
  }
  const detailId = Number(row.applicationDetailId || 0)
  if (!detailId) {
    ElMessage.warning('请先保存申请后再上传附件')
    return
  }
  currentAttachmentDetailId.value = detailId
  currentAttachmentRowIndex.value = index
  attachmentDialogVisible.value = true
  await loadDetailAttachments()
}

async function loadDetailAttachments() {
  if (!savedApplicationId.value || !currentAttachmentDetailId.value) {
    detailAttachmentList.value = []
    return
  }
  attachmentLoading.value = true
  try {
    detailAttachmentList.value = await listTransferApplicationDetailAttachments(
      savedApplicationId.value,
      currentAttachmentDetailId.value
    )
  } finally {
    attachmentLoading.value = false
  }
}

const beforeUploadDetailAttachment = async (file: File) => {
  if (!savedApplicationId.value || !currentAttachmentDetailId.value) {
    ElMessage.warning('请先保存申请后再上传附件')
    return false
  }
  await uploadTransferApplicationDetailAttachment(savedApplicationId.value, currentAttachmentDetailId.value, file)
  ElMessage.success('附件上传成功')
  await loadDetailAttachments()
  if (currentAttachmentRowIndex.value != null) {
    detailRows.value[currentAttachmentRowIndex.value].attachments = [...detailAttachmentList.value]
  }
  return false
}

async function downloadAttachment(row: TransferApplicationDetailAttachment) {
  if (!savedApplicationId.value || !currentAttachmentDetailId.value) return
  const blob = await downloadTransferApplicationDetailAttachment(
    savedApplicationId.value,
    currentAttachmentDetailId.value,
    row.attachmentId
  )
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = row.fileName
  a.click()
  window.URL.revokeObjectURL(url)
}

function rowLineSummary(row: TransferDetailRow) {
  const name = String(row.docName || '').trim()
  const code = String(row.docBusiNo || '').trim()
  if (name && code) return `${name}（${code}）`
  if (name) return name
  if (code) return code
  return '未填写文档名称/业务编码'
}

function busiModuleLabel(moduleCode: string) {
  const name = findBusinessModuleNameByCode(businessModuleTree.value, moduleCode)
  return name || moduleCode
}

function pickerDocumentTypeLabel(row: any) {
  const byApiName = String(row.documentTypeName || '').trim()
  if (byApiName) return byApiName
  const code = String(row.documentTypeCode || headerForm.documentTypeCode || '').trim()
  if (!code) return '-'
  return findDocumentTypeNameByCode(documentTypeTree.value, code) || code
}

function pickerBusinessModuleLabel(row: any) {
  const moduleCode = String(row.businessModuleTypeCode || row.busiModuleCode || '').trim()
  if (moduleCode) return busiModuleLabel(moduleCode)
  const archiveType = String(row.archiveTypeCode || '').trim()
  return archiveType || '-'
}

function syncRowArchivePathFromCode(row: TransferDetailRow) {
  row.archiveDestinationPath = [
    ...buildArchiveDestinationPath(row.archPlaceAlpha2Code, countryOptions.value, provinceOptions.value, cityOptions.value)
  ]
}

function onRowArchiveDestinationPathUpdate(row: TransferDetailRow, path: string[]) {
  row.archiveDestinationPath = path && path.length ? [...path] : []
  row.archPlaceAlpha2Code = path && path.length ? path[path.length - 1]! : ''
}

function findDocumentTypeNameByCode(nodes: DocumentTypeTreeNode[], code: string): string {
  for (const node of nodes) {
    if (node.typeCode === code) return node.typeName
    if (Array.isArray(node.children) && node.children.length) {
      const found = findDocumentTypeNameByCode(node.children as DocumentTypeTreeNode[], code)
      if (found) return found
    }
  }
  return ''
}

async function loadRowExtFields(row: TransferDetailRow, preserveValues: boolean) {
  if (!row.busiModuleCode) {
    row.extFieldDefs = []
    if (!preserveValues) row.extValues = {}
    return
  }
  try {
    const defs = await fetchBusinessModuleExtFieldsByApplicationFunction(
      row.busiModuleCode,
      TRANSFER_APPLICATION_FUNCTION,
      'BASIC'
    )
    row.extFieldDefs = [...defs].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.fieldCode.localeCompare(b.fieldCode))
    const allowed = new Set((row.extFieldDefs ?? []).map((f) => f.fieldCode))
    const base: Record<string, string> = preserveValues ? { ...row.extValues } : {}
    for (const code of allowed) {
      if (base[code] === undefined) base[code] = ''
    }
    Object.keys(base).forEach((k) => {
      if (!allowed.has(k)) delete base[k]
    })
    row.extValues = base
  } catch (e) {
    console.error(e)
    row.extFieldDefs = []
    if (!preserveValues) row.extValues = {}
    ElMessage.error(resolveErrorMessage(e, '加载该行扩展字段失败'))
  }
}

async function onDetailBusiModuleChange(row: TransferDetailRow) {
  const moduleCode = String(row.busiModuleCode || '').trim()
  const docTypeCode = String(headerForm.documentTypeCode || '').trim()
  if (
    moduleCode &&
    docTypeCode &&
    !isModuleCodeWithinDocumentType(businessModuleTree.value || [], docTypeCode, moduleCode)
  ) {
    ElMessage.warning('所选业务模块不属于当前文档类型，请重新选择')
    row.busiModuleCode = ''
    row.extFieldDefs = []
    row.extValues = {}
    return
  }
  await loadRowExtFields(row, false)
  await applyArchiveRuleMatchForRow(row)
}

async function handleHeaderDocTypeChange() {
  detailRows.value.forEach((r) => {
    r.extValues = {}
    r.extFieldDefs = []
  })
  if (!showHandoverFormField.value) {
    headerForm.handoverForm = ''
  }
  if (!headerForm.documentTypeCode) {
    detailRows.value.forEach((r) => {
      r.busiModuleCode = ''
      r.extFieldDefs = []
      r.extValues = {}
    })
  } else {
    detailRows.value.forEach((r) => {
      if (
        r.busiModuleCode &&
        !isModuleCodeWithinDocumentType(
          businessModuleTree.value || [],
          headerForm.documentTypeCode,
          r.busiModuleCode
        )
      ) {
        r.busiModuleCode = ''
        r.extFieldDefs = []
        r.extValues = {}
      }
    })
  }
  await loadTransferFieldVisibility()
}

async function loadForEdit(applicationId: number) {
  const detail = await getTransferApplication(applicationId)
  savedApplicationId.value = detail.applicationId
  applicationNumber.value = detail.applicationNumber || applicationNumber.value
  headerForm.applicationDate = detail.applicationDate ? String(detail.applicationDate).slice(0, 10) : headerForm.applicationDate
  const docType = (detail as { documentTypeCode?: string }).documentTypeCode || detail.busiModuleCode || ''
  headerForm.documentTypeCode = docType
  headerForm.applyMethod = detail.applyMethod || 'DIRECT'
  headerForm.expressType = detail.expressType || ''
  headerForm.expressNumber = detail.expressNumber || ''
  headerForm.documentRecipient = detail.documentRecipient ?? undefined
  headerForm.handoverForm = detail.handoverForm || ''
  headerForm.applicationDescription = detail.applicationDescription || ''
  await handleHeaderDocTypeChange()
  detailRows.value = (detail.details || []).map((item) => {
    const extMap: Record<string, string> = {}
    ;(item.extValues || []).forEach((ext) => {
      extMap[ext.fieldCode] = ext.value == null ? '' : String(ext.value)
    })
    return {
      applicationDetailId: item.applicationDetailId,
      rowSource: 'MANUAL',
      lockedByDocument: false,
      companyProjectCode: item.companyProjectCode || '',
      companyProjectName: '',
      docBusiNo: item.docBusiNo || '',
      docName: item.docName || '',
      busiModuleCode: item.busiModuleCode || '',
      archPlaceAlpha2Code: item.archPlaceAlpha2Code || '',
      documentOrganizationCode: item.documentOrganizationCode || '',
      carrierType: item.carrierType || '',
      endArchPeriod: item.endArchPeriod || '',
      startArchPeriod: item.startArchPeriod || '',
      busiVolumeNo: (item as { busiVolumeNo?: string }).busiVolumeNo || '',
      docGenerationDate: item.docGenerationDate || '',
      archCopies: Number(item.archCopies || 1),
      remark: item.remark || '',
      description: item.description || '',
      extValues: extMap,
      extFieldDefs: [],
      attachments: item.attachments || [],
      archiveDestinationPath: []
    }
  })
  for (const row of detailRows.value) {
    syncRowArchivePathFromCode(row)
    if (row.busiModuleCode) {
      await loadRowExtFields(row, true)
    }
  }
}

function onApplyMethodChanged() {
  if (headerForm.applyMethod !== 'MAIL') {
    headerForm.expressType = ''
    headerForm.expressNumber = ''
  }
}

function isFieldVisible(fieldCode: keyof typeof defaultTransferFieldVisibility | string) {
  return transferFieldVisibility.value[fieldCode] !== false
}

async function loadTransferFieldVisibility() {
  if (!headerForm.documentTypeCode) {
    transferFieldVisibility.value = { ...defaultTransferFieldVisibility }
    return
  }
  try {
    const visibility = await getTransferApplyFieldVisibility(headerForm.documentTypeCode, DEFAULT_TENANT_ID)
    transferFieldVisibility.value = {
      ...defaultTransferFieldVisibility,
      ...(visibility || {})
    }
  } catch (e) {
    console.error(e)
    transferFieldVisibility.value = { ...defaultTransferFieldVisibility }
    ElMessage.warning('加载通用字段显隐配置失败，已按默认全部显示')
  }
}

function validateDetails(): boolean {
  if (!detailRows.value.length) {
    ElMessage.warning('请至少新增一条申请行')
    return false
  }
  for (const [idx, row] of detailRows.value.entries()) {
    if (
      (isFieldVisible('companyProjectCode') && !row.companyProjectCode) ||
      (isFieldVisible('docBusiNo') && !row.docBusiNo) ||
      (isFieldVisible('docName') && !row.docName) ||
      (isFieldVisible('busiModuleCode') && !row.busiModuleCode) ||
      (isFieldVisible('archPlaceAlpha2Code') && !row.archPlaceAlpha2Code) ||
      (isFieldVisible('startArchPeriod') && !row.startArchPeriod) ||
      (isFieldVisible('endArchPeriod') && !row.endArchPeriod) ||
      !headerForm.documentTypeCode ||
      (isFieldVisible('archCopies') && !row.archCopies) ||
      (isFieldVisible('carrierType') && !row.carrierType)
    ) {
      ElMessage.error(`第${idx + 1}行存在必填项未填写`)
      return false
    }
    if (showBusiVolumeNoColumn.value && !String(row.busiVolumeNo || '').trim()) {
      ElMessage.error(`第${idx + 1}行业务册号必填`)
      return false
    }
    for (const f of row.extFieldDefs ?? []) {
      if (f.requiredFlag === 'Y' && !String(row.extValues[f.fieldCode] || '').trim()) {
        ElMessage.error(`第${idx + 1}行扩展字段“${f.fieldName}”必填`)
        return false
      }
    }
  }
  return true
}

function toCreateCommand(applicationStatus: string): TransferApplicationCreateCommand {
  return {
    applicationId: savedApplicationId.value ?? undefined,
    applicationNumber: applicationNumber.value,
    applicant: currentUserId.value,
    applicationDate: `${headerForm.applicationDate}T00:00:00`,
    department: currentUserDept.value,
    documentTypeCode: headerForm.documentTypeCode,
    applyMethod: headerForm.applyMethod,
    expressType: headerForm.expressType || undefined,
    expressNumber: headerForm.expressNumber || undefined,
    documentRecipient: Number(headerForm.documentRecipient),
    handoverForm: headerForm.handoverForm || undefined,
    applicationStatus,
    applicationDescription: headerForm.applicationDescription || undefined,
    tenantid: DEFAULT_TENANT_ID,
    details: detailRows.value.map((row) => ({
      companyProjectCode: row.companyProjectCode,
      docBusiNo: row.docBusiNo,
      docName: row.docName,
      busiModuleCode: row.busiModuleCode,
      archPlaceAlpha2Code: row.archPlaceAlpha2Code,
      documentOrganizationCode: row.documentOrganizationCode || undefined,
      endArchPeriod: row.endArchPeriod,
      startArchPeriod: row.startArchPeriod,
      busiVolumeNo: row.busiVolumeNo || undefined,
      archTypeCode: headerForm.documentTypeCode,
      carrierType: row.carrierType,
      docGenerationDate: row.docGenerationDate || undefined,
      archCopies: Number(row.archCopies),
      remark: row.remark || undefined,
      description: row.description || undefined,
      extValues: Object.entries(row.extValues).map(([fieldCode, value]) => ({ fieldCode, value }))
    }))
  }
}

async function saveDraft() {
  const valid = await headerFormRef.value?.validate().catch(() => false)
  if (!valid || !validateDetails()) return
  saving.value = true
  try {
    const payload = toCreateCommand('DRAFT')
    const res = savedApplicationId.value
      ? await updateTransferApplication(savedApplicationId.value, payload)
      : await createTransferApplication(payload)
    savedApplicationId.value = res.applicationId
    ElMessage.success('已保存草稿')
  } catch (e) {
    console.error(e)
    ElMessage.error(resolveErrorMessage(e, '保存失败，请重试'))
  } finally {
    saving.value = false
  }
}

async function submit() {
  const valid = await headerFormRef.value?.validate().catch(() => false)
  if (!valid || !validateDetails()) return
  submitting.value = true
  try {
    const payload = toCreateCommand('SUBMITTED')
    const res = savedApplicationId.value
      ? await updateTransferApplication(savedApplicationId.value, payload)
      : await createTransferApplication(payload)
    savedApplicationId.value = res.applicationId
    ElMessage.success('提交成功，已向文档接收人发起审批流程')
    router.push('/archive-management/transfer-query')
  } catch (e) {
    console.error(e)
    ElMessage.error(resolveErrorMessage(e, '提交失败，请重试'))
  } finally {
    submitting.value = false
  }
}

function cancel() {
  router.push('/archive-management/transfer-query')
}

async function queryPicker() {
  if (!headerForm.documentTypeCode) {
    ElMessage.warning('请先选择申请头文档类型')
    return
  }
  const res = await queryArchives({
    documentTypeCode: headerForm.documentTypeCode,
    documentName: pickerFilter.documentName || undefined,
    businessCode: pickerFilter.businessCode || undefined,
    companyProjectCode: pickerFilter.companyProjectCode || undefined,
    excludeSubmittedTransferApplied: true
  })
  pickerRows.value = res.records ?? []
}

function onPickerSelect(rows: any[]) {
  pickerSelection.value = rows
}

function isRowBlank(row: TransferDetailRow) {
  const noExtValue = !Object.values(row.extValues || {}).some((v) => String(v || '').trim())
  return !row.companyProjectCode
    && !row.docBusiNo
    && !row.docName
    && !row.busiModuleCode
    && !row.archPlaceAlpha2Code
    && !row.endArchPeriod
    && !row.startArchPeriod
    && !row.docGenerationDate
    && Number(row.archCopies || 0) <= 1
    && !String(row.remark || '').trim()
    && !String(row.description || '').trim()
    && noExtValue
}

function isPickerSelectable(row: any) {
  const businessCode = String(row.businessCode ?? '').trim()
  if (!businessCode) return false
  return !detailRows.value.some((item) => String(item.docBusiNo || '').trim() === businessCode)
}

async function addFromPicker() {
  if (!pickerSelection.value.length) {
    ElMessage.warning('请先选择待归档数据')
    return
  }
  const addedBusinessCodes = new Set(detailRows.value.map((item) => String(item.docBusiNo || '').trim()).filter(Boolean))
  let addedCount = 0
  for (const item of pickerSelection.value) {
    const businessCode = String(item.businessCode ?? '').trim()
    if (!businessCode || addedBusinessCodes.has(businessCode)) {
      continue
    }
    const row: TransferDetailRow = {
      rowSource: 'PICKER',
      lockedByDocument: true,
      companyProjectCode: '',
      companyProjectName: '',
      docBusiNo: businessCode,
      docName: '',
      busiModuleCode: '',
      archPlaceAlpha2Code: '',
      documentOrganizationCode: '',
      archiveDestinationPath: [],
      carrierType: carrierTypeOptions.value[0]?.code || '',
      startArchPeriod: '',
      endArchPeriod: '',
      busiVolumeNo: '',
      docGenerationDate: '',
      archCopies: 1,
      remark: '',
      description: '',
      extValues: {},
      extFieldDefs: []
    }
    await fillRowFromArchiveRecord(row, item, 'PICKER')
    addedBusinessCodes.add(businessCode)
    detailRows.value.push(row)
    addedCount += 1
  }
  if (addedCount > 0) {
    detailRows.value = detailRows.value.filter((row) => !isRowBlank(row))
  }
  pickerSelection.value = []
  pickerVisible.value = false
  if (addedCount === 0) {
    ElMessage.warning('没有可新增的数据（已存在或无效）')
    return
  }
  ElMessage.success(`已添加${addedCount}条`)
}

function toLabelOptions(items: DictionaryItem[]): LabelOption[] {
  return items.filter((i) => i.enabledFlag === 'Y').map((i) => ({ code: i.itemCode, name: i.itemName }))
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data
    if (data && typeof data === 'object') {
      const payload = data as { msg?: string; message?: string }
      if (payload.msg) return payload.msg
      if (payload.message) return payload.message
    }
  }
  if (error instanceof Error && error.message) return error.message
  return fallback
}

async function loadGeoRegionsForTransfer() {
  const countries = await fetchCountryRegions({ regionLevel: 'COUNTRY' })
  const countryCodes = countries.map((item) => item.regionCode).filter(Boolean)
  const provincesNested = await Promise.all(
    countryCodes.map((countryCode) => fetchCountryRegions({ regionLevel: 'PROVINCE', parentRegionCode: countryCode }))
  )
  const provinces = provincesNested.flat()
  const provinceCodes = provinces.map((item) => item.regionCode).filter(Boolean)
  const citiesNested = await Promise.all(
    provinceCodes.map((provinceCode) => fetchCountryRegions({ regionLevel: 'CITY', parentRegionCode: provinceCode }))
  )
  const cities = citiesNested.flat()
  countryOptions.value = countries
  provinceOptions.value = provinces
  cityOptions.value = cities
}

onMounted(async () => {
  headerForm.applicationDate = new Date().toISOString().slice(0, 10)
  const [tree, options, companies, businessModules, applyMethods, expressTypes, handoverForms, users] = await Promise.all([
    fetchDocumentTypeTree(),
    fetchArchiveCreateOptions(),
    fetchCompanyInfos({ enabledFlag: 'Y' }),
    fetchBusinessModuleTree(),
    fetchDictionaryItems('TRANSFER_APPLY_METHOD').catch(() => []),
    fetchDictionaryItems('TRANSFER_EXPRESS_TYPE').catch(() => []),
    fetchDictionaryItems('HANDOVER_FORM').catch(() => []),
    fetchUsers().catch(() => [])
  ])
  documentTypeTree.value = tree
  companyProjectOptions.value = (companies || []).map((item) => ({ code: item.companyCode, name: item.companyName }))
  businessModuleTree.value = businessModules || []
  await loadGeoRegionsForTransfer()
  carrierTypeOptions.value = (options as ArchiveCreateOptions).carrierTypes ?? []
  archiveDestinationsFlat.value = (options as ArchiveCreateOptions).archiveDestinations ?? []
  documentOrganizationOptions.value = (options as ArchiveCreateOptions).documentOrganizations ?? []
  const applyMethodDict = toLabelOptions(applyMethods as DictionaryItem[])
  const expressDict = toLabelOptions(expressTypes as DictionaryItem[])
  const handoverFormDict = toLabelOptions(handoverForms as DictionaryItem[])
  if (applyMethodDict.length) applyMethodOptions.value = applyMethodDict
  if (expressDict.length) expressTypeOptions.value = expressDict
  handoverFormOptions.value = handoverFormDict
  userOptions.value = (users || []).map((u: any) => ({
    id: Number(u.userId),
    name: `${u.userName || u.username || `用户-${u.userId}`}${u.employeeNo ? ` ${u.employeeNo}` : ''}`,
    dutyDepartment: u.dutyDepartment || ''
  }))
  const me = userOptions.value.find((u) => u.id === currentUserId.value) || userOptions.value[0]
  if (me) {
    currentUserId.value = me.id
    currentUserName.value = me.name
    if (me.dutyDepartment) currentUserDept.value = me.dutyDepartment
  }
  const editingIdRaw = route.query.applicationId
  const editingId = typeof editingIdRaw === 'string' ? Number(editingIdRaw) : NaN
  if (Number.isFinite(editingId) && editingId > 0) {
    try {
      await loadForEdit(editingId)
    } catch (e) {
      console.error(e)
      ElMessage.error(resolveErrorMessage(e, '加载申请数据失败'))
      addRow()
    }
  } else {
    addRow()
  }
})
</script>

<style scoped>
.transfer-page { display: grid; gap: 16px; }
.sub-card { margin-bottom: 16px; }
.sub-card__title { font-weight: 600; }
.row-between { display: flex; justify-content: space-between; align-items: center; }
.transfer-collapse { margin-bottom: 16px; }
.collapse-title { width: 100%; padding-right: 8px; }
.header-grid { display: grid; grid-template-columns: repeat(4, minmax(180px, 1fr)); gap: 10px; }
.span-2 { grid-column: span 2; }
.input-w180 { width: 180px; }
.footer-actions { display: flex; justify-content: flex-end; gap: 10px; }
.picker-form { width: 100%; }
.picker-input { width: 180px; }
.picker-query-item { margin-left: auto; }
.ext-info-hint { margin: 0 0 12px; color: #606266; font-size: 13px; line-height: 1.5; }
.ext-block { border: 1px solid var(--el-border-color-lighter); border-radius: 8px; padding: 12px 14px; margin-bottom: 12px; background: var(--el-fill-color-blank); }
.ext-block__caption { font-weight: 600; margin-bottom: 12px; color: var(--el-text-color-primary); line-height: 1.4; }
.ext-form-grid { display: grid; grid-template-columns: repeat(2, minmax(220px, 1fr)); gap: 4px 16px; }
.input-ext { max-width: 360px; width: 100%; }
.cell-control-full { width: 100%; min-width: 0; }
.cell-control-full :deep(.el-select__wrapper),
.cell-control-full :deep(.el-tree-select__wrapper),
.cell-control-full :deep(.el-cascader),
.cell-control-full :deep(.el-input__wrapper) {
  width: 100%;
}
</style>
