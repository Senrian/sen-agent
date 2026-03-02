package com.senagent.agent;

import com.senagent.model.ChatRequest;
import com.senagent.model.ChatResponse;
import com.senagent.service.AiService;
import com.senagent.memory.ChatMemory;
import com.senagent.tool.ToolRegistry;
import lombok.Data;
import lombok.extern.slf4j;

import java.util.*;

/**
 * LCEL风格的Agent - 对标LangChain的Runnable
 * 
 * 特性:
 * - 链式调用
 * - 流式输出
 * - 绑定工具
 * - 记忆管理
 */
@Slf4j
public class RunnableAgent {

    private final String id;
    private final String name;
    private final String systemPrompt;
    private final AiService aiService;
    private final List<Object> tools = new ArrayList<>();
    private ChatMemory memory;
    private int maxIterations = 5;
    private boolean streamEnabled = false;

    public RunnableAgent(String name, String systemPrompt, AiService aiService) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.aiService = aiService;
        this.memory = new ChatMemory(20);
    }

    /**
     * 绑定工具
     */
    public RunnableAgent bindTools(List<Object> tools) {
        this.tools.addAll(tools);
        return this;
    }

    /**
     * 设置记忆
     */
    public RunnableAgent withMemory(ChatMemory memory) {
        this.memory = memory;
        return this;
    }

    /**
     * 流式输出
     */
    public RunnableAgent withStreaming() {
        this.streamEnabled = true;
        return this;
    }

    /**
     * 执行 - 对标invoke()
     */
    public AgentOutput invoke(String input) {
        memory.addUserMessage(input);
        
        ChatRequest request = new ChatRequest();
        request.setSystemPrompt(systemPrompt);
        request.setMessages(memory.getMessages());
        request.setTools(convertTools(tools));
        
        ChatResponse response = aiService.chat(request);
        
        String result = response.getContent();
        memory.addAIResponse(result);
        
        AgentOutput output = new AgentOutput();
        output.setOutput(result);
        output.setToolCalls(response.getToolCalls());
        
        // 处理工具调用
        if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
            output.setHasToolCalls(true);
        }
        
        return output;
    }

    /**
     * 批量执行
     */
    public List<AgentOutput> batch(List<String> inputs) {
        List<AgentOutput> outputs = new ArrayList<>();
        for (String input : inputs) {
            outputs.add(invoke(input));
        }
        return outputs;
    }

    /**
     * 清空记忆
     */
    public void clearMemory() {
        memory.clear();
    }

    private List<ChatRequest.ToolDefinition> convertTools(List<Object> tools) {
        // 转换工具定义
        return new ArrayList<>();
    }

    @Data
    public static class AgentOutput {
        private String output;
        private List<ChatResponse.ToolCall> toolCalls;
        private boolean hasToolCalls;
        private Map<String, Object> metadata = new HashMap<>();
    }
}
