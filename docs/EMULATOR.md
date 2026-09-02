# KM-003 — 에뮬레이터 부팅 검증

검증일: 2026-09-02 (한국 시간)

## 결과

기존 AVD `Medium_Phone_API_36.0`을 부팅하여 Android 홈 화면, ADB 연결, 화면 캡처를 확인했다. KM-003의 인수 조건 3개는 모두 통과했다.

Gradle Wrapper가 없어 공통 필수 검증은 실행에 실패했다. AGENTS.md 18/27항에 따라 작업 상태는 `[!]`로 보류한다. KM-010 및 이후 작업은 진행하지 않았다.

## 검증 환경

| 항목 | 값 |
| --- | --- |
| Android SDK | `C:\Users\UUH\AppData\Local\Android\Sdk` |
| Android 사용자 설정 | `C:\Users\UUH\.android` |
| AVD 경로 | `C:\Users\UUH\.android\avd\Medium_Phone.avd` |
| AVD 이름 | `Medium_Phone_API_36.0` |
| 시스템 이미지 | Android API 36, Google Play, x86_64 |
| Emulator | 35.5.10 |
| 가속 | `WHPX(10.0.19044) is installed and usable.` |
| ADB 일련번호 | `emulator-5554` |
| 화면 크기 | 1080 × 2400 |

## 실행 방법

기존 AVD를 사용할 수 있는 사용자 계정의 PowerShell에서 프로젝트 루트를 작업 경로로 사용한다. 환경 변수는 해당 프로세스에만 적용한다. 실행 전에 `adb devices -l`로 사용 중인 기기를 확인한다. 아래 명령은 포트 5554가 비어 있는 상태에서 실행했다.

```powershell
$env:ANDROID_HOME = 'C:/Users/UUH/AppData/Local/Android/Sdk'
$env:ANDROID_USER_HOME = 'C:/Users/UUH/.android'
$env:ANDROID_AVD_HOME = 'C:/Users/UUH/.android/avd'
$adbPath = "$env:ANDROID_HOME/platform-tools/adb.exe"
$emulatorPath = "$env:ANDROID_HOME/emulator/emulator.exe"

& $adbPath devices -l
& $emulatorPath -list-avds
& $emulatorPath -accel-check

New-Item -ItemType Directory -Path captures/km-003 -Force | Out-Null
$emulatorProcess = Start-Process -FilePath $emulatorPath `
    -ArgumentList @('-avd', 'Medium_Phone_API_36.0', '-port', '5554',
                    '-no-window', '-no-snapshot', '-no-audio') `
    -WindowStyle Hidden `
    -RedirectStandardOutput "$PWD/captures/km-003/emulator.retry.stdout.log" `
    -RedirectStandardError "$PWD/captures/km-003/emulator.retry.stderr.log" `
    -PassThru
