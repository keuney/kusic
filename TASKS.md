Keuney Music — Codex Execution Backlog

Status legend:

[ ] Not started

[-] In progress

[x] Done

[!] Blocked

Codex는 항상 AGENTS.md를 먼저 읽고 한 번에 하나의 Task만 수행한다.

---

## 재개 지점 (2026-09-03 기준)

새 세션은 이 절과 아래 상태 표시를 먼저 본다. 작업별 상세 결과는 각 Task의 완료 기록에, 기술 결정은 docs/DECISIONS.md에, 시행착오까지 포함한 전체 흐름은 docs/SEQUENTIAL_RUN.md에 있다.

**현재: 완료 52 / 미착수 25. 보류 없음. 작업 트리 깨끗하고 브랜치는 main 하나이며 origin/main과 같다.** 상태 표시를 세어 확인한 값이며 미착수 25에는 최종 게이트 KM-200이 포함된다.

**다음 작업: KM-096 (Repeat).** 인수 조건은 off·one·all·state persistence optional이다. 반복 상태를 읽는 것은 KM-090에서 이미 되어 있고(`PlaybackState.repeatMode`) 이번에 세 상태를 도는 토글을 붙인다. KM-095의 셔플 칩 옆에 같은 방식으로 두면 된다. 착수 시 정할 것: state persistence는 optional인데 PRD 34의 DataStore 항목에는 반복 모드가 들어 있다. 지금 저장할지(설정 저장소가 이미 있으니 어렵지 않다) 미룰지 정해야 한다.

**M6 진행 상황:** KM-090~095 완료. 남은 것은 KM-096(반복)·097(Queue UI)다.

**KM-097 착수 시 알아야 할 제약(ADR-053):** 컨트롤러가 받는 Timeline에는 셔플 순서가 실려 오지 않는다. 대기열 화면은 넣은 순서만 보여줄 수 있고 셔플이 켜졌을 때의 실제 재생 순서는 보여줄 수 없다. 순서를 보여줄지, 셔플 중에는 순서 표시를 감출지 정해야 한다. 또한 화면에서 대기열을 만드는 경로가 아직 없다(playTrack은 setMediaItem으로 한 곡을 갈아 끼우고, playQueue는 Gate 검증용이다). 그 경로를 KM-097에서 만든다.

**화면 구성 현황:** 하단 탭 홈·검색·라이브러리에 전체 화면 `now-playing`. 홈(KM-151)과 라이브러리(KM-116)는 자리표시자다. Now Playing 조작 줄은 즐겨찾기(비활성)·이전·재생/일시정지·다음(대기열이 한 곡이라 비활성)·대기열(비활성)이고 그 아래 셔플 칩이 있다. 즐겨찾기는 KM-112, 대기열은 KM-097에서 살린다. WiFi 전용 스위치는 Now Playing에 있고 KM-153 설정 화면으로 옮긴다.

**KM-110 착수 시 다시 볼 것:** KM-110의 엔티티 목록에 `SearchHistoryEntity`가 있으나 KM-074에서 최근 검색어를 DataStore에 두기로 정했으므로(ADR-046) 필요하지 않다. 그 항목을 빼거나 검색어를 Room으로 옮기고 ADR-046을 대체할지 그때 판단한다.

**내장 테스트 음원은 artworkUri가 없어** 미니 플레이어에서 자리표시자 색으로 보인다. 알림은 기존대로 artworkData를 쓴다. `playQueue`는 이미지 주소를 받지 않는다(Gate 검증용 진입점).

**진행 방식(이 세션에서 사용자와 합의한 것):**

- 작업마다 `codex/KM-xxx-설명` 브랜치를 따고, 검증 통과 후 main으로 fast-forward 머지한 뒤 브랜치를 삭제한다.
- 작업 완료 시 TASKS.md 상태·완료 기록, docs/DECISIONS.md의 ADR, docs/SEQUENTIAL_RUN.md 기록, 필요하면 README를 함께 갱신한다.
- 필수 검증은 `test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`이며 종료 코드 0을 확인한다.

**백로그 순서를 벗어난 작업:** KM-134(스트리밍 캐시)와 KM-137(네트워크 사용 정책)은 M8 소속이지만 progressive 전환의 대역폭 부담을 줄이기 위해 사용자 요청으로 앞당겨 완료했다. KM-137은 이 과정에서 새로 추가한 작업이다. KM-150(앱 내비게이션)은 M9 소속이지만 KM-092가 갈 수 있는 화면을 필요로 해 사용자 요청으로 앞당겨 완료했다. KM-064(Provider B 평가)는 KM-059 Gate가 PASS라 활성화하지 않는다.

**원격 저장소:** `origin`은 https://github.com/keuney/kusic.git 이다. 2026-09-03 사용자 요청으로 첫 push를 했다. 그 전에 로컬 `master`를 `main`으로 바꿨으므로 본선은 `main`이고 upstream은 `origin/main`이다. push는 외부 공개에 해당하므로 이후에도 사용자가 명시적으로 요청할 때만 한다. push 전에 추적 파일에 키·토큰·자격증명이 없는지 확인했다.

---



M0 — Environment Verification

KM-001 — Repository baseline

Status: [x]

재판정 (2026-09-02): 보류 사유였던 필수 Gradle 검증을 KM-010·KM-011 완료 후 재실행해 통과했다. `gradlew test lint assembleDebug` PASS(오프라인 29초). 인수 조건 3개는 최초 진행 시 이미 충족했고 현재도 유지된다. 최초 커밋 87aecb0으로 저장소 기반을 확정했다. 완료로 전환한다.

진행 기록 (2026-09-02):

저장소 기반 구현 및 아래 인수 조건 3개 충족. AGENTS.md 18/27의 필수 Gradle 검증을 통과할 수 없어 완료 처리는 보류한다.

- Git 저장소 초기화 완료. git status 성공.
- 기존 필수 문서 4개 확인, README 초안 및 docs/DECISIONS.md 추가.
- .gitignore 추가. git check-ignore로 제외 대상 18개 및 추적 가능 대상 8개 확인.
- ./gradlew test: 실패 — Gradle Wrapper 실행 파일 없음.
- ./gradlew lint: 실패 — Gradle Wrapper 실행 파일 없음.
- ./gradlew assembleDebug: 실패 — Gradle Wrapper 실행 파일 없음.
- 원인: 초기 폴더에는 문서 4개만 있었으며 Gradle 구성은 KM-010, 앱 모듈은 KM-011 범위다. 이번 작업에서 후속 작업을 구현하지 않는다.
- Git 명령은 성공했으나 사용자 전역 제외 파일 접근 권한 경고가 발생했다. 확인 결과는 저장소의 .gitignore 기준이다.
- 최종 확인 중 실행 계정과 .git 소유자가 달라 일반 git status가 거부되었다. 현재 경로만 지정한 git -c safe.directory=D:/uuh_workspace/keuney_music status는 성공했다. 전역 Git 설정은 변경하지 않았다.
- 비즈니스 로직 변경 없음. 단위 테스트 추가 및 실기기 검증 대상 없음.

Goal:

프로젝트 저장소의 기본 문서와 디렉터리 구조를 준비한다.

Work:

git repository 상태 확인

root 문서 확인

docs/ 생성

.gitignore 준비

README skeleton

Acceptance Criteria:

repository root에 AGENTS.md, PRD.md, ARCHITECTURE.md, TASKS.md 존재

docs/DECISIONS.md 존재

.gitignore에 Android/Gradle/IDE secret 및 build output 포함

Verification:

git status

KM-002 — Android toolchain verification

Status: [x]

재판정 (2026-09-02): 보류 사유는 당시 Wrapper 부재로 인한 필수 Gradle 검증 실패였으며 KM-010·KM-011 이후 해소됐다. 인수 조건 재확인 — `java -version` OpenJDK 17.0.18 Temurin PASS, `adb version` 1.0.41 / 35.0.2 PASS, `adb devices` R9PRB0PNLVT device PASS. 기록은 docs/ENVIRONMENT.md에 유지된다. 완료로 전환한다.

진행 기록 (2026-09-02):

- 사용자 지시에 따라 KM-001은 보류하고 KM-002만 수행했다.
- JDK 17.0.18, Android SDK, Platform Tools 35.0.2, Emulator 35.5.10 및 등록된 AVD 1개 확인.
- 기본 JAVA_HOME은 JDK 11이며 adb는 PATH에 없었다. 현재 검증 프로세스에만 JDK 17, SDK, AVD 경로를 지정해 재검증했다. 사용자·시스템 환경 변수는 변경하지 않았다.
- java -version, adb version, adb devices 성공. 연결된 기기는 없으며 에뮬레이터 부팅은 수행하지 않았다.
- 인수 조건과 상세 명령·결과를 docs/ENVIRONMENT.md에 기록했다.
- ./gradlew test, ./gradlew lint, ./gradlew assembleDebug는 Wrapper가 없어 모두 실행 실패. AGENTS.md 18/27에 따라 전체 완료 처리는 보류한다.
- KM-003 및 이후 작업은 진행하지 않았다.

Goal:

Android 개발 환경을 확인한다.

Check:

JDK 17+

Android SDK

platform-tools

adb

emulator

available AVD

Acceptance Criteria:

java -version
adb version
adb devices

결과를 docs/ENVIRONMENT.md에 기록.

KM-003 — Emulator boot verification

Status: [x]

