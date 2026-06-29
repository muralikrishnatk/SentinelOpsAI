package com.sentinel.aiops.telemetry;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * In-memory rolling window of traffic, bucketed per second per service. Backs the
 * local SLI source so the platform computes real SLIs from live (changing) traffic
 * even without Prometheus. Bounded: buckets older than the retention are pruned.
 */
@Component
public class TrafficStore {

    /** Latency histogram upper edges (ms); last bucket is +Inf. */
    static final int[] EDGES = {50, 100, 150, 200, 300, 500, 750, 1000, 2000, Integer.MAX_VALUE};
    private static final long RETENTION_SECONDS = 60 * 60; // 1h

    private static final class Bucket {
        final LongAdder total = new LongAdder();
        final LongAdder errors = new LongAdder();
        final LongAdder[] latency = new LongAdder[EDGES.length];
        Bucket() { for (int i = 0; i < latency.length; i++) latency[i] = new LongAdder(); }
    }

    // service -> (epochSecond -> bucket)
    private final Map<String, ConcurrentSkipListMap<Long, Bucket>> store = new ConcurrentHashMap<>();

    public void record(String service, boolean ok, int latencyMs) {
        long sec = Instant.now().getEpochSecond();
        Bucket b = store.computeIfAbsent(service, s -> new ConcurrentSkipListMap<>())
                .computeIfAbsent(sec, s -> new Bucket());
        b.total.increment();
        if (!ok) b.errors.increment();
        b.latency[edgeIndex(latencyMs)].increment();
        prune(service);
    }

    public long total(String service, Duration window) {
        return agg(service, window, (acc, b) -> acc + b.total.sum());
    }

    public long errors(String service, Duration window) {
        return agg(service, window, (acc, b) -> acc + b.errors.sum());
    }

    /** Fraction of requests slower than thresholdMs (approx via histogram edges). */
    public double slowRatio(String service, Duration window, int thresholdMs) {
        long[] totalHolder = {0};
        long[] slowHolder = {0};
        forEach(service, window, b -> {
            for (int i = 0; i < EDGES.length; i++) {
                long c = b.latency[i].sum();
                totalHolder[0] += c;
                if (EDGES[i] > thresholdMs) slowHolder[0] += c;
            }
        });
        return totalHolder[0] == 0 ? 0.0 : (double) slowHolder[0] / totalHolder[0];
    }

    public double errorRatio(String service, Duration window) {
        long t = total(service, window);
        return t == 0 ? 0.0 : (double) errors(service, window) / t;
    }

    public double requestsPerSec(String service, Duration window) {
        long t = total(service, window);
        return t / (double) Math.max(1, window.getSeconds());
    }

    private interface Reducer { long apply(long acc, Bucket b); }

    private long agg(String service, Duration window, Reducer r) {
        long[] holder = {0};
        forEach(service, window, b -> holder[0] = r.apply(holder[0], b));
        return holder[0];
    }

    private void forEach(String service, Duration window, java.util.function.Consumer<Bucket> fn) {
        ConcurrentSkipListMap<Long, Bucket> m = store.get(service);
        if (m == null) return;
        long from = Instant.now().getEpochSecond() - window.getSeconds();
        m.tailMap(from, true).values().forEach(fn);
    }

    private void prune(String service) {
        ConcurrentSkipListMap<Long, Bucket> m = store.get(service);
        if (m == null) return;
        long cutoff = Instant.now().getEpochSecond() - RETENTION_SECONDS;
        m.headMap(cutoff, false).clear();
    }

    private int edgeIndex(int latencyMs) {
        for (int i = 0; i < EDGES.length; i++) if (latencyMs <= EDGES[i]) return i;
        return EDGES.length - 1;
    }
}
