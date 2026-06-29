package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.ServiceCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalogEntry, Long> {
    Optional<ServiceCatalogEntry> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}
