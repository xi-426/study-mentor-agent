# StudyMentor Agent 调试复盘总表

> 本文记录 StudyMentor Agent 所有版本中遇到的错误、原因、修改过程和最终结果。
>
> 这些内容很重要，因为 Agent 项目不只是“能调模型”，还要处理配置、启动方式、环境变量、外部 API 响应结构、编码等工程问题。

## 记录规则

以后所有版本的错误都只写在这个文件里，不再为每个版本单独创建 debug 文档。

每个问题按这个格式记录：

```text
问题现象
原因分析
修改过程
涉及代码
最终结果
学到的工程点
面试表达
```

当前已记录：

```text
V2：真实 LLM Client 接入过程中的调试问题
```

## V2：真实 LLM Client 接入调试复盘

### 1. 背景

V1 阶段项目只有：

```text
PromptAgent -> LlmClient -> MockLlmClient
```

`MockLlmClient` 不请求网络，只返回固定模板。

V2 的目标是新增真实模型调用能力：

```text
PromptAgent -> LlmClient -> OpenAiCompatibleLlmClient -> DeepSeek API
```

关键设计要求：

- `PromptAgent` 不应该改。
- 真实模型调用应该作为 `LlmClient` 的一个新实现。
- 默认仍然可以用 Mock，避免没有 API key 时项目无法运行。
- 真实模型通过 `--real` 开关启用。

### 2. 当前 V2 新增的核心类

#### 2.1 `LlmConfig`

路径：

```text
src/main/java/com/studymentor/llm/LlmConfig.java
```

职责：

- 读取 LLM 配置。
- 校验真实模型模式下必填配置。
- 支持从系统环境变量读取。
- 后来增加了从 `.env` 文件读取。

核心配置：

```text
LLM_BASE_URL
LLM_API_KEY
LLM_MODEL
LLM_TEMPERATURE
```

#### 2.2 `ChatCompletionRequest`

路径：

```text
src/main/java/com/studymentor/llm/ChatCompletionRequest.java
```

职责：

表示发给 OpenAI-compatible 模型接口的请求体。

字段：

```java
String model
List<AgentMessage> messages
double temperature
```

它会被 Jackson 转成 JSON：

```json
{
  "model": "deepseek-chat",
  "messages": [
    {"role": "system", "content": "..."},
    {"role": "user", "content": "..."}
  ],
  "temperature": 0.3
}
```

#### 2.3 `ChatCompletionResponse`

路径：

```text
src/main/java/com/studymentor/llm/ChatCompletionResponse.java
```

职责：

表示模型返回结果中当前需要的部分。

当前只关心：

```text
choices[0].message.content
```

#### 2.4 `OpenAiCompatibleLlmClient`

路径：

```text
src/main/java/com/studymentor/llm/OpenAiCompatibleLlmClient.java
```

职责：

- 构造 HTTP 请求。
- 把 Java 请求对象转成 JSON。
- 调用 `{baseUrl}/chat/completions`。
- 解析响应 JSON。
- 返回 assistant 的文本内容。

关键代码：

```java
HttpRequest request = HttpRequest.newBuilder()
        .uri(chatCompletionsUri())
        .timeout(Duration.ofSeconds(60))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + config.apiKey())
        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
        .build();
```

含义：

```text
ChatCompletionRequest Java 对象
  -> objectMapper.writeValueAsString(body)
  -> JSON 字符串
  -> HttpRequest.BodyPublishers.ofString(...)
  -> HTTP POST 请求体
  -> 发给 DeepSeek
```

### 3. 问题一：Spring Boot 启动时报 MySQL 连接错误

#### 3.1 现象

运行 `StudyMentorApplication` 时报错：

```text
org.hibernate.exception.GenericJDBCException: unable to obtain isolated JDBC connection
Access denied for user 'root'@'localhost' (using password: NO)
```

#### 3.2 原因

最开始的 `application.yml` 中直接配置了 MySQL：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/study_mentor
    username: root
    password: ${MYSQL_PASSWORD:}
