package com.keuney.music.core.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 재생 중 내려받은 구간을 잠시 보관해 같은 곡을 다시 들을 때 다시 받지 않게 한다.
 * 영구 다운로드가 아니다. 상한을 넘으면 오래된 것부터 지우고, 사용자가 언제든 비울 수 있으며,
 * 운영체제가 캐시 디렉터리를 정리해도 무방하다(AGENTS.md 15, ADR-035).
 */
@Singleton
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
internal class PlaybackCache @Inject constructor(@param:ApplicationContext context: Context) {
    val cache: Cache = shared(context)

    val usedBytes: Long get() = cache.cacheSpace

    /** 보관 중인 구간을 모두 비운다. 재생에 사용 중인 구간은 남을 수 있다. */
    fun clear() {
        cache.keys.toList().forEach(cache::removeResource)
    }

    companion object {
        const val MAX_BYTES = 256L * 1024 * 1024

        /** 저장 확정 단위. 작을수록 짧게 듣고 멈춰도 받은 구간이 남는다. */
        const val FRAGMENT_BYTES = 1L * 1024 * 1024
        private const val DIRECTORY = "media"

        /**
         * SimpleCache는 한 디렉터리를 프로세스에서 하나만 열 수 있다. 주입 그래프가 다시
         * 만들어져도 같은 인스턴스를 쓰도록 프로세스 단위로 보관한다.
         */
        @Volatile
        private var instance: SimpleCache? = null

        private fun shared(context: Context): SimpleCache =
            instance ?: synchronized(this) {
                instance ?: SimpleCache(
                    File(context.applicationContext.cacheDir, DIRECTORY),
                    LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                    StandaloneDatabaseProvider(context.applicationContext),
                ).also { instance = it }
            }
    }
}