```

창 없이 실행했으며 저장된 스냅샷을 불러오거나 저장하지 않았다. 기존 AVD 설정 파일은 편집하지 않았고 데이터 초기화 옵션도 사용하지 않았다.

부팅 중에는 `offline`이 관찰됐다. 5초 간격으로 부팅 상태를 확인했고 다음 조건을 모두 만족한 후 캡처했다.

```powershell
& $adbPath -s emulator-5554 shell getprop sys.boot_completed
# 1
& $adbPath -s emulator-5554 shell getprop init.svc.bootanim
# stopped
& $adbPath -s emulator-5554 shell getprop ro.boot.qemu.avd_name
# Medium_Phone_API_36.0
& $adbPath -s emulator-5554 shell getprop ro.build.version.sdk
# 36
& $adbPath devices -l
# emulator-5554 device product:sdk_gphone64_x86_64
#   model:sdk_gphone64_x86_64 device:emu64xa transport_id:1
```

화면 캡처와 패키지 관리자 응답을 확인한 명령:

```powershell
& $adbPath -s emulator-5554 shell input keyevent KEYCODE_HOME
& $adbPath -s emulator-5554 shell screencap -p /data/local/tmp/keuney-km003.png
& $adbPath -s emulator-5554 pull /data/local/tmp/keuney-km003.png captures/km-003/boot.png
& $adbPath -s emulator-5554 shell wm size
& $adbPath -s emulator-5554 shell cmd package path com.android.settings
Get-FileHash captures/km-003/boot.png -Algorithm SHA256
```

패키지 관리자는 `/system_ext/priv-app/SettingsGoogle/SettingsGoogle.apk` 경로를 반환했다. 이는 패키지 관리자 응답 확인이며, Keuney Music APK의 설치·실행 검증은 아니다.

## 인수 조건 및 증거

| 인수 조건 | 결과 | 증거 |
| --- | --- | --- |
| AVD 부팅 성공 | 통과 | `sys.boot_completed=1`, `init.svc.bootanim=stopped`, 홈 화면 이미지 확인 |
| `adb devices`에서 `device` 확인 | 통과 | `emulator-5554 device` 및 AVD 이름 확인 |
| 화면 캡처 명령 동작 | 통과 | `screencap` 및 `pull` 종료 코드 0, PNG를 직접 열어 정상 홈 화면 확인 |

로컬 캡처: `captures/km-003/boot.png`  
크기: 1080 × 2400, 1,387,220 바이트  
SHA-256: `3936D04EEBC68A9BE8A4413EC31FE40CB8C8B7CAB97928A5FAE1A11DC0557098`

`captures/`는 기존 `.gitignore`에 의해 제외된다. 캡처와 실행 로그는 로컬 증거이며 Git에 포함하지 않는다.

## 실패 원인과 조치

첫 실행은 샌드박스 계정의 `.android/emu-last-feature-flags.protobuf.lock` 생성 실패를 반복하며 부팅하지 못했다. 최초 프로세스와 그 자식 QEMU 프로세스를 부모 PID로 식별하여 중지했다. 반복 로그는 최초 12줄만 남겼다.

사용자 계정 권한으로 `ANDROID_USER_HOME` 및 `ANDROID_AVD_HOME`을 명시하여 다시 실행한 뒤 부팅·연결·캡처 검증에 성공했다. 전역 환경 변수, Git 설정, SDK 설치 버전은 변경하지 않았다.

검증 종료 후 기기 내부 임시 캡처를 삭제하고 이번 작업에서 시작한 에뮬레이터를 정상 종료했다. 로컬 PNG는 유지했다.

```powershell
& $adbPath -s emulator-5554 shell rm /data/local/tmp/keuney-km003.png
& $adbPath -s emulator-5554 emu kill
# OK: killing emulator, bye bye
# OK
```

## 공통 필수 검증

| 실행 명령 | 결과 |
| --- | --- |
| `./gradlew test` | 실행 실패, 종료 코드 1 — Wrapper 없음 |
| `./gradlew lint` | 실행 실패, 종료 코드 1 — Wrapper 없음 |
| `./gradlew assembleDebug` | 실행 실패, 종료 코드 1 — Wrapper 없음 |

릴리스 설정 및 비즈니스 로직 변경은 없어 릴리스 빌드와 신규 단위 테스트는 해당하지 않는다.

## 남은 위험과 범위

- 일반 샌드박스 계정에서는 AVD 실행에 필요한 사용자 설정 파일 접근이 실패할 수 있다. 재실행에는 해당 AVD에 접근 가능한 사용자 권한이 필요하다.
- Keuney Music 앱 모듈과 APK가 아직 없어 앱 설치·실행은 미검증이다. KM-011 이후 확인한다.
- 실제 기기, 백그라운드 재생, 화면 꺼짐 및 Bluetooth 동작은 이번 작업의 검증 범위가 아니다.
- 에뮬레이터는 검증 후 종료된 상태다. 이후 기기 검증 시 다시 실행해야 한다.
- 전체 완료 처리를 위해 Gradle 구성이 준비된 뒤 공통 필수 검증을 통과해야 한다.
