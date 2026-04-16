# Spring Boot Demo

一个功能完整的 Spring Boot 演示项目，集成了 PostgreSQL 数据库、AI SQL 生成、智能图表推荐和 Vue.js 前端。

![AI SQL Query Helper](AI SQL query helper.png)

## 技术栈

### 后端
- **框架**: Spring Boot 2.7.10
- **Java 版本**: 1.8
- **数据库**: PostgreSQL
- **连接池**: Druid
- **缓存**: Redis
- **AI**: Ollama (Llama 3.2)
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
- Ollama (可选，用于 AI SQL 生成)

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

### AI SQL 生成
- 使用自然语言描述查询需求
- AI 自动生成 PostgreSQL SQL 语句
- 自动执行查询并返回结果
- 支持 SQL 验证和自动修正

### 智能图表推荐
- 自动分析查询结果数据结构
- 智能推荐最适合的图表类型（柱状图、折线图、饼图）
- 支持手动切换图表类型
- 动态构建 ECharts 配置

### 数据库特性
- 动态 Schema 发现（自动获取表结构）
- SQL 执行日志记录
- 查询结果缓存
- 支持按省份、日期、月份等多种维度统计

### API 接口

| 路径 | 方法 | 描述 |
|------|------|------|
| `/ai/query` | POST | AI 自然语言查询 |
| `/ai/execute` | POST | 直接执行 SQL |
| `/ai/validate` | POST | SQL 验证 |
| `/ws` | WebSocket | 实时日志推送 |

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

1. **后端** (Spring Boot) - 负责业务逻辑、数据处理和 AI 交互
2. **前端** (Vue.js) - 负责用户界面、图表渲染和交互

CORS 已配置，允许跨域请求。

## 许可证

MIT License
