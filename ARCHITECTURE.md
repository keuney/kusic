Keuney Music Architecture

Version: 1.0
Status: Baseline architecture
Related: PRD.md, AGENTS.md, TASKS.md

1. Architecture Goals

Keuney Music 아키텍처는 다음 문제를 해결해야 한다.

Activity와 무관한 안정적인 background playback

화면 OFF 상태 playback 유지

특정 콘텐츠 Source 구현에 대한 결합 최소화

stream URL 만료에 대한 대응

로컬 playlist / favorites / history

Codex가 작은 Task 단위로 안전하게 수정 가능한 구조

향후 Source Provider 교체 가능성 확보

2. Architecture Principles

2.1 Service owns playback

ExoPlayer는 MusicService : MediaLibraryService가 소유한다.

2.2 UI is a client

Compose UI는 MediaController를 통해 playback state를 구독하고 명령을 전달한다.

2.3 Domain is provider-neutral

Domain 모델은 YouTube, InnerTube, NewPipe 등 특정 구현명을 포함하지 않는다.

2.4 Source is replaceable

외부 콘텐츠 접근은 MusicSource 인터페이스 뒤에 둔다.

2.5 Stream URL is ephemeral

resolved URL은 permanent state가 아니다.

2.6 Local user data is persistent

Favorites, playlists, history는 Room에 저장한다.

3. High-Level Diagram

┌─────────────────────────────────────┐
│             Compose UI              │
│ Home / Search / Library / Player    │
└──────────────────┬──────────────────┘
                   │
              ViewModels
                   │
            Repositories
         ┌─────────┴───────────┐
         │                     │
      Local Data            Playback
         │                     │
       Room             MediaController
                               │
                        MusicService
                               │
                     MediaLibrarySession
                               │
                           ExoPlayer
                               │
                        StreamResolver
                               │
                           MusicSource
                         ┌─────┴─────┐
                         │           │
                    Provider A   Provider B

4. Suggested Source Tree

app/
  MainActivity.kt
  KeuneyApp.kt
  navigation/
  di/

core/
  model/
    Track.kt
    Playlist.kt
    PlayableStream.kt
    AppError.kt
  common/
    ResultExt.kt
    DispatcherProvider.kt
  player/
    MusicService.kt
    PlayerConnection.kt
    StreamResolver.kt
    PlaybackStateMapper.kt
  database/
    KeuneyDatabase.kt
    entity/
    dao/

data/
  repository/
    SearchRepositoryImpl.kt
    LibraryRepositoryImpl.kt
  source/
    MusicSource.kt
    providerA/
      ProviderAClient.kt
      api/
      dto/
      mapper/
    providerB/
      NewPipeMusicSource.kt

feature/
  home/
  search/
  player/
  library/

docs/
  DECISIONS.md
  SOURCE_PROVIDER.md
  RELEASE.md

초기 repository 크기가 작다면 core/, data/, feature/는 Gradle module이 아닌 package directory로 시작해도 된다.

5. Layer Responsibilities

UI Layer

책임:

화면 렌더링

사용자 입력

ViewModel state 구독

navigation

금지:

network 직접 호출

DAO 직접 호출

MusicSource 직접 호출

ExoPlayer 소유

ViewModel Layer

책임:

UiState

user intent 처리

repository 호출

error → UI 표현 변환

금지:

source DTO 사용

Room DAO 직접 호출

ExoPlayer 소유

Repository Layer

책임:

domain operation orchestration

local/remote coordination

cache strategy

Infrastructure Layer

책임:

Ktor

provider-specific API

Room

Media3

Android service

6. Core Domain Interfaces

interface MusicSource {
    suspend fun search(query: String): Result<List<Track>>
    suspend fun getTrack(trackId: String): Result<Track>
    suspend fun resolveStream(trackId: String): Result<PlayableStream>
    suspend fun getRelated(trackId: String): Result<List<Track>>
}

interface SearchRepository {
    suspend fun search(query: String): Result<List<Track>>
}

