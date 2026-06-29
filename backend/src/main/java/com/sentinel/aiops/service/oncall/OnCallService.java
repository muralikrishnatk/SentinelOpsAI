package com.sentinel.aiops.service.oncall;

import com.sentinel.aiops.domain.OnCallShift;
import com.sentinel.aiops.dto.OnCallDtos.*;
import com.sentinel.aiops.exception.NotFoundException;
import com.sentinel.aiops.repository.OnCallShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * On-call rotations. {@link #whoIsOnCall(String)} resolves the responder currently
 * covering a team, which lets incident auto-assignment route to a <i>person</i> on
 * rotation instead of a static team label.
 */
@Service
public class OnCallService {

    private final OnCallShiftRepository shifts;

    public OnCallService(OnCallShiftRepository shifts) { this.shifts = shifts; }

    @Transactional(readOnly = true)
    public Optional<String> whoIsOnCall(String teamName) {
        Instant now = Instant.now();
        return shifts.findFirstByTeamNameAndStartAtBeforeAndEndAtAfterOrderByStartAtDesc(teamName, now, now)
                .map(OnCallShift::getUsername);
    }

    @Transactional(readOnly = true)
    public OnCallNow currentForTeam(String teamName) {
        return whoIsOnCall(teamName)
                .map(u -> new OnCallNow(teamName, u, true))
                .orElse(new OnCallNow(teamName, null, false));
    }

    @Transactional(readOnly = true)
    public List<ShiftView> schedule(String teamName) {
        var list = (teamName == null)
                ? shifts.findByEndAtAfterOrderByStartAtAsc(Instant.now())
                : shifts.findByTeamNameOrderByStartAtAsc(teamName);
        return list.stream().map(ShiftView::from).toList();
    }

    @Transactional
    public ShiftView addShift(ShiftRequest req) {
        if (req.endAt().isBefore(req.startAt()))
            throw new IllegalArgumentException("Shift endAt must be after startAt");
        OnCallShift s = shifts.save(OnCallShift.builder()
                .teamName(req.teamName()).username(req.username())
                .startAt(req.startAt()).endAt(req.endAt()).build());
        return ShiftView.from(s);
    }

    @Transactional
    public void deleteShift(Long id) {
        if (!shifts.existsById(id)) throw new NotFoundException("Shift " + id + " not found");
        shifts.deleteById(id);
    }
}
