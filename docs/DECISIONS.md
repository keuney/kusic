# 기술 결정 기록

## 2026-09-02 — KM-001 저장소 기반

- 기존 AGENTS.md, PRD.md, ARCHITECTURE.md, TASKS.md를 프로젝트 기준 문서로 유지한다.
- Git 저장소를 초기화하고 README 초안과 `.gitignore`를 추가한다.
- `.gitignore`는 Android/Gradle 빌드 결과, IDE 로컬 설정, SDK 로컬 경로, 서명 키 및 비밀 설정을 제외한다. Gradle Wrapper JAR와 Version Catalog는 제외하지 않는다.
- 애플리케이션 코드, Gradle 구성, 의존성은 추가하지 않는다. 해당 작업은 TASKS.md의 개별 작업에서 진행한다.
- 이번 작업에서 아키텍처 변경이나 신규 의존성 선택은 없다.

## ADR-009 — Gradle Wrapper와 루트 빌드 기반

- 날짜: 2026-09-02
- 대상 작업: KM-010
- 상태: 채택

### 결정

Gradle 9.7.1의 공식 `bin` 배포본과 Wrapper를 사용한다. 확인 시점 공식 버전 API에서 `current=true`, `snapshot=false`, `released=true`인 안정 버전이며, 이 프로젝트에서 JDK 17.0.18로 실행을 검증한다. 기존에 설치된 Gradle 8.14.1은 최초 Wrapper 생성에만 사용한다.

버전은 `gradle/libs.versions.toml`에서 관리하고 루트 `build.gradle.kts`의 `wrapper` 작업이 이를 읽는다. 배포 ZIP의 SHA-256을 Wrapper 설정에 고정한다. 버전을 바꿀 때에는 공식 체크섬도 함께 갱신하고 Wrapper를 재생성해야 한다.

- 배포 ZIP SHA-256: `acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a`
- Wrapper JAR SHA-256: `7a9ce74cff467ca1bf60a4fcd9f05185acceda4d0f382434d393e17864262c5d`

저장소는 `settings.gradle.kts`에서 중앙 관리한다. 플러그인 조회에는 Google Maven, Maven Central, Gradle Plugin Portal을 사용하고 라이브러리는 Google Maven과 Maven Central을 사용한다. 하위 프로젝트의 저장소 추가는 `FAIL_ON_PROJECT_REPOS`로 차단한다.

현재 필요한 외부 플러그인·라이브러리는 없으므로 추가하지 않는다. Android 앱 모듈과 AGP/Kotlin 플러그인 버전 조합은 KM-011에서 실제 호환성을 확인하고 선택한다. Gradle 배포본에 내장된 Kotlin 버전은 앱의 Kotlin 플러그인 선택을 의미하지 않는다.

### 범위와 영향

- 앱 모듈, SDK 버전, Compose 및 기능 코드는 이번 작업에 포함하지 않는다.
- 테스트·린트·APK 빌드 작업을 대신하는 빈 작업을 만들지 않는다. 실제 앱 모듈 구성 전에는 공통 필수 검증이 실패하는 상태를 그대로 보고한다.
- 검증에서는 `JAVA_HOME`을 JDK 17로 지정하고 `GRADLE_USER_HOME`을 저장소의 `.gradle/user-home`으로 지정한다. 사용자 전역 Gradle 설정 및 회사 프로젝트 설정은 변경하지 않는다.
- `.gitattributes`로 셸 스크립트는 LF, Windows 배치 파일은 CRLF, Wrapper JAR는 바이너리로 관리한다.

