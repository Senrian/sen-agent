package com.senagent.agent;

import java.util.*;

/**
 * Agent上下文 - 在Agent之间传递状态
 */
public class AgentContext {

    private final String id;
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Object> metadata = new HashMap<>();
    private final List<String> history = new ArrayList<>();
    private long createdAt;
    private long updatedAt;

    public AgentContext() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = createdAt;
    }

    public AgentContext(String id) {
        this.id = id;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = createdAt;
    }

    /**
     * 设置值
     */
    public AgentContext set(String key, Object value) {
        values.put(key, value);
        updatedAt = System.currentTimeMillis();
        return this;
    }

    /**
     * 获取值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) values.get(key);
    }

    /**
     * 获取值(带默认值)
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        return (T) values.getOrDefault(key, defaultValue);
    }

    /**
     * 获取字符串
     */
    public String getString(String key) {
        return get(key, "");
    }

    /**
     * 获取数字
     */
    public Integer getInt(String key, int defaultValue) {
        Object value = values.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * 设置元数据
     */
    public AgentContext setMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 添加历史记录
     */
    public void addHistory(String entry) {
        history.add(entry);
        updatedAt = System.currentTimeMillis();
    }

    public List<String> getHistory() {
        return history;
    }

    /**
     * 清除
     */
    public void clear() {
        values.clear();
        metadata.clear();
        history.clear();
    }

    /**
     * 复制
     */
    public AgentContext copy() {
        AgentContext ctx = new AgentContext();
        ctx.values.putAll(this.values);
        ctx.metadata.putAll(this.metadata);
        ctx.history.addAll(this.history);
        return ctx;
    }

    // Getters
    public String getId() { return id; }
    public Map<String, Object> getValues() { return values; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
