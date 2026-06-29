package com.sentinel.aiops.service.slo;

import com.sentinel.aiops.domain.Slo;
import com.sentinel.aiops.domain.enums.SliType;
import com.sentinel.aiops.dto.SloDtos.*;
import com.sentinel.aiops.exception.NotFoundException;
import com.sentinel.aiops.repository.SloRepository;
import com.sentinel.aiops.telemetry.SliSourceProvider;
import com.sentinel.aiops.telemetry.TrafficGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class SloService {

    private static final Duration CURRENT_WINDOW = Duration.ofMinutes(5);

    private final SloRepository repo;
    private final BurnRateEvaluator evaluator;
    private final SliSourceProvider sli;
    private final TrafficGenerator traffic;

    public SloService(SloRepository repo, BurnRateEvaluator evaluator,
                      SliSourceProvider sli, TrafficGenerator traffic) {
        this.repo = repo;
        this.evaluator = evaluator;
        this.sli = sli;
        this.traffic = traffic;
    }

    @Transactional
    public SloView create(SloRequest req) {
        Slo slo = Slo.builder()
                .service(req.service())
                .name(req.name())
                .sliType(req.sliType() == null ? SliType.AVAILABILITY : req.sliType())
                .objectivePercent(req.objectivePercent())
                .windowDays(req.windowDays() == null ? 30 : req.windowDays())
                .latencyThresholdMs(req.latencyThresholdMs())
                .enabled(true)
                .build();
        return view(repo.save(slo));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new NotFoundException("SLO " + id + " not found");
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<SloView> status() {
        return repo.findAll().stream().map(this::view).toList();
    }

    public SloView view(Slo slo) {
        double currentBad = evaluator.badRatio(slo, CURRENT_WINDOW);
        double currentSli = (1.0 - currentBad) * 100.0;
        double headroom = clamp((1.0 - evaluator.headroomBurn(slo)) * 100.0, -100, 100);
        BurnRateEvaluator.Evaluation ev = evaluator.evaluate(slo);
        return new SloView(
                slo.getId(), slo.getService(), slo.getName(), slo.getSliType(),
                slo.getObjectivePercent(), slo.getWindowDays(), slo.getLatencyThresholdMs(),
                round(currentSli, 3), round(headroom, 1), ev.tier().name(),
                ev.windows().stream().map(w -> new BurnWindow(w.label(), round(w.burnRate(), 2), w.breaching())).toList(),
                sli.activeName(), traffic.hasFault(slo.getService()));
    }

    private double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private double round(double v, int dp) { double f = Math.pow(10, dp); return Math.round(v * f) / f; }
}
