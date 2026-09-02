# KEUNEY MUSIC
## Product Requirements Document
### Codex Execution Specification

Version: 1.0  
Date: 2026-09-02  
Platform: Android  
Development workflow: ChatGPT Codex  
Distribution: Personal sideload only

---

# 1. Product Summary

## 1.1 Product Name

**Keuney Music**

Working package name:

`com.keuney.music`

## 1.2 Product Vision

Keuney Music은 사용자가 Android 스마트폰에서 음악을 검색하고 음악 플레이어처럼 편리하게 청취할 수 있도록 만드는 개인용 Android 애플리케이션이다.

핵심 사용자 경험:

1. 음악 검색
2. 검색 결과 탐색
3. 곡 선택
4. 즉시 재생
5. 다른 앱으로 이동해도 재생 유지
6. 화면 OFF 상태에서도 재생 유지
7. 잠금화면 / 알림 / Bluetooth에서 재생 제어

제품은 Spotify, FLO와 같은 스트리밍 서비스 자체를 만드는 것이 아니라,

**Android Media Player UX + Replaceable Music Source Provider**

구조의 경량 플레이어를 만드는 것을 목표로 한다.

---

# 2. Project Scope

## 2.1 Primary Goal

다음 사용자 흐름을 안정적으로 구현한다.

```text
앱 실행
  ↓
음악 검색
  ↓
검색 결과
  ↓
곡 선택
  ↓
재생
  ↓
Home 버튼
  ↓
계속 재생
  ↓
Screen Off
  ↓
계속 재생
  ↓
Lock screen
  ↓
Play / Pause / Next
```

---

# 3. Distribution Boundary

이 프로젝트는 다음 조건을 전제로 한다.

- 개인 사용
- 개인 학습
- APK sideload
- 비상업적
- 광고 없음
- 결제 없음
- 공개 앱스토어 배포 제외

MVP에는 다음을 포함하지 않는다.

- Google Play 출시
- App Store 출시
- 상업적 배포
- SaaS 서비스
- 다중 사용자 계정
- 서버 기반 사용자 데이터

향후 배포 전략을 변경하려면 Source Provider 및 콘텐츠 이용 정책을 별도로 재검토한다.

---

# 4. Policy Boundary

YouTube 계열 콘텐츠를 사용하는 경우 공식 API 기반 구현과 비공식 Source Provider 구현을 동일하게 취급하지 않는다.

프로젝트 핵심 요구사항인 백그라운드 음악 재생을 공식 YouTube API만으로 해결하려는 설계를 전제로 하지 않는다.

Source Provider 구현은 앱의 나머지 부분에서 반드시 격리한다.

---

# 5. Product Principles

1. **Playback reliability first**
2. **Background-first architecture**
3. **Replaceable source provider**
4. **Local-first user data**
5. **Minimal dependencies**
6. **Testable architecture**
7. **Codex-friendly task decomposition**
8. **No premature features**

---

# 6. Target User

Android에서 YouTube 기반으로 음악을 자주 듣는 사용자.

대표 사용 상황:

- 카카오톡을 하면서 음악 듣기
- 웹 브라우징하면서 음악 듣기
- 화면을 끄고 음악 듣기
- 블루투스 이어폰으로 음악 제어
- 자동차 Bluetooth media control 사용
- YouTube Music 곡 검색
- 일반 YouTube의 라이브/커버 음악 검색

---

# 7. Supported Environment

Minimum:

- Android 8.0
- API 26

Project bootstrap 시:

- latest stable compileSdk / targetSdk
- JDK 17+
- Kotlin
- Jetpack Compose
- Gradle Kotlin DSL
- Version Catalog

---

# 8. MVP Scope — v0.1.0

필수 기능:

- Search
- Search result
- Play
- Pause
- Seek
- Previous
- Next
- Queue
- Shuffle
- Repeat
- Background playback
- Screen-off playback
- Notification control
- Lock-screen control
- Bluetooth media control
- Mini player
- Now Playing screen
- Favorites
- Local playlist
- Play history
- Search history
- Dark mode

---

