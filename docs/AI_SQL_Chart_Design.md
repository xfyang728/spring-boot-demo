# AI 生成 SQL 与图表可视化系统设计文档

## 1. 项目概述

### 1.1 项目背景

spring-boot-demo 是一个集成 PostgreSQL 数据库、Redis 缓存、WebSocket 实时通信和 Vue.js 前端的 Spring Boot 演示项目。其核心功能之一是通过 AI (Ollama/Llama) 自动生成 SQL 查询语句，并将查询结果以图表形式可视化展示。

### 1.2 核心功能

- **AI SQL 生成**: 用户输入自然语言需求，AI 自动生成 PostgreSQL SQL
- **智能查询执行**: 后端执行 SQL 并返回结果
- **图表可视化**: 根据数据特征自动推荐并生成图表
- **实时交互**: 支持用户编辑生成的 SQL 并重新查询

---

## 2. 现有架构分析

### 2.1 当前流程

```
用户输入自然语言 → Ollama(Llama) → SQL → 后端执行 → 前端图表展示
```

### 2.2 现有组件

| 组件 | 技术 | 职责 |
|------|------|------|
| 前端 | Vue 3 + Vite | 用户界面、图表渲染 |
| AI 服务 | Ollama + Llama 3.2 | SQL 生成 |
| 后端 | Spring Boot 2.7.10 | SQL 执行、API 提供 |
| 数据库 | PostgreSQL | 数据存储 |
| 缓存 | Redis (已配置) | 预留缓存层 |

### 2.3 存在的问题

| 问题类型 | 具体问题 | 影响程度 |
|----------|----------|----------|
| **Prompt 不完善** | 缺少详细 Schema、类型信息、示例 | 高 |
| **安全风险** | 无 SQL 验证，可能存在注入风险 | 高 |
| **错误处理弱** | SQL 执行失败无自动修复机制 | 中 |
| **图表硬编码** | 用户需手动选择图表类型 | 中 |
| **无缓存** | 重复查询浪费 AI 和数据库资源 | 低 |
| **无重试机制** | AI 生成失败时用户体验差 | 中 |

---

## 3. 系统架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端层 (Vue.js)                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │ 用户输入  │→ │ Prompt   │→ │ 图表渲染  │→ │ 错误提示    │  │
│  │ 组件     │  │ 构建器    │  │ 组件     │  │ 组件        │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API 网关层                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │ 限流     │→ │ 认证     │→ │ 路由     │→ │ 日志        │   │
│  │ (Redis)  │  │ JWT      │  │         │  │             │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘   │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      AI 服务层                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ Prompt 管理器 │→ │ SQL 生成器   │→ │ 多模型支持 (Ollama)  │  │
│  │ - Schema     │  │              │  │ - Llama 3.2         │  │
│  │ - 示例       │  │              │  │ - GPT-4 (预留)      │  │
│  │ - 规则       │  │              │  │ - Claude (预留)    │  │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    安全验证层                                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ SQL 语法验证  │→ │ 注入检测     │→ │ 权限/资源限制        │   │
│  │ - 关键字     │  │ - 黑名单     │  │ - 行数限制          │   │
│  │ - 复杂度     │  │ - 白名单     │  │ - 查询超时          │   │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                      数据执行层                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐  │
│  │ 智能重试机制  │→ │ 结果缓存     │→ │ 查询优化              │   │
│  │ - 自动修正   │  │ - Redis      │  │ - 索引提示           │   │
│  │ - 降级策略   │  │ - TTL        │  │ - 执行计划分析       │   │
│  └──────────────┘  └──────────────┘  └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 数据流设计

```
1. 用户输入 → Prompt 构建 → AI 生成 SQL
2. SQL 验证 → 语法检查 → 注入检测 → 危险关键字过滤
3. SQL 执行 → 结果缓存 → 响应返回
4. 数据分析 → 图表推荐 → 图表渲染
5. 错误发生 → 错误分类 → 自动修复 → 重试机制
```

