package com.keuney.music.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.keuney.music.R
import com.keuney.music.core.player.ConnectionState
import com.keuney.music.feature.player.MiniPlayer
import com.keuney.music.feature.player.PlayerViewModel
import com.keuney.music.feature.player.NowPlayingScreen
import com.keuney.music.feature.player.QueueScreen
import com.keuney.music.feature.search.SearchScreen
import com.keuney.music.feature.search.SearchViewModel

/**
 * 앱의 화면 구성 전부. 하단 내비게이션 세 탭과 미니 플레이어를 감싸고 목적지를 갈아 끼운다.
 *
 * ViewModel은 Activity가 만들어 넘긴다. 목적지마다 새로 만들면 탭을 옮길 때 재생 상태가
 * 끊기는데, KM-150의 인수 조건이 바로 그것이 유지되는 것이다.
 */
@Composable
internal fun KeuneyNavHost(
    playerViewModel: PlayerViewModel,
    searchViewModel: SearchViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val connection by playerViewModel.connectionState.collectAsStateWithLifecycle()
    val playback by playerViewModel.playbackState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val connected = connection == ConnectionState.Connected
    val nowPlaying = playback.nowPlaying
    // 전체 화면 목적지에서는 하단을 비운다.
    val onFullScreen = currentRoute == NOW_PLAYING_ROUTE || currentRoute == QUEUE_ROUTE
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // 전체 화면 플레이어는 하단을 비워 앨범 이미지에 자리를 준다. 뒤로 가기와 화면 안의
            // 뒤로 버튼으로 떠났던 탭으로 돌아간다.
            if (!onFullScreen) Column {
                // 같은 것을 위아래로 두 번 보여주지 않는다.
                if (nowPlaying != null) {
                    HorizontalDivider()
                    MiniPlayer(
                        nowPlaying = nowPlaying,
                        isPlaying = playback.canPause,
                        enabled = connected,
                        onPlayPause = { if (playback.canPause) playerViewModel.pause() else playerViewModel.play() },
                        onClick = { navController.navigate(NOW_PLAYING_ROUTE) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.switchTab(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Search.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            // 홈 내용은 KM-151, 라이브러리 내용은 KM-116이다.
            composable(TopLevelDestination.Home.route) { NotReadyScreen() }
            composable(TopLevelDestination.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    selectEnabled = connected,
                    // 고른 곡부터 재생하고 나머지 결과를 대기열에 남긴다.
                    onSelect = playerViewModel::playTracks,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            }
            composable(TopLevelDestination.Library.route) { NotReadyScreen() }
            composable(NOW_PLAYING_ROUTE) {
                NowPlayingScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() },
                    onOpenQueue = { navController.navigate(QUEUE_ROUTE) },
                )
            }
            composable(QUEUE_ROUTE) {
                QueueScreen(
                    viewModel = playerViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/**
 * 탭 이동은 쌓지 않는다. 같은 탭을 다시 누르면 아무 일도 일어나지 않고, 다른 탭을 누르면
 * 시작 목적지까지 정리해 뒤로 가기가 앱을 벗어난다.
 */
private fun NavHostController.switchTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun NotReadyScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.screen_not_ready),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
