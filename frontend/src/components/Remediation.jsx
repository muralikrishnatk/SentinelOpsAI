import { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';

const STATUS_CLASS = {
  SUCCEEDED: 'st-ok',
  RUNNING: 'st-run',
  PENDING_APPROVAL: 'st-pending',
  FAILED: 'st-fail',
  SKIPPED: 'st-skip',
};

export default function Remediation({ canWrite }) {
  const [runbooks, setRunbooks] = useState([]);
  const [execs, setExecs] = useState([]);
  const [busy, setBusy] = useState(null);

  const load = useCallback(async () => {
    const [r, e] = await Promise.all([api.runbooks(), api.remediations()]);
    setRunbooks(r); setExecs(e);
  }, []);

  useEffect(() => {
    load().catch(console.error);
    const id = setInterval(() => load().catch(console.error), 5000);
    const onSlo = () => load().catch(console.error);
    window.addEventListener('aiops:slo', onSlo);
    window.addEventListener('aiops:incident', onSlo);
    return () => {
      clearInterval(id);
      window.removeEventListener('aiops:slo', onSlo);
      window.removeEventListener('aiops:incident', onSlo);
    };
  }, [load]);

  const approve = async (id) => {
    setBusy(id);
    try { await api.approveRemediation(id); await load(); }
    catch (e) { alert(e.message || 'Failed to approve'); }
    finally { setBusy(null); }
  };

  return (
    <div>
      <p className="section-title">Remediation runbooks</p>
      <div className="svc-grid">
        {runbooks.map((rb) => (
          <div className="card" key={rb.id}>
            <div className="row">
              <h3>{rb.name}</h3>
              <span className={`badge ${rb.autoExecute ? 'auto-badge' : 'manual-badge'}`}>
                {rb.autoExecute ? 'AUTO' : 'APPROVAL'}
              </span>
            </div>
            <div className="meta">{rb.service} · max {rb.maxExecutionsPerHour}/h</div>
            <ol className="steps">
              {rb.steps.map((s, i) => (
                <li key={i}><code>{s.action}</code>{s.params ? ` — ${s.params}` : ''}</li>
              ))}
            </ol>
          </div>
        ))}
      </div>

      <p className="section-title" style={{ marginTop: 22 }}>Recent executions</p>
      <div className="panel">
        {execs.length === 0 && <div className="muted">No remediation runs yet.</div>}
        {execs.map((e) => (
          <div className="exec" key={e.id}>
            <div className="row">
              <div>
                <b>{e.runbookName}</b> <span className="muted">on {e.service}</span>
                {e.incidentId && <span className="alert-inc"> → incident #{e.incidentId}</span>}
              </div>
              <span className={`badge ${STATUS_CLASS[e.status] || ''}`}>{e.status}</span>
            </div>
            <div className="tl-meta" style={{ gridColumn: 1 }}>
              triggered by {e.triggeredBy}{e.approvedBy ? ` · approved by ${e.approvedBy}` : ''} · {new Date(e.createdAt).toLocaleTimeString()}
            </div>
            {e.log && <pre className="exec-log">{e.log}</pre>}
            {canWrite && e.status === 'PENDING_APPROVAL' && (
              <button className="btn small" disabled={busy === e.id} onClick={() => approve(e.id)}>
                {busy === e.id ? 'Running…' : 'Approve & run'}
              </button>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
