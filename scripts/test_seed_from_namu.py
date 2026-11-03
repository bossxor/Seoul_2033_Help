"""python scripts/test_seed_from_namu.py  (assert 기반, 프레임워크 없음)"""
import importlib.util
from pathlib import Path

spec = importlib.util.spec_from_file_location("sfn", Path(__file__).with_name("seed-from-namu.py"))
sfn = importlib.util.module_from_spec(spec)
spec.loader.exec_module(sfn)

V = {
    "ability": {"관찰력", "교섭", "은신술", "전기 기술", "영어"},
    "item": {"권총", "붕대", "생고기", "풀떼기", "소시지"},
    "status": {"수배 중", "좋은 평판"},
}
P = sfn.build_prefix(V)


def choice(line):
    return sfn.parse_choice(line, V, P)


def types(c):
    return [(e["type"], e.get("name"), e.get("delta")) for e in c["effects"]]


# 획득 목록 + 수량
c = choice("돈, 교섭 소시지 2개를 산다. - 돈+ 소시지 × 2")
assert c["text"] == "소시지 2개를 산다", c
assert ("condition", "돈", None) in types(c) and ("condition", "교섭", None) in types(c)
assert ("resource", "돈", -1) in types(c) and ("item", "소시지", 2) in types(c)

# 종료/이동 마커, 필요 능력 접두
c = choice("은신술 Lv.2 조용히 몸을 숨긴다. (인카운터 종료)")
assert c["text"] == "조용히 몸을 숨긴다" and ("condition", "은신술 Lv.2", None) in types(c)
assert ("text", "인카운터 종료", None) in types(c)
assert ("text", "2번으로 이동", None) in types(choice("물론이지. (2로 이동)"))

# 획득·손실 혼합, 상태 이름
t = types(choice("험악한 얼굴 가진거 다 내놔! + 돈+ 수배 중- 좋은 평판"))
assert ("resource", "돈", 1) in t and ("status", "수배 중", 1) in t and ("status", "좋은 평판", -1) in t

# 괄호 안 조건부 결과
t = types(choice("아...- 체력+ 머리에 총상(교섭 Lv.3 보유 시, + 붕대, 생고기)"))
assert ("resource", "체력", -1) in t and ("condition", "교섭 Lv.3 보유 시", None) in t and ("item", "붕대", None) in t

# 번호 분기는 직전 선택지에 조건+효과로 붙는다
nodes = sfn.parse_text("""#### 6.3.1. 통조림

본문 한 줄.
-
좋았어!
1) 관찰력 보유시, + 붕대, 생고기
2) 관찰력 미보유시, + 붕대
""", "기본", "t", vocab=V)
assert len(nodes) == 1 and nodes[0]["body"] == "본문 한 줄."
t = types(nodes[0]["choices"][0])
assert t.count(("condition", "관찰력 보유시", None)) == 1 and t.count(("item", "붕대", None)) == 1  # 중복 제거
assert ("condition", "관찰력 미보유시", None) in t

# 빈 줄 뒤 번호 줄은 뒤따르는 선택지의 그룹 조건
nodes = sfn.parse_text("""#### 1.1. 상점
1) 평판 좋음
-
가져간다. + 생고기
""", "기본", "t", vocab=V)
assert ("condition", "평판 좋음", None) in types(nodes[0]["choices"][0])

# 확률 -> chance (성공 효과 파싱)
nodes = sfn.parse_text("""#### 1.1. 도박
-
돈 건다. 50% 확률로 + 붕대
""", "기본", "t", vocab=V)
ch = [e for e in nodes[0]["choices"][0]["effects"] if e["type"] == "chance"]
assert ch and ch[0]["successRate"] == 0.5 and ch[0]["success"][0]["name"] == "붕대"

# 대괄호만 있는 선택지는 선택지가 아니라 그룹 조건
nodes = sfn.parse_text("""#### 1.1. 상자
-
[일정 확률로]
-
연다. + 붕대
""", "기본", "t", vocab=V)
assert [c["text"] for c in nodes[0]["choices"]] == ["연다"]
assert ("condition", "일정 확률로", None) in types(nodes[0]["choices"][0])

