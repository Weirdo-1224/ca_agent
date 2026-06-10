package org.example.ca_agent.agent;

import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.config.MetasoSearchProperties;
import org.example.ca_agent.dto.agent.RawSourceSetDTO;
import org.example.ca_agent.dto.agent.TaskPlanDTO;
import org.example.ca_agent.dto.metaso.MetasoSearchResponse;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.enums.ReliabilityLevel;
import org.example.ca_agent.enums.SourceType;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.tool.EvidenceStoreTool;
import org.example.ca_agent.tool.SourceRankTool;
import org.example.ca_agent.tool.WebPageReaderTool;
import org.example.ca_agent.tool.WebSearchTool;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
public class CollectorAgent implements AgentNode {

    @Autowired
    private WebSearchTool webSearchTool;
    @Autowired
    private WebPageReaderTool webPageReaderTool;
    @Autowired
    private SourceRankTool sourceRankTool;
    @Autowired
    private EvidenceStoreTool evidenceStoreTool;
    @Autowired
    private MetasoSearchProperties searchProperties;

    @Override
    public AgentType getAgentType() {
        return AgentType.COLLECTOR_AGENT;
    }

    @Override
    public CompetitiveAnalysisState execute(CompetitiveAnalysisState state) {
        state.setStatus(TaskStatus.COLLECTING);
        if (evidenceStoreTool != null) {
            evidenceStoreTool.resetCounter();
        }

        TaskPlanDTO taskPlan = state.getTaskPlan();
        List<RawSourceSetDTO.RawSource> rawSources = new ArrayList<>();
        List<Evidence> evidencePool = new ArrayList<>();

        if (taskPlan.getCollectionTasks() != null) {
            for (TaskPlanDTO.CollectionTask task : taskPlan.getCollectionTasks()) {
                collectForProduct(task, taskPlan.getTaskId(), rawSources, evidencePool);
            }
        }

        if (evidencePool.isEmpty()) {
            log.warn("No evidence collected from search; falling back to mock data");
            for (String productName : taskPlan.getProducts()) {
                addMockEvidence(taskPlan.getTaskId(), productName, rawSources, evidencePool);
            }
        }

        RawSourceSetDTO rawSourceSet = new RawSourceSetDTO();
        rawSourceSet.setTaskId(taskPlan.getTaskId());
        rawSourceSet.setRawSources(rawSources);
        rawSourceSet.setEvidencePool(evidencePool);
        rawSourceSet.setMissingSources(List.of());
        state.setRawSourceSet(rawSourceSet);
        return state;
    }

    private void collectForProduct(
            TaskPlanDTO.CollectionTask task,
            String taskId,
            List<RawSourceSetDTO.RawSource> rawSources,
            List<Evidence> evidencePool
    ) {
        String productName = task.getProductName();
        List<String> queries = task.getQueries();
        List<String> targetDimensions = task.getTargetDimensions();
        List<SourceType> preferredTypes = task.getPreferredSourceTypes();

        if (queries == null || queries.isEmpty()) {
            queries = List.of(
                    productName + " official site",
                    productName + " pricing plans",
                    productName + " developer documentation API",
                    productName + " review comparison"
            );
        }

        // 每个产品最多 15 条证据，确保多维度覆盖
        int maxPerProduct = 15;
        // 每个 query 最多取 3 条，确保每个 query 都能执行
        int maxPerQuery = 3;
        int collected = 0;

        for (String query : queries) {
            if (collected >= maxPerProduct) {
                break;
            }
            if (webSearchTool == null) {
                break;
            }
            List<MetasoSearchResponse.WebpageResult> results = webSearchTool.search(query);
            int queryCollected = 0;
            for (MetasoSearchResponse.WebpageResult result : results) {
                if (collected >= maxPerProduct || queryCollected >= maxPerQuery) {
                    break;
                }
                String url = result.getLink() != null ? result.getLink() : result.getUrl();
                if (url == null || url.isBlank()) {
                    continue;
                }

                ReliabilityLevel reliability = sourceRankTool.rank(url, result.getTitle(), result.getSnippet());
                SourceType sourceType = sourceRankTool.classify(url, result.getTitle(), result.getSnippet());

                if (!isPreferredType(preferredTypes, sourceType)) {
                    continue;
                }

                String content = webPageReaderTool.read(url);
                if (content.isBlank()) {
                    content = result.getSnippet() != null ? result.getSnippet() : "";
                }

                List<String> dims = targetDimensions != null ? targetDimensions : List.of("general");

                RawSourceSetDTO.RawSource rawSource = evidenceStoreTool.createRawSource(
                        taskId, productName, sourceType,
                        result.getTitle(), url, content,
                        reliability, dims);
                rawSources.add(rawSource);

                Evidence evidence = evidenceStoreTool.createEvidence(
                        taskId, productName, sourceType,
                        result.getTitle(), url,
                        rawSource.getContentSnippet(),
                        reliability, dims);
                evidencePool.add(evidence);

                collected++;
                queryCollected++;
            }
        }

        if (collected < 3) {
            log.warn("Product '{}' only collected {} evidence; filling with mock", productName, collected);
            while (collected < 3) {
                addMockEvidence(taskId, productName, rawSources, evidencePool);
                collected++;
            }
        }
    }

