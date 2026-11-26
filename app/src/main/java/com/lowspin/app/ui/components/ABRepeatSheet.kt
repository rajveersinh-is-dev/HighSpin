package com.lowspin.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lowspin.app.ui.theme.LowspinOnAccent
import com.lowspin.app.ui.theme.LowspinTextMuted
import com.lowspin.app.ui.theme.LowspinTextPrimary
import com.lowspin.app.util.formatDuration
import com.lowspin.app.util.formatDurationWithMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ABRepeatSheet(
    currentPositionMs: Long,
    durationMs: Long,
    currentLoopRange: Pair<Long, Long>?,
    onSetLoop: (startMs: Long, endMs: Long) -> Unit,
    onClearLoop: () -> Unit,
    onSeek: (positionMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pointA by remember(currentLoopRange) { mutableStateOf(currentLoopRange?.first) }
    var pointB by remember(currentLoopRange) { mutableStateOf(currentLoopRange?.second) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val effectiveA = pointA ?: 0L
    val effectiveB = pointB ?: safeDuration
    val isValidLoop = (pointA != null || pointB != null || currentLoopRange != null) && (effectiveB > effectiveA)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.98f),
        contentColor = LowspinTextPrimary,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.AllInclusive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "A-B Repeat & Loop Section",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LowspinTextPrimary
                    )
                    Text(
                        text = "Current Position: ${formatDurationWithMillis(currentPositionMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Point A Controls
            LoopPointCard(
                label = "Point A (Loop Start)",
                pointMs = pointA,
                onSetCurrent = { pointA = currentPositionMs },
                onAdjust = { delta ->
                    val maxA = if (pointB != null) (pointB!! - 50L).coerceAtLeast(0L) else safeDuration
                    pointA = ((pointA ?: currentPositionMs) + delta).coerceIn(0L, maxA)
                },
                onJump = { pointA?.let(onSeek) }
            )

            Spacer(Modifier.height(14.dp))

            // Point B Controls
            LoopPointCard(
                label = "Point B (Loop End)",
                pointMs = pointB,
                onSetCurrent = { pointB = currentPositionMs },
                onAdjust = { delta ->
                    val minB = if (pointA != null) (pointA!! + 50L).coerceAtMost(safeDuration) else 0L
                    pointB = ((pointB ?: currentPositionMs) + delta).coerceIn(minB, safeDuration)
                },
                onJump = { pointB?.let(onSeek) }
            )

            Spacer(Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val a = pointA ?: 0L
                        val b = pointB ?: safeDuration
                        if (b > a) {
                            onSetLoop(a, b)
                            onDismiss()
                        }
                    },
                    enabled = isValidLoop,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = LowspinOnAccent
                    )
                ) {
                    Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (currentLoopRange != null) "Update Loop" else "Start Loop", fontWeight = FontWeight.Bold)
                }

                if (currentLoopRange != null || pointA != null || pointB != null) {
                    OutlinedButton(
                        onClick = {
                            pointA = null
                            pointB = null
                            onClearLoop()
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoopPointCard(
    label: String,
    pointMs: Long?,
    onSetCurrent: () -> Unit,
    onAdjust: (Long) -> Unit,
    onJump: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.70f))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pointMs?.let { formatDurationWithMillis(it) } ?: "Not set",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (pointMs != null) LowspinTextPrimary else LowspinTextMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (pointMs != null) {
                    TextButton(onClick = onJump) {
                        Text("Jump", color = MaterialTheme.colorScheme.primary)
                    }
                }
                Button(
                    onClick = onSetCurrent,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text("Set Now", fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Millisecond Fine-tuning pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FineTunePill(label = "-1s", onClick = { onAdjust(-1000L) }, modifier = Modifier.weight(1f))
            FineTunePill(label = "-100ms", onClick = { onAdjust(-100L) }, modifier = Modifier.weight(1f))
            FineTunePill(label = "+100ms", onClick = { onAdjust(100L) }, modifier = Modifier.weight(1f))
            FineTunePill(label = "+1s", onClick = { onAdjust(1000L) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FineTunePill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = LowspinTextPrimary
        )
    }
}
