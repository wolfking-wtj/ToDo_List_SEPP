package com.todo.model;

import lombok.Data;

@Data
public class AgentChatResponse {
    private String response;
    private String thinkContent;

    public AgentChatResponse(String response, String thinkContent) {
        this.response = response;
        this.thinkContent = thinkContent;
    }
}