---

## 4. 核心模块设计

### 4.1 Prompt 管理器 (PromptManager)

#### 职责
- 管理和构建 AI 提示词
- 包含完整的数据库 Schema 信息
- 提供 SQL 生成规则和示例

#### 数据结构

```java
public class PromptTemplate {
    private String systemPrompt;
    private DatabaseSchema schema;
    private List<SqlExample> examples;
    private SqlGenerationRules rules;
}

public class DatabaseSchema {
    private String tableName;
    private List<ColumnInfo> columns;
}

public class ColumnInfo {
    private String columnName;
    private String dataType;
    private String description;
    private boolean nullable;
}

public class SqlGenerationRules {
    private Set<String> allowedOperations;
    private Set<String> forbiddenKeywords;
    private int maxResultRows;
    private int maxQueryTimeout;
}
```

#### System Prompt 模板

```
你是 PostgreSQL SQL 专家。

数据库信息：
- 表名: car_sale
- 字段定义:
  * 日期 (varchar, 格式: YYYY-MM-DD HH:MI:SS, 描述: 订单日期)
  * 订单id (varchar, 描述: 唯一订单标识)
  * 车型 (varchar, 描述: 汽车型号)
  * 配置 (varchar, 描述: 车型配置: 高配/中配/低配)
  * 是否购买车险 (varchar, 描述: 是/否)
  * 经销商 (varchar, 描述: 经销商名称)
  * 省份 (varchar, 描述: 省份名称)
  * 订单数量 (int, 描述: 订单数量)
  * 销售额 (decimal, 描述: 销售金额)
  * 折扣 (decimal, 描述: 折扣率)

生成规则：
1. 只输出 SQL 语句，不要其他解释
2. 必须使用正确的 PostgreSQL 语法
3. 禁止使用的操作: DROP, DELETE, TRUNCATE, ALTER, CREATE, INSERT, UPDATE, GRANT, REVOKE
4. 必须指定 LIMIT 限制返回行数 (最大 1000 行)
5. 日期字段使用 CAST(日期 AS DATE) 进行类型转换
6. 避免使用复杂子查询，优先使用简单高效的查询
7. 不要在 SQL 中包含注释
```

### 4.2 SQL 安全验证器 (SqlSecurityValidator)

#### 职责
- 验证 SQL 语法正确性
- 检测潜在 SQL 注入攻击
- 检查查询复杂度

#### 验证规则

```java
public class SqlValidationRule {
    // 1. SQL 长度限制
    private static final int MAX_SQL_LENGTH = 500;

    // 2. 危险关键字黑名单
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
        "DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT",
        "UPDATE", "GRANT", "REVOKE", "EXEC", "EXECUTE", ";", "--"
    );

    // 3. 必须以 SELECT 开头
    private static final Set<String> ALLOWED_PREFIXES = Set.of(
        "SELECT", "WITH"
    );

    // 4. 最大 LIMIT 值
    private static final int MAX_LIMIT = 1000;
    private static final int DEFAULT_LIMIT = 100;

    // 5. 最大 JOIN 数量
    private static final int MAX_JOINS = 3;

    // 6. 最大 WHERE 条件数
    private static final int MAX_WHERE_CONDITIONS = 10;
}
```

#### 验证流程

```
SQL 输入
    ↓
长度检查 (> 500 字符?)[拒绝]
    ↓
危险关键字检查 (包含黑名单词?)[拒绝]
    ↓
前缀检查 (以 SELECT/WITH 开头?)[拒绝]
    ↓
LIMIT 检查 (未指定? 添加 DEFAULT_LIMIT)
    ↓
复杂度检查 (JOIN/WHERE 数量?)[警告]
    ↓
通过验证
```

### 4.3 智能错误修复器 (IntelligentErrorCorrector)

#### 职责
- 分类 SQL 执行错误
- 自动尝试修复常见错误
- 提供用户友好的错误提示

#### 错误类型与修复策略

