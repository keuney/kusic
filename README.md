# Keuney Music

개인 사용 및 학습을 위한 Android 음악 플레이어 프로젝트다. APK 사이드로드를 전제로 하며 공개 앱스토어 배포는 v0.1 범위에 포함하지 않는다.

## 현재 상태

KM-039까지 기본 재생을 구현했다. `com.keuney.music` 앱에서 내장된 120초 테스트 음원을 재생·일시정지하고 슬라이더로 위치를 이동할 수 있다. 화면에는 현재 재생 상태와 시간이 표시된다. 최소 API 26, 컴파일 SDK 37.2, 대상 SDK 37을 사용한다.

Hilt, Room, DataStore, Media3, Ktor, Coil 및 GitHub Actions 기본 구성을 추가했다. ExoPlayer와 MediaLibrarySession은 MusicService만 소유하며 화면은 ViewModel과 MediaController를 통해 통신한다. KM-050~055의 공통 모델·MusicSource 계약·실제 공개 검색 POC는 완료했다. 검색은 아직 앱 화면과 연결하지 않았으며 라이브러리 화면도 없다.

KM-056은 실제 응답에서 직접 재생할 오디오 URL을 얻지 못해 보류했다. 일반 빌드와 단위 검사는 통과하지만 실제 스트림 계약 검사는 실패한다. 앱은 현재 내장 테스트 음원을 재생한다. 실기기 연결 이후의 변경 파일·실행 명령·인수 조건과 제한은 [종합 보고서](docs/DEVICE_RESUME_REPORT.md)에 정리했다.

Samsung SM-T220 / Android 14 실기기에서 Home 이동·Activity 종료 후 32초 재생, 62초 화면 꺼짐 재생, 알림·잠금화면 재생 제어와 오디오 포커스를 검증했다. USB 충전 상태의 단기 검사이며 Bluetooth·장시간 절전·다른 OEM은 아직 검증하지 않았다. KM-001·002·003·010은 Wrapper와 앱 모듈 부재로 필수 Gradle 검증을 실행할 수 없어 보류했던 작업이며, KM-011 이후 검증이 통과하므로 인수 조건을 재확인하고 완료로 전환했다. 현재 보류는 KM-056 하나다. 작업별 상태는 TASKS.md, 변경 파일·실행 명령·검증 결과는 docs/SEQUENTIAL_RUN.md에서 확인한다.

## 프로젝트 문서

- [AGENTS.md](AGENTS.md): 작업 및 구현 규칙
- [PRD.md](PRD.md): 제품 요구사항과 범위
- [ARCHITECTURE.md](ARCHITECTURE.md): 계층 구조와 기술 제약
- [TASKS.md](TASKS.md): 작업 순서, 인수 조건, 진행 상태
- [docs/DECISIONS.md](docs/DECISIONS.md): 기술 결정 기록
- [docs/SEQUENTIAL_RUN.md](docs/SEQUENTIAL_RUN.md): 순차 구현 및 작업별 검증 결과
- [docs/TESTING_PLAYBACK.md](docs/TESTING_PLAYBACK.md): 실기기 재생 회귀 검사 절차
- [docs/DEVICE_RESUME_REPORT.md](docs/DEVICE_RESUME_REPORT.md): 실기기 연결 이후 종합 결과
- [docs/ENVIRONMENT.md](docs/ENVIRONMENT.md): 개발 도구 확인 결과
- [docs/EMULATOR.md](docs/EMULATOR.md): 에뮬레이터 부팅 확인 결과

## 현재 디렉터리 구조

```text
keuney_music/
├── .gitignore
├── .gitattributes
├── AGENTS.md
├── PRD.md
├── ARCHITECTURE.md
├── TASKS.md
├── README.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── app/
│   ├── build.gradle.kts
│   ├── schemas/
│   ├── src/test/kotlin/com/keuney/music/
│   ├── src/androidTest/kotlin/com/keuney/music/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── kotlin/com/keuney/music/
│       │   ├── MainActivity.kt
│       │   ├── KeuneyApp.kt
│       │   ├── core/{database,model,player,settings}/
│       │   ├── data/{network,settings,source}/
│       │   ├── di/
│       │   ├── feature/player/
│       │   └── ui/{components,theme}/
│       └── res/{raw,values}/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── scripts/{verify-km012,generate-test-audio}.ps1
└── docs/
    ├── DECISIONS.md
    ├── ENVIRONMENT.md
    ├── EMULATOR.md
    └── SEQUENTIAL_RUN.md
```

## 개발 및 검증

한 번에 하나의 KM 작업만 진행한다. 빌드에는 JDK 17, Android SDK Platform 37.2 및 Build Tools 36.0.0이 필요하다. Gradle 9.7.1과 AGP 9.4.0은 프로젝트 설정에 고정되어 있다.