interface LibraryRepository {
    fun observeFavorites(): Flow<List<Track>>
    suspend fun addFavorite(track: Track)
    suspend fun removeFavorite(trackId: String)
}

Player control은 Media3 MediaController 경계를 우선 사용한다.

7. Playback Component Diagram

MainActivity / Compose
       │
       │ connect
       ▼
MediaController
       │
       ▼
MusicService : MediaLibraryService
       │
       ├── MediaLibrarySession
       │
       ├── ExoPlayer
       │
       └── StreamResolver
                 │
                 ▼
             MusicSource

8. MusicService Lifecycle

Service 생성:

ExoPlayer 생성

AudioAttributes 설정

MediaLibrarySession 생성

callbacks 연결

Playback active:

Service owns player

notification / media session 활성

Activity 없어도 playback 가능

Service 종료:

player release

session release

cache/reference cleanup

9. MediaController Lifecycle

Activity 또는 app UI process는 SessionToken으로 Service에 연결한다.

연결 성공:

playback state 관찰

metadata 관찰

transport controls 사용

Activity destroy:

controller 연결 해제 가능

재생 중 Service는 독립적으로 유지

10. MediaItem Design

MediaItem에는 permanent identity와 display metadata만 둔다.

mediaId = Track.id
title
artist
artworkUri
duration metadata if known

resolved stream URL을 identity로 사용하지 않는다.

11. StreamResolver Design

목표:

Track ID → fresh playable URL

pseudo flow:

Player asks for media
↓
StreamResolver receives Track ID
↓
MusicSource.resolveStream(trackId)
↓
PlayableStream
↓
DataSource loads URL

URL 오류:

Playback load error
↓
is refreshable?
↓ yes
resolveStream again
↓
retry once

12. Retry Policy

기본 원칙:

network transient error: 제한된 retry

expired/forbidden stream: re-resolve 후 1회

malformed source response: retry하지 않고 source error

permanent not-found: skip/error

infinite retry 금지

Retry 횟수와 backoff는 한 곳에서 관리한다.

13. Source Provider Isolation

Provider A package 외부로 노출 금지:

request DTO

response DTO

endpoint path

client context

parser logic

provider-specific constants

외부에 전달 가능한 것:

Track

PlayableStream

AppError

14. Provider Decision Gate

M3/M4에서 다음을 평가한다.

search stability

stream resolution

response parsing complexity

maintenance burden

rate/403 behavior

10-track queue reliability

Provider A가 Gate 실패 시 Provider B adapter를 평가한다.

15. Search Architecture

SearchScreen
  ↓ user query
SearchViewModel
  ↓
SearchRepository
  ↓
MusicSource.search()
  ↓
Track[]
  ↓
SearchUiState.Success

ViewModel:

data class SearchUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val error: UiError? = null
)

16. Library Architecture

Compose
  ↓
LibraryViewModel
  ↓
LibraryRepository
  ↓
Room DAO

Room entities:

TrackEntity

FavoriteEntity

PlaylistEntity

PlaylistItemEntity

PlaybackHistoryEntity

SearchHistoryEntity

Track metadata는 중복 방지를 위해 정규화 가능하다.

17. Database Identity

Track의 source-independent identity를 임의 생성하지 않는다.

기본적으로:

(sourceType, trackId)

조합을 안정적인 식별자로 취급한다.

Provider 교체 후 동일 콘텐츠 매칭은 v0.1 목표가 아니다.

18. Queue Architecture

초기 queue source of truth는 Media3 player playlist로 둔다.

앱 종료 후 queue 복원은 후순위 옵션이다.

Queue operations:

add

add next

remove

reorder

next

previous

shuffle

repeat

QueueManager 같은 추가 abstraction은 필요성이 확인될 때 도입한다.

19. Playback State Mapping

Media3 state를 UI state로 변환하는 mapper를 둔다.

예:

IDLE
BUFFERING
READY
ENDED
ERROR

UI는 ExoPlayer raw state constants를 직접 해석하지 않는다.

20. Audio Focus

