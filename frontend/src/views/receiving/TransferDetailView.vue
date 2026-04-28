<template>
  <div class="transfer-page">
    <el-card shadow="never">
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="8" animated />
      </div>
      <div v-else-if="detailData" class="transfer-detail">
        <el-collapse v-model="activePanels" class="transfer-collapse">
          <el-collapse-item name="header">
            <template #title>
              <div class="sub-card__title">申请头</div>
            </template>
            <el-form :model="detailData" label-width="120px" disabled>
              <div class="header-grid">
                <el-form-item label="申请单号">
                  <el-input :model-value="detailData.applicationNumber || '-'" class="input-w180" disabled />
                </el-form-item>
                <el-form-item label="申请状态">
                  <el-input :model-value="labelOf(statusOptions, detailData.applicationStatus)" class="input-w180" disabled />
                </el-form-item>
                <el-form-item label="申请人">
                  <el-input :model-value="detailData.applicantName || formatUser(detailData.applicant)" class="input-w180" disabled />
                </el-form-item>
                <el-form-item label="申请日期">
                  <el-input :model-value="formatDateTime(detailData.applicationDate)" class="input-w180" disabled />
                </el-form-item>
                <el-form-item label="移交方式">
                  <el-input :model-value="labelOf(applyMethodOptions, detailData.applyMethod)" class="input-w180" disabled />
                </el-form-item>
                <el-form-item label="邮寄方式">
                  <el-input :model-value="labelOf(expressTypeOptions, detailData.expressType)" class="input-w180" disabled />
                </el-form-item>
                <el-form-item label="邮寄单号">
                  <el-input :model-value="detailData.expressNumber || '-'" class="input-w180" disabled />
                </el-form-item>
                <el-form-item label="文档接收人">
                  <el-input :model-value="detailData.documentRecipientName || formatUser(detailData.documentRecipient)" class="input-w180" disabled />
                </el-form-item>
                <el-form-item v-if="showHandoverFormField" label="移交形式">
                  <el-input :model-value="labelOf(handoverFormOptions, detailData.handoverForm)" class="input-w180" disabled />
                </el-form-item>
                <el-form-item label="差异原因">
                  <el-input :model-value="labelOf(diffReasonOptions, detailData.diffReasonCode)" class="input-w180" disabled />
                </el-form-item>
                <el-form-item label="申请描述" class="span-2">
                  <el-input :model-value="detailData.applicationDescription || '-'" type="textarea" :rows="2" disabled />
                </el-form-item>
              </div>
            </el-form>
          </el-collapse-item>

          <el-collapse-item name="details">
            <template #title>
              <div class="sub-card__title">申请行</div>
            </template>
            <el-table :data="detailRowsForView" border>
              <el-table-column type="index" label="序号" width="64" />
              <el-table-column v-if="isFieldVisible('companyProjectCode')" label="公司" min-width="220">
                <template #default="{ row }">{{ labelOf(companyOptions, row.companyProjectCode) }}</template>
              </el-table-column>
              <el-table-column v-if="isFieldVisible('docBusiNo')" prop="docBusiNo" label="文档业务编码" min-width="160" show-overflow-tooltip />
              <el-table-column v-if="isFieldVisible('docName')" prop="docName" label="文档名称" min-width="180" show-overflow-tooltip />
              <el-table-column v-if="isFieldVisible('busiModuleCode')" label="业务模块" min-width="220">
                <template #default="{ row }">{{ labelOf(busiModuleFlatOptions, row.busiModuleCode) }}</template>
              </el-table-column>
              <el-table-column v-if="isFieldVisible('archPlaceAlpha2Code')" prop="archPlaceAlpha2Code" label="归档地" min-width="180" show-overflow-tooltip />
              <el-table-column v-if="isFieldVisible('documentOrganizationCode')" label="文档组织" min-width="180" show-overflow-tooltip>
                <template #default="{ row }">{{ labelOf(documentOrganizationOptions, row.documentOrganizationCode) }}</template>
              </el-table-column>
              <el-table-column v-if="isFieldVisible('startArchPeriod')" label="开始档期" min-width="140">
                <template #default="{ row }">{{ row.startArchPeriod || '-' }}</template>
              </el-table-column>
              <el-table-column v-if="isFieldVisible('endArchPeriod')" label="结束档期" min-width="140">
                <template #default="{ row }">{{ row.endArchPeriod || '-' }}</template>
              </el-table-column>
              <el-table-column v-if="isFieldVisible('docGenerationDate')" label="文档生成日期" min-width="140">
                <template #default="{ row }">{{ row.docGenerationDate || '-' }}</template>
              </el-table-column>
              <el-table-column v-if="isFieldVisible('carrierType')" label="载体类型" min-width="130">
                <template #default="{ row }">{{ labelOf(carrierTypeOptions, row.carrierType) }}</template>
              </el-table-column>
              <el-table-column v-if="isFieldVisible('archCopies')" label="份数" min-width="100">
                <template #default="{ row }">{{ row.archCopies || '-' }}</template>
              </el-table-column>
              <el-table-column v-if="isFieldVisible('remark')" prop="remark" label="备注" min-width="150" show-overflow-tooltip />
              <el-table-column v-if="isFieldVisible('description')" prop="description" label="描述" min-width="180" show-overflow-tooltip />
            </el-table>
          </el-collapse-item>

          <el-collapse-item name="extInfo">
            <template #title>
              <div class="sub-card__title">扩展信息</div>
            </template>
            <template v-for="(row, idx) in detailRowsForView" :key="'detail-ext-' + idx">
              <div v-if="hasExtValues(row)" class="ext-block">
                <div class="ext-block__caption">
                  第 {{ idx + 1 }} 行 · {{ detailRowSummary(row) }} · 业务模块：{{ labelOf(busiModuleFlatOptions, row.busiModuleCode) }}
                </div>
                <el-form label-width="140px" class="ext-form-grid" disabled>
                  <el-form-item
                    v-for="ext in row.extValues || []"
                    :key="`${row.applicationDetailId}-${ext.fieldCode}`"
                    :label="detailExtFieldLabel(row.busiModuleCode, ext.fieldCode)"
                  >
                    <el-input :model-value="ext.value || '-'" class="input-ext" disabled />
                  </el-form-item>
                </el-form>
              </div>
            </template>
            <el-empty v-if="!detailRowsForView.some((row) => hasExtValues(row))" description="暂无扩展信息" />
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { fetchBusinessModuleExtFieldsByApplicationFunction, fetchDocumentTypeTree } from '../../api/modules/documentType'
import { getTransferApplyFieldVisibility } from '../../api/modules/transferApplyFieldConfig'
import { fetchArchiveCreateOptions } from '../../api/modules/archiveManagement'
import { fetchBusinessModuleTree, type ModuleQueryTreeNode } from '../../api/modules/businessModule'
import { fetchCompanyInfos } from '../../api/modules/companyInfo'
import { fetchDictionaryItems } from '../../api/modules/dictionary'
import { fetchUsers } from '../../api/modules/security'
import {
  getTransferApplication,
  type TransferApplicationDetailItem,
  type TransferApplicationDetailPayload
} from '../../api/modules/transferApplications'
import type { BusinessModuleNode, DictionaryItem, DocumentTypeTreeNode } from '../../types'

