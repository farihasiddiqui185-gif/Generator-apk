package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.robloxshorts.ui.screens.CreateShortsScreen
import com.example.robloxshorts.ui.screens.GalleryScreen
import com.example.robloxshorts.ui.screens.HomeScreen
import com.example.robloxshorts.ui.screens.PlayerScreen
import com.example.robloxshorts.ui.screens.SettingsScreen
import com.example.robloxshorts.ui.viewmodel.ShortsViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.RobloxShortsTheme
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RobloxShortsTheme {
                MainAppRoot()
            }
        }
    }
}

@Composable
fun MainAppRoot(viewModel: ShortsViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activePlayingVideo by viewModel.activePlayingVideo.collectAsState()
    val notification by viewModel.notification.collectAsState()
    val completedVideos by viewModel.completedVideos.collectAsState()
    val activeGeneratingCount by viewModel.activeGeneratingCount.collectAsState()
    val context = LocalContext.current

    // Show native toast when notification changes
    LaunchedEffect(notification) {
        notification?.let {
            Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
        }
    }

    // Handle back button on sub-tabs
    BackHandler(enabled = currentTab != 0 && activePlayingVideo == null) {
        viewModel.setTab(0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = CyberBlack,
            bottomBar = {
                if (activePlayingVideo == null) {
                    NavigationBar(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .border(1.dp, SurfaceCardBorder),
                        containerColor = SurfaceDark,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = currentTab == 0,
                            onClick = { viewModel.setTab(0) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberBlack,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_home")
                        )

                        NavigationBarItem(
                            selected = currentTab == 1,
                            onClick = { viewModel.setTab(1) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (activeGeneratingCount > 0) {
                                            Badge(containerColor = NeonPurple) {
                                                Text("$activeGeneratingCount", color = Color.White)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Create")
                                }
                            },
                            label = { Text("Create", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberBlack,
                                selectedTextColor = NeonPurple,
                                indicatorColor = NeonPurple,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_create")
                        )

                        NavigationBarItem(
                            selected = currentTab == 2,
                            onClick = { viewModel.setTab(2) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (completedVideos.isNotEmpty()) {
                                            Badge(containerColor = NeonGreen) {
                                                Text("${completedVideos.size}", color = CyberBlack)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.VideoLibrary, contentDescription = "Gallery")
                                }
                            },
                            label = { Text("Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberBlack,
                                selectedTextColor = NeonGreen,
                                indicatorColor = NeonGreen,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_gallery")
                        )

                        NavigationBarItem(
                            selected = currentTab == 3,
                            onClick = { viewModel.setTab(3) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CyberBlack,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            ),
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    0 -> HomeScreen(viewModel = viewModel)
                    1 -> CreateShortsScreen(viewModel = viewModel)
                    2 -> GalleryScreen(viewModel = viewModel)
                    3 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Overlay Notification Banner
        AnimatedVisibility(
            visible = notification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            notification?.let { notif ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (notif.isSuccess) SurfaceCard else Color(0xFF3B1219))
                        .border(
                            1.5.dp,
                            if (notif.isSuccess) NeonGreen else NeonRed,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = notif.message,
                        color = if (notif.isSuccess) Color.White else NeonRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Full Screen Video Player Overlay
        activePlayingVideo?.let { playingVideo ->
            PlayerScreen(
                video = playingVideo,
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
