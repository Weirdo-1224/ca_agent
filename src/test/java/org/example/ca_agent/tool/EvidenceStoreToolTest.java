package org.example.ca_agent.tool;

import org.example.ca_agent.dto.agent.RawSourceSetDTO;
import org.example.ca_agent.enums.ReliabilityLevel;
import org.example.ca_agent.enums.SourceType;
import org.example.ca_agent.schema.Evidence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceStoreToolTest {

    private final EvidenceStoreTool tool = new EvidenceStoreTool();

    @BeforeEach
    void setUp() {
        tool.resetCounter();
    }

    @Test
    void createsRawSourceWithAllFields() {
        RawSourceSetDTO.RawSource rs = tool.createRawSource(
                "task-1", "Cursor", SourceType.OFFICIAL_SITE,
                "Cursor Home", "https://cursor.com", "Cursor is an AI editor.",
                ReliabilityLevel.HIGH, List.of("official"));

        assertThat(rs.getSourceId()).startsWith("src_cursor_");
        assertThat(rs.getProductName()).isEqualTo("Cursor");
        assertThat(rs.getSourceType()).isEqualTo(SourceType.OFFICIAL_SITE);
        assertThat(rs.getTitle()).isEqualTo("Cursor Home");
        assertThat(rs.getUrl()).isEqualTo("https://cursor.com");
        assertThat(rs.getRawText()).isEqualTo("Cursor is an AI editor.");
        assertThat(rs.getContentSnippet()).isEqualTo("Cursor is an AI editor.");
        assertThat(rs.getReliability()).isEqualTo(ReliabilityLevel.HIGH);
        assertThat(rs.getTargetDimensions()).containsExactly("official");
        assertThat(rs.getCollectedAt()).isNotNull();
    }

    @Test
    void createsEvidenceWithAllFields() {
        Evidence ev = tool.createEvidence(
                "task-1", "Cursor", SourceType.PRICING_PAGE,
                "Cursor Pricing", "https://cursor.com/pricing",
                "Pricing info", ReliabilityLevel.HIGH, List.of("pricing"));

        assertThat(ev.getEvidenceId()).startsWith("ev_cursor_");
        assertThat(ev.getProductName()).isEqualTo("Cursor");
        assertThat(ev.getSourceType()).isEqualTo(SourceType.PRICING_PAGE);
        assertThat(ev.getSourceTitle()).isEqualTo("Cursor Pricing");
        assertThat(ev.getUrl()).isEqualTo("https://cursor.com/pricing");
        assertThat(ev.getContentSnippet()).isEqualTo("Pricing info");
        assertThat(ev.getReliability()).isEqualTo(ReliabilityLevel.HIGH);
        assertThat(ev.getUsedFor()).containsExactly("pricing");
    }

    @Test
    void incrementsCounterForEachCreation() {
        RawSourceSetDTO.RawSource rs1 = tool.createRawSource(
                "t", "A", SourceType.OFFICIAL_SITE, "T1", "u1", "c1", ReliabilityLevel.HIGH, List.of());
        RawSourceSetDTO.RawSource rs2 = tool.createRawSource(
                "t", "A", SourceType.OFFICIAL_SITE, "T2", "u2", "c2", ReliabilityLevel.HIGH, List.of());

        assertThat(rs1.getSourceId()).endsWith("_1");
        assertThat(rs2.getSourceId()).endsWith("_2");
    }

    @Test
    void truncatesLongContentForSnippet() {
        String longText = "a".repeat(600);
        RawSourceSetDTO.RawSource rs = tool.createRawSource(
                "t", "A", SourceType.OFFICIAL_SITE, "T", "u", longText, ReliabilityLevel.HIGH, List.of());

        assertThat(rs.getContentSnippet()).endsWith("...");
        assertThat(rs.getContentSnippet().length()).isLessThanOrEqualTo(503);
    }
}
