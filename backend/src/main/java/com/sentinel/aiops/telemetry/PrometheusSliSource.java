package com.sentinel.aiops.telemetry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Reads SLIs from a real Prometheus server via its HTTP query API. Activated only
 * when {@code aiops.prometheus.url} is set. Availability uses PromQL over the demo
 * request counter; latency SLI reuses the in-process histogram (bucket layout is
 * environment-specific), a pragmatic, documented split.
 */
@Component
@ConditionalOnExpression("'${aiops.prometheus.url:}' != ''")
@Slf4j
public class PrometheusSliSource implements SliSource {

    private final RestClient client;
    private final String baseUrl;
    private final TrafficStore latencyFallback;

    public PrometheusSliSource(@Value("${aiops.prometheus.url:}") String url,
                               TrafficStore latencyFallback) {
        this.baseUrl = url;
        this.latencyFallback = latencyFallback;
        this.client = RestClient.builder().baseUrl(url).build();
        log.info("PrometheusSliSource active -> {}", url);
    }

    @Override public String name() { return "prometheus(" + baseUrl + ")"; }
    @Override public boolean isRemote() { return true; }

    /** Reachability probe used by the selector. */
    public boolean healthy() {
        try {
            instantQuery("vector(1)");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public double errorRatio(String service, Duration window) {
        long s = Math.max(60, window.getSeconds());
        String errors = String.format(
                "sum(increase(aiops_demo_requests_total{service=\"%s\",outcome=\"error\"}[%ds]))", service, s);
        String total = String.format(
                "sum(increase(aiops_demo_requests_total{service=\"%s\"}[%ds]))", service, s);
        double e = instantQuery(errors);
        double t = instantQuery(total);
        return t <= 0 ? 0.0 : e / t;
    }

    @Override
    public double slowRatio(String service, Duration window, int thresholdMs) {
        // Latency histogram bucket layout is environment-specific; use the
        // in-process histogram for the latency SLI.
        return latencyFallback.slowRatio(service, window, thresholdMs);
    }

    @SuppressWarnings("unchecked")
    private double instantQuery(String promql) {
        Map<String, Object> resp = client.get()
                .uri(uri -> uri.path("/api/v1/query").queryParam("query", promql).build())
                .retrieve()
                .body(Map.class);
        if (resp == null) return 0.0;
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        if (data == null) return 0.0;
        List<Map<String, Object>> result = (List<Map<String, Object>>) data.get("result");
        if (result == null || result.isEmpty()) return 0.0;
        List<Object> value = (List<Object>) result.get(0).get("value");
        if (value == null || value.size() < 2) return 0.0;
        try {
            return Double.parseDouble(String.valueOf(value.get(1)));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
