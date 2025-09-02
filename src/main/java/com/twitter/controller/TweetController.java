package com.twitter.controller;

import com.twitter.dto.TweetDto;
import com.twitter.dto.TweetRequest;
import com.twitter.model.Tweet;
import com.twitter.security.CurrentUser;
import com.twitter.security.UserPrincipal;
import com.twitter.service.TweetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/tweets")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TweetController {
    
    @Autowired
    private TweetService tweetService;
    
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TweetDto> createTweet(@Valid @RequestBody TweetRequest tweetRequest,
                                               @CurrentUser UserPrincipal currentUser) {
        try {
            Tweet tweet = tweetService.createTweet(
                currentUser.getId(),
                tweetRequest.getContent(),
                tweetRequest.getImageUrl()
            );
            
            TweetDto tweetDto = tweetService.getTweetById(tweet.getId(), currentUser.getId());
            return ResponseEntity.ok(tweetDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{tweetId}/reply")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TweetDto> replyToTweet(@PathVariable Long tweetId,
                                                @Valid @RequestBody TweetRequest tweetRequest,
                                                @CurrentUser UserPrincipal currentUser) {
        try {
            Tweet reply = tweetService.createReply(
                currentUser.getId(),
                tweetId,
                tweetRequest.getContent()
            );
            
            TweetDto tweetDto = tweetService.getTweetById(reply.getId(), currentUser.getId());
            return ResponseEntity.ok(tweetDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/{tweetId}/retweet")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TweetDto> retweetTweet(@PathVariable Long tweetId,
                                                @RequestBody(required = false) TweetRequest tweetRequest,
                                                @CurrentUser UserPrincipal currentUser) {
        try {
            String content = tweetRequest != null ? tweetRequest.getContent() : null;
            Tweet retweet = tweetService.createRetweet(
                currentUser.getId(),
                tweetId,
                content
            );
            
            TweetDto tweetDto = tweetService.getTweetById(retweet.getId(), currentUser.getId());
            return ResponseEntity.ok(tweetDto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{tweetId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteTweet(@PathVariable Long tweetId,
                                        @CurrentUser UserPrincipal currentUser) {
        try {
            tweetService.deleteTweet(tweetId, currentUser.getId());
            return ResponseEntity.ok().body("Tweet deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @PostMapping("/{tweetId}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> likeTweet(@PathVariable Long tweetId,
                                      @CurrentUser UserPrincipal currentUser) {
        try {
            tweetService.likeTweet(tweetId, currentUser.getId());
            return ResponseEntity.ok().body("Tweet liked successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{tweetId}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> unlikeTweet(@PathVariable Long tweetId,
                                        @CurrentUser UserPrincipal currentUser) {
        try {
            tweetService.unlikeTweet(tweetId, currentUser.getId());
            return ResponseEntity.ok().body("Tweet unliked successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/{tweetId}")
    public TweetDto getTweet(@PathVariable Long tweetId,
                            @CurrentUser UserPrincipal currentUser) {
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return tweetService.getTweetById(tweetId, currentUserId);
    }
    
    @GetMapping("/{tweetId}/replies")
    public Page<TweetDto> getReplies(@PathVariable Long tweetId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @CurrentUser UserPrincipal currentUser) {
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return tweetService.getReplies(tweetId, pageable, currentUserId);
    }
    
    @GetMapping("/{tweetId}/retweets")
    public Page<TweetDto> getRetweets(@PathVariable Long tweetId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @CurrentUser UserPrincipal currentUser) {
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return tweetService.getRetweets(tweetId, pageable, currentUserId);
    }
    
    @GetMapping("/timeline")
    @PreAuthorize("hasRole('USER')")
    public Page<TweetDto> getTimeline(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @CurrentUser UserPrincipal currentUser) {
        Pageable pageable = PageRequest.of(page, size);
        return tweetService.getTimelineTweets(currentUser.getId(), pageable);
    }
    
    @GetMapping("/trending")
    public Page<TweetDto> getTrendingTweets(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @CurrentUser UserPrincipal currentUser) {
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return tweetService.getTrendingTweets(pageable, currentUserId);
    }
    
    @GetMapping("/search")
    public Page<TweetDto> searchTweets(@RequestParam String q,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      @CurrentUser UserPrincipal currentUser) {
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return tweetService.searchTweets(q, pageable, currentUserId);
    }
    
    @GetMapping("/user/{userId}")
    public Page<TweetDto> getUserTweets(@PathVariable Long userId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @CurrentUser UserPrincipal currentUser) {
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return tweetService.getUserTweets(userId, pageable, currentUserId);
    }
    
    @GetMapping("/user/{userId}/likes")
    public Page<TweetDto> getUserLikedTweets(@PathVariable Long userId,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @CurrentUser UserPrincipal currentUser) {
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        return tweetService.getLikedTweets(userId, pageable, currentUserId);
    }
}