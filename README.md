# Keuney Music

개인 사용 및 학습을 위한 Android 음악 플레이어 프로젝트다. APK 사이드로드를 전제로 하며 공개 앱스토어 배포는 v0.1 범위에 포함하지 않는다.

## 무엇을 하는 앱인가

곡을 검색해 듣는다. 검색 결과에서 곡을 고르면 그 목록이 대기열이 되고, 화면을 끄거나 다른 앱을 써도
재생이 이어지며 알림과 잠금화면에서 조작할 수 있다. 들은 곡은 최근 재생에 쌓이고 즐겨찾기와 재생목록으로
정리한다. 서버도 계정도 없다. 모든 기록은 기기 안에만 있다.

**현재 상태(2026-09-04): 작업 73개 완료, 4개 미착수. 최종 게이트 KM-200을 통과했다**(Must PASS 15개 중 14개 PASS,
Bluetooth Play/Pause는 기기가 없어 미검증). 검색·재생·배경 재생·알림·잠금화면·대기열·즐겨찾기·
재생목록·최근 재생·홈·라이브러리·설정·어두운 화면까지 동작한다. Samsung SM-T220 / Android 14 실기기에서
31분 연속 사용 검사를 크래시 없이 통과했고(KM-136), 최종 게이트는 릴리스 빌드에서 확인했다(KM-200).
최소 API 26, 컴파일 SDK 37.2, 대상 SDK 37.

남은 미착수 4개는 코드 작업이 아니다. 블루투스 기기가 필요한 두 건(KM-130·131), 조건이 충족되지 않아
보류한 한 건(KM-135), Gate 결과로 활성화하지 않는 한 건(KM-064)이다.

작업별 상태와 재개 지점은 [TASKS.md](TASKS.md), 결정의 이유는 [docs/DECISIONS.md](docs/DECISIONS.md),
시행착오까지 포함한 전체 흐름은 [docs/SEQUENTIAL_RUN.md](docs/SEQUENTIAL_RUN.md)에 있다.

## 개인용 sideload 경계

이 앱은 **한 사람이 자기 기기에 APK를 직접 넣어 쓰는 것**을 전제로 만들었다. 그 전제가 설계 곳곳을 정한다.

- 공개 앱스토어 배포는 v0.1 범위가 아니다. 릴리스 서명은 본인의 키로 로컬에서만 한다(아래 "릴리스 서명").
- 로그인이 없다. 계정·쿠키·토큰을 저장하지 않으며 요청에 실어 보내지도 않는다(PRD 9, AGENTS.md 13).
- 영구 다운로드가 없다. 재생한 구간만 캐시에 잠시 두고 상한을 넘으면 오래된 것부터 지운다. 사용자가
  언제든 비울 수 있고 운영체제가 정리해도 무방하다(ADR-035).
- 재생 주소(스트림 URL)는 어디에도 저장하지 않는다. 필요한 순간에 해석하고 그대로 버린다(ADR-005).
  앨범 이미지 주소(`artwork_url`)를 빼면 데이터베이스에 `url`이 들어간 열이 없는지 계측 검사가 확인한다.
- 음원은 외부 공급자의 공개 웹 프로토콜을 관찰해 가져온다. 그것은 안정된 API 계약이 아니라 관찰값이며
  공급자가 바꾸면 깨진다. 계약 검사가 앱보다 먼저 깨지도록 해 두었다(아래 "외부 소스 검증").
- 오픈소스 라이선스와 재배포 시 생기는 의무는 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)에 적었다.
  GPL 계열 의존성은 없다.

## 아키텍처 요약

세 겹이다. `core`가 도메인 모델과 계약, `data`가 구현(공급자·저장소·설정), `feature`가 화면이다.
`navigation`이 화면을 잇고 `ui`에 공용 구성 요소를 둔다.

- **재생은 `MusicService : MediaLibraryService`만 소유한다.** 화면은 `MediaController`를 통해서만 말을
  건다. 그래서 화면이 사라져도 재생이 이어지고, 알림·잠금화면·미디어 버튼이 같은 세션을 조작한다
  (ADR-002). 반복 모드처럼 저장된 설정을 재생에 옮기는 일도 서비스가 한다(ADR-054).
- **외부 콘텐츠는 `MusicSource` 뒤에 격리한다.** 공급자 DTO와 HTTP 응답은 그 경계를 넘지 못하고, 실패는
  도메인 오류(`AppError`)로 바뀌어 나온다. 원문 예외와 응답은 거기서 끊긴다(AGENTS.md 12, ADR-028).
