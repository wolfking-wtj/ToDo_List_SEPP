package com.todo.config;

import com.todo.dao.UserRepository;
import com.todo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestDataInitializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User user1 = new User();
            user1.setUsername("testuser1");
            user1.setEmail("test1@example.com");
            user1.setPassword(passwordEncoder.encode("password123"));
            user1.setAvatar("https://via.placeholder.com/150");
            user1.setLevel(1);
            user1.setPoints(0);
            userRepository.save(user1);
            
            User user2 = new User();
            user2.setUsername("testuser2");
            user2.setEmail("test2@example.com");
            user2.setPassword(passwordEncoder.encode("password123"));
            user2.setAvatar("https://via.placeholder.com/150");
            user2.setLevel(2);
            user2.setPoints(100);
            userRepository.save(user2);
            
            User user3 = new User();
            user3.setUsername("testuser3");
            user3.setEmail("test3@example.com");
            user3.setPassword(passwordEncoder.encode("password123"));
            user3.setAvatar("https://via.placeholder.com/150");
            user3.setLevel(3);
            user3.setPoints(200);
            userRepository.save(user3);
            
            System.out.println("测试用户已创建");
        }
    }
}
