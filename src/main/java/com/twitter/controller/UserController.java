package com.twitter.controller;

import com.twitter.dto.UserDto;
import com.twitter.model.User;
import com.twitter.security.CurrentUser;
import com.twitter.security.UserPrincipal;
import com.twitter.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public UserDto getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        return userService.getUserProfile(currentUser.getId(), currentUser.getId());
    }
    
    @GetMapping("/{userId}")
    public UserDto getUserProfile(@PathVariable Long userId, 
                                 @CurrentUser UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return userService.getUserProfile(userId, currentUserId);
    }
    
    @GetMapping("/username/{username}")
    public ResponseEntity<UserDto> getUserByUsername(@PathVariable String username,
                                                    @CurrentUser UserPrincipal currentUser) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        UserDto userDto = userService.getUserProfile(user.getId(), currentUserId);
        
        return ResponseEntity.ok(userDto);
    }
    
    @PutMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserDto> updateProfile(@CurrentUser UserPrincipal currentUser,
                                               @RequestBody Map<String, String> updates) {
        User updatedUser = userService.updateProfile(
            currentUser.getId(),
            updates.get("displayName"),
            updates.get("bio"),
            updates.get("profileImageUrl")
        );
        
        UserDto userDto = userService.getUserProfile(updatedUser.getId(), currentUser.getId());
        return ResponseEntity.ok(userDto);
    }
    
    @PostMapping("/{userId}/follow")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> followUser(@PathVariable Long userId,
                                       @CurrentUser UserPrincipal currentUser) {
        try {
            userService.followUser(currentUser.getId(), userId);
            return ResponseEntity.ok().body("Successfully followed user");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{userId}/follow")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> unfollowUser(@PathVariable Long userId,
                                         @CurrentUser UserPrincipal currentUser) {
        try {
            userService.unfollowUser(currentUser.getId(), userId);
            return ResponseEntity.ok().body("Successfully unfollowed user");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/{userId}/followers")
    public Page<UserDto> getFollowers(@PathVariable Long userId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userService.getFollowers(userId, pageable);
    }
    
    @GetMapping("/{userId}/following")
    public Page<UserDto> getFollowing(@PathVariable Long userId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userService.getFollowing(userId, pageable);
    }
    
    @GetMapping("/search")
    public Page<UserDto> searchUsers(@RequestParam String q,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userService.searchUsers(q, pageable);
    }
}