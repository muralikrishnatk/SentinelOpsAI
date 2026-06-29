package com.sentinel.aiops.service.slo;

import com.sentinel.aiops.domain.Slo;
import com.sentinel.aiops.domain.enums.BurnTier;
import com.sentinel.aiops.domain.enums.SliType;
import com.sentinel.aiops.dto.SloDtos.BurnWindow;
import com.sentinel.aiops.telemetry.SliSourceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Multi-window, multi-burn-rate evaluation per the Google SRE Workbook.
 *
 * <p>For each tier we require BOTH a long and a short window to exceed the burn-rate
 * threshold. The long window proves the burn is sustained (fewer false positives);
 * the short window ensures we stop alerting quickly once it recovers. A "burn rate"
 * of N means we are consuming the error budget N× faster than sustainable.</p>
 *
 * <p>Windows are configurable; defaults are demo-friendly (so alerts fire in minutes)
 * but the production-canonical values for a 30-day budget are documented in the README.</p>
 */
@Component
public class BurnRateEvaluator {

    private final SliSourceProvider sli;

    private final Duration fastLong, fastShort, slowLong, slowShort, ticketLong, ticketShort;
    private final double fastRate, slowRate, ticketRate;

    public BurnRateEvaluator(
            SliSourceProvider sli,
            @Value("${aiops.slo.fast.long-window:10m}") String fastLong,
            @Value("${aiops.slo.fast.short-window:2m}") String fastShort,
            @Value("${aiops.slo.fast.burn-rate:14.4}") double fastRate,
            @Value("${aiops.slo.slow.long-window:1h}") String slowLong,
            @Value("${aiops.slo.slow.short-window:10m}") String slowShort,
            @Value("${aiops.slo.slow.burn-rate:6}") double slowRate,
            @Value("${aiops.slo.ticket.long-window:6h}") String ticketLong,
            @Value("${aiops.slo.ticket.short-window:30m}") String ticketShort,
            @Value("${aiops.slo.ticket.burn-rate:3}") double ticketRate) {
        this.sli = sli;
        this.fastLong = DurationStyle.detectAndParse(fastLong);
        this.fastShort = DurationStyle.detectAndParse(fastShort);
        this.fastRate = fastRate;
        this.slowLong = DurationStyle.detectAndParse(slowLong);
        this.slowShort = DurationStyle.detectAndParse(slowShort);
        this.slowRate = slowRate;
        this.ticketLong = DurationStyle.detectAndParse(ticketLong);
        this.ticketShort = DurationStyle.detectAndParse(ticketShort);
        this.ticketRate = ticketRate;
    }

    /** Observed "bad" ratio for the SLO (errors for availability, slow for latency). */
    public double badRatio(Slo slo, Duration window) {
        if (slo.getSliType() == SliType.LATENCY) {
            int t = slo.getLatencyThresholdMs() == null ? 300 : slo.getLatencyThresholdMs();
            return sli.slowRatio(slo.getService(), window, t);
        }
        return sli.errorRatio(slo.getService(), window);
    }

    public double allowedErrorRatio(Slo slo) {
        return Math.max(1e-9, 1.0 - slo.getObjectivePercent() / 100.0);
    }

    public double burnRate(Slo slo, Duration window) {
        return badRatio(slo, window) / allowedErrorRatio(slo);
    }

    public record Evaluation(BurnTier tier, double burnShort, double burnLong, List<BurnWindow> windows) {}

    public Evaluation evaluate(Slo slo) {
        double fastL = burnRate(slo, fastLong), fastS = burnRate(slo, fastShort);
        double slowL = burnRate(slo, slowLong), slowS = burnRate(slo, slowShort);
        double tickL = burnRate(slo, ticketLong), tickS = burnRate(slo, ticketShort);

        List<BurnWindow> windows = List.of(
                new BurnWindow(human(fastShort), fastS, fastS >= fastRate),
                new BurnWindow(human(fastLong), fastL, fastL >= fastRate),
                new BurnWindow(human(slowLong), slowL, slowL >= slowRate),
                new BurnWindow(human(ticketLong), tickL, tickL >= ticketRate));

        // Highest-urgency tier wins; both windows of a tier must breach.
        if (fastS >= fastRate && fastL >= fastRate)
            return new Evaluation(BurnTier.FAST, fastS, fastL, windows);
        if (slowS >= slowRate && slowL >= slowRate)
            return new Evaluation(BurnTier.SLOW, slowS, slowL, windows);
        if (tickS >= ticketRate && tickL >= ticketRate)
            return new Evaluation(BurnTier.TICKET, tickS, tickL, windows);
        return new Evaluation(BurnTier.NONE, fastS, fastL, windows);
    }

    /** Long-window burn rate used for the budget-headroom gauge. */
    public double headroomBurn(Slo slo) { return burnRate(slo, slowLong); }

    private String human(Duration d) {
        long m = d.toMinutes();
        return m >= 60 ? (m / 60) + "h" : m + "m";
    }
}
