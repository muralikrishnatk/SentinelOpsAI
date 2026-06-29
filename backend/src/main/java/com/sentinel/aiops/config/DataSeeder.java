package com.sentinel.aiops.config;

import com.sentinel.aiops.domain.Organization;
import com.sentinel.aiops.domain.OnCallShift;
import com.sentinel.aiops.domain.Team;
import com.sentinel.aiops.domain.enums.*;
import com.sentinel.aiops.dto.AuthDtos.RegisterRequest;
import com.sentinel.aiops.dto.CreateIncidentRequest;
import com.sentinel.aiops.dto.RunbookDtos.RunbookRequest;
import com.sentinel.aiops.dto.RunbookDtos.StepReq;
import com.sentinel.aiops.dto.ServiceDtos.ServiceRequest;
import com.sentinel.aiops.dto.SloDtos.SloRequest;
import com.sentinel.aiops.repository.OnCallShiftRepository;
import com.sentinel.aiops.repository.OrganizationRepository;
import com.sentinel.aiops.repository.TeamRepository;
import com.sentinel.aiops.repository.UserRepository;
import com.sentinel.aiops.service.IncidentService;
import com.sentinel.aiops.service.ServiceCatalogService;
import com.sentinel.aiops.service.UserService;
import com.sentinel.aiops.service.remediation.RemediationService;
import com.sentinel.aiops.service.slo.SloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Seeds demo users, services, SLOs, sample incidents, and remediation runbooks. */
@Configuration
@Slf4j
public class DataSeeder {

    @Bean
    CommandLineRunner seed(UserService users, ServiceCatalogService services, SloService slos,
                           IncidentService incidents, RemediationService remediation,
                           UserRepository userRepo, OrganizationRepository orgRepo,
                           TeamRepository teamRepo, OnCallShiftRepository shiftRepo) {
        return args -> {
            // Idempotent: with a persistent database, only seed an empty schema.
            if (userRepo.count() > 0) {
                log.info("Seed skipped — existing data detected ({} users).", userRepo.count());
                return;
            }

            // ---- Organization & teams (platform tenancy) ----
            Organization org = orgRepo.save(Organization.builder()
                    .name("Acme Corp").slug("acme").plan("ENTERPRISE").build());
            teamRepo.save(Team.builder().name("payments-oncall").organizationId(org.getId())
                    .description("Payments & checkout").escalationContact("payments-lead").build());
            teamRepo.save(Team.builder().name("identity-oncall").organizationId(org.getId())
                    .description("Auth & identity").escalationContact("identity-lead").build());
            teamRepo.save(Team.builder().name("platform-oncall").organizationId(org.getId())
                    .description("Core platform").escalationContact("platform-lead").build());

            // ---- On-call: put someone on call now so routing works in the demo ----
            Instant now = Instant.now();
            shiftRepo.save(OnCallShift.builder().teamName("payments-oncall").username("responder")
                    .startAt(now.minus(1, ChronoUnit.HOURS)).endAt(now.plus(7, ChronoUnit.DAYS)).build());
            shiftRepo.save(OnCallShift.builder().teamName("platform-oncall").username("admin")
                    .startAt(now.minus(1, ChronoUnit.HOURS)).endAt(now.plus(7, ChronoUnit.DAYS)).build());

            // ---- Users ----
            users.register(new RegisterRequest("admin", "admin123", "Ada Admin",
                    "admin@sentinel.io", Role.ADMIN, "platform-oncall"), true);
            users.register(new RegisterRequest("responder", "responder123", "Rob Responder",
                    "rob@sentinel.io", Role.RESPONDER, "payments-oncall"), true);
            users.register(new RegisterRequest("viewer", "viewer123", "Vic Viewer",
                    "vic@sentinel.io", Role.VIEWER, null), true);

            // ---- Service catalog ----
            services.create(new ServiceRequest("checkout-api", "Customer checkout & cart",
                    "payments-oncall", ServiceTier.TIER1, "https://runbooks.sentinel.io/checkout"));
            services.create(new ServiceRequest("payment-gateway", "Payment processing",
                    "payments-oncall", ServiceTier.TIER1, "https://runbooks.sentinel.io/payments"));
            services.create(new ServiceRequest("identity-service", "Auth & login",
                    "identity-oncall", ServiceTier.TIER1, null));
            services.create(new ServiceRequest("data-platform", "Data warehouse & pipelines",
                    "data-platform-oncall", ServiceTier.TIER2, null));

            // ---- SLOs (evaluated live against real traffic metrics) ----
            slos.create(new SloRequest("checkout-api", "Checkout availability",
                    SliType.AVAILABILITY, 99.9, 30, null));
            slos.create(new SloRequest("checkout-api", "Checkout latency < 300ms",
                    SliType.LATENCY, 99.0, 30, 300));
            slos.create(new SloRequest("payment-gateway", "Payments availability",
                    SliType.AVAILABILITY, 99.95, 30, null));
            slos.create(new SloRequest("identity-service", "Login availability",
                    SliType.AVAILABILITY, 99.9, 30, null));
            slos.create(new SloRequest("data-platform", "Data platform availability",
                    SliType.AVAILABILITY, 99.5, 30, null));

            // ---- A couple of illustrative incidents (seeded BEFORE runbooks so they
            //      don't trigger remediation at startup) ----
            incidents.create(new CreateIncidentRequest(
                    "Elevated login latency",
                    "Login requests slow but succeeding.",
                    "identity-service", null,
                    "p99 latency 2.1s vs 200ms baseline; auth DB pool near limit"));
            incidents.create(new CreateIncidentRequest(
                    "Disk usage warning on data node",
                    "Data node disk at 78% and climbing.",
                    "data-platform", null,
                    "node data-3 disk 78%; WARN threshold 75%"));

            // ---- Remediation runbooks ----
            remediation.create(new RunbookRequest(
                    "Checkout auto-recover", "checkout-api", true, 3,
                    List.of(new StepReq(RunbookActionType.RUN_DIAGNOSTIC, "health probe"),
                            new StepReq(RunbookActionType.RESTART_SERVICE, "rolling"))));
            remediation.create(new RunbookRequest(
                    "Payments failover (approval required)", "payment-gateway", false, 2,
                    List.of(new StepReq(RunbookActionType.RUN_DIAGNOSTIC, "check primary"),
                            new StepReq(RunbookActionType.FAILOVER, "standby region"))));
            remediation.create(new RunbookRequest(
                    "Identity scale-out", "identity-service", true, 4,
                    List.of(new StepReq(RunbookActionType.SCALE_OUT, "replicas=6"))));

            log.info("Seed complete: org, teams, on-call, users, services, SLOs, incidents, runbooks.");
        };
    }
}
