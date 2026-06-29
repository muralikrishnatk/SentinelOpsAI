package com.sentinel.aiops.service.audit;

import com.sentinel.aiops.domain.AuditEvent;
import com.sentinel.aiops.dto.AuditDtos.AuditView;
import com.sentinel.aiops.repository.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) { this.repo = repo; }

    /** Persisted off the request thread so auditing never slows the API. */
    @Async
    public void record(AuditEvent event) { repo.save(event); }

    public Page<AuditView> list(int page, int size, String q) {
        var pageable = PageRequest.of(page, Math.min(size, 200), Sort.by(Sort.Direction.DESC, "at"));
        Page<AuditEvent> result = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByActorContainingIgnoreCaseOrActionContainingIgnoreCase(q, q, pageable);
        return result.map(AuditView::from);
    }
}
