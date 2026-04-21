package com.todo.controller;

import com.todo.dao.FriendRepository;
import com.todo.dao.UserRepository;
import com.todo.model.Friend;
import com.todo.model.User;
import com.todo.service.FriendService;
import com.todo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/friends")
@CrossOrigin
public class FriendController {
    @Autowired
    private FriendService friendService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private FriendRepository friendRepository;
    
    @Autowired
    private UserRepository userRepository;
    
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
    
    @GetMapping
    public ResponseEntity<?> getFriends() {
        try {
            User user = getCurrentUser();
            List<Map<String, Object>> friends = friendService.getFriendsWithTodos(user.getId());
            return ResponseEntity.ok(friends);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get friends: " + e.getMessage());
        }
    }
    
    @GetMapping("/heatmap")
    public ResponseEntity<?> getFriendsHeatmap() {
        try {
            User user = getCurrentUser();
            List<Map<String, Object>> heatmaps = friendService.getFriendsHeatmap(user.getId());
            return ResponseEntity.ok(heatmaps);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get friends heatmap: " + e.getMessage());
        }
    }
    
    @GetMapping("/{friendId}/heatmap")
    public ResponseEntity<?> getFriendHeatmap(@PathVariable Long friendId, 
                                              @RequestParam(required = false) String year,
                                              @RequestParam(required = false) String month) {
        try {
            User user = getCurrentUser();
            Map<String, Object> heatmap;
            
            if (year != null && month != null) {
                heatmap = friendService.getFriendHeatmap(friendId, year, month);
            } else {
                heatmap = friendService.getFriendHeatmap(friendId);
            }
            heatmap.put("friendId", friendId);
            
            User friendUser = userRepository.findById(friendId).orElse(null);
            if (friendUser != null) {
                if (friendUser.getId().equals(user.getId())) {
                    heatmap.put("isSelf", true);
                } else {
                    heatmap.put("isSelf", false);
                }
            }
            
            return ResponseEntity.ok(heatmap);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get friend heatmap: " + e.getMessage());
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        try {
            User currentUser = getCurrentUser();
            List<User> users = userService.searchUsers(keyword);
            
            List<Map<String, Object>> result = users.stream()
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("name", user.getName());
                    userMap.put("email", user.getEmail());
                    userMap.put("avatar", user.getAvatar());
                    userMap.put("level", user.getLevel());
                    userMap.put("points", user.getPoints());
                    
                    boolean isFriend = friendService.isFriend(currentUser.getId(), user.getId());
                    userMap.put("isFriend", isFriend);
                    
                    return userMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to search users: " + e.getMessage());
        }
    }
    
    @GetMapping("/{userId}/info")
    public ResponseEntity<?> getUserInfo(@PathVariable Long userId) {
        try {
            User user = userService.findByUsername(getCurrentUser().getUsername());
            if (user == null || !user.getId().equals(userId)) {
                User targetUser = userService.findByUsername(getCurrentUser().getUsername());
                if (targetUser != null && targetUser.getId().equals(userId)) {
                    user = targetUser;
                } else {
                    user = userService.findById(userId);
                }
            }
            
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            // 将头像 URL 转换为 API 访问路径
            String avatar = user.getAvatar();
            if (avatar != null && avatar.startsWith("/uploads/avatars/")) {
                avatar = "/api/auth" + avatar;
            }
            userInfo.put("avatar", avatar);
            userInfo.put("level", user.getLevel());
            userInfo.put("points", user.getPoints());
            
            User currentUser = getCurrentUser();
            boolean isFriend = friendService.isFriend(currentUser.getId(), user.getId());
            userInfo.put("isFriend", isFriend);
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to get user info: " + e.getMessage());
        }
    }
    
    @PostMapping
    public ResponseEntity<?> addFriend(@RequestBody Map<String, Long> request) {
        try {
            User user = getCurrentUser();
            Long friendId = request.get("friendId");
            
            if (friendId == null) {
                return ResponseEntity.badRequest().body("Friend ID is required");
            }
            
            if (friendId.equals(user.getId())) {
                return ResponseEntity.badRequest().body("Cannot add yourself as friend");
            }
            
            User friendUser = userService.findById(friendId);
            if (friendUser == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            
            if (friendService.isFriend(user.getId(), friendId)) {
                return ResponseEntity.badRequest().body("Friend already exists");
            }
            
            Friend friend = new Friend();
            friend.setUserId(user.getId());
            friend.setFriendId(friendId);
            friend.setStatus(1);
            
            friendRepository.save(friend);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", friend.getId());
            response.put("username", friendUser.getUsername());
            response.put("name", friendUser.getName());
            // 将头像 URL 转换为 API 访问路径
            String avatar = friendUser.getAvatar();
            if (avatar != null && avatar.startsWith("/uploads/avatars/")) {
                avatar = "/api/auth" + avatar;
            }
            response.put("avatar", avatar);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add friend: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{friendId}")
    public ResponseEntity<?> deleteFriend(@PathVariable Long friendId) {
        try {
            User user = getCurrentUser();
            
            Friend friend = friendService.getFriends(user.getId()).stream()
                    .filter(f -> f.getFriendId().equals(friendId))
                    .findFirst()
                    .orElse(null);
            
            if (friend != null) {
                friendRepository.delete(friend);
            }
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to delete friend: " + e.getMessage());
        }
    }
}
