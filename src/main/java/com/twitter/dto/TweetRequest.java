package com.twitter.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class TweetRequest {
    
    @NotBlank
    @Size(max = 280)
    private String content;
    
    private String imageUrl;
    
    private Long parentTweetId; // For replies
    
    private Long originalTweetId; // For retweets
    
    public TweetRequest() {}
    
    public TweetRequest(String content) {
        this.content = content;
    }
    
    public TweetRequest(String content, Long parentTweetId) {
        this.content = content;
        this.parentTweetId = parentTweetId;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public Long getParentTweetId() {
        return parentTweetId;
    }
    
    public void setParentTweetId(Long parentTweetId) {
        this.parentTweetId = parentTweetId;
    }
    
    public Long getOriginalTweetId() {
        return originalTweetId;
    }
    
    public void setOriginalTweetId(Long originalTweetId) {
        this.originalTweetId = originalTweetId;
    }
}