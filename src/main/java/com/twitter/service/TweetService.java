package com.twitter.service;

import com.twitter.dto.TweetDto;
import com.twitter.dto.UserDto;
import com.twitter.model.Tweet;
import com.twitter.model.User;
import com.twitter.repository.TweetRepository;
import com.twitter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TweetService {
    
    @Autowired
    private TweetRepository tweetRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public Tweet createTweet(Long userId, String content, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Tweet tweet = new Tweet(content, user);
        tweet.setImageUrl(imageUrl);
        
        return tweetRepository.save(tweet);
    }
    
    public Tweet createReply(Long userId, Long parentTweetId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Tweet parentTweet = tweetRepository.findById(parentTweetId)
                .orElseThrow(() -> new RuntimeException("Parent tweet not found"));
        
        Tweet reply = new Tweet(content, user, null, Tweet.TweetType.REPLY);
        reply.setParentTweet(parentTweet);
        
        return tweetRepository.save(reply);
    }
    
    public Tweet createRetweet(Long userId, Long originalTweetId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Tweet originalTweet = tweetRepository.findById(originalTweetId)
                .orElseThrow(() -> new RuntimeException("Original tweet not found"));
        
        // Check if user already retweeted this tweet
        Boolean alreadyRetweeted = tweetRepository.isRetweetedByUser(originalTweetId, userId);
        if (alreadyRetweeted) {
            throw new RuntimeException("Already retweeted this tweet");
        }
        
        Tweet retweet = new Tweet(content != null ? content : "", user, originalTweet, Tweet.TweetType.RETWEET);
        
        return tweetRepository.save(retweet);
    }
    
    public void deleteTweet(Long tweetId, Long userId) {
        Tweet tweet = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new RuntimeException("Tweet not found"));
        
        if (!tweet.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own tweets");
        }
        
        tweetRepository.delete(tweet);
    }
    
    public void likeTweet(Long tweetId, Long userId) {
        Tweet tweet = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new RuntimeException("Tweet not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.hasLiked(tweet)) {
            throw new RuntimeException("Already liked this tweet");
        }
        
        user.likeTweet(tweet);
        userRepository.save(user);
    }
    
    public void unlikeTweet(Long tweetId, Long userId) {
        Tweet tweet = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new RuntimeException("Tweet not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!user.hasLiked(tweet)) {
            throw new RuntimeException("Haven't liked this tweet");
        }
        
        user.unlikeTweet(tweet);
        userRepository.save(user);
    }
    
    public TweetDto getTweetById(Long tweetId, Long currentUserId) {
        Tweet tweet = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new RuntimeException("Tweet not found"));
        
        return convertToDto(tweet, currentUserId);
    }
    
    public Page<TweetDto> getUserTweets(Long userId, Pageable pageable, Long currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Page<Tweet> tweets = tweetRepository.findByAuthorOrderByCreatedAtDesc(user, pageable);
        return tweets.map(tweet -> convertToDto(tweet, currentUserId));
    }
    
    public Page<TweetDto> getTimelineTweets(Long userId, Pageable pageable) {
        Page<Tweet> tweets = tweetRepository.findTimelineTweets(userId, pageable);
        return tweets.map(tweet -> convertToDto(tweet, userId));
    }
    
    public Page<TweetDto> getTrendingTweets(Pageable pageable, Long currentUserId) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        Page<Tweet> tweets = tweetRepository.findTrendingTweets(since, pageable);
        return tweets.map(tweet -> convertToDto(tweet, currentUserId));
    }
    
    public Page<TweetDto> searchTweets(String query, Pageable pageable, Long currentUserId) {
        Page<Tweet> tweets = tweetRepository.searchTweets(query, pageable);
        return tweets.map(tweet -> convertToDto(tweet, currentUserId));
    }
    
    public Page<TweetDto> getReplies(Long tweetId, Pageable pageable, Long currentUserId) {
        Page<Tweet> replies = tweetRepository.findRepliesByTweetId(tweetId, pageable);
        return replies.map(tweet -> convertToDto(tweet, currentUserId));
    }
    
    public Page<TweetDto> getRetweets(Long tweetId, Pageable pageable, Long currentUserId) {
        Page<Tweet> retweets = tweetRepository.findRetweetsByTweetId(tweetId, pageable);
        return retweets.map(tweet -> convertToDto(tweet, currentUserId));
    }
    
    public Page<TweetDto> getLikedTweets(Long userId, Pageable pageable, Long currentUserId) {
        Page<Tweet> likedTweets = tweetRepository.findLikedTweetsByUserId(userId, pageable);
        return likedTweets.map(tweet -> convertToDto(tweet, currentUserId));
    }
    
    private TweetDto convertToDto(Tweet tweet, Long currentUserId) {
        Long likesCount = tweetRepository.countLikesByTweetId(tweet.getId());
        Long retweetsCount = tweetRepository.countRetweetsByTweetId(tweet.getId());
        Long repliesCount = tweetRepository.countRepliesByTweetId(tweet.getId());
        
        Boolean isLiked = false;
        Boolean isRetweeted = false;
        
        if (currentUserId != null) {
            isLiked = tweetRepository.isLikedByUser(tweet.getId(), currentUserId);
            isRetweeted = tweetRepository.isRetweetedByUser(tweet.getId(), currentUserId);
        }
        
        TweetDto tweetDto = new TweetDto(tweet, likesCount, retweetsCount, repliesCount, isLiked, isRetweeted);
        
        // Set author info
        if (tweet.getAuthor() != null) {
            tweetDto.setAuthor(new UserDto(tweet.getAuthor()));
        }
        
        // Set original tweet info for retweets
        if (tweet.getOriginalTweet() != null) {
            TweetDto originalTweetDto = convertToDto(tweet.getOriginalTweet(), currentUserId);
            tweetDto.setOriginalTweet(originalTweetDto);
        }
        
        // Set parent tweet info for replies
        if (tweet.getParentTweet() != null) {
            TweetDto parentTweetDto = convertToDto(tweet.getParentTweet(), currentUserId);
            tweetDto.setParentTweet(parentTweetDto);
        }
        
        return tweetDto;
    }
}