import { searchNodes } from "./normalize.js";
import { format, formatAll, isChance } from "./effects.js";

const $ = (sel) => document.querySelector(sel);

let nodes = [];
let byId = new Map();
let showRecommend = true;
let selectedId = null;

async function boot() {
  const err = $("#error");
  try {
    const res = await fetch("./encounters.json", { cache: "no-store" });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    nodes = data.nodes || [];
    if (!nodes.length) throw new Error("시드에 노드가 없습니다");
    byId = new Map(nodes.map((n) => [n.id, n]));
    err.hidden = true;
    $("#search").addEventListener("input", () => renderList());
    $("#recommend").addEventListener("change", (e) => {
      showRecommend = e.target.checked;
      if (selectedId) renderDetail(selectedId);
      renderList();
    });
    renderList();
  } catch (e) {
    err.hidden = false;
    err.textContent = `시드 로드 실패: ${e.message}. web/encounters.json 확인 후 sync-seed.ps1 실행, HTTP로 열어 주세요.`;
    $("#list").innerHTML = "";
    $("#detail").innerHTML = "";
  }
}

function renderList() {
  const q = $("#search").value;
  const hits = searchNodes(nodes, q);
  const ul = $("#list");
  ul.innerHTML = "";
  if (!hits.length) {
    ul.innerHTML = `<li class="empty">결과 없음</li>`;
    return;
  }
  for (const n of hits) {
    const li = document.createElement("li");
    if (n.id === selectedId) li.classList.add("active");
    li.innerHTML = `<strong>${escapeHtml(n.title)}</strong><span class="story">${escapeHtml(n.story || "")}</span>`;
    li.addEventListener("click", () => {
      selectedId = n.id;
      renderList();
      renderDetail(n.id);
    });
    ul.appendChild(li);
  }
}

function renderDetail(id) {
  const node = byId.get(id);
  const pane = $("#detail");
  if (!node) {
    pane.innerHTML = `<p class="empty">노드를 찾을 수 없습니다</p>`;
    return;
  }
  const choices = (node.choices || [])
    .map((c) => {
      const rec = showRecommend && c.recommend ? " recommend" : "";
      const effectsHtml = (c.effects || [])
        .map((e) => {
          const cls = isChance(e) ? "chance" : "";
          return `<div class="effects ${cls}">${escapeHtml(format(e))}</div>`;
        })
        .join("");
      const next =
        c.nextId && byId.has(c.nextId)
          ? `<a class="next" href="#" data-next="${escapeAttr(c.nextId)}">다음: ${escapeHtml(byId.get(c.nextId).title)} →</a>`
          : "";
      return `<div class="choice${rec}"><div class="text">${escapeHtml(c.text)}</div>${effectsHtml || `<div class="effects">${escapeHtml(formatAll(c.effects))}</div>`}${next}</div>`;
    })
    .join("");

  pane.innerHTML = `
    <p class="muted">${escapeHtml(node.story || "")}</p>
    <h2>${escapeHtml(node.title)}</h2>
    ${node.body ? `<p>${escapeHtml(node.body)}</p>` : ""}
    ${choices || `<p class="empty">선택지 없음</p>`}
    ${node.source ? `<p class="muted">출처: ${escapeHtml(node.source)}</p>` : ""}
  `;
  pane.querySelectorAll("a.next").forEach((a) => {
    a.addEventListener("click", (ev) => {
      ev.preventDefault();
      const nid = a.getAttribute("data-next");
      selectedId = nid;
      renderList();
      renderDetail(nid);
    });
  });
}

function escapeHtml(s) {
  return String(s ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function escapeAttr(s) {
  return escapeHtml(s).replace(/'/g, "&#39;");
}

boot();
