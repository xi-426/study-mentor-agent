# StudyMentor Agent 项目路线图

> 这是本项目后续开发和学习的唯一主线文件。
>
> 项目名：StudyMentor Agent
>
> 技术栈：Java 17、Spring Boot 3、Maven、MySQL 8、Spring Data JPA。
>
> 学习重点：用 Java 做 Agent 工程。默认你会 Java 基础，不再讲 main 方法、package、基础语法，重点讲 Agent 相关设计。

## 0. 我们的学习协议

这个项目必须一步一步写，不能直接跳到完整应用。

每个版本、每个小步骤都按这个节奏来：

1. 先讲这个 Agent 概念是什么。
2. 再讲真实 Agent 产品为什么需要它。
3. 明确这一步要改哪些文件。
4. 只做最小可用的代码改动。
5. 运行或验证结果。
6. 代码跑通后再解释每个核心类的作用。
7. 给你一个小任务，让你亲手改一点。
8. 最后总结这一步面试和简历怎么讲。

重要规则：

- 不先解释概念，就不直接写大功能。
- 不把 Agent 概念藏在普通 Spring Boot 代码里。
- 不把你当 Java 零基础；重点讲 Agent 思维和 Java 落地方式。
- 每个版本都尽量保持项目可运行，至少能被验证。
- API key、数据库密码、token 等敏感信息不能提交到 Git。
- 配置用 `.env.example` 或环境变量占位。
- 每完成一个有意义的小版本，就提交一次 Git。

## 1. 最终产品目标

StudyMentor Agent 是一个学习陪跑 Agent。

最终它要帮助学习者完成这些事情：

- 设置学习目标。
- 把目标拆成阶段计划。
- 生成每日学习任务。
- 解释技术知识点。
- 自动出练习题。
- 批改答案。
- 记录知识点掌握度。
- 发现薄弱知识点。
- 生成每日/每周学习复盘。
- 根据学习进度动态调整计划。

示例输入：

```text
我想 6 周学会 Java Agent 开发。
```

最终理想行为：

```text
1. Agent 理解学习目标。
2. Agent 生成阶段计划。
3. 系统把目标和计划保存到 MySQL。
4. Agent 生成今天的学习任务。
5. Agent 可以讲解某个知识点。
6. Agent 可以出题。
7. Agent 可以批改答案。
8. 系统更新知识点掌握度。
9. Agent 推荐下一次复习内容。
```

## 2. 架构如何一步步进化

我们不一次性搭最终架构，而是让它逐层长出来。

```text
V1：
CLI -> PromptAgent -> LlmClient -> MockLlmClient

V2：
CLI -> PromptAgent -> LlmClient -> 真实大模型 API

V3：
CLI/API -> StructuredAgent -> LlmClient -> JSON -> DTO 校验

V4：
HTTP API -> AgentService -> LlmClient

V5：
HTTP API -> AgentService -> MySQL 记忆

V6：
AgentService -> ToolAgent -> ToolRegistry -> Java 工具 -> MySQL

V7：
SkillAgent -> SkillLoader -> ToolRegistry -> LLM

V8：
前端 -> Spring Boot API/SSE -> AgentService -> Tools/Skills/MySQL/LLM
```

核心思想：

```text
Agent 不等于一次大模型调用。

Agent 是围绕大模型的一层编排系统：
prompt、messages、tools、skills、memory、state、permissions、product UX。
```

## 3. 当前已有模块说明

当前代码故意很小。它只是第一个 Agent 闭环。

### 3.1 `AgentMessage`

路径：

```text
src/main/java/com/studymentor/agent/AgentMessage.java
```

作用：

表示发给模型或从模型返回的一条消息。

一条消息通常包含：

```text
role：消息角色
content：消息内容
```

常见 role：

- `system`：Agent 的身份、规则、边界。
- `user`：用户输入。
- `assistant`：模型回复。
- `tool`：工具返回结果，后面再加。

为什么重要：

大部分聊天模型不是只收一个字符串，而是收一个 `messages` 数组。没有统一的消息对象，后面做聊天历史、工具调用、记忆都会乱。

### 3.2 `LlmClient`

路径：

```text
src/main/java/com/studymentor/llm/LlmClient.java
```

作用：

定义所有大模型客户端都必须遵守的接口。

当前形式：

```java
String chat(List<AgentMessage> messages);
```

核心思想：

Agent 不应该绑定某一个具体模型厂商。

