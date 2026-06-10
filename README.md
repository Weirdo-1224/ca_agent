# Compass · 智能竞品分析平台

> 基于多 Agent 协作的竞品分析自动化平台，利用大语言模型（LLM）驱动六个智能体（规划分析 → 信息采集 → 要素提取 → 深度分析 → 报告撰写 → 质量审查），自动完成从任务规划、信息收集、证据提取、分析对比、报告撰写到质量审查的完整竞品分析工作流。支持跨行业调研，不限定固定产品或领域。

---

## 🚀 功能特性

| 特性 | 说明 |
|------|------|
| **多 Agent 协作** | 6 个智能体按 DAG 工作流串行执行，支持质检不合格时自动修复迭代（最多 3 轮） |
| **跨行业调研** | 支持任意领域和产品的自由输入，不限于固定行业 |
| **实时搜索采集** | 集成秘塔 Metaso 搜索 API，多维度搜索策略覆盖官网/定价/文档/评测/博客/GitHub |
| **Markdown 结构化报告** | 自动生成多章节竞品分析报告，含对比表格、数据引用和来源标注，前端实时渲染 Markdown |
| **质量审查与自动修复** | Reviewer Agent 对报告进行 100 分制评分，低于阈值自动触发修复迭代 |
| **修复闭环可视化** | 完整记录每轮修复的 before/after diff，可视化展示修复历程 |
| **证据链追溯** | Report → Evidence → Claim 完整追溯，证据链接可直接跳转原始来源 |
| **LLM 幻觉防护** | 双层防护机制（sanitize 软修正 + warn 宽容校验），有效防止 evidenceId 幻觉 |
| **LLM 调用追踪** | 记录每次 Agent 的 Prompt、Response、Token 用量和耗时 |
| **中间状态持久化** | 工作流失败时自动保存已完成的中间结果（报告/证据/质检），不丢失进度 |
| **专业 Web 前端** | Vue 3 + Element Plus 现代化分析控制台，任务管理、报告浏览、证据审查一体化 |

---

## 🏗 技术架构

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 3.4.1 | Web 框架 |
| MyBatis-Plus | 3.5.16 | ORM 框架 |
| Spring AI | 1.1.7 | LLM 抽象层 |
| PostgreSQL | — | 主数据库（local 环境） |
| H2 | — | 测试数据库（test 环境） |

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.34 | 前端框架 |
| TypeScript | ~6.0 | 类型系统 |
| Vite | 8.0 | 构建工具 |
| Element Plus | 2.14 | UI 组件库 |
| Vue Router | 4.6 | 路由管理 |

### 前端关键依赖

| 技术 | 用途 |
|------|------|
| marked | Markdown → HTML 渲染（GFM + breaks） |
| axios | HTTP 请求 |

### 外部服务

| 服务 | 用途 |
|------|------|
| Doubao (OpenAI-compatible) | LLM 推理 |
| 秘塔 Metaso API | 网页搜索与内容读取 |

---

## 📁 项目结构

