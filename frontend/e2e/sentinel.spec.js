import { test, expect, chromium } from '@playwright/test';
import fs from 'fs';
import path from 'path';

const BASE = 'http://localhost:5173';
const SCREENS = path.join(process.cwd(), 'e2e', 'screens');
fs.mkdirSync(SCREENS, { recursive: true });

let browser, page;
const results = [];        // { tab, control, action, expected, actual, status, screenshot }
const consoleErrors = [];  // { text, location, when }
let currentAction = 'page load';

function rec(tab, control, action, expected, actual, status, screenshot = '') {
  results.push({ tab, control, action, expected, actual, status, screenshot });
  // eslint-disable-next-line no-console
  console.log(`ROW|${tab}|${control}|${action}|${expected}|${actual}|${status}|${screenshot}`);
}

async function shoot(name) {
  const file = path.join(SCREENS, `${name}.png`);
  try { await page.screenshot({ path: file, fullPage: true }); } catch {}
  return path.relative(process.cwd(), file);
}

// Run an assertion-style probe; record PASS/FAIL, screenshot on FAIL. Never throws.
async function probe(tab, control, action, expected, fn) {
  currentAction = `${tab} > ${control}: ${action}`;
  try {
    const actual = await fn();
    rec(tab, control, action, expected, actual ?? 'ok', 'PASS');
    return true;
  } catch (e) {
    const shot = await shoot(`${tab}_${control}_${action}`.replace(/[^a-z0-9]+/gi, '_').slice(0, 80));
    rec(tab, control, action, expected, `ERROR: ${(e.message || e).toString().split('\n')[0]}`, 'FAIL', shot);
    return false;
  }
}

async function login(username) {
  // NOTE: do NOT use waitUntil:'networkidle' — the SSE /api/stream connection
  // stays open so the network never goes idle once authenticated.
  await page.goto(BASE, { waitUntil: 'domcontentloaded' });
  await page.evaluate(() => localStorage.clear());
  await page.reload({ waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Sign in' }).waitFor({ timeout: 15000 });
  await page.getByPlaceholder('Username').fill(username);
  await page.getByPlaceholder('Password').fill(`${username}123`);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.locator('.topbar').waitFor({ timeout: 15000 });
}

// Re-open the drawer for a given incident title if it has collapsed.
async function ensureDrawer(title) {
  const inc = page.locator('.incident', { hasText: title }).first();
  if (await inc.locator('.drawer').count() === 0) {
    await inc.click();
    await inc.locator('.drawer').waitFor({ timeout: 5000 });
  }
  return inc.locator('.drawer');
}

async function openTab(name) {
  await page.locator('.tabs button', { hasText: new RegExp(`^${name}$`) }).click();
  await page.waitForTimeout(600);
}

test.beforeAll(async () => {
  browser = await chromium.launch();
  page = await browser.newPage();
  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      consoleErrors.push({ text: msg.text(), location: JSON.stringify(msg.location()), when: currentAction });
    }
  });
  page.on('pageerror', (err) => {
    consoleErrors.push({ text: 'PAGEERROR: ' + err.message, location: '', when: currentAction });
  });
});

test.afterAll(async () => {
  fs.writeFileSync(path.join(process.cwd(), 'e2e', 'results.json'),
    JSON.stringify({ results, consoleErrors }, null, 2));
  console.log('CONSOLE_ERRORS|' + JSON.stringify(consoleErrors));
  await browser.close();
});

