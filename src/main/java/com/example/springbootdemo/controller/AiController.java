package com.example.springbootdemo.controller;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.example.springbootdemo.model.dataReq;
import com.example.springbootdemo.service.*;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("ai")
@CrossOrigin
public class AiController {

    @Resource
    private PromptManager promptManager;

    @Resource
    private SqlSecurityValidator sqlSecurityValidator;

    @Resource
    private SqlAutoCorrector sqlAutoCorrector;

    @Resource
    private DataService dataService;

    @Resource
    private ChartRecommendationService chartRecommendationService;

    @Resource
    private ExecutionLogService executionLogService;

    @Resource
    private QueryCacheService queryCacheService;

    private static final int MAX_RETRY_COUNT = 2;

    @PostMapping("query")
    public Map<String, Object> query(@RequestBody dataReq req) {
        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();
        String sql = null;

        try {
            String question = req.getResult();
            String generatedSql = generateSqlFromAI(question);
            sql = generatedSql;

            String sanitizedSql = sqlSecurityValidator.sanitizeSql(sqlAutoCorrector.preProcess(generatedSql));
            sql = sanitizedSql;

            SqlSecurityValidator.ValidationResult validationResult =
                sqlSecurityValidator.validate(sanitizedSql);

            if (validationResult.hasErrors()) {
                response.put("success", false);
                response.put("error", Map.of(
                    "code", "VALIDATION_ERROR",
                    "message", "SQL 验证失败",
                    "details", String.join("; ", validationResult.getErrors())
                ));

                long executionTime = System.currentTimeMillis() - startTime;
                executionLogService.logExecution(sql, executionTime, 0, "VALIDATION_ERROR",
                    String.join("; ", validationResult.getErrors()));

                return response;
            }

            List<Map<String, Object>> result = queryCacheService.queryWithCache(sql, null);

            if (result == null) {
                result = executeSqlWithRetry(sanitizedSql);
                result = queryCacheService.queryWithCache(sql, result);
            }

            ChartRecommendationService.ChartRecommendation chartRecommendation =
                chartRecommendationService.recommend(result);

            long executionTime = System.currentTimeMillis() - startTime;
            executionLogService.logExecution(sql, executionTime, result.size(), "SUCCESS", null);

            response.put("success", true);
            response.put("data", Map.of(
                "sql", sanitizedSql,
                "results", result,
                "rowCount", result.size(),
                "executionTimeMs", executionTime,
                "validationWarnings", validationResult.getWarnings(),
                "validationSuggestions", validationResult.getSuggestions(),
                "fromCache", queryCacheService.isCached(sql)
            ));
            response.put("chartRecommendation", chartRecommendation);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            executionLogService.logExecution(sql != null ? sql : "N/A", executionTime, 0, "ERROR", e.getMessage());

            response.put("success", false);
            response.put("error", Map.of(
                "code", "EXECUTION_ERROR",
                "message", "查询执行失败",
                "details", e.getMessage()
            ));
        }

        return response;
    }

    @PostMapping("validate")
    public Map<String, Object> validateSql(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        String sql = request.get("sql");
        SqlSecurityValidator.ValidationResult result = sqlSecurityValidator.validate(sql);

        response.put("valid", result.isValid());
        response.put("errors", result.getErrors());
        response.put("warnings", result.getWarnings());
        response.put("suggestions", result.getSuggestions());

        return response;
    }

    @PostMapping("execute")
    public Map<String, Object> executeSql(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();
        String sql = null;

        try {
            sql = request.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", Map.of(
                    "code", "INVALID_REQUEST",
                    "message", "SQL 语句不能为空"
                ));
                return response;
            }

            String sanitizedSql = sqlSecurityValidator.sanitizeSql(sqlAutoCorrector.preProcess(sql));

            SqlSecurityValidator.ValidationResult validationResult =
                sqlSecurityValidator.validate(sanitizedSql);

            if (validationResult.hasErrors()) {
                response.put("success", false);
                response.put("error", Map.of(
                    "code", "VALIDATION_ERROR",
                    "message", "SQL 验证失败",
                    "details", String.join("; ", validationResult.getErrors())
                ));

                long executionTime = System.currentTimeMillis() - startTime;
                executionLogService.logExecution(sql, executionTime, 0, "VALIDATION_ERROR",
                    String.join("; ", validationResult.getErrors()));

                return response;
            }

            List<Map<String, Object>> result = queryCacheService.queryWithCache(sanitizedSql, null);

            if (result == null) {
                result = dataService.query(sanitizedSql);
                queryCacheService.queryWithCache(sanitizedSql, result);
            }

            ChartRecommendationService.ChartRecommendation chartRecommendation =
                chartRecommendationService.recommend(result);

            long executionTime = System.currentTimeMillis() - startTime;
            executionLogService.logExecution(sanitizedSql, executionTime, result.size(), "SUCCESS", null);

            response.put("success", true);
            response.put("data", Map.of(
                "sql", sanitizedSql,
                "results", result,
                "rowCount", result.size(),
                "executionTimeMs", executionTime,
                "validationWarnings", validationResult.getWarnings(),
                "validationSuggestions", validationResult.getSuggestions(),
                "fromCache", queryCacheService.isCached(sanitizedSql)
            ));
            response.put("chartRecommendation", chartRecommendation);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            executionLogService.logExecution(sql != null ? sql : "N/A", executionTime, 0, "ERROR", e.getMessage());

            response.put("success", false);
            response.put("error", Map.of(
                "code", "EXECUTION_ERROR",
                "message", "查询执行失败",
                "details", e.getMessage()
            ));
        }

        return response;
    }

    private String generateSqlFromAI(String question) {
        String prompt = promptManager.buildPrompt(question);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("model", "llama3.2:3b");
        paramMap.put("stream", false);
        paramMap.put("messages", List.of(
            Map.of("role", "system", "content", promptManager.getSystemPrompt()),
            Map.of("role", "user", "content", question)
        ));

        String result = HttpUtil.post("http://localhost:11434/api/chat",
            JSONUtil.toJsonStr(paramMap));

        Map<String, Object> responseMap = JSONUtil.toBean(result, Map.class);
        Map<String, Object> message = (Map<String, Object>) responseMap.get("message");

        String generatedSql = message.get("content").toString().trim();
        System.out.println("[AI Generated SQL] " + generatedSql);
        return generatedSql;
    }

    private List<Map<String, Object>> executeSqlWithRetry(String sql) {
        String currentSql = sql;
        String lastError = null;

        for (int i = 0; i <= MAX_RETRY_COUNT; i++) {
            try {
                return dataService.query(currentSql);
            } catch (BadSqlGrammarException e) {
                lastError = e.getMessage();

                if (i < MAX_RETRY_COUNT && sqlAutoCorrector.needsAutoCorrect(lastError)) {
                    SqlAutoCorrector.CorrectionResult correction =
                        sqlAutoCorrector.correctSql(currentSql, lastError);

                    if (correction.isCorrected()) {
                        currentSql = correction.getCorrectedSql();
                        continue;
                    }
                }

                throw e;
            }
        }

        throw new RuntimeException("SQL 执行失败: " + lastError);
    }
}
