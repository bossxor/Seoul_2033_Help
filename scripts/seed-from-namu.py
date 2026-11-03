#!/usr/bin/env python3
r"""나무위키(현행 표기) 원문 -> EncounterSeedFile JSON 초안.

표기 규칙(현행 위키):
  #### 6.3.2. 제목       인카운터 (번호 있는 소제목)
  -                      선택지 시작 마커 (다음 줄이 선택지)
  [조건] 문구 + 획득 - 손실 (종료) (2로 이동)
  1) 조건, + 획득        직전 선택지의 조건부 결과 (빈 줄 뒤면 뒤따르는 선택지들의 그룹 조건)

Usage:
  python scripts/seed-from-namu.py data/raw/namu/기본\ 인카운터.txt --story 기본 --section 6 \\
      --out data/encounters.namu.json
"""
from __future__ import annotations

import argparse
import json
import re
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RAW = ROOT / "data" / "raw" / "namu"
TODAY = "2026-11-03"
RESOURCES = ("체력", "멘탈", "돈", "식량")

HEADING = re.compile(r"^#{2,6}\s+(\d+(?:\.\d+)*)\.\s+(.+?)\s*$")
BRANCH = re.compile(r"^(\d+(?:-\d+)*)\)\s*(.*)$")
GADGET_HEADING = re.compile(r'^#{3,6}\s+[\d.]+\s+"([^"]+)"')
END = re.compile(r"[\(\[]\s*(?:인카운터\s*)?종료\s*[\)\]]")
SCREEN = re.compile(r"^(\d+(?:-\d+)*)\.\s+(\S.*)$")  # 한 인카운터 안의 화면 구분 번호 줄
BARE_MOVE = re.compile(r"\s*(?:or|,)?\s*(\d+(?:-\d+)*)(?:으)?로\s*이동\s*$")
MOVE = re.compile(r"\(\s*([\d\-]+)\s*(?:으)?로\s*이동\s*\)")
LV = r"(?:\s*Lv\.?\s*[\d?□]+)?"


def slugify(title: str) -> str:
    t = unicodedata.normalize("NFKC", title).strip().lower()
    t = re.sub(r"[^\w가-힣\-]+", "-", t, flags=re.UNICODE)
    return re.sub(r"-+", "-", t).strip("-")[:64] or "node"


def load_vocab(raw: Path = RAW) -> dict[str, set[str]]:
    """가젯 문서의 `### 2.3. "근력" (...)` 제목에서 능력·아이템·상태 이름을 읽는다."""
    vocab: dict[str, set[str]] = {"ability": set(), "item": set(), "status": set()}
    for kind, fname in (("ability", "가젯_능력"), ("item", "가젯_아이템"), ("status", "가젯_상태")):
        p = raw / f"{fname}.txt"
        if p.exists():
            for line in p.read_text(encoding="utf-8").splitlines():
                m = GADGET_HEADING.match(line)
                if m:
                    vocab[kind].add(m.group(1).strip())
                elif kind == "ability" and (m := re.match(r"^([가-힣 ]{2,8}):\s", line)):
                    vocab[kind].add(m.group(1).strip())  # 계열형 가젯 (신체 능력, 수리 기술 ...)
    return vocab


def classify(name: str, sign: int, n: int, vocab: dict[str, set[str]]) -> dict:
    """한 항목(이름, 부호, 수량) -> Effect."""
    name = re.sub(r"\s+", " ", name).strip(" .·'\"")
    if name in RESOURCES:
        return {"type": "resource", "name": name, "delta": sign * n}
    if name in vocab["status"] or re.search(r"수배|평판|악명|우호|적대", name):
        return {"type": "status", "name": name, "delta": sign}
    if name in vocab["ability"]:
        if sign > 0:
            return {"type": "ability", "name": name}
        return {"type": "text", "name": f"능력 상실: {name}"}
    item = {"type": "item", "name": name}
    if sign < 0:
        item["delta"] = -n
    elif n > 1:
        item["delta"] = n
    return item


