package com.senagent.tool;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 文件操作工具 - 对标OpenClaw的文件能力
 */
@Slf4j
public class FileTool implements FnCallAgent.ToolExecutor {

    private final String baseDir;
    
    public FileTool() {
        this(System.getProperty("java.io.tmpdir") + "/mini-agent-files");
    }
    
    public FileTool(String baseDir) {
        this.baseDir = baseDir;
        new java.io.File(baseDir).mkdirs();
    }

    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        String operation = (String) params.get("operation");
        
        switch (operation) {
            case "read": return readFile((String) params.get("path"));
            case "write": return writeFile((String) params.get("path"), (String) params.get("content"));
            case "delete": return deleteFile((String) params.get("path"));
            case "list": return listFiles((String) params.get("path"));
            case "exists": return fileExists((String) params.get("path"));
            default: return Map.of("error", "Unknown operation: " + operation);
        }
    }

    private Object readFile(String path) {
        try {
            java.io.File file = getFile(path);
            if (!file.exists()) {
                return Map.of("error", "File not found: " + path);
            }
            String content = java.nio.file.Files.readString(file.toPath());
            return Map.of("content", content, "path", path, "size", file.length());
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private Object writeFile(String path, String content) {
        try {
            java.io.File file = getFile(path);
            file.getParentFile().mkdirs();
            java.nio.file.Files.writeString(file.toPath(), content);
            return Map.of("success", true, "path", path, "size", file.length());
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private Object deleteFile(String path) {
        try {
            java.io.File file = getFile(path);
            if (!file.exists()) {
                return Map.of("error", "File not found: " + path);
            }
            boolean deleted = file.delete();
            return Map.of("success", deleted, "path", path);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private Object listFiles(String path) {
        try {
            java.io.File dir = path != null ? getFile(path) : new java.io.File(baseDir);
            if (!dir.exists() || !dir.isDirectory()) {
                return Map.of("error", "Directory not found: " + path);
            }
            String[] files = dir.list();
            return Map.of("files", files != null ? Arrays.asList(files) : List.of(), "path", dir.getPath());
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    private Object fileExists(String path) {
        java.io.File file = getFile(path);
        return Map.of("exists", file.exists(), "isDirectory", file.isDirectory(), "path", path);
    }

    private java.io.File getFile(String path) {
        if (path.startsWith("/")) {
            return new java.io.File(path);
        }
        return new java.io.File(baseDir, path);
    }

    @Override
    public String getDescription() {
        return "File operations: read, write, delete, list, exists. " +
               "Input: {'operation': '...', 'path': '...', 'content': '...'}";
    }

    @Override
    public Map<String, com.senagent.model.ChatRequest.ToolParameter> getParameters() {
        Map<String, com.senagent.model.ChatRequest.ToolParameter> params = new HashMap<>();
        
        com.senagent.model.ChatRequest.ToolParameter opParam = new com.senagent.model.ChatRequest.ToolParameter();
        opParam.setType("string");
        opParam.setDescription("Operation: read, write, delete, list, exists");
        opParam.setRequired(true);
        params.put("operation", opParam);
        
        com.senagent.model.ChatRequest.ToolParameter pathParam = new com.senagent.model.ChatRequest.ToolParameter();
        pathParam.setType("string");
        pathParam.setDescription("File path");
        pathParam.setRequired(true);
        params.put("path", pathParam);
        
        return params;
    }
}
