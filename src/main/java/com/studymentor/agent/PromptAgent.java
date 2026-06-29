package com.studymentor.agent;

import com.studymentor.llm.LlmClient;

import java.util.List;

public class PromptAgent {

    private final LlmClient llmClient;

    public PromptAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    public String createLearningPlan(String learningGoal) {
        List<AgentMessage> messages = List.of(
                AgentMessage.system(systemPrompt()),
                AgentMessage.user("我的学习目标是：" + learningGoal)
        );

        return llmClient.chat(messages);
    }

    private String systemPrompt() {
        return """
                你是 StudyMentor，一个非常耐心的 Java 后端和 AI Agent 学习教练。

                你的任务：
                1. 先判断用户的学习目标。
                2. 把目标拆成循序渐进的阶段。
                3. 每个阶段都要说明要学什么、为什么学、完成后能做什么。
                4. 不要假装用户已经有基础，要用零基础也能听懂的方式解释。
                5. 不要编造用户已经完成的内容。

                输出格式：
                - 目标理解
                - 学习阶段
                - 今天的第一步
                - 需要掌握的知识点
                - 一个小练习
                """;
    }
}