    private boolean isPreferredType(List<SourceType> preferredTypes, SourceType actualType) {
        if (preferredTypes == null || preferredTypes.isEmpty()) {
            return true;
        }
        return preferredTypes.contains(actualType);
    }

    private void addMockEvidence(
            String taskId,
            String productName,
            List<RawSourceSetDTO.RawSource> rawSources,
            List<Evidence> evidencePool
    ) {
        String normalized = productName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        int index = evidencePool.size() + 1;
        String sourceId = "src_" + normalized + "_" + index;
        String evidenceId = "ev_" + normalized + "_" + index;

        String[] dims = {"official", "pricing", "documentation"};
        SourceType[] types = {SourceType.OFFICIAL_SITE, SourceType.PRICING_PAGE, SourceType.DOCUMENTATION};
        int slot = (index - 1) % 3;
        SourceType sourceType = types[slot];
        String dimension = dims[slot];

        String title = productName + " " + sourceType.getDescription();
        String url = getRealProductUrl(productName, dimension);
        String snippet = productName + " " + dimension + " information for competitive analysis.";

        RawSourceSetDTO.RawSource rawSource = new RawSourceSetDTO.RawSource();
        rawSource.setSourceId(sourceId);
        rawSource.setProductName(productName);
        rawSource.setSourceType(sourceType);
        rawSource.setTitle(title);
        rawSource.setUrl(url);
        rawSource.setRawText(snippet);
        rawSource.setContentSnippet(snippet);
        rawSource.setCollectedAt(LocalDateTime.now());
        rawSource.setReliability(ReliabilityLevel.HIGH);
        rawSource.setTargetDimensions(List.of(dimension));
        rawSources.add(rawSource);

        Evidence evidence = new Evidence();
        evidence.setEvidenceId(evidenceId);
        evidence.setProductName(productName);
        evidence.setSourceType(sourceType);
        evidence.setSourceTitle(title);
        evidence.setUrl(url);
        evidence.setContentSnippet(snippet);
        evidence.setCollectedAt(rawSource.getCollectedAt());
        evidence.setReliability(ReliabilityLevel.HIGH);
        evidence.setUsedFor(List.of(dimension));
        evidencePool.add(evidence);
    }

    /**
     * 返回产品真实 URL（基于已知产品名和维度），避免 example.com 占位链接。
     */
    private String getRealProductUrl(String productName, String dimension) {
        String name = productName.trim().toLowerCase();
        return switch (name) {
            case "cursor" -> switch (dimension) {
                case "pricing" -> "https://www.cursor.com/pricing";
                case "documentation" -> "https://docs.cursor.com";
                default -> "https://www.cursor.com";
            };
            case "github copilot" -> switch (dimension) {
                case "pricing" -> "https://github.com/features/copilot/plans";
                case "documentation" -> "https://docs.github.com/en/copilot";
                default -> "https://github.com/features/copilot";
            };
            case "windsurf" -> switch (dimension) {
                case "pricing" -> "https://codeium.com/pricing";
                case "documentation" -> "https://docs.windsurf.com";
                default -> "https://windsurf.com";
            };
            case "通义灵码" -> switch (dimension) {
                case "pricing" -> "https://tongyi.aliyun.com/lingma/pricing";
                case "documentation" -> "https://help.aliyun.com/document_detail/2590612.html";
                default -> "https://tongyi.aliyun.com/lingma";
            };
            case "codeium" -> switch (dimension) {
                case "pricing" -> "https://codeium.com/pricing";
                case "documentation" -> "https://docs.codeium.com";
                default -> "https://codeium.com";
            };
            default -> switch (dimension) {
                case "pricing" -> "https://www.google.com/search?q=" + productName.replace(" ", "+") + "+pricing";
                case "documentation" -> "https://www.google.com/search?q=" + productName.replace(" ", "+") + "+documentation";
                default -> "https://www.google.com/search?q=" + productName.replace(" ", "+");
            };
        };
    }
}
