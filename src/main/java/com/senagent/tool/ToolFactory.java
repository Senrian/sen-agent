package com.senagent.tool;

import lombok.extern.slf4j;

import java.util.*;

/**
 * 工具工厂 - 统一创建工具
 */
@Slf4j
public class ToolFactory {

    private static final Map<String, ToolCreator> creators = new HashMap<>();

    static {
        // 注册内置工具
        register("python", params -> {
            String code = (String) params.getOrDefault("code", "");
            return PythonSandbox.execute(code);
        });

        register("web_search", params -> {
            String query = (String) params.getOrDefault("query", "");
            try {
                return new WebSearchTool().search(query);
            } catch (Exception e) {
                return "搜索失败: " + e.getMessage();
            }
        });

        register("web_extract", params -> {
            String url = (String) params.getOrDefault("url", "");
            try {
                return new WebExtractorTool().extract(url);
            } catch (Exception e) {
                return "提取失败: " + e.getMessage();
            }
        });

        register("weather", params -> {
            String city = (String) params.getOrDefault("city", "北京");
            try {
                return new WeatherTool().getWeather(city);
            } catch (Exception e) {
                return "天气查询失败: " + e.getMessage();
            }
        });

        register("news", params -> {
            try {
                return new NewsTool().getNews();
            } catch (Exception e) {
                return "新闻获取失败: " + e.getMessage();
            }
        });

        register("file_read", params -> {
            String path = (String) params.get("path");
            return new FileTool().execute(Map.of("operation", "read", "path", path));
        });

        register("file_write", params -> {
            String path = (String) params.get("path");
            String content = (String) params.get("content");
            return new FileTool().execute(Map.of("operation", "write", "path", path, "content", content));
        });
    }

    /**
     * 注册工具
     */
    public static void register(String name, ToolCreator creator) {
        creators.put(name, creator);
    }

    /**
     * 创建工具
     */
    public static Tool create(String name) {
        ToolCreator creator = creators.get(name);
        if (creator == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        return params -> creator.create(params);
    }

    /**
     * 列出所有工具
     */
    public static List<String> listTools() {
        return new ArrayList<>(creators.keySet());
    }

    /**
     * 执行工具
     */
    public static Object execute(String name, Map<String, Object> params) {
        ToolCreator creator = creators.get(name);
        if (creator == null) {
            return Map.of("error", "Unknown tool: " + name);
        }
        return creator.create(params);
    }

    @FunctionalInterface
    public interface ToolCreator {
        Object create(Map<String, Object> params);
    }

    public interface Tool {
        Object execute(Map<String, Object> params);
    }
}
