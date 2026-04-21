package com.todo.model;

import lombok.Data;
import jakarta.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column
    private String name;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Todo> todos;
    
    @Column(nullable = false, columnDefinition = "int default 1")
    private Integer level = 1;
    
    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer points = 0;
    
    @Column
    private String avatar;
}