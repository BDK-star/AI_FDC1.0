<template>
  <Teleport to="body">
    <Transition name="f03-batch-fade">
      <div
        v-if="modelValue"
        class="f03-batch-overlay"
        role="dialog"
        aria-modal="true"
        @click.self="onOverlayClick"
      >
        <div class="f03-batch-modal" @click.stop>
          <div class="f03-batch-modal__header">
            <div class="f03-batch-modal__title-row">
              <el-icon class="f03-batch-modal__title-icon" :size="22">
                <Upload v-if="headerIcon === 'upload'" />
                <RefreshRight v-else />
              </el-icon>
              <h3 class="f03-batch-modal__title">{{ title }}</h3>
            </div>
            <button type="button" class="f03-batch-modal__close" aria-label="关闭" @click="close">
              <el-icon :size="20"><Close /></el-icon>
            </button>
          </div>

          <div class="f03-batch-modal__body">
            <div class="f03-batch-modal__template-card">
              <el-icon class="f03-batch-modal__template-icon" :size="28"><Document /></el-icon>
              <div class="f03-batch-modal__template-text">
                <p class="f03-batch-modal__template-heading">导入模板</p>
                <p class="f03-batch-modal__template-desc">
                  {{ hint }}
                </p>
                <button
                  type="button"
                  class="f03-batch-modal__download"
                  :disabled="!templateCsvReady"
                  :title="templateCsvReady ? '' : '模板内容尚未就绪，请稍候再试'"
                  @click="downloadTemplate"
                >
                  <el-icon :size="16"><Download /></el-icon>
                  下载导入模板
                </button>
              </div>
            </div>

            <div class="f03-batch-modal__upload-block">
              <span class="f03-batch-modal__upload-label">选择文件</span>
              <el-upload
                ref="uploadRef"
                class="f03-batch-modal__upload"
                drag
                :auto-upload="false"
                :limit="1"
                :accept="accept"
                :file-list="fileList"
                :on-change="onFileChange"
                :on-remove="onFileRemove"
              >
                <el-icon class="f03-batch-modal__upload-cloud"><UploadFilled /></el-icon>
                <p class="f03-batch-modal__upload-hint">点击或拖拽文件到此处上传</p>
                <p class="f03-batch-modal__upload-sub">{{ acceptHint }}</p>
              </el-upload>
              <p v-if="selectedName" class="f03-batch-modal__selected-name">已选：{{ selectedName }}</p>
            </div>

            <div v-if="enableOperationInputs" class="f03-batch-modal__op-block">
              <span class="f03-batch-modal__upload-label">本次操作备注</span>
              <el-input v-model="operationRemark" type="textarea" :rows="2" placeholder="可选，将写入文档操作日志" />
              <template v-if="enableAuditAttachmentUpload">
                <span class="f03-batch-modal__upload-label">补充说明附件</span>
                <el-upload :http-request="handleAuditUpload" :show-file-list="false" multiple>
                  <el-button type="primary" plain>上传文件</el-button>
                </el-upload>
                <div v-if="auditAttachments.length" class="f03-batch-modal__audit-tags">
                  <el-tag v-for="(a, i) in auditAttachments" :key="(a.storageKey || a.fileName || '') + i" closable @close="auditAttachments.splice(i, 1)">
                    {{ a.fileName || a.storageKey || '附件' }}
                  </el-tag>
                </div>
              </template>
            </div>
          </div>

          <div class="f03-batch-modal__footer">
            <div class="f03-batch-modal__footer-left">
              <button
                v-if="showGoMyImport"
                type="button"
                class="f03-batch-modal__btn f03-batch-modal__btn--secondary"
                @click="goMyImport"
              >
                前往我的导入
              </button>
            </div>
            <div class="f03-batch-modal__footer-right">
              <button type="button" class="f03-batch-modal__btn f03-batch-modal__btn--secondary" @click="close">
                取消
              </button>
              <button
                type="button"
                class="f03-batch-modal__btn f03-batch-modal__btn--primary"
                :disabled="loading"
                @click="submit"
              >
                {{ loading ? '处理中…' : confirmLabel }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { Close, Document, Download, RefreshRight, Upload, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadInstance, UploadRequestOptions, UploadUserFile } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    /** 与原型一致：批量更新用 sync 图标 */
    headerIcon?: 'upload' | 'sync'
    hint?: string
    templateCsv: string
    downloadFileName: string
    accept?: string
    acceptHint?: string
    confirmLabel?: string
    loading?: boolean
    enableOperationInputs?: boolean
    /** 应归档批量创建：跳转「我的导入」 */
    showGoMyImport?: boolean
    /** 异步批量创建等场景可不展示随单审计附件 */
    enableAuditAttachmentUpload?: boolean
    uploadPendingAuditAttachment?: (file: File) => Promise<{ fileId: number; fileName?: string; storageKey?: string; fileSize?: number }>
  }>(),
  {
    headerIcon: 'upload',
    hint: '请下载模板文件，按照模板格式填写数据后上传。',
    accept: '.csv,.txt,text/csv,.xlsx,.xls',
    acceptHint: '支持 CSV；如需 Excel 请先按模板另存为 CSV再上传',
    confirmLabel: '确认导入',
    loading: false,
    enableOperationInputs: false,
    showGoMyImport: false,
    enableAuditAttachmentUpload: true
  }
)