- **화면은 ViewModel이 노출하는 불변 상태만 본다.** Compose가 저장소나 `MusicSource`에 직접 닿지 않고,
  Media3 상수를 해석하지도 않는다(PRD 36, ARCHITECTURE 19).
- **저장은 Room과 DataStore로 나눈다.** 곡·즐겨찾기·재생목록·재생 기록은 Room(명시적 마이그레이션과
  스키마 파일 보관), 화면 색·반복·WiFi 전용·캐시 상한·기록 켜기는 DataStore다.
- 재생 캐시는 스트림 해석보다 바깥에 둔다. 그래서 캐시에 있는 구간은 주소를 다시 해석하지 않고
  WiFi 전용 제한에도 걸리지 않는다(ADR-036).

자세한 계층 규칙은 [ARCHITECTURE.md](ARCHITECTURE.md)에, 결정의 근거는 ADR에 있다.

## 알려진 한계

- **Bluetooth 조작과 헤드셋 분리를 확인하지 못했다.** 검증 환경에 블루투스 기기가 없다(KM-130·131 미착수).
  잠긴 화면에서 미디어 버튼(재생·다음)이 동작하는 것은 확인했고 그것이 AVRCP와 같은 경로이지만,
  블루투스 전송 자체는 확인한 것이 아니다.
- **재생할 수 없는 곡에서 재생이 멈춰 선다.** 대기열에 다음 곡이 있어도 넘어가지 않는다. 30분 사용 검사에서
  시도한 여섯 곡 중 셋이 공급자 쪽 이유로 해석되지 않았다. 자동으로 다음 곡으로 넘기는 처리가 필요하다
  (KM-136에서 찾았고 아직 작업으로 만들지 않았다).
- **셔플이 켜졌을 때의 실제 재생 순서를 화면에 보여줄 수 없다.** 세션이 컨트롤러에 보내는 Timeline에 셔플
  순서가 실려 오지 않는다. 대기열 화면은 넣은 순서를 보여주고 그렇다고 알린다(ADR-053).
- **캐시 상한을 바꾸면 다음 실행부터 적용된다.** Media3의 evictor는 만든 뒤 상한을 바꿀 방법이 없다.
  설정 화면이 그 사실을 말한다(ADR-068).
- **재생 위치 슬라이더에 접근성 이름을 붙이지 못했다.** 붙이면 접근성 트리에 읽히지 않을 수 있는 별개
  노드가 생긴다. 바로 아래 위치 줄이 "재생 위치 0:09 / 2:00"으로 그 뜻을 말한다(ADR-069).
- **긴 제목의 곡에서는 재생 화면의 셔플·반복 칩이 화면 밖으로 밀린다.** 화면이 세로로 흘러 닿을 수 있지만
  자주 쓰는 조작이 스크롤 뒤에 있다.
- **어둡게를 골라 둔 경우 시작할 때 아주 짧게 시스템 색이 보인다.** 저장된 값이 오기 전의 첫 프레임이다.
  없애려면 값이 올 때까지 화면을 그리지 않아야 하고 그만큼 시작이 늦어진다(ADR-067).
- **OEM 절전으로 인한 배경 종료는 재현되지 않았다.** 깊은 doze와 restricted 대기 버킷에서 34분을 두어도
  재생이 끊기지 않아 관련 안내 화면을 만들지 않았다(KM-135 보류, ADR-065). 삼성의 절전 목록은 adb로 강제할
  수 없어 확인 범위 밖이다.
- **오디오 전용 주소를 쓰지 못한다.** 공급자가 그 주소의 구간 요청을 거부해 영상이 함께 든 progressive
  형식을 쓰고 영상 트랙을 끈다. 곡당 8~25MB로 오디오 전용의 약 3배를 쓴다(ADR-034).

## 프로젝트 문서

