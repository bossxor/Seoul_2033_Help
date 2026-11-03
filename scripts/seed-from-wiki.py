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

# ### and #### (nested 2.19.1 …) both start a new encounter section
SECTION_RE = re.compile(r"^#{2,6}\s+(\d+(?:\.\d+)*)\.\s+(.+?)\s*$")
BULLET_RE = re.compile(r"^-\s+(.+)$")
# choice - outcome  (first " - " that looks like separator after choice)
SPLIT_CHOICE = re.compile(r"^(.+?)\s+-\s+(.+)$")

RESOURCE_NAMES = ("체력", "멘탈", "돈", "식량")
ABILITY_HINTS = (
    "술", "힘", "지식", "재능", "기술", "얼굴", "투지", "직감", "은신",
    "실력", "영어", "철학", "유머", "거짓말", "교섭", "웅변", "유도", "소매치기", "사격", "관찰력", "날렵함",
)
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


def split_sentences(text: str) -> list[str]:
    parts = re.split(r"(?<=[.!?])\s+|\s+/\s+", text.strip())
    return [p.strip(" ./-") for p in parts if p.strip(" ./-")]


def make_chance(rate: float, success: str, failure: str) -> dict:
    ok = parse_effects(success) if success else []
    bad = parse_effects(failure) if failure else []
    return {
        "type": "chance",
        "successRate": rate,
        "success": ok or [{"type": "text", "name": "성공 (위키 서술 참고)"}],
        "failure": bad or [{"type": "text", "name": "실패 (위키 서술 참고)"}],
    }


def split_chance(o: str) -> tuple[str, dict] | None:
    """'N% 확률로 A. 아니면 B' -> (앞 문장, chance). 첫 확률 문장 = 성공, 이후 = 실패."""
    sents = split_sentences(o)
    for i, s in enumerate(sents):
        m = re.search(r"(\d+)%\s*확률(?:로|이다|로는)?", s)
        if not m:
            continue
        head = s[: m.start()].strip(" ,")
        succ = (head + " " + s[m.end():]).strip(" ,")
        succ = re.sub(r"^(?:잘\s*)?(?:구울|만들|성공할)\s*경우\s*", "", succ)
        pre = " ".join(sents[:i])
        return pre, make_chance(int(m.group(1)) / 100.0, succ, " ".join(sents[i + 1:]))
    return None