재판정 (2026-09-02): 보류 사유는 당시 Wrapper 부재로 인한 필수 Gradle 검증 실패였으며 KM-010·KM-011 이후 해소됐다. 인수 조건 3개는 Medium_Phone_API_36.0 AVD 부팅·`adb devices` device·screencap으로 이미 충족했고 docs/EMULATOR.md에 기록돼 있다. AVD는 현재도 등록돼 있으며 이후 KM-011~040에서 에뮬레이터·실기기 설치와 실행을 반복 확인했다. 완료로 전환한다.

진행 기록 (2026-09-02):

- KM-001·KM-002의 Gradle 검증 보류 상태를 유지하고 KM-003만 수행했다.
- 기존 Medium_Phone_API_36.0 AVD를 사용자 계정 권한으로 부팅했다. WHPX 사용 가능, sys.boot_completed=1, bootanim=stopped 확인.
- adb devices에서 emulator-5554가 device 상태인 것을 확인했다.
- screencap 및 pull 성공. 1080×2400 PNG를 직접 열어 Android 홈 화면을 확인했다.
- 인수 조건 3개 모두 통과. 명령·결과·재현 절차는 docs/EMULATOR.md에 기록했다.
- 검증용 에뮬레이터는 정상 종료했고 로컬 캡처는 captures/km-003/boot.png에 유지했다.
- ./gradlew test, ./gradlew lint, ./gradlew assembleDebug는 Wrapper 부재로 실행 실패. AGENTS.md 18/27에 따라 전체 완료 처리는 보류한다.
- 앱 설치·실행 및 실기기 재생은 미검증이며 KM-010 이후 작업은 진행하지 않았다.

Goal:

개발용 emulator에서 앱 설치/실행 가능한 상태를 만든다.

Acceptance Criteria:

AVD boot 성공

adb devices에서 device 확인

screen capture 명령 동작

M1 — Project Bootstrap

KM-010 — Gradle wrapper and settings

Status: [x]

재판정 (2026-09-02): 보류 사유였던 "앱 모듈 부재로 test/lint/assembleDebug 작업 없음"이 KM-011 완료로 해소됐다. 인수 조건 재확인 — Wrapper 4개 파일·settings.gradle.kts·build.gradle.kts·gradle/libs.versions.toml 존재 PASS, `gradlew tasks` BUILD SUCCESSFUL(8초) PASS. 필수 검증 `gradlew test lint assembleDebug` PASS. 완료로 전환한다.

진행 기록 (2026-09-02):

- Gradle 9.7.1 공식 Wrapper 4개 파일, settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml 추가. 인수 조건 4개 충족.
- Kotlin DSL과 Version Catalog를 연결하고 저장소 선언을 settings.gradle.kts로 중앙화했다. 앱 모듈과 외부 플러그인·라이브러리는 추가하지 않았다.
- 배포 ZIP 체크섬을 고정했고 Wrapper JAR SHA-256이 공식 값과 일치함을 확인했다. .gitattributes에 줄바꿈 규칙을 추가했다.
- gradlew의 Git 실행 권한 100755를 기록하기 위해 해당 파일만 스테이징했다. 커밋·업로드는 수행하지 않았다.
- 설치된 Gradle 8.14.1로 wrapper 실행: 최초 배포 URL 접근 실패 후 네트워크 권한을 허용한 재실행에서 성공.
- .\gradlew.bat wrapper --no-daemon --console=plain: Gradle 9.7.1을 내려받아 Wrapper 재생성 성공. 배치 파일 자체 교체 중 일시적 명령 해석 메시지가 있었으나 종료 코드 0이며, 생성 완료 후 아래 명령으로 최종 파일을 재검증했다.
- .\gradlew.bat --version: 통과, Gradle 9.7.1 및 JDK 17.0.18 확인.
- .\gradlew.bat tasks --no-daemon --offline --console=plain: 통과, 종료 코드 0.
- .\gradlew.bat test --no-daemon --offline --console=plain: 실패, 종료 코드 1, test 작업 없음.
- .\gradlew.bat lint --no-daemon --offline --console=plain: 실패, 종료 코드 1, lint 작업 없음.
- .\gradlew.bat assembleDebug --no-daemon --offline --console=plain: 실패, 종료 코드 1, assembleDebug 작업 없음.
- 모든 Gradle 검증은 현재 프로세스의 JAVA_HOME을 설치된 JDK 17로, GRADLE_USER_HOME을 프로젝트의 .gradle/user-home으로 지정했다. 전역 설정은 변경하지 않았다.
- 공통 필수 검증은 KM-011의 앱 모듈 구성이 필요하므로 AGENTS.md 18/27에 따라 최종 완료 처리는 보류한다. 빈 대체 작업을 만들지 않았다.
- 결정은 docs/DECISIONS.md의 ADR-009에 기록했고 README의 실행 방법을 갱신했다. KM-011 이후 작업은 진행하지 않았다.

Goal:

Android project Gradle baseline 생성.

Acceptance Criteria:

Gradle wrapper 존재

settings.gradle.kts

root build.gradle.kts

gradle/libs.versions.toml

Verification:

./gradlew tasks

KM-011 — Android app module

Status: [x]

완료 기록 (2026-09-02):

- :app Android 애플리케이션 모듈, Kotlin MainActivity, Manifest 런처 등록 및 앱 이름 리소스를 추가했다.
- applicationId/namespace com.keuney.music, minSdk 26, 안정 compileSdk 37.2 및 targetSdk 37 설정. 인수 조건 4개 모두 통과.
- AGP 9.4.0을 Version Catalog에 등록하고 내장 Kotlin을 사용했다. 플랫폼 SDK 37.2를 설치했으며 Gradle 9.7.1/JDK 17.0.18로 검증했다.
- .\gradlew.bat help --no-daemon --console=plain: 통과.
- .\gradlew.bat test lint assembleDebug assembleRelease --continue --no-daemon --console=plain: 통과, 종료 코드 0, 87개 작업 실행.
- testDebugUnitTest는 NO-SOURCE다. 비즈니스 로직과 단위 테스트 소스가 없어 실행된 테스트는 0개다.
- lint: 오류 0개, DataExtractionRules 경고 1개. 경고의 원인과 범위를 ADR-010에 기록했으며 린트 억제는 추가하지 않았다.
- apkanalyzer manifest print로 APK의 패키지, 최소/대상 SDK 및 MAIN/LAUNCHER Activity를 확인했다. 릴리스 APK는 debuggable=false이며 서명되지 않은 산출물이다.
- API 36 에뮬레이터에서 adb install -r 성공, am start -W 결과 Status: ok, MainActivity의 topResumedActivity 및 프로세스 확인. 빈 화면 캡처는 captures/km-011/launch.png에 보관했다.
- 디버그 APK: app/build/outputs/apk/debug/app-debug.apk (874,070 바이트). SHA-256: a8b03ad79e8c2cb88948ca6369fd0a762139282b45e10b69ca932d772b0aa8db.
- aapt dump badging은 플랫폼 기본 아이콘 참조 해석 오류가 있어 apkanalyzer와 실제 설치·실행으로 산출물을 확인했다.
- 검증용 에뮬레이터는 정상 종료했다. API 26/37 및 실기기 실행은 미검증이다.
- 결정은 docs/DECISIONS.md ADR-010에 기록하고 README 빌드·실행 방법을 갱신했다. KM-012 이후 작업은 진행하지 않았다.

Goal:

app module을 생성하고 빈 Android app을 build한다.

Acceptance Criteria:

package com.keuney.music

minSdk 26

current stable compile/target SDK

launcher activity 존재

Verification:

./gradlew assembleDebug

KM-012 — Compose + Material 3

Status: [x]

완료 기록 (2026-09-02):

- Compose 컴파일러 플러그인과 `buildFeatures.compose`를 활성화했다.
- Material 3 밝은 테마와 화면 중앙의 `Keuney Music` 기본 문구를 구현했다.
- `gradlew.bat test lint assembleDebug assembleRelease --continue --no-daemon --console=plain` PASS. 단위 테스트는 소스가 없어 0개, 린트는 오류 0개·경고 4개다.
- `scripts/verify-km012.ps1` PASS: API 36 에뮬레이터에 설치 후 콜드 실행, UI 계층의 앱 이름 확인, 화면 캡처 육안 확인을 완료했다.
- 의존성과 검증 한계는 ADR-011에 기록했다. 기존 보류 작업의 상태는 유지하며 KM-013은 진행하지 않았다.

Goal:

Compose UI baseline 구성.

Acceptance Criteria:

Compose enabled

Material3 theme

앱 실행 시 Keuney Music placeholder 표시

Verification:

debug build

emulator launch

KM-013 — Hilt setup

Status: [x]

완료 기록 (2026-09-02):

- `KeuneyApp`에 `@HiltAndroidApp` 적용 및 Manifest 등록, `MainActivity`에 `@AndroidEntryPoint` 적용.
- Hilt 2.60.1 및 KSP 2.3.11을 Version Catalog에 등록하고 앱·계측 테스트 코드 생성을 구성했다.
- `HiltInjectionTest`에서 생성자 주입, Application Context 전달, 싱글턴 인스턴스 공유를 검증했다. 샘플은 androidTest에만 존재한다.
- 첫 빌드는 Gradle Metaspace 384MB 한도에서 메모리 부족으로 실패했다. 로그로 원인을 확인한 후 프로젝트 JVM 힙·메타스페이스 한도를 각각 1GB로 설정했다.
- 최종 `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest --continue --no-daemon --console=plain` PASS: 종료 코드 0, 31초. 계측 테스트 1개 통과, 실패·오류·건너뜀 0개. 단위 테스트는 소스가 없어 0개다.
- 린트 오류 0개·기존 경고 4개. API 36 에뮬레이터에서 기존 실행 검증 스크립트로 실제 앱의 설치·콜드 실행·기본 문구 표시도 확인했다.
- 모든 인수 조건 통과. 상세 결정과 재현 명령은 ADR-012 및 README에 기록했다. 기존 보류 상태와 KM-014 이후 작업은 변경하지 않았다.

