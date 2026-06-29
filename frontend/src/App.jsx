import { useEffect, useState, useCallback, useRef } from 'react';
import { api, auth } from './api/client';
import Login from './components/Login.jsx';
import StatCards from './components/StatCards.jsx';
import SeverityChart from './components/SeverityChart.jsx';
import Incidents from './components/Incidents.jsx';
import AiAssistant from './components/AiAssistant.jsx';
import Services from './components/Services.jsx';
import Users from './components/Users.jsx';
import Reliability from './components/Reliability.jsx';
import Remediation from './components/Remediation.jsx';
import Integrations from './components/Integrations.jsx';
import NotificationCenter from './components/NotificationCenter.jsx';

export default function App() {
  const [me, setMe] = useState(null);
  const [booting, setBooting] = useState(true);
  const [tab, setTab] = useState('dashboard');
  const [notifications, setNotifications] = useState([]);

  const [incidents, setIncidents] = useState([]);
  const [stats, setStats] = useState(null);
  const [services, setServices] = useState([]);
  const [aiStatus, setAiStatus] = useState(null);
  const esRef = useRef(null);

  const loadProfile = useCallback(async () => {
    if (!auth.token) { setMe(null); setBooting(false); return; }
    try {
      setMe(await api.me());
    } catch {
      auth.clear();
      setMe(null);
    } finally {
      setBooting(false);
    }
  }, []);

  const refresh = useCallback(async () => {
    const [list, dash, svc, ai] = await Promise.all([
      api.listIncidents(), api.dashboard(), api.listServices(), api.aiStatus(),
    ]);
    setIncidents(list); setStats(dash); setServices(svc); setAiStatus(ai);
  }, []);

  useEffect(() => { loadProfile(); }, [loadProfile]);

  useEffect(() => {
    const onUnauth = () => setMe(null);
    window.addEventListener('aiops:unauthorized', onUnauth);
    return () => window.removeEventListener('aiops:unauthorized', onUnauth);
  }, []);

  // Load data + open the live SSE stream once authenticated.
  useEffect(() => {
    if (!me) return;
    refresh().catch(console.error);

    const es = new EventSource(`/api/stream?token=${encodeURIComponent(auth.token)}`);
    esRef.current = es;
    const onChange = () => refresh().catch(console.error);
    const pushNote = (kind, data) => setNotifications((list) => [{
      id: data?.id ?? Date.now(), kind,
      title: data?.title || (kind === 'incident.created' ? 'New incident opened' : 'Incident updated'),
      severity: data?.severity || null, time: Date.now(),
    }, ...list].slice(0, 50));

    es.addEventListener('incident.created', (e) => {
      let d = null; try { d = JSON.parse(e.data); } catch {}
      pushNote('incident.created', d); onChange(); window.dispatchEvent(new Event('aiops:incident'));
    });
    ['incident.updated', 'incident.comment'].forEach((ev) =>
      es.addEventListener(ev, () => { onChange(); window.dispatchEvent(new Event('aiops:incident')); })
    );
    // SLO engine ticks drive the Reliability + Automation tabs without a full refresh.
    es.addEventListener('slo.updated', (e) => {
      let d = null; try { d = JSON.parse(e.data); } catch {}
      if (d && d.breach) pushNote('slo.updated', { id: 'slo-' + Date.now(), title: 'SLO burn-rate alert firing', severity: 'SEV2' });
      window.dispatchEvent(new Event('aiops:slo'));
    });
    es.onerror = () => { /* browser auto-reconnects */ };
    return () => es.close();
  }, [me, refresh]);

  const logout = () => { auth.clear(); setMe(null); esRef.current?.close(); };

  if (booting) return <div className="app"><div className="spinner">Loading…</div></div>;
  if (!me) return <Login onLogin={loadProfile} />;

  const isAdmin = me.role === 'ADMIN';
  const canWrite = me.role === 'ADMIN' || me.role === 'RESPONDER';

  return (
    <div className="app">
      <div className="topbar">
        <div className="brand">
          <div className="logo">S</div>
          <div>
            <h1>Sentinel AIOps</h1>
            <small>AI-powered incident management &amp; observability</small>
          </div>
        </div>
        <div className="top-right">
          <span className="ai-pill">
            AI: <b>{aiStatus ? aiStatus.activeProvider : '…'}</b>
            {aiStatus && !aiStatus.remote ? ' (offline)' : ''}
          </span>
          <NotificationCenter items={notifications} onClear={() => setNotifications([])} />
          <span className="user-chip">
            {me.displayName} · <span className={`role-tag role-${me.role}`}>{me.role}</span>
          </span>
          <button className="btn ghost small" onClick={logout}>Logout</button>
        </div>
      </div>

      <div className="tabs">
        <button className={tab === 'dashboard' ? 'tab active' : 'tab'} onClick={() => setTab('dashboard')}>
          Dashboard
        </button>
        <button className={tab === 'services' ? 'tab active' : 'tab'} onClick={() => setTab('services')}>
          Services
        </button>
        <button className={tab === 'reliability' ? 'tab active' : 'tab'} onClick={() => setTab('reliability')}>
          Reliability
        </button>
        <button className={tab === 'automation' ? 'tab active' : 'tab'} onClick={() => setTab('automation')}>
          Automation
        </button>
        <button className={tab === 'integrations' ? 'tab active' : 'tab'} onClick={() => setTab('integrations')}>
          Integrations
        </button>
        {isAdmin && (
          <button className={tab === 'users' ? 'tab active' : 'tab'} onClick={() => setTab('users')}>
            Users
          </button>
        )}
      </div>

      {tab === 'dashboard' && (
        <>
          <div className="grid cards"><StatCards stats={stats} /></div>
          <div className="cols">
            <div>
              <p className="section-title">Active Incidents</p>
              <Incidents incidents={incidents} onChange={refresh} canWrite={canWrite} />
            </div>
            <div>
              <p className="section-title">Severity Mix</p>
              <SeverityChart stats={stats} />
              <div style={{ height: 16 }} />
              <p className="section-title">AI Assistant</p>
              <AiAssistant />
            </div>
          </div>
        </>
      )}

      {tab === 'services' && <Services services={services} isAdmin={isAdmin} onChange={refresh} />}
      {tab === 'reliability' && <Reliability canWrite={canWrite} />}
      {tab === 'automation' && <Remediation canWrite={canWrite} />}
      {tab === 'integrations' && <Integrations />}
      {tab === 'users' && isAdmin && <Users />}
    </div>
  );
}
