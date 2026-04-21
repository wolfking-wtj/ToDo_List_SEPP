package com.todo.service.impl;

import com.todo.dao.UserRepository;
import com.todo.model.User;
import com.todo.service.FriendService;
import com.todo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private FriendService friendService;
    
    @Override
    public User register(User user) {
        User existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser != null) {
            throw new RuntimeException("Username already exists");
        }
        
        User existingEmail = userRepository.findByEmail(user.getEmail());
        if (existingEmail != null) {
            throw new RuntimeException("Email already exists");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
    
    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), new ArrayList<>());
    }
    
    @Override
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    @Override
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    @Override
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String searchKeyword = "%" + keyword + "%";
        List<User> users = userRepository.searchUsers(searchKeyword);
        
        System.out.println("搜索关键字: " + keyword + ", 结果数量: " + users.size());
        for (User user : users) {
            System.out.println("找到用户: " + user.getUsername() + " (" + user.getEmail() + ")");
        }
        
        return users;
    }
    
    @Override
    public boolean isFriend(Long userId, Long friendId) {
        return friendService.isFriend(userId, friendId);
    }
}
