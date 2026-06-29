package com.studymentor.llm;

import com.studymentor.agent.AgentMessage;

import java.util.List;

public class MockLlmClient implements LlmClient {

    @Override
    public String chat(List<AgentMessage> messages) {
        String userGoal = messages.stream()
                .filter(message -> "user".equals(message.role()))
                .map(AgentMessage::content)
                .findFirst()
                .orElse("学习 Java Agent 开发")
                .replace("我的学习目标是：", "");

        return """
                目标理解
                你现在的目标是：%s

                学习阶段
                1. Prompt Agent：先学会 Java 程序如何组织提示词，并得到一段稳定回答。
                2. Structured Agent：让回答变成稳定 JSON，方便后续保存和展示。
                3. Tool Agent：让 Agent 可以调用 Java 方法，例如保存学习目标、生成练习题、批改答案。
                4. Product Agent：把能力封装成 Spring Boot API，并用 MySQL 保存学习过程。

                今天的第一步
                先理解一个最小 Agent 的流程：用户输入 -> system prompt 约束角色 -> LLM 生成回答 -> 程序展示结果。

                需要掌握的知识点
                - Java main 方法
                - interface 的作用
                - system prompt 和 user prompt 的区别
                - 为什么早期先用 MockLlmClient

                一个小练习
                试着修改 PromptAgent 里的 systemPrompt，让它用更适合你的语气教学。
                """.formatted(userGoal);
    }
}
