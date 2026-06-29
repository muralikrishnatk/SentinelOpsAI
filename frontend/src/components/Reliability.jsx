import { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';

function HeadroomBar({ pct }) {
  const clamped = Math.max(-100, Math.min(100, pct));
  const width = Math.max(0, Math.min(100, clamped));
  const color = clamped < 0 ? 'var(--sev1)' : clamped < 25 ? 'var(--sev3)' : 'var(--ok)';
  return (
    <div className="budget">
      <div className="budget-track">
        <div className="budget-fill" style={{ width: `${width}%`, background: color }} />
      </div>
      <span className="budget-label" style={{ color }}>
        {clamped < 0 ? `over budget (${clamped.toFixed(0)}%)` : `${clamped.toFixed(0)}% headroom`}
      </span>
    </div>
  );
}

function SloCard({ slo, canWrite, onFault, onClear }) {
  const tierClass =
    slo.alertTier === 'FAST' ? 'tier-fast'
      : slo.alertTier === 'SLOW' ? 'tier-slow'
      : slo.alertTier === 'TICKET' ? 'tier-ticket' : 'tier-none';

  return (
    <div className="card slo-card">
      <div className="row">
        <div>
          <h3>{slo.name}</h3>
          <div className="meta">{slo.service} · target {slo.objectivePercent}% · {slo.windowDays}d</div>
        </div>
        <span className={`tierpill ${tierClass}`}>{slo.alertTier === 'NONE' ? 'OK' : slo.alertTier}</span>
      </div>

      <div className="sli-row">
        <div>
          <div className="sli-big">{slo.currentSliPercent}%</div>
          <div className="meta">current SLI ({slo.sliType.toLowerCase()})</div>
        </div>
        {slo.faulted && <span className="badge breach-badge">FAULT ACTIVE</span>}
      </div>


      <HeadroomBar pct={slo.budgetHeadroomPercent} />

      <div className="windows">
        {slo.windows.map((w) => (
          <div className={`win ${w.breaching ? 'win-breach' : ''}`} key={w.label}>
            <div className="win-rate">{w.burnRate}×</div>
            <div className="win-label">{w.label}</div>
          </div>
        ))}
      </div>

      <div className="meta" style={{ marginTop: 8 }}>SLI source: {slo.sliSource}</div>

      {canWrite && (
        <div className="btn-row" style={{ marginTop: 8 }}>
          {slo.faulted
            ? <button className="btn small ghost" onClick={() => onClear(slo.service)}>Clear fault</button>
            : <button className="btn small" onClick={() => onFault(slo.service)}>Inject fault</button>}
        </div>
      )}
    </div>
  );
}

export default function Reliability({ canWrite }) {
  const [slos, setSlos] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [telemetry, setTelemetry] = useState(null);

  const load = useCallback(async () => {
    const [s, a, t] = await Promise.all([api.slos(), api.sloAlerts(), api.telemetry()]);
    setSlos(s); setAlerts(a); setTelemetry(t);
  }, []);

  useEffect(() => {
    load().catch(console.error);
    const id = setInterval(() => load().catch(console.error), 5000); // live refresh
    const onSlo = () => load().catch(console.error);
    window.addEventListener('aiops:slo', onSlo);
    return () => { clearInterval(id); window.removeEventListener('aiops:slo', onSlo); };
  }, [load]);

  const onFault = async (service) => {
    await api.injectFault({ service, errorRate: 0.3, extraLatencyMs: 500, durationSec: 600 });
    load();
  };
  const onClear = async (service) => { await api.clearFault(service); load(); };

  return (
    <div>
      <div className="row">
        <p className="section-title">Service Level Objectives &amp; Error Budgets</p>
        {telemetry && (
          <span className="ai-pill">
            SLI source: <b>{telemetry.sliSource}</b>{telemetry.usingPrometheus ? '' : ' (in-memory)'}
          </span>
        )}
      </div>
      {canWrite && (
        <div className="muted" style={{ marginBottom: 12, fontSize: 13 }}>
          Tip: inject a fault to spike errors — within a couple of minutes the burn-rate alert fires,
          an incident is auto-opened (and correlated), and the matching runbook remediates it. Watch the
          headroom bar recover.
        </div>
      )}

      <div className="svc-grid">
        {slos.map((s) => (
          <SloCard key={s.id} slo={s} canWrite={canWrite} onFault={onFault} onClear={onClear} />
        ))}
      </div>

      <p className="section-title" style={{ marginTop: 22 }}>Recent burn-rate alerts (after correlation)</p>
      <div className="panel">
        {alerts.length === 0 && <div className="muted">No alerts yet.</div>}
        {alerts.slice(0, 15).map((a) => (
          <div className="alert-row" key={a.id}>
            <span className={`tierpill ${a.tier === 'FAST' ? 'tier-fast' : a.tier === 'SLOW' ? 'tier-slow' : 'tier-ticket'}`}>{a.tier}</span>
            <span className="alert-svc">{a.service}</span>
            <span className="muted">{a.sloName} · burn {a.burnRateShort.toFixed(1)}× / {a.burnRateLong.toFixed(1)}×</span>
            <span className="alert-inc">{a.incidentId ? `→ incident #${a.incidentId}` : 'deduped'}</span>
            <span className="tl-meta">{new Date(a.firedAt).toLocaleTimeString()}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
