package com.sentinel.aiops.controller;

import com.sentinel.aiops.integration.health.IntegrationRegistry;
import com.sentinel.aiops.integration.health.IntegrationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/integrations")
@Tag(name = "Integrations", description = "Health of external connectors (telemetry, notify, remediation)")
public class IntegrationController {

    private final IntegrationRegistry registry;

    public IntegrationController(IntegrationRegistry registry) { this.registry = registry; }

    @GetMapping
    @Operation(summary = "Status of every integration: live vs simulated")
    public List<IntegrationStatus> all() { return registry.all(); }
}
