# StudyMentor Agent

Java 17 + Spring Boot 3 + MySQL 8 学习陪跑 Agent。

项目长期路线图和学习协议见：`docs/PROJECT_ROADMAP.md`。后续开发以该文件为准，按版本一步一步推进。

当前阶段说明见：`docs/V1_PROMPT_AGENT.md`。

当前默认启动不连接数据库。MySQL 配置放在 `src/main/resources/application-mysql.yml`，等进入 V5：MySQL 记忆阶段时，再通过 `mysql` profile 启用。

这个项目不是一次写完的 Demo，而是按 Agent 能力一层一层长出来：

1. Prompt Agent：会根据学习目标给出计划和讲解。
2. Structured Agent：输出稳定 JSON，方便前端和数据库使用。
3. Tool Agent：能调用 Java 工具读写学习目标、任务、练习和进度。
4. Skill Agent：把教学流程、出题规则、复习规则写成 `SKILL.md`。
5. Product Agent：Spring Boot API + MySQL + 前端 + 流式输出。

## 当前版本

V0/V1 骨架版：

- 标准 Maven 项目结构。
- Java 17。
- Spring Boot 3 依赖已在 `pom.xml` 中声明。
- MySQL 配置模板已放在 `src/main/resources/application.yml`。
- 最小 Prompt Agent 代码已放入 `src/main/java/com/studymentor`。
- 当前 `MockLlmClient` 不调用真实模型，用来学习 Agent 的第一层流程。

## 下一步

1. 安装 Maven 或在 IDE 中导入 Maven 项目。
2. 创建 MySQL 数据库 `study_mentor`。
3. 把 Mock LLM 换成真实 LLM API。
4. 进入 V2：结构化学习计划 JSON。

## 项目运行思路

早期先跑命令行 Agent，理解最小闭环：

```text
用户输入学习目标
  -> PromptAgent 组织 system prompt 和 user prompt
  -> LlmClient 生成回复
  -> CLI 打印学习计划
```

后期升级成 Web 产品：

```text
前端页面
  -> Spring Boot Controller
  -> Agent Service
  -> Tool / Skill / LLM
  -> MySQL 保存状态
```
