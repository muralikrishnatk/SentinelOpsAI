package com.sentinel.aiops.service;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.ServiceCatalogEntry;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import com.sentinel.aiops.domain.enums.Severity;
import com.sentinel.aiops.domain.enums.ServiceTier;
import com.sentinel.aiops.dto.ServiceDtos.*;
import com.sentinel.aiops.exception.NotFoundException;
import com.sentinel.aiops.repository.IncidentRepository;
import com.sentinel.aiops.repository.ServiceCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServiceCatalogService {

    private final ServiceCatalogRepository repo;
    private final IncidentRepository incidents;

    public ServiceCatalogService(ServiceCatalogRepository repo, IncidentRepository incidents) {
        this.repo = repo;
        this.incidents = incidents;
    }

    @Transactional
    public ServiceView create(ServiceRequest req) {
        if (repo.existsByNameIgnoreCase(req.name())) {
            throw new IllegalArgumentException("Service already exists: " + req.name());
        }
        ServiceCatalogEntry e = ServiceCatalogEntry.builder()
                .name(req.name())
                .description(req.description())
                .ownerTeam(req.ownerTeam())
                .tier(req.tier() == null ? ServiceTier.TIER2 : req.tier())
                .runbookUrl(req.runbookUrl())
                .build();
        return toView(repo.save(e));
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new NotFoundException("Service " + id + " not found");
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ServiceView> list() {
        return repo.findAll().stream().map(this::toView).toList();
    }

    private ServiceView toView(ServiceCatalogEntry e) {
        List<Incident> open = incidents.findByAffectedServiceIgnoreCase(e.getName()).stream()
                .filter(i -> i.getStatus() != IncidentStatus.RESOLVED
                        && i.getStatus() != IncidentStatus.CLOSED)
                .toList();
        String health = "HEALTHY";
        if (open.stream().anyMatch(i -> i.getSeverity() == Severity.SEV1)) health = "CRITICAL";
        else if (!open.isEmpty()) health = "DEGRADED";
        return new ServiceView(e.getId(), e.getName(), e.getDescription(), e.getOwnerTeam(),
                e.getTier(), e.getRunbookUrl(), health, open.size());
    }
}
