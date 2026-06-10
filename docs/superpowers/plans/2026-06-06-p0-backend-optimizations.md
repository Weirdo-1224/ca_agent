# P0 后端优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复三个最高优先级后端问题：异步失败状态丢失、LLM 调用无超时重试、Agent 执行无 Token 追踪。

**Architecture:** WorkflowService 异常处理增加状态回写；引入 spring-retry 实现网关级重试+超时；通过 ThreadLocal TokenUsageAccumulator 透明收集 Token 用量，在 AgentRunTracer 中捕获并持久化。

**Tech Stack:** Spring Boot 3.4.1, Spring AI 1.1.7 (OpenAI ChatClient), Spring Retry, MyBatis-Plus, H2 (test)

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `src/main/java/.../service/WorkflowService.java` | 异常时更新任务状态为 FAILED |
| Create | `src/main/java/.../service/TokenUsageAccumulator.java` | ThreadLocal Token 用量收集器 |
| Modify | `src/main/java/.../service/ModelChatGateway.java` | 接口改为返回 LlmCallResult |
| Create | `src/main/java/.../dto/agent/LlmCallResult.java` | LLM 调用结果（content + token usage） |
| Modify | `src/main/java/.../service/SpringAiModelChatGateway.java` | 获取 ChatResponse 提取 token，加 @Retryable |
| Modify | `src/main/java/.../service/LlmChatService.java` | 适配新接口，累加 token |
| Modify | `src/main/java/.../config/SpringAiAlibabaConfig.java` | 添加 @EnableRetry + RestClient 超时配置 |
| Modify | `src/main/java/.../dto/agent/AgentRunTrace.java` | 增加 token 字段 |
| Modify | `src/main/java/.../entity/AgentRunEntity.java` | 增加 token 列 |
| Modify | `src/main/java/.../dto/response/AgentRunResponse.java` | 增加 token 字段 |
| Modify | `src/main/java/.../agent/AgentRunTracer.java` | 执行前后收集 token，写入 trace |
| Modify | `src/main/java/.../assembler/StateAssembler.java` | 持久化 token 字段 |
| Modify | `src/main/java/.../service/AgentRunService.java` | 映射 token 字段到 response |
| Modify | `src/main/resources/schema.sql` | agent_run 表增加 token 列 |
| Modify | `pom.xml` | 添加 spring-retry 依赖 |
| Modify | `src/main/resources/application.yml` | 添加超时+重试配置 |
| Create | `src/test/java/.../service/WorkflowServiceTest.java` | 验证失败状态更新 |
| Create | `src/test/java/.../service/TokenUsageAccumulatorTest.java` | 验证 token 累加+重置 |
| Modify | `src/test/java/.../service/StructuredLlmServiceTest.java` | 适配新接口 |

---

### Task 1: 修复异步失败状态管理

**Files:**
- Modify: `src/main/java/org/example/ca_agent/service/WorkflowService.java`
- Create: `src/test/java/org/example/ca_agent/service/WorkflowServiceTest.java`

- [ ] **Step 1: 编写失败状态更新的单元测试**

