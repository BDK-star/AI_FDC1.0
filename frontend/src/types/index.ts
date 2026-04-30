export interface DashboardMetric {
  code: string
  label: string
  value: string
  trend: string
}

export interface DashboardWorkspaceStats {
  pendingCount: number
  dueTodayCount: number
  overdueCount: number
  highPriorityCount: number
}

export interface DashboardTaskItem {
  title: string
  businessType: string
  currentStep: string
  initiator: string
  deadline: string
  priority: string
  status: string
  route: string
}

export interface DashboardTaskBucket {
  key: string
  label: string
  tasks: DashboardTaskItem[]
}

export interface DashboardNoticeItem {
  id: string
  category: string
  title: string
  summary: string
  time: string
  tagType: 'danger' | 'warning' | 'primary' | 'success' | 'info'
  route: string
}

export interface DashboardRecentItem {
  id: string
  title: string
  type: string
  time: string
  route: string
}

export interface DashboardTrendPoint {
  label: string
  value: number
}

export interface DashboardTrendGroup {
  range: string
  points: DashboardTrendPoint[]
}

export interface DashboardDistributionItem {
  label: string
  value: number
  color: string
}

export interface DashboardRiskIndicator {
  label: string
  value: number
}

export interface DashboardSummary {
  metrics: DashboardMetric[]
  workspaceStats: DashboardWorkspaceStats
  workspaceBuckets: DashboardTaskBucket[]
  notifications: DashboardNoticeItem[]
  recentActivities: DashboardRecentItem[]
  archiveTrends: DashboardTrendGroup[]
  borrowTrends: DashboardTrendGroup[]
  distributions: DashboardDistributionItem[]
  riskIndicators: DashboardRiskIndicator[]
}

export interface ArchiveObject {
  id: number
  archiveCode: string
  title: string
  archiveType: string
  securityLevel: string
  retentionPeriod: string
  organizationName: string
  currentWorkflowStage: string
  currentLocationCode: string
  responsiblePerson: string
  formedDate: string
  aiClassified: boolean
  aiMetadataExtracted: boolean
}

export interface ArchiveReceipt {
  id: number
  receiptCode: string
  sourceDept: string
  archiveTitle: string
  archiveType: string
  securityLevel: string
  receiveStatus: string
  workflowInstanceCode: string
  submittedBy: string
  submittedAt: string
}

export interface CatalogTask {
  id: number
  taskCode: string
  archiveCode: string
  archiveTitle: string
  taskStatus: string
  assignee: string
  dueDate: string
}

export interface BorrowRecord {
  id: number
  borrowCode: string
  archiveCode: string
  archiveTitle: string
  borrower: string
  borrowType: string
  approvalStatus: string
  borrowStatus: string
  expectedReturnDate: string
  borrowedAt: string
}

export interface BorrowOrderDetail {
  id: number
  borrowOrderId: number
  businessCode?: string
  documentName?: string
  company: string
  documentType?: string
  description?: string
  demandType?: string
  needReturn: boolean
  expectedReturnDate?: string
  lendingApprover?: string
  lendingRemark?: string
  handler?: string
  handlerRemark?: string
  createdAt?: string
}

export interface BorrowOrder {
  id: number
  orderNo: string
  userName: string
  userDepartment?: string
  applicantName: string
  applyTime: string
  purpose?: string
  reason?: string
  reasonAttachment?: string
  approvalComment?: string
  demandApprover?: string
  demandReviewer?: string
  demandAnalyst?: string
  ccUsers: string[]
  status: string
  workflowInstanceId?: string
  currentHandler?: string
  details: BorrowOrderDetail[]
}

export interface RenewableBorrowOrder {
  orderNo: string
  applyTime: string
  borrowTime?: string
  expireTime?: string
  currentHandler?: string
  applicantName?: string
}

export interface MyBorrowDocument {
  company: string
  businessCode: string
  documentName: string
  documentType: string
  businessModule: string
  archivePeriod: string
  orderNo: string
  status: string
  borrowTime?: string
  attachment?: string
}

export interface BorrowRenewDetail {
  id: number
  borrowRenewOrderId: number
  sourceDetailId?: number
  businessCode?: string
  documentName?: string
  company?: string
  borrowType?: string
  borrowTime?: string
  currentExpireTime?: string
  renewExpireTime?: string
  renewReason?: string
}

