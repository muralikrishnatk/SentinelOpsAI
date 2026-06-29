package com.sentinel.aiops.repository;

import com.sentinel.aiops.domain.OnCallShift;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OnCallShiftRepository extends JpaRepository<OnCallShift, Long> {

    // Current on-call: a shift for the team whose window contains 'now', most recent first.
    Optional<OnCallShift> findFirstByTeamNameAndStartAtBeforeAndEndAtAfterOrderByStartAtDesc(
            String teamName, Instant nowStart, Instant nowEnd);

    List<OnCallShift> findByTeamNameOrderByStartAtAsc(String teamName);
    List<OnCallShift> findByEndAtAfterOrderByStartAtAsc(Instant after);
}
