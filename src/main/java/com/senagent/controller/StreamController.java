package com.senagent.controller;

import com.senagent.model.ChatRequest;
import com.senagent.model.ChatResponse;
import com.senagent.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流式API控制器 - 支持SSE
 */
@Slf4j
@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final AiService aiService;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public StreamController(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * 创建流式会话
     */
    @PostMapping("/create")
    public Map<String, Object> createStream() {
        String sessionId = java.util.UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        emitters.put(sessionId, emitter);
        
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));
        emitter.onError(e -> {
            log.error("SSE error: {}", e.getMessage());
            emitters.remove(sessionId);
        });
        
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("expiresIn", 300);
        
        return result;
    }

    /**
     * 流式聊天
     */
    @PostMapping(value = "/chat/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Void> streamChat(
            @PathVariable String sessionId,
            @RequestBody ChatRequest request,
            SseEmitter emitter) {
        
        try {
            // 发送开始信号
            emitter.send(SseEmitter.event().name("start").data("start"));
            
            aiService.chatStream(request, new AiService.StreamCallback() {
                @Override
                public void onMessage(String data) {
                    try {
                        emitter.send(SseEmitter.event().name("chunk").data(data));
                    } catch (IOException e) {
                        log.error("Send chunk error", e);
                    }
                }

                @Override
                public void onComplete() {
                    try {
                        emitter.send(SseEmitter.event().name("complete").data("complete"));
                        emitter.complete();
                    } catch (IOException e) {
                        log.error("Complete error", e);
                    }
                }

                @Override
                public void onError(String error) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data(error));
                        emitter.completeWithError(new RuntimeException(error));
                    } catch (IOException e) {
                        log.error("Error send error", e);
                    }
                }
            });
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Stream chat error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 简单流式聊天
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Void> simpleStreamChat(
            @RequestBody ChatRequest request,
            SseEmitter emitter) {
        
        try {
            aiService.chatStream(request, new AiService.StreamCallback() {
                @Override
                public void onMessage(String data) {
                    try {
                        emitter.send(SseEmitter.event().data(data));
                    } catch (IOException e) {
                        log.debug("Stream send error", e);
                    }
                }

                @Override
                public void onComplete() {
                    try {
                        emitter.complete();
                    } catch (Exception e) {
                        log.debug("Stream complete error", e);
                    }
                }

                @Override
                public void onError(String error) {
                    try {
                        emitter.completeWithError(new RuntimeException(error));
                    } catch (Exception e) {
                        log.debug("Stream error error", e);
                    }
                }
            });
            
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Stream error", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 关闭会话
     */
    @DeleteMapping("/close/{sessionId}")
    public Map<String, Object> closeStream(@PathVariable String sessionId) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            emitter.complete();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sessionId", sessionId);
        
        return result;
    }
}
