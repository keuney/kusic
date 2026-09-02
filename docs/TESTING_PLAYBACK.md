# 재생 기반 회귀 검사

KM-040. 테스트 음원을 사용하는 M2 재생 기반의 반복 검증 절차다. 외부 음악 검색과 스트리밍은 이 검사의 대상이 아니다.

## 준비

- README의 JDK 17 및 Android SDK 환경을 설정한다.
- 실제 Android 기기에서 개발자 옵션과 USB 디버깅을 켜고 PC 연결 허용 창을 승인한다.
- 아래 명령으로 `device` 상태를 확인한다. `unauthorized`라면 기기 화면에서 연결을 허용한다. `offline`이면 USB 연결 상태를 확인한다.
- 기기를 잠금 해제하고 음량을 적당히 조절한다. 계측 검사는 테스트 APK 설치와 음원 재생, Home 이동, 화면 꺼짐을 수행하므로 실행 중 기기를 조작하지 않는다.
- Gradle 계측 명령은 연결된 여러 기기에서 실행할 수 있다. 원하는 기기 한 대만 연결하고 사용하지 않는 에뮬레이터는 종료한다. 아래 `$serial`은 수동 ADB 명령에만 적용된다.

```powershell
$adb = Join-Path $env:ANDROID_HOME 'platform-tools/adb.exe'
& $adb devices -l
$serial = '여기에 기기 식별자 입력'
```

## 전체 자동 검사

저장소 루트에서 실행한다.

```powershell
.\gradlew.bat test lint assembleDebug connectedDebugAndroidTest --continue --no-daemon --console=plain
```

프로세스 종료 코드 0과 `BUILD SUCCESSFUL`, 각 보고서의 실패·오류·건너뜀 0을 함께 확인한다. 단위 테스트만으로 실기기 성공을 판정하지 않는다. 릴리스 설정을 바꾼 경우 `assembleRelease`도 실행한다.

- 단위 결과: `app/build/test-results/testDebugUnitTest/`
- 계측 결과: `app/build/reports/androidTests/connected/debug/index.html`
- 린트 결과: `app/build/reports/lint-results-debug.html`

재생 기반만 재검사하려면 다음 명령을 사용한다. 변경 작업을 완료할 때는 위 전체 검사도 수행한다.

```powershell
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.keuney.music.core.player --no-daemon --console=plain
```

## 백그라운드 검사

`BackgroundPlaybackTest`는 다음 조건을 자동 확인한다.

1. 실제 Activity에서 테스트 음원을 재생한다.
2. Home으로 이동하고 Activity와 모든 화면/테스트 컨트롤러를 해제한다.
3. 32초 동안 컨트롤러 없이 기다린다.
4. 재연결 전에 서비스가 foreground인지 확인한다.
5. 재연결 후 재생 중이며 위치가 30초 이상 증가했는지 확인한다.

수동 확인 시 아래 일반 APK를 설치하고 앱에서 재생을 누른다. Home으로 이동해 다른 앱을 사용하며 30초 이상 소리가 이어지는지 확인한 후 앱으로 돌아와 위치 증가를 확인한다.

```powershell
& $adb -s $serial install -r app/build/outputs/apk/debug/app-debug.apk
& $adb -s $serial shell am start -W -n com.keuney.music/.MainActivity
& $adb -s $serial shell dumpsys activity services com.keuney.music
```

계측 실행 뒤 앱이 제거된 경우에도 일반 APK를 다시 설치하면 된다. 서비스 출력에서 `MusicService`와 `isForeground=true`를 확인한다.

## 화면 꺼짐 검사

`ScreenOffPlaybackTest`는 재생 후 화면을 끄고 Activity/컨트롤러를 해제한다. 62초간 매초 화면이 꺼져 있음을 검사하고, 화면을 켜기 전에 재연결해 재생 중 상태와 60초 이상 위치 진행을 확인한다. 정리 과정에서 재생을 멈추고 화면을 깨운다.

수동 검사에서는 음원을 처음부터 재생하고 전원 버튼으로 화면을 끈다. 최소 1분 동안 음향이 이어지는지 확인한 뒤 화면을 켜고 재생 위치를 확인한다. 음원은 120초이므로 검사 시작 전에 끝부분에 있지 않은지 확인한다.

화면만 꺼졌다는 사실과 재생 진행을 모두 확인해야 통과다. USB 충전 상태의 단기 검사는 충전 분리·장시간 Doze·다른 제조사 배터리 정책 검증을 대체하지 않는다.

## 알림·잠금화면·오디오 포커스

- `MediaNotificationTest`: 제목·아티스트·기본 이미지·앱 복귀 Intent, 실제 알림 동작의 일시정지/재생을 검사한다.
- 잠금화면: 재생 중 화면을 껐다 켜고 잠금을 해제하기 전에 미디어 카드의 제목과 이미지, 일시정지/재생 버튼을 확인한다. 버튼을 각각 누르고 아래 세션 상태도 확인한다. 보안 잠금 여부를 결과에 기록한다.
- `AudioFocusTest`: 경쟁하는 일시적 포커스 요청에서 위치가 멈추고 반환 후 재개되는지, 영구 요청에서는 반환 후에도 일시정지를 유지하는지 확인한다. 실제 전화·이어폰·Bluetooth 이벤트는 별도로 검사해야 한다.
- 이전/다음은 현재 테스트 항목 하나의 Media3 기본 명령 구조를 사용한다. 여러 곡 이동 검증은 queue 구현 후 수행한다.

## 미디어 세션 확인

```powershell
& $adb -s $serial shell dumpsys media_session
& $adb -s $serial shell dumpsys window policy
```

`package=com.keuney.music`인 세션 블록에서 metadata와 PlaybackState를 읽는다. 다른 앱의 세션과 혼동하지 않는다. `state=3`은 재생, `state=2`는 일시정지, `state=1`은 정지다. 재생 중에는 시간 간격을 두고 position 증가를 확인한다. pause/play 버튼 전후 상태를 비교하고, 음원이 자연 종료한 정지 상태를 버튼 성공으로 판정하지 않는다.

잠금화면 검사에서는 window policy의 keyguard `showing=true`도 확인한다. 제조사/Android 버전에 따라 출력 형식과 미디어 카드 배치가 다를 수 있다. 전체 dumpsys나 logcat은 다른 앱 정보를 포함할 수 있으므로 공유 기록에는 해당 앱의 필요한 상태만 남긴다. 스트림 URL이나 인증 정보를 기록하지 않는다.

## 결과 기록과 종료

검사 날짜, 앱 변경 사항, 모델/API, 실기기/에뮬레이터 구분, USB 충전 여부, 명령·종료 코드·테스트 수, 각 조건 PASS/FAIL, 미검증 항목을 `docs/SEQUENTIAL_RUN.md` 또는 해당 작업 기록에 남긴다. 에뮬레이터만 통과한 백그라운드/화면 꺼짐 검사는 실기기 완료로 표시하지 않는다.

수동 검사 후 앱이나 미디어 카드에서 일시정지한다. 자동 검사는 자체 정리 절차를 수행한다. 실패 시 먼저 실패 보고서와 해당 앱 상태를 확인하고 최소 수정 후 영향 검사와 전체 검사를 다시 실행한다.
