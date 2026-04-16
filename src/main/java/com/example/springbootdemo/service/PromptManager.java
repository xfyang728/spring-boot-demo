package com.example.springbootdemo.service;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class PromptManager {

    private SchemaDiscoveryService schemaDiscoveryService;
    private String systemPrompt;

    public PromptManager(SchemaDiscoveryService schemaDiscoveryService) {
        this.schemaDiscoveryService = schemaDiscoveryService;
    }

    @PostConstruct
    public void init() {
        this.systemPrompt = buildSystemPrompt();
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 PostgreSQL SQL 专家。\n");
        sb.append("\n");
        sb.append(schemaDiscoveryService.getSchemaDescription());
        sb.append("\n");
        sb.append("【强制输出规则】必须严格遵守以下所有规则：\n");
        sb.append("1. 只输出纯 SQL，不要任何前缀、后缀、说明文字、代码块标记（如 ```sql```）\n");
        sb.append("2. 不要包含分号（;）\n");
        sb.append("3. LIMIT 限制最大 100 行，不要写 LIMIT 1000\n");
        sb.append("4. 【重要】必须使用数据库中实际的列名（如 province、date、value），不要使用中文列名（如\"省份\"、\"日期\"、\"销售额\"）\n");
        sb.append("5. PostgreSQL 标识符如果与关键字冲突或包含特殊字符才使用双引号，否则可以直接写列名\n");
        sb.append("6. 禁止使用的操作: DROP, DELETE, TRUNCATE, ALTER, CREATE, INSERT, UPDATE, GRANT, REVOKE\n");
        sb.append("7. 【重要】按日期分组统计时，必须使用 CAST(date AS DATE) 提取纯日期，不能直接用 date 分组\n");
        sb.append("8. 不要在 SQL 中包含注释（-- 或 /* */）\n");
        sb.append("9. 不要使用反引号（`），PostgreSQL 不支持反引号\n");
        sb.append("10. 优先使用简单的 JOIN，避免过多表连接\n");
        sb.append("11. 按日期统计示例（正确）：SELECT CAST(date AS DATE) AS date, COUNT(*) FROM car_sale GROUP BY CAST(date AS DATE) ORDER BY date LIMIT 100\n");
        sb.append("    按省份统计示例（正确）：SELECT province, SUM(value) AS total FROM car_sale GROUP BY province ORDER BY total DESC LIMIT 100\n");
        sb.append("    按月份统计示例（正确）：SELECT TO_CHAR(CAST(date AS DATE), 'YYYY-MM') AS month, SUM(value) FROM car_sale GROUP BY TO_CHAR(CAST(date AS DATE), 'YYYY-MM') ORDER BY month LIMIT 100\n");
        sb.append("    错误示例：SELECT date, COUNT(*) FROM car_sale GROUP BY date （错误：没有转换日期类型）\n");

        String prompt = sb.toString();
        System.out.println("========== [System Prompt for AI] ==========");
        System.out.println(prompt);
        System.out.println("==============================================");
        return prompt;
    }

    public String buildPrompt(String userQuestion) {
        return systemPrompt + "\n\n用户问题: " + userQuestion;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void refreshSchema() {
        schemaDiscoveryService.refreshSchemaCache();
        this.systemPrompt = buildSystemPrompt();
    }
}