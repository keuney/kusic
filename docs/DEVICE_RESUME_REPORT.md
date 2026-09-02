# 실기기 연결 이후 종합 결과

날짜: 2026-09-02. 사용자 요청에 따라 한 번에 KM 작업 하나씩 구현·검증·기록한 뒤 다음 작업으로 진행했다.

## 완료 내용

- Samsung SM-T220 / Android 14(API 34) 연결을 확인하고 KM-035~040의 실기기 재생 기반을 완료했다.
- KM-050~055의 공통 모델·소스 계약·통신 골격·실제 검색 POC를 완료했다.
- KM-056은 직접 오디오 URL을 얻지 못해 보류했다. KM-057 이후 작업은 시작하지 않았다.
- KM-001/002/003/010의 이전 보류 상태는 재판정하지 않았다.

현재 앱 화면은 내장된 120초 테스트 음원을 재생한다. 실제 검색 코드는 검증했지만 아직 검색 화면이나 실제 음악 재생으로 연결하지 않았다.

## 인수 조건

| 작업 | 확인 내용 | 결과 |
| --- | --- | --- |
| KM-035 | Home 이동·Activity/컨트롤러 해제 후 32초 재생, foreground 서비스 유지 | PASS |
| KM-036 | 62초 동안 화면 꺼짐 유지, 화면을 켜기 전 60초 이상 재생 위치 진행 | PASS |
| KM-037 | 제목·이미지·알림 play/pause, Media3 이전/다음 명령 구조 | PASS |
| KM-038 | 실제 잠금화면 metadata 및 버튼으로 PAUSED → PLAYING | PASS |
| KM-039 | 일시적/영구적 오디오 포커스 손실 상태·복귀 정책, 충돌 없음 | PASS |
| KM-040 | 재생 회귀 문서, background/screen-off 절차, dumpsys 확인법 | PASS |
| KM-050 | 공급자 중립 Track, 검증 로직 없는 불변 메타데이터 | PASS |
| KM-051 | URL·MIME·bitrate·선택적 만료, URL 비노출 표현 | PASS |
| KM-052 | 공통 오류 분류 다섯 종류 | PASS |
| KM-053 | search/getTrack/resolveStream/getRelated, 도메인 타입 한정 | PASS |
| KM-054 | 공급자 DTO 격리, header/context 중앙 관리, UI 의존성 없음 | PASS |
| KM-055 | 실제 검색어 세 종류, 빈 결과/오류/취소 처리, mapper 검사 | PASS |
| KM-056 | 직접 형식의 도메인 변환·URL 비로깅 | 단위 검사 PASS |
| KM-056 | 실제 테스트 곡의 재생 스트림 해석 | FAIL — 미완료 |

알림의 이전/다음은 현재 한 곡의 Media3 기본 구조만 확인했다. 여러 곡 이동은 향후 queue 검증 대상이다. 잠금화면 검사는 암호 없는 스와이프 잠금 조건이다.

## 변경 파일

이번 실기기 재개 이후 추가·수정한 파일이다. 저장소에는 그 이전 작업의 untracked 파일도 있으며 아래 목록이 전체 git status와 같다는 의미는 아니다.

