package com.twitter.service;

import com.twitter.dto.UserDto;
import com.twitter.model.User;
import com.twitter.repository.TweetRepository;
import com.twitter.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TweetRepository tweetRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setDisplayName("Test User");
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createUser_Success() {
        // Given
        String username = "newuser";
        String email = "new@example.com";
        String password = "password123";
        String displayName = "New User";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUser(username, email, password, displayName);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        verify(userRepository).existsByUsername(username);
        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_UsernameAlreadyExists() {
        // Given
        String username = "existinguser";
        String email = "new@example.com";
        String password = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(username, email, password, null);
        });

        assertEquals("Username is already taken!", exception.getMessage());
        verify(userRepository).existsByUsername(username);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_EmailAlreadyExists() {
        // Given
        String username = "newuser";
        String email = "existing@example.com";
        String password = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(username, email, password, null);
        });

        assertEquals("Email Address already in use!", exception.getMessage());
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserProfile_Success() {
        // Given
        Long userId = 1L;
        Long currentUserId = 2L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.countFollowersByUserId(userId)).thenReturn(10L);
        when(userRepository.countFollowingByUserId(userId)).thenReturn(5L);
        when(tweetRepository.countTweetsByUserId(userId)).thenReturn(20L);
        when(userRepository.isFollowing(currentUserId, userId)).thenReturn(true);

        // When
        UserDto result = userService.getUserProfile(userId, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        assertEquals(10L, result.getFollowersCount());
        assertEquals(5L, result.getFollowingCount());
        assertEquals(20L, result.getTweetsCount());
        assertTrue(result.getIsFollowing());
    }

    @Test
    void getUserProfile_UserNotFound() {
        // Given
        Long userId = 999L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.getUserProfile(userId, null);
        });

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void followUser_Success() {
        // Given
        Long followerId = 1L;
        Long followingId = 2L;

        User follower = new User();
        follower.setId(followerId);
        User following = new User();
        following.setId(followingId);

        when(userRepository.findById(followerId)).thenReturn(Optional.of(follower));
        when(userRepository.findById(followingId)).thenReturn(Optional.of(following));
        when(userRepository.save(follower)).thenReturn(follower);

        // When
        userService.followUser(followerId, followingId);

        // Then
        verify(userRepository).findById(followerId);
        verify(userRepository).findById(followingId);
        verify(userRepository).save(follower);
    }

    @Test
    void followUser_CannotFollowSelf() {
        // Given
        Long userId = 1L;

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.followUser(userId, userId);
        });

        assertEquals("You cannot follow yourself", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void searchUsers_Success() {
        // Given
        String query = "test";
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Arrays.asList(testUser));

        when(userRepository.searchUsers(query, pageable)).thenReturn(userPage);
        when(userRepository.countFollowersByUserId(testUser.getId())).thenReturn(10L);
        when(userRepository.countFollowingByUserId(testUser.getId())).thenReturn(5L);
        when(tweetRepository.countTweetsByUserId(testUser.getId())).thenReturn(20L);

        // When
        Page<UserDto> result = userService.searchUsers(query, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testUser.getUsername(), result.getContent().get(0).getUsername());
        verify(userRepository).searchUsers(query, pageable);
    }

    @Test
    void updateProfile_Success() {
        // Given
        Long userId = 1L;
        String newDisplayName = "Updated Name";
        String newBio = "Updated bio";
        String newProfileImageUrl = "http://example.com/image.jpg";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        User result = userService.updateProfile(userId, newDisplayName, newBio, newProfileImageUrl);

        // Then
        assertNotNull(result);
        assertEquals(newDisplayName, result.getDisplayName());
        assertEquals(newBio, result.getBio());
        assertEquals(newProfileImageUrl, result.getProfileImageUrl());
        verify(userRepository).save(testUser);
    }
}