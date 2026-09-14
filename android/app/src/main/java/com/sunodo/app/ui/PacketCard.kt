package com.sunodo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunodo.app.data.Packet
import com.sunodo.app.data.PacketType
import com.sunodo.app.ui.theme.Coral
import com.sunodo.app.ui.theme.Ink
import com.sunodo.app.ui.theme.Marigold
import com.sunodo.app.ui.theme.Sage
import com.sunodo.app.ui.theme.Steel
import com.sunodo.app.ui.theme.Surface
import com.sunodo.app.ui.theme.TextMuted
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private fun accentFor(type: PacketType): Color = when (type) {
    PacketType.TASK -> Marigold
    PacketType.QUESTION -> Coral
    PacketType.DECISION -> Sage
    PacketType.INFO -> Steel
}

private fun labelFor(type: PacketType): String = when (type) {
    PacketType.TASK -> "Task"
    PacketType.QUESTION -> "Question"
    PacketType.DECISION -> "Decision"
    PacketType.INFO -> "Info"
}

private fun actionLabelFor(packet: Packet): String = when (packet.type) {
    PacketType.TASK, PacketType.DECISION -> if (packet.dueEpochDay != null) "Add to calendar" else "Save as reminder"
    PacketType.QUESTION -> "Reply"
    PacketType.INFO -> "Copy"
}

private fun friendlyDate(epochDay: Long): String {
    val date = LocalDate.ofEpochDay(epochDay)
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    return "$weekday, ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
}

/**
 * One packet, one accent color, one primary action — deliberately not a
 * generic identical-looking card grid. The left accent bar plus a plain-text
 * kind label carries the type; the action button always states exactly what
 * tapping it does (see actionLabelFor). `onAction` fires the real
 * OSActionBridge intent via PacketActionHandler (Stage 4).
 *
 * Dismissing works two ways on purpose: swipe, for anyone used to that
 * pattern, and the explicit × button for anyone who isn't — the same
 * "don't assume everyone types/gestures the same way" reasoning
 * docs/blueprint.md applies to voice notes in the first place applies here
 * too. Both call the same onDismiss.
 *
 * SwipeToDismissBox's exact parameter names have shifted across Compose
 * Material3 releases while the API was experimental; this matches the
 * shape documented around the compose-bom version pinned in this project's
 * build.gradle.kts, but — like the MediaPipe integration in pipeline/ —
 * hasn't been checked against a live Gradle sync from this sandbox, so
 * verify it if Android Studio's sync flags it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketCard(
    packet: Packet,
    onAction: (Packet) -> Unit,
    onDismiss: (Packet) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDismiss(packet)
            }
            true
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        backgroundContent = { DismissBackground(dismissState.targetValue) }
    ) {
        PacketCardContent(packet = packet, onAction = onAction, onDismiss = onDismiss)
    }
}

@Composable
private fun DismissBackground(targetValue: SwipeToDismissBoxValue) {
    val (color, alignment) = when (targetValue) {
        SwipeToDismissBoxValue.StartToEnd -> Coral to Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Coral to Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> Color.Transparent to Alignment.Center
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        if (targetValue != SwipeToDismissBoxValue.Settled) {
            Icon(imageVector = Icons.Filled.Close, contentDescription = "Dismiss", tint = Ink)
        }
    }
}

@Composable
private fun PacketCardContent(
    packet: Packet,
    onAction: (Packet) -> Unit,
    onDismiss: (Packet) -> Unit
) {
    val accent = accentFor(packet.type)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxWidth()
                    .background(accent)
            )
            Column(modifier = Modifier.weight(1f).padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = labelFor(packet.type),
                        style = MaterialTheme.typography.bodySmall,
                        color = accent
                    )
                    IconButton(onClick = { onDismiss(packet) }, modifier = Modifier.size(22.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = packet.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = if (packet.dueEpochDay != null) 4.dp else 10.dp)
                )
                packet.dueEpochDay?.let { epochDay ->
                    Text(
                        text = "Due ${friendlyDate(epochDay)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                OutlinedButton(
                    onClick = { onAction(packet) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                ) {
                    Text(actionLabelFor(packet), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