| 错误类型 | 错误特征 | 修复策略 |
|----------|----------|----------|
| 类型转换错误 | "cannot be cast" | 添加适当的 CAST |
| EXTRACT 错误 | "pg_catalog.extract" | 使用 CAST 包裹日期字段 |
| 不存在函数 | "function does not exist" | 简化函数调用 |
| 语法错误 | "syntax error" | 返回给用户手动修正 |
| 超时错误 | "timeout" | 添加 LIMIT 或优化查询 |

#### 修复示例

```java
public class SqlAutoCorrector {

    public String correctSql(String sql, String errorMessage) {
        // 1. 修复 EXTRACT 函数问题
        if (errorMessage.contains("pg_catalog.extract") &&
            sql.contains("EXTRACT(DAY FROM")) {
            return sql.replaceAll(
                "EXTRACT\\(DAY FROM\\s*(\\w+)\\)",
                "EXTRACT(DAY FROM CAST($1 AS DATE))"
            );
        }

        // 2. 修复类型转换问题
        if (errorMessage.contains("cannot be cast") ||
            errorMessage.contains("undefined cast")) {
            // 移除可能导致问题的类型转换
            return sql.replaceAll("::\\w+", "")
                     .replaceAll("::\\w+\\(\\)", "");
        }

        // 3. 移除多余的引号
        if (sql.contains("\"") && !sql.contains("\"")) {
            return sql.replaceAll("\"(\\w+)\"", "$1");
        }

        return null; // 无法自动修复
    }
}
```

### 4.4 智能图表推荐器 (ChartRecommendationEngine)

#### 职责
- 分析数据特征
- 推荐最适合的图表类型
- 自动生成 ECharts 配置

#### 推荐规则

```javascript
function recommendChartType(data, columns) {
    const rowCount = data.length;
    const numericColumns = columns.filter(col =>
        typeof data[0]?.[col] === 'number'
    );
    const dateColumns = columns.filter(col =>
        String(data[0]?.[col])?.match(/^\d{4}-\d{2}-\d{2}/)
    );
    const categoryColumns = columns.filter(col =>
        typeof data[0]?.[col] === 'string' &&
        !dateColumns.includes(col)
    );

    // 推荐逻辑
    if (rowCount === 0) {
        return { type: 'empty', message: '无数据' };
    }

    // 时间序列 + 数值 → 折线图
    if (dateColumns.length >= 1 && numericColumns.length >= 1) {
        return {
            type: rowCount > 50 ? 'line' : 'bar',
            title: '趋势图',
            xAxis: dateColumns[0],
            yAxis: numericColumns[0]
        };
    }

    // 分类 + 数值 → 柱状图
    if (categoryColumns.length >= 1 && numericColumns.length >= 1) {
        return {
            type: 'bar',
            title: '分类对比图',
            xAxis: categoryColumns[0],
            yAxis: numericColumns[0]
        };
    }

    // 单一数值分布 → 饼图
    if (numericColumns.length === 1 && rowCount <= 10) {
        return {
            type: 'pie',
            title: '占比分布图',
            valueColumn: numericColumns[0]
        };
    }

    // 大数据集 → 表格
    if (rowCount > 100) {
        return {
            type: 'table',
            title: '数据表格'
        };
    }

    // 默认表格
    return { type: 'table', title: '数据详情' };
}
```

### 4.5 查询缓存服务 (QueryCacheService)

#### 职责
- 缓存 SQL 查询结果
- 减少数据库和 AI 压力
- 提供缓存管理

#### 缓存策略

```java
@Service
public class CachedQueryService {

    private static final long DEFAULT_TTL = 300; // 5分钟
    private static final long POPULAR_QUERY_TTL = 1800; // 30分钟

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public List queryWithCache(String sql, CacheStrategy strategy) {
        String cacheKey = generateCacheKey(sql);
        List cachedResult = (List) redisTemplate.opsForValue().get(cacheKey);

        if (cachedResult != null) {
            return cachedResult;
        }

        List result = executeQuery(sql);
        long ttl = determineTtl(strategy, sql);
        redisTemplate.opsForValue().set(cacheKey, result, ttl, TimeUnit.SECONDS);

        return result;
    }

    private long determineTtl(CacheStrategy strategy, String sql) {
        if (strategy == CacheStrategy.POPULAR) {
            return POPULAR_QUERY_TTL;
        }
        return DEFAULT_TTL;
    }
}
```

