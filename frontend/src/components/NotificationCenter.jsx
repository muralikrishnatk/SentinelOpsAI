import { useEffect, useRef, useState } from 'react';

/**
 * In-app notifications driven by the live SSE stream. Shows a bell with an unread
 * count, a dropdown list, and a transient toast when a new incident/alert arrives.
 * `items` is newest-first: { id, kind, title, severity, time }.
 */
export default function NotificationCenter({ items, onClear }) {
  const [open, setOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [toast, setToast] = useState(null);
  const lastIdRef = useRef(null);
  const toastTimer = useRef(null);

  useEffect(() => {
    if (!items || items.length === 0) return;
    const newest = items[0];
    const key = `${newest.kind}:${newest.id}:${newest.time}`;
    if (lastIdRef.current === null) { lastIdRef.current = key; return; } // skip initial load
    if (key !== lastIdRef.current) {
      lastIdRef.current = key;
      setUnread((u) => u + 1);
      setToast(newest);
      clearTimeout(toastTimer.current);
      toastTimer.current = setTimeout(() => setToast(null), 6000);
    }
  }, [items]);

  const toggle = () => { setOpen((o) => !o); setUnread(0); };

  return (
    <div className="notif-wrap">
      <button className="notif-bell" onClick={toggle} title="Notifications">
        <span aria-hidden>🔔</span>
        {unread > 0 && <span className="notif-count">{unread > 9 ? '9+' : unread}</span>}
      </button>

      {open && (
        <div className="notif-panel">
          <div className="notif-head">
            <b>Notifications</b>
            <button className="btn ghost small" onClick={() => { onClear?.(); setOpen(false); }}>Clear</button>
          </div>
          {(!items || items.length === 0) && <div className="muted" style={{ padding: 10 }}>Nothing yet.</div>}
          {items && items.slice(0, 20).map((n) => (
            <div className="notif-item" key={`${n.kind}-${n.id}-${n.time}`}>
              <span className={`badge sev-${n.severity || 'SEV3'}`}>{n.severity || 'INFO'}</span>
              <span className="notif-title">{n.title}</span>
              <span className="tl-meta">{new Date(n.time).toLocaleTimeString()}</span>
            </div>
          ))}
        </div>
      )}

      {toast && (
        <div className="notif-toast" onClick={() => setToast(null)}>
          <span className={`badge sev-${toast.severity || 'SEV3'}`}>{toast.severity || 'INFO'}</span>
          <span>{toast.title}</span>
        </div>
      )}
    </div>
  );
}