```

同时 `pom.xml` 里已经引入了：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

Spring Boot 检测到 JPA 和 datasource 配置后，会在启动时自动初始化数据库连接。

但当前项目还在 V1/V2 阶段，尚未进入：

```text
V5：MySQL 记忆
```

因此默认启动不应该强制连接 MySQL。

报错中的：

```text
using password: NO
```

表示程序正在用 `root` + 空密码连接本地 MySQL，导致被拒绝。

#### 3.3 修改过程

把默认配置改成不启用数据库/JPA：

路径：

```text
src/main/resources/application.yml
```

修改后默认配置：

```yaml
spring:
  application:
    name: study-mentor-agent
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
```

同时新增 MySQL 专用 profile：

路径：

```text
src/main/resources/application-mysql.yml
```

内容：

```yaml
spring:
  autoconfigure:
    exclude: []
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:study_mentor}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD}
```

#### 3.4 结果

默认启动：

```text
不连接 MySQL
不初始化 JPA
Spring Boot 可以正常启动
```

以后进入 V5 时，再通过 profile 启用：

```text
--spring.profiles.active=mysql
```

#### 3.5 学到的工程点

早期项目可以先声明依赖，但不要让未完成阶段的配置阻塞当前阶段运行。

对 Agent 项目来说也一样：

```text
V1/V2 重点是 LLM 调用链路
数据库记忆应该等 V5 再启用
```

### 4. 问题二：`--real` 不生效，仍然走 Mock

#### 4.1 现象

在程序提示输入后输入：

```text
--real 6周学会 Java Agent 开发
```

结果仍然返回 `MockLlmClient` 的固定模板。

#### 4.2 原因

最初代码只在 `main(String[] args)` 里检查 `--real`：

```java
List<String> arguments = Arrays.asList(args);
boolean useRealLlm = arguments.contains("--real");
```

这表示只有“启动参数”里的 `--real` 才会被识别。

但用户是在程序启动后的输入框中输入：

```text
> --real 6周学会 Java Agent 开发
```

这部分内容最初没有被当成参数列表重新解析。

#### 4.3 修改过程

修改 `StudyMentorCli`：

路径：

```text
src/main/java/com/studymentor/cli/StudyMentorCli.java
```

让它支持两种方式：

方式一：启动参数

```text
--real 6周学会 Java Agent 开发
```

方式二：运行后输入

```text
> --real 6周学会 Java Agent 开发
```

关键逻辑：

```java
if (args.length > 0) {
    arguments = Arrays.asList(args);
} else {
    arguments = splitInput(readLine());
}

boolean useRealLlm = arguments.contains("--real");
```

#### 4.4 后续再次调整

后来用户希望“自己输入学习目标”，不希望 VS Code 启动配置直接预填目标。

于是将 `.vscode/launch.json` 调整为：

```json
{
  "name": "StudyMentor CLI - Real LLM",
  "args": "--real"
}
```

这代表：

```text
启动时只指定模式为 real
学习目标仍然由用户在控制台输入
```

为了支持这种模式，`StudyMentorCli` 又增加了逻辑：

```java
if (goal.isBlank()) {
    System.out.println("请输入你的学习目标，例如：6周学会 Java Agent 开发");
    System.out.print("> ");
    goal = readLine().trim();
}
```

#### 4.5 结果

现在支持：

```text
StudyMentor CLI - Mock
  -> 用户输入目标
  -> 走 MockLlmClient

StudyMentor CLI - Real LLM
  -> 启动参数含 --real
  -> 用户输入目标
  -> 走 OpenAiCompatibleLlmClient
