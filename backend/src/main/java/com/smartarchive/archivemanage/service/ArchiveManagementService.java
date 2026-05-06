package com.smartarchive.archivemanage.service;

import com.smartarchive.archivemanage.dto.AiModelConfigResponse;
import com.smartarchive.archivemanage.dto.ArchiveAskCommand;
import com.smartarchive.archivemanage.dto.ArchiveAskResponse;
import com.smartarchive.archivemanage.dto.ArchiveAttachmentResponse;
import com.smartarchive.archivemanage.dto.ArchiveAttachmentUpdateCommand;
import com.smartarchive.archivemanage.dto.ArchiveCreateCommand;
import com.smartarchive.archivemanage.dto.ArchiveCreateOptionsResponse;
import com.smartarchive.archivemanage.dto.ArchiveCreateSessionCommand;
import com.smartarchive.archivemanage.dto.ArchiveCreateSessionResponse;
import com.smartarchive.archivemanage.dto.ArchiveDefaultResolveResponse;
import com.smartarchive.archivemanage.dto.ArchiveQueryCommand;
import com.smartarchive.archivemanage.dto.ArchiveQueryResponse;
import com.smartarchive.archivemanage.dto.ArchiveSummaryResponse;
import com.smartarchive.archivemanage.dto.ArchiveTransferCommand;
import com.smartarchive.archivemanage.dto.ArchiveTransferResponse;
import com.smartarchive.archivemanage.dto.PendingAuditAttachmentRef;
import com.smartarchive.archivemanage.dto.PendingAuditDownload;
import com.smartarchive.archivemanage.dto.PendingDocumentBatchDeleteCommand;
import com.smartarchive.archivemanage.dto.PendingDocumentWriteCommand;
import com.smartarchive.archivemanage.dto.BindBatchResponse;
import com.smartarchive.archivemanage.dto.BindCreateCommand;
import com.smartarchive.archivemanage.dto.BindOptionsResponse;
import com.smartarchive.archivemanage.dto.BindPreviewCommand;
import com.smartarchive.archivemanage.dto.BindPreviewResponse;
import com.smartarchive.archivemanage.dto.BindQueryCommand;
import com.smartarchive.archivemanage.dto.StorageBatchResponse;
import com.smartarchive.archivemanage.dto.StorageCreateCommand;
import com.smartarchive.archivemanage.dto.StorageLedgerQueryCommand;
import com.smartarchive.archivemanage.dto.StorageLedgerResponse;
import com.smartarchive.archivemanage.dto.StorageOptionsResponse;
import com.smartarchive.archivemanage.dto.StorageQueryCommand;
import com.smartarchive.archivemanage.dto.StorageQueryResponse;
import com.smartarchive.workspace.dto.WorkspaceIoJobSummaryResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface ArchiveManagementService {
    ArchiveCreateOptionsResponse loadCreateOptions();
    ArchiveDefaultResolveResponse resolveDefaults(String companyProjectCode, String busiModuleCode, String customRule, String archiveDestination);

    /**
     * 与 {@link #resolveDefaults} 相同的规则候选集下，列出规则中配置过的归档地（去重、排序），供创建页归档地下拉。
     */
    List<String> listCandidateArchiveDestinationsForFlow(String companyProjectCode, String busiModuleCode);
    ArchiveCreateSessionResponse createSession(ArchiveCreateSessionCommand command);
    ArchiveCreateSessionResponse getSession(String sessionCode);
    ArchiveAttachmentResponse uploadAttachment(String sessionCode, String attachmentRole, String attachmentTypeCode, String remark, MultipartFile file);
    ArchiveAttachmentResponse updateAttachment(String sessionCode, Long attachmentId, ArchiveAttachmentUpdateCommand command);
    ArchiveSummaryResponse createArchive(ArchiveCreateCommand command);
    ArchiveQueryResponse queryArchives(ArchiveQueryCommand command);
    ArchiveAskResponse ask(ArchiveAskCommand command);
    List<AiModelConfigResponse> listAiModels();
    ArchiveSummaryResponse getArchiveDetail(Long archiveId);

    /**
     * 按公司编码 + 三级业务模块 + 开始档期（月初）+ 文档业务编码定位唯一一条 lifecycle=UNARCHIVED 的正式应归档行；0 条或多条均返回 empty。
     */
    Optional<Long> findUnarchivedFormalDocumentIdByNaturalKey(String companyCode, String bizModuleCode, LocalDate startPeriod, String docBizNo);

    void requireArchiveTypeUnderDocumentType(String archiveTypeCode, String documentTypeRootCode);

    ArchiveSummaryResponse createPendingDocument(PendingDocumentWriteCommand command);
    ArchiveSummaryResponse updatePendingDocument(Long docId, PendingDocumentWriteCommand command);
    void deletePendingDocuments(PendingDocumentBatchDeleteCommand command, long operatorUserId);
    ArchiveSummaryResponse duplicatePendingDocument(Long sourceDocId, long operatorUserId);
    PendingAuditAttachmentRef uploadPendingAuditAttachment(MultipartFile file);
    PendingAuditDownload downloadPendingAuditAttachment(Long fileId, String storageKey);
    PendingAuditDownload downloadArchiveAttachment(Long attachmentId);
    PendingAuditDownload previewArchiveAttachment(Long attachmentId);
    PendingAuditDownload downloadArchiveAttachmentsZip(Long archiveId);
    /**
     * 文档查询 / 应归档数据导出：生成 CSV 文本（不含 UTF-8 BOM）。供同步下载与异步任务复用。
     */
    String exportPendingDocumentsCsvContent(List<Long> docIds, String exportScope);

    WorkspaceIoJobSummaryResponse createPendingDocumentsExportJob(List<Long> docIds, String exportFileFormat, String exportScope, Long operatorUserId);
    WorkspaceIoJobSummaryResponse submitArchiveImportQueryJob(MultipartFile file, String documentTypeCode, Long operatorUserId);
    ArchiveTransferResponse transferArchives(ArchiveTransferCommand command);
    BindOptionsResponse loadBindOptions();
    BindPreviewResponse previewBind(BindPreviewCommand command);
    BindBatchResponse createBindBatch(BindCreateCommand command);
    BindBatchResponse getBindBatch(String bindBatchCode);
    List<BindBatchResponse> queryBindBatches(BindQueryCommand command);
    StorageOptionsResponse loadStorageOptions();
    StorageQueryResponse queryStorage(StorageQueryCommand command);
    StorageBatchResponse createStorageBatch(StorageCreateCommand command);
    StorageBatchResponse getStorageBatch(String storageBatchCode);
    List<StorageLedgerResponse> queryStorageLedger(StorageLedgerQueryCommand command);
    StorageLedgerResponse getStorageLedger(Long ledgerId);
}
