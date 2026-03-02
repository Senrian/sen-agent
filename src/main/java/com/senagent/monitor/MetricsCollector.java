package com.senagent.monitor;

import lombok.Data;
import lombok.extern.slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 指标收集器 - 可观测性
 */
@Slf4j
public class MetricsCollector {

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Gauge> gauges = new ConcurrentHashMap<>();
    private final Map<String, Histogram> histograms = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();

    /** 计数器 */
    public void incrementCounter(String name) {
        counters.computeIfAbsent(name, k -> new Counter()).inc();
    }

    public void incrementCounter(String name, long delta) {
        counters.computeIfAbsent(name, k -> new Counter()).inc(delta);
    }

    public long getCounter(String name) {
        Counter c = counters.get(name);
        return c != null ? c.getValue() : 0;
    }

    /** 仪表 */
    public void setGauge(String name, double value) {
        gauges.computeIfAbsent(name, k -> new Gauge()).set(value);
    }

    public double getGauge(String name) {
        Gauge g = gauges.get(name);
        return g != null ? g.getValue() : 0;
    }

    /** 直方图 */
    public void recordHistogram(String name, double value) {
        histograms.computeIfAbsent(name, k -> new Histogram()).record(value);
    }

    public HistogramData getHistogram(String name) {
        Histogram h = histograms.get(name);
        return h != null ? h.getData() : new HistogramData();
    }

    /** 计时器 */
    public void recordTime(String name, long ms) {
        timers.computeIfAbsent(name, k -> new Timer()).record(ms);
    }

    public TimerData getTimer(String name) {
        Timer t = timers.get(name);
        return t != null ? t.getData() : new TimerData();
    }

    /** 获取所有指标 */
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        Map<String, Long> counterData = new HashMap<>();
        counters.forEach((k, v) -> counterData.put(k, v.getValue()));
        metrics.put("counters", counterData);

        Map<String, Double> gaugeData = new HashMap<>();
        gauges.forEach((k, v) -> gaugeData.put(k, v.getValue()));
        metrics.put("gauges", gaugeData);

        metrics.put("histograms", histograms.keySet().stream()
                .collect(java.util.stream.Collectors.toMap(k -> k, this::getHistogram)));

        metrics.put("timers", timers.keySet().stream()
                .collect(java.util.stream.Collectors.toMap(k -> k, this::getTimer)));

        return metrics;
    }

    static class Counter {
        private final AtomicLong count = new AtomicLong(0);
        public void inc() { count.incrementAndGet(); }
        public void inc(long delta) { count.addAndGet(delta); }
        public long getValue() { return count.get(); }
    }

    static class Gauge {
        private volatile double value = 0;
        public void set(double value) { this.value = value; }
        public double getValue() { return value; }
    }

    static class Histogram {
        private final List<Double> values = Collections.synchronizedList(new ArrayList<>());
        public void record(double value) { values.add(value); }
        public synchronized HistogramData getData() {
            if (values.isEmpty()) return new HistogramData();
            Collections.sort(values);
            double sum = values.stream().mapToDouble(v -> v).sum();
            return new HistogramData(values.size(), values.get(0), values.get(values.size() - 1),
                    values.get(values.size() / 2), sum / values.size());
        }
    }

    static class Timer {
        private final Histogram histogram = new Histogram();
        public void record(long ms) { histogram.record(ms); }
        public TimerData getData() {
            HistogramData d = histogram.getData();
            return new TimerData(d.count, d.min, d.max, d.p50, d.mean);
        }
    }

    @Data
    public static class HistogramData {
        private long count;
        private double min, max, p50, mean;
        public HistogramData() {}
        public HistogramData(long c, double min, double max, double p50, double mean) {
            this.count = c; this.min = min; this.max = max; this.p50 = p50; this.mean = mean;
        }
    }

    @Data
    public static class TimerData {
        private long count;
        private double min, max, p50, mean;
        public TimerData() {}
        public TimerData(long c, double min, double max, double p50, double mean) {
            this.count = c; this.min = min; this.max = max; this.p50 = p50; this.mean = mean;
        }
    }
}
