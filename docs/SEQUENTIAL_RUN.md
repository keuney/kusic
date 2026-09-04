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

## KM-150 완료 — App navigation (백로그 순서 앞당김)

- 브랜치 `codex/KM-150-app-navigation`. 사용자 요청으로 M9의 KM-150을 앞당겼다. KM-092를 하려면 갈 수 있는 화면이 있어야 한다. KM-072·091에서 미뤄 둔 화면 분리를 여기서 했다.
- `navigation/Destinations.kt`와 `navigation/KeuneyNavHost.kt`를 추가했다. 하단 내비게이션 홈·검색·라이브러리에 전체 화면 목적지 `now-playing`이 붙는다. 시작 목적지는 검색이다.
- 신규 의존성 둘: `androidx.navigation:navigation-compose` 2.10.0(안정판)과 `androidx.compose.material:material-icons-core`(Compose BOM 관리). 아이콘은 `NavigationBarItem`의 필수 인자이며 Home·Search·List가 core 집합에 있어 extended는 쓰지 않았다. icons-core는 1.7.8에서 멈춘 산출물이지만 현재 BOM이 관리하는 안정판이다.
- ViewModel은 Activity가 만들어 내려준다. 목적지마다 새로 만들면 탭 이동 때 상태가 끊기는데 인수 조건이 바로 그 유지다. 그래서 `hilt-navigation-compose`도 필요 없다.
- 탭 이동은 시작 목적지까지 `popUpTo` + `saveState`/`restoreState` + `launchSingleTop`이다. 뒤로 가기를 여러 번 눌러야 앱을 벗어나는 일이 없다.
- 홈·라이브러리는 "아직 준비되지 않은 화면입니다." 자리표시자다. 내용은 KM-151·KM-116이다.
- 미니 플레이어를 하단 내비게이션 위로 옮기고 전체 화면 플레이어에서는 접는다. 줄을 누르면 `now-playing`으로 간다. KM-091에서 미뤄 둔 "tap opens Now Playing"이 채워졌다.
- `TestPlaybackScreen`에서 검색을 떼고 재생만 남겼다. 현재 곡 제목·아티스트를 함께 보여준다. 이 화면은 KM-092가 대체한다. WiFi 전용 스위치는 KM-153에서 설정으로 옮긴다.
- `PlaybackState.canPause`를 더해 셸과 전체 화면이 같은 계산을 되풀이하지 않게 했다.
- 하단 내비게이션은 Now Playing에서도 보인다. 임시 화면이라 탭을 항상 닿게 두는 편이 낫다고 판단했고, 전체 화면에서 감출지는 KM-092에서 정한다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(6분 43초). 단위 110개·실제 계약 7개·실기기 계측 28개, 실패/오류 0. 린트 오류 0·경고 22(변동 없음).
- 실기기 SM-T220 / Android 14 확인: 하단 탭 세 개가 아이콘·이름과 함께 나오고 검색 탭이 시작 화면이다. 검색 탭에서 곡을 골라 재생한 뒤 홈 탭으로 옮겨도 미니 플레이어가 앨범 이미지·제목·아티스트와 함께 남고 세션은 PLAYING을 유지하며 위치가 15250 → 33293 → 63591로 계속 진행했다(인수 조건 player state survives navigation PASS). 미니 플레이어를 누르면 전체 화면으로 가고 그 화면에서는 미니 플레이어가 접힌다. 뒤로 가기를 누르면 떠났던 홈 탭으로 돌아오고 재생은 그대로다. 화면 캡처는 captures/km-150에 보관했다(저장소 추적 대상 아님).
- UI 자동 검사는 넣지 않았다. Compose UI 검사 의존성이 없고 AGENTS.md 16이 UI 검사를 최소화하라고 한다. 내비게이션은 실기기 눈 확인으로 다뤘다.
- 결정 ADR-049. 다음은 KM-092 Now Playing screen이다.

## KM-092 완료 — Now Playing screen

- 브랜치 `codex/KM-092-now-playing`. `feature/player/NowPlayingScreen`을 추가하고 임시 `TestPlaybackScreen`을 지웠다. KM-150이 붙인 `now-playing` 목적지의 실제 내용이다.
- 전체 화면에서는 하단 내비게이션을 감춘다. 앨범 이미지가 화면 폭 전체의 정사각형이라 자리가 필요하고, 탭이 남아 있으면 이 화면이 탭의 일부처럼 보인다. 화면 안에 뒤로 버튼을 두어 시스템 뒤로 가기 말고도 나갈 길을 만들었다. KM-150에서 미뤄 둔 판단이다.
- 사용자와 합의한 대로 즐겨찾기·대기열 버튼은 자리만 두고 비활성이다. 즐겨찾기 저장은 KM-112, 대기열 화면은 KM-097이다. 반응 없는 버튼은 고장으로 보이므로 비활성으로 두고 "아직 준비되지 않은 기능입니다."를 함께 적었다.
- 이전·다음은 두지 않았다. KM-094가 UI·알림·잠금화면을 함께 다루는 작업이라 UI만 먼저 만들면 반쪽으로 시작된다.
- 진행은 슬라이더와 위치·길이 표시다. 끌고 있는 동안에는 손가락 위치의 시간을 보여준다. 끌어서 탐색의 인수 조건 확인은 KM-093이다.
- 화면을 세로로 흘린다. 제목 두 줄이나 요금제 안내가 붙으면 세로가 모자랄 수 있다.
- WiFi 전용 스위치는 남겼다. 설정 화면이 KM-153이라 지금 없애면 KM-137 기능에 닿을 길이 사라진다.
- `destination_now_playing` 문자열을 지웠다. 이미지와 제목이 머리글 역할을 하므로 제목 줄이 필요 없고, 남기면 쓰이지 않는 리소스가 된다(린트 UnusedResources로 잡혔다).
- 단위·계측 검사를 새로 넣지 않았다. 이 작업은 화면 배치이며 재생 상태 매핑은 KM-090에서, 세션 이미지는 KM-091에서 이미 검사한다. AGENTS.md 16이 UI 검사를 최소화하라고 한다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(4분 50초). 단위 110개·실제 계약 7개·실기기 계측 28개, 실패/오류 0. 린트 오류 0·경고 22(문자열을 지워 21개에서 늘었던 것이 되돌아왔다).
- 실기기 SM-T220 / Android 14 확인: 미니 플레이어를 눌러 들어오면 뒤로 버튼·전체 폭 앨범 이미지·제목·아티스트·"재생 중"·진행(1:47/3:41)·즐겨찾기(흐림)·일시정지·대기열(흐림)·안내 문구가 나오고 하단 내비게이션은 없다. 즐겨찾기와 대기열을 눌러도 화면이 그대로이고 재생도 영향받지 않는다(PLAYING 유지). 일시정지를 누르면 세션이 PAUSED(2), 다시 누르면 PLAYING(3)이다. 화면 안 뒤로 버튼을 누르면 떠났던 검색 탭으로 돌아온다. 화면 캡처는 captures/km-092에 보관했다(저장소 추적 대상 아님).
- 함정: 검색 탭에서 좌표로 조작할 때 소프트 키보드가 올라와 있으면 하단 미니 플레이어 자리에 키보드가 있어 엉뚱한 키가 눌린다. 실제로 검색어에 'ㄹ'이 입력됐다. 좌표 조작 전에 뒤로 가기로 키보드를 내리고 `uiautomator dump`로 좌표를 다시 확인해야 한다.
- 검증 동안 화면 타임아웃을 600000으로 올리고 끝난 뒤 30000으로 되돌렸다.
- 결정 ADR-050. 신규 의존성 없음. 다음은 KM-093 Seek다.

## KM-093 완료 — Seek

- 브랜치 `codex/KM-093-seek`. 끌어서 탐색 자체는 이미 동작했으나 손을 뗀 직후 표시가 탐색 이전 자리로 한 번 되돌아갔다가 목표로 뛰는 문제가 있었다. 위치 보고가 250ms 간격이라 손가락 값을 지운 순간에는 옛 위치만 있기 때문이다. 인수 조건 progress remains synchronized가 가리키는 지점이라 이것을 고쳤다.
- `feature/player/PendingSeek`를 추가했다. 표시 위치는 손가락 → 아직 도달하지 않은 목표 → 실제 위치 순으로 고르며 그 규칙을 `seekDisplayPositionMs`로 떼어 화면 없이 검사한다.
- 목표를 놓아주는 조건 셋: 목표 근처(±1초), 목표를 지나 재생이 계속됨, 목표에서 멀어짐. 마지막이 없으면 탐색이 받아들여지지 않았을 때 표시가 목표에 붙어 멈춘다.
- 도달하면 목표를 지운다. 남겨 두면 재생이 진행한 뒤 오차 범위를 벗어나 표시를 다시 가로챈다. 지우는 일은 `LaunchedEffect`에서 한다. 조립 중에 상태를 쓰지 않는다.
- 시간 문구도 슬라이더와 같은 값을 쓴다. 둘이 다른 것을 가리키면 안 된다.
- 단위 7개 추가: 끌는 중에는 손가락이 이김(이전 목표보다도), 탐색이 없으면 실제 위치, 도달까지 목표 유지 후 넘김, 뒤로 탐색도 같음, 목표를 지나쳤으면 놓음, 도달하지 못한 탐색이 표시를 얼리지 않음, 오차 범위 경계.
- 계측 1개 추가(`SeekTest`): 앞으로 60초 탐색이 실제 위치를 옮기고 그 뒤 진행이 이어지는지, 뒤로 10초 탐색도 같은지, 음수는 0으로 길이 초과는 길이로 좁혀지는지 확인한다. 계측 28개 → 29개.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 29초). 단위 117개·실제 계약 7개·실기기 계측 29개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 SM-T220 / Android 14 확인(내장 테스트 음원 120초): 0:06에서 슬라이더를 오른쪽으로 끌어 손을 떼자 화면이 곧바로 1:24~1:26을 보이고 세션 위치가 87353ms였다. 이전 자리(0:06)로 되돌아가는 구간이 없다. 이어서 위치가 1:50까지 계속 진행했다. 왼쪽으로 끌자 0:32(세션 32782ms)로 옮겨지고 다시 0:39까지 진행했다. 화면 캡처는 captures/km-093에 보관했다(저장소 추적 대상 아님).
- 검증 동안 화면 타임아웃을 600000으로 올리고 끝난 뒤 30000으로 되돌렸으며 재생을 일시정지했다.
- 결정 ADR-051. 신규 의존성 없음. 다음은 KM-094 Previous / Next다.

## KM-094 완료 — Previous / Next

