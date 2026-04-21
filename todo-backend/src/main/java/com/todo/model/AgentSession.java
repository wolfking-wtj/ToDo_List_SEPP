package com.todo.model;

import lombok.Data;
import org.springframework.ai.chat.messages.Message;
import java.util.ArrayList;
import java.util.List;

@Data
public class AgentSession {
    private String userId;
    private List<Todo> currentTasks;
    private List<User> currentFriends;
    private String state;
    private Object pendingAction;
    private int step;
    private List<Message> chatHistory = new ArrayList<>();
}