今天可以接 OpenAI-compatible API，明天可以接 DeepSeek、通义千问、Claude 或本地模型。只要它们都实现 `LlmClient`，`PromptAgent` 就不需要大改。

后面可能进化成：

```java
LlmResponse chat(ChatRequest request);
Stream<LlmEvent> stream(ChatRequest request);
```

### 3.3 `MockLlmClient`

路径：

```text
src/main/java/com/studymentor/llm/MockLlmClient.java
```

作用：

假的大模型客户端，用固定回复模拟模型。

为什么先用它：

一开始不要同时处理太多问题。我们先验证 Agent 流程：

```text
用户输入 -> PromptAgent -> LlmClient -> 回复
```

等这个流程通了，再接真实 API。

它帮我们暂时避开：

- API key。
- 网络请求。
- JSON 请求体。
- 模型返回格式。
- 鉴权错误。

### 3.4 `PromptAgent`

路径：

```text
src/main/java/com/studymentor/agent/PromptAgent.java
```

作用：

当前最小的 Agent。

它负责：

- 接收学习目标。
- 生成 system message。
- 生成 user message。
- 调用 `LlmClient`。
- 返回模型回复。

核心思想：

Prompt Agent 是 Agent 的第一层。它还没有工具、数据库、技能、记忆，但已经可以通过 system prompt 约束模型行为。

### 3.5 `StudyMentorCli`

路径：

```text
src/main/java/com/studymentor/cli/StudyMentorCli.java
```

作用：

临时命令行入口。

为什么先用 CLI：

早期学 Agent 不应该被前端、接口、数据库分散注意力。CLI 能最快验证：

```text
输入 -> Agent -> 输出
```

后面它会被 Spring Boot API 和前端页面替代或补充。

## 4. 版本计划

## V0：项目地基

状态：

已部分完成。

目标：

创建一个干净的 Java 17 项目地基，让它后面能逐步成长为 Agent 产品。

范围：

- Maven 项目。
- Spring Boot 依赖声明。
- MySQL 依赖声明。
- 基础包结构。
- README。
- `.env.example`。
- 路线图文件。

涉及文件：

```text
pom.xml
README.md
.gitignore
.env.example
docs/PROJECT_ROADMAP.md
src/main/resources/application.yml
```

引入的 Agent 概念：

- Agent 项目会快速变复杂，所以一开始要有清晰结构。
- Agent 编排逻辑和模型调用逻辑要分开。
- 敏感配置不能进 Git。

V0 不做什么：

- 不做完整 Spring Boot API。
- 不建数据库表。
- 不接真实大模型。
- 不做前端。

验收标准：

- 项目存在于 `D:\鲸域\study-mentor-agent`。
- Java 17 可用。
- Maven 项目文件存在。
- 路线图可读。
- 没有提交敏感信息。

你要亲手做的小任务：

- 阅读本文件。
- 确认路线能接受。
- 把 V0 推到 GitHub。

面试价值：

可以说这个项目不是一次性糊 Demo，而是从 Agent 抽象开始，逐步演进成产品级应用。

## V1：最小 Prompt Agent

状态：

已部分完成，目前使用 `MockLlmClient`。

目标：

构建最小 Agent 闭环：

```text
用户目标 -> messages -> LLM client -> answer
```

涉及文件：

```text
src/main/java/com/studymentor/agent/AgentMessage.java
src/main/java/com/studymentor/agent/PromptAgent.java
src/main/java/com/studymentor/llm/LlmClient.java
src/main/java/com/studymentor/llm/MockLlmClient.java
src/main/java/com/studymentor/cli/StudyMentorCli.java
```

Agent 概念：

- Agent 是编排逻辑，不是模型本身。
- system prompt 定义行为边界。
- user prompt 承载用户当前请求。
- messages 按 role 组织上下文。
- `LlmClient` 抽象模型供应商。

实现要点：

`PromptAgent` 不能知道后面是假模型还是真模型，它只依赖 `LlmClient` 接口。

流程：

```text
StudyMentorCli
  -> PromptAgent.createLearningPlan(goal)
  -> 构造 List<AgentMessage>
  -> LlmClient.chat(messages)
  -> 返回 response
```

验收标准：

- CLI 可以用学习目标运行。
- 输出能展示阶段化学习计划。
- `PromptAgent` 依赖 `LlmClient`，而不是直接依赖 `MockLlmClient`。

你要亲手做的小任务：

- 修改 system prompt，让它更符合你想要的学习教练风格。
- 再运行 CLI，观察输出变化。

教学重点：

