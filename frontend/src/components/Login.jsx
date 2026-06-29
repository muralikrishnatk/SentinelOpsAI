import { useState } from 'react';
import { api, auth } from '../api/client';

const DEMO = [
  { u: 'admin', label: 'Admin' },
  { u: 'responder', label: 'Responder' },
  { u: 'viewer', label: 'Viewer' },
];

export default function Login({ onLogin }) {
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setError(null);
    setBusy(true);
    try {
      const res =
        mode === 'login'
          ? await api.login(username, password)
          : await api.register({ username, password, displayName: username });
      auth.token = res.token;
      onLogin();
    } catch (e) {
      setError(e.message || 'Authentication failed');
    } finally {
      setBusy(false);
    }
  };

  const quick = (u) => {
    setUsername(u);
    setPassword(`${u}123`);
    setMode('login');
  };

  return (
    <div className="login-wrap">
      <div className="login-card">
        <div className="brand" style={{ marginBottom: 18 }}>
          <div className="logo">S</div>
          <div>
            <h1>Sentinel AIOps</h1>
            <small>Incident management &amp; observability</small>
          </div>
        </div>

        <input
          className="field"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && submit()}
        />
        <input
          className="field"
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && submit()}
        />
        {error && <div className="error-text">{error}</div>}
        <button className="btn" style={{ width: '100%' }} onClick={submit} disabled={busy}>
          {busy ? '…' : mode === 'login' ? 'Sign in' : 'Create account'}
        </button>

        <div className="muted" style={{ marginTop: 12, fontSize: 13 }}>
          {mode === 'login' ? "No account? " : 'Have an account? '}
          <a
            className="link"
            onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
          >
            {mode === 'login' ? 'Sign up' : 'Sign in'}
          </a>
        </div>

        <div className="demo-row">
          <span className="muted" style={{ fontSize: 12 }}>Demo logins:</span>
          {DEMO.map((d) => (
            <button key={d.u} className="btn ghost small" onClick={() => quick(d.u)}>
              {d.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
