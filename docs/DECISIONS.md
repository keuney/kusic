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
| ADR-004 | Source Provider 선택 | 미확정, Provider Gate 결과 필요 |
| ADR-005 | 스트림 URL 영구 저장 금지 | 기존 설계에 명시 |
| ADR-006 | Room 기반 로컬 저장 | 기존 설계에 명시 |
| ADR-007 | v0.1 로그인 제외 | 기존 설계에 명시 |
| ADR-008 | v0.1 영구 다운로드 제외 | 기존 설계에 명시 |
