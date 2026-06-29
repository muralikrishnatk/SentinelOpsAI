package com.sentinel.aiops.controller;

import com.sentinel.aiops.dto.AiQueryRequest;
import com.sentinel.aiops.dto.AiQueryResponse;
import com.sentinel.aiops.dto.DashboardStats;
import com.sentinel.aiops.service.IncidentService;
import com.sentinel.aiops.service.ai.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "AI & Insights", description = "AI assistant and aggregated dashboard data")
@CrossOrigin
public class InsightsController {

    private final IncidentService service;
    private final AiAnalysisService ai;

    public InsightsController(IncidentService service, AiAnalysisService ai) {
        this.service = service;
        this.ai = ai;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Aggregated metrics for the dashboard")
    public DashboardStats dashboard() { return service.stats(); }

    @PostMapping("/ai/ask")
    @Operation(summary = "Ask the AI assistant a natural-language question about incidents")
    public AiQueryResponse ask(@Valid @RequestBody AiQueryRequest req) { return service.ask(req); }

    @GetMapping("/ai/status")
    @Operation(summary = "Which AI provider is active")
    public Map<String, Object> aiStatus() {
        return Map.of(
                "activeProvider", ai.activeProviderName(),
                "remote", ai.primaryIsRemote());
    }
}
