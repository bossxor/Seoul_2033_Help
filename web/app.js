/* global S2033 */
(function () {
  var $ = function (sel) { return document.querySelector(sel); };

  var nodes = [];
  var byId = {};
  var showRecommend = true;
  var selectedId = null;

  function boot() {
    var err = $("#error");
    try {
      var seed = (window.S2033 && window.S2033.SEED) || null;
      if (!seed || !seed.nodes || !seed.nodes.length) {
        throw new Error("임베드 시드 없음 — scripts/sync-seed.ps1 실행");
      }
      nodes = seed.nodes;
      byId = {};
      for (var i = 0; i < nodes.length; i++) byId[nodes[i].id] = nodes[i];
      err.hidden = true;
      $("#search").addEventListener("input", renderList);
      $("#recommend").addEventListener("change", function (e) {
        showRecommend = e.target.checked;
        if (selectedId) renderDetail(selectedId);
        renderList();
      });
      renderList();
    } catch (e) {
      err.hidden = false;
      err.textContent = "시드 로드 실패: " + e.message;
      $("#list").innerHTML = "";
      $("#detail").innerHTML = "";
    }
  }

  function renderList() {
    var q = $("#search").value;
    var hits = S2033.searchNodes(nodes, q);
    var ul = $("#list");
    ul.innerHTML = "";
    if (!hits.length) {
      ul.innerHTML = '<li class="empty">결과 없음</li>';
      return;
    }
    hits.forEach(function (n) {
      var li = document.createElement("li");
      if (n.id === selectedId) li.classList.add("active");
      li.innerHTML =
        "<strong>" + escapeHtml(n.title) + '</strong><span class="story">' +
        escapeHtml(n.story || "") + "</span>";
      li.addEventListener("click", function () {
        selectedId = n.id;
        renderList();
        renderDetail(n.id);
      });
      ul.appendChild(li);
    });
  }

  function renderDetail(id) {
    var node = byId[id];
    var pane = $("#detail");
    if (!node) {
      pane.innerHTML = '<p class="empty">노드를 찾을 수 없습니다</p>';
      return;
    }
    var choices = (node.choices || [])
      .map(function (c) {
        var rec = showRecommend && c.recommend ? " recommend" : "";
        var effectsHtml = (c.effects || [])
          .map(function (e) {
            var tone = S2033.tone(e);
            return '<div class="effects tone-' + tone + '">' + escapeHtml(S2033.format(e)) + "</div>";
          })
          .join("");
        var next = "";
        if (c.nextId && byId[c.nextId]) {
          next =
            '<a class="next" href="#" data-next="' +
            escapeAttr(c.nextId) +
            '">다음: ' +
            escapeHtml(byId[c.nextId].title) +
            " →</a>";
        }
        return (
          '<div class="choice' +
          rec +
          '"><div class="text">' +
          escapeHtml(c.text) +
          "</div>" +
          (effectsHtml || "") +
          next +
          "</div>"
        );
      })
      .join("");

    pane.innerHTML =
      '<p class="muted">' +
      escapeHtml(node.story || "") +
      "</p><h2>" +
      escapeHtml(node.title) +
      "</h2>" +
      (node.body ? "<p>" + escapeHtml(node.body) + "</p>" : "") +
      (choices || '<p class="empty">선택지 없음</p>');

    pane.querySelectorAll("a.next").forEach(function (a) {
      a.addEventListener("click", function (ev) {
        ev.preventDefault();
        selectedId = a.getAttribute("data-next");
        renderList();
        renderDetail(selectedId);
      });
    });
  }

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function escapeAttr(s) {
    return escapeHtml(s).replace(/'/g, "&#39;");
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", boot);
  } else {
    boot();
  }
})();
