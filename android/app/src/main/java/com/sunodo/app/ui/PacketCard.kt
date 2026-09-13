package com.sunodo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sunodo.app.data.Packet
import com.sunodo.app.data.PacketType
import com.sunodo.app.ui.theme.Coral
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
    PacketType.QUESTION -> "Copy reply"
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
 * tapping it does (see actionLabelFor), never a bare icon.
 *
 * The primary action is wired to `onAction` only — actually firing a native
 * Calendar/reply Intent is Stage 4's OSActionBridge; for now the caller can
 * show a placeholder toast so the UI is fully clickable end to end already.
 */
@Composable
fun PacketCard(
    packet: Packet,
    onAction: (Packet) -> Unit,
    onDismiss: (Packet) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = accentFor(packet.type)

    Card(
        modifier = modifier.fillMaxWidth(),
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
