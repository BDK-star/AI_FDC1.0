<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <strong>公司管理</strong>
          <el-button type="primary" @click="openCreate">新增</el-button>
        </div>
      </template>

      <el-form inline :model="query">
        <el-form-item label="公司编码">
          <el-input v-model="query.companyCode" clearable />
        </el-form-item>
        <el-form-item label="公司名称">
          <el-input v-model="query.companyName" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="rows" border>
        <el-table-column prop="companyCode" label="公司编码" min-width="140" />
        <el-table-column prop="companyName" label="公司名称" min-width="180" />
        <el-table-column prop="region" label="地区部" min-width="120" />
        <el-table-column prop="representativeOffice" label="代表处" min-width="120" />
        <el-table-column prop="country" label="国家" min-width="120" />
        <el-table-column label="标签" min-width="200">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags || []" :key="tag" class="mr">{{ tag }}</el-tag>
            <span v-if="!row.tags?.length">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabledFlag === 'Y' ? 'success' : 'info'">{{ row.enabledFlag === 'Y' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="mode === 'create' ? '新增公司' : '编辑公司'" width="640px">
      <el-form label-position="top">
        <el-form-item label="公司编码" required>
          <el-input v-model="form.companyCode" :disabled="mode === 'edit'" />
        </el-form-item>
        <el-form-item label="公司名称" required>
          <el-input v-model="form.companyName" />
        </el-form-item>
        <el-form-item label="地区部">
          <el-input v-model="form.region" />
        </el-form-item>
        <el-form-item label="代表处">
          <el-input v-model="form.representativeOffice" />
        </el-form-item>
        <el-form-item label="国家">
          <el-input v-model="form.country" />
        </el-form-item>
        <el-form-item label="标签（逗号分隔）">
          <el-input v-model="tagText" placeholder="示例：能源,海外" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.enabledFlag">
            <el-radio value="Y">启用</el-radio>
            <el-radio value="N">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'
import { createCompanyInfo, deleteCompanyInfo, fetchCompanyInfos, updateCompanyInfo, type CompanyInfoCommand } from '../../api/modules/companyInfo'
import type { CompanyInfo } from '../../types'

const rows = ref<CompanyInfo[]>([])
const query = reactive({ companyCode: '', companyName: '' })
const visible = ref(false)
const mode = ref<'create' | 'edit'>('create')
const tagText = ref('')
const saving = ref(false)
const form = reactive<CompanyInfoCommand>({
  companyCode: '',
  companyName: '',
  region: '',
  representativeOffice: '',
  country: '',
  description: '',
  tags: [],
  enabledFlag: 'Y'
})

const load = async () => {
  rows.value = await fetchCompanyInfos({
    companyCodes: query.companyCode.trim() ? [query.companyCode.trim()] : undefined,
    companyName: query.companyName.trim() || undefined
  })
}

const reset = async () => {
  query.companyCode = ''
  query.companyName = ''
  await load()
}

const openCreate = () => {
  mode.value = 'create'
  Object.assign(form, { companyCode: '', companyName: '', region: '', representativeOffice: '', country: '', description: '', tags: [], enabledFlag: 'Y' })
  tagText.value = ''
  visible.value = true
}

const openEdit = (row: CompanyInfo) => {
  mode.value = 'edit'
  Object.assign(form, {
    companyCode: row.companyCode,
    companyName: row.companyName,
    region: row.region || '',
    representativeOffice: row.representativeOffice || '',
    country: row.country || '',
    description: row.description || '',
    tags: [...(row.tags || [])],
    enabledFlag: row.enabledFlag
  })
  tagText.value = (row.tags || []).join(',')
  visible.value = true
}

const save = async () => {
  if (saving.value) return
  if (!form.companyCode.trim() || !form.companyName.trim()) {
    ElMessage.warning('请填写公司编码和名称')
    return
  }

  saving.value = true
  try {
    form.tags = tagText.value.split(',').map((v) => v.trim()).filter(Boolean)
    if (mode.value === 'create') {
      await createCompanyInfo({ ...form, companyCode: form.companyCode.trim(), companyName: form.companyName.trim() })
    } else {
      await updateCompanyInfo(form.companyCode, { ...form, companyName: form.companyName.trim() })
    }
    visible.value = false
    ElMessage.success('保存成功')
    try {
      await load()
    } catch {
      ElMessage.warning('保存成功，但列表刷新失败，请手动点击查询刷新')
    }
  } catch (error) {
    const responseMessage = (error as { response?: { data?: { msg?: string; message?: string } } })?.response?.data
    ElMessage.error(responseMessage?.msg || responseMessage?.message || (error instanceof Error ? error.message : '保存失败'))
  } finally {
    saving.value = false
  }
}

const remove = async (row: CompanyInfo) => {
  await ElMessageBox.confirm(`确认删除公司 ${row.companyCode} 吗？`, '提示', { type: 'warning' })
  await deleteCompanyInfo(row.companyCode)
  await load()
  ElMessage.success('删除成功')
}

onMounted(load)
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.head { display: flex; justify-content: space-between; align-items: center; }
.mr { margin-right: 6px; }
</style>