```

#### 4.6 学到的工程点

CLI 程序里：

```text
启动参数 args
运行后标准输入 Scanner/readLine
```

是两套不同输入来源。

如果希望两种方式都支持，就要显式统一解析。

### 5. 问题三：环境变量读不到，报 `LLM_BASE_URL is required`

#### 5.1 现象

输入：

```text
--real 4周学会 Java Agent 开发
```

报错：

```text
Exception in thread "main" java.lang.IllegalStateException: LLM_BASE_URL is required when using --real
```

#### 5.2 原因

`LlmConfig` 最初只从系统环境变量读取：

```java
System.getenv("LLM_BASE_URL")
```

但用户可能把配置写在：

```text
.vscode/launch.json
```

或以为 `.env.example` 会自动生效。

实际上：

- `.env.example` 只是模板，不会被程序读取。
- VS Code 某个 launch 配置里的 env，只对该配置启动的进程生效。
- 如果用户用普通 Run 或终端方式运行，launch 配置里的 env 不一定注入。

所以 Java 进程中读不到：

```text
LLM_BASE_URL
LLM_API_KEY
LLM_MODEL
```

#### 5.3 修改过程

新增真实本地配置文件：

路径：

```text
.env
```

内容：

```text
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_API_KEY=replace_me
LLM_MODEL=deepseek-chat
LLM_TEMPERATURE=0.3
```

`.env` 已经在 `.gitignore` 中：

```text
.env
```

不会提交到 Git。

同时修改 `LlmConfig`，让它读取顺序变成：

```text
1. 系统环境变量
2. 项目根目录 .env
3. 默认值
```

关键代码：

```java
private static String readConfig(String name, String defaultValue, Map<String, String> dotEnv) {
    String value = System.getenv(name);
    if (value != null && !value.isBlank()) {
        return value;
    }
    value = dotEnv.get(name);
    return value == null || value.isBlank() ? defaultValue : value;
}
```

读取 `.env` 的代码：

```java
private static Map<String, String> readDotEnv() {
    Path path = Path.of(".env");
    if (!Files.exists(path)) {
        return Map.of();
    }

    Map<String, String> values = new HashMap<>();
    for (String line : Files.readAllLines(path)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            continue;
        }
        int equalsIndex = trimmed.indexOf('=');
        if (equalsIndex <= 0) {
            continue;
        }
        String key = trimmed.substring(0, equalsIndex).trim();
        String value = trimmed.substring(equalsIndex + 1).trim();
        values.put(key, stripQuotes(value));
    }
    return values;
}
```

#### 5.4 结果

现在即使不用 VS Code 的 Real LLM launch 配置，只要项目根目录存在 `.env`，程序也能读取 DeepSeek 配置。

#### 5.5 `.env` 和 `.env.example` 区别

`.env`：

```text
真实本地配置
可以放真实 API key
不能提交 Git
```

`.env.example`：

```text
示例模板
告诉别人需要哪些配置
可以提交 Git
不能放真实 API key
```

### 6. 问题四：DeepSeek 响应解析失败，`Unrecognized field "index"`

#### 6.1 现象

真实调用 DeepSeek 后报错：

```text
Failed to read or write LLM JSON
Unrecognized field "index" (class ChatCompletionResponse$Choice)
```

关键错误：

```text
Unrecognized field "index"
```

#### 6.2 原因

最初的响应 DTO 只写了当前需要的字段：

```java
public record ChatCompletionResponse(List<Choice> choices) {
    public record Choice(AgentMessage message) {
    }
}
```

但 DeepSeek 返回的 `choice` 里不只有 `message`，还会有：

```json
{
  "index": 0,
  "message": {
    "role": "assistant",
    "content": "..."
  },
  "finish_reason": "stop"
}
```

Jackson 默认比较严格。

当 Java record 中没有 `index` 字段，而 JSON 里出现 `index`，就会报：

```text
UnrecognizedPropertyException
```

#### 6.3 修改过程

修改：

```text
src/main/java/com/studymentor/llm/ChatCompletionResponse.java
```

添加：

```java
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
```

并加注解：

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(AgentMessage message) {
    }
}
```

#### 6.4 结果

DeepSeek 返回的额外字段：

```text
index
finish_reason
usage
id
created
```

如果当前 DTO 没有定义，会被忽略。

程序只取：

```java
completion.choices().get(0).message().content()
```

#### 6.5 学到的工程点

接外部 API 时，DTO 不能过于脆弱。

我们当前阶段只需要：

```text
choices.message.content
```

所以其他字段先忽略。

未来如果要统计 token，可以再加：

```text
usage.prompt_tokens
usage.completion_tokens
usage.total_tokens
```

### 7. 问题五：中文输入或输出乱码

#### 7.1 现象

控制台出现：

```text
璇疯緭鍏ヤ綘鐨勫涔犵洰鏍...
```

或模型回复中提到：

```text
我注意到你的目标描述中出现了乱码（“4?????”）
```

#### 7.2 原因

这是 Windows + VS Code + PowerShell + Java 控制台编码不一致导致的。

可能情况：

- Java 按 UTF-8 输出。
- PowerShell 按 GBK/系统代码页显示。
- VS Code 普通 Java Run 没有走 `launch.json` 中的 `vmArgs`。
- 中文输入在进入 Java 程序前已经损坏。

因此模型看到的用户目标可能已经变成：

```text
4????? Java Agent ????
```

DeepSeek 只是根据坏掉的输入做了合理推测。

