package com.zaaam.liphify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zaaam.liphify.ui.browse.BrowseScreen
import com.zaaam.liphify.ui.library.LibraryScreen
import com.zaaam.liphify.ui.listennow.ListenNowScreen
import com.zaaam.liphify.ui.nav.Tab
import com.zaaam.liphify.ui.player.MiniPlayer
import com.zaaam.liphify.ui.player.NowPlayingScreen
import com.zaaam.liphify.ui.player.PlaybackViewModel
import com.zaaam.liphify.ui.search.SearchScreen
import com.zaaam.liphify.ui.theme.LiPhifyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val player: PlaybackViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LiPhifyTheme {
                LiPhifyScaffold(player)
            }
        }
    }
}

@Composable
fun LiPhifyScaffold(player: PlaybackViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val pState by player.state.collectAsState()
    val snack = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                if (!pState.isExpanded) {
                    MiniPlayer(
                        state = pState,
                        onTap = { player.setExpanded(true) },
                        onToggle = { player.togglePlayPause() },
                        onNext = { player.next() },
                    )
                }
                NavigationBar {
                    TabItem(Tab.ListenNow, Icons.Filled.Home, route, nav)
                    TabItem(Tab.Browse, Icons.Filled.Apps, route, nav)
                    TabItem(Tab.Library, Icons.Filled.LibraryMusic, route, nav)
                    IconButton(onClick = { nav.navigate("search") }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            NavHost(nav, startDestination = Tab.Library.route) {
                composable(Tab.ListenNow.route) { ListenNowScreen(nav, player) }
                composable(Tab.Browse.route) { BrowseScreen(player) }
                composable(Tab.Library.route) { LibraryScreen(player) }
                composable("search") { SearchScreen(player) }
            }
            if (pState.isExpanded) {
                NowPlayingScreen(state = pState, player = player, snack = snack)
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: Tab,
    icon: ImageVector,
    route: String?,
    nav: androidx.navigation.NavController,
) {
    val selected = route == tab.route
    IconButton(onClick = { nav.navigate(tab.route) }) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = tab.label,
                tint = if (selected) com.zaaam.liphify.ui.theme.Accent else com.zaaam.liphify.ui.theme.TextSecondary,
            )
            Text(
                tab.label,
                color = if (selected) com.zaaam.liphify.ui.theme.Accent else com.zaaam.liphify.ui.theme.TextSecondary,
            )
        }
    }
}
