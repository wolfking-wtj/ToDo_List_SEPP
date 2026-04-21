package com.todo.dao;

import com.todo.model.Todo;
import com.todo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    List<Todo> findByUser(User user);
    List<Todo> findByUserAndCompleted(User user, boolean completed);
    List<Todo> findByUserAndDeleted(User user, boolean deleted);
    
    @Query("SELECT t FROM Todo t WHERE t.user.id = ?1 AND t.deleted = false")
    List<Todo> findByUserIdAndNotDeleted(Long userId);
    
    @Query("SELECT t FROM Todo t WHERE t.user.id = ?1 AND t.completed = ?2 AND t.deleted = false")
    List<Todo> findByUserIdAndCompletedAndNotDeleted(Long userId, boolean completed);
    
    @Query("SELECT t FROM Todo t WHERE t.user = ?1 AND t.completed = ?2 AND t.deleted = false")
    List<Todo> findByUserAndCompletedAndNotDeleted(User user, boolean completed);
    
    List<Todo> findByTargetUserId(Long userId);
    
    @Query("SELECT t FROM Todo t JOIN FETCH t.user WHERE t.targetUser.id = ?1 AND t.deleted = false AND t.accepted = false")
    List<Todo> findByTargetUserIdAndNotDeletedAndNotAccepted(Long userId);
    
    @Query("SELECT t FROM Todo t JOIN FETCH t.user WHERE t.targetUser = ?1 AND t.deleted = false AND t.accepted = false")
    List<Todo> findByTargetUserAndNotDeletedAndNotAccepted(User user);
}
