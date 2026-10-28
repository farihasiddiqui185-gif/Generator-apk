package com.example.robloxshorts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robloxshorts.data.model.ShortsCategory
import com.example.robloxshorts.data.model.VideoDuration
import com.example.robloxshorts.data.model.VideoLanguage
import com.example.robloxshorts.ui.components.NeonButton
import com.example.robloxshorts.ui.components.NeonCard
import com.example.robloxshorts.ui.components.RobloxBadge
import com.example.robloxshorts.ui.viewmodel.ShortsViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CreateShortsScreen(
    viewModel: ShortsViewModel,
    modifier: Modifier = Modifier
) {
    val params by viewModel.generationParams.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 36.dp)
    ) {
        Text(
            text = "AI SHORTS GENERATOR",
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            letterSpacing = 1.sp
        )
        Text(
            text = "Configure bulk video batch parameters & press Generate",
            color = NeonCyan,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 1. NUMBER OF VIDEOS ---
        SectionHeader("1. NUMBER OF VIDEOS")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1, 5, 10, 20).forEach { qty ->
                val isSelected = params.quantity == qty
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonPurple else SurfaceCard)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else SurfaceCardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.updateParams { it.copy(quantity = qty) } }
                        .testTag("qty_$qty"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$qty",
                        color = if (isSelected) Color.White else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Quantity Stepper
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceCard)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Custom Quantity: ${params.quantity} videos",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (params.quantity > 1) {
                            viewModel.updateParams { it.copy(quantity = it.quantity - 1) }
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Minus", tint = NeonCyan)
                }
                Text(
                    text = "${params.quantity}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                IconButton(
                    onClick = {
                        if (params.quantity < 50) {
                            viewModel.updateParams { it.copy(quantity = it.quantity + 1) }
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Plus", tint = NeonCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 2. VIDEO DURATION ---
        SectionHeader("2. VIDEO DURATION")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VideoDuration.values().forEach { dur ->
                val isSelected = params.duration == dur
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) NeonCyan else SurfaceCard)
                        .border(
                            1.dp,
                            if (isSelected) Color.White else SurfaceCardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.updateParams { it.copy(duration = dur) } }
                        .testTag("dur_${dur.seconds}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${dur.seconds}s",
                        color = if (isSelected) CyberBlack else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 3. FORMAT ---
        SectionHeader("3. FORMAT & RESOLUTION")
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = SurfaceCardBorder,
            backgroundColor = SurfaceCard
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Smartphone,
                    contentDescription = "Format",
                    tint = NeonPink,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vertical 9:16 (1080 × 1920 MP4)",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Optimized for YouTube Shorts, TikTok & Reels",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                RobloxBadge(text = "9:16", textColor = NeonPink, backgroundColor = SurfaceElevated)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 4. LANGUAGE ---
        SectionHeader("4. LANGUAGE")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(VideoLanguage.EN, VideoLanguage.HI, VideoLanguage.ES, VideoLanguage.PT).forEach { lang ->
                val isSelected = params.language == lang
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) SurfaceElevated else SurfaceCard)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else SurfaceCardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.updateParams { it.copy(language = lang) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lang.displayName,
                        color = if (isSelected) NeonCyan else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 5. CATEGORIES ---
        SectionHeader("5. CATEGORIES (STORY THEME)")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShortsCategory.values().forEach { cat ->
                val isSelected = params.category == cat
                NeonCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateParams { it.copy(category = cat) } }
                        .testTag("cat_${cat.name}"),
                    borderColor = if (isSelected) NeonCyan else SurfaceCardBorder,
                    backgroundColor = if (isSelected) SurfaceElevated else SurfaceCard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(cat.primaryColorHex))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cat.title,
                                color = if (isSelected) NeonCyan else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = cat.subtitle,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        if (isSelected) {
                            RobloxBadge(
                                text = "SELECTED",
                                textColor = NeonCyan,
                                backgroundColor = CyberBlack
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- 6. CUSTOM PROMPT (OPTIONAL) ---
        SectionHeader("6. CUSTOM STORY / CHARACTER IDEA (OPTIONAL)")
        OutlinedTextField(
            value = params.customPromptPrompt,
            onValueChange = { newVal ->
                viewModel.updateParams { it.copy(customPromptPrompt = newVal) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_story_input"),
            placeholder = {
                Text(
                    text = "e.g. Bacon Hair discovers an ancient portal behind Brookhaven hospital...",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = SurfaceCardBorder,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(18.dp))

        // --- 7. AUDIO & SUBTITLE TOGGLES ---
        SectionHeader("7. AUDIO & VIRAL ENHANCEMENTS")
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = SurfaceCardBorder,
            backgroundColor = SurfaceCard
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                ToggleRow(
                    title = "Roblox Sound Effects",
                    subtitle = "Oof sound, jumpscare risers, cash registers & whooshes",
                    checked = params.soundEffectsEnabled,
                    onCheckedChange = { chk -> viewModel.updateParams { it.copy(soundEffectsEnabled = chk) } }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ToggleRow(
                    title = "Dynamic Background Music",
                    subtitle = "Synthwave, 8-bit chip-hop & dramatic piano scores",
                    checked = params.bgMusicEnabled,
                    onCheckedChange = { chk -> viewModel.updateParams { it.copy(bgMusicEnabled = chk) } }
                )
                Spacer(modifier = Modifier.height(10.dp))
                ToggleRow(
                    title = "Animated Viral Subtitles",
                    subtitle = "TikTok/Shorts style high-energy word highlighting",
                    checked = params.animatedSubtitlesEnabled,
                    onCheckedChange = { chk -> viewModel.updateParams { it.copy(animatedSubtitlesEnabled = chk) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 8. BIG GENERATE BUTTON ---
        NeonButton(
            text = "⚡ GENERATE ALL VIDEOS (${params.quantity})",
            onClick = { viewModel.startBulkGeneration() },
            icon = Icons.Default.Bolt,
            gradientColors = listOf(NeonPurple, NeonCyan),
            height = 60.dp,
            modifier = Modifier.fillMaxWidth(),
            testTag = "btn_generate_all_videos"
        )
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

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonCyan,
                uncheckedTrackColor = SurfaceElevated
            )
        )
    }
}
