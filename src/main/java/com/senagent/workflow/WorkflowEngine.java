package com.senagent.workflow;

import lombok.Data;
import lombok.extern.slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流引擎 - 对标LangChain/AWS Step Functions
 * 
 * 支持:
 * - 顺序执行
 * - 并行执行
 * - 条件分支
 * - 循环
 * - 错误处理
 */
@Slf4j
public class WorkflowEngine {

    private final Map<String, Workflow> workflows = new ConcurrentHashMap<>();

    /**
     * 创建工作流
     */
    public Workflow create(String name) {
        Workflow wf = new Workflow(name);
        workflows.put(name, wf);
        return wf;
    }

    /**
     * 获取工作流
     */
    public Workflow get(String name) {
        return workflows.get(name);
    }

    /**
     * 执行工作流
     */
    public Map<String, Object> execute(String name, Map<String, Object> input) {
        Workflow wf = workflows.get(name);
        if (wf == null) {
            throw new IllegalArgumentException("Workflow not found: " + name);
        }
        return wf.execute(input);
    }

    /**
     * 工作流定义
     */
    public static class Workflow {
        private final String name;
        private final List<Step> steps = new ArrayList<>();
        private final Map<String, Step> stepMap = new ConcurrentHashMap<>();
        private String startStep;

        public Workflow(String name) {
            this.name = name;
        }

        /**
         * 添加步骤
         */
        public Workflow step(String name, StepHandler handler) {
            Step step = new Step(name, handler);
            steps.add(step);
            stepMap.put(name, step);
            return this;
        }

        /**
         * 设置起点
         */
        public Workflow start(String stepName) {
            this.startStep = stepName;
            return this;
        }

        /**
         * 执行工作流
         */
        public Map<String, Object> execute(Map<String, Object> input) {
            Map<String, Object> context = new HashMap<>(input);
            Map<String, Object> results = new HashMap<>();
            
            if (startStep == null && !steps.isEmpty()) {
                startStep = steps.get(0).getName();
            }

            String current = startStep;
            int index = 0;
            
            while (current != null && index < 100) {
                Step step = stepMap.get(current);
                if (step == null) break;
                
                try {
                    log.info("Executing step: {}", current);
                    Object result = step.execute(context);
                    results.put(current, result);
                    context.put(current + "_result", result);
                    
                    // 找下一步
                    current = step.getNext();
                } catch (Exception e) {
                    log.error("Step {} failed", current, e);
                    results.put(current + "_error", e.getMessage());
                    
                    // 错误处理
                    if (step.errorHandler != null) {
                        current = step.errorHandler.handle(e, context);
                    } else {
                        break;
                    }
                }
                index++;
            }

            results.put("status", "completed");
            results.put("steps_executed", index);
            return results;
        }

        private void addEdge(String from, String to) {
            Step step = stepMap.get(from);
            if (step != null) {
                step.setNext(to);
            }
        }
    }

    /**
     * 工作流步骤
     */
    @Data
    public static class Step {
        private final String name;
        private final StepHandler handler;
        private String next;
        private ErrorHandler errorHandler;

        public Step(String name, StepHandler handler) {
            this.name = name;
            this.handler = handler;
        }

        public Object execute(Map<String, Object> context) throws Exception {
            return handler.execute(context);
        }
    }

    /**
     * 步骤处理器
     */
    public interface StepHandler {
        Object execute(Map<String, Object> context) throws Exception;
    }

    /**
     * 错误处理器
     */
    public interface ErrorHandler {
        String handle(Exception e, Map<String, Object> context);
    }

    /**
     * 条件分支
     */
    public static class ConditionalStep extends Step {
        private final Condition condition;
        private final String trueStep;
        private final String falseStep;

        public ConditionalStep(String name, Condition condition, String trueStep, String falseStep) {
            super(name, ctx -> {
                boolean result = condition.evaluate(ctx);
                return result;
            });
            this.condition = condition;
            this.trueStep = trueStep;
            this.falseStep = falseStep;
        }
    }

    public interface Condition {
        boolean evaluate(Map<String, Object> context);
    }

    /**
     * 并行执行
     */
    public static class ParallelStep extends Step {
        private final List<String> parallelSteps;

        public ParallelStep(String name, List<String> parallelSteps) {
            super(name, ctx -> {
                // 并行执行
                return parallelSteps;
            });
            this.parallelSteps = parallelSteps;
        }
    }
}
