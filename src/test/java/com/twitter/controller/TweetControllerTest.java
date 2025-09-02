package com.twitter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.dto.TweetDto;
import com.twitter.dto.TweetRequest;
import com.twitter.model.Tweet;
import com.twitter.model.User;
import com.twitter.security.JwtTokenProvider;
import com.twitter.security.UserPrincipal;
import com.twitter.service.TweetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TweetController.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TweetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TweetService tweetService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    private UserPrincipal userPrincipal;
    private Tweet testTweet;
    private TweetDto testTweetDto;
    private TweetRequest tweetRequest;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        userPrincipal = UserPrincipal.create(testUser);

        testTweet = new Tweet();
        testTweet.setId(1L);
        testTweet.setContent("Test tweet content");
        testTweet.setAuthor(testUser);
        testTweet.setCreatedAt(LocalDateTime.now());

        testTweetDto = new TweetDto(testTweet, 5L, 3L, 2L, true, false);

        tweetRequest = new TweetRequest();
        tweetRequest.setContent("New tweet content");
    }

    @Test
    @WithMockUser
    void createTweet_Success() throws Exception {
        // Given
        when(tweetService.createTweet(anyLong(), anyString(), anyString()))
                .thenReturn(testTweet);
        when(tweetService.getTweetById(testTweet.getId(), userPrincipal.getId()))
                .thenReturn(testTweetDto);

        // When & Then
        mockMvc.perform(post("/api/tweets")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tweetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testTweetDto.getId()))
                .andExpect(jsonPath("$.content").value(testTweetDto.getContent()))
                .andExpect(jsonPath("$.likesCount").value(5))
                .andExpect(jsonPath("$.retweetsCount").value(3))
                .andExpect(jsonPath("$.repliesCount").value(2));

        verify(tweetService).createTweet(userPrincipal.getId(), tweetRequest.getContent(), null);
        verify(tweetService).getTweetById(testTweet.getId(), userPrincipal.getId());
    }

    @Test
    @WithMockUser
    void createTweet_InvalidContent() throws Exception {
        // Given
        TweetRequest invalidRequest = new TweetRequest();
        invalidRequest.setContent(""); // Empty content

        // When & Then
        mockMvc.perform(post("/api/tweets")
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(tweetService, never()).createTweet(anyLong(), anyString(), anyString());
    }

    @Test
    @WithMockUser
    void replyToTweet_Success() throws Exception {
        // Given
        Long tweetId = 1L;
        Tweet reply = new Tweet();
        reply.setId(2L);
        reply.setContent("Reply content");

        TweetDto replyDto = new TweetDto(reply, 0L, 0L, 0L, false, false);

        when(tweetService.createReply(userPrincipal.getId(), tweetId, tweetRequest.getContent()))
                .thenReturn(reply);
        when(tweetService.getTweetById(reply.getId(), userPrincipal.getId()))
                .thenReturn(replyDto);

        // When & Then
        mockMvc.perform(post("/api/tweets/{tweetId}/reply", tweetId)
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tweetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(replyDto.getId()))
                .andExpect(jsonPath("$.content").value(replyDto.getContent()));

        verify(tweetService).createReply(userPrincipal.getId(), tweetId, tweetRequest.getContent());
    }

    @Test
    @WithMockUser
    void retweetTweet_Success() throws Exception {
        // Given
        Long tweetId = 1L;
        Tweet retweet = new Tweet();
        retweet.setId(3L);
        retweet.setContent("Retweet comment");

        TweetDto retweetDto = new TweetDto(retweet, 0L, 0L, 0L, false, false);

        when(tweetService.createRetweet(userPrincipal.getId(), tweetId, tweetRequest.getContent()))
                .thenReturn(retweet);
        when(tweetService.getTweetById(retweet.getId(), userPrincipal.getId()))
                .thenReturn(retweetDto);

        // When & Then
        mockMvc.perform(post("/api/tweets/{tweetId}/retweet", tweetId)
                .with(user(userPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tweetRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(retweetDto.getId()));

        verify(tweetService).createRetweet(userPrincipal.getId(), tweetId, tweetRequest.getContent());
    }

    @Test
    @WithMockUser
    void deleteTweet_Success() throws Exception {
        // Given
        Long tweetId = 1L;
        doNothing().when(tweetService).deleteTweet(tweetId, userPrincipal.getId());

        // When & Then
        mockMvc.perform(delete("/api/tweets/{tweetId}", tweetId)
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().string("Tweet deleted successfully"));

        verify(tweetService).deleteTweet(tweetId, userPrincipal.getId());
    }

    @Test
    @WithMockUser
    void likeTweet_Success() throws Exception {
        // Given
        Long tweetId = 1L;
        doNothing().when(tweetService).likeTweet(tweetId, userPrincipal.getId());

        // When & Then
        mockMvc.perform(post("/api/tweets/{tweetId}/like", tweetId)
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().string("Tweet liked successfully"));

        verify(tweetService).likeTweet(tweetId, userPrincipal.getId());
    }

    @Test
    @WithMockUser
    void unlikeTweet_Success() throws Exception {
        // Given
        Long tweetId = 1L;
        doNothing().when(tweetService).unlikeTweet(tweetId, userPrincipal.getId());

        // When & Then
        mockMvc.perform(delete("/api/tweets/{tweetId}/like", tweetId)
                .with(user(userPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().string("Tweet unliked successfully"));

        verify(tweetService).unlikeTweet(tweetId, userPrincipal.getId());
    }

    @Test
    void getTweet_Success() throws Exception {
        // Given
        Long tweetId = 1L;
        when(tweetService.getTweetById(tweetId, null)).thenReturn(testTweetDto);

        // When & Then
        mockMvc.perform(get("/api/tweets/{tweetId}", tweetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testTweetDto.getId()))
                .andExpect(jsonPath("$.content").value(testTweetDto.getContent()));

        verify(tweetService).getTweetById(tweetId, null);
    }

    @Test
    void getReplies_Success() throws Exception {
        // Given
        Long tweetId = 1L;
        Page<TweetDto> repliesPage = new PageImpl<>(Arrays.asList(testTweetDto));

        when(tweetService.getReplies(eq(tweetId), any(PageRequest.class), isNull()))
                .thenReturn(repliesPage);

        // When & Then
        mockMvc.perform(get("/api/tweets/{tweetId}/replies", tweetId)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(testTweetDto.getId()));

        verify(tweetService).getReplies(eq(tweetId), any(PageRequest.class), isNull());
    }

    @Test
    @WithMockUser
    void getTimeline_Success() throws Exception {
        // Given
        Page<TweetDto> timelinePage = new PageImpl<>(Arrays.asList(testTweetDto));

        when(tweetService.getTimelineTweets(eq(userPrincipal.getId()), any(PageRequest.class)))
                .thenReturn(timelinePage);

        // When & Then
        mockMvc.perform(get("/api/tweets/timeline")
                .with(user(userPrincipal))
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(testTweetDto.getId()));

        verify(tweetService).getTimelineTweets(eq(userPrincipal.getId()), any(PageRequest.class));
    }

    @Test
    void getTrendingTweets_Success() throws Exception {
        // Given
        Page<TweetDto> trendingPage = new PageImpl<>(Arrays.asList(testTweetDto));

        when(tweetService.getTrendingTweets(any(PageRequest.class), isNull()))
                .thenReturn(trendingPage);

        // When & Then
        mockMvc.perform(get("/api/tweets/trending")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(testTweetDto.getId()));

        verify(tweetService).getTrendingTweets(any(PageRequest.class), isNull());
    }

    @Test
    void searchTweets_Success() throws Exception {
        // Given
        String query = "test";
        Page<TweetDto> searchPage = new PageImpl<>(Arrays.asList(testTweetDto));

        when(tweetService.searchTweets(eq(query), any(PageRequest.class), isNull()))
                .thenReturn(searchPage);

        // When & Then
        mockMvc.perform(get("/api/tweets/search")
                .param("q", query)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(testTweetDto.getId()));

        verify(tweetService).searchTweets(eq(query), any(PageRequest.class), isNull());
    }

    @Test
    void getUserTweets_Success() throws Exception {
        // Given
        Long userId = 1L;
        Page<TweetDto> userTweetsPage = new PageImpl<>(Arrays.asList(testTweetDto));

        when(tweetService.getUserTweets(eq(userId), any(PageRequest.class), isNull()))
                .thenReturn(userTweetsPage);

        // When & Then
        mockMvc.perform(get("/api/tweets/user/{userId}", userId)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(testTweetDto.getId()));

        verify(tweetService).getUserTweets(eq(userId), any(PageRequest.class), isNull());
    }

    @Test
    void getUserLikedTweets_Success() throws Exception {
        // Given
        Long userId = 1L;
        Page<TweetDto> likedTweetsPage = new PageImpl<>(Arrays.asList(testTweetDto));

        when(tweetService.getLikedTweets(eq(userId), any(PageRequest.class), isNull()))
                .thenReturn(likedTweetsPage);

        // When & Then
        mockMvc.perform(get("/api/tweets/user/{userId}/likes", userId)
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpected(jsonPath("$.content[0].id").value(testTweetDto.getId()));

        verify(tweetService).getLikedTweets(eq(userId), any(PageRequest.class), isNull());
    }
}