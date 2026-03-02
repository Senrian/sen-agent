package com.senagent.util;

import lombok.extern.slf4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 通用工具类
 */
@Slf4j
public class CommonUtils {

    /**
     * 生成UUID
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 短UUID
     */
    public static String shortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * MD5哈希
     */
    public static String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(text.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return text;
        }
    }

    /**
     * SHA256哈希
     */
    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return text;
        }
    }

    /**
     * 字符串截断
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    /**
     * 判断是否为空
     */
    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * 判断是否不为空
     */
    public static boolean isNotEmpty(String text) {
        return !isEmpty(text);
    }

    /**
     * 安全获取
     */
    public static String safeGet(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 格式化时间
     */
    public static String formatTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    }

    /**
     * 格式化时长
     */
    public static String formatDuration(long millis) {
        if (millis < 1000) return millis + "ms";
        if (millis < 60000) return String.format("%.1fs", millis / 1000.0);
        if (millis < 3600000) return String.format("%.1fm", millis / 60000.0);
        return String.format("%.1fh", millis / 3600000.0);
    }

    /**
     * 重试
     */
    public static <T> T retry(int times, int delayMs, Retryable<T> action) {
        Exception lastException = null;
        for (int i = 0; i < times; i++) {
            try {
                return action.execute();
            } catch (Exception e) {
                lastException = e;
                if (i < times - 1) {
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

    public interface Retryable<T> {
        T execute() throws Exception;
    }

    /**
     * 防抖
     */
    public static <T> T debounce(long delayMs, Debounceable<T> action) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return action.execute();
    }

    public interface Debounceable<T> {
        T execute();
    }

    /**
     * 节流
     */
    private static final Map<String, Long> throttleMap = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean throttle(String key, long intervalMs) {
        long now = System.currentTimeMillis();
        Long last = throttleMap.get(key);
        if (last == null || now - last > intervalMs) {
            throttleMap.put(key, now);
            return true;
        }
        return false;
    }
}
