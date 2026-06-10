package org.example.ca_agent.service;

import org.example.ca_agent.common.BizException;
import org.example.ca_agent.dto.agent.CompetitiveAnalysisDTO;
import org.example.ca_agent.dto.agent.ProductProfileSetDTO;
import org.example.ca_agent.dto.agent.ReportDraftDTO;
import org.example.ca_agent.dto.agent.ReviewResultDTO;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.dto.agent.TaskPlanDTO;
import org.example.ca_agent.schema.Claim;
import org.example.ca_agent.schema.Evidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AgentOutputValidator {

    private static final Logger log = LoggerFactory.getLogger(AgentOutputValidator.class);
    private static final Set<String> REVIEW_ACTIONS = Set.of("finish", "repair", "human_review");

    public void validatePlanner(TaskPlanDTO output, TaskInputDTO input) {
        requireTaskId("Planner", output == null ? null : output.getTaskId(), input.getTaskId());
        List<String> expectedProducts = emptyIfNull(input.getTargetProducts());
        Set<String> coveredProducts = new HashSet<>();
        for (TaskPlanDTO.CollectionTask task : emptyIfNull(output.getCollectionTasks())) {
            if (task != null && task.getProductName() != null) {
                coveredProducts.add(task.getProductName());
            }
        }
        if (!coveredProducts.containsAll(expectedProducts)) {
            reject("Planner", "collectionTasks must cover every target product");
        }
    }

    public void validateExtractor(ProductProfileSetDTO output, String taskId, List<Evidence> evidencePool) {
        requireTaskId("Extractor", output == null ? null : output.getTaskId(), taskId);
        if (output.getProducts() == null || output.getProducts().isEmpty()) {
            reject("Extractor", "products must not be empty");
        }
        for (ProductProfileSetDTO.ProductProfile product : output.getProducts()) {
            for (Claim claim : emptyIfNull(product == null ? null : product.getClaims())) {
                if (claim == null || claim.getEvidenceIds() == null || claim.getEvidenceIds().isEmpty()) {
                    reject("Extractor", "every Claim evidenceIds must not be empty");
                }
            }
        }
        // LLM 可能编造不存在的 evidenceId，先过滤再校验（软修正，不阻断流程）
        sanitizeEvidenceIds("Extractor", output, evidencePool);
        validateEvidenceIds("Extractor", output, evidencePool);
        validateSemanticRelevance("Extractor", output, evidencePool);
    }

    public void validateAnalyzer(
            CompetitiveAnalysisDTO output,
            String taskId,
            List<String> productNames,
            List<Evidence> evidencePool
    ) {
        requireTaskId("Analyzer", output == null ? null : output.getTaskId(), taskId);
        if (output.getComparisonMatrix() == null || output.getComparisonMatrix().isEmpty()) {
            reject("Analyzer", "comparisonMatrix must not be empty");
        }
        Set<String> expectedProducts = new HashSet<>(emptyIfNull(productNames));
        for (CompetitiveAnalysisDTO.ComparisonMatrixItem matrix : output.getComparisonMatrix()) {
            Set<String> coveredProducts = new HashSet<>();
            for (CompetitiveAnalysisDTO.ComparisonProductItem item :
                    emptyIfNull(matrix == null ? null : matrix.getItems())) {
                if (item != null && item.getProductName() != null) {
                    coveredProducts.add(item.getProductName());
                }
            }
            if (!coveredProducts.containsAll(expectedProducts)) {
                reject("Analyzer", "comparisonMatrix must cover every product");
            }
        }
        sanitizeEvidenceIds("Analyzer", output, evidencePool);
        validateEvidenceIds("Analyzer", output, evidencePool);
    }

    public void validateWriter(ReportDraftDTO output, String taskId, List<Evidence> evidencePool) {
        requireTaskId("Writer", output == null ? null : output.getTaskId(), taskId);
        List<ReportDraftDTO.ReportSection> sections = output.getSections();
        if (sections == null || sections.size() != 14) {
            reject("Writer", "report must contain exactly 14 standard sections");
        }
        Set<String> titles = new HashSet<>();
        for (ReportDraftDTO.ReportSection section : sections) {
            if (section == null || section.getTitle() == null || section.getTitle().isBlank()) {
                reject("Writer", "14 standard sections must have titles");
            }
            titles.add(section.getTitle().trim());
        }
        if (titles.size() != 14) {
            reject("Writer", "14 standard sections must have distinct titles");
        }
        sanitizeEvidenceIds("Writer", output, evidencePool);
        validateEvidenceIds("Writer", output, evidencePool);
        validateReportCitationCoverage(output);
    }

    public void validateReviewer(ReviewResultDTO output, String taskId) {
        requireTaskId("Reviewer", output == null ? null : output.getTaskId(), taskId);
        if (output.getPassed() == null) {
            reject("Reviewer", "passed must not be null");
        }
        // score 为 null 时给默认值，不阻断流程
        if (output.getScore() == null) {
            log.warn("[Reviewer] score is null, defaulting to 0");
            output.setScore(0);
        }
        // nextAction 为 null 时构建默认值
        if (output.getNextAction() == null) {
            log.warn("[Reviewer] nextAction is null, defaulting to finish");
            ReviewResultDTO.NextAction defaultAction = new ReviewResultDTO.NextAction();
            defaultAction.setAction(output.getPassed() ? "finish" : "repair");
            output.setNextAction(defaultAction);
        } else if (output.getNextAction().getAction() == null
                || !REVIEW_ACTIONS.contains(output.getNextAction().getAction())) {
            reject("Reviewer", "nextAction action must be finish, repair, or human_review");
        }
        for (ReviewResultDTO.ReviewIssue issue : emptyIfNull(output.getIssues())) {
            if (issue == null || issue.getTargetAgent() == null) {
                reject("Reviewer", "issue targetAgent must be a valid AgentType");
            }
        }
    }

    private void requireTaskId(String agent, String actualTaskId, String expectedTaskId) {
        if (actualTaskId == null || !actualTaskId.equals(expectedTaskId)) {
            reject(agent, "taskId must match the real taskId");
        }
    }

    private void validateEvidenceIds(String agent, Object output, List<Evidence> evidencePool) {
        Set<String> allowedEvidenceIds = buildAllowedEvidenceIds(evidencePool);

        Set<String> referencedEvidenceIds = new HashSet<>();
        collectEvidenceIds(output, referencedEvidenceIds, new IdentityHashMap<>());
        if (!allowedEvidenceIds.containsAll(referencedEvidenceIds)) {
            Set<String> unknownIds = new HashSet<>(referencedEvidenceIds);
            unknownIds.removeAll(allowedEvidenceIds);
            reject(agent, "evidenceIds must come from the input evidencePool: " + unknownIds);
        }
    }

    /**
     * 软修正：通过反射过滤掉 LLM 幻觉产生的不存在 evidenceId。
     * 仅记录告警日志，不抛异常。用于 Extractor 等 LLM 输出不可完全信任的场景。
     */
    @SuppressWarnings("unchecked")
    private void sanitizeEvidenceIds(String agent, Object output, List<Evidence> evidencePool) {
        Set<String> allowed = buildAllowedEvidenceIds(evidencePool);
        sanitizeObject(output, allowed, agent, new IdentityHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private void sanitizeObject(Object value, Set<String> allowed, String agent,
                                IdentityHashMap<Object, Boolean> visited) {
        if (value == null || isScalar(value.getClass()) || visited.put(value, Boolean.TRUE) != null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> sanitizeObject(item, allowed, agent, visited));
            return;
        }
        if (!value.getClass().getPackageName().startsWith("org.example.ca_agent")) {
            return;
        }
        for (Field field : value.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(value);
                if ("evidenceIds".equals(field.getName()) && fieldValue instanceof java.util.List<?> ids) {
                    java.util.List<String> original = (java.util.List<String>) ids;
                    java.util.List<String> filtered = original.stream()
                            .filter(id -> id == null || allowed.contains(id))
                            .toList();
                    if (filtered.size() < original.size()) {
                        Set<String> removed = new HashSet<>(original);
                        removed.removeAll(new HashSet<>(filtered));
                        log.warn("[{}] Sanitized {} hallucinated evidenceIds: {}",
                                agent, removed.size(), removed);
                        // 如果过滤后为空，保留第一个原始 ID 作为回退（避免空列表触发校验）
                        if (filtered.isEmpty() && !original.isEmpty()) {
                            String fallback = original.get(0);
                            log.warn("[{}] All evidenceIds were hallucinated, keeping first as fallback: {}",
                                    agent, fallback);
                            filtered = java.util.List.of(fallback);
                        }
                        field.set(value, new java.util.ArrayList<>(filtered));
                    }
                } else {
                    sanitizeObject(fieldValue, allowed, agent, visited);
                }
            } catch (IllegalAccessException e) {
                // 忽略无法访问的字段
            }
        }
    }

    private Set<String> buildAllowedEvidenceIds(List<Evidence> evidencePool) {
        Set<String> allowed = new HashSet<>();
        for (Evidence evidence : emptyIfNull(evidencePool)) {
            if (evidence != null && evidence.getEvidenceId() != null) {
                allowed.add(evidence.getEvidenceId());
            }
        }
        return allowed;
    }

    private void collectEvidenceIds(Object value, Set<String> target, IdentityHashMap<Object, Boolean> visited) {
        if (value == null || isScalar(value.getClass()) || visited.put(value, Boolean.TRUE) != null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collectEvidenceIds(item, target, visited));
            return;
        }
        if (!value.getClass().getPackageName().startsWith("org.example.ca_agent")) {
            return;
        }

        for (Field field : value.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(value);
                if ("evidenceIds".equals(field.getName()) && fieldValue instanceof Collection<?> ids) {
                    ids.stream().filter(String.class::isInstance).map(String.class::cast).forEach(target::add);
                } else {
                    collectEvidenceIds(fieldValue, target, visited);
                }
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot inspect agent output", exception);
            }
        }
    }

    private boolean isScalar(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || CharSequence.class.isAssignableFrom(type)
                || Number.class.isAssignableFrom(type)
                || Boolean.class == type;
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void reject(String agent, String rule) {
        throw new BizException(422, agent + " output validation failed: " + rule);
    }

    // ========== P1-3: 幻觉控制增强 ==========

    /**
     * 语义关联校验：检查 claim.statement 与引用的 evidence.contentSnippet 是否有关键词交集。
     * 交集为空时记录告警日志（不阻断流程）。
     */
    private void validateSemanticRelevance(String agent, Object output, List<Evidence> evidencePool) {
        if (!(output instanceof ProductProfileSetDTO profileSet)) {
            return;
        }
        Map<String, Evidence> evidenceMap = new HashMap<>();
        for (Evidence e : emptyIfNull(evidencePool)) {
            if (e != null && e.getEvidenceId() != null) {
                evidenceMap.put(e.getEvidenceId(), e);
            }
        }

        for (ProductProfileSetDTO.ProductProfile product : emptyIfNull(profileSet.getProducts())) {
            if (product == null) continue;
            for (Claim claim : emptyIfNull(product.getClaims())) {
                if (claim == null || claim.getStatement() == null || claim.getEvidenceIds() == null) {
                    continue;
                }
                Set<String> claimKeywords = extractKeywords(claim.getStatement());
                boolean hasOverlap = false;
                for (String evidenceId : claim.getEvidenceIds()) {
                    Evidence evidence = evidenceMap.get(evidenceId);
                    if (evidence != null && evidence.getContentSnippet() != null) {
                        Set<String> evidenceKeywords = extractKeywords(evidence.getContentSnippet());
                        evidenceKeywords.retainAll(claimKeywords);
                        if (!evidenceKeywords.isEmpty()) {
                            hasOverlap = true;
                            break;
                        }
                    }
                }
                if (!hasOverlap) {
                    log.warn("[{}] Hallucination risk: claim '{}' (product={}) has no keyword overlap with referenced evidence {}",
                            agent, truncateLog(claim.getStatement(), 80),
                            product.getProductName(), claim.getEvidenceIds());
                }
            }
        }
    }

    /**
     * 报告引用覆盖率检查：每个 section 应有 evidenceIds，空引用记录告警。
     */
    private void validateReportCitationCoverage(ReportDraftDTO report) {
        if (report == null || report.getSections() == null) {
            return;
        }
        for (ReportDraftDTO.ReportSection section : report.getSections()) {
            if (section == null) continue;
            if (section.getEvidenceIds() == null || section.getEvidenceIds().isEmpty()) {
                log.warn("[Writer] Report section '{}' has no evidence citations - potential unsupported content",
                        section.getTitle());
            }
        }
    }

    /**
     * 简单关键词提取：提取长度>=2的词片段（跨语言兼容）。
     */
    private Set<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> keywords = new HashSet<>();
        // Split by non-word characters (handles both CJK and Latin)
        String[] tokens = text.toLowerCase().split("[\\s\\p{Punct}]+");
        for (String token : tokens) {
            if (token.length() >= 2) {
                keywords.add(token);
            }
        }
        // Also extract 2-char CJK bigrams for Chinese text
        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            char next = text.charAt(i + 1);
            if (Character.isIdeographic(c) && Character.isIdeographic(next)) {
                keywords.add(String.valueOf(c) + next);
            }
        }
        return keywords;
    }

    private static String truncateLog(String text, int maxLen) {
        if (text == null) return "null";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