- 브랜치 `codex/KM-094-previous-next`. 사용자와 합의해 대기열을 만드는 경로는 KM-097로 미루고 버튼과 명령 연결만 다뤘다. `playTrack`이 `setMediaItem`으로 한 곡을 갈아 끼우므로 대기열에는 늘 한 곡뿐이다.
- 한 곡뿐이어도 이전은 뜻이 있다. Media3는 다음 곡이 없을 때 `seekToPrevious()`를 그 곡의 처음으로 되돌리는 동작으로 정의한다. 이전은 살리고 다음은 비활성이다.
- 가용성을 앱이 따로 계산하지 않고 `Player.isCommandAvailable`을 그대로 읽어 `PlaybackState.hasPrevious`·`hasNext`로 옮겼다. 화면·알림·잠금화면이 같은 근거를 쓰므로 세 곳이 갈릴 수 없다. 인수 조건 consistent behavior를 이 방식으로 만족시킨다.
- 알림·잠금화면 버튼은 새로 만들지 않았다. Media3의 알림 제공자가 같은 가용성으로 버튼을 구성하고 곡이 하나일 때 다음 버튼을 내지 않는다. 앱이 자체 버튼을 끼우면 규칙이 둘이 된다.
- 아이콘 대신 문자열 버튼을 썼다. `material-icons-core`의 클래스 목록을 확인했더니 SkipPrevious·SkipNext가 없다. 두 글리프 때문에 훨씬 큰 extended를 넣지 않고, 이 화면의 재생·일시정지도 이미 문자열이라 표기가 일관된다.
- 단위 2개 추가: 이전·다음 가용성이 상태까지 도달, 기본 상태에는 둘 다 없음.
- 계측 1개 추가(`PreviousNextTest`): 한 곡 상태에서 hasPrevious 참·hasNext 거짓, 화면 경로의 이전이 곡을 처음으로 되돌림, 다음은 위치를 되돌리지 않음, 알림에 이전 버튼은 있고 다음 버튼은 없음, 알림의 이전 버튼도 같은 결과를 냄. 계측 29개 → 30개.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(4분 26초). 단위 119개·실제 계약 7개·실기기 계측 30개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 SM-T220 / Android 14 확인: Now Playing의 조작 줄이 즐겨찾기(흐림)·이전(활성)·일시정지·다음(흐림)·대기열(흐림)이다. 슬라이더로 89678ms까지 옮긴 뒤 화면의 이전을 누르면 0으로 돌아가고, 다시 89442ms로 옮긴 뒤 다음을 눌러도 위치가 되돌지 않는다. 잠금화면 미디어 카드에는 이전과 일시정지만 있고 다음 버튼이 없으며, 카드의 이전을 누르면 112639ms에서 다시 처음으로 돌아갔다(keyguard showing=true). 화면 캡처는 captures/km-094에 보관했다(저장소 추적 대상 아님).
- 함정: `dumpsys media_session`의 상태 줄에는 `position=`과 `buffered position=`이 함께 있어 `grep -o "position=[0-9]*"`가 두 값을 낸다. 실제 재생 위치를 볼 때는 `state=...(n), position=` 형태로 함께 잡아야 한다.
- 검증 동안 화면 타임아웃을 600000으로 올리고 끝난 뒤 30000으로 되돌렸으며 재생을 일시정지했다.
- 결정 ADR-052. 신규 의존성 없음. 다음은 KM-095 Shuffle이다.

## KM-095 완료 — Shuffle

- 브랜치 `codex/KM-095-shuffle`. `PlayerConnection.setShuffleEnabled`를 붙이고 Now Playing에 선택 상태를 가진 `FilterChip`을 뒀다. 켜짐은 채워진 칩, 꺼짐은 테두리 칩이다.
- 화면의 켜짐 표시는 세션이 돌려준 `PlaybackState.shuffleEnabled`만 근거로 한다. 눌렀다는 사실을 따로 기억하면 세션이 거절했을 때 화면이 거짓을 보인다.
- 사용자 요청대로 queue behavior를 `playQueue` 계측 검사로 덮으려 했고, 그 과정에서 중요한 한계를 확인했다. **섞인 재생 순서는 컨트롤러에서 관찰할 수 없다.** 세션이 보내는 `Timeline`에 셔플 순서가 실려 오지 않아(`RemotableTimeline`이 선형 순서로 되돌아감) 컨트롤러에 다음 곡을 물으면 늘 넣은 순서가 나온다.
- 확인 과정: 처음에는 "셔플을 켜면 대기열 전부를 한 번씩 지난다"로 썼는데 Media3가 섞인 순서에서 현재 곡의 자리를 그대로 두므로 앞으로만 지나가면 전부를 만나지 못해 실패했다. 다음으로 "열 번 다시 섞으면 다음 곡이 달라진다"로 바꿨더니 열 번 모두 넣은 순서였다. 그 사이 "컨트롤러가 다음이 있다고 보는데 넘어가지 못하는" 상태도 관찰했다(hasNext=true, phase=Buffering인데 15초 동안 이동 없음). 두 결과가 함께 가리키는 것은 컨트롤러가 셔플 순서를 모른다는 사실이다.
- 그래서 계측 검사는 관찰 가능한 범위로 확정했다. 토글이 세션까지 닿아 상태로 돌아오는 것과, 셔플을 켜고 끄어도 대기열 내용·컨트롤러가 보는 순서가 그대로인 것이다. 섞인 순서 자체는 고정하지 않고 한계를 검사 문서와 ADR-053에 적었다.
- 계측 검사를 앞선 계측이 남긴 상태에 의존하지 않게 만들었다. 시작할 때 대기열을 다시 넣고, 모든 대기를 `withTimeoutOrNull`과 설명이 붙은 assert로 바꿔 실패 지점이 드러나게 했다. 처음 판은 단독 실행에서는 통과하고 전체 스위트에서 5초 대기에 걸렸는데 메시지가 없어 원인을 알 수 없었다.
- 단위 1개 추가: 화면의 셔플 표시는 세션이 돌려준 값만 근거로 하며 연결 전에는 꺼짐.
- 계측 1개 추가(`ShuffleTest`). 계측 30개 → 31개.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(4분 39초). 단위 120개·실제 계약 7개·실기기 계측 31개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 SM-T220 / Android 14 확인: Now Playing을 아래로 밀면 셔플 칩이 나온다. 누르면 테두리 칩에서 채워진 칩으로 바뀌고 다시 누르면 되돌아간다. 화면 캡처는 captures/km-095에 보관했다(저장소 추적 대상 아님).
- 함정: Compose `FilterChip`의 선택 상태는 `uiautomator dump`의 `selected`/`checked`에 나오지 않는다. 화면 캡처로 확인해야 한다.
- 결정 ADR-053. 신규 의존성 없음. 다음은 KM-096 Repeat다.

## KM-096 완료 — Repeat

- 브랜치 `codex/KM-096-repeat`. Now Playing의 셔플 칩 옆에 반복 칩을 두고 없음 → 전체 → 한 곡 → 없음을 돌게 했다. 지금 무엇인지는 칩의 글자가 말한다.
- 사용자 요청으로 저장까지 넣었다. 인수 조건에는 optional이었지만 PRD 34의 DataStore 항목에 반복 모드가 있고 설정 저장소가 이미 있어 추가 비용이 거의 없다.
- 저장된 설정이 곧 적용되는 값이다. 화면은 `setRepeatMode`만 부르고 플레이어에 지시하지 않는다. 적용은 `MusicService`가 설정 흐름을 구독해 `player.repeatMode`에 옮긴다. 경로가 하나뿐이라 저장값과 실제 재생이 어긋나지 않고, UI 없이 세션만 살아난 경우에도 저장된 모드로 시작한다.
- 화면 표시는 저장값이 아니라 플레이어가 돌려준 `PlaybackState.repeatMode`를 쓴다. 저장값으로 그리면 적용이 실패했을 때 화면이 거짓을 보인다.
- `RepeatMode`를 공개 타입으로 바꿨다. 설정 계약에 들어가기 때문이다. 저장 값은 상수 이름으로 남으므로 이름을 바꾸면 이전 설정을 읽지 못한다는 것을 주석에 남겼다.
- `MusicService`에 서비스 수명의 코루틴 범위를 두고 `onDestroy`에서 끊는다.
- 단위 6개 추가: 순환 세 단계·세 번 누르면 제자리·모든 모드 도달(3개), 저장 기본값 없음·세 모드 저장과 재개방 후 유지·알 수 없는 값은 없음(3개).
- 계측 1개 추가(`RepeatModeTest`): 세 모드를 저장하면 각각 재생 상태로 돌아온다. 마지막에 없음으로 되돌려 기기 설정을 원래대로 남긴다. 계측 31개 → 32개.
- `SettingsRepository`에 멤버가 늘어 가짜 구현 셋(`NetworkPolicyTest`, `MeteredPlaybackBlockTest`, `SearchToPlayTest`)도 함께 고쳤다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 45초). 단위 126개·실제 계약 7개·실기기 계측 32개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 SM-T220 / Android 14 확인: 칩을 누를 때마다 "반복 없음" → "전체 반복" → "한 곡 반복" → "반복 없음"으로 돈다. "한 곡 반복"으로 두고 앱을 force-stop한 뒤 다시 켰더니 그대로 "한 곡 반복"이었다(인수 조건 state persistence PASS). 확인 후 "반복 없음"으로 되돌렸다. 화면 캡처는 captures/km-096에 보관했다(저장소 추적 대상 아님).
- 함정: Bash 안에서 PowerShell을 중첩 호출해 Gradle을 돌리자 빌드가 끝난 뒤에도 파이프라인이 반환되지 않고 멈췄다(Gradle 쪽 CPU는 진행이 없는데 PowerShell이 대기). Gradle은 PowerShell에서 직접 실행한다.
- 결정 ADR-054. 신규 의존성 없음. 다음은 KM-097 Queue UI다.

## KM-097 완료 — Queue UI (대기열 생성 경로 포함)

