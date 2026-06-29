import { useEffect, useState } from 'react';
import { api } from '../api/client';

const ROLES = ['ADMIN', 'RESPONDER', 'VIEWER'];

export default function Users() {
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState({ username: '', password: '', displayName: '', role: 'RESPONDER', team: '' });
  const [busy, setBusy] = useState(false);
  const [show, setShow] = useState(false);

  const load = () => api.listUsers().then(setUsers).catch(() => {});
  useEffect(() => { load(); }, []);

  const create = async () => {
    if (!form.username || !form.password) return;
    setBusy(true);
    try { await api.createUser(form); setForm({ username: '', password: '', displayName: '', role: 'RESPONDER', team: '' }); setShow(false); load(); }
    finally { setBusy(false); }
  };

  const setRole = async (id, role) => { await api.changeRole(id, role); load(); };

  return (
    <div>
      <p className="section-title">User Management</p>
      {!show && <button className="btn" style={{ marginBottom: 14 }} onClick={() => setShow(true)}>+ Add User</button>}
      {show && (
        <div className="panel" style={{ marginBottom: 14, maxWidth: 480 }}>
          <input className="field" placeholder="Username" value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} />
          <input className="field" type="password" placeholder="Password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
          <input className="field" placeholder="Display name" value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} />
          <input className="field" placeholder="Team" value={form.team} onChange={(e) => setForm({ ...form, team: e.target.value })} />
          <select className="field" value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            {ROLES.map((r) => <option key={r}>{r}</option>)}
          </select>
          <div className="btn-row">
            <button className="btn" onClick={create} disabled={busy}>Create</button>
            <button className="btn ghost" onClick={() => setShow(false)}>Cancel</button>
          </div>
        </div>
      )}

      <div className="panel">
        <table className="utable">
          <thead><tr><th>User</th><th>Email</th><th>Team</th><th>Role</th></tr></thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td>{u.displayName}<div className="muted" style={{ fontSize: 12 }}>@{u.username}</div></td>
                <td className="muted">{u.email || '—'}</td>
                <td className="muted">{u.team || '—'}</td>
                <td>
                  <select className="role-select" value={u.role} onChange={(e) => setRole(u.id, e.target.value)}>
                    {ROLES.map((r) => <option key={r}>{r}</option>)}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