근거: [Gradle 9.7.1 릴리스 노트](https://docs.gradle.org/9.7.1/release-notes.html), [공식 버전 API](https://services.gradle.org/versions/current), [Wrapper 문서](https://docs.gradle.org/current/userguide/gradle_wrapper.html).

## ADR-010 — 빈 Android 앱 모듈과 빌드 도구

- 날짜: 2026-09-02
- 대상 작업: KM-011
- 상태: 채택

### 결정과 근거

`:app`을 단일 Android 애플리케이션 모듈로 추가한다. 패키지와 namespace는 `com.keuney.music`, 최소 SDK는 26, 대상 SDK는 37, 컴파일 SDK는 안정 플랫폼 37.2로 설정한다. Google SDK 저장소 및 `sdkmanager --list --channel=0`에서 37.2 정식 패키지를 확인했으며 설치 후 `PreviewSdkInt=0`, 비어 있는 CodeName을 확인했다. minor API는 `compileSdk { version = release(37) { minorApiLevel = 2 } }`로 지정하며 targetSdk는 메이저 API 37을 사용한다.

Android Gradle Plugin 9.4.0을 Version Catalog에 고정한다. 공식 Google Maven 메타데이터에서 정식 버전 게시를 확인했다. 공식 요구사항은 Gradle 9.6.0 이상과 JDK 17이며, 기존 Gradle 9.7.1 및 JDK 17.0.18 조합에서 빌드에 성공했다. Build Tools는 AGP 기본값 36.0.0을 사용한다.

AGP의 내장 Kotlin을 사용하므로 별도 `org.jetbrains.kotlin.android` 플러그인을 추가하지 않는다. Java/Kotlin 바이트코드 대상은 17이다. 앱의 Kotlin 런타임은 내장 Kotlin의 기본 의존성 관리를 따른다. 이번 단계에서는 추가 런타임 라이브러리가 필요하지 않다.

### 빈 앱의 범위

- `MainActivity : android.app.Activity`만 정의하고 MAIN/LAUNCHER intent filter를 등록한다.
- 화면 콘텐츠, XML 레이아웃, Compose 및 Material 3 의존성은 추가하지 않는다. KM-012에서 Compose 기반 화면을 구성한다.
- 플랫폼 기본 NoActionBar 테마와 재생 아이콘을 임시 앱 리소스로 참조한다. Manifest와 문자열 XML은 플랫폼 설정용이며 XML UI 레이아웃이 아니다.
- 네트워크·미디어 권한, 재생 서비스, 데이터베이스 및 비즈니스 로직은 추가하지 않는다.
- 버전은 `0.1.0` / 코드 `1`로 시작한다. 릴리스 키 설정은 하지 않아 릴리스 산출물은 서명되지 않은 APK다.

### 검증과 한계

`test lint assembleDebug assembleRelease`가 모두 성공했다. 로직과 테스트 소스가 없으므로 `testDebugUnitTest`는 `NO-SOURCE`이며 실행된 단위 테스트는 0개다. 테스트 성공을 꾸미기 위한 임시 테스트나 빈 대체 Gradle 작업은 추가하지 않는다.

API 36 에뮬레이터에 디버그 APK를 설치하여 런처 실행, 프로세스 및 resumed Activity를 확인했다. API 26/37 기기와 실기기에서는 실행하지 않았다.

`allowBackup=false`를 명시했으며 린트에는 Android 12 이상 `dataExtractionRules` 설정을 권하는 경고 1개가 있다. 현재 앱에는 저장 로직이 없다. 기기 간 데이터 이전 정책은 아직 정의하지 않았으며, 백업·전송이 모든 환경에서 차단된 것으로 간주하지 않는다. 경고를 숨기거나 린트 기준을 완화하지 않았다.

근거: [AGP 9.4 호환성](https://developer.android.com/build/releases/agp-9-4-0-release-notes), [내장 Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin), [SDK 설정과 minor API](https://developer.android.com/build), [Google SDK 메타데이터](https://dl.google.com/android/repository/repository2-3.xml).

## ADR-011 — Compose 및 Material 3 기본 화면

- 날짜: 2026-09-02
- 대상 작업: KM-012
- 상태: 채택

### 결정과 근거

AGP 9.4.0 POM에서 내장 Kotlin Gradle Plugin이 2.2.10임을 확인했다. Compose 컴파일러 플러그인 `org.jetbrains.kotlin.plugin.compose`도 2.2.10을 사용한다. 별도 Android Kotlin 플러그인을 추가하지 않으며 기존 빌드 도구 버전을 유지한다.

기존 Android 플랫폼 API만으로는 요구된 Compose·Material 3 UI를 구성할 수 없으므로 다음 최소 의존성을 Version Catalog에 추가한다. 기본 화면 구현에 충분한 안정 버전 조합을 고정하고 실제 컴파일·실행으로 검증했다. 최신 버전 조합을 검증했다는 의미는 아니다.

| 의존성 | 버전 | 필요성 |
| --- | --- | --- |
| Compose BOM | 2025.08.00 | Compose 라이브러리 버전 정렬 |
| Activity Compose | 1.11.0 | ComponentActivity의 Compose 콘텐츠와 시스템 바 설정 |
| Compose Runtime / UI | BOM: 1.9.0 | Composable, Modifier, 문자열 리소스 |
| Foundation Layout | BOM: 1.9.0 | 중앙 배치와 시스템 바 안전 영역 |
| Material 3 | BOM: 1.3.2 | 테마, Surface 및 Text |

직접 사용하는 Compose API의 모듈을 명시하며 추가 이미지·내비게이션·DI·테스트 프레임워크 의존성은 없다. Material 3 기본 밝은 색상과 typography를 사용한다. 시스템 바에는 어두운 아이콘을 적용하고 콘텐츠는 안전 영역 안에 배치한다. 사용자 테마 선택 및 다크 모드 기능은 KM-152 범위다.

### 검증과 한계

- `gradlew.bat test lint assembleDebug assembleRelease --continue --no-daemon --console=plain`: PASS, 92개 작업 실행.
- 비즈니스 로직이 없는 기본 화면이므로 단위 테스트는 0개(`NO-SOURCE`)다. 대신 `scripts/verify-km012.ps1`에 실제 APK 설치·콜드 실행·UI 계층의 앱 이름 확인을 자동 검증으로 추가했다.
- API 36 에뮬레이터에서 해당 스크립트 PASS. 캡처에서 화면 중앙 문구, 밝은 배경, 읽을 수 있는 시스템 바를 확인했다. 캡처 및 UI 계층은 `captures/km-012/`에 보관하며 Git에서 제외한다.
- 린트 오류 0개·경고 4개: 기존 `DataExtractionRules` 1개, BOM/Activity의 최신 버전 안내 2개, Compose 컴파일러 최신 버전 안내 1개. 내장 Kotlin과 컴파일러 버전을 일치시켰으며 경고를 억제하지 않았다.
- 빌드는 성공했으나 기본 Gradle 메타스페이스 제한 경고와 `libandroidx.graphics.path.so` 심볼 제거 불가 안내가 있었다. 라이브러리는 그대로 패키징됐으며 현재 실행 검증을 통과했다. 메모리 설정·NDK 설정을 이번 작업에서 변경하지 않았다.
- API 26/37 및 실기기는 미검증이다. KM-012는 에뮬레이터 실행을 요구하며 실기기 재생 Gate는 이 작업의 검증 대상이 아니다. 릴리스 APK는 여전히 미서명이다.

근거: [Compose 컴파일러 설정](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler), [Activity 안정 버전 이력](https://developer.android.com/jetpack/androidx/releases/activity#1.11.0), [공식 Compose BOM POM](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/2025.08.00/compose-bom-2025.08.00.pom).

## ADR-012 — Hilt 의존성 주입 기반

- 날짜: 2026-09-02
- 대상 작업: KM-013
- 상태: 채택

### 결정과 근거

ARCHITECTURE.md의 Hilt 선택에 따라 `KeuneyApp : Application`에 `@HiltAndroidApp`을 적용하고 Manifest에 등록한다. `MainActivity`는 `@AndroidEntryPoint`로 연결한다. 기능별 binding은 해당 기능을 구현하는 작업에서 추가한다.

기존 스택에는 Hilt 및 코드 생성 도구가 없어 다음 안정 의존성을 Version Catalog에 등록한다. Hilt 플러그인·런타임·컴파일러·테스트 라이브러리는 동일 버전으로 유지한다.

| 의존성 | 버전 | 필요성 |
| --- | --- | --- |
| Hilt 플러그인, hilt-android, hilt-compiler | 2.60.1 | Android 진입점과 DI 컴포넌트 생성 |
| KSP | 2.3.11 | Kotlin 소스의 Hilt annotation 처리 |
| hilt-android-testing | 2.60.1 | 테스트 컴포넌트 및 HiltAndroidRule |
| AndroidX Test Runner | 1.7.0 | 에뮬레이터에서 계측 테스트 실행 |
| JUnit | 4.13.2 | 테스트 선언, rule 및 assertion |

AGP 내장 Kotlin을 유지하고 KSP로 처리하므로 kapt 및 별도 Android Kotlin 플러그인은 추가하지 않는다. `ksp`와 `kspAndroidTest`에 Hilt 컴파일러를 등록한다. 작성하는 소스는 Kotlin이며 Hilt가 빌드 중 생성하는 Java는 빌드 산출물이다.

### 테스트 설계

`HiltTestRunner`는 계측 테스트에서만 `HiltTestApplication`을 사용한다. `HiltInjectionTest`는 실제 Hilt 컴포넌트를 생성해 샘플 생성자 의존성 주입, `@ApplicationContext` 전달 및 `@Singleton` 인스턴스 공유를 검증한다. 샘플과 테스트 러너는 `androidTest`에만 존재한다. 테스트 전용 의존성은 일반 앱 APK에 포함하지 않는다.

일반 앱의 Manifest/Application/Activity 연결은 별도로 디버그 APK를 설치·실행해 검증한다. 단위 테스트 명령과 계측 테스트 명령을 구분하여 결과를 기록한다.

### 빌드 메모리 수정

첫 전체 검증 중 Gradle 기본 메타스페이스 한도 384MB에서 `OutOfMemoryError: Metaspace`가 발생했다. 데몬 로그로 원인을 확인한 후 이번 검증에서 시작한 데몬만 종료했다. 프로젝트 `gradle.properties`에 최대 힙 1GB·메타스페이스 1GB와 UTF-8을 설정한다. Hilt/KSP·린트의 클래스 로딩을 수용하기 위한 프로젝트 범위 설정이며 사용자 전역 Gradle 설정은 변경하지 않는다. 실패한 실행을 성공으로 계산하지 않고 필수 검증 전체를 재실행한다.

### 실행 명령 및 결과

검증 프로세스에서 JDK 17.0.18, 기존 Android SDK 및 프로젝트 `.gradle/user-home`을 지정했다.

1. `gradlew.bat test lint assembleDebug assembleRelease assembleDebugAndroidTest --continue --no-daemon --console=plain`: FAIL. 위 Metaspace 오류 확인 후 해당 데몬을 종료하여 종료 코드 1.
2. `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest --continue --no-daemon --console=plain`: 메모리 수정 후 PASS, 45초. 테스트 생성자 qualifier의 애너테이션 대상 경고를 `@param:ApplicationContext`로 수정했다.
3. 같은 전체 검증 명령을 최종 소스에서 다시 실행: PASS, 31초, 140개 작업 중 6개 실행·134개 최신 상태.
4. `scripts/verify-km012.ps1`: Hilt를 적용한 실제 앱 APK 설치·콜드 실행·기본 문구 표시 PASS. 기존 스크립트를 변경 없이 재사용했다.

`connectedDebugAndroidTest` 결과는 API 36 에뮬레이터에서 테스트 1개, 실패 0개, 오류 0개, 건너뜀 0개다. 보고서는 `app/build/reports/androidTests/connected/debug/index.html`이며 단위 테스트는 소스가 없어 0개(`NO-SOURCE`)다. 린트는 오류 0개·기존 경고 4개다. 추가한 Hilt/KSP 의존성과 테스트 코드에 관한 린트 경고는 없다.

### 남은 한계

실기기 및 API 26/37에서는 실행하지 않았다. 이번 DI 기반 작업은 실기기 재생 검증 대상이 아니다. 기존 백업 설정·의존성 버전 경고 4개와 미서명 릴리스 상태는 유지한다. 최초 빌드에서 Compose 네이티브 라이브러리의 심볼 제거 안내가 있었으며 패키징 자체는 통과했다. 프로젝트의 JVM 최대 메모리 설정은 빌드 머신 자원 사용에 영향을 준다.

근거: [Hilt Gradle 및 KSP 설정](https://dagger.dev/hilt/gradle-setup.html), [Hilt 계측 테스트](https://dagger.dev/hilt/instrumentation-testing.html), [Hilt 2.60.1](https://github.com/google/dagger/releases/tag/dagger-2.60.1), [KSP 2.3.11](https://github.com/google/ksp/releases/tag/2.3.11), [AndroidX Test 릴리스](https://developer.android.com/jetpack/androidx/releases/test).

## ADR-013 — Room 초기 데이터베이스 (KM-014)

- 날짜: 2026-09-02
- Room 2.8.4의 runtime/compiler/Gradle plugin을 추가한다. 기존 KSP를 사용하고 Room plugin으로 스키마 입출력을 관리한다. 별도 Room 테스트 라이브러리는 현재 smoke test에 필요하지 않다.
- `KeuneyDatabase` 버전 1과 Hilt의 앱 범위 인스턴스를 구성한다. UI·ViewModel 접근과 라이브러리 기능은 추가하지 않는다.
- `entities=[]`로 실제 빌드했으나 Room이 엔티티 목록을 요구하여 실패했다. 따라서 데이터가 비어 있는 `schema_baseline(id)` 테이블 하나만 둔다. 음악 메타데이터 및 resolved URL 컬럼은 없으며 쓰기 DAO도 없다.
- `app/schemas/` JSON을 버전 관리한다. 향후 초기화 테이블 제거나 라이브러리 테이블 추가도 스키마 버전 증가 및 명시적 Migration과 테스트를 요구한다. `fallbackToDestructiveMigration`은 사용하지 않으며 migration 누락·downgrade는 실패시키는 Room 기본 정책을 유지한다.
- 파일 기반 DB 생성→열기→닫기→재열기로 버전 1과 빈 테이블을 검증한다. 테스트는 고유 이름의 DB만 생성·삭제한다.
- 근거: [Room 안정 버전과 설정](https://developer.android.com/jetpack/androidx/releases/room), [Room 마이그레이션](https://developer.android.com/training/data-storage/room/migrating-db-versions).

## ADR-014 — DataStore 설정 저장소 (KM-015)

- DataStore Preferences Core 1.2.1을 사용한다. Android 전용 migration/delegate API는 필요하지 않아 core 아티팩트만 추가한다. 직접 사용하는 Flow·suspend API를 위해 coroutines-core 1.10.2를 Version Catalog에 명시한다.
- `SettingsRepository`는 테마 Flow와 suspend 쓰기만 노출한다. 실제 저장은 DataStore 구현이 담당하며 Hilt가 앱 범위의 파일별 단일 DataStore를 제공한다. UI 연결은 테마 화면 작업에서 수행한다.
- 저장 값은 System/Light/Dark이며 미설정·알 수 없는 값은 System으로 해석한다. 파일 읽기·쓰기 실패 및 취소는 숨기지 않고 호출 계층에 전달한다. 현재 UI에서 호출하지 않으며 향후 UI 연결 시 앱 오류로 매핑해야 한다.
- 설정 파일은 내부 `files/datastore/settings.preferences_pb`다. 계정 정보·resolved URL은 저장하지 않는다. enum 이름은 저장 포맷이므로 이름 변경 시 변환 정책이 필요하다.
- 기존 JUnit을 단위 테스트에도 사용한다. 임시 파일을 대상으로 기본값, 모든 테마의 쓰기·읽기·재열기 지속성, 알 수 없는 값 fallback을 검증하고 각 DataStore 작업을 취소·종료한 뒤 파일을 재연다.
- Windows JVM에서 기본 FileStorage가 기존 파일을 rename으로 교체하지 못해 지속성 테스트가 실패했다. Path 팩토리도 같은 FileStorage로 연결됨을 실패 스택에서 확인했다. 앱·테스트가 공통 `createSettingsDataStore`를 사용하며 공식 `OkioStorage`와 `PreferencesSerializer`를 명시한다. 이미 전이 의존성으로 포함된 datastore-core-okio 1.2.1·Okio 3.9.1을 직접 사용하므로 Catalog에 명시했다. 테스트의 파일 재열기·반복 갱신 조건은 유지한다.
- 근거: [DataStore 안정 버전과 core 옵션](https://developer.android.com/jetpack/androidx/releases/datastore), [Coroutines 1.10.2](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.10.2).

## ADR-015 — Media3 의존성 (KM-016)

- Media3 안정 버전 1.11.0의 ExoPlayer와 Session만 추가하고 동일 버전으로 정렬한다. 기존 스택에는 재생 엔진과 MediaLibraryService가 없어 필수다.
- 플레이어 인스턴스와 서비스는 후속 재생 기반 작업에서 생성한다. UI용 Media3 모듈·스트리밍 프로토콜 확장·cache는 현재 추가하지 않는다.
- 근거: [Media3 1.11.0 공식 릴리스](https://developer.android.com/jetpack/androidx/releases/media3#1.11.0).

## ADR-016 — Ktor 네트워크 기반 (KM-017)

- 기존 Kotlin 2.2.10 컴파일러를 유지하는 안정 조합으로 Ktor 3.2.3, kotlinx.serialization 1.9.0을 사용한다. serialization plugin은 Kotlin과 같은 2.2.10이다. 최신 Ktor 버전 채택을 의미하지 않는다.
- Ktor core, OkHttp engine, ContentNegotiation, Kotlin JSON 모듈을 추가한다. Hilt가 프로세스 수명의 공용 HttpClient를 제공하며 실제 Provider 요청은 후속 Source 작업에서만 수행한다.
- connect 10초 / socket 20초 / request 30초. 자동 애플리케이션 재시도는 추가하지 않는다. HTTP 오류는 실패로 처리하고 취소를 전파한다. Provider 계층에서 앱 오류로 변환해야 하며 UI에서 이 클라이언트를 호출하지 않는다.
- JSON의 미지원 필드는 무시한다. 로깅 플러그인·인터셉터, 쿠키 저장소, 인증 정보는 구성하지 않는다. Manifest에는 INTERNET만 추가한다.
- 테스트는 같은 클라이언트 설정과 Ktor MockEngine을 사용해 시간 제한 전달·JSON 디코딩·요청 timeout·호출자 취소를 실제 네트워크 없이 확인한다. mock 모듈은 테스트에만 포함한다.
- 근거: [Ktor 3.2.3](https://github.com/ktorio/ktor/releases/tag/3.2.3), [타임아웃](https://ktor.io/docs/client-timeout.html), [MockEngine](https://ktor.io/docs/client-testing.html).

## ADR-017 — Coil 기반 아트워크 컴포넌트 (KM-018)

- Coil Compose 안정 버전 3.4.0을 추가한다. `Artwork`는 AsyncImage에 크기·접근성 설명을 전달하며 로딩·오류·모델 없음에 공통 Material 3 색상 자리표시자를 사용한다.
- 현재 인수 조건은 Compose 의존성과 placeholder 구성이다. 로컬 이미지 모델과 placeholder 기반만 마련하며 네트워크 fetcher는 실제 원격 artwork 연결 작업에서 필요에 맞게 추가한다. 이 상태에서 원격 URL 로딩이 구현된 것으로 간주하지 않는다.
- 화면 기능 및 상태 로직은 추가하지 않으며 컴파일·린트로 Compose API 호환성을 확인한다.
- 근거: [Coil 공식 프로젝트](https://github.com/coil-kt/coil), [AsyncImage API](https://coil-kt.github.io/coil/api/coil-compose-core/coil3.compose/-async-image.html).

## ADR-018 — GitHub Actions 기본 검증 (KM-019)

- push, pull_request, workflow_dispatch에서 JDK 17과 SDK 37.2 / Build Tools 36.0.0을 준비하고 Wrapper의 test/lint/assembleDebug를 차례로 실행한다. Ubuntu 24.04를 사용한다.
- 공식 릴리스 API에서 확인한 checkout v7.0.1, setup-java v6.0.0, setup-android v4.0.1을 커밋 SHA로 고정한다. 권한은 contents:read이며 checkout 인증 정보를 보존하지 않는다. 서명 키나 별도 비밀값이 필요하지 않다.
- 기본 CI는 단위 테스트와 정적 검사·빌드까지만 수행한다. 외부 Source 계약과 실기기 재생 검증을 대체하지 않는다.
- 워크플로 파일과 로컬 명령 통과가 현재 인수 조건이다. GitHub에 push하거나 원격 workflow를 실행하지 않은 상태에서는 원격 CI 성공으로 보고하지 않는다.
- 근거: [Gradle CI](https://docs.github.com/en/actions/tutorials/build-and-test-code/java-with-gradle), [setup-java](https://github.com/actions/setup-java), [setup-android](https://github.com/android-actions/setup-android).

## ADR-019 — MusicService 기본 등록 (KM-030)

- `MusicService : MediaLibraryService`를 Manifest에 등록한다. foreground service 유형은 mediaPlayback이며 해당 기본·미디어 권한을 명시한다.
- Media3 Library 서비스 action과 플랫폼 MediaBrowser 호환 action을 등록한다. 세션은 KM-032에서 구성하므로 현재 onGetSession은 null을 반환한다.
- 계측 테스트는 실제 서비스 바인딩으로 시작·연결 성공과 Manifest 서비스 유형을 검증한다. 이 단계는 실제 오디오나 foreground 재생 검증을 의미하지 않는다.
- 근거: [MediaLibraryService](https://developer.android.com/reference/androidx/media3/session/MediaLibraryService), [백그라운드 서비스 구성](https://developer.android.com/media/media3/session/background-playback).

## ADR-020 — 서비스의 ExoPlayer 소유권 (KM-031)

- MusicService.onCreate에서 ExoPlayer를 생성하고 private 필드로 소유한다. Activity와 ViewModel에는 엔진 참조를 제공하지 않는다.
- onDestroy에서 release 후 참조를 비운다. 아직 세션·컨트롤러·재생 소스를 연결하지 않는다.
- 서비스 바인딩 계측 테스트 및 Media3의 실제 Init/Release 로그로 생성·해제 경로를 확인한다. 서비스 lifecycle 자체는 Android 계측으로 검증하며 순수 비즈니스 로직은 추가하지 않는다.

## ADR-021 — 서비스의 MediaLibrarySession (KM-032)

- onCreate에서 서비스가 소유한 동일 ExoPlayer를 MediaLibrarySession에 연결하고 onGetSession에서 반환한다. onDestroy에서 player와 session을 모두 해제한다.
- 현재 라이브러리 탐색은 구현하지 않아 기본 Library Callback을 사용한다. MediaController의 실제 비동기 연결과 유휴 플레이어 상태·재생 명령 가용성을 계측 테스트로 확인한다.
- Activity/화면 연결은 KM-033에서 수행한다. 새 의존성은 없다.

## ADR-022 — 화면 생명주기와 컨트롤러 연결 (KM-033)

- PlayerConnection은 application context로 MediaController만 소유하며 MainActivity.onStart/onStop에서 연결·해제한다. stop/pause 명령이나 서비스 종료를 화면 종료에 연결하지 않는다.
- 메인 스레드에서만 조작하고 연결 Future의 동일성을 확인해 취소된 연결의 늦은 응답을 무시한다. releaseFuture로 연결 도중 해제도 처리한다. 실패 원문 대신 Unavailable 상태를 노출한다.
- 연결 상태는 읽기 전용 StateFlow다. 연결 중 취소·중복 호출·재연결은 실제 서비스 계측 테스트로 검사한다. Android 연결 관리만 추가하며 순수 비즈니스 로직은 없다. 재생 중 서비스 유지 검증은 KM-035 범위다.
- 근거: [MediaController 연결과 해제](https://developer.android.com/media/media3/session/connect-to-media-app).

## ADR-023 — 로컬 테스트 음원과 기본 재생 화면 (KM-034)

- 외부 Source 없이 재현 가능한 120초 mono PCM WAV를 생성해 앱에 포함한다. 스크립트가 낮은 진폭의 단순 음계를 합성하며 외부 녹음은 사용하지 않는다. 실제 음악 Source·다운로드·cache 기능은 없다.
- 서비스가 고정 ID known-test-tone과 리소스 URI를 가진 미디어 항목 하나를 준비한다. 재생은 사용자 명령 이후 시작하며 Activity는 MediaController의 play/pause/seek만 호출한다. URI는 APK 리소스 주소이고 영구 저장하는 resolved stream URL이 아니다.
- Hilt ViewModel이 PlayerConnection을 소유하고 읽기 전용 상태와 명령을 화면에 전달한다. Activity onStop은 컨트롤러만 분리한다. Player 이벤트와 연결 중 250ms 간격의 위치 갱신을 상태로 변환하며 해제 시 listener·갱신 작업을 제거한다.
- 직접 사용하는 lifecycle-viewmodel/runtime-compose 2.9.4를 Catalog에 명시한다. 이미 전이 의존성으로 해결된 안정 버전이며 화면의 lifecycle에 맞춰 상태를 수집하기 위해 필요하다. [공식 릴리스](https://developer.android.com/jetpack/androidx/releases/lifecycle#2.9.4).
- 재생 상태와 시간 경계는 JVM 단위 테스트, 실제 재생·일시정지·탐색·재개는 계측 테스트로 검사한다. 배경·화면 꺼짐·Bluetooth 실기기 검증을 대체하지 않는다.

## ADR-024 — 백그라운드 재생 검증 (KM-035)

- MediaLibraryService의 기본 foreground 전환을 사용한다. Activity 종료 시 player를 멈추는 별도 코드는 추가하지 않는다.
- 실제 Activity를 시작하고 Home 입력 후 Activity와 UI/테스트 컨트롤러를 모두 해제한다. 32초 대기 후 재연결 전에 dumpsys로 foreground 서비스를 확인하고, 재연결하여 계속 재생 중이며 위치가 30초 이상 증가했는지 검사한다.
- Hilt 계측 환경에서 실제 MainActivity를 사용한다. 에뮬레이터 결과와 실기기 결과를 별도로 기록하며 실기기 미검증이면 KM-035는 미완료다.
- 회귀 검사에서 기존 세션 테스트가 항상 새 유휴 세션을 가정하여 실패했다. MediaController 해제와 Android 서비스 종료는 비동기이며 재생 후 세션이 남을 수 있다. 세션 테스트가 stop 명령으로 자신의 시작 상태를 준비하도록 수정했다. 백그라운드 테스트는 재생 중인 32초 구간에는 어떤 컨트롤러도 유지하지 않으며 재생 상태 검증 조건을 완화하지 않았다.

## ADR-025 — 화면 꺼짐 재생 (KM-036)

- MusicService의 ExoPlayer에 WAKE_MODE_LOCAL을 명시하고 Manifest에 WAKE_LOCK을 명시한다. Media3 1.11의 기본 설정도 LOCAL이지만 화면 꺼짐 재생 정책을 명시적으로 유지한다. wake lock은 Media3가 재생 상태에 맞춰 관리하며 화면을 켜 두거나 배터리 최적화 제외를 강제하지 않는다.
- 계측 테스트는 실제 Activity에서 재생을 시작하고 화면을 끈 뒤 Activity/컨트롤러를 해제한다. 62초간 매초 화면 꺼짐을 확인하고 화면을 깨우기 전에 재연결하여 60초 이상 진행·재생 중 상태를 검사한다. 마지막에 재생을 정리하고 화면을 깨운다.
- 신규 의존성과 비즈니스 로직은 없다. 충전 상태 및 OEM별 장시간 동작의 검증 범위는 결과 기록에서 구분한다.
- 근거: [ExoPlayer.Builder 기본값과 setWakeMode](https://developer.android.com/reference/androidx/media3/exoplayer/ExoPlayer.Builder).

## ADR-026 — 기본 미디어 알림 (KM-037)

- Media3의 기본 알림·플랫폼 미디어 세션을 사용한다. 제목·아티스트와 앱 내부에서 그린 PNG 재생 아이콘을 metadata로 전달하고 immutable PendingIntent로 앱 화면에 복귀하도록 연결한다.
- PRD에 따라 POST_NOTIFICATIONS를 선언한다. 현재 알림은 미디어 세션 알림이므로 Android 13 이상 권한 예외 대상이며 시작 시 추가 권한 창은 강제하지 않는다. 일반 알림을 도입할 경우 별도로 권한 흐름이 필요하다.
- 이전/다음은 Media3 playlist와 Player command 가용성에 따르는 기본 구조를 유지한다. 현재 테스트 음원은 한 곡이므로 없는 다음 곡을 위해 가짜 버튼을 활성화하지 않는다.
- 계측 테스트가 앱 자신의 활성 알림에서 제목·아티스트·큰 이미지·복귀 Intent를 확인하고 실제 알림 PendingIntent로 pause/play를 수행한다. 신규 의존성은 없다.
- 근거: [Media3 백그라운드 재생과 알림](https://developer.android.com/media/media3/session/background-playback), [알림 권한 예외](https://developer.android.com/develop/ui/views/notifications/notification-permission#exemptions).

## ADR-027 — 오디오 포커스 (KM-039)

- ExoPlayer에 USAGE_MEDIA / CONTENT_TYPE_MUSIC과 handleAudioFocus=true를 설정한다. 별도 포커스 상태 머신이나 AudioManager listener를 앱 코드에 중복 구현하지 않는다.
- 일시적 손실은 재생을 억제하고 포커스 복귀 시 Media3 정책으로 재개한다. 영구 손실은 재생 요청을 해제하며 임의 자동 재개하지 않는다. 실제 isPlaying=false를 UI의 일시정지 상태로 변환하는 기존 mapper를 사용한다.
- 단위 테스트는 재생 의도가 남아 있어도 억제된 상태가 일시정지로 표시됨을 확인한다. 실기기 계측 테스트는 별도 AudioFocusRequest로 일시적/영구적 포커스 경쟁을 만들어 중단·위치 정지·복귀 정책을 확인한다. 전화·Bluetooth·헤드셋 실물 이벤트까지 검증하는 것은 아니다.
- 근거: [Android 오디오 포커스와 ExoPlayer 위임](https://developer.android.com/media/optimize/audio-focus).

## 기존 설계의 ADR 관리

### ADR-063 — 네트워크 끊김과 재연결 (KM-132)

- **짧은 끊김은 손대지 않는다.** ExoPlayer가 이미 스스로 몇 번 다시 읽어 보고, 그 사이 재생은 버퍼로 이어진다. 실기기에서 WiFi를 끈 뒤에도 재생이 그대로 이어지는 것을 확인했다. 재시도 횟수를 늘리는 방법도 있지만, 그것은 "준비 중"으로 오래 붙잡아 두는 쪽이고 끊긴 것을 알려 주지 않는다.
- **멈춘 뒤에는 왜 멈췄는지 말한다.** 재생 상태에 [PlaybackFailure]를 실어 네트워크와 소스 두 가지로 나눈다. 화면은 Media3 오류 코드를 해석하지 않는다(ARCHITECTURE 19). 기다리면 되는 실패와 다른 곡을 골라야 하는 실패가 같은 문구로 보이면 사용자는 할 일을 알 수 없다.
- 연결 실패와 시간 초과만 네트워크로 본다. 분류하지 못한 입출력 오류(`ERROR_CODE_IO_UNSPECIFIED`)는 네트워크로 보지 않는다. 그 코드는 "무엇인지 모르겠다"는 뜻이고, 연결이 돌아올 때마다 같은 실패를 되풀이할 뿐이다.
- 그래서 `TrackStreamResolver`가 해석 실패를 `DataSourceException`의 오류 코드로 바꿔 보낸다. 이전에는 연결이 끊겨서 주소를 못 푼 것과 곡 자체를 못 가져오는 것이 똑같은 `IOException("Stream unavailable")`이었다. 코드만 넘기고 원문 예외와 메시지는 여기서 끊는다(AGENTS.md 12·13).
- **연결이 돌아오면 이어 붙인다.** `ConnectivityManager`의 기본 연결 감시를 서비스가 걸고, 돌아왔을 때 멈춘 자리에서 다시 준비시킨다. 시간을 두고 되풀이해 보지 않는다. 끊긴 동안의 재시도는 어차피 실패하고 얼마 만에 돌아올지는 알 수 없다.
- 되살리는 대상은 **네트워크 때문에 멈췄고 그때 듣던 중이던 재생**뿐이다. 사용자가 멈춰 둔 것을 연결이 돌아왔다고 다시 트는 것은 회복이 아니라 참견이다. 판단은 `NetworkRecovery`가 상태만으로 하고, 실제로 준비시키는 일은 재생을 소유한 `MusicService`가 한다(ADR-002). 규칙을 떼어 두었기 때문에 기기 없이 단위 검사로 고정할 수 있다.
- 자동 시도는 연속 3회로 제한하고 한 번이라도 다시 재생되면 처음으로 되돌린다. 연결이 붙었다 끊겼다 하는 곳에서 같은 실패를 끝없이 되풀이하지 않기 위해서다. 그 뒤에는 "다시 시도" 버튼이 남는다. 기다리는 것 말고 지금 눌러 볼 방법도 있어야 한다.
- WiFi 전용 재생으로 막힌 것(ADR-036)은 네트워크 실패가 아니다. 연결이 돌아와도 요금제가 그대로면 다시 막힌다. 자동 회복 대상이 되지 않도록 분류하지 않은 입출력 오류로 남긴다.
- `ACCESS_NETWORK_STATE`를 앱 manifest에 선언한다. 지금까지는 의존성이 병합해 준 권한으로 요금제 확인이 동작했다. 쓰는 쪽이 선언하지 않으면 그 의존성이 바뀔 때 조용히 깨진다.
- 근거: [ExoPlayer 오류 코드](https://developer.android.com/reference/androidx/media3/common/PlaybackException), [기본 연결 감시](https://developer.android.com/reference/android/net/ConnectivityManager#registerDefaultNetworkCallback(android.net.ConnectivityManager.NetworkCallback)).

### ADR-062 — 라이브러리 화면 구성 (KM-116)

- 세 구획(최근 재생·즐겨찾기·재생목록)은 KM-112·113·115에서 이미 들어왔다. 이 작업은 그것들이 실제 사용에서 무너지는 지점을 고쳤다.
- **곡 구획은 앞의 다섯 개만 보여주고 나머지는 "더 보기"로 넘긴다.** 최근 재생은 50개까지 쌓이고 즐겨찾기는 제한이 없다. 전부 쏟으면 아래 구획이 화면 밖으로 밀려 재생목록에 닿으려면 수십 곡을 지나야 한다. 구획을 접는 방법도 있지만 접힌 구획은 찾아온 것을 감춘다. 목록은 이미 최근 순으로 정렬돼 있어 앞의 다섯 개가 가장 볼 만하다.
- 재생목록 구획은 전부 보여준다. 줄마다 곡이 아니라 재생목록이 오고 각 줄이 짧아 아래를 밀어내지 않는다. 재생목록이 많아지는 것은 v0.1에서 예상하는 상황이 아니다.
- 전체 목록은 목적지 `library-section/{section}` 하나가 구획 이름을 받아 그린다. 구획마다 화면을 만들면 같은 목록을 두 번 쓴다. 알 수 없는 이름이 오면 최근 재생을 보여준다. 목적지 인자는 문자열이라 무엇이든 올 수 있고, 빈 화면보다 낫다.
- **즐겨찾기 전체 목록에서는 줄마다 해제 버튼을 둔다.** 그 화면에 들어온 것은 목록을 정리하려는 뜻이다. 요약 화면에는 두지 않는다. 훑어보다 실수로 누르기 쉽다. 재생목록 화면과 같은 규칙이며(ADR-059) 이로써 KM-112에서 남겨 둔 "목록에서 바로 해제할 방법"이 채워졌다.
- 최근 재생에는 줄마다 버튼이 없다. 한 곡만 빼는 것은 뜻이 약하고 전체 지우기는 요약 화면 머리에 있다.
- **세 구획이 모두 비면 구획을 나열하지 않는다.** 첫 실행에서 "없습니다"가 세 줄인 화면은 무엇을 해야 하는지 알려주지 않는다. 한 줄 안내와 재생목록 만들기 버튼만 둔다. 재생목록은 곡 없이도 만들 수 있는 유일한 것이라 여기서 시작할 수 있다.
- 두 곡 구획이 같은 그리기 함수를 쓴다. 머리·앞부분·더 보기의 모양이 구획마다 달라지면 같은 화면으로 보이지 않는다.

### ADR-061 — 재생 기록 (KM-115)

- **"들었다"의 기준은 재생 시작이 아니라 얼마 이상 들었을 때다.** 시작만으로 남기면 훑어보며 넘긴 곡까지 들어와 최근 재생이 방금 스쳐 간 목록이 된다. 기준은 10초이며, 곡이 20초보다 짧으면 절반만 들어도 남긴다. 그렇게 하지 않으면 짧은 곡은 끝까지 들어도 기록되지 않는다. 규칙은 `PlaybackHistory.listenedThresholdMs`로 떼어 단위 검사로 고정했다.
- **기록은 재생을 소유한 `MusicService`가 남긴다.** 화면이 닫혀도 배경 재생은 이어지는데, ViewModel이 남기면 그때 관찰이 끊겨 기록이 빠진다. 배경 재생이 이 앱의 핵심이므로 그 경로가 비면 안 된다. 반복 모드를 서비스가 적용하기로 한 것과 같은 이유다(ADR-054).
- 위치를 주기적으로 살피지 않는다. 재생이 시작될 때 남은 시간만큼 기다린 뒤 여전히 같은 곡이 재생 중인지 확인한다. 폴링 없이 같은 일을 한다.
- **"이미 남겼다"는 판단을 곡 ID가 아니라 재생 위치로 되돌린다.** 위치가 기준보다 앞이면 새로 듣기 시작한 것으로 본다. 처음에는 곡 ID로만 판단했는데, 같은 곡을 다시 골라 재생하면 Media3가 전환 콜백을 주지 않아 기록이 갱신되지 않았다. 계측이 스위트에서만 실패해 알아냈고 실제 사용 경로(최근 재생에서 같은 곡을 다시 누르기)에서도 같은 문제가 생긴다.
- 중복 정책: **곡마다 한 행만 남긴다.** `PlaybackHistoryDao.record`가 그 곡의 이전 기록을 지우고 새로 넣는다. 목록에는 곡별로 한 번만 나오므로 여러 행을 쌓아도 보이는 것은 같은데 표만 자란다. 곡마다 한 행이면 표 크기가 들은 곡 수를 넘지 않는다. 목록 조회는 곡별 최신 시각으로 묶는 쿼리를 그대로 두었다. 이전 판이 남긴 여러 행도 올바르게 읽힌다.
- 최근 재생은 개수를 제한해 읽는다(50개). "최근"이 수백 곡이면 목록을 훑는 것보다 검색이 빠르다.
- 라이브러리 화면의 구획 순서는 최근 재생 → 즐겨찾기 → 재생목록이다. PRD 35의 Home 구획 순서를 따랐고, 가장 자주 보는 것이 위에 온다.
- 지우기는 최근 재생 구획 머리에 두고 목록이 비어 있으면 감춘다. 확인을 묻지 않는다. 재생목록 삭제와 같은 판단이며 되돌릴 수 없다는 점은 남는 위험이다.
- **"history enabled" 설정은 넣지 않았다.** PRD 34의 DataStore 항목에 있지만 켜고 끌 자리가 없다. 설정 화면은 KM-153이며 그때 함께 넣는다. 인수 조건에도 없다.
- `NowPlaying.toTrack`을 `feature/player`에서 `core/player`로 옮겼다. 서비스도 써야 하는 도메인 변환이며 화면 계층에 둘 이유가 없다.
- 계측에서 서비스가 남긴 기록을 관찰로 볼 수 없다. Hilt가 검사마다 새 싱글턴 컴포넌트를 만들어 검사의 저장소가 서비스의 것과 다른 인스턴스이기 때문이다. 데이터베이스 파일은 같아 다시 물어보면 보이므로 그렇게 확인하고, 실제 앱의 화면 갱신은 기기에서 눈으로 확인했다.

### ADR-060 — 재생목록 재생 검증 (KM-114)

- 이 작업에서 앱 코드는 바뀌지 않았다. 재생목록 → 대기열은 KM-113이, 이전·다음 명령과 가용성은 KM-094가, 대기열 상태 노출은 KM-097이 이미 만들었다. 남은 것은 **여러 곡 대기열에서 그 세 가지가 실제로 맞물리는지 확인하는 일**이었고 그것이 KM-114의 내용이다. 할 일이 없다고 넘기지 않고 확인을 검사로 고정했다.
- 검증에 내장 테스트 음원을 두 번 넣은 대기열을 쓴다. 실제 소리가 나고 네트워크가 필요하지 않으며 끝까지 기다릴 수 있다. 두 항목의 ID가 같아 어디에 있는지는 KM-097이 더한 `queueIndex`로 본다. 가짜 ID를 쓰면 주소 해석이 실패해 이어 듣기를 볼 수 없다.
- 이어 듣기는 곡 끝 2초 앞에서 재생을 시작해 다음 곡으로 넘어가는 것을 본다. 120초를 다 기다리지 않으면서도 실제 종료 경로를 지난다.
- 이전은 두 갈래를 모두 확인한다. 곡을 막 시작한 자리에서는 앞 곡으로 가고, 한참 들은 자리에서는 그 곡의 처음으로 되돌린다. Media3의 `maxSeekToPreviousPosition`(기본 3초) 규칙이며 KM-094에서 한 곡으로 본 것이 여기서 여러 곡 기준으로 확인된다.
- **명령 가용성은 대기열·자리와 같은 순간에 오지 않는다.** 처음에는 `seekToNext` 뒤 `hasNext`를 즉시 읽어 검사가 깨졌다. 컨트롤러가 자리를 먼저 알리고 명령 집합을 나중에 갱신하기 때문이다. 가용성을 조건에 넣어 기다리도록 고쳤다. ShuffleTest에서 얻은 교훈과 같다.
- 화면 확인은 실제 재생목록으로 했다. 다만 UI의 이전 버튼은 두 번째 곡을 몇 초 들은 뒤 눌러 "그 곡의 처음으로" 갈래를 지났다. 앞 곡으로 가는 갈래는 계측 검사가 결정적으로 다룬다.

### ADR-059 — 재생목록 화면 구성 (KM-113)

- 재생목록 **목록**은 라이브러리 탭의 두 번째 구획에 두고, 한 재생목록의 **곡 목록**은 전체 화면 목적지(`playlist/{id}`)에 둔다. 라이브러리 탭은 사용자가 모은 것이 있는 자리이고, 곡 목록은 자기 공간이 필요하다. 전체 화면 목적지는 이미 둘(`now-playing`·`queue`) 있어 새로운 구조가 아니다.
- 즐겨찾기와 재생목록 두 구획이 한 `LazyColumn` 안에서 함께 흐른다. 구획마다 스크롤을 주면 같은 방향으로 겹친 스크롤이 되고 Compose가 허용하지 않는다.
- **곡을 담는 진입점은 Now Playing이다.** 지금 듣고 있는 곡이 담고 싶은 곡이고, 즐겨찾기도 그 화면에 있어 규칙이 하나로 유지된다. 검색 결과 줄마다 담기 버튼을 붙이지 않는다. KM-112에서 즐겨찾기 해제 버튼을 목록에 두지 않은 것과 같은 이유다.
- 담기 대화상자에서 새 재생목록을 고르면 만들고 그 곡을 바로 담는다. 만든 뒤 다시 고르게 하면 같은 것을 두 번 묻는 셈이다.
- **이름 바꾸기와 삭제는 재생목록 화면에 둔다.** 목록의 줄에 삭제를 붙이면 실수로 누르기 쉽고 되돌릴 방법이 없다. 그 화면에 들어온 것은 그 재생목록을 다루려는 뜻이다. 반면 곡 빼기는 그 화면의 줄마다 둔다. 대기열 화면과 같은 판단이다.
- 삭제에 확인을 묻지 않는다. 대신 목록에서 한 번 더 들어가야 닿는 자리에 뒀다. v0.1에 되돌리기가 없으므로 확인 대화상자를 넣는 편이 안전하지만, 그러면 대화상자가 셋이 된다. 담긴 곡은 외래 키가 함께 지우고 곡 메타데이터는 다른 곳에서 쓰이면 남는다(ADR-057).
- 빈 이름으로는 만들거나 바꿀 수 없다. 이름 없는 줄이 생기면 지울 수밖에 없다. 이름의 앞뒤 공백은 잘라낸다. 같은 이름은 막지 않는다. 그것은 사용자의 선택이다.
- 재생목록 화면의 곡을 누르면 그 곡부터 재생하고 재생목록 전체가 대기열이 된다. 검색·즐겨찾기와 같은 방식이다(ADR-055·058). 누르면 아무 일도 없는 줄을 두지 않기 위해 여기서 연결했고, 이어 듣기와 이전·다음의 인수 조건 확인은 KM-114다.
- 재생목록 상태는 `LibraryViewModel`이 함께 든다. Activity가 만들어 내려주므로 라이브러리 탭과 재생목록 화면, 그리고 Now Playing의 담기 대화상자가 같은 것을 본다. 목적지마다 ViewModel을 만들면 화면을 옮길 때 목록이 다시 로딩된다.

### ADR-058 — 즐겨찾기 (KM-112)

- 사용자와 정한 대로 즐겨찾기 목록을 라이브러리 탭에 바로 넣었다. 인수 조건에 Library display가 있고, 그때까지 목록을 어디에도 두지 않으면 저장은 되지만 볼 수 없는 상태가 된다. 화면 전체 구성은 KM-116이 다듬고 재생목록·최근 재생 구획은 각 기능 작업에서 더한다.
- 즐겨찾기 여부는 저장소에서 흐르는 값을 그대로 보여준다. 눌렀다는 사실을 앱이 따로 기억하면 저장이 실패했을 때 화면이 거짓을 보인다. 셔플(ADR-053)·반복(ADR-054)과 같은 방식이다.
- 곡 하나의 여부는 전체 목록을 받아 뒤지지 않고 `isFavorite(trackId)`로 묻는다. 전체 목록으로 판단하면 질문 하나에 목록 전체를 읽어야 한다.
- **세션이 준 것으로 곡을 만든다.** `NowPlaying.toTrack(durationMs)`가 그 일을 한다. 길이는 세션 metadata에 없어 재생 상태에서 받고, 알 수 없으면 넣지 않는다. 0을 넣으면 화면이 "0:00"을 사실처럼 보여준다. `SourceType`은 v0.1에 Remote 하나뿐이라 그것으로 두며, 출처가 늘어나면 세션에 어떤 출처인지 함께 실어야 하고 이 함수도 다시 봐야 한다. ADR-047에서 `Track` 재구성을 거부한 것과 다르지 않다. 그때는 없는 값을 지어내는 것이 문제였고 여기서는 넣을 수 있는 값만 넣는다.
- 즐겨찾기 목록의 항목을 누르면 그 곡부터 재생하고 목록 전체가 대기열이 된다. 검색 결과와 같은 방식이다(ADR-055). 목록마다 다르게 동작하면 사용자가 규칙을 두 번 배워야 한다.
- 라이브러리 화면에 즐겨찾기 해제 버튼을 두지 않았다. 해제는 그 곡을 재생하는 화면에서 한다. 목록에 줄마다 버튼을 붙이면 실수로 누르기 쉽고, 지금은 되돌릴 방법이 없다.
- 곡 한 줄의 모양을 `ui/components/TrackRow`로 옮겨 검색과 라이브러리가 같은 것을 쓴다. 두 곳에 같은 줄을 그리면 한쪽만 고쳐지는 날이 온다. `trackSubtitle`과 그 검사도 함께 옮겼다.
- `LibraryViewModel`을 새로 두고 Activity가 만들어 내려준다. 전체 화면 플레이어와 라이브러리 탭이 함께 쓴다. 재생 상태를 다루는 `PlayerViewModel`에 라이브러리 저장소를 넣지 않는다.
- `LibraryRepositoryImpl`의 시계는 두 번째 생성자로 갈랐다. Dagger는 Kotlin 기본값을 보지 않아 함수 타입을 주입하려 든다. 시계 하나를 위해 타입을 만들 이유는 없으므로 주입용 생성자는 시계 없이 받고 검사만 넘긴다.
- Now Playing의 "아직 준비되지 않은 기능입니다." 문구와 문자열을 지웠다. 비활성인 조작이 더 없다.

### ADR-057 — 라이브러리 저장소 경계 (KM-111)

- DAO 넷(`TrackDao`·`FavoriteDao`·`PlaylistDao`·`PlaybackHistoryDao`)을 `core/database/dao`에 두고 `core/library/LibraryRepository` 하나로 묶었다. 구현은 `data/repository/LibraryRepositoryImpl`이다. ARCHITECTURE 4가 `LibraryRepositoryImpl` 하나를 지정하고 v0.1의 라이브러리는 이 정도 크기다. 커지면 나눈다.
- DAO는 `KeuneyDatabase` 밖으로 나가지만 주입은 저장소 구현만 받는다. 화면과 ViewModel의 그래프에는 DAO가 올라가지 않는다(AGENTS.md 10). DAO 타입도 `internal`이라 모듈 밖에서 볼 수 없다.
- 읽기는 모두 `Flow`다. 즐겨찾기를 켜면 라이브러리 목록이 스스로 바뀌어야 하는데, 화면이 다시 물어보게 만들면 화면마다 갱신 시점을 따로 챙겨야 한다. 인수 조건의 Flow-based observation이 이것이다.
- 쓰기는 `Track`을 받고 구현이 곡을 먼저 저장한다. 세 표가 모두 `tracks`를 외래 키로 가리키므로 곡이 없으면 쓰기가 실패한다. 부르는 쪽이 그 순서를 알아야 할 이유가 없다.
- 시각은 저장소가 읽는다. 화면이 시계를 넘기게 만들면 화면마다 다른 시계를 쓸 수 있다. 검사에서는 시계를 주입해 고정한다.
- 어디에서도 가리키지 않는 곡은 정리한다. 즐겨찾기를 지우거나 재생목록·기록에서 빠질 때 `deleteUnreferenced`를 부른다. 남겨 두면 `tracks`가 계속 자란다. 아직 다른 곳에서 가리키면 남는다.
- 최근 재생은 곡별로 가장 최근 시각만 남겨 묶는다. 표에는 들을 때마다 행이 쌓이는데 목록에 같은 곡이 여러 번 나오면 "최근 재생"이 쓸모없어진다. 이것이 KM-115가 문서화해야 할 중복 정책의 데이터 계층 몫이며, 언제 기록할지는 KM-115가 정한다.
- 재생목록의 자리 번호를 정하는 것과 담는 것을 한 트랜잭션에 둔다. 둘로 나뉘면 같은 자리 번호가 두 번 나온다. 같은 곡을 여러 번 담을 수 있고 빼면 그 곡의 항목이 모두 빠진다.
- 저장된 `source` 이름을 알 수 없으면 `Remote`로 읽는다. v0.1의 유일한 출처가 외부 콘텐츠다.
- M7의 세 기능을 한 인터페이스에 담았으므로 KM-112~115는 화면과 정책만 붙인다. 언제 기록할지, 즐겨찾기 버튼을 어디 두는지 같은 판단은 이 경계가 아니라 부르는 쪽이 한다.

### ADR-056 — Room 스키마와 명시적 마이그레이션 (KM-110)

- 사용자와 정한 세 가지로 진행했다. `SearchHistoryEntity`는 넣지 않고, 자리표시 표 `schema_baseline`은 지우고, 마이그레이션은 명시적으로 쓴다.
- `SearchHistoryEntity`를 넣지 않는다. KM-074에서 최근 검색어를 DataStore에 두기로 정했다(ADR-046). 같은 값을 두 곳에 두면 어느 쪽이 진짜인지 흐려진다. TASKS의 KM-110 엔티티 목록에서 이 항목은 빠진다. PRD 29의 `search_history` 표도 만들지 않는다.
- 자리표시 표를 지운다. 쓰인 적이 없고 남겨 두면 Room이 기대하는 스키마와 어긋난다. 마이그레이션에서 `DROP TABLE`한다.
- **명시적 마이그레이션을 쓴다.** 지우고 다시 만드는 방식은 기기에 있는 데이터를 말없이 없앤다. 이 앱은 사이드로드로 쓰이므로 기기의 데이터가 사용자의 유일한 사본이다. 설치 기반이 한 명이라는 것은 데이터를 지워도 된다는 뜻이 아니다.
- 마이그레이션 SQL은 손으로 쓰지 않고 Room이 내보낸 스키마(2.json)의 `createSql`을 그대로 옮겼다. 손으로 쓰면 Room이 기대하는 정의와 한 글자만 달라도 실행 중 검증이 깨진다. 표를 바꿀 때도 같은 방식으로 새 스키마 파일에서 옮긴다.
- 검사 전용으로 `androidx.room:room-testing`을 추가했다. `MigrationTestHelper.runMigrationsAndValidate`가 마이그레이션 결과를 내보낸 스키마와 견주므로, 옮겨 적은 SQL을 믿을 근거가 된다. Room과 같은 버전 참조를 쓰고 앱 산출물에는 들어가지 않는다.
- 표 다섯 개를 만든다. `tracks`가 곡 메타데이터를 들고 나머지가 그것을 가리킨다. `favorites`는 곡마다 한 행이라 곡 ID가 기본 키다. `playlist_items`와 `playback_history`는 같은 곡이 여러 번 올 수 있어 별도 키와 자리·시각을 둔다. 자리 번호를 키로 삼으면 순서를 바꿀 때마다 키가 흔들린다.
- 외래 키는 모두 곡·재생목록 삭제 시 함께 지운다. 가리키는 곡이 없는 즐겨찾기나 재생목록 항목은 화면에 그릴 수 없다. Room이 요구하는 자식 열 인덱스도 함께 만든다.
- **재생 주소는 어떤 표에도 없다**(AGENTS.md 8). `artwork_url`만 예외이며 그것은 앨범 이미지 주소다. 이 성질을 계측 검사로 고정했다. 열 이름에 `url`이 들어가면 `artwork_url`이어야 한다.
- DAO는 넣지 않았다. 이 작업은 표와 마이그레이션까지이며 DAO와 저장소는 KM-111이다.
- `SourceType`은 이름 문자열로 저장한다. 상수 이름을 바꾸면 이전 행을 읽지 못한다는 것을 주석에 남겼다.

### ADR-055 — 대기열 화면과 대기열 생성 경로 (KM-097)

- 사용자 요청으로 대기열을 만드는 경로까지 포함했다. `playQueue(tracks, startIndex)`가 검색 결과 목록 전체를 대기열에 넣고 고른 자리부터 재생한다. 검색 결과 항목은 이제 고른 곡 하나가 아니라 목록과 그 안의 자리를 넘긴다. 한 곡만 갈아 끼우면 이전·다음과 대기열 화면이 쓸 것이 없다.
- `playQueue`의 인자를 `Triple` 목록에서 `List<Track>`으로 바꿨다. 화면이 이미 `Track`을 들고 있고 앨범 이미지까지 넘겨야 한다. `core/player`는 `StreamResolver`에서 이미 `core/model`을 쓰므로 새 의존 방향이 생기지 않는다. `playTrack`은 계측 검사가 내장 음원 ID로 직접 부르므로 원시 인자를 유지한다.
- 대기열을 `PlaybackState.queue`와 `queueIndex`로 노출한다. 목록은 Timeline의 창 순서, 즉 넣은 순서다. 대기열 밖을 가리키는 자리는 -1로 정리한다. Media3는 대기열이 비어도 현재 자리를 0으로 돌려주므로 그대로 쓰면 화면이 없는 항목을 가리킨다.
- 대기열은 바뀔 때만 다시 읽는다. 위치 갱신이 250ms마다 오는데 매번 목록을 만들면 낭비다. `onTimelineChanged`로 표시를 세우고 연결 직후에도 한 번 읽는다(붙는 순간의 대기열은 알림 없이 이미 있다).
- **셔플이 켜졌을 때의 실제 재생 순서는 보여주지 않는다.** ADR-053의 제약이다. 화면은 늘 넣은 순서를 보여주고 셔플이 켜져 있으면 "실제 재생 순서는 이 순서와 다릅니다"라고 알린다. 순서를 감추는 것보다 낫다. 감추면 빼기·옮기기를 할 수 없다.
- 끌어서 옮기기는 넣지 않았다. Compose에 끌어 정렬하는 기본 목록이 없어 별도 의존성이나 직접 구현이 필요하고 인수 조건의 reorder는 "가능하면"이다. 위·아래 버튼으로 같은 일을 하며 접근성 설명도 붙는다.
- 현재 곡은 배경색과 "재생 중" 문구로 구분한다. 글자만 굵게 하면 목록에서 눈에 잘 띄지 않는다.
- 대기열 밖을 가리키는 빼기·옮기기 요청은 `PlayerConnection`에서 막는다. 화면이 계산한 자리와 실제 대기열이 한 박자 어긋날 수 있다.
- ShuffleTest의 셔플 켠 상태 확인을 순회에서 대기열 목록으로 바꿨다. 컨트롤러 순회는 셔플이 켜지면 결정적이지 않다. 컨트롤러가 스스로 답할 때는 넣은 순서가, 세션이 진짜 상태를 밀어 넣은 뒤에는 섞인 순서가 나온다. 두 결과를 모두 관찰했다. 대기열 목록은 Timeline 창 순서라 셔플과 무관하게 결정적이므로 그것으로 본다.

### ADR-054 — 반복 모드와 저장 경로 (KM-096)

- 반복은 하나의 칩으로 없음 → 전체 → 한 곡 → 없음을 돈다. 여러 곡을 이어 듣는 쪽이 한 곡 반복보다 흔해 전체가 먼저 온다. 지금 무엇인지는 칩의 글자가 말한다. 순환 규칙은 `RepeatMode.next()`로 떼어 단위 검사로 고정했다.
- 사용자 요청으로 저장까지 넣었다. PRD 34의 DataStore 항목에 반복 모드가 들어 있고 설정 저장소가 이미 있어 추가 비용이 거의 없다. 인수 조건에는 optional이었다.
- **저장된 설정이 곧 적용되는 값이다.** 화면은 `SettingsRepository.setRepeatMode`만 부르고 플레이어에 직접 지시하지 않는다. 적용은 재생을 소유한 `MusicService`가 설정 흐름을 구독해 `player.repeatMode`에 옮긴다. 경로가 하나뿐이라 저장값과 실제 재생이 어긋날 수 없고, UI 없이 세션만 살아난 경우에도 저장된 모드로 시작한다.
- 화면의 표시는 저장값이 아니라 `PlaybackState.repeatMode`(플레이어가 돌려준 값)를 쓴다. 설정을 저장값으로 그리면 적용이 실패했을 때 화면이 거짓을 보인다. 저장 → 서비스 → 플레이어 → 상태의 한 방향 흐름이다.
- 셔플과 다른 선택이다. 셔플은 세션 명령으로 바로 넣고 저장하지 않는다(ADR-053). 반복만 저장하는 것은 PRD가 그렇게 정했기 때문이며, 저장하는 값은 저장소를 유일한 출처로 두는 편이 낫다.
- `RepeatMode`를 공개 타입으로 바꿨다. 설정 저장소 계약에 들어가기 때문이다. 저장된 값은 상수 이름으로 남으므로 이름을 바꾸면 이전 설정을 읽지 못한다. 알 수 없는 값은 없음으로 본다.
- `MusicService`에 서비스 수명의 코루틴 범위를 뒀다. `onDestroy`에서 끊는다. 서비스가 설정을 읽는 것은 이미 WiFi 전용 정책(KM-137)에서 하던 일이다.

### ADR-053 — 셔플과 섞인 순서의 관찰 한계 (KM-095)

- 셔플 토글을 `PlayerConnection.setShuffleEnabled`로 붙였다. `COMMAND_SET_SHUFFLE_MODE`가 있을 때만 부르고, 화면의 켜짐 표시는 앱이 기억한 값이 아니라 세션이 돌려준 `PlaybackState.shuffleEnabled`만 근거로 한다. 눌렀다는 사실을 따로 들고 있으면 세션이 거절했을 때 화면이 거짓을 보인다.
- 표시는 선택 상태를 가진 `FilterChip`이다. 켜짐이 채워진 칩, 꺼짐이 테두리 칩으로 눈에 보인다. `material-icons-core`에 셔플 아이콘이 없어(KM-094에서 확인) 글자 칩을 쓴다.
- **섞인 재생 순서는 UI 계층에서 관찰할 수 없다.** 세션이 컨트롤러에 보내는 `Timeline`에는 셔플 순서가 실려 오지 않는다(`RemotableTimeline`이 선형 순서로 되돌아간다). 그래서 컨트롤러에 다음 곡을 물으면 늘 넣은 순서가 나온다. 실제로 대기열 네 곡을 열 번 다시 섞어 확인했더니 매번 넣은 순서였고, 반대로 세션이 진짜 상태를 밀어 넣는 순간에는 컨트롤러가 다음이 있다고 본 항목으로 넘어가지 못했다. 섞인 순서는 세션 뒤 Media3의 동작이다.
- 따라서 인수 조건의 queue behavior test는 관찰 가능한 범위까지만 확인한다. 토글이 세션까지 닿아 상태로 돌아오는 것, 셔플을 켜고 끄어도 대기열 내용과 컨트롤러가 보는 순서가 그대로인 것이다. 섞인 순서 자체는 계측으로 고정하지 않는다. 사용자와 합의한 playQueue 계측 검사의 실제 한계다.
- **KM-097(Queue UI)에도 같은 제약이 걸린다.** 컨트롤러만 보는 대기열 화면은 넣은 순서를 보여줄 수 있고 셔플이 켜졌을 때의 실제 재생 순서는 보여줄 수 없다. 그 화면을 만들 때 순서를 보여줄지, 셔플 중에는 순서 표시를 감출지 정해야 한다.
- 셔플 상태를 영구 저장하지 않는다. 세션이 살아 있는 동안만 유지된다. PRD 34의 DataStore 항목에는 반복 모드만 있고 셔플은 없다.

### ADR-052 — 이전·다음 (KM-094)

- 사용자와 합의해 대기열을 만드는 경로는 KM-097로 미루고 이번에는 버튼과 명령 연결만 다뤘다. 지금 `playTrack`은 `setMediaItem`으로 한 곡을 갈아 끼우므로 대기열에는 늘 한 곡뿐이다.
- 한 곡뿐이어도 이전은 뜻이 있다. Media3는 다음 곡이 없을 때 `seekToPrevious()`를 그 곡의 처음으로 되돌리는 동작으로 정의한다. 그래서 이전은 살리고 다음은 비활성으로 둔다.
- 가용성을 앱이 따로 계산하지 않고 `Player.isCommandAvailable`을 그대로 읽어 `PlaybackState.hasPrevious`·`hasNext`로 옮긴다. 화면·알림·잠금화면이 모두 같은 명령 가용성을 근거로 삼으므로 세 곳의 동작이 갈릴 수 없다. 이것이 인수 조건 consistent behavior를 만족시키는 방식이다.
- 알림·잠금화면 버튼은 새로 만들지 않았다. Media3의 알림 제공자가 같은 가용성으로 버튼을 구성하며, 곡이 하나일 때 다음 버튼을 아예 내지 않는다. 앱이 그 자리에 자체 버튼을 끼우면 두 규칙이 생긴다.
- `seekToPrevious`·`seekToNext` 모두 명령 가용성을 확인한 뒤에만 호출한다. 쓸 수 없는 명령을 던지면 Media3가 예외를 낸다.
- 아이콘 대신 "이전"·"다음" 문자열 버튼을 썼다. `material-icons-core`에는 SkipPrevious·SkipNext가 없고(확인함), 두 글리프 때문에 훨씬 큰 `material-icons-extended`를 넣지 않는다. 이 화면의 재생·일시정지도 이미 문자열 버튼이라 표기가 일관된다.
- 기본 상태의 `hasPrevious`·`hasNext`는 거짓이다. 연결 전에 쓸 수 있다고 잘못 보는 쪽이 더 나쁘다.

### ADR-051 — 탐색 표시 위치 (KM-093)

- 끌어서 탐색은 이미 동작했지만 손을 뗀 직후 표시가 탐색 이전 자리로 한 번 되돌아갔다가 목표로 뛰었다. 위치 보고가 250ms 간격이라 손가락 값을 지운 순간에는 아직 옛 위치만 있기 때문이다. 인수 조건 progress remains synchronized가 가리키는 지점이 이것이라 이번 작업의 실제 내용으로 삼았다.
- `PendingSeek(fromMs, toMs)`를 두고 표시 위치를 손가락 → 아직 도달하지 않은 목표 → 실제 위치 순으로 고른다. 이 규칙을 `seekDisplayPositionMs`로 떼어 화면 없이 검사할 수 있게 했다.
- 목표를 놓아주는 조건을 셋으로 정했다. 목표 근처(±1초)에 왔을 때, 목표를 지나 재생이 계속될 때, 그리고 목표에서 오히려 멀어졌을 때다. 마지막 조건이 없으면 탐색이 받아들여지지 않았을 때 표시가 목표에 붙어 멈춘 채 소리만 흐른다.
- 허용 오차는 1초다. 위치 보고 간격(250ms)보다 넉넉해 한 번의 보고로 정리된다.
- 목표는 도달하면 지운다. 남겨 두면 재생이 진행한 뒤 오차 범위를 벗어나 표시를 다시 가로챈다. 지우는 일은 `LaunchedEffect`에서 한다. 조립 중에 상태를 쓰지 않는다.
- 진행 표시의 시간 문구도 같은 값을 쓴다. 슬라이더와 숫자가 다른 것을 가리키면 안 된다.
- 좁히기 규칙은 `PlayerConnection.seekTo`에 그대로 둔다. 화면은 목표를 그대로 넘기고 범위 판단은 컨트롤러가 한다. 화면이 길이를 알고 있어도 실제 탐색 가능 여부는 플레이어만 안다.

### ADR-050 — Now Playing 화면 (KM-092)

- `feature/player/NowPlayingScreen`이 임시 `TestPlaybackScreen`을 대체하고 그 파일을 지웠다. KM-150이 붙인 `now-playing` 목적지의 실제 내용이다.
- 전체 화면에서는 하단 내비게이션을 감춘다. 앨범 이미지가 화면 폭 전체의 정사각형이라 자리가 필요하고, 전체 화면 플레이어 위에 탭이 남아 있으면 이 화면이 탭의 일부처럼 보인다. 대신 화면 안에 뒤로 버튼을 두어 시스템 뒤로 가기 말고도 나갈 길을 만들었다. KM-150에서 미뤄 둔 판단을 여기서 정했다.
- **즐겨찾기와 대기열 버튼은 자리만 두고 눌리지 않는다.** 사용자와 합의한 범위다. 즐겨찾기 저장은 Room 기반 KM-112, 대기열 화면은 KM-097 소속이며 둘 다 이 작업보다 뒤에 있다. 아무 반응 없는 버튼으로 두면 고장으로 보이므로 비활성으로 두고 "아직 준비되지 않은 기능입니다."를 함께 적었다.
- 이전·다음 버튼은 두지 않았다. KM-094가 UI·알림·잠금화면을 한 번에 다루는 작업이므로 여기서 UI만 먼저 만들면 그 작업이 반쪽으로 시작된다.
- 진행은 슬라이더와 현재 위치·전체 길이 표시다. 끌어서 탐색은 이미 동작하며 인수 조건으로 확인하는 것은 KM-093이다. 끌고 있는 동안에는 손가락 위치의 시간을 보여준다.
- 화면을 세로로 흘린다. 제목이 두 줄이거나 요금제 안내가 붙으면 세로가 모자랄 수 있는데, 잘리는 것보다 흐르는 것이 낫다.
- WiFi 전용 스위치는 이 화면에 남겼다. 설정 화면이 KM-153이라 지금 없애면 KM-137 기능에 닿을 길이 사라진다. KM-153에서 옮긴다.
- 연결 상태 문구를 남겼다. 세션에 붙지 못했을 때 화면이 왜 비어 있는지 알려주는 유일한 단서다.
- `destination_now_playing` 문자열을 지웠다. 이 화면은 이미지와 제목이 머리글 역할을 하므로 제목 줄이 필요하지 않고, 남겨 두면 쓰이지 않는 리소스가 된다.

### ADR-049 — 앱 내비게이션 (KM-150)

- KM-092(Now Playing)를 하려면 갈 수 있는 화면이 있어야 하므로 사용자 요청으로 M9의 KM-150을 앞당겼다. KM-072·091에서 미뤄 둔 화면 분리가 여기서 이루어진다.
- `androidx.navigation:navigation-compose` 2.10.0(안정판)을 추가했다. 하단 탭 세 개에 전체 화면 목적지가 붙고 앞으로 Queue UI·설정이 더해지므로 뒤로 가기 스택과 상태 저장을 직접 만들 이유가 없다. 손으로 만든 라우터는 결국 같은 것을 나쁘게 다시 구현한다.
- `androidx.compose.material:material-icons-core`를 추가했다. `NavigationBarItem`은 아이콘이 필수이며 필요한 Home·Search·List는 core 집합에 있다. extended는 쓰지 않는다. 이 산출물은 1.7.8에서 멈췄지만 현재 쓰는 Compose BOM이 관리하는 안정판이다. BOM에서 빠지면 벡터 드로어블을 직접 만든다.
- ViewModel은 Activity가 만들어 내려준다. 목적지마다 `hiltViewModel()`로 새로 만들면 탭을 옮길 때 검색·재생 상태가 끊기는데, KM-150의 인수 조건이 바로 재생 상태가 유지되는 것이다. 그래서 `hilt-navigation-compose`도 필요하지 않다.
- 탭 이동은 쌓지 않는다. 시작 목적지까지 `popUpTo`하고 `saveState`/`restoreState`로 탭별 상태를 남긴다. 뒤로 가기를 여러 번 눌러야 앱을 벗어나는 일이 없다.
- 시작 목적지는 검색이다. 홈(KM-151)과 라이브러리(KM-116)는 아직 내용이 없어 "준비되지 않은 화면" 자리표시자만 둔다. 세 탭은 KM-150의 사양이므로 자리표시자를 두는 것이 미래 기능 선구현은 아니다.
- 미니 플레이어는 하단 내비게이션 위에 두고 전체 화면 플레이어에서는 접는다. 같은 것을 위아래로 두 번 보여주지 않는다. 이로써 KM-091에서 미뤄 둔 "눌러서 Now Playing"이 채워진다.
- 하단 내비게이션은 Now Playing 목적지에서도 남겨 뒀다. 지금 그 자리는 KM-092가 대신할 임시 화면이라 탭을 항상 닿게 두는 편이 낫다. 전체 화면에서 감출지는 KM-092에서 정한다.
- `PlaybackState.canPause`를 더했다. 내비게이션 셸과 전체 화면이 같은 계산(재생 요청 중이고 끝나지도 재생 불가도 아님)을 되풀이하지 않게 한다.
- `TestPlaybackScreen`에서 검색을 떼어 검색 탭으로 옮겼다. 이 화면은 KM-092가 실제 Now Playing으로 대체한다. WiFi 전용 스위치는 설정 화면(KM-153)이 생기면 옮긴다.

### ADR-048 — 미니 플레이어와 세션 앨범 이미지 (KM-091)

- 대기열 항목에 앨범 이미지 주소를 넣는다. `PlayerConnection.playTrack`이 `artworkUri`를 함께 받아 `MediaMetadata.setArtworkUri`로 세션에 넣고, 상태로 다시 읽어 화면에 준다. ADR-047에서 미뤄 둔 것을 사용자 결정에 따라 이번에 넣었다.
- 재생 주소가 아니라 이미지 주소이므로 AGENTS.md 8의 스트림 URL 저장 금지와 무관하다. 이 값도 영구 저장하지 않고 대기열 항목에만 있다.
- `https`로 시작하는 주소만 넣는다. 다른 스킴은 세션의 이미지 로더가 읽으려다 실패할 뿐이다.
- 세션에 넣으면 알림과 잠금화면이 같은 이미지를 쓴다. KM-037·038은 이미지가 없는 상태(자리표시자)에서 검증했으므로 원격 곡으로 다시 확인했다. 계측 검사 하나와 실기기 눈 확인으로 제목·아티스트·진행·이전/일시정지 버튼과 실제 이미지 표시, 잠금화면 일시정지·재생 왕복을 확인했다.
- `playQueue`는 이미지 주소를 받지 않는다. Gate 검증용 진입점이고 서명이 `Triple`이라 네 번째 값을 넣으려면 타입을 바꿔야 한다. 제품 경로는 `playTrack`이므로 필요해질 때 함께 정리한다.
- 내장 테스트 음원은 `artworkData`(그려 넣은 PNG)를 쓰고 `artworkUri`가 없다. 따라서 미니 플레이어에서는 자리표시자 색으로 보인다. 알림은 기존대로 그 데이터를 쓴다. 테스트 음원 표시를 위해 별도 리소스 주소를 만들지 않았다.
- 미니 플레이어가 재생·일시정지를 들고 있으므로 화면의 독립 재생 버튼을 없앴다. 같은 버튼을 두 곳에 두지 않는다. 대기열이 비어 현재 곡이 없을 때와 연결이 끊겼을 때만 단독 버튼을 둔다.
- **눌러서 Now Playing으로 가는 동작은 넣지 않았다.** 화면 전환이 필요하고 내비게이션은 KM-150, 목적 화면은 KM-092다. 아무 일도 하지 않는 탭 영역을 미리 만들지 않는다. KM-072와 같이 백로그 순서를 지키기로 사용자와 합의했다.
- 이미지 로딩에는 WiFi 전용 설정(KM-137)을 적용하지 않는다. 그 설정은 재생 대역폭을 막는 것이고 섬네일은 재생이 아니다. 알림 이미지도 세션의 이미지 로더가 받으므로 같은 판단을 따른다.

### ADR-047 — 재생 상태 어댑터 확장 (KM-090)

- TASKS의 `PlayerUiState`를 새 타입으로 만들지 않고 이미 있는 `core/player/PlaybackState`를 확장했다. 이 타입이 곧 ARCHITECTURE 19가 요구하는 Media3 → UI 매퍼의 결과이며, 같은 것을 가리키는 상태 타입을 둘로 두면 화면이 어느 쪽을 봐야 하는지가 흐려진다(AGENTS.md 24). 이름은 ARCHITECTURE 4의 `PlaybackStateMapper.kt`와 맞추어 그대로 둔다.
- 현재 곡은 `NowPlaying(mediaId, title, artist)`로 담는다. `Track`을 그대로 쓰지 않는다. 세션에서 돌아오는 것은 대기열에 넣은 metadata뿐이라 `Track`의 `source`나 길이를 복원할 수 없고, 없는 값을 지어내면 화면이 사실이 아닌 것을 믿게 된다.
- 앨범 이미지는 아직 대기열 항목에 넣지 않는다. 넣으려면 `playTrack`이 이미지 주소를 함께 보내야 하고 그러면 알림 이미지 동작이 바뀌어 KM-037·038에서 검증한 것을 다시 확인해야 한다. 이미지가 실제로 필요해지는 Now Playing 화면(KM-091·092)에서 함께 다룬다.
- `playing`과 `buffering`은 별도 필드로 두지 않고 `phase`에서 읽는 계산 속성이다. 같은 사실을 두 곳에 두면 서로 어긋날 수 있다. 인수 조건의 이름은 속성 이름으로 드러난다.
- 반복은 `RepeatMode(Off/One/All)`로 좁힌다. 알 수 없는 상수가 오면 꺼짐으로 본다. 반복을 켠 것으로 잘못 보는 쪽이 사용자에게 더 나쁘다.
- 이번 작업은 상태를 읽는 것까지다. 반복·셔플을 바꾸는 조작은 KM-095·096 소속이라 설정 함수를 만들지 않았다. 그래서 실기기에서는 세션 기본값(꺼짐)까지만 확인할 수 있고, 켠 상태의 매핑은 일반 단위 검사로 고정했다.
- 매퍼의 새 인자는 기본값을 주어 뒤에 붙였다. 기존 호출과 검사가 그대로 컴파일되고, 새 값은 호출 지점에서 이름을 붙여 넘긴다.
- 현재 곡 조립을 `nowPlayingOf`로 따로 뺐다. Media3의 기본 `mediaId`가 빈 문자열이라 "곡 없음"과 구분되지 않는데, 이 판단을 매퍼 안에 두어야 화면 계층이 Media3의 기본값 규칙을 알 필요가 없다.

### ADR-046 — 최근 검색어 저장 위치 (KM-074)

- 최근 검색어를 Room이 아니라 이미 있는 설정 DataStore에 둔다. 저장할 것은 개수가 정해진 짧은 문자열 목록이고 조회·정렬·조합이 필요하지 않다. AGENTS.md 10의 "Room은 local library 상태를 저장한다"에 비추어도 검색어는 라이브러리 상태가 아니다.
- Room을 쓰면 지금 스키마를 1에서 2로 올려야 한다. `KeuneyDatabase`는 실제 엔티티 없이 자리표시자 하나만 두고 라이브러리 엔티티를 KM-110으로 미뤄 둔 상태이며, 검색어 하나 때문에 마이그레이션과 마이그레이션 검사를 앞당기는 것은 이 작업에 필요한 최소 변경이 아니다.
- **KM-110에 영향이 있다.** KM-110의 엔티티 목록에 `SearchHistoryEntity`가 들어 있으나 이 결정에 따라 필요하지 않다. KM-110 착수 시 그 항목을 빼거나, 검색어를 Room으로 옮기고 이 ADR을 대체할지 다시 판단한다. 재개 지점에 남겼다.
- Preferences에는 순서를 지키는 목록 타입이 없다. 문자열 집합을 쓰면 최신 순서를 잃으므로 JSON 배열 한 값으로 저장한다. 직렬화는 이미 있는 kotlinx-serialization을 쓰며 신규 의존성은 없다.
- 저장된 값이 깨져 있으면 목록이 없는 것으로 본다. 최근 검색어는 편의 기능이며 이 값 때문에 검색이 막히면 안 된다. 깨진 값 위에 새 검색어를 남길 수 있는 것도 검사로 고정했다.
- 개수 상한은 10이다. 같은 검색어를 다시 검색하면 새 항목이 생기지 않고 맨 앞으로 올라온다. 상한을 넘으면 가장 오래된 것이 빠진다.
- 오류 없이 끝난 검색만 남긴다. 결과가 없는 검색도 검색 자체는 성공한 것이므로 남긴다. 실패한 검색은 남기지 않는다.
- 저장 호출을 검색 작업과 분리했다. 사용자가 결과를 보고 곧바로 다음 검색을 시작하면 검색 작업이 취소되는데, 이미 성공한 검색은 그와 무관하게 남아야 한다.
- 목록은 결과가 화면에 없을 때(Idle)만 보여준다. KM-072의 제약대로 검색과 재생이 한 화면에 함께 있어 자리가 넉넉하지 않다. 검색어를 비우면 Idle로 돌아오므로 목록과 지우기 버튼에 언제든 닿을 수 있다.
- 저장소 구독은 `SharingStarted.Eagerly`다. 상류는 값이 바뀔 때만 흐르는 DataStore 한 키라 계속 구독해도 비용이 없고, 화면이 열리는 순간 이미 목록이 있어야 한다.

### ADR-045 — 검색 결과 목록과 이미지 로딩 (KM-073)

- 결과 항목을 `feature/search/SearchResultList`로 뺐다. 목록은 곡을 그리는 일만 하고 고른 항목을 `onSelect`로 넘긴다. 재생 여부는 목록이 판단하지 않는다.
- 앨범 이미지 표시에 `io.coil-kt.coil3:coil-network-okhttp`를 추가했다. Coil 3은 네트워크 fetcher를 본체에서 떼어 별도 산출물로 옮겼기 때문에 `coil-compose`만으로는 `https` 이미지가 조용히 실패한다. 기존 `ui/components/Artwork`(Coil)가 쓰이지 않고 있었고 `Track.artworkUrl`은 이미 검색 mapper가 채우고 있었으므로, 이번 인수 조건의 artwork 표시를 위해 필요한 최소 추가다.
- ktor 대신 okhttp 변형을 골랐다. OkHttp는 `ktor-client-okhttp`로 이미 classpath에 있어 실제로 늘어나는 것은 Coil의 얇은 fetcher뿐이다. ktor 변형을 쓰면 이미지 로딩이 앱의 `HttpClient` 구성과 얽히는데, 그 구성은 공급자 요청용 헤더와 대기 정책을 갖고 있어 이미지에 그대로 적용할 이유가 없다. Coil 버전과 같은 `coil` 버전 참조를 쓴다.
- 별도 `ImageLoader` 등록 코드는 넣지 않았다. 산출물만 추가하면 Coil이 fetcher를 스스로 찾는다. 실기기에서 실제 섬네일이 그려지는 것을 확인해 판단했다.
- 이미지는 장식으로 두고 `contentDescription`을 비웠다. 제목이 바로 옆에 있어 읽어 주면 같은 내용이 두 번 나온다. 접근성 전반 점검은 KM-154다.
- 제목은 두 줄까지, 부제는 한 줄까지 그리고 넘치면 줄인다. 검색 결과 제목은 아주 길 수 있어 제한이 없으면 한 항목이 화면을 다 차지한다.
- 부제 조립을 `trackSubtitle`로 떼어 단위 검사로 고정했다. 길이는 아는 경우에만 붙이고, 아티스트와 길이가 모두 없으면 빈 문자열이며 그때는 줄 자체를 그리지 않는다. 공급자가 둘 다 주지 않는 결과가 실제로 있다.
- 이미지 로딩에는 WiFi 전용 설정(KM-137)을 적용하지 않는다. 그 설정은 재생 대역폭을 막는 것이고 섬네일은 재생이 아니다. 필요해지면 별도 작업으로 다룬다.

### ADR-044 — 검색 화면 분리 (KM-072)

- 검색어 입력과 결과 표시를 `feature/search/SearchScreen`으로 옮겼다. `TestPlaybackScreen`에는 재생 제어만 남는다. 검색 화면은 `SearchViewModel`만 알고, 결과를 고른 뒤 무엇을 할지는 `onSelect` 콜백으로 밖에 맡긴다. 재생 의존성을 검색 화면에 들이지 않는다.
- 화면을 파일로만 나누고 배치는 그대로 뒀다. 진짜로 두 화면을 만들려면 화면 전환이 필요하고 내비게이션은 KM-150(M9) 범위다. 백로그 순서를 지키고 되돌리기 쉬운 쪽을 골랐다. 사용자와 합의한 선택이다.
- 검색어는 화면의 `rememberSaveable` 상태로 둔다. ViewModel로 올리면 회전·프로세스 복원 처리를 직접 짜야 하고, 입력 중 글자마다 상태 갱신이 검색 상태 흐름과 섞인다. 검색 결과와 달리 입력 중인 글자는 화면 밖에서 쓰이지 않는다.
- 검색어를 비우면 `SearchViewModel.clear()`를 불러 이전 결과도 치운다. 빈 입력창 아래 옛 결과가 남아 있으면 화면이 거짓말을 한다.
- 키보드에 `ImeAction.Search`를 붙여 입력 직후 바로 검색할 수 있게 했다. 검색 버튼과 같은 동작이며 버튼은 그대로 둔다.
- 다섯 상태를 모두 그린다. Loading은 진행 표시와 문구, Empty와 Error는 문구로 알린다. Error는 오류 색을 쓰고 `AppError`를 문자열 리소스로만 바꿔 보여준다. 원문 예외나 응답 내용은 화면까지 오지 않는다(AGENTS.md 12).
- `분:초` 표기를 `ui/format/formatDuration`으로 옮겼다. 이전에는 재생 위치와 곡 길이가 같은 규칙을 각각 계산했다. 한곳에 두고 단위 검사로 고정했다.
- 결과 목록은 이번에 옮기기만 했다. 앨범 이미지를 포함한 항목 구성은 KM-073 범위이므로 여기서 만들지 않는다.

### ADR-043 — SearchViewModel 분리 (KM-071)

- 검색을 `PlayerViewModel`에서 떼어 `feature/search/SearchViewModel`로 옮겼다. 상태 이름을 TASKS의 정의에 맞춰 Idle/Loading/Success/Empty/Error로 바꿨다. 상태는 `StateFlow`로만 노출하고 화면은 읽기만 한다(AGENTS.md 11).
- 분리의 실익은 검사 가능성이다. `PlayerViewModel`은 `PlayerConnection`을 통해 Android `Handler`/`Looper`에 묶여 있어 ViewModel 검사를 전부 계측으로 돌려야 했다. 검색만 떼면 재생 의존성이 사라져 일반 단위 검사로 확인할 수 있다. 실행이 빠르고 기기 없이도 돈다.
- 이를 위해 `kotlinx-coroutines-test`를 테스트 전용 의존성으로 추가했다. `viewModelScope`가 `Dispatchers.Main`을 쓰므로 검사에서 이를 대체해야 한다. coroutines와 같은 버전을 쓰며 앱 산출물에는 들어가지 않는다.
- 새 검색은 이전 검색을 취소한다. 취소하지 않으면 늦게 도착한 이전 결과가 새 결과를 덮어쓴다. 이 동작을 단위 검사로 고정했다.
- 검색 상태 전이 검사를 계측에서 일반 단위로 옮겼다. 계측에는 실제 검색 → 선택 → 재생 → Home 유지의 end-to-end 하나만 남긴다. 같은 것을 두 곳에서 검사하지 않는다.
- 화면은 아직 POC 하나이며 두 ViewModel을 함께 받는다. 검색 화면 분리와 결과 목록 추출은 KM-072·KM-073 범위다.

### ADR-042 — SearchRepository 경계 (KM-070)

- ARCHITECTURE 6의 `SearchRepository` 계약을 그대로 쓴다. 인터페이스는 `core/search`, 구현은 `data/repository`에 둔다(ARCHITECTURE 4의 트리).
- 이 경계의 존재 이유는 실패 표현을 바꾸는 데 있다. 이전에는 ViewModel이 `data.source.toAppError`를 직접 불러 화면 계층이 데이터 계층 함수에 의존했다. 이제 repository가 모든 실패를 `AppErrorException`으로 감싸 돌려주므로 화면은 `AppError`만 안다.
- `Result<List<Track>>` 서명은 ARCHITECTURE와 동일하게 유지한다. `Result`의 실패는 Throwable이어야 하므로 도메인 오류를 실어 나를 얇은 예외 타입 `AppErrorException`을 `core/model`에 둔다. 원문 예외와 메시지는 담지 않는다.
- repository는 얇게 둔다. 검색어 정리와 빈 검색어 처리는 이미 공급자 구현에 있고, 옮기면 같은 규칙이 두 곳에 생긴다. 지금 repository의 책임은 위임과 오류 변환뿐이다. 로컬/원격 조합과 캐시 전략은 필요해질 때 여기에 붙인다.
- 취소는 실패로 바꾸지 않고 그대로 전파한다. 취소를 오류로 만들면 화면이 실패 문구를 띄우고 불필요한 재시도를 유도한다(ADR-040과 같은 이유).
- POC 화면의 ViewModel을 이 경계 뒤로 옮겼다. 별도의 SearchViewModel과 화면 분리는 KM-071·KM-072 범위다.

### ADR-041 — 소스 계약 검사 스위트 (KM-063)

- 계약 검사는 별도 Gradle 작업 `sourceContractTest`로 실행하고 일반 `test`에서는 `*SourceContractTest`를 제외한다. 실제 외부 요청이므로 매 실행 네트워크를 확인하며 결과를 캐시하지 않는다(AGENTS.md 17).
- 스위트의 목적은 "앱보다 검사가 먼저 깨지는 것"이다. 검색 계약, 스트림 계약, 오류 계약, Gate 계약 네 종류로 나눠 각각이 지키는 것을 명시한다.
- 검색 계약을 필드 계약까지 확장했다. 이전에는 ID와 제목만 확인해 아티스트·길이·이미지가 응답에서 사라져도 통과했다. mapper가 이 값들을 nullable로 다루기 때문에 화면 품질만 조용히 떨어진다. 곡마다 없을 수 있으므로 결과의 절반 이상에 존재하는지로 판단한다.
- 스트림 계약의 구간 요청을 앞부분에서 파일 중간 지점으로 바꿨다. 앞부분만 받아도 통과하던 구멍이 실제로 결함을 놓친 적이 있다(ADR-034). Gate 계약과 같은 기준을 쓴다.
- 오류 계약을 새로 넣었다. 단위 검사는 우리가 만든 응답으로만 분류를 검증하므로, 공급자가 실패를 알리는 방식을 바꾸면 단위는 통과하면서 사용자에게 엉뚱한 문구가 보인다. 없는 트랙이 재생 불가로 분류되는지, 결과 없음이 실패로 바뀌지 않는지를 실제 응답으로 확인한다.
- Gate 계약은 일회성 판정이 아니라 회귀 감지 장치로 스위트에 남긴다. 실행에 약 25초가 걸리지만 채택 조건이 무너지는 것을 가장 먼저 잡는다(ADR-037).
- 실패 원인을 요약 출력에 남긴다. 공급자 거부(4xx)와 전송 지연(타임아웃)을 구분해 기록하며 URL과 응답 원문은 출력하지 않는다.

### ADR-040 — 네트워크 대기와 취소 정책 (KM-062)

- 대기 상한을 `NetworkTimeouts` 한곳에 모으고 요청 성격에 따라 값을 나눈다. 메타데이터 요청(검색, 주소 해석)은 사용자가 화면에서 기다리므로 짧게, 재생 요청은 큰 파일을 이어 받으므로 조금 더 길게 잡는다.
- 메타데이터: 연결 10초, 바이트 대기 20초, 요청 전체 30초. 재생: 연결 10초, 바이트 대기 20초.
- 재생 쪽 바이트 대기를 media3 기본 8초에서 20초로 늘렸다. KM-059·KM-060 검증에서 공급자가 전송을 늦게 시작해 SocketTimeoutException이 나는 것을 실제로 관찰했다. 기본값이면 그런 곡은 곧바로 끊긴다.
- 값을 무한정 늘리지 않는다. 스로틀링으로 전송이 사실상 멈춘 경우까지 기다리면 사용자는 멈춘 화면을 보게 된다. 상한을 넘긴 실패는 KM-061의 재해석·1회 재시도로 이어지고, 그래도 실패하면 Network 오류로 표시된다(ADR-038, ADR-039).
- 취소는 상한과 무관하게 즉시 존중한다. 공급자 구현은 `CancellationException`을 실패 값으로 삼키지 않고 그대로 전파한다. 취소를 오류로 바꾸면 화면이 "실패"를 보여주고 불필요한 재시도를 유도한다.
- 검사를 위해 `createMusicHttpClient`가 대기 값을 인자로 받게 했다. 기본값은 정책 그대로이며 검사에서만 짧은 값을 넣어 실제 동작을 빠르게 확인한다. 상한을 실제로 기다리는 느린 검사를 만들지 않는다.

### ADR-039 — 재생 중 스트림 재해석과 1회 재시도 (KM-061)

- 재생 중 스트림이 만료되거나 거부되면 `RefreshingDataSource`가 상위 소스를 새로 열어 한 번만 다시 시도한다. 상위 소스를 새로 여는 것이 곧 재해석이다. `ResolvingDataSource`가 열 때마다 주소를 해석하고 앱은 해석 결과를 보관하지 않기 때문이다.
- 읽던 위치부터 이어 연다. `DataSpec.subrange(읽은 바이트)`로 재요청하므로 처음부터 다시 받지 않는다. 이미 받은 구간의 데이터를 버리지 않는다.
- 재시도는 열기 한 번당 1회다. 이어 연 뒤의 실패는 그대로 올려 보내 종점 오류가 된다. 무한 재시도를 만들지 않는다(PRD 22).
- 배치는 캐시 안쪽, 해석 바깥쪽이다. 캐시 적중은 재시도 경로를 타지 않고, 재시도는 항상 새 주소로 이뤄진다.
- 열기 실패와 읽기 도중 실패를 모두 다룬다. ExoPlayer의 기본 정책은 403 같은 응답을 재시도 대상으로 보지 않아 그대로 두면 종점 오류가 된다.
- `PlayableStream.expiresAt`은 여전히 읽는 곳이 없다. 주소를 열 때마다 새로 해석하고 만료는 실패로 드러나 재시도로 처리되므로, 시각을 미리 비교하는 경로가 필요하지 않다. 값은 PRD의 모델 정의를 따라 유지한다.
- 실패 종류별 사용자 문구는 KM-060의 `AppError` 매핑을 그대로 쓴다. 타임아웃 값 자체의 정책은 KM-062에서 다룬다.

### ADR-038 — 소스 오류 매핑 (KM-060)

- 공급자와 인프라의 실패를 `SourceFailure` 다섯 분류(Network, Parse, NotFound, Restricted, Unknown)로 먼저 좁힌 뒤 도메인 `AppError`로 바꾼다. 원문 예외와 메시지는 이 경계에서 끊기며 UI로 넘어가지 않는다(AGENTS.md 12).
- 분류 기준: 연결 실패·타임아웃·5xx·429·408은 Network, 응답 구조 변경과 직렬화 실패는 Parse, 404·410과 재생 불가 상태는 NotFound, 401·403과 로그인/연령/콘텐츠 확인 요구는 Restricted, 나머지는 Unknown이다. 공급자의 재생 가능 상태는 상태 문자열만 보고 판단하며 응답 원문을 읽지 않는다.
- `AppError` 매핑에서 **Restricted를 GeoRestricted로 보내지 않는다.** PRD의 GeoRestricted는 지역 제한을 뜻하는데 로그인·연령 제한을 그 이름으로 표시하면 사용자에게 틀린 이유를 보여준다. Restricted와 NotFound 모두 PlaybackUnavailable("이 곡은 재생할 수 없습니다")로 보낸다.
- 그 결과 `AppError.GeoRestricted`는 현재 생성되는 곳이 없다. 공급자가 지역 제한을 구조적으로 알려주지 않기 때문이다. 응답 문구를 문자열로 판별하는 방식은 쓰지 않는다. 지역 제한을 따로 표시해야 한다면 판별 가능한 신호를 먼저 찾거나 PRD의 오류 분류를 바꿔야 한다.
- 재생 가능한 전송 방식이 없는 응답은 Parse로 분류해 SourceUnavailable이 된다. 상태는 정상인데 앱이 쓸 수 있는 형식이 없는 것은 소스가 방식을 바꾼 경우다.
- 화면은 다섯 분류별 문구를 보여준다. 검색 실패 상태가 `AppError`를 함께 들고 다니도록 바꿨다.
- 검증 중 KM-059 Gate 검사가 간헐적으로 실패했다. 원인은 공급자의 거부가 아니라 특정 트랙의 SocketTimeoutException이었다. 계약 위반과 전송 지연을 구분하도록 판정을 나눴다. 공급자 거부는 한 건도 허용하지 않고, 재시도 후에도 남는 전송 지연만 1건까지 허용하며 결과에 건수를 그대로 기록한다. 타임아웃 값 자체의 정책은 KM-062에서 다룬다.

### ADR-037 — Provider A를 v0.1 source로 채택 (KM-059 Gate PASS)

- KM-059 Gate를 통과했다. 여러 아티스트 10곡 전부 스트림 해석과 파일 중간 지점 구간 요청에 성공했고, 실기기에서 큐 자동 전환과 81분 트랙의 먼 지점 이어 재생을 확인했다. 판정 근거와 곡별 결과는 `docs/SOURCE_PROVIDER.md`에 기록했다. ARCHITECTURE의 ADR-004(Source Provider 선택) 미확정 상태를 이 결정으로 닫는다.
- 채택하되 다음을 v0.1의 알려진 제약으로 명시한다. 오디오 전용 스트림을 쓸 수 없어 영상이 포함된 progressive 형식만 재생하며(ADR-034), 대역폭이 오디오 전용의 약 3배다. 완화 수단은 KM-134 캐시와 KM-137 WiFi 전용 재생이다.
- 공급자 설정은 비공개 프로토콜의 관찰값이며 안정된 API 계약이 아니다. `sourceContractTest`가 검색·해석·구간 요청을 실제로 확인하고 클라이언트 종류별 결과를 남겨 깨짐을 먼저 드러낸다. 이 검사를 유지하는 것이 채택의 전제다.
- PO token 등 공급자의 접근 제한을 우회하는 수단은 도입하지 않는다. 이 방침 때문에 오디오 전용 제약이 유지된다.
- KM-064 Provider B 평가는 활성화하지 않는다. `docs/SOURCE_PROVIDER.md`의 재판정 기준에 해당하는 상황이 생기면 그때 활성화한다.
- Gate 검증을 위해 `PlayerConnection.playQueue`와 `currentMediaId`를 추가했다. 큐 UI와 이전/다음 조작은 KM-094·KM-097 범위이며 여기서는 대기열 구성과 자동 전환만 다룬다.
- 첫 실행에서 1곡의 구간 요청이 전송 예외로 실패했으나 재실행에서 통과했다. 응답 코드를 그대로 기록하도록 검사를 고쳐 일시적 오류와 구조적 거부를 구분한다.

### ADR-036 — WiFi 전용 재생 (KM-137)

- progressive 전환으로 곡당 데이터가 8~25MB가 되어 사용자가 데이터 사용을 통제할 수 있어야 한다(ADR-034). 설정 하나를 DataStore에 저장하며 기본값은 꺼짐이다. 기존 동작을 바꾸지 않는다.
- 판단은 `NetworkPolicy`가 한다. 설정이 켜져 있고 `ConnectivityManager.isActiveNetworkMetered`가 true일 때만 막는다. 연결 정보를 얻지 못하면 막지 않는다. 알 수 없는 상태 때문에 재생을 멈추지 않는다.
- 차단 지점은 `TrackStreamResolver`다. 스트림 주소를 해석하기 전에 고정 메시지의 `MeteredNetworkBlockedException`을 던진다. 캐시는 해석보다 바깥에 있으므로 이미 받아 둔 구간은 이 판단을 거치지 않고 그대로 재생된다. 제한을 켜도 들었던 곡은 계속 들린다.
- 화면에는 스위치와, 제한이 실제로 걸리는 상태일 때의 안내 문구를 둔다. 원문 오류는 노출하지 않는다.
- `NetworkPolicy`는 연결 확인을 람다로 받아 실제 기기 상태와 무관하게 검사할 수 있다. 상속을 열지 않았다.
- 이 설정은 KM-153 Settings 화면이 생기면 그쪽으로 옮긴다. 지금은 POC 화면에 둔다. 신규 의존성은 없다.

### ADR-035 — 재생 스트리밍 캐시 (KM-134)

- Media3 `SimpleCache`와 `LeastRecentlyUsedCacheEvictor`를 쓴다. 상한 256MB, 위치는 `cacheDir/media`다. 운영체제가 정리할 수 있는 영역이며 상한을 넘으면 오래된 것부터 지운다. `DownloadManager`나 영구 저장은 쓰지 않는다(AGENTS.md 15).
- 캐시를 재생 경로 가장 바깥(해석보다 위)에 둔다. 캐시 키가 `keuney://track/<id>` 자리표시 URI가 되어 매번 달라지는 스트림 주소와 무관하게 재사용된다. 캐시에 있으면 주소 해석 요청 자체를 보내지 않는다.
- 저장 확정 조각을 기본 5MB에서 1MB로 줄였다. 기본값에서는 곡을 짧게 듣고 멈추면 받은 구간이 하나도 남지 않는다. 조각이 작을수록 부분 청취도 남는다.
- `SimpleCache`는 한 디렉터리를 프로세스에서 하나만 열 수 있다. 주입 그래프가 다시 만들어져도 같은 인스턴스를 쓰도록 프로세스 단위로 보관한다. 같은 이유로 설정 DataStore도 프로세스 단위로 보관하도록 바꿨다.
- 캐시 크기 조절과 비우기 UI는 KM-153 Settings 범위다. 지금은 `PlaybackCache.clear()`만 제공한다.
- 신규 의존성은 `androidx.media3:media3-database` 하나이며 `StandaloneDatabaseProvider`에 필요하다. media3와 같은 버전을 쓴다.

### ADR-034 — progressive 형식만 재생 스트림으로 인정 (KM-058)

- KM-058 수동 검증에서 재생이 약 34초 지점(첫 512KB 직후)에 403으로 멈췄다. KM-057의 실기기 검증이 10초 미만이라 첫 청크 안에서 끝나 이 결함을 놓쳤다.
- 실기기 진단 결과, 오디오 전용 adaptive 주소는 **0에서 시작하지 않는 모든 요청을 거부**한다. 헤더 `Range: bytes=524288-1048575` 403, 쿼리 `range=524288-1048575` 403, 둘을 섞어도 403, 다음 청크도 403이다. 즉 첫 512KB만 받을 수 있어 이어 재생이 불가능하다. 이후 재확인에서는 오프셋 0 요청도 403이 되어 이 경로 자체를 신뢰할 수 없다.
- 같은 응답의 progressive 형식(`streamingData.formats`, video/mp4 306kbps 다중화)은 임의 구간 요청을 모두 허용한다. 닫힌 Range, 열린 Range, Range 없음, 4MB 구간이 모두 200/206이다.
- 따라서 mapper는 **progressive 형식만 재생 가능한 스트림으로 인정**한다. 재생할 수 없는 전송 방식을 성공 값으로 포장하지 않는다는 ADR-031의 원칙을 그대로 적용한다. progressive가 없으면 실패를 반환해 다음 클라이언트 후보로 넘어간다.
- 후보 순서를 ANDROID 우선으로 바꿨다. ANDROID만 직접 URL이 있는 progressive 형식을 제공하며 IOS는 오디오 전용 adaptive만 준다.
- 대가: progressive는 영상이 함께 들어 있는 다중화 스트림이라 오디오 전용(약 50~60kbps)보다 대역폭을 5~6배 쓴다. 음악만 필요하므로 ExoPlayer의 track selection에서 영상 트랙을 끈다. 데이터 사용량이 중요한 환경에서는 재검토가 필요하며 KM-059 Gate의 판단 요소다.
- ADR-033의 `ChunkedHttpDataSource`는 제거했다. progressive 주소가 열린 Range와 큰 구간 요청을 그대로 받으므로 청크 분할이 필요 없고, 청크로 나눠도 adaptive 주소의 403은 해결되지 않는다. 쓰이지 않는 복잡도를 남기지 않는다.
- 회귀 방지: 실기기 계측에 먼 지점(길이 - 60초)으로 탐색해 이어 재생하는 검사를 추가했다. 파일 앞부분만 받아도 통과하던 구멍을 막는다. 수동으로도 95초 연속 재생과 Home 이후 130초까지 유지를 확인했다.
- PO token 등 공급자의 접근 제한을 우회하는 수단은 도입하지 않았다. 공개 응답이 그대로 제공하는 형식만 사용한다.
- 추가 확인 (2026-09-02, 오디오 전용 재검토): 일시적 제한인지 확인하기 위해 실제 음악 트랙 3곡 × ANDROID·IOS 클라이언트로 요청 간격을 두고 다시 측정했다. 결과는 6건 모두 동일하게 앞 512KB 206, 1MB 이후 구간 403이었다. 일시적 rate limit이 아니라 체계적인 제한이며 오디오 전용 주소로는 첫 512KB까지만 받을 수 있다. 가장 낮은 32kbps 형식도 곡 전체가 512KB를 넘어 대안이 되지 않는다. ANDROID_VR은 LOGIN_REQUIRED, TVHTML5_SIMPLY_EMBEDDED_PLAYER는 ERROR를 반환한다.
- 대역폭 실측 (음악 4곡): progressive 302~662kbps, 오디오 전용 최고 136~175kbps. 곡당 progressive 8~25MB, 오디오 전용 3~5MB로 평균 약 3배(2.3~5.4배)다. progressive 비트레이트는 원본 영상 품질에 따라 달라지며 `formats`에는 항상 1개만 있어 더 낮은 선택지가 없다.
- 컨테이너 안에서 오디오와 영상 샘플이 촘촘히 뒤섞여 있어 progressive에서 영상 구간만 건너뛰고 받는 것은 현실적이지 않다. 재생 시 영상 트랙을 끄는 것으로 디코딩 비용만 줄인다.

### ADR-033 — MusicSource와 재생 연결 (KM-057, 청크 Range 부분은 ADR-034로 대체)

- `ProviderAMusicSource`가 검색과 스트림 해석을 MusicSource 계약 뒤에 묶고 Hilt `SourceModule`이 이를 바인딩한다. `getTrack`/`getRelated`는 구현한 작업이 없어 고정 메시지의 안전한 실패를 반환한다. `core/player/StreamResolver`는 MusicSource만 호출하는 얇은 seam이며 KM-061의 만료 재해석이 여기에 붙는다.
- MediaItem에는 실제 주소 대신 `keuney://track/<id>` 자리표시 URI만 넣는다. 컨트롤러가 보낸 MediaItem은 URI를 잃으므로 `MediaLibrarySession.Callback.onAddMediaItems`에서 mediaId로 자리표시 URI를 복원한다. Track ID는 `[A-Za-z0-9_-]{1,64}`만 허용해 요청 대상이 바뀌지 않게 한다. 대기열과 컨트롤러는 스트림 주소를 알지 못하며 어디에도 저장하지 않는다(AGENTS.md 8).
- `ResolvingDataSource`가 재생 직전 로딩 스레드에서 자리표시 URI를 실제 주소로 바꾼다. 해석 실패는 고정 메시지 IOException으로 바꿔 ExoPlayer 오류로 노출하며 URL·응답 원문을 남기지 않는다.
- `PlayableStream`에 `requestHeaders`를 추가하고 해석에 사용한 클라이언트의 User-Agent를 담아 재생 요청에 함께 보낸다. 일시적 값이며 저장하지 않는다.
- 실기기 검증에서 공급자 스트림이 **열린 Range 요청을 403으로 거부**했다. 같은 URL·같은 기기에서 닫힌 Range는 512KB까지 206, 1MB 이상은 403이었다. 같은 URL의 반복 요청은 문제없었다. ExoPlayer의 progressive 로딩은 길이 미지정으로 한 번에 열기 때문에 그대로는 재생할 수 없다.
- 따라서 `ChunkedHttpDataSource`가 하나의 재생 요청을 512KB 이하의 닫힌 Range 요청 여러 개로 나눠 순서대로 읽는다. 첫 청크 응답의 Content-Range에서 전체 길이를 얻어 상위 계층에 알린다. `DefaultDataSource`의 base 소스로만 끼워 http(s)에만 적용하며 내장 테스트 음원 같은 로컬 스킴은 기존 경로를 그대로 쓴다. 청크 상한은 관찰값이며 공급자가 바꾸면 조정해야 한다.
- 네트워크 재생을 위해 wake mode를 `WAKE_MODE_LOCAL`에서 `WAKE_MODE_NETWORK`로 바꿨다. 기존 WAKE_LOCK 권한을 그대로 사용하며 ADR-025의 화면 꺼짐 재생 결론은 유지된다.
- media3의 데이터 소스 API는 UnstableApi다. Kotlin `@file:OptIn`은 Android lint의 UnsafeOptInUsageError를 만족시키지 못해 `@androidx.annotation.OptIn(markerClass = [UnstableApi::class])`을 해당 클래스에 붙였다. lint 오류를 억제하거나 baseline을 만들지 않았다. 신규 의존성은 없다.
- 화면의 원격 재생 버튼과 `PlayerViewModel`의 고정 Track ID는 KM-057 확인용이며 KM-058의 검색 결과 선택으로 대체한다.
- 검증 한계: 단일 트랙·단일 기기·WiFi 조건이다. 여러 곡 연속 재생, 장시간 재생 중 URL 만료, 탐색 반복, 네트워크 전환은 KM-058·KM-061·KM-132·KM-136에서 확인한다.

### ADR-032 — 재생 요청의 클라이언트 프로필 분리 (KM-056)

- WEB 클라이언트는 player 응답의 audio 형식에 직접 URL과 signatureCipher를 더 이상 포함하지 않고 serverAbrStreamingUrl만 반환한다. 재생 요청에 사용할 클라이언트 설정을 `ProviderAClientProfile`로 분리하고 후보를 순서대로 시도해 직접 URL을 제공하는 종류를 사용한다. 검색 경로는 KM-055에서 통과한 WEB 설정을 그대로 유지한다.
- 후보 순서는 IOS → ANDROID → ANDROID_VR → TVHTML5_SIMPLY_EMBEDDED_PLAYER → WEB이다. 실제 계약 검사에서 IOS와 ANDROID가 직접 URL을 반환했고 나머지는 실패했으므로 성공한 종류를 앞에 둔다. 실패한 종류도 대체 경로로 남기며 첫 성공에서 즉시 중단하므로 정상 경로의 추가 요청은 없다.
- `sendsSignatureTimestamp`가 true인 종류에만 playbackContext의 signatureTimestamp를 보낸다. 직접 URL을 주는 종류는 서명 해석이 필요 없어 플레이어 버전을 보내지 않는다. 선택 필드는 기본값 null이며 기본 Json 설정의 encodeDefaults=false로 요청 본문에서 생략된다.
- 모든 후보가 실패하면 마지막 실패를 그대로 반환한다. 예외 메시지는 고정 문자열이며 응답 원문·URL을 포함하지 않는다. 취소는 계속 전파한다. 로그인·쿠키·토큰·PO token은 사용하지 않고 신규 의존성도 없다.
- 실제 계약 검사는 후보 전체를 시도한 뒤 클라이언트별 결과를 요약 출력하고, 해석된 스트림에 Range 요청을 보내 206 응답까지 확인한다. 출력에는 URL과 응답 원문을 남기지 않는다. AGENTS.md 17의 provider 변경 조기 감지 목적에 맞춰 어떤 종류가 깨졌는지 한 번에 드러난다.
- clientVersion·signatureTimestamp와 마찬가지로 클라이언트 종류별 설정은 공개 클라이언트의 관찰값이며 안정된 API 계약이 아니다. 공급자가 이 경로도 SABR로 전환하면 다시 실패하며, 그 경우 KM-059 Gate 판정과 KM-064 Provider B 평가로 처리한다.
- 검증은 JVM 계약 검사와 Range 요청까지이며 Android 기기의 실제 재생은 KM-057·KM-058에서 확인한다. 이 결정은 ADR-031의 "직접 URL 없는 전송 주소를 성공으로 취급하지 않는다"를 유지한 채 전송 방식 선택만 바꾼다.

### ADR-031 — 스트림 해석 POC의 제한 (KM-056, ADR-032로 대체)

- player 요청에 공개 플레이어 JavaScript에서 확인한 signatureTimestamp 20684를 전달한다. clientVersion과 함께 중앙 설정에 고정한 POC 값이며 동적 플레이어 버전 해석을 구현한 것은 아니다.
- 직접 HTTPS 주소가 있는 audio/mp4 또는 audio/webm 형식 중 높은 bitrate를 선택한다. expiresInSeconds를 절대 만료 시각으로 변환하고 도메인 PlayableStream만 반환한다. URL/원문 응답을 로그에 남기거나 저장하지 않는다.
- 직접 주소가 없는 signatureCipher와 serverAbrStreamingUrl을 일반 오디오 URL로 간주하지 않는다. 로그인/재생 불가 상태도 안전한 실패로 반환한다. 별도 전송 프로토콜을 가짜 성공 값으로 포장하지 않는다.
- 실제 공개 영상 gdZLi9oWNZg의 player 상태는 timestamp 반영 후 OK지만, 응답의 audio 형식에는 URL/signatureCipher가 없고 serverAbrStreamingUrl이 존재했다. 현재 직접 URL 해석 경로로는 재생 주소를 얻지 못하여 실제 계약 검사가 실패했다. 검색 검사 3개는 같은 실행에서 통과했다.
- 추가 전송 방식이나 추출기 도입을 결정하지 않았다. KM-056은 미완료이며 플레이어 연결(KM-057) 및 Provider A 최종 채택은 진행하지 않는다. Provider B는 기존 백로그의 KM-059 실패 조건을 따르며 이번 작업에서 임의로 활성화하지 않는다.
- 신규 의존성, 로그인, DRM/인증 우회, 다운로드, 서비스 소유권 변경은 없다. 공개 재생 페이지와 player 응답의 상태/필드 이름만 진단했다.

### ADR-030 — Provider A 검색 POC와 실제 계약 검증 (KM-055)

- 검색 POC는 공개 WEB 검색 요청 → 내부 JSON mapper → Track 목록으로 구성한다. videoRenderer만 채택하고 채널/재생목록 결과는 제외한다. ID 중복을 제거하며 길이/이미지 누락은 nullable로 유지한다. 예상한 검색 구조가 없으면 빈 목록 대신 실패한다. 취소는 전파하고 외부 예외 원문은 안전한 검색 실패로 치환한다.
- WEB_REMIX 검색은 아이유/BTS Dynamite/Bach에서 노래 목록이 비었다. 공개 ytmusicapi 파라미터와 기존 필터를 비교해도 같았다. PRD가 일반 YouTube의 라이브/커버를 포함하므로 동일 InnerTube-compatible Provider A의 일반 공개 WEB 검색 경로로 변경했다. 최종 설정은 www.youtube.com / WEB / ID 1 / 2.20260901.00.00이며 공개 홈페이지에서 직접 확인했다. 라이브러리/구현 코드를 도입하지 않았고 새 dependency가 없다.
- 일반 영상의 artist에는 제공되는 채널/작성자 이름을 사용한다. 정식 음악 카탈로그의 아티스트 메타데이터와 동일하다고 가정하지 않는다. 검색 결과가 음악 콘텐츠인지를 별도 분류하지 않으며 사용자의 음악 검색어와 공급자의 검색 순서를 따른다.
- AGENTS.md 17에 따라 이 POC의 실제 검색 검사를 sourceContractTest로 분리한다. 일반 단위 테스트에서는 *SourceContractTest를 제외하며, 별도 작업은 매 실행 네트워크를 확인하고 결과를 캐시하지 않는다. KM-063 전체 source 회귀 suite 완료를 의미하지 않는다.
- 이 PC의 기본 JDK 신뢰 저장소에서는 SSLHandshakeException → ValidatorException → SunCertPathBuilderException이 발생했다. 명시적 -PsourceContractUseWindowsTrust=true 옵션을 줄 때만 계약 테스트 JVM에 Windows-ROOT/SunMSCAPI를 사용한다. 이미 OS가 신뢰하는 루트를 사용하며 인증서 검증/호스트 검증을 끄거나 새 인증서를 설치하지 않는다. Android 앱과 일반 테스트의 TLS 설정은 변경하지 않는다.
- 초기 WEB_REMIX 경로의 빈 결과는 지역/계정/프로토콜 변화 중 어느 원인인지 확정하지 않았다. 일반 공개 WEB 경로의 검증 결과로 검색 POC를 판정하며 최종 Provider A 채택은 스트림과 재생 Gate가 통과한 뒤 결정한다.
- 근거: [공개 검색 파라미터 정의](https://github.com/sigma67/ytmusicapi/blob/main/ytmusicapi/parsers/search.py), [Gradle JVM 테스트 작업](https://docs.gradle.org/current/userguide/java_testing.html), [JDK 17 Windows 신뢰 저장소](https://docs.oracle.com/en/java/javase/17/security/oracle-providers.html).

### ADR-029 — Provider A 통신 골격 (KM-054)

- 기존 Ktor HttpClient를 주입받는 internal ProviderAClient와 internal context DTO를 data/source/providerA 아래에 격리한다. UI/도메인은 HTTP 응답이나 이 DTO를 사용하지 않는다.
- 인증 없이 공개 Music 페이지에서 확인한 WEB_REMIX / client ID 67 / 1.20260830.16.00 설정을 사용한다(2026-09-02). Origin·User-Agent·클라이언트 헤더와 언어/지역을 ProviderAConfig 한곳에서 관리한다. 설정은 비공개 프로토콜의 관찰값이며 안정된 API 계약이 아니다.
- 요청마다 중앙 context를 적용하며 쿠키·토큰·로그인 세션이나 별도 API key를 수집하지 않는다. 기존 timeout/취소 동작을 사용하고 요청/응답 로그를 추가하지 않는다.
- MockEngine으로 헤더/JSON/인증 정보 없음 및 HTTP 실패 전달을 검사한다. 아직 검색 mapper·stream resolve·MusicSource binding은 구현하지 않으며 최종 공급자 채택은 Gate 이후에 결정한다. 신규 의존성 없음.
- 근거: [공개 Music 페이지](https://music.youtube.com/), [Ktor 요청 구성](https://ktor.io/docs/client-requests.html).

### ADR-028 — MusicSource 계약 구현 (KM-053)

- PRD/ARCHITECTURE의 네 suspend 함수와 Kotlin Result를 그대로 사용한다. 반환값은 Track/PlayableStream 도메인 타입뿐이며 공급자 DTO는 허용하지 않는다.
- 위치는 ARCHITECTURE의 data/source/MusicSource.kt를 따른다. 이 파일 자체는 Android·HTTP·공급자 라이브러리에 의존하지 않는 경계 계약이다. 구현체는 취소를 실패 값으로 삼키지 않아야 한다.
- 이번 작업은 계약 선언까지다. 공급자 선택이나 오류 변환, DI 연결은 구현하지 않으며 새로운 의존성이 없다.

### KM-051 — 일시적 스트림 모델

- PlayableStream은 PRD의 URL·선택적 MIME/bitrate·만료 시각을 보유한다. 만료 시각은 최소 API 26에서 제공하는 java.time.Instant를 사용하며 신규 의존성은 없다.
- Room/직렬화 annotation을 부여하지 않고 Track과 분리한다. 영구 저장 금지 규칙을 명시하고 실수로 객체를 출력해도 URL이 노출되지 않도록 toString을 가린다. URL 접근 자체와 표현식 비노출을 단위 테스트한다.

### KM-050 — 공급자 중립 곡 메타데이터

- core/model의 Track은 PRD의 ID·제목·아티스트·선택적 이미지/길이·source만 가진 불변 데이터다. 재생 URL이나 공급자 DTO는 포함하지 않는다.
- SourceType.Remote는 외부 콘텐츠라는 출처 분류이며 특정 접근 어댑터 이름이 아니다. ID는 소스가 제공한 값을 유지하고 임의로 다시 생성하지 않는다. 어댑터 교체와 곡 메타데이터를 분리한다.
- 현재는 별도 검증/변환 로직을 넣지 않는다. 값의 검증과 누락 메타데이터 정책은 실제 소스 mapper에서 검증한다. 신규 의존성이나 DB 변경은 없다.

아래 항목은 ARCHITECTURE.md에 정의된 초기 ADR 목록이다. 기존 설계 제약은 그대로 적용하며, 구체적인 구현 결정과 검증 근거는 해당 작업에서 기록한다.

| 번호 | 주제 | 현재 기준 |
| --- | --- | --- |
| ADR-001 | Kotlin 및 Compose | 기존 설계에 명시 |
| ADR-002 | MediaLibraryService의 재생 소유권 | 기존 설계에 명시 |
| ADR-003 | MusicSource 추상화 | 기존 설계에 명시 |
| ADR-004 | Source Provider 선택 | 확정, ADR-037에서 Provider A 채택 |
| ADR-005 | 스트림 URL 영구 저장 금지 | 기존 설계에 명시 |
| ADR-006 | Room 기반 로컬 저장 | 기존 설계에 명시 |
| ADR-007 | v0.1 로그인 제외 | 기존 설계에 명시 |
| ADR-008 | v0.1 영구 다운로드 제외 | 기존 설계에 명시 |
