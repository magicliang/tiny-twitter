package com.twitter.dto;

import com.twitter.model.Tweet;

import java.time.LocalDateTime;

public class TweetDto {
    private Long id;
    private String content;
    private UserDto author;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long likesCount;
    private Long retweetsCount;
    private Long repliesCount;
    private Boolean isLiked;
    private Boolean isRetweeted;
    private String type;
    private String imageUrl;
    private TweetDto originalTweet;
    private TweetDto parentTweet;
    
    public TweetDto() {}
    
    public TweetDto(Tweet tweet) {
        this.id = tweet.getId();
        this.content = tweet.getContent();
        this.createdAt = tweet.getCreatedAt();
        this.updatedAt = tweet.getUpdatedAt();
        this.type = tweet.getType().name();
        this.imageUrl = tweet.getImageUrl();
        
        if (tweet.getAuthor() != null) {
            this.author = new UserDto(tweet.getAuthor());
        }
        
        if (tweet.getOriginalTweet() != null) {
            this.originalTweet = new TweetDto(tweet.getOriginalTweet());
        }
        
        if (tweet.getParentTweet() != null) {
            this.parentTweet = new TweetDto(tweet.getParentTweet());
        }
    }
    
    public TweetDto(Tweet tweet, Long likesCount, Long retweetsCount, Long repliesCount) {
        this(tweet);
        this.likesCount = likesCount;
        this.retweetsCount = retweetsCount;
        this.repliesCount = repliesCount;
    }
    
    public TweetDto(Tweet tweet, Long likesCount, Long retweetsCount, Long repliesCount, 
                   Boolean isLiked, Boolean isRetweeted) {
        this(tweet, likesCount, retweetsCount, repliesCount);
        this.isLiked = isLiked;
        this.isRetweeted = isRetweeted;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public UserDto getAuthor() {
        return author;
    }
    
    public void setAuthor(UserDto author) {
        this.author = author;
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
    
    public Long getLikesCount() {
        return likesCount;
    }
    
    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }
    
    public Long getRetweetsCount() {
        return retweetsCount;
    }
    
    public void setRetweetsCount(Long retweetsCount) {
        this.retweetsCount = retweetsCount;
    }
    
    public Long getRepliesCount() {
        return repliesCount;
    }
    
    public void setRepliesCount(Long repliesCount) {
        this.repliesCount = repliesCount;
    }
    
    public Boolean getIsLiked() {
        return isLiked;
    }
    
    public void setIsLiked(Boolean isLiked) {
        this.isLiked = isLiked;
    }
    
    public Boolean getIsRetweeted() {
        return isRetweeted;
    }
    
    public void setIsRetweeted(Boolean isRetweeted) {
        this.isRetweeted = isRetweeted;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public TweetDto getOriginalTweet() {
        return originalTweet;
    }
    
    public void setOriginalTweet(TweetDto originalTweet) {
        this.originalTweet = originalTweet;
    }
    
    public TweetDto getParentTweet() {
        return parentTweet;
    }
    
    public void setParentTweet(TweetDto parentTweet) {
        this.parentTweet = parentTweet;
    }
}