package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.Slo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SloRepository extends JpaRepository<Slo, Long> {
    List<Slo> findByEnabledTrue();
    List<Slo> findByService(String service);
}
