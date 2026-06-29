package com.sentinel.aiops.controller;

import com.sentinel.aiops.dto.RunbookDtos.*;
import com.sentinel.aiops.service.remediation.RemediationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Runbooks & Remediation", description = "Automated, guarded remediation")
public class RunbookController {

    private final RemediationService remediation;

    public RunbookController(RemediationService remediation) { this.remediation = remediation; }

    @GetMapping("/runbooks")
    @Operation(summary = "List runbooks")
    public List<RunbookView> runbooks() { return remediation.list(); }

    @PostMapping("/runbooks")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a runbook (ADMIN)")
    public RunbookView create(@Valid @RequestBody RunbookRequest req) { return remediation.create(req); }

    @GetMapping("/remediations")
    @Operation(summary = "Recent remediation executions")
    public List<ExecutionView> executions() { return remediation.recentExecutions(); }

    @PostMapping("/remediations/{id}/approve")
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    @Operation(summary = "Approve and run a queued remediation")
    public ExecutionView approve(@PathVariable Long id, Authentication auth) {
        return remediation.approve(id, auth.getName());
    }
}
