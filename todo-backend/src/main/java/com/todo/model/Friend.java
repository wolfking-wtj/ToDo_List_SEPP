package com.todo.model;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "friends")
public class Friend {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private Long friendId;
    
    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer status = 0;
    
    @Column
    private String createdAt;
}