export interface BorrowRenewOrder {
  id: number
  renewOrderNo: string
  sourceOrderNo: string
  userName: string
  userDepartment?: string
  applicantName: string
  applyTime: string
  purpose?: string
  reason?: string
  reasonAttachment?: string
  reviewer?: string
  handler?: string
  ccUsers: string[]
  status: string
  workflowInstanceId?: string
  currentHandler?: string
  details: BorrowRenewDetail[]
}

export interface InventoryTask {
  id: number
  taskCode: string
  warehouseCode: string
  inventoryScope: string
  taskStatus: string
  abnormalCount: number
  owner: string
  dueDate: string
}

export interface DisposalRecord {
  id: number
  disposalCode: string
  archiveCode: string
  archiveTitle: string
  retentionPeriod: string
  appraisalConclusion: string
  approvalStatus: string
  disposalStatus: string
  createdAt: string
}

export interface Warehouse {
  id: number
  warehouseCode: string
  warehouseName: string
  warehouseType: string
  managerName: string
  contactPhone: string
  address: string
  areaSize?: number
  photoUrl?: string
  status: string
  description: string
  updatedAt?: string
}

export interface WarehouseSummary {
  warehouse: Warehouse
  areaCount: number
  rackCount: number
  locationCount: number
  freeLocationCount: number
  occupiedLocationCount: number
  lastUpdatedAt?: string
}

export interface WarehouseArea {
  id: number
  warehouseCode: string
  areaCode: string
  areaName: string
  sortOrder: number
  startX: number
  startY: number
  width: number
  height: number
  status: string
  updatedAt?: string
}

export interface WarehouseRack {
  id: number
  warehouseCode: string
  areaCode: string
  rackCode: string
  rackName: string
  layerCount: number
  slotCount: number
  startX: number
  startY: number
  status: string
  updatedAt?: string
}

export interface WarehouseLocation {
  id: number
  warehouseCode: string
  warehouseName: string
  areaCode: string
  shelfCode: string
  layerCode: string
  locationCode: string
  locationName: string
  status: string
  capacity: number
  occupiedCount: number
  x: number
  y: number
  width: number
  height: number
}

export interface WarehouseVisualNode {
  id: number
  warehouseCode: string
  locationCode: string
  locationName: string
  status: string
  capacity: number
  occupiedCount: number
  x: number
  y: number
  width: number
  height: number
  color: string
}

export interface WarehouseVisualization {
  warehouseCode: string
  warehouseName: string
  totalLocations: number
  occupiedLocations: number
  warningLocations: number
  nodes: WarehouseVisualNode[]
}

export interface WarehouseManagementResponse {
  warehouse: Warehouse
  areas: WarehouseArea[]
  racks: WarehouseRack[]
  locations: WarehouseLocation[]
}

export interface WorkflowDefinition {
  code: string
  name: string
  scenario: string
  status: string
}

export interface RuleDefinition {
  code: string
  name: string
  category: string
  expression: string
}

export interface AiCapability {
  code: string
  name: string
  status: string
  description: string
}

export interface SecurityPolicy {
  policyCode: string
  name: string
  category: string
  status: string
}
export interface DocumentTypeTreeNode {
  id: number
  typeCode: string
  typeName: string
  description?: string
  enabledFlag: string
  parentCode?: string
  levelNum: number
  ancestorPath?: string
  sortOrder: number
  deleteFlag: string
  createdBy: number
  creationDate: string
  lastUpdatedBy: number
  lastUpdateDate: string
  children: DocumentTypeTreeNode[]
}

export interface PermissionPoint {
  code: string
  name: string
  action: string
  description: string
}

export interface DataDimension {
  code: string
  name: string
  description: string
}

export interface DocumentTypePermissionPreview {
  moduleCode: string
  moduleName: string
  permissionPoints: PermissionPoint[]
  dataDimensions: DataDimension[]
}

export interface AuditRecord {
  id: number
  moduleCode: string
  moduleName: string
  businessType: string
  businessKey: string
  operationType: string
  operationTypeName?: string
  operationSummary: string
  beforeSnapshot?: string
  afterSnapshot?: string
  operatorId: number
  operatorName: string
  operationTime: string
  operationRemark?: string
  auditAttachments?: Array<{ fileId?: number; fileName?: string; storageKey?: string; fileSize?: number }>
}

