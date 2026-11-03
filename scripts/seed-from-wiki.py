#!/usr/bin/env python3
"""Heuristic wiki text -> EncounterSeedFile JSON draft.

Usage:
  python scripts/seed-from-wiki.py
  python scripts/seed-from-wiki.py --merge-curated data/encounters.json --out data/encounters.json --batch1
"""
from __future__ import annotations

import argparse
import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "data" / "raw" / "basic-encounters-readonly.txt"
DRAFT = ROOT / "data" / "encounters.draft.json"
TODAY = "2026-11-03"
SOURCE = "namu:기본 인카운터"
STORY = "기본"

SECTION_RE = re.compile(r"^###\s+(\d+(?:\.\d+)*)\.\s+(.+?)\s*$")
BULLET_RE = re.compile(r"^-\s+(.+)$")
# choice - outcome  (first " - " that looks like separator after choice)
SPLIT_CHOICE = re.compile(r"^(.+?)\s+-\s+(.+)$")

RESOURCE_NAMES = ("체력", "멘탈", "돈", "식량")
# Batch-1: simpler / short encounters by section number prefix
BATCH1_PREFIXES = {
    "2.1", "2.8", "2.9", "2.12", "2.13", "2.14", "2.15", "2.16", "2.17",
    "2.26", "2.27", "2.28", "2.29", "2.30", "2.37", "2.38", "2.40", "2.42",
    "2.48", "2.49", "2.50", "2.51", "2.61", "2.65", "2.72", "2.73", "2.77",
    "2.83", "2.85", "2.89", "2.91", "2.92", "2.93", "2.98", "2.104", "2.110",
}


def slugify(title: str) -> str:
    t = unicodedata.normalize("NFKC", title).strip().lower()
    t = re.sub(r"[/·．.]+", "-", t)
    t = re.sub(r"[^\w가-힣\-]+", "-", t, flags=re.UNICODE)
    t = re.sub(r"-+", "-", t).strip("-")
    if not t:
        t = "node"
    # Prefer latin-ish ids when Hangul-only: keep hangul (JSON ok) but Android/search fine
    return t[:64]


def parse_effects(outcome: str) -> list[dict]:
    if not outcome or not outcome.strip():
        return [{"type": "text", "name": "결과 미상"}]
    o = outcome.strip()
    effects: list[dict] = []

    # conditions in parentheses on choice side are handled elsewhere; here outcomes
    for m in re.finditer(r"(체력|멘탈|돈|식량)\s*(\d+)\s*칸", o):
        name, n = m.group(1), int(m.group(2))
        delta = n if re.search(rf"{name}.*(?:회복|얻|채)", o) or "회복" in o else -n
        if re.search(r"(깎|소모|잃|감소)", o):
            delta = -abs(n)
        if re.search(r"(회복|채우|얻)", o) and not re.search(r"(깎|소모|잃)", o):
            delta = abs(n)
        effects.append({"type": "resource", "name": name, "delta": delta})

    # "체력 1칸이 깎" patterns without "칸" duplication already caught; also "체력이 1칸"
    for m in re.finditer(r"(체력|멘탈|돈)\s*(?:이\s*)?(\d+)\s*칸\s*(?:이\s*)?(깎|소모|잃)", o):
        effects.append({"type": "resource", "name": m.group(1), "delta": -int(m.group(2))})

    for m in re.finditer(r"(\d+)%\s*확률", o):
        rate = int(m.group(1)) / 100.0
        effects.append({
            "type": "chance",
            "successRate": rate,
            "success": [{"type": "text", "name": "성공 (위키 서술 참고)"}],
            "failure": [{"type": "text", "name": "실패 (위키 서술 참고)"}],
        })

    for m in re.finditer(r"([가-힣A-Za-z0-9]+)\s*상태이상", o):
        effects.append({"type": "status", "name": m.group(1)})

    # X 획득
    for m in re.finditer(r"([가-힣A-Za-z0-9 ·<>]+?)\s*획득", o):
        name = m.group(1).strip(" ·")
        name = re.sub(r"^.*?(?:경우|때|시)\s*", "", name)
        name = re.sub(r"^.*깎이고\s*", "", name)
        if not name or name in RESOURCE_NAMES or len(name) > 40:
            continue
        if name in ("체력", "멘탈", "돈") or "상태이상" in name:
            continue
        # ability-ish short names vs items
        if any(k in name for k in ("술", "힘", "지식", "재능", "기술", "얼굴", "투지", "직감")) or len(name) <= 6:
            effects.append({"type": "ability", "name": name})
        else:
            effects.append({"type": "item", "name": name})

    if re.search(r"인카운터 종료|지나|무시|통과", o) and not effects:
        effects.append({"type": "text", "name": o[:80]})

    if not effects:
        effects.append({"type": "text", "name": o[:120]})

    # de-dupe identical dicts
    seen = set()
    uniq = []
    for e in effects:
        key = json.dumps(e, ensure_ascii=False, sort_keys=True)
        if key not in seen:
            seen.add(key)
            uniq.append(e)
    return uniq