```
CA_Agent/
├── docs/                          # 项目文档（PRD、架构、API 规范等）
├── frontend/                      # Vue 3 前端项目
│   ├── src/
│   │   ├── api/                   # API 请求封装
│   │   ├── components/            # 页面组件（8 个核心组件）
│   │   │   ├── AgentRunsTab.vue       # Agent 执行轨迹时间线
│   │   │   ├── AgentWorkflowPanel.vue # 左侧工作流状态面板
│   │   │   ├── EvidenceTab.vue        # 证据池管理
│   │   │   ├── OverviewTab.vue        # 任务概览
│   │   │   ├── RepairDiffTab.vue      # 修复闭环 diff 对比
│   │   │   ├── ReportTab.vue          # Markdown 报告渲染
│   │   │   ├── ReviewTab.vue          # 质检结果展示
│   │   │   └── TaskStatusHeader.vue   # 顶部状态卡片
│   │   ├── router/                # 路由配置
│   │   ├── types/                 # TypeScript 类型定义
│   │   └── views/                 # 页面视图（TaskCreate、TaskDetail）
│   └── vite.config.ts             # Vite 配置（含 API 代理）
├── src/main/java/org/example/ca_agent/
│   ├── agent/                     # 6 个 Agent 实现 + AgentRunTracer
│   ├── assembler/                 # DTO/State 装配器（EntityAssembler + StateAssembler）
│   ├── client/                    # 外部 API 客户端（Metaso）
│   ├── config/                    # 配置类（模型、搜索、Prompt 模板）
│   ├── controller/                # REST API 控制器
│   ├── dto/                       # 数据传输对象
│   ├── entity/                    # 数据库实体
│   ├── enums/                     # 枚举（AgentType、TaskStatus 等）
│   ├── prompt/                    # 各 Agent 的 Prompt 模板
│   ├── repository/                # MyBatis-Plus Mapper
│   ├── schema/                    # JSON Schema 定义
│   ├── service/                   # 业务逻辑层 + AgentOutputValidator + RepairDiffService
│   ├── tool/                      # Agent 工具组件（搜索、排序）
│   └── workflow/                  # 工作流编排（DAG + RepairRouter + 状态管理）
├── src/main/resources/
│   ├── application.yml            # 主配置
│   ├── application-local.yml      # 本地开发配置（含 API Key）
│   └── static/                    # 前端构建产物
└── src/test/                      # 单元测试与集成测试
```

---

## 🎨 前端页面

### 任务创建页

- 两栏专业布局：左侧 Hero 品牌区 + 六步 Agent 工作流介绍，右侧表单
- 支持自由输入产品名称和领域（跨行业调研）
- 表单分组：基础信息 / 分析配置 / 运行设置
- 快捷入口：查看已有任务详情

### 任务详情页（分析控制台）

- **顶部任务状态卡片**：标题、状态 badge、指标面板（修复轮次/报告章节/证据数/质检分数/总耗时）
- **左侧 Agent 工作流面板**（sticky）：六步中文命名 Agent（规划分析→信息采集→要素提取→深度分析→报告撰写→质量审查），颜色化状态卡片
- **右侧 Tabs 内容区**：
  - 📊 **概览** — 四块卡片：任务摘要、执行摘要、质量摘要（大数字评分）、关键产物统计
  - 📄 **报告** — 左侧章节目录 + 右侧 Markdown 渲染正文（含对比表格），阅读模式切换（简洁阅读/显示证据），证据链 Drawer
  - 🔍 **证据** — 统计筛选条、toolbar 样式筛选栏、表格行点击 Drawer 详情，链接可直接跳转原始来源
  - ✅ **质检** — 大数字评分卡片、nextAction callout、问题卡片列表
  - 🔄 **修复记录** — 每轮修复的 before/after diff 对比，展示修复历程
  - ⏱ **Agent 轨迹** — 统计面板 + 折叠时间线，LLM Prompt/Response 代码块（280px 滚动 + 复制）

---

## ⚡ 快速开始

### 环境要求

- **JDK 17+**
- **Maven 3.9+**（或 IDE 内置 Maven）
- **Node.js 18+**（前端）
- **PostgreSQL**（local 环境，或使用 test 环境的 H2）

### 1. 克隆项目

```bash
git clone https://github.com/Weirdo-1224/ca_agent.git
cd ca_agent
```

### 2. 配置本地环境

复制并编辑 `application-local.yml`：

```yaml
ca-agent:
  ai:
    openai:
      api-key: your-doubao-api-key      # 豆包 API Key
      base-url: https://ark.cn-beijing.volces.com/api/v3
  search:
    enabled: true                         # 是否启用真实搜索（false 则用 mock）
    api-key: your-metaso-api-key          # 秘塔 API Key
```

> ⚠️ `application-local.yml` 已加入 `.gitignore`，不会提交到仓库。

### 3. 启动后端

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