Goal:

Dependency Injection baseline 추가.

Acceptance Criteria:

Application class 설정

Hilt plugin/configuration

sample injected dependency test 가능

build PASS

KM-014 — Room setup

Status: [x]

완료 기록 (2026-09-02): KeuneyDatabase v1, 빈 초기화 테이블, Hilt 제공 및 스키마 export 구성. 마이그레이션은 명시적 경로만 허용하며 ADR-013에 기록했다. test/lint/assembleDebug/assembleRelease/connectedDebugAndroidTest PASS. DB 생성·재열기 및 기존 Hilt 계측 테스트 통과. 자세한 명령·변경 파일은 docs/SEQUENTIAL_RUN.md 참조.

Goal:

Room dependency 및 empty database baseline 추가.

Acceptance Criteria:

KeuneyDatabase

migration policy 명시

instrumentation 또는 unit smoke test

KM-015 — DataStore setup

Status: [x]

완료 기록 (2026-09-02): SettingsRepository와 DataStore 구현, Hilt 단일 인스턴스 구성. 테마 기본값·모든 값 읽기/쓰기/재열기·미지원 값 fallback 단위 테스트 3개 PASS. Windows 파일 교체 실패를 명시적 OkioStorage로 해결했다. test/lint/assembleDebug/assembleRelease/connectedDebugAndroidTest PASS. ADR-014 및 docs/SEQUENTIAL_RUN.md 참조.

Goal:

Preferences infrastructure 추가.

Acceptance Criteria:

settings repository skeleton

theme preference read/write test

KM-016 — Media3 dependencies

Status: [x]

완료 기록 (2026-09-02): Media3 ExoPlayer/Session 1.11.0 등록. test/lint/assembleDebug/assembleRelease PASS (1분 51초). 재생 인스턴스는 아직 생성하지 않는다. ADR-015 참조.

Goal:

Media3 player/session dependency 추가.

Acceptance Criteria:

exoplayer dependency

session dependency

build PASS

KM-017 — Ktor networking baseline

Status: [x]

완료 기록 (2026-09-02): Hilt 공용 Ktor/OkHttp 클라이언트, JSON 직렬화, connect/socket/request timeout 및 로깅 미설정. MockEngine의 타임아웃 설정·JSON·취소 테스트 3개 PASS. 공통 test/lint/assembleDebug/assembleRelease PASS (2분 3초). ADR-016 참조.

Goal:

Ktor + OkHttp engine + serialization baseline 구성.

Acceptance Criteria:

reusable HttpClient provider

timeout 설정

sensitive logging disabled by default

KM-018 — Coil setup

Status: [x]

완료 기록 (2026-09-02): Coil Compose 3.4.0과 Artwork placeholder/error/fallback 컴포넌트 추가. 공통 test/lint/assembleDebug/assembleRelease PASS (2분 7초). 원격 이미지 fetcher는 미포함이며 ADR-017에 범위를 기록했다.

Goal:

Compose artwork loading baseline 추가.

Acceptance Criteria:

Coil Compose dependency

placeholder image composable 가능

KM-019 — CI baseline

Status: [x]

완료 기록 (2026-09-02): GitHub Actions workflow 추가 및 JDK/SDK 준비, test/lint/assembleDebug 구성. 로컬 동일 명령 PASS (10초). 인수 조건 충족. 원격 push/Actions 실행은 수행하지 않았으며 ADR-018에 구분 기록했다.

Goal:

GitHub Actions 기본 build pipeline.

Checks:

./gradlew test
./gradlew lint
./gradlew assembleDebug

Acceptance Criteria:

workflow file 존재

local commands PASS

M2 — Playback Foundation

KM-030 — MusicService skeleton

Status: [x]

검증 (2026-09-02): test/lint/assembleDebug/assembleRelease 및 API 36 서비스 바인딩 계측 테스트 PASS. Manifest 등록·mediaPlayback 유형·서비스 시작 인수 조건 충족.

Goal:

MusicService : MediaLibraryService 생성.

Acceptance Criteria:

manifest service 등록

foreground service type mediaPlayback

service starts without crash

KM-031 — ExoPlayer ownership

Status: [x]

검증 (2026-09-02): test/lint/assembleDebug/connectedDebugAndroidTest PASS. ExoPlayer 참조는 MusicService에만 있으며 실제 바인딩·해제 후 같은 인스턴스의 Init/Release 로그 확인.

Goal:

ExoPlayer를 MusicService가 생성/소유/해제하도록 구현.

Acceptance Criteria:

Activity/ViewModel에 ExoPlayer 없음

service destroy 시 release

KM-032 — MediaLibrarySession

Status: [x]

검증 (2026-09-02): test/lint/assembleDebug/connectedDebugAndroidTest PASS. 실제 컨트롤러 연결·플레이어 상태 조회 및 세션/플레이어 종료 해제 확인.

Goal:

MusicService에 MediaLibrarySession 구성.

Acceptance Criteria:

session 생성

player 연결

service destroy 시 session release

KM-033 — MediaController connection

Status: [x]

검증 (2026-09-02): test/lint/assembleDebug/connectedDebugAndroidTest PASS. 연결 중 취소·중복 연결/해제·재연결 및 StateFlow 관찰 확인. 실제 앱 설치/콜드 실행 PASS. Activity는 컨트롤러만 해제하고 서비스 재생을 중단하지 않는다.

Goal:

UI에서 MusicService에 연결하는 controller layer 작성.

Acceptance Criteria:

Activity 재생 lifecycle과 player lifecycle 분리

connect/disconnect 안전

state observable

KM-034 — Known test audio playback

Status: [x]

검증 (2026-09-02): test/lint/assembleDebug/assembleRelease/connectedDebugAndroidTest PASS. 단위 8개·계측 6개 통과. 120초 내장 음원의 재생/일시정지/30초 탐색/재개 및 실제 UI 상태·1분 슬라이더 탐색 확인.

Goal:

외부 Source Provider 없이 검증 가능한 test audio 재생.

Acceptance Criteria:

Play/Pause

seek 가능

app UI에서 playback state 표시

KM-035 — Background playback

Status: [x]

검증 완료 (2026-09-02): API 36 에뮬레이터에 이어 Samsung SM-T220 / Android 14(API 34) 실기기에서 Home 후 Activity와 UI/테스트 컨트롤러를 모두 해제하고 32초 후 foreground 서비스 유지·재생 위치 30초 이상 증가를 확인했다. test/lint/assembleDebug/connectedDebugAndroidTest PASS (1분 17초, 단위 8개·계측 7개). 모든 인수 조건 충족. 다른 OEM·장시간 배터리 동작까지 검증했다는 의미는 아니다.

Goal:

Home 버튼 후 재생 지속.

Acceptance Criteria:

playback 시작

Home

30초 이상 playback 유지

UI Activity 없어도 Service active

Device verification required.

KM-036 — Screen-off playback

Status: [x]

검증 (2026-09-02): Samsung SM-T220 / Android 14 실기기에서 화면 꺼짐을 62초 동안 확인하고, 화면을 깨우기 전 60초 이상 재생 위치 증가·재생 중 상태 확인. test/lint/assembleDebug/assembleRelease/connectedDebugAndroidTest PASS (단위 8개·계측 8개). 충전 연결 상태의 단기 검증이며 장시간 절전/OEM 전체 검증은 별도다.

Goal:

화면 OFF 상태 playback 유지.

Acceptance Criteria:

playback start

screen off

60초 이상 지속

Real-device verification preferred.

KM-037 — Media notification

Status: [x]

검증 (2026-09-02): test/lint/assembleDebug/assembleRelease/connectedDebugAndroidTest PASS (단위 8개·SM-T220 계측 9개). 실제 알림의 제목·아티스트·기본 이미지·앱 복귀 Intent 및 알림 PendingIntent의 pause/play 확인. 이전/다음은 Media3 playlist/명령 가용성 구조를 유지하며 현재 한 곡에서 없는 다음 곡 버튼은 비활성이다. 알림 패널 스크린샷으로 표시 확인.

Goal:

Media3 기반 notification control.

Acceptance Criteria:

title

artwork 또는 placeholder

play/pause

previous/next 가능한 구조

KM-038 — Lock-screen controls

Status: [x]

검증 (2026-09-02): SM-T220 / Android 14 실제 잠금화면에서 제목·아티스트·기본 이미지 표시 및 버튼 터치로 PAUSED(2) → PLAYING(3) 전환 확인. keyguard showing=true 유지. test/lint/assembleDebug PASS. 암호 없는 잠금화면 조건이며 다른 OEM/보안 잠금 설정은 미검증.

Goal:

Lock-screen media control 확인.

Acceptance Criteria:

play/pause 동작

metadata 표시

Real-device verification required.

KM-039 — Audio focus

Status: [x]

검증 (2026-09-02): test/lint/assembleDebug/connectedDebugAndroidTest PASS. 단위 9개·SM-T220 실기기 계측 10개 통과. 일시적 포커스 손실 시 일시정지·위치 정지·복귀 후 재개, 영구 손실 시 자동 재개하지 않음, 충돌 없이 종료 확인. 실제 전화/블루투스 이벤트는 별도 검증 대상.

