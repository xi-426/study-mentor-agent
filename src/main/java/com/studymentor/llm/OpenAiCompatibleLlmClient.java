package com.studymentor.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studymentor.agent.AgentMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class OpenAiCompatibleLlmClient implements LlmClient {

    private final LlmConfig config;
    private final HttpClient httpClient;
    //处理JSON的对象映射器，用于将Java对象转换为JSON字符串，或将JSON字符串解析为Java对象
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(LlmConfig config) {
        config.validateForRealClient();
        this.config = config;
        //连不上模型服务器时，最多等 20 秒。
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        //创建 JSON 工具。
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String chat(List<AgentMessage> messages) {
        try {
            ChatCompletionRequest body = new ChatCompletionRequest(
                    config.model(),
                    messages,
                    config.temperature()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(chatCompletionsUri())
                    .timeout(Duration.ofSeconds(60))
                    //告诉服务器 我发的是JSON
                    .header("Content-Type", "application/json")
                    //设置鉴权头。 大多数 OpenAI-compatible API 都用：Authorization: Bearer API_KEY
                    .header("Authorization", "Bearer " + config.apiKey())
                    //把 ChatCompletionRequest 转成 JSON 字符串，然后作为 POST 请求体。
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("LLM request failed with status "
                        + response.statusCode() + ": " + response.body());
            }
            //把响应 JSON 转成 Java 对象。
            ChatCompletionResponse completion = objectMapper.readValue(response.body(), ChatCompletionResponse.class);
            if (completion.choices() == null || completion.choices().isEmpty()) {
                throw new IllegalStateException("LLM response has no choices: " + response.body());
            }

            AgentMessage message = completion.choices().get(0).message();
            if (message == null || message.content() == null || message.content().isBlank()) {
                throw new IllegalStateException("LLM response has empty assistant message: " + response.body());
            }
            return message.content();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read or write LLM JSON", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM request was interrupted", e);
        }
    }

    //如果 baseUrl 最后有 /，就去掉。
    private URI chatCompletionsUri() {
        String normalizedBaseUrl = config.baseUrl().endsWith("/")
                ? config.baseUrl().substring(0, config.baseUrl().length() - 1)
                : config.baseUrl();
        return URI.create(normalizedBaseUrl + "/chat/completions");
    }
}
