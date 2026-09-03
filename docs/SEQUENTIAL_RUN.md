# 순차 구현 기록 — 2026-09-02

사용자는 KM-013 이후 매 작업마다 재요청하지 않고, 작업별 구현·검증·완료 기록을 마친 뒤 다음 작업을 순차 실행하도록 요청했다. 한 시점에는 하나의 KM 작업만 구현하며, 실패한 인수 조건을 통과로 처리하지 않는다. 실기기 검증 등 외부 조건이 필요한 경우 진행 가능한 범위와 차단 지점을 구분한다.

## 실행 환경과 명령

- JDK 17.0.18, Android SDK 37.2, Gradle 9.7.1, API 36 에뮬레이터.
- `JAVA_HOME`, `ANDROID_HOME`, `GRADLE_USER_HOME`은 기존 README 절차에 따라 검증 프로세스에만 지정한다.
- 공통 검증: `gradlew.bat test lint assembleDebug assembleRelease --continue --no-daemon --console=plain`.
- 계측 테스트가 있는 작업은 `connectedDebugAndroidTest`도 실행한다.
- 작업별 로그는 Git 제외된 `captures/km-xxx-build.log`에 저장한다.
- 이전 Gradle 메모리 부족으로 발생한 `.hprof` 파일이 추적되지 않도록 `.gitignore`에 제외 규칙을 추가했다.

## KM-014 — Room 기반

완료. 인수 조건: KeuneyDatabase PASS, 마이그레이션 정책 PASS, DB 생성·재열기 smoke test PASS.

- 첫 빌드: Room이 엔티티 0개를 거부하여 FAIL. 빈 초기화 테이블 하나로 최소 수정했다.
- 최종 공통 검증 및 `connectedDebugAndroidTest`: PASS (1분 5초). 계측 테스트 2개, 실패·오류·건너뜀 0개. 단위 테스트 소스 없음. 린트 오류 0개, 기존 경고 4개.
- 변경: `.gitignore`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/kotlin/com/keuney/music/core/database/KeuneyDatabase.kt`, `app/src/main/kotlin/com/keuney/music/di/DatabaseModule.kt`, `app/src/androidTest/kotlin/com/keuney/music/core/database/KeuneyDatabaseTest.kt`, `app/schemas/com.keuney.music.core.database.KeuneyDatabase/1.json`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록.
- 파괴적 마이그레이션 fallback은 사용하지 않는다. 초기화 테이블 제거를 포함한 스키마 변경 시 명시적 Migration과 테스트가 필요하다. 실기기 미검증.

## KM-015 — DataStore 설정 저장소

완료. 저장소 skeleton 및 테마 읽기·쓰기 인수 조건 PASS.

- 기본 FileStorage와 Path 팩토리에서 Windows 파일 교체 오류로 각각 테스트 FAIL. 공식 OkioStorage를 앱·테스트에 공통 적용 후 최종 공통 검증과 계측 테스트 PASS (1분 50초).
- 단위 테스트 3개, 계측 테스트 2개 통과. 린트 오류 0개·경고 6개(백업 1, 의존성 최신 버전 안내 5).
- 변경: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/kotlin/com/keuney/music/core/settings/SettingsRepository.kt`, `app/src/main/kotlin/com/keuney/music/data/settings/DataStoreSettingsRepository.kt`, `app/src/main/kotlin/com/keuney/music/data/settings/SettingsDataStore.kt`, `app/src/main/kotlin/com/keuney/music/di/SettingsModule.kt`, `app/src/test/kotlin/com/keuney/music/data/settings/DataStoreSettingsRepositoryTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록.
- 아직 UI와 연결하지 않았다. enum 저장 이름 변경 시 변환 정책 필요. 파일 오류는 호출 계층에 전달하며 향후 UI 연결 시 사용자용 오류로 변환해야 한다.

## KM-016 — Media3 의존성

완료. ExoPlayer/Session 의존성 및 build 인수 조건 PASS. 공통 검증 PASS (1분 51초), 기존 단위 테스트 3개 통과. 의존성 구성만 변경하여 계측 테스트는 재실행하지 않았다.

변경: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 실제 재생은 후속 작업의 검증 대상이다.

## KM-017 — Ktor 네트워크 기반

완료. 공용 클라이언트·세 가지 timeout·민감 로깅 비활성 인수 조건 PASS. 공통 검증 PASS (2분 3초), 단위 테스트 총 6개 통과. 실제 외부 서버는 호출하지 않았다.

변경: `gradle/libs.versions.toml`, `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/com/keuney/music/data/network/MusicHttpClient.kt`, `app/src/main/kotlin/com/keuney/music/di/NetworkModule.kt`, `app/src/test/kotlin/com/keuney/music/data/network/MusicHttpClientTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. Source 오류 매핑과 실제 네트워크 계약 검증은 Source 작업 범위다.

## KM-018 — Coil 기반

완료. Coil Compose 의존성·placeholder 컴포넌트 인수 조건 PASS. 공통 검증 PASS (2분 7초), 단위 테스트 6개 통과. 화면 연결 및 원격 URL fetcher는 아직 없다.

변경: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/kotlin/com/keuney/music/ui/components/Artwork.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록.

## KM-019 — CI 기반

완료. workflow 존재 및 로컬 test/lint/assembleDebug 인수 조건 PASS (10초). Wrapper의 Git 실행 권한 100755도 확인했다. 외부 Actions는 공식 릴리스 커밋으로 고정했다. 원격 GitHub CI 실행은 미검증이며 push하지 않았다.

변경: `.github/workflows/android.yml`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록.

## KM-030 — MusicService 기본 등록

완료. Manifest 서비스 등록, mediaPlayback 유형, 실제 서비스 시작 인수 조건 모두 PASS. 공통 검증과 connectedDebugAndroidTest PASS (1분). 단위 테스트 6개, 계측 테스트 3개 통과. 실제 오디오 재생은 아직 미구현이다.

변경: `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/com/keuney/music/core/player/MusicService.kt`, `app/src/androidTest/kotlin/com/keuney/music/core/player/MusicServiceTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정: ADR-019.

## KM-031 — ExoPlayer 소유권

완료. Activity/ViewModel에 ExoPlayer 없음 및 서비스 종료 시 해제 인수 조건 PASS. `gradlew.bat test lint assembleDebug connectedDebugAndroidTest --continue --no-daemon --console=plain` PASS (44초), 단위 6개·계측 3개 통과. release 설정은 변경하지 않았다.

`rg -n ExoPlayer app/src/main/kotlin`으로 서비스에만 참조가 있음을 확인했다. `adb -s emulator-5554 logcat -d -v brief --pid=5961 ExoPlayerImpl:I *:S`에서 같은 bf3fb8a 인스턴스의 Init/Release를 확인했다. 추가 비즈니스 로직은 없다.

변경: `app/src/main/kotlin/com/keuney/music/core/player/MusicService.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정: ADR-020. 아직 세션·실제 재생은 연결하지 않았다.

## KM-032 — MediaLibrarySession

완료. 세션 생성·플레이어 연결·종료 시 세션 해제 인수 조건 PASS. `gradlew.bat test lint assembleDebug connectedDebugAndroidTest --continue --no-daemon --console=plain` PASS (41초), 단위 6개·계측 4개 통과. 실제 컨트롤러에서 서비스 세션에 연결하여 유휴 상태와 재생 명령을 확인했다. 세션 해제는 onDestroy 코드 및 MediaSessionImpl 로그로 확인했다.

변경: `app/src/main/kotlin/com/keuney/music/core/player/MusicService.kt`, `app/src/androidTest/kotlin/com/keuney/music/core/player/MusicSessionTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정: ADR-021. UI 연결·오디오 재생은 아직 없다.

## KM-033 — MediaController 연결

완료. Activity/플레이어 생명주기 분리, 안전한 연결·해제, 관찰 가능한 상태 인수 조건 PASS. `gradlew.bat test lint assembleDebug connectedDebugAndroidTest --continue --no-daemon --console=plain` 최종 PASS (50초), 단위 6개·계측 5개 통과. 린트 오류 0·경고 15개. 연결 직후 끊김 처리를 보강한 후 전체 검증을 재실행했다.

`scripts/verify-km012.ps1 -Serial emulator-5554`를 재사용하여 일반 앱 설치·Hilt Activity 시작·화면 표시도 PASS. 실제 재생 유지 여부는 아직 검증 대상이 아니다.

변경: `app/src/main/kotlin/com/keuney/music/core/player/PlayerConnection.kt`, `app/src/main/kotlin/com/keuney/music/MainActivity.kt`, `app/src/androidTest/kotlin/com/keuney/music/core/player/PlayerConnectionTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정: ADR-022.

## KM-034 — 알려진 음원 재생

완료. Play/Pause, seek, UI 재생 상태 인수 조건 모두 PASS.

- 최초 전체 검사: 계측 테스트 함수가 내부 PlaybackState 반환 타입을 추론하여 컴파일 FAIL. 테스트 반환 타입을 Unit으로 명시했다.
- `gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.keuney.music.core.player.TestAudioPlaybackTest --no-daemon --console=plain`: 수정 후 PASS (41초).
- 공통 검증과 전체 connectedDebugAndroidTest: 최종 PASS (29초), 단위 8개·계측 6개. 린트 오류 0·경고 17개.
- `scripts/generate-test-audio.ps1`: 120초/16kHz/16-bit mono WAV 생성, 3,840,044 bytes, SHA256 `2B64DA5B2210D0923883EF0988F596F1D8F34BFDE52B4C5A62EFD0924E9B9116`.
- `scripts/verify-km012.ps1 -Serial emulator-5554`: 실제 앱 설치·콜드 실행 PASS. ADB `input tap`, `uiautomator dump`, `pull`, `screencap`으로 재생 중/일시정지 표시, 시간 증가, 화면 슬라이더 1:00 / 2:00 탐색을 확인했다. 화면 캡처도 확인했다. 증거: `captures/km-034-*.xml/png`.
- 변경: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/kotlin/com/keuney/music/MainActivity.kt`, `app/src/main/kotlin/com/keuney/music/core/player/MusicService.kt`, `app/src/main/kotlin/com/keuney/music/core/player/PlayerConnection.kt`, `app/src/main/kotlin/com/keuney/music/core/player/PlaybackState.kt`, `app/src/main/kotlin/com/keuney/music/feature/player/PlayerViewModel.kt`, `app/src/main/kotlin/com/keuney/music/feature/player/TestPlaybackScreen.kt`, `app/src/main/res/values/strings.xml`, `app/src/main/res/raw/test_tone.wav`, `scripts/generate-test-audio.ps1`, `app/src/test/kotlin/com/keuney/music/core/player/PlaybackStateTest.kt`, `app/src/androidTest/kotlin/com/keuney/music/core/player/TestAudioPlaybackTest.kt`, `app/src/androidTest/kotlin/com/keuney/music/core/player/MusicSessionTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록.

