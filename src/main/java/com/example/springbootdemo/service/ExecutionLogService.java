package com.example.springbootdemo.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ExecutionLogService {

    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, ExecutionLogEntry> inMemoryLogs = new ConcurrentHashMap<>();

    public ExecutionLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initDatabaseTable();
    }

    private void initDatabaseTable() {
        try {
            String createTableSql = "CREATE TABLE IF NOT EXISTS sql_execution_log (" +
                "id SERIAL PRIMARY KEY," +
                "sql_text TEXT NOT NULL," +
                "execution_time_ms INTEGER," +
                "row_count INTEGER," +
                "status VARCHAR(20)," +
                "error_message TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

            jdbcTemplate.execute(createTableSql);

            String createIndexSql = "CREATE INDEX IF NOT EXISTS idx_log_created_at ON sql_execution_log(created_at DESC)";
            jdbcTemplate.execute(createIndexSql);

        } catch (Exception e) {
            System.err.println("Failed to initialize execution log table: " + e.getMessage());
        }
    }

    public String logExecution(String sql, long executionTimeMs, int rowCount, String status, String errorMessage) {
        String logId = generateLogId(sql);

        ExecutionLogEntry entry = new ExecutionLogEntry();
        entry.setId(logId);
        entry.setSqlText(sql);
        entry.setExecutionTimeMs(executionTimeMs);
        entry.setRowCount(rowCount);
        entry.setStatus(status);
        entry.setErrorMessage(errorMessage);
        entry.setCreatedAt(LocalDateTime.now());

        inMemoryLogs.put(logId, entry);

        try {
            String insertSql = "INSERT INTO sql_execution_log (sql_text, execution_time_ms, row_count, status, error_message, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

            jdbcTemplate.update(insertSql,
                sql,
                executionTimeMs,
                rowCount,
                status,
                errorMessage,
                new Timestamp(System.currentTimeMillis())
            );

        } catch (Exception e) {
            System.err.println("Failed to persist execution log: " + e.getMessage());
        }

        return logId;
    }

    public List<Map<String, Object>> getRecentLogs(int limit) {
        try {
            String sql = "SELECT * FROM sql_execution_log ORDER BY created_at DESC LIMIT ?";
            return jdbcTemplate.queryForList(sql, limit);
        } catch (Exception e) {
            return inMemoryLogs.values().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .map(this::toMap)
                .collect(Collectors.toList());
        }
    }

    public List<Map<String, Object>> getLogsByStatus(String status, int limit) {
        try {
            String sql = "SELECT * FROM sql_execution_log WHERE status = ? ORDER BY created_at DESC LIMIT ?";
            return jdbcTemplate.queryForList(sql, status, limit);
        } catch (Exception e) {
            return inMemoryLogs.values().stream()
                .filter(log -> log.getStatus().equals(status))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(limit)
                .map(this::toMap)
                .collect(Collectors.toList());
        }
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            String countSql = "SELECT COUNT(*) as total FROM sql_execution_log";
            Long total = jdbcTemplate.queryForObject(countSql, Long.class);
            stats.put("totalExecutions", total);

            String errorSql = "SELECT COUNT(*) as errors FROM sql_execution_log WHERE status = 'ERROR'";
            Long errors = jdbcTemplate.queryForObject(errorSql, Long.class);
            stats.put("errorCount", errors);
            stats.put("errorRate", total > 0 ? (double) errors / total : 0);

            String avgTimeSql = "SELECT AVG(execution_time_ms) as avg_time FROM sql_execution_log WHERE execution_time_ms IS NOT NULL";
            Double avgTime = jdbcTemplate.queryForObject(avgTimeSql, Double.class);
            stats.put("avgExecutionTimeMs", avgTime != null ? avgTime : 0);

            String maxTimeSql = "SELECT MAX(execution_time_ms) as max_time FROM sql_execution_log";
            Integer maxTime = jdbcTemplate.queryForObject(maxTimeSql, Integer.class);
            stats.put("maxExecutionTimeMs", maxTime != null ? maxTime : 0);

        } catch (Exception e) {
            stats.put("totalExecutions", inMemoryLogs.size());
            stats.put("errorCount", inMemoryLogs.values().stream()
                .filter(log -> "ERROR".equals(log.getStatus()))
                .count());
            stats.put("errorRate", 0);
            stats.put("avgExecutionTimeMs", 0);
            stats.put("maxExecutionTimeMs", 0);
        }

        return stats;
    }

    public void clearLogs() {
        try {
            jdbcTemplate.execute("TRUNCATE TABLE sql_execution_log");
        } catch (Exception e) {
            System.err.println("Failed to clear logs: " + e.getMessage());
        }
        inMemoryLogs.clear();
    }

    private String generateLogId(String sql) {
        return String.valueOf(System.currentTimeMillis()) + "_" + Math.abs(sql.hashCode());
    }

    private Map<String, Object> toMap(ExecutionLogEntry entry) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entry.getId());
        map.put("sqlText", entry.getSqlText());
        map.put("executionTimeMs", entry.getExecutionTimeMs());
        map.put("rowCount", entry.getRowCount());
        map.put("status", entry.getStatus());
        map.put("errorMessage", entry.getErrorMessage());
        map.put("createdAt", entry.getCreatedAt().format(FORMATTER));
        return map;
    }

    public static class ExecutionLogEntry {
        private String id;
        private String sqlText;
        private Long executionTimeMs;
        private Integer rowCount;
        private String status;
        private String errorMessage;
        private LocalDateTime createdAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSqlText() {
            return sqlText;
        }

        public void setSqlText(String sqlText) {
            this.sqlText = sqlText;
        }

        public Long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public void setExecutionTimeMs(Long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
        }

        public Integer getRowCount() {
            return rowCount;
        }

        public void setRowCount(Integer rowCount) {
            this.rowCount = rowCount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