- 什么是 system prompt。
- 为什么 message role 很重要。
- 为什么要用 LLM 接口。
- 为什么先 mock。

面试价值：

可以解释 Agent 的第一层抽象：把 Agent 编排逻辑和模型供应商解耦。

## V2：真实 LLM Client

目标：

把固定回复换成真实大模型 API 调用。

可选模型：

- OpenAI-compatible endpoint。
- DeepSeek。
- 通义千问。
- 其他支持 chat completions 的模型。

新增文件：

```text
src/main/java/com/studymentor/llm/OpenAiCompatibleLlmClient.java
src/main/java/com/studymentor/llm/ChatCompletionRequest.java
src/main/java/com/studymentor/llm/ChatCompletionResponse.java
src/main/java/com/studymentor/llm/LlmProperties.java
```

Agent 概念：

- 模型供应商。
- API key。
- Base URL。
- Chat completion request。
- 模型选择。
- temperature。
- 错误处理。

技术选择：

- 先用 Java 17 自带 `HttpClient`，后面进入 Spring Boot 后可换 `RestClient`。
- API key 放环境变量。
- 不能把 API key 写死在代码里。

请求大概长这样：

```json
{
  "model": "model-name",
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "..."}
  ],
  "temperature": 0.3
}
```

返回处理：

- 读取第一条 assistant message。
- 处理空 choices。
- 处理 HTTP 错误。
- 处理缺少 API key。

验收标准：

- `PromptAgent` 可以通过 `LlmClient` 使用真实模型。
- mock 和 real client 切换方便。
- API key 不进 Git。
- 错误信息对开发者可读。

你要亲手做的小任务：

- 通过配置修改模型名。
- 比较不同 system prompt 对输出的影响。

教学重点：

- chat completion API 做了什么。
- API key 为什么必须放环境变量。
- temperature 控制什么。
- Java HTTP 请求如何映射到 LLM 调用。

面试价值：

可以解释 Java 后端如何安全、可替换地接入大模型。

## V3：结构化输出 Agent

目标：

让 Agent 输出稳定 JSON，而不是只输出自然语言。

为什么需要：

自然语言适合人读，但产品需要结构化数据来做 UI、数据库和工作流。

目标输出示例：

```json
{
  "goal": "6周学会 Java Agent 开发",
  "stages": [
    {
      "name": "Prompt Agent",
      "description": "学习 prompt 和 message 编排",
      "tasks": ["理解 system prompt", "运行 CLI Agent"]
    }
  ],
  "todayTasks": [
    {
      "title": "阅读当前 Agent 结构",
      "estimatedMinutes": 30
    }
  ]
}
```

新增文件：

```text
src/main/java/com/studymentor/agent/StructuredAgent.java
src/main/java/com/studymentor/learning/dto/LearningPlanDto.java
src/main/java/com/studymentor/learning/dto/LearningStageDto.java
src/main/java/com/studymentor/learning/dto/LearningTaskDto.java
src/main/java/com/studymentor/common/JsonUtils.java
```

Agent 概念：

- 结构化输出。
- Schema。
- DTO。
- 解析。
- 校验。
- 修复 prompt。

实现步骤：

1. 要求模型只返回 JSON。
2. 用 Jackson 解析 JSON。
3. 校验必填字段。
4. 如果解析失败，展示原始输出并解释失败原因。
5. 后面再加 repair step。

验收标准：

- Agent 可以返回 `LearningPlanDto`。
- 非法 JSON 能被检测出来。
- 用户能看到解析失败原因。

你要亲手做的小任务：

- 给学习阶段增加一个 `difficulty` 字段。
- 同时更新 prompt 和 DTO。

教学重点：

- 为什么 Agent 产品需要 JSON。
- 为什么不能完全相信模型输出。
- Agent workflow 里的 validation 是什么。

面试价值：

可以解释如何让 LLM 输出从“聊天文本”变成“产品可用数据”。

## V4：Spring Boot Chat API

目标：

把 Agent 封装成 HTTP API。

为什么需要：

产品不能只依赖命令行。前端、移动端或其他服务都需要通过 API 调用 Agent。

新增文件：

```text
src/main/java/com/studymentor/agent/AgentService.java
src/main/java/com/studymentor/chat/ChatController.java
src/main/java/com/studymentor/chat/dto/ChatRequest.java
src/main/java/com/studymentor/chat/dto/ChatResponse.java
```

API 示例：

```text
POST /api/chat
POST /api/learning-plan
GET /api/health
```

Agent 概念：