- 브랜치 `codex/KM-097-queue-ui`. 사용자 요청으로 대기열을 만드는 경로까지 포함했다. M6의 마지막 작업이다.
- `playQueue(tracks, startIndex)`가 검색 결과 전체를 대기열에 넣고 고른 자리부터 재생한다. `SearchResultList`·`SearchScreen`의 선택 콜백이 고른 곡 하나가 아니라 목록과 자리를 넘기도록 바꿨다. `playQueue`의 인자도 `Triple` 목록에서 `List<Track>`으로 바꿨다.
- `PlaybackState`에 `queue`와 `queueIndex`를 더했다. 목록은 Timeline 창 순서(넣은 순서)이고 대기열 밖을 가리키는 자리는 -1로 정리한다. Media3는 대기열이 비어도 현재 자리를 0으로 돌려준다.
- 대기열은 바뀔 때만 다시 읽는다. 위치 갱신이 250ms마다 오므로 매번 만들면 낭비다. `onTimelineChanged`로 표시를 세우고 연결 직후에도 한 번 읽는다.
- `feature/player/QueueScreen`을 추가하고 `queue` 목적지를 붙였다. 전체 화면 플레이어의 대기열 버튼이 살아났다(KM-092에서 비활성으로 뒀던 것). 전체 화면 목적지에서는 하단 내비게이션을 비운다.
- 셔플이 켜졌을 때는 넣은 순서를 그대로 보여주고 "실제 재생 순서는 이 순서와 다릅니다"라고 알린다(ADR-053). 순서를 감추면 빼기·옮기기를 할 수 없다.
- 끌어서 옮기기 대신 위·아래 버튼을 뒀다. Compose에 끌어 정렬하는 기본 목록이 없고 인수 조건의 reorder는 "가능하면"이다.
- 단위 3개 추가: 대기열과 현재 자리가 상태까지 도달, 대기열 밖 자리는 -1, 기본 상태는 빈 대기열.
- 계측 1개 추가(`QueueTest`): 두 번째 자리부터 재생하도록 넣으면 목록 전체와 자리·제목이 상태로 오고, 자리를 옮기면 순서와 현재 자리가 함께 바뀌고 재생 중인 곡은 그대로이며, 빼면 목록이 줄고, 대기열 밖을 가리키는 요청은 아무 일도 하지 않는다. 계측 32개 → 33개.
- ShuffleTest의 셔플 켠 상태 확인을 순회에서 대기열 목록으로 바꿨다. 전체 스위트에서 "queue-order-4 다음으로 넘어가지 않았다"로 실패했는데, 이는 컨트롤러 순회가 셔플이 켜지면 결정적이지 않다는 뜻이다. 컨트롤러가 스스로 답할 때는 넣은 순서, 세션이 진짜 상태를 밀어 넣은 뒤에는 섞인 순서가 나온다. 대기열 목록은 Timeline 창 순서라 결정적이므로 그것으로 본다.
- 린트가 `player_not_ready_action`을 미사용으로 잡았다. 대기열 버튼이 살아나 안내 문구를 지웠기 때문인데, 즐겨찾기는 여전히 비활성이라 그 안내는 아직 필요하다. 문구를 되살렸다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 5초). 단위 129개·실제 계약 7개·실기기 계측 33개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 SM-T220 / Android 14 확인: "iu" 검색 결과에서 두 번째 곡을 골랐더니 그 곡이 재생되고 나머지 결과가 모두 대기열에 남았다. 대기열 화면에 앨범 이미지·제목·아티스트가 나오고 현재 곡은 배경색과 "재생 중"으로 구분된다. 첫 항목의 위로 버튼은 비활성이고, 세 번째 항목의 위로 버튼을 누르면 두 번째 자리로 올라간다. 첫 항목의 X를 누르면 목록에서 사라지고 재생은 그대로 이어진다(PLAYING 유지). 셔플을 켜고 대기열로 돌아오면 안내 문구가 붙는다. 대기열이 생겨 Now Playing의 다음 버튼도 활성이 됐다. 화면 캡처는 captures/km-097에 보관했다(저장소 추적 대상 아님).
- 결정 ADR-055. 신규 의존성 없음. M6 Player UX 완료. 다음은 M7 Library의 KM-110이다.

## KM-110 완료 — Room schema

- 브랜치 `codex/KM-110-room-schema`. M7 Library의 첫 작업이다. 사용자와 정한 세 가지로 진행했다. `SearchHistoryEntity` 제외, 자리표시 표 삭제, 명시적 마이그레이션.
- 표 다섯 개를 `core/database/entity`에 만들었다. `tracks`·`favorites`·`playlists`·`playlist_items`·`playback_history`다. `search_history`는 만들지 않는다. 최근 검색어는 DataStore에 있다(ADR-046).
- 자리표시 표 `schema_baseline`을 지우고 버전을 1에서 2로 올렸다. 마이그레이션에서 `DROP TABLE`한다.
- 마이그레이션 SQL은 손으로 쓰지 않았다. 엔티티를 먼저 쓰고 `kspDebugKotlin`으로 2.json을 내보낸 뒤 그 `createSql`을 그대로 옮겼다. 손으로 쓰면 Room이 기대하는 정의와 한 글자만 달라도 실행 중 검증이 깨진다.
- 검사 전용 `androidx.room:room-testing`을 추가했다. `MigrationTestHelper.runMigrationsAndValidate`가 결과 스키마를 내보낸 2.json과 견주므로 옮겨 적은 SQL을 믿을 근거가 된다.
- `DatabaseModule`에 `addMigrations`를 등록했다. 지우고 다시 만드는 방식은 쓰지 않는다. 사이드로드 앱이라 기기의 데이터가 유일한 사본이다.
- 계측 3개(기존 1개 갱신 + 신규 2개): 새 데이터베이스가 열리고 다시 열리며 기대한 다섯 표만 있고 비어 있다, 어떤 표도 재생 주소를 담지 않는다(열 이름에 url이 있으면 artwork_url이어야 한다), 버전 1 데이터베이스가 마이그레이션으로 버전 2가 되고 자리표시 표가 사라지며 새 표에 외래 키 관계까지 쓸 수 있다. 계측 33개 → 35개.
- DAO는 넣지 않았다. 이 작업은 표와 마이그레이션까지이며 DAO와 저장소는 KM-111이다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 34초). 단위 129개·실제 계약 7개·실기기 계측 35개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 마이그레이션 확인은 계측 검사가 담당한다. 실제 `keuney.db`는 아직 기기에 없다. 데이터베이스를 여는 코드가 없어(DAO 사용처가 KM-111부터) 파일이 만들어지지 않았기 때문이다. 계측 검사는 진짜 버전 1 파일을 만들어 마이그레이션을 돌리므로 같은 경로를 기기에서 확인한다.
- 결정 ADR-056. 신규 의존성 1개(검사 전용 room-testing). 다음은 KM-111 Library repository다.

## KM-111 완료 — Library repository

- 브랜치 `codex/KM-111-library-repository`. DAO 넷을 `core/database/dao`에 두고 `core/library/LibraryRepository` 하나로 묶었다. 구현은 `data/repository/LibraryRepositoryImpl`이다.
- DAO는 저장소 구현만 주입받는다. 화면과 ViewModel의 그래프에는 DAO가 올라가지 않으며 DAO 타입도 internal이다(AGENTS.md 10).
- 읽기는 모두 Flow다. 쓰기는 `Track`을 받고 구현이 곡을 먼저 저장한다. 세 표가 tracks를 외래 키로 가리키므로 곡이 없으면 쓰기가 실패한다.
- 시각은 저장소가 읽고 검사에서는 주입해 고정한다. 화면이 시계를 넘기면 화면마다 다른 시계를 쓸 수 있다.
- 어디에서도 가리키지 않는 곡은 정리한다. 아직 다른 곳에서 가리키면 남는다.
- 최근 재생은 곡별로 가장 최근 시각만 남겨 묶는다. 같은 곡이 여러 번 나오면 "최근 재생"이 쓸모없어진다. 언제 기록할지는 KM-115가 정한다.
- 재생목록 자리 번호를 정하는 것과 담는 것을 한 트랜잭션에 뒀다. 둘로 나뉘면 같은 자리가 두 번 나온다.
- `core/model/Playlist`를 추가했다(ARCHITECTURE 4의 트리에 있던 파일). 담긴 곡 수를 함께 들고 곡 목록은 따로 읽는다.
- 단위 11개 추가: 즐겨찾기가 곡을 먼저 저장, 해제 시 행 제거와 곡 정리, 도메인 타입으로 반환, 즐겨찾기 여부 관찰, 재생목록의 곡 수, 만들 때 시각 기록, 담을 때 곡 먼저 저장, 재생 기록의 곡 저장과 시각, 기록 지우기와 곡 정리, 알 수 없는 source는 Remote, 왕복 후 메타데이터 유지.
- 계측 8개 추가(`LibraryDaoTest`, 메모리 데이터베이스): 즐겨찾기 최신순 관찰과 해제, 같은 곡 두 번 즐겨찾기해도 한 행, 재생목록 담은 순서 유지와 빼기, 같은 곡 두 번 담기와 한 번에 빼기, 이름 변경과 삭제 시 항목까지 삭제, 최근 재생 곡별 한 번 최신순과 개수 제한과 지우기, 곡 삭제 시 즐겨찾기·기록 함께 삭제, 다른 곳이 가리키면 곡이 남음. 계측 35개 → 43개.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 33초). 단위 140개·실제 계약 7개·실기기 계측 43개, 실패/오류 0. 린트 오류 0·경고 22.
- 화면 변경은 없다. M7의 세 기능을 한 인터페이스에 담았으므로 KM-112~115는 화면과 정책만 붙인다.
- 함정: Room 메모리 데이터베이스도 SQLite 기본대로 외래 키가 꺼져 있다. 실제 앱과 같게 동작을 확인하려면 `PRAGMA foreign_keys = ON`을 켜야 한다.
- 결정 ADR-057. 신규 의존성 없음. 다음은 KM-112 Favorites다.

## KM-112 완료 — Favorites

- 브랜치 `codex/KM-112-favorites`. 사용자와 정한 대로 즐겨찾기 목록을 라이브러리 탭에 바로 넣었다. 그때까지 목록을 어디에도 두지 않으면 저장은 되지만 볼 수 없는 상태가 된다.
- `feature/library/LibraryViewModel`과 `LibraryScreen`을 추가하고 라이브러리 탭의 자리표시자를 대체했다. Activity가 ViewModel을 만들어 내려주며 전체 화면 플레이어와 라이브러리 탭이 함께 쓴다.
- Now Playing의 즐겨찾기 버튼을 살렸다. 채워진 하트와 빈 하트로 상태를 보이고 설명도 "즐겨찾기"/"즐겨찾기 해제"로 바뀐다. 여부는 저장소에서 흐르는 값을 그대로 쓴다.
- 세션이 준 것으로 곡을 만드는 `NowPlaying.toTrack(durationMs)`를 추가했다. 길이는 재생 상태에서 받고 알 수 없으면 넣지 않는다. 0을 넣으면 화면이 "0:00"을 사실처럼 보여준다.
- 즐겨찾기 항목을 누르면 그 곡부터 재생하고 목록 전체가 대기열이 된다. 검색 결과와 같은 방식이다.
- 곡 한 줄의 모양을 `ui/components/TrackRow`로 옮겨 검색과 라이브러리가 같은 것을 쓴다. `trackSubtitle`과 그 단위 검사도 함께 옮겼다.
- Now Playing의 "아직 준비되지 않은 기능입니다." 문구와 문자열을 지웠다. 비활성인 조작이 더 없다.
- 함정: Dagger는 Kotlin 기본값을 보지 않는다. `LibraryRepositoryImpl`의 시계에 기본값을 준 채 `@Inject`를 붙였더니 `Function0<Long>`을 주입할 수 없다는 오류가 났다. 주입용 생성자를 시계 없이 따로 두고 검사만 시계를 넘긴다.
- 단위 7개 추가: 세션 metadata가 곡이 됨, 알 수 없는 길이는 비움(0과 음수), 이미지 없음 유지, 즐겨찾기 목록이 저장소를 따라감, 켜기·끄기가 저장소까지 닿음, 여부가 저장소에서 옴(다른 곡은 아님).
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(4분 53초). 단위 147개·실제 계약 7개·실기기 계측 43개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 SM-T220 / Android 14 확인: 곡을 재생하고 하트를 누르면 설명이 "즐겨찾기"에서 "즐겨찾기 해제"로 바뀌고 아이콘이 채워진다. 라이브러리 탭에 그 곡이 앨범 이미지·제목·"1theK (원더케이) · 3:41"과 함께 나온다. 앱을 force-stop하고 다시 켜도 그대로 남는다(인수 조건 app restart persistence PASS). 이때 기기에 실제 `keuney.db` 파일이 처음 만들어진 것도 확인했다. 라이브러리 항목을 누르면 그 곡이 재생되고(PLAYING), Now Playing에서 하트를 다시 누르면 라이브러리가 "즐겨찾기한 곡이 없습니다."로 돌아간다. 확인 후 즐겨찾기를 해제해 기기 데이터를 원래대로 남겼다. 화면 캡처는 captures/km-112에 보관했다(저장소 추적 대상 아님).
- 결정 ADR-058. 신규 의존성 없음. 다음은 KM-113 Playlists다.

