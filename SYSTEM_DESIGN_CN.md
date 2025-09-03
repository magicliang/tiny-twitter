# 系统设计

本文档详细描述了Twitter克隆应用的系统设计，包括架构决策、组件设计和实现细节。

## 目录

1. [系统概览](#系统概览)
2. [架构设计](#架构设计)
3. [数据模型设计](#数据模型设计)
4. [API设计](#api设计)
5. [安全设计](#安全设计)
6. [性能设计](#性能设计)
7. [可扩展性设计](#可扩展性设计)
8. [部署设计](#部署设计)

## 系统概览

### 项目目标

创建一个简化版的Twitter克隆应用，支持以下核心功能：
- 用户注册和认证
- 发布和查看推文
- 关注其他用户
- 点赞推文
- 个人时间线和主页时间线

### 技术栈选择

| 层级 | 技术选择 | 理由 |
|------|----------|------|
| 后端框架 | Spring Boot 2.7.18 | 成熟稳定，生态丰富 |
| 数据访问 | Spring Data JPA | 简化数据访问层开发 |
| 数据库 | H2 (开发) / MySQL (生产) | 开发便利性和生产稳定性 |
| 安全框架 | Spring Security + JWT | 无状态认证，易于扩展 |
| API文档 | SpringDoc OpenAPI | 自动生成API文档 |
| 构建工具 | Maven | 依赖管理和项目构建 |

### 系统边界

#### 包含的功能
- ✅ 用户管理 (注册、登录、个人资料)
- ✅ 推文管理 (发布、删除、查看)
- ✅ 社交功能 (关注、点赞)
- ✅ 时间线 (个人、主页)
- ✅ 认证授权 (JWT)

#### 不包含的功能
- ❌ 实时通知
- ❌ 媒体文件上传
- ❌ 私信功能
- ❌ 推文转发
- ❌ 话题标签

## 架构设计

### 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        客户端层                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐│
│  │   Web浏览器     │  │   移动应用      │  │   API客户端     ││
│  └─────────────────┘  └─────────────────┘  └─────────────────┘│
└─────────────────────────────────────────────────────────────┘
                              │ HTTP/HTTPS
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        API网关层                             │
│                   (未来扩展预留)                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      应用服务层                              │
│                   Spring Boot应用                           │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐│
│  │   控制器层      │  │    服务层       │  │   数据访问层    ││
│  │  @Controller    │  │   @Service      │  │  @Repository    ││
│  └─────────────────┘  └─────────────────┘  └─────────────────┘│
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                        数据层                                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐│
│  │   关系数据库    │  │     缓存        │  │   文件存储      ││
│  │  H2/MySQL       │  │   (未来)        │  │   (未来)        ││
│  └─────────────────┘  └─────────────────┘  └─────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

### 分层架构详解

#### 1. 控制器层 (Controller Layer)
```java
@RestController
@RequestMapping("/api")
public class TweetController {
    // 处理HTTP请求
    // 参数验证
    // 响应格式化

}
```

**职责:**
- 处理HTTP请求和响应
- 请求参数验证
- 异常处理
- API文档注解

#### 2. 服务层 (Service Layer)
```java
@Service
@Transactional
public class TweetService {
    // 业务逻辑处理
    // 事务管理
    // 数据转换
}
```

**职责:**
- 核心业务逻辑
- 事务管理
- 数据转换 (Entity ↔ DTO)
- 业务规则验证

#### 3. 数据访问层 (Repository Layer)
```java
@Repository
public interface TweetRepository extends JpaRepository<Tweet, Long> {
    // 数据访问方法
    // 自定义查询
}
```

**职责:**
- 数据持久化
- 查询优化
- 数据库事务

### 组件交互设计

```
HTTP请求 → 安全过滤器 → 控制器 → 服务层 → 数据访问层 → 数据库
    ↓         ↓          ↓        ↓         ↓           ↓
  认证检查   JWT验证   参数验证  业务逻辑   SQL执行    数据存储
    ↑         ↑          ↑        ↑         ↑           ↑
HTTP响应 ← JSON序列化 ← DTO转换 ← 结果处理 ← 实体映射 ← 查询结果
```

## 数据模型设计

### 核心实体设计

#### 1. 用户实体 (User)
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    private String bio;
    private String profileImage;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

#### 2. 推文实体 (Tweet)
```java
@Entity
@Table(name = "tweets")
public class Tweet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 280)
    private String content;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

#### 3. 关注关系实体 (UserFollow)
```java
@Entity
@Table(name = "user_follows")
public class UserFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

#### 4. 点赞实体 (UserLike)
```java
@Entity
@Table(name = "user_likes")
public class UserLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tweet_id", nullable = false)
    private Tweet tweet;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
}
```

### 数据关系设计

```
Users (1) ←→ (N) Tweets
  ↑                ↓
  │                │
  │                ▼
  └─ (N) UserFollows (N)
  └─ (N) UserLikes (N) → Tweets
```

### 数据库约束设计

#### 唯一约束
```sql
-- 用户名和邮箱唯一
ALTER TABLE users ADD CONSTRAINT uk_users_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email);

-- 防止重复关注
ALTER TABLE user_follows ADD CONSTRAINT uk_user_follows 
    UNIQUE (follower_id, following_id);

-- 防止重复点赞
ALTER TABLE user_likes ADD CONSTRAINT uk_user_likes 
    UNIQUE (user_id, tweet_id);
```

#### 外键约束
```sql
-- 推文外键
ALTER TABLE tweets ADD CONSTRAINT fk_tweets_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 关注关系外键
ALTER TABLE user_follows ADD CONSTRAINT fk_follows_follower 
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_follows ADD CONSTRAINT fk_follows_following 
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE;

-- 点赞外键
ALTER TABLE user_likes ADD CONSTRAINT fk_likes_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE user_likes ADD CONSTRAINT fk_likes_tweet 
    FOREIGN KEY (tweet_id) REFERENCES tweets(id) ON DELETE CASCADE;
```

## API设计

### RESTful API设计原则

1. **资源导向**: URL表示资源，HTTP方法表示操作
2. **无状态**: 每个请求包含所有必要信息
3. **统一接口**: 一致的请求/响应格式
4. **分层系统**: 客户端不需要知道服务器内部结构

### API端点设计

#### 认证相关 API
```
POST /api/auth/register    # 用户注册
POST /api/auth/login       # 用户登录
POST /api/auth/logout      # 用户登出 (可选)
```

#### 用户相关 API
```
GET    /api/users/{id}           # 获取用户信息
PUT    /api/users/{id}           # 更新用户信息
GET    /api/users/{id}/tweets    # 获取用户推文
GET    /api/users/{id}/followers # 获取关注者列表
GET    /api/users/{id}/following # 获取关注列表
POST   /api/users/{id}/follow    # 关注用户
DELETE /api/users/{id}/follow    # 取消关注
```

#### 推文相关 API
```
GET    /api/tweets         # 获取推文列表
POST   /api/tweets         # 创建推文
GET    /api/tweets/{id}    # 获取特定推文
DELETE /api/tweets/{id}    # 删除推文
POST   /api/tweets/{id}/like    # 点赞推文
DELETE /api/tweets/{id}/like    # 取消点赞
```

#### 时间线相关 API
```
GET /api/timeline/home      # 获取主页时间线
GET /api/timeline/user/{id} # 获取用户时间线
```

### 请求/响应格式设计

#### 标准响应格式
```json
{
  "success": true,
  "message": "操作成功",
  "data": {
    // 实际数据
  },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

#### 错误响应格式
```json
{
  "success": false,
  "message": "错误描述",
  "error": {
    "code": "ERROR_CODE",
    "details": "详细错误信息"
  },
  "timestamp": "2024-01-01T12:00:00Z"
}
```

#### 分页响应格式
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": {
      "number": 0,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5
    }
  }
}
```

### API版本控制

```java
@RestController
@RequestMapping("/api/v1")  // 版本控制
public class TweetController {
    // API实现
}
```

## 安全设计

### 认证机制设计

#### JWT认证流程
```
1. 用户登录 → 验证凭据 → 生成JWT令牌
2. 客户端存储JWT → 请求时携带 → 服务器验证
3. 令牌过期 → 重新登录 → 刷新令牌
```

#### JWT令牌结构
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "用户ID",
    "username": "用户名",
    "iat": 1640995200,
    "exp": 1641081600
  },
  "signature": "签名"
}
```

### 授权机制设计

#### 基于角色的访问控制 (RBAC)
```java
@PreAuthorize("hasRole('USER')")
public ResponseEntity<Tweet> createTweet(@RequestBody TweetRequest request) {
    // 只有认证用户可以创建推文
}

@PreAuthorize("@tweetService.isOwner(#id, authentication.name)")
public ResponseEntity<Void> deleteTweet(@PathVariable Long id) {
    // 只有推文作者可以删除
}
```

### 安全配置

#### Spring Security配置
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

### 数据安全

#### 密码安全
```java
@Service
public class AuthService {
    
    private final PasswordEncoder passwordEncoder;
    
    public void registerUser(RegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        // 存储加密后的密码
    }
}
```

#### 输入验证
```java
@PostMapping("/tweets")
public ResponseEntity<Tweet> createTweet(@Valid @RequestBody TweetRequest request) {
    // @Valid 触发验证
}

public class TweetRequest {
    @NotBlank(message = "推文内容不能为空")
    @Size(max = 280, message = "推文长度不能超过280字符")
    private String content;
}
```

## 性能设计

### 数据库性能优化

#### 索引策略
```sql
-- 主要查询索引
CREATE INDEX idx_tweets_user_created ON tweets(user_id, created_at DESC);
CREATE INDEX idx_user_follows_follower ON user_follows(follower_id);
CREATE INDEX idx_user_likes_user_tweet ON user_likes(user_id, tweet_id);
```

#### 查询优化
```java
// 使用分页避免大量数据加载
@Query("SELECT t FROM Tweet t WHERE t.user.id IN :userIds ORDER BY t.createdAt DESC")
Page<Tweet> findTimelineTweets(@Param("userIds") List<Long> userIds, Pageable pageable);

// 使用投影减少数据传输
@Query("SELECT new com.twitter.dto.TweetSummary(t.id, t.content, t.createdAt, u.username) " +
       "FROM Tweet t JOIN t.user u WHERE t.id = :id")
TweetSummary findTweetSummary(@Param("id") Long id);
```

### 应用性能优化

#### 连接池配置
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
```

#### 异步处理
```java
@Async
public CompletableFuture<Void> sendNotificationAsync(User user, Tweet tweet) {
    // 异步处理通知
    return CompletableFuture.completedFuture(null);
}
```

## 可扩展性设计

### 水平扩展准备

#### 无状态设计
- 使用JWT而非Session
- 避免服务器端状态存储
- 支持多实例部署

#### 数据库扩展策略
```
阶段1: 单库单表
阶段2: 主从复制 (读写分离)
阶段3: 分库分表 (按用户ID分片)
```

### 微服务演进路径

#### 服务拆分策略
```
当前: 单体应用
未来: 
- 用户服务 (User Service)
- 推文服务 (Tweet Service)
- 时间线服务 (Timeline Service)
- 通知服务 (Notification Service)
```

#### 服务间通信
```
同步通信: REST API / gRPC
异步通信: 消息队列 (RabbitMQ/Kafka)
```

## 部署设计

### 环境配置

#### 开发环境
```yaml
spring:
  profiles:
    active: dev
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    show-sql: true
```

#### 生产环境
```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://localhost:3306/twitter_clone
  jpa:
    show-sql: false
```

### 容器化部署

#### Dockerfile
```dockerfile
FROM openjdk:17-jre-slim
COPY target/twitter-clone.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

#### Docker Compose
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      - db
  db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: twitter_clone
      MYSQL_ROOT_PASSWORD: password
```

### 监控和日志

#### 应用监控
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

#### 日志配置
```yaml
logging:
  level:
    com.twitter: INFO
    org.springframework.security: DEBUG
  pattern:
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

---

*注意: 这个系统设计文档描述了当前实现和未来扩展计划。实际实现可能会根据具体需求进行调整。*