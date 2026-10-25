package com.example.lddc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.lddc.common.models.info.SongInfo
import com.example.lddc.presentation.screens.search.LyricsDetailScreen
import com.example.lddc.presentation.screens.search.LyricsDetailViewModel
import com.example.lddc.presentation.screens.search.SearchFilters
import com.example.lddc.presentation.screens.search.SearchScreen
import com.example.lddc.presentation.screens.search.SearchViewModel
import com.example.lddc.presentation.screens.settings.SettingsScreen
import com.example.lddc.presentation.screens.settings.SettingsViewModel
import com.example.lddc.presentation.theme.LDDCTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Search : Screen(
        "search?initialQuery={initialQuery}&songName={songName}&artist={artist}&album={album}",
        "搜索",
        Icons.Default.Home
    )

    object Settings : Screen("settings", "设置", Icons.Default.Settings)

    object LyricsDetail : Screen("lyrics_detail/{songJson}", "歌词详情", Icons.Default.Home) {
        fun createRoute(song: SongInfo): String {
            val songJson = Json.encodeToString(song)
            val encoded = URLEncoder.encode(songJson, StandardCharsets.UTF_8.toString())
            return "lyrics_detail/$encoded"
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LDDCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LDDCApp()
                }
            }
        }
    }
}

@Composable
fun LDDCApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Search.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = Screen.Search.route,
                arguments = listOf(
                    navArgument("initialQuery") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("songName") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("artist") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("album") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val viewModel: SearchViewModel = hiltViewModel()
                val initialQuery = backStackEntry.arguments?.getString("initialQuery") ?: ""
                val songName = backStackEntry.arguments?.getString("songName") ?: ""
                val artist = backStackEntry.arguments?.getString("artist") ?: ""
                val album = backStackEntry.arguments?.getString("album") ?: ""
                SearchScreen(
                    viewModel = viewModel,
                    initialQuery = initialQuery,
                    initialFilters = SearchFilters(
                        songName = songName,
                        artist = artist,
                        album = album
                    ),
                    onSongClick = { song ->
                        navController.navigate(Screen.LyricsDetail.createRoute(song))
                    }
                )
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = viewModel)
            }

            composable(
                route = Screen.LyricsDetail.route,
                arguments = listOf(
                    navArgument("songJson") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val viewModel: LyricsDetailViewModel = hiltViewModel()
                val songJson = backStackEntry.arguments?.getString("songJson") ?: ""
                val decodedJson = URLDecoder.decode(songJson, StandardCharsets.UTF_8.toString())
                val song = try {
                    Json.decodeFromString<SongInfo>(decodedJson)
                } catch (e: Exception) {
                    null
                }

                LyricsDetailScreen(
                    viewModel = viewModel,
                    song = song,
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen.Search,
        Screen.Settings
    )

    val context = LocalContext.current
    val isRound = context.resources.configuration.isScreenRound

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    if (isRound) {
        // 圆屏适配：使用居中的胶囊悬浮导航，拉近搜索与设置的距离
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp), // 避免底边被圆弧裁切
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 32.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(48.dp), // 控制两个按钮的间距
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Icon(
                                screen.icon, 
                                contentDescription = screen.title,
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = screen.title,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        // 手机默认适配
        NavigationBar {
            items.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                NavigationBarItem(
                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                    label = {
                        Text(
                            text = screen.title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}