package com.sunodo.app.actions

import android.content.Context
import android.widget.Toast
import com.sunodo.app.data.Packet
import com.sunodo.app.data.PacketType

/**
 * The one place that decides which real OS action a tapped card fires —
 * both MainActivity and ShareReceiverActivity call this instead of
 * duplicating the type-to-action mapping from docs/blueprint.md §3.2:
 * task -> Calendar/Reminders, question -> reply, decision -> Calendar,
 * info -> Copy. If the intent OSActionBridge tries has nothing to handle it
 * (startActivitySafely returns false), this falls back to a clipboard copy
 * so an action never just silently does nothing.
 */
object PacketActionHandler {

    fun handle(context: Context, packet: Packet) {
        when (packet.type) {
            PacketType.TASK, PacketType.DECISION -> handleTaskOrDecision(context, packet)
            PacketType.QUESTION -> handleQuestion(context, packet)
            PacketType.INFO -> {
                OSActionBridge.copyToClipboard(context, "SunoDo", packet.content)
                toast(context, "Copied to clipboard")
            }
        }
    }

    private fun handleTaskOrDecision(context: Context, packet: Packet) {
        val due = packet.dueEpochDay
        val fired = if (due != null) {
            OSActionBridge.addToCalendar(context, packet.content, due)
        } else {
            OSActionBridge.shareText(context, packet.content, "Save as reminder")
        }
        if (!fired) fallBackToClipboard(context, packet.content)
    }

    private fun handleQuestion(context: Context, packet: Packet) {
        val replyText = packet.replySuggestion ?: packet.content
        val fired = OSActionBridge.shareText(context, replyText, "Reply with")
        if (!fired) fallBackToClipboard(context, replyText)
    }

    private fun fallBackToClipboard(context: Context, text: String) {
        OSActionBridge.copyToClipboard(context, "SunoDo", text)
        toast(context, "No app found for that — copied to clipboard instead")
    }

    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
