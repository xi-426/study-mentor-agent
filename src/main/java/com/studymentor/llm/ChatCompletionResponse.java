package com.studymentor.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.studymentor.agent.AgentMessage;

import java.util.List;

//JSON 里有我暂时不关心的字段，就忽略掉。
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(List<Choice> choices) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(AgentMessage message) {
    }
}
