package com.keuney.music

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import com.keuney.music.ui.theme.KeuneyMusicTheme
import dagger.hilt.android.AndroidEntryPoint
import com.keuney.music.feature.player.PlayerViewModel
import com.keuney.music.feature.library.LibraryViewModel
import com.keuney.music.feature.search.SearchViewModel
import com.keuney.music.feature.settings.SettingsViewModel
import com.keuney.music.navigation.KeuneyNavHost
import com.keuney.music.ui.theme.isDark

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val playerViewModel by lazy { ViewModelProvider(this)[PlayerViewModel::class.java] }
    private val searchViewModel by lazy { ViewModelProvider(this)[SearchViewModel::class.java] }
    private val libraryViewModel by lazy { ViewModelProvider(this)[LibraryViewModel::class.java] }
    private val settingsViewModel by lazy { ViewModelProvider(this)[SettingsViewModel::class.java] }

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
        applyEdgeToEdge(darkTheme = false)
        setContent {
            val theme by settingsViewModel.theme.collectAsStateWithLifecycle()
            val darkTheme = theme.isDark(isSystemInDarkTheme())
            // 시스템 표시줄의 아이콘 색은 창의 성질이라 Compose 밖에서 정해야 한다. 화면 색이
            // 바뀔 때마다 다시 걸어 준다. 그러지 않으면 어두운 화면에서 아이콘이 어두운 채로
            // 남아 보이지 않는다.
            LaunchedEffect(darkTheme) { applyEdgeToEdge(darkTheme) }
            KeuneyMusicTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KeuneyNavHost(playerViewModel, searchViewModel, libraryViewModel, settingsViewModel)
                }
            }
        }
    }

    /**
     * 표시줄을 투명하게 두고 아이콘 색만 화면에 맞춘다.
     *
     * [SystemBarStyle.light]는 밝은 배경 위의 어두운 아이콘, [SystemBarStyle.dark]는 그 반대다.
     * 시스템 설정이 아니라 **앱이 지금 그리는 색**을 따라야 한다. 사용자가 시스템은 밝게 두고
     * 앱만 어둡게 고를 수 있기 때문이다.
     */
    private fun applyEdgeToEdge(darkTheme: Boolean) {
        val style = if (darkTheme) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
