package com.senagent.retry;

import lombok.extern.slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * 重试机制
 */
@Slf4j
public class Retry {

    /**
     * 执行带重试
     */
    public static <T> T execute(Supplier<T> action, int maxAttempts, long delayMs) {
        Exception lastException = null;
        
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed: {}", i + 1, maxAttempts, e.getMessage());
                
                if (i < maxAttempts - 1) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new RuntimeException("重试失败", lastException);
    }

    /**
     * 执行带重试(指数退避)
     */
    public static <T> T executeWithBackoff(Supplier<T> action, int maxAttempts, long initialDelayMs) {
        Exception lastException = null;
        long delay = initialDelayMs;
        
        for (int i = 0; i < maxAttempts; i++) {
            try {
                return action.get();
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed: {}", i + 1, maxAttempts, e.getMessage());
                
                if (i < maxAttempts - 1) {
                    try {
                        Thread.sleep(delay);
                        delay *= 2; // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        throw new RuntimeException("重试失败", lastException);
    }

    /**
     * 异步重试
     */
    public static <T> CompletableFuture<T> executeAsync(Supplier<T> action, int maxAttempts, long delayMs) {
        return CompletableFuture.supplyAsync(() -> execute(action, maxAttempts, delayMs));
    }

    /**
     * 带条件重试
     */
    public static <T> T executeIf(Supplier<T> action, int maxAttempts, long delayMs, java.util.function.Predicate<T> condition) {
        for (int i = 0; i < maxAttempts; i++) {
            try {
                T result = action.get();
                if (condition.test(result)) {
                    return result;
                }
            } catch (Exception e) {
                log.warn("Attempt {}/{} failed: {}", i + 1, maxAttempts, e.getMessage());
            }
            
            if (i < maxAttempts - 1) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        throw new RuntimeException("条件重试失败");
    }
}
