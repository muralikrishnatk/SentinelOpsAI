package com.sentinel.aiops.service;

import com.sentinel.aiops.domain.Incident;
import com.sentinel.aiops.domain.TimelineEvent;
import com.sentinel.aiops.domain.enums.IncidentStatus;
import com.sentinel.aiops.domain.enums.TimelineEventType;
import com.sentinel.aiops.dto.*;
import com.sentinel.aiops.dto.TimelineDtos.TimelineView;
import com.sentinel.aiops.exception.InvalidTransitionException;
import com.sentinel.aiops.exception.NotFoundException;
import com.sentinel.aiops.repository.IncidentRepository;
import com.sentinel.aiops.service.ai.AiAnalysisService;
import com.sentinel.aiops.service.command.CommandInvoker;
import com.sentinel.aiops.service.command.IncidentCommand;
import com.sentinel.aiops.service.event.IncidentCreatedEvent;
import com.sentinel.aiops.service.event.IncidentStatusChangedEvent;
import com.sentinel.aiops.service.realtime.SseService;
import com.sentinel.aiops.service.sla.SlaService;
import com.sentinel.aiops.service.state.IncidentState;
import com.sentinel.aiops.service.state.IncidentStateFactory;
import com.sentinel.aiops.service.timeline.TimelineService;
import com.sentinel.aiops.service.triage.TriagePipeline;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service coordinating the whole feature set: AI triage, the state
 * machine, the activity timeline, SLA stamping, live SSE updates, and domain
 * events. Mutating operations run through the Command invoker.
 */
@Service
@Slf4j
public class IncidentService {

    private final IncidentRepository repo;
    private final TriagePipeline triage;
    private final IncidentStateFactory stateFactory;
    private final AiAnalysisService ai;
    private final CommandInvoker invoker;
    private final ApplicationEventPublisher events;
    private final TimelineService timeline;
    private final SseService sse;
    private final SlaService sla;

    public IncidentService(IncidentRepository repo, TriagePipeline triage,
                           IncidentStateFactory stateFactory, AiAnalysisService ai,
                           CommandInvoker invoker, ApplicationEventPublisher events,
                           TimelineService timeline, SseService sse, SlaService sla) {
        this.repo = repo;
        this.triage = triage;
        this.stateFactory = stateFactory;
        this.ai = ai;
        this.invoker = invoker;
        this.events = events;
        this.timeline = timeline;
        this.sse = sse;
        this.sla = sla;
    }

    @Transactional
    public IncidentResponse create(CreateIncidentRequest req) {
        return doCreate(req, currentUser(), TimelineEventType.CREATED, "Incident reported");
    }

    /** Used by the alert webhook — reporter is the monitoring system. */
    @Transactional
    public IncidentResponse createFromAlert(CreateIncidentRequest req) {
        return doCreate(req, "alertmanager", TimelineEventType.ALERT_INGESTED,
                "Auto-created from monitoring alert");
    }

