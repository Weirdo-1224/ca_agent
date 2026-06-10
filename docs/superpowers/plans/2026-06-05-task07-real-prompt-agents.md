# Task 07：真实 Prompt Agent 接入实施计划

> 执行本计划时，必须按任务顺序逐项完成，并使用复选框跟踪进度。每项功能遵循测试驱动开发：先写失败测试，再实现功能，最后运行完整测试。

**目标：** 将 Planner、Extractor、Analyzer、Writer 和 Reviewer Agent 接入已配置的 Doubao OpenAI-compatible 模型，同时保留现有 Mock 工作流作为默认运行模式。

**架构方案：** 不修改现有 Agent 类型和工作流路由。新增 `mock|llm` 运行模式开关、统一的结构化 JSON 模型调用服务、各 Agent 专属 Prompt 构建器和集中式输出校验器。CollectorAgent 在 Task 08 前继续使用 Mock；Agent 执行日志继续只由 Graph 层的 `AgentRunTracer` 统一记录。

**技术栈：** Java 17、Spring Boot 3.4.1、Spring AI 1.1.7 ChatClient、Jackson、JUnit 5、AssertJ

---

## 文件改动范围

**新增文件**

- `src/main/java/org/example/ca_agent/config/AgentModeProperties.java`：绑定 `ca-agent.agent.mode` 配置。
- `src/main/java/org/example/ca_agent/service/StructuredLlmService.java`：调用 system/user Prompt、清洗 JSON、解析 DTO、执行有限重试。
- `src/main/java/org/example/ca_agent/service/AgentOutputValidator.java`：校验 DTO 完整性、产品覆盖情况和证据引用。
- `src/test/java/org/example/ca_agent/common/JsonUtilsModelOutputTest.java`
- `src/test/java/org/example/ca_agent/service/StructuredLlmServiceTest.java`
- `src/test/java/org/example/ca_agent/service/AgentOutputValidatorTest.java`
- `src/test/java/org/example/ca_agent/agent/RealPromptAgentTest.java`

**修改文件**

- `src/main/resources/application.yml`
- `src/main/resources/application-local.yml`：只修改本地被忽略的配置。
- `src/main/java/org/example/ca_agent/common/JsonUtils.java`
- `src/main/java/org/example/ca_agent/service/LlmChatService.java`
- `src/main/java/org/example/ca_agent/service/ModelChatGateway.java`
- `src/main/java/org/example/ca_agent/service/SpringAiModelChatGateway.java`
- `src/main/java/org/example/ca_agent/prompt/PlannerPrompt.java`
- `src/main/java/org/example/ca_agent/prompt/ExtractorPrompt.java`
- `src/main/java/org/example/ca_agent/prompt/AnalyzerPrompt.java`
- `src/main/java/org/example/ca_agent/prompt/WriterPrompt.java`
- `src/main/java/org/example/ca_agent/prompt/ReviewerPrompt.java`
- `src/main/java/org/example/ca_agent/agent/PlannerAgent.java`
- `src/main/java/org/example/ca_agent/agent/ExtractorAgent.java`
- `src/main/java/org/example/ca_agent/agent/AnalyzerAgent.java`
- `src/main/java/org/example/ca_agent/agent/WriterAgent.java`
- `src/main/java/org/example/ca_agent/agent/ReviewerAgent.java`
- 根据新增构造依赖调整现有直接实例化 Agent 或 Graph 的测试。

---

### 任务 1：增加 Agent 运行模式

- [ ] 先编写失败的配置绑定测试，验证默认模式为 `mock`，配置 `ca-agent.agent.mode=llm` 后启用真实 Agent。
- [ ] 新增 `AgentModeProperties`，使用明确的 `MOCK` 和 `LLM` 枚举。
- [ ] 在 `application.yml` 增加默认配置：

```yaml
ca-agent:
  agent:
    mode: ${CA_AGENT_AGENT_MODE:mock}
```

- [ ] 只在被 Git 忽略的 `application-local.yml` 中配置 `ca-agent.agent.mode: llm`。
- [ ] 运行配置绑定测试和完整测试。
- [ ] 提交：`feat: add configurable agent execution mode`

