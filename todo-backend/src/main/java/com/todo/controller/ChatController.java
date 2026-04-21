package com.todo.controller;

import com.todo.model.ChatRequest;
import com.todo.model.ChatResponse;
import com.todo.model.AgentChatResponse;
import com.todo.model.User;
import com.todo.service.ChatService;
import com.todo.service.TodoAgentService;
import com.todo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    private final ChatService chatService;
    private final TodoAgentService todoAgentService;
    private final UserService userService;

    @Autowired
    public ChatController(ChatService chatService, TodoAgentService todoAgentService, UserService userService) {
        this.chatService = chatService;
        this.todoAgentService = todoAgentService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        try {
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Message cannot be empty");
            }

            // 尝试获取当前用户
            User currentUser = null;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                currentUser = userService.findByUsername(userDetails.getUsername());
            }

            if (currentUser != null) {
                // 如果有用户登录，使用 TodoAgentService
                AgentChatResponse response = todoAgentService.processUserMessage(request.getMessage(), currentUser);
                return ResponseEntity.ok(response);
            } else {
                // 否则使用普通 ChatService
                ChatResponse chatResponse = chatService.getResponse(request.getMessage());
                return ResponseEntity.ok(chatResponse);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Failed to get AI response: " + e.getMessage());
        }
    }
}