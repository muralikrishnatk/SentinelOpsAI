import { useState } from 'react';
import { api } from '../api/client';

const SUGGESTIONS = [
  'Which incidents are still unresolved?',
  'What is the most critical incident right now?',
  'Summarize the payment-related incidents.',
];

export default function AiAssistant() {
  const [q, setQ] = useState('');
  const [resp, setResp] = useState(null);
  const [busy, setBusy] = useState(false);

  const ask = async (question) => {
    const text = question ?? q;
    if (!text.trim()) return;
    setBusy(true);
    setResp(null);
    try {
      const r = await api.ask(text);
      setResp(r);
    } catch (e) {
      setResp({ answer: 'Error: ' + (e.message || 'request failed'), provider: '-' });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="panel">
      <textarea
        className="field"
        placeholder="Ask about your incidents in plain English…"
        value={q}
        onChange={(e) => setQ(e.target.value)}
      />
      <div className="btn-row" style={{ marginBottom: 10 }}>
        <button className="btn" onClick={() => ask()} disabled={busy}>
          {busy ? 'Thinking…' : 'Ask AI'}
        </button>
      </div>
      <div className="btn-row" style={{ marginBottom: 8 }}>
        {SUGGESTIONS.map((s) => (
          <button key={s} className="btn ghost small" onClick={() => { setQ(s); ask(s); }}>
            {s}
          </button>
        ))}
      </div>
      {resp && (
        <div className="ai-box">
          {resp.answer}
          <div className="muted" style={{ marginTop: 8, fontSize: 11 }}>
            via {resp.provider}
            {resp.degraded ? ' · degraded mode' : ''}
          </div>
        </div>
      )}
    </div>
  );
}