결정: ADR-023. 아직 실제 음악 Source, 배경·화면 꺼짐 실기기 검증은 없다. 내장 음원은 기본 재생 검증 전용이며 릴리스도 미서명이다.

## KM-035 — 백그라운드 재생

구현 및 에뮬레이터 검증 통과, 실기기 검증 대기. 완료로 표시하지 않았다.

| 인수 조건 | API 36 에뮬레이터 | 실제 휴대폰 |
| --- | --- | --- |
| 재생 시작 | PASS | 미검증 |
| Home 이동 | PASS | 미검증 |
| 30초 이상 재생 유지 | PASS — 32초 대기 후 재생 중·위치 증가 확인 | 미검증 |
| UI Activity 없이 서비스 활성 | PASS — Activity 파괴·모든 UI/테스트 컨트롤러 해제 후 foreground 서비스 확인 | 미검증 |

- 처음에는 백그라운드 검사는 통과했으나 뒤따르는 세션 검사가 이전 재생 상태를 초기 상태로 오인하여 FAIL. 서비스가 없어질 때까지 기다리는 시도도 timeout으로 FAIL했다. 서비스 종료·컨트롤러 해제의 비동기성을 고려하여 세션 검사가 stop 명령으로 자신의 시작 상태를 준비하게 수정했다. 실제 재생 유지 기준은 변경하지 않았다.
- 영향 검사: `gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.keuney.music.core.player.BackgroundPlaybackTest,com.keuney.music.core.player.MusicSessionTest --no-daemon --console=plain` 시도 후, 전체 계측 회귀 검사 `gradlew.bat connectedDebugAndroidTest --no-daemon --console=plain` 최종 PASS (1분 17초).
- 최종 필수 검사: `gradlew.bat test lint assembleDebug connectedDebugAndroidTest --continue --no-daemon --console=plain` PASS (1분 5초). 단위 8개·계측 7개, 실패·오류·건너뜀 0개. 린트 오류 0개·경고 17개. 앱/release 설정 변경은 없어 assembleRelease는 KM-034의 최종 PASS를 유지한다.
- 변경: `app/src/androidTest/kotlin/com/keuney/music/core/player/BackgroundPlaybackTest.kt`, `app/src/androidTest/kotlin/com/keuney/music/core/player/MusicSessionTest.kt`, `README.md`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정: ADR-024.

## 다음 실행과 남은 위험

아래 목록은 최초 에뮬레이터 실행 종료 시점의 기록이다. 이후 실기기 진행 상황은 다음 절에 별도로 갱신한다.

- 검증 당시 `adb devices -l`에는 emulator-5554만 있었다. USB 디버깅을 허용한 실제 Android 휴대폰에서 KM-035를 확인해야 한다. 실기기 통과 전에는 완료하지 않으며 KM-036 이후 작업을 시작하지 않았다.
- KM-001/002/003/010의 이전 보류 상태는 사용자 지시에 따라 유지한다. 과거 보류 원인이 현재도 그대로라는 의미는 아니며 이번 순차 실행에서 재판정하지 않았다.
- 화면 꺼짐, 잠금화면, Bluetooth, 헤드셋 분리, 오디오 포커스 및 OEM 배터리 동작은 아직 검증하지 않았다. API 26/37에서도 실행하지 않았다.
- 린트 경고 17개: 백업 전송 정책 1개, Media3용 exported 서비스 1개, 의존성 버전 안내 15개. 린트 기준을 완화하지 않았다.
- GitHub Actions는 파일과 로컬 검증만 완료했으며 원격 실행·push는 하지 않았다. 대부분 파일이 아직 untracked이며 이번 실행에서 commit하지 않았다. `git diff --check`는 추적된 변경만 검사하므로 전체 파일 검사로 간주하지 않는다.
- 검토 명령: `Get-Content`, `rg`, `git -c safe.directory=D:/uuh_workspace/keuney_music status --short`, `git ... diff --check`, 테스트 XML·린트 결과 조회. ADB 장치 확인과 서비스/Media3 로그를 함께 검토했다.
- 로컬 디버그 APK: `app/build/outputs/apk/debug/app-debug.apk`. 릴리스 APK는 미서명이다. 작업별 로그·UI 증거는 Git 제외된 captures에 보관한다.
- 검증 후 `adb -s emulator-5554 shell am force-stop com.keuney.music`, `adb -s emulator-5554 emu kill`로 이번 실행에서 시작한 테스트 앱과 에뮬레이터를 종료했다.

## 실기기 재개 — KM-035 완료

- 사용자가 USB 디버깅을 허용한 Samsung SM-T220 / Android 14(API 34)를 연결했다. `adb devices -l` 및 getprop로 연결·모델·버전을 확인했다.
- `gradlew.bat test lint assembleDebug connectedDebugAndroidTest --continue --no-daemon --console=plain`: PASS (1분 17초). 단위 8개·실기기 계측 7개, 실패·오류·건너뜀 0개. 로그: `captures/km-035-device.log`.
- Home 이동, Activity 파괴, 모든 UI/테스트 컨트롤러 해제, 32초 후 foreground 서비스 및 재생 위치 증가를 실기기에서 확인했다. KM-035 인수 조건 모두 PASS로 완료 처리했다.
- 코드 수정 없음. 변경 파일: `TASKS.md`, `README.md`, 이 기록. 기존 ADR-024 설계를 유지하며 신규 기술 결정은 없다.
- USB 연결된 한 기기의 단기 검사 결과이며 다른 OEM, 충전 분리 상태, 장시간 절전 검증은 남아 있다.

## 실기기 재개 — KM-036 완료

- WAKE_MODE_LOCAL과 WAKE_LOCK을 명시했다. Media3가 재생 시 CPU wake lock을 관리한다. 배터리 최적화 설정은 변경하지 않았다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest --continue --no-daemon --console=plain`: PASS (2분 55초). 단위 8개·SM-T220 실기기 계측 8개, 실패·오류·건너뜀 0개. 로그: `captures/km-036-device.log`.
- 재생 시작 PASS, 화면 꺼짐 PASS, 62초간 화면 꺼짐 유지 및 화면을 켜기 전 60초 이상 재생 위치 증가·재생 중 상태 PASS. 검사 종료 시 재생을 정리하고 화면을 깨웠다.
- 변경: `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/com/keuney/music/core/player/MusicService.kt`, `app/src/androidTest/kotlin/com/keuney/music/core/player/ScreenOffPlaybackTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정: ADR-025.
- 이 작업에서는 에뮬레이터를 재실행하지 않았다. USB 충전 상태의 실기기 한 대에서 통과했으며 긴 Doze·다른 OEM·네트워크 스트리밍 검증은 남는다.

## 실기기 재개 — KM-037 완료

- 제목/아티스트, 앱 내부에서 생성한 PNG 기본 이미지, 앱으로 복귀하는 PendingIntent를 Media3 기본 알림에 연결했다. POST_NOTIFICATIONS를 선언하고 미디어 세션 알림 예외를 사용한다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest --continue --no-daemon --console=plain`: PASS (3분 4초). 단위 8개·실기기 계측 9개, 실패·오류·건너뜀 0개. 로그: `captures/km-037-device.log`.
- 제목 PASS, 기본 이미지 PASS, 실제 알림 PendingIntent의 pause/play PASS, Media3 playlist/명령 가용성에 따른 이전/다음 구조 PASS. 한 곡만 준비되어 있으므로 실제 다음 곡 이동은 후속 queue 작업에서 검증한다.
- 일반 APK를 `adb install -r`로 별도 설치하고 `am start`, `input tap`, `cmd statusbar expand-notifications`, `screencap`, `pull`로 확인했다. 이미지·제목·이전/일시정지 버튼이 실제 Samsung 미디어 카드에 표시됐다. `uiautomator dump`는 재생 애니메이션 때문에 idle timeout이 발생하여 해당 XML은 증거로 사용하지 않았고 PNG를 직접 확인했다.
- 변경: `app/src/main/AndroidManifest.xml`, `app/src/main/kotlin/com/keuney/music/core/player/MusicService.kt`, `app/src/main/kotlin/com/keuney/music/core/player/PlaceholderArtwork.kt`, `app/src/androidTest/kotlin/com/keuney/music/core/player/MediaNotificationTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정: ADR-026.
- 계측 검증 후 일반 앱 미설치로 첫 수동 실행이 실패했으나 디버그 APK를 설치한 뒤 정상 실행했다. 제조사별 미디어 카드 배치는 달라질 수 있다.

## 실기기 재개 — KM-038 완료

- Media3 기본 잠금화면 표시를 사용하며 추가 앱 코드는 필요하지 않았다. 실제 SM-T220 잠금화면에서 테스트 오디오 제목·아티스트·이미지가 표시됐다.
- 잠금화면의 버튼을 직접 터치한 뒤 `dumpsys media_session`에서 PAUSED(2), 다시 터치 후 PLAYING(3)을 확인했다. `dumpsys window policy`의 showing=true도 확인했다. 스크린샷에서 일시정지/재생 아이콘 전환을 확인했다. 인수 조건 모두 PASS.
- `gradlew.bat test lint assembleDebug --continue --no-daemon --console=plain`: PASS (9초). 코드 변경이 없어 기존 단위 테스트 8개는 최신 상태를 재사용했다. 이번 작업은 수동 실기기 UI 검증이며 계측 테스트 수를 추가하지 않는다.
- 실행: ADB `input keyevent KEYCODE_SLEEP/KEYCODE_WAKEUP`, `input swipe`, `input tap`, `am start`, `dumpsys media_session`, `dumpsys window policy`, `screencap`, `pull`. 증거: `captures/km-038-lockscreen.png`, `km-038-paused.png`, `km-038-paused.txt`, `km-038-resumed.txt`, `km-038-keyguard.txt`, 빌드 로그 `km-038-build.log`.
- 검사 도중 USB가 offline으로 전환되어 장치 한 대에 대한 `adb -s <serial> reconnect`로 복구했다. 첫 버튼 검사는 음원 종료와 겹쳐 인정하지 않았고 음원을 처음부터 재시작해 pause/resume을 연속 검증했다.
- 변경: `TASKS.md`, 이 기록. 신규 의존성·기술 결정 없음. 암호가 없는 스와이프 잠금화면 조건이며 보안 잠금과 다른 OEM 배치는 미검증이다. 검사 후 음원을 일시정지했다.

## 실기기 재개 — KM-039 완료

