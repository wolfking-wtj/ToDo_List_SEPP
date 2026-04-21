package com.todo.dao;

import com.todo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    User findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(:keyword) OR LOWER(u.email) LIKE LOWER(:keyword)")
    List<User> searchUsers(@Param("keyword") String keyword);
}
