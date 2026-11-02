# Seoul 2033 Help

보드게임 **서울 2033** 조우 선택지 도우미. 나무위키 기반 seed를 검색·오버레이로 보여 줍니다. 읽기 전용.

## Android MVP

프로젝트 루트에서 Android Studio로 `android/` 폴더를 엽니다.

```text
android/          # Gradle 앱
data/encounters.json   # 정본 seed
docs/superpowers/specs/2026-11-02-seoul2033-help-overlay-design.md
```

### Seed 동기화

`data/encounters.json`이 정본. 수정 후:

```powershell
.\scripts\sync-seed.ps1
```

Android assets / 테스트 리소스 / `web/encounters.json`으로 복사합니다.

### Seed 채우기 (기본 인카운터)

1. raw: `data/raw/basic-encounters-readonly.txt` (+ `SOURCE.md`)
2. 초안 생성:

```powershell
$py = "$env:LOCALAPPDATA\Python\bin\python.exe"
& $py scripts/seed-from-wiki.py --merge-curated data/encounters.curated.json --out data/encounters.json --batch1
.\scripts\sync-seed.ps1
```

- `encounters.curated.json` = 손수 검수 노드(예: 들개 무리). id 충돌 시 curated 우선.
- `--batch1` = 단순 인카운터 부분집합. 전체 초안은 `data/encounters.draft.json`.
- 애매한 결과는 `type: text`로 두고 나중에 고친다.

### 권한

| 권한 | 용도 |
|------|------|
| `SYSTEM_ALERT_WINDOW` | 게임 위 오버레이 |
| `FOREGROUND_SERVICE` / `MEDIA_PROJECTION` | 화면 캡처 OCR |
| `POST_NOTIFICATIONS` | 포그라운드 알림 |

설정에서 **오버레이 시작** → 캡처 동의 → 인식 버튼(또는 자동 OCR).

### 빌드 (UNC 경로 이슈 시)

네트워크 경로에서 Gradle이 깨지면 로컬로 복사해 빌드:

```powershell
robocopy "<repo>\android" C:\tmp\s2033 /MIR /XD .gradle build .idea
cd C:\tmp\s2033
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### MVP 체크리스트

- [ ] 검색으로 샘플 조우 찾기
- [ ] 확률 효과 `%` 표시
- [ ] 추천 선택 테두리 구분
- [ ] 오버레이 투명도 유지
- [ ] OCR 하단 crop 비율 반영
- [ ] Ambiguous 후보 선택
- [ ] Miss → `정보 없음`
- [ ] 닫기 = 앱 종료

### 런타임 AI 캐시

설정에 OpenAI 호환 **API 키** / Base URL / 모델을 넣으면, 오버레이 매칭 미스 시 **AI 해독**이 켜집니다. 성공 노드는 기기 `filesDir/local_encounters.json`에 쌓이고(시드와 동일 id면 캐시 우선), 검색·재인식에 반영됩니다.

- 설정 → **AI 캐시 export** 로 JSON 공유
- adb (디버그): `adb shell run-as com.bossxor.seoul2033help cat files/local_encounters.json`

키 없으면 기존처럼 `정보 없음` + 검색만.

## Web 검색 답지

PC에서 폰 게임과 병행하는 검색·트리 UI. OCR/오버레이/AI 없음. 시드는 Android와 동일.

```powershell
.\scripts\sync-seed.ps1
cd web
python -m http.server 8080
```

브라우저에서 http://localhost:8080 열기.

```powershell
node --test web/test.mjs
```

## 나중에

- Seed 추가 배치 / 검수
- 비주얼 스킨 폴리시

## 출처

조우·선택지 정보는 [나무위키:서울 2033](https://namu.wiki/w/%EC%84%9C%EC%9A%B8%202033) 등을 참고합니다. 비공식 팬 도구이며 공식 앱이 아닙니다.