- ExoPlayer에 음악용 AudioAttributes와 자동 포커스 처리를 연결했다. 다른 포커스 요청의 일시적/영구적 손실에 대한 계측 검사와 재생 억제 상태의 단위 검사를 추가했다.
- `gradlew.bat test lint assembleDebug connectedDebugAndroidTest --continue --no-daemon --console=plain`: PASS (3분 6초). 단위 9개·SM-T220 계측 10개, 실패·오류·건너뜀 0개. 린트 오류 0개·경고 18개(기존 17개와 기본 이미지 생성의 UseKtx 권고). 로그: `captures/km-039-device.log`.
- 인수 조건: 다른 오디오 세션의 중단 처리 PASS, 포커스 손실 상태 테스트 PASS, 충돌 없이 검사 종료 PASS. 일시적 손실에서 위치 정지·복귀 후 재개, 영구 손실 후 자동 재개하지 않음을 확인했다.
- 변경: `app/src/main/kotlin/com/keuney/music/core/player/MusicService.kt`, `app/src/test/kotlin/com/keuney/music/core/player/PlaybackStateTest.kt`, `app/src/androidTest/kotlin/com/keuney/music/core/player/AudioFocusTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정: ADR-027, 신규 의존성 없음.
- 실제 통화·Bluetooth·다른 OEM 및 API 35 이상 포커스 정책의 실기기 검증은 남는다.

## KM-040 완료 — 재생 회귀 검사 절차

- `docs/TESTING_PLAYBACK.md`에 기기 연결·전체/재생 계측 명령·수동 검사·정리·실패 재현 절차를 작성했다. 기존 테스트를 재사용하며 신규 앱 코드와 의존성은 없다.
- 인수 조건: 문서 존재 PASS, 백그라운드 절차 PASS, 화면 꺼짐 절차 PASS, 앱 세션을 구분하는 dumpsys media_session 확인법 PASS.
- `gradlew.bat test lint assembleDebug connectedDebugAndroidTest --continue --no-daemon --console=plain`: PASS (2분 36초), 단위 9개·SM-T220 계측 10개. 로그 `captures/km-040-regression.log`. 진행 확인에 ADB logcat의 TestRunner 태그를 사용했다.
- 변경: `docs/TESTING_PLAYBACK.md`, `README.md`, `TASKS.md`, 이 기록. 신규 기술 결정 없음. 기존 실기기 조건과 미검증 범위는 문서에 명시했다.

## KM-050 완료 — Track

- 공급자 중립의 불변 메타데이터 모델을 추가했다. source는 Remote 출처 분류이며 어댑터 이름은 노출하지 않는다. 재생 URL은 포함하지 않는다.
- 인수 조건: 도메인 모델에 공급자 이름 없음 PASS. 별도 검증 로직 없어 신규 단위 검사 불필요. `gradlew.bat test lint assembleDebug --continue --no-daemon --console=plain` PASS (36초), 기존 단위 9개 통과. 로그 `captures/km-050-build.log`.
- 변경: `app/src/main/kotlin/com/keuney/music/core/model/Track.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 신규 의존성 없음. 외부 데이터의 누락/검증 정책은 아직 구현하지 않았다.

## KM-051 완료 — PlayableStream

- URL·MIME·bitrate·선택적 Instant 만료 시각을 갖는 일시적 모델과 URL 비노출 toString을 추가했다. 인수 조건 네 항목 모두 PASS.
- `gradlew.bat test lint assembleDebug --continue --no-daemon --console=plain` PASS (36초), 단위 10개. 로그 `captures/km-051-build.log`.
- 변경: `app/src/main/kotlin/com/keuney/music/core/model/PlayableStream.kt`, `app/src/test/kotlin/com/keuney/music/core/model/PlayableStreamTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 영구 저장 금지와 표현식 비노출 결정을 기록했다. 실제 스트림 만료/재해석은 아직 구현하지 않았다.

## KM-052 완료 — AppError

- PRD에 정의된 오류 분류 다섯 개를 sealed interface로 선언했다. 원문 예외·메시지·URL을 담지 않는다. 인수 조건 다섯 항목 모두 PASS. 선언만 추가하여 신규 비즈니스 로직 테스트 대상은 없다.
- `gradlew.bat test lint assembleDebug --continue --no-daemon --console=plain` PASS (36초), 기존 단위 10개. 로그 `captures/km-052-build.log`.
- 변경: `app/src/main/kotlin/com/keuney/music/core/model/AppError.kt`, `TASKS.md`, 이 기록. 기존 PRD 결정을 구현하며 신규 의존성/ADR 없음. 공급자 오류 mapper는 후속 작업이다.

## KM-053 완료 — MusicSource 계약

- 네 suspend 함수가 도메인 Track/PlayableStream만 반환하도록 계약을 선언했다. 함수 네 개와 도메인 타입 한정 인수 조건 모두 PASS. 동작 로직 없는 인터페이스이므로 신규 단위 테스트 대상은 없다.
- `gradlew.bat test lint assembleDebug --continue --no-daemon --console=plain` PASS (35초), 기존 단위 10개. 로그 `captures/km-053-build.log`.
- 변경: `app/src/main/kotlin/com/keuney/music/data/source/MusicSource.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정 ADR-028. 구현체와 실제 외부 요청은 아직 연결하지 않았다.

## KM-054 완료 — Provider A 통신 골격

