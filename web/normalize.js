(function (root, factory) {
  var api = factory();
  if (typeof module !== "undefined" && module.exports) {
    module.exports = api;
  }
  root.S2033 = root.S2033 || {};
  root.S2033.normalize = api.normalize;
  root.S2033.searchNodes = api.searchNodes;
})(typeof globalThis !== "undefined" ? globalThis : this, function () {
  function normalize(s) {
    if (!s) return "";
    var m;
    try {
      m = String(s).toLowerCase().match(/[\p{L}\p{N}]+/gu);
    } catch (_) {
      m = String(s).toLowerCase().match(/[a-z0-9\u3131-\ud79d]+/gi);
    }
    return m ? m.join("") : "";
  }

  function searchNodes(nodes, query) {
    var q = normalize(query);
    if (!q) return nodes;
    return nodes.filter(function (node) {
      if (normalize(node.title).indexOf(q) !== -1) return true;
      if (normalize(node.story || "").indexOf(q) !== -1) return true;
      return (node.choices || []).some(function (c) {
        return normalize(c.text).indexOf(q) !== -1;
      });
    });
  }

  return { normalize: normalize, searchNodes: searchNodes };
});
