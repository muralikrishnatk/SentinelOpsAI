import { test, chromium, expect } from '@playwright/test';
import fs from 'fs';
import path from 'path';

const BASE = 'http://localhost:5173';
const SCREENS = path.join(process.cwd(), 'e2e', 'screens');
fs.mkdirSync(SCREENS, { recursive: true });

let browser, page;
const results = [];
const consoleErrors = [];
let currentAction = 'load';

function rec(group, control, expected, actual, status, shot = '') {
  results.push({ group, control, expected, actual, status, shot });
  console.log(`DASH|${group}|${control}|${expected}|${actual}|${status}|${shot}`);
}
async function shoot(name) {
  const f = path.join(SCREENS, `dash_${name}`.replace(/[^a-z0-9]+/gi, '_').slice(0, 80) + '.png');
  try { await page.screenshot({ path: f, fullPage: true }); } catch {}
  return path.relative(process.cwd(), f);
}
async function probe(group, control, expected, fn) {
  currentAction = `${group} > ${control}`;
  try { const a = await fn(); rec(group, control, expected, a ?? 'ok', 'PASS'); return true; }
  catch (e) { const s = await shoot(`${group}_${control}`); rec(group, control, expected, 'ERROR: ' + (e.message || e).toString().split('\n')[0], 'FAIL', s); return false; }
}

test.beforeAll(async () => {
  browser = await chromium.launch();
  page = await browser.newPage();
  page.on('console', (m) => { if (m.type() === 'error') consoleErrors.push({ when: currentAction, text: m.text() }); });
  page.on('pageerror', (e) => consoleErrors.push({ when: currentAction, text: 'PAGEERROR: ' + e.message }));
});
test.afterAll(async () => {
  fs.writeFileSync(path.join(process.cwd(), 'e2e', 'dashboard-results.json'), JSON.stringify({ results, consoleErrors }, null, 2));
  console.log('DASH_CONSOLE|' + JSON.stringify(consoleErrors));
  await browser.close();
});

