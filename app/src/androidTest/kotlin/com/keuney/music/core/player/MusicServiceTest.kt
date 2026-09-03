package com.keuney.music.core.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.Build
import androidx.media3.session.MediaLibraryService
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MusicServiceTest {
    @get:Rule
    val hilt = HiltAndroidRule(this)

    @Test
    fun serviceIsRegisteredAndCanBeBound() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val component = ComponentName(context, MusicService::class.java)
        val info = context.packageManager.getServiceInfo(component, 0)
        assertTrue(info.exported)
        if (Build.VERSION.SDK_INT >= 29) {
            assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK, info.foregroundServiceType)
        }
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                connected.countDown()
            }
            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
        val intent = Intent(context, MusicService::class.java).setAction(MediaLibraryService.SERVICE_INTERFACE)
        assertTrue(context.bindService(intent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("서비스 연결 시간 초과", connected.await(10, TimeUnit.SECONDS))
        } finally {
            context.unbindService(connection)
        }
    }
}