- internal client/DTO와 중앙 설정을 추가했다. 공개 페이지의 클라이언트 이름·버전·번호만 확인했으며 인증 정보를 수집하거나 저장하지 않았다.
- 인수 조건: 공급자 DTO 격리 PASS, headers/context 중앙 관리 PASS, UI 의존성 없음 PASS. 모의 HTTP 검사 2개 추가.
- `gradlew.bat test lint assembleDebug --continue --no-daemon --console=plain` PASS (38초), 단위 12개. 로그 `captures/km-054-build.log`. 추가 명령: 공개 페이지 `Invoke-WebRequest`와 설정 키 한정 정규식 조회, 공식 Ktor 문서 확인.
- 변경: `app/src/main/kotlin/com/keuney/music/data/source/providerA/ProviderAClient.kt`, `ProviderAConfig.kt`, `dto/ProviderAContext.kt`, `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderAClientTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정 ADR-029. 비공개 프로토콜의 변경 가능성은 있으며 실제 검색/재생 성공은 아직 주장하지 않는다.

## KM-055 완료 — 실제 공개 검색 POC

- ProviderASearch와 JSON → Track mapper를 추가했다. 빈 검색·빈 결과·HTTP/parse 실패·취소·중복 ID·선택적 메타데이터를 검사한다. 실제 검색 검사는 일반 단위 검사에서 분리했다.
- 초기 WEB_REMIX 검색은 HTTP 200이지만 노래 결과가 없었다. 일반 검색에도 팟캐스트 등 비음악 결과만 있어 완료로 인정하지 않았다. 같은 Provider A의 공개 일반 WEB 검색으로 변경한 후 실제 곡 후보와 메타데이터를 받았다. 채널/작성자를 artist로 사용하며 정식 음악 카탈로그의 아티스트 정보와 같다고 가정하지 않는다.
- 초기 JVM 실제 검사 3개는 TLS 인증서 신뢰 실패였다. 예외 종류만 기록해 SSLHandshakeException → ValidatorException → SunCertPathBuilderException을 확인했다. Windows-ROOT를 사용하는 명시적 테스트 JVM 옵션으로 해결했다. TLS 검증을 끄거나 계정/인증 정보를 사용하지 않았다. 영향 검사에서 음악 전용 경로의 빈 결과도 재현했다.
- 최종 `gradlew.bat test lint assembleDebug assembleRelease sourceContractTest -PsourceContractUseWindowsTrust=true --continue --no-daemon --console=plain` PASS (51초). 단위 20개·실제 계약 3개, 오류·실패 0개. 로그 `captures/km-055-public-search.log`. 이전 실패/진단 로그는 km-055-verification/diagnostic/tls-diagnostic/windows-trust에 남겼다.
- 인수 조건: 실제 검색어 아이유/BTS Dynamite/Bach PASS, 빈 결과/오류 처리 PASS, DTO mapper 테스트 PASS. 최신 실기기 재생 검사는 KM-040의 10개이며 검색 POC를 화면/플레이어에 아직 연결하지 않았다.
- 변경: `app/build.gradle.kts`, `app/src/main/kotlin/com/keuney/music/data/source/providerA/ProviderAConfig.kt`, `ProviderASearch.kt`, `mapper/ProviderASearchMapper.kt`, `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderAClientTest.kt`, `ProviderASearchTest.kt`, `ProviderASearchMapperTest.kt`, `ProviderASearchSourceContractTest.kt`, `TASKS.md`, `docs/DECISIONS.md`, 이 기록. 결정 ADR-030, 새 의존성 없음.
- 추가 명령: 공개 홈페이지/검색 endpoint에 대한 Invoke-WebRequest/Invoke-RestMethod, 설정 키/결과 유형 한정 조회, sourceContractTest 단독 및 단일 테스트 재현, 최종 전체 검사 전 test/lint/assembleDebug/assembleRelease 재검사(32초 PASS). 다음 단계는 실제 stream resolve다.

## KM-056 보류 — 직접 스트림 해석 미충족

- 직접 HTTPS 오디오 형식 선택, 만료 시각 변환, 안전한 실패·취소 처리를 구현하고 단위 8개를 추가했다. URL은 PlayableStream에 일시적으로만 보유하며 로그/영구 저장을 하지 않는다. 공급자 응답의 별도 전송 주소나 signatureCipher를 오디오 URL로 간주하지 않는다.
- 초기 player 요청은 UNPLAYABLE이었다. 공개 재생 페이지/플레이어 스크립트에서 확인한 signatureTimestamp 20684를 포함하면 OK가 되지만, 오디오 형식에는 직접 URL/signatureCipher가 없고 serverAbrStreamingUrl만 존재했다. 공개 테스트 영상과 음악 영상의 상태·필드 이름만 비교하고 URL은 출력하지 않았다.
- `gradlew.bat test lint assembleDebug assembleRelease sourceContractTest -PsourceContractUseWindowsTrust=true --continue --no-daemon --console=plain`: FAIL (51초), 실제 검색 계약 3개 PASS·스트림 계약 1개 FAIL. 일반 단위 28개·lint·debug/release 빌드 대상은 통과했다. 로그 `captures/km-056-verification.log`.
- 분리한 최종 `gradlew.bat test lint assembleDebug assembleRelease --continue --no-daemon --console=plain`: PASS (10초), 단위 28개 실패/오류 0, 린트 오류 0·경고 18. 로그 `captures/km-056-final-build.log`.
- 인수 조건: 도메인 PlayableStream 변환·URL 비로깅은 구현/단위 검사 PASS, 실제 곡의 stream resolve 성공은 FAIL. AGENTS.md 19/27에 따라 완료하지 않고 TASKS.md에 보류로 표시했다. KM-057 이후 작업은 시작하지 않았다.
- 변경: `app/src/main/kotlin/com/keuney/music/data/source/providerA/ProviderAConfig.kt`, `ProviderAStreamResolver.kt`, `mapper/ProviderAStreamMapper.kt`, `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderAStreamMapperTest.kt`, `ProviderAStreamResolverTest.kt`, `ProviderAStreamSourceContractTest.kt`, `README.md`, `TASKS.md`, `docs/DECISIONS.md`, `docs/DEVICE_RESUME_REPORT.md`, 이 기록. 결정 ADR-031. 신규 의존성 없음.
- 종료 전 실제 기기 get-state=device, `adb install -r app/build/outputs/apk/debug/app-debug.apk` Success, `adb shell am start -W -n com.keuney.music/.MainActivity` Status ok를 확인했다. 내장 테스트 음원 화면을 열었고 음원을 자동 재생하지 않았다.
- 종합 결과와 전체 변경 파일·명령·인수 조건·위험은 `docs/DEVICE_RESUME_REPORT.md`에 정리했다. 추가 전송 방식 검증 또는 Provider B 평가 순서 조정이 필요하며 아직 어떤 대안도 채택하지 않았다. commit/push는 하지 않았다.

## 저장소 베이스라인 커밋과 M0 보류 재판정 (2026-09-02)

- 저장소에 커밋이 하나도 없어 KM-001~056의 결과 전체가 버전 관리 밖에 있었다. 추적 대상 80개 파일을 최초 커밋 `87aecb0 feat: KM-001~KM-056 구현 베이스라인`으로 보존했다. `.gitignore`가 `build/`, `.gradle/`, `.kotlin/`, `captures/`, `*.apk`, `*.hprof`, 서명키·시크릿 패턴을 정상 제외함을 스테이징 목록으로 확인했다. 커밋 시 `.gitattributes`의 `* text=auto`가 저장소 내용을 LF로 정규화했다.
- 루트에 남아 있던 힙 덤프 `java_pid31588.hprof`(424MB)를 삭제했다. ADR-012의 메타스페이스 부족 진단 잔해이며 `gradle.properties`의 `-Xmx1g -XX:MaxMetaspaceSize=1g`로 원인이 해소된 상태다. Git 추적 대상이 아니었고 다른 `.hprof` 파일은 없다.
- KM-001·002·003·010의 보류 사유는 모두 "당시 Gradle Wrapper 또는 앱 모듈이 없어 AGENTS.md 18/27의 필수 검증을 실행할 수 없음" 하나였고 KM-010·011 완료로 해소됐다. 각 작업의 인수 조건을 재확인한 뒤 완료로 전환했다.
- 재판정 근거: `gradlew.bat test lint assembleDebug --offline --continue --no-daemon --console=plain` BUILD SUCCESSFUL(29초). `gradlew.bat tasks --offline` BUILD SUCCESSFUL(8초). `java -version` OpenJDK 17.0.18 Temurin, `adb version` 1.0.41 / 35.0.2, `adb devices` R9PRB0PNLVT device, `emulator -list-avds` Medium_Phone_API_36.0. 루트 문서 4종·docs/DECISIONS.md·.gitignore·Wrapper 4개 파일·settings/build/libs.versions.toml 존재 확인.
- 변경: `TASKS.md`(상태 4개와 재판정 기록), `README.md`, 이 기록. 앱 코드·의존성·기술 결정 변경 없음. 신규 테스트 대상 없음.
- 현재 상태: 완료 30개, 보류 1개(KM-056), 미착수 44개. 보류는 KM-056 하나뿐이며 KM-057 이후는 이 Gate에 막혀 있다. 원격 push와 작업 브랜치 생성은 하지 않았다.

## KM-056 완료 — 재생 요청의 클라이언트 프로필 분리

- 브랜치 `codex/KM-056-stream-client`에서 작업했다. WEB이 직접 오디오 URL을 주지 않는 문제를, 재생 요청에 사용할 클라이언트 설정을 `ProviderAClientProfile`로 분리하고 후보를 순서대로 시도하는 방식으로 해결했다. 검색 경로는 KM-055에서 통과한 WEB 설정을 그대로 둔다.
- 후보 5종을 한 번에 판정하도록 계약 검사를 확장했다. 실제 결과: IOS 해석 성공(audio/webm, bitrate 151020, 만료시각 있음, 부분요청 PASS), ANDROID 동일 성공, ANDROID_VR·TVHTML5_SIMPLY_EMBEDDED_PLAYER는 "Track is not playable", WEB은 "No direct audio stream available". 성공한 종류를 앞으로 옮겨 최종 순서를 IOS → ANDROID → ANDROID_VR → TVHTML5 → WEB으로 정했다.
- 인수 조건: 실제 테스트 Track stream resolve 성공 PASS, full resolved URL 로그 금지 PASS, domain PlayableStream 반환 PASS.
- `gradlew.bat test lint assembleDebug assembleRelease sourceContractTest -PsourceContractUseWindowsTrust=true --offline --continue --no-daemon --console=plain`: PASS, 종료 코드 0(50초). 단위 31개(기존 28 + 신규 3)·실제 계약 4개 전부 통과, 실패/오류 0. 린트 오류 0·경고 18(변화 없음). debug/release APK 생성.
- 단위 검사 추가: 서명 타임스탬프를 보내는 종류와 보내지 않는 종류의 요청 본문 구분, 직접 URL이 없는 응답에서 다음 후보로 넘어가는 대체 동작, 모든 후보 실패 시 마지막 안전 실패 유지. 기존 취소 전파와 빈 ID 검사는 유지했다.
- 변경: `app/src/main/kotlin/com/keuney/music/data/source/providerA/ProviderAConfig.kt`, `ProviderAClient.kt`, `ProviderAStreamResolver.kt`, `dto/ProviderAContext.kt`, `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderAStreamResolverTest.kt`, `ProviderAStreamSourceContractTest.kt`, `TASKS.md`, `README.md`, `docs/DECISIONS.md`, 이 기록. 결정 ADR-032, ADR-031은 대체 표시. 신규 의존성 없음.
- 계약 검사 요약 출력에는 클라이언트 이름·성공 여부·고정 실패 메시지·MIME·bitrate·만료 유무만 남기며 URL과 응답 원문은 출력하지 않는다.
- 한계: 클라이언트 종류별 설정은 공개 관찰값이라 공급자 변경에 취약하다. 검증은 JVM 계약 검사와 Range 요청 206까지이며 Android 기기의 실제 재생, 여러 곡·장시간 재생, 만료 후 재해석은 KM-057·KM-058·KM-061에서 확인한다. 단일 트랙 기준이며 10곡 세트 판정은 KM-059 Gate에서 수행한다. commit은 작업 브랜치에만 있고 push는 하지 않았다.

## KM-057 완료 — MusicSource와 재생 연결

- 브랜치 `codex/KM-057-stream-resolver-integration`. `ProviderAMusicSource`가 검색·스트림 해석을 MusicSource 계약 뒤에 묶고 `SourceModule`이 바인딩한다. `core/player/StreamResolver`는 MusicSource만 호출하는 얇은 seam이며 KM-061의 만료 재해석 자리다.
- MediaItem에는 `keuney://track/<id>` 자리표시 URI만 넣는다. 컨트롤러가 보낸 MediaItem은 URI를 잃으므로 `onAddMediaItems`에서 mediaId로 복원하고, `ResolvingDataSource`가 재생 직전 로딩 스레드에서 실제 주소로 바꾼다. 대기열·컨트롤러는 주소를 알지 못한다.
- 첫 실기기 시도는 HTTP 403이었다. 같은 기기·같은 URL에서 OkHttp와 HttpURLConnection은 206인데 media3만 403이어서 요청 형태 차이를 좁혔다. 결과: 열린 Range 403, 닫힌 32B/64KB/512KB 206, 닫힌 1MB·4MB·전체 크기 403, 같은 URL 64KB 4회 반복 모두 206. 해석 URL을 매번 새로 받아도 같았다.
- 따라서 `ChunkedHttpDataSource`로 하나의 재생 요청을 512KB 이하 닫힌 Range 요청 여러 개로 나눈다. 첫 청크의 Content-Range에서 전체 길이를 얻어 상위에 알린다. `DefaultDataSource`의 base 소스로만 끼워 http(s)에만 적용하며 내장 음원 경로는 그대로다. 적용 후 실기기 원격 재생이 통과했다.
- 부수 변경: 네트워크 재생을 위해 wake mode를 `WAKE_MODE_NETWORK`로 바꿨다. `PlayableStream`에 일시적 `requestHeaders`를 추가해 해석에 쓴 클라이언트 User-Agent를 재생 요청에 함께 보낸다. 계약 검사의 Range 요청도 같은 헤더를 보내도록 맞췄다.
- 회귀 1건을 잡았다. 새 원격 재생 테스트가 서비스의 공유 대기열을 바꿔 뒤 순서의 `ScreenOffPlaybackTest`·`TestAudioPlaybackTest`가 실패했다. 새 테스트가 종료 시 내장 테스트 음원으로 되돌리도록 고쳐 14개 모두 통과했다.
- media3 데이터 소스 API는 UnstableApi다. Kotlin `@file:OptIn`으로는 lint의 UnsafeOptInUsageError가 해소되지 않아(오류 43 → 46) `@androidx.annotation.OptIn(markerClass = [UnstableApi::class])`을 클래스에 붙였다. baseline 생성이나 규칙 비활성화는 하지 않았다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --offline --continue --no-daemon --console=plain`: PASS, 종료 코드 0(2분 50초). 단위 36개·실제 계약 4개·실기기 계측 14개, 실패/오류 0. 린트 오류 0·경고 19(UseKtx 1건 증가). debug/release APK 생성.
- 신규 파일: `core/player/TrackUri.kt`, `StreamResolver.kt`, `TrackStreamResolver.kt`, `ChunkedHttpDataSource.kt`, `data/source/providerA/ProviderAMusicSource.kt`, `di/SourceModule.kt`, `app/src/test/.../TrackUriTest.kt`, `ProviderAMusicSourceTest.kt`, `app/src/androidTest/.../ChunkedHttpDataSourceTest.kt`, `RemoteTrackPlaybackTest.kt`. 변경: `MusicService.kt`, `PlayerConnection.kt`, `PlayerViewModel.kt`, `TestPlaybackScreen.kt`, `core/model/PlayableStream.kt`, `ProviderAStreamResolver.kt`, `ProviderAStreamSourceContractTest.kt`, `res/values/strings.xml`, `TASKS.md`, `README.md`, `docs/DECISIONS.md`, 이 기록. 결정 ADR-033, 신규 의존성 없음.
- 진단용으로 만든 기기 테스트는 결과를 ADR에 남기고 삭제했다. 대신 상위 소스를 흉내 낸 `ChunkedHttpDataSourceTest`로 청크 경계·시작 위치·명시 길이를 검증한다.
- 한계: 단일 트랙·단일 기기·WiFi 조건이다. 청크 상한 512KB는 관찰값이며 공급자가 바꾸면 조정해야 한다. 화면의 원격 재생 버튼과 고정 Track ID는 KM-058에서 대체한다. push는 하지 않았다.

## KM-058 완료 — 검색에서 재생까지 수직 슬라이스

- 브랜치 `codex/KM-058-search-to-play`. POC 화면에 검색어 입력·검색 버튼·결과 목록을 추가하고 결과를 탭하면 재생하도록 연결했다. 검색 상태는 Idle/Searching/Results/Empty/Failed 다섯 가지이며 원문 오류는 노출하지 않는다.
- 검색은 ViewModel이 MusicSource를 직접 호출하는 임시 구조다. KM-070 SearchRepository와 KM-071 SearchViewModel이 이 자리를 대체한다. 신규 의존성은 목록 표시를 위한 `androidx.compose.foundation` 하나이며 Compose BOM 버전을 따른다.
- KM-057에서 넣었던 고정 Track ID의 원격 재생 버튼과 문자열은 제거했다.
- **검증 중 결함 발견**: 수동 재생이 약 34초(첫 512KB 직후) 지점에서 403으로 멈췄다. KM-057의 실기기 검증이 10초 미만이라 첫 청크 안에서 끝나 이 결함을 놓쳤다.
- 진단(실기기, 매번 새 URL): 오디오 전용 adaptive 주소는 헤더 `Range: bytes=524288-1048575` 403, 쿼리 `range=524288-1048575` 403, 두 방식 혼합 403, 다음 청크 403이었다. 이후 재확인에서는 오프셋 0 요청도 403이 되었다. 같은 응답의 progressive 형식(video/mp4 306kbps)은 닫힌 Range·열린 Range·Range 없음·4MB 구간이 모두 200/206이었다.
- 조치: mapper가 progressive 형식만 재생 가능한 스트림으로 인정하고, 없으면 실패해 다음 클라이언트로 넘어간다. 후보 순서를 ANDROID 우선으로 바꿨다(progressive를 제공하는 유일한 종류). 영상 트랙은 ExoPlayer track selection에서 끈다. 필요 없어진 `ChunkedHttpDataSource`와 그 계측 검사를 제거했다. 결정 ADR-034, ADR-033은 해당 부분 대체 표시.
- 회귀 방지: 실기기 계측에 길이 - 60초 지점으로 탐색해 이어 재생하는 검사를 추가했다. 파일 앞부분만 받아도 통과하던 구멍을 막는다.
- 수동 검증: 앱 실행 → 검색어 "BTS Dynamite" 입력 → 검색 → 결과 10건 이상 표시 → 첫 결과 탭 → 재생 시작. 20초 14.8초, 45초 41.8초, 70초 66.9초, 100초 94.9초 위치로 연속 진행했다. Home 이동 후에도 115.2초 → 130.2초로 계속 재생했고 미디어 키 일시정지가 동작했다. 증거는 Git 제외된 `captures/km-058/`에 보관했다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --offline --continue --no-daemon --console=plain`: PASS, 종료 코드 0(4분 7초). 단위 38개·실제 계약 4개·실기기 계측 16개, 실패/오류 0. 린트 오류 0·경고 19. 계약 검사 요약은 ANDROID만 해석 성공(video/mp4, 306098bps)이고 나머지 네 종류는 실패다.
- 변경: `feature/player/PlayerViewModel.kt`, `TestPlaybackScreen.kt`, `data/source/providerA/mapper/ProviderAStreamMapper.kt`, `ProviderAConfig.kt`, `core/player/MusicService.kt`, `res/values/strings.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, 관련 단위/계측 테스트, `TASKS.md`, `README.md`, `docs/DECISIONS.md`, 이 기록. 삭제: `core/player/ChunkedHttpDataSource.kt`와 그 계측 검사.
- 한계와 대가: progressive는 영상이 포함된 다중화 스트림이라 오디오 전용보다 대역폭을 5~6배 쓴다. 단일 트랙·단일 기기·WiFi 조건이며 여러 곡 연속 재생, 재생 중 URL 만료, 네트워크 전환은 KM-061·KM-132·KM-136 대상이다. 공급자 접근 제한을 우회하는 수단은 도입하지 않았다. push는 하지 않았다.

## KM-134 완료 — 재생 스트리밍 캐시

- 브랜치 `codex/KM-134-streaming-cache`. Media3 `SimpleCache`(LRU, 256MB, `cacheDir/media`)를 재생 경로 가장 바깥에 두었다. 캐시 키가 `keuney://track/<id>` 자리표시 URI라 매번 달라지는 스트림 주소와 무관하게 재사용되고, 캐시에 있으면 해석 요청 자체를 보내지 않는다.
- 인수 조건 네 항목 모두 PASS. 영구 다운로드가 아님은 위치(cacheDir)·LRU 자동 삭제·DownloadManager 미사용으로 확인했다.
- 검증 중 두 가지를 고쳤다. 첫째, `SimpleCache`는 한 디렉터리를 프로세스에서 하나만 열 수 있는데 계측 테스트마다 주입 그래프가 다시 만들어져 두 번째 생성이 실패했다. 프로세스 단위 보관으로 바꿨다. 둘째, 저장 확정 조각이 기본 5MB라 3초만 듣고 멈추면 캐시에 아무것도 남지 않았다. 1MB로 줄였고, 짧은 청취도 남도록 하는 실제 개선이다.
- 계측 3개를 추가했다. 재생한 구간이 상위 소스 없이 캐시만으로 읽히는지, 비우면 더 이상 읽히지 않는지, 기본 상한과 저장 위치가 맞는지 확인한다.
- 신규 의존성 `androidx.media3:media3-database`(StandaloneDatabaseProvider). 결정 ADR-035.