# 9. Explicit Non-Goals — v0.1

- Login
- YouTube account sync
- YouTube likes/subscriptions sync
- Comments
- Shorts
- Video upload
- Cast
- Android Auto
- Lyrics
- Equalizer
- SponsorBlock
- Cloud sync
- User account
- Recommendation AI
- Social features
- Permanent offline downloads

Streaming cache와 permanent download는 명확히 구분한다.

---

# 10. Core Architecture

```text
Compose UI
   ↓
ViewModel
   ↓
UseCase / Repository
   ↓
Domain Interfaces
   ↓
Infrastructure Implementations
```

---

# 11. Initial Project Structure

```text
keuney-music/
├── AGENTS.md
├── PRD.md
├── ARCHITECTURE.md
├── TASKS.md
├── README.md
├── TESTING.md
├── LICENSE.md
├── THIRD_PARTY_NOTICES.md
├── app/
├── core/
│   ├── model/
│   ├── player/
│   ├── database/
│   └── common/
├── data/
│   └── source/
├── feature/
│   ├── search/
│   ├── player/
│   └── library/
└── docs/
    ├── DECISIONS.md
    ├── SOURCE_PROVIDER.md
    └── RELEASE.md
```

초기에는 과도한 Gradle 멀티모듈화를 피하고 논리적 패키지 경계를 우선한다.

---

# 12. Source Provider Contract

```kotlin
interface MusicSource {
    suspend fun search(query: String): Result<List<Track>>
    suspend fun resolveStream(trackId: String): Result<PlayableStream>
    suspend fun getTrack(trackId: String): Result<Track>
    suspend fun getRelated(trackId: String): Result<List<Track>>
}
```

앱의 다른 계층은 특정 provider의 DTO 또는 API 구조를 알면 안 된다.

---

# 13. Source Provider Strategy

## Provider A — InnerTube-compatible prototype

목적:

- 음악 검색
- Track metadata
- Stream resolution

장점:

- Kotlin으로 작게 구현 가능
- 음악 중심 메타데이터
- 의존성 최소화

위험:

- 비공개 API
- 응답 구조 변경
- client context 변경
- stream resolution 방식 변경

따라서 **POC 통과 전에는 최종 provider로 확정하지 않는다.**

## Provider B — Extractor adapter

Provider A가 안정성 또는 유지보수성 Gate를 통과하지 못하면 fallback으로 평가한다.

예:

```text
NewPipeMusicSource : MusicSource
```

GPL 계열 코드를 직접 포함하는 경우 라이선스 검토를 선행한다.

---

# 14. Provider Decision Gate

Provider A 합격 기준:

- search 성공
- resolveStream 성공
- 테스트 Track 10개 재생 성공
- 화면 OFF 상태 재생 성공
- 재해석/refresh 성공
- Queue 10곡 연속 재생 성공

실패하면 Provider B를 평가한다.

---

# 15. Domain Models

```kotlin
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationMs: Long?,
    val source: SourceType
)

data class PlayableStream(
    val url: String,
    val mimeType: String?,
    val bitrate: Int?,
    val expiresAt: Instant?
)
```

추가 모델:

- Playlist
- PlaylistItem
- HistoryEntry
- SearchHistoryEntry

---

# 16. Playback Architecture

```text
Compose UI
   ↓
MediaController
   ↓
MusicService : MediaLibraryService
   ↓
MediaLibrarySession
   ↓
ExoPlayer
   ↓
StreamResolver
   ↓
MusicSource.resolveStream()
```

---

# 17. Player Ownership Rule

ExoPlayer는 Activity 또는 ViewModel이 소유하지 않는다.

**Player owner:**

```text
MusicService : MediaLibraryService
```

Activity는 `MediaController` client다.

ViewModel은 UI 상태 변환과 사용자 의도 전달만 담당한다.

---

# 18. Media Service Responsibilities

`MusicService` 책임:

- ExoPlayer lifecycle
- MediaLibrarySession lifecycle
- Playback queue
- Media metadata
- Background playback
- Notification
- Media buttons
- Audio focus
- Playback error propagation

---

