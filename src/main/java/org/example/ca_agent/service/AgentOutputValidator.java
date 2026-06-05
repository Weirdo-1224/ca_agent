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
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

@Service
public class AgentOutputValidator {

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
        validateEvidenceIds("Extractor", output, evidencePool);
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
        validateEvidenceIds("Writer", output, evidencePool);
    }

    public void validateReviewer(ReviewResultDTO output, String taskId) {
        requireTaskId("Reviewer", output == null ? null : output.getTaskId(), taskId);
        if (output.getPassed() == null || output.getScore() == null || output.getNextAction() == null) {
            reject("Reviewer", "passed, score and nextAction must not be null");
        }
        if (output.getNextAction().getAction() == null
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
        Set<String> allowedEvidenceIds = new HashSet<>();
        for (Evidence evidence : emptyIfNull(evidencePool)) {
            if (evidence != null && evidence.getEvidenceId() != null) {
                allowedEvidenceIds.add(evidence.getEvidenceId());
            }
        }

        Set<String> referencedEvidenceIds = new HashSet<>();
        collectEvidenceIds(output, referencedEvidenceIds, new IdentityHashMap<>());
        if (!allowedEvidenceIds.containsAll(referencedEvidenceIds)) {
            Set<String> unknownIds = new HashSet<>(referencedEvidenceIds);
            unknownIds.removeAll(allowedEvidenceIds);
            reject(agent, "evidenceIds must come from the input evidencePool: " + unknownIds);
        }
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
}
