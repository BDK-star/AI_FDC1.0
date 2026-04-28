import { createRouter, createWebHistory } from 'vue-router'
import ConsoleLayout from '../layouts/ConsoleLayout.vue'
import CreateArchiveView from '../views/archive-management/CreateArchiveView.vue'
import AiSearchResultsView from '../views/archive-management/AiSearchResultsView.vue'
import ArchiveQueryView from '../views/archive-management/ArchiveQueryView.vue'
import ArchiveDetailView from '../views/archive-management/ArchiveDetailView.vue'
import BaseDataArchiveFlowRuleView from '../views/base-data/BaseDataArchiveFlowRuleView.vue'
import BaseDataCompanyProjectDetailView from '../views/base-data/BaseDataCompanyProjectDetailView.vue'
import BaseDataCompanyProjectDictionaryView from '../views/base-data/BaseDataCompanyProjectDictionaryView.vue'
import BaseDataCompanyProjectListView from '../views/base-data/BaseDataCompanyProjectListView.vue'
import BaseDataDocumentOrganizationView from '../views/base-data/BaseDataDocumentOrganizationView.vue'
import BaseDataWarehouseView from '../views/base-data/BaseDataWarehouseView.vue'
import BusinessModuleConfigView from '../views/base-data/BusinessModuleConfigView.vue'
import CompanyInfoView from '../views/base-data/CompanyInfoView.vue'
import DepartmentSignatoryView from '../views/base-data/DepartmentSignatoryView.vue'
import DictionaryManagementView from '../views/base-data/DictionaryManagementView.vue'
import DocumentTypeConfigView from '../views/base-data/DocumentTypeConfigView.vue'
import BorrowingView from '../views/borrowing/BorrowingView.vue'
import DashboardView from '../views/dashboard/DashboardView.vue'
import GovernanceView from '../views/governance/GovernanceView.vue'
import FourPropertiesConfigView from '../views/security/FourPropertiesConfigView.vue'
import FourPropertiesExecutionView from '../views/security/FourPropertiesExecutionView.vue'
import FourPropertiesReportView from '../views/security/FourPropertiesReportView.vue'
import WorkflowManagementView from '../views/workflow/WorkflowManagementView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: ConsoleLayout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: DashboardView,
          meta: {
            title: '首页 / 档案智能工作台',
            breadcrumb: ['首页']
          }
        },
        {
          path: 'workspace/import-query',
          name: 'workspace-import-query',
          component: () => import('../views/workspace/ImportQueryWorkspaceView.vue'),
          meta: {
            title: '我的导入',
            breadcrumb: ['我的工作空间', '我的导入']
          }
        },
        {
          path: 'workspace/export-query',
          name: 'workspace-export-query',
          component: () => import('../views/workspace/ExportQueryWorkspaceView.vue'),
          meta: {
            title: '我的导出',
            breadcrumb: ['我的工作空间', '我的导出']
          }
        },
        {
          path: 'workspace/my-drafts',
          name: 'workspace-my-drafts',
          component: () => import('../views/workspace/MyDraftsWorkspaceView.vue'),
          meta: {
            title: '我的草稿',
            breadcrumb: ['我的工作空间', '我的草稿']
          }
        },
        {
          path: 'base-data/warehouse',
          name: 'base-data-warehouse',
          component: BaseDataWarehouseView,
          meta: { title: '库房管理', breadcrumb: ['配置中心', '库房管理'] }
        },
        {
          path: 'base-data/document-type-configs',
          name: 'base-data-document-type-configs',
          component: DocumentTypeConfigView,
          meta: { title: '文档类型配置', breadcrumb: ['配置中心', '文档类型配置'] }
        },
        {
          path: 'base-data/business-modules',
          name: 'base-data-business-modules',
          component: BusinessModuleConfigView,
          meta: { title: '业务模块配置', breadcrumb: ['配置中心', '业务模块配置'] }
        },
        {
          path: 'base-data/company-infos',
          name: 'base-data-company-infos',
          component: CompanyInfoView,
          meta: { title: '公司管理', breadcrumb: ['配置中心', '公司管理'] }
        },
        {
          path: 'base-data/department-signatories',
          name: 'base-data-department-signatories',
          component: DepartmentSignatoryView,
          meta: { title: '权签人维护', breadcrumb: ['配置中心', '权签人维护'] }
        },
        {
          path: 'base-data/dictionaries',
          name: 'base-data-dictionaries',
          component: DictionaryManagementView,
          meta: { title: '字典管理', breadcrumb: ['配置中心', '字典管理'] }
        },
        {
          path: 'base-data/document-organizations',
          name: 'base-data-document-organizations',
          component: BaseDataDocumentOrganizationView,
          meta: { title: '文档组织管理', breadcrumb: ['配置中心', '文档组织管理'] }
        },
        {
          path: 'base-data/archive-flow-rules',
          name: 'base-data-archive-flow-rules',
          component: BaseDataArchiveFlowRuleView,
          meta: { title: '归档规则管理', breadcrumb: ['配置中心', '归档规则管理'] }
        },
        {
          path: 'base-data/company-projects',
          name: 'base-data-company-project-list',
          component: BaseDataCompanyProjectListView,
          meta: { title: '公司项目管理', breadcrumb: ['配置中心', '公司项目管理'] }
        },
        {
          path: 'base-data/company-projects/create',
          name: 'base-data-company-project-create',
          component: BaseDataCompanyProjectDetailView,
          meta: { title: '新建公司项目', breadcrumb: ['配置中心', '公司项目管理', '新建'] }
        },
        {
          path: 'base-data/company-projects/:companyProjectCode/view',
          name: 'base-data-company-project-view',
          component: BaseDataCompanyProjectDetailView,
          meta: { title: '查看公司项目', breadcrumb: ['配置中心', '公司项目管理', '查看'] }
        },
        {
          path: 'base-data/company-projects/:companyProjectCode/edit',
          name: 'base-data-company-project-edit',
          component: BaseDataCompanyProjectDetailView,
          meta: { title: '编辑公司项目', breadcrumb: ['配置中心', '公司项目管理', '编辑'] }
        },
        {
          path: 'base-data/company-project-dictionaries',
          name: 'base-data-company-project-dictionaries',
          component: BaseDataCompanyProjectDictionaryView,
          meta: { title: '公司字典管理', breadcrumb: ['配置中心', '公司字典管理'] }
        },
        {
          path: 'archive-management/create',
          name: 'archive-management-create',
          component: CreateArchiveView,
          meta: {
            title: '发起归档',
            breadcrumb: ['档案业务管理', '发起归档']
          }
        },
        {
          path: 'archive-management/pending-archive',
          redirect: '/archive-management/pending-archive/query'
        },
        {
          path: 'archive-management/pending-archive/query',
          name: 'archive-management-pending-archive-query',
          component: () => import('../views/archive-management/PendingArchiveQueryView.vue'),
          meta: {
            title: '应归档数据管理',
            breadcrumb: ['应归档数据管理'],
            description: '对未归档数据进行手工调整。',
            requiresDocumentType: true
          }
        },
        {
          path: 'archive-management/pending-archive/create',
          name: 'archive-management-pending-archive-create',
          component: () => import('../views/archive-management/PendingArchiveCreateView.vue'),
          meta: {
            title: '创建应归档数据',
            breadcrumb: ['应归档数据管理', '创建应归档数据'],
            description: '参考 .docs/features/F03/reference_html/pages/document_create.html',
            requiresDocumentType: true,
            hidePageHead: true
          }
        },
        {
          path: 'archive-management/pending-archive/edit/:docId',
          name: 'archive-management-pending-archive-edit',
          component: () => import('../views/archive-management/PendingArchiveEditView.vue'),
          meta: {
            title: '编辑文档',
            breadcrumb: ['应归档数据管理', '编辑应归档数据'],
            description: '参考 .docs/features/F03/reference_html/pages/document_edit.html',
            requiresDocumentType: true,
            hidePageHead: true
          }
        },
        {
          path: 'archive-management/ai-search',
          name: 'archive-management-ai-search',
          component: AiSearchResultsView,
          meta: {
            title: 'AI 搜索结果页',
            breadcrumb: ['档案业务管理', 'AI+档案']
          }
        },
        {
          path: 'archive-management/query',
          name: 'archive-management-query',
          component: ArchiveQueryView,
          meta: {
            title: '文档查询',
            breadcrumb: ['文档查询'],
            requiresDocumentType: true
          }
        },
        {
          path: 'archive-management/detail/:id',
          name: 'archive-management-detail',
          component: ArchiveDetailView,
          meta: {
            title: '文档详情',
            hidePageHead: true
          }
        },
        {
          path: 'archive-management/transfer',
          name: 'archive-management-transfer',
          component: () => import('../views/receiving/TransferManagementView.vue'),
          meta: {
            title: '移交申请提交',
            breadcrumb: ['档案业务管理', '文档移交', '移交申请提交']
          }
        },
        {
          path: 'archive-management/transfer-query',
          name: 'archive-management-transfer-query',
          component: () => import('../views/receiving/TransferQueryView.vue'),
          meta: {
            title: '移交申请查询',
            breadcrumb: ['档案业务管理', '文档移交', '移交申请查询']
          }
        },
        {
          path: 'archive-management/transfer-field-config',
          name: 'archive-management-transfer-field-config',
          component: () => import('../views/receiving/TransferApplyFieldConfigView.vue'),
          meta: {
            title: '移交申请通用字段配置',
            breadcrumb: ['档案业务管理', '文档移交', '移交申请通用字段配置']
          }
        },
        {
          path: 'archive-management/transfer-detail/:id',
          name: 'archive-management-transfer-detail',
          component: () => import('../views/receiving/TransferDetailView.vue'),
          meta: {
            title: '移交申请详情',
            breadcrumb: ['档案业务管理', '文档移交', '移交申请查询', '移交申请详情']
          }
        },
        {
          path: 'archive-management/borrow',
          name: 'archive-management-borrow',
          component: BorrowingView,
          meta: {
            title: '借阅文档',
            breadcrumb: ['档案业务管理', '借阅文档']
          }
        },
        {
          path: 'archive-management/borrow-renew',
          name: 'archive-management-borrow-renew',
          component: BorrowingView,
          meta: {
            title: '续借申请',
            breadcrumb: ['档案业务管理', '借阅申请', '续借申请']
          }
        },
        {
          path: 'archive-management/bind',
          name: 'archive-management-bind',
          component: () => import('../views/archive-management/BindArchiveView.vue'),
          meta: {
            title: '成册整理',
            breadcrumb: ['档案业务管理', '成册整理'],
            description: '成册整理将衔接归档完成后的后续业务处理。'
          }
        },
        {
          path: 'archive-management/storage',
          name: 'archive-management-storage',
          component: () => import('../views/archive-management/StorageManagementView.vue'),
          meta: {
            title: '入库上架',
            breadcrumb: ['档案业务管理', '入库上架'],
            description: '入库上架页面将承接实体档案入库、上架和库位绑定。'
          }
        },
        {
          path: 'governance',
          name: 'governance',
          component: GovernanceView,
          meta: {
            title: '流程规则与 AI 治理',
            breadcrumb: ['平台治理', '流程规则与 AI 治理']
          }
        },
        {
          path: 'security/user-role-config',
          name: 'security-user-role-config',
          component: () => import('../views/security/UserRoleConfigView.vue'),
          meta: {
            title: '系统角色配置',
            breadcrumb: ['权限中心', '系统角色配置']
          }
        },
        {
          path: 'workflow',
          name: 'workflow',
          component: WorkflowManagementView,
          meta: {
            title: '工作流管理',
            breadcrumb: ['平台治理', '工作流管理']
          }
        },
        {
          path: 'security/four-properties/config',
          name: 'security-four-properties-config',
          component: FourPropertiesConfigView,
          meta: {
            title: '四性检测配置',
            breadcrumb: ['档案安全管理', '四性检测', '四性检测配置']
          }
        },
        {
          path: 'security/four-properties/execution',
          name: 'security-four-properties-execution',
          component: FourPropertiesExecutionView,
          meta: {
            title: '四性检测执行',
            breadcrumb: ['档案安全管理', '四性检测', '四性检测执行']
          }
        },
        {
          path: 'security/four-properties/report',
          name: 'security-four-properties-report',
          component: FourPropertiesReportView,
          meta: {
            title: '四性检测报告',
            breadcrumb: ['档案安全管理', '四性检测', '四性检测报告']
          }
        }
      ]
    }
  ]
})

export default router
