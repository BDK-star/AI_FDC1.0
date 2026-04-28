<template>
  <div class="page">
    <el-card shadow="never">
      <template #header><strong>四性检测方案配置</strong></template>
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="检测方案名称">
          <el-input v-model="queryForm.inspectionName" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="检测环节">
          <el-select v-model="queryForm.inspectionStage" clearable style="width: 180px">
            <el-option
              v-for="item in inspectionStageOptions"
              :key="item.itemCode"
              :label="item.itemName"
              :value="item.itemCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-select v-model="queryForm.enableFlag" style="width: 180px">
            <el-option label="是" value="Y" />
            <el-option label="否" value="N" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="toolbar">
          <el-button type="primary" @click="openCreate">新增</el-button>
          <el-button type="primary" :disabled="!hasEditingRows" @click="saveEditingRows">保存</el-button>
          <el-button @click="onImport">导入</el-button>
          <el-button @click="onExport">导出</el-button>
        </div>
      </template>
      <input ref="importInputRef" type="file" accept=".csv,text/csv" style="display: none" @change="onImportFileChange" />
      <el-table :data="listData" @current-change="onCurrentChange" highlight-current-row border>
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="row.__editing && row.__isNew" link type="danger" @click="cancelNewRow(row)">删除</el-button>
            <el-button v-if="!row.__editing" link type="primary" @click="startEditRow(row)">编辑</el-button>
            <el-button v-if="!row.__editing" link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        <el-table-column label="检测方案名称" min-width="180">
          <template #default="{ row }">
            <el-input v-if="row.__editing" v-model="row.inspectionName" />
            <span v-else>{{ row.inspectionName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="检测环节" width="140">
          <template #default="{ row }">
            <el-select v-if="row.__editing" v-model="row.inspectionStage">
              <el-option
                v-for="item in inspectionStageOptions"
                :key="item.itemCode"
                :label="item.itemName"
                :value="item.itemCode"
              />
            </el-select>
            <span v-else>{{ stageLabelMap[row.inspectionStage] || row.inspectionStage }}</span>
          </template>
        </el-table-column>
        <el-table-column label="数据包规范" min-width="140">
          <template #default="{ row }">
            <el-input v-if="row.__editing" v-model="row.dataPackageSpec" />
            <span v-else>{{ row.dataPackageSpec }}</span>
          </template>
        </el-table-column>
        <el-table-column label="元数据规范" min-width="140">
          <template #default="{ row }">
            <el-input v-if="row.__editing" v-model="row.metadataSpec" />
            <span v-else>{{ row.metadataSpec }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否启用" width="100">
          <template #default="{ row }">
            <el-switch v-if="row.__editing" v-model="row.enableFlag" active-value="Y" inactive-value="N" />
            <span v-else>{{ row.enableFlag === 'Y' ? '是' : '否' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建人" width="180">
          <template #default="{ row }">{{ formatUserDisplay(row.createdBy) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="120">
          <template #default="{ row }">{{ formatDate(row.creationDate) }}</template>
        </el-table-column>
        <el-table-column label="最后更新人" width="180">
          <template #default="{ row }">{{ formatUserDisplay(row.lastUpdatedBy) }}</template>
        </el-table-column>
        <el-table-column label="最后更新时间" min-width="120">
          <template #default="{ row }">{{ formatDate(row.lastUpdateDate) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-drawer v-model="detailVisible" size="80%" title="四性检测详情">
      <div class="detail-actions">
        <el-button type="primary" @click="saveDetails">保存</el-button>
        <el-button @click="detailVisible = false">取消</el-button>
      </div>
      <el-collapse v-model="activePanels">
        <el-collapse-item v-for="item in panelDefs" :key="item.type" :name="item.type" :title="item.label">
          <div style="margin-bottom: 12px">
            <el-button type="primary" size="small" @click="appendDetail(item.type)">新增</el-button>
          </div>
          <el-table :data="detailsByType(item.type)">
            <el-table-column type="index" label="序号" width="70" />
            <el-table-column prop="inspectionType" label="检测类型" width="120" />
            <el-table-column label="检测编号" min-width="120"><template #default="{ row }"><el-input v-model="row.inspectionCode" /></template></el-table-column>
            <el-table-column label="检测项" min-width="160"><template #default="{ row }"><el-input v-model="row.inspectionItem" /></template></el-table-column>
            <el-table-column label="检测目的" min-width="180"><template #default="{ row }"><el-input v-model="row.inspectionPurpose" /></template></el-table-column>
            <el-table-column label="检测对象" min-width="180"><template #default="{ row }"><el-input v-model="row.inspectionObject" /></template></el-table-column>
            <el-table-column label="检测依据和方法" min-width="220"><template #default="{ row }"><el-input v-model="row.inspectionBasisMethod" /></template></el-table-column>
            <el-table-column label="是否启用" width="100"><template #default="{ row }"><el-switch v-model="row.enableFlag" active-value="Y" inactive-value="N" /></template></el-table-column>
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { fetchDictionaryItems } from '../../api/modules/dictionary'
import { fetchUsers } from '../../api/modules/security'
import {
  createFourAttrInspection,
  exportFourAttrInspections,
  getFourAttrInspectionDetail,
  importFourAttrInspections,
  queryFourAttrInspections,
  saveFourAttrInspectionDetails,
  updateFourAttrInspection,
  type FourAttrInspectionSaveCommand
} from '../../api/modules/archiveManagement'
import type { DictionaryItem, FourAttrInspectionConfig, FourAttrInspectionDetail } from '../../types'

type FourAttrInspectionRow = FourAttrInspectionConfig & {
  __isNew?: boolean
  __editing?: boolean
}

const queryForm = reactive({ inspectionName: '', inspectionStage: '', enableFlag: 'Y' as 'Y' | 'N' })
const listData = ref<FourAttrInspectionRow[]>([])
const currentRow = ref<FourAttrInspectionRow>()
const detailVisible = ref(false)
const currentDetails = ref<FourAttrInspectionDetail[]>([])
const importInputRef = ref<HTMLInputElement>()
const inspectionStageOptions = ref<DictionaryItem[]>([])
const userDisplayById = ref<Record<number, string>>({})
const activePanels = ref(['AUTHENTICITY', 'INTEGRITY', 'USABILITY', 'SECURITY'])
const panelDefs = [
  { type: 'AUTHENTICITY', label: '真实性' },
  { type: 'INTEGRITY', label: '完整性' },
  { type: 'USABILITY', label: '可用性' },
  { type: 'SECURITY', label: '安全性' }
]
const hasEditingRows = computed(() => listData.value.some(row => row.__editing))
const stageLabelMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  for (const item of inspectionStageOptions.value) {
    map[item.itemCode] = item.itemName
  }
  return map
})

const loadInspectionStageOptions = async () => {
  try {
    const items = await fetchDictionaryItems('FOUR_ATTR_INSPECTION')
    inspectionStageOptions.value = items.filter(item => item.enabledFlag === 'Y').sort((a, b) => a.sortOrder - b.sortOrder)
  } catch (e: any) {
    ElMessage.error(e?.message || '检测环节字典加载失败')
  }
}

const loadList = async () => {
  try {
    const rows = await queryFourAttrInspections({ ...queryForm })
    listData.value = rows.map(row => ({ ...row, __isNew: false, __editing: false }))
  } catch (e: any) {
    ElMessage.error(e?.message || '查询失败')
  }
}
const resetQuery = async () => {
  queryForm.inspectionName = ''
  queryForm.inspectionStage = ''
  queryForm.enableFlag = 'Y'
  await loadList()
}
const onCurrentChange = (row?: FourAttrInspectionConfig) => {
  currentRow.value = row as FourAttrInspectionRow | undefined
}
const openCreate = () => {
  const emptyRow: FourAttrInspectionRow = {
    inspectionId: 0,
    inspectionName: '',
    inspectionStage: inspectionStageOptions.value[0]?.itemCode || '',
    dataPackageSpec: '',
    metadataSpec: '',
    enableFlag: 'Y',
    details: [],
    __isNew: true,
    __editing: true
  }
  listData.value = [emptyRow, ...listData.value]
  currentRow.value = emptyRow
}
const startEditRow = (row: FourAttrInspectionRow) => {
  row.__editing = true
}
const saveRow = async (row: FourAttrInspectionRow) => {
  const payload: FourAttrInspectionSaveCommand = {
    inspectionName: row.inspectionName?.trim(),
    inspectionStage: row.inspectionStage,
    dataPackageSpec: row.dataPackageSpec?.trim(),
    metadataSpec: row.metadataSpec?.trim(),
    enableFlag: row.enableFlag
  }
  if (!payload.inspectionName || !payload.inspectionStage || !payload.dataPackageSpec || !payload.metadataSpec) {
    return ElMessage.warning('请完整填写方案名称、检测环节、数据包规范、元数据规范')
  }
  try {
    if (row.__isNew) await createFourAttrInspection(payload)
    else await updateFourAttrInspection(row.inspectionId, payload)
    await loadList()
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}
const saveEditingRows = async () => {
  const targets = listData.value.filter(row => row.__editing)
  if (!targets.length) return
  for (const row of targets) {
    await saveRow(row)
  }
}
const cancelNewRow = (row: FourAttrInspectionRow) => {
  listData.value = listData.value.filter(item => item !== row)
}
const openDetail = async (row?: FourAttrInspectionRow) => {
  const target = row ?? currentRow.value
  if (!target || !target.inspectionId) return ElMessage.warning('请先选择一条数据')
  try {
    currentRow.value = target
    const detail = await getFourAttrInspectionDetail(target.inspectionId)
    currentDetails.value = detail.details || []
    detailVisible.value = true
  } catch (e: any) {
    ElMessage.error(e?.message || '加载详情失败')
  }
}
const detailsByType = (type: string) => computed(() => currentDetails.value.filter(item => item.inspectionType === type)).value
const appendDetail = (type: string) => {
  currentDetails.value.push({
    inspectionType: type,
    inspectionCode: '',
    inspectionItem: '',
    inspectionPurpose: '',
    inspectionObject: '',
    inspectionBasisMethod: '',
    enableFlag: 'Y',
    displayOrder: detailsByType(type).length + 1
  })
}
const saveDetails = async () => {
  if (!currentRow.value) return
  try {
    await saveFourAttrInspectionDetails(currentRow.value.inspectionId, {
      inspectionId: currentRow.value.inspectionId,
      details: currentDetails.value.map((item, idx) => ({ ...item, displayOrder: idx + 1 }))
    })
    detailVisible.value = false
    ElMessage.success('明细保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '明细保存失败')
  }
}
const onImport = () => {
  if (!importInputRef.value) return
  importInputRef.value.value = ''
  importInputRef.value.click()
}
const onImportFileChange = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const imported = await importFourAttrInspections(file)
    ElMessage.success(`导入完成，共处理 ${imported} 行`)
    await loadList()
  } catch (e: any) {
    ElMessage.error(e?.message || '导入失败')
  }
}
const onExport = async () => {
  try {
    const blob = await exportFourAttrInspections({ ...queryForm })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'four-attr-inspections.csv'
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '导出失败')
  }
}

const formatDate = (value?: string) => {
  if (!value) return ''
  const text = value.replace('T', ' ')
  return text.slice(0, 10)
}

const formatUserDisplay = (userId?: number) => {
  if (!userId || userId <= 0) return '-'
  return userDisplayById.value[userId] || String(userId)
}

const loadUserDisplays = async () => {
  try {
    const users = await fetchUsers()
    userDisplayById.value = Object.fromEntries(
      users.map((u) => [
        Number(u.userId),
        `${u.userName || u.username || `用户-${u.userId}`}${u.employeeNo ? ` ${u.employeeNo}` : ''}`
      ])
    )
  } catch {
    userDisplayById.value = {}
  }
}

onMounted(async () => {
  await loadInspectionStageOptions()
  await loadUserDisplays()
  await loadList()
})
</script>

<style scoped>
.page {
  display: grid;
  gap: 16px;
}
.query-form {
  display: flex;
  flex-wrap: wrap;
}
.toolbar {
  display: flex;
  gap: 10px;
}
.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 12px;
}
</style>
