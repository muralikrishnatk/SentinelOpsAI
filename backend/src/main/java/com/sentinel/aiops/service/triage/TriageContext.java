package com.sentinel.aiops.service.triage;

import com.sentinel.aiops.domain.Incident;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/** Mutable context passed along the triage chain. */
@Getter
public class TriageContext {
    private final Incident incident;
    private final List<String> actions = new ArrayList<>();

    public TriageContext(Incident incident) { this.incident = incident; }

    public void log(String action) { actions.add(action); }
}
