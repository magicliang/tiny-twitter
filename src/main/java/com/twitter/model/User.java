package com.twitter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(min = 3, max = 50)
    @Column(unique = true)
    private String username;
    
    @NotBlank
    @Size(max = 50)
    @Email
    @Column(unique = true)
    private String email;
    
    @NotBlank
    @Size(min = 6, max = 100)
    @JsonIgnore
    private String password;
    
    @Size(max = 100)
    private String displayName;
    
    @Size(max = 160)
    private String bio;
    
    private String profileImageUrl;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Tweet> tweets = new HashSet<>();
    
    @ManyToMany
    @JoinTable(
        name = "user_follows",
        joinColumns = @JoinColumn(name = "follower_id"),
        inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    @JsonIgnore
    private Set<User> following = new HashSet<>();
    
    @ManyToMany(mappedBy = "following")
    @JsonIgnore
    private Set<User> followers = new HashSet<>();
    
    @ManyToMany
    @JoinTable(
        name = "user_likes",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "tweet_id")
    )
    @JsonIgnore
    private Set<Tweet> likedTweets = new HashSet<>();
    
    // Constructors
    public User() {}
    
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
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
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Set<Tweet> getTweets() {
        return tweets;
    }
    
    public void setTweets(Set<Tweet> tweets) {
        this.tweets = tweets;
    }
    
    public Set<User> getFollowing() {
        return following;
    }
    
    public void setFollowing(Set<User> following) {
        this.following = following;
    }
    
    public Set<User> getFollowers() {
        return followers;
    }
    
    public void setFollowers(Set<User> followers) {
        this.followers = followers;
    }
    
    public Set<Tweet> getLikedTweets() {
        return likedTweets;
    }
    
    public void setLikedTweets(Set<Tweet> likedTweets) {
        this.likedTweets = likedTweets;
    }
    
    // Helper methods
    public void follow(User user) {
        this.following.add(user);
        user.getFollowers().add(this);
    }
    
    public void unfollow(User user) {
        this.following.remove(user);
        user.getFollowers().remove(this);
    }
    
    public boolean isFollowing(User user) {
        return this.following.contains(user);
    }
    
    public void likeTweet(Tweet tweet) {
        this.likedTweets.add(tweet);
        tweet.getLikedBy().add(this);
    }
    
    public void unlikeTweet(Tweet tweet) {
        this.likedTweets.remove(tweet);
        tweet.getLikedBy().remove(this);
    }
    
    public boolean hasLiked(Tweet tweet) {
        return this.likedTweets.contains(tweet);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}