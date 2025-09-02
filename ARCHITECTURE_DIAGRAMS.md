# Twitter Clone - Architecture Diagrams

This document contains various architecture diagrams for the Twitter Clone system using PlantUML format.

## Table of Contents
1. [System Overview](#system-overview)
2. [Component Architecture](#component-architecture)
3. [Database Schema](#database-schema)
4. [Authentication Flow](#authentication-flow)
5. [Tweet Creation Flow](#tweet-creation-flow)
6. [Timeline Generation Flow](#timeline-generation-flow)
7. [Deployment Architecture](#deployment-architecture)
8. [Class Diagram](#class-diagram)

---

## System Overview

```plantuml
@startuml System_Overview
!define RECTANGLE class

skinparam backgroundColor #FAFAFA
skinparam componentStyle rectangle

actor User as U
actor Admin as A

package "Client Layer" {
    [Web Browser] as WB
    [Mobile App] as MA
    [API Client] as AC
}

package "Load Balancer" {
    [Kubernetes Ingress] as LB
}

package "Application Layer" {
    [Spring Boot App 1] as APP1
    [Spring Boot App 2] as APP2
    [Spring Boot App 3] as APP3
}

package "Data Layer" {
    [H2 Database] as DB
    [File System] as FS
}

package "Infrastructure" {
    [Kubernetes Cluster] as K8S
    [Docker Registry] as DR
}

U --> WB
U --> MA
A --> AC

WB --> LB
MA --> LB
AC --> LB

LB --> APP1
LB --> APP2
LB --> APP3

APP1 --> DB
APP2 --> DB
APP3 --> DB

APP1 --> FS
APP2 --> FS
APP3 --> FS

K8S ..> APP1
K8S ..> APP2
K8S ..> APP3

DR ..> K8S

@enduml
```

---

## Component Architecture

```plantuml
@startuml Component_Architecture
!define RECTANGLE class

skinparam backgroundColor #FAFAFA
skinparam componentStyle rectangle

package "Presentation Layer" {
    [Auth Controller] as AuthC
    [User Controller] as UserC
    [Tweet Controller] as TweetC
    [Static Content] as Static
}

package "Security Layer" {
    [JWT Filter] as JWTFilter
    [Authentication Manager] as AuthMgr
    [User Details Service] as UserDetails
    [JWT Token Provider] as JWTProvider
}

package "Service Layer" {
    [User Service] as UserS
    [Tweet Service] as TweetS
    [Timeline Service] as TimelineS
}

package "Repository Layer" {
    [User Repository] as UserR
    [Tweet Repository] as TweetR
}

package "Data Layer" {
    [H2 Database] as DB
}

package "Configuration" {
    [Security Config] as SecConfig
    [JPA Config] as JPAConfig
    [Web Config] as WebConfig
}

' Presentation to Security
AuthC --> JWTFilter
UserC --> JWTFilter
TweetC --> JWTFilter

' Security Layer connections
JWTFilter --> AuthMgr
AuthMgr --> UserDetails
JWTFilter --> JWTProvider

' Presentation to Service
AuthC --> UserS
UserC --> UserS
TweetC --> TweetS
TweetC --> TimelineS

' Service to Repository
UserS --> UserR
TweetS --> TweetR
TweetS --> UserR
TimelineS --> TweetR
TimelineS --> UserR

' Repository to Data
UserR --> DB
TweetR --> DB

' Configuration
SecConfig ..> JWTFilter
SecConfig ..> AuthMgr
JPAConfig ..> UserR
JPAConfig ..> TweetR
WebConfig ..> AuthC
WebConfig ..> UserC
WebConfig ..> TweetC

@enduml
```

---

## Database Schema

```plantuml
@startuml Database_Schema
!define TABLE class

skinparam backgroundColor #FAFAFA

entity "users" as users {
    * id : BIGINT <<PK>>
    --
    * username : VARCHAR(50) <<UK>>
    * email : VARCHAR(100) <<UK>>
    * password : VARCHAR(255)
    display_name : VARCHAR(100)
    bio : VARCHAR(160)
    profile_image_url : VARCHAR(500)
    is_verified : BOOLEAN
    * created_at : TIMESTAMP
    * updated_at : TIMESTAMP
}

entity "tweets" as tweets {
    * id : BIGINT <<PK>>
    --
    * content : TEXT
    * author_id : BIGINT <<FK>>
    original_tweet_id : BIGINT <<FK>>
    reply_to_id : BIGINT <<FK>>
    image_url : VARCHAR(500)
    * created_at : TIMESTAMP
    * updated_at : TIMESTAMP
}

entity "user_follows" as follows {
    * follower_id : BIGINT <<FK>>
    * following_id : BIGINT <<FK>>
    --
    * created_at : TIMESTAMP
}

entity "tweet_likes" as likes {
    * user_id : BIGINT <<FK>>
    * tweet_id : BIGINT <<FK>>
    --
    * created_at : TIMESTAMP
}

' Relationships
users ||--o{ tweets : "author_id"
tweets ||--o{ tweets : "original_tweet_id (retweet)"
tweets ||--o{ tweets : "reply_to_id (reply)"
users ||--o{ follows : "follower_id"
users ||--o{ follows : "following_id"
users ||--o{ likes : "user_id"
tweets ||--o{ likes : "tweet_id"

@enduml
```

---

## Authentication Flow

```plantuml
@startuml Authentication_Flow
skinparam backgroundColor #FAFAFA

actor User as U
participant "Web Client" as WC
participant "Auth Controller" as AC
participant "Authentication Manager" as AM
participant "User Details Service" as UDS
participant "JWT Token Provider" as JTP
participant "User Repository" as UR
database "H2 Database" as DB

U -> WC: Enter credentials
WC -> AC: POST /api/auth/login
AC -> AM: authenticate(credentials)
AM -> UDS: loadUserByUsername(username)
UDS -> UR: findByUsername(username)
UR -> DB: SELECT * FROM users WHERE username = ?
DB --> UR: User entity
UR --> UDS: User entity
UDS --> AM: UserPrincipal
AM -> AM: validatePassword(password)
AM --> AC: Authentication object
AC -> JTP: generateToken(userPrincipal)
JTP --> AC: JWT token
AC --> WC: AuthResponse with token
WC --> U: Login successful

note right of JTP
Token contains:
- User ID
- Username
- Roles
- Expiration time
end note

@enduml
```

---

## Tweet Creation Flow

```plantuml
@startuml Tweet_Creation_Flow
skinparam backgroundColor #FAFAFA

actor User as U
participant "Web Client" as WC
participant "JWT Filter" as JF
participant "Tweet Controller" as TC
participant "Tweet Service" as TS
participant "User Repository" as UR
participant "Tweet Repository" as TR
database "H2 Database" as DB

U -> WC: Create tweet
WC -> JF: POST /api/tweets (with JWT)
JF -> JF: validateToken()
JF -> TC: Authenticated request
TC -> TS: createTweet(userId, content, imageUrl)
TS -> UR: findById(userId)
UR -> DB: SELECT * FROM users WHERE id = ?
DB --> UR: User entity
UR --> TS: User entity
TS -> TS: validateTweetContent()
TS -> TR: save(tweet)
TR -> DB: INSERT INTO tweets (...)
DB --> TR: Tweet entity with ID
TR --> TS: Tweet entity
TS -> TS: convertToDto(tweet)
TS --> TC: TweetDto
TC --> WC: HTTP 200 + TweetDto
WC --> U: Tweet created successfully

note right of TS
Validation includes:
- Content length (≤ 280 chars)
- User exists and active
- Content sanitization
end note

@enduml
```

---

## Timeline Generation Flow

```plantuml
@startuml Timeline_Generation_Flow
skinparam backgroundColor #FAFAFA

actor User as U
participant "Web Client" as WC
participant "JWT Filter" as JF
participant "Tweet Controller" as TC
participant "Tweet Service" as TS
participant "User Repository" as UR
participant "Tweet Repository" as TR
database "H2 Database" as DB

U -> WC: Request home timeline
WC -> JF: GET /api/tweets/home (with JWT)
JF -> JF: validateToken()
JF -> TC: Authenticated request
TC -> TS: getHomeTimeline(userId, pageable)
TS -> UR: getFollowingIds(userId)
UR -> DB: SELECT following_id FROM user_follows WHERE follower_id = ?
DB --> UR: List of following IDs
UR --> TS: Following IDs
TS -> TR: findTweetsByAuthorsOrderByCreatedAtDesc(followingIds, pageable)
TR -> DB: SELECT * FROM tweets WHERE author_id IN (...) ORDER BY created_at DESC
DB --> TR: List of tweets
TR --> TS: Tweet entities
TS -> TS: enrichTweetsWithMetadata(tweets, userId)
TS -> TR: getLikeCounts(tweetIds)
TS -> TR: getRetweetCounts(tweetIds)
TS -> TR: getUserLikes(userId, tweetIds)
TS -> TS: convertToDtos(tweets)
TS --> TC: List<TweetDto>
TC --> WC: HTTP 200 + Timeline data
WC --> U: Display timeline

note right of TS
Metadata enrichment:
- Like counts
- Retweet counts
- Reply counts
- User's like status
- User's retweet status
end note

@enduml
```

---

## Deployment Architecture

```plantuml
@startuml Deployment_Architecture
!define RECTANGLE class

skinparam backgroundColor #FAFAFA
skinparam nodeStyle rectangle

cloud "Internet" as Internet

node "Kubernetes Cluster" as K8S {
    
    package "Ingress Layer" {
        [Ingress Controller] as Ingress
        [Load Balancer] as LB
    }
    
    package "Application Namespace" {
        [ConfigMap] as CM
        [Secret] as Secret
        
        package "Application Pods" {
            [App Pod 1] as Pod1
            [App Pod 2] as Pod2
            [App Pod 3] as Pod3
        }
        
        [Service] as Svc
        [HPA] as HPA
    }
    
    package "Storage" {
        [Persistent Volume] as PV
        [H2 Database Files] as DBFiles
    }
    
    package "Monitoring" {
        [Prometheus] as Prom
        [Grafana] as Graf
    }
}

node "Docker Registry" as Registry {
    [twitter-clone:latest] as Image
}

Internet --> Ingress
Ingress --> LB
LB --> Svc
Svc --> Pod1
Svc --> Pod2
Svc --> Pod3

CM --> Pod1
CM --> Pod2
CM --> Pod3

Secret --> Pod1
Secret --> Pod2
Secret --> Pod3

Pod1 --> PV
Pod2 --> PV
Pod3 --> PV

PV --> DBFiles

HPA --> Pod1
HPA --> Pod2
HPA --> Pod3

Prom --> Pod1
Prom --> Pod2
Prom --> Pod3

Graf --> Prom

Registry --> Pod1
Registry --> Pod2
Registry --> Pod3

@enduml
```

---

## Class Diagram

```plantuml
@startuml Class_Diagram
skinparam backgroundColor #FAFAFA

package "Model" {
    class User {
        -Long id
        -String username
        -String email
        -String password
        -String displayName
        -String bio
        -String profileImageUrl
        -boolean isVerified
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        -Set<User> followers
        -Set<User> following
        -Set<Tweet> tweets
        -Set<Tweet> likedTweets
        +getters()
        +setters()
    }
    
    class Tweet {
        -Long id
        -String content
        -User author
        -Tweet originalTweet
        -Tweet replyTo
        -String imageUrl
        -LocalDateTime createdAt
        -LocalDateTime updatedAt
        -Set<User> likedBy
        -Set<Tweet> replies
        -Set<Tweet> retweets
        +getters()
        +setters()
    }
}

package "Repository" {
    interface UserRepository {
        +findByUsername(String): Optional<User>
        +findByEmail(String): Optional<User>
        +searchByUsernameContaining(String, Pageable): Page<User>
        +getFollowersCount(Long): Long
        +getFollowingCount(Long): Long
    }
    
    interface TweetRepository {
        +findByAuthorOrderByCreatedAtDesc(User, Pageable): Page<Tweet>
        +findByAuthorInOrderByCreatedAtDesc(List<User>, Pageable): Page<Tweet>
        +countByAuthor(User): Long
        +countLikesByTweetId(Long): Long
        +countRetweetsByOriginalTweet(Tweet): Long
    }
}

package "Service" {
    class UserService {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        +createUser(UserRequest): User
        +getUserById(Long): UserDto
        +followUser(Long, Long): void
        +unfollowUser(Long, Long): void
        +searchUsers(String, Pageable): Page<UserDto>
    }
    
    class TweetService {
        -TweetRepository tweetRepository
        -UserRepository userRepository
        +createTweet(Long, String, String): Tweet
        +getTweetById(Long, Long): TweetDto
        +likeTweet(Long, Long): void
        +retweetTweet(Long, Long): Tweet
        +getHomeTimeline(Long, Pageable): Page<TweetDto>
        +getUserTimeline(Long, Pageable): Page<TweetDto>
    }
}

package "Controller" {
    class AuthController {
        -UserService userService
        -AuthenticationManager authManager
        -JwtTokenProvider tokenProvider
        +register(AuthRequest): ResponseEntity<AuthResponse>
        +login(AuthRequest): ResponseEntity<AuthResponse>
    }
    
    class UserController {
        -UserService userService
        +getProfile(UserPrincipal): ResponseEntity<UserDto>
        +updateProfile(UserRequest, UserPrincipal): ResponseEntity<UserDto>
        +followUser(Long, UserPrincipal): ResponseEntity<Void>
        +searchUsers(String, Pageable): ResponseEntity<Page<UserDto>>
    }
    
    class TweetController {
        -TweetService tweetService
        +createTweet(TweetRequest, UserPrincipal): ResponseEntity<TweetDto>
        +getTweet(Long, UserPrincipal): ResponseEntity<TweetDto>
        +likeTweet(Long, UserPrincipal): ResponseEntity<Void>
        +getHomeTimeline(Pageable, UserPrincipal): ResponseEntity<Page<TweetDto>>
    }
}

package "Security" {
    class JwtTokenProvider {
        -String jwtSecret
        -int jwtExpirationInMs
        +generateToken(Authentication): String
        +getUserIdFromToken(String): Long
        +validateToken(String): boolean
    }
    
    class JwtAuthenticationFilter {
        -JwtTokenProvider tokenProvider
        -CustomUserDetailsService userDetailsService
        +doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain): void
    }
    
    class UserPrincipal {
        -Long id
        -String username
        -String email
        -Collection<GrantedAuthority> authorities
        +create(User): UserPrincipal
    }
}

' Relationships
User ||--o{ Tweet : "author"
Tweet ||--o{ Tweet : "originalTweet"
Tweet ||--o{ Tweet : "replyTo"
User }o--o{ User : "follows"
User }o--o{ Tweet : "likes"

UserRepository --> User
TweetRepository --> Tweet

UserService --> UserRepository
TweetService --> TweetRepository
TweetService --> UserRepository

AuthController --> UserService
UserController --> UserService
TweetController --> TweetService

JwtAuthenticationFilter --> JwtTokenProvider
JwtTokenProvider --> UserPrincipal

@enduml
```

---

## Sequence Diagram - Follow User

```plantuml
@startuml Follow_User_Sequence
skinparam backgroundColor #FAFAFA

actor User as U
participant "Web Client" as WC
participant "JWT Filter" as JF
participant "User Controller" as UC
participant "User Service" as US
participant "User Repository" as UR
database "H2 Database" as DB

U -> WC: Click "Follow" button
WC -> JF: POST /api/users/{userId}/follow (with JWT)
JF -> JF: validateToken()
JF -> UC: Authenticated request
UC -> US: followUser(followerId, followingId)
US -> UR: findById(followerId)
UR -> DB: SELECT * FROM users WHERE id = ?
DB --> UR: Follower user
UR --> US: Follower user
US -> UR: findById(followingId)
UR -> DB: SELECT * FROM users WHERE id = ?
DB --> UR: Following user
UR --> US: Following user
US -> US: validateFollowOperation()
US -> UR: saveFollowRelationship(follower, following)
UR -> DB: INSERT INTO user_follows (follower_id, following_id, created_at)
DB --> UR: Success
UR --> US: Success
US --> UC: Success
UC --> WC: HTTP 200
WC --> U: "Following" status updated

alt Follow relationship already exists
    US -> US: checkExistingRelationship()
    US --> UC: BadRequest("Already following")
    UC --> WC: HTTP 400
    WC --> U: Error message
end

@enduml
```

---

## Activity Diagram - Tweet Lifecycle

```plantuml
@startuml Tweet_Lifecycle_Activity
skinparam backgroundColor #FAFAFA

start

:User creates tweet;
:Validate content length;

if (Content > 280 chars?) then (yes)
    :Return validation error;
    stop
else (no)
    :Sanitize content;
endif

:Save tweet to database;
:Generate tweet ID;

fork
    :Update user's tweet count;
fork again
    :Process mentions (future);
fork again
    :Process hashtags (future);
end fork

:Return tweet DTO;

note right
Tweet is now visible in:
- User's timeline
- Followers' home feeds
- Public timeline
end note

:Tweet created successfully;

stop

@enduml
```

---

## State Diagram - User Account

```plantuml
@startuml User_Account_State
skinparam backgroundColor #FAFAFA

[*] --> Unregistered

Unregistered --> Registered : register()
Registered --> Active : email_verification (future)
Registered --> Active : auto_activate (current)

Active --> Suspended : admin_suspend()
Suspended --> Active : admin_reactivate()

Active --> Deactivated : user_deactivate()
Deactivated --> Active : user_reactivate()

Active --> Deleted : admin_delete()
Suspended --> Deleted : admin_delete()
Deactivated --> Deleted : admin_delete()

Deleted --> [*]

note right of Active
User can:
- Create tweets
- Follow others
- Like tweets
- Update profile
end note

note right of Suspended
User cannot:
- Create tweets
- Follow others
- Login to system
end note

@enduml
```

---

## Component Interaction Overview

```plantuml
@startuml Component_Interaction_Overview
skinparam backgroundColor #FAFAFA

package "External" {
    [Browser] as Browser
    [Mobile App] as Mobile
}

package "Infrastructure" {
    [Kubernetes] as K8S
    [Docker] as Docker
    [Ingress] as Ingress
}

package "Application" {
    [Spring Boot] as App
    [Security Filter] as Security
    [Controllers] as Controllers
    [Services] as Services
    [Repositories] as Repos
}

package "Data" {
    [H2 Database] as DB
    [File System] as FS
}

package "Configuration" {
    [ConfigMaps] as CM
    [Secrets] as Secrets
    [Environment] as Env
}

Browser --> Ingress : HTTPS
Mobile --> Ingress : HTTPS
Ingress --> App : HTTP

K8S --> App : Deploy/Scale
Docker --> K8S : Container Images

App --> Security : Filter Requests
Security --> Controllers : Authenticated Requests
Controllers --> Services : Business Logic
Services --> Repos : Data Access
Repos --> DB : SQL Queries

App --> FS : Static Files
CM --> App : Configuration
Secrets --> App : Sensitive Data
Env --> App : Runtime Config

@enduml
```

This collection of architecture diagrams provides a comprehensive visual representation of the Twitter Clone system, covering all major aspects from high-level system overview to detailed component interactions and data flows.