JDK 17 이상을 사용한다. 이 PC의 기본 `JAVA_HOME`은 JDK 11이므로 PowerShell에서 아래처럼 현재 프로세스의 경로를 지정한다. 다른 PC에서는 실제 JDK 설치 경로로 바꾼다.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot'
$env:ANDROID_HOME = 'C:/Users/UUH/AppData/Local/Android/Sdk'
$env:GRADLE_USER_HOME = Join-Path $PWD '.gradle/user-home'
.\gradlew.bat --version
.\gradlew.bat tasks --no-daemon --console=plain
```

SDK가 준비되어 있지 않다면 Android SDK Manager로 `platforms;android-37.2`와 `build-tools;36.0.0`을 설치한다. 위 로컬 경로는 실제 설치 위치에 맞게 지정한다.

최초 실행에는 공식 Gradle 배포본과 빌드 의존성을 받기 위한 네트워크 접근이 필요하다. Wrapper가 고정된 SHA-256을 확인한다. 필요한 의존성을 모두 내려받은 뒤에는 `--offline`을 사용할 수 있다. macOS/Linux에서는 JDK와 SDK를 지정한 뒤 `./gradlew`를 사용한다.

공통 필수 검증 명령:

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

KM-056 검증에서 일반 단위 28개·lint·assembleDebug·assembleRelease가 통과했다. 실제 계약 검사는 검색 3개 통과·스트림 1개 실패다. 마지막 실기기 회귀 검사(KM-040)는 Samsung SM-T220의 계측 10개가 통과했다. 이후 소스 POC는 앱 재생 화면에 아직 연결하지 않았다. 린트는 오류 0개·경고 18개이며 경고를 억제하지 않았다. 작업별 추가 검증과 한계는 순차 구현 기록을 참고한다.

이전 빌드의 메타스페이스 부족을 해결하기 위해 프로젝트 JVM 힙·메타스페이스 한도를 각각 1GB로 설정했다(ADR-012).

백그라운드 재생 테스트는 API 36 에뮬레이터와 실기기에서 각각 확인했다. 에뮬레이터 성공만으로 실기기 성공을 판정하지 않는다.

디버그 APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성된다. 에뮬레이터가 `adb devices`에 연결된 상태에서 설치·실행한다.

```powershell
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
& "$env:ANDROID_HOME/platform-tools/adb.exe" -s emulator-5554 shell am start -W -n com.keuney.music/.MainActivity
```

`emulator-5554`는 실제 기기 식별자로 바꿀 수 있다. API 36 에뮬레이터에서 설치·실행을 확인했다. 릴리스 APK는 `app/build/outputs/apk/release/app-release-unsigned.apk`이며 서명 전에는 배포용으로 설치할 수 없다.

KM-012 화면 검증을 재실행하려면 디버그 빌드 후 에뮬레이터를 켜고 다음을 실행한다. 스크립트는 APK 설치, 앱 재실행, UI 계층의 앱 이름 확인을 수행하며 실패 시 오류를 반환한다. 화면 캡처와 UI 계층은 Git에서 제외된 `captures/km-012/`에 저장된다. 화면 캡처로 글자와 시스템 바의 가독성도 확인한다.

```powershell
.\scripts/verify-km012.ps1 -Serial emulator-5554
```

서비스·데이터베이스·재생을 포함한 계측 테스트는 API 26 이상 에뮬레이터 또는 기기를 연결한 상태에서 실행한다. 일반 `test` 명령은 계측 테스트를 실행하지 않는다. 테스트 러너는 계측 실행에만 Hilt 테스트 Application을 사용하며 일반 앱은 Manifest의 `KeuneyApp`으로 시작한다. 계측 테스트는 앱 설치와 테스트 음원 재생을 수행한다.

```powershell
.\gradlew.bat connectedDebugAndroidTest --no-daemon --console=plain
```

계측 결과는 `app/build/reports/androidTests/connected/debug/index.html`에서 확인한다. 일반 앱 실행 검증에는 위 `verify-km012.ps1`을 재사용한다. Hilt 및 테스트 의존성 선택은 ADR-012에 기록했다.

## 외부 소스 검증

실제 외부 요청은 일반 단위 검사에서 제외되어 있다. 명시적으로 검사하려면 다음을 실행한다. 현재는 KM-056 스트림 검사 실패로 종료 코드 1이 예상되며 정상 완료로 취급하지 않는다.

```powershell
.\gradlew.bat sourceContractTest --no-daemon --console=plain
```

이 PC는 기본 JDK에서 인증서 신뢰 오류가 발생하여 계약 테스트 JVM에만 Windows의 기존 신뢰 루트를 사용한다. 이 옵션은 Windows에서만 유효하며 앱 TLS 설정을 바꾸지 않는다.

```powershell
.\gradlew.bat sourceContractTest -PsourceContractUseWindowsTrust=true --no-daemon --console=plain
```

결과는 `app/build/reports/tests/sourceContractTest/index.html`에서 확인한다. 인증서 검증을 끄거나 로그인 정보를 추가하지 않는다.

## 설계 방향

재생은 `MusicService : MediaLibraryService`가 소유하고, 외부 콘텐츠 접근은 `MusicSource` 뒤에 격리한다. 첫 재생 검증은 알려진 테스트 오디오를 사용한다. 상세 설계와 구현 순서는 위 문서를 따른다.
