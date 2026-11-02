# Seoul 2033 Help — Design System (Master)

**Product:** 핵전쟁 이후 서울 텍스트 인카운터 답지 앱 (게임 오버레이 + 검색)  
**Audience:** 플레이 중 한손·짧은 시선. 스포일러보다 **지금 선택지 결과**가 우선.  
**Job:** 인카운터명·선택지·결과(확률 포함)를 게임 위에 읽히게.  
**Stack:** Jetpack Compose (Android 우선). Web은 동일 토큰 이식.  
**Mode:** Dark only (OLED). Light 모드 없음 (MVP).

> 자동 `--design-system` 초안(크림 배경·퍼플 accent·Orbitron 게이밍 키트)은 **폐기**.  
> 주제(폐허 서울·지하·비상 안내)에 맞춰 재선택함.

## Style direction

**Bunker Notice Board** — 콘크리트·그을음·비상등 호박색.  
HUD 네온/마젠타/글리치/스캔라인 **없음** (가독·배터리·오버레이 투명도와 충돌).  
카드 남발 금지. 선택지 행 = 얇은 패널 + **테두리로만** 위계.

## Color tokens

| Token | Hex | Use |
|-------|-----|-----|
| `bg` | `#121212` | 앱 배경 (OLED grey, pure black 눈부심 완화) |
| `surface` | `#1C1C1E` | 검색 행·오버레이 패널 본체 |
| `surface-elevated` | `#2A2A2C` | 입력/슬라이더 트랙 |
| `text` | `#E8E4DC` | 본문 (따뜻한 회백 — 폐지 느낌) |
| `text-muted` | `#9A958C` | 스토리 라벨·부가 |
| `border` | `#3F3C38` | 기본 선택지 테두리 |
| `recommend` | `#E6A817` | **추천 선택지 border** (비상등/호박) |
| `chance` | `#D4A574` | 확률 결과 강조 텍스트 |
| `danger` | `#C45C4A` | 실패·손실 요약 |
| `ok` | `#7A9E7E` | 이득·성공 요약 (채도 낮게) |
| `focus` | `#E6A817` | 포커스 링 |
| `scrim` | `#121212` @ alpha | 오버레이 배경 = `UserSettings.overlayAlpha` |

추천 vs 비추천은 **색만으로 끝내지 않음** — border 두께 2dp vs 1dp + (선택) 좌측 3dp 앰버 바.

## Typography

| Role | Family | Notes |
|------|--------|-------|
| UI / body | `SansSerif` (플랫폼 기본) 또는 Noto Sans KR | 오버레이 가독 최우선. 커스텀 웹폰트 로드 안 함 |
| Title | Same family, SemiBold | Orbitron/네온 게이밍 폰트 금지 |
| Mono data | `Monospace` | 신뢰도 % · 확률 % 만 |

Type scale (Compose sp): title 18, choice 16, effect 13, meta 12.  
줄길이 짧게. 효과 문구는 한 줄 요약 + 필요 시 wrap.

## Layout

- **8dp rhythm.** 선택지 간격 8dp. 터치 타깃 **≥48dp** (Android).
- 인접 버튼 간격 ≥8dp.
- 오버레이: 화면 가장 차지하지 않음. 드래그 핸들 + 스크롤 리스트.
- 검색: `LazyColumn` + stable `key = node.id`.
- 고정 헤더/컨트롤 아래 리스트가 가리지 않게 content padding.

### Overlay (ASCII)

```
┌─[ story / title          신뢰도 nn% ]─┐
│ choice text                         │  ← border amber if recommend
│ effects summary (chance 구분)       │
│ ...                                 │
├─ [인식] [검색] [추천] [α] [닫기] ─┤
└─────────────────────────────────────┘
```

## Motion

거의 없음. 인식 중 CircularProgressIndicator만.  
리스트 fade-in 금지. `prefers-reduced-motion` 해당 없음(Compose: 애니메이션 최소화).

## Components

| Component | Rule |
|-----------|------|
| `ChoiceRow` | 전체폭. border. 추천=`recommend` 색 2dp. 비추천=`border` 1dp |
| Chance effect | `chance` 색 + `44%` monospace. 성공=`ok` / 실패=`danger` 조각 |
| Miss / error | 짧은 한국어 + 다음 행동 (검색 / 재시도) |
| Icons | 벡터만. 이모지 아이콘 금지. Material Icons Outlined 일관 |

## Anti-patterns (이 프로젝트)

- 크림 `#FFFBEB` / 퍼플 CTA / 골드+퍼플 코인 키트
- 네온 마젠타·매트릭스 그린 글로우·스캔라인
- 모든 블록을 동일 radius 카드+그림자
- ALL-CAPS eyebrow, `A · B · C` 메타 줄
- 조건 필터 UI (스펙 제외)

## Compose notes (verified search)

- `LazyColumn` + `key`
- `Arrangement`/`Alignment`로 간격
- immutable list in state
- Touch ≥48dp, gap ≥8dp

## Page overrides

- `pages/overlay.md` — 플로팅 패널
- `pages/search.md` — 전체화면 검색/상세
