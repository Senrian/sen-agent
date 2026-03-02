package com.senagent.validation;

import lombok.extern.slf4j;

import java.util.*;

/**
 * 输出验证器 - 对标LangChain的OutputParser
 * 
 * 支持:
 * - JSON解析
 * - 列表解析
 * - 结构化输出
 */
@Slf4j
public class OutputParser {

    /**
     * 解析JSON
     */
    public static Map<String, Object> parseJson(String text) {
        try {
            // 尝试直接解析
            text = text.trim();
            if (text.startsWith("```json")) {
                text = text.substring(7);
            }
            if (text.startsWith("```")) {
                text = text.substring(3);
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length() - 3);
            }
            text = text.trim();
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(text, Map.class);
        } catch (Exception e) {
            log.warn("JSON解析失败: {}", e.getMessage());
            return Map.of("error", "解析失败", "raw", text);
        }
    }

    /**
     * 解析列表
     */
    public static List<String> parseList(String text) {
        List<String> items = new ArrayList<>();
        
        // 尝试JSON数组
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List list = mapper.readValue(text, List.class);
            return (List<String>) list;
        } catch (Exception ignored) {}
        
        // 尝试逐行解析
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.matches("^\\d+[.、].+") || line.matches("^[*-].+")) {
                line = line.replaceFirst("^\\d+[.、]\\s*", "").replaceFirst("^[*-]\\s*", "");
                items.add(line);
            }
        }
        
        if (items.isEmpty()) {
            items.add(text);
        }
        
        return items;
    }

    /**
     * 提取代码块
     */
    public static String extractCode(String text) {
        String[] lines = text.split("\n");
        boolean inCode = false;
        StringBuilder code = new StringBuilder();
        String language = "";
        
        for (String line : lines) {
            if (line.trim().startsWith("```")) {
                if (inCode) {
                    inCode = false;
                } else {
                    inCode = true;
                    language = line.trim().substring(3).trim();
                }
                continue;
            }
            if (inCode) {
                code.append(line).append("\n");
            }
        }
        
        return code.toString().trim();
    }

    /**
     * 提取关键信息
     */
    public static Map<String, String> extractKeyValue(String text) {
        Map<String, String> result = new HashMap<>();
        
        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains(":")) {
                int idx = line.indexOf(":");
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                result.put(key, value);
            }
        }
        
        return result;
    }

    /**
     * 验证是否为有效JSON
     */
    public static boolean isValidJson(String text) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.readValue(text, Object.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 结构化输出解析器
     */
    public interface StructuredParser<T> {
        T parse(String text);
    }

    /**
     * JSON解析器
     */
    public static class JsonParser<T> implements StructuredParser<T> {
        private final Class<T> clazz;

        public JsonParser(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public T parse(String text) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(text.trim(), clazz);
            } catch (Exception e) {
                throw new RuntimeException("解析失败: " + e.getMessage());
            }
        }
    }

    /**
     * 列表解析器
     */
    public static class ListParser implements StructuredParser<List<String>> {
        @Override
        public List<String> parse(String text) {
            return parseList(text);
        }
    }
}