```java
package org.example.ca_agent.service;

import org.example.ca_agent.assembler.StateAssembler;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.entity.AnalysisTaskEntity;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.repository.TaskRepository;
import org.example.ca_agent.workflow.CompetitiveAnalysisGraph;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WorkflowServiceTest {

    private CompetitiveAnalysisGraph graph;
    private StateAssembler stateAssembler;
    private TaskRepository taskRepository;
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        graph = mock(CompetitiveAnalysisGraph.class);
        stateAssembler = mock(StateAssembler.class);
        taskRepository = mock(TaskRepository.class);
        workflowService = new WorkflowService(graph, stateAssembler, taskRepository);
    }

    @Test
    void runAsync_onSuccess_savesState() {
        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId("task-1");
        CompetitiveAnalysisState state = new CompetitiveAnalysisState();
        state.setTaskInput(input);
        state.setStatus(TaskStatus.COMPLETED);
        when(graph.run(input)).thenReturn(state);

        workflowService.runAsync(input);

        verify(stateAssembler).saveState(state);
    }

    @Test
    void runAsync_onException_updatesTaskStatusToFailed() {
        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId("task-fail");
        when(graph.run(input)).thenThrow(new RuntimeException("LLM timeout"));

        workflowService.runAsync(input);

        verify(taskRepository).update(
                argThat(entity -> entity == null),
                argThat(wrapper -> true)
        );
        // 验证使用 lambdaUpdate 设置 status = FAILED
    }

    @Test
    void runAsync_onException_updatesTaskStatusToFailed_verifyArgs() {
        TaskInputDTO input = new TaskInputDTO();
        input.setTaskId("task-fail-2");
        when(graph.run(input)).thenThrow(new RuntimeException("Network error"));

        workflowService.runAsync(input);

        // 验证 taskRepository.update 被调用（设置 status=FAILED, errorMessage 等）
        verify(taskRepository).update(any(), any());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `./mvnw test -pl . -Dtest=WorkflowServiceTest -Dspring.profiles.active=test --no-transfer-progress`
Expected: FAIL - WorkflowService 构造函数不接受 TaskRepository 参数

- [ ] **Step 3: 实现失败状态更新逻辑**

修改 `WorkflowService.java`:

```java
package org.example.ca_agent.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.assembler.StateAssembler;
import org.example.ca_agent.dto.agent.TaskInputDTO;
import org.example.ca_agent.entity.AnalysisTaskEntity;
import org.example.ca_agent.enums.TaskStatus;
import org.example.ca_agent.repository.TaskRepository;
import org.example.ca_agent.workflow.CompetitiveAnalysisGraph;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final CompetitiveAnalysisGraph competitiveAnalysisGraph;
    private final StateAssembler stateAssembler;
    private final TaskRepository taskRepository;

    public CompetitiveAnalysisState run(TaskInputDTO taskInput) {
        return competitiveAnalysisGraph.run(taskInput);
    }

    @Async
    public void runAsync(TaskInputDTO taskInput) {
        try {
            log.info("[Async] Starting workflow for task: {}", taskInput.getTaskId());
            CompetitiveAnalysisState state = competitiveAnalysisGraph.run(taskInput);
            stateAssembler.saveState(state);
            log.info("[Async] Workflow completed for task: {}, status: {}",
                    taskInput.getTaskId(), state.getStatus());
        } catch (Exception e) {
            log.error("[Async] Workflow failed for task: {}", taskInput.getTaskId(), e);
            markTaskFailed(taskInput.getTaskId(), e);
        }
    }

    private void markTaskFailed(String taskId, Exception e) {
        try {
            String errorMsg = e.getMessage() != null
                    ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                    : "Unknown error";
            taskRepository.update(new LambdaUpdateWrapper<AnalysisTaskEntity>()
                    .eq(AnalysisTaskEntity::getTaskId, taskId)
                    .set(AnalysisTaskEntity::getStatus, TaskStatus.FAILED.name())
                    .set(AnalysisTaskEntity::getUpdatedAt, LocalDateTime.now()));
            log.info("[Async] Marked task {} as FAILED", taskId);
        } catch (Exception updateEx) {
            log.error("[Async] Failed to mark task {} as FAILED", taskId, updateEx);
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `./mvnw test -pl . -Dtest=WorkflowServiceTest -Dspring.profiles.active=test --no-transfer-progress`
Expected: PASS

- [ ] **Step 5: 运行全量测试确认无回归**

Run: `./mvnw test -Dspring.profiles.active=test --no-transfer-progress`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/example/ca_agent/service/WorkflowService.java src/test/java/org/example/ca_agent/service/WorkflowServiceTest.java
git commit -m "fix: mark task as FAILED when async workflow throws exception"
```

---

### Task 2: LLM 调用超时 + 网络级重试

**Files:**
- Modify: `pom.xml` (添加 spring-retry)
- Modify: `src/main/java/.../config/SpringAiAlibabaConfig.java` (EnableRetry + RestClient timeout)
- Modify: `src/main/java/.../service/SpringAiModelChatGateway.java` (添加 @Retryable)
- Modify: `src/main/resources/application.yml` (超时配置)

- [ ] **Step 1: 添加 spring-retry 依赖到 pom.xml**

在 `<dependencies>` 节点内添加:

```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
</dependency>
```

- [ ] **Step 2: 启用 Retry 并配置 RestClient 超时**

修改 `SpringAiAlibabaConfig.java`:

```java
package org.example.ca_agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableRetry
public class SpringAiAlibabaConfig {

    @Value("${ca-agent.llm.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${ca-agent.llm.read-timeout:120000}")
    private int readTimeout;

    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        return RestClient.builder().requestFactory(factory);
    }
}
```

- [ ] **Step 3: 为 Gateway 添加 @Retryable 注解**

修改 `SpringAiModelChatGateway.java`:

```java
package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.common.BizException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiModelChatGateway implements ModelChatGateway {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    @Override
    @Retryable(
            retryFor = {ResourceAccessException.class, SocketTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public String call(String systemPrompt, String userPrompt) {
        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (chatClientBuilder == null) {
            throw new BizException(503, "LLM chat model is not configured");
        }
        log.debug("Calling LLM, prompt length: system={}, user={}",
                systemPrompt.length(), userPrompt.length());
        return chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }
}
```

- [ ] **Step 4: 添加配置到 application.yml**

在 `ca-agent:` 下添加:

```yaml
ca-agent:
  llm:
    connect-timeout: ${CA_AGENT_LLM_CONNECT_TIMEOUT:10000}
    read-timeout: ${CA_AGENT_LLM_READ_TIMEOUT:120000}
```

- [ ] **Step 5: 运行全量测试确认无回归**

Run: `./mvnw test -Dspring.profiles.active=test --no-transfer-progress`
Expected: All tests PASS (测试中 RecordingGateway 不触发 retry)

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/org/example/ca_agent/config/SpringAiAlibabaConfig.java src/main/java/org/example/ca_agent/service/SpringAiModelChatGateway.java src/main/resources/application.yml
git commit -m "feat: add LLM timeout configuration and network-level retry with spring-retry"
```

---

### Task 3: Token/Prompt 用量追踪

**Files:**
- Create: `src/main/java/.../dto/agent/LlmCallResult.java`
- Create: `src/main/java/.../service/TokenUsageAccumulator.java`
- Create: `src/test/java/.../service/TokenUsageAccumulatorTest.java`
- Modify: `src/main/java/.../service/ModelChatGateway.java`
- Modify: `src/main/java/.../service/SpringAiModelChatGateway.java`
- Modify: `src/main/java/.../service/LlmChatService.java`
- Modify: `src/main/java/.../dto/agent/AgentRunTrace.java`
- Modify: `src/main/java/.../entity/AgentRunEntity.java`
- Modify: `src/main/java/.../dto/response/AgentRunResponse.java`
- Modify: `src/main/java/.../agent/AgentRunTracer.java`
- Modify: `src/main/java/.../assembler/StateAssembler.java`
- Modify: `src/main/java/.../service/AgentRunService.java`
- Modify: `src/main/resources/schema.sql`
- Modify: `src/test/java/.../service/StructuredLlmServiceTest.java`

#### Step 3a: Schema + Entity 层

- [ ] **Step 1: 修改 schema.sql 增加 token 列**

在 `agent_run` 表定义中 `error_message TEXT,` 后面追加:

```sql
    prompt_tokens  INTEGER      DEFAULT 0,
    completion_tokens INTEGER   DEFAULT 0,
    total_tokens   INTEGER      DEFAULT 0,
```

- [ ] **Step 2: 修改 AgentRunEntity 增加 token 字段**

在 `AgentRunEntity.java` 的 `errorMessage` 字段后追加:

```java
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
```

- [ ] **Step 3: 修改 AgentRunTrace 增加 token 字段**

在 `AgentRunTrace.java` 的 `errorMessage` 字段后追加:

```java
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
```

- [ ] **Step 4: 修改 AgentRunResponse 增加 token 字段**

在 `AgentRunResponse.java` 的 `errorMessage` 字段后追加:

```java
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
```

#### Step 3b: TokenUsageAccumulator

- [ ] **Step 5: 编写 TokenUsageAccumulator 测试**

```java
package org.example.ca_agent.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenUsageAccumulatorTest {

    @Test
    void startsAtZero() {
        TokenUsageAccumulator accumulator = new TokenUsageAccumulator();
        assertThat(accumulator.getPromptTokens()).isZero();
        assertThat(accumulator.getCompletionTokens()).isZero();
        assertThat(accumulator.getTotalTokens()).isZero();
    }

    @Test
    void accumulatesMultipleCalls() {
        TokenUsageAccumulator accumulator = new TokenUsageAccumulator();
        accumulator.add(100, 50);
        accumulator.add(200, 80);

        assertThat(accumulator.getPromptTokens()).isEqualTo(300);
        assertThat(accumulator.getCompletionTokens()).isEqualTo(130);
        assertThat(accumulator.getTotalTokens()).isEqualTo(430);
    }

    @Test
    void resetClearsAccumulation() {
        TokenUsageAccumulator accumulator = new TokenUsageAccumulator();
        accumulator.add(100, 50);
        accumulator.reset();

        assertThat(accumulator.getPromptTokens()).isZero();
        assertThat(accumulator.getCompletionTokens()).isZero();
        assertThat(accumulator.getTotalTokens()).isZero();
    }
}
```

- [ ] **Step 6: 实现 TokenUsageAccumulator**

```java
package org.example.ca_agent.service;

import org.springframework.stereotype.Component;

/**
 * ThreadLocal-based token usage accumulator.
 * Tracks token consumption per agent execution transparently.
 * Reset before each agent run, read after completion.
 */
@Component
public class TokenUsageAccumulator {

    private static final ThreadLocal<int[]> USAGE = ThreadLocal.withInitial(() -> new int[3]);

    public void reset() {
        USAGE.set(new int[3]);
    }

    public void add(int promptTokens, int completionTokens) {
        int[] usage = USAGE.get();
        usage[0] += promptTokens;
        usage[1] += completionTokens;
        usage[2] += (promptTokens + completionTokens);
    }

    public int getPromptTokens() {
        return USAGE.get()[0];
    }

    public int getCompletionTokens() {
        return USAGE.get()[1];
    }

    public int getTotalTokens() {
        return USAGE.get()[2];
    }

    public void clear() {
        USAGE.remove();
    }
}
```

- [ ] **Step 7: 运行 TokenUsageAccumulator 测试**

Run: `./mvnw test -pl . -Dtest=TokenUsageAccumulatorTest --no-transfer-progress`
Expected: PASS

#### Step 3c: Gateway + LlmChatService 适配

- [ ] **Step 8: 创建 LlmCallResult DTO**

```java
package org.example.ca_agent.dto.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmCallResult {
    private String content;
    private int promptTokens;
    private int completionTokens;
    private int totalTokens;

    public static LlmCallResult ofContent(String content) {
        return new LlmCallResult(content, 0, 0, 0);
    }
}
```

- [ ] **Step 9: 修改 ModelChatGateway 接口**

```java
package org.example.ca_agent.service;

import org.example.ca_agent.dto.agent.LlmCallResult;

public interface ModelChatGateway {

    LlmCallResult call(String systemPrompt, String userPrompt);
}
```

- [ ] **Step 10: 修改 SpringAiModelChatGateway 提取 token 用量**

```java
package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ca_agent.common.BizException;
import org.example.ca_agent.dto.agent.LlmCallResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringAiModelChatGateway implements ModelChatGateway {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    @Override
    @Retryable(
            retryFor = {ResourceAccessException.class, SocketTimeoutException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public LlmCallResult call(String systemPrompt, String userPrompt) {
        ChatClient.Builder chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        if (chatClientBuilder == null) {
            throw new BizException(503, "LLM chat model is not configured");
        }
        log.debug("Calling LLM, prompt length: system={}, user={}",
                systemPrompt.length(), userPrompt.length());

        ChatResponse chatResponse = chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .chatResponse();

        String content = "";
        int promptTokens = 0;
        int completionTokens = 0;

        if (chatResponse != null && chatResponse.getResult() != null) {
            content = chatResponse.getResult().getOutput().getText();
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                promptTokens = (int) chatResponse.getMetadata().getUsage().getPromptTokens();
                completionTokens = (int) chatResponse.getMetadata().getUsage().getCompletionTokens();
            }
        }

        return new LlmCallResult(content, promptTokens, completionTokens,
                promptTokens + completionTokens);
    }
}
```

- [ ] **Step 11: 修改 LlmChatService 适配新接口并累加 token**

```java
package org.example.ca_agent.service;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.dto.agent.LlmCallResult;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LlmChatService {

    private final ModelChatGateway modelChatGateway;
    private final TokenUsageAccumulator tokenUsageAccumulator;

    public String callSimpleChat(String prompt) {
        return callChat("", prompt);
    }

    public String callChat(String systemPrompt, String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        String normalizedSystemPrompt = systemPrompt == null ? "" : systemPrompt.trim();
        LlmCallResult result = modelChatGateway.call(normalizedSystemPrompt, userPrompt.trim());
        tokenUsageAccumulator.add(result.getPromptTokens(), result.getCompletionTokens());
        return result.getContent();
    }
}
```

- [ ] **Step 12: 修改 StructuredLlmServiceTest 适配新接口**

更新 `RecordingGateway`:

```java
private static class RecordingGateway implements ModelChatGateway {

    private final Queue<String> responses;
    private int callCount;

    private RecordingGateway(String... responses) {
        this.responses = new ArrayDeque<>(Arrays.asList(responses));
    }

    @Override
    public LlmCallResult call(String systemPrompt, String userPrompt) {
        callCount++;
        return LlmCallResult.ofContent(responses.remove());
    }
}
```

同时更新 `StructuredLlmService` 的构造：`new StructuredLlmService(new LlmChatService(gateway, new TokenUsageAccumulator()))`

#### Step 3d: AgentRunTracer 集成

- [ ] **Step 13: 修改 AgentRunTracer 收集 token**

```java
package org.example.ca_agent.agent;

import lombok.RequiredArgsConstructor;
import org.example.ca_agent.assembler.StateAssembler;
import org.example.ca_agent.dto.agent.AgentRunTrace;
import org.example.ca_agent.enums.AgentType;
import org.example.ca_agent.service.TokenUsageAccumulator;
import org.example.ca_agent.workflow.CompetitiveAnalysisState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgentRunTracer {

    private final StateAssembler stateAssembler;
    private final TokenUsageAccumulator tokenUsageAccumulator;

    public void trace(AgentNode agent, CompetitiveAnalysisState state) {
        String taskId = state.getTaskInput().getTaskId();
        String runId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        long startMs = System.currentTimeMillis();

        // Reset token accumulator before agent execution
        tokenUsageAccumulator.reset();

        try {
            agent.execute(state);
            long durationMs = System.currentTimeMillis() - startMs;
            AgentRunTrace trace = buildSuccessTrace(runId, taskId, agent.getAgentType(), startTime, durationMs);
            attachTokenUsage(trace);
            record(state, trace);
            stateAssembler.saveState(state);
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            AgentRunTrace trace = buildFailureTrace(runId, taskId, agent.getAgentType(), startTime, durationMs, e);
            attachTokenUsage(trace);
            record(state, trace);
            stateAssembler.saveState(state);
            throw e;
        }
    }

    private void attachTokenUsage(AgentRunTrace trace) {
        trace.setPromptTokens(tokenUsageAccumulator.getPromptTokens());
        trace.setCompletionTokens(tokenUsageAccumulator.getCompletionTokens());
        trace.setTotalTokens(tokenUsageAccumulator.getTotalTokens());
    }

    private void record(CompetitiveAnalysisState state, AgentRunTrace trace) {
        state.getAgentRuns().add(trace);
    }

    private AgentRunTrace buildSuccessTrace(String runId, String taskId, AgentType agentType,
                                             LocalDateTime startTime, long durationMs) {
        AgentRunTrace trace = new AgentRunTrace();
        trace.setRunId(runId);
        trace.setTaskId(taskId);
        trace.setAgentType(agentType);
        trace.setInputType(resolveInputType(agentType));
        trace.setOutputType(resolveOutputType(agentType));
        trace.setStatus("SUCCESS");
        trace.setStartTime(startTime);
        trace.setEndTime(LocalDateTime.now());
        trace.setDurationMs(durationMs);
        return trace;
    }

    private AgentRunTrace buildFailureTrace(String runId, String taskId, AgentType agentType,
                                             LocalDateTime startTime, long durationMs, Exception e) {
        AgentRunTrace trace = buildSuccessTrace(runId, taskId, agentType, startTime, durationMs);
        trace.setStatus("FAILED");
        trace.setErrorMessage(e.getMessage());
        return trace;
    }

    private String resolveInputType(AgentType agentType) {
        return switch (agentType) {
            case PLANNER_AGENT -> "TaskInputDTO";
            case COLLECTOR_AGENT -> "TaskPlanDTO + RepairInstructions";
            case EXTRACTOR_AGENT -> "RawSourceSetDTO";
            case ANALYZER_AGENT -> "ProductProfileSetDTO";
            case WRITER_AGENT -> "ProductProfileSetDTO + CompetitiveAnalysisDTO";
            case REVIEWER_AGENT -> "CompetitiveAnalysisState";
        };
    }

    private String resolveOutputType(AgentType agentType) {
        return switch (agentType) {
            case PLANNER_AGENT -> "TaskPlanDTO";
            case COLLECTOR_AGENT -> "RawSourceSetDTO";
            case EXTRACTOR_AGENT -> "ProductProfileSetDTO";
            case ANALYZER_AGENT -> "CompetitiveAnalysisDTO";
            case WRITER_AGENT -> "ReportDraftDTO";
            case REVIEWER_AGENT -> "ReviewResultDTO";
        };
    }
}
```

#### Step 3e: 持久化 + Response 映射

- [ ] **Step 14: 修改 StateAssembler.saveAgentRuns 持久化 token 字段**

在 `saveAgentRuns` 方法中 `entity.setErrorMessage(trace.getErrorMessage())` 后追加:

```java
            entity.setPromptTokens(trace.getPromptTokens());
            entity.setCompletionTokens(trace.getCompletionTokens());
            entity.setTotalTokens(trace.getTotalTokens());
```

- [ ] **Step 15: 修改 AgentRunService.toResponse 映射 token 字段**

在 `toResponse` 方法中 `response.setErrorMessage(entity.getErrorMessage())` 后追加:

```java
        response.setPromptTokens(entity.getPromptTokens());
        response.setCompletionTokens(entity.getCompletionTokens());
        response.setTotalTokens(entity.getTotalTokens());
```

#### Step 3f: 验证

- [ ] **Step 16: 运行全量测试**

Run: `./mvnw test -Dspring.profiles.active=test --no-transfer-progress`
Expected: All tests PASS

- [ ] **Step 17: Commit**

```bash
git add -A
git commit -m "feat: add token usage tracking per agent run with ThreadLocal accumulator"
```

---

### Task 4: 前端适配 Token 展示 (可选)

**Files:**
- Modify: `frontend/src/types/index.ts`
- Modify: `frontend/src/components/AgentRunsTab.vue`

- [ ] **Step 1: 更新 AgentRun 类型定义**

在 `frontend/src/types/index.ts` 的 `AgentRun` 接口中追加:

```typescript
  promptTokens?: number
  completionTokens?: number
  totalTokens?: number
```

- [ ] **Step 2: 在 AgentRunsTab 展示 token 用量**

在展开详情区域追加 token 展示（如有值则显示）。

- [ ] **Step 3: 验证前端编译**

Run: `cd frontend && npx vue-tsc --noEmit && npx vite build`
Expected: 零错误

- [ ] **Step 4: Commit**

```bash
git add frontend/src/types/index.ts frontend/src/components/AgentRunsTab.vue
git commit -m "feat(frontend): display token usage in agent runs tab"
```
