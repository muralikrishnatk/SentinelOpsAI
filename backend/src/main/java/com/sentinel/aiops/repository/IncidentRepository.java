package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import com.sentinel.aiops.domain.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pattern. Spring Data generates the implementation at runtime,
 * abstracting away all persistence details from the service layer.
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findBySeverity(Severity severity);
    List<Incident> findByAffectedServiceIgnoreCase(String affectedService);
    long countByStatus(IncidentStatus status);
    long countBySeverity(Severity severity);
}
