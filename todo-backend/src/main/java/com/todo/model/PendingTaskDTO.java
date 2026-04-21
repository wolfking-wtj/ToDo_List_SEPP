package com.todo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingTaskDTO {
    private Long id;
    private String title;
    private String description;
    private String priority;
    private boolean completed;
    private boolean deleted;
    private Date createdAt;
    private Date dueDate;
    private Date completedAt;
    private Long targetUserId;
    private boolean accepted;

    private TaskSenderInfo sender;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSenderInfo {
        private Long id;
        private String username;
        private String name;
        private String avatar;
    }

    public static PendingTaskDTO fromTodo(Todo todo) {
        PendingTaskDTO dto = new PendingTaskDTO();
        dto.setId(todo.getId());
        dto.setTitle(todo.getTitle());
        dto.setDescription(todo.getDescription());
        dto.setPriority(todo.getPriority());
        dto.setCompleted(todo.isCompleted());
        dto.setDeleted(todo.isDeleted());
        dto.setCreatedAt(todo.getCreatedAt());
        dto.setDueDate(todo.getDueDate());
        dto.setCompletedAt(todo.getCompletedAt());
        dto.setTargetUserId(todo.getTargetUserId());
        dto.setAccepted(todo.isAccepted());

        if (todo.getUser() != null) {
            User user = todo.getUser();
            TaskSenderInfo sender = new TaskSenderInfo();
            sender.setId(user.getId());
            sender.setUsername(user.getUsername());
            sender.setName(user.getName());

            String avatar = user.getAvatar();
            if (avatar != null && avatar.startsWith("/uploads/avatars/")) {
                avatar = "/api/auth/avatar" + avatar.substring("/uploads/avatars".length());
            }
            sender.setAvatar(avatar);

            dto.setSender(sender);
        }

        return dto;
    }
}