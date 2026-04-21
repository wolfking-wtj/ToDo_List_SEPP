package com.todo.model;

import lombok.Data;

@Data
public class ChatResponse {
    private String response;

    public ChatResponse(String response) {
        this.response = response;
    }
}