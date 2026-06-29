package com.sentinel.aiops.service.triage;

import com.sentinel.aiops.domain.Incident;
import java.util.Locale;

/** Final link: route to an owning team based on the affected service. */
public class AutoAssignHandler extends AbstractTriageHandler {
    @Override
    protected void doHandle(TriageContext ctx) {
        Incident i = ctx.getIncident();
        if (i.getAssignee() != null) { ctx.log("Assignee preset: " + i.getAssignee()); return; }
        String svc = i.getAffectedService().toLowerCase(Locale.ROOT);
        String team;
        if (svc.contains("pay") || svc.contains("checkout") || svc.contains("billing")) team = "payments-oncall";
        else if (svc.contains("auth") || svc.contains("login") || svc.contains("identity")) team = "identity-oncall";
        else if (svc.contains("db") || svc.contains("data")) team = "data-platform-oncall";
        else team = "platform-oncall";
        i.setAssignee(team);
        ctx.log("Auto-assigned to " + team);
    }
}
