# V1：最小 Prompt Agent

> 当前阶段目标：先理解一个 Agent 最小闭环是什么。
>
> 暂时不接真实大模型、不接数据库、不接工具、不接 Spring Boot API。

## 1. V1 要解决什么问题

我们现在要先回答一个最基础的问题：

```text
一个 Agent 在代码里最小需要哪些部分？
```

V1 的答案是：

```text
用户输入
  -> PromptAgent 组织消息
  -> LlmClient 负责生成回复
  -> MockLlmClient 暂时代替真实大模型
  -> CLI 打印结果
```

对应结构：

```text
StudyMentorCli
    ↓
PromptAgent
    ↓
LlmClient
    ↑
MockLlmClient
```

再加一个消息对象：

```text
AgentMessage
```

## 2. 为什么先做 Prompt Agent

很多人一开始写 Agent，会直接这样写：

```text
main 方法里拼 prompt
main 方法里调模型
main 方法里处理返回
```

这能跑，但很快会乱。

因为后面我们会加入：

- 真实 LLM API。
- JSON 结构化输出。
- 工具调用。
- MySQL 记忆。
- Skill 文件。
- SSE 流式输出。
- 前端聊天页面。

如果一开始不拆清楚，后面每加一个功能都要重写。

所以 V1 先拆出三个核心抽象：

```text
PromptAgent：负责“怎么问模型”
LlmClient：负责“问哪个模型”
AgentMessage：负责“用什么消息格式问”
```

这是 Agent 工程的第一层地基。

## 3. 当前文件总览

V1 涉及 5 个文件：

```text
src/main/java/com/studymentor/agent/AgentMessage.java
src/main/java/com/studymentor/agent/PromptAgent.java
src/main/java/com/studymentor/llm/LlmClient.java
src/main/java/com/studymentor/llm/MockLlmClient.java
src/main/java/com/studymentor/cli/StudyMentorCli.java
```

它们的职责：

| 文件 | 角色 | 一句话解释 |
|---|---|---|
| `AgentMessage` | 消息格式 | 表示一条 system/user/assistant 消息 |
| `LlmClient` | 模型接口 | 规定所有模型客户端都要有 `chat` 能力 |
| `MockLlmClient` | 假模型 | 暂时返回固定内容，帮助验证流程 |
| `PromptAgent` | Agent 雏形 | 组织 prompt 和 messages，然后调用模型 |
| `StudyMentorCli` | 入口 | 从命令行接收输入，调用 Agent，打印结果 |

## 4. `AgentMessage` 是什么

路径：

```text
src/main/java/com/studymentor/agent/AgentMessage.java
```

代码核心：

```java
public record AgentMessage(String role, String content) {
}
```

一条消息由两部分组成：

```text
role：谁说的
content：说了什么
```

常见 role：

```text
system：系统规则，告诉模型它是谁、要怎么做
user：用户输入
assistant：模型回复
tool：工具结果，后面 Tool Agent 阶段再加
```

为什么不直接传字符串？

因为真实大模型聊天 API 通常接收的是：

```json
[
  {
    "role": "system",
    "content": "你是一个学习教练"
  },
  {
    "role": "user",
    "content": "我想 6 周学会 Java Agent 开发"
  }
]
```

所以 `AgentMessage` 是 Java 里的消息抽象。

面试怎么讲：

```text
我没有把 prompt 当成一整个字符串处理，而是抽象成带 role 的消息对象，这样后面可以自然支持多轮对话、工具结果和上下文记忆。
```

## 5. `LlmClient` 是什么

路径：

```text
src/main/java/com/studymentor/llm/LlmClient.java
```

代码：

```java
public interface LlmClient {
    String chat(List<AgentMessage> messages);
}
```

它不是某个具体模型，而是“模型客户端接口”。

意思是：

```text
只要你能接收 messages 并返回回复，你就可以作为一个 LlmClient。
```

后面可以有很多实现：

