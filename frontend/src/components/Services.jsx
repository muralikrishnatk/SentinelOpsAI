import { useState } from 'react';
import { api } from '../api/client';

const HEALTH_CLASS = { HEALTHY: 'h-ok', DEGRADED: 'h-warn', CRITICAL: 'h-crit' };

export default function Services({ services, isAdmin, onChange }) {
  const [form, setForm] = useState({ name: '', description: '', ownerTeam: '', tier: 'TIER2', runbookUrl: '' });
  const [busy, setBusy] = useState(false);
  const [show, setShow] = useState(false);

  const create = async () => {
    if (!form.name) return;
    setBusy(true);
    try { await api.createService(form); setForm({ name: '', description: '', ownerTeam: '', tier: 'TIER2', runbookUrl: '' }); setShow(false); onChange(); }
    finally { setBusy(false); }
  };

  const remove = async (id) => { await api.deleteService(id); onChange(); };

  return (
    <div>
      <p className="section-title">Service Catalog</p>
      {isAdmin && !show && <button className="btn" style={{ marginBottom: 14 }} onClick={() => setShow(true)}>+ Register Service</button>}
      {isAdmin && show && (
        <div className="panel" style={{ marginBottom: 14 }}>
          <input className="field" placeholder="Service name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input className="field" placeholder="Description" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <input className="field" placeholder="Owner team" value={form.ownerTeam} onChange={(e) => setForm({ ...form, ownerTeam: e.target.value })} />
          <select className="field" value={form.tier} onChange={(e) => setForm({ ...form, tier: e.target.value })}>
            <option>TIER1</option><option>TIER2</option><option>TIER3</option>
          </select>
          <input className="field" placeholder="Runbook URL" value={form.runbookUrl} onChange={(e) => setForm({ ...form, runbookUrl: e.target.value })} />
          <div className="btn-row">
            <button className="btn" onClick={create} disabled={busy}>Save</button>
            <button className="btn ghost" onClick={() => setShow(false)}>Cancel</button>
          </div>
        </div>
      )}

      <div className="svc-grid">
        {services.map((s) => (
          <div className="card svc-card" key={s.id}>
            <div className="row">
              <h3>{s.name}</h3>
              <span className={`health ${HEALTH_CLASS[s.health]}`}>{s.health}</span>
            </div>
            <div className="meta">{s.tier} · {s.ownerTeam || 'unassigned'}</div>
            {s.description && <div className="summary">{s.description}</div>}
            <div className="meta" style={{ marginTop: 8 }}>{s.openIncidents} open incident(s)</div>
            <div className="btn-row" style={{ marginTop: 8 }}>
              {s.runbookUrl && <a className="link" href={s.runbookUrl} target="_blank" rel="noreferrer">Runbook ↗</a>}
              {isAdmin && <button className="btn ghost small" onClick={() => remove(s.id)}>Delete</button>}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
