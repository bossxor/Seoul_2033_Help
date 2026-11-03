(function (root, factory) {
  var api = factory();
  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  } else {
    root.S2033 = root.S2033 || {};
    root.S2033.format = api.format;
    root.S2033.formatAll = api.formatAll;
    root.S2033.isChance = api.isChance;
  }
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  function formatAll(effects) {
    if (!effects || !effects.length) return "";
    return effects.map(format).join(" · ");
  }

  function format(effect) {
    if (!effect || !effect.type) return "";
    switch (effect.type) {
      case "item":
        return "아이템: " + effect.name + (effect.delta != null ? " (" + effect.delta + ")" : "");
      case "ability":
        return "능력: " + effect.name;
      case "status":
        return "상태: " + effect.name;
      case "resource":
        return effect.name + ": " + effect.delta;
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
      case "ability":
        return "good";
      case "status":
        return "bad";
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