```text
MockLlmClient
OpenAiCompatibleLlmClient
DeepSeekLlmClient
QwenLlmClient
LocalModelLlmClient
```

为什么要这样设计？

因为 Agent 不应该绑定死某个模型厂商。

如果 `PromptAgent` 直接写死调用 OpenAI，那么以后换 DeepSeek 就要改 Agent 逻辑。

现在变成：

```text
PromptAgent 只认识 LlmClient
具体用哪个模型由外部传进来
```

这叫“依赖抽象，而不是依赖具体实现”。

面试怎么讲：

```text
我把大模型调用抽象成 LlmClient 接口，让 Agent 编排逻辑和模型供应商解耦。后续接入不同 OpenAI-compatible 模型时，只需要替换 LlmClient 实现，不需要改 PromptAgent。
```

## 6. `MockLlmClient` 是什么

路径：

```text
src/main/java/com/studymentor/llm/MockLlmClient.java
```

它实现了：

```java
public class MockLlmClient implements LlmClient
```

它的作用是假装自己是模型，但不真的请求网络。

为什么要先 mock？

因为 V1 的学习重点不是 API，而是 Agent 流程。

如果一开始就接真实模型，你会同时遇到：

- API key 怎么配。
- 请求 JSON 怎么写。
- HTTP 错误怎么处理。
- 网络失败怎么办。
- 模型返回格式怎么解析。

这些会掩盖 V1 真正要学的东西。

V1 真正要学的是：

```text
PromptAgent 如何组织 messages
PromptAgent 如何依赖 LlmClient
调用结果如何返回
```

所以我们先用 `MockLlmClient` 保证流程稳定。

面试怎么讲：

```text
早期我用 MockLlmClient 先验证 Agent 编排链路，避免一开始被真实 API 的鉴权和网络问题干扰。后续再把 mock 替换成真实 LLM client。
```

## 7. `PromptAgent` 是什么

路径：

```text
src/main/java/com/studymentor/agent/PromptAgent.java
```

这是当前真正的 Agent 雏形。

核心属性：

```java
private final LlmClient llmClient;
```

这说明：

```text
PromptAgent 自己不生成文本，它需要一个 LlmClient。
```

核心方法：

```java
public String createLearningPlan(String learningGoal)
```

它做了三件事：

```text
1. 准备 system prompt
2. 把用户目标包装成 user message
3. 调用 llmClient.chat(messages)
```

流程：

```text
learningGoal
  -> systemPrompt()
  -> AgentMessage.system(...)
  -> AgentMessage.user(...)
  -> List<AgentMessage>
  -> llmClient.chat(messages)
  -> answer
```

为什么它叫 Prompt Agent？

因为它目前只靠 prompt 控制模型行为，还没有工具、记忆和 skill。

这不是完整 Agent，但它已经具备 Agent 的第一层特征：

```text
它不只是转发用户输入，而是主动加入 system prompt，规定模型角色、任务和输出格式。
```

面试怎么讲：

```text
PromptAgent 是项目的第一层 Agent 编排器。它负责构造 system/user messages，约束模型以学习教练身份输出阶段化计划，同时通过 LlmClient 抽象调用模型。
```

## 8. `StudyMentorCli` 是什么

路径：

```text
src/main/java/com/studymentor/cli/StudyMentorCli.java
```

它是临时入口。

核心代码：

```java
PromptAgent agent = new PromptAgent(new MockLlmClient());
```

这句话把结构串起来：

```text
创建一个假模型 MockLlmClient
把它传给 PromptAgent
PromptAgent 用它生成回复
```

这叫依赖注入的最简单形式：

```text
PromptAgent 不自己 new 模型
外部把模型客户端传给它
```

为什么先用 CLI？

因为早期我们要快速验证 Agent 思路，不想被前端和 Spring Boot 分散注意力。

后面会升级成：

```text
Spring Boot Controller -> AgentService -> PromptAgent/ToolAgent
```

但 CLI 会作为学习和调试入口保留一段时间。

## 9. V1 完整运行流程

