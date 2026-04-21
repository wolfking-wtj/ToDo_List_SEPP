package com.todo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import jakarta.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "todos")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(name = "description")
    private String description;
    
    @Column(nullable = false)
    private String priority;
    
    @Column(nullable = false)
    private boolean completed;
    
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted;
    
    @Column(nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    
    private Date dueDate;
    
    private Date completedAt;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"todos", "password"})
    private User user;
    
    @Column(name = "target_user_id", nullable = true)
    private Long targetUserId;
    
    @ManyToOne
    @JoinColumn(name = "target_user_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"todos", "password"})
    private User targetUser;
    
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean accepted = false;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}