def extract_condition(choice_text: str) -> tuple[str, list[dict]]:
    """Pull trailing (조건) into condition effects."""
    effects: list[dict] = []
    text = choice_text.strip()
    m = re.search(r"^(.*?)\(([^)]+)\)\s*$", text)
    if m:
        text = m.group(1).strip()
        cond = m.group(2).strip()
        if cond and cond not in ("일반판", "후원자"):
            effects.append({"type": "condition", "name": cond})
    return text, effects


def parse_sections(text: str) -> list[dict]:
    lines = text.splitlines()
    # skip until first real encounter section after 개요 (### 2.1.)
    sections: list[tuple[str, str, list[str]]] = []
    cur_num = cur_title = None
    buf: list[str] = []

    def flush():
        nonlocal cur_num, cur_title, buf
        if cur_num and cur_title:
            sections.append((cur_num, cur_title, buf))
        buf = []

    for line in lines:
        m = SECTION_RE.match(line)
        if m:
            flush()
            cur_num, cur_title = m.group(1), m.group(2).strip()
            continue
        if cur_num is None:
            continue
        buf.append(line)
    flush()

    nodes = []
    for num, title, body_lines in sections:
        # skip deep nested like 2.19.1 unless we want them — keep only depth <= 2 for batch (2.x)
        parts = num.split(".")
        if len(parts) > 2:
            continue
        if not num.startswith("2."):
            continue

        body_paras = []
        choices_raw: list[tuple[str, str]] = []
        for line in body_lines:
            bm = BULLET_RE.match(line.strip())
            if bm:
                content = bm.group(1).strip()
                # skip pure category headers / empty
                if content.startswith("<") and " - " not in content and "획득" in content:
                    # library book style: treat as choice
                    sm = SPLIT_CHOICE.match(content)
                    if sm:
                        choices_raw.append((sm.group(1).strip(), sm.group(2).strip()))
                    else:
                        choices_raw.append((content, content))
                    continue
                sm = SPLIT_CHOICE.match(content)
                if sm:
                    choices_raw.append((sm.group(1).strip(), sm.group(2).strip()))
                else:
                    choices_raw.append((content, ""))
            else:
                s = line.strip()
                if s and not s.startswith("|") and not s.startswith("'''"):
                    # strip light wiki bold
                    s = s.replace("'''", "")
                    body_paras.append(s)

        body = " ".join(body_paras[:3])[:400] if body_paras else title

        # Fold orphan outcome-bullets into previous choice when wiki nests poorly
        folded: list[tuple[str, str]] = []
        outcomeish = re.compile(
            r"(깎|소모|잃|회복|획득|확률|통과|종료|멘탈|체력|돈\s*\d|즉사|사라)"
        )
        for ct, outcome in choices_raw:
            if (
                folded
                and not outcome
                and outcomeish.search(ct)
                and not re.search(r"(한다|간다|본다|판다|산다|먹는다|쏜다|포기|무시|지나)", ct)
            ):
                prev_t, prev_o = folded[-1]
                joined = (prev_o + " / " + ct).strip(" /")
                folded[-1] = (prev_t, joined)
                continue
            folded.append((ct, outcome))

        choices = []
        for ct, outcome in folded:
            text, cond_fx = extract_condition(ct)
            if not text or len(text) > 80:
                continue
            # skip nested narrative fragments that aren't real choices
            if text.startswith("지뢰밭일 경우") or text.startswith("구호 물품일 경우"):
                continue
            fx = cond_fx + parse_effects(outcome)
            choices.append({
                "text": text,
                "effects": fx,
                "nextId": None,
                "recommend": False,
            })

        # pickup with no bullets: synthesize
        if not choices:
            choices = [
                {
                    "text": "살펴본다",
                    "effects": [{"type": "text", "name": body[:100] or "위키 서술 참고"}],
                    "nextId": None,
                    "recommend": True,
                },
                {
                    "text": "지나친다",
                    "effects": [{"type": "text", "name": "지나감"}],
                    "nextId": None,
                    "recommend": False,
                },
            ]

        nid = slugify(title)
        nodes.append({
            "id": nid,
            "title": title,
            "story": STORY,
            "body": body or None,
            "choices": choices,
            "source": SOURCE,
            "updatedAt": TODAY,
            "_section": num,
        })
    return nodes