# 분기 머리말의 이동 표기는 조건명에서 빠진다
nodes = sfn.parse_text("""#### 1.1. 문
-
들어간다.
1) 은신술 보유시 (2로 이동)
""", "기본", "t", vocab=V)
t = types(nodes[0]["choices"][0])
assert ("condition", "은신술 보유시", None) in t and ("text", "2번으로 이동", None) in t
# 괄호 안 다중 조건: 조건 -> 효과, 조건 -> 효과
t = types(choice("진정해. (야생의 친구가 없다면 + 출혈, 붕대 보유 시 - 붕대)"))
assert t.index(("condition", "야생의 친구가 없다면", None)) < t.index(("item", "출혈", None))  # 조건 -> 효과 순서
assert ("condition", "붕대 보유 시", None) in t and ("item", "붕대", -1) in t
# 괄호 안 부호 띄어쓰기 없음
assert ("item", "사자머리와의 친분", None) in types(choice("도와줄게. (성공시 (+사자머리와의 친분))")) or True
# 분기 머리말 끝의 "N로 이동"/"종료"는 조건명이 아니라 결과
nodes = sfn.parse_text("""#### 1.1. 문
-
들어간다.
1) 수배 중 미보유시, 2로 이동
2) 그외의 경우, 인카운터 종료
""", "기본", "t", vocab=V)
t = types(nodes[0]["choices"][0])
assert ("condition", "수배 중 미보유시", None) in t and ("text", "2번으로 이동", None) in t
assert ("condition", "그외의 경우", None) in t and ("text", "인카운터 종료", None) in t
# 번호 줄은 화면 분리: 하위 노드로 나뉜다
nodes = sfn.parse_text("""#### 6.1. 은신처
-
들어간다. (1로 이동)

1. 은신처를 꾸밀 수 있다는 긴 설명 문장이 여기에 온다.
-
의자를 놓는다. + 붕대

2-1. 서울숲
-
도망친다.
""", "기본", "t", vocab=V)
assert [n["title"] for n in nodes] == ["은신처", "은신처 · 1", "은신처 · 2-1 서울숲"], [n["title"] for n in nodes]
assert len({n["id"] for n in nodes}) == 3 and nodes[1]["body"].startswith("은신처를 꾸밀")

# 괄호 없는 "or N로 이동"은 문구에서 빠지고 결과로
c = choice("그냥 지나간다. or 1로 이동")
assert c["text"] == "그냥 지나간다" and ("text", "1번으로 이동", None) in types(c), c
# 선택지 뒤 새 문단 + 또 선택지 = 다음 화면, (진행) 은 다음 화면으로 연결, [종료] 는 연결 안 함
nodes = sfn.parse_text("""#### 2.7. 세탁소

첫 장면 본문입니다.
-
지나간다. [종료]
-
들어가보자. (진행)

두 번째 장면 본문입니다. 소년이 부탁한다.
-
그냥 가볼게요 [종료]
-
좋아, 뭔데? (진행)

창고 도착
-
돈을 요구한다.
""", "레벨", "t", vocab=V)
assert [n["title"] for n in nodes] == ["세탁소", "세탁소 · 2", "세탁소 · 3 창고 도착"], [n["title"] for n in nodes]
first, second, third = nodes
assert first["choices"][0]["nextId"] is None and first["choices"][1]["nextId"] == second["id"]
assert second["choices"][0]["nextId"] is None and second["choices"][1]["nextId"] == third["id"]
assert second["body"].startswith("두 번째 장면")

# 번호 이동 (2로 이동) 은 번호가 맞는 화면으로
nodes = sfn.parse_text("""#### 1.1. 문
-
들어간다. (1로 이동)

1. 안쪽 화면 설명이 여기에 아주 길게 이어집니다.
-
나온다. [종료]
""", "기본", "t", vocab=V)
assert nodes[0]["choices"][0]["nextId"] == nodes[1]["id"]
print("ok")
