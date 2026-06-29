package com.studymentor.agent;

public record AgentMessage(String role, String content) {

    public static AgentMessage system(String content) {
        return new AgentMessage("system", content);
    }

    public static AgentMessage user(String content) {
        return new AgentMessage("user", content);
    }

    public static AgentMessage assistant(String content) {
        return new AgentMessage("assistant", content);
    }
}