# 19. Manifest Requirements

필수 권한:

```text
android.permission.INTERNET
android.permission.FOREGROUND_SERVICE
android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK
```

Android 13+:

```text
android.permission.POST_NOTIFICATIONS
```

Service:

```text
foregroundServiceType="mediaPlayback"
```

---

# 20. Queue & MediaItem Rule

Queue는 Media3 playlist를 기반으로 관리한다.

MediaItem에는 다음만 유지한다.

- mediaId = Track.id
- title
- artist
- artwork

**resolved stream URL은 장기 저장하지 않는다.**

---

# 21. Stream Resolution

```text
Track ID
   ↓
Playback requested
   ↓
StreamResolver
   ↓
MusicSource.resolveStream(trackId)
   ↓
Playable URL
```

가능하면 playback 직전에 resolve 한다.

---

# 22. Stream Failure Recovery

고려할 오류:

- HTTP 401
- HTTP 403
- HTTP 404
- expired stream
- network disconnect
- malformed source response

기본 정책:

1. 최초 오류 감지
2. `resolveStream()` 다시 호출
3. 새 URL로 1회 재시도
4. 다시 실패하면 Player Error 처리
5. 무한 retry 금지

---

# 23. Search Flow

```text
SearchScreen
  ↓
SearchViewModel
  ↓
SearchRepository
  ↓
MusicSource.search()
  ↓
List<Track>
```

Search state:

- Idle
- Loading
- Success
- Empty
- Error

---

# 24. Player UI

## Mini Player

- artwork
- title
- play/pause
- tap → Now Playing

## Now Playing

- artwork
- title
- artist
- progress
- current time
- duration
- shuffle
- previous
- play/pause
- next
- repeat
- favorite
- queue

---

# 25. Notification / Lockscreen / Bluetooth

MediaSession을 통해 제공한다.

필수:

- artwork
- title
- artist
- play
- pause
- next
- previous

별도 커스텀 미디어 시스템을 만들지 않고 Media3 기본 기능을 우선한다.

---

# 26. Audio Focus

ExoPlayer AudioAttributes와 audio focus 처리를 활성화한다.

기대 동작:

- 전화 수신 → pause
- 일시적 audio focus loss → pause 또는 duck
- Bluetooth disconnect → pause
- headset unplug → pause

---

# 27. Background Acceptance

실기기 기준:

```text
Playback start
→ Home
→ 30 seconds
→ Playback continues
```

```text
Screen Off
→ 60 seconds
→ Playback continues
```

```text
Open another app
→ 5 minutes
→ Playback continues
```

---

# 28. OEM Battery Management

먼저 Android 표준 foreground media playback 구조를 올바르게 구현한다.

배터리 최적화 제외 요청을 앱 시작 시 강제하지 않는다.

실기기에서 실제 문제가 재현되는 경우 Settings에 안내 기능을 추가한다.

---

# 29. Local Storage

Room tables:

- tracks
- favorites
- playlists
- playlist_items
- playback_history
- search_history

DataStore:

- theme
- repeat mode
- cache size
- history enabled

---

# 30. Cache

v0.1에서는 Media3 streaming cache를 선택적으로 사용한다.

기본 목표:

- 중복 네트워크 요청 감소
- seek/replay 효율 개선

Cache는 LRU 삭제 가능해야 한다.

Permanent download는 v0.1 범위 밖이다.

---

# 31. Technology Stack

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Media3
- ExoPlayer
- MediaLibraryService
- MediaLibrarySession
- Ktor Client
- OkHttp engine
- kotlinx.serialization
- Room
- DataStore
- Hilt
- Coil
- Gradle Kotlin DSL
- Version Catalog

---

# 32. Dependency Policy

새 dependency 추가 전:

1. 기존 dependency로 해결 가능한지 확인
2. 꼭 필요한 경우에만 추가
3. Version Catalog 사용
4. 이유를 `docs/DECISIONS.md`에 기록

alpha/snapshot dependency는 특별한 사유가 없으면 금지한다.

---

# 33. Error Model

raw exception을 UI까지 직접 전달하지 않는다.

예:

