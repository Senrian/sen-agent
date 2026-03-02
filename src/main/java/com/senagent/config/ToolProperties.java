package com.senagent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 工具配置 - 可插拔
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "tool")
public class ToolProperties {

    /** 启用Python沙箱 */
    private Boolean pythonEnabled = true;

    /** Python超时(秒) */
    private Integer pythonTimeout = 30;

    /** Python最大内存(MB) */
    private Integer pythonMaxMemory = 512;

    /** 启用Web搜索 */
    private Boolean webSearchEnabled = true;

    /** Web搜索超时 */
    private Long webSearchTimeout = 10000L;

    /** 启用天气 */
    private Boolean weatherEnabled = true;

    /** 启用新闻 */
    private Boolean newsEnabled = true;

    /** 启用文件操作 */
    private Boolean fileEnabled = true;

    /** 文件工作目录 */
    private String fileBaseDir = "/tmp/sen-agent-files";

    /** 允许的文件扩展名 */
    private String[] allowedExtensions = {".txt", ".json", ".md", ".yaml", ".xml", ".csv"};

    /** 最大文件大小(MB) */
    private Integer maxFileSize = 10;
}
