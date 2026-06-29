package com.sentinel.aiops.controller;

import com.sentinel.aiops.dto.SloDtos.*;
import com.sentinel.aiops.repository.AlertRecordRepository;
import com.sentinel.aiops.service.slo.SloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slos")
@Tag(name = "SLOs", description = "Service level objectives, error budgets, burn-rate alerts")
public class SloController {

    private final SloService slos;
    private final AlertRecordRepository alerts;

    public SloController(SloService slos, AlertRecordRepository alerts) {
        this.slos = slos;
        this.alerts = alerts;
    }

    @GetMapping
    @Operation(summary = "Live SLO status: SLI, budget headroom, burn rates, alert tier")
    public List<SloView> status() { return slos.status(); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Define an SLO (ADMIN)")
    public SloView create(@Valid @RequestBody SloRequest req) { return slos.create(req); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an SLO (ADMIN)")
    public void delete(@PathVariable Long id) { slos.delete(id); }

    @GetMapping("/alerts")
    @Operation(summary = "Recent burn-rate alerts (post-correlation)")
    public List<AlertView> recentAlerts() {
        return alerts.findTop50ByOrderByFiredAtDesc().stream()
                .map(a -> new AlertView(a.getId(), a.getService(), a.getSloName(), a.getTier().name(),
                        a.getBurnRateShort(), a.getBurnRateLong(), a.getIncidentId(), a.getFiredAt()))
                .toList();
    }
}
