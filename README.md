# Sentinel AIOps — AI-assisted Reliability Platform

Sentinel is a closed-loop reliability platform, not just an incident tracker. It watches
**live telemetry**, computes **SLO error-budget burn** from it, fires **multi-window
multi-burn-rate alerts**, **correlates** an alert storm into a single incident, and runs
**guarded auto-remediation runbooks** that actually clear the fault — so the metrics and the
error budget recover. The whole loop is observable end-to-end.

> The thing to look at as a senior SRE: the SLO engine, the burn-rate alerting math, the
> correlation/dedup, and the closed remediation loop — all driven by real metrics, with a
> Prometheus path and an in-process fallback.

---

## The closed loop (the demo that matters)

```
inject fault --> live metrics spike --> SLO engine computes budget burn
     ^                                          |
     |                                          v
metrics recover <-- runbook remediates <-- alert correlated into ONE incident
```

1. **Inject a fault** on a service (UI button on the Reliability tab, or `POST /api/demo/fault`).
   The synthetic traffic generator raises that service's real error/latency metrics.
2. The **SLO engine** (every 15s) recomputes burn rate over multiple windows. When a tier
   breaches (both its windows exceed the threshold), it fires a burn-rate alert.
3. **Correlation** deduplicates repeat alerts and groups related ones, opening **one** incident
   (SEV mapped from burn tier) instead of paging N times.
4. A matching **runbook** auto-executes (or queues for approval). Mitigating steps call back into
   the telemetry layer to **clear the fault** — closing the loop.
5. **Budget headroom recovers** on the dashboard within a minute or two.

---

## Standout capabilities

**1. SLO + error-budget engine with multi-window multi-burn-rate alerting.**
Implements the Google SRE Workbook approach: each alert tier requires *both* a long and a short
window to exceed its burn-rate threshold (long window = sustained burn -> fewer false positives;
short window = fast reset on recovery). Tiers: FAST (page now), SLOW (page), TICKET (low urgency).

**2. Alert correlation & deduplication.** Identical alerts within a dedup window are dropped;
surviving alerts for a service are correlated into a single open incident, with the rest grouped
in. This is the alert-fatigue fix.

**3. Closed-loop auto-remediation runbooks.** Runbooks bind to a service and run ordered actions
(diagnostic, restart, scale-out, failover, rollback) through the Command pattern, with guardrails:
optional human approval and a per-hour rate limit. Mitigating actions clear the underlying fault.

