package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import com.sentinel.aiops.domain.enums.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Paged, filtered search for the incident list. All filters are optional (null = ignore).
     * Server-side paging is essential at scale — never return every incident to every client.
     */
    @Query("""
            SELECT i FROM Incident i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:severity IS NULL OR i.severity = :severity)
              AND (LOWER(i.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(i.affectedService) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Incident> search(@Param("status") IncidentStatus status,
                          @Param("severity") Severity severity,
                          @Param("q") String q,
                          Pageable pageable);
}