---

## 5. API 设计

### 5.1 核心 API

#### SQL 生成与执行

```
POST /api/ai/query
Content-Type: application/json

Request:
{
  "question": "每天的订单数量是多少",
  "tableName": "car_sale"
}

Response:
{
  "success": true,
  "data": {
    "sql": "SELECT CAST(日期 AS DATE) as sale_date, SUM(订单数量) as total_count FROM car_sale GROUP BY CAST(日期 AS DATE) ORDER BY sale_date LIMIT 100",
    "results": [
      {"sale_date": "2020-05-01", "total_count": 25},
      {"sale_date": "2020-05-02", "total_count": 30}
    ],
    "rowCount": 2,
    "executionTime": 125
  },
  "chartRecommendation": {
    "type": "line",
    "title": "每日订单趋势"
  }
}
```

#### Schema 查询

```
GET /api/schema/{tableName}

Response:
{
  "tableName": "car_sale",
  "columns": [
    {"name": "日期", "type": "varchar", "nullable": true, "description": "订单日期"},
    {"name": "订单id", "type": "varchar", "nullable": false, "description": "唯一订单标识"},
    {"name": "订单数量", "type": "int", "nullable": false, "description": "订单数量"}
  ]
}
```

#### SQL 验证

```
POST /api/sql/validate
Content-Type: application/json

Request:
{
  "sql": "SELECT * FROM car_sale LIMIT 10"
}

Response:
{
  "valid": true,
  "warnings": [],
  "suggestions": ["建议添加具体的 SELECT 字段而非 *"]
}
```

### 5.2 错误响应格式

```json
{
  "success": false,
  "error": {
    "code": "SQL_EXECUTION_ERROR",
    "message": "SQL 执行失败",
    "details": "错误: 函数 pg_catalog.extract(unknown, character varying) 不存在",
    "suggestedFix": "将 EXTRACT(DAY FROM date) 修改为 EXTRACT(DAY FROM CAST(date AS DATE))",
    "userFriendlyMessage": "日期字段需要转换为日期类型，请修正 SQL 后重试"
  }
}
```

---

## 6. 数据库设计

### 6.1 Schema 信息表

```sql
CREATE TABLE IF NOT EXISTS database_schema (
    id SERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    column_name VARCHAR(100) NOT NULL,
    data_type VARCHAR(50),
    description TEXT,
    is_nullable BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_schema_table ON database_schema(table_name);
```

### 6.2 SQL 执行日志表

