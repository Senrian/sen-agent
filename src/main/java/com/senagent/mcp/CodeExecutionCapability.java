package com.senagent.mcp;

/**
 * MCP Code Execution 能力接口
 * 
 * 按需加载核心思想（来自 Claude Code / Anthropic 技术博客）：
 * - 传统 Agent: 把所有工具定义塞进 Context (token 黑洞)
 * - MCP 方式: 按需获取能力，只在真正需要时才加载
 * 
 * 这个接口定义了代码执行能力的标准获取方式
 */
public interface CodeExecutionCapability {
    
    /**
     * 按需获取能力描述
     * 
     * 关键点：不在启动时暴露所有工具定义
     * 而是在真正需要时，才返回能力描述
     */
    CapabilityInfo getCapability();
    
    /**
     * 能力信息
     */
    class CapabilityInfo {
        private String type = "code_execution";
        private String[] languages;
        private int maxExecutionTime;
        private boolean sandboxed;
        private String description;
        
        public CapabilityInfo() {}
        
        public CapabilityInfo(String[] languages, int maxExecutionTime, boolean sandboxed, String description) {
            this.languages = languages;
            this.maxExecutionTime = maxExecutionTime;
            this.sandboxed = sandboxed;
            this.description = description;
        }
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String[] getLanguages() { return languages; }
        public void setLanguages(String[] languages) { this.languages = languages; }
        public int getMaxExecutionTime() { return maxExecutionTime; }
        public void setMaxExecutionTime(int maxExecutionTime) { this.maxExecutionTime = maxExecutionTime; }
        public boolean isSandboxed() { return sandboxed; }
        public void setSandboxed(boolean sandboxed) { this.sandboxed = sandboxed; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