def parse_effects(outcome: str) -> list[dict]:
    if not outcome or not outcome.strip():
        return []
    # drop namu footnote markers "[1]" only (keep the surrounding sentence)
    o = re.sub(r"\s*\[\d+\]", "", outcome.strip()).strip(" -")
    chance = split_chance(o)
    if chance:
        pre, effect = chance
        return (parse_effects(pre) if pre else []) + [effect]
    effects: list[dict] = []

    # 퀘스트/인카운터 진입 → 중립 텍스트 (이득 아님)
    if re.search(r"(퀘스트|인카운터).{0,8}진입|진입$", o) and not re.search(
        r"(획득|습득|소모|깎|회복)", o
    ):
        return [{"type": "text", "name": o[:80]}]

    # [관찰력]이 있으면 … / [수배중] 가젯
    for m in re.finditer(r"\[([^\]]+)\](?:이|가)?\s*있으면", o):
        effects.append({"type": "condition", "name": m.group(1).strip()})
    for m in re.finditer(r"\[([^\]]+)\]\s*가젯", o):
        name = m.group(1).strip().replace("수배중", "수배 중")
        effects.append({"type": "condition", "name": name})

    # (+ 돈 1, 영어) / (- 체력 1) compact wiki form
    for m in re.finditer(r"\(\s*([+\-＋－])\s*([^)]+)\)", o):
        sign = 1 if m.group(1) in "+＋" else -1
        for piece in re.split(r"\s*,\s*", m.group(2)):
            piece = piece.strip()
            if not piece:
                continue
            rm = re.match(r"(체력|멘탈|돈|식량)\s*(\d+)$", piece)
            if rm:
                effects.append(
                    {
                        "type": "resource",
                        "name": rm.group(1),
                        "delta": sign * int(rm.group(2)),
                    }
                )
                continue
            name = re.sub(r"\s+", " ", piece)
            name = name.replace("수배중", "수배 중")
            if "수배" in name or name in ("출혈", "감염", "골절") or "적대" in name or "우호" in name:
                effects.append({"type": "status", "name": name, "delta": sign})
            elif sign > 0 and (
                any(k in name for k in ABILITY_HINTS)
                or name.endswith(("술", "재주", "기술", "영어"))
                or len(name) <= 8
            ):
                if name.endswith(("술", "재주", "기술", "영어")) or any(
                    k in name for k in ABILITY_HINTS
                ):
                    effects.append({"type": "ability", "name": name})
                else:
                    effects.append({"type": "item", "name": name})
            elif sign < 0:
                effects.append({"type": "status", "name": name, "delta": -1})
            else:
                effects.append({"type": "item", "name": name})

    # 멘탈을 1칸 소모 / 체력 2칸 회복
    for m in re.finditer(
        r"(체력|멘탈|돈|식량)(?:을|를|이|가)?\s*(\d+)\s*칸", o
    ):
        name, n = m.group(1), int(m.group(2))
        window = o[max(0, m.start() - 2) : m.end() + 12]
        if re.search(r"(깎|소모|잃|감소|까인|까이|까임)", window) or re.search(
            r"(깎|소모|잃|감소|까인|까이|까임)", o[m.end() : m.end() + 8]
        ):
            delta = -abs(n)
        elif re.search(r"(회복|채우)", window) or re.search(
            r"(회복|채우)", o[m.end() : m.end() + 8]
        ):
            delta = abs(n)
        else:
            # default: "소모하며" later in sentence → lose; "획득" alone → keep heuristic
            delta = -abs(n) if re.search(r"(깎|소모|잃|감소|까인|까이|까임)", o) else abs(n)
            if re.search(r"(회복|채우)", o) and not re.search(r"(깎|소모|잃)", o):
                delta = abs(n)
        effects.append({"type": "resource", "name": name, "delta": delta})

    for m in re.finditer(r"(\d+)%\s*확률", o):
        effects.append(make_chance(int(m.group(1)) / 100.0, "", ""))

    for m in re.finditer(r"([가-힣A-Za-z0-9]+)\s*상태이상", o):
        if m.group(1) in ("똑같이", "모든", "모두", "어떤", "각종"):
            continue
        effects.append({"type": "status", "name": m.group(1), "delta": 1})

    # 수배 중 삭제 / X 소멸 → status delta -1
    for m in re.finditer(
        r"([가-힣A-Za-z0-9 ·]+?)\s*(?:삭제|소멸)(?!\s*하며)", o
    ):
        name = m.group(1).strip(" ·'")
        name = re.sub(r"^(?:및|그리고|또는)\s*", "", name)
        if not name or name in RESOURCE_NAMES or len(name) > 24:
            continue
        if name in ("근력", "체력"):
            continue
        effects.append({"type": "status", "name": name, "delta": -1})

    def strip_josa(name: str) -> str:
        name = name.strip(" ·'")
        name = re.sub(r"[을를이가은는]$", "", name)
        return name.strip()

    def append_gain(name: str) -> None:
        name = strip_josa(name)
        name = re.sub(r"\s+", " ", name)
        if not name or name in RESOURCE_NAMES or len(name) > 24:
            return
        if re.search(r"(소모|칸|확률|종료|통과|진입|퀘스트)", name):
            return
        if any(e.get("name") == name for e in effects):
            return
        if "수배" in name or "평판" in name or name == "악명":
            effects.append({"type": "status", "name": name, "delta": 1})
        elif (
            any(k in name for k in ABILITY_HINTS)
            or name.endswith("술")
            or name.endswith("재주")
        ):
            effects.append({"type": "ability", "name": name})
        else:
            effects.append({"type": "item", "name": name})

    def append_gains(blob: str) -> None:
        blob = re.sub(r"^.*?(?:하며|면서)\s*", "", blob.strip())
        for part in re.split(r"\s*(?:와|과|및|/)\s*", blob):
            append_gain(part)

    # X(을) 획득/습득/얻는다 — 조사 분리, 와/과 복수
    for m in re.finditer(
        r"([가-힣A-Za-z0-9 ·]{2,40}?)\s*(?:을|를)?\s*(?:획득|습득|얻는다|얻음)", o
    ):
        blob = m.group(1).strip()
        # "멘탈 … 소모하면서 의약품" → 의약품만
        blob = re.sub(r"^.*?(?:하며|면서)\s*", "", blob)
        if re.search(r"(체력|멘탈|돈|식량).{0,6}\d|칸\s*(?:을\s*)?(?:소모|깎)", blob):
            continue
        append_gains(blob)

    if re.search(r"인카운터 종료|지나|무시|통과", o) and not effects:
        effects.append({"type": "text", "name": "인카운터 종료" if "종료" in o else o[:80]})

    # bare loot noun: "군복" / "살충제, …대체" / "손재주"
    if not effects and re.fullmatch(r"[가-힣A-Za-z0-9 ·,/]{1,80}", o):
        if not re.search(r"(종료|통과|무시|확률|소모|깎|실패|죽|요구|강요)", o):
            head, _, tail = o.partition(",")
            append_gain(head.strip())
            if tail.strip() and effects:
                effects.append({"type": "text", "name": tail.strip()[:100]})

    if not effects:
        for s in split_sentences(o)[:4]:
            effects.append({"type": "text", "name": s[:100]})

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
    """Pull (조건) and [조건] into condition effects; strip footnote [N]."""
    effects: list[dict] = []
    text = choice_text.strip().replace("'''", "")
    text = re.sub(r"\[\d+\]", "", text)

    for m in re.finditer(r"\[([^\]]+)\]", text):
        inner = m.group(1).strip()
        if not inner or re.fullmatch(r"\d+", inner):
            continue
        for piece in re.split(r"\s*,\s*", inner):
            piece = piece.strip()
            if piece and piece not in ("일반판", "후원자"):
                effects.append({"type": "condition", "name": piece})
    text = re.sub(r"\s*\[[^\]]+\]", "", text)

    m = re.search(r"^(.*?)\(([^)]+)\)\s*$", text)
    if m:
        text = m.group(1).strip()
        cond = m.group(2).strip()
        if cond and cond not in ("일반판", "후원자"):
            effects.append({"type": "condition", "name": cond})

    text = re.sub(r"\s+", " ", text).strip(" .")
    return text, effects


