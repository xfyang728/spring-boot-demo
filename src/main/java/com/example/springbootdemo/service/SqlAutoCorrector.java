package com.example.springbootdemo.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class SqlAutoCorrector {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile(
        "^\\s*```sql\\s*|\\s*```\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CODE_BLOCK_ONLY_PATTERN = Pattern.compile(
        "^\\s*```\\s*|\\s*```\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BACKTICK_PATTERN = Pattern.compile("`");

    private static final Pattern EXTRACT_PATTERN = Pattern.compile(
        "EXTRACT\\s*\\(\\s*DAY\\s+FROM\\s+(\\w+)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXTRACT_FULL_PATTERN = Pattern.compile(
        "EXTRACT\\s*\\(\\s*\\w+\\s+FROM\\s+(\\w+)\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CAST_DATE_PATTERN = Pattern.compile(
        "CAST\\s*\\(\\s*(\\w+)\\s+AS\\s+DATE\\s*\\)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern DOUBLE_COLON_PATTERN = Pattern.compile(
        "::\\w+"
    );

    private static final Pattern UNQUOTED_IDENTIFIER_PATTERN = Pattern.compile(
        "\"(\\w+)\"(?![^']*')"
    );

    public String preProcess(String sql) {
        if (sql == null) {
            return null;
        }

        String processed = sql;

        processed = BACKTICK_PATTERN.matcher(processed).replaceAll("\"");

        String[] lines = processed.split("\n");
        StringBuilder sqlOnly = new StringBuilder();
        boolean inCodeBlock = false;

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }
            if (line.startsWith("SELECT") || line.startsWith("WITH")) {
                inCodeBlock = true;
            }
            if (inCodeBlock && !line.isEmpty()) {
                if (sqlOnly.length() > 0) {
                    sqlOnly.append(" ");
                }
                sqlOnly.append(line);
            }
        }

        processed = sqlOnly.toString();

        processed = processed.replaceAll(";\\s*$", "").trim();

        processed = Pattern.compile("LIMIT\\s+1000\\s*$", Pattern.CASE_INSENSITIVE).matcher(processed).replaceAll("LIMIT 100");

        return processed;
    }

    public CorrectionResult correctSql(String sql, String errorMessage) {
        String preprocessed = preProcess(sql);

        CorrectionResult result = new CorrectionResult();
        result.setOriginalSql(sql);
        result.setCorrectedSql(preprocessed);
        result.setCorrected(false);

        if (preprocessed == null || errorMessage == null) {
            return result;
        }

        if (errorMessage.contains("pg_catalog.extract") ||
            errorMessage.contains("function") && errorMessage.contains("does not exist")) {

            if (sql.contains("EXTRACT(DAY FROM")) {
                String corrected = EXTRACT_PATTERN.matcher(sql).replaceAll(
                    "EXTRACT(DAY FROM CAST($1 AS DATE))"
                );

                if (!corrected.equals(sql)) {
                    result.setCorrectedSql(corrected);
                    result.setCorrected(true);
                    result.setCorrectionType(CorrectionType.EXTRACT_DATE_CAST);
                    result.setExplanation("已自动将 EXTRACT 函数的日期参数转换为 DATE 类型");
                }
            }
        }

        if (errorMessage.contains("cannot be cast") ||
            errorMessage.contains("undefined cast") ||
            errorMessage.contains("invalid input syntax for type")) {

            String corrected = DOUBLE_COLON_PATTERN.matcher(sql).replaceAll("");

            if (!corrected.equals(sql)) {
                result.setCorrectedSql(corrected);
                result.setCorrected(true);
                result.setCorrectionType(CorrectionType.TYPE_CAST_REMOVAL);
                result.setExplanation("已移除可能导致类型转换错误的类型转换语句");
            }
        }

        if (errorMessage.contains("column") && errorMessage.contains("does not exist")) {
            String corrected = sql.replaceAll("\"(\\w+)\"", "$1");

            if (!corrected.equals(sql)) {
                result.setCorrectedSql(corrected);
                result.setCorrected(true);
                result.setCorrectionType(CorrectionType.QUOTE_REMOVAL);
                result.setExplanation("已移除不必要的引号");
            }
        }

        if (!result.isCorrected() && errorMessage.contains("syntax error")) {
            String trimmed = sql.trim();
            if (trimmed.endsWith(";")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
                result.setCorrectedSql(trimmed);
                result.setCorrected(true);
                result.setCorrectionType(CorrectionType.SEMICOLON_REMOVAL);
                result.setExplanation("已移除末尾的分号");
            }
        }

        if (!result.isCorrected() && errorMessage.contains("LIMIT")) {
            String corrected = sql.replaceAll(
                "LIMIT\\s+\\d+\\s*$",
                "LIMIT 100"
            );

            if (!corrected.equals(sql)) {
                result.setCorrectedSql(corrected);
                result.setCorrected(true);
                result.setCorrectionType(CorrectionType.LIMIT_FIX);
                result.setExplanation("已修正 LIMIT 子句");
            }
        }

        return result;
    }

    public boolean needsAutoCorrect(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }

        return errorMessage.contains("pg_catalog.extract") ||
               errorMessage.contains("cannot be cast") ||
               errorMessage.contains("undefined cast") ||
               errorMessage.contains("does not exist") ||
               errorMessage.contains("syntax error");
    }

    public static class CorrectionResult {
        private String originalSql;
        private String correctedSql;
        private boolean corrected;
        private CorrectionType correctionType;
        private String explanation;

        public String getOriginalSql() {
            return originalSql;
        }

        public void setOriginalSql(String originalSql) {
            this.originalSql = originalSql;
        }

        public String getCorrectedSql() {
            return correctedSql;
        }

        public void setCorrectedSql(String correctedSql) {
            this.correctedSql = correctedSql;
        }

        public boolean isCorrected() {
            return corrected;
        }

        public void setCorrected(boolean corrected) {
            this.corrected = corrected;
        }

        public CorrectionType getCorrectionType() {
            return correctionType;
        }

        public void setCorrectionType(CorrectionType correctionType) {
            this.correctionType = correctionType;
        }

        public String getExplanation() {
            return explanation;
        }

        public void setExplanation(String explanation) {
            this.explanation = explanation;
        }
    }

    public enum CorrectionType {
        EXTRACT_DATE_CAST("EXTRACT 日期类型修正"),
        TYPE_CAST_REMOVAL("移除错误类型转换"),
        QUOTE_REMOVAL("移除多余引号"),
        SEMICOLON_REMOVAL("移除末尾分号"),
        LIMIT_FIX("修正 LIMIT 子句"),
        UNKNOWN("未知错误");

        private final String description;

        CorrectionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