Goal:

AudioAttributes와 focus handling.

Acceptance Criteria:

다른 audio session interruption 처리

focus loss state test 가능

no crash

KM-040 — Playback foundation regression test

Status: [x]

검증 (2026-09-02): docs/TESTING_PLAYBACK.md에 준비·전체 검사·백그라운드·화면 꺼짐·알림/잠금화면/포커스·dumpsys 확인 및 결과 기록 절차 작성. 문서의 test/lint/assembleDebug/connectedDebugAndroidTest 명령 실기기 PASS (단위 9개·계측 10개).

Goal:

M2 기능을 반복 검증할 수 있는 절차/스크립트 작성.

Acceptance Criteria:

docs/TESTING_PLAYBACK.md

background test 절차

screen-off test 절차

dumpsys media_session 확인법

M3 — Domain & Source POC

KM-050 — Domain Track model

Status: [x]

검증 (2026-09-02): 공급자 이름/DTO/재생 URL 없는 불변 Track과 SourceType 추가. 검증 로직 없는 데이터 선언이므로 별도 단위 테스트 대상 없음. test/lint/assembleDebug PASS.

Goal:

Provider-neutral Track 모델 생성.

Acceptance Criteria:

no provider name in domain model

unit tests if validation logic exists

KM-051 — PlayableStream model

Status: [x]

검증 (2026-09-02): URL·선택적 MIME/bitrate·만료 시각을 가진 일시적 모델 추가. toString URL 비노출 단위 검사 포함. test/lint/assembleDebug PASS.

Goal:

ephemeral stream metadata 모델 생성.

Acceptance Criteria:

url

mimeType

bitrate

optional expiry

KM-052 — AppError model

Status: [x]

검증 (2026-09-02): PRD의 Network/SourceUnavailable/PlaybackUnavailable/GeoRestricted/Unknown 타입 정의. 예외 원문과 URL을 보유하지 않는 sealed interface. test/lint/assembleDebug PASS.

Goal:

UI로 raw exception이 전달되지 않도록 공통 오류 타입 생성.

Acceptance Criteria:

Network

SourceUnavailable

PlaybackUnavailable

GeoRestricted

Unknown

KM-053 — MusicSource interface

Status: [x]

검증 (2026-09-02): search/getTrack/resolveStream/getRelated suspend 계약 정의. 도메인 타입과 Kotlin Result만 사용하며 Android/HTTP/공급자 의존성 없음. test/lint/assembleDebug PASS.

Goal:

Source Provider 계약 정의.

Acceptance Criteria:

search

getTrack

resolveStream

getRelated

domain type only

KM-054 — Provider A client skeleton

Status: [x]

검증 (2026-09-02): data/source/providerA 내부 client/context DTO 및 중앙 header/context 설정 추가. 공급자 DTO 외부 누출·UI 의존성 없음. 인증 정보 없는 요청 구성/HTTP 오류 테스트 포함. test/lint/assembleDebug PASS (단위 12개).

Goal:

InnerTube-compatible Provider A를 data/source/providerA에 격리.

Acceptance Criteria:

provider-specific DTO 밖으로 누출 없음

headers/context 한 곳에서 관리

no UI dependency

KM-055 — Provider A search POC

Status: [x]

검증 (2026-09-02): 일반 공개 WEB 검색을 Track으로 변환. 아이유/BTS Dynamite/Bach 실제 검색 3종 PASS, 빈 결과/오류/취소 및 mapper 단위 검사 PASS. test/lint/assembleDebug/assembleRelease/sourceContractTest PASS (단위 20개·실제 계약 3개). 음악 전용 WEB_REMIX의 빈 결과는 일반 공개 영상 검색으로 해결했으며 최종 공급자 채택은 아직 미정.

Goal:

query → List<Track> 변환.

Acceptance Criteria:

실제 query 3종 테스트

empty/error 처리

DTO mapper test

KM-056 — Provider A resolveStream POC

Status: [x]

완료 (2026-09-02): 재생 요청의 클라이언트 설정을 ProviderAClientProfile로 분리하고 후보를 순서대로 시도하도록 구현했다. 실제 계약 검사에서 IOS와 ANDROID가 직접 오디오 URL을 반환했고 Range 요청 206까지 확인했다. ANDROID_VR·TVHTML5_SIMPLY_EMBEDDED_PLAYER는 재생 불가, WEB은 직접 URL 없음으로 실패해 대체 경로로 남겼다. 후보 순서는 IOS → ANDROID → ANDROID_VR → TVHTML5 → WEB이다.

