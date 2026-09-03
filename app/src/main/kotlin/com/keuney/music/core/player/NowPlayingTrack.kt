package com.keuney.music.core.player

import com.keuney.music.core.model.SourceType
import com.keuney.music.core.model.Track

/**
 * 지금 재생 중인 곡을 라이브러리에 넣을 수 있는 [Track]으로 바꾼다.
 *
 * 세션은 대기열에 넣은 metadata만 돌려주므로 길이는 재생 상태에서 따로 받는다. 알 수 없으면
 * 넣지 않는다. 0을 넣으면 화면이 "0:00"을 사실처럼 보여준다.
 *
 * [SourceType]은 v0.1에 Remote 하나뿐이라 그것으로 둔다. 출처가 늘어나면 세션에 어떤 출처인지
 * 함께 실어 보내야 하고 이 함수도 그때 다시 봐야 한다.
 */
internal fun NowPlaying.toTrack(durationMs: Long): Track = Track(
    id = mediaId,
    title = title,
    artist = artist,
    artworkUrl = artworkUri,
    durationMs = durationMs.takeIf { it > 0 },
    source = SourceType.Remote,
)
