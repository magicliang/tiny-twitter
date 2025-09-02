package com.twitter.repository;

import com.twitter.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser1 = new User();
        testUser1.setUsername("testuser1");
        testUser1.setEmail("test1@example.com");
        testUser1.setPassword("password123");
        testUser1.setDisplayName("Test User 1");

        testUser2 = new User();
        testUser2.setUsername("testuser2");
        testUser2.setEmail("test2@example.com");
        testUser2.setPassword("password123");
        testUser2.setDisplayName("Test User 2");

        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);
    }

    @Test
    void findByUsername_Success() {
        // When
        Optional<User> found = userRepository.findByUsername("testuser1");

        // Then
        assertTrue(found.isPresent());
        assertEquals("testuser1", found.get().getUsername());
        assertEquals("test1@example.com", found.get().getEmail());
    }

    @Test
    void findByUsername_NotFound() {
        // When
        Optional<User> found = userRepository.findByUsername("nonexistent");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void findByEmail_Success() {
        // When
        Optional<User> found = userRepository.findByEmail("test1@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("testuser1", found.get().getUsername());
        assertEquals("test1@example.com", found.get().getEmail());
    }

    @Test
    void findByUsernameOrEmail_WithUsername() {
        // When
        Optional<User> found = userRepository.findByUsernameOrEmail("testuser1", "testuser1");

        // Then
        assertTrue(found.isPresent());
        assertEquals("testuser1", found.get().getUsername());
    }

    @Test
    void findByUsernameOrEmail_WithEmail() {
        // When
        Optional<User> found = userRepository.findByUsernameOrEmail("test1@example.com", "test1@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("test1@example.com", found.get().getEmail());
    }

    @Test
    void existsByUsername_True() {
        // When
        Boolean exists = userRepository.existsByUsername("testuser1");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByUsername_False() {
        // When
        Boolean exists = userRepository.existsByUsername("nonexistent");

        // Then
        assertFalse(exists);
    }

    @Test
    void existsByEmail_True() {
        // When
        Boolean exists = userRepository.existsByEmail("test1@example.com");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByEmail_False() {
        // When
        Boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertFalse(exists);
    }

    @Test
    void searchUsers_ByUsername() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.searchUsers("testuser1", pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("testuser1", result.getContent().get(0).getUsername());
    }

    @Test
    void searchUsers_ByDisplayName() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.searchUsers("Test User 1", pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("Test User 1", result.getContent().get(0).getDisplayName());
    }

    @Test
    void searchUsers_PartialMatch() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> result = userRepository.searchUsers("test", pageable);

        // Then
        assertEquals(2, result.getContent().size());
    }

    @Test
    void followingRelationship() {
        // Given
        testUser1.follow(testUser2);
        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);

        // When
        Boolean isFollowing = userRepository.isFollowing(testUser1.getId(), testUser2.getId());
        Long followersCount = userRepository.countFollowersByUserId(testUser2.getId());
        Long followingCount = userRepository.countFollowingByUserId(testUser1.getId());

        // Then
        assertTrue(isFollowing);
        assertEquals(1L, followersCount);
        assertEquals(1L, followingCount);
    }

    @Test
    void getFollowers() {
        // Given
        testUser1.follow(testUser2);
        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);
        
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> followers = userRepository.findFollowersByUserId(testUser2.getId(), pageable);

        // Then
        assertEquals(1, followers.getContent().size());
        assertEquals("testuser1", followers.getContent().get(0).getUsername());
    }

    @Test
    void getFollowing() {
        // Given
        testUser1.follow(testUser2);
        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);
        
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<User> following = userRepository.findFollowingByUserId(testUser1.getId(), pageable);

        // Then
        assertEquals(1, following.getContent().size());
        assertEquals("testuser2", following.getContent().get(0).getUsername());
    }
}