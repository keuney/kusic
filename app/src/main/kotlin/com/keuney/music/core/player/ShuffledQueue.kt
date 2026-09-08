package com.keuney.music.core.player

import com.keuney.music.core.model.Track
import kotlin.random.Random

/**
 * 목록을 무작위 순서의 대기열로 만든다(KM-139).
 *
 * **무작위를 대기열 자체에 넣는다.** Media3의 셔플 모드로도 순서를 섞을 수 있지만 그 순서는
 * 세션 뒤에 있어 컨트롤러가 읽을 수 없다(ADR-053). 그래서 대기열 화면은 넣은 순서를 보여 주고
 * 실제 재생 순서는 그와 다르다는 안내를 달아야 한다. 앱이 순서를 정해 넣으면 보이는 것이
 * 그대로 재생된다.
 *
 * 곡을 빼거나 겹치지 않는다. [Random]을 받는 이유는 기기 없이 순서를 고정해 검사하기
 * 위해서다([UnplayableSkip]과 같은 방식). 받은 목록은 바꾸지 않는다. 화면이 관찰 중인 목록을
 * 그대로 넘기므로 그것을 뒤섞으면 목록의 줄 순서가 함께 바뀐다.
 */
internal fun shuffledQueue(tracks: List<Track>, random: Random = Random.Default): List<Track> =
    tracks.shuffled(random)
