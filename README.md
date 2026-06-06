# CA Agent - 智能竞品分析系统

> 基于多 Agent 协作的竞品分析自动化平台，利用大语言模型（LLM）驱动 Planner、Collector、Extractor、Analyzer、Writer、Reviewer 六个智能体，自动完成从任务规划、信息收集、证据提取、分析对比、报告撰写到质量审查的完整竞品分析工作流。

---

## 🚀 功能特性

| 特性 | 说明 |
|------|------|
| **多 Agent 协作** | 6 个智能体按 DAG 工作流串行/循环执行，支持自动修复迭代 |
| **实时搜索采集** | 集成秘塔 Metaso 搜索 API，支持网页搜索与内容读取 |
| **结构化报告** | 自动生成 14 章节完整中文竞品分析报告，含数据引用和来源标注 |
| **质量审查** | Reviewer Agent 对报告进行 100 分制评分，自动触发修复迭代 |
| **Web 前端** | Vue 3 + Element Plus 管理界面，支持任务创建、进度追踪、报告浏览 |
| **全流程可视化** | 任务状态实时轮询，Agent 执行轨迹时间线展示 |

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
| Vite | 8.0.12 | 构建工具 |
| Element Plus | 2.14.1 | UI 组件库 |
| Vue Router | 4.6.4 | 路由管理 |

### 外部服务

| 服务 | 用途 |
|------|------|
| Doubao (OpenAI-compatible) | LLM 推理，模型 `ep-20260514111325-xjmj7` |
| 秘塔 Metaso API | 网页搜索与内容读取 |

---

## 📁 项目结构

```
CA_Agent/
├── docs/                          # 项目文档（PRD、架构、API 规范等）
├── frontend/                      # Vue 3 前端项目
│   ├── src/
│   │   ├── api/                   # API 请求封装
│   │   ├── components/            # Tab 组件、状态标签
│   │   ├── router/                # 路由配置
│   │   ├── types/                 # TypeScript 类型定义
│   │   └── views/                 # 页面视图（创建页、详情页）
│   └── vite.config.ts             # Vite 配置（含 API 代理）
├── src/main/java/org/example/ca_agent/
│   ├── agent/                     # 6 个 Agent 实现 + 执行追踪器
│   ├── client/                    # 外部 API 客户端（Metaso）
│   ├── config/                    # 配置类
│   ├── controller/                # REST API 控制器
│   ├── dto/                       # 数据传输对象
│   ├── entity/                    # 数据库实体
│   ├── repository/                # MyBatis-Plus Mapper
│   ├── service/                   # 业务逻辑层
│   ├── tool/                      # Agent 工具组件
│   └── workflow/                  # 工作流编排与状态管理
├── src/main/resources/
│   ├── application.yml            # 主配置
│   ├── application-local.yml      # 本地开发配置（含 API Key）
│   └── static/                    # 前端构建产物
└── src/test/                      # 单元测试与集成测试
```

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

打开浏览器访问 `http://localhost:5173，即可创建竞品分析任务。

---

## 🔌 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/tasks` | 创建任务 |
| `GET` | `/api/tasks/{taskId}` | 获取任务详情 |
| `GET` | `/api/tasks/{taskId}/report` | 获取报告 |
| `GET` | `/api/tasks/{taskId}/evidence` | 获取证据列表 |
| `GET` | `/api/tasks/{taskId}/review` | 获取质检结果 |
| `GET` | `/api/tasks/{taskId}/agent-runs` | 获取 Agent 执行轨迹 |

---

## 🧪 测试

```bash
# 运行全部测试（mock 模式，无需外部 API）
mvn test

# 运行包含 live 标签的测试（需要真实 API Key，耗时较长）
mvn test -Dtest-profile=live
```

当前测试覆盖：117 个 mock 测试通过，5 个 live 测试可选执行。

---

## 📝 开发任务清单

| 任务 | 状态 | 说明 |
|------|------|------|
| Task 01-06 | ✅ | 基础架构、数据库、实体、Prompt 模板 |
| Task 07 | ✅ | 5 个核心 Agent 连接真实 LLM，92 mock + 5 live 测试通过 |
| Task 08 | ✅ | 秘塔搜索 API 集成，端到端完整报告生成（约 5 分钟，质检 82 分） |
| Task 09 | ✅ | 前端页面实现（Vue 3 + Element Plus） |
| Task 10 | 🔄 | Demo 数据与演示材料（待完成） |

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request。

---

## 📄 License

MIT License
