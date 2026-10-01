// Stress test Payment Platform : 10 -> 10000 utilisateurs connectés.
// Mix : login (1x/VU + refresh sur 401), création/consultation/confirmation de
// paiements, commandes, livraisons, soldes, notifications. Rapport JSON + HTML.
//
// Usage (PowerShell, depuis payment-platform-ui/ ou deploy/stress/) :
//   node stress.mjs                              # run complet (STAGES par défaut)
//   $env:STAGES="10,20"; $env:STAGE_SECS="20"; node stress.mjs   # smoke
//   $env:BASE_URL="http://localhost:8081"; node stress.mjs
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));
const BASE = process.env.BASE_URL || 'http://localhost:8081';
const STAGES = (process.env.STAGES || '10,20,40,80,160,320,640,1280,2560,5120,10000')
  .split(',').map((s) => parseInt(s.trim(), 10)).filter((n) => n > 0);
const STAGE_SECS = parseInt(process.env.STAGE_SECS || '60', 10);
const RAMP_SECS = parseInt(process.env.RAMP_SECS || '10', 10);
const REQ_TIMEOUT = parseInt(process.env.REQ_TIMEOUT_MS || '15000', 10);
const SHOP_USER = process.env.SHOP_USER || 'ali.e2e';
const SHOP_PASS = process.env.SHOP_PASS || 'test1234';
const SUPPLIER_USER = process.env.SUPPLIER_USER || 'covale.admin.e2e';
const SUPPLIER_PASS = process.env.SUPPLIER_PASS || 'test1234';

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const rand = (n) => Math.floor(Math.random() * n);
const pick = (arr) => arr[rand(arr.length)];

// Histogramme log-buckets : 120 buckets de 1ms à ~60s.
const BUCKETS = 120;
const bucketOf = (ms) => {
  if (ms <= 0) return 0;
  const b = Math.floor(Math.log10(ms) * 20);
  return Math.max(0, Math.min(BUCKETS - 1, b));
};
const bucketMs = (b) => Math.pow(10, b / 20);

function newStats() {
  return { count: 0, ok: 0, fail: 0, throttled: 0, buckets: new Array(BUCKETS).fill(0), errors: new Map() };
}
function record(stats, ms, ok, errKey, throttled) {
  stats.count++;
  if (ok) stats.ok++;
  else stats.fail++;
  if (throttled) stats.throttled++;
  stats.buckets[bucketOf(ms)]++;
  if (errKey) stats.errors.set(errKey, (stats.errors.get(errKey) || 0) + 1);
}
function percentile(stats, p) {
  const target = Math.ceil((stats.count * p) / 100);
  let acc = 0;
  for (let b = 0; b < BUCKETS; b++) {
    acc += stats.buckets[b];
    if (acc >= target) return Math.round(bucketMs(b));
  }
  return 0;
}
function summarize(stats, secs) {
  return {
    requests: stats.count,
    ok: stats.ok,
    fail: stats.fail,
    throttled429: stats.throttled,
    rps: secs > 0 ? +(stats.count / secs).toFixed(1) : 0,
    p50ms: percentile(stats, 50),
    p95ms: percentile(stats, 95),
    p99ms: percentile(stats, 99),
    errors: Object.fromEntries(stats.errors),
  };
}

