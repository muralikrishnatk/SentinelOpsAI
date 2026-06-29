package com.sentinel.aiops.controller;

import com.sentinel.aiops.dto.OnCallDtos.*;
import com.sentinel.aiops.service.oncall.OnCallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/oncall")
@Tag(name = "On-call", description = "On-call rotations and current responder")
public class OnCallController {

    private final OnCallService oncall;

    public OnCallController(OnCallService oncall) { this.oncall = oncall; }

    @GetMapping
    @Operation(summary = "On-call schedule (optionally filtered by team)")
    public List<ShiftView> schedule(@RequestParam(required = false) String team) { return oncall.schedule(team); }

    @GetMapping("/current")
    @Operation(summary = "Who is on call now for a team")
    public OnCallNow current(@RequestParam String team) { return oncall.currentForTeam(team); }

    @PostMapping
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    @Operation(summary = "Add an on-call shift")
    public ShiftView add(@Valid @RequestBody ShiftRequest req) { return oncall.addShift(req); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESPONDER','ADMIN')")
    public void delete(@PathVariable Long id) { oncall.deleteShift(id); }
}