### 任务 2：支持 System Prompt 和 User Prompt 分离调用

- [ ] 先编写失败测试，验证 `ModelChatGateway` 可以分别接收 system Prompt 和 user Prompt。
- [ ] 将网关接口修改为：

```java
String call(String systemPrompt, String userPrompt);
```

- [ ] 保留 Task 06 的 `LlmChatService.callSimpleChat(String)`，并新增：

```java
String callChat(String systemPrompt, String userPrompt);
```

- [ ] 修改 `SpringAiModelChatGateway`，调用方式为：

```java
chatClientBuilder.build()
        .prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .call()
        .content();
```

- [ ] 运行针对性测试和完整测试。
- [ ] 提交：`feat: support system and user LLM prompts`

### 任务 3：安全清洗并解析模型 JSON

- [ ] 先编写失败的 `JsonUtilsModelOutputTest`，覆盖纯 JSON、Markdown JSON 代码块、JSON 前后带说明文字、字符串中包含大括号、非法 JSON。
- [ ] 新增 `JsonUtils.extractJsonObject(String)`，使用能够识别引号和转义字符的大括号扫描逻辑，不使用贪婪正则表达式。
- [ ] 新增 `JsonUtils.fromModelJson(String, Class<T>)`。
- [ ] 编写失败的 `StructuredLlmServiceTest`，覆盖正常解析、非法 JSON 后重试一次、超过重试次数后失败。
- [ ] 实现 `StructuredLlmService.generate(systemPrompt, userPrompt, outputType)`，只允许有限重试，最终失败时抛出 `BizException`。
- [ ] 运行针对性测试和完整测试。
- [ ] 提交：`feat: add structured LLM response parsing`

### 任务 4：实现带版本号的 Prompt 构建器

- [ ] 先编写测试，验证每个 Prompt 都包含版本号、只输出纯 JSON 的约束、输入 JSON 和必需输出字段。
- [ ] 为以下 Prompt 类实现 `SYSTEM_PROMPT`、`VERSION` 和 `buildUserPrompt(...)`：

```text
planner_prompt_v1
extractor_prompt_v1
analyzer_prompt_v1
writer_prompt_v1
reviewer_prompt_v1
```

- [ ] Prompt 内容以 `docs/05_agent_prompt_spec.md` 中对应章节为准。
- [ ] Extractor、Analyzer、Writer 和 Reviewer Prompt 需要包含 `repairInstructions`，使回退重跑能够处理 Reviewer 的修复要求。
- [ ] 明确要求模型输出中的 ID 和枚举值符合现有 DTO 约束。
- [ ] 运行 Prompt 测试和完整测试。
- [ ] 提交：`feat: implement versioned agent prompts`

### 任务 5：增加集中式 Agent 输出校验

- [ ] 针对每条拒绝规则先编写失败测试。
- [ ] 实现以下校验规则：

```text
Planner：
  collectionTasks 必须覆盖所有目标产品。

Extractor：
  products 不能为空。
  每个 Claim 必须包含 evidenceIds。

Analyzer：
  comparisonMatrix 不能为空。
  comparisonMatrix 必须覆盖所有产品。

Writer：
  必须包含规定的 14 个标准报告章节。

Reviewer：
  passed、score 和 nextAction 不能为空。
  issue.targetAgent 必须是有效 Agent。

通用规则：
  所有 evidenceIds 必须来自输入 evidencePool。
  所有 taskId 必须与 State 中的真实 taskId 一致。
```

- [ ] 校验失败时抛出 `BizException`，错误信息需要包含 Agent 名称和失败规则。
- [ ] 运行校验器测试和完整测试。
- [ ] 提交：`feat: validate structured agent outputs`

### 任务 6：迁移 PlannerAgent

- [ ] 使用假模型返回值编写 PlannerAgent 失败测试。
- [ ] 在 `llm` 模式下：序列化 `TaskInputDTO`、构建 Planner Prompt、生成并解析 `TaskPlanDTO`、执行校验、覆盖真实 taskId、写入 State。
- [ ] 在 `mock` 模式下：完整保留现有行为。
- [ ] 运行 PlannerAgent 测试和现有 Mock Graph 测试。
- [ ] 提交：`feat: connect planner agent to LLM`

