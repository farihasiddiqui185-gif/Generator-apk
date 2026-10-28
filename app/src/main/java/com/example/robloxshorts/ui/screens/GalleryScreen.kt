package com.example.robloxshorts.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.robloxshorts.ui.components.NeonButton
import com.example.robloxshorts.ui.components.NeonCard
import com.example.robloxshorts.ui.components.RobloxBadge
import com.example.robloxshorts.ui.components.VideoCard
import com.example.robloxshorts.ui.viewmodel.ShortsViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GalleryScreen(
    viewModel: ShortsViewModel,
    modifier: Modifier = Modifier
) {
    val allVideos by viewModel.allVideos.collectAsState()
    val selectedIds by viewModel.selectedVideoIds.collectAsState()
    val completedCount = allVideos.count { it.status == "COMPLETED" }
    val activeCount = allVideos.count { it.status == "RENDERING" || it.status == "SCRIPTING" || it.status == "QUEUED" }
    val failedCount = allVideos.count { it.status == "FAILED" }

    var filterType by remember { mutableStateOf("ALL") } // "ALL", "COMPLETED", "IN_PROGRESS", "FAILED"

    val filteredVideos = when (filterType) {
        "COMPLETED" -> allVideos.filter { it.status == "COMPLETED" }
        "IN_PROGRESS" -> allVideos.filter { it.status == "RENDERING" || it.status == "SCRIPTING" || it.status == "QUEUED" }
        "FAILED" -> allVideos.filter { it.status == "FAILED" }
        else -> allVideos
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBlack)
            .padding(16.dp)
    ) {
        // --- 1. TITLE & STATS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VIDEO LIBRARY",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${allVideos.size} videos total • $completedCount completed",
                    color = NeonCyan,
                    fontSize = 12.sp
                )
            }

            if (completedCount > 0) {
                NeonButton(
                    text = "ZIP ALL",
                    onClick = { viewModel.exportAllAsZip() },
                    icon = Icons.Default.Archive,
                    gradientColors = listOf(NeonGreen, NeonCyan),
                    height = 38.dp,
                    testTag = "zip_all_btn"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. FILTER CHIPS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                label = "All (${allVideos.size})",
                isSelected = filterType == "ALL",
                color = NeonCyan,
                onClick = { filterType = "ALL" }
            )
            FilterChip(
                label = "Done ($completedCount)",
                isSelected = filterType == "COMPLETED",
                color = NeonGreen,
                onClick = { filterType = "COMPLETED" }
            )
            FilterChip(
                label = "Active ($activeCount)",
                isSelected = filterType == "IN_PROGRESS",
                color = NeonPurple,
                onClick = { filterType = "IN_PROGRESS" }
            )
            if (failedCount > 0) {
                FilterChip(
                    label = "Failed ($failedCount)",
                    isSelected = filterType == "FAILED",
                    color = NeonRed,
                    onClick = { filterType = "FAILED" }
                )
            }
        }

        // --- 3. MULTI-SELECTION ACTION BAR ---
        AnimatedVisibility(visible = selectedIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonCyan,
                backgroundColor = SurfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedIds.size} SELECTED",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.exportSelectedAsZip() }) {
                            Icon(Icons.Default.Archive, contentDescription = "Zip", tint = NeonGreen)
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonRed)
                        }
                        IconButton(onClick = { viewModel.selectAllCompleted() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All", tint = TextPrimary)
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 4. VIDEO LIST ---
        if (filteredVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 60.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = SurfaceCardBorder,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Videos in This View",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Press 'Create AI Shorts' or 'Generate 10' to start!",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NeonButton(
                        text = "⚡ GENERATE FIRST SHORT",
                        onClick = { viewModel.setTab(1) },
                        gradientColors = listOf(NeonPurple, NeonCyan),
                        height = 46.dp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_library_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredVideos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        isSelected = selectedIds.contains(video.id),
                        onSelectToggle = { viewModel.toggleSelectVideo(video.id) },
                        onPlay = { viewModel.playVideo(video) },
                        onSaveToGallery = { viewModel.saveToPhoneGallery(video) },
                        onDownloadMp4 = { viewModel.downloadMp4(video) },
                        onShare = { viewModel.shareVideo(video) },
                        onRetry = { viewModel.retryVideo(video.id) },
                        onDelete = { viewModel.deleteVideo(video) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else SurfaceCard)
            .border(
                1.dp,
                if (isSelected) color else SurfaceCardBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) color else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
