package com.senagent.agent;

import com.senagent.service.AiService;
import lombok.extern.slf4j;

import java.util.*;

/**
 * Agent工厂 - 对标LangChain的agent factory
 */
@Slf4j
public class AgentFactory {

    private final AiService aiService;
    private final Map<String, Agent> agents = new HashMap<>();

    public AgentFactory(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 创建通用对话Agent
     */
    public Agent createChatAgent(String name, String systemPrompt) {
        MiniAgent agent = new MiniAgent(aiService, systemPrompt);
        agents.put(name, agent);
        return agent;
    }

    /**
     * 创建函数调用Agent
     */
    public FnCallAgent createFnCallAgent(String name, String systemPrompt) {
        FnCallAgent agent = new FnCallAgent(aiService, systemPrompt);
        agents.put(name, agent);
        return agent;
    }

    /**
     * 创建ReAct Agent
     */
    public ReActAgent createReActAgent(String name, String systemPrompt) {
        ReActAgent agent = new ReActAgent(aiService, systemPrompt);
        agents.put(name, agent);
        return agent;
    }

    /**
     * 创建代码Agent
     */
    public CodeAgent createCodeAgent(String name) {
        CodeAgent agent = new CodeAgent(aiService);
        agents.put(name, agent);
        return agent;
    }

    /**
     * 创建浏览器Agent
     */
    public com.senagent.agent.browser.BrowserAgent createBrowserAgent(String name) {
        com.senagent.agent.browser.BrowserAgent agent = new com.senagent.agent.browser.BrowserAgent(aiService);
        agents.put(name, agent);
        return agent;
    }

    /**
     * 创建Runnable Agent (LCEL风格)
     */
    public RunnableAgent createRunnableAgent(String name, String systemPrompt) {
        RunnableAgent agent = new RunnableAgent(name, systemPrompt, aiService);
        agents.put(name, agent);
        return agent;
    }

    /**
     * 获取Agent
     */
    public Agent get(String name) {
        return agents.get(name);
    }

    /**
     * 列出所有Agent
     */
    public List<String> listAgents() {
        return new ArrayList<>(agents.keySet());
    }

    /**
     * 预设Agent模板
     */
    public static class Templates {

        public static final String ASSISTANT = "你是一个有帮助的AI助手。";
        
        public static final String CODER = """你是一个专业的程序员助手。
你的职责:
1. 编写高质量代码
2. 代码审查
3. 解释代码逻辑
4. 优化性能
5. 修复bug
请用中文回复。""";

        public static final String ANALYST = """你是一个专业的数据分析师。
你的职责:
1. 分析数据趋势
2. 生成统计报告
3. 可视化建议
4. 商业洞察
请用中文回复。""";

        public static final String RESEARCHER = """你是一个专业的研究员。
你的职责:
1. 搜索信息
2. 整理资料
3. 总结要点
4. 提供洞见
请用中文回复。""";

        public static final String WRITER = """你是一个专业的作家。
你的职责:
1. 撰写文章
2. 编辑润色
3. 内容创作
4. 创意写作
请用中文回复。""";

        public static final String MATHEMATICIAN = """你是一个数学专家。
你的职责:
1. 数学计算
2. 公式推导
3. 证明解答
4. 逻辑推理
请用中文回复。""";
    }

    public interface Agent {
        Object chat(String message);
    }
}
