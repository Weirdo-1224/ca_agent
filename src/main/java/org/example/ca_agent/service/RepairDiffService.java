package org.example.ca_agent.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.ca_agent.common.JsonUtils;
import org.example.ca_agent.dto.agent.RepairDiffDTO;
import org.example.ca_agent.dto.agent.ReportDraftDTO;
import org.example.ca_agent.entity.RepairDiffEntity;
import org.example.ca_agent.repository.RepairDiffRepository;
import org.example.ca_agent.schema.Claim;
import org.example.ca_agent.schema.Evidence;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 计算修复前后差异（Repair Diff）。
 * <p>
 * 比较维度：
 * 1. evidenceId 集合差异 → 新增/删除证据
 * 2. claimId 集合差异 → 新增/删除结论
 * 3. report section 内容 hash 差异 → 修改章节
 * 4. Reviewer score 差异 → 分数提升/下降
 * 5. issue 数量差异 → 问题减少/增加
 * <p>
 * 注意：使用 ID 集合 + 内容 hash 做简单比较，避免全 JSON 字符串对比导致误判。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RepairDiffService {

    private final RepairDiffRepository repairDiffRepository;

    /**
     * 修复前的状态快照，仅保留比较必需的轻量数据。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Snapshot {
        private Set<String> evidenceIds;
        private Set<String> claimIds;
        /** sectionId → contentHashCode，用于检测内容是否变化 */
        private Map<String, Integer> sectionContentHash;
        private Integer score;
        private int issueCount;
    }

    /**
     * 从当前 state 中提取快照。
     * 如果某部分数据尚未生成（如 report），则记录空集合/0。
     */
    public Snapshot captureSnapshot(CompetitiveAnalysisState state) {
        // Evidence IDs
        Set<String> evidenceIds = new HashSet<>();
        if (state.getRawSourceSet() != null && state.getRawSourceSet().getEvidencePool() != null) {
            for (Evidence e : state.getRawSourceSet().getEvidencePool()) {
                if (e.getEvidenceId() != null) {
                    evidenceIds.add(e.getEvidenceId());
                }
            }
        }

        // Claim IDs
        Set<String> claimIds = new HashSet<>();
        if (state.getProductProfileSet() != null && state.getProductProfileSet().getProducts() != null) {
            for (var product : state.getProductProfileSet().getProducts()) {
                if (product.getClaims() != null) {
                    for (Claim c : product.getClaims()) {
                        if (c.getClaimId() != null) {
                            claimIds.add(c.getClaimId());
                        }
                    }
                }
            }
        }

        // Section content hash: sectionId → hashCode of content
        Map<String, Integer> sectionHash = new LinkedHashMap<>();
        if (state.getReportDraft() != null && state.getReportDraft().getSections() != null) {
            for (ReportDraftDTO.ReportSection s : state.getReportDraft().getSections()) {
                // 用 sectionId 做 key，content 的 hashCode 做值（轻量比较）
                int hash = s.getContent() != null ? s.getContent().hashCode() : 0;
                sectionHash.put(s.getSectionId(), hash);
            }
        }

        // Review score + issue count
        Integer score = 0;
        int issueCount = 0;
        if (state.getReviewResult() != null) {
            score = state.getReviewResult().getScore() != null ? state.getReviewResult().getScore() : 0;
            issueCount = state.getReviewResult().getIssues() != null ? state.getReviewResult().getIssues().size() : 0;
        }

        return new Snapshot(evidenceIds, claimIds, sectionHash, score, issueCount);
    }

    /**
     * 比较 before / after 快照，生成 RepairDiffDTO。
     *
     * @param before      修复前快照
     * @param after       修复后快照
     * @param taskId      任务 ID
     * @param iteration   当前修复轮次
     * @param targetAgent 本轮修复的目标 Agent
     * @return RepairDiffDTO
     */
    public RepairDiffDTO computeDiff(Snapshot before, Snapshot after,
                                     String taskId, int iteration, String targetAgent) {
        RepairDiffDTO diff = new RepairDiffDTO();
        diff.setTaskId(taskId);
        diff.setIteration(iteration);
        diff.setTargetAgent(targetAgent);

        // Score & issue 变化
        diff.setBeforeScore(before.getScore());
        diff.setAfterScore(after.getScore());
        diff.setBeforeIssueCount(before.getIssueCount());
        diff.setAfterIssueCount(after.getIssueCount());
        // 已修复 issue 数量 = 修复前 issue 数 - 修复后 issue 数（至少 0）
        diff.setFixedIssueCount(Math.max(0, before.getIssueCount() - after.getIssueCount()));

        // 新增 evidence（after 有但 before 没有）
        List<String> addedEvidenceIds = new ArrayList<>(after.getEvidenceIds());
        addedEvidenceIds.removeAll(before.getEvidenceIds());
        diff.setAddedEvidenceIds(addedEvidenceIds);

        // 新增 claim
        List<String> addedClaimIds = new ArrayList<>(after.getClaimIds());
        addedClaimIds.removeAll(before.getClaimIds());
        diff.setAddedClaimIds(addedClaimIds);

        // 修改章节：sectionId 在 before 中存在但 hash 不同，或 after 中新增
        List<String> changedSectionTitles = computeChangedSections(before, after, diff);
        diff.setChangedSectionTitles(changedSectionTitles);

        // 涉及的产品：从新增证据所属产品推断（此处暂用 repairInstruction 中的 targetProduct）
        // 简化处理：由 CompetitiveAnalysisGraph 后续填充
        diff.setChangedProducts(List.of());

        // 生成自然语言总结
        diff.setSummary(buildSummary(diff));
        diff.setCreatedAt(LocalDateTime.now());

        return diff;
    }

    /**
     * 比较 section content hash 差异。
     * 由于 Snapshot 只存 hash 不存 title，需要额外传入 after state 来获取 title 映射。
     * 这里简化：返回 sectionId 列表，由调用方映射为 title。
     */
    private List<String> computeChangedSections(Snapshot before, Snapshot after, RepairDiffDTO diff) {
        List<String> changedSectionIds = new ArrayList<>();
        // 检查 hash 变化的 section
        for (Map.Entry<String, Integer> entry : after.getSectionContentHash().entrySet()) {
            String sectionId = entry.getKey();
            Integer afterHash = entry.getValue();
            Integer beforeHash = before.getSectionContentHash().get(sectionId);
            if (beforeHash == null || !beforeHash.equals(afterHash)) {
                changedSectionIds.add(sectionId);
            }
        }
        // 暂存 sectionId 列表，由 Graph 层在拿到 state 后解析为 title
        return changedSectionIds;
    }

    /**
     * 根据 diff 数据生成中文自然语言总结。
     */
    private String buildSummary(RepairDiffDTO diff) {
        StringBuilder sb = new StringBuilder();
        sb.append("第 ").append(diff.getIteration()).append(" 轮修复");

        // 证据变化
        int addedEvCount = diff.getAddedEvidenceIds() != null ? diff.getAddedEvidenceIds().size() : 0;
        if (addedEvCount > 0) {
            sb.append("，新增 ").append(addedEvCount).append(" 条证据");
        }

        // Claim 变化
        int addedClaimCount = diff.getAddedClaimIds() != null ? diff.getAddedClaimIds().size() : 0;
        if (addedClaimCount > 0) {
            sb.append("，新增 ").append(addedClaimCount).append(" 条结论");
        }

        // 章节变化
        int changedSectionCount = diff.getChangedSectionTitles() != null ? diff.getChangedSectionTitles().size() : 0;
        if (changedSectionCount > 0) {
            sb.append("，修改 ").append(changedSectionCount).append(" 个章节");
        }

        // 分数变化
        if (diff.getBeforeScore() != null && diff.getAfterScore() != null) {
            int delta = diff.getAfterScore() - diff.getBeforeScore();
            if (delta != 0) {
                sb.append("，Reviewer 分数由 ").append(diff.getBeforeScore())
                        .append(delta > 0 ? " 提升到 " : " 降至 ")
                        .append(diff.getAfterScore());
            } else {
                sb.append("，Reviewer 分数保持 ").append(diff.getAfterScore());
            }
        }

        sb.append("。");
        return sb.toString();
    }

    /**
     * 辅助方法：将 sectionId 列表解析为 section title 列表。
     * 从 after state 的 reportDraft 中查找 title。
     */
    public List<String> resolveSectionTitles(List<String> sectionIds, CompetitiveAnalysisState state) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return List.of();
        }
        if (state.getReportDraft() == null || state.getReportDraft().getSections() == null) {
            return sectionIds;
        }
        Map<String, String> idToTitle = state.getReportDraft().getSections().stream()
                .collect(Collectors.toMap(
                        ReportDraftDTO.ReportSection::getSectionId,
                        s -> s.getTitle() != null ? s.getTitle() : s.getSectionId(),
                        (a, b) -> a
                ));
        return sectionIds.stream()
                .map(id -> idToTitle.getOrDefault(id, id))
                .toList();
    }

    /**
     * 从数据库加载某个任务的所有修复 Diff 记录，按 iteration 升序排列。
     * 如果没有任何修复记录，返回空列表。
     */
    public List<RepairDiffDTO> getRepairDiffs(String taskId) {
        List<RepairDiffEntity> entities = repairDiffRepository.selectList(
                new LambdaQueryWrapper<RepairDiffEntity>()
                        .eq(RepairDiffEntity::getTaskId, taskId)
                        .orderByAsc(RepairDiffEntity::getIteration)
        );
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream().map(this::toDTO).toList();
    }

    private RepairDiffDTO toDTO(RepairDiffEntity entity) {
        RepairDiffDTO dto = new RepairDiffDTO();
        dto.setTaskId(entity.getTaskId());
        dto.setIteration(entity.getIteration());
        dto.setTargetAgent(entity.getTargetAgent());
        dto.setBeforeScore(entity.getBeforeScore());
        dto.setAfterScore(entity.getAfterScore());
        dto.setBeforeIssueCount(entity.getBeforeIssueCount());
        dto.setAfterIssueCount(entity.getAfterIssueCount());
        dto.setFixedIssueCount(entity.getFixedIssueCount());
        dto.setAddedEvidenceIds(parseJsonList(entity.getAddedEvidenceIdsJson()));
        dto.setAddedClaimIds(parseJsonList(entity.getAddedClaimIdsJson()));
        dto.setChangedSectionTitles(parseJsonList(entity.getChangedSectionsJson()));
        dto.setChangedProducts(parseJsonList(entity.getChangedProductsJson()));
        dto.setSummary(entity.getSummary());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return JsonUtils.fromJsonList(json, String.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