- 인수 조건: 실제 테스트 Track(gdZLi9oWNZg) stream resolve 성공 PASS, full resolved URL 로그 금지 PASS(고정 문자열 예외·URL 비출력 요약만 기록), domain PlayableStream 반환 PASS.
- `gradlew.bat test lint assembleDebug assembleRelease sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(50초). 단위 31개·실제 계약 4개 전부 통과, 실패/오류 0. 린트 오류 0·경고 18. debug/release APK 생성.
- 결정은 ADR-032에 기록했다. 신규 의존성·로그인·PO token·다운로드 없음. 클라이언트 설정은 공개 관찰값이라 공급자 변경에 취약하며 Android 기기의 실제 재생은 KM-057·KM-058에서 확인한다.

이전 검증 (2026-09-02): 직접 HTTPS 오디오 형식 선택·만료 시각·안전한 오류/취소 처리를 구현하고 관련 단위 8개 추가. 전체 단위 28개·lint·debug/release 빌드 대상은 통과했으나 실제 스트림 계약 1개 FAIL. 공개 플레이어의 signatureTimestamp를 반영하면 상태는 OK지만 adaptiveFormats에 직접 URL/signatureCipher가 없고 serverAbrStreamingUrl만 존재해 PlayableStream을 얻지 못했다. 실제 곡 해석 성공 조건 미충족이므로 보류하며 KM-057 이후는 시작하지 않는다. 상세 결과는 docs/DEVICE_RESUME_REPORT.md.

Goal:

Track ID → playable stream resolution.

Acceptance Criteria:

실제 테스트 Track에 대해 stream resolve 성공

full resolved URL 로그 금지

domain PlayableStream 반환

KM-057 — StreamResolver integration

Status: [x]

완료 (2026-09-02): MusicSource 구현을 Hilt로 바인딩하고 MusicService 재생과 연결했다. MediaItem에는 Track ID 자리표시 URI만 넣고 ResolvingDataSource가 재생 직전에 실제 주소로 바꾼다. 실기기에서 공급자가 열린 Range 요청을 403으로 거부해 ChunkedHttpDataSource로 512KB 이하 닫힌 Range 요청을 이어 붙이도록 했다.

- 인수 조건: Track ID → resolveStream → ExoPlayer → playback을 Samsung SM-T220에서 확인 PASS. 원격 트랙이 실제로 재생되고 위치가 진행하며 길이를 얻는다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(2분 50초). 단위 36개·실제 계약 4개·실기기 계측 14개 전부 통과, 실패/오류 0. 린트 오류 0·경고 19.
- 진단 결과: 열린 Range 403, 닫힌 512KB 206, 닫힌 1MB 이상 403, 같은 URL 반복 요청 정상. 청크 상한은 관찰값이다.
- 결정은 ADR-033에 기록했다. 신규 의존성 없음. 화면의 원격 재생 버튼과 고정 Track ID는 확인용이며 KM-058에서 대체한다.
- 보정 (KM-058에서 확인): 당시 실기기 검증이 10초 미만이라 첫 512KB 안에서 끝났고, 실제로는 약 34초 지점에서 403으로 멈추는 결함이 남아 있었다. 원인은 오디오 전용 adaptive 주소가 오프셋 요청을 거부하는 것이며 KM-058에서 progressive 형식만 사용하도록 고쳤다(ADR-034). ChunkedHttpDataSource는 필요 없어져 제거했다.
- 남은 검증: 여러 곡 연속 재생·장시간 재생 중 만료·네트워크 전환은 KM-061·KM-132·KM-136 대상이다.

Goal:

MusicService playback과 MusicSource를 연결.

Acceptance Criteria:

Track ID
→ resolveStream
→ ExoPlayer
→ playback

KM-058 — Search-to-play vertical slice

Status: [x]

완료 (2026-09-02): 최소 UI에 검색어 입력·결과 목록·결과 선택 재생을 붙여 실제 음악 검색에서 재생까지 이었다. 검증 중 재생이 약 34초에 멈추는 결함을 발견해 progressive 형식만 사용하도록 고쳤다(ADR-034).

- 인수 조건: query 입력 PASS, results 표시 PASS, result tap PASS, playback 시작 PASS, Home 후 playback 유지 PASS. 모두 Samsung SM-T220에서 확인했다.
- 수동 검증: "BTS Dynamite" 검색 → 결과 10건 이상 표시 → 첫 결과 탭 → 재생 시작. 95초 연속 재생 후 Home 이동, 115초 → 130초까지 재생 유지. 증거는 Git 제외된 captures/km-058/에 보관했다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 7초). 단위 38개·실제 계약 4개·실기기 계측 16개 전부 통과. 린트 오류 0·경고 19.
- 발견한 결함: 오디오 전용 adaptive 주소는 오프셋 요청을 전부 거부한다(헤더/쿼리 모두 403). 같은 응답의 progressive 형식은 임의 구간 요청을 허용한다. progressive만 인정하도록 바꾸고 후보 순서를 ANDROID 우선으로 조정했다. 회귀 방지로 먼 지점 탐색 재생 계측을 추가했다.
- 대가: progressive는 영상이 포함된 다중화 스트림이라 대역폭을 5~6배 쓴다. 영상 트랙은 재생에서 끈다. KM-059 Gate의 판단 요소다.
- 검색은 MusicSource를 직접 호출하는 임시 구조이며 KM-070 SearchRepository·KM-071 SearchViewModel·KM-072~073 화면이 대체한다. 신규 의존성은 androidx.compose.foundation 하나다.

Goal:

최소 UI로 실제 음악 검색 → 선택 → 재생 성공.

Acceptance Criteria:

query 입력

results 표시

result tap

playback 시작

Home 후 playback 유지

KM-059 — Provider A Gate

Status: [x]

완료 (2026-09-02): PASS. Provider A를 v0.1 source로 채택한다. 판정 근거와 곡별 결과는 docs/SOURCE_PROVIDER.md, 기술 결정은 ADR-037에 기록했다.

- Test set: 검색어 5종에서 모은 중복 없는 10곡, 서로 다른 아티스트 10종, 가장 긴 곡 4883초(81분). 큐 재생 확인.
- 소스 판정: 해석 10/10, 파일 중간 지점 구간 요청 10/10. 앞부분만 받아도 통과하지 않도록 전체 길이의 절반 지점을 요청한다.
- 기기 판정: 큐 두 곡의 자동 전환 PASS, 81분 트랙의 종료 2분 전 지점 이어 재생 PASS.
- 인수 조건: 결과를 docs/SOURCE_PROVIDER.md에 기록 PASS, PASS 시 채택 ADR 작성 PASS(ADR-037). KM-064는 활성화하지 않으며 재판정 기준을 문서에 남겼다.
- 채택 조건: 오디오 전용 스트림 불가로 progressive만 사용하며 대역폭이 약 3배다(ADR-034). 완화는 KM-134 캐시와 KM-137 WiFi 전용 재생이다. 공급자 설정은 관찰값이라 sourceContractTest 유지가 채택의 전제다.
- Gate 검증을 위해 PlayerConnection에 playQueue/currentMediaId를 추가했다. 큐 UI와 이전/다음은 KM-094·KM-097 범위다.

Goal:

Provider A를 최종 v0.1 source로 채택할지 결정.

Test set:

10 tracks

multiple artists

at least one long track

queue playback

Acceptance Criteria:

결과를 docs/SOURCE_PROVIDER.md에 기록

PASS: Provider A 채택 ADR

FAIL: Provider B evaluation task 활성화

M4 — Source Hardening

KM-060 — Source error mapping

Status: [x]

완료 (2026-09-02): 공급자·인프라 실패를 SourceFailure 다섯 분류로 좁힌 뒤 도메인 AppError로 바꾸고 화면 문구까지 연결했다. 원문 예외와 메시지는 경계에서 끊긴다.

- 인수 조건: network PASS, parse PASS, not found PASS, restricted PASS, unknown PASS. 단위 검사 12개 추가.
- Restricted는 GeoRestricted가 아니라 PlaybackUnavailable로 보낸다. 로그인·연령 제한을 "지역 제한"으로 표시하면 사용자에게 틀린 이유가 된다. AppError.GeoRestricted는 지역 제한을 구조적으로 판별할 수 있을 때까지 생성하지 않는다(ADR-038).
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 10초). 단위 53개·실제 계약 5개·실기기 계측 24개.
- KM-059 Gate 검사가 간헐 실패했다. 공급자 거부가 아니라 SocketTimeoutException이었고, 계약 위반과 전송 지연을 구분하도록 판정을 나눴다. 타임아웃 정책 자체는 KM-062 대상이다.

Goal:

Provider-specific error → AppError 매핑.

Acceptance Criteria:

network

parse

not found

restricted

unknown

tests required.

KM-061 — Stream refresh on playback failure

Status: [x]

완료 (2026-09-02): RefreshingDataSource를 캐시 안쪽·해석 바깥쪽에 두어 재생 중 실패 시 주소를 다시 해석하고 읽던 위치부터 한 번만 이어 시도한다.

- 인수 조건: first failure PASS, resolve again PASS(상위 소스를 새로 열면 재해석된다), retry once PASS, second failure → terminal error PASS, 무한 retry 없음 PASS.
- 읽던 위치부터 이어 연다. 처음부터 다시 받지 않으며 이미 받은 구간을 버리지 않는다.
- 열기 실패와 읽기 도중 실패를 모두 다룬다. ExoPlayer 기본 정책은 403을 재시도 대상으로 보지 않아 그대로 두면 종점 오류가 된다.
- 계측 검사 5개 추가: 열기 실패 후 재해석, 읽기 실패 후 이어받기와 내용 일치, 열기 두 번째 실패의 종점 처리, 읽기 두 번째 실패의 종점 처리, 정상 스트림은 다시 열지 않음.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 53초). 단위 53개·실제 계약 5개·실기기 계측 29개.
- 결정은 ADR-039에 기록했다. 신규 의존성 없음. 실제 만료 시각에 맞춘 장시간 재생 검증은 KM-136 대상이다.

Goal:

expired/forbidden stream 발생 시 재해석.

Acceptance Criteria:

first failure

resolve again

retry once

second failure → terminal error

infinite retry 없음

KM-062 — Network timeout policy

Status: [x]

완료 (2026-09-02): 대기 상한을 NetworkTimeouts 한곳에 모으고 메타데이터 요청과 재생 요청으로 나눠 적용했다. 취소는 상한과 무관하게 즉시 존중한다.

- 인수 조건: connect/read/request timeout PASS(메타데이터 10/20/30초, 재생 10/20초), coroutine cancellation honored PASS(검색·주소 해석 모두 CancellationException을 전파하며 취소된 요청은 상한을 기다리지 않는다).
- 재생 쪽 바이트 대기를 media3 기본 8초에서 20초로 늘렸다. KM-059·KM-060 검증에서 공급자가 전송을 늦게 시작하는 것을 실제로 관찰했고, 기본값이면 그런 곡이 곧바로 끊긴다.
- 상한을 넘긴 실패는 KM-061의 재해석·1회 재시도로 이어지고 그래도 실패하면 Network 오류로 표시된다.
- 단위 검사 5개 추가. 검사에서만 짧은 값을 주입해 실제 동작을 빠르게 확인하며, 상한을 그대로 기다리는 느린 검사를 만들지 않았다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(5분 2초). 단위 58개·실제 계약 5개·실기기 계측 29개.
- 결정은 ADR-040에 기록했다. 신규 의존성 없음.

Goal:

Source requests의 timeout 및 cancellation 정책 정리.

Acceptance Criteria:

connect/read/request timeout

coroutine cancellation honored

KM-063 — Source contract test suite

Status: [x]

완료 (2026-09-02): 기존 스위트를 점검해 회귀 감지의 구멍 세 곳을 메웠다. 검색·스트림·오류·Gate 네 종류로 정리하고 각각이 지키는 것을 문서화했다.

- 인수 조건: search contract PASS(3개), resolve contract PASS(1개), 별도 Gradle task PASS(`sourceContractTest`), 일반 unit test와 분리 PASS(`test`가 `*SourceContractTest` 제외).
- 메운 구멍 1: 검색 계약이 ID·제목만 확인해 아티스트·길이·이미지가 사라져도 통과했다. 결과의 절반 이상에 존재하는지로 필드 계약을 추가했다.
- 메운 구멍 2: 스트림 계약이 파일 앞부분만 요청해 "앞부분만 되는 주소"를 통과시켰다. Gate와 같은 중간 지점 기준으로 바꿨다.
- 메운 구멍 3: 오류 경로 계약이 없었다. 없는 트랙이 재생 불가로 분류되는지, 결과 없음이 실패로 바뀌지 않는지를 실제 응답으로 확인한다. 실행 결과 없는 트랙은 NotFound → PlaybackUnavailable로 KM-060 매핑과 일치했다.
- 계약 7개 전부 통과(검색 3·스트림 1·오류 2·Gate 1). README에 각 검사가 지키는 것과 깨졌을 때의 증상을 표로 정리했다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 17초). 단위 58개·실제 계약 7개·실기기 계측 29개.
- 함께 고친 것: 계측 4개가 MusicService에 연결하면서 HiltAndroidRule이 없어 순서에 따라 간헐적으로 실패했다. 네 곳에 규칙을 추가했다.
- 결정은 ADR-041에 기록했다.

Goal:

실제 provider regression 탐지.

Acceptance Criteria:

search contract

resolve contract

별도 Gradle task 또는 test source set

일반 unit test와 분리

KM-064 — Provider B evaluation

Status: [ ]

Condition:

KM-059가 FAIL일 때 수행.

Goal:

Extractor adapter fallback 평가.

Acceptance Criteria:

integration feasibility

APK/dependency impact

license impact

provider contract compatibility

ADR 작성

M5 — Search UX

KM-070 — SearchRepository

Status: [x]

완료 (2026-09-02): ARCHITECTURE 6의 계약대로 인터페이스는 core/search, 구현은 data/repository에 두고 Hilt로 바인딩했다. POC 화면의 ViewModel을 이 경계 뒤로 옮겼다.

- 인수 조건: MusicSource injected PASS, error mapping PASS(모든 실패를 AppErrorException으로 감싼다), unit tests PASS(6개 추가).
- 이 경계의 실익은 실패 표현이다. 이전에는 ViewModel이 data.source.toAppError를 직접 불러 화면 계층이 데이터 계층 함수에 의존했다. 이제 화면은 AppError만 안다.
- repository는 얇게 둔다. 검색어 정리와 빈 검색어 처리는 이미 공급자 구현에 있어 옮기지 않았다. 취소는 실패로 바꾸지 않고 전파한다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 51초). 단위 64개·실제 계약 7개·실기기 계측 29개.
- 결정은 ADR-042에 기록했다. 신규 의존성 없음. SearchViewModel 분리는 KM-071이다.

Goal:

ViewModel과 MusicSource 사이 repository layer 구현.

Acceptance Criteria:

MusicSource injected

error mapping

unit tests

KM-071 — SearchViewModel

Status: [x]

완료 (2026-09-02): 검색을 PlayerViewModel에서 떼어 feature/search/SearchViewModel로 옮기고 상태 이름을 사양대로 맞췄다.

- 인수 조건: SearchUiState 다섯 상태 PASS(Idle/Loading/Success/Empty/Error), StateFlow PASS(읽기 전용 노출), unit tests PASS(9개 추가).
- 분리의 실익은 검사 가능성이다. PlayerViewModel은 PlayerConnection을 통해 Handler/Looper에 묶여 ViewModel 검사를 전부 계측으로 돌려야 했다. 검색만 떼니 일반 단위 검사로 확인된다.
- 새 검색은 이전 검색을 취소한다. 늦게 도착한 이전 결과가 새 결과를 덮어쓰지 않는 것을 단위 검사로 고정했다.
- 검색 상태 검사를 계측에서 단위로 옮기고, 계측에는 실제 검색 → 선택 → 재생 → Home 유지 end-to-end 하나만 남겼다. 계측 29개 → 26개.
- 신규 테스트 의존성 kotlinx-coroutines-test(앱 산출물 미포함). `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(6분 20초). 단위 73개·실제 계약 7개·실기기 계측 26개.
- 결정은 ADR-043에 기록했다. 검색 화면 분리는 KM-072, 결과 목록 추출은 KM-073이다.