def parse_items(blob: str, sign: int, vocab: dict[str, set[str]]) -> list[dict]:
    """`생고기, 풀떼기`, `소시지 × 2`, `붕대 or 아스피린` 같은 목록."""
    out: list[dict] = []
    for part in re.split(r"\s*(?:,|\bor\b|또는)\s*", blob):
        part = part.strip()
        if not part:
            continue
        n = 1
        m = re.search(r"\s*[×xX]\s*(\d+)\s*$|\s+(\d+)\s*(?:개|칸)\s*$", part)
        if m:
            n = int(m.group(1) or m.group(2))
            part = part[: m.start()].strip()
        else:
            m = re.fullmatch(r"(체력|멘탈|돈|식량)\s+(\d+)", part)
            if m:
                part, n = m.group(1), int(m.group(2))
        if not part or len(part) > 30 or re.search(r"[?？]|일정|랜덤|확률", part):
            if part:
                out.append({"type": "text", "name": part[:60]})
            continue
        out.append(classify(part, sign, n, vocab))
    return out


def split_top(line: str) -> list[tuple[str, str]]:
    """괄호 밖의 `+ ` / `- ` 부호로 (부호, 텍스트) 조각을 나눈다. 첫 조각 부호는 ''(머리말)."""
    pieces: list[tuple[str, str]] = []
    sign, buf, depth, i = "", [], 0, 0
    while i < len(line):
        c = line[i]
        if c in "([":
            depth += 1
        elif c in ")]":
            depth = max(0, depth - 1)
        if (
            depth == 0
            and c in "+-"
            and i + 1 < len(line)
            and line[i + 1] == " "
            and not (c == "-" and i > 0 and line[i - 1].isdigit() and i + 2 < len(line) and line[i + 2].isdigit())
        ):
            pieces.append((sign, "".join(buf).strip()))
            sign, buf = c, []
            i += 2
            continue
        buf.append(c)
        i += 1
    pieces.append((sign, "".join(buf).strip()))
    return pieces


PAREN_SIGN = re.compile(r"\(([^()]*(?:[+\-]\s|(?<![\w가-힣])[+\-][가-힣A-Za-z<])[^()]*)\)")
COND_END = re.compile(r"(시|면|경우|다면)\s*$")


def parse_paren(content: str, vocab: dict[str, set[str]]) -> list[dict]:
    content = re.sub(r"(?<![\w가-힣])([+\-])(?=[가-힣A-Za-z<])", r" ", content)
    pieces = split_top(content)
    effects: list[dict] = []
    pending = pieces[0][1].strip(" ,")
    for k, (sign, seg) in enumerate(pieces[1:], 1):
        items = [x.strip() for x in seg.split(",")]
        nxt = ""
        while items and COND_END.search(items[-1]) and k < len(pieces) - 1:
            nxt = items.pop()  # 뒤따르는 조각의 조건
        if pending:
            effects.append({"type": "condition", "name": pending[:60]})
        effects += parse_items(", ".join(i for i in items if i), 1 if sign == "+" else -1, vocab)
        pending = nxt
    return effects


def parse_outcome(text: str, vocab: dict[str, set[str]]) -> list[dict]:
    """`머리말 + 획득 - 손실 (종료) (2로 이동)` 의 결과 부분 -> Effect 목록."""
    effects: list[dict] = []
    text = re.sub(r"\[\s*중략\s*\]|\[\d+\]", "", text)
    if END.search(text):
        effects.append({"type": "text", "name": "인카운터 종료"})
        text = END.sub("", text)
    for m in MOVE.finditer(text):
        effects.append({"type": "text", "name": f"{m.group(1)}번으로 이동"})
    text = MOVE.sub("", text)

    # 괄호 안 조건부 결과: (불굴의 투지 보유 시, + 엽총) / (A 없다면 + 출혈, B 보유 시 - B) / (+친분)
    def paren(m: re.Match) -> str:
        effects.extend(parse_paren(m.group(1), vocab))
        return ""
    text = PAREN_SIGN.sub(paren, text)

    m = re.search(r"(\d+)%\s*확률(?:로|이)?", text)
    if m:
        tail = text[m.end():]
        ok = parse_outcome(tail, vocab) if re.search(r"[+\-]\s", tail) else []
        effects.append({
            "type": "chance", "successRate": int(m.group(1)) / 100,
            "success": ok or [{"type": "text", "name": "성공 (위키 서술 참고)"}],
            "failure": [{"type": "text", "name": "실패 (위키 서술 참고)"}],
        })
        text = text[: m.start()] + " "
    for sign, seg in split_top(text)[1:]:
        seg = re.sub(r"\([^()]*\)", "", seg).strip()
        effects += parse_items(seg, 1 if sign == "+" else -1, vocab)
    return effects


