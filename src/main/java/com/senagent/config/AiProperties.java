package com.senagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * AI配置 - 配置驱动
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** AI Provider: deepseek/openai/anthropic/minimax */
    private String provider = "deepseek";

    /** API Key - 从环境变量注入 */
    private String apiKey = System.getenv().getOrDefault("DEEPSEEK_API_KEY", "");

    /** Base URL */
    private String baseUrl = "https://api.deepseek.com/v1";

    /** 模型名称 */
    private String model = "deepseek-chat";

    /** 最大Token */
    private Integer maxTokens = 4096;

    /** 温度 */
    private Double temperature = 0.7;

    /** 超时(毫秒) */
    private Long timeout = 120000L;

    /** 系统提示词 */
    private String systemPrompt = "You are a helpful AI assistant.";

    /** 启用流式 */
    private Boolean streamEnabled = true;

    /** 重试配置 */
    private RetryProperties retry = new RetryProperties();

    /** 自定义配置 */
    private Map<String, String> custom = new HashMap<>();

    @Data
    public static class RetryProperties {
        private int maxAttempts = 3;
        private long delayMs = 1000;
        private double backoffMultiplier = 2.0;
        private boolean enabled = true;
    }
}
