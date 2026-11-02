# Overlay page override

Master 위에 적용.

- 패널 `surface` + 사용자 alpha. 글자 대비를 위해 alpha 하한 0.4 유지(스펙).
- 컨트롤 한 줄: 아이콘+텍스트 또는 아이콘만이면 contentDescription 필수.
- `닫기`는 destructive 톤(`danger` 테두리 가능) — 앱 종료임을 짧게 명시 가능: "종료".
- 후보 목록(Ambiguous): 제목만 큰 터치행, 탭 후 본 답지로.
- OCR 중에는 선택지 영역 대신 상태 한 줄.