## KM-137 완료 — 네트워크 사용 정책(WiFi 전용 재생)

- 브랜치 `codex/KM-134-streaming-cache`에서 이어 진행했다. 백로그에 없던 작업이라 M8에 KM-137을 새로 추가했다. progressive 전환으로 곡당 8~25MB가 되어 데이터 통제 수단이 필요하다는 판단이다(ADR-034).
- `SettingsRepository`에 `wifiOnlyPlayback`을 추가하고 DataStore에 저장한다. 기본값은 꺼짐이라 기존 동작이 바뀌지 않는다.
- `NetworkPolicy`가 설정과 `isActiveNetworkMetered`를 함께 보고 판단한다. 연결 정보를 얻지 못하면 막지 않는다. 차단은 `TrackStreamResolver`에서 주소 해석 전에 일어나므로, 캐시에 있는 구간은 판단을 거치지 않고 그대로 재생된다. 제한을 켜도 들었던 곡은 계속 들린다.
- 화면에 스위치와 차단 안내 문구를 넣었다. 원문 오류는 노출하지 않는다.
- `NetworkPolicy`는 연결 확인을 람다로 받아 상속 없이 검사할 수 있다. 단위 3개(설정·연결 조합), 계측 3개(차단 시 공급자 미호출, 비측정 연결에서 해석, 자리표시가 아닌 요청 통과)를 추가했다.
- 검증 중 회귀 1건을 잡았다. 재생 경로가 설정을 읽게 되면서 계측 테스트마다 DataStore가 다시 생성돼 "multiple DataStores active for the same file"로 실패했다. 캐시와 같은 이유이며 DataStore도 프로세스 단위 보관으로 바꿨다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --offline --continue --no-daemon --console=plain`: PASS, 종료 코드 0(4분 5초). 단위 41개·실제 계약 4개·실기기 계측 22개, 실패/오류 0. 린트 오류 0·경고 19.
- 결정 ADR-036. 설정 UI는 KM-153 Settings로 옮긴다. 실제 측정 요금제(모바일 데이터) 기기 검증은 아직 하지 않았고 계측은 연결 확인을 대체한 검사다. push는 하지 않았다.

## KM-059 완료 — Provider A Gate PASS

- 브랜치 `codex/KM-059-provider-a-gate`. 검색어 5종에서 상위 결과를 모아 중복 없는 10곡을 구성했다. 서로 다른 아티스트 10종, 가장 긴 곡 4883초(81분)로 요구 조건(복수 아티스트, 긴 곡 1개 이상)을 채웠다.
- 소스 판정은 `sourceContractTest`에 `ProviderAGateSourceContractTest`를 추가해 수행했다. 해석 10/10, 파일 중간 지점 구간 요청 10/10. 앞부분만 받아도 통과하던 구멍을 막기 위해 먼저 앞부분 응답의 Content-Range로 전체 길이를 얻고 그 절반 지점을 요청한다. 파일 끝을 넘는 요청과 실제 거부를 구분한다.
- 첫 실행에서 1곡이 실패했는데 HTTP 상태가 아니라 전송 예외였다. 응답 코드를 그대로 기록하도록 고치고 재실행하니 206으로 통과했다. 일시적 네트워크 오류이며 공급자의 구조적 거부와 구분해 기록했다.
- 기기 판정은 `ProviderAGateDeviceTest` 2개다. 두 곡을 대기열에 넣고 첫 곡 끝으로 이동해 자동 전환과 다음 곡 재생을 확인했고, 81분 트랙을 종료 2분 전 지점으로 이동해 이어 재생을 확인했다.
- Gate 검증을 위해 `PlayerConnection.playQueue`와 `currentMediaId`를 추가했다. 대기열에도 Track ID와 metadata만 넣는다. 큐 UI와 이전/다음 조작은 KM-094·KM-097 범위이며 여기서는 구성과 자동 전환만 다룬다.
- 판정 결과와 곡별 표, 채택 조건, 재판정 기준을 `docs/SOURCE_PROVIDER.md`에 기록했다. 결정은 ADR-037이며 ARCHITECTURE의 ADR-004 미확정 상태를 닫았다. KM-064 Provider B 평가는 활성화하지 않는다.
- 채택 조건으로 남긴 제약: 오디오 전용 스트림 불가로 progressive만 사용하며 대역폭이 약 3배다. 완화는 KM-134 캐시와 KM-137 WiFi 전용 재생이다. 공급자 설정은 관찰값이라 `sourceContractTest` 유지가 채택의 전제다. 접근 제한 우회 수단은 도입하지 않는다.
- 한계: WiFi 연결의 단일 기기·단일 지역 기준이다. 다른 지역, 측정 요금제, 다른 OEM은 별도 검증 대상이다. push는 하지 않았다.

## KM-060 완료 — 소스 오류 매핑

- 브랜치 `codex/KM-060-source-error-mapping`. `data/source/SourceError.kt`에 `SourceFailure`(Network/Parse/NotFound/Restricted/Unknown)와 `Throwable.toAppError()`를 두고, 공급자 예외들이 자기 분류를 들고 다니도록 `SourceFailureAware`를 구현하게 했다.
- 분류 기준: 연결 실패·타임아웃·5xx·429·408 → Network, 응답 구조 변경·직렬화 실패 → Parse, 404·410·재생 불가 상태 → NotFound, 401·403·로그인/연령/콘텐츠 확인 요구 → Restricted, 나머지 → Unknown. 공급자의 재생 가능 상태는 상태 문자열만 보고 판단하며 응답 원문을 읽지 않는다.
- 판단 하나를 기록해 둔다. Restricted를 `AppError.GeoRestricted`로 보내지 않았다. PRD의 GeoRestricted는 지역 제한을 뜻하는데 로그인·연령 제한에 그 이름을 붙이면 사용자에게 틀린 이유를 보여준다. Restricted와 NotFound 모두 PlaybackUnavailable로 보낸다. 그 결과 GeoRestricted는 현재 생성되는 곳이 없으며, 응답 문구를 문자열로 판별하는 방식은 쓰지 않기로 했다(ADR-038).
- 검색 실패 상태가 `AppError`를 함께 들고 다니도록 바꾸고 화면에 다섯 분류별 문구를 넣었다.
- 단위 검사 12개 추가: 분류별 AppError 매핑 전수 확인, HTTP 상태 분류, 인프라 예외 분류, 원문 메시지 비노출, 공급자 재생 가능 상태별 분류, 검색 실패 분류, 미구현 연산 분류.
- 검증 중 KM-059 Gate 검사가 간헐적으로 실패했다. 로그를 보니 공급자의 거부(403)가 아니라 특정 트랙의 SocketTimeoutException이었고, 재시도 후에도 재현됐다가 다음 실행에서는 통과했다. 계약 위반과 전송 지연을 구분하도록 판정을 나눴다. 공급자 거부는 0건만 허용하고, 전송 지연은 1회 재시도 후에도 남는 건수를 1건까지 허용하며 결과에 그대로 기록한다.
- `BackgroundPlaybackTest`도 전체 실행 중 한 번 실패했으나 단독 실행에서 통과했다. 앞선 실행이 중간에 끊긴 영향으로 보이며 코드 변경과 무관하다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --offline --continue --no-daemon --console=plain`: PASS, 종료 코드 0(4분 10초). 단위 53개·실제 계약 5개·실기기 계측 24개, 실패/오류 0. 린트 오류 0·경고 19. Gate 판정은 해석 10/10, 중간 구간 10/10(공급자 거부 0, 전송 지연 0).
- 결정 ADR-038. 신규 의존성 없음. 재생 중 실패의 재해석과 재시도는 KM-061, 타임아웃 값 정책은 KM-062다.

