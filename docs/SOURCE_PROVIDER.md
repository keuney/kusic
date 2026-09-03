# Source Provider Gate 결과

KM-059의 판정 기록이다. 대상은 Provider A(InnerTube 호환 공개 엔드포인트)다.

## 판정: PASS

v0.1의 source로 Provider A를 채택한다. 근거와 조건은 아래와 같으며 기술 결정은 ADR-037에 기록했다.

## 검사 조건

- 날짜: 2026-09-02
- 기기: Samsung SM-T220 / Android 14(API 34), WiFi 연결
- 검색어 5종에서 상위 결과를 모아 중복 없는 10곡을 구성했다: 아이유 좋은 날 / BTS Dynamite / Beethoven Symphony No 9 full / Queen Bohemian Rhapsody / Jazz piano long mix
- 소스 판정은 `sourceContractTest`, 재생 판정은 `connectedDebugAndroidTest`로 수행했다.

## 10곡 판정 결과

`해석`은 재생 가능한 스트림을 얻었는지, `중간구간`은 파일 전체 길이의 절반 지점에 Range 요청을 보내 206을 받았는지다. 앞부분만 받아도 통과하는 것을 막기 위해 중간 지점을 쓴다. URL은 기록하지 않는다.

| 트랙 | 아티스트 | 길이 | 해석 | 크기 | 중간구간 |
| --- | --- | --- | --- | --- | --- |
| 아이유(IU) - 좋은 날 [가사/Lyrics] | 웅키 | 236초 | PASS | 8,686KB | 206 PASS |
| IU (아이유) _ Good Day (좋은 날) | 1theK (원더케이) | 358초 | PASS | 24,855KB | 206 PASS |
| 아이유 - 좋은 날 [유희열의 스케치북] | KBS Kpop | 256초 | PASS | 18,088KB | 206 PASS |
| BTS 'Dynamite' Official MV | HYBE LABELS | 224초 | PASS | 8,344KB | 206 PASS |
| BTS 'Dynamite' @ America's Got Talent | BANGTANTV | 202초 | PASS | 16,275KB | 206 PASS |
| BTS Dynamite Lyrics | Jaeguchi | 320초 | PASS | 4,265KB | 206 PASS |
| Beethoven - Symphony No. 9 | EuroArtsChannel | 4,707초 | PASS | 249,656KB | 206 PASS |
| Muti Conducts Beethoven 9 | Chicago Symphony | 4,883초 | PASS | 249,495KB | 206 PASS |
| Symphony No. 9 ~ Beethoven | Evan Bennet | 3,939초 | PASS | 29,470KB | 206 PASS |
| Queen – Bohemian Rhapsody | Queen Official | 360초 | PASS | 17,083KB | 206 PASS |

- 해석 10/10, 중간 구간 요청 10/10
- 서로 다른 아티스트 10종 (요구 조건: 복수 아티스트)
- 가장 긴 곡 4,883초 = 81분 (요구 조건: 긴 곡 1개 이상)

첫 실행에서 1곡의 중간 구간 요청이 예외로 실패했으나 재실행에서 206으로 통과했다. 응답 코드가 아닌 전송 예외였으므로 일시적 네트워크 오류로 판단한다. 공급자의 구조적 거부와 구분하기 위해 검사에 응답 코드를 그대로 기록하도록 했다.

## 기기 재생 판정

| 항목 | 확인 내용 | 결과 |
| --- | --- | --- |
| 큐 재생 | 두 곡을 대기열에 넣고 첫 곡 끝으로 이동, 자동으로 다음 곡으로 전환되어 재생 지속 | PASS |
| 긴 곡 재생 | 81분 트랙을 재생하고 종료 2분 전 지점으로 이동, 이어 재생 확인 | PASS |
| 검색 → 선택 → 재생 | KM-058에서 확인. Home 이동 후에도 유지 | PASS |
| 재생 지속 | 95초 연속 재생, Home 이후 130초까지 유지 | PASS |

## 채택 조건과 알려진 한계

채택하되 아래를 v0.1의 알려진 제약으로 명시한다.

1. **오디오 전용 스트림을 쓸 수 없다.** 오디오 전용 adaptive 주소는 어떤 구간 요청에도 거부된다. 영상이 함께 들어 있는 progressive 형식만 재생 가능하다(ADR-034). 재생 시 영상 트랙은 끈다.
2. **대역폭이 오디오 전용의 약 3배다.** 곡당 8~25MB. 완화 수단으로 256MB LRU 캐시(KM-134)와 WiFi 전용 재생 설정(KM-137)을 제공한다.
3. **비공개 프로토콜에 의존한다.** clientVersion, signatureTimestamp, 클라이언트 종류별 설정은 모두 공개 클라이언트의 관찰값이며 안정된 API 계약이 아니다. 공급자가 바꾸면 깨진다.
4. **깨짐을 먼저 감지하는 장치를 유지한다.** `sourceContractTest`가 검색·해석·구간 요청을 실제로 확인하며, 클라이언트 종류별 결과를 표로 남겨 어디가 깨졌는지 즉시 드러낸다.
5. **접근 제한을 우회하지 않는다.** PO token 생성 등 공급자의 봇 차단 검증을 우회하는 수단은 도입하지 않는다. 이 방침 때문에 1번 제약이 유지된다.
6. 이 판정은 WiFi 연결의 단일 기기·단일 지역 기준이다. 다른 지역, 측정 요금제, 다른 OEM은 별도 검증 대상이다.

## 재판정 기준

아래 중 하나라도 발생하면 Provider A 채택을 다시 판단하고 KM-064 Provider B 평가를 활성화한다.

- `sourceContractTest`의 해석 또는 구간 요청이 지속적으로 실패한다
- progressive 형식도 오디오 전용과 같은 제한을 받게 된다
- 대역폭 제약이 실사용에서 감당하기 어렵다고 판단된다

## 실행 명령

```powershell
.\gradlew.bat sourceContractTest -PsourceContractUseWindowsTrust=true --no-daemon --console=plain
.\gradlew.bat connectedDebugAndroidTest --no-daemon --console=plain
```
