package com.senagent.mcp;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * MCP Browser Execution - 模拟人类浏览器行为
 * 
 * 核心功能：
 * - 模拟人类滚动（随机停顿、分段滚动）
 * - 模拟人类鼠标移动轨迹
 * - 模拟人类点击行为
 * - 随机延迟
 * - 隐藏自动化特征 (webdriver)
 * 
 * 按需加载：只在真正需要时返回能力描述
 */
@Slf4j
public class BrowserExecutionServer implements BrowserExecutionCapability {

    private boolean headless;
    private Map<String, Object> config;
    
    public BrowserExecutionServer() {
        this(true);
    }
    
    public BrowserExecutionServer(boolean headless) {
        this.headless = headless;
        this.config = new HashMap<>();
        config.put("viewport", Map.of("width", 1920, "height", 1080));
        config.put("userAgent", getRandomUserAgent());
    }
    
    /**
     * 按需获取能力 - MCP 核心思想
     */
    @Override
    public CapabilityInfo getCapability() {
        return new CapabilityInfo(
            "browser_execution",
            new String[]{"scroll", "click", "mouse_move", "wait", "extract"},
            "模拟人类浏览器行为，支持滚动、点击、鼠标移动等操作"
        );
    }
    
    /**
     * 模拟人类滚动
     * 
     * @param minPause 最小停顿时间(秒)
     * @param maxPause 最大停顿时间(秒)
     * @return 滚动结果
     */
    public ScrollResult humanScroll(double minPause, double maxPause) {
        ScrollResult result = new ScrollResult();
        
        // 模拟随机分段滚动
        int segments = (int) (Math.random() * 5) + 3; // 3-8段
        List<Integer> scrollPositions = new ArrayList<>();
        
        int current = 0;
        for (int i = 0; i < segments; i++) {
            current += (int) (Math.random() * 300) + 100; // 100-400px
            scrollPositions.add(current);
            
            // 随机停顿模拟阅读
            try {
                Thread.sleep((long) ((minPause + Math.random() * (maxPause - minPause)) * 1000));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        result.setSuccess(true);
        result.setScrollPositions(scrollPositions);
        result.setTotalScroll(scrollPositions.get(scrollPositions.size() - 1));
        
        return result;
    }
    
    /**
     * 模拟人类鼠标移动
     * 
     * @param startX 起始X
     * @param startY 起始Y
     * @param endX 结束X
     * @param endY 结束Y
     * @return 移动结果
     */
    public MouseMoveResult humanMouseMove(int startX, int startY, int endX, int endY) {
        MouseMoveResult result = new MouseMoveResult();
        
        // 生成中间点（模拟曲线运动）
        List<Map<String, Integer>> points = new ArrayList<>();
        int numPoints = (int) (Math.random() * 10) + 5; // 5-15个点
        
        for (int i = 0; i <= numPoints; i++) {
            double progress = (double) i / numPoints;
            int x = (int) (startX + (endX - startX) * progress + (Math.random() - 0.5) * 60);
            int y = (int) (startY + (endY - startY) * progress + (Math.random() - 0.5) * 60);
            x = Math.max(0, x);
            y = Math.max(0, y);
            
            points.add(Map.of("x", x, "y", y));
        }
        
        result.setSuccess(true);
        result.setPoints(points);
        result.setDuration(points.size() * 50); // 模拟时间
        
        return result;
    }
    
    /**
     * 模拟人类点击
     */
    public ClickResult humanClick(int x, int y) {
        ClickResult result = new ClickResult();
        
        // 先移动到目标位置附近
        try {
            Thread.sleep((long) (Math.random() * 200) + 100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 模拟点击（带微小移动）
        int offsetX = (int) (Math.random() * 10) - 5;
        int offsetY = (int) (Math.random() * 10) - 5;
        
        result.setSuccess(true);
        result.setClickX(x + offsetX);
        result.setClickY(y + offsetY);
        result.setOriginalX(x);
        result.setOriginalY(y);
        
        return result;
    }
    
    /**
     * 随机延迟
     */
    public void randomDelay(double minSec, double maxSec) {
        try {
            Thread.sleep((long) ((minSec + Math.random() * (maxSec - minSec)) * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 获取随机 User-Agent
     */
    private String getRandomUserAgent() {
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
        };
        return userAgents[(int) (Math.random() * userAgents.length)];
    }
    
    // 结果类
    public static class ScrollResult {
        private boolean success;
        private List<Integer> scrollPositions;
        private int totalScroll;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public List<Integer> getScrollPositions() { return scrollPositions; }
        public void setScrollPositions(List<Integer> scrollPositions) { this.scrollPositions = scrollPositions; }
        public int getTotalScroll() { return totalScroll; }
        public void setTotalScroll(int totalScroll) { this.totalScroll = totalScroll; }
    }
    
    public static class MouseMoveResult {
        private boolean success;
        private List<Map<String, Integer>> points;
        private long duration;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public List<Map<String, Integer>> getPoints() { return points; }
        public void setPoints(List<Map<String, Integer>> points) { this.points = points; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
    }
    
    public static class ClickResult {
        private boolean success;
        private int clickX;
        private int clickY;
        private int originalX;
        private int originalY;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public int getClickX() { return clickX; }
        public void setClickX(int clickX) { this.clickX = clickX; }
        public int getClickY() { return clickY; }
        public void setClickY(int clickY) { this.clickY = clickY; }
        public int getOriginalX() { return originalX; }
        public void setOriginalX(int originalX) { this.originalX = originalX; }
        public int getOriginalY() { return originalY; }
        public void setOriginalY(int originalY) { this.originalY = originalY; }
    }
}
