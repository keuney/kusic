package com.keuney.music.core.player

import com.keuney.music.core.settings.SettingsRepository
import com.keuney.music.core.settings.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** KM-137: 설정이 켜져 있고 측정 요금제일 때만 새 요청을 막는다. */
class NetworkPolicyTest {
    @Test
    fun defaultSettingNeverBlocksPlayback(): Unit = runBlocking {
        assertFalse(NetworkPolicy(settings(wifiOnly = false), metered = { true }).blocksRemoteFetch())
        assertFalse(NetworkPolicy(settings(wifiOnly = false), metered = { false }).blocksRemoteFetch())
    }

    @Test
    fun blocksOnlyWhenTheSettingIsOnAndTheConnectionIsMetered(): Unit = runBlocking {
        assertTrue(NetworkPolicy(settings(wifiOnly = true), metered = { true }).blocksRemoteFetch())
        assertFalse(NetworkPolicy(settings(wifiOnly = true), metered = { false }).blocksRemoteFetch())
    }

    @Test
    fun theSettingIsReadEachTimeSoChangesApplyImmediately(): Unit = runBlocking {
        val settings = FakeSettings(false)
        val policy = NetworkPolicy(settings, metered = { true })

        assertFalse(policy.blocksRemoteFetch())
        settings.setWifiOnlyPlayback(true)
        assertTrue(policy.blocksRemoteFetch())
    }

    private fun settings(wifiOnly: Boolean): SettingsRepository = FakeSettings(wifiOnly)

    private class FakeSettings(wifiOnly: Boolean) : SettingsRepository {
        private val state = MutableStateFlow(wifiOnly)
        override val theme: Flow<ThemePreference> = MutableStateFlow(ThemePreference.System)
        override suspend fun setTheme(theme: ThemePreference) = Unit
        override val wifiOnlyPlayback: Flow<Boolean> = state
        override suspend fun setWifiOnlyPlayback(enabled: Boolean) {
            state.value = enabled
        }
    }
}
