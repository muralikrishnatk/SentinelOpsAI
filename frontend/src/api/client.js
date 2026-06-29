// API client with JWT auth. Token persists in localStorage (real app, not an artifact).
const TOKEN_KEY = 'aiops_token';

export const auth = {
  get token() { return localStorage.getItem(TOKEN_KEY); },
  set token(t) { t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY); },
  clear() { localStorage.removeItem(TOKEN_KEY); },
};

function headers(extra = {}) {
  const h = { 'Content-Type': 'application/json', ...extra };
  if (auth.token) h['Authorization'] = `Bearer ${auth.token}`;
  return h;
}

async function handle(r) {
  if (r.status === 401) {
    auth.clear();
    window.dispatchEvent(new Event('aiops:unauthorized'));
  }
  if (!r.ok) {
    const err = await r.json().catch(() => ({ message: r.statusText }));
    return Promise.reject(err);
  }
  return r.status === 204 ? null : r.json();
}

const get = (u) => fetch(u, { headers: headers() }).then(handle);
const post = (u, body) =>
  fetch(u, { method: 'POST', headers: headers(), body: body ? JSON.stringify(body) : undefined }).then(handle);
const put = (u, body) =>
  fetch(u, { method: 'PUT', headers: headers(), body: body ? JSON.stringify(body) : undefined }).then(handle);
const del = (u) => fetch(u, { method: 'DELETE', headers: headers() }).then(handle);

export const api = {
  login: (username, password) => post('/api/auth/login', { username, password }),
  register: (body) => post('/api/auth/register', body),
  me: () => get('/api/auth/me'),

  listIncidents: () => get('/api/incidents'),
  searchIncidents: ({ q = '', status = '', severity = '', page = 0, size = 10 } = {}) => {
    const params = new URLSearchParams();
    if (q) params.set('q', q);
    if (status) params.set('status', status);
    if (severity) params.set('severity', severity);
    params.set('page', page);
    params.set('size', size);
    return get(`/api/incidents/search?${params.toString()}`);
  },
  createIncident: (body) => post('/api/incidents', body),
  transition: (id, body) => post(`/api/incidents/${id}/transition`, body),
  reanalyze: (id) => post(`/api/incidents/${id}/reanalyze`),
  assign: (id, assignee) => post(`/api/incidents/${id}/assign`, { assignee }),
  timeline: (id) => get(`/api/incidents/${id}/timeline`),
  comment: (id, message) => post(`/api/incidents/${id}/comments`, { message }),
  generatePostmortem: (id) => post(`/api/incidents/${id}/postmortem`),
  getPostmortem: (id) => get(`/api/incidents/${id}/postmortem`),

  dashboard: () => get('/api/dashboard'),
  aiStatus: () => get('/api/ai/status'),
  ask: (question) => post('/api/ai/ask', { question }),

  listServices: () => get('/api/services'),
  createService: (body) => post('/api/services', body),
  deleteService: (id) => del(`/api/services/${id}`),

  listUsers: () => get('/api/users'),
  createUser: (body) => post('/api/users', body),
  changeRole: (id, role) => put(`/api/users/${id}/role?role=${role}`),

  // SLOs & error budgets
  slos: () => get('/api/slos'),
  createSlo: (body) => post('/api/slos', body),
  deleteSlo: (id) => del(`/api/slos/${id}`),
  sloAlerts: () => get('/api/slos/alerts'),

  // Runbooks & remediation
  runbooks: () => get('/api/runbooks'),
  createRunbook: (body) => post('/api/runbooks', body),
  remediations: () => get('/api/remediations'),
  approveRemediation: (id) => post(`/api/remediations/${id}/approve`),

  // Demo / telemetry
  telemetry: () => get('/api/demo/telemetry'),
  injectFault: (body) => post('/api/demo/fault', body),
  clearFault: (service) => post(`/api/demo/clear/${service}`),

  // Integrations health
  integrations: () => get('/api/integrations'),

  // On-call
  oncall: (team) => get(`/api/oncall${team ? `?team=${encodeURIComponent(team)}` : ''}`),
  oncallNow: (team) => get(`/api/oncall/current?team=${encodeURIComponent(team)}`),
  addShift: (body) => post('/api/oncall', body),
  deleteShift: (id) => del(`/api/oncall/${id}`),

  // Organizations & teams
  orgs: () => get('/api/orgs'),
  teams: (orgId) => get(`/api/teams${orgId ? `?orgId=${orgId}` : ''}`),

  // Audit (admin)
  audit: ({ page = 0, size = 25, q = '' } = {}) => {
    const params = new URLSearchParams();
    params.set('page', page); params.set('size', size);
    if (q) params.set('q', q);
    return get(`/api/audit?${params.toString()}`);
  },
};
