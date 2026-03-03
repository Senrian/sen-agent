package com.senagent.mcp;

/**
 * MCP Browser Execution 能力接口
 * 
 * 按需加载核心思想：
 * - 不在启动时暴露所有浏览器操作
 * - 只在真正需要时，才返回能力描述
 */
public interface BrowserExecutionCapability {
    
    CapabilityInfo getCapability();
    
    class CapabilityInfo {
        private String type;
        private String[] actions;
        private String description;
        
        public CapabilityInfo() {}
        
        public CapabilityInfo(String type, String[] actions, String description) {
            this.type = type;
            this.actions = actions;
            this.description = description;
        }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String[] getActions() { return actions; }
        public void setActions(String[] actions) { this.actions = actions; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
