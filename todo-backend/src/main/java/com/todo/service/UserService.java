package com.todo.service;

import com.todo.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import java.util.List;

public interface UserService extends UserDetailsService {
    User register(User user);
    User findByUsername(String username);
    User updateUser(User user);
    User findById(Long id);
    List<User> searchUsers(String keyword);
    boolean isFriend(Long userId, Long friendId);
}
