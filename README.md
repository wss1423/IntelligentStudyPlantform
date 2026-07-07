# 智能化学习服务平台 (Intelligent Study Plantform) — 在线教育平台

## 项目简介

智能化学习服务平台是一个基于微服务架构的在线教育平台后端系统，提供课程管理、在线学习、考试测评、支付交易等完整的在线教育解决方案。项目采用 Spring Cloud Alibaba 技术栈，实现了服务注册发现、配置中心、网关路由、分布式事务、任务调度等基础设施能力。

## 技术栈

| 分类 | 技术 |
|------|------|
| **核心框架** | Spring Boot 3.3.5, Spring Cloud 2023.0.3 |
| **微服务** | Spring Cloud Alibaba (Nacos 注册中心 & 配置中心), Spring Cloud Gateway |
| **ORM** | MyBatis-Plus 3.5.9 |
| **数据库** | MySQL, Redis (Redisson 客户端) |
| **搜索引擎** | Elasticsearch 7.12.1 |
| **熔断降级** | Sentinel |
| **分布式事务** | Seata 1.5.1 |
| **分布式调度** | XXL-Job 2.3.1 |
| **AI** | Spring AI 1.0.0-M6 |
| **消息服务** | 腾讯云 SMS, 阿里云短信 |
| **对象存储** | 阿里云 OSS, 腾讯云 COS |
| **媒体处理** | 腾讯云 VOD (点播) |
| **支付** | 支付宝 SDK |
| **容器化** | Docker |
| **Java版本** | Java 17 |
| **构建工具** | Maven (多模块) |

## 模块架构

```
tjxt/
├── tj-common         # 公共基础模块（工具类、通用配置、统一异常处理）
├── tj-gateway        # API 网关（Spring Cloud Gateway，统一入口、路由、鉴权）
├── tj-auth           # 认证授权中心
│   ├── tj-auth-common          # 认证公共模块
│   ├── tj-auth-service         # 认证服务（JWT 签发、登录、权限管理）
│   ├── tj-auth-gateway-sdk     # 网关鉴权 SDK
│   └── tj-auth-resource-sdk    # 资源服务鉴权 SDK
├── tj-api            # Feign 远程调用接口声明（服务间通信）
├── tj-user           # 用户服务（用户管理、角色权限）
├── tj-course         # 课程服务（课程分类、课程管理、目录管理）
├── tj-learning       # 学习服务（学习计划、学习记录、笔记、问答互动、签到、积分排行）
├── tj-exam           # 考试服务（题库管理、考试测评）
├── tj-media          # 媒体服务（文件上传、视频管理）
├── tj-trade          # 交易服务（购物车、订单）
├── tj-pay            # 支付服务
│   ├── tj-pay-api              # 支付 Feign 接口
│   ├── tj-pay-domain           # 支付领域模型
│   └── tj-pay-service          # 支付核心服务
├── tj-promotion      # 营销服务（优惠券、推广活动）
├── tj-message        # 消息通知服务
│   ├── tj-message-api          # 消息 Feign 接口
│   ├── tj-message-domain       # 消息领域模型
│   └── tj-message-service      # 消息核心服务（短信、站内信）
├── tj-search         # 搜索服务（Elasticsearch 全文检索）
├── tj-remark         # 评价服务（课程评价、评分）
└── tj-data           # 数据中心（运营数据看板、排行榜）
```

## 核心功能

### 1. 课程管理
- 课程分类管理（多级分类）
- 课程基本信息管理
- 课程目录/章节管理
- 课程上下架流程

### 2. 在线学习
- 学习计划制定与跟踪
- 学习进度记录（视频进度、章节完成）
- 笔记功能
- 问答互动（提问、回复）
- 签到打卡
- 积分系统与排行榜

### 3. 考试测评
- 题库管理
- 在线考试
- 自动评分

### 4. 交易与支付
- 购物车
- 订单管理
- 支付宝支付集成
- 退款流程

### 5. 营销推广
- 优惠券管理
- 营销活动

### 6. 搜索
- 课程全文检索 (Elasticsearch)
- 搜索建议

### 7. 评价系统
- 课程评价与评分
- 评价管理

### 8. 消息通知
- 短信通知 (腾讯云 / 阿里云)
- 站内信

### 9. 认证与授权
- JWT 令牌认证
- 基于角色的权限控制 (RBAC)
- 网关统一鉴权
- 菜单与权限管理

### 10. 运营数据
- 数据看板
- 排行榜

## 基础设施

| 组件 | 用途 |
|------|------|
| Nacos | 服务注册与发现、配置中心 |
| Spring Cloud Gateway | API 网关、路由转发、跨域、熔断 |
| Sentinel | 服务熔断降级、流量控制 |
| Seata | 分布式事务 (AT 模式) |
| XXL-Job | 分布式定时任务调度 |
| Redisson | Redis 分布式锁、缓存 |

## 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis
- Nacos 2.x
- Elasticsearch 7.12.1 (可选，用于搜索服务)
- XXL-Job (可选，用于任务调度)
- Seata (可选，用于分布式事务)

## 快速启动

1. **启动基础设施**：确保 Nacos、MySQL、Redis 已启动。

2. **配置 Nacos**：在各模块的 `application.yml` 和 `bootstrap.yml` 中配置 Nacos 地址、命名空间等信息。

3. **初始化数据库**：在各模块 SQL 目录下执行建表脚本。

4. **构建项目**：
   ```bash
   mvn clean package -DskipTests
   ```

5. **启动服务**（按依赖顺序）：
   - `tj-gateway` — API 网关 (端口 8080)
   - `tj-auth-service` — 认证服务
   - `tj-user` — 用户服务
   - `tj-course` — 课程服务
   - `tj-learning` — 学习服务
   - 其他业务服务按需启动

6. **Docker 部署**（参考 `Dockerfile` 和 `startup.sh`）：
   ```bash
   ./startup.sh -c <container_name> -n <project_name> -d <project_path> -p <port>
   ```

## 模块端口分配

| 服务 | 默认端口 |
|------|---------|
| tj-gateway | 8080 |
| tj-auth-service | 8101 |
| tj-user | 8102 |
| tj-course | 8103 |
| tj-learning | 8104 |
| tj-media | 8105 |
| tj-trade | 8106 |
| tj-pay-service | 8107 |
| tj-exam | 8108 |
| tj-promotion | 8109 |
| tj-message-service | 8110 |
| tj-search | 8111 |
| tj-remark | 8112 |
| tj-data | 8113 |

## 许可证

本项目仅供学习参考，未经授权不得用于商业用途。
