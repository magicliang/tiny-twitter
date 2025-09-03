# 技术决策

本文档记录了Twitter克隆项目中的重要技术决策，包括决策背景、考虑因素和最终选择。

## 目录

1. [架构决策](#架构决策)
2. [技术栈选择](#技术栈选择)
3. [数据库设计决策](#数据库设计决策)
4. [安全决策](#安全决策)
5. [API设计决策](#api设计决策)
6. [性能决策](#性能决策)
7. [部署决策](#部署决策)
8. [工具选择](#工具选择)

## 架构决策

### ADR-001: 选择单体架构而非微服务

**状态**: 已接受  
**日期**: 2024-01-01  
**决策者**: 开发团队

#### 背景
需要为Twitter克隆项目选择合适的架构模式。

#### 考虑的选项
1. **单体架构**: 所有功能在一个应用中
2. **微服务架构**: 功能拆分为多个独立服务
3. **模块化单体**: 单体应用但模块化设计

#### 决策
选择**单体架构**，但设计时考虑未来向微服务演进。

#### 理由
- **开发效率**: 项目初期，单体架构开发和调试更简单
- **部署简单**: 只需部署一个应用，运维成本低
- **性能优势**: 避免网络调用开销
- **团队规模**: 小团队更适合单体架构
- **演进路径**: 设计时保持模块边界清晰，便于未来拆分

#### 后果
- **正面**: 快速开发，简单部署，性能好
- **负面**: 扩展性受限，技术栈绑定
- **缓解措施**: 保持良好的模块化设计，为未来微服务化做准备

---

### ADR-002: 采用分层架构模式

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要确定应用内部的架构模式。

#### 决策
采用经典的三层架构：控制器层 → 服务层 → 数据访问层

#### 理由
- **职责分离**: 每层有明确的职责
- **可测试性**: 便于单元测试和集成测试
- **可维护性**: 代码组织清晰
- **Spring Boot支持**: 框架天然支持分层架构

```
┌─────────────────┐
│   控制器层      │ ← HTTP请求处理
├─────────────────┤
│    服务层       │ ← 业务逻辑
├─────────────────┤
│  数据访问层     │ ← 数据持久化
└─────────────────┘
```

---

## 技术栈选择

### ADR-003: 选择Spring Boot作为后端框架

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要选择后端开发框架。

#### 考虑的选项
1. **Spring Boot**: Java生态，成熟稳定
2. **Node.js + Express**: JavaScript，开发快速
3. **Django**: Python，功能丰富
4. **Go + Gin**: 性能优秀，并发能力强

#### 决策
选择**Spring Boot 2.7.18**

#### 理由
- **生态成熟**: 丰富的第三方库和工具
- **企业级**: 适合构建企业级应用
- **社区支持**: 庞大的社区和文档
- **团队熟悉度**: 团队对Java和Spring生态熟悉
- **集成能力**: 与数据库、安全、监控等组件集成良好

#### 技术栈组合
```yaml
框架: Spring Boot 2.7.18
数据访问: Spring Data JPA
安全: Spring Security
文档: SpringDoc OpenAPI
构建: Maven
```

---

### ADR-004: 选择JPA作为数据访问技术

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要选择数据访问层技术。

#### 考虑的选项
1. **Spring Data JPA**: ORM，开发效率高
2. **MyBatis**: SQL控制精确，性能好
3. **JDBC Template**: 轻量级，灵活性高
4. **jOOQ**: 类型安全的SQL构建

#### 决策
选择**Spring Data JPA**

#### 理由
- **开发效率**: 自动生成CRUD操作
- **类型安全**: 编译时检查
- **关系映射**: 自动处理对象关系映射
- **查询方法**: 支持方法名查询和自定义查询
- **事务管理**: 声明式事务支持

```java
// 示例：简洁的数据访问代码
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByUsernameContaining(String keyword);
}
```

---

## 数据库设计决策

### ADR-005: 选择H2作为开发数据库，MySQL作为生产数据库

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要选择合适的数据库系统。

#### 决策
- **开发环境**: H2内存数据库
- **生产环境**: MySQL 8.0

#### 理由

**H2用于开发**:
- **零配置**: 无需安装和配置
- **快速启动**: 应用启动速度快
- **测试友好**: 每次测试都是干净环境
- **开发便利**: 内置Web控制台

**MySQL用于生产**:
- **稳定可靠**: 经过大规模生产验证
- **性能优秀**: 支持高并发读写
- **生态丰富**: 工具和监控支持完善
- **运维成熟**: 备份、恢复、监控方案成熟

#### 配置示例
```yaml
# 开发环境
spring:
  profiles: dev
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true

# 生产环境  
spring:
  profiles: prod
  datasource:
    url: jdbc:mysql://localhost:3306/twitter_clone
    driver-class-name: com.mysql.cj.jdbc.Driver
```

---

### ADR-006: 数据库表设计决策

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要设计数据库表结构。

#### 关键决策

**1. 用户表设计**
```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- BCrypt加密
    bio TEXT,
    profile_image VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**决策理由**:
- `BIGINT` ID: 支持大量用户
- `VARCHAR(50)` 用户名: 限制长度，提高性能
- `VARCHAR(255)` 密码: 足够存储BCrypt哈希
- 时间戳字段: 审计和排序需要

**2. 推文表设计**
```sql
CREATE TABLE tweets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,  -- 支持280字符限制
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**决策理由**:
- `TEXT` 内容: 支持多语言字符
- 级联删除: 用户删除时自动删除推文
- 索引设计: `(user_id, created_at)` 复合索引

**3. 关注关系表设计**
```sql
CREATE TABLE user_follows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,    -- 关注者
    following_id BIGINT NOT NULL,   -- 被关注者
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_follow (follower_id, following_id)
);
```

**决策理由**:
- 独立表: 避免用户表过于复杂
- 唯一约束: 防止重复关注
- 双向外键: 保证数据一致性

---

## 安全决策

### ADR-007: 选择JWT作为认证机制

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要选择用户认证和授权机制。

#### 考虑的选项
1. **Session + Cookie**: 传统方式，服务器端状态
2. **JWT**: 无状态，客户端存储
3. **OAuth 2.0**: 第三方认证
4. **API Key**: 简单但安全性较低

#### 决策
选择**JWT (JSON Web Token)**

#### 理由
- **无状态**: 服务器不需要存储会话信息
- **可扩展**: 支持水平扩展和负载均衡
- **跨域支持**: 适合前后端分离架构
- **标准化**: 业界标准，工具支持好
- **信息携带**: 可以携带用户信息

#### 实现细节
```java
// JWT配置
@Component
public class JwtUtils {
    private String jwtSecret = "mySecretKey";
    private int jwtExpirationMs = 86400000; // 24小时
    
    public String generateJwtToken(UserPrincipal userPrincipal) {
        return Jwts.builder()
            .setSubject(userPrincipal.getUsername())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(SignatureAlgorithm.HS256, jwtSecret)
            .compact();
    }
}
```

#### 安全考虑
- **密钥管理**: 使用环境变量存储密钥
- **过期时间**: 设置合理的过期时间
- **HTTPS**: 生产环境必须使用HTTPS
- **刷新机制**: 考虑实现令牌刷新

---

### ADR-008: 密码加密策略

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要选择密码存储和验证方式。

#### 决策
使用**BCrypt**进行密码哈希

#### 理由
- **安全性高**: 自适应哈希函数，抗彩虹表攻击
- **盐值内置**: 自动生成和管理盐值
- **可调节强度**: 可以调整计算复杂度
- **Spring支持**: Spring Security内置支持

```java
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // 强度12
    }
}
```

---

## API设计决策

### ADR-009: 选择RESTful API设计

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要设计API接口规范。

#### 考虑的选项
1. **RESTful API**: 基于HTTP的资源导向
2. **GraphQL**: 灵活的查询语言
3. **RPC**: 远程过程调用
4. **WebSocket**: 实时双向通信

#### 决策
选择**RESTful API**作为主要接口

#### 理由
- **标准化**: HTTP标准，易于理解和使用
- **缓存友好**: 支持HTTP缓存机制
- **工具支持**: 丰富的测试和文档工具
- **简单性**: 学习成本低，开发效率高

#### 设计原则
```
资源导向: /api/users, /api/tweets
HTTP方法: GET(查询), POST(创建), PUT(更新), DELETE(删除)
状态码: 200(成功), 201(创建), 400(客户端错误), 500(服务器错误)
版本控制: /api/v1/users
```

#### API示例
```
GET    /api/users/{id}        # 获取用户信息
POST   /api/tweets            # 创建推文
PUT    /api/users/{id}        # 更新用户信息
DELETE /api/tweets/{id}       # 删除推文
```

---

### ADR-010: API响应格式标准化

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要统一API响应格式。

#### 决策
采用统一的响应格式包装

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

#### 理由
- **一致性**: 所有API返回格式一致
- **错误处理**: 统一的错误信息格式
- **客户端友好**: 客户端可以统一处理响应
- **扩展性**: 便于添加元数据信息

---

## 性能决策

### ADR-011: 数据库连接池选择

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要选择数据库连接池实现。

#### 考虑的选项
1. **HikariCP**: 高性能，Spring Boot默认
2. **Tomcat JDBC Pool**: Tomcat内置
3. **Apache DBCP**: Apache项目
4. **C3P0**: 老牌连接池

#### 决策
选择**HikariCP**

#### 理由
- **性能优秀**: 基准测试表现最佳
- **轻量级**: 代码简洁，依赖少
- **Spring默认**: Spring Boot 2.x默认选择
- **配置简单**: 合理的默认配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 20000
```

---

### ADR-012: 分页策略

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
推文列表和用户列表需要分页处理。

#### 考虑的选项
1. **偏移分页**: OFFSET + LIMIT
2. **游标分页**: 基于ID的游标
3. **时间分页**: 基于时间戳

#### 决策
采用**偏移分页**作为主要方式，**游标分页**作为优化选项

#### 理由
**偏移分页**:
- 实现简单，Spring Data JPA原生支持
- 支持跳转到任意页面
- 适合小到中等数据量

**游标分页**:
- 性能稳定，不受数据量影响
- 适合实时数据流
- 用于时间线等场景

```java
// 偏移分页
Page<Tweet> tweets = tweetRepository.findAll(PageRequest.of(page, size));

// 游标分页
List<Tweet> tweets = tweetRepository.findByIdLessThanOrderByIdDesc(lastId, limit);
```

---

## 部署决策

### ADR-013: 容器化部署策略

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要选择应用部署方式。

#### 考虑的选项
1. **传统部署**: 直接在服务器上运行JAR
2. **容器化**: Docker容器部署
3. **云原生**: Kubernetes + Docker
4. **Serverless**: 函数计算

#### 决策
选择**Docker容器化**部署

#### 理由
- **环境一致性**: 开发、测试、生产环境一致
- **部署简单**: 一键部署，回滚方便
- **资源隔离**: 容器间相互隔离
- **扩展性**: 便于水平扩展

```dockerfile
FROM openjdk:17-jre-slim
COPY target/twitter-clone.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

---

### ADR-014: 配置管理策略

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要管理不同环境的配置。

#### 决策
采用**Spring Profiles + 外部配置**

#### 配置层次
```
1. application.yml (默认配置)
2. application-{profile}.yml (环境特定配置)
3. 环境变量 (敏感信息)
4. 命令行参数 (运行时覆盖)
```

#### 示例
```yaml
# application.yml
spring:
  application:
    name: twitter-clone
  jpa:
    hibernate:
      ddl-auto: validate

# application-dev.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    show-sql: true

# application-prod.yml
spring:
  datasource:
    url: ${DATABASE_URL}
  jpa:
    show-sql: false
```

---

## 工具选择

### ADR-015: API文档工具选择

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要生成和维护API文档。

#### 考虑的选项
1. **SpringDoc OpenAPI**: Spring Boot集成
2. **Springfox**: 老牌Swagger集成
3. **手写文档**: Markdown文档
4. **Postman**: 接口测试工具

#### 决策
选择**SpringDoc OpenAPI**

#### 理由
- **自动生成**: 基于代码注解自动生成
- **实时更新**: 代码变更时文档自动更新
- **交互式**: 支持在线测试API
- **标准化**: 基于OpenAPI 3.0标准
- **Spring Boot 3兼容**: 未来升级友好

```java
@RestController
@Tag(name = "推文管理", description = "推文相关API")
public class TweetController {
    
    @PostMapping("/tweets")
    @Operation(summary = "创建推文", description = "用户创建新推文")
    public ResponseEntity<Tweet> createTweet(@RequestBody TweetRequest request) {
        // 实现
    }
}
```

---

### ADR-016: 构建工具选择

**状态**: 已接受  
**日期**: 2024-01-01

#### 背景
需要选择项目构建工具。

#### 考虑的选项
1. **Maven**: XML配置，生态成熟
2. **Gradle**: Groovy/Kotlin DSL，灵活性高
3. **SBT**: Scala构建工具
4. **Bazel**: Google构建工具

#### 决策
选择**Maven**

#### 理由
- **生态成熟**: 插件丰富，文档完善
- **Spring支持**: Spring Boot官方推荐
- **团队熟悉**: 团队对Maven更熟悉
- **企业标准**: 大多数企业项目使用Maven
- **IDE支持**: 所有主流IDE都很好支持

```xml
<project>
    <groupId>com.twitter</groupId>
    <artifactId>twitter-clone</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>
</project>
```

---

## 决策总结

### 核心技术栈
- **后端框架**: Spring Boot 2.7.18
- **数据访问**: Spring Data JPA
- **数据库**: H2 (开发) / MySQL (生产)
- **安全**: Spring Security + JWT
- **API文档**: SpringDoc OpenAPI
- **构建工具**: Maven
- **部署**: Docker容器化

### 架构特点
- **单体架构**: 简单高效，便于开发和部署
- **分层设计**: 职责分离，便于维护
- **RESTful API**: 标准化接口设计
- **无状态认证**: JWT支持水平扩展

### 未来演进
- **微服务化**: 业务增长时拆分服务
- **缓存层**: 引入Redis提升性能
- **消息队列**: 异步处理和解耦
- **监控体系**: 完善监控和告警

---

*注意: 技术决策会随着项目发展和需求变化而调整。本文档会持续更新以反映最新的决策状态。*