interface LabelOption {
  code: string
  name: string
}

const route = useRoute()
const loading = ref(false)
const detailData = ref<TransferApplicationDetailPayload | null>(null)
const activePanels = ref(['header', 'details', 'extInfo'])
const detailExtFieldLabels = ref<Record<string, Record<string, string>>>({})
const companyOptions = ref<LabelOption[]>([])
const documentOrganizationOptions = ref<LabelOption[]>([])
const carrierTypeOptions = ref<LabelOption[]>([])
const busiModuleFlatOptions = ref<LabelOption[]>([])
const statusOptions = ref<LabelOption[]>([])
const diffReasonOptions = ref<LabelOption[]>([])
const applyMethodOptions = ref<LabelOption[]>([])
const expressTypeOptions = ref<LabelOption[]>([])
const handoverFormOptions = ref<LabelOption[]>([])
const documentTypeTree = ref<DocumentTypeTreeNode[]>([])
const DEFAULT_TENANT_ID = 1
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
const userOptions = ref<Array<{ id: number; name: string }>>([])

const detailRowsForView = computed(() => detailData.value?.details ?? [])

const ACCOUNTING_DOCUMENT_TYPE_CODES = new Set(['ACCOUNT_DOC', 'ACCTOUNT_DOC'])
const selectedDocumentTypeCode = computed(
  () => (detailData.value as (TransferApplicationDetailPayload & { documentTypeCode?: string }) | null)?.documentTypeCode || ''
)
const showHandoverFormField = computed(() => {
  const normalized = String(selectedDocumentTypeCode.value || '').trim().toUpperCase()
  return ACCOUNTING_DOCUMENT_TYPE_CODES.has(normalized)
})

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

