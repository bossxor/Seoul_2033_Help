/** Mirror of Android TextNormalizer: keep letters/digits, lowercase, concat. */
export function normalize(s) {
  if (!s) return "";
  const m = String(s).toLowerCase().match(/[\p{L}\p{N}]+/gu);
  return m ? m.join("") : "";
}

export function searchNodes(nodes, query) {
  const q = normalize(query);
  if (!q) return nodes;
  return nodes.filter((node) => {
    if (normalize(node.title).includes(q)) return true;
    if (normalize(node.story || "").includes(q)) return true;
    return (node.choices || []).some((c) => normalize(c.text).includes(q));
  });
}
