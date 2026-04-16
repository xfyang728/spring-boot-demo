# Spring Boot Demo

一个功能完整的 Spring Boot 演示项目，集成了 PostgreSQL 数据库、Redis 缓存、WebSocket 实时通信和 Vue.js 前端。

## 技术栈

### 后端
- **框架**: Spring Boot 2.7.10
- **Java 版本**: 1.8
- **数据库**: PostgreSQL
- **连接池**: Druid
- **缓存**: Redis
- **实时通信**: WebSocket
- **工具库**: Hutool、Lombok

### 前端
- **框架**: Vue 3
- **构建工具**: Vite
- **HTTP 客户端**: Axios
- **图表库**: ECharts

## 项目结构

```
spring-boot-demo/
├── src/
│   ├── main/
│   │   ├── java/com/example/springbootdemo/
│   │   │   ├── controller/     # REST API 控制器
│   │   │   ├── model/         # 数据模型
│   │   │   ├── service/       # 业务逻辑层
│   │   │   ├── utils/         # 工具类
│   │   │   ├── websocket/     # WebSocket 配置
│   │   │   └── SpringBootDemoApplication.java
│   │   └── resources/
│   │       ├── application.yaml  # 应用配置
│   │       └── db/init.sql       # 数据库初始化脚本
│   └── test/                     # 测试类
├── web/                          # Vue.js 前端项目
│   ├── src/
│   │   ├── components/       # Vue 组件
│   │   ├── assets/           # 静态资源
│   │   ├── App.vue           # 根组件
│   │   └── main.js           # 入口文件
│   └── package.json
├── pom.xml                      # Maven 配置
└── mvnw                         # Maven 包装器
```

## 快速开始

### 环境要求

- JDK 1.8+
- Node.js 14+
- PostgreSQL 12+
- Redis 6+

### 后端启动

```bash
# 初始化数据库
psql -U postgres -f src/main/resources/db/init.sql

# 启动 Spring Boot 应用
./mvnw spring-boot:run
```

服务将在 `http://localhost:7878` 启动

### 前端启动

```bash
cd web
npm install
npm run dev
```

前端将在 `http://localhost:5173` 启动

## 主要功能

### 控制器端点

| 路径 | 方法 | 描述 |
|------|------|------|
| `/ai/query` | POST | AI 查询接口 |
| `/demo/test` | GET | 测试接口 |

### 数据库配置

默认配置：
- **主机**: 127.0.0.1
- **端口**: 5432
- **数据库**: postgres
- **用户名**: postgres
- **密码**: postgres

### Redis 配置

- **主机**: localhost
- **端口**: 6379
- **数据库**: 0

## 开发说明

项目采用前后端分离架构：

1. **后端** (Spring Boot) - 负责业务逻辑和数据处理
2. **前端** (Vue.js) - 负责用户界面展示

CORS 已配置，允许跨域请求。

## 许可证

MIT License
