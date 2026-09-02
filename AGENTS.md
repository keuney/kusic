# AGENTS.md
# Keuney Music — Codex Operating Rules

이 파일은 Codex가 저장소에서 작업할 때 따라야 하는 최상위 구현 규칙이다.

PRD의 제품 요구사항과 ARCHITECTURE의 구조적 결정을 위반해서는 안 된다.

---

## 1. Mandatory Reading

모든 작업을 시작하기 전에 다음 파일을 읽는다.

1. `AGENTS.md`
2. `PRD.md`
3. `ARCHITECTURE.md`
4. `TASKS.md`
5. `docs/DECISIONS.md` — 존재하는 경우

현재 Task의 Acceptance Criteria를 확인한 후 구현을 시작한다.

---

## 2. Project Goal

Keuney Music은 개인 사이드로드용 Android 음악 플레이어다.

핵심 목표:

- 음악 검색
- 재생
- background playback
- screen-off playback
- notification / lock screen / Bluetooth media control
- local favorites / playlists / history

v0.1에서 Google Play 배포, 로그인, 영구 다운로드는 목표가 아니다.

---

## 3. Scope Rule

한 번에 **하나의 `KM-xxx` Task만 구현한다.**

현재 Task에 필요하지 않은 미래 기능을 미리 구현하지 않는다.

금지 예:

- Search Task 중 Lyrics 추가
- Player Task 중 Login 추가
- Library Task 중 Android Auto 추가
- Source Task 중 Download 기능 추가

관련 없는 TODO도 불필요하게 만들지 않는다.

---

## 4. Language / UI Rules

- Kotlin only
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL
- Version Catalog 사용
- Java 신규 작성 금지
- XML UI 신규 작성 금지 — 플랫폼 요구 등 특별한 이유가 있을 때만 허용

---

## 5. Architecture Rules

기본 흐름:

```text
UI
↓
ViewModel
↓
UseCase / Repository
↓
Domain Interface
↓
Infrastructure
```

금지:

- Compose → DAO 직접 접근
- Compose → MusicSource 직접 접근
- ViewModel → Room DAO 직접 접근
- ViewModel → ExoPlayer 직접 소유
- UI → source-specific DTO 직접 사용

---

## 6. Player Ownership

ExoPlayer는 반드시 `MusicService : MediaLibraryService`가 소유한다.

Activity 또는 ViewModel이 ExoPlayer instance를 소유해서는 안 된다.

UI는 `MediaController`를 통해 playback service와 통신한다.

이 규칙은 절대 변경하지 않는다. 변경이 필요하면 ADR을 먼저 작성한다.

---

## 7. Source Boundary

외부 콘텐츠 접근은 반드시 `MusicSource` 뒤에 둔다.

예:

```kotlin
interface MusicSource {
    suspend fun search(query: String): Result<List<Track>>
    suspend fun resolveStream(trackId: String): Result<PlayableStream>
    suspend fun getTrack(trackId: String): Result<Track>
    suspend fun getRelated(trackId: String): Result<List<Track>>
}
```

source-specific DTO는 `data/source/` 밖으로 노출하지 않는다.

Domain에는 `Track`, `PlayableStream` 등 provider-neutral 모델만 전달한다.

---

## 8. Stream URL Rule

resolved stream URL을 Room, DataStore 또는 영구 파일에 저장하지 않는다.

Queue에는 Track ID와 metadata만 저장한다.

재생에 실제 URL이 필요할 때 `StreamResolver`를 통해 지연 해석한다.

URL 만료 또는 HTTP 오류 시 재해석 후 제한된 횟수만 재시도한다.

무한 retry 금지.

---

## 9. Dependency Rule

Codex는 dependency를 편의상 임의 추가하지 않는다.

새 dependency가 필요하면:

1. 기존 stack으로 해결 가능한지 확인
2. 최소 dependency 선택
3. stable release 사용
4. `gradle/libs.versions.toml`에 추가
5. `docs/DECISIONS.md`에 이유 기록

alpha / beta / snapshot은 명확한 이유 없이는 사용하지 않는다.

---

## 10. Database Rule

Room은 local library 상태를 저장한다.

ViewModel은 DAO를 직접 호출하지 않는다.

Repository를 거친다.

resolved stream URL은 저장하지 않는다.

DB migration이 발생하면 migration test 또는 명시적 migration 전략을 추가한다.

---

## 11. State Management

ViewModel은 immutable UI state를 노출한다.

권장:

```kotlin
StateFlow<UiState>
```

Compose가 mutable repository state를 직접 관리하지 않는다.

One-off event는 navigation, snackbar 등 필요한 경우에만 별도 abstraction을 사용한다.

---

## 12. Error Handling

raw exception을 UI에 직접 표시하지 않는다.

Infrastructure exception을 domain/app error로 변환한다.

UI에는 사용자 친화적 오류를 노출한다.

예:

- Network
- SourceUnavailable
- PlaybackUnavailable
- GeoRestricted
- Unknown

---

## 13. Logging & Secrets

절대 로그에 남기지 않는다.

- cookies
- auth tokens
- credentials
- complete resolved stream URLs
- sensitive URL query parameters

Release build에서 디버그용 상세 네트워크 로그를 남기지 않는다.

---

## 14. Login Rule