## KM-113 완료 — Playlists

- 브랜치 `codex/KM-113-playlists`. 방향은 내가 정했다. 재생목록 목록은 라이브러리 탭의 두 번째 구획, 한 재생목록의 곡 목록은 전체 화면 목적지 `playlist/{id}`, 곡을 담는 진입점은 Now Playing이다.
- `feature/library/PlaylistScreen`과 `PlaylistDialogs`(이름 입력·담을 곳 고르기)를 추가하고 `LibraryScreen`에 재생목록 구획을 더했다. 이 프로젝트에서 대화상자가 처음 등장한다.
- 이름 바꾸기·삭제는 재생목록 화면에, 곡 빼기는 그 화면의 줄마다 뒀다. 목록의 줄에 삭제를 붙이면 실수로 누르기 쉽고 되돌릴 방법이 없다.
- 삭제에 확인을 묻지 않는다. 대신 한 번 더 들어가야 닿는 자리에 뒀다. 대화상자를 셋으로 늘리지 않기 위한 선택이며 되돌리기가 없다는 점은 남는 위험이다.
- 담기 대화상자에서 새 재생목록을 고르면 만들고 그 곡을 바로 담는다. 만든 뒤 다시 고르게 하면 같은 것을 두 번 묻는 셈이다.
- 빈 이름으로는 만들거나 바꿀 수 없고 앞뒤 공백은 잘라낸다. 같은 이름은 막지 않는다.
- 재생목록 화면의 곡을 누르면 그 곡부터 재생하고 재생목록 전체가 대기열이 된다. 누르면 아무 일도 없는 줄을 두지 않기 위해 연결했다. 이어 듣기와 이전·다음 확인은 KM-114다.
- 두 구획이 한 `LazyColumn` 안에서 함께 흐른다. 구획마다 스크롤을 주면 같은 방향으로 겹친 스크롤이 되고 Compose가 허용하지 않는다.
- 단위 6개 추가: 재생목록 목록이 저장소를 따라감, 만들 때 이름 trim과 빈 이름 무시, 새 목록에 곡 바로 담기, 빈 이름이면 만들지도 담지도 않음, 이름 바꾸기 trim과 빈 이름 무시, 담기·빼기·삭제가 저장소까지 닿음.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 7초). 단위 153개·실제 계약 7개·실기기 계측 43개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 SM-T220 / Android 14에서 인수 조건 다섯 개를 확인했다. 만들기로 "morning"(0곡) 생성 → Now Playing의 담기 버튼으로 그 목록에 담아 "1곡" → 재생목록 화면에서 이름을 "evening"으로 바꿈 → 곡을 빼서 "담긴 곡이 없습니다." → 삭제하면 라이브러리로 돌아오고 "재생목록이 없습니다."가 된다. 확인 후 기기 데이터는 비어 있는 상태로 남았다. 화면 캡처는 captures/km-113에 보관했다(저장소 추적 대상 아님).
- 함정: 대화상자의 입력창을 누르면 소프트 키보드가 확인·취소 버튼을 덮는다. 좌표로 조작할 때는 글자를 넣은 뒤 뒤로 가기로 키보드를 내리고 다시 좌표를 확인해야 한다. 처음에는 키보드에 가려진 자리를 눌러 대화상자만 닫혔다.
- 결정 ADR-059. 신규 의존성 없음. 다음은 KM-114 Playlist playback이다.

## KM-114 완료 — Playlist playback

- 브랜치 `codex/KM-114-playlist-playback`. **앱 코드는 바뀌지 않았다.** 재생목록 → 대기열은 KM-113, 이전·다음 명령은 KM-094, 대기열 상태는 KM-097이 이미 만들었다. 이 작업의 내용은 여러 곡 대기열에서 그것들이 실제로 맞물리는지 확인하는 것이었다.
- 계측 1개 추가(`PlaylistPlaybackTest`). 내장 테스트 음원을 두 번 넣은 대기열로 확인한다. 두 항목의 ID가 같아 자리는 `queueIndex`로 본다. 가짜 ID를 쓰면 주소 해석이 실패해 이어 듣기를 볼 수 없다.
- 확인 내용: 대기열 두 곡과 첫 자리·다음 가용성 → 다음으로 두 번째 자리로 이동하고 마지막에서는 다음이 없음 → 막 시작한 자리에서 이전은 앞 곡으로 → 40초 지점에서 이전은 그 곡의 처음으로(자리는 그대로) → 곡 끝 2초 앞에서 재생하면 다음 곡으로 저절로 이어지고 계속 재생됨. 계측 43개 → 44개.
- 함정: 처음 판은 `seekToNext` 뒤 `hasNext`를 즉시 읽어 "마지막 곡인데 다음을 쓸 수 있다"로 깨졌다. 컨트롤러가 자리를 먼저 알리고 명령 집합을 나중에 갱신한다. 가용성도 기다리는 조건에 넣어 고쳤다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 5초). 단위 153개·실제 계약 7개·실기기 계측 44개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 SM-T220 / Android 14에서 실제 재생목록으로 화면 경로를 확인했다. 검색 결과 두 곡을 Now Playing의 담기로 새 재생목록 "drive"에 담아 2곡을 만들고, 재생목록 화면에서 첫 곡을 눌러 재생(PLAYING)한 뒤 Now Playing의 다음 버튼으로 두 번째 곡으로 넘어갔다. 이전 버튼은 두 번째 곡을 몇 초 들은 뒤 눌러 "그 곡의 처음으로" 갈래를 지났다. 앞 곡으로 가는 갈래는 계측 검사가 다룬다. 확인 후 재생목록을 삭제하고 일시정지했다.
- 함정: `connectedDebugAndroidTest`는 실행이 끝나면 앱과 테스트 APK를 기기에서 지운다. 기기 화면 확인은 그 뒤에 다시 설치해야 하며, 앱 데이터도 함께 사라진다. 실제로 계측만 따로 돌린 뒤 `am start`가 "Activity class does not exist"로 실패해 알아냈다.
- 함정: 기기가 이미 잠금이 풀린 상태에서 잠금 해제용 스와이프를 보내면 홈 화면 위젯이 열린다. 시스템 위치 권한 팝업이 떠 앱을 덮은 적이 있다. 조작 전에 `dumpsys window policy`로 잠금 상태를 확인한다.
- 결정 ADR-060. 신규 의존성 없음. 다음은 KM-115 Playback history다.

## KM-115 완료 — Playback history

- 브랜치 `codex/KM-115-playback-history`. 방향은 내가 정했다. 기준은 "얼마 이상 들었을 때", 기록하는 자리는 `MusicService`, 최근 재생 구획은 라이브러리 탭 맨 위, `history enabled` 설정은 KM-153으로 미룸이다.
- 기준은 10초이고 곡이 20초보다 짧으면 절반이다. 규칙을 `PlaybackHistory.listenedThresholdMs`로 떼어 단위 검사로 고정했다. 시작만으로 남기면 훑어보며 넘긴 곡까지 들어온다.
- 기록은 서비스가 남긴다. 화면이 닫혀도 배경 재생은 이어지는데 ViewModel이 남기면 그때 관찰이 끊겨 기록이 빠진다. 위치를 주기적으로 살피지 않고, 재생이 시작될 때 남은 시간만큼 기다린 뒤 여전히 같은 곡이 재생 중인지 본다.
- 중복 정책: 곡마다 한 행만 남긴다. `record`가 그 곡의 이전 기록을 지우고 새로 넣는다. 목록 조회는 곡별 최신 시각으로 묶는 쿼리를 그대로 두어 이전 판의 여러 행도 올바르게 읽힌다.
- 라이브러리 구획 순서는 최근 재생 → 즐겨찾기 → 재생목록(PRD 35). 지우기는 최근 재생 머리에 있고 목록이 비면 감춘다.
- `NowPlaying.toTrack`을 `feature/player`에서 `core/player`로 옮겼다. 서비스도 쓰는 도메인 변환이다.
- **시행착오 1.** 처음에는 "이미 남겼다"를 곡 ID로만 판단했다. 계측이 단독 실행에서는 통과하고 스위트에서만 실패했다. 원인은 앞선 계측이 같은 음원을 이미 남겨 래치가 걸려 있었고, 같은 항목을 다시 걸어도 Media3가 전환 콜백을 주지 않아 풀리지 않은 것이다. 전환 콜백에 기대는 판을 만들었다가 그것도 실패해, 판단을 **재생 위치**로 바꿨다. 위치가 기준보다 앞이면 새로 듣기 시작한 것으로 본다. 이는 검사만의 문제가 아니라 최근 재생에서 같은 곡을 다시 누를 때도 기록이 갱신되지 않는 실제 결함이었다.
- **시행착오 2.** 위치 기준으로 바꾼 뒤에도 계측이 실패했다. 임시 로그를 넣어 보니 서비스는 기록에 성공("recorded")했는데 검사의 Flow가 그것을 보지 못했다. 저장소 인스턴스 해시를 함께 찍어 확인한 결과 서비스와 검사가 서로 다른 인스턴스였다(37433786 vs 145070195). Hilt가 계측 검사마다 새 싱글턴 컴포넌트를 만들기 때문이며, Room의 변경 알림은 자기 인스턴스의 쓰기만 안다. 검사를 관찰에서 "다시 물어보기"로 바꿨고 로그는 걷어냈다.
- 실제 앱은 컴포넌트가 하나이므로 화면은 관찰만으로 갱신된다. 기기에서 확인했다. 라이브러리 탭으로 옮긴 직후에는 "최근 들은 곡이 없습니다."였고, 화면을 옮기지 않고 16초를 기다리자 그 자리에 곡이 나타났다.
- 단위 5개 추가: 기준 10초, 짧은 곡은 절반, 길이를 모르면 기준 그대로(3개), 최근 재생이 저장소를 따라가며 개수를 제한해 읽음, 지우기가 저장소까지 닿음(2개).
- 계측 1개 추가(`PlaybackHistoryRecordingTest`): 짧게 듣고 멈춘 곡은 남지 않고, 기준을 넘겨 들으면 남고, 지우면 비워진다. 계측 44개 → 45개.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 9초). 단위 158개·실제 계약 7개·실기기 계측 45개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 확인 후 최근 재생을 지우고 일시정지했다. 화면 캡처는 captures/km-115에 보관했다(저장소 추적 대상 아님).
- 결정 ADR-061. 신규 의존성 없음. 다음은 KM-116 Library screen이다.

