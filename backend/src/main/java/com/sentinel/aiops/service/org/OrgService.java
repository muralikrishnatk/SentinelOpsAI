package com.sentinel.aiops.service.org;

import com.sentinel.aiops.domain.Organization;
import com.sentinel.aiops.domain.Team;
import com.sentinel.aiops.dto.OrgDtos.*;
import com.sentinel.aiops.exception.NotFoundException;
import com.sentinel.aiops.repository.OrganizationRepository;
import com.sentinel.aiops.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrgService {

    private final OrganizationRepository orgs;
    private final TeamRepository teams;

    public OrgService(OrganizationRepository orgs, TeamRepository teams) {
        this.orgs = orgs;
        this.teams = teams;
    }

    @Transactional(readOnly = true)
    public List<OrgView> listOrgs() { return orgs.findAll().stream().map(OrgView::from).toList(); }

    @Transactional
    public OrgView createOrg(OrgRequest req) {
        if (orgs.existsBySlug(req.slug()))
            throw new IllegalArgumentException("Organization slug already exists: " + req.slug());
        Organization o = orgs.save(Organization.builder()
                .name(req.name()).slug(req.slug()).plan(req.plan() == null ? "ENTERPRISE" : req.plan()).build());
        return OrgView.from(o);
    }

    @Transactional(readOnly = true)
    public List<TeamView> listTeams(Long orgId) {
        List<Team> result = orgId == null ? teams.findAll() : teams.findByOrganizationId(orgId);
        return result.stream().map(TeamView::from).toList();
    }

    @Transactional
    public TeamView createTeam(TeamRequest req) {
        Team t = teams.save(Team.builder()
                .name(req.name()).organizationId(req.organizationId())
                .description(req.description()).escalationContact(req.escalationContact()).build());
        return TeamView.from(t);
    }

    @Transactional
    public void deleteTeam(Long id) {
        if (!teams.existsById(id)) throw new NotFoundException("Team " + id + " not found");
        teams.deleteById(id);
    }
}
