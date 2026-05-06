<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <strong>条码模块配置</strong>
          <el-button type="primary" @click="openCreate">新增</el-button>
        </div>
      </template>

      <el-form inline :model="query">
        <el-form-item label="编码">
          <el-input v-model="query.barcodeCode" clearable placeholder="模糊" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.barcodeName" clearable placeholder="模糊" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="applyFilter">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="displayRows" border empty-text="暂无数据">
        <el-table-column prop="barcodeCode" label="条码模块编码" min-width="120" />
        <el-table-column prop="barcodeName" label="条码模块名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="启用" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enableFlag === 'Y' ? 'success' : 'info'">{{ row.enableFlag === 'Y' ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联业务模块" min-width="160" align="center">
          <template #default="{ row }">
            <el-button
              v-if="(row.linkedBusinessModuleCount ?? 0) > 0"
              link
              type="primary"
              @click="openLinkedModules(row)"
            >
              {{ row.linkedBusinessModuleCount }}
            </el-button>
            <span v-else class="muted">未关联业务模块</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-tooltip
              :disabled="(row.linkedBusinessModuleCount ?? 0) === 0"
              content="已有业务模块映射该条码模块，请先解除映射后再删除"
              placement="top"
            >
              <span style="margin-left: 8px">
                <el-button
                  link
                  type="danger"
                  :disabled="(row.linkedBusinessModuleCount ?? 0) > 0"
                  @click="remove(row)"
                >
                  删除
                </el-button>
              </span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="mode === 'create' ? '新增条码模块' : '编辑条码模块'" width="560px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="条码模块编码" required>
          <el-input
            v-model="form.barcodeCode"
            maxlength="5"
            show-word-limit
            :disabled="mode === 'edit'"
            placeholder="1～5 位：数字、大写字母、连接符 -"
            @input="onBarcodeCodeInput"
          />
          <div class="field-hint">仅允许数字、大写字母与「-」，最多 5 位字符。</div>
        </el-form-item>
        <el-form-item label="条码模块名称" required>
          <el-input v-model="form.barcodeName" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="启用">
          <el-radio-group v-model="form.enableFlag">
            <el-radio value="Y">是</el-radio>
            <el-radio value="N">否</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="linkedVisible" title="关联的业务模块" width="520px" destroy-on-close>
      <el-table v-loading="linkedLoading" :data="linkedRows" border empty-text="暂无关联">
        <el-table-column prop="moduleCode" label="业务模块编码" min-width="160" />
        <el-table-column prop="moduleName" label="业务模块名称" min-width="200" show-overflow-tooltip />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="linkedVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import {
  createBarcodeModule,
  deleteBarcodeModule,
  fetchBarcodeModules,
  fetchLinkedBusinessModules,
  updateBarcodeModule,
  type BarcodeModuleCommand,
  type LinkedBusinessModuleItem
} from '../../api/modules/barcodeModule'
import type { BarcodeModule } from '../../types'

const rows = ref<BarcodeModule[]>([])
/** 表格展示数据：仅在点击「查询」或「重置」、以及加载/保存后刷新，不在输入框变更时自动筛选 */
const displayRows = ref<BarcodeModule[]>([])
const query = reactive({ barcodeCode: '', barcodeName: '' })
const visible = ref(false)
const mode = ref<'create' | 'edit'>('create')
const saving = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<BarcodeModuleCommand>({
  barcodeCode: '',
  barcodeName: '',
  description: '',
  enableFlag: 'Y'
})

const linkedVisible = ref(false)
const linkedLoading = ref(false)
const linkedRows = ref<LinkedBusinessModuleItem[]>([])
function normalizeBarcodeCodeInput(raw: string) {
  return raw
    .replace(/[^0-9A-Za-z\-]/g, '')
    .toUpperCase()
    .slice(0, 5)
}

function onBarcodeCodeInput() {
  form.barcodeCode = normalizeBarcodeCodeInput(form.barcodeCode || '')
}

function applyFilterFromQuery() {
  const c = query.barcodeCode.trim().toLowerCase()
  const n = query.barcodeName.trim().toLowerCase()
  displayRows.value = rows.value.filter((r) => {
    const okC = !c || String(r.barcodeCode || '').toLowerCase().includes(c)
    const okN = !n || String(r.barcodeName || '').toLowerCase().includes(n)
    return okC && okN
  })
}

const load = async () => {
  rows.value = await fetchBarcodeModules()
  displayRows.value = [...rows.value]
}

const applyFilter = () => {
  applyFilterFromQuery()
}

const resetQuery = () => {
  query.barcodeCode = ''
  query.barcodeName = ''
  displayRows.value = [...rows.value]
}

const openCreate = () => {
  mode.value = 'create'
  editingId.value = null
  form.barcodeCode = ''
  form.barcodeName = ''
  form.description = ''
  form.enableFlag = 'Y'
  visible.value = true
}

const openEdit = (row: BarcodeModule) => {
  mode.value = 'edit'
  editingId.value = row.barcodeId
  form.barcodeCode = row.barcodeCode
  form.barcodeName = row.barcodeName
  form.description = row.description || ''
  form.enableFlag = row.enableFlag
  visible.value = true
}

async function openLinkedModules(row: BarcodeModule) {
  linkedVisible.value = true
  linkedLoading.value = true
  linkedRows.value = []
  try {
    linkedRows.value = await fetchLinkedBusinessModules(row.barcodeCode)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    linkedLoading.value = false
  }
}

const save = async () => {
  const code = normalizeBarcodeCodeInput(form.barcodeCode.trim())
  if (!code) return ElMessage.warning('请输入条码模块编码')
  if (!form.barcodeName.trim()) return ElMessage.warning('请输入条码模块名称')
  saving.value = true
  try {
    if (mode.value === 'create') {
      await createBarcodeModule({
        barcodeCode: code,
        barcodeName: form.barcodeName.trim(),
        description: form.description?.trim() || undefined,
        enableFlag: form.enableFlag
      })
      ElMessage.success('已新增')
    } else if (editingId.value != null) {
      await updateBarcodeModule(editingId.value, {
        barcodeCode: code,
        barcodeName: form.barcodeName.trim(),
        description: form.description?.trim() || undefined,
        enableFlag: form.enableFlag
      })
      ElMessage.success('已保存')
    }
    visible.value = false
    await load()
    applyFilterFromQuery()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

const remove = (row: BarcodeModule) => {
  if ((row.linkedBusinessModuleCount ?? 0) > 0) return
  ElMessageBox.confirm(`确定删除条码模块「${row.barcodeName}」（${row.barcodeCode}）吗？`, '删除确认', {
    type: 'warning'
  })
    .then(async () => {
      await deleteBarcodeModule(row.barcodeId)
      ElMessage.success('已删除')
      await load()
      applyFilterFromQuery()
    })
    .catch(() => {})
}

onMounted(() => {
  void load()
})
</script>

<style scoped>
.page {
  padding: 16px;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.muted {
  color: #909399;
  font-size: 13px;
}
.field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}
</style>