#### 7.3 尝试过的修改

先尝试在 VS Code 配置中强制 UTF-8：

```json
"vmArgs": "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8"
```

又尝试添加：

```json
"java.debug.settings.vmArgs": "-Dfile.encoding=UTF-8 ..."
```

但在当前 PowerShell 环境下，强制 UTF-8 反而可能和终端显示编码冲突，导致提示文本继续乱码。

后来撤销了这些强制 UTF-8 配置。

#### 7.4 当前采取的临时方案

在 `StudyMentorCli` 中增加一个 `demo` 输入：

```java
if ("demo".equalsIgnoreCase(goal)) {
    goal = "4周学会 Java Agent 开发";
}
```

这样用户可以输入：

```text
demo
```

程序内部会替换成：

```text
4周学会 Java Agent 开发
```

避免终端中文输入阶段损坏。

#### 7.5 结果

真实 LLM 链路可以继续验证：

```text
CLI -> PromptAgent -> OpenAiCompatibleLlmClient -> DeepSeek -> JSON 解析 -> 输出
```

即使终端中文显示暂时不稳定，也不阻塞 V2 主线。

#### 7.6 后续更好的解决方式

后面进入 Spring Boot API 后，CLI 不再是主要入口。

届时请求会通过：

```text
HTTP JSON
```

传入，比如：

```json
{
  "goal": "4周学会 Java Agent 开发"
}
```

编码会比 Windows 控制台稳定很多。

所以当前阶段不继续在 CLI 编码问题上消耗太多时间。

### 8. 当前最终运行方式

#### 8.1 Mock 模式

运行：

```text
StudyMentor CLI - Mock
```

输入：

```text
demo
```

结果：

```text
走 MockLlmClient
返回固定模板
```

#### 8.2 DeepSeek 真实模式

确保 `.env` 中有：

```text
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_API_KEY=你的真实 key
LLM_MODEL=deepseek-chat
LLM_TEMPERATURE=0.3
```

运行：

```text
StudyMentor CLI - Real LLM
```

输入：

```text
demo
```

结果：

```text
走 OpenAiCompatibleLlmClient
调用 DeepSeek
解析 choices[0].message.content
输出真实模型回答
```

### 9. 现在 V2 的完整流程

```text
用户运行 StudyMentor CLI - Real LLM
  -> launch.json 传入 --real
  -> StudyMentorCli 读取用户输入
  -> 如果输入 demo，替换为 4周学会 Java Agent 开发
  -> 判断 useRealLlm = true
  -> LlmConfig.fromEnv()
       先读系统环境变量
       再读 .env
  -> new OpenAiCompatibleLlmClient(config)
       校验 baseUrl/apiKey/model
       创建 HttpClient
       创建 ObjectMapper
  -> new PromptAgent(llmClient)
  -> PromptAgent.createLearningPlan(goal)
       构造 system message
       构造 user message
       调用 llmClient.chat(messages)
  -> OpenAiCompatibleLlmClient.chat(messages)
       构造 ChatCompletionRequest
       Java 对象转 JSON
       POST {baseUrl}/chat/completions
       读取 HTTP response
       JSON 转 ChatCompletionResponse
       取 choices[0].message.content
  -> 返回给 PromptAgent
  -> 返回给 CLI
  -> 控制台输出
```

### 10. 这段经历的面试表达

可以这样讲：

```text
在真实 LLM Client 接入时，我没有直接把 HTTP 调用写进 Agent，而是通过 LlmClient 接口新增 OpenAiCompatibleLlmClient，实现 Agent 编排逻辑和模型供应商解耦。

接入 DeepSeek 过程中处理了几个工程问题：默认配置不应强制连接 MySQL，因此把数据库配置移到 mysql profile；真实模型配置不能依赖单一 VS Code launch env，因此增加 .env 读取；DeepSeek 返回的 choice 包含 index、finish_reason 等额外字段，因此通过 JsonIgnoreProperties 忽略未知字段；Windows 终端中文编码不稳定，临时增加 demo 输入绕过 CLI 编码问题，后续会通过 HTTP API 替代 CLI 入口。
```

这比单纯说“我会调 API”更有含金量，因为它体现了：

- 配置隔离。
- 接口抽象。
- 外部 API 兼容。
- 错误定位。
- 本地开发环境处理。
- Agent 项目分阶段演进。