- [AGENTS.md](AGENTS.md): 작업 및 구현 규칙
- [PRD.md](PRD.md): 제품 요구사항과 범위
- [ARCHITECTURE.md](ARCHITECTURE.md): 계층 구조와 기술 제약
- [TASKS.md](TASKS.md): 작업 순서, 인수 조건, 진행 상태
- [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md): 배포물에 들어가는 오픈소스와 라이선스, GPL 계열 의존성 상태
- [docs/DECISIONS.md](docs/DECISIONS.md): 기술 결정 기록
- [docs/SOURCE_PROVIDER.md](docs/SOURCE_PROVIDER.md): 소스 공급자 Gate 판정과 채택 조건
- [docs/SEQUENTIAL_RUN.md](docs/SEQUENTIAL_RUN.md): 순차 구현 및 작업별 검증 결과
- [docs/TESTING_PLAYBACK.md](docs/TESTING_PLAYBACK.md): 실기기 재생 회귀 검사 절차
- [docs/DEVICE_RESUME_REPORT.md](docs/DEVICE_RESUME_REPORT.md): 실기기 연결 이후 종합 결과
- [docs/ENVIRONMENT.md](docs/ENVIRONMENT.md): 개발 도구 확인 결과
- [docs/EMULATOR.md](docs/EMULATOR.md): 에뮬레이터 부팅 확인 결과

## 디렉터리 구조

```text
keuney_music/
├── AGENTS.md, PRD.md, ARCHITECTURE.md, TASKS.md, README.md
├── THIRD_PARTY_NOTICES.md
├── settings.gradle.kts, build.gradle.kts, gradle.properties
├── gradlew, gradlew.bat
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── app/
│   ├── build.gradle.kts
│   ├── schemas/                      Room 스키마 파일(마이그레이션 근거)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/keuney/music/
│       │   │   ├── MainActivity.kt, KeuneyApp.kt
│       │   │   ├── core/             도메인 모델과 계약
│       │   │   │   ├── database/{dao,entity}/
│       │   │   │   ├── library/, model/, player/, search/, settings/
│       │   │   ├── data/             구현
│       │   │   │   ├── network/, repository/, settings/
│       │   │   │   └── source/providerA/{dto,mapper}/
│       │   │   ├── di/
│       │   │   ├── feature/          화면
│       │   │   │   └── home/, library/, player/, search/, settings/
│       │   │   ├── navigation/
│       │   │   └── ui/{components,format,theme}/
│       │   └── res/{raw,values,values-night}/
│       ├── test/kotlin/com/keuney/music/          단위 검사 35개 파일
│       └── androidTest/kotlin/com/keuney/music/   실기기 계측 29개 파일
├── scripts/{verify-km012,generate-test-audio}.ps1
└── docs/
    ├── DECISIONS.md, SEQUENTIAL_RUN.md
    ├── SOURCE_PROVIDER.md, TESTING_PLAYBACK.md
    ├── DEVICE_RESUME_REPORT.md
    └── ENVIRONMENT.md, EMULATOR.md
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

작업마다 아래 한 줄을 돌리고 종료 코드 0을 확인한다. 계측 검사가 들어 있으므로 기기나 에뮬레이터가
`adb devices`에 연결돼 있어야 한다.

```powershell
.\gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue
```

2026-09-04 기준으로 단위 183개·실제 계약 7개·실기기 계측 46개가 통과하고 린트는 오류 0개·경고 22개다.
경고를 억제하지 않는다. 문서만 바꾼 작업에서는 계측을 다시 돌리지 않고 같은 코드의 직전 결과를 근거로
적는다.

이전 빌드의 메타스페이스 부족을 해결하기 위해 프로젝트 JVM 힙·메타스페이스 한도를 각각 1GB로 설정했다(ADR-012).

백그라운드 재생 테스트는 API 36 에뮬레이터와 실기기에서 각각 확인했다. 에뮬레이터 성공만으로 실기기 성공을 판정하지 않는다.

실기기 30분 연속 사용 검사(KM-136)는 SM-T220 / Android 14에서 31분 5초 동안 검색·여러 곡·백그라운드·화면 꺼짐·잠금화면 조작을 이어서 수행해 통과했다. 프로세스는 한 번도 다시 시작하지 않았고 crash 버퍼와 ANR 기록이 비어 있다. Bluetooth는 기기가 없어 확인하지 못했다(KM-130·131과 함께 남는다).

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

## 릴리스 서명

서명 정보는 **저장소에 넣지 않는다**(AGENTS.md 13). 빌드는 두 곳에서만 읽는다.

1. 프로젝트 루트의 `keystore.properties` — `.gitignore` 대상이다.
2. 환경 변수 `KEUNEY_KEYSTORE_FILE`·`KEUNEY_KEYSTORE_PASSWORD`·`KEUNEY_KEY_ALIAS`·`KEUNEY_KEY_PASSWORD`.

파일이 있으면 파일을, 없으면 환경 변수를 본다. 둘 다 없으면 **서명하지 않은 APK**를 그대로 만든다.
키가 없는 곳(CI, 다른 PC)에서도 `test`·`lint`·`assembleRelease`가 돌아야 하므로 빌드를 실패시키지 않는다.
대신 release를 만들 때 왜 서명이 빠졌는지 한 줄로 알려 준다.

### 키를 만든다

키는 **본인이 만들고 비밀번호도 본인이 정한다.** 이 저장소에는 키도 비밀번호도 들어오지 않는다.
`-storepass`·`-keypass`를 생략하면 keytool이 물어보므로 명령 기록에 비밀번호가 남지 않는다.

```powershell
& "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -v `
  -keystore C:/keys/keuney-release.jks -alias keuney `
  -keyalg RSA -keysize 4096 -validity 10000
```