- Agent 作为后端服务。
- 请求/响应边界。
- 先无状态，后有状态。
- 错误响应。

验收标准：

- Spring Boot 可以启动。
- `POST /api/chat` 返回 Agent 回复。
- API 不暴露内部堆栈。

你要亲手做的小任务：

- 给 request 增加 `mode` 字段，例如 `teach`、`plan`、`quiz`。

教学重点：

- 为什么要把 Agent 包成 API。
- CLI 和产品接口有什么区别。
- 前端后面如何调用 Agent。

面试价值：

可以解释 Agent 能力如何变成 Java 后端服务。

## V5：MySQL 记忆

目标：

把学习状态持久化到 MySQL。

为什么需要：

没有记忆，Agent 就会忘记目标、任务、对话和进度。

第一批表：

```text
learning_goals
learning_tasks
chat_sessions
chat_messages
```

后续表：

```text
quiz_questions
quiz_attempts
knowledge_points
mastery_records
daily_reviews
```

新增文件：

```text
src/main/java/com/studymentor/learning/entity/LearningGoal.java
src/main/java/com/studymentor/learning/entity/LearningTask.java
src/main/java/com/studymentor/chat/entity/ChatSession.java
src/main/java/com/studymentor/chat/entity/ChatMessage.java
src/main/java/com/studymentor/learning/repository/LearningGoalRepository.java
src/main/java/com/studymentor/chat/repository/ChatMessageRepository.java
```

Agent 概念：

- 记忆。
- 短期上下文 vs 长期持久化。
- 对话历史。
- 用户状态。

实现步骤：

1. 创建 Entity。
2. 配置 MySQL 连接。
3. 保存学习目标。
4. 保存聊天消息。
5. 从数据库加载最近历史放回 prompt 上下文。

验收标准：

- MySQL 能保存目标和消息。
- 应用重启后数据不丢。
- prompt 可以带入最近历史。

你要亲手做的小任务：

- 给 `LearningTask` 增加 `status` 字段。
- 增加一个接口把任务标记为完成。

教学重点：

- Agent 系统里的 memory 是什么。
- 保存消息和把消息作为上下文是两件事。
- 为什么不能把全部历史都塞进 prompt。

面试价值：

可以解释 MySQL 如何为 Agent 提供长期记忆和学习状态。

## V6：Tool Agent

目标：

让 Agent 可以调用 Java 工具。

为什么需要：

LLM 只能生成文本。工具让 Agent 能真正做事：

- 保存学习目标。
- 读取学习进度。
- 生成练习题。
- 批改答案。
- 更新掌握度。

新增文件：

```text
src/main/java/com/studymentor/tool/Tool.java
src/main/java/com/studymentor/tool/ToolCall.java
src/main/java/com/studymentor/tool/ToolResult.java
src/main/java/com/studymentor/tool/ToolRegistry.java
src/main/java/com/studymentor/tool/SaveLearningGoalTool.java
src/main/java/com/studymentor/tool/GetProgressTool.java
src/main/java/com/studymentor/tool/GenerateQuizTool.java
src/main/java/com/studymentor/agent/ToolAgent.java
```

Agent 概念：

- Tool calling。
- Tool schema。
- 工具选择。
- 工具参数。
- 工具结果。
- 工具失败。
- 工具执行循环。

实现路线：

先走简单路线：

```text
模型输出 JSON：
{"tool": "save_learning_goal", "arguments": {...}}
```

后面再接模型供应商原生 function/tool calling。

验收标准：

- Agent 至少能决定调用一个工具。
- 工具结果能回填给最终回答。
- 未知工具名能优雅处理。
- 参数错误不会让应用崩溃。

你要亲手做的小任务：

- 新增一个工具，例如 `list_today_tasks`。

教学重点：

- 为什么工具是 chatbot 和 Agent 的关键区别。
- 为什么工具 schema 必须精确。
- 为什么工具执行需要校验。

面试价值：

可以解释 Agent 如何把 Java 方法当作工具操作业务数据。

## V7：Skill Agent

目标：

把教学流程规则放进 `SKILL.md`。

为什么需要：

工具定义 Agent 能做什么，Skill 定义 Agent 应该怎么做。

已有文件：

```text
src/main/resources/skills/teaching-coach/SKILL.md
```

新增文件：

```text
src/main/java/com/studymentor/skill/Skill.java
src/main/java/com/studymentor/skill/SkillLoader.java
src/main/java/com/studymentor/agent/SkillAgent.java
src/main/resources/skills/quiz-master/SKILL.md
src/main/resources/skills/review-planner/SKILL.md
```

