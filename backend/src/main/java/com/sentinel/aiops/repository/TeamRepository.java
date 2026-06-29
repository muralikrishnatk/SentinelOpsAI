package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByOrganizationId(Long organizationId);
    boolean existsByName(String name);
}