Goal:

SearchUiState 구현.

States:

Idle

Loading

Success

Empty

Error

Acceptance Criteria:

unit tests

StateFlow

KM-072 — SearchScreen

Status: [x]

완료 (2026-09-03): 검색어 입력과 결과 표시를 feature/search/SearchScreen으로 옮겼다. TestPlaybackScreen에는 재생 제어만 남는다.

- 인수 조건: query input PASS, Search action PASS(버튼과 키보드 ImeAction.Search), loading PASS(진행 표시와 문구), error PASS(오류 색 문구), empty state PASS. 다섯 상태 모두 실기기에서 확인했다.
- 착수 전 미결 사항은 사용자와 합의했다. 화면을 컴포저블로만 분리하고 배치는 현재대로 둔다. 내비게이션으로 두 화면을 실제로 나누는 것은 KM-150이다.
- 검색 화면은 SearchViewModel만 알고 선택은 onSelect 콜백으로 넘긴다. 재생 의존성이 검색 화면에 들어가지 않는다.
- 검색어는 화면의 rememberSaveable 상태다. 검색어를 비우면 clear()로 이전 결과도 치운다.
- 분:초 표기를 ui/format/formatDuration으로 합쳤다. 이전에는 재생 위치와 결과 목록의 길이가 같은 규칙을 각각 계산했다. 단위 6개 추가.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 38초). 단위 79개·실제 계약 7개·실기기 계측 26개. 린트 오류 0.
- 실기기 SM-T220 / Android 14 확인: Idle → 검색 → Loading → Success(길이 18:24 등) → 검색어 삭제 시 Idle 복귀 → 없는 검색어로 Empty → WiFi 차단 후 Error. 확인 후 WiFi를 되돌렸다.
- 결정은 ADR-044에 기록했다. 신규 의존성 없음. 결과 목록 항목 구성은 KM-073이다.

Goal:

검색 입력 UI.

Acceptance Criteria:

query input

Search action

loading

error

empty state

KM-073 — SearchResultList

Status: [x]

완료 (2026-09-03): 결과 항목을 feature/search/SearchResultList로 빼고 앨범 이미지를 붙였다.

- Display: artwork PASS, title PASS(두 줄 제한), artist PASS, duration where known PASS(아는 경우에만 붙임).
- coil-network-okhttp를 추가했다. Coil 3은 네트워크 fetcher가 별도 산출물이라 coil-compose만으로는 원격 이미지가 조용히 실패한다. OkHttp는 ktor-client-okhttp로 이미 classpath에 있다.
- Track.artworkUrl과 ui/components/Artwork는 이전부터 있었으나 화면에서 쓰이지 않았다. 이번에 연결했다.
- 부제 조립을 trackSubtitle로 떼어 단위 6개 추가. 아티스트와 길이가 모두 없으면 줄 자체를 그리지 않는다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(5분 42초). 단위 85개·실제 계약 7개·실기기 계측 26개. 린트 오류 0.
- 실기기 SM-T220 / Android 14에서 실제 섬네일과 제목·아티스트·길이(18:24, 3:42)를 확인하고, 항목 선택 후 재생 시작까지 확인했다.
- 결정은 ADR-045에 기록했다. 최근 검색어 저장은 KM-074다.

Goal:

Track 결과 목록.

Display:

artwork

title

artist

duration where known

KM-074 — Search history

Status: [x]

완료 (2026-09-03): 최근 검색어를 core/search/SearchHistoryRepository 뒤에 두고 설정 DataStore에 저장한다.

- 인수 조건: successful search 저장 PASS, history clear PASS, app restart persistence PASS. 셋 다 실기기에서 확인했다.
- 저장 위치는 Room이 아니라 DataStore다. 짧은 문자열 목록이고 조회·정렬이 필요하지 않으며, Room을 쓰면 자리표시자 엔티티만 있는 스키마를 1에서 2로 올려야 한다. Preferences에 순서를 지키는 목록 타입이 없어 JSON 배열 한 값으로 저장한다.
- KM-110의 SearchHistoryEntity는 이 결정에 따라 필요하지 않다. KM-110 착수 시 다시 판단한다.
- 오류 없이 끝난 검색만 남긴다. 결과가 없는 검색도 성공한 검색이므로 남기고 실패한 검색은 남기지 않는다. 상한 10, 같은 검색어는 중복 없이 앞으로 올라온다.
- 목록은 Idle일 때만 보여준다. 한 화면에 검색과 재생이 함께 있어 자리가 넉넉하지 않다. 칩을 누르면 그 검색어로 다시 검색한다.
- 단위 15개 추가(저장소 9·ViewModel 6). 저장소 재개방 검사가 앱 재시작 유지를 단위 검사로 덮는다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 59초). 단위 100개·실제 계약 7개·실기기 계측 26개. 린트 오류 0.
- 결정은 ADR-046에 기록했다. 신규 의존성 없음. M5 검색 완료.

Goal:

최근 검색어 local persistence.

Acceptance Criteria:

successful search 저장

history clear

app restart persistence

M6 — Player UX

KM-090 — Player UI state adapter

Status: [x]

완료 (2026-09-03): core/player/PlaybackState에 현재 곡·반복·셔플을 더하고 playing·buffering을 이름 붙여 드러냈다.

- 인수 조건: current Track PASS(NowPlaying), playing PASS, buffering PASS, duration PASS, position PASS, repeat PASS(RepeatMode Off/One/All), shuffle PASS.
- PlayerUiState를 새 타입으로 만들지 않고 이미 있는 PlaybackState를 확장했다. 이 타입이 ARCHITECTURE 19가 요구하는 Media3 → UI 매퍼의 결과이며, 같은 것을 가리키는 상태 타입을 둘로 두지 않는다. 이름은 ARCHITECTURE 4의 PlaybackStateMapper.kt에 맞춘다.
- 현재 곡은 NowPlaying(mediaId, title, artist)다. Track을 재구성하지 않는다. 세션에서 오는 것은 대기열 metadata뿐이라 source나 길이를 지어내면 안 된다.
- 앨범 이미지는 아직 대기열 항목에 넣지 않는다. 넣으면 알림 이미지 동작이 바뀌어 KM-037·038 검증을 다시 해야 하므로 KM-091·092에서 함께 다룬다.
- playing·buffering은 phase에서 읽는 계산 속성이다. 같은 사실을 두 곳에 두지 않는다.
- 반복·셔플을 바꾸는 조작은 넣지 않았다. KM-095·096 범위다. 따라서 실기기에서는 세션 기본값(꺼짐)까지만 확인되고 켠 상태 매핑은 단위 검사로 고정했다.
- 단위 8개·계측 1개 추가(계측 26개 → 27개). 새 필드를 쓰는 화면은 아직 없고 소비는 KM-091부터다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(5분 3초). 단위 108개·실제 계약 7개·실기기 계측 27개. 린트 오류 0.
- 결정은 ADR-047에 기록했다. 신규 의존성 없음.

Goal:

MediaController state → PlayerUiState.

Acceptance Criteria:

current Track

playing

buffering

duration

position

repeat

shuffle

KM-091 — Mini Player

Status: [x]

완료 (2026-09-03): feature/player/MiniPlayer가 현재 곡의 앨범 이미지·제목·아티스트와 재생·일시정지를 한 줄로 보여준다.