### 任务 7：迁移 ExtractorAgent

- [ ] 编写失败测试，覆盖包含有效证据的产品画像 JSON，以及 Claim 缺少 evidenceIds 的非法输出。
- [ ] 在 `llm` 模式下：发送 `RawSourceSetDTO` 和 repairInstructions，解析 `ProductProfileSetDTO`，校验证据引用，写入 State。
- [ ] 在 `mock` 模式下：保留现有行为。
- [ ] 运行针对性测试和完整测试。
- [ ] 提交：`feat: connect extractor agent to LLM`

### 任务 8：迁移 AnalyzerAgent

- [ ] 编写失败测试，验证产品对比覆盖情况和 evidenceIds 校验。
- [ ] 在 `llm` 模式下：发送产品画像、证据池和 repairInstructions，解析并校验 `CompetitiveAnalysisDTO`。
- [ ] 在 `mock` 模式下：保留现有行为。
- [ ] 运行针对性测试和完整测试。
- [ ] 提交：`feat: connect analyzer agent to LLM`

### 任务 9：迁移 WriterAgent

- [ ] 编写失败测试，验证模型输出必须包含全部 14 个标准章节。
- [ ] 在 `llm` 模式下：发送产品画像、竞品分析、证据池和 repairInstructions，解析并校验 `ReportDraftDTO`。
- [ ] `sourceList` 必须使用 State 中的真实 evidencePool，不信任模型生成的 sourceList。
- [ ] 在 `mock` 模式下：保留现有行为。
- [ ] 运行针对性测试和完整测试。
- [ ] 提交：`feat: connect writer agent to LLM`

### 任务 10：迁移 ReviewerAgent

- [ ] 编写失败测试，覆盖质检通过、需要回退修复、nextAction 非法三种情况。
- [ ] 在 `llm` 模式下：发送完整的待质检 State 和当前迭代次数，解析并校验 `ReviewResultDTO`。
- [ ] 只在 `mock` 模式下保留“第一次失败、第二次通过”的确定性行为。
- [ ] 不修改 `WorkflowRouter` 和 `RepairRouter`。
- [ ] 运行针对性测试和完整测试。
- [ ] 提交：`feat: connect reviewer agent to LLM`

### 任务 11：集成验证和真实模型冒烟测试

- [ ] 保持 `CollectorAgent` 使用 Mock，并确认没有提前实现 Task 08 的工具。
- [ ] 运行 `mvn test`，确认 Mock 工作流、持久化、路由、Prompt、JSON 解析、输出校验和 LLM 模式单元测试全部通过。
- [ ] 使用被忽略的 `application-local.yml`，在 `ca-agent.agent.mode=llm` 下运行一次受控的真实工作流。
- [ ] 验证每个真实 Agent 输出都能成功解析，evidenceIds 保持有效，模型调用或校验失败时 AgentRun 能记录错误。
- [ ] 真实模型测试不能加入默认 Maven 测试套件，避免普通测试依赖网络和 API Key。
- [ ] 如有必要，只提交本任务范围内的修复：`test: verify real prompt agent workflow`

---

## 明确不做的内容

- 不替换 `CollectorAgent` 的 Mock 采集逻辑。
- 不实现网页搜索、网页读取或 Tool Calling。
- 不修改 DTO 字段、WorkflowRouter、RepairRouter 或数据库结构。
- 不在 Agent 内重复记录 AgentRun；`CompetitiveAnalysisGraph` 已统一记录。
- `llm` 模式调用或校验失败时，不允许静默降级为 Mock 输出。

## 完成验收条件

- 默认保持 `mock` 模式，无 API Key 时所有现有测试仍能通过。
- `llm` 模式能够选择当前配置的 Doubao Endpoint。
- 五个真实 Agent 能返回并解析为对应 DTO 的纯 JSON。
- 非法 JSON、非法结构和模型捏造的 evidenceIds 会被明确拒绝。
- CollectorAgent 仍然保持 Mock。