async function req(method, path, token, body, stats, label) {
  const t0 = Date.now();
  const ctrl = new AbortController();
  const to = setTimeout(() => ctrl.abort(), REQ_TIMEOUT);
  try {
    const res = await fetch(`${BASE}${path}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: ctrl.signal,
    });
    const ms = Date.now() - t0;
    if (res.status === 429) {
      record(stats, ms, false, `${label}:429`, true);
      return { status: 429, body: null };
    }
    let parsed = null;
    try { parsed = await res.json(); } catch { /* corps vide */ }
    const ok = res.status >= 200 && res.status < 300;
    record(stats, ms, ok, ok ? null : `${label}:${res.status}`);
    return { status: res.status, body: parsed };
  } catch (e) {
    const ms = Date.now() - t0;
    const key = e?.name === 'AbortError' ? `${label}:TIMEOUT` : `${label}:NET`;
    record(stats, ms, false, key, false);
    return { status: 0, body: null };
  } finally {
    clearTimeout(to);
  }
}

async function login(username, password, stats) {
  // Backoff anti-429 sur /login (budget auth limité côté gateway).
  for (let attempt = 0; attempt < 6; attempt++) {
    const r = await req('POST', '/api/auth/login', null, { username, password }, stats, 'login');
    if (r.status === 200 && r.body?.accessToken) return r.body.accessToken;
    if (r.status === 429) await sleep(1000 * (attempt + 1) + rand(500));
    else if (r.status === 0) await sleep(500);
    else return null;
  }
  return null;
}

// Contexte partagé (org ids, produit) résolu une fois au démarrage.
const shared = { shopId: null, supplierId: null, productId: null, agentId: null };

async function boot(stats) {
  const admin = await login('system.admin', '@PAssword012345', stats);
  if (!admin) throw new Error('login system.admin impossible');
  const H = (t) => ({ Authorization: `Bearer ${t}` });
  const get = async (p) => (await (await fetch(`${BASE}${p}`, { headers: H(admin) })).json());
  const sup = await get('/api/admin/suppliers?size=50');
  const shops = await get('/api/admin/shops?size=50');
  const items = (b) => (Array.isArray(b) ? b : b?.items || []);
  shared.supplierId = items(sup).find((s) => s.name?.includes('Covale'))?.id || items(sup)[0]?.id;
  shared.shopId = items(shops).find((s) => s.name?.includes('Ali'))?.id || items(shops)[0]?.id;
  const products = await get(`/api/suppliers/${shared.supplierId}/products`);
  const list = Array.isArray(products) ? products : products?.items || [];
  shared.productId = list.find((p) => (p.quantity - (p.reservedQty || 0)) >= 5)?.id || list[0]?.id || null;
  if (!shared.shopId || !shared.supplierId) throw new Error('contexte seed introuvable');
  console.log(`[boot] shop=${shared.shopId} supplier=${shared.supplierId} product=${shared.productId}`);
}

// Une VU = boucle fermée : login 1x (shop+supplier), puis itérations du mix.
async function vuLoop(id, stageCtl, stats) {
  let shopTok = await login(SHOP_USER, SHOP_PASS, stats);
  let supTok = await login(SUPPLIER_USER, SUPPLIER_PASS, stats);
  if (!shopTok || !supTok) return;
  const myPayments = [];
  const myOrders = [];
  // Démarrage étalé du stage (anti-pic).
  await sleep(rand(RAMP_SECS * 1000));
  while (stageCtl.running) {
    const roll = Math.random();
    try {
      if (roll < 0.30) {
        // Création paiement (boutique).
        const r = await req('POST', '/api/payments', shopTok, {
          shopId: shared.shopId, supplierId: shared.supplierId,
          amount: 10 + rand(990) + 0.99, currency: 'TND', notes: 'STRESS',
        }, stats, 'pay.create');
        if (r.status === 201 || r.status === 200) {
          if (r.body?.id) { myPayments.push(r.body.id); if (myPayments.length > 50) myPayments.shift(); }
        } else if (r.status === 401) {
          shopTok = await login(SHOP_USER, SHOP_PASS, stats);
        }
      } else if (roll < 0.55) {
        // Consultation paiements (liste + filtre).
        await req('GET', `/api/payments?page=${rand(3)}&size=20`, shopTok, null, stats, 'pay.list');
      } else if (roll < 0.68) {
        // Confirmation paiement (fournisseur) sur un paiement créé par la VU.
        const pid = pick(myPayments);
        if (pid) {
          const r = await req('POST', `/api/payments/${pid}/confirm`, supTok, {}, stats, 'pay.confirm');
          if (r.status === 401) supTok = await login(SUPPLIER_USER, SUPPLIER_PASS, stats);
        } else {
          await req('GET', `/api/payments/supplier-summary?supplierId=${shared.supplierId}`, supTok, null, stats, 'pay.summary');
        }
      } else if (roll < 0.80) {
        // Commande : création (+ confirm fournisseur occasionnel).
        if (shared.productId) {
          const r = await req('POST', '/api/orders', shopTok, {
            supplierId: shared.supplierId, shopId: shared.shopId, asapPayment: false,
            currency: 'TND', notes: 'STRESS',
            items: [{ productId: shared.productId, quantity: 1 + rand(3), discount: 0 }],
          }, stats, 'order.create');
          if ((r.status === 200 || r.status === 201) && r.body?.id) {
            myOrders.push(r.body.id);
            if (Math.random() < 0.4) {
              await req('POST', `/api/orders/${r.body.id}/confirm`, supTok, {}, stats, 'order.confirm');
            }
          } else if (r.status === 401) {
            shopTok = await login(SHOP_USER, SHOP_PASS, stats);
          }
        }
      } else if (roll < 0.90) {
        // Livraisons : mes livraisons (boutique) + détail commande occasionnel.
        await req('GET', '/api/orders/my-deliveries', shopTok, null, stats, 'delivery.mine');
        const oid = pick(myOrders);
        if (oid && Math.random() < 0.3) {
          await req('GET', `/api/orders/${oid}`, supTok, null, stats, 'order.get');
        }
      } else {
        // Soldes + notifications.
        if (Math.random() < 0.5) {
          await req('GET', `/api/balances/shop/${shared.shopId}`, shopTok, null, stats, 'balance.shop');
        } else {
          await req('GET', '/api/notifications/unread-count', shopTok, null, stats, 'notif.count');
        }
      }
    } catch (e) {
      record(stats, 0, false, `vu:EXC`, false);
    }
    if (RAMP_SECS > 0) await sleep(rand(100));
  }
}

async function main() {
  console.log(`[stress] base=${BASE} stages=${STAGES.join(',')} stageSecs=${STAGE_SECS}`);
  const bootStats = newStats();
  await boot(bootStats);
  const report = { base: BASE, startedAt: new Date().toISOString(), stages: [], boot: summarize(bootStats, 0) };
  const workers = [];
  let stageCtl = { running: true };
  for (const target of STAGES) {
    const stats = newStats();
    stageCtl = { running: true };
    // Monte en charge jusqu'à target VUs (démarrage étalé via RAMP_SECS dans chaque VU).
    while (workers.length < target) {
      const id = workers.length;
      workers.push(vuLoop(id, stageCtl, stats).catch(() => {}));
      if (workers.length % 500 === 0) await sleep(50);
    }
    console.log(`[stress] STAGE ${target} VUs pendant ${STAGE_SECS}s...`);
    const t0 = Date.now();
    await sleep(STAGE_SECS * 1000);
    stageCtl.running = false;
    const secs = (Date.now() - t0) / 1000;
    // Les VUs du stage terminent leur itération en cours ; on attend 5s max.
    await Promise.race([Promise.allSettled(workers.splice(0, workers.length)), sleep(5000)]);
    const sum = summarize(stats, secs);
    report.stages.push({ vus: target, secs: +secs.toFixed(1), ...sum });
    console.log(`[stress] stage ${target}: ${sum.requests} req, ${sum.rps}/s, p50=${sum.p50ms}ms p95=${sum.p95ms}ms p99=${sum.p99ms}ms fail=${sum.fail} (429:${sum.throttled429})`);
    if (sum.fail > 0) console.log(`[stress] erreurs: ${JSON.stringify(sum.errors)}`);
    await sleep(2000);
  }
  report.finishedAt = new Date().toISOString();
  mkdirSync(join(ROOT, 'reports'), { recursive: true });
  const stamp = new Date().toISOString().replace(/[:.]/g, '-');
  writeFileSync(join(ROOT, 'reports', `report-${stamp}.json`), JSON.stringify(report, null, 2));
  writeFileSync(join(ROOT, 'reports', `report-${stamp}.html`), renderHtml(report));
  console.log(`[stress] rapport: reports/report-${stamp}.json + .html`);
}

function renderHtml(r) {
  const rows = r.stages.map((s) => `<tr><td>${s.vus}</td><td>${s.requests}</td><td>${s.rps}</td><td>${s.p50ms}</td><td>${s.p95ms}</td><td>${s.p99ms}</td><td>${s.fail}</td><td>${s.throttled429}</td></tr>`).join('');
  const errs = r.stages.flatMap((s) => Object.entries(s.errors).map(([k, v]) => `<tr><td>${s.vus}</td><td>${k}</td><td>${v}</td></tr>`)).join('') || '<tr><td colspan="3">Aucune erreur</td></tr>';
  return `<!DOCTYPE html><html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Rapport stress ${r.startedAt}</title><style>
body{font-family:Segoe UI,Arial,sans-serif;background:#f0f9ff;color:#0c4a6e;margin:0;padding:24px}
h1{color:#0369a1}h2{color:#0284c7;margin-top:32px}.card{background:#fff;border-radius:12px;padding:20px;box-shadow:0 2px 8px rgba(2,132,199,.12);margin-bottom:20px;overflow-x:auto}
table{width:100%;border-collapse:collapse}th{background:#9FCBED;color:#0c4a6e;padding:10px}td{border:1px solid #bae6fd;padding:8px 10px;text-align:center}
.meta{color:#475569}</style></head><body><h1>Test de charge — Payment Platform</h1>
<p class="meta">Base ${r.base} · début ${r.startedAt} · fin ${r.finishedAt}</p>
<div class="card"><h2>Par palier (utilisateurs connectés)</h2><table><tr><th>VUs</th><th>Requêtes</th><th>req/s</th><th>p50 ms</th><th>p95 ms</th><th>p99 ms</th><th>Échecs</th><th>429</th></tr>${rows}</table></div>
<div class="card"><h2>Erreurs par palier</h2><table><tr><th>VUs</th><th>Clé</th><th>Nombre</th></tr>${errs}</table></div>
</body></html>`;
}

main().catch((e) => { console.error('[stress] FATAL', e); process.exit(1); });
