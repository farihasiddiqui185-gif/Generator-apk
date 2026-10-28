package com.example.robloxshorts.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    gradientColors: List<Color> = listOf(NeonPurple, NeonCyan),
    enabled: Boolean = true,
    height: Dp = 52.dp,
    testTag: String = "neon_button"
) {
    val shape = RoundedCornerShape(12.dp)
    val brush = if (enabled) {
        Brush.horizontalGradient(gradientColors)
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF332A55), Color(0xFF231B3D)))
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .height(height)
            .clip(shape)
            .background(brush)
            .border(
                BorderStroke(
                    1.5.dp,
                    if (enabled) gradientColors.first().copy(alpha = 0.8f) else Color.Transparent
                ),
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else Color.Gray,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) Color.White else Color.Gray,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun NeonCard(
    modifier: Modifier = Modifier,
    borderColor: Color = SurfaceCardBorder,
    backgroundColor: Color = SurfaceCard,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Card(
        modifier = modifier
            .clip(shape)
            .border(BorderStroke(1.dp, borderColor), shape),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = shape
    ) {
        content()
    }
}

@Composable
fun RobloxBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceElevated,
    textColor: Color = NeonCyan,
    borderColor: Color = textColor.copy(alpha = 0.5f)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
fun NeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    colors: List<Color> = listOf(NeonPurple, NeonCyan)
) {
    val shape = RoundedCornerShape(height / 2)
    val clamped = progress.coerceIn(0f, 1f)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .height(height)
            .fillMaxWidth()
            .clip(shape)
            .background(CyberBlack)
            .border(BorderStroke(0.8.dp, SurfaceCardBorder), shape)
    ) {
        Box(
            modifier = Modifier
                .height(height)
                .fillMaxWidth(clamped)
                .clip(shape)
                .background(Brush.horizontalGradient(colors).apply {  })
                .border(BorderStroke(0.5.dp, colors.last().copy(alpha = alphaAnim)), shape)
        )
    }
}