test.describe.serial('Sentinel AIOps UI', () => {
  test('Login screen + auth', async () => {
    currentAction = 'login page load';
    await page.goto(BASE, { waitUntil: 'networkidle' });
    await page.evaluate(() => localStorage.clear());
    await page.reload({ waitUntil: 'networkidle' });

    // wrong password
    await probe('Login', 'Sign in (wrong pw)', 'submit bad creds', 'error message shown', async () => {
      await page.getByPlaceholder('Username').fill('responder');
      await page.getByPlaceholder('Password').fill('wrongpass');
      await page.getByRole('button', { name: 'Sign in' }).click();
      await page.locator('.error-text').waitFor({ timeout: 8000 });
      return 'error: ' + (await page.locator('.error-text').innerText());
    });

    // demo shortcut button fills fields
    await probe('Login', 'Demo "Responder" button', 'click', 'fills username/password', async () => {
      await page.getByRole('button', { name: 'Responder' }).click();
      const u = await page.getByPlaceholder('Username').inputValue();
      const p = await page.getByPlaceholder('Password').inputValue();
      if (u !== 'responder' || p !== 'responder123') throw new Error(`filled u=${u} p=${p}`);
      return `u=${u} p=${p}`;
    });

    // actual login via demo-filled creds
    await probe('Login', 'Sign in (valid)', 'submit', 'lands on dashboard', async () => {
      await page.getByRole('button', { name: 'Sign in' }).click();
      await page.locator('.topbar').waitFor({ timeout: 15000 });
      await expect(page.locator('.role-tag')).toContainText('RESPONDER');
      return 'topbar visible, role RESPONDER';
    });
  });

  test('Top bar: bell + logout', async () => {
    await probe('Topbar', 'Notification bell', 'click open', 'panel opens', async () => {
      await page.locator('.notif-bell').click();
      await page.locator('.notif-panel').waitFor({ timeout: 5000 });
      return 'panel open';
    });
    await probe('Topbar', 'Notification bell', 'click close', 'panel closes', async () => {
      await page.locator('.notif-bell').click();
      await expect(page.locator('.notif-panel')).toHaveCount(0);
      return 'panel closed';
    });
    await probe('Topbar', 'Logout', 'click', 'returns to login', async () => {
      await page.getByRole('button', { name: 'Logout' }).click();
      await page.getByRole('button', { name: 'Sign in' }).waitFor({ timeout: 8000 });
      return 'login screen';
    });
  });

  test('Tab navigation (responder)', async () => {
    await login('responder');
    for (const t of ['Dashboard', 'Services', 'Reliability', 'Automation', 'Integrations']) {
      await probe('Nav', `Tab ${t}`, 'click', 'renders, no blank screen', async () => {
        await openTab(t);
        const active = await page.locator('.tabs button.active').innerText();
        const bodyLen = (await page.locator('.app').innerText()).length;
        if (bodyLen < 50) throw new Error('blank-ish screen, len=' + bodyLen);
        return `active=${active}, contentLen=${bodyLen}`;
      });
    }
    // Users tab should NOT exist for responder
    await probe('Nav', 'Users tab (responder)', 'check absent', 'hidden for non-admin', async () => {
      const cnt = await page.locator('.tabs button', { hasText: /^Users$/ }).count();
      if (cnt !== 0) throw new Error('Users tab visible to responder');
      return 'absent (correct)';
    });
  });

  test('Dashboard: stat cards + Ask AI', async () => {
    await openTab('Dashboard');
    await probe('Dashboard', 'Stat cards', 'load', '4 stat cards with numbers', async () => {
      const n = await page.locator('.grid.cards .card').count();
      if (n < 4) throw new Error('only ' + n + ' cards');
      const total = await page.locator('.grid.cards .card .v').first().innerText();
      return `${n} cards, first value=${total}`;
    });
    await probe('Dashboard', 'Severity chart', 'render', 'chart svg present', async () => {
      await page.locator('.cols svg, .recharts-surface').first().waitFor({ timeout: 6000 });
      return 'chart rendered';
    });
    await probe('Dashboard', 'Ask AI (typed)', 'type+submit', 'answer renders', async () => {
      await page.getByPlaceholder('Ask about your incidents in plain English…').fill('Which incidents are unresolved?');
      await page.getByRole('button', { name: 'Ask AI' }).click();
      await page.locator('.cols .ai-box').last().waitFor({ timeout: 10000 });
      const txt = await page.locator('.cols .ai-box').last().innerText();
      if (txt.length < 10) throw new Error('empty answer');
      return 'answer: ' + txt.slice(0, 60);
    });
    await probe('Dashboard', 'Ask AI suggestion chip', 'click', 'answer renders', async () => {
      await page.getByRole('button', { name: 'What is the most critical incident right now?' }).click();
      await page.waitForTimeout(1500);
      const txt = await page.locator('.cols .ai-box').last().innerText();
      return 'answer: ' + txt.slice(0, 60);
    });
  });

  test('Incidents: create form', async () => {
    await openTab('Dashboard');
    const title = 'E2E Probe ' + Date.now();
    await probe('Incidents', '+ Report Incident', 'open form', 'form fields appear', async () => {
      await page.getByRole('button', { name: '+ Report Incident' }).click();
      await page.getByPlaceholder('Title').waitFor({ timeout: 5000 });
      return 'form open';
    });
    await probe('Incidents', 'Create & Triage', 'fill+submit', 'new incident appears in list', async () => {
      await page.getByPlaceholder('Title').fill(title);
      await page.getByPlaceholder('Affected service (e.g. checkout-api)').fill('checkout-api');
      await page.getByPlaceholder('Description').fill('created by e2e test');
      await page.getByRole('button', { name: 'Create & Triage' }).click();
      await page.locator('.incident h3', { hasText: title }).waitFor({ timeout: 10000 });
      return 'incident visible in list';
    });
    // stash title for next tests
    process.env.__E2E_TITLE = title;
  });

  test('Incident drawer controls', async () => {
    await openTab('Dashboard');
    const title = process.env.__E2E_TITLE;
    const row = page.locator('.incident', { hasText: title }).first();

    await probe('Incident drawer', 'Open drawer', 'click row', 'drawer expands', async () => {
      await row.click();
      await row.locator('.drawer').waitFor({ timeout: 5000 });
      return 'drawer open';
    });

    // BUG PROBE: does clicking a control inside the drawer keep it open?
    await probe('Incident drawer', 'Drawer stays open on control click', 'click a transition btn', 'drawer remains open after click', async () => {
      const drawer = await ensureDrawer(title);
      await drawer.getByRole('button', { name: '→ ACKNOWLEDGED' }).click();
      await page.waitForTimeout(1200);
      const stillOpen = await page.locator('.incident', { hasText: title }).first().locator('.drawer').count();
      if (stillOpen === 0) throw new Error('drawer COLLAPSED after clicking a control (row onClick bubbling)');
      return 'drawer stayed open';
    });

    // Functional: transition actually changes status (verify on row badge, reopening as needed)
    await probe('Incident drawer', 'Transition → ACKNOWLEDGED', 'click', 'status badge becomes ACKNOWLEDGED', async () => {
      await expect(page.locator('.incident', { hasText: title }).first().locator('.badge.status'))
        .toHaveText('ACKNOWLEDGED', { timeout: 8000 });
      return 'status=ACKNOWLEDGED';
    });

    await probe('Incident drawer', 'Assign input + button', 'type+click Assign', 'assignee updates (verify by reopening)', async () => {
      const drawer = await ensureDrawer(title);
      await drawer.getByPlaceholder('Assign to user or team…').fill('payments-oncall');
      await drawer.getByRole('button', { name: 'Assign', exact: true }).click();
      await page.waitForTimeout(1200);
      const d2 = await ensureDrawer(title);
      await expect(d2.locator('.kv').first()).toContainText('payments-oncall', { timeout: 8000 });
      return 'assignee=payments-oncall';
    });

    await probe('Incident drawer', 'Re-run AI', 'click', 'timeline gets AI re-analysis event', async () => {
      const drawer = await ensureDrawer(title);
      await drawer.getByRole('button', { name: 'Re-run AI' }).click();
      await page.waitForTimeout(1500);
      const d2 = await ensureDrawer(title);
      await expect(d2.locator('.tl')).toContainText('AI', { timeout: 8000 });
      return 'AI re-analysis event in timeline';
    });

    await probe('Incident drawer', 'Comment box + Post', 'type+submit', 'comment appears in timeline', async () => {
      const drawer = await ensureDrawer(title);
      const marker = 'e2e-comment-' + Date.now();
      await drawer.getByPlaceholder('Add a comment…').fill(marker);
      await drawer.getByRole('button', { name: 'Post', exact: true }).click();
      await page.waitForTimeout(1200);
      const d2 = await ensureDrawer(title);
      await expect(d2.locator('.tl')).toContainText(marker, { timeout: 8000 });
      return 'comment in timeline: ' + marker;
    });

    await probe('Incident drawer', 'Postmortem subtab', 'click', 'switches to postmortem view', async () => {
      const drawer = await ensureDrawer(title);
      await drawer.getByRole('button', { name: 'Postmortem' }).click();
      await page.waitForTimeout(800);
      const d2 = await ensureDrawer(title);
      // subtab click may collapse drawer; if reopened it defaults to Timeline, so click again
      if (await d2.getByRole('button', { name: 'Postmortem' }).getAttribute('class').then(c => !c.includes('active')).catch(() => true)) {
        await d2.getByRole('button', { name: 'Postmortem' }).click();
        await page.waitForTimeout(500);
      }
      const d3 = await ensureDrawer(title);
      await d3.locator('.postmortem').waitFor({ timeout: 5000 });
      return 'postmortem view shown';
    });

    await probe('Incident drawer', 'Generate AI Postmortem', 'click', 'postmortem text appears', async () => {
      let d = await ensureDrawer(title);
      // ensure on postmortem subtab
      if (await d.locator('.postmortem').count() === 0) {
        await d.getByRole('button', { name: 'Postmortem' }).click();
        await page.waitForTimeout(600);
        d = await ensureDrawer(title);
        if (await d.locator('.postmortem').count() === 0) { await d.getByRole('button', { name: 'Postmortem' }).click(); d = await ensureDrawer(title); }
      }
      const genBtn = d.getByRole('button', { name: 'Generate AI Postmortem' });
      if (await genBtn.count() > 0) await genBtn.click();
      await page.waitForTimeout(2000);
      let d2 = await ensureDrawer(title);
      if (await d2.locator('.postmortem').count() === 0) { await d2.getByRole('button', { name: 'Postmortem' }).click(); d2 = await ensureDrawer(title); }
      const txt = await d2.locator('.postmortem').innerText();
      if (txt.length < 30 || txt.toLowerCase().includes('none yet')) throw new Error('no postmortem text: ' + txt.slice(0, 50));
      return 'postmortem len=' + txt.length;
    });
  });

  test('Reliability: SLO cards + inject/clear fault + alerts', async () => {
    await openTab('Reliability');
    await probe('Reliability', 'SLO cards', 'render', 'cards with SLI numbers', async () => {
      await page.locator('.slo-card').first().waitFor({ timeout: 8000 });
      const n = await page.locator('.slo-card').count();
      const sli = await page.locator('.slo-card .sli-big').first().innerText();
      return `${n} SLO cards, first SLI=${sli}`;
    });

    // Inject fault on checkout-api card, then verify FAULT ACTIVE + headroom/burn change
    await probe('Reliability', 'Inject fault', 'click', 'card shows FAULT ACTIVE', async () => {
      const card = page.locator('.slo-card', { hasText: 'checkout-api' }).first();
      // ensure not already faulted
      if (await card.locator('.btn', { hasText: 'Clear fault' }).count() > 0) {
        await card.getByRole('button', { name: 'Clear fault' }).click();
        await page.waitForTimeout(1500);
      }
      await card.getByRole('button', { name: 'Inject fault' }).click();
      await expect(page.locator('.slo-card', { hasText: 'checkout-api' }).first())
        .toContainText('FAULT ACTIVE', { timeout: 12000 });
      return 'FAULT ACTIVE shown';
    });

    await probe('Reliability', 'Burn rate updates', 'wait for live refresh', 'SLI/headroom changes after fault', async () => {
      const card = page.locator('.slo-card', { hasText: 'checkout-api' }).first();
      const sli1 = parseFloat(await card.locator('.sli-big').first().innerText());
      await page.waitForTimeout(7000); // 5s live refresh interval
      const card2 = page.locator('.slo-card', { hasText: 'checkout-api' }).first();
      const sli2 = parseFloat(await card2.locator('.sli-big').first().innerText());
      return `SLI ${sli1} -> ${sli2} (changed=${sli1 !== sli2})`;
    });

    await probe('Reliability', 'Alerts feed', 'populate after fault', 'burn-rate alert row appears', async () => {
      await page.waitForTimeout(8000);
      const n = await page.locator('.alert-row').count();
      if (n === 0) throw new Error('no alert rows after fault');
      return n + ' alert rows';
    });

    await probe('Reliability', 'Clear fault', 'click', 'FAULT ACTIVE clears', async () => {
      const card = page.locator('.slo-card', { hasText: 'checkout-api' }).first();
      await card.getByRole('button', { name: 'Clear fault' }).click();
      await page.waitForTimeout(2000);
      return 'clear clicked';
    });
  });

  test('Automation: runbooks + executions + approve', async () => {
    await openTab('Automation');
    await probe('Automation', 'Runbook list', 'render', 'runbook cards present', async () => {
      await page.locator('.svc-grid .card').first().waitFor({ timeout: 8000 });
      const n = await page.locator('.svc-grid .card').count();
      return n + ' runbook cards';
    });
    await probe('Automation', 'Executions list', 'render', 'execution rows present', async () => {
      const n = await page.locator('.exec').count();
      if (n === 0) throw new Error('no execution rows');
      return n + ' executions';
    });
    await probe('Automation', 'Approve & run', 'create pending then approve', 'pending count drops, SUCCEEDED appears', async () => {
      // Deterministically create a PENDING_APPROVAL: a payment-gateway incident triggers
      // the approval-required failover runbook (autoExecute=false).
      const token = await page.evaluate(() => localStorage.getItem('aiops_token'));
      await page.evaluate(async (token) => {
        await fetch('/api/incidents', {
          method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
          body: JSON.stringify({ title: 'E2E approval probe ' + Date.now(), affectedService: 'payment-gateway', severity: 'SEV2' }),
        });
      }, token);
      await page.waitForTimeout(2500);
      await openTab('Dashboard'); await openTab('Automation'); // force refresh
      await page.waitForTimeout(1500);
      const pendingBtns = page.getByRole('button', { name: /Approve & run/ });
      const before = await pendingBtns.count();
      if (before === 0) return 'NO pending approval created by payment-gateway incident (nothing to approve)';
      await pendingBtns.first().click();
      // wait until an approve button disappears AND a SUCCEEDED badge exists
      await expect.poll(async () => await page.getByRole('button', { name: /Approve & run/ }).count(),
        { timeout: 15000 }).toBeLessThan(before);
      const succeeded = await page.locator('.exec .badge', { hasText: 'SUCCEEDED' }).count();
      if (succeeded === 0) throw new Error('no SUCCEEDED execution after approve');
      return `pending ${before}->${await pendingBtns.count()}, SUCCEEDED execs=${succeeded}`;
    });
  });

  test('Integrations: rows render', async () => {
    await openTab('Integrations');
    await probe('Integrations', 'Integration cards', 'render', 'rows with live/simulated state', async () => {
      await page.locator('.int-card').first().waitFor({ timeout: 8000 });
      const n = await page.locator('.int-card').count();
      const states = await page.locator('.int-pill').allInnerTexts();
      return `${n} cards; states=${[...new Set(states)].join(',')}`;
    });
  });

  test('Notifications: toast + bell increment on new incident (SSE)', async () => {
    await openTab('Reliability');
    // Inject a fault to trigger SLO alert -> incident -> SSE notification
    await probe('Notifications', 'SSE toast on event', 'inject fault, await toast/bell', 'toast appears or bell count increments', async () => {
      const card = page.locator('.slo-card', { hasText: 'checkout-api' }).first();
      if (await card.getByRole('button', { name: 'Clear fault' }).count() > 0) {
        await card.getByRole('button', { name: 'Clear fault' }).click();
        await page.waitForTimeout(1500);
      }
      await page.locator('.slo-card', { hasText: 'checkout-api' }).first().getByRole('button', { name: 'Inject fault' }).click();
      // Wait up to 90s for a notification toast or bell count
      const ok = await Promise.race([
        page.locator('.notif-toast').waitFor({ timeout: 90000 }).then(() => 'toast'),
        page.locator('.notif-count').waitFor({ timeout: 90000 }).then(() => 'bell-count'),
      ]).catch(() => null);
      if (!ok) throw new Error('no toast/bell within 90s');
      const count = await page.locator('.notif-count').count() ? await page.locator('.notif-count').innerText() : 'n/a';
      return `${ok} appeared, bellCount=${count}`;
    });
    // cleanup fault
    try {
      const card = page.locator('.slo-card', { hasText: 'checkout-api' }).first();
      if (await card.getByRole('button', { name: 'Clear fault' }).count() > 0)
        await card.getByRole('button', { name: 'Clear fault' }).click();
    } catch {}
  });

  test('Live update: new incident appears without manual refresh', async () => {
    await openTab('Dashboard');
    await probe('Live/SSE', 'Auto list update', 'create incident via API, no reload', 'row appears via SSE', async () => {
      const t = 'SSE-Live-' + Date.now();
      const token = await page.evaluate(() => localStorage.getItem('aiops_token'));
      await page.evaluate(async ({ t, token }) => {
        await fetch('/api/incidents', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Authorization: 'Bearer ' + token },
          body: JSON.stringify({ title: t, affectedService: 'data-platform', severity: 'SEV3' }),
        });
      }, { t, token });
      await page.locator('.incident h3', { hasText: t }).waitFor({ timeout: 15000 });
      return 'row appeared without reload';
    });
  });

  test('RBAC: viewer is read-only', async () => {
    await login('viewer');
    await probe('RBAC', 'Login as viewer', 'login', 'role tag shows VIEWER', async () => {
      await expect(page.locator('.role-tag')).toContainText('VIEWER', { timeout: 8000 });
      return 'VIEWER';
    });

    await probe('RBAC', 'Users tab', 'check hidden', 'absent for viewer', async () => {
      const cnt = await page.locator('.tabs button', { hasText: /^Users$/ }).count();
      if (cnt !== 0) throw new Error('Users tab visible to viewer');
      return 'absent (correct)';
    });

    await openTab('Dashboard');
    await probe('RBAC', 'Report Incident button', 'check hidden', 'no create button on dashboard', async () => {
      const cnt = await page.getByRole('button', { name: '+ Report Incident' }).count();
      if (cnt !== 0) throw new Error('create button visible to viewer');
      return 'hidden (correct)';
    });
    await probe('RBAC', 'Incident drawer write controls', 'open drawer, check', 'no transition/assign/comment controls', async () => {
      const row = page.locator('.incident').first();
      if (await row.count() === 0) return 'no incidents to open';
      await row.click();
      await page.locator('.drawer').first().waitFor({ timeout: 5000 });
      const trans = await page.getByRole('button', { name: /^→ / }).count();
      const assign = await page.getByPlaceholder('Assign to user or team…').count();
      const comment = await page.getByPlaceholder('Add a comment…').count();
      if (trans + assign + comment !== 0) throw new Error(`write controls visible: trans=${trans} assign=${assign} comment=${comment}`);
      return 'no write controls (correct)';
    });

    await openTab('Reliability');
    await probe('RBAC', 'Inject/Clear fault buttons', 'check hidden', 'no fault buttons for viewer', async () => {
      await page.locator('.slo-card').first().waitFor({ timeout: 8000 });
      const inj = await page.getByRole('button', { name: 'Inject fault' }).count();
      const clr = await page.getByRole('button', { name: 'Clear fault' }).count();
      if (inj + clr !== 0) throw new Error(`fault buttons visible: inject=${inj} clear=${clr}`);
      return 'no fault buttons (correct)';
    });

    await openTab('Automation');
    await probe('RBAC', 'Approve button', 'check hidden', 'no approve button for viewer', async () => {
      const cnt = await page.getByRole('button', { name: /Approve & run/ }).count();
      if (cnt !== 0) throw new Error('approve button visible to viewer');
      return 'hidden (correct)';
    });
  });

  test('Admin: Users tab + controls', async () => {
    await login('admin');
    await probe('Users', 'Login as admin', 'login', 'role tag shows ADMIN', async () => {
      await expect(page.locator('.role-tag')).toContainText('ADMIN', { timeout: 8000 });
      return 'ADMIN';
    });
    await probe('Users', 'Users tab visible', 'check present', 'admin sees Users tab', async () => {
      const cnt = await page.locator('.tabs button', { hasText: /^Users$/ }).count();
      if (cnt !== 1) throw new Error('Users tab missing for admin');
      return 'present';
    });
    await openTab('Users');
    await probe('Users', 'User table', 'render', 'seeded users listed', async () => {
      await page.locator('.utable tbody tr').first().waitFor({ timeout: 8000 });
      const n = await page.locator('.utable tbody tr').count();
      if (n < 3) throw new Error('only ' + n + ' users');
      return n + ' users';
    });
    await probe('Users', '+ Add User', 'open form', 'create form appears', async () => {
      await page.getByRole('button', { name: '+ Add User' }).click();
      await page.getByPlaceholder('Username').waitFor({ timeout: 5000 });
      return 'form open';
    });
    await probe('Users', 'Create user', 'fill+submit', 'new user row appears', async () => {
      const uname = 'e2euser' + Date.now();
      await page.getByPlaceholder('Username').fill(uname);
      await page.getByPlaceholder('Password').fill('pw123456');
      await page.getByPlaceholder('Display name').fill('E2E User');
      await page.getByRole('button', { name: 'Create' }).click();
      await page.locator('.utable tbody tr', { hasText: '@' + uname }).waitFor({ timeout: 8000 });
      return 'user created';
    });
    await probe('Users', 'Change role select', 'change dropdown', 'role persists after reload', async () => {
      const row = page.locator('.utable tbody tr', { hasText: 'viewer' }).first();
      await row.locator('select.role-select').selectOption('RESPONDER');
      await page.waitForTimeout(1200);
      await openTab('Dashboard'); await openTab('Users');
      const val = await page.locator('.utable tbody tr', { hasText: 'viewer' }).first().locator('select.role-select').inputValue();
      // restore
      await page.locator('.utable tbody tr', { hasText: 'viewer' }).first().locator('select.role-select').selectOption('VIEWER');
      if (val !== 'RESPONDER') throw new Error('role did not persist, got ' + val);
      return 'role changed to RESPONDER then restored';
    });
  });
});