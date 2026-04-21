package com.todo.service;

import com.todo.model.PendingTaskDTO;
import com.todo.model.Todo;
import com.todo.model.User;
import java.util.List;

public interface TodoService {
    Todo createTodo(Todo todo, User user);
    List<Todo> getTodosByUser(User user);
    Todo getTodoById(Long id, User user);
    Todo updateTodo(Long id, Todo todo, User user);
    void deleteTodo(Long id, User user);
    Todo toggleTodoStatus(Long id, User user);
    List<Todo> getCompletedTodos(User user);
    List<Todo> getPendingTodos(User user);
    List<Todo> getAllTodosByUser(User user);
    Todo postTaskToFriend(Todo todo, User sender);
    Todo acceptTask(Long taskId, User receiver);
    Todo rejectTask(Long taskId, User receiver);
    List<Todo> getPendingTasks(User user);
    List<PendingTaskDTO> getPendingTasksDTO(User user);
}