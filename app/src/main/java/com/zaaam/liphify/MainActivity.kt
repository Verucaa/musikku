package com.zaaam.liphify

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zaaam.liphify.ui.browse.BrowseScreen
import com.zaaam.liphify.ui.home.HomeScreen
import com.zaaam.liphify.ui.library.LibraryScreen
import com.zaaam.liphify.ui.nav.Tab
import com.zaaam.liphify.ui.player.MiniPlayer
import com.zaaam.liphify.ui.player.NowPlayingScreen
import com.zaaam.liphify.ui.player.PlaybackViewModel
import com.zaaam.liphify.ui.search.SearchScreen
import com.zaaam.liphify.ui.theme.Accent
import com.zaaam.liphify.ui.theme.LiPhifyTheme
import com.zaaam.liphify.ui.theme.TextSecondary
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

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
    val hazeState = remember { HazeState() }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 10.dp)) {
                if (!pState.isExpanded && pState.current != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2C2C2E).copy(alpha = 0.78f),
                        modifier = Modifier.padding(bottom = 8.dp).hazeChild(
                            state = hazeState,
                            style = HazeDefaults.style(
                                backgroundColor = Color(0xFF2C2C2E).copy(alpha = 0.78f),
                                blurRadius = 24.dp,
                            ),
                        ),
                    ) {
                        MiniPlayer(
                            state = pState,
                            onTap = { player.setExpanded(true) },
                            onToggle = { player.togglePlayPause() },
                            onNext = { player.next() },
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF1C1C1E).copy(alpha = 0.78f),
                    modifier = Modifier.hazeChild(
                        state = hazeState,
                        style = HazeDefaults.style(
                            backgroundColor = Color(0xFF1C1C1E).copy(alpha = 0.78f),
                            blurRadius = 24.dp,
                        ),
                    ),
                ) {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TabItem(Tab.Home, Icons.Filled.Home, route, nav, Modifier.weight(1f))
                        TabItem(Tab.New, Icons.Filled.AutoAwesome, route, nav, Modifier.weight(1f))
                        TabItem(Tab.Library, Icons.Filled.LibraryMusic, route, nav, Modifier.weight(1f))
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            val onSearch = route?.startsWith("search") == true
                            IconButton(
                                onClick = { nav.navigate("search") },
                                modifier = Modifier.size(44.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(
                                        if (onSearch) Accent else Color.White.copy(alpha = 0.14f),
                                    ),
                            ) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = if (onSearch) Color.White else TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad).haze(state = hazeState)) {
            NavHost(nav, startDestination = Tab.Library.route) {
                composable(Tab.Home.route) { HomeScreen(player) }
                composable(Tab.New.route) { BrowseScreen(onGenre = { nav.navigate("search?preset=" + android.net.Uri.encode(it)) }) }
                composable(Tab.Library.route) { LibraryScreen(player) }
                composable(
                    "search?preset={preset}",
                    arguments = listOf(navArgument("preset") { type = NavType.StringType; defaultValue = "" }),
                ) { entry ->
                    SearchScreen(preset = entry.arguments?.getString("preset") ?: "", player = player)
                }
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
    modifier: Modifier = Modifier,
) {
    val selected = route == tab.route
    IconButton(onClick = { nav.navigate(tab.route) }, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = tab.label,
                tint = if (selected) Accent else TextSecondary,
            )
            Text(
                tab.label,
                fontSize = 10.sp,
                color = if (selected) Accent else TextSecondary,
            )
        }
    }
}