def parse_circle_choices(blob: str) -> tuple[str | None, list[tuple[str, str]]]:
    """Split wiki ○/● inline choice lists. Return (intro, choices)."""
    if "○" not in blob and "●" not in blob:
        return None, []
    m = re.search(r"[○●]", blob)
    intro = blob[: m.start()].strip() or None
    rest = blob[m.start() :]
    choices: list[tuple[str, str]] = []
    for part in re.split(r"[○●]\s*", rest):
        part = part.strip().replace("'''", "")
        if not part:
            continue
        sm = SPLIT_CHOICE.match(part)
        if sm:
            ct, oc = sm.group(1).strip(), sm.group(2).strip()
        else:
            ct, oc = part, ""
        # footnotes only on outcome tail (do not eat [역사학] on choice)
        oc = re.sub(r"\s*\[\d+\]", "", oc).strip()
        ct = re.sub(r"\s+", " ", ct).strip(" .")
        if ct and len(ct) <= 80:
            choices.append((ct, oc))
    return intro, choices


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
        if not num.startswith("2."):
            continue

        body_paras = []
        choices_raw: list[tuple[str, str]] = []
        group_start = 0  # first bullet of the current contiguous bullet group
        after_bullet = False
        for line in body_lines:
            # nested heading leaked as body (belt-and-suspenders; SECTION_RE should catch)
            if re.match(r"^#{2,6}\s+\d+", line.strip()):
                continue
            bm = BULLET_RE.match(line.strip())
            if bm and not after_bullet:
                group_start = len(choices_raw)
            if bm:
                after_bullet = True
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
                if (
                    s
                    and after_bullet
                    and len(choices_raw) - group_start == 1
                    and not choices_raw[-1][1]
                    and not s.startswith("'''")
                ):
                    # "- 스테이크를 굽는다(생고기)" + 다음 문단 = 그 선택지의 결과
                    choices_raw[-1] = (choices_raw[-1][0], s.replace("'''", ""))
                    after_bullet = False
                    continue
                if s:
                    after_bullet = False
                if s and not s.startswith("|") and not s.startswith("'''"):
                    # strip light wiki bold
                    s = s.replace("'''", "")
                    if re.match(r"^#{2,6}\s+\d+", s):
                        continue
                    body_paras.append(s)

        joined_body = " ".join(body_paras)
        intro_circle, circle_choices = parse_circle_choices(joined_body)
        if circle_choices and not choices_raw:
            choices_raw = circle_choices
            body = (intro_circle or "")[:400] or None
        else:
            body = joined_body[:400] if body_paras else None
            # strip circle markers from body preview if bullets already present
            if body and ("○" in body or "●" in body) and intro_circle:
                body = intro_circle[:400]

        # Fold orphan outcome-bullets into previous choice when wiki nests poorly
        folded: list[tuple[str, str]] = []
        outcomeish = re.compile(
            r"(깎|소모|잃|회복|획득|확률|통과|종료|멘탈|체력|돈\s*\d|즉사|사라)"
        )
        branch = re.compile(r"(?:경우|판)\s*$")
        for ct, outcome in choices_raw:
            if folded and branch.search(ct.replace("'''", "")):
                prev_t, prev_o = folded[-1]
                label = ct.replace("'''", "").strip()
                folded[-1] = (prev_t, (prev_o + " / " + label + ": " + outcome).strip(" /"))
                continue
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
            fx = cond_fx + parse_effects(outcome)
            choices.append({
                "text": text,
                "effects": fx,
                "nextId": None,
                "recommend": False,
            })

        # circle fallback if bullets produced nothing useful
        if (not choices) and circle_choices:
            for ct, outcome in circle_choices:
                text, cond_fx = extract_condition(ct)
                if not text or len(text) > 80:
                    continue
                choices.append({
                    "text": text,
                    "effects": cond_fx + parse_effects(outcome),
                    "nextId": None,
                    "recommend": False,
                })
            if intro_circle:
                body = intro_circle[:400]

        # pickup with no bullets: synthesize
        if not choices:
            choices = [
                {
                    "text": "살펴본다",
                    "effects": [{"type": "text", "name": (body or title)[:100] or "위키 서술 참고"}],
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