def build_prefix(vocab: dict[str, set[str]]) -> re.Pattern:
    names = sorted(vocab["ability"] | vocab["item"] | set(RESOURCES), key=len, reverse=True)
    alt = "|".join(re.escape(n) for n in names)
    return re.compile(rf"^\*?\s*({alt}){LV}(?=[\s,]|$)\s*,?\s*")


def parse_choice(line: str, vocab: dict[str, set[str]], prefix: re.Pattern) -> dict | None:
    line = re.sub(r"\[\s*중략\s*\]|\[\d+\]", "", line).strip()
    if not line:
        return None
    head, *_ = split_top(line)[0:1]
    head_text = head[1]
    effects: list[dict] = []
    rest = line
    # 머리말에서 필요 능력/아이템 접두 추출
    conds: list[str] = []
    cur = head_text
    if re.search(r"\d+%\s*확률", head_text):  # 확률 표기가 문구 쪽에 붙은 경우: 줄 전체에서 chance 해석
        tail = parse_outcome(line, vocab)
        cur = re.sub(r"\d+%\s*확률\S*", "", cur)
    else:
        tail = parse_outcome(rest[len(head_text):] if line.startswith(head_text) else rest, vocab)
    rewards = {e.get("name") for e in tail}
    while True:
        m = prefix.match(cur)
        if not m or (m.group(1) in vocab["item"] and m.group(1) in rewards):
            break  # 보상으로 얻는 아이템은 필요 조건이 아니라 선택지 문구의 일부
        conds.append(re.sub(r"^\*\s*", "", cur[: m.end()]).strip(" ,"))
        cur = cur[m.end():]
    bare = BARE_MOVE.search(cur)
    if bare:
        cur = cur[: bare.start()]
        effects.append({"type": "text", "name": f"{bare.group(1)}번으로 이동"})
    text = re.sub(r"\([^()]*\)|\[[^\[\]]*\]", "", cur)  # 괄호 메모 제거
    text = re.sub(r"\s+", " ", text).strip(" .")
    if not text:
        text, conds = re.sub(r"\s+", " ", re.sub(r"\([^()]*\)|\[[^\[\]]*\]", "", head_text)).strip(" ."), []
    if not text:
        text = head_text.strip(" .") or line[:40]
    effects += [{"type": "condition", "name": c} for c in conds]
    for m in re.finditer(r"\[([^\[\]]+)\]", cur):
        if m.group(1) not in ("종료",) and not re.fullmatch(r"\d+", m.group(1)):
            effects.append({"type": "condition", "name": m.group(1).strip()})
    effects += tail
    if END.search(head_text) or MOVE.search(head_text) or PAREN_SIGN.search(head_text):
        effects += parse_outcome(head_text, vocab)
    return {"text": text[:80], "effects": effects, "nextId": None, "recommend": False, "_raw": line}


