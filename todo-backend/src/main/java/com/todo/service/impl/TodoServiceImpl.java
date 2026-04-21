package com.todo.service.impl;

import com.todo.dao.TodoRepository;
import com.todo.model.PendingTaskDTO;
import com.todo.model.Todo;
import com.todo.model.User;
import com.todo.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TodoServiceImpl implements TodoService {
    @Autowired
    private TodoRepository todoRepository;
    
    @Override
    public Todo createTodo(Todo todo, User user) {
        todo.setUser(user);
        todo.setCreatedAt(new Date());
        todo.setCompleted(false);
        return todoRepository.save(todo);
    }
    
    @Override
    public List<Todo> getTodosByUser(User user) {
        // 只返回已接受的任务或用户自己创建的任务（非分享任务）
        return todoRepository.findByUserAndDeleted(user, false)
                .stream()
                .filter(todo -> todo.isAccepted() || todo.getTargetUserId() == null)
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public Todo getTodoById(Long id, User user) {
        Todo todo = todoRepository.findById(id).orElse(null);
        if (todo != null && todo.getUser().getId().equals(user.getId())) {
            return todo;
        }
        return null;
    }
    
    @Override
    public Todo updateTodo(Long id, Todo todo, User user) {
        Todo existingTodo = getTodoById(id, user);
        if (existingTodo != null) {
            existingTodo.setTitle(todo.getTitle());
            existingTodo.setDescription(todo.getDescription());
            existingTodo.setPriority(todo.getPriority());
            existingTodo.setDueDate(todo.getDueDate());
            return todoRepository.save(existingTodo);
        }
        return null;
    }
    
    @Override
    public void deleteTodo(Long id, User user) {
        Todo todo = getTodoById(id, user);
        if (todo != null) {
            todo.setDeleted(true);
            todoRepository.save(todo);
        }
    }
    
    @Override
    public Todo toggleTodoStatus(Long id, User user) {
        Todo todo = getTodoById(id, user);
        if (todo != null) {
            todo.setCompleted(!todo.isCompleted());
            todo.setCompletedAt(todo.isCompleted() ? new Date() : null);
            return todoRepository.save(todo);
        }
        return null;
    }
    
    @Override
    public List<Todo> getCompletedTodos(User user) {
        return todoRepository.findByUserAndCompletedAndNotDeleted(user, true);
    }
    
    @Override
    public List<Todo> getPendingTodos(User user) {
        return todoRepository.findByUserAndCompletedAndNotDeleted(user, false);
    }
    
    @Override
    public List<Todo> getAllTodosByUser(User user) {
        return todoRepository.findByUser(user);
    }
    
    @Override
    public Todo postTaskToFriend(Todo todo, User sender) {
        todo.setUser(sender);
        if (todo.getTargetUserId() != null) {
            todo.setTargetUserId(todo.getTargetUserId());
        }
        todo.setAccepted(false);
        todo.setCreatedAt(new Date());
        todo.setCompleted(false);
        return todoRepository.save(todo);
    }
    
    @Override
    public Todo acceptTask(Long taskId, User receiver) {
        Todo todo = todoRepository.findById(taskId).orElse(null);
        if (todo != null && todo.getTargetUserId() != null && 
            todo.getTargetUserId().equals(receiver.getId())) {
            todo.setAccepted(true);
            todo.setUser(receiver); // 将任务所有权转移给接收者
            return todoRepository.save(todo);
        }
        return null;
    }
    
    @Override
    public Todo rejectTask(Long taskId, User receiver) {
        Todo todo = todoRepository.findById(taskId).orElse(null);
        if (todo != null && todo.getTargetUserId() != null && 
            todo.getTargetUserId().equals(receiver.getId())) {
            todo.setDeleted(true);
            return todoRepository.save(todo);
        }
        return null;
    }
    
    @Override
    public List<Todo> getPendingTasks(User user) {
        return todoRepository.findByTargetUserIdAndNotDeletedAndNotAccepted(user.getId());
    }

    @Override
    public List<PendingTaskDTO> getPendingTasksDTO(User user) {
        List<Todo> todos = todoRepository.findByTargetUserIdAndNotDeletedAndNotAccepted(user.getId());
        return todos.stream()
                .map(PendingTaskDTO::fromTodo)
                .collect(Collectors.toList());
    }
}