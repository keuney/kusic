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
import com.keuney.music.feature.library.LibraryViewModel
import com.keuney.music.feature.search.SearchViewModel
import com.keuney.music.navigation.KeuneyNavHost

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val playerViewModel by lazy { ViewModelProvider(this)[PlayerViewModel::class.java] }
    private val searchViewModel by lazy { ViewModelProvider(this)[SearchViewModel::class.java] }
    private val libraryViewModel by lazy { ViewModelProvider(this)[LibraryViewModel::class.java] }

    override fun onStart() {
        super.onStart()
        playerViewModel.connect()
    }

    /**
     * 구성 변경으로 잠깐 사라지는 것은 떠나는 것이 아니다. ViewModel이 그대로 남으므로 다시
     * 만들어진 Activity가 같은 연결을 이어 쓴다.
     *
     * 여기서 끊으면 회전할 때마다 세션에 다시 붙는다. 붙는 동안 재생 상태가 비어 미니 플레이어가
     * 사라지고 조작 버튼이 꺼진다. 화면을 돌렸을 뿐인데 재생이 끊긴 것처럼 보인다.
     *
     * 정말로 떠날 때(Activity 종료)는 [PlayerViewModel.onCleared]가 끊는다.
     */
    override fun onStop() {
        if (!isChangingConfigurations) playerViewModel.disconnect()
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
                    KeuneyNavHost(playerViewModel, searchViewModel, libraryViewModel)
                }
            }
        }
    }
}
