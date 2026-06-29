package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.AlertRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertRecordRepository extends JpaRepository<AlertRecord, Long> {
    List<AlertRecord> findByDedupKeyAndFiredAtAfter(String dedupKey, Instant after);
    List<AlertRecord> findByServiceAndIncidentIdIsNullAndFiredAtAfter(String service, Instant after);
    Optional<AlertRecord> findFirstByServiceAndIncidentIdIsNotNullAndFiredAtAfterOrderByFiredAtDesc(
            String service, Instant after);
    List<AlertRecord> findTop50ByOrderByFiredAtDesc();
}
