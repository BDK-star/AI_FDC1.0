<template>
  <div class="create-page">
    <el-card shadow="never" class="flow-card">
      <div class="flow-track">
        <div
          v-for="(step, index) in archiveSteps"
          :key="step.code"
          class="flow-step"
          :class="{ 'is-active': step.code === currentStep, 'is-future': index > currentStepIndex }"
        >
          <div class="flow-step__dot">{{ index + 1 }}</div>
          <div class="flow-step__body">
            <div class="flow-step__title">{{ step.label }}</div>
            <div class="flow-step__meta">{{ step.meta }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-head">
          <div>
            <strong>创建档案</strong>
            <span>优先通过文件自动创建，系统会在上传后同步完成全文解析、OCR、AI 摘要生成和结构化字段预填。</span>
          </div>
          <div class="section-actions">
            <el-tag type="info" effect="plain">会话：{{ session?.sessionCode || '-' }}</el-tag>
            <el-tag type="success" effect="plain">状态：{{ session?.sessionStatus || 'READY' }}</el-tag>
            <el-button @click="resetForm">重置</el-button>
            <el-button type="primary" @click="submitArchive">保存档案</el-button>
          </div>
        </div>
      </template>

      <el-form label-width="110px" class="archive-form">
        <el-card shadow="never" class="sub-card">
          <template #header><div class="sub-card__title">创建方式</div></template>
          <div class="mode-panel">
            <el-radio-group v-model="form.createMode" @change="handleCreateModeChange">
              <el-radio-button label="AUTO">通过文件自动创建</el-radio-button>
              <el-radio-button label="MANUAL">手工创建</el-radio-button>
            </el-radio-group>
            <el-alert :title="createModeHint" type="info" :closable="false" show-icon />
          </div>
        </el-card>

        <el-card v-if="form.createMode === 'AUTO'" shadow="never" class="sub-card">
          <template #header><div class="sub-card__title">自动创建入口</div></template>
          <div class="auto-upload">
            <div class="auto-upload__intro">
              <strong>先上传原始文件</strong>
              <span>上传后系统会同步解析全文、提取摘要、生成切片和向量，并尽量回填档案名称、业务编码、扩展字段和附件信息。</span>
            </div>
            <el-upload
              drag
              multiple
              :auto-upload="true"
              :show-file-list="false"
              :http-request="handleElectronicUpload"
              :on-error="handleUploadError"
            >
              <div class="upload-trigger">
                <div class="upload-trigger__title">点击或拖拽上传电子文件</div>
                <div class="upload-trigger__desc">支持批量上传，上传完成后会自动进入 AI 解析并同步回填。</div>
              </div>
            </el-upload>
          </div>
        </el-card>

        <el-card shadow="never" class="sub-card">
          <template #header><div class="sub-card__title">文档基本信息</div></template>
          <div class="form-grid form-grid--4">
            <el-form-item label="文档类型" required class="span-2">
              <CommonTreeSelect
                v-model="form.documentTypeCode"
                :data="documentTypeLevel1Tree"
                placeholder="请选择文档类型"
                label-key="typeName"
                value-key="typeCode"
                children-key="children"
                @update:model-value="handleDocumentTypeChange"
              />
            </el-form-item>
            <el-form-item label="公司" required>
              <el-select v-model="form.companyProjectCode" filterable @change="handleDefaultRefresh">
                <el-option v-for="item in options.companyProjects" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="载体类型" required>
              <el-select v-model="form.carrierTypeCode">
                <el-option v-for="item in options.carrierTypes" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>

            <el-form-item label="文档名称" required class="span-2">
              <el-input v-model="form.documentName" placeholder="请输入文档名称" />
            </el-form-item>
            <el-form-item label="文档业务编码">
              <el-input v-model="form.businessCode" placeholder="请输入业务编码" />
            </el-form-item>
            <el-form-item label="业务模块" required>
              <el-select v-model="form.archiveTypeCode" clearable :disabled="!form.documentTypeCode" @change="handleDefaultRefresh">
                <el-option v-for="item in businessModuleOptions" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>

            <el-form-item label="开始档期" required>
              <el-date-picker v-model="form.beginPeriod" type="month" value-format="YYYY-MM" style="width: 100%" />
            </el-form-item>
            <el-form-item label="结束档期" required>
              <el-date-picker v-model="form.endPeriod" type="month" value-format="YYYY-MM" style="width: 100%" />
            </el-form-item>
            <el-form-item label="文档生成日期" required>
              <el-date-picker v-model="form.documentDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
            <el-form-item label="保存期限" required>
              <div class="inline-unit">
                <el-input-number v-model="form.retentionPeriodYears" :min="0" :max="100" style="width: 100%" />
                <el-tag effect="plain" type="info">年</el-tag>
              </div>
            </el-form-item>

            <el-form-item label="归档责任人" required>
              <el-input v-model="form.dutyPerson" placeholder="请输入归档责任人" />
            </el-form-item>
            <el-form-item label="归档责任部门" required>
              <el-input v-model="form.dutyDepartment" placeholder="请输入归档责任部门" />
            </el-form-item>
            <el-form-item label="密级" required>
              <el-select v-model="form.securityLevelCode" filterable>
                <el-option v-for="item in options.securityLevels" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="文档组织" required>
              <el-select v-model="form.documentOrganizationCode" filterable>
                <el-option v-for="item in options.documentOrganizations" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>

            <el-form-item label="归档地">
              <el-select v-model="form.archiveDestination" filterable @change="handleDefaultRefresh">
                <el-option v-for="item in options.archiveDestinations" :key="item.code" :label="item.name" :value="item.code" />
              </el-select>
            </el-form-item>
            <el-form-item label="系统来源">
              <el-input v-model="form.sourceSystem" placeholder="请输入系统来源" />
            </el-form-item>
            <el-form-item label="产生地">
              <el-input v-model="form.originPlace" placeholder="请输入产生地" />
            </el-form-item>
          </div>

          <el-form-item label="备注">
            <el-alert
              v-if="parseExplainText"
              :title="parseExplainText"
              type="info"
              :closable="false"
              show-icon
              class="parse-explain"
            />
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
          </el-form-item>
          <el-form-item label="AI档案摘要">
            <el-input v-model="form.aiArchiveSummary" type="textarea" :rows="3" placeholder="系统会根据结构化信息和附件内容自动生成，用户可继续修订" />
          </el-form-item>
        </el-card>

        <el-card shadow="never" class="sub-card">
          <template #header><div class="sub-card__title">文档扩展信息</div></template>
          <el-empty v-if="!extFields.length" description="当前文档类型未配置有效扩展字段" />
          <div v-else class="form-grid form-grid--4">
            <el-form-item
              v-for="field in extFields"
              :key="field.fieldCode"
              :label="field.fieldName"
              :required="field.requiredFlag === 'Y'"
              :class="{ 'span-2': field.fieldType === 'TEXT' && field.fieldName.length > 8 }"
            >
              <el-input v-model="extValues[field.fieldCode]" :placeholder="field.fieldType === 'DICT' ? '请输入字典项编码或名称' : '请输入内容'" />
            </el-form-item>
          </div>
        </el-card>

        <el-card v-if="showElectronicSection" shadow="never" class="sub-card">
          <template #header>
            <div class="sub-card__header">
              <div>
                <div class="sub-card__title">电子件信息</div>
                <div class="sub-card__subtitle">每个附件都可单独维护附件类型、备注和 AI 摘要。</div>
              </div>
              <el-upload
                v-if="form.createMode !== 'AUTO'"
                multiple
                :auto-upload="true"
                :show-file-list="false"
                :http-request="handleElectronicUpload"
                :on-error="handleUploadError"
              >
                <template #trigger>
                  <el-button type="primary" plain>上传电子附件</el-button>
                </template>
              </el-upload>
            </div>
          </template>
          <el-table :data="electronicAttachments" border class="attachment-table" empty-text="暂无电子附件">
            <el-table-column prop="fileName" label="文件名称" min-width="240" show-overflow-tooltip />
            <el-table-column label="附件类型" width="180">
              <template #default="{ row }">
                <el-select v-model="row.attachmentTypeCode" placeholder="请选择" clearable @change="saveAttachmentRow(row)">
                  <el-option v-for="item in options.attachmentTypes" :key="item.code" :label="item.name" :value="item.code" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="本次备注" min-width="220">
              <template #default="{ row }">
                <el-input v-model="row.remark" placeholder="AI 未推导时可手工补充" @change="saveAttachmentRow(row)" />
              </template>
            </el-table-column>
            <el-table-column label="AI摘要" min-width="340">
              <template #default="{ row }">
                <el-input v-model="row.aiSummary" type="textarea" :rows="2" placeholder="AI 自动生成后可继续修改" @change="saveAttachmentRow(row)" />
              </template>
            </el-table-column>
            <el-table-column label="解析状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.parseStatus)" effect="plain">{{ row.parseStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="向量状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.vectorStatus)" effect="plain">{{ row.vectorStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="versionNo" label="版本" width="80" />
          </el-table>
        </el-card>

        <el-card v-if="showPaperSection" shadow="never" class="sub-card">
          <template #header>
            <div class="sub-card__header">
              <div>
                <div class="sub-card__title">纸质件信息</div>
                <div class="sub-card__subtitle">扫描件同样支持单附件维护，纸质件数量单独记录。</div>
              </div>
              <el-upload multiple :auto-upload="true" :show-file-list="false" :http-request="handlePaperUpload" :on-error="handleUploadError">
                <template #trigger>
                  <el-button type="primary" plain>上传扫描件</el-button>
                </template>
              </el-upload>
            </div>
          </template>
          <div class="form-grid form-grid--4 compact-grid">
            <el-form-item label="计划归档份数" required>
              <el-input-number v-model="form.paperInfo.plannedCopyCount" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="实际归档份数" required>
              <el-input-number v-model="form.paperInfo.actualCopyCount" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item label="纸质件说明" class="span-2">
              <el-input v-model="form.paperInfo.remark" placeholder="请输入纸质件说明" />
            </el-form-item>
          </div>
          <el-table :data="paperAttachments" border class="attachment-table" empty-text="暂无扫描件">
            <el-table-column prop="fileName" label="扫描件名称" min-width="240" show-overflow-tooltip />
            <el-table-column label="附件类型" width="180">
              <template #default="{ row }">
                <el-select v-model="row.attachmentTypeCode" placeholder="请选择" clearable @change="saveAttachmentRow(row)">
                  <el-option v-for="item in options.attachmentTypes" :key="item.code" :label="item.name" :value="item.code" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="本次备注" min-width="220">
              <template #default="{ row }">
                <el-input v-model="row.remark" placeholder="AI 未推导时可手工补充" @change="saveAttachmentRow(row)" />
              </template>
            </el-table-column>
            <el-table-column label="AI摘要" min-width="340">
              <template #default="{ row }">
                <el-input v-model="row.aiSummary" type="textarea" :rows="2" placeholder="AI 自动生成后可继续修改" @change="saveAttachmentRow(row)" />
              </template>
            </el-table-column>
            <el-table-column label="解析状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.parseStatus)" effect="plain">{{ row.parseStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="向量状态" width="110">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.vectorStatus)" effect="plain">{{ row.vectorStatus }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="versionNo" label="版本" width="80" />
          </el-table>
        </el-card>
      </el-form>
    </el-card>

    <el-dialog
      v-model="parseDialogVisible"
      title="AI 正在解析文件"
      width="440px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <div class="parse-dialog">
        <el-icon class="is-loading" size="28"><Loading /></el-icon>
        <div>
          <div class="parse-dialog__title">文件已上传，系统正在同步解析</div>
          <div class="parse-dialog__desc">正在进行全文提取、OCR、摘要生成、切片和向量化入库，请稍候。</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'
import { ElLoading, ElMessage } from 'element-plus'
import type { LoadingInstance, UploadRequestOptions } from 'element-plus'
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import CommonTreeSelect from '../../components/CommonTreeSelect.vue'
import {
  createArchive,
  createArchiveSession,
  fetchArchiveCreateOptions,
  fetchArchiveSession,
  fetchEffectiveDocumentTypeExtFields,
  resolveArchiveDefaults,
  updateArchiveAttachment,
  uploadArchiveAttachment,
  type ArchiveCreateCommand
} from '../../api/modules/archiveManagement'
import { fetchDocumentTypeTree, fetchLevel3Modules } from '../../api/modules/documentType'
import type { ArchiveAttachmentItem, ArchiveCreateOptions, ArchiveCreateSession, DocumentTypeExtField } from '../../types'

const options = reactive<ArchiveCreateOptions>({
  companyProjects: [],
  documentTypes: [],
  archiveDestinations: [],
  documentOrganizations: [],
  securityLevels: [],
  carrierTypes: [],
  attachmentTypes: [],
  archiveTypes: [],
  aiModels: []
})

const archiveSteps = [
  { code: 'CREATED', label: '创建', meta: '办理人：当前用户 · 时间：待保存' },
  { code: 'TRANSFERRED', label: '移交/核销', meta: '办理人：待流转 · 时间：待处理' },
  { code: 'BOUND', label: '成册', meta: '办理人：待流转 · 时间：待处理' },
  { code: 'STORED', label: '入库', meta: '办理人：待流转 · 时间：待处理' },
  { code: 'ARCHIVED', label: '已归档', meta: '归档完成 · 时间：待完成' }
]

const currentStep = ref('CREATED')
const currentStepIndex = computed(() => Math.max(archiveSteps.findIndex(item => item.code === currentStep.value), 0))
const createModeHint = computed(() => form.createMode === 'AUTO'
  ? '建议先上传文件，系统会自动完成全文解析、OCR、AI 摘要生成和字段预填，尽量减少手工录入。'
  : '手工创建适用于无原始电子文件，或仅需录入纸质档案信息的场景。')

const session = ref<ArchiveCreateSession>()
const savedArchive = ref<any>()
const extFields = ref<DocumentTypeExtField[]>([])
const extValues = reactive<Record<string, string>>({})
const documentTypeTree = ref<any[]>([])
const documentTypeLevel1Tree = computed(() => (Array.isArray(documentTypeTree.value) ? documentTypeTree.value : []).map((n: any) => ({ ...n, children: [] })))
const businessModuleOptions = ref<{ code: string; name: string }[]>([])
const parseDialogVisible = ref(false)
const uploadingCount = ref(0)
let loadingInstance: LoadingInstance | null = null

const form = reactive({
  createMode: 'AUTO' as 'AUTO' | 'MANUAL',
  documentTypeCode: '',
  companyProjectCode: '',
  beginPeriod: '',
  endPeriod: '',
  businessCode: '',
  documentName: '',
  dutyPerson: '',
  dutyDepartment: '',
  documentDate: '',
  securityLevelCode: '',
  sourceSystem: '',
  archiveDestination: '',
  originPlace: '',
  carrierTypeCode: '',
  remark: '',
  aiArchiveSummary: '',
  documentOrganizationCode: '',
  retentionPeriodYears: 10,
  archiveTypeCode: '',
  countryCode: '',
  paperInfo: { plannedCopyCount: undefined as number | undefined, actualCopyCount: undefined as number | undefined, remark: '' }
})

const electronicAttachments = computed(() => session.value?.attachments.filter(item => item.attachmentRole === 'ELECTRONIC') ?? [])
const paperAttachments = computed(() => session.value?.attachments.filter(item => item.attachmentRole === 'PAPER_SCAN') ?? [])
const showElectronicSection = computed(() => ['ELECTRONIC', 'HYBRID'].includes(form.carrierTypeCode))
const showPaperSection = computed(() => ['PAPER', 'HYBRID'].includes(form.carrierTypeCode))
const parseExplainText = computed(() => session.value?.aiParseResult?.parseExplain || '')

const statusTagType = (status?: string) => {
  switch (status) {
    case 'SUCCESS': return 'success'
    case 'FAILED': return 'danger'
    case 'PROCESSING':
    case 'PENDING': return 'warning'
    default: return 'info'
  }
}

const applyAiExtValues = () => {
  const aiResult = session.value?.aiParseResult
  if (!aiResult) return
  if (aiResult.extendedValues) {
    Object.entries(aiResult.extendedValues).forEach(([fieldCode, value]) => {
      if (!extValues[fieldCode] && value) extValues[fieldCode] = value
    })
  }
  const descriptionFallback = (aiResult.extendedValues?.description || aiResult.extractedTextPreview || '').trim()
  extFields.value
    .filter(field => field.fieldType === 'TEXT' && !extValues[field.fieldCode] && descriptionFallback)
    .forEach(field => { extValues[field.fieldCode] = descriptionFallback })
}

const loadOptions = async () => {
  const result = await fetchArchiveCreateOptions()
  Object.assign(options, result)
  if (!form.carrierTypeCode && options.carrierTypes.length) form.carrierTypeCode = options.carrierTypes[0].code
}

const ensureSession = async () => {
  if (!session.value) session.value = await createArchiveSession(form.createMode)
}

const loadSession = async () => {
  if (!session.value?.sessionCode) return
  session.value = await fetchArchiveSession(session.value.sessionCode)
  const aiResult = session.value.aiParseResult
  if (!aiResult) return
  if (aiResult.documentName && !form.documentName) form.documentName = aiResult.documentName
  if (aiResult.businessCode && !form.businessCode) form.businessCode = aiResult.businessCode
  if (aiResult.companyProjectCode && !form.companyProjectCode) form.companyProjectCode = aiResult.companyProjectCode
  if (aiResult.sourceSystem && !form.sourceSystem) form.sourceSystem = aiResult.sourceSystem
  if (aiResult.aiSummary) form.aiArchiveSummary = aiResult.aiSummary
  if (aiResult.beginPeriod && !form.beginPeriod) form.beginPeriod = aiResult.beginPeriod
  if (aiResult.endPeriod && !form.endPeriod) form.endPeriod = aiResult.endPeriod
  if (aiResult.documentDate && !form.documentDate) form.documentDate = aiResult.documentDate
  if (session.value.carrierTypeCodeGuess && !form.carrierTypeCode) form.carrierTypeCode = session.value.carrierTypeCodeGuess
  if (session.value.busiModuleCodeGuess && !form.documentTypeCode) {
    form.documentTypeCode = session.value.busiModuleCodeGuess
    await handleDocumentTypeChange(form.documentTypeCode)
  }
  if (form.companyProjectCode && form.archiveTypeCode) {
    await handleDefaultRefresh()
  }
  applyAiExtValues()
}

const handleCreateModeChange = async () => {
  session.value = await createArchiveSession(form.createMode)
  savedArchive.value = undefined
}

const handleDocumentTypeChange = async (typeCode?: string) => {
  form.archiveTypeCode = ''
  businessModuleOptions.value = []
  if (!typeCode) {
    extFields.value = []
    return
  }
  businessModuleOptions.value = await fetchLevel3Modules(typeCode)
  extFields.value = await fetchEffectiveDocumentTypeExtFields(typeCode)
  extFields.value.forEach(field => {
    if (!(field.fieldCode in extValues)) extValues[field.fieldCode] = ''
  })
  applyAiExtValues()
  await handleDefaultRefresh()
}

const handleDefaultRefresh = async () => {
  if (!form.companyProjectCode || !form.archiveTypeCode) return
  const defaults = await resolveArchiveDefaults({
    companyProjectCode: form.companyProjectCode,
    busiModuleCode: form.archiveTypeCode,
    archiveDestination: form.archiveDestination || undefined
  })
  form.securityLevelCode = defaults.securityLevelCode || form.securityLevelCode
  form.archiveDestination = defaults.archiveDestination || form.archiveDestination
  form.documentOrganizationCode = defaults.documentOrganizationCode || form.documentOrganizationCode
  form.retentionPeriodYears = defaults.retentionPeriodYears ?? form.retentionPeriodYears
  form.countryCode = defaults.countryCode || form.countryCode
}

const uploadFiles = async (role: 'ELECTRONIC' | 'PAPER_SCAN', file: File) => {
  await ensureSession()
  uploadingCount.value += 1
  parseDialogVisible.value = true
  if (!loadingInstance) {
    loadingInstance = ElLoading.service({
      lock: true,
      text: 'AI 正在解析文件，请稍候...',
      background: 'rgba(255, 255, 255, 0.78)'
    })
  }
  await nextTick()
  const startedAt = Date.now()
  try {
    await uploadArchiveAttachment({
      sessionCode: session.value!.sessionCode,
      attachmentRole: role,
      file
    })
    await loadSession()
  } finally {
    const elapsed = Date.now() - startedAt
    if (elapsed < 1200) {
      await new Promise(resolve => window.setTimeout(resolve, 1200 - elapsed))
    }
    uploadingCount.value = Math.max(uploadingCount.value - 1, 0)
    parseDialogVisible.value = uploadingCount.value > 0
    if (uploadingCount.value === 0 && loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }
  }
}

const handleElectronicUpload = async (request: UploadRequestOptions) => {
  try {
    await uploadFiles('ELECTRONIC', request.file as File)
    ElMessage.success('电子附件已上传并完成解析')
    request.onSuccess?.({})
  } catch (error: any) {
    request.onError?.(error)
    ElMessage.error(error?.message || '电子附件上传失败')
  }
}

const handlePaperUpload = async (request: UploadRequestOptions) => {
  try {
    await uploadFiles('PAPER_SCAN', request.file as File)
    ElMessage.success('纸质扫描件已上传并完成解析')
    request.onSuccess?.({})
  } catch (error: any) {
    request.onError?.(error)
    ElMessage.error(error?.message || '纸质扫描件上传失败')
  }
}

const saveAttachmentRow = async (row: ArchiveAttachmentItem) => {
  if (!session.value?.sessionCode) return
  await updateArchiveAttachment({
    sessionCode: session.value.sessionCode,
    attachmentId: row.attachmentId,
    attachmentTypeCode: row.attachmentTypeCode,
    remark: row.remark,
    aiSummary: row.aiSummary
  })
}

const handleUploadError = (error: Error) => {
  parseDialogVisible.value = false
  if (loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
  ElMessage.error(error?.message || '上传失败')
}

const validateForm = () => {
  const requiredFields = [
    ['documentTypeCode', '请选择文档类型'],
    ['companyProjectCode', '请选择公司'],
    ['carrierTypeCode', '请选择载体类型'],
    ['documentName', '请输入文档名称'],
    ['beginPeriod', '请选择开始档期'],
    ['endPeriod', '请选择结束档期'],
    ['documentDate', '请选择文档生成日期'],
    ['dutyPerson', '请输入归档责任人'],
    ['dutyDepartment', '请输入归档责任部门'],
    ['securityLevelCode', '请选择密级'],
    ['documentOrganizationCode', '请选择文档组织'],
    ['archiveTypeCode', '请选择业务模块']
  ] as const

  for (const [key, message] of requiredFields) {
    if (!form[key]) {
      ElMessage.warning(message)
      return false
    }
  }

  for (const field of extFields.value.filter(item => item.requiredFlag === 'Y')) {
    if (!extValues[field.fieldCode]) {
      ElMessage.warning(`请填写扩展字段“${field.fieldName}”`)
      return false
    }
  }

  if (showElectronicSection.value && !electronicAttachments.value.length) {
    ElMessage.warning('当前载体类型包含电子件，请至少上传一个电子附件')
    return false
  }

  if (showPaperSection.value && (form.paperInfo.plannedCopyCount == null || form.paperInfo.actualCopyCount == null)) {
    ElMessage.warning('当前载体类型包含纸质件，请填写计划归档份数和实际归档份数')
    return false
  }

  return true
}

const submitArchive = async () => {
  await ensureSession()
  if (!validateForm()) return
  const payload: ArchiveCreateCommand = {
    sessionCode: session.value?.sessionCode,
    createMode: form.createMode,
    documentTypeCode: form.documentTypeCode,
    companyProjectCode: form.companyProjectCode,
    beginPeriod: form.beginPeriod,
    endPeriod: form.endPeriod,
    businessCode: form.businessCode,
    documentName: form.documentName,
    dutyPerson: form.dutyPerson,
    dutyDepartment: form.dutyDepartment,
    documentDate: form.documentDate,
    securityLevelCode: form.securityLevelCode,
    sourceSystem: form.sourceSystem,
    archiveDestination: form.archiveDestination,
    originPlace: form.originPlace,
    carrierTypeCode: form.carrierTypeCode as 'ELECTRONIC' | 'PAPER' | 'HYBRID',
    remark: form.remark,
    aiArchiveSummary: form.aiArchiveSummary,
    documentOrganizationCode: form.documentOrganizationCode,
    retentionPeriodYears: form.retentionPeriodYears,
    archiveTypeCode: form.archiveTypeCode,
    countryCode: form.countryCode,
    extValues: { ...extValues },
    paperInfo: showPaperSection.value
      ? {
          plannedCopyCount: form.paperInfo.plannedCopyCount,
          actualCopyCount: form.paperInfo.actualCopyCount,
          remark: form.paperInfo.remark
        }
      : undefined
  }
  try {
    savedArchive.value = await createArchive(payload)
    currentStep.value = 'CREATED'
    ElMessage.success(`档案 ${savedArchive.value.documentName} 创建成功`)
  } catch (error: any) {
    ElMessage.error(error?.message || '创建档案失败')
  }
}

const resetForm = async () => {
  Object.assign(form, {
    createMode: 'AUTO',
    documentTypeCode: '',
    companyProjectCode: '',
    beginPeriod: '',
    endPeriod: '',
    businessCode: '',
    documentName: '',
    dutyPerson: '',
    dutyDepartment: '',
    documentDate: '',
    securityLevelCode: '',
    sourceSystem: '',
    archiveDestination: '',
    originPlace: '',
    carrierTypeCode: options.carrierTypes[0]?.code || '',
    remark: '',
    aiArchiveSummary: '',
    documentOrganizationCode: '',
    retentionPeriodYears: 10,
    archiveTypeCode: options.archiveTypes[0]?.code || '',
    countryCode: '',
    paperInfo: { plannedCopyCount: undefined, actualCopyCount: undefined, remark: '' }
  })
  Object.keys(extValues).forEach(key => delete extValues[key])
  extFields.value = []
  session.value = await createArchiveSession('AUTO')
  savedArchive.value = undefined
  currentStep.value = 'CREATED'
}

onMounted(async () => {
  await loadOptions()
  documentTypeTree.value = await fetchDocumentTypeTree()
  session.value = await createArchiveSession('AUTO')
})
</script>

<style scoped>
.create-page { display: grid; gap: 16px; }
.flow-track { position: relative; display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 18px; }
.flow-track::before { content: ''; position: absolute; left: 48px; right: 48px; top: 16px; height: 2px; background: #dfe7f2; }
.flow-step { position: relative; display: flex; align-items: flex-start; gap: 12px; z-index: 1; padding-right: 8px; background: linear-gradient(180deg, #fff 0, #fff 88%, transparent 100%); }
.flow-step__dot { width: 32px; height: 32px; border-radius: 999px; background: #d9ecff; color: #409eff; display: grid; place-items: center; font-weight: 700; flex: 0 0 auto; }
.flow-step__body { min-width: 0; padding-top: 2px; }
.flow-step__title { font-weight: 600; color: #24324a; line-height: 1.4; }
.flow-step__meta { margin-top: 6px; font-size: 12px; line-height: 1.55; color: #8a94a6; }
.flow-step.is-active .flow-step__dot { background: #409eff; color: #fff; box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.15); }
.flow-step.is-future .flow-step__dot { background: #f0f2f5; color: #a0a8b5; }
.section-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.section-head strong { display: block; color: #24324a; font-size: 16px; }
.section-head span { color: #7a879a; font-size: 12px; line-height: 1.6; }
.section-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.archive-form { display: grid; gap: 16px; }
.sub-card { border: 1px solid #edf2f7; }
.sub-card__title { font-weight: 600; color: #24324a; }
.sub-card__subtitle { margin-top: 4px; font-size: 12px; color: #7a879a; }
.sub-card__header { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.mode-panel, .auto-upload { display: grid; gap: 16px; }
.auto-upload__intro { display: grid; gap: 4px; }
.auto-upload__intro strong { color: #24324a; }
.auto-upload__intro span { color: #6b7280; font-size: 13px; line-height: 1.6; }
.form-grid { display: grid; gap: 12px 16px; }
.form-grid--4 { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.compact-grid { margin-bottom: 12px; }
.span-2 { grid-column: span 2; }
.inline-unit { display: flex; align-items: center; gap: 8px; width: 100%; }
.upload-trigger { padding: 28px 16px; }
.upload-trigger__title { font-size: 16px; font-weight: 600; color: #24324a; }
.upload-trigger__desc { margin-top: 6px; font-size: 13px; color: #7a879a; }
.attachment-table :deep(.el-textarea__inner) { min-height: 60px !important; }
.attachment-table :deep(.el-input__wrapper), .attachment-table :deep(.el-select__wrapper) { min-height: 34px; }
.parse-dialog { display: flex; align-items: center; gap: 16px; }
.parse-dialog__title { font-weight: 600; color: #24324a; }
.parse-dialog__desc { margin-top: 6px; font-size: 13px; color: #7a879a; line-height: 1.6; }
.parse-explain { margin-bottom: 16px; }
.parse-explain :deep(.el-alert__title) { white-space: pre-line; line-height: 1.7; }
@media (max-width: 1280px) {
  .flow-track, .form-grid--4 { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .span-2 { grid-column: span 2; }
}
@media (max-width: 960px) {
  .flow-track, .form-grid--4 { grid-template-columns: 1fr; }
  .span-2 { grid-column: span 1; }
  .flow-track::before { display: none; }
  .section-head, .sub-card__header { flex-direction: column; }
  .section-actions { width: 100%; justify-content: flex-start; }
}
</style>
