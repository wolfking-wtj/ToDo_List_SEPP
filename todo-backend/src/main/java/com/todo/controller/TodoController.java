package com.todo.controller;

import com.todo.model.Todo;
import com.todo.model.User;
import com.todo.service.TodoService;
import com.todo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin
public class TodoController {
    @Autowired
    private TodoService todoService;
    
    @Autowired
    private UserService userService;
    
    private User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new RuntimeException("Invalid authentication principal");
        }
        
        UserDetails userDetails = (UserDetails) principal;
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }
    
    @PostMapping
    public ResponseEntity<?> createTodo(@RequestBody Todo todo) {
        try {
            User user = getCurrentUser();
            Todo createdTodo = todoService.createTodo(todo, user);
            return ResponseEntity.ok(createdTodo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create todo: " + e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<?> getTodos() {
        try {
            User user = getCurrentUser();
            List<Todo> todos = todoService.getTodosByUser(user);
            return ResponseEntity.ok(todos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get todos: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getTodoById(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            Todo todo = todoService.getTodoById(id, user);
            if (todo != null) {
                return ResponseEntity.ok(todo);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get todo: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTodo(@PathVariable Long id, @RequestBody Todo todo) {
        try {
            User user = getCurrentUser();
            Todo updatedTodo = todoService.updateTodo(id, todo, user);
            if (updatedTodo != null) {
                return ResponseEntity.ok(updatedTodo);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update todo: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTodo(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            todoService.deleteTodo(id, user);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete todo: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleTodoStatus(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            Todo updatedTodo = todoService.toggleTodoStatus(id, user);
            if (updatedTodo != null) {
                return ResponseEntity.ok(updatedTodo);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to toggle todo status: " + e.getMessage());
        }
    }
    
    @GetMapping("/completed")
    public ResponseEntity<?> getCompletedTodos() {
        try {
            User user = getCurrentUser();
            List<Todo> todos = todoService.getCompletedTodos(user);
            return ResponseEntity.ok(todos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get completed todos: " + e.getMessage());
        }
    }
    
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingTodos() {
        try {
            User user = getCurrentUser();
            List<Todo> todos = todoService.getPendingTodos(user);
            return ResponseEntity.ok(todos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get pending todos: " + e.getMessage());
        }
    }
    
    @GetMapping("/all")
    public ResponseEntity<?> getAllTodos() {
        try {
            User user = getCurrentUser();
            List<Todo> todos = todoService.getAllTodosByUser(user);
            return ResponseEntity.ok(todos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get all todos: " + e.getMessage());
        }
    }
    
    @PostMapping("/post")
    public ResponseEntity<?> postTaskToFriend(@RequestBody Todo todo) {
        try {
            User user = getCurrentUser();
            todo.setUser(user);
            if (todo.getTargetUserId() != null) {
                todo.setTargetUserId(todo.getTargetUserId());
            }
            Todo createdTodo = todoService.postTaskToFriend(todo, user);
            return ResponseEntity.ok(createdTodo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to post task: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/accept")
    public ResponseEntity<?> acceptTask(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            Todo updatedTodo = todoService.acceptTask(id, user);
            if (updatedTodo != null) {
                return ResponseEntity.ok(updatedTodo);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to accept task: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectTask(@PathVariable Long id) {
        try {
            User user = getCurrentUser();
            Todo updatedTodo = todoService.rejectTask(id, user);
            if (updatedTodo != null) {
                return ResponseEntity.ok(updatedTodo);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to reject task: " + e.getMessage());
        }
    }
    
    @GetMapping("/pending-tasks")
    public ResponseEntity<?> getPendingTasks() {
        try {
            User user = getCurrentUser();
            var tasks = todoService.getPendingTasksDTO(user);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get pending tasks: " + e.getMessage());
        }
    }
}