**4. Real telemetry via Prometheus.** `docker compose up` runs Prometheus, which scrapes the
backend's Micrometer endpoint; the backend then queries Prometheus' HTTP API (PromQL) to compute
SLIs. If Prometheus is unset/unreachable, an in-process SLI source is used automatically — the SLO
engine is identical either way (Strategy pattern, mirroring the AI provider's online/offline split).

---

## Run it

**Docker (full stack incl. Prometheus):**
```bash
docker compose up --build
# Frontend   http://localhost:3000
# Backend    http://localhost:8080  (Swagger: /swagger-ui.html)
# Prometheus http://localhost:9090
```

**Local dev:**
```bash
cd backend && mvn spring-boot:run         # :8080  (in-memory SLI source unless PROMETHEUS_URL set)
cd frontend && npm install && npm run dev  # :5173
```

Optional real AI postmortems/triage: set `ANTHROPIC_API_KEY` (otherwise an offline heuristic
provider is used). Point the SLO engine at Prometheus locally with `PROMETHEUS_URL=http://localhost:9090`.

**Demo logins:** `admin/admin123` (ADMIN), `responder/responder123` (RESPONDER), `viewer/viewer123` (VIEWER).

### Try the loop
1. Log in as `responder`, open **Reliability**.
2. Click **Inject fault** on *checkout-api*. Watch the FAST window burn rate climb and headroom drop.
3. A SEV1 incident opens automatically (Dashboard), with correlated alerts in its timeline.
4. The *Checkout auto-recover* runbook runs (Automation tab) -> fault clears -> headroom recovers.
5. For *payment-gateway*, the runbook requires **approval** — approve it on the Automation tab to
   trigger the failover.

---

## Key endpoints

| Method | Path | Notes |
|---|---|---|
| GET | `/api/slos` | Live SLO status: SLI, budget headroom, per-window burn, alert tier |
| POST / DELETE | `/api/slos` , `/api/slos/{id}` | Manage SLOs (ADMIN) |
| GET | `/api/slos/alerts` | Recent burn-rate alerts (post-correlation) |
| GET / POST | `/api/runbooks` | List / create runbooks (POST: ADMIN) |
| GET | `/api/remediations` | Recent remediation executions |
| POST | `/api/remediations/{id}/approve` | Approve a queued remediation (RESPONDER/ADMIN) |
| POST | `/api/demo/fault` | Inject fault `{service,errorRate,extraLatencyMs,durationSec}` |
| POST | `/api/demo/clear/{service}` | Clear an injected fault |
| GET | `/api/demo/telemetry` | Active SLI source + per-service fault state |
| GET | `/api/stream` | SSE: `incident.*`, `slo.updated` |
| - | `/actuator/prometheus` | Micrometer metrics scraped by Prometheus |

Plus the existing incident, service-catalog, user, auth, and AI endpoints.

---

## Architecture notes

Spring Boot 3 / Java 17 / H2, React 18 + Vite. JWT auth with role-based access (ADMIN/RESPONDER/
VIEWER). SSE pushes live updates. The reliability layer reuses the existing patterns — Strategy
(SLI source, AI provider), Command (runbook steps), Observer (incident events -> remediation),
Chain of Responsibility + Template Method (triage) — so the new capabilities are wired in, not bolted on.

Burn-rate defaults are **demo-friendly** so alerts fire in minutes: FAST = 10m/2m @ 14.4x,
SLOW = 1h/10m @ 6x, TICKET = 6h/30m @ 3x (all configurable under `aiops.slo.*`). The
production-canonical 30-day values are FAST 1h/5m @ 14.4x, SLOW 6h/30m @ 6x, TICKET 1d/2h @ 3x.

### Honest caveats
- **Budget headroom** is shown as `(1 - long-window burn rate)`, a *current-rate headroom* view,
  not a true 30-day budget integral (which needs 30 days of history).
- The **latency SLI** uses the in-process histogram even when Prometheus is the availability source,
  because histogram bucket layout (`le`) is environment-specific.
- The **traffic generator** emits *real* Micrometer metrics (real counters/timers, changing in real
  time) but is self-generated demo load; point SLOs at any service you actually instrument and the
  same engine applies.
- `localStorage` token storage and the SSE query-string token are demo conveniences to harden for production.
- This repository was written and cross-reviewed but **not compiled in this environment** (no Maven/npm/
  network). Run `mvn -q -DskipTests package` and `npm install` locally and address any environment-specific issues.

---

## Real integrations & going live

Every external touchpoint is a real connector with a safe simulated fallback, selected by
config. With nothing set, the platform runs fully offline on prepopulated demo data; set the
relevant env vars and it does real work. The **Integrations** tab shows each connector's
live/simulated state.

| Integration | Category | Live when set | Env var |
|---|---|---|---|
| Prometheus | telemetry | SLIs read via PromQL | `PROMETHEUS_URL` |
| Anthropic AI | ai | real triage/postmortems | `ANTHROPIC_API_KEY` |
| Slack | notify | incoming webhook | `SLACK_WEBHOOK_URL` |
| PagerDuty | notify | Events API v2 | `PAGERDUTY_ROUTING_KEY` |
| Generic webhook | notify | Teams/Opsgenie/SIEM | `OUTBOUND_WEBHOOK_URL` |
| Alertmanager | ingestion | inbound `POST /api/ingest/alertmanager` | (always on) |
| Remediation executor | remediation | webhook or Kubernetes | see below |

**Remediation execution modes** (`RemediationExecutor` Strategy):
- `simulated` (default) — clears the injected fault so the demo loop closes.
- `webhook` — `POST`s the action to your automation system (`REMEDIATION_WEBHOOK_URL`).
- `kubernetes` — rollout-restart / scale via the K8s API (`K8S_API_URL`, `K8S_TOKEN`, `K8S_NAMESPACE`).

A **dry-run guardrail** (`REMEDIATION_DRY_RUN`, default `true`) forces simulation regardless of
mode — so the platform never touches real infrastructure until you explicitly set it to `false`.
To run real remediation against a cluster:
```bash
REMEDIATION_DRY_RUN=false REMEDIATION_MODE=kubernetes \
K8S_API_URL=https://my-cluster:6443 K8S_TOKEN=*** K8S_NAMESPACE=prod
```

---

## Design patterns (load-bearing, not decorative)

The reliability and integration features are built *out of* these — swapping a provider or adding
a channel is a new class, not an edit to the core.

- **Strategy** — AI provider, SLI source (Prometheus/in-memory), notification channels, remediation executor.
- **Adapter** — Slack, PagerDuty, generic webhook, Kubernetes API, Anthropic, Alertmanager.
- **Factory** — notifier-set per severity; remediation executor selection.
- **Decorator** — retrying notification wrapper.
- **Observer** — incident events fan out to metrics, audit, notifications, and remediation.
- **Command** — incident operations and each runbook step run through one invoker.
- **Template Method** — integration health probes share one timed/guarded algorithm.
- **Chain of Responsibility** — triage pipeline (classify → enrich → root-cause → assign).
- **State** — incident lifecycle transitions.
- **Proxy / Facade** — caching + circuit-breaking around AI; analysis facade; integration registry.
- Plus **Builder**, **Repository**, **Singleton/DI**, and a **Circuit Breaker** (Resilience4j).