- 인수 조건: artwork PASS, title PASS, play/pause PASS. **tap opens Now Playing은 넣지 않았다.** 화면 전환이 필요하고 내비게이션은 KM-150, 목적 화면은 KM-092다. 사용자와 백로그 순서를 지키기로 합의했으며 아무 일도 하지 않는 탭 영역을 미리 만들지 않았다. 이 조건은 KM-092·150에서 충족된다.
- 사용자 결정에 따라 대기열 항목에 앨범 이미지 주소를 넣었다(ADR-047에서 미뤄 둔 것). playTrack이 artworkUri를 받아 세션에 넣고 상태로 다시 읽는다. https로 시작하는 주소만 넣는다.
- 그 결과 알림·잠금화면 이미지가 자리표시자에서 실제 이미지로 바뀌므로 KM-037·038을 재검증했다. 계측 1개와 실기기 눈 확인으로 알림·잠금화면의 제목·아티스트·이미지·진행·버튼과 잠금화면 일시정지/재생 왕복을 확인했다.
- 미니 플레이어가 재생·일시정지를 들고 있으므로 화면의 독립 재생 버튼을 없앴다. 현재 곡이 없을 때와 연결이 끊겼을 때만 단독 버튼을 둔다.
- 내장 테스트 음원은 artworkData만 있고 artworkUri가 없어 미니 플레이어에서는 자리표시자 색으로 보인다. 알림은 기존대로 그 데이터를 쓴다.
- playQueue는 이미지 주소를 받지 않는다. Gate 검증용 진입점이며 제품 경로는 playTrack이다.
- 단위 2개·계측 1개 추가(계측 27개 → 28개).
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 32초). 단위 110개·실제 계약 7개·실기기 계측 28개. 린트 오류 0·경고 22(새 Uri.parse의 UseKtx 1건 증가, 기존 2건과 같은 종류).
- 결정은 ADR-048에 기록했다. 신규 의존성 없음.

Acceptance Criteria:

artwork

title

play/pause

tap opens Now Playing

KM-092 — Now Playing screen

Status: [x]

완료 (2026-09-03): feature/player/NowPlayingScreen이 임시 TestPlaybackScreen을 대체하고 그 파일을 지웠다.

- 인수 조건: artwork PASS, title PASS, artist PASS, transport controls PASS(재생·일시정지), progress PASS(슬라이더와 위치·길이). favorite·queue button은 **자리만 두고 비활성**이다.
- 즐겨찾기·대기열 동작은 사용자와 합의해 각 소속 작업으로 미뤘다. 즐겨찾기 저장은 KM-112, 대기열 화면은 KM-097이다. 반응 없는 버튼은 고장으로 보이므로 비활성으로 두고 "아직 준비되지 않은 기능입니다."를 함께 적었다.
- 이전·다음은 두지 않았다. KM-094가 UI·알림·잠금화면을 함께 다루는 작업이라 UI만 먼저 만들면 반쪽으로 시작된다. 끌어서 탐색은 이미 동작하며 인수 조건 확인은 KM-093이다.
- 전체 화면에서는 하단 내비게이션을 감춘다. 앨범 이미지가 화면 폭 전체의 정사각형이라 자리가 필요하다. 화면 안에 뒤로 버튼을 두었다. KM-150에서 미뤄 둔 판단이다.
- 화면을 세로로 흘린다. 제목 두 줄이나 요금제 안내가 붙으면 세로가 모자랄 수 있다.
- WiFi 전용 스위치는 이 화면에 남겼다. 설정 화면이 KM-153이라 지금 없애면 KM-137 기능에 닿을 길이 사라진다.
- 단위·계측 검사를 새로 넣지 않았다. 화면 배치 작업이며 상태 매핑은 KM-090, 세션 이미지는 KM-091이 검사한다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 50초). 단위 110개·실제 계약 7개·실기기 계측 28개. 린트 오류 0·경고 22.
- 결정은 ADR-050에 기록했다. 신규 의존성 없음.

Acceptance Criteria:

artwork

title

artist

transport controls

progress

favorite

queue button

KM-093 — Seek

Status: [x]

완료 (2026-09-03): 끌어서 탐색은 이미 동작했으나 손을 뗀 직후 표시가 탐색 이전 자리로 되돌아갔다가 목표로 뛰는 문제를 고쳤다.

- 인수 조건: drag seek PASS, controller updates player PASS, progress remains synchronized PASS.
- 원인은 위치 보고가 250ms 간격이라 손가락 값을 지운 순간에는 옛 위치만 있다는 것이다. feature/player/PendingSeek를 두어 표시 위치를 손가락 → 아직 도달하지 않은 목표 → 실제 위치 순으로 고른다.
- 목표를 놓아주는 조건 셋: 목표 근처(±1초), 목표를 지나 재생이 계속됨, 목표에서 멀어짐. 마지막이 없으면 탐색이 받아들여지지 않았을 때 표시가 목표에 붙어 멈춘다.
- 시간 문구도 슬라이더와 같은 값을 쓴다.
- 단위 7개·계측 1개 추가(계측 28개 → 29개).
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(5분 29초). 단위 117개·실제 계약 7개·실기기 계측 29개. 린트 오류 0.
- 실기기 확인: 0:06에서 오른쪽으로 끌어 손을 떼자 곧바로 1:24를 표시하고 세션 위치 87353ms, 되돌아가는 구간 없음. 이후 1:50까지 진행. 왼쪽으로 끌면 0:32(32782ms)로 옮겨지고 0:39까지 진행.
- 결정은 ADR-051에 기록했다. 신규 의존성 없음.

Acceptance Criteria:

drag seek

controller updates player

progress remains synchronized

KM-094 — Previous / Next

Status: [x]

완료 (2026-09-03): 사용자와 합의해 대기열을 만드는 경로는 KM-097로 미루고 버튼과 명령 연결만 다뤘다.

- 인수 조건: UI button PASS, notification button PASS, lockscreen button PASS, consistent behavior PASS.
- 지금 대기열에는 한 곡뿐이다(playTrack이 setMediaItem으로 갈아 끼움). 그래도 이전은 뜻이 있다. Media3는 다음 곡이 없을 때 seekToPrevious()를 그 곡의 처음으로 되돌리는 동작으로 정의한다. 이전은 살리고 다음은 비활성이다.
- 가용성을 앱이 따로 계산하지 않고 Player.isCommandAvailable을 그대로 읽어 PlaybackState.hasPrevious·hasNext로 옮긴다. 화면·알림·잠금화면이 같은 근거를 쓰므로 세 곳이 갈릴 수 없다. consistent behavior를 이 방식으로 만족시킨다.
- 알림·잠금화면 버튼은 새로 만들지 않았다. Media3 알림 제공자가 같은 가용성으로 버튼을 구성하고 곡이 하나일 때 다음 버튼을 내지 않는다.
- 아이콘 대신 문자열 버튼("이전"·"다음")을 썼다. material-icons-core에 SkipPrevious·SkipNext가 없고 두 글리프 때문에 extended를 넣지 않는다. 이 화면의 재생·일시정지도 이미 문자열이다.
- 단위 2개·계측 1개 추가(계측 29개 → 30개).
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 26초). 단위 119개·실제 계약 7개·실기기 계측 30개. 린트 오류 0.
- 실기기 확인: 화면의 이전으로 89678ms → 0, 다음은 89442ms에서 변화 없음. 잠금화면 카드에 다음 버튼이 없고 카드의 이전으로 112639ms → 처음.
- 결정은 ADR-052에 기록했다. 신규 의존성 없음.

Acceptance Criteria:

UI button

notification button

lockscreen button

consistent behavior

KM-095 — Shuffle

Status: [x]

완료 (2026-09-03): PlayerConnection.setShuffleEnabled과 Now Playing의 선택 상태 칩을 붙였다.

- 인수 조건: toggle PASS, state visible PASS(켜짐은 채워진 칩·꺼짐은 테두리 칩), queue behavior test **부분 충족**.
- 화면의 켜짐 표시는 세션이 돌려준 PlaybackState.shuffleEnabled만 근거로 한다. 눌렀다는 사실을 따로 기억하면 세션이 거절했을 때 화면이 거짓을 보인다.
- **섞인 재생 순서는 UI 계층에서 관찰할 수 없다.** 세션이 컨트롤러에 보내는 Timeline에 셔플 순서가 실려 오지 않는다(RemotableTimeline이 선형 순서로 되돌아감). 네 곡 대기열을 열 번 다시 섞어도 컨트롤러가 보는 다음 곡은 매번 넣은 순서였다. 그래서 계측 검사는 토글이 세션까지 닿는 것과 셔플을 켜고 끄어도 대기열이 그대로인 것까지만 고정하고, 섞인 순서 자체는 고정하지 않았다. 상세는 ADR-053.
- **KM-097에도 같은 제약이 걸린다.** 컨트롤러만 보는 대기열 화면은 넣은 순서만 보여줄 수 있다.
- 셔플 상태는 영구 저장하지 않는다. PRD 34의 DataStore 항목에도 셔플은 없다.
- 단위 1개·계측 1개 추가(계측 30개 → 31개).
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 39초). 단위 120개·실제 계약 7개·실기기 계측 31개. 린트 오류 0.
- 결정은 ADR-053에 기록했다. 신규 의존성 없음.

Acceptance Criteria:

toggle

state visible

queue behavior test

KM-096 — Repeat

Status: [ ]

Acceptance Criteria:

off

one

all

state persistence optional

KM-097 — Queue UI

Status: [ ]

Goal:

현재 queue 표시 및 기본 수정.

Acceptance Criteria:

show list

current item highlight

remove

reorder if practical

M7 — Library

KM-110 — Room schema

Status: [ ]

Entities:

TrackEntity

FavoriteEntity

PlaylistEntity

PlaylistItemEntity

PlaybackHistoryEntity

SearchHistoryEntity

Acceptance Criteria:

schema export configured

DB test

KM-111 — Library repository

Status: [ ]

