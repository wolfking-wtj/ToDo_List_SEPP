package com.todo.service;

import com.todo.dao.FriendRepository;
import com.todo.dao.TodoRepository;
import com.todo.dao.UserRepository;
import com.todo.model.Friend;
import com.todo.model.Todo;
import com.todo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FriendService {
    @Autowired
    private FriendRepository friendRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TodoRepository todoRepository;
    
    public List<Friend> getFriends(Long userId) {
        List<Friend> friends1 = friendRepository.findByUserId(userId);
        List<Friend> friends2 = friendRepository.findByFriendId(userId);
        
        Set<Friend> uniqueFriends = new HashSet<>();
        
        for (Friend f : friends1) {
            if (f.getStatus() == 1) {
                uniqueFriends.add(f);
            }
        }
        
        for (Friend f : friends2) {
            if (f.getStatus() == 1) {
                uniqueFriends.add(f);
            }
        }
        
        return uniqueFriends.stream().collect(Collectors.toList());
    }
    
    public List<Map<String, Object>> getFriendsWithTodos(Long userId) {
        List<Friend> friends = getFriends(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Friend f : friends) {
            Map<String, Object> friendMap = new HashMap<>();
            User friendUser = userRepository.findById(f.getFriendId()).orElse(null);
            
            if (friendUser != null) {
                friendMap.put("id", friendUser.getId());
                friendMap.put("username", friendUser.getUsername());
                friendMap.put("name", friendUser.getName());
                friendMap.put("email", friendUser.getEmail());
                // 将头像 URL 转换为 API 访问路径
                String avatar = friendUser.getAvatar();
                if (avatar != null && avatar.startsWith("/uploads/avatars/")) {
                    avatar = "/api/auth" + avatar;
                }
                friendMap.put("avatar", avatar);
                friendMap.put("level", friendUser.getLevel());
                friendMap.put("points", friendUser.getPoints());
                
                List<Todo> todos = todoRepository.findByUser(friendUser);
                friendMap.put("totalTasks", todos.size());
                friendMap.put("completedTasks", (int) todos.stream().filter(Todo::isCompleted).count());
                
                List<Map<String, Object>> dailyStats = getDailyStats(friendUser);
                friendMap.put("dailyStats", dailyStats);
                
                result.add(friendMap);
            }
        }
        
        return result;
    }
    
    private List<Map<String, Object>> getDailyStats(User user) {
        List<Todo> todos = todoRepository.findByUser(user);
        
        Map<String, Integer> stats = new LinkedHashMap<>();
        
        for (int i = 6; i >= 0; i--) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -i);
            String date = String.format("%d-%02d-%02d", 
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
            stats.put(date, 0);
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        for (Todo todo : todos) {
            if (todo.isCompleted() && todo.getCompletedAt() != null) {
                String completedDate = sdf.format(todo.getCompletedAt());
                if (stats.containsKey(completedDate)) {
                    stats.put(completedDate, stats.get(completedDate) + 1);
                }
            }
        }
        
        return stats.entrySet().stream()
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("date", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
    }
    
    public Map<String, Object> getFriendHeatmap(Long friendId) {
        Map<String, Object> result = new HashMap<>();
        
        User friendUser = userRepository.findById(friendId).orElse(null);
        if (friendUser == null) {
            return result;
        }
        
        List<Todo> todos = todoRepository.findByUser(friendUser);
        
        Map<String, Integer> heatmapData = new LinkedHashMap<>();
        Map<String, Integer> weekdayMap = new LinkedHashMap<>();
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -29);
        
        for (int i = 0; i < 30; i++) {
            String date = String.format("%d-%02d-%02d", 
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
            int weekday = cal.get(Calendar.DAY_OF_WEEK);
            heatmapData.put(date, 0);
            weekdayMap.put(date, weekday);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        for (Todo todo : todos) {
            if (todo.isCompleted() && todo.getCompletedAt() != null) {
                String completedDate = sdf.format(todo.getCompletedAt());
                if (heatmapData.containsKey(completedDate)) {
                    heatmapData.put(completedDate, heatmapData.get(completedDate) + 1);
                }
            }
        }
        
        int maxCount = 0;
        int totalCompleted = 0;
        int activeDays = 0;
        
        for (int count : heatmapData.values()) {
            if (count > maxCount) {
                maxCount = count;
            }
            totalCompleted += count;
            if (count > 0) {
                activeDays++;
            }
        }
        
        result.put("heatmapData", heatmapData);
        result.put("weekdayMap", weekdayMap);
        result.put("maxCount", maxCount);
        result.put("totalCompleted", totalCompleted);
        result.put("activeDays", activeDays);
        result.put("friendName", friendUser.getName() != null ? friendUser.getName() : friendUser.getUsername());
        // 将头像 URL 转换为 API 访问路径
        String avatar = friendUser.getAvatar();
        if (avatar != null && avatar.startsWith("/uploads/avatars/")) {
            avatar = "/api/auth" + avatar;
        }
        result.put("friendAvatar", avatar);
        
        return result;
    }
    
    public Map<String, Object> getFriendHeatmap(Long friendId, String year, String month) {
        Map<String, Object> result = new HashMap<>();
        
        User friendUser = userRepository.findById(friendId).orElse(null);
        if (friendUser == null) {
            return result;
        }
        
        List<Todo> todos = todoRepository.findByUser(friendUser);
        
        Map<String, Integer> heatmapData = new LinkedHashMap<>();
        Map<String, Integer> weekdayMap = new LinkedHashMap<>();
        
        Calendar cal = Calendar.getInstance();
        cal.set(Integer.parseInt(year), Integer.parseInt(month) - 1, 1);
        
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        int startIndex = firstDayOfWeek - 1;
        
        for (int i = 0; i < 42; i++) {
            int dayNum = i - startIndex + 1;
            String date = "";
            int weekday = 0;
            
            if (dayNum >= 1 && dayNum <= daysInMonth) {
                date = String.format("%s-%02d-%02d", year, Integer.parseInt(month), dayNum);
                cal.set(Integer.parseInt(year), Integer.parseInt(month) - 1, dayNum);
                weekday = cal.get(Calendar.DAY_OF_WEEK);
            } else if (dayNum < 1) {
                int prevMonthDay = daysInMonth + dayNum;
                cal.set(Integer.parseInt(year), Integer.parseInt(month) - 2, prevMonthDay);
                date = String.format("%d-%02d-%02d", 
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));
                weekday = cal.get(Calendar.DAY_OF_WEEK);
            } else {
                int nextMonthDay = dayNum - daysInMonth;
                cal.set(Integer.parseInt(year), Integer.parseInt(month), nextMonthDay);
                date = String.format("%d-%02d-%02d", 
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH));
                weekday = cal.get(Calendar.DAY_OF_WEEK);
            }
            
            heatmapData.put(date, 0);
            weekdayMap.put(date, weekday);
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        
        for (Todo todo : todos) {
            if (todo.isCompleted() && todo.getCompletedAt() != null) {
                String completedDate = sdf.format(todo.getCompletedAt());
                if (heatmapData.containsKey(completedDate)) {
                    heatmapData.put(completedDate, heatmapData.get(completedDate) + 1);
                }
            }
        }
        
        int maxCount = 0;
        int totalCompleted = 0;
        int activeDays = 0;
        
        for (int count : heatmapData.values()) {
            if (count > maxCount) {
                maxCount = count;
            }
            totalCompleted += count;
            if (count > 0) {
                activeDays++;
            }
        }
        
        result.put("heatmapData", heatmapData);
        result.put("weekdayMap", weekdayMap);
        result.put("maxCount", maxCount);
        result.put("totalCompleted", totalCompleted);
        result.put("activeDays", activeDays);
        result.put("friendName", friendUser.getName() != null ? friendUser.getName() : friendUser.getUsername());
        result.put("friendAvatar", friendUser.getAvatar());
        
        return result;
    }
    
    public List<Map<String, Object>> getFriendsHeatmap(Long userId) {
        List<Friend> friends = getFriends(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Friend f : friends) {
            Map<String, Object> friendHeatmap = getFriendHeatmap(f.getFriendId());
            User friendUser = userRepository.findById(f.getFriendId()).orElse(null);
            
            if (friendUser != null) {
                friendHeatmap.put("friendId", friendUser.getId());
                friendHeatmap.put("friendName", friendUser.getName() != null ? friendUser.getName() : friendUser.getUsername());
                // 将头像 URL 转换为 API 访问路径
                String avatar = friendUser.getAvatar();
                if (avatar != null && avatar.startsWith("/uploads/avatars/")) {
                    avatar = "/api/auth" + avatar;
                }
                friendHeatmap.put("friendAvatar", avatar);
                result.add(friendHeatmap);
            }
        }
        
        return result;
    }
    
    public boolean isFriend(Long userId, Long friendId) {
        List<Friend> friends = friendRepository.findByUserId(userId);
        List<Friend> friends2 = friendRepository.findByFriendId(userId);
        
        Set<Friend> uniqueFriends = new HashSet<>();
        uniqueFriends.addAll(friends);
        uniqueFriends.addAll(friends2);
        
        return uniqueFriends.stream()
                .anyMatch(f -> f.getStatus() == 1 && 
                    (f.getFriendId().equals(friendId) || f.getUserId().equals(friendId)));
    }
    
    public List<User> getFriends(User user) {
        List<Friend> friends = getFriends(user.getId());
        List<User> friendUsers = new ArrayList<>();
        
        for (Friend f : friends) {
            Long friendId = f.getUserId().equals(user.getId()) ? f.getFriendId() : f.getUserId();
            User friendUser = userRepository.findById(friendId).orElse(null);
            if (friendUser != null) {
                friendUsers.add(friendUser);
            }
        }
        
        return friendUsers;
    }
}