```sql
CREATE TABLE IF NOT EXISTS sql_execution_log (
    id SERIAL PRIMARY KEY,
    sql_text TEXT NOT NULL,
    execution_time_ms INTEGER,
    row_count INTEGER,
    status VARCHAR(20),
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 6.3 缓存管理表

```sql
CREATE TABLE IF NOT EXISTS query_cache_stats (
    id SERIAL PRIMARY KEY,
    cache_key VARCHAR(255) UNIQUE NOT NULL,
    hit_count INTEGER DEFAULT 0,
    last_accessed TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 7. 安全设计

### 7.1 SQL 注入防护

1. **参数化查询**: 所有用户输入必须参数化
2. **输入过滤**: 过滤特殊字符 `;`, `--`, `/*`, `*/`
3. **权限控制**: 只允许 SELECT 操作
4. **资源限制**: LIMIT 最大 1000 行

### 7.2 速率限制

```yaml
rate-limit:
  default: 60/minute
  ai-query: 10/minute
  sql-execute: 30/minute
```

### 7.3 审计日志

记录所有 SQL 执行：
- 执行时间
- SQL 内容
- 执行结果
- 用户标识
- IP 地址

---

## 8. 性能优化

### 8.1 缓存策略

| 缓存级别 | 内容 | TTL | 适用场景 |
|----------|------|-----|----------|
| L1 | Schema 信息 | 1小时 | 启动时加载 |
| L2 | 查询结果 | 5分钟 | 常规查询 |
| L3 | AI 生成结果 | 30分钟 | 热门查询 |

### 8.2 查询优化

1. 添加 LIMIT 限制
2. 使用索引提示
3. 分析执行计划
4. 慢查询日志

### 8.3 并发控制

- 最大并发查询: 50
- 查询超时: 30 秒
- 连接池大小: 20

---

## 9. 实施计划

### 9.1 第一阶段 (P0 - 核心功能)

- [ ] 完善 Prompt 提示词模板
- [ ] 添加 SQL 安全验证层
- [ ] 实现错误自动修复机制
- [ ] 添加 LIMIT 限制

### 9.2 第二阶段 (P1 - 增强功能)

- [ ] 智能图表推荐系统
- [ ] 查询结果缓存
- [ ] SQL 编辑器组件
- [ ] 执行日志记录

### 9.3 第三阶段 (P2 - 高级功能)

- [ ] Schema 自动发现
- [ ] 多模型支持
- [ ] 查询性能分析
- [ ] 高级缓存策略

---

## 10. 技术选型

| 组件 | 技术方案 | 备选方案 |
|------|----------|----------|
| AI 模型 | Ollama + Llama 3.2 | OpenAI GPT-4 |
| 图表库 | ECharts | Chart.js, D3.js |
| 缓存 | Redis | Memcached |
| 数据库 | PostgreSQL | MySQL, SQL Server |
| 后端 | Spring Boot 2.7.10 | Spring Boot 3.x |
| 前端 | Vue 3 + Vite | React + Next.js |

---

## 11. 监控与运维

### 11.1 关键指标

- SQL 执行成功率
- 平均执行时间
- 缓存命中率
- AI 响应时间
- 错误率

### 11.2 告警规则

- 执行时间 > 5秒
- 错误率 > 5%
- 缓存命中率 < 50%

---

---

## 13. 市场前景与竞品分析

### 13.1 市场概况

| 指标 | 数据 |
|------|------|
| 全球 BI 市场规模 (2024) | $133.5 亿美元 |
| 预计年复合增长率 (CAGR) | 10.3% |
| AI in Analytics 细分市场 | $30-50 亿美元 |
| 自然语言查询市场份额 | 快速增长中 |

### 13.2 核心竞品

| 竞品 | 定位 | 定价 | NL2SQL | 图表 | 开源 |
|------|------|------|--------|------|------|
| ThoughtSpot | 企业级 BI | $50K+/年 | ✅ 强 | ✅ 强 | ❌ |
| Power BI + Copilot | 微软生态 | $10-20/人/月 | ✅ 强 | ✅ 强 | ❌ |
| Tableau | 可视化分析 | $70/人/月 | ⚠️ 基础 | ✅ 强 | ❌ |
| Metabase | 轻量级 BI | 开源免费 | ⚠️ 基础 | ✅ 中 | ✅ |
| SQLChat | NL2SQL 工具 | 开源免费 | ✅ 中 | ❌ | ✅ |
| Vanna.ai | NL2SQL 库 | 开源免费 | ✅ 中 | ❌ | ✅ |
| **本项目** | **NL2SQL + 图表** | **开源免费** | **✅ 中** | **✅ 中** | **✅** |

### 13.3 SWOT 分析

| 维度 | 内容 |
|------|------|
| **优势** | 开源免费、私有部署、Vue + Spring Boot 架构、Ollama 支持 |
| **劣势** | 功能不完整、无商业支持、AI 能力弱、无数据治理 |
| **机会** | 数据民主化需求、AI 爆发、信创政策、开源生态 |
| **威胁** | 大厂竞争、技术迭代、付费意愿、数据安全 |

### 13.4 商业化路径

```
阶段 1 (MVP): 专注 NL2SQL + 图表，强化 Prompt + 安全验证
阶段 2 (商业化): 垂直行业 (金融/医疗) + 企业级功能
阶段 3 (生态): 集成 LangChain/Dify，成为 AI Flow 节点
```

---

## 14. 使用场景分析

### 14.1 适用场景

| 场景 | 传统方式耗时 | 本项目耗时 | 提升效率 |
|------|-------------|-----------|----------|
| 月度销售报表 | 数据工程师开发 2-3 天 | 10 分钟 | 120 倍 |
| 临时数据查询 | 提交工单等待 4-8 小时 | 即时响应 | 240 倍 |
| 多维度分析 | 需要写复杂 SQL | 自然语言描述 | 50 倍 |
| 快速原型开发 | 前端 + 后端 + DBA 协作 | 一个人搞定 | 3 人 → 1 人 |
| 企业内部数据门户 | 购买 Tableau $70/人/月 | 开源免费 | 成本降低 90% |

### 14.2 不适用场景

| 场景 | 原因 | 替代方案 |
|------|------|----------|
| 复杂企业级 BI | 缺乏权限管理、数据治理 | ThoughtSpot、Power BI |
| 实时流数据处理 | 仅支持静态查询 | Kafka + Flink + Grafana |
| 超大规模数据查询 | 无查询优化器 | Presto、Trino、Snowflake |

### 14.3 对比传统软件

| 维度 | 传统 BI (Tableau/Power BI) | **本项目** | 优劣 |
|------|---------------------------|-----------|------|
| 学习成本 | 高 (需要培训) | 低 (自然语言) | ✅ 本项目胜 |
| SQL 依赖 | 需要写 SQL | 自动生成 | ✅ 本项目胜 |
| 部署成本 | $70-200/人/月 | 开源免费 | ✅ 本项目胜 |
| 数据源支持 | 100+ 连接器 | PostgreSQL 为主 | ❌ 本项目败 |
| 图表美观度 | 50+ 图表类型 | 基础图表 | ❌ 本项目败 |
| 企业特性 | SSO/权限/审计 | 缺失 | ❌ 本项目败 |

### 14.4 用户画像

```
✅ 完美匹配:
├── 个人开发者 (20%) - 快速数据分析、简历项目
├── 小型创业公司 (30%) - 降低 BI 成本、快速迭代
├── 中型企业部门 (25%) - 业务人员自助分析
├── 政府/国企 (15%) - 信创替代、数据安全
└── 高校/研究机构 (10%) - 教学演示、数据探索

❌ 不适合:
├── 超大型企业 - 需要完整企业级 BI
├── 实时数据处理需求 - 需要流式计算平台
├── 超大规模数据 (PB 级) - 需要专用数据仓库
└── 强合规行业 (金融/医疗) - 需要认证的安全审计
```

### 14.5 核心定位

```
本项目 = "SQL 查询的智能助手" + "轻量级图表生成器"

不是: 替代 Tableau/Power BI 的企业级 BI
而是: 降低数据分析门槛的入口工具
```

---

## 12. 附录

### 12.1 参考资料

- [PostgreSQL EXTRACT 文档](https://www.postgresql.org/docs/current/functions-datetime.html#FUNCTIONS-DATETIME-EXTRACT)
- [ECharts 文档](https://echarts.apache.org/zh/index.html)
- [Vue 3 文档](https://vuejs.org/)

### 12.2 术语表

| 术语 | 说明 |
|------|------|
| Prompt | 给 AI 模型的输入提示 |
| SQL 注入 | 通过恶意输入破坏 SQL 语句 |
| LIMIT | 限制查询返回行数 |
| Schema | 数据库表结构定义 |
| TTL | 缓存生存时间 |
| NL2SQL | Natural Language to SQL，自然语言转 SQL |
| BI | Business Intelligence，商业智能 |
