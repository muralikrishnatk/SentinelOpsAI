import { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';

export default function OnCall({ canWrite }) {
  const [teams, setTeams] = useState([]);
  const [shifts, setShifts] = useState([]);
  const [now, setNow] = useState({});
  const [form, setForm] = useState({ teamName: '', username: '', startAt: '', endAt: '' });

  const load = useCallback(async () => {
    const [t, s] = await Promise.all([api.teams(), api.oncall()]);
    setTeams(t);
    setShifts(s);
    const current = {};
    await Promise.all(t.map(async (team) => {
      try { current[team.name] = await api.oncallNow(team.name); } catch { /* ignore */ }
    }));
    setNow(current);
  }, []);

  useEffect(() => { load().catch(console.error); }, [load]);

  const addShift = async () => {
    if (!form.teamName || !form.username || !form.startAt || !form.endAt) return;
    await api.addShift({
      teamName: form.teamName, username: form.username,
      startAt: new Date(form.startAt).toISOString(), endAt: new Date(form.endAt).toISOString(),
    });
    setForm({ teamName: '', username: '', startAt: '', endAt: '' });
    load();
  };
  const removeShift = async (id) => { await api.deleteShift(id); load(); };

  return (
    <div>
      <p className="section-title">On-call rotations</p>

      <div className="svc-grid">
        {teams.map((t) => (
          <div className="card" key={t.id}>
            <div className="row">
              <h3>{t.name}</h3>
              <span className={`badge ${now[t.name]?.covered ? 'auto-badge' : 'manual-badge'}`}>
                {now[t.name]?.covered ? `on call: ${now[t.name].username}` : 'uncovered'}
              </span>
            </div>
            <div className="meta">{t.description || '—'}{t.escalationContact ? ` · escalates to ${t.escalationContact}` : ''}</div>
          </div>
        ))}
        {teams.length === 0 && <div className="muted">No teams yet.</div>}
      </div>

      {canWrite && (
        <div className="panel" style={{ marginTop: 16 }}>
          <p className="section-title" style={{ fontSize: 13 }}>Add shift</p>
          <div className="btn-row" style={{ flexWrap: 'wrap', gap: 8 }}>
            <select className="field" style={{ marginBottom: 0, maxWidth: 200 }}
              value={form.teamName} onChange={(e) => setForm({ ...form, teamName: e.target.value })}>
              <option value="">Team…</option>
              {teams.map((t) => <option key={t.id} value={t.name}>{t.name}</option>)}
            </select>
            <input className="field" style={{ marginBottom: 0, maxWidth: 160 }} placeholder="username"
              value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
            <input className="field" style={{ marginBottom: 0 }} type="datetime-local"
              value={form.startAt} onChange={(e) => setForm({ ...form, startAt: e.target.value })} />
            <input className="field" style={{ marginBottom: 0 }} type="datetime-local"
              value={form.endAt} onChange={(e) => setForm({ ...form, endAt: e.target.value })} />
            <button className="btn small" onClick={addShift}>Add</button>
          </div>
        </div>
      )}

      <p className="section-title" style={{ marginTop: 22 }}>Schedule</p>
      <div className="panel">
        {shifts.length === 0 && <div className="muted">No shifts scheduled.</div>}
        {shifts.map((s) => (
          <div className="alert-row" key={s.id}>
            <span className="alert-svc">{s.teamName}</span>
            <span>{s.username}</span>
            <span className="muted">{new Date(s.startAt).toLocaleString()} → {new Date(s.endAt).toLocaleString()}</span>
            {canWrite && <button className="btn small ghost" onClick={() => removeShift(s.id)}>Remove</button>}
          </div>
        ))}
      </div>
    </div>
  );
}