export interface CompanyProjectLine {
  id: number
  lineNo: number
  orgCategory: string
  organizationCode: string
  organizationName: string
  validFlag: string
  deleteFlag: string
  createdBy: number
  creationDate: string
  lastUpdatedBy: number
  lastUpdateDate: string
}

export interface CompanyProjectSummary {
  id: number
  companyProjectCode: string
  companyProjectName: string
  countryCode: string
  managementArea: string
  enabledFlag: string
  organizationCount: number
  lastUpdatedBy: number
  lastUpdateDate: string
}

export interface CompanyProjectDetail {
  id: number
  companyProjectCode: string
  companyProjectName: string
  countryCode: string
  managementArea: string
  companyTag?: string
  enabledFlag: string
  deleteFlag: string
  createdBy: number
  creationDate: string
  lastUpdatedBy: number
  lastUpdateDate: string
  lines: CompanyProjectLine[]
}

export interface CompanyProjectOrgCategory {
  categoryCode: string
  categoryName: string
}

export interface CountryOption {
  countryCode: string
  countryName: string
}

export interface DocumentOrganizationCityOption {
  countryCode: string
  cityCode: string
  cityName: string
}

export interface DocumentOrganizationSummary {
  id: number
  documentOrganizationCode: string
  documentOrganizationName: string
  description: string
  countryCode: string
  cityCode?: string
  enabledFlag: string
  lastUpdatedBy: number
  lastUpdateDate: string
}

export interface DocumentOrganizationDetail {
  id: number
  documentOrganizationCode: string
  documentOrganizationName: string
  description: string
  countryCode: string
  cityCode?: string
  enabledFlag: string
  deleteFlag: string
  createdBy: number
  creationDate: string
  lastUpdatedBy: number
  lastUpdateDate: string
}

export interface CountryDictionaryItem {
  id: number
  countryCode: string
  countryName: string
  sortOrder: number
  enabledFlag: string
}

export interface OrgCategoryDictionaryItem {
  id: number
  categoryCode: string
  categoryName: string
  sortOrder: number
  enabledFlag: string
}

export interface CountryRegionItem {
  regionCode: string
  regionName: string
  regionLevel: 'COUNTRY' | 'PROVINCE' | 'CITY'
  parentRegionCode?: string
  sortOrder?: number
  enabledFlag?: 'Y' | 'N'
}

export interface DocumentTypeConfig {
  documentTypeId: number
  docTypeCode: string
  docTypeDescription: string
  enableFlag: 'Y' | 'N'
  tenantid?: number
  createdBy?: number
  creationDate?: string
  lastUpdatedBy?: number
  lastUpdateDate?: string
}

export interface CompanyInfo {
  companyId: number
  companyCode: string
  companyName: string
  region?: string
  representativeOffice?: string
  country?: string
  description?: string
  tags?: string[]
  enabledFlag: 'Y' | 'N'
  lastUpdateDate?: string
}

export interface CompanyTag {
  tagId: number
  tagValue: string
  enabledFlag: 'Y' | 'N'
}

export interface DepartmentSignatory {
  departmentSignatoryId: number
  firstLevelDepartment: string
  secondLevelDepartment?: string
  thirdLevelDepartment?: string
  fourthLevelDepartment?: string
  signatories: string[]
  lastUpdateDate?: string
}

export interface BusinessModuleNode {
  id: number
  moduleCode: string
  moduleName: string
  parentCode?: string
  levelNum: number
  ancestorPath?: string
  enabledFlag: 'Y' | 'N'
  securityLevel?: string
  integrationType?: string
  description?: string
  remark?: string
  sortOrder: number
  lastUpdatedBy?: number
  lastUpdateDate?: string
  children: BusinessModuleNode[]
}

export interface BusinessModuleExtField {
  fieldId: number
  fieldCode: string
  moduleCode: string
  fieldScope: 'BASIC' | 'ATTACHMENT'
  applicationFunctions?: ('应收' | '移交')[]
  extAttribute?: string
  fieldName: string
  englishFieldName?: string
  dataType: 'TEXT' | 'NUMBER' | 'DATE' | 'DATETIME' | 'DICT' | 'BOOLEAN'
  queryFlag: 'Y' | 'N'
  requiredFlag: 'Y' | 'N'
  enabledFlag: 'Y' | 'N'
  sortOrder: number
  lastUpdateDate?: string
}


