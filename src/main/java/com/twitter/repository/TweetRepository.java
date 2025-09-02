package com.twitter.repository;

import com.twitter.model.Tweet;
import com.twitter.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {
    
    Page<Tweet> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);
    
    @Query("SELECT t FROM Tweet t WHERE t.author.id IN :authorIds ORDER BY t.createdAt DESC")
    Page<Tweet> findByAuthorIdInOrderByCreatedAtDesc(@Param("authorIds") List<Long> authorIds, Pageable pageable);
    
    @Query("SELECT t FROM Tweet t WHERE t.content LIKE %:query% ORDER BY t.createdAt DESC")
    Page<Tweet> searchTweets(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT t FROM Tweet t WHERE t.parentTweet.id = :tweetId ORDER BY t.createdAt ASC")
    Page<Tweet> findRepliesByTweetId(@Param("tweetId") Long tweetId, Pageable pageable);
    
    @Query("SELECT t FROM Tweet t WHERE t.originalTweet.id = :tweetId AND t.type = 'RETWEET' ORDER BY t.createdAt DESC")
    Page<Tweet> findRetweetsByTweetId(@Param("tweetId") Long tweetId, Pageable pageable);
    
    @Query("SELECT t FROM Tweet t JOIN t.likedBy u WHERE u.id = :userId ORDER BY t.createdAt DESC")
    Page<Tweet> findLikedTweetsByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(t) FROM Tweet t WHERE t.author.id = :userId")
    Long countTweetsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(l) FROM Tweet t JOIN t.likedBy l WHERE t.id = :tweetId")
    Long countLikesByTweetId(@Param("tweetId") Long tweetId);
    
    @Query("SELECT COUNT(r) FROM Tweet t JOIN t.retweets r WHERE t.id = :tweetId")
    Long countRetweetsByTweetId(@Param("tweetId") Long tweetId);
    
    @Query("SELECT COUNT(r) FROM Tweet t JOIN t.replies r WHERE t.id = :tweetId")
    Long countRepliesByTweetId(@Param("tweetId") Long tweetId);
    
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Tweet t JOIN t.likedBy u WHERE t.id = :tweetId AND u.id = :userId")
    Boolean isLikedByUser(@Param("tweetId") Long tweetId, @Param("userId") Long userId);
    
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Tweet t WHERE t.originalTweet.id = :tweetId AND t.author.id = :userId AND t.type = 'RETWEET'")
    Boolean isRetweetedByUser(@Param("tweetId") Long tweetId, @Param("userId") Long userId);
    
    @Query("SELECT t FROM Tweet t WHERE t.createdAt >= :since ORDER BY t.createdAt DESC")
    Page<Tweet> findTrendingTweets(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT t FROM Tweet t WHERE t.author.id IN " +
           "(SELECT f.id FROM User u JOIN u.following f WHERE u.id = :userId) " +
           "OR t.author.id = :userId ORDER BY t.createdAt DESC")
    Page<Tweet> findTimelineTweets(@Param("userId") Long userId, Pageable pageable);
}