# Twitter Clone - System Design Document

## Table of Contents
1. [Problem Statement](#problem-statement)
2. [Requirements](#requirements)
3. [Capacity Estimation](#capacity-estimation)
4. [System APIs](#system-apis)
5. [High-Level Design](#high-level-design)
6. [Database Design](#database-design)
7. [Detailed Component Design](#detailed-component-design)
8. [Security Design](#security-design)
9. [Scalability & Performance](#scalability--performance)
10. [Monitoring & Observability](#monitoring--observability)
11. [Deployment Architecture](#deployment-architecture)
12. [Trade-offs & Considerations](#trade-offs--considerations)

---

## Problem Statement

Design a Twitter-like social media platform that allows users to:
- Create accounts and authenticate
- Post short messages (tweets) up to 280 characters
- Follow/unfollow other users
- Like and retweet posts
- View personalized timeline feeds
- Reply to tweets (threading)

The system should be scalable, secure, and maintainable with proper testing coverage.

---

## Requirements

### Functional Requirements
1. **User Management**
   - User registration and authentication
   - User profile management (bio, display name, profile image)
   - Follow/unfollow functionality
   - User search capabilities

2. **Tweet Operations**
   - Create tweets (max 280 characters)
   - Delete own tweets
   - Like/unlike tweets
   - Retweet functionality
   - Reply to tweets (threading)
   - Image attachments support

3. **Timeline & Feed**
   - Personal timeline (user's own tweets)
   - Home feed (tweets from followed users)
   - Public timeline (all tweets)
   - Pagination support

4. **Search & Discovery**
   - Search tweets by content
   - Search users by username/display name
   - Trending topics (future enhancement)

### Non-Functional Requirements
1. **Performance**
   - Low latency for read operations (< 200ms)
   - High availability (99.9% uptime)
   - Support for concurrent users

2. **Scalability**
   - Horizontal scaling capability
   - Database partitioning ready
   - Microservices architecture ready

3. **Security**
   - JWT-based authentication
   - Input validation and sanitization
   - Rate limiting (future enhancement)
   - HTTPS encryption

4. **Reliability**
   - Data consistency
   - Graceful error handling
   - Comprehensive logging

---

## Capacity Estimation

### Assumptions
- **Daily Active Users (DAU)**: 10K users
- **Tweets per user per day**: 5 tweets
- **Read:Write ratio**: 100:1
- **Average tweet size**: 140 characters
- **Storage per tweet**: ~1KB (including metadata)

### Traffic Estimates
- **Daily tweets**: 10K × 5 = 50K tweets/day
- **Tweets per second**: 50K / (24 × 3600) ≈ 0.6 TPS
- **Read requests**: 0.6 × 100 = 60 RPS
- **Peak traffic**: 3x average = 180 RPS

### Storage Estimates
- **Daily storage**: 50K × 1KB = 50MB/day
- **Annual storage**: 50MB × 365 ≈ 18GB/year
- **With replication (3x)**: 54GB/year

### Memory Estimates
- **Cache 20% of daily tweets**: 50K × 0.2 × 1KB = 10MB
- **User sessions**: 10K users × 1KB = 10MB
- **Total cache requirement**: ~50MB

---

## System APIs

### Authentication APIs
```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

### User Management APIs
```http
GET    /api/users/profile
PUT    /api/users/profile
GET    /api/users/{userId}
POST   /api/users/{userId}/follow
DELETE /api/users/{userId}/follow
GET    /api/users/{userId}/followers
GET    /api/users/{userId}/following
GET    /api/users/search?q={query}
```

### Tweet APIs
```http
POST   /api/tweets
GET    /api/tweets/{tweetId}
DELETE /api/tweets/{tweetId}
POST   /api/tweets/{tweetId}/like
DELETE /api/tweets/{tweetId}/like
POST   /api/tweets/{tweetId}/retweet
DELETE /api/tweets/{tweetId}/retweet
POST   /api/tweets/{tweetId}/reply
GET    /api/tweets/timeline
GET    /api/tweets/home
GET    /api/tweets/search?q={query}
```

---

## High-Level Design

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web Client    │    │  Mobile Client  │    │   API Client    │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                    ┌─────────────▼─────────────┐
                    │     Load Balancer         │
                    │    (Kubernetes Ingress)   │
                    └─────────────┬─────────────┘
                                 │
                    ┌─────────────▼─────────────┐
                    │   Spring Boot App         │
                    │   (Multiple Instances)    │
                    │                           │
                    │  ┌─────────────────────┐  │
                    │  │   Security Layer    │  │
                    │  │   (JWT + Spring)    │  │
                    │  └─────────────────────┘  │
                    │                           │
                    │  ┌─────────────────────┐  │
                    │  │  Controller Layer   │  │
                    │  └─────────────────────┘  │
                    │                           │
                    │  ┌─────────────────────┐  │
                    │  │   Service Layer     │  │
                    │  └─────────────────────┘  │
                    │                           │
                    │  ┌─────────────────────┐  │
                    │  │ Repository Layer    │  │
                    │  └─────────────────────┘  │
                    └─────────────┬─────────────┘
                                 │
                    ┌─────────────▼─────────────┐
                    │      H2 Database          │
                    │    (In-Memory/File)       │
                    └───────────────────────────┘
```

---

## Database Design

### Entity Relationship Diagram

```
┌─────────────────┐         ┌─────────────────┐
│      Users      │         │     Tweets      │
├─────────────────┤         ├─────────────────┤
│ id (PK)         │◄────────┤ id (PK)         │
│ username        │         │ content         │
│ email           │         │ author_id (FK)  │
│ password        │         │ created_at      │
│ display_name    │         │ updated_at      │
│ bio             │         │ original_tweet_id│
│ profile_image   │         │ reply_to_id     │
│ created_at      │         │ image_url       │
│ updated_at      │         └─────────────────┘
│ is_verified     │                   │
└─────────────────┘                   │
         │                            │
         │                            │
    ┌────▼────────┐              ┌────▼────────┐
    │  Follows    │              │   Likes     │
    ├─────────────┤              ├─────────────┤
    │ follower_id │              │ user_id     │
    │ following_id│              │ tweet_id    │
    │ created_at  │              │ created_at  │
    └─────────────┘              └─────────────┘
```

### Database Schema

#### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    bio VARCHAR(160),
    profile_image_url VARCHAR(500),
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_username (username),
    INDEX idx_email (email)
);
```

#### Tweets Table
```sql
CREATE TABLE tweets (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    original_tweet_id BIGINT,
    reply_to_id BIGINT,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (original_tweet_id) REFERENCES tweets(id) ON DELETE CASCADE,
    FOREIGN KEY (reply_to_id) REFERENCES tweets(id) ON DELETE CASCADE,
    
    INDEX idx_author_created (author_id, created_at DESC),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_original_tweet (original_tweet_id),
    INDEX idx_reply_to (reply_to_id)
);
```

#### Follows Table
```sql
CREATE TABLE user_follows (
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (follower_id, following_id),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_follower (follower_id),
    INDEX idx_following (following_id)
);
```

#### Likes Table
```sql
CREATE TABLE tweet_likes (
    user_id BIGINT NOT NULL,
    tweet_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    PRIMARY KEY (user_id, tweet_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (tweet_id) REFERENCES tweets(id) ON DELETE CASCADE,
    
    INDEX idx_tweet_likes (tweet_id),
    INDEX idx_user_likes (user_id)
);
```

---

## Detailed Component Design

### 1. Authentication & Authorization

#### JWT Token Structure
```json
{
  "sub": "user123",
  "username": "john_doe",
  "roles": ["ROLE_USER"],
  "iat": 1640995200,
  "exp": 1641081600
}
```

#### Security Flow
```
Client Request → JWT Filter → Authentication → Authorization → Controller
                     ↓
              Token Validation
                     ↓
              User Principal Creation
```

### 2. Service Layer Architecture

#### User Service
- **Responsibilities**: User CRUD, follow/unfollow, profile management
- **Key Methods**:
  - `createUser(UserRequest)`: Register new user
  - `followUser(followerId, followingId)`: Create follow relationship
  - `getUserProfile(userId)`: Get user details with stats
  - `searchUsers(query)`: Search users by username/display name

#### Tweet Service
- **Responsibilities**: Tweet CRUD, likes, retweets, timeline generation
- **Key Methods**:
  - `createTweet(userId, content, imageUrl)`: Create new tweet
  - `likeTweet(userId, tweetId)`: Toggle like on tweet
  - `retweetTweet(userId, tweetId)`: Create retweet
  - `getHomeTimeline(userId, pageable)`: Get personalized feed
  - `getUserTimeline(userId, pageable)`: Get user's tweets

### 3. Data Transfer Objects (DTOs)

#### TweetDto
```java
public class TweetDto {
    private Long id;
    private String content;
    private UserDto author;
    private LocalDateTime createdAt;
    private int likeCount;
    private int retweetCount;
    private int replyCount;
    private boolean isLiked;
    private boolean isRetweeted;
    private TweetDto originalTweet; // for retweets
    private String imageUrl;
}
```

#### UserDto
```java
public class UserDto {
    private Long id;
    private String username;
    private String displayName;
    private String bio;
    private String profileImageUrl;
    private boolean isVerified;
    private int followersCount;
    private int followingCount;
    private int tweetsCount;
    private boolean isFollowing;
}
```

---

## Security Design

### 1. Authentication Flow
```
1. User submits credentials → AuthController
2. Validate credentials → UserDetailsService
3. Generate JWT token → JwtTokenProvider
4. Return token to client
5. Client includes token in Authorization header
6. JWT filter validates token on each request
```

### 2. Authorization Matrix
| Role | Create Tweet | Delete Tweet | Follow User | Admin Actions |
|------|-------------|-------------|-------------|---------------|
| USER | ✓ (own)     | ✓ (own)     | ✓           | ✗             |
| ADMIN| ✓           | ✓ (any)     | ✓           | ✓             |

### 3. Input Validation
- **Tweet content**: Max 280 characters, XSS prevention
- **User input**: Email validation, username format
- **File uploads**: Size limits, type validation
- **SQL injection**: Parameterized queries via JPA

### 4. Rate Limiting (Future Enhancement)
```java
@RateLimiter(name = "tweet-creation", fallbackMethod = "rateLimitFallback")
public ResponseEntity<TweetDto> createTweet(...) {
    // Implementation
}
```

---

## Scalability & Performance

### 1. Caching Strategy

#### Application-Level Caching
```java
@Cacheable(value = "user-profiles", key = "#userId")
public UserDto getUserProfile(Long userId) {
    // Implementation
}

@Cacheable(value = "tweet-details", key = "#tweetId")
public TweetDto getTweetById(Long tweetId) {
    // Implementation
}
```

#### Cache Hierarchy
```
L1: Application Cache (Caffeine) → 100ms TTL
L2: Redis Cache → 5min TTL
L3: Database → Persistent
```

### 2. Database Optimization

#### Indexing Strategy
- **Primary indexes**: All primary keys
- **Composite indexes**: (author_id, created_at) for timeline queries
- **Search indexes**: username, email for user search
- **Foreign key indexes**: All foreign key columns

#### Query Optimization
```sql
-- Optimized timeline query
SELECT t.*, u.username, u.display_name 
FROM tweets t 
JOIN users u ON t.author_id = u.id 
WHERE t.author_id IN (
    SELECT following_id FROM user_follows WHERE follower_id = ?
)
ORDER BY t.created_at DESC 
LIMIT 20 OFFSET ?;
```

### 3. Horizontal Scaling

#### Database Sharding (Future)
- **Shard by user_id**: Distribute users across shards
- **Shard by tweet_id**: Distribute tweets across shards
- **Cross-shard queries**: Use application-level aggregation

#### Microservices Decomposition (Future)
```
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│ User Service│  │Tweet Service│  │Timeline Svc │
└─────────────┘  └─────────────┘  └─────────────┘
       │                │                │
┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│  User DB    │  │  Tweet DB   │  │  Cache      │
└─────────────┘  └─────────────┘  └─────────────┘
```

---

## Monitoring & Observability

### 1. Application Metrics
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

#### Key Metrics to Track
- **Request metrics**: Response time, throughput, error rate
- **Business metrics**: Tweets/second, user registrations, active users
- **System metrics**: CPU, memory, disk usage
- **Database metrics**: Connection pool, query performance

### 2. Logging Strategy
```java
@Slf4j
public class TweetService {
    public TweetDto createTweet(Long userId, String content) {
        log.info("Creating tweet for user: {}", userId);
        try {
            // Implementation
            log.info("Tweet created successfully: {}", tweet.getId());
        } catch (Exception e) {
            log.error("Failed to create tweet for user: {}", userId, e);
            throw e;
        }
    }
}
```

#### Log Levels
- **ERROR**: System errors, exceptions
- **WARN**: Business rule violations, deprecated API usage
- **INFO**: Business events, user actions
- **DEBUG**: Detailed execution flow (dev/test only)

### 3. Health Checks
```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            // Check database connectivity
            return Health.up()
                .withDetail("database", "H2")
                .withDetail("status", "Connected")
                .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

---

## Deployment Architecture

### 1. Kubernetes Deployment

#### Pod Configuration
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: twitter-clone-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: twitter-clone
  template:
    spec:
      containers:
      - name: twitter-clone
        image: twitter-clone:latest
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

#### Service Configuration
```yaml
apiVersion: v1
kind: Service
metadata:
  name: twitter-clone-service
spec:
  selector:
    app: twitter-clone
  ports:
  - port: 80
    targetPort: 8080
  type: ClusterIP
```

### 2. Auto-scaling Configuration
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: twitter-clone-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: twitter-clone-app
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
```

### 3. Configuration Management
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: twitter-clone-config
data:
  application.yml: |
    spring:
      profiles:
        active: prod
      datasource:
        url: jdbc:h2:file:/data/twitterdb
```

### 4. Secrets Management
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: twitter-clone-secret
type: Opaque
data:
  JWT_SECRET: <base64-encoded-secret>
  DB_PASSWORD: <base64-encoded-password>
```

---

## Trade-offs & Considerations

### 1. Technology Choices

#### H2 Database
**Pros:**
- Zero configuration setup
- Perfect for development and testing
- In-memory mode for fast tests
- File-based persistence option

**Cons:**
- Not suitable for production at scale
- Limited concurrent connections
- No built-in replication
- Single point of failure

**Migration Path:**
```
H2 → PostgreSQL → PostgreSQL Cluster → Sharded PostgreSQL
```

#### Spring Boot Monolith
**Pros:**
- Rapid development and deployment
- Simplified testing and debugging
- Strong consistency guarantees
- Lower operational complexity

**Cons:**
- Scaling limitations
- Technology lock-in
- Single point of failure
- Deployment coupling

**Evolution Path:**
```
Monolith → Modular Monolith → Microservices
```

### 2. Performance Trade-offs

#### Consistency vs. Availability
- **Current**: Strong consistency (ACID transactions)
- **Future**: Eventual consistency for better availability
- **Trade-off**: Accept some data lag for better performance

#### Storage vs. Computation
- **Current**: Compute follower feeds on-demand
- **Future**: Pre-compute and cache popular feeds
- **Trade-off**: Storage cost vs. response time

### 3. Security vs. Performance
- **JWT validation**: Every request validated vs. session caching
- **Input sanitization**: Thorough validation vs. processing speed
- **Rate limiting**: Protection vs. user experience

### 4. Development vs. Production

#### Development Optimizations
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # Auto-create schema
    show-sql: true           # Debug SQL queries
  h2:
    console:
      enabled: true          # H2 web console
```

#### Production Optimizations
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate     # Validate schema only
    show-sql: false          # Disable SQL logging
  h2:
    console:
      enabled: false         # Disable web console
```

---

## Future Enhancements

### 1. Short-term (Next 3 months)
- **Redis caching**: Implement distributed caching
- **Rate limiting**: Prevent abuse and spam
- **Image upload**: Support for media attachments
- **Real-time notifications**: WebSocket implementation

### 2. Medium-term (3-6 months)
- **PostgreSQL migration**: Production-ready database
- **Search optimization**: Elasticsearch integration
- **Mobile API**: Optimized endpoints for mobile apps
- **Analytics dashboard**: User engagement metrics

### 3. Long-term (6+ months)
- **Microservices architecture**: Service decomposition
- **Message queues**: Asynchronous processing
- **CDN integration**: Global content delivery
- **Machine learning**: Recommendation algorithms

---

## Testing Strategy

### 1. Test Pyramid
```
    ┌─────────────┐
    │   E2E Tests │  ← 10%
    └─────────────┘
  ┌─────────────────┐
  │Integration Tests│  ← 20%
  └─────────────────┘
┌─────────────────────┐
│   Unit Tests        │  ← 70%
└─────────────────────┘
```

### 2. Test Coverage Goals
- **Unit tests**: 80%+ line coverage
- **Integration tests**: All API endpoints
- **E2E tests**: Critical user journeys

### 3. Test Categories

#### Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class TweetServiceTest {
    @Mock
    private TweetRepository tweetRepository;
    
    @InjectMocks
    private TweetService tweetService;
    
    @Test
    void shouldCreateTweetSuccessfully() {
        // Test implementation
    }
}
```

#### Integration Tests
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
class TweetControllerIntegrationTest {
    @Test
    void shouldCreateTweetViaAPI() {
        // Test implementation
    }
}
```

---

## Conclusion

This Twitter clone system design demonstrates a well-architected, scalable social media platform built with modern technologies and best practices. The design emphasizes:

1. **Modularity**: Clean separation of concerns with layered architecture
2. **Scalability**: Horizontal scaling capabilities with Kubernetes
3. **Security**: Comprehensive authentication and authorization
4. **Testability**: Extensive test coverage with multiple test types
5. **Maintainability**: Clear code structure and documentation
6. **Observability**: Comprehensive monitoring and logging

The system is designed to handle the current requirements efficiently while providing a clear path for future enhancements and scaling. The choice of technologies balances development speed with production readiness, making it suitable for both learning and real-world deployment scenarios.