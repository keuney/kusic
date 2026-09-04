package com.keuney.music.core.player

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.keuney.music.core.settings.CacheLimit
import com.keuney.music.core.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 재생 중 내려받은 구간을 잠시 보관해 같은 곡을 다시 들을 때 다시 받지 않게 한다.
 * 영구 다운로드가 아니다. 상한을 넘으면 오래된 것부터 지우고, 사용자가 언제든 비울 수 있으며,
 * 운영체제가 캐시 디렉터리를 정리해도 무방하다(AGENTS.md 15, ADR-035).
 */
@Singleton
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
internal class PlaybackCache @Inject constructor(
    @param:ApplicationContext context: Context,
    settings: SettingsRepository,
) {
    /**
     * 지금 열려 있는 캐시에 걸린 상한.
     *
     * 상한은 캐시를 만들 때 정해진다. Media3의 evictor는 만든 뒤에 상한을 바꿀 방법이 없고,
     * 한 디렉터리의 SimpleCache는 프로세스에 하나뿐이며 이미 재생에 쓰이고 있다. 그래서 설정을
     * 바꾸면 다음 실행부터 적용되고, 화면은 이 값으로 "지금 적용된 것"을 말한다.
     */
    val limitBytes: Long

    val cache: Cache

    init {
        // 캐시는 플레이어를 만들기 전에 있어야 하고 상한은 만들 때 필요하다. 작은 설정 파일
        // 한 번 읽기이므로 여기서 기다린다.
        cache = shared(context, runBlocking { settings.cacheLimit.first() })
        // 저장된 값이 아니라 실제로 걸린 값을 보고한다. 이미 열려 있던 캐시라면 그때의 상한이다.
        limitBytes = activeLimitBytes
    }

    val usedBytes: Long get() = cache.cacheSpace

    /** 보관 중인 구간을 모두 비운다. 재생에 사용 중인 구간은 남을 수 있다. */
    fun clear() {
        cache.keys.toList().forEach(cache::removeResource)
    }

    companion object {
        /** 저장 확정 단위. 작을수록 짧게 듣고 멈춰도 받은 구간이 남는다. */
        const val FRAGMENT_BYTES = 1L * 1024 * 1024
        private const val DIRECTORY = "media"

        /**
         * SimpleCache는 한 디렉터리를 프로세스에서 하나만 열 수 있다. 주입 그래프가 다시
         * 만들어져도 같은 인스턴스를 쓰도록 프로세스 단위로 보관한다.
         *
         * 그래서 상한은 **처음 만들 때의 값**으로 굳는다. 두 번째 호출의 상한은 무시된다.
         */
        @Volatile
        private var instance: SimpleCache? = null

        /** 열려 있는 캐시에 실제로 걸린 상한. 저장된 설정과 다를 수 있다. */
        @Volatile
        private var activeLimitBytes: Long = 0

        private fun shared(context: Context, limit: CacheLimit): SimpleCache =
            instance ?: synchronized(this) {
                instance ?: SimpleCache(
                    File(context.applicationContext.cacheDir, DIRECTORY),
                    LeastRecentlyUsedCacheEvictor(limit.bytes),
                    StandaloneDatabaseProvider(context.applicationContext),
                ).also {
                    instance = it
                    activeLimitBytes = limit.bytes
                }
            }
    }
}
