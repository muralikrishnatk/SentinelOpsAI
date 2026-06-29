import { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';

export default function Audit() {
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0, number: 0 });
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);

  const load = useCallback(async () => {
    setData(await api.audit({ page, size: 25, q }));
  }, [page, q]);

  useEffect(() => { load().catch(console.error); }, [load]);

  return (
    <div>
      <div className="row">
        <p className="section-title">Audit log</p>
        <span className="ai-pill">{data.totalElements} events</span>
      </div>
      <div className="btn-row" style={{ marginBottom: 12 }}>
        <input className="field" style={{ marginBottom: 0, maxWidth: 280 }}
          placeholder="Filter by actor or action…" value={q}
          onChange={(e) => { setPage(0); setQ(e.target.value); }} />
      </div>

      <div className="panel">
        <div className="audit-head">
          <span>When</span><span>Actor</span><span>Action</span><span>Status</span><span>IP</span>
        </div>
        {data.content.length === 0 && <div className="muted" style={{ padding: 10 }}>No audit events.</div>}
        {data.content.map((e) => (
          <div className="audit-row" key={e.id}>
            <span className="tl-meta">{new Date(e.at).toLocaleString()}</span>
            <span>{e.actor}{e.role ? <span className="muted"> ({e.role.replace('ROLE_', '')})</span> : ''}</span>
            <span className="mono">{e.action}</span>
            <span className={`badge ${e.status >= 400 ? 'st-fail' : 'st-ok'}`}>{e.status}</span>
            <span className="muted">{e.ip}</span>
          </div>
        ))}
      </div>

      <div className="btn-row" style={{ marginTop: 12, justifyContent: 'center' }}>
        <button className="btn small ghost" disabled={page <= 0} onClick={() => setPage((p) => p - 1)}>Prev</button>
        <span className="meta" style={{ alignSelf: 'center' }}>Page {data.number + 1} / {Math.max(1, data.totalPages)}</span>
        <button className="btn small ghost" disabled={page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
      </div>
    </div>
  );
}