test('Dashboard page: cards, buttons, tabs', async () => {
  // ---- deploy/login ----
  await page.goto(BASE, { waitUntil: 'domcontentloaded' });
  await page.evaluate(() => localStorage.clear());
  await page.reload({ waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Sign in' }).waitFor({ timeout: 15000 });
  await page.getByPlaceholder('Username').fill('responder');
  await page.getByPlaceholder('Password').fill('responder123');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.locator('.topbar').waitFor({ timeout: 15000 });
  await page.locator('.tabs button', { hasText: /^Dashboard$/ }).click();
  await page.waitForTimeout(800);

  // ---- STAT CARDS ----
  await probe('Stat cards', 'Four KPI cards render with values', '4 cards, numeric values', async () => {
    const cards = page.locator('.grid.cards .card');
    const n = await cards.count();
    if (n < 4) throw new Error(`only ${n} cards`);
    const out = [];
    for (let i = 0; i < n; i++) {
      const k = await cards.nth(i).locator('.k').innerText();
      const v = await cards.nth(i).locator('.v').innerText();
      if (v.trim() === '' ) throw new Error(`card "${k}" has empty value`);
      out.push(`${k}=${v}`);
    }
    return out.join(', ');
  });

  // ---- SEVERITY CHART ----
  await probe('Severity chart', 'Donut chart renders', 'svg/recharts present', async () => {
    await page.locator('.cols svg, .recharts-surface').first().waitFor({ timeout: 6000 });
    const slices = await page.locator('.recharts-pie-sector, .recharts-sector').count();
    return `chart svg present, ${slices} slices`;
  });

  // ---- AI ASSISTANT (button + suggestion chips) ----
  await probe('AI Assistant', 'Ask AI button (typed query)', 'answer renders in ai-box', async () => {
    await page.getByPlaceholder('Ask about your incidents in plain English…').fill('Which incidents are unresolved?');
    await page.getByRole('button', { name: 'Ask AI' }).click();
    const box = page.locator('.cols .ai-box').last();
    await box.waitFor({ timeout: 10000 });
    const t = await box.innerText();
    if (t.trim().length < 10) throw new Error('empty answer');
    return t.slice(0, 70);
  });
  for (const chip of ['Which incidents are still unresolved?', 'What is the most critical incident right now?', 'Summarize the payment-related incidents.']) {
    await probe('AI Assistant', `Suggestion chip: "${chip.slice(0, 30)}…"`, 'answer renders', async () => {
      await page.getByRole('button', { name: chip }).click();
      await page.waitForTimeout(1500);
      const t = await page.locator('.cols .ai-box').last().innerText();
      if (t.trim().length < 10) throw new Error('empty answer');
      return t.slice(0, 50);
    });
  }

  // ---- INCIDENTS PANEL on dashboard ----
  await probe('Incidents panel', 'Active incidents list renders', 'rows present or empty-state', async () => {
    const rows = await page.locator('.cols .incident').count();
    return `${rows} incident rows`;
  });
  await probe('Incidents panel', '+ Report Incident button opens form', 'form fields appear', async () => {
    await page.getByRole('button', { name: '+ Report Incident' }).click();
    await page.getByPlaceholder('Title').waitFor({ timeout: 5000 });
    return 'form opened';
  });
  await probe('Incidents panel', 'Cancel button closes form', 'form closes', async () => {
    await page.getByRole('button', { name: 'Cancel' }).click();
    await expect(page.getByPlaceholder('Title')).toHaveCount(0, { timeout: 5000 });
    return 'form closed';
  });
  await probe('Incidents panel', 'Create & Triage adds incident', 'new row appears', async () => {
    const title = 'Dash-card-test ' + Date.now();
    await page.getByRole('button', { name: '+ Report Incident' }).click();
    await page.getByPlaceholder('Title').fill(title);
    await page.getByPlaceholder('Affected service (e.g. checkout-api)').fill('checkout-api');
    await page.getByRole('button', { name: 'Create & Triage' }).click();
    await page.locator('.cols .incident h3', { hasText: title }).waitFor({ timeout: 10000 });
    return 'incident created and listed';
  });

  // ---- TOP BAR controls reachable from dashboard ----
  await probe('Top bar', 'AI status pill shows provider', 'provider text present', async () => {
    const t = await page.locator('.ai-pill').first().innerText();
    if (!/AI:/.test(t)) throw new Error('no AI pill: ' + t);
    return t.replace(/\s+/g, ' ').trim();
  });
  await probe('Top bar', 'Notification bell opens panel', 'panel opens', async () => {
    await page.locator('.notif-bell').click();
    await page.locator('.notif-panel').waitFor({ timeout: 5000 });
    const open = await page.locator('.notif-panel').isVisible();
    await page.locator('.notif-bell').click(); // close again
    return 'panel toggled ' + open;
  });

  // ---- TAB BAR: every tab switches and dashboard reachable again ----
  const tabs = ['Dashboard', 'Services', 'Reliability', 'Automation', 'Integrations'];
  for (const t of tabs) {
    await probe('Tabs', `Tab "${t}" activates & renders`, 'becomes active, content present', async () => {
      await page.locator('.tabs button', { hasText: new RegExp(`^${t}$`) }).click();
      await page.waitForTimeout(500);
      const active = await page.locator('.tabs button.active').innerText();
      if (active.trim() !== t) throw new Error(`active tab is "${active}", expected "${t}"`);
      const len = (await page.locator('.app').innerText()).length;
      if (len < 80) throw new Error('near-blank content len=' + len);
      return `active=${active}, contentLen=${len}`;
    });
  }
  // back to dashboard, capture a clean screenshot
  await page.locator('.tabs button', { hasText: /^Dashboard$/ }).click();
  await page.waitForTimeout(800);
  const shot = await shoot('dashboard_final');
  rec('Snapshot', 'Dashboard rendered screenshot', 'captured', shot, 'PASS', shot);
});
