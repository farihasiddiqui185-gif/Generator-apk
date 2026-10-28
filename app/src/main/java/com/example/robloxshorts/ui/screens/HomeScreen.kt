package com.example.robloxshorts.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.robloxshorts.data.model.ShortsCategory
import com.example.robloxshorts.ui.components.NeonButton
import com.example.robloxshorts.ui.components.NeonCard
import com.example.robloxshorts.ui.components.NeonProgressBar
import com.example.robloxshorts.ui.components.RobloxBadge
import com.example.robloxshorts.ui.viewmodel.ShortsViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: ShortsViewModel,
    modifier: Modifier = Modifier
) {
    val activeCount by viewModel.activeGeneratingCount.collectAsState()
    val completedVideos by viewModel.completedVideos.collectAsState()
    val allVideos by viewModel.allVideos.collectAsState()
    val currentlyRenderingVideo = allVideos.firstOrNull { it.status == "RENDERING" || it.status == "SCRIPTING" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        // --- 1. HERO BANNER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = "Roblox Shorts Factory Banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark gradient overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, CyberBlack.copy(alpha = 0.95f))
                        )
                    )
            )

            // Floating App Header
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.roblox_shorts_logo),
                    contentDescription = "Roblox AI Shorts Logo",
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, NeonCyan, RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "ROBLOX AI SHORTS",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "BULK VIDEO FACTORY & GALLERY",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. ACTIVE RENDERING BANNER (If generating) ---
        AnimatedVisibility(visible = activeCount > 0) {
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                borderColor = NeonCyan,
                backgroundColor = SurfaceElevated
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AI FACTORY PRODUCING: $activeCount VIDEO(S)",
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        RobloxBadge(
                            text = "LIVE",
                            textColor = NeonCyan,
                            backgroundColor = CyberBlack
                        )
                    }

                    currentlyRenderingVideo?.let { activeVideo ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activeVideo.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeVideo.progressMessage,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NeonProgressBar(progress = activeVideo.progress, height = 8.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 3. CORE PROMPT ACTIONS (User Specified Controls) ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Big Primary CTA: CREATE AI SHORTS
            NeonButton(
                text = "⚡ CREATE AI SHORTS (CUSTOMIZE)",
                onClick = { viewModel.setTab(1) },
                icon = Icons.Default.AutoAwesome,
                gradientColors = listOf(NeonPurple, NeonPink),
                height = 58.dp,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_create_ai_shorts"
            )

            // Row: Quick Bulk Generators 10 & 20
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeonButton(
                    text = "GENERATE 10",
                    onClick = { viewModel.quickGenerate(10) },
                    icon = Icons.Default.Bolt,
                    gradientColors = listOf(NeonCyan, NeonBlue),
                    height = 52.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_generate_10"
                )

                NeonButton(
                    text = "GENERATE 20",
                    onClick = { viewModel.quickGenerate(20) },
                    icon = Icons.Default.Bolt,
                    gradientColors = listOf(NeonYellow, NeonPink),
                    height = 52.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_generate_20"
                )
            }

            // Row: MY VIDEO GALLERY & DOWNLOAD ALL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NeonButton(
                    text = "MY GALLERY (${completedVideos.size})",
                    onClick = { viewModel.setTab(2) },
                    icon = Icons.Default.VideoLibrary,
                    gradientColors = listOf(NeonPurple, Color(0xFF4C1D95)),
                    height = 50.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_my_gallery"
                )

                NeonButton(
                    text = "DOWNLOAD ALL",
                    onClick = { viewModel.exportAllAsZip() },
                    icon = Icons.Default.Archive,
                    gradientColors = listOf(NeonGreen, Color(0xFF047857)),
                    height = 50.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_download_all"
                )
            }

            // VIDEO SETTINGS
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setTab(3) }
                    .testTag("btn_video_settings"),
                borderColor = SurfaceCardBorder,
                backgroundColor = SurfaceDark
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VIDEO SETTINGS & AI API",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Configure Gemini API, resolution (1080x1920), soundfx & voices",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                    RobloxBadge(
                        text = "CONFIGURE",
                        textColor = NeonCyan,
                        backgroundColor = SurfaceElevated
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 4. 1-TAP VIRAL THEMES LAUNCHER ---
        Text(
            text = "ONE-TAP VIRAL CATEGORIES",
            color = TextPrimary,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeQuickCard(
                title = "Roblox Horror Stories 😱",
                subtitle = "Doors Room 100, 3AM Creepy Elevator & The Mimic",
                badge = "HORROR",
                badgeColor = NeonPink,
                onClick = {
                    viewModel.updateParams { it.copy(category = ShortsCategory.HORROR, quantity = 1) }
                    viewModel.startBulkGeneration()
                }
            )

            ThemeQuickCard(
                title = "0 Robux vs 100M Robux 💎",
                subtitle = "Bacon Hair traded for secret ancient Dominus",
                badge = "RICH VS POOR",
                badgeColor = NeonYellow,
                onClick = {
                    viewModel.updateParams { it.copy(category = ShortsCategory.RICH_VS_POOR, quantity = 1) }
                    viewModel.startBulkGeneration()
                }
            )

            ThemeQuickCard(
                title = "Troll Admin Command Fails! 😂",
                subtitle = "Infinite fling jump physics & ragdoll destruction",
                badge = "FUNNY",
                badgeColor = NeonCyan,
                onClick = {
                    viewModel.updateParams { it.copy(category = ShortsCategory.FUNNY, quantity = 1) }
                    viewModel.startBulkGeneration()
                }
            )

            ThemeQuickCard(
                title = "Escaping Mr. Stinky's School! 🏫",
                subtitle = "Laser detention vents & cafeteria lava floor parkour",
                badge = "SCHOOL",
                badgeColor = NeonPurple,
                onClick = {
                    viewModel.updateParams { it.copy(category = ShortsCategory.SCHOOL, quantity = 1) }
                    viewModel.startBulkGeneration()
                }
            )
        }
    }
}

@Composable
private fun ThemeQuickCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        borderColor = SurfaceCardBorder,
        backgroundColor = SurfaceCard
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RobloxBadge(
                        text = badge,
                        textColor = badgeColor,
                        backgroundColor = SurfaceElevated
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SurfaceElevated)
                    .border(1.dp, badgeColor.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Generate",
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