运行命令：

```powershell
java -cp target\classes com.studymentor.cli.StudyMentorCli "6周学会 Java Agent 开发"
```

内部发生了什么：

```text
1. StudyMentorCli 启动。
2. 创建 MockLlmClient。
3. 创建 PromptAgent，并传入 MockLlmClient。
4. 读取学习目标。
5. 调用 agent.createLearningPlan(goal)。
6. PromptAgent 创建 messages：
   - system：你是 StudyMentor...
   - user：我的学习目标是：6周学会 Java Agent 开发
7. PromptAgent 调用 llmClient.chat(messages)。
8. 当前实现是 MockLlmClient，所以返回固定模板。
9. CLI 打印结果。
```

图：

```text
用户输入
  ↓
StudyMentorCli
  ↓
PromptAgent.createLearningPlan
  ↓
List<AgentMessage>
  ↓
LlmClient.chat
  ↓
MockLlmClient.chat
  ↓
固定学习计划
  ↓
控制台输出
```

## 10. V1 的边界

V1 已经有：

- Agent message 抽象。
- LLM client 抽象。
- PromptAgent。
- Mock 模型。
- CLI 验证入口。

V1 还没有：

- 真实模型 API。
- JSON 结构化输出。
- 工具调用。
- 数据库记忆。
- Skill 加载。
- Spring Boot API。
- 前端。

这些都不能急着加，因为每个都是一个新概念。

## 11. V1 验收标准

完成 V1 需要满足：

- `PromptAgent` 依赖 `LlmClient` 接口。
- `MockLlmClient` 实现 `LlmClient`。
- `AgentMessage` 能表示 system/user/assistant 消息。
- CLI 能传入一个学习目标。
- 输出能展示学习计划。
- 你能说清楚每个文件是干什么的。

## 12. 你要亲手做的小任务

任务：修改 `PromptAgent` 的 system prompt。

路径：

```text
src/main/java/com/studymentor/agent/PromptAgent.java
```

在 `systemPrompt()` 里加一条规则：

```text
如果用户的目标太大，你要先拆成不超过 4 个阶段，不要一次列太多内容。
```

然后重新编译运行：

```powershell
javac -encoding UTF-8 -d target\classes src\main\java\com\studymentor\agent\AgentMessage.java src\main\java\com\studymentor\llm\LlmClient.java src\main\java\com\studymentor\llm\MockLlmClient.java src\main\java\com\studymentor\agent\PromptAgent.java src\main\java\com\studymentor\cli\StudyMentorCli.java

java -cp target\classes com.studymentor.cli.StudyMentorCli "6周学会 Java Agent 开发"
```

注意：

当前用的是 `MockLlmClient`，所以 prompt 改动不会真正影响输出。这个现象本身也很重要：

```text
Mock 只能验证代码链路，不能验证模型行为。
真实 prompt 效果要等 V2 接入真实 LLM 后才能看到。
```

## 13. V1 面试表达

你可以这样讲：

```text
项目第一阶段我先实现了最小 Prompt Agent，没有一开始就接复杂工具和数据库。
我把 Agent 编排逻辑、模型调用和消息结构拆开：
PromptAgent 负责组织 system/user messages；
LlmClient 抽象模型供应商；
MockLlmClient 用于早期验证流程；
AgentMessage 统一表示带 role 的消息。
这样后续接真实大模型、结构化输出、工具调用和数据库记忆时，不需要重写 Agent 主流程。
```

## 14. V1 和后续阶段的关系

V1 是地基。

后面每一步都在它上面加东西：

```text
V2：把 MockLlmClient 换成真实 LLM client。
V3：让 PromptAgent/StructuredAgent 输出 JSON。
V4：把 CLI 能力包装成 Spring Boot API。
V5：把对话和学习计划保存到 MySQL。
V6：让 Agent 调用工具。
V7：把教学规则抽成 Skill。
```

如果 V1 没搞懂，后面就会觉得所有东西都混在一起。

