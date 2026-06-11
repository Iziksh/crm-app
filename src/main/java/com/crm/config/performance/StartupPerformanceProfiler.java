package com.crm.config.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Temporary startup/load profiler. Enable with {@code app.performance.profiling=true}.
 * Remove or disable after performance investigation is complete.
 */
public final class StartupPerformanceProfiler {

    private static final Logger log = LoggerFactory.getLogger(StartupPerformanceProfiler.class);

    private static volatile boolean enabled;
    private static volatile long jvmStartNanos = System.nanoTime();

    private static final ConcurrentHashMap<String, Long> timingsNanos = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> counters = new ConcurrentHashMap<>();

    private StartupPerformanceProfiler() {}

    public static void setEnabled(boolean value) {
        enabled = value;
        if (value) {
            jvmStartNanos = System.nanoTime();
            log.info("[PERF] Startup performance profiling ENABLED");
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void markJvmStart() {
        jvmStartNanos = System.nanoTime();
    }

    public static void record(String operation, long durationNanos) {
        if (!enabled) return;
        timingsNanos.merge(operation, durationNanos, Long::sum);
        counters.merge(operation, 1, Integer::sum);
    }

    public static void time(String operation, Runnable action) {
        if (!enabled) {
            action.run();
            return;
        }
        long start = System.nanoTime();
        try {
            action.run();
        } finally {
            record(operation, System.nanoTime() - start);
        }
    }

    public static <T> T time(String operation, Supplier<T> action) {
        if (!enabled) {
            return action.get();
        }
        long start = System.nanoTime();
        try {
            return action.get();
        } finally {
            record(operation, System.nanoTime() - start);
        }
    }

    public static long elapsedSinceJvmStartMs() {
        return (System.nanoTime() - jvmStartNanos) / 1_000_000;
    }

    public static List<TimingEntry> topOperations(int limit) {
        List<TimingEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Long> e : timingsNanos.entrySet()) {
            int count = counters.getOrDefault(e.getKey(), 1);
            entries.add(new TimingEntry(e.getKey(), e.getValue(), count));
        }
        entries.sort(Comparator.comparingLong(TimingEntry::totalNanos).reversed());
        return entries.subList(0, Math.min(limit, entries.size()));
    }

    public static Map<String, Long> allTimingsMs() {
        Map<String, Long> result = new LinkedHashMap<>();
        topOperations(timingsNanos.size()).forEach(e ->
                result.put(e.operation(), e.totalMs()));
        return result;
    }

    public static void logSummary(String phase) {
        if (!enabled) return;
        log.info("[PERF] === {} (elapsed since JVM start: {} ms) ===",
                phase, elapsedSinceJvmStartMs());
        int rank = 1;
        for (TimingEntry entry : topOperations(10)) {
            log.info("[PERF]   #{} {} — {} ms total ({} calls, avg {} ms)",
                    rank++,
                    entry.operation(),
                    entry.totalMs(),
                    entry.count(),
                    entry.avgMs());
        }
    }

    public record TimingEntry(String operation, long totalNanos, int count) {
        public long totalMs() { return totalNanos / 1_000_000; }
        public long avgMs() { return count > 0 ? totalMs() / count : 0; }
    }
}