Goal:

DAO를 UI/ViewModel에서 숨긴다.

Acceptance Criteria:

repository interfaces

Flow-based observation

tests

KM-112 — Favorites

Status: [ ]

Acceptance Criteria:

add/remove

Library display

app restart persistence

KM-113 — Playlists

Status: [ ]

Acceptance Criteria:

create

rename

delete

add track

remove track

KM-114 — Playlist playback

Status: [ ]

Acceptance Criteria:

playlist → Media3 queue

sequential playback

next/previous

KM-115 — Playback history

Status: [ ]

Acceptance Criteria:

successful playback 기록

Recently Played

clear history

duplicate policy documented

KM-116 — Library screen

Status: [ ]

Sections:

Favorites

Playlists

History

M8 — Stability & Device Behavior

KM-130 — Bluetooth controls

Status: [ ]

Acceptance Criteria:

headset Play/Pause

Next if supported

Previous if supported

Real-device required.

KM-131 — Headset disconnect behavior

Status: [ ]

Acceptance Criteria:

wired/Bluetooth disconnect 시 playback policy 구현

accidental speaker playback 방지

KM-132 — Network reconnect

Status: [ ]

Acceptance Criteria:

temporary loss

reconnect

reasonable recovery or clear failure

KM-133 — Activity/process lifecycle regression

Status: [ ]

Acceptance Criteria:

Activity recreation

rotation/config change where applicable

Activity finish

Service playback unaffected under valid playback conditions

KM-134 — Streaming cache

Status: [x]

완료 (2026-09-02): Media3 SimpleCache를 재생 경로 가장 바깥에 두어 같은 곡을 다시 들을 때 다시 받지 않게 했다. 캐시 키는 Track 자리표시 URI라 매번 달라지는 스트림 주소와 무관하게 재사용된다.

- 인수 조건: LRU PASS(LeastRecentlyUsedCacheEvictor), 기본 상한 256MB PASS, 캐시 비우기 PASS, 영구 다운로드 아님 PASS(cacheDir 아래, 상한 초과 시 자동 삭제, DownloadManager 미사용).
- 저장 확정 조각을 기본 5MB에서 1MB로 줄였다. 기본값에서는 곡을 짧게 듣고 멈추면 받은 구간이 하나도 남지 않는다.
- SimpleCache는 한 디렉터리를 프로세스에서 하나만 열 수 있어 프로세스 단위 인스턴스로 보관한다. 주입 그래프가 다시 만들어져도 같은 캐시를 쓴다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0. 실기기 계측에 캐시 검사 3개를 추가했다.
- 결정은 ADR-035에 기록했다. 신규 의존성은 androidx.media3:media3-database 하나다. 캐시 크기 설정 화면은 KM-153 범위다.

Goal:

Media3 disposable cache 추가.

Acceptance Criteria:

LRU

default target 256MB

clear cache function

no permanent download behavior

KM-135 — OEM battery help

Status: [ ]

Condition:

실기기에서 실제 background kill 문제가 재현될 때만 구현.

Acceptance Criteria:

generic settings help

no aggressive battery exemption prompt at first launch

KM-136 — 30-minute smoke test

Status: [ ]

Acceptance Criteria:

실기기 30분 사용:

search

multiple tracks

background

screen off

lockscreen

Bluetooth where available

Crash 없음.

KM-137 — 네트워크 사용 정책

Status: [x]

완료 (2026-09-02): WiFi 전용 재생 설정을 추가했다. 켜면 측정 요금제 연결에서 새로 내려받는 재생을 막고, 이미 캐시에 있는 구간은 그대로 재생한다.

- 인수 조건: 설정 저장·복원 PASS(DataStore), 측정 요금제에서 원격 재생 차단 PASS, 차단 사유 안내 PASS(화면 문구), 캐시 구간은 그대로 재생 PASS(캐시가 해석보다 바깥에 있어 판단을 거치지 않음), 기본값 꺼짐 PASS.
- 차단은 `TrackStreamResolver`에서 주소 해석 전에 이뤄진다. 단위 3개·계측 3개를 추가했다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(4분 5초). 단위 41개·실제 계약 4개·실기기 계측 22개.
- 결정은 ADR-036에 기록했다. 신규 의존성 없음. 설정 UI는 KM-153 Settings 화면이 생기면 옮긴다.

배경:

Provider A는 오디오 전용 주소의 구간 요청을 거부해 영상이 포함된 progressive 형식만 재생할 수 있다(ADR-034). 곡당 8~25MB로 오디오 전용의 약 3배다. 사용자가 데이터 사용을 통제할 수 있어야 한다.

Goal:

측정 요금제 네트워크에서의 재생을 사용자가 통제한다.

Acceptance Criteria:

WiFi 전용 재생 설정 저장 및 복원

설정이 켜져 있고 측정 요금제 연결이면 원격 재생을 시작하지 않는다

차단 시 사용자에게 이유를 알린다

캐시에 있는 구간은 네트워크 없이 그대로 재생한다

기본값은 꺼짐

M9 — Polish & Release

KM-150 — App navigation

Status: [x]

완료 (2026-09-03): 사용자 요청으로 M9에서 앞당겼다. KM-092를 하려면 갈 수 있는 화면이 있어야 한다.

- 인수 조건: player state survives navigation PASS. 검색 탭에서 재생 후 홈 탭으로 옮겨도 미니 플레이어가 남고 세션 위치가 15250 → 33293 → 63591로 계속 진행했다.
- navigation/Destinations.kt와 navigation/KeuneyNavHost.kt 추가. 하단 탭 홈·검색·라이브러리에 전체 화면 목적지 now-playing이 붙는다. 시작 목적지는 검색이다.
- 신규 의존성 둘: androidx.navigation:navigation-compose 2.10.0(안정판), androidx.compose.material:material-icons-core(Compose BOM 관리). 아이콘은 NavigationBarItem 필수 인자이며 필요한 세 개가 core 집합에 있어 extended는 쓰지 않았다.
- ViewModel은 Activity가 만들어 내려준다. 목적지마다 새로 만들면 탭 이동 때 상태가 끊긴다. hilt-navigation-compose도 필요 없다.
- 홈·라이브러리는 자리표시자다. 내용은 KM-151·KM-116이다.
- 미니 플레이어를 하단 내비게이션 위로 옮기고 전체 화면에서는 접는다. KM-091에서 미뤄 둔 "tap opens Now Playing"이 채워졌다.
- TestPlaybackScreen에서 검색을 떼어 검색 탭으로 옮겼다. 이 화면은 KM-092가 대체한다. WiFi 전용 스위치는 KM-153에서 설정으로 옮긴다.
- 하단 내비게이션은 Now Playing에서도 보인다. 전체 화면에서 감출지는 KM-092에서 정한다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(6분 43초). 단위 110개·실제 계약 7개·실기기 계측 28개. 린트 오류 0.
- UI 자동 검사는 넣지 않았다. Compose UI 검사 의존성이 없고 AGENTS.md 16이 UI 검사 최소화를 요구한다. 실기기 눈 확인으로 다뤘다.
- 결정은 ADR-049에 기록했다.

Bottom navigation:

Home

Search

Library

Acceptance Criteria:

player state survives navigation

KM-151 — Home screen

Status: [ ]

Sections:

Recently Played

Favorites

Playlists

KM-152 — Dark theme

Status: [ ]

Acceptance Criteria:

System

Light

Dark

no unreadable contrast in primary screens

KM-153 — Settings

Status: [ ]

Initial options:

theme

cache size

clear cache

history enabled

Do not add unused settings.

KM-154 — Accessibility pass

Status: [ ]

Acceptance Criteria:

media controls contentDescription

touch target acceptable

large font basic usability

KM-155 — OSS notices

Status: [ ]

Acceptance Criteria:

THIRD_PARTY_NOTICES.md

dependency licenses reviewed

GPL dependency status explicitly documented

KM-156 — README

Status: [ ]

Required:

project purpose

personal sideload boundary

build instructions

architecture summary

known limitations

KM-157 — Release signing configuration

Status: [ ]

Acceptance Criteria:

signing secret not committed

local signing instructions documented

KM-158 — Release APK

Status: [ ]

Verification:

./gradlew test
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease

Acceptance Criteria:

release APK installs

core smoke test PASS

Final MVP Gate — KM-200

Status: [ ]

Goal:

Keuney Music v0.1 Definition of Done 최종 검증.

Must PASS:

Search works

Track playback works

Background playback works

Screen-off playback works

Notification controls work

Lock-screen controls work

Bluetooth Play/Pause works

Queue Next works

Favorites persist

Playlists persist

History persists

Network errors handled

No known core-flow crash

Unit tests PASS

Lint PASS

Debug build PASS

Release build PASS

Source contract PASS

Real-device playback Gate PASS

When all items pass, tag candidate:

v0.1.0

Codex Start Prompt

첫 작업:

Read AGENTS.md, PRD.md, ARCHITECTURE.md and TASKS.md.

Start with KM-001 only.

Follow all rules in AGENTS.md.
Do not implement future tasks.

After implementation:
1. list changed files,
2. list commands executed,
3. report verification results,
4. report acceptance criteria,
5. list remaining risks.

이후:

Continue the Keuney Music project.

Read AGENTS.md, PRD.md, ARCHITECTURE.md, TASKS.md,
and docs/DECISIONS.md.

Find the next incomplete KM task.
Implement that task only.

Run the required verification.
Mark it complete only if its Acceptance Criteria pass.

Report:
- changed files
- commands
- tests/build results
- decisions
- remaining risks
