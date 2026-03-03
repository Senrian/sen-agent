package com.senagent.controller;

import com.senagent.cache.Cache;
import com.senagent.rate.RateLimiter;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统管理API
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final Cache<String, Object> systemCache = new Cache<>(300000, 1000);

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        result.put("uptime", Runtime.getRuntime().totalMemory());
        
        Runtime rt = Runtime.getRuntime();
        result.put("memory", Map.of(
            "total", rt.totalMemory(),
            "free", rt.freeMemory(),
            "used", rt.totalMemory() - rt.freeMemory(),
            "max", rt.maxMemory()
        ));
        
        result.put("threads", Map.of(
            "active", Thread.activeCount(),
            "available", Runtime.getRuntime().availableProcessors()
        ));
        
        return result;
    }

    /**
     * 限流状态
     */
    @GetMapping("/rate-limit")
    public Map<String, Object> rateLimitStatus(
            @RequestParam(required = false) String ip) {
        
        Map<String, Object> result = new HashMap<>();
        
        if (ip != null) {
            result.put("ip", ip);
            result.put("available", RateLimiter.tryAcquire(ip, 60, 60000));
        } else {
            result.put("global", RateLimiter.globalAvailable());
        }
        
        return result;
    }

    /**
     * 触发限流
     */
    @PostMapping("/rate-limit")
    public Map<String, Object> acquireRateLimit(
            @RequestParam String ip,
            @RequestParam(defaultValue = "60") int maxRequests,
            @RequestParam(defaultValue = "60000") long windowMs) {
        
        boolean acquired = RateLimiter.tryAcquire(ip, maxRequests, windowMs);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", acquired);
        result.put("ip", ip);
        
        return result;
    }

    /**
     * 缓存操作
     */
    @GetMapping("/cache")
    public Map<String, Object> cacheInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("size", systemCache.size());
        return result;
    }

    @PostMapping("/cache/{key}")
    public Map<String, Object> setCache(@PathVariable String key, @RequestBody Map<String, Object> body) {
        systemCache.put(key, body.get("value"));
        
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("success", true);
        return result;
    }

    @GetMapping("/cache/{key}")
    public ResponseEntity<Object> getCache(@PathVariable String key) {
        Object value = systemCache.get(key);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(value);
    }

    @DeleteMapping("/cache/{key}")
    public Map<String, Object> deleteCache(@PathVariable String key) {
        systemCache.remove(key);
        
        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("success", true);
        return result;
    }

    /**
     * 线程池状态
     */
    @GetMapping("/thread-pool")
    public Map<String, Object> threadPoolStatus() {
        Map<String, Object> result = new HashMap<>();
        
        // 简化的线程信息
        Map<String, Object> threadInfo = new HashMap<>();
        threadInfo.put("activeCount", Thread.activeCount());
        threadInfo.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        
        result.put("main", threadInfo);
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }

    /**
     * 系统信息
     */
    @GetMapping("/info")
    public Map<String, Object> systemInfo() {
        Map<String, Object> result = new HashMap<>();
        result.put("name", "senAgent");
        result.put("version", "0.0.1-SNAPSHOT");
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("osName", System.getProperty("os.name"));
        result.put("timestamp", System.currentTimeMillis());
        
        Runtime rt = Runtime.getRuntime();
        result.put("cpuCores", rt.availableProcessors());
        result.put("totalMemory", rt.totalMemory());
        result.put("maxMemory", rt.maxMemory());
        
        return result;
    }
}
