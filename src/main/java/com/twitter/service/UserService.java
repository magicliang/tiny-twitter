package com.twitter.service;

import com.twitter.dto.UserDto;
import com.twitter.model.User;
import com.twitter.repository.TweetRepository;
import com.twitter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TweetRepository tweetRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public User createUser(String username, String email, String password, String displayName) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username is already taken!");
        }
        
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email Address already in use!");
        }
        
        User user = new User(username, email, passwordEncoder.encode(password));
        user.setDisplayName(displayName != null ? displayName : username);
        
        return userRepository.save(user);
    }
    
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
    }
    
    public UserDto getUserProfile(Long userId, Long currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Long followersCount = userRepository.countFollowersByUserId(userId);
        Long followingCount = userRepository.countFollowingByUserId(userId);
        Long tweetsCount = tweetRepository.countTweetsByUserId(userId);
        
        Boolean isFollowing = null;
        if (currentUserId != null && !currentUserId.equals(userId)) {
            isFollowing = userRepository.isFollowing(currentUserId, userId);
        }
        
        return new UserDto(user, followersCount, followingCount, tweetsCount, isFollowing);
    }
    
    public User updateProfile(Long userId, String displayName, String bio, String profileImageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (displayName != null) {
            user.setDisplayName(displayName);
        }
        if (bio != null) {
            user.setBio(bio);
        }
        if (profileImageUrl != null) {
            user.setProfileImageUrl(profileImageUrl);
        }
        
        return userRepository.save(user);
    }
    
    public void followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new RuntimeException("You cannot follow yourself");
        }
        
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new RuntimeException("User to follow not found"));
        
        if (follower.isFollowing(following)) {
            throw new RuntimeException("Already following this user");
        }
        
        follower.follow(following);
        userRepository.save(follower);
    }
    
    public void unfollowUser(Long followerId, Long followingId) {
        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower not found"));
        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new RuntimeException("User to unfollow not found"));
        
        if (!follower.isFollowing(following)) {
            throw new RuntimeException("Not following this user");
        }
        
        follower.unfollow(following);
        userRepository.save(follower);
    }
    
    public Page<UserDto> getFollowers(Long userId, Pageable pageable) {
        Page<User> followers = userRepository.findFollowersByUserId(userId, pageable);
        return followers.map(user -> {
            Long followersCount = userRepository.countFollowersByUserId(user.getId());
            Long followingCount = userRepository.countFollowingByUserId(user.getId());
            Long tweetsCount = tweetRepository.countTweetsByUserId(user.getId());
            return new UserDto(user, followersCount, followingCount, tweetsCount);
        });
    }
    
    public Page<UserDto> getFollowing(Long userId, Pageable pageable) {
        Page<User> following = userRepository.findFollowingByUserId(userId, pageable);
        return following.map(user -> {
            Long followersCount = userRepository.countFollowersByUserId(user.getId());
            Long followingCount = userRepository.countFollowingByUserId(user.getId());
            Long tweetsCount = tweetRepository.countTweetsByUserId(user.getId());
            return new UserDto(user, followersCount, followingCount, tweetsCount);
        });
    }
    
    public Page<UserDto> searchUsers(String query, Pageable pageable) {
        Page<User> users = userRepository.searchUsers(query, pageable);
        return users.map(user -> {
            Long followersCount = userRepository.countFollowersByUserId(user.getId());
            Long followingCount = userRepository.countFollowingByUserId(user.getId());
            Long tweetsCount = tweetRepository.countTweetsByUserId(user.getId());
            return new UserDto(user, followersCount, followingCount, tweetsCount);
        });
    }
}