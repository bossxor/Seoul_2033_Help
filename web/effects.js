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

  return { format: format, formatAll: formatAll, isChance: isChance };
});