```kotlin
sealed interface AppError {
    data object Network : AppError
    data object SourceUnavailable : AppError
    data object PlaybackUnavailable : AppError
    data object GeoRestricted : AppError
    data object Unknown : AppError
}
```

---

# 34. Logging & Security

금지:

- cookie 로그
- auth token 로그
- full resolved stream URL query parameter 로그
- credentials 저장

v0.1에서는 login을 구현하지 않는다.

---

# 35. Navigation

Bottom navigation:

- Home
- Search
- Library

Home:

- Recently Played
- Favorites
- Playlists

---

# 36. UI State

ViewModel은 immutable UI state를 노출한다.

Compose가 Repository 또는 MusicSource에 직접 접근하는 것을 금지한다.

기본 state primitive:

`StateFlow<UiState>`

---

# 37. Testing Strategy

## Unit

- Queue
- Repository
- UseCase
- ViewModel
- Mapper

## Source Contract

실제 provider 호출:

```text
search()
resolveStream()
```

## Playback Integration

Service → ExoPlayer → playing state

## Real Device Acceptance

- Home background
- Screen off
- Lock screen
- Bluetooth
- Activity destroyed

---

# 38. CI

Required:

```bash
./gradlew test
./gradlew lint
./gradlew assembleDebug
```

Release candidate:

```bash
./gradlew assembleRelease
```

---

# 39. Codex Workflow

Codex에는 전체 앱 구현을 한 번에 맡기지 않는다.

Task 단위:

```text
Read:
AGENTS.md
PRD.md
ARCHITECTURE.md
TASKS.md
docs/DECISIONS.md

Implement only the selected KM task.
Do not implement future tasks.
Run required verification.
Report changed files, commands, results, risks.
```

---

# 40. Milestones

## M0 — Environment

- JDK
- SDK
- adb
- Emulator
- Git

## M1 — Bootstrap

- Android app
- Compose
- Material3
- Hilt
- Room
- DataStore
- Media3
- Ktor
- Coil
- CI

## M2 — Playback Foundation

Provider 없이 known test audio로:

- MusicService
- MediaLibrarySession
- ExoPlayer
- MediaController
- Notification
- Background playback
- Screen-off playback

## M3 — Source Provider POC

- MusicSource
- search
- resolveStream
- 실제 음악 재생
- provider gate

## M4 — Source Hardening

- retry
- error mapping
- contract tests
- fallback abstraction

## M5 — Player UX

- Mini Player
- Now Playing
- Queue
- Seek
- Shuffle
- Repeat

## M6 — Library

- Favorites
- Playlists
- History
- Search history

## M7 — Stability

- Network recovery
- Bluetooth
- headset
- cache
- background regression

## M8 — Release

- Dark theme
- Settings
- OSS licenses
- README
- release APK

---

# 41. MVP Definition of Done

모두 PASS 해야 한다.

1. 검색 결과 표시
2. 곡 선택 후 재생
3. Home 후 재생 유지
4. Screen Off 후 재생 유지
5. Lock screen Pause/Resume
6. Bluetooth Play/Pause
7. Next 동작
8. Playlist persistence
9. 네트워크 오류에 명확한 복구 또는 오류 표시
10. Activity 종료 후 정상 조건에서 Service playback 유지
11. unit tests PASS
12. lint PASS
13. debug build PASS
14. release build PASS
15. source contract tests PASS
16. real-device background playback PASS

---

# 42. Final Product Boundary

Keuney Music의 핵심 제품은 특정 YouTube 파서가 아니라,

**안정적인 Android 음악 플레이어**다.

외부 Source Provider가 변경되어도 다음은 영향을 최소화해야 한다.

- Player
- UI
- Library
- Queue
- History
- Playlist

---

# 43. Final MVP Statement

> 사용자가 음악을 검색하고 곡을 선택한 뒤 스마트폰 화면을 끄거나 다른 앱을 사용해도 안정적으로 음악을 들을 수 있으며, 잠금화면·알림·Bluetooth를 통해 재생을 제어할 수 있는 개인용 Android 음악 플레이어를 완성한다.
