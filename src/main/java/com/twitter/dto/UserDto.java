package com.twitter.dto;

import com.twitter.model.User;

import java.time.LocalDateTime;

public class UserDto {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private Long followersCount;
    private Long followingCount;
    private Long tweetsCount;
    private Boolean isFollowing;
    
    public UserDto() {}
    
    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.displayName = user.getDisplayName();
        this.bio = user.getBio();
        this.profileImageUrl = user.getProfileImageUrl();
        this.createdAt = user.getCreatedAt();
    }
    
    public UserDto(User user, Long followersCount, Long followingCount, Long tweetsCount) {
        this(user);
        this.followersCount = followersCount;
        this.followingCount = followingCount;
        this.tweetsCount = tweetsCount;
    }
    
    public UserDto(User user, Long followersCount, Long followingCount, Long tweetsCount, Boolean isFollowing) {
        this(user, followersCount, followingCount, tweetsCount);
        this.isFollowing = isFollowing;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getProfileImageUrl() {
        return profileImageUrl;
    }
    
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Long getFollowersCount() {
        return followersCount;
    }
    
    public void setFollowersCount(Long followersCount) {
        this.followersCount = followersCount;
    }
    
    public Long getFollowingCount() {
        return followingCount;
    }
    
    public void setFollowingCount(Long followingCount) {
        this.followingCount = followingCount;
    }
    
    public Long getTweetsCount() {
        return tweetsCount;
    }
    
    public void setTweetsCount(Long tweetsCount) {
        this.tweetsCount = tweetsCount;
    }
    
    public Boolean getIsFollowing() {
        return isFollowing;
    }
    
    public void setIsFollowing(Boolean isFollowing) {
        this.isFollowing = isFollowing;
    }
}