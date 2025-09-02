package com.twitter.repository;

import com.twitter.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.displayName LIKE %:query%")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.id IN :userIds")
    List<User> findByIdIn(@Param("userIds") List<Long> userIds);
    
    @Query("SELECT u.followers FROM User u WHERE u.id = :userId")
    Page<User> findFollowersByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT u.following FROM User u WHERE u.id = :userId")
    Page<User> findFollowingByUserId(@Param("userId") Long userId, Pageable pageable);
    
    @Query("SELECT COUNT(f) FROM User u JOIN u.followers f WHERE u.id = :userId")
    Long countFollowersByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(f) FROM User u JOIN u.following f WHERE u.id = :userId")
    Long countFollowingByUserId(@Param("userId") Long userId);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u JOIN u.following f WHERE u.id = :followerId AND f.id = :followingId")
    Boolean isFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);
}