v0.1에는 Login을 구현하지 않는다.

다음 저장 금지:

- Google password
- OAuth token
- YouTube auth cookie
- account session credential

Login을 추가하려면 별도 PRD/ADR이 필요하다.

---

## 15. Download Rule

Permanent offline download는 v0.1 범위 밖이다.

Streaming cache와 permanent download를 혼동하지 않는다.

Cache는 삭제 가능한 LRU 형태로만 다룬다.

---

## 16. Test Rule

Business logic을 추가하면 관련 unit test를 추가한다.

특히 다음은 우선 테스트 대상이다.

- Queue
- Repository
- ViewModel
- DTO mapper
- error mapper
- Source contract
- playback state mapping

UI test는 핵심 flow 위주로 최소화한다.

---

## 17. Source Contract Tests

실제 외부 Source를 호출하는 테스트는 일반 unit test와 분리한다.

예:

```text
sourceContractTest
```

목적:

- search 응답 구조 변경 감지
- resolveStream 파손 감지
- provider 변경 조기 감지

CI 기본 경로와 분리할 수 있다.

---

## 18. Required Verification

Task 완료 전 최소 다음을 실행한다.

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Task가 release 설정에 영향을 주면:

```bash
./gradlew assembleRelease
```

관련 Task가 source contract 또는 device test를 요구하면 해당 검증도 수행한다.

---

## 19. Build Failure Rule

빌드 실패 상태에서 완료 처리하지 않는다.

순서:

1. reproduce
2. root cause inspect
3. 최소 수정
4. affected test 재실행
5. full verification 재실행

현재 Task와 무관한 기존 실패면 원인을 문서화하고 보고한다.

관련 없는 코드를 대량 수정해 build를 억지로 맞추지 않는다.

---

## 20. Device Validation Rule

다음 기능은 emulator 통과만으로 완료 처리하지 않는다.

- background playback
- screen-off playback
- Bluetooth controls
- headset disconnect
- OEM battery behavior

가능한 경우 실제 Android 기기에서 Acceptance Criteria를 확인한다.

실기기 검증을 수행할 수 없는 환경이면:

- 구현 완료 여부
- emulator 결과
- 남은 real-device verification

을 명확히 분리해 보고한다.

---

## 21. Git Rule

권장 branch:

```text
codex/KM-xxx-short-description
```

Commit prefix:

```text
feat:
fix:
refactor:
test:
docs:
build:
```

한 Task는 가능한 한 하나의 논리적 commit으로 유지한다.

Codex가 사용자 허락 없이 destructive git operation을 수행하지 않는다.

---

## 22. Documentation Rule

Architecture 변경 또는 중요한 기술 선택은 `docs/DECISIONS.md`에 기록한다.

최소 ADR 대상:

- MediaLibraryService
- MusicSource abstraction
- Source Provider 선택
- Stream URL non-persistence
- Room local-first
- Login 제외
- Download 제외
- 신규 GPL dependency

---

## 23. Refactoring Rule

기능 추가와 대규모 refactor를 한 Task에서 동시에 수행하지 않는다.

Architecture 변경이 필요하면:

1. 기존 문서 확인
2. 변경 사유 정리
3. 최소 변경
4. ADR 기록
5. 테스트

---

## 24. Coding Quality Rules

- 작은 함수
- 명확한 이름
- nullable 최소화
- provider-specific magic constant 격리
- duplicate logic 제거
- public API 최소화
- unnecessary abstraction 금지
- premature optimization 금지

---

## 25. Naming Rules

Domain 이름에는 provider 이름을 넣지 않는다.

좋음:

```text
Track
MusicSource
PlayableStream
PlayerRepository
```

나쁨:

```text
YouTubeSong
YouTubeTrackRepository
```

provider 구현 내부에서는 구체적 이름을 사용할 수 있다.

---

## 26. Completion Report

Task 종료 시 다음 형식으로 보고한다.

### Completed
- 구현한 내용

### Changed files
- 파일 목록

### Verification
- 실행한 명령
- PASS / FAIL

### Acceptance Criteria
- 각 항목 PASS / FAIL

### Decisions
- 추가/변경한 기술 결정

### Remaining risks
- 남은 위험
- real-device 검증 필요 여부

---

## 27. Task Completion Rule

Task는 다음 조건을 모두 만족해야 완료다.

- 구현 완료
- 관련 테스트 추가/수정
- Acceptance Criteria 충족
- `./gradlew test` PASS
- `./gradlew lint` PASS
- `./gradlew assembleDebug` PASS
- 문서 변경이 필요한 경우 반영

Task status를 `TASKS.md`에서 완료로 표시한다.

---

## 28. First Priority

처음부터 Source Provider 또는 UI를 만들지 않는다.

첫 핵심 Gate:

```text
MusicService
↓
ExoPlayer
↓
MediaLibrarySession
↓
known test audio
↓
Home background playback
↓
Screen off playback
```

이 Gate가 통과한 뒤 Source Provider 작업을 진행한다.

---

## 29. Final Rule

프로젝트의 핵심은 특정 YouTube API가 아니다.

**안정적인 Android 음악 플레이어**가 핵심이다.

Source Provider가 바뀌어도 Player / UI / Library / Queue는 가능한 한 그대로 유지되어야 한다.