## KM-116 완료 — Library screen (M7 완료)

- 브랜치 `codex/KM-116-library-screen`. 세 구획은 KM-112·113·115에서 이미 들어왔으므로 이 작업은 실제 사용에서 무너지는 지점을 고쳤다.
- 곡 구획은 앞의 다섯 개만 보여주고 나머지는 "더 보기"로 넘긴다. 최근 재생은 50개까지 쌓이고 즐겨찾기는 제한이 없어 전부 쏟으면 재생목록 구획이 화면 밖으로 밀린다. 접는 방법도 있지만 접힌 구획은 찾아온 것을 감춘다.
- 전체 목록은 목적지 `library-section/{section}` 하나가 구획 이름을 받아 그린다. 알 수 없는 이름이면 최근 재생을 보여준다.
- 즐겨찾기 전체 목록에는 줄마다 해제 버튼을 뒀다. KM-112에서 남겨 둔 "목록에서 바로 해제"가 채워졌다. 요약 화면에는 두지 않는다.
- 세 구획이 모두 비면 한 줄 안내와 재생목록 만들기 버튼만 보여준다. "없습니다" 세 줄은 무엇을 해야 하는지 알려주지 않는다.
- 재생목록 구획은 전부 보여준다. 줄이 짧고 개수가 적어 아래를 밀어내지 않는다.
- 단위 3개 추가: 구획 이름 왕복, 알 수 없는 이름은 구획이 아님, 요약이 구획마다 다섯 개만 보여줌.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(4분 53초). 단위 161개·실제 계약 7개·실기기 계측 45개, 실패/오류 0. 린트 오류 0·경고 22.
- 실기기 SM-T220 / Android 14 확인: 라이브러리가 비면 "아직 라이브러리가 비어 있습니다..." 한 줄과 새 재생목록 버튼만 나온다. 검색 결과에서 대기열을 만들고 Now Playing의 하트와 다음을 번갈아 눌러 여섯 곡을 즐겨찾기하자 요약 화면의 즐겨찾기 구획이 다섯 곡과 "더 보기"로 나왔다. 더 보기를 누르면 여섯 곡 전체와 줄마다 해제 버튼이 나오고, 하나를 해제하면 다섯 곡이 된다. 전부 해제하면 "즐겨찾기한 곡이 없습니다."가 되고 요약 화면도 같이 바뀐다. 확인 후 최근 재생을 지우고 일시정지했다. 화면 캡처는 captures/km-116에 보관했다(저장소 추적 대상 아님).
- 기기 조작 요령: Now Playing의 조작 줄은 제목이 한 줄인지 두 줄인지에 따라 y가 40px쯤 움직인다. 여러 곡을 연달아 다루려면 매번 `uiautomator dump`로 좌표를 다시 읽어야 한다.
- 결정 ADR-062. 신규 의존성 없음. **M7 Library 완료.** 다음은 M8의 KM-130이다.

## KM-132 완료 — Network reconnect

- 브랜치 `codex/KM-132-network-reconnect`. 사용자가 블루투스 기기가 없다고 해 KM-130·131을 건너뛰고 이 작업부터 했다.
- **짧은 끊김에는 손대지 않았다.** 기기에서 재생 중 WiFi를 끄자 재생이 그대로 이어졌다. ExoPlayer가 스스로 다시 읽어 보고 그 사이 버퍼로 이어진다. 인수 조건의 "temporary loss"는 이미 만족돼 있었다.
- 고칠 것은 그 뒤였다. 버퍼가 닿지 않는 자리로 가면 재생이 멈추는데, 화면은 "재생할 수 없습니다. 다시 시도해 주세요." 한 줄만 보여 주고 연결이 돌아와도 그대로 멈춰 있었다.
- 상태에 `PlaybackFailure`(Network/Source)를 실어 문구를 갈랐다. 연결 실패·시간 초과만 네트워크로 본다. 분류하지 못한 입출력 오류는 네트워크로 보지 않는다. 연결이 돌아올 때마다 같은 실패를 되풀이할 뿐이다.
- 그래서 `TrackStreamResolver`가 해석 실패를 `DataSourceException`의 오류 코드로 바꿔 던지게 했다. 이전에는 연결이 끊겨서 주소를 못 푼 것과 곡 자체를 못 가져오는 것이 똑같은 `IOException("Stream unavailable")`이었다. 코드만 넘기고 원문은 그대로 끊는다.
- 회복은 `ConnectivityManager`의 기본 연결 감시로 한다. 시간을 두고 되풀이해 보지 않는다. 끊긴 동안의 재시도는 어차피 실패하고 얼마 만에 돌아올지는 알 수 없다. 되살리는 것은 네트워크 때문에 멈췄고 그때 듣던 중이던 재생뿐이며, 판단은 `NetworkRecovery`가 상태만으로 한다. 자동 시도는 연속 3회까지이고 한 번이라도 다시 재생되면 처음으로 되돌린다.
- `ACCESS_NETWORK_STATE`를 manifest에 선언했다. 지금까지 요금제 확인(`NetworkPolicy`)은 의존성이 병합해 준 권한으로 동작하고 있었다. 쓰는 쪽이 선언하지 않으면 그 의존성이 바뀔 때 조용히 깨진다.
- **시행착오.** 첫 검증에서 lint가 `UnsafeOptInUsageError`로 실패했다. 클래스에 붙인 `@OptIn`은 같은 파일의 최상위 함수까지 덮지 않는다. 새로 뗀 확장 함수에 따로 붙였다. 계측·단위·계약은 그 실행에서 이미 모두 통과했다.
- 단위 10개 추가: 오류 코드 분류 4개(`PlaybackFailureTest`), 회복 규칙 6개(`NetworkRecoveryTest`).
- 계측은 늘리지 않았다. 연결을 끊었다 붙이는 일은 계측 안에서 만들 수 없다. 규칙은 단위 검사로 고정하고 실제 동작은 기기에서 확인했다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 16초). 단위 171개·실제 계약 7개·실기기 계측 45개, 실패/오류 0. 린트 오류 0·경고 22.
- **실기기 SM-T220 / Android 14 확인(WiFi 전용 기기, adb는 USB).** 713분짜리 원격 곡을 재생하고 `svc wifi disable` → 10초 뒤에도 "재생 중" 0:37(버퍼로 이어짐). 슬라이더로 649분 자리로 옮기자 "네트워크가 끊겼습니다. 연결되면 이어서 재생합니다."와 "다시 시도" 버튼이 나왔다. `svc wifi enable` 후 **화면을 건드리지 않고** 20초 만에 650:02에서 "재생 중"으로 돌아왔다.
- 오프라인에서 새 곡을 고르는 경로도 확인했다. WiFi를 끈 채 다른 곡을 누르면 같은 네트워크 문구가 나오고, 다시 켜자 손대지 않았는데 0:20에서 재생됐다. 주소 해석 실패의 오류 코드가 컨트롤러까지 닿는다는 증거다.
- 확인 후 최근 재생을 지우고 일시정지했으며 WiFi와 화면 꺼짐 시간(30000)을 되돌렸다. 화면 캡처는 captures/km-132에 보관했다(저장소 추적 대상 아님).
- 결정 ADR-063. 신규 의존성 없음. 다음은 KM-133이다. KM-130·131은 블루투스 기기가 생기면 진행한다.

## KM-133 완료 — Activity/process lifecycle regression

- 브랜치 `codex/KM-133-lifecycle-regression`. 인수 조건 넷 중 셋은 이미 지켜지고 있었다. 먼저 기기에서 확인하고 어긋나는 곳을 찾았다.
- 실기기 SM-T220 확인: 회전(세로↔가로)해도 재생이 이어지고 목적지도 그대로였다(세션 PLAYING 21.1초). 개발자 옵션 "활동 유지 안 함"을 켜고 홈으로 나갔다 돌아와도 Now Playing 화면과 위치(1:13)가 되살아났다. 뒤로 가기로 Activity를 끝내도 서비스는 계속 재생했고(95.2초) 프로세스도 살아 있었다. 확인 후 옵션을 껐다.
- 어긋난 곳은 **구성 변경 때 세션 연결을 버린다는 것**이었다. `onStop`이 무조건 끊으니 회전할 때마다 컨트롤러를 다시 만든다. 그 사이 재생 상태가 비어 미니 플레이어가 사라지고 조작 버튼이 꺼진다.
- `isChangingConfigurations`이면 끊지 않게 했다. ViewModel이 남으므로 다시 만들어진 Activity가 같은 연결을 이어 쓰고, `connect()`는 이미 붙어 있으면 아무 일도 하지 않는다. 정말로 떠날 때는 `PlayerViewModel.onCleared`가 끊는다.
- `android:configChanges`로 재생성을 막는 방법은 쓰지 않았다. 재생성은 그대로 두고 세션 연결만 이어 붙인다.
- 계측 1개 추가(`ActivityRecreationTest`). 회전을 흉내 내지 않고 `Activity.recreate()`로 같은 길을 지나가게 하며, 화면이 실제로 쓰는 ViewModel을 본다. 재생성이 끝난 뒤의 값만이 아니라 **그 사이의 연결 상태 변화를 모아** 확인한다.
- 이 검사가 실제로 회귀를 잡는지 확인했다. 고침을 잠시 되돌리고 돌리자 `expected:<[Connected]> but was:<[Connected, Disconnected, Connecting]>`으로 실패했다. 재생성 2초 뒤에도 아직 `Connecting`이었다는 뜻이기도 하다. 확인 후 고침을 되돌려 놓았다.
- Activity 종료 뒤의 재생 지속은 `BackgroundPlaybackTest`가 이미 다루므로 새로 만들지 않았다. 계측 45개 → 46개.
- 최근 앱 목록에서 밀어내는 경로는 자동화하지 못했다. `input swipe`로 카드를 지우려다 옆의 다른 앱이 열렸다. 이 경로는 남은 검증으로 적어 둔다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(4분 51초). 단위 171개·실제 계약 7개·실기기 계측 46개, 실패/오류 0. 린트 오류 0·경고 22.
- 확인 후 자동 회전과 화면 꺼짐 시간(30000)을 되돌리고 앱을 다시 설치했다.
- 결정 ADR-064. 신규 의존성 없음. 다음은 KM-135이다.

## KM-135 보류 — OEM battery help (조건 미충족)

