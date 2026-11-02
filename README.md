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

`data/encounters.json` 수정 후:

```powershell
.\scripts\sync-seed.ps1
```

assets / 테스트 리소스로 복사합니다.

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

## 나중에

- 런타임 AI 캐시 (미스 시 구조화)
- Web 검색 UI (동일 seed)
- Seed 대량 수집

## 출처

조우·선택지 정보는 [나무위키:서울 2033](https://namu.wiki/w/%EC%84%9C%EC%9A%B8%202033) 등을 참고합니다. 비공식 팬 도구이며 공식 앱이 아닙니다.
