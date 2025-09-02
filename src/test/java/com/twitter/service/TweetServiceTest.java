package com.twitter.service;

import com.twitter.dto.TweetDto;
import com.twitter.model.Tweet;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TweetServiceTest {

    @Mock
    private TweetRepository tweetRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TweetService tweetService;

    private User testUser;
    private Tweet testTweet;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testTweet = new Tweet();
        testTweet.setId(1L);
        testTweet.setContent("Test tweet content");
        testTweet.setAuthor(testUser);
        testTweet.setCreatedAt(LocalDateTime.now());
        testTweet.setType(Tweet.TweetType.ORIGINAL);
    }

    @Test
    void createTweet_Success() {
        // Given
        Long userId = 1L;
        String content = "New tweet content";
        String imageUrl = "http://example.com/image.jpg";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(tweetRepository.save(any(Tweet.class))).thenReturn(testTweet);

        // When
        Tweet result = tweetService.createTweet(userId, content, imageUrl);

        // Then
        assertNotNull(result);
        assertEquals(testTweet.getId(), result.getId());
        verify(userRepository).findById(userId);
        verify(tweetRepository).save(any(Tweet.class));
    }

    @Test
    void createTweet_UserNotFound() {
        // Given
        Long userId = 999L;
        String content = "New tweet content";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tweetService.createTweet(userId, content, null);
        });

        assertEquals("User not found", exception.getMessage());
        verify(tweetRepository, never()).save(any(Tweet.class));
    }

    @Test
    void createReply_Success() {
        // Given
        Long userId = 1L;
        Long parentTweetId = 2L;
        String content = "Reply content";

        Tweet parentTweet = new Tweet();
        parentTweet.setId(parentTweetId);

        Tweet reply = new Tweet();
        reply.setId(3L);
        reply.setContent(content);
        reply.setAuthor(testUser);
        reply.setParentTweet(parentTweet);
        reply.setType(Tweet.TweetType.REPLY);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(tweetRepository.findById(parentTweetId)).thenReturn(Optional.of(parentTweet));
        when(tweetRepository.save(any(Tweet.class))).thenReturn(reply);

        // When
        Tweet result = tweetService.createReply(userId, parentTweetId, content);

        // Then
        assertNotNull(result);
        assertEquals(reply.getId(), result.getId());
        assertEquals(Tweet.TweetType.REPLY, result.getType());
        verify(userRepository).findById(userId);
        verify(tweetRepository).findById(parentTweetId);
        verify(tweetRepository).save(any(Tweet.class));
    }

    @Test
    void createRetweet_Success() {
        // Given
        Long userId = 1L;
        Long originalTweetId = 2L;
        String content = "Retweet comment";

        Tweet originalTweet = new Tweet();
        originalTweet.setId(originalTweetId);

        Tweet retweet = new Tweet();
        retweet.setId(3L);
        retweet.setContent(content);
        retweet.setAuthor(testUser);
        retweet.setOriginalTweet(originalTweet);
        retweet.setType(Tweet.TweetType.RETWEET);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(tweetRepository.findById(originalTweetId)).thenReturn(Optional.of(originalTweet));
        when(tweetRepository.isRetweetedByUser(originalTweetId, userId)).thenReturn(false);
        when(tweetRepository.save(any(Tweet.class))).thenReturn(retweet);

        // When
        Tweet result = tweetService.createRetweet(userId, originalTweetId, content);

        // Then
        assertNotNull(result);
        assertEquals(retweet.getId(), result.getId());
        assertEquals(Tweet.TweetType.RETWEET, result.getType());
        verify(tweetRepository).isRetweetedByUser(originalTweetId, userId);
        verify(tweetRepository).save(any(Tweet.class));
    }

    @Test
    void createRetweet_AlreadyRetweeted() {
        // Given
        Long userId = 1L;
        Long originalTweetId = 2L;

        Tweet originalTweet = new Tweet();
        originalTweet.setId(originalTweetId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(tweetRepository.findById(originalTweetId)).thenReturn(Optional.of(originalTweet));
        when(tweetRepository.isRetweetedByUser(originalTweetId, userId)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tweetService.createRetweet(userId, originalTweetId, null);
        });

        assertEquals("Already retweeted this tweet", exception.getMessage());
        verify(tweetRepository, never()).save(any(Tweet.class));
    }

    @Test
    void likeTweet_Success() {
        // Given
        Long tweetId = 1L;
        Long userId = 1L;

        when(tweetRepository.findById(tweetId)).thenReturn(Optional.of(testTweet));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);

        // When
        tweetService.likeTweet(tweetId, userId);

        // Then
        verify(tweetRepository).findById(tweetId);
        verify(userRepository).findById(userId);
        verify(userRepository).save(testUser);
    }

    @Test
    void deleteTweet_Success() {
        // Given
        Long tweetId = 1L;
        Long userId = 1L;

        when(tweetRepository.findById(tweetId)).thenReturn(Optional.of(testTweet));

        // When
        tweetService.deleteTweet(tweetId, userId);

        // Then
        verify(tweetRepository).findById(tweetId);
        verify(tweetRepository).delete(testTweet);
    }

    @Test
    void deleteTweet_NotOwner() {
        // Given
        Long tweetId = 1L;
        Long userId = 2L; // Different user

        when(tweetRepository.findById(tweetId)).thenReturn(Optional.of(testTweet));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tweetService.deleteTweet(tweetId, userId);
        });

        assertEquals("You can only delete your own tweets", exception.getMessage());
        verify(tweetRepository, never()).delete(any(Tweet.class));
    }

    @Test
    void getTweetById_Success() {
        // Given
        Long tweetId = 1L;
        Long currentUserId = 1L;

        when(tweetRepository.findById(tweetId)).thenReturn(Optional.of(testTweet));
        when(tweetRepository.countLikesByTweetId(tweetId)).thenReturn(5L);
        when(tweetRepository.countRetweetsByTweetId(tweetId)).thenReturn(3L);
        when(tweetRepository.countRepliesByTweetId(tweetId)).thenReturn(2L);
        when(tweetRepository.isLikedByUser(tweetId, currentUserId)).thenReturn(true);
        when(tweetRepository.isRetweetedByUser(tweetId, currentUserId)).thenReturn(false);

        // When
        TweetDto result = tweetService.getTweetById(tweetId, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(testTweet.getId(), result.getId());
        assertEquals(testTweet.getContent(), result.getContent());
        assertEquals(5L, result.getLikesCount());
        assertEquals(3L, result.getRetweetsCount());
        assertEquals(2L, result.getRepliesCount());
        assertTrue(result.getIsLiked());
        assertFalse(result.getIsRetweeted());
    }

    @Test
    void getUserTweets_Success() {
        // Given
        Long userId = 1L;
        Long currentUserId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tweet> tweetPage = new PageImpl<>(Arrays.asList(testTweet));

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(tweetRepository.findByAuthorOrderByCreatedAtDesc(testUser, pageable)).thenReturn(tweetPage);
        when(tweetRepository.countLikesByTweetId(testTweet.getId())).thenReturn(5L);
        when(tweetRepository.countRetweetsByTweetId(testTweet.getId())).thenReturn(3L);
        when(tweetRepository.countRepliesByTweetId(testTweet.getId())).thenReturn(2L);
        when(tweetRepository.isLikedByUser(testTweet.getId(), currentUserId)).thenReturn(true);
        when(tweetRepository.isRetweetedByUser(testTweet.getId(), currentUserId)).thenReturn(false);

        // When
        Page<TweetDto> result = tweetService.getUserTweets(userId, pageable, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testTweet.getContent(), result.getContent().get(0).getContent());
        verify(userRepository).findById(userId);
        verify(tweetRepository).findByAuthorOrderByCreatedAtDesc(testUser, pageable);
    }

    @Test
    void searchTweets_Success() {
        // Given
        String query = "test";
        Long currentUserId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Tweet> tweetPage = new PageImpl<>(Arrays.asList(testTweet));

        when(tweetRepository.searchTweets(query, pageable)).thenReturn(tweetPage);
        when(tweetRepository.countLikesByTweetId(testTweet.getId())).thenReturn(5L);
        when(tweetRepository.countRetweetsByTweetId(testTweet.getId())).thenReturn(3L);
        when(tweetRepository.countRepliesByTweetId(testTweet.getId())).thenReturn(2L);
        when(tweetRepository.isLikedByUser(testTweet.getId(), currentUserId)).thenReturn(true);
        when(tweetRepository.isRetweetedByUser(testTweet.getId(), currentUserId)).thenReturn(false);

        // When
        Page<TweetDto> result = tweetService.searchTweets(query, pageable, currentUserId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testTweet.getContent(), result.getContent().get(0).getContent());
        verify(tweetRepository).searchTweets(query, pageable);
    }
}