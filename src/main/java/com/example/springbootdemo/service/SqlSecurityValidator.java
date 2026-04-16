package com.example.springbootdemo.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SqlSecurityValidator {

    private static final int MAX_SQL_LENGTH = 500;
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
        "DROP", "DELETE", "TRUNCATE", "ALTER", "CREATE", "INSERT",
        "UPDATE", "GRANT", "REVOKE", "EXEC", "EXECUTE"
    );
    private static final Set<String> ALLOWED_PREFIXES = Set.of("SELECT", "WITH");
    private static final int MAX_LIMIT = 1000;
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_JOINS = 3;
    private static final int MAX_WHERE_CONDITIONS = 10;

    private static final Pattern COMMENT_PATTERN = Pattern.compile("--|/\\*|\\*/");
    private static final Pattern SEMICOLON_PATTERN = Pattern.compile(";");

    public ValidationResult validate(String sql) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);

        if (sql == null || sql.trim().isEmpty()) {
            result.setValid(false);
            result.addError("SQL 语句不能为空");
            return result;
        }

        String trimmedSql = sql.trim();
        String upperSql = trimmedSql.toUpperCase();

        if (trimmedSql.length() > MAX_SQL_LENGTH) {
            result.setValid(false);
            result.addError("SQL 语句过长，最大允许 " + MAX_SQL_LENGTH + " 字符");
        }

        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                result.setValid(false);
                result.addError("禁止使用危险关键字: " + keyword);
            }
        }

        boolean hasAllowedPrefix = ALLOWED_PREFIXES.stream()
            .anyMatch(prefix -> upperSql.startsWith(prefix));
        if (!hasAllowedPrefix) {
            result.setValid(false);
            result.addError("SQL 必须以 SELECT 或 WITH 开头");
        }

        if (COMMENT_PATTERN.matcher(trimmedSql).find()) {
            result.addWarning("SQL 不应包含注释");
        }

        if (SEMICOLON_PATTERN.matcher(trimmedSql).find()) {
            result.setValid(false);
            result.addError("SQL 不应包含分号");
        }

        long joinCount = countOccurrences(upperSql, "JOIN");
        if (joinCount > MAX_JOINS) {
            result.addWarning("JOIN 数量过多，最多允许 " + MAX_JOINS + " 个");
        }

        long whereCount = countOccurrences(upperSql, "WHERE");
        if (whereCount > MAX_WHERE_CONDITIONS) {
            result.addWarning("WHERE 条件过多，最多允许 " + MAX_WHERE_CONDITIONS + " 个");
        }

        if (!upperSql.contains("LIMIT")) {
            result.addSuggestion("建议添加 LIMIT 限制以提高查询性能");
        }

        if (upperSql.contains("SELECT *")) {
            result.addSuggestion("建议使用具体的字段名替代 *");
        }

        return result;
    }

    public String sanitizeSql(String sql) {
        if (sql == null) {
            return null;
        }

        String sanitized = sql.trim();

        sanitized = sanitized.replaceAll(";$", "");

        if (!sanitized.toUpperCase().contains("LIMIT")) {
            sanitized += " LIMIT " + DEFAULT_LIMIT;
        }

        return sanitized;
    }

    public String enforceLimit(String sql, int limit) {
        if (sql == null) {
            return null;
        }

        String upperSql = sql.toUpperCase();
        int existingLimitPos = upperSql.lastIndexOf("LIMIT");

        if (existingLimitPos > 0) {
            String beforeLimit = sql.substring(0, existingLimitPos).trim();
            int enforcedLimit = Math.min(limit, MAX_LIMIT);
            return beforeLimit + " LIMIT " + enforcedLimit;
        } else {
            int enforcedLimit = Math.min(limit, MAX_LIMIT);
            return sql.trim() + " LIMIT " + enforcedLimit;
        }
    }

    private long countOccurrences(String str, String substr) {
        long count = 0;
        int idx = 0;
        while ((idx = str.indexOf(substr, idx)) != -1) {
            count++;
            idx += substr.length();
        }
        return count;
    }

    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        private List<String> suggestions;

        public ValidationResult() {
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.suggestions = new ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        public List<String> getSuggestions() {
            return suggestions;
        }

        public void addSuggestion(String suggestion) {
            this.suggestions.add(suggestion);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            if (!errors.isEmpty()) {
                sb.append("错误: ").append(String.join(", ", errors)).append("; ");
            }
            if (!warnings.isEmpty()) {
                sb.append("警告: ").append(String.join(", ", warnings)).append("; ");
            }
            if (!suggestions.isEmpty()) {
                sb.append("建议: ").append(String.join(", ", suggestions));
            }
            return sb.toString();
        }
    }
}
