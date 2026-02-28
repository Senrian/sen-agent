package com.senagent.mcp;

import lombok.Data;
import lombok.extern.slf4j;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP协议支持 - 对标OpenClaw的MCP (Model Context Protocol)
 * 
 * 核心组件:
 * - Server: MCP服务器
 * - Client: MCP客户端
 * - Tool: 工具定义
 * - Resource: 资源定义
 * - Prompt: 提示模板
 */
@Slf4j
public class MCPServer {

    private final String name;
    private final String version;
    private final Map<String, MCPTool> tools = new ConcurrentHashMap<>();
    private final Map<String, MCPResource> resources = new ConcurrentHashMap<>();
    private final Map<String, MCPPrompt> prompts = new ConcurrentHashMap<>();
    private MCPTransport transport;

    public MCPServer(String name, String version) {
        this.name = name;
        this.version = version;
    }

    /**
     * 注册工具
     */
    public void registerTool(MCPTool tool) {
        tools.put(tool.getName(), tool);
        log.info("Registered MCP tool: {}", tool.getName());
    }

    /**
     * 注册资源
     */
    public void registerResource(MCPResource resource) {
        resources.put(resource.getUri(), resource);
    }

    /**
     * 注册提示
     */
    public void registerPrompt(MCPPrompt prompt) {
        prompts.put(prompt.getName(), prompt);
    }

    /**
     * 处理请求
     */
    public MCPResponse handleRequest(MCPRequest request) {
        log.debug("Handling MCP request: {}", request.getMethod());
        
        try {
            switch (request.getMethod()) {
                case "tools/list":
                    return handleToolsList();
                case "tools/call":
                    return handleToolsCall(request);
                case "resources/list":
                    return handleResourcesList();
                case "resources/read":
                    return handleResourcesRead(request);
                case "prompts/list":
                    return handlePromptsList();
                case "prompts/get":
                    return handlePromptsGet(request);
                default:
                    return MCPResponse.error("Unknown method: " + request.getMethod());
            }
        } catch (Exception e) {
            log.error("MCP request error", e);
            return MCPResponse.error(e.getMessage());
        }
    }

    private MCPResponse handleToolsList() {
        List<Map<String, Object>> toolList = new ArrayList<>();
        for (MCPTool tool : tools.values()) {
            toolList.add(Map.of(
                "name", tool.getName(),
                "description", tool.getDescription(),
                "inputSchema", tool.getInputSchema()
            ));
        }
        return MCPResponse.success(Map.of("tools", toolList));
    }

    private MCPResponse handleToolsCall(MCPRequest request) {
        Map<String, Object> params = request.getParams();
        String name = (String) params.get("name");
        Map<String, Object> args = (Map<String, Object>) params.get("arguments");
        
        MCPTool tool = tools.get(name);
        if (tool == null) {
            return MCPResponse.error("Tool not found: " + name);
        }
        
        Object result = tool.execute(args);
        return MCPResponse.success(Map.of("content", List.of(Map.of("type", "text", "text", result.toString()))));
    }

    private MCPResponse handleResourcesList() {
        List<Map<String, Object>> resourceList = new ArrayList<>();
        for (MCPResource resource : resources.values()) {
            resourceList.add(Map.of(
                "uri", resource.getUri(),
                "name", resource.getName(),
                "description", resource.getDescription(),
                "mimeType", resource.getMimeType()
            ));
        }
        return MCPResponse.success(Map.of("resources", resourceList));
    }

    private MCPResponse handleResourcesRead(MCPRequest request) {
        String uri = (String) request.getParams().get("uri");
        MCPResource resource = resources.get(uri);
        
        if (resource == null) {
            return MCPResponse.error("Resource not found: " + uri);
        }
        
        Object content = resource.read();
        return MCPResponse.success(Map.of("contents", List.of(Map.of(
            "uri", uri,
            "mimeType", resource.getMimeType(),
            "text", content.toString()
        ))));
    }

    private MCPResponse handlePromptsList() {
        List<Map<String, Object>> promptList = new ArrayList<>();
        for (MCPPrompt prompt : prompts.values()) {
            promptList.add(Map.of(
                "name", prompt.getName(),
                "description", prompt.getDescription(),
                "arguments", prompt.getArguments()
            ));
        }
        return MCPResponse.success(Map.of("prompts", promptList));
    }

    private MCPResponse handlePromptsGet(MCPRequest request) {
        String name = (String) request.getParams().get("name");
        Map<String, Object> args = (Map<String, Object>) request.getParams().get("arguments");
        
        MCPPrompt prompt = prompts.get(name);
        if (prompt == null) {
            return MCPResponse.error("Prompt not found: " + name);
        }
        
        String rendered = prompt.render(args);
        return MCPResponse.success(Map.of("messages", List.of(Map.of(
            "role", "user",
            "content", Map.of("type", "text", "text", rendered)
        ))));
    }

    // Getters
    public String getName() { return name; }
    public String getVersion() { return version; }
    public Map<String, MCPTool> getTools() { return tools; }

    // 数据类
    @Data
    public static class MCPTool {
        private String name;
        private String description;
        private Map<String, Object> inputSchema;
        private ToolHandler handler;

        public interface ToolHandler {
            Object execute(Map<String, Object> args) throws Exception;
        }

        public Object execute(Map<String, Object> args) {
            if (handler != null) {
                return handler.execute(args);
            }
            return "Tool not implemented";
        }
    }

    @Data
    public static class MCPResource {
        private String uri;
        private String name;
        private String description;
        private String mimeType;
        private ResourceReader reader;

        public interface ResourceReader {
            Object read() throws Exception;
        }

        public Object read() {
            if (reader != null) {
                return reader.read();
            }
            return "Resource not readable";
        }
    }

    @Data
    public static class MCPPrompt {
        private String name;
        private String description;
        private Map<String, Object> arguments;
        private String template;
        private PromptRenderer renderer;

        public interface PromptRenderer {
            String render(Map<String, Object> args);
        }

        public String render(Map<String, Object> args) {
            if (renderer != null) {
                return renderer.render(args);
            }
            return template;
        }
    }

    @Data
    public static class MCPRequest {
        private String jsonrpc = "2.0";
        private String method;
        private Map<String, Object> params;
        private String id;
    }

    @Data
    public static class MCPResponse {
        private String jsonrpc = "2.0";
        private Object result;
        private Object error;
        private String id;

        public static MCPResponse success(Object result) {
            MCPResponse resp = new MCPResponse();
            resp.setResult(result);
            return resp;
        }

        public static MCPResponse error(String message) {
            MCPResponse resp = new MCPResponse();
            resp.setError(Map.of("code", -32600, "message", message));
            return resp;
        }
    }

    public interface MCPTransport {
        void send(MCPResponse response);
        void onRequest(MCPRequest request);
    }
}
