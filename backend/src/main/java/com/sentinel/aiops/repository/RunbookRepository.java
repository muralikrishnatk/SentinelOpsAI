package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.Runbook;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RunbookRepository extends JpaRepository<Runbook, Long> {
    List<Runbook> findByService(String service);
}
