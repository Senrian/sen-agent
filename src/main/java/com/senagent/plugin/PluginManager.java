package com.senagent.plugin;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件管理器 - 插件可插拔
 */
@Slf4j
public class PluginManager {

    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();
    private final Map<String, Object> services = new ConcurrentHashMap<>();

    /**
     * 注册插件
     */
    public void register(Plugin plugin) {
        if (plugins.containsKey(plugin.getId())) {
            log.warn("Plugin already registered: {}", plugin.getId());
            return;
        }

        try {
            plugin.init();
            plugin.start();
            plugins.put(plugin.getId(), plugin);
            log.info("✅ Plugin registered: {} v{}", plugin.getName(), plugin.getVersion());
        } catch (Exception e) {
            log.error("Failed to register plugin: {}", plugin.getId(), e);
            throw new RuntimeException("Plugin registration failed: " + plugin.getId(), e);
        }
    }

    /**
     * 卸载插件
     */
    public void unregister(String pluginId) {
        Plugin plugin = plugins.remove(pluginId);
        if (plugin != null) {
            try {
                plugin.stop();
                log.info("Plugin unregistered: {}", pluginId);
            } catch (Exception e) {
                log.error("Error stopping plugin: {}", pluginId, e);
            }
        }
    }

    /**
     * 获取插件
     */
    public Plugin get(String pluginId) {
        return plugins.get(pluginId);
    }

    /**
     * 获取所有插件
     */
    public List<Plugin> getAll() {
        return new ArrayList<>(plugins.values());
    }

    /**
     * 获取启用的插件
     */
    public List<Plugin> getEnabled() {
        return plugins.values().stream()
                .sorted(Comparator.comparingInt(Plugin::getPriority))
                .toList();
    }

    /**
     * 注册服务
     */
    public <T> void registerService(Class<T> type, T instance) {
        services.put(type.getName(), instance);
    }

    /**
     * 获取服务
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> type) {
        return (T) services.get(type.getName());
    }

    /**
     * 关闭所有插件
     */
    public void shutdown() {
        plugins.values().forEach(plugin -> {
            try {
                plugin.stop();
            } catch (Exception e) {
                log.error("Error stopping plugin: {}", plugin.getId(), e);
            }
        });
        plugins.clear();
        log.info("All plugins stopped");
    }
}
