# V2：真实 LLM Client

> 当前阶段目标：把 `MockLlmClient` 替换成可选的真实大模型客户端。
>
> 注意：默认仍然使用 Mock，不会强制你配置 API key。

## 1. V2 要解决什么问题

V1 里我们已经有了这个结构：

```text
PromptAgent -> LlmClient -> MockLlmClient
```

但 `MockLlmClient` 只是固定返回文本，不能验证真实 prompt 效果。

V2 要把结构升级成：

```text
PromptAgent -> LlmClient -> OpenAiCompatibleLlmClient -> 真实大模型
```

关键点：

```text
PromptAgent 不改。
只新增一个 LlmClient 的真实实现。
```

这说明 V1 的接口抽象是有价值的。

## 2. OpenAI-compatible 是什么意思

很多模型供应商虽然不是 OpenAI，但会兼容 OpenAI 的 Chat Completions API 格式。

典型请求：

```http
POST {baseUrl}/chat/completions
Authorization: Bearer {apiKey}
Content-Type: application/json
```

请求体：

```json
{
  "model": "model-name",
  "messages": [
    {"role": "system", "content": "你是一个学习教练"},
    {"role": "user", "content": "我想学 Agent"}
  ],
  "temperature": 0.3
}
```

只要供应商支持类似格式，我们就可以用同一个 `OpenAiCompatibleLlmClient`。

## 3. 新增文件

```text
src/main/java/com/studymentor/llm/LlmConfig.java
src/main/java/com/studymentor/llm/ChatCompletionRequest.java
src/main/java/com/studymentor/llm/ChatCompletionResponse.java
src/main/java/com/studymentor/llm/OpenAiCompatibleLlmClient.java
```

## 4. 每个文件做什么

### `LlmConfig`

从环境变量读取模型配置：

```text
LLM_BASE_URL
LLM_API_KEY
LLM_MODEL
LLM_TEMPERATURE
```

它还会在使用真实模型时检查配置是否齐全。

### `ChatCompletionRequest`

表示发给模型的 JSON 请求体。

包括：

```text
model
messages
temperature
```

### `ChatCompletionResponse`

表示模型返回的 JSON 结构。

当前只取第一条 choice 的 assistant message。

### `OpenAiCompatibleLlmClient`

真正发 HTTP 请求。

它负责：

- 把 Java 对象序列化成 JSON。
- 带上 Authorization header。
- 调用 `/chat/completions`。
- 检查 HTTP 状态码。
- 解析模型返回。
- 返回 assistant 文本。

## 5. CLI 如何切换 Mock 和真实模型

默认：

```powershell
java -cp target\classes com.studymentor.cli.StudyMentorCli "6周学会 Java Agent 开发"
```

使用：

```text
MockLlmClient
```

真实模型：

```powershell
java -cp target\classes com.studymentor.cli.StudyMentorCli --real "6周学会 Java Agent 开发"
```

使用：

```text
OpenAiCompatibleLlmClient
```

但运行真实模型前，必须配置环境变量：

```text
LLM_BASE_URL
LLM_API_KEY
LLM_MODEL
```

## 6. 为什么默认不用真实模型

因为真实模型会引入额外变量：

- API key 是否正确。
- 网络是否可用。
- base URL 是否写对。
- 模型名是否可用。
- 余额或权限是否足够。

如果默认就用真实模型，很容易让“Agent 结构学习”被环境问题打断。

所以当前设计是：

```text
默认 Mock，保证项目随时可跑。
加 --real，才调用真实模型。
```

## 7. V2 验收标准

- `PromptAgent` 不需要修改也能支持真实模型。
- CLI 默认仍然能跑 Mock。
- CLI 加 `--real` 时会使用真实模型客户端。
- 缺少 API key 时能给出明确错误。
- API key 不写进代码，也不提交 Git。

## 8. 你要亲手做的小任务

任务 1：

运行默认 Mock，确认不需要 API key 也能跑。

任务 2：

配置你自己的模型环境变量，然后尝试运行：

```powershell
java -cp target\classes com.studymentor.cli.StudyMentorCli --real "我想学习 Java Agent 开发"
```

任务 3：

改 `LLM_TEMPERATURE`，观察输出风格是否变化。

## 9. 面试怎么讲

```text
在 V2 中，我把真实大模型接入封装成 OpenAiCompatibleLlmClient，并继续通过 LlmClient 接口暴露给 PromptAgent。
这样 PromptAgent 不需要关心底层模型供应商，默认可以使用 Mock 做本地验证，需要真实推理时再通过环境变量切换到真实模型。
这体现了 Agent 编排逻辑和模型调用层的解耦。
```

