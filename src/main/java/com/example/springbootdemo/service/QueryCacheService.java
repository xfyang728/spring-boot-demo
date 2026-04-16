package com.example.springbootdemo.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class QueryCacheService {

    private static final long DEFAULT_TTL_MS = 300_000;
    private static final long POPULAR_QUERY_TTL_MS = 1800_000;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public List<Map<String, Object>> queryWithCache(String sql, List<Map<String, Object>> data) {
        String cacheKey = generateCacheKey(sql);
        CacheEntry entry = cache.get(cacheKey);

        if (entry != null && !entry.isExpired()) {
            return entry.getData();
        }

        if (data != null) {
            cache.put(cacheKey, new CacheEntry(data, DEFAULT_TTL_MS));
        }

        return data;
    }

    public void invalidateCache(String sql) {
        String cacheKey = generateCacheKey(sql);
        cache.remove(cacheKey);
    }

    public void invalidateAll() {
        cache.clear();
    }

    public void refreshCache(String sql, List<Map<String, Object>> data) {
        invalidateCache(sql);
        queryWithCache(sql, data);
    }

    private String generateCacheKey(String sql) {
        String normalized = sql.trim().toLowerCase().replaceAll("\\s+", " ");
        return String.valueOf(Math.abs(normalized.hashCode()));
    }

    public Long getCacheTtl(String sql) {
        String cacheKey = generateCacheKey(sql);
        CacheEntry entry = cache.get(cacheKey);
        if (entry != null) {
            return entry.getRemainingTtl();
        }
        return -1L;
    }

    public boolean isCached(String sql) {
        String cacheKey = generateCacheKey(sql);
        CacheEntry entry = cache.get(cacheKey);
        return entry != null && !entry.isExpired();
    }

    public Map<String, Object> getCacheStats() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());

        return Map.of(
            "totalCachedQueries", cache.size(),
            "defaultTtlSeconds", DEFAULT_TTL_MS / 1000,
            "popularQueryTtlSeconds", POPULAR_QUERY_TTL_MS / 1000
        );
    }

    private static class CacheEntry {
        private final List<Map<String, Object>> data;
        private final long expireTime;

        CacheEntry(List<Map<String, Object>> data, long ttlMs) {
            this.data = data;
            this.expireTime = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        List<Map<String, Object>> getData() {
            return data;
        }

        long getRemainingTtl() {
            long remaining = expireTime - System.currentTimeMillis();
            return remaining > 0 ? remaining / 1000 : 0;
        }
    }
}