def load_curated(path: Path) -> list[dict]:
    if not path.exists():
        return []
    data = json.loads(path.read_text(encoding="utf-8"))
    return data.get("nodes", [])


def apply_batch1(nodes: list[dict]) -> list[dict]:
    out = []
    for n in nodes:
        sec = n.get("_section", "")
        if any(sec == p or sec.startswith(p + ".") for p in BATCH1_PREFIXES):
            out.append(n)
    return out


def strip_meta(nodes: list[dict]) -> list[dict]:
    clean = []
    for n in nodes:
        c = {k: v for k, v in n.items() if not k.startswith("_")}
        clean.append(c)
    return clean


def merge_by_id(primary: list[dict], secondary: list[dict]) -> list[dict]:
    """primary wins on id collision; drop secondary titles that match primary titles."""
    primary_titles = {n["title"] for n in primary}
    filtered_secondary = [n for n in secondary if n["title"] not in primary_titles]
    by_id = {}
    order = []
    for n in filtered_secondary + primary:
        i = n["id"]
        if i not in by_id:
            order.append(i)
        by_id[i] = n
    return [by_id[i] for i in order]


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--raw", type=Path, default=RAW)
    ap.add_argument("--draft", type=Path, default=DRAFT)
    ap.add_argument("--out", type=Path, default=None)
    ap.add_argument("--merge-curated", type=Path, default=None)
    ap.add_argument("--batch1", action="store_true")
    args = ap.parse_args()

    text = args.raw.read_text(encoding="utf-8")
    nodes = parse_sections(text)
    args.draft.parent.mkdir(parents=True, exist_ok=True)
    draft_nodes = strip_meta(nodes)
    args.draft.write_text(
        json.dumps({"version": 1, "nodes": draft_nodes}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"draft: {len(draft_nodes)} nodes -> {args.draft}")

    if args.out is None:
        return

    selected = apply_batch1(nodes) if args.batch1 else nodes
    selected = strip_meta(selected)
    if args.merge_curated:
        curated = load_curated(args.merge_curated)
        # curated wins
        selected = merge_by_id(curated, selected)
    # ensure unique titles keep wild-dogs curated structure
    payload = {"version": 1, "nodes": selected}
    args.out.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"out: {len(selected)} nodes -> {args.out}")


if __name__ == "__main__":
    main()
