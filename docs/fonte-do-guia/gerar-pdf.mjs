// Abre o guia.html no Edge headless e imprime em PDF (A4, com rodapé numerado).
import { spawn } from 'node:child_process';
import { writeFileSync, mkdtempSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const [entrada, saida] = process.argv.slice(2);
const EDGE = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
const PORT = 9335;
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

const edge = spawn(EDGE, ['--headless=new', '--disable-gpu', `--remote-debugging-port=${PORT}`,
  `--user-data-dir=${mkdtempSync(join(tmpdir(), 'edge-'))}`, 'about:blank'], { stdio: 'ignore' });
let wsUrl;
for (let i = 0; i < 50 && !wsUrl; i++) {
  await sleep(200);
  try { wsUrl = (await (await fetch(`http://127.0.0.1:${PORT}/json/list`)).json()).find((t) => t.type === 'page')?.webSocketDebuggerUrl; } catch { /* subindo */ }
}
const ws = new WebSocket(wsUrl);
await new Promise((r) => ws.addEventListener('open', r));
let id = 0;
const pend = new Map();
const eventos = [];
ws.addEventListener('message', (m) => {
  const x = JSON.parse(m.data);
  if (x.id && pend.has(x.id)) { pend.get(x.id)(x); pend.delete(x.id); } else if (x.method) eventos.push(x);
});
const send = (method, params = {}) => new Promise((r) => { const i = ++id; pend.set(i, r); ws.send(JSON.stringify({ id: i, method, params })); });

await send('Page.enable');
await send('Page.navigate', { url: pathToFileURL(resolve(entrada)).href });
for (let i = 0; i < 100 && !eventos.some((e) => e.method === 'Page.loadEventFired'); i++) await sleep(100);
await sleep(800);

const rodape = '<div style="font-family:Segoe UI,sans-serif;font-size:8px;color:#8a8f96;width:100%;padding:0 18mm;display:flex;justify-content:space-between">'
  + '<span>Denarco · Guia do projeto</span><span>página <span class="pageNumber"></span> de <span class="totalPages"></span></span></div>';
const r = await send('Page.printToPDF', {
  printBackground: true, preferCSSPageSize: true, displayHeaderFooter: true,
  headerTemplate: '<span></span>', footerTemplate: rodape
});
if (!r.result) { console.error(JSON.stringify(r)); process.exit(1); }
writeFileSync(saida, Buffer.from(r.result.data, 'base64'));
console.log('PDF gerado: ' + saida);
ws.close(); edge.kill(); process.exit(0);
