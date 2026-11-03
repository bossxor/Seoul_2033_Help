import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const { normalize, searchNodes } = require("./normalize.js");
const { format } = require("./effects.js");

describe("normalize", () => {
  it("strips punctuation and lowercases", () => {
    assert.equal(normalize("들개 무리!"), "들개무리");
    assert.equal(normalize("ABC-123"), "abc123");
  });
});

describe("searchNodes", () => {
  const nodes = [
    { id: "a", title: "들개 무리", story: "기본", choices: [{ text: "지나친다" }] },
    { id: "b", title: "특이한 버섯", story: "기본", choices: [{ text: "먹는다" }] },
  ];
  it("finds by title", () => {
    assert.equal(searchNodes(nodes, "들개").length, 1);
  });
  it("finds by choice", () => {
    assert.equal(searchNodes(nodes, "지나친다").length, 1);
  });
  it("empty query returns all", () => {
    assert.equal(searchNodes(nodes, "").length, 2);
  });
});

describe("effects format", () => {
  it("formats chance percent", () => {
    const s = format({
      type: "chance",
      successRate: 0.44,
      success: [{ type: "ability", name: "날렵함" }],
      failure: [{ type: "resource", name: "체력", delta: -1 }],
    });
    assert.match(s, /^44% 성공:/);
    assert.match(s, /날렵함/);
    assert.match(s, /체력 -1/);
  });
  it("formats resource signed", () => {
    assert.equal(format({ type: "resource", name: "멘탈", delta: -1 }), "멘탈 -1");
  });
  it("formats status remove", () => {
    assert.equal(format({ type: "status", name: "수배 중", delta: -1 }), "- 수배 중");
  });
  it("formats item loss and count", () => {
    assert.equal(format({ type: "item", name: "음료", delta: -1 }), "- 음료");
    assert.equal(format({ type: "item", name: "소시지", delta: 2 }), "+ 소시지 ×2");
    assert.equal(S2033.tone({ type: "item", name: "음료", delta: -1 }), "bad");
  });
  it("formats item with plus", () => {
    assert.equal(format({ type: "item", name: "의약품" }), "+ 의약품");
  });
});