키 파일은 **저장소 밖**에 둔다. 잃어버리면 이미 설치된 앱을 갱신할 수 없다. Android는 같은 서명이 아닌
APK를 업데이트로 받지 않으므로, 지우고 다시 설치해야 하며 그때 기기 안의 즐겨찾기·재생목록·기록이 사라진다.

### 빌드에 알려 준다

`keystore.properties`를 프로젝트 루트에 만든다. **경로는 `/`로 쓴다.** `.properties`에서 역슬래시는
이스케이프 문자라 `C:\keys\a.jks`는 엉뚱한 경로가 된다(이 경우 빌드가 그 사실을 알려 준다).

```properties
storeFile=C:/keys/keuney-release.jks
storePassword=...
keyAlias=keuney
keyPassword=...
```

파일은 UTF-8로 저장한다. 빌드가 BOM을 떼어 내므로 편집기가 붙였더라도 동작한다. 값이 비어 있거나 키
파일이 없으면 그 이유를 알려 주고 서명 없이 만든다.

### 확인한다

```powershell
.\gradlew.bat assembleRelease
& "$env:ANDROID_HOME/build-tools/36.0.0/apksigner.bat" verify --print-certs app/build/outputs/apk/release/app-release.apk
```

서명이 걸리면 결과물 이름이 `app-release.apk`이고, 걸리지 않으면 `app-release-unsigned.apk`다.
`apksigner verify`가 인증서를 출력하면 서명이 유효하다.

## 외부 소스 검증

실제 외부 요청은 일반 단위 검사에서 제외되어 있다(`test`는 `*SourceContractTest`를 제외한다). 명시적으로 검사하려면 아래 명령을 실행한다. 공급자가 무언가를 바꾸면 앱보다 이 검사가 먼저 깨지는 것이 목적이다.

| 검사 | 지키는 것 | 깨지면 |
| --- | --- | --- |
| 검색 계약 (3) | 검색 경로 생존, 응답 구조와 mapper 일치, 제목·아티스트·길이·이미지가 계속 채워짐 | 검색 결과가 비거나 화면 메타데이터가 사라진다 |
| 스트림 계약 (1) | 후보 클라이언트별 해석 결과, 파일 **중간 지점** 구간 요청 수용 | 재생이 시작되더라도 앞부분만 듣고 끊긴다 |
| 오류 계약 (2) | 없는 트랙이 재생 불가로 분류되는지, 결과 없음이 실패로 바뀌지 않는지 | 사용자에게 엉뚱한 오류 문구가 보인다 |
| Gate 계약 (1) | 여러 아티스트 10곡의 해석과 중간 구간, 공급자 거부 0건 | Provider A 채택 조건이 무너진다(docs/SOURCE_PROVIDER.md의 재판정 기준) |

스트림·Gate 검사는 종류별 결과와 응답 코드를 요약 출력한다. 공급자 거부(4xx)와 전송 지연(타임아웃)을 구분해 기록하므로 무엇이 깨졌는지 바로 드러난다. URL과 응답 원문은 출력하지 않는다.

```powershell
.\gradlew.bat sourceContractTest --no-daemon --console=plain
```

이 PC는 기본 JDK에서 인증서 신뢰 오류가 발생하여 계약 테스트 JVM에만 Windows의 기존 신뢰 루트를 사용한다. 이 옵션은 Windows에서만 유효하며 앱 TLS 설정을 바꾸지 않는다.

```powershell
.\gradlew.bat sourceContractTest -PsourceContractUseWindowsTrust=true --no-daemon --console=plain
```

결과는 `app/build/reports/tests/sourceContractTest/index.html`에서 확인한다. 인증서 검증을 끄거나 로그인 정보를 추가하지 않는다.
