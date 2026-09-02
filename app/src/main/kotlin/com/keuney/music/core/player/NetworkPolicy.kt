package com.keuney.music.core.player

import android.content.Context
import android.net.ConnectivityManager
import com.keuney.music.core.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 새로 내려받는 재생을 허용할지 판단한다. 이미 캐시에 있는 구간은 이 판단을 거치지 않고
 * 그대로 재생된다. 캐시가 스트림 해석보다 바깥에 있기 때문이다(ADR-036).
 */
@Singleton
internal class NetworkPolicy(
    private val settings: SettingsRepository,
    private val metered: () -> Boolean,
) {
    @Inject
    constructor(@ApplicationContext context: Context, settings: SettingsRepository) : this(
        settings,
        {
            // 연결 정보를 얻지 못하면 막지 않는다. 알 수 없는 상태 때문에 재생을 멈추지 않는다.
            context.getSystemService(ConnectivityManager::class.java)?.isActiveNetworkMetered ?: false
        },
    )

    suspend fun blocksRemoteFetch(): Boolean = settings.wifiOnlyPlayback.first() && metered()

    fun isMetered(): Boolean = metered()
}
