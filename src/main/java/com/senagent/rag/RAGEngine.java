package com.senagent.rag;

import java.util.*;

/**
 * RAG引擎 - 检索增强生成
 * 
 * 功能:
 * - 文档加载
 * - 文本分割
 * - 向量化
 * - 相似度检索
 * - 生成答案
 */
public class RAGEngine {

    private VectorStore vectorStore;
    private String systemPrompt;
    private Object llm; // AiService

    public RAGEngine(Object aiService) {
        this.llm = aiService;
        this.vectorStore = new VectorStore();
    }

    /**
     * 添加文档
     */
    public void addDocument(String content) {
        addDocument(content, new HashMap<>());
    }

    public void addDocument(String content, Map<String, Object> metadata) {
        // 分割文档
        TextSplitter splitter = new TextSplitter(1000, 200);
        List<String> chunks = splitter.splitText(content);
        
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> meta = new HashMap<>(metadata);
            meta.put("chunk", i);
            vectorStore.addDocument(chunks.get(i), meta);
        }
    }

    /**
     * 添加文档(从文本)
     */
    public void addDocuments(List<String> contents) {
        for (String content : contents) {
            addDocument(content);
        }
    }

    /**
     * 问答
     */
    public String answer(String question) {
        return answer(question, 3);
    }

    /**
     * 问答(指定返回结果数)
     */
    public String answer(String question, int topK) {
        // 1. 检索相关文档
        List<VectorStore.Document> docs = vectorStore.similaritySearch(question, topK);
        
        if (docs.isEmpty()) {
            return "没有找到相关内容";
        }

        // 2. 构建上下文
        StringBuilder context = new StringBuilder();
        for (VectorStore.Document doc : docs) {
            context.append(doc.getContent()).append("\n\n");
        }

        // 3. 构建提示
        String prompt = buildPrompt(question, context.toString());
        
        // 4. 调用LLM生成答案 (简化版)
        // 实际应该调用AiService
        return generateAnswer(prompt, docs);
    }

    /**
     * MMR检索
     */
    public String answerWithMMR(String question, int topK, double lambda) {
        List<VectorStore.Document> docs = vectorStore.maxMarginalRelevanceSearch(question, topK, lambda);
        
        if (docs.isEmpty()) {
            return "没有找到相关内容";
        }

        StringBuilder context = new StringBuilder();
        for (VectorStore.Document doc : docs) {
            context.append(doc.getContent()).append("\n\n");
        }

        return generateAnswer(buildPrompt(question, context.toString()), docs);
    }

    private String buildPrompt(String question, String context) {
        return "请根据以下上下文回答问题。如果上下文中没有相关信息，请说明没有找到相关信息。\n\n" +
               "上下文:\n" + context + "\n\n" +
               "问题: " + question + "\n\n" +
               "回答:";
    }

    private String generateAnswer(String prompt, List<VectorStore.Document> docs) {
        // 这里应该调用实际的LLM
        // 简化返回检索结果
        StringBuilder result = new StringBuilder();
        result.append("根据检索到的").append(docs.size()).append("个相关文档:\n\n");
        
        for (int i = 0; i < docs.size(); i++) {
            result.append("--- 文档 ").append(i + 1).append(" (相似度: ")
                  .append(String.format("%.2f", docs.get(i).getScore())).append(") ---\n");
            result.append(docs.get(i).getContent().substring(0, Math.min(200, docs.get(i).getContent().length())))
                  .append("...\n\n");
        }
        
        return result.toString();
    }

    /**
     * 获取文档数
     */
    public int getDocumentCount() {
        return vectorStore.count();
    }

    /**
     * 清空索引
     */
    public void clear() {
        vectorStore = new VectorStore();
    }

    // 文本分割器
    public static class TextSplitter {
        private final int chunkSize;
        private final int chunkOverlap;

        public TextSplitter(int chunkSize, int chunkOverlap) {
            this.chunkSize = chunkSize;
            this.chunkOverlap = chunkOverlap;
        }

        public List<String> splitText(String text) {
            if (text == null || text.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> chunks = new ArrayList<>();
            int start = 0;

            while (start < text.length()) {
                int end = Math.min(start + chunkSize, text.length());
                String chunk = text.substring(start, end);
                chunks.add(chunk);
                
                start += chunkSize - chunkOverlap;
                if (start >= text.length()) break;
            }

            return chunks;
        }

        /**
         * 按段落分割
         */
        public List<String> splitByParagraphs(String text) {
            String[] paragraphs = text.split("\\n\\n+");
            List<String> chunks = new ArrayList<>();
            StringBuilder current = new StringBuilder();

            for (String para : paragraphs) {
                if (current.length() + para.length() > chunkSize && current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder();
                }
                current.append(para).append("\n\n");
            }

            if (current.length() > 0) {
                chunks.add(current.toString().trim());
            }

            return chunks;
        }
    }
}
