<template>
  <div class="company-project-page">
    <el-card shadow="never">
      <div class="toolbar">
        <el-form :inline="true" :model="query" class="query-form">
          <el-form-item label="关键字">
            <el-input v-model.trim="query.keyword" placeholder="请输入编码或名称" clearable />
          </el-form-item>
          <el-form-item label="国家">
            <el-select v-model="query.countryCode" placeholder="全部" clearable style="width: 160px">
              <el-option v-for="item in countries" :key="item.countryCode" :label="item.countryName" :value="item.countryCode" />
            </el-select>
          </el-form-item>
          <el-form-item label="地区部">
            <el-input v-model.trim="query.managementArea" placeholder="请输入地区部" clearable />
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
          <el-button type="success" @click="router.push('/base-data/company-projects/create')">新建公司</el-button>
        </div>
      </div>

      <el-table :data="items" border empty-text="暂无公司数据，请调整筛选条件或新建数据">
        <el-table-column prop="companyProjectCode" label="公司编码" min-width="160" />
        <el-table-column prop="companyProjectName" label="公司名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="国家" min-width="120">
          <template #default="{ row }">{{ formatCountry(row.countryCode) }}</template>
        </el-table-column>
        <el-table-column prop="managementArea" label="地区部" min-width="140" show-overflow-tooltip />
        <el-table-column label="启用标识" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabledFlag === 'Y' ? 'success' : 'info'">{{ row.enabledFlag === 'Y' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="organizationCount" label="组织行数" width="100" align="center" />
        <el-table-column prop="lastUpdateDate" label="最后更新时间" min-width="180" />
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.companyProjectCode, 'view')">查看</el-button>
            <el-button link type="primary" @click="openDetail(row.companyProjectCode, 'edit')">编辑</el-button>
            <el-popconfirm title="确认软删除该公司吗？" @confirm="removeItem(row.companyProjectCode)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { onActivated, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteCompanyProject, fetchCompanyProjectCountries, fetchCompanyProjects, type CompanyProjectQuery } from '../../api/modules/companyProject'
import type { CompanyProjectSummary, CountryOption } from '../../types'

const router = useRouter()
const route = useRoute()

const query = reactive<CompanyProjectQuery>({
  keyword: '',
  countryCode: '',
  managementArea: '',
  enabledFlag: ''
})

const items = ref<CompanyProjectSummary[]>([])
const countries = ref<CountryOption[]>([])

const syncQueryFromRoute = () => {
  query.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
}

const loadList = async () => {
  items.value = await fetchCompanyProjects({
    keyword: query.keyword?.trim() || undefined,
    countryCode: query.countryCode || undefined,
    managementArea: query.managementArea?.trim() || undefined,
    enabledFlag: query.enabledFlag || undefined
  })
}

const loadCountries = async () => {
  countries.value = await fetchCompanyProjectCountries()
}

const resetQuery = async () => {
  query.keyword = ''
  query.countryCode = ''
  query.managementArea = ''
  query.enabledFlag = ''
  await loadList()
}

const openDetail = (companyProjectCode: string, mode: 'view' | 'edit') => {
  router.push(`/base-data/company-projects/${companyProjectCode}/${mode}`)
}

const removeItem = async (companyProjectCode: string) => {
  try {
    await deleteCompanyProject(companyProjectCode)
    ElMessage.success('公司已软删除')
    await loadList()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

const formatCountry = (countryCode: string) => countries.value.find((item) => item.countryCode === countryCode)?.countryName ?? countryCode

onMounted(async () => {
  syncQueryFromRoute()
  await loadCountries()
  await loadList()
})

onActivated(async () => {
  syncQueryFromRoute()
  await loadList()
})

watch(() => route.query.keyword, async () => {
  syncQueryFromRoute()
  await loadList()
})
</script>

<style scoped>
.company-project-page { display: grid; gap: 16px; }
.toolbar { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 16px; flex-wrap: wrap; }
.query-form { flex: 1; }
.toolbar-actions { display: flex; gap: 12px; align-items: flex-start; }
</style>