export interface ArchiveFlowRuleOption {
  code: string
  name: string
}

export interface ArchiveFlowRuleSummary {
  id: number
  companyProjectCode: string
  companyProjectName: string
  busiModuleCode: string
  busiModuleName: string
  customRule?: string
  archiveDestination?: string
  archiveDestinationName?: string
  documentOrganizationCode: string
  documentOrganizationName: string
  retentionPeriodYears: number
  externalDisplayFlag: string
  defaultFlag: string
  enabledFlag: string
  lastUpdatedBy: number
  lastUpdateDate: string
}

export interface ArchiveFlowRuleDetail {
  id: number
  companyProjectCode: string
  busiModuleCode: string
  customRule?: string
  archiveDestination?: string
  documentOrganizationCode: string
  retentionPeriodYears: number
  externalDisplayFlag: string
  defaultFlag: string
  enabledFlag: string
  deleteFlag: string
  createdBy: number
  creationDate: string
  lastUpdatedBy: number
  lastUpdateDate: string
}

export interface LabelValueOption {
  code: string
  name: string
}

export interface DocumentTypeExtField {
  fieldId: number
  fieldCode: string
  documentTypeCode: string
  usageModule: string
  relatedModuleCode: string
  relatedField: string
  fieldName: string
  fieldType: 'TEXT' | 'DICT'
  dictCategoryCode?: string
  requiredFlag: 'Y' | 'N'
  enabledFlag: 'Y' | 'N'
  formSortOrder: number
  queryEnabledFlag: 'Y' | 'N'
  querySortOrder: number
  sourceLevel?: number
  sourceDocumentTypeCode?: string
  lastUpdateDate?: string
}

export interface ArchiveCreateOptions {
  companyProjects: LabelValueOption[]
  documentTypes: LabelValueOption[]
  archiveDestinations: LabelValueOption[]
  documentOrganizations: LabelValueOption[]
  securityLevels: LabelValueOption[]
  carrierTypes: LabelValueOption[]
  attachmentTypes: LabelValueOption[]
  archiveTypes: LabelValueOption[]
  aiModels: LabelValueOption[]
  geoCountries: LabelValueOption[]
  geoRepOffices: LabelValueOption[]
  geoRegions: LabelValueOption[]
  custodyStatuses: LabelValueOption[]
}

export interface ArchiveDefaultResolve {
  securityLevelCode?: string
  archiveDestination?: string
  documentOrganizationCode?: string
  retentionPeriodYears?: number
  countryCode?: string
}

export interface ArchiveAttachmentItem {
  attachmentId: number
  attachmentRole: 'ELECTRONIC' | 'PAPER_SCAN'
  attachmentTypeCode?: string
  attachmentSeq: number
  versionNo: number
  fileName: string
  mimeType?: string
  fileSize?: number
  remark?: string
  aiSummary?: string
  parseStatus: string
  vectorStatus: string
  creationDate?: string
}

export interface ArchiveAiParseResult {
  suggestedDocumentTypeCode?: string
  suggestedCarrierTypeCode?: string
  documentName?: string
  businessCode?: string
  companyProjectCode?: string
  companyProjectName?: string
  beginPeriod?: string
  endPeriod?: string
  documentDate?: string
  sourceSystem?: string
  aiSummary?: string
  extractedTextPreview?: string
  confidence?: number
  parseExplain?: string
  extendedValues?: Record<string, string>
}

export interface ArchiveCreateSession {
  sessionId: number
  sessionCode: string
  createMode: 'AUTO' | 'MANUAL'
  sessionStatus: string
  busiModuleCodeGuess?: string
  carrierTypeCodeGuess?: string
  parseStatus: string
  aiSummarySnapshot?: string
  expireTime?: string
  attachments: ArchiveAttachmentItem[]
  aiParseResult?: ArchiveAiParseResult
}

