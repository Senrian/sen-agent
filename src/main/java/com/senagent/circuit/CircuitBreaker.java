package com.senagent.circuit;

import lombok.extern.slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 熔断器 - 健壮性与容错
 */
@Slf4j
public class CircuitBreaker {

    private final String name;
    private final int failureThreshold;
    private final long resetTimeoutMs;
    private State state = State.CLOSED;
    private AtomicInteger failureCount = new AtomicInteger(0);
    private long lastFailureTime = 0;

    public CircuitBreaker(String name, int failureThreshold, long resetTimeoutMs) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
    }

    /**
     * 执行保护
     */
    public <T> T execute(Supplier<T> action) {
        if (state == State.OPEN) {
            if (shouldAttemptReset()) {
                state = State.HALF_OPEN;
                log.info("Circuit {}: HALF_OPEN", name);
            } else {
                throw new CircuitOpenException("Circuit " + name + " is OPEN");
            }
        }

        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    private void onSuccess() {
        failureCount.set(0);
        if (state == State.HALF_OPEN) {
            state = State.CLOSED;
            log.info("Circuit {}: CLOSED", name);
        }
    }

    private void onFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime = System.currentTimeMillis();

        if (failures >= failureThreshold) {
            state = State.OPEN;
            log.warn("Circuit {}: OPEN ({} failures)", name, failures);
        }
    }

    private boolean shouldAttemptReset() {
        return System.currentTimeMillis() - lastFailureTime > resetTimeoutMs;
    }

    public State getState() { return state; }
    public int getFailureCount() { return failureCount.get(); }

    public enum State { CLOSED, OPEN, HALF_OPEN }

    @FunctionalInterface
    public interface Supplier<T> {
        T get() throws Exception;
    }

    public static class CircuitOpenException extends RuntimeException {
        public CircuitOpenException(String msg) { super(msg); }
    }

    /**
     * 工厂
     */
    private static final Map<String, CircuitBreaker> breakers = new HashMap<>();

    public static CircuitBreaker getOrCreate(String name) {
        return breakers.computeIfAbsent(name, k -> new CircuitBreaker(name, 5, 30000));
    }
}
