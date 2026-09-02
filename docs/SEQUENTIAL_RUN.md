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
