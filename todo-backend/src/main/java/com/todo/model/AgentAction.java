package com.todo.model;

import lombok.Data;

@Data
public class AgentAction {
    private String type; // "get_tasks", "get_friends", "create_task", "complete_task", "delete_task", "add_friend_task", "update_task", "accept_task", "answer"
    private String title;
    private String newTitle;
    private String description;
    private String dueDate;
    private String priority;
    private Long id;
    private String friendName;
    private String answer;
}
