import { useState, useEffect } from 'react';
import { api } from '../api/client';

const TRANSITIONS = {
  OPEN: ['ACKNOWLEDGED', 'CLOSED'],
  ACKNOWLEDGED: ['INVESTIGATING', 'RESOLVED'],
  INVESTIGATING: ['RESOLVED', 'ACKNOWLEDGED'],
  RESOLVED: ['CLOSED', 'INVESTIGATING'],
  CLOSED: [],
};

function CreateForm({ onCreated }) {
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState({ title: '', affectedService: '', description: '', rawSignal: '' });
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    if (!form.title || !form.affectedService) return;
    setBusy(true);
    try {
      await api.createIncident(form);
      setForm({ title: '', affectedService: '', description: '', rawSignal: '' });
      setOpen(false);
      onCreated();
    } finally { setBusy(false); }
  };

  if (!open)
    return <button className="btn" style={{ marginBottom: 14 }} onClick={() => setOpen(true)}>+ Report Incident</button>;

  return (
      <div className="panel" style={{ marginBottom: 14 }}>
        <input className="field" placeholder="Title" value={form.title}
               onChange={(e) => setForm({ ...form, title: e.target.value })} />
        <input className="field" placeholder="Affected service (e.g. checkout-api)" value={form.affectedService}
               onChange={(e) => setForm({ ...form, affectedService: e.target.value })} />
        <textarea className="field" placeholder="Description" value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })} />
        <textarea className="field" placeholder="Raw signal / log snippet (AI uses this)" value={form.rawSignal}
                  onChange={(e) => setForm({ ...form, rawSignal: e.target.value })} />
        <div className="btn-row">
          <button className="btn" onClick={submit} disabled={busy}>{busy ? 'Running AI triage…' : 'Create & Triage'}</button>
          <button className="btn ghost" onClick={() => setOpen(false)}>Cancel</button>
        </div>
      </div>
  );
}

function Timeline({ id, refreshKey, canWrite }) {
  const [events, setEvents] = useState([]);
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);

  const load = () => api.timeline(id).then(setEvents).catch(() => {});
  useEffect(() => { load(); }, [id, refreshKey]);

  const send = async () => {
    if (!comment.trim()) return;
    setBusy(true);
    try { await api.comment(id, comment); setComment(''); load(); }
    finally { setBusy(false); }
  };

  return (
      <div style={{ marginTop: 10 }}>
        <div className="tl">
          {events.map((e) => (
              <div className="tl-item" key={e.id}>
                <span className={`tl-type tl-${e.type}`}>{e.type}</span>
                <span className="tl-msg">{e.message}</span>
                <span className="tl-meta">{e.actor} · {new Date(e.createdAt).toLocaleTimeString()}</span>
              </div>
          ))}
        </div>
        {canWrite && (
            <div className="btn-row" style={{ marginTop: 8 }}>
              <input className="field" style={{ marginBottom: 0 }} placeholder="Add a comment…"
                     value={comment} onChange={(e) => setComment(e.target.value)}
                     onKeyDown={(e) => e.key === 'Enter' && send()} />
              <button className="btn small" onClick={send} disabled={busy}>Post</button>
            </div>
        )}
      </div>
  );
}

