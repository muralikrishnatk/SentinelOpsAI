package com.sentinel.aiops.dto;

import com.sentinel.aiops.domain.Organization;
import com.sentinel.aiops.domain.Team;
import jakarta.validation.constraints.NotBlank;

public class OrgDtos {
    public record OrgRequest(@NotBlank String name, @NotBlank String slug, String plan) {}
    public record OrgView(Long id, String name, String slug, String plan) {
        public static OrgView from(Organization o) { return new OrgView(o.getId(), o.getName(), o.getSlug(), o.getPlan()); }
    }
    public record TeamRequest(@NotBlank String name, Long organizationId, String description, String escalationContact) {}
    public record TeamView(Long id, String name, Long organizationId, String description, String escalationContact) {
        public static TeamView from(Team t) {
            return new TeamView(t.getId(), t.getName(), t.getOrganizationId(), t.getDescription(), t.getEscalationContact());
        }
    }
}