## KM-061 완료 — 재생 중 스트림 재해석과 1회 재시도

- 브랜치 `codex/KM-061-stream-refresh`. `RefreshingDataSource`를 캐시 안쪽·해석 바깥쪽에 넣었다. 캐시 적중은 재시도 경로를 타지 않고, 재시도는 항상 새 주소로 이뤄진다.
- 상위 소스를 새로 여는 것이 곧 재해석이다. `ResolvingDataSource`가 열 때마다 주소를 해석하고 앱이 해석 결과를 보관하지 않기 때문에 다시 열면 새 주소가 나온다.
- 읽던 위치부터 이어 연다. `DataSpec.subrange(읽은 바이트)`로 재요청하므로 처음부터 다시 받지 않는다. 계측에서 이어받은 내용이 원본과 바이트 단위로 일치하는지 확인한다.
- 재시도는 열기 한 번당 1회다. 이어 연 뒤의 실패는 그대로 올려 보내 종점 오류가 된다. 계측으로 상위 소스 생성 횟수가 2를 넘지 않음을 확인해 무한 재시도가 없음을 보인다.
- 열기 실패와 읽기 도중 실패를 모두 다룬다. ExoPlayer의 기본 정책은 403 같은 응답을 재시도 대상으로 보지 않아, 그대로 두면 재생 중 만료가 곧 종점 오류였다.
- `PlayableStream.expiresAt`은 여전히 읽는 곳이 없다. 열 때마다 새로 해석하고 만료는 실패로 드러나 재시도로 처리되므로 시각을 미리 비교할 필요가 없다. 값은 PRD 모델 정의를 따라 유지하며 이 판단을 ADR-039에 남겼다.
- 계측 5개 추가: 열기 실패 후 재해석, 읽기 실패 후 이어받기와 내용 일치, 열기 두 번째 실패의 종점 처리, 읽기 두 번째 실패의 종점 처리, 정상 스트림은 다시 열지 않음. 네트워크 없이 상위 소스를 흉내 내 결정적으로 검사한다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --offline --continue --no-daemon --console=plain`: PASS, 종료 코드 0(4분 53초). 단위 53개·실제 계약 5개·실기기 계측 29개, 실패/오류 0. 린트 오류 0·경고 19.
- 결정 ADR-039. 신규 의존성 없음. 한계: 실제 만료 시각까지 기다리는 장시간 재생은 검증하지 않았고 KM-136의 30분 연속 재생 검사 대상이다. 타임아웃 값 정책은 KM-062다. push는 하지 않았다.

## KM-062 완료 — 네트워크 대기와 취소 정책

- 브랜치 `codex/KM-062-timeout-policy`. 흩어져 있던 대기 값을 `data/network/NetworkTimeouts`로 모으고 요청 성격에 따라 나눴다. 메타데이터 요청은 연결 10초·바이트 대기 20초·요청 전체 30초, 재생 요청은 연결 10초·바이트 대기 20초다.
- 재생 쪽 바이트 대기를 media3 기본 8초에서 20초로 늘렸다. KM-059·KM-060 검증에서 공급자가 전송을 늦게 시작해 SocketTimeoutException이 나는 것을 실제로 관찰했고, 기본값이면 그런 곡은 곧바로 끊긴다. 다만 무한정 늘리지는 않는다. 스로틀링으로 전송이 사실상 멈춘 경우까지 기다리면 사용자는 멈춘 화면을 보게 된다.
- 상한을 넘긴 실패는 KM-061의 재해석·1회 재시도로 이어지고, 그래도 실패하면 KM-060의 Network 오류로 표시된다. 세 작업이 한 경로로 이어진다.
- 취소는 상한과 무관하게 즉시 존중한다. 검색과 주소 해석 모두 `CancellationException`을 실패 값으로 삼키지 않고 전파하며, 취소된 요청이 30초 상한을 기다리지 않는 것을 검사로 확인했다.
- 검사를 위해 `createMusicHttpClient`가 대기 값을 인자로 받게 했다. 기본값은 정책 그대로이고 검사에서만 짧은 값을 넣는다. 상한을 실제로 기다리는 느린 검사를 만들지 않았다.
- 단위 검사 5개 추가: 기본값이 정책과 일치, 값 사이의 대소 관계, 상한 초과 시 Network 오류, 상한 안의 응답 성공, 취소 시 즉시 중단.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --offline --continue --no-daemon --console=plain`: PASS, 종료 코드 0(5분 2초). 단위 58개·실제 계약 5개·실기기 계측 29개, 실패/오류 0. 린트 오류 0·경고 19. Gate 판정은 해석 10/10, 중간 구간 10/10(공급자 거부 0, 전송 지연 0).
- 결정 ADR-040. 신규 의존성 없음. 한계: 늘린 재생 대기 값이 실제 스로틀링 상황에서 충분한지는 KM-136의 30분 연속 재생에서 다시 본다. push는 하지 않았다.

## KM-063 완료 — 소스 계약 검사 스위트

- 브랜치 `codex/KM-063-contract-suite`. 인수 조건 네 항목은 이미 갖춰져 있었다(검색·해석 계약, 별도 Gradle 작업 `sourceContractTest`, 일반 `test`에서 분리). 그래서 이번 작업은 회귀 감지의 구멍을 메우는 데 썼다.
- 구멍 1 — 검색 계약이 얕았다. ID와 제목만 확인해 아티스트·길이·이미지가 응답에서 사라져도 통과했다. mapper가 이 값들을 nullable로 다루므로 화면 품질만 조용히 떨어진다. 결과의 절반 이상에 존재하는지로 필드 계약을 추가했다. 중복 ID 검사도 넣었다.
- 구멍 2 — 스트림 계약이 파일 앞부분(`bytes=0-31`)만 요청했다. "앞부분만 되는 주소"를 통과시키는 구조이며 실제로 그 결함을 놓친 적이 있다(ADR-034). Content-Range로 전체 길이를 얻어 절반 지점을 요청하도록 바꿔 Gate와 기준을 맞췄다.
- 구멍 3 — 오류 경로 계약이 없었다. 단위 검사는 우리가 만든 응답으로만 분류를 검증하므로, 공급자가 실패를 알리는 방식을 바꾸면 단위는 통과하면서 사용자에게 엉뚱한 문구가 보인다. 없는 트랙과 결과 없는 검색어로 계약 2개를 추가했다.
- 실행 결과 없는 트랙은 NotFound → PlaybackUnavailable로 분류돼 KM-060 매핑이 실제 공급자 동작과 일치함을 확인했다. 결과 없는 검색어는 실패가 아닌 성공으로 처리된다.
- 계약 7개 전부 통과(검색 3·스트림 1·오류 2·Gate 1). README의 외부 소스 검증 섹션에 각 검사가 지키는 것과 깨졌을 때의 증상을 표로 정리했다.
- 함께 고친 것: 계측 4개(`MusicServiceTest`, `MusicSessionTest`, `PlayerConnectionTest`, `TestAudioPlaybackTest`)가 `@AndroidEntryPoint`인 MusicService에 연결하면서 `HiltAndroidRule`이 없었다. 앞선 테스트가 컴포넌트를 정리하면 "The component was not created"로 실패하는 구조라 순서에 따라 간헐적으로 깨졌다. 네 곳에 규칙을 추가했다. 앞서 두 차례 관찰한 전체 실행 중 단발 실패의 원인이 이것이다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --offline --continue --no-daemon --console=plain`: PASS, 종료 코드 0(4분 17초). 단위 58개·실제 계약 7개·실기기 계측 29개, 실패/오류 0. 린트 오류 0·경고 19.
- 결정 ADR-041. 신규 의존성 없음. M4 Source Hardening 완료. push는 하지 않았다.

## KM-070 완료 — SearchRepository 경계

- 브랜치 `codex/KM-070-search-repository`. 인터페이스 `core/search/SearchRepository`, 구현 `data/repository/SearchRepositoryImpl`, 바인딩 `di/RepositoryModule`을 추가했다. ARCHITECTURE 6의 서명과 4의 트리를 그대로 따랐다.
- 이 경계의 실익은 실패 표현을 바꾸는 데 있다. 이전에는 `PlayerViewModel`이 `data.source.toAppError`를 직접 불러 화면 계층이 데이터 계층 함수에 의존했다. 이제 repository가 모든 실패를 `AppErrorException`으로 감싸 돌려주므로 화면은 `AppError`만 안다.
- `Result<List<Track>>` 서명은 ARCHITECTURE와 동일하게 유지했다. Result의 실패는 Throwable이어야 하므로 도메인 오류를 실어 나를 얇은 예외 타입 `AppErrorException`을 core/model에 뒀다. 원문 예외와 메시지는 담지 않는다.
- repository를 얇게 유지했다. 검색어 정리와 빈 검색어 처리는 이미 공급자 구현에 있고 옮기면 같은 규칙이 두 곳에 생긴다. 지금 책임은 위임과 오류 변환뿐이며, 로컬/원격 조합과 캐시 전략은 필요해질 때 붙인다.
- 취소는 실패로 바꾸지 않고 전파한다. 취소를 오류로 만들면 화면이 실패 문구를 띄우고 불필요한 재시도를 유도한다.
- 단위 6개 추가: 성공 결과 그대로 전달과 검색어 전달, 다섯 분류의 도메인 오류 매핑, 인프라 예외 매핑, 던져진 예외도 감싸기, 원문 메시지 비노출, 취소 전파.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --offline --continue --no-daemon --console=plain`: PASS, 종료 코드 0(4분 51초). 단위 64개·실제 계약 7개·실기기 계측 29개, 실패/오류 0. 린트 오류 0·경고 19.
- 결정 ADR-042. 신규 의존성 없음. SearchViewModel 분리는 KM-071, 화면 분리는 KM-072다. push는 하지 않았다.

