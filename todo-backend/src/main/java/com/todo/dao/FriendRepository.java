package com.todo.dao;

import com.todo.model.Friend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendRepository extends JpaRepository<Friend, Long> {
    List<Friend> findByUserId(Long userId);
    List<Friend> findByFriendId(Long friendId);
    Friend findByUserIdAndFriendId(Long userId, Long friendId);
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);
    
    @Query("SELECT f FROM Friend f WHERE f.userId = :userId AND f.status = 1")
    List<Friend> findActiveFriends(@Param("userId") Long userId);
    
    @Query("SELECT f FROM Friend f WHERE f.friendId = :userId AND f.status = 1")
    List<Friend> findFriendsByFriendId(@Param("userId") Long userId);
}
