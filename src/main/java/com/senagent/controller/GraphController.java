package com.senagent.controller;

import com.senagent.agent.AgentService;
import com.senagent.agent.MiniAgent;
import com.senagent.graph.StateGraph;
import com.senagent.chain.Chain;
import com.senagent.rag.VectorStore;
import com.senagent.model.ChatRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * LangGraph风格API - 对标LangGraph的REST API
 */
@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final Map<String, StateGraph> graphs = new HashMap<>();
    private final Map<String, StateGraph.CompiledGraph> compiled = new HashMap<>();

    /**
     * 创建图
     */
    @PostMapping
    public Map<String, Object> createGraph(@RequestBody CreateGraphRequest request) {
        StateGraph graph = new StateGraph(request.getName());
        
        // 添加节点
        if (request.getNodes() != null) {
            for (var node : request.getNodes()) {
                graph.addNode(node.getName(), state -> {
                    // 简单节点实现
                    return Map.of("result", "Node " + node.getName() + " executed");
                });
            }
        }
        
        // 添加边
        if (request.getEdges() != null) {
            for (var edge : request.getEdges()) {
                graph.addEdge(edge.getFrom(), edge.getTo());
            }
        }
        
        graphs.put(request.getName(), graph);
        
        return Map.of(
            "name", request.getName(),
            "nodes", request.getNodes() != null ? request.getNodes().size() : 0,
            "status", "created"
        );
    }

    /**
     * 编译图
     */
    @PostMapping("/{name}/compile")
    public Map<String, Object> compileGraph(@PathVariable String name) {
        StateGraph graph = graphs.get(name);
        if (graph == null) {
            return Map.of("error", "Graph not found: " + name);
        }
        
        graph.setStart("node1"); // 默认起点
        StateGraph.CompiledGraph compiledGraph = graph.compile();
        compiled.put(name, compiledGraph);
        
        return Map.of("name", name, "status", "compiled");
    }

    /**
     * 执行图
     */
    @PostMapping("/{name}/invoke")
    public Map<String, Object> invokeGraph(@PathVariable String name, @RequestBody Map<String, Object> input) {
        StateGraph.CompiledGraph compiledGraph = compiled.get(name);
        if (compiledGraph == null) {
            return Map.of("error", "Graph not compiled: " + name);
        }
        
        Map<String, Object> result = compiledGraph.invoke(input);
        return result;
    }

    /**
     * 列出所有图
     */
    @GetMapping
    public Map<String, Object> listGraphs() {
        return Map.of(
            "graphs", graphs.keySet(),
            "count", graphs.size()
        );
    }

    // Request classes
    public static class CreateGraphRequest {
        private String name;
        private List<Node> nodes;
        private List<Edge> edges;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Node> getNodes() { return nodes; }
        public void setNodes(List<Node> nodes) { this.nodes = nodes; }
        public List<Edge> getEdges() { return edges; }
        public void setEdges(List<Edge> edges) { this.edges = edges; }
    }

    public static class Node {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Edge {
        private String from;
        private String to;
        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }
        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }
    }
}
