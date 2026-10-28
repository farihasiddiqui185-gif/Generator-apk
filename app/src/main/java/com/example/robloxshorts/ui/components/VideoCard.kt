package com.example.robloxshorts.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.robloxshorts.data.local.entity.VideoEntity
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VideoCard(
    video: VideoEntity,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onPlay: () -> Unit,
    onSaveToGallery: () -> Unit,
    onDownloadMp4: () -> Unit,
    onShare: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompleted = video.status == "COMPLETED"
    val isRendering = video.status == "RENDERING" || video.status == "SCRIPTING" || video.status == "QUEUED"
    val isFailed = video.status == "FAILED"

    val statusColor = when {
        isCompleted -> NeonGreen
        isRendering -> NeonCyan
        isFailed -> NeonRed
        else -> NeonYellow
    }

    NeonCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("video_card_${video.id}"),
        borderColor = if (isSelected) NeonCyan else SurfaceCardBorder,
        backgroundColor = if (isSelected) SurfaceCard.copy(alpha = 0.95f) else SurfaceCard
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Header with Selection, Title, Category and Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = NeonCyan,
                        checkmarkColor = CyberBlack,
                        uncheckedColor = SurfaceCardBorder
                    ),
                    modifier = Modifier.testTag("checkbox_${video.id}")
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RobloxBadge(
                            text = video.category,
                            textColor = NeonPurple,
                            backgroundColor = SurfaceElevated
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${video.durationSeconds}s",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (video.fileSizeBytes > 0) {
                            Text(
                                text = " • ${"%.1f".format(video.fileSizeBytes / (1024f * 1024f))} MB",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_btn_${video.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Thumbnail Preview & Status Box
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vertical 9:16 Video Thumbnail Box
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberBlack)
                        .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                        .clickable(enabled = isCompleted, onClick = onPlay),
                    contentAlignment = Alignment.Center
                ) {
                    val thumbBitmap = remember(video.thumbnailPath) {
                        video.thumbnailPath?.let { path ->
                            val f = File(path)
                            if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                        }
                    }
                    if (thumbBitmap != null) {
                        Image(
                            bitmap = thumbBitmap,
                            contentDescription = "Shorts Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Cyber Roblox Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(SurfaceElevated, CyberBlack)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ROBLOX\n9:16",
                                color = NeonPurple.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Play icon overlay if completed
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CyberBlack.copy(alpha = 0.75f))
                                .border(1.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Status Details & Progress
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = video.status,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (video.isSavedToGallery) {
                            Spacer(modifier = Modifier.width(8.dp))
                            RobloxBadge(
                                text = "GALLERY SAVED",
                                textColor = NeonGreen,
                                backgroundColor = SurfaceElevated
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = video.progressMessage,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    if (isRendering) {
                        Spacer(modifier = Modifier.height(8.dp))
                        NeonProgressBar(
                            progress = video.progress,
                            height = 8.dp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${(video.progress * 100).toInt()}% rendered",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (isFailed && video.errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Error: ${video.errorMessage}",
                            color = NeonRed,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        NeonButton(
                            text = "RETRY VIDEO",
                            onClick = onRetry,
                            icon = Icons.Default.Refresh,
                            height = 36.dp,
                            gradientColors = listOf(NeonRed, NeonYellow),
                            testTag = "retry_btn_${video.id}"
                        )
                    }

                    val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        .format(Date(video.createdAt))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Created: $dateStr",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            // Row 3: Action Buttons (Only when video is ready)
            if (isCompleted) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeonButton(
                        text = "GALLERY",
                        onClick = onSaveToGallery,
                        icon = Icons.Default.Save,
                        gradientColors = listOf(NeonGreen, NeonCyan),
                        height = 42.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "save_gallery_${video.id}"
                    )

                    NeonButton(
                        text = "DOWNLOAD",
                        onClick = onDownloadMp4,
                        icon = Icons.Default.Download,
                        gradientColors = listOf(NeonPurple, NeonPink),
                        height = 42.dp,
                        modifier = Modifier.weight(1f),
                        testTag = "download_mp4_${video.id}"
                    )

                    IconButton(
                        onClick = onPlay,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, NeonCyan, RoundedCornerShape(10.dp))
                            .testTag("play_icon_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = NeonCyan
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, SurfaceCardBorder, RoundedCornerShape(10.dp))
                            .testTag("share_icon_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TextPrimary
                        )
                    }
                }
            }
        }
    }
}