ExoPlayer AudioAttributes를 이용한다.

Expected:

incoming call → pause

transient loss → pause/duck

regain → 정책에 따라 resume

headset unplug → pause

Bluetooth disconnect → pause

자동 resume 여부는 UX 검증 후 결정한다.

21. Notification

가능하면 Media3 session notification 기본 흐름을 사용한다.

커스텀 notification은 다음이 필요할 때만 도입한다.

기본 action으로 충족 불가

UX 요구가 명확함

regression test 준비됨

22. Cache Architecture

Optional Media3 cache:

Network DataSource
      ↓
CacheDataSource
      ↓
SimpleCache

Cache 특성:

disposable

LRU

user-clearable

permanent library가 아님

기본 제한은 256MB를 시작점으로 사용할 수 있다.

23. Error Architecture

Infrastructure:

IOException
HTTP status
SerializationException
Provider-specific error

↓

Mapper

↓

AppError.Network
AppError.SourceUnavailable
AppError.PlaybackUnavailable
AppError.GeoRestricted
AppError.Unknown

↓

UI-friendly message

24. Dependency Injection

Hilt 사용.

대표 binding:

MusicSource → ProviderAMusicSource
SearchRepository → SearchRepositoryImpl
LibraryRepository → LibraryRepositoryImpl

MusicService에 필요한 player dependencies는 service-compatible scope를 사용한다.

Singleton 남용 금지.

25. Coroutine Policy

UI/ViewModel: viewModelScope

Repository: suspend/Flow

blocking I/O: Dispatchers.IO

Main thread blocking 금지

callback API는 적절한 suspend/Flow adapter 사용

GlobalScope 금지.

26. Network Layer

Ktor + OkHttp engine + kotlinx.serialization.

원칙:

timeout 명시

provider-specific headers 한 곳에서 관리

full stream URL logging 금지

retry policy 중복 금지

parsing DTO와 domain mapper 분리

27. Security Boundary

v0.1:

no login

no token storage

no cookie persistence

no credential flow

향후 login은 별도 보안 설계가 선행되어야 한다.

28. Testing Architecture

Unit

pure Kotlin 중심:

mapper

queue logic

repositories

ViewModel

error mapping

Integration

Room

playback service where practical

Contract

provider search

provider resolve

Device

screen off

background

lockscreen

Bluetooth

29. Source Contract Test Separation

외부 네트워크 의존 테스트는 unit test에 섞지 않는다.

예:

src/sourceContractTest/

또는 별도 Gradle task.

CI에서는 scheduled/manual 실행도 가능하다.

30. Architecture Change Process

Architecture 변경 시:

PRD와 현재 architecture 확인

변경 필요성 기술

docs/DECISIONS.md ADR 작성

최소 변경

테스트

문서 업데이트

31. Initial ADR List

ADR-001 Kotlin + Compose

ADR-002 MediaLibraryService owns playback

ADR-003 MusicSource abstraction

ADR-004 Source Provider choice

ADR-005 Stream URL not persisted

ADR-006 Room local-first storage

ADR-007 No login in v0.1

ADR-008 No permanent download in v0.1

32. Non-Functional Requirements

Reliability

Background playback이 앱 UI 생명주기와 분리되어야 한다.

Maintainability

Source changes가 player/UI에 전파되지 않아야 한다.

Observability

Debug에서 다음 확인 가능:

current track

player state

queue index

source operation success/failure

Privacy

사용자 계정 없이 동작.

33. Performance Goals

초기 목표:

cold launch: 가능하면 2초 이내 첫 UI

search: provider 응답 즉시 표시

playback start: 가능하면 2~3초 이내

외부 Source 성능 때문에 초기에는 hard Gate로 쓰지 않는다.

34. Final Architecture Constraint

다음 의존성 방향을 유지한다.

feature/UI
   ↓
domain/repository interfaces
   ↓
data/player infrastructure

반대 방향 의존 금지.

특정 provider의 변경이 UI rewrite를 요구한다면 architecture violation으로 간주한다.