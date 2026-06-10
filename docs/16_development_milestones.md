# Compass 智能竞品分析平台 — 开发里程碑

> 本文档记录项目从初始化到当前版本的完整开发历程，包含各版本的功能变更、技术决策和关键节点。

---

## 版本总览

| 版本 | 阶段 | 核心交付 | 状态 |
|------|------|----------|------|
| v0.1 | 项目初始化 | 项目结构、数据协议层 | ✅ |
| v0.2 | 数据持久化 | MyBatis-Plus + PostgreSQL | ✅ |
| v0.3 | LLM 集成 | Spring AI + 豆包对接 | ✅ |
| v0.4 | 真实 Agent | 6 Agent 全部对接 LLM | ✅ |
| v0.5 | 搜索集成 | 秘塔 Metaso API | ✅ |
| v0.6 | 前端实现 | Vue 3 基础页面 | ✅ |
| v0.7 | P0/P1 优化 | 稳定性 + 质量保障 | ✅ |
| v0.8 | UI 重设计 | 专业版前端界面 | ✅ |
| v0.9 | 增强功能 | 修复闭环 + Markdown 渲染 | ✅ |
| v1.0 | 正式版 | 全部功能完成 | ✅ |

---

## 详细版本记录

### v0.1 — 项目初始化

**提交**: `4ae7c14` feat: init project structure and data protocol layer

**交付内容**：
- Spring Boot 3.4.1 项目骨架
- 6 Agent 基础类定义（PlannerAgent、CollectorAgent 等）
- 工作流 DAG 结构（CompetitiveAnalysisGraph）
- DTO 协议层定义（TaskInputDTO、TaskPlanDTO 等）
- Mock 数据支持（MockCompetitiveAnalysisFixtures）

**技术决策**：
- 采用 Java 17 + Spring Boot 3.x
- 使用 DAG 模式编排 Agent 执行顺序
- Mock 模式与 Live 模式分离

---

### v0.2 — 数据持久化

**提交**: `f74ed65` refactor(persistence): 迁移 JPA 到 MyBatis-Plus 并添加 PostgreSQL 支持

**交付内容**：
- MyBatis-Plus ORM 集成
- PostgreSQL 数据库支持
- 7 张核心表结构（analysis_task、evidence、claim、report、review_issue、agent_run、repair_instruction）
- Entity/Repository/Assembler 层实现
- H2 测试环境配置

**技术决策**：
- 从 JPA 迁移到 MyBatis-Plus（更灵活的 SQL 控制）
- 采用 `CREATE TABLE IF NOT EXISTS` 自动建表
- 生产环境使用 PostgreSQL，测试使用 H2

---

### v0.3 — LLM 集成

**提交**: `e41dcac` feat: add OpenAI-compatible LLM chat client

**交付内容**：
- Spring AI 1.1.7 集成
- OpenAI-compatible 客户端（适配豆包 API）
- 配置化模型参数（temperature、max-tokens）
- Agent 执行模式切换（mock/live）

**技术决策**：
- 采用 Spring AI 作为 LLM 抽象层
- 使用豆包 doubao-seed-2.0-lite 模型
- 支持环境变量注入 API Key

---

### v0.4 — 真实 Agent 对接

**提交**: `2c165bf` ~ `4c593d4` feat: connect [Agent] to LLM

**交付内容**：
- 6 套 Prompt 模板设计（System Prompt + User Prompt）
- 结构化 JSON 输出解析
- AgentOutputValidator 校验框架
- 版本化 Prompt 管理

**技术决策**：
- 每个 Agent 独立 Prompt 类，支持版本追踪
- 强制要求纯 JSON 输出，禁止 Markdown 包裹
- 严格校验 DTO Schema，防止幻觉

---

### v0.5 — 搜索集成

**提交**: `b2e0371` feat(task08): integrate Metaso search API for CollectorAgent

**交付内容**：
- 秘塔 Metaso API 对接
- MetasoSearchClient HTTP 客户端
- 多维度搜索策略（官网/定价/文档/评测/博客/GitHub）
- 搜索结果聚合和去重

**技术决策**：
- 每个产品 4-6 个英文 query，提升搜索覆盖率
- 支持 search 和 read_url 两种 API 模式
- 结果数量可配置（默认 10 条/query）

---

### v0.6 — 前端实现

**提交**: `faafe85` feat(task09): 前端页面实现 + 后端异步优化

**交付内容**：
- Vue 3 + TypeScript + Element Plus 前端项目
- TaskCreate 任务创建页
- TaskDetail 任务详情页（6 个 Tab）
- 后端异步工作流（@Async）
- Vite 代理配置

**技术决策**：
- 采用 Vue 3 Composition API
- Element Plus 作为 UI 组件库
- Vite 开发服务器 + API 代理

---

### v0.7 — P0/P1 优化

**提交**: `6e6576c` feat: P0 optimizations + LLM call trace observability

**交付内容**：
- P0：LLM 调用追踪（Prompt/Response/Token/耗时）
- P1：增量保存中间结果
- P1：Reviewer 评分容错（score=null 处理）
- P1：上下文保真（保留完整 Evidence contentSnippet）
- P1：Reviewer 先存后验（防止异常时结果丢失）