function toLabelOptions(items: DictionaryItem[]): LabelOption[] {
  return items
    .filter((i) => i.enabledFlag === 'Y')
    .map((i) => ({ code: i.itemCode, name: i.itemName }))
}

function flattenBusinessModulesToLabels(nodes: BusinessModuleNode[]): LabelOption[] {
  const out: LabelOption[] = []
  const walk = (ns: BusinessModuleNode[]) => {
    for (const n of ns) {
      const code = n.moduleCode?.trim()
      const name = (n.moduleName ?? '').trim()
      if (code) out.push({ code, name: name || code })
      if (n.children?.length) walk(n.children)
    }
  }
  walk(nodes)
  return out
}

function labelOf(options: LabelOption[], code?: string | null) {
  if (!code) return '-'
  return options.find((o) => o.code === code)?.name ?? code
}

function formatUser(id?: number | null) {
  if (id == null) return '-'
  return userOptions.value.find((u) => u.id === id)?.name ?? `用户-${id}`
}

function formatDateTime(value?: string | null) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

function detailRowSummary(row: TransferApplicationDetailItem) {
  const name = String(row.docName || '').trim()
  const code = String(row.docBusiNo || '').trim()
  if (name && code) return `${name}（${code}）`
  if (name) return name
  if (code) return code
  return '未填写文档名称/业务编码'
}

function hasExtValues(row: TransferApplicationDetailItem) {
  return Array.isArray(row.extValues) && row.extValues.length > 0
}

function detailExtFieldLabel(moduleCode?: string, fieldCode?: string) {
  if (!fieldCode) return '-'
  if (!moduleCode) return fieldCode
  return detailExtFieldLabels.value[moduleCode]?.[fieldCode] ?? fieldCode
}

function isFieldVisible(fieldCode: keyof typeof defaultTransferFieldVisibility | string) {
  return transferFieldVisibility.value[fieldCode] !== false
}

async function loadTransferFieldVisibility(documentTypeCode?: string) {
  const code = String(documentTypeCode || '').trim()
  if (!code) {
    transferFieldVisibility.value = { ...defaultTransferFieldVisibility }
    return
  }
  try {
    const visibility = await getTransferApplyFieldVisibility(code, DEFAULT_TENANT_ID)
    transferFieldVisibility.value = {
      ...defaultTransferFieldVisibility,
      ...(visibility || {})
    }
  } catch (e) {
    console.error(e)
    transferFieldVisibility.value = { ...defaultTransferFieldVisibility }
  }
}