def parse_branch(m: re.Match, vocab: dict[str, set[str]]) -> list[dict]:
    """`1) 관찰력 보유시, + 꽁치` -> [condition, 효과...]"""
    body = m.group(2).strip()
    if not body:
        return []
    pieces = split_top(body)
    head = re.sub(r"\[\s*중략\s*\]|\[\d+\]", "", pieces[0][1])
    cond = MOVE.sub("", END.sub("", head)).strip(" ,")
    cond = re.sub(r"^\[([^\[\]]+)\]\s*", lambda m: m.group(1) + " ", cond).strip(" ,")
    out: list[dict] = []
    tail_fx: list[dict] = []
    mv = re.search(r",?\s*([\d\-]+)(?:으)?로\s*이동\s*$", cond)
    if mv:  # "수배 중 미보유시, 2로 이동"
        tail_fx.append({"type": "text", "name": f"{mv.group(1)}번으로 이동"})
        cond = cond[: mv.start()].strip(" ,")
    en = re.search(r",?\s*(?:인카운터\s*)?종료\s*$", cond)
    if en:  # "그외의 경우, 인카운터 종료"
        tail_fx.append({"type": "text", "name": "인카운터 종료"})
        cond = cond[: en.start()].strip(" ,")
    if cond:
        out.append({"type": "condition", "name": cond[:60]})
    out += tail_fx
    rest = body[len(pieces[0][1]):] if pieces[0][1] else body
    markers = " ".join(m.group(0) for m in list(END.finditer(head)) + list(MOVE.finditer(head)))
    out += parse_outcome(((markers + " ") if markers else "") + rest, vocab)
    return out


MOVE_NUM = re.compile(r"(\d+(?:-\d+)*)(?:으)?로\s*이동")
GO_LABEL = re.compile(r"([가-힣][가-힣 ]{1,12}?)\s*(?:으로|로)?\s*(?:진행|이동)")


def link_screens(seg: list[tuple[str | None, str, dict]]) -> None:
    """한 인카운터의 화면들 사이를 nextId 로 잇는다.
    `(2로 이동)` 은 번호 화면, `(물품 찾기 진행)` 은 이름이 맞는 화면, `(진행)` 은 다음 화면. `[종료]` 는 잇지 않는다."""
    for idx, (_, _, node) in enumerate(seg):
        for c in node["choices"]:
            raw = c.get("_raw", "")
            if not raw or END.search(raw):
                continue
            target = None
            m = MOVE_NUM.search(raw)
            if m:
                target = next((n for lab, _, n in seg if lab == m.group(1)), None)
            if target is None and ("진행" in raw or "이동" in raw):
                g = GO_LABEL.search(raw)
                key = g.group(1).strip() if g else ""
                if key and key not in ("다음", "화면"):
                    target = next((n for _, t, n in seg if key in t or key in n["title"]), None)
                if target is None and idx + 1 < len(seg):
                    target = seg[idx + 1][2]
            if target is not None and target is not node:
                c["nextId"] = target["id"]


