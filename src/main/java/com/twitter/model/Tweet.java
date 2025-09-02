package com.twitter.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tweets")
@EntityListeners(AuditingEntityListener.class)
public class Tweet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 280)
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    @ManyToMany(mappedBy = "likedTweets")
    @JsonIgnore
    private Set<User> likedBy = new HashSet<>();
    
    @OneToMany(mappedBy = "originalTweet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Tweet> retweets = new HashSet<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_tweet_id")
    private Tweet originalTweet;
    
    @OneToMany(mappedBy = "parentTweet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Tweet> replies = new HashSet<>();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_tweet_id")
    private Tweet parentTweet;
    
    @Enumerated(EnumType.STRING)
    private TweetType type = TweetType.ORIGINAL;
    
    private String imageUrl;
    
    // Constructors
    public Tweet() {}
    
    public Tweet(String content, User author) {
        this.content = content;
        this.author = author;
    }
    
    public Tweet(String content, User author, Tweet originalTweet, TweetType type) {
        this.content = content;
        this.author = author;
        this.originalTweet = originalTweet;
        this.type = type;
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
    
    public User getAuthor() {
        return author;
    }
    
    public void setAuthor(User author) {
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
    
    public Set<User> getLikedBy() {
        return likedBy;
    }
    
    public void setLikedBy(Set<User> likedBy) {
        this.likedBy = likedBy;
    }
    
    public Set<Tweet> getRetweets() {
        return retweets;
    }
    
    public void setRetweets(Set<Tweet> retweets) {
        this.retweets = retweets;
    }
    
    public Tweet getOriginalTweet() {
        return originalTweet;
    }
    
    public void setOriginalTweet(Tweet originalTweet) {
        this.originalTweet = originalTweet;
    }
    
    public Set<Tweet> getReplies() {
        return replies;
    }
    
    public void setReplies(Set<Tweet> replies) {
        this.replies = replies;
    }
    
    public Tweet getParentTweet() {
        return parentTweet;
    }
    
    public void setParentTweet(Tweet parentTweet) {
        this.parentTweet = parentTweet;
    }
    
    public TweetType getType() {
        return type;
    }
    
    public void setType(TweetType type) {
        this.type = type;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    // Helper methods
    public int getLikesCount() {
        return likedBy.size();
    }
    
    public int getRetweetsCount() {
        return retweets.size();
    }
    
    public int getRepliesCount() {
        return replies.size();
    }
    
    public boolean isLikedBy(User user) {
        return likedBy.contains(user);
    }
    
    public boolean isRetweetedBy(User user) {
        return retweets.stream().anyMatch(retweet -> retweet.getAuthor().equals(user));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tweet)) return false;
        Tweet tweet = (Tweet) o;
        return id != null && id.equals(tweet.getId());
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
    public enum TweetType {
        ORIGINAL, RETWEET, REPLY
    }
}