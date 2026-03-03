package com.senagent.mcp;

import com.senagent.tool.PythonSandbox;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * MCP Code Execution Server - 按需加载代码执行能力
 * 
 * 核心思路（来自 Claude Code + Anthropic 技术博客）：
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  传统 Tool Calling 模式                                          │
 * │  ─────────────────                                              │
 * │  1. AI 决定调用工具                                              │
 * │  2. 系统把所有工具定义塞进 Context (可能几十K tokens)             │
 * │  3. 工具执行，返回结果                                           │
 * │  4. 结果塞回 Context                                             │
 * │  5. 循环...                                                     │
 * │                                                                 │
 * │  问题: token 消耗大、Context 塞满、延迟高                        │
 * └─────────────────────────────────────────────────────────────────┘
 * 
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  MCP Code Execution 模式                                        │
 * │  ─────────────────                                              │
 * │  1. AI 直接编写代码完成任务                                       │
 * │  2. 沙箱执行代码                                                │
 * │  3. 返回结果                                                    │
 * │  4. 循环...                                                     │
 * │                                                                 │
 * │  优势: 轻量、快速、省 token                                      │
 * └─────────────────────────────────────────────────────────────────┘
 */
@Slf4j
public class CodeExecutionServer implements CodeExecutionCapability {

    private PythonSandbox sandbox;
    
    public CodeExecutionServer() {
        this.sandbox = new PythonSandbox();
    }
    
    public CodeExecutionServer(int timeoutSeconds, int maxOutputSize) {
        this.sandbox = new PythonSandbox(timeoutSeconds, maxOutputSize, 
            java.nio.file.Paths.get("/tmp/sen-agent-sandbox"));
    }
    
    /**
     * 按需获取能力 - 这是 MCP 的核心思想！
     * 
     * 不像传统 Agent 启动时加载所有工具
     * 只在真正需要时，才返回能力描述
     */
    @Override
    public CapabilityInfo getCapability() {
        return new CapabilityInfo(
            new String[]{"python", "javascript"},
            30,
            true,
            "在沙箱中执行代码。支持 Python 和 JavaScript。"
        );
    }
    
    /**
     * 执行 Python 代码
     * 
     * @param code 要执行的 Python 代码
     * @return 执行结果
     */
    public ExecutionResult executePython(String code) {
        ExecutionResult result = new ExecutionResult();
        
        try {
            // 清理危险代码
            code = sanitizeCode(code);
            
            // 执行
            Object execResult = sandbox.execute(Collections.singletonMap("code", code));
            
            @SuppressWarnings("unchecked")
            Map<String, Object> resultMap = (Map<String, Object>) execResult;
            
            result.setSuccess((Boolean) resultMap.getOrDefault("success", false));
            result.setOutput((String) resultMap.getOrDefault("output", ""));
            result.setError((String) resultMap.getOrDefault("error", ""));
            result.setExitCode((Integer) resultMap.getOrDefault("exitCode", 0));
            
        } catch (Exception e) {
            log.error("Python execution error", e);
            result.setSuccess(false);
            result.setError(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 执行代码（通用接口）
     * 
     * @param code 代码
     * @param language 语言 (python/javascript)
     * @return 执行结果
     */
    public ExecutionResult execute(String code, String language) {
        if ("python".equalsIgnoreCase(language)) {
            return executePython(code);
        } else {
            ExecutionResult result = new ExecutionResult();
            result.setSuccess(false);
            result.setError("Unsupported language: " + language);
            return result;
        }
    }
    
    /**
     * 清理危险代码
     */
    private String sanitizeCode(String code) {
        String[] forbidden = {
            "import os",
            "import sys", 
            "import subprocess",
            "import socket",
            "import requests",
            "import urllib",
            "import httpx",
            "__import__",
            "eval(",
            "exec(",
        };
        
        for (String cmd : forbidden) {
            if (code.contains(cmd)) {
                log.warn("Blocked dangerous code: {}", cmd);
                code = code.replace(cmd, "# Blocked: " + cmd);
            }
        }
        
        return code;
    }
    
    /**
     * 执行结果
     */
    public static class ExecutionResult {
        private boolean success;
        private String output;
        private String error;
        private int exitCode;
        private long executionTime;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getOutput() { return output; }
        public void setOutput(String output) { this.output = output; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }
        public long getExecutionTime() { return executionTime; }
        public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
        
        @Override
        public String toString() {
            if (success) {
                return "Output:\n" + output;
            } else {
                return "Error:\n" + error;
            }
        }
    }
}
