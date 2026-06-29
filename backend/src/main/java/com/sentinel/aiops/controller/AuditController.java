package com.sentinel.aiops.controller;

import com.sentinel.aiops.dto.AuditDtos.AuditView;
import com.sentinel.aiops.service.audit.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit", description = "Immutable audit trail (ADMIN)")
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) { this.audit = audit; }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Paged audit log; optional q matches actor or action")
    public Page<AuditView> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String q) {
        return audit.list(page, size, q);
    }
}