    private IncidentResponse doCreate(CreateIncidentRequest req, String reporter,
                                      TimelineEventType firstEvent, String firstMsg) {
        Incident saved = invoker.run(new IncidentCommand<>() {
            @Override public Incident execute() {
                Incident incident = Incident.builder()      // Builder
                        .title(req.title())
                        .description(req.description())
                        .affectedService(req.affectedService())
                        .severity(req.severity())
                        .rawSignal(req.rawSignal())
                        .reporter(reporter)
                        .status(IncidentStatus.OPEN)
                        .build();

                triage.triage(incident);                    // Chain of Responsibility (AI)
                sla.applyTargets(incident);                 // SLA stamping
                Incident persisted = repo.save(incident);

                timeline.record(persisted.getId(), firstEvent, reporter, firstMsg);
                if (persisted.getAiSummary() != null) {
                    timeline.record(persisted.getId(), TimelineEventType.AI_ANALYSIS, "ai",
                            "AI triage: severity " + persisted.getSeverity()
                                    + ", auto-assigned " + persisted.getAssignee());
                }
                events.publishEvent(new IncidentCreatedEvent(persisted));  // Observer
                return persisted;
            }
            @Override public String describe() { return "CreateIncident(title=" + req.title() + ")"; }
        });
        sse.broadcast("incident.created", IncidentResponse.from(saved));
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse transition(Long id, StatusTransitionRequest req) {
        IncidentResponse result = invoker.run(new IncidentCommand<>() {
            @Override public IncidentResponse execute() {
                Incident incident = repo.findById(id)
                        .orElseThrow(() -> new NotFoundException("Incident " + id + " not found"));
                IncidentStatus from = incident.getStatus();
                IncidentStatus to = req.targetStatus();

                IncidentState current = stateFactory.forStatus(from);   // State pattern
                if (!current.canTransitionTo(to)) {
                    throw new InvalidTransitionException("Cannot move incident from " + from + " to " + to);
                }
                incident.setStatus(to);
                if (to == IncidentStatus.ACKNOWLEDGED && incident.getAcknowledgedAt() == null) {
                    incident.setAcknowledgedAt(Instant.now());
                }
                if (req.assignee() != null) incident.setAssignee(req.assignee());
                stateFactory.forStatus(to).onEnter(incident);

                Incident saved = repo.save(incident);
                String actor = currentUser();
                timeline.record(saved.getId(), TimelineEventType.STATUS_CHANGED, actor,
                        from + " → " + to + (req.note() != null ? " (" + req.note() + ")" : ""));
                events.publishEvent(new IncidentStatusChangedEvent(saved, from, to));
                return IncidentResponse.from(saved);
            }
            @Override public String describe() { return "Transition(id=" + id + " -> " + req.targetStatus() + ")"; }
        });
        sse.broadcast("incident.updated", result);
        return result;
    }

    @Transactional
    public TimelineView comment(Long id, String message) {
        repo.findById(id).orElseThrow(() -> new NotFoundException("Incident " + id + " not found"));
        TimelineEvent e = timeline.record(id, TimelineEventType.COMMENT, currentUser(), message);
        sse.broadcast("incident.comment", Map.of("incidentId", id));
        return TimelineView.from(e);
    }

    @Transactional(readOnly = true)
    public List<TimelineView> timeline(Long id) {
        return timeline.forIncident(id).stream().map(TimelineView::from).toList();
    }

    @Transactional
    public Map<String, String> generatePostmortem(Long id) {
        Incident incident = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident " + id + " not found"));
        List<String> tl = timeline.forIncident(id).stream()
                .map(e -> e.getCreatedAt() + " [" + e.getType() + "] " + e.getActor() + ": " + e.getMessage())
                .toList();
        String doc = ai.generatePostmortem(incident, tl);
        incident.setPostmortem(doc);
        incident.touch();
        repo.save(incident);
        timeline.record(id, TimelineEventType.POSTMORTEM, "ai", "Postmortem generated");
        return Map.of("postmortem", doc);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getPostmortem(Long id) {
        Incident incident = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident " + id + " not found"));
        return Map.of("postmortem", incident.getPostmortem() == null ? "" : incident.getPostmortem());
    }

    @Transactional(readOnly = true)
    public IncidentResponse get(Long id) {
        return repo.findById(id).map(IncidentResponse::from)
                .orElseThrow(() -> new NotFoundException("Incident " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> list() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(Incident::getCreatedAt).reversed())
                .map(IncidentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        List<Incident> all = repo.findAll();
        long total = all.size();
        long open = all.stream().filter(i -> i.getResolvedAt() == null).count();
        long resolved = total - open;
        Map<String, Long> bySeverity = all.stream()
                .collect(Collectors.groupingBy(i -> i.getSeverity().name(), Collectors.counting()));
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(i -> i.getStatus().name(), Collectors.counting()));
        double mttr = all.stream()
                .filter(i -> i.getResolvedAt() != null)
                .mapToDouble(i -> Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMinutes())
                .average().orElse(0.0);
        return new DashboardStats(total, open, resolved, bySeverity, byStatus,
                Math.round(mttr * 10.0) / 10.0);
    }

    @Transactional(readOnly = true)
    public AiQueryResponse ask(AiQueryRequest req) {
        List<Incident> context = repo.findAll();
        String answer = ai.answer(req.question(), context);
        boolean degraded = ai.primaryIsRemote() && !ai.activeProviderName().contains("anthropic");
        return new AiQueryResponse(answer, ai.activeProviderName(), degraded);
    }

    @Transactional
    public IncidentResponse reanalyze(Long id) {
        Incident incident = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident " + id + " not found"));
        incident.setAiSummary(ai.summarize(incident));
        incident.setAiRootCause(ai.suggestRootCause(incident));
        incident.touch();
        Incident saved = repo.save(incident);
        timeline.record(id, TimelineEventType.AI_ANALYSIS, currentUser(), "Re-ran AI analysis");
        sse.broadcast("incident.updated", IncidentResponse.from(saved));
        return IncidentResponse.from(saved);
    }

    @Transactional
    public IncidentResponse assign(Long id, String assignee) {
        Incident incident = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Incident " + id + " not found"));
        incident.setAssignee(assignee);
        incident.touch();
        Incident saved = repo.save(incident);
        timeline.record(id, TimelineEventType.ASSIGNED, currentUser(), "Assigned to " + assignee);
        sse.broadcast("incident.updated", IncidentResponse.from(saved));
        return IncidentResponse.from(saved);
    }

    private String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth == null || auth.getName() == null) ? "system" : auth.getName();
    }
}
