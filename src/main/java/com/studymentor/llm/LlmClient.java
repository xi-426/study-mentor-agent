package com.studymentor.llm;

import com.studymentor.agent.AgentMessage;

import java.util.List;

public interface LlmClient {

    String chat(List<AgentMessage> messages);
}
