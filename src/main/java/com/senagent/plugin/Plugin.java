package com.senagent.plugin;

/**
 * 插件接口 - 组件可插拔
 */
public interface Plugin {

    /**
     * 插件ID
     */
    String getId();

    /**
     * 插件名称
     */
    String getName();

    /**
     * 版本
     */
    String getVersion();

    /**
     * 描述
     */
    String getDescription();

    /**
     * 初始化
     */
    default void init() {}

    /**
     * 启动
     */
    default void start() {}

    /**
     * 停止
     */
    default void stop() {}

    /**
     * 优先级(越小越先)
     */
    default int getPriority() {
        return 100;
    }
}
