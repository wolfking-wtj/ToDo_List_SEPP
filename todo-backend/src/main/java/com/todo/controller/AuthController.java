package com.todo.controller;

import com.todo.model.User;
import com.todo.service.JwtUtil;
import com.todo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registeredUser = userService.register(user);
            return ResponseEntity.ok(registeredUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String token = jwtUtil.generateToken(userDetails);
            
            // 获取完整用户信息
            User user = userService.findByUsername(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", user);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Login failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Not authenticated");
            }
            
            Object principal = authentication.getPrincipal();
            if (!(principal instanceof UserDetails)) {
                return ResponseEntity.status(401).body("Invalid authentication");
            }
            
            UserDetails userDetails = (UserDetails) principal;
            User user = userService.findByUsername(userDetails.getUsername());
            
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get user: " + e.getMessage());
        }
    }
    
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestBody User userUpdate) {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Not authenticated");
            }
            
            Object principal = authentication.getPrincipal();
            if (!(principal instanceof UserDetails)) {
                return ResponseEntity.status(401).body("Invalid authentication");
            }
            
            UserDetails userDetails = (UserDetails) principal;
            User user = userService.findByUsername(userDetails.getUsername());
            
            // 更新用户信息
            if (userUpdate.getAvatar() != null) {
                user.setAvatar(userUpdate.getAvatar());
            }
            if (userUpdate.getEmail() != null) {
                user.setEmail(userUpdate.getEmail());
            }
            if (userUpdate.getUsername() != null) {
                user.setUsername(userUpdate.getUsername());
            }
            if (userUpdate.getName() != null) {
                user.setName(userUpdate.getName());
            }
            
            User updatedUser = userService.updateUser(user);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to update user: " + e.getMessage());
        }
    }
    
    @PostMapping("/upload-avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("Not authenticated");
            }
            
            // 创建上传目录
            String uploadDir = System.getProperty("user.dir") + "/uploads/avatars";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            File dest = new File(dir, fileName);
            
            // 保存文件
            file.transferTo(dest);
            
            // 生成文件访问URL - 使用 /api/auth/avatar 作为前缀
            String avatarUrl = "/api/auth/avatar/" + fileName;
            
            // 更新用户头像
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername());
            user.setAvatar(avatarUrl);
            userService.updateUser(user);
            
            Map<String, String> response = new HashMap<>();
            response.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to upload avatar: " + e.getMessage());
        }
    }
    
    @GetMapping("/avatar/{fileName}")
    public ResponseEntity<?> getAvatar(@PathVariable String fileName) {
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/avatars";
            File file = new File(uploadDir, fileName);

            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageData = java.nio.file.Files.readAllBytes(file.toPath());

            return ResponseEntity.ok()
                    .header("Content-Type", "image/png")
                    .header("Cache-Control", "no-cache, no-store, must-revalidate")
                    .body(imageData);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Failed to get avatar: " + e.getMessage());
        }
    }
}