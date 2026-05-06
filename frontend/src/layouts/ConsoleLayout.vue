<template>
  <el-container class="console-layout">
    <el-aside :width="layout.collapsed ? '72px' : '240px'" class="console-aside">
      <div class="brand" @click="goDashboard">
        <div class="brand-mark">档</div>
        <div v-show="!layout.collapsed" class="brand-copy">
          <strong>档案智能工作台</strong>
          <span>Smart Archive OS</span>
        </div>
      </div>

      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          class="menu"
          router
          :unique-opened="true"
          :collapse="layout.collapsed"
          background-color="#0d2238"
          text-color="#adc4d8"
          active-text-color="#ffffff"
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <span>首页工作台</span>
          </el-menu-item>

          <el-menu-item index="/archive-management/query">
            <el-icon><Search /></el-icon>
            <span>文档查询</span>
          </el-menu-item>

          <el-sub-menu index="/workspace">
            <template #title>
              <el-icon><List /></el-icon>
              <span>我的工作空间</span>
            </template>
            <el-menu-item index="/workspace/import-query">我的导入</el-menu-item>
            <el-menu-item index="/workspace/export-query">我的导出</el-menu-item>
            <el-menu-item index="/workspace/my-drafts">我的草稿</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="/base-data">
            <template #title>
              <el-icon><Files /></el-icon>
              <span>配置中心</span>
            </template>
            <el-menu-item index="/base-data/company-infos">公司管理</el-menu-item>
            <el-menu-item index="/base-data/company-projects">公司项目管理</el-menu-item>
            <el-menu-item index="/base-data/document-organizations">文档组织管理</el-menu-item>
            <el-menu-item index="/base-data/document-type-configs">文档类型配置</el-menu-item>
            <el-menu-item index="/base-data/business-modules">业务模块配置</el-menu-item>
            <el-menu-item index="/base-data/barcode-modules">条码模块配置</el-menu-item>
            <el-menu-item index="/base-data/archive-flow-rules">归档规则管理</el-menu-item>
            <el-menu-item index="/base-data/warehouse">库房管理</el-menu-item>
            <el-menu-item index="/base-data/department-signatories">权签人维护</el-menu-item>
            <el-menu-item index="/base-data/company-project-dictionaries">公司字典</el-menu-item>
            <el-menu-item index="/base-data/dictionaries">字典管理</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="/archive-management">
            <template #title>
              <el-icon><FolderOpened /></el-icon>
              <span>档案业务管理</span>
            </template>
            <el-menu-item index="/archive-management/pending-archive/query">应归档数据管理</el-menu-item>
            <el-menu-item index="/archive-management/create">发起归档</el-menu-item>
            <el-menu-item index="/archive-management/query?mine=1">待我归档</el-menu-item>
            <el-sub-menu index="archive-management-doc-transfer">
              <template #title>
                <span>文档移交</span>
              </template>
              <el-menu-item index="/archive-management/transfer">移交申请提交</el-menu-item>
              <el-menu-item index="/archive-management/transfer-query">移交申请查询</el-menu-item>
              <el-menu-item index="/archive-management/transfer-field-config">移交申请通用字段配置</el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/archive-management/ai-search">AI+档案</el-menu-item>
            <el-menu-item index="/archive-management/borrow">借阅文档</el-menu-item>
            <el-menu-item index="/archive-management/borrow-renew">续借申请</el-menu-item>
            <el-menu-item index="/archive-management/bind">成册整理</el-menu-item>
            <el-menu-item index="/archive-management/storage">入库上架</el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/governance">
            <el-icon><Setting /></el-icon>
            <span>流程规则与 AI 治理</span>
          </el-menu-item>
          <el-menu-item index="/workflow">
            <el-icon><List /></el-icon>
            <span>工作流管理</span>
          </el-menu-item>
          <el-sub-menu index="/security">
            <template #title>
              <el-icon>
                <svg viewBox="0 0 1024 1024" width="1em" height="1em" fill="currentColor" aria-hidden="true">
                  <path d="M512 64l320 128v245.632c0 218.112-131.008 414.656-332.288 499.84L512 960l12.288-22.528C323.008 852.288 192 655.744 192 437.632V192L512 64zm0 69.056L256 235.328v202.304c0 187.136 109.44 356.352 280.48 432.064L512 884.736l-24.48-15.04C316.48 793.984 256 624.768 256 437.632V235.328l256-102.272z"/>
                </svg>
              </el-icon>
              <span>档案安全管理</span>
            </template>
            <el-menu-item index="/security/user-role-config">系统角色配置</el-menu-item>
            <el-sub-menu index="/security/four-properties">
              <template #title>
                <span>四性检测</span>
              </template>
              <el-menu-item index="/security/four-properties/config">四性检测配置</el-menu-item>
              <el-menu-item index="/security/four-properties/execution">四性检测执行</el-menu-item>
              <el-menu-item index="/security/four-properties/report">四性检测报告</el-menu-item>
            </el-sub-menu>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="console-content">
      <el-header class="console-header">
        <div class="console-header__left">
          <el-button text class="icon-button" @click="layout.toggle()">
            <el-icon><component :is="layout.collapsed ? Expand : Fold" /></el-icon>
          </el-button>
        </div>

        <div class="console-header__right-fixed">
          <el-input
            v-model="globalKeyword"
            class="global-search"
            placeholder="全局检索档案标题、档号、标签或直接输入自然语言"
            clearable
            @keyup.enter="handleGlobalSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>

        <div class="console-header__right">
          <el-badge :value="todoBadge" :max="99" :hidden="!todoCount">
            <el-button text class="icon-button" @click="goTodoCenter">
              <el-icon><List /></el-icon>
            </el-button>
          </el-badge>

          <el-badge is-dot :hidden="!hasUnreadMessages">
            <el-popover placement="bottom" :width="360" trigger="click">
              <template #reference>
                <el-button text class="icon-button">
                  <el-icon><Bell /></el-icon>
                </el-button>
              </template>
              <div class="message-panel">
                <div class="message-panel__head">
                  <strong>消息通知</strong>
                  <el-button text size="small" @click="markMessagesRead">全部已读</el-button>
                </div>
                <el-empty v-if="!messages.length" description="暂无消息" :image-size="64" />
                <div v-else class="message-list">
                  <button
                    v-for="item in messages"
                    :key="item.id"
                    type="button"
                    class="message-item"
                    @click="openMessage(item.route)"
                  >
                    <div class="message-item__meta">
                      <el-tag size="small" :type="item.type">{{ item.category }}</el-tag>
                      <span>{{ item.time }}</span>
                    </div>
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.summary }}</p>
                  </button>
                </div>
              </div>
            </el-popover>
          </el-badge>

          <el-dropdown>
            <div class="user-entry">
              <el-avatar :size="36">档</el-avatar>
              <div class="user-copy">
                <strong>档案管理员</strong>
                <span>欢迎回来</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人设置</el-dropdown-item>
                <el-dropdown-item>消息中心</el-dropdown-item>
                <el-dropdown-item>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="console-main">
        <template v-if="usePageContextBar">
          <div class="page-context-bar">
            <el-breadcrumb separator="/" class="page-context-bar__crumb">
              <el-breadcrumb-item v-for="(crumb, i) in breadcrumbs" :key="`${i}-${crumb}`">{{ crumb }}</el-breadcrumb-item>
            </el-breadcrumb>
            <div class="page-context-bar__right">
              <div class="page-context-bar__meta">
                <el-tag effect="plain" type="success" size="small">统一业务入口</el-tag>
                <div class="ai-ready-entry">
                  <el-tag effect="plain" type="warning" size="small">AI 检索已就绪</el-tag>
                  <el-button v-if="showAiArchiveButton" type="info" plain size="small" @click="goAiSearch">AI+档案</el-button>
                </div>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="page-breadcrumb">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item v-for="(crumb, i) in breadcrumbs" :key="`${i}-${crumb}`">{{ crumb }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="page-head" :class="{ 'page-head--no-title': hideLayoutPageHead }">
            <div v-if="!hideLayoutPageHead">
              <h1>{{ pageTitle }}</h1>
              <p>{{ pageDescription }}</p>
            </div>
            <div class="page-head__meta">
              <el-tag effect="plain" type="success">统一业务入口</el-tag>
              <div class="ai-ready-entry">
                <el-tag effect="plain" type="warning">AI 检索已就绪</el-tag>
                <el-button v-if="showAiArchiveButton" type="info" plain size="small" @click="goAiSearch">AI+档案</el-button>
              </div>
            </div>
          </div>
        </template>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import {
  Bell,
  DataAnalysis,
  Expand,
  Files,
  Fold,
  FolderOpened,
  List,
  Search,
  Setting
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLayoutStore } from '../stores/useLayoutStore'

interface HeaderMessage {
  id: string
  title: string
  summary: string
  category: string
  time: string
  type: 'primary' | 'warning' | 'danger' | 'success' | 'info'
  route: string
}

const route = useRoute()
const router = useRouter()
const layout = useLayoutStore()

/** vue-router 的 query 可能是 string | string[] */
const firstQueryValue = (q: unknown) => {
  if (q == null) return ''
  return String(Array.isArray(q) ? q[0] : q)
}
const globalKeyword = ref('')
const hasUnreadMessages = ref(true)

const messages = ref<HeaderMessage[]>([
  {
    id: 'msg-1',
    title: '2 项借阅申请待审批',
    summary: '其中 1 项将在今天 18:00 超时，请优先处理高密级借阅。',
    category: '流程催办',
    time: '刚刚',
    type: 'warning',
    route: '/archive-management/borrow'
  },
  {
    id: 'msg-2',
    title: '元数据缺失档案数上升',
    summary: '本周新增 12 份待补录档案，建议尽快复核归档字段。',
    category: '业务预警',
    time: '10 分钟前',
    type: 'danger',
    route: '/dashboard'
  },
  {
    id: 'msg-3',
    title: '档案查询能力已升级',
    summary: '现已支持自然语言检索和知识问答联动。',
    category: '系统通知',
    time: '今天',
    type: 'primary',
    route: '/archive-management/query'
  }
])

const todoCount = computed(() => 12)
const todoBadge = computed(() => (todoCount.value > 99 ? '99+' : todoCount.value))
const activeMenu = computed(() => {
  if (route.path.startsWith('/base-data/company-infos')) return '/base-data/company-infos'
  if (route.path.startsWith('/base-data/company-projects')) return '/base-data/company-projects'
  return route.path
})
const pageTitle = computed(() => (route.meta.title as string) ?? '档案智能工作台')
const hideLayoutPageHead = computed(() => route.meta.hidePageHead === true)
const usePageContextBar = computed(
  () =>
    route.name === 'archive-management-detail' ||
    route.name === 'archive-management-pending-archive-edit' ||
    route.name === 'archive-management-pending-archive-create'
)

const showAiArchiveButton = computed(
  () =>
    route.path === '/archive-management/query' ||
    route.name === 'archive-management-detail' ||
    route.name === 'archive-management-pending-archive-edit' ||
    route.name === 'archive-management-pending-archive-create'
)
const breadcrumbs = computed(() => {
  if (route.name === 'archive-management-detail') {
    if (firstQueryValue(route.query.from) === 'pending') {
      return ['应归档数据管理', '应归档数据详情']
    }
    return ['文档查询', '文档详情']
  }
  return (route.meta.breadcrumb as string[]) ?? [pageTitle.value]
})


const pageDescription = computed(() => {
  if (route.meta.description) return route.meta.description as string

  if (route.path.startsWith('/archive-management/pending-archive')) {
    return '对应路线图 F02：应归档数据的集成、录入、批量维护、查询与编辑；详细规格以 .docs 下 F02 功能文档为准。'
  }

  switch (route.path) {
    case '/dashboard':
      return '集中承接待办处理、核心业务发起、经营看板与智能检索，是全系统统一工作入口。'
    case '/base-data/warehouse':
      return '维护库房、区域、货架和库位的可视化基础数据。'
    case '/base-data/dictionaries':
      return '维护档案业务所需的标准字典与配置项。'
    case '/archive-management/create':
      return '支持自动与手工两种归档模式，提升归档发起效率。'
    case '/archive-management/query':
      return '支持结构化筛选、全文检索与自然语言问答。'
    case '/archive-management/ai-search':
      return '集中展示 AI 回答、相关文档、命中依据和右侧预览，承接自然语言搜索结果。'
    case '/archive-management/borrow':
      return '统一管理档案借阅申请、审批、借出与归还过程。'
    case '/archive-management/borrow-renew':
      return '统一处理续借申请发起、审批流转和续借状态跟踪。'
    case '/base-data/department-signatories':
      return '维护部门层级权签人，用于借阅与流程审批自动带出处理人。'
    case '/security/user-role-config':
      return '配置用户系统角色及其管辖的公司、业务模块等数据范围。'
    case '/governance':
      return '集中查看流程、规则、AI 能力与安全策略配置。'
    case '/security/four-properties/config':
      return '配置档案四性检测规则、适用范围与执行策略。'
    case '/security/four-properties/execution':
      return '执行四性检测任务并跟踪运行过程与结果。'
    case '/security/four-properties/report':
      return '查看四性检测报告，定位问题并跟踪整改。'
    default:
      return '围绕统一档案对象模型建设可扩展、可治理、可落地的现代化档案系统。'
  }
})

const goDashboard = () => {
  router.push('/dashboard')
}

const handleGlobalSearch = () => {
  const keyword = globalKeyword.value.trim()
  if (!keyword) {
    ElMessage.warning('请输入检索内容')
    return
  }
  router.push({ path: '/archive-management/ai-search', query: { q: keyword, mode: 'qa' } })
}

const goTodoCenter = () => {
  router.push({ path: '/dashboard', query: { focus: 'workspace' } })
}

const goAiSearch = () => {
  router.push('/archive-management/ai-search')
}

const markMessagesRead = () => {
  hasUnreadMessages.value = false
  ElMessage.success('消息已全部标记为已读')
}

const openMessage = (path: string) => {
  hasUnreadMessages.value = false
  router.push(path)
}
</script>

<style scoped>
.console-layout {
  height: 100vh;
  overflow: hidden;
}

.console-aside {
  background:
    radial-gradient(circle at top, rgba(76, 141, 196, 0.26), transparent 26%),
    linear-gradient(180deg, #10243a 0%, #0a1727 100%);
  transition: width 0.2s ease;
  position: sticky;
  top: 0;
  height: 100vh;
  flex-shrink: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 68px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
}

.brand-mark {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: linear-gradient(135deg, #65b7ff, #2b6cb8);
  color: #fff;
  display: grid;
  place-items: center;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-copy {
  display: grid;
  gap: 4px;
  overflow: hidden;
}

.brand-copy strong {
  color: #fff;
  font-size: 15px;
}

.brand-copy span {
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
}

.menu-scroll {
  height: calc(100vh - 68px);
}

.console-content {
  height: 100vh;
  overflow: hidden;
}

.menu {
  border-right: 0;
}

.console-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  height: 72px;
  padding: 0 20px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid #e8edf3;
}

.console-header__left,
.console-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.console-header__right-fixed {
  margin-left: auto;
  display: flex;
  align-items: center;
  width: 560px;
  max-width: 52vw;
}

.console-header__right-fixed .global-search {
  width: 100%;
}

.global-search :deep(.el-input__wrapper) {
  border-radius: 999px;
  box-shadow: 0 0 0 1px rgba(35, 87, 137, 0.08) inset;
  min-height: 42px;
}

.icon-button {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: #355c7d;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-left: 4px;
  cursor: pointer;
}

.user-copy {
  display: grid;
  gap: 2px;
}

.user-copy strong {
  font-size: 14px;
  color: #213547;
}

.user-copy span {
  font-size: 12px;
  color: #7a8797;
}

.message-panel {
  display: grid;
  gap: 12px;
}

.message-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.message-list {
  display: grid;
  gap: 10px;
}

.message-item {
  width: 100%;
  padding: 12px;
  border: 1px solid #e7edf5;
  border-radius: 14px;
  background: #f8fbff;
  text-align: left;
  cursor: pointer;
  transition: all 0.2s ease;
}

.message-item:hover {
  transform: translateY(-1px);
  border-color: #9bc3e8;
}

.message-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #7d8b99;
  font-size: 12px;
}

.message-item strong {
  display: block;
  margin-bottom: 6px;
  color: #203040;
}

.message-item p {
  margin: 0;
  color: #5d6b78;
  font-size: 13px;
  line-height: 1.5;
}

.console-main {
  padding: 20px;
  height: calc(100vh - 72px);
  overflow-y: auto;
}

.page-breadcrumb {
  margin-bottom: 12px;
}

.page-context-bar {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 16px;
  flex-wrap: nowrap;
  margin-bottom: 16px;
  min-height: 28px;
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
}

.page-context-bar__crumb {
  flex: 0 1 auto;
  min-width: 0;
  white-space: nowrap;
}

.page-context-bar__crumb :deep(.el-breadcrumb) {
  white-space: nowrap;
  flex-wrap: nowrap;
}

.page-context-bar__right {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  flex-shrink: 0;
  margin-left: auto;
  gap: 8px;
}

.page-context-bar__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  flex-shrink: 0;
}


.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-head--no-title {
  justify-content: flex-end;
  margin-bottom: 12px;
}

.page-head h1 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #203040;
}

.page-head p {
  margin: 0;
  max-width: 760px;
  color: #6a7b8d;
  font-size: 14px;
  line-height: 1.6;
}

.page-head__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}
.ai-ready-entry {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1280px) {
  .console-header {
    align-items: flex-start;
    height: auto;
    padding: 16px 20px;
    flex-wrap: wrap;
  }

  .console-header__right-fixed {
    order: 2;
    width: 100%;
    margin-left: 0;
  }

  .console-header__right {
    order: 3;
    margin-left: auto;
  }
}

@media (max-width: 960px) {
  .console-main {
    padding: 16px;
  }

  .page-head,
  .console-header__left,
  .console-header__right,
  .user-entry {
    flex-wrap: wrap;
  }

  .page-head {
    flex-direction: column;
  }

  .page-head__meta {
    flex-wrap: wrap;
  }

  .user-copy {
    display: none;
  }
}
</style>
