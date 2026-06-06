package org.example.ca_agent.tool;

import org.example.ca_agent.dto.agent.RawSourceSetDTO;
import org.example.ca_agent.enums.ReliabilityLevel;
import org.example.ca_agent.enums.SourceType;
import org.example.ca_agent.schema.Evidence;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class EvidenceStoreTool {

    private final AtomicInteger counter = new AtomicInteger(0);

    public RawSourceSetDTO.RawSource createRawSource(
            String taskId,
            String productName,
            SourceType sourceType,
            String title,
            String url,
            String content,
            ReliabilityLevel reliability,
            List<String> targetDimensions
    ) {
        String sourceId = nextId("src", taskId, productName);
        RawSourceSetDTO.RawSource rawSource = new RawSourceSetDTO.RawSource();
        rawSource.setSourceId(sourceId);
        rawSource.setProductName(productName);
        rawSource.setSourceType(sourceType);
        rawSource.setTitle(title);
        rawSource.setUrl(url);
        rawSource.setRawText(content);
        rawSource.setContentSnippet(truncate(content, 300));
        rawSource.setCollectedAt(LocalDateTime.now());
        rawSource.setReliability(reliability);
        rawSource.setTargetDimensions(targetDimensions);
        return rawSource;
    }

    public Evidence createEvidence(
            String taskId,
            String productName,
            SourceType sourceType,
            String title,
            String url,
            String contentSnippet,
            ReliabilityLevel reliability,
            List<String> usedFor
    ) {
        String evidenceId = nextId("ev", taskId, productName);
        Evidence evidence = new Evidence();
        evidence.setEvidenceId(evidenceId);
        evidence.setProductName(productName);
        evidence.setSourceType(sourceType);
        evidence.setSourceTitle(title);
        evidence.setUrl(url);
        evidence.setContentSnippet(contentSnippet);
        evidence.setCollectedAt(LocalDateTime.now());
        evidence.setReliability(reliability);
        evidence.setUsedFor(usedFor);
        return evidence;
    }

    private String nextId(String prefix, String taskId, String productName) {
        String normalized = productName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        int index = counter.incrementAndGet();
        return prefix + "_" + normalized + "_" + index;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    public String truncate(String text) {
        return truncate(text, 300);
    }

    public void resetCounter() {
        counter.set(0);
    }
}
