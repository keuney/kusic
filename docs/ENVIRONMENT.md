# Android 개발 환경 검증

검증일: 2026-09-02 (한국 시간)  
대상 작업: KM-002  
작업 경로: `D:\uuh_workspace\keuney_music`

## 결과

설치된 도구의 경로를 현재 PowerShell 프로세스에 지정한 뒤 KM-002의 환경 검증 명령은 모두 성공했다. 연결된 기기는 없으며, 등록된 AVD는 1개다. 에뮬레이터 부팅은 KM-003 범위이므로 수행하지 않았다.

Gradle Wrapper 및 앱 모듈이 없어 AGENTS.md 18/27항의 필수 검증은 통과하지 못했다. 따라서 KM-002의 전체 완료 상태는 보류(`[!]`)다.

## 설치 상태

| 항목 | 확인 결과 |
| --- | --- |
| 운영체제 | Windows 10.0.19044 (`adb version` 출력 기준) |
| JDK | Eclipse Temurin 17.0.18+8 |
| Java 컴파일러 | `javac 17.0.18` |
| JDK 경로 | `C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot` |
| Android SDK | `C:\Users\UUH\AppData\Local\Android\Sdk` |
| SDK Platform | `android-35` |
| Build Tools | `34.0.0`, `35.0.0`, `35.0.1`, `36.0.0` |
| Platform Tools | `35.0.2-12147458` |
| ADB | `1.0.41` |
| Emulator | `35.5.10.0`, 빌드 `13402964` |
| 명령줄 도구 | `cmdline-tools\latest\bin`에 `sdkmanager.bat`, `avdmanager.bat` 존재 |
| 시스템 이미지 디렉터리 | `system-images\android-36\google_apis_playstore` |
| 등록된 AVD | `Medium_Phone_API_36.0` |
| AVD 데이터 경로 | `C:\Users\UUH\.android\avd\Medium_Phone.avd` |
| 연결된 기기 | 없음 |

설치된 SDK 목록을 기록한 것이며, 프로젝트의 compileSdk/targetSdk 또는 빌드 도구 버전을 선택한 것은 아니다.

## 기본 셸에서 발견한 문제

- `java`와 `javac`는 PATH에서 JDK 17로 해석되지만, `JAVA_HOME`은 `C:\Program Files\Java\jdk-11`이다. 해당 경로의 Java는 버전 11로 확인됐다.
- `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `ANDROID_AVD_HOME`, `ANDROID_USER_HOME`은 현재 프로세스에서 설정되지 않았다.
- `adb`, `emulator`, `sdkmanager`, `avdmanager`는 기본 PATH에서 찾을 수 없다.
- 따라서 최초 `adb version`, `adb devices`는 명령을 찾지 못해 종료 코드 1로 실패했다.
- Emulator 절대 경로로 실행한 최초 `-list-avds`는 이름을 출력하지 않았다. `ANDROID_AVD_HOME`을 명시한 재검증에서는 등록된 AVD를 확인했다.

사용자·시스템 환경 변수와 회사 프로젝트 설정은 수정하지 않았다. 아래 설정은 실행한 PowerShell 프로세스에만 적용된다.

## 재현 명령

프로젝트 루트에서 PowerShell로 실행한다. 새 셸에서는 경로 설정을 다시 적용해야 한다.

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot'
$env:ANDROID_HOME = 'C:/Users/UUH/AppData/Local/Android/Sdk'
$env:ANDROID_AVD_HOME = 'C:/Users/UUH/.android/avd'
$env:PATH = "$env:JAVA_HOME/bin;$env:ANDROID_HOME/platform-tools;$env:ANDROID_HOME/emulator;$env:PATH"

java -version
javac -version
adb version
adb devices
emulator -version
emulator -list-avds
```

## 실행 결과 및 인수 조건

| 명령 또는 조건 | 결과 | 근거 |
| --- | --- | --- |
| `java -version` | 통과 | OpenJDK `17.0.18`, Temurin `17.0.18+8`, 종료 코드 0 |
| `javac -version` | 통과 | `17.0.18`, 종료 코드 0 |
| `adb version` | 통과 | ADB `1.0.41`, Platform Tools `35.0.2-12147458`, 절대 경로 실행 종료 코드 0 및 경로 설정 후 동일 출력 |
| `adb devices` | 통과 | 종료 코드 0, 기기 목록 조회 성공, 연결 기기 없음 |
| `emulator -version` | 통과 | 절대 경로로 실행하여 버전 `35.5.10.0` 확인 |
| `emulator -list-avds` | 통과 | `ANDROID_AVD_HOME` 지정 후 `Medium_Phone_API_36.0` 출력, 종료 코드 0 |
| `docs/ENVIRONMENT.md`에 결과 기록 | 통과 | 이 문서에 도구 경로, 결과, 재현 명령 기록 |

주요 출력:

```text
openjdk version "17.0.18" 2026-01-20
OpenJDK Runtime Environment Temurin-17.0.18+8 (build 17.0.18+8)
OpenJDK 64-Bit Server VM Temurin-17.0.18+8 (build 17.0.18+8, mixed mode, sharing)
javac 17.0.18

Android Debug Bridge version 1.0.41
Version 35.0.2-12147458
Installed as C:\Users\UUH\AppData\Local\Android\Sdk\platform-tools\adb.exe
Running on Windows 10.0.19044

List of devices attached

Medium_Phone_API_36.0
```

마지막 줄은 `emulator -list-avds` 출력이며, 실행 중인 기기를 뜻하지 않는다.

## 공통 필수 검증

| 실행 명령 | 결과 | 원인 |
| --- | --- | --- |
| `./gradlew test` | 실행 실패, 종료 코드 1 | `gradlew` 없음 |
| `./gradlew lint` | 실행 실패, 종료 코드 1 | `gradlew` 없음 |
| `./gradlew assembleDebug` | 실행 실패, 종료 코드 1 | `gradlew` 없음 |

테스트나 빌드 내부에서 실패한 것이 아니라 Gradle 실행 자체가 불가능한 상태다. Wrapper는 KM-010, 앱 모듈은 KM-011 범위이며 이번 작업에서는 추가하지 않았다. 릴리스 설정 및 비즈니스 로직 변경이 없어 릴리스 빌드와 신규 단위 테스트는 해당하지 않는다.

## 남은 확인 사항

- 새 셸 및 후속 빌드에서 JDK 17과 SDK 경로를 지정해야 한다. 기본 `JAVA_HOME`을 그대로 사용하면 JDK 11이 선택될 수 있다.
- AVD 등록과 도구 실행만 확인했다. 부팅, 가상화 가속, 앱 설치·실행, 화면 캡처는 검증하지 않았다.
- 실기기 연결 및 재생 검증은 수행하지 않았다. KM-002의 인수 조건에는 실기기 연결 성공이 포함되어 있지 않다.
- Gradle 구성 이후 필수 테스트·린트·디버그 빌드를 재검증해야 전체 완료 처리가 가능하다.
