package com.example.robloxshorts.ui.screens

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robloxshorts.ui.components.NeonButton
import com.example.robloxshorts.ui.components.NeonCard
import com.example.robloxshorts.ui.components.RobloxBadge
import com.example.robloxshorts.ui.viewmodel.ShortsViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: ShortsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentApiKey by viewModel.apiKey.collectAsState()
    var tempKey by remember(currentApiKey) { mutableStateOf(currentApiKey) }

    val videosDir = File(context.filesDir, "videos")
    val cacheSizeMB = remember {
        val totalBytes = videosDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        "%.1f".format(totalBytes / (1024f * 1024f))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 36.dp)
    ) {
        Text(
            text = "FACTORY SETTINGS",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            letterSpacing = 1.sp
        )
        Text(
            text = "AI API configurations, video encoding limits & storage",
            color = NeonCyan,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // --- 1. GEMINI AI API CONFIGURATION ---
        SectionHeader("1. GEMINI AI API KEY (STORY & SCRIPTING)")
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = SurfaceCardBorder,
            backgroundColor = SurfaceCard
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "API Key",
                        tint = NeonYellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gemini AI Script & Story Engine",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Used for generating unique, high-retention Roblox storylines, character dialogues, and synchronized subtitle timestamps.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tempKey,
                    onValueChange = { tempKey = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input"),
                    placeholder = {
                        Text(
                            text = "AI Studio Secrets key (or paste AIzaSy...)",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceCardBorder,
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))
                NeonButton(
                    text = "SAVE API KEY",
                    onClick = { viewModel.updateApiKey(tempKey) },
                    gradientColors = listOf(NeonCyan, NeonPurple),
                    height = 42.dp,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_api_key_btn"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 2. API USAGE & COST TRANSPARENCY (Mandated by Prompt) ---
        SectionHeader("2. PRICING & USAGE LIMITS")
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = NeonGreen.copy(alpha = 0.5f),
            backgroundColor = SurfaceCard
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Limits Info",
                        tint = NeonGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% Free On-Device Video Rendering",
                        color = NeonGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Video rendering & audio synthesis run completely on-device using Android's native MediaCodec hardware encoder. No recurring cloud video GPU charges!\n\n" +
                            "• Gemini API Free Tier: Google AI Studio provides 15 Requests Per Minute (RPM) completely FREE, allowing you to bulk generate dozens of Roblox scripts without cost.\n\n" +
                            "• Offline / Fallback: If no API key is set or quota is exhausted, our built-in Procedural Roblox Director engine generates complete story scripts automatically with zero downtime.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 3. GALLERY & STORAGE DESTINATIONS ---
        SectionHeader("3. STORAGE & GALLERY INTEGRATION")
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = SurfaceCardBorder,
            backgroundColor = SurfaceCard
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Storage",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phone Gallery & Storage Location",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "When you tap 'SAVE TO PHONE GALLERY', videos are indexed via Android MediaStore into:\n" +
                            "📂 Movies/RobloxShorts/\n\n" +
                            "They appear immediately inside Google Photos, Samsung Gallery, or any video player on your phone.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Internal Cache: $cacheSizeMB MB",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                    NeonButton(
                        text = "CLEAR CACHE",
                        onClick = {
                            videosDir.deleteRecursively()
                            videosDir.mkdirs()
                        },
                        icon = Icons.Default.CleaningServices,
                        gradientColors = listOf(Color(0xFF6B21A8), Color(0xFF374151)),
                        height = 36.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 4. ABOUT & VERSION ---
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = SurfaceCardBorder,
            backgroundColor = SurfaceElevated
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Roblox AI Shorts Factory v1.0",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Built with Kotlin, Jetpack Compose, MediaCodec & Gemini AI",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
