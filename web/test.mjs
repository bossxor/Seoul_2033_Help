import { describe, it } from "node:test";
import assert from "node:assert/strict";
import { normalize, searchNodes } from "./normalize.js";
import { format } from "./effects.js";

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
    assert.match(s, /능력: 날렵함/);
    assert.match(s, /체력: -1/);
  });
});
