package com.keuney.music

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.keuney.music.ui.theme.KeuneyMusicTheme
import dagger.hilt.android.AndroidEntryPoint
import com.keuney.music.feature.player.PlayerViewModel
import com.keuney.music.feature.player.TestPlaybackScreen
import com.keuney.music.feature.search.SearchViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val playerViewModel by lazy { ViewModelProvider(this)[PlayerViewModel::class.java] }
    private val searchViewModel by lazy { ViewModelProvider(this)[SearchViewModel::class.java] }

    override fun onStart() {
        super.onStart()
        playerViewModel.connect()
    }

    override fun onStop() {
        playerViewModel.disconnect()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            KeuneyMusicTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TestPlaybackScreen(playerViewModel, searchViewModel)
                }
            }
        }
    }
}