const emit = defineEmits<{
  'update:modelValue': [boolean]
  confirm: [payload: { file: File | null; operationRemark?: string; auditAttachments?: Array<{ fileId: number; fileName?: string; storageKey?: string; fileSize?: number }> }]
}>()

const router = useRouter()

const uploadRef = ref<UploadInstance>()
const fileList = ref<UploadUserFile[]>([])
const selectedFile = ref<File | null>(null)
const operationRemark = ref('')
const auditAttachments = ref<Array<{ fileId: number; fileName?: string; storageKey?: string; fileSize?: number }>>([])
const selectedName = computed(() => selectedFile.value?.name ?? '')
const templateCsvReady = computed(() => (props.templateCsv ?? '').trim().length > 0)

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      fileList.value = []
      selectedFile.value = null
      operationRemark.value = ''
      auditAttachments.value = []
      uploadRef.value?.clearFiles()
    }
  }
)

const close = () => {
  emit('update:modelValue', false)
}

const goMyImport = () => {
  close()
  void router.push('/workspace/import-query')
}

const onOverlayClick = () => {
  close()
}

const onFileChange = (file: UploadFile) => {
  selectedFile.value = (file.raw as File) || null
  fileList.value = file.raw
    ? [{ name: file.name, uid: file.uid, status: 'ready', raw: file.raw }]
    : []
}

const onFileRemove = () => {
  selectedFile.value = null
  fileList.value = []
}

const downloadTemplate = () => {
  const raw = (props.templateCsv || '').trim()
  if (!raw) {
    ElMessage.warning('模板尚未加载完成，请稍候再下载')
    return
  }
  const blob = new Blob(['\uFEFF' + raw], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = props.downloadFileName || 'import-template.csv'
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
  ElMessage.success('模板已下载')
}

const submit = () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择要上传的文件')
    return
  }
  emit('confirm', {
    file: selectedFile.value,
    operationRemark: operationRemark.value.trim() || undefined,
    auditAttachments: auditAttachments.value.length ? auditAttachments.value : undefined
  })
}

const handleAuditUpload = async (opt: UploadRequestOptions) => {
  try {
    if (!props.uploadPendingAuditAttachment) {
      throw new Error('附件上传能力未配置')
    }
    const ref = await props.uploadPendingAuditAttachment(opt.file as File)
    if (ref.fileId != null && ref.fileId > 0) {
      auditAttachments.value.push(ref)
    }
    ElMessage.success('已上传')
    opt.onSuccess?.({} as any)
  } catch (e: unknown) {
    opt.onError?.(e as any)
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  }
}
</script>

