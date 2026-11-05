package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.AttendanceType
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.NavyBorder
import com.example.ui.theme.NavySurface
import com.example.ui.theme.VerifiedGreen
import com.example.ui.theme.VerifiedGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberContainer

@Composable
fun StatusBadge(
    status: AttendanceStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status) {
        AttendanceStatus.ON_TIME -> Triple(VerifiedGreenContainer, VerifiedGreen, Icons.Default.CheckCircle)
        AttendanceStatus.LATE -> Triple(WarningAmberContainer, WarningAmber, Icons.Default.Warning)
        AttendanceStatus.OVERTIME -> Triple(Color(0xFF312E81), Color(0xFFA5B4FC), Icons.Default.CheckCircle)
        AttendanceStatus.EARLY_DEPARTURE -> Triple(WarningAmberContainer, WarningAmber, Icons.Default.Warning)
        AttendanceStatus.GEO_VERIFIED -> Triple(Color(0xFF064E3B), CyanPrimary, Icons.Default.CheckCircle)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor.copy(alpha = 0.5f))
            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TypeBadge(
    type: AttendanceType,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (type) {
        AttendanceType.CHECK_IN -> Pair(Color(0xFF0369A1).copy(alpha = 0.35f), CyanPrimary)
        AttendanceType.CHECK_OUT -> Pair(Color(0xFF4338CA).copy(alpha = 0.35f), Color(0xFFC7D2FE))
        AttendanceType.BREAK_START -> Pair(Color(0xFFB45309).copy(alpha = 0.35f), Color(0xFFFDE68A))
        AttendanceType.BREAK_END -> Pair(Color(0xFF0F766E).copy(alpha = 0.35f), Color(0xFF99F6E4))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = type.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NavySurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NavyBorder))
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ConfidencePill(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val pct = (confidence * 100).toInt()
    val color = when {
        pct >= 95 -> VerifiedGreen
        pct >= 80 -> WarningAmber
        else -> AlertRed
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$pct% MATCH",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun CloudSyncPill(
    syncStatus: com.example.data.sync.CloudSyncStatus,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when {
        !syncStatus.isOnline -> Triple(
            WarningAmberContainer.copy(alpha = 0.45f),
            WarningAmber,
            if (syncStatus.pendingCount > 0) "Offline (${syncStatus.pendingCount} Cached)" else "Offline Mode"
        )
        syncStatus.isSyncing -> Triple(
            CyanPrimary.copy(alpha = 0.15f),
            CyanPrimary,
            "Syncing to Cloud..."
        )
        syncStatus.pendingCount > 0 -> Triple(
            WarningAmberContainer.copy(alpha = 0.4f),
            WarningAmber,
            "${syncStatus.pendingCount} Pending Sync"
        )
        else -> Triple(
            VerifiedGreenContainer.copy(alpha = 0.35f),
            VerifiedGreen,
            "Cloud Synced (Firestore)"
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
            .clickable(enabled = !syncStatus.isSyncing, onClick = onSyncClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (syncStatus.isSyncing) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                color = textColor,
                strokeWidth = 1.5.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(textColor)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

