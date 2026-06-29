package com.sentinel.aiops.controller;

import com.sentinel.aiops.dto.*;
import com.sentinel.aiops.dto.TimelineDtos.CommentRequest;
import com.sentinel.aiops.dto.TimelineDtos.TimelineView;
import com.sentinel.aiops.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Incident API. Reads are open to any authenticated user; mutating operations
 * require RESPONDER or ADMIN (enforced by {@code @PreAuthorize}).
 */
@RestController
@RequestMapping("/api/incidents")
@Tag(name = "Incidents", description = "Create, query, transition, comment, postmortem")
public class IncidentController {

    private final IncidentService service;

    public IncidentController(IncidentService service) { this.service = service; }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    @Operation(summary = "Report a new incident (runs AI triage chain)")
    public ResponseEntity<IncidentResponse> create(@Valid @RequestBody CreateIncidentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    @Operation(summary = "List all incidents")
    public List<IncidentResponse> list() { return service.list(); }

    @GetMapping("/{id}")
    @Operation(summary = "Get one incident")
    public IncidentResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    @Operation(summary = "Transition incident status (validated by State machine)")
    public IncidentResponse transition(@PathVariable Long id,
                                       @Valid @RequestBody StatusTransitionRequest req) {
        return service.transition(id, req);
    }

    @PostMapping("/{id}/reanalyze")
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    @Operation(summary = "Re-run AI summary + root-cause analysis")
    public IncidentResponse reanalyze(@PathVariable Long id) { return service.reanalyze(id); }

    public record AssignRequest(@jakarta.validation.constraints.NotBlank String assignee) {}

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    @Operation(summary = "Assign or reassign an incident to a user or on-call team")
    public IncidentResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest req) {
        return service.assign(id, req.assignee());
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Activity timeline for an incident")
    public List<TimelineView> timeline(@PathVariable Long id) { return service.timeline(id); }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    @Operation(summary = "Add a comment to the incident timeline")
    public TimelineView comment(@PathVariable Long id, @Valid @RequestBody CommentRequest req) {
        return service.comment(id, req.message());
    }

    @PostMapping("/{id}/postmortem")
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    @Operation(summary = "Generate an AI postmortem document")
    public Map<String, String> generatePostmortem(@PathVariable Long id) {
        return service.generatePostmortem(id);
    }

    @GetMapping("/{id}/postmortem")
    @Operation(summary = "Fetch the stored postmortem (if any)")
    public Map<String, String> getPostmortem(@PathVariable Long id) {
        return service.getPostmortem(id);
    }
}