<style scoped>
.f03-batch-fade-enter-active,
.f03-batch-fade-leave-active {
  transition: opacity 0.2s ease;
}
.f03-batch-fade-enter-from,
.f03-batch-fade-leave-to {
  opacity: 0;
}

.f03-batch-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  padding: 16px;
  box-sizing: border-box;
}

.f03-batch-modal {
  width: 100%;
  max-width: 32rem;
  background: #fff;
  border-radius: 12px;
  box-shadow:
    0 25px 50px -12px rgba(0, 0, 0, 0.25),
    0 0 0 1px rgba(219, 224, 230, 0.8);
  overflow: hidden;
  font-family:
    'Microsoft YaHei',
    '微软雅黑',
    Inter,
    system-ui,
    -apple-system,
    sans-serif;
}

.f03-batch-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid #dbe0e6;
  background: #f9fafb;
}

.f03-batch-modal__title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.f03-batch-modal__title-icon {
  color: #1173d4;
}

.f03-batch-modal__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111418;
  line-height: 1.3;
}

.f03-batch-modal__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #617589;
  cursor: pointer;
  transition:
    background 0.15s ease,
    color 0.15s ease;
}
.f03-batch-modal__close:hover {
  background: #f3f4f6;
  color: #111418;
}

.f03-batch-modal__body {
  padding: 24px;
}

.f03-batch-modal__template-card {
  display: flex;
  gap: 12px;
  padding: 16px;
  margin-bottom: 24px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
}

.f03-batch-modal__template-icon {
  flex-shrink: 0;
  color: #1173d4;
}

.f03-batch-modal__template-heading {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #111418;
}

.f03-batch-modal__template-desc {
  margin: 0 0 8px;
  font-size: 12px;
  line-height: 1.5;
  color: #617589;
}

.f03-batch-modal__download {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: none;
  font-size: 14px;
  font-weight: 500;
  color: #1173d4;
  cursor: pointer;
}
.f03-batch-modal__download:hover {
  color: #0d5caa;
}

.f03-batch-modal__upload-block {
  margin-bottom: 0;
}

.f03-batch-modal__upload-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #617589;
}

.f03-batch-modal__upload :deep(.el-upload) {
  width: 100%;
}

.f03-batch-modal__upload :deep(.el-upload-dragger) {
  width: 100%;
  padding: 32px 24px;
  border: 2px dashed #dbe0e6;
  border-radius: 8px;
  background: #fff;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.f03-batch-modal__upload :deep(.el-upload-dragger:hover) {
  border-color: #1173d4;
  background: rgba(17, 115, 212, 0.04);
}

.f03-batch-modal__upload-cloud {
  font-size: 40px;
  color: #617589;
  margin-bottom: 8px;
}

.f03-batch-modal__upload-hint {
  margin: 0 0 4px;
  font-size: 14px;
  color: #617589;
}

.f03-batch-modal__upload-sub {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
}

.f03-batch-modal__selected-name {
  margin: 8px 0 0;
  font-size: 13px;
  color: #111418;
}

.f03-batch-modal__op-block {
  margin-top: 16px;
  display: grid;
  gap: 8px;
}

.f03-batch-modal__audit-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.f03-batch-modal__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid #dbe0e6;
  background: #f9fafb;
}

.f03-batch-modal__footer-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.f03-batch-modal__btn {
  min-width: 88px;
  height: 36px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  cursor: pointer;
  transition:
    background 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease;
}

.f03-batch-modal__btn--secondary {
  border: 1px solid #dbe0e6;
  background: #fff;
  color: #617589;
}
.f03-batch-modal__btn--secondary:hover {
  background: #f3f4f6;
}

.f03-batch-modal__btn--primary {
  border: none;
  background: #1173d4;
  color: #fff;
}
.f03-batch-modal__btn--primary:hover:not(:disabled) {
  background: #0d5caa;
}
.f03-batch-modal__btn--primary:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}
</style>
