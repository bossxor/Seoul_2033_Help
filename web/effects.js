/** Mirror of Android EffectFormatter. */
export function formatAll(effects) {
  if (!effects || !effects.length) return "";
  return effects.map(format).join(" · ");
}

export function format(effect) {
  if (!effect || !effect.type) return "";
  switch (effect.type) {
    case "item":
      return "아이템: " + effect.name + (effect.delta != null ? ` (${effect.delta})` : "");
    case "ability":
      return "능력: " + effect.name;
    case "status":
      return "상태: " + effect.name;
    case "resource":
      return `${effect.name}: ${effect.delta}`;
    case "text":
      return effect.name;
    case "condition":
      return "조건: " + effect.name;
    case "chance": {
      const pct = Math.round((effect.successRate || 0) * 100);
      const ok = formatAll(effect.success) || "-";
      const bad = formatAll(effect.failure) || "-";
      return `${pct}% 성공: ${ok} / 실패: ${bad}`;
    }
    default:
      return JSON.stringify(effect);
  }
}

export function isChance(effect) {
  return effect && effect.type === "chance";
}