Agent 概念：

- Skill。
- Policy。
- Workflow。
- 可复用任务说明。
- 工具能力和任务策略分离。

Skill 示例：

- Teaching Coach：一步步讲知识点。
- Quiz Master：出题和批改。
- Review Planner：选择薄弱知识点复习。

验收标准：

- 可以从 resources 加载 skill 内容。
- Agent prompt 中能加入选中的 skill。
- 不同 skill 能改变 Agent 行为。

你要亲手做的小任务：

- 写一个小的 `quiz-master/SKILL.md`。
- 用它改变出题方式。

教学重点：

- Skill 和 prompt 的区别。
- Skill 和 tool 的区别。
- OpenClaw 那类 skill 如何映射到我们的 Java 项目。

面试价值：

可以解释如何把业务流程从代码中外置成 skill 指令。

## V8：练习与掌握度系统

目标：

形成完整学习闭环：

```text
教学 -> 出题 -> 批改 -> 更新掌握度 -> 推荐复习
```

新增表：

```text
quiz_questions
quiz_attempts
knowledge_points
mastery_records
```

新增功能：

- 按知识点生成题目。
- 保存题目。
- 提交答案。
- 用评分规则批改。
- 保存分数和反馈。
- 更新掌握度。

Agent 概念：

- Evaluation。
- Rubric。
- Feedback loop。
- User model。
- Adaptive learning。

验收标准：

- 用户可以请求练习题。
- 用户可以提交答案。
- Agent 可以打分并解释。
- 分数被持久化。
- 能列出薄弱知识点。

你要亲手做的小任务：

- 给评分规则增加一个维度。

教学重点：

- 为什么 Agent 产品需要评估闭环。
- 批改和普通建议有什么不同。
- 为什么掌握度要独立于聊天文本保存。

面试价值：

可以解释 Agent 如何根据用户表现自适应调整学习路径。

## V9：SSE 流式输出

目标：

让 Agent 输出可以流式返回给客户端。

为什么需要：

Agent 回复可能比较慢。流式输出能改善体验，也能展示中间状态。

新增文件：

```text
src/main/java/com/studymentor/chat/StreamingChatController.java
src/main/java/com/studymentor/agent/stream/AgentEvent.java
src/main/java/com/studymentor/agent/stream/AgentEventType.java
```

Agent 概念：

- Streaming。
- Event type。
- `thinking` event。
- `tool_call` event。
- `tool_result` event。
- `final` event。
- `error` event。

为什么先 SSE：

SSE 比 WebSocket 简单，适合先做模型单向流式输出。WebSocket 后面再考虑。

验收标准：

- 客户端可以收到流式事件。
- final answer 能保存。
- error event 可读。

你要亲手做的小任务：

- 工具开始执行时增加一个 `progress` event。

教学重点：

- SSE 和 WebSocket 的区别。
- 为什么 Agent UI 需要事件类型。
- 它和 Arvio/OpenClaw 事件流有什么对应关系。

面试价值：

可以解释如何在 Java 后端设计流式 Agent 响应。

## V10：前端 MVP

目标：

做一个可以真实使用的小界面。

可选技术：

- React。
- Next.js。
- 或先用 Spring Boot 静态页面。

页面：

```text
Dashboard
Chat
Learning Plan
Quiz
Review
```

Agent 概念：

- 产品体验。
- 聊天消息渲染。
- 工具事件渲染。
- 结构化结果渲染。
- loading/error 状态。

验收标准：

- 用户可以在浏览器和 Agent 聊天。
- 用户可以看到当前学习目标。
- 用户可以看到今日任务。
- 用户可以回答练习题。

你要亲手做的小任务：

- 增加一个掌握度展示组件。

教学重点：

- 为什么 raw model output 不够。
- 结构化 Agent 数据如何变成 UI。
- 产品界面如何展示 Agent 状态。

面试价值：

可以展示一个可用 Agent 产品，而不只是后端代码。

## V11：定时任务与每日复盘

目标：

让 Agent 主动生成每日复习计划。

功能：

- 每日任务生成。
- 薄弱知识点复习。
- 每日总结。
- 每周报告。

新增文件：

```text
src/main/java/com/studymentor/scheduler/DailyReviewScheduler.java
src/main/java/com/studymentor/review/DailyReviewService.java
```

Agent 概念：

- Proactive Agent。
- 定时任务。
- 后台 job。
- 基于状态的推荐。

验收标准：

