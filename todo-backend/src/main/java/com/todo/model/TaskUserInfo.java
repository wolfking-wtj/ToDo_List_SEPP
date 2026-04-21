package com.todo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskUserInfo {
    private Long id;
    private String username;
    private String name;
    private String avatar;

    @JsonIgnoreProperties({"todos", "password", "email"})
    private User user;

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.name = user.getName();
            String avatar = user.getAvatar();
            if (avatar != null && avatar.startsWith("/uploads/avatars/")) {
                avatar = "/api/auth/avatar" + avatar.substring("/uploads/avatars".length());
            }
            this.avatar = avatar;
        }
    }
}