- 브랜치 `codex/KM-135-battery-doze`. 이 작업에는 조건이 붙어 있다. "실기기에서 실제 background kill 문제가 재현될 때만 구현." 그래서 코드를 쓰기 전에 재현을 시도했다.
- 재현 시도(SM-T220 / Android 14): 한 곡 반복으로 내장 음원을 걸어 끝나지 않게 하고, 화면을 끄고, `dumpsys battery unplug`로 충전을 떼고, `dumpsys deviceidle force-idle`로 깊은 doze에 넣고, `am set-standby-bucket com.keuney.music restricted`로 대기 버킷을 45(RESTRICTED)까지 내렸다.
- 결과: **약 34분 동안 프로세스와 재생이 그대로였다.** 같은 pid(4687), 상태 PLAYING, deep state IDLE, 버킷 45. 재생이 끊기거나 프로세스가 죽는 일은 없었다.
- 버티는 이유는 구조에 있다. `dumpsys activity services`에서 `isForeground=true types=00000002`(mediaPlayback)이고 알림이 붙어 있다. 시스템은 이것을 사용자가 지금 쓰는 것으로 본다. 이 사실은 `BackgroundPlaybackTest`가 이미 검사한다.
- 인수 조건 둘 중 "no aggressive battery exemption prompt at first launch"는 지금 상태로 이미 충족한다. `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`를 선언하지 않고 관련 화면도 없다. 코드 전체에 배터리 관련 권한·문자열·인텐트가 없고 기기의 예외 목록에도 이 앱이 없다.
- 다른 하나("generic settings help")는 재현되지 않았으므로 만들지 않았다. 필요하지 않은 안내를 넣으면 쓰이지 않는 화면이 하나 늘고, 예외를 요구하는 앱과 구별되지 않는다.
- 그래서 상태 표시를 `[ ]`로 남긴다. KM-064와 같은 방식이다. 조건이 충족되면 그때 연다.
- 남은 한계: 삼성의 "사용하지 않는 앱 절전" 목록은 adb로 강제할 수 없고 보통 며칠 쓰지 않은 앱에 적용된다. 강제할 수 있는 것은 AOSP doze와 대기 버킷까지였다. KM-136 30분 사용 검사와 장기 실사용에서 다시 본다.
- 확인 후 `deviceidle unforce`·`battery reset`·버킷 active로 되돌리고, 반복 모드를 반복 없음으로 되돌리고 재생을 멈췄다. 반복 모드는 설정에 저장되므로 되돌리지 않으면 이후 작업까지 따라간다.
- 결정 ADR-065. 코드 변경 없음. 다음은 KM-136이다.

## KM-136 완료 — 30-minute smoke test

- 브랜치 `codex/KM-136-smoke-test`. 코드 변경은 없다. 실기기에서 실제로 써 보고 무엇이 깨지는지 보는 작업이다.
- 실기기 SM-T220 / Android 14. `logcat -c -b all`로 로그를 비우고 `am force-stop` 뒤 새로 시작해 **10:52:59 ~ 11:24:04, 31분 5초** 동안 이어서 썼다. 그 사이 프로세스는 pid 6716 하나였다. 다시 시작한 적이 없다.
- 인수 조건별로 무엇을 했는지:
  - **search**: "jazz", "jazzpiano+sleep" 두 번 검색했다. 두 번 다 결과가 왔다.
  - **multiple tracks**: 검색 결과 전체를 대기열로 만들고 다음·이전으로 옮겨 다녔다. 대기열 화면에서 항목을 빼고 다른 항목을 눌러 재생했다. 셔플과 전체 반복을 켰다 껐다. 즐겨찾기와 재생목록 만들기까지 했다.
  - **background**: 홈으로 나간 뒤 6분. 재생이 2.8초 → 367초로 이어졌고 pid 그대로.
  - **screen off**: 화면을 끈 뒤 7분. `mWakefulness=Dozing`에서 재생이 799초까지 이어졌다.
  - **lockscreen**: 잠긴 상태에서 미디어 버튼으로 조작했다. play/pause(85)로 PLAYING → PAUSED → PLAYING, 다음(87)로 대기열 다음 항목으로 넘어가 재생됐다. 잠금화면 조작과 Bluetooth AVRCP가 쓰는 것과 같은 경로다. 다만 이것은 미디어 버튼 처리를 확인한 것이고 Bluetooth 전송 자체는 아니다.
  - **Bluetooth**: 기기가 없어 확인하지 못했다(AGENTS.md 20). KM-130·131과 함께 남는다.
