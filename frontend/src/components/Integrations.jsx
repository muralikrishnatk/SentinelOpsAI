import { useEffect, useState, useCallback } from 'react';
import { api } from '../api/client';

const STATE_CLASS = {
  UP: 'int-up',
  DOWN: 'int-down',
  SIMULATED: 'int-sim',
  NOT_CONFIGURED: 'int-na',
};

const CATEGORY_LABEL = {
  telemetry: 'Telemetry',
  ai: 'AI',
  notification: 'Outbound notifications',
  remediation: 'Remediation',
  ingestion: 'Inbound ingestion',
};

export default function Integrations() {
  const [rows, setRows] = useState([]);

  const load = useCallback(async () => setRows(await api.integrations()), []);

  useEffect(() => {
    load().catch(console.error);
    const id = setInterval(() => load().catch(console.error), 10000);
    return () => clearInterval(id);
  }, [load]);

  const categories = [...new Set(rows.map((r) => r.category))];
  const liveCount = rows.filter((r) => r.state === 'UP').length;

  return (
    <div>
      <div className="row">
        <p className="section-title">Integrations &amp; connectivity</p>
        <span className="ai-pill">{liveCount}/{rows.length} live</span>
      </div>
      <div className="muted" style={{ marginBottom: 14, fontSize: 13 }}>
        Each connector runs live when configured (env vars) or in a safe simulated mode for the demo.
        Switch remediation to a real executor with <code>REMEDIATION_DRY_RUN=false</code> and a mode.
      </div>

      {categories.map((cat) => (
        <div key={cat} style={{ marginBottom: 18 }}>
          <p className="section-title" style={{ fontSize: 13 }}>{CATEGORY_LABEL[cat] || cat}</p>
          <div className="svc-grid">
            {rows.filter((r) => r.category === cat).map((r) => (
              <div className="card int-card" key={r.name}>
                <div className="row">
                  <h3>{r.name}</h3>
                  <span className={`int-pill ${STATE_CLASS[r.state] || ''}`}>{r.state}</span>
                </div>
                <div className="meta">{r.detail}</div>
                {r.latencyMs > 0 && <div className="meta">probe {r.latencyMs}ms</div>}
              </div>
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}
