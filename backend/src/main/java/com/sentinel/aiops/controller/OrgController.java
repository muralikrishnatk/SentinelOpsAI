package com.sentinel.aiops.controller;

import com.sentinel.aiops.dto.OrgDtos.*;
import com.sentinel.aiops.service.org.OrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Organizations & Teams", description = "Tenants and team structure")
public class OrgController {

    private final OrgService orgs;

    public OrgController(OrgService orgs) { this.orgs = orgs; }

    @GetMapping("/orgs")
    public List<OrgView> orgs() { return orgs.listOrgs(); }

    @PostMapping("/orgs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an organization (ADMIN)")
    public OrgView createOrg(@Valid @RequestBody OrgRequest req) { return orgs.createOrg(req); }

    @GetMapping("/teams")
    public List<TeamView> teams(@RequestParam(required = false) Long orgId) { return orgs.listTeams(orgId); }

    @PostMapping("/teams")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a team (ADMIN)")
    public TeamView createTeam(@Valid @RequestBody TeamRequest req) { return orgs.createTeam(req); }

    @DeleteMapping("/teams/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTeam(@PathVariable Long id) { orgs.deleteTeam(id); }
}