export interface ArchiveRecordSummary {
  archiveId: number
  archiveCode: string
  documentTypeCode: string
  documentTypeName?: string
  companyProjectCode: string
  /** 三级业务模块 type_code（fdc_document_t.biz_module_code） */
  businessModuleTypeCode?: string
  companyProjectName?: string
  beginPeriod?: string
  endPeriod?: string
  documentName: string
  businessCode?: string
  dutyPerson: string
  createdBy?: string
  dutyDepartment: string
  documentDate: string
  securityLevelCode: string
  /** 密级中文展示名（与 securityLevelCode 成对） */
  securityLevelName?: string
  sourceSystem?: string
  archiveDestination?: string
  originPlace?: string
  carrierTypeCode: string
  remark?: string
  bindVolumeCode?: string
  currentWarehouseCode?: string
  currentLocationCode?: string
  aiArchiveSummary?: string
  documentOrganizationCode: string
  retentionPeriodYears: number
  archiveTypeCode: string
  archiveStatus: string
  lifecycleStatus?: string
  custodyStatus?: string
  /** 是否可见（attr1），详情归档信息区使用 */
  documentVisibility?: string
  parseStatus: string
  vectorStatus: string
  lastUpdateDate?: string
  attachmentCount: number
  extValues: Record<string, string>
  attachments: ArchiveAttachmentItem[]
}

export interface ArchiveQueryResult {
  records: ArchiveRecordSummary[]
  queryFields: DocumentTypeExtField[]
  total: number
  page: number
  pageSize: number
}

export interface ArchiveAskResult {
  answer: string
  references: ArchiveRecordSummary[]
}

export interface ArchiveAiModelSummary {
  modelCode: string
  modelName: string
  modelType: 'CHAT' | 'EMBEDDING' | 'RERANK'
  modelIdentifier: string
  embeddingDimension: number
  timeoutSeconds: number
  topK: number
  scoreThreshold?: number
  enabledFlag: 'Y' | 'N'
  remark?: string
}

export interface WorkspaceIoJobSummary {
  jobId: number
  jobType: string
  dataType: string
  jobName: string
  documentTypeCode?: string
  queryConfigJson?: string
  inputFileName?: string
  inputTotal?: number
  resultTotal?: number
  durationMs?: number
  jobStatus: string
  errorMessage?: string
  failedFileCsv?: string
  exportFileFormat?: string
  artifactExpiresAt?: string
  /** 列表展示：含 EXPIRED、COMPLETED */
  displayStatus?: string
  exportDownloadable?: boolean
  /** 应归档批量导入等：可下载结果 Excel */
  resultArtifactDownloadable?: boolean
  creationDate?: string
}

export interface WorkspaceIoJobPage {
  records: WorkspaceIoJobSummary[]
  total: number
  pages: number
  page: number
  pageSize: number
}

export interface WorkspaceImportQueryResultRow {
  archiveId?: number
  businessCode?: string
  companyProjectName?: string
  archiveTypeCode?: string
  beginPeriod?: string
  endPeriod?: string
  archiveDestination?: string
  originPlace?: string
  documentOrganizationCode?: string
  archiveStatus?: string
  documentName?: string
  documentDate?: string
  dutyPerson?: string
  dutyDepartment?: string
  carrierTypeCode?: string
  documentVisibility?: string
  sourceSystem?: string
  securityLevelName?: string
  remark?: string
  creationDate?: string
  createdBy?: string
}

export interface DictionaryCategory {
  id: number
  categoryCode: string
  categoryName: string
  description?: string
  enabledFlag: 'Y' | 'N'
  itemCount: number
}

export interface DictionaryItem {
  id: number
  categoryCode: string
  itemCode: string
  itemName: string
  itemValue?: string
  sortOrder: number
  enabledFlag: 'Y' | 'N'
}

export interface AiModelConfig {
  modelConfigId?: number
  modelCode: string
  modelName: string
  modelType: 'CHAT' | 'EMBEDDING' | 'RERANK'
  apiUrl: string
  apiKey: string
  modelIdentifier: string
  promptTemplate?: string
  embeddingDimension: number
  timeoutSeconds: number
  topK: number
  scoreThreshold?: number
  officialResultCount: number
  officialScoreThreshold: number
  relatedResultCount: number
  relatedScoreThreshold: number
  enabledFlag: 'Y' | 'N'
  remark?: string
}

export interface AiModelConnectionTestResult {
  status: string
  message: string
}

export interface BindArchiveCandidate {
  archiveId: number
  archiveCode: string
  documentName: string
  documentTypeCode: string
  companyProjectCode: string
  businessCode?: string
  beginPeriod?: string
  endPeriod?: string
  archiveStatus: string
  carrierTypeCode: string
  bindVolumeCode?: string
}

