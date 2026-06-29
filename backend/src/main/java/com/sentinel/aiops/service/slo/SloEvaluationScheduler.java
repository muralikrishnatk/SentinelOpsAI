package com.sentinel.aiops.service.slo;

import com.sentinel.aiops.domain.Slo;
import com.sentinel.aiops.repository.SloRepository;
import com.sentinel.aiops.service.correlation.CorrelationService;
import com.sentinel.aiops.service.realtime.SseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Periodically evaluates every enabled SLO's burn rate. When a tier breaches, it
 * routes the alert through correlation (which dedups/groups and opens incidents).
 * This is the engine that turns live telemetry into action automatically.
 */
@Component
@Slf4j
public class SloEvaluationScheduler {

    private final SloRepository slos;
    private final BurnRateEvaluator evaluator;
    private final CorrelationService correlation;
    private final SseService sse;

    public SloEvaluationScheduler(SloRepository slos, BurnRateEvaluator evaluator,
                                  CorrelationService correlation, SseService sse) {
        this.slos = slos;
        this.evaluator = evaluator;
        this.correlation = correlation;
        this.sse = sse;
    }

    @Scheduled(fixedDelay = 15_000)
    public void evaluateAll() {
        boolean anyBreach = false;
        for (Slo slo : slos.findByEnabledTrue()) {
            BurnRateEvaluator.Evaluation ev = evaluator.evaluate(slo);
            if (ev.tier() != com.sentinel.aiops.domain.enums.BurnTier.NONE) {
                anyBreach = true;
                correlation.handleBurnAlert(slo, ev.tier(), ev.burnShort(), ev.burnLong());
            }
        }
        sse.broadcast("slo.updated", Map.of("evaluatedAt", System.currentTimeMillis(), "breach", anyBreach));
    }
}