后端服务将启动在 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器将启动在 `http://localhost:5173`，API 请求通过 Vite 代理到后端 `8080` 端口。

### 5. 访问应用

打开浏览器访问 `http://localhost:5173`，即可创建竞品分析任务。

---

## 🔌 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/tasks` | 创建分析任务 |
| `GET` | `/api/tasks/{taskId}` | 获取任务详情与状态 |
| `GET` | `/api/tasks/{taskId}/report` | 获取结构化报告（Markdown 格式） |
| `GET` | `/api/tasks/{taskId}/evidence` | 获取证据池列表 |
| `GET` | `/api/tasks/{taskId}/review` | 获取质检结果与评分 |
| `GET` | `/api/tasks/{taskId}/agent-runs` | 获取 Agent 执行轨迹（含 LLM 调用详情） |
| `GET` | `/api/tasks/{taskId}/repair-diffs` | 获取修复闭环 diff 记录 |

---

## 🧪 测试

```bash
# 运行全部测试（mock 模式，无需外部 API）
mvn test

# 运行包含 live 标签的测试（需要真实 API Key，耗时较长）
mvn test -Dtest-profile=live
```

当前测试覆盖：122 个 mock 测试通过，5 个 live 测试可选执行。

---

## 🔧 已完成优化

### P0 - 核心可用性
- ✅ Agent 实际对接 LLM，输出结构化 JSON
- ✅ LLM 调用追踪（Prompt/Response/Token/耗时）
- ✅ 增量保存中间结果

### P1 - 质量与稳定性
- ✅ 搜索策略多样化（4-6 个英文 query，覆盖官网/定价/文档/评测/博客/GitHub）
- ✅ Reviewer 评分容错（score=null 默认 0，nextAction 自动构建）
- ✅ 上下文保真（保留完整 evidence contentSnippet 和完整报告给 Reviewer）
- ✅ Reviewer 先存后验，确保异常时结果不丢失
- ✅ LLM 幻觉双层防护（sanitize 软修正 + warn 宽容校验）
- ✅ 工作流失败时中间状态持久化（两步法 createState + execute）

### P2 - 报告增强
- ✅ 报告 Markdown 实时渲染（marked 库 + GFM + 完整排版样式）
- ✅ 自动生成对比表格（竞品概览/功能矩阵/定价对比）
- ✅ 证据链接映射真实产品 URL（可直接跳转原始来源）

### P3 - 修复闭环可视化
- ✅ 修复 diff 记录与持久化（RepairDiffService + RepairDiffEntity）
- ✅ 前端 diff 对比视图（RepairDiffTab 组件）
- ✅ RepairRouter 条件路由（分数 < 阈值 & 轮次 < 上限 → 触发修复）

### 前端 UI 正式化
- ✅ 品牌改造：产品名 "Compass · 智能竞品分析平台"，正式版 UI 风格
- ✅ Agent 中文业务命名（规划分析/信息采集/要素提取/深度分析/报告撰写/质量审查）
- ✅ TaskCreate 两栏专业布局 + 跨行业自由输入
- ✅ TaskDetail 多 Agent 分析控制台（状态卡片 + 双栏布局 + 现代化 Tabs）
- ✅ 报告页 Markdown 渲染 + 阅读模式切换 + 证据链 Drawer
- ✅ 证据页统计筛选 + 详情 Drawer
- ✅ 质检页空数据智能判断（区分"未执行"与"无问题"）
- ✅ Agent 轨迹折叠时间线 + Prompt/Response 复制

---

## 📝 后续计划

| 项目 | 说明 |
|------|------|
| 搜索并发优化 | CompletableFuture 并行搜索，提升采集速度 |
| 请求参数校验 | @Valid + JSR-303 注解校验 |
| 多轮对话支持 | 基于已有报告进行追问和深入分析 |
| 历史任务列表 | 支持分页浏览和搜索历史分析任务 |
| 导出功能 | 支持导出 PDF/Word 格式的分析报告 |

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request。

---

## 📄 License

MIT License