## KM-071 완료 — SearchViewModel 분리

- 브랜치 `codex/KM-071-search-viewmodel`. 검색을 `PlayerViewModel`에서 떼어 `feature/search/SearchViewModel`로 옮겼다. 상태 이름을 TASKS 정의에 맞춰 Idle/Loading/Success/Empty/Error로 바꾸고 `StateFlow`로만 노출한다.
- 분리의 실익은 검사 가능성이다. `PlayerViewModel`은 `PlayerConnection`을 통해 Android Handler/Looper에 묶여 있어 ViewModel 검사를 전부 계측으로 돌려야 했다. 검색만 떼면 재생 의존성이 사라져 일반 단위 검사로 확인할 수 있고 기기 없이 빠르게 돈다.
- `kotlinx-coroutines-test`를 테스트 전용으로 추가했다. `viewModelScope`가 `Dispatchers.Main`을 쓰므로 검사에서 대체해야 한다. coroutines와 같은 버전이며 앱 산출물에는 들어가지 않는다. 오프라인 캐시에 없어 이 의존성 한 번은 네트워크로 받았다.
- 단위 9개 추가: 초기 Idle, Loading을 거쳐 Success, 결과 없음은 Empty, 실패는 도메인 오류를 실은 Error, 예상 밖 실패는 Unknown, 빈 검색어는 repository 미호출, 검색어 trim, 늦게 도착한 이전 결과가 새 결과를 덮지 않음, clear로 Idle 복귀.
- 마지막 항목이 실제 동작 결정이다. 새 검색이 이전 검색을 취소하지 않으면 늦게 온 이전 결과가 화면을 덮는다. 취소를 넣고 검사로 고정했다.
- 검색 상태 전이 검사를 계측에서 단위로 옮겼다. 계측에는 실제 검색 → 선택 → 재생 → Home 유지 end-to-end 하나만 남겼다. 같은 것을 두 곳에서 검사하지 않는다. 계측 29개 → 26개.
- 화면은 아직 POC 하나이며 두 ViewModel을 함께 받는다. MainActivity가 둘 다 만들어 전달한다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --offline --continue --no-daemon --console=plain`: PASS, 종료 코드 0(6분 20초). 단위 73개·실제 계약 7개·실기기 계측 26개, 실패/오류 0. 린트 오류 0·경고 19.
- 결정 ADR-043. 검색 화면 분리는 KM-072, 결과 목록 추출은 KM-073이다. push는 하지 않았다.

## KM-072 완료 — SearchScreen

- 브랜치 `codex/KM-072-search-screen`. 검색어 입력과 결과 표시를 `feature/search/SearchScreen`으로 옮겼다. `TestPlaybackScreen`에는 재생 제어만 남고 검색 화면을 자리만 잡아 넣는다.
- 착수 전 미결 사항이었던 배치 문제는 사용자와 합의했다. 화면을 컴포저블로만 분리하고 배치는 현재대로 둔다. 내비게이션으로 실제 두 화면을 만드는 것은 KM-150 범위다. 백로그 순서를 지키고 되돌리기 쉬운 쪽이다.
- 검색 화면은 `SearchViewModel`만 알고 `onSelect` 콜백으로 선택을 밖에 넘긴다. 재생 의존성이 검색 화면에 들어가지 않는다.
- 검색어는 화면의 `rememberSaveable` 상태다. 검색어를 비우면 `clear()`로 이전 결과도 치운다. 빈 입력창 아래 옛 결과가 남지 않게 한다.
- 키보드에 `ImeAction.Search`를 붙였다. 검색 버튼과 같은 동작이며 버튼은 그대로 둔다.
- `분:초` 표기를 `ui/format/formatDuration`으로 합쳤다. 이전에는 재생 위치·곡 길이와 결과 목록의 길이가 같은 규칙을 각각 계산했다. 단위 6개 추가: 0, 10초 미만 두 자리 유지, 초 절삭, 60초 분 올림, 한 시간 초과, 음수는 0.
- 결과 목록은 옮기기만 했다. 앨범 이미지를 포함한 항목 구성은 KM-073이다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(4분 38초). 단위 79개·실제 계약 7개·실기기 계측 26개, 실패/오류 0. 린트 오류 0·경고 20(신규 파일에서 발생한 경고 없음).
- 실기기 SM-T220 / Android 14에서 다섯 상태를 눈으로 확인했다. Idle(결과 없음), 입력 후 검색 버튼 → Loading(진행 표시와 "검색 중"), Success(제목·아티스트·길이 18:24/3:42/5:07), 검색어 삭제 → Idle 복귀와 버튼 비활성, 없는 검색어를 키보드 검색 키로 실행 → Empty("결과가 없습니다."), WiFi를 끈 뒤 검색 → Error(오류 색 "네트워크에 연결할 수 없습니다."). 확인 후 WiFi를 되돌려 연결을 확인했다. 화면 캡처는 captures/km-072에 보관했다(저장소 추적 대상 아님).
- 결정 ADR-044. 신규 의존성 없음. 결과 목록 추출은 KM-073, 최근 검색어 저장은 KM-074다. push는 하지 않았다.

## KM-073 완료 — SearchResultList

- 브랜치 `codex/KM-073-search-result-list`. 결과 항목을 `feature/search/SearchResultList`로 빼고 앨범 이미지·제목·아티스트·아는 경우의 길이를 그린다. `SearchScreen`의 Success 분기는 이 목록을 부른다.
- 함정: `coil-compose`만으로는 원격 이미지가 그려지지 않는다. Coil 3은 네트워크 fetcher를 별도 산출물로 옮겼고, 없으면 오류 없이 자리표시자만 남는다. `io.coil-kt.coil3:coil-network-okhttp`를 추가해 해결했다. OkHttp는 `ktor-client-okhttp`로 이미 classpath에 있어 실제 추가분은 Coil의 얇은 fetcher뿐이다. 별도 `ImageLoader` 등록 코드는 필요하지 않았다.
- `Track.artworkUrl`은 이전부터 검색 mapper가 채우고 있었고 `ui/components/Artwork`도 이미 있었으나 화면에서 쓰이지 않았다. 이번에 연결했다.
- 이미지는 56dp 정사각형으로 자르고 모서리를 둥글렸다. 섬네일은 16:9라 좌우가 잘린다. 목록에서는 이 편이 줄 높이가 일정하다.
- 제목 두 줄, 부제 한 줄로 제한하고 넘치면 줄인다. 실제 결과에 아주 긴 제목이 있어 제한이 없으면 한 항목이 화면을 다 차지한다.
- 부제 조립을 `trackSubtitle`로 떼어 단위 6개 추가: 아티스트와 길이 결합, 길이 없음, 아티스트 없음, 공백 아티스트를 없음으로 취급, 둘 다 없으면 빈 문자열, 한 시간 초과 길이.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 42초). 단위 85개·실제 계약 7개·실기기 계측 26개, 실패/오류 0. 린트 오류 0·경고 21(신규 하나는 새 의존성 줄의 버전 안내이며 새 코드에서 발생한 경고는 없다).
- 실기기 SM-T220 / Android 14에서 확인했다. "iu" 검색 결과에 실제 섬네일이 그려지고 제목·아티스트·길이(18:24, 3:42)가 나온다. 둘째 항목을 눌러 재생이 시작되는 것까지 확인했다(0:09 → 1:02, 3:41). 확인 후 일시정지하고, 검증 중 켜진 WiFi 전용 설정을 원래대로 껐다. 화면 캡처는 captures/km-073에 보관했다(저장소 추적 대상 아님).
- 결정 ADR-045. 최근 검색어 저장은 KM-074다.

## KM-074 완료 — Search history

- 브랜치 `codex/KM-074-search-history`. 최근 검색어를 `core/search/SearchHistoryRepository` 뒤에 두고 구현은 설정 DataStore를 쓰는 `data/repository/SearchHistoryRepositoryImpl`에 뒀다. 화면은 저장 수단을 알지 못한다.
- 저장 위치를 Room이 아니라 DataStore로 정했다. 짧은 문자열 목록이고 조회·정렬이 필요하지 않으며, Room을 쓰면 자리표시자 엔티티만 있는 스키마를 1에서 2로 올려야 한다. KM-110의 `SearchHistoryEntity`는 이 결정에 따라 필요하지 않으므로 KM-110 착수 시 다시 판단한다. 재개 지점에 남겼다.
- Preferences에 순서를 지키는 목록 타입이 없어 JSON 배열 한 값으로 저장한다. 문자열 집합을 쓰면 최신 순서를 잃는다. 신규 의존성 없음.
- 오류 없이 끝난 검색만 남긴다. 결과가 없는 검색도 성공한 검색이므로 남기고, 실패한 검색은 남기지 않는다. 저장 호출은 검색 작업과 분리해 사용자가 곧바로 다음 검색을 시작해도 이미 성공한 검색이 남게 했다.
- 목록은 Idle일 때만 보여준다. 한 화면에 검색과 재생이 함께 있어 자리가 넉넉하지 않다. 검색어를 비우면 Idle로 돌아오므로 목록과 지우기 버튼에 언제든 닿는다.
- 단위 15개 추가. 저장소 9개: 초기 빈 목록, 최신 우선, 같은 검색어 재검색 시 중복 없이 앞으로, trim과 빈 검색어 무시, 상한 10 초과 시 가장 오래된 것 제거, 지우기, 저장소 재개방 후 유지, 지우기의 재개방 후 유지, 깨진 값을 목록 없음으로 취급하고 그 위에 새로 남기기. ViewModel 6개: 성공 검색 저장(정리된 검색어), 결과 없는 검색도 저장, 실패 검색 미저장, 빈 검색어 미저장, 목록 노출, 지우기 위임.
- 재개방 검사가 인수 조건의 앱 재시작 유지를 일반 단위 검사로 덮는다. 기존 테마 검사와 같은 방식이다.
- `SearchViewModel` 생성자가 하나 늘어 계측 `SearchToPlayTest`도 함께 고쳤다. 이 계측 검사는 실제 검색을 성공시키므로 기기 최근 검색어에 그 검색어가 남는다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(4분 59초). 단위 100개·실제 계약 7개·실기기 계측 26개, 실패/오류 0. 린트 오류 0·경고 21(새 코드에서 발생한 경고 없음).
- 실기기 SM-T220 / Android 14에서 인수 조건 3개를 확인했다. "iu" 검색 후 검색어를 비우면 "최근 검색어"에 iu 칩이 나오고, "bts"를 더 검색하면 bts가 앞에 온다(x=43 대 x=127). 앱을 force-stop하고 다시 켜도 두 칩이 남는다. 칩을 누르면 그 검색어로 다시 검색된다. "지우기"를 누르면 목록이 사라지고 재시작 후에도 비어 있다. 화면 캡처와 UI 덤프는 captures/km-074에 보관했다(저장소 추적 대상 아님).
- 함정: 기기 화면 타임아웃이 30초라 여러 단계를 이어 검증할 수 없었다. 검증 동안만 600000으로 올리고 끝난 뒤 30000으로 되돌렸다. 또한 검색 입력창에 포커스가 없는 상태에서 `input keyevent 66`을 보내면 그 키가 포커스를 가진 다른 위젯으로 가서 WiFi 전용 스위치가 켜지는 일이 있었다. 좌표는 매번 `uiautomator dump`로 확인하고, 검색 실행은 ENTER 대신 검색 버튼 좌표를 눌러 검증했다.
- 결정 ADR-046. M5 검색 완료. 다음은 M6 Player UX의 KM-090이다.

## KM-090 완료 — Player UI state adapter

- 브랜치 `codex/KM-090-player-ui-state`. `core/player/PlaybackState`에 현재 곡·반복·셔플을 더하고 `playing`·`buffering`을 이름 붙여 드러냈다. `PlayerConnection.updatePlayback`이 MediaController에서 이 값들을 함께 읽는다.
- TASKS의 `PlayerUiState`를 새 타입으로 만들지 않았다. 이미 있는 `PlaybackState`가 ARCHITECTURE 19의 매퍼 결과이고, 같은 것을 가리키는 상태 타입이 둘이면 화면이 어느 쪽을 봐야 하는지가 흐려진다. 이름은 ARCHITECTURE 4의 `PlaybackStateMapper.kt`에 맞춰 그대로 뒀다.
- 현재 곡은 `NowPlaying(mediaId, title, artist)`다. `Track`을 재구성하지 않는다. 세션에서 오는 것은 대기열에 넣은 metadata뿐이라 `source`나 길이를 복원할 수 없고, 없는 값을 지어내면 화면이 사실이 아닌 것을 믿는다.
- 앨범 이미지는 아직 대기열 항목에 넣지 않는다. 넣으면 알림 이미지 동작이 바뀌어 KM-037·038 검증을 다시 해야 하므로, 이미지가 실제로 필요해지는 KM-091·092에서 함께 다룬다.
- `playing`·`buffering`은 `phase`에서 읽는 계산 속성이다. 같은 사실을 두 곳에 두지 않는다.
- 반복은 `RepeatMode(Off/One/All)`이며 알 수 없는 상수는 꺼짐으로 본다. 반복을 켠 것으로 잘못 보는 쪽이 더 나쁘다.
- 매퍼 새 인자는 기본값을 주어 뒤에 붙였다. 기존 호출과 검사가 그대로 컴파일된다.
- 단위 8개 추가: phase에서 나오는 playing/buffering, 반복 세 가지와 알 수 없는 값, 셔플 양방향, 기본 상태에 곡·반복·셔플 없음, 대기열 항목이 현재 곡이 됨, 빈 ID/빈 문자열/공백은 곡 없음, 제목·아티스트 없음은 빈 문자열, 세 값이 상태까지 도달.
- 계측 1개 추가(`PlaybackStateAdapterTest`): 실제 세션 값이 화면 상태로 오는지 확인한다. 연결 전 곡 없음 → 연결 후 내장 테스트 음원이 현재 곡(제목·아티스트 비어 있지 않음) → 반복 꺼짐·셔플 꺼짐 → 재생 시 isPlaying이고 isBuffering이 아니며 길이 120초 → 일시정지에도 위치와 현재 곡 유지 → 연결 해제 시 곡 정보 사라짐. 계측 26개 → 27개.
- 반복·셔플을 켠 상태는 실기기에서 만들 수 없다. 조작이 KM-095·096 소속이라 설정 함수를 만들지 않았기 때문이다. 세션 기본값까지만 기기에서 확인하고 켠 상태의 매핑은 단위 검사로 고정했다.
- 새 필드를 쓰는 화면은 아직 없다. 소비는 KM-091 Mini Player부터다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 3초). 단위 108개·실제 계약 7개·실기기 계측 27개, 실패/오류 0. 린트 오류 0·경고 21(새 코드에서 발생한 경고 없음).
- 결정 ADR-047. 신규 의존성 없음. 다음은 KM-091 Mini Player다.

## KM-091 완료 — Mini Player

- 브랜치 `codex/KM-091-mini-player`. `feature/player/MiniPlayer`를 추가하고 현재 곡의 앨범 이미지·제목·아티스트와 재생·일시정지를 한 줄로 보여준다. 배치는 KM-072 제약대로 지금 화면 안이다.
- 사용자 결정에 따라 대기열 항목에 앨범 이미지 주소를 넣었다. `playTrack`이 `artworkUri`를 받아 `MediaMetadata.setArtworkUri`로 세션에 넣고, 상태로 다시 읽는다. `https`로 시작하는 주소만 넣는다.
- 세션에 넣은 결과로 알림·잠금화면 이미지가 자리표시자에서 실제 이미지로 바뀐다. KM-037·038 재검증을 함께 했다.
- 미니 플레이어가 재생·일시정지를 들고 있으므로 화면의 독립 재생 버튼을 없앴다. 현재 곡이 없을 때와 연결이 끊겼을 때만 단독 버튼을 둔다. 같은 버튼을 두 곳에 두지 않는다.
- 눌러서 Now Playing으로 가는 동작은 넣지 않았다. 내비게이션은 KM-150, 목적 화면은 KM-092다. 아무 일도 하지 않는 탭 영역을 미리 만들지 않는다. 백로그 순서를 지키기로 사용자와 합의했다.
- 내장 테스트 음원은 `artworkData`만 있고 `artworkUri`가 없어 미니 플레이어에서는 자리표시자 색으로 보인다. 알림은 기존대로 그 데이터를 쓴다.
- `playQueue`는 이미지 주소를 받지 않는다. Gate 검증용 진입점이고 서명이 `Triple`이라 타입을 바꿔야 한다. 제품 경로는 `playTrack`이다.
- 단위 2개 추가: 이미지 주소가 현재 곡까지 도달, 없거나 빈 주소는 이미지 없음.
- 계측 1개 추가(`NowPlayingArtworkTest`): 실제 검색 결과에서 이미지 주소가 있는 곡을 골라 재생하고, 상태에 주소가 그대로 돌아오는지와 알림에 제목·아티스트·PendingIntent·실제 큰 아이콘이 실리는지 확인한다. 이미지 CDN이 필요한 검사다. 계측 27개 → 28개.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(4분 32초). 단위 110개·실제 계약 7개·실기기 계측 28개, 실패/오류 0. 린트 오류 0·경고 22.
- 린트 경고가 21개에서 22개로 늘었다. 새로 쓴 `Uri.parse`에 대한 `UseKtx`이며 기존 두 건과 같은 종류다. androidx.core-ktx 의존성이 없어 고칠 수 없고, 이 한 줄 때문에 의존성을 넣지 않는다.
- 실기기 SM-T220 / Android 14 눈 확인: 미니 플레이어에 실제 섬네일과 제목 "[MV] IU(아이유) _ Blueming(블루밍)"·아티스트 "1theK (원더케이)"·일시정지 버튼이 나오고 독립 재생 버튼은 없다. 알림 패널의 미디어 카드에 실제 이미지와 제목·아티스트·진행(00:32/03:41)·이전·일시정지가 표시된다. 잠금화면(keyguard showing=true)에도 같은 카드가 나오고, 카드의 일시정지를 누르면 세션이 PLAYING(3) → PAUSED(2), 다시 누르면 PAUSED(2) → PLAYING(3)으로 왕복한다. 확인 후 일시정지했다. 화면 캡처는 captures/km-091에 보관했다(저장소 추적 대상 아님).
- 함정: 삼성 잠금화면의 미디어 카드는 `uiautomator dump`의 접근성 트리에 나오지 않는다. 스크린샷 좌표로 눌러야 하며, 상태 변화는 `dumpsys media_session`의 `state=`로 확인한다.
- 검증 동안 화면 타임아웃을 600000으로 올리고 끝난 뒤 30000으로 되돌렸다.
- 결정 ADR-048. 신규 의존성 없음. 다음은 KM-092 Now Playing screen이다.