- **Crash 없음.** `logcat -b crash`에 이 패키지 기록이 없고(버퍼 전체가 0줄), main 로그에 `FATAL EXCEPTION`·`ANR in`·`Force finishing`이 하나도 없다. 우리 패키지의 E 로그는 시작 시 `am force-stop`으로 옛 프로세스를 정리하며 시스템 컴포지터가 남긴 것뿐이다.
- 30분 사용 뒤 라이브러리에는 최근 재생 3곡·즐겨찾기 1곡·재생목록 1개가 실제 사용대로 쌓여 있었다. KM-115 기록과 KM-112·116 화면이 실사용에서 동작한다는 뜻이다.
- **찾은 것 1(중요).** 재생할 수 없는 곡이 적지 않고, 그 곡에서 **재생이 멈춰 선다.** 여섯 곡을 시도해 셋이 `Source error`였다("jazz" 검색의 Playlist형 긴 영상들). KM-132의 분류대로 "이 곡을 재생할 수 없습니다."가 뜨고 다시 시도 버튼이 나오는 것은 맞게 동작했다. 문제는 그 뒤다. 대기열에 다음 곡이 있어도 넘어가지 않으므로, 30분 듣는 동안 한 곡만 걸려도 음악이 끊긴다. 자동으로 다음 곡으로 넘기는 것이 필요하다. 이 작업의 인수 조건이 아니므로 고치지 않고 후속 후보로 남긴다.
- **찾은 것 2.** 제목이 두 줄인 곡에서는 Now Playing의 셔플·반복 칩과 WiFi 전용 줄이 화면 밖에 있다. 세로로 흘리게 해 둔 것은 의도지만(KM-091), 자주 쓰는 조작이 스크롤 뒤에 있다. 실기기 조작 중에 이 자리를 눌러 다른 앱이 열리는 일도 있었다.
- **찾은 것 3.** 즐겨찾기가 다섯 곡 이하일 때는 전체 목록 화면으로 갈 수 없어(더 보기가 없다) 해제하려면 그 곡의 Now Playing까지 가야 한다. KM-116에서 요약 화면에 줄마다 버튼을 두지 않기로 한 결과다.
- 확인 후 되돌린 것: 셔플 끔, 반복 없음(설정에 저장되는 값), 재생 일시정지, 최근 재생 지우기, 즐겨찾기 해제, smoke 재생목록 삭제, 화면 꺼짐 시간 30000, 자동 회전 켬. 라이브러리가 빈 상태로 돌아온 것을 확인했다.
- 화면 캡처는 captures/km-136에 보관했다(저장소 추적 대상 아님).
- `gradlew.bat test lint assembleDebug assembleRelease sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0. 코드 변경이 없어 실기기 계측은 KM-133의 46개 통과 결과가 그대로 유효하다.
- 결정 기록 없음. 설계를 정한 것이 없고 확인만 했다. 다음은 M9의 KM-150 이후 남은 화면 작업(KM-151·152·153)이다.

## KM-151 완료 — Home screen

- 브랜치 `codex/KM-151-home-screen`. 방향은 내가 정했다. 인수 조건의 세 구획이 KM-116의 라이브러리 화면과 같은 것이어서 무엇으로 가를지 정하는 것이 이 작업의 일이었다.
- PRD 35는 **홈**에 최근 재생·즐겨찾기·재생목록을 두라고 하고 라이브러리의 내용은 정하지 않는다. 그 세 구획은 KM-112·116에서 라이브러리 탭에 들어갔다. 같은 모양으로 홈을 또 만들면 탭 하나가 남는다.
- 가른 기준: **홈은 한 번 눌러 다시 듣는 자리, 라이브러리는 전부 보고 정리하는 자리.** 모양으로 가른다. 홈은 가로로 흐르는 큰 정사각형 카드, 라이브러리는 세로 목록이다.
- 홈에는 지우기·해제 같은 정리 버튼이 없다. 다시 듣기 위해 들어온 자리에서 실수로 지우는 일이 없어야 한다. 비어 있는 구획은 그리지 않는다. 세 구획이 다 비면 한 줄 안내와 "검색으로 가기"만 둔다.
- 카드 줄은 개수를 자르지 않는다. 가로 줄은 보이는 것만 만들어 값이 들지 않고, 자르면 누른 자리와 대기열의 자리가 어긋날 위험만 생긴다. 라이브러리 요약이 다섯 개로 자르는 것은 세로로 쌓여 아래를 밀어내기 때문이다.
- 카드를 누르면 그 줄 전체가 대기열이 되고 누른 곡부터 재생한다. 재생목록 카드는 재생하지 않고 그 화면을 연다.
- **시작 목적지를 검색에서 홈으로 옮겼다.** 홈이 자리표시자일 때는 검색이 시작 화면이어야 했다. 자리표시자 `NotReadyScreen`과 문자열 `screen_not_ready`는 지웠다. 남겨 두면 린트가 쓰이지 않는 리소스로 잡는다(KM-092·094에서 두 번 겪었다).
- 새 데이터 경로는 없다. 홈은 라이브러리와 같은 `LibraryViewModel`을 본다. Activity 범위라 탭을 옮겨도 같은 인스턴스이고 질의가 늘지 않는다.
- **단위 검사를 새로 만들지 않았다.** 새로 판단하는 규칙이 없고 기존 상태를 다른 모양으로 그리기만 한다. 만들 수 있는 것은 `take` 같은 사소한 함수를 위한 검사뿐이어서, 대신 실기기에서 확인했다.
- 실기기 SM-T220 / Android 14 확인: 새로 시작하면 홈이 먼저 열리고 빈 상태에서 "아직 들은 곡이 없습니다..." 한 줄과 "검색으로 가기"가 나온다. 그 버튼이 검색 탭으로 보낸다. 검색해서 한 곡을 듣고 즐겨찾기와 재생목록을 만들자 홈에 세 구획이 카드로 나왔다. 즐겨찾기 카드를 눌러 재생됐고, 재생목록 카드는 그 재생목록 화면을 열었다. 가로 화면에서도 구획이 그대로 나온다.
- 확인 후 최근 재생·즐겨찾기·재생목록을 지우고 재생을 멈췄으며 자동 회전을 되돌렸다. 화면 캡처는 captures/km-151에 보관했다(저장소 추적 대상 아님).
- **시행착오.** 검증에서 계측이 두 번 실패했다. 처음에는 `SeekTest`가 재생 시작을 기다리다 시간이 지났고(단독 실행은 통과), 다음에는 `ActivityRecreationTest`가 "세션에 연결되지 않았다"로 실패했다. 두 번째는 재현됐다.
- 실패 메시지에 상태를 실어 다시 돌리자 원인이 나왔다: `연결 상태: Disconnected, 재생 상태: Idle, Activity 단계: {STOPPED=1}`. **화면이 꺼져 있으면 띄운 Activity가 곧바로 STOPPED로 가고, `onStop`이 세션 연결을 끊는다.** 오래 걸리는 검증을 무인으로 돌리는 동안 화면이 꺼져 그 조건이 만들어졌다.
- 코드의 결함이 아니라 KM-133에서 내가 만든 **검사의 결함**이다. 사람이 화면을 만지고 있을 때만 통과하는 검사였다. 검사가 시작할 때 `input keyevent KEYCODE_WAKEUP`과 `wm dismiss-keyguard`로 스스로 조건을 만들고, Activity가 앞에 나올 때까지 기다린 뒤 연결을 보도록 고쳤다. 실패 메시지에는 그때의 상태를 함께 싣는다.
- 고친 검사를 화면을 일부러 끈 상태(`mWakefulness=Dozing`)에서 돌려 통과하는 것을 확인했다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 22초). 단위 171개·실제 계약 7개·실기기 계측 46개, 실패/오류 0. 린트 오류 0·경고 22.
- 결정 ADR-066. 신규 의존성 없음. 다음은 KM-152 Dark theme다.

## KM-152 완료 — Dark theme

- 브랜치 `codex/KM-152-dark-theme`. `ThemePreference`(시스템·밝게·어둡게)와 DataStore 저장은 초기 작업에서 이미 있었고 아무도 읽지 않고 있었다. 이 작업이 그것을 화면에 연결했다.
- 색은 Material 3 기본 배색을 그대로 쓴다. 고유한 브랜드 색을 정한 적이 없고 기본 배색은 대비가 이미 맞춰져 있다.
- 합치는 규칙은 `ThemePreference.isDark(systemDark)` 한 줄이다. 시스템은 기기를 따르고, 밝게·어둡게는 기기 설정과 달라도 그것을 따른다. 단위 3개로 고정했다(171개 → 174개).
- **시스템 표시줄 아이콘 색이 앱 색을 따르게 고쳤다.** 이전에는 `SystemBarStyle.light`로 고정돼 있어 어두운 화면에서 아이콘이 보이지 않았다. 화면 색이 바뀔 때마다 `enableEdgeToEdge`를 다시 건다. `WindowInsetsControllerCompat`는 선언하지 않은 의존성(androidx.core)에 있어 쓰지 않았다.
- 시작 창에 밝게·어둡게 두 벌을 뒀다(`values/themes.xml`, `values-night/themes.xml`). 앱이 그리기 전의 창 색이라 정하지 않으면 어두운 화면으로 들어갈 때 흰 창이 번쩍인다. 이 값은 시스템 설정만 따를 수 있다.
- 고르는 자리는 재생 화면에 임시로 뒀다. WiFi 전용 재생과 함께 KM-153에서 설정 화면으로 옮긴다. 지금 여기 두는 것은 고른 값이 적용되는지 볼 자리가 없기 때문이다.
- 앱 전체 설정은 새 `SettingsViewModel`이 갖는다. KM-153이 나머지 설정을 여기에 더한다.
- 실기기 SM-T220 / Android 14에서 네 가지를 모두 확인했다: 시스템 밝게 + 시스템 설정, 시스템 어둡게 + 시스템 설정(어둡게 그려짐), **시스템 어둡게 + 앱 밝게**(앱만 밝게, 표시줄 글자는 어둡게), **시스템 밝게 + 앱 어둡게**(앱만 어둡게, 표시줄 글자는 밝게). 네 경우 모두 표시줄이 읽힌다.
- 주요 화면의 대비도 어두운 색에서 확인했다: 검색 결과(제목·아티스트·길이), 라이브러리 빈 화면, 재생 화면, 미니 플레이어와 하단 탭. 읽히지 않는 곳은 없었다.
- 캡처를 한 번 잘못 읽었다. 설정을 바꾼 직후 3초 만에 찍은 화면에서는 표시줄이 아직 이전 색이었다. 5초 뒤 다시 찍어 제대로 바뀐 것을 확인했다. 표시줄 색은 창 속성이라 Compose 프레임보다 늦게 반영된다.
- 확인 후 화면 색을 시스템으로, 기기 야간 모드를 끔으로, 화면 꺼짐 시간을 30000으로 되돌렸다. 화면 캡처는 captures/km-152에 보관했다(저장소 추적 대상 아님).
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 32초). 단위 174개·실제 계약 7개·실기기 계측 46개, 실패/오류 0. 린트 오류 0·경고 22.
- 결정 ADR-067. 신규 의존성 없음. 다음은 KM-153 Settings다. 거기서 임시로 둔 화면 색과 WiFi 전용 재생을 옮긴다.

## KM-153 완료 — Settings (M9 완료)

- 브랜치 `codex/KM-153-settings`. PRD가 정한 네 가지만 뒀다: 화면 색, 캐시 상한, 캐시 지우기, 재생 기록. 작업 지시가 "쓰이지 않는 설정은 두지 않는다"이므로 더 넣지 않았다.
- 여기로 옮긴 것: WiFi 전용 재생(KM-137)과 화면 색(KM-152). 둘 다 재생 화면에 임시로 두었던 것이다. **반복은 옮기지 않았다.** 지금 듣는 것을 바꾸는 조작이고 셔플과 나란히 있어야 뜻이 통한다. 재생 화면에는 요금제 때문에 막혔다는 안내만 남았다. 그것은 설정이 아니라 지금 재생에 일어난 일이다.
- 설정으로 가는 길은 홈 화면의 버튼 하나다. 하단 탭은 PRD 35가 세 개로 정해 두었고 홈이 시작 화면이라 어디에서든 한 번 눌러 올 수 있다.
- **캐시 상한**은 새 설정이다. `PlaybackCache`가 만들 때 저장된 값을 읽는다. Media3의 evictor는 만든 뒤에 상한을 바꿀 수 없고 `SimpleCache`는 프로세스에 하나뿐이라, 고른 값은 다음 실행부터 적용된다. 화면은 지금 걸린 상한으로 사용량을 말하고, 고른 값과 다를 때만 안내를 붙인다. `PlaybackCache.MAX_BYTES` 상수는 없애고 `CacheLimit`로 옮겼다.
- **재생 기록 켜기/끄기**도 새 설정이다. KM-115에서 여기로 미뤄 둔 것이다. 끄면 그때부터 남기지 않고 이미 남은 기록은 두었다. 설정을 끄는 것과 기록을 버리는 것은 다른 뜻이다.
- **시행착오(실기기에서 찾은 결함).** 처음에는 예약된 작업 안에서 `historyEnabled`를 물어봤다. 껐다 켜니 지금 듣고 있는 곡이 기록되지 않았다. 원인은 꺼진 채로 예약된 작업이 일찍 돌아 나오면서 "이미 처리했다"는 표시(`historyJob`)만 남긴 것이다. 위치가 기준보다 앞으로 돌아가지 않으면 표시가 풀리지 않아 다시 남길 기회가 없었다.
- 고침: 서비스가 설정 값을 받아 들고 있다가 **예약 전에** 본다. 꺼져 있으면 기다리던 것도 접는다. 그리고 설정이 바뀔 때 판단을 한 번 더 돌려, 켜는 순간 지금 곡부터 판단하게 했다. 실기기에서 80초를 들은 뒤 켜자 곧바로 최근 재생에 나타났다.
- 단위 9개 추가: 크기 표기 4개(`ByteFormatTest`), 기록 설정 2개·캐시 상한 3개(`DataStoreSettingsRepositoryTest`). 174개 → 183개.
- `SettingsRepository`에 두 값이 늘어 검사용 대역 구현 세 곳(`NetworkPolicyTest`, `MeteredPlaybackBlockTest`, `SearchToPlayTest`)을 함께 고쳤다.
- 실기기 SM-T220 / Android 14 확인:
  - 화면 색: KM-152에서 확인한 세 가지가 설정 화면에서도 그대로 동작한다.
  - 재생 기록: 끈 상태로 18초를 들어도 최근 재생이 비어 있다. 켜고 다시 들으면 남는다. 듣는 중에 켜도 그 곡이 남는다.
  - 캐시 상한: 512MB를 고르면 안내가 나오고, 앱을 다시 시작하면 "사용 중 0KB / 512MB"로 바뀌며 안내가 사라진다. 256MB로 되돌리고 다시 시작해 원래대로 돌아온 것도 확인했다.
  - 캐시 지우기: 재생 뒤 "사용 중 3MB / 512MB"였고 지우니 "0KB"가 됐다.
- 확인 후 상한을 256MB로, 기록을 켜짐으로, 화면 색을 시스템으로 되돌리고 최근 재생을 지우고 재생을 멈췄다. 화면 꺼짐 시간도 30000으로 되돌렸다. 화면 캡처는 captures/km-153에 보관했다(저장소 추적 대상 아님).
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 49초). 단위 183개·실제 계약 7개·실기기 계측 46개, 실패/오류 0. 린트 오류 0·경고 22.
- 결정 ADR-068. 신규 의존성 없음. **M9의 화면 작업 완료.** 남은 것은 KM-154(접근성)·KM-155(OSS 고지)·KM-200(최종 게이트)이다.

## KM-154 완료 — Accessibility pass

- 브랜치 `codex/KM-154-accessibility`. 고치기 전에 **재어 봤다.** 세 인수 조건 중 둘은 이미 충족돼 있었다.
- **아이콘 이름:** 코드의 모든 `Icon`에 `contentDescription`이 붙어 있거나 장식용으로 명시적 null이었다(앨범 이미지, 하단 탭 아이콘 — 탭은 옆에 글자가 있다). 재생 조작 중 이전·재생·다음은 아이콘이 아니라 글자 버튼이라 그 자체가 이름이다.
- **터치 영역:** 기기에서 `uiautomator`로 clickable 노드의 크기를 재어 dp로 환산했다(밀도 213 → px/1.33). 설정 화면의 칩·스위치·버튼과 대기열의 아이콘 버튼이 모두 **47~48dp**였다. 칩은 보이는 높이가 32dp인데도 그렇다. Material 3이 구성 요소마다 최소 터치 영역을 강제하기 때문이다. 우리가 만든 클릭 영역(곡 줄·카드·미니 플레이어)은 이미지와 여백 때문에 그보다 크다.
- **큰 글꼴:** `font_scale 1.5`로 올려 설정·재생·대기열·홈 화면을 봤다. 잘리거나 겹치는 곳이 없었다. 설정 화면의 칩 세 개도 한 줄에 들어갔고 안내 문구는 두 줄로 접혔다.
- 그래서 이 작업에서 고친 것은 **읽는 쪽이 이름을 알 수 없던 곳**이다.
  - 커스텀 클릭에 `onClickLabel`을 붙였다. 곡 줄·홈 카드·대기열 줄은 "재생", 재생목록은 "열기", 미니 플레이어 줄은 "전체 화면 플레이어 열기", 담기 항목은 "재생목록에 담기". 없으면 "두 번 눌러 실행"으로만 읽힌다.
  - 재생 상태 줄을 live region으로 표시했다. 재생·일시정지·오류가 그 줄로 바뀌므로 바뀔 때 읽어 준다.
  - 위치 줄을 한 덩어리로 묶어 "재생 위치 0:09 / 2:00"으로 읽히게 했다. 기기에서 값이 따라 바뀌는 것을 확인했다.
- **시행착오.** 처음에는 슬라이더에 직접 이름을 붙였다. 기기의 접근성 트리를 보니 SeekBar 노드와 이름만 가진 노드가 **따로** 생겼고(같은 좌표, 이름 노드는 focusable=false) `mergeDescendants = true`로도 합쳐지지 않았다. 읽히지 않을 수 있는 것을 "고쳤다"고 남겨 두지 않기로 하고 걷어냈다. 대신 바로 아래 위치 줄이 그 뜻을 말한다.
- 새 단위 검사는 없다. 접근성 의미는 Compose 트리에 있는 것이라 일반 단위 검사로 볼 수 없고, 이 저장소에는 Compose UI 검사 도구가 없다(새 의존성을 들이지 않았다). 기기의 접근성 트리 덤프로 확인했다.
- 확인 후 `font_scale`을 1.0으로, 화면 꺼짐 시간을 30000으로 되돌렸다. 화면 캡처는 captures/km-154에 보관했다(저장소 추적 대상 아님).
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 44초). 단위 183개·실제 계약 7개·실기기 계측 46개, 실패/오류 0. 린트 오류 0·경고 22.
- 결정 ADR-069. 신규 의존성 없음. 다음은 KM-155 OSS notices다.

## KM-155 완료 — OSS notices

- 브랜치 `codex/KM-155-oss-notices`. 코드 변경 없음. THIRD_PARTY_NOTICES.md를 새로 만들었다.
- 목록은 기억이 아니라 빌드에서 뽑았다. `gradlew :app:dependencies --configuration releaseRuntimeClasspath`로 release 빌드에 들어가는 전이 의존성 전체를 얻고(238개), 각 아티팩트의 라이선스를 Gradle 캐시의 **POM licenses 항목**에서 읽었다. POM에 없으면 같은 버전의 부모 POM을 따라갔다.
- 결과: **Apache-2.0 236개, BSD-3-Clause 1개, MIT 1개. GPL·LGPL·AGPL 없음.**
  - BSD-3-Clause는 `androidx.datastore:datastore-preferences-external-protobuf:1.2.1`이다. DataStore가 쓰는 protobuf 런타임을 다시 묶은 것이다.
  - MIT는 `org.slf4j:slf4j-api:2.0.17`이다. 이 POM에는 라이선스 항목이 없어 같은 버전 `slf4j-bom`에서 확인했고 그 경로도 문서에 적었다.
- 빌드에만 쓰이는 것(KSP 처리기·검사 라이브러리·Gradle 플러그인)은 배포물에 들어가지 않아 뺐고, 뺀 이유를 문서에 적었다.
- 라이선스 원문은 배포물에 넣지 않았다. 개인용 sideload이며 재배포하지 않기 때문이다(PRD 1·9). 대신 **재배포하게 되면 무엇을 해야 하는지**(Apache-2.0 4(a)·4(d)의 사본·NOTICE 포함, BSD·MIT 표시 의무) 문서 안에 적었다.
- 버전만 정하는 `*-bom` 세 개는 코드가 없지만 목록에 남기고 그렇다고 표시했다.
- README의 문서 목록에 이 파일을 넣었다. 아무도 가리키지 않는 고지 문서는 없는 것과 같다. README 본문 갱신은 KM-156이다.
- `gradlew.bat test lint assembleDebug assembleRelease sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(1분 15초). `app/` 아래 변경이 없어 실기기 계측은 KM-154의 46개 통과 결과가 그대로 유효하다.
- 결정 ADR-070. 다음은 KM-156 README다. 지금 README의 "현재 상태"는 KM-039 시점에 머물러 있어 그 작업에서 전체를 다시 쓴다.

