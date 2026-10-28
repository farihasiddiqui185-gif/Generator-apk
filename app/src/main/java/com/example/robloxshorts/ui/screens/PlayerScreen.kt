package com.example.robloxshorts.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.robloxshorts.data.local.entity.VideoEntity
import com.example.robloxshorts.ui.components.NeonButton
import com.example.robloxshorts.ui.components.RobloxBadge
import com.example.robloxshorts.ui.viewmodel.ShortsViewModel
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.io.File

@Composable
fun PlayerScreen(
    video: VideoEntity,
    viewModel: ShortsViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.closePlayer()
    }

    var isPlaying by remember { mutableStateOf(true) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen")
    ) {
        val videoFile = video.videoPath?.let { File(it) }

        if (videoFile != null && videoFile.exists()) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        setVideoURI(Uri.fromFile(videoFile))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.start()
                            isPlaying = true
                        }
                        videoViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        videoViewRef?.let { vv ->
                            if (vv.isPlaying) {
                                vv.pause()
                                isPlaying = false
                            } else {
                                vv.start()
                                isPlaying = true
                            }
                        }
                    }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Video file not found or corrupted",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }

        // Top Gradient & Title Info Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(CyberBlack.copy(alpha = 0.9f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RobloxBadge(
                            text = video.category,
                            textColor = NeonCyan,
                            backgroundColor = SurfaceElevated
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${video.durationSeconds}s • 1080x1920 MP4",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.closePlayer() },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CyberBlack.copy(alpha = 0.7f))
                        .border(1.dp, SurfaceCardBorder, CircleShape)
                        .testTag("close_player_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }

        // Center Play/Pause indicator icon when paused
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(CyberBlack.copy(alpha = 0.8f))
                    .border(2.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = NeonCyan,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Bottom Action Bar: SAVE TO GALLERY, DOWNLOAD MP4, SHARE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, CyberBlack.copy(alpha = 0.95f))
                    )
                )
                .padding(16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeonButton(
                    text = "SAVE TO GALLERY",
                    onClick = { viewModel.saveToPhoneGallery(video) },
                    icon = Icons.Default.Save,
                    gradientColors = listOf(NeonGreen, NeonCyan),
                    height = 50.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "player_save_gallery"
                )

                NeonButton(
                    text = "DOWNLOAD MP4",
                    onClick = { viewModel.downloadMp4(video) },
                    icon = Icons.Default.Download,
                    gradientColors = listOf(NeonPurple, NeonPink),
                    height = 50.dp,
                    modifier = Modifier.weight(1f),
                    testTag = "player_download_mp4"
                )

                IconButton(
                    onClick = { viewModel.shareVideo(video) },
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                        .testTag("player_share_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
