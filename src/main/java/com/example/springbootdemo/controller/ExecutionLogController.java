package com.example.springbootdemo.controller;

import com.example.springbootdemo.service.ExecutionLogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/logs")
@CrossOrigin
public class ExecutionLogController {

    @Resource
    private ExecutionLogService executionLogService;

    @GetMapping("/recent")
    public List<Map<String, Object>> getRecentLogs(
        @RequestParam(defaultValue = "50") int limit) {
        return executionLogService.getRecentLogs(limit);
    }

    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        return executionLogService.getStatistics();
    }

    @GetMapping("/errors")
    public List<Map<String, Object>> getErrorLogs(
        @RequestParam(defaultValue = "20") int limit) {
        return executionLogService.getLogsByStatus("ERROR", limit);
    }

    @DeleteMapping("/clear")
    public Map<String, Object> clearLogs() {
        executionLogService.clearLogs();
        return Map.of("success", true, "message", "日志已清空");
    }
}
