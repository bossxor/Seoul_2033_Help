(function (root, factory) {
  var api = factory();
  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  }
  // Always expose on window for browser (some envs define `module`)
  root.S2033 = root.S2033 || {};
  root.S2033.format = api.format;
  root.S2033.formatAll = api.formatAll;
  root.S2033.isChance = api.isChance;
  root.S2033.tone = api.tone;
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  function formatAll(effects) {
    if (!effects || !effects.length) return "";
    return effects.map(format).join(" · ");
  }

  function format(effect) {
    if (!effect || !effect.type) return "";
    switch (effect.type) {
      case "item":
        var d = effect.delta == null ? 1 : effect.delta | 0;
        return (d < 0 ? "- " : "+ ") + effect.name + (Math.abs(d) > 1 ? " ×" + Math.abs(d) : "");
      case "ability":
        return "+ " + effect.name;
      case "status": {
        var sd = effect.delta == null ? 1 : (effect.delta | 0);
        return (sd >= 0 ? "+ " : "- ") + effect.name;
      }
      case "resource": {
        var d = effect.delta | 0;
        return effect.name + " " + (d > 0 ? "+" + d : String(d));
      }
      case "text":
        return effect.name;
      case "condition":
        return "조건: " + effect.name;
      case "chance": {
        var pct = Math.round((effect.successRate || 0) * 100);
        var ok = formatAll(effect.success) || "-";
        var bad = formatAll(effect.failure) || "-";
        return pct + "% 성공: " + ok + " / 실패: " + bad;
      }
      default:
        return JSON.stringify(effect);
    }
  }

  function isChance(effect) {
    return !!(effect && effect.type === "chance");
  }

  /** condition | good | bad | chance | neutral */
  function tone(effect) {
    if (!effect || !effect.type) return "neutral";
    switch (effect.type) {
      case "condition":
        return "condition";
      case "item":
        return (effect.delta == null ? 1 : effect.delta | 0) < 0 ? "bad" : "good";
      case "ability":
        return "good";
      case "status": {
        var sn = effect.name || "";
        var sd = effect.delta == null ? 1 : (effect.delta | 0);
        if (sd < 0) return "bad";
        if (/우호|좋은\s*평판/.test(sn)) return "good";
        return "bad";
      }
      case "resource":
        return (effect.delta | 0) >= 0 ? "good" : "bad";
      case "chance":
        return "chance";
      case "text": {
        var n = effect.name || "";
        if (/회복|획득|얻/.test(n)) return "good";
        if (/깎|잃|소모|실패|즉사|부상/.test(n)) return "bad";
        return "neutral";
      }
      default:
        return "neutral";
    }
  }

  return { format: format, formatAll: formatAll, isChance: isChance, tone: tone };
});
