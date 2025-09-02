# Twitter 克隆 - Spring Boot 应用程序

一个功能完整的 Twitter 克隆，使用 Spring Boot、H2 内存数据库构建，专为 Kubernetes 部署而设计。

## 系统设计文档

本项目包含遵循 **Grokking System Design** 原则的全面系统设计文档：

### 📋 核心设计文档
- **[系统设计文档](SYSTEM_DESIGN.md)** - 完整的系统设计分析，包括：
  - 问题陈述和需求分析
  - 容量估算和流量预测
  - 高层架构和组件设计
  - 数据库模式和 API 规范
  - 安全架构和部署策略
  - 权衡取舍和未来增强

- **[架构图](ARCHITECTURE_DIAGRAMS.md)** - 使用 PlantUML 的可视化系统架构：
  - 系统概览和组件交互
  - 数据库模式和关系
  - 认证和业务逻辑流程
  - 部署架构和类图
  - 关键操作的时序图

- **[技术决策记录](TECHNICAL_DECISIONS.md)** - 记录的技术选择：
  - 框架选择理由（Spring Boot、H2、JWT）
  - 架构决策（单体 → 微服务演进）
  - 技术权衡和考虑的替代方案
  - 实施策略和后果

- **[性能和可扩展性分析](PERFORMANCE_SCALABILITY.md)** - 全面的扩展策略：
  - 性能要求和当前分析
  - 瓶颈识别和优化策略
  - 缓存架构和数据库优化
  - 负载测试方法和扩展路线图
  - 成本分析和性能权衡

### 🎯 设计亮点
- **可扩展架构**：设计为从单体向微服务演进
- **性能优化**：多级缓存和数据库优化策略
- **生产就绪**：Kubernetes 部署，具备监控和可观测性
- **文档完善**：每个架构决策都有理由记录
- **面试就绪**：遵循系统设计面试最佳实践

## 功能特性

- **用户管理**：注册、认证、个人资料管理
- **推文操作**：创建、删除、点赞、转发、回复推文
- **社交功能**：关注/取消关注用户、时间线、热门推文
- **搜索**：搜索用户和推文
- **安全**：基于 JWT 的认证和授权
- **API 文档**：Swagger/OpenAPI 集成
- **监控**：用于健康检查和指标的 Actuator 端点
- **测试**：全面的单元测试和集成测试
- **容器化**：支持多阶段构建的 Docker
- **Kubernetes**：完整的 K8s 部署，包含 HPA、入口和监控

## 技术栈

- **后端**：Spring Boot 2.7.18（兼容 Java 8）
- **数据库**：H2 内存数据库
- **安全**：Spring Security 与 JWT
- **测试**：JUnit 5、Mockito、Spring Boot Test
- **构建工具**：Maven
- **容器化**：Docker
- **编排**：Kubernetes
- **文档**：Swagger/OpenAPI 3

## API 端点

### 认证
- `POST /api/auth/signup` - 用户注册
- `POST /api/auth/signin` - 用户登录
- `POST /api/auth/refresh` - 刷新 JWT 令牌

### 用户
- `GET /api/users/me` - 获取当前用户资料
- `GET /api/users/{userId}` - 根据 ID 获取用户资料
- `GET /api/users/username/{username}` - 根据用户名获取用户
- `PUT /api/users/me` - 更新当前用户资料
- `POST /api/users/{userId}/follow` - 关注用户
- `DELETE /api/users/{userId}/follow` - 取消关注用户
- `GET /api/users/{userId}/followers` - 获取用户粉丝
- `GET /api/users/{userId}/following` - 获取用户关注的人
- `GET /api/users/search` - 搜索用户

### 推文
- `POST /api/tweets` - 创建新推文
- `GET /api/tweets/{tweetId}` - 根据 ID 获取推文
- `DELETE /api/tweets/{tweetId}` - 删除推文
- `POST /api/tweets/{tweetId}/like` - 点赞推文
- `DELETE /api/tweets/{tweetId}/like` - 取消点赞推文
- `POST /api/tweets/{tweetId}/reply` - 回复推文
- `POST /api/tweets/{tweetId}/retweet` - 转发推文
- `GET /api/tweets/{tweetId}/replies` - 获取推文回复
- `GET /api/tweets/{tweetId}/retweets` - 获取推文转发
- `GET /api/tweets/timeline` - 获取用户时间线
- `GET /api/tweets/trending` - 获取热门推文
- `GET /api/tweets/search` - 搜索推文
- `GET /api/tweets/user/{userId}` - 获取用户推文
- `GET /api/tweets/user/{userId}/likes` - 获取用户点赞的推文

