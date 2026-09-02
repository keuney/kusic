package com.keuney.music.core.player

/**
 * 재생 대기열에는 Track ID만 남기고 실제 스트림 주소는 재생 직전에 해석한다(AGENTS.md 8).
 * MediaItem에는 이 자리표시 URI만 넣으며 해석된 주소는 어디에도 저장하지 않는다.
 */
internal object TrackUri {
    const val SCHEME = "keuney"
    private const val PREFIX = "$SCHEME://track/"
    private val allowedId = Regex("[A-Za-z0-9_-]{1,64}")

    fun of(trackId: String): String {
        require(allowedId.matches(trackId)) { "Unsupported track id" }
        return PREFIX + trackId
    }

    /** 자리표시 URI가 아니거나 형식이 다르면 null을 반환해 원래 요청을 그대로 두게 한다. */
    fun trackIdOrNull(uri: String): String? {
        if (!uri.startsWith(PREFIX)) return null
        return uri.removePrefix(PREFIX).takeIf(allowedId::matches)
    }
}