async function loadDetailExtFieldLabels(detail: TransferApplicationDetailPayload) {
  const moduleCodes = Array.from(
    new Set((detail.details || []).map((item) => String(item.busiModuleCode || '').trim()).filter(Boolean))
  )
  if (!moduleCodes.length) {
    detailExtFieldLabels.value = {}
    return
  }
  const entries = await Promise.all(
    moduleCodes.map(async (moduleCode) => {
      try {
        const defs = await fetchBusinessModuleExtFieldsByApplicationFunction(moduleCode, '移交', 'BASIC')
        const labelMap: Record<string, string> = {}
        defs.forEach((d) => {
          labelMap[d.fieldCode] = d.fieldName || d.fieldCode
        })
        return [moduleCode, labelMap] as const
      } catch {
        return [moduleCode, {}] as const
      }
    })
  )
  detailExtFieldLabels.value = Object.fromEntries(entries)
}

async function loadDetail() {
  const id = Number(route.params.id)
  if (!Number.isFinite(id) || id <= 0) {
    ElMessage.error('申请 ID 无效')
    return
  }
  loading.value = true
  try {
    const [detail, options, companies, moduleTree, docTypeTree, st, diff, am, ex, handover, users] = await Promise.all([
      getTransferApplication(id),
      fetchArchiveCreateOptions(),
      fetchCompanyInfos({ enabledFlag: 'Y' }),
      fetchBusinessModuleTree().catch((): BusinessModuleNode[] => []),
      fetchDocumentTypeTree().catch((): DocumentTypeTreeNode[] => []),
      fetchDictionaryItems('TRANSFER_APPLICATION_STATUS').catch((): DictionaryItem[] => []),
      fetchDictionaryItems('TRANSFER_DIFF_REASON').catch((): DictionaryItem[] => []),
      fetchDictionaryItems('TRANSFER_APPLY_METHOD').catch((): DictionaryItem[] => []),
      fetchDictionaryItems('TRANSFER_EXPRESS_TYPE').catch((): DictionaryItem[] => []),
      fetchDictionaryItems('HANDOVER_FORM').catch((): DictionaryItem[] => []),
      fetchUsers().catch(() => [])
    ])
    detailData.value = detail
    companyOptions.value = companies.map((c) => ({ code: c.companyCode, name: c.companyName }))
    documentOrganizationOptions.value = (options.documentOrganizations ?? []).map((o) => ({ code: o.code, name: o.name }))
    carrierTypeOptions.value = (options.carrierTypes ?? []).map((c) => ({ code: c.code, name: c.name }))
    busiModuleFlatOptions.value = flattenBusinessModulesToLabels(moduleTree)
    documentTypeTree.value = docTypeTree
    statusOptions.value = toLabelOptions(st)
    diffReasonOptions.value = toLabelOptions(diff)
    applyMethodOptions.value = toLabelOptions(am)
    expressTypeOptions.value = toLabelOptions(ex)
    handoverFormOptions.value = toLabelOptions(handover)
    userOptions.value = (users || []).map((u: any) => ({
      id: Number(u.userId),
      name: `${u.userName || u.username || `用户-${u.userId}`}${u.employeeNo ? ` ${u.employeeNo}` : ''}`
    }))
    await loadDetailExtFieldLabels(detail)
    await loadTransferFieldVisibility(detail.busiModuleCode)
  } catch (e) {
    console.error(e)
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadDetail()
})
</script>

<style scoped>
.transfer-page { display: grid; gap: 16px; }
.loading-container { padding: 16px 0; }
.transfer-detail { display: grid; gap: 16px; }
.sub-card__title { font-weight: 600; }
.transfer-collapse { margin-bottom: 16px; }
.header-grid { display: grid; grid-template-columns: repeat(4, minmax(180px, 1fr)); gap: 10px; }
.span-2 { grid-column: span 2; }
.input-w180 { width: 180px; }
.ext-block { border: 1px solid var(--el-border-color-lighter); border-radius: 8px; padding: 12px 14px; margin-bottom: 12px; background: var(--el-fill-color-blank); }
.ext-block__caption { font-weight: 600; margin-bottom: 12px; color: var(--el-text-color-primary); line-height: 1.4; }
.ext-form-grid { display: grid; grid-template-columns: repeat(2, minmax(220px, 1fr)); gap: 4px 16px; }
.input-ext { max-width: 360px; width: 100%; }
</style>
