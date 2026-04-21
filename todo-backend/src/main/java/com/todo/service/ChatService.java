package com.todo.service;

import com.todo.model.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一个友好的AI助手，名字叫小助手。你可以帮助用户管理待办事项、回答问题，或者只是聊聊天。请用友好、简洁的方式回复。如果你不知道答案，就诚实地说不知道。")
                .build();
    }

    public ChatResponse getResponse(String userMessage) {
        String response = chatClient.prompt()
                .user(userMessage)
                .call()
                .content();

        return new ChatResponse(response);
    }
}