def parse_text(text: str, story: str, source: str, section: str | None = None,
               vocab: dict[str, set[str]] | None = None, keep_raw: bool = False,
               used: set[str] | None = None) -> list[dict]:
    vocab = vocab or load_vocab()
    prefix = build_prefix(vocab)
    segs: list[tuple[str, str, list[str]]] = []
    cur = None
    for line in text.splitlines():
        m = HEADING.match(line)
        if m:
            cur = (m.group(1), m.group(2), [])
            segs.append(cur)
        elif cur is not None:
            cur[2].append(line.rstrip())
    nodes: list[dict] = []
    used = used if used is not None else set()
    for num, title, lines in segs:
        if section and not (num == section or num.startswith(section + ".")):
            continue
        body, choices, group_cond = [], [], []
        blocks = [(None, "", body, choices)]
        last_blank, attach = True, False
        i = 0
        while i < len(lines):
            s = lines[i].strip()
            i += 1
            if not s:
                last_blank, attach = True, False
                continue
            sm = SCREEN.match(s)
            if sm:  # 새 화면: 이후 선택지는 하위 노드로 분리
                body, choices, group_cond = [], [], []
                blocks.append((sm.group(1), sm.group(2), body, choices))
                attach, last_blank = False, False
                continue
            if s == "-":
                j = i
                while j < len(lines) and not lines[j].strip():
                    j += 1
                if j < len(lines) and re.fullmatch(r"\[[^\[\]]+\]", lines[j].strip()):
                    group_cond = [{"type": "condition", "name": lines[j].strip()[1:-1]}]
                    i = j + 1
                    attach = False
                    last_blank = False
                    continue
                bm2 = j < len(lines) and re.match(r"^\[([^\[\]]+)\]\s*(?=[+\-]\s)(.*)$", lines[j].strip())
                if bm2 and choices:
                    choices[-1]["effects"] += [{"type": "condition", "name": bm2.group(1)}] + parse_outcome(bm2.group(2), vocab)
                    i = j + 1
                    continue
                if j < len(lines):
                    c = parse_choice(lines[j], vocab, prefix)
                    if c:
                        c["effects"] = [dict(e) for e in group_cond] + c["effects"]
                        c["_g"] = bool(group_cond)
                        choices.append(c)
                        attach = True
                    i = j + 1
                last_blank = False
                continue
            bm = BRANCH.match(s)
            if bm:
                effs = parse_branch(bm, vocab)
                nxt = next((x.strip() for x in lines[i:] if x.strip()), "")
                if choices and nxt != "-":
                    choices[-1]["effects"] += effs  # 다음이 선택지 마커가 아니면 직전 선택지의 분기
                else:
                    group_cond = [e for e in effs if e["type"] == "condition"][:1]
                last_blank = False
                continue
            para_start = last_blank
            last_blank = False
            group_cond = []
            if choices and para_start and any(x.strip() == "-" for x in lines[i:]):
                body, choices = [], []  # 선택지 뒤에 새 문단과 또 선택지: 다음 화면
                short = len(s) <= 16
                blocks.append((str(len(blocks) + 1), s if short else "", body, choices))
                if short:
                    continue
            if (not choices and para_start and len(s) <= 16 and blocks[-1][0] is not None and not blocks[-1][1]
                    and next((x.strip() for x in lines[i:] if x.strip()), "") == "-"):
                blocks[-1] = (blocks[-1][0], s, blocks[-1][2], blocks[-1][3])  # "창고 도착" 같은 화면 이름 한 줄
                continue
            if not choices:
                body.append(re.sub(r"\[\s*중략\s*\]|\[\d+\]|'''", "", s))
        seg: list[tuple[str | None, str, dict]] = []
        for label, stext, body, choices in blocks:
            if not choices:
                continue
            ntitle, nbody = title, " ".join(body)
            nid = slugify(title)
            if label:
                short = len(stext) <= 16
                ntitle = f"{title} · {label}" + (f" {stext}" if short and stext else "")
                nbody = nbody if short else f"{stext} {nbody}"
                nid = f"{nid}-{slugify(label)}"
            base, k = nid, 2
            if nid in used:
                nid = f"{base}-{slugify(num)}"
            while nid in used:
                nid = f"{base}-{slugify(num)}-{k}"
                k += 1
            used.add(nid)
            node = {
                "id": nid, "title": ntitle, "story": story,
                "body": (nbody.strip()[:400] or None),
                "choices": choices, "source": source, "updatedAt": TODAY,
            }
            seg.append((label, stext, node))
            nodes.append(node)
        link_screens(seg)
        for _, _, node in seg:
            for c in node["choices"]:
                c.pop("_g", None)
                if not keep_raw:
                    c.pop("_raw", None)
                seen, uniq = set(), []
                for e in c["effects"]:
                    k2 = json.dumps(e, ensure_ascii=False, sort_keys=True)
                    if k2 not in seen:
                        seen.add(k2)
                        uniq.append(e)
                c["effects"] = uniq
    return nodes


def report(nodes: list[dict], limit: int = 12) -> None:
    """작성자별 표기 차이로 놓쳤을 만한 줄을 사유별로 보여 준다 (parse_text(keep_raw=True) 결과)."""
    typed = {"item", "resource", "status", "ability", "chance"}
    reasons: dict[str, list[str]] = {"산문 결과(획득/소모 등)인데 효과 없음": [], "부호가 있는데 효과 없음": [],
                                       "선택지 문구가 너무 김(>60자)": [], "효과가 하나도 없음": []}
    total = 0
    for n in nodes:
        for c in n["choices"]:
            total += 1
            raw, kinds = c.get("_raw", ""), {e["type"] for e in c["effects"]}
            tag = f'{n["title"]}: {raw[:90]}'
            if not kinds & typed and re.search(r"획득|얻는다|잃는다|소모|회복|깎", raw):
                reasons["산문 결과(획득/소모 등)인데 효과 없음"].append(tag)
            if not kinds & typed and re.search(r"\s[+\-]\s", raw):
                reasons["부호가 있는데 효과 없음"].append(tag)
            if len(raw) > 60 and not kinds & typed:
                reasons["선택지 문구가 너무 김(>60자)"].append(tag)
            if not c["effects"]:
                reasons["효과가 하나도 없음"].append(tag)
    print(f"선택지 {total}개 점검")
    for why, rows in reasons.items():
        print(f"\n[{why}] {len(rows)}건")
        for r in rows[:limit]:
            print("  ", r)


