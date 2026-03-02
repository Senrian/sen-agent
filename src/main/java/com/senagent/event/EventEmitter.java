package com.senagent.event;

import lombok.extern.slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 事件系统 - EventEmitter模式
 */
@Slf4j
public class EventEmitter {

    private final Map<String, List<Consumer<Event>>> listeners = new ConcurrentHashMap<>();

    /**
     * 监听事件
     */
    public EventEmitter on(String event, Consumer<Event> handler) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(handler);
        return this;
    }

    /**
     * 一次性监听
     */
    public EventEmitter once(String event, Consumer<Event> handler) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(e -> {
            handler.accept(e);
            off(event, handler);
        });
        return this;
    }

    /**
     * 移除监听
     */
    public EventEmitter off(String event, Consumer<Event> handler) {
        List<Consumer<Event>> handlers = listeners.get(event);
        if (handlers != null) {
            handlers.remove(handler);
        }
        return this;
    }

    /**
     * 触发事件
     */
    public void emit(String event, Object data) {
        List<Consumer<Event>> handlers = listeners.get(event);
        if (handlers != null) {
            Event e = new Event(event, data);
            for (Consumer<Event> handler : handlers) {
                try {
                    handler.accept(e);
                } catch (Exception ex) {
                    log.error("Event handler error for {}: {}", event, ex.getMessage());
                }
            }
        }
    }

    /**
     * 触发异步事件
     */
    public void emitAsync(String event, Object data) {
        new Thread(() -> emit(event, data)).start();
    }

    /**
     * 清除所有监听
     */
    public void clear() {
        listeners.clear();
    }

    /**
     * 清除特定事件监听
     */
    public void clear(String event) {
        listeners.remove(event);
    }

    /**
     * 获取监听器数量
     */
    public int listenerCount(String event) {
        List<Consumer<Event>> handlers = listeners.get(event);
        return handlers != null ? handlers.size() : 0;
    }

    /**
     * 事件对象
     */
    public static class Event {
        private final String type;
        private final Object data;
        private final long timestamp;

        public Event(String type, Object data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public String getType() { return type; }
        public Object getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * 预定义事件
     */
    public static class Types {
        public static final String MESSAGE = "message";
        public static final String TOOL_CALL = "tool_call";
        public static final String AGENT_START = "agent_start";
        public static final String AGENT_END = "agent_end";
        public static final String ERROR = "error";
    }
}
