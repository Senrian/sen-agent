package com.senagent.cache;

import lombok.extern.slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单缓存 - 内存缓存
 */
@Slf4j
public class Cache<K, V> {

    private final Map<K, CacheEntry<V>> store = new ConcurrentHashMap<>();
    private final long ttlMs;
    private final int maxSize;

    public Cache() {
        this(5 * 60 * 1000, 1000); // 5分钟, 1000条
    }

    public Cache(long ttlMs, int maxSize) {
        this.ttlMs = ttlMs;
        this.maxSize = maxSize;
    }

    /**
     * 设置缓存
     */
    public void put(K key, V value) {
        evictIfNeeded();
        store.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMs));
    }

    /**
     * 设置缓存(自定义TTL)
     */
    public void put(K key, V value, long ttlMs) {
        evictIfNeeded();
        store.put(key, new CacheEntry<>(value, System.currentTimeMillis() + ttlMs));
    }

    /**
     * 获取缓存
     */
    public V get(K key) {
        CacheEntry<V> entry = store.get(key);
        if (entry == null) return null;
        
        if (entry.isExpired()) {
            store.remove(key);
            return null;
        }
        
        return entry.value;
    }

    /**
     * 获取或创建
     */
    public V getOrCompute(K key, java.util.function.Supplier<V> supplier) {
        V value = get(key);
        if (value == null) {
            value = supplier.get();
            put(key, value);
        }
        return value;
    }

    /**
     * 删除
     */
    public void remove(K key) {
        store.remove(key);
    }

    /**
     * 清空
     */
    public void clear() {
        store.clear();
    }

    /**
     * 大小
     */
    public int size() {
        return store.size();
    }

    /**
     * 包含
     */
    public boolean contains(K key) {
        return get(key) != null;
    }

    private void evictIfNeeded() {
        if (store.size() >= maxSize) {
            // 清理过期
            long now = System.currentTimeMillis();
            store.entrySet().removeIf(e -> e.getValue().isExpired());
            
            // 还满则清理最老的
            if (store.size() >= maxSize) {
                store.entrySet().iterator().remove();
            }
        }
    }

    private class CacheEntry<V> {
        final V value;
        final long expiresAt;

        CacheEntry(V value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