STORY_NAMES = {
    "기본 인카운터": "기본", "주요 스토리 인카운터": "주요 스토리", "액티브 인카운터": "액티브",
    "하드 모드": "하드 모드", "레벨 인카운터": "레벨", "이야기 인카운터": "이야기", "시즌 패스 인카운터": "시즌 패스",
}
SKIP = ("가젯_", "허브", "확장팩 인카운터")


def build_all(raw: Path, curated: Path | None) -> list[dict]:
    """raw 디렉터리의 모든 문서를 변환하고 id를 전역 유일하게 만든다. curated 노드가 우선."""
    vocab = load_vocab(raw)
    nodes: list[dict] = []
    used: set[str] = set()
    if curated and curated.exists():
        used |= {n["id"] for n in json.loads(curated.read_text(encoding="utf-8")).get("nodes", [])}
    rank = {name: i for i, name in enumerate(STORY_NAMES)}  # 기본 -> 주요 스토리 -> ... -> 확장팩(가나다)
    for f in sorted(raw.glob("*.txt"), key=lambda f: (rank.get(f.stem, len(rank)), f.stem)):
        if f.stem.startswith(SKIP[0]) or f.stem in SKIP[1:]:
            continue
        story = STORY_NAMES.get(f.stem, f.stem)
        nodes += parse_text(f.read_text(encoding="utf-8"), story, f"namu:{f.stem}", vocab=vocab, used=used)
    if curated and curated.exists():
        keep = json.loads(curated.read_text(encoding="utf-8")).get("nodes", [])
        titles = {n["title"] for n in keep}
        nodes = keep + [n for n in nodes if n["title"] not in titles or n["story"] != "기본"]
    used: set[str] = set()
    for n in nodes:
        nid = n["id"]
        if nid in used:
            nid = f"{n['id']}-{slugify(n['story'])}"
        k = 2
        while nid in used:
            nid = f"{n['id']}-{slugify(n['story'])}-{k}"
            k += 1
        n["id"] = nid
        used.add(nid)
    return nodes


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("raw", type=Path, help="원문 파일, --all이면 원문 디렉터리")
    ap.add_argument("--all", action="store_true", help="디렉터리의 모든 문서를 변환·병합")
    ap.add_argument("--curated", type=Path, default=None)
    ap.add_argument("--story", default="기본")
    ap.add_argument("--section", default=None, help="번호 접두 (예: 6)")
    ap.add_argument("--out", type=Path, default=None)
    ap.add_argument("--report", action="store_true", help="놓쳤을 만한 표기 변형을 출력")
    args = ap.parse_args()
    if args.all:
        nodes = build_all(args.raw, args.curated)
        args.out.write_text(json.dumps({"version": 1, "nodes": nodes}, ensure_ascii=False, separators=(",", ":")) + "\n",
                            encoding="utf-8")
        print(f"{len(nodes)} nodes, {sum(len(n['choices']) for n in nodes)} choices -> {args.out}")
        return
    nodes = parse_text(args.raw.read_text(encoding="utf-8"), args.story,
                       f"namu:{args.raw.stem}", args.section, keep_raw=args.report)
    if args.report:
        report(nodes)
    if args.out is None:
        return
    args.out.write_text(json.dumps({"version": 1, "nodes": nodes}, ensure_ascii=False, indent=2) + "\n",
                        encoding="utf-8")
    print(f"{len(nodes)} nodes, {sum(len(n['choices']) for n in nodes)} choices -> {args.out}")


if __name__ == "__main__":
    main()
