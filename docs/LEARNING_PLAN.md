# StudyMentor Agent 学习路线图

## V0：项目骨架

目标：

- 创建 Java 17 项目结构。
- 配置 Maven、Spring Boot、MySQL 依赖。
- 明确后续目录分层。

你要学：

- Java 项目目录。
- Maven 是什么。
- `pom.xml` 是什么。
- Spring Boot 项目入口是什么。
- `application.yml` 是什么。

## V1：Prompt Agent

目标：

- 写一个命令行 Agent。
- 输入学习目标，输出学习计划。
- 当前先用 `MockLlmClient` 模拟大模型回复。

你要学：

- `main` 方法。
- `interface`。
- `record`。
- system prompt。
- user prompt。
- Agent 最小闭环。

## V2：真实 LLM Client

目标：

- 把 `MockLlmClient` 换成真实 API 调用。

你要学：

- HTTP 请求。
- API key。
- JSON 请求体。
- 模型返回值解析。
- 错误处理。

## V3：结构化输出

目标：

- 让 Agent 输出稳定学习计划 JSON。

你要学：

- JSON schema。
- Jackson。
- DTO。
- 校验。
- 为什么结构化输出对产品很重要。

## V4：Spring Boot API

目标：

- 把命令行 Agent 包成 HTTP API。

你要学：

- Controller。
- Service。
- Request/Response。
- REST API。

## V5：MySQL 持久化

目标：

- 保存学习目标、任务和聊天记录。

你要学：

- 表。
- 主键。
- 一对多关系。
- JPA Entity。
- Repository。
- 事务。

## V6：Tool Agent

目标：

- Agent 可以调用 Java 工具读写数据库。

你要学：

- Tool calling。
- 工具注册。
- 参数解析。
- 工具结果。
- 工具失败处理。

## V7：Skill Agent

目标：

- 把教学规则写成 `SKILL.md`。

你要学：

- Skill 和 prompt 的区别。
- 业务流程规则。
- 输出约束。

## V8：前端和流式输出

目标：

- 做一个可以真实使用的网页学习陪跑工具。

你要学：

- 前后端通信。
- Chat UI。
- SSE。
- 生成中状态。
- 错误状态。