- 应用可以根据存储的学习进度生成每日复盘。
- 复盘能持久化。
- 支持手动重新生成。

你要亲手做的小任务：

- 增加一个接口，手动重新生成今天的复盘。

教学重点：

- 被动 Agent 和主动 Agent 的区别。
- 后台任务为什么需要幂等。
- 定时任务失败怎么办。

面试价值：

可以解释 Agent 如何异步工作，而不是只响应聊天。

## V12：插件与 MCP 思想扩展

目标：

让工具系统可扩展。

为什么需要：

产品级 Agent 不应该把所有能力硬编码在一个 service 里。

新增概念：

- Plugin manifest。
- Tool discovery。
- Tool registry。
- Remote tool server。
- MCP-inspired protocol。

可能新增文件：

```text
plugins/study/plugin.json
plugins/study/tools.md
src/main/java/com/studymentor/plugin/PluginManifest.java
src/main/java/com/studymentor/plugin/PluginLoader.java
src/main/java/com/studymentor/toolserver/ToolServerController.java
```

Agent 概念：

- Plugin。
- MCP。
- 工具发现。
- 本地工具 vs 远程工具。
- 权限边界。

验收标准：

- 至少一个工具可以从插件元数据注册。
- Agent 可以列出可用工具。
- 工具描述和核心 Agent 代码分离。

你要亲手做的小任务：

- 增加一个简单的 `flashcard` 插件。

教学重点：

- Plugin 和 Skill 的区别。
- MCP 和本地 Java 方法的区别。
- 为什么工具元数据会影响模型推理。

面试价值：

可以解释你的 Agent 如何扩展到初始代码之外。

## V13：安全、权限与护栏

目标：

给 Agent 行为加基础保护。

主题：

- API key 安全。
- Prompt injection。
- 工具权限。
- 高风险操作确认。
- 审计日志。
- rate limit / quota。

新增功能：

- 工具 allowlist。
- 工具风险等级。
- 高风险工具执行前确认。
- 审计表。

验收标准：

- 工具有风险等级。
- 高风险工具不能未经确认直接执行。
- 工具调用有日志。

你要亲手做的小任务：

- 给一个工具增加 risk level。

教学重点：

- Agent 安全和普通 API 安全有什么不同。
- 为什么不能让模型输出直接触发危险动作。
- Human-in-the-loop。

面试价值：

可以解释安全 Agent 执行和可审计性。

## V14：项目包装与简历表达

目标：

把项目整理成 GitHub 和简历可展示版本。

产出：

- 完整 README。
- 架构图。
- 功能列表。
- 截图或 API 示例。
- Roadmap 状态。
- 运行指南。
- 简历 bullet。
- 面试讲解稿。

README 应包含：

- 项目做什么。
- 为什么做。
- 架构。
- 技术栈。
- 如何运行。
- 当前状态。
- 后续计划。

简历示例：

```text
- 基于 Java 17 + Spring Boot 3 + MySQL 8 开发学习陪跑 Agent，按 Prompt Agent、Tool Agent、Skill Agent、流式产品 API 逐步演进，实现学习计划生成、练习批改、掌握度追踪和复习推荐。
- 设计 LLM 抽象层、结构化输出解析、工具注册中心和 MySQL 记忆模块，支持个性化学习计划、对话历史、练习记录和知识点掌握度持久化。
- 实现 Agent workflow 核心能力，包括 prompt 编排、tool calling、skill loading、SSE 流式输出、持久化记忆和安全护栏。
```

验收标准：

- GitHub 仓库公开。
- README 能清楚说明项目。
- 没有提交任何密钥。
- 项目可以在面试中讲清楚。

## 5. Git 提交计划

推荐提交节点：

```text
init project roadmap
add minimal prompt agent
add real llm client
add structured learning plan output
add spring boot chat api
add mysql memory
add tool registry and first tools
add skill loader
add quiz and mastery system
add sse streaming
add frontend mvp
add daily review scheduler
add plugin extension prototype
add security guardrails
polish readme and resume notes
```

每次 commit 应该小到一句话能讲清楚。

## 6. 以后如何继续

每次重新开始时：

1. 先打开这个文件。
2. 确认当前做到哪个版本。
3. 不跳步。
4. 从下一个未完成的小步骤继续。
5. 每完成一步，必要时更新本文件状态。

当前下一步建议：

```text
继续 V1：
先确保完全理解 AgentMessage、LlmClient、MockLlmClient、PromptAgent、StudyMentorCli。
然后进入 V2，把 MockLlmClient 替换成真实 LLM Client。
```