function Detail({ incident, onChange, canWrite }) {
  const [busy, setBusy] = useState(false);
  const [tab, setTab] = useState('timeline');
  const [postmortem, setPostmortem] = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [assignee, setAssignee] = useState('');

  const act = async (fn) => { setBusy(true); try { await fn(); onChange(); setRefreshKey((k) => k + 1); } finally { setBusy(false); } };

  const loadPostmortem = async () => {
    const r = await api.getPostmortem(incident.id);
    setPostmortem(r.postmortem || '(none yet — generate one)');
  };
  useEffect(() => { if (tab === 'postmortem') loadPostmortem(); }, [tab]);

  return (
      <div className="drawer">
        <div className="kv"><span className="k">Assignee</span><span>{incident.assignee || '—'}</span></div>
        <div className="kv"><span className="k">Reporter</span><span>{incident.reporter || '—'}</span></div>
        <div className="kv"><span className="k">SLA</span>
          <span>
          ack ≤ {incident.slaAckTargetMinutes}m {incident.slaAckBreached && <span className="breach">ACK BREACHED</span>}
            {' · '}resolve ≤ {incident.slaResolveTargetMinutes}m {incident.slaResolveBreached && <span className="breach">RESOLVE BREACHED</span>}
        </span>
        </div>

        {incident.aiSummary && <div className="ai-box" style={{ marginTop: 10 }}><span className="tag">AI Summary · </span>{incident.aiSummary}</div>}
        {incident.aiRootCause && <div className="ai-box" style={{ marginTop: 8 }}><span className="tag">AI Root Cause · </span>{incident.aiRootCause}</div>}

        {canWrite && (
            <div className="btn-row" style={{ marginTop: 12 }}>
              {(TRANSITIONS[incident.status] || []).map((s) => (
                  <button key={s} className="btn small" disabled={busy} onClick={() => act(() => api.transition(incident.id, { targetStatus: s }))}>→ {s}</button>
              ))}
              <button className="btn small ghost" disabled={busy} onClick={() => act(() => api.reanalyze(incident.id))}>Re-run AI</button>
            </div>
        )}

        {canWrite && (
            <div className="btn-row" style={{ marginTop: 8 }}>
              <input className="field" style={{ marginBottom: 0, maxWidth: 220 }}
                     placeholder="Assign to user or team…" value={assignee}
                     onChange={(e) => setAssignee(e.target.value)}
                     onKeyDown={(e) => e.key === 'Enter' && assignee.trim() && act(() => api.assign(incident.id, assignee.trim()))} />
              <button className="btn small" disabled={busy || !assignee.trim()}
                      onClick={() => act(() => api.assign(incident.id, assignee.trim()))}>Assign</button>
            </div>
        )}

        <div className="subtabs">
          <button className={tab === 'timeline' ? 'subtab active' : 'subtab'} onClick={() => setTab('timeline')}>Timeline</button>
          <button className={tab === 'postmortem' ? 'subtab active' : 'subtab'} onClick={() => setTab('postmortem')}>Postmortem</button>
        </div>

        {tab === 'timeline' && <Timeline id={incident.id} refreshKey={refreshKey} canWrite={canWrite} />}
        {tab === 'postmortem' && (
            <div style={{ marginTop: 10 }}>
              {canWrite && (
                  <button className="btn small" disabled={busy}
                          onClick={() => act(async () => { const r = await api.generatePostmortem(incident.id); setPostmortem(r.postmortem); })}>
                    {busy ? 'Generating…' : 'Generate AI Postmortem'}
                  </button>
              )}
              <pre className="postmortem">{postmortem || 'Loading…'}</pre>
            </div>
        )}
      </div>
  );
}

export default function Incidents({ incidents, onChange, canWrite }) {
  const [selected, setSelected] = useState(null);
  return (
      <div>
        {canWrite && <CreateForm onCreated={onChange} />}
        {incidents.length === 0 && <div className="muted">No incidents.</div>}
        {incidents.map((i) => (
            <div className="incident" key={i.id} onClick={() => setSelected(selected === i.id ? null : i.id)}>
              <div className="row">
                <h3>{i.title}</h3>
                <div className="btn-row">
                  {(i.slaAckBreached || i.slaResolveBreached) && <span className="badge breach-badge">SLA</span>}
                  <span className={`badge sev-${i.severity}`}>{i.severity}</span>
                  <span className="badge status">{i.status}</span>
                </div>
              </div>
              <div className="meta">#{i.id} · {i.affectedService} · {i.reporter} · {new Date(i.updatedAt).toLocaleString()}</div>
              {i.aiSummary && selected !== i.id && <div className="summary">{i.aiSummary.slice(0, 140)}…</div>}
              {selected === i.id && (
                  <div onClick={(e) => e.stopPropagation()}>
                    <Detail incident={i} onChange={onChange} canWrite={canWrite} />
                  </div>
              )}
            </div>
        ))}
      </div>
  );
}