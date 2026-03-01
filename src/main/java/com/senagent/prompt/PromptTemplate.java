package com.senagent.prompt;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词模板 - 对标LangChain的PromptTemplate
 * 
 * 支持:
 * - 变量替换
 * - 多个模板
 * - 输出格式化
 */
public class PromptTemplate {

    private final String template;
    private final List<String> inputVariables;
    private final Pattern variablePattern = Pattern.compile("\\{(\\w+)\\}");

    public PromptTemplate(String template) {
        this.template = template;
        this.inputVariables = extractVariables(template);
    }

    /**
     * 工厂方法
     */
    public static PromptTemplate fromTemplate(String template) {
        return new PromptTemplate(template);
    }

    /**
     * 格式化
     */
    public String format(Map<String, Object> params) {
        String result = template;
        for (String var : inputVariables) {
            Object value = params.get(var);
            String replacement = value != null ? value.toString() : "";
            result = result.replace("{" + var + "}", replacement);
        }
        return result;
    }

    /**
     * 格式化(使用可变参数)
     */
    public String format(Object... keyValues) {
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            params.put(keyValues[i].toString(), keyValues[i + 1]);
        }
        return format(params);
    }

    /**
     * 提取变量
     */
    private List<String> extractVariables(String template) {
        List<String> vars = new ArrayList<>();
        Matcher matcher = variablePattern.matcher(template);
        while (matcher.find()) {
            vars.add(matcher.group(1));
        }
        return vars;
    }

    public List<String> getInputVariables() {
        return inputVariables;
    }

    public String getTemplate() {
        return template;
    }

    /**
     * 预定义模板
     */
    public static class Templates {

        /**
         * 问答模板
         */
        public static final String QA = """
            请根据以下上下文回答问题。

            上下文:
            {context}

            问题: {question}

            回答:
            """;

        /**
         * 总结模板
         */
        public static final String SUMMARIZE = """
            请总结以下内容:

            {content}

            总结:
            """;

        /**
         * 翻译模板
         */
        public static final String TRANSLATE = """
            请将以下内容翻译成{target_language}:

            {content}

            翻译:
            """;

        /**
         * 代码审查模板
         */
        public static final String CODE_REVIEW = """
            请审查以下代码:

            ```{language}
            {code}
            ```

            请指出:
            1. 问题
            2. 改进建议
            3. 评分(1-10)
            """;

        /**
         * 角色扮演模板
         */
        public static final String ROLEPLAY = """
            你现在扮演{role}。

            背景: {background}

            请根据这个角色回复用户的问题。

            用户: {message}

            {role}:
            """;

        /**
         * 思维链模板
         */
        public static final String CHAIN_OF_THOUGHT = """
            问题: {question}

            让我们一步步思考:

            Step 1: {step1}
            Step 2: {step2}
            Step 3: {step3}

            结论:
            """;

        /**
         * 工具调用模板
         */
        public static final String TOOL_CALL = """
            你可以使用以下工具:
            {tools}

            用户请求: {request}

            请决定使用哪个工具并给出参数。
            """;
    }
}