재생 구현·실기기 검사:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/kotlin/com/keuney/music/core/player/MusicService.kt`
- `app/src/main/kotlin/com/keuney/music/core/player/PlaceholderArtwork.kt`
- `app/src/test/kotlin/com/keuney/music/core/player/PlaybackStateTest.kt`
- `app/src/androidTest/kotlin/com/keuney/music/core/player/ScreenOffPlaybackTest.kt`
- `app/src/androidTest/kotlin/com/keuney/music/core/player/MediaNotificationTest.kt`
- `app/src/androidTest/kotlin/com/keuney/music/core/player/AudioFocusTest.kt`

공통 모델·소스 구현:

- `app/src/main/kotlin/com/keuney/music/core/model/Track.kt`
- `app/src/main/kotlin/com/keuney/music/core/model/PlayableStream.kt`
- `app/src/main/kotlin/com/keuney/music/core/model/AppError.kt`
- `app/src/main/kotlin/com/keuney/music/data/source/MusicSource.kt`
- `app/src/main/kotlin/com/keuney/music/data/source/providerA/ProviderAClient.kt`
- `app/src/main/kotlin/com/keuney/music/data/source/providerA/ProviderAConfig.kt`
- `app/src/main/kotlin/com/keuney/music/data/source/providerA/dto/ProviderAContext.kt`
- `app/src/main/kotlin/com/keuney/music/data/source/providerA/ProviderASearch.kt`
- `app/src/main/kotlin/com/keuney/music/data/source/providerA/mapper/ProviderASearchMapper.kt`
- `app/src/main/kotlin/com/keuney/music/data/source/providerA/ProviderAStreamResolver.kt`
- `app/src/main/kotlin/com/keuney/music/data/source/providerA/mapper/ProviderAStreamMapper.kt`

단위·외부 계약 검사:

- `app/src/test/kotlin/com/keuney/music/core/model/PlayableStreamTest.kt`
- `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderAClientTest.kt`
- `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderASearchMapperTest.kt`
- `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderASearchTest.kt`
- `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderASearchSourceContractTest.kt`
- `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderAStreamMapperTest.kt`
- `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderAStreamResolverTest.kt`
- `app/src/test/kotlin/com/keuney/music/data/source/providerA/ProviderAStreamSourceContractTest.kt`

빌드·문서:

- `app/build.gradle.kts`
- `README.md`
- `TASKS.md`
- `docs/DECISIONS.md`
- `docs/SEQUENTIAL_RUN.md`
- `docs/TESTING_PLAYBACK.md`
- `docs/DEVICE_RESUME_REPORT.md`

로그와 화면 증거는 Git 제외된 `captures/`에 보관했다. Gradle 산출물과 기기 임시 캡처는 소스 변경 목록에서 제외했다.

## 실행 명령과 검증 결과

각 명령에 사용한 프로세스 환경:

```powershell
$env:JAVA_HOME = 'C:/Program Files/Eclipse Adoptium/jdk-17.0.18.8-hotspot'
$env:ANDROID_HOME = 'C:/Users/UUH/AppData/Local/Android/Sdk'
$env:GRADLE_USER_HOME = 'D:/uuh_workspace/keuney_music/.gradle/user-home'
```

사용자/시스템 전역 환경 변수는 바꾸지 않았다. 아래는 반복 실행의 공통 명령이며 작업별 최종 로그는 SEQUENTIAL_RUN.md에 기록했다.

```powershell
.\gradlew.bat test lint assembleDebug --continue --no-daemon --console=plain
.\gradlew.bat test lint assembleDebug connectedDebugAndroidTest --continue --no-daemon --console=plain
.\gradlew.bat test lint assembleDebug assembleRelease connectedDebugAndroidTest --continue --no-daemon --console=plain
.\gradlew.bat test lint assembleDebug assembleRelease --continue --no-daemon --console=plain
.\gradlew.bat sourceContractTest --no-daemon --console=plain
.\gradlew.bat sourceContractTest --tests '*koreanQuery' --no-daemon --console=plain
.\gradlew.bat sourceContractTest -PsourceContractUseWindowsTrust=true --no-daemon --console=plain
.\gradlew.bat test lint assembleDebug assembleRelease sourceContractTest -PsourceContractUseWindowsTrust=true --continue --no-daemon --console=plain
```

| 검사 | 최종 결과 | 근거 |
| --- | --- | --- |
| 일반 단위 검사 | 28개 PASS, 실패/오류 0 | `app/build/test-results/testDebugUnitTest/` |
| 린트 | 오류 0, 경고 18 | `app/build/reports/lint-results-debug.html` |
| assembleDebug | PASS | 디버그 APK 생성 |
| assembleRelease | PASS | 미서명 릴리스 APK 생성 |
| 실기기 계측 회귀 | 10개 PASS | KM-040, SM-T220, 2분 36초 |
| 공개 검색 계약 | 3개 PASS | 아이유 / BTS Dynamite / Bach |
| 실제 스트림 계약 | 1개 FAIL | 직접 오디오 스트림 해석 실패 |
| KM-056 계약 포함 전체 명령 | 종료 코드 1 | `captures/km-056-verification.log`, 51초 |
| KM-056 일반 검사·빌드만 분리 실행 | 종료 코드 0, PASS | `captures/km-056-final-build.log`, 10초 |

외부 스트림 검사의 실패를 일반 빌드 성공으로 덮어 완료 처리하지 않았다. 실제 계약 검사는 일반 단위 검사와 분리되어 명시적으로 실행한다.

기기 확인·수동 검증에 사용한 ADB 명령 종류(아래 `<기기>`는 연결된 실제 식별자):

```text
adb devices -l
adb -s <기기> get-state
adb -s <기기> shell getprop
adb -s <기기> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <기기> shell am start -W -n com.keuney.music/.MainActivity
adb -s <기기> shell input keyevent KEYCODE_WAKEUP
adb -s <기기> shell input keyevent KEYCODE_SLEEP
adb -s <기기> shell input swipe <화면 좌표>
adb -s <기기> shell input tap <확인한 버튼 좌표>
adb -s <기기> shell cmd statusbar expand-notifications
adb -s <기기> shell cmd statusbar collapse
adb -s <기기> shell dumpsys media_session
adb -s <기기> shell dumpsys window policy
adb -s <기기> shell dumpsys activity services com.keuney.music
adb -s <기기> shell screencap -p <기기 임시 PNG 경로>
adb -s <기기> pull <기기 파일> <captures 파일>
adb -s <기기> shell uiautomator dump <기기 임시 XML 경로>
adb -s <기기> logcat -d -v brief -s TestRunner:I
adb -s <기기> reconnect
```

`uiautomator dump`는 재생 애니메이션 때문에 idle timeout이 발생해 성공 증거로 사용하지 않았다. 스크린샷과 실제 세션 상태로 검증했다. 한 차례 USB offline은 해당 기기의 reconnect로 복구했다. 자연 종료와 겹친 첫 잠금화면 조작은 인정하지 않고 처음부터 다시 검사했다.

파일·환경·소스 진단에는 `Get-Content`, `Get-ChildItem`, `rg`, 테스트/린트 XML 파싱, `git -c safe.directory=D:/uuh_workspace/keuney_music status --short`, 공개 페이지/API에 대한 `Invoke-WebRequest`/`Invoke-RestMethod`와 필요한 필드만 추출하는 정규식을 사용했다. 파일 편집은 apply_patch로 수행했다. 스트림 URL·전체 원문·쿠키·방문자 정보는 진단 출력이나 영구 파일로 남기지 않았다.

## 기술 결정

- ADR-025~027: Media3의 wake lock·기본 알림·오디오 포커스 관리를 사용한다. ExoPlayer는 계속 MusicService만 소유한다.
- Track과 PlayableStream을 분리하고 스트림 객체의 문자열 표현에서 URL을 가린다.
- ADR-028: MusicSource의 도메인 계약을 구현한다.
- ADR-029~030: Provider A를 내부 패키지로 격리한다. 음악 전용 검색의 빈 결과를 확인한 뒤 일반 공개 WEB 검색으로 변경해 실제 검색 세 종류가 통과했다.
- JDK의 TLS 신뢰 오류는 선택적 Windows-ROOT 계약 테스트 설정으로 해결했다. TLS 검증과 호스트 검증을 끄지 않았고 인증서를 설치하지 않았다.
- ADR-031: 직접 URL 없는 별도 전송 주소를 PlayableStream 성공 값으로 취급하지 않는다. 새 dependency·로그인·다운로드·Provider B 구현은 추가하지 않았다.

## 남은 위험과 다음 단계

1. KM-056의 실제 스트림 해석이 실패한다. 공개 플레이어 설정 반영 후 상태 OK와 오디오 형식 메타데이터는 받지만 직접 URL/signatureCipher가 없고 serverAbrStreamingUrl이 있다. 현재 구현의 일반 오디오 URL 경로로는 사용할 수 없다.
2. 계속 진행하려면 Provider A의 재생 전송 방식에 대한 추가 검증 또는 Provider B 평가 순서의 조정이 필요하다. 현재 KM-064는 KM-059 실패를 조건으로 하므로 임의로 건너뛰지 않았다. 실패 테스트를 통과로 바꾸거나 KM-057 이후를 구현하지 않았다.
3. clientVersion/signatureTimestamp는 공개 클라이언트의 현재 관찰값으로 고정되어 변경에 취약하다. Provider A는 아직 최종 채택하지 않았다.
4. 실기기 결과는 USB 충전 중인 한 삼성 기기의 단기 검사다. 실제 통화, Bluetooth, 헤드셋 분리, 충전 분리 상태의 장시간 절전, 다른 OEM/API 조합과 보안 잠금화면은 미검증이다. 외부 소스 계약은 JVM에서 실행했으며 Android 기기에서의 실제 소스 요청/TLS는 아직 검증하지 않았다.
5. 린트 경고 18개는 남아 있다. 릴리스 APK는 미서명이다. 원격 CI 실행·commit·push는 하지 않았다. Git 상태 확인에는 기존 전역 ignore 파일 권한 경고가 있었다.

디버그 APK: `app/build/outputs/apk/debug/app-debug.apk`. 전체 순차 기록은 `docs/SEQUENTIAL_RUN.md`, 반복 실기기 검사 절차는 `docs/TESTING_PLAYBACK.md`를 참고한다.

최종 디버그 APK를 연결된 기기에 install -r로 설치해 Success를 확인했고 MainActivity 실행은 Status ok였다. 음원을 자동 재생하지 않았다.
