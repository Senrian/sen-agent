package com.senagent.rate;

import lombok.extern.slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流器 - 令牌桶/滑动窗口
 */
@Slf4j
public class RateLimiter {

    private final int maxRequests;
    private final long windowMs;
    private final Map<String, SlidingWindow> windows = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    /**
     * 尝试获取令牌
     */
    public boolean tryAcquire(String key) {
        SlidingWindow window = windows.computeIfAbsent(key, k -> new SlidingWindow());
        return window.tryAcquire();
    }

    /**
     * 获取剩余令牌
     */
    public int available(String key) {
        SlidingWindow window = windows.get(key);
        return window != null ? window.available() : maxRequests;
    }

    /**
     * 重置
     */
    public void reset(String key) {
        windows.remove(key);
    }

    private class SlidingWindow {
        private final AtomicInteger count = new AtomicInteger(0);
        private long windowStart = System.currentTimeMillis();

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMs) {
                windowStart = now;
                count.set(0);
            }
            
            if (count.get() < maxRequests) {
                count.incrementAndGet();
                return true;
            }
            return false;
        }

        synchronized int available() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMs) {
                return maxRequests;
            }
            return maxRequests - count.get();
        }
    }

    /**
     * 全局限流
     */
    private static final RateLimiter GLOBAL = new RateLimiter(100, 60000);

    public static boolean globalTryAcquire() {
        return GLOBAL.tryAcquire("global");
    }

    public static int globalAvailable() {
        return GLOBAL.available("global");
    }

    /**
     * IP限流
     */
    private static final Map<String, RateLimiter> IP_LIMITERS = new ConcurrentHashMap<>();

    public static boolean tryAcquire(String ip, int maxRequests, long windowMs) {
        RateLimiter limiter = IP_LIMITERS.computeIfAbsent(ip, k -> new RateLimiter(maxRequests, windowMs));
        return limiter.tryAcquire(ip);
    }
}
