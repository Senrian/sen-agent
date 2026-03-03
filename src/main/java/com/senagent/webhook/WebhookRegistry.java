package com.senagent.webhook;

import lombok.Data;
import lombok.extern.slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Webhook系统 - 对标OpenClaw的消息推送
 */
@Slf4j
public class WebhookRegistry {

    private final Map<String, Webhook> webhooks = new ConcurrentHashMap<>();

    /**
     * 注册Webhook
     */
    public void register(Webhook webhook) {
        webhooks.put(webhook.getId(), webhook);
        log.info("Registered webhook: {} -> {}", webhook.getId(), webhook.getUrl());
    }

    /**
     * 触发Webhook
     */
    public void trigger(String event, Map<String, Object> data) {
        for (Webhook webhook : webhooks.values()) {
            if (webhook.getEvents().contains(event) || webhook.getEvents().contains("*")) {
                webhook.trigger(event, data);
            }
        }
    }

    /**
     * 移除Webhook
     */
    public void unregister(String id) {
        webhooks.remove(id);
    }

    /**
     * 获取所有Webhook
     */
    public List<Webhook> getAll() {
        return new ArrayList<>(webhooks.values());
    }

    @Data
    public static class Webhook {
        private String id;
        private String url;
        private String secret;
        private List<String> events;
        private boolean enabled = true;

        public Webhook(String id, String url) {
            this.id = id;
            this.url = url;
            this.events = List.of("*");
        }

        public Webhook withEvents(String... events) {
            this.events = Arrays.asList(events);
            return this;
        }

        public Webhook withSecret(String secret) {
            this.secret = secret;
            return this;
        }

        public void trigger(String event, Map<String, Object> data) {
            if (!enabled) return;
            
            log.info("Triggering webhook {} for event {}", id, event);
            
            // 实际发送请求
            new Thread(() -> {
                try {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("event", event);
                    payload.put("timestamp", System.currentTimeMillis());
                    payload.put("data", data);
                    
                    // 简化的HTTP调用
                    log.info("Webhook payload: {}", payload);
                } catch (Exception e) {
                    log.error("Webhook trigger failed", e);
                }
            }).start();
        }
    }

    /**
     * Webhook事件
     */
    public static class Events {
        public static final String AGENT_START = "agent.start";
        public static final String AGENT_END = "agent.end";
        public static final String TOOL_CALL = "tool.call";
        public static final String ERROR = "error";
        public static final String MESSAGE = "message";
    }
}
