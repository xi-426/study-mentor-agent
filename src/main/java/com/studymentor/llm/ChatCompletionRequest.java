package com.studymentor.llm;

import com.studymentor.agent.AgentMessage;

import java.util.List;

public record ChatCompletionRequest(
        String model,
        List<AgentMessage> messages,
        double temperature
) {
}