**技术决策**：
- AgentRunTracer 统一追踪所有 LLM 调用
- 每个 Agent 执行后立即持久化结果
- ReviewerAgent 上下文不再压缩，保留完整信息

---

### v0.8 — UI 重设计

**提交**: `9b94321` ~ `03defb5` feat(frontend): redesign

**交付内容**：
- TaskCreate 两栏专业布局
- TaskDetail 多 Agent 分析控制台
- 报告页阅读模式切换（简洁/证据）
- 证据链 Drawer 展示
- Agent 轨迹折叠时间线

**技术决策**：
- 采用 sticky 布局，左侧工作流面板固定
- Drawer 替代内联展示，优化空间利用
- 代码块支持复制功能

---

### v0.9 — 增强功能

**提交**: `37daab3` feat: 修复闭环可视化 + repair diff

**交付内容**：
- 修复闭环可视化（RepairDiffTab 组件）
- RepairDiffService 计算 before/after 快照
- 报告页 Markdown 渲染（marked 库）
- WriterPrompt 增加对比表格生成指令
- evidenceId 幻觉双层防护（sanitize + warn）

**技术决策**：
- 采用 marked 库进行 Markdown → HTML 渲染
- 双层防护：sanitize 软修正 + warn 宽容校验
- 修复 diff 持久化到 repair_diff 表

---

### v1.0 — 正式版

**提交**: `68aceae` refactor: 前端界面正式版风格改造

**交付内容**：
- 品牌改造：产品名 "Compass · 智能竞品分析平台"
- Agent 中文业务命名（规划分析/信息采集/要素提取/深度分析/报告撰写/质量审查）
- 移除 "CA Agent"、"Local Demo" 等技术字样
- 真实产品 URL 映射（证据链接可点击跳转）
- 工作流失败时保存中间状态
- 质检页空数据智能判断

**技术决策**：
- 两步法状态管理：createState() + execute()
- 异常捕获中保存中间结果，不丢失进度
- 前端区分"未执行质检"与"无问题"两种空状态

---

## 代码统计

### 代码量（截至 v1.0）

| 类型 | 文件数 | 代码行数 |
|------|--------|----------|
| Java 后端 | 50+ | 5000+ |
| Vue/TS 前端 | 15+ | 3500+ |
| 测试代码 | 25+ | 2000+ |
| SQL Schema | 1 | 150+ |
| **总计** | **90+** | **10,000+** |

### 核心模块分布

| 模块 | 文件数 | 说明 |
|------|--------|------|
| agent/ | 8 | 6 Agent + Tracer + 路由 |
| prompt/ | 6 | 6 套 Prompt 模板 |
| service/ | 10 | 业务逻辑层 |
| workflow/ | 4 | 工作流编排 |
| components/ | 8 | 前端核心组件 |
| views/ | 2 | 页面视图 |

---

## 分支策略

| 分支 | 用途 | 合并状态 |
|------|------|----------|
| `main` | 主分支，稳定版本 | ✅ |
| `feature/task04-persistence` | 数据持久化 | ✅ 已合并 |
| `feature/task06-llm-chat-client` | LLM 集成 | ✅ 已合并 |
| `feature/task07-real-prompt-agents` | 真实 Agent | ✅ 已合并 |
| `feature/task08-search-tools` | 搜索工具 | ✅ 已合并 |
| `feature/task09-frontend` | 前端实现 | ✅ 已合并 |
| `feature/p1-optimizations` | P1 优化 | ✅ 已合并 |
| `feature/frontend-ui-redesign` | UI 重设计 | ✅ 已合并 |

---

## 关键技术决策回顾

| 决策 | 选项 | 选择 | 原因 |
|------|------|------|------|
| ORM 框架 | JPA vs MyBatis-Plus | MyBatis-Plus | 更灵活的 SQL 控制，适合复杂查询 |
| LLM 抽象层 | 直接 HTTP vs Spring AI | Spring AI | 标准化接口，便于切换模型提供商 |
| 前端框架 | React vs Vue 3 | Vue 3 | 开发效率更高，学习曲线更平缓 |
| UI 组件库 | Ant Design vs Element Plus | Element Plus | Vue 3 生态成熟，中文文档完善 |
| Markdown 渲染 | 后端渲染 vs 前端渲染 | 前端 marked 库 | 实时预览，减少服务端压力 |
| 工作流模式 | 同步 vs 异步 | 异步 @Async | 避免 HTTP 超时，提升用户体验 |

---

## 后续规划

| 优先级 | 功能 | 预计时间 |
|--------|------|----------|
| P0 | 云端部署（阿里云/腾讯云） | 2 周 |
| P0 | 用户系统（登录/注册） | 1 周 |
| P1 | 任务列表页（历史任务管理） | 1 周 |
| P1 | 报告导出（PDF/Word） | 2 周 |
| P2 | 多轮对话（追问分析） | 3 周 |
| P2 | 搜索并发优化（CompletableFuture） | 1 周 |
