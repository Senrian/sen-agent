package com.senagent.callback;

import lombok.Data;
import lombok.extern.slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 回调系统 - 对标LangChain的Callbacks
 * 
 * 支持:
 * - 链式回调
 * - 事件处理
 * - 异步执行
 */
@Slf4j
public class CallbackManager {

    private final List<CallbackHandler> handlers = new ArrayList<>();
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();

    /**
     * 添加回调
     */
    public CallbackManager addHandler(CallbackHandler handler) {
        handlers.add(handler);
        return this;
    }

    /**
     * 触发回调
     */
    public void trigger(CallbackEvent event) {
        for (CallbackHandler handler : handlers) {
            try {
                handler.handle(event);
            } catch (Exception e) {
                log.error("Callback handler error: {}", handler.getClass().getSimpleName(), e);
            }
        }
    }

    /**
     * 设置元数据
     */
    public CallbackManager setMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * 回调事件
     */
    @Data
    public static class CallbackEvent {
        private final EventType type;
        private final String runId;
        private final Map<String, Object> data = new HashMap<>();
        private final long timestamp;

        public CallbackEvent(EventType type, String runId) {
            this.type = type;
            this.runId = runId;
            this.timestamp = System.currentTimeMillis();
        }

        public void addData(String key, Object value) {
            data.put(key, value);
        }
    }

    /**
     * 事件类型
     */
    public enum EventType {
        START,           // 开始
        CHUNK,           // 流式输出块
        TOOL_START,      // 工具开始
        TOOL_END,        // 工具结束
        ERROR,           // 错误
        COMPLETE,        // 完成
        RETRY,           // 重试
    }

    /**
     * 回调处理器接口
     */
    public interface CallbackHandler {
        void handle(CallbackEvent event);
    }

    /**
     * 打印回调 - 输出到日志
     */
    public static class LoggingHandler implements CallbackHandler {
        @Override
        public void handle(CallbackEvent event) {
            switch (event.getType()) {
                case START -> log.info("🔔 [{}] 开始执行", event.getRunId());
                case CHUNK -> log.debug("📝 [{}] 流式输出: {}", event.getRunId(), event.getData().get("chunk"));
                case TOOL_START -> log.info("🔧 [{}] 工具开始: {}", event.getRunId(), event.getData().get("tool"));
                case TOOL_END -> log.info("✅ [{}] 工具结束: {}", event.getRunId(), event.getData().get("tool"));
                case ERROR -> log.error("❌ [{}] 错误: {}", event.getRunId(), event.getData().get("error"));
                case COMPLETE -> log.info("🎉 [{}] 完成", event.getRunId());
                case RETRY -> log.warn("🔄 [{}] 重试", event.getRunId());
            }
        }
    }

    /**
     * 统计回调 - 收集指标
     */
    public static class StatsHandler implements CallbackHandler {
        private final Map<String, Long> timings = new ConcurrentHashMap<>();
        private final Map<String, Integer> counts = new ConcurrentHashMap<>();

        @Override
        public void handle(CallbackEvent event) {
            switch (event.getType()) {
                case START -> {
                    timings.put(event.getRunId(), System.currentTimeMillis());
                    counts.merge(event.getRunId(), 1, Integer::sum);
                }
                case COMPLETE -> {
                    Long start = timings.get(event.getRunId());
                    if (start != null) {
                        long duration = System.currentTimeMillis() - start;
                        log.info("⏱️ 执行时间: {}ms", duration);
                    }
                }
                case ERROR -> {
                    log.error("❌ 错误次数: {}", counts.getOrDefault(event.getRunId() + "_error", 0));
                }
            }
        }
    }
}
