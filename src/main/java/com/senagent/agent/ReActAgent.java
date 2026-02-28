package com.senagent.agent;

import com.senagent.model.ChatRequest;
import com.senagent.model.ChatResponse;
import com.senagent.service.AiService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * ReAct Agent - 改进版
 * 
 * 实现ReAct (Reasoning + Acting) 模式:
 * - Thought: 思考
 * - Action: 行动
 * - Observation: 观察
 * - 循环直到完成
 */
@Slf4j
public class ReActAgent {

    private String id;
    private String name;
    private String systemPrompt;
    private List<ChatRequest.Message> history;
    private AiService aiService;
    private Map<String, Tool> tools;
    private int maxIterations;
    private boolean verbose;

    public ReActAgent(AiService aiService, String systemPrompt) {
        this(aiService, systemPrompt, new HashMap<>());
    }

    public ReActAgent(AiService aiService, String systemPrompt, Map<String, Tool> tools) {
        this.id = UUID.randomUUID().toString();
        this.aiService = aiService;
        this.systemPrompt = systemPrompt;
        this.tools = tools;
        this.history = new ArrayList<>();
        this.maxIterations = 10;
        this.verbose = false;
    }

    /**
     * 执行对话
     */
    public Result chat(String userMessage) {
        history.add(createMessage("user", userMessage));
        
        String currentThought = "";
        String currentAction = "";
        String currentObservation = "";
        
        for (int i = 0; i < maxIterations; i++) {
            // 构建提示
            String prompt = buildPrompt(userMessage, currentThought, currentAction, currentObservation);
            
            // 调用LLM
            ChatRequest request = new ChatRequest();
            request.setMessages(List.of(createMessage("user", prompt)));
            request.setSystemPrompt(systemPrompt);
            
            ChatResponse response = aiService.chat(request);
            String content = response.getContent();
            
            // 解析响应
            ParsedStep step = parseStep(content);
            
            if (step == null) {
                // 没有有效的步骤，返回结果
                history.add(createMessage("assistant", content));
                return new Result(content, i + 1, false);
            }
            
            currentThought = step.thought;
            currentAction = step.action;
            
            // 执行动作
            if ("finish".equalsIgnoreCase(step.action)) {
                history.add(createMessage("assistant", step.observation));
                return new Result(step.observation, i + 1, true);
            }
            
            // 执行工具
            currentObservation = executeTool(step.action, step.actionInput);
            
            if (verbose) {
                log.info("Step {}: {} -> {} -> {}", i + 1, step.thought, step.action, currentObservation);
            }
        }
        
        // 超时
        String finalMsg = "达到最大迭代次数 " + maxIterations;
        history.add(createMessage("assistant", finalMsg));
        return new Result(finalMsg, maxIterations, false);
    }

    private String buildPrompt(String userMessage, String thought, String action, String observation) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Question: ").append(userMessage).append("\n\n");
        
        if (!thought.isEmpty()) {
            sb.append("Thought: ").append(thought).append("\n");
        }
        if (!action.isEmpty()) {
            sb.append("Action: ").append(action).append("\n");
        }
        if (!observation.isEmpty()) {
            sb.append("Observation: ").append(observation).append("\n");
        }
        
        sb.append("\n请按以下格式回答:\n");
        sb.append("Thought: [你的思考]\n");
        sb.append("Action: [工具名 或 finish]\n");
        sb.append("Action Input: [工具输入]\n");
        
        if (!tools.isEmpty()) {
            sb.append("\n可用工具: ").append(String.join(", ", tools.keySet()));
        }
        
        return sb.toString();
    }

    private ParsedStep parseStep(String content) {
        if (content == null || content.isEmpty()) return null;
        
        ParsedStep step = new ParsedStep();
        
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Thought:")) {
                step.thought = line.substring("Thought:".length()).trim();
            } else if (line.startsWith("Action:")) {
                step.action = line.substring("Action:".length()).trim();
            } else if (line.startsWith("Action Input:")) {
                step.actionInput = line.substring("Action Input:".length()).trim();
            } else if (line.startsWith("Observation:")) {
                step.observation = line.substring("Observation:".length()).trim();
            }
        }
        
        return step.action != null && !step.action.isEmpty() ? step : null;
    }

    private String executeTool(String action, String actionInput) {
        Tool tool = tools.get(action);
        if (tool == null) {
            return "工具不存在: " + action;
        }
        
        try {
            Map<String, Object> args = parseJson(actionInput);
            Object result = tool.execute(args);
            return result != null ? result.toString() : "null";
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("input", json);
            return result;
        }
    }

    private ChatRequest.Message createMessage(String role, String content) {
        ChatRequest.Message msg = new ChatRequest.Message();
        msg.setRole(role);
        msg.setContent(content);
        return msg;
    }

    // Setters
    public void setTools(Map<String, Tool> tools) { this.tools = tools; }
    public void setMaxIterations(int maxIterations) { this.maxIterations = maxIterations; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }
    public void clearHistory() { history.clear(); }

    // Tool接口
    public interface Tool {
        Object execute(Map<String, Object> args) throws Exception;
    }

    @Data
    private static class ParsedStep {
        String thought;
        String action;
        String actionInput;
        String observation;
    }

    @Data
    public static class Result {
        private final String output;
        private final int iterations;
        private final boolean finished;
    }
}
