package com.smartarchive.archivemanage.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.smartarchive.archiveflow.dto.ArchiveRuleMatchResponse;
import com.smartarchive.archiveflow.service.ArchiveFlowRuleService;
import com.smartarchive.archivemanage.dto.ArchiveDefaultResolveResponse;
import com.smartarchive.archivemanage.dto.ArchiveSummaryResponse;
import com.smartarchive.archivemanage.dto.PendingDocumentWriteCommand;
import com.smartarchive.archivemanage.service.ArchiveManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 需本地 PostgreSQL（application-local.yml）且已 Flyway 迁移含 V53 演示规则。
 */
@SpringBootTest
@ActiveProfiles("local")
class ArchiveManagementResolveDefaultsIntegrationTest {

    @Autowired
    private ArchiveManagementService archiveManagementService;

    @Autowired
    private ArchiveFlowRuleService archiveFlowRuleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void resolveDefaults_matchesFinAccVchApShanghaiByRegionCode() {
        ArchiveDefaultResolveResponse r = archiveManagementService.resolveDefaults(
            "CP-DEMO-001", "FIN_ACC_VCH_AP", null, "310000");
        assertTrue(
            "DO-DEMO-SH".equalsIgnoreCase(r.getDocumentOrganizationCode()),
            "documentOrganizationCode was: " + r.getDocumentOrganizationCode());
        assertEquals("FIN_ACC_VCH_AP", r.getResolvedCustMappingCode());
    }

    @Test
    void resolveDefaults_matchesFinAccVchArViaWildcardRule() {
        ArchiveDefaultResolveResponse r = archiveManagementService.resolveDefaults(
            "CP-DEMO-001", "FIN_ACC_VCH_AR", null, "310000");
        assertEquals("DO-DEMO-SH", r.getDocumentOrganizationCode());
    }

    @Test
    void createPendingDocument_persistsDocumentOrganizationFromFlowRule() {
        String biz = "IT-DOCORG-" + System.currentTimeMillis();
        PendingDocumentWriteCommand cmd = new PendingDocumentWriteCommand();
        cmd.setSubmitMode("SUBMIT");
        cmd.setOperationTypeCode("BATCH_CREATE");
        cmd.setOperatorUserId(1L);
        cmd.setDocumentTypeCode("FIN_ACC");
        cmd.setCompanyProjectCode("CP-DEMO-001");
        cmd.setArchiveTypeCode("FIN_ACC_VCH_AP");
        cmd.setBusinessCode(biz);
        cmd.setBeginPeriod("2026-05");
        cmd.setDocumentDate("2026-04-30 12:00:00");
        cmd.setArchiveDestination("310000");
        cmd.setOriginPlace("CN");
        cmd.setDocumentName("集成测试-文档组织自流向");
        cmd.setDutyPerson("admin");
        cmd.setCarrierTypeCode("ELECTRONIC");
        cmd.setSourceSystem("PORTAL");
        cmd.setSecurityLevelCode("INTERNAL");

        ArchiveSummaryResponse created = archiveManagementService.createPendingDocument(cmd);
        assertEquals("DO-DEMO-SH", created.getDocumentOrganizationCode());

        String stored = jdbcTemplate.queryForObject(
            """
            select doc_organization_code from fdc_document_t
             where doc_id = ? and coalesce(delete_flag, 0) = 0
            """,
            String.class,
            created.getArchiveId());
        assertEquals("DO-DEMO-SH", stored);
    }

    @Test
    void createPendingDocument_ignoresPlaceholderDefaultDocumentOrganization() {
        String biz = "IT-DOCORG-DEF-" + System.currentTimeMillis();
        PendingDocumentWriteCommand cmd = new PendingDocumentWriteCommand();
        cmd.setSubmitMode("SUBMIT");
        cmd.setOperationTypeCode("BATCH_CREATE");
        cmd.setOperatorUserId(1L);
        cmd.setDocumentTypeCode("FIN_ACC");
        cmd.setCompanyProjectCode("CP-DEMO-001");
        cmd.setArchiveTypeCode("FIN_ACC_VCH_AP");
        cmd.setBusinessCode(biz);
        cmd.setBeginPeriod("2026-05");
        cmd.setDocumentDate("2026-04-30 12:00:00");
        cmd.setArchiveDestination("310000");
        cmd.setOriginPlace("CN");
        cmd.setDocumentName("集成测试-DEFAULT占位应被忽略");
        cmd.setDutyPerson("admin");
        cmd.setCarrierTypeCode("ELECTRONIC");
        cmd.setSourceSystem("PORTAL");
        cmd.setSecurityLevelCode("INTERNAL");
        cmd.setDocumentOrganizationCode("DEFAULT");

        ArchiveSummaryResponse created = archiveManagementService.createPendingDocument(cmd);
        assertEquals("DO-DEMO-SH", created.getDocumentOrganizationCode());
    }

    @Test
    void matchArchiveRule_alignsWithResolveDefaults_finAccVchAp() {
        ArchiveRuleMatchResponse m = archiveFlowRuleService.matchArchiveRule(
            "CP-DEMO-001", "FIN_ACC_VCH_AP", null, "310000");
        assertTrue(m.isMatched());
        assertEquals("DO-DEMO-SH", m.getDocumentOrganizationCode());
        assertEquals("FIN_ACC_VCH_AP", m.getCustomRule());
    }
}
