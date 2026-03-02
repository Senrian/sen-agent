package com.senagent.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j;

import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 */
@Slf4j
public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 对象转JSON
     */
    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
    }

    /**
     * 对象转JSON(格式化)
     */
    public static String toPrettyJson(Object obj) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            return "{}";
        }
    }

    /**
     * JSON转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON反序列化失败", e);
            return null;
        }
    }

    /**
     * JSON转Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMap(String json) {
        try {
            return mapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("JSON转Map失败", e);
            return Map.of();
        }
    }

    /**
     * JSON转List
     */
    @SuppressWarnings("unchecked")
    public static List<Object> toList(String json) {
        try {
            return mapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            log.error("JSON转List失败", e);
            return List.of();
        }
    }

    /**
     * 合并Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> merge(Map<String, Object> base, Map<String, Object> update) {
        Map<String, Object> result = new java.util.HashMap<>(base);
        result.putAll(update);
        return result;
    }

    /**
     * 深层合并
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> update) {
        Map<String, Object> result = new java.util.HashMap<>(base);
        
        for (Map.Entry<String, Object> entry : update.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof Map && result.get(key) instanceof Map) {
                result.put(key, deepMerge((Map<String, Object>) result.get(key), (Map<String, Object>) value));
            } else {
                result.put(key, value);
            }
        }
        
        return result;
    }

    /**
     * 从Map获取值
     */
    public static Object get(Map<String, Object> map, String path) {
        String[] keys = path.split("\\.");
        Object current = map;
        
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                return null;
            }
        }
        
        return current;
    }

    /**
     * 安全获取字符串
     */
    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    /**
     * 安全获取数字
     */
    public static Integer getInt(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * 安全获取布尔
     */
    public static Boolean getBoolean(Map<String, Object> map, String key, Boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
    }
}