export interface BindVolumeItem {
  itemId?: number
  archiveId: number
  archiveCode: string
  documentName: string
  sortNo: number
  primaryFlag: 'Y' | 'N'
  bindReason?: string
}

export interface BindVolume {
  volumeId?: number
  bindVolumeCode?: string
  volumeTitle: string
  bindRuleKey?: string
  carrierTypeCode: string
  archiveCount: number
  totalPageCount: number
  totalCopyCount: number
  bindStatus: string
  remark?: string
  items: BindVolumeItem[]
}

export interface BindBatch {
  bindBatchId: number
  bindBatchCode: string
  bindMode: string
  bindStatus: string
  bindRemark?: string
  guidedStorageFlag: 'Y' | 'N'
  volumeCount: number
  archiveCount: number
  nextAction: string
  creationDate: string
  volumes: BindVolume[]
}

export interface BindOptions {
  bindModes: LabelValueOption[]
  candidates: BindArchiveCandidate[]
}

export interface BindPreviewResult {
  bindMode: string
  groupCount: number
  archiveCount: number
  groups: BindVolume[]
}

export interface StorageBatchItem {
  storageItemId: number
  itemType: 'VOLUME' | 'ARCHIVE'
  volumeId?: number
  archiveId?: number
  bindVolumeCode?: string
  archiveCode?: string
  locationCode: string
  resultStatus: string
  errorMessage?: string
  storedAt?: string
}

export interface StorageBatch {
  storageBatchId: number
  storageBatchCode: string
  sourceType: string
  sourceBindBatchCode?: string
  warehouseCode: string
  operatorName: string
  storageStatus: string
  remark?: string
  createdAt: string
  items: StorageBatchItem[]
}

export interface StorageLedger {
  ledgerId: number
  ledgerCode: string
  storageBatchCode: string
  itemType: 'VOLUME' | 'ARCHIVE'
  bindBatchCode?: string
  bindVolumeCode?: string
  archiveCode?: string
  warehouseCode: string
  locationCode: string
  actionType: string
  resultStatus: string
  operatorName: string
  operationTime: string
  operationSummary: string
}

export interface StorageOptions {
  sourceTypes: LabelValueOption[]
  warehouses: LabelValueOption[]
  locations: LabelValueOption[]
}

export interface StorageQueryResult {
  volumes: BindVolume[]
  archives: BindArchiveCandidate[]
}

export interface TransferApplicationExtValue {
  fieldCode: string
  value?: string
}

export interface TransferApplicationDetailAttachment {
  attachmentId: number
  applicationId: number
  applicationDetailId: number
  fileName: string
  mimeType?: string
  fileSize?: number
  remark?: string
  creationDate?: string
}

export interface TransferApplicationDetailCreate {
  companyProjectCode: string
  docBusiNo: string
  docName: string
  busiModuleCode: string
  archPlaceAlpha2Code: string
  documentOrganizationCode?: string
  carrierType: string
  endArchPeriod: string
  startArchPeriod: string
  busiVolumeNo?: string
  archTypeCode: string
  docGenerationDate?: string
  archCopies: number
  remark?: string
  description?: string
  extValues?: TransferApplicationExtValue[]
  attachments?: TransferApplicationDetailAttachment[]
}

export interface TransferApplicationCreateCommand {
  applicationId?: number
  applicationNumber: string
  applicant: number
  applicationDate: string
  department: string
  documentTypeCode: string
  applyMethod: string
  expressType?: string
  expressNumber?: string
  documentRecipient: number
  handoverForm?: string
  applicationStatus?: string
  applicationDescription?: string
  tenantid: number
  details: TransferApplicationDetailCreate[]
}

export interface FourAttrInspectionDetail {
  detailId?: number
  inspectionType: string
  inspectionCode?: string
  inspectionItem?: string
  inspectionPurpose?: string
  inspectionObject?: string
  inspectionBasisMethod?: string
  displayOrder?: number
  enableFlag: 'Y' | 'N'
}

export interface FourAttrInspectionConfig {
  inspectionId: number
  inspectionName: string
  inspectionStage: string
  dataPackageSpec: string
  metadataSpec: string
  enableFlag: 'Y' | 'N'
  createdBy?: number
  creationDate?: string
  lastUpdatedBy?: number
  lastUpdateDate?: string
  details: FourAttrInspectionDetail[]
}