## 快速开始

### 前置要求
- Java 8 或更高版本
- Maven 3.6+
- Docker（可选）
- Kubernetes 集群（可选）

### 本地开发

1. **克隆仓库**
   ```bash
   git clone <repository-url>
   cd twitter-clone
   ```

2. **运行应用程序**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **访问应用程序**
   - API：http://localhost:8080/api
   - H2 控制台：http://localhost:8080/api/h2-console
   - Swagger UI：http://localhost:8080/api/swagger-ui/
   - 健康检查：http://localhost:8080/api/actuator/health

### 测试

运行所有测试：
```bash
./mvnw test
```

运行带覆盖率的测试：
```bash
./mvnw test jacoco:report
```

### Docker 部署

1. **构建 Docker 镜像**
   ```bash
   docker build -t twitter-clone:latest .
   ```

2. **运行容器**
   ```bash
   docker run -p 8080:8080 twitter-clone:latest
   ```

### Kubernetes 部署

1. **应用所有 Kubernetes 资源**
   ```bash
   kubectl apply -k k8s/
   ```

2. **检查部署状态**
   ```bash
   kubectl get pods -n twitter-clone
   kubectl get services -n twitter-clone
   ```

3. **访问应用程序**
   ```bash
   # 使用 NodePort
   kubectl get service twitter-clone-nodeport -n twitter-clone
   
   # 使用 Ingress（如果已配置）
   kubectl get ingress -n twitter-clone
   ```

4. **扩展应用程序**
   ```bash
   kubectl scale deployment twitter-clone-app --replicas=5 -n twitter-clone
   ```

## Kubernetes 资源

应用程序包含全面的 Kubernetes 配置：

- **命名空间**：应用程序的隔离环境
- **ConfigMap**：应用程序配置
- **Secret**：敏感数据，如 JWT 密钥
- **Deployment**：具有 3 个副本的应用程序部署
- **Service**：ClusterIP 和 NodePort 服务
- **Ingress**：外部访问路由
- **HPA**：用于自动扩展的水平 Pod 自动扩展器
- **Kustomization**：使用 Kustomize 进行资源管理

## 监控和健康检查

应用程序提供多个监控端点：

- `/api/actuator/health` - 应用程序健康状态
- `/api/actuator/info` - 应用程序信息
- `/api/actuator/metrics` - 应用程序指标

## 安全功能

- 基于 JWT 的认证
- 使用 BCrypt 的密码加密
- CORS 配置
- 安全头
- 输入验证
- SQL 注入防护

## 数据库模式

应用程序使用 H2 内存数据库，包含以下主要实体：

- **Users**：用户账户和资料
- **Tweets**：推文内容和元数据
- **User_Follows**：关注关系
- **User_Likes**：推文点赞

## 测试策略

项目包含全面的测试：

- **单元测试**：服务层和仓库测试
- **集成测试**：控制器和完整应用程序测试
- **测试覆盖率**：JaCoCo 集成用于覆盖率报告
- **测试配置文件**：测试的独立配置

## 性能考虑

- 数据库连接的连接池
- 大结果集的分页
- 频繁访问数据的缓存策略
- 使用 Kubernetes HPA 的水平扩展
- 配置的资源限制和请求

## 开发指南

1. **代码风格**：遵循 Spring Boot 最佳实践
2. **测试**：保持高测试覆盖率（>80%）
3. **安全**：始终验证输入并使用参数化查询
4. **文档**：保持 API 文档更新
5. **监控**：添加适当的日志记录和指标

## 故障排除

### 常见问题

1. **应用程序无法启动**
   - 检查 Java 版本（需要 Java 8+）
   - 验证端口 8080 是否可用
   - 检查应用程序日志

2. **数据库连接问题**
   - H2 是内存数据库，重启时数据会丢失
   - 检查 H2 控制台的数据库状态

3. **认证问题**
   - 验证 JWT 密钥配置
   - 检查令牌过期设置

4. **Kubernetes 部署问题**
   - 验证集群连接
   - 检查资源配额
   - 查看 Pod 日志：`kubectl logs -f deployment/twitter-clone-app -n twitter-clone`

## 贡献

1. Fork 仓库
2. 创建功能分支
3. 进行更改
4. 为新功能添加测试
5. 确保所有测试通过
6. 提交拉取请求

## 许可证

本项目采用 MIT 许可证 - 详情请参阅 LICENSE 文件。

## 支持

如需支持和问题咨询：
- 在仓库中创建问题
- 查看故障排除部分
- 查阅 API 文档