## KM-156 완료 — README

- 브랜치 `codex/KM-156-readme`. 코드 변경 없음. README를 다시 썼다.
- 손대기 전 README의 "현재 상태"는 **KM-039 시점**에 멈춰 있었다. "검색은 아직 앱 화면과 연결하지 않았으며 라이브러리 화면도 없다"고 적혀 있었다. 지금은 검색·재생·대기열·즐겨찾기·재생목록·최근 재생·홈·라이브러리·설정·어두운 화면이 모두 동작한다. 처음 보는 사람에게 거짓을 말하는 문서였다.
- 인수 조건 다섯 가지를 절로 나눠 채웠다.
  - **project purpose**: "무엇을 하는 앱인가". 기능 목록이 아니라 무엇을 할 수 있는지로 썼다. 서버도 계정도 없고 기록은 기기 안에만 있다는 것을 함께 적었다.
  - **personal sideload boundary**: 왜 그 전제가 설계를 정하는지로 썼다. 스토어 배포 제외·로그인 없음·영구 다운로드 없음·스트림 URL 미저장·공급자 프로토콜은 관찰값·라이선스 고지 위치.
  - **build instructions**: 기존 절을 유지하되 필수 검증을 지금 쓰는 한 줄로 갈았다. 이전에는 `test`·`lint`·`assembleDebug`·`assembleRelease` 네 줄만 있고 계측·계약 검사가 빠져 있었다. 통과 수치도 KM-071 시점(단위 73개)에서 현재(183·7·46)로 고쳤다.
  - **architecture summary**: 세 겹과 다섯 가지 규칙(재생 소유권·MusicSource 격리·불변 상태·Room/DataStore 분담·캐시 위치)으로 정리하고 각각 ADR을 가리켰다. 이전의 "설계 방향" 두 문장은 이 절이 대신하므로 지웠다.
  - **known limitations**: 아홉 가지를 적었다. 블루투스 미확인, 재생 불가 곡에서 멈춤, 셔플 순서 표시 불가, 캐시 상한의 다음 실행 적용, 슬라이더 접근성 이름, 긴 제목에서 칩이 밀림, 어두운 화면의 첫 프레임, OEM 절전 미재현, progressive 형식의 데이터 사용량. 모두 이 저장소의 기록에서 나온 것이며 짐작을 적지 않았다.
- 디렉터리 구조도 갱신했다. 이전 트리에는 `feature/player`만 있고 home·library·search·settings·navigation·ui/format·values-night·THIRD_PARTY_NOTICES.md가 없었다.
- 한 곳을 고쳤다. 스트림 URL 미저장을 설명하며 "데이터베이스에 url이 들어간 열이 없다"고 쓸 뻔했으나, 실제 검사는 `artwork_url`을 예외로 둔다. 검사 코드를 확인하고 문구를 정확히 고쳤다.
- `gradlew.bat test lint assembleDebug assembleRelease sourceContractTest -PsourceContractUseWindowsTrust=true --continue`: PASS, 종료 코드 0(29초). `app/` 아래 변경이 없어 실기기 계측은 KM-154의 46개 통과 결과가 그대로 유효하다.
- ADR을 남기지 않았다. 설계를 정한 것이 없고 있는 사실을 적었을 뿐이다. 다음은 KM-157 릴리스 서명 설정이다.

## KM-157 완료 — Release signing configuration

- 브랜치 `codex/KM-157-release-signing`. 서명 설정을 붙이되 저장소에는 키도 비밀번호도 들어오지 않게 했다.
- 읽는 곳은 두 군데다. 프로젝트 루트의 `keystore.properties`(`.gitignore` 대상)와 환경 변수 넷(`KEUNEY_KEYSTORE_FILE`·`KEUNEY_KEYSTORE_PASSWORD`·`KEUNEY_KEY_ALIAS`·`KEUNEY_KEY_PASSWORD`). 파일이 있으면 파일을, 없으면 환경 변수를 본다.
- 서명 정보가 없어도 빌드는 성공한다. 키가 없는 곳에서도 `test`·`lint`·`assembleRelease`가 돌아야 한다. 그 대신 release를 만들 때 **왜 서명이 빠졌는지** 한 줄로 찍는다. 서명 실패는 조용해서, 이름(`app-release-unsigned.apk`)을 눈여겨보지 않으면 모른다.
- **시행착오(그리고 그 덕에 고친 것).** 검증용 `keystore.properties`를 PowerShell `Out-File -Encoding utf8`로 만들었더니 서명이 걸리지 않았다. 원인은 **BOM**이었다. `Properties.load(InputStream)`은 ISO-8859-1로 읽으므로 BOM이 첫 키 이름에 섞여 `storeFile` 값이 비었고, 조용히 서명 없이 빌드됐다.
- 사용자도 편집기에 따라 같은 일을 겪는다. 그래서 읽는 쪽을 고쳤다. UTF-8로 읽고 BOM을 떼어 낸다(비밀번호에 한글·특수문자가 있어도 읽힌다). 그리고 상태를 `SigningState.Missing(reason)`으로 들고 다니며 이유를 말하게 했다.
- 알려 주는 경우 세 가지를 실제로 만들어 확인했다.
  - 설정이 아예 없을 때: "서명 정보가 없다(keystore.properties도, 환경 변수도 없다)".
  - 값이 비었을 때: "keystore.properties 의 storePassword 값이 비어 있다".
  - 경로가 어긋났을 때: 역슬래시를 쓴 `C:\keys\typo.jks`가 `C:keys` + 탭 + `ypo.jks`로 망가진 것을 그대로 보여 주고 `/`를 쓰라고 말한다. `.properties`에서 역슬래시는 이스케이프 문자이고 \t는 탭이 된다.
- 로그에는 키 파일 **이름만** 찍는다. 경로 전체·별칭·비밀번호는 찍지 않는다. 빌드 로그는 남고 공유된다.
- 서명되는 경로도 실제로 확인했다. **버릴 키**를 저장소 밖(스크래치 디렉터리)에 만들어 걸었더니 결과물이 `app-release.apk`(17,259,613바이트)로 나왔고 `apksigner verify --print-certs`가 인증서를 출력했다. 확인 뒤 그 키와 `keystore.properties`를 지웠고 `git status`에 남은 것이 없음을 확인했다. 사용자의 키는 사용자가 만든다.
- README에 "릴리스 서명" 절을 넣었다. 키 만들기(`-storepass` 생략해 명령 기록에 비밀번호가 남지 않게), `keystore.properties` 형식과 경로 주의, 환경 변수, 확인 방법, 키를 잃으면 설치된 앱을 갱신할 수 없다는 사실까지 적었다.
- `gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest sourceContractTest -PsourceContractUseWindowsTrust=true --continue --console=plain`: PASS, 종료 코드 0(5분 24초). 단위 183개·실제 계약 7개·실기기 계측 46개, 실패/오류 0. 린트 오류 0·경고 22.
- 결정 ADR-071. 신규 의존성 없음. 다음은 KM-158 Release APK다. 거기서는 서명한 APK를 실기기에 설치해 스모크 검사를 한다. 사용자의 키가 필요하므로 진행 전에 물